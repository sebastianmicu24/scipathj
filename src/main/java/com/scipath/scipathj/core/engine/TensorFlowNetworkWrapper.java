package com.scipath.scipathj.core.engine;

import de.csbdresden.csbdeep.network.model.tensorflow.TensorFlowNetwork;
import de.csbdresden.csbdeep.task.Task;
import net.imagej.tensorflow.TensorFlowService;
import org.scijava.plugin.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.scipath.scipathj.core.utils.DirectFileLogger;

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
        DirectFileLogger.logTensorFlow("Costruttore chiamato con task: " +
            (associatedTask != null ? associatedTask.getClass().getSimpleName() : "null"));
    }
    
    
    @Override
    public void loadLibrary() {
        DirectFileLogger.logTensorFlow("=== INIZIO CARICAMENTO LIBRERIA TENSORFLOW ===");
        DirectFileLogger.logTensorFlow("Java Version: " + System.getProperty("java.version"));
        DirectFileLogger.logTensorFlow("Java Vendor: " + System.getProperty("java.vendor"));
        DirectFileLogger.logTensorFlow("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        
        LOGGER.info("Loading TensorFlow library with Java 21 compatibility fixes");
        
        try {
            // Apply comprehensive ClassLoader fixes before TensorFlow loading
            DirectFileLogger.logTensorFlow("Applicazione fix ClassLoader prima del caricamento TensorFlow");
            LOGGER.debug("Applying ClassLoader fixes before TensorFlow library loading");
            Java21ClassLoaderFix.applyFix();
            Java21ClassLoaderFix.forceURLClassLoaderEnvironment();
            
            String classLoaderInfo = Java21ClassLoaderFix.getClassLoaderInfo();
            DirectFileLogger.logTensorFlow("ClassLoader info: " + classLoaderInfo);
            LOGGER.debug("ClassLoader info before TensorFlow loading: {}", classLoaderInfo);
            
            // Store current thread context
            Thread currentThread = Thread.currentThread();
            ClassLoader originalContextClassLoader = currentThread.getContextClassLoader();
            try {
                // First try our custom TensorFlow library loader
                DirectFileLogger.logTensorFlow("Tentativo di caricamento TensorFlow con custom loader");
                LOGGER.debug("Attempting to load TensorFlow library using custom loader");
                boolean customLoadSuccess = TensorFlowLibraryLoader.loadTensorFlowLibrary();
                
                DirectFileLogger.logTensorFlow("Custom loader success: " + customLoadSuccess);
                
                if (customLoadSuccess) {
                    String loadedPath = TensorFlowLibraryLoader.getLoadedLibraryPath();
                    DirectFileLogger.logTensorFlow("TensorFlow library caricata con successo dal path: " + loadedPath);
                    LOGGER.info("TensorFlow library loaded successfully using custom loader");
                    LOGGER.info("Library loaded from: {}", loadedPath);
                    
                    // Still call the TensorFlow service to initialize properly
                    try {
                        DirectFileLogger.logTensorFlow("Inizializzazione TensorFlow service...");
                        DirectFileLogger.logTensorFlow("TensorFlow service class: " +
                            (tensorFlowService != null ? tensorFlowService.getClass().getName() : "null"));
                        
                        if (tensorFlowService != null) {
                            tensorFlowService.loadLibrary();
                            boolean serviceLoaded = tensorFlowService.getStatus().isLoaded();
                            DirectFileLogger.logTensorFlow("TensorFlow service loaded: " + serviceLoaded);
                            
                            if (serviceLoaded) {
                                String statusInfo = tensorFlowService.getStatus().getInfo();
                                DirectFileLogger.logTensorFlow("TensorFlow service info: " + statusInfo);
                                LOGGER.info("TensorFlow service initialized successfully");
                                log(statusInfo);
                                setTensorFlowLoaded(true);
                            } else {
                                // Even if service reports not loaded, we loaded the native library
                                DirectFileLogger.logTensorFlow("Service non caricato ma libreria nativa OK");
                                LOGGER.warn("TensorFlow service reports not loaded, but native library is loaded");
                                setTensorFlowLoaded(true);
                            }
                        } else {
                            DirectFileLogger.logTensorFlow("ERRORE: tensorFlowService è null!");
                            setTensorFlowLoaded(true); // Procediamo comunque se la libreria nativa è caricata
                        }
                    } catch (Exception serviceException) {
                        DirectFileLogger.logTensorFlowException("Eccezione durante inizializzazione service", serviceException);
                        LOGGER.warn("TensorFlow service initialization failed, but native library is loaded", serviceException);
                        setTensorFlowLoaded(true);
                    }
                } else {
                    String loaderStatus = TensorFlowLibraryLoader.getLoadingStatus();
                    DirectFileLogger.logTensorFlow("Custom loader fallito, status: " + loaderStatus);
                    DirectFileLogger.logTensorFlow("Tentativo con approccio standard...");
                    
                    LOGGER.warn("Custom TensorFlow library loader failed, trying standard approach");
                    LOGGER.debug("Custom loader status: {}", loaderStatus);
                    
                    // Fall back to original approach
                    if (tensorFlowService != null) {
                        tensorFlowService.loadLibrary();
                        
                        boolean serviceLoaded = tensorFlowService.getStatus().isLoaded();
                        DirectFileLogger.logTensorFlow("Standard approach - service loaded: " + serviceLoaded);
                        
                        if (serviceLoaded) {
                            String statusInfo = tensorFlowService.getStatus().getInfo();
                            DirectFileLogger.logTensorFlow("Standard approach - service info: " + statusInfo);
                            LOGGER.info("TensorFlow library loaded successfully using standard approach");
                            log(statusInfo);
                            setTensorFlowLoaded(true);
                        } else {
                            DirectFileLogger.logTensorFlow("ERRORE: Tutti gli approcci di caricamento falliti");
                            LOGGER.error("TensorFlow library failed to load using all approaches");
                            setTensorFlowLoaded(false);
                            handleTensorFlowLoadFailure();
                        }
                    } else {
                        DirectFileLogger.logTensorFlow("ERRORE: tensorFlowService è null per approccio standard");
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
                DirectFileLogger.log("TensorFlowNetworkWrapper", "ECCEZIONE durante caricamento TensorFlow: " +
                    e.getClass().getSimpleName() + " - " + e.getMessage());
                if (e.getCause() != null) {
                    DirectFileLogger.log("TensorFlowNetworkWrapper", "Causa: " + e.getCause().getClass().getSimpleName() +
                        " - " + e.getCause().getMessage());
                }
                LOGGER.error("Failed to load TensorFlow library with ClassLoader fixes", e);
                setTensorFlowLoaded(false);
                handleTensorFlowLoadFailure();
            }
            
            DirectFileLogger.log("TensorFlowNetworkWrapper", "=== FINE CARICAMENTO LIBRERIA TENSORFLOW ===");
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