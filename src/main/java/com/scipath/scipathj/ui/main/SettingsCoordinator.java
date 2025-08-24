package com.scipath.scipathj.ui.main;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.config.MainSettings;
import com.scipath.scipathj.ui.common.dialogs.settings.DisplaySettingsDialog;
import com.scipath.scipathj.ui.common.dialogs.settings.MainSettingsDialog;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinates settings dialogs and their interactions.
 *
 * <p>This class manages the creation, display, and coordination of all settings dialogs
 * in the application, ensuring consistent behavior and proper integration with the
 * configuration system.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class SettingsCoordinator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SettingsCoordinator.class);

  private final ConfigurationManager configurationManager;
  private final Window parentWindow;
  private final Consumer<MainSettings> settingsChangeCallback;

  /**
   * Creates a new SettingsCoordinator instance.
   *
   * @param configurationManager the configuration manager
   * @param parentWindow the parent window for dialogs
   * @param settingsChangeCallback callback for when settings change
   */
  public SettingsCoordinator(
      ConfigurationManager configurationManager,
      Window parentWindow,
      Consumer<MainSettings> settingsChangeCallback) {
    this.configurationManager = configurationManager;
    this.parentWindow = parentWindow;
    this.settingsChangeCallback = settingsChangeCallback;
  }

  /**
   * Opens the main settings dialog.
   */
  public void openMainSettings() {
    LOGGER.debug("Main settings dialog requested");
    MainSettingsDialog dialog = new MainSettingsDialog(
        (Frame) parentWindow,
        configurationManager,
        this::handleSettingsChanged);
    dialog.setVisible(true);

    if (dialog.isSettingsChanged()) {
      LOGGER.info("Main settings were modified");
      handleSettingsChanged(null);

      // Show confirmation message
      JOptionPane.showMessageDialog(
          parentWindow,
          "Main settings have been saved successfully.\nChanges will take effect immediately.",
          "Settings Saved",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

  /**
   * Opens the display settings dialog.
   */
  public void openDisplaySettings() {
    LOGGER.debug("Display settings dialog requested");
    MainSettings currentSettings = configurationManager.loadMainSettings();
    DisplaySettingsDialog dialog = new DisplaySettingsDialog(
        (Frame) parentWindow,
        currentSettings,
        this::handleSettingsChanged);
    dialog.setVisible(true);

    if (dialog.isSettingsChanged()) {
      LOGGER.info("Display settings were modified");
      handleSettingsChanged(null);

      // Show confirmation message
      JOptionPane.showMessageDialog(
          parentWindow,
          "Display settings have been saved successfully.\nChanges will take effect immediately.",
          "Settings Saved",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

  /**
   * Handles settings changes from any settings dialog.
   *
   * @param newSettings the updated settings (may be null if not provided)
   */
  private void handleSettingsChanged(MainSettings newSettings) {
    LOGGER.info("Settings changed, updating UI components");

    // Load current settings if not provided
    MainSettings currentSettings = newSettings != null ? newSettings :
        configurationManager.loadMainSettings();

    // Notify callback about settings change
    if (settingsChangeCallback != null) {
      settingsChangeCallback.accept(currentSettings);
    }

    // Bring main window to front and request focus to ensure immediate visual update
    SwingUtilities.invokeLater(() -> {
      parentWindow.toFront();
      parentWindow.requestFocus();
      if (parentWindow instanceof Frame) {
        ((Frame) parentWindow).setState(java.awt.Frame.NORMAL);
      }
      LOGGER.debug("Main window brought to front and focused");
    });
  }

  /**
   * Creates an action listener for opening main settings.
   *
   * @return action listener for main settings
   */
  public java.awt.event.ActionListener createMainSettingsAction() {
    return (ActionEvent e) -> openMainSettings();
  }

  /**
   * Creates an action listener for opening display settings.
   *
   * @return action listener for display settings
   */
  public java.awt.event.ActionListener createDisplaySettingsAction() {
    return (ActionEvent e) -> openDisplaySettings();
  }
}