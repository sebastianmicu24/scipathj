package com.scipath.scipathj.dataset.model;

import java.io.File;
import java.util.List;

/**
 * Immutable configuration record for dataset creation.
 * Contains all parameters needed to create a training dataset.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public record DatasetConfiguration(
    File roiZipFile,
    File imageFolder,
    List<String> selectedFeatures,
    boolean includeTextualFeatures,
    boolean includeMorphologicalFeatures,
    boolean includeColorFeatures
) {
    
    /**
     * Creates a default configuration with all features enabled.
     */
    public static DatasetConfiguration createDefault(File roiZipFile, File imageFolder) {
        return new DatasetConfiguration(
            roiZipFile,
            imageFolder,
            List.of(), // Empty list means all features
            true,
            true,
            true
        );
    }
    
    /**
     * Validates the configuration parameters.
     */
    public DatasetConfiguration {
        if (roiZipFile == null || !roiZipFile.exists()) {
            throw new IllegalArgumentException("ROI ZIP file must exist");
        }
        if (imageFolder == null || !imageFolder.exists() || !imageFolder.isDirectory()) {
            throw new IllegalArgumentException("Image folder must exist and be a directory");
        }
        if (selectedFeatures == null) {
            throw new IllegalArgumentException("Selected features list cannot be null");
        }
        if (!includeTextualFeatures && !includeMorphologicalFeatures && !includeColorFeatures) {
            throw new IllegalArgumentException("At least one feature type must be enabled");
        }
    }
    
    /**
     * Returns true if all feature types are enabled (default behavior).
     */
    public boolean isAllFeaturesEnabled() {
        return includeTextualFeatures && includeMorphologicalFeatures && includeColorFeatures 
               && selectedFeatures.isEmpty();
    }
    
    /**
     * Returns the image folder path as string.
     */
    public String getImageFolderPath() {
        return imageFolder.getAbsolutePath();
    }
    
    /**
     * Returns the ROI ZIP file path as string.
     */
    public String getRoiZipPath() {
        return roiZipFile.getAbsolutePath();
    }
}