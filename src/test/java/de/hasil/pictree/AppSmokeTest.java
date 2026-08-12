package de.hasil.pictree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.Test;

import de.hasil.pictree.ui.MainFrame;

class AppSmokeTest {

    @Test
    void appNameIsDefined() {
        assertEquals("PicTree FileAnnotator", App.APP_NAME);
    }

    @Test
    void mainFrameCanBeConstructed() {
        // In einer echten Desktop-Umgebung darf das Fenster erzeugt werden
        // (ohne setVisible poppt kein Fenster auf). Headless -> überspringen.
        assumeFalse(GraphicsEnvironment.isHeadless(), "Headless-Umgebung: GUI-Test übersprungen");
        MainFrame frame = new MainFrame();
        assertNotNull(frame);
        assertEquals("PicTree FileAnnotator", frame.getTitle());
        frame.dispose();
    }
}
