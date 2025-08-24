package com.scipath.scipathj.ui.analysis.dialogs.settings;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.config.CytoplasmSegmentationSettings;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings dialog for Cytoplasm Segmentation step.
 * This dialog allows users to configure all cytoplasm segmentation parameters.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class CytoplasmSegmentationSettingsDialog extends JDialog {

   private static final Logger LOGGER =
       LoggerFactory.getLogger(CytoplasmSegmentationSettingsDialog.class);

   private CytoplasmSegmentationSettings settings;
   private final ConfigurationManager configManager;
   private final com.scipath.scipathj.core.config.MainSettings mainSettings;

  // UI Components
  private JSpinner voronoiExpansionSpinner;
  private JCheckBox useVesselExclusionCheck;
  private JSpinner minCellSizeSpinner;
  private JSpinner maxCellSizeSpinner;
  private JSpinner gaussianBlurSigmaSpinner;
  private JSpinner morphClosingRadiusSpinner;
  private JSpinner watershedToleranceSpinner;
  private JSpinner minCytoplasmAreaSpinner;
  private JSpinner maxCytoplasmAreaSpinner;
  private JCheckBox fillHolesCheck;
  private JCheckBox smoothBoundariesCheck;
  private JCheckBox verboseCheck;

  private boolean settingsChanged = false;

  public CytoplasmSegmentationSettingsDialog(
      Frame parent, ConfigurationManager configurationManager) {
    super(parent, "Cytoplasm Segmentation Settings", true);
    this.configManager = configurationManager;
    this.settings = configManager.loadCytoplasmSegmentationSettings();
    this.mainSettings = configManager.loadMainSettings();
    initializeDialog();
    loadCurrentSettings();
  }

  private void initializeDialog() {
    setSize(500, 650);
    setLocationRelativeTo(getParent());
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    JLabel titleLabel =
        UIUtils.createBoldLabel("Cytoplasm Segmentation Settings", UIConstants.SUBTITLE_FONT_SIZE);
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

    // Voronoi Tessellation Settings
    addSeparator(panel, gbc, "Voronoi Tessellation");
    addVoronoiRows(panel, gbc);

    // Vessel Exclusion Settings
    addSeparator(panel, gbc, "Vessel Exclusion");
    addVesselExclusionRows(panel, gbc);

    // Size Filtering
    addSeparator(panel, gbc, "Size Filtering");
    addSizeFilteringRows(panel, gbc);

    // Image Processing
    addSeparator(panel, gbc, "Image Processing");
    addImageProcessingRows(panel, gbc);

    // Advanced Options
    addSeparator(panel, gbc, "Advanced Options");
    addAdvancedRows(panel, gbc);

    return panel;
  }

  private void addVoronoiRows(JPanel panel, GridBagConstraints gbc) {
    // Voronoi Expansion
    addSpinnerRow(
        panel,
        gbc,
        "Voronoi Expansion:",
        new SpinnerNumberModel(5.0, 0.0, 100.0, 0.5),
        "Expansion distance for Voronoi tessellation (pixels)");
    voronoiExpansionSpinner = (JSpinner) panel.getComponent(panel.getComponentCount() - 1);
  }

  private void addVesselExclusionRows(JPanel panel, GridBagConstraints gbc) {
    // Use Vessel Exclusion checkbox
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    useVesselExclusionCheck = new JCheckBox("Exclude Vessel Areas");
    useVesselExclusionCheck.setToolTipText("Exclude vessel areas from cytoplasm segmentation");
    panel.add(useVesselExclusionCheck, gbc);
    gbc.gridwidth = 1;
  }

  private void addSizeFilteringRows(JPanel panel, GridBagConstraints gbc) {
    // Min Cell Size - show in scaled units
    String minCellLabel = "Min Cell Size (" + mainSettings.scaleUnit() + "²):";
    double minCellScaled = CytoplasmSegmentationSettings.pixelsToScaledSize(
        CytoplasmSegmentationSettings.DEFAULT_MIN_CELL_SIZE, mainSettings);
    addSpinnerRow(
        panel,
        gbc,
        minCellLabel,
        new SpinnerNumberModel(minCellScaled, 1.0, 10000.0, 10.0),
        "Minimum cell area in " + mainSettings.scaleUnit() + "² (converted to pixels automatically)");
    minCellSizeSpinner = (JSpinner) panel.getComponent(panel.getComponentCount() - 1);

    // Max Cell Size - show in scaled units
    String maxCellLabel = "Max Cell Size (" + mainSettings.scaleUnit() + "²):";
    double maxCellScaled = CytoplasmSegmentationSettings.pixelsToScaledSize(
        CytoplasmSegmentationSettings.DEFAULT_MAX_CELL_SIZE, mainSettings);
    addSpinnerRow(
        panel,
        gbc,
        maxCellLabel,
        new SpinnerNumberModel(maxCellScaled, 1.0, 100000.0, 100.0),
        "Maximum cell area in " + mainSettings.scaleUnit() + "² (converted to pixels automatically)");
    maxCellSizeSpinner = (JSpinner) panel.getComponent(panel.getComponentCount() - 1);

    // Min Cytoplasm Area - show in scaled units
    String minCytoplasmLabel = "Min Cytoplasm Area (" + mainSettings.scaleUnit() + "²):";
    double minCytoplasmScaled = CytoplasmSegmentationSettings.pixelsToScaledSize(
        CytoplasmSegmentationSettings.DEFAULT_MIN_CYTOPLASM_SIZE, mainSettings);
    addSpinnerRow(
        panel,
        gbc,
        minCytoplasmLabel,
        new SpinnerNumberModel(minCytoplasmScaled, 1.0, 10000.0, 5.0),
        "Minimum cytoplasm area in " + mainSettings.scaleUnit() + "² (converted to pixels automatically)");
    minCytoplasmAreaSpinner = (JSpinner) panel.getComponent(panel.getComponentCount() - 1);

    // Max Cytoplasm Area - show in scaled units
    String maxCytoplasmLabel = "Max Cytoplasm Area (" + mainSettings.scaleUnit() + "²):";
    double maxCytoplasmScaled = CytoplasmSegmentationSettings.pixelsToScaledSize(
        CytoplasmSegmentationSettings.DEFAULT_MAX_CYTOPLASM_AREA, mainSettings);
    addSpinnerRow(
        panel,
        gbc,
        maxCytoplasmLabel,
        new SpinnerNumberModel(maxCytoplasmScaled, 1.0, 100000.0, 50.0),
        "Maximum cytoplasm area in " + mainSettings.scaleUnit() + "² (converted to pixels automatically)");
    maxCytoplasmAreaSpinner = (JSpinner) panel.getComponent(panel.getComponentCount() - 1);
  }

  private void addImageProcessingRows(JPanel panel, GridBagConstraints gbc) {
    // Gaussian Blur Sigma
    addSpinnerRow(
        panel,
        gbc,
        "Gaussian Blur Sigma:",
        new SpinnerNumberModel(1.0, 0.0, 10.0, 0.1),
        "Gaussian blur sigma for preprocessing");
    gaussianBlurSigmaSpinner = (JSpinner) panel.getComponent(panel.getComponentCount() - 1);

    // Morphological Closing Radius
    addSpinnerRow(
        panel,
        gbc,
        "Morph Closing Radius:",
        new SpinnerNumberModel(2.0, 0.0, 20.0, 0.5),
        "Morphological closing radius for gap filling");
    morphClosingRadiusSpinner = (JSpinner) panel.getComponent(panel.getComponentCount() - 1);

    // Watershed Tolerance
    addSpinnerRow(
        panel,
        gbc,
        "Watershed Tolerance:",
        new SpinnerNumberModel(0.5, 0.0, 10.0, 0.1),
        "Watershed tolerance for cell separation");
    watershedToleranceSpinner = (JSpinner) panel.getComponent(panel.getComponentCount() - 1);
  }

  private void addAdvancedRows(JPanel panel, GridBagConstraints gbc) {
    // Fill Holes
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    fillHolesCheck = new JCheckBox("Fill Holes in Cytoplasm");
    fillHolesCheck.setToolTipText("Fill small holes within cytoplasm regions");
    panel.add(fillHolesCheck, gbc);

    // Smooth Boundaries
    gbc.gridy++;
    smoothBoundariesCheck = new JCheckBox("Smooth Boundaries");
    smoothBoundariesCheck.setToolTipText("Apply boundary smoothing to cytoplasm regions");
    panel.add(smoothBoundariesCheck, gbc);

    // Verbose output
    gbc.gridy++;
    verboseCheck = new JCheckBox("Verbose Output");
    verboseCheck.setToolTipText("Enable verbose output during processing");
    panel.add(verboseCheck, gbc);

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
    if (model.getValue() instanceof Double) {
      JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0.0");
      spinner.setEditor(editor);
    }

    panel.add(spinner, gbc);

    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
  }

  private void loadCurrentSettings() {
    // Note: CytoplasmSegmentationSettings is now a record with different parameters
    // Using available parameters and defaults for missing ones
    useVesselExclusionCheck.setSelected(settings.useVesselExclusion());

    // Convert pixel values to scaled units for display
    double minCellScaled = CytoplasmSegmentationSettings.pixelsToScaledSize(
        settings.minCellSize(), mainSettings);
    double maxCellScaled = CytoplasmSegmentationSettings.pixelsToScaledSize(
        settings.maxCellSize(), mainSettings);
    double minCytoplasmScaled = CytoplasmSegmentationSettings.pixelsToScaledSize(
        settings.minCytoplasmSize(), mainSettings);
    double maxCytoplasmScaled = CytoplasmSegmentationSettings.pixelsToScaledSize(
        CytoplasmSegmentationSettings.DEFAULT_MAX_CYTOPLASM_AREA, mainSettings);

    minCellSizeSpinner.setValue(minCellScaled);
    maxCellSizeSpinner.setValue(maxCellScaled);
    minCytoplasmAreaSpinner.setValue(minCytoplasmScaled);
    maxCytoplasmAreaSpinner.setValue(maxCytoplasmScaled);

    // Set defaults for parameters not in the new record
    voronoiExpansionSpinner.setValue(5.0); // Default value
    gaussianBlurSigmaSpinner.setValue(1.5); // Default value
    morphClosingRadiusSpinner.setValue(2); // Default value
    watershedToleranceSpinner.setValue(10.0); // Default value
    fillHolesCheck.setSelected(true); // Default value
    smoothBoundariesCheck.setSelected(true); // Default value
    verboseCheck.setSelected(false); // Default value

    LOGGER.debug("Loaded current cytoplasm segmentation settings into dialog");
  }

  /**
   * Validate the current input values.
   *
   * @return true if all values are valid
   */
  private boolean validateInputs() {
    try {
      double voronoiExpansion = ((Number) voronoiExpansionSpinner.getValue()).doubleValue();
      double minCellSize = ((Number) minCellSizeSpinner.getValue()).doubleValue();
      double maxCellSize = ((Number) maxCellSizeSpinner.getValue()).doubleValue();
      double minCytoplasmArea = ((Number) minCytoplasmAreaSpinner.getValue()).doubleValue();
      double maxCytoplasmArea = ((Number) maxCytoplasmAreaSpinner.getValue()).doubleValue();
      double gaussianBlurSigma = ((Number) gaussianBlurSigmaSpinner.getValue()).doubleValue();
      double morphClosingRadius = ((Number) morphClosingRadiusSpinner.getValue()).doubleValue();
      double watershedTolerance = ((Number) watershedToleranceSpinner.getValue()).doubleValue();

      if (voronoiExpansion < 0) {
        showErrorMessage("Voronoi expansion must be non-negative");
        return false;
      }

      if (minCellSize <= 0) {
        showErrorMessage("Minimum cell size must be positive");
        return false;
      }

      if (maxCellSize <= 0) {
        showErrorMessage("Maximum cell size must be positive");
        return false;
      }

      if (minCellSize > maxCellSize) {
        showErrorMessage("Minimum cell size cannot be greater than maximum cell size");
        return false;
      }

      if (minCytoplasmArea <= 0) {
        showErrorMessage("Minimum cytoplasm area must be positive");
        return false;
      }

      if (maxCytoplasmArea <= 0) {
        showErrorMessage("Maximum cytoplasm area must be positive");
        return false;
      }

      if (minCytoplasmArea > maxCytoplasmArea) {
        showErrorMessage("Minimum cytoplasm area cannot be greater than maximum cytoplasm area");
        return false;
      }

      if (gaussianBlurSigma < 0) {
        showErrorMessage("Gaussian blur sigma must be non-negative");
        return false;
      }

      if (morphClosingRadius < 0) {
        showErrorMessage("Morphological closing radius must be non-negative");
        return false;
      }

      if (watershedTolerance < 0) {
        showErrorMessage("Watershed tolerance must be non-negative");
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
   * @return The CytoplasmSegmentationSettings instance
   */
  public CytoplasmSegmentationSettings getSettings() {
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
        // Convert scaled values back to pixels
        double minCellScaled = ((Number) minCellSizeSpinner.getValue()).doubleValue();
        double maxCellScaled = ((Number) maxCellSizeSpinner.getValue()).doubleValue();
        double minCytoplasmScaled = ((Number) minCytoplasmAreaSpinner.getValue()).doubleValue();
        double maxCytoplasmScaled = ((Number) maxCytoplasmAreaSpinner.getValue()).doubleValue();

        double minCellPixels = CytoplasmSegmentationSettings.scaledSizeToPixels(minCellScaled, mainSettings);
        double maxCellPixels = CytoplasmSegmentationSettings.scaledSizeToPixels(maxCellScaled, mainSettings);
        double minCytoplasmPixels = CytoplasmSegmentationSettings.scaledSizeToPixels(minCytoplasmScaled, mainSettings);
        double maxCytoplasmPixels = CytoplasmSegmentationSettings.scaledSizeToPixels(maxCytoplasmScaled, mainSettings);

        // Create new immutable settings instance with updated values
        // Note: CytoplasmSegmentationSettings record has specific parameters:
        // useVesselExclusion, addImageBorder, borderWidth, applyVoronoi,
        // minCellSize, maxCellSize, minCytoplasmSize, validateCellShape,
        // maxAspectRatio, linkNucleusToCytoplasm, createCellROIs, excludeBorderCells
        settings =
            new CytoplasmSegmentationSettings(
                useVesselExclusionCheck.isSelected(),
                true, // addImageBorder - default
                1, // borderWidth - default
                true, // applyVoronoi - default
                minCellPixels,
                maxCellPixels,
                minCytoplasmPixels,
                true, // validateCellShape - default
                5.0, // maxAspectRatio - default
                true, // linkNucleusToCytoplasm - default
                true, // createCellROIs - default
                false); // excludeBorderCells - default

        // Save to file
        configManager.saveCytoplasmSegmentationSettings(settings);

        settingsChanged = true;
        LOGGER.info("Cytoplasm segmentation settings saved successfully");

        dispose();

      } catch (Exception ex) {
        LOGGER.error("Failed to save cytoplasm segmentation settings", ex);
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
              CytoplasmSegmentationSettingsDialog.this,
              "Are you sure you want to reset all settings to their default values?",
              "Reset to Defaults",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE);

      if (result == JOptionPane.YES_OPTION) {
        // Create new settings instance with default values
        settings = CytoplasmSegmentationSettings.createDefault();
        loadCurrentSettings();
        LOGGER.info("Cytoplasm segmentation settings reset to default values");
      }
    }
  }
}
