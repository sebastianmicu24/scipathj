package com.scipath.scipathj.ui.analysis;

import com.scipath.scipathj.analysis.algorithms.classification.CellFeatureAggregator;
import com.scipath.scipathj.analysis.algorithms.classification.UnsupervisedClassifier;
import com.scipath.scipathj.infrastructure.roi.CellROI;
import com.scipath.scipathj.ui.common.ROIManager;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panel for configuring and running unsupervised classification (clustering).
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class UnsupervisedClusteringPanel extends JPanel {

  private static final Logger LOGGER = LoggerFactory.getLogger(UnsupervisedClusteringPanel.class);

  private final ROIManager roiManager;
  private final UnsupervisedClassifier classifier;
  private final JSpinner clusterCountSpinner;
  private final JPanel featureSelectionPanel;
  private final JButton runButton;
  private final JTextArea resultsArea;
  private final List<JCheckBox> featureCheckboxes;

  public UnsupervisedClusteringPanel(ROIManager roiManager) {
    this.roiManager = roiManager;
    this.classifier = new UnsupervisedClassifier();
    this.featureCheckboxes = new ArrayList<>();
    this.clusterCountSpinner = new JSpinner(new SpinnerNumberModel(3, 2, 20, 1));
    this.featureSelectionPanel = createFeatureSelectionPanel();
    this.runButton = createRunButton();
    this.resultsArea = new JTextArea(10, 40);

    initializeComponents();
  }

  private void initializeComponents() {
    setLayout(new BorderLayout());
    setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    // Header
    add(UIUtils.createTitleLabel("Unsupervised Classification"), BorderLayout.NORTH);

    // Main Content
    JPanel contentPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(5, 5, 5, 5);

    // Cluster Count
    contentPanel.add(new JLabel("Number of Clusters (K):"), gbc);
    gbc.gridx = 1;
    contentPanel.add(clusterCountSpinner, gbc);

    // Feature Selection
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    contentPanel.add(new JLabel("Select Features:"), gbc);
    
    gbc.gridy = 2;
    contentPanel.add(new JScrollPane(featureSelectionPanel), gbc);

    // Run Button
    gbc.gridy = 3;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.CENTER;
    contentPanel.add(runButton, gbc);

    add(contentPanel, BorderLayout.CENTER);

    // Results Area
    resultsArea.setEditable(false);
    add(new JScrollPane(resultsArea), BorderLayout.SOUTH);
  }

  private JPanel createFeatureSelectionPanel() {
    JPanel panel = new JPanel(new GridLayout(0, 2)); // 2 columns
    String[] features = CellFeatureAggregator.getFeatureNames();
    
    for (String feature : features) {
      JCheckBox cb = new JCheckBox(formatFeatureName(feature));
      cb.setName(feature); // Store raw name
      cb.setSelected(true); // Default to all selected
      featureCheckboxes.add(cb);
      panel.add(cb);
    }
    return panel;
  }

  private String formatFeatureName(String rawName) {
    return rawName.replace("_", " ").toUpperCase();
  }

  private JButton createRunButton() {
    JButton btn = new JButton("Run Clustering");
    btn.setIcon(FontIcon.of(FontAwesomeSolid.PLAY, 16, Color.WHITE));
    btn.setBackground(UIConstants.ACCENT_COLOR);
    btn.setForeground(Color.WHITE);
    btn.addActionListener(e -> runClustering());
    return btn;
  }

  private void runClustering() {
    int k = (Integer) clusterCountSpinner.getValue();
    List<String> selectedFeatures = new ArrayList<>();
    for (JCheckBox cb : featureCheckboxes) {
      if (cb.isSelected()) {
        selectedFeatures.add(cb.getName());
      }
    }

    if (selectedFeatures.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Please select at least one feature.", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Get cells from ROIManager (assuming we can access them)
    // Note: ROIManager needs a method to get all CellROIs. 
    // For now, we'll assume a method exists or we need to add it.
    List<CellROI> cells = roiManager.getCellROIs(); 
    
    if (cells.isEmpty()) {
        resultsArea.setText("No cells found to cluster.");
        return;
    }

    // We also need the raw features map. 
    // This is tricky because ROIManager stores ROIs but maybe not the raw feature map from FeatureExtraction.
    // Ideally, FeatureExtraction results should be stored in ROIManager or accessible.
    // For this implementation, we'll assume ROIManager has a way to get features or we re-extract them (slow).
    // BETTER: Let's assume ROIManager has `getAllExtractedFeatures()`
    Map<String, Map<String, Object>> allFeatures = roiManager.getAllExtractedFeatures();

    if (allFeatures == null || allFeatures.isEmpty()) {
         resultsArea.setText("No features available. Please run analysis first.");
         return;
    }

    runButton.setEnabled(false);
    resultsArea.setText("Running clustering with K=" + k + "...\n");

    SwingWorker<Map<Integer, List<CellROI>>, Void> worker = new SwingWorker<>() {
      @Override
      protected Map<Integer, List<CellROI>> doInBackground() {
        // Use the first image name found in features as a fallback if needed, 
        // but really we should pass the correct image name.
        // For batch processing, this might be complex. 
        // We'll use a placeholder or the first key's prefix.
        String imageFileName = "unknown"; 
        if (!allFeatures.isEmpty()) {
            String firstKey = allFeatures.keySet().iterator().next();
            if (firstKey.contains("_")) {
                imageFileName = firstKey.substring(0, firstKey.lastIndexOf('_'));
            }
        }
        
        // Use default parameters for now as this panel is being deprecated/replaced by the main pipeline
        return classifier.clusterCells(cells, allFeatures, imageFileName, k, selectedFeatures, "K-Means", 100, 0.5, 5);
      }

      @Override
      protected void done() {
        try {
          Map<Integer, List<CellROI>> clusters = get();
          displayResults(clusters);
          colorROIsByCluster(clusters);
        } catch (Exception ex) {
          LOGGER.error("Clustering error", ex);
          resultsArea.append("Error: " + ex.getMessage());
        } finally {
          runButton.setEnabled(true);
        }
      }
    };
    worker.execute();
  }

  private void displayResults(Map<Integer, List<CellROI>> clusters) {
    StringBuilder sb = new StringBuilder();
    sb.append("Clustering Results:\n");
    for (Map.Entry<Integer, List<CellROI>> entry : clusters.entrySet()) {
      sb.append(String.format("Cluster %d: %d cells\n", entry.getKey() + 1, entry.getValue().size()));
    }
    resultsArea.setText(sb.toString());
  }

  private void colorROIsByCluster(Map<Integer, List<CellROI>> clusters) {
    // Generate distinct colors for K clusters
    Color[] colors = generateColors(clusters.size());

    for (Map.Entry<Integer, List<CellROI>> entry : clusters.entrySet()) {
      int clusterId = entry.getKey();
      Color color = colors[clusterId % colors.length];
      
      for (CellROI cell : entry.getValue()) {
        cell.setDisplayColor(color);
        // Also color associated nucleus and cytoplasm
        if (cell.getAssociatedNucleus() != null) {
            cell.getAssociatedNucleus().setDisplayColor(color);
        }
        if (cell.getAssociatedCytoplasm() != null) {
            cell.getAssociatedCytoplasm().setDisplayColor(color);
        }
      }
    }
    
    // Trigger repaint
    roiManager.notifyROIsChanged();
  }

  private Color[] generateColors(int n) {
    Color[] colors = new Color[n];
    for (int i = 0; i < n; i++) {
      colors[i] = Color.getHSBColor((float) i / n, 0.8f, 0.9f);
    }
    return colors;
  }
}