package com.scipath.scipathj.ui.analysis.dialogs.settings;

import com.scipath.scipathj.analysis.config.ClassificationSettings;
import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.training.XGBoostModelBundle;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import org.kordamp.ikonli.swing.FontIcon;

public class ClassificationSettingsDialog extends JDialog {

  private ClassificationSettings currentSettings;
  private ConfigurationManager configManager;

  // UI components
  private JTextField jsonFileField;
  private JButton jsonBrowseButton;
  private JPanel modelInfoPanel;
  private JPanel previewContentPanel;
  private XGBoostModelBundle loadedBundle;

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
    setSize(900, 700);
    setLocationRelativeTo(getParent());

    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    // Title
    JPanel titlePanel = new JPanel(new BorderLayout());
    titlePanel.setBorder(UIUtils.createPadding(0, 0, UIConstants.MEDIUM_SPACING, 0));
    JLabel titleLabel = UIUtils.createBoldLabel("XGBoost Model Configuration", UIConstants.SUBTITLE_FONT_SIZE);
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

    titlePanel.add(titleLabel, BorderLayout.CENTER);
    contentPanel.add(titlePanel, BorderLayout.NORTH);

    // Model selection panel
    JPanel selectionPanel = createModelSelectionPanel();
    contentPanel.add(selectionPanel, BorderLayout.NORTH);

    // Model info panel (initially hidden)
    modelInfoPanel = createModelInfoPanel();
    modelInfoPanel.setVisible(false);
    contentPanel.add(modelInfoPanel, BorderLayout.CENTER);

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

  private JPanel createModelSelectionPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING,
                            UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING);
    gbc.anchor = GridBagConstraints.WEST;

    // Add JSON file setting
    addJsonFileSetting(panel, gbc);

    return panel;
  }

  private JPanel createModelInfoPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(getBorderColor(), 1),
        new EmptyBorder(20, 20, 20, 20)
    ));

    // Title
    JLabel titleLabel = UIUtils.createBoldLabel("Model Preview", UIConstants.LARGE_FONT_SIZE);
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    panel.add(titleLabel, BorderLayout.NORTH);

    // Create empty content panel that can be populated later
    previewContentPanel = new JPanel();
    previewContentPanel.setLayout(new BoxLayout(previewContentPanel, BoxLayout.Y_AXIS));
    previewContentPanel.setOpaque(false);
    previewContentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Create placeholder content
    JLabel placeholderLabel = new JLabel("No model bundle selected");
    placeholderLabel.setHorizontalAlignment(SwingConstants.CENTER);
    placeholderLabel.setForeground(getTextSecondaryColor());
    previewContentPanel.add(placeholderLabel);

    JScrollPane scrollPane = new JScrollPane(previewContentPanel);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setBorder(null);
    scrollPane.setPreferredSize(new Dimension(-1, 280));
    panel.add(scrollPane, BorderLayout.CENTER);

    return panel;
  }

  private void displayModelPreview(XGBoostModelBundle bundle) {
    if (modelInfoPanel == null || previewContentPanel == null) return;

    // Clear existing content
    previewContentPanel.removeAll();

    // Title and description
    if (bundle.modelInfo != null) {
      if (bundle.modelInfo.title != null && !bundle.modelInfo.title.trim().isEmpty()) {
        JLabel titleLabel = new JLabel(bundle.modelInfo.title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(getPrimaryColor());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewContentPanel.add(titleLabel);
      }

      if (bundle.modelInfo.description != null && !bundle.modelInfo.description.trim().isEmpty()) {
        JLabel descLabel = new JLabel("<html><div style='width: 400px;'>" + bundle.modelInfo.description + "</div></html>");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descLabel.setBorder(new EmptyBorder(5, 0, 15, 0));
        previewContentPanel.add(descLabel);
      }
    }

    // Classes with colors
    if (bundle.labelMetadata != null && bundle.labelMetadata.classDetails != null && !bundle.labelMetadata.classDetails.isEmpty()) {
      JLabel classesHeader = new JLabel("Available Classes:");
      classesHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
      classesHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
      classesHeader.setBorder(new EmptyBorder(0, 0, 8, 0));
      previewContentPanel.add(classesHeader);

      JPanel classesPanel = new JPanel();
      classesPanel.setLayout(new BoxLayout(classesPanel, BoxLayout.Y_AXIS));
      classesPanel.setOpaque(false);
      classesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

      for (Map.Entry<Integer, XGBoostModelBundle.ClassDetail> entry : bundle.labelMetadata.classDetails.entrySet()) {
        XGBoostModelBundle.ClassDetail classDetail = entry.getValue();
        Color classColor = Color.decode(classDetail.color);

        JPanel classPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        classPanel.setOpaque(false);

        // Color square
        JPanel colorSquare = new JPanel();
        colorSquare.setPreferredSize(new Dimension(20, 20));
        colorSquare.setBackground(classColor);
        colorSquare.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        classPanel.add(colorSquare);

        // Class name and ID
        String classText = classDetail.name;
        if (classDetail.id >= 0) {
          classText += " (ID: " + classDetail.id + ")";
        }
        JLabel classNameLabel = new JLabel(classText);
        classNameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        classPanel.add(classNameLabel);

        classesPanel.add(classPanel);
      }

      previewContentPanel.add(classesPanel);
      previewContentPanel.add(Box.createVerticalStrut(15));
    }

    // Performance metrics
    if (bundle.evaluationResults != null && bundle.evaluationResults.overallMetrics != null && !bundle.evaluationResults.overallMetrics.isEmpty()) {
      JLabel metricsHeader = new JLabel("Performance Metrics:");
      metricsHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
      metricsHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
      metricsHeader.setBorder(new EmptyBorder(0, 0, 8, 0));
      previewContentPanel.add(metricsHeader);

      JPanel metricsPanel = new JPanel(new GridLayout(0, 2, 15, 5));
      metricsPanel.setOpaque(false);
      metricsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

      for (Map.Entry<String, Double> entry : bundle.evaluationResults.overallMetrics.entrySet()) {
        // Format metric name (capitalize first letter, replace underscores)
        String metricName = entry.getKey().replace("_", " ").toLowerCase();
        metricName = metricName.substring(0, 1).toUpperCase() + metricName.substring(1);

        JLabel nameLabel = new JLabel(metricName + ":");
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        nameLabel.setForeground(getTextSecondaryColor());

        String valueStr = String.format("%.3f", entry.getValue());
        if (metricName.toLowerCase().contains("accuracy")) {
          valueStr = String.format("%.1f%%", entry.getValue() * 100);
        }

        JLabel valueLabel = new JLabel(valueStr);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        valueLabel.setForeground(getSuccessColor());

        metricsPanel.add(nameLabel);
        metricsPanel.add(valueLabel);
      }

      previewContentPanel.add(metricsPanel);
    }

    // Refresh the panel
    previewContentPanel.revalidate();
    previewContentPanel.repaint();
    modelInfoPanel.setVisible(true);
    
    // Force the entire dialog to refresh
    this.revalidate();
    this.repaint();
    
    // Debug logging
    System.out.println("Model preview displayed successfully for: " +
        (bundle.modelInfo != null ? bundle.modelInfo.title : "Unknown model"));
  }

  private void addModelInfoRow(JPanel panel, GridBagConstraints gbc, String label, String value) {
    JLabel labelComp = new JLabel(label);
    labelComp.setFont(new Font("Segoe UI", Font.BOLD, 12));
    labelComp.setForeground(getPrimaryColor());

    String displayValue = value != null && !value.isEmpty() ? value : "Not specified";
    JLabel valueComp = new JLabel(displayValue);
    valueComp.setFont(new Font("Segoe UI", Font.PLAIN, 12));

    panel.add(labelComp, gbc);
    gbc.gridx = 1;
    panel.add(valueComp, gbc);
  }

  /**
   * Safely format a numeric value that might be Integer, Double, or other types
   * @param value The object value to format
   * @param format The printf format string
   * @return Formatted string or "N/A" if not a number
   */
  private String formatNumberValue(Object value, String format) {
    if (value instanceof Number) {
      return String.format(format, ((Number) value).doubleValue());
    } else if (value instanceof String && !"N/A".equals(value)) {
      try {
        // Try to parse as number
        double num = Double.parseDouble((String) value);
        return String.format(format, num);
      } catch (NumberFormatException e) {
        return "N/A";
      }
    } else {
      return "N/A";
    }
  }

  private JButton createSaveButton() {
    JButton saveButton = UIUtils.createStandardButton("Save Model Configuration", null);
    saveButton.setPreferredSize(new Dimension(200, 30));
    saveButton.addActionListener(e -> saveSettings());
    return saveButton;
  }



  private void addJsonFileSetting(JPanel panel, GridBagConstraints gbc) {
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

    FontIcon icon = FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.FILE_CODE,
                                20, getPrimaryColor());
    JPanel labelWithIcon = new JPanel(new FlowLayout(FlowLayout.LEFT));
    labelWithIcon.setOpaque(false);
    labelWithIcon.add(new JLabel("Select XGBoost Model ZIP Bundle"));
    labelWithIcon.add(new JLabel("<html>&nbsp;&nbsp;</html>")); // Spacing
    labelWithIcon.add(new JLabel(icon));
    headerPanel.add(labelWithIcon);

    // Description
    JLabel descLabel = new JLabel("<html><div style='width: 450px; color: #666;'>Choose a ZIP file containing the XGBoost model bundle. The ZIP contains metadata.json with configuration and model.ubj with the trained model in UBJSON format for optimal storage and performance.</div></html>");
    descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    descLabel.setForeground(getTextSecondaryColor());
    descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    descLabel.setBorder(new EmptyBorder(5, 0, 15, 0));
    headerPanel.add(descLabel);

    zipPanel.add(headerPanel, BorderLayout.NORTH);

    // Input panel
    JPanel inputPanel = new JPanel(new BorderLayout(12, 0));
    inputPanel.setOpaque(false);

    jsonFileField = new JTextField();
    jsonFileField.setPreferredSize(new Dimension(400, 40));
    jsonFileField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    jsonFileField.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(getBorderColor(), 1),
        new EmptyBorder(8, 12, 8, 12)
    ));

    jsonBrowseButton = UIUtils.createStandardButton("Browse JSON Files...", null);
    jsonBrowseButton.setPreferredSize(new Dimension(140, 40));
    jsonBrowseButton.addActionListener(e -> browseForJsonFile());

    inputPanel.add(jsonFileField, BorderLayout.CENTER);
    inputPanel.add(jsonBrowseButton, BorderLayout.EAST);

    zipPanel.add(inputPanel, BorderLayout.CENTER);

    panel.add(zipPanel, gbc);
  }


  private void browseForJsonFile() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Select XGBoost Model ZIP Bundle");
    fileChooser.setAcceptAllFileFilterUsed(true);

    // Set ZIP file filter
    fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
      @Override
      public boolean accept(java.io.File f) {
        return f.isDirectory() ||
               f.getName().toLowerCase().endsWith(".zip");
      }

      @Override
      public String getDescription() {
        return "ZIP Bundles (*.zip)";
      }
    });

    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      java.io.File selectedFile = fileChooser.getSelectedFile();
      jsonFileField.setText(selectedFile.getAbsolutePath());

      // Load and display model information
      try {
        System.out.println("Loading bundle from: " + selectedFile.getAbsolutePath());
        loadedBundle = loadBundleInfo(selectedFile.getAbsolutePath());
        if (loadedBundle != null) {
          System.out.println("Bundle loaded successfully, displaying preview...");
          displayModelPreview(loadedBundle);
        } else {
          System.out.println("Bundle is null, cannot display preview");
        }
      } catch (Exception e) {
        JOptionPane.showMessageDialog(this,
            "Selected file is not a valid XGBoost model bundle:\n" + e.getMessage(),
            "Invalid Bundle", JOptionPane.ERROR_MESSAGE);
        loadedBundle = null;
        modelInfoPanel.setVisible(false);
      }
    }
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
      String bundlePath = jsonFileField.getText().trim();
      if (bundlePath.isEmpty() || !Files.exists(Paths.get(bundlePath)) || !bundlePath.endsWith(".zip")) {
        // Use defaults - no custom ZIP bundle selected
        ClassificationSettings newSettings = ClassificationSettings.createDefault();
        configManager.saveClassificationSettings(newSettings);
        JOptionPane.showMessageDialog(this,
            "Using default classifiers. No custom ZIP bundle selected.",
            "Settings Saved", JOptionPane.INFORMATION_MESSAGE);
      } else {
        // Validate ZIP bundle exists and can be loaded
        if (!isValidZipBundle(bundlePath)) {
          JOptionPane.showMessageDialog(this,
              "Selected file is not a valid XGBoost model bundle or cannot be loaded.\nPlease select a ZIP file created by SciPathJ's training process.",
              "Invalid Bundle", JOptionPane.ERROR_MESSAGE);
          return;
        }

        // Create settings pointing to the ZIP bundle
        // The classification system will detect this is a ZIP bundle and load appropriately
        ClassificationSettings newSettings = ClassificationSettings.withCustomModel(bundlePath);
        newSettings.validate();
        configManager.saveClassificationSettings(newSettings);

        // Load basic info from the bundle for display
        try {
          XGBoostModelBundle bundle = loadBundleInfo(bundlePath);
          JOptionPane.showMessageDialog(this,
              "ZIP bundle validated and settings saved successfully!\n\n" +
              "Title: " + bundle.modelInfo.title + "\n" +
              "Description: " + bundle.modelInfo.description + "\n" +
              "File: " + new File(bundlePath).getName(),
              "Settings Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
          JOptionPane.showMessageDialog(this,
              "ZIP bundle loaded and settings saved successfully!\n\n" + getConfigurationInfo(newSettings),
              "Settings Saved", JOptionPane.INFORMATION_MESSAGE);
        }
      }
      dispose();
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this,
          "Error saving settings: " + e.getMessage(),
          "Save Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Validate that the selected file is a valid XGBoost model bundle (ZIP format)
   */
  private boolean isValidZipBundle(String zipPath) {
    try {
      XGBoostModelBundle bundle = loadBundleInfo(zipPath);
      return bundle != null &&
             bundle.modelInfo != null &&
             bundle.featureMetadata != null;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Load basic bundle information for validation/display (supports both ZIP and JSON formats)
   */
  private XGBoostModelBundle loadBundleInfo(String bundlePath) throws IOException {
    try {
      // Log what we're trying to load
      java.util.logging.Logger logger = java.util.logging.Logger.getLogger("XGBoostSettings");
      logger.info("Attempting to load XGBoost bundle from: " + bundlePath);

      File bundleFile = new File(bundlePath);
      if (!bundleFile.exists()) {
        throw new IOException("Bundle file does not exist: " + bundlePath);
      }
      logger.info("Bundle file exists, size: " + bundleFile.length() + " bytes");

      ObjectMapper mapper = new ObjectMapper();

      // Configure ObjectMapper with comprehensive error handling
      mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, false);
      mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
      mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_FLOAT_AS_INT, true);
      mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      // Determine if this is ZIP or JSON based on file extension
      if (bundlePath.toLowerCase().endsWith(".zip")) {
        // Handle ZIP bundle - read metadata.json from ZIP
        logger.info("Detected ZIP bundle format, loading metadata.json from archive");

        ZipFile zipFile = null;
        try {
          zipFile = new ZipFile(bundlePath);
          ZipEntry metadataEntry = zipFile.getEntry("metadata.json");

          if (metadataEntry == null) {
            throw new IOException("No metadata.json found in ZIP bundle");
          }

          // Read metadata.json from ZIP
          try (java.io.InputStream metadataStream = zipFile.getInputStream(metadataEntry)) {
            XGBoostModelBundle bundle = mapper.readValue(metadataStream, XGBoostModelBundle.class);
            logger.info("Successfully loaded ZIP bundle: " + (bundle.modelInfo != null ? bundle.modelInfo.title : "Unknown"));
            return bundle;
          }

        } finally {
          if (zipFile != null) {
            zipFile.close();
          }
        }

      } else if (bundlePath.toLowerCase().endsWith(".json")) {
        // Handle legacy JSON bundle
        logger.info("Detected legacy JSON bundle format");

        XGBoostModelBundle bundle = mapper.readValue(bundleFile, XGBoostModelBundle.class);

        // Handle backwards compatibility - copy root-level fields to nested structure if present
        if (bundle.modelInfo == null) {
          bundle.modelInfo = new XGBoostModelBundle.ModelInfo();
        }

        if (bundle.modelTitle != null && !bundle.modelTitle.trim().isEmpty()) {
          bundle.modelInfo.title = bundle.modelTitle;
        }
        if (bundle.modelDescription != null && !bundle.modelDescription.trim().isEmpty()) {
          bundle.modelInfo.description = bundle.modelDescription;
        }
        if (bundle.modelVersion != null && !bundle.modelVersion.trim().isEmpty()) {
          bundle.modelInfo.version = bundle.modelVersion;
        }

        logger.info("Successfully loaded legacy JSON bundle for: " + (bundle.modelInfo != null ? bundle.modelInfo.title : "Unknown"));
        return bundle;

      } else {
        throw new IOException("Unsupported bundle format. Expected .zip or .json file, got: " + bundlePath);
      }

    } catch (Exception e) {
      java.util.logging.Logger loggerInstance = java.util.logging.Logger.getLogger("XGBoostSettings");
      loggerInstance.severe("Failed to load XGBoost bundle: " + e.getMessage());
      loggerInstance.severe("Exception type: " + e.getClass().getName());
      throw new IOException("Failed to load XGBoost bundle: " + e.getMessage(), e);
    }
  }


  private void resetToDefaults() {
    int confirm = JOptionPane.showConfirmDialog(this,
        "Are you sure you want to reset to default classifiers?",
        "Confirm Reset", JOptionPane.YES_NO_OPTION);

    if (confirm == JOptionPane.YES_OPTION) {
      jsonFileField.setText("");
      JOptionPane.showMessageDialog(this,
          "Reset to defaults. Click 'Save & Close' to apply.",
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
