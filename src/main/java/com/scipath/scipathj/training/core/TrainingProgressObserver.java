package com.scipath.scipathj.training.core;

import com.scipath.scipathj.training.model.TrainingResult;

/**
 * Observer interface for monitoring training progress.
 * Implements the Observer pattern for training notifications.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public interface TrainingProgressObserver {
    
    /**
     * Called when training progress updates.
     * 
     * @param current current step
     * @param total total steps
     * @param status current status message
     */
    void onProgressUpdate(int current, int total, String status);
    
    /**
     * Called when an error occurs during training.
     * 
     * @param error the error that occurred
     */
    void onError(Exception error);
    
    /**
     * Called when training completes successfully.
     * 
     * @param result the training result
     */
    void onComplete(TrainingResult result);
}