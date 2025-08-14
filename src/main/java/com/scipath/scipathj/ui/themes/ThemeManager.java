package com.scipath.scipathj.ui.themes;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages application themes and look-and-feel settings.
 *
 * <p>This class provides centralized theme management for the SciPathJ application,
 * supporting both light and dark themes with modern FlatLaf styling.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class ThemeManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ThemeManager.class);

  /**
   * Available theme types.
   */
  public enum Theme {
    LIGHT("Light", FlatLightLaf.class),
    DARK("Dark", FlatDarculaLaf.class);

    private final String displayName;
    private final Class<? extends LookAndFeel> lafClass;

    Theme(String displayName, Class<? extends LookAndFeel> lafClass) {
      this.displayName = displayName;
      this.lafClass = lafClass;
    }

    public String getDisplayName() {
      return displayName;
    }

    public Class<? extends LookAndFeel> getLafClass() {
      return lafClass;
    }
  }

  private static Theme currentTheme = Theme.DARK; // Default to dark theme

  /**
   * Initializes the theme system with the default theme.
   */
  public static void initializeTheme() {
    LOGGER.info("Initializing theme system");

    try {
      // Set default look and feel decorations
      JFrame.setDefaultLookAndFeelDecorated(true);
      JDialog.setDefaultLookAndFeelDecorated(true);

      // Apply default theme
      applyTheme(currentTheme);

      LOGGER.info("Theme system initialized with {} theme", currentTheme.getDisplayName());

    } catch (Exception e) {
      LOGGER.error("Failed to initialize theme system", e);
      // Fallback to system look and feel
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        LOGGER.warn("Fallback to system look and feel");
      } catch (Exception fallbackException) {
        LOGGER.error("Failed to set fallback look and feel", fallbackException);
      }
    }
  }

  /**
   * Applies the specified theme to the application.
   *
   * @param theme the theme to apply
   * @throws UnsupportedLookAndFeelException if the theme is not supported
   */
  public static void applyTheme(Theme theme) throws UnsupportedLookAndFeelException {
    LOGGER.debug("Applying {} theme", theme.getDisplayName());

    try {
      // Create and set the look and feel
      LookAndFeel laf = theme.getLafClass().getDeclaredConstructor().newInstance();
      UIManager.setLookAndFeel(laf);

      // Update current theme
      currentTheme = theme;

      // Update all existing windows
      updateAllWindows();

      LOGGER.info("Successfully applied {} theme", theme.getDisplayName());

    } catch (Exception e) {
      LOGGER.error("Failed to apply {} theme", theme.getDisplayName(), e);
      throw new UnsupportedLookAndFeelException("Failed to apply theme: " + e.getMessage());
    }
  }

  /**
   * Gets the currently active theme.
   *
   * @return current theme
   */
  public static Theme getCurrentTheme() {
    return currentTheme;
  }

  /**
   * Switches to the opposite theme (light <-> dark).
   */
  public static void toggleTheme() {
    Theme newTheme = (currentTheme == Theme.LIGHT) ? Theme.DARK : Theme.LIGHT;

    try {
      applyTheme(newTheme);
    } catch (UnsupportedLookAndFeelException e) {
      LOGGER.error("Failed to toggle theme", e);
    }
  }

  /**
   * Updates all existing windows to reflect the new theme.
   */
  private static void updateAllWindows() {
    SwingUtilities.invokeLater(
        () -> {
          // Update all frames
          for (Frame frame : Frame.getFrames()) {
            SwingUtilities.updateComponentTreeUI(frame);
            if (frame instanceof JFrame) {
              ((JFrame) frame).pack();
            }
          }

          // Update all dialogs
          for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            if (window instanceof JDialog) {
              ((JDialog) window).pack();
            }
          }
        });
  }

  /**
   * Gets all available themes.
   *
   * @return array of available themes
   */
  public static Theme[] getAvailableThemes() {
    return Theme.values();
  }

  /**
   * Checks if dark theme is currently active.
   *
   * @return true if dark theme is active
   */
  public static boolean isDarkTheme() {
    return currentTheme == Theme.DARK;
  }

  /**
   * Checks if light theme is currently active.
   *
   * @return true if light theme is active
   */
  public static boolean isLightTheme() {
    return currentTheme == Theme.LIGHT;
  }
}
