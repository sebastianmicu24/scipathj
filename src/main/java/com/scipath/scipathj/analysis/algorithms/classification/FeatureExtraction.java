package com.scipath.scipathj.analysis.algorithms.classification;

import com.scipath.scipathj.analysis.config.FeatureExtractionSettings;
import com.scipath.scipathj.analysis.algorithms.segmentation.HEDeconvolution;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.PolygonRoi;
import ij.process.ImageStatistics;
import ij.measure.Measurements;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ultra-Fast Feature Extraction for SciPathJ H&E Analysis.
 * Optimized implementation using ImageJ's native C functions and spatial indexing.
 * 
 * Features exactly 47 measurements per ROI type (following SCHELI specification):
 * - Spatial: Vessel Distance, Closest Vessel, Neighbor Count, Closest Neighbor Distance, Closest Neighbor
 * - Basic: Area, X, Y, XM, YM, Perim., BX, BY, Width, Height  
 * - Shape: Major, Minor, Angle, Circ., Feret, FeretX, FeretY, FeretAngle, MinFeret, AR, Round, Solidity
 * - Intensity: IntDen, Mean, StdDev, Mode, Min, Max, Median, Skew, Kurt
 * - H&E: Hema_Mean, Hema_StdDev, Hema_Mode, Hema_Min, Hema_Max, Hema_Median, Hema_Skew, Hema_Kurt,
 *        Eosin_Mean, Eosin_StdDev, Eosin_Mode, Eosin_Min, Eosin_Max, Eosin_Median, Eosin_Skew, Eosin_Kurt
 *
 * @author Ultra-Fast SCHELI-Compatible Implementation for SciPathJ
 * @version 2.0.0 - Ultra-Fast Implementation
 * @since 1.0.0
 */
public class FeatureExtraction {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureExtraction.class);

    // Grid cell size for spatial indexing (optimized for typical ROI sizes)
    private static final int GRID_CELL_SIZE = 100;
    
    // Neighbor radius in pixels
    private static final double NEIGHBOR_RADIUS = 50.0;

    // ImageJ measurement flags optimized for performance
    private static final int BASIC_MEASUREMENTS = Measurements.AREA | Measurements.CENTROID | 
                                                 Measurements.CENTER_OF_MASS | Measurements.RECT |
                                                 Measurements.ELLIPSE;
    
    private static final int INTENSITY_MEASUREMENTS = Measurements.MEAN | Measurements.STD_DEV |
                                                     Measurements.MODE | Measurements.MIN_MAX |
                                                     Measurements.MEDIAN | Measurements.SKEWNESS |
                                                     Measurements.KURTOSIS;
    
    private static final int ALL_MEASUREMENTS = BASIC_MEASUREMENTS | INTENSITY_MEASUREMENTS;

    // Pre-computed feature names in SCHELI order
    private static final String[] FEATURE_NAMES = {
        "vessel_distance", "closest_vessel", "neighbor_count", "closest_neighbor_distance", "closest_neighbor",
        "area", "x", "y", "xm", "ym", "perim", "bx", "by", "width", "height",
        "major", "minor", "angle", "circ", "intden", "feret", "feretx", "ferety", "feretangle", "minferet", 
        "ar", "round", "solidity", "mean", "stddev", "mode", "min", "max", "median", "skew", "kurt",
        "hema_mean", "hema_stddev", "hema_mode", "hema_min", "hema_max", "hema_median", "hema_skew", "hema_kurt",
        "eosin_mean", "eosin_stddev", "eosin_mode", "eosin_min", "eosin_max", "eosin_median", "eosin_skew", "eosin_kurt"
    };

    // Core data
    private final ImagePlus originalImage;
    private final String imageFileName;
    private final List<UserROI> vesselROIs;
    private final List<UserROI> nucleusROIs;
    private final List<UserROI> cytoplasmROIs;
    private final List<UserROI> cellROIs;
    private final FeatureExtractionSettings settings;
    private final com.scipath.scipathj.infrastructure.config.MainSettings mainSettings;

    // H&E deconvolution (computed once)
    private ImagePlus hematoxylinImage;
    private ImagePlus eosinImage;
    private boolean hasHEImages = false;

    // Spatial indexing for ultra-fast distance calculations
    private SpatialGrid<SpatialROIData> vesselGrid;
    private SpatialGrid<SpatialROIData> nucleusGrid;
    private SpatialGrid<SpatialROIData> cellGrid;
    private List<SpatialROIData> vesselDataList;
    private List<SpatialROIData> nucleusDataList;
    private List<SpatialROIData> cellDataList;

    // High-performance caching for mixed types
    private final Map<String, Map<String, Object>> roiFeatureCacheObject = new ConcurrentHashMap<>();

    /**
     * Constructor for ultra-fast feature extraction.
     */
    public FeatureExtraction(
            ImagePlus originalImage,
            String imageFileName,
            List<UserROI> vesselROIs,
            List<UserROI> nucleusROIs,
            List<UserROI> cytoplasmROIs,
            List<UserROI> cellROIs,
            FeatureExtractionSettings settings,
            com.scipath.scipathj.infrastructure.config.MainSettings mainSettings) {

        this.originalImage = originalImage;
        this.imageFileName = imageFileName != null ? imageFileName : "unknown";
        this.vesselROIs = vesselROIs != null ? vesselROIs : Collections.emptyList();
        this.nucleusROIs = nucleusROIs != null ? nucleusROIs : Collections.emptyList();
        this.cytoplasmROIs = cytoplasmROIs != null ? cytoplasmROIs : Collections.emptyList();
        this.cellROIs = cellROIs != null ? cellROIs : Collections.emptyList();
        this.settings = settings != null ? settings : FeatureExtractionSettings.createDefault();
        this.mainSettings = mainSettings != null ? mainSettings : com.scipath.scipathj.infrastructure.config.MainSettings.createDefault();

        LOGGER.info("Ultra-Fast FeatureExtraction initialized for image: {} ({} vessels, {} nuclei, {} cytoplasm, {} cells)",
                this.imageFileName, this.vesselROIs.size(), this.nucleusROIs.size(), this.cytoplasmROIs.size(), this.cellROIs.size());

        // Initialize optimizations
        initializeOptimizations();
    }

    /**
     * Backward-compatible constructor that uses default MainSettings.
     */
    public FeatureExtraction(
            ImagePlus originalImage,
            String imageFileName,
            List<UserROI> vesselROIs,
            List<UserROI> nucleusROIs,
            List<UserROI> cytoplasmROIs,
            List<UserROI> cellROIs,
            FeatureExtractionSettings settings) {

        this(originalImage, imageFileName, vesselROIs, nucleusROIs, cytoplasmROIs, cellROIs, settings,
             com.scipath.scipathj.infrastructure.config.MainSettings.createDefault());
    }

    /**
     * Initialize all optimizations for maximum performance.
     */
    private void initializeOptimizations() {
        long startTime = System.currentTimeMillis();

        // 1. Initialize H&E deconvolution (once for entire image)
        initializeHEDeconvolution();

        // 2. Build spatial indexes for ultra-fast distance calculations
        buildSpatialIndexes();

        long endTime = System.currentTimeMillis();
        LOGGER.info("Ultra-Fast optimizations initialized in {} ms", (endTime - startTime));
    }

    /**
     * Initialize H&E deconvolution once for the entire image.
     */
    private void initializeHEDeconvolution() {
        try {
            LOGGER.info("Initializing H&E deconvolution for ultra-fast feature extraction");
            
            HEDeconvolution heDeconvolution = new HEDeconvolution(originalImage, true);
            
            if (heDeconvolution.isHAndEImage()) {
                heDeconvolution.performDeconvolution();
                
                this.hematoxylinImage = heDeconvolution.getHematoxylinImage();
                this.eosinImage = heDeconvolution.getEosinImage();
                this.hasHEImages = (hematoxylinImage != null && eosinImage != null);
                
                LOGGER.info("H&E deconvolution completed successfully");
            } else {
                LOGGER.info("Image is not H&E stained, using grayscale fallback");
                this.hasHEImages = false;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize H&E deconvolution: {}", e.getMessage());
            this.hasHEImages = false;
        }
    }

    /**
     * Build spatial indexes for ultra-fast distance calculations.
     */
    private void buildSpatialIndexes() {
        // Initialize spatial grids
        vesselGrid = new SpatialGrid<>();
        nucleusGrid = new SpatialGrid<>();
        cellGrid = new SpatialGrid<>();
        vesselDataList = new ArrayList<>();
        nucleusDataList = new ArrayList<>();
        cellDataList = new ArrayList<>();

        // Process vessels
        for (UserROI roi : vesselROIs) {
            SpatialROIData data = new SpatialROIData(roi.getName(), roi.getCenterX(), roi.getCenterY(),
                                                    roi.getArea(), "vessel");
            vesselDataList.add(data);
            vesselGrid.add(data);
        }

        // Process nuclei
        for (UserROI roi : nucleusROIs) {
            SpatialROIData data = new SpatialROIData(roi.getName(), roi.getCenterX(), roi.getCenterY(),
                                                    roi.getArea(), "nucleus");
            nucleusDataList.add(data);
            nucleusGrid.add(data);
        }

        // Process cells
        for (UserROI roi : cellROIs) {
            SpatialROIData data = new SpatialROIData(roi.getName(), roi.getCenterX(), roi.getCenterY(),
                                                    roi.getArea(), "cell");
            cellDataList.add(data);
            cellGrid.add(data);
        }

        LOGGER.debug("Built spatial indexes: {} vessels, {} nuclei, {} cells",
                     vesselDataList.size(), nucleusDataList.size(), cellDataList.size());
    }

    /**
     * Extract features with maximum performance using ImageJ's native functions.
     */
    public Map<String, Map<String, Object>> extractFeatures() {
        LOGGER.info("Starting ultra-fast feature extraction for image: {}", imageFileName);
        
        long startTime = System.currentTimeMillis();
        Map<String, Map<String, Object>> allFeatures = new HashMap<>();

        try {
            // Process each ROI type using optimized batch processing
            processROITypeOptimized(nucleusROIs, "nucleus", allFeatures);
            processROITypeOptimized(cytoplasmROIs, "cytoplasm", allFeatures);
            processROITypeOptimized(cellROIs, "cell", allFeatures);
            processROITypeOptimized(vesselROIs, "vessel", allFeatures);

            long endTime = System.currentTimeMillis();
            LOGGER.info("Ultra-fast feature extraction completed in {} ms for {} ROIs",
                       (endTime - startTime), allFeatures.size());

            return allFeatures;

        } catch (Exception e) {
            LOGGER.error("Ultra-fast feature extraction failed: {}", e.getMessage(), e);
            return allFeatures;
        }
    }

    /**
     * Process ROI type with maximum optimization.
     */
    private void processROITypeOptimized(List<UserROI> rois, String roiType,
                                       Map<String, Map<String, Object>> allFeatures) {
        if (rois.isEmpty()) {
            return;
        }

        LOGGER.debug("Processing {} {} ROIs with ultra-fast optimization", rois.size(), roiType);

        for (UserROI roi : rois) {
            try {
                String cacheKey = imageFileName + "_" + roi.getName();
                
                // Check cache first
                Map<String, Object> features = roiFeatureCacheObject.get(cacheKey);
                if (features == null) {
                    features = extractOptimizedFeatures(roi, roiType);
                    roiFeatureCacheObject.put(cacheKey, features);
                }
                
                if (!features.isEmpty()) {
                    allFeatures.put(cacheKey, features);
                }
            } catch (Exception e) {
                LOGGER.debug("Failed to extract features for {} ROI {}: {}", roiType, roi.getName(), e.getMessage());
            }
        }
    }

    /**
     * Extract optimized features using ImageJ's fastest native functions.
     */
    private Map<String, Object> extractOptimizedFeatures(UserROI roi, String roiType) {
        Map<String, Object> features = new LinkedHashMap<>();

        try {
            Roi imageJRoi = roi.getImageJRoi();
            if (imageJRoi == null) {
                imageJRoi = new Roi(roi.getX(), roi.getY(), roi.getWidth(), roi.getHeight());
            }

            // Set ROI on original image for measurements
            originalImage.setRoi(imageJRoi);

            // 1. Get comprehensive statistics in one call (ultra-fast)
            ImageStatistics stats = originalImage.getStatistics(ALL_MEASUREMENTS);

            // 2. Spatial features (using pre-computed spatial indexes)
            addSpatialFeaturesOptimized(roi, roiType, features);

            // 3. Basic geometric features (direct from ImageJ)
            addBasicFeaturesOptimized(roi, stats, features);

            // 4. Shape features (using ImageJ's optimized functions)
            addShapeFeaturesOptimized(roi, imageJRoi, stats, features);

            // 5. Intensity features (from pre-computed statistics)
            addIntensityFeaturesOptimized(stats, features);

            // 6. H&E channel features (if available)
            addHEFeaturesOptimized(roi, imageJRoi, features);

            // 7. Add ignore status
            features.put("ignore", roi.isIgnored());

        } catch (Exception e) {
            LOGGER.debug("Error in optimized feature extraction for {} {}: {}", roiType, roi.getName(), e.getMessage());
        }

        return features;
    }

    /**
     * Add spatial features using ultra-fast spatial indexing.
     */
    private void addSpatialFeaturesOptimized(UserROI roi, String roiType, Map<String, Object> features) {
        try {
            // Vessel distance calculation using spatial grid
            SpatialResult vesselResult = calculateVesselDistanceOptimized(roi);
            features.put("vessel_distance", vesselResult.distance);
            // Store the actual vessel name as string (SCHELI compatible)
            features.put("closest_vessel", vesselResult.name != null ? vesselResult.name : "N/A");

            // Neighbor analysis using spatial grid
            SpatialResult neighborResult = calculateNeighborDataOptimized(roi, roiType);
            features.put("neighbor_count", neighborResult.distance); // Using distance field for count
            features.put("closest_neighbor_distance", neighborResult.extraData); // Using extraData for distance
            // Store the actual neighbor name as string (SCHELI compatible)
            features.put("closest_neighbor", neighborResult.name != null ? neighborResult.name : "N/A");

        } catch (Exception e) {
            LOGGER.debug("Error in spatial features: {}", e.getMessage());
            // Add default values
            features.put("vessel_distance", -1.0);
            features.put("closest_vessel", "N/A");
            features.put("neighbor_count", 0.0);
            features.put("closest_neighbor_distance", -1.0);
            features.put("closest_neighbor", "N/A");
        }
    }

    /**
     * Add basic features using direct ImageJ data.
     * Keep features in pixel units for classification compatibility.
     */
    private void addBasicFeaturesOptimized(UserROI roi, ImageStatistics stats, Map<String, Object> features) {
        features.put("area", stats.area);
        features.put("x", stats.xCentroid);
        features.put("y", stats.yCentroid);
        features.put("xm", stats.xCenterOfMass);
        features.put("ym", stats.yCenterOfMass);
        features.put("bx", (double) stats.roiX);
        features.put("by", (double) stats.roiY);
        features.put("width", (double) stats.roiWidth);
        features.put("height", (double) stats.roiHeight);

        // Perimeter using ImageJ's optimized calculation
        Roi imageJRoi = roi.getImageJRoi();
        if (imageJRoi != null) {
            features.put("perim", imageJRoi.getLength());
        } else {
            double perimeter = 2.0 * (roi.getWidth() + roi.getHeight());
            features.put("perim", perimeter);
        }
    }

    /**
     * Add shape features using ImageJ's native optimized functions.
     */
    private void addShapeFeaturesOptimized(UserROI roi, Roi imageJRoi, ImageStatistics stats, Map<String, Object> features) {
        try {
            // Ellipse parameters from ImageJ
            features.put("major", stats.major);
            features.put("minor", stats.minor);
            features.put("angle", stats.angle);

            // Circularity using pre-computed values
            double perimeter = imageJRoi.getLength();
            features.put("circ", (4.0 * Math.PI * stats.area) / (perimeter * perimeter));

            // Feret measurements using ImageJ's optimized function
            double[] feretValues = imageJRoi.getFeretValues();
            features.put("feret", feretValues[0]);
            features.put("feretx", feretValues[3]);
            features.put("ferety", feretValues[4]);
            features.put("feretangle", feretValues[1]);
            features.put("minferet", feretValues[2]);

            // Aspect ratio and roundness
            features.put("ar", stats.major / stats.minor);
            features.put("round", stats.minor / stats.major);

            // Solidity using optimized convex hull calculation
            features.put("solidity", calculateSolidityOptimized(imageJRoi, stats.area));

        } catch (Exception e) {
            LOGGER.debug("Error in shape features: {}", e.getMessage());
            // Add fallback values
            features.put("major", (double) Math.max(roi.getWidth(), roi.getHeight()));
            features.put("minor", (double) Math.min(roi.getWidth(), roi.getHeight()));
            features.put("angle", 0.0);
            features.put("circ", 1.0);
            features.put("feret", (double) Math.max(roi.getWidth(), roi.getHeight()));
            features.put("feretx", (double) roi.getCenterX());
            features.put("ferety", (double) roi.getCenterY());
            features.put("feretangle", 0.0);
            features.put("minferet", (double) Math.min(roi.getWidth(), roi.getHeight()));
            features.put("ar", (double) roi.getWidth() / roi.getHeight());
            features.put("round", (double) Math.min(roi.getWidth(), roi.getHeight()) / Math.max(roi.getWidth(), roi.getHeight()));
            features.put("solidity", 1.0);
        }
    }

    /**
     * Add intensity features from pre-computed statistics (ultra-fast).
     */
    private void addIntensityFeaturesOptimized(ImageStatistics stats, Map<String, Object> features) {
        features.put("intden", stats.area * stats.mean);
        features.put("mean", stats.mean);
        features.put("stddev", stats.stdDev);
        features.put("mode", (double) stats.mode);
        features.put("min", stats.min);
        features.put("max", stats.max);
        features.put("median", stats.median);
        features.put("skew", stats.skewness);
        features.put("kurt", stats.kurtosis);
    }

    /**
     * Add H&E features using pre-computed deconvolved images.
     */
    private void addHEFeaturesOptimized(UserROI roi, Roi imageJRoi, Map<String, Object> features) {
        if (!hasHEImages) {
            // Add zero values for non-H&E images
            addZeroHEFeatures(features);
            return;
        }

        try {
            // Hematoxylin features
            hematoxylinImage.setRoi(imageJRoi);
            ImageStatistics hemaStats = hematoxylinImage.getStatistics(INTENSITY_MEASUREMENTS);
            addChannelFeatures(hemaStats, "hema", features);

            // Eosin features  
            eosinImage.setRoi(imageJRoi);
            ImageStatistics eosinStats = eosinImage.getStatistics(INTENSITY_MEASUREMENTS);
            addChannelFeatures(eosinStats, "eosin", features);

        } catch (Exception e) {
            LOGGER.debug("Error in H&E features: {}", e.getMessage());
            addZeroHEFeatures(features);
        }
    }

    /**
     * Add channel-specific features using optimized statistics.
     */
    private void addChannelFeatures(ImageStatistics stats, String prefix, Map<String, Object> features) {
        features.put(prefix + "_mean", stats.mean);
        features.put(prefix + "_stddev", stats.stdDev);
        features.put(prefix + "_mode", (double) stats.mode);
        features.put(prefix + "_min", stats.min);
        features.put(prefix + "_max", stats.max);
        features.put(prefix + "_median", stats.median);
        features.put(prefix + "_skew", stats.skewness);
        features.put(prefix + "_kurt", stats.kurtosis);
    }

    /**
     * Add zero H&E features for non-H&E images.
     */
    private void addZeroHEFeatures(Map<String, Object> features) {
        String[] prefixes = {"hema", "eosin"};
        String[] suffixes = {"_mean", "_stddev", "_mode", "_min", "_max", "_median", "_skew", "_kurt"};
        
        for (String prefix : prefixes) {
            for (String suffix : suffixes) {
                features.put(prefix + suffix, 0.0);
            }
        }
    }

    /**
     * Calculate vessel distance using ultra-fast spatial indexing.
     */
    private SpatialResult calculateVesselDistanceOptimized(UserROI roi) {
        if (vesselDataList.isEmpty()) {
            return new SpatialResult(-1.0, null, -1.0);
        }

        SpatialROIData queryPoint = new SpatialROIData("query", roi.getCenterX(), roi.getCenterY(),
                                                      roi.getArea(), "query");

        // Use spatial grid for fast nearest neighbor search
        List<SpatialROIData> nearbyVessels = vesselGrid.getNearby(queryPoint, 2);
        
        if (nearbyVessels.isEmpty()) {
            nearbyVessels = vesselDataList; // Fallback to all vessels
        }

        double minDistance = Double.MAX_VALUE;
        String closestVesselName = null;
        
        for (SpatialROIData vessel : nearbyVessels) {
            if (!vessel.name.equals(roi.getName())) {
                double dx = vessel.x - queryPoint.x;
                double dy = vessel.y - queryPoint.y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestVesselName = vessel.name;
                }
            }
        }

        return new SpatialResult(
            minDistance == Double.MAX_VALUE ? -1.0 : minDistance,
            closestVesselName,
            -1.0
        );
    }

    /**
     * Calculate neighbor data using ultra-fast spatial indexing.
     * Properly excludes the same ROI by comparing ID portions and returns just the ID of closest neighbor.
     */
    private SpatialResult calculateNeighborDataOptimized(UserROI roi, String roiType) {
        List<SpatialROIData> relevantData;
        SpatialGrid<SpatialROIData> relevantGrid;

        // Determine which spatial data to use based on ROI type
        switch (roiType) {
            case "nucleus":
            case "cytoplasm":
                relevantData = nucleusDataList;
                relevantGrid = nucleusGrid;
                break;
            case "cell":
                relevantData = cellDataList;
                relevantGrid = cellGrid;
                break;
            case "vessel":
            default:
                relevantData = vesselDataList;
                relevantGrid = vesselGrid;
                break;
        }

        if (relevantData.isEmpty()) {
            return new SpatialResult(0.0, null, -1.0);
        }

        SpatialROIData queryPoint = new SpatialROIData("query", roi.getCenterX(), roi.getCenterY(),
                                                      roi.getArea(), roiType);
        
        int gridSearchRadius = (int)(NEIGHBOR_RADIUS / GRID_CELL_SIZE) + 1;
        List<SpatialROIData> nearbyROIs = relevantGrid.getNearby(queryPoint, gridSearchRadius);

        int neighborCount = 0;
        double minDistance = Double.MAX_VALUE;
        String closestNeighborId = null;
        
        // Extract the ID of the current ROI for comparison
        String currentRoiId = extractROIId(roi.getName());

        for (SpatialROIData other : nearbyROIs) {
            String otherRoiId = extractROIId(other.name);
            
            // Exclude the same ROI by comparing IDs, not full names
            if (!otherRoiId.equals(currentRoiId)) {
                double dx = other.x - queryPoint.x;
                double dy = other.y - queryPoint.y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                
                if (distance <= NEIGHBOR_RADIUS) {
                    neighborCount++;
                }
                if (distance < minDistance) {
                    minDistance = distance;
                    closestNeighborId = otherRoiId; // Store just the ID, not the full name
                }
            }
        }

        return new SpatialResult(
            (double) neighborCount,
            closestNeighborId,
            minDistance == Double.MAX_VALUE ? -1.0 : minDistance
        );
    }

    /**
     * Calculate solidity using optimized convex hull.
     */
    private double calculateSolidityOptimized(Roi roi, double area) {
        try {
            java.awt.Polygon convexHull = roi.getConvexHull();
            if (convexHull != null) {
                float[] xpoints = new float[convexHull.npoints];
                float[] ypoints = new float[convexHull.npoints];
                
                for (int i = 0; i < convexHull.npoints; i++) {
                    xpoints[i] = convexHull.xpoints[i];
                    ypoints[i] = convexHull.ypoints[i];
                }
                
                PolygonRoi convexRoi = new PolygonRoi(xpoints, ypoints, convexHull.npoints, Roi.POLYGON);
                originalImage.setRoi(convexRoi);
                double convexArea = originalImage.getStatistics(Measurements.AREA).area;
                
                return convexArea > 0 ? area / convexArea : 1.0;
            }
        } catch (Exception e) {
            LOGGER.debug("Could not calculate convex hull: {}", e.getMessage());
        }
        return 1.0;
    }

    /**
     * Result class for spatial calculations that includes names.
     */
    private static class SpatialResult {
        final double distance;
        final String name;
        final double extraData;

        SpatialResult(double distance, String name, double extraData) {
            this.distance = distance;
            this.name = name;
            this.extraData = extraData;
        }
    }

    /**
     * Spatial ROI data for ultra-fast distance calculations.
     */
    private static class SpatialROIData {
        final String name;
        final double x, y;
        final double area;
        final String type;
        final int gridX, gridY;

        SpatialROIData(String name, double x, double y, double area, String type) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.area = area;
            this.type = type;
            this.gridX = (int)(x / GRID_CELL_SIZE);
            this.gridY = (int)(y / GRID_CELL_SIZE);
        }
    }

    /**
     * Ultra-fast spatial grid for nearest neighbor searches.
     */
    private static class SpatialGrid<T extends SpatialROIData> {
        private final Map<String, List<T>> grid = new HashMap<>();

        void add(T item) {
            String key = item.gridX + "," + item.gridY;
            grid.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        List<T> getNearby(T item, int radius) {
            List<T> result = new ArrayList<>();
            
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    String key = (item.gridX + dx) + "," + (item.gridY + dy);
                    List<T> cellItems = grid.get(key);
                    if (cellItems != null) {
                        result.addAll(cellItems);
                    }
                }
            }
            
            return result;
        }
    }

    /**
     * Get the H&E deconvolution images.
     */
    public ImagePlus getHematoxylinImage() {
        return hematoxylinImage;
    }

    public ImagePlus getEosinImage() {
        return eosinImage;
    }

    /**
     * Check if H&E processing is available.
     */
    public boolean isHEAvailable() {
        return hasHEImages;
    }

    /**
     * Get total number of features per ROI (SCHELI compatibility).
     */
    public static int getFeaturesPerROI() {
        return FEATURE_NAMES.length;
    }

    /**
     * Get feature names in SCHELI order.
     */
    public static String[] getFeatureNames() {
        return FEATURE_NAMES.clone();
    }

    /**
     * Get the actual closest vessel name for a given ROI (as string).
     */
    public String getClosestVesselName(UserROI roi) {
        try {
            SpatialResult result = calculateVesselDistanceOptimized(roi);
            return result.name;
        } catch (Exception e) {
            LOGGER.debug("Error getting closest vessel name: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get the actual closest neighbor name for a given ROI and type (as string).
     */
    public String getClosestNeighborName(UserROI roi, String roiType) {
        try {
            SpatialResult result = calculateNeighborDataOptimized(roi, roiType);
            return result.name;
        } catch (Exception e) {
            LOGGER.debug("Error getting closest neighbor name: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get detailed spatial information for a ROI.
     */
    public Map<String, Object> getDetailedSpatialInfo(UserROI roi, String roiType) {
        Map<String, Object> info = new HashMap<>();
        
        try {
            // Vessel information
            SpatialResult vesselResult = calculateVesselDistanceOptimized(roi);
            info.put("vessel_distance", vesselResult.distance);
            info.put("closest_vessel_name", vesselResult.name);
            
            // Neighbor information
            SpatialResult neighborResult = calculateNeighborDataOptimized(roi, roiType);
            info.put("neighbor_count", (int) neighborResult.distance);
            info.put("closest_neighbor_distance", neighborResult.extraData);
            info.put("closest_neighbor_name", neighborResult.name);
            
        } catch (Exception e) {
            LOGGER.debug("Error getting detailed spatial info: {}", e.getMessage());
        }
        
        return info;
    }

    /**
     * Extract ROI ID from ROI name (the part after the last underscore).
     */
    private String extractROIId(String roiName) {
        if (roiName == null || roiName.isEmpty()) {
            return "unknown";
        }
        
        int lastUnderscoreIndex = roiName.lastIndexOf('_');
        if (lastUnderscoreIndex > 0 && lastUnderscoreIndex < roiName.length() - 1) {
            return roiName.substring(lastUnderscoreIndex + 1);
        }
        
        // If no underscore found, return the full name
        return roiName;
    }
}
