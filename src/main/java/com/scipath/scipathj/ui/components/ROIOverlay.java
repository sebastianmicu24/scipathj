package com.scipath.scipathj.ui.components;

import com.scipath.scipathj.core.config.MainSettings;
import com.scipath.scipathj.data.model.UserROI;
import com.scipath.scipathj.ui.utils.UIConstants;
import ij.gui.Roi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Transparent overlay component that displays ROIs on top of images.
 * Handles ROI rendering and mouse interactions for ROI selection and creation.
 *
 * NOTE: The square ROI selection is currently a temporary feature for testing purposes.
 */
public class ROIOverlay extends JComponent {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ROIOverlay.class);
    
    // ROI display settings (using MainSettings for configurable appearance)
    private static final int SELECTION_STROKE_WIDTH = UIConstants.SELECTION_STROKE_WIDTH;
    private static final float[] DASH_PATTERN = UIConstants.DASH_PATTERN;
    private static final Color SELECTION_COLOR = UIConstants.ROI_SELECTION_COLOR;
    
    // Main settings for configurable appearance
    private final MainSettings mainSettings;
    
    // Current ROIs to display
    private final List<UserROI> displayedROIs;
    
    // ROI creation state
    private boolean isCreatingROI = false;
    private Point roiStartPoint;
    private Point roiCurrentPoint;
    private UserROI.ROIType creationMode = UserROI.ROIType.SQUARE;
    
    // Selection state
    private UserROI selectedROI;
    private String currentImageFileName;
    
    // Scale factor for coordinate transformation - use double for better precision
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private double imageOffsetX = 0.0;
    private double imageOffsetY = 0.0;
    
    // Listeners
    private final List<ROIOverlayListener> listeners;
    
    public interface ROIOverlayListener {
        void onROICreated(UserROI roi);
        void onROISelected(UserROI roi);
        void onROIDeselected();
    }
    
    public ROIOverlay() {
        this.displayedROIs = new CopyOnWriteArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.mainSettings = MainSettings.getInstance();
        
        setOpaque(false);
        setBackground(new Color(0, 0, 0, 0)); // Fully transparent
        
        setupMouseHandlers();
        
        // Listen for settings changes to refresh the display
        mainSettings.addSettingsChangeListener(this::onSettingsChanged);
    }
    
    /**
     * Handle settings changes by refreshing the display
     */
    private void onSettingsChanged() {
        SwingUtilities.invokeLater(this::repaint);
        LOGGER.debug("ROI overlay refreshed due to settings change");
    }
    
    private void setupMouseHandlers() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e);
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClicked(e);
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    
    /**
     * Add a listener for ROI overlay events
     */
    public void addROIOverlayListener(ROIOverlayListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a listener for ROI overlay events
     */
    public void removeROIOverlayListener(ROIOverlayListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Set the ROIs to display for the current image
     */
    public void setDisplayedROIs(List<UserROI> rois, String imageFileName) {
        this.displayedROIs.clear();
        if (rois != null) {
            this.displayedROIs.addAll(rois);
        }
        this.currentImageFileName = imageFileName;
        this.selectedROI = null;
        repaint();
        LOGGER.debug("Updated displayed ROIs: {} ROIs for image '{}'", 
                    this.displayedROIs.size(), imageFileName);
    }
    
    /**
     * Set the scale factors for coordinate transformation
     */
    public void setImageTransform(double scaleX, double scaleY, double offsetX, double offsetY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.imageOffsetX = offsetX;
        this.imageOffsetY = offsetY;
        
        LOGGER.debug("ROI transform updated: scale=({}, {}), offset=({}, {})",
                    scaleX, scaleY, offsetX, offsetY);
        repaint();
    }
    /**
     * Enable ROI creation mode
     */
    public void setROICreationMode(UserROI.ROIType type) {
        this.creationMode = type;
        this.isCreatingROI = false;
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        LOGGER.debug("Enabled ROI creation mode: {}", type);
    }
    
    /**
     * Disable ROI creation mode
     */
    public void disableROICreationMode() {
        this.isCreatingROI = false;
        this.roiStartPoint = null;
        this.roiCurrentPoint = null;
        setCursor(Cursor.getDefaultCursor());
        repaint();
        LOGGER.debug("Disabled ROI creation mode");
    }
    
    /**
     * Select a specific ROI
     */
    public void selectROI(UserROI roi) {
        this.selectedROI = roi;
        repaint();
        
        // Notify listeners
        listeners.forEach(listener -> {
            try {
                listener.onROISelected(roi);
            } catch (Exception e) {
                LOGGER.error("Error notifying ROI overlay listener", e);
            }
        });
    }
    
    /**
     * Clear ROI selection
     */
    public void clearSelection() {
        this.selectedROI = null;
        repaint();
        
        // Notify listeners
        listeners.forEach(listener -> {
            try {
                listener.onROIDeselected();
            } catch (Exception e) {
                LOGGER.error("Error notifying ROI overlay listener", e);
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        try {
            // Draw existing ROIs
            drawExistingROIs(g2d);
            
            // Draw ROI being created
            if (isCreatingROI && roiStartPoint != null && roiCurrentPoint != null) {
                drawCreationROI(g2d);
            }
        } finally {
            g2d.dispose();
        }
    }
    
    private void drawExistingROIs(Graphics2D g2d) {
        for (UserROI roi : displayedROIs) {
            drawROI(g2d, roi, roi.equals(selectedROI));
        }
    }
    private void drawROI(Graphics2D g2d, UserROI roi, boolean isSelected) {
        // Determine ROI category and get appropriate settings
        MainSettings.ROICategory category = determineROICategory(roi);
        MainSettings.ROIAppearanceSettings settings = mainSettings.getSettingsForCategory(category);
        
        // Set stroke style using type-specific settings
        Stroke stroke;
        Color borderColor;
        
        if (isSelected) {
            stroke = new BasicStroke(SELECTION_STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            borderColor = SELECTION_COLOR;
        } else {
            stroke = new BasicStroke(settings.getBorderWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            // Use ROI's specific color if set, otherwise use category-specific border color
            borderColor = roi.getDisplayColor() != null ? roi.getDisplayColor() : settings.getBorderColor();
        }
        
        g2d.setStroke(stroke);
        g2d.setColor(borderColor);
        
        // Check if this ROI has a complex shape (vessel ROI)
        if (roi.hasComplexShape()) {
            drawComplexROI(g2d, roi, borderColor, settings);
        } else {
            drawSimpleROI(g2d, roi, borderColor, settings);
        }
        
        // Draw ROI name if selected
        if (isSelected) {
            Rectangle bounds = transformRectangle(roi.getBounds());
            drawROILabel(g2d, roi.getName(), bounds);
        }
    }
    
        /**
         * Determine the ROI category based on the ROI properties
         */
        private MainSettings.ROICategory determineROICategory(UserROI roi) {
            // First check the ROI type enum
            UserROI.ROIType roiType = roi.getType();
            switch (roiType) {
                case VESSEL:
                    return MainSettings.ROICategory.VESSEL;
                case NUCLEUS:
                    return MainSettings.ROICategory.NUCLEUS;
                case CYTOPLASM:
                    return MainSettings.ROICategory.CYTOPLASM;
                case CELL:
                    return MainSettings.ROICategory.CELL;
                default:
                    break;
            }
            
            // Fallback to name-based detection for backward compatibility
            String name = roi.getName().toLowerCase();
            
            // Check for cell ROI
            if (name.contains("cell")) {
                return MainSettings.ROICategory.CELL;
            }
            
            // Check for cytoplasm ROI
            if (name.contains("cytoplasm") || name.contains("cyto")) {
                return MainSettings.ROICategory.CYTOPLASM;
            }
            
            // Check for nucleus ROI
            if (name.contains("nucleus") || name.contains("nuclei")) {
                return MainSettings.ROICategory.NUCLEUS;
            }
            
            // Check if this is a vessel ROI (has complex shape or is named as vessel)
            if (roi.hasComplexShape() || name.contains("vessel")) {
                return MainSettings.ROICategory.VESSEL;
            }
            
            // Default to vessel ROI for any other user-created ROIs
            return MainSettings.ROICategory.VESSEL;
        }
    
    /**
     * Draw a simple ROI (rectangle, square, circle)
     */
    private void drawSimpleROI(Graphics2D g2d, UserROI roi, Color borderColor, MainSettings.ROIAppearanceSettings settings) {
        Rectangle originalBounds = roi.getBounds();
        Rectangle bounds = transformRectangle(originalBounds);
        
        LOGGER.trace("Drawing simple ROI '{}': original=({},{} {}x{}) -> transformed=({},{} {}x{})",
                    roi.getName(), originalBounds.x, originalBounds.y, originalBounds.width, originalBounds.height,
                    bounds.x, bounds.y, bounds.width, bounds.height);
        
        // Create fill color using type-specific settings
        Color fillColor = settings.getFillColor();
        g2d.setColor(fillColor);
        
        switch (roi.getType()) {
            case SQUARE:
            case RECTANGLE:
                g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                break;
            case CIRCLE:
                g2d.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
                break;
            default:
                g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                break;
        }
        
        // Draw the ROI boundary on top of the fill
        g2d.setColor(borderColor);
        switch (roi.getType()) {
            case SQUARE:
            case RECTANGLE:
                g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
                break;
            case CIRCLE:
                g2d.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
                break;
            default:
                g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
                break;
        }
    }
    
    /**
     * Draw a complex ROI (vessel with actual shape)
     */
    private void drawComplexROI(Graphics2D g2d, UserROI roi, Color borderColor, MainSettings.ROIAppearanceSettings settings) {
        Roi imageJRoi = roi.getImageJRoi();
        if (imageJRoi == null) {
            // Fallback to simple drawing
            drawSimpleROI(g2d, roi, borderColor, settings);
            return;
        }
        
        Rectangle originalBounds = imageJRoi.getBounds();
        LOGGER.trace("Drawing complex ROI '{}': ImageJ bounds=({},{} {}x{}), type={}",
                    roi.getName(), originalBounds.x, originalBounds.y,
                    originalBounds.width, originalBounds.height, imageJRoi.getTypeAsString());
        
        // Save the current transform
        AffineTransform originalTransform = g2d.getTransform();
        
        try {
            // Apply scaling and offset transformation with proper precision
            AffineTransform transform = new AffineTransform();
            // Use precise double values for translation and scaling
            transform.translate(imageOffsetX, imageOffsetY);
            transform.scale(scaleX, scaleY);
            g2d.setTransform(transform);
            
            LOGGER.trace("Complex ROI transform: translate=({}, {}), scale=({}, {})",
                        imageOffsetX, imageOffsetY, scaleX, scaleY);
            
            // Try to get the polygon representation of the ROI
            Polygon polygon = imageJRoi.getPolygon();
            if (polygon != null) {
                // Draw fill with type-specific settings
                Color fillColor = settings.getFillColor();
                g2d.setColor(fillColor);
                g2d.fillPolygon(polygon);
                
                // Draw outline on top
                g2d.setColor(borderColor);
                g2d.drawPolygon(polygon);
            } else {
                // Fallback: draw bounding rectangle with fill
                Rectangle bounds = imageJRoi.getBounds();
                Color fillColor = settings.getFillColor();
                g2d.setColor(fillColor);
                g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                
                g2d.setColor(borderColor);
                g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        } catch (Exception e) {
            LOGGER.warn("Error drawing complex ROI shape, falling back to bounding box: {}", e.getMessage());
            // Restore original transform and draw simple rectangle
            g2d.setTransform(originalTransform);
            drawSimpleROI(g2d, roi, borderColor, settings);
        } finally {
            // Always restore the original transform
            g2d.setTransform(originalTransform);
        }
    }
    
    private void drawCreationROI(Graphics2D g2d) {
        Rectangle bounds = createRectangleFromPoints(roiStartPoint, roiCurrentPoint);
        
        // Use vessel ROI settings for creation preview
        MainSettings.ROIAppearanceSettings settings = mainSettings.getVesselSettings();
        Color borderColor = settings.getBorderColor();
        
        // Draw fill with lower opacity for creation preview
        float previewOpacity = Math.max(0.1f, settings.getFillOpacity() * 0.5f); // Half the normal opacity
        Color fillColor = new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(),
                                   Math.round(previewOpacity * 255));
        g2d.setColor(fillColor);
        
        switch (creationMode) {
            case SQUARE:
                // Make it a square by using the smaller dimension
                int size = Math.min(bounds.width, bounds.height);
                g2d.fillRect(bounds.x, bounds.y, size, size);
                break;
            case RECTANGLE:
                g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                break;
            case CIRCLE:
                g2d.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
                break;
            default:
                g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                break;
        }
        
        // Draw dashed outline on top
        g2d.setStroke(new BasicStroke(settings.getBorderWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                                     0, DASH_PATTERN, 0));
        g2d.setColor(borderColor);
        
        switch (creationMode) {
            case SQUARE:
                // Make it a square by using the smaller dimension
                int size = Math.min(bounds.width, bounds.height);
                g2d.drawRect(bounds.x, bounds.y, size, size);
                break;
            case RECTANGLE:
                g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
                break;
            case CIRCLE:
                g2d.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
                break;
            default:
                g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
                break;
        }
    }
    
    private void drawROILabel(Graphics2D g2d, String label, Rectangle bounds) {
        FontMetrics fm = g2d.getFontMetrics();
        int labelWidth = fm.stringWidth(label);
        int labelHeight = fm.getHeight();
        
        // Position label above the ROI
        int labelX = bounds.x + (bounds.width - labelWidth) / 2;
        int labelY = bounds.y - 5;
        
        // Ensure label is within component bounds
        labelX = Math.max(0, Math.min(labelX, getWidth() - labelWidth));
        labelY = Math.max(labelHeight, labelY);
        
        // Draw label background
        g2d.setColor(new Color(0, 0, 0, 128));
        g2d.fillRect(labelX - 2, labelY - labelHeight + 2, labelWidth + 4, labelHeight);
        
        // Draw label text
        g2d.setColor(Color.WHITE);
        g2d.drawString(label, labelX, labelY);
    }
    
    private Rectangle transformRectangle(Rectangle original) {
        // Use high-precision transformation with proper rounding
        double transformedX = original.x * scaleX + imageOffsetX;
        double transformedY = original.y * scaleY + imageOffsetY;
        double transformedWidth = original.width * scaleX;
        double transformedHeight = original.height * scaleY;
        
        int x = (int) Math.round(transformedX);
        int y = (int) Math.round(transformedY);
        int width = (int) Math.round(transformedWidth);
        int height = (int) Math.round(transformedHeight);
        
        LOGGER.trace("Transform rectangle: ({},{} {}x{}) -> ({},{} {}x{}) [scale=({},{}), offset=({},{})]",
                    original.x, original.y, original.width, original.height,
                    x, y, width, height,
                    scaleX, scaleY, imageOffsetX, imageOffsetY);
        
        return new Rectangle(x, y, width, height);
    }
    
    private Rectangle inverseTransformRectangle(Rectangle transformed) {
        // Use Math.round for better precision instead of casting to int
        int x = (int) Math.round((transformed.x - imageOffsetX) / scaleX);
        int y = (int) Math.round((transformed.y - imageOffsetY) / scaleY);
        int width = (int) Math.round(transformed.width / scaleX);
        int height = (int) Math.round(transformed.height / scaleY);
        return new Rectangle(x, y, width, height);
    }
    
    private Rectangle createRectangleFromPoints(Point start, Point end) {
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(end.x - start.x);
        int height = Math.abs(end.y - start.y);
        return new Rectangle(x, y, width, height);
    }
    
    private void handleMousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (creationMode != null && currentImageFileName != null) {
                // Start creating a new ROI
                isCreatingROI = true;
                roiStartPoint = e.getPoint();
                roiCurrentPoint = e.getPoint();
                clearSelection();
                repaint();
            } else {
                // Check if clicking on an existing ROI
                UserROI clickedROI = findROIAtPoint(e.getPoint());
                if (clickedROI != null) {
                    selectROI(clickedROI);
                } else {
                    clearSelection();
                }
            }
        }
    }
    
    private void handleMouseDragged(MouseEvent e) {
        if (isCreatingROI && roiStartPoint != null) {
            roiCurrentPoint = e.getPoint();
            repaint();
        }
    }
    
    private void handleMouseReleased(MouseEvent e) {
        if (isCreatingROI && roiStartPoint != null && roiCurrentPoint != null) {
            // Create the ROI
            Rectangle bounds = createRectangleFromPoints(roiStartPoint, roiCurrentPoint);
            
            // Minimum size check
            if (bounds.width > 5 && bounds.height > 5) {
                // Transform coordinates back to image space
                Rectangle imageBounds = inverseTransformRectangle(bounds);
                
                // Create the appropriate ROI type
                UserROI newROI;
                if (creationMode == UserROI.ROIType.SQUARE) {
                    int size = Math.min(imageBounds.width, imageBounds.height);
                    newROI = UserROI.createSquareROI(imageBounds.x, imageBounds.y, size, currentImageFileName);
                } else {
                    newROI = UserROI.createRectangleROI(imageBounds.x, imageBounds.y, 
                                                       imageBounds.width, imageBounds.height, currentImageFileName);
                }
                
                // Notify listeners
                listeners.forEach(listener -> {
                    try {
                        listener.onROICreated(newROI);
                    } catch (Exception ex) {
                        LOGGER.error("Error notifying ROI overlay listener", ex);
                    }
                });
                
                LOGGER.info("Created new {} ROI: {}", creationMode, newROI);
            }
            
            // Reset creation state
            isCreatingROI = false;
            roiStartPoint = null;
            roiCurrentPoint = null;
            repaint();
        }
    }
    
    private void handleMouseClicked(MouseEvent e) {
        // Handle double-click for ROI editing (future enhancement)
        if (e.getClickCount() == 2) {
            UserROI clickedROI = findROIAtPoint(e.getPoint());
            if (clickedROI != null) {
                LOGGER.debug("Double-clicked ROI: {}", clickedROI.getName());
                // Future: Open ROI properties dialog
            }
        }
    }
    
    private UserROI findROIAtPoint(Point point) {
        // Check ROIs in reverse order (top to bottom)
        for (int i = displayedROIs.size() - 1; i >= 0; i--) {
            UserROI roi = displayedROIs.get(i);
            
            if (roi.hasComplexShape()) {
                // For complex shapes, check the actual shape
                if (isPointInComplexROI(point, roi)) {
                    return roi;
                }
            } else {
                // For simple shapes, check the bounding rectangle
                Rectangle transformedBounds = transformRectangle(roi.getBounds());
                if (transformedBounds.contains(point)) {
                    return roi;
                }
            }
        }
        return null;
    }
    
    /**
     * Check if a point is inside a complex ROI shape
     */
    private boolean isPointInComplexROI(Point point, UserROI roi) {
        Roi imageJRoi = roi.getImageJRoi();
        if (imageJRoi == null) {
            return false;
        }
        
        try {
            // Transform the point back to image coordinates with better precision
            double imageX = (point.x - imageOffsetX) / scaleX;
            double imageY = (point.y - imageOffsetY) / scaleY;
            
            // Check if the point is contained in the ImageJ ROI
            return imageJRoi.contains((int) Math.round(imageX), (int) Math.round(imageY));
        } catch (Exception e) {
            LOGGER.warn("Error checking point in complex ROI, falling back to bounding box: {}", e.getMessage());
            // Fallback to bounding box check
            Rectangle transformedBounds = transformRectangle(roi.getBounds());
            return transformedBounds.contains(point);
        }
    }
}