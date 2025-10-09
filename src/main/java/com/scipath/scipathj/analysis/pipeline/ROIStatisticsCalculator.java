package com.scipath.scipathj.analysis.pipeline;

import com.scipath.scipathj.infrastructure.roi.UserROI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Statistical calculator for ROI features that computes mean values across different ROI types.
 * Excludes ignored ROIs from calculations to ensure data quality.
 *
 * Uses KNIME-compatible statistical methods for numerical reliability.
 */
public class ROIStatisticsCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ROIStatisticsCalculator.class);

    /**
     * Calculates mean feature values for each ROI type, excluding ignored ROIs.
     *
     * @param roiFeatures Map of ROI identifier to feature name-value pairs
     * @param allROIs List of all ROIs processed
     * @return Map of ROI type to feature name-value means
     */
    public Map<UserROI.ROIType, Map<String, Double>> calculateROITypeFeatureMeans(
            Map<String, Map<String, Object>> roiFeatures,
            List<UserROI> allROIs) {

        Map<UserROI.ROIType, Map<String, Double>> result = new HashMap<>();

        // Group ROIs by type, filtering out ignored ones
        Map<UserROI.ROIType, List<UserROI>> roisByType = allROIs.stream()
                .filter(roi -> !roi.isIgnored())
                .collect(Collectors.groupingBy(UserROI::getType));

        // For each ROI type, calculate mean feature values
        for (Map.Entry<UserROI.ROIType, List<UserROI>> entry : roisByType.entrySet()) {
            UserROI.ROIType roiType = entry.getKey();
            List<UserROI> validROIs = entry.getValue();

            Map<String, Double> typeMeanFeatures = calculateFeatureMeans(validROIs, roiFeatures);
            result.put(roiType, typeMeanFeatures);

            LOGGER.info("Calculated mean features for {} ROIs of type {}",
                    validROIs.size(), roiType.getDisplayName());
        }

        return result;
    }

    /**
     * Calculates mean values for each feature across a list of ROIs.
     *
     * @param rois List of ROIs to calculate means for
     * @param roiFeatures Map of ROI features
     * @return Map of feature name to mean value
     */
    private Map<String, Double> calculateFeatureMeans(List<UserROI> rois,
                                                      Map<String, Map<String, Object>> roiFeatures) {

        Map<String, Double> meanFeatures = new HashMap<>();

        if (rois.isEmpty()) {
            return meanFeatures;
        }

        // Collect all feature values by feature name
        Map<String, List<Double>> featureValues = new HashMap<>();

        for (UserROI roi : rois) {
            String roiKey = roi.getImageFileName() + "_" + roi.getName();
            Map<String, Object> roiFeatureValues = roiFeatures.get(roiKey);

            if (roiFeatureValues != null) {
                for (Map.Entry<String, Object> featureEntry : roiFeatureValues.entrySet()) {
                    String featureName = featureEntry.getKey();
                    Object value = featureEntry.getValue();

                    // Only process numeric values for mean calculation
                    if (value instanceof Number) {
                        featureValues.computeIfAbsent(featureName, k -> new ArrayList<>())
                                .add(((Number) value).doubleValue());
                    }
                }
            }
        }

        // Calculate mean for each feature
        for (Map.Entry<String, List<Double>> entry : featureValues.entrySet()) {
            String featureName = entry.getKey();
            List<Double> values = entry.getValue();

            if (!values.isEmpty()) {
                double sum = 0.0;
                for (double value : values) {
                    sum += value;
                }
                double mean = sum / values.size();

                // Round to 4 decimal places for readability
                meanFeatures.put(featureName, Math.round(mean * 10000.0) / 10000.0);
            }
        }

        return meanFeatures;
    }

    /**
     * Logs the calculated ROI type feature means in a structured format.
     *
     * @param roiTypeMeans Map of ROI type to feature means
     * @param imageFileName Name of the image processed
     */
    public void logROITypeFeatureMeans(Map<UserROI.ROIType, Map<String, Double>> roiTypeMeans,
                                       String imageFileName) {

        LOGGER.info("=== ROI FEATURE MEANS CALCULATION COMPLETED: {} ===", imageFileName);

        for (Map.Entry<UserROI.ROIType, Map<String, Double>> typeEntry : roiTypeMeans.entrySet()) {
            UserROI.ROIType roiType = typeEntry.getKey();
            Map<String, Double> featureMeans = typeEntry.getValue();

            LOGGER.info("--- {} ROI Type Feature Means ---", roiType.getDisplayName());

            if (featureMeans.isEmpty()) {
                LOGGER.info("  No features calculated for this ROI type");
            } else {
                for (Map.Entry<String, Double> featureEntry : featureMeans.entrySet()) {
                    LOGGER.info("  {}: {}", featureEntry.getKey(), featureEntry.getValue());
                }
            }
        }

        LOGGER.info("=== END ROI FEATURE MEANS: {} ===", imageFileName);
    }
}