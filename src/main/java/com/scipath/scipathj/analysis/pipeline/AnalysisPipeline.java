package com.scipath.scipathj.analysis.pipeline;
import com.scipath.scipathj.analysis.config.ClassificationSettings;
import com.scipath.scipathj.analysis.config.UnsupervisedClassificationSettings;

import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.analysis.config.CytoplasmSegmentationSettings;
import com.scipath.scipathj.analysis.config.FeatureExtractionSettings;
import com.scipath.scipathj.infrastructure.config.MainSettings;
import com.scipath.scipathj.analysis.config.NuclearSegmentationSettings;
import com.scipath.scipathj.analysis.config.SegmentationConstants;
import com.scipath.scipathj.analysis.config.VesselSegmentationSettings;
import com.scipath.scipathj.infrastructure.roi.CellROI;
import com.scipath.scipathj.infrastructure.roi.CytoplasmROI;
import com.scipath.scipathj.infrastructure.roi.NucleusROI;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.analysis.algorithms.segmentation.VesselSegmentation;
import com.scipath.scipathj.analysis.algorithms.segmentation.NuclearSegmentation;
import com.scipath.scipathj.analysis.algorithms.segmentation.CytoplasmSegmentation;
import com.scipath.scipathj.analysis.algorithms.classification.FeatureExtraction;
import com.scipath.scipathj.analysis.algorithms.classification.CellClassification;
import com.scipath.scipathj.analysis.algorithms.classification.UnsupervisedClassifier;
import com.scipath.scipathj.ui.common.ROIManager;
import com.scipath.scipathj.ui.utils.ImageLoader;
import com.scipath.scipathj.infrastructure.utils.DirectFileLogger;
import ij.ImagePlus;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
   private final ClassificationSettings classificationSettings;

  private final ConfigurationManager configurationManager;
  private final VesselSegmentationSettings vesselSettings;
  private final NuclearSegmentationSettings nuclearSettings;
  private final CytoplasmSegmentationSettings cytoplasmSettings;
  private final FeatureExtractionSettings featureExtractionSettings;
  private final UnsupervisedClassificationSettings unsupervisedSettings;
  private final MainSettings mainSettings;
  private final ROIManager roiManager;

  // Image information for ignore calculation
  private ImagePlus currentImage;

  // Progress tracking
  private final AtomicBoolean isProcessing = new AtomicBoolean(false);
  private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
  private final AtomicInteger processedImages = new AtomicInteger(0);
  private volatile int totalImages = 0;
  private static long imageCounter = 0;

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
         configurationManager.loadFeatureExtractionSettings(),
         configurationManager.loadClassificationSettings(),
         configurationManager.loadUnsupervisedClassificationSettings(),
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
   * @param featureExtractionSettings custom feature extraction settings
   * @param mainSettings the main settings instance
   * @param roiManager the ROI manager instance
   */
  public AnalysisPipeline(
      final ConfigurationManager configurationManager,
      final VesselSegmentationSettings vesselSettings,
      final NuclearSegmentationSettings nuclearSettings,
      final CytoplasmSegmentationSettings cytoplasmSettings,
      final FeatureExtractionSettings featureExtractionSettings,
      final ClassificationSettings classificationSettings,
      final UnsupervisedClassificationSettings unsupervisedSettings,
      final MainSettings mainSettings,
      final ROIManager roiManager) {
    // Defensive copying to prevent exposure of internal representation
    this.configurationManager = configurationManager; // ConfigurationManager is immutable by design
    this.vesselSettings = vesselSettings; // Settings objects are immutable by design
    this.nuclearSettings = nuclearSettings;
    this.cytoplasmSettings = cytoplasmSettings;
    this.featureExtractionSettings = featureExtractionSettings; // Settings objects are immutable by design
    this.classificationSettings = classificationSettings; // Settings objects are immutable by design
    this.unsupervisedSettings = unsupervisedSettings; // Settings objects are immutable by design
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
      return new AnalysisResults(0, 0, 0, 0, java.util.Map.of());
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
    java.util.Map<String, java.util.Map<String, Object>> allFeatures = new java.util.HashMap<>();

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
          // Clear ROIs from previous image to prevent accumulation
          roiManager.clearAllROIs();

          // Track processing time for this image
          long imageStartTime = System.currentTimeMillis();
          ImageAnalysisResult result = processImage(imageFile);
          long imageEndTime = System.currentTimeMillis();
          long processingTimeMs = imageEndTime - imageStartTime;

          if (result.success()) {
            // Processing statistics were already stored in processImage method

            // Log completion with timing info
            DirectFileLogger.logPerformance("=== IMAGE COMPLETED: " + fileName +
                " (vessels=" + result.vesselCount() +
                ", nuclei=" + result.nucleusCount() +
                ", cells=" + result.cellCount() +
                ", time=" + processingTimeMs + "ms) ===");

            totalVessels += result.vesselCount();
            totalNuclei += result.nucleusCount();
            totalCells += result.cellCount();
            successfulImages++;

            // Collect features from this image
            if (result.extractedFeatures() != null && !result.extractedFeatures().isEmpty()) {
              allFeatures.putAll(result.extractedFeatures());
            }
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

    return new AnalysisResults(successfulImages, totalVessels, totalNuclei, totalCells, allFeatures);
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
    // Initialize performance logging for first image
    if (++imageCounter == 1) {
      DirectFileLogger.initializePerformanceLogging();
    }

    final long totalStartTime = System.currentTimeMillis();
    DirectFileLogger.logPerformance("=== Starting analysis of image: " + fileName + " (image #" + imageCounter + ") ===");
    DirectFileLogger.logMemoryUsage();

    this.currentImage = imagePlus; // Store for ignore calculation
    try {
      // Step 1: Vessel Segmentation
      long vesselSetupStart = System.currentTimeMillis();
      DirectFileLogger.logPerformance("Step 1: Starting vessel segmentation");
      VesselSegmentation vesselSegmentation =
          new VesselSegmentation(configurationManager, imagePlus, fileName, vesselSettings);
      long setupTime = System.currentTimeMillis() - vesselSetupStart;
      DirectFileLogger.logPerformance("Vessel segmentation setup took " + setupTime + "ms");

      long vesselSegmentStart = System.currentTimeMillis();
      List<UserROI> vesselROIs = vesselSegmentation.segmentVessels();
      long vesselSegmentTime = System.currentTimeMillis() - vesselSegmentStart;
      DirectFileLogger.logPerformance("Vessel segmentation completed in " + vesselSegmentTime + "ms - found " + vesselROIs.size() + " vessels");
      DirectFileLogger.logMemoryUsage();

      // Step 2: Nuclear Segmentation
      long nuclearSetupStart = System.currentTimeMillis();
      DirectFileLogger.logPerformance("Step 2: Starting nuclear segmentation setup");
      NuclearSegmentation nuclearSegmentation =
          new NuclearSegmentation(
              configurationManager, imagePlus, fileName, nuclearSettings, roiManager);

      long nuclearSetupTime = System.currentTimeMillis() - nuclearSetupStart;
      DirectFileLogger.logPerformance("Nuclear segmentation setup took " + nuclearSetupTime + "ms");

      List<NucleusROI> nucleusROIs = List.of();
      try {
        if (nuclearSegmentation.isAvailable()) {
          long nuclearSegmentStart = System.currentTimeMillis();
          DirectFileLogger.logPerformance("Starting nuclear segmentation execution");
          nucleusROIs = nuclearSegmentation.segmentNuclei();
          long nuclearSegmentTime = System.currentTimeMillis() - nuclearSegmentStart;
          DirectFileLogger.logPerformance("Nuclear segmentation execution completed in " + nuclearSegmentTime + "ms - found " + nucleusROIs.size() + " nuclei");
        } else {
          DirectFileLogger.logPerformance("Nuclear segmentation skipped - StarDist H&E model not available");
          LOGGER.warn("StarDist H&E model not available for image: {}", fileName);
        }
      } catch (Exception e) {
        DirectFileLogger.logPerformance("Nuclear segmentation failed: " + e.getMessage());
        LOGGER.error("StarDist segmentation failed for image: {}", fileName, e);
        throw new ImageProcessingException("Nuclear segmentation failed", e);
      } finally {
        long nuclearCloseStart = System.currentTimeMillis();
        nuclearSegmentation.close();
        long nuclearCloseTime = System.currentTimeMillis() - nuclearCloseStart;
        DirectFileLogger.logPerformance("Nuclear segmentation cleanup took " + nuclearCloseTime + "ms");
      }
      DirectFileLogger.logMemoryUsage();

      // Step 3: Cytoplasm Segmentation
      List<CellROI> cellROIs = List.of();
      List<CytoplasmROI> cytoplasmROIs = List.of();

      if (!nucleusROIs.isEmpty()) {
        long cytoplasmSetupStart = System.currentTimeMillis();
        DirectFileLogger.logPerformance("Step 3: Starting cytoplasm segmentation setup");
        List<UserROI> vesselROIsForExclusion =
            cytoplasmSettings.useVesselExclusion() ? vesselROIs : List.of();

        long setupDecisionTime = System.currentTimeMillis() - cytoplasmSetupStart;
        DirectFileLogger.logPerformance("Cytoplasm setup decisions took " + setupDecisionTime + "ms");

        try {
          long cytoplasmSegmentSetupStart = System.currentTimeMillis();
          DirectFileLogger.logPerformance("Creating cytoplasm segmentation object");
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
          long objectCreationTime = System.currentTimeMillis() - cytoplasmSegmentSetupStart;
          DirectFileLogger.logPerformance("Cytoplasm segmentation object creation took " + objectCreationTime + "ms");

          long cytoplasmExecStart = System.currentTimeMillis();
          DirectFileLogger.logPerformance("Starting cytoplasm segmentation execution");
          cytoplasmROIs = cytoplasmSegmentation.segmentCytoplasm();
          long cytoplasmSegmentTime = System.currentTimeMillis() - cytoplasmExecStart;
          DirectFileLogger.logPerformance("Cytoplasm segmentation execution took " + cytoplasmSegmentTime + "ms - found " + cytoplasmROIs.size() + " cytoplasm ROIs");

          long cellExtractionStart = System.currentTimeMillis();
          cellROIs = cytoplasmSegmentation.getCellROIs();
          long cellExtractionTime = System.currentTimeMillis() - cellExtractionStart;
          DirectFileLogger.logPerformance("Cell ROI extraction took " + cellExtractionTime + "ms - found " + cellROIs.size() + " cell ROIs");

        } catch (CytoplasmSegmentation.CytoplasmSegmentationException e) {
          DirectFileLogger.logPerformance("Cytoplasm segmentation failed: " + e.getMessage());
          LOGGER.error("Cytoplasm segmentation failed for image: {}", fileName, e);
          throw new ImageProcessingException("Cytoplasm segmentation failed", e);
        }
      } else {
        DirectFileLogger.logPerformance("Step 3: Skipping cytoplasm segmentation (no nuclei found)");
      }
      DirectFileLogger.logMemoryUsage();

      // Add ROIs to manager with proper colors
      long roiManagerStart = System.currentTimeMillis();
      DirectFileLogger.logPerformance("Starting ROI manager operations - adding " + (vesselROIs.size() + nucleusROIs.size() + cellROIs.size() + cytoplasmROIs.size()) + " ROIs total");
      addROIsToManager(vesselROIs, nucleusROIs, cellROIs, cytoplasmROIs);
      long roiManagerTime = System.currentTimeMillis() - roiManagerStart;
      DirectFileLogger.logPerformance("ROI manager operations completed in " + roiManagerTime + "ms");
      DirectFileLogger.logMemoryUsage();

      // Prepare detailed statistics (will be stored after total time calculation)
      int totalIgnored = calculateIgnoredROICount(vesselROIs, nucleusROIs, cellROIs, cytoplasmROIs);

      // Step 4: Ultra-Fast Feature Extraction with H&E support and scale conversion
      DirectFileLogger.logPerformance("Step 4: Starting ultra-fast feature extraction");
      LOGGER.info("Starting ultra-fast feature extraction for image: {}", fileName);

      long featureSetupStart = System.currentTimeMillis();
      FeatureExtraction featureExtraction = new FeatureExtraction(
          imagePlus,
          fileName,
          vesselROIs,
          (java.util.List<UserROI>) (java.util.List<?>) nucleusROIs, // Cast NucleusROI to UserROI
          (java.util.List<UserROI>) (java.util.List<?>) cytoplasmROIs, // Cast CytoplasmROI to UserROI
          (java.util.List<UserROI>) (java.util.List<?>) cellROIs, // Cast CellROI to UserROI
          featureExtractionSettings,
          mainSettings);
      long featureSetupTime = System.currentTimeMillis() - featureSetupStart;
      DirectFileLogger.logPerformance("Feature extraction setup took " + featureSetupTime + "ms");

      long featureExecStart = System.currentTimeMillis();
      java.util.Map<String, java.util.Map<String, Object>> extractedFeatures = featureExtraction.extractFeatures();
      long featureExecTime = System.currentTimeMillis() - featureExecStart;
      DirectFileLogger.logPerformance("Feature extraction execution took " + featureExecTime + "ms - processed " + extractedFeatures.size() + " ROIs");
      DirectFileLogger.logMemoryUsage();
      LOGGER.info("Feature extraction completed for image: {} - extracted features for {} ROIs",
          fileName, extractedFeatures.size());

      // Store extracted features in ROIManager for unsupervised analysis
      roiManager.storeExtractedFeatures(extractedFeatures);

     // Step 5: Cell Classification (Unsupervised)
      java.util.Map<String, CellClassification.ClassificationResult> classificationResults = java.util.Map.of();
      
      if (unsupervisedSettings.enabled()) {
        DirectFileLogger.logPerformance("Step 5: Starting unsupervised cell classification (clustering)");
        LOGGER.info("Starting unsupervised classification for image: {}", fileName);
        
        long classificationStart = System.currentTimeMillis();
        UnsupervisedClassifier classifier = new UnsupervisedClassifier();
        
        // Use configured features or all available if empty
        List<String> featuresToUse = unsupervisedSettings.selectedFeatures();
        if (featuresToUse.isEmpty()) {
            featuresToUse = java.util.Arrays.asList(com.scipath.scipathj.analysis.algorithms.classification.CellFeatureAggregator.getFeatureNames());
        }
        
        Map<Integer, List<CellROI>> clusters = classifier.clusterCells(
            cellROIs,
            extractedFeatures,
            fileName,
            unsupervisedSettings.k(),
            featuresToUse,
            unsupervisedSettings.algorithm(),
            unsupervisedSettings.maxIterations(),
            unsupervisedSettings.epsilon(),
            unsupervisedSettings.minPoints()
        );
        
        // Convert clusters to classification results format for compatibility
        // We'll use "Cluster X" as the class name
        Map<String, CellClassification.ClassificationResult> results = new java.util.HashMap<>();
        for (Map.Entry<Integer, List<CellROI>> entry : clusters.entrySet()) {
            int clusterId = entry.getKey();
            String className = "Cluster " + (clusterId + 1);
            
            for (CellROI cell : entry.getValue()) {
                String roiKey = fileName + "_" + cell.getName();
                // Create a result with 100% confidence for the assigned cluster
                results.put(roiKey, new CellClassification.ClassificationResult(className, 1.0));
                
                // Also update ROI color based on cluster
                // Generate distinct colors for K clusters
                java.awt.Color color = java.awt.Color.getHSBColor((float) clusterId / unsupervisedSettings.k(), 0.8f, 0.9f);
                cell.setDisplayColor(color);
                if (cell.getAssociatedNucleus() != null) {
                    cell.getAssociatedNucleus().setDisplayColor(color);
                }
                if (cell.getAssociatedCytoplasm() != null) {
                    cell.getAssociatedCytoplasm().setDisplayColor(color);
                }
            }
        }
        
        classificationResults = results;
        roiManager.setClassificationResults(classificationResults);
        
        long classificationTime = System.currentTimeMillis() - classificationStart;
        DirectFileLogger.logPerformance("Unsupervised classification completed in " + classificationTime + "ms - clustered " + cellROIs.size() + " cells into " + unsupervisedSettings.k() + " clusters");
      } else {
        DirectFileLogger.logPerformance("Step 5: Classification skipped (disabled)");
      }
      DirectFileLogger.logMemoryUsage();

      // Step 6: Calculate ROI feature statistics (mean values per ROI type)
      DirectFileLogger.logPerformance("Step 6: Starting ROI feature statistics calculation");
      long statsStart = System.currentTimeMillis();

      ROIStatisticsCalculator statisticsCalculator = new ROIStatisticsCalculator();
      // Collect all ROIs processed for this image
      List<UserROI> allROIsForImage = new ArrayList<>();
      allROIsForImage.addAll(vesselROIs);
      allROIsForImage.addAll(nucleusROIs);
      allROIsForImage.addAll(cytoplasmROIs);
      allROIsForImage.addAll(cellROIs);

      Map<UserROI.ROIType, Map<String, Double>> roiTypeFeatureMeans =
          statisticsCalculator.calculateROITypeFeatureMeans(extractedFeatures, allROIsForImage);

      long statsTime = System.currentTimeMillis() - statsStart;
      DirectFileLogger.logPerformance("ROI feature statistics calculation took " + statsTime + "ms");

      // Store the statistics for UI access (per image)
      roiManager.setFeatureStatisticsAveragesForImage(fileName, roiTypeFeatureMeans);

      // Log the calculated means
      statisticsCalculator.logROITypeFeatureMeans(roiTypeFeatureMeans, fileName);

      LOGGER.info("Feature extraction and ROI statistics completed for {} with {} ROIs processed", fileName, extractedFeatures.size());

      // Clean up
      long cleanupStart = System.currentTimeMillis();
      DirectFileLogger.logPerformance("Starting final cleanup");
      imagePlus.close();
      long cleanupTime = System.currentTimeMillis() - cleanupStart;
      DirectFileLogger.logPerformance("Final cleanup took " + cleanupTime + "ms");

      // Calculate final processing time and create detailed stats
      long totalProcessingTime = System.currentTimeMillis() - totalStartTime;
      ImageProcessingStats detailedStats = new ImageProcessingStats(
          totalProcessingTime, vesselROIs.size(), nucleusROIs.size(),
          cytoplasmROIs.size(), cellROIs.size(), totalIgnored);

      // Store in ROI manager for persistent UI access
      roiManager.storeProcessingStats(fileName, totalProcessingTime,
          vesselROIs.size(), nucleusROIs.size(), cytoplasmROIs.size(),
          cellROIs.size(), totalIgnored);

      // Save ROIs to temp file for persistent "Save All ROIs" functionality
      roiManager.saveROIsToTempFile(fileName);

      return ImageAnalysisResult.success(
          fileName, vesselROIs.size(), nucleusROIs.size(), cellROIs.size(), detailedStats, extractedFeatures, classificationResults);

    } catch (ImageProcessingException e) {
      // Re-throw ImageProcessingException as-is
      long failureTime = System.currentTimeMillis() - totalStartTime;
      DirectFileLogger.logPerformance("=== ANALYSIS FAILED for " + fileName + " after " + failureTime + "ms: " + e.getMessage() + " ===");
      throw e;
    } catch (RuntimeException e) {
      long failureTime = System.currentTimeMillis() - totalStartTime;
      DirectFileLogger.logPerformance("=== RUNTIME ERROR during analysis of " + fileName + " after " + failureTime + "ms: " + e.getMessage() + " ===");
      LOGGER.error("Runtime error during analysis of image: {}", fileName, e);
      throw new ImageProcessingException("Image analysis failed for " + fileName, e);
    } finally {
      long finalTotalTime = System.currentTimeMillis() - totalStartTime;
      DirectFileLogger.logPerformance("=== TOTAL TIME for " + fileName + ": " + finalTotalTime + "ms ===");
      DirectFileLogger.logPerformance("");
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

    // First pass: mark ROIs as ignored based on border distance (only if ignore functionality is enabled)
    if (mainSettings.enableIgnoreFunctionality()) {
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
    } else {
      // Ignore functionality is disabled, set all ROIs as not ignored
      vesselROIs.forEach(
          roi -> {
            roi.setDisplayColor(mainSettings.getVesselSettings().borderColor());
            roi.setIgnored(false);
          });

      nucleusROIs.forEach(
          roi -> {
            roi.setDisplayColor(mainSettings.getNucleusSettings().borderColor());
            roi.setIgnored(false);
          });

      cellROIs.forEach(
          roi -> {
            roi.setDisplayColor(mainSettings.getCellSettings().borderColor());
            roi.setIgnored(false);
          });

      cytoplasmROIs.forEach(
          roi -> {
            roi.setDisplayColor(mainSettings.getCytoplasmSettings().borderColor());
            roi.setIgnored(false);
          });
    }

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
   * Record for complete image processing statistics including all ROI types.
   */
  public record ImageProcessingStats(
      long processingTimeMs,
      int vesselCount,
      int nucleusCount,
      int cytoplasmCount,
      int cellCount,
      int ignoredCount) {
  }

  /**
   * Result record for batch analysis operations using Java 16+ record syntax.
   */
  public record AnalysisResults(
      int processedImages,
      int totalVessels,
      int totalNuclei,
      int totalCells,
      java.util.Map<String, java.util.Map<String, Object>> allExtractedFeatures) {
    @Override
    public String toString() {
      return String.format(
          "AnalysisResults[images=%d, vessels=%d, nuclei=%d, cells=%d, features=%d ROIs]",
          processedImages, totalVessels, totalNuclei, totalCells,
          allExtractedFeatures != null ? allExtractedFeatures.size() : 0);
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
      int cellCount,
      ImageProcessingStats detailedStats,
      java.util.Map<String, java.util.Map<String, Object>> extractedFeatures,
      java.util.Map<String, CellClassification.ClassificationResult> classificationResults) {

    public static ImageAnalysisResult success(
        final String fileName, final int vesselCount, final int nucleusCount, final int cellCount,
        final ImageProcessingStats detailedStats,
        final java.util.Map<String, java.util.Map<String, Object>> extractedFeatures,
        final java.util.Map<String, CellClassification.ClassificationResult> classificationResults) {
      return new ImageAnalysisResult(fileName, true, null, vesselCount, nucleusCount, cellCount, detailedStats, extractedFeatures, classificationResults);
    }

    public static ImageAnalysisResult success(
        final String fileName, final int vesselCount, final int nucleusCount, final int cellCount,
        final ImageProcessingStats detailedStats,
        final java.util.Map<String, java.util.Map<String, Object>> extractedFeatures) {
      return new ImageAnalysisResult(fileName, true, null, vesselCount, nucleusCount, cellCount, detailedStats, extractedFeatures, java.util.Map.of());
    }

    public static ImageAnalysisResult success(
        final String fileName, final int vesselCount, final int nucleusCount, final int cellCount,
        final ImageProcessingStats detailedStats) {
      return new ImageAnalysisResult(fileName, true, null, vesselCount, nucleusCount, cellCount, detailedStats, java.util.Map.of(), java.util.Map.of());
    }

    public static ImageAnalysisResult failure(final String fileName, final String errorMessage) {
      return new ImageAnalysisResult(fileName, false, errorMessage, 0, 0, 0, null, java.util.Map.of(), java.util.Map.of());
    }

    @Override
    public String toString() {
      return success
          ? String.format(
              "ImageAnalysisResult[%s: vessels=%d, nuclei=%d, cells=%d, features=%d ROIs, classifications=%d ROIs]",
              fileName, vesselCount, nucleusCount, cellCount, extractedFeatures.size(),
              classificationResults != null ? classificationResults.size() : 0)
          : String.format("ImageAnalysisResult[%s: FAILED - %s]", fileName, errorMessage);
    }
  }

  /**
   * Calculate the total number of ROIs marked as ignored.
   * This must be called after addROIsToManager since that's where ROIs are marked as ignored.
   */
  private int calculateIgnoredROICount(List<UserROI> vesselROIs, List<NucleusROI> nucleusROIs,
      List<CellROI> cellROIs, List<CytoplasmROI> cytoplasmROIs) {
    int ignoredCount = 0;

    for (UserROI roi : vesselROIs) {
      if (roi.isIgnored()) ignoredCount++;
    }
    for (NucleusROI roi : nucleusROIs) {
      if (roi.isIgnored()) ignoredCount++;
    }
    for (CellROI roi : cellROIs) {
      if (roi.isIgnored()) ignoredCount++;
    }
    for (CytoplasmROI roi : cytoplasmROIs) {
      if (roi.isIgnored()) ignoredCount++;
    }

    return ignoredCount;
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
