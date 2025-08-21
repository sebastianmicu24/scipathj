package com.scipath.scipathj.ui.components;

import com.scipath.scipathj.core.config.MainSettings;
import com.scipath.scipathj.data.model.UserROI;
import com.scipath.scipathj.ui.utils.UIConstants;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ultra-efficient ROI overlay with single-calculation and buffered rendering.
 *
 * Key optimizations:
 * - ShapeRoi shapes calculated once at creation time
 * - Large buffer covering entire image at native resolution
 * - Fast copy operations for scroll/zoom (no re-rendering)
 * - Native image resolution as baseline
 */
public class ROIOverlay extends JComponent {

  private static final Logger LOGGER = LoggerFactory.getLogger(ROIOverlay.class);

  // Performance constants
  private static final int SELECTION_STROKE_WIDTH = UIConstants.SELECTION_STROKE_WIDTH;
  private static final float[] DASH_PATTERN = UIConstants.DASH_PATTERN;
  private static final Color SELECTION_COLOR = UIConstants.ROI_SELECTION_COLOR;
  private static final int BUFFER_MARGIN = 100; // Extra space around image for scrolling

  // Core settings
  private MainSettings mainSettings;

  // ROI data
  private final List<UserROI> displayedROIs = new CopyOnWriteArrayList<>();
  private String currentImageFileName;

  // Filter state
  private boolean vesselFilterEnabled = true;
  private boolean nucleusFilterEnabled = true;
  private boolean cytoplasmFilterEnabled = true;
  private boolean cellFilterEnabled = true;

  // Single-calculation shape cache - calculated once, never recalculated
  private final Map<Integer, java.awt.Shape> originalShapes = new ConcurrentHashMap<>();

  // Large buffer covering entire image at native resolution
  private BufferedImage masterBuffer;
  private int bufferWidth;
  private int bufferHeight;
  private boolean bufferValid = false;

  // Transform state for fast copy operations
  private double scaleX = 1.0;
  private double scaleY = 1.0;
  private double offsetX = 0.0;
  private double offsetY = 0.0;
  private int imageWidth = 0;
  private int imageHeight = 0;

  // Mouse interaction state
  private UserROI selectedROI;
  private boolean isCreatingROI = false;
  private Point roiStartPoint;
  private Point roiCurrentPoint;
  private UserROI.ROIType creationMode = UserROI.ROIType.SQUARE;

  // Listeners
  private final List<ROIOverlayListener> listeners = new CopyOnWriteArrayList<>();

  public interface ROIOverlayListener {
    void onROICreated(UserROI roi);
    void onROISelected(UserROI roi);
    void onROIDeselected();
  }

  public ROIOverlay() {
    this(MainSettings.createDefault());
  }

  public ROIOverlay(MainSettings mainSettings) {
    this.mainSettings = Objects.requireNonNull(mainSettings, "mainSettings");
    setOpaque(false);

    setupMouseHandlers();
    LOGGER.info("Created ultra-efficient ROI overlay");
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

  // ===== SINGLE-CALCULATION SHAPE SYSTEM =====

  /**
   * Calculate and cache the original shape for an ROI - done once per ROI lifetime
   */
  private java.awt.Shape getOrCalculateOriginalShape(UserROI roi) {
    Integer roiKey = roi.hashCode();

    java.awt.Shape cachedShape = originalShapes.get(roiKey);
    if (cachedShape != null) {
      return cachedShape;
    }

    // Calculate shape once and cache forever
    java.awt.Shape calculatedShape = calculateOriginalShape(roi);
    if (calculatedShape != null) {
      originalShapes.put(roiKey, calculatedShape);
      // LOGGER.debug("Calculated and cached original shape for ROI: {}", roi.getName());
    }

    return calculatedShape;
  }

  /**
   * Calculate the original shape for an ROI at native image resolution
   */
  private java.awt.Shape calculateOriginalShape(UserROI roi) {
    Roi imageJRoi = roi.getImageJRoi();
    if (imageJRoi == null) {
      // Fallback to simple rectangle
      Rectangle bounds = roi.getBounds();
      return new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    // Handle ShapeRoi (donut shapes) - calculate once at creation
    if (imageJRoi instanceof ShapeRoi) {
      ShapeRoi shapeRoi = (ShapeRoi) imageJRoi;
      java.awt.Shape shape = shapeRoi.getShape();
      if (shape != null) {
        // LOGGER.debug("Extracted Shape from ShapeRoi for '{}': {}", roi.getName(), shape.getClass().getSimpleName());
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

  // ===== MASTER BUFFER SYSTEM =====

  /**
   * Initialize or resize the master buffer for native image resolution
   */
  private void ensureBufferSize(int width, int height) {
    // For native resolution rendering, we need space for the entire image
    // plus margins for positioning ROIs that might extend beyond image bounds
    int requiredWidth = width + 2 * BUFFER_MARGIN;
    int requiredHeight = height + 2 * BUFFER_MARGIN;

    if (masterBuffer == null || bufferWidth < requiredWidth || bufferHeight < requiredHeight) {
      bufferWidth = Math.max(requiredWidth, bufferWidth);
      bufferHeight = Math.max(requiredHeight, bufferHeight);

      masterBuffer = new BufferedImage(bufferWidth, bufferHeight, BufferedImage.TYPE_INT_ARGB);
      bufferValid = false;
    }
  }

  /**
   * Render all ROIs to the master buffer at native image resolution
   */
  private void renderToMasterBuffer() {
    if (masterBuffer == null || displayedROIs.isEmpty()) {
      bufferValid = false;
      return;
    }


    Graphics2D g2d = masterBuffer.createGraphics();

    try {
      // Clear buffer with transparent background
      g2d.setComposite(AlphaComposite.Clear);
      g2d.fillRect(0, 0, bufferWidth, bufferHeight);
      g2d.setComposite(AlphaComposite.SrcOver);

      // Disable antialiasing for maximum performance
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

      // Render only ROIs that pass the current filter
      for (UserROI roi : displayedROIs) {
        if (shouldDisplayROI(roi)) {
          renderROIAtNativeResolution(g2d, roi, BUFFER_MARGIN, BUFFER_MARGIN);
        }
      }

      bufferValid = true;

    } finally {
      g2d.dispose();
    }
  }

  /**
   * Render a single ROI to the buffer at native resolution
   * IMPORTANT: Only apply base coordinate offsets here, not scaling/translation
   * The main scaling and translation will be applied during copyFromMasterBuffer()
   */
  private void renderROIAtNativeResolution(Graphics2D g2d, UserROI roi, int offsetX, int offsetY) {
    java.awt.Shape originalShape = getOrCalculateOriginalShape(roi);
    if (originalShape == null) return;

    // Get appearance settings
    MainSettings.ROICategory category = determineROICategory(roi);
    MainSettings.ROIAppearanceSettings settings = mainSettings.getSettingsForCategory(category);

    // Check if ROI should be ignored
    boolean isIgnored = roi.isIgnored();
    Color fillColor, borderColor;
    float borderWidth;

    if (isIgnored) {
      // Use ignore settings for ignored ROIs
      MainSettings.IgnoreROIAppearanceSettings ignoreSettings = mainSettings.ignoreSettings();
      if (!ignoreSettings.showIgnoredROIs()) {
        return; // Don't render ignored ROIs if they're disabled
      }
      fillColor = new Color(
          ignoreSettings.ignoreColor().getRed(),
          ignoreSettings.ignoreColor().getGreen(),
          ignoreSettings.ignoreColor().getBlue(),
          (int)(settings.fillOpacity() * 255));
      borderColor = ignoreSettings.ignoreColor();
      borderWidth = settings.borderWidth();
    } else {
      // Use normal settings for regular ROIs
      fillColor = settings.getFillColor();
      borderColor = roi.getDisplayColor() != null ? roi.getDisplayColor() : settings.borderColor();
      borderWidth = settings.borderWidth();
    }

    // Calculate position: base margin offset plus ROI-specific base coordinates
    double totalOffsetX = offsetX;
    double totalOffsetY = offsetY;

    // For ShapeRoi objects, account for their base coordinates
    Roi imageJRoi = roi.getImageJRoi();
    if (imageJRoi instanceof ShapeRoi) {
      ShapeRoi shapeRoi = (ShapeRoi) imageJRoi;
      totalOffsetX += shapeRoi.getXBase();
      totalOffsetY += shapeRoi.getYBase();
    }

    // Create transformed shape with ONLY position offset (no scaling)
    AffineTransform transform = AffineTransform.getTranslateInstance(totalOffsetX, totalOffsetY);
    java.awt.Shape transformedShape = transform.createTransformedShape(originalShape);

    // Fill shape
    g2d.setColor(fillColor);
    g2d.fill(transformedShape);

    // Draw border
    Stroke borderStroke = new BasicStroke(borderWidth);
    g2d.setStroke(borderStroke);
    g2d.setColor(borderColor);
    g2d.draw(transformedShape);
  }
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    Graphics2D g2d = (Graphics2D) g.create();
    
    try {
      // HIGH-LEVEL LOGGING: Track rendering path and ROI count
      String renderingPath;
      if (bufferValid && masterBuffer != null) {
        renderingPath = "BUFFER_COPY";
        copyFromMasterBuffer(g2d);
      } else if (!displayedROIs.isEmpty()) {
        renderingPath = "DIRECT_RENDER";
        renderDirectly(g2d);
      } else {
        renderingPath = "NO_ROIS";
      }
      

      // Always render creation ROI directly (not in buffer)
      if (isCreatingROI && roiStartPoint != null && roiCurrentPoint != null) {
        renderCreationROI(g2d);
      }

    } finally {
      g2d.dispose();
    }
  }

  /**
   * Fast copy from master buffer with exact coordinate synchronization
   * CRITICAL: This must use exactly the same transform as the image
   */
  private void copyFromMasterBuffer(Graphics2D g2d) {
    if (masterBuffer == null || !bufferValid) {
      renderDirectly(g2d);
      return;
    }

    // SIMPLIFIED APPROACH: The buffer contains ROIs at native resolution
    // We always copy the entire image region from the buffer and let the transform handle positioning
    // This eliminates complex coordinate calculations that could introduce errors
    try {
      int srcX = BUFFER_MARGIN;
      int srcY = BUFFER_MARGIN;
      int srcWidth = Math.min(imageWidth, bufferWidth - 2 * BUFFER_MARGIN);
      int srcHeight = Math.min(imageHeight, bufferHeight - 2 * BUFFER_MARGIN);

      if (srcWidth <= 0 || srcHeight <= 0) {
        renderDirectly(g2d);
        return;
      }

      // Extract the image portion from the buffer (fixed region)
      BufferedImage visiblePortion = masterBuffer.getSubimage(srcX, srcY, srcWidth, srcHeight);

      // Apply EXACTLY the same transform as the image uses
      // This ensures the overlay moves exactly the same amount as the image
      AffineTransform transform = new AffineTransform();
      transform.scale(scaleX, scaleY);
      transform.translate(offsetX / scaleX, offsetY / scaleY);


      g2d.drawImage(visiblePortion, transform, null);

    } catch (Exception e) {

    }
  }

  /**
   * Direct rendering fallback when buffer is not available
   */
  private void renderDirectly(Graphics2D g2d) {
    LOGGER.debug("DIRECT RENDERING: {} ROIs with transform scale=({}, {}), offset=({}, {})",
        displayedROIs.size(), scaleX, scaleY, offsetX, offsetY);

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

    // Render only ROIs that pass the current filter
    for (UserROI roi : displayedROIs) {
      if (shouldDisplayROI(roi)) {
        renderROIDirect(g2d, roi);
      }
    }
  }

  private void renderROIDirect(Graphics2D g2d, UserROI roi) {
    java.awt.Shape originalShape = getOrCalculateOriginalShape(roi);
    if (originalShape == null) return;

    // UNIFIED APPROACH: Use exactly the same transform as buffer copy path
    // First, create the base transform (same as buffer copy)
    AffineTransform transform = new AffineTransform();
    transform.scale(scaleX, scaleY);
    transform.translate(offsetX / scaleX, offsetY / scaleY);

    // Apply any additional ROI-specific offsets (for ShapeRoi base coordinates)
    Roi imageJRoi = roi.getImageJRoi();
    if (imageJRoi instanceof ShapeRoi) {
      ShapeRoi shapeRoi = (ShapeRoi) imageJRoi;
      // Apply base coordinate offset AFTER the main transform
      transform.translate(shapeRoi.getXBase(), shapeRoi.getYBase());
    }

    // Apply the transform to the shape
    java.awt.Shape transformedShape = transform.createTransformedShape(originalShape);

    // Get appearance settings and render
    MainSettings.ROICategory category = determineROICategory(roi);
    MainSettings.ROIAppearanceSettings settings = mainSettings.getSettingsForCategory(category);

    // Check if ROI should be ignored
    boolean isIgnored = roi.isIgnored();
    Color fillColor, borderColor;
    float borderWidth;

    if (isIgnored) {
      // Use ignore settings for ignored ROIs
      MainSettings.IgnoreROIAppearanceSettings ignoreSettings = mainSettings.ignoreSettings();
      if (!ignoreSettings.showIgnoredROIs()) {
        return; // Don't render ignored ROIs if they're disabled
      }
      fillColor = new Color(
          ignoreSettings.ignoreColor().getRed(),
          ignoreSettings.ignoreColor().getGreen(),
          ignoreSettings.ignoreColor().getBlue(),
          (int)(settings.fillOpacity() * 255));
      borderColor = ignoreSettings.ignoreColor();
      borderWidth = settings.borderWidth();
    } else {
      // Use normal settings for regular ROIs
      fillColor = settings.getFillColor();
      borderColor = roi.getDisplayColor() != null ? roi.getDisplayColor() : settings.borderColor();
      borderWidth = settings.borderWidth();
    }

    g2d.setColor(fillColor);
    g2d.fill(transformedShape);

    Stroke borderStroke = new BasicStroke(borderWidth);
    g2d.setStroke(borderStroke);
    g2d.setColor(borderColor);
    g2d.draw(transformedShape);
  }

  private void renderCreationROI(Graphics2D g2d) {
    Rectangle bounds = createRectangleFromPoints(roiStartPoint, roiCurrentPoint);

    // UNIFIED TRANSFORM: Use exactly the same transform as other rendering paths
    AffineTransform transform = new AffineTransform();
    transform.scale(scaleX, scaleY);
    transform.translate(offsetX / scaleX, offsetY / scaleY);
    java.awt.Shape transformedShape = transform.createTransformedShape(bounds);

    // Use vessel settings for creation preview
    MainSettings.ROIAppearanceSettings settings = mainSettings.getVesselSettings();
    Color borderColor = settings.borderColor();

    // Semi-transparent fill for preview
    float previewOpacity = Math.max(0.1f, settings.fillOpacity() * 0.5f);
    Color fillColor = new Color(
        borderColor.getRed(),
        borderColor.getGreen(),
        borderColor.getBlue(),
        Math.round(previewOpacity * 255));

    g2d.setColor(fillColor);
    g2d.fill(transformedShape);

    // Dashed border
    g2d.setStroke(new BasicStroke(
        settings.borderWidth(),
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND,
        0,
        DASH_PATTERN,
        0));
    g2d.setColor(borderColor);
    g2d.draw(transformedShape);
  }

  // ===== PUBLIC API =====

  public void setDisplayedROIs(List<UserROI> rois, String imageFileName) {
    displayedROIs.clear();
    if (rois != null) {
      // Add all ROIs without filtering - filtering will happen during rendering
      displayedROIs.addAll(rois);
    }
    currentImageFileName = imageFileName;
    selectedROI = null;

    // Invalidate buffer and trigger re-render
    bufferValid = false;
    repaint();

    LOGGER.debug("Set {} ROIs for image '{}' (all ROIs kept for filtering during render)",
        displayedROIs.size(), imageFileName, rois != null ? rois.size() : 0);
  }

  public void setImageTransform(double scaleX, double scaleY, double offsetX, double offsetY) {
    // DIAGNOSTIC LOG: Track transform changes
    boolean scaleChanged = Math.abs(this.scaleX - scaleX) > 0.001 || Math.abs(this.scaleY - scaleY) > 0.001;
    boolean offsetChanged = Math.abs(this.offsetX - offsetX) > 0.1 || Math.abs(this.offsetY - offsetY) > 0.1;
    
    this.scaleX = scaleX;
    this.scaleY = scaleY;
    this.offsetX = offsetX;
    this.offsetY = offsetY;

    // No need to invalidate buffer - just repaint with new transform
    repaint();
  }

  public void setImageDimensions(int width, int height) {
    if (this.imageWidth != width || this.imageHeight != height) {
      this.imageWidth = width;
      this.imageHeight = height;

      ensureBufferSize(width, height);

      // Trigger buffer re-render on dimension change
      if (!displayedROIs.isEmpty()) {
        renderToMasterBuffer();
      }
    }
  }

  public void updateSettings(MainSettings newSettings) {
    this.mainSettings = Objects.requireNonNull(newSettings, "newSettings");
    bufferValid = false; // Settings change requires re-render
    repaint();
  }

  public void addROIOverlayListener(ROIOverlayListener listener) {
    listeners.add(listener);
  }

  public void removeROIOverlayListener(ROIOverlayListener listener) {
    listeners.remove(listener);
  }

  /**
   * Update filter state for ROI types
   */
  public void setFilterState(MainSettings.ROICategory category, boolean enabled) {
    switch (category) {
      case VESSEL:
        vesselFilterEnabled = enabled;
        break;
      case NUCLEUS:
        nucleusFilterEnabled = enabled;
        break;
      case CYTOPLASM:
        cytoplasmFilterEnabled = enabled;
        break;
      case CELL:
        cellFilterEnabled = enabled;
        break;
    }
    // Trigger re-render with new filter state
    bufferValid = false;
    repaint();
  }

  // ===== ROI CREATION AND SELECTION =====

  public void setROICreationMode(UserROI.ROIType type) {
    creationMode = type;
    isCreatingROI = false;
    roiStartPoint = null;
    roiCurrentPoint = null;
    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
  }

  public void disableROICreationMode() {
    isCreatingROI = false;
    roiStartPoint = null;
    roiCurrentPoint = null;
    setCursor(Cursor.getDefaultCursor());
    repaint();
  }

  public void selectROI(UserROI roi) {
    selectedROI = roi;
    repaint();
    listeners.forEach(listener -> {
      try {
        listener.onROISelected(roi);
      } catch (Exception e) {
        LOGGER.error("Error notifying ROI selection", e);
      }
    });
  }

  public void clearSelection() {
    selectedROI = null;
    repaint();
    listeners.forEach(listener -> {
      try {
        listener.onROIDeselected();
      } catch (Exception e) {
        LOGGER.error("Error notifying ROI deselection", e);
      }
    });
  }

  // ===== MOUSE HANDLING =====

  private void handleMousePressed(MouseEvent e) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      if (creationMode != null && currentImageFileName != null) {
        isCreatingROI = true;
        roiStartPoint = e.getPoint();
        roiCurrentPoint = e.getPoint();
        clearSelection();
        repaint();
      } else {
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
      Rectangle bounds = createRectangleFromPoints(roiStartPoint, roiCurrentPoint);

      if (bounds.width > 5 && bounds.height > 5) {
        Rectangle imageBounds = inverseTransformRectangle(bounds);

        UserROI newROI;
        if (creationMode == UserROI.ROIType.SQUARE) {
          int size = Math.min(imageBounds.width, imageBounds.height);
          newROI = UserROI.createSquareROI(imageBounds.x, imageBounds.y, size, currentImageFileName);
        } else {
          newROI = UserROI.createRectangleROI(
              imageBounds.x, imageBounds.y, imageBounds.width, imageBounds.height, currentImageFileName);
        }

        listeners.forEach(listener -> {
          try {
            listener.onROICreated(newROI);
          } catch (Exception ex) {
            LOGGER.error("Error notifying ROI creation", ex);
          }
        });

        LOGGER.info("Created new {} ROI: {}", creationMode, newROI);
      }

      isCreatingROI = false;
      roiStartPoint = null;
      roiCurrentPoint = null;
      repaint();
    }
  }

  private void handleMouseClicked(MouseEvent e) {
    if (e.getClickCount() == 2) {
      UserROI clickedROI = findROIAtPoint(e.getPoint());

    }
  }

  private UserROI findROIAtPoint(Point point) {
    // Iterate backwards to find topmost visible ROI
    for (int i = displayedROIs.size() - 1; i >= 0; i--) {
      UserROI roi = displayedROIs.get(i);
      // Only consider ROIs that pass the current filter
      if (shouldDisplayROI(roi) && isPointInROI(point, roi)) {
        return roi;
      }
    }
    return null;
  }

  private boolean isPointInROI(Point point, UserROI roi) {
    java.awt.Shape originalShape = getOrCalculateOriginalShape(roi);
    if (originalShape == null) return false;

    // Transform point back to image coordinates
    double imageX = (point.x - offsetX) / scaleX;
    double imageY = (point.y - offsetY) / scaleY;

    return originalShape.contains(imageX, imageY);
  }

 /**
  * Determine the ROI category for appearance settings
  */
 private MainSettings.ROICategory determineROICategory(UserROI roi) {
   // First check the class type - this is more reliable than ROI type
   if (roi instanceof com.scipath.scipathj.data.model.NucleusROI) {
     return MainSettings.ROICategory.NUCLEUS;
   }

   // Then check the ROI type
   UserROI.ROIType roiType = roi.getType();
   switch (roiType) {
     case VESSEL: return MainSettings.ROICategory.VESSEL;
     case COMPLEX_SHAPE: return MainSettings.ROICategory.VESSEL;
     case NUCLEUS: return MainSettings.ROICategory.NUCLEUS;
     case CYTOPLASM: return MainSettings.ROICategory.CYTOPLASM;
     case CELL: return MainSettings.ROICategory.CELL;
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

 /**
  * Check if an ROI should be displayed based on current filter state
  */
 private boolean shouldDisplayROI(UserROI roi) {
   MainSettings.ROICategory category = determineROICategory(roi);

   switch (category) {
     case VESSEL:
       return vesselFilterEnabled;
     case NUCLEUS:
       return nucleusFilterEnabled;
     case CYTOPLASM:
       return cytoplasmFilterEnabled;
     case CELL:
       return cellFilterEnabled;
     default:
       return true; // Show unknown categories by default
   }
 }

  private Rectangle createRectangleFromPoints(Point start, Point end) {
    int x = Math.min(start.x, end.x);
    int y = Math.min(start.y, end.y);
    int width = Math.abs(end.x - start.x);
    int height = Math.abs(end.y - start.y);
    return new Rectangle(x, y, width, height);
  }

  private Rectangle inverseTransformRectangle(Rectangle transformed) {
    int x = (int) Math.round((transformed.x - offsetX) / scaleX);
    int y = (int) Math.round((transformed.y - offsetY) / scaleY);
    int width = (int) Math.round(transformed.width / scaleX);
    int height = (int) Math.round(transformed.height / scaleY);
    return new Rectangle(x, y, width, height);
  }
}
