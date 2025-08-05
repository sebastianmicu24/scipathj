package com.scipath.scipathj.core.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Direct file logger that bypasses SLF4J/Logback for guaranteed logging in packaged applications.
 * This ensures we can always capture debug information regardless of logging configuration issues.
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class DirectFileLogger {
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static File logDir;
    private static boolean initialized = false;
    
    /**
     * Initialize the direct file logger.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            String userHome = System.getProperty("user.home");
            logDir = new File(userHome, "Desktop/scipathj-logs");
            
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            initialized = true;
            
            // Write initialization message
            writeToFile("direct-debug.log", "INFO", "DirectFileLogger", "Direct file logger initialized successfully");
            writeToFile("direct-debug.log", "INFO", "DirectFileLogger", "Log directory: " + logDir.getAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("Failed to initialize DirectFileLogger: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Write a log message directly to a file.
     */
    public static void writeToFile(String filename, String level, String logger, String message) {
        if (!initialized) {
            initialize();
        }
        
        try {
            File logFile = new File(logDir, filename);
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String logLine = String.format("%s [%s] %-5s %s - %s%n", 
                                         timestamp, Thread.currentThread().getName(), level, logger, message);
            
            try (FileWriter fw = new FileWriter(logFile, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.print(logLine);
                pw.flush();
            }
            
        } catch (IOException e) {
            System.err.println("Failed to write to log file " + filename + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Write an exception to a log file.
     */
    public static void writeException(String filename, String level, String logger, String message, Throwable throwable) {
        if (!initialized) {
            initialize();
        }
        
        try {
            File logFile = new File(logDir, filename);
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            
            try (FileWriter fw = new FileWriter(logFile, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                
                pw.printf("%s [%s] %-5s %s - %s%n", 
                         timestamp, Thread.currentThread().getName(), level, logger, message);
                
                if (throwable != null) {
                    throwable.printStackTrace(pw);
                }
                
                pw.flush();
            }
            
        } catch (IOException e) {
            System.err.println("Failed to write exception to log file " + filename + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Log StarDist specific information.
     */
    public static void logStarDist(String level, String message) {
        writeToFile("stardist-direct.log", level, "StarDist", message);
    }
    
    /**
     * Log StarDist exception.
     */
    public static void logStarDistException(String message, Throwable throwable) {
        writeException("stardist-direct.log", "ERROR", "StarDist", message, throwable);
    }
    
    /**
     * Log TensorFlow specific information.
     */
    public static void logTensorFlow(String message) {
        writeToFile("tensorflow-direct.log", "INFO", "TensorFlow", message);
    }
    
    /**
     * Log TensorFlow exception.
     */
    public static void logTensorFlowException(String message, Throwable throwable) {
        writeException("tensorflow-direct.log", "ERROR", "TensorFlow", message, throwable);
    }
    
    /**
     * Log exception with custom logger name and message.
     */
    public static void logException(String loggerName, String message, Throwable throwable) {
        writeException("exceptions-direct.log", "ERROR", loggerName, message, throwable);
    }
    
    /**
     * Log system information.
     */
    public static void logSystemInfo() {
        writeToFile("system-info.log", "INFO", "System", "=== System Information ===");
        writeToFile("system-info.log", "INFO", "System", "Java Version: " + System.getProperty("java.version"));
        writeToFile("system-info.log", "INFO", "System", "Java Vendor: " + System.getProperty("java.vendor"));
        writeToFile("system-info.log", "INFO", "System", "Java Home: " + System.getProperty("java.home"));
        writeToFile("system-info.log", "INFO", "System", "OS Name: " + System.getProperty("os.name"));
        writeToFile("system-info.log", "INFO", "System", "OS Version: " + System.getProperty("os.version"));
        writeToFile("system-info.log", "INFO", "System", "OS Architecture: " + System.getProperty("os.arch"));
        writeToFile("system-info.log", "INFO", "System", "User Name: " + System.getProperty("user.name"));
        writeToFile("system-info.log", "INFO", "System", "User Home: " + System.getProperty("user.home"));
        writeToFile("system-info.log", "INFO", "System", "User Directory: " + System.getProperty("user.dir"));
        writeToFile("system-info.log", "INFO", "System", "Working Directory: " + new File(".").getAbsolutePath());
        
        // Log memory information
        Runtime runtime = Runtime.getRuntime();
        writeToFile("system-info.log", "INFO", "System", "Available Processors: " + runtime.availableProcessors());
        writeToFile("system-info.log", "INFO", "System", "Max Memory: " + (runtime.maxMemory() / (1024 * 1024)) + " MB");
        writeToFile("system-info.log", "INFO", "System", "Total Memory: " + (runtime.totalMemory() / (1024 * 1024)) + " MB");
        writeToFile("system-info.log", "INFO", "System", "Free Memory: " + (runtime.freeMemory() / (1024 * 1024)) + " MB");
        
        writeToFile("system-info.log", "INFO", "System", "=== End System Information ===");
    }
    
    /**
     * Convenience method for logging with logger name and message.
     * Uses a default debug log file.
     */
    public static void log(String loggerName, String message) {
        writeToFile("debug.log", "INFO", loggerName, message);
    }
    
    /**
     * Convenience method for logging with logger name, level and message.
     */
    public static void log(String loggerName, String level, String message) {
        writeToFile("debug.log", level, loggerName, message);
    }
}