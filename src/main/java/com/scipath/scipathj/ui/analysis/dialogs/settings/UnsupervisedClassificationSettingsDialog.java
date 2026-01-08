package com.scipath.scipathj.ui.analysis.dialogs.settings;

import com.scipath.scipathj.analysis.algorithms.classification.CellFeatureAggregator;
import com.scipath.scipathj.analysis.config.UnsupervisedClassificationSettings;
import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Dialog for configuring unsupervised classification settings.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class UnsupervisedClassificationSettingsDialog extends JDialog {

  private final ConfigurationManager configManager;
  private UnsupervisedClassificationSettings currentSettings;

  // UI Components
  private JCheckBox enabledCheckBox;
  private JComboBox<String> algorithmComboBox;
  private JSpinner kSpinner;
  private JSpinner maxIterationsSpinner;
  private JSpinner epsilonSpinner;
  private JSpinner minPointsSpinner;
  private List<JCheckBox> featureCheckboxes;

  // Panels for algorithm-specific settings
  private JPanel kMeansPanel;
  private JPanel dbscanPanel;

  public UnsupervisedClassificationSettingsDialog(Frame parent, ConfigurationManager configManager) {
    super(parent, "Unsupervised Classification Settings", true);
    this.configManager = configManager;
    this.currentSettings = configManager.loadUnsupervisedClassificationSettings();
    this.featureCheckboxes = new ArrayList<>();
    
    initializeDialog();
  }

  private void initializeDialog() {
    setSize(600, 500);
    setLocationRelativeTo(getParent());

    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    // Title
    JLabel titleLabel = UIUtils.createBoldLabel("Unsupervised Classification (Clustering)", UIConstants.SUBTITLE_FONT_SIZE);
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    contentPanel.add(titleLabel, BorderLayout.NORTH);

    // Settings Panel
    JPanel settingsPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    // Enabled Checkbox
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    enabledCheckBox = new JCheckBox("Enable Unsupervised Classification");
    enabledCheckBox.setSelected(currentSettings.enabled());
    enabledCheckBox.addActionListener(e -> updateControls());
    settingsPanel.add(enabledCheckBox, gbc);

    // Algorithm Selection
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    settingsPanel.add(new JLabel("Clustering Algorithm:"), gbc);

    gbc.gridx = 1;
    String[] algorithms = {"K-Means", "DBSCAN"};
    algorithmComboBox = new JComboBox<>(algorithms);
    algorithmComboBox.setSelectedItem(currentSettings.algorithm());
    algorithmComboBox.addActionListener(e -> updateAlgorithmPanels());
    settingsPanel.add(algorithmComboBox, gbc);

    // Algorithm Specific Panels
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    
    // K-Means Panel
    kMeansPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    kMeansPanel.add(new JLabel("Number of Clusters (K):"));
    kSpinner = new JSpinner(new SpinnerNumberModel(currentSettings.k(), 2, 50, 1));
    kMeansPanel.add(kSpinner);
    kMeansPanel.add(new JLabel("Max Iterations:"));
    maxIterationsSpinner = new JSpinner(new SpinnerNumberModel(currentSettings.maxIterations(), 10, 1000, 10));
    kMeansPanel.add(maxIterationsSpinner);
    settingsPanel.add(kMeansPanel, gbc);

    // DBSCAN Panel
    dbscanPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    dbscanPanel.add(new JLabel("Epsilon:"));
    epsilonSpinner = new JSpinner(new SpinnerNumberModel(currentSettings.epsilon(), 0.1, 10.0, 0.1));
    dbscanPanel.add(epsilonSpinner);
    dbscanPanel.add(new JLabel("Min Points:"));
    minPointsSpinner = new JSpinner(new SpinnerNumberModel(currentSettings.minPoints(), 2, 50, 1));
    dbscanPanel.add(minPointsSpinner);
    settingsPanel.add(dbscanPanel, gbc);

    // Feature Selection
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 2;
    settingsPanel.add(new JLabel("Select Features for Clustering:"), gbc);

    gbc.gridy = 4;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    settingsPanel.add(createFeatureSelectionPanel(), gbc);

    // Select/Deselect All Buttons
    gbc.gridy = 5;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    settingsPanel.add(createSelectionButtonsPanel(), gbc);

    contentPanel.add(settingsPanel, BorderLayout.CENTER);

    // Button Panel
    contentPanel.add(createButtonPanel(), BorderLayout.SOUTH);

    add(contentPanel);
    updateControls();
  }

  private JScrollPane createFeatureSelectionPanel() {
    JPanel panel = new JPanel(new GridLayout(0, 2)); // 2 columns
    String[] allFeatures = CellFeatureAggregator.getFeatureNames();
    List<String> selectedFeatures = currentSettings.selectedFeatures();
    
    // If selected features list is empty, it means "all features" by default logic
    boolean selectAll = selectedFeatures.isEmpty();

    // Sort features alphabetically for better readability
    java.util.Arrays.sort(allFeatures);

    for (String feature : allFeatures) {
      JCheckBox cb = new JCheckBox(formatFeatureName(feature));
      cb.setName(feature);
      cb.setSelected(selectAll || selectedFeatures.contains(feature));
      featureCheckboxes.add(cb);
      panel.add(cb);
    }
    
    JScrollPane scrollPane = new JScrollPane(panel);
    scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
    return scrollPane;
  }

  private String formatFeatureName(String rawName) {
    return rawName.replace("_", " ").toUpperCase();
  }

  private JPanel createSelectionButtonsPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    
    JButton selectAllButton = new JButton("Select All");
    selectAllButton.addActionListener(e -> setAllFeaturesSelected(true));
    
    JButton deselectAllButton = new JButton("Deselect All");
    deselectAllButton.addActionListener(e -> setAllFeaturesSelected(false));
    
    panel.add(selectAllButton);
    panel.add(deselectAllButton);
    
    return panel;
  }

  private void setAllFeaturesSelected(boolean selected) {
    for (JCheckBox cb : featureCheckboxes) {
      if (cb.isEnabled()) {
        cb.setSelected(selected);
      }
    }
  }

  private JPanel createButtonPanel() {
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    JButton saveButton = UIUtils.createStandardButton("Save", null);
    JButton cancelButton = UIUtils.createStandardButton("Cancel", null);

    saveButton.addActionListener(e -> saveSettings());
    cancelButton.addActionListener(e -> dispose());

    buttonPanel.add(cancelButton);
    buttonPanel.add(saveButton);

    return buttonPanel;
  }

  private void updateControls() {
    boolean enabled = enabledCheckBox.isSelected();
    algorithmComboBox.setEnabled(enabled);
    updateAlgorithmPanels();
    
    for (JCheckBox cb : featureCheckboxes) {
      cb.setEnabled(enabled);
    }
  }

  private void updateAlgorithmPanels() {
      boolean enabled = enabledCheckBox.isSelected();
      String selectedAlgorithm = (String) algorithmComboBox.getSelectedItem();
      
      boolean isKMeans = "K-Means".equals(selectedAlgorithm);
      
      kMeansPanel.setVisible(isKMeans);
      dbscanPanel.setVisible(!isKMeans);
      
      setContainerEnabled(kMeansPanel, enabled && isKMeans);
      setContainerEnabled(dbscanPanel, enabled && !isKMeans);
  }

  private void setContainerEnabled(Container container, boolean enabled) {
      container.setEnabled(enabled);
      for (Component child : container.getComponents()) {
          child.setEnabled(enabled);
          if (child instanceof Container) {
              setContainerEnabled((Container) child, enabled);
          }
      }
  }

  private void saveSettings() {
    boolean enabled = enabledCheckBox.isSelected();
    String algorithm = (String) algorithmComboBox.getSelectedItem();
    int k = (Integer) kSpinner.getValue();
    int maxIterations = (Integer) maxIterationsSpinner.getValue();
    double epsilon = (Double) epsilonSpinner.getValue();
    int minPoints = (Integer) minPointsSpinner.getValue();
    
    List<String> selectedFeatures = new ArrayList<>();
    for (JCheckBox cb : featureCheckboxes) {
      if (cb.isSelected()) {
        selectedFeatures.add(cb.getName());
      }
    }

    if (enabled && selectedFeatures.isEmpty()) {
      JOptionPane.showMessageDialog(this,
          "Please select at least one feature for clustering.",
          "Invalid Settings", JOptionPane.ERROR_MESSAGE);
      return;
    }

    UnsupervisedClassificationSettings newSettings = new UnsupervisedClassificationSettings(
        k, selectedFeatures, enabled, algorithm, maxIterations, epsilon, minPoints);
    configManager.saveUnsupervisedClassificationSettings(newSettings);
    dispose();
  }
}