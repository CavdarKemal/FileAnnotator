package de.hasil.pictree.ui;

import java.awt.BorderLayout;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import de.hasil.pictree.model.ImageTypeFilter;
import de.hasil.pictree.util.I18n;

/**
 * Kleines Steuerelement zum Filtern der im Explorer angezeigten Bildtypen.
 * Ein Button öffnet ein Popup mit „Alle Bilder" sowie je einer Checkbox pro
 * Bildgruppe (JPEG, PNG, …). Änderungen werden über einen Callback gemeldet.
 */
public class FileFilterControl extends JPanel {

    private final transient Consumer<ImageTypeFilter> onChange;
    private final JButton button = new JButton();
    private final JPopupMenu popup = new JPopupMenu();
    private final JCheckBox allBox = new JCheckBox(I18n.t("filter.allImages"));
    private final Map<String, JCheckBox> groupBoxes = new LinkedHashMap<>();

    private transient ImageTypeFilter filter;

    public FileFilterControl(ImageTypeFilter initial, Consumer<ImageTypeFilter> onChange) {
        super(new BorderLayout());
        this.filter = initial == null ? ImageTypeFilter.all() : initial;
        this.onChange = onChange;

        button.setText(I18n.t("filter.title"));
        button.setToolTipText(I18n.t("filter.title"));
        buildPopup();
        button.addActionListener(e -> {
            syncBoxes();
            popup.show(button, 0, button.getHeight());
        });
        add(button, BorderLayout.CENTER);
        updateButton();
    }

    private void buildPopup() {
        allBox.addActionListener(e -> {
            filter = allBox.isSelected() ? ImageTypeFilter.all() : emptyFilter();
            syncBoxes();
            fireChange();
        });
        popup.add(allBox);
        popup.addSeparator();

        for (String group : ImageTypeFilter.GROUPS.keySet()) {
            JCheckBox box = new JCheckBox(labelFor(group));
            box.addActionListener(e -> {
                filter = filter.withGroup(group, box.isSelected());
                allBox.setSelected(filter.isAllActive());
                fireChange();
            });
            groupBoxes.put(group, box);
            popup.add(box);
        }
        syncBoxes();
    }

    private static ImageTypeFilter emptyFilter() {
        ImageTypeFilter f = ImageTypeFilter.all();
        for (String group : ImageTypeFilter.GROUPS.keySet()) {
            f = f.withGroup(group, false);
        }
        return f;
    }

    private static String labelFor(String group) {
        // i18n-Key z. B. "filter.jpeg"; fehlt er, liefert I18n den Gruppennamen zurück.
        String translated = I18n.t("filter." + group.toLowerCase(Locale.ROOT));
        return translated.startsWith("filter.") ? group : translated;
    }

    /** Gleicht die Checkbox-Zustände an den aktuellen Filter an. */
    private void syncBoxes() {
        allBox.setSelected(filter.isAllActive());
        for (Map.Entry<String, JCheckBox> e : groupBoxes.entrySet()) {
            e.getValue().setSelected(filter.isGroupActive(e.getKey()));
        }
    }

    private void fireChange() {
        updateButton();
        if (onChange != null) {
            onChange.accept(filter);
        }
    }

    private void updateButton() {
        String summary = filter.isAllActive()
                ? I18n.t("filter.allImages")
                : (filter.isEmpty() ? "—" : String.join(", ", activeGroupLabels()));
        button.setToolTipText(I18n.t("filter.title") + ": " + summary);
    }

    private java.util.List<String> activeGroupLabels() {
        java.util.List<String> labels = new java.util.ArrayList<>();
        for (String group : ImageTypeFilter.GROUPS.keySet()) {
            if (filter.isGroupActive(group)) {
                labels.add(labelFor(group));
            }
        }
        return labels;
    }

    public ImageTypeFilter getFilter() {
        return filter;
    }

    public JComponent getComponent() {
        return this;
    }
}
