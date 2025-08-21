package com.scipath.scipathj.ui.dialogs;

import com.scipath.scipathj.core.config.MainSettings;
import com.scipath.scipathj.data.model.UserROI;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog for displaying ROI statistics per image and globally.
 * Shows counts by ROI type for each image and provides summary statistics.
 */
public class ROIStatisticsDialog extends JDialog {

  private static final Logger LOGGER = LoggerFactory.getLogger(ROIStatisticsDialog.class);

  // UI Components
  private JTable statisticsTable;
  private DefaultTableModel tableModel;
  private JLabel totalROIsLabel;
  private JLabel totalImagesLabel;
  private JLabel averageROIsLabel;

  // Data
  private Map<String, List<UserROI>> roiData; // imageFileName -> ROI list
  private MainSettings mainSettings;

  public ROIStatisticsDialog(Frame parent, Map<String, List<UserROI>> roiData, MainSettings mainSettings) {
    super(parent, "ROI Statistics", true);
    this.roiData = roiData != null ? roiData : Map.of();
    this.mainSettings = mainSettings;

    initializeComponents();
    updateStatistics();
    setupLayout();

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setSize(800, 600);
    setLocationRelativeTo(parent);

    LOGGER.info("Created ROI Statistics dialog with {} images", roiData.size());
  }

  private void initializeComponents() {
    // Create table model with columns
    String[] columnNames = {"Image", "Total ROIs", "Vessels", "Nuclei", "Cytoplasms", "Cells", "Other"};
    tableModel = new DefaultTableModel(columnNames, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false; // Make table read-only
      }

      @Override
      public Class<?> getColumnClass(int column) {
        return String.class; // All columns as strings for simplicity
      }
    };

    // Create table
    statisticsTable = new JTable(tableModel);
    statisticsTable.setFillsViewportHeight(true);
    statisticsTable.setRowHeight(25);
    statisticsTable.setGridColor(Color.LIGHT_GRAY);
    statisticsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    // Set custom renderer for center alignment
    DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
    centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    for (int i = 1; i < statisticsTable.getColumnCount(); i++) {
      statisticsTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
    }

    // Create summary labels
    totalROIsLabel = UIUtils.createLabel("Total ROIs: 0", UIConstants.SMALL_FONT_SIZE, UIManager.getColor("Label.foreground"));
    totalImagesLabel = UIUtils.createLabel("Total Images: 0", UIConstants.SMALL_FONT_SIZE, UIManager.getColor("Label.foreground"));
    averageROIsLabel = UIUtils.createLabel("Average ROIs per Image: 0.0", UIConstants.SMALL_FONT_SIZE, UIManager.getColor("Label.foreground"));
  }

  private void setupLayout() {
    setLayout(new BorderLayout(UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING));

    // Title panel
    JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    JLabel titleLabel = UIUtils.createLabel("ROI Statistics", UIConstants.LARGE_FONT_SIZE, UIManager.getColor("Label.foreground"));
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
    titlePanel.add(titleLabel);
    add(titlePanel, BorderLayout.NORTH);

    // Table panel with scroll
    JScrollPane tableScrollPane = new JScrollPane(statisticsTable);
    tableScrollPane.setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING));
    add(tableScrollPane, BorderLayout.CENTER);

    // Summary panel
    JPanel summaryPanel = new JPanel(new GridLayout(1, 3, UIConstants.MEDIUM_SPACING, 0));
    summaryPanel.setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING));
    summaryPanel.add(totalImagesLabel);
    summaryPanel.add(totalROIsLabel);
    summaryPanel.add(averageROIsLabel);
    add(summaryPanel, BorderLayout.SOUTH);
  }

  private void updateStatistics() {
    // Clear existing data
    tableModel.setRowCount(0);

    if (roiData.isEmpty()) {
      // Add empty row
      tableModel.addRow(new Object[]{"No images with ROIs", "0", "0", "0", "0", "0", "0"});
      return;
    }

    // Process each image
    int totalROIs = 0;
    int imageCount = roiData.size();

    for (Map.Entry<String, List<UserROI>> entry : roiData.entrySet()) {
      String imageName = getDisplayImageName(entry.getKey());
      List<UserROI> rois = entry.getValue();

      // Count ROIs by category
      Map<MainSettings.ROICategory, Long> categoryCounts = new java.util.HashMap<>();
      for (UserROI roi : rois) {
        MainSettings.ROICategory category = determineROICategory(roi);
        categoryCounts.put(category, categoryCounts.getOrDefault(category, 0L) + 1);
      }

      int totalForImage = rois.size();
      totalROIs += totalForImage;

      int vesselCount = categoryCounts.getOrDefault(MainSettings.ROICategory.VESSEL, 0L).intValue();
      int nucleusCount = categoryCounts.getOrDefault(MainSettings.ROICategory.NUCLEUS, 0L).intValue();
      int cytoplasmCount = categoryCounts.getOrDefault(MainSettings.ROICategory.CYTOPLASM, 0L).intValue();
      int cellCount = categoryCounts.getOrDefault(MainSettings.ROICategory.CELL, 0L).intValue();

      // Calculate other types (ROIs that don't fit into the main categories)
      int categorizedCount = vesselCount + nucleusCount + cytoplasmCount + cellCount;
      int otherCount = totalForImage - categorizedCount;

      // Add row to table
      tableModel.addRow(new Object[]{
          imageName,
          String.valueOf(totalForImage),
          String.valueOf(vesselCount),
          String.valueOf(nucleusCount),
          String.valueOf(cytoplasmCount),
          String.valueOf(cellCount),
          String.valueOf(otherCount)
      });
    }

    // Update summary labels
    totalImagesLabel.setText(String.format("Total Images: %d", imageCount));
    totalROIsLabel.setText(String.format("Total ROIs: %d", totalROIs));
    averageROIsLabel.setText(String.format("Average ROIs per Image: %.1f", imageCount > 0 ? (double) totalROIs / imageCount : 0.0));

    LOGGER.debug("Updated ROI statistics: {} images, {} total ROIs", imageCount, totalROIs);
  }

  /**
   * Get a display-friendly image name from the full path
   */
  private String getDisplayImageName(String fullPath) {
    if (fullPath == null) return "Unknown";

    // Extract just the filename from the path
    int lastSlash = Math.max(fullPath.lastIndexOf('/'), fullPath.lastIndexOf('\\'));
    String fileName = lastSlash >= 0 ? fullPath.substring(lastSlash + 1) : fullPath;

    // Remove file extension if present
    int dotIndex = fileName.lastIndexOf('.');
    return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
  }

  /**
   * Get color for ROI type (for potential future use)
   */
  private Color getColorForROIType(UserROI.ROIType type) {
    if (mainSettings == null) return Color.BLACK;

    // Create a dummy ROI with the specified type to use the updated logic
    UserROI dummyROI = new UserROI(type, new java.awt.Rectangle(0, 0, 1, 1), "dummy", "dummy");
    MainSettings.ROICategory category = determineROICategory(dummyROI);
    return mainSettings.getSettingsForCategory(category).borderColor();
  }

  /**
   * Map ROI to category based on its type and class
   */
  private MainSettings.ROICategory determineROICategory(UserROI roi) {
    // First check the class type - this is more reliable than ROI type
    if (roi instanceof com.scipath.scipathj.data.model.NucleusROI) {
      return MainSettings.ROICategory.NUCLEUS;
    }

    // Then check the ROI type
    UserROI.ROIType roiType = roi.getType();
    switch (roiType) {
      case VESSEL: return MainSettings.ROICategory.VESSEL;
      case COMPLEX_SHAPE: return MainSettings.ROICategory.VESSEL;
      case NUCLEUS: return MainSettings.ROICategory.NUCLEUS;
      case CYTOPLASM: return MainSettings.ROICategory.CYTOPLASM;
      case CELL: return MainSettings.ROICategory.CELL;
      default: break;
    }

    // Check name-based heuristics as fallback
    String name = roi.getName().toLowerCase();
    if (name.contains("cell")) return MainSettings.ROICategory.CELL;
    if (name.contains("cytoplasm") || name.contains("cyto")) return MainSettings.ROICategory.CYTOPLASM;
    if (name.contains("nucleus") || name.contains("nuclei")) return MainSettings.ROICategory.NUCLEUS;
    if (roi.hasComplexShape() || name.contains("vessel")) return MainSettings.ROICategory.VESSEL;

    return MainSettings.ROICategory.VESSEL; // Default fallback
  }

}