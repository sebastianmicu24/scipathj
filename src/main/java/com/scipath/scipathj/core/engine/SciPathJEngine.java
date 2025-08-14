package com.scipath.scipathj.core.engine;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.events.EventBus;
import com.scipath.scipathj.core.pipeline.Pipeline;
import com.scipath.scipathj.core.pipeline.PipelineExecutor;
import com.scipath.scipathj.data.model.ProcessingResult;
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
  private final EventBus eventBus;
  private final PipelineExecutor pipelineExecutor;
  private final ResourceManager resourceManager;
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
    this.eventBus = new EventBus();
    this.resourceManager = new ResourceManager();
    this.pipelineExecutor = new PipelineExecutor(eventBus, resourceManager);
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

  public CompletableFuture<List<ProcessingResult>> processImages(
      List<Path> imagePaths, Pipeline pipeline) {
    checkNotShutdown();
    LOGGER.info(
        "Starting batch processing of {} images with pipeline: {}",
        imagePaths.size(),
        pipeline.getName());

    return CompletableFuture.supplyAsync(
        () ->
            executeWithErrorHandling(
                () -> pipelineExecutor.executeBatch(imagePaths, pipeline),
                "Batch processing failed"),
        executorService);
  }

  public CompletableFuture<ProcessingResult> processImage(Path imagePath, Pipeline pipeline) {
    checkNotShutdown();
    LOGGER.debug("Starting processing of image: {}", imagePath);

    return CompletableFuture.supplyAsync(
        () ->
            executeWithErrorHandling(
                () -> pipelineExecutor.executeSingle(imagePath, pipeline),
                "Image processing failed for: " + imagePath),
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
   * Gets the event bus for subscribing to processing events.
   *
   * @return the event bus instance
   */
  public EventBus getEventBus() {
    return eventBus;
  }

  /**
   * Gets the resource manager for monitoring system resources.
   *
   * @return the resource manager instance
   */
  public ResourceManager getResourceManager() {
    return resourceManager;
  }

  /**
   * Checks if the engine is currently processing any tasks.
   *
   * @return true if processing is active, false otherwise
   */
  public boolean isProcessing() {
    return pipelineExecutor.isProcessing();
  }

  /**
   * Gets the current processing progress as a percentage.
   *
   * @return progress percentage (0.0 to 1.0), or 0.0 if not processing
   */
  public double getProgress() {
    return pipelineExecutor.getProgress();
  }

  /**
   * Cancels all currently running processing tasks.
   *
   * @return true if cancellation was successful, false otherwise
   */
  public boolean cancelProcessing() {
    LOGGER.info("Cancelling all processing tasks");
    return pipelineExecutor.cancelAll();
  }

  public void shutdown() {
    if (isShutdown) return;

    LOGGER.info("Shutting down SciPathJ Engine");
    isShutdown = true;

    try {
      pipelineExecutor.cancelAll();
      shutdownExecutorService();
      resourceManager.cleanup();
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
