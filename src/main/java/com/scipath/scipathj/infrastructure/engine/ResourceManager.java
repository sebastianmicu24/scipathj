package com.scipath.scipathj.infrastructure.engine;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages system resources and monitors memory usage during image processing.
 *
 * <p>This class provides utilities for monitoring system resources, managing memory
 * usage, and preventing out-of-memory conditions during intensive image processing
 * operations.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class ResourceManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManager.class);

  private static final double MEMORY_WARNING_THRESHOLD = 0.8; // 80%
  private static final double MEMORY_CRITICAL_THRESHOLD = 0.9; // 90%

  private final MemoryMXBean memoryBean;
  private final ScheduledExecutorService monitoringService;
  private final Runtime runtime;

  private volatile boolean isMonitoring = false;

  /**
   * Creates a new ResourceManager instance.
   */
  public ResourceManager() {
    this.memoryBean = ManagementFactory.getMemoryMXBean();
    this.runtime = Runtime.getRuntime();
    this.monitoringService =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "ResourceMonitor");
              t.setDaemon(true);
              return t;
            });

    startMonitoring();
    LOGGER.info("ResourceManager initialized");
  }

  /**
   * Starts continuous resource monitoring.
   */
  private void startMonitoring() {
    if (isMonitoring) {
      return;
    }

    isMonitoring = true;
    monitoringService.scheduleAtFixedRate(this::checkMemoryUsage, 0, 5, TimeUnit.SECONDS);
    LOGGER.debug("Resource monitoring started");
  }

  /**
   * Checks current memory usage and logs warnings if thresholds are exceeded.
   */
  @SuppressWarnings("CallToSystemGC")
  private void checkMemoryUsage() {
    try {
      double memoryUsageRatio = getMemoryUsageRatio();

      if (memoryUsageRatio > MEMORY_CRITICAL_THRESHOLD) {
        LOGGER.error(
            "CRITICAL: Memory usage at {:.1f}% - processing may fail", memoryUsageRatio * 100);
        // Force garbage collection in critical situations
        System.gc();
      } else if (memoryUsageRatio > MEMORY_WARNING_THRESHOLD) {
        LOGGER.warn(
            "WARNING: Memory usage at {:.1f}% - consider reducing batch size",
            memoryUsageRatio * 100);
      }

    } catch (Exception e) {
      LOGGER.debug("Error checking memory usage", e);
    }
  }

  /**
   * Gets the current memory usage ratio.
   *
   * @return memory usage as a ratio (0.0 to 1.0)
   */
  public double getMemoryUsageRatio() {
    MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    long used = heapUsage.getUsed();
    long max = heapUsage.getMax();

    if (max <= 0) {
      // If max is not available, use committed memory
      max = heapUsage.getCommitted();
    }

    return max > 0 ? (double) used / max : 0.0;
  }

  /**
   * Gets the current memory usage in MB.
   *
   * @return current memory usage in megabytes
   */
  public long getMemoryUsageMB() {
    return memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
  }

  /**
   * Gets the maximum available memory in MB.
   *
   * @return maximum memory in megabytes
   */
  public long getMaxMemoryMB() {
    long max = memoryBean.getHeapMemoryUsage().getMax();
    if (max <= 0) {
      max = runtime.maxMemory();
    }
    return max / (1024 * 1024);
  }

  /**
   * Gets the number of available processor cores.
   *
   * @return number of available processors
   */
  public int getAvailableProcessors() {
    return runtime.availableProcessors();
  }

  /**
   * Checks if there is sufficient memory available for processing.
   *
   * @param requiredMemoryMB estimated memory requirement in MB
   * @return true if sufficient memory is available
   */
  public boolean hasSufficientMemory(long requiredMemoryMB) {
    long availableMemory = getMaxMemoryMB() - getMemoryUsageMB();
    return availableMemory > requiredMemoryMB;
  }

  /**
   * Estimates memory requirements for processing an image.
   *
   * @param imageWidth image width in pixels
   * @param imageHeight image height in pixels
   * @param channels number of image channels
   * @return estimated memory requirement in MB
   */
  public long estimateImageMemoryMB(int imageWidth, int imageHeight, int channels) {
    // Rough estimation:
    // - Original image: width * height * channels * 4 bytes (float)
    // - Processing buffers: 2x original size
    // - ROI storage: ~10% of image size
    // - Feature data: minimal compared to image data

    long pixelCount = (long) imageWidth * imageHeight * channels;
    long bytesPerPixel = 4; // Assuming float processing
    long baseMemory = pixelCount * bytesPerPixel;
    long totalMemory = baseMemory * 3; // 3x for processing overhead

    return totalMemory / (1024 * 1024); // Convert to MB
  }

  /**
   * Suggests an optimal batch size based on available memory and image size.
   *
   * @param imageWidth typical image width
   * @param imageHeight typical image height
   * @param channels number of image channels
   * @return suggested batch size
   */
  public int suggestBatchSize(int imageWidth, int imageHeight, int channels) {
    long memoryPerImage = estimateImageMemoryMB(imageWidth, imageHeight, channels);
    long availableMemory =
        (long) ((getMaxMemoryMB() - getMemoryUsageMB()) * 0.7); // Use 70% of available

    if (memoryPerImage <= 0) {
      return 1;
    }

    int batchSize = (int) Math.max(1, availableMemory / memoryPerImage);

    // Cap batch size to reasonable limits
    batchSize = Math.min(batchSize, 10);

    LOGGER.debug(
        "Suggested batch size: {} (memory per image: {}MB, available: {}MB)",
        batchSize,
        memoryPerImage,
        availableMemory);

    return batchSize;
  }

  /**
   * Forces garbage collection and logs memory statistics.
   *
   * <p>This method should be used sparingly, typically only when memory
   * usage is critical or between processing batches.</p>
   */
  @SuppressWarnings("CallToSystemGC")
  public void forceGarbageCollection() {
    long beforeGC = getMemoryUsageMB();

    System.gc();

    // Give GC time to complete
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    long afterGC = getMemoryUsageMB();
    long freed = beforeGC - afterGC;

    LOGGER.info(
        "Garbage collection completed. Freed {}MB memory ({}MB -> {}MB)", freed, beforeGC, afterGC);
  }

  /**
   * Gets a summary of current resource usage.
   *
   * @return resource usage summary string
   */
  public String getResourceSummary() {
    return String.format(
        "Memory: %dMB/%dMB (%.1f%%), Processors: %d",
        getMemoryUsageMB(),
        getMaxMemoryMB(),
        getMemoryUsageRatio() * 100,
        getAvailableProcessors());
  }

  /**
   * Cleans up resources and stops monitoring.
   */
  public void cleanup() {
    LOGGER.info("Cleaning up ResourceManager");

    isMonitoring = false;
    monitoringService.shutdown();

    try {
      if (!monitoringService.awaitTermination(5, TimeUnit.SECONDS)) {
        monitoringService.shutdownNow();
      }
    } catch (InterruptedException e) {
      monitoringService.shutdownNow();
      Thread.currentThread().interrupt();
    }

    LOGGER.debug("ResourceManager cleanup complete");
  }
}
