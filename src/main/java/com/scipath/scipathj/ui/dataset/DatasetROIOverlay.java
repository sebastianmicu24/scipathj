package com.scipath.scipathj.ui.dataset;

import com.scipath.scipathj.infrastructure.config.MainSettings;
import com.scipath.scipathj.infrastructure.roi.ROIRenderingEngine;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced Dataset ROI Overlay with ROIRenderingEngine integration.
 * Provides interactive class assignment, hover effects, and visual controls
 * inspired by the HTML Cell Classifier.
 * 
 * Key Features:
 * - Uses shared ROIRenderingEngine for optimized rendering
 * - Interactive click-to-assign class functionality
 * - Hover effects with visual feedback
 * - Keyboard shortcuts (E key for outline toggle)
 * - Custom color provider for dataset-specific appearance
 * - Real-time visual control updates
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 */
public class DatasetROIOverlay extends JComponent implements DatasetROIManager.DatasetInteractionListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetROIOverlay.class);
    
    // Core components
    private final ROIRenderingEngine renderingEngine;
    private final DatasetROIManager datasetManager;
    private final DatasetColorProvider colorProvider;
    
    // ROI display data
    private final List<UserROI> displayedROIs = new CopyOnWriteArrayList<>();
    private String currentImageFileName;
    
    // Transform state for coordinate synchronization
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private int imageWidth = 0;
    private int imageHeight = 0;
    
    /**
     * Custom color provider for dataset-specific ROI appearance.
     */
    private class DatasetColorProvider implements ROIRenderingEngine.ROIColorProvider {
        
        @Override
        public Color getFillColor(UserROI roi, MainSettings.ROICategory category) {
            if (!datasetManager.areOutlinesVisible()) {
                return new Color(0, 0, 0, 0); // Transparent when outlines hidden
            }
            
            // Get class-based color with opacity
            String roiKey = datasetManager.generateROIKey(roi.getImageFileName(), roi.getName());
            String assignedClass = datasetManager.getClassAssignment(roiKey);
            Color baseColor = datasetManager.getClassColor(assignedClass);
            
            // Apply fill opacity
            float opacity = datasetManager.getFillOpacity();
            return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 
                           Math.round(255 * opacity));
        }
        
        @Override
        public Color getBorderColor(UserROI roi, MainSettings.ROICategory category) {
            if (!datasetManager.areOutlinesVisible()) {
                return new Color(0, 0, 0, 0); // Transparent when outlines hidden
            }
            
            // Get class-based color
            String roiKey = datasetManager.generateROIKey(roi.getImageFileName(), roi.getName());
            String assignedClass = datasetManager.getClassAssignment(roiKey);
            Color baseColor = datasetManager.getClassColor(assignedClass);
            
            // Special highlighting for selected/hovered ROIs
            if (roi.equals(datasetManager.getSelectedROI())) {
                return baseColor.brighter().brighter(); // Extra bright for selected
            } else if (roi.equals(datasetManager.getHoveredROI())) {
                return baseColor.brighter(); // Bright for hovered
            }
            
            return baseColor;
        }
        
        @Override
        public float getBorderWidth(UserROI roi, MainSettings.ROICategory category) {
            if (!datasetManager.areOutlinesVisible()) {
                return 0.0f;
            }
            
            float baseWidth = datasetManager.getBorderWidth();
            
            // Thicker border for selected/hovered ROIs
            if (roi.equals(datasetManager.getSelectedROI())) {
                return Math.max(baseWidth, 3.0f);
            } else if (roi.equals(datasetManager.getHoveredROI())) {
                return Math.max(baseWidth, 2.5f);
            }
            
            return baseWidth;
        }
        
        @Override
        public boolean shouldRenderROI(UserROI roi) {
            // Only render nucleus and cell ROIs in dataset context
            UserROI.ROIType type = roi.getType();
            return type == UserROI.ROIType.NUCLEUS || type == UserROI.ROIType.CELL;
        }
    }
    
    public DatasetROIOverlay(DatasetROIManager datasetManager, MainSettings settings) {
        this.datasetManager = datasetManager;
        this.colorProvider = new DatasetColorProvider();
        this.renderingEngine = new ROIRenderingEngine(settings);
        
        // Set custom color provider for dataset-specific rendering
        renderingEngine.setColorProvider(colorProvider);
        
        // Setup component
        setOpaque(false);
        setFocusable(true); // For keyboard shortcuts
        
        // Setup event handlers
        setupMouseHandlers();
        setupKeyboardHandlers();
        
        // Register as listener for interaction events
        datasetManager.addInteractionListener(this);
        
        LOGGER.debug("Created enhanced DatasetROIOverlay with ROIRenderingEngine");
    }
    
    /**
     /**
      * Set the ROIs to display.
      * Orders ROIs so cells are on top (rendered last, selected first).
      */
     public void setRois(List<UserROI> rois) {
         displayedROIs.clear();
         
         if (rois != null) {
             // Filter and order ROIs: nuclei first (bottom), then cells (top)
             rois.stream()
                 .filter(colorProvider::shouldRenderROI)
                 .sorted((roi1, roi2) -> {
                     // Order: NUCLEUS first (bottom layer), CELL last (top layer for hover priority)
                     UserROI.ROIType type1 = roi1.getType();
                     UserROI.ROIType type2 = roi2.getType();
                     
                     if (type1 == UserROI.ROIType.NUCLEUS && type2 == UserROI.ROIType.CELL) {
                         return -1; // nucleus comes first (bottom)
                     } else if (type1 == UserROI.ROIType.CELL && type2 == UserROI.ROIType.NUCLEUS) {
                         return 1; // cell comes last (top)
                     } else {
                         return 0; // same type or other types
                     }
                 })
                 .forEach(displayedROIs::add);
         }
         
         // Update rendering engine
         renderingEngine.clearShapeCache();
         renderingEngine.invalidateBuffer();
         repaint();
         
         LOGGER.debug("Updated dataset overlay with {} filtered ROIs (from {} total)",
                    displayedROIs.size(), rois != null ? rois.size() : 0);
     }
    /**
     * Set image transform for coordinate synchronization.
     */
    public void setImageTransform(double scaleX, double scaleY, double offsetX, double offsetY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        renderingEngine.invalidateBuffer();
        repaint();
    }
    
    /**
     * Set image dimensions for buffer sizing.
     */
    public void setImageDimensions(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
        renderingEngine.ensureBufferSize(width, height);
    }
    
    /**
     * Set current image filename for ROI key generation.
     */
    public void setCurrentImageFileName(String imageFileName) {
        this.currentImageFileName = imageFileName;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (displayedROIs.isEmpty()) {
            return;
        }
        
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            // Use ROIRenderingEngine for optimized rendering
            renderingEngine.renderToBuffer(displayedROIs, imageWidth, imageHeight);
            renderingEngine.copyFromBuffer(g2d, imageWidth, imageHeight, scaleX, scaleY, offsetX, offsetY);
        } finally {
            g2d.dispose();
        }
    }
    
    /**
     * Setup mouse event handlers for interaction.
     */
    private void setupMouseHandlers() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                UserROI hoveredROI = findROIAtPoint(e.getPoint());
                datasetManager.setHoveredROI(hoveredROI);
                setCursor(hoveredROI != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) 
                                             : Cursor.getDefaultCursor());
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                UserROI clickedROI = findROIAtPoint(e.getPoint());
                if (clickedROI != null) {
                    // Select the ROI
                    datasetManager.selectROI(clickedROI);
                    
                    // Assign current selected class to the clicked ROI
                    String assignedClass = datasetManager.assignClassToROI(clickedROI, null);
                    LOGGER.debug("Assigned class '{}' to ROI '{}' via click", assignedClass, clickedROI.getName());
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                datasetManager.setHoveredROI(null);
                setCursor(Cursor.getDefaultCursor());
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    
    /**
     * Setup keyboard event handlers (E key for outline toggle).
     */
    private void setupKeyboardHandlers() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_E) {
                    datasetManager.toggleOutlines();
                    LOGGER.debug("Toggled outlines via E key");
                }
            }
        });
    }
    
    /**
     * Find ROI at given point using ROIRenderingEngine hit testing.
     */
    private UserROI findROIAtPoint(Point point) {
        // Check ROIs in reverse order (top to bottom)
        for (int i = displayedROIs.size() - 1; i >= 0; i--) {
            UserROI roi = displayedROIs.get(i);
            if (renderingEngine.isPointInROI(point, roi, scaleX, scaleY, offsetX, offsetY)) {
                return roi;
            }
        }
        return null;
    }
    
    // === DatasetInteractionListener Implementation ===
    
    @Override
    public void onROISelected(UserROI roi) {
        repaint(); // Update visual feedback
    }
    
    @Override
    public void onROIHovered(UserROI roi) {
        repaint(); // Update hover effects
    }
    
    @Override
    public void onROIUnhovered(UserROI roi) {
        repaint(); // Remove hover effects
    }
    
    @Override
    public void onClassCreated(String className, Color color) {
        renderingEngine.invalidateBuffer();
        repaint();
    }
    
    @Override
    public void onClassRemoved(String className) {
        renderingEngine.invalidateBuffer();
        repaint();
    }
    
    @Override
    public void onSelectedClassChanged(String className) {
        // Visual feedback could be added here if needed
    }
    
    @Override
    public void onVisualSettingsChanged(float opacity, float borderWidth, boolean outlinesVisible) {
        renderingEngine.invalidateBuffer();
        repaint();
        LOGGER.debug("Visual settings updated: opacity={}, borderWidth={}, outlines={}", 
                   opacity, borderWidth, outlinesVisible);
    }
    
    /**
     * Clear all displayed ROIs and reset state.
     */
    public void clear() {
        displayedROIs.clear();
        renderingEngine.clearShapeCache();
        renderingEngine.invalidateBuffer();
        repaint();
    }
    
    /**
     * Update settings for the rendering engine.
     */
    public void updateSettings(MainSettings newSettings) {
        renderingEngine.updateSettings(newSettings);
        repaint();
    }
    
    /**
     * Cleanup resources when overlay is no longer needed.
     */
    public void dispose() {
        datasetManager.removeInteractionListener(this);
        displayedROIs.clear();
        renderingEngine.clearShapeCache();
        LOGGER.debug("Disposed DatasetROIOverlay");
    }
}