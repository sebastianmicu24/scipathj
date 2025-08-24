package com.scipath.scipathj.infrastructure.pipeline;

import com.scipath.scipathj.roi.model.ImageMetadata;
import com.scipath.scipathj.roi.model.ProcessingResult;
import ij.ImagePlus;
import java.util.List;

/**
 * Interface defining the contract for image analysis pipelines.
 *
 * <p>A pipeline represents a complete workflow for analyzing histopathological images,
 * including segmentation, feature extraction, and classification steps.</p>
 *
 * <p>Implementations should be thread-safe and stateless to allow concurrent execution.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public interface Pipeline {

  /**
   * Gets the unique name of this pipeline.
   *
   * @return pipeline name (e.g., "Liver H&E Analysis")
   */
  String getName();

  /**
   * Gets a human-readable description of this pipeline.
   *
   * @return pipeline description
   */
  String getDescription();

  /**
   * Gets the version of this pipeline.
   *
   * @return pipeline version string
   */
  String getVersion();

  /**
   * Gets the list of processing steps in this pipeline.
   *
   * @return ordered list of pipeline steps
   */
  List<PipelineStep> getSteps();

  /**
   * Gets the default configuration for this pipeline.
   *
   * @return default pipeline configuration
   */
  PipelineConfiguration getDefaultConfiguration();

  /**
   * Checks if this pipeline is compatible with the given image metadata.
   *
   * @param metadata image metadata to check
   * @return true if compatible, false otherwise
   */
  boolean isCompatible(ImageMetadata metadata);

  /**
   * Executes the complete pipeline on the given image.
   *
   * @param image the image to process
   * @param config pipeline configuration
   * @return processing result containing all analysis data
   * @throws PipelineException if processing fails
   */
  ProcessingResult execute(ImagePlus image, PipelineConfiguration config) throws PipelineException;

  /**
   * Validates the pipeline configuration.
   *
   * @param config configuration to validate
   * @return validation result with any errors or warnings
   */
  ValidationResult validateConfiguration(PipelineConfiguration config);

  /**
   * Gets the estimated processing time for an image of the given size.
   *
   * @param imageWidth image width in pixels
   * @param imageHeight image height in pixels
   * @return estimated processing time in milliseconds
   */
  long estimateProcessingTime(int imageWidth, int imageHeight);

  /**
   * Gets the supported image formats for this pipeline.
   *
   * @return list of supported file extensions (e.g., "tif", "png", "jpg")
   */
  List<String> getSupportedFormats();

  /**
   * Gets metadata about this pipeline including author, creation date, etc.
   *
   * @return pipeline metadata
   */
  PipelineMetadata getMetadata();
}
