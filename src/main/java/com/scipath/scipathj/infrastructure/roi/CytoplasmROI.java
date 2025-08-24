package com.scipath.scipathj.infrastructure.roi;

import ij.gui.Roi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a cytoplasm ROI that is created by subtracting the nucleus from the cell.
 * This class maintains the relationship between cytoplasm, nucleus, and parent cell.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class CytoplasmROI extends UserROI {

  private static final Logger LOGGER = LoggerFactory.getLogger(CytoplasmROI.class);

  private NucleusROI associatedNucleus;
  private CellROI parentCell;
  private String segmentationMethod;
  private double cytoplasmArea;
  private double meanIntensity;
  private double stdIntensity;

  /**
   * Constructor for CytoplasmROI.
   *
   * @param roi the ImageJ ROI representing the cytoplasm region
   * @param imageFileName the filename of the source image
   * @param cytoplasmName the name identifier for this cytoplasm
   */
  public CytoplasmROI(Roi roi, String imageFileName, String cytoplasmName) {
    super(roi, imageFileName, cytoplasmName);
    this.cytoplasmArea = roi.getStatistics().area;
    this.segmentationMethod = "Voronoi_Subtraction";
  }

  /**
   * Constructor with associated nucleus.
   *
   * @param roi the ImageJ ROI representing the cytoplasm region
   * @param imageFileName the filename of the source image
   * @param cytoplasmName the name identifier for this cytoplasm
   * @param associatedNucleus the nucleus ROI that this cytoplasm surrounds
   */
  public CytoplasmROI(
      Roi roi, String imageFileName, String cytoplasmName, NucleusROI associatedNucleus) {
    this(roi, imageFileName, cytoplasmName);
    this.associatedNucleus = associatedNucleus;

    if (associatedNucleus != null) {
      // Extract nucleus number from name for linking
      String nucleusName = associatedNucleus.getName();
      setNotes(
          String.format(
              "Cytoplasm region for %s. Created by subtraction from Voronoi cell.", nucleusName));
    }
  }

  /**
   * Sets the associated nucleus for this cytoplasm.
   *
   * @param nucleus the nucleus ROI that this cytoplasm surrounds
   */
  public void setAssociatedNucleus(NucleusROI nucleus) {
    this.associatedNucleus = nucleus;
    if (nucleus != null) {
      String nucleusName = nucleus.getName();
      setNotes(
          String.format(
              "Cytoplasm region for %s. Created by subtraction from Voronoi cell.", nucleusName));
    }
  }

  /**
   * Gets the associated nucleus ROI.
   *
   * @return the nucleus ROI, or null if not set
   */
  public NucleusROI getAssociatedNucleus() {
    return associatedNucleus;
  }

  /**
   * Sets the parent cell that contains this cytoplasm.
   *
   * @param parentCell the parent cell ROI
   */
  public void setParentCell(CellROI parentCell) {
    this.parentCell = parentCell;
  }

  /**
   * Gets the parent cell that contains this cytoplasm.
   *
   * @return the parent cell ROI, or null if not set
   */
  public CellROI getParentCell() {
    return parentCell;
  }

  /**
   * Gets the segmentation method used to create this cytoplasm.
   *
   * @return the segmentation method
   */
  public String getSegmentationMethod() {
    return segmentationMethod;
  }

  /**
   * Sets the segmentation method used to create this cytoplasm.
   *
   * @param segmentationMethod the segmentation method
   */
  public void setSegmentationMethod(String segmentationMethod) {
    this.segmentationMethod = segmentationMethod;
  }

  /**
   * Gets the cytoplasm area in pixels.
   *
   * @return the cytoplasm area
   */
  public double getCytoplasmArea() {
    return cytoplasmArea;
  }

  /**
   * Gets the mean intensity of the cytoplasm region.
   *
   * @return the mean intensity, or 0.0 if not calculated
   */
  public double getMeanIntensity() {
    return meanIntensity;
  }

  /**
   * Sets the mean intensity of the cytoplasm region.
   *
   * @param meanIntensity the mean intensity value
   */
  public void setMeanIntensity(double meanIntensity) {
    this.meanIntensity = meanIntensity;
  }

  /**
   * Gets the standard deviation of intensity in the cytoplasm region.
   *
   * @return the standard deviation of intensity, or 0.0 if not calculated
   */
  public double getStdIntensity() {
    return stdIntensity;
  }

  /**
   * Sets the standard deviation of intensity in the cytoplasm region.
   *
   * @param stdIntensity the standard deviation value
   */
  public void setStdIntensity(double stdIntensity) {
    this.stdIntensity = stdIntensity;
  }

  /**
   * Gets the nucleus number associated with this cytoplasm.
   * Extracts the number from the cytoplasm name (e.g., "Cytoplasm_5" -> 5).
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
      LOGGER.debug("Could not parse nucleus number from cytoplasm name: {}", getName());
    }
    return -1;
  }

  /**
   * Checks if this cytoplasm is linked to a nucleus.
   *
   * @return true if associated nucleus is set
   */
  public boolean hasAssociatedNucleus() {
    return associatedNucleus != null;
  }

  /**
   * Checks if this cytoplasm is part of a complete cell.
   *
   * @return true if parent cell is set
   */
  public boolean isPartOfCell() {
    return parentCell != null;
  }

  /**
   * Gets the distance from cytoplasm centroid to nucleus center.
   *
   * @return distance in pixels, or -1.0 if nucleus is not available
   */
  public double getDistanceToNucleus() {
    if (associatedNucleus == null) {
      return -1.0;
    }

    // Get cytoplasm centroid
    int[] cytoplasmCenter = {getX() + getWidth() / 2, getY() + getHeight() / 2};

    // Get nucleus center
    int[] nucleusCenter = associatedNucleus.getNucleusCenter();

    // Calculate Euclidean distance
    double dx = cytoplasmCenter[0] - nucleusCenter[0];
    double dy = cytoplasmCenter[1] - nucleusCenter[1];

    return Math.sqrt(dx * dx + dy * dy);
  }

  /**
   * Gets detailed information about this cytoplasm.
   *
   * @return formatted information string
   */
  public String getCytoplasmInfo() {
    StringBuilder info = new StringBuilder();
    info.append(String.format("Cytoplasm: %s\n", getName()));
    info.append(String.format("Area: %.1f pixels\n", cytoplasmArea));
    info.append(String.format("Segmentation: %s\n", segmentationMethod));

    if (meanIntensity > 0) {
      info.append(String.format("Mean Intensity: %.2f ± %.2f\n", meanIntensity, stdIntensity));
    }

    if (associatedNucleus != null) {
      info.append(String.format("Associated Nucleus: %s\n", associatedNucleus.getName()));
      double distance = getDistanceToNucleus();
      if (distance >= 0) {
        info.append(String.format("Distance to Nucleus: %.1f pixels\n", distance));
      }
    }

    if (parentCell != null) {
      info.append(String.format("Parent Cell: %s\n", parentCell.getName()));
    }

    return info.toString();
  }

  @Override
  public ROIType getType() {
    return ROIType.CYTOPLASM;
  }

  @Override
  public String toString() {
    return String.format(
        "CytoplasmROI[%s: area=%.1f, nucleus=%s, cell=%s]",
        getName(),
        cytoplasmArea,
        associatedNucleus != null ? associatedNucleus.getName() : "none",
        parentCell != null ? parentCell.getName() : "none");
  }
}
