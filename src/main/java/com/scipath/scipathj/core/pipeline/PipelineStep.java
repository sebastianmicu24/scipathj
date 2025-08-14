package com.scipath.scipathj.core.pipeline;

import com.scipath.scipathj.data.model.ProcessingResult;

/**
 * Interface for individual pipeline steps
 */
public interface PipelineStep {

  /**
   * Get the unique identifier for this step
   */
  String getId();

  /**
   * Get the display name for this step
   */
  String getName();

  /**
   * Get the description of what this step does
   */
  String getDescription();

  /**
   * Execute this pipeline step
   * @param input The input data for this step
   * @return The result of processing
   * @throws PipelineException if processing fails
   */
  ProcessingResult execute(ProcessingResult input) throws PipelineException;

  /**
   * Validate that this step can be executed with the given input
   * @param input The input to validate
   * @return ValidationResult indicating if the step can proceed
   */
  ValidationResult validate(ProcessingResult input);

  /**
   * Get the estimated processing time for this step
   * @param input The input data
   * @return Estimated time in milliseconds
   */
  long getEstimatedProcessingTime(ProcessingResult input);

  /**
   * Check if this step can be cancelled during execution
   */
  boolean isCancellable();

  /**
   * Cancel the execution of this step if possible
   */
  void cancel();

  /**
   * Get the current progress of this step (0.0 to 1.0)
   */
  double getProgress();
}
