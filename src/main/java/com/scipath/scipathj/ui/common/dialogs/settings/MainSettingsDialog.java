package com.scipath.scipathj.ui.common.dialogs.settings;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.config.MainSettings;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modal dialog for editing main application settings.
 * Uses dependency injection pattern and works with immutable MainSettings records.
 * Provides settings for scale conversion and ROI appearance for each category.
 *
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 1.0.0
 */
public class MainSettingsDialog extends JDialog {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainSettingsDialog.class);

  // Dependencies injected via constructor
  private final ConfigurationManager configurationManager;
  private final Consumer<MainSettings> onSettingsChanged;

  // Current settings state
  private MainSettings currentSettings;

  // UI Components for scale settings
  private JSpinner pixelsPerMicrometerSpinner;
  private JTextField scaleUnitField;
  private JLabel scalePreviewLabel;
  private JSpinner borderDistanceSpinner;

  // UI Components for ROI categories
  private ROICategoryPanel vesselPanel;
  private ROICategoryPanel nucleusPanel;
  private ROICategoryPanel cytoplasmPanel;
  private ROICategoryPanel cellPanel;
  private IgnoreROIPanel ignorePanel;

  // UI Components for CSV settings
  private CsvFormatToggleSwitch csvFormatToggleSwitch;

  // UI Components for ignore functionality
  private JCheckBox enableIgnoreFunctionalityCheckBox;

  // UI Components for CSV inclusion
  private JCheckBox includeIgnoredInCsvCheckBox;

  // Result tracking
  private boolean settingsChanged = false;

  /**
   * Creates a new main settings dialog with dependency injection.
   *
   * @param parent The parent frame for modal positioning
   * @param configurationManager Configuration manager for persistence
   * @param onSettingsChanged Callback invoked when settings are successfully saved
   */
  public MainSettingsDialog(
      Frame parent,
      ConfigurationManager configurationManager,
      Consumer<MainSettings> onSettingsChanged) {

    super(parent, "Main Application Settings", true);
    this.configurationManager = configurationManager;
    this.onSettingsChanged = onSettingsChanged;
    this.currentSettings = configurationManager.loadMainSettings();

    initializeComponents();
    layoutComponents();
    loadCurrentSettings();
    setupEventHandlers();

    setSize(650, 550);
    setLocationRelativeTo(parent);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
  }

  /**
   * Initialize all UI components.
   */
  private void initializeComponents() {
    // Scale conversion components
    pixelsPerMicrometerSpinner =
        new JSpinner(
            new SpinnerNumberModel(MainSettings.DEFAULT_PIXELS_PER_MICROMETER, 0.001, 1000.0, 0.1));
    pixelsPerMicrometerSpinner.setToolTipText("Number of pixels per micrometer");

    scaleUnitField = new JTextField(MainSettings.DEFAULT_SCALE_UNIT, 10);
    scaleUnitField.setToolTipText("Unit for scale display (e.g., μm, mm)");

    scalePreviewLabel = new JLabel();
    scalePreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
    scalePreviewLabel.setBorder(UIUtils.createPadding(UIConstants.SMALL_SPACING));

    // ROI category panels
    vesselPanel = new ROICategoryPanel(MainSettings.ROICategory.VESSEL);
    nucleusPanel = new ROICategoryPanel(MainSettings.ROICategory.NUCLEUS);
    cytoplasmPanel = new ROICategoryPanel(MainSettings.ROICategory.CYTOPLASM);
    cellPanel = new ROICategoryPanel(MainSettings.ROICategory.CELL);
    ignorePanel = new IgnoreROIPanel();

    // CSV format toggle switch
    csvFormatToggleSwitch = new CsvFormatToggleSwitch();
    csvFormatToggleSwitch.setToolTipText("Toggle between US and EU CSV format");

    // Ignore functionality checkbox
    enableIgnoreFunctionalityCheckBox = new JCheckBox("Ignore Cells near the borders");
    enableIgnoreFunctionalityCheckBox.setToolTipText("Enable/disable the ignore functionality for cells near image borders");
    enableIgnoreFunctionalityCheckBox.setSelected(MainSettings.DEFAULT_ENABLE_IGNORE_FUNCTIONALITY);

    // CSV inclusion checkbox
    includeIgnoredInCsvCheckBox = new JCheckBox("Extract Ignore data to csv");
    includeIgnoredInCsvCheckBox.setToolTipText("Include ROIs with ignore=true in CSV exports");
    includeIgnoredInCsvCheckBox.setSelected(MainSettings.DEFAULT_INCLUDE_IGNORED_IN_CSV);
  }

  /**
   * Layout all components in the dialog.
   */
  private void layoutComponents() {
    setLayout(new BorderLayout());

    // Title
    JLabel titleLabel =
        UIUtils.createBoldLabel("Main Application Settings", UIConstants.SUBTITLE_FONT_SIZE);
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    titleLabel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    // Main content with tabs
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Scale & Units", createScalePanel());
    tabbedPane.addTab("CSV Export", createCsvPanel());
    tabbedPane.addTab("Ignore Distance", createIgnoreDistancePanel());

    // Button panel
    JPanel buttonPanel = createButtonPanel();

    // Layout
    add(titleLabel, BorderLayout.NORTH);
    add(tabbedPane, BorderLayout.CENTER);
    add(buttonPanel, BorderLayout.SOUTH);
  }

  /**
   * Create the scale conversion settings panel.
   */
  private JPanel createScalePanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets =
        new Insets(
            UIConstants.MEDIUM_SPACING,
            UIConstants.MEDIUM_SPACING,
            UIConstants.MEDIUM_SPACING,
            UIConstants.MEDIUM_SPACING);
    gbc.anchor = GridBagConstraints.WEST;

    // Pixels per micrometer
    gbc.gridx = 0;
    gbc.gridy = 0;
    JLabel pixelsLabel =
        UIUtils.createLabel("Pixels per Micrometer:", UIConstants.NORMAL_FONT_SIZE, null);
    panel.add(pixelsLabel, gbc);

    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    panel.add(pixelsPerMicrometerSpinner, gbc);

    // Scale unit
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    JLabel unitLabel = UIUtils.createLabel("Scale Unit:", UIConstants.NORMAL_FONT_SIZE, null);
    panel.add(unitLabel, gbc);

    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    panel.add(scaleUnitField, gbc);

    // Scale preview
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    scalePreviewLabel.setBorder(BorderFactory.createTitledBorder("Scale Preview"));
    panel.add(scalePreviewLabel, gbc);

    return panel;
  }

  /**
   * Create the CSV settings panel.
   */
  private JPanel createCsvPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING,
                            UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING);
    gbc.anchor = GridBagConstraints.WEST;

    // CSV format selection
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;

    // Title label
    JLabel formatLabel = UIUtils.createBoldLabel("CSV Export Format", UIConstants.NORMAL_FONT_SIZE);
    panel.add(formatLabel, gbc);

    // CSV format toggle switch
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    panel.add(csvFormatToggleSwitch, gbc);

    // Add description labels
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;

    // Description panel
    JPanel descriptionPanel = new JPanel(new GridLayout(2, 1, 0, 2));
    descriptionPanel.setBorder(UIUtils.createPadding(UIConstants.SMALL_SPACING));
    descriptionPanel.setOpaque(false);

    // US format description
    String usDesc = "US Format: comma (,) delimiter, period (.) decimal separator";
    JLabel usLabel = UIUtils.createLabel(usDesc, UIConstants.SMALL_FONT_SIZE, UIManager.getColor("Label.foreground"));
    usLabel.setFont(usLabel.getFont().deriveFont(Font.ITALIC));
    descriptionPanel.add(usLabel);

    // EU format description
    String euDesc = "EU Format: semicolon (;) delimiter, comma (,) decimal separator";
    JLabel euLabel = UIUtils.createLabel(euDesc, UIConstants.SMALL_FONT_SIZE, UIManager.getColor("Label.foreground"));
    euLabel.setFont(euLabel.getFont().deriveFont(Font.ITALIC));
    descriptionPanel.add(euLabel);

    panel.add(descriptionPanel, gbc);

    // Add some spacing
    gbc.gridy = 3;
    gbc.weighty = 1.0;
    panel.add(new JPanel(), gbc); // Empty panel for spacing

    return panel;
  }

  /**
   * Create the ignore distance settings panel.
   */
  private JPanel createIgnoreDistancePanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING,
                            UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING);
    gbc.anchor = GridBagConstraints.WEST;

    // Enable ignore functionality checkbox
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    panel.add(enableIgnoreFunctionalityCheckBox, gbc);

    // Border distance
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    JLabel borderDistanceLabel = UIUtils.createLabel("Border Distance (pixels):", UIConstants.NORMAL_FONT_SIZE, null);
    panel.add(borderDistanceLabel, gbc);

    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    borderDistanceSpinner = new JSpinner(new SpinnerNumberModel(MainSettings.DEFAULT_BORDER_DISTANCE, 0, 1000, 1));
    borderDistanceSpinner.setToolTipText("Distance from image borders to consider ROIs as ignore");
    panel.add(borderDistanceSpinner, gbc);

    // CSV inclusion checkbox (at the bottom)
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    panel.add(includeIgnoredInCsvCheckBox, gbc);

    return panel;
  }

  /**
   * Create the button panel.
   */
  private JPanel createButtonPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    panel.setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING));

    JButton resetButton = UIUtils.createStandardButton("Reset to Defaults", null);
    JButton cancelButton = UIUtils.createStandardButton("Cancel", null);
    JButton okButton = UIUtils.createStandardButton("OK", null);

    // Make buttons wider
    resetButton.setPreferredSize(new Dimension(160, resetButton.getPreferredSize().height));
    cancelButton.setPreferredSize(new Dimension(100, cancelButton.getPreferredSize().height));
    okButton.setPreferredSize(new Dimension(100, okButton.getPreferredSize().height));

    resetButton.addActionListener(this::handleResetToDefaults);
    cancelButton.addActionListener(this::handleCancel);
    okButton.addActionListener(this::handleOK);

    panel.add(resetButton);
    panel.add(cancelButton);
    panel.add(okButton);

    return panel;
  }

  /**
   * Setup event handlers for live updates.
   */
  private void setupEventHandlers() {
    // Update preview when scale settings change
    pixelsPerMicrometerSpinner.addChangeListener(e -> updateScalePreview());
    scaleUnitField
        .getDocument()
        .addDocumentListener(
            new javax.swing.event.DocumentListener() {
              public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateScalePreview();
              }

              public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateScalePreview();
              }

              public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateScalePreview();
              }
            });

    // Update border distance spinner state and CSV inclusion checkbox when ignore functionality checkbox changes
    enableIgnoreFunctionalityCheckBox.addActionListener(e -> {
        updateBorderDistanceSpinnerState();
        updateCsvInclusionCheckboxState();
    });
  }

  /**
   * Update the enabled state of the border distance spinner based on the ignore functionality checkbox.
   */
  private void updateBorderDistanceSpinnerState() {
    boolean enabled = enableIgnoreFunctionalityCheckBox.isSelected();
    borderDistanceSpinner.setEnabled(enabled);
  }

  /**
   * Update the enabled state of the CSV inclusion checkbox based on the ignore functionality checkbox.
   */
  private void updateCsvInclusionCheckboxState() {
    boolean enabled = enableIgnoreFunctionalityCheckBox.isSelected();
    includeIgnoredInCsvCheckBox.setEnabled(enabled);
  }

  /**
   * Load current settings into UI components.
   */
  private void loadCurrentSettings() {
    pixelsPerMicrometerSpinner.setValue(currentSettings.pixelsPerMicrometer());
    scaleUnitField.setText(currentSettings.scaleUnit());
    borderDistanceSpinner.setValue(currentSettings.ignoreSettings().borderDistance());

    // Load CSV format setting
    csvFormatToggleSwitch.setSelected(currentSettings.useEuCsvFormat());

    // Load ignore functionality setting
    enableIgnoreFunctionalityCheckBox.setSelected(currentSettings.enableIgnoreFunctionality());
    updateBorderDistanceSpinnerState();

    // Load CSV inclusion setting
    includeIgnoredInCsvCheckBox.setSelected(currentSettings.includeIgnoredInCsv());
    updateCsvInclusionCheckboxState();

    updateScalePreview();
    LOGGER.debug("Loaded current settings into dialog components");
  }

  /**
   * Update the scale preview display.
   */
  private void updateScalePreview() {
    try {
      double pixelsPerMicrometer = (Double) pixelsPerMicrometerSpinner.getValue();
      String unit = scaleUnitField.getText().trim();
      if (unit.isEmpty()) unit = "μm";

      double micrometersFor100Pixels = 100.0 / pixelsPerMicrometer;
      scalePreviewLabel.setText(
          String.format("100 pixels = %.2f %s", micrometersFor100Pixels, unit));
    } catch (Exception e) {
      scalePreviewLabel.setText("Invalid values");
    }
  }

  /**
   * Validate all input values.
   */
  private boolean validateInputs() {
    try {
      double pixelsPerMicrometer = (Double) pixelsPerMicrometerSpinner.getValue();
      String scaleUnit = scaleUnitField.getText().trim();

      if (pixelsPerMicrometer <= 0) {
        showErrorMessage("Pixels per micrometer must be positive");
        return false;
      }

      if (scaleUnit.isEmpty()) {
        showErrorMessage("Scale unit cannot be empty");
        return false;
      }

      // Validate border distance
      int borderDistance = (Integer) borderDistanceSpinner.getValue();
      if (borderDistance < 0) {
        showErrorMessage("Border distance must be non-negative");
        return false;
      }

      return true;

    } catch (Exception e) {
      showErrorMessage("Invalid input values: " + e.getMessage());
      return false;
    }
  }

  /**
   * Create updated settings from current UI state.
   */
  private MainSettings createUpdatedSettings() {
    double pixelsPerMicrometer = (Double) pixelsPerMicrometerSpinner.getValue();
    String scaleUnit = scaleUnitField.getText().trim();
    int borderDistance = (Integer) borderDistanceSpinner.getValue();

    // Create updated ignore settings with new border distance but keep other ignore settings
    MainSettings.IgnoreROIAppearanceSettings updatedIgnoreSettings =
        currentSettings.ignoreSettings().withBorderDistance(borderDistance);

    // Get CSV format setting from toggle switch
    boolean useEuCsvFormat = csvFormatToggleSwitch.isSelected();

    // Get ignore functionality setting from checkbox
    boolean enableIgnoreFunctionality = enableIgnoreFunctionalityCheckBox.isSelected();

    // Get CSV inclusion setting from checkbox
    boolean includeIgnoredInCsv = includeIgnoredInCsvCheckBox.isSelected();

    return new MainSettings(
         pixelsPerMicrometer,
         scaleUnit,
         currentSettings.vesselSettings(),
         currentSettings.nucleusSettings(),
         currentSettings.cytoplasmSettings(),
         currentSettings.cellSettings(),
         updatedIgnoreSettings,
         useEuCsvFormat,
         enableIgnoreFunctionality,
         includeIgnoredInCsv);
  }

  /**
   * Handle OK button click.
   */
  private void handleOK(ActionEvent e) {
    if (!validateInputs()) {
      return;
    }

    try {
      MainSettings updatedSettings = createUpdatedSettings();
      configurationManager.saveMainSettings(updatedSettings);

      settingsChanged = true;
      LOGGER.info("Main settings saved successfully");

      // Notify callback if provided
      if (onSettingsChanged != null) {
        onSettingsChanged.accept(updatedSettings);
      }

      dispose();

    } catch (Exception ex) {
      LOGGER.error("Failed to save main settings", ex);
      showErrorMessage("Failed to save settings: " + ex.getMessage());
    }
  }

  /**
   * Handle Cancel button click.
   */
  private void handleCancel(ActionEvent e) {
    dispose();
  }

  /**
   * Handle Reset to Defaults button click.
   */
  private void handleResetToDefaults(ActionEvent e) {
    int result =
        JOptionPane.showConfirmDialog(
            this,
            "Reset all settings to their default values?",
            "Reset to Defaults",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

    if (result == JOptionPane.YES_OPTION) {
      currentSettings = MainSettings.createDefault();
      loadCurrentSettings();
      LOGGER.info("Settings reset to defaults");
    }
  }

  /**
   * Show an error message dialog.
   */
  private void showErrorMessage(String message) {
    JOptionPane.showMessageDialog(this, message, "Invalid Input", JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Check if settings were changed.
   */
  public boolean isSettingsChanged() {
    return settingsChanged;
  }

  /**
   * Panel for configuring a specific ROI category.
   */
  private static class ROICategoryPanel extends JPanel {

    private final MainSettings.ROICategory category;
    private final JButton borderColorButton;
    private final JSlider fillOpacitySlider;
    private final JLabel fillOpacityValueLabel;
    private final JSpinner borderWidthSpinner;

    public ROICategoryPanel(MainSettings.ROICategory category) {
      super(new GridBagLayout());
      this.category = category;

      setBorder(
          BorderFactory.createCompoundBorder(
              UIUtils.createPadding(UIConstants.LARGE_SPACING),
              BorderFactory.createTitledBorder(category.getDisplayName() + " ROI Settings")));

      GridBagConstraints gbc = new GridBagConstraints();
      gbc.insets =
          new Insets(
              UIConstants.MEDIUM_SPACING,
              UIConstants.MEDIUM_SPACING,
              UIConstants.MEDIUM_SPACING,
              UIConstants.MEDIUM_SPACING);
      gbc.anchor = GridBagConstraints.WEST;

      // Border color
      gbc.gridx = 0;
      gbc.gridy = 0;
      JLabel borderColorLabel =
          UIUtils.createLabel("Border Color:", UIConstants.NORMAL_FONT_SIZE, null);
      add(borderColorLabel, gbc);

      gbc.gridx = 1;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1.0;
      borderColorButton = new JButton();
      borderColorButton.setPreferredSize(new Dimension(100, 30));
      borderColorButton.setBackground(category.getDefaultBorderColor());
      borderColorButton.setToolTipText("Click to change border color");
      borderColorButton.addActionListener(this::chooseBorderColor);
      add(borderColorButton, gbc);

      // Fill opacity
      gbc.gridx = 0;
      gbc.gridy = 1;
      gbc.fill = GridBagConstraints.NONE;
      gbc.weightx = 0.0;
      JLabel fillOpacityLabel =
          UIUtils.createLabel("Fill Opacity:", UIConstants.NORMAL_FONT_SIZE, null);
      add(fillOpacityLabel, gbc);

      gbc.gridx = 1;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1.0;

      JPanel opacityPanel = new JPanel(new BorderLayout());
      fillOpacitySlider = new JSlider(0, 100, Math.round(category.getDefaultFillOpacity() * 100));
      fillOpacitySlider.setMajorTickSpacing(25);
      fillOpacitySlider.setMinorTickSpacing(5);
      fillOpacitySlider.setPaintTicks(true);
      fillOpacitySlider.setPaintLabels(true);

      fillOpacityValueLabel = new JLabel("0%");
      fillOpacityValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
      fillOpacityValueLabel.setPreferredSize(new Dimension(40, 20));
      fillOpacitySlider.addChangeListener(
          e -> {
            int value = fillOpacitySlider.getValue();
            fillOpacityValueLabel.setText(value + "%");
          });

      opacityPanel.add(fillOpacitySlider, BorderLayout.CENTER);
      opacityPanel.add(fillOpacityValueLabel, BorderLayout.EAST);
      add(opacityPanel, gbc);

      // Border width
      gbc.gridx = 0;
      gbc.gridy = 2;
      gbc.fill = GridBagConstraints.NONE;
      gbc.weightx = 0.0;
      JLabel borderWidthLabel =
          UIUtils.createLabel("Border Width:", UIConstants.NORMAL_FONT_SIZE, null);
      add(borderWidthLabel, gbc);

      gbc.gridx = 1;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1.0;
      borderWidthSpinner =
          new JSpinner(new SpinnerNumberModel(category.getDefaultBorderWidth(), 1, 10, 1));
      borderWidthSpinner.setToolTipText("Border width in pixels");
      add(borderWidthSpinner, gbc);
    }

    private void chooseBorderColor(ActionEvent e) {
      Color currentColor = borderColorButton.getBackground();
      Color newColor =
          JColorChooser.showDialog(
              this, "Choose " + category.getDisplayName() + " ROI Border Color", currentColor);
      if (newColor != null) {
        borderColorButton.setBackground(newColor);
      }
    }

    public void loadSettings(MainSettings.ROIAppearanceSettings settings) {
      borderColorButton.setBackground(settings.borderColor());
      fillOpacitySlider.setValue(Math.round(settings.fillOpacity() * 100));
      fillOpacityValueLabel.setText(Math.round(settings.fillOpacity() * 100) + "%");
      borderWidthSpinner.setValue(settings.borderWidth());
    }

    public MainSettings.ROIAppearanceSettings createSettings() {
      return new MainSettings.ROIAppearanceSettings(
          borderColorButton.getBackground(),
          fillOpacitySlider.getValue() / 100.0f,
          (Integer) borderWidthSpinner.getValue());
    }

    public boolean validateInputs() {
      try {
        float opacity = fillOpacitySlider.getValue() / 100.0f;
        int borderWidth = (Integer) borderWidthSpinner.getValue();

        if (opacity < 0.0f || opacity > 1.0f) {
          showValidationError("Fill opacity must be between 0% and 100%");
          return false;
        }

        if (borderWidth < 1) {
          showValidationError("Border width must be at least 1");
          return false;
        }

        return true;
      } catch (Exception e) {
        showValidationError("Invalid settings: " + e.getMessage());
        return false;
      }
    }

    private void showValidationError(String message) {
      JOptionPane.showMessageDialog(
          this,
          category.getDisplayName() + " ROI: " + message,
          "Invalid Input",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Panel for configuring ignore ROI settings.
   */
  private static class IgnoreROIPanel extends JPanel {

    private final JSpinner borderDistanceSpinner;
    private final JButton ignoreColorButton;
    private final JCheckBox showIgnoredROIsCheckBox;

    public IgnoreROIPanel() {
      super(new GridBagLayout());
      setBorder(
          BorderFactory.createCompoundBorder(
              UIUtils.createPadding(UIConstants.LARGE_SPACING),
              BorderFactory.createTitledBorder("Ignore ROI Settings")));

      GridBagConstraints gbc = new GridBagConstraints();
      gbc.insets =
          new Insets(
              UIConstants.MEDIUM_SPACING,
              UIConstants.MEDIUM_SPACING,
              UIConstants.MEDIUM_SPACING,
              UIConstants.MEDIUM_SPACING);
      gbc.anchor = GridBagConstraints.WEST;

      // Border distance
      gbc.gridx = 0;
      gbc.gridy = 0;
      JLabel borderDistanceLabel =
          UIUtils.createLabel("Border Distance (pixels):", UIConstants.NORMAL_FONT_SIZE, null);
      add(borderDistanceLabel, gbc);

      gbc.gridx = 1;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1.0;
      borderDistanceSpinner =
          new JSpinner(new SpinnerNumberModel(MainSettings.DEFAULT_BORDER_DISTANCE, 0, 1000, 1));
      borderDistanceSpinner.setToolTipText("Distance from image borders to consider ROIs as ignore");
      add(borderDistanceSpinner, gbc);

      // Ignore color
      gbc.gridx = 0;
      gbc.gridy = 1;
      gbc.fill = GridBagConstraints.NONE;
      gbc.weightx = 0.0;
      JLabel ignoreColorLabel =
          UIUtils.createLabel("Ignore Color:", UIConstants.NORMAL_FONT_SIZE, null);
      add(ignoreColorLabel, gbc);

      gbc.gridx = 1;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1.0;
      ignoreColorButton = new JButton();
      ignoreColorButton.setPreferredSize(new Dimension(100, 30));
      ignoreColorButton.setBackground(MainSettings.DEFAULT_IGNORE_COLOR);
      ignoreColorButton.setToolTipText("Click to change ignore ROI color");
      ignoreColorButton.addActionListener(this::chooseIgnoreColor);
      add(ignoreColorButton, gbc);

      // Show ignored ROIs checkbox
      gbc.gridx = 0;
      gbc.gridy = 2;
      gbc.gridwidth = 2;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1.0;
      showIgnoredROIsCheckBox = new JCheckBox("Show ignored ROIs on image");
      showIgnoredROIsCheckBox.setSelected(MainSettings.DEFAULT_SHOW_IGNORE_ROIS);
      showIgnoredROIsCheckBox.setToolTipText("Whether to display ignored ROIs or hide them completely");
      add(showIgnoredROIsCheckBox, gbc);
    }

    private void chooseIgnoreColor(ActionEvent e) {
      Color currentColor = ignoreColorButton.getBackground();
      Color newColor =
          JColorChooser.showDialog(
              this, "Choose Ignore ROI Color", currentColor);
      if (newColor != null) {
        ignoreColorButton.setBackground(newColor);
      }
    }

    public void loadSettings(MainSettings.IgnoreROIAppearanceSettings settings) {
      borderDistanceSpinner.setValue(settings.borderDistance());
      ignoreColorButton.setBackground(settings.ignoreColor());
      showIgnoredROIsCheckBox.setSelected(settings.showIgnoredROIs());
    }

    public MainSettings.IgnoreROIAppearanceSettings createSettings() {
      return new MainSettings.IgnoreROIAppearanceSettings(
          (Integer) borderDistanceSpinner.getValue(),
          ignoreColorButton.getBackground(),
          MainSettings.DEFAULT_FILL_OPACITY,
          MainSettings.DEFAULT_BORDER_WIDTH,
          showIgnoredROIsCheckBox.isSelected());
    }

    public boolean validateInputs() {
      try {
        int borderDistance = (Integer) borderDistanceSpinner.getValue();

        if (borderDistance < 0) {
          showValidationError("Border distance must be non-negative");
          return false;
        }

        return true;
      } catch (Exception e) {
        showValidationError("Invalid settings: " + e.getMessage());
        return false;
      }
    }

    private void showValidationError(String message) {
      JOptionPane.showMessageDialog(
          this,
          "Ignore ROI Settings: " + message,
          "Invalid Input",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Static factory method for convenient usage.
   */
  public static void showDialog(
      Frame parent,
      ConfigurationManager configurationManager,
      Consumer<MainSettings> onSettingsChanged) {

    MainSettingsDialog dialog =
        new MainSettingsDialog(parent, configurationManager, onSettingsChanged);
    dialog.setVisible(true);
  }

  /**
   * Custom toggle switch component for CSV format selection.
   */
  private static class CsvFormatToggleSwitch extends JPanel {

    private boolean selected = false;
    private final int SWITCH_WIDTH = 60;
    private final int SWITCH_HEIGHT = 30;
    private final int CIRCLE_DIAMETER = 24;
    private final int ANIMATION_DURATION = 200; // milliseconds

    private Timer animationTimer;
    private long animationStartTime;
    private double currentPosition = 0.0; // 0.0 = left (US), 1.0 = right (EU)

    public CsvFormatToggleSwitch() {
      super();
      setPreferredSize(new Dimension(SWITCH_WIDTH + 80, SWITCH_HEIGHT)); // Extra space for labels
      setOpaque(false);
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

      addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
          toggleSelection();
        }
      });
    }

    public boolean isSelected() {
      return selected;
    }

    public void setSelected(boolean selected) {
      if (this.selected != selected) {
        this.selected = selected;
        startAnimation();
      }
    }

    private void toggleSelection() {
      setSelected(!selected);
    }

    private void startAnimation() {
      if (animationTimer != null) {
        animationTimer.stop();
      }

      animationStartTime = System.currentTimeMillis();
      currentPosition = selected ? 0.0 : 1.0; // Start from opposite of target

      animationTimer = new Timer(16, e -> { // ~60 FPS
        long elapsed = System.currentTimeMillis() - animationStartTime;
        double progress = Math.min(1.0, (double) elapsed / ANIMATION_DURATION);

        // Easing function for smooth animation
        progress = easeInOutCubic(progress);

        // Update position based on target
        double targetPosition = selected ? 1.0 : 0.0;
        currentPosition = currentPosition + (targetPosition - currentPosition) * progress;

        repaint();

        if (progress >= 1.0) {
          animationTimer.stop();
          animationTimer = null;
          currentPosition = targetPosition; // Ensure exact final position
        }
      });
      animationTimer.start();
    }

    private double easeInOutCubic(double t) {
      return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D) g.create();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      int centerY = getHeight() / 2;
      int switchY = centerY - SWITCH_HEIGHT / 2;

      // Draw background track
      g2d.setColor(selected ? new Color(76, 175, 80) : new Color(189, 189, 189));
      g2d.fillRoundRect(40, switchY, SWITCH_WIDTH, SWITCH_HEIGHT, SWITCH_HEIGHT, SWITCH_HEIGHT);

      // Draw circle
      int circleX = 40 + (int) (currentPosition * (SWITCH_WIDTH - CIRCLE_DIAMETER));
      int circleY = centerY - CIRCLE_DIAMETER / 2;
      g2d.setColor(Color.WHITE);
      g2d.fillOval(circleX, circleY, CIRCLE_DIAMETER, CIRCLE_DIAMETER);

      // Draw labels
      g2d.setColor(UIManager.getColor("Label.foreground"));
      g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 12f));

      // US label (left)
      FontMetrics fm = g2d.getFontMetrics();
      g2d.setColor(selected ? Color.GRAY : UIManager.getColor("Label.foreground"));
      g2d.drawString("US", 10, centerY + fm.getAscent() / 2);

      // EU label (right)
      g2d.setColor(selected ? UIManager.getColor("Label.foreground") : Color.GRAY);
      g2d.drawString("EU", 40 + SWITCH_WIDTH + 10, centerY + fm.getAscent() / 2);

      g2d.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(SWITCH_WIDTH + 80, SWITCH_HEIGHT);
    }
  }
}
