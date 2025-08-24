package com.scipath.scipathj.ui.controllers;

import java.awt.*;
import java.io.File;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for UI state transitions and navigation.
 *
 * <p>This class handles the logic for switching between different UI states
 * and managing the associated UI updates, keeping the navigation logic separate
 * from component management.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class UIStateManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(UIStateManager.class);

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

  private final CardLayout cardLayout;
  private final JPanel mainContentPanel;

  private UIState currentState = UIState.MAIN_MENU;
  private Runnable startButtonStateUpdater;

  /**
   * Creates a new UIStateManager instance.
   *
   * @param cardLayout the card layout for switching panels
   * @param mainContentPanel the main content panel
   */
  public UIStateManager(CardLayout cardLayout, JPanel mainContentPanel) {
    this.cardLayout = cardLayout;
    this.mainContentPanel = mainContentPanel;
    LOGGER.debug("UI state manager created");
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
   * Switches to the specified UI state.
   *
   * @param state the UI state to switch to
   */
  public void switchToState(UIState state) {
    this.currentState = state;
    cardLayout.show(mainContentPanel, state.name());
    updateStartButtonState();
    LOGGER.info("Switched to UI state: {}", state);
  }

  /**
   * Switches to the main menu state.
   */
  public void switchToMainMenu() {
    switchToState(UIState.MAIN_MENU);
  }

  /**
   * Switches to the folder selection state.
   */
  public void switchToFolderSelection() {
    switchToState(UIState.FOLDER_SELECTION);
  }

  /**
   * Switches to the image gallery state.
   */
  public void switchToImageGallery() {
    switchToState(UIState.IMAGE_GALLERY);
  }

  /**
   * Switches to the dataset creation state.
   */
  public void switchToDatasetCreation() {
    switchToState(UIState.DATASET_CREATION);
  }

  /**
   * Switches to the results visualization state.
   */
  public void switchToResultsVisualization() {
    switchToState(UIState.RESULTS_VISUALIZATION);
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
   * Checks if the current state supports analysis operations.
   *
   * @return true if analysis can be started in the current state
   */
  public boolean isAnalysisState() {
    return currentState == UIState.FOLDER_SELECTION || currentState == UIState.IMAGE_GALLERY;
  }

  /**
   * Updates the start button state based on current state.
   */
  private void updateStartButtonState() {
    if (startButtonStateUpdater != null) {
      startButtonStateUpdater.run();
    }
  }
}