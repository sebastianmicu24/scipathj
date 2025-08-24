package com.scipath.scipathj.ui.common.dialogs;

import com.scipath.scipathj.ui.themes.ThemeManager;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import javax.swing.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Dialog for managing application preferences and settings.
 */
public class PreferencesDialog extends JDialog {

  private JComboBox<ThemeManager.Theme> themeComboBox;
  private boolean settingsChanged = false;

  public PreferencesDialog(JFrame parent) {
    super(parent, "Preferences", true);

    initializeComponents();
    setupLayout();
    setupEventHandlers();
    loadCurrentSettings();

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setResizable(false);
    pack();
    setLocationRelativeTo(parent);
  }

  private void initializeComponents() {
    themeComboBox = new JComboBox<>(ThemeManager.getAvailableThemes());
    themeComboBox.setRenderer(new ThemeComboBoxRenderer());
  }

  private void setupLayout() {
    setLayout(new BorderLayout());
    add(createContentPanel(), BorderLayout.CENTER);
    add(createButtonPanel(), BorderLayout.SOUTH);
  }

  private JPanel createContentPanel() {
    JPanel panel = UIUtils.createVerticalPanel();
    panel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));
    panel.add(createAppearanceSection());
    return panel;
  }

  private JPanel createAppearanceSection() {
    JPanel section = UIUtils.createPanel(new GridBagLayout());
    section.setBorder(BorderFactory.createTitledBorder("Appearance"));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets =
        new Insets(
            UIConstants.MEDIUM_SPACING,
            UIConstants.MEDIUM_SPACING,
            UIConstants.MEDIUM_SPACING,
            UIConstants.MEDIUM_SPACING);
    gbc.anchor = GridBagConstraints.WEST;

    // Theme selection
    gbc.gridx = 0;
    gbc.gridy = 0;
    JLabel themeLabel = UIUtils.createIconLabel(FontAwesomeSolid.PALETTE, 16, null);
    themeLabel.setText("Theme:");
    section.add(themeLabel, gbc);

    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    themeComboBox.setPreferredSize(new Dimension(200, 30));
    section.add(themeComboBox, gbc);

    // Theme description
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 2;
    JLabel descriptionLabel =
        UIUtils.createLabel(
            "<html><i>Choose between light and dark theme for the application"
                + " interface.</i></html>",
            UIConstants.TINY_FONT_SIZE,
            Color.GRAY);
    section.add(descriptionLabel, gbc);

    return section;
  }

  private JPanel createButtonPanel() {
    JPanel panel = UIUtils.createPanel(new FlowLayout(FlowLayout.RIGHT));
    panel.setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING, UIConstants.LARGE_SPACING));

    JButton applyButton = createDialogButton("Apply", FontAwesomeSolid.CHECK, e -> applySettings());
    JButton cancelButton = createDialogButton("Cancel", FontAwesomeSolid.TIMES, e -> dispose());
    JButton okButton =
        createDialogButton(
            "OK",
            FontAwesomeSolid.CHECK_CIRCLE,
            e -> {
              applySettings();
              dispose();
            });

    panel.add(applyButton);
    panel.add(Box.createHorizontalStrut(UIConstants.MEDIUM_SPACING));
    panel.add(cancelButton);
    panel.add(Box.createHorizontalStrut(UIConstants.MEDIUM_SPACING));
    panel.add(okButton);

    return panel;
  }

  private JButton createDialogButton(
      String text, FontAwesomeSolid icon, java.awt.event.ActionListener listener) {
    JButton button = UIUtils.createButton(text, icon, listener);
    button.setPreferredSize(new Dimension(80, 30));
    return button;
  }

  private void setupEventHandlers() {
    themeComboBox.addActionListener(e -> settingsChanged = true);
  }

  private void loadCurrentSettings() {
    themeComboBox.setSelectedItem(ThemeManager.getCurrentTheme());
    settingsChanged = false;
  }

  private void applySettings() {
    try {
      ThemeManager.Theme selectedTheme = (ThemeManager.Theme) themeComboBox.getSelectedItem();
      if (selectedTheme != null && selectedTheme != ThemeManager.getCurrentTheme()) {
        ThemeManager.applyTheme(selectedTheme);
      }

      settingsChanged = false;
      showMessage(
          "Settings applied successfully!", "Settings Applied", JOptionPane.INFORMATION_MESSAGE);

    } catch (Exception e) {
      showMessage(
          "Failed to apply settings: " + e.getMessage(),
          "Settings Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private void showMessage(String message, String title, int messageType) {
    JOptionPane.showMessageDialog(this, message, title, messageType);
  }

  public boolean hasUnsavedChanges() {
    return settingsChanged;
  }

  /**
   * Custom renderer for the theme combo box.
   */
  private static class ThemeComboBoxRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(
        JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      if (value instanceof ThemeManager.Theme) {
        ThemeManager.Theme theme = (ThemeManager.Theme) value;
        setText(theme.getDisplayName());

        // Add theme-specific icons
        FontIcon icon;
        switch (theme) {
          case LIGHT:
            icon = FontIcon.of(FontAwesomeSolid.SUN, 16);
            break;
          case DARK:
            icon = FontIcon.of(FontAwesomeSolid.MOON, 16);
            break;
          default:
            icon = FontIcon.of(FontAwesomeSolid.SUN, 16);
            break;
        }
        setIcon(icon);
      }

      return this;
    }
  }

  /**
   * Shows the preferences dialog.
   *
   * @param parent the parent frame
   */
  public static void showPreferencesDialog(JFrame parent) {
    SwingUtilities.invokeLater(
        () -> {
          PreferencesDialog dialog = new PreferencesDialog(parent);
          dialog.setVisible(true);
        });
  }
}
