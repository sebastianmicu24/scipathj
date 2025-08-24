package com.scipath.scipathj.ui.common.components;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.ui.common.dialogs.AboutDialog;
import com.scipath.scipathj.ui.common.dialogs.PreferencesDialog;
import com.scipath.scipathj.ui.analysis.dialogs.settings.VesselSegmentationSettingsDialog;
import com.scipath.scipathj.ui.main.MainWindow;
import java.awt.*;
import javax.swing.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the application menu bar and its functionality.
 *
 * <p>This class handles the creation and management of the main application
 * menu bar, including File, Preferences, and Help menus.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class MenuBarManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(MenuBarManager.class);

  private final JFrame parentFrame;
  private final ConfigurationManager configurationManager;
  private JMenuBar menuBar;
  private JMenuItem fullscreenMenuItem;

  /**
   * Creates a new MenuBarManager instance.
   *
   * @param parentFrame the parent frame for dialogs
   * @param configurationManager the configuration manager instance
   */
  public MenuBarManager(JFrame parentFrame, ConfigurationManager configurationManager) {
    this.parentFrame = parentFrame;
    this.configurationManager = configurationManager;
    createMenuBar();
  }

  /**
   * Gets the created menu bar.
   *
   * @return the menu bar
   */
  public JMenuBar getMenuBar() {
    return menuBar;
  }

  /**
   * Creates the menu bar with all menus.
   */
  private void createMenuBar() {
    menuBar = new JMenuBar();

    menuBar.add(createFileMenu());
    menuBar.add(createViewMenu());
    menuBar.add(createPreferencesMenu());
    menuBar.add(createHelpMenu());

    LOGGER.debug("Menu bar created successfully");
  }

  /**
   * Creates the File menu.
   *
   * @return the File menu
   */
  private JMenu createFileMenu() {
    JMenu fileMenu = new JMenu("Files");
    fileMenu.setMnemonic('F');

    JMenuItem exitItem = new JMenuItem("Exit");
    exitItem.setMnemonic('x');
    exitItem.setIcon(FontIcon.of(FontAwesomeSolid.SIGN_OUT_ALT, 14));
    exitItem.addActionListener(this::handleExit);

    fileMenu.add(exitItem);
    return fileMenu;
  }

  /**
   * Creates the View menu.
   *
   * @return the View menu
   */
  private JMenu createViewMenu() {
    JMenu viewMenu = new JMenu("View");
    viewMenu.setMnemonic('V');

    fullscreenMenuItem = new JMenuItem("Toggle Fullscreen");
    fullscreenMenuItem.setMnemonic('F');
    fullscreenMenuItem.setIcon(FontIcon.of(FontAwesomeSolid.EXPAND, 14));
    fullscreenMenuItem.setAccelerator(KeyStroke.getKeyStroke("F11"));
    fullscreenMenuItem.addActionListener(this::handleToggleFullscreen);

    viewMenu.add(fullscreenMenuItem);
    return viewMenu;
  }

  /**
   * Creates the Preferences menu.
   *
   * @return the Preferences menu
   */
  private JMenu createPreferencesMenu() {
    JMenu preferencesMenu = new JMenu("Preferences");
    preferencesMenu.setMnemonic('P');

    JMenuItem styleSettingsItem = new JMenuItem("Style Settings");
    styleSettingsItem.setMnemonic('S');
    styleSettingsItem.setIcon(FontIcon.of(FontAwesomeSolid.PALETTE, 14));
    styleSettingsItem.addActionListener(this::handleStyleSettings);

    JMenuItem vesselSettingsItem = new JMenuItem("Vessel Segmentation Settings");
    vesselSettingsItem.setMnemonic('V');
    vesselSettingsItem.setIcon(FontIcon.of(FontAwesomeSolid.COG, 14));
    vesselSettingsItem.addActionListener(this::handleVesselSettings);

    preferencesMenu.add(styleSettingsItem);
    preferencesMenu.addSeparator();
    preferencesMenu.add(vesselSettingsItem);
    return preferencesMenu;
  }

  /**
   * Creates the Help menu.
   *
   * @return the Help menu
   */
  private JMenu createHelpMenu() {
    JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic('H');

    JMenuItem aboutItem = new JMenuItem("About");
    aboutItem.setMnemonic('A');
    aboutItem.setIcon(FontIcon.of(FontAwesomeSolid.INFO_CIRCLE, 14));
    aboutItem.addActionListener(this::handleAbout);

    helpMenu.add(aboutItem);
    return helpMenu;
  }

  /**
   * Handles the exit menu item action.
   *
   * @param e the action event
   */
  private void handleExit(java.awt.event.ActionEvent e) {
    int result =
        JOptionPane.showConfirmDialog(
            parentFrame,
            "Are you sure you want to exit SciPathJ?",
            "Confirm Exit",
            JOptionPane.YES_NO_OPTION);
    if (result == JOptionPane.YES_OPTION) {
      LOGGER.info("Application exit requested by user");
      System.exit(0);
    }
  }

  /**
   * Handles the style settings menu item action.
   *
   * @param e the action event
   */
  private void handleStyleSettings(java.awt.event.ActionEvent e) {
    LOGGER.debug("Style settings dialog requested");
    PreferencesDialog.showPreferencesDialog(parentFrame);
  }

  /**
   * Handles the vessel segmentation settings menu item action.
   *
   * @param e the action event
   */
  private void handleVesselSettings(java.awt.event.ActionEvent e) {
    LOGGER.debug("Vessel segmentation settings dialog requested");
    VesselSegmentationSettingsDialog dialog =
        new VesselSegmentationSettingsDialog(parentFrame, configurationManager);
    dialog.setVisible(true);

    if (dialog.isSettingsChanged()) {
      LOGGER.info("Vessel segmentation settings were modified");
      // Optionally show a message to the user that settings were saved
      JOptionPane.showMessageDialog(
          parentFrame,
          "Vessel segmentation settings have been saved successfully.",
          "Settings Saved",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

  /**
   * Handles the about menu item action.
   *
   * @param e the action event
   */
  private void handleAbout(java.awt.event.ActionEvent e) {
    LOGGER.debug("About dialog requested");
    AboutDialog.showAboutDialog(parentFrame);
  }

  /**
   * Handles the toggle fullscreen menu item action.
   *
   * @param e the action event
   */
  private void handleToggleFullscreen(java.awt.event.ActionEvent e) {
    if (parentFrame instanceof MainWindow) {
      MainWindow mainWindow = (MainWindow) parentFrame;
      mainWindow.toggleFullscreen();

      // Update menu item text based on current state
      if (mainWindow.isFullScreen()) {
        fullscreenMenuItem.setText("Exit Fullscreen");
        fullscreenMenuItem.setIcon(FontIcon.of(FontAwesomeSolid.COMPRESS, 14));
      } else {
        fullscreenMenuItem.setText("Toggle Fullscreen");
        fullscreenMenuItem.setIcon(FontIcon.of(FontAwesomeSolid.EXPAND, 14));
      }

      LOGGER.debug("Fullscreen toggle requested");
    }
  }

  /**
   * Updates the fullscreen menu item text and icon based on current state.
   *
   * @param isFullScreen true if currently in fullscreen mode
   */
  public void updateFullscreenMenuItem(boolean isFullScreen) {
    if (fullscreenMenuItem != null) {
      if (isFullScreen) {
        fullscreenMenuItem.setText("Exit Fullscreen");
        fullscreenMenuItem.setIcon(FontIcon.of(FontAwesomeSolid.COMPRESS, 14));
      } else {
        fullscreenMenuItem.setText("Toggle Fullscreen");
        fullscreenMenuItem.setIcon(FontIcon.of(FontAwesomeSolid.EXPAND, 14));
      }
    }
  }
}
