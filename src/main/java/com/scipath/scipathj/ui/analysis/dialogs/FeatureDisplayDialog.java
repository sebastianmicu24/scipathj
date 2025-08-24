package com.scipath.scipathj.ui.analysis.dialogs;

import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Dialog for displaying extracted features in a table format.
 * Shows features for each ROI with columns for image name, cell type, ROI ID, and all feature values.
 */
public class FeatureDisplayDialog extends JDialog {

  private JTable featuresTable;
  private DefaultTableModel tableModel;
  private Map<String, Map<String, Object>> featuresData;
  private String imageName;
  private com.scipath.scipathj.core.config.MainSettings mainSettings;

  public FeatureDisplayDialog(Frame parent, Map<String, Map<String, Object>> features) {
    this(parent, features, null, null);
  }

  public FeatureDisplayDialog(Frame parent, Map<String, Map<String, Object>> features, String imageName) {
    this(parent, features, imageName, null);
  }

  public FeatureDisplayDialog(Frame parent, Map<String, Map<String, Object>> features, String imageName, com.scipath.scipathj.core.config.MainSettings mainSettings) {
    super(parent, "Extracted Features", true);
    this.featuresData = features;
    this.imageName = imageName;
    this.mainSettings = mainSettings;
    initializeDialog();
  }

  private void initializeDialog() {
    setSize(1200, 800);
    setLocationRelativeTo(getParent());

    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING));

    // Title
    JLabel titleLabel = UIUtils.createBoldLabel("Extracted Features", UIConstants.SUBTITLE_FONT_SIZE);
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    contentPanel.add(titleLabel, BorderLayout.NORTH);

    // Create table
    createFeaturesTable();
    JScrollPane scrollPane = new JScrollPane(featuresTable);
    scrollPane.setBorder(BorderFactory.createTitledBorder("Feature Data"));
    contentPanel.add(scrollPane, BorderLayout.CENTER);

    // Statistics panel
    JPanel statsPanel = createStatisticsPanel();
    contentPanel.add(statsPanel, BorderLayout.SOUTH);

    add(contentPanel);
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
    columnNames.addAll(allFeatureNames.stream().sorted().toList());

    // Create table model
    tableModel = new DefaultTableModel(columnNames.toArray(), 0) {
      @Override
      public Class<?> getColumnClass(int column) {
        // First 3 columns are strings, rest can be numbers or strings
        if (column < 3) {
          return String.class;
        }
        
        // For feature columns, check the actual data type
        // Look at the first non-null value in this column to determine type
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

    // Populate table data, filtering based on ignore setting
    int filteredOutCount = 0;
    for (Map.Entry<String, Map<String, Object>> entry : featuresData.entrySet()) {
      String roiName = entry.getKey();
      Map<String, Object> roiFeatures = entry.getValue();

      // Check if we should filter out this ROI based on ignore setting
      boolean shouldIncludeROI = true;
      if (mainSettings != null && !mainSettings.includeIgnoredInCsv()) {
        Object ignoreValue = roiFeatures.get("ignore");
        if (ignoreValue != null) {
          String ignoreStr = ignoreValue.toString().toLowerCase().trim();
          if ("true".equals(ignoreStr) || "1".equals(ignoreStr) || "yes".equals(ignoreStr) || Boolean.parseBoolean(ignoreStr)) {
            shouldIncludeROI = false;
            filteredOutCount++;
          }
        }
      }

      if (shouldIncludeROI) {
        // Parse ROI name to extract components
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

        // Add feature values in the same order as column names
        for (int i = 3; i < columnNames.size(); i++) {
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

        tableModel.addRow(rowData.toArray());
      }
    }

    // Log filtering information if any ROIs were filtered out
    if (filteredOutCount > 0) {
      System.out.println("FeatureDisplayDialog: Filtered out " + filteredOutCount + " ignored ROI(s) from table display");
    }

    // Create table
    featuresTable = new JTable(tableModel);
    featuresTable.setAutoCreateRowSorter(true);
    featuresTable.setRowSelectionAllowed(true);
    featuresTable.setColumnSelectionAllowed(true);

    // Set column widths
    TableColumnModel columnModel = featuresTable.getColumnModel();
    if (columnModel.getColumnCount() > 0) {
      columnModel.getColumn(0).setPreferredWidth(200); // Image Name
      columnModel.getColumn(1).setPreferredWidth(100); // Cell Type
      columnModel.getColumn(2).setPreferredWidth(80);  // ROI ID

      // Set reasonable width for feature columns
      for (int i = 3; i < columnModel.getColumnCount(); i++) {
        columnModel.getColumn(i).setPreferredWidth(100);
      }
    }

    // TODO: Add custom renderer for tooltips if needed
    // featuresTable.setDefaultRenderer(Double.class, new TooltipRenderer(featuresTable));
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

    JButton exportButton = UIUtils.createStandardButton("Export to CSV", null);
    exportButton.addActionListener(e -> exportToCSV());

    JButton closeButton = UIUtils.createStandardButton("Close", null);
    closeButton.addActionListener(e -> dispose());

    statsPanel.add(statsLabel);
    statsPanel.add(exportButton);
    statsPanel.add(closeButton);

    return statsPanel;
  }

  private String extractImageName(String roiName) {
    if (roiName == null || roiName.trim().isEmpty()) {
      return "Unknown";
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
      return "Unknown"; // Likely an ID number, don't log as it might be normal
    }
    else {
      // Debug: Log unknown ROI names to help identify the issue
      System.out.println("FeatureDisplayDialog: Unknown ROI type for name: '" + roiName + "'");
      return "Unknown";
    }
  }

  private String extractROIId(String roiName) {
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
              String stringValue = value.toString();
              // If using EU format and this is a numeric column, replace decimal separator
              if (mainSettings != null && mainSettings.useEuCsvFormat() && value instanceof Number) {
                stringValue = stringValue.replace(".", decimalSeparator);
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

}