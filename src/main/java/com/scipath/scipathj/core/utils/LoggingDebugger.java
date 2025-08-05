package com.scipath.scipathj.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class to debug and ensure logging is working properly in packaged applications.
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class LoggingDebugger {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingDebugger.class);
    
    /**
     * Initialize logging and create debug files to ensure logging is working.
     */
    public static void initializeAndTestLogging() {
        LOGGER.info("=== LoggingDebugger: Starting logging initialization ===");
        
        // Create log directory on Desktop
        String userHome = System.getProperty("user.home");
        File logDir = new File(userHome, "Desktop/scipathj-logs");
        
        LOGGER.info("Creating log directory: {}", logDir.getAbsolutePath());
        
        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            LOGGER.info("Log directory created: {}", created);
            
            if (!created) {
                LOGGER.error("Failed to create log directory: {}", logDir.getAbsolutePath());
                // Try alternative location
                logDir = new File(userHome, "scipathj-logs");
                LOGGER.info("Trying alternative location: {}", logDir.getAbsolutePath());
                created = logDir.mkdirs();
                LOGGER.info("Alternative directory created: {}", created);
            }
        } else {
            LOGGER.info("Log directory already exists: {}", logDir.getAbsolutePath());
        }
        
        // Write a simple test file to verify write permissions
        writeTestFile(logDir);
        
        // Log system information
        logSystemInformation();
        
        LOGGER.info("=== LoggingDebugger: Logging initialization complete ===");
    }
    
    /**
     * Write a test file to verify write permissions.
     */
    private static void writeTestFile(File logDir) {
        try {
            File testFile = new File(logDir, "logging-test.txt");
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write("SciPathJ Logging Test\n");
                writer.write("Timestamp: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n");
                writer.write("Log directory: " + logDir.getAbsolutePath() + "\n");
                writer.write("Java version: " + System.getProperty("java.version") + "\n");
                writer.write("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n");
                writer.write("User: " + System.getProperty("user.name") + "\n");
                writer.write("Working directory: " + System.getProperty("user.dir") + "\n");
                writer.write("\nThis file confirms that SciPathJ can write to the log directory.\n");
                writer.write("If you can see this file, logging should be working.\n");
            }
            
            LOGGER.info("Test file created successfully: {}", testFile.getAbsolutePath());
            
        } catch (IOException e) {
            LOGGER.error("Failed to create test file in log directory", e);
        }
    }
    
    /**
     * Log important system information for debugging.
     */
    private static void logSystemInformation() {
        LOGGER.info("=== System Information ===");
        LOGGER.info("Java Version: {}", System.getProperty("java.version"));
        LOGGER.info("Java Vendor: {}", System.getProperty("java.vendor"));
        LOGGER.info("Java Home: {}", System.getProperty("java.home"));
        LOGGER.info("OS Name: {}", System.getProperty("os.name"));
        LOGGER.info("OS Version: {}", System.getProperty("os.version"));
        LOGGER.info("OS Architecture: {}", System.getProperty("os.arch"));
        LOGGER.info("User Name: {}", System.getProperty("user.name"));
        LOGGER.info("User Home: {}", System.getProperty("user.home"));
        LOGGER.info("User Directory: {}", System.getProperty("user.dir"));
        LOGGER.info("Java Class Path: {}", System.getProperty("java.class.path"));
        LOGGER.info("Java Library Path: {}", System.getProperty("java.library.path"));
        LOGGER.info("Temp Directory: {}", System.getProperty("java.io.tmpdir"));
        
        // Log memory information
        Runtime runtime = Runtime.getRuntime();
        LOGGER.info("Available Processors: {}", runtime.availableProcessors());
        LOGGER.info("Max Memory: {} MB", runtime.maxMemory() / (1024 * 1024));
        LOGGER.info("Total Memory: {} MB", runtime.totalMemory() / (1024 * 1024));
        LOGGER.info("Free Memory: {} MB", runtime.freeMemory() / (1024 * 1024));
        
        LOGGER.info("=== End System Information ===");
    }
    
    /**
     * Test StarDist specific logging.
     */
    public static void testStarDistLogging() {
        LOGGER.info("=== StarDist Logging Test ===");
        
        // Test different log levels
        LOGGER.debug("DEBUG: StarDist debug message test");
        LOGGER.info("INFO: StarDist info message test");
        LOGGER.warn("WARN: StarDist warning message test");
        LOGGER.error("ERROR: StarDist error message test");
        
        // Test System.out capture
        System.out.println("=== StarDist System.out test message ===");
        System.out.println("This should appear in system-output.log");
        System.err.println("=== StarDist System.err test message ===");
        System.err.println("This should also appear in system-output.log");
        
        LOGGER.info("=== End StarDist Logging Test ===");
    }
}