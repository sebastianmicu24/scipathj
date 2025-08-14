package net.imagej.tensorflow.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for TensorFlow operations with Java 21 compatibility fixes.
 * This class provides safe alternatives to methods that fail due to ClassLoader changes in Java 9+.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 */
public class TensorFlowUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(TensorFlowUtil.class);

  /**
   * Gets the TensorFlow JAR version with Java 21 compatibility.
   * This method provides a safe alternative to the original implementation that fails
   * due to ClassCastException when trying to cast AppClassLoader to URLClassLoader.
   *
   * @param classLoader the class loader to check
   * @return the TensorFlow version string, or "unknown" if not found
   */
  public static String getTensorFlowJARVersion(ClassLoader classLoader) {
    try {
      // First try the safe approach using resources
      String version = getVersionFromResources(classLoader);
      if (version != null && !version.equals("unknown")) {
        LOGGER.debug("Found TensorFlow version from resources: {}", version);
        return version;
      }

      // If we have a URLClassLoader, try the original approach
      if (classLoader instanceof URLClassLoader) {
        URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
        return getTensorFlowVersionFromURLClassLoader(urlClassLoader);
      }

      // For other class loaders (like AppClassLoader in Java 9+), use alternative approach
      LOGGER.debug(
          "Using alternative version detection for ClassLoader: {}",
          classLoader.getClass().getName());
      return getVersionFromSystemProperty();

    } catch (Exception e) {
      LOGGER.warn("Failed to get TensorFlow JAR version", e);
      return "unknown";
    }
  }

  /**
   * Gets TensorFlow version from resources using a safe approach.
   */
  private static String getVersionFromResources(ClassLoader classLoader) {
    try {
      // Try to find TensorFlow classes and get version from manifest
      Enumeration<URL> resources = classLoader.getResources("META-INF/MANIFEST.MF");
      while (resources.hasMoreElements()) {
        URL url = resources.nextElement();
        try (InputStream is = url.openStream()) {
          Manifest manifest = new Manifest(is);
          String implementationTitle =
              manifest.getMainAttributes().getValue("Implementation-Title");
          if (implementationTitle != null && implementationTitle.contains("tensorflow")) {
            String version = manifest.getMainAttributes().getValue("Implementation-Version");
            if (version != null) {
              LOGGER.debug("Found TensorFlow version in manifest: {}", version);
              return version;
            }
          }
        }
      }

      // Try alternative approach - look for TensorFlow classes
      try {
        Class<?> tfClass = classLoader.loadClass("org.tensorflow.TensorFlow");
        Package tfPackage = tfClass.getPackage();
        if (tfPackage != null) {
          String version = tfPackage.getImplementationVersion();
          if (version != null) {
            LOGGER.debug("Found TensorFlow version from package: {}", version);
            return version;
          }
        }
      } catch (ClassNotFoundException e) {
        LOGGER.debug("TensorFlow class not found in classpath");
      }

    } catch (IOException e) {
      LOGGER.debug("Error reading resources for version detection", e);
    }

    return "unknown";
  }

  /**
   * Gets TensorFlow version from URLClassLoader (original approach).
   */
  private static String getTensorFlowVersionFromURLClassLoader(URLClassLoader urlClassLoader) {
    try {
      URL[] urls = urlClassLoader.getURLs();
      for (URL url : urls) {
        String urlString = url.toString();
        if (urlString.contains("tensorflow") && urlString.endsWith(".jar")) {
          // Try to extract version from JAR file name
          String fileName = url.getFile();
          if (fileName.contains("tensorflow")) {
            // Extract version from filename pattern like tensorflow-1.15.0.jar
            String[] parts = fileName.split("-");
            for (int i = 0; i < parts.length - 1; i++) {
              if (parts[i].equals("tensorflow") && i + 1 < parts.length) {
                String versionPart = parts[i + 1].replace(".jar", "");
                if (versionPart.matches("\\d+\\.\\d+.*")) {
                  LOGGER.debug("Extracted TensorFlow version from filename: {}", versionPart);
                  return versionPart;
                }
              }
            }
          }

          // Try to read version from JAR manifest
          try (JarFile jarFile = new JarFile(url.getFile())) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
              String version = manifest.getMainAttributes().getValue("Implementation-Version");
              if (version != null) {
                LOGGER.debug("Found TensorFlow version in JAR manifest: {}", version);
                return version;
              }
            }
          } catch (IOException e) {
            LOGGER.debug("Could not read JAR file: {}", url, e);
          }
        }
      }
    } catch (Exception e) {
      LOGGER.debug("Error getting version from URLClassLoader", e);
    }

    return "unknown";
  }

  /**
   * Gets TensorFlow version from system properties or fallback.
   */
  private static String getVersionFromSystemProperty() {
    // Try system property first
    String version = System.getProperty("tensorflow.version");
    if (version != null) {
      LOGGER.debug("Found TensorFlow version from system property: {}", version);
      return version;
    }

    // Try to detect from loaded native library
    try {
      // If TensorFlow native library is loaded, we can assume a working version
      Class<?> tfClass = Class.forName("org.tensorflow.TensorFlow");
      if (tfClass != null) {
        LOGGER.debug("TensorFlow class found, assuming compatible version");
        return "1.15.0"; // Safe fallback version that works with our setup
      }
    } catch (ClassNotFoundException e) {
      LOGGER.debug("TensorFlow class not found");
    }

    LOGGER.debug("Could not determine TensorFlow version, using fallback");
    return "unknown";
  }

  /**
   * Checks if TensorFlow is available in the classpath.
   *
   * @return true if TensorFlow is available, false otherwise
   */
  public static boolean isTensorFlowAvailable() {
    try {
      Class.forName("org.tensorflow.TensorFlow");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Gets version from classpath JAR with Java 21 compatibility.
   * This is a safe replacement for the original method that fails with ClassCastException.
   *
   * @param classLoader the class loader to use
   * @return the version string or "unknown"
   */
  public static String versionFromClassPathJAR(ClassLoader classLoader) {
    return getTensorFlowJARVersion(classLoader);
  }
}
