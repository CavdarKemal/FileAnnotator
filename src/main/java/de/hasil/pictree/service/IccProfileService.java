package de.hasil.pictree.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.imaging.Imaging;

import de.hasil.pictree.util.Logging;

/**
 * Überträgt das ICC-Farbprofil vom Originalbild in ein neu geschriebenes
 * JPEG. commons-imaging kann ICC lesen, aber nicht schreiben; daher wird das
 * Profil als {@code APP2}-Segment ("ICC_PROFILE") direkt hinter dem SOI-Marker
 * eingefügt.
 */
public class IccProfileService {

    private static final Logger LOG = Logging.get(IccProfileService.class);

    /** Max. Nutzdaten je APP2-Segment (65535 - 2 Länge - 12 Kennung - 2 Sequenz). */
    private static final int MAX_CHUNK = 65519;

    /**
     * Kopiert das ICC-Profil von {@code source} in {@code targetJpeg}.
     *
     * @return {@code true}, wenn ein Profil übertragen wurde.
     */
    public boolean copyIccProfile(File source, File targetJpeg) {
        try {
            byte[] icc = Imaging.getIccProfileBytes(source);
            if (icc == null || icc.length == 0) {
                return false;
            }
            byte[] jpeg = Files.readAllBytes(targetJpeg.toPath());
            byte[] result = insertIccProfile(jpeg, icc);
            Files.write(targetJpeg.toPath(), result);
            return true;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "ICC-Kopie fehlgeschlagen (" + source + " -> " + targetJpeg + ")", ex);
            return false;
        }
    }

    /** Fügt ein ICC-Profil als APP2-Segment(e) direkt hinter dem SOI-Marker ein. */
    static byte[] insertIccProfile(byte[] jpeg, byte[] icc) throws IOException {
        if (jpeg.length < 2 || (jpeg[0] & 0xFF) != 0xFF || (jpeg[1] & 0xFF) != 0xD8) {
            throw new IOException("Keine gültige JPEG-Datei (SOI fehlt)");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(jpeg.length + icc.length + 64);
        out.write(0xFF);
        out.write(0xD8); // SOI

        int chunks = (icc.length + MAX_CHUNK - 1) / MAX_CHUNK;
        int offset = 0;
        for (int seq = 1; seq <= chunks; seq++) {
            int len = Math.min(MAX_CHUNK, icc.length - offset);
            int segmentLength = len + 2 + 12 + 2; // Länge + "ICC_PROFILE\0" + seq + count
            out.write(0xFF);
            out.write(0xE2); // APP2
            out.write((segmentLength >> 8) & 0xFF);
            out.write(segmentLength & 0xFF);
            out.write("ICC_PROFILE".getBytes(StandardCharsets.US_ASCII));
            out.write(0);
            out.write(seq);
            out.write(chunks);
            out.write(icc, offset, len);
            offset += len;
        }

        // Restlicher JPEG-Inhalt hinter dem SOI.
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
    }
}
