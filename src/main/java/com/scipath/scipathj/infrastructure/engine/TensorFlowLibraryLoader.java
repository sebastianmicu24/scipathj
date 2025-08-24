package com.scipath.scipathj.infrastructure.engine;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optimized TensorFlow native library loader for Java 21.
 * Uses TensorFlow's native loading mechanism with ClassLoader fixes.
 * Previous multiple strategies removed as only one works reliably.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class TensorFlowLibraryLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(TensorFlowLibraryLoader.class);

  private static boolean libraryLoaded = false;
  private static String loadedLibraryPath = null;
  private static Throwable lastLoadException = null;


  /**
   * Attempts to load the TensorFlow native library using multiple strategies.
   *
   * @return true if the library was loaded successfully, false otherwise
   */
  public static synchronized boolean loadTensorFlowLibrary() {
   if (libraryLoaded) {
     LOGGER.trace("TensorFlow library already loaded from: {}", loadedLibraryPath);
     return true;
   }

   LOGGER.debug("Attempting to load TensorFlow native library with Java 21 compatibility");

   // Apply ClassLoader fixes before attempting to load
   Java21ClassLoaderFix.applyFix();

   // Only try the strategy that actually works: TensorFlow's native loading mechanism
   if (tryTensorFlowNativeLoad()) {
     return true;
   }

   LOGGER.error("Failed to load TensorFlow native library");
   if (lastLoadException != null) {
     LOGGER.debug("Last exception:", lastLoadException);
   }

   return false;
 }


  /**
   * Strategy 5: Try to use TensorFlow's own loading mechanism with ClassLoader fixes.
   */
  private static boolean tryTensorFlowNativeLoad() {
    LOGGER.trace("Trying TensorFlow's native loading mechanism");

    return AccessController.doPrivileged(
        new PrivilegedAction<Boolean>() {
          @Override
          public Boolean run() {
            try {
              // Try to use TensorFlow's NativeLibrary class directly
              Class<?> nativeLibClass = Class.forName("org.tensorflow.NativeLibrary");
              Method loadMethod = nativeLibClass.getDeclaredMethod("load");
              loadMethod.setAccessible(true);
              loadMethod.invoke(null);

              libraryLoaded = true;
              loadedLibraryPath = "TensorFlow NativeLibrary.load()";
              LOGGER.debug("Successfully loaded TensorFlow library using NativeLibrary.load()");
              return true;

            } catch (Exception e) {
              LOGGER.trace("Failed to load using TensorFlow NativeLibrary: {}", e.getMessage());
              lastLoadException = e;

              // Try alternative approach with reflection
              try {
                Class<?> tensorFlowClass = Class.forName("org.tensorflow.TensorFlow");
                Method versionMethod = tensorFlowClass.getDeclaredMethod("version");
                String version = (String) versionMethod.invoke(null);

                libraryLoaded = true;
                loadedLibraryPath = "TensorFlow.version() - " + version;
                LOGGER.debug("TensorFlow library appears to be loaded (version: {})", version);
                return true;

              } catch (Exception e2) {
                LOGGER.trace("Failed to verify TensorFlow loading: {}", e2.getMessage());
                lastLoadException = e2;
              }
            }

            return false;
          }
        });
  }


  /**
   * Checks if the TensorFlow library is loaded.
   */
  public static boolean isLibraryLoaded() {
    return libraryLoaded;
  }

  /**
   * Gets the path of the loaded library.
   */
  public static String getLoadedLibraryPath() {
    return loadedLibraryPath;
  }

  /**
   * Gets the last exception that occurred during loading.
   */
  public static Throwable getLastLoadException() {
    return lastLoadException;
  }

  /**
   * Gets detailed information about the TensorFlow library loading status.
   */
  public static String getLoadingStatus() {
    StringBuilder status = new StringBuilder();
    status.append("TensorFlow Library Loading Status:\n");
    status.append("- Loaded: ").append(libraryLoaded).append("\n");
    status
        .append("- Library Path: ")
        .append(loadedLibraryPath != null ? loadedLibraryPath : "N/A")
        .append("\n");
    status.append("- Java Version: ").append(System.getProperty("java.version")).append("\n");
    status
        .append("- OS: ")
        .append(System.getProperty("os.name"))
        .append(" ")
        .append(System.getProperty("os.arch"))
        .append("\n");
    status
        .append("- Library Path Property: ")
        .append(System.getProperty("java.library.path"))
        .append("\n");

    if (lastLoadException != null) {
      status.append("- Last Exception: ").append(lastLoadException.getMessage()).append("\n");
    }

    return status.toString();
  }
}
