package com.scipath.scipathj.analysis;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.config.NuclearSegmentationSettings;
import com.scipath.scipathj.data.model.NucleusROI;
import com.scipath.scipathj.ui.components.ROIManager;
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
 * Simplified H&E nuclear segmentation using StarDist's built-in Versatile H&E model.
 * This implementation uses StarDist's model choice mechanism to avoid accessing private fields.
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 */
public class SimpleHENuclearSegmentation {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleHENuclearSegmentation.class);
    
    private final ImagePlus originalImage;
    private final String imageFileName;
    private final ROIManager roiManager;
    private final NuclearSegmentationSettings settings;
    private Context context;
    private CommandService commandService;
    private DatasetService datasetService;
    
    /**
     * Constructor for SimpleHENuclearSegmentation.
     * 
     * @param originalImage The original image to segment
     * @param imageFileName The filename of the image for ROI association
     */
    public SimpleHENuclearSegmentation(ImagePlus originalImage, String imageFileName) {
        this.originalImage = originalImage;
        this.imageFileName = imageFileName;
        this.roiManager = ROIManager.getInstance();
        this.settings = ConfigurationManager.getInstance().initializeNuclearSegmentationSettings();
        initializeContext();
        
        LOGGER.debug("SimpleHENuclearSegmentation initialized with settings: {}", settings);
    }
    
    /**
     * Constructor with custom settings.
     *
     * @param originalImage The original image to segment
     * @param imageFileName The filename of the image for ROI association
     * @param settings Custom nuclear segmentation settings
     */
    public SimpleHENuclearSegmentation(ImagePlus originalImage, String imageFileName, NuclearSegmentationSettings settings) {
        this.originalImage = originalImage;
        this.imageFileName = imageFileName;
        this.roiManager = ROIManager.getInstance();
        this.settings = settings != null ? settings : ConfigurationManager.getInstance().initializeNuclearSegmentationSettings();
        initializeContext();
        
        LOGGER.debug("SimpleHENuclearSegmentation initialized with custom settings: {}", this.settings);
    }
    
    /**
     * Initialize minimal SciJava context.
     */
    private void initializeContext() {
        try {
            LOGGER.debug("Initializing SciJava context for H&E nuclear segmentation");
            
            // Set up TensorFlow model cache directory to avoid permission issues
            setupTensorFlowCache();
            
            // Create a SciJava context with all services required by CSBDeep/StarDist
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
            
            this.commandService = context.getService(CommandService.class);
            this.datasetService = context.getService(DatasetService.class);
            
            LOGGER.debug("SciJava context initialized successfully with {} services",
                        context.getServiceIndex().size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize SciJava context", e);
            throw new RuntimeException("Failed to initialize context: " + e.getMessage(), e);
        }
    }
    /**
     * Set up TensorFlow model cache directory to avoid permission issues.
     */
    private void setupTensorFlowCache() {
        try {
            // Create a writable cache directory in the user's temp directory
            String tempDir = System.getProperty("java.io.tmpdir");
            java.io.File cacheDir = new java.io.File(tempDir, "scipathj-tensorflow-models");
            
            if (!cacheDir.exists()) {
                boolean created = cacheDir.mkdirs();
                if (created) {
                    LOGGER.debug("Created TensorFlow cache directory: {}", cacheDir.getAbsolutePath());
                } else {
                    LOGGER.warn("Failed to create TensorFlow cache directory: {}", cacheDir.getAbsolutePath());
                }
            }
            
            // Set multiple system properties that TensorFlow/CSBDeep might use
            System.setProperty("imagej.tensorflow.models.cache.dir", cacheDir.getAbsolutePath());
            System.setProperty("scijava.cache.dir", cacheDir.getAbsolutePath());
            System.setProperty("tensorflow.models.cache", cacheDir.getAbsolutePath());
            
            // Also set the Maven local repository to avoid permission issues
            String userHome = System.getProperty("user.home");
            java.io.File mavenRepo = new java.io.File(userHome, ".m2/repository");
            if (mavenRepo.exists()) {
                System.setProperty("maven.repo.local", mavenRepo.getAbsolutePath());
            }
            
            LOGGER.debug("Set TensorFlow cache directory to: {}", cacheDir.getAbsolutePath());
            
        } catch (Exception e) {
            LOGGER.warn("Failed to setup TensorFlow cache directory", e);
            // Continue anyway - this is not critical
        }
    }
    
    /**
     * Perform H&E nuclear segmentation using StarDist.
     * 
     * @return List of NucleusROI objects representing detected nuclei
     * @throws Exception if segmentation fails
     */
    public List<NucleusROI> segmentNuclei() throws Exception {
        LOGGER.info("Starting H&E nuclear segmentation for image '{}'", imageFileName);
        
        if (originalImage == null) {
            throw new IllegalArgumentException("Original image is null for file: " + imageFileName);
        }
        
        // Check StarDist availability
        checkStarDistAvailability();
        
        // Convert ImagePlus to Dataset
        Dataset inputDataset = convertToDataset(originalImage);
        
        // Execute StarDist with H&E model
        List<NucleusROI> nucleiROIs = executeStarDistHE(inputDataset);
        
        // Add to ROI manager
        for (NucleusROI nucleusROI : nucleiROIs) {
            roiManager.addROI(nucleusROI);
        }
        
        LOGGER.info("H&E nuclear segmentation completed. Found {} nuclei", nucleiROIs.size());
        
        return nucleiROIs;
    }
    
    /**
     * Check if StarDist and CSBDeep are available.
     */
    private void checkStarDistAvailability() throws Exception {
        try {
            Class.forName("de.csbdresden.stardist.StarDist2D");
            Class.forName("de.csbdresden.csbdeep.commands.GenericNetwork");
            LOGGER.debug("StarDist and CSBDeep are available");
        } catch (ClassNotFoundException e) {
            throw new Exception("StarDist or CSBDeep plugin not found: " + e.getMessage());
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
            
            // Convert to ImgLib2
            Img img = ImageJFunctions.wrapReal(processedImage);
            
            // Create axes
            AxisType[] axes = {Axes.X, Axes.Y};
            if (processedImage.getNChannels() > 1) {
                axes = new AxisType[]{Axes.X, Axes.Y, Axes.CHANNEL};
            }
            
            ImgPlus imgPlus = new ImgPlus(img, processedImage.getTitle(), axes);
            Dataset dataset = datasetService.create(imgPlus);
            
            LOGGER.debug("Converted ImagePlus to Dataset: {}x{} pixels", 
                        dataset.getWidth(), dataset.getHeight());
            
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
     * Execute StarDist with H&E model using built-in model choice.
     /**
      * Execute StarDist with H&E model using a fallback approach that avoids complex TensorFlow integration.
      */
     private List<NucleusROI> executeStarDistHE(Dataset inputDataset) throws Exception {
         
         LOGGER.debug("Executing StarDist2D with H&E model choice: {}", settings.getModelChoice());
         
         // Clear any existing ROI Manager
         RoiManager ijRoiManager = RoiManager.getInstance();
         if (ijRoiManager != null) {
             ijRoiManager.reset();
         }
         
         try {
             // Try the normal StarDist2D command first
             Map<String, Object> params = createHEParameters(inputDataset);
             
             LOGGER.debug("Executing StarDist2D command with parameters: {}", params.keySet());
             
             // Execute StarDist2D command with timeout and error handling
             Future<CommandModule> future = commandService.run(StarDist2D.class, true, params);
             CommandModule result = future.get();
             
             // Check if execution was successful
             if (result == null) {
                 LOGGER.error("StarDist2D command returned null result");
                 return createFallbackNucleiDetection(inputDataset);
             }
             
             // Log any outputs from the command
             if (result.getOutputs() != null && !result.getOutputs().isEmpty()) {
                 LOGGER.debug("StarDist2D command outputs: {}", result.getOutputs().keySet());
             }
             
             // Extract ROIs from ImageJ ROI Manager
             ijRoiManager = RoiManager.getInstance();
             if (ijRoiManager == null) {
                 LOGGER.warn("No ROI Manager found after StarDist execution");
                 return createFallbackNucleiDetection(inputDataset);
             }
             
             Roi[] detectedRois = ijRoiManager.getRoisAsArray();
             LOGGER.debug("StarDist detected {} ROIs", detectedRois.length);
             
             // Convert to NucleusROI objects
             return convertToNucleusROIs(detectedRois);
             
         } catch (Exception e) {
             LOGGER.error("StarDist execution failed, using fallback detection", e);
             
             // Use fallback detection instead of failing completely
             return createFallbackNucleiDetection(inputDataset);
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
                 50.0,  // min size (typical nucleus size)
                 2000.0, // max size
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
            checkStarDistAvailability();
            return true;
        } catch (Exception e) {
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