package com.scipath.scipathj.core.engine;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java 21 compatibility fix for ClassLoader casting issues in TensorFlow/CSBDeep.
 * This class provides workarounds for the ClassCastException that occurs when
 * libraries try to cast AppClassLoader to URLClassLoader in Java 9+.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class Java21ClassLoaderFix {

  private static final Logger LOGGER = LoggerFactory.getLogger(Java21ClassLoaderFix.class);

  private static final AtomicBoolean fixApplied = new AtomicBoolean(false);
  private static URLClassLoader compatibleClassLoader = null;
  private static ClassLoader originalClassLoader = null;
  private static ClassLoader originalSystemClassLoader = null;

  /**
   * Applies the Java 21 compatibility fix for ClassLoader issues.
   * This method should be called before initializing TensorFlow/CSBDeep.
   */
  public static synchronized void applyFix() {
    if (fixApplied.get()) {
      LOGGER.debug("Java 21 ClassLoader fix already applied");
      return;
    }

    try {
      LOGGER.info("Applying Java 21 ClassLoader compatibility fix for TensorFlow/CSBDeep");

      // Store original class loaders
      originalClassLoader = Thread.currentThread().getContextClassLoader();
      originalSystemClassLoader = ClassLoader.getSystemClassLoader();

      // Check if we're running on Java 9+ where AppClassLoader is not URLClassLoader
      if (isJava9Plus() && !isURLClassLoader(originalClassLoader)) {
        LOGGER.debug(
            "Detected Java 9+ with non-URLClassLoader: {}",
            originalClassLoader.getClass().getName());

        // Create a compatible URLClassLoader wrapper
        compatibleClassLoader = createCompatibleClassLoader(originalClassLoader);

        // Set the compatible class loader as the context class loader
        Thread.currentThread().setContextClassLoader(compatibleClassLoader);

        // Try to replace the system class loader reference if possible
        replaceSystemClassLoaderReferences();

        LOGGER.info("Successfully applied Java 21 ClassLoader fix");
      } else {
        LOGGER.debug(
            "ClassLoader fix not needed - already compatible: {}",
            originalClassLoader.getClass().getName());
      }

      fixApplied.set(true);

    } catch (Exception e) {
      LOGGER.error("Failed to apply Java 21 ClassLoader fix", e);
      // Don't throw exception - let the application continue and hope for the best
    }
  }

  /**
   * Restores the original ClassLoader after TensorFlow/CSBDeep operations.
   * This should be called after TensorFlow operations are complete.
   */
  public static synchronized void restoreOriginalClassLoader() {
    if (!fixApplied.get()) {
      return;
    }

    try {
      if (originalClassLoader != null) {
        Thread.currentThread().setContextClassLoader(originalClassLoader);
        LOGGER.debug("Restored original ClassLoader: {}", originalClassLoader.getClass().getName());
      }
    } catch (Exception e) {
      LOGGER.warn("Failed to restore original ClassLoader", e);
    }
  }

  /**
   * Attempts to replace system class loader references using reflection.
   * This is a more aggressive approach to fix ClassCastException issues.
   */
  private static void replaceSystemClassLoaderReferences() {
    AccessController.doPrivileged(
        new PrivilegedAction<Void>() {
          @Override
          public Void run() {
            try {
              // Try to replace the system class loader field in ClassLoader
              Field sclField = ClassLoader.class.getDeclaredField("scl");
              sclField.setAccessible(true);

              // Only replace if it's not already a URLClassLoader
              Object currentScl = sclField.get(null);
              if (currentScl != null && !isURLClassLoader((ClassLoader) currentScl)) {
                sclField.set(null, compatibleClassLoader);
                LOGGER.debug("Replaced system class loader reference");
              }
            } catch (Exception e) {
              LOGGER.debug("Could not replace system class loader reference: {}", e.getMessage());
            }

            try {
              // Try to replace the system class loader in the current thread's context
              Field contextClassLoaderField = Thread.class.getDeclaredField("contextClassLoader");
              contextClassLoaderField.setAccessible(true);
              contextClassLoaderField.set(Thread.currentThread(), compatibleClassLoader);
              LOGGER.debug("Set thread context class loader");
            } catch (Exception e) {
              LOGGER.debug("Could not set thread context class loader: {}", e.getMessage());
            }

            return null;
          }
        });
  }

  /**
   * Creates a URLClassLoader that wraps the current ClassLoader.
   * This provides compatibility with libraries that expect URLClassLoader.
   */
  private static URLClassLoader createCompatibleClassLoader(ClassLoader parent) {
    try {
      // Try to extract URLs from the parent class loader if possible
      URL[] urls = extractURLsFromClassLoader(parent);

      if (urls == null || urls.length == 0) {
        // Fallback: create URLClassLoader with empty URL array
        // This still provides URLClassLoader interface compatibility
        urls = new URL[0];
        LOGGER.debug("Created URLClassLoader with empty URL array as fallback");
      } else {
        LOGGER.debug("Created URLClassLoader with {} URLs from parent", urls.length);
      }

      return new URLClassLoader(urls, parent);

    } catch (Exception e) {
      LOGGER.warn("Failed to create compatible URLClassLoader, using fallback", e);
      // Ultimate fallback - empty URLClassLoader
      return new URLClassLoader(new URL[0], parent);
    }
  }

  /**
   * Attempts to extract URLs from a ClassLoader using reflection.
   * This is a best-effort approach and may not work with all ClassLoader implementations.
   */
  private static URL[] extractURLsFromClassLoader(ClassLoader classLoader) {
    return AccessController.doPrivileged(
        new PrivilegedAction<URL[]>() {
          @Override
          public URL[] run() {
            try {
              // Try different approaches to get URLs

              // Approach 1: Check if it has a getURLs method (some custom loaders)
              try {
                Method getURLsMethod = classLoader.getClass().getMethod("getURLs");
                Object result = getURLsMethod.invoke(classLoader);
                if (result instanceof URL[]) {
                  return (URL[]) result;
                }
              } catch (Exception ignored) {
                // Method doesn't exist or failed
              }

              // Approach 2: Try to access ucp field (for system class loaders)
              try {
                Field ucpField = classLoader.getClass().getDeclaredField("ucp");
                ucpField.setAccessible(true);
                Object ucp = ucpField.get(classLoader);

                if (ucp != null) {
                  Method getURLsMethod = ucp.getClass().getMethod("getURLs");
                  Object result = getURLsMethod.invoke(ucp);
                  if (result instanceof URL[]) {
                    return (URL[]) result;
                  }
                }
              } catch (Exception ignored) {
                // Field doesn't exist or failed
              }

              // Approach 3: Get system class path URLs
              String classPath = System.getProperty("java.class.path");
              if (classPath != null && !classPath.isEmpty()) {
                String[] paths = classPath.split(System.getProperty("path.separator"));
                URL[] urls = new URL[paths.length];
                for (int i = 0; i < paths.length; i++) {
                  try {
                    urls[i] = new java.io.File(paths[i]).toURI().toURL();
                  } catch (Exception e) {
                    // Skip invalid paths
                    LOGGER.debug("Skipping invalid classpath entry: {}", paths[i]);
                  }
                }
                return urls;
              }

            } catch (Exception e) {
              LOGGER.debug("Failed to extract URLs from ClassLoader", e);
            }

            return new URL[0];
          }
        });
  }

  /**
   * Checks if the current JVM is Java 9 or later.
   */
  private static boolean isJava9Plus() {
    try {
      String version = System.getProperty("java.version");
      if (version.startsWith("1.")) {
        // Java 8 or earlier (1.8, 1.7, etc.)
        return false;
      } else {
        // Java 9+ (9, 10, 11, 17, 21, etc.)
        return true;
      }
    } catch (Exception e) {
      // Assume modern Java if we can't determine version
      return true;
    }
  }

  /**
   * Checks if the given ClassLoader is a URLClassLoader.
   */
  private static boolean isURLClassLoader(ClassLoader classLoader) {
    return classLoader instanceof URLClassLoader;
  }

  /**
   * Gets information about the current ClassLoader setup.
   */
  public static String getClassLoaderInfo() {
    ClassLoader current = Thread.currentThread().getContextClassLoader();
    ClassLoader system = ClassLoader.getSystemClassLoader();

    return String.format(
        "Current: %s, System: %s, Fix Applied: %s, Java Version: %s, Compatible CL: %s",
        current != null ? current.getClass().getSimpleName() : "null",
        system != null ? system.getClass().getSimpleName() : "null",
        fixApplied.get(),
        System.getProperty("java.version"),
        compatibleClassLoader != null ? compatibleClassLoader.getClass().getSimpleName() : "null");
  }

  /**
   * Forces the creation of a URLClassLoader-compatible environment.
   * This is called as a last resort when other methods fail.
   */
  public static synchronized void forceURLClassLoaderEnvironment() {
    if (fixApplied.get()) {
      return;
    }

    try {
      LOGGER.warn("Forcing URLClassLoader environment as fallback");

      // Create a minimal URLClassLoader with system classpath
      String classPath = System.getProperty("java.class.path");
      URL[] urls = new URL[0];

      if (classPath != null && !classPath.isEmpty()) {
        String[] paths = classPath.split(System.getProperty("path.separator"));
        urls = new URL[paths.length];
        for (int i = 0; i < paths.length; i++) {
          try {
            urls[i] = new java.io.File(paths[i]).toURI().toURL();
          } catch (Exception e) {
            LOGGER.debug("Skipping invalid classpath entry: {}", paths[i]);
          }
        }
      }

      // Create URLClassLoader with current class loader as parent
      ClassLoader parent = Thread.currentThread().getContextClassLoader();
      compatibleClassLoader = new URLClassLoader(urls, parent);

      // Set as context class loader
      Thread.currentThread().setContextClassLoader(compatibleClassLoader);

      fixApplied.set(true);
      LOGGER.info("Forced URLClassLoader environment created successfully");

    } catch (Exception e) {
      LOGGER.error("Failed to force URLClassLoader environment", e);
    }
  }
}
