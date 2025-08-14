package com.scipath.scipathj.core.pipeline;

import com.scipath.scipathj.core.engine.ResourceManager;
import com.scipath.scipathj.core.events.EventBus;
import com.scipath.scipathj.data.model.ProcessingResult;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes image processing pipelines with progress tracking and resource management.
 *
 * <p>This class provides the core pipeline execution functionality, managing
 * batch processing, progress reporting, and resource allocation.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class PipelineExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(PipelineExecutor.class);

  private final EventBus eventBus;
  private final ResourceManager resourceManager;

  // Processing state
  private final AtomicBoolean isProcessing = new AtomicBoolean(false);
  private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
  private final AtomicInteger currentProgress = new AtomicInteger(0);
  private volatile int totalItems = 0;

  /**
   * Creates a new PipelineExecutor instance.
   *
   * @param eventBus the event bus for progress notifications
   * @param resourceManager the resource manager for monitoring system resources
   */
  public PipelineExecutor(EventBus eventBus, ResourceManager resourceManager) {
    this.eventBus = eventBus;
    this.resourceManager = resourceManager;
    LOGGER.info("PipelineExecutor initialized with resource monitoring");
  }

  /**
   * Executes a pipeline on a batch of images.
   *
   * @param imagePaths the list of image paths to process
   * @param pipeline the pipeline to execute
   * @return list of processing results for each image
   */
  public List<ProcessingResult> executeBatch(List<Path> imagePaths, Pipeline pipeline) {
    if (imagePaths == null || imagePaths.isEmpty()) {
      LOGGER.warn("No images provided for batch processing");
      return new ArrayList<>();
    }

    if (isProcessing.get()) {
      throw new IllegalStateException("Pipeline executor is already processing");
    }

    LOGGER.info(
        "Starting batch processing of {} images with pipeline: {}",
        imagePaths.size(),
        pipeline.getName());

    isProcessing.set(true);
    cancelRequested.set(false);
    totalItems = imagePaths.size();
    currentProgress.set(0);

    List<ProcessingResult> results = new ArrayList<>();
    Instant startTime = Instant.now();

    try {
      for (int i = 0; i < imagePaths.size(); i++) {
        if (cancelRequested.get()) {
          LOGGER.info("Batch processing cancelled at image {} of {}", i + 1, imagePaths.size());
          break;
        }

        Path imagePath = imagePaths.get(i);
        LOGGER.debug("Processing image {} of {}: {}", i + 1, imagePaths.size(), imagePath);

        try {
          ProcessingResult result = executeSingle(imagePath, pipeline);
          results.add(result);

          // Update progress
          currentProgress.set(i + 1);
          double progressPercent = ((double) (i + 1) / imagePaths.size()) * 100;
          LOGGER.debug("Progress: {:.1f}% ({}/{})", progressPercent, i + 1, imagePaths.size());

          // Check memory usage and suggest cleanup if needed
          if (resourceManager.getMemoryUsageRatio() > 0.8) {
            LOGGER.warn("High memory usage detected, forcing garbage collection");
            resourceManager.forceGarbageCollection();
          }

        } catch (Exception e) {
          LOGGER.error("Failed to process image: {}", imagePath, e);
          results.add(ProcessingResult.failure(imagePath.toString(), e));
        }
      }

      Duration processingTime = Duration.between(startTime, Instant.now());
      LOGGER.info(
          "Batch processing completed in {}ms. Processed {} images, {} successful",
          processingTime.toMillis(),
          results.size(),
          results.stream().mapToInt(r -> r.isSuccess() ? 1 : 0).sum());

    } finally {
      isProcessing.set(false);
      currentProgress.set(0);
      totalItems = 0;
    }

    return results;
  }

  /**
   * Executes a pipeline on a single image.
   *
   * @param imagePath the path to the image to process
   * @param pipeline the pipeline to execute
   * @return processing result for the image
   */
  public ProcessingResult executeSingle(Path imagePath, Pipeline pipeline) {
    if (imagePath == null) {
      throw new IllegalArgumentException("Image path cannot be null");
    }
    if (pipeline == null) {
      throw new IllegalArgumentException("Pipeline cannot be null");
    }

    LOGGER.debug("Executing single image processing: {}", imagePath);
    Instant startTime = Instant.now();

    try {
      // For now, return a basic success result
      // This will be expanded when actual pipeline steps are implemented
      Duration processingTime = Duration.between(startTime, Instant.now());

      LOGGER.debug(
          "Single image processing completed in {}ms: {}", processingTime.toMillis(), imagePath);

      return ProcessingResult.success(
          imagePath.toString(),
          new ArrayList<>(),
          java.util.Map.of("processingTime", processingTime.toMillis()));

    } catch (Exception e) {
      LOGGER.error("Single image processing failed: {}", imagePath, e);
      return ProcessingResult.failure(imagePath.toString(), e);
    }
  }

  /**
   * Checks if the executor is currently processing.
   *
   * @return true if processing is active, false otherwise
   */
  public boolean isProcessing() {
    return isProcessing.get();
  }

  /**
   * Gets the current processing progress as a ratio (0.0 to 1.0).
   *
   * @return progress ratio, or 0.0 if not processing
   */
  public double getProgress() {
    if (!isProcessing.get() || totalItems == 0) {
      return 0.0;
    }
    return (double) currentProgress.get() / totalItems;
  }

  /**
   * Requests cancellation of all currently running processing tasks.
   *
   * @return true if cancellation was requested successfully
   */
  public boolean cancelAll() {
    if (isProcessing.get()) {
      LOGGER.info("Cancellation requested for current processing");
      cancelRequested.set(true);
      return true;
    } else {
      LOGGER.debug("No processing to cancel");
      return false;
    }
  }

  /**
   * Gets the current progress as a percentage (0-100).
   *
   * @return progress percentage
   */
  public int getProgressPercent() {
    return (int) (getProgress() * 100);
  }

  /**
   * Gets the number of items currently processed.
   *
   * @return number of processed items
   */
  public int getProcessedCount() {
    return currentProgress.get();
  }

  /**
   * Gets the total number of items to process.
   *
   * @return total number of items
   */
  public int getTotalCount() {
    return totalItems;
  }
}
