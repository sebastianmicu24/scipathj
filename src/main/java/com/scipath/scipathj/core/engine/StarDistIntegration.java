package com.scipath.scipathj.core.engine;

import com.scipath.scipathj.core.config.NuclearSegmentationSettings;
import de.csbdresden.stardist.StarDist2D;
import de.csbdresden.stardist.StarDist2DNMS;
import de.csbdresden.stardist.Candidates;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.command.CommandModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.PolygonRoi;
import java.awt.Polygon;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Future;

/**
 * Standalone StarDist integration for SciPathJ without ImageJ 1.x legacy dependencies.
 * This class uses StarDist2D and StarDist2DNMS directly through SciJava CommandService 
 * and extracts ROI data without relying on ImageJ's legacy ROI Manager.
 * 
 * @author Sebastian Micu
 * @version 3.0.0
 * @since 1.0.0
 */
public class StarDistIntegration {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StarDistIntegration.class);
    
    private final NuclearSegmentationSettings settings;
    private Context context;
    private CommandService commandService;
    private DatasetService datasetService;
    
    /**
     * Constructor for StarDistIntegration.
     * 
     * @param settings The nuclear segmentation settings to use
     */
    public StarDistIntegration(NuclearSegmentationSettings settings) {
        this.settings = settings;
        initializeContext();
        LOGGER.debug("StarDistIntegration initialized with settings: {}", settings);
    }
    
    /**
     * Initializes the SciJava context without legacy services.
     * Applies Java 21 ClassLoader compatibility fix before initialization.
     */
    private void initializeContext() {
        try {
            // Apply Java 21 ClassLoader fix before initializing TensorFlow/CSBDeep
            LOGGER.debug("Applying Java 21 ClassLoader compatibility fix");
            Java21ClassLoaderFix.applyFix();
            LOGGER.debug("ClassLoader info: {}", Java21ClassLoaderFix.getClassLoaderInfo());
            
            // Create a SciJava context with all services required by CSBDeep/StarDist
            // but without legacy services to avoid LegacyService conflicts
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
            // Try to restore original ClassLoader on failure
            Java21ClassLoaderFix.restoreOriginalClassLoader();
            throw new RuntimeException("Failed to initialize SciJava context: " + e.getMessage(), e);
        }
    }
    
    /**
     * Executes StarDist segmentation on the provided image.
     *
     * @param imagePlus The image to segment
     * @return Array of ROIs detected by StarDist
     * @throws StarDistException if StarDist execution fails
     */
    public Roi[] executeStarDist(ImagePlus imagePlus) throws StarDistException {
        if (imagePlus == null) {
            throw new IllegalArgumentException("ImagePlus cannot be null");
        }
        
        LOGGER.info("Starting StarDist nucleus segmentation on image: {}", imagePlus.getTitle());
        
        // Check if StarDist is available
        if (!isStarDistAvailable()) {
            throw new StarDistException("StarDist plugin is not available. Please ensure StarDist is properly installed.");
        }
        
        try {
            // Convert ImagePlus to Dataset for StarDist
            Dataset inputDataset = convertImagePlusToDataset(imagePlus);
            
            // Execute StarDist CNN to get probability and distance maps
            Map<String, Object> cnnResult = executeStarDistCNN(inputDataset);
            Dataset probDataset = (Dataset) cnnResult.get("prob");
            Dataset distDataset = (Dataset) cnnResult.get("dist");
            
            // Execute StarDist NMS to get polygons
            Candidates candidates = executeStarDistNMS(probDataset, distDataset);
            
            // Extract ROIs from candidates without using ROI Manager
            List<Roi> detectedRois = extractROIsFromCandidates(candidates);
            
            LOGGER.info("StarDist execution completed. Detected {} nuclei", detectedRois.size());
            
            return detectedRois.toArray(new Roi[0]);
            
        } catch (Exception e) {
            LOGGER.error("StarDist execution failed for image: {}", imagePlus.getTitle(), e);
            throw new StarDistException("StarDist execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts ImagePlus to Dataset for StarDist processing.
     * 
     * @param imagePlus The ImagePlus to convert
     * @return The converted Dataset
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Dataset convertImagePlusToDataset(ImagePlus imagePlus) {
        try {
            // Check and convert image bit depth if necessary
            ImagePlus processedImage = ensureSupportedBitDepth(imagePlus);
            
            // Convert ImagePlus to ImgLib2 Img
            Img img = ImageJFunctions.wrapReal(processedImage);
            
            // Create ImgPlus with proper axes
            AxisType[] axes = {Axes.X, Axes.Y};
            if (processedImage.getNChannels() > 1) {
                axes = new AxisType[]{Axes.X, Axes.Y, Axes.CHANNEL};
            }
            
            ImgPlus imgPlus = new ImgPlus(img, processedImage.getTitle(), axes);
            
            // Create Dataset using raw types to avoid generic type inference issues
            Dataset dataset = datasetService.create(imgPlus);
            
            LOGGER.debug("Successfully converted ImagePlus to Dataset: {}", dataset.getName());
            
            return dataset;
            
        } catch (Exception e) {
            LOGGER.error("Failed to convert ImagePlus to Dataset", e);
            throw new RuntimeException("Image conversion failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Ensures the ImagePlus has a supported bit depth (8, 16, or 32-bit).
     * Converts the image if necessary.
     *
     * @param imagePlus The input ImagePlus
     * @return ImagePlus with supported bit depth
     */
    private ImagePlus ensureSupportedBitDepth(ImagePlus imagePlus) {
        int bitDepth = imagePlus.getBitDepth();
        
        LOGGER.debug("Input image bit depth: {} bits", bitDepth);
        
        // Check if bit depth is already supported
        if (bitDepth == 8 || bitDepth == 16 || bitDepth == 32) {
            LOGGER.debug("Image bit depth is supported, no conversion needed");
            return imagePlus;
        }
        
        // Convert to 32-bit float for unsupported bit depths
        LOGGER.info("Converting image from {}-bit to 32-bit float for compatibility", bitDepth);
        
        ImagePlus convertedImage = imagePlus.duplicate();
        convertedImage.setTitle(imagePlus.getTitle() + "_32bit");
        
        // Convert to 32-bit
        new ij.process.ImageConverter(convertedImage).convertToGray32();
        
        LOGGER.debug("Successfully converted image to 32-bit float");
        
        return convertedImage;
    }
    
    /**
     * Executes the StarDist CNN to get probability and distance maps.
     * 
     * @param inputDataset The input dataset for StarDist
     * @return Map containing prob and dist datasets
     * @throws StarDistException if command execution fails
     */
    private Map<String, Object> executeStarDistCNN(Dataset inputDataset) throws StarDistException {
        try {
            LOGGER.debug("Executing StarDist CNN for probability and distance prediction");
            
            // Ensure ClassLoader fix is applied before TensorFlow operations
            Java21ClassLoaderFix.applyFix();
            
            // Create parameters for CSBDeep GenericNetwork (used by StarDist)
            Map<String, Object> cnnParams = createStarDistCNNParameters(inputDataset);
            
            LOGGER.debug("CNN Parameters: {}", cnnParams.keySet());
            for (Map.Entry<String, Object> entry : cnnParams.entrySet()) {
                LOGGER.debug("  {}: {}", entry.getKey(), entry.getValue());
            }
            
            // Execute CSBDeep GenericNetwork command
            Future<CommandModule> futureCNN = commandService.run(
                de.csbdresden.csbdeep.commands.GenericNetwork.class, true, cnnParams);
            
            CommandModule cnnResult = futureCNN.get();
            Dataset prediction = (Dataset) cnnResult.getOutput("output");
            
            // Split prediction into probability and distance maps
            Map<String, Object> splitResult = splitPrediction(prediction);
            
            LOGGER.debug("StarDist CNN execution completed successfully");
            
            return splitResult;
            
        } catch (Exception e) {
            LOGGER.error("StarDist CNN execution failed", e);
            throw new StarDistException("StarDist CNN execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Executes the StarDist NMS to get polygon candidates.
     * 
     * @param probDataset The probability dataset
     * @param distDataset The distance dataset
     * @return Candidates object containing polygons
     * @throws StarDistException if command execution fails
     */
    private Candidates executeStarDistNMS(Dataset probDataset, Dataset distDataset) throws StarDistException {
        try {
            LOGGER.debug("Executing StarDist NMS for polygon extraction");
            
            // Create parameters for StarDist NMS
            Map<String, Object> nmsParams = createStarDistNMSParameters(probDataset, distDataset);
            
            // Execute StarDist NMS command
            Future<CommandModule> futureNMS = commandService.run(
                StarDist2DNMS.class, true, nmsParams);
            
            CommandModule nmsResult = futureNMS.get();
            Candidates candidates = (Candidates) nmsResult.getOutput("polygons");
            
            LOGGER.debug("StarDist NMS execution completed successfully");
            
            return candidates;
            
        } catch (Exception e) {
            LOGGER.error("StarDist NMS execution failed", e);
            throw new StarDistException("StarDist NMS execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts ROIs from StarDist candidates without using ROI Manager.
     * 
     * @param candidates The candidates object from StarDist
     * @return List of ROIs
     */
    private List<Roi> extractROIsFromCandidates(Candidates candidates) {
        List<Roi> rois = new ArrayList<>();
        
        if (candidates == null) {
            LOGGER.warn("Candidates object is null");
            return rois;
        }
        
        List<Integer> winners = candidates.getWinner();
        LOGGER.debug("Processing {} winning candidates", winners.size());
        
        for (int i = 0; i < winners.size(); i++) {
            try {
                int candidateIndex = winners.get(i);
                
                // Get the polygon ROI from candidates
                PolygonRoi polygonRoi = candidates.getPolygonRoi(candidateIndex);
                
                if (polygonRoi != null) {
                    // Set a meaningful name for the ROI
                    polygonRoi.setName("Nucleus_" + (i + 1));
                    rois.add(polygonRoi);
                    
                    LOGGER.debug("Extracted ROI {}: {} with {} points", 
                               i + 1, polygonRoi.getName(), polygonRoi.getNCoordinates());
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to extract ROI for candidate {}: {}", i, e.getMessage());
            }
        }
        
        LOGGER.debug("Successfully extracted {} ROIs from candidates", rois.size());
        
        return rois;
    }
    
    /**
     * Splits the StarDist prediction into probability and distance maps.
     * This is a simplified version - in practice you might need to adapt based on the actual output format.
     * 
     * @param prediction The prediction dataset from StarDist
     * @return Map containing prob and dist datasets
     */
    private Map<String, Object> splitPrediction(Dataset prediction) {
        Map<String, Object> result = new HashMap<>();
        
        // This is a placeholder implementation
        // In practice, you would need to split the channels of the prediction
        // The first channel is typically probability, the rest are distance maps
        
        // For now, we'll use the prediction as both prob and dist
        // This needs to be implemented properly based on StarDist's output format
        result.put("prob", prediction);
        result.put("dist", prediction);
        
        LOGGER.debug("Split prediction into probability and distance maps");
        
        return result;
    }
    
    /**
     * Creates parameters for StarDist CNN execution.
     /**
      * Creates parameters for StarDist CNN execution.
      *
      * @param inputDataset The input dataset
      * @return Parameter map for CNN
      */
     private Map<String, Object> createStarDistCNNParameters(Dataset inputDataset) {
         Map<String, Object> params = new HashMap<>();
         
         params.put("input", inputDataset);
         params.put("normalizeInput", settings.isNormalizeInput());
         params.put("percentileBottom", (double) settings.getPercentileBottom());
         params.put("percentileTop", (double) settings.getPercentileTop());
         params.put("clip", false);
         params.put("nTiles", settings.getNTiles());
         params.put("blockMultiple", 64);
         params.put("overlap", 64);
         params.put("batchSize", 1);
         params.put("showProgressDialog", settings.isShowCsbdeepProgress());
         
         // Map StarDist model choices to actual model file paths
         String modelChoice = settings.getModelChoice();
         String modelFilePath = mapModelChoiceToCSBDeepModel(modelChoice);
         
         LOGGER.debug("Model choice: {}, mapped to file path: {}", modelChoice, modelFilePath);
         
         if (modelFilePath != null) {
             // Convert resource path to absolute file path
             try {
                 java.net.URL resourceUrl = getClass().getClassLoader().getResource(modelFilePath);
                 if (resourceUrl != null) {
                     java.io.File modelFile = new java.io.File(resourceUrl.toURI());
                     params.put("modelFile", modelFile);
                     LOGGER.debug("Using CSBDeep model file: {} (absolute: {}) for StarDist choice: {}",
                                modelFilePath, modelFile.getAbsolutePath(), modelChoice);
                 } else {
                     LOGGER.error("Model resource not found: {}", modelFilePath);
                     // Try to extract from JAR to temporary file
                     java.io.File tempModelFile = extractModelToTempFile(modelFilePath);
                     if (tempModelFile != null) {
                         params.put("modelFile", tempModelFile);
                         LOGGER.debug("Extracted model to temporary file: {} for StarDist choice: {}",
                                    tempModelFile.getAbsolutePath(), modelChoice);
                     }
                 }
             } catch (Exception e) {
                 LOGGER.error("Failed to resolve model file path: {}", modelFilePath, e);
                 // Fallback: try to extract from JAR
                 java.io.File tempModelFile = extractModelToTempFile(modelFilePath);
                 if (tempModelFile != null) {
                     params.put("modelFile", tempModelFile);
                     LOGGER.debug("Fallback: extracted model to temporary file: {} for StarDist choice: {}",
                                tempModelFile.getAbsolutePath(), modelChoice);
                 }
             }
         } else {
             LOGGER.warn("Unknown model choice: {}, using default CSBDeep behavior", modelChoice);
         }
         
         return params;
     }
     
     /**
      * Maps StarDist model choices to actual model file paths in resources.
      *
      * @param modelChoice The StarDist model choice
      * @return The corresponding model file path, or null if not found
      */
     private String mapModelChoiceToCSBDeepModel(String modelChoice) {
         switch (modelChoice) {
             case "Versatile (H&E nuclei)":
                 return "models/2D/he_heavy_augment.zip";
             case "Versatile (fluorescent nuclei)":
                 return "models/2D/dsb2018_heavy_augment.zip";
             case "DSB 2018 (from StarDist 2D paper)":
                 return "models/2D/dsb2018_paper.zip";
             case "Model File":
                 // Custom model file - would need additional handling
                 return null;
             default:
                 LOGGER.warn("Unknown StarDist model choice: {}", modelChoice);
                 return "models/2D/he_heavy_augment.zip"; // Default fallback to H&E model
         }
     }
     
     /**
      * Extracts a model file from the JAR resources to a temporary file.
      *
      * @param resourcePath The resource path of the model file
      * @return The temporary file containing the extracted model, or null if extraction failed
      */
     private java.io.File extractModelToTempFile(String resourcePath) {
         try {
             // Get the resource as an input stream
             java.io.InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
             if (resourceStream == null) {
                 LOGGER.error("Model resource not found in classpath: {}", resourcePath);
                 return null;
             }
             
             // Create a temporary file
             String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
             String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
             String extension = fileName.substring(fileName.lastIndexOf('.'));
             
             java.io.File tempFile = java.io.File.createTempFile("stardist_model_" + baseName + "_", extension);
             tempFile.deleteOnExit();
             
             // Copy the resource to the temporary file
             try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
                  java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(fos)) {
                 
                 byte[] buffer = new byte[8192];
                 int bytesRead;
                 while ((bytesRead = resourceStream.read(buffer)) != -1) {
                     bos.write(buffer, 0, bytesRead);
                 }
                 bos.flush();
             }
             
             resourceStream.close();
             
             LOGGER.debug("Successfully extracted model {} to temporary file: {}",
                         resourcePath, tempFile.getAbsolutePath());
             
             return tempFile;
             
         } catch (java.io.IOException e) {
             LOGGER.error("Failed to extract model resource {} to temporary file", resourcePath, e);
             return null;
         }
     }
     /* Creates parameters for StarDist NMS execution.
     * 
     * @param probDataset The probability dataset
     * @param distDataset The distance dataset
     * @return Parameter map for NMS
     */
    private Map<String, Object> createStarDistNMSParameters(Dataset probDataset, Dataset distDataset) {
        Map<String, Object> params = new HashMap<>();
        
        params.put("prob", probDataset);
        params.put("dist", distDataset);
        params.put("probThresh", (double) settings.getProbThresh());
        params.put("nmsThresh", (double) settings.getNmsThresh());
        params.put("excludeBoundary", settings.getExcludeBoundary());
        params.put("roiPosition", settings.getRoiPosition());
        params.put("verbose", settings.isVerbose());
        params.put("outputType", "Polygons"); // We want polygons, not ROI Manager
        
        return params;
    }
    
    /**
     * Checks if StarDist plugin is available.
     * 
     * @return true if StarDist is available, false otherwise
     */
    public boolean isStarDistAvailable() {
        try {
            // Check if StarDist class can be loaded
            Class.forName("de.csbdresden.stardist.StarDist2D");
            Class.forName("de.csbdresden.stardist.StarDist2DNMS");
            
            // Check if CSBDeep is available (required by StarDist)
            Class.forName("de.csbdresden.csbdeep.commands.GenericNetwork");
            
            LOGGER.debug("StarDist and CSBDeep plugins are available");
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.debug("StarDist or CSBDeep plugin not found: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the current nuclear segmentation settings.
     * 
     * @return The settings instance
     */
    public NuclearSegmentationSettings getSettings() {
        return settings;
    }
    
    /**
     /**
      * Closes the SciJava context and releases resources.
      * Also restores the original ClassLoader.
      */
     public void close() {
         try {
             if (context != null) {
                 context.dispose();
                 LOGGER.debug("SciJava context disposed");
             }
         } finally {
             // Always try to restore the original ClassLoader
             Java21ClassLoaderFix.restoreOriginalClassLoader();
             LOGGER.debug("ClassLoader restored after StarDist operations");
         }
     }
    /**
     * Custom exception for StarDist-related errors.
     */
    public static class StarDistException extends Exception {
        
        public StarDistException(String message) {
            super(message);
        }
        
        public StarDistException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    @Override
    public String toString() {
        return String.format("StarDistIntegration[settings=%s, available=%s]", 
                           settings, isStarDistAvailable());
    }
}