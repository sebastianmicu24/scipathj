package com.scipath.scipathj.core.analysis;

import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataReorder class to prepare feature data for the XGBoost classifier.
 *
 * This class handles the mismatch between FeatureExtraction output format and
 * the SCHELI-trained classifier expectations.
 *
 * @author SciPathJ Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DataReorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataReorder.class);

    // Mapping from SCHELI feature names to FeatureExtraction feature names
    private static final Map<String, String> SCHELI_TO_FEATURE_EXTRACTOR_MAP = new HashMap<>();

    // Reverse mapping for faster lookups
    private static final Map<String, List<String>> FEATURE_EXTRACTOR_TO_SCHELI_MAP = new HashMap<>();

    static {
        // Initialize the mapping
        initializeFeatureMapping();
    }

    /**
     * Initialize the feature name mapping between SCHELI and FeatureExtraction formats.
     */
    private static void initializeFeatureMapping() {
        // Spatial features
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Vessel Distance", "vessel_distance");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Neighbor Count", "neighbor_count");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Closest Neighbor Distance", "closest_neighbor_distance");

        // Nucleus basic features
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Area", "area");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Perim.", "perim");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Width", "width");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Height", "height");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Major", "major");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Minor", "minor");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Angle", "angle");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Circ.", "circ");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_IntDen", "intden");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Feret", "feret");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_FeretAngle", "feretangle");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_MinFeret", "minferet");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_AR", "ar");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Round", "round");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Solidity", "solidity");

        // Nucleus intensity features
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Mean", "mean");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_StdDev", "stddev");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Mode", "mode");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Min", "min");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Max", "max");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Median", "median");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Skew", "skew");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Kurt", "kurt");

        // Nucleus H&E features
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Hema_Mean", "hema_mean");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Hema_StdDev", "hema_stddev");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Hema_Mode", "hema_mode");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Hema_Min", "hema_min");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Hema_Max", "hema_max");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Hema_Median", "hema_median");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Hema_Skew", "hema_skew");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Hema_Kurt", "hema_kurt");

        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Eosin_Mean", "eosin_mean");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Eosin_StdDev", "eosin_stddev");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Eosin_Mode", "eosin_mode");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Eosin_Min", "eosin_min");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Eosin_Max", "eosin_max");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Eosin_Median", "eosin_median");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Eosin_Skew", "eosin_skew");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Nucleus_Eosin_Kurt", "eosin_kurt");

        // Cytoplasm features - same pattern
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Area", "area");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Perim.", "perim");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Width", "width");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Height", "height");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Major", "major");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Minor", "minor");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Angle", "angle");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Circ.", "circ");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_IntDen", "intden");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Feret", "feret");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_FeretAngle", "feretangle");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_MinFeret", "minferet");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_AR", "ar");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Round", "round");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Solidity", "solidity");

        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Mean", "mean");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_StdDev", "stddev");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Mode", "mode");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Min", "min");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Max", "max");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Median", "median");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Skew", "skew");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Kurt", "kurt");

        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Hema_Mean", "hema_mean");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Hema_StdDev", "hema_stddev");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Hema_Mode", "hema_mode");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Hema_Min", "hema_min");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Hema_Max", "hema_max");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Hema_Median", "hema_median");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Hema_Skew", "hema_skew");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Hema_Kurt", "hema_kurt");

        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Eosin_Mean", "eosin_mean");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Eosin_StdDev", "eosin_stddev");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Eosin_Mode", "eosin_mode");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Eosin_Min", "eosin_min");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Eosin_Max", "eosin_max");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Eosin_Median", "eosin_median");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Eosin_Skew", "eosin_skew");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cytoplasm_Eosin_Kurt", "eosin_kurt");

        // Cell features - same pattern
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Area", "area");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Perim.", "perim");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Width", "width");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Height", "height");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Major", "major");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Minor", "minor");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Angle", "angle");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Circ.", "circ");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_IntDen", "intden");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Feret", "feret");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_FeretAngle", "feretangle");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_MinFeret", "minferet");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_AR", "ar");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Round", "round");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Solidity", "solidity");

        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Mean", "mean");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_StdDev", "stddev");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Mode", "mode");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Min", "min");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Max", "max");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Median", "median");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Skew", "skew");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Kurt", "kurt");

        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Hema_Mean", "hema_mean");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Hema_StdDev", "hema_stddev");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Hema_Mode", "hema_mode");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Hema_Min", "hema_min");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Hema_Max", "hema_max");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Hema_Median", "hema_median");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Hema_Skew", "hema_skew");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Hema_Kurt", "hema_kurt");

        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Eosin_Mean", "eosin_mean");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Eosin_StdDev", "eosin_stddev");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Eosin_Mode", "eosin_mode");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Eosin_Min", "eosin_min");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Eosin_Max", "eosin_max");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Eosin_Median", "eosin_median");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Eosin_Skew", "eosin_skew");
        SCHELI_TO_FEATURE_EXTRACTOR_MAP.put("Cell_Eosin_Kurt", "eosin_kurt");

        // Build reverse mapping for faster lookups
        for (Map.Entry<String, String> entry : SCHELI_TO_FEATURE_EXTRACTOR_MAP.entrySet()) {
            FEATURE_EXTRACTOR_TO_SCHELI_MAP.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }

        LOGGER.info("Initialized feature mapping with {} SCHELI to FeatureExtraction mappings", SCHELI_TO_FEATURE_EXTRACTOR_MAP.size());
    }

    /**
     * Prepare features for classification by reordering them to match SCHELI format.
     *
     * @param roiFeatures Features extracted by FeatureExtraction (key: roiKey, value: featureMap)
     * @param selectedFeatures List of features expected by the classifier in SCHELI format
     * @return Map of ROI keys to properly ordered feature arrays for classification
     */
    public static Map<String, float[]> prepareFeaturesForClassification(
            Map<String, Map<String, Object>> roiFeatures,
            List<String> selectedFeatures) {

        LOGGER.info("Preparing features for classification: {} ROIs, {} selected features",
            roiFeatures.size(), selectedFeatures.size());

        Map<String, float[]> preparedFeatures = new HashMap<>();

        for (Map.Entry<String, Map<String, Object>> roiEntry : roiFeatures.entrySet()) {
            String roiKey = roiEntry.getKey();
            Map<String, Object> features = roiEntry.getValue();

            float[] orderedFeatures = new float[selectedFeatures.size()];
            int validFeatures = 0;
            int nanCount = 0;

            for (int i = 0; i < selectedFeatures.size(); i++) {
                String scheliFeatureName = selectedFeatures.get(i);
                String featureExtractorName = SCHELI_TO_FEATURE_EXTRACTOR_MAP.get(scheliFeatureName);

                if (featureExtractorName != null) {
                    Object featureValue = features.get(featureExtractorName);
                    if (featureValue instanceof Number) {
                        float value = ((Number) featureValue).floatValue();
                        orderedFeatures[i] = value;
                        validFeatures++;
                        if (Float.isNaN(value)) {
                            nanCount++;
                        }
                    } else {
                        orderedFeatures[i] = Float.NaN;
                        nanCount++;
                    }
                } else {
                    // Try direct match if no mapping found
                    Object featureValue = features.get(scheliFeatureName.toLowerCase().replace(" ", "_"));
                    if (featureValue instanceof Number) {
                        float value = ((Number) featureValue).floatValue();
                        orderedFeatures[i] = value;
                        validFeatures++;
                        if (Float.isNaN(value)) {
                            nanCount++;
                        }
                    } else {
                        orderedFeatures[i] = Float.NaN;
                        nanCount++;
                    }
                }
            }

            if (validFeatures > 0) {
                preparedFeatures.put(roiKey, orderedFeatures);
                if (nanCount > 0) {
                    LOGGER.debug("ROI {}: {} valid features, {} NaN values", roiKey, validFeatures, nanCount);
                }
            } else {
                LOGGER.warn("ROI {}: No valid features found for classification", roiKey);
            }
        }

        LOGGER.info("Feature preparation completed: {} ROIs prepared for classification", preparedFeatures.size());
        return preparedFeatures;
    }

    /**
     * Get the mapping from SCHELI feature names to FeatureExtraction feature names.
     *
     * @return Unmodifiable map of SCHELI to FeatureExtraction feature name mappings
     */
    public static Map<String, String> getFeatureMapping() {
        return Collections.unmodifiableMap(SCHELI_TO_FEATURE_EXTRACTOR_MAP);
    }

    /**
     * Get the list of SCHELI feature names that correspond to a given FeatureExtraction feature.
     *
     * @param featureExtractorName The feature name from FeatureExtraction (e.g., "area")
     * @return List of SCHELI feature names that map to this feature (e.g., ["Nucleus_Area", "Cytoplasm_Area", "Cell_Area"])
     */
    public static List<String> getScheliFeatureNames(String featureExtractorName) {
        return SCHELI_TO_FEATURE_EXTRACTOR_MAP.entrySet().stream()
            .filter(entry -> entry.getValue().equals(featureExtractorName))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
}