package com.scipath.scipathj.ui.analysis.dialogs.settings;

import com.scipath.scipathj.analysis.config.ClassificationSettings;
import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.io.File;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import org.kordamp.ikonli.swing.FontIcon;

public class ClassificationSettingsDialog extends JDialog {

  private ClassificationSettings currentSettings;
  private ConfigurationManager configManager;

  // UI mode: either individual files or ZIP
  private boolean useZipMode = true; // Default to ZIP mode for simplicity

  // UI components
  private JTextField zipFileField;
  private JButton zipBrowseButton;

  // Legacy individual file fields (used when not in ZIP mode)
  private JTextField modelPathField;
  private JTextField selectedFeaturesPathField;
  private JTextField labelMappingPathField;
  private JTextField classDetailsPathField;

  // Mode switching
  private JRadioButton zipModeButton;
  private JRadioButton individualModeButton;

  public ClassificationSettingsDialog(Frame parent) {
    super(parent, "Classification Settings", true);
    // Legacy constructor - for backwards compatibility, but requires configuration manager
    // This should not be used in the new system
    throw new UnsupportedOperationException("ClassificationSettingsDialog requires ConfigurationManager. Use ClassificationSettingsDialog(Frame, ConfigurationManager) instead.");
  }

  public ClassificationSettingsDialog(Frame parent, ConfigurationManager configManager) {
    super(parent, "XGBoost Classification Settings", true);
    this.configManager = configManager;
    this.currentSettings = configManager.loadClassificationSettings();
    initializeDialog();
  }

  private void initializeDialog() {
    setSize(600, 450);
    setLocationRelativeTo(getParent());

    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    // Title
    JPanel titlePanel = new JPanel(new BorderLayout());
    titlePanel.setBorder(UIUtils.createPadding(0, 0, UIConstants.MEDIUM_SPACING, 0));
    JLabel titleLabel = UIUtils.createBoldLabel("XGBoost Classification Settings", UIConstants.SUBTITLE_FONT_SIZE);
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

    // Info text
    JTextArea infoArea = new JTextArea(
        "Configure paths to XGBoost classification model files.\n" +
        "• Model File (.json): Required - Your trained XGBoost model\n" +
        "• Selected Features (.txt): Optional - Auto-generated if missing\n" +
        "• Label Mapping (.properties): Optional - Auto-generated if missing\n" +
        "• Class Details (.json): Optional - Default names used if missing\n\n" +
        "You can use either absolute file paths or resource paths (embedded in JAR).");
    infoArea.setEditable(false);
    infoArea.setOpaque(false);
    infoArea.setFont(UIUtils.createLabel("", UIConstants.SMALL_FONT_SIZE, null).getFont());
    infoArea.setWrapStyleWord(true);
    infoArea.setLineWrap(true);

    titlePanel.add(titleLabel, BorderLayout.NORTH);
    titlePanel.add(infoArea, BorderLayout.CENTER);
    contentPanel.add(titlePanel, BorderLayout.NORTH);

    // Settings panel
    JPanel settingsPanel = createSettingsPanel();
    contentPanel.add(new JScrollPane(settingsPanel), BorderLayout.CENTER);

    // Button panel
    contentPanel.add(createButtonPanel(), BorderLayout.SOUTH);
    add(contentPanel);
  }

  // Helper methods for colors and styling
  private static Color getPrimaryColor() {
    return new Color(0, 123, 255); // #007bff - blue works in both themes
  }

  private static Color getSuccessColor() {
    return new Color(40, 167, 69); // #28a745 - green works in both themes
  }

  private static Color getCardColor() {
    return UIManager.getColor("Panel.background");
  }

  private static Color getBorderColor() {
    Color fg = UIManager.getColor("Label.foreground");
    return new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 64);
  }

  private static Color getTextSecondaryColor() {
    return UIManager.getColor("Label.disabledForeground");
  }

  private JPanel createSettingsPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING,
                           UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING);
    gbc.anchor = GridBagConstraints.WEST;

    // Add mode selection
    addModeSelection(panel, gbc);

    // Add spacing
    gbc.gridy++;
    gbc.gridx = 0;
    gbc.gridwidth = 3;
    panel.add(new JLabel(""), gbc);

    // Add mode-specific content
    updateModeContent(panel, gbc);

    return panel;
  }

  private void addModeSelection(JPanel panel, GridBagConstraints gbc) {
    // Mode selection panel
    JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    modePanel.setOpaque(false);

    // Radio buttons for mode selection
    ButtonGroup modeGroup = new ButtonGroup();
    zipModeButton = new JRadioButton("ZIP Bundle (Recommended)", useZipMode);
    individualModeButton = new JRadioButton("Individual Files", !useZipMode);

    modeGroup.add(zipModeButton);
    modeGroup.add(individualModeButton);

    // Add change listeners
    zipModeButton.addActionListener(e -> switchToZipMode());
    individualModeButton.addActionListener(e -> switchToIndividualMode());

    modePanel.add(zipModeButton);
    modePanel.add(individualModeButton);

    // Add help text
    modePanel.add(new JLabel("<html><div style='width: 400px; color: #666; font-style: italic;'>ZIP bundles contain all model files in one convenient package</div></html>"));

    gbc.gridy = 0;
    gbc.gridx = 0;
    gbc.gridwidth = 3;
    panel.add(modePanel, gbc);
  }

  private void switchToZipMode() {
    if (!useZipMode) {
      useZipMode = true;
      // Re-create the settings panel to show ZIP mode
      updateDialogLayout();
    }
  }

  private void switchToIndividualMode() {
    if (useZipMode) {
      useZipMode = false;
      // Re-create the settings panel to show individual files mode
      updateDialogLayout();
    }
  }

  private void updateDialogLayout() {
    // Remove current content panel
    getContentPane().removeAll();

    // Re-create and add new content
    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    // Re-create title panel
    JPanel titlePanel = new JPanel(new BorderLayout());
    titlePanel.setBorder(UIUtils.createPadding(0, 0, UIConstants.MEDIUM_SPACING, 0));
    JLabel titleLabel = UIUtils.createBoldLabel("XGBoost Classification Settings", UIConstants.SUBTITLE_FONT_SIZE);
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

    JTextArea infoArea = new JTextArea(
        useZipMode ? "Configure XGBoost classification using a ZIP bundle.\nThe ZIP should contain: xgboost_model.json, selected_features.txt, etc." :
                     "Configure XGBoost classification using individual files.\nYou can use either absolute file paths or resource paths (embedded in JAR).");
    infoArea.setEditable(false);
    infoArea.setOpaque(false);
    infoArea.setFont(UIUtils.createLabel("", UIConstants.SMALL_FONT_SIZE, null).getFont());
    infoArea.setWrapStyleWord(true);
    infoArea.setLineWrap(true);
    infoArea.setRows(2);

    titlePanel.add(titleLabel, BorderLayout.NORTH);
    titlePanel.add(infoArea, BorderLayout.CENTER);
    contentPanel.add(titlePanel, BorderLayout.NORTH);

    // Create new settings panel
    JPanel settingsPanel = createSettingsPanel();
    contentPanel.add(new JScrollPane(settingsPanel), BorderLayout.CENTER);
    contentPanel.add(createButtonPanel(), BorderLayout.SOUTH);
    add(contentPanel);

    // Refresh the dialog
    revalidate();
    repaint();
    pack();
    setLocationRelativeTo(getParent());
  }

  private void updateModeContent(JPanel panel, GridBagConstraints gbc) {
    if (useZipMode) {
      // ZIP mode: single file selection
      addZipFileSetting(panel, gbc);
    } else {
      // Individual files mode
      addIndividualFileSettings(panel, gbc);
    }
  }

  private void addZipFileSetting(JPanel panel, GridBagConstraints gbc) {
    gbc.gridy = 2;
    gbc.gridx = 0;
    gbc.gridwidth = 3;

    JPanel zipPanel = new JPanel(new BorderLayout());
    zipPanel.setOpaque(true);
    zipPanel.setBackground(getCardColor());
    zipPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(getBorderColor(), 1),
        new EmptyBorder(20, 20, 20, 20)
    ));

    // Label with icon
    JPanel headerPanel = new JPanel();
    headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
    headerPanel.setOpaque(false);

    FontIcon icon = FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.FILE_ARCHIVE,
                                20, getPrimaryColor());
    JPanel labelWithIcon = new JPanel(new FlowLayout(FlowLayout.LEFT));
    labelWithIcon.setOpaque(false);
    labelWithIcon.add(new JLabel("Select XGBoost Model ZIP Bundle"));
    labelWithIcon.add(new JLabel("<html>&nbsp;&nbsp;</html>")); // Spacing
    labelWithIcon.add(new JLabel(icon));
    headerPanel.add(labelWithIcon);

    // Description
    JLabel descLabel = new JLabel("<html><div style='width: 450px; color: #666;'>Choose a ZIP file containing all XGBoost model components. This ZIP file should include xgboost_model.json, selected_features.txt, and other supporting files.</div></html>");
    descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    descLabel.setForeground(getTextSecondaryColor());
    descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    descLabel.setBorder(new EmptyBorder(5, 0, 15, 0));
    headerPanel.add(descLabel);

    zipPanel.add(headerPanel, BorderLayout.NORTH);

    // Input panel
    JPanel inputPanel = new JPanel(new BorderLayout(12, 0));
    inputPanel.setOpaque(false);

    zipFileField = new JTextField();
    zipFileField.setPreferredSize(new Dimension(400, 40));
    zipFileField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    zipFileField.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(getBorderColor(), 1),
        new EmptyBorder(8, 12, 8, 12)
    ));

    zipBrowseButton = UIUtils.createStandardButton("Browse ZIP Files...", null);
    zipBrowseButton.setPreferredSize(new Dimension(140, 40));
    zipBrowseButton.addActionListener(e -> browseForZipFile());

    inputPanel.add(zipFileField, BorderLayout.CENTER);
    inputPanel.add(zipBrowseButton, BorderLayout.EAST);

    zipPanel.add(inputPanel, BorderLayout.CENTER);

    panel.add(zipPanel, gbc);
  }

  private void addIndividualFileSettings(JPanel panel, GridBagConstraints gbc) {
    // Reset row counter for individual mode
    gbc.gridy = 2;

    // Add original individual file settings
    addFileSettingRow(panel, gbc, "XGBoost Model File (.json):",
                      currentSettings.modelPath(), modelPathField = new JTextField(),
                      "XGBoost model file in JSON format (Required)");

    addFileSettingRow(panel, gbc, "Selected Features File (.txt):",
                      currentSettings.selectedFeaturesPath(), selectedFeaturesPathField = new JTextField(),
                      "Text file containing selected feature names (Optional - Auto-generated if missing)");

    addFileSettingRow(panel, gbc, "Label Mapping File (.properties):",
                      currentSettings.labelMappingPath(), labelMappingPathField = new JTextField(),
                      "Properties file mapping XGBoost indices to class IDs (Optional - Auto-generated if missing)");

    addFileSettingRow(panel, gbc, "Class Details File (.json):",
                      currentSettings.classDetailsPath(), classDetailsPathField = new JTextField(),
                      "JSON file containing class names, IDs, and colors (Optional - Defaults used if missing)");
  }

  private void browseForZipFile() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Select XGBoost Model ZIP Bundle");
    fileChooser.setAcceptAllFileFilterUsed(true);

    // Set ZIP file filter
    fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
      @Override
      public boolean accept(java.io.File f) {
        return f.isDirectory() ||
               f.getName().toLowerCase().endsWith(".zip") ||
               f.getName().toLowerCase().endsWith(".gz");
      }

      @Override
      public String getDescription() {
        return "ZIP Archives (*.zip, *.gz)";
      }
    });

    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      java.io.File selectedFile = fileChooser.getSelectedFile();
      zipFileField.setText(selectedFile.getAbsolutePath());
    }
  }

  private void addFileSettingRow(JPanel panel, GridBagConstraints gbc,
                                String labelText, String currentValue,
                                JTextField textField, String tooltip) {

    // Label
    gbc.gridx = 0;
    gbc.gridwidth = 1;
    gbc.anchor = GridBagConstraints.WEST;
    JLabel label = UIUtils.createLabel(labelText, UIConstants.NORMAL_FONT_SIZE, null);
    label.setToolTipText(tooltip);
    panel.add(label, gbc);

    // Text field
    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    textField.setText(currentValue);
    textField.setToolTipText(tooltip);
    panel.add(textField, gbc);

    // Browse button
    gbc.gridx = 2;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    JButton browseButton = UIUtils.createSmallButton("Browse...", null);
    browseButton.addActionListener(e -> browseForFile(textField, getFileFilterForLabel(labelText)));
    browseButton.setToolTipText("Browse for file");
    panel.add(browseButton, gbc);

    // Next row
    gbc.gridy++;
  }

  private void browseForFile(JTextField textField, FileFilter fileFilter) {
    JFileChooser fileChooser = new JFileChooser();

    // Set current directory to the text field value if it's an absolute path
    String currentPath = textField.getText();
    if (currentPath != null && !currentPath.isEmpty()) {
      File currentFile = new File(currentPath);
      if (currentFile.exists()) {
        fileChooser.setCurrentDirectory(currentFile.getParentFile());
        fileChooser.setSelectedFile(currentFile);
      } else if (currentPath.startsWith("/")) {
        // Resource path, start from project root
        fileChooser.setCurrentDirectory(new File("."));
      }
    }

    if (fileFilter != null) {
      fileChooser.setFileFilter(fileFilter);
    }

    int result = fileChooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
      File selectedFile = fileChooser.getSelectedFile();
      textField.setText(selectedFile.getAbsolutePath());
    }
  }

  private FileFilter getFileFilterForLabel(String labelText) {
    if (labelText.contains(".json")) {
      return new FileFilter() {
        @Override
        public boolean accept(File f) {
          return f.isDirectory() || f.getName().toLowerCase().endsWith(".json");
        }

        @Override
        public String getDescription() {
          return "JSON Files (*.json)";
        }
      };
    } else if (labelText.contains(".txt")) {
      return new FileFilter() {
        @Override
        public boolean accept(File f) {
          return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt");
        }

        @Override
        public String getDescription() {
          return "Text Files (*.txt)";
        }
      };
    } else if (labelText.contains(".properties")) {
      return new FileFilter() {
        @Override
        public boolean accept(File f) {
          return f.isDirectory() || f.getName().toLowerCase().endsWith(".properties");
        }

        @Override
        public String getDescription() {
          return "Properties Files (*.properties)";
        }
      };
    }
    return null;
  }

  private JPanel createButtonPanel() {
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    JButton okButton = UIUtils.createStandardButton("Save & Close", null);
    JButton cancelButton = UIUtils.createStandardButton("Cancel", null);
    JButton resetButton = UIUtils.createStandardButton("Reset to Defaults", null);

    okButton.addActionListener(e -> saveSettings());
    cancelButton.addActionListener(e -> dispose());
    resetButton.addActionListener(e -> resetToDefaults());

    buttonPanel.add(resetButton);
    buttonPanel.add(cancelButton);
    buttonPanel.add(okButton);

    return buttonPanel;
  }

  private void saveSettings() {
    try {
      ClassificationSettings newSettings = new ClassificationSettings(
          modelPathField.getText().trim(),
          selectedFeaturesPathField.getText().trim(),
          labelMappingPathField.getText().trim(),
          classDetailsPathField.getText().trim()
      );

      // Validate the settings
      newSettings.validate();

      // Save to configuration manager
      configManager.saveClassificationSettings(newSettings);

      // Show success message
      JOptionPane.showMessageDialog(this,
          "Classification settings saved successfully!\n\n" + getConfigurationInfo(newSettings),
          "Settings Saved", JOptionPane.INFORMATION_MESSAGE);

      // Close dialog
      dispose();

    } catch (IllegalArgumentException e) {
      JOptionPane.showMessageDialog(this,
          "Invalid settings: " + e.getMessage(),
          "Validation Error", JOptionPane.ERROR_MESSAGE);
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this,
          "Error saving settings: " + e.getMessage(),
          "Save Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void resetToDefaults() {
    int confirm = JOptionPane.showConfirmDialog(this,
        "Are you sure you want to reset all settings to defaults?",
        "Confirm Reset", JOptionPane.YES_NO_OPTION);

    if (confirm == JOptionPane.YES_OPTION) {
      ClassificationSettings defaultSettings = ClassificationSettings.createDefault();

      modelPathField.setText(defaultSettings.modelPath());
      selectedFeaturesPathField.setText(defaultSettings.selectedFeaturesPath());
      labelMappingPathField.setText(defaultSettings.labelMappingPath());
      classDetailsPathField.setText(defaultSettings.classDetailsPath());

      JOptionPane.showMessageDialog(this,
          "Settings reset to defaults. Click 'Save & Close' to apply.",
          "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
    }
  }

  private String getConfigurationInfo() {
    return getConfigurationInfo(currentSettings);
  }

  private String getConfigurationInfo(ClassificationSettings settings) {
    StringBuilder info = new StringBuilder();
    info.append("Current Configuration:\n");
    info.append("─ Using Default Paths: ").append(settings.isUsingDefaults() ? "YES" : "NO").append("\n");
    info.append("─ Model: ").append(new File(settings.modelPath()).getName()).append("\n");
    info.append("─ Features: ").append(new File(settings.selectedFeaturesPath()).getName()).append("\n");
    info.append("─ Mapping: ").append(new File(settings.labelMappingPath()).getName()).append("\n");
    info.append("─ Classes: ").append(new File(settings.classDetailsPath()).getName());
    return info.toString();
  }
}
