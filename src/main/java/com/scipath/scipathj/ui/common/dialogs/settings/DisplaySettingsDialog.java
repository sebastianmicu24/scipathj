package com.scipath.scipathj.ui.common.dialogs.settings;

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
 * Dialog for configuring ROI display settings (colors, opacity, border width).
 * Contains settings for all ROI categories: Vessel, Nucleus, Cytoplasm, Cell, and Ignore.
 */
public class DisplaySettingsDialog extends JDialog {

  private static final Logger LOGGER = LoggerFactory.getLogger(DisplaySettingsDialog.class);

  // Dependencies injected via constructor
  private final MainSettings mainSettings;
  private final Consumer<MainSettings> onSettingsChanged;

  // UI Components for ROI categories
  private ROICategoryPanel vesselPanel;
  private ROICategoryPanel nucleusPanel;
  private ROICategoryPanel cytoplasmPanel;
  private ROICategoryPanel cellPanel;
  private IgnoreROIPanel ignorePanel;

  // Result tracking
  private boolean settingsChanged = false;

  public DisplaySettingsDialog(Frame parent, MainSettings mainSettings, Consumer<MainSettings> onSettingsChanged) {
    super(parent, "Display Settings", true);
    this.mainSettings = mainSettings;
    this.onSettingsChanged = onSettingsChanged;

    initializeComponents();
    layoutComponents();
    loadCurrentSettings();
    setupEventHandlers();

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setSize(700, 600);
    setLocationRelativeTo(parent);

    LOGGER.info("Created Display Settings dialog");
  }

  private void initializeComponents() {
    // ROI category panels
    vesselPanel = new ROICategoryPanel(MainSettings.ROICategory.VESSEL);
    nucleusPanel = new ROICategoryPanel(MainSettings.ROICategory.NUCLEUS);
    cytoplasmPanel = new ROICategoryPanel(MainSettings.ROICategory.CYTOPLASM);
    cellPanel = new ROICategoryPanel(MainSettings.ROICategory.CELL);
    ignorePanel = new IgnoreROIPanel();
  }

  private void layoutComponents() {
    setLayout(new BorderLayout());

    // Title
    JLabel titleLabel = UIUtils.createBoldLabel("ROI Display Settings", UIConstants.SUBTITLE_FONT_SIZE);
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    titleLabel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));
    add(titleLabel, BorderLayout.NORTH);

    // Main content with tabs
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Vessel ROIs", vesselPanel);
    tabbedPane.addTab("Nucleus ROIs", nucleusPanel);
    tabbedPane.addTab("Cytoplasm ROIs", cytoplasmPanel);
    tabbedPane.addTab("Cell ROIs", cellPanel);
    tabbedPane.addTab("Ignore ROIs", ignorePanel);
    add(tabbedPane, BorderLayout.CENTER);

    // Button panel
    JPanel buttonPanel = createButtonPanel();
    add(buttonPanel, BorderLayout.SOUTH);
  }

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

  private void setupEventHandlers() {
    // No live updates needed for display settings
  }

  private void loadCurrentSettings() {
    vesselPanel.loadSettings(mainSettings.vesselSettings());
    nucleusPanel.loadSettings(mainSettings.nucleusSettings());
    cytoplasmPanel.loadSettings(mainSettings.cytoplasmSettings());
    cellPanel.loadSettings(mainSettings.cellSettings());
    ignorePanel.loadSettings(mainSettings.ignoreSettings());

    LOGGER.debug("Loaded current settings into display dialog components");
  }

  private boolean validateInputs() {
    return vesselPanel.validateInputs()
        && nucleusPanel.validateInputs()
        && cytoplasmPanel.validateInputs()
        && cellPanel.validateInputs()
        && ignorePanel.validateInputs();
  }

  private MainSettings createUpdatedSettings() {
    return new MainSettings(
        mainSettings.pixelsPerMicrometer(),
        mainSettings.scaleUnit(),
        vesselPanel.createSettings(),
        nucleusPanel.createSettings(),
        cytoplasmPanel.createSettings(),
        cellPanel.createSettings(),
        ignorePanel.createSettings(),
        mainSettings.useEuCsvFormat(),
        mainSettings.enableIgnoreFunctionality(),
        mainSettings.includeIgnoredInCsv());
  }

  private void handleOK(ActionEvent e) {
    if (!validateInputs()) {
      return;
    }

    try {
      MainSettings updatedSettings = createUpdatedSettings();

      settingsChanged = true;
      LOGGER.info("Display settings saved successfully");

      // Notify callback if provided
      if (onSettingsChanged != null) {
        onSettingsChanged.accept(updatedSettings);
      }

      dispose();

    } catch (Exception ex) {
      LOGGER.error("Failed to save display settings", ex);
      JOptionPane.showMessageDialog(
          this, "Failed to save settings: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void handleCancel(ActionEvent e) {
    dispose();
  }

  private void handleResetToDefaults(ActionEvent e) {
    int result = JOptionPane.showConfirmDialog(
        this,
        "Reset all display settings to their default values?",
        "Reset to Defaults",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE);

    if (result == JOptionPane.YES_OPTION) {
      MainSettings defaultSettings = MainSettings.createDefault();
      vesselPanel.loadSettings(defaultSettings.vesselSettings());
      nucleusPanel.loadSettings(defaultSettings.nucleusSettings());
      cytoplasmPanel.loadSettings(defaultSettings.cytoplasmSettings());
      cellPanel.loadSettings(defaultSettings.cellSettings());
      ignorePanel.loadSettings(defaultSettings.ignoreSettings());

      LOGGER.info("Display settings reset to defaults");
    }
  }

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

      setBorder(BorderFactory.createCompoundBorder(
          UIUtils.createPadding(UIConstants.LARGE_SPACING),
          BorderFactory.createTitledBorder(category.getDisplayName() + " ROI Settings")));

      GridBagConstraints gbc = new GridBagConstraints();
      gbc.insets = new Insets(UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING,
                             UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING);
      gbc.anchor = GridBagConstraints.WEST;

      // Border color
      gbc.gridx = 0;
      gbc.gridy = 0;
      JLabel borderColorLabel = UIUtils.createLabel("Border Color:", UIConstants.NORMAL_FONT_SIZE, null);
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
      JLabel fillOpacityLabel = UIUtils.createLabel("Fill Opacity:", UIConstants.NORMAL_FONT_SIZE, null);
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
      fillOpacitySlider.addChangeListener(e -> {
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
      JLabel borderWidthLabel = UIUtils.createLabel("Border Width:", UIConstants.NORMAL_FONT_SIZE, null);
      add(borderWidthLabel, gbc);

      gbc.gridx = 1;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1.0;
      borderWidthSpinner = new JSpinner(new SpinnerNumberModel(category.getDefaultBorderWidth(), 1, 10, 1));
      borderWidthSpinner.setToolTipText("Border width in pixels");
      add(borderWidthSpinner, gbc);
    }

    private void chooseBorderColor(ActionEvent e) {
      Color currentColor = borderColorButton.getBackground();
      Color newColor = JColorChooser.showDialog(
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
          JOptionPane.showMessageDialog(
              this,
              category.getDisplayName() + " ROI: Fill opacity must be between 0% and 100%",
              "Invalid Input",
              JOptionPane.ERROR_MESSAGE);
          return false;
        }

        if (borderWidth < 1) {
          JOptionPane.showMessageDialog(
              this,
              category.getDisplayName() + " ROI: Border width must be at least 1",
              "Invalid Input",
              JOptionPane.ERROR_MESSAGE);
          return false;
        }

        return true;
      } catch (Exception e) {
        JOptionPane.showMessageDialog(
            this,
            category.getDisplayName() + " ROI: Invalid settings: " + e.getMessage(),
            "Invalid Input",
            JOptionPane.ERROR_MESSAGE);
        return false;
      }
    }
  }

  /**
   * Panel for configuring ignore ROI settings.
   */
  private static class IgnoreROIPanel extends JPanel {

    private final JButton ignoreColorButton;
    private final JSlider fillOpacitySlider;
    private final JLabel fillOpacityValueLabel;
    private final JSpinner borderWidthSpinner;
    private final JCheckBox showIgnoredROIsCheckBox;

    public IgnoreROIPanel() {
      super(new GridBagLayout());
      setBorder(BorderFactory.createCompoundBorder(
          UIUtils.createPadding(UIConstants.LARGE_SPACING),
          BorderFactory.createTitledBorder("Ignore ROI Settings")));

      GridBagConstraints gbc = new GridBagConstraints();
      gbc.insets = new Insets(UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING,
                             UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING);
      gbc.anchor = GridBagConstraints.WEST;

      // Ignore color
      gbc.gridx = 0;
      gbc.gridy = 0;
      JLabel ignoreColorLabel = UIUtils.createLabel("Ignore Color:", UIConstants.NORMAL_FONT_SIZE, null);
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

      // Fill opacity
      gbc.gridx = 0;
      gbc.gridy = 1;
      gbc.fill = GridBagConstraints.NONE;
      gbc.weightx = 0.0;
      JLabel fillOpacityLabel = UIUtils.createLabel("Fill Opacity:", UIConstants.NORMAL_FONT_SIZE, null);
      add(fillOpacityLabel, gbc);

      gbc.gridx = 1;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1.0;

      JPanel opacityPanel = new JPanel(new BorderLayout());
      fillOpacitySlider = new JSlider(0, 100, Math.round(MainSettings.DEFAULT_FILL_OPACITY * 100));
      fillOpacitySlider.setMajorTickSpacing(25);
      fillOpacitySlider.setMinorTickSpacing(5);
      fillOpacitySlider.setPaintTicks(true);
      fillOpacitySlider.setPaintLabels(true);

      fillOpacityValueLabel = new JLabel("20%");
      fillOpacityValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
      fillOpacityValueLabel.setPreferredSize(new Dimension(40, 20));
      fillOpacitySlider.addChangeListener(e -> {
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
      JLabel borderWidthLabel = UIUtils.createLabel("Border Width:", UIConstants.NORMAL_FONT_SIZE, null);
      add(borderWidthLabel, gbc);

      gbc.gridx = 1;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1.0;
      borderWidthSpinner = new JSpinner(new SpinnerNumberModel(MainSettings.DEFAULT_BORDER_WIDTH, 1, 10, 1));
      borderWidthSpinner.setToolTipText("Border width in pixels");
      add(borderWidthSpinner, gbc);

      // Show ignored ROIs checkbox
      gbc.gridx = 0;
      gbc.gridy = 3;
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
      Color newColor = JColorChooser.showDialog(
          this, "Choose Ignore ROI Color", currentColor);
      if (newColor != null) {
        ignoreColorButton.setBackground(newColor);
      }
    }

    public void loadSettings(MainSettings.IgnoreROIAppearanceSettings settings) {
      ignoreColorButton.setBackground(settings.ignoreColor());
      fillOpacitySlider.setValue(Math.round(settings.fillOpacity() * 100));
      fillOpacityValueLabel.setText(Math.round(settings.fillOpacity() * 100) + "%");
      borderWidthSpinner.setValue(settings.borderWidth());
      showIgnoredROIsCheckBox.setSelected(settings.showIgnoredROIs());
    }

    public MainSettings.IgnoreROIAppearanceSettings createSettings() {
      return new MainSettings.IgnoreROIAppearanceSettings(
          10, // Border distance not used in display settings
          ignoreColorButton.getBackground(),
          fillOpacitySlider.getValue() / 100.0f,
          (Integer) borderWidthSpinner.getValue(),
          showIgnoredROIsCheckBox.isSelected());
    }

    public boolean validateInputs() {
      return true; // No validation needed for display settings
    }
  }

  /**
   * Static factory method for convenient usage.
   */
  public static void showDialog(Frame parent, MainSettings mainSettings, Consumer<MainSettings> onSettingsChanged) {
    DisplaySettingsDialog dialog = new DisplaySettingsDialog(parent, mainSettings, onSettingsChanged);
    dialog.setVisible(true);
  }
}