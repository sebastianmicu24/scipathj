package com.scipath.scipathj.infrastructure.roi;

import ij.gui.Roi;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import java.awt.Rectangle;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of ROIService that provides core ROI management functionality.
 * This replaces the singleton ROIManager with a proper service-based approach.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 */
public class DefaultROIService implements ROIService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultROIService.class);
    
    // Map of image filename to list of ROIs for that image
    private final Map<String, List<UserROI>> imageROIs = new ConcurrentHashMap<>();
    
    // Listeners for ROI changes
    private final List<ROIChangeListener> listeners = new CopyOnWriteArrayList<>();
    
    public DefaultROIService() {
        LOGGER.debug("Created DefaultROIService instance");
    }
    
    @Override
    public void addROI(UserROI roi) {
        if (roi == null) {
            LOGGER.warn("Attempted to add null ROI");
            return;
        }
        
        String imageFileName = roi.getImageFileName();
        List<UserROI> roisForImage = imageROIs.computeIfAbsent(imageFileName, k -> new ArrayList<>());
        
        // Check for duplicate ROI by ID to prevent double counting
        boolean isDuplicate = roisForImage.stream().anyMatch(existing -> existing.getId().equals(roi.getId()));
        if (isDuplicate) {
            LOGGER.debug("Skipping duplicate ROI '{}' for image '{}'", roi.getName(), imageFileName);
            return;
        }
        
        roisForImage.add(roi);
        LOGGER.debug("Added ROI '{}' to image '{}' (total: {})", roi.getName(), imageFileName, roisForImage.size());
        
        // Notify listeners
        notifyListeners(listener -> listener.onROIAdded(roi));
    }
    
    @Override
    public boolean removeROI(String roiId) {
        if (roiId == null) {
            return false;
        }
        
        for (List<UserROI> rois : imageROIs.values()) {
            UserROI toRemove = rois.stream()
                .filter(roi -> roi.getId().equals(roiId))
                .findFirst()
                .orElse(null);
            
            if (toRemove != null) {
                rois.remove(toRemove);
                LOGGER.debug("Removed ROI '{}' from image '{}'", toRemove.getName(), toRemove.getImageFileName());
                
                // Notify listeners
                notifyListeners(listener -> listener.onROIRemoved(toRemove));
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean removeROI(UserROI roi) {
        return roi != null && removeROI(roi.getId());
    }
    
    @Override
    public List<UserROI> getROIsForImage(String imageFileName) {
        if (imageFileName == null) {
            return Collections.emptyList();
        }
        return imageROIs.getOrDefault(imageFileName, Collections.emptyList())
            .stream()
            .collect(Collectors.toList()); // Return defensive copy
    }
    
    @Override
    public List<UserROI> getAllROIs() {
        return imageROIs.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, List<UserROI>> getAllROIsByImage() {
        // Return defensive copy
        Map<String, List<UserROI>> result = new HashMap<>();
        imageROIs.forEach((image, rois) -> 
            result.put(image, new ArrayList<>(rois)));
        return result;
    }
    
    @Override
    public void clearROIsForImage(String imageFileName) {
        if (imageFileName == null) {
            return;
        }
        
        List<UserROI> removed = imageROIs.remove(imageFileName);
        if (removed != null && !removed.isEmpty()) {
            LOGGER.debug("Cleared {} ROIs from image '{}'", removed.size(), imageFileName);
            
            // Notify listeners
            notifyListeners(listener -> listener.onROIsCleared(imageFileName));
        }
    }
    
    @Override
    public void clearAllROIs() {
        Set<String> imageNames = new HashSet<>(imageROIs.keySet());
        imageROIs.clear();
        
        LOGGER.debug("Cleared all ROIs from {} images", imageNames.size());
        
        // Notify listeners for each image
        imageNames.forEach(imageName -> 
            notifyListeners(listener -> listener.onROIsCleared(imageName)));
    }
    
    @Override
    public int getROICount(String imageFileName) {
        if (imageFileName == null) {
            return 0;
        }
        return imageROIs.getOrDefault(imageFileName, Collections.emptyList()).size();
    }
    
    @Override
    public int getTotalROICount() {
        return imageROIs.values().stream().mapToInt(List::size).sum();
    }
    
    @Override
    public boolean hasROIs(String imageFileName) {
        return getROICount(imageFileName) > 0;
    }
    
    @Override
    public UserROI findROIById(String roiId) {
        if (roiId == null) {
            return null;
        }
        return getAllROIs().stream()
            .filter(roi -> roi.getId().equals(roiId))
            .findFirst()
            .orElse(null);
    }
    
    @Override
    public void saveROIsToFile(String imageFileName, File outputFile) throws IOException {
        List<UserROI> rois = getROIsForImage(imageFileName);
        if (rois.isEmpty()) {
            throw new IllegalArgumentException("No ROIs found for image: " + imageFileName);
        }
        
        if (rois.size() == 1) {
            saveSingleROI(rois.get(0), outputFile);
        } else {
            saveMultipleROIs(rois, outputFile);
        }
        
        LOGGER.info("Saved {} ROIs from image '{}' to file '{}'", 
            rois.size(), imageFileName, outputFile.getAbsolutePath());
    }
    
    @Override
    public void saveAllROIsToMasterZip(File outputFile) throws IOException {
        if (imageROIs.isEmpty()) {
            throw new IllegalArgumentException("No ROIs found in any image");
        }
        
        File tempDir = new File(System.getProperty("java.io.tmpdir"), 
            "scipathj_master_rois_" + System.currentTimeMillis());
        tempDir.mkdirs();
        
        try {
            // Create ZIP file for each image that has ROIs
            for (Map.Entry<String, List<UserROI>> entry : imageROIs.entrySet()) {
                String imageFileName = entry.getKey();
                List<UserROI> rois = entry.getValue();
                
                if (!rois.isEmpty()) {
                    String safeImageName = sanitizeFileName(imageFileName);
                    File imageZipFile = new File(tempDir, safeImageName + "_ROIs.zip");
                    
                    if (rois.size() == 1) {
                        saveSingleROIAsZip(rois.get(0), imageZipFile);
                    } else {
                        saveMultipleROIs(rois, imageZipFile);
                    }
                }
            }
            
            // Create master ZIP file containing all image ZIP files
            createMasterZipFile(tempDir, outputFile);
            
            LOGGER.info("Saved ROIs from {} images to master ZIP file '{}'", 
                imageROIs.size(), outputFile.getAbsolutePath());
                
        } finally {
            deleteDirectory(tempDir);
        }
    }
    
    @Override
    public List<UserROI> loadROIsFromFile(File inputFile, String imageFileName) throws IOException {
        List<UserROI> loadedROIs = new ArrayList<>();
        
        if (inputFile.getName().toLowerCase().endsWith(".zip")) {
            loadedROIs.addAll(loadROISetFromZip(inputFile, imageFileName));
        } else {
            UserROI roi = loadSingleROI(inputFile, imageFileName);
            if (roi != null) {
                loadedROIs.add(roi);
            }
        }
        
        LOGGER.info("Loaded {} ROIs from file '{}' for image '{}'", 
            loadedROIs.size(), inputFile.getAbsolutePath(), imageFileName);
        
        return loadedROIs;
    }
    
    @Override
    public void addChangeListener(ROIChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    @Override
    public void removeChangeListener(ROIChangeListener listener) {
        listeners.remove(listener);
    }
    
    // === PRIVATE HELPER METHODS ===
    
    private void notifyListeners(java.util.function.Consumer<ROIChangeListener> action) {
        listeners.forEach(listener -> {
            try {
                action.accept(listener);
            } catch (Exception e) {
                LOGGER.error("Error notifying ROI listener", e);
            }
        });
    }
    
    private void saveSingleROI(UserROI userROI, File outputFile) throws IOException {
        Roi ijRoi = convertToImageJROI(userROI);
        RoiEncoder.save(ijRoi, outputFile.getAbsolutePath());
    }
    
    private void saveMultipleROIs(List<UserROI> rois, File outputFile) throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), 
            "scipathj_rois_" + System.currentTimeMillis());
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
            deleteDirectory(tempDir);
        }
    }
    
    private void saveSingleROIAsZip(UserROI userROI, File outputFile) throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), 
            "scipathj_single_roi_" + System.currentTimeMillis());
        tempDir.mkdirs();
        
        try {
            Roi ijRoi = convertToImageJROI(userROI);
            File roiFile = new File(tempDir, "0001-" + userROI.getName() + ".roi");
            RoiEncoder.save(ijRoi, roiFile.getAbsolutePath());
            
            createROIZipFile(tempDir, outputFile);
            
        } finally {
            deleteDirectory(tempDir);
        }
    }
    
    private void createMasterZipFile(File sourceDir, File outputFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
            File[] zipFiles = sourceDir.listFiles((dir, name) -> name.endsWith(".zip"));
            if (zipFiles != null) {
                for (File zipFile : zipFiles) {
                    ZipEntry entry = new ZipEntry(zipFile.getName());
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
    
    private void createROIZipFile(File roiDir, File outputFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
            File[] roiFiles = roiDir.listFiles((dir, name) -> name.endsWith(".roi"));
            if (roiFiles != null) {
                for (File roiFile : roiFiles) {
                    ZipEntry entry = new ZipEntry(roiFile.getName());
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
    
    private String sanitizeFileName(String fileName) {
        String baseName = fileName;
        int lastDot = baseName.lastIndexOf('.');
        if (lastDot > 0) {
            baseName = baseName.substring(0, lastDot);
        }
        return baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
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
    
    private Roi convertToImageJROI(UserROI userROI) {
        if (userROI.hasComplexShape() && userROI.getImageJRoi() != null) {
            Roi roi = (Roi) userROI.getImageJRoi().clone();
            roi.setName(userROI.getName());
            roi.setStrokeColor(userROI.getDisplayColor());
            roi.setStrokeWidth(2.0f);
            return roi;
        } else {
            // Fallback to bounding rectangle for simple cases
            Rectangle bounds = userROI.getBounds();
            Roi roi = new Roi(bounds.x, bounds.y, bounds.width, bounds.height);
            roi.setName(userROI.getName());
            roi.setStrokeColor(userROI.getDisplayColor());
            roi.setStrokeWidth(2.0f);
            return roi;
        }
    }
    
    private UserROI loadSingleROI(File inputFile, String imageFileName) throws IOException {
        Roi ijRoi = RoiDecoder.open(inputFile.getAbsolutePath());
        if (ijRoi != null) {
            return convertFromImageJROI(ijRoi, imageFileName);
        }
        return null;
    }
    
    private List<UserROI> loadROISetFromZip(File zipFile, String imageFileName) throws IOException {
        List<UserROI> rois = new ArrayList<>();
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".roi")) {
                    File tempFile = File.createTempFile("roi_", ".roi");
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                    
                    UserROI roi = loadSingleROI(tempFile, imageFileName);
                    if (roi != null) {
                        rois.add(roi);
                    }
                    
                    tempFile.delete();
                }
                zis.closeEntry();
            }
        }
        
        return rois;
    }
    
    private UserROI convertFromImageJROI(Roi ijRoi, String imageFileName) {
        String name = ijRoi.getName();
        if (name == null || name.isEmpty()) {
            name = "ROI_" + System.currentTimeMillis() % 10000;
        }
        
        // Create UserROI with complex shape (all biological structures are complex)
        UserROI userROI = new UserROI(ijRoi, imageFileName, name);
        
        // Set properties from ImageJ ROI
        if (ijRoi.getStrokeColor() != null) {
            userROI.setDisplayColor(ijRoi.getStrokeColor());
        }
        
        return userROI;
    }
}