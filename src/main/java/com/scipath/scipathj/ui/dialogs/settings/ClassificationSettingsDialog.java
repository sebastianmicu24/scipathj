package com.scipath.scipathj.ui.dialogs.settings;

import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import javax.swing.*;

public class ClassificationSettingsDialog extends JDialog {

  public ClassificationSettingsDialog(Frame parent) {
    super(parent, "Classification Settings", true);
    initializeDialog();
  }

  private void initializeDialog() {
    setSize(450, 350);
    setLocationRelativeTo(getParent());

    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    JLabel titleLabel =
        UIUtils.createBoldLabel("Classification Settings", UIConstants.SUBTITLE_FONT_SIZE);
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    contentPanel.add(titleLabel, BorderLayout.NORTH);

    JPanel settingsPanel = createSettingsPanel();
    contentPanel.add(new JScrollPane(settingsPanel), BorderLayout.CENTER);
    contentPanel.add(createButtonPanel(), BorderLayout.SOUTH);
    add(contentPanel);
  }

  private JPanel createSettingsPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets =
        new Insets(
            UIConstants.MEDIUM_SPACING,
            UIConstants.MEDIUM_SPACING,
            UIConstants.MEDIUM_SPACING,
            UIConstants.MEDIUM_SPACING);
    gbc.anchor = GridBagConstraints.WEST;

    addSettingRow(
        panel, gbc, "XGBoost Model:", "liver_he_v1.model", "XGBoost model file for classification");
    addSettingRow(
        panel,
        gbc,
        "Confidence Threshold:",
        "0.7",
        "Minimum confidence for classification (0.0-1.0)");
    addSettingRow(
        panel, gbc, "Feature Selection:", "auto", "Feature selection method (auto/manual)");
    addSettingRow(panel, gbc, "Cross Validation:", "5", "Number of cross-validation folds");

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

    okButton.addActionListener(e -> dispose());
    cancelButton.addActionListener(e -> dispose());
    resetButton.addActionListener(
        e -> {
          /* TODO: Reset values */
        });

    buttonPanel.add(resetButton);
    buttonPanel.add(cancelButton);
    buttonPanel.add(okButton);
    return buttonPanel;
  }
}
