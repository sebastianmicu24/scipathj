package com.scipath.scipathj.core.analysis;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.config.CytoplasmSegmentationSettings;
import com.scipath.scipathj.core.config.MainSettings;
import com.scipath.scipathj.data.model.CellROI;
import com.scipath.scipathj.data.model.CytoplasmROI;
import com.scipath.scipathj.data.model.NucleusROI;
import com.scipath.scipathj.data.model.UserROI;
import com.scipath.scipathj.ui.components.ROIManager;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.ImageCalculator;
import ij.process.ByteProcessor;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Step 3 of the analysis pipeline: Cytoplasm Segmentation.
 *
 * Implements Voronoi tessellation-based cytoplasm segmentation using nucleus centers as seeds.
 * Creates cell ROIs using Voronoi tessellation, then creates cytoplasm ROIs by subtracting
 * the nucleus from each cell. Links nuclei to their corresponding cytoplasm regions.
 *
 * Based on the SCHELI plugin approach for robust cell segmentation.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class CytoplasmSegmentation {

  private static final Logger LOGGER = LoggerFactory.getLogger(CytoplasmSegmentation.class);

  private final ImagePlus originalImage;
  private final String imageFileName;
  private final List<UserROI> vesselROIs;
  private final List<NucleusROI> nucleusROIs;
  private final CytoplasmSegmentationSettings settings;
  private final MainSettings mainSettings;
  private final ROIManager roiManager;

  private List<CellROI> cellROIs;
  private List<CytoplasmROI> cytoplasmROIs;
  private ImagePlus backgroundMask;

  /**
   * Constructor for CytoplasmSegmentation with default settings.
   *
   * @param configurationManager The configuration manager instance
   * @param originalImage The original image to segment
   * @param imageFileName The filename of the image for ROI association
   * @param vesselROIs Previously detected vessel ROIs
   * @param nucleusROIs Previously detected nucleus ROIs
   */
  public CytoplasmSegmentation(
      ConfigurationManager configurationManager,
      ImagePlus originalImage,
      String imageFileName,
      List<UserROI> vesselROIs,
      List<NucleusROI> nucleusROIs) {
    this.originalImage = originalImage;
    this.imageFileName = imageFileName;
    this.vesselROIs = vesselROIs != null ? vesselROIs : new ArrayList<>();
    this.nucleusROIs = nucleusROIs != null ? nucleusROIs : new ArrayList<>();
    this.settings = configurationManager.initializeCytoplasmSegmentationSettings();
    this.mainSettings = MainSettings.getInstance();
    this.roiManager = ROIManager.getInstance();
    this.cellROIs = new ArrayList<>();
    this.cytoplasmROIs = new ArrayList<>();

    // Create background mask from vessels if vessel exclusion is enabled
    if (settings.isUseVesselExclusion() && !this.vesselROIs.isEmpty()) {
      this.backgroundMask = createBackgroundMaskFromVessels();
    }

    LOGGER.debug(
        "CytoplasmSegmentation initialized for image: {} with {} nuclei and {} vessels",
        imageFileName,
        this.nucleusROIs.size(),
        this.vesselROIs.size());
  }

  /**
   * Constructor with custom settings.
   *
   * @param configurationManager The configuration manager instance
   * @param originalImage The original image to segment
   * @param imageFileName The filename of the image for ROI association
   * @param vesselROIs Previously detected vessel ROIs
   * @param nucleusROIs Previously detected nucleus ROIs
   * @param settings Custom cytoplasm segmentation settings
   */
  public CytoplasmSegmentation(
      ConfigurationManager configurationManager,
      ImagePlus originalImage,
      String imageFileName,
      List<UserROI> vesselROIs,
      List<NucleusROI> nucleusROIs,
      CytoplasmSegmentationSettings settings) {
    this.originalImage = originalImage;
    this.imageFileName = imageFileName;
    this.vesselROIs = vesselROIs != null ? vesselROIs : new ArrayList<>();
    this.nucleusROIs = nucleusROIs != null ? nucleusROIs : new ArrayList<>();
    this.settings =
        settings != null
            ? settings
            : configurationManager.initializeCytoplasmSegmentationSettings();
    this.mainSettings = MainSettings.getInstance();
    this.roiManager = ROIManager.getInstance();
    this.cellROIs = new ArrayList<>();
    this.cytoplasmROIs = new ArrayList<>();

    // Create background mask from vessels if vessel exclusion is enabled
    if (this.settings.isUseVesselExclusion() && !this.vesselROIs.isEmpty()) {
      this.backgroundMask = createBackgroundMaskFromVessels();
    }

    LOGGER.debug(
        "CytoplasmSegmentation initialized for image: {} with custom settings", imageFileName);
  }

  /**
   * Perform cytoplasm segmentation using Voronoi tessellation.
   *
   * This method:
   * 1. Creates a binary mask of nuclei
   * 2. Applies Voronoi tessellation using nucleus centers as seeds
   * 3. Excludes vessel regions if enabled
   * 4. Creates cell ROIs from Voronoi regions
   * 5. Creates cytoplasm ROIs by subtracting nuclei from cells
   * 6. Links nuclei to their corresponding cytoplasm regions
   *
   * @return List of cytoplasm ROIs
   */
  public List<CytoplasmROI> segmentCytoplasm() {
    LOGGER.info(
        "Starting cytoplasm segmentation using Voronoi tessellation for image: {}", imageFileName);

    if (nucleusROIs == null || nucleusROIs.isEmpty()) {
      LOGGER.warn("No nucleus ROIs found. Cannot create cytoplasm ROIs.");
      return new ArrayList<>();
    }

    // Clear any existing ROIs
    cellROIs.clear();
    cytoplasmROIs.clear();

    try {
      // Get image dimensions
      int imageWidth = originalImage.getWidth();
      int imageHeight = originalImage.getHeight();

      LOGGER.debug("Processing image dimensions: {}x{}", imageWidth, imageHeight);

      // Step 1: Create a binary mask of nuclei
      ImagePlus nucleiMask = createNucleiMask();
      nucleiMask.show();
      moveWindowOffScreen(nucleiMask);

      // Step 2: Create Voronoi tessellation
      ImagePlus voronoiImage = createVoronoiTessellation(nucleiMask);

      // Step 3: Apply vessel exclusion if enabled
      ImagePlus cytoplasmImage = applyVesselExclusion(voronoiImage);

      // Step 4: Process each nucleus to create cell and cytoplasm ROIs
      processNucleiForCellCreation(cytoplasmImage, imageWidth, imageHeight);

      // Step 5: Link nuclei to cytoplasm and add to ROI manager
      linkNucleiToCytoplasm();

      // Clean up temporary images
      cleanupTemporaryImages(nucleiMask, voronoiImage, cytoplasmImage);

      LOGGER.info(
          "Cytoplasm segmentation completed. Created {} cells and {} cytoplasm ROIs",
          cellROIs.size(),
          cytoplasmROIs.size());

    } catch (Exception e) {
      LOGGER.error("Error during cytoplasm segmentation for image: {}", imageFileName, e);
      throw new RuntimeException("Cytoplasm segmentation failed: " + e.getMessage(), e);
    }

    return cytoplasmROIs;
  }

  /**
   * Creates a binary mask from nucleus ROIs.
   */
  private ImagePlus createNucleiMask() {
    int width = originalImage.getWidth();
    int height = originalImage.getHeight();

    ByteProcessor bp = new ByteProcessor(width, height);
    bp.setValue(0); // Background black
    bp.fill();

    // Draw nuclei as white
    bp.setValue(255);
    for (NucleusROI nucleusROI : nucleusROIs) {
      Roi roi = nucleusROI.getImageJRoi();
      if (roi != null) {
        bp.fill(roi);
      }
    }

    return new ImagePlus("Nuclei_Mask", bp);
  }

  /**
   * Creates Voronoi tessellation from the nuclei mask.
   */
  private ImagePlus createVoronoiTessellation(ImagePlus nucleiMask) {
    // Create duplicate for Voronoi processing
    ImagePlus voronoiImage = nucleiMask.duplicate();
    voronoiImage.setTitle("Voronoi_" + System.currentTimeMillis());
    voronoiImage.show();
    moveWindowOffScreen(voronoiImage);

    // Ensure binary format
    IJ.setThreshold(voronoiImage, 1, 255);
    IJ.run(voronoiImage, "Convert to Mask", "");

    // Apply Voronoi tessellation
    if (settings.isApplyVoronoi()) {
      IJ.run(voronoiImage, "Voronoi", "");

      // Threshold to make borders black
      if (voronoiImage.getBitDepth() != 8) {
        IJ.run(voronoiImage, "8-bit", "");
      }
      IJ.setThreshold(voronoiImage, 1, 255);
      IJ.run(voronoiImage, "Convert to Mask", "");
    }

    // Add image border if enabled
    if (settings.isAddImageBorder()) {
      addImageBorder(voronoiImage);
    }

    return voronoiImage;
  }

  /**
   * Adds a border around the image to prevent edge selection issues.
   */
  private void addImageBorder(ImagePlus image) {
    int width = image.getWidth();
    int height = image.getHeight();
    int borderWidth = settings.getBorderWidth();

    IJ.setForegroundColor(255, 255, 255); // White foreground

    // Add borders
    image.setRoi(0, 0, borderWidth, height); // Left
    IJ.run(image, "Fill", "slice");

    image.setRoi(width - borderWidth, 0, borderWidth, height); // Right
    IJ.run(image, "Fill", "slice");

    image.setRoi(0, 0, width, borderWidth); // Top
    IJ.run(image, "Fill", "slice");

    image.setRoi(0, height - borderWidth, width, borderWidth); // Bottom
    IJ.run(image, "Fill", "slice");

    image.deleteRoi();
    image.updateAndDraw();
  }

  /**
   * Applies vessel exclusion to the Voronoi image if enabled.
   */
  private ImagePlus applyVesselExclusion(ImagePlus voronoiImage) {
    if (settings.isUseVesselExclusion() && backgroundMask != null) {
      ImageCalculator ic = new ImageCalculator();
      ImagePlus result = ic.run("Max create", voronoiImage, backgroundMask);
      result.setTitle("Cytoplasm_" + System.currentTimeMillis());
      result.show();
      moveWindowOffScreen(result);
      return result;
    }
    return voronoiImage;
  }

  /**
   * Creates a background mask from vessel ROIs.
   */
  private ImagePlus createBackgroundMaskFromVessels() {
    if (vesselROIs == null || vesselROIs.isEmpty()) {
      LOGGER.debug("No vessel ROIs provided for background mask creation");
      return null;
    }

    int width = originalImage.getWidth();
    int height = originalImage.getHeight();

    ByteProcessor bp = new ByteProcessor(width, height);
    bp.setValue(0); // Background black
    bp.fill();

    // Draw vessels as white
    bp.setValue(255);
    for (UserROI vesselROI : vesselROIs) {
      Roi roi = vesselROI.getImageJRoi();
      if (roi != null) {
        bp.fill(roi);
      }
    }

    LOGGER.debug("Created background mask from {} vessel ROIs", vesselROIs.size());
    return new ImagePlus("Vessel_Mask", bp);
  }

  /**
   * Processes each nucleus to create cell and cytoplasm ROIs.
   */
  private void processNucleiForCellCreation(
      ImagePlus cytoplasmImage, int imageWidth, int imageHeight) {
    for (int i = 0; i < nucleusROIs.size(); i++) {
      NucleusROI nucleusROI = nucleusROIs.get(i);
      int nucleusNumber = nucleusROI.getNucleusNumber();
      if (nucleusNumber == -1) {
        nucleusNumber = i + 1;
      }

      // Get nucleus center
      int[] center = nucleusROI.getNucleusCenter();
      int x = center[0];
      int y = center[1];

      // Adjust center if too close to borders
      x = Math.max(2, Math.min(imageWidth - 3, x));
      y = Math.max(2, Math.min(imageHeight - 3, y));

      // Use doWand to select the cell region
      cytoplasmImage.setActivated();
      IJ.doWand(x, y);
      Roi cellRoi = cytoplasmImage.getRoi();

      if (cellRoi != null && isValidCell(cellRoi)) {
        // Create cell ROI
        CellROI cell = createCellROI(cellRoi, nucleusROI, nucleusNumber);
        cellROIs.add(cell);

        // Create cytoplasm ROI by subtracting nucleus from cell
        CytoplasmROI cytoplasm = createCytoplasmROI(cellRoi, nucleusROI, nucleusNumber);
        if (cytoplasm != null && isValidCytoplasm(cytoplasm)) {
          cytoplasmROIs.add(cytoplasm);

          // Link the components
          cell.setAssociatedNucleus(nucleusROI);
          cell.setAssociatedCytoplasm(cytoplasm);
          cytoplasm.setAssociatedNucleus(nucleusROI);
          cytoplasm.setParentCell(cell);
        }
      } else {
        LOGGER.debug("Could not create valid cell ROI for nucleus {}", nucleusNumber);
      }
    }
  }

  /**
   * Creates a cell ROI from the Voronoi region.
   */
  private CellROI createCellROI(Roi cellRoi, NucleusROI nucleusROI, int nucleusNumber) {
    Roi cellRoiCopy = (Roi) cellRoi.clone();
    String cellName = "Cell_" + nucleusNumber;

    CellROI cell = new CellROI(cellRoiCopy, imageFileName, cellName, nucleusROI);
    cell.setDisplayColor(mainSettings.getCellSettings().getBorderColor());
    cell.setSegmentationMethod("Voronoi_Tessellation");

    return cell;
  }

  /**
   * Creates a cytoplasm ROI by subtracting the nucleus from the cell.
   */
  private CytoplasmROI createCytoplasmROI(Roi cellRoi, NucleusROI nucleusROI, int nucleusNumber) {
    try {
      // Create ShapeRoi objects for subtraction operation
      ShapeRoi cellShape = new ShapeRoi(cellRoi);
      ShapeRoi nucleusShape = new ShapeRoi(nucleusROI.getImageJRoi());

      // Subtraction operation: cell - nucleus = cytoplasm (creates donut shape)
      ShapeRoi cytoplasmShape = cellShape.not(nucleusShape);

      if (cytoplasmShape != null) {
        String cytoplasmName = "Cytoplasm_" + nucleusNumber;
        CytoplasmROI cytoplasm =
            new CytoplasmROI(cytoplasmShape, imageFileName, cytoplasmName, nucleusROI);
        cytoplasm.setDisplayColor(mainSettings.getCytoplasmSettings().getBorderColor());
        cytoplasm.setSegmentationMethod("Voronoi_Subtraction");

        return cytoplasm;
      }
    } catch (Exception e) {
      LOGGER.warn(
          "Failed to create cytoplasm ROI for nucleus {}: {}", nucleusNumber, e.getMessage());
    }

    return null;
  }

  /**
   * Validates if a cell ROI meets the criteria.
   */
  private boolean isValidCell(Roi cellRoi) {
    if (cellRoi == null) return false;

    double area = cellRoi.getStatistics().area;
    if (area < settings.getMinCellSize() || area > settings.getMaxCellSize()) {
      return false;
    }

    if (settings.isValidateCellShape()) {
      Rectangle bounds = cellRoi.getBounds();
      double aspectRatio =
          (double) Math.max(bounds.width, bounds.height) / Math.min(bounds.width, bounds.height);
      if (aspectRatio > settings.getMaxAspectRatio()) {
        return false;
      }
    }

    return true;
  }

  /**
   * Validates if a cytoplasm ROI meets the criteria.
   */
  private boolean isValidCytoplasm(CytoplasmROI cytoplasm) {
    if (cytoplasm == null) return false;

    double area = cytoplasm.getCytoplasmArea();
    return area >= settings.getMinCytoplasmSize();
  }

  /**
   * Links nuclei to their cytoplasm and adds ROIs to the manager.
   */
  private void linkNucleiToCytoplasm() {
    if (settings.isLinkNucleusToCytoplasm()) {
      for (CytoplasmROI cytoplasm : cytoplasmROIs) {
        NucleusROI nucleus = cytoplasm.getAssociatedNucleus();
        if (nucleus != null) {
          nucleus.setAssociatedCytoplasm(cytoplasm);
        }
      }
    }

    // Add ROIs to manager
    if (settings.isCreateCellROIs()) {
      for (CellROI cell : cellROIs) {
        roiManager.addROI(cell);
      }
    }

    for (CytoplasmROI cytoplasm : cytoplasmROIs) {
      roiManager.addROI(cytoplasm);
    }
  }

  /**
   * Moves a window off-screen to hide it.
   */
  private void moveWindowOffScreen(ImagePlus image) {
    if (image.getWindow() != null) {
      image.getWindow().setLocation(-2000, -2000);
    }
  }

  /**
   * Cleans up temporary images.
   */
  private void cleanupTemporaryImages(ImagePlus... images) {
    for (ImagePlus image : images) {
      if (image != null) {
        image.changes = false;
        image.close();
      }
    }
  }

  /**
   * Gets the created cell ROIs.
   *
   * @return list of cell ROIs
   */
  public List<CellROI> getCellROIs() {
    return new ArrayList<>(cellROIs);
  }

  /**
   * Gets the created cytoplasm ROIs.
   *
   * @return list of cytoplasm ROIs
   */
  public List<CytoplasmROI> getCytoplasmROIs() {
    return new ArrayList<>(cytoplasmROIs);
  }

  /**
   * Get statistics about the cytoplasm segmentation results.
   *
   * @param cytoplasmROIs list of detected cytoplasm ROIs
   * @return formatted statistics string
   */
  public String getStatistics(List<CytoplasmROI> cytoplasmROIs) {
    if (cytoplasmROIs.isEmpty()) {
      return "No cytoplasm regions detected";
    }

    double totalArea = cytoplasmROIs.stream().mapToDouble(CytoplasmROI::getCytoplasmArea).sum();

    double avgArea = totalArea / cytoplasmROIs.size();

    double minArea =
        cytoplasmROIs.stream().mapToDouble(CytoplasmROI::getCytoplasmArea).min().orElse(0.0);

    double maxArea =
        cytoplasmROIs.stream().mapToDouble(CytoplasmROI::getCytoplasmArea).max().orElse(0.0);

    long linkedCount =
        cytoplasmROIs.stream().mapToLong(c -> c.hasAssociatedNucleus() ? 1 : 0).sum();

    return String.format(
        "Cytoplasm: %d regions, Total area: %.1f px, Avg area: %.1f px (range: %.1f-%.1f), Linked:"
            + " %d",
        cytoplasmROIs.size(), totalArea, avgArea, minArea, maxArea, linkedCount);
  }

  /**
   * Gets the current settings.
   *
   * @return the cytoplasm segmentation settings
   */
  public CytoplasmSegmentationSettings getSettings() {
    return settings;
  }
}
