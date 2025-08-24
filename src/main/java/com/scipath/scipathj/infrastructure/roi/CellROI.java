package com.scipath.scipathj.infrastructure.roi;

import ij.gui.Roi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a complete cell ROI that encompasses both nucleus and cytoplasm.
 * This class links a nucleus to its corresponding cytoplasm region.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class CellROI extends UserROI {

  private static final Logger LOGGER = LoggerFactory.getLogger(CellROI.class);

  private NucleusROI associatedNucleus;
  private CytoplasmROI associatedCytoplasm;
  private String segmentationMethod;
  private double cellArea;
  private double nucleusToCytoplasmRatio;

  /**
   * Constructor for CellROI.
   *
   * @param roi the ImageJ ROI representing the complete cell boundary
   * @param imageFileName the filename of the source image
   * @param cellName the name identifier for this cell
   */
  public CellROI(Roi roi, String imageFileName, String cellName) {
    super(roi, imageFileName, cellName);
    this.cellArea = roi.getStatistics().area;
    this.segmentationMethod = "Voronoi_Tessellation";
  }

  /**
   * Constructor with associated nucleus.
   *
   * @param roi the ImageJ ROI representing the complete cell boundary
   * @param imageFileName the filename of the source image
   * @param cellName the name identifier for this cell
   * @param associatedNucleus the nucleus ROI contained within this cell
   */
  public CellROI(Roi roi, String imageFileName, String cellName, NucleusROI associatedNucleus) {
    this(roi, imageFileName, cellName);
    this.associatedNucleus = associatedNucleus;

    if (associatedNucleus != null) {
      // Link the nucleus back to this cell
      associatedNucleus.setParentCell(this);
      calculateNucleusToCytoplasmRatio();
    }
  }

  /**
   * Sets the associated nucleus for this cell.
   *
   * @param nucleus the nucleus ROI contained within this cell
   */
  public void setAssociatedNucleus(NucleusROI nucleus) {
    this.associatedNucleus = nucleus;
    if (nucleus != null) {
      nucleus.setParentCell(this);
      calculateNucleusToCytoplasmRatio();
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
   * Sets the associated cytoplasm for this cell.
   *
   * @param cytoplasm the cytoplasm ROI contained within this cell
   */
  public void setAssociatedCytoplasm(CytoplasmROI cytoplasm) {
    this.associatedCytoplasm = cytoplasm;
    if (cytoplasm != null) {
      cytoplasm.setParentCell(this);
      calculateNucleusToCytoplasmRatio();
    }
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
   * Gets the segmentation method used to create this cell.
   *
   * @return the segmentation method
   */
  public String getSegmentationMethod() {
    return segmentationMethod;
  }

  /**
   * Sets the segmentation method used to create this cell.
   *
   * @param segmentationMethod the segmentation method
   */
  public void setSegmentationMethod(String segmentationMethod) {
    this.segmentationMethod = segmentationMethod;
  }

  /**
   * Gets the total cell area in pixels.
   *
   * @return the cell area
   */
  public double getCellArea() {
    return cellArea;
  }

  /**
   * Gets the nucleus to cytoplasm area ratio.
   *
   * @return the ratio, or 0.0 if components are not available
   */
  public double getNucleusToCytoplasmRatio() {
    return nucleusToCytoplasmRatio;
  }

  /**
   * Calculates the nucleus to cytoplasm area ratio.
   */
  private void calculateNucleusToCytoplasmRatio() {
    if (associatedNucleus != null && associatedCytoplasm != null) {
      double nucleusArea = associatedNucleus.getNucleusArea();
      double cytoplasmArea = associatedCytoplasm.getCytoplasmArea();

      if (cytoplasmArea > 0) {
        this.nucleusToCytoplasmRatio = nucleusArea / cytoplasmArea;
      } else {
        this.nucleusToCytoplasmRatio = 0.0;
      }
    }
  }

  /**
   * Checks if this cell has both nucleus and cytoplasm components.
   *
   * @return true if both components are present
   */
  public boolean isComplete() {
    return associatedNucleus != null && associatedCytoplasm != null;
  }

  /**
   * Gets the center coordinates of the cell (based on nucleus center).
   *
   * @return array with [x, y] coordinates, or null if nucleus is not available
   */
  public int[] getCellCenter() {
    if (associatedNucleus != null) {
      return associatedNucleus.getNucleusCenter();
    }

    // Fallback to ROI bounds center
    return new int[] {getX() + getWidth() / 2, getY() + getHeight() / 2};
  }

  /**
   * Gets detailed information about this cell.
   *
   * @return formatted information string
   */
  public String getCellInfo() {
    StringBuilder info = new StringBuilder();
    info.append(String.format("Cell: %s\n", getName()));
    info.append(String.format("Total Area: %.1f pixels\n", cellArea));
    info.append(String.format("Segmentation: %s\n", segmentationMethod));

    if (associatedNucleus != null) {
      info.append(String.format("Nucleus Area: %.1f pixels\n", associatedNucleus.getNucleusArea()));
    }

    if (associatedCytoplasm != null) {
      info.append(
          String.format("Cytoplasm Area: %.1f pixels\n", associatedCytoplasm.getCytoplasmArea()));
    }

    if (isComplete()) {
      info.append(String.format("N/C Ratio: %.3f\n", nucleusToCytoplasmRatio));
    }

    return info.toString();
  }

  @Override
  public ROIType getType() {
    return ROIType.CELL;
  }

  @Override
  public String toString() {
    return String.format(
        "CellROI[%s: area=%.1f, complete=%s, N/C=%.3f]",
        getName(), cellArea, isComplete(), nucleusToCytoplasmRatio);
  }
}
