package com.scipath.scipathj.infrastructure.roi;

import com.scipath.scipathj.infrastructure.config.MainSettings;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared ROI rendering engine that provides optimized rendering for all parts of the application.
 * This replaces the duplicated rendering logic in ROIOverlay and DatasetROIOverlay.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 */
public class ROIRenderingEngine {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ROIRenderingEngine.class);
    
    // Performance constants
    private static final int BUFFER_MARGIN = 100;
    
    // Shape cache for performance optimization
    private final Map<String, java.awt.Shape> shapeCache = new ConcurrentHashMap<>();
    
    // Rendering buffer
    private BufferedImage masterBuffer;
    private int bufferWidth;
    private int bufferHeight;
    private boolean bufferValid = false;
    
    // Current rendering settings
    private MainSettings settings;
    private ROIColorProvider colorProvider;
    
    /**
     * Interface for providing custom colors for ROIs in different contexts.
     */
    public interface ROIColorProvider {
        Color getFillColor(UserROI roi, MainSettings.ROICategory category);
        Color getBorderColor(UserROI roi, MainSettings.ROICategory category);
        float getBorderWidth(UserROI roi, MainSettings.ROICategory category);
        boolean shouldRenderROI(UserROI roi);
    }
    
    /**
     * Default color provider that uses MainSettings appearance.
     */
    public static class DefaultColorProvider implements ROIColorProvider {
        private final MainSettings settings;
        
        public DefaultColorProvider(MainSettings settings) {
            this.settings = settings;
        }
        
        @Override
        public Color getFillColor(UserROI roi, MainSettings.ROICategory category) {
            if (roi.isIgnored()) {
                return settings.ignoreSettings().getFillColor();
            }
            return settings.getSettingsForCategory(category).getFillColor();
        }
        
        @Override
        public Color getBorderColor(UserROI roi, MainSettings.ROICategory category) {
            if (roi.isIgnored()) {
                return settings.ignoreSettings().ignoreColor();
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
            return settings.getSettingsForCategory(category).borderWidth();
        }
        
        @Override
        public boolean shouldRenderROI(UserROI roi) {
            if (roi.isIgnored()) {
                return settings.ignoreSettings().showIgnoredROIs();
            }
            return true;
        }
    }
    
    public ROIRenderingEngine(MainSettings settings) {
        this.settings = settings;
        this.colorProvider = new DefaultColorProvider(settings);
        LOGGER.debug("Created ROI rendering engine");
    }
    
    /**
     * Set custom color provider for different rendering contexts.
     */
    public void setColorProvider(ROIColorProvider colorProvider) {
        this.colorProvider = colorProvider != null ? colorProvider : new DefaultColorProvider(settings);
        invalidateBuffer();
    }
    
    /**
     * Update rendering settings.
     */
    public void updateSettings(MainSettings newSettings) {
        this.settings = newSettings;
        if (colorProvider instanceof DefaultColorProvider) {
            this.colorProvider = new DefaultColorProvider(newSettings);
        }
        invalidateBuffer();
    }
    
    /**
     * Ensure buffer is large enough for the specified dimensions.
     */
    public void ensureBufferSize(int imageWidth, int imageHeight) {
        int requiredWidth = imageWidth + 2 * BUFFER_MARGIN;
        int requiredHeight = imageHeight + 2 * BUFFER_MARGIN;
        
        if (masterBuffer == null || bufferWidth < requiredWidth || bufferHeight < requiredHeight) {
            bufferWidth = Math.max(requiredWidth, bufferWidth);
            bufferHeight = Math.max(requiredHeight, bufferHeight);
            masterBuffer = new BufferedImage(bufferWidth, bufferHeight, BufferedImage.TYPE_INT_ARGB);
            invalidateBuffer();
            LOGGER.debug("Resized rendering buffer to {}x{}", bufferWidth, bufferHeight);
        }
    }
    
    /**
     * Render ROIs to the master buffer at native resolution.
     */
    public void renderToBuffer(List<UserROI> rois, int imageWidth, int imageHeight) {
        ensureBufferSize(imageWidth, imageHeight);
        
        if (masterBuffer == null || rois.isEmpty()) {
            bufferValid = false;
            return;
        }
        
        Graphics2D g2d = masterBuffer.createGraphics();
        try {
            // Clear buffer
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, bufferWidth, bufferHeight);
            g2d.setComposite(AlphaComposite.SrcOver);
            
            // Optimize for speed
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            
            // Render all ROIs
            for (UserROI roi : rois) {
                if (colorProvider.shouldRenderROI(roi)) {
                    renderROIToBuffer(g2d, roi);
                }
            }
            
            bufferValid = true;
            LOGGER.debug("Rendered {} ROIs to buffer", rois.size());
            
        } finally {
            g2d.dispose();
        }
    }
    
    /**
     * Copy from master buffer to target graphics with specified transform.
     */
    public void copyFromBuffer(Graphics2D target, int imageWidth, int imageHeight, 
                              double scaleX, double scaleY, double offsetX, double offsetY) {
        if (!bufferValid || masterBuffer == null) {
            LOGGER.debug("Buffer not valid, cannot copy");
            return;
        }
        
        try {
            int srcX = BUFFER_MARGIN;
            int srcY = BUFFER_MARGIN;
            int srcWidth = Math.min(imageWidth, bufferWidth - 2 * BUFFER_MARGIN);
            int srcHeight = Math.min(imageHeight, bufferHeight - 2 * BUFFER_MARGIN);
            
            if (srcWidth <= 0 || srcHeight <= 0) {
                return;
            }
            
            BufferedImage visiblePortion = masterBuffer.getSubimage(srcX, srcY, srcWidth, srcHeight);
            
            // Apply transform for proper coordinate synchronization
            AffineTransform transform = new AffineTransform();
            transform.scale(scaleX, scaleY);
            transform.translate(offsetX / scaleX, offsetY / scaleY);
            
            target.drawImage(visiblePortion, transform, null);
            
        } catch (Exception e) {
            LOGGER.warn("Failed to copy from buffer: {}", e.getMessage());
        }
    }
    
    /**
     * Render ROIs directly to target graphics (fallback when buffer is not available).
     */
    public void renderDirectly(Graphics2D target, List<UserROI> rois, 
                              double scaleX, double scaleY, double offsetX, double offsetY) {
        target.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        target.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        
        for (UserROI roi : rois) {
            if (colorProvider.shouldRenderROI(roi)) {
                renderROIDirectly(target, roi, scaleX, scaleY, offsetX, offsetY);
            }
        }
    }
    
    /**
     * Get or calculate shape for ROI (cached for performance).
     */
    public java.awt.Shape getROIShape(UserROI roi) {
        String roiKey = roi.getId();
        java.awt.Shape cachedShape = shapeCache.get(roiKey);
        
        if (cachedShape == null) {
            cachedShape = calculateROIShape(roi);
            if (cachedShape != null) {
                shapeCache.put(roiKey, cachedShape);
            }
        }
        
        return cachedShape;
    }
    
    /**
     * Test if a point intersects with an ROI.
     */
    public boolean isPointInROI(Point point, UserROI roi, 
                               double scaleX, double scaleY, double offsetX, double offsetY) {
        java.awt.Shape shape = getROIShape(roi);
        if (shape == null) {
            return false;
        }
        
        try {
            // Apply full transform to match display
            AffineTransform transform = new AffineTransform();
            transform.scale(scaleX, scaleY);
            transform.translate(offsetX / scaleX, offsetY / scaleY);
            
            // Account for ShapeRoi base coordinates
            Roi imageJRoi = roi.getImageJRoi();
            if (imageJRoi instanceof ShapeRoi) {
                ShapeRoi shapeRoi = (ShapeRoi) imageJRoi;
                transform.translate(shapeRoi.getXBase(), shapeRoi.getYBase());
            }
            
            java.awt.Shape transformedShape = transform.createTransformedShape(shape);
            return transformedShape.contains(point);
            
        } catch (Exception e) {
            LOGGER.debug("Error testing point in ROI: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Invalidate the buffer to force re-rendering.
     */
    public void invalidateBuffer() {
        bufferValid = false;
    }
    
    /**
     * Clear shape cache (call when ROIs are modified).
     */
    public void clearShapeCache() {
        shapeCache.clear();
    }
    
    // === PRIVATE METHODS ===
    
    private void renderROIToBuffer(Graphics2D g2d, UserROI roi) {
        java.awt.Shape shape = getROIShape(roi);
        if (shape == null) {
            return;
        }
        
        // Calculate position with buffer margin
        double totalOffsetX = BUFFER_MARGIN;
        double totalOffsetY = BUFFER_MARGIN;
        
        // Account for ShapeRoi base coordinates
        Roi imageJRoi = roi.getImageJRoi();
        if (imageJRoi instanceof ShapeRoi) {
            ShapeRoi shapeRoi = (ShapeRoi) imageJRoi;
            totalOffsetX += shapeRoi.getXBase();
            totalOffsetY += shapeRoi.getYBase();
        }
        
        // Transform shape with position offset only
        AffineTransform transform = AffineTransform.getTranslateInstance(totalOffsetX, totalOffsetY);
        java.awt.Shape transformedShape = transform.createTransformedShape(shape);
        
        // Get colors and render
        MainSettings.ROICategory category = determineROICategory(roi);
        renderShape(g2d, transformedShape, roi, category);
    }
    
    private void renderROIDirectly(Graphics2D g2d, UserROI roi, 
                                  double scaleX, double scaleY, double offsetX, double offsetY) {
        java.awt.Shape shape = getROIShape(roi);
        if (shape == null) {
            return;
        }
        
        // Apply full transform
        AffineTransform transform = new AffineTransform();
        transform.scale(scaleX, scaleY);
        transform.translate(offsetX / scaleX, offsetY / scaleY);
        
        // Account for ShapeRoi base coordinates
        Roi imageJRoi = roi.getImageJRoi();
        if (imageJRoi instanceof ShapeRoi) {
            ShapeRoi shapeRoi = (ShapeRoi) imageJRoi;
            transform.translate(shapeRoi.getXBase(), shapeRoi.getYBase());
        }
        
        java.awt.Shape transformedShape = transform.createTransformedShape(shape);
        
        // Get colors and render
        MainSettings.ROICategory category = determineROICategory(roi);
        renderShape(g2d, transformedShape, roi, category);
    }
    
    private void renderShape(Graphics2D g2d, java.awt.Shape shape, UserROI roi, MainSettings.ROICategory category) {
        // Get colors from provider
        Color fillColor = colorProvider.getFillColor(roi, category);
        Color borderColor = colorProvider.getBorderColor(roi, category);
        float borderWidth = colorProvider.getBorderWidth(roi, category);
        
        // Fill shape
        g2d.setColor(fillColor);
        g2d.fill(shape);
        
        // Draw border
        g2d.setStroke(new BasicStroke(borderWidth));
        g2d.setColor(borderColor);
        g2d.draw(shape);
    }
    
    private java.awt.Shape calculateROIShape(UserROI roi) {
        Roi imageJRoi = roi.getImageJRoi();
        if (imageJRoi == null) {
            // Fallback to bounds rectangle
            Rectangle bounds = roi.getBounds();
            return new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        
        // Handle ShapeRoi (complex biological shapes)
        if (imageJRoi instanceof ShapeRoi) {
            ShapeRoi shapeRoi = (ShapeRoi) imageJRoi;
            java.awt.Shape shape = shapeRoi.getShape();
            if (shape != null) {
                return shape;
            }
        }
        
        // Fallback to polygon for other complex shapes
        Polygon polygon = imageJRoi.getPolygon();
        if (polygon != null) {
            return polygon;
        }
        
        // Final fallback to bounding rectangle
        Rectangle bounds = imageJRoi.getBounds();
        return new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
    }
    
    private MainSettings.ROICategory determineROICategory(UserROI roi) {
        // Check the class type first - most reliable
        if (roi instanceof NucleusROI) {
            return MainSettings.ROICategory.NUCLEUS;
        }
        if (roi instanceof CytoplasmROI) {
            return MainSettings.ROICategory.CYTOPLASM;
        }
        if (roi instanceof CellROI) {
            return MainSettings.ROICategory.CELL;
        }
        
        // Check ROI type
        UserROI.ROIType roiType = roi.getType();
        switch (roiType) {
            case VESSEL: return MainSettings.ROICategory.VESSEL;
            case NUCLEUS: return MainSettings.ROICategory.NUCLEUS;
            case CYTOPLASM: return MainSettings.ROICategory.CYTOPLASM;
            case CELL: return MainSettings.ROICategory.CELL;
            case IGNORE: return MainSettings.ROICategory.VESSEL; // Treat ignore as vessel category
            default: break;
        }
        
        // Check name-based heuristics as fallback
        String name = roi.getName().toLowerCase();
        if (name.contains("cell")) return MainSettings.ROICategory.CELL;
        if (name.contains("cytoplasm") || name.contains("cyto")) return MainSettings.ROICategory.CYTOPLASM;
        if (name.contains("nucleus") || name.contains("nuclei")) return MainSettings.ROICategory.NUCLEUS;
        if (roi.hasComplexShape() || name.contains("vessel")) return MainSettings.ROICategory.VESSEL;
        
        return MainSettings.ROICategory.VESSEL; // Default fallback
    }
}