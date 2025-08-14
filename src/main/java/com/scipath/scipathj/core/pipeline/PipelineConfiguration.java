package com.scipath.scipathj.core.pipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration settings for pipeline execution
 */
public class PipelineConfiguration {
  private final Map<String, Object> parameters;
  private final String organType;
  private final String stainingType;
  private final boolean enableParallelProcessing;
  private final int maxThreads;
  private final double pixelSizeX;
  private final double pixelSizeY;
  private final String pixelUnit;

  public PipelineConfiguration(String organType, String stainingType) {
    this.organType = organType;
    this.stainingType = stainingType;
    this.parameters = new HashMap<>();
    this.enableParallelProcessing = true;
    this.maxThreads = Runtime.getRuntime().availableProcessors();
    this.pixelSizeX = 1.0;
    this.pixelSizeY = 1.0;
    this.pixelUnit = "pixel";
  }

  public PipelineConfiguration(
      String organType,
      String stainingType,
      boolean enableParallelProcessing,
      int maxThreads,
      double pixelSizeX,
      double pixelSizeY,
      String pixelUnit) {
    this.organType = organType;
    this.stainingType = stainingType;
    this.enableParallelProcessing = enableParallelProcessing;
    this.maxThreads = maxThreads;
    this.pixelSizeX = pixelSizeX;
    this.pixelSizeY = pixelSizeY;
    this.pixelUnit = pixelUnit;
    this.parameters = new HashMap<>();
  }

  // Parameter management
  public void setParameter(String key, Object value) {
    parameters.put(key, value);
  }

  public Object getParameter(String key) {
    return parameters.get(key);
  }

  public Object getParameter(String key, Object defaultValue) {
    return parameters.getOrDefault(key, defaultValue);
  }

  public boolean hasParameter(String key) {
    return parameters.containsKey(key);
  }

  // Getters
  public String getOrganType() {
    return organType;
  }

  public String getStainingType() {
    return stainingType;
  }

  public boolean isParallelProcessingEnabled() {
    return enableParallelProcessing;
  }

  public int getMaxThreads() {
    return maxThreads;
  }

  public double getPixelSizeX() {
    return pixelSizeX;
  }

  public double getPixelSizeY() {
    return pixelSizeY;
  }

  public String getPixelUnit() {
    return pixelUnit;
  }

  public Map<String, Object> getAllParameters() {
    return new HashMap<>(parameters);
  }

  public Properties toProperties() {
    Properties props = new Properties();
    props.setProperty("organType", organType);
    props.setProperty("stainingType", stainingType);
    props.setProperty("enableParallelProcessing", String.valueOf(enableParallelProcessing));
    props.setProperty("maxThreads", String.valueOf(maxThreads));
    props.setProperty("pixelSizeX", String.valueOf(pixelSizeX));
    props.setProperty("pixelSizeY", String.valueOf(pixelSizeY));
    props.setProperty("pixelUnit", pixelUnit);

    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
      props.setProperty(entry.getKey(), entry.getValue().toString());
    }

    return props;
  }
}
