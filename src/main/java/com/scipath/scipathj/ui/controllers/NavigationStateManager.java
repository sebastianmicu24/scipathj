package com.scipath.scipathj.ui.controllers;

import java.awt.*;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages UI state transitions and navigation state.
 *
 * <p>This class handles the pure state management aspects of navigation,
 * keeping track of current UI state, selected items, and navigation history
 * without handling the actual UI transitions.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class NavigationStateManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(NavigationStateManager.class);

  /**
   * UI State enumeration.
   */
  public enum UIState {
    MAIN_MENU,
    FOLDER_SELECTION,
    IMAGE_GALLERY,
    DATASET_CREATION,
    RESULTS_VISUALIZATION
  }

  // Current state
  private UIState currentState = UIState.MAIN_MENU;
  private UIState previousState = null;

  // Selection state
  private com.scipath.scipathj.ui.model.PipelineInfo selectedPipeline;
  private File selectedFolder;
  private File selectedFile;

  // Navigation history for back button functionality
  private java.util.Stack<UIState> navigationHistory = new java.util.Stack<>();

  /**
   * Creates a new NavigationStateManager instance.
   */
  public NavigationStateManager() {
    LOGGER.debug("Navigation state manager created");
  }

  /**
   * Transitions to a new UI state.
   *
   * @param newState the state to transition to
   * @return true if the transition was successful, false otherwise
   */
  public boolean transitionToState(UIState newState) {
    if (newState == currentState) {
      LOGGER.debug("Already in state: {}", newState);
      return false;
    }

    // Save current state to history for back navigation
    if (currentState != UIState.MAIN_MENU) {
      navigationHistory.push(currentState);
    }

    previousState = currentState;
    currentState = newState;

    LOGGER.info("Transitioned from {} to {}", previousState, newState);
    return true;
  }

  /**
   * Goes back to the previous state.
   *
   * @return the previous state, or null if no previous state exists
   */
  public UIState goBack() {
    if (!navigationHistory.isEmpty()) {
      UIState previous = navigationHistory.pop();
      previousState = currentState;
      currentState = previous;
      LOGGER.info("Went back from {} to {}", previousState, currentState);
      return currentState;
    }

    // If no history, go to main menu
    if (currentState != UIState.MAIN_MENU) {
      previousState = currentState;
      currentState = UIState.MAIN_MENU;
      LOGGER.info("No history available, went to main menu from {}", previousState);
      return currentState;
    }

    return null;
  }

  /**
   * Checks if we can go back to a previous state.
   *
   * @return true if back navigation is possible, false otherwise
   */
  public boolean canGoBack() {
    return !navigationHistory.isEmpty() || currentState != UIState.MAIN_MENU;
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
   * Gets the previous UI state.
   *
   * @return the previous UI state, or null if none exists
   */
  public UIState getPreviousState() {
    return previousState;
  }

  /**
   * Checks if the current state supports analysis operations.
   *
   * @return true if analysis can be started in the current state
   */
  public boolean isAnalysisState() {
    return currentState == UIState.FOLDER_SELECTION || currentState == UIState.IMAGE_GALLERY;
  }

  /**
   * Checks if the current state requires folder selection.
   *
   * @return true if folder selection is needed
   */
  public boolean needsFolderSelection() {
    return currentState == UIState.FOLDER_SELECTION;
  }

  /**
   * Checks if the current state allows image gallery operations.
   *
   * @return true if image gallery operations are available
   */
  public boolean canShowImageGallery() {
    return currentState == UIState.IMAGE_GALLERY;
  }

  /**
   * Sets the selected pipeline.
   *
   * @param pipeline the selected pipeline
   */
  public void setSelectedPipeline(com.scipath.scipathj.ui.model.PipelineInfo pipeline) {
    this.selectedPipeline = pipeline;
    LOGGER.debug("Selected pipeline: {}", pipeline != null ? pipeline.getDisplayName() : "null");
  }

  /**
   * Gets the selected pipeline.
   *
   * @return the selected pipeline
   */
  public com.scipath.scipathj.ui.model.PipelineInfo getSelectedPipeline() {
    return selectedPipeline;
  }

  /**
   * Sets the selected folder.
   *
   * @param folder the selected folder
   */
  public void setSelectedFolder(File folder) {
    this.selectedFolder = folder;
    LOGGER.debug("Selected folder: {}", folder != null ? folder.getAbsolutePath() : "null");
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
   * Sets the selected file.
   *
   * @param file the selected file
   */
  public void setSelectedFile(File file) {
    this.selectedFile = file;
    LOGGER.debug("Selected file: {}", file != null ? file.getName() : "null");
  }

  /**
   * Gets the selected file.
   *
   * @return the selected file
   */
  public File getSelectedFile() {
    return selectedFile;
  }

  /**
   * Checks if a pipeline has been selected.
   *
   * @return true if a pipeline is selected, false otherwise
   */
  public boolean hasSelectedPipeline() {
    return selectedPipeline != null;
  }

  /**
   * Checks if a folder has been selected.
   *
   * @return true if a folder is selected, false otherwise
   */
  public boolean hasSelectedFolder() {
    return selectedFolder != null;
  }

  /**
   * Checks if a file has been selected.
   *
   * @return true if a file is selected, false otherwise
   */
  public boolean hasSelectedFile() {
    return selectedFile != null;
  }

  /**
   * Clears all selections and resets to initial state.
   */
  public void clearSelections() {
    selectedPipeline = null;
    selectedFolder = null;
    selectedFile = null;
    navigationHistory.clear();
    LOGGER.debug("Cleared all selections and navigation history");
  }

  /**
   * Gets the navigation history size.
   *
   * @return the number of states in navigation history
   */
  public int getHistorySize() {
    return navigationHistory.size();
  }
}