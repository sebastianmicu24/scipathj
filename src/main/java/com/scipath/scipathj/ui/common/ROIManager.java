package com.scipath.scipathj.ui.common;

import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.analysis.algorithms.classification.CellClassification;
import ij.gui.Roi;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import java.awt.Rectangle;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages user-created ROIs for all images in the application.
 * Handles ROI storage, retrieval, and persistence operations.
 */
public class ROIManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ROIManager.class);

  // Map of image filename to list of ROIs for that image
  private final Map<String, List<UserROI>> imageROIs;

  // Map of ROI key to classification results for tooltip display
  private final Map<String, CellClassification.ClassificationResult> classificationResults;

  // Listeners for ROI changes
  private final List<ROIChangeListener> listeners;

  // Singleton instance
  private static ROIManager instance;

  private ROIManager() {
    this.imageROIs = new ConcurrentHashMap<>();
    this.classificationResults = new ConcurrentHashMap<>();
    this.listeners = new ArrayList<>();
  }

  /**
   * Get the singleton instance of ROIManager
   */
  public static synchronized ROIManager getInstance() {
    if (instance == null) {
      instance = new ROIManager();
    }
    return instance;
  }

  /**
   * Set classification results for ROIs (used for tooltip display)
   */
  public void setClassificationResults(Map<String, CellClassification.ClassificationResult> results) {
    if (results != null) {
      this.classificationResults.putAll(results);
      LOGGER.info("Stored {} classification results for ROI tooltips", results.size());
    }
  }

  /**
   * Get classification result for a specific ROI (for tooltip display)
   */
  public CellClassification.ClassificationResult getClassificationResult(String roiKey) {
    return this.classificationResults.get(roiKey);
  }

  /**
   * Get classification tooltip text for a specific ROI
   */
  public String getClassificationTooltipText(String roiKey) {
    CellClassification.ClassificationResult result = this.classificationResults.get(roiKey);
    if (result != null) {
      return String.format("Cell type: %s (confidence: %.1f%%)",
        result.getPredictedClass(),
        result.getConfidence() * 100);
    }
    return "Cell type: (not classified)";
  }

  /**
   * Clear all classification results
   */
  public void clearClassificationResults() {
    this.classificationResults.clear();
    LOGGER.info("Cleared all classification results");
  }

  /**
   * Get all classification results
   */
  public Map<String, CellClassification.ClassificationResult> getAllClassificationResults() {
    return new HashMap<>(this.classificationResults);
  }

  /**
   * Interface for listening to ROI changes
   */
  public interface ROIChangeListener {
    void onROIAdded(UserROI roi);

    void onROIRemoved(UserROI roi);

    void onROIUpdated(UserROI roi);

    void onROIsCleared(String imageFileName);
  }

  /**
   * Add a ROI change listener
   */
  public void addROIChangeListener(ROIChangeListener listener) {
    listeners.add(listener);
  }

  /**
   * Remove a ROI change listener
   */
  public void removeROIChangeListener(ROIChangeListener listener) {
    listeners.remove(listener);
  }

  /**
   * Add a ROI to the specified image
   */
  public void addROI(UserROI roi) {
    if (roi == null) return;

    String imageFileName = roi.getImageFileName();
    List<UserROI> roisForImage = imageROIs.computeIfAbsent(imageFileName, k -> new ArrayList<>());

    // Check for duplicate ROI by ID to prevent double counting
    boolean isDuplicate = roisForImage.stream().anyMatch(existing -> existing.getId().equals(roi.getId()));
    if (isDuplicate) {
      LOGGER.debug("Skipping duplicate ROI '{}' for image '{}'", roi.getName(), imageFileName);
      return;
    }

    roisForImage.add(roi);

    // LOGGER.info("Added ROI '{}' to image '{}'", roi.getName(), imageFileName);

    // Notify listeners
    listeners.forEach(
        listener -> {
          try {
            listener.onROIAdded(roi);
          } catch (Exception e) {
            LOGGER.error("Error notifying ROI listener", e);
          }
        });
  }

  /**
   * Remove a ROI by ID
   */
  public boolean removeROI(String roiId) {
    for (List<UserROI> rois : imageROIs.values()) {
      UserROI toRemove =
          rois.stream().filter(roi -> roi.getId().equals(roiId)).findFirst().orElse(null);

      if (toRemove != null) {
        rois.remove(toRemove);
        LOGGER.info(
            "Removed ROI '{}' from image '{}'", toRemove.getName(), toRemove.getImageFileName());

        // Notify listeners
        listeners.forEach(
            listener -> {
              try {
                listener.onROIRemoved(toRemove);
              } catch (Exception e) {
                LOGGER.error("Error notifying ROI listener", e);
              }
            });
        return true;
      }
    }
    return false;
  }

  /**
   * Remove a specific ROI
   */
  public boolean removeROI(UserROI roi) {
    return removeROI(roi.getId());
  }

  /**
   * Get all ROIs for a specific image
   */
  public List<UserROI> getROIsForImage(String imageFileName) {
    return imageROIs.getOrDefault(imageFileName, Collections.emptyList()).stream()
        .collect(Collectors.toList()); // Return defensive copy
  }

  /**
   * Get all ROIs across all images
   */
  public List<UserROI> getAllROIs() {
    return imageROIs.values().stream().flatMap(List::stream).collect(Collectors.toList());
  }

  /**
   * Get all ROIs organized by image filename
   * @return Map where key is image filename and value is list of ROIs for that image
   */
  public Map<String, List<UserROI>> getAllROIsByImage() {
    return new HashMap<>(imageROIs); // Return defensive copy
  }

  /**
   * Clear all ROIs for a specific image
   */
  public void clearROIsForImage(String imageFileName) {
    List<UserROI> removed = imageROIs.remove(imageFileName);
    if (removed != null && !removed.isEmpty()) {
      LOGGER.info("Cleared {} ROIs from image '{}'", removed.size(), imageFileName);

      // Notify listeners
      listeners.forEach(
          listener -> {
            try {
              listener.onROIsCleared(imageFileName);
            } catch (Exception e) {
              LOGGER.error("Error notifying ROI listener", e);
            }
          });
    }
  }

  /**
   * Clear all ROIs from all images
   */
  public void clearAllROIs() {
    Set<String> imageNames = new HashSet<>(imageROIs.keySet());
    imageROIs.clear();

    LOGGER.info("Cleared all ROIs from {} images", imageNames.size());

    // Notify listeners for each image
    imageNames.forEach(
        imageName -> {
          listeners.forEach(
              listener -> {
                try {
                  listener.onROIsCleared(imageName);
                } catch (Exception e) {
                  LOGGER.error("Error notifying ROI listener", e);
                }
              });
        });
  }

  /**
   * Get ROI count for a specific image
   */
  public int getROICount(String imageFileName) {
    return imageROIs.getOrDefault(imageFileName, Collections.emptyList()).size();
  }

  /**
   * Get total ROI count across all images
   */
  public int getTotalROICount() {
    return imageROIs.values().stream().mapToInt(List::size).sum();
  }

  /**
   * Check if an image has any ROIs
   */
  public boolean hasROIs(String imageFileName) {
    return getROICount(imageFileName) > 0;
  }

  /**
   * Find ROI by ID
   */
  public UserROI findROIById(String roiId) {
    return getAllROIs().stream().filter(roi -> roi.getId().equals(roiId)).findFirst().orElse(null);
  }

  /**
   * Save ROIs for a specific image to a .roi file (ImageJ compatible)
   */
  public void saveROIsToFile(String imageFileName, File outputFile) throws IOException {
    List<UserROI> rois = getROIsForImage(imageFileName);
    if (rois.isEmpty()) {
      throw new IllegalArgumentException("No ROIs found for image: " + imageFileName);
    }

    // For single ROI, save directly as .roi file
    if (rois.size() == 1) {
      saveSingleROI(rois.get(0), outputFile);
    } else {
      // For multiple ROIs, always save as .zip file (ImageJ ROI Set format)
      saveMultipleROIs(rois, outputFile);
    }

    LOGGER.info(
        "Saved {} ROIs from image '{}' to file '{}'",
        rois.size(),
        imageFileName,
        outputFile.getAbsolutePath());
  }

  /**
   * Save all ROIs from all images to a master ZIP file.
   * Each image's ROIs are saved in a separate ZIP file within the master ZIP.
   */
  public void saveAllROIsToMasterZip(File outputFile) throws IOException {
    if (imageROIs.isEmpty()) {
      throw new IllegalArgumentException("No ROIs found in any image");
    }

    // Create a temporary directory for individual image ZIP files
    File tempDir =
        new File(
            System.getProperty("java.io.tmpdir"),
            "scipathj_master_rois_" + System.currentTimeMillis());
    tempDir.mkdirs();

    try {
      // Create ZIP file for each image that has ROIs
      for (Map.Entry<String, List<UserROI>> entry : imageROIs.entrySet()) {
        String imageFileName = entry.getKey();
        List<UserROI> rois = entry.getValue();

        if (!rois.isEmpty()) {
          // Create safe filename for the image's ZIP file
          String safeImageName = sanitizeFileName(imageFileName);
          File imageZipFile = new File(tempDir, safeImageName + "_ROIs.zip");

          // Save ROIs for this image
          if (rois.size() == 1) {
            // For single ROI, create a ZIP containing the single .roi file
            saveSingleROIAsZip(rois.get(0), imageZipFile);
          } else {
            // For multiple ROIs, save as ZIP
            saveMultipleROIs(rois, imageZipFile);
          }
        }
      }

      // Create master ZIP file containing all image ZIP files
      createMasterZipFile(tempDir, outputFile);

      LOGGER.info(
          "Saved ROIs from {} images to master ZIP file '{}'",
          imageROIs.size(),
          outputFile.getAbsolutePath());

    } finally {
      // Clean up temporary files
      deleteDirectory(tempDir);
    }
  }

  /**
   * Save a single ROI as a ZIP file (for consistency in master ZIP)
   */
  private void saveSingleROIAsZip(UserROI userROI, File outputFile) throws IOException {
    // Create a temporary directory for the single ROI file
    File tempDir =
        new File(
            System.getProperty("java.io.tmpdir"),
            "scipathj_single_roi_" + System.currentTimeMillis());
    tempDir.mkdirs();

    try {
      // Save the ROI as individual file
      Roi ijRoi = convertToImageJROI(userROI);
      File roiFile = new File(tempDir, "0001-" + userROI.getName() + ".roi");
      RoiEncoder.save(ijRoi, roiFile.getAbsolutePath());

      // Create ZIP file containing the single ROI
      createROIZipFile(tempDir, outputFile);

    } finally {
      // Clean up temporary files
      deleteDirectory(tempDir);
    }
  }

  /**
   * Create master ZIP file containing all image ZIP files
   */
  private void createMasterZipFile(File sourceDir, File outputFile) throws IOException {
    try (java.util.zip.ZipOutputStream zos =
        new java.util.zip.ZipOutputStream(new FileOutputStream(outputFile))) {
      File[] zipFiles = sourceDir.listFiles((dir, name) -> name.endsWith(".zip"));
      if (zipFiles != null) {
        for (File zipFile : zipFiles) {
          java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(zipFile.getName());
          zos.putNextEntry(entry);

          try (FileInputStream fis = new FileInputStream(zipFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
              zos.write(buffer, 0, length);
            }
          }
          zos.closeEntry();
        }
      }
    }
  }

  /**
   * Sanitize filename to be safe for file system
   */
  private String sanitizeFileName(String fileName) {
    // Remove file extension if present
    String baseName = fileName;
    int lastDot = baseName.lastIndexOf('.');
    if (lastDot > 0) {
      baseName = baseName.substring(0, lastDot);
    }

    // Replace unsafe characters with underscores
    return baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
  }

  /**
   * Save a single ROI to file
   */
  private void saveSingleROI(UserROI userROI, File outputFile) throws IOException {
    Roi ijRoi = convertToImageJROI(userROI);
    RoiEncoder.save(ijRoi, outputFile.getAbsolutePath());
  }

  /**
   * Save multiple ROIs to file (as ROI set)
   */
  private void saveMultipleROIs(List<UserROI> rois, File outputFile) throws IOException {
    // Create a temporary directory for individual ROI files
    File tempDir =
        new File(
            System.getProperty("java.io.tmpdir"), "scipathj_rois_" + System.currentTimeMillis());
    tempDir.mkdirs();

    try {
      // Save each ROI as individual file
      for (int i = 0; i < rois.size(); i++) {
        UserROI userROI = rois.get(i);
        Roi ijRoi = convertToImageJROI(userROI);
        File roiFile = new File(tempDir, String.format("%04d-%s.roi", i + 1, userROI.getName()));
        RoiEncoder.save(ijRoi, roiFile.getAbsolutePath());
      }

      // Create ZIP file containing all ROIs
      createROIZipFile(tempDir, outputFile);

    } finally {
      // Clean up temporary files
      deleteDirectory(tempDir);
    }
  }

  /**
   * Create a ZIP file containing all ROI files (ImageJ ROI Set format)
   */
  private void createROIZipFile(File roiDir, File outputFile) throws IOException {
    try (java.util.zip.ZipOutputStream zos =
        new java.util.zip.ZipOutputStream(new FileOutputStream(outputFile))) {
      File[] roiFiles = roiDir.listFiles((dir, name) -> name.endsWith(".roi"));
      if (roiFiles != null) {
        for (File roiFile : roiFiles) {
          java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(roiFile.getName());
          zos.putNextEntry(entry);

          try (FileInputStream fis = new FileInputStream(roiFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
              zos.write(buffer, 0, length);
            }
          }
          zos.closeEntry();
        }
      }
    }
  }

  /**
   * Delete directory and all its contents
   */
  private void deleteDirectory(File dir) {
    if (dir.exists()) {
      File[] files = dir.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory()) {
            deleteDirectory(file);
          } else {
            file.delete();
          }
        }
      }
      dir.delete();
    }
  }

  /**
   * Convert UserROI to ImageJ ROI for saving
   */
  private Roi convertToImageJROI(UserROI userROI) {
    Roi roi;

    // Debug logging to understand the issue
    LOGGER.debug("Converting ROI: type={}, hasComplexShape={}, getImageJRoi()={}",
        userROI.getType(), userROI.hasComplexShape(), userROI.getImageJRoi());

    // Check if this UserROI has a complex shape (vessel ROI)
    if (userROI.hasComplexShape()) {
      // Use the stored ImageJ ROI directly
      roi = userROI.getImageJRoi();
      if (roi != null) {
        // Clone to avoid modifying the original
        roi = (Roi) roi.clone();
        LOGGER.debug("Using complex shape ROI: {}", roi.getClass().getSimpleName());
      } else {
        // Fallback to bounding rectangle
        roi = new Roi(userROI.getX(), userROI.getY(), userROI.getWidth(), userROI.getHeight());
        LOGGER.warn("Complex shape ROI was null, falling back to rectangle for ROI: {}", userROI.getName());
      }
    } else {
      // Handle simple shapes
      LOGGER.debug("Using simple shape ROI for type: {}", userROI.getType());
      switch (userROI.getType()) {
        case SQUARE:
        case RECTANGLE:
          roi = new Roi(userROI.getX(), userROI.getY(), userROI.getWidth(), userROI.getHeight());
          break;
        case CIRCLE:
          // Create oval ROI for circles
          roi =
              new ij.gui.OvalRoi(
                  userROI.getX(), userROI.getY(), userROI.getWidth(), userROI.getHeight());
          break;
        case COMPLEX_SHAPE:
        case VESSEL:
          // Special handling for vessel ROIs that might not have complex shape flag set
          roi = userROI.getImageJRoi();
          if (roi != null) {
            roi = (Roi) roi.clone();
            LOGGER.debug("Found ImageJ ROI for vessel type: {}", roi.getClass().getSimpleName());
          } else {
            roi = new Roi(userROI.getX(), userROI.getY(), userROI.getWidth(), userROI.getHeight());
            LOGGER.warn("No ImageJ ROI found for vessel type, using rectangle fallback for ROI: {}", userROI.getName());
          }
          break;
        default:
          roi = new Roi(userROI.getX(), userROI.getY(), userROI.getWidth(), userROI.getHeight());
          break;
      }
    }

    // Set common properties
    roi.setName(userROI.getName());
    roi.setStrokeColor(userROI.getDisplayColor());

    // Set stroke width
    roi.setStrokeWidth(2.0f);

    return roi;
  }

  /**
   * Load ROIs from a .roi file (ImageJ compatible)
   */
  public List<UserROI> loadROIsFromFile(File inputFile, String imageFileName) throws IOException {
    List<UserROI> loadedROIs = new ArrayList<>();

    if (inputFile.getName().toLowerCase().endsWith(".zip")) {
      // Load ROI set (ZIP file)
      loadedROIs.addAll(loadROISetFromZip(inputFile, imageFileName));
    } else {
      // Load single ROI file
      UserROI roi = loadSingleROI(inputFile, imageFileName);
      if (roi != null) {
        loadedROIs.add(roi);
      }
    }

    LOGGER.info(
        "Loaded {} ROIs from file '{}' for image '{}'",
        loadedROIs.size(),
        inputFile.getAbsolutePath(),
        imageFileName);

    return loadedROIs;
  }

  /**
   * Load a single ROI from file
   */
  private UserROI loadSingleROI(File inputFile, String imageFileName) throws IOException {
    Roi ijRoi = RoiDecoder.open(inputFile.getAbsolutePath());
    if (ijRoi != null) {
      return convertFromImageJROI(ijRoi, imageFileName);
    }
    return null;
  }

  /**
   * Load multiple ROIs from ZIP file
   */
  private List<UserROI> loadROISetFromZip(File zipFile, String imageFileName) throws IOException {
    List<UserROI> rois = new ArrayList<>();

    try (java.util.zip.ZipInputStream zis =
        new java.util.zip.ZipInputStream(new FileInputStream(zipFile))) {
      java.util.zip.ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.getName().endsWith(".roi")) {
          // Create temporary file for this ROI
          File tempFile = File.createTempFile("roi_", ".roi");
          try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = zis.read(buffer)) > 0) {
              fos.write(buffer, 0, length);
            }
          }

          // Load ROI from temporary file
          UserROI roi = loadSingleROI(tempFile, imageFileName);
          if (roi != null) {
            rois.add(roi);
          }

          // Clean up temporary file
          tempFile.delete();
        }
        zis.closeEntry();
      }
    }

    return rois;
  }

  /**
   * Convert ImageJ ROI to UserROI
   */
  private UserROI convertFromImageJROI(Roi ijRoi, String imageFileName) {
    String name = ijRoi.getName();
    if (name == null || name.isEmpty()) {
      name = "ROI_" + System.currentTimeMillis() % 10000;
    }

    UserROI userROI;

    // Check if this is a complex shape (not a simple rectangle or oval)
    if (isComplexShape(ijRoi)) {
      // Create UserROI with complex shape
      userROI = new UserROI(ijRoi, imageFileName, name);
    } else {
      // Create UserROI with simple shape
      Rectangle bounds = ijRoi.getBounds();
      UserROI.ROIType type;

      // Determine ROI type
      if (ijRoi instanceof ij.gui.OvalRoi) {
        type = UserROI.ROIType.CIRCLE;
      } else if (bounds.width == bounds.height) {
        type = UserROI.ROIType.SQUARE;
      } else {
        type = UserROI.ROIType.RECTANGLE;
      }

      userROI = new UserROI(type, bounds, imageFileName, name);
    }

    // Set properties from ImageJ ROI
    if (ijRoi.getStrokeColor() != null) {
      userROI.setDisplayColor(ijRoi.getStrokeColor());
    }

    return userROI;
  }

  /**
   * Check if an ImageJ ROI represents a complex shape
   */
  private boolean isComplexShape(Roi ijRoi) {
    // Consider it complex if it's not a simple rectangle or oval
    return !(ijRoi instanceof ij.gui.Roi && ijRoi.getType() == Roi.RECTANGLE)
        && !(ijRoi instanceof ij.gui.OvalRoi);
  }

  /**
   * Get statistics about ROIs
   */
  public Map<String, Object> getROIStatistics() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("totalImages", imageROIs.size());
    stats.put("totalROIs", getTotalROICount());

    // Per-image statistics
    Map<String, Integer> perImageStats = new HashMap<>();
    imageROIs.forEach((image, rois) -> perImageStats.put(image, rois.size()));
    stats.put("perImageROICount", perImageStats);

    return stats;
  }
}
