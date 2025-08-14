package com.scipath.scipathj.core.bootstrap;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.engine.SciPathJEngine;
import com.scipath.scipathj.ui.main.MainWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application context that manages dependency injection and component lifecycle.
 * Replaces singleton pattern with proper dependency injection.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ApplicationContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContext.class);

  private ConfigurationManager configurationManager;
  private SciPathJEngine engine;
  private ThemeService themeService;
  private SystemConfigurationService systemConfigurationService;
  private ApplicationLifecycleManager lifecycleManager;

  /**
   * Initializes the application context and all managed components.
   */
  public void initialize() {
    LOGGER.debug("Initializing application context");

    // Initialize services in dependency order
    systemConfigurationService = new SystemConfigurationService();
    themeService = new ThemeService();
    configurationManager = new ConfigurationManager();

    // Initialize all settings
    initializeSettings();

    engine = new SciPathJEngine(configurationManager);
    lifecycleManager = new ApplicationLifecycleManager(engine);

    LOGGER.debug("Application context initialized successfully");
  }

  /**
   * Initializes all application settings.
   */
  private void initializeSettings() {
    try {
      // Load settings to ensure they are available and valid
      configurationManager.loadVesselSegmentationSettings();
      configurationManager.loadMainSettings();
      configurationManager.loadNuclearSegmentationSettings();
      configurationManager.loadCytoplasmSegmentationSettings();
      LOGGER.debug("All settings initialized successfully");
    } catch (Exception e) {
      LOGGER.warn("Failed to initialize some settings, using defaults", e);
    }
  }

  /**
   * Creates the main application window with injected dependencies.
   *
   * @return configured main window instance
   */
  public MainWindow createMainWindow() {
    if (engine == null || configurationManager == null) {
      throw new IllegalStateException("Application context not initialized");
    }
    return new MainWindow(engine, configurationManager);
  }

  /**
   * Shuts down all managed components in reverse order.
   */
  public void shutdown() {
    LOGGER.debug("Shutting down application context");

    if (lifecycleManager != null) {
      lifecycleManager.shutdown();
    }
    if (engine != null) {
      engine.shutdown();
    }

    LOGGER.debug("Application context shutdown completed");
  }

  // Getters for dependency injection
  public ConfigurationManager getConfigurationManager() {
    return configurationManager;
  }

  public SciPathJEngine getEngine() {
    return engine;
  }

  public ThemeService getThemeService() {
    return themeService;
  }

  public SystemConfigurationService getSystemConfigurationService() {
    return systemConfigurationService;
  }

  public ApplicationLifecycleManager getLifecycleManager() {
    return lifecycleManager;
  }
}
