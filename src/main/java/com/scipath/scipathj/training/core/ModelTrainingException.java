package com.scipath.scipathj.training.core;

/**
 * Exception thrown when model training operations fail.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public class ModelTrainingException extends Exception {
    
    public ModelTrainingException(String message) {
        super(message);
    }
    
    public ModelTrainingException(String message, Throwable cause) {
        super(message, cause);
    }
}