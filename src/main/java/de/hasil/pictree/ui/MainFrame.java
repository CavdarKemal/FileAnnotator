package de.hasil.pictree.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import de.hasil.pictree.App;

/**
 * Hauptfenster der Anwendung. In Schritt 1 nur ein leeres Grundgerüst;
 * Baum, Vorschau und Werkzeugleiste folgen in den nächsten Schritten.
 */
public class MainFrame extends JFrame {

    public MainFrame() {
        super(App.APP_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));
        setLocationByPlatform(true);
        buildContent();
    }

    private void buildContent() {
        JPanel root = new JPanel(new BorderLayout());
        JLabel placeholder = new JLabel("PicTree – Grundgerüst läuft.", SwingConstants.CENTER);
        root.add(placeholder, BorderLayout.CENTER);
        setContentPane(root);
    }
}
