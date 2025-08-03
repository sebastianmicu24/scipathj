package com.scipath.scipathj.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings class for vessel segmentation parameters.
 * Manages default values and user-configured values with persistence.
 */
public class VesselSegmentationSettings {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(VesselSegmentationSettings.class);
    
    // Default values
    public static final int DEFAULT_THRESHOLD = 220;
    public static final double DEFAULT_MIN_ROI_SIZE = 50.0;
    public static final double DEFAULT_MAX_ROI_SIZE = 10000.0;
    public static final double DEFAULT_GAUSSIAN_BLUR_SIGMA = 2.0;
    public static final boolean DEFAULT_APPLY_MORPHOLOGICAL_CLOSING = true;
    
    // Current values (initialized with defaults)
    private int threshold = DEFAULT_THRESHOLD;
    private double minRoiSize = DEFAULT_MIN_ROI_SIZE;
    private double maxRoiSize = DEFAULT_MAX_ROI_SIZE;
    private double gaussianBlurSigma = DEFAULT_GAUSSIAN_BLUR_SIGMA;
    private boolean applyMorphologicalClosing = DEFAULT_APPLY_MORPHOLOGICAL_CLOSING;
    
    // Singleton instance
    private static VesselSegmentationSettings instance;
    
    private VesselSegmentationSettings() {
        LOGGER.debug("VesselSegmentationSettings initialized with default values");
    }
    
    /**
     * Get the singleton instance of VesselSegmentationSettings.
     * 
     * @return The singleton instance
     */
    public static synchronized VesselSegmentationSettings getInstance() {
        if (instance == null) {
            instance = new VesselSegmentationSettings();
        }
        return instance;
    }
    
    // Getters
    public int getThreshold() {
        return threshold;
    }
    
    public double getMinRoiSize() {
        return minRoiSize;
    }
    
    public double getMaxRoiSize() {
        return maxRoiSize;
    }
    
    public double getGaussianBlurSigma() {
        return gaussianBlurSigma;
    }
    
    public boolean isApplyMorphologicalClosing() {
        return applyMorphologicalClosing;
    }
    
    // Setters with validation
    public void setThreshold(int threshold) {
        if (threshold < 0 || threshold > 255) {
            throw new IllegalArgumentException("Threshold must be between 0 and 255");
        }
        this.threshold = threshold;
        LOGGER.debug("Threshold set to: {}", threshold);
    }
    
    public void setMinRoiSize(double minRoiSize) {
        if (minRoiSize < 0) {
            throw new IllegalArgumentException("Minimum ROI size must be non-negative");
        }
        if (minRoiSize > maxRoiSize) {
            throw new IllegalArgumentException("Minimum ROI size cannot be greater than maximum ROI size");
        }
        this.minRoiSize = minRoiSize;
        LOGGER.debug("Minimum ROI size set to: {}", minRoiSize);
    }
    
    public void setMaxRoiSize(double maxRoiSize) {
        if (maxRoiSize < 0) {
            throw new IllegalArgumentException("Maximum ROI size must be non-negative");
        }
        if (maxRoiSize < minRoiSize) {
            throw new IllegalArgumentException("Maximum ROI size cannot be less than minimum ROI size");
        }
        this.maxRoiSize = maxRoiSize;
        LOGGER.debug("Maximum ROI size set to: {}", maxRoiSize);
    }
    
    public void setGaussianBlurSigma(double gaussianBlurSigma) {
        if (gaussianBlurSigma < 0) {
            throw new IllegalArgumentException("Gaussian blur sigma must be non-negative");
        }
        this.gaussianBlurSigma = gaussianBlurSigma;
        LOGGER.debug("Gaussian blur sigma set to: {}", gaussianBlurSigma);
    }
    
    public void setApplyMorphologicalClosing(boolean applyMorphologicalClosing) {
        this.applyMorphologicalClosing = applyMorphologicalClosing;
        LOGGER.debug("Apply morphological closing set to: {}", applyMorphologicalClosing);
    }
    
    /**
     * Reset all settings to their default values.
     */
    public void resetToDefaults() {
        threshold = DEFAULT_THRESHOLD;
        minRoiSize = DEFAULT_MIN_ROI_SIZE;
        maxRoiSize = DEFAULT_MAX_ROI_SIZE;
        gaussianBlurSigma = DEFAULT_GAUSSIAN_BLUR_SIGMA;
        applyMorphologicalClosing = DEFAULT_APPLY_MORPHOLOGICAL_CLOSING;
        LOGGER.info("Vessel segmentation settings reset to defaults");
    }
    
    /**
     * Check if current settings are different from defaults.
     * 
     * @return true if any setting differs from its default value
     */
    public boolean hasCustomValues() {
        return threshold != DEFAULT_THRESHOLD ||
               minRoiSize != DEFAULT_MIN_ROI_SIZE ||
               maxRoiSize != DEFAULT_MAX_ROI_SIZE ||
               gaussianBlurSigma != DEFAULT_GAUSSIAN_BLUR_SIGMA ||
               applyMorphologicalClosing != DEFAULT_APPLY_MORPHOLOGICAL_CLOSING;
    }
    
    /**
     * Get a string representation of current settings.
     * 
     * @return String representation of settings
     */
    @Override
    public String toString() {
        return String.format("VesselSegmentationSettings{threshold=%d, minRoiSize=%.1f, maxRoiSize=%.1f, " +
                           "gaussianBlurSigma=%.1f, applyMorphologicalClosing=%b}",
                           threshold, minRoiSize, maxRoiSize, gaussianBlurSigma, applyMorphologicalClosing);
    }
    
    /**
     * Validate that all current settings are within acceptable ranges.
     * 
     * @throws IllegalStateException if any setting is invalid
     */
    public void validate() {
        if (threshold < 0 || threshold > 255) {
            throw new IllegalStateException("Invalid threshold value: " + threshold);
        }
        if (minRoiSize < 0) {
            throw new IllegalStateException("Invalid minimum ROI size: " + minRoiSize);
        }
        if (maxRoiSize < 0) {
            throw new IllegalStateException("Invalid maximum ROI size: " + maxRoiSize);
        }
        if (minRoiSize > maxRoiSize) {
            throw new IllegalStateException("Minimum ROI size cannot be greater than maximum ROI size");
        }
        if (gaussianBlurSigma < 0) {
            throw new IllegalStateException("Invalid Gaussian blur sigma: " + gaussianBlurSigma);
        }
    }
}