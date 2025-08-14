package net.imagej.tensorflow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.imagej.tensorflow.util.TensorFlowUtil;
import org.scijava.Context;
import org.scijava.download.DiskLocationCache;
import org.scijava.io.location.Location;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom TensorFlow service with Java 21 compatibility fixes.
 * This service provides safe model caching and version detection.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 */
@Plugin(type = Service.class, priority = 1000.0) // Higher priority than default
public class DefaultTensorFlowService extends AbstractService implements TensorFlowService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTensorFlowService.class);

  @Parameter private Context context;

  private DiskLocationCache modelCache;
  private boolean libraryLoaded = false;
  private String tensorFlowVersion;

  @Override
  public void initialize() {
    super.initialize();
    LOGGER.debug("Initializing custom TensorFlow service with Java 21 compatibility");

    try {
      // Initialize model cache with safe directory
      initializeModelCache();

      // Get TensorFlow version safely
      this.tensorFlowVersion = TensorFlowUtil.getTensorFlowJARVersion(getClass().getClassLoader());
      LOGGER.debug("TensorFlow version detected: {}", tensorFlowVersion);

    } catch (Exception e) {
      LOGGER.warn("Error during TensorFlow service initialization", e);
    }
  }

  /**
   * Initialize model cache with a safe, writable directory.
   */
  private void initializeModelCache() {
    try {
      // Use the same directory as our SimpleHENuclearSegmentation
      String tempDir = System.getProperty("java.io.tmpdir");
      Path cacheDir = Paths.get(tempDir, "scipathj-tensorflow-models");

      // Create directory if it doesn't exist
      if (!Files.exists(cacheDir)) {
        Files.createDirectories(cacheDir);
        LOGGER.debug("Created TensorFlow model cache directory: {}", cacheDir);
      }

      // Verify directory is writable
      if (!Files.isWritable(cacheDir)) {
        LOGGER.warn("TensorFlow cache directory is not writable: {}", cacheDir);
        // Try alternative location
        cacheDir = Paths.get(System.getProperty("user.home"), ".scipathj", "tensorflow-models");
        Files.createDirectories(cacheDir);
        LOGGER.debug("Using alternative cache directory: {}", cacheDir);
      }

      // Initialize cache
      this.modelCache = new DiskLocationCache();
      this.modelCache.setBaseDirectory(cacheDir.toFile());

      LOGGER.debug("TensorFlow model cache initialized at: {}", cacheDir);

    } catch (Exception e) {
      LOGGER.error("Failed to initialize model cache", e);
      // Continue without cache - models will be loaded directly
    }
  }

  @Override
  public void loadLibrary() {
    if (libraryLoaded) {
      LOGGER.debug("TensorFlow library already loaded");
      return;
    }

    try {
      LOGGER.debug("Loading TensorFlow library with Java 21 compatibility");

      // Check if TensorFlow is available
      if (!TensorFlowUtil.isTensorFlowAvailable()) {
        throw new RuntimeException("TensorFlow not found in classpath");
      }

      // Try to load the native library
      System.loadLibrary("tensorflow_jni");
      libraryLoaded = true;

      LOGGER.info("TensorFlow library loaded successfully");

    } catch (UnsatisfiedLinkError e) {
      LOGGER.debug(
          "Could not load tensorflow_jni library directly, trying alternative approach", e);

      try {
        // Try loading TensorFlow class which should trigger native library loading
        Class<?> tfClass = Class.forName("org.tensorflow.TensorFlow");
        // If we get here, TensorFlow is available
        libraryLoaded = true;
        LOGGER.info("TensorFlow library loaded via class loading");

      } catch (ClassNotFoundException ex) {
        LOGGER.error("TensorFlow not available", ex);
        throw new RuntimeException("TensorFlow library could not be loaded", ex);
      }
    } catch (Exception e) {
      LOGGER.error("Failed to load TensorFlow library", e);
      throw new RuntimeException("TensorFlow library loading failed", e);
    }
  }

  @Override
  public CachedModelBundle loadCachedModel(Location location, String modelName, String modelUrl) {
    LOGGER.debug(
        "Loading cached model with location: {} - {} from {}", location, modelName, modelUrl);
    return loadCachedModel(modelName, modelUrl);
  }

  @Override
  public TensorFlowVersion getTensorFlowVersion() {
    return new TensorFlowVersion(
        getJarVersion(),
        isLibraryLoaded(),
        isLibraryLoaded() ? "TensorFlow loaded successfully" : "TensorFlow not loaded");
  }

  @Override
  public TensorFlowVersion getStatus() {
    return getTensorFlowVersion();
  }

  @Override
  public String getJarVersion() {
    if (tensorFlowVersion == null) {
      tensorFlowVersion = TensorFlowUtil.getTensorFlowJARVersion(getClass().getClassLoader());
    }
    return tensorFlowVersion;
  }

  @Override
  public DiskLocationCache modelCache() {
    if (modelCache == null) {
      initializeModelCache();
    }
    return modelCache;
  }

  @Override
  public File modelDir(String modelName) {
    try {
      if (modelCache != null) {
        return modelCache.getBaseDirectory();
      } else {
        // Fallback to temp directory
        String tempDir = System.getProperty("java.io.tmpdir");
        File modelDir = new File(tempDir, "scipathj-tensorflow-models");
        if (!modelDir.exists()) {
          modelDir.mkdirs();
        }
        return modelDir;
      }
    } catch (Exception e) {
      LOGGER.warn("Error getting model directory", e);
      return new File(System.getProperty("java.io.tmpdir"));
    }
  }

  @Override
  public CachedModelBundle loadCachedModel(String modelName, String modelUrl) {
    LOGGER.debug("Loading cached model: {} from {}", modelName, modelUrl);

    try {
      // Try to load the actual TensorFlow model
      LOGGER.info("Loading TensorFlow model {} from {}", modelName, modelUrl);

      // First, try to load from the model URL/path
      org.tensorflow.SavedModelBundle savedModel = null;

      // Check if modelUrl is a file path
      if (modelUrl != null && !modelUrl.isEmpty()) {
        File modelFile = new File(modelUrl);
        if (modelFile.exists() && modelFile.isDirectory()) {
          LOGGER.debug("Loading model from directory: {}", modelFile.getAbsolutePath());
          try {
            savedModel = org.tensorflow.SavedModelBundle.load(modelFile.getAbsolutePath(), "serve");
            LOGGER.info(
                "Successfully loaded SavedModelBundle from: {}", modelFile.getAbsolutePath());
          } catch (Exception e) {
            LOGGER.warn(
                "Failed to load SavedModelBundle from directory, trying alternative tags", e);
            // Try with different tags
            String[] tags = {"serve", "inference", "predict"};
            for (String tag : tags) {
              try {
                savedModel = org.tensorflow.SavedModelBundle.load(modelFile.getAbsolutePath(), tag);
                LOGGER.info(
                    "Successfully loaded SavedModelBundle with tag '{}' from: {}",
                    tag,
                    modelFile.getAbsolutePath());
                break;
              } catch (Exception tagException) {
                LOGGER.debug("Failed to load with tag '{}': {}", tag, tagException.getMessage());
              }
            }
          }
        } else if (modelUrl.startsWith("file:/")) {
          // Handle file:// URLs
          String filePath = modelUrl.substring(6); // Remove "file:/"
          File urlFile = new File(filePath);
          if (urlFile.exists()) {
            LOGGER.debug("Loading model from file URL: {}", filePath);
            try {
              savedModel = org.tensorflow.SavedModelBundle.load(urlFile.getAbsolutePath(), "serve");
              LOGGER.info("Successfully loaded SavedModelBundle from file URL: {}", filePath);
            } catch (Exception e) {
              LOGGER.warn("Failed to load SavedModelBundle from file URL", e);
            }
          }
        }
      }

      // If we couldn't load the model, try to find it in the extracted models directory
      if (savedModel == null) {
        LOGGER.debug("Trying to load model from extracted models directory");
        try {
          // Look for the extracted model in multiple possible locations
          File modelsDir = null;
          String[] possiblePaths = {
            "models", // When running from scipathj/ directory
            "scipathj/models", // When running from parent directory
            "./models", // Explicit current directory
            "./scipathj/models", // Explicit parent to scipathj
            System.getProperty("user.dir") + "/models", // Absolute current dir
            System.getProperty("user.dir") + "/scipathj/models" // Absolute scipathj dir
          };

          for (String path : possiblePaths) {
            File testDir = new File(path);
            if (testDir.exists() && testDir.isDirectory()) {
              modelsDir = testDir;
              LOGGER.debug("Found models directory at: {}", testDir.getAbsolutePath());
              break;
            }
          }

          if (modelsDir != null && modelsDir.exists()) {
            LOGGER.debug("Using models directory: {}", modelsDir.getAbsolutePath());

            // Look for the specific model directory
            // (GenericNetwork_4fe3e0afe4f0d34891e153c8fca922cf)
            File[] modelDirs = modelsDir.listFiles(File::isDirectory);
            if (modelDirs != null && modelDirs.length > 0) {
              for (File modelDir : modelDirs) {
                LOGGER.debug("Checking model directory: {}", modelDir.getName());

                // Check if this directory contains a SavedModel
                File savedModelPb = new File(modelDir, "saved_model.pb");
                File variablesDir = new File(modelDir, "variables");

                if (savedModelPb.exists() && variablesDir.exists()) {
                  LOGGER.debug("Found SavedModel structure in: {}", modelDir.getAbsolutePath());

                  try {
                    savedModel =
                        org.tensorflow.SavedModelBundle.load(modelDir.getAbsolutePath(), "serve");
                    LOGGER.info(
                        "Successfully loaded SavedModelBundle from extracted model: {}",
                        modelDir.getAbsolutePath());
                    break;
                  } catch (Exception e) {
                    LOGGER.debug("Failed to load with 'serve' tag, trying alternatives", e);
                    // Try with different tags
                    String[] tags = {"inference", "predict", "serving_default"};
                    for (String tag : tags) {
                      try {
                        savedModel =
                            org.tensorflow.SavedModelBundle.load(modelDir.getAbsolutePath(), tag);
                        LOGGER.info(
                            "Successfully loaded SavedModelBundle with tag '{}' from: {}",
                            tag,
                            modelDir.getAbsolutePath());
                        break;
                      } catch (Exception tagException) {
                        LOGGER.debug(
                            "Failed to load with tag '{}': {}", tag, tagException.getMessage());
                      }
                    }
                    if (savedModel != null) break;
                  }
                }
              }
            }
          }
        } catch (Exception e) {
          LOGGER.debug("Could not load from extracted models directory", e);
        }
      }

      // If we still couldn't load the model, try to find it in resources
      if (savedModel == null) {
        LOGGER.debug("Trying to load model from resources");
        try {
          // Look for the model in the resources directory
          String resourcePath = "/models/2D/he_heavy_augment.zip";
          java.net.URL resourceUrl = getClass().getResource(resourcePath);
          if (resourceUrl != null) {
            LOGGER.debug("Found model resource at: {}", resourceUrl);
            // TODO: Extract and load the ZIP file properly
            // For now, we'll create a bundle that indicates the model exists
            // The actual model loading will be handled by the TensorFlow framework
            CachedModelBundle bundle =
                new CachedModelBundle(modelName, resourceUrl.toString(), null, true);
            LOGGER.info("Created resource-based model bundle for: {}", modelName);
            return bundle;
          }
        } catch (Exception e) {
          LOGGER.debug("Could not load from resources", e);
        }
      }

      if (savedModel != null) {
        // Create a proper cached model bundle with the loaded model
        CachedModelBundle bundle = new CachedModelBundle(modelName, modelUrl, savedModel, true);
        LOGGER.info("Successfully created CachedModelBundle for: {}", modelName);
        return bundle;
      } else {
        LOGGER.warn("Could not load TensorFlow model from any source: {}", modelUrl);
        return null;
      }

    } catch (Exception e) {
      LOGGER.error("Error loading cached model: {}", modelName, e);
      return null;
    }
  }

  /**
   * Creates a crash file to indicate TensorFlow issues (safe implementation).
   */
  public void createCrashFile(String reason) {
    try {
      File crashFile = new File(System.getProperty("java.io.tmpdir"), ".scipathj-tensorflow-crash");
      if (!crashFile.exists()) {
        crashFile.createNewFile();
        LOGGER.debug("Created TensorFlow crash file: {}", crashFile.getAbsolutePath());
      }
    } catch (IOException e) {
      LOGGER.debug("Could not create crash file", e);
      // This is not critical, continue
    }
  }

  /**
   * Safe implementation of loadFromJAR that doesn't cause ClassCastException.
   */
  public void loadFromJAR() {
    try {
      LOGGER.debug("Loading TensorFlow from JAR with safe implementation");

      // Use our safe version detection
      String version = TensorFlowUtil.getTensorFlowJARVersion(getClass().getClassLoader());
      LOGGER.debug("Detected TensorFlow version: {}", version);

      // Try to load the library
      loadLibrary();

    } catch (Exception e) {
      LOGGER.warn("Error loading TensorFlow from JAR", e);
      throw new RuntimeException("Failed to load TensorFlow from JAR", e);
    }
  }

  /**
   * Initialize model cache with safe directory handling.
   */
  public void initModelCache() {
    initializeModelCache();
  }

  @Override
  public boolean isLibraryLoaded() {
    return libraryLoaded;
  }

  @Override
  public String toString() {
    return String.format(
        "CustomTensorFlowService[loaded=%s, version=%s, cache=%s]",
        libraryLoaded,
        tensorFlowVersion,
        modelCache != null ? modelCache.getBaseDirectory() : "none");
  }
}
