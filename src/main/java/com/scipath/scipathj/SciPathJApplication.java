package com.scipath.scipathj;

import com.scipath.scipathj.core.bootstrap.ApplicationContext;
import com.scipath.scipathj.ui.main.MainWindow;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application entry point for SciPathJ.
 *
 * <p>SciPathJ (Segmentation and Classification of Images, Pipelines for the Analysis of Tissue
 * Histopathology) is a comprehensive image analysis software for histopathological research.
 *
 * <p>This application provides automated analysis workflows for tissue images, including
 * segmentation, feature extraction, and machine learning-based classification.
 *
 * <p>This class follows SOLID principles:
 * <ul>
 *   <li><strong>SRP:</strong> Only responsible for application entry point and coordination
 *   <li><strong>OCP:</strong> Extensible through ApplicationContext and dependency injection
 *   <li><strong>LSP:</strong> Uses abstractions that can be substituted
 *   <li><strong>ISP:</strong> Depends only on specific interfaces needed
 *   <li><strong>DIP:</strong> Depends on abstractions, not concrete implementations
 * </ul>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SciPathJApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(SciPathJApplication.class);
  private static final int EXIT_CODE_ERROR = 1;

  private SciPathJApplication() {
    // Utility class - prevent instantiation
  }

  /**
   * Application entry point.
   *
   * @param args command line arguments (currently unused)
   */
  public static void main(final String[] args) {
    LOGGER.info("Starting SciPathJ Application v1.0.0");

    try {
      // Initialize application context with dependency injection
      ApplicationContext context = new ApplicationContext();
      context.initialize();

      // Configure system properties for optimal UI experience
      context.getSystemConfigurationService().configureSystemProperties();

      // Initialize theme system
      context.getThemeService().initializeTheme();

      // Start GUI on Event Dispatch Thread
      SwingUtilities.invokeLater(() -> createAndShowGUI(context));

    } catch (Exception e) {
      LOGGER.error("Failed to start SciPathJ application", e);
      showErrorDialog("Application Startup Error", "Failed to start SciPathJ: " + e.getMessage());
      System.exit(EXIT_CODE_ERROR);
    }
  }

  /**
   * Creates and displays the main application GUI.
   * This method runs on the Event Dispatch Thread.
   *
   * @param context the initialized application context
   */
  private static void createAndShowGUI(final ApplicationContext context) {
    try {
      LOGGER.info("Initializing main application window");

      // Create main window with injected dependencies
      MainWindow mainWindow = context.createMainWindow();

      // Configure main window
      mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      mainWindow.setLocationRelativeTo(null);

      // Show the application
      mainWindow.setVisible(true);

      LOGGER.info("SciPathJ application started successfully");

    } catch (Exception e) {
      LOGGER.error("Failed to create main application window", e);
      showErrorDialog(
          "GUI Initialization Error", "Failed to initialize user interface: " + e.getMessage());
      System.exit(EXIT_CODE_ERROR);
    }
  }

  /**
   * Shows an error dialog to the user.
   *
   * @param title the dialog title
   * @param message the error message to display
   */
  private static void showErrorDialog(final String title, final String message) {
    SwingUtilities.invokeLater(
        () -> JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE));
  }
}
