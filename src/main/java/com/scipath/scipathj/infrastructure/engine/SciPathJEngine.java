package com.scipath.scipathj.infrastructure.engine;

import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.analysis.pipeline.AnalysisPipeline;
import com.scipath.scipathj.infrastructure.pipeline.ProcessingResult;
import com.scipath.scipathj.ui.common.ROIManager;
import com.scipath.scipathj.infrastructure.config.MainSettings;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core engine for SciPathJ image analysis operations.
 *
 * <p>This class serves as the central coordinator for all image processing operations,
 * managing pipeline execution, resource allocation, and progress reporting.</p>
 *
 * <p>The engine coordinates all image processing operations with dependency injection
 * for better testability and maintainability.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class SciPathJEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(SciPathJEngine.class);

  private final ConfigurationManager configManager;
  private final AnalysisPipeline analysisPipeline;
  private final ExecutorService executorService;

  private volatile boolean isShutdown = false;

  /**
   * Creates a new SciPathJ engine with the specified configuration manager.
   *
   * @param configManager the configuration manager to use
   */
  public SciPathJEngine(ConfigurationManager configManager) {
    LOGGER.info("Initializing SciPathJ Engine");

    this.configManager = configManager;

    // Create MainSettings and ROIManager needed for AnalysisPipeline
    MainSettings mainSettings = configManager.loadMainSettings();
    ROIManager roiManager = ROIManager.getInstance();

    this.analysisPipeline = new AnalysisPipeline(configManager, mainSettings, roiManager);
    this.executorService = createExecutorService();

    LOGGER.info("SciPathJ Engine initialized");
  }

  private ExecutorService createExecutorService() {
    int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
    ExecutorService service =
        Executors.newFixedThreadPool(
            threadCount,
            r -> {
              Thread t = new Thread(r, "SciPathJ-Worker");
              t.setDaemon(true);
              return t;
            });
    LOGGER.info("Created thread pool with {} worker threads", threadCount);
    return service;
  }

  public CompletableFuture<AnalysisPipeline.AnalysisResults> processImages(
      List<Path> imagePaths) {
    checkNotShutdown();
    LOGGER.info(
        "Starting batch processing of {} images with AnalysisPipeline",
        imagePaths.size());

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            // Convert Path list to File array for AnalysisPipeline
            File[] imageFiles = imagePaths.stream()
                .map(Path::toFile)
                .toArray(File[]::new);

            return analysisPipeline.processBatch(imageFiles);
          } catch (Exception e) {
            LOGGER.error("Batch processing failed", e);
            throw new RuntimeException("Batch processing failed", e);
          }
        },
        executorService);
  }

  public CompletableFuture<AnalysisPipeline.ImageAnalysisResult> processImage(Path imagePath) {
    checkNotShutdown();
    LOGGER.debug("Starting processing of image: {}", imagePath);

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            File imageFile = imagePath.toFile();
            return analysisPipeline.processImage(imageFile);
          } catch (Exception e) {
            LOGGER.error("Image processing failed for: {}", imagePath, e);
            throw new RuntimeException("Image processing failed for: " + imagePath, e);
          }
        },
        executorService);
  }

  private void checkNotShutdown() {
    if (isShutdown) {
      throw new IllegalStateException("Engine has been shut down");
    }
  }

  private <T> T executeWithErrorHandling(
      java.util.function.Supplier<T> operation, String errorMessage) {
    try {
      return operation.get();
    } catch (Exception e) {
      LOGGER.error(errorMessage, e);
      throw new RuntimeException(errorMessage, e);
    }
  }

  /**
   * Gets the configuration manager.
   *
   * @return the configuration manager instance
   */
  public ConfigurationManager getConfigurationManager() {
    return configManager;
  }


  /**
   * Checks if the engine is currently processing any tasks.
   *
   * @return true if processing is active, false otherwise
   */
  public boolean isProcessing() {
    return analysisPipeline.isProcessing();
  }

  /**
   * Gets the current processing progress as a percentage.
   *
   * @return progress percentage (0.0 to 1.0), or 0.0 if not processing
   */
  public double getProgress() {
    if (!isProcessing()) return 0.0;
    return (double) analysisPipeline.getProgressPercent() / 100.0;
  }

  /**
   * Cancels all currently running processing tasks.
   *
   * @return true if cancellation was successful, false otherwise
   */
  public boolean cancelProcessing() {
    LOGGER.info("Cancelling all processing tasks");
    analysisPipeline.cancel();
    return true;
  }

  public void shutdown() {
    if (isShutdown) return;

    LOGGER.info("Shutting down SciPathJ Engine");
    isShutdown = true;

    try {
      cancelProcessing();
      shutdownExecutorService();
      LOGGER.info("SciPathJ Engine shutdown complete");
    } catch (InterruptedException e) {
      LOGGER.warn("Shutdown interrupted", e);
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      LOGGER.error("Error during engine shutdown", e);
    }
  }

  private void shutdownExecutorService() throws InterruptedException {
    executorService.shutdown();
    if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
      LOGGER.warn("Executor service did not terminate gracefully, forcing shutdown");
      executorService.shutdownNow();
    }
  }
}
