package com.scipath.scipathj.analysis;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.config.VesselSegmentationSettings;
import com.scipath.scipathj.data.model.UserROI;
import com.scipath.scipathj.ui.components.ROIManager;
import com.scipath.scipathj.ui.utils.UIConstants;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.Binary;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Vessel segmentation functionality for SciPathJ.
 * Performs thresholding-based vessel detection and creates ROIs from the segmented vessels.
 */
public class VesselSegmentation {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(VesselSegmentation.class);
    
    private final ImagePlus originalImage;
    private final String imageFileName;
    private final ROIManager roiManager;
    private final VesselSegmentationSettings settings;
    
    /**
     * Constructor for VesselSegmentation
     * 
     * @param originalImage The original image to segment
     * @param imageFileName The filename of the image for ROI association
     */
    public VesselSegmentation(ImagePlus originalImage, String imageFileName) {
        this.originalImage = originalImage;
        this.imageFileName = imageFileName;
        this.roiManager = ROIManager.getInstance();
        this.settings = ConfigurationManager.getInstance().initializeVesselSegmentationSettings();
    }
    
    /**
     * Perform vessel segmentation on the image.
     * This method:
     * 1. Converts the image to 8-bit grayscale (without saving changes)
     * 2. Applies Gaussian blur to reduce noise
     * 3. Applies threshold to select pixels with value > 150
     * 4. Processes the binary mask (fill holes)
     * 5. Creates VesselROI objects from the detected vessel shapes
     *
     * @return List of vessel ROIs created from the segmentation
     */
    public List<UserROI> segmentVessels() {
        return segmentVessels(settings.getThreshold());
    }
    
    /**
     * Perform vessel segmentation with custom threshold.
     *
     * @param threshold The threshold value (0-255) for pixel selection
     * @return List of UserROI objects (converted from VesselROI for compatibility)
     */
    public List<UserROI> segmentVessels(int threshold) {
        LOGGER.info("Starting vessel segmentation for image '{}' with threshold {}", imageFileName, threshold);
        
        List<UserROI> vesselROIs = new ArrayList<>();
        
        try {
            // Step 1: Create a duplicate for processing (don't modify original)
            ImagePlus workingImage = originalImage.duplicate();
            workingImage.setTitle("Vessel_Segmentation_" + System.currentTimeMillis());
            
            // Step 2: Convert to 8-bit grayscale if needed
            if (workingImage.getType() != ImagePlus.GRAY8) {
                LOGGER.debug("Converting image to 8-bit grayscale");
                IJ.run(workingImage, "8-bit", "");
            }
            
            // Step 3: Apply Gaussian blur to reduce noise (similar to SCHELI)
            GaussianBlur gaussianBlur = new GaussianBlur();
            gaussianBlur.blurGaussian(workingImage.getProcessor(), settings.getGaussianBlurSigma());
            LOGGER.debug("Applied Gaussian blur with sigma: {}", settings.getGaussianBlurSigma());
            
            // Step 4: Apply thresholding to select pixels > threshold
            ImageProcessor processor = workingImage.getProcessor();
            LOGGER.debug("Applying threshold: pixels > {}", threshold);
            
            // Create binary mask: pixels > threshold become 255 (white), others become 0 (black)
            processor.threshold(threshold);
            
            // Step 5: Process the binary mask (fill holes, similar to SCHELI)
            if (settings.isApplyMorphologicalClosing()) {
                processBinaryMask(workingImage);
            }
            
            // Step 6: Find vessels using particle analysis with actual shapes
            vesselROIs = findVesselsFromBinaryImage(workingImage);
            
            // Step 7: Add ROIs to the ROI manager
            for (UserROI roi : vesselROIs) {
                roiManager.addROI(roi);
            }
            
            LOGGER.info("Vessel segmentation completed. Found {} vessels", vesselROIs.size());
            
            // Clean up working image
            workingImage.changes = false;
            workingImage.close();
            
        } catch (Exception e) {
            LOGGER.error("Error during vessel segmentation for image '{}'", imageFileName, e);
            throw new RuntimeException("Vessel segmentation failed: " + e.getMessage(), e);
        }
        
        return vesselROIs;
    }
    
    /**
     * Process the binary mask to improve vessel detection (similar to SCHELI approach).
     * This includes filling holes and other morphological operations.
     */
    private void processBinaryMask(ImagePlus binaryImage) {
        LOGGER.debug("Processing vessel binary mask...");
        ImageProcessor processor = binaryImage.getProcessor();
        
        // Fill holes in the binary mask
        Binary binary = new Binary();
        binary.setup("fill", binaryImage);
        binary.run(processor);
        
        LOGGER.debug("Vessel binary mask processed: filled holes");
    }
    
    /**
     * Find vessels from a binary image using a wand tool approach.
     * This preserves the actual shape of the vessels without using ImageJ's ROI Manager.
     *
     * @param binaryImage The binary image with vessels as white regions
     * @return List of UserROI objects representing the detected vessels with their actual shapes
     */
    private List<UserROI> findVesselsFromBinaryImage(ImagePlus binaryImage) {
        List<UserROI> vesselROIs = new ArrayList<>();
        
        try {
            LOGGER.debug("Finding vessels using wand tool approach...");
            
            // Show the image temporarily (required for some ImageJ operations)
            binaryImage.show();
            
            // Hide the window by moving it off-screen
            if (binaryImage.getWindow() != null) {
                binaryImage.getWindow().setLocation(-2000, -2000);
            }
            
            ImageProcessor processor = binaryImage.getProcessor();
            int width = processor.getWidth();
            int height = processor.getHeight();
            
            // Create a mask to track which pixels have been processed
            boolean[][] processed = new boolean[width][height];
            
            // Use ImageJ's Wand tool to trace vessel contours
            ij.gui.Wand wand = new ij.gui.Wand(processor);
            int vesselCount = 0;
            
            // Scan the image for white pixels (vessels)
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // Check if this pixel is white (vessel) and not yet processed
                    if (processor.getPixel(x, y) == 255 && !processed[x][y]) {
                        // Use wand tool to trace the vessel boundary
                        wand.autoOutline(x, y, 255, 255); // Trace white pixels
                        
                        if (wand.npoints > 0) {
                            // Create polygon ROI from wand points
                            int[] xPoints = new int[wand.npoints];
                            int[] yPoints = new int[wand.npoints];
                            System.arraycopy(wand.xpoints, 0, xPoints, 0, wand.npoints);
                            System.arraycopy(wand.ypoints, 0, yPoints, 0, wand.npoints);
                            
                            ij.gui.PolygonRoi polygonRoi = new ij.gui.PolygonRoi(xPoints, yPoints, wand.npoints, Roi.POLYGON);
                            
                            // Check if the ROI meets size requirements
                            Rectangle bounds = polygonRoi.getBounds();
                            double area = polygonRoi.getStatistics().area;
                            
                            LOGGER.debug("Found potential vessel with area: {:.1f} (min: {:.1f}, max: {:.1f})",
                                       area, settings.getMinRoiSize(), settings.getMaxRoiSize());
                            
                            if (area >= settings.getMinRoiSize() && area <= settings.getMaxRoiSize()) {
                                vesselCount++;
                                String vesselName = "Vessel_" + vesselCount;
                                
                                // Create UserROI from the polygon ROI (preserves the actual shape)
                                UserROI vesselROI = new UserROI(polygonRoi, imageFileName, vesselName);
                                vesselROI.setDisplayColor(UIConstants.VESSEL_ROI_COLOR);
                                vesselROI.setNotes("Vessel detected by automated segmentation. Area: " +
                                                  String.format("%.1f", area) + " pixels");
                                
                                vesselROIs.add(vesselROI);
                                
                                LOGGER.debug("Created vessel ROI: {} [{}] area={:.1f} at ({}, {}) size {}x{}",
                                    vesselROI.getName(), vesselROI.getType(), vesselROI.getArea(),
                                    vesselROI.getX(), vesselROI.getY(),
                                    vesselROI.getWidth(), vesselROI.getHeight());
                                
                                // Mark all pixels in this ROI as processed
                                markROIAsProcessed(polygonRoi, processed);
                            } else {
                                LOGGER.debug("Vessel rejected due to size constraints: area={:.1f}", area);
                                // Still mark pixels as processed to avoid reprocessing
                                markROIAsProcessed(polygonRoi, processed);
                            }
                        }
                    }
                }
            }
            
            LOGGER.debug("Found {} vessels in the binary image", vesselCount);
            
        } catch (Exception e) {
            LOGGER.error("Error in vessel detection using wand tool", e);
            throw new RuntimeException("Vessel detection failed: " + e.getMessage(), e);
        } finally {
            // Ensure the binary image window is closed
            if (binaryImage != null && binaryImage.getWindow() != null) {
                binaryImage.changes = false;
                binaryImage.close();
            }
        }
        
        return vesselROIs;
    }
    
    /**
     * Mark all pixels within a ROI as processed to avoid duplicate detection
     */
    private void markROIAsProcessed(Roi roi, boolean[][] processed) {
        Rectangle bounds = roi.getBounds();
        for (int y = bounds.y; y < bounds.y + bounds.height; y++) {
            for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
                if (x >= 0 && x < processed.length && y >= 0 && y < processed[0].length) {
                    if (roi.contains(x, y)) {
                        processed[x][y] = true;
                    }
                }
            }
        }
    }
    
    
    /**
     * Get the current threshold value being used.
     *
     * @return The threshold value (0-255)
     */
    public static int getDefaultThreshold() {
        return VesselSegmentationSettings.DEFAULT_THRESHOLD;
    }
    
    /**
     * Get the minimum vessel size constraint.
     *
     * @return The minimum vessel area in pixels
     */
    public static double getMinVesselSize() {
        return VesselSegmentationSettings.DEFAULT_MIN_ROI_SIZE;
    }
    
    /**
     * Get the maximum vessel size constraint.
     *
     * @return The maximum vessel area in pixels
     */
    public static double getMaxVesselSize() {
        return VesselSegmentationSettings.DEFAULT_MAX_ROI_SIZE;
    }
    
    /**
     * Get the current settings instance being used.
     *
     * @return The VesselSegmentationSettings instance
     */
    public VesselSegmentationSettings getSettings() {
        return settings;
    }
}