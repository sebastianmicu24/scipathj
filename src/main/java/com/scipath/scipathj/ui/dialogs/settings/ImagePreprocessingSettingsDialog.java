package com.scipath.scipathj.ui.dialogs.settings;

import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import javax.swing.*;

/**
 * Settings dialog for Image Preprocessing step.
 */
public class ImagePreprocessingSettingsDialog extends JDialog {

  public ImagePreprocessingSettingsDialog(Frame parent) {
    super(parent, "Image Preprocessing Settings", true);
    initializeDialog();
  }

  private void initializeDialog() {
    setSize(450, 350);
    setLocationRelativeTo(getParent());

    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBorder(UIUtils.createPadding(20, 20, 20, 20));

    // Title
    JLabel titleLabel =
        UIUtils.createBoldLabel("Image Preprocessing Settings", UIConstants.SUBTITLE_FONT_SIZE);
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    contentPanel.add(titleLabel, BorderLayout.NORTH);

    // Settings content
    JPanel settingsPanel = createSettingsPanel();
    JScrollPane scrollPane = new JScrollPane(settingsPanel);
    scrollPane.setBorder(null);
    contentPanel.add(scrollPane, BorderLayout.CENTER);

    // Button panel
    JPanel buttonPanel = createButtonPanel();
    contentPanel.add(buttonPanel, BorderLayout.SOUTH);

    add(contentPanel);
  }

  private JPanel createSettingsPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(10, 10, 10, 10);
    gbc.anchor = GridBagConstraints.WEST;

    // Gaussian Blur Settings
    addSettingRow(
        panel,
        gbc,
        "Gaussian Blur Sigma:",
        "2.0",
        "Standard deviation for Gaussian blur preprocessing");

    // Brightness Adjustment
    addSettingRow(
        panel, gbc, "Brightness Adjustment:", "0", "Brightness adjustment value (-100 to +100)");

    // Contrast Enhancement
    addSettingRow(
        panel, gbc, "Contrast Enhancement:", "1.0", "Contrast multiplication factor (0.1 to 3.0)");

    // Noise Reduction
    addSettingRow(panel, gbc, "Noise Reduction:", "true", "Enable noise reduction preprocessing");

    // Color Normalization
    addSettingRow(panel, gbc, "Color Normalization:", "false", "Enable color normalization");

    return panel;
  }

  private void addSettingRow(
      JPanel panel, GridBagConstraints gbc, String labelText, String defaultValue, String tooltip) {
    gbc.gridx = 0;
    gbc.gridy++;
    JLabel label = UIUtils.createLabel(labelText, UIConstants.NORMAL_FONT_SIZE, null);
    label.setToolTipText(tooltip);
    panel.add(label, gbc);

    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    JTextField textField = new JTextField(defaultValue, 15);
    textField.setToolTipText(tooltip);
    panel.add(textField, gbc);

    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
  }

  private JPanel createButtonPanel() {
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    JButton okButton = UIUtils.createStandardButton("OK", null);
    JButton cancelButton = UIUtils.createStandardButton("Cancel", null);
    JButton resetButton = UIUtils.createStandardButton("Reset to Defaults", null);

    okButton.addActionListener(
        e -> {
          // TODO: Save settings
          dispose();
        });

    cancelButton.addActionListener(e -> dispose());

    resetButton.addActionListener(
        e -> {
          // TODO: Reset to default values
        });

    buttonPanel.add(resetButton);
    buttonPanel.add(cancelButton);
    buttonPanel.add(okButton);

    return buttonPanel;
  }
}
