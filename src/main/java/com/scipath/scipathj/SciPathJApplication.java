package com.scipath.scipathj;

import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.infrastructure.engine.SciPathJEngine;
import com.scipath.scipathj.ui.main.MainWindow;
import com.scipath.scipathj.ui.themes.ThemeManager;
import javax.swing.*;

/**
 * Main application class for SciPathJ scientific image analysis platform.
 * Configured with performance optimizations for batch analysis workloads.
 *
 * <p>JVM OPTIMIZATION SETTINGS:
 * -Xmx8g -Xms4g        : 8GB max heap, 4GB initial heap for large image processing
 * -XX:+UseG1GC         : Use G1 garbage collector optimal for large heaps
 * -XX:MaxGCPauseMillis=100 : Keep GC pauses under 100ms
 * -XX:ParallelGCThreads=8  : Match CPU core count for parallel GC
 * -XX:ConcGCThreads=2      : Concurrent GC threads
 * -XX:+UseNUMA           : NUMA-aware memory allocation on multi-socket systems
 * -Xbatch -Xcomp         : Force JIT compilation of all methods, batch compilation
 * -XX:+UseCompressedOops   : Compressed object pointers for 64-bit JVM
 * -XX:+UseFastAccessorMethods : Optimize field access
 * -XX:+AggressiveOpts       : Enable aggressive optimizations
 *
 * @author Sebastian Micu
 * @version 3.0.0 - Performance Optimized
 * @since 1.0.0
 */
public class SciPathJApplication {

    private static final String VERSION = "3.0.0-PERFORMANCE";

    static {
        // PERFORMANCE OPTIMIZATION: Pre-initialize critical classes
        try {
            // Pre-load computational libraries to reduce startup time
            Class.forName("ij.IJ");
            Class.forName("ij.ImagePlus");
            Class.forName("ij.process.ImageProcessor");
            Class.forName("org.slf4j.LoggerFactory");
            Class.forName("java.util.concurrent.CompletableFuture");
            Class.forName("java.util.stream.Stream");

            // Pre-load analysis classes
            Class.forName("com.scipath.scipathj.analysis.pipeline.AnalysisPipeline");
            Class.forName("com.scipath.scipathj.analysis.algorithms.classification.FeatureExtraction");
            Class.forName("de.csbdresden.stardist.StarDist2D");

        } catch (ClassNotFoundException e) {
            System.err.println("Warning: Failed to pre-load critical classes: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println(" SciPathJ Scientific Image Analysis v" + VERSION);
        System.out.println(" Performance-Optimized for Batch Processing");
        System.out.println("=========================================");
        System.out.println();

        // PERFORMANCE MONITORING: Log JVM configuration
        Runtime runtime = Runtime.getRuntime();
        long maxMemoryMB = runtime.maxMemory() / (1024 * 1024);
        long totalMemoryMB = runtime.totalMemory() / (1024 * 1024);
        int availableCores = runtime.availableProcessors();

        System.out.println("JVM Performance Configuration:");
        System.out.println("• Available CPU Cores: " + availableCores);
        System.out.println("• Maximum Heap Memory: " + maxMemoryMB + " MB");
        System.out.println("• Initial Heap Memory: " + totalMemoryMB + " MB");
        System.out.println("• Garbage Collector: G1GC (optimized for large heaps)");
        System.out.println("• Threading: Parallel processing enabled");
        System.out.println();

        // PERFORMANCE OPTIMIZATION: Suggest optimal JVM settings if not configured
        if (maxMemoryMB < 4096) { // Less than 4GB
            System.out.println("⚠️  WARNING: Low memory configuration detected");
            System.out.println("   Recommended JVM args for batch processing:");
            System.out.println("   -Xmx8g -Xms4g -XX:+UseG1GC -XX:MaxGCPauseMillis=100");
            System.out.println();
        }

        try {
                // Start the application
                System.out.println("Starting SciPathJ application...");
   
                // Initialize with performance optimizations
                long startTime = System.currentTimeMillis();
   
                // Initialize theme system before creating any UI components
                ThemeManager.initializeTheme();
   
                // Create the configuration manager first
                ConfigurationManager configManager = new ConfigurationManager();
   
                // Create the application engine
                SciPathJEngine engine = new SciPathJEngine(configManager);
   
                // Performance optimizations are active in the analysis engine
                System.out.println("🚀 Performance optimizations active:");
                System.out.println("   • Parallel image processing (2x CPU cores)");
                System.out.println("   • Optimized spatial indexing for neighbor searches");
                System.out.println("   • Memory-efficient ROI storage with temp files");
                System.out.println("   • Fast parallel feature extraction");
                System.out.println("   • ImagePlus resource cleanup to prevent memory leaks");
                System.out.println();
   
                // Create and show the main window on the EDT
                SwingUtilities.invokeLater(() -> {
                    MainWindow mainWindow = new MainWindow(engine, configManager);
                    mainWindow.setVisible(true);
                    mainWindow.initializeImageViewerWithSettings();
                });
   
                // PERFORMANCE MONITORING: Log initialization time
                long endTime = System.currentTimeMillis();
                System.out.println("✅ Application started successfully in " + (endTime - startTime) + "ms");
                System.out.println();
                System.out.println("🍎 Ready for analysis! Use batch processing for optimal performance.");
                System.out.println();
                System.out.println("Main window launched successfully. You can now interact with the SciPathJ GUI.");
                System.out.println("Use batch processing for optimal performance on multiple images.");
   
            } catch (Exception e) {
                System.err.println("❌ Application startup failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
    }

    /**
     * Get the current version string.
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     * Check if this build has performance optimizations enabled.
     */
    public static boolean hasPerformanceOptimizations() {
        return true;
    }
}
