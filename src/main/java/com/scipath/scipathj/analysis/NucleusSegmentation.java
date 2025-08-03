package com.scipath.scipathj.analysis;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.config.NuclearSegmentationSettings;
import com.scipath.scipathj.core.engine.StarDistIntegration;
import com.scipath.scipathj.data.model.NucleusROI;
import com.scipath.scipathj.ui.components.ROIManager;
import ij.ImagePlus;
import ij.gui.Roi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Nuclear segmentation functionality for SciPathJ using StarDist.
 * Now uses a simplified direct approach with the Versatile H&E model as primary method,
 * with fallback to the complex integration if needed.
 *
 * @author Sebastian Micu
 * @version 3.0.0
 * @since 1.0.0
 */
public class NucleusSegmentation {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NucleusSegmentation.class);
    
    private final ImagePlus originalImage;
    private final String imageFileName;
    private final ROIManager roiManager;
    private final NuclearSegmentationSettings settings;
    private final StarDistIntegration starDistIntegration;
    private final SimpleHENuclearSegmentation simpleHESegmentation;
    private List<NucleusROI> nucleiROIs;
    
    /**
     * Constructor for NucleusSegmentation.
     * 
     * @param originalImage The original image to segment
     * @param imageFileName The filename of the image for ROI association
     */
    public NucleusSegmentation(ImagePlus originalImage, String imageFileName) {
        this.originalImage = originalImage;
        this.imageFileName = imageFileName;
        this.roiManager = ROIManager.getInstance();
        this.settings = ConfigurationManager.getInstance().initializeNuclearSegmentationSettings();
        this.starDistIntegration = new StarDistIntegration(settings);
        this.simpleHESegmentation = new SimpleHENuclearSegmentation(originalImage, imageFileName, settings);
        this.nucleiROIs = new ArrayList<>();
    }
    
    /**
     * Perform nucleus segmentation using StarDist.
     * Uses the simplified direct approach first, with fallback to complex integration if needed.
     *
     * @return List of NucleusROI objects representing detected nuclei
     * @throws NucleusSegmentationException if segmentation fails
     */
    public List<NucleusROI> segmentNuclei() throws NucleusSegmentationException {
        LOGGER.info("Starting nucleus segmentation for image '{}' using direct StarDist H&E approach", imageFileName);
        
        // Clear any existing nuclei ROIs
        nucleiROIs.clear();
        
        // Validate input image
        if (originalImage == null) {
            throw new NucleusSegmentationException("Original image is null for file: " + imageFileName);
        }
        
        try {
            // Use ONLY the simplified H&E approach that works in tests
            LOGGER.debug("Using simplified StarDist H&E segmentation approach (same as test)");
            nucleiROIs = simpleHESegmentation.segmentNuclei();
            
            // Filter ROIs based on size criteria
            filterROIsBySize();
            
            // Add nuclei ROIs to the main ROI manager
            for (NucleusROI nucleusROI : nucleiROIs) {
                roiManager.addROI(nucleusROI);
            }
            
            LOGGER.info("Simplified H&E nucleus segmentation completed successfully. Found {} valid nuclei", nucleiROIs.size());
            return nucleiROIs;
            
        } catch (Exception e) {
            LOGGER.error("Simple H&E segmentation failed for image '{}'", imageFileName, e);
            throw new NucleusSegmentationException("StarDist H&E segmentation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fallback segmentation using the complex StarDist integration.
     */
    private List<NucleusROI> segmentNucleiFallback() throws NucleusSegmentationException {
        LOGGER.info("Using fallback StarDist integration for image '{}'", imageFileName);
        
        // Check StarDist availability
        if (!starDistIntegration.isStarDistAvailable()) {
            throw new NucleusSegmentationException(
                "StarDist plugin is not available. Please ensure StarDist is properly installed.");
        }
        
        try {
            LOGGER.debug("StarDist is available, proceeding with fallback segmentation");
            
            // Execute StarDist segmentation
            Roi[] detectedRois = starDistIntegration.executeStarDist(originalImage);
            
            // Convert ROIs to NucleusROI objects
            convertRoisToNucleusROIs(detectedRois);
            
            // Filter ROIs based on size criteria
            filterROIsBySize();
            
            // Add nuclei ROIs to the main ROI manager
            for (NucleusROI nucleusROI : nucleiROIs) {
                roiManager.addROI(nucleusROI);
            }
            
            LOGGER.info("Fallback nucleus segmentation completed successfully. Found {} valid nuclei", nucleiROIs.size());
            
        } catch (StarDistIntegration.StarDistException e) {
            LOGGER.error("Fallback StarDist execution failed for image '{}'", imageFileName, e);
            throw new NucleusSegmentationException("Fallback StarDist execution failed: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error during fallback nucleus segmentation for image '{}'", imageFileName, e);
            throw new NucleusSegmentationException("Fallback nucleus segmentation failed: " + e.getMessage(), e);
        }
        
        return nucleiROIs;
    }
    
    /**
     * Converts ImageJ ROIs to NucleusROI objects.
     * This follows the SCHELI pattern of naming ROIs as Nucleus_1, Nucleus_2, etc.
     * 
     * @param rois Array of ImageJ ROIs from StarDist
     */
    private void convertRoisToNucleusROIs(Roi[] rois) {
        if (rois == null || rois.length == 0) {
            LOGGER.warn("No ROIs detected by StarDist");
            return;
        }
        
        LOGGER.debug("Converting {} ROIs to NucleusROI objects", rois.length);
        
        for (int i = 0; i < rois.length; i++) {
            Roi roi = rois[i];
            
            if (roi == null) {
                LOGGER.warn("Skipping null ROI at index {}", i);
                continue;
            }
            
            // Create nucleus name following SCHELI pattern
            String nucleusName = "Nucleus_" + (i + 1);
            
            // Create NucleusROI from the ImageJ ROI
            NucleusROI nucleusROI = new NucleusROI(roi, imageFileName, nucleusName);
            nucleusROI.setSegmentationMethod("StarDist");
            nucleusROI.setNotes(String.format(
                "Nucleus detected by StarDist. Model: %s, Prob threshold: %.3f, NMS threshold: %.3f",
                settings.getModelChoice(), settings.getProbThresh(), settings.getNmsThresh()
            ));
            
            nucleiROIs.add(nucleusROI);
            
            LOGGER.debug("Created nucleus ROI: {} with area: {:.1f} pixels",
                       nucleusName, nucleusROI.getNucleusArea());
        }
        
        LOGGER.debug("Successfully converted {} ROIs to NucleusROI objects", nucleiROIs.size());
    }
    
    /**
     * Filters ROIs based on size criteria to remove noise and artifacts.
     */
    private void filterROIsBySize() {
        int beforeCount = nucleiROIs.size();
        
        nucleiROIs = nucleiROIs.stream()
            .filter(roi -> {
                double area = roi.getNucleusArea();
                boolean sizeValid = area >= settings.getMinNucleusSize() && 
                                  area <= settings.getMaxNucleusSize();
                
                if (!sizeValid) {
                    LOGGER.debug("Filtered out nucleus {} due to size: {:.1f} (range: {:.1f}-{:.1f})",
                               roi.getName(), area, settings.getMinNucleusSize(), settings.getMaxNucleusSize());
                }
                
                roi.setValid(sizeValid);
                return sizeValid;
            })
            .collect(Collectors.toList());
        
        int afterCount = nucleiROIs.size();
        int filteredCount = beforeCount - afterCount;
        
        if (filteredCount > 0) {
            LOGGER.info("Filtered out {} nuclei due to size constraints. {} nuclei remain.",
                       filteredCount, afterCount);
        }
    }
    
    /**
     * Gets the list of detected nucleus ROIs.
     * 
     * @return List of NucleusROI objects
     */
    public List<NucleusROI> getNucleiROIs() {
        return new ArrayList<>(nucleiROIs);
    }
    
    /**
     * Gets the current nuclear segmentation settings.
     * 
     * @return The NuclearSegmentationSettings instance
     */
    public NuclearSegmentationSettings getSettings() {
        return settings;
    }
    
    /**
     * Gets the number of detected nuclei.
     * 
     * @return The count of detected nuclei
     */
    public int getNucleiCount() {
        return nucleiROIs.size();
    }
    
    /**
     * Gets statistics about the detected nuclei.
     * 
     * @return A formatted string with nucleus statistics
     */
    public String getNucleiStatistics() {
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
        
        double avgCircularity = nucleiROIs.stream()
            .mapToDouble(NucleusROI::getCircularity)
            .average()
            .orElse(0.0);
        
        return String.format(
            "Nuclei: %d, Total area: %.1f px, Avg area: %.1f px (range: %.1f-%.1f), Avg circularity: %.3f",
            nucleiROIs.size(), totalArea, avgArea, minArea, maxArea, avgCircularity
        );
    }
    
    /**
     * Adds all nucleus ROIs to the main ROI manager.
     */
    public void addToRoiManager() {
        for (NucleusROI nucleusROI : nucleiROIs) {
            roiManager.addROI(nucleusROI);
        }
        LOGGER.debug("Added {} nucleus ROIs to main ROI manager", nucleiROIs.size());
    }
    
    /**
     * Gets the StarDist integration instance.
     * 
     * @return The StarDistIntegration instance
     */
    public StarDistIntegration getStarDistIntegration() {
        return starDistIntegration;
    }
    
    /**
     * Checks if StarDist is available for segmentation.
     *
     * @return true if StarDist is available, false otherwise
     */
    public boolean isStarDistAvailable() {
        return simpleHESegmentation.isAvailable();
    }
    
    /**
     * Gets detailed information about the StarDist integration status.
     *
     * @return A string with StarDist status information
     */
    public String getStarDistStatus() {
        if (simpleHESegmentation.isAvailable()) {
            return "StarDist is available (Simple H&E approach). Optimized for H&E nuclear segmentation.";
        } else {
            return "StarDist is not available. Please ensure StarDist plugin is properly installed.";
        }
    }
    
    /**
     * Gets the simple H&E segmentation instance for advanced usage.
     *
     * @return The SimpleHENuclearSegmentation instance
     */
    public SimpleHENuclearSegmentation getSimpleHESegmentation() {
        return simpleHESegmentation;
    }
    
    /**
     * Close resources and cleanup.
     */
    public void close() {
        try {
            if (simpleHESegmentation != null) {
                simpleHESegmentation.close();
            }
            if (starDistIntegration != null) {
                starDistIntegration.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Error during cleanup", e);
        }
    }
    
    /**
     * Custom exception for nucleus segmentation errors.
     */
    public static class NucleusSegmentationException extends Exception {
        
        public NucleusSegmentationException(String message) {
            super(message);
        }
        
        public NucleusSegmentationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    @Override
    public String toString() {
        return String.format("NucleusSegmentation[image=%s, nuclei=%d, simpleHEAvailable=%s, fallbackAvailable=%s]",
                           imageFileName, nucleiROIs.size(),
                           simpleHESegmentation.isAvailable(), starDistIntegration.isStarDistAvailable());
    }
}