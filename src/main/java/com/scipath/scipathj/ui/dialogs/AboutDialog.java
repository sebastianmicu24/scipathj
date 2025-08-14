package com.scipath.scipathj.ui.dialogs;

import java.awt.*;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * About dialog for the SciPathJ application.
 *
 * <p>This class provides a simple about dialog that displays application
 * information including version, Java version, and copyright.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class AboutDialog {

  private static final Logger LOGGER = LoggerFactory.getLogger(AboutDialog.class);

  /**
   * Shows the about dialog.
   *
   * @param parentComponent the parent component for the dialog
   */
  public static void showAboutDialog(Component parentComponent) {
    LOGGER.debug("Showing about dialog");

    String message =
        "<html><center>"
            + "<h2>SciPathJ</h2>"
            + "<p>Segmentation and Classification of Images<br>"
            + "Pipelines for the Analysis of Tissue Histopathology</p>"
            + "<p>Version 1.0.0-SNAPSHOT</p>"
            + "<p>Built with Java "
            + System.getProperty("java.version")
            + "</p>"
            + "<p>© 2025 Sebastian Micu</p>"
            + "</center></html>";

    JOptionPane.showMessageDialog(
        parentComponent, message, "About SciPathJ", JOptionPane.INFORMATION_MESSAGE);

    LOGGER.debug("About dialog closed");
  }
}
