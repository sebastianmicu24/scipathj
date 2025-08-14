package com.scipath.scipathj.ui.model;

/**
 * Represents information about an available analysis pipeline.
 *
 * <p>This class encapsulates the metadata and configuration for different
 * histopathological analysis pipelines available in SciPathJ.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class PipelineInfo {

  private final String id;
  private final String displayName;
  private final String description;
  private final boolean enabled;
  private final String[] steps;

  /**
   * Creates a new PipelineInfo instance.
   *
   * @param id unique identifier for the pipeline
   * @param displayName user-friendly display name
   * @param description detailed description of the pipeline
   * @param enabled whether the pipeline is currently available
   * @param steps array of pipeline step names
   */
  public PipelineInfo(
      String id, String displayName, String description, boolean enabled, String[] steps) {
    this.id = id;
    this.displayName = displayName;
    this.description = description;
    this.enabled = enabled;
    this.steps = steps.clone();
  }

  /**
   * Gets the unique identifier for this pipeline.
   *
   * @return pipeline ID
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the user-friendly display name.
   *
   * @return display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Gets the detailed description of the pipeline.
   *
   * @return pipeline description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Checks if the pipeline is currently enabled and available for use.
   *
   * @return true if enabled, false otherwise
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Gets the array of pipeline step names.
   *
   * @return array of step names
   */
  public String[] getSteps() {
    return steps.clone();
  }

  /**
   * Gets a formatted string representation of the pipeline steps.
   *
   * @return formatted steps string
   */
  public String getFormattedSteps() {
    return String.join(" → ", steps);
  }

  @Override
  public String toString() {
    return displayName;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    PipelineInfo that = (PipelineInfo) obj;
    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
