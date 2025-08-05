package com.scipath.scipathj.data.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the comprehensive result of image processing operations.
 *
 * <p>This class encapsulates all information about the processing of a single image,
 * including detected ROIs, processing metrics, timing information, and any errors
 * that occurred during processing.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class ProcessingResult {
    
    private final String imageName;
    private final boolean success;
    private final List<UserROI> detectedROIs;
    private final Map<String, Object> metrics;
    private final Instant processingStartTime;
    private final Duration processingDuration;
    private final Optional<Exception> error;
    private final String errorMessage;
    
    /**
     * Private constructor for creating processing results.
     * Use factory methods {@link #success} or {@link #failure} instead.
     */
    private ProcessingResult(String imageName, boolean success, List<UserROI> detectedROIs,
                           Map<String, Object> metrics, Duration processingDuration,
                           Exception error, String errorMessage) {
        this.imageName = imageName;
        this.success = success;
        this.detectedROIs = detectedROIs != null ? List.copyOf(detectedROIs) : Collections.emptyList();
        this.metrics = metrics != null ? Map.copyOf(metrics) : Collections.emptyMap();
        this.processingStartTime = Instant.now();
        this.processingDuration = processingDuration;
        this.error = Optional.ofNullable(error);
        this.errorMessage = errorMessage;
    }
    
    /**
     * Creates a successful processing result.
     *
     * @param imageName the name of the processed image
     * @param detectedROIs the list of ROIs detected in the image
     * @param metrics processing metrics and statistics
     * @return a successful processing result
     */
    public static ProcessingResult success(String imageName, List<UserROI> detectedROIs,
                                         Map<String, Object> metrics) {
        Duration duration = metrics.containsKey("processingTime")
            ? Duration.ofMillis(((Number) metrics.get("processingTime")).longValue())
            : Duration.ZERO;
        
        return new ProcessingResult(imageName, true, detectedROIs, metrics, duration, null, null);
    }
    
    /**
     * Creates a failed processing result.
     *
     * @param imageName the name of the image that failed to process
     * @param error the exception that caused the failure
     * @return a failed processing result
     */
    public static ProcessingResult failure(String imageName, Exception error) {
        String errorMessage = error != null ? error.getMessage() : "Unknown error";
        return new ProcessingResult(imageName, false, null, null, Duration.ZERO, error, errorMessage);
    }
    
    /**
     * Creates a failed processing result with a custom error message.
     *
     * @param imageName the name of the image that failed to process
     * @param errorMessage the error message describing the failure
     * @return a failed processing result
     */
    public static ProcessingResult failure(String imageName, String errorMessage) {
        return new ProcessingResult(imageName, false, null, null, Duration.ZERO, null, errorMessage);
    }
    
    /**
     * Gets the name of the processed image.
     *
     * @return the image name
     */
    public String getImageName() {
        return imageName;
    }
    
    /**
     * Checks if the processing was successful.
     *
     * @return true if processing succeeded, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Gets the list of ROIs detected in the image.
     *
     * @return immutable list of detected ROIs, empty if processing failed
     */
    public List<UserROI> getDetectedROIs() {
        return detectedROIs;
    }
    
    /**
     * Gets the processing metrics and statistics.
     *
     * @return immutable map of metrics, empty if processing failed
     */
    public Map<String, Object> getMetrics() {
        return metrics;
    }
    
    /**
     * Gets the time when processing started.
     *
     * @return processing start time
     */
    public Instant getProcessingStartTime() {
        return processingStartTime;
    }
    
    /**
     * Gets the duration of the processing operation.
     *
     * @return processing duration
     */
    public Duration getProcessingDuration() {
        return processingDuration;
    }
    
    /**
     * Gets the exception that caused processing to fail, if any.
     *
     * @return optional exception, empty if processing succeeded or failed without exception
     */
    public Optional<Exception> getError() {
        return error;
    }
    
    /**
     * Gets the error message describing why processing failed.
     *
     * @return error message, or null if processing succeeded
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Gets the number of ROIs detected.
     *
     * @return number of detected ROIs, 0 if processing failed
     */
    public int getROICount() {
        return detectedROIs.size();
    }
    
    /**
     * Gets a specific metric value.
     *
     * @param metricName the name of the metric
     * @return the metric value, or null if not present
     */
    public Object getMetric(String metricName) {
        return metrics.get(metricName);
    }
    
    /**
     * Gets a specific metric value as a number.
     *
     * @param metricName the name of the metric
     * @return the metric value as a number, or 0 if not present or not a number
     */
    public double getMetricAsDouble(String metricName) {
        Object value = metrics.get(metricName);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
    
    /**
     * Checks if the result has a specific metric.
     *
     * @param metricName the name of the metric
     * @return true if the metric is present
     */
    public boolean hasMetric(String metricName) {
        return metrics.containsKey(metricName);
    }
    
    @Override
    public String toString() {
        if (success) {
            return String.format("ProcessingResult{imageName='%s', success=true, roiCount=%d, duration=%dms}",
                               imageName, detectedROIs.size(), processingDuration.toMillis());
        } else {
            return String.format("ProcessingResult{imageName='%s', success=false, error='%s'}",
                               imageName, errorMessage);
        }
    }
}