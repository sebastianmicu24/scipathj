package com.scipath.scipathj.ui.analysis.dialogs;

import com.scipath.scipathj.infrastructure.config.MainSettings;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog for displaying ROI feature statistics averages.
 * Shows mean values for each feature grouped by ROI type.
 */
public class ROIStatisticsAveragesDialog extends JDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ROIStatisticsAveragesDialog.class);

    // UI Components
    private JTable averagesTable;
    private DefaultTableModel tableModel;
    private DefaultTableCellRenderer centerRenderer;

    // Data
    private Map<String, Map<UserROI.ROIType, Map<String, Double>>> perImageStats;
    private MainSettings mainSettings;

    public ROIStatisticsAveragesDialog(Frame parent, Map<String, Map<UserROI.ROIType, Map<String, Double>>> perImageStats, MainSettings mainSettings) {
        super(parent, "ROI Feature Averages", true);
        this.perImageStats = perImageStats;
        this.mainSettings = mainSettings;

        initializeComponents();
        updateTable();
        setupLayout();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(parent);

        int totalROITypes = perImageStats.values().stream().mapToInt(Map::size).sum();
        LOGGER.info("Created ROI Statistics Averages dialog with {} processed images and {} total ROI type entries", perImageStats.size(), totalROITypes);
    }

    private void initializeComponents() {
        // Create table model with dynamic columns based on features
        // We'll set columns after we see what features are available
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return String.class;
            }
        };

        // Create table
        averagesTable = new JTable(tableModel);
        averagesTable.setFillsViewportHeight(true);
        averagesTable.setRowHeight(25);
        averagesTable.setGridColor(Color.LIGHT_GRAY);
        averagesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Set custom renderer for center alignment for numeric columns
        centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void setupLayout() {
        setLayout(new BorderLayout(UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING));

        // Title panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel titleLabel = UIUtils.createLabel("ROI Feature Statistics Averages", UIConstants.LARGE_FONT_SIZE,
            UIManager.getColor("Label.foreground"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // Table panel with horizontal scrolling support
        JScrollPane tableScrollPane = new JScrollPane(averagesTable);
        tableScrollPane.setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING));
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(tableScrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING));

        // Left side: info label
        String infoText = String.format("Showing average feature values across %d processed images",
            perImageStats.size());
        JLabel infoLabel = UIUtils.createLabel(infoText, UIConstants.SMALL_FONT_SIZE,
            UIManager.getColor("Label.foreground"));
        buttonPanel.add(infoLabel, BorderLayout.CENTER);

        // Right side: export button
        JButton exportButton = UIUtils.createStandardButton("Export to CSV", FontIcon.of(FontAwesomeSolid.DOWNLOAD, 16));
        exportButton.addActionListener(this::exportToCSV);
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonWrapper.add(exportButton);
        buttonPanel.add(buttonWrapper, BorderLayout.EAST);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void updateTable() {
        // Clear existing data
        tableModel.setRowCount(0);

        if (perImageStats.isEmpty()) {
            tableModel.addRow(new Object[]{"No data available", ""});
            return;
        }

        // Collect all unique features across all images and ROI types
        Set<String> allFeatures = new LinkedHashSet<>();
        for (Map<UserROI.ROIType, Map<String, Double>> imageStats : perImageStats.values()) {
            for (Map<String, Double> roiTypeStats : imageStats.values()) {
                allFeatures.addAll(roiTypeStats.keySet());
            }
        }

        // Create column names: Image, ROI Type, [feature1, feature2, ...]
        java.util.List<String> featureList = new java.util.ArrayList<>(allFeatures);
        String[] columnNames = new String[2 + featureList.size()];
        columnNames[0] = "Image";
        columnNames[1] = "ROI Type";
        for (int i = 0; i < featureList.size(); i++) {
            columnNames[i + 2] = featureList.get(i);
        }

        // Set column names to table model
        tableModel.setColumnIdentifiers(columnNames);

        // Set renderers for numeric columns (Image and ROI Type are strings, features are numbers)
        for (int i = 2; i < columnNames.length; i++) {
            averagesTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Set minimum column widths to prevent cut-off text
        // Image: 150px, ROI Type: 100px, Features: 120px each
        averagesTable.getColumnModel().getColumn(0).setMinWidth(150);
        averagesTable.getColumnModel().getColumn(1).setMinWidth(100);
        for (int i = 2; i < columnNames.length; i++) {
            averagesTable.getColumnModel().getColumn(i).setMinWidth(120);
        }

        // Enable auto-resize off to allow horizontal scrolling when needed
        averagesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Process each image
        for (Map.Entry<String, Map<UserROI.ROIType, Map<String, Double>>> imageEntry : perImageStats.entrySet()) {
            String imageName = imageEntry.getKey();
            Map<UserROI.ROIType, Map<String, Double>> imageStats = imageEntry.getValue();

            // Process each ROI type for this image
            for (Map.Entry<UserROI.ROIType, Map<String, Double>> typeEntry : imageStats.entrySet()) {
                UserROI.ROIType roiType = typeEntry.getKey();
                Map<String, Double> featureMeans = typeEntry.getValue();
                String roiTypeName = roiType.getDisplayName();

                // Create row data
                Object[] rowData = new Object[columnNames.length];
                rowData[0] = imageName;
                rowData[1] = roiTypeName;

                // Add feature values
                for (int i = 0; i < featureList.size(); i++) {
                    String feature = featureList.get(i);
                    Double value = featureMeans.get(feature);
                    if (value != null) {
                        rowData[i + 2] = String.format("%.4f", value);
                    } else {
                        rowData[i + 2] = "";
                    }
                }

                tableModel.addRow(rowData);
            }
        }
    }

    private void exportToCSV(ActionEvent e) {
        if (perImageStats.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "No ROI statistics available to export.",
                "No Data",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Show file chooser for CSV export
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export ROI Averages to CSV");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));

        // Suggest filename
        fileChooser.setSelectedFile(new java.io.File("roi_averages.csv"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File outputFile = fileChooser.getSelectedFile();

            // Ensure .csv extension
            if (!outputFile.getName().toLowerCase().endsWith(".csv")) {
                outputFile = new java.io.File(outputFile.getAbsolutePath() + ".csv");
            }

            try {
                exportAveragesToCSV(outputFile);
            } catch (Exception ex) {
                LOGGER.error("Error exporting averages to CSV: {}", ex.getMessage());
                JOptionPane.showMessageDialog(
                    this,
                    "Error exporting to CSV:\n" + ex.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportAveragesToCSV(java.io.File outputFile) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            // Collect all unique features across all images and ROI types
            Set<String> allFeatures = new LinkedHashSet<>();
            for (Map<UserROI.ROIType, Map<String, Double>> imageStats : perImageStats.values()) {
                for (Map<String, Double> roiTypeStats : imageStats.values()) {
                    allFeatures.addAll(roiTypeStats.keySet());
                }
            }

            // Determine CSV format based on main settings
            String delimiter;
            String decimalSeparator;
            String formatType;

            if (mainSettings != null && mainSettings.useEuCsvFormat()) {
                delimiter = ";";
                decimalSeparator = ",";
                formatType = "European";
            } else {
                delimiter = ",";
                decimalSeparator = ".";
                formatType = "American";
            }

            // Sort features alphabetically for consistent column ordering
            java.util.List<String> featureList = new java.util.ArrayList<>(allFeatures);
            java.util.Collections.sort(featureList);

            // Write CSV header: Image,ROI Type,Feature1,Feature2,...
            writer.write("Image" + delimiter + "ROI Type");
            for (String feature : featureList) {
                // Escape commas in feature names
                String escapedFeature = feature.replace("\"", "\"\"");
                if (escapedFeature.contains(delimiter) || escapedFeature.contains("\"") || escapedFeature.contains("\n")) {
                    escapedFeature = "\"" + escapedFeature + "\"";
                }
                writer.write(delimiter + escapedFeature);
            }
            writer.write("\n");

            // Write data for each image and ROI type
            for (Map.Entry<String, Map<UserROI.ROIType, Map<String, Double>>> imageEntry : perImageStats.entrySet()) {
                String imageName = imageEntry.getKey();
                Map<UserROI.ROIType, Map<String, Double>> imageStats = imageEntry.getValue();

                for (Map.Entry<UserROI.ROIType, Map<String, Double>> typeEntry : imageStats.entrySet()) {
                    UserROI.ROIType roiType = typeEntry.getKey();
                    String roiTypeName = roiType.getDisplayName();
                    Map<String, Double> featureMeans = typeEntry.getValue();

                    // Escape image name if needed
                    String escapedImageName = imageName.replace("\"", "\"\"");
                    if (escapedImageName.contains(delimiter) || escapedImageName.contains("\"")) {
                        escapedImageName = "\"" + escapedImageName + "\"";
                    }

                    // Escape ROI type name if needed
                    String escapedROIName = roiTypeName.replace("\"", "\"\"");
                    if (escapedROIName.contains(delimiter) || escapedROIName.contains("\"")) {
                        escapedROIName = "\"" + escapedROIName + "\"";
                    }

                    // Write image name and ROI type
                    writer.write(escapedImageName + delimiter + escapedROIName);

                    // Write feature values with proper decimal separator
                    for (String feature : featureList) {
                        Double value = featureMeans.get(feature);
                        if (value != null) {
                            String formattedValue = String.format("%.4f", value).replace(".", decimalSeparator);
                            writer.write(delimiter + formattedValue);
                        } else {
                            writer.write(delimiter);
                        }
                    }
                    writer.write("\n");
                }
            }

            JOptionPane.showMessageDialog(
                this,
                String.format("ROI averages exported successfully!\nFormat: %s format\nSaved to: %s",
                    formatType, outputFile.getAbsolutePath()),
                "Export Successful",
                JOptionPane.INFORMATION_MESSAGE);

            LOGGER.info("Successfully exported ROI averages to: {}", outputFile.getAbsolutePath());
        }
    }
}