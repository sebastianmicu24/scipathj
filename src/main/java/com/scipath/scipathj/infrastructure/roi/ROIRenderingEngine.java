package com.scipath.scipathj.infrastructure.roi;

import com.scipath.scipathj.infrastructure.config.MainSettings;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.*;
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
    private long lastRenderTime = 0;
    private int lastROICount = 0;
    
    // Current rendering settings
    private MainSettings settings;
    private ROIColorProvider colorProvider;
    
    // Performance mode settings
    private boolean fastModeEnabled = false;
    
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
     * Enable fast mode for dataset creation (simple rectangle rendering like Fiji).
     */
    public void setFastModeEnabled(boolean enabled) {
        if (this.fastModeEnabled != enabled) {
            this.fastModeEnabled = enabled;
            invalidateBuffer();
            LOGGER.debug("Fast rendering mode {}", enabled ? "enabled" : "disabled");
        }
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
     * Render ROIs to the master buffer at native resolution with smart caching.
     */
    public void renderToBuffer(List<UserROI> rois, int imageWidth, int imageHeight) {
        ensureBufferSize(imageWidth, imageHeight);
        
        if (masterBuffer == null || rois.isEmpty()) {
            bufferValid = false;
            return;
        }
        
        // Smart caching: only re-render if buffer is invalid or data changed
        long currentTime = System.currentTimeMillis();
        boolean dataChanged = (rois.size() != lastROICount);
        
        if (bufferValid && !dataChanged && (currentTime - lastRenderTime) < 100) {
            // Buffer is valid and recent, skip rendering
            LOGGER.trace("Skipping buffer render - valid cache ({}ms ago, {} ROIs)",
                        currentTime - lastRenderTime, rois.size());
            return;
        }
        
        long startTime = System.nanoTime();
        
        Graphics2D g2d = masterBuffer.createGraphics();
        try {
            // Clear buffer
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, bufferWidth, bufferHeight);
            g2d.setComposite(AlphaComposite.SrcOver);
            
            // Optimize for maximum speed
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            
            // Choose rendering approach based on mode
            int renderedCount;
            if (fastModeEnabled) {
                renderedCount = renderROIsFastMode(g2d, rois);
            } else {
                renderedCount = renderROIsBatch(g2d, rois);
            }
            
            bufferValid = true;
            lastRenderTime = currentTime;
            lastROICount = rois.size();
            
            long renderTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            LOGGER.debug("Rendered {} ROIs to buffer in {}ms", renderedCount, renderTimeMs);
            
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
            transform.translate(offsetX, offsetY);
            transform.scale(scaleX, scaleY);
            
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
            // Apply correct transform to match display coordinates
            AffineTransform transform = new AffineTransform();
            transform.translate(offsetX, offsetY);
            transform.scale(scaleX, scaleY);
            // Buffer margin is already included in the rendered coordinates
            
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
        lastRenderTime = 0; // Reset cache timer
    }
    
    /**
     * Clear shape cache (call when ROIs are modified).
     */
    public void clearShapeCache() {
        shapeCache.clear();
    }
    
    // === PRIVATE METHODS ===
    
    /**
     * Ultra-fast polygon rendering mode for dataset creation - optimized for speed.
     */
    private int renderROIsFastMode(Graphics2D g2d, List<UserROI> rois) {
        int renderedCount = 0;
        
        // Set single color and stroke for all ROIs to minimize state changes
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(1.0f));
        
        // Pre-allocate arrays for polygon coordinates to avoid repeated allocations
        int[] xPoints = new int[1000]; // Most ROIs won't exceed this
        int[] yPoints = new int[1000];
        
        // Render all ROIs as fast polygons
        for (UserROI roi : rois) {
            if (!colorProvider.shouldRenderROI(roi)) {
                continue;
            }
            
            try {
                Roi imageJRoi = roi.getImageJRoi();
                if (imageJRoi != null) {
                    // Get polygon directly from ImageJ ROI (fastest method)
                    Polygon polygon = imageJRoi.getPolygon();
                    if (polygon != null && polygon.npoints > 2) {
                        
                        // Ensure arrays are large enough
                        if (polygon.npoints > xPoints.length) {
                            xPoints = new int[polygon.npoints + 100];
                            yPoints = new int[polygon.npoints + 100];
                        }
                        
                        // Copy and translate coordinates with proper offset handling
                        int baseOffsetX = BUFFER_MARGIN;
                        int baseOffsetY = BUFFER_MARGIN;
                        
                        // For ShapeRoi, DON'T add base coordinates as they're already in the polygon
                        // The polygon coordinates from getPolygon() are already relative to the image
                        
                        for (int i = 0; i < polygon.npoints; i++) {
                            xPoints[i] = polygon.xpoints[i] + baseOffsetX;
                            yPoints[i] = polygon.ypoints[i] + baseOffsetY;
                        }
                        
                        // Draw polygon outline (fast method)
                        g2d.drawPolygon(xPoints, yPoints, polygon.npoints);
                        renderedCount++;
                    }
                }
            } catch (Exception e) {
                // Ignore errors in fast mode for maximum performance
            }
        }
        
        return renderedCount;
    }
    
    /**
     * Fast batch rendering optimized for large numbers of ROIs.
     */
    private int renderROIsBatch(Graphics2D g2d, List<UserROI> rois) {
        int renderedCount = 0;
        
        // Group ROIs by color to minimize graphics state changes
        Map<Color, List<UserROI>> roisByColor = new HashMap<>();
        
        for (UserROI roi : rois) {
            if (!colorProvider.shouldRenderROI(roi)) {
                continue;
            }
            
            MainSettings.ROICategory category = determineROICategory(roi);
            Color borderColor = colorProvider.getBorderColor(roi, category);
            
            roisByColor.computeIfAbsent(borderColor, k -> new ArrayList<>()).add(roi);
        }
        
        // Render each color group in batch
        for (Map.Entry<Color, List<UserROI>> entry : roisByColor.entrySet()) {
            Color color = entry.getKey();
            List<UserROI> roisWithColor = entry.getValue();
            
            // Set color once for the whole batch
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2.0f)); // Use fixed stroke width for speed
            
            // Render all ROIs with this color
            for (UserROI roi : roisWithColor) {
                if (renderROIFast(g2d, roi)) {
                    renderedCount++;
                }
            }
        }
        
        return renderedCount;
    }
    
    /**
     * Fast ROI rendering using simplified geometry.
     */
    private boolean renderROIFast(Graphics2D g2d, UserROI roi) {
        try {
            // Use bounds rectangle for maximum speed (like Fiji)
            Rectangle bounds = roi.getBounds();
            if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
                return false;
            }
            
            // Calculate position with buffer margin only (bounds are already image-relative)
            int x = bounds.x + BUFFER_MARGIN;
            int y = bounds.y + BUFFER_MARGIN;
            
            // Draw simple rectangle outline (fastest approach)
            g2d.drawRect(x, y, bounds.width, bounds.height);
            
            return true;
        } catch (Exception e) {
            LOGGER.warn("Failed to render ROI '{}': {}", roi.getName(), e.getMessage());
            return false;
        }
    }
    
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