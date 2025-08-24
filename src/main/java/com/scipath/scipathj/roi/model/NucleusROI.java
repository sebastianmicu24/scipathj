package com.scipath.scipathj.roi.model;

import ij.gui.Roi;
import java.awt.Color;
import java.awt.Rectangle;

/**
 * Represents a nucleus region of interest (ROI) in SciPathJ.
 * This class extends UserROI to provide nucleus-specific functionality
 * and metadata for nuclear segmentation results.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class NucleusROI extends UserROI {

  /** Default color for nucleus ROIs */
  public static final Color DEFAULT_NUCLEUS_COLOR =
      new Color(0, 255, 0, 128); // Semi-transparent green

  private double area;
  private double perimeter;
  private double circularity;
  private double aspectRatio;
  private double solidity;
  private boolean isValid;
  private String segmentationMethod;
  private double centroidX;
  private double centroidY;
  private CellROI parentCell;
  private CytoplasmROI associatedCytoplasm;

  /**
   * Creates a new NucleusROI from an ImageJ ROI.
   *
   * @param roi the ImageJ ROI representing the nucleus
   * @param imageFileName the filename of the image this ROI belongs to
   * @param nucleusName the name/identifier for this nucleus
   */
  public NucleusROI(Roi roi, String imageFileName, String nucleusName) {
    super(roi, imageFileName, nucleusName);
    setDisplayColor(DEFAULT_NUCLEUS_COLOR);
    this.segmentationMethod = "StarDist";
    this.isValid = true;
    calculateMorphologicalFeatures();
  }

  /**
   * Creates a new NucleusROI with specified coordinates and dimensions.
   *
   * @param x the x-coordinate of the nucleus
   * @param y the y-coordinate of the nucleus
   * @param width the width of the nucleus
   * @param height the height of the nucleus
   * @param imageFileName the filename of the image this ROI belongs to
   * @param nucleusName the name/identifier for this nucleus
   */
  public NucleusROI(int x, int y, int width, int height, String imageFileName, String nucleusName) {
    super(ROIType.COMPLEX_SHAPE, new Rectangle(x, y, width, height), imageFileName, nucleusName);
    setDisplayColor(DEFAULT_NUCLEUS_COLOR);
    this.segmentationMethod = "StarDist";
    this.isValid = true;
    calculateMorphologicalFeatures();
  }

  /**
   * Calculates morphological features for this nucleus.
   * This includes area, perimeter, circularity, aspect ratio, and solidity.
   */
  private void calculateMorphologicalFeatures() {
    if (getImageJRoi() != null) {
      // Get statistics from the ROI
      ij.process.ImageStatistics stats = getImageJRoi().getStatistics();
      this.area = stats.area;

      // Calculate additional morphological features
      Rectangle bounds = getImageJRoi().getBounds();
      this.aspectRatio = (double) bounds.width / bounds.height;

      // Calculate centroid
      this.centroidX = bounds.x + bounds.width / 2.0;
      this.centroidY = bounds.y + bounds.height / 2.0;

      // Estimate perimeter and circularity
      this.perimeter = getImageJRoi().getLength();
      if (perimeter > 0) {
        this.circularity = 4.0 * Math.PI * area / (perimeter * perimeter);
      } else {
        this.circularity = 0.0;
      }

      // Estimate solidity (area / convex hull area)
      this.solidity = calculateSolidity();
    } else {
      // Use bounds for basic calculations
      Rectangle bounds = getBounds();
      this.area = bounds.width * bounds.height;
      this.perimeter = 2 * (bounds.width + bounds.height);
      this.aspectRatio = (double) bounds.width / bounds.height;
      this.centroidX = bounds.x + bounds.width / 2.0;
      this.centroidY = bounds.y + bounds.height / 2.0;
      this.circularity = 4.0 * Math.PI * area / (perimeter * perimeter);
      this.solidity = 1.0; // Rectangle has solidity of 1.0
    }
  }

  /**
   * Calculates the solidity of the nucleus (area / convex hull area).
   *
   * @return the solidity value (0.0 to 1.0)
   */
  private double calculateSolidity() {
    if (getImageJRoi() != null) {
      try {
        java.awt.Polygon convexHull = getImageJRoi().getConvexHull();
        if (convexHull != null) {
          // Create a PolygonRoi from the convex hull polygon
          ij.gui.PolygonRoi convexRoi = new ij.gui.PolygonRoi(convexHull, Roi.POLYGON);
          double convexArea = convexRoi.getStatistics().area;
          return convexArea > 0 ? area / convexArea : 1.0;
        }
      } catch (Exception e) {
        // If convex hull calculation fails, return a default value
        return 0.8; // Typical solidity for nuclei
      }
    }
    return 1.0;
  }

  /**
   * Gets the area of the nucleus in pixels.
   *
   * @return the nucleus area
   */
  public double getNucleusArea() {
    return area;
  }

  /**
   * Gets the perimeter of the nucleus in pixels.
   *
   * @return the nucleus perimeter
   */
  public double getPerimeter() {
    return perimeter;
  }

  /**
   * Gets the circularity of the nucleus (4π × area / perimeter²).
   *
   * @return the circularity value (0.0 to 1.0, where 1.0 is a perfect circle)
   */
  public double getCircularity() {
    return circularity;
  }

  /**
   * Gets the aspect ratio of the nucleus (width / height).
   *
   * @return the aspect ratio
   */
  public double getAspectRatio() {
    return aspectRatio;
  }

  /**
   * Gets the solidity of the nucleus (area / convex hull area).
   *
   * @return the solidity value (0.0 to 1.0)
   */
  public double getSolidity() {
    return solidity;
  }

  /**
   * Gets the X coordinate of the nucleus centroid.
   *
   * @return the centroid X coordinate
   */
  public double getCentroidX() {
    return centroidX;
  }

  /**
   * Gets the Y coordinate of the nucleus centroid.
   *
   * @return the centroid Y coordinate
   */
  public double getCentroidY() {
    return centroidY;
  }

  /**
   * Checks if this nucleus is considered valid based on morphological criteria.
   *
   * @return true if the nucleus is valid, false otherwise
   */
  public boolean isValid() {
    return isValid;
  }

  /**
   * Sets the validity status of this nucleus.
   *
   * @param valid true if the nucleus is valid, false otherwise
   */
  public void setValid(boolean valid) {
    this.isValid = valid;
  }

  /**
   * Gets the segmentation method used to detect this nucleus.
   *
   * @return the segmentation method name
   */
  public String getSegmentationMethod() {
    return segmentationMethod;
  }

  /**
   * Sets the segmentation method used to detect this nucleus.
   *
   * @param method the segmentation method name
   */
  public void setSegmentationMethod(String method) {
    this.segmentationMethod = method;
  }

  /**
   * Gets the parent cell that contains this nucleus.
   *
   * @return the parent cell ROI, or null if not set
   */
  public CellROI getParentCell() {
    return parentCell;
  }

  /**
   * Sets the parent cell that contains this nucleus.
   *
   * @param parentCell the parent cell ROI
   */
  public void setParentCell(CellROI parentCell) {
    this.parentCell = parentCell;
  }

  /**
   * Gets the associated cytoplasm ROI.
   *
   * @return the cytoplasm ROI, or null if not set
   */
  public CytoplasmROI getAssociatedCytoplasm() {
    return associatedCytoplasm;
  }

  /**
   * Sets the associated cytoplasm ROI.
   *
   * @param cytoplasm the cytoplasm ROI
   */
  public void setAssociatedCytoplasm(CytoplasmROI cytoplasm) {
    this.associatedCytoplasm = cytoplasm;
  }

  /**
   * Gets the center coordinates of the nucleus as an integer array.
   *
   * @return array with [x, y] coordinates of the nucleus center
   */
  public int[] getNucleusCenter() {
    return new int[] {(int) Math.round(centroidX), (int) Math.round(centroidY)};
  }

  /**
   * Checks if this nucleus is part of a complete cell.
   *
   * @return true if both parent cell and cytoplasm are set
   */
  public boolean isPartOfCompleteCell() {
    return parentCell != null && associatedCytoplasm != null;
  }

  /**
   * Gets the nucleus number from the name.
   * Extracts the number from names like "Nucleus_5" -> 5.
   *
   * @return the nucleus number, or -1 if not found
   */
  public int getNucleusNumber() {
    try {
      String name = getName();
      if (name != null && name.contains("_")) {
        String[] parts = name.split("_");
        if (parts.length > 1) {
          return Integer.parseInt(parts[1]);
        }
      }
    } catch (NumberFormatException e) {
      // Return -1 if parsing fails
    }
    return -1;
  }

  /**
   * Gets a summary of the nucleus morphological features.
   *
   * @return a formatted string with nucleus measurements
   */
  public String getMorphologySummary() {
    return String.format(
        "Area: %.1f px, Perimeter: %.1f px, Circularity: %.3f, Aspect Ratio: %.2f, Solidity: %.3f",
        area, perimeter, circularity, aspectRatio, solidity);
  }

  @Override
  public String toString() {
    return String.format(
        "NucleusROI[%s] - %s - Valid: %s - Method: %s",
        getName(), getMorphologySummary(), isValid, segmentationMethod);
  }
}
