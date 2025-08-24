package com.scipath.scipathj.dataset;

import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.ui.common.ROIManager;
import ij.gui.Roi;
import ij.io.RoiDecoder;
import java.awt.Rectangle;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ultra-high-performance ROI loader with parallel processing and batch optimization.
 * Designed to handle thousands of ROIs with minimal latency.
 */
public class DatasetROILoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetROILoader.class);
    
    private final ROIManager roiManager;
    private final Map<String, List<UserROI>> instantCache = new ConcurrentHashMap<>();
    
    // Performance optimization: Reusable temp directory
    private Path tempDir;
    
    public DatasetROILoader() {
        this.roiManager = ROIManager.getInstance();
        initializeTempDirectory();
    }
    
    /**
     * Initialize reusable temp directory for maximum performance.
     */
    private void initializeTempDirectory() {
        try {
            tempDir = Files.createTempDirectory("scipathj_roi_batch_");
            tempDir.toFile().deleteOnExit();
            LOGGER.debug("Initialized batch temp directory: {}", tempDir);
        } catch (IOException e) {
            LOGGER.warn("Could not create batch temp directory, falling back to individual temp files");
            tempDir = null;
        }
    }
    
    /**
     * ULTRA-FAST ROI loading with parallel batch processing.
     */
    public List<UserROI> loadROIsForImage(File datasetZipFile, String imageFileName) {
        if (datasetZipFile == null || !datasetZipFile.exists() || imageFileName == null) {
            return new ArrayList<>();
        }
        
        String imageBaseName = getImageBaseName(imageFileName);
        String cacheKey = datasetZipFile.getAbsolutePath() + ":" + imageBaseName;
        
        // Instant cache lookup
        if (instantCache.containsKey(cacheKey)) {
            List<UserROI> cached = instantCache.get(cacheKey);
            LOGGER.debug("INSTANT: Retrieved {} ROIs from cache for '{}'", cached.size(), imageBaseName);
            return new ArrayList<>(cached);
        }
        
        // High-performance batch loading
        List<UserROI> rois = batchLoadROIs(datasetZipFile, imageBaseName, imageFileName);
        
        // Cache for future instant access
        if (!rois.isEmpty()) {
            instantCache.put(cacheKey, new ArrayList<>(rois));
        }
        
        return rois;
    }
    
    /**
     * Batch-optimized ROI loading with parallel processing.
     */
    private List<UserROI> batchLoadROIs(File zipFile, String imageBaseName, String imageFileName) {
        List<UserROI> rois = new ArrayList<>();
        
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile), 65536))) {
            ZipEntry entry;
            
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".zip") && matchesImage(entry.getName(), imageBaseName)) {
                    LOGGER.debug("BATCH: Found matching ZIP '{}' for image '{}'", entry.getName(), imageBaseName);
                    
                    // Process ROI ZIP with batch optimization
                    rois = processBatchROIZip(zis, imageFileName);
                    break; // Stop as soon as we find our ROI ZIP
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            LOGGER.error("Batch loading failed for '{}': {}", imageBaseName, e.getMessage());
        }
        
        return rois;
    }
    
    /**
     * Process ROI ZIP with batch extraction and parallel loading.
     */
    private List<UserROI> processBatchROIZip(ZipInputStream parentStream, String imageFileName) throws IOException {
        // Read the nested ZIP content
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[32768];
        int bytesRead;
        while ((bytesRead = parentStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        
        // Extract all ROI files to temp directory in batch
        Map<String, byte[]> roiDataMap = new ConcurrentHashMap<>();
        
        try (ZipInputStream nestedZip = new ZipInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            ZipEntry roiEntry;
            while ((roiEntry = nestedZip.getNextEntry()) != null) {
                if (roiEntry.getName().endsWith(".roi")) {
                    ByteArrayOutputStream roiBuffer = new ByteArrayOutputStream();
                    byte[] roiChunk = new byte[4096];
                    int roiBytesRead;
                    while ((roiBytesRead = nestedZip.read(roiChunk)) != -1) {
                        roiBuffer.write(roiChunk, 0, roiBytesRead);
                    }
                    roiDataMap.put(roiEntry.getName(), roiBuffer.toByteArray());
                }
                nestedZip.closeEntry();
            }
        }
        
        LOGGER.debug("BATCH: Extracted {} ROI files, starting parallel processing", roiDataMap.size());
        
        // Process ROIs in parallel batches for maximum performance
        return processROIsInParallel(roiDataMap, imageFileName);
    }
    
    /**
     * Process ROIs in parallel for maximum performance.
     */
    private List<UserROI> processROIsInParallel(Map<String, byte[]> roiDataMap, String imageFileName) {
        List<UserROI> rois = new ArrayList<>();
        
        if (roiDataMap.isEmpty()) {
            return rois;
        }
        
        // Create batch processing tasks
        List<CompletableFuture<List<UserROI>>> tasks = new ArrayList<>();
        List<Map.Entry<String, byte[]>> entries = new ArrayList<>(roiDataMap.entrySet());
        
        // Process in batches of 100 ROIs for optimal performance
        int batchSize = 100;
        for (int i = 0; i < entries.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, entries.size());
            List<Map.Entry<String, byte[]>> batch = entries.subList(i, endIndex);
            
            CompletableFuture<List<UserROI>> task = CompletableFuture.supplyAsync(() -> 
                processBatch(batch, imageFileName), ForkJoinPool.commonPool());
            tasks.add(task);
        }
        
        // Collect results from all batches
        for (CompletableFuture<List<UserROI>> task : tasks) {
            try {
                rois.addAll(task.get());
            } catch (Exception e) {
                LOGGER.error("Batch processing failed: {}", e.getMessage());
            }
        }
        
        LOGGER.debug("PARALLEL: Processed {} ROIs in {} batches", rois.size(), tasks.size());
        return rois;
    }
    
    /**
     * Process a batch of ROIs efficiently.
     */
    private List<UserROI> processBatch(List<Map.Entry<String, byte[]>> batch, String imageFileName) {
        List<UserROI> batchRois = new ArrayList<>();
        
        for (Map.Entry<String, byte[]> entry : batch) {
            try {
                UserROI roi = loadROIFromData(entry.getValue(), entry.getKey(), imageFileName);
                if (roi != null) {
                    batchRois.add(roi);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to load ROI '{}': {}", entry.getKey(), e.getMessage());
            }
        }
        
        return batchRois;
    }
    
    /**
     * Load single ROI from data with optimized temp file handling.
     */
    private UserROI loadROIFromData(byte[] roiData, String roiName, String imageFileName) {
        try {
            // Use batch temp directory if available, otherwise fall back to individual temp files
            File tempFile;
            if (tempDir != null) {
                tempFile = tempDir.resolve(roiName + "_" + System.nanoTime()).toFile();
            } else {
                tempFile = File.createTempFile("roi_", ".roi");
                tempFile.deleteOnExit();
            }
            
            // Write ROI data
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(roiData);
            }
            
            // Load and convert
            Roi ijRoi = RoiDecoder.open(tempFile.getAbsolutePath());
            
            // Immediate cleanup
            tempFile.delete();
            
            if (ijRoi != null) {
                return convertROI(ijRoi, imageFileName, roiName);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load ROI '{}': {}", roiName, e.getMessage());
        }
        return null;
    }
    
    /**
     * Convert ImageJ ROI to UserROI.
     */
    private UserROI convertROI(Roi ijRoi, String imageFileName, String roiName) {
        String cleanName = roiName.replaceAll("\\d+-", "").replace(".roi", "");
        if (cleanName.isEmpty()) {
            cleanName = "ROI";
        }
        
        UserROI.ROIType type = UserROI.ROIType.COMPLEX_SHAPE;
        if (ijRoi.getType() == Roi.RECTANGLE) {
            type = UserROI.ROIType.RECTANGLE;
        } else if (ijRoi.getType() == Roi.OVAL) {
            type = UserROI.ROIType.NUCLEUS;
        }
        
        if (type == UserROI.ROIType.COMPLEX_SHAPE) {
            return new UserROI(ijRoi, imageFileName, cleanName);
        } else {
            Rectangle bounds = ijRoi.getBounds();
            return new UserROI(type, bounds, imageFileName, cleanName);
        }
    }
    
    /**
     * Check if ZIP entry matches image name.
     */
    private boolean matchesImage(String zipName, String imageBaseName) {
        String baseName = zipName.toLowerCase();
        if (baseName.endsWith(".zip")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        baseName = baseName.replaceAll("_rois?$", "");
        
        String normalizedImage = imageBaseName.toLowerCase().replace(" ", "_");
        String normalizedEntry = baseName.replace(" ", "_");
        
        return normalizedEntry.equals(normalizedImage) || 
               normalizedEntry.contains(normalizedImage) ||
               normalizedImage.contains(normalizedEntry);
    }
    
    /**
     * Get image base name without extension.
     */
    private String getImageBaseName(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
    
    /**
     * Clear ROIs for an image.
     */
    public void clearROIsForImage(String imageFileName) {
        roiManager.clearROIsForImage(imageFileName);
    }
    
    /**
     * Get ROI count for an image.
     */
    public int getROICount(String imageFileName) {
        return roiManager.getROICount(imageFileName);
    }
    
    /**
     * Clear all caches.
     */
    public void clearCache() {
        instantCache.clear();
    }
    
    /**
     * Cleanup resources.
     */
    public void cleanup() {
        clearCache();
        if (tempDir != null) {
            try {
                Files.deleteIfExists(tempDir);
            } catch (IOException e) {
                LOGGER.warn("Could not delete temp directory: {}", e.getMessage());
            }
        }
    }
}