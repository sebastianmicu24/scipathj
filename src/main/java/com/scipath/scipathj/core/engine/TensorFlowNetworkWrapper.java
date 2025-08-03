package com.scipath.scipathj.core.engine;

import de.csbdresden.csbdeep.network.model.tensorflow.TensorFlowNetwork;
import de.csbdresden.csbdeep.task.Task;
import net.imagej.tensorflow.TensorFlowService;
import org.scijava.plugin.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Wrapper for TensorFlowNetwork that applies Java 21 ClassLoader fixes
 * before loading the TensorFlow library.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class TensorFlowNetworkWrapper<T extends net.imglib2.type.numeric.RealType<T>> extends TensorFlowNetwork<T> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TensorFlowNetworkWrapper.class);
    
    @Parameter
    private TensorFlowService tensorFlowService;
    
    public TensorFlowNetworkWrapper(Task associatedTask) {
        super(associatedTask);
    }
    
    @Override
    public void loadLibrary() {
        LOGGER.info("Loading TensorFlow library with Java 21 compatibility fixes");
        
        try {
            // Apply comprehensive ClassLoader fixes before TensorFlow loading
            LOGGER.debug("Applying ClassLoader fixes before TensorFlow library loading");
            Java21ClassLoaderFix.applyFix();
            Java21ClassLoaderFix.forceURLClassLoaderEnvironment();
            
            LOGGER.debug("ClassLoader info before TensorFlow loading: {}",
                        Java21ClassLoaderFix.getClassLoaderInfo());
            
            // Store current thread context
            Thread currentThread = Thread.currentThread();
            ClassLoader originalContextClassLoader = currentThread.getContextClassLoader();
            
            try {
                // First try our custom TensorFlow library loader
                LOGGER.debug("Attempting to load TensorFlow library using custom loader");
                boolean customLoadSuccess = TensorFlowLibraryLoader.loadTensorFlowLibrary();
                
                if (customLoadSuccess) {
                    LOGGER.info("TensorFlow library loaded successfully using custom loader");
                    LOGGER.info("Library loaded from: {}", TensorFlowLibraryLoader.getLoadedLibraryPath());
                    
                    // Still call the TensorFlow service to initialize properly
                    try {
                        tensorFlowService.loadLibrary();
                        if (tensorFlowService.getStatus().isLoaded()) {
                            LOGGER.info("TensorFlow service initialized successfully");
                            log(tensorFlowService.getStatus().getInfo());
                            setTensorFlowLoaded(true);
                        } else {
                            // Even if service reports not loaded, we loaded the native library
                            LOGGER.warn("TensorFlow service reports not loaded, but native library is loaded");
                            setTensorFlowLoaded(true);
                        }
                    } catch (Exception serviceException) {
                        LOGGER.warn("TensorFlow service initialization failed, but native library is loaded", serviceException);
                        setTensorFlowLoaded(true);
                    }
                } else {
                    LOGGER.warn("Custom TensorFlow library loader failed, trying standard approach");
                    LOGGER.debug("Custom loader status: {}", TensorFlowLibraryLoader.getLoadingStatus());
                    
                    // Fall back to original approach
                    tensorFlowService.loadLibrary();
                    
                    if (tensorFlowService.getStatus().isLoaded()) {
                        LOGGER.info("TensorFlow library loaded successfully using standard approach");
                        log(tensorFlowService.getStatus().getInfo());
                        setTensorFlowLoaded(true);
                    } else {
                        LOGGER.error("TensorFlow library failed to load using all approaches");
                        setTensorFlowLoaded(false);
                        handleTensorFlowLoadFailure();
                    }
                }
                
            } finally {
                // Restore original context class loader if it was changed
                if (currentThread.getContextClassLoader() != originalContextClassLoader) {
                    LOGGER.debug("Restoring original context ClassLoader");
                    currentThread.setContextClassLoader(originalContextClassLoader);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load TensorFlow library with ClassLoader fixes", e);
            setTensorFlowLoaded(false);
            handleTensorFlowLoadFailure();
        }
    }
    
    /**
     * Handles TensorFlow library loading failure.
     */
    private void handleTensorFlowLoadFailure() {
        LOGGER.error("Could not load TensorFlow. Check previous errors and warnings for details.");
        
        // Get detailed loading status
        String customLoaderStatus = TensorFlowLibraryLoader.getLoadingStatus();
        Throwable lastException = TensorFlowLibraryLoader.getLastLoadException();
        
        LOGGER.error("Detailed TensorFlow loading status:\n{}", customLoaderStatus);
        if (lastException != null) {
            LOGGER.error("Last loading exception:", lastException);
        }
        
        // Show error dialog with detailed information
        String errorMessage = String.format(
            "<html>" +
            "<h3>TensorFlow Loading Failed</h3>" +
            "<p>Could not load TensorFlow library using any available method.</p>" +
            "<p><b>Java Version:</b> %s</p>" +
            "<p><b>ClassLoader Info:</b> %s</p>" +
            "<p><b>Custom Loader Status:</b> %s</p>" +
            "<p><b>Possible Solutions:</b></p>" +
            "<ul>" +
            "<li>Ensure TensorFlow native libraries are available</li>" +
            "<li>Check Java module system compatibility</li>" +
            "<li>Verify system architecture compatibility</li>" +
            "<li>Try running with --add-opens JVM arguments</li>" +
            "<li>Check java.library.path system property</li>" +
            "</ul>" +
            "<p>Opening the TensorFlow Library Management tool...</p>" +
            "</html>",
            System.getProperty("java.version"),
            Java21ClassLoaderFix.getClassLoaderInfo(),
            TensorFlowLibraryLoader.isLibraryLoaded() ? "Loaded" : "Failed"
        );
        
        JOptionPane.showMessageDialog(null, errorMessage,
                                    "TensorFlow Loading Failed",
                                    JOptionPane.ERROR_MESSAGE);
        
        // Try to open TensorFlow management tool
        try {
            if (getCommandService() != null) {
                getCommandService().run(
                    net.imagej.tensorflow.ui.TensorFlowLibraryManagementCommand.class, true);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not open TensorFlow Library Management tool", e);
        }
    }
    
    /**
     * Sets the tensorFlowLoaded flag using reflection since it's private in parent.
     */
    private void setTensorFlowLoaded(boolean loaded) {
        try {
            java.lang.reflect.Field field = TensorFlowNetwork.class.getDeclaredField("tensorFlowLoaded");
            field.setAccessible(true);
            field.set(this, loaded);
        } catch (Exception e) {
            LOGGER.warn("Could not set tensorFlowLoaded field", e);
        }
    }
    
    /**
     * Gets the CommandService using reflection since it's private in parent.
     */
    private org.scijava.command.CommandService getCommandService() {
        try {
            java.lang.reflect.Field field = TensorFlowNetwork.class.getDeclaredField("commandService");
            field.setAccessible(true);
            return (org.scijava.command.CommandService) field.get(this);
        } catch (Exception e) {
            LOGGER.warn("Could not get CommandService field", e);
            return null;
        }
    }
}