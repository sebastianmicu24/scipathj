package com.scipath.scipathj.ui.dialogs.settings;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.config.MainSettings;
import com.scipath.scipathj.core.config.VesselSegmentationSettings;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings dialog for Vessel Segmentation step.
 * Provides UI controls for configuring vessel segmentation parameters
 * with persistence and default value management.
 */
public class VesselSegmentationSettingsDialog extends JDialog {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(VesselSegmentationSettingsDialog.class);

  // Settings instances
  private final VesselSegmentationSettings settings;
  private final MainSettings mainSettings;
  private final ConfigurationManager configManager;

  // UI Components
  private JSlider thresholdSlider;
  private JLabel thresholdValueLabel;
  private JSpinner minRoiSizeSpinner;
  private JSpinner maxRoiSizeSpinner;
  private JSpinner gaussianBlurSigmaSpinner;
  private JCheckBox morphologicalClosingCheckBox;

  // Result flag
  private boolean settingsChanged = false;

  public VesselSegmentationSettingsDialog(Frame parent, ConfigurationManager configurationManager) {
    super(parent, "Vessel Segmentation Settings", true);
    this.settings = VesselSegmentationSettings.getInstance();
    this.mainSettings = MainSettings.getInstance();
    this.configManager = configurationManager;

    // Load settings from file first
    configManager.loadVesselSegmentationSettings(settings);
    configManager.loadMainSettings(mainSettings);

    initializeDialog();
    loadCurrentSettings();
  }

  private void initializeDialog() {
    setSize(500, 400);
    setLocationRelativeTo(getParent());
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    // Title
    JLabel titleLabel =
        UIUtils.createBoldLabel("Vessel Segmentation Settings", UIConstants.SUBTITLE_FONT_SIZE);
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    contentPanel.add(titleLabel, BorderLayout.NORTH);

    // Settings content
    JPanel settingsPanel = createSettingsPanel();
    JScrollPane scrollPane = new JScrollPane(settingsPanel);
    scrollPane.setBorder(null);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    contentPanel.add(scrollPane, BorderLayout.CENTER);

    // Button panel
    JPanel buttonPanel = createButtonPanel();
    contentPanel.add(buttonPanel, BorderLayout.SOUTH);

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

    // Threshold Value
    gbc.gridx = 0;
    gbc.gridy = 0;
    JLabel thresholdLabel =
        UIUtils.createLabel("Threshold Value:", UIConstants.NORMAL_FONT_SIZE, null);
    thresholdLabel.setToolTipText("Pixel intensity threshold for vessel detection (0-255)");
    panel.add(thresholdLabel, gbc);

    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;

    // Create a panel to hold the slider and value label
    JPanel thresholdPanel = new JPanel(new BorderLayout());

    thresholdSlider = new JSlider(0, 255, VesselSegmentationSettings.DEFAULT_THRESHOLD);
    thresholdSlider.setToolTipText("Pixel intensity threshold for vessel detection (0-255)");
    thresholdSlider.setMajorTickSpacing(50);
    thresholdSlider.setMinorTickSpacing(10);
    thresholdSlider.setPaintTicks(true);
    thresholdSlider.setPaintLabels(true);

    thresholdValueLabel = new JLabel(String.valueOf(VesselSegmentationSettings.DEFAULT_THRESHOLD));
    thresholdValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
    thresholdValueLabel.setPreferredSize(new Dimension(40, 20));

    // Add change listener to update the value label
    thresholdSlider.addChangeListener(
        e -> {
          thresholdValueLabel.setText(String.valueOf(thresholdSlider.getValue()));
        });

    thresholdPanel.add(thresholdSlider, BorderLayout.CENTER);
    thresholdPanel.add(thresholdValueLabel, BorderLayout.EAST);

    panel.add(thresholdPanel, gbc);

    // Min ROI Size
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    String scaleUnit = mainSettings.getScaleUnit();
    JLabel minRoiSizeLabel =
        UIUtils.createLabel(
            "Min ROI Size (" + scaleUnit + "²):", UIConstants.NORMAL_FONT_SIZE, null);
    minRoiSizeLabel.setToolTipText(
        "Minimum vessel area in " + scaleUnit + "² (converted to pixels automatically)");
    panel.add(minRoiSizeLabel, gbc);

    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    // Convert default pixel value to micrometers for display
    double defaultMinSizeInMicrometers =
        mainSettings.pixelsToMicrometers(VesselSegmentationSettings.DEFAULT_MIN_ROI_SIZE);
    minRoiSizeSpinner =
        new JSpinner(new SpinnerNumberModel(defaultMinSizeInMicrometers, 0.0, 10000.0, 0.1));
    minRoiSizeSpinner.setToolTipText(
        "Minimum vessel area in " + scaleUnit + "² (converted to pixels automatically)");
    panel.add(minRoiSizeSpinner, gbc);

    // Max ROI Size
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    JLabel maxRoiSizeLabel =
        UIUtils.createLabel(
            "Max ROI Size (" + scaleUnit + "²):", UIConstants.NORMAL_FONT_SIZE, null);
    maxRoiSizeLabel.setToolTipText(
        "Maximum vessel area in " + scaleUnit + "² (converted to pixels automatically)");
    panel.add(maxRoiSizeLabel, gbc);

    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    // Convert default pixel value to micrometers for display
    double defaultMaxSizeInMicrometers =
        mainSettings.pixelsToMicrometers(VesselSegmentationSettings.DEFAULT_MAX_ROI_SIZE);
    maxRoiSizeSpinner =
        new JSpinner(new SpinnerNumberModel(defaultMaxSizeInMicrometers, 1.0, 100000.0, 10.0));
    maxRoiSizeSpinner.setToolTipText(
        "Maximum vessel area in " + scaleUnit + "² (converted to pixels automatically)");
    panel.add(maxRoiSizeSpinner, gbc);

    // Gaussian Blur Sigma
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    JLabel gaussianBlurSigmaLabel =
        UIUtils.createLabel("Gaussian Blur Sigma:", UIConstants.NORMAL_FONT_SIZE, null);
    gaussianBlurSigmaLabel.setToolTipText("Standard deviation for preprocessing blur");
    panel.add(gaussianBlurSigmaLabel, gbc);

    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gaussianBlurSigmaSpinner =
        new JSpinner(
            new SpinnerNumberModel(
                VesselSegmentationSettings.DEFAULT_GAUSSIAN_BLUR_SIGMA, 0.0, 10.0, 0.1));
    gaussianBlurSigmaSpinner.setToolTipText("Standard deviation for preprocessing blur");
    panel.add(gaussianBlurSigmaSpinner, gbc);

    // Morphological Operations
    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    JLabel morphologicalLabel =
        UIUtils.createLabel("Apply Morphological Closing:", UIConstants.NORMAL_FONT_SIZE, null);
    morphologicalLabel.setToolTipText("Apply morphological closing to fill gaps");
    panel.add(morphologicalLabel, gbc);

    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    morphologicalClosingCheckBox = new JCheckBox();
    morphologicalClosingCheckBox.setSelected(
        VesselSegmentationSettings.DEFAULT_APPLY_MORPHOLOGICAL_CLOSING);
    morphologicalClosingCheckBox.setToolTipText("Apply morphological closing to fill gaps");
    panel.add(morphologicalClosingCheckBox, gbc);

    return panel;
  }

  private JPanel createButtonPanel() {
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    // Create standard Swing buttons to ensure they're visible
    JButton resetButton = new JButton("Reset to Defaults");
    JButton cancelButton = new JButton("Cancel");
    JButton saveButton = new JButton("Save Settings");

    // Set preferred sizes to ensure buttons are visible
    Dimension buttonSize = new Dimension(120, 30);
    resetButton.setPreferredSize(new Dimension(140, 30));
    cancelButton.setPreferredSize(buttonSize);
    saveButton.setPreferredSize(new Dimension(130, 30));

    // Add action listeners
    resetButton.addActionListener(new ResetToDefaultsAction());
    cancelButton.addActionListener(e -> dispose());
    saveButton.addActionListener(new SaveSettingsAction());

    // Make the Save button more prominent
    saveButton.setBackground(new java.awt.Color(70, 130, 180));
    saveButton.setForeground(java.awt.Color.WHITE);
    saveButton.setOpaque(true);
    saveButton.setBorderPainted(true);

    // Add buttons to panel
    buttonPanel.add(resetButton);
    buttonPanel.add(cancelButton);
    buttonPanel.add(saveButton);

    return buttonPanel;
  }

  /**
   * Load current settings into the UI components.
   */
  private void loadCurrentSettings() {
    try {
      thresholdSlider.setValue(settings.getThreshold());
      thresholdValueLabel.setText(String.valueOf(settings.getThreshold()));

      // Convert pixel values to micrometers for display
      double minRoiSizeInMicrometers = mainSettings.pixelsToMicrometers(settings.getMinRoiSize());
      double maxRoiSizeInMicrometers = mainSettings.pixelsToMicrometers(settings.getMaxRoiSize());

      minRoiSizeSpinner.setValue(minRoiSizeInMicrometers);
      maxRoiSizeSpinner.setValue(maxRoiSizeInMicrometers);
      gaussianBlurSigmaSpinner.setValue(settings.getGaussianBlurSigma());
      morphologicalClosingCheckBox.setSelected(settings.isApplyMorphologicalClosing());

      LOGGER.debug("Loaded current settings into UI components: {}", settings.toString());
    } catch (Exception e) {
      LOGGER.error("Error loading settings into UI components", e);
      // Load defaults if there's an error
      loadDefaultSettings();
    }
  }

  /**
   * Load default settings into the UI components.
   */
  private void loadDefaultSettings() {
    thresholdSlider.setValue(VesselSegmentationSettings.DEFAULT_THRESHOLD);
    thresholdValueLabel.setText(String.valueOf(VesselSegmentationSettings.DEFAULT_THRESHOLD));

    // Convert default pixel values to micrometers for display
    double defaultMinSizeInMicrometers =
        mainSettings.pixelsToMicrometers(VesselSegmentationSettings.DEFAULT_MIN_ROI_SIZE);
    double defaultMaxSizeInMicrometers =
        mainSettings.pixelsToMicrometers(VesselSegmentationSettings.DEFAULT_MAX_ROI_SIZE);

    minRoiSizeSpinner.setValue(defaultMinSizeInMicrometers);
    maxRoiSizeSpinner.setValue(defaultMaxSizeInMicrometers);
    gaussianBlurSigmaSpinner.setValue(VesselSegmentationSettings.DEFAULT_GAUSSIAN_BLUR_SIGMA);
    morphologicalClosingCheckBox.setSelected(
        VesselSegmentationSettings.DEFAULT_APPLY_MORPHOLOGICAL_CLOSING);

    LOGGER.debug("Loaded default settings into UI components");
  }

  /**
   * Validate the current input values.
   *
   * @return true if all values are valid
   */
  private boolean validateInputs() {
    try {
      int threshold = thresholdSlider.getValue();
      double minRoiSizeInMicrometers = (Double) minRoiSizeSpinner.getValue();
      double maxRoiSizeInMicrometers = (Double) maxRoiSizeSpinner.getValue();
      double gaussianBlurSigma = (Double) gaussianBlurSigmaSpinner.getValue();

      if (threshold < 0 || threshold > 255) {
        showErrorMessage("Threshold must be between 0 and 255");
        return false;
      }

      if (minRoiSizeInMicrometers < 0) {
        showErrorMessage("Minimum ROI size must be non-negative");
        return false;
      }

      if (maxRoiSizeInMicrometers < 0) {
        showErrorMessage("Maximum ROI size must be non-negative");
        return false;
      }

      if (minRoiSizeInMicrometers > maxRoiSizeInMicrometers) {
        showErrorMessage("Minimum ROI size cannot be greater than maximum ROI size");
        return false;
      }

      if (gaussianBlurSigma < 0) {
        showErrorMessage("Gaussian blur sigma must be non-negative");
        return false;
      }

      return true;

    } catch (Exception e) {
      showErrorMessage("Invalid input values: " + e.getMessage());
      return false;
    }
  }

  /**
   * Show an error message dialog.
   */
  private void showErrorMessage(String message) {
    JOptionPane.showMessageDialog(this, message, "Invalid Input", JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Check if settings were changed and saved.
   *
   * @return true if settings were changed
   */
  public boolean isSettingsChanged() {
    return settingsChanged;
  }

  /**
   * Action listener for saving settings.
   */
  private class SaveSettingsAction implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (!validateInputs()) {
        return;
      }

      try {
        // Convert micrometer values back to pixels for storage
        double minRoiSizeInMicrometers = (Double) minRoiSizeSpinner.getValue();
        double maxRoiSizeInMicrometers = (Double) maxRoiSizeSpinner.getValue();
        double minRoiSizeInPixels = mainSettings.micrometersToPixels(minRoiSizeInMicrometers);
        double maxRoiSizeInPixels = mainSettings.micrometersToPixels(maxRoiSizeInMicrometers);

        // Update settings with new values
        settings.setThreshold(thresholdSlider.getValue());
        settings.setMinRoiSize(minRoiSizeInPixels);
        settings.setMaxRoiSize(maxRoiSizeInPixels);
        settings.setGaussianBlurSigma((Double) gaussianBlurSigmaSpinner.getValue());
        settings.setApplyMorphologicalClosing(morphologicalClosingCheckBox.isSelected());

        // Save to file
        configManager.saveVesselSegmentationSettings(settings);

        settingsChanged = true;
        LOGGER.info("Vessel segmentation settings saved successfully");

        dispose();

      } catch (Exception ex) {
        LOGGER.error("Failed to save vessel segmentation settings", ex);
        showErrorMessage("Failed to save settings: " + ex.getMessage());
      }
    }
  }

  /**
   * Action listener for resetting to defaults.
   */
  private class ResetToDefaultsAction implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      int result =
          JOptionPane.showConfirmDialog(
              VesselSegmentationSettingsDialog.this,
              "Are you sure you want to reset all settings to their default values?",
              "Reset to Defaults",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE);

      if (result == JOptionPane.YES_OPTION) {
        loadDefaultSettings();
        LOGGER.info("UI components reset to default values");
      }
    }
  }
}
