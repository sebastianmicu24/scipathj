package com.scipath.scipathj.ui.main;

import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.infrastructure.config.MainSettings;
import com.scipath.scipathj.infrastructure.engine.SciPathJEngine;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.ui.analysis.components.FolderSelectionPanel;
import com.scipath.scipathj.ui.common.MainImageViewer;
import com.scipath.scipathj.ui.common.components.MainMenuPanel;
import com.scipath.scipathj.ui.common.components.MenuBarManager;
import com.scipath.scipathj.ui.analysis.components.PipelineRecapPanel;
import com.scipath.scipathj.ui.common.ROIManager;
import com.scipath.scipathj.ui.analysis.components.ROIToolbar;
import com.scipath.scipathj.ui.common.SimpleImageGallery;
import com.scipath.scipathj.ui.common.StatusPanel;
import com.scipath.scipathj.ui.controllers.AnalysisController;
import com.scipath.scipathj.ui.controllers.NavigationController;
import com.scipath.scipathj.ui.dataset.DatasetMainPanel;
import com.scipath.scipathj.ui.analysis.dialogs.ROIStatisticsDialog;
import com.scipath.scipathj.ui.visualization.ResultsVisualizationPanel;
import com.scipath.scipathj.ui.common.dialogs.settings.DisplaySettingsDialog;
import com.scipath.scipathj.ui.common.dialogs.settings.MainSettingsDialog;
import com.scipath.scipathj.ui.model.PipelineInfo;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application window for SciPathJ.
 *
 * <p>This class provides the primary user interface for the SciPathJ application,
 * including pipeline selection, configuration, and processing controls.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class MainWindow extends JFrame {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainWindow.class);
  private final SciPathJEngine engine;
  private final ConfigurationManager configurationManager;

  // UI Components
  private CardLayout cardLayout;
  private JPanel mainContentPanel;
  private MainMenuPanel mainMenuPanel;
  private DatasetMainPanel datasetCreationPanel;
  private ResultsVisualizationPanel resultsVisualizationPanel;
  private JPanel analysisSetupPanel;
  private PipelineRecapPanel pipelineRecapPanel;
  private FolderSelectionPanel folderSelectionPanel;
  private JPanel imageViewPanel;
  private SimpleImageGallery imageGallery;
  private MainImageViewer mainImageViewer;
  private ROIToolbar roiToolbar;
  private StatusPanel statusPanel;

  // ROI management
  private ROIManager roiManager;

  // Controllers and Managers
  private MenuBarManager menuBarManager;
  private NavigationController navigationController;
  private AnalysisController analysisController;

  // Window state management
  private boolean isFullScreen = false;
  private Rectangle normalBounds;
  private int normalExtendedState;

  /**
   * Creates a new MainWindow instance.
   *
   * @param engine the SciPathJ engine instance
   * @param configurationManager the configuration manager instance
   */
  public MainWindow(SciPathJEngine engine, ConfigurationManager configurationManager) {
    this.engine = engine;
    this.configurationManager = configurationManager;
    this.roiManager = ROIManager.getInstance();

    LOGGER.debug("Creating main window");
    initializeComponents();
    setupLayout();
    setupEventHandlers();
    setupROISystem();

    LOGGER.info("Main window created successfully");
  }

  /**
   * Initializes all UI components.
   */
  private void initializeComponents() {
    setupWindow();
    setupCardLayout();
    createMainPanels();
    createControllers();
  }

  /**
   * /**
   * Sets up the main window properties.
   */
  private void setupWindow() {
    setTitle("SciPathJ - Segmentation and Classification of Images");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(UIConstants.WINDOW_WIDTH, UIConstants.WINDOW_HEIGHT);
    setMinimumSize(new Dimension(800, 600)); // Set minimum size for usability
    setResizable(true); // Enable window resizing

    // Center the window on screen
    setLocationRelativeTo(null);

    // Set application icon
    try {
      ImageIcon icon = new ImageIcon(getClass().getResource("/icon.png"));
      setIconImage(icon.getImage());
    } catch (Exception e) {
      LOGGER.warn("Could not load application icon", e);
    }

    // Add keyboard shortcuts for fullscreen
    setupKeyboardShortcuts();
  }


  /**
   * Sets up the card layout system.
   */
  private void setupCardLayout() {
    cardLayout = new CardLayout();
    mainContentPanel = new JPanel(cardLayout);
  }

  /**
   * Creates the main panels and adds them to the card layout.
   */
  private void createMainPanels() {
    mainMenuPanel = new MainMenuPanel();
    datasetCreationPanel = new DatasetMainPanel(configurationManager.loadMainSettings());
    resultsVisualizationPanel = new ResultsVisualizationPanel();
    analysisSetupPanel = createAnalysisSetupPanel();
    imageViewPanel = createImageViewPanel();

    mainContentPanel.add(
        mainMenuPanel, NavigationController.UIState.MAIN_MENU.name());
    mainContentPanel.add(
        datasetCreationPanel, NavigationController.UIState.DATASET_CREATION.name());
    mainContentPanel.add(
        resultsVisualizationPanel, NavigationController.UIState.RESULTS_VISUALIZATION.name());
    mainContentPanel.add(analysisSetupPanel, NavigationController.UIState.FOLDER_SELECTION.name());
    mainContentPanel.add(imageViewPanel, NavigationController.UIState.IMAGE_GALLERY.name());
  }

  /**
   * Creates the analysis setup panel.
   *
   * @return analysis setup panel
   */
  private JPanel createAnalysisSetupPanel() {
    JPanel panel = new JPanel(new BorderLayout());

    // Pipeline recap at the top
    pipelineRecapPanel = new PipelineRecapPanel(configurationManager);
    panel.add(pipelineRecapPanel, BorderLayout.NORTH);

    // Folder selection in the center
    folderSelectionPanel = new FolderSelectionPanel();
    panel.add(folderSelectionPanel, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Creates the image view panel with gallery and main image viewer.
   *
   * @return image view panel
   */
  private JPanel createImageViewPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(createImageViewTopPanel(), BorderLayout.NORTH);
    panel.add(createImageViewMainContent(), BorderLayout.CENTER);

    // Add ROI toolbar directly to the panel (it will be above the status panel)
    roiToolbar = new ROIToolbar();

    // Initialize toolbar with current settings
    MainSettings currentSettings = configurationManager.loadMainSettings();
    if (currentSettings != null) {
      roiToolbar.setMainSettings(currentSettings);
      roiToolbar.updateROITypeCounts(getROICountsByType());
    }

    roiToolbar.setPreferredSize(new Dimension(roiToolbar.getPreferredSize().width, 90));
    panel.add(roiToolbar, BorderLayout.SOUTH);

    return panel;
  }

  /**
   * Creates the top panel for image view with pipeline recap and change folder button.
   */
  private JPanel createImageViewTopPanel() {
    JPanel topPanel = new JPanel(new BorderLayout());

    // Create button panel with both change folder and main settings buttons
    JButton changeFolderButton =
        UIUtils.createSmallButton("Change Folder", FontIcon.of(FontAwesomeSolid.FOLDER_OPEN, 14));
    changeFolderButton.setPreferredSize(new Dimension(180, 32));
    changeFolderButton.addActionListener(e -> navigationController.switchToFolderSelection());

    JButton mainSettingsButton =
        UIUtils.createSmallButton("Main Settings", FontIcon.of(FontAwesomeSolid.COG, 14));
    mainSettingsButton.setPreferredSize(new Dimension(180, 32));
    mainSettingsButton.addActionListener(this::openMainSettings);

    JButton displaySettingsButton =
        UIUtils.createSmallButton("Display Settings", FontIcon.of(FontAwesomeSolid.PALETTE, 14));
    displaySettingsButton.setPreferredSize(new Dimension(180, 32));
    displaySettingsButton.addActionListener(this::openDisplaySettings);

    JPanel buttonPanel =
        new JPanel(
            new FlowLayout(
                FlowLayout.RIGHT, UIConstants.MEDIUM_SPACING, UIConstants.SMALL_SPACING));
    buttonPanel.setBorder(
        UIUtils.createPadding(
            UIConstants.SMALL_SPACING,
            UIConstants.MEDIUM_SPACING,
            UIConstants.MEDIUM_SPACING,
            UIConstants.MEDIUM_SPACING));
    buttonPanel.add(displaySettingsButton);
    buttonPanel.add(mainSettingsButton);
    buttonPanel.add(changeFolderButton);
    topPanel.add(buttonPanel, BorderLayout.NORTH);

    // Add pipeline recap panel below the buttons with some spacing
    JPanel recapWrapper = new JPanel(new BorderLayout());
    recapWrapper.setBorder(UIUtils.createPadding(0, 0, UIConstants.SMALL_SPACING, 0));
    recapWrapper.add(pipelineRecapPanel, BorderLayout.CENTER);
    topPanel.add(recapWrapper, BorderLayout.CENTER);

    return topPanel;
  }

  /**
   * Creates the main content area with gallery and image viewer.
   */
  private JPanel createImageViewMainContent() {
    JPanel mainContent = new JPanel(new BorderLayout());

    imageGallery = new SimpleImageGallery();
    // Add simple border without title
    imageGallery.setBorder(BorderFactory.createEtchedBorder());
    mainContent.add(imageGallery, BorderLayout.WEST);

    mainImageViewer = new MainImageViewer();
    // Add simple border without title
    mainImageViewer.setBorder(BorderFactory.createEtchedBorder());
    mainContent.add(mainImageViewer, BorderLayout.CENTER);

    return mainContent;
  }

  /**
   * Creates a standard titled border.
   */
  private javax.swing.border.TitledBorder createTitledBorder(String title) {
    return BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(),
        title,
        javax.swing.border.TitledBorder.LEFT,
        javax.swing.border.TitledBorder.TOP,
        new Font(Font.SANS_SERIF, Font.BOLD, (int) UIConstants.TINY_FONT_SIZE));
  }


  /**
   * Creates the controllers and managers.
   */
  private void createControllers() {
    // Create status panel
    statusPanel = new StatusPanel();

    // Create menu bar manager
    menuBarManager = new MenuBarManager(this, configurationManager);

    // Create navigation controller
    navigationController =
        new NavigationController(
            cardLayout,
            mainContentPanel,
            statusPanel,
            pipelineRecapPanel,
            folderSelectionPanel,
            imageGallery,
            mainImageViewer);

    // Create analysis controller
    analysisController = new AnalysisController(engine, configurationManager, statusPanel, this);
    analysisController.setControlButtons(statusPanel.getStartButton(), statusPanel.getStopButton());

    // Set up navigation controller callback
    navigationController.setStartButtonStateUpdater(this::updateStartButtonState);

    // Set up status panel back button
    statusPanel.setBackButtonListener(e -> navigationController.switchToMainMenu());
  }

  /**
   * Sets up the main layout of the window.
   */
  private void setupLayout() {
    setLayout(new BorderLayout());

    // Add main content panel with card layout
    add(mainContentPanel, BorderLayout.CENTER);

    // Add status panel
    add(statusPanel, BorderLayout.SOUTH);

    // Set menu bar
    setJMenuBar(menuBarManager.getMenuBar());
  }

  /**
   * Sets up event handlers for UI components.
   */
  private void setupEventHandlers() {
    // Main menu selection handler
    mainMenuPanel.setOptionSelectedListener(
        e -> {
          PipelineInfo selectedPipeline = mainMenuPanel.getSelectedPipeline();
          if (selectedPipeline != null) {
            String pipelineId = selectedPipeline.getId();

            switch (pipelineId) {
              case "full_he":
                // Analysis workflow - go to folder selection
                navigationController.switchToAnalysisSetup(selectedPipeline);
                break;
              case "dataset_creator":
                // Dataset creation - go directly to dataset creation panel
                navigationController.switchToDatasetCreation();
                break;
              case "View_Results":
                // Results visualization - go directly to visualization panel
                navigationController.switchToResultsVisualization();
                break;
              default:
                LOGGER.warn("Unknown pipeline selected: {}", pipelineId);
                break;
            }
          }
        });

    // Folder selection handler
    folderSelectionPanel.setFolderChangeListener(
        e -> {
          File selectedFolder = folderSelectionPanel.getSelectedFolder();
          File selectedFile = folderSelectionPanel.getSelectedFile();

          if (selectedFolder != null && selectedFolder.isDirectory()) {
            navigationController.setSelectedFolder(selectedFolder);
            // Switch to image gallery view
            if (selectedFile != null && selectedFile.isFile()) {
              // Single file was selected - use parent folder but highlight the specific file
              navigationController.switchToImageGallery(selectedFolder, selectedFile);
            } else {
              // Folder was selected
              navigationController.switchToImageGallery(selectedFolder);
            }
          }

          updateStartButtonState();
        });

    // Image gallery selection handler
    imageGallery.setSelectionChangeListener(
        e -> {
          File selectedImageFile = imageGallery.getSelectedImageFile();
          if (selectedImageFile != null) {
            mainImageViewer.displayImage(selectedImageFile);
            LOGGER.debug("Selected image: {}", selectedImageFile.getName());
          }
        });

    // Setup start button action to use analysis controller
    statusPanel.getStartButton().addActionListener(
        e -> {
          PipelineInfo pipeline = navigationController.getSelectedPipeline();
          File folder = navigationController.getSelectedFolder();
          int imageCount = navigationController.getImageCount();

          if (pipeline != null && folder != null) {
            analysisController.startAnalysis(pipeline, folder, imageCount);
          }
        });
  }

  /**
   * Sets up the ROI system integration.
   */
  private void setupROISystem() {
    // Set up ROI toolbar listeners
    roiToolbar.addROIToolbarListener(
        new ROIToolbar.ROIToolbarListener() {
          @Override
          public void onROICreationModeChanged(UserROI.ROIType type, boolean enabled) {
            if (mainImageViewer != null) {
              mainImageViewer.setROICreationMode(type);
            }
            LOGGER.debug("ROI creation mode changed: {} (enabled: {})", type, enabled);
          }

          @Override
          public void onSaveROIs(String imageFileName, File outputFile) {
            try {
              roiManager.saveROIsToFile(imageFileName, outputFile);
              JOptionPane.showMessageDialog(
                  MainWindow.this,
                  "ROIs saved successfully to:\n" + outputFile.getAbsolutePath(),
                  "Save ROIs",
                  JOptionPane.INFORMATION_MESSAGE);
              LOGGER.info("Successfully saved ROIs to file: {}", outputFile.getAbsolutePath());
            } catch (IOException e) {
              LOGGER.error("Error saving ROIs to file: {}", outputFile.getAbsolutePath(), e);
              JOptionPane.showMessageDialog(
                  MainWindow.this,
                  "Error saving ROIs:\n" + e.getMessage(),
                  "Save ROIs Error",
                  JOptionPane.ERROR_MESSAGE);
            }
          }

          @Override
          public void onSaveAllROIs(File outputFile) {
            try {
              roiManager.saveAllROIsToMasterZip(outputFile);
              JOptionPane.showMessageDialog(
                  MainWindow.this,
                  "All ROIs saved successfully to master ZIP:\n" + outputFile.getAbsolutePath(),
                  "Save All ROIs",
                  JOptionPane.INFORMATION_MESSAGE);
              LOGGER.info(
                  "Successfully saved all ROIs to master ZIP file: {}",
                  outputFile.getAbsolutePath());
            } catch (IOException e) {
              LOGGER.error(
                  "Error saving all ROIs to master ZIP file: {}", outputFile.getAbsolutePath(), e);
              JOptionPane.showMessageDialog(
                  MainWindow.this,
                  "Error saving all ROIs:\n" + e.getMessage(),
                  "Save All ROIs Error",
                  JOptionPane.ERROR_MESSAGE);
            }
          }

          @Override
          public void onClearAllROIs() {
            String currentImageName = getCurrentImageName();
            if (currentImageName != null) {
              roiManager.clearROIsForImage(currentImageName);
              updateROIToolbarState();
              LOGGER.info("Cleared all ROIs for image: {}", currentImageName);
            }
          }

          @Override
          public void onROIFilterChanged(MainSettings.ROICategory category, boolean enabled) {
            LOGGER.debug("ROI filter changed: {} -> {}", category, enabled);
            // Update the ROI overlay to reflect filter changes
            if (mainImageViewer != null && mainImageViewer.getROIOverlay() != null) {
              // Update filter state and refresh the ROI display
              mainImageViewer.getROIOverlay().setFilterState(category, enabled);
              mainImageViewer.getROIOverlay().repaint();
            }
          }

          @Override
          public void onShowROIStatistics() {
            // Collect ROI data from all images
            java.util.Map<String, java.util.List<UserROI>> allROIs = roiManager.getAllROIsByImage();
            MainSettings currentSettings = configurationManager.loadMainSettings();

            // Show the statistics dialog
            ROIStatisticsDialog dialog = new ROIStatisticsDialog(MainWindow.this, allROIs, currentSettings);
            dialog.setVisible(true);

            LOGGER.info("Showing ROI statistics dialog with {} images", allROIs.size());
          }

          @Override
          public void onShowFeatures() {
            // Show the features dialog through the analysis controller
            analysisController.showFeaturesDialog();
            LOGGER.debug("Features dialog requested from ROI toolbar");
          }

          @Override
          public void onChangeROIType(String imageFileName, UserROI.ROIType newType) {
            LOGGER.debug("ROI type change requested for image {} to type {}", imageFileName, newType);
            // This method is called when ROI types need to be changed
            // Implementation can be added later if needed
          }
        });

    // Set up ROI manager listeners
    roiManager.addROIChangeListener(
        new ROIManager.ROIChangeListener() {
          @Override
          public void onROIAdded(UserROI roi) {
            updateROIToolbarState();
          }

          @Override
          public void onROIRemoved(UserROI roi) {
            updateROIToolbarState();
          }

          @Override
          public void onROIUpdated(UserROI roi) {
            updateROIToolbarState();
          }

          @Override
          public void onROIsCleared(String imageFileName) {
            updateROIToolbarState();
          }
        });

    // Update image gallery selection handler to update ROI toolbar
    imageGallery.setSelectionChangeListener(
        e -> {
          File selectedImageFile = imageGallery.getSelectedImageFile();
          if (selectedImageFile != null) {
            mainImageViewer.displayImage(selectedImageFile);
            roiToolbar.setCurrentImage(selectedImageFile.getName());
            updateROIToolbarState();
            LOGGER.debug("Selected image: {}", selectedImageFile.getName());
          }
        });
  }

  /**
   * Updates the ROI toolbar state based on current image and ROI count.
   */
  private void updateROIToolbarState() {
    String currentImageName = getCurrentImageName();
    if (currentImageName != null) {
      int totalCount = roiManager.getROICount(currentImageName);
      roiToolbar.updateROICount(totalCount);

      // Also update type counts
      roiToolbar.updateROITypeCounts(getROICountsByType());
    } else {
      roiToolbar.updateROICount(0);
    }
  }

  /**
   * Gets the current image name from the main image viewer.
   */
  private String getCurrentImageName() {
    if (mainImageViewer != null && mainImageViewer.getCurrentImageFile() != null) {
      return mainImageViewer.getCurrentImageFile().getName();
    }
    return null;
  }

  /**
   * Updates the start button state based on current selections.
   */
  private void updateStartButtonState() {
    boolean canStart = navigationController.canStartAnalysis();
    analysisController.updateStartButtonState(canStart);
  }

  /**
   * Sets up keyboard shortcuts for the application.
   */
  private void setupKeyboardShortcuts() {
    // F11 for fullscreen toggle
    KeyStroke f11KeyStroke = KeyStroke.getKeyStroke("F11");
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(f11KeyStroke, "toggleFullscreen");
    getRootPane()
        .getActionMap()
        .put(
            "toggleFullscreen",
            new AbstractAction() {
              @Override
              public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleFullscreen();
              }
            });

    // Alt+Enter for fullscreen toggle (alternative shortcut)
    KeyStroke altEnterKeyStroke = KeyStroke.getKeyStroke("alt ENTER");
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(altEnterKeyStroke, "toggleFullscreenAlt");
    getRootPane()
        .getActionMap()
        .put(
            "toggleFullscreenAlt",
            new AbstractAction() {
              @Override
              public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleFullscreen();
              }
            });

    LOGGER.debug("Keyboard shortcuts set up: F11 and Alt+Enter for fullscreen toggle");
  }

  /**
   * Toggles between fullscreen and windowed mode.
   */
  public void toggleFullscreen() {
    if (isFullScreen) {
      exitFullscreen();
    } else {
      enterFullscreen();
    }
  }

  /**
   * Enters fullscreen mode.
   */
  private void enterFullscreen() {
    if (isFullScreen) {
      return; // Already in fullscreen
    }

    // Store current window state
    normalBounds = getBounds();
    normalExtendedState = getExtendedState();

    // Hide menu bar and decorations
    dispose();
    setUndecorated(true);

    // Get the graphics device and enter fullscreen
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice gd = ge.getDefaultScreenDevice();

    if (gd.isFullScreenSupported()) {
      try {
        gd.setFullScreenWindow(this);
        isFullScreen = true;
        LOGGER.info("Entered fullscreen mode");
      } catch (Exception e) {
        LOGGER.error("Failed to enter fullscreen mode", e);
        // Fallback to maximized window
        setUndecorated(false);
        setVisible(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        isFullScreen = true;
      }
    } else {
      // Fallback for systems that don't support fullscreen
      setUndecorated(false);
      setVisible(true);
      setExtendedState(JFrame.MAXIMIZED_BOTH);
      isFullScreen = true;
      LOGGER.warn("Fullscreen not supported, using maximized window instead");
    }

    setVisible(true);

    // Update menu bar state
    if (menuBarManager != null) {
      menuBarManager.updateFullscreenMenuItem(isFullScreen);
    }
  }

  /**
   * Exits fullscreen mode and returns to windowed mode.
   */
  private void exitFullscreen() {
    if (!isFullScreen) {
      return; // Already in windowed mode
    }

    // Exit fullscreen
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice gd = ge.getDefaultScreenDevice();

    if (gd.getFullScreenWindow() == this) {
      gd.setFullScreenWindow(null);
    }

    // Restore window decorations and state
    dispose();
    setUndecorated(false);
    setVisible(true);

    // Restore previous bounds and state
    if (normalBounds != null) {
      setBounds(normalBounds);
      setExtendedState(normalExtendedState);
    } else {
      // Fallback to default size
      setSize(UIConstants.WINDOW_WIDTH, UIConstants.WINDOW_HEIGHT);
      setLocationRelativeTo(null);
    }

    isFullScreen = false;
    LOGGER.info("Exited fullscreen mode");

    // Update menu bar state
    if (menuBarManager != null) {
      menuBarManager.updateFullscreenMenuItem(isFullScreen);
    }
  }

  /**
   * Gets the current fullscreen state.
   *
   * @return true if the window is in fullscreen mode, false otherwise
   */
  public boolean isFullScreen() {
    return isFullScreen;
  }

  /**
   * Opens the main settings dialog.
   */
  private void openMainSettings(java.awt.event.ActionEvent e) {
    LOGGER.debug("Main settings dialog requested");
    MainSettingsDialog dialog = new MainSettingsDialog(this, configurationManager, this::handleSettingsChanged);
    dialog.setVisible(true);

    if (dialog.isSettingsChanged()) {
      LOGGER.info("Main settings were modified");
      // Optionally refresh UI components that depend on main settings
      refreshUIWithNewSettings();

      // Show confirmation message
      JOptionPane.showMessageDialog(
          this,
          "Main settings have been saved successfully.\nChanges will take effect immediately.",
          "Settings Saved",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

  private void openDisplaySettings(java.awt.event.ActionEvent e) {
    LOGGER.debug("Display settings dialog requested");
    MainSettings currentSettings = configurationManager.loadMainSettings();
    DisplaySettingsDialog dialog = new DisplaySettingsDialog(this, currentSettings, this::handleSettingsChanged);
    dialog.setVisible(true);

    if (dialog.isSettingsChanged()) {
      LOGGER.info("Display settings were modified");
      // Refresh UI components that depend on display settings
      refreshUIWithNewSettings();

      // Show confirmation message
      JOptionPane.showMessageDialog(
          this,
          "Display settings have been saved successfully.\nChanges will take effect immediately.",
          "Settings Saved",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }
  /**
   * Refresh UI components that depend on main settings.
   */
  private void refreshUIWithNewSettings() {
    // Refresh ROI display if there are ROIs visible
    if (mainImageViewer != null) {
      mainImageViewer.repaint();
    }

    // Update any other components that depend on main settings
    LOGGER.debug("UI refreshed with new main settings");
  }

  /**
   * Initialize the image viewer with loaded settings at startup.
   */
  public void initializeImageViewerWithSettings() {
    if (mainImageViewer != null && configurationManager != null) {
      MainSettings loadedSettings = configurationManager.loadMainSettings();
      if (loadedSettings != null) {
        mainImageViewer.initializeWithSettings(loadedSettings);
        LOGGER.debug("Initialized image viewer with loaded main settings");
      } else {
        LOGGER.warn("Could not load main settings for image viewer initialization");
      }
    }
  }

  /**
   * Handle settings changes from the MainSettingsDialog.
   * This method is called when the user saves changes in the settings dialog.
   *
   * @param newSettings The updated MainSettings
   */
  private void handleSettingsChanged(MainSettings newSettings) {
    LOGGER.info("Main settings changed, updating UI components");

    // Update ROI toolbar with new settings
    if (roiToolbar != null) {
      roiToolbar.setMainSettings(newSettings);
      roiToolbar.updateROITypeCounts(getROICountsByType());
      LOGGER.debug("Updated ROI toolbar with new settings");
    }

    // Update any ROI overlays if they exist and refresh the entire viewer
    if (mainImageViewer != null && mainImageViewer.getROIOverlay() != null) {
      mainImageViewer.getROIOverlay().updateSettings(newSettings);
      LOGGER.debug("Updated ROI overlay with new settings");
    }

    // Force a complete refresh of the image viewer (like after zoom/scroll)
    if (mainImageViewer != null) {
      mainImageViewer.revalidate();
      mainImageViewer.repaint();
      LOGGER.debug("Forced complete refresh of image viewer");
    }

    // Bring main window to front and request focus to ensure immediate visual update
    SwingUtilities.invokeLater(() -> {
      toFront();
      requestFocus();
      setState(java.awt.Frame.NORMAL);
      LOGGER.debug("Main window brought to front and focused");
    });

    // Any other components that depend on MainSettings should be updated here
    LOGGER.debug("All UI components updated with new main settings");
  }

  /**
   * Get ROI counts by type for all images, including ignored ROIs
   */
  private java.util.Map<MainSettings.ROICategory, Integer> getROICountsByType() {
    java.util.Map<MainSettings.ROICategory, Integer> counts = new java.util.HashMap<>();

    // Initialize counts
    for (MainSettings.ROICategory category : MainSettings.ROICategory.values()) {
      counts.put(category, 0);
    }

    // Count ROIs by type across all images
    java.util.Map<String, java.util.List<UserROI>> allROIs = roiManager.getAllROIsByImage();
    int totalROIs = 0;
    int ignoredROIs = 0;

    for (java.util.List<UserROI> roiList : allROIs.values()) {
      for (UserROI roi : roiList) {
        if (roi.isIgnored()) {
          ignoredROIs++;
        } else {
          MainSettings.ROICategory category = determineROICategory(roi);
          counts.put(category, counts.get(category) + 1);
        }
        totalROIs++;

        // Debug logging for first few ROIs
        if (totalROIs <= 5) {
          // LOGGER.debug("ROI '{}' of type '{}' class '{}' -> category '{}' (ignored: {})",
          //     roi.getName(), roi.getType(), roi.getClass().getSimpleName(), category, roi.isIgnored());
        }
      }
    }

    // Store ignored ROI count (we'll need to handle this in the toolbar)
    counts.put(null, ignoredROIs); // Use null key for ignored ROIs

    // LOGGER.debug("Total ROI count by category: VESSEL={}, NUCLEUS={}, CYTOPLASM={}, CELL={}, IGNORED={}",
    //     counts.get(MainSettings.ROICategory.VESSEL),
    //     counts.get(MainSettings.ROICategory.NUCLEUS),
    //     counts.get(MainSettings.ROICategory.CYTOPLASM),
    //     counts.get(MainSettings.ROICategory.CELL),
    //     ignoredROIs);

    return counts;
  }

  /**
   * Map ROI to category based on its type and class
   */
  private MainSettings.ROICategory determineROICategory(UserROI roi) {
    // First check the class type - this is more reliable than ROI type
    if (roi instanceof com.scipath.scipathj.infrastructure.roi.NucleusROI) {
      return MainSettings.ROICategory.NUCLEUS;
    }

    // Then check the ROI type
    UserROI.ROIType roiType = roi.getType();
    switch (roiType) {
      case VESSEL:
        return MainSettings.ROICategory.VESSEL;
      case NUCLEUS:
        return MainSettings.ROICategory.NUCLEUS;
      case CYTOPLASM:
        return MainSettings.ROICategory.CYTOPLASM;
      case CELL:
        return MainSettings.ROICategory.CELL;
      case IGNORE:
        return MainSettings.ROICategory.VESSEL; // Treat ignore as vessel category
      default:
        // Check name-based heuristics as fallback
        String name = roi.getName().toLowerCase();
        if (name.contains("vessel")) {
          return MainSettings.ROICategory.VESSEL;
        } else if (name.contains("nucleus") || name.contains("nuclei")) {
          return MainSettings.ROICategory.NUCLEUS;
        } else if (name.contains("cytoplasm") || name.contains("cyto")) {
          return MainSettings.ROICategory.CYTOPLASM;
        } else if (name.contains("cell")) {
          return MainSettings.ROICategory.CELL;
        }
        return MainSettings.ROICategory.VESSEL; // Default fallback
    }
  }
}
