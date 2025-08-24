package com.scipath.scipathj.roi.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Metadata information for processed images
 */
public class ImageMetadata {
  private final String filename;
  private final int width;
  private final int height;
  private final int channels;
  private final String pixelType;
  private final double pixelSizeX;
  private final double pixelSizeY;
  private final String unit;
  private final LocalDateTime acquisitionTime;
  private final Map<String, Object> customProperties;

  public ImageMetadata(
      String filename,
      int width,
      int height,
      int channels,
      String pixelType,
      double pixelSizeX,
      double pixelSizeY,
      String unit) {
    this.filename = filename;
    this.width = width;
    this.height = height;
    this.channels = channels;
    this.pixelType = pixelType;
    this.pixelSizeX = pixelSizeX;
    this.pixelSizeY = pixelSizeY;
    this.unit = unit;
    this.acquisitionTime = LocalDateTime.now();
    this.customProperties = new HashMap<>();
  }

  // Getters
  public String getFilename() {
    return filename;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int getChannels() {
    return channels;
  }

  public String getPixelType() {
    return pixelType;
  }

  public double getPixelSizeX() {
    return pixelSizeX;
  }

  public double getPixelSizeY() {
    return pixelSizeY;
  }

  public String getUnit() {
    return unit;
  }

  public LocalDateTime getAcquisitionTime() {
    return acquisitionTime;
  }

  public Map<String, Object> getCustomProperties() {
    return customProperties;
  }

  public void setCustomProperty(String key, Object value) {
    customProperties.put(key, value);
  }

  public Object getCustomProperty(String key) {
    return customProperties.get(key);
  }
}
