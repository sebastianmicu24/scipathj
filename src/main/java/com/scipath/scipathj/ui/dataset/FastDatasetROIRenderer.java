package com.scipath.scipathj.ui.dataset;

import com.scipath.scipathj.infrastructure.roi.UserROI;
import ij.gui.Roi;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ultra-fast ROI renderer for dataset creation - mimics Fiji's performance.
 * Simplified architecture with direct polygon rendering and proper coordinate handling.
 * 
 * @author Sebastian Micu
 * @version 3.0.0
 */
public class FastDatasetROIRenderer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FastDatasetROIRenderer.class);
    
    // Visual settings
    private Color defaultColor = Color.YELLOW;
    private Color selectedColor = Color.CYAN;
    private Color hoveredColor = Color.ORANGE;
    private float borderWidth = 1.0f;
    private float fillOpacity = 0.2f;
    private boolean outlinesVisible = true;
    
    // Current state
    private UserROI selectedROI = null;
    private UserROI hoveredROI = null;
    private final List<UserROI> visibleROIs = new CopyOnWriteArrayList<>();
    
    // Performance optimization
    private BufferedImage cachedOverlay = null;
    private int lastImageWidth = 0;
    private int lastImageHeight = 0;
    private boolean overlayDirty = true;
    
    /**
     * Set visual properties.
     */
    public void setVisualProperties(float borderWidth, float fillOpacity, boolean outlinesVisible) {
        if (this.borderWidth != borderWidth || this.fillOpacity != fillOpacity || 
            this.outlinesVisible != outlinesVisible) {
            this.borderWidth = borderWidth;
            this.fillOpacity = fillOpacity;
            this.outlinesVisible = outlinesVisible;
            overlayDirty = true;
        }
    }
    
    /**
     * Set ROIs to display.
     */
    public void setROIs(List<UserROI> rois) {
        visibleROIs.clear();
        if (rois != null) {
            visibleROIs.addAll(rois);
        }
        overlayDirty = true;
        LOGGER.debug("Updated visible ROIs: {} total", visibleROIs.size());
    }
    
    /**
     * Add ROIs progressively for loading.
     */
    public void addROIs(List<UserROI> newROIs) {
        if (newROIs != null && !newROIs.isEmpty()) {
            visibleROIs.addAll(newROIs);
            overlayDirty = true;
        }
    }
    
    /**
     * Set interaction state.
     */
    public void setSelectedROI(UserROI roi) {
        if (selectedROI != roi) {
            selectedROI = roi;
            overlayDirty = true;
        }
    }
    
    public void setHoveredROI(UserROI roi) {
        if (hoveredROI != roi) {
            hoveredROI = roi;
            overlayDirty = true;
        }
    }
    
    /**
     * Fast rendering to graphics context with type filtering.
     */
    public void render(Graphics2D g2d, int imageWidth, int imageHeight,
                      double scaleX, double scaleY, double offsetX, double offsetY) {
        
        // Remove global outlinesVisible check - let type-specific filtering handle visibility
        if (visibleROIs.isEmpty()) {
            return;
        }
        
        long startTime = System.nanoTime();
        
        // Setup for high-speed rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        
        // Apply transform
        AffineTransform originalTransform = g2d.getTransform();
        g2d.translate(offsetX, offsetY);
        g2d.scale(scaleX, scaleY);
        
        try {
            // Render all ROIs in optimal order
            for (UserROI roi : visibleROIs) {
                renderSingleROI(g2d, roi);
            }
            
        } finally {
            g2d.setTransform(originalTransform);
        }
        
        long renderTime = (System.nanoTime() - startTime) / 1_000_000;
        LOGGER.trace("Rendered {} ROIs in {}ms", visibleROIs.size(), renderTime);
    }
    
    /**
     * Fast rendering with type-specific visibility controls.
     */
    public void render(Graphics2D g2d, int imageWidth, int imageHeight,
                      double scaleX, double scaleY, double offsetX, double offsetY,
                      boolean nucleiVisible, boolean cellsVisible) {
        
        if (visibleROIs.isEmpty()) {
            return;
        }
        
        long startTime = System.nanoTime();
        
        // Setup for high-speed rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        
        // Apply transform
        AffineTransform originalTransform = g2d.getTransform();
        g2d.translate(offsetX, offsetY);
        g2d.scale(scaleX, scaleY);
        
        try {
            // Render ROIs based on type visibility
            for (UserROI roi : visibleROIs) {
                if (shouldRenderROI(roi, nucleiVisible, cellsVisible)) {
                    renderSingleROI(g2d, roi);
                }
            }
            
        } finally {
            g2d.setTransform(originalTransform);
        }
        
        long renderTime = (System.nanoTime() - startTime) / 1_000_000;
        LOGGER.trace("Rendered {} ROIs in {}ms with type filtering", visibleROIs.size(), renderTime);
    }
    
    /**
     * Check if ROI should be rendered based on type visibility.
     */
    private boolean shouldRenderROI(UserROI roi, boolean nucleiVisible, boolean cellsVisible) {
        UserROI.ROIType type = roi.getType();
        switch (type) {
            case NUCLEUS:
                return nucleiVisible;
            case CELL:
            case CYTOPLASM:
                return cellsVisible;
            default:
                return nucleiVisible || cellsVisible; // Show other types if either is visible
        }
    }
    
    /**
     * Fast single ROI rendering.
     */
    private void renderSingleROI(Graphics2D g2d, UserROI roi) {
        try {
            // Get ROI geometry
            Roi ijRoi = roi.getImageJRoi();
            if (ijRoi == null) {
                return;
            }
            
            // Get polygon (fastest approach)
            Polygon polygon = ijRoi.getPolygon();
            if (polygon == null || polygon.npoints < 3) {
                return;
            }
            
            // Determine colors
            Color borderColor = getBorderColor(roi);
            Color fillColor = getFillColor(roi, borderColor);
            
            // Fill if opacity > 0
            if (fillOpacity > 0 && fillColor.getAlpha() > 0) {
                g2d.setColor(fillColor);
                g2d.fillPolygon(polygon);
            }
            
            // Draw border
            g2d.setColor(borderColor);
            g2d.setStroke(new BasicStroke(getBorderWidth(roi)));
            g2d.drawPolygon(polygon);
            
        } catch (Exception e) {
            LOGGER.trace("Failed to render ROI '{}': {}", roi.getName(), e.getMessage());
        }
    }
    
    /**
     * Get border color based on ROI state and class assignment.
     */
    private Color getBorderColor(UserROI roi) {
        // Priority 1: Selection state overrides everything
        if (roi == selectedROI) {
            return selectedColor;
        }
        // Priority 2: Hover state
        else if (roi == hoveredROI) {
            return hoveredColor;
        }
        // Priority 3: Use ROI's display color if set (from class assignment)
        else if (roi.getDisplayColor() != null) {
            return roi.getDisplayColor();
        }
        // Priority 4: Fall back to default color
        else {
            return defaultColor;
        }
    }
    
    /**
     * Get fill color with opacity.
     */
    private Color getFillColor(UserROI roi, Color borderColor) {
        int alpha = Math.round(255 * fillOpacity);
        return new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), alpha);
    }
    
    /**
     * Get border width based on ROI state.
     */
    private float getBorderWidth(UserROI roi) {
        if (roi == selectedROI) {
            return Math.max(borderWidth, 2.0f);
        } else if (roi == hoveredROI) {
            return Math.max(borderWidth, 1.5f);
        } else {
            return borderWidth;
        }
    }
    /**
     * Hit testing for click detection.
     */
    public UserROI findROIAtPoint(Point point, double scaleX, double scaleY, double offsetX, double offsetY) {
        // Transform point to image coordinates
        double imageX = (point.x - offsetX) / scaleX;
        double imageY = (point.y - offsetY) / scaleY;
        
        // Check ROIs in reverse order (top to bottom)
        for (int i = visibleROIs.size() - 1; i >= 0; i--) {
            UserROI roi = visibleROIs.get(i);
            if (isPointInROI(imageX, imageY, roi)) {
                return roi;
            }
        }
        
        return null;
    }
    
    /**
     * Hit testing for click detection with type-specific visibility.
     */
    public UserROI findROIAtPoint(Point point, double scaleX, double scaleY, double offsetX, double offsetY,
                                  boolean nucleiVisible, boolean cellsVisible) {
        // Transform point to image coordinates
        double imageX = (point.x - offsetX) / scaleX;
        double imageY = (point.y - offsetY) / scaleY;
        
        // Check ROIs in reverse order (top to bottom), respecting type visibility
        for (int i = visibleROIs.size() - 1; i >= 0; i--) {
            UserROI roi = visibleROIs.get(i);
            if (shouldRenderROI(roi, nucleiVisible, cellsVisible) && isPointInROI(imageX, imageY, roi)) {
                return roi;
            }
        }
        
        return null;
    }
    
    /**
     * Fast point-in-polygon test.
     */
    private boolean isPointInROI(double x, double y, UserROI roi) {
        try {
            Roi ijRoi = roi.getImageJRoi();
            if (ijRoi == null) {
                return false;
            }
            
            // Use ImageJ's built-in contains method (fastest)
            return ijRoi.contains((int) Math.round(x), (int) Math.round(y));
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Clear all ROIs.
     */
    public void clear() {
        visibleROIs.clear();
        selectedROI = null;
        hoveredROI = null;
        overlayDirty = true;
    }
    
    /**
     * Get current ROI count.
     */
    public int getROICount() {
        return visibleROIs.size();
    }
    
    /**
     * Set default colors.
     */
    public void setColors(Color defaultColor, Color selectedColor, Color hoveredColor) {
        this.defaultColor = defaultColor;
        this.selectedColor = selectedColor;
        this.hoveredColor = hoveredColor;
        overlayDirty = true;
    }
}