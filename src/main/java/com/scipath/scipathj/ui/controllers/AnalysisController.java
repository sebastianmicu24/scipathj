package com.scipath.scipathj.ui.controllers;

import com.scipath.scipathj.analysis.SimpleHENuclearSegmentation;
import com.scipath.scipathj.analysis.VesselSegmentation;
import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.config.NuclearSegmentationSettings;
import com.scipath.scipathj.core.engine.SciPathJEngine;
import com.scipath.scipathj.data.model.NucleusROI;
import com.scipath.scipathj.data.model.UserROI;
import com.scipath.scipathj.ui.components.StatusPanel;
import com.scipath.scipathj.ui.model.PipelineInfo;
import com.scipath.scipathj.ui.utils.ImageLoader;
import ij.ImagePlus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Controller for managing analysis operations.
 *
 * <p>This class handles the start/stop analysis functionality, progress tracking,
 * and coordination between the UI and the analysis engine.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class AnalysisController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisController.class);
    
    private final SciPathJEngine engine;
    private final StatusPanel statusPanel;
    private final Component parentComponent;
    
    private JButton startButton;
    private JButton stopButton;
    private SwingWorker<Void, String> currentAnalysisWorker;
    
    // Analysis parameters
    private PipelineInfo currentPipeline;
    private File currentFolder;
    private int currentImageCount;
    
    /**
     * Creates a new AnalysisController instance.
     *
     * @param engine the SciPathJ engine
     * @param statusPanel the status panel for progress updates
     * @param parentComponent the parent component for dialogs
     */
    public AnalysisController(SciPathJEngine engine, StatusPanel statusPanel, Component parentComponent) {
        this.engine = engine;
        this.statusPanel = statusPanel;
        this.parentComponent = parentComponent;
        
        LOGGER.debug("Analysis controller created");
    }
    
    /**
     * Sets the start and stop buttons.
     *
     * @param startButton the start analysis button
     * @param stopButton the stop analysis button
     */
    public void setControlButtons(JButton startButton, JButton stopButton) {
        this.startButton = startButton;
        this.stopButton = stopButton;
        
        // Set up event handlers
        startButton.addActionListener(e -> startAnalysis());
        stopButton.addActionListener(e -> stopAnalysis());
        
        LOGGER.debug("Control buttons configured");
    }
    
    /**
     * Starts the analysis process.
     *
     * @param selectedPipeline the selected pipeline
     * @param selectedFolder the selected folder
     * @param imageCount the number of images to process
     */
    public void startAnalysis(PipelineInfo selectedPipeline, File selectedFolder, int imageCount) {
        LOGGER.info("Analysis start requested for pipeline: {} with folder: {} ({} images)",
                   selectedPipeline.getDisplayName(), selectedFolder.getAbsolutePath(), imageCount);
        
        // Store analysis parameters
        this.currentPipeline = selectedPipeline;
        this.currentFolder = selectedFolder;
        this.currentImageCount = imageCount;
        
        // Show progress bar
        statusPanel.showProgress(0, "Starting analysis...");
        
        // Start combined vessel and nucleus segmentation analysis
        performCombinedSegmentationAnalysis();
    }
    
    /**
     * Starts the analysis process (called by button).
     */
    private void startAnalysis() {
        // This method will be called by the button, but the actual logic
        // should be handled by the main window which has access to the selections
        LOGGER.debug("Start analysis button clicked");
    }
    
    /**
     * Stops the analysis process.
     */
    public void stopAnalysis() {
        LOGGER.info("Analysis stop requested");
        
        if (currentAnalysisWorker != null && !currentAnalysisWorker.isDone()) {
            currentAnalysisWorker.cancel(true);
        }
        
        if (startButton != null) {
            startButton.setEnabled(true);
        }
        if (stopButton != null) {
            stopButton.setEnabled(false);
        }
        
        statusPanel.setProgress(0);
        statusPanel.setProgressMessage("Stopped");
        statusPanel.hideProgress();
        statusPanel.setStatus("Analysis stopped");
    }
    
    /**
     * Performs combined vessel and nucleus segmentation analysis on all images in the selected folder.
     * For each image, vessels are segmented first, followed by nuclei segmentation.
     */
    private void performCombinedSegmentationAnalysis() {
        if (startButton != null) {
            startButton.setEnabled(false);
        }
        if (stopButton != null) {
            stopButton.setEnabled(true);
        }
        
        currentAnalysisWorker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // Get all image files from the folder
                    File[] imageFiles = getImageFiles(currentFolder);
                    
                    if (imageFiles.length == 0) {
                        publish("No supported image files found in the selected folder.");
                        return null;
                    }
                    
                    LOGGER.info("Starting combined segmentation (vessels + nuclei) for {} images", imageFiles.length);
                    
                    int totalVessels = 0;
                    int totalNuclei = 0;
                    
                    for (int i = 0; i < imageFiles.length; i++) {
                        if (isCancelled()) break;
                        
                        File imageFile = imageFiles[i];
                        String fileName = imageFile.getName();
                        
                        // Update progress for current image
                        int baseProgress = (int) ((double) i / imageFiles.length * 100);
                        statusPanel.setProgress(baseProgress);
                        publish("Processing image " + (i + 1) + "/" + imageFiles.length + ": " + fileName);
                        
                        try {
                            // Load the image
                            ImagePlus imagePlus = ImageLoader.loadImage(imageFile.getAbsolutePath());
                            if (imagePlus == null) {
                                LOGGER.warn("Could not load image: {}", fileName);
                                continue;
                            }
                            
                            // Step 1: Perform vessel segmentation
                            publish("Segmenting vessels in " + fileName + "...");
                            VesselSegmentation vesselSeg = new VesselSegmentation(imagePlus, fileName);
                            List<UserROI> vesselROIs = vesselSeg.segmentVessels();
                            totalVessels += vesselROIs.size();
                            LOGGER.info("Found {} vessels in image: {}", vesselROIs.size(), fileName);
                            
                            // Update progress (vessel segmentation complete for this image)
                            int vesselProgress = baseProgress + (int) ((double) 1 / imageFiles.length * 50);
                            statusPanel.setProgress(vesselProgress);
                                                        // Step 2: Perform nucleus segmentation using simplified H&E approach with current settings
                                                        publish("Segmenting nuclei in " + fileName + " (H&E optimized)...");
                                                        NuclearSegmentationSettings nuclearSettings = ConfigurationManager.getInstance().initializeNuclearSegmentationSettings();
                                                        SimpleHENuclearSegmentation nucleusSeg = new SimpleHENuclearSegmentation(imagePlus, fileName, nuclearSettings);
                            
                            int nucleiCount = 0;
                            try {
                                if (nucleusSeg.isAvailable()) {
                                    List<NucleusROI> nucleusROIs = nucleusSeg.segmentNuclei();
                                    nucleiCount = nucleusROIs.size();
                                    totalNuclei += nucleiCount;
                                    LOGGER.info("Found {} nuclei in image: {} using H&E model", nucleiCount, fileName);
                                } else {
                                    LOGGER.warn("StarDist H&E model not available for image: {}", fileName);
                                    publish("StarDist H&E model not available for " + fileName);
                                }
                                
                                // Clean up segmentation resources
                                nucleusSeg.close();
                                
                            } catch (Exception e) {
                                LOGGER.error("H&E nucleus segmentation failed for image: {}", fileName, e);
                                publish("H&E nucleus segmentation failed for " + fileName + ": " + e.getMessage());
                                // Continue with next image instead of failing completely
                            }
                            
                            
                            // Update progress (both segmentations complete for this image)
                            int completeProgress = baseProgress + (int) ((double) 1 / imageFiles.length * 100);
                            statusPanel.setProgress(completeProgress);
                            
                            publish("Completed segmentation for " + fileName +
                                   " - Vessels: " + vesselROIs.size() + ", Nuclei: " + nucleiCount);
                            
                            // Clean up
                            imagePlus.close();
                            
                        } catch (Exception e) {
                            LOGGER.error("Error processing image: {}", fileName, e);
                            publish("Error processing " + fileName + ": " + e.getMessage());
                        }
                        
                        // Small delay to show progress
                        Thread.sleep(200);
                    }
                    
                    // Final update
                    statusPanel.setProgress(100);
                    publish("Combined segmentation completed! Found " + totalVessels + " vessels and " +
                           totalNuclei + " nuclei across " + imageFiles.length + " images.");
                    
                } catch (Exception e) {
                    LOGGER.error("Error during combined segmentation analysis", e);
                    publish("Analysis failed: " + e.getMessage());
                }
                
                return null;
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                String latestMessage = chunks.get(chunks.size() - 1);
                statusPanel.setProgressMessage(latestMessage);
                statusPanel.setStatus("Combined Segmentation: " + latestMessage);
            }
            
            @Override
            protected void done() {
                if (startButton != null) {
                    startButton.setEnabled(true);
                }
                if (stopButton != null) {
                    stopButton.setEnabled(false);
                }
                
                try {
                    get(); // Check for exceptions
                    statusPanel.setProgress(100);
                    statusPanel.setProgressMessage("Analysis Complete");
                    statusPanel.setStatus("Combined segmentation analysis completed successfully");
                    
                    // Show completion dialog
                    JOptionPane.showMessageDialog(
                        parentComponent,
                        "Combined segmentation analysis completed!\n\n" +
                        "Pipeline: " + currentPipeline.getDisplayName() + "\n" +
                        "Images processed: " + currentImageCount + "\n" +
                        "Check the ROI toolbar to see detected vessels and nuclei.",
                        "Analysis Complete",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    
                } catch (Exception e) {
                    LOGGER.error("Analysis failed", e);
                    statusPanel.setProgressMessage("Analysis Failed");
                    statusPanel.setStatus("Analysis failed: " + e.getMessage());
                    
                    JOptionPane.showMessageDialog(
                        parentComponent,
                        "Analysis failed:\n" + e.getMessage(),
                        "Analysis Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
                
                // Hide progress after a delay
                Timer timer = new Timer(3000, e -> statusPanel.hideProgress());
                timer.setRepeats(false);
                timer.start();
                
                LOGGER.info("Combined segmentation analysis completed");
            }
        };
        
        currentAnalysisWorker.execute();
    }
    
    /**
     * Gets all supported image files from the specified folder.
     */
    private File[] getImageFiles(File folder) {
        return folder.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                   lowerName.endsWith(".png") || lowerName.endsWith(".tif") ||
                   lowerName.endsWith(".tiff") || lowerName.endsWith(".bmp") ||
                   lowerName.endsWith(".gif") || lowerName.endsWith(".lsm") ||
                   lowerName.endsWith(".czi") || lowerName.endsWith(".nd2") ||
                   lowerName.endsWith(".oib") || lowerName.endsWith(".oif") ||
                   lowerName.endsWith(".vsi") || lowerName.endsWith(".ims") ||
                   lowerName.endsWith(".lif") || lowerName.endsWith(".scn") ||
                   lowerName.endsWith(".svs") || lowerName.endsWith(".ndpi");
        });
    }
    
    /**
     * Updates the start button state based on current conditions.
     *
     * @param canStart whether analysis can be started
     */
    public void updateStartButtonState(boolean canStart) {
        if (startButton != null) {
            startButton.setEnabled(canStart);
        }
    }
    
    /**
     * Checks if analysis is currently running.
     *
     * @return true if analysis is running, false otherwise
     */
    public boolean isAnalysisRunning() {
        return currentAnalysisWorker != null && !currentAnalysisWorker.isDone();
    }
    
    /**
     * Gets the start button.
     *
     * @return the start button
     */
    public JButton getStartButton() {
        return startButton;
    }
    
    /**
     * Gets the stop button.
     *
     * @return the stop button
     */
    public JButton getStopButton() {
        return stopButton;
    }
}