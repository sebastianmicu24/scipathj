package com.scipath.scipathj.core.pipeline;

/**
 * Exception thrown during pipeline execution
 */
public class PipelineException extends Exception {
    private final String stepId;
    private final String errorCode;
    
    public PipelineException(String message) {
        super(message);
        this.stepId = null;
        this.errorCode = "GENERAL_ERROR";
    }
    
    public PipelineException(String message, Throwable cause) {
        super(message, cause);
        this.stepId = null;
        this.errorCode = "GENERAL_ERROR";
    }
    
    public PipelineException(String stepId, String message, String errorCode) {
        super(message);
        this.stepId = stepId;
        this.errorCode = errorCode;
    }
    
    public PipelineException(String stepId, String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.stepId = stepId;
        this.errorCode = errorCode;
    }
    
    public String getStepId() {
        return stepId;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PipelineException");
        if (stepId != null) {
            sb.append(" [").append(stepId).append("]");
        }
        if (errorCode != null) {
            sb.append(" (").append(errorCode).append(")");
        }
        sb.append(": ").append(getMessage());
        return sb.toString();
    }
}