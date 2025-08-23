package com.scipath.scipathj.core.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Step 5 of the analysis pipeline: Cell Classification using XGBoost.
 *
 * Classifies cells based on extracted features using a pre-trained XGBoost model.
 * Compatible with SCHELI-trained models and integrates with the existing feature extraction system.
 *
 * @author SciPathJ Team
 * @version 2.0.0
 * @since 1.0.0
 */
public class CellClassification {

   private static final Logger LOGGER = LoggerFactory.getLogger(CellClassification.class);

   // XGBoost model and supporting files
   private Booster booster;
   private Map<Integer, Integer> xgbIndexToClassId = new HashMap<>();
   private List<String> loadedSelectedFeatureNames = new ArrayList<>();
   private Map<Integer, ClassDetails> classIdToDetails = new HashMap<>();

   // Decimal format configuration
   private static final String FLOAT_FORMAT_PATTERN = "#.######";
   private String csvSeparator = ";"; // EU format by default
   private char decimalSeparator = ',';

   // Model paths
   private static final String MODEL_DIR = "/models/2D/";
   private static final String MODEL_PATH = MODEL_DIR + "xgboost_model.json";
   private static final String SELECTED_FEATURES_PATH = MODEL_DIR + "selected_features.txt";
   private static final String LABEL_MAPPING_PATH = MODEL_DIR + "xgboost_label_mapping.properties";
   private static final String CLASS_DETAILS_PATH = MODEL_DIR + "class_details.json";

   private final String imageFileName;
   private final Map<String, Map<String, Object>> features;

  // Inner class for class details
  private static class ClassDetails {
      final String name;
      final int id;
      final String color;

      ClassDetails(String name, int id, String color) {
          this.name = name;
          this.id = id;
          this.color = color;
      }
  }

  /**
   * Constructor for CellClassification.
   *
   * @param imageFileName The filename of the image
   * @param features Previously extracted features for all ROIs
   */
  public CellClassification(
      final String imageFileName, final Map<String, Map<String, Object>> features) {
    this.imageFileName = imageFileName;
    this.features = features != null ? features : new HashMap<>();

    LOGGER.info("CellClassification initialized for image: {} with XGBoost classifier", imageFileName);

    // Initialize the classifier
    initializeClassifier();
  }

  /**
   * Initialize the XGBoost classifier with the pre-trained model and supporting files.
   */
  private void initializeClassifier() {
      try {
          // Load the XGBoost model
          loadXGBoostModel();

          // Load supporting files
          loadSelectedFeatures();
          loadLabelMapping();
          loadClassDetails();

          LOGGER.info("XGBoost classifier initialized successfully");
          LOGGER.info("ROI Analysis: {}", getROIStatistics());
      } catch (Exception e) {
          LOGGER.error("Failed to initialize XGBoost classifier: {}", e.getMessage(), e);
      }
  }

  /**
   * Load the pre-trained XGBoost model.
   */
  private void loadXGBoostModel() throws XGBoostError, IOException {
      try {
          String modelResourcePath = MODEL_PATH;
          var modelResource = getClass().getResource(modelResourcePath);

          if (modelResource == null) {
              throw new IOException("XGBoost model file not found in resources: " + modelResourcePath);
          }

          File modelFile = new File(modelResource.getFile());
          if (!modelFile.exists()) {
              throw new IOException("XGBoost model file not found: " + modelResourcePath);
          }

          booster = XGBoost.loadModel(modelFile.getAbsolutePath());
          LOGGER.info("XGBoost model loaded successfully from: {}", modelFile.getAbsolutePath());

      } catch (Exception e) {
          LOGGER.error("Error loading XGBoost model: {}", e.getMessage());
          throw e;
      }
  }

  /**
   * Load the list of selected features used during training.
   */
  private void loadSelectedFeatures() throws IOException {
      try {
          String featuresResourcePath = SELECTED_FEATURES_PATH;
          var featuresResource = getClass().getResource(featuresResourcePath);

          if (featuresResource == null) {
              throw new IOException("Selected features file not found in resources: " + featuresResourcePath);
          }

          File featuresFile = new File(featuresResource.getFile());
          if (!featuresFile.exists()) {
              throw new IOException("Selected features file not found: " + featuresResourcePath);
          }

          loadedSelectedFeatureNames = Files.readAllLines(Paths.get(featuresFile.getAbsolutePath()), StandardCharsets.UTF_8);
          loadedSelectedFeatureNames.removeIf(String::isEmpty);

          if (loadedSelectedFeatureNames.isEmpty()) {
              throw new IOException("Selected features file is empty: " + featuresResourcePath);
          }

          LOGGER.info("Loaded {} selected feature names from: {}", loadedSelectedFeatureNames.size(), featuresFile.getAbsolutePath());

      } catch (Exception e) {
          LOGGER.error("Error loading selected features: {}", e.getMessage());
          throw new IOException("Failed to load selected features", e);
      }
  }

  /**
   * Load the label mapping (XGBoost Index -> Original Class ID).
   */
  private void loadLabelMapping() throws IOException {
      try {
          String mappingResourcePath = LABEL_MAPPING_PATH;
          var mappingResource = getClass().getResource(mappingResourcePath);

          if (mappingResource == null) {
              throw new IOException("Label mapping file not found in resources: " + mappingResourcePath);
          }

          File mappingFile = new File(mappingResource.getFile());
          if (!mappingFile.exists()) {
              throw new IOException("Label mapping file not found: " + mappingResourcePath);
          }

          Properties props = new Properties();
          try (FileReader reader = new FileReader(mappingFile)) {
              props.load(reader);
          }

          xgbIndexToClassId.clear();
          for (String originalLabelStr : props.stringPropertyNames()) {
              try {
                  float originalLabel = Float.parseFloat(originalLabelStr);
                  int xgbIndex = Integer.parseInt(props.getProperty(originalLabelStr));
                  xgbIndexToClassId.put(xgbIndex, (int) originalLabel);
              } catch (NumberFormatException e) {
                  LOGGER.warn("Could not parse mapping entry: {} = {}", originalLabelStr, props.getProperty(originalLabelStr));
              }
          }

          LOGGER.info("Loaded label mapping for {} classes from: {}", xgbIndexToClassId.size(), mappingFile.getAbsolutePath());

      } catch (Exception e) {
          LOGGER.error("Error loading label mapping: {}", e.getMessage());
          throw new IOException("Failed to load label mapping", e);
      }
  }

  /**
   * Load class details (name, color) from JSON file.
   */
  private void loadClassDetails() throws IOException {
      try {
          String jsonResourcePath = CLASS_DETAILS_PATH;
          var jsonResource = getClass().getResource(jsonResourcePath);

          if (jsonResource == null) {
              LOGGER.warn("Class details JSON file not found in resources: {}. Using default class names.", jsonResourcePath);
              return;
          }

          File jsonFile = new File(jsonResource.getFile());
          if (!jsonFile.exists()) {
              LOGGER.warn("Class details JSON file not found: {}. Using default class names.", jsonResourcePath);
              return;
          }

          try (BufferedReader br = new BufferedReader(new FileReader(jsonFile))) {
              String json = br.lines().collect(Collectors.joining());

              int classesIndex = json.indexOf("\"classes\":");
              if (classesIndex == -1) {
                  LOGGER.warn("No 'classes' section in JSON file");
                  return;
              }

              int classesStart = json.indexOf("{", classesIndex);
              int classesEnd = findMatchingBrace(json, classesStart);
              if (classesStart == -1 || classesEnd == -1) {
                  LOGGER.warn("Malformed 'classes' section in JSON");
                  return;
              }

              String classesContent = json.substring(classesStart + 1, classesEnd);

              Pattern classPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{(.*?)\\}(,|$)", Pattern.DOTALL);
              Matcher classMatcher = classPattern.matcher(classesContent);

              while (classMatcher.find()) {
                  String className = classMatcher.group(1).trim();
                  String classContent = classMatcher.group(2);

                  Matcher idMatcher = Pattern.compile("\"id\"\\s*:\\s*([\\d.]+)").matcher(classContent);
                  Matcher colorMatcher = Pattern.compile("\"color\"\\s*:\\s*\"([^\"]+)\"").matcher(classContent);

                  if (idMatcher.find() && colorMatcher.find()) {
                      int classId = (int) Float.parseFloat(idMatcher.group(1).trim());
                      String color = colorMatcher.group(1).trim();
                      classIdToDetails.put(classId, new ClassDetails(className, classId, color));
                  }
              }
          }

          LOGGER.info("Loaded details for {} classes from JSON: {}", classIdToDetails.size(), jsonFile.getAbsolutePath());

      } catch (Exception e) {
          LOGGER.error("Error loading class details from JSON: {}", e.getMessage());
          // Don't throw exception, continue with default names
      }
  }

  /**
   * Helper method to find matching closing brace in JSON.
   */
  private int findMatchingBrace(String s, int start) {
      int count = 1;
      for (int i = start + 1; i < s.length(); i++) {
          char c = s.charAt(i);
          if (c == '{') count++;
          else if (c == '}') count--;
          if (count == 0) return i;
      }
      return -1;
  }

  /**
   * Classify cells based on extracted features using the pre-trained XGBoost model.
   *
   * This method:
   * 1. Prepares features for classification (extracts selected features)
   * 2. Creates DMatrix for XGBoost prediction
   * 3. Runs prediction using the loaded XGBoost model
   * 4. Maps predictions back to original class IDs and names
   * 5. Returns classification results with confidence scores
   *
   * @return Map of ROI names to classification results
   */
  public Map<String, ClassificationResult> classifyCells() {
      if (booster == null) {
          LOGGER.error("XGBoost model not loaded. Cannot perform classification.");
          return new HashMap<>();
      }

      if (features.isEmpty()) {
          LOGGER.warn("No features available for classification.");
          return new HashMap<>();
      }

      LOGGER.info("Starting XGBoost cell classification for {} total ROIs", features.size());

      try {
          // Prepare data for XGBoost using optimized bulk processing (cell ROIs only)
          Map<String, float[]> preparedFeatures = prepareFeaturesBulk();
          if (preparedFeatures.isEmpty()) {
              LOGGER.warn("No valid cell ROIs found for classification (vessel, nucleus, and cytoplasm ROIs are filtered out).");
              return new HashMap<>();
          }

          LOGGER.info("Prepared {} biological entities for classification (from {} total ROIs)",
                     preparedFeatures.size(), features.size());

          // Convert to DMatrix format
          List<String> roiKeys = new ArrayList<>(preparedFeatures.keySet());
          int numRows = roiKeys.size();
          int numCols = loadedSelectedFeatureNames.size();
          float[] featureData = new float[numRows * numCols];

          int rowIndex = 0;
          for (String roiKey : roiKeys) {
              float[] features = preparedFeatures.get(roiKey);
              System.arraycopy(features, 0, featureData, rowIndex * numCols, numCols);
              rowIndex++;
          }

          // Create DMatrix
          DMatrix classificationMatrix = new DMatrix(featureData, numRows, numCols, Float.NaN);
          LOGGER.debug("Created DMatrix for classification: {} rows, {} columns", numRows, numCols);

          // Run prediction
          float[][] predictions = booster.predict(classificationMatrix);
          LOGGER.debug("XGBoost prediction completed for {} samples", predictions.length);

          // Process results
          Map<String, ClassificationResult> results = new HashMap<>();
          for (int i = 0; i < roiKeys.size(); i++) {
              String roiKey = roiKeys.get(i);
              float[] probs = predictions[i];

              ClassificationResult result = processPrediction(roiKey, probs);
              if (result != null) {
                  results.put(roiKey, result);
              }
          }

          // Expand results to include all ROIs that belong to each classified entity
          Map<String, Map<String, Map<String, Object>>> entityGroups = groupROIsByEntity();
          Map<String, ClassificationResult> expandedResults = expandClassificationResults(results, entityGroups);

          LOGGER.info("Cell classification completed successfully. Classified {} biological entities ({} total ROI results).",
                     results.size(), expandedResults.size());
          return expandedResults;

      } catch (XGBoostError e) {
          LOGGER.error("XGBoost error during classification: {}", e.getMessage(), e);
          return new HashMap<>();
      } catch (Exception e) {
          LOGGER.error("Unexpected error during classification: {}", e.getMessage(), e);
          return new HashMap<>();
      }
  }

  /**
   * Prepare features by combining nucleus, cytoplasm, and cell ROIs for each biological entity.
   * This follows the SCHELI approach where each biological cell is represented by combined features
   * from its three ROI components, and each entity is classified exactly once.
   */
  private Map<String, float[]> prepareFeaturesBulk() {
      Map<String, float[]> preparedFeatures = new HashMap<>();
      Map<String, String> featureMapping = DataReorder.getFeatureMapping();

      // Group ROIs by biological entity ID
      Map<String, Map<String, Map<String, Object>>> entityGroups = groupROIsByEntity();

      LOGGER.debug("Found {} biological entities with complete ROI sets", entityGroups.size());

      for (Map.Entry<String, Map<String, Map<String, Object>>> entityEntry : entityGroups.entrySet()) {
          String entityId = entityEntry.getKey();
          Map<String, Map<String, Object>> entityROIs = entityEntry.getValue();

          // Get features for each ROI type
          Map<String, Object> nucleusFeatures = entityROIs.get("Nucleus");
          Map<String, Object> cytoplasmFeatures = entityROIs.get("Cytoplasm");
          Map<String, Object> cellFeatures = entityROIs.get("Cell");

          if (nucleusFeatures == null || cytoplasmFeatures == null || cellFeatures == null) {
              LOGGER.debug("Skipping entity {} - missing ROI components", entityId);
              continue;
          }

          // Combine features from all three ROI types into a single vector
          float[] combinedFeatures = combineEntityFeatures(
              nucleusFeatures, cytoplasmFeatures, cellFeatures, featureMapping);

          if (combinedFeatures != null && hasValidFeatures(combinedFeatures)) {
              // Use the cell ROI key as the identifier for this entity
              String cellKey = findCellKeyForEntity(entityId);
              if (cellKey != null) {
                  preparedFeatures.put(cellKey, combinedFeatures);
              } else {
                  // Fallback to entity ID if cell key not found
                  preparedFeatures.put("Entity_" + entityId, combinedFeatures);
              }
          }
      }

      LOGGER.debug("Bulk prepared features for {} biological entities (from {} total ROIs)",
                   preparedFeatures.size(), features.size());
      return preparedFeatures;
  }

  /**
   * Expand classification results to include all ROIs that belong to each classified entity.
   * This ensures that hovering over nucleus, cytoplasm, or cell ROIs all show the classification.
   */
  private Map<String, ClassificationResult> expandClassificationResults(
          Map<String, ClassificationResult> classificationResults,
          Map<String, Map<String, Map<String, Object>>> entityGroups) {

      Map<String, ClassificationResult> expandedResults = new HashMap<>(classificationResults);

      // For each classified entity, add results for all its ROI components
      for (Map.Entry<String, ClassificationResult> entry : classificationResults.entrySet()) {
          String resultKey = entry.getKey();
          ClassificationResult result = entry.getValue();

          // Extract entity ID from the result key
          String entityId = extractEntityIdFromResultKey(resultKey);
          if (entityId == null) continue;

          // Find the entity group
          Map<String, Map<String, Object>> entityROIs = entityGroups.get(entityId);
          if (entityROIs == null) continue;

          // Add classification result for each ROI component of this entity
          for (Map.Entry<String, Map<String, Object>> roiEntry : entityROIs.entrySet()) {
              String roiType = roiEntry.getKey();
              String roiKey = findROIKeyForEntity(entityId, roiType);

              if (roiKey != null && !expandedResults.containsKey(roiKey)) {
                  expandedResults.put(roiKey, result);
              }
          }
      }

      return expandedResults;
  }

  /**
   * Extract entity ID from classification result key.
   */
  private String extractEntityIdFromResultKey(String resultKey) {
      if (resultKey == null) return null;

      // Handle "Entity_ID" format
      if (resultKey.startsWith("Entity_")) {
          return resultKey.substring(7);
      }

      // Handle cell ROI key format
      ROIInfo roiInfo = parseROIInfo(resultKey);
      if (roiInfo != null && roiInfo.roiType.equals("Cell")) {
          return roiInfo.entityId;
      }

      return null;
  }

  /**
   * Find the ROI key for a specific entity and ROI type.
   */
  private String findROIKeyForEntity(String entityId, String roiType) {
      for (String roiKey : features.keySet()) {
          ROIInfo roiInfo = parseROIInfo(roiKey);
          if (roiInfo != null &&
              roiInfo.entityId.equals(entityId) &&
              roiInfo.roiType.equals(roiType)) {
              return roiKey;
          }
      }
      return null;
  }

  /**
   * Group ROIs by their biological entity ID, similar to SCHELI's approach.
   * Each entity should have nucleus, cytoplasm, and cell components.
   */
  private Map<String, Map<String, Map<String, Object>>> groupROIsByEntity() {
      Map<String, Map<String, Map<String, Object>>> entityGroups = new HashMap<>();

      for (Map.Entry<String, Map<String, Object>> roiEntry : features.entrySet()) {
          String roiKey = roiEntry.getKey();
          Map<String, Object> roiFeatures = roiEntry.getValue();

          // Skip vessel ROIs
          if (isVesselROI(roiKey)) {
              continue;
          }

          // Parse ROI type and entity ID
          ROIInfo roiInfo = parseROIInfo(roiKey);
          if (roiInfo == null) {
              continue;
          }

          // Group by entity ID
          entityGroups.computeIfAbsent(roiInfo.entityId, k -> new HashMap<>())
                     .put(roiInfo.roiType, roiFeatures);
      }

      return entityGroups;
  }

  /**
   * Combine features from nucleus, cytoplasm, and cell ROIs into a single feature vector.
   * This follows SCHELI's feature concatenation approach.
   */
  private float[] combineEntityFeatures(Map<String, Object> nucleusFeatures,
                                      Map<String, Object> cytoplasmFeatures,
                                      Map<String, Object> cellFeatures,
                                      Map<String, String> featureMapping) {
      float[] combinedFeatures = new float[loadedSelectedFeatureNames.size()];
      int validFeatures = 0;

      for (int i = 0; i < loadedSelectedFeatureNames.size(); i++) {
          String expectedFeatureName = loadedSelectedFeatureNames.get(i);
          String mappedFeatureName = featureMapping.get(expectedFeatureName);

          Object featureValue = null;

          // Try mapped name first across all ROI types
          if (mappedFeatureName != null) {
              featureValue = findFeatureInEntity(mappedFeatureName, nucleusFeatures,
                                               cytoplasmFeatures, cellFeatures);
          }

          // Fallback to direct match
          if (featureValue == null) {
              featureValue = findFeatureInEntity(expectedFeatureName, nucleusFeatures,
                                               cytoplasmFeatures, cellFeatures);
          }

          // Fallback to base feature name (remove prefix)
          if (featureValue == null) {
              int lastUnderscore = expectedFeatureName.lastIndexOf('_');
              if (lastUnderscore > 0) {
                  String baseName = expectedFeatureName.substring(lastUnderscore + 1);
                  featureValue = findFeatureInEntity(baseName, nucleusFeatures,
                                                   cytoplasmFeatures, cellFeatures);
              }
          }

          if (featureValue instanceof Number) {
              float value = ((Number) featureValue).floatValue();
              combinedFeatures[i] = value;
              validFeatures++;
          } else {
              combinedFeatures[i] = Float.NaN;
          }
      }

      return validFeatures > 0 ? combinedFeatures : null;
  }

  /**
   * Find a feature value in any of the three ROI types for an entity.
   */
  private Object findFeatureInEntity(String featureName, Map<String, Object> nucleusFeatures,
                                   Map<String, Object> cytoplasmFeatures, Map<String, Object> cellFeatures) {
      // Try cell features first (most specific)
      Object value = cellFeatures.get(featureName);
      if (value != null) return value;

      // Try nucleus features
      value = nucleusFeatures.get(featureName);
      if (value != null) return value;

      // Try cytoplasm features
      value = cytoplasmFeatures.get(featureName);
      if (value != null) return value;

      return null;
  }

  /**
   * Check if a feature vector has any valid (non-NaN) features.
   */
  private boolean hasValidFeatures(float[] features) {
      for (float feature : features) {
          if (!Float.isNaN(feature)) {
              return true;
          }
      }
      return false;
  }

  /**
   * Find the cell ROI key for a given entity ID.
   */
  private String findCellKeyForEntity(String entityId) {
      for (String roiKey : features.keySet()) {
          if (isVesselROI(roiKey)) continue;

          ROIInfo roiInfo = parseROIInfo(roiKey);
          if (roiInfo != null && roiInfo.entityId.equals(entityId) && roiInfo.roiType.equals("Cell")) {
              return roiKey;
          }
      }
      return null;
  }

  /**
   * Parse ROI information from ROI key to extract type and entity ID.
   * Handles cache keys in format: "imageName_ROIType_ID"
   */
  private ROIInfo parseROIInfo(String roiKey) {
      if (roiKey == null) return null;

      // Debug logging removed for production

      // Extract the ROI name from the key (format: "imageName_ROIType_ID")
      // Find the last occurrence of ROI type pattern (Cell_, Cytoplasm_, Nucleus_)
      String roiName = roiKey;
      String roiType = null;
      String entityId = null;

      // Try to find Cell pattern first
      if (roiKey.contains("_Cell_")) {
          roiType = "Cell";
          int cellIndex = roiKey.lastIndexOf("_Cell_");
          entityId = roiKey.substring(cellIndex + 6); // Skip "_Cell_"
      }
      // Try Cytoplasm pattern
      else if (roiKey.contains("_Cytoplasm_")) {
          roiType = "Cytoplasm";
          int cytoIndex = roiKey.lastIndexOf("_Cytoplasm_");
          entityId = roiKey.substring(cytoIndex + 11); // Skip "_Cytoplasm_"
      }
      // Try Nucleus pattern
      else if (roiKey.contains("_Nucleus_")) {
          roiType = "Nucleus";
          int nucleusIndex = roiKey.lastIndexOf("_Nucleus_");
          entityId = roiKey.substring(nucleusIndex + 9); // Skip "_Nucleus_"
      }
      // Try Vessel pattern for completeness
      else if (roiKey.contains("_Vessel_")) {
          roiType = "Vessel";
          int vesselIndex = roiKey.lastIndexOf("_Vessel_");
          entityId = roiKey.substring(vesselIndex + 8); // Skip "_Vessel_"
      }

      // Validate that we found a valid type and ID
      if (roiType != null && entityId != null && !entityId.isEmpty()) {
          // Only accept known ROI types (exclude vessels)
          if (roiType.equals("Nucleus") || roiType.equals("Cytoplasm") || roiType.equals("Cell")) {
              return new ROIInfo(roiType, entityId);
          }
      }

      return null;
  }

  /**
   * Check if an ROI is a vessel ROI.
   */
  private boolean isVesselROI(String roiKey) {
      if (roiKey == null) return false;
      return roiKey.contains("_Vessel_");
  }

  /**
   * Inner class to hold ROI parsing information.
   */
  private static class ROIInfo {
      final String roiType;
      final String entityId;

      ROIInfo(String roiType, String entityId) {
          this.roiType = roiType;
          this.entityId = entityId;
      }
  }


  /**
   * Extract selected features from the full feature map in the correct order.
   * This method handles the mismatch between FeatureExtraction output and classifier expectations.
   * Kept for compatibility but prepareFeaturesBulk() is preferred for performance.
   */
  private float[] extractSelectedFeatures(Map<String, Object> roiFeatures) {
      if (loadedSelectedFeatureNames.isEmpty()) {
          LOGGER.warn("No selected features loaded");
          return null;
      }

      float[] selectedFeatures = new float[loadedSelectedFeatureNames.size()];
      int nanCount = 0;

      for (int i = 0; i < loadedSelectedFeatureNames.size(); i++) {
          String expectedFeatureName = loadedSelectedFeatureNames.get(i);

          // Use DataReorder to find the correct feature value
          Object featureValue = findFeatureValue(expectedFeatureName, roiFeatures);

          if (featureValue instanceof Number) {
              float value = ((Number) featureValue).floatValue();
              selectedFeatures[i] = value;
              if (Float.isNaN(value)) {
                  nanCount++;
              }
          } else {
              selectedFeatures[i] = Float.NaN;
              nanCount++;
          }
      }

      if (nanCount > 0) {
          LOGGER.debug("Found {} NaN values in selected features", nanCount);
      }

      return selectedFeatures;
  }

  /**
   * Find the feature value in the ROI features map.
   * This handles the mismatch between classifier-expected names (e.g., "Nucleus_Area")
   * and FeatureExtraction output names (e.g., "area").
   */
  private Object findFeatureValue(String expectedFeatureName, Map<String, Object> roiFeatures) {
      // Use DataReorder to find the correct feature name mapping
      String featureExtractorName = DataReorder.getFeatureMapping().get(expectedFeatureName);

      if (featureExtractorName != null) {
          Object value = roiFeatures.get(featureExtractorName);
          if (value != null) {
              LOGGER.debug("Mapped feature '{}' to '{}' and found value", expectedFeatureName, featureExtractorName);
              return value;
          }
      }

      // Fallback: try direct match
      Object value = roiFeatures.get(expectedFeatureName);
      if (value != null) {
          return value;
      }

      // Fallback: try removing prefix and suffix variations
      int lastUnderscoreIndex = expectedFeatureName.lastIndexOf('_');
      if (lastUnderscoreIndex > 0 && lastUnderscoreIndex < expectedFeatureName.length() - 1) {
          String baseFeatureName = expectedFeatureName.substring(lastUnderscoreIndex + 1);
          value = roiFeatures.get(baseFeatureName);
          if (value != null) {
              LOGGER.debug("Found feature '{}' as '{}' using base name", expectedFeatureName, baseFeatureName);
              return value;
          }
      }

      // Try case-insensitive matching as last resort
      for (String key : roiFeatures.keySet()) {
          if (key.equalsIgnoreCase(expectedFeatureName)) {
              LOGGER.debug("Found feature '{}' with case-insensitive match '{}'", expectedFeatureName, key);
              return roiFeatures.get(key);
          }
      }

      return null;
  }

  /**
   * Process XGBoost prediction results and convert to ClassificationResult.
   */
  private ClassificationResult processPrediction(String roiKey, float[] probabilities) {
      if (probabilities == null || probabilities.length == 0) {
          LOGGER.warn("Invalid prediction probabilities for ROI: {}", roiKey);
          return null;
      }

      // Find the predicted class (highest probability)
      int predictedIndex = 0;
      float maxProb = probabilities[0];

      for (int i = 1; i < probabilities.length; i++) {
          if (probabilities[i] > maxProb) {
              maxProb = probabilities[i];
              predictedIndex = i;
          }
      }

      // Map XGBoost index to original class ID
      int predictedClassId = xgbIndexToClassId.getOrDefault(predictedIndex, -1);
      if (predictedClassId == -1) {
          LOGGER.warn("Could not map XGBoost index {} to class ID for ROI: {}", predictedIndex, roiKey);
          predictedClassId = predictedIndex; // Fallback to index as ID
      }

      // Get class details
      String predictedClassName = "Unknown";
      String predictedClassColor = "#000000";

      ClassDetails details = classIdToDetails.get(predictedClassId);
      if (details != null) {
          predictedClassName = details.name;
          predictedClassColor = details.color;
      } else {
          predictedClassName = "Class_" + predictedClassId;
      }

      // Create probability map for all classes
      Map<String, Double> classProbabilities = new HashMap<>();
      for (int i = 0; i < probabilities.length; i++) {
          int classId = xgbIndexToClassId.getOrDefault(i, i);
          String className = "Class_" + classId;
          ClassDetails classDetails = classIdToDetails.get(classId);
          if (classDetails != null) {
              className = classDetails.name;
          }
          classProbabilities.put(className, (double) probabilities[i]);
      }

      return new ClassificationResult(
          roiKey,
          predictedClassName,
          (double) maxProb,
          classProbabilities
      );
  }

  /**
   * Load a pre-trained classification model.
   *
   * This method is maintained for compatibility but the model is loaded automatically
   * during initialization. This method can be used to reload a different model.
   *
   * @param modelPath path to the classification model file (should be an XGBoost .json file)
   * @return true if model loaded successfully
   */
  public boolean loadModel(final String modelPath) {
      try {
          LOGGER.info("Loading XGBoost model from: {}", modelPath);

          if (modelPath == null || modelPath.isEmpty()) {
              LOGGER.error("Model path is null or empty");
              return false;
          }

          File modelFile = new File(modelPath);
          if (!modelFile.exists()) {
              LOGGER.error("Model file not found: {}", modelPath);
              return false;
          }

          // Load the new model
          Booster newBooster = XGBoost.loadModel(modelPath);

          // Update the booster reference
          this.booster = newBooster;

          LOGGER.info("XGBoost model loaded successfully from: {}", modelPath);
          return true;

      } catch (XGBoostError e) {
          LOGGER.error("XGBoost error loading model from {}: {}", modelPath, e.getMessage(), e);
          return false;
      } catch (Exception e) {
          LOGGER.error("Error loading model from {}: {}", modelPath, e.getMessage(), e);
          return false;
      }
  }

  /**
   * Preprocess features for classification.
   *
   * Feature preprocessing is handled automatically during classification.
   * This method is maintained for compatibility and can be used for additional
   * custom preprocessing if needed.
   *
   * @param rawFeatures raw extracted features
   * @return preprocessed features ready for classification
   */
  public Map<String, Object> preprocessFeatures(final Map<String, Object> rawFeatures) {
      LOGGER.debug("Feature preprocessing handled automatically during classification");

      // Feature selection and preprocessing is handled in the classifyCells method
      // This method can be extended for additional custom preprocessing if needed

      return rawFeatures; // Return as-is, preprocessing handled elsewhere
  }

  /**
   * Get statistics about the classification results.
   *
   * @param results classification results map
   * @return formatted statistics string
   */
  public String getStatistics(final Map<String, ClassificationResult> results) {
      if (results.isEmpty()) {
          return "No cells classified";
      }

      // Calculate class distribution
      Map<String, Integer> classCounts = new HashMap<>();
      Map<String, Double> classConfidences = new HashMap<>();

      for (ClassificationResult result : results.values()) {
          String className = result.getPredictedClass();
          classCounts.merge(className, 1, Integer::sum);

          // Accumulate confidence for average calculation
          classConfidences.merge(className, result.getConfidence(),
              (oldVal, newVal) -> oldVal + newVal);
      }

      StringBuilder stats = new StringBuilder();
      stats.append(String.format("Classified %d cells:\n", results.size()));

      // Sort classes by count for better display
      classCounts.entrySet().stream()
          .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
          .forEach(entry -> {
              String className = entry.getKey();
              int count = entry.getValue();
              double avgConfidence = classConfidences.get(className) / count;
              stats.append(String.format("  %s: %d cells (avg confidence: %.1f%%)\n",
                  className, count, avgConfidence * 100));
          });

      return stats.toString();
  }

  /**
   * Check if the classifier is properly initialized and ready for use.
   *
   * @return true if classifier is ready
   */
  public boolean isReady() {
      return booster != null && !loadedSelectedFeatureNames.isEmpty() && !xgbIndexToClassId.isEmpty();
  }

  /**
   * Get information about the loaded model and configuration.
   *
   * @return model information string
   */
  public String getModelInfo() {
      StringBuilder info = new StringBuilder();
      info.append("XGBoost Cell Classification Model:\n");
      info.append(String.format("  Model Loaded: %s\n", (booster != null ? "Yes" : "No")));
      info.append(String.format("  Selected Features: %d\n", loadedSelectedFeatureNames.size()));
      info.append(String.format("  Classes Mapped: %d\n", xgbIndexToClassId.size()));
      info.append(String.format("  Class Details Loaded: %d\n", classIdToDetails.size()));

      if (!classIdToDetails.isEmpty()) {
          info.append("  Available Classes:\n");
          classIdToDetails.values().stream()
              .sorted((a, b) -> Integer.compare(a.id, b.id))
              .forEach(details -> {
                  info.append(String.format("    %s (ID: %d, Color: %s)\n",
                      details.name, details.id, details.color));
              });
      }

      return info.toString();
  }

  /**
   * Get statistics about ROI types in the current feature set.
   * This helps understand what types of ROIs are available for classification.
   *
   * @return ROI type statistics string
   */
  public String getROIStatistics() {
      Map<String, Integer> roiTypeCounts = new HashMap<>();

      for (String roiKey : features.keySet()) {
          String roiType = getROIType(roiKey);
          roiTypeCounts.merge(roiType, 1, Integer::sum);
      }

      StringBuilder stats = new StringBuilder();
      stats.append("ROI Type Statistics:\n");
      stats.append(String.format("  Total ROIs: %d\n", features.size()));

      // Count complete biological entities that will be classified
      Map<String, Map<String, Map<String, Object>>> entityGroups = groupROIsByEntity();
      int completeEntityCount = (int) entityGroups.values().stream()
          .filter(entityROIs -> entityROIs.containsKey("Nucleus") &&
                               entityROIs.containsKey("Cytoplasm") &&
                               entityROIs.containsKey("Cell"))
          .count();
      stats.append(String.format("  Complete Biological Entities (will be classified): %d\n", completeEntityCount));

      roiTypeCounts.entrySet().stream()
          .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
          .forEach(entry -> {
              String marker = entry.getKey().toLowerCase().contains("cell") ? " ← CLASSIFIED" : "";
              stats.append(String.format("  %s: %d%s\n", entry.getKey(), entry.getValue(), marker));
          });

      return stats.toString();
  }

  /**
   * Extract ROI type from ROI key for statistics.
   */
  private String getROIType(String roiKey) {
      if (roiKey == null) return "unknown";

      // Extract the ROI name from the key (format: "imageName_ROIName")
      String roiName = roiKey;
      int lastUnderscore = roiKey.lastIndexOf('_');
      if (lastUnderscore > 0 && lastUnderscore < roiKey.length() - 1) {
          roiName = roiKey.substring(lastUnderscore + 1);
      }

      String lowerName = roiName.toLowerCase();

      // Determine type based on flexible pattern matching
      if (lowerName.startsWith("cell_") || lowerName.contains("cell") || lowerName.endsWith("_cell")) {
          return "Cell ROIs";
      } else if (lowerName.startsWith("nucleus_") || lowerName.contains("nucleus") || lowerName.endsWith("_nucleus")) {
          return "Nucleus ROIs";
      } else if (lowerName.startsWith("cytoplasm_") || lowerName.contains("cytoplasm") ||
                 lowerName.contains("cyto") || lowerName.endsWith("_cytoplasm") ||
                 lowerName.startsWith("cyto_") || lowerName.endsWith("_cyto")) {
          return "Cytoplasm ROIs";
      } else if (lowerName.startsWith("vessel_") || lowerName.contains("vessel") || lowerName.endsWith("_vessel")) {
          return "Vessel ROIs";
      } else {
          // Debug: Log unknown ROI names to help identify the issue
          System.out.println("CellClassification: Unknown ROI type for name: '" + roiName + "' (full key: '" + roiKey + "')");
          return "Other ROIs";
      }
  }

  /**
   * Result class for cell classification operations.
   */
  public static class ClassificationResult {
    private final String roiName;
    private final String predictedClass;
    private final double confidence;
    private final Map<String, Double> classProbabilities;

    public ClassificationResult(
        final String roiName,
        final String predictedClass,
        final double confidence,
        final Map<String, Double> classProbabilities) {
      this.roiName = roiName;
      this.predictedClass = predictedClass;
      this.confidence = confidence;
      this.classProbabilities = classProbabilities != null ? classProbabilities : new HashMap<>();
    }

    public String getRoiName() {
      return roiName;
    }

    public String getPredictedClass() {
      return predictedClass;
    }

    public double getConfidence() {
      return confidence;
    }

    public Map<String, Double> getClassProbabilities() {
      // Return defensive copy to prevent exposure of internal representation
      return new HashMap<>(classProbabilities);
    }

    @Override
    public String toString() {
      return String.format(
          "ClassificationResult[%s: %s (%.3f)]", roiName, predictedClass, confidence);
    }
  }
}
