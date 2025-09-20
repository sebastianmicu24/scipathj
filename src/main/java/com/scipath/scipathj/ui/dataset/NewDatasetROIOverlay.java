package com.scipath.scipathj.ui.dataset;

import com.scipath.scipathj.infrastructure.roi.CellROI;
import com.scipath.scipathj.infrastructure.roi.NucleusROI;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Streamlined Dataset ROI Overlay with fast rendering and proper integration.
 * Uses FastDatasetROIRenderer for performance and ProgressiveROILoader for loading.
 * 
 * @author Sebastian Micu
 * @version 3.0.0
 */
public class NewDatasetROIOverlay extends JComponent implements ProgressiveROILoader.ProgressListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NewDatasetROIOverlay.class);
    
    // Core components
    private final FastDatasetROIRenderer renderer;
    private final ProgressiveROILoader loader;
    
    // ROI storage and management
    private final List<UserROI> allROIs = new CopyOnWriteArrayList<>();
    private final List<String> classNames = new ArrayList<>();
    private String selectedClassName = "Unclassified";

    // Persistent storage for ROI classifications across image switches
    private final Map<String, Map<String, String>> persistentClassifications = new java.util.LinkedHashMap<>();
    
    // Class color mapping - will be provided by controls panel
    private java.util.Map<String, Color> classColors = new java.util.HashMap<>();
    
    // Transform state for coordinate mapping
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private int imageWidth = 0;
    private int imageHeight = 0;
    private String currentImageFileName = null;
    
    // Visual controls
    private float borderWidth = 2.0f;
    private float fillOpacity = 0.2f;
    private boolean nucleiVisible = true;
    private boolean cellsVisible = true; // Allow cell ROIs to be visible but cytoplasm will be filtered separately
    
    // Interaction listeners
    private final List<InteractionListener> interactionListeners = new ArrayList<>();
    
    /**
     * Interface for interaction events.
     */
    public interface InteractionListener {
        void onROIClicked(UserROI roi, String assignedClass);
        void onROIHovered(UserROI roi);
        void onClassAssigned(UserROI roi, String className);
        void onProgressUpdate(int loaded, int total);
    }
    
    public NewDatasetROIOverlay() {
        this.renderer = new FastDatasetROIRenderer();
        this.loader = new ProgressiveROILoader();
        
        // Setup component
        setOpaque(false);
        setFocusable(true);
        
        // Setup colors
        renderer.setColors(Color.YELLOW, Color.CYAN, Color.ORANGE);
        
        // Register for loader events
        loader.addProgressListener(this);
        
        // Setup event handlers
        setupEventHandlers();
        
        LOGGER.debug("Created streamlined DatasetROIOverlay");
    }
    
    // === PUBLIC API ===
    
    /**
     * Load ROIs progressively from ZIP file.
     * Preserves classifications across image switches.
     */
    public void loadROIsFromZip(java.io.File zipFile, String imageFileName) {
        // Save current image's classifications before switching
        if (currentImageFileName != null && !allROIs.isEmpty()) {
            Map<String, String> currentClasses = new java.util.HashMap<>();
            for (UserROI roi : allROIs) {
                String assignedClass = roi.getAssignedClass();
                if (assignedClass != null && !assignedClass.isEmpty()) {
                    currentClasses.put(roi.getName(), assignedClass);
                }
            }
            if (!currentClasses.isEmpty()) {
                persistentClassifications.put(currentImageFileName, currentClasses);
                LOGGER.debug("Saved {} classifications for image: {}", currentClasses.size(), currentImageFileName);
            }
        }

        this.currentImageFileName = imageFileName;

        // Clear current ROIs but keep persistent classifications
        allROIs.clear();
        renderer.clear();
        repaint();

        loader.loadROIsProgressively(zipFile, imageFileName);
        LOGGER.info("Started progressive loading for image: {}", imageFileName);
    }
    
    /**
     * Set image transform for coordinate mapping.
     */
    public void setImageTransform(double scaleX, double scaleY, double offsetX, double offsetY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        repaint();
    }
    
    /**
     * Set image dimensions.
     */
    public void setImageDimensions(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
    }
    
    /**
     * Set visual controls with separate nuclei and cell visibility.
     */
    public void setVisualControls(float borderWidth, float fillOpacity, boolean nucleiVisible, boolean cellsVisible) {
        this.borderWidth = borderWidth;
        this.fillOpacity = fillOpacity;
        this.nucleiVisible = nucleiVisible;

        // ALLOW CELL VISIBILITY: Let cellsVisible param control CELL ROIs, cytoplasm handled separately
        this.cellsVisible = cellsVisible; // Restore original behavior for cells

        // Only update border width and fill opacity, not the global visibility
        // The type-specific visibility will be handled in the render method
        renderer.setVisualProperties(borderWidth, fillOpacity, true); // Always true, let type filtering handle visibility
        repaint();

        LOGGER.debug("Updated visual controls: border={}, opacity={}, nuclei={}, cells={}",
                    borderWidth, fillOpacity, nucleiVisible, this.cellsVisible);
    }
    
    /**
     * Legacy method for backward compatibility.
     */
    public void setVisualControls(float borderWidth, float fillOpacity, boolean outlinesVisible) {
        setVisualControls(borderWidth, fillOpacity, outlinesVisible, outlinesVisible);
    }
    
    /**
     * Set selected class for assignment.
     */
    public void setSelectedClass(String className) {
        this.selectedClassName = className != null ? className : "Unclassified";
    }
    
    /**
     * Add interaction listener.
     */
    public void addInteractionListener(InteractionListener listener) {
        if (listener != null) {
            interactionListeners.add(listener);
        }
    }
    
    /**
     * Remove interaction listener.
     */
    public void removeInteractionListener(InteractionListener listener) {
        interactionListeners.remove(listener);
    }
    
    /**
     * Get current ROI count.
     */
    public int getROICount() {
        return allROIs.size();
    }

    /**
     * Get classification counts for ROIs.
     * Counts CELL type ROIs: assigned cells by their class, unassigned cells as "Unclassified".
     * Avoids double-counting (since each cell is linked to a nucleus).
     */
    public java.util.Map<String, Integer> getClassificationCounts() {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (UserROI roi : allROIs) {
            if (roi.getType() == UserROI.ROIType.CELL) {
                String className = roi.getAssignedClass();
                if (className == null || className.isEmpty()) {
                    // Count unassigned cells as "Unclassified"
                    counts.put("Unclassified", counts.getOrDefault("Unclassified", 0) + 1);
                } else {
                    // Count assigned cells by their class
                    counts.put(className, counts.getOrDefault(className, 0) + 1);
                }
            }
        }
        return counts;
    }
    
    /**
     * Clear all ROIs.
     */
    public void clear() {
        allROIs.clear();
        renderer.clear();
        repaint();
    }
    
    // === PROGRESSIVE LOADER EVENTS ===
    
    @Override
    public void onBatchLoaded(List<UserROI> batch, int totalLoaded, int totalExpected) {
        // Apply previously saved classifications to new ROIs
        Map<String, String> savedClasses = persistentClassifications.get(currentImageFileName);
        if (savedClasses != null && !savedClasses.isEmpty()) {
            for (UserROI roi : batch) {
                String savedClass = savedClasses.get(roi.getName());
                if (savedClass != null && !savedClass.isEmpty()) {
                    roi.setAssignedClass(savedClass);
                    Color classColor = classColors.get(savedClass);
                    if (classColor != null) {
                        roi.setDisplayColor(classColor);
                    }
                    LOGGER.debug("Restored classification '{}' for ROI '{}'", savedClass, roi.getName());
                }
            }
        }

        // Add new ROIs to storage
        allROIs.addAll(batch);

        // Update renderer with new ROIs
        renderer.addROIs(batch);

        // Trigger repaint for immediate display
        repaint();

        // Notify listeners
        notifyProgressUpdate(totalLoaded, totalExpected);

        LOGGER.debug("Batch loaded: {} ROIs, total: {} (with {} saved classifications restored)",
                    batch.size(), totalLoaded, savedClasses != null ? savedClasses.size() : 0);
    }
    
    @Override
    public void onLoadingComplete(int totalLoaded) {
        LOGGER.info("Loading complete: {} ROIs loaded for image '{}'", totalLoaded, currentImageFileName);
        notifyProgressUpdate(totalLoaded, totalLoaded);
    }
    
    @Override
    public void onLoadingFailed(String error) {
        LOGGER.error("Loading failed: {}", error);
    }
    
    // === SWING COMPONENT OVERRIDES ===
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (allROIs.isEmpty()) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            // Use fast renderer with type-specific visibility
            renderer.render(g2d, imageWidth, imageHeight, scaleX, scaleY, offsetX, offsetY, nucleiVisible, cellsVisible);
        } finally {
            g2d.dispose();
        }
    }
    
    // === PRIVATE METHODS ===
    
    private void setupEventHandlers() {
        // Mouse handling for interaction
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                UserROI hoveredROI = renderer.findROIAtPoint(e.getPoint(), scaleX, scaleY, offsetX, offsetY, nucleiVisible, cellsVisible);
                renderer.setHoveredROI(hoveredROI);
                setCursor(hoveredROI != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                             : Cursor.getDefaultCursor());
                
                if (hoveredROI != null) {
                    notifyROIHovered(hoveredROI);
                }
                
                repaint();
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                UserROI clickedROI = renderer.findROIAtPoint(e.getPoint(), scaleX, scaleY, offsetX, offsetY, nucleiVisible, cellsVisible);
                if (clickedROI != null) {
                    // Set as selected for visual feedback
                    renderer.setSelectedROI(clickedROI);
                    
                    // Assign selected class
                    String assignedClass = assignClassToROI(clickedROI, selectedClassName);
                    
                    // Notify listeners
                    notifyROIClicked(clickedROI, assignedClass);
                    notifyClassAssigned(clickedROI, assignedClass);
                    
                    repaint();
                    
                    LOGGER.debug("Clicked ROI '{}', assigned class '{}'", clickedROI.getName(), assignedClass);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                renderer.setHoveredROI(null);
                setCursor(Cursor.getDefaultCursor());
                repaint();
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        
        // Keyboard handling for outline toggle
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_E) {
                    // Toggle outlines (toggle both nuclei and cells)
                    boolean newState = !(nucleiVisible || cellsVisible);
                    setVisualControls(borderWidth, fillOpacity, newState, newState);
                    LOGGER.debug("Toggled outlines via E key: nuclei={}, cells={}", nucleiVisible, cellsVisible);
                }
            }
        });
    }
    
    private String assignClassToROI(UserROI roi, String className) {
        // Assign the class to the ROI and its linked ROIs
        assignClassWithLinking(roi, className);

        // Save to persistent storage
        if (currentImageFileName != null) {
            persistentClassifications.computeIfAbsent(currentImageFileName, k -> new java.util.HashMap<>());
            persistentClassifications.get(currentImageFileName).put(roi.getName(), className);

            // Also save linked ROI if present
            UserROI linkedROI = findLinkedROI(roi);
            if (linkedROI != null) {
                persistentClassifications.get(currentImageFileName).put(linkedROI.getName(), className);
            }
        }

        // Force a repaint to show the color change
        repaint();

        return className;
    }

    /**
     * Assign class to ROI with bidirectional linking between nucleus and cell.
     */
    private void assignClassWithLinking(UserROI roi, String className) {
        // Always assign to the clicked ROI
        roi.setAssignedClass(className);
        Color classColor = classColors.get(className);
        if (classColor != null) {
            roi.setDisplayColor(classColor);
        }

        // Handle bidirectional linking by finding linked ROI
        UserROI linkedROI = findLinkedROI(roi);
        if (linkedROI != null) {
            linkedROI.setAssignedClass(className);
            if (classColor != null) {
                linkedROI.setDisplayColor(classColor);
            }
            LOGGER.debug("Linked '{}' -> '{}' with class '{}'",
                        roi.getName(), linkedROI.getName(), className);
        }
    }

    /**
     * Find the linked ROI for bidirectional assignment.
     * Links nucleus to cell and vice versa by matching numbers in names.
     */
    private UserROI findLinkedROI(UserROI roi) {
        String roiName = roi.getName();
        if (roiName == null || !roiName.contains("_")) {
            LOGGER.debug("ROI '{}' has no '_' for linking", roiName);
            return null;
        }

        // Extract number from name (e.g., "Nucleus_5" -> 5, "Cell_12" -> 12)
        String[] parts = roiName.split("_");
        if (parts.length < 2) {
            LOGGER.debug("ROI '{}' split gives {} parts, expected >=2", roiName, parts.length);
            return null;
        }

        String numberPart = parts[parts.length - 1]; // Take the last part after underscore
        String targetName;
        UserROI.ROIType targetType;

        if (roi.getType() == UserROI.ROIType.NUCLEUS) {
            // Find cell with matching number
            targetName = "Cell_" + numberPart;
            targetType = UserROI.ROIType.CELL;
        }
        else if (roi.getType() == UserROI.ROIType.CELL) {
            // Find nucleus with matching number
            targetName = "Nucleus_" + numberPart;
            targetType = UserROI.ROIType.NUCLEUS;
        }
        else {
            LOGGER.debug("ROI '{}' type {} not linkable", roiName, roi.getType());
            return null;
        }

        LOGGER.debug("Looking for linked ROI: '{}' of type {}", targetName, targetType);

        // Search through all ROIs for the matching name and type
        for (UserROI candidate : allROIs) {
            if (candidate.getType() == targetType &&
                targetName.equals(candidate.getName())) {
                LOGGER.debug("Found linked ROI: '{}' for '{}'", candidate.getName(), roiName);
                return candidate;
            }
        }

        LOGGER.debug("No linked ROI found for '{}', searched for '{}'", roiName, targetName);
        return null;
    }
    
    /**
     * Set class colors for assignment.
     */
    public void setClassColors(java.util.Map<String, Color> classColors) {
        this.classColors.clear();
        this.classColors.putAll(classColors);
    }
    
    /**
     * Add or update a class color.
     */
    public void setClassColor(String className, Color color) {
        classColors.put(className, color);
    }

    /**
     * Get all persistent classifications across all images.
     * This is used for multi-image training data export.
     */
    public Map<String, Map<String, String>> getAllPersistentClassifications() {
        return new java.util.LinkedHashMap<>(persistentClassifications);
    }

    /**
     * Get persistent classifications for the current image.
     */
    public Map<String, String> getCurrentImageClassifications() {
        return currentImageFileName != null ? persistentClassifications.get(currentImageFileName) : null;
    }
    
    private void notifyROIClicked(UserROI roi, String assignedClass) {
        for (InteractionListener listener : interactionListeners) {
            try {
                listener.onROIClicked(roi, assignedClass);
            } catch (Exception e) {
                LOGGER.error("Error notifying ROI clicked", e);
            }
        }
    }
    
    private void notifyROIHovered(UserROI roi) {
        for (InteractionListener listener : interactionListeners) {
            try {
                listener.onROIHovered(roi);
            } catch (Exception e) {
                LOGGER.error("Error notifying ROI hovered", e);
            }
        }
    }
    
    private void notifyClassAssigned(UserROI roi, String className) {
        for (InteractionListener listener : interactionListeners) {
            try {
                listener.onClassAssigned(roi, className);
            } catch (Exception e) {
                LOGGER.error("Error notifying class assigned", e);
            }
        }
    }
    
    private void notifyProgressUpdate(int loaded, int total) {
        for (InteractionListener listener : interactionListeners) {
            try {
                listener.onProgressUpdate(loaded, total);
            } catch (Exception e) {
                LOGGER.error("Error notifying progress update", e);
            }
        }
    }
    
    /**
     * Cleanup resources.
     */
    public void dispose() {
        loader.removeProgressListener(this);
        loader.cancelLoading();
        allROIs.clear();
        renderer.clear();
        interactionListeners.clear();
        LOGGER.debug("Disposed NewDatasetROIOverlay");
    }
}