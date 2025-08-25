package com.scipath.scipathj.ui.dataset;

import com.scipath.scipathj.infrastructure.roi.UserROI;
import ij.gui.Roi;
import ij.io.RoiDecoder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Progressive ROI loader that displays ROIs as they're loaded for immediate visual feedback.
 * Optimized for speed with smart filtering and batch processing.
 * 
 * @author Sebastian Micu
 * @version 3.0.0
 */
public class ProgressiveROILoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressiveROILoader.class);
    
    // Batch processing settings
    private static final int BATCH_SIZE = 200;
    private static final int BATCH_DELAY_MS = 50;
    
    // Progress listeners
    private final List<ProgressListener> listeners = new ArrayList<>();
    
    // Loading state
    private volatile boolean isLoading = false;
    private volatile boolean isCancelled = false;
    
    /**
     * Interface for progress updates.
     */
    public interface ProgressListener {
        void onBatchLoaded(List<UserROI> batch, int totalLoaded, int totalExpected);
        void onLoadingComplete(int totalLoaded);
        void onLoadingFailed(String error);
    }
    
    /**
     * Load ROIs progressively from ZIP file.
     */
    public CompletableFuture<Void> loadROIsProgressively(File zipFile, String imageFileName) {
        if (zipFile == null || !zipFile.exists() || imageFileName == null) {
            notifyError("Invalid file or image name");
            return CompletableFuture.completedFuture(null);
        }
        
        isLoading = true;
        isCancelled = false;
        
        LOGGER.info("Starting progressive loading from: {}", zipFile.getName());
        
        return CompletableFuture.runAsync(() -> {
            try {
                loadROIsFromZipFile(zipFile, imageFileName);
            } catch (Exception e) {
                LOGGER.error("Progressive loading failed", e);
                SwingUtilities.invokeLater(() -> notifyError(e.getMessage()));
            } finally {
                isLoading = false;
            }
        });
    }
    
    /**
     * Cancel ongoing loading operation.
     */
    public void cancelLoading() {
        isCancelled = true;
        isLoading = false;
    }
    
    /**
     * Check if currently loading.
     */
    public boolean isLoading() {
        return isLoading;
    }
    
    /**
     * Add progress listener.
     */
    public void addProgressListener(ProgressListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remove progress listener.
     */
    public void removeProgressListener(ProgressListener listener) {
        listeners.remove(listener);
    }
    
    // === PRIVATE METHODS ===
    
    private void loadROIsFromZipFile(File zipFile, String imageFileName) throws IOException {
        List<UserROI> allROIs = new ArrayList<>();
        
        // Convert image filename to expected ZIP format
        String expectedZipName = convertImageNameToZipName(imageFileName);
        LOGGER.debug("Looking for nested ZIP: {} for image: {}", expectedZipName, imageFileName);
        
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile), 65536))) {
            ZipEntry entry;
            
            while ((entry = zis.getNextEntry()) != null && !isCancelled) {
                if (entry.getName().equals(expectedZipName) && !entry.isDirectory()) {
                    LOGGER.info("Found matching nested ZIP: {}", entry.getName());
                    
                    // Load ROIs from nested ZIP with progressive updates
                    loadROIsFromNestedZipProgressive(zis, imageFileName, allROIs);
                    break;
                }
                zis.closeEntry();
            }
        }
        
        if (!isCancelled) {
            SwingUtilities.invokeLater(() -> notifyComplete(allROIs.size()));
        }
    }
    
    private void loadROIsFromNestedZipProgressive(ZipInputStream parentZis, String imageFileName, 
                                                 List<UserROI> allROIs) throws IOException {
        
        // Read nested ZIP data
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = parentZis.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        
        // Process nested ZIP with progressive updates
        try (ZipInputStream nestedZis = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            ZipEntry nestedEntry;
            List<UserROI> currentBatch = new ArrayList<>();
            int totalProcessed = 0;
            
            while ((nestedEntry = nestedZis.getNextEntry()) != null && !isCancelled) {
                if (nestedEntry.getName().endsWith(".roi")) {
                    try {
                        UserROI roi = loadROIFromZipEntry(nestedZis, nestedEntry.getName(), imageFileName);
                        if (roi != null && shouldIncludeROI(roi)) {
                            currentBatch.add(roi);
                            allROIs.add(roi);
                            totalProcessed++;
                            
                            // Send batch when it reaches size limit
                            if (currentBatch.size() >= BATCH_SIZE) {
                                sendBatchUpdate(new ArrayList<>(currentBatch), totalProcessed, -1);
                                currentBatch.clear();
                                
                                // Small delay to allow UI updates
                                try {
                                    Thread.sleep(BATCH_DELAY_MS);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to load ROI '{}': {}", nestedEntry.getName(), e.getMessage());
                    }
                }
                nestedZis.closeEntry();
            }
            
            // Send final batch if not empty
            if (!currentBatch.isEmpty() && !isCancelled) {
                sendBatchUpdate(new ArrayList<>(currentBatch), totalProcessed, totalProcessed);
            }
        }
        
        LOGGER.info("Progressive loading complete: {} ROIs processed", allROIs.size());
    }
    
    private UserROI loadROIFromZipEntry(ZipInputStream zis, String entryName, String imageFileName) throws IOException {
        String roiName = cleanROIName(entryName);
        
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
    
    private boolean shouldIncludeROI(UserROI roi) {
        if (roi == null) {
            return false;
        }
        
        // Filter for cells and nuclei only for performance
        String name = roi.getName().toLowerCase();
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
        
        // Remove numerical prefixes
        if (name.matches("\\d+-.*")) {
            int dashIndex = name.indexOf('-');
            if (dashIndex > 0) {
                name = name.substring(dashIndex + 1);
            }
        }
        
        return name.isEmpty() ? "ROI_" + System.currentTimeMillis() % 10000 : name;
    }
    
    private String convertImageNameToZipName(String imageFileName) {
        if (imageFileName == null) {
            return null;
        }
        
        String nameWithoutExt = imageFileName;
        int lastDot = imageFileName.lastIndexOf('.');
        if (lastDot > 0) {
            nameWithoutExt = imageFileName.substring(0, lastDot);
        }
        
        return nameWithoutExt.replace(' ', '_') + "_ROIs.zip";
    }
    
    private void sendBatchUpdate(List<UserROI> batch, int totalLoaded, int totalExpected) {
        SwingUtilities.invokeLater(() -> {
            for (ProgressListener listener : listeners) {
                try {
                    listener.onBatchLoaded(batch, totalLoaded, totalExpected);
                } catch (Exception e) {
                    LOGGER.error("Error notifying progress listener", e);
                }
            }
        });
    }
    
    private void notifyComplete(int totalLoaded) {
        for (ProgressListener listener : listeners) {
            try {
                listener.onLoadingComplete(totalLoaded);
            } catch (Exception e) {
                LOGGER.error("Error notifying completion listener", e);
            }
        }
    }
    
    private void notifyError(String error) {
        for (ProgressListener listener : listeners) {
            try {
                listener.onLoadingFailed(error);
            } catch (Exception e) {
                LOGGER.error("Error notifying error listener", e);
            }
        }
    }
}