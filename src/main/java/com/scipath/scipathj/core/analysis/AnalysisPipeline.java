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
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analysis pipeline coordinator that orchestrates the 3-step analysis workflow:
 * 1. Vessel Segmentation
 * 2. Nuclear Segmentation
 * 3. Cytoplasm Segmentation
 *
 * This class coordinates the workflow following the Single Responsibility Principle,
 * delegating each analysis step to specialized classes.
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

  // Image information for ignore calculation
  private ImagePlus currentImage;

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
   * This constructor follows Dependency Injection principles.
   *
   * @param configurationManager the configuration manager instance
   * @param mainSettings the main settings instance
   * @param roiManager the ROI manager instance
   */
  public AnalysisPipeline(
      final ConfigurationManager configurationManager,
      final MainSettings mainSettings,
      final ROIManager roiManager) {
    this(
        configurationManager,
        configurationManager.loadVesselSegmentationSettings(),
        configurationManager.loadNuclearSegmentationSettings(),
        configurationManager.loadCytoplasmSegmentationSettings(),
        mainSettings,
        roiManager);
  }

  /**
   * Creates a new AnalysisPipeline with custom settings for all segmentation steps.
   * This is the primary constructor that follows Dependency Injection principles.
   *
   * @param configurationManager the configuration manager instance
   * @param vesselSettings custom vessel segmentation settings
   * @param nuclearSettings custom nuclear segmentation settings
   * @param cytoplasmSettings custom cytoplasm segmentation settings
   * @param mainSettings the main settings instance
   * @param roiManager the ROI manager instance
   */
  public AnalysisPipeline(
      final ConfigurationManager configurationManager,
      final VesselSegmentationSettings vesselSettings,
      final NuclearSegmentationSettings nuclearSettings,
      final CytoplasmSegmentationSettings cytoplasmSettings,
      final MainSettings mainSettings,
      final ROIManager roiManager) {
    // Defensive copying to prevent exposure of internal representation
    this.configurationManager = configurationManager; // ConfigurationManager is immutable by design
    this.vesselSettings = vesselSettings; // Settings objects are immutable by design
    this.nuclearSettings = nuclearSettings;
    this.cytoplasmSettings = cytoplasmSettings;
    this.mainSettings = mainSettings; // MainSettings is immutable by design
    this.roiManager = roiManager; // ROIManager is a service object, not data
  }

  /**
   * Sets the progress message callback.
   *
   * @param callback callback to receive progress messages
   */
  public void setProgressMessageCallback(final Consumer<String> callback) {
    this.progressMessageCallback = callback;
  }

  /**
   * Sets the progress percentage callback.
   *
   * @param callback callback to receive progress percentages (0-100)
   */
  public void setProgressPercentCallback(final Consumer<Integer> callback) {
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
  public AnalysisResults processBatch(final File[] imageFiles) {
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

        updateProgress(i, "Analyzing...");

        try {
          ImageAnalysisResult result = processImage(imageFile);
          if (result.success()) {
            totalVessels += result.vesselCount();
            totalNuclei += result.nucleusCount();
            totalCells += result.cellCount();
            successfulImages++;
          } else {
            LOGGER.warn("Analysis failed for image: {}", fileName);
          }

        } catch (ImageProcessingException e) {
          LOGGER.error("Image processing error for {}: {}", fileName, e.getMessage());
        } catch (IOException e) {
          LOGGER.error("IO error processing image {}: {}", fileName, e.getMessage());
        } catch (Exception e) {
          LOGGER.error("Unexpected error processing image {}: {}", fileName, e.getMessage());
        }

        processedImages.incrementAndGet();

        // Use CompletableFuture for non-blocking delay
        CompletableFuture.delayedExecutor(
            SegmentationConstants.DEFAULT_BATCH_PROCESSING_DELAY,
            java.util.concurrent.TimeUnit.MILLISECONDS);
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
   * @throws ImageProcessingException if image processing fails
   * @throws IOException if image loading fails
   */
  public ImageAnalysisResult processImage(final File imageFile)
      throws ImageProcessingException, IOException {
    String fileName = imageFile.getName();

    // Load the image
    ImagePlus imagePlus = ImageLoader.loadImage(imageFile.getAbsolutePath());
    if (imagePlus == null) {
      throw new IOException("Failed to load image: " + fileName);
    }

    return processImage(imagePlus, fileName);
  }

  /**
   * Processes a loaded ImagePlus through the complete analysis pipeline.
   * Currently implements steps 1-3 (vessel, nuclear, and cytoplasm segmentation).
   *
   * @param imagePlus the loaded image
   * @param fileName the filename for ROI association
   * @return analysis result for the image
   * @throws ImageProcessingException if image processing fails
   */
  public ImageAnalysisResult processImage(final ImagePlus imagePlus, final String fileName)
      throws ImageProcessingException {
    this.currentImage = imagePlus; // Store for ignore calculation
    try {
      // Step 1: Vessel Segmentation
      VesselSegmentation vesselSegmentation =
          new VesselSegmentation(configurationManager, imagePlus, fileName, vesselSettings);
      List<UserROI> vesselROIs = vesselSegmentation.segmentVessels();

      // Step 2: Nuclear Segmentation
      NuclearSegmentation nuclearSegmentation =
          new NuclearSegmentation(
              configurationManager, imagePlus, fileName, nuclearSettings, roiManager);

      List<NucleusROI> nucleusROIs = List.of();
      try {
        if (nuclearSegmentation.isAvailable()) {
          nucleusROIs = nuclearSegmentation.segmentNuclei();
        } else {
          LOGGER.warn("StarDist H&E model not available for image: {}", fileName);
        }
      } catch (Exception e) {
        LOGGER.error("StarDist segmentation failed for image: {}", fileName, e);
        throw new ImageProcessingException("Nuclear segmentation failed", e);
      } finally {
        nuclearSegmentation.close();
      }

      // Step 3: Cytoplasm Segmentation
      List<CellROI> cellROIs = List.of();
      List<CytoplasmROI> cytoplasmROIs = List.of();

      if (!nucleusROIs.isEmpty()) {
        List<UserROI> vesselROIsForExclusion =
            cytoplasmSettings.useVesselExclusion() ? vesselROIs : List.of();

        try {
          CytoplasmSegmentation cytoplasmSegmentation =
              new CytoplasmSegmentation(
                  configurationManager,
                  imagePlus,
                  fileName,
                  vesselROIsForExclusion,
                  nucleusROIs,
                  cytoplasmSettings,
                  mainSettings,
                  roiManager);

          cytoplasmROIs = cytoplasmSegmentation.segmentCytoplasm();
          cellROIs = cytoplasmSegmentation.getCellROIs();
        } catch (CytoplasmSegmentation.CytoplasmSegmentationException e) {
          LOGGER.error("Cytoplasm segmentation failed for image: {}", fileName, e);
          throw new ImageProcessingException("Cytoplasm segmentation failed", e);
        }
      }

      // Add ROIs to manager with proper colors
      addROIsToManager(vesselROIs, nucleusROIs, cellROIs, cytoplasmROIs);

      // Clean up
      imagePlus.close();

      return ImageAnalysisResult.success(
          fileName, vesselROIs.size(), nucleusROIs.size(), cellROIs.size());

    } catch (ImageProcessingException e) {
      // Re-throw ImageProcessingException as-is
      throw e;
    } catch (RuntimeException e) {
      LOGGER.error("Runtime error during analysis of image: {}", fileName, e);
      throw new ImageProcessingException("Image analysis failed for " + fileName, e);
    }
  }

  /**
   * Adds ROIs to the manager with appropriate colors and ignore status.
   * This method follows the Single Responsibility Principle by separating ROI management.
   * Ensures consistency: if a cell or cytoplasm is ignored, its nucleus is also ignored.
   */
  private void addROIsToManager(
      final List<UserROI> vesselROIs,
      final List<NucleusROI> nucleusROIs,
      final List<CellROI> cellROIs,
      final List<CytoplasmROI> cytoplasmROIs) {

    // Get image dimensions for ignore calculation
    int imageWidth = currentImage.getWidth();
    int imageHeight = currentImage.getHeight();
    int borderDistance = mainSettings.ignoreSettings().borderDistance();

    // First pass: mark ROIs as ignored based on border distance
    vesselROIs.forEach(
        roi -> {
          roi.setDisplayColor(mainSettings.getVesselSettings().borderColor());
          // Mark as ignored if too close to borders
          roi.setIgnored(roi.shouldBeIgnored(imageWidth, imageHeight, borderDistance));
        });

    nucleusROIs.forEach(
        roi -> {
          roi.setDisplayColor(mainSettings.getNucleusSettings().borderColor());
          // Mark as ignored if too close to borders
          roi.setIgnored(roi.shouldBeIgnored(imageWidth, imageHeight, borderDistance));
        });

    cellROIs.forEach(
        roi -> {
          roi.setDisplayColor(mainSettings.getCellSettings().borderColor());
          // Mark as ignored if too close to borders
          roi.setIgnored(roi.shouldBeIgnored(imageWidth, imageHeight, borderDistance));
        });

    cytoplasmROIs.forEach(
        roi -> {
          roi.setDisplayColor(mainSettings.getCytoplasmSettings().borderColor());
          // Mark as ignored if too close to borders
          roi.setIgnored(roi.shouldBeIgnored(imageWidth, imageHeight, borderDistance));
        });

    // Second pass: ensure consistency - if cell or cytoplasm is ignored, nucleus should be too
    cellROIs.forEach(cell -> {
      if (cell.isIgnored() && cell.getAssociatedNucleus() != null) {
        cell.getAssociatedNucleus().setIgnored(true);
      }
    });

    cytoplasmROIs.forEach(cytoplasm -> {
      if (cytoplasm.isIgnored() && cytoplasm.getAssociatedNucleus() != null) {
        cytoplasm.getAssociatedNucleus().setIgnored(true);
      }
    });

    // Add all ROIs to manager after consistency check
    vesselROIs.forEach(roiManager::addROI);
    nucleusROIs.forEach(roiManager::addROI);
    cellROIs.forEach(roiManager::addROI);
    cytoplasmROIs.forEach(roiManager::addROI);
  }

  /**
   * Updates progress and notifies callbacks.
   */
  private void updateProgress(final int imageIndex, final String message) {
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
   * Result record for batch analysis operations using Java 16+ record syntax.
   */
  public record AnalysisResults(
      int processedImages, int totalVessels, int totalNuclei, int totalCells) {
    @Override
    public String toString() {
      return String.format(
          "AnalysisResults[images=%d, vessels=%d, nuclei=%d, cells=%d]",
          processedImages, totalVessels, totalNuclei, totalCells);
    }
  }

  /**
   * Result record for single image analysis operations using Java 16+ record syntax.
   */
  public record ImageAnalysisResult(
      String fileName,
      boolean success,
      String errorMessage,
      int vesselCount,
      int nucleusCount,
      int cellCount) {

    public static ImageAnalysisResult success(
        final String fileName, final int vesselCount, final int nucleusCount, final int cellCount) {
      return new ImageAnalysisResult(fileName, true, null, vesselCount, nucleusCount, cellCount);
    }

    public static ImageAnalysisResult failure(final String fileName, final String errorMessage) {
      return new ImageAnalysisResult(fileName, false, errorMessage, 0, 0, 0);
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

  /**
   * Custom exception for image processing errors.
   */
  public static class ImageProcessingException extends Exception {
    public ImageProcessingException(final String message) {
      super(message);
    }

    public ImageProcessingException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
