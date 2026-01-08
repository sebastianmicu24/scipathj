package com.scipath.scipathj.analysis.algorithms.classification;

import com.scipath.scipathj.infrastructure.roi.CellROI;
import com.scipath.scipathj.infrastructure.roi.CytoplasmROI;
import com.scipath.scipathj.infrastructure.roi.NucleusROI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates features from Cell, Nucleus, and Cytoplasm ROIs into a single feature vector.
 * This class implements the "Aggregated Approach" for handling polynucleated cells,
 * where nuclear features are summarized (count, mean area, etc.) rather than listed individually.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class CellFeatureAggregator {

  // Feature names for the aggregated vector
  private static final String[] FEATURE_NAMES = {
      // Cell Features
      "cell_angle", "cell_ar", "cell_area", "cell_bx", "cell_by", "cell_circ",
      "cell_eosin_kurt", "cell_eosin_max", "cell_eosin_mean", "cell_eosin_median", "cell_eosin_min", "cell_eosin_mode", "cell_eosin_skew", "cell_eosin_stddev",
      "cell_feret", "cell_feretangle", "cell_feretx", "cell_ferety", "cell_height",
      "cell_hema_kurt", "cell_hema_max", "cell_hema_mean", "cell_hema_median", "cell_hema_min", "cell_hema_mode", "cell_hema_skew", "cell_hema_stddev",
      "cell_intden", "cell_kurt", "cell_major", "cell_max", "cell_mean", "cell_median", "cell_min", "cell_minferet", "cell_minor", "cell_mode",
      "cell_perim", "cell_round", "cell_skew", "cell_solidity", "cell_stddev", "cell_width", "cell_x", "cell_xm", "cell_y", "cell_ym",
      
      // Nucleus Features (Aggregated - Mean values)
      "nucleus_count", "total_nucleus_area",
      "mean_nucleus_angle", "mean_nucleus_ar", "mean_nucleus_area", "mean_nucleus_bx", "mean_nucleus_by", "mean_nucleus_circ",
      "mean_nucleus_eosin_kurt", "mean_nucleus_eosin_max", "mean_nucleus_eosin_mean", "mean_nucleus_eosin_median", "mean_nucleus_eosin_min", "mean_nucleus_eosin_mode", "mean_nucleus_eosin_skew", "mean_nucleus_eosin_stddev",
      "mean_nucleus_feret", "mean_nucleus_feretangle", "mean_nucleus_feretx", "mean_nucleus_ferety", "mean_nucleus_height",
      "mean_nucleus_hema_kurt", "mean_nucleus_hema_max", "mean_nucleus_hema_mean", "mean_nucleus_hema_median", "mean_nucleus_hema_min", "mean_nucleus_hema_mode", "mean_nucleus_hema_skew", "mean_nucleus_hema_stddev",
      "mean_nucleus_intden", "mean_nucleus_kurt", "mean_nucleus_major", "mean_nucleus_max", "mean_nucleus_mean", "mean_nucleus_median", "mean_nucleus_min", "mean_nucleus_minferet", "mean_nucleus_minor", "mean_nucleus_mode",
      "mean_nucleus_perim", "mean_nucleus_round", "mean_nucleus_skew", "mean_nucleus_solidity", "mean_nucleus_stddev", "mean_nucleus_width", "mean_nucleus_x", "mean_nucleus_xm", "mean_nucleus_y", "mean_nucleus_ym",
      
      // Cytoplasm Features
      "cytoplasm_angle", "cytoplasm_ar", "cytoplasm_area", "cytoplasm_bx", "cytoplasm_by", "cytoplasm_circ",
      "cytoplasm_eosin_kurt", "cytoplasm_eosin_max", "cytoplasm_eosin_mean", "cytoplasm_eosin_median", "cytoplasm_eosin_min", "cytoplasm_eosin_mode", "cytoplasm_eosin_skew", "cytoplasm_eosin_stddev",
      "cytoplasm_feret", "cytoplasm_feretangle", "cytoplasm_feretx", "cytoplasm_ferety", "cytoplasm_height",
      "cytoplasm_hema_kurt", "cytoplasm_hema_max", "cytoplasm_hema_mean", "cytoplasm_hema_median", "cytoplasm_hema_min", "cytoplasm_hema_mode", "cytoplasm_hema_skew", "cytoplasm_hema_stddev",
      "cytoplasm_intden", "cytoplasm_kurt", "cytoplasm_major", "cytoplasm_max", "cytoplasm_mean", "cytoplasm_median", "cytoplasm_min", "cytoplasm_minferet", "cytoplasm_minor", "cytoplasm_mode",
      "cytoplasm_perim", "cytoplasm_round", "cytoplasm_skew", "cytoplasm_solidity", "cytoplasm_stddev", "cytoplasm_width", "cytoplasm_x", "cytoplasm_xm", "cytoplasm_y", "cytoplasm_ym",
      
      // Ratios and Interactions
      "nucleus_cytoplasm_ratio", "nucleus_cell_ratio", "cytoplasm_cell_ratio",
      
      // Spatial Features
      "closest_neighbour_distance", "neighbour_density", "closest_vessel_distance", "neighbor_count"
  };

  /**
   * Aggregates features for a single cell.
   *
   * @param cell the cell ROI to process
   * @param extractedFeatures the map of extracted features from FeatureExtraction
   * @param imageFileName the name of the image file (used for cache keys)
   * @return a map of feature names to values
   */
  public Map<String, Double> aggregateFeatures(
      CellROI cell,
      Map<String, Map<String, Object>> extractedFeatures,
      String imageFileName) {

    Map<String, Double> aggregated = new LinkedHashMap<>();

    // 1. Cell Features
    String cellKey = imageFileName + "_" + cell.getName();
    Map<String, Object> cellFeats = extractedFeatures.getOrDefault(cellKey, Map.of());

    // Add all cell features
    addFeatures(aggregated, cellFeats, "cell");

    // 2. Nucleus Features (Aggregated)
    // Currently CellROI only links one nucleus, but we structure this for future N-nuclei support
    NucleusROI nucleus = cell.getAssociatedNucleus();
    List<NucleusROI> nuclei = new ArrayList<>();
    if (nucleus != null) {
      nuclei.add(nucleus);
    }

    double totalNucleusArea = 0;
    // Aggregate nucleus features
    Map<String, Double> nucleusSums = new LinkedHashMap<>();
    int nucleusCount = nuclei.size();

    for (NucleusROI n : nuclei) {
      String nucKey = imageFileName + "_" + n.getName();
      Map<String, Object> nucFeats = extractedFeatures.getOrDefault(nucKey, Map.of());
      
      for (String key : nucFeats.keySet()) {
          double val = getDouble(nucFeats, key);
          nucleusSums.put(key, nucleusSums.getOrDefault(key, 0.0) + val);
      }
    }

    aggregated.put("nucleus_count", (double) nucleusCount);
    aggregated.put("total_nucleus_area", nucleusSums.getOrDefault("area", 0.0));
    
    // Add mean values for all nucleus features
    String[] featureKeys = {
        "angle", "ar", "area", "bx", "by", "circ",
        "eosin_kurt", "eosin_max", "eosin_mean", "eosin_median", "eosin_min", "eosin_mode", "eosin_skew", "eosin_stddev",
        "feret", "feretangle", "feretx", "ferety", "height",
        "hema_kurt", "hema_max", "hema_mean", "hema_median", "hema_min", "hema_mode", "hema_skew", "hema_stddev",
        "intden", "kurt", "major", "max", "mean", "median", "min", "minferet", "minor", "mode",
        "perim", "round", "skew", "solidity", "stddev", "width", "x", "xm", "y", "ym"
    };

    for (String key : featureKeys) {
        double sum = nucleusSums.getOrDefault(key, 0.0);
        aggregated.put("mean_nucleus_" + key, nucleusCount > 0 ? sum / nucleusCount : 0.0);
    }
    // 3. Cytoplasm Features
    CytoplasmROI cytoplasm = cell.getAssociatedCytoplasm();
    if (cytoplasm != null) {
      String cytoKey = imageFileName + "_" + cytoplasm.getName();
      Map<String, Object> cytoFeats = extractedFeatures.getOrDefault(cytoKey, Map.of());

      addFeatures(aggregated, cytoFeats, "cytoplasm");
    } else {
      // Add zeros for all cytoplasm features if no cytoplasm
      addZeroFeatures(aggregated, "cytoplasm");
    }

    // 4. Ratios
    double cytoArea = aggregated.getOrDefault("cytoplasm_area", 0.0);
    double cellArea = aggregated.getOrDefault("cell_area", 0.0);
    double totalNucArea = aggregated.getOrDefault("total_nucleus_area", 0.0);
    
    aggregated.put("nucleus_cytoplasm_ratio", cytoArea > 0 ? totalNucArea / cytoArea : 0.0);
    aggregated.put("nucleus_cell_ratio", cellArea > 0 ? totalNucArea / cellArea : 0.0);
    aggregated.put("cytoplasm_cell_ratio", cellArea > 0 ? cytoArea / cellArea : 0.0);

    // 5. Spatial Features
    aggregated.put("closest_neighbour_distance", getDouble(cellFeats, "closest_neighbour_dist"));
    aggregated.put("neighbour_density", getDouble(cellFeats, "neighbour_density"));
    aggregated.put("closest_vessel_distance", getDouble(cellFeats, "closest_vessel_dist"));
    aggregated.put("neighbor_count", getDouble(cellFeats, "neighbor_count"));

    return aggregated;
  }

  /**
   * Helper to add all standard features with a prefix.
   */
  private void addFeatures(Map<String, Double> aggregated, Map<String, Object> features, String prefix) {
      String[] keys = {
          "angle", "ar", "area", "bx", "by", "circ",
          "eosin_kurt", "eosin_max", "eosin_mean", "eosin_median", "eosin_min", "eosin_mode", "eosin_skew", "eosin_stddev",
          "feret", "feretangle", "feretx", "ferety", "height",
          "hema_kurt", "hema_max", "hema_mean", "hema_median", "hema_min", "hema_mode", "hema_skew", "hema_stddev",
          "intden", "kurt", "major", "max", "mean", "median", "min", "minferet", "minor", "mode",
          "perim", "round", "skew", "solidity", "stddev", "width", "x", "xm", "y", "ym"
      };

      for (String key : keys) {
          aggregated.put(prefix + "_" + key, getDouble(features, key));
      }
  }

  /**
   * Helper to add zero values for all standard features with a prefix.
   */
  private void addZeroFeatures(Map<String, Double> aggregated, String prefix) {
      String[] keys = {
          "angle", "ar", "area", "bx", "by", "circ",
          "eosin_kurt", "eosin_max", "eosin_mean", "eosin_median", "eosin_min", "eosin_mode", "eosin_skew", "eosin_stddev",
          "feret", "feretangle", "feretx", "ferety", "height",
          "hema_kurt", "hema_max", "hema_mean", "hema_median", "hema_min", "hema_mode", "hema_skew", "hema_stddev",
          "intden", "kurt", "major", "max", "mean", "median", "min", "minferet", "minor", "mode",
          "perim", "round", "skew", "solidity", "stddev", "width", "x", "xm", "y", "ym"
      };

      for (String key : keys) {
          aggregated.put(prefix + "_" + key, 0.0);
      }
  }

  /**
   * Helper to safely get a double value from the feature map.
   */
  private double getDouble(Map<String, Object> features, String key) {
    Object val = features.get(key);
    if (val instanceof Number) {
      return ((Number) val).doubleValue();
    }
    return 0.0;
  }

  /**
   * Gets the list of all available aggregated feature names.
   */
  public static String[] getFeatureNames() {
    return FEATURE_NAMES.clone();
  }
}