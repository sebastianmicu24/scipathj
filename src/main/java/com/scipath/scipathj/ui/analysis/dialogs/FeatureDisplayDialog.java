package com.scipath.scipathj.ui.analysis.dialogs;

import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import com.scipath.scipathj.infrastructure.config.MainSettings;
import com.scipath.scipathj.analysis.algorithms.classification.CellClassification;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableColumn;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;

/**
 * Dialog for displaying extracted features in a table format with pagination.
 * Shows features for each ROI with columns for image name, cell type, ROI ID, and all feature values.
 * Uses pagination to handle large datasets efficiently.
 */
public class FeatureDisplayDialog extends JDialog {

  private static final int PAGE_SIZE = 100;

  private JTable featuresTable;
  private DefaultTableModel tableModel;
  private Map<String, Map<String, Object>> featuresData;
  private Map<String, Map<String, Object>> allFeaturesData;
  private List<String> roiKeys; // Sorted list of ROI keys for pagination
  private String imageName;
  private MainSettings mainSettings;

  // Pagination controls
  private JButton prevButton;
  private JButton nextButton;
  private JLabel pageLabel;
  private int currentPage = 0;
  private int totalPages = 0;

  public FeatureDisplayDialog(Frame parent, Map<String, Map<String, Object>> features) {
    this(parent, features, null, null);
  }

  public FeatureDisplayDialog(Frame parent, Map<String, Map<String, Object>> features, String imageName) {
    this(parent, features, imageName, null);
  }

  public FeatureDisplayDialog(Frame parent, Map<String, Map<String, Object>> features, String imageName, MainSettings mainSettings) {
    super(parent, "Extracted Features", true);
    this.featuresData = features;
    this.allFeaturesData = features; // Store unfiltered version for full export
    this.imageName = imageName;
    this.mainSettings = mainSettings;
    initializeDialog();
  }

  private void initializeDialog() {
    setSize(1200, 900);
    setLocationRelativeTo(getParent());

    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    // Title
    JLabel titleLabel = UIUtils.createBoldLabel("Extracted Features", UIConstants.SUBTITLE_FONT_SIZE);
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    contentPanel.add(titleLabel, BorderLayout.NORTH);

    // Prepare data for pagination
    roiKeys = new ArrayList<>(featuresData.keySet());
    Collections.sort(roiKeys); // Sort for consistent ordering
    totalPages = (int) Math.ceil((double) roiKeys.size() / PAGE_SIZE);

    // Create table
    createFeaturesTable();
    JScrollPane scrollPane = new JScrollPane(featuresTable);
    scrollPane.setBorder(BorderFactory.createTitledBorder("Feature Data"));
    contentPanel.add(scrollPane, BorderLayout.CENTER);

    // Pagination controls
    JPanel paginationPanel = createPaginationPanel();
    contentPanel.add(paginationPanel, BorderLayout.SOUTH);

    add(contentPanel);
// Load first page
loadPage(0);
updatePaginationControls();
}

  private void createFeaturesTable() {
    // Determine all unique feature names across all ROIs
    Set<String> allFeatureNames = new HashSet<>();
    for (Map<String, Object> roiFeatures : featuresData.values()) {
      allFeatureNames.addAll(roiFeatures.keySet());
    }

    // Create column names
    List<String> columnNames = new ArrayList<>();
    columnNames.add("Image Name");
    columnNames.add("Cell Type");
    columnNames.add("ROI ID");
    columnNames.add("Predicted Class");
    columnNames.add("Confidence");
    columnNames.addAll(allFeatureNames.stream().sorted().toList());

    // Create table model - simplified, all columns are strings to avoid DoubleRenderer issues
    tableModel = new DefaultTableModel(columnNames.toArray(), 0) {
      @Override
      public Class<?> getColumnClass(int column) {
        return String.class; // All columns as strings to avoid Double rendering issues
      }

      @Override
      public boolean isCellEditable(int row, int column) {
        return false; // Table is read-only
      }
    };

    // Create table
    featuresTable = new JTable(tableModel);
    featuresTable.setAutoCreateRowSorter(true);
    featuresTable.setRowSelectionAllowed(true);
    featuresTable.setColumnSelectionAllowed(true);
    featuresTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    // Set column widths
    TableColumnModel columnModel = featuresTable.getColumnModel();
    if (columnModel.getColumnCount() > 0) {
      columnModel.getColumn(0).setMinWidth(150); // Image Name
      columnModel.getColumn(1).setMinWidth(100); // Cell Type
      columnModel.getColumn(2).setMinWidth(80);  // ROI ID
      columnModel.getColumn(3).setMinWidth(120); // Predicted Class
      columnModel.getColumn(4).setMinWidth(100); // Confidence

      // Set reasonable min width for feature columns
      for (int i = 5; i < columnModel.getColumnCount(); i++) {
        columnModel.getColumn(i).setMinWidth(80);
      }
    }

    // Custom renderer for confidence column to handle percentages
    if (columnModel.getColumnCount() > 4) {
      TableColumn confidenceColumn = columnModel.getColumn(4);
      confidenceColumn.setCellRenderer(new DefaultTableCellRenderer() {
        @Override
        protected void setValue(Object value) {
          if (value == null || value.toString().isEmpty()) {
            setText("");
          } else if (value instanceof Double) {
            Double doubleValue = (Double) value;
            if (doubleValue.isNaN()) {
              setText("");
            } else {
              setText(String.format("%.1f%%", doubleValue));
            }
          } else {
            // Already a string, just display as-is
            setText(value.toString());
          }
        }
      });
    }
  }

  private void loadPage(int page) {
    currentPage = page;

    // Clear current table data
    tableModel.setRowCount(0);

    // Calculate range for this page
    int startIndex = page * PAGE_SIZE;
    int endIndex = Math.min(startIndex + PAGE_SIZE, roiKeys.size());

    // Load data for this page
    for (int i = startIndex; i < endIndex; i++) {
      String roiName = roiKeys.get(i);
      Map<String, Object> roiFeatures = featuresData.get(roiName);

      // Parse ROI name to extract components
      String currentImageName = this.imageName != null && !this.imageName.trim().isEmpty() ?
          this.imageName.trim() : extractImageName(roiName);
      if ("Unknown".equals(currentImageName)) {
        currentImageName = roiName;
      }

      String cellType = extractCellType(roiName);
      String roiId = extractROIId(roiName);

      // Create row data
      List<Object> rowData = new ArrayList<>();
      rowData.add(currentImageName);
      rowData.add(cellType);
      rowData.add(roiId);

      // Add classification data
      CellClassification.ClassificationResult classification = null;
      String roiPart = extractROITypeAndId(roiName);

      try {
        // Try multiple lookup strategies
        classification = com.scipath.scipathj.ui.common.ROIManager.getInstance().getClassificationResult(roiName);

        if (classification == null && roiPart != null) {
          String reconstructedKey = currentImageName + "_" + roiPart;
          classification = com.scipath.scipathj.ui.common.ROIManager.getInstance().getClassificationResult(reconstructedKey);

          if (classification == null) {
            String colonKey = currentImageName + ":" + roiPart;
            classification = com.scipath.scipathj.ui.common.ROIManager.getInstance().getClassificationResult(colonKey);

            if (classification == null) {
              // Entity ID matching for partial classifications
              String entityId = extractEntityIdFromROI(roiName);
              if (entityId != null) {
                Map<String, CellClassification.ClassificationResult> allResults =
                  com.scipath.scipathj.ui.common.ROIManager.getInstance().getAllClassificationResults();

                for (Map.Entry<String, CellClassification.ClassificationResult> entry : allResults.entrySet()) {
                  String availableKey = entry.getKey();
                  String availableEntityId = extractEntityIdFromROI(availableKey);
                  if (entityId.equals(availableEntityId)) {
                    classification = entry.getValue();
                    break;
                  }
                }
              }
            }
          }
        }
      } catch (Exception e) {
        System.out.println("Error looking up classification for '" + roiName + "': " + e.getMessage());
      }

      if (classification != null) {
        rowData.add(classification.getPredictedClass());
        rowData.add(classification.getConfidence() * 100.0); // Store as Double for percentage formatting
      } else {
        rowData.add(""); // Empty string for predicted class
        rowData.add(""); // Empty string for confidence
      }

      // Add feature values
      for (int j = 5; j < tableModel.getColumnCount(); j++) {
        String featureName = tableModel.getColumnName(j);
        Object value = roiFeatures.get(featureName);
        if (value == null) {
          rowData.add("");
        } else if (value instanceof Number) {
          rowData.add(value.toString()); // Convert to string to avoid Double rendering issues
        } else {
          rowData.add(value.toString());
        }
      }

      tableModel.addRow(rowData.toArray());
    }
  }

  private JPanel createPaginationPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    prevButton = new JButton("Previous");
    nextButton = new JButton("Next");
    pageLabel = new JLabel();

    prevButton.addActionListener(e -> {
      if (currentPage > 0) {
        loadPage(currentPage - 1);
        updatePaginationControls();
      }
    });

    nextButton.addActionListener(e -> {
      if (currentPage < totalPages - 1) {
        loadPage(currentPage + 1);
        updatePaginationControls();
      }
    });

    JButton exportButton = UIUtils.createStandardButton("Export All Features", null);
    exportButton.addActionListener(e -> exportAllFeaturesToCSV());

    panel.add(prevButton);
    panel.add(pageLabel);
    panel.add(nextButton);
    panel.add(exportButton);

    return panel;
  }

  private void updatePaginationControls() {
    prevButton.setEnabled(currentPage > 0);
    nextButton.setEnabled(currentPage < totalPages - 1);
    pageLabel.setText(String.format("Page %d of %d (%d total ROIs)",
      currentPage + 1, totalPages, roiKeys.size()));

    // Update dialog title with page info
    setTitle(String.format("Extracted Features - Page %d of %d", currentPage + 1, totalPages));
  }
  private JPanel createStatisticsPanel() {
    JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    int originalTotalROIs = featuresData.size();
    int displayedROIs = tableModel.getRowCount();
    int totalFeatures = tableModel.getColumnCount() - 3; // Subtract non-feature columns
    int filteredOutROIs = originalTotalROIs - displayedROIs;

    String statsText = String.format("Total ROIs: %d | Total Features per ROI: %d",
        displayedROIs, totalFeatures);
    if (filteredOutROIs > 0) {
      statsText += String.format(" | Filtered out: %d ignored", filteredOutROIs);
    }

    JLabel statsLabel = UIUtils.createLabel(statsText, UIConstants.NORMAL_FONT_SIZE, null);

    JButton exportButton = UIUtils.createStandardButton("Save Displayed Features", null);
    exportButton.setPreferredSize(new Dimension(200, exportButton.getPreferredSize().height));
    exportButton.addActionListener(e -> exportToCSV());

    JButton exportAllButton = UIUtils.createStandardButton("Save All Features", null);
    exportAllButton.setPreferredSize(new Dimension(180, exportAllButton.getPreferredSize().height));
    exportAllButton.addActionListener(e -> exportAllFeaturesToCSV());

    JButton closeButton = UIUtils.createStandardButton("Close", null);
    closeButton.setPreferredSize(new Dimension(100, closeButton.getPreferredSize().height));
    closeButton.addActionListener(e -> dispose());

    statsPanel.add(statsLabel);
    statsPanel.add(exportButton);
    statsPanel.add(exportAllButton);
    statsPanel.add(closeButton);

    return statsPanel;
  }

  private String extractImageName(String roiName) {
    if (roiName == null || roiName.trim().isEmpty()) {
      return "Unknown";
    }

    // Check if this is a merged entity feature (simple numeric ID)
    if (roiName.matches("^\\d+$")) {
      // For merged entity features, use the provided image name if available
      if (this.imageName != null && !this.imageName.trim().isEmpty()) {
        return this.imageName.trim();
      }
      return "Entity_" + roiName; // Fallback for merged entities
    }

    // New format: "ImageName.ext_ROIType_ID" or "ImageName_ROIType_ID"
    // First, handle file paths by extracting the filename
    String filename = roiName;
    int lastSlashIndex = roiName.lastIndexOf('/');
    if (lastSlashIndex >= 0 && lastSlashIndex < roiName.length() - 1) {
      filename = roiName.substring(lastSlashIndex + 1);
    }

    // If filename contains path separators, take the last part
    int lastBackslashIndex = filename.lastIndexOf('\\');
    if (lastBackslashIndex >= 0 && lastBackslashIndex < filename.length() - 1) {
      filename = filename.substring(lastBackslashIndex + 1);
    }

    // Now extract the image name part
    // Look for the first underscore which separates image name from ROI info
    int firstUnderscoreIndex = filename.indexOf('_');
    if (firstUnderscoreIndex > 0) {
      String imageNamePart = filename.substring(0, firstUnderscoreIndex);

      // Check if this part has a file extension
      int dotIndex = imageNamePart.lastIndexOf('.');
      if (dotIndex > 0) {
        // Has extension, take everything before it
        return imageNamePart.substring(0, dotIndex);
      } else {
        // No extension, return as is
        return imageNamePart;
      }
    } else {
      // No underscore found, fall back to original logic
      // Look for file extension first
      int dotIndex = filename.lastIndexOf('.');
      if (dotIndex > 0) {
        // Has extension, take everything before it
        String baseName = filename.substring(0, dotIndex);

        // Now look for underscore in the base name
        int underscoreIndex = baseName.indexOf('_');
        if (underscoreIndex > 0) {
          return baseName.substring(0, underscoreIndex);
        } else {
          return baseName;
        }
      } else {
        // No extension, look for underscore
        int underscoreIndex = filename.indexOf('_');
        if (underscoreIndex > 0) {
          return filename.substring(0, underscoreIndex);
        } else {
          // No extension, no underscore - this might be just a type name
          // Check if it's a known ROI type and handle accordingly
          String lower = filename.toLowerCase();
          if (lower.contains("nucleus") || lower.contains("cytoplasm") || lower.contains("vessel")) {
            return "Unknown"; // Don't use ROI type as image name
          }
          return filename;
        }
      }
    }
  }

  private String extractCellType(String roiName) {
    // Check if this is a merged entity feature (simple numeric ID)
    if (roiName.matches("^\\d+$")) {
      return "Entity"; // Merged entity features from multiple ROI types
    }
    
    // Extract cell type based on ROI name pattern
    String lowerName = roiName.toLowerCase();

    // Check for vessel patterns
    if (lowerName.contains("vessel") || lowerName.startsWith("vessel_") || lowerName.endsWith("_vessel")) {
      return "Vessel";
    }
    // Check for nucleus patterns
    else if (lowerName.contains("nucleus") || lowerName.startsWith("nucleus_") || lowerName.endsWith("_nucleus")) {
      return "Nucleus";
    }
    // Check for cytoplasm patterns
    else if (lowerName.contains("cytoplasm") || lowerName.contains("cyto") ||
             lowerName.startsWith("cytoplasm_") || lowerName.endsWith("_cytoplasm") ||
             lowerName.startsWith("cyto_") || lowerName.endsWith("_cyto")) {
      return "Cytoplasm";
    }
    // Check for cell patterns
    else if (lowerName.contains("cell") || lowerName.startsWith("cell_") || lowerName.endsWith("_cell")) {
      return "Cell";
    }
    // Additional fallback patterns that might be created during fusion
    else if (lowerName.contains("biological") || lowerName.contains("entity") || lowerName.contains("combined")) {
      return "Cell"; // Assume fused biological entities are cells
    }
    else if (lowerName.matches("\\d+.*") && lowerName.length() < 10) {
      return "Entity"; // Likely merged entity ID
    }
    else {
      return "Unknown";
    }
  }

  private String extractROIId(String roiName) {
    // Check if this is a merged entity feature (simple numeric ID)
    if (roiName.matches("^\\d+$")) {
      return roiName; // Entity ID is the ROI name itself
    }
    
    // Extract the number after the last underscore
    int lastUnderscoreIndex = roiName.lastIndexOf('_');
    if (lastUnderscoreIndex > 0 && lastUnderscoreIndex < roiName.length() - 1) {
      String afterUnderscore = roiName.substring(lastUnderscoreIndex + 1);
      try {
        Integer.parseInt(afterUnderscore);
        return afterUnderscore;
      } catch (NumberFormatException e) {
        // Not a number, return the part after underscore
        return afterUnderscore;
      }
    }
    return roiName;
  }

  /**
   * Extract the ROI type and ID part from a full ROI name.
   * E.g., "P1 - 9 - 03_Nucleus_132" -> "Nucleus_132"
   */
  private String extractROITypeAndId(String fullRoiName) {
      String cellType = extractCellType(fullRoiName);
      String roiId = extractROIId(fullRoiName);
      if (!"Unknown".equals(cellType) && !"Unknown".equals(roiId)) {
          return cellType + "_" + roiId;
      }
      // Fallback to the full name if parsing fails
      return fullRoiName;
  }

  /**
   * Extract entity ID from ROI name for classification lookup
   * Examples: "P5 - 235 - 02.tif_Cell_2222" -> "2222"
   *          "P5 - 235 - 02.tif_Nucleus_188" -> "188"
   */
  private String extractEntityIdFromROI(String roiName) {
    if (roiName == null) return null;
    
    // If it's already just a number, return it
    if (roiName.matches("\\d+")) {
      return roiName;
    }
    
    // Extract the last number after underscore
    String[] parts = roiName.split("_");
    if (parts.length > 0) {
      String lastPart = parts[parts.length - 1];
      if (lastPart.matches("\\d+")) {
        return lastPart;
      }
    }
    
    return null;
  }

 private void exportToCSV() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setSelectedFile(new File("features_export.csv")); // Default filename
    int result = fileChooser.showSaveDialog(this);

    if (result == JFileChooser.APPROVE_OPTION) {
      File selectedFile = fileChooser.getSelectedFile();
      // Ensure the file has .csv extension
      if (!selectedFile.getName().toLowerCase().endsWith(".csv")) {
        selectedFile = new File(selectedFile.getAbsolutePath() + ".csv");
      }

      try (FileWriter writer = new FileWriter(selectedFile)) {
        // Determine CSV format settings
        String delimiter;
        String decimalSeparator;

        if (mainSettings != null && mainSettings.useEuCsvFormat()) {
          // EU format: semicolon delimiter, comma decimal separator
          delimiter = ";";
          decimalSeparator = ",";
        } else {
          // US format: comma delimiter, period decimal separator
          delimiter = ",";
          decimalSeparator = ".";
        }

        // Write headers
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
          writer.write(tableModel.getColumnName(i));
          if (i < tableModel.getColumnCount() - 1) {
            writer.write(delimiter);
          }
        }
        writer.write("\n");

        // Write all data from table (filtering already done during table creation)
        int exportedRows = tableModel.getRowCount();
        for (int row = 0; row < exportedRows; row++) {
          for (int col = 0; col < tableModel.getColumnCount(); col++) {
            Object value = tableModel.getValueAt(row, col);
            if (value != null) {
              String stringValue;
              if (value instanceof Double && Double.isNaN((Double) value)) {
                // Handle NaN values (from missing classification data) as empty string
                stringValue = "";
              } else {
                stringValue = value.toString();
                // If using EU format and this is a numeric column, replace decimal separator
                if (mainSettings != null && mainSettings.useEuCsvFormat() && value instanceof Number) {
                  stringValue = stringValue.replace(".", decimalSeparator);
                }
            }
              
              writer.write(stringValue);
            } else {
              writer.write("");
            }
            if (col < tableModel.getColumnCount() - 1) {
              writer.write(delimiter);
            }
          }
          writer.write("\n");
        }

        String formatType = (mainSettings != null && mainSettings.useEuCsvFormat()) ? "EU" : "US";
        String inclusionInfo = "";
        if (mainSettings != null && !mainSettings.includeIgnoredInCsv()) {
          int originalTotal = featuresData.size();
          int filteredOut = originalTotal - exportedRows;
          if (filteredOut > 0) {
            inclusionInfo = String.format("\nFiltered out %d ignored ROI%s", filteredOut, filteredOut == 1 ? "" : "s");
          }
        }

        JOptionPane.showMessageDialog(this,
            String.format("CSV export successful!\nFormat: %s\nRows exported: %d%s\nSaved to: %s",
                formatType + " format", exportedRows, inclusionInfo, selectedFile.getAbsolutePath()),
            "Export Success", JOptionPane.INFORMATION_MESSAGE);
      } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "Error exporting to CSV: " + e.getMessage(),
                                        "Export Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void exportAllFeaturesToCSV() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setSelectedFile(new File("all_features_export.csv")); // Default filename
    int result = fileChooser.showSaveDialog(this);

    if (result == JFileChooser.APPROVE_OPTION) {
      File selectedFile = fileChooser.getSelectedFile();
      // Ensure the file has .csv extension
      if (!selectedFile.getName().toLowerCase().endsWith(".csv")) {
        selectedFile = new File(selectedFile.getAbsolutePath() + ".csv");
      }

      try (FileWriter writer = new FileWriter(selectedFile)) {
        // Determine CSV format settings
        String delimiter;
        String decimalSeparator;

        if (mainSettings != null && mainSettings.useEuCsvFormat()) {
          // EU format: semicolon delimiter, comma decimal separator
          delimiter = ";";
          decimalSeparator = ",";
        } else {
          // US format: comma delimiter, period decimal separator
          delimiter = ",";
          decimalSeparator = ".";
        }

        // Create table model for all features (with filtering disabled)
        DefaultTableModel allFeaturesModel = createAllFeaturesTableModel();

        // Write headers
        for (int i = 0; i < allFeaturesModel.getColumnCount(); i++) {
          writer.write(allFeaturesModel.getColumnName(i));
          if (i < allFeaturesModel.getColumnCount() - 1) {
            writer.write(delimiter);
          }
        }
        writer.write("\n");

        // Write all data from all features table (no filtering applied)
        int exportedRows = allFeaturesModel.getRowCount();
        for (int row = 0; row < exportedRows; row++) {
          for (int col = 0; col < allFeaturesModel.getColumnCount(); col++) {
            Object value = allFeaturesModel.getValueAt(row, col);
            if (value != null) {
              String stringValue;
              if (value instanceof Double && Double.isNaN((Double) value)) {
                // Handle NaN values (from missing classification data) as empty string
                stringValue = "";
              } else {
                stringValue = value.toString();
                // If using EU format and this is a numeric column, replace decimal separator
                if (mainSettings != null && mainSettings.useEuCsvFormat() && value instanceof Number) {
                  stringValue = stringValue.replace(".", decimalSeparator);
                }
              }
              writer.write(stringValue);
            } else {
              writer.write("");
            }
            if (col < allFeaturesModel.getColumnCount() - 1) {
              writer.write(delimiter);
            }
          }
          writer.write("\n");
        }

        String formatType = (mainSettings != null && mainSettings.useEuCsvFormat()) ? "EU" : "US";
        JOptionPane.showMessageDialog(this,
            String.format("All features CSV export successful!\nFormat: %s\nRows exported: %d\nSaved to: %s",
                formatType + " format", exportedRows, selectedFile.getAbsolutePath()),
            "Export Success", JOptionPane.INFORMATION_MESSAGE);
      } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "Error exporting all features to CSV: " + e.getMessage(),
                                        "Export Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  /**
   * Creates a table model for all features without any filtering.
   */
  private DefaultTableModel createAllFeaturesTableModel() {
    // Determine all unique feature names across all ROIs
    Set<String> allFeatureNames = new HashSet<>();
    for (Map<String, Object> roiFeatures : allFeaturesData.values()) {
      allFeatureNames.addAll(roiFeatures.keySet());
    }

    // Create column names (same as filtered table)
    List<String> columnNames = new ArrayList<>();
    columnNames.add("Image Name");
    columnNames.add("Cell Type");
    columnNames.add("ROI ID");
    columnNames.add("Predicted Class");
    columnNames.add("Confidence");
    columnNames.addAll(allFeatureNames.stream().sorted().toList());

    // Create table model
    DefaultTableModel model = new DefaultTableModel(columnNames.toArray(), 0) {
      @Override
      public Class<?> getColumnClass(int column) {
        // First 3 columns are strings
        if (column < 3) {
          return String.class;
        }
        // Columns 3 and 4 are Predicted Class (String) and Confidence (Double)
        if (column == 3) { // "Predicted Class"
          return String.class;
        }
        if (column == 4) { // "Confidence" - can contain null values, so use Object.class for custom rendering
          return Object.class; // Always use Object.class for confidence column to allow null handling
        }

        // Override the default renderer for confidence column by forcing it to use Object.class
        // This ensures our custom renderer will be used instead of the default DoubleRenderer
        if (column == 4 && getRowCount() > 0) {
          // Check if any row has null in confidence column
          for (int row = 0; row < getRowCount(); row++) {
            Object value = getValueAt(row, 4);
            if (value == null) {
              return Object.class; // Force Object.class when nulls are present
            }
          }
        }

        // For feature columns (starting at column 5), check the actual data type
        for (int row = 0; row < getRowCount(); row++) {
          Object value = getValueAt(row, column);
          if (value != null) {
            return value instanceof String ? String.class : Double.class;
          }
        }

        // Default to Object if no data found
        return Object.class;
      }

      @Override
      public boolean isCellEditable(int row, int column) {
        return false; // Table is read-only
      }
    };

    // Populate table data from all features (no filtering)
    for (Map.Entry<String, Map<String, Object>> entry : allFeaturesData.entrySet()) {
      String roiName = entry.getKey();
      Map<String, Object> roiFeatures = entry.getValue();

      // Parse ROI name to extract components (same as filtered table)
      String currentImageName;
      if (this.imageName != null && !this.imageName.trim().isEmpty()) {
        // Use provided image name if available
        currentImageName = this.imageName.trim();
      } else {
        // Extract from ROI name
        currentImageName = extractImageName(roiName);
        // If extraction returns "Unknown", try to use the ROI name itself as fallback
        if ("Unknown".equals(currentImageName)) {
          currentImageName = roiName;
        }
      }
      String cellType = extractCellType(roiName);
      String roiId = extractROIId(roiName);

      // Create row data
      List<Object> rowData = new ArrayList<>();
      rowData.add(currentImageName);
      rowData.add(cellType);
      rowData.add(roiId);

      // Add classification data (columns 3-4: "Predicted Class", "Confidence")
      // Try to find classification data using key lookups to match tooltip behavior
      CellClassification.ClassificationResult classification = null;
      String roiPart = extractROITypeAndId(roiName);
      try {
        // First try roiName directly (may already be the correct key)
        classification = com.scipath.scipathj.ui.common.ROIManager.getInstance().getClassificationResult(roiName);
        if (classification == null) {
          // Try reconstructed key like tooltip: imageName + "_" + ROIType_ID
          String reconstructedKey = currentImageName + "_" + roiPart;
          classification = com.scipath.scipathj.ui.common.ROIManager.getInstance().getClassificationResult(reconstructedKey);
          if (classification == null) {
            // Try colon format as tooltip fallback
            String colonKey = currentImageName + ":" + roiPart;
            classification = com.scipath.scipathj.ui.common.ROIManager.getInstance().getClassificationResult(colonKey);
          }
        }
      } catch (Exception e) {
        // Ignore classification lookup errors
      }

      if (classification != null) {
        rowData.add(classification.getPredictedClass());
        rowData.add(classification.getConfidence() * 100.0); // Store as percentage
      } else {
        rowData.add(""); // Empty string for predicted class
        rowData.add(""); // Use empty string instead of null for missing confidence
      }

      // Add feature values in the same order as column names (starting from column 5)
      for (int i = 5; i < columnNames.size(); i++) {
        String featureName = columnNames.get(i);
        Object value = roiFeatures.get(featureName);
        if (value == null) {
          rowData.add(""); // Use empty string for null values
        } else if (value instanceof String) {
          rowData.add(value); // Keep strings as strings
        } else if (value instanceof Number) {
          rowData.add(((Number) value).doubleValue()); // Convert numbers to double
        } else {
          rowData.add(value.toString()); // Convert other types to string
        }
      }

      model.addRow(rowData.toArray());
    }

    return model;
  }

}