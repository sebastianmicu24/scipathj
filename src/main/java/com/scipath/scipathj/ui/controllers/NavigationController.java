package com.scipath.scipathj.ui.controllers;

import com.scipath.scipathj.ui.components.FolderSelectionPanel;
import com.scipath.scipathj.ui.components.MainImageViewer;
import com.scipath.scipathj.ui.components.PipelineRecapPanel;
import com.scipath.scipathj.ui.components.SimpleImageGallery;
import com.scipath.scipathj.ui.components.StatusPanel;
import com.scipath.scipathj.ui.model.PipelineInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Controller for managing navigation between different UI states.
 *
 * <p>This class handles the navigation logic between pipeline selection,
 * folder selection, and image gallery views, managing the card layout
 * and updating UI components accordingly.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class NavigationController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NavigationController.class);
    
    /**
     * UI State enumeration.
     */
    public enum UIState {
        PIPELINE_SELECTION,
        FOLDER_SELECTION,
        IMAGE_GALLERY
    }
    
    private final CardLayout cardLayout;
    private final JPanel mainContentPanel;
    private final StatusPanel statusPanel;
    private final PipelineRecapPanel pipelineRecapPanel;
    private final FolderSelectionPanel folderSelectionPanel;
    private final SimpleImageGallery imageGallery;
    private final MainImageViewer mainImageViewer;
    
    private UIState currentState = UIState.PIPELINE_SELECTION;
    private PipelineInfo selectedPipeline;
    private File selectedFolder;
    
    // Callback for updating start button state
    private Runnable startButtonStateUpdater;
    
    /**
     * Creates a new NavigationController instance.
     *
     * @param cardLayout the card layout for switching panels
     * @param mainContentPanel the main content panel
     * @param statusPanel the status panel
     * @param pipelineRecapPanel the pipeline recap panel
     * @param folderSelectionPanel the folder selection panel
     * @param imageGallery the image gallery
     * @param mainImageViewer the main image viewer
     */
    public NavigationController(CardLayout cardLayout, JPanel mainContentPanel,
                              StatusPanel statusPanel, PipelineRecapPanel pipelineRecapPanel,
                              FolderSelectionPanel folderSelectionPanel, SimpleImageGallery imageGallery,
                              MainImageViewer mainImageViewer) {
        this.cardLayout = cardLayout;
        this.mainContentPanel = mainContentPanel;
        this.statusPanel = statusPanel;
        this.pipelineRecapPanel = pipelineRecapPanel;
        this.folderSelectionPanel = folderSelectionPanel;
        this.imageGallery = imageGallery;
        this.mainImageViewer = mainImageViewer;
        
        LOGGER.debug("Navigation controller created");
    }
    
    /**
     * Sets the callback for updating start button state.
     *
     * @param startButtonStateUpdater the callback to update start button state
     */
    public void setStartButtonStateUpdater(Runnable startButtonStateUpdater) {
        this.startButtonStateUpdater = startButtonStateUpdater;
    }
    
    /**
     * Switches to the analysis setup screen.
     *
     * @param pipeline the selected pipeline
     */
    public void switchToAnalysisSetup(PipelineInfo pipeline) {
        this.selectedPipeline = pipeline;
        currentState = UIState.FOLDER_SELECTION;
        cardLayout.show(mainContentPanel, UIState.FOLDER_SELECTION.name());
        
        // Update pipeline recap
        pipelineRecapPanel.setPipeline(selectedPipeline);
        
        // Show back button
        statusPanel.showBackButton();
        
        // Update status
        statusPanel.setStatus("Select a folder containing images");
        
        // Update start button state
        updateStartButtonState();
        
        LOGGER.info("Switched to analysis setup for pipeline: {}", pipeline.getDisplayName());
    }
    
    /**
     * Switches to the image gallery view.
     *
     * @param folder the selected folder
     */
    public void switchToImageGallery(File folder) {
        this.selectedFolder = folder;
        currentState = UIState.IMAGE_GALLERY;
        cardLayout.show(mainContentPanel, UIState.IMAGE_GALLERY.name());
        
        // Load images into gallery
        imageGallery.loadImagesFromFolder(selectedFolder);
        
        // Update status
        statusPanel.setStatus("Select images for analysis");
        
        // Update start button state
        updateStartButtonState();
        
        LOGGER.info("Switched to image gallery view with folder: {}", selectedFolder.getAbsolutePath());
    }
    
    /**
     * Switches back to folder selection from image gallery.
     */
    public void switchToFolderSelection() {
        currentState = UIState.FOLDER_SELECTION;
        cardLayout.show(mainContentPanel, UIState.FOLDER_SELECTION.name());
        
        // Clear image viewer
        mainImageViewer.clearImage();
        
        // Update status
        statusPanel.setStatus("Select a folder containing images");
        
        // Update start button state
        updateStartButtonState();
        
        LOGGER.info("Switched back to folder selection");
    }
    
    /**
     * Switches back to the pipeline selection screen.
     */
    public void switchToPipelineSelection() {
        currentState = UIState.PIPELINE_SELECTION;
        cardLayout.show(mainContentPanel, UIState.PIPELINE_SELECTION.name());
        
        // Hide back button
        statusPanel.hideBackButton();
        
        // Update status
        statusPanel.setStatus("Select a pipeline to begin");
        
        // Clear selections
        selectedPipeline = null;
        selectedFolder = null;
        folderSelectionPanel.clearSelection();
        mainImageViewer.clearImage();
        
        // Update start button state
        updateStartButtonState();
        
        LOGGER.info("Switched back to pipeline selection");
    }
    
    /**
     * Updates the start button state based on current selections.
     */
    private void updateStartButtonState() {
        if (startButtonStateUpdater != null) {
            startButtonStateUpdater.run();
        }
    }
    
    /**
     * Checks if analysis can be started based on current state and selections.
     *
     * @return true if analysis can be started, false otherwise
     */
    public boolean canStartAnalysis() {
        return (currentState == UIState.FOLDER_SELECTION || currentState == UIState.IMAGE_GALLERY) && 
               selectedPipeline != null && 
               selectedFolder != null &&
               folderSelectionPanel.hasSelection();
    }
    
    /**
     * Gets the current UI state.
     *
     * @return the current UI state
     */
    public UIState getCurrentState() {
        return currentState;
    }
    
    /**
     * Gets the selected pipeline.
     *
     * @return the selected pipeline
     */
    public PipelineInfo getSelectedPipeline() {
        return selectedPipeline;
    }
    
    /**
     * Gets the selected folder.
     *
     * @return the selected folder
     */
    public File getSelectedFolder() {
        return selectedFolder;
    }
    
    /**
     * Sets the selected folder.
     *
     * @param folder the selected folder
     */
    public void setSelectedFolder(File folder) {
        this.selectedFolder = folder;
        updateStartButtonState();
    }
    
    /**
     * Gets the image count for analysis.
     *
     * @return the number of images available for analysis
     */
    public int getImageCount() {
        return currentState == UIState.IMAGE_GALLERY ? imageGallery.getImageCount() : 0;
    }
}