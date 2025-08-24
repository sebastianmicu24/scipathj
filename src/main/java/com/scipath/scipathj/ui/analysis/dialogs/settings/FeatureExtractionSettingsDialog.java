package com.scipath.scipathj.ui.analysis.dialogs.settings;

import com.scipath.scipathj.analysis.config.FeatureExtractionSettings;
import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class FeatureExtractionSettingsDialog extends JDialog {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureExtractionSettingsDialog.class);

  // UI Components for settings - individual feature checkboxes
  private List<JCheckBox> morphologicalCheckBoxes;
  private List<JCheckBox> intensityCheckBoxes;
  private List<JCheckBox> spatialCheckBoxes;
  private List<JCheckBox> stainSpecificCheckBoxes;
  private JCheckBox performanceOptimizationsCheckBox;
  private JSpinner spatialGridSizeSpinner;
  private JSpinner batchSizeSpinner;
  private JCheckBox sortROIsCheckBox;

  private FeatureExtractionSettings currentSettings;
  private ConfigurationManager configurationManager;

  public FeatureExtractionSettingsDialog(Frame parent) {
    super(parent, "Feature Extraction Settings", true);
    this.configurationManager = new ConfigurationManager();
    loadCurrentSettings();
    initializeDialog();
  }

  private void initializeDialog() {
    setSize(450, 350);
    setLocationRelativeTo(getParent());

    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    JLabel titleLabel =
        UIUtils.createBoldLabel("Feature Extraction Settings", UIConstants.SUBTITLE_FONT_SIZE);
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    contentPanel.add(titleLabel, BorderLayout.NORTH);

    JPanel settingsPanel = createSettingsPanel();
    contentPanel.add(new JScrollPane(settingsPanel), BorderLayout.CENTER);
    contentPanel.add(createButtonPanel(), BorderLayout.SOUTH);
    add(contentPanel);
  }

  private void loadCurrentSettings() {
    // Load current settings from configuration manager
    currentSettings = configurationManager.loadFeatureExtractionSettings();
  }

  private JPanel createSettingsPanel() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

    // Create tabbed pane for different region types
    JTabbedPane tabbedPane = new JTabbedPane();

    // Cell features tab
    JPanel cellPanel = createRegionFeaturePanel("Cell Features", currentSettings.cellFeatures());
    tabbedPane.addTab("Cell", cellPanel);

    // Nucleus features tab
    JPanel nucleusPanel = createRegionFeaturePanel("Nucleus Features", currentSettings.nucleusFeatures());
    tabbedPane.addTab("Nucleus", nucleusPanel);

    // Cytoplasm features tab
    JPanel cytoplasmPanel = createRegionFeaturePanel("Cytoplasm Features", currentSettings.cytoplasmFeatures());
    tabbedPane.addTab("Cytoplasm", cytoplasmPanel);

    // Vessel features tab
    JPanel vesselPanel = createRegionFeaturePanel("Vessel Features", currentSettings.vesselFeatures());
    tabbedPane.addTab("Vessel", vesselPanel);

    // Performance Settings Section
    JPanel performancePanel = createPerformanceSettingsSection();

    // Add all sections to main panel
    mainPanel.add(tabbedPane);
    mainPanel.add(Box.createVerticalStrut(UIConstants.MEDIUM_SPACING));
    mainPanel.add(performancePanel);

    // Wrap in scroll pane
    JScrollPane scrollPane = new JScrollPane(mainPanel);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    JPanel container = new JPanel(new BorderLayout());
    container.add(scrollPane, BorderLayout.CENTER);
    return container;
  }

  private JPanel createRegionFeaturePanel(String title, java.util.Map<String, Boolean> features) {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout());

    // Title label with bigger font inside the panel
    JLabel titleLabel = new JLabel(title);
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, UIConstants.SUBTITLE_FONT_SIZE));
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    titleLabel.setBorder(BorderFactory.createEmptyBorder(UIConstants.SMALL_SPACING, 0, UIConstants.MEDIUM_SPACING, 0));
    mainPanel.add(titleLabel, BorderLayout.NORTH);

    // Content panel
    JPanel contentPanel = new JPanel();
    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

    // Group features by category
    java.util.Map<String, java.util.List<String>> featureGroups = groupFeaturesByCategory(features);

    for (java.util.Map.Entry<String, java.util.List<String>> entry : featureGroups.entrySet()) {
      String category = entry.getKey();
      java.util.List<String> featureList = entry.getValue();

      JPanel categoryPanel = new JPanel();
      categoryPanel.setLayout(new BoxLayout(categoryPanel, BoxLayout.Y_AXIS));
      categoryPanel.setBorder(BorderFactory.createTitledBorder(category));

      // Group checkbox to select/deselect all in category
      JCheckBox groupCheckBox = new JCheckBox("Select All " + category);
      groupCheckBox.setFont(groupCheckBox.getFont().deriveFont(Font.BOLD));

      // Add checkboxes in a grid layout
      JPanel checkboxPanel = new JPanel(new GridLayout(0, 2, 5, 2)); // 2 columns

      java.util.List<JCheckBox> categoryCheckBoxes = new java.util.ArrayList<>();
      for (String featureName : featureList) {
        boolean isEnabled = features.getOrDefault(featureName, false);
        JCheckBox checkBox = new JCheckBox(formatFeatureName(featureName), isEnabled);
        checkBox.setToolTipText("Enable extraction of " + featureName.toLowerCase());
        checkBox.addActionListener(e -> {
          updateFeatureState(title, featureName, checkBox.isSelected());
          // Reset group checkbox text and color when individual checkbox changes
          groupCheckBox.setText("Select All " + category);
          groupCheckBox.setForeground(Color.BLACK);
          updateGroupCheckbox(groupCheckBox, categoryCheckBoxes);
        });
        categoryCheckBoxes.add(checkBox);
        checkboxPanel.add(checkBox);
      }

      // Initialize group checkbox state
      updateGroupCheckbox(groupCheckBox, categoryCheckBoxes);

      // Add listener to group checkbox
      groupCheckBox.addActionListener(e -> {
        boolean selected = groupCheckBox.isSelected();
        toggleCategoryFeatures(featureList, selected, title);
        // Update all individual checkboxes
        for (JCheckBox cb : categoryCheckBoxes) {
          cb.setSelected(selected);
        }
        // Reset text and color
        groupCheckBox.setText("Select All " + category);
        groupCheckBox.setForeground(Color.BLACK);
        // Force update of group checkbox state after toggling
        updateGroupCheckbox(groupCheckBox, categoryCheckBoxes);
      });

      categoryPanel.add(groupCheckBox);
      categoryPanel.add(Box.createVerticalStrut(UIConstants.SMALL_SPACING));
      categoryPanel.add(checkboxPanel);
      contentPanel.add(categoryPanel);
      contentPanel.add(Box.createVerticalStrut(UIConstants.MEDIUM_SPACING));
    }

    mainPanel.add(contentPanel, BorderLayout.CENTER);
    return mainPanel;
  }

  private void toggleCategoryFeatures(java.util.List<String> featureList, boolean selected, String regionType) {
    for (String featureName : featureList) {
      updateFeatureState(regionType, featureName, selected);
    }
  }

  private void updateGroupCheckbox(JCheckBox groupCheckBox, java.util.List<JCheckBox> categoryCheckBoxes) {
    if (categoryCheckBoxes.isEmpty()) {
      groupCheckBox.setSelected(false);
      return;
    }

    boolean allSelected = categoryCheckBoxes.stream().allMatch(JCheckBox::isSelected);
    boolean noneSelected = categoryCheckBoxes.stream().noneMatch(JCheckBox::isSelected);

    if (allSelected) {
      groupCheckBox.setSelected(true);
      groupCheckBox.setText("Select All (All Selected)");
    } else if (noneSelected) {
      groupCheckBox.setSelected(false);
      groupCheckBox.setText("Select All (None Selected)");
    } else {
      // Indeterminate state - some selected, some not
      groupCheckBox.setSelected(false);
      groupCheckBox.setText("Select All (Some Selected)");
      // Make it visually distinct
      groupCheckBox.setForeground(Color.GRAY);
    }
  }

  private java.util.Map<String, java.util.List<String>> groupFeaturesByCategory(java.util.Map<String, Boolean> features) {
    java.util.Map<String, java.util.List<String>> groups = new java.util.LinkedHashMap<>();

    for (String feature : features.keySet()) {
      String category = categorizeFeature(feature);
      groups.computeIfAbsent(category, k -> new java.util.ArrayList<>()).add(feature);
    }

    return groups;
  }

  private String categorizeFeature(String feature) {
    if (feature.contains("vessel") || feature.contains("neighbor")) {
      return "Spatial Features";
    } else if (feature.contains("hema") || feature.contains("eosin") || feature.contains("ratio") || feature.contains("score") ||
               feature.contains("h_e") || feature.contains("h-score")) {
      return "H&E Stain-Specific Features";
    } else if (feature.contains("mean") || feature.contains("std") || feature.contains("min") || feature.contains("max") ||
               feature.contains("median") || feature.contains("skew") || feature.contains("kurt") || feature.contains("intden")) {
      return "Intensity Features";
    } else {
      return "Morphological Features";
    }
  }

  private String formatFeatureName(String featureName) {
    // Convert snake_case to Title Case
    String[] parts = featureName.split("_");
    StringBuilder result = new StringBuilder();
    for (String part : parts) {
      if (result.length() > 0) {
        result.append(" ");
      }
      result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
    }

    String formatted = result.toString();

    // Add helpful descriptions for H&E features
    if (featureName.contains("hema")) {
      formatted += " (Hematoxylin - Nuclei)";
    } else if (featureName.contains("eosin")) {
      formatted += " (Eosin - Cytoplasm)";
    } else if (featureName.contains("h_e_ratio")) {
      formatted += " (H/E Ratio)";
    } else if (featureName.contains("h_score")) {
      formatted += " (H-Score)";
    }

    return formatted;
  }

  private void updateFeatureState(String regionType, String featureName, boolean enabled) {
    // Update the current settings in memory
    java.util.Map<String, Boolean> regionFeatures = getRegionFeatures(regionType);
    if (regionFeatures != null) {
      // Create a new map with the updated feature
      java.util.Map<String, Boolean> updatedFeatures = new java.util.HashMap<>(regionFeatures);
      updatedFeatures.put(featureName, enabled);

      // Update the settings with the new map (we'll need to create a new settings object)
      updateSettingsWithNewFeatures(regionType, updatedFeatures);
    }

    LOGGER.debug("Updated feature {} in region {} to {}", featureName, regionType, enabled);
  }

  private java.util.Map<String, Boolean> getRegionFeatures(String regionType) {
    return switch (regionType) {
      case "Cell Features" -> currentSettings.cellFeatures();
      case "Nucleus Features" -> currentSettings.nucleusFeatures();
      case "Cytoplasm Features" -> currentSettings.cytoplasmFeatures();
      case "Vessel Features" -> currentSettings.vesselFeatures();
      default -> null;
    };
  }

  private void updateSettingsWithNewFeatures(String regionType, java.util.Map<String, Boolean> newFeatures) {
    // Create new settings with updated feature map
    java.util.Map<String, Boolean> cellFeatures = "Cell Features".equals(regionType) ? newFeatures : currentSettings.cellFeatures();
    java.util.Map<String, Boolean> nucleusFeatures = "Nucleus Features".equals(regionType) ? newFeatures : currentSettings.nucleusFeatures();
    java.util.Map<String, Boolean> cytoplasmFeatures = "Cytoplasm Features".equals(regionType) ? newFeatures : currentSettings.cytoplasmFeatures();
    java.util.Map<String, Boolean> vesselFeatures = "Vessel Features".equals(regionType) ? newFeatures : currentSettings.vesselFeatures();

    currentSettings = new FeatureExtractionSettings(
        cellFeatures, nucleusFeatures, cytoplasmFeatures, vesselFeatures,
        currentSettings.enablePerformanceOptimizations(),
        currentSettings.spatialGridSize(),
        currentSettings.batchSize(),
        currentSettings.sortROIs());
  }

  private JPanel createPerformanceSettingsSection() {
    JPanel section = new JPanel();
    section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
    section.setBorder(BorderFactory.createTitledBorder("Performance Settings"));

    performanceOptimizationsCheckBox = new JCheckBox("Enable Performance Optimizations",
                                                   currentSettings.enablePerformanceOptimizations());
    spatialGridSizeSpinner = new JSpinner(new SpinnerNumberModel(currentSettings.spatialGridSize(), 1, 500, 10));
    batchSizeSpinner = new JSpinner(new SpinnerNumberModel(currentSettings.batchSize(), 1, 1000, 50));
    sortROIsCheckBox = new JCheckBox("Sort ROIs", currentSettings.sortROIs());

    performanceOptimizationsCheckBox.setToolTipText("Enable spatial indexing and caching optimizations");
    spatialGridSizeSpinner.setToolTipText("Grid size for spatial indexing (pixels)");
    batchSizeSpinner.setToolTipText("Number of ROIs to process in each batch");
    sortROIsCheckBox.setToolTipText("Sort ROIs by name for consistent output ordering");

    JPanel settingsPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(2, 5, 2, 5);
    gbc.anchor = GridBagConstraints.WEST;

    gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
    settingsPanel.add(performanceOptimizationsCheckBox, gbc);

    gbc.gridwidth = 1; gbc.gridy = 1;
    settingsPanel.add(UIUtils.createLabel("Spatial Grid Size:", UIConstants.NORMAL_FONT_SIZE, null), gbc);
    gbc.gridx = 1;
    settingsPanel.add(spatialGridSizeSpinner, gbc);

    gbc.gridx = 0; gbc.gridy = 2;
    settingsPanel.add(UIUtils.createLabel("Batch Size:", UIConstants.NORMAL_FONT_SIZE, null), gbc);
    gbc.gridx = 1;
    settingsPanel.add(batchSizeSpinner, gbc);

    gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
    settingsPanel.add(sortROIsCheckBox, gbc);

    section.add(settingsPanel);
    return section;
  }

  private JPanel createButtonPanel() {
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton okButton = UIUtils.createStandardButton("OK", null);
    JButton cancelButton = UIUtils.createStandardButton("Cancel", null);
    JButton resetButton = UIUtils.createStandardButton("Reset to Defaults", null);

    okButton.addActionListener(e -> saveAndClose());
    cancelButton.addActionListener(e -> dispose());
    resetButton.addActionListener(e -> resetToDefaults());

    buttonPanel.add(resetButton);
    buttonPanel.add(cancelButton);
    buttonPanel.add(okButton);
    return buttonPanel;
  }

  private void saveAndClose() {
    try {
      // Save the current settings to configuration manager
      configurationManager.saveFeatureExtractionSettings(currentSettings);
      LOGGER.info("Feature extraction settings saved successfully");
      dispose();
    } catch (Exception e) {
      LOGGER.error("Failed to save feature extraction settings", e);
      JOptionPane.showMessageDialog(this, "Failed to save settings: " + e.getMessage(),
          "Save Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private boolean getMorphologicalValue(int index) {
    return index < morphologicalCheckBoxes.size() ? morphologicalCheckBoxes.get(index).isSelected() : true;
  }

  private boolean getIntensityValue(int index) {
    return index < intensityCheckBoxes.size() ? intensityCheckBoxes.get(index).isSelected() : true;
  }

  private boolean getSpatialValue(int index) {
    return index < spatialCheckBoxes.size() ? spatialCheckBoxes.get(index).isSelected() : true;
  }

  private boolean getStainValue(int index) {
    return index < stainSpecificCheckBoxes.size() ? stainSpecificCheckBoxes.get(index).isSelected() : false;
  }

  private void resetToDefaults() {
    FeatureExtractionSettings defaults = FeatureExtractionSettings.createDefault();

    // Reset morphological checkboxes
    for (int i = 0; i < morphologicalCheckBoxes.size(); i++) {
      boolean defaultValue = getDefaultMorphologicalValue(i);
      morphologicalCheckBoxes.get(i).setSelected(defaultValue);
    }

    // Reset intensity checkboxes
    for (int i = 0; i < intensityCheckBoxes.size(); i++) {
      boolean defaultValue = getDefaultIntensityValue(i);
      intensityCheckBoxes.get(i).setSelected(defaultValue);
    }

    // Reset spatial checkboxes
    for (int i = 0; i < spatialCheckBoxes.size(); i++) {
      boolean defaultValue = getDefaultSpatialValue(i);
      spatialCheckBoxes.get(i).setSelected(defaultValue);
    }

    // Reset stain checkboxes
    for (int i = 0; i < stainSpecificCheckBoxes.size(); i++) {
      boolean defaultValue = getDefaultStainValue(i);
      stainSpecificCheckBoxes.get(i).setSelected(defaultValue);
    }

    performanceOptimizationsCheckBox.setSelected(defaults.enablePerformanceOptimizations());
    spatialGridSizeSpinner.setValue(defaults.spatialGridSize());
    batchSizeSpinner.setValue(defaults.batchSize());
    sortROIsCheckBox.setSelected(defaults.sortROIs());
  }

  private boolean getDefaultMorphologicalValue(int index) {
    return switch (index) {
      case 0 -> true; // area
      case 1 -> true; // perimeter
      case 2 -> true; // circularity
      case 3 -> true; // aspectRatio
      case 4 -> true; // solidity
      case 5 -> true; // centroidX
      case 6 -> true; // centroidY
      case 7 -> true; // feretDiameter
      case 8 -> true; // feretAngle
      case 9 -> true; // minFeret
      case 10 -> true; // feretX
      case 11 -> true; // feretY
      case 12 -> true; // majorAxis
      case 13 -> true; // minorAxis
      case 14 -> true; // ellipseAngle
      case 15 -> true; // roundness
      case 16 -> true; // centerMassX
      case 17 -> true; // centerMassY
      default -> true;
    };
  }

  private boolean getDefaultIntensityValue(int index) {
    return switch (index) {
      case 0 -> true; // meanIntensity
      case 1 -> true; // stdIntensity
      case 2 -> true; // minIntensity
      case 3 -> true; // maxIntensity
      case 4 -> true; // medianIntensity
      case 5 -> true; // integratedDensity
      case 6 -> true; // skewness
      case 7 -> true; // kurtosis
      default -> true;
    };
  }

  private boolean getDefaultSpatialValue(int index) {
    return switch (index) {
      case 0 -> true; // distanceToNearestVessel
      case 1 -> true; // neighborCount
      case 2 -> true; // distanceToNearestNeighbor
      case 3 -> true; // isBorderROI
      default -> true;
    };
  }

  private boolean getDefaultStainValue(int index) {
    return switch (index) {
      case 0 -> false; // hematoxylinMeanIntensity
      case 1 -> false; // eosinMeanIntensity
      case 2 -> false; // hERatio
      case 3 -> false; // hScore
      default -> false;
    };
  }
}
