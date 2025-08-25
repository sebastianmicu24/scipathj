package com.scipath.scipathj.infrastructure.roi;

import ij.gui.Roi;
import java.awt.Color;
import java.awt.Rectangle;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a user-created Region of Interest (ROI) for manual selection and analysis.
 * This is different from analysis-generated ROIs like NucleusROI and CytoplasmROI.
 */
public class UserROI {

  public enum ROIType {
    NUCLEUS("Nucleus"),
    CYTOPLASM("Cytoplasm"),
    CELL("Cell"),
    VESSEL("Vessel"),
    IGNORE("Ignore");

    private final String displayName;

    ROIType(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }

  private final String id;
  private final ROIType type;
  private final Rectangle bounds;
  private final String name;
  private final LocalDateTime createdAt;
  private final String imageFileName;
  private Color displayColor;
  private String notes;
  private boolean ignored = false;
  private String assignedClass = null; // For dataset classification

  // For complex shapes (like vessels), store the actual ImageJ ROI
  private final Roi imageJRoi;

  /**
   * Creates a new UserROI from an ImageJ ROI (used for all biological structures).
   * @param imageJRoi The ImageJ ROI containing the biological structure
   * @param imageFileName The name of the image this ROI belongs to
   * @param name Optional name for the ROI (can be null for auto-generated names)
   * @param type The biological type of this ROI
   */
  public UserROI(Roi imageJRoi, String imageFileName, String name, ROIType type) {
    this.id = UUID.randomUUID().toString();
    this.type = type;
    this.bounds = imageJRoi.getBounds(); // Get bounds from ImageJ ROI
    this.imageFileName = imageFileName;
    this.name = name != null ? name : generateDefaultName();
    this.createdAt = LocalDateTime.now();
    this.displayColor = generateDefaultColorForType(type);
    this.notes = "";
    this.imageJRoi = (Roi) imageJRoi.clone(); // Store a copy of the ImageJ ROI
  }

  /**
   * Creates a new UserROI from an ImageJ ROI with auto-detected type.
   * @param imageJRoi The ImageJ ROI containing the biological structure
   * @param imageFileName The name of the image this ROI belongs to
   * @param name Optional name for the ROI (can be null for auto-generated names)
   */
  public UserROI(Roi imageJRoi, String imageFileName, String name) {
    this(imageJRoi, imageFileName, name, detectTypeFromName(name));
  }

  private String generateDefaultName() {
    return type.getDisplayName() + "_" + System.currentTimeMillis() % 10000;
  }

  private Color generateDefaultColorForType(ROIType type) {
    switch (type) {
      case NUCLEUS:
        return new Color(0, 255, 0, 128); // Semi-transparent green
      case CYTOPLASM:
        return new Color(0, 100, 255, 100); // Semi-transparent blue
      case CELL:
        return new Color(255, 255, 0, 120); // Semi-transparent yellow
      case VESSEL:
        return new Color(255, 0, 0, 120); // Semi-transparent red
      case IGNORE:
        return new Color(128, 128, 128, 80); // Semi-transparent gray
      default:
        return new Color(255, 255, 255, 100); // Semi-transparent white
    }
  }

  private static ROIType detectTypeFromName(String name) {
    if (name == null) {
      return ROIType.VESSEL; // Default fallback
    }
    
    String lowerName = name.toLowerCase();
    if (lowerName.contains("nucleus") || lowerName.contains("nuclei")) {
      return ROIType.NUCLEUS;
    } else if (lowerName.contains("cytoplasm") || lowerName.contains("cyto")) {
      return ROIType.CYTOPLASM;
    } else if (lowerName.contains("cell")) {
      return ROIType.CELL;
    } else if (lowerName.contains("vessel") || lowerName.contains("blood")) {
      return ROIType.VESSEL;
    } else {
      return ROIType.VESSEL; // Default to vessel for complex shapes
    }
  }

  // Getters
  public String getId() {
    return id;
  }

  public ROIType getType() {
    return type;
  }

  public Rectangle getBounds() {
    return new Rectangle(bounds);
  } // Return defensive copy

  public String getName() {
    return name;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public String getImageFileName() {
    return imageFileName;
  }

  public Color getDisplayColor() {
    return displayColor;
  }

  public String getNotes() {
    return notes;
  }

  public boolean isIgnored() {
    return ignored;
  }

  public void setIgnored(boolean ignored) {
    this.ignored = ignored;
  }

  /**
   * Checks if this ROI should be ignored based on its distance from image borders.
   *
   * @param imageWidth The width of the image
   * @param imageHeight The height of the image
   * @param borderDistance The minimum distance from borders to not be ignored
   * @return true if this ROI should be ignored
   */
  public boolean shouldBeIgnored(int imageWidth, int imageHeight, int borderDistance) {
    Rectangle bounds = getBounds();

    // Calculate distances to all four borders
    int distanceToLeft = bounds.x;
    int distanceToTop = bounds.y;
    int distanceToRight = imageWidth - (bounds.x + bounds.width);
    int distanceToBottom = imageHeight - (bounds.y + bounds.height);

    // ROI should be ignored if it's too close to any border
    return distanceToLeft < borderDistance
        || distanceToTop < borderDistance
        || distanceToRight < borderDistance
        || distanceToBottom < borderDistance;
  }

  // Additional getter for complex shapes
  public Roi getImageJRoi() {
    return imageJRoi != null ? (Roi) imageJRoi.clone() : null;
  }

  public boolean hasComplexShape() {
    return imageJRoi != null; // All biological structures have complex shapes
  }

  // Setters for mutable properties
  public void setDisplayColor(Color color) {
    this.displayColor = color;
  }

  public void setNotes(String notes) {
    this.notes = notes != null ? notes : "";
  }

  public String getAssignedClass() {
    return assignedClass;
  }

  public void setAssignedClass(String assignedClass) {
    this.assignedClass = assignedClass;
  }

  // Utility methods
  public int getX() {
    return bounds.x;
  }

  public int getY() {
    return bounds.y;
  }

  public int getWidth() {
    return bounds.width;
  }

  public int getHeight() {
    return bounds.height;
  }

  public int getCenterX() {
    return bounds.x + bounds.width / 2;
  }

  public int getCenterY() {
    return bounds.y + bounds.height / 2;
  }

  public double getArea() {
    if (hasComplexShape()) {
      return imageJRoi.getStatistics().area;
    }
    return bounds.width * bounds.height;
  }

  public boolean contains(int x, int y) {
    if (hasComplexShape()) {
      return imageJRoi.contains(x, y);
    }
    return bounds.contains(x, y);
  }

  public boolean intersects(Rectangle other) {
    return bounds.intersects(other);
  }

  @Override
  public String toString() {
    return String.format(
        "%s [%d,%d %dx%d] on %s",
        name, bounds.x, bounds.y, bounds.width, bounds.height, imageFileName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    UserROI userROI = (UserROI) obj;
    return id.equals(userROI.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
