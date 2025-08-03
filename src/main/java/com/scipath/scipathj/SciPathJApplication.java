package com.scipath.scipathj;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.config.MainSettings;
import com.scipath.scipathj.core.engine.SciPathJEngine;
import com.scipath.scipathj.core.engine.Java21ClassLoaderFix;
import com.scipath.scipathj.core.engine.ClassLoaderDebugger;
import com.scipath.scipathj.core.engine.TensorFlowLibraryLoader;
import com.scipath.scipathj.ui.main.MainWindow;
import com.scipath.scipathj.ui.themes.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Main application entry point for SciPathJ.
 * 
 * <p>SciPathJ (Segmentation and Classification of Images, Pipelines for the Analysis 
 * of Tissue Histopathology) is a comprehensive image analysis software for 
 * histopathological research.</p>
 * 
 * <p>This application provides automated analysis workflows for tissue images,
 * including segmentation, feature extraction, and machine learning-based classification.</p>
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class SciPathJApplication {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SciPathJApplication.class);
    
    /**
     * Application entry point.
     * 
     * @param args command line arguments (currently unused)
     */
    public static void main(String[] args) {
        LOGGER.info("Starting SciPathJ Application v1.0.0");
        try {
            // Comprehensive ClassLoader debugging
            LOGGER.info("Starting comprehensive ClassLoader debugging");
            ClassLoaderDebugger.debugClassLoaderEnvironment();
            ClassLoaderDebugger.identifyPotentialIssues();
            
            // Apply Java 21 ClassLoader compatibility fix early in startup
            LOGGER.info("Applying Java 21 ClassLoader compatibility fix");
            Java21ClassLoaderFix.applyFix();
            
            // Force URLClassLoader environment as additional safety measure
            Java21ClassLoaderFix.forceURLClassLoaderEnvironment();
            
            LOGGER.info("ClassLoader info after fixes: {}", Java21ClassLoaderFix.getClassLoaderInfo());
            
            // Preload TensorFlow library early in startup
            LOGGER.info("Attempting to preload TensorFlow library");
            boolean tensorFlowPreloaded = TensorFlowLibraryLoader.loadTensorFlowLibrary();
            if (tensorFlowPreloaded) {
                LOGGER.info("TensorFlow library preloaded successfully: {}",
                           TensorFlowLibraryLoader.getLoadedLibraryPath());
            } else {
                LOGGER.warn("TensorFlow library preloading failed, will try again during StarDist initialization");
                LOGGER.debug("TensorFlow loading status: {}", TensorFlowLibraryLoader.getLoadingStatus());
            }
            
            // Set system properties for better UI experience
            setupSystemProperties();
            
            // Initialize theme manager
            ThemeManager.initializeTheme();
            
            // Start application on EDT
            SwingUtilities.invokeLater(SciPathJApplication::createAndShowGUI);
            
        } catch (Exception e) {
            LOGGER.error("Failed to start SciPathJ application", e);
            
            // Additional debugging on failure
            LOGGER.error("Performing additional debugging due to startup failure");
            try {
                ClassLoaderDebugger.debugClassLoaderEnvironment();
            } catch (Exception debugException) {
                LOGGER.error("Even debugging failed", debugException);
            }
            
            showErrorDialog("Application Startup Error",
                          "Failed to start SciPathJ: " + e.getMessage());
            System.exit(1);
        }
        
    }
    
    /**
     * Sets up system properties for optimal UI experience and Java module compatibility.
     */
    private static void setupSystemProperties() {
        // Enable high-DPI support
        System.setProperty("sun.java2d.uiScale", "1.0");
        
        // Improve font rendering
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        // Enable hardware acceleration
        System.setProperty("sun.java2d.opengl", "true");
        
        // Set application name for OS integration
        System.setProperty("apple.awt.application.name", "SciPathJ");
        
        // Java module system compatibility for TensorFlow and legacy libraries
        System.setProperty("java.system.class.loader", "java.lang.ClassLoader");
        System.setProperty("jdk.module.illegalAccess", "permit");
        System.setProperty("jdk.module.illegalAccess.silent", "true");
        
        // TensorFlow specific compatibility settings
        System.setProperty("tensorflow.eager", "false");
        System.setProperty("org.tensorflow.NativeLibrary.DEBUG", "false");
        
        LOGGER.debug("System properties configured for optimal UI experience and Java module compatibility");
    }
    
    /**
     * Creates and displays the main application GUI.
     * This method runs on the Event Dispatch Thread.
     */
    private static void createAndShowGUI() {
        try {
            LOGGER.info("Initializing main application window");
            
            // Initialize configuration manager and load all settings
            ConfigurationManager configManager = ConfigurationManager.getInstance();
            configManager.initializeVesselSegmentationSettings();
            configManager.initializeMainSettings();
            LOGGER.info("Configuration manager initialized and all settings loaded");
            
            // Initialize core engine
            SciPathJEngine engine = SciPathJEngine.getInstance();
            
            // Create main window
            MainWindow mainWindow = new MainWindow(engine);
            
            // Configure main window
            mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainWindow.setLocationRelativeTo(null);
            
            // Add shutdown hook for clean resource cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutting down SciPathJ application");
                engine.shutdown();
                // Restore original ClassLoader on shutdown
                Java21ClassLoaderFix.restoreOriginalClassLoader();
            }));
            
            // Show the application
            mainWindow.setVisible(true);
            
            LOGGER.info("SciPathJ application started successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to create main application window", e);
            showErrorDialog("GUI Initialization Error", 
                          "Failed to initialize user interface: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * Shows an error dialog to the user.
     * 
     * @param title the dialog title
     * @param message the error message to display
     */
    private static void showErrorDialog(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                null,
                message,
                title,
                JOptionPane.ERROR_MESSAGE
            );
        });
    }
}