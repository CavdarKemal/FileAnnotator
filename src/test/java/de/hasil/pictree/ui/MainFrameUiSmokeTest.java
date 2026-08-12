package de.hasil.pictree.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.hasil.pictree.service.AppSettings;

/**
 * UI-Smoke-Test direkt auf dem Event-Dispatch-Thread. Baut das Hauptfenster,
 * prüft die Verdrahtung (Menü, Buttons) und eine einfache Interaktion
 * (Kommentar-Eingabe → Aktivierung/State). In Headless-Umgebungen (CI) wird der
 * Test übersprungen.
 *
 * <p>Hinweis: AssertJ-Swing wäre die naheliegende Wahl, referenziert aber
 * {@code java.applet.Applet}, das in JDK 26 entfernt wurde. Daher dieser
 * bibliotheksfreie EDT-Test.
 */
class MainFrameUiSmokeTest {

    @Test
    void buildsWiresAndReactsToCommentInput(@TempDir Path dir) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Headless: UI-Smoke-Test übersprungen");

        AppSettings settings = new AppSettings(dir.resolve("settings.properties"));
        AtomicReference<MainFrame> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> ref.set(new MainFrame(settings)));
        MainFrame frame = ref.get();

        try {
            SwingUtilities.invokeAndWait(() -> {
                // Grundverdrahtung.
                assertNotNull(frame.getJMenuBar(), "Menüleiste vorhanden");
                assertTrue(frame.getJMenuBar().getMenuCount() >= 4, "Datei/Bearbeiten/Ansicht/Vorlagen…");
                assertEquals("PicTree FileAnnotator", frame.getTitle());

                CommentPanel comment = frame.getCommentPanel();
                // Ohne selektiertes Bild sind Speichern/Batch inaktiv.
                assertFalse(comment.getSaveButton().isEnabled());
                assertFalse(comment.getBatchButton().isEnabled());

                // Interaktion: Text eingeben -> wird übernommen.
                comment.setText("Hallo Welt");
                assertEquals("Hallo Welt", comment.getText());
            });
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }
}
