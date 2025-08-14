package com.scipath.scipathj.ui.dialogs.settings;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.config.MainSettings;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main settings dialog for global application configuration.
 * Provides UI controls for scale conversion, ROI appearance, and other global settings
 * with persistence and default value management.
 */
public class MainSettingsDialog extends JDialog {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainSettingsDialog.class);

  // Settings instances
  private MainSettings settings;
  private final ConfigurationManager configManager;

  // Scale conversion UI components
  private JSpinner pixelsPerMicrometerSpinner;
  private JTextField scaleUnitField;
  private JLabel scalePreviewLabel;

  // ROI appearance UI components for each category
  private final ROICategoryPanel vesselPanel;
  private final ROICategoryPanel nucleusPanel;
  private final ROICategoryPanel cytoplasmPanel;
  private final ROICategoryPanel cellPanel;

  // Result flag
  private boolean settingsChanged = false;

  public MainSettingsDialog(Frame parent, ConfigurationManager configurationManager) {
    super(parent, "Main Settings", true);
    this.configManager = configurationManager;
    this.settings = configManager.loadMainSettings();

    // Initialize ROI category panels
    this.vesselPanel = new ROICategoryPanel(MainSettings.ROICategory.VESSEL);
    this.nucleusPanel = new ROICategoryPanel(MainSettings.ROICategory.NUCLEUS);
    this.cytoplasmPanel = new ROICategoryPanel(MainSettings.ROICategory.CYTOPLASM);
    this.cellPanel = new ROICategoryPanel(MainSettings.ROICategory.CELL);

    // Load settings from file first
    // Note: loadMainSettings now needs to be updated to work with non-singleton MainSettings

    initializeDialog();
    loadCurrentSettings();
    updatePreview();
  }

  private void initializeDialog() {
    setSize(700, 600);
    setLocationRelativeTo(getParent());
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    // Title
    JLabel titleLabel =
        UIUtils.createBoldLabel("Main Application Settings", UIConstants.SUBTITLE_FONT_SIZE);
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
    JPanel mainPanel = new JPanel(new BorderLayout());

    // Create tabbed pane for different setting categories
    JTabbedPane tabbedPane = new JTabbedPane();

    // Scale conversion tab
    JPanel scalePanel = createScaleConversionPanel();
    tabbedPane.addTab("Scale & Units", scalePanel);

    // ROI appearance tabs for each category
    tabbedPane.addTab("Vessel ROIs", vesselPanel);
    tabbedPane.addTab("Nucleus ROIs", nucleusPanel);
    tabbedPane.addTab("Cytoplasm ROIs", cytoplasmPanel);
    tabbedPane.addTab("Cell ROIs", cellPanel);

    mainPanel.add(tabbedPane, BorderLayout.CENTER);

    return mainPanel;
  }

  private JPanel createScaleConversionPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createTitledBorder("Scale Conversion Settings"));

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
    JLabel pixelsPerMicrometerLabel =
        UIUtils.createLabel("Pixels per Micrometer:", UIConstants.NORMAL_FONT_SIZE, null);
    pixelsPerMicrometerLabel.setToolTipText("Number of pixels that represent one micrometer");
    panel.add(pixelsPerMicrometerLabel, gbc);

    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    pixelsPerMicrometerSpinner =
        new JSpinner(
            new SpinnerNumberModel(MainSettings.DEFAULT_PIXELS_PER_MICROMETER, 0.001, 1000.0, 0.1));
    pixelsPerMicrometerSpinner.setToolTipText("Number of pixels that represent one micrometer");
    pixelsPerMicrometerSpinner.addChangeListener(e -> updateScalePreview());
    panel.add(pixelsPerMicrometerSpinner, gbc);

    // Scale unit
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    JLabel scaleUnitLabel = UIUtils.createLabel("Scale Unit:", UIConstants.NORMAL_FONT_SIZE, null);
    scaleUnitLabel.setToolTipText("Unit of measurement for scale display");
    panel.add(scaleUnitLabel, gbc);

    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    scaleUnitField = new JTextField(MainSettings.DEFAULT_SCALE_UNIT, 10);
    scaleUnitField.setToolTipText("Unit of measurement for scale display");
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
    panel.add(scaleUnitField, gbc);

    // Scale preview
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    scalePreviewLabel = new JLabel("Preview: 100 pixels = 100.00 μm");
    scalePreviewLabel.setBorder(BorderFactory.createTitledBorder("Scale Preview"));
    scalePreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
    panel.add(scalePreviewLabel, gbc);

    return panel;
  }

  /**
   * Panel for configuring a specific ROI category
   */
  private class ROICategoryPanel extends JPanel {
    private final MainSettings.ROICategory category;
    private final JButton borderColorButton;
    private final JSlider fillOpacitySlider;
    private final JLabel fillOpacityValueLabel;
    private final JSpinner borderWidthSpinner;
    private ROIPreviewPanel previewPanel;

    public ROICategoryPanel(MainSettings.ROICategory category) {
      super(new GridBagLayout());
      this.category = category;

      setBorder(BorderFactory.createTitledBorder(category.getDisplayName() + " ROI Settings"));

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
      borderColorLabel.setToolTipText("Color of " + category.getDisplayName() + " ROI borders");
      add(borderColorLabel, gbc);

      gbc.gridx = 1;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1.0;
      borderColorButton = new JButton();
      borderColorButton.setPreferredSize(new Dimension(100, 30));
      borderColorButton.setToolTipText(
          "Click to change " + category.getDisplayName() + " ROI border color");
      borderColorButton.addActionListener(e -> chooseBorderColor());
      add(borderColorButton, gbc);

      // Fill opacity
      gbc.gridx = 0;
      gbc.gridy = 1;
      gbc.fill = GridBagConstraints.NONE;
      gbc.weightx = 0.0;
      JLabel fillOpacityLabel =
          UIUtils.createLabel("Fill Opacity:", UIConstants.NORMAL_FONT_SIZE, null);
      fillOpacityLabel.setToolTipText(
          "Opacity of "
              + category.getDisplayName()
              + " ROI fill (0% = transparent, 100% = opaque)");
      add(fillOpacityLabel, gbc);

      gbc.gridx = 1;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1.0;

      JPanel opacityPanel = new JPanel(new BorderLayout());

      fillOpacitySlider = new JSlider(0, 100, Math.round(category.getDefaultFillOpacity() * 100));
      fillOpacitySlider.setToolTipText("Opacity of " + category.getDisplayName() + " ROI fill");
      fillOpacitySlider.setMajorTickSpacing(25);
      fillOpacitySlider.setMinorTickSpacing(5);
      fillOpacitySlider.setPaintTicks(true);
      fillOpacitySlider.setPaintLabels(true);

      fillOpacityValueLabel = new JLabel(Math.round(category.getDefaultFillOpacity() * 100) + "%");
      fillOpacityValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
      fillOpacityValueLabel.setPreferredSize(new Dimension(40, 20));
      fillOpacitySlider.addChangeListener(
          e -> {
            int value = fillOpacitySlider.getValue();
            fillOpacityValueLabel.setText(value + "%");
            if (previewPanel != null) {
              previewPanel.repaint();
            }
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
      borderWidthLabel.setToolTipText(
          "Width of " + category.getDisplayName() + " ROI borders in pixels");
      add(borderWidthLabel, gbc);

      gbc.gridx = 1;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1.0;
      borderWidthSpinner =
          new JSpinner(new SpinnerNumberModel(category.getDefaultBorderWidth(), 1, 10, 1));
      borderWidthSpinner.setToolTipText(
          "Width of " + category.getDisplayName() + " ROI borders in pixels");
      borderWidthSpinner.addChangeListener(
          e -> {
            if (previewPanel != null) {
              previewPanel.repaint();
            }
          });
      add(borderWidthSpinner, gbc);

      // Preview
      gbc.gridx = 0;
      gbc.gridy = 3;
      gbc.gridwidth = 2;
      gbc.fill = GridBagConstraints.BOTH;
      gbc.weightx = 1.0;
      gbc.weighty = 1.0;
      previewPanel = new ROIPreviewPanel(this);
      previewPanel.setBorder(
          BorderFactory.createTitledBorder(category.getDisplayName() + " ROI Preview"));
      previewPanel.setPreferredSize(new Dimension(200, 150));
      add(previewPanel, gbc);
    }

    private void chooseBorderColor() {
      Color currentColor = borderColorButton.getBackground();
      Color newColor =
          JColorChooser.showDialog(
              MainSettingsDialog.this,
              "Choose " + category.getDisplayName() + " ROI Border Color",
              currentColor);
      if (newColor != null) {
        borderColorButton.setBackground(newColor);
        if (previewPanel != null) {
          previewPanel.repaint();
        }
      }
    }

    public void loadSettings(MainSettings.ROIAppearanceSettings settings) {
      borderColorButton.setBackground(settings.borderColor());
      fillOpacitySlider.setValue(Math.round(settings.fillOpacity() * 100));
      fillOpacityValueLabel.setText(Math.round(settings.fillOpacity() * 100) + "%");
      borderWidthSpinner.setValue(settings.borderWidth());
    }

    public void loadDefaults() {
      borderColorButton.setBackground(category.getDefaultBorderColor());
      fillOpacitySlider.setValue(Math.round(category.getDefaultFillOpacity() * 100));
      fillOpacityValueLabel.setText(Math.round(category.getDefaultFillOpacity() * 100) + "%");
      borderWidthSpinner.setValue(category.getDefaultBorderWidth());
    }

    public MainSettings.ROIAppearanceSettings createUpdatedSettings() {
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
          showErrorMessage(
              category.getDisplayName() + " ROI fill opacity must be between 0% and 100%");
          return false;
        }

        if (borderWidth < 1) {
          showErrorMessage(category.getDisplayName() + " ROI border width must be at least 1");
          return false;
        }

        return true;
      } catch (Exception e) {
        showErrorMessage(
            "Invalid " + category.getDisplayName() + " ROI settings: " + e.getMessage());
        return false;
      }
    }
  }

  private JPanel createButtonPanel() {
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    JButton resetButton = UIUtils.createStandardButton("Reset to Defaults", null);
    JButton cancelButton = UIUtils.createStandardButton("Cancel", null);
    JButton okButton = UIUtils.createStandardButton("OK", null);

    resetButton.addActionListener(new ResetToDefaultsAction());
    cancelButton.addActionListener(e -> dispose());
    okButton.addActionListener(new SaveSettingsAction());

    buttonPanel.add(resetButton);
    buttonPanel.add(cancelButton);
    buttonPanel.add(okButton);

    return buttonPanel;
  }

  /**
   * Load current settings into the UI components.
   */
  private void loadCurrentSettings() {
    try {
      // Load scale settings
      pixelsPerMicrometerSpinner.setValue(settings.getPixelsPerMicrometer());
      scaleUnitField.setText(settings.getScaleUnit());

      // Load ROI category settings
      vesselPanel.loadSettings(settings.getVesselSettings());
      nucleusPanel.loadSettings(settings.getNucleusSettings());
      cytoplasmPanel.loadSettings(settings.getCytoplasmSettings());
      cellPanel.loadSettings(settings.getCellSettings());

      LOGGER.debug("Loaded current settings into UI components: {}", settings.toString());
    } catch (Exception e) {
      LOGGER.error("Error loading settings into UI components", e);
      loadDefaultSettings();
    }
  }

  /**
   * Load default settings into the UI components.
   */
  private void loadDefaultSettings() {
    // Load scale defaults
    pixelsPerMicrometerSpinner.setValue(MainSettings.DEFAULT_PIXELS_PER_MICROMETER);
    scaleUnitField.setText(MainSettings.DEFAULT_SCALE_UNIT);

    // Load ROI category defaults
    vesselPanel.loadDefaults();
    nucleusPanel.loadDefaults();
    cytoplasmPanel.loadDefaults();
    cellPanel.loadDefaults();

    LOGGER.debug("Loaded default settings into UI components");
  }

  /**
   * Update the scale preview label.
   */
  private void updateScalePreview() {
    try {
      double pixelsPerMicrometer = (Double) pixelsPerMicrometerSpinner.getValue();
      String unit = scaleUnitField.getText().trim();
      if (unit.isEmpty()) unit = "μm";

      double micrometersFor100Pixels = 100.0 / pixelsPerMicrometer;
      scalePreviewLabel.setText(
          String.format("Preview: 100 pixels = %.2f %s", micrometersFor100Pixels, unit));
    } catch (Exception e) {
      scalePreviewLabel.setText("Preview: Invalid values");
    }
  }

  /**
   * Update both previews.
   */
  private void updatePreview() {
    updateScalePreview();
    // ROI previews are updated automatically by their panels
  }

  /**
   * Validate the current input values.
   *
   * @return true if all values are valid
   */
  private boolean validateInputs() {
    try {
      // Validate scale settings
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

      // Validate all ROI category settings
      if (!vesselPanel.validateInputs()) return false;
      if (!nucleusPanel.validateInputs()) return false;
      if (!cytoplasmPanel.validateInputs()) return false;
      if (!cellPanel.validateInputs()) return false;

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
   * Custom panel for ROI preview.
   */
  private class ROIPreviewPanel extends JPanel {
    private final ROICategoryPanel categoryPanel;

    public ROIPreviewPanel(ROICategoryPanel categoryPanel) {
      this.categoryPanel = categoryPanel;
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D) g.create();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      // Draw background
      g2d.setColor(Color.WHITE);
      g2d.fillRect(0, 0, getWidth(), getHeight());

      // Draw sample ROI
      int centerX = getWidth() / 2;
      int centerY = getHeight() / 2;
      int roiWidth = 80;
      int roiHeight = 60;

      // Get current settings from the category panel
      Color borderColor = categoryPanel.borderColorButton.getBackground();
      float opacity = categoryPanel.fillOpacitySlider.getValue() / 100.0f;
      int borderWidth = (Integer) categoryPanel.borderWidthSpinner.getValue();

      // Create fill color with current opacity
      Color fillColor =
          new Color(
              borderColor.getRed(),
              borderColor.getGreen(),
              borderColor.getBlue(),
              Math.round(opacity * 255));

      // Draw filled rectangle
      g2d.setColor(fillColor);
      g2d.fillRect(centerX - roiWidth / 2, centerY - roiHeight / 2, roiWidth, roiHeight);

      // Draw border
      g2d.setColor(borderColor);
      g2d.setStroke(new BasicStroke(borderWidth));
      g2d.drawRect(centerX - roiWidth / 2, centerY - roiHeight / 2, roiWidth, roiHeight);

      // Draw label
      g2d.setColor(Color.BLACK);
      g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
      String label = categoryPanel.category.getDisplayName() + " ROI";
      FontMetrics fm = g2d.getFontMetrics();
      int labelWidth = fm.stringWidth(label);
      g2d.drawString(label, centerX - labelWidth / 2, centerY + roiHeight / 2 + 20);

      g2d.dispose();
    }
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
        // Create new MainSettings with updated values
        MainSettings updatedSettings =
            new MainSettings(
                (Double) pixelsPerMicrometerSpinner.getValue(),
                scaleUnitField.getText().trim(),
                vesselPanel.createUpdatedSettings(),
                nucleusPanel.createUpdatedSettings(),
                cytoplasmPanel.createUpdatedSettings(),
                cellPanel.createUpdatedSettings());

        // Save to file
        configManager.saveMainSettings(updatedSettings);

        settingsChanged = true;
        LOGGER.info(
            "Main settings saved successfully for vessel, nucleus, cytoplasm, and cell ROI types");

        dispose();

      } catch (Exception ex) {
        LOGGER.error("Failed to save main settings", ex);
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
              MainSettingsDialog.this,
              "Are you sure you want to reset all settings to their default values?",
              "Reset to Defaults",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE);

      if (result == JOptionPane.YES_OPTION) {
        loadDefaultSettings();
        updatePreview();
        LOGGER.info("UI components reset to default values");
      }
    }
  }
}
