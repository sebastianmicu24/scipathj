package com.scipath.scipathj.ui.main;

import com.scipath.scipathj.ui.common.components.MenuBarManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages window lifecycle, fullscreen mode, and keyboard shortcuts.
 *
 * <p>This class handles all window-related operations including fullscreen toggling,
 * window sizing, keyboard shortcuts, and window state management.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class WindowManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(WindowManager.class);

  private final JFrame window;
  private final MenuBarManager menuBarManager;

  private boolean isFullScreen = false;
  private Rectangle normalBounds;
  private int normalExtendedState;

  /**
   * Creates a new WindowManager instance.
   *
   * @param window the main application window
   * @param menuBarManager the menu bar manager for fullscreen state updates
   */
  public WindowManager(JFrame window, MenuBarManager menuBarManager) {
    this.window = window;
    this.menuBarManager = menuBarManager;
  }

  /**
   * Sets up the main window properties.
   *
   * @param title the window title
   * @param width the default window width
   * @param height the default window height
   */
  public void setupWindow(String title, int width, int height) {
    window.setTitle(title);
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.setSize(width, height);
    window.setMinimumSize(new Dimension(800, 600));
    window.setResizable(true);
    window.setLocationRelativeTo(null);

    // Set application icon
    try {
      ImageIcon icon = new ImageIcon(getClass().getResource("/icon.png"));
      window.setIconImage(icon.getImage());
    } catch (Exception e) {
      LOGGER.warn("Could not load application icon", e);
    }

    // Add keyboard shortcuts for fullscreen
    setupKeyboardShortcuts();
  }

  /**
   * Sets up keyboard shortcuts for the application.
   */
  private void setupKeyboardShortcuts() {
    // F11 for fullscreen toggle
    KeyStroke f11KeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0);
    window.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f11KeyStroke, "toggleFullscreen");
    window.getRootPane().getActionMap().put("toggleFullscreen", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        toggleFullscreen();
      }
    });

    // Alt+Enter for fullscreen toggle (alternative shortcut)
    KeyStroke altEnterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ActionEvent.ALT_MASK);
    window.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(altEnterKeyStroke, "toggleFullscreenAlt");
    window.getRootPane().getActionMap().put("toggleFullscreenAlt", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
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
    normalBounds = window.getBounds();
    normalExtendedState = window.getExtendedState();

    // Hide menu bar and decorations
    window.dispose();
    window.setUndecorated(true);

    // Get the graphics device and enter fullscreen
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice gd = ge.getDefaultScreenDevice();

    if (gd.isFullScreenSupported()) {
      try {
        gd.setFullScreenWindow(window);
        isFullScreen = true;
        LOGGER.info("Entered fullscreen mode");
      } catch (Exception e) {
        LOGGER.error("Failed to enter fullscreen mode", e);
        // Fallback to maximized window
        window.setUndecorated(false);
        window.setVisible(true);
        window.setExtendedState(JFrame.MAXIMIZED_BOTH);
        isFullScreen = true;
      }
    } else {
      // Fallback for systems that don't support fullscreen
      window.setUndecorated(false);
      window.setVisible(true);
      window.setExtendedState(JFrame.MAXIMIZED_BOTH);
      isFullScreen = true;
      LOGGER.warn("Fullscreen not supported, using maximized window instead");
    }

    window.setVisible(true);

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

    if (gd.getFullScreenWindow() == window) {
      gd.setFullScreenWindow(null);
    }

    // Restore window decorations and state
    window.dispose();
    window.setUndecorated(false);
    window.setVisible(true);

    // Restore previous bounds and state
    if (normalBounds != null) {
      window.setBounds(normalBounds);
      window.setExtendedState(normalExtendedState);
    } else {
      // Fallback to default size
      window.setSize(1200, 800);
      window.setLocationRelativeTo(null);
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
   * Gets the window bounds for saving window state.
   *
   * @return the current window bounds
   */
  public Rectangle getWindowBounds() {
    return window.getBounds();
  }

  /**
   * Sets the window bounds when restoring from saved state.
   *
   * @param bounds the bounds to restore
   */
  public void setWindowBounds(Rectangle bounds) {
    window.setBounds(bounds);
  }

  /**
   * Gets the window extended state.
   *
   * @return the extended state (maximized, minimized, etc.)
   */
  public int getExtendedState() {
    return window.getExtendedState();
  }

  /**
   * Sets the window extended state.
   *
   * @param state the extended state to set
   */
  public void setExtendedState(int state) {
    window.setExtendedState(state);
  }
}