package com.scipath.scipathj.ui.dataset.controller;

import com.scipath.scipathj.ui.dataset.view.DatasetControlsView;
import com.scipath.scipathj.ui.dataset.model.*;
import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.ui.dataset.NewDatasetROIOverlay;
import com.scipath.scipathj.ui.dataset.DatasetImageViewer;
import com.scipath.scipathj.ui.main.MainWindow;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Controller for DatasetControlsPanel.
 * Handles event coordination between View and Model components.
 * Follows the MVC pattern with observers for loose coupling.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class DatasetControlsController {

    // Core components
    private final DatasetControlsView view;
    private final TargetClassManager classManager;
    private final FileOperationsHandler fileHandler;
    private final FeatureExtractorRefactored featureExtractor;
    private final ConfigurationManager configurationManager;

    // External dependencies
    private NewDatasetROIOverlay overlay;
    private DatasetImageViewer datasetImageViewer;
    private MainWindow mainWindow;

    // State
    private VisualControlsState visualState;
    private ProgressState progressState;
    private boolean initialized = false;

    /**
     * Creates a new DatasetControlsController.
     *
     * @param view The view component
     */
    public DatasetControlsController(DatasetControlsView view) {
        this(view, null); // Backward compatibility - call the ConfigurationManager constructor with null
    }

    /**
     * Creates a new DatasetControlsController with ConfigurationManager for comprehensive feature extraction.
     *
     * @param view The view component
     * @param configurationManager The configuration manager for feature extraction
     */
    public DatasetControlsController(DatasetControlsView view, ConfigurationManager configurationManager) {
        this.view = view;
        this.configurationManager = configurationManager;
        this.visualState = new VisualControlsState();
        this.progressState = new ProgressState();

        // Initialize core components
        this.classManager = new TargetClassManager();
        this.fileHandler = new FileOperationsHandler(
            this::updateStatus,
            this::updateProgress,
            this::clearROIsCallback
        );
        this.featureExtractor = createFeatureExtractor();

        // Initialize UI with default state
        view.updateVisualControls(visualState);

        // Setup event handlers
        setupEventHandlers();
        setupClassManagers();

        initialized = true;
    }

    /**
     * Create the feature extractor with dependencies.
     */
    private FeatureExtractorRefactored createFeatureExtractor() {
        return new FeatureExtractorRefactored(
            configurationManager, // Now properly injected from constructor
            this::updateStatus,
            this::updateProgressPercentage
        );
    }

    /**
     * Setup all event handlers between View and Model components.
     */
    private void setupEventHandlers() {
        // File operation handlers
        view.getLoadROIsButton().addActionListener(e -> handleLoadROIs());
        view.getClearROIsButton().addActionListener(e -> handleClearROIs());
        view.getDownloadTrainingDataButton().addActionListener(e -> handleDownloadTrainingData());
        view.getTrainXGBoostButton().addActionListener(e -> handleTrainXGBoost());

        // Visual control handlers
        view.getBorderWidthSlider().addChangeListener(this::handleBorderWidthChanged);
        view.getFillOpacitySlider().addChangeListener(this::handleFillOpacityChanged);
        view.getShowNucleiCheckBox().addActionListener(e -> handleVisualControlsChanged());
        view.getShowCellsCheckBox().addActionListener(e -> handleVisualControlsChanged());

        // Class management handlers
        view.getClassComboBox().addActionListener(e -> handleClassSelection());
        view.getAddClassButton().addActionListener(e -> handleAddClass());
        view.setColorPickerAction(e -> handleColorPicker());
    }

    /**
     * Setup class management integration.
     */
    private void setupClassManagers() {
        // Connect the class manager to the combo box
        classManager.setComboBox(view.getClassComboBox());

        // Setup initial default class
        SwingUtilities.invokeLater(() -> {
            if (classManager.getClassNames().isEmpty()) {
                classManager.addClass("Unclassified", new java.awt.Color(255, 255, 0)); // Yellow
            }

            // Pass initial class colors to overlay
            if (overlay != null) {
                overlay.setClassColors(classManager.getClassColors());
            }
        });
    }

    // === FILE OPERATION HANDLERS ===

    private void handleLoadROIs() {
        Container parent = SwingUtilities.getWindowAncestor(view);

        // Execute file operation synchronously
        boolean success = fileHandler.loadROIsFromZip(parent);

        if (success) {
            updateClassCounts();
            view.setStatus("ROIs loaded successfully");
        } else {
            view.setStatus("Failed to load ROIs");
        }
    }

    private void handleClearROIs() {
        fileHandler.clearROIs(SwingUtilities.getWindowAncestor(view));
    }

    private void handleDownloadTrainingData() {
        Container parent = SwingUtilities.getWindowAncestor(view);

        // Access ROIs through overlay (would be injected through controller)
        Map<String, UserROI> allROIs = getAllROIsFromOverlay();

        if (allROIs.isEmpty()) {
            showErrorMessage("No ROIs available. Please load ROIs first.");
            return;
        }

        // Show file chooser on EDT first, then use background thread for processing
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("training_data.json"));
        fileChooser.setDialogTitle("Save Training Data");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files (*.json)", "json"));
        
        int result = fileChooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) {
            updateStatus("Training data export cancelled");
            return;
        }
        File selectedFile = fileChooser.getSelectedFile();
        // Ensure .json extension
        final File outputFile;
        if (!selectedFile.getName().toLowerCase().endsWith(".json")) {
            outputFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".json");
        } else {
            outputFile = selectedFile;
        }

        // Use FeatureExtractor in a separate thread for long-running operation
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("Starting feature extraction...");
                
                // Get the current ImagePlus from the datasetImageViewer
                ij.ImagePlus currentImagePlus = null;
                if (datasetImageViewer != null) {
                    currentImagePlus = datasetImageViewer.getCurrentImagePlus();
                }
                
                // If we don't have an ImagePlus from the viewer, try to get it from ImageJ
                if (currentImagePlus == null) {
                    currentImagePlus = ij.WindowManager.getCurrentImage();
                }
                
                if (currentImagePlus != null) {
                    publish("Using comprehensive feature extraction with image: " + currentImagePlus.getTitle());
                } else {
                    publish("Warning: No ImagePlus available, using basic feature extraction");
                }
                
                featureExtractor.extractAndSaveTrainingData(outputFile, allROIs, currentImagePlus);
                publish("Training data saved successfully to: " + outputFile.getName());

                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    updateStatus(message);
                }
            }

            @Override
            protected void done() {
                try {
                    // Check if the operation was successful (no exception thrown)
                    get(); // This will throw an exception if doInBackground() failed
                    
                    // Enable training button after successful data extraction
                    SwingUtilities.invokeLater(() -> {
                        view.getTrainXGBoostButton().setVisible(true);
                        view.getTrainXGBoostButton().setToolTipText("Train XGBoost model using the downloaded training data");
                        
                        // Force UI refresh to ensure button is visible
                        view.revalidate();
                        view.repaint();
                        
                        updateStatus("Training data saved successfully - XGBoost training now available");
                    });
                    
                } catch (Exception e) {
                    // Log the full exception details for debugging
                    java.util.logging.Logger.getLogger(getClass().getName())
                        .severe("FULL TRAINING DATA EXTRACTION ERROR: " + e.getMessage());
                    e.printStackTrace(); // Print full stack trace to console
                    
                    // If download failed, don't show the training button
                    SwingUtilities.invokeLater(() -> {
                        view.getTrainXGBoostButton().setVisible(false);
                        updateStatus("Training data download failed: " + e.getMessage() +
                                   " (Check console for full error details)");
                    });
                }
            }
        };

        worker.execute();
    }

    private void handleTrainXGBoost() {
        Container parent = SwingUtilities.getWindowAncestor(view);
        fileHandler.trainXGBoostModel(parent);
    }

    // === VISUAL CONTROL HANDLERS ===

    private void handleBorderWidthChanged(ChangeEvent e) {
        JSlider slider = view.getBorderWidthSlider();
        if (!slider.getValueIsAdjusting()) {
            visualState.setBorderWidth(slider.getValue());
            handleVisualControlsChanged();
        }
    }

    private void handleFillOpacityChanged(ChangeEvent e) {
        JSlider slider = view.getFillOpacitySlider();
        if (!slider.getValueIsAdjusting()) {
            visualState.setFillOpacity(slider.getValue());
            handleVisualControlsChanged();
        }
    }

    private void handleVisualControlsChanged() {
        visualState.setShowNuclei(view.getShowNucleiCheckBox().isSelected());
        visualState.setShowCells(view.getShowCellsCheckBox().isSelected());

        // Apply visual controls to overlay
        if (overlay != null) {
            overlay.setVisualControls(
                visualState.getBorderWidth(),
                visualState.getFillOpacity(),
                visualState.isShowNuclei(),
                visualState.isShowCells()
            );
        }

        updateStatus(String.format("Visual controls updated: border=%.1f, opacity=%.2f%%, nuclei=%s, cells=%s",
            visualState.getBorderWidth(),
            visualState.getFillOpacity(),
            visualState.isShowNuclei(),
            visualState.isShowCells()
        ));
    }

    // === CLASS MANAGEMENT HANDLERS ===

    private void handleClassSelection() {
        ClassItem selectedItem = (ClassItem) view.getClassComboBox().getSelectedItem();
        if (selectedItem != null) {
            classManager.setSelectedClass(selectedItem.getName());
            updateStatus("Selected class: " + selectedItem.getName());

            // Notify overlay of class change
            if (overlay != null) {
                overlay.setSelectedClass(selectedItem.getName());
            }
        }
    }
private void handleAddClass() {
    String className = view.getClassNameField().getText().trim();

    if (className.isEmpty() || className.equals("Enter class name")) {
        showErrorMessage("Please enter a valid class name.");
        return;
    }

    if (classManager.containsClass(className)) {
        showErrorMessage("Class already exists.");
        return;
    }

    Color selectedColor = view.getColorPickerButton().getBackground();

    if (classManager.addClass(className, selectedColor)) {
        // Update overlay with new class colors
        if (overlay != null) {
            overlay.setClassColor(className, selectedColor);
            overlay.setSelectedClass(className); // Set the new class as selected in overlay
        }

        // Automatically select the newly created class in the combo box
        SwingUtilities.invokeLater(() -> {
            try {
                // Set the new class as selected in the class manager
                classManager.setSelectedClass(className);
                
                // Find and select the new class in the combo box
                for (int i = 0; i < view.getClassComboBox().getItemCount(); i++) {
                    ClassItem item = view.getClassComboBox().getItemAt(i);
                    if (item != null && className.equals(item.getName())) {
                        view.getClassComboBox().setSelectedIndex(i);
                        break;
                    }
                }
            } catch (Exception e) {
                updateStatus("Error selecting new class: " + e.getMessage());
            }
        });

        // Update UI
        updateClassCounts();
        view.getClassNameField().setText("");
        view.getClassNameField().setForeground(UIManager.getColor("TextField.foreground"));
        
        // Clear the class name field placeholder
        view.getClassNameField().setText("Enter class name");
        view.getClassNameField().setForeground(new Color(128, 128, 128)); // Gray placeholder text
        
        updateStatus("Added and selected new class: " + className);
    } else {
        showErrorMessage("Failed to add class.");
    }
}

    private void handleColorPicker() {
        Color currentColor = view.getColorPickerButton().getBackground();
        Color newColor = JColorChooser.showDialog(view,
            "Choose Class Color",
            currentColor);

        if (newColor != null) {
            view.getColorPickerButton().setBackground(newColor);
            view.getColorPreview().setBackground(newColor);
        }
    }

    // === CALLBACK METHODS ===

    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> view.setStatus(message));
    }

    private void updateProgress(ProgressState state) {
        SwingUtilities.invokeLater(() -> {
            view.updateProgress(state.getDetails(), (int) state.getCurrent(), (int) state.getTotal());
        });
    }

    private void updateProgressPercentage(double percentage) {
        // Could be used for additional progress visualization
        SwingUtilities.invokeLater(() -> {
            String message = String.format("%.0f%% complete", percentage * 100);
            view.getProgressLabel().setText(message);
        });
    }

    private void clearROIsCallback() {
        // Implementation would clear ROIs through overlay
        updateStatus("ROIs cleared");
    }

    // === UTILITY METHODS ===

    private void updateClassCounts() {
        SwingUtilities.invokeLater(() -> {
            updateCountsFromOverlay();
            view.updateClassCounters(classManager.getClassCounts(), classManager.getClassColors());
        });
    }

    /**
     * Update class counts from overlay's persistent classifications.
     * This aggregates counts across all images with persistent classifications.
     */
    private void updateClassCountsFromOverlay() {
        if (overlay == null) {
            return;
        }

        try {
            // Get all persistent classifications from overlay
            Map<String, Map<String, String>> allPersistentClassifications = overlay.getAllPersistentClassifications();

            if (allPersistentClassifications == null || allPersistentClassifications.isEmpty()) {
                // Reset counts if no classifications
                classManager.resetCounts();
                return;
            }

            // Reset all class counts
            classManager.resetCounts();

            // Aggregate class counts from all persistent classifications across all images
            Map<String, Set<String>> uniqueCellIdsPerClass = new java.util.HashMap<>();

            for (Map.Entry<String, Map<String, String>> imageEntry : allPersistentClassifications.entrySet()) {
                String imageName = imageEntry.getKey();
                Map<String, String> imageClassifications = imageEntry.getValue();

                if (imageClassifications != null) {
                    for (Map.Entry<String, String> classificationEntry : imageClassifications.entrySet()) {
                        String roiName = classificationEntry.getKey();
                        String className = classificationEntry.getValue();

                        if (className != null && !className.trim().isEmpty() && !"Unclassified".equals(className)) {
                            // Extract cell ID from ROI name (e.g., "Cell_123" -> "123")
                            String cellId = TargetClassManager.extractCellIdFromROIName(roiName);
                            String uniqueCellKey = imageName + "_" + cellId;

                            uniqueCellIdsPerClass.computeIfAbsent(className, k -> new java.util.HashSet<>()).add(uniqueCellKey);
                        }
                    }
                }
            }

            // Count unique cells per class and ensure classes exist in manager
            for (Map.Entry<String, Set<String>> entry : uniqueCellIdsPerClass.entrySet()) {
                String className = entry.getKey();
                int uniqueCellCount = entry.getValue().size();
                
                // Add class if it doesn't exist (this can happen when loading classifications)
                if (!classManager.containsClass(className)) {
                    // Use a default color for new classes found in classifications
                    Color defaultColor = generateDefaultColorForClass(className);
                    classManager.addClass(className, defaultColor);
                }
                
                classManager.setClassCount(className, uniqueCellCount);
            }

            // Log the updated counts for debugging
            java.util.logging.Logger.getLogger(getClass().getName())
                .info("Updated class counts: " + classManager.getClassCounts());

        } catch (Exception e) {
            java.util.logging.Logger.getLogger(getClass().getName())
                .warning("Error updating class counts from overlay: " + e.getMessage());
        }
    }

    /**
     * Generate a default color for a class that doesn't exist yet.
     */
    private Color generateDefaultColorForClass(String className) {
        // Use hash code to generate consistent color for same class name
        int hash = className.hashCode();
        int r = Math.abs(hash) % 200 + 55; // 55-254 range
        int g = Math.abs(hash >> 8) % 200 + 55;
        int b = Math.abs(hash >> 16) % 200 + 55;
        return new Color(r, g, b);
    }

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(view, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private Map<String, UserROI> getAllROIsFromOverlay() {
        if (overlay == null) {
            return Map.of();
        }

        // Get ROIs from overlay - we need to access the allROIs field via reflection
        // since it's private in the overlay
        try {
            java.lang.reflect.Field allROIsField = overlay.getClass().getDeclaredField("allROIs");
            allROIsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<UserROI> roiList = (java.util.List<UserROI>) allROIsField.get(overlay);

            // Convert to map by image + roi name
            Map<String, UserROI> roiMap = new java.util.HashMap<>();
            for (UserROI roi : roiList) {
                String key = roi.getImageFileName() + ":" + roi.getName();
                roiMap.put(key, roi);
            }
            return roiMap;

        } catch (Exception e) {
            java.util.logging.Logger.getLogger(getClass().getName())
                .warning("Could not access overlay ROIs: " + e.getMessage());
            return Map.of();
        }
    }

    private JLabel createStyledCounter(String text, Color color) {
        Color contrastColor = getContrastColor(color);

        JLabel counter = new JLabel(text);
        counter.setOpaque(true);
        counter.setBackground(color);
        counter.setForeground(contrastColor);
        counter.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        counter.setFont(new Font("Segoe UI", Font.BOLD, 11));

        return counter;
    }

    private Color getContrastColor(Color color) {
        // Calculate luminance
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255.0;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    // === DEPENDENCY INJECTION METHODS ===

    /**
     * Set the overlay component for ROI management.
     */
    public void setOverlay(NewDatasetROIOverlay overlay) {
        this.overlay = overlay;
        fileHandler.setOverlay(overlay);
    }

    /**
     * Set the dataset image viewer for image operations.
     */
    public void setDatasetImageViewer(DatasetImageViewer datasetImageViewer) {
        this.datasetImageViewer = datasetImageViewer;
        fileHandler.setDatasetImageViewer(datasetImageViewer);
    }

    /**
     * Set the main window for modal dialogs.
     */
    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        fileHandler.setMainWindow(mainWindow);
    }

    /**
     * Get the current visual controls state.
     */
    public VisualControlsState getVisualState() {
        return visualState.copy();
    }

    /**
     * Set the visual controls state.
     */
    public void setVisualState(VisualControlsState state) {
        this.visualState = state;
        view.updateVisualControls(state);
    }

    /**
     * Check if the controller has been properly initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Get access to the class manager for external operations.
     */
    public TargetClassManager getClassManager() {
        return classManager;
    }

    /**
     * Update class counts from overlay data. Public method for external access.
     */
    public void updateCountsFromOverlay() {
        updateClassCountsFromOverlay();
    }
}