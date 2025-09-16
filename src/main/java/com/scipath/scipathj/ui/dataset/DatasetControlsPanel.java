package com.scipath.scipathj.ui.dataset;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import ij.gui.Roi;
import ij.WindowManager;
import ij.ImagePlus;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.infrastructure.config.MainSettings;
import com.scipath.scipathj.analysis.algorithms.classification.FeatureExtraction;
import com.scipath.scipathj.analysis.config.FeatureExtractionSettings;
import com.scipath.scipathj.ui.utils.ImageLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modern control panel for dataset creation with enhanced class management and visual controls.
 * Features color-coded class system, modern styling, and theme-aware interface design.
 * Rewritten with robust color square rendering that maintains size regardless of layout constraints.
 * 
 * @author Sebastian Micu
 * @version 5.1.0
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
    
    private static final Color UNCLASSIFIED_COLOR = new Color(255, 255, 0);  // Default yellow ROI color
    
    // UI Components
    private JButton loadROIsButton;
    private JButton clearROIsButton;
    private JButton downloadTrainingDataButton;
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
    private ImagePlus currentImagePlus;

    // Reference to DatasetImageViewer for accessing image files
    private DatasetImageViewer datasetImageViewer;

    // Feature extraction components
    private com.scipath.scipathj.analysis.algorithms.classification.FeatureExtraction featureExtractor;
    private Map<String, Map<String, Object>> currentFeatures;
    
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
        
        LOGGER.debug("Created robust DatasetControlsPanel v5.1.0 with improved color square rendering");
    }
    
    /**
     * Initialize default classes with colors.
     */
    private void initializeDefaultClasses() {
        classColors.put("Unclassified", UNCLASSIFIED_COLOR);

        classCounts.put("Unclassified", 0);
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
                    updateClassCountsFromOverlay();
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

            // Update class counts from existing ROIs
            updateClassCountsFromOverlay();
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
             // Update class counters from overlay when loading completes
             if (current == total && current > 0) {
                 updateClassCountsFromOverlay();
             }
             LOGGER.debug("Progress: {} / {} ROIs loaded", current, total);
         });
     }
    
    // === PRIVATE METHODS ===
    
    private void initializeComponents() {
        setBackground(getBackgroundColor());
        
        // File operations
        loadROIsButton = createModernButton("Load ROIs from ZIP", getSuccessColor());
        clearROIsButton = createModernButton("Clear ROIs", getDangerColor());
        downloadTrainingDataButton = createModernButton("Download Training Data", new Color(102, 102, 255)); // Dark blue
        
        // Visual controls
        borderWidthSlider = createModernSlider(1, 5, 2, 1);
        fillOpacitySlider = createModernSlider(0, 100, 20, 25);
        
        // Split outline checkbox into nuclei and cells
        showNucleiCheckBox = new JCheckBox("Show Nuclei", true);
        showCellsCheckBox = new JCheckBox("Show Cells", true);
        styleCheckBox(showNucleiCheckBox);
        styleCheckBox(showCellsCheckBox);
        
        // Class management - using robust renderer
        classModel = new DefaultComboBoxModel<>();
        classModel.addElement(new ClassItem("Unclassified", UNCLASSIFIED_COLOR));
        
        classComboBox = new JComboBox<>(classModel);
        classComboBox.setRenderer(new RobustClassItemRenderer());

        // Ensure minimum dimensions for combo box to always show color squares properly
        classComboBox.setMinimumSize(new Dimension(140, 28));
        classComboBox.setPreferredSize(new Dimension(180, 28));

        // Additional protection against layout compression
        classComboBox.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                Dimension currentSize = classComboBox.getSize();
                if (currentSize.width < 120) {
                    // Prevent excessive compression
                    classComboBox.setSize(Math.max(120, currentSize.width), currentSize.height);
                }
            }
        });
        
        // Class creation components
        classNameField = new JTextField(15);
        styleTextField(classNameField);
        classNameField.setText("Enter class name");
        classNameField.setForeground(getTextSecondaryColor());
        
        colorPickerButton = createColorPickerButton();
        colorPreview = createColorPreview();
        addClassButton = createModernButton("Add Class", getPrimaryColor());

        // Class counters panel - use adaptive layout to prevent overflow
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
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
        panel.setOpaque(false);
        panel.add(loadROIsButton);
        panel.add(clearROIsButton);
        panel.add(downloadTrainingDataButton);
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
        // Set fixed size for the color picker button
        button.setPreferredSize(new Dimension(40, 30));
        button.setMinimumSize(new Dimension(40, 30));
        button.setMaximumSize(new Dimension(40, 30));
        button.setSize(new Dimension(40, 30));
        button.setBackground(new Color(255, 87, 34)); // Material Orange
        button.setBorder(BorderFactory.createLineBorder(getBorderColor(), 1));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Disable layout expansion
        button.setBorderPainted(true);
        button.setFocusPainted(false);

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
     * Adaptive renderer that scales color square based on available space.
     * Prevents the square from becoming a dot when panel width is constrained.
     */
    private class RobustClassItemRenderer extends JLabel implements ListCellRenderer<ClassItem> {
        private static final int PREFERRED_SQUARE_SIZE = 16;
        private static final int MIN_SQUARE_SIZE = 8;
        private final int BORDER_MARGIN = 2;

        private ClassItem currentItem;
        private int availableWidth = 0;

        public RobustClassItemRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ClassItem> list, ClassItem value,
                                                    int index, boolean isSelected, boolean cellHasFocus) {
            this.currentItem = value;

            // Set text and basic properties
            setText(value != null ? value.getName() : "");
            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            setFont(new Font("Segoe UI", Font.PLAIN, 12));

            // Add left padding to prevent text from overlapping the color square
            setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 0));

            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (currentItem != null) {
                // Calculate dynamic square size and position based on available space
                Dimension size = getSize();
                int availableWidth = size.width;
                int availableHeight = size.height;

                // Calculate square size - scale down if space is limited
                int squareSize = calculateDynamicSquareSize(availableWidth, availableHeight);
                int squareX = calculateDynamicSquareX(availableWidth, squareSize);
                int y = Math.max(1, (availableHeight - squareSize) / 2);

                // Ensure square stays within bounds
                if (squareX + squareSize > availableWidth) {
                    squareSize = Math.max(MIN_SQUARE_SIZE, availableWidth - squareX - BORDER_MARGIN);
                }
                if (squareSize < MIN_SQUARE_SIZE) {
                    squareSize = MIN_SQUARE_SIZE;
                    squareX = Math.max(0, availableWidth - squareSize - BORDER_MARGIN);
                }

                // Fill square
                g.setColor(currentItem.getColor());
                g.fillRect(squareX, y, squareSize, squareSize);

                // Draw border
                g.setColor(Color.DARK_GRAY);
                g.drawRect(squareX, y, squareSize, squareSize);
            }
        }

        private int calculateDynamicSquareSize(int availableWidth, int availableHeight) {
            // Try preferred size first
            if (availableWidth >= PREFERRED_SQUARE_SIZE + BORDER_MARGIN * 2 + 40) { // Space for square + text
                return PREFERRED_SQUARE_SIZE;
            }

            // Scale down proportionally if space is limited
            int maxSquareSize = Math.min(availableWidth - BORDER_MARGIN * 2 - 20, availableHeight - 4);
            return Math.max(MIN_SQUARE_SIZE, Math.min(PREFERRED_SQUARE_SIZE, maxSquareSize));
        }

        private int calculateDynamicSquareX(int availableWidth, int squareSize) {
            // Position at the left, with small margin
            return BORDER_MARGIN;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension textSize = super.getPreferredSize();
            // Ensure enough space for preferred square + text
            int preferredWidth = PREFERRED_SQUARE_SIZE + BORDER_MARGIN * 2 + textSize.width + 10;
            return new Dimension(preferredWidth, Math.max(24, textSize.height));
        }

        @Override
        public Dimension getMinimumSize() {
            // Absolute minimum - smaller than before to allow more flexibility
            return new Dimension(MIN_SQUARE_SIZE + BORDER_MARGIN * 2 + 15, 18);
        }

        public int getAppropriateSquareSize(int availableWidth) {
            return calculateDynamicSquareSize(availableWidth, PREFERRED_SQUARE_SIZE);
        }
    }
    
    private void setupEventHandlers() {
        // File operations
        loadROIsButton.addActionListener(e -> handleLoadROIs());
        clearROIsButton.addActionListener(e -> handleClearROIs());
        downloadTrainingDataButton.addActionListener(e -> handleDownloadTrainingData());
        
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

        // Reset class counts when clearing ROIs
        for (String className : classCounts.keySet()) {
            classCounts.put(className, 0);
        }
        updateClassCounters();

        updateStatus("ROIs cleared");

        // Notify listeners
        notifyListeners(listener -> listener.onClearROIsRequested());
    }

    /**
     * Handle the download training data button click.
     * Uses the existing analysis pipeline to extract features from manually classified cells.
     */
    private void handleDownloadTrainingData() {
        try {
            if (overlay == null) {
                JOptionPane.showMessageDialog(this, "No ROI overlay available. Please load ROIs first.",
                                                "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (datasetImageViewer == null || datasetImageViewer.getCurrentImageFile() == null) {
                JOptionPane.showMessageDialog(this, "No image file loaded. Please load an image first.",
                                                "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if we have any ROIs with manual classifications (excluding unassigned)
            boolean hasManualClassifications = false;
            int totalCells = 0;
            for (String className : classCounts.keySet()) {
                Integer count = classCounts.get(className);
                if (count != null && count > 0) {
                    totalCells += count;
                    if (!"Unclassified".equals(className)) {
                        hasManualClassifications = true;
                    }
                }
            }

            if (!hasManualClassifications) {
                String message = totalCells == 0
                    ? "No cells found in the current overlay. Please load ROIs first."
                    : totalCells + " cells found, but all are unclassified. Please classify some cells first.";
                JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Show file chooser to select output location
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File("training_data.json"));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File outputFile = fileChooser.getSelectedFile();

                // Extract features using the analysis pipeline approach
                extractAndSaveTrainingDataUsingPipeline(outputFile);

                updateStatus("Training data saved to: " + outputFile.getName());
                JOptionPane.showMessageDialog(this, "Training data successfully saved using the analysis pipeline!\n" +
                        "This includes full feature extraction with H&E deconvolution.\n" +
                        "You can now use this comprehensive JSON file to train your XGBoost classifier.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception e) {
            LOGGER.error("Error downloading training data", e);
            JOptionPane.showMessageDialog(this, "Error extracting training data: " + e.getMessage(),
                                            "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Extract features from classified cells using the analysis pipeline and save to JSON file.
     */
    private void extractAndSaveTrainingDataUsingPipeline(File outputFile) throws Exception {
        File imageFile = datasetImageViewer.getCurrentImageFile();
        String imageFileName = datasetImageViewer.getCurrentImageFileName();

        // Load the image using the analysis pipeline's ImageLoader (loads into ImageJ's global window manager)
        ij.ImagePlus imagePlus = ImageLoader.loadImage(imageFile.getAbsolutePath());
        if (imagePlus == null) {
            throw new Exception("Failed to load image: " + imageFileName);
        }

        // Initialize configuration manager and settings for feature extraction
        ConfigurationManager configManager = new ConfigurationManager();
        MainSettings mainSettings = configManager.loadMainSettings();
        FeatureExtractionSettings featureSettings = configManager.loadFeatureExtractionSettings();

        // Get all ROIs from the overlay
        Map<String, UserROI> allROIs = getAllROIsFromOverlay();
        if (allROIs.isEmpty()) {
            throw new Exception("No ROIs found in overlay");
        }

        // Separate ROIs by type for FeatureExtraction
        List<UserROI> vesselROIs = new ArrayList<>();
        List<UserROI> nucleusROIs = new ArrayList<>();
        List<UserROI> cytoplasmROIs = new ArrayList<>();
        List<UserROI> cellROIs = new ArrayList<>();
        List<UserROI> classifiedCells = new ArrayList<>();
        List<UserROI> classifiedNuclei = new ArrayList<>();
        List<UserROI> classifiedCytoplasms = new ArrayList<>();

        // Initialize cytoplasm ROIs collection to track what we find
        int initialCytoplasmCount = 0;

        // DEBUG: Log all ROIs before categorization
        LOGGER.info("=== ROI CATEGORIZATION DEBUGGING START ===");
        LOGGER.info("Total ROIs to categorize: {}", allROIs.size());
        int cytoplasmCount = 0, nucleusCount = 0, cellCount = 0, vesselCount = 0;
        for (UserROI roi : allROIs.values()) {
            switch (roi.getType()) {
                case CYTOPLASM:
                    cytoplasmCount++;
                    break;
                case NUCLEUS:
                    nucleusCount++;
                    break;
                case CELL:
                    cellCount++;
                    break;
                case VESSEL:
                    vesselCount++;
                    break;
            }
        }
        LOGGER.info("ROI type counts: Cells={}, Nuclei={}, Cytoplasm={}, Vessels={}", cellCount, nucleusCount, cytoplasmCount, vesselCount);

        // Log specific details about Cytoplasm ROIs
        if (cytoplasmCount > 0) {
            LOGGER.info("Cytoplasm ROI details:");
            int counter = 0;
            for (UserROI roi : allROIs.values()) {
                if (roi.getType() == com.scipath.scipathj.infrastructure.roi.UserROI.ROIType.CYTOPLASM && counter < 10) {
                    LOGGER.info("  Cytoplasm ROI: {} (class: '{}', assignedClass: '{}')",
                                roi.getName(),
                                roi.getType(),
                                roi.getAssignedClass() != null ? "'" + roi.getAssignedClass() + "'" : "NULL");
                    counter++;
                }
            }
        } else {
            LOGGER.warn("NO CYTOPLASM ROIs found in overlay - this is likely the issue!");
        }
        LOGGER.info("=== ROI CATEGORIZATION DEBUGGING END ===");

        // Categorize ROIs and filter for manually classified ROIs
        for (UserROI roi : allROIs.values()) {
            switch (roi.getType()) {
                case VESSEL:
                    vesselROIs.add(roi);
                    break;
                case NUCLEUS:
                    nucleusROIs.add(roi);
                    String nucleusClassName = roi.getAssignedClass();
                    // Include classified nuclei
                    if (nucleusClassName != null && !nucleusClassName.trim().isEmpty()) {
                        classifiedNuclei.add(roi);
                    }
                    break;
                case CYTOPLASM:
                    cytoplasmROIs.add(roi);
                    String cytoplasmClassName = roi.getAssignedClass();
                    // Include classified cytoplasm
                    if (cytoplasmClassName != null && !cytoplasmClassName.trim().isEmpty()) {
                        classifiedCytoplasms.add(roi);
                        LOGGER.debug("Added CYTOPLASM ROI to classified: {} -> class '{}'", roi.getName(), cytoplasmClassName);
                    } else {
                        LOGGER.debug("CYTOPLASM ROI not classified: {} -> class '{}' (isEmpty: {})",
                                     roi.getName(), cytoplasmClassName, cytoplasmClassName != null ? cytoplasmClassName.trim().isEmpty() : "NULL");
                    }
                    break;
                case CELL:
                    cellROIs.add(roi);
                    String cellClassName = roi.getAssignedClass();
                    // Include classified cells
                    if (cellClassName != null && !cellClassName.trim().isEmpty()) {
                        classifiedCells.add(roi);
                    }
                    break;
            }
        }

        if (classifiedCells.isEmpty() && classifiedNuclei.isEmpty() && classifiedCytoplasms.isEmpty()) {
           // Comprehensive debugging info to understand why no classified ROIs
           LOGGER.warn("=== CLASSIFICATION DEBUGGING START ===");
           LOGGER.warn("No manually classified ROIs found. Comprehensive debugging info:");
           LOGGER.warn("Total ROIs extracted from overlay: {}", allROIs.size());
           LOGGER.warn("Total cells extracted: {}", cellROIs.size());
           LOGGER.warn("Total nuclei extracted: {}", nucleusROIs.size());
           LOGGER.warn("Total cytoplasm extracted: {}", cytoplasmROIs.size());

           // Log detailed classification status for each ROI type
           LOGGER.warn("=== DETAILED CLASSIFICATION BREAKDOWN ===");
           LOGGER.warn("CLASSIFIED CELLS: {}", classifiedCells.size());
           for (UserROI cell : classifiedCells.subList(0, Math.min(classifiedCells.size(), 5))) {
               LOGGER.warn(" - Cell: {} -> class: '{}'", cell.getName(), cell.getAssignedClass());
           }

           LOGGER.warn("CLASSIFIED NUCLEI: {}", classifiedNuclei.size());
           for (UserROI nucleus : classifiedNuclei.subList(0, Math.min(classifiedNuclei.size(), 5))) {
               LOGGER.warn(" - Nuclei: {} -> class: '{}'", nucleus.getName(), nucleus.getAssignedClass());
           }

           LOGGER.warn("CLASSIFIED CYTOPLASM: {}", classifiedCytoplasms.size());
           for (UserROI cytoplasm : classifiedCytoplasms.subList(0, Math.min(classifiedCytoplasms.size(), 5))) {
               LOGGER.warn(" - Cytoplasm: {} -> class: '{}'", cytoplasm.getName(), cytoplasm.getAssignedClass());
           }

            // Log all ROIs with their details
            LOGGER.warn("=== ALL ROI DETAILS ===");
            for (UserROI roi : allROIs.values()) {
                LOGGER.warn("ROI: {} -> class: '{}' (type: {}, file: {}, key: {})",
                    roi.getName(),
                    roi.getAssignedClass() != null ? "'" + roi.getAssignedClass() + "'" : "NULL",
                    roi.getType(),
                    roi.getImageFileName(),
                    roi.getImageFileName() + ":" + roi.getName());
            }

            // Log cell-specific details with filtering analysis
            LOGGER.warn("=== CELL-SPECIFIC ANALYSIS ===");
            for (UserROI roi : cellROIs) {
                String className = roi.getAssignedClass();
                boolean hasClass = className != null && !className.trim().isEmpty();
                boolean isUnclassified = "Unclassified".equals(className);
                boolean wouldBeIncluded = hasClass && !isUnclassified;

                LOGGER.warn("Cell ROI: '{}' -> class: '{}' (hasClass: {}, isUnclassified: {}, included: {})",
                    roi.getName(), className != null ? "'" + className + "'" : "NULL",
                    hasClass, isUnclassified, wouldBeIncluded);
            }

            LOGGER.warn("=== CLASSIFICATION DEBUGGING END ===");
            throw new Exception("No manually classified cells found - see detailed debug logs above");
        }

        LOGGER.info("Creating FeatureExtraction with {} vessels, {} nuclei, {} cytoplasm, {} cells ({} classified)",
                vesselROIs.size(), nucleusROIs.size(), cytoplasmROIs.size(), cellROIs.size(), classifiedCells.size());

        // Create FeatureExtraction instance using the same approach as AnalysisPipeline
        FeatureExtraction featureExtraction = new FeatureExtraction(
            imagePlus,
            imageFileName,
            vesselROIs,
            nucleusROIs,
            cytoplasmROIs,
            cellROIs,
            featureSettings,
            mainSettings
        );

        // Extract features for all ROIs
        Map<String, Map<String, Object>> allFeatures = featureExtraction.extractFeatures();
        LOGGER.info("Feature extraction completed - extracted features for {} ROIs total", allFeatures.size());

        // Reorganize features by image name -> cell ID -> ROI type
        Map<String, Map<String, Map<String, Map<String, Object>>>> organizedFeatures = new LinkedHashMap<>();

        // CRITICAL FIX: Add classification info to features that would otherwise have null class
        // Feature extraction doesn't include the class info, so we need to add it from our ROIs
        Map<String, String> roiClassMapping = new LinkedHashMap<>();
        for (UserROI roi : allROIs.values()) {
            if (roi.getAssignedClass() != null && !roi.getAssignedClass().isEmpty()) {
                String roiKey = roi.getImageFileName() + "_" + roi.getType().toString() + "_" + roi.getName().split("_")[1];
                roiClassMapping.put(roiKey, roi.getAssignedClass());
                LOGGER.debug("Mapped ROI class: '{}' -> class '{}'", roiKey, roi.getAssignedClass());
            }
        }

        // Debug: Log first 10 feature keys to verify structure
        int keyCount = 0;
        for (String key : allFeatures.keySet()) {
            Map<String, Object> features = allFeatures.get(key);
            // FIX: Determine the cell ID from the feature key and look up its class
            // Feature keys look like: "P1 - 9 - 03.tif_Cell_1541"
            String cellId = null;
            String roiType = null;

            // Parse roiType and cellId from feature key
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".*_([^_]+)_(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(key);
            if (matcher.matches()) {
                roiType = matcher.group(1).toUpperCase(); // "CELL", "NUCLEUS", etc.
                cellId = matcher.group(2); // "1541", etc.
            }

            // Look for this combination in our classification mapping
            if (cellId != null && roiType != null) {
                for (String mappedKey : roiClassMapping.keySet()) {
                    // mappedKey looks like: "P1 - 9 - 03.tif_CELL_1005"
                    // We match if the cell ID matches (ignore exact roiType for now)
                    if (mappedKey.contains(cellId)) {
                        if (features == null) {
                            features = new LinkedHashMap<>();
                            allFeatures.put(key, features);
                        }
                        String assignedClass = roiClassMapping.get(mappedKey);
                        features.put("class", assignedClass);
                        LOGGER.debug("Added class '{}' to feature key '{}' (matched with {})",
                                   assignedClass, key, mappedKey);
                        break;
                    }
                }
            }

            if (keyCount < 5) {
                LOGGER.debug("Feature key example: '{}' -> class: '{}'",
                    key, features != null && features.containsKey("class") ? features.get("class") : "null");
            }
            keyCount++;
        }
        LOGGER.info("Sample keys analyzed. Total features to organize: {}", allFeatures.size());

        // Process all features (now with class info added)
        for (String key : allFeatures.keySet()) {
            Map<String, Object> features = allFeatures.get(key);
            if (features != null && features.containsKey("class")) {
                parseAndOrganizeFeatures(key, features, organizedFeatures);
            }
        }

        // DEBUG: Log what we have after organization
        LOGGER.info("=== ORGANIZATION SUMMARY ===");
        LOGGER.info("Total organized images: {}", organizedFeatures.size());
        for (String imgName : organizedFeatures.keySet()) {
            Map<String, Map<String, Map<String, Object>>> img = organizedFeatures.get(imgName);
            LOGGER.info("Image '{}' has {} cell IDs", imgName, img.size());
            for (String cellId : img.keySet().toArray(new String[0])) {
                // Only log first few
                if (img.keySet().size() <= 5 || cellId.equals("395")) { // Include cell 395 for debugging
                    Map<String, Map<String, Object>> types = img.get(cellId);
                    LOGGER.info("  Cell {} has ROI types: {}", cellId, types.keySet());
                    if (cellId.equals("395")) {
                        LOGGER.info("    Cell 395 details:");
                        for (String type : types.keySet()) {
                            LOGGER.info("      {}: {} features", type, types.get(type).size());
                        }
                    }
                }
                if (img.keySet().size() > 5 && !cellId.equals("395")) break; // Skip most for brevity
            }
        }
        LOGGER.info("=== END ORGANIZATION SUMMARY ===");

        // Filter to only include images and cells that have manual classifications
        Map<String, Map<String, Map<String, Map<String, Object>>>> filteredFeatures = new LinkedHashMap<>();

        LOGGER.info("Starting filter process - {} organized images, {} classified cells, {} classified nuclei, {} classified cytoplasm",
                   organizedFeatures.size(), classifiedCells.size(), classifiedNuclei.size(), classifiedCytoplasms.size());

        // Quick debug of what we're trying to match
        if (!classifiedCells.isEmpty() && !organizedFeatures.isEmpty()) {
            UserROI firstCell = classifiedCells.get(0);
            String firstImage = organizedFeatures.keySet().iterator().next();
            Map<String, Map<String, Map<String, Object>>> firstImageData = organizedFeatures.get(firstImage);
            String firstFeatureCellId = firstImageData.keySet().iterator().next();

            LOGGER.info("Sample mapping: Classified cell '{}' (type={}) -> looking for features like '{}' in image '{}'",
                       firstCell.getName(), firstCell.getType(), firstFeatureCellId, firstImage);
        }

        java.util.Set<String> matchedCells = new java.util.HashSet<>();
        int totalClassifiedROIs = classifiedCells.size() + classifiedNuclei.size() + classifiedCytoplasms.size();

        // Helper method to process classified ROIs of any type
        java.util.function.BiConsumer<java.util.List<UserROI>, String> processClassifiedROIs =
            (roiList, roiTypeName) -> {
                LOGGER.info("Processing {} classified {} ROIs for feature matching...", roiList.size(), roiTypeName);
                for (UserROI roi : roiList) {
                    String[] parts = roi.getName().split("_");
                    if (parts.length >= 2) {
                        String imageName = roi.getImageFileName();
                        String roiName = roi.getName();
                        String roiClass = roi.getAssignedClass();

                        // Extract numerical ID from ROI name (e.g., "Cell_15" -> "15", "Nucleus_3" -> "3")
                        String baseId;
                        try {
                            String[] nameParts = roiName.split("_");
                            if (nameParts.length >= 2) {
                                baseId = nameParts[nameParts.length - 1]; // Last part should be the number
                            } else {
                                continue; // Skip if can't parse
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Could not parse ID from ROI name: '{}'", roiName);
                            continue;
                        }

                        LOGGER.debug("Looking for features matching {} ({}) with class '{}'", roiName, baseId, roiClass);

                        // Try to find matching features
                        boolean found = false;
                        for (String imageKey : organizedFeatures.keySet()) {
                            LOGGER.debug("Checking image key: '{}' (looking for match with '{}')", imageKey, imageName);
                            if (!imageKey.contains(imageName.split("\\.")[0])) { // Loose image name matching
                                continue;
                            }

                            Map<String, Map<String, Map<String, Object>>> imageFeatures = organizedFeatures.get(imageKey);
                            LOGGER.debug("Found {} feature cell IDs for image '{}'", imageFeatures.size(), imageKey);

                            for (String featureCellId : imageFeatures.keySet()) {
                                Map<String, Map<String, Object>> roiTypes = imageFeatures.get(featureCellId);

                                LOGGER.debug("Checking feature cell ID '{}' against ROI ID '{}'", featureCellId, baseId);

                                // Try exact match first
                                if (baseId.equals(featureCellId)) {
                                    // Found matching cell ID
                                    String targetKey = "classified_" + roi.getType().toString().toLowerCase() + "_" + baseId;
                                    matchedCells.add(targetKey);

                                    if (!filteredFeatures.containsKey(imageName)) {
                                        filteredFeatures.put(imageName, new LinkedHashMap<>());
                                    }
                                    if (!filteredFeatures.get(imageName).containsKey(baseId)) {
                                        filteredFeatures.get(imageName).put(baseId, new LinkedHashMap<>());
                                    }

                                    // Add all ROI type features for this cell ID
                                    filteredFeatures.get(imageName).get(baseId).putAll(roiTypes);
                                    LOGGER.info("SUCCESS: MATCHED {} -> {} ROI types included for cell {}", roiTypeName, roiTypes.size(), baseId);

                                    LOGGER.debug("MATCHED {}: {} ({}) -> features for {}", roiTypeName, roiName, baseId, featureCellId);
                                    found = true;
                                    break;
                                }
                            }

                            if (found) {
                                LOGGER.debug("Found match in image '{}'", imageKey);
                                break;
                            }
                        }

                        if (!found) {
                            LOGGER.warn("No match found for classified {}: {} (id={}) - check if features were extracted for this ROI", roiTypeName, roiName, baseId);
                        }
                    } else {
                        LOGGER.warn("Invalid ROI name for {} category: '{}'", roiTypeName, parts.length);
                    }
                }
            };

        // Process all classified ROI types
        LOGGER.info("Processing {} classified cells...", classifiedCells.size());
        processClassifiedROIs.accept(classifiedCells, "CELL");

        LOGGER.info("Processing {} classified nuclei...", classifiedNuclei.size());
        processClassifiedROIs.accept(classifiedNuclei, "NUCLEUS");

        LOGGER.info("Processing {} classified cytoplasm...", classifiedCytoplasms.size());
        processClassifiedROIs.accept(classifiedCytoplasms, "CYTOPLASM");

        LOGGER.info("Filter process complete - processed {} ROIs total, matched {} entries, {} total cells in output",
                   totalClassifiedROIs, matchedCells.size(), countTotalCells(filteredFeatures));

        // Clean up the ImagePlus to free memory
        if (imagePlus != null) {
            imagePlus.close();
        }

        // Create JSON structure with new format
        Map<String, Object> jsonData = new LinkedHashMap<>();
        jsonData.put("timestamp", new java.util.Date().toString());
        jsonData.put("totalClassifiedCells", countTotalCells(filteredFeatures));
        jsonData.put("classes", new ArrayList<>(classCounts.keySet()));
        jsonData.put("classifiedROIs", filteredFeatures);
        jsonData.put("featureExtractionSettings", featureSettings.toString());

        // Convert to JSON and save
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, jsonData);

        LOGGER.info("Training data saved using analysis pipeline - reorganized structure with {} images",
                filteredFeatures.size());
    }

    /**
     * Get all ROIs from the overlay.
     * Uses reflection as a workaround since the overlay currently doesn't expose ROIs directly.
     */
    private java.util.Map<String, UserROI> getAllROIsFromOverlay() {
        Map<String, UserROI> allROIs = new HashMap<>();

        LOGGER.info("=== ROI EXTRACTION DEBUGGING ===");
        LOGGER.info("Attempting to get ROIs from overlay. ROI overlay present: {}", overlay != null);
        if (overlay != null) {
            LOGGER.info("Overlay ROICount from getROICount(): {}", overlay.getROICount());

            // Try to get classification counts as additional validation
            Map<String, Integer> currentCounts = overlay.getClassificationCounts();
            if (currentCounts != null && !currentCounts.isEmpty()) {
                LOGGER.info("Current classification counts in overlay: {}", currentCounts);
            } else {
                LOGGER.warn("No classification counts available from overlay");
            }
        }

        // Try multiple extraction methods in order of preference
        // NOTE: For training data export, we want ALL ROIs (including cytoplasm),
        // not just the ones that are visually displayed
        boolean extractionSuccess = false;

        LOGGER.info("=== TRAINING DATA: Attempting comprehensive ROI extraction ===");

        // Method 1: Direct reflection access to allROIs field (including hidden ROIs)
        extractionSuccess = extractViaDirectReflection(allROIs, true); // true = include all ROIs
        LOGGER.info("Method 1 result: {} (found {} ROIs)", extractionSuccess, allROIs.size());

        if (!extractionSuccess) {
            // Method 2: Alternative reflection methods with full access
            extractionSuccess = extractViaAlternativeMethods(allROIs, true); // true = include all ROIs
            LOGGER.info("Method 2 result: {} (found {} ROIs)", extractionSuccess, allROIs.size());
        }

        if (!extractionSuccess) {
            // Method 3: Fallback to ROI Manager (will get all available ROIs)
            Map<String, UserROI> fallbackROIs = getROIsFromFallbackSources();
            allROIs.putAll(fallbackROIs);
            extractionSuccess = !fallbackROIs.isEmpty();
            LOGGER.info("Method 3 result: {} (found {} ROIs)", extractionSuccess, allROIs.size());
        }

        // ADDITIONAL METHOD: Try to access internal ROI storage that might bypass visibility filters
        if (!extractionSuccess) {
            try {
                extractionSuccess = extractFromInternalStorage(allROIs);
                LOGGER.info("Method 4 (Internal Storage) result: {} (total {} ROIs)",
                           extractionSuccess, allROIs.size());
            } catch (Exception e) {
                LOGGER.warn("Method 4 failed: {}", e.getMessage());
            }
        }

        // EMERGENCY METHOD: Try to directly reconstruct cytoplasm ROIs from overlay data
        // (Only if there are no ROIs found at all - cytoplasm reconstruct check happens after categorization)
        if (!extractionSuccess) {
            LOGGER.info("=== EMERGENCY: Attempting to reconstruct missing cytoplasm ROIs ===");
            extractionSuccess = reconstructHiddenCytoplasmROIs(allROIs);
            LOGGER.info("Emergency reconstruction result: {} (total {} ROIs)",
                       extractionSuccess, allROIs.size());
        }

        LOGGER.info("Final ROI extraction result: {} total ROIs extracted to training data", allROIs.size());
        LOGGER.info("=== END ROI EXTRACTION DEBUGGING ===");

        return allROIs;
    }

    /**
     * Try to extract ROIs from overlay's internal storage that might contain hidden ROIs
     * This method attempts to access private fields that might store filtered data
     */
    private boolean extractFromInternalStorage(Map<String, UserROI> targetMap) {
        try {
            // Try to access any internal storage fields that might contain all ROIs
            java.lang.reflect.Field[] fields = overlay.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName().toLowerCase();

                // Look for fields that might contain ROI data
                if (fieldName.contains("roi") || fieldName.contains("all") || fieldName.contains("data")) {
                    Object fieldValue = field.get(overlay);
                    if (fieldValue instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, UserROI> roiMap = (Map<String, UserROI>) fieldValue;
                        if (roiMap != null && !roiMap.isEmpty()) {
                            int addedCount = 0;
                            for (UserROI roi : roiMap.values()) {
                                if (roi != null && roi.getImageFileName() != null) {
                                    String key = roi.getImageFileName() + ":" + roi.getName();
                                    if (!targetMap.containsKey(key)) { // Don't overwrite existing
                                        targetMap.put(key, roi);
                                        addedCount++;

                                        if (roi.getType() == com.scipath.scipathj.infrastructure.roi.UserROI.ROIType.CYTOPLASM) {
                                            LOGGER.debug("🔍 INTERNAL STORAGE: Found cytoplasm ROI: {} (class: '{}')",
                                                        roi.getName(),
                                                        roi.getAssignedClass() != null ? roi.getAssignedClass() : "NULL");
                                        }
                                    }
                                }
                            }
                            if (addedCount > 0) {
                                LOGGER.info("Internal storage extraction via {}: {} ROIs", fieldName, addedCount);
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Internal storage extraction failed: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Emergency method to try to reconstruct missing cytoplasm ROIs
     * This attempts to find cytoplasm ROIs through alternative access methods
     */
    private boolean reconstructHiddenCytoplasmROIs(Map<String, UserROI> targetMap) {
        LOGGER.warn("=== ENTERING EMERGENCY CYTOPLASM RECOVERY MODE ===");

        try {
            // Try to access any method on the overlay that might return ROIs
            java.lang.reflect.Method[] methods = overlay.getClass().getDeclaredMethods();
            int methodsTried = 0;

            for (java.lang.reflect.Method method : methods) {
                if (method.getReturnType().equals(List.class) || method.getReturnType().equals(Map.class)) {
                    method.setAccessible(true);
                    String methodName = method.getName();

                    // Try methods that look like they return ROI data
                    if (methodName.toLowerCase().contains("roi") || methodName.toLowerCase().contains("get") ||
                        methodName.toLowerCase().contains("list") || methodName.toLowerCase().contains("collect")) {

                        try {
                            Object result = method.invoke(overlay);

                            if (result instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, UserROI> roiMap = (Map<String, UserROI>) result;
                                int cytoplasmFound = 0;

                                for (UserROI roi : roiMap.values()) {
                                    if (roi.getType() == com.scipath.scipathj.infrastructure.roi.UserROI.ROIType.CYTOPLASM) {
                                        cytoplasmFound++;
                                        String key = roi.getImageFileName() + ":" + roi.getName();
                                        if (!targetMap.containsKey(key)) {
                                            targetMap.put(key, roi);
                                            LOGGER.info("🔥 EMERGENCY RECOVERY: Reconstructed cytoplasm ROI: {} (class: '{}')",
                                                        roi.getName(),
                                                        roi.getAssignedClass() != null ? roi.getAssignedClass() : "NULL");
                                        }
                                    }
                                }

                                if (cytoplasmFound > 0) {
                                    LOGGER.info("✅ EMERGENCY RECOVERY SUCCESS: Found {} cytoplasm ROIs via {}", cytoplasmFound, methodName);
                                    return true;
                                }

                            } else if (result instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<UserROI> roiList = (List<UserROI>) result;
                                int cytoplasmFound = 0;

                                for (UserROI roi : roiList) {
                                    if (roi.getType() == com.scipath.scipathj.infrastructure.roi.UserROI.ROIType.CYTOPLASM) {
                                        cytoplasmFound++;
                                        String key = roi.getImageFileName() + ":" + roi.getName();
                                        if (!targetMap.containsKey(key)) {
                                            targetMap.put(key, roi);
                                            LOGGER.info("🔥 EMERGENCY RECOVERY: Reconstructed cytoplasm ROI: {} (class: '{}')",
                                                        roi.getName(),
                                                        roi.getAssignedClass() != null ? roi.getAssignedClass() : "NULL");
                                        }
                                    }
                                }

                                if (cytoplasmFound > 0) {
                                    LOGGER.info("✅ EMERGENCY RECOVERY SUCCESS: Found {} cytoplasm ROIs via {}", cytoplasmFound, methodName);
                                    return true;
                                }
                            }

                            methodsTried++;
                        } catch (Exception invokerEx) {
                            // Just skip this method
                        }
                    }
                }
            }

            LOGGER.warn("=== EMERGENCY CYTOPLASM RECOVERY: Tried {} methods, found 0 cytoplasm ROIs ===", methodsTried);

        } catch (Exception e) {
            LOGGER.error("Emergency cytoplasm recovery failed: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Extract ROIs directly from allROIs field using reflection
     * @param targetMap Map to store extracted ROIs
     * @param includeAll If true, bypasses any visibility filters for training data
     */
    private boolean extractViaDirectReflection(Map<String, UserROI> targetMap, boolean includeAll) {
        try {
            java.lang.reflect.Field allROIsField = overlay.getClass().getDeclaredField("allROIs");
            allROIsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<UserROI> roiList = (List<UserROI>) allROIsField.get(overlay);

            if (roiList != null && !roiList.isEmpty()) {
                int addedCount = 0;
                int skippedHidden = 0;
                for (UserROI roi : roiList) {
                    if (roi != null && roi.getImageFileName() != null) {
                        // For training data export, include ALL ROIs regardless of visibility
                        String key = roi.getImageFileName() + ":" + roi.getName();
                        targetMap.put(key, roi);
                        addedCount++;

                        // Debug log for cytoplasm ROIs
                        if (roi.getType() == com.scipath.scipathj.infrastructure.roi.UserROI.ROIType.CYTOPLASM) {
                            LOGGER.debug("✅ TRAINING DATA: Included cytoplasmic ROI: {} (class: '{}')",
                                        roi.getName(),
                                        roi.getAssignedClass() != null ? roi.getAssignedClass() : "NULL");
                        }
                    }
                }
                LOGGER.info("Direct reflection extracted {} ROIs{}",
                           addedCount,
                           includeAll ? " (including hidden)" : "");
                return addedCount > 0;
            }
        } catch (Exception e) {
            LOGGER.warn("Direct reflection failed: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extract ROIs using alternative reflection methods
     * @param targetMap Map to store extracted ROIs
     * @param includeAll If true, bypasses any visibility filters for training data
     */
    private boolean extractViaAlternativeMethods(Map<String, UserROI> targetMap, boolean includeAll) {
        // Try accessing via renderer field
        try {
            java.lang.reflect.Field rendererField = overlay.getClass().getDeclaredField("renderer");
            rendererField.setAccessible(true);
            Object renderer = rendererField.get(overlay);

            if (renderer != null) {
                // Try to get ROIs from renderer
                java.lang.reflect.Field roiListField = renderer.getClass().getDeclaredField("roiList");
                roiListField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<UserROI> roiList = (List<UserROI>) roiListField.get(renderer);

                if (roiList != null && !roiList.isEmpty()) {
                    int count = 0;
                    for (UserROI roi : roiList) {
                        if (roi != null && roi.getImageFileName() != null) {
                            targetMap.put(roi.getImageFileName() + ":" + roi.getName(), roi);
                            count++;

                            // Debug log for cytoplasm ROIs
                            if (roi.getType() == com.scipath.scipathj.infrastructure.roi.UserROI.ROIType.CYTOPLASM) {
                                LOGGER.debug("✅ TRAINING DATA (ALT): Included cytoplasmic ROI: {} (class: '{}')",
                                            roi.getName(),
                                            roi.getAssignedClass() != null ? roi.getAssignedClass() : "NULL");
                            }
                        }
                    }
                    LOGGER.info("Renderer method extracted {} ROIs{}",
                               count,
                               includeAll ? " (including hidden)" : "");
                    return count > 0;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Renderer method failed: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Fallback method to get ROIs if reflection fails.
     */
    private Map<String, UserROI> getROIsFromFallbackSources() {
        Map<String, UserROI> allROIs = new HashMap<>();

        // Try to get ROIs from ImageJ ROI Manager if possible
        try {
            if (datasetImageViewer != null && datasetImageViewer.getCurrentImagePlus() != null) {
                ij.ImagePlus currentImage = datasetImageViewer.getCurrentImagePlus();
                ij.plugin.frame.RoiManager roiManagerFrame = ij.plugin.frame.RoiManager.getInstance();
                if (roiManagerFrame != null) {
                    Roi[] rois = roiManagerFrame.getRoisAsArray();
                    for (int i = 0; i < rois.length; i++) {
                        String imageName = datasetImageViewer.getCurrentImageFileName();
                        if (imageName != null) {
                            String roiName = "Cell_" + (i + 1); // Generic naming
                            UserROI userROI = new UserROI(rois[i], imageName, roiName);
                            userROI.setAssignedClass("Unclassified"); // Default classification
                            allROIs.put(imageName + ":" + roiName, userROI);
                        }
                    }
                    LOGGER.debug("Extracted {} ROIs from ImageJ ROI Manager", allROIs.size());
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not get ROIs from ImageJ ROI Manager: {}", e.getMessage());
        }

        return allROIs;
    }

    /**
     * Calculate circularity for a ROI.
     */
    private double calculateCircularity(UserROI roi) {
        try {
            Roi imageJRoi = roi.getImageJRoi();
            if (imageJRoi != null) {
                double area = imageJRoi.getStatistics().area;
                double perimeter = imageJRoi.getLength();
                if (perimeter > 0) {
                    return (4.0 * Math.PI * area) / (perimeter * perimeter);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error calculating circularity for ROI {}: {}", roi.getName(), e.getMessage());
        }
        return 1.0; // Default circularity (circle)
    }

    /**
     * Create basic features as fallback when comprehensive feature extraction fails.
     */
    private Map<String, Object> createBasicFeatures(UserROI roi) {
        Map<String, Object> features = new LinkedHashMap<>();
        features.put("class", roi.getAssignedClass());
        features.put("area", roi.getArea());
        features.put("x", roi.getCenterX());
        features.put("y", roi.getCenterY());
        features.put("width", roi.getWidth());
        features.put("height", roi.getHeight());
        features.put("circularity", calculateCircularity(roi));
        features.put("bounding_box", roi.getBounds().toString());
        return features;
    }

    /**
     * Parse feature key and organize by image -> cell ID -> ROI type.
     * Expected key format: "imageName_ROIType_XX" or "imageName_ROIType_YY" or similar.
     */
    private void parseAndOrganizeFeatures(String key, Map<String, Object> features,
                                         Map<String, Map<String, Map<String, Map<String, Object>>>> organizedData) {
        try {
            // Parse the key - expected format: "imageName_ROIType_cellId"
            // e.g., "P1 - 9 - 03.tif_Nucleus_1", "P1 - 9 - 03.tif_Cell_1", "P1 - 9 - 03.tif_Cytoplasm_1"
            String[] parts = key.split("_");
            if (parts.length < 3) {
                // If parsing fails, try alternative format or skip
                return;
            }

            // Extract components - handle complex file names with dashes
            String roiType;
            String cellId;
            String imageName;

            // Check if we have at least 3 parts after splitting
            if (parts.length >= 3) {
                // Last part is cell ID
                cellId = parts[parts.length - 1];

                // Second to last part is ROI type
                roiType = parts[parts.length - 2];

                // Everything before is image name (reconstruct with underscores if needed)
                if (parts.length > 3) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < parts.length - 2; i++) {
                        if (i > 0) sb.append("_");
                        sb.append(parts[i]);
                    }
                    imageName = sb.toString();
                } else {
                    imageName = parts[0];
                }
            } else {
                // Fallback for unexpected format
                imageName = key;
                roiType = "Unknown";
                cellId = "0";
            }

            // Ensure proper ROI type mapping
            if (!roiType.equals("Cell") && !roiType.equals("Nucleus") && !roiType.equals("Cytoplasm")) {
                // If not a recognized type, try to interpret differently
                if (roiType.toLowerCase().contains("cell")) {
                    roiType = "Cell";
                } else if (roiType.toLowerCase().contains("nucleus")) {
                    roiType = "Nucleus";
                } else if (roiType.toLowerCase().contains("cyto")) {
                    roiType = "Cytoplasm";
                }
            }

            // Initialize nested maps
            organizedData.computeIfAbsent(imageName, k -> new LinkedHashMap<>());
            organizedData.get(imageName).computeIfAbsent(cellId, k -> new LinkedHashMap<>());

            // Add features for this ROI type
            organizedData.get(imageName).get(cellId).put(roiType, features);

        } catch (Exception e) {
            LOGGER.debug("Error parsing ROI key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Count total unique cells across all images.
     */
    private int countTotalCells(Map<String, Map<String, Map<String, Map<String, Object>>>> data) {
        int totalCells = 0;
        for (Map<String, Map<String, Map<String, Object>>> imageData : data.values()) {
            totalCells += imageData.size(); // Each image key represents one cell
        }
        return totalCells;
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
    
    /**
     * Update class counts from the overlay.
     */
    private void updateClassCountsFromOverlay() {
        if (overlay != null) {
            java.util.Map<String, Integer> overlayCounts = overlay.getClassificationCounts();
            if (overlayCounts != null) {
                // Reset counts to 0
                for (String className : classCounts.keySet()) {
                    classCounts.put(className, 0);
                }
                // Update with overlay counts
                for (java.util.Map.Entry<String, Integer> entry : overlayCounts.entrySet()) {
                    classCounts.put(entry.getKey(), entry.getValue());
                }
                updateClassCounters();
                LOGGER.debug("Updated class counts from overlay: {}", classCounts);
            }
        }
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

    /**
     * Set the current ImagePlus for feature extraction.
     */
    public void setCurrentImage(ImagePlus imagePlus) {
        this.currentImagePlus = imagePlus;
    }

    /**
     * Get the current ImagePlus.
     */
    public ImagePlus getCurrentImage() {
        return currentImagePlus;
    }

    /**
     * Set the DatasetImageViewer reference for accessing image files.
     */
    public void setDatasetImageViewer(DatasetImageViewer viewer) {
        this.datasetImageViewer = viewer;
    }
}