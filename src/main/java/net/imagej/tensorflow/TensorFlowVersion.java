package net.imagej.tensorflow;

/**
 * Represents a TensorFlow version with Java 21 compatibility.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 */
public class TensorFlowVersion {

  private final String version;
  private final boolean loaded;
  private final String info;

  public TensorFlowVersion(String version, boolean loaded, String info) {
    this.version = version;
    this.loaded = loaded;
    this.info = info;
  }

  public TensorFlowVersion(String version) {
    this(version, true, "TensorFlow " + version + " loaded successfully");
  }

  public String getVersion() {
    return version;
  }

  public boolean isLoaded() {
    return loaded;
  }

  public String getInfo() {
    return info;
  }

  /**
   * Check if TensorFlow is configured to use GPU.
   * For now, we assume CPU-only usage for compatibility.
   * @return false (CPU-only)
   */
  public boolean usesGPU() {
    return false;
  }

  @Override
  public String toString() {
    return version;
  }
}
