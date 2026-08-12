package de.hasil.pictree.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.hasil.pictree.util.I18n;

/**
 * Eingabebereich für den Kommentar-/Stempeltext samt Aktions-Buttons
 * ("Bild speichern", "Ordner stempeln"). Textänderungen werden live an einen
 * Listener gemeldet (Live-Preview). Die Buttons werden in späteren Schritten
 * verdrahtet und sind bis dahin deaktiviert.
 */
public class CommentPanel extends JPanel {

    private final JTextArea textArea = new JTextArea(3, 20);
    private final JButton saveButton = new JButton(I18n.t("button.save"));
    private final JButton batchButton = new JButton(I18n.t("button.batch"));

    public CommentPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(I18n.t("comment.title")));

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveButton.setEnabled(false);
        batchButton.setEnabled(false);
        buttons.add(batchButton);
        buttons.add(saveButton);
        add(buttons, BorderLayout.SOUTH);
    }

    /** Meldet jede Textänderung (Einfügen/Löschen/Ändern) an den Listener. */
    public void addTextChangeListener(Consumer<String> listener) {
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                listener.accept(getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                listener.accept(getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                listener.accept(getText());
            }
        });
    }

    public String getText() {
        return textArea.getText();
    }

    public void setText(String text) {
        textArea.setText(text == null ? "" : text);
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    public JButton getBatchButton() {
        return batchButton;
    }
}
