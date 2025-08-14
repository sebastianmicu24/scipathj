package com.scipath.scipathj.core.engine;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive debugging utility for ClassLoader issues in Java 21.
 * This class provides detailed information about the current ClassLoader
 * environment and helps identify potential issues.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class ClassLoaderDebugger {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClassLoaderDebugger.class);

  /**
   * Performs comprehensive ClassLoader debugging and logs all relevant information.
   */
  public static void debugClassLoaderEnvironment() {
    LOGGER.info("=== ClassLoader Environment Debug Report ===");

    // Basic Java environment info
    logJavaEnvironmentInfo();

    // ClassLoader hierarchy
    logClassLoaderHierarchy();

    // System properties related to ClassLoaders
    logRelevantSystemProperties();

    // Thread context ClassLoader
    logThreadContextClassLoader();

    // Module system information
    logModuleSystemInfo();

    // TensorFlow specific debugging
    logTensorFlowEnvironment();

    LOGGER.info("=== End ClassLoader Debug Report ===");
  }

  /**
   * Logs basic Java environment information.
   */
  private static void logJavaEnvironmentInfo() {
    LOGGER.info("Java Version: {}", System.getProperty("java.version"));
    LOGGER.info("Java Vendor: {}", System.getProperty("java.vendor"));
    LOGGER.info("Java Runtime: {}", System.getProperty("java.runtime.name"));
    LOGGER.info("Java VM: {}", System.getProperty("java.vm.name"));
    LOGGER.info("Java VM Version: {}", System.getProperty("java.vm.version"));
    LOGGER.info(
        "OS: {} {} {}",
        System.getProperty("os.name"),
        System.getProperty("os.version"),
        System.getProperty("os.arch"));
  }

  /**
   * Logs the complete ClassLoader hierarchy.
   */
  private static void logClassLoaderHierarchy() {
    LOGGER.info("--- ClassLoader Hierarchy ---");

    // Current thread context ClassLoader
    ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
    LOGGER.info("Thread Context ClassLoader: {}", getClassLoaderInfo(contextCL));

    // System ClassLoader
    ClassLoader systemCL = ClassLoader.getSystemClassLoader();
    LOGGER.info("System ClassLoader: {}", getClassLoaderInfo(systemCL));

    // Platform ClassLoader (Java 9+)
    try {
      ClassLoader platformCL = ClassLoader.getPlatformClassLoader();
      LOGGER.info("Platform ClassLoader: {}", getClassLoaderInfo(platformCL));
    } catch (Exception e) {
      LOGGER.debug("Platform ClassLoader not available: {}", e.getMessage());
    }

    // Bootstrap ClassLoader (null)
    LOGGER.info("Bootstrap ClassLoader: null (native)");

    // Walk up the hierarchy from context ClassLoader
    LOGGER.info("--- ClassLoader Chain from Context ---");
    ClassLoader current = contextCL;
    int level = 0;
    while (current != null && level < 10) { // Prevent infinite loops
      LOGGER.info("Level {}: {}", level, getClassLoaderInfo(current));
      current = current.getParent();
      level++;
    }
  }

  /**
   * Gets detailed information about a ClassLoader.
   */
  private static String getClassLoaderInfo(ClassLoader cl) {
    if (cl == null) {
      return "null (Bootstrap ClassLoader)";
    }

    StringBuilder info = new StringBuilder();
    info.append(cl.getClass().getName());
    info.append(" [").append(System.identityHashCode(cl)).append("]");

    // Check if it's a URLClassLoader
    if (cl instanceof URLClassLoader) {
      URLClassLoader urlCL = (URLClassLoader) cl;
      URL[] urls = urlCL.getURLs();
      info.append(" (URLClassLoader with ").append(urls.length).append(" URLs)");
    } else {
      info.append(" (NOT URLClassLoader)");
    }

    // Try to get module information
    try {
      Module module = cl.getClass().getModule();
      if (module != null) {
        info.append(" Module: ").append(module.getName());
      }
    } catch (Exception e) {
      // Ignore
    }

    return info.toString();
  }

  /**
   * Logs relevant system properties.
   */
  private static void logRelevantSystemProperties() {
    LOGGER.info("--- Relevant System Properties ---");

    String[] relevantProps = {
      "java.class.path",
      "java.library.path",
      "java.system.class.loader",
      "jdk.module.illegalAccess",
      "jdk.module.illegalAccess.silent",
      "tensorflow.eager",
      "org.tensorflow.NativeLibrary.DEBUG"
    };

    for (String prop : relevantProps) {
      String value = System.getProperty(prop);
      if (value != null) {
        if (prop.equals("java.class.path") || prop.equals("java.library.path")) {
          // Truncate long paths
          if (value.length() > 200) {
            value = value.substring(0, 200) + "... (truncated)";
          }
        }
        LOGGER.info("{}: {}", prop, value);
      } else {
        LOGGER.info("{}: <not set>", prop);
      }
    }
  }

  /**
   * Logs thread context ClassLoader information.
   */
  private static void logThreadContextClassLoader() {
    LOGGER.info("--- Thread Context ClassLoader Details ---");

    Thread currentThread = Thread.currentThread();
    ClassLoader contextCL = currentThread.getContextClassLoader();

    LOGGER.info("Current Thread: {}", currentThread.getName());
    LOGGER.info("Thread Group: {}", currentThread.getThreadGroup().getName());
    LOGGER.info("Context ClassLoader: {}", getClassLoaderInfo(contextCL));

    // Try to access internal fields
    AccessController.doPrivileged(
        new PrivilegedAction<Void>() {
          @Override
          public Void run() {
            try {
              // Check if we can access the contextClassLoader field
              Field contextField = Thread.class.getDeclaredField("contextClassLoader");
              contextField.setAccessible(true);
              ClassLoader fieldValue = (ClassLoader) contextField.get(currentThread);
              LOGGER.info("Context ClassLoader (via field): {}", getClassLoaderInfo(fieldValue));
            } catch (Exception e) {
              LOGGER.debug("Could not access contextClassLoader field: {}", e.getMessage());
            }
            return null;
          }
        });
  }

  /**
   * Logs module system information.
   */
  private static void logModuleSystemInfo() {
    LOGGER.info("--- Module System Information ---");

    try {
      // Check if we're running with modules
      ModuleLayer bootLayer = ModuleLayer.boot();
      LOGGER.info("Boot Module Layer: {}", bootLayer);
      LOGGER.info("Boot Layer Modules: {}", bootLayer.modules().size());

      // Check our own module
      Module ourModule = ClassLoaderDebugger.class.getModule();
      LOGGER.info("Our Module: {}", ourModule.getName());
      LOGGER.info("Our Module Named: {}", ourModule.isNamed());

    } catch (Exception e) {
      LOGGER.debug("Module system information not available: {}", e.getMessage());
    }
  }

  /**
   * Logs TensorFlow-specific environment information.
   */
  private static void logTensorFlowEnvironment() {
    LOGGER.info("--- TensorFlow Environment ---");

    // Check if TensorFlow classes are available
    String[] tensorFlowClasses = {
      "org.tensorflow.TensorFlow",
      "org.tensorflow.Tensor",
      "net.imagej.tensorflow.TensorFlowService",
      "net.imagej.tensorflow.Tensors"
    };

    for (String className : tensorFlowClasses) {
      try {
        Class<?> clazz = Class.forName(className);
        ClassLoader loader = clazz.getClassLoader();
        LOGGER.info("{}: Available (loaded by {})", className, getClassLoaderInfo(loader));
      } catch (ClassNotFoundException e) {
        LOGGER.info("{}: Not available", className);
      }
    }

    // Check TensorFlow native library path
    String libraryPath = System.getProperty("java.library.path");
    if (libraryPath != null) {
      String[] paths = libraryPath.split(System.getProperty("path.separator"));
      LOGGER.info("Native library paths: {}", Arrays.toString(paths));
    }
  }

  /**
   * Attempts to identify potential ClassCastException sources.
   */
  public static void identifyPotentialIssues() {
    LOGGER.info("=== Potential ClassCastException Issues ===");

    ClassLoader systemCL = ClassLoader.getSystemClassLoader();
    ClassLoader contextCL = Thread.currentThread().getContextClassLoader();

    // Check if system ClassLoader is URLClassLoader
    if (!(systemCL instanceof URLClassLoader)) {
      LOGGER.warn(
          "ISSUE: System ClassLoader is not URLClassLoader: {}", systemCL.getClass().getName());
      LOGGER.warn("This is the likely cause of ClassCastException in Java 9+");
    }

    // Check if context ClassLoader is URLClassLoader
    if (!(contextCL instanceof URLClassLoader)) {
      LOGGER.warn(
          "ISSUE: Context ClassLoader is not URLClassLoader: {}", contextCL.getClass().getName());
    }

    // Check Java version
    String javaVersion = System.getProperty("java.version");
    if (javaVersion.startsWith("1.8") || javaVersion.startsWith("8")) {
      LOGGER.info("Running on Java 8 - ClassLoader issues less likely");
    } else {
      LOGGER.warn("Running on Java 9+ ({}) - ClassLoader casting issues expected", javaVersion);
    }

    LOGGER.info("=== End Issue Identification ===");
  }
}
