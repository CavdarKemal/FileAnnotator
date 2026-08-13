package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/**
 * Regressions-Schutz für die Render-Pipeline: das Ergebnis fester Eingaben wird
 * gegen ein eingechecktes Referenzbild verglichen. Die Toleranz fängt kleine
 * Antialiasing-/Hinting-Unterschiede zwischen Plattformen ab; grobe Regressionen
 * (fehlender Text, falsche Farbe/Position) schlagen an.
 *
 * <p>Bei bewusster Rendering-Änderung das Referenzbild neu erzeugen (siehe
 * {@code gen/GenGolden.java} in der Projekt-Historie).
 */
class GoldenImageTest {

    @Test
    void renderMatchesGoldenWithinTolerance() throws Exception {
        BufferedImage reference;
        try (InputStream in = getClass().getResourceAsStream("/golden/stamp-basic.png")) {
            assertNotNull(in, "Referenzbild /golden/stamp-basic.png fehlt");
            reference = ImageIO.read(in);
        }

        BufferedImage actual = GoldenFixtures.render();
        assertEquals(reference.getWidth(), actual.getWidth());
        assertEquals(reference.getHeight(), actual.getHeight());

        long differing = 0;
        long total = (long) reference.getWidth() * reference.getHeight();
        for (int y = 0; y < reference.getHeight(); y++) {
            for (int x = 0; x < reference.getWidth(); x++) {
                if (channelMaxDiff(reference.getRGB(x, y), actual.getRGB(x, y)) > 30) {
                    differing++;
                }
            }
        }
        double fraction = (double) differing / total;
        assertTrue(fraction < 0.03, "Zu viele abweichende Pixel: " + String.format("%.4f", fraction));
    }

    private static int channelMaxDiff(int a, int b) {
        int dr = Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF));
        int dg = Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF));
        int db = Math.abs((a & 0xFF) - (b & 0xFF));
        return Math.max(dr, Math.max(dg, db));
    }
}
