package com.scipath.scipathj.ui.dialogs.settings;

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

  // UI Components for ROI categories
  private ROICategoryPanel vesselPanel;
  private ROICategoryPanel nucleusPanel;
  private ROICategoryPanel cytoplasmPanel;
  private ROICategoryPanel cellPanel;

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
    tabbedPane.addTab("Vessel ROIs", vesselPanel);
    tabbedPane.addTab("Nucleus ROIs", nucleusPanel);
    tabbedPane.addTab("Cytoplasm ROIs", cytoplasmPanel);
    tabbedPane.addTab("Cell ROIs", cellPanel);

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
   * Create the button panel.
   */
  private JPanel createButtonPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    panel.setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING));

    JButton resetButton = UIUtils.createStandardButton("Reset to Defaults", null);
    JButton cancelButton = UIUtils.createStandardButton("Cancel", null);
    JButton okButton = UIUtils.createStandardButton("OK", null);

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
  }

  /**
   * Load current settings into UI components.
   */
  private void loadCurrentSettings() {
    pixelsPerMicrometerSpinner.setValue(currentSettings.pixelsPerMicrometer());
    scaleUnitField.setText(currentSettings.scaleUnit());

    vesselPanel.loadSettings(currentSettings.vesselSettings());
    nucleusPanel.loadSettings(currentSettings.nucleusSettings());
    cytoplasmPanel.loadSettings(currentSettings.cytoplasmSettings());
    cellPanel.loadSettings(currentSettings.cellSettings());

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

      return vesselPanel.validateInputs()
          && nucleusPanel.validateInputs()
          && cytoplasmPanel.validateInputs()
          && cellPanel.validateInputs();

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

    return new MainSettings(
        pixelsPerMicrometer,
        scaleUnit,
        vesselPanel.createSettings(),
        nucleusPanel.createSettings(),
        cytoplasmPanel.createSettings(),
        cellPanel.createSettings());
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
}
