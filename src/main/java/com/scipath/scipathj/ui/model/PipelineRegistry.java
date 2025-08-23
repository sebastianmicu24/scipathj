package com.scipath.scipathj.ui.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry for managing available analysis pipelines.
 *
 * <p>This class provides centralized access to pipeline information and
 * manages the list of available pipelines in the application.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class PipelineRegistry {

  private static final List<PipelineInfo> AVAILABLE_PIPELINES = new ArrayList<>();

  static {
    // Initialize available pipelines
    initializePipelines();
  }

  /**
   * Initializes the list of available pipelines.
   */
  private static void initializePipelines() {
    // H&E Liver Pipeline - Currently available
    AVAILABLE_PIPELINES.add(
        new PipelineInfo(
            "full_he",
            "Full H&E Analysis",
            "Histopathological analysis of parenchymatous tissues stained with H&E - batch or single image analysis",
            true,
            new String[] {
              "Vessel Segmentation",
              "Nuclear Segmentation",
              "Cell Creation",
              "Feature Extraction",
              "Cell Classification",
              "Final Analysis"
            }));

    // H&E Kidney Pipeline - Coming soon
    AVAILABLE_PIPELINES.add(
        new PipelineInfo(
            "dataset_creator",
            "Create Dataset",
            "Select your cells and create a custom classification model",
            false,
            new String[] {
              "Vessel Segmentation",
              "Nuclear Segmentation",
              "Cell Creation",
              "Feature Extraction",
              "Cell Classification",
              "Final Analysis"
            }));

    // H&E White Adipose Tissue Pipeline - Coming soon
    AVAILABLE_PIPELINES.add(
        new PipelineInfo(
            "View_Results",
            "View Results",
            "Visualize the features and classes of your segmented cells",
            false,
            new String[] {
              "Vessel Segmentation",
              "Nuclear Segmentation",
              "Cell Creation",
              "Feature Extraction",
              "Cell Classification",
              "Final Analysis"
            }));
  }

  /**
   * Gets all available pipelines.
   *
   * @return list of all pipeline information
   */
  public static List<PipelineInfo> getAllPipelines() {
    return new ArrayList<>(AVAILABLE_PIPELINES);
  }

  /**
   * Gets only the enabled pipelines.
   *
   * @return list of enabled pipeline information
   */
  public static List<PipelineInfo> getEnabledPipelines() {
    return AVAILABLE_PIPELINES.stream().filter(PipelineInfo::isEnabled).toList();
  }

  /**
   * Gets a pipeline by its ID.
   *
   * @param id the pipeline ID
   * @return pipeline information, or null if not found
   */
  public static PipelineInfo getPipelineById(String id) {
    return AVAILABLE_PIPELINES.stream()
        .filter(pipeline -> pipeline.getId().equals(id))
        .findFirst()
        .orElse(null);
  }

  /**
   * Checks if a pipeline exists and is enabled.
   *
   * @param id the pipeline ID
   * @return true if the pipeline exists and is enabled
   */
  public static boolean isPipelineAvailable(String id) {
    PipelineInfo pipeline = getPipelineById(id);
    return pipeline != null && pipeline.isEnabled();
  }
}
