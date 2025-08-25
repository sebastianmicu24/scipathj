package com.scipath.scipathj.ui.dataset;

import com.scipath.scipathj.infrastructure.roi.DefaultROIService;
import com.scipath.scipathj.infrastructure.roi.ROIService;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import ij.gui.Roi;
import ij.io.RoiDecoder;
import java.awt.Color;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced Dataset-specific ROI manager with interactive features for dataset creation.
 * Inspired by HTML Cell Classifier with click-to-assign, hover effects, and visual controls.
 *
 * Key Features:
 * - Interactive class assignment with custom colors
 * - Real-time statistics and visual feedback
 * - Visual controls (opacity, border width, selection state)
 * - ROI selection and hover state management
 *
 * @author Sebastian Micu
 * @version 2.0.0
 */
public class DatasetROIManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetROIManager.class);
    
    // Core ROI service for basic operations
    private final ROIService roiService;
    
    // Dataset-specific data
    private final Map<String, String> roiClassAssignments = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> availableClasses = new ConcurrentHashMap<>();
    private final Map<String, Integer> classStatistics = new ConcurrentHashMap<>();
    
    // Enhanced interactive features
    private final Map<String, Color> classColors = new ConcurrentHashMap<>();
    private String selectedClassName = "Unclassified";
    private UserROI selectedROI = null;
    private UserROI hoveredROI = null;
    
    // Visual control settings (from HTML Cell Classifier)
    private float fillOpacity = 0.2f;
    private float borderWidth = 2.0f;
    private boolean outlinesVisible = true;
    
    // Loading state
    private volatile boolean isLoading = false;
    private int totalROIsLoaded = 0;
    private String currentLoadingOperation = "";
    
    // Listeners for dataset-specific events
    private final List<DatasetROIListener> datasetListeners = new CopyOnWriteArrayList<>();
    private final List<DatasetInteractionListener> interactionListeners = new CopyOnWriteArrayList<>();
    
    /**
     * Interface for listening to dataset-specific ROI events.
     */
    public interface DatasetROIListener {
        void onClassAssigned(String roiKey, String className);
        void onClassRemoved(String roiKey);
        void onLoadingStarted(String operation);
        void onLoadingProgress(int loaded, int total);
        void onLoadingCompleted(int totalLoaded);
        void onLoadingFailed(String error);
    }
    
    /**
     * Interface for listening to interactive events (inspired by HTML Cell Classifier).
     */
    public interface DatasetInteractionListener {
        void onROISelected(UserROI roi);
        void onROIHovered(UserROI roi);
        void onROIUnhovered(UserROI roi);
        void onClassCreated(String className, Color color);
        void onClassRemoved(String className);
        void onSelectedClassChanged(String className);
        void onVisualSettingsChanged(float opacity, float borderWidth, boolean outlinesVisible);
    }
    
    public DatasetROIManager() {
        this.roiService = new DefaultROIService();
        
        // Initialize default "Unclassified" class
        classColors.put("Unclassified", new Color(61, 61, 61)); // Dark gray like HTML version
        
        LOGGER.debug("Created enhanced DatasetROIManager with interactive features");
    }
    
    // === INTERACTIVE CLASS MANAGEMENT (inspired by HTML Cell Classifier) ===
    
    /**
     * Create a new class with custom color.
     */
    public void createClass(String className, Color color) {
        if (className == null || className.trim().isEmpty()) {
            LOGGER.warn("Cannot create class with empty name");
            return;
        }
        
        String trimmedName = className.trim();
        if (classColors.containsKey(trimmedName)) {
            LOGGER.debug("Class '{}' already exists, updating color", trimmedName);
        }
        
        classColors.put(trimmedName, color);
        
        // Notify listeners
        notifyInteractionListeners(listener -> listener.onClassCreated(trimmedName, color));
        LOGGER.debug("Created class '{}' with color {}", trimmedName, color);
    }
    
    /**
     * Remove a class and unassign all ROIs from it.
     */
    public void removeClass(String className) {
        if (className == null || "Unclassified".equals(className)) {
            LOGGER.warn("Cannot remove null or 'Unclassified' class");
            return;
        }
        
        if (!classColors.containsKey(className)) {
            LOGGER.warn("Class '{}' does not exist", className);
            return;
        }
        
        // Unassign all ROIs from this class
        roiClassAssignments.entrySet().removeIf(entry -> className.equals(entry.getValue()));
        
        // Remove from available classes
        availableClasses.values().forEach(classSet -> classSet.remove(className));
        
        // Remove color mapping
        classColors.remove(className);
        
        // Update statistics
        classStatistics.remove(className);
        
        // Reset selected class if it was removed
        if (className.equals(selectedClassName)) {
            selectedClassName = "Unclassified";
            notifyInteractionListeners(listener -> listener.onSelectedClassChanged(selectedClassName));
        }
        
        // Notify listeners
        notifyInteractionListeners(listener -> listener.onClassRemoved(className));
        LOGGER.debug("Removed class '{}' and unassigned all ROIs", className);
    }
    
    /**
     /**
      * Set the currently selected class for assignment.
      */
     public void setSelectedClass(String className) {
         final String finalClassName = (className != null) ? className : "Unclassified";
         
         if (!selectedClassName.equals(finalClassName)) {
             selectedClassName = finalClassName;
             notifyInteractionListeners(listener -> listener.onSelectedClassChanged(finalClassName));
             LOGGER.debug("Selected class changed to '{}'", finalClassName);
         }
     }
    /**
     * Get the currently selected class.
     */
    public String getSelectedClass() {
        return selectedClassName;
    }
    
    /**
     /**
      * Get color for a class.
      */
     public Color getClassColor(String className) {
         if (className == null) {
             return new Color(61, 61, 61); // Default gray for unassigned ROIs
         }
         return classColors.getOrDefault(className, new Color(61, 61, 61));
     }
    /**
     * Get all available classes with their colors.
     */
    public Map<String, Color> getAllClassColors() {
        return new HashMap<>(classColors);
    }
    
    // === ROI SELECTION AND INTERACTION ===
    
    /**
     * Select an ROI (from click interaction).
     */
    public void selectROI(UserROI roi) {
        if (selectedROI != roi) {
            selectedROI = roi;
            notifyInteractionListeners(listener -> listener.onROISelected(roi));
            LOGGER.debug("Selected ROI: {}", roi != null ? roi.getName() : "none");
        }
    }
    
    /**
     * Set hovered ROI (from mouse movement).
     */
    public void setHoveredROI(UserROI roi) {
        if (hoveredROI != roi) {
            UserROI previousHovered = hoveredROI;
            hoveredROI = roi;
            
            if (previousHovered != null) {
                notifyInteractionListeners(listener -> listener.onROIUnhovered(previousHovered));
            }
            if (roi != null) {
                notifyInteractionListeners(listener -> listener.onROIHovered(roi));
            }
        }
    }
    
    /**
     * Get currently selected ROI.
     */
    public UserROI getSelectedROI() {
        return selectedROI;
    }
    
    /**
     * Get currently hovered ROI.
     */
    public UserROI getHoveredROI() {
        return hoveredROI;
    }
    
    /**
     /**
      * Assign the currently selected class to an ROI (click-to-assign functionality).
      */
     public String assignClassToROI(UserROI roi, String className) {
         if (roi == null) {
             return null;
         }
         
         // Make className effectively final for use in lambda
         final String finalClassName = (className != null) ? className : selectedClassName;
         
         String roiKey = generateROIKey(roi.getImageFileName(), roi.getName());
         
         // Remove from old class if assigned
         String oldClass = roiClassAssignments.get(roiKey);
         if (oldClass != null && !oldClass.equals(finalClassName)) {
             updateClassStatistics(oldClass, -1);
         }
         
         if ("Unclassified".equals(finalClassName)) {
             // Unclassify the ROI
             roiClassAssignments.remove(roiKey);
             if (oldClass != null) {
                 notifyDatasetListeners(listener -> listener.onClassRemoved(roiKey));
             }
         } else {
             // Assign new class
             roiClassAssignments.put(roiKey, finalClassName);
             updateClassStatistics(finalClassName, 1);
             
             // Add to available classes
             String imageFileName = roi.getImageFileName();
             if (imageFileName != null) {
                 availableClasses.computeIfAbsent(imageFileName, k -> ConcurrentHashMap.newKeySet()).add(finalClassName);
             }
             
             notifyDatasetListeners(listener -> listener.onClassAssigned(roiKey, finalClassName));
         }
         
         LOGGER.debug("Assigned class '{}' to ROI '{}'", finalClassName, roi.getName());
         return finalClassName;
     }
    // === VISUAL CONTROLS (from HTML Cell Classifier) ===
    
    /**
     * Set fill opacity (0.0 to 1.0).
     */
    public void setFillOpacity(float opacity) {
        opacity = Math.max(0.0f, Math.min(1.0f, opacity));
        if (Math.abs(fillOpacity - opacity) > 0.001f) {
            fillOpacity = opacity;
            notifyInteractionListeners(listener ->
                listener.onVisualSettingsChanged(fillOpacity, borderWidth, outlinesVisible));
            LOGGER.debug("Fill opacity changed to {}", opacity);
        }
    }
    
    /**
     * Set border width.
     */
    public void setBorderWidth(float width) {
        width = Math.max(0.0f, width);
        if (Math.abs(borderWidth - width) > 0.001f) {
            borderWidth = width;
            notifyInteractionListeners(listener ->
                listener.onVisualSettingsChanged(fillOpacity, borderWidth, outlinesVisible));
            LOGGER.debug("Border width changed to {}", width);
        }
    }
    
    /**
     * Toggle outline visibility (E key functionality from HTML).
     */
    public void toggleOutlines() {
        outlinesVisible = !outlinesVisible;
        notifyInteractionListeners(listener ->
            listener.onVisualSettingsChanged(fillOpacity, borderWidth, outlinesVisible));
        LOGGER.debug("Outlines visibility toggled to {}", outlinesVisible);
    }
    
    /**
     * Get current fill opacity.
     */
    public float getFillOpacity() {
        return fillOpacity;
    }
    
    /**
     * Get current border width.
     */
    public float getBorderWidth() {
        return borderWidth;
    }
    
    /**
     * Check if outlines are visible.
     */
    public boolean areOutlinesVisible() {
        return outlinesVisible;
    }
    
    // === CORE ROI OPERATIONS (delegated to service) ===
    
    public void addROI(UserROI roi) {
        roiService.addROI(roi);
        LOGGER.debug("Added ROI '{}' to dataset", roi.getName());
    }
    
    public boolean removeROI(String roiId) {
        // Clean up dataset-specific data
        String roiKey = findROIKeyById(roiId);
        if (roiKey != null) {
            String removedClass = roiClassAssignments.remove(roiKey);
            if (removedClass != null) {
                updateClassStatistics(removedClass, -1);
                notifyDatasetListeners(listener -> listener.onClassRemoved(roiKey));
            }
        }
        
        boolean removed = roiService.removeROI(roiId);
        if (removed) {
            LOGGER.debug("Removed ROI '{}' from dataset with all class data", roiId);
        }
        return removed;
    }
    
    public List<UserROI> getROIsForImage(String imageFileName) {
        return roiService.getROIsForImage(imageFileName);
    }
    
    public List<UserROI> getAllROIs() {
        return roiService.getAllROIs();
    }
    
    public void clearROIsForImage(String imageFileName) {
        // Clean up dataset-specific data for this image
        List<UserROI> rois = roiService.getROIsForImage(imageFileName);
        for (UserROI roi : rois) {
            String roiKey = generateROIKey(imageFileName, roi.getName());
            String removedClass = roiClassAssignments.remove(roiKey);
            if (removedClass != null) {
                updateClassStatistics(removedClass, -1);
            }
        }
        
        roiService.clearROIsForImage(imageFileName);
        LOGGER.debug("Cleared all ROIs and class assignments for image '{}'", imageFileName);
    }
    
    public void clearAllROIs() {
        roiClassAssignments.clear();
        classStatistics.clear();
        availableClasses.clear();
        roiService.clearAllROIs();
        LOGGER.debug("Cleared all ROIs and dataset data");
    }
    
    public int getROICount(String imageFileName) {
        return roiService.getROICount(imageFileName);
    }
    
    public boolean hasROIs(String imageFileName) {
        return roiService.hasROIs(imageFileName);
    }
    
    public UserROI findROIById(String roiId) {
        return roiService.findROIById(roiId);
    }
    
    // === DATASET-SPECIFIC OPERATIONS ===
    
    /**
     * Load ROIs from a ZIP file with improved error handling and async processing.
     */
    public void loadROIsFromZipFile(File zipFile, String imageFileName) {
        if (zipFile == null || !zipFile.exists() || imageFileName == null) {
            notifyDatasetListeners(listener -> listener.onLoadingFailed("Invalid file or image name"));
            return;
        }
        
        isLoading = true;
        currentLoadingOperation = "Loading ROIs from " + zipFile.getName();
        notifyDatasetListeners(listener -> listener.onLoadingStarted(currentLoadingOperation));
        
        // Load ROIs asynchronously for better performance
        CompletableFuture.supplyAsync(() -> {
            try {
                return loadROIsFromZip(zipFile, imageFileName);
            } catch (Exception e) {
                LOGGER.error("Failed to load ROIs from ZIP file '{}': {}", zipFile.getName(), e.getMessage(), e);
                SwingUtilities.invokeLater(() -> {
                    notifyDatasetListeners(listener -> listener.onLoadingFailed(e.getMessage()));
                    isLoading = false;
                    currentLoadingOperation = "";
                });
                return new ArrayList<UserROI>();
            }
        }).thenAcceptAsync(loadedROIs -> {
            SwingUtilities.invokeLater(() -> {
                // Add ROIs in batches for better UI responsiveness
                int batchSize = 100;
                for (int i = 0; i < loadedROIs.size(); i += batchSize) {
                    int endIndex = Math.min(i + batchSize, loadedROIs.size());
                    List<UserROI> batch = loadedROIs.subList(i, endIndex);
                    
                    for (UserROI roi : batch) {
                        roiService.addROI(roi);
                    }
                    
                    // Update progress
                    final int progress = endIndex;
                    notifyDatasetListeners(listener -> listener.onLoadingProgress(progress, loadedROIs.size()));
                }
                
                totalROIsLoaded = loadedROIs.size();
                notifyDatasetListeners(listener -> listener.onLoadingCompleted(totalROIsLoaded));
                
                LOGGER.info("Successfully loaded {} ROIs from ZIP file '{}' for image '{}'",
                    totalROIsLoaded, zipFile.getName(), imageFileName);
                
                isLoading = false;
                currentLoadingOperation = "";
            });
        });
    }
    
    /**
     * Assign a class to an ROI.
     */
    public void assignClass(String roiKey, String className) {
        if (roiKey == null || className == null || className.trim().isEmpty()) {
            return;
        }
        
        // Remove from old class if assigned
        String oldClass = roiClassAssignments.get(roiKey);
        if (oldClass != null && !oldClass.equals(className)) {
            updateClassStatistics(oldClass, -1);
        }
        
        // Assign new class
        roiClassAssignments.put(roiKey, className);
        updateClassStatistics(className, 1);
        
        // Add to available classes
        String imageFileName = extractImageFromROIKey(roiKey);
        if (imageFileName != null) {
            availableClasses.computeIfAbsent(imageFileName, k -> ConcurrentHashMap.newKeySet()).add(className);
        }
        
        notifyDatasetListeners(listener -> listener.onClassAssigned(roiKey, className));
        LOGGER.debug("Assigned class '{}' to ROI '{}'", className, roiKey);
    }
    
    /**
     * Remove class assignment from an ROI.
     */
    public void removeClassAssignment(String roiKey) {
        String removedClass = roiClassAssignments.remove(roiKey);
        if (removedClass != null) {
            updateClassStatistics(removedClass, -1);
            notifyDatasetListeners(listener -> listener.onClassRemoved(roiKey));
            LOGGER.debug("Removed class assignment from ROI '{}'", roiKey);
        }
    }
    
    /**
     * Get class assignment for an ROI.
     */
    public String getClassAssignment(String roiKey) {
        return roiClassAssignments.get(roiKey);
    }
    
    /**
     * Get all class assignments.
     */
    public Map<String, String> getAllClassAssignments() {
        return new HashMap<>(roiClassAssignments);
    }
    
    /**
     * Get available classes for an image.
     */
    public Set<String> getAvailableClasses(String imageFileName) {
        return new HashSet<>(availableClasses.getOrDefault(imageFileName, Collections.emptySet()));
    }
    
    /**
     * Get all available classes across all images.
     */
    public Set<String> getAllAvailableClasses() {
        return availableClasses.values().stream()
            .flatMap(Set::stream)
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }
    
    /**
     * Get class statistics.
     */
    public Map<String, Integer> getClassStatistics() {
        return new HashMap<>(classStatistics);
    }
    
    /**
     * Get ROIs filtered by class.
     */
    public List<UserROI> getROIsByClass(String className) {
        if (className == null) {
            return Collections.emptyList();
        }
        
        return roiService.getAllROIs().stream()
            .filter(roi -> {
                String roiKey = generateROIKey(roi.getImageFileName(), roi.getName());
                return className.equals(roiClassAssignments.get(roiKey));
            })
            .toList();
    }
    
    /**
     * Get unassigned ROIs for an image.
     */
    public List<UserROI> getUnassignedROIsForImage(String imageFileName) {
        return getROIsForImage(imageFileName).stream()
            .filter(roi -> {
                String roiKey = generateROIKey(imageFileName, roi.getName());
                return !roiClassAssignments.containsKey(roiKey);
            })
            .toList();
    }
    
    // === LOADING STATUS ===
    
    public boolean isLoading() {
        return isLoading;
    }
    
    public String getCurrentLoadingOperation() {
        return currentLoadingOperation;
    }
    
    public int getTotalROIsLoaded() {
        return totalROIsLoaded;
    }
    
    // === FILE OPERATIONS ===
    
    public void saveROIsToFile(String imageFileName, File outputFile) throws IOException {
        roiService.saveROIsToFile(imageFileName, outputFile);
    }
    
    public void saveAllROIsToMasterZip(File outputFile) throws IOException {
        roiService.saveAllROIsToMasterZip(outputFile);
    }
    
    // === LISTENER MANAGEMENT ===
    
    public void addDatasetListener(DatasetROIListener listener) {
        if (listener != null) {
            datasetListeners.add(listener);
        }
    }
    
    public void removeDatasetListener(DatasetROIListener listener) {
        datasetListeners.remove(listener);
    }
    
    public void addROIChangeListener(ROIService.ROIChangeListener listener) {
        roiService.addChangeListener(listener);
    }
    
    public void removeROIChangeListener(ROIService.ROIChangeListener listener) {
        roiService.removeChangeListener(listener);
    }
    
    public void addInteractionListener(DatasetInteractionListener listener) {
        if (listener != null) {
            interactionListeners.add(listener);
        }
    }
    
    public void removeInteractionListener(DatasetInteractionListener listener) {
        interactionListeners.remove(listener);
    }
    
    // === HELPER METHODS ===
    
    private List<UserROI> loadROIsFromZip(File zipFile, String imageFileName) throws IOException {
        List<UserROI> rois = new ArrayList<>();
        int processedFiles = 0;
        
        // Convert image filename to expected ZIP filename format (spaces to underscores)
        String expectedZipName = convertImageNameToZipName(imageFileName);
        LOGGER.debug("Looking for nested ZIP file '{}' for image '{}'", expectedZipName, imageFileName);
        
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile), 65536))) {
            ZipEntry entry;
            
            while ((entry = zis.getNextEntry()) != null) {
                LOGGER.debug("Found ZIP entry: '{}' (size: {}, dir: {})",
                    entry.getName(), entry.getSize(), entry.isDirectory());
                
                // Look for nested ZIP file matching the image name
                if (entry.getName().equals(expectedZipName) && !entry.isDirectory()) {
                    LOGGER.info("Found matching nested ZIP file '{}' for image '{}'", entry.getName(), imageFileName);
                    
                    // Load ROIs from the nested ZIP file
                    rois.addAll(loadROIsFromNestedZip(zis, imageFileName));
                    processedFiles++;
                    break; // Found our ZIP file, no need to continue
                }
                zis.closeEntry();
            }
        }
        
        LOGGER.info("Loaded {} ROIs from {} nested ZIP files", rois.size(), processedFiles);
        return rois;
    }
    
    /**
     * Convert image filename to expected ZIP filename format.
     * Spaces become underscores, adds _ROIs suffix, keeps file extension as .zip.
     */
    private String convertImageNameToZipName(String imageFileName) {
        if (imageFileName == null) {
            return null;
        }
        
        // Remove file extension and replace spaces with underscores
        String nameWithoutExt = imageFileName;
        int lastDot = imageFileName.lastIndexOf('.');
        if (lastDot > 0) {
            nameWithoutExt = imageFileName.substring(0, lastDot);
        }
        
        return nameWithoutExt.replace(' ', '_') + "_ROIs.zip";
    }
    
    /**
     * Load ROIs from a nested ZIP file stream.
     */
    private List<UserROI> loadROIsFromNestedZip(ZipInputStream parentZis, String imageFileName) throws IOException {
        List<UserROI> rois = new ArrayList<>();
        
        // Read all bytes from the nested ZIP
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = parentZis.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        
        // Create a new ZipInputStream from the nested ZIP data
        try (ZipInputStream nestedZis = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            ZipEntry nestedEntry;
            
            while ((nestedEntry = nestedZis.getNextEntry()) != null) {
                if (nestedEntry.getName().endsWith(".roi")) {
                    try {
                        UserROI roi = loadROIFromZipEntry(nestedZis, nestedEntry.getName(), imageFileName);
                        if (roi != null) {
                            rois.add(roi);
                            LOGGER.debug("Loaded ROI '{}' from nested ZIP", nestedEntry.getName());
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to load ROI '{}' from nested ZIP: {}", nestedEntry.getName(), e.getMessage());
                    }
                }
                nestedZis.closeEntry();
            }
        }
        
        LOGGER.info("Loaded {} ROIs from nested ZIP for image '{}'", rois.size(), imageFileName);
        return rois;
    }
    
    private UserROI loadROIFromZipEntry(ZipInputStream zis, String entryName, String imageFileName) throws IOException {
        // Filter ROIs at loading time for better performance - only load cells and nuclei
        String roiName = cleanROIName(entryName);
        if (!shouldLoadROI(roiName)) {
            LOGGER.debug("Skipping ROI '{}' - not a cell or nucleus", roiName);
            return null;
        }
        
        // Create temporary file for ROI data
        File tempFile = File.createTempFile("roi_", ".roi");
        tempFile.deleteOnExit();
        
        try {
            // Write ROI data to temp file
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = zis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            
            // Load ROI from temp file
            Roi ijRoi = RoiDecoder.open(tempFile.getAbsolutePath());
            if (ijRoi != null) {
                return new UserROI(ijRoi, imageFileName, roiName);
            }
            
        } finally {
            tempFile.delete();
        }
        
        return null;
    }
    
    /**
     * Check if ROI should be loaded based on name (performance optimization).
     */
    private boolean shouldLoadROI(String roiName) {
        if (roiName == null) {
            return false;
        }
        String name = roiName.toLowerCase();
        return name.startsWith("cell") || name.startsWith("nucleus");
    }
    
    private String cleanROIName(String entryName) {
        String name = entryName;
        
        // Remove path components
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        
        // Remove file extension
        if (name.endsWith(".roi")) {
            name = name.substring(0, name.length() - 4);
        }
        
        // Remove numerical prefixes (e.g., "0001-" from "0001-Cell_5.roi")
        if (name.matches("\\d+-.*")) {
            int dashIndex = name.indexOf('-');
            if (dashIndex > 0) {
                name = name.substring(dashIndex + 1);
            }
        }
        
        return name.isEmpty() ? "ROI_" + System.currentTimeMillis() % 10000 : name;
    }
    
    private void updateClassStatistics(String className, int delta) {
        classStatistics.merge(className, delta, Integer::sum);
        
        // Remove class if count reaches zero
        if (classStatistics.get(className) <= 0) {
            classStatistics.remove(className);
        }
    }
    
    private void notifyDatasetListeners(java.util.function.Consumer<DatasetROIListener> action) {
        datasetListeners.forEach(listener -> {
            try {
                action.accept(listener);
            } catch (Exception e) {
                LOGGER.error("Error notifying dataset ROI listener", e);
            }
        });
    }
    
    public String generateROIKey(String imageFileName, String roiName) {
        return imageFileName + "_" + roiName;
    }
    
    private String extractImageFromROIKey(String roiKey) {
        int underscoreIndex = roiKey.indexOf('_');
        return underscoreIndex > 0 ? roiKey.substring(0, underscoreIndex) : null;
    }
    
    private String findROIKeyById(String roiId) {
        UserROI roi = roiService.findROIById(roiId);
        if (roi != null) {
            return generateROIKey(roi.getImageFileName(), roi.getName());
        }
        return null;
    }
    
    /**
     /**
      * Get statistics about the dataset ROIs.
      */
     public DatasetStatistics getDatasetStatistics() {
         int totalROIs = roiService.getTotalROICount();
         int assignedROIs = roiClassAssignments.size();
         int totalClasses = getAllAvailableClasses().size();
         
         return new DatasetStatistics(totalROIs, assignedROIs, totalClasses);
     }
     
     private void notifyInteractionListeners(java.util.function.Consumer<DatasetInteractionListener> action) {
         interactionListeners.forEach(listener -> {
             try {
                 action.accept(listener);
             } catch (Exception e) {
                 LOGGER.error("Error notifying dataset interaction listener", e);
             }
         });
     }
     
     /**
      * Simple record for dataset statistics.
      */
     public record DatasetStatistics(int totalROIs, int assignedROIs, int totalClasses) {
         public double getAssignmentRate() {
             return totalROIs > 0 ? (double) assignedROIs / totalROIs : 0.0;
         }
         
         public boolean isComplete() {
             return totalROIs > 0 && assignedROIs == totalROIs;
         }
     }
 }