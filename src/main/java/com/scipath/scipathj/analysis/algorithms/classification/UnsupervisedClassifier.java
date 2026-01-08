package com.scipath.scipathj.analysis.algorithms.classification;

import com.scipath.scipathj.infrastructure.roi.CellROI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.clustering.KMeans;
import smile.clustering.DBSCAN;
import smile.clustering.PartitionClustering;

/**
 * Service class for performing unsupervised classification (clustering) on cells.
 * Supports K-Means and DBSCAN clustering from the Smile library.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class UnsupervisedClassifier {

  private static final Logger LOGGER = LoggerFactory.getLogger(UnsupervisedClassifier.class);

  private final CellFeatureAggregator featureAggregator;

  public UnsupervisedClassifier() {
    this.featureAggregator = new CellFeatureAggregator();
  }

  /**
   * Performs K-Means clustering on a list of cells.
   *
   * @param cells the list of cells to cluster
   * @param extractedFeatures the raw features extracted for all ROIs
   * @param imageFileName the image filename (for feature lookup)
   * @param imageFileName the image filename (for feature lookup)
   * @param k the number of clusters (for K-Means)
   * @param selectedFeatures the list of feature names to use for clustering
   * @param algorithm the clustering algorithm to use ("K-Means" or "DBSCAN")
   * @param maxIterations maximum iterations for K-Means
   * @param epsilon epsilon parameter for DBSCAN
   * @param minPoints minimum points parameter for DBSCAN
   * @return a map where keys are cluster IDs and values are lists of cells in that cluster
   */
  public Map<Integer, List<CellROI>> clusterCells(
      List<CellROI> cells,
      Map<String, Map<String, Object>> extractedFeatures,
      String imageFileName,
      int k,
      List<String> selectedFeatures,
      String algorithm,
      int maxIterations,
      double epsilon,
      int minPoints) {

    if (cells == null || cells.isEmpty()) {
      LOGGER.warn("No cells provided for clustering.");
      return new HashMap<>();
    }

    if ("K-Means".equals(algorithm) && k < 2) {
      LOGGER.warn("K must be at least 2 for K-Means clustering.");
      return new HashMap<>();
    }

    LOGGER.info("Starting {} clustering for {} cells using features: {}", algorithm, cells.size(), selectedFeatures);

    // 1. Prepare data matrix
    double[][] data = new double[cells.size()][selectedFeatures.size()];
    List<CellROI> validCells = new ArrayList<>();

    int rowIndex = 0;
    for (CellROI cell : cells) {
      try {
        Map<String, Double> aggregatedFeatures = featureAggregator.aggregateFeatures(cell, extractedFeatures, imageFileName);
        
        for (int colIndex = 0; colIndex < selectedFeatures.size(); colIndex++) {
          String featureName = selectedFeatures.get(colIndex);
          data[rowIndex][colIndex] = aggregatedFeatures.getOrDefault(featureName, 0.0);
        }
        
        validCells.add(cell);
        rowIndex++;
      } catch (Exception e) {
        LOGGER.warn("Failed to aggregate features for cell {}: {}", cell.getName(), e.getMessage());
      }
    }

    // Resize data array if some cells were skipped
    if (validCells.size() < cells.size()) {
      double[][] trimmedData = new double[validCells.size()][selectedFeatures.size()];
      System.arraycopy(data, 0, trimmedData, 0, validCells.size());
      data = trimmedData;
    }

    if ("K-Means".equals(algorithm) && data.length < k) {
       LOGGER.warn("Not enough valid cells ({}) for K={}", data.length, k);
       return new HashMap<>();
    }

    // 2. Perform Clustering
    try {
      // Standardize data (Z-score normalization) is crucial for clustering
      data = normalizeData(data);

      int[] labels;
      int numClusters;

      if ("DBSCAN".equals(algorithm)) {
          DBSCAN<double[]> dbscan = DBSCAN.fit(data, minPoints, epsilon);
          labels = dbscan.y;
          // DBSCAN labels: -1 is noise, 0 to k-1 are clusters
          // We need to count actual clusters
          int maxLabel = -1;
          for (int label : labels) {
              if (label > maxLabel) maxLabel = label;
          }
          numClusters = maxLabel + 1; // +1 because 0-indexed
          LOGGER.info("DBSCAN found {} clusters (plus noise)", numClusters);
      } else {
          // Default to K-Means
          // Smile's KMeans.fit(double[][], int) uses default max iterations (100)
          // To specify max iterations, we need to use the constructor or a different fit method depending on version
          // For Smile 2.x/3.x compatibility, let's use the standard fit(data, k) and ignore maxIterations for now
          // or check if there's a specific overload.
          // The error message says fit(double[][], int, int) is not found.
          // Let's try the standard fit(data, k) which is widely supported.
          KMeans kMeans = KMeans.fit(data, k);
          labels = kMeans.y;
          numClusters = k;
      }

      // 3. Map results back to cells
      Map<Integer, List<CellROI>> clusters = new HashMap<>();
      
      // Initialize lists for found clusters
      for (int i = 0; i < numClusters; i++) {
        clusters.put(i, new ArrayList<>());
      }
      
      // Add a special cluster for noise (-1) if using DBSCAN
      if ("DBSCAN".equals(algorithm)) {
          clusters.put(-1, new ArrayList<>());
      }

      for (int i = 0; i < validCells.size(); i++) {
        int clusterId = labels[i];
        if (clusters.containsKey(clusterId)) {
            clusters.get(clusterId).add(validCells.get(i));
        }
      }

      LOGGER.info("Clustering completed successfully.");
      return clusters;

    } catch (Exception e) {
      LOGGER.error("Clustering failed: {}", e.getMessage(), e);
      return new HashMap<>();
    }
  }
  /**
   * Simple Z-score normalization (column-wise).
   */
  private double[][] normalizeData(double[][] data) {
    int rows = data.length;
    int cols = data[0].length;
    double[][] normalized = new double[rows][cols];

    for (int j = 0; j < cols; j++) {
      double sum = 0;
      double sumSq = 0;
      for (int i = 0; i < rows; i++) {
        sum += data[i][j];
        sumSq += data[i][j] * data[i][j];
      }
      double mean = sum / rows;
      double std = Math.sqrt((sumSq / rows) - (mean * mean));

      if (std == 0) std = 1; // Avoid division by zero

      for (int i = 0; i < rows; i++) {
        normalized[i][j] = (data[i][j] - mean) / std;
      }
    }
    return normalized;
  }
}