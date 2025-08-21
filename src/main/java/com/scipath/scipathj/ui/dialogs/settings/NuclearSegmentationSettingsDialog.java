package com.scipath.scipathj.ui.dialogs.settings;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.config.NuclearSegmentationSettings;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings dialog for Nuclear Segmentation step using StarDist.
 * This dialog allows users to configure all StarDist parameters for nucleus detection.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class NuclearSegmentationSettingsDialog extends JDialog {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(NuclearSegmentationSettingsDialog.class);

  private NuclearSegmentationSettings settings;
  private final ConfigurationManager configManager;

  // UI Components
  private JComboBox<String> modelChoiceCombo;
  private JCheckBox normalizeInputCheck;
  private JSlider percentileBottomSlider;
  private JSlider percentileTopSlider;
  private JSlider probThreshSlider;
  private JSlider nmsThreshSlider;
  private JSpinner nTilesSpinner;
  private JSpinner excludeBoundarySpinner;
  private JSpinner minNucleusSizeSpinner;
  private JSpinner maxNucleusSizeSpinner;
  private JCheckBox verboseCheck;
  private JCheckBox showProgressCheck;
  private JCheckBox showProbDistCheck;

  private boolean settingsChanged = false;

  public NuclearSegmentationSettingsDialog(
      Frame parent, ConfigurationManager configurationManager) {
    super(parent, "Nuclear Segmentation Settings", true);
    this.configManager = configurationManager;
    this.settings = configManager.loadNuclearSegmentationSettings();
    initializeDialog();
    loadCurrentSettings();
  }

  private void initializeDialog() {
    setSize(500, 600);
    setLocationRelativeTo(getParent());
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    JLabel titleLabel =
        UIUtils.createBoldLabel(
            "Nuclear Segmentation Settings (StarDist)", UIConstants.SUBTITLE_FONT_SIZE);
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    contentPanel.add(titleLabel, BorderLayout.NORTH);

    JPanel settingsPanel = createSettingsPanel();
    JScrollPane scrollPane = new JScrollPane(settingsPanel);
    scrollPane.setBorder(null);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    contentPanel.add(scrollPane, BorderLayout.CENTER);

    contentPanel.add(createButtonPanel(), BorderLayout.SOUTH);
    add(contentPanel);
  }

  private JPanel createSettingsPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets =
        new Insets(
            UIConstants.SMALL_SPACING,
            UIConstants.MEDIUM_SPACING,
            UIConstants.SMALL_SPACING,
            UIConstants.MEDIUM_SPACING);
    gbc.anchor = GridBagConstraints.WEST;

    // StarDist Model Selection
    addModelChoiceRow(panel, gbc);

    // Normalization Settings
    addSeparator(panel, gbc, "Normalization Settings");
    addNormalizationRows(panel, gbc);

    // Detection Thresholds
    addSeparator(panel, gbc, "Detection Thresholds");
    addThresholdRows(panel, gbc);

    // Processing Settings
    addSeparator(panel, gbc, "Processing Settings");
    addProcessingRows(panel, gbc);

    // Size Filtering
    addSeparator(panel, gbc, "Size Filtering");
    addSizeFilteringRows(panel, gbc);

    // Advanced Options - Hidden by default as requested
    // Uncomment the lines below to show advanced options
    // addSeparator(panel, gbc, "Advanced Options");
    addAdvancedRows(panel, gbc);

    return panel;
  }

  private void addModelChoiceRow(JPanel panel, GridBagConstraints gbc) {
    gbc.gridx = 0;
    gbc.gridy++;
    JLabel label = UIUtils.createLabel("StarDist Model:", UIConstants.NORMAL_FONT_SIZE, null);
    label.setToolTipText("StarDist model for nuclear detection");
    panel.add(label, gbc);

    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    String[] models = {"Versatile (H&E nuclei)", "2D_versatile_fluo", "2D_paper_dsb2018"};
    modelChoiceCombo = new JComboBox<>(models);
    modelChoiceCombo.setToolTipText("Select the StarDist model appropriate for your images");
    panel.add(modelChoiceCombo, gbc);

    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
  }

  private void addNormalizationRows(JPanel panel, GridBagConstraints gbc) {
    // Normalize Input checkbox
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    normalizeInputCheck = new JCheckBox("Normalize Input");
    normalizeInputCheck.setToolTipText("Enable input normalization using percentiles");
    panel.add(normalizeInputCheck, gbc);
    gbc.gridwidth = 1;

    // Percentile Bottom
    addSliderRow(
        panel,
        gbc,
        "Bottom Percentile:",
        0, 100, 1,
        "Bottom percentile for normalization (0-100)");
    percentileBottomSlider = (JSlider) ((JPanel) panel.getComponent(panel.getComponentCount() - 1)).getComponent(0);

    // Percentile Top
    addSliderRow(
        panel,
        gbc,
        "Top Percentile:",
        0, 100, 99,
        "Top percentile for normalization (0-100)");
    percentileTopSlider = (JSlider) ((JPanel) panel.getComponent(panel.getComponentCount() - 1)).getComponent(0);
  }

  private void addThresholdRows(JPanel panel, GridBagConstraints gbc) {
    // Probability Threshold
    addSliderRow(
        panel,
        gbc,
        "Probability Threshold:",
        0, 100, 50,
        "Detection probability threshold (0.0-1.0)");
    probThreshSlider = (JSlider) ((JPanel) panel.getComponent(panel.getComponentCount() - 1)).getComponent(0);

    // Overlap Threshold (formerly NMS Threshold)
    addSliderRow(
        panel,
        gbc,
        "Overlap Threshold:",
        0, 100, 40,
        "Overlap threshold for nucleus detection (0.0-1.0)");
    nmsThreshSlider = (JSlider) ((JPanel) panel.getComponent(panel.getComponentCount() - 1)).getComponent(0);
  }

  private void addProcessingRows(JPanel panel, GridBagConstraints gbc) {
    // Number of Tiles
    addSpinnerRow(
        panel,
        gbc,
        "Number of Tiles:",
        new SpinnerNumberModel(1, 1, 16, 1),
        "Number of tiles for processing large images");
    nTilesSpinner = (JSpinner) panel.getComponent(panel.getComponentCount() - 1);

    // Exclude Boundary
    addSpinnerRow(
        panel,
        gbc,
        "Exclude Boundary:",
        new SpinnerNumberModel(2, 0, 50, 1),
        "Exclude nuclei within this distance from image boundary (pixels)");
    excludeBoundarySpinner = (JSpinner) panel.getComponent(panel.getComponentCount() - 1);
  }

  private void addSizeFilteringRows(JPanel panel, GridBagConstraints gbc) {
    // Min Nucleus Size
    addSpinnerRow(
        panel,
        gbc,
        "Min Nucleus Size:",
        new SpinnerNumberModel(
            Double.valueOf(10.0),
            Double.valueOf(1.0),
            Double.valueOf(10000.0),
            Double.valueOf(1.0)),
        "Minimum nucleus area in pixels");
    minNucleusSizeSpinner = (JSpinner) panel.getComponent(panel.getComponentCount() - 1);

    // Max Nucleus Size
    addSpinnerRow(
        panel,
        gbc,
        "Max Nucleus Size:",
        new SpinnerNumberModel(
            Double.valueOf(1000.0),
            Double.valueOf(1.0),
            Double.valueOf(100000.0),
            Double.valueOf(10.0)),
        "Maximum nucleus area in pixels");
    maxNucleusSizeSpinner = (JSpinner) panel.getComponent(panel.getComponentCount() - 1);
  }

  private void addAdvancedRows(JPanel panel, GridBagConstraints gbc) {
    // Verbose output
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    verboseCheck = new JCheckBox("Verbose Output");
    verboseCheck.setToolTipText("Enable verbose output during processing");
    // Hide advanced options by default as requested
    verboseCheck.setVisible(false);
    panel.add(verboseCheck, gbc);

    // Show progress
    gbc.gridy++;
    showProgressCheck = new JCheckBox("Show CSBDeep Progress");
    showProgressCheck.setToolTipText("Show CSBDeep progress during processing");
    // Hide advanced options by default as requested
    showProgressCheck.setVisible(false);
    panel.add(showProgressCheck, gbc);

    // Show probability and distance maps
    gbc.gridy++;
    showProbDistCheck = new JCheckBox("Show Probability & Distance Maps");
    showProbDistCheck.setToolTipText("Display probability and distance maps (for debugging)");
    // Hide advanced options by default as requested
    showProbDistCheck.setVisible(false);
    panel.add(showProbDistCheck, gbc);

    gbc.gridwidth = 1;
  }

  private void addSeparator(JPanel panel, GridBagConstraints gbc, String title) {
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    JPanel separatorPanel = new JPanel(new BorderLayout());
    separatorPanel.setBorder(
        BorderFactory.createEmptyBorder(
            UIConstants.MEDIUM_SPACING, 0, UIConstants.SMALL_SPACING, 0));

    JLabel titleLabel = UIUtils.createBoldLabel(title, UIConstants.NORMAL_FONT_SIZE);
    titleLabel.setForeground(UIConstants.ACCENT_COLOR);
    separatorPanel.add(titleLabel, BorderLayout.WEST);

    JSeparator separator = new JSeparator();
    separatorPanel.add(separator, BorderLayout.CENTER);

    panel.add(separatorPanel, gbc);

    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.NONE;
  }

  private void addSpinnerRow(
      JPanel panel,
      GridBagConstraints gbc,
      String labelText,
      SpinnerNumberModel model,
      String tooltip) {
    gbc.gridx = 0;
    gbc.gridy++;
    JLabel label = UIUtils.createLabel(labelText, UIConstants.NORMAL_FONT_SIZE, null);
    label.setToolTipText(tooltip);
    panel.add(label, gbc);

    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    JSpinner spinner = new JSpinner(model);
    spinner.setToolTipText(tooltip);

    // Format spinner display
    if (model.getValue() instanceof Float || model.getValue() instanceof Double) {
      JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0.000");
      spinner.setEditor(editor);
    }

    panel.add(spinner, gbc);

    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
  }

  private void addSliderRow(
      JPanel panel,
      GridBagConstraints gbc,
      String labelText,
      int min,
      int max,
      int value,
      String tooltip) {
    gbc.gridx = 0;
    gbc.gridy++;
    JLabel label = UIUtils.createLabel(labelText, UIConstants.NORMAL_FONT_SIZE, null);
    label.setToolTipText(tooltip);
    panel.add(label, gbc);

    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;

    // Create slider
    JSlider slider = new JSlider(min, max, value);
    slider.setToolTipText(tooltip);
    slider.setMajorTickSpacing((max - min) / 5);
    slider.setMinorTickSpacing((max - min) / 20);
    slider.setPaintTicks(true);
    slider.setPaintLabels(true);
    slider.setSnapToTicks(false);

    // Create a panel to hold slider and value label
    JPanel sliderPanel = new JPanel(new BorderLayout());
    sliderPanel.add(slider, BorderLayout.CENTER);

    // Add value label
    JLabel valueLabel = new JLabel(String.valueOf(value));
    valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
    valueLabel.setFont(valueLabel.getFont().deriveFont(UIConstants.SMALL_FONT_SIZE));
    sliderPanel.add(valueLabel, BorderLayout.SOUTH);

    // Add change listener to update label
    slider.addChangeListener(e -> {
      int sliderValue = slider.getValue();
      if (min == 0 && max == 100) {
        // Integer values for percentiles
        valueLabel.setText(String.valueOf(sliderValue));
      } else {
        // Float values for thresholds (divide by 100)
        valueLabel.setText(String.format("%.2f", sliderValue / 100.0));
      }
    });

    panel.add(sliderPanel, gbc);

    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
  }

  private void loadCurrentSettings() {
    modelChoiceCombo.setSelectedItem(settings.modelChoice());
    normalizeInputCheck.setSelected(settings.normalizeInput());
    percentileBottomSlider.setValue(Math.round(settings.percentileBottom()));
    percentileTopSlider.setValue(Math.round(settings.percentileTop()));
    probThreshSlider.setValue(Math.round(settings.probThresh() * 100));
    nmsThreshSlider.setValue(Math.round(settings.nmsThresh() * 100));
    nTilesSpinner.setValue(settings.nTiles());
    excludeBoundarySpinner.setValue(settings.excludeBoundary());
    minNucleusSizeSpinner.setValue(settings.minNucleusSize());
    maxNucleusSizeSpinner.setValue(settings.maxNucleusSize());
    verboseCheck.setSelected(settings.verbose());
    showProgressCheck.setSelected(settings.showCsbdeepProgress());
    showProbDistCheck.setSelected(settings.showProbAndDist());

    LOGGER.debug("Loaded current nuclear segmentation settings into dialog");
  }

  /**
   * Validate the current input values.
   *
   * @return true if all values are valid
   */
  private boolean validateInputs() {
    try {
      // Get values from sliders (convert from int to float where needed)
      float probThresh = probThreshSlider.getValue() / 100.0f;
      float nmsThresh = nmsThreshSlider.getValue() / 100.0f;
      float percentileBottom = percentileBottomSlider.getValue();
      float percentileTop = percentileTopSlider.getValue();
      double minNucleusSize = ((Number) minNucleusSizeSpinner.getValue()).doubleValue();
      double maxNucleusSize = ((Number) maxNucleusSizeSpinner.getValue()).doubleValue();

      if (probThresh < 0.0f || probThresh > 1.0f) {
        showErrorMessage("Probability threshold must be between 0.0 and 1.0");
        return false;
      }

      if (nmsThresh < 0.0f || nmsThresh > 1.0f) {
        showErrorMessage("Overlap threshold must be between 0.0 and 1.0");
        return false;
      }

      if (percentileBottom < 0.0f || percentileBottom > 100.0f) {
        showErrorMessage("Bottom percentile must be between 0.0 and 100.0");
        return false;
      }

      if (percentileTop < 0.0f || percentileTop > 100.0f) {
        showErrorMessage("Top percentile must be between 0.0 and 100.0");
        return false;
      }

      if (percentileBottom >= percentileTop) {
        showErrorMessage("Bottom percentile must be less than top percentile");
        return false;
      }

      if (minNucleusSize < 0) {
        showErrorMessage("Minimum nucleus size must be non-negative");
        return false;
      }

      if (maxNucleusSize < 0) {
        showErrorMessage("Maximum nucleus size must be non-negative");
        return false;
      }

      if (minNucleusSize > maxNucleusSize) {
        showErrorMessage("Minimum nucleus size cannot be greater than maximum nucleus size");
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
   * Check if settings were changed during this dialog session.
   *
   * @return true if settings were modified and saved
   */
  public boolean isSettingsChanged() {
    return settingsChanged;
  }

  /**
   * Get the current settings instance.
   *
   * @return The NuclearSegmentationSettings instance
   */
  public NuclearSegmentationSettings getSettings() {
    return settings;
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
        // Create new immutable settings instance with updated values
        settings =
            new NuclearSegmentationSettings(
                (String) modelChoiceCombo.getSelectedItem(),
                normalizeInputCheck.isSelected(),
                percentileBottomSlider.getValue(),
                percentileTopSlider.getValue(),
                probThreshSlider.getValue() / 100.0f,
                nmsThreshSlider.getValue() / 100.0f,
                NuclearSegmentationSettings.DEFAULT_OUTPUT_TYPE,
                ((Number) nTilesSpinner.getValue()).intValue(),
                ((Number) excludeBoundarySpinner.getValue()).intValue(),
                NuclearSegmentationSettings.DEFAULT_ROI_POSITION,
                verboseCheck.isSelected(),
                showProgressCheck.isSelected(),
                showProbDistCheck.isSelected(),
                ((Number) minNucleusSizeSpinner.getValue()).doubleValue(),
                ((Number) maxNucleusSizeSpinner.getValue()).doubleValue());

        // Save to file
        configManager.saveNuclearSegmentationSettings(settings);

        settingsChanged = true;
        LOGGER.info("Nuclear segmentation settings saved successfully");

        dispose();

      } catch (Exception ex) {
        LOGGER.error("Failed to save nuclear segmentation settings", ex);
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
              NuclearSegmentationSettingsDialog.this,
              "Are you sure you want to reset all settings to their default values?",
              "Reset to Defaults",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE);

      if (result == JOptionPane.YES_OPTION) {
        // Create new settings instance with default values
        settings = NuclearSegmentationSettings.createDefault();
        loadCurrentSettings();
        LOGGER.info("Nuclear segmentation settings reset to default values");
      }
    }
  }
}
