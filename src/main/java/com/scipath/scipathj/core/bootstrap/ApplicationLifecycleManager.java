package com.scipath.scipathj.core.bootstrap;

import com.scipath.scipathj.core.engine.SciPathJEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the application lifecycle including startup and shutdown procedures.
 * Handles shutdown hooks and resource cleanup following SRP.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ApplicationLifecycleManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationLifecycleManager.class);

  private final SciPathJEngine engine;

  /**
   * Creates a new lifecycle manager with the specified engine.
   *
   * @param engine the application engine to manage
   */
  public ApplicationLifecycleManager(SciPathJEngine engine) {
    this.engine = engine;
    registerShutdownHook();
  }

  /**
   * Registers a shutdown hook for clean application termination.
   */
  private void registerShutdownHook() {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  LOGGER.info("Shutting down SciPathJ application");
                  shutdown();
                },
                "SciPathJ-Shutdown"));
  }

  /**
   * Performs clean shutdown of all application components.
   */
  public void shutdown() {
    try {
      if (engine != null) {
        engine.shutdown();
      }
      LOGGER.info("Application shutdown completed successfully");
    } catch (Exception e) {
      LOGGER.error("Error during application shutdown", e);
    }
  }
}
