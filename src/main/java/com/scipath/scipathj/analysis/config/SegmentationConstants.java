package com.scipath.scipathj.analysis.config;

/**
 * Constants for segmentation algorithms and analysis parameters.
 * This class contains all business logic constants related to image analysis,
 * organized by functional groups for better maintainability.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SegmentationConstants {

  // Prevent instantiation
  private SegmentationConstants() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  // ===================================================================
  // VESSEL SEGMENTATION CONSTANTS
  // ===================================================================

  /**
   * Vessel segmentation default parameters for threshold-based detection.
   */
  public static final class VesselSegmentation {
    /** Default threshold value for vessel detection (0-255) */
    public static final int DEFAULT_THRESHOLD = 220;

    /** Alias for DEFAULT_THRESHOLD for backward compatibility */
    public static final int DEFAULT_VESSEL_THRESHOLD = DEFAULT_THRESHOLD;

    /** Minimum vessel area in pixels to be considered valid */
    public static final double DEFAULT_MIN_SIZE = 50.0;

    /** Alias for DEFAULT_MIN_SIZE for backward compatibility */
    public static final int DEFAULT_MIN_VESSEL_SIZE = (int) DEFAULT_MIN_SIZE;

    /** Maximum vessel area in pixels (no limit by default) */
    public static final double DEFAULT_MAX_SIZE = Double.MAX_VALUE;

    /** Alias for DEFAULT_MAX_SIZE for backward compatibility */
    public static final int DEFAULT_MAX_VESSEL_SIZE = 10000;

    /** Gaussian blur sigma parameter to reduce noise in vessel detection */
    public static final double DEFAULT_GAUSSIAN_BLUR_SIGMA = 2.0;

    /** Default morphological operations for vessel cleanup */
    public static final int DEFAULT_EROSION_ITERATIONS = 1;

    public static final int DEFAULT_DILATION_ITERATIONS = 2;

    /** Whether to apply morphological closing by default */
    public static final boolean DEFAULT_APPLY_MORPHOLOGICAL_CLOSING = true;

    private VesselSegmentation() {
      throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
  }

  // ===================================================================
  // NUCLEAR SEGMENTATION CONSTANTS (StarDist)
  // ===================================================================

  /**
   * Nuclear segmentation default parameters for StarDist-based detection.
   */
  public static final class NuclearSegmentation {
    /** Default StarDist model for H&E stained nuclei */
    public static final String DEFAULT_MODEL = "Versatile (H&E nuclei)";

    /** Default probability threshold for nucleus detection (0.0-1.0) */
    public static final float DEFAULT_PROB_THRESHOLD = 0.5f;

    /** Default non-maximum suppression threshold (0.0-1.0) */
    public static final float DEFAULT_NMS_THRESHOLD = 0.4f;

    /** Default bottom percentile for input normalization (0.0-100.0) */
    public static final float DEFAULT_PERCENTILE_BOTTOM = 1.0f;

    /** Default top percentile for input normalization (0.0-100.0) */
    public static final float DEFAULT_PERCENTILE_TOP = 99.8f;

    /** Default number of tiles for processing large images */
    public static final int DEFAULT_N_TILES = 1;

    /** Default boundary exclusion distance in pixels */
    public static final int DEFAULT_EXCLUDE_BOUNDARY = 2;

    /** Minimum nucleus area in pixels to be considered valid */
    public static final double DEFAULT_MIN_SIZE = 10.0;

    /** Maximum nucleus area in pixels to be considered valid */
    public static final double DEFAULT_MAX_SIZE = 1000.0;

    /** Default ROI position setting for StarDist */
    public static final String DEFAULT_ROI_POSITION = "Automatic";

    /** Default output type for StarDist results */
    public static final String DEFAULT_OUTPUT_TYPE = "ROI Manager";

    /** Default setting for input normalization */
    public static final boolean DEFAULT_NORMALIZE_INPUT = true;

    /** Default setting for verbose output */
    public static final boolean DEFAULT_VERBOSE = false;

    // Additional constants needed by VesselSegmentation and other components
    public static final double DEFAULT_VESSEL_THRESHOLD = 128.0;
    public static final int DEFAULT_MIN_VESSEL_SIZE = (int) DEFAULT_MIN_SIZE;
    public static final int DEFAULT_MAX_VESSEL_SIZE = 10000;
    public static final String[] SUPPORTED_IMAGE_EXTENSIONS = {
      ".tif", ".tiff", ".jpg", ".jpeg", ".png", ".bmp"
    };
    public static final long DEFAULT_BATCH_PROCESSING_DELAY = 100; // milliseconds

    /** Default setting for showing CSBDeep progress */
    public static final boolean DEFAULT_SHOW_CSBDEEP_PROGRESS = false;

    /** Default setting for showing probability and distance maps */
    public static final boolean DEFAULT_SHOW_PROB_AND_DIST = false;

    private NuclearSegmentation() {
      throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
  }

  // ===================================================================
  // CYTOPLASM SEGMENTATION CONSTANTS
  // ===================================================================

  /**
   * Cytoplasm segmentation default parameters for various detection methods.
   */
  public static final class CytoplasmSegmentation {
    /** Default threshold for cytoplasm detection */
    public static final int DEFAULT_THRESHOLD = 180;

    /** Default minimum cytoplasm area in pixels */
    public static final double DEFAULT_MIN_SIZE = 100.0;

    /** Alias for DEFAULT_MIN_SIZE for backward compatibility */
    public static final int DEFAULT_MIN_CYTOPLASM_AREA = (int) DEFAULT_MIN_SIZE;

    /** Default maximum cytoplasm area in pixels */
    public static final double DEFAULT_MAX_SIZE = 10000.0;

    /** Alias for DEFAULT_MAX_SIZE for backward compatibility */
    public static final int DEFAULT_MAX_CYTOPLASM_AREA = (int) DEFAULT_MAX_SIZE;

    /** Default Gaussian blur sigma for noise reduction */
    public static final double DEFAULT_GAUSSIAN_BLUR_SIGMA = 1.5;

    /** Default morphological operations */
    public static final int DEFAULT_EROSION_ITERATIONS = 2;

    public static final int DEFAULT_DILATION_ITERATIONS = 3;

    /** Default morphological closing radius */
    public static final int DEFAULT_MORPH_CLOSING_RADIUS = 2;

    /** Default edge detection parameters */
    public static final double DEFAULT_EDGE_THRESHOLD = 0.1;

    public static final boolean DEFAULT_USE_EDGE_DETECTION = true;

    /** Default watershed parameters */
    public static final double DEFAULT_WATERSHED_TOLERANCE = 0.5;

    public static final boolean DEFAULT_USE_WATERSHED = true;

    /** Default region growing parameters */
    public static final double DEFAULT_REGION_GROWING_TOLERANCE = 10.0;

    public static final int DEFAULT_REGION_GROWING_MAX_ITERATIONS = 100;

    /** Default Voronoi expansion parameter */
    public static final double DEFAULT_VORONOI_EXPANSION = 5.0;

    /** Default setting for filling holes */
    public static final boolean DEFAULT_FILL_HOLES = true;

    /** Default setting for smoothing boundaries */
    public static final boolean DEFAULT_SMOOTH_BOUNDARIES = true;

    /** Default setting for verbose output */
    public static final boolean DEFAULT_VERBOSE = false;

    private CytoplasmSegmentation() {
      throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
  }

  // ===================================================================
  // VALIDATION RANGES
  // ===================================================================

  /**
   * Validation constants for parameter ranges and limits.
   */
  public static final class Validation {
    /** Probability threshold validation range */
    public static final float MIN_PROB_THRESHOLD = 0.0f;

    public static final float MAX_PROB_THRESHOLD = 1.0f;

    /** NMS threshold validation range */
    public static final float MIN_NMS_THRESHOLD = 0.0f;

    public static final float MAX_NMS_THRESHOLD = 1.0f;

    /** Percentile validation range */
    public static final float MIN_PERCENTILE = 0.0f;

    public static final float MAX_PERCENTILE = 100.0f;

    /** Area size validation range */
    public static final double MIN_AREA_SIZE = 0.0;

    public static final double MAX_REASONABLE_AREA_SIZE = 100000.0;

    /** Threshold validation range (for 8-bit images) */
    public static final int MIN_THRESHOLD = 0;

    public static final int MAX_THRESHOLD = 255;

    /** Gaussian blur sigma validation range */
    public static final double MIN_GAUSSIAN_SIGMA = 0.1;

    public static final double MAX_GAUSSIAN_SIGMA = 10.0;

    /** Morphological operations validation range */
    public static final int MIN_MORPHOLOGICAL_ITERATIONS = 0;

    public static final int MAX_MORPHOLOGICAL_ITERATIONS = 20;

    private Validation() {
      throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
  }

  // ===================================================================
  // IMAGE PROCESSING CONSTANTS
  // ===================================================================

  /**
   * General image processing and file handling constants.
   */
  public static final class ImageProcessing {
    /** Supported image file extensions for batch processing */
    private static final String[] SUPPORTED_EXTENSIONS_INTERNAL = {
      ".jpg", ".jpeg", ".png", ".tif", ".tiff", ".bmp", ".gif",
      ".lsm", ".czi", ".nd2", ".oib", ".oif", ".vsi", ".ims",
      ".lif", ".scn", ".svs", ".ndpi"
    };

    /** Supported image file extensions for batch processing */
    public static final String[] SUPPORTED_EXTENSIONS = SUPPORTED_EXTENSIONS_INTERNAL.clone();

    /** Alias for SUPPORTED_EXTENSIONS for backward compatibility */
    public static final String[] SUPPORTED_IMAGE_EXTENSIONS = SUPPORTED_EXTENSIONS_INTERNAL.clone();

    /** Default timeout for image processing operations (milliseconds) */
    public static final long DEFAULT_PROCESSING_TIMEOUT = 300000; // 5 minutes

    /** Default memory threshold for triggering garbage collection (ratio) */
    public static final double DEFAULT_MEMORY_THRESHOLD = 0.8;

    /** Default delay between processing images in batch mode (milliseconds) */
    public static final int DEFAULT_BATCH_PROCESSING_DELAY = 200;

    /** Interval for progress updates during segmentation (milliseconds) */
    public static final int PROGRESS_UPDATE_INTERVAL = 100;

    /** Default setting for showing segmentation progress dialogs */
    public static final boolean DEFAULT_SHOW_PROGRESS = false;

    private ImageProcessing() {
      throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
  }

  // ===================================================================
  // PERFORMANCE CONSTANTS
  // ===================================================================

  /**
   * Performance and threading configuration constants.
   */
  public static final class Performance {
    /** Default thread pool size for parallel processing */
    public static final int DEFAULT_THREAD_POOL_SIZE =
        Math.max(2, Runtime.getRuntime().availableProcessors() - 1);

    /** Default queue capacity for batch processing */
    public static final int DEFAULT_PROCESSING_QUEUE_CAPACITY = 1000;

    /** Default chunk size for progress reporting */
    public static final int DEFAULT_PROGRESS_CHUNK_SIZE = 10;

    /** Default maximum memory usage before forcing cleanup (bytes) */
    public static final long DEFAULT_MAX_MEMORY_USAGE =
        (long) (Runtime.getRuntime().maxMemory() * 0.8);

    /** Default timeout for thread operations (milliseconds) */
    public static final long DEFAULT_THREAD_TIMEOUT = 30000; // 30 seconds

    private Performance() {
      throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
  }

  // ===================================================================
  // ALGORITHM IDENTIFIERS
  // ===================================================================

  /**
   * String identifiers for different segmentation algorithms.
   */
  public static final class Algorithms {
    /** Vessel segmentation algorithm identifiers */
    public static final String VESSEL_THRESHOLD = "threshold";

    public static final String VESSEL_ADAPTIVE = "adaptive";
    public static final String VESSEL_EDGE_BASED = "edge_based";

    /** Nuclear segmentation algorithm identifiers */
    public static final String NUCLEAR_STARDIST = "stardist";

    public static final String NUCLEAR_WATERSHED = "watershed";
    public static final String NUCLEAR_THRESHOLD = "threshold";

    /** Cytoplasm segmentation algorithm identifiers */
    public static final String CYTOPLASM_REGION_GROWING = "region_growing";

    public static final String CYTOPLASM_WATERSHED = "watershed";
    public static final String CYTOPLASM_EDGE_DETECTION = "edge_detection";
    public static final String CYTOPLASM_COMBINED = "combined";

    private Algorithms() {
      throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
  }

  // ===================================================================
  // UTILITY METHODS
  // ===================================================================

  /**
   * Validates if a probability threshold is within acceptable range.
   *
   * @param threshold The threshold to validate
   * @return true if valid, false otherwise
   */
  public static boolean isValidProbabilityThreshold(float threshold) {
    return threshold >= Validation.MIN_PROB_THRESHOLD && threshold <= Validation.MAX_PROB_THRESHOLD;
  }

  /**
   * Validates if a percentile value is within acceptable range.
   *
   * @param percentile The percentile to validate
   * @return true if valid, false otherwise
   */
  public static boolean isValidPercentile(float percentile) {
    return percentile >= Validation.MIN_PERCENTILE && percentile <= Validation.MAX_PERCENTILE;
  }

  /**
   * Validates if an area size is within reasonable range.
   *
   * @param area The area to validate
   * @return true if valid, false otherwise
   */
  public static boolean isValidAreaSize(double area) {
    return area >= Validation.MIN_AREA_SIZE && area <= Validation.MAX_REASONABLE_AREA_SIZE;
  }

  /**
   * Validates if a threshold value is within acceptable range for 8-bit images.
   *
   * @param threshold The threshold to validate
   * @return true if valid, false otherwise
   */
  public static boolean isValidThreshold(int threshold) {
    return threshold >= Validation.MIN_THRESHOLD && threshold <= Validation.MAX_THRESHOLD;
  }

  /**
   * Validates if a Gaussian blur sigma is within acceptable range.
   *
   * @param sigma The sigma value to validate
   * @return true if valid, false otherwise
   */
  public static boolean isValidGaussianSigma(double sigma) {
    return sigma >= Validation.MIN_GAUSSIAN_SIGMA && sigma <= Validation.MAX_GAUSSIAN_SIGMA;
  }

  /**
   * Checks if a file extension is supported for image processing.
   *
   * @param extension The file extension to check (with or without leading dot)
   * @return true if supported, false otherwise
   */
  public static boolean isSupportedImageExtension(String extension) {
    if (extension == null || extension.trim().isEmpty()) {
      return false;
    }

    String normalizedExt = extension.toLowerCase().trim();
    if (!normalizedExt.startsWith(".")) {
      normalizedExt = "." + normalizedExt;
    }

    for (String supportedExt : ImageProcessing.SUPPORTED_EXTENSIONS) {
      if (supportedExt.equals(normalizedExt)) {
        return true;
      }
    }
    return false;
  }

  // ===================================================================
  // BACKWARD COMPATIBILITY ALIASES
  // ===================================================================

  /** Alias for VesselSegmentation.DEFAULT_VESSEL_THRESHOLD */
  public static final int DEFAULT_VESSEL_THRESHOLD = VesselSegmentation.DEFAULT_VESSEL_THRESHOLD;

  /** Alias for VesselSegmentation.DEFAULT_MIN_VESSEL_SIZE */
  public static final int DEFAULT_MIN_VESSEL_SIZE = VesselSegmentation.DEFAULT_MIN_VESSEL_SIZE;

  /** Alias for VesselSegmentation.DEFAULT_MAX_VESSEL_SIZE */
  public static final int DEFAULT_MAX_VESSEL_SIZE = VesselSegmentation.DEFAULT_MAX_VESSEL_SIZE;

  /** Alias for ImageProcessing.SUPPORTED_IMAGE_EXTENSIONS */
  @SuppressWarnings("MS_MUTABLE_ARRAY")
  public static final String[] SUPPORTED_IMAGE_EXTENSIONS =
      ImageProcessing.SUPPORTED_EXTENSIONS_INTERNAL.clone();

  /** Alias for ImageProcessing.DEFAULT_BATCH_PROCESSING_DELAY */
  public static final int DEFAULT_BATCH_PROCESSING_DELAY =
      ImageProcessing.DEFAULT_BATCH_PROCESSING_DELAY;
}
