package com.scipath.scipathj.dataset.core;

/**
 * Exception thrown when dataset operations fail.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public class DatasetException extends Exception {
    
    public DatasetException(String message) {
        super(message);
    }
    
    public DatasetException(String message, Throwable cause) {
        super(message, cause);
    }
}