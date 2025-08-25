package com.scipath.scipathj.infrastructure.roi;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Core ROI service interface that replaces the singleton ROIManager.
 * Provides clean separation of concerns and allows for different implementations
 * for analysis, dataset, and visualization contexts.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 */
public interface ROIService {
    
    /**
     * Add a ROI to the service.
     */
    void addROI(UserROI roi);
    
    /**
     * Remove a ROI by ID.
     */
    boolean removeROI(String roiId);
    
    /**
     * Remove a specific ROI.
     */
    boolean removeROI(UserROI roi);
    
    /**
     * Get all ROIs for a specific image.
     */
    List<UserROI> getROIsForImage(String imageFileName);
    
    /**
     * Get all ROIs across all images.
     */
    List<UserROI> getAllROIs();
    
    /**
     * Get all ROIs organized by image filename.
     */
    Map<String, List<UserROI>> getAllROIsByImage();
    
    /**
     * Clear all ROIs for a specific image.
     */
    void clearROIsForImage(String imageFileName);
    
    /**
     * Clear all ROIs from all images.
     */
    void clearAllROIs();
    
    /**
     * Get ROI count for a specific image.
     */
    int getROICount(String imageFileName);
    
    /**
     * Get total ROI count across all images.
     */
    int getTotalROICount();
    
    /**
     * Check if an image has any ROIs.
     */
    boolean hasROIs(String imageFileName);
    
    /**
     * Find ROI by ID.
     */
    UserROI findROIById(String roiId);
    
    /**
     * Save ROIs for a specific image to file.
     */
    void saveROIsToFile(String imageFileName, File outputFile) throws IOException;
    
    /**
     * Save all ROIs to a master ZIP file.
     */
    void saveAllROIsToMasterZip(File outputFile) throws IOException;
    
    /**
     * Load ROIs from file for a specific image.
     */
    List<UserROI> loadROIsFromFile(File inputFile, String imageFileName) throws IOException;
    
    /**
     * Add a change listener for ROI events.
     */
    void addChangeListener(ROIChangeListener listener);
    
    /**
     * Remove a change listener.
     */
    void removeChangeListener(ROIChangeListener listener);
    
    /**
     * Interface for listening to ROI changes.
     */
    interface ROIChangeListener {
        void onROIAdded(UserROI roi);
        void onROIRemoved(UserROI roi);
        void onROIUpdated(UserROI roi);
        void onROIsCleared(String imageFileName);
    }
}