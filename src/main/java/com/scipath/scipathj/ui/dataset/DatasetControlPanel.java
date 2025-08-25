package com.scipath.scipathj.ui.dataset;

import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced control panel for dataset creation with interactive features
 * inspired by the HTML Cell Classifier.
 * 
 * Features:
 * - Dynamic class management with color picker
 * - Visual controls (opacity, border width sliders)
 * - Real-time statistics display
 * - Save/load classification data
 * - Keyboard shortcuts support
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 */
public class DatasetControlPanel extends JPanel implements DatasetROIManager.DatasetInteractionListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetControlPanel.class);
    
    private final DatasetROIManager datasetManager;
    
    // Class management components
    private JColorChooser classColorPicker;
    private JTextField classNameField;
    private JButton addClassButton;
    private JComboBox<String> classSelectionDropdown;
    private JButton removeClassButton;
    
    // Visual control components
    private JSlider opacitySlider;
    private JSlider borderWidthSlider;
    private JCheckBox outlinesVisibleCheckbox;
    private JLabel opacityValueLabel;
    private JLabel borderWidthValueLabel;
    
    // Statistics display
    private JPanel statisticsPanel;
    private JLabel totalROIsLabel;
    private JLabel assignedROIsLabel;
    private JLabel unassignedROIsLabel;
    
    // Save/Load components
    private JButton saveClassificationButton;
    private JButton loadClassificationButton;
    
    public DatasetControlPanel(DatasetROIManager datasetManager) {
        this.datasetManager = datasetManager;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        
        // Register as listener
        datasetManager.addInteractionListener(this);
        
        // Initial update
        updateStatistics();
        updateClassDropdown();
        
        LOGGER.debug("Created DatasetControlPanel with interactive features");
    }
    
    private void initializeComponents() {
        // Class management components
        classColorPicker = new JColorChooser(Color.RED);
        classColorPicker.setPreviewPanel(new JPanel()); // Remove preview panel to save space
        
        classNameField = new JTextField(15);
        classNameField.setToolTipText("Enter class name");
        
        addClassButton = new JButton("Add Class");
        addClassButton.setToolTipText("Add new class with selected color");
        
        classSelectionDropdown = new JComboBox<>();
        classSelectionDropdown.setToolTipText("Select active class for ROI assignment");
        
        removeClassButton = new JButton("Remove Class");
        removeClassButton.setToolTipText("Remove selected class");
        
        // Visual control components
        opacitySlider = new JSlider(0, 100, 20); // 0-100% opacity
        opacitySlider.setMajorTickSpacing(25);
        opacitySlider.setMinorTickSpacing(5);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setPaintLabels(true);
        opacitySlider.setToolTipText("Fill opacity (0-100%)");
        
        borderWidthSlider = new JSlider(0, 50, 20); // 0-5.0 pixels (scaled by 10)
        borderWidthSlider.setMajorTickSpacing(10);
        borderWidthSlider.setMinorTickSpacing(5);
        borderWidthSlider.setPaintTicks(true);
        borderWidthSlider.setPaintLabels(true);
        borderWidthSlider.setToolTipText("Border width (0-5.0 pixels)");
        
        outlinesVisibleCheckbox = new JCheckBox("Show Outlines", true);
        outlinesVisibleCheckbox.setToolTipText("Toggle outline visibility (E key)");
        
        opacityValueLabel = new JLabel("20%");
        borderWidthValueLabel = new JLabel("2.0px");
        
        // Statistics components
        totalROIsLabel = new JLabel("Total ROIs: 0");
        assignedROIsLabel = new JLabel("Assigned: 0");
        unassignedROIsLabel = new JLabel("Unassigned: 0");
        
        // Save/Load components
        saveClassificationButton = new JButton("Save Classification");
        saveClassificationButton.setToolTipText("Save class assignments to JSON file");
        
        loadClassificationButton = new JButton("Load Classification");
        loadClassificationButton.setToolTipText("Load class assignments from JSON file");
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createPadding(UIConstants.SMALL_SPACING));
        
        // Main content panel with multiple sections
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        // Class Management Section
        JPanel classManagementPanel = createClassManagementPanel();
        mainPanel.add(classManagementPanel);
        mainPanel.add(Box.createVerticalStrut(UIConstants.MEDIUM_SPACING));
        
        // Visual Controls Section
        JPanel visualControlsPanel = createVisualControlsPanel();
        mainPanel.add(visualControlsPanel);
        mainPanel.add(Box.createVerticalStrut(UIConstants.MEDIUM_SPACING));
        
        // Statistics Section
        statisticsPanel = createStatisticsPanel();
        mainPanel.add(statisticsPanel);
        mainPanel.add(Box.createVerticalStrut(UIConstants.MEDIUM_SPACING));
        
        // Save/Load Section
        JPanel saveLoadPanel = createSaveLoadPanel();
        mainPanel.add(saveLoadPanel);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createClassManagementPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Class Management"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        
        // Color picker (compact)
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH;
        JPanel colorPanel = new JPanel(new BorderLayout());
        colorPanel.add(new JLabel("Color:"), BorderLayout.WEST);
        JPanel colorPickerPanel = new JPanel();
        colorPickerPanel.setBackground(classColorPicker.getColor());
        colorPickerPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        colorPickerPanel.setPreferredSize(new Dimension(40, 25));
        colorPickerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                Color newColor = JColorChooser.showDialog(DatasetControlPanel.this, "Choose Class Color", classColorPicker.getColor());
                if (newColor != null) {
                    classColorPicker.setColor(newColor);
                    colorPickerPanel.setBackground(newColor);
                }
            }
        });
        colorPanel.add(colorPickerPanel, BorderLayout.CENTER);
        panel.add(colorPanel, gbc);
        
        // Class name input
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(classNameField, gbc);
        
        // Add class button
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(addClassButton, gbc);
        
        // Remove class button
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(removeClassButton, gbc);
        
        // Class selection dropdown
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(classSelectionDropdown, gbc);
        
        return panel;
    }
    
    private JPanel createVisualControlsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Visual Controls"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        
        // Opacity control
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Fill Opacity:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(opacitySlider, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        panel.add(opacityValueLabel, gbc);
        
        // Border width control
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Border Width:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(borderWidthSlider, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        panel.add(borderWidthValueLabel, gbc);
        
        // Outlines checkbox
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(outlinesVisibleCheckbox, gbc);
        
        return panel;
    }
    
    private JPanel createStatisticsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 2, 2));
        panel.setBorder(new TitledBorder("Statistics"));
        
        panel.add(totalROIsLabel);
        panel.add(assignedROIsLabel);
        panel.add(unassignedROIsLabel);
        
        return panel;
    }
    
    private JPanel createSaveLoadPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 5, 0));
        panel.setBorder(new TitledBorder("Data Management"));
        
        panel.add(saveClassificationButton);
        panel.add(loadClassificationButton);
        
        return panel;
    }
    
    private void setupEventHandlers() {
        // Add class button
        addClassButton.addActionListener(e -> {
            String className = classNameField.getText().trim();
            if (!className.isEmpty()) {
                Color color = classColorPicker.getColor();
                datasetManager.createClass(className, color);
                classNameField.setText("");
                // Color picker color will be updated via listener
            }
        });
        
        // Remove class button
        removeClassButton.addActionListener(e -> {
            String selectedClass = (String) classSelectionDropdown.getSelectedItem();
            if (selectedClass != null && !selectedClass.equals("Unclassified")) {
                int result = JOptionPane.showConfirmDialog(this,
                    "Remove class '" + selectedClass + "' and unassign all ROIs?",
                    "Confirm Class Removal",
                    JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    datasetManager.removeClass(selectedClass);
                }
            }
        });
        
        // Class selection dropdown
        classSelectionDropdown.addActionListener(e -> {
            String selectedClass = (String) classSelectionDropdown.getSelectedItem();
            if (selectedClass != null) {
                datasetManager.setSelectedClass(selectedClass);
            }
        });
        
        // Opacity slider
        opacitySlider.addChangeListener(e -> {
            float opacity = opacitySlider.getValue() / 100.0f;
            datasetManager.setFillOpacity(opacity);
            opacityValueLabel.setText(String.format("%.0f%%", opacity * 100));
        });
        
        // Border width slider
        borderWidthSlider.addChangeListener(e -> {
            float borderWidth = borderWidthSlider.getValue() / 10.0f;
            datasetManager.setBorderWidth(borderWidth);
            borderWidthValueLabel.setText(String.format("%.1fpx", borderWidth));
        });
        
        // Outlines checkbox
        outlinesVisibleCheckbox.addActionListener(e -> {
            datasetManager.toggleOutlines();
        });
        
        // Save/Load buttons
        saveClassificationButton.addActionListener(this::saveClassificationData);
        loadClassificationButton.addActionListener(this::loadClassificationData);
    }
    
    private void saveClassificationData(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Classification Data");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Files", "json"));
        fileChooser.setSelectedFile(new File("cell_classification.json"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                saveClassificationToFile(file);
                JOptionPane.showMessageDialog(this, "Classification data saved successfully!");
            } catch (IOException ex) {
                LOGGER.error("Failed to save classification data", ex);
                JOptionPane.showMessageDialog(this, "Failed to save classification data: " + ex.getMessage(), 
                                            "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void loadClassificationData(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Classification Data");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Files", "json"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                loadClassificationFromFile(file);
                JOptionPane.showMessageDialog(this, "Classification data loaded successfully!");
            } catch (IOException ex) {
                LOGGER.error("Failed to load classification data", ex);
                JOptionPane.showMessageDialog(this, "Failed to load classification data: " + ex.getMessage(), 
                                            "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void saveClassificationToFile(File file) throws IOException {
        // Simple JSON-like format for class assignments
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"classes\": {\n");
        
        Map<String, Color> classColors = datasetManager.getAllClassColors();
        Map<String, String> assignments = datasetManager.getAllClassAssignments();
        
        boolean firstClass = true;
        for (Map.Entry<String, Color> entry : classColors.entrySet()) {
            if (!firstClass) json.append(",\n");
            firstClass = false;
            
            String className = entry.getKey();
            Color color = entry.getValue();
            
            json.append("    \"").append(className).append("\": {\n");
            json.append("      \"color\": \"#").append(String.format("%06X", color.getRGB() & 0xFFFFFF)).append("\",\n");
            json.append("      \"assignments\": [");
            
            boolean firstAssignment = true;
            for (Map.Entry<String, String> assignment : assignments.entrySet()) {
                if (assignment.getValue().equals(className)) {
                    if (!firstAssignment) json.append(", ");
                    firstAssignment = false;
                    json.append("\"").append(assignment.getKey()).append("\"");
                }
            }
            
            json.append("]\n");
            json.append("    }");
        }
        
        json.append("\n  }\n");
        json.append("}\n");
        
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(json.toString());
        }
        
        LOGGER.info("Saved classification data to {}", file.getAbsolutePath());
    }
    
    private void loadClassificationFromFile(File file) throws IOException {
        // This is a simplified loader - in a real implementation you'd use Jackson or similar
        String content = Files.readString(file.toPath());
        LOGGER.info("Loaded classification data from {}", file.getAbsolutePath());
        // TODO: Implement proper JSON parsing and class restoration
    }
    
    private void updateStatistics() {
        DatasetROIManager.DatasetStatistics stats = datasetManager.getDatasetStatistics();
        totalROIsLabel.setText("Total ROIs: " + stats.totalROIs());
        assignedROIsLabel.setText("Assigned: " + stats.assignedROIs());
        unassignedROIsLabel.setText("Unassigned: " + (stats.totalROIs() - stats.assignedROIs()));
    }
    
    private void updateClassDropdown() {
        classSelectionDropdown.removeAllItems();
        classSelectionDropdown.addItem("Unclassified");
        
        for (String className : datasetManager.getAllAvailableClasses()) {
            if (!className.equals("Unclassified")) {
                classSelectionDropdown.addItem(className);
            }
        }
        
        classSelectionDropdown.setSelectedItem(datasetManager.getSelectedClass());
    }
    
    // === DatasetInteractionListener Implementation ===
    
    @Override
    public void onROISelected(UserROI roi) {
        // Could add ROI-specific info display here
    }
    
    @Override
    public void onROIHovered(UserROI roi) {
        // Could add hover info display here
    }
    
    @Override
    public void onROIUnhovered(UserROI roi) {
        // Could remove hover info display here
    }
    
    @Override
    public void onClassCreated(String className, Color color) {
        updateClassDropdown();
        updateStatistics();
        classSelectionDropdown.setSelectedItem(className);
    }
    
    @Override
    public void onClassRemoved(String className) {
        updateClassDropdown();
        updateStatistics();
    }
    
    @Override
    public void onSelectedClassChanged(String className) {
        classSelectionDropdown.setSelectedItem(className);
    }
    
    @Override
    public void onVisualSettingsChanged(float opacity, float borderWidth, boolean outlinesVisible) {
        // Update UI controls to match programmatic changes
        SwingUtilities.invokeLater(() -> {
            opacitySlider.setValue(Math.round(opacity * 100));
            borderWidthSlider.setValue(Math.round(borderWidth * 10));
            outlinesVisibleCheckbox.setSelected(outlinesVisible);
            opacityValueLabel.setText(String.format("%.0f%%", opacity * 100));
            borderWidthValueLabel.setText(String.format("%.1fpx", borderWidth));
        });
    }
    
    /**
     * Cleanup resources when control panel is disposed.
     */
    public void dispose() {
        datasetManager.removeInteractionListener(this);
        LOGGER.debug("Disposed DatasetControlPanel");
    }
}