package com.scipath.scipathj.dataset.repository;

import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.dataset.core.DatasetException;

import java.io.File;
import java.util.Map;
import java.util.List;

/**
 * Repository interface for ROI data access.
 * Provides clean abstraction for ROI loading and management.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public interface ROIRepository extends AutoCloseable {
    
    /**
     * Loads ROIs from a ZIP file for a specific image folder.
     * 
     * @param roiZipFile ZIP file containing ROI data
     * @param imageFolder folder containing corresponding images
     * @return map of ROI key to UserROI objects
     * @throws DatasetException if loading fails
     */
    Map<String, UserROI> loadROIs(File roiZipFile, File imageFolder) throws DatasetException;
    
    /**
     * Loads ROIs for a specific image.
     * 
     * @param roiZipFile ZIP file containing ROI data
     * @param imageName name of the specific image
     * @return list of ROIs for the image
     * @throws DatasetException if loading fails
     */
    List<UserROI> loadROIsForImage(File roiZipFile, String imageName) throws DatasetException;
    
    /**
     * Saves ROIs to a ZIP file.
     * 
     * @param rois ROIs to save
     * @param outputFile output ZIP file
     * @throws DatasetException if saving fails
     */
    void saveROIs(Map<String, UserROI> rois, File outputFile) throws DatasetException;
    
    /**
     * Filters ROIs by type.
     * 
     * @param rois input ROIs
     * @param types types to include
     * @return filtered ROIs
     */
    Map<String, UserROI> filterByType(Map<String, UserROI> rois, UserROI.ROIType... types);
    
    /**
     * Filters ROIs to include only classified ones.
     * 
     * @param rois input ROIs
     * @return ROIs with assigned classes (excluding "Unclassified")
     */
    Map<String, UserROI> filterClassified(Map<String, UserROI> rois);
    
    @Override
    default void close() {
        // Default implementation - override if cleanup needed
    }
}