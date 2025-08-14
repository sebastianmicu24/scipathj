package com.scipath.scipathj.core.analysis;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.config.CytoplasmSegmentationSettings;
import com.scipath.scipathj.core.config.MainSettings;
import com.scipath.scipathj.core.config.NuclearSegmentationSettings;
import com.scipath.scipathj.core.config.SegmentationConstants;
import com.scipath.scipathj.core.config.VesselSegmentationSettings;
import com.scipath.scipathj.data.model.CellROI;
import com.scipath.scipathj.data.model.CytoplasmROI;
import com.scipath.scipathj.data.model.NucleusROI;
import com.scipath.scipathj.data.model.UserROI;
import com.scipath.scipathj.ui.components.ROIManager;
import com.scipath.scipathj.ui.utils.ImageLoader;
import ij.ImagePlus;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analysis pipeline coordinator that orchestrates the 6-step analysis workflow:
 * 1. Vessel Segmentation
 * 2. Nuclear Segmentation
 * 3. Cytoplasm Segmentation
 * 4. Feature Extraction (TODO)
 * 5. Cell Classification (TODO)
 * 6. Statistical Analysis (TODO)
 *
 * This class coordinates the workflow but keeps each analysis step semantically distinct.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class AnalysisPipeline {

  private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisPipeline.class);

  private final ConfigurationManager configurationManager;
  private final VesselSegmentationSettings vesselSettings;
  private final NuclearSegmentationSettings nuclearSettings;
  private final CytoplasmSegmentationSettings cytoplasmSettings;
  private final MainSettings mainSettings;
  private final ROIManager roiManager;

  // Progress tracking
  private final AtomicBoolean isProcessing = new AtomicBoolean(false);
  private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
  private final AtomicInteger processedImages = new AtomicInteger(0);
  private volatile int totalImages = 0;

  // Progress callbacks
  private Consumer<String> progressMessageCallback;
  private Consumer<Integer> progressPercentCallback;

  /**
   * Creates a new AnalysisPipeline with default settings.
   *
   * @param configurationManager the configuration manager instance
   */
  public AnalysisPipeline(ConfigurationManager configurationManager) {
    this.configurationManager = configurationManager;
    this.vesselSettings = configurationManager.initializeVesselSegmentationSettings();
    this.nuclearSettings = configurationManager.initializeNuclearSegmentationSettings();
    this.cytoplasmSettings = configurationManager.initializeCytoplasmSegmentationSettings();
    this.mainSettings = MainSettings.getInstance();
    this.roiManager = ROIManager.getInstance();

    LOGGER.debug("AnalysisPipeline initialized with default settings");
  }

  /**
   * Creates a new AnalysisPipeline with custom vessel and nuclear settings.
   * Cytoplasm settings will use defaults.
   *
   * @param vesselSettings custom vessel segmentation settings
   * @param nuclearSettings custom nuclear segmentation settings
   */
  public AnalysisPipeline(
      ConfigurationManager configurationManager,
      VesselSegmentationSettings vesselSettings,
      NuclearSegmentationSettings nuclearSettings) {
    this.configurationManager = configurationManager;
    this.vesselSettings =
        vesselSettings != null
            ? vesselSettings
            : configurationManager.initializeVesselSegmentationSettings();
    this.nuclearSettings =
        nuclearSettings != null
            ? nuclearSettings
            : configurationManager.initializeNuclearSegmentationSettings();
    this.cytoplasmSettings = configurationManager.initializeCytoplasmSegmentationSettings();
    this.mainSettings = MainSettings.getInstance();
    this.roiManager = ROIManager.getInstance();

    LOGGER.debug("AnalysisPipeline initialized with custom vessel and nuclear settings");
  }

  /**
   * Creates a new AnalysisPipeline with custom settings for all segmentation steps.
   *
   * @param vesselSettings custom vessel segmentation settings
   * @param nuclearSettings custom nuclear segmentation settings
   * @param cytoplasmSettings custom cytoplasm segmentation settings
   */
  public AnalysisPipeline(
      ConfigurationManager configurationManager,
      VesselSegmentationSettings vesselSettings,
      NuclearSegmentationSettings nuclearSettings,
      CytoplasmSegmentationSettings cytoplasmSettings) {
    this.configurationManager = configurationManager;
    this.vesselSettings =
        vesselSettings != null
            ? vesselSettings
            : configurationManager.initializeVesselSegmentationSettings();
    this.nuclearSettings =
        nuclearSettings != null
            ? nuclearSettings
            : configurationManager.initializeNuclearSegmentationSettings();
    this.cytoplasmSettings =
        cytoplasmSettings != null
            ? cytoplasmSettings
            : configurationManager.initializeCytoplasmSegmentationSettings();
    this.mainSettings = MainSettings.getInstance();
    this.roiManager = ROIManager.getInstance();

    LOGGER.debug("AnalysisPipeline initialized with custom settings for all steps");
  }

  /**
   * Sets the progress message callback.
   *
   * @param callback callback to receive progress messages
   */
  public void setProgressMessageCallback(Consumer<String> callback) {
    this.progressMessageCallback = callback;
  }

  /**
   * Sets the progress percentage callback.
   *
   * @param callback callback to receive progress percentages (0-100)
   */
  public void setProgressPercentCallback(Consumer<Integer> callback) {
    this.progressPercentCallback = callback;
  }

  /**
   * Processes a batch of images through the complete analysis pipeline.
   * Currently implements steps 1-3 (vessel, nuclear, and cytoplasm segmentation).
   *
   * @param imageFiles array of image files to process
   * @return analysis results containing counts for each step
   * @throws IllegalStateException if pipeline is already processing
   */
  public AnalysisResults processBatch(File[] imageFiles) {
    if (imageFiles == null || imageFiles.length == 0) {
      LOGGER.warn("No image files provided for batch processing");
      return new AnalysisResults(0, 0, 0, 0);
    }

    if (isProcessing.get()) {
      throw new IllegalStateException("Analysis pipeline is already processing");
    }

    LOGGER.info("Starting batch analysis of {} images", imageFiles.length);

    isProcessing.set(true);
    cancelRequested.set(false);
    totalImages = imageFiles.length;
    processedImages.set(0);

    int totalVessels = 0;
    int totalNuclei = 0;
    int totalCells = 0;
    int successfulImages = 0;

    try {
      for (int i = 0; i < imageFiles.length; i++) {
        if (cancelRequested.get()) {
          LOGGER.info("Batch processing cancelled at image {} of {}", i + 1, imageFiles.length);
          break;
        }

        File imageFile = imageFiles[i];
        String fileName = imageFile.getName();

        updateProgress(
            i, "Processing image " + (i + 1) + "/" + imageFiles.length + ": " + fileName);

        try {
          ImageAnalysisResult result = processImage(imageFile);
          if (result.isSuccess()) {
            totalVessels += result.getVesselCount();
            totalNuclei += result.getNucleusCount();
            totalCells += result.getCellCount();
            successfulImages++;

            LOGGER.info(
                "Completed analysis for {} - Vessels: {}, Nuclei: {}, Cells: {}",
                fileName,
                result.getVesselCount(),
                result.getNucleusCount(),
                result.getCellCount());
          } else {
            LOGGER.warn("Analysis failed for image: {}", fileName);
          }

        } catch (Exception e) {
          LOGGER.error("Error processing image: {}", fileName, e);
        }

        processedImages.incrementAndGet();

        // Small delay to allow UI updates
        try {
          Thread.sleep(SegmentationConstants.DEFAULT_BATCH_PROCESSING_DELAY);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }

      updateProgress(
          100,
          String.format(
              "Batch analysis completed! Found %d vessels, %d nuclei, and %d cells across %d"
                  + " images.",
              totalVessels, totalNuclei, totalCells, successfulImages));

      LOGGER.info(
          "Batch analysis completed: {} images processed, {} vessels, {} nuclei, {} cells",
          successfulImages,
          totalVessels,
          totalNuclei,
          totalCells);

    } finally {
      isProcessing.set(false);
      processedImages.set(0);
      totalImages = 0;
    }

    return new AnalysisResults(successfulImages, totalVessels, totalNuclei, totalCells);
  }

  /**
   * Processes a single image through the complete analysis pipeline.
   * Currently implements steps 1-3 (vessel, nuclear, and cytoplasm segmentation).
   *
   * @param imageFile the image file to process
   * @return analysis result for the image
   */
  public ImageAnalysisResult processImage(File imageFile) {
    String fileName = imageFile.getName();
    LOGGER.debug("Processing single image: {}", fileName);

    try {
      // Load the image
      ImagePlus imagePlus = ImageLoader.loadImage(imageFile.getAbsolutePath());
      if (imagePlus == null) {
        LOGGER.warn("Could not load image: {}", fileName);
        return ImageAnalysisResult.failure(fileName, "Failed to load image");
      }

      return processImage(imagePlus, fileName);

    } catch (Exception e) {
      LOGGER.error("Error processing image file: {}", fileName, e);
      return ImageAnalysisResult.failure(fileName, e.getMessage());
    }
  }

  /**
   * Processes a loaded ImagePlus through the complete analysis pipeline.
   * Currently implements steps 1-3 (vessel, nuclear, and cytoplasm segmentation).
   *
   * @param imagePlus the loaded image
   * @param fileName the filename for ROI association
   * @return analysis result for the image
   */
  public ImageAnalysisResult processImage(ImagePlus imagePlus, String fileName) {
    try {
      // Step 1: Vessel Segmentation
      LOGGER.debug("Step 1: Starting vessel segmentation for: {}", fileName);
      VesselSegmentation vesselSegmentation =
          new VesselSegmentation(configurationManager, imagePlus, fileName, vesselSettings);
      List<UserROI> vesselROIs = vesselSegmentation.segmentVessels();
      LOGGER.debug("Step 1 complete: Found {} vessels in image: {}", vesselROIs.size(), fileName);

      // Step 2: Nuclear Segmentation
      LOGGER.debug("Step 2: Starting nuclear segmentation for: {}", fileName);
      SimpleHENuclearSegmentation nuclearSegmentation =
          new SimpleHENuclearSegmentation(
              configurationManager, imagePlus, fileName, nuclearSettings);

      List<NucleusROI> nucleusROIs;
      try {
        LOGGER.info("Step 2: Starting nuclear segmentation for image: {}", fileName);

        LOGGER.info("Checking if nuclear segmentation is available...");
        boolean isAvailable = nuclearSegmentation.isAvailable();
        LOGGER.info("Nuclear segmentation available: {}", isAvailable);

        if (isAvailable) {
          LOGGER.info("✓ StarDist is available - attempting nuclear segmentation...");
          nucleusROIs = nuclearSegmentation.segmentNuclei();
          LOGGER.info(
              "Step 2 complete: Found {} nuclei in image: {}", nucleusROIs.size(), fileName);
        } else {
          LOGGER.error("✗ Step 2: StarDist H&E model not available for image: {}", fileName);
          LOGGER.error("Nuclear segmentation will be skipped - no nuclei will be detected");
          nucleusROIs = List.of(); // Empty list
        }
      } catch (Exception e) {
        LOGGER.error("Step 2: StarDist segmentation failed for image: {}", fileName, e);
        nucleusROIs = List.of(); // Empty list on failure
      } finally {
        nuclearSegmentation.close();
      }

      // Step 3: Cytoplasm Segmentation
      LOGGER.debug("Step 3: Starting cytoplasm segmentation for: {}", fileName);
      List<CellROI> cellROIs = List.of();
      List<CytoplasmROI> cytoplasmROIs = List.of();

      if (!nucleusROIs.isEmpty()) {
        // Get vessel ROIs for exclusion if enabled
        List<UserROI> vesselROIsForExclusion =
            cytoplasmSettings.isExcludeVessels() ? vesselROIs : List.of();

        CytoplasmSegmentation cytoplasmSegmentation =
            new CytoplasmSegmentation(
                configurationManager,
                imagePlus,
                fileName,
                vesselROIsForExclusion,
                nucleusROIs,
                cytoplasmSettings);

        cytoplasmROIs = cytoplasmSegmentation.segmentCytoplasm();
        cellROIs = cytoplasmSegmentation.getCellROIs();

        LOGGER.debug(
            "Step 3 complete: Found {} cells and {} cytoplasm regions in image: {}",
            cellROIs.size(),
            cytoplasmROIs.size(),
            fileName);
      } else {
        LOGGER.debug("Step 3: No nuclei found, skipping cytoplasm segmentation for: {}", fileName);
      }

      // Step 4: Feature Extraction (TODO)
      LOGGER.debug("Step 4: Feature extraction - TODO (not implemented yet)");

      // Step 5: Cell Classification (TODO)
      LOGGER.debug("Step 5: Cell classification - TODO (not implemented yet)");

      // Step 6: Statistical Analysis (TODO)
      LOGGER.debug("Step 6: Statistical analysis - TODO (not implemented yet)");

      // Add ROIs to manager with proper colors
      for (UserROI vesselROI : vesselROIs) {
        vesselROI.setDisplayColor(mainSettings.getVesselSettings().getBorderColor());
        roiManager.addROI(vesselROI);
      }
      for (NucleusROI nucleusROI : nucleusROIs) {
        nucleusROI.setDisplayColor(mainSettings.getNucleusSettings().getBorderColor());
        roiManager.addROI(nucleusROI);
      }
      for (CellROI cellROI : cellROIs) {
        cellROI.setDisplayColor(mainSettings.getCellSettings().getBorderColor());
        roiManager.addROI(cellROI);
      }
      for (CytoplasmROI cytoplasmROI : cytoplasmROIs) {
        cytoplasmROI.setDisplayColor(mainSettings.getCytoplasmSettings().getBorderColor());
        roiManager.addROI(cytoplasmROI);
      }

      // Clean up
      imagePlus.close();

      return ImageAnalysisResult.success(
          fileName, vesselROIs.size(), nucleusROIs.size(), cellROIs.size());

    } catch (Exception e) {
      LOGGER.error("Error during analysis of image: {}", fileName, e);
      return ImageAnalysisResult.failure(fileName, e.getMessage());
    }
  }

  /**
   * Updates progress and notifies callbacks.
   */
  private void updateProgress(int imageIndex, String message) {
    if (progressMessageCallback != null) {
      progressMessageCallback.accept(message);
    }

    if (progressPercentCallback != null) {
      int percent = totalImages > 0 ? (int) ((double) imageIndex / totalImages * 100) : 0;
      progressPercentCallback.accept(percent);
    }
  }

  /**
   * Requests cancellation of the current batch processing.
   */
  public void cancel() {
    if (isProcessing.get()) {
      LOGGER.info("Cancellation requested for analysis pipeline");
      cancelRequested.set(true);
    }
  }

  /**
   * Checks if the pipeline is currently processing.
   *
   * @return true if processing is active
   */
  public boolean isProcessing() {
    return isProcessing.get();
  }

  /**
   * Gets the current progress as a percentage (0-100).
   *
   * @return progress percentage
   */
  public int getProgressPercent() {
    if (!isProcessing.get() || totalImages == 0) {
      return 0;
    }
    return (int) ((double) processedImages.get() / totalImages * 100);
  }

  /**
   * Gets the number of processed images.
   *
   * @return number of processed images
   */
  public int getProcessedCount() {
    return processedImages.get();
  }

  /**
   * Gets the total number of images to process.
   *
   * @return total number of images
   */
  public int getTotalCount() {
    return totalImages;
  }

  /**
   * Result class for batch analysis operations.
   */
  public static class AnalysisResults {
    private final int processedImages;
    private final int totalVessels;
    private final int totalNuclei;
    private final int totalCells;

    public AnalysisResults(int processedImages, int totalVessels, int totalNuclei, int totalCells) {
      this.processedImages = processedImages;
      this.totalVessels = totalVessels;
      this.totalNuclei = totalNuclei;
      this.totalCells = totalCells;
    }

    public int getProcessedImages() {
      return processedImages;
    }

    public int getTotalVessels() {
      return totalVessels;
    }

    public int getTotalNuclei() {
      return totalNuclei;
    }

    public int getTotalCells() {
      return totalCells;
    }

    @Override
    public String toString() {
      return String.format(
          "AnalysisResults[images=%d, vessels=%d, nuclei=%d, cells=%d]",
          processedImages, totalVessels, totalNuclei, totalCells);
    }
  }

  /**
   * Result class for single image analysis operations.
   */
  public static class ImageAnalysisResult {
    private final String fileName;
    private final boolean success;
    private final String errorMessage;
    private final int vesselCount;
    private final int nucleusCount;
    private final int cellCount;

    private ImageAnalysisResult(
        String fileName,
        boolean success,
        String errorMessage,
        int vesselCount,
        int nucleusCount,
        int cellCount) {
      this.fileName = fileName;
      this.success = success;
      this.errorMessage = errorMessage;
      this.vesselCount = vesselCount;
      this.nucleusCount = nucleusCount;
      this.cellCount = cellCount;
    }

    public static ImageAnalysisResult success(
        String fileName, int vesselCount, int nucleusCount, int cellCount) {
      return new ImageAnalysisResult(fileName, true, null, vesselCount, nucleusCount, cellCount);
    }

    public static ImageAnalysisResult failure(String fileName, String errorMessage) {
      return new ImageAnalysisResult(fileName, false, errorMessage, 0, 0, 0);
    }

    public String getFileName() {
      return fileName;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public int getVesselCount() {
      return vesselCount;
    }

    public int getNucleusCount() {
      return nucleusCount;
    }

    public int getCellCount() {
      return cellCount;
    }

    @Override
    public String toString() {
      return success
          ? String.format(
              "ImageAnalysisResult[%s: vessels=%d, nuclei=%d, cells=%d]",
              fileName, vesselCount, nucleusCount, cellCount)
          : String.format("ImageAnalysisResult[%s: FAILED - %s]", fileName, errorMessage);
    }
  }
}
