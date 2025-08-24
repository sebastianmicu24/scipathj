package com.scipath.scipathj.infrastructure.bootstrap;

import com.scipath.scipathj.ui.themes.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for theme initialization and management.
 * Encapsulates theme-related operations following SRP.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ThemeService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ThemeService.class);

  /**
   * Initializes the application theme system.
   */
  public void initializeTheme() {
    try {
      LOGGER.debug("Initializing application theme");
      ThemeManager.initializeTheme();
      LOGGER.debug("Theme initialized successfully");
    } catch (Exception e) {
      LOGGER.warn("Failed to initialize theme, using default", e);
    }
  }
}
