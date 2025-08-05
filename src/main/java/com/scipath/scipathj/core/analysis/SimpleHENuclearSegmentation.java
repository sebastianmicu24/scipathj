package com.scipath.scipathj.core.analysis;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.config.NuclearSegmentationSettings;
import com.scipath.scipathj.core.config.SegmentationConstants;
import com.scipath.scipathj.data.model.NucleusROI;
import com.scipath.scipathj.ui.components.ROIManager;
import com.scipath.scipathj.core.utils.DirectFileLogger;
import de.csbdresden.stardist.StarDist2D;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.command.CommandModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Step 2 of the analysis pipeline: Nuclear Segmentation.
 * 
 * Simplified H&E nuclear segmentation using StarDist's built-in Versatile H&E model.
 * This implementation uses StarDist's model choice mechanism to avoid accessing private fields.
 * This class handles the second step of the 6-step analysis workflow.
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class SimpleHENuclearSegmentation {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleHENuclearSegmentation.class);
    
    // Execution tracking to prevent duplicates
    private static final java.util.Set<String> EXECUTING_IMAGES = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final java.util.concurrent.atomic.AtomicInteger EXECUTION_COUNTER = new java.util.concurrent.atomic.AtomicInteger(0);
    
    private final ImagePlus originalImage;
    private final String imageFileName;
    private final ROIManager roiManager;
    private final NuclearSegmentationSettings settings;
    private Context context;
    private CommandService commandService;
    private DatasetService datasetService;
    private final int executionId;
    
    /**
     /**
      * Constructor for SimpleHENuclearSegmentation with default settings.
      *
      * @param originalImage The original image to segment
      * @param imageFileName The filename of the image for ROI association
      */
     public SimpleHENuclearSegmentation(ImagePlus originalImage, String imageFileName) {
         this.executionId = EXECUTION_COUNTER.incrementAndGet();
         this.originalImage = originalImage;
         this.imageFileName = imageFileName;
         this.roiManager = ROIManager.getInstance();
         this.settings = ConfigurationManager.getInstance().initializeNuclearSegmentationSettings();
         
         DirectFileLogger.logStarDist("INFO", "=== SimpleHENuclearSegmentation Constructor [ID:" + executionId + "] ===");
         DirectFileLogger.logStarDist("INFO", "Image: " + imageFileName + ", Execution ID: " + executionId);
         DirectFileLogger.logStarDist("INFO", "Total executions so far: " + EXECUTION_COUNTER.get());
         
         initializeContext();
         
         LOGGER.debug("SimpleHENuclearSegmentation initialized with default settings for image: {} [ID:{}]", imageFileName, executionId);
     }
     
     /**
      * Constructor with custom settings.
      *
      * @param originalImage The original image to segment
      * @param imageFileName The filename of the image for ROI association
      * @param settings Custom nuclear segmentation settings
      */
     public SimpleHENuclearSegmentation(ImagePlus originalImage, String imageFileName, NuclearSegmentationSettings settings) {
         this.executionId = EXECUTION_COUNTER.incrementAndGet();
         this.originalImage = originalImage;
         this.imageFileName = imageFileName;
         this.roiManager = ROIManager.getInstance();
         this.settings = settings != null ? settings : ConfigurationManager.getInstance().initializeNuclearSegmentationSettings();
         
         DirectFileLogger.logStarDist("INFO", "=== SimpleHENuclearSegmentation Constructor [ID:" + executionId + "] ===");
         DirectFileLogger.logStarDist("INFO", "Image: " + imageFileName + ", Execution ID: " + executionId);
         DirectFileLogger.logStarDist("INFO", "Total executions so far: " + EXECUTION_COUNTER.get());
         
         initializeContext();
         
         LOGGER.debug("SimpleHENuclearSegmentation initialized with custom settings for image: {} [ID:{}]", imageFileName, executionId);
     }
    /**
     * Initialize minimal SciJava context.
     */
    private void initializeContext() {
        try {
            LOGGER.info("Initializing SciJava context for H&E nuclear segmentation");
            DirectFileLogger.logStarDist("INFO", "=== Initializing SciJava context for H&E nuclear segmentation ===");
            
            // Set up TensorFlow model cache directory to avoid permission issues
            setupTensorFlowCache();
            
            // Create a SciJava context with required services (excluding ScriptService for JPackage compatibility)
            LOGGER.info("Creating SciJava context with required services...");
            DirectFileLogger.logStarDist("INFO", "Creating SciJava context (excluding ScriptService for JPackage compatibility)...");
            
            // Detect if running from JPackage to adjust service loading
            String appImage = System.getProperty("jpackage.app-path");
            boolean isJPackage = appImage != null;
            
            if (isJPackage) {
                // JPackage context - include all required services for StarDist and CSBDeep
                DirectFileLogger.logStarDist("INFO", "Using JPackage-optimized SciJava context with all required services");
                this.context = new Context(
                    CommandService.class,
                    DatasetService.class,
                    org.scijava.app.StatusService.class,
                    org.scijava.log.LogService.class,
                    org.scijava.thread.ThreadService.class,
                    org.scijava.plugin.PluginService.class,
                    org.scijava.convert.ConvertService.class,
                    org.scijava.module.ModuleService.class,
                    net.imagej.tensorflow.TensorFlowService.class,
                    org.scijava.ui.UIService.class,
                    net.imagej.lut.LUTService.class,
                    net.imagej.ops.OpService.class,
                    org.scijava.prefs.PrefService.class,
                    org.scijava.io.IOService.class,
                    org.scijava.display.DisplayService.class,
                    org.scijava.parse.ParseService.class,
                    org.scijava.object.ObjectService.class,
                    net.imagej.display.ImageDisplayService.class,
                    net.imagej.types.DataTypeService.class,
                    org.scijava.app.AppService.class,
                    org.scijava.event.EventService.class
                );
            } else {
                // Full context for regular JAR execution
                DirectFileLogger.logStarDist("INFO", "Using full SciJava context for JAR environment");
                this.context = new Context(
                    CommandService.class,
                    DatasetService.class,
                    org.scijava.app.StatusService.class,
                    org.scijava.log.LogService.class,
                    org.scijava.thread.ThreadService.class,
                    org.scijava.plugin.PluginService.class,
                    org.scijava.convert.ConvertService.class,
                    org.scijava.module.ModuleService.class,
                    net.imagej.tensorflow.TensorFlowService.class,
                    org.scijava.ui.UIService.class,
                    net.imagej.ops.OpService.class,
                    net.imagej.lut.LUTService.class,
                    org.scijava.io.IOService.class,
                    org.scijava.script.ScriptService.class,
                    org.scijava.event.EventService.class
                );
            }
            
            this.commandService = context.getService(CommandService.class);
            this.datasetService = context.getService(DatasetService.class);
            
            LOGGER.info("SciJava context initialized successfully with {} services",
                        context.getServiceIndex().size());
            DirectFileLogger.logStarDist("INFO", "✓ SciJava context initialized successfully with " + context.getServiceIndex().size() + " services");
            
            // Log TensorFlow service availability
            try {
                net.imagej.tensorflow.TensorFlowService tfService = context.getService(net.imagej.tensorflow.TensorFlowService.class);
                LOGGER.info("TensorFlow service available: {}", tfService != null);
                DirectFileLogger.logStarDist("INFO", "TensorFlow service available: " + (tfService != null));
                if (tfService != null) {
                    LOGGER.info("TensorFlow service class: {}", tfService.getClass().getName());
                    DirectFileLogger.logStarDist("INFO", "TensorFlow service class: " + tfService.getClass().getName());
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to get TensorFlow service", e);
                DirectFileLogger.logStarDistException("Failed to get TensorFlow service", e);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize SciJava context", e);
            DirectFileLogger.logStarDistException("Failed to initialize SciJava context", e);
            throw new RuntimeException("Failed to initialize context: " + e.getMessage(), e);
        }
    }
    
    /**
     * Set up TensorFlow model cache directory and JPackage-specific configurations.
     */
    private void setupTensorFlowCache() {
        try {
            // Detect if running from JPackage
            String appImage = System.getProperty("jpackage.app-path");
            boolean isJPackage = appImage != null;
            
            LOGGER.info("Setting up TensorFlow cache (JPackage: {})", isJPackage);
            DirectFileLogger.logStarDist("INFO", "Setting up TensorFlow cache (JPackage: " + isJPackage + ")");
            
            // Create a writable cache directory in the user's temp directory
            String tempDir = System.getProperty("java.io.tmpdir");
            java.io.File cacheDir = new java.io.File(tempDir, "scipathj-tensorflow-models");
            
            LOGGER.info("Setting up TensorFlow cache directory: {}", cacheDir.getAbsolutePath());
            DirectFileLogger.logStarDist("INFO", "TensorFlow cache directory: " + cacheDir.getAbsolutePath());
            
            if (!cacheDir.exists()) {
                boolean created = cacheDir.mkdirs();
                if (created) {
                    LOGGER.info("Created TensorFlow cache directory: {}", cacheDir.getAbsolutePath());
                    DirectFileLogger.logStarDist("INFO", "✓ Created TensorFlow cache directory");
                } else {
                    LOGGER.error("Failed to create TensorFlow cache directory: {}", cacheDir.getAbsolutePath());
                    DirectFileLogger.logStarDist("ERROR", "✗ Failed to create TensorFlow cache directory");
                }
            } else {
                LOGGER.info("TensorFlow cache directory already exists: {}", cacheDir.getAbsolutePath());
                DirectFileLogger.logStarDist("INFO", "✓ TensorFlow cache directory already exists");
            }
            
            // Set multiple system properties that TensorFlow/CSBDeep might use
            System.setProperty("imagej.tensorflow.models.cache.dir", cacheDir.getAbsolutePath());
            System.setProperty("scijava.cache.dir", cacheDir.getAbsolutePath());
            System.setProperty("tensorflow.models.cache", cacheDir.getAbsolutePath());
            
            // JPackage-specific TensorFlow configurations
            if (isJPackage) {
                setupJPackageTensorFlowEnvironment(appImage, cacheDir);
            }
            
            // Also set the Maven local repository to avoid permission issues
            String userHome = System.getProperty("user.home");
            java.io.File mavenRepo = new java.io.File(userHome, ".m2/repository");
            if (mavenRepo.exists()) {
                System.setProperty("maven.repo.local", mavenRepo.getAbsolutePath());
                LOGGER.info("Set Maven local repository: {}", mavenRepo.getAbsolutePath());
                DirectFileLogger.logStarDist("INFO", "✓ Set Maven local repository");
            } else {
                LOGGER.warn("Maven local repository not found: {}", mavenRepo.getAbsolutePath());
                DirectFileLogger.logStarDist("WARN", "✗ Maven local repository not found");
            }
            
            LOGGER.info("TensorFlow cache setup complete. Cache dir: {}", cacheDir.getAbsolutePath());
            DirectFileLogger.logStarDist("INFO", "✓ TensorFlow cache setup complete");
            
        } catch (Exception e) {
            LOGGER.error("Failed to setup TensorFlow cache directory", e);
            DirectFileLogger.logStarDistException("Failed to setup TensorFlow cache directory", e);
            // Continue anyway - this is not critical
        }
    }
    
    /**
     * Set up JPackage-specific TensorFlow environment configurations.
     */
    /**
     * Set up JPackage-specific TensorFlow environment configurations.
     */
    private void setupJPackageTensorFlowEnvironment(String appImage, java.io.File cacheDir) {
        try {
            DirectFileLogger.logStarDist("INFO", "=== Setting up JPackage-specific TensorFlow environment ===");
            
            // Detect GUI vs Console execution context
            boolean isGuiContext = detectGuiExecutionContext();
            DirectFileLogger.logStarDist("INFO", "Execution context: " + (isGuiContext ? "GUI (double-click)" : "Console (terminal)"));
            
            // CRITICAL FIX: Set working directory to JPackage app directory to resolve resource dependencies
            java.io.File appDir = new java.io.File(appImage).getParentFile();
            String currentWorkingDir = System.getProperty("user.dir");
            DirectFileLogger.logStarDist("INFO", "Current working directory: " + currentWorkingDir);
            DirectFileLogger.logStarDist("INFO", "JPackage app directory: " + appDir.getAbsolutePath());
            
            // Force working directory to JPackage app directory for resource resolution
            System.setProperty("user.dir", appDir.getAbsolutePath());
            DirectFileLogger.logStarDist("INFO", "✓ Set working directory to JPackage app directory: " + appDir.getAbsolutePath());
            
            // Set TensorFlow working directory to a writable location
            String userHome = System.getProperty("user.home");
            java.io.File tensorflowWorkingDir = new java.io.File(userHome, "scipathj-work");
            if (!tensorflowWorkingDir.exists()) {
                tensorflowWorkingDir.mkdirs();
            }
            System.setProperty("user.dir.tensorflow", tensorflowWorkingDir.getAbsolutePath());
            DirectFileLogger.logStarDist("INFO", "Set TensorFlow working directory: " + tensorflowWorkingDir.getAbsolutePath());
            
            // Set native library path to include JPackage app directory and lib subdirectory
           java.io.File libDir = new java.io.File(appDir, "app/lib");
            java.io.File libsDir = new java.io.File(appDir, "app/libs"); // Additional libs directory
            
            DirectFileLogger.logStarDist("INFO", "JPackage app directory: " + appDir.getAbsolutePath());
            DirectFileLogger.logStarDist("INFO", "JPackage lib directory: " + libDir.getAbsolutePath());
            DirectFileLogger.logStarDist("INFO", "JPackage libs directory: " + libsDir.getAbsolutePath());
            DirectFileLogger.logStarDist("INFO", "JPackage lib directory exists: " + libDir.exists());
            DirectFileLogger.logStarDist("INFO", "JPackage libs directory exists: " + libsDir.exists());
            
            String nativeLibPath = System.getProperty("java.library.path");
            StringBuilder newLibPath = new StringBuilder();
            
            // Add JPackage lib directory first
            if (libDir.exists()) {
                newLibPath.append(libDir.getAbsolutePath());
                DirectFileLogger.logStarDist("INFO", "Added JPackage lib directory: " + libDir.getAbsolutePath());
            }
            
            // Add JPackage libs directory for dependencies
            if (libsDir.exists()) {
                if (newLibPath.length() > 0) {
                    newLibPath.append(System.getProperty("path.separator"));
                }
                newLibPath.append(libsDir.getAbsolutePath());
                DirectFileLogger.logStarDist("INFO", "Added JPackage libs directory: " + libsDir.getAbsolutePath());
            }
            
            // Add JPackage app directory
            if (newLibPath.length() > 0) {
                newLibPath.append(System.getProperty("path.separator"));
            }
            newLibPath.append(appDir.getAbsolutePath());
            
            // Add existing library path
            if (nativeLibPath != null && !nativeLibPath.isEmpty()) {
                newLibPath.append(System.getProperty("path.separator")).append(nativeLibPath);
            }
            
            System.setProperty("java.library.path", newLibPath.toString());
            DirectFileLogger.logStarDist("INFO", "Updated java.library.path: " + newLibPath.toString());
            
            // Also set JNA library path for additional native library loading
            System.setProperty("jna.library.path", newLibPath.toString());
            DirectFileLogger.logStarDist("INFO", "Set jna.library.path: " + newLibPath.toString());
            
            // Set TensorFlow-specific JPackage properties
            System.setProperty("tensorflow.jpackage.mode", "true");
            System.setProperty("tensorflow.native.path", libDir.exists() ? libDir.getAbsolutePath() : appDir.getAbsolutePath());
            
            // GUI-specific TensorFlow configuration
            if (isGuiContext) {
                DirectFileLogger.logStarDist("INFO", "=== Applying GUI-specific TensorFlow fixes ===");
                
                // Force TensorFlow to use explicit native library loading
                System.setProperty("org.tensorflow.NativeLibrary.MODE", "bundled");
                System.setProperty("tensorflow.native.library.mode", "explicit");
                System.setProperty("tensorflow.gui.mode", "true");
                
                // Set thread context for GUI execution
                System.setProperty("tensorflow.thread.context", "gui");
                System.setProperty("java.awt.headless", "false");
                
                // Force early TensorFlow initialization in GUI context
                initializeTensorFlowForGui(libDir);
                
                DirectFileLogger.logStarDist("INFO", "✓ GUI-specific TensorFlow configuration applied");
            } else {
                // Console-specific settings (keep existing behavior)
                System.setProperty("org.tensorflow.NativeLibrary.DEBUG", "false");
                System.setProperty("tensorflow.native.library.debug", "false");
                System.setProperty("java.library.debug", "false");
            }
            
            DirectFileLogger.logStarDist("INFO", "Set TensorFlow native path: " + (libDir.exists() ? libDir.getAbsolutePath() : appDir.getAbsolutePath()));
            
            // Ensure TensorFlow uses the correct temp directory
            System.setProperty("java.io.tmpdir.tensorflow", cacheDir.getAbsolutePath());
            
            // Set model extraction directory to a writable location
            java.io.File modelExtractDir = new java.io.File(cacheDir, "extracted-models");
            if (!modelExtractDir.exists()) {
                modelExtractDir.mkdirs();
            }
            System.setProperty("tensorflow.model.extract.dir", modelExtractDir.getAbsolutePath());
            DirectFileLogger.logStarDist("INFO", "Set TensorFlow model extract directory: " + modelExtractDir.getAbsolutePath());
            
            // Force TensorFlow to use file-based model loading instead of in-memory
            System.setProperty("tensorflow.model.loading.mode", "file");
            System.setProperty("tensorflow.eager", "false");
            
            // Set CSBDeep-specific JPackage properties
            System.setProperty("csbdeep.jpackage.mode", "true");
            System.setProperty("csbdeep.model.cache.dir", cacheDir.getAbsolutePath());
            
            // GUI-specific CSBDeep configuration
            if (isGuiContext) {
                System.setProperty("csbdeep.gui.mode", "true");
                System.setProperty("csbdeep.thread.context", "gui");
            }
            
            // Additional JPackage compatibility settings
            System.setProperty("imagej.jpackage.mode", "true");
            System.setProperty("scijava.jpackage.mode", "true");
            
            // Try to force reload of native library path
            try {
                // This is a hack to force the JVM to reload the library path
                java.lang.reflect.Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
                fieldSysPath.setAccessible(true);
                fieldSysPath.set(null, null);
                DirectFileLogger.logStarDist("INFO", "✓ Forced reload of native library path");
            } catch (Exception e) {
                DirectFileLogger.logStarDist("WARN", "Could not force reload of native library path: " + e.getMessage());
            }
            
            DirectFileLogger.logStarDist("INFO", "✓ JPackage-specific TensorFlow environment setup complete");
            
        } catch (Exception e) {
            DirectFileLogger.logStarDistException("Failed to setup JPackage TensorFlow environment", e);
        }
    }
    
    /**
     * Detect if we're running in GUI context (double-click) vs Console context (terminal).
     */
    private boolean detectGuiExecutionContext() {
        try {
            // Check if we have a console attached
            java.io.Console console = System.console();
            boolean hasConsole = console != null;
            
            // Check if standard streams are redirected (typical in GUI context)
            boolean stdoutRedirected = System.out != System.out;
            boolean stderrRedirected = System.err != System.err;
            
            // Check if we're running in headless mode
            boolean isHeadless = java.awt.GraphicsEnvironment.isHeadless();
            
            // Check parent process (Windows-specific)
            boolean isGuiParent = false;
            try {
                String osName = System.getProperty("os.name").toLowerCase();
                if (osName.contains("windows")) {
                    // On Windows, GUI applications typically have explorer.exe as parent
                    // while console applications have cmd.exe or powershell.exe
                    ProcessHandle current = ProcessHandle.current();
                    ProcessHandle parent = current.parent().orElse(null);
                    if (parent != null) {
                        String parentCommand = parent.info().command().orElse("").toLowerCase();
                        isGuiParent = parentCommand.contains("explorer") ||
                                     parentCommand.contains("dwm") ||
                                     (!parentCommand.contains("cmd") && !parentCommand.contains("powershell"));
                    }
                }
            } catch (Exception e) {
                // Ignore parent process detection errors
            }
            
            // GUI context if: no console AND (headless OR GUI parent)
            boolean isGuiContext = !hasConsole && (!isHeadless || isGuiParent);
            
            DirectFileLogger.logStarDist("INFO", "Execution context detection:");
            DirectFileLogger.logStarDist("INFO", "  Has console: " + hasConsole);
            DirectFileLogger.logStarDist("INFO", "  Is headless: " + isHeadless);
            DirectFileLogger.logStarDist("INFO", "  GUI parent: " + isGuiParent);
            DirectFileLogger.logStarDist("INFO", "  Detected as GUI context: " + isGuiContext);
            
            return isGuiContext;
            
        } catch (Exception e) {
            DirectFileLogger.logStarDist("WARN", "Failed to detect execution context, assuming GUI: " + e.getMessage());
            return true; // Default to GUI context for safety
        }
    }
    
    /**
     * Initialize TensorFlow specifically for GUI execution context.
     */
    private void initializeTensorFlowForGui(java.io.File libDir) {
        try {
            DirectFileLogger.logStarDist("INFO", "=== Initializing TensorFlow for GUI context ===");
            
            // Pre-load TensorFlow native libraries explicitly
            if (libDir.exists()) {
                java.io.File[] tfLibs = libDir.listFiles((dir, name) ->
                    name.contains("tensorflow") && (name.endsWith(".dll") || name.endsWith(".so") || name.endsWith(".dylib")));
                
                if (tfLibs != null && tfLibs.length > 0) {
                    for (java.io.File tfLib : tfLibs) {
                        try {
                            DirectFileLogger.logStarDist("INFO", "Pre-loading TensorFlow library: " + tfLib.getName());
                            System.load(tfLib.getAbsolutePath());
                            DirectFileLogger.logStarDist("INFO", "✓ Successfully pre-loaded: " + tfLib.getName());
                        } catch (Exception e) {
                            DirectFileLogger.logStarDist("WARN", "Failed to pre-load " + tfLib.getName() + ": " + e.getMessage());
                        }
                    }
                }
            }
            
            // Force TensorFlow class loading in GUI thread context
            try {
                DirectFileLogger.logStarDist("INFO", "Force-loading TensorFlow class in GUI context...");
                Class<?> tfClass = Class.forName("org.tensorflow.TensorFlow");
                
                // Try to get version to trigger native library initialization
                java.lang.reflect.Method versionMethod = tfClass.getMethod("version");
                String version = (String) versionMethod.invoke(null);
                DirectFileLogger.logStarDist("INFO", "✓ TensorFlow initialized in GUI context, version: " + version);
                
            } catch (Exception e) {
                DirectFileLogger.logStarDist("WARN", "TensorFlow GUI initialization failed: " + e.getMessage());
                // Continue anyway - the main initialization will handle this
            }
            
            // Set GUI-specific thread properties
            Thread currentThread = Thread.currentThread();
            currentThread.setName("SciPathJ-GUI-TensorFlow-" + currentThread.getId());
            
            DirectFileLogger.logStarDist("INFO", "✓ TensorFlow GUI initialization complete");
            
        } catch (Exception e) {
            DirectFileLogger.logStarDist("WARN", "TensorFlow GUI initialization encountered issues: " + e.getMessage());
        }
    }
     /*
     * @return List of NucleusROI objects representing detected nuclei
     * @throws Exception if segmentation fails
     */
    public List<NucleusROI> segmentNuclei() throws Exception {
        String executionKey = imageFileName + "_" + System.currentTimeMillis();
        
        // Check for duplicate execution and prevent it
        if (EXECUTING_IMAGES.contains(imageFileName)) {
            DirectFileLogger.logStarDist("WARN", "=== DUPLICATE EXECUTION DETECTED [ID:" + executionId + "] ===");
            DirectFileLogger.logStarDist("WARN", "Image '" + imageFileName + "' is already being processed!");
            DirectFileLogger.logStarDist("WARN", "This may explain why you're seeing double ROIs");
            DirectFileLogger.logStarDist("WARN", "Currently executing images: " + EXECUTING_IMAGES);
            DirectFileLogger.logStarDist("WARN", "PREVENTING DUPLICATE EXECUTION - returning empty list");
            return new ArrayList<>(); // Return empty list to prevent duplicate processing
        }
        
        EXECUTING_IMAGES.add(imageFileName);
        
        try {
            LOGGER.info("Starting H&E nuclear segmentation for image '{}' [ID:{}]", imageFileName, executionId);
            DirectFileLogger.logStarDist("INFO", "=== Starting H&E nuclear segmentation [ID:" + executionId + "] ===");
            DirectFileLogger.logStarDist("INFO", "Image: " + imageFileName + ", Execution ID: " + executionId);
            DirectFileLogger.logStarDist("INFO", "Currently executing images: " + EXECUTING_IMAGES);
            
            if (originalImage == null) {
                DirectFileLogger.logStarDist("ERROR", "Original image is null for file: " + imageFileName);
                throw new IllegalArgumentException("Original image is null for file: " + imageFileName);
            }
            
            DirectFileLogger.logStarDist("INFO", "Image details: " + originalImage.getWidth() + "x" + originalImage.getHeight() +
                                        ", " + originalImage.getNChannels() + " channels, " + originalImage.getBitDepth() + "-bit");
            
            // Check StarDist availability
            DirectFileLogger.logStarDist("INFO", "Checking StarDist availability...");
            checkStarDistAvailability();
            DirectFileLogger.logStarDist("INFO", "StarDist availability check passed");
            
            // Convert ImagePlus to Dataset
            DirectFileLogger.logStarDist("INFO", "Converting ImagePlus to Dataset...");
            Dataset inputDataset = convertToDataset(originalImage);
            DirectFileLogger.logStarDist("INFO", "Dataset conversion completed");
            
            // Execute StarDist with H&E model
            DirectFileLogger.logStarDist("INFO", "Executing StarDist with H&E model...");
            List<NucleusROI> nucleiROIs = executeStarDistHE(inputDataset);
            DirectFileLogger.logStarDist("INFO", "StarDist execution completed, found " + nucleiROIs.size() + " nuclei [ID:" + executionId + "]");
            
            // Add to ROI manager
            for (NucleusROI nucleusROI : nucleiROIs) {
                roiManager.addROI(nucleusROI);
            }
            
            LOGGER.info("H&E nuclear segmentation completed. Found {} nuclei [ID:{}]", nucleiROIs.size(), executionId);
            DirectFileLogger.logStarDist("INFO", "=== H&E nuclear segmentation completed successfully [ID:" + executionId + "] ===");
            
            return nucleiROIs;
            
        } catch (Exception e) {
            DirectFileLogger.logStarDistException("StarDist nuclear segmentation failed for image: " + imageFileName + " [ID:" + executionId + "]", e);
            throw e;
        } finally {
            EXECUTING_IMAGES.remove(imageFileName);
            DirectFileLogger.logStarDist("INFO", "Removed '" + imageFileName + "' from executing set [ID:" + executionId + "]");
        }
    }
    /**
     * Check if StarDist and CSBDeep are available.
     */
    private void checkStarDistAvailability() throws Exception {
        try {
            LOGGER.info("Checking StarDist availability...");
            DirectFileLogger.logStarDist("INFO", "Checking StarDist class availability...");
            
            Class<?> starDistClass = Class.forName("de.csbdresden.stardist.StarDist2D");
            LOGGER.info("StarDist2D class found: {}", starDistClass.getName());
            DirectFileLogger.logStarDist("INFO", "✓ StarDist2D class found: " + starDistClass.getName());
            
            Class<?> csbDeepClass = Class.forName("de.csbdresden.csbdeep.commands.GenericNetwork");
            LOGGER.info("CSBDeep GenericNetwork class found: {}", csbDeepClass.getName());
            DirectFileLogger.logStarDist("INFO", "✓ CSBDeep GenericNetwork class found: " + csbDeepClass.getName());
            
            // Check if TensorFlow native libraries are available
            try {
                validateTensorFlowNativeLibraries();
                DirectFileLogger.logStarDist("INFO", "✓ TensorFlow validation completed successfully");
            } catch (Exception e) {
                DirectFileLogger.logStarDist("ERROR", "✗ TensorFlow validation failed: " + e.getMessage());
                DirectFileLogger.logStarDistException("TensorFlow validation failed", e);
                // Continue anyway to see if StarDist can work without full TensorFlow validation
                DirectFileLogger.logStarDist("WARN", "Continuing with StarDist execution despite TensorFlow validation failure");
            }
            
            LOGGER.info("StarDist and CSBDeep availability check passed");
            DirectFileLogger.logStarDist("INFO", "✓ StarDist and CSBDeep availability check passed");
            
        } catch (ClassNotFoundException e) {
            LOGGER.error("StarDist or CSBDeep plugin not found: {}", e.getMessage());
            DirectFileLogger.logStarDistException("✗ StarDist or CSBDeep plugin not found", e);
            throw new Exception("StarDist or CSBDeep plugin not found: " + e.getMessage());
        }
    }
    
    /**
     * Validate TensorFlow native libraries with enhanced JPackage support.
     */
    private void validateTensorFlowNativeLibraries() throws Exception {
        try {
            DirectFileLogger.logStarDist("INFO", "=== TensorFlow Native Library Validation ===");
            
            // Check TensorFlow class availability
            Class<?> tfClass = Class.forName("org.tensorflow.TensorFlow");
            LOGGER.info("TensorFlow class found: {}", tfClass.getName());
            DirectFileLogger.logStarDist("INFO", "✓ TensorFlow class found: " + tfClass.getName());
            
            // Check current library path
            String libraryPath = System.getProperty("java.library.path");
            DirectFileLogger.logStarDist("INFO", "Current java.library.path: " + libraryPath);
            
            // Check if running from JPackage
            String appImage = System.getProperty("jpackage.app-path");
            boolean isJPackage = appImage != null;
            DirectFileLogger.logStarDist("INFO", "JPackage environment: " + isJPackage);
            
            if (isJPackage) {
                // Validate JPackage-specific library paths
                java.io.File appDir = new java.io.File(appImage).getParentFile();
                java.io.File libDir = new java.io.File(appDir, "app/lib");
                
                DirectFileLogger.logStarDist("INFO", "JPackage app directory: " + appDir.getAbsolutePath());
                DirectFileLogger.logStarDist("INFO", "JPackage lib directory exists: " + libDir.exists());
                
                if (libDir.exists()) {
                    java.io.File[] libFiles = libDir.listFiles((dir, name) ->
                        name.contains("tensorflow") || name.contains("jni") || name.endsWith(".dll") || name.endsWith(".so"));
                    
                    if (libFiles != null && libFiles.length > 0) {
                        DirectFileLogger.logStarDist("INFO", "Found " + libFiles.length + " potential TensorFlow native libraries:");
                        for (java.io.File libFile : libFiles) {
                            DirectFileLogger.logStarDist("INFO", "  - " + libFile.getName() + " (" + libFile.length() + " bytes)");
                        }
                    } else {
                        DirectFileLogger.logStarDist("WARN", "No TensorFlow native libraries found in lib directory");
                    }
                }
            }
            
            // Try to get TensorFlow version (this will trigger native library loading)
            try {
                java.lang.reflect.Method versionMethod = tfClass.getMethod("version");
                String version = (String) versionMethod.invoke(null);
                LOGGER.info("TensorFlow version: {}", version);
                DirectFileLogger.logStarDist("INFO", "✓ TensorFlow version: " + version);
                DirectFileLogger.logStarDist("INFO", "✓ TensorFlow native libraries loaded successfully");
                
            } catch (Exception e) {
                LOGGER.error("TensorFlow native library loading failed", e);
                DirectFileLogger.logStarDistException("✗ TensorFlow native library loading failed", e);
                
                if (isJPackage) {
                    DirectFileLogger.logStarDist("ERROR", "This is a JPackage environment. The issue is likely:");
                    DirectFileLogger.logStarDist("ERROR", "1. TensorFlow native libraries are not included in the package");
                    DirectFileLogger.logStarDist("ERROR", "2. Native library path is not set correctly");
                    DirectFileLogger.logStarDist("ERROR", "3. Native libraries are not compatible with the target system");
                    
                    // Try to provide more specific guidance
                    String osName = System.getProperty("os.name").toLowerCase();
                    String osArch = System.getProperty("os.arch").toLowerCase();
                    DirectFileLogger.logStarDist("ERROR", "Target system: " + osName + " " + osArch);
                    
                    if (osName.contains("windows")) {
                        DirectFileLogger.logStarDist("ERROR", "For Windows, ensure tensorflow_jni.dll is in the lib directory");
                    } else if (osName.contains("linux")) {
                        DirectFileLogger.logStarDist("ERROR", "For Linux, ensure libtensorflow_jni.so is in the lib directory");
                    } else if (osName.contains("mac")) {
                        DirectFileLogger.logStarDist("ERROR", "For macOS, ensure libtensorflow_jni.dylib is in the lib directory");
                    }
                }
                
                throw new Exception("TensorFlow native library validation failed: " + e.getMessage(), e);
            }
            
            DirectFileLogger.logStarDist("INFO", "=== TensorFlow Native Library Validation Complete ===");
            
        } catch (ClassNotFoundException e) {
            DirectFileLogger.logStarDist("ERROR", "TensorFlow class not found: " + e.getMessage());
            throw new Exception("TensorFlow class not found: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert ImagePlus to Dataset.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Dataset convertToDataset(ImagePlus imagePlus) {
        try {
            // Prepare image for StarDist (8-bit preferred)
            ImagePlus processedImage = prepareImageForStarDist(imagePlus);
            
            LOGGER.debug("Before ImgLib2 conversion: {}x{}, {} channels, {}-bit, type={}",
                        processedImage.getWidth(), processedImage.getHeight(),
                        processedImage.getNChannels(), processedImage.getBitDepth(), processedImage.getType());
            
            // Convert to ImgLib2
            Img img = ImageJFunctions.wrapReal(processedImage);
            
            LOGGER.debug("After ImgLib2 conversion: {} dimensions, type={}",
                        img.numDimensions(), img.getClass().getSimpleName());
            
            // Log dimensions
            StringBuilder dimStr = new StringBuilder();
            for (int i = 0; i < img.numDimensions(); i++) {
                if (i > 0) dimStr.append("x");
                dimStr.append(img.dimension(i));
            }
            LOGGER.debug("ImgLib2 dimensions: {}", dimStr.toString());
            
            // Create axes
            AxisType[] axes = {Axes.X, Axes.Y};
            if (processedImage.getNChannels() > 1) {
                axes = new AxisType[]{Axes.X, Axes.Y, Axes.CHANNEL};
                LOGGER.debug("Using 3D axes: [X, Y, CHANNEL]");
            } else {
                LOGGER.debug("Using 2D axes: [X, Y]");
            }
            
            ImgPlus imgPlus = new ImgPlus(img, processedImage.getTitle(), axes);
            Dataset dataset = datasetService.create(imgPlus);
            
            LOGGER.debug("Final Dataset: {}x{} pixels, {} channels",
                        dataset.getWidth(), dataset.getHeight(), dataset.getChannels());
            
            return dataset;
            
        } catch (Exception e) {
            LOGGER.error("Failed to convert ImagePlus to Dataset", e);
            throw new RuntimeException("Image conversion failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Prepare image for StarDist H&E model (ensure proper RGB format compatible with ImgLib2).
     */
    private ImagePlus prepareImageForStarDist(ImagePlus imagePlus) {
        LOGGER.debug("Preparing image for StarDist H&E model: {}x{}, {} channels, {}-bit, type={}",
                    imagePlus.getWidth(), imagePlus.getHeight(),
                    imagePlus.getNChannels(), imagePlus.getBitDepth(), imagePlus.getType());
        
        // The H&E model needs RGB input, but ImgLib2 doesn't support 24-bit RGB directly
        // We need to convert RGB to separate 8-bit channels that ImgLib2 can handle
        
        if (imagePlus.getBitDepth() == 24 || imagePlus.getType() == ImagePlus.COLOR_RGB) {
            LOGGER.info("Converting 24-bit RGB image to 8-bit RGB stack for ImgLib2 compatibility");
            
            // Convert RGB to separate 8-bit channels
            return convertRGBToChannelStack(imagePlus);
        }
        
        // For grayscale images, convert to 8-bit if needed
        if (imagePlus.getBitDepth() != 8) {
            LOGGER.info("Converting {}-bit image to 8-bit for ImgLib2 compatibility", imagePlus.getBitDepth());
            ImagePlus converted = imagePlus.duplicate();
            converted.setTitle(imagePlus.getTitle() + "_8bit");
            new ij.process.ImageConverter(converted).convertToGray8();
            
            LOGGER.debug("Converted to 8-bit: {}x{}, {} channels, {}-bit, type={}",
                       converted.getWidth(), converted.getHeight(),
                       converted.getNChannels(), converted.getBitDepth(), converted.getType());
            
            return converted;
        }
        
        // Already 8-bit, return as is
        LOGGER.debug("Image is already 8-bit, using as is");
        return imagePlus;
    }
    
    /**
     * Convert RGB image to a 3-channel 8-bit stack that ImgLib2 can handle.
     */
    private ImagePlus convertRGBToChannelStack(ImagePlus rgbImage) {
        try {
            // Ensure we have an RGB image
            ImagePlus rgb = rgbImage.duplicate();
            if (!(rgb.getProcessor() instanceof ij.process.ColorProcessor)) {
                new ij.process.ImageConverter(rgb).convertToRGB();
            }
            
            // Split RGB channels
            ij.plugin.ChannelSplitter splitter = new ij.plugin.ChannelSplitter();
            ImagePlus[] channels = splitter.split(rgb);
            
            if (channels.length != 3) {
                LOGGER.warn("Expected 3 RGB channels, got {}", channels.length);
                // Fallback: convert to grayscale
                ImagePlus gray = rgb.duplicate();
                new ij.process.ImageConverter(gray).convertToGray8();
                return gray;
            }
            
            // Create a 3-channel stack
            ij.ImageStack stack = new ij.ImageStack(rgb.getWidth(), rgb.getHeight());
            stack.addSlice("Red", channels[0].getProcessor());
            stack.addSlice("Green", channels[1].getProcessor());
            stack.addSlice("Blue", channels[2].getProcessor());
            
            ImagePlus result = new ImagePlus(rgb.getTitle() + "_RGB_Stack", stack);
            result.setDimensions(3, 1, 1); // 3 channels, 1 slice, 1 frame
            
            LOGGER.debug("Created RGB channel stack: {}x{}, {} channels, {}-bit, type={}",
                       result.getWidth(), result.getHeight(),
                       result.getNChannels(), result.getBitDepth(), result.getType());
            
            return result;
            
        } catch (Exception e) {
            LOGGER.error("Failed to convert RGB to channel stack, falling back to grayscale", e);
            
            // Fallback: convert to 8-bit grayscale
            ImagePlus gray = rgbImage.duplicate();
            new ij.process.ImageConverter(gray).convertToGray8();
            
            LOGGER.debug("Fallback grayscale: {}x{}, {} channels, {}-bit, type={}",
                       gray.getWidth(), gray.getHeight(),
                       gray.getNChannels(), gray.getBitDepth(), gray.getType());
            
            return gray;
        }
    }
    
    /**
     * Execute StarDist with H&E model, with enhanced debugging for JPackage environment issues.
     */
    private List<NucleusROI> executeStarDistHE(Dataset inputDataset) throws Exception {
        
        LOGGER.info("Executing StarDist2D with H&E model choice: {}", settings.getModelChoice());
        DirectFileLogger.logStarDist("INFO", "Executing StarDist2D with H&E model choice: " + settings.getModelChoice());
        
        LOGGER.info("Dataset dimensions: {}x{}, channels: {}",
                   inputDataset.getWidth(), inputDataset.getHeight(), inputDataset.getChannels());
        DirectFileLogger.logStarDist("INFO", "Dataset dimensions: " + inputDataset.getWidth() + "x" + inputDataset.getHeight() +
                                    ", channels: " + inputDataset.getChannels());
        
        // Log runtime environment information
        logRuntimeEnvironment();
        
        // Clear any existing ROI Manager
        RoiManager ijRoiManager = RoiManager.getInstance();
        if (ijRoiManager != null) {
            LOGGER.info("Clearing existing ROI Manager with {} ROIs", ijRoiManager.getCount());
            DirectFileLogger.logStarDist("INFO", "Clearing existing ROI Manager with " + ijRoiManager.getCount() + " ROIs");
            ijRoiManager.reset();
        }
        
        try {
            // Try the normal StarDist2D command first
            DirectFileLogger.logStarDist("INFO", "Creating StarDist parameters...");
            Map<String, Object> params = createHEParameters(inputDataset);
            
            LOGGER.info("Executing StarDist2D command with parameters: {}", params.keySet());
            DirectFileLogger.logStarDist("INFO", "StarDist2D parameters: " + params.keySet());
            
            LOGGER.info("Model choice: {}, Prob threshold: {}, NMS threshold: {}",
                       params.get("modelChoice"), params.get("probThresh"), params.get("nmsThresh"));
            DirectFileLogger.logStarDist("INFO", "Model: " + params.get("modelChoice") +
                                        ", Prob threshold: " + params.get("probThresh") +
                                        ", NMS threshold: " + params.get("nmsThresh"));
            
            // Execute StarDist2D command with timeout and error handling
            LOGGER.info("Starting StarDist2D command execution...");
            DirectFileLogger.logStarDist("INFO", "Starting StarDist2D command execution...");
            
            // Log detailed parameter information for debugging
            DirectFileLogger.logStarDist("INFO", "=== StarDist2D Execution Parameters ===");
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String value = entry.getValue() != null ? entry.getValue().toString() : "null";
                if (value.length() > 100) value = value.substring(0, 100) + "...";
                DirectFileLogger.logStarDist("INFO", "  " + entry.getKey() + " = " + value);
            }
            DirectFileLogger.logStarDist("INFO", "=== End Parameters ===");
            
            try {
                Future<CommandModule> future = commandService.run(StarDist2D.class, true, params);
                DirectFileLogger.logStarDist("INFO", "✓ StarDist2D command submitted successfully");
                
                LOGGER.info("Waiting for StarDist2D command to complete...");
                DirectFileLogger.logStarDist("INFO", "Waiting for StarDist2D command to complete...");
                
                // Add timeout to prevent hanging
                CommandModule result = future.get(60, java.util.concurrent.TimeUnit.SECONDS);
                DirectFileLogger.logStarDist("INFO", "✓ StarDist2D command.get() returned successfully");
                
                if (result == null) {
                    DirectFileLogger.logStarDist("ERROR", "✗ StarDist2D command returned null result");
                    throw new Exception("StarDist2D command returned null result - this usually indicates TensorFlow model execution failure in JPackage environment");
                }
                
                DirectFileLogger.logStarDist("INFO", "✓ StarDist2D command completed successfully");
                
                // Log detailed outputs from the command
                DirectFileLogger.logStarDist("INFO", "=== StarDist2D Command Outputs Analysis ===");
                if (result.getOutputs() != null && !result.getOutputs().isEmpty()) {
                    LOGGER.info("StarDist2D command outputs: {}", result.getOutputs().keySet());
                    DirectFileLogger.logStarDist("INFO", "StarDist2D command outputs: " + result.getOutputs().keySet());
                    
                    for (Map.Entry<String, Object> entry : result.getOutputs().entrySet()) {
                        Object value = entry.getValue();
                        String valueInfo = "null";
                        if (value != null) {
                            valueInfo = value.getClass().getSimpleName();
                            if (value instanceof Dataset) {
                                Dataset ds = (Dataset) value;
                                valueInfo += " [" + ds.getWidth() + "x" + ds.getHeight() + ", " + ds.getChannels() + " channels]";
                            }
                        }
                        LOGGER.info("Output '{}': {}", entry.getKey(), valueInfo);
                        DirectFileLogger.logStarDist("INFO", "  Output '" + entry.getKey() + "': " + valueInfo);
                    }
                } else {
                    LOGGER.warn("StarDist2D command produced no outputs");
                    DirectFileLogger.logStarDist("WARN", "✗ StarDist2D command produced no outputs - this indicates CSBDeep GenericNetwork failed");
                }
                DirectFileLogger.logStarDist("INFO", "=== End Outputs Analysis ===");
                
                // Extract ROIs from ImageJ ROI Manager
                DirectFileLogger.logStarDist("INFO", "Extracting ROIs from ImageJ ROI Manager...");
                ijRoiManager = RoiManager.getInstance();
                if (ijRoiManager == null) {
                    LOGGER.error("No ROI Manager found after StarDist execution");
                    DirectFileLogger.logStarDist("ERROR", "No ROI Manager found after StarDist execution");
                    throw new Exception("No ROI Manager found after StarDist execution");
                }
                
                Roi[] detectedRois = ijRoiManager.getRoisAsArray();
                LOGGER.info("StarDist detected {} ROIs in ROI Manager", detectedRois.length);
                DirectFileLogger.logStarDist("INFO", "StarDist detected " + detectedRois.length + " ROIs in ROI Manager");
                
                if (detectedRois.length == 0) {
                    LOGGER.warn("StarDist detected 0 ROIs - this might indicate a problem with the model or input");
                    DirectFileLogger.logStarDist("WARN", "StarDist detected 0 ROIs - this might indicate a problem with the model or input");
                }
                
                // Convert to NucleusROI objects
                DirectFileLogger.logStarDist("INFO", "Converting ROIs to NucleusROI objects...");
                List<NucleusROI> nucleusROIs = convertToNucleusROIs(detectedRois);
                LOGGER.info("Successfully converted {} ROIs to NucleusROI objects", nucleusROIs.size());
                DirectFileLogger.logStarDist("INFO", "✓ Successfully converted " + nucleusROIs.size() + " ROIs to NucleusROI objects");
                
                return nucleusROIs;
                
            } catch (java.util.concurrent.ExecutionException ee) {
                DirectFileLogger.logStarDist("ERROR", "✗ StarDist2D command execution failed with ExecutionException");
                DirectFileLogger.logStarDistException("StarDist2D ExecutionException", ee);
                
                // Get the underlying cause
                Throwable cause = ee.getCause();
                if (cause != null) {
                    DirectFileLogger.logStarDist("ERROR", "Underlying cause: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
                    DirectFileLogger.logStarDistException("StarDist2D underlying cause", cause);
                    
                    // Check for specific TensorFlow-related errors
                    if (cause.getMessage() != null) {
                        String msg = cause.getMessage().toLowerCase();
                        if (msg.contains("tensorflow") || msg.contains("native") || msg.contains("jni")) {
                            DirectFileLogger.logStarDist("ERROR", "This appears to be a TensorFlow native library loading issue");
                            DirectFileLogger.logStarDist("ERROR", "In JPackage environment, ensure TensorFlow JNI libraries are properly included");
                        }
                    }
                }
                
                throw new Exception("StarDist2D command execution failed: " + ee.getMessage(), ee);
                
            } catch (java.util.concurrent.TimeoutException te) {
                DirectFileLogger.logStarDist("ERROR", "✗ StarDist2D command timed out");
                DirectFileLogger.logStarDistException("StarDist2D timeout", te);
                throw new Exception("StarDist2D command timed out: " + te.getMessage(), te);
                
            } catch (InterruptedException ie) {
                DirectFileLogger.logStarDist("ERROR", "✗ StarDist2D command was interrupted");
                DirectFileLogger.logStarDistException("StarDist2D interrupted", ie);
                Thread.currentThread().interrupt(); // Restore interrupted status
                throw new Exception("StarDist2D command was interrupted: " + ie.getMessage(), ie);
            }
            
            
        } catch (Exception e) {
            LOGGER.error("StarDist execution failed with exception: {}", e.getMessage(), e);
            DirectFileLogger.logStarDistException("StarDist execution failed", e);
            
            // Log the full stack trace for debugging
            LOGGER.error("Full StarDist execution stack trace:", e);
            
            // Re-throw the exception instead of using fallback
            throw new Exception("StarDist execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Log runtime environment information to help debug JPackage vs JAR differences.
     */
    private void logRuntimeEnvironment() {
        try {
            DirectFileLogger.logStarDist("INFO", "=== Runtime Environment Debug Info ===");
            
            // Java runtime info
            String javaVersion = System.getProperty("java.version");
            String javaVendor = System.getProperty("java.vendor");
            String javaHome = System.getProperty("java.home");
            DirectFileLogger.logStarDist("INFO", "Java Version: " + javaVersion);
            DirectFileLogger.logStarDist("INFO", "Java Vendor: " + javaVendor);
            DirectFileLogger.logStarDist("INFO", "Java Home: " + javaHome);
            
            // Application runtime info
            String classPath = System.getProperty("java.class.path");
            DirectFileLogger.logStarDist("INFO", "Classpath: " + (classPath.length() > 200 ? classPath.substring(0, 200) + "..." : classPath));
            
            // Check if running from JPackage
            String appImage = System.getProperty("jpackage.app-path");
            boolean isJPackage = appImage != null;
            DirectFileLogger.logStarDist("INFO", "Running from JPackage: " + isJPackage);
            if (isJPackage) {
                DirectFileLogger.logStarDist("INFO", "JPackage App Path: " + appImage);
            }
            
            // Working directory
            String userDir = System.getProperty("user.dir");
            DirectFileLogger.logStarDist("INFO", "Working Directory: " + userDir);
            
            // Temp directory
            String tempDir = System.getProperty("java.io.tmpdir");
            DirectFileLogger.logStarDist("INFO", "Temp Directory: " + tempDir);
            
            // Check StarDist model loading
            try {
                Class<?> starDistModelClass = Class.forName("de.csbdresden.stardist.StarDist2DModel");
                DirectFileLogger.logStarDist("INFO", "StarDist2DModel class loaded from: " + starDistModelClass.getProtectionDomain().getCodeSource().getLocation());
            } catch (Exception e) {
                DirectFileLogger.logStarDist("ERROR", "Failed to get StarDist2DModel location: " + e.getMessage());
            }
            
            DirectFileLogger.logStarDist("INFO", "=== End Runtime Environment Debug Info ===");
            
        } catch (Exception e) {
            DirectFileLogger.logStarDist("ERROR", "Failed to log runtime environment: " + e.getMessage());
        }
    }
    
    /**
     * Fallback nuclei detection using simple image processing when StarDist fails.
     */
    private List<NucleusROI> createFallbackNucleiDetection(Dataset inputDataset) {
        LOGGER.info("Using fallback nuclei detection (simple image processing)");
        
        try {
            // Convert Dataset back to ImagePlus for simple processing
            ImagePlus fallbackImage = convertDatasetToImagePlus(inputDataset);
            
            // Apply simple nuclei detection using ImageJ's built-in tools
            return performSimpleNucleiDetection(fallbackImage);
            
        } catch (Exception e) {
            LOGGER.error("Fallback nuclei detection also failed", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Convert Dataset back to ImagePlus for fallback processing.
     */
    private ImagePlus convertDatasetToImagePlus(Dataset dataset) {
        // For now, return the original image since we have it
        // In a more complete implementation, we would convert the Dataset back
        return originalImage;
    }
    
    /**
     * Perform simple nuclei detection using basic image processing.
     */
    private List<NucleusROI> performSimpleNucleiDetection(ImagePlus image) {
        List<NucleusROI> nuclei = new ArrayList<>();
        
        try {
            LOGGER.debug("Performing simple nuclei detection on {}x{} image", image.getWidth(), image.getHeight());
            
            // Create a working copy
            ImagePlus workingImage = image.duplicate();
            workingImage.setTitle("Nuclei_Detection_" + System.currentTimeMillis());
            
            // Convert to 8-bit grayscale if needed
            if (workingImage.getType() != ImagePlus.GRAY8) {
                new ij.process.ImageConverter(workingImage).convertToGray8();
            }
            
            // Apply Gaussian blur to reduce noise
            ij.plugin.filter.GaussianBlur blur = new ij.plugin.filter.GaussianBlur();
            blur.blurGaussian(workingImage.getProcessor(), 1.0);
            
            // Apply threshold to detect dark nuclei
            workingImage.getProcessor().threshold(120); // Adjust threshold for nuclei
            
            // Use particle analyzer to find nuclei-like objects
            ij.measure.ResultsTable rt = new ij.measure.ResultsTable();
            ij.plugin.filter.ParticleAnalyzer pa = new ij.plugin.filter.ParticleAnalyzer(
                ij.plugin.filter.ParticleAnalyzer.ADD_TO_MANAGER | ij.plugin.filter.ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES,
                ij.measure.Measurements.AREA | ij.measure.Measurements.CENTROID | ij.measure.Measurements.CIRCULARITY,
                rt,
                settings.getMinNucleusSize(),  // min size
                settings.getMaxNucleusSize(),  // max size
                0.3,   // min circularity
                1.0    // max circularity
            );
            
            pa.analyze(workingImage);
            
            // Get ROIs from the ROI Manager
            RoiManager roiManager = RoiManager.getInstance();
            if (roiManager != null) {
                Roi[] rois = roiManager.getRoisAsArray();
                LOGGER.debug("Simple detection found {} potential nuclei", rois.length);
                
                // Convert to NucleusROI objects
                for (int i = 0; i < rois.length; i++) {
                    String nucleusName = "Nucleus_" + (i + 1);
                    NucleusROI nucleusROI = new NucleusROI(rois[i], imageFileName, nucleusName);
                    nucleusROI.setSegmentationMethod("Simple_Fallback");
                    nucleusROI.setNotes("Nucleus detected by simple image processing fallback method");
                    nuclei.add(nucleusROI);
                }
                
                roiManager.reset(); // Clear for next use
            }
            
            // Clean up
            workingImage.close();
            
            LOGGER.info("Simple nuclei detection completed: found {} nuclei", nuclei.size());
            
        } catch (Exception e) {
            LOGGER.error("Simple nuclei detection failed", e);
        }
        
        return nuclei;
    }

    private Map<String, Object> createHEParameters(Dataset inputDataset) {
        Map<String, Object> params = new HashMap<>();
        
        // Input and model selection
        params.put("input", inputDataset);
        params.put("modelChoice", settings.getModelChoice());
        
        // Normalization settings from configuration
        params.put("normalizeInput", settings.isNormalizeInput());
        params.put("percentileBottom", (double) settings.getPercentileBottom());
        params.put("percentileTop", (double) settings.getPercentileTop());
        
        // Detection thresholds from configuration
        params.put("probThresh", (double) settings.getProbThresh());
        params.put("nmsThresh", (double) settings.getNmsThresh());
        
        // Output settings from configuration
        params.put("outputType", settings.getOutputType());
        params.put("excludeBoundary", settings.getExcludeBoundary());
        params.put("roiPosition", settings.getRoiPosition());
        
        // Performance settings from configuration
        params.put("nTiles", settings.getNTiles());
        params.put("verbose", settings.isVerbose());
        params.put("showCsbdeepProgress", settings.isShowCsbdeepProgress());
        params.put("showProbAndDist", settings.isShowProbAndDist());
        
        LOGGER.debug("Created H&E parameters: model={}, probThresh={}, nmsThresh={}, normalize={}, percentiles={}-{}",
                    settings.getModelChoice(), settings.getProbThresh(), settings.getNmsThresh(),
                    settings.isNormalizeInput(), settings.getPercentileBottom(), settings.getPercentileTop());
        
        return params;
    }
    
    /**
     * Convert ImageJ ROIs to NucleusROI objects.
     */
    private List<NucleusROI> convertToNucleusROIs(Roi[] rois) {
        List<NucleusROI> nucleiROIs = new ArrayList<>();
        
        if (rois == null || rois.length == 0) {
            LOGGER.warn("No ROIs detected by StarDist");
            return nucleiROIs;
        }
        
        for (int i = 0; i < rois.length; i++) {
            Roi roi = rois[i];
            
            if (roi == null) {
                LOGGER.warn("Skipping null ROI at index {}", i);
                continue;
            }
            
            // Create nucleus name
            String nucleusName = "Nucleus_" + (i + 1);
            
            // Create NucleusROI
            NucleusROI nucleusROI = new NucleusROI(roi, imageFileName, nucleusName);
            nucleusROI.setSegmentationMethod("StarDist_HE_Simple");
            nucleusROI.setNotes(String.format(
                "Nucleus detected by StarDist %s model. " +
                "Prob threshold: %.3f, NMS threshold: %.3f, Normalization: %s (%.1f-%.1f%%)",
                settings.getModelChoice(), settings.getProbThresh(), settings.getNmsThresh(),
                settings.isNormalizeInput() ? "enabled" : "disabled",
                settings.getPercentileBottom(), settings.getPercentileTop()
            ));
            
            nucleiROIs.add(nucleusROI);
            
            LOGGER.debug("Created nucleus ROI: {} with area: {:.1f} pixels",
                       nucleusName, nucleusROI.getNucleusArea());
        }
        
        LOGGER.debug("Successfully converted {} ROIs to NucleusROI objects", nucleiROIs.size());
        return nucleiROIs;
    }
    
    /**
     * Get statistics about the detected nuclei.
     */
    public String getStatistics(List<NucleusROI> nucleiROIs) {
        if (nucleiROIs.isEmpty()) {
            return "No nuclei detected";
        }
        
        double totalArea = nucleiROIs.stream()
            .mapToDouble(NucleusROI::getNucleusArea)
            .sum();
        
        double avgArea = totalArea / nucleiROIs.size();
        
        double minArea = nucleiROIs.stream()
            .mapToDouble(NucleusROI::getNucleusArea)
            .min()
            .orElse(0.0);
        
        double maxArea = nucleiROIs.stream()
            .mapToDouble(NucleusROI::getNucleusArea)
            .max()
            .orElse(0.0);
        
        return String.format(
            "H&E Nuclei: %d, Total area: %.1f px, Avg area: %.1f px (range: %.1f-%.1f)",
            nucleiROIs.size(), totalArea, avgArea, minArea, maxArea
        );
    }
    
    /**
     * Check if StarDist is available.
     */
    public boolean isAvailable() {
        try {
            LOGGER.info("Checking if StarDist is available...");
            DirectFileLogger.logStarDist("INFO", "=== CHECKING STARDIST AVAILABILITY ===");
            checkStarDistAvailability();
            LOGGER.info("StarDist is available");
            DirectFileLogger.logStarDist("INFO", "✓ StarDist is available - returning true");
            return true;
        } catch (Exception e) {
            LOGGER.error("StarDist is not available: {}", e.getMessage(), e);
            DirectFileLogger.logStarDist("ERROR", "✗ StarDist is NOT available: " + e.getMessage());
            DirectFileLogger.logStarDistException("StarDist availability check failed", e);
            return false;
        }
    }
    
    /**
     * Close the context and release resources.
     */
    public void close() {
        try {
            if (context != null) {
                context.dispose();
                LOGGER.debug("SciJava context disposed");
            }
        } catch (Exception e) {
            LOGGER.warn("Error disposing context", e);
        }
    }
    
    @Override
    public String toString() {
        return String.format("SimpleHENuclearSegmentation[image=%s, model=%s, probThresh=%.3f, available=%s]",
                           imageFileName, settings.getModelChoice(), settings.getProbThresh(), isAvailable());
    }
    
    /**
     * Get the current nuclear segmentation settings.
     *
     * @return The settings instance
     */
    public NuclearSegmentationSettings getSettings() {
        return settings;
    }
}