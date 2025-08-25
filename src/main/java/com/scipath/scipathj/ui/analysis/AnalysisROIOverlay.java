package com.scipath.scipathj.ui.analysis;

import com.scipath.scipathj.infrastructure.config.MainSettings;
import com.scipath.scipathj.infrastructure.roi.ROIRenderingEngine;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.ui.utils.UIConstants;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analysis-specific ROI overlay that uses the shared rendering engine.
 * Provides analysis features like ROI creation, selection, and classification display.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 */
public class AnalysisROIOverlay extends JComponent {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisROIOverlay.class);
    
    // Shared rendering engine
    private final ROIRenderingEngine renderingEngine;
    
    // Analysis-specific manager
    private final AnalysisROIManager roiManager;
    
    // Display state
    private List<UserROI> displayedROIs = new CopyOnWriteArrayList<>();
    private String currentImageFileName;
    
    // Transform state
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private int imageWidth = 0;
    private int imageHeight = 0;
    
    // Interaction state
    private UserROI selectedROI;
    private UserROI hoveredROI;
    
    // Filter state (analysis-specific filters)
    private boolean showClassifiedOnly = false;
    private boolean showValidOnly = false;
    private Predicate<UserROI> customFilter;
    
    // Listeners
    private final List<AnalysisROIOverlayListener> listeners = new CopyOnWriteArrayList<>();
    
    /**
     * Interface for listening to analysis ROI overlay events.
     */
    public interface AnalysisROIOverlayListener {
        void onROISelected(UserROI roi);
        void onROIDeselected();
        void onROIDoubleClicked(UserROI roi);
        void onROIContextMenu(UserROI roi, Point location);
    }
    
    /**
     * Custom color provider for analysis context.
     */
    private static class AnalysisColorProvider implements ROIRenderingEngine.ROIColorProvider {
        private final MainSettings settings;
        private final AnalysisROIManager roiManager;
        private final boolean highlightClassified;
        
        public AnalysisColorProvider(MainSettings settings, AnalysisROIManager roiManager, boolean highlightClassified) {
            this.settings = settings;
            this.roiManager = roiManager;
            this.highlightClassified = highlightClassified;
        }
        
        @Override
        public Color getFillColor(UserROI roi, MainSettings.ROICategory category) {
            if (roi.isIgnored()) {
                return settings.ignoreSettings().getFillColor();
            }
            
            // Highlight classified ROIs in analysis
            if (highlightClassified) {
                String roiKey = roi.getImageFileName() + "_" + roi.getName();
                if (roiManager.getClassificationResult(roiKey) != null) {
                    Color baseColor = settings.getSettingsForCategory(category).getFillColor();
                    return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 
                        Math.min(255, baseColor.getAlpha() + 50)); // Slightly more opaque
                }
            }
            
            return settings.getSettingsForCategory(category).getFillColor();
        }
        
        @Override
        public Color getBorderColor(UserROI roi, MainSettings.ROICategory category) {
            if (roi.isIgnored()) {
                return settings.ignoreSettings().ignoreColor();
            }
            
            // Use classification-based coloring if available
            String roiKey = roi.getImageFileName() + "_" + roi.getName();
            var classificationResult = roiManager.getClassificationResult(roiKey);
            if (classificationResult != null) {
                // Color based on classification confidence
                double confidence = classificationResult.getConfidence();
                if (confidence > 0.8) {
                    return Color.GREEN.darker(); // High confidence - dark green
                } else if (confidence > 0.6) {
                    return Color.ORANGE; // Medium confidence - orange
                } else {
                    return Color.RED; // Low confidence - red
                }
            }
            
            Color roiColor = roi.getDisplayColor();
            if (roiColor != null) {
                return roiColor;
            }
            return settings.getSettingsForCategory(category).borderColor();
        }
        
        @Override
        public float getBorderWidth(UserROI roi, MainSettings.ROICategory category) {
            if (roi.isIgnored()) {
                return settings.ignoreSettings().borderWidth();
            }
            
            // Thicker border for classified ROIs
            String roiKey = roi.getImageFileName() + "_" + roi.getName();
            if (roiManager.getClassificationResult(roiKey) != null) {
                return Math.max(2.0f, settings.getSettingsForCategory(category).borderWidth());
            }
            
            return settings.getSettingsForCategory(category).borderWidth();
        }
        
        @Override
        public boolean shouldRenderROI(UserROI roi) {
            if (roi.isIgnored() && !settings.ignoreSettings().showIgnoredROIs()) {
                return false;
            }
            return true;
        }
    }
    
    public AnalysisROIOverlay(MainSettings settings, AnalysisROIManager roiManager) {
        this.roiManager = roiManager;
        this.renderingEngine = new ROIRenderingEngine(settings);
        
        // Set analysis-specific color provider
        this.renderingEngine.setColorProvider(new AnalysisColorProvider(settings, roiManager, true));
        
        setOpaque(false);
        setupMouseHandlers();
        setupTooltips();
        
        LOGGER.debug("Created AnalysisROIOverlay");
    }
    
    // === PUBLIC API ===
    
    /**
     * Set the ROIs to display.
     */
    public void setDisplayedROIs(List<UserROI> rois, String imageFileName) {
        this.displayedROIs = rois != null ? new CopyOnWriteArrayList<>(rois) : new CopyOnWriteArrayList<>();
        this.currentImageFileName = imageFileName;
        this.selectedROI = null;
        this.hoveredROI = null;
        
        // Apply current filters
        List<UserROI> filteredROIs = applyFilters(this.displayedROIs);
        
        // Render to buffer
        renderingEngine.renderToBuffer(filteredROIs, imageWidth, imageHeight);
        repaint();
        
        LOGGER.debug("Updated analysis overlay with {} ROIs (filtered from {})", 
            filteredROIs.size(), this.displayedROIs.size());
    }
    
    /**
     * Set image transform for coordinate synchronization.
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
        renderingEngine.ensureBufferSize(width, height);
    }
    
    /**
     * Update settings and refresh rendering.
     */
    public void updateSettings(MainSettings newSettings) {
        renderingEngine.updateSettings(newSettings);
        renderingEngine.setColorProvider(new AnalysisColorProvider(newSettings, roiManager, true));
        
        // Re-render with new settings
        List<UserROI> filteredROIs = applyFilters(displayedROIs);
        renderingEngine.renderToBuffer(filteredROIs, imageWidth, imageHeight);
        repaint();
    }
    
    /**
     * Set analysis-specific filters.
     */
    public void setAnalysisFilters(boolean showClassifiedOnly, boolean showValidOnly, Predicate<UserROI> customFilter) {
        this.showClassifiedOnly = showClassifiedOnly;
        this.showValidOnly = showValidOnly;
        this.customFilter = customFilter;
        
        // Re-apply filters and update display
        List<UserROI> filteredROIs = applyFilters(displayedROIs);
        renderingEngine.renderToBuffer(filteredROIs, imageWidth, imageHeight);
        repaint();
        
        LOGGER.debug("Updated analysis filters: classified={}, valid={}, custom={}", 
            showClassifiedOnly, showValidOnly, customFilter != null);
    }
    
    /**
     * Select a specific ROI.
     */
    public void selectROI(UserROI roi) {
        if (selectedROI != roi) {
            selectedROI = roi;
            repaint();
            
            // Notify listeners
            notifyListeners(listener -> {
                if (roi != null) {
                    listener.onROISelected(roi);
                } else {
                    listener.onROIDeselected();
                }
            });
        }
    }
    
    /**
     * Clear selection.
     */
    public void clearSelection() {
        selectROI(null);
    }
    
    /**
     * Get currently selected ROI.
     */
    public UserROI getSelectedROI() {
        return selectedROI;
    }
    
    // === LISTENER MANAGEMENT ===
    
    public void addOverlayListener(AnalysisROIOverlayListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    public void removeOverlayListener(AnalysisROIOverlayListener listener) {
        listeners.remove(listener);
    }
    
    // === RENDERING ===
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (displayedROIs.isEmpty()) {
            return;
        }
        
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            // Use shared rendering engine
            List<UserROI> filteredROIs = applyFilters(displayedROIs);
            renderingEngine.copyFromBuffer(g2d, imageWidth, imageHeight, scaleX, scaleY, offsetX, offsetY);
            
            // Render selection overlay if needed
            if (selectedROI != null) {
                renderSelectionOverlay(g2d, selectedROI);
            }
            
            // Render hover overlay if needed
            if (hoveredROI != null && hoveredROI != selectedROI) {
                renderHoverOverlay(g2d, hoveredROI);
            }
            
        } finally {
            g2d.dispose();
        }
    }
    
    // === MOUSE INTERACTION ===
    
    private void setupMouseHandlers() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                UserROI clickedROI = findROIAtPoint(e.getPoint());
                
                if (SwingUtilities.isLeftMouseButton(e)) {
                    selectROI(clickedROI);
                } else if (SwingUtilities.isRightMouseButton(e) && clickedROI != null) {
                    // Context menu
                    notifyListeners(listener -> listener.onROIContextMenu(clickedROI, e.getPoint()));
                }
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && selectedROI != null) {
                    notifyListeners(listener -> listener.onROIDoubleClicked(selectedROI));
                }
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                UserROI newHoveredROI = findROIAtPoint(e.getPoint());
                if (hoveredROI != newHoveredROI) {
                    hoveredROI = newHoveredROI;
                    setCursor(hoveredROI != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                    repaint();
                }
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    
    private void setupTooltips() {
        // Configure tooltip for immediate appearance
        ToolTipManager.sharedInstance().setInitialDelay(100);
        ToolTipManager.sharedInstance().setDismissDelay(10000);
        
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                UserROI roi = findROIAtPoint(e.getPoint());
                if (roi != null) {
                    String roiKey = roi.getImageFileName() + "_" + roi.getName();
                    String classificationText = roiManager.getClassificationTooltipText(roiKey);
                    
                    String tooltipText = String.format(
                        "<html><b>%s</b><br/>" +
                        "Type: %s<br/>" +
                        "%s<br/>" +
                        "Status: %s</html>",
                        roi.getName(),
                        roi.getType().getDisplayName(),
                        classificationText,
                        roi.isIgnored() ? "Ignored" : "Valid");
                    
                    setToolTipText(tooltipText);
                } else {
                    setToolTipText(null);
                }
            }
        });
    }
    
    // === HELPER METHODS ===
    
    private List<UserROI> applyFilters(List<UserROI> rois) {
        return rois.stream()
            .filter(roi -> {
                // Apply classification filter
                if (showClassifiedOnly) {
                    String roiKey = roi.getImageFileName() + "_" + roi.getName();
                    if (roiManager.getClassificationResult(roiKey) == null) {
                        return false;
                    }
                }
                
                // Apply validation filter
                if (showValidOnly && roi.isIgnored()) {
                    return false;
                }
                
                // Apply custom filter
                if (customFilter != null && !customFilter.test(roi)) {
                    return false;
                }
                
                return true;
            })
            .toList();
    }
    
    private UserROI findROIAtPoint(Point point) {
        List<UserROI> filteredROIs = applyFilters(displayedROIs);
        
        // Check ROIs in reverse order (top to bottom)
        for (int i = filteredROIs.size() - 1; i >= 0; i--) {
            UserROI roi = filteredROIs.get(i);
            if (renderingEngine.isPointInROI(point, roi, scaleX, scaleY, offsetX, offsetY)) {
                return roi;
            }
        }
        return null;
    }
    
    private void renderSelectionOverlay(Graphics2D g2d, UserROI roi) {
        java.awt.Shape shape = renderingEngine.getROIShape(roi);
        if (shape == null) return;
        
        // Apply transform and render selection border
        java.awt.geom.AffineTransform transform = new java.awt.geom.AffineTransform();
        transform.scale(scaleX, scaleY);
        transform.translate(offsetX / scaleX, offsetY / scaleY);
        
        java.awt.Shape transformedShape = transform.createTransformedShape(shape);
        
        // Selection outline
        g2d.setStroke(new BasicStroke(
            UIConstants.SELECTION_STROKE_WIDTH,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
            0,
            UIConstants.DASH_PATTERN,
            0));
        g2d.setColor(UIConstants.ROI_SELECTION_COLOR);
        g2d.draw(transformedShape);
    }
    
    private void renderHoverOverlay(Graphics2D g2d, UserROI roi) {
        java.awt.Shape shape = renderingEngine.getROIShape(roi);
        if (shape == null) return;
        
        // Apply transform and render hover highlight
        java.awt.geom.AffineTransform transform = new java.awt.geom.AffineTransform();
        transform.scale(scaleX, scaleY);
        transform.translate(offsetX / scaleX, offsetY / scaleY);
        
        java.awt.Shape transformedShape = transform.createTransformedShape(shape);
        
        // Hover highlight
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.setColor(Color.WHITE);
        g2d.draw(transformedShape);
    }
    
    private void notifyListeners(java.util.function.Consumer<AnalysisROIOverlayListener> action) {
        listeners.forEach(listener -> {
            try {
                action.accept(listener);
            } catch (Exception e) {
                LOGGER.error("Error notifying analysis ROI overlay listener", e);
            }
        });
    }
}