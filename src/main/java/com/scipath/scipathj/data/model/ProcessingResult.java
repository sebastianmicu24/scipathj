package com.scipath.scipathj.data.model;

/**
 * Represents the result of image processing.
 * 
 * <p>This is a stub implementation for the initial skeleton version.</p>
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class ProcessingResult {
    
    private final String imageName;
    private final boolean success;
    
    public ProcessingResult(String imageName, boolean success) {
        this.imageName = imageName;
        this.success = success;
    }
    
    public String getImageName() {
        return imageName;
    }
    
    public boolean isSuccess() {
        return success;
    }
}