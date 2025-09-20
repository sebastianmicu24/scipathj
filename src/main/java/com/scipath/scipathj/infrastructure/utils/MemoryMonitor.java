package com.scipath.scipathj.infrastructure.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Memory monitoring utility for tracking and managing memory usage.
 * Provides methods to check memory status and suggest garbage collection.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public class MemoryMonitor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryMonitor.class);
    
    // Configuration constants
    private static final double DEFAULT_WARNING_THRESHOLD = 0.8; // 80% of max memory
    private static final double DEFAULT_CRITICAL_THRESHOLD = 0.9; // 90% of max memory
    
    // Singleton instance
    private static volatile MemoryMonitor instance;
    
    private final double warningThreshold;
    private final double criticalThreshold;
    
    /**
     * Private constructor for singleton pattern.
     */
    private MemoryMonitor(double warningThreshold, double criticalThreshold) {
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
    }
    
    /**
     * Gets the singleton instance with default thresholds.
     */
    public static MemoryMonitor getInstance() {
        return getInstance(DEFAULT_WARNING_THRESHOLD, DEFAULT_CRITICAL_THRESHOLD);
    }
    
    /**
     * Gets the singleton instance with custom thresholds.
     */
    public static MemoryMonitor getInstance(double warningThreshold, double criticalThreshold) {
        if (instance == null) {
            synchronized (MemoryMonitor.class) {
                if (instance == null) {
                    instance = new MemoryMonitor(warningThreshold, criticalThreshold);
                }
            }
        }
        return instance;
    }
    
    /**
     * Gets current memory usage information.
     */
    public MemoryInfo getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return new MemoryInfo(maxMemory, totalMemory, usedMemory, freeMemory);
    }
    
    /**
     * Checks current memory usage and suggests actions if thresholds are exceeded.
     */
    public MemoryStatus checkMemoryUsage() {
        MemoryInfo info = getMemoryInfo();
        double usageRatio = (double) info.usedMemory() / info.maxMemory();
        
        if (usageRatio >= criticalThreshold) {
            LOGGER.warn("CRITICAL: Memory usage at {:.1f}% ({} MB used of {} MB max)", 
                       usageRatio * 100, info.usedMemory() / (1024 * 1024), info.maxMemory() / (1024 * 1024));
            
            // Force garbage collection at critical level
            System.gc();
            
            // Check again after GC
            MemoryInfo afterGC = getMemoryInfo();
            double afterGCRatio = (double) afterGC.usedMemory() / afterGC.maxMemory();
            
            LOGGER.info("After GC: Memory usage at {:.1f}% ({} MB used)", 
                       afterGCRatio * 100, afterGC.usedMemory() / (1024 * 1024));
            
            return MemoryStatus.CRITICAL;
            
        } else if (usageRatio >= warningThreshold) {
            LOGGER.warn("WARNING: Memory usage at {:.1f}% ({} MB used of {} MB max)", 
                       usageRatio * 100, info.usedMemory() / (1024 * 1024), info.maxMemory() / (1024 * 1024));
            
            // Suggest garbage collection at warning level
            System.gc();
            
            return MemoryStatus.WARNING;
            
        } else {
            LOGGER.debug("Memory usage normal: {:.1f}% ({} MB used of {} MB max)", 
                        usageRatio * 100, info.usedMemory() / (1024 * 1024), info.maxMemory() / (1024 * 1024));
            
            return MemoryStatus.NORMAL;
        }
    }
    
    /**
     * Logs detailed memory information.
     */
    public void logMemoryDetails() {
        MemoryInfo info = getMemoryInfo();
        double usagePercentage = ((double) info.usedMemory() / info.maxMemory()) * 100;
        
        LOGGER.info("=== Memory Status ===");
        LOGGER.info("Max Memory:   {} MB", info.maxMemory() / (1024 * 1024));
        LOGGER.info("Total Memory: {} MB", info.totalMemory() / (1024 * 1024));
        LOGGER.info("Used Memory:  {} MB ({:.1f}%)", info.usedMemory() / (1024 * 1024), usagePercentage);
        LOGGER.info("Free Memory:  {} MB", info.freeMemory() / (1024 * 1024));
        LOGGER.info("====================");
    }
    
    /**
     * Suggests garbage collection if memory usage is above warning threshold.
     */
    public void suggestGCIfNeeded() {
        MemoryInfo info = getMemoryInfo();
        double usageRatio = (double) info.usedMemory() / info.maxMemory();
        
        if (usageRatio >= warningThreshold) {
            LOGGER.info("Memory usage high ({:.1f}%), suggesting garbage collection", usageRatio * 100);
            System.gc();
        }
    }
    
    /**
     * Monitors memory usage in a background thread (useful for long-running operations).
     */
    public void startBackgroundMonitoring(long intervalMs) {
        Thread monitorThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    checkMemoryUsage();
                    Thread.sleep(intervalMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.info("Memory monitoring thread interrupted");
            }
        }, "MemoryMonitor-Thread");
        
        monitorThread.setDaemon(true);
        monitorThread.start();
        
        LOGGER.info("Started background memory monitoring with {}ms interval", intervalMs);
    }
    
    /**
     * Memory information record.
     */
    public record MemoryInfo(
        long maxMemory,
        long totalMemory,
        long usedMemory,
        long freeMemory
    ) {
        public double getUsagePercentage() {
            return ((double) usedMemory / maxMemory) * 100;
        }
        
        public String getUsageSummary() {
            return String.format("%.1f%% (%d MB used of %d MB max)", 
                getUsagePercentage(), usedMemory / (1024 * 1024), maxMemory / (1024 * 1024));
        }
    }
    
    /**
     * Memory status enumeration.
     */
    public enum MemoryStatus {
        NORMAL("Memory usage is normal"),
        WARNING("Memory usage is high - consider cleanup"),
        CRITICAL("Memory usage is critical - immediate action required");
        
        private final String description;
        
        MemoryStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}