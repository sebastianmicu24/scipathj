package com.scipath.scipathj.core.config;

/**
 * Constants for segmentation algorithms and analysis parameters.
 * This class contains all business logic constants related to image analysis,
 * separated from UI styling constants.
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SegmentationConstants {
    
    // Prevent instantiation
    private SegmentationConstants() {}
    
    // === VESSEL SEGMENTATION CONSTANTS ===
    
    /** Default threshold value for vessel detection */
    public static final int DEFAULT_VESSEL_THRESHOLD = 220;
    
    /** Minimum vessel area in pixels to be considered valid */
    public static final double DEFAULT_MIN_VESSEL_SIZE = 50.0;
    
    /** Maximum vessel area in pixels (no limit by default) */
    public static final double DEFAULT_MAX_VESSEL_SIZE = Double.MAX_VALUE;
    
    /** Gaussian blur sigma parameter to reduce noise in vessel detection */
    public static final double DEFAULT_VESSEL_GAUSSIAN_BLUR_SIGMA = 2.0;
    
    /** Default morphological operations for vessel cleanup */
    public static final int DEFAULT_VESSEL_EROSION_ITERATIONS = 1;
    public static final int DEFAULT_VESSEL_DILATION_ITERATIONS = 2;
    
    // === NUCLEAR SEGMENTATION CONSTANTS (StarDist) ===
    
    /** Default StarDist model for H&E stained nuclei */
    public static final String DEFAULT_STARDIST_MODEL = "Versatile (H&E nuclei)";
    
    /** Default probability threshold for nucleus detection */
    public static final float DEFAULT_NUCLEUS_PROB_THRESHOLD = 0.5f;
    
    /** Default non-maximum suppression threshold */
    public static final float DEFAULT_NUCLEUS_NMS_THRESHOLD = 0.4f;
    
    /** Default bottom percentile for input normalization */
    public static final float DEFAULT_NUCLEUS_PERCENTILE_LOW = 1.0f;
    
    /** Default top percentile for input normalization */
    public static final float DEFAULT_NUCLEUS_PERCENTILE_HIGH = 99.8f;
    
    /** Default number of tiles for processing large images */
    public static final int DEFAULT_NUCLEUS_N_TILES = 1;
    
    /** Default boundary exclusion distance in pixels */
    public static final int DEFAULT_NUCLEUS_EXCLUDE_BOUNDARY = 2;
    
    /** Minimum nucleus area in pixels to be considered valid */
    public static final double DEFAULT_MIN_NUCLEUS_SIZE = 10.0;
    
    /** Maximum nucleus area in pixels to be considered valid */
    public static final double DEFAULT_MAX_NUCLEUS_SIZE = 1000.0;
    
    /** Default ROI position setting for StarDist */
    public static final String DEFAULT_NUCLEUS_ROI_POSITION = "Automatic";
    
    /** Default output type for StarDist results */
    public static final String DEFAULT_NUCLEUS_OUTPUT_TYPE = "ROI Manager";
    
    // === SEGMENTATION WORKFLOW CONSTANTS ===
    
    /** Interval for progress updates during segmentation (milliseconds) */
    public static final int SEGMENTATION_PROGRESS_UPDATE_INTERVAL = 100;
    
    /** Default setting for showing segmentation progress dialogs */
    public static final boolean DEFAULT_SHOW_SEGMENTATION_PROGRESS = false;
    
    /** Default setting for verbose segmentation logging */
    public static final boolean DEFAULT_VERBOSE_SEGMENTATION = false;
    
    /** Default setting for showing CSBDeep progress */
    public static final boolean DEFAULT_SHOW_CSBDEEP_PROGRESS = false;
    
    /** Default setting for showing probability and distance maps */
    public static final boolean DEFAULT_SHOW_PROB_AND_DIST = false;
    
    /** Default setting for input normalization */
    public static final boolean DEFAULT_NORMALIZE_INPUT = true;
    
    // === IMAGE PROCESSING CONSTANTS ===
    
    /** Supported image file extensions for batch processing */
    public static final String[] SUPPORTED_IMAGE_EXTENSIONS = {
        ".jpg", ".jpeg", ".png", ".tif", ".tiff", ".bmp", ".gif",
        ".lsm", ".czi", ".nd2", ".oib", ".oif", ".vsi", ".ims",
        ".lif", ".scn", ".svs", ".ndpi"
    };
    
    /** Default timeout for image processing operations (milliseconds) */
    public static final long DEFAULT_PROCESSING_TIMEOUT = 300000; // 5 minutes
    
    /** Default memory threshold for triggering garbage collection (ratio) */
    public static final double DEFAULT_MEMORY_THRESHOLD = 0.8;
    
    /** Default delay between processing images in batch mode (milliseconds) */
    public static final int DEFAULT_BATCH_PROCESSING_DELAY = 200;
    
    // === VALIDATION CONSTANTS ===
    
    /** Minimum valid probability threshold */
    public static final float MIN_PROB_THRESHOLD = 0.0f;
    
    /** Maximum valid probability threshold */
    public static final float MAX_PROB_THRESHOLD = 1.0f;
    
    /** Minimum valid NMS threshold */
    public static final float MIN_NMS_THRESHOLD = 0.0f;
    
    /** Maximum valid NMS threshold */
    public static final float MAX_NMS_THRESHOLD = 1.0f;
    
    /** Minimum valid percentile value */
    public static final float MIN_PERCENTILE = 0.0f;
    
    /** Maximum valid percentile value */
    public static final float MAX_PERCENTILE = 100.0f;
    
    /** Minimum valid area size */
    public static final double MIN_AREA_SIZE = 0.0;
    
    /** Maximum reasonable area size for validation */
    public static final double MAX_REASONABLE_AREA_SIZE = 100000.0;
    
    // === PERFORMANCE CONSTANTS ===
    
    /** Default thread pool size for parallel processing */
    public static final int DEFAULT_THREAD_POOL_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
    
    /** Default queue capacity for batch processing */
    public static final int DEFAULT_PROCESSING_QUEUE_CAPACITY = 1000;
    
    /** Default chunk size for progress reporting */
    public static final int DEFAULT_PROGRESS_CHUNK_SIZE = 10;
}