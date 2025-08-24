package com.scipath.scipathj.infrastructure.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for configuring system properties for optimal application performance.
 * Handles UI rendering, font settings, and platform-specific configurations.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SystemConfigurationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SystemConfigurationService.class);

  /**
   * Configures all necessary system properties for optimal UI experience.
   */
  public void configureSystemProperties() {
    LOGGER.debug("Configuring system properties");

    configureUIProperties();
    configureFontRendering();
    configurePlatformSpecific();

    LOGGER.debug("System properties configured successfully");
  }

  private void configureUIProperties() {
    // Enable high-DPI support
    System.setProperty("sun.java2d.uiScale", "1.0");

    // Enable hardware acceleration
    System.setProperty("sun.java2d.opengl", "true");
  }

  private void configureFontRendering() {
    // Improve font rendering
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");
  }

  private void configurePlatformSpecific() {
    // Set application name for OS integration
    System.setProperty("apple.awt.application.name", "SciPathJ");

    // Encoding settings
    System.setProperty("file.encoding", "UTF-8");
  }
}
