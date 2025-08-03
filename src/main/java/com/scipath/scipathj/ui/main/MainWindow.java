package com.scipath.scipathj.ui.main;

import com.scipath.scipathj.core.engine.SciPathJEngine;
import com.scipath.scipathj.data.model.UserROI;
import com.scipath.scipathj.ui.components.FolderSelectionPanel;
import com.scipath.scipathj.ui.components.MainImageViewer;
import com.scipath.scipathj.ui.components.MenuBarManager;
import com.scipath.scipathj.ui.components.PipelineRecapPanel;
import com.scipath.scipathj.ui.components.PipelineSelectionPanel;
import com.scipath.scipathj.ui.components.ROIManager;
import com.scipath.scipathj.ui.components.ROIToolbar;
import com.scipath.scipathj.ui.components.SimpleImageGallery;
import com.scipath.scipathj.ui.components.StatusPanel;
import com.scipath.scipathj.ui.controllers.AnalysisController;
import com.scipath.scipathj.ui.controllers.NavigationController;
import com.scipath.scipathj.ui.dialogs.settings.MainSettingsDialog;
import com.scipath.scipathj.ui.model.PipelineInfo;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

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
    
    // UI Components
    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private PipelineSelectionPanel pipelineSelectionPanel;
    private JPanel analysisSetupPanel;
    private PipelineRecapPanel pipelineRecapPanel;
    private FolderSelectionPanel folderSelectionPanel;
    private JPanel imageViewPanel;
    private SimpleImageGallery imageGallery;
    private MainImageViewer mainImageViewer;
    private ROIToolbar roiToolbar;
    private StatusPanel statusPanel;
    private JButton startButton;
    private JButton stopButton;
    
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
     */
    public MainWindow(SciPathJEngine engine) {
        this.engine = engine;
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
        createControlButtons();
        setupCardLayout();
        createMainPanels();
        createControllers();
    }
    
    /**
     /**
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
     * Creates the control buttons.
     */
    private void createControlButtons() {
        startButton = UIUtils.createStandardButton("Start Analysis",
            FontIcon.of(FontAwesomeSolid.PLAY, 16));
        startButton.setEnabled(false);
        
        stopButton = UIUtils.createStandardButton("Stop Analysis",
            FontIcon.of(FontAwesomeSolid.STOP, 16));
        stopButton.setEnabled(false);
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
        pipelineSelectionPanel = new PipelineSelectionPanel();
        analysisSetupPanel = createAnalysisSetupPanel();
        imageViewPanel = createImageViewPanel();
        
        mainContentPanel.add(pipelineSelectionPanel, NavigationController.UIState.PIPELINE_SELECTION.name());
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
        pipelineRecapPanel = new PipelineRecapPanel();
        panel.add(pipelineRecapPanel, BorderLayout.NORTH);
        
        // Folder selection in the center
        folderSelectionPanel = new FolderSelectionPanel();
        panel.add(folderSelectionPanel, BorderLayout.CENTER);
        
        // Control buttons at the bottom
        JPanel controlPanel = createControlButtonPanel();
        panel.add(controlPanel, BorderLayout.SOUTH);
        
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
        
        // Create bottom panel with ROI toolbar and control buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        // Add ROI toolbar
        roiToolbar = new ROIToolbar();
        bottomPanel.add(roiToolbar, BorderLayout.NORTH);
        
        // Add control buttons
        bottomPanel.add(createControlButtonPanel(), BorderLayout.SOUTH);
        
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }
    
    /**
     * Creates the top panel for image view with pipeline recap and change folder button.
     */
    private JPanel createImageViewTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        
        // Create button panel with both change folder and main settings buttons
        JButton changeFolderButton = UIUtils.createSmallButton("Change Folder",
            FontIcon.of(FontAwesomeSolid.FOLDER_OPEN, 14));
        changeFolderButton.setPreferredSize(new Dimension(140, 32));
        changeFolderButton.addActionListener(e -> navigationController.switchToFolderSelection());
        
        JButton mainSettingsButton = UIUtils.createSmallButton("Main Settings",
            FontIcon.of(FontAwesomeSolid.COG, 14));
        mainSettingsButton.setPreferredSize(new Dimension(140, 32));
        mainSettingsButton.addActionListener(this::openMainSettings);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,
            UIConstants.MEDIUM_SPACING, UIConstants.SMALL_SPACING));
        buttonPanel.setBorder(UIUtils.createPadding(UIConstants.SMALL_SPACING,
            UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING, UIConstants.MEDIUM_SPACING));
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
        imageGallery.setBorder(createTitledBorder("Images"));
        mainContent.add(imageGallery, BorderLayout.WEST);
        
        mainImageViewer = new MainImageViewer();
        mainImageViewer.setBorder(createTitledBorder("Image Viewer"));
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
            new Font(Font.SANS_SERIF, Font.BOLD, (int) UIConstants.TINY_FONT_SIZE)
        );
    }
    
    /**
     * Creates the control button panel for the analysis setup.
     * 
     * @return control button panel
     */
    private JPanel createControlButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER,
            UIConstants.LARGE_SPACING, UIConstants.LARGE_SPACING));
        panel.add(startButton);
        panel.add(stopButton);
        return panel;
    }
    
    /**
     * Creates the controllers and managers.
     */
    private void createControllers() {
        // Create status panel
        statusPanel = new StatusPanel();
        
        // Create menu bar manager
        menuBarManager = new MenuBarManager(this);
        
        // Create navigation controller
        navigationController = new NavigationController(
            cardLayout, mainContentPanel, statusPanel, pipelineRecapPanel,
            folderSelectionPanel, imageGallery, mainImageViewer
        );
        
        // Create analysis controller
        analysisController = new AnalysisController(engine, statusPanel, this);
        analysisController.setControlButtons(startButton, stopButton);
        
        // Set up navigation controller callback
        navigationController.setStartButtonStateUpdater(this::updateStartButtonState);
        
        // Set up status panel back button
        statusPanel.setBackButtonListener(e -> navigationController.switchToPipelineSelection());
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
        // Pipeline selection handler
        pipelineSelectionPanel.setSelectionListener(e -> {
            PipelineInfo selectedPipeline = pipelineSelectionPanel.getSelectedPipeline();
            if (selectedPipeline != null) {
                navigationController.switchToAnalysisSetup(selectedPipeline);
            }
        });
        
        // Folder selection handler
        folderSelectionPanel.setFolderChangeListener(e -> {
            File selectedFolder = folderSelectionPanel.getSelectedFolder();
            
            if (selectedFolder != null && selectedFolder.isDirectory()) {
                navigationController.setSelectedFolder(selectedFolder);
                // Switch to image gallery view
                navigationController.switchToImageGallery(selectedFolder);
            }
            
            updateStartButtonState();
        });
        
        // Image gallery selection handler
        imageGallery.setSelectionChangeListener(e -> {
            File selectedImageFile = imageGallery.getSelectedImageFile();
            if (selectedImageFile != null) {
                mainImageViewer.displayImage(selectedImageFile);
                LOGGER.debug("Selected image: {}", selectedImageFile.getName());
            }
        });
        
        // Override start button action to use analysis controller
        startButton.addActionListener(e -> {
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
        roiToolbar.addROIToolbarListener(new ROIToolbar.ROIToolbarListener() {
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
                    JOptionPane.showMessageDialog(MainWindow.this,
                                                "ROIs saved successfully to:\n" + outputFile.getAbsolutePath(),
                                                "Save ROIs",
                                                JOptionPane.INFORMATION_MESSAGE);
                    LOGGER.info("Successfully saved ROIs to file: {}", outputFile.getAbsolutePath());
                } catch (IOException e) {
                    LOGGER.error("Error saving ROIs to file: {}", outputFile.getAbsolutePath(), e);
                    JOptionPane.showMessageDialog(MainWindow.this,
                                                "Error saving ROIs:\n" + e.getMessage(),
                                                "Save ROIs Error",
                                                JOptionPane.ERROR_MESSAGE);
                }
            }
            
                        @Override
                        public void onSaveAllROIs(File outputFile) {
                            try {
                                roiManager.saveAllROIsToMasterZip(outputFile);
                                JOptionPane.showMessageDialog(MainWindow.this,
                                                            "All ROIs saved successfully to master ZIP:\n" + outputFile.getAbsolutePath(),
                                                            "Save All ROIs",
                                                            JOptionPane.INFORMATION_MESSAGE);
                                LOGGER.info("Successfully saved all ROIs to master ZIP file: {}", outputFile.getAbsolutePath());
                            } catch (IOException e) {
                                LOGGER.error("Error saving all ROIs to master ZIP file: {}", outputFile.getAbsolutePath(), e);
                                JOptionPane.showMessageDialog(MainWindow.this,
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
        });
        
        // Set up ROI manager listeners
        roiManager.addROIChangeListener(new ROIManager.ROIChangeListener() {
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
        imageGallery.setSelectionChangeListener(e -> {
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
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f11KeyStroke, "toggleFullscreen");
        getRootPane().getActionMap().put("toggleFullscreen", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleFullscreen();
            }
        });
        
        // Alt+Enter for fullscreen toggle (alternative shortcut)
        KeyStroke altEnterKeyStroke = KeyStroke.getKeyStroke("alt ENTER");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(altEnterKeyStroke, "toggleFullscreenAlt");
        getRootPane().getActionMap().put("toggleFullscreenAlt", new AbstractAction() {
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
        MainSettingsDialog dialog = new MainSettingsDialog(this);
        dialog.setVisible(true);
        
        if (dialog.isSettingsChanged()) {
            LOGGER.info("Main settings were modified");
            // Optionally refresh UI components that depend on main settings
            refreshUIWithNewSettings();
            
            // Show confirmation message
            JOptionPane.showMessageDialog(this,
                "Main settings have been saved successfully.\nChanges will take effect immediately.",
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
}
