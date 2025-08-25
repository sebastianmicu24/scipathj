package com.scipath.scipathj.ui.dataset;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modern control panel for dataset creation with enhanced class management and visual controls.
 * Features color-coded class system, modern styling, and theme-aware interface design.
 * 
 * @author Sebastian Micu
 * @version 5.0.0
 */
public class DatasetControlsPanel extends JPanel {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetControlsPanel.class);
    
    // Theme-aware color scheme that adapts to light/dark themes
    private static Color getPrimaryColor() {
        return new Color(0, 123, 255); // #007bff - blue works in both themes
    }
    
    private static Color getSuccessColor() {
        return new Color(40, 167, 69); // #28a745 - green works in both themes
    }
    
    private static Color getDangerColor() {
        return new Color(220, 53, 69); // #dc3545 - red works in both themes
    }
    
    private static Color getBackgroundColor() {
        return UIManager.getColor("Panel.background");
    }
    
    private static Color getCardColor() {
        return UIManager.getColor("Panel.background");
    }
    
    private static Color getTextSecondaryColor() {
        return UIManager.getColor("Label.disabledForeground");
    }
    
    private static Color getBorderColor() {
        Color fg = UIManager.getColor("Label.foreground");
        return new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 64); // Semi-transparent border
    }
    
    private static final Color UNCLASSIFIED_COLOR = new Color(61, 61, 61);  // #3d3d3d
    
    // UI Components
    private JButton loadROIsButton;
    private JButton clearROIsButton;
    private JSlider borderWidthSlider;
    private JSlider fillOpacitySlider;
    private JCheckBox showNucleiCheckBox;
    private JCheckBox showCellsCheckBox;
    
    // Class management with colors
    private JComboBox<ClassItem> classComboBox;
    private JButton addClassButton;
    private DefaultComboBoxModel<ClassItem> classModel;
    private JButton colorPickerButton;
    private JPanel colorPreview;
    private JTextField classNameField;
    private JPanel classCountersPanel;
    private Map<String, Integer> classCounts = new HashMap<>();
    private Map<String, Color> classColors = new HashMap<>();
    
    // Integration
    private NewDatasetROIOverlay overlay;
    private final List<ControlListener> listeners = new ArrayList<>();
    
    /**
     * Class item for combo box with color support.
     */
    private static class ClassItem {
        private final String name;
        private final Color color;
        
        public ClassItem(String name, Color color) {
            this.name = name;
            this.color = color;
        }
        
        public String getName() { return name; }
        public Color getColor() { return color; }
        
        @Override
        public String toString() { return name; }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ClassItem) {
                return name.equals(((ClassItem) obj).name);
            }
            return false;
        }
        
        @Override
        public int hashCode() { return name.hashCode(); }
    }

    /**
     * Interface for control events.
     */
    public interface ControlListener {
        void onLoadROIsRequested();
        void onClearROIsRequested();
        void onVisualControlsChanged(float borderWidth, float fillOpacity, boolean showNuclei, boolean showCells);
        void onSelectedClassChanged(String className);
        void onClassAdded(String className, Color color);
    }
    
    public DatasetControlsPanel() {
        initializeDefaultClasses();
        initializeComponents();
        setupModernLayout();
        setupEventHandlers();
        
        LOGGER.debug("Created modern DatasetControlsPanel v5.0.0 (status panel removed)");
    }
    
    /**
     * Initialize default classes with colors.
     */
    private void initializeDefaultClasses() {
        classColors.put("Unclassified", UNCLASSIFIED_COLOR);
        classColors.put("Normal", new Color(76, 175, 80));   // Material Green
        classColors.put("Tumor", new Color(244, 67, 54));    // Material Red
        
        classCounts.put("Unclassified", 0);
        classCounts.put("Normal", 0);
        classCounts.put("Tumor", 0);
    }
    
    /**
     * Set the overlay to control.
     */
    public void setOverlay(NewDatasetROIOverlay overlay) {
        this.overlay = overlay;
        if (overlay != null) {
            // Register for overlay events
            overlay.addInteractionListener(new NewDatasetROIOverlay.InteractionListener() {
                @Override
                public void onROIClicked(com.scipath.scipathj.infrastructure.roi.UserROI roi, String assignedClass) {
                    LOGGER.debug("ROI '{}' clicked and assigned to class '{}'", roi.getName(), assignedClass);
                }
                
                @Override
                public void onROIHovered(com.scipath.scipathj.infrastructure.roi.UserROI roi) {
                    LOGGER.debug("ROI '{}' hovered", roi.getName());
                }
                
                @Override
                public void onClassAssigned(com.scipath.scipathj.infrastructure.roi.UserROI roi, String className) {
                    LOGGER.info("Assigned '{}' to {}", className, roi.getName());
                }
                
                @Override
                public void onProgressUpdate(int loaded, int total) {
                    updateProgress(loaded, total);
                }
            });
            
            // Pass current class colors to overlay
            overlay.setClassColors(classColors);
            
            // Apply initial visual settings
            applyVisualControls();
        }
    }
    
    /**
     * Add control listener.
     */
    public void addControlListener(ControlListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remove control listener.
     */
    public void removeControlListener(ControlListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Update status text (now just logs).
     */
    public void updateStatus(String status) {
        LOGGER.debug("Status: {}", status);
    }
    
    /**
     * Update progress (status removed, keeping for compatibility).
     */
    public void updateProgress(int current, int total) {
        SwingUtilities.invokeLater(() -> {
            // Update class counters based on current progress
            updateClassCounters();
            LOGGER.debug("Progress: {} / {} ROIs loaded", current, total);
        });
    }
    
    // === PRIVATE METHODS ===
    
    private void initializeComponents() {
        setBackground(getBackgroundColor());
        
        // File operations
        loadROIsButton = createModernButton("Load ROIs from ZIP", getSuccessColor());
        clearROIsButton = createModernButton("Clear ROIs", getDangerColor());
        
        // Visual controls
        borderWidthSlider = createModernSlider(1, 5, 2, 1);
        fillOpacitySlider = createModernSlider(0, 100, 20, 25);
        
        // Split outline checkbox into nuclei and cells
        showNucleiCheckBox = new JCheckBox("Show Nuclei", true);
        showCellsCheckBox = new JCheckBox("Show Cells", true);
        styleCheckBox(showNucleiCheckBox);
        styleCheckBox(showCellsCheckBox);
        
        // Class management
        classModel = new DefaultComboBoxModel<>();
        classModel.addElement(new ClassItem("Unclassified", UNCLASSIFIED_COLOR));
        classModel.addElement(new ClassItem("Normal", new Color(76, 175, 80)));
        classModel.addElement(new ClassItem("Tumor", new Color(244, 67, 54)));
        
        classComboBox = new JComboBox<>(classModel);
        classComboBox.setRenderer(new ClassItemRenderer());
        
        // Class creation components
        classNameField = new JTextField(15);
        styleTextField(classNameField);
        classNameField.setText("Enter class name");
        classNameField.setForeground(getTextSecondaryColor());
        
        colorPickerButton = createColorPickerButton();
        colorPreview = createColorPreview();
        addClassButton = createModernButton("Add Class", getPrimaryColor());
        
        // Class counters panel
        classCountersPanel = new JPanel();
        classCountersPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        classCountersPanel.setOpaque(false);
        updateClassCounters();
    }
    
    private void setupModernLayout() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Main panel with modern card-based sections
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);
        
        // File operations section
        mainPanel.add(createModernSection("File Operations", createFileOperationsPanel()));
        mainPanel.add(Box.createVerticalStrut(15));
        
        // Visual controls section
        mainPanel.add(createModernSection("Display Settings", createVisualControlsPanel()));
        mainPanel.add(Box.createVerticalStrut(15));
        
        // Class management section
        mainPanel.add(createModernSection("Class Assignment", createClassManagementPanel()));
        mainPanel.add(Box.createVerticalStrut(15));
        
        // Class counters section
        mainPanel.add(createModernSection("Class Counts", classCountersPanel));
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createFileOperationsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        panel.setOpaque(false);
        panel.add(loadROIsButton);
        panel.add(clearROIsButton);
        return panel;
    }
    
    private JPanel createVisualControlsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.insets = new Insets(5, 5, 5, 5);
        panel.add(new JLabel("Border Width:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(borderWidthSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Fill Opacity:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(fillOpacitySlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        panel.add(showNucleiCheckBox, gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(showCellsCheckBox, gbc);
        
        return panel;
    }
    
    private JPanel createClassManagementPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Current class selection
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.insets = new Insets(5, 5, 5, 5);
        panel.add(new JLabel("Selected Class:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(classComboBox, gbc);
        
        // Add new class row
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        panel.add(colorPickerButton, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(classNameField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(addClassButton, gbc);
        
        return panel;
    }
    
    private JPanel createModernSection(String title, JPanel content) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(getCardColor());
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        // Title label
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(getPrimaryColor());
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        section.add(titleLabel, BorderLayout.NORTH);
        section.add(content, BorderLayout.CENTER);
        
        return section;
    }
    
    // === MODERN STYLING METHODS ===
    
    private JButton createModernButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
    
    private JSlider createModernSlider(int min, int max, int value, int majorTick) {
        JSlider slider = new JSlider(min, max, value);
        slider.setMajorTickSpacing(majorTick);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setOpaque(false);
        return slider;
    }
    
    private void styleCheckBox(JCheckBox checkBox) {
        checkBox.setOpaque(false);
        checkBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        checkBox.setForeground(UIManager.getColor("CheckBox.foreground"));
    }
    
    private void styleTextField(JTextField textField) {
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            new EmptyBorder(6, 8, 6, 8)
        ));
        
        // Add placeholder functionality
        textField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (textField.getText().equals("Enter class name")) {
                    textField.setText("");
                    textField.setForeground(UIManager.getColor("TextField.foreground"));
                }
            }
            
            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (textField.getText().isEmpty()) {
                    textField.setText("Enter class name");
                    textField.setForeground(getTextSecondaryColor());
                }
            }
        });
    }
    
    private JButton createColorPickerButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(40, 30));
        button.setBackground(new Color(255, 87, 34)); // Material Orange
        button.setBorder(BorderFactory.createLineBorder(getBorderColor(), 1));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Class Color", button.getBackground());
            if (newColor != null) {
                button.setBackground(newColor);
                colorPreview.setBackground(newColor);
            }
        });
        
        return button;
    }
    
    private JPanel createColorPreview() {
        JPanel preview = new JPanel();
        preview.setPreferredSize(new Dimension(20, 20));
        preview.setBackground(new Color(255, 87, 34)); // Material Orange
        preview.setBorder(BorderFactory.createLineBorder(getBorderColor(), 1));
        return preview;
    }
    
    private void updateClassCounters() {
        classCountersPanel.removeAll();
        
        for (Map.Entry<String, Color> entry : classColors.entrySet()) {
            String className = entry.getKey();
            Color color = entry.getValue();
            int count = classCounts.getOrDefault(className, 0);
            
            JLabel counter = new JLabel(className + ": " + count);
            counter.setOpaque(true);
            counter.setBackground(color);
            counter.setForeground(getContrastColor(color));
            counter.setBorder(new EmptyBorder(5, 10, 5, 10));
            counter.setFont(new Font("Segoe UI", Font.BOLD, 11));
            
            classCountersPanel.add(counter);
        }
        
        classCountersPanel.revalidate();
        classCountersPanel.repaint();
    }
    
    private Color getContrastColor(Color color) {
        // Calculate luminance
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255.0;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }
    
    /**
     * Custom renderer for class combo box with colors.
     */
    private class ClassItemRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof ClassItem) {
                ClassItem item = (ClassItem) value;
                setBackground(isSelected ? list.getSelectionBackground() : item.getColor());
                setForeground(isSelected ? list.getSelectionForeground() : getContrastColor(item.getColor()));
                setText(item.getName());
            }
            
            return this;
        }
    }
    
    private void setupEventHandlers() {
        // File operations
        loadROIsButton.addActionListener(e -> handleLoadROIs());
        clearROIsButton.addActionListener(e -> handleClearROIs());
        
        // Visual controls
        borderWidthSlider.addChangeListener(e -> applyVisualControls());
        fillOpacitySlider.addChangeListener(e -> applyVisualControls());
        showNucleiCheckBox.addActionListener(e -> applyVisualControls());
        showCellsCheckBox.addActionListener(e -> applyVisualControls());
        
        // Class selection
        classComboBox.addActionListener(e -> handleClassSelection());
        addClassButton.addActionListener(e -> handleAddClass());
    }
    
    private void handleLoadROIs() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("ZIP files", "zip"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // For simplicity, assume image name for now
            String imageName = "P1 - 9 - 03.tif"; // This would come from image selection
            
            if (overlay != null) {
                overlay.loadROIsFromZip(selectedFile, imageName);
            }
            
            updateStatus("Loading ROIs from: " + selectedFile.getName());
            
            // Notify listeners
            notifyListeners(listener -> listener.onLoadROIsRequested());
        }
    }
    
    private void handleClearROIs() {
        if (overlay != null) {
            overlay.clear();
        }
        
        updateStatus("ROIs cleared");
        
        // Notify listeners
        notifyListeners(listener -> listener.onClearROIsRequested());
    }
    
    private void applyVisualControls() {
        float borderWidth = borderWidthSlider.getValue();
        float fillOpacity = fillOpacitySlider.getValue() / 100.0f;
        boolean showNuclei = showNucleiCheckBox.isSelected();
        boolean showCells = showCellsCheckBox.isSelected();
        
        if (overlay != null) {
            // Use the new method with separate nuclei/cells visibility
            overlay.setVisualControls(borderWidth, fillOpacity, showNuclei, showCells);
        }
        
        // Notify listeners
        notifyListeners(listener -> listener.onVisualControlsChanged(borderWidth, fillOpacity, showNuclei, showCells));
    }
    
    private void handleClassSelection() {
        ClassItem selectedItem = (ClassItem) classComboBox.getSelectedItem();
        if (selectedItem != null && overlay != null) {
            overlay.setSelectedClass(selectedItem.getName());
        }
        
        // Notify listeners
        String selectedClass = selectedItem != null ? selectedItem.getName() : null;
        notifyListeners(listener -> listener.onSelectedClassChanged(selectedClass));
    }
    
    private void handleAddClass() {
        String newClass = classNameField.getText().trim();
        if (newClass.isEmpty() || newClass.equals("Enter class name")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid class name.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (containsClass(newClass)) {
            JOptionPane.showMessageDialog(this, "Class already exists.", "Duplicate Class", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Color selectedColor = colorPickerButton.getBackground();
        ClassItem newItem = new ClassItem(newClass, selectedColor);
        
        classModel.addElement(newItem);
        classComboBox.setSelectedItem(newItem);
        
        // Add to maps
        classColors.put(newClass, selectedColor);
        classCounts.put(newClass, 0);
        
        // Update overlay with new color
        if (overlay != null) {
            overlay.setClassColor(newClass, selectedColor);
        }
        
        // Update UI
        updateClassCounters();
        classNameField.setText("Enter class name");
        classNameField.setForeground(getTextSecondaryColor());
        
        // Notify listeners
        notifyListeners(listener -> listener.onClassAdded(newClass, selectedColor));
        
        LOGGER.debug("Added new class: {} with color: {}", newClass, selectedColor);
    }
    
    private boolean containsClass(String className) {
        for (int i = 0; i < classModel.getSize(); i++) {
            ClassItem item = classModel.getElementAt(i);
            if (className.equals(item.getName())) {
                return true;
            }
        }
        return false;
    }
    
    private void notifyListeners(java.util.function.Consumer<ControlListener> action) {
        for (ControlListener listener : listeners) {
            try {
                action.accept(listener);
            } catch (Exception e) {
                LOGGER.error("Error notifying control listener", e);
            }
        }
    }
    
    /**
     * Updates class count for a specific class.
     */
    public void updateClassCount(String className, int count) {
        classCounts.put(className, count);
        updateClassCounters();
    }
    
    /**
     * Gets the color for a specific class.
     */
    public Color getClassColor(String className) {
        return classColors.getOrDefault(className, UNCLASSIFIED_COLOR);
    }
    
    /**
     * Gets the currently selected class name.
     */
    public String getSelectedClassName() {
        ClassItem selected = (ClassItem) classComboBox.getSelectedItem();
        return selected != null ? selected.getName() : "Unclassified";
    }
}