package com.scipath.scipathj.core.config;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages application configuration settings with persistence.
 * Handles loading and saving settings to/from properties files.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class ConfigurationManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationManager.class);

  // Configuration file paths
  private static final String CONFIG_DIR =
      System.getProperty("user.home") + File.separator + ".scipathj";
  private static final String VESSEL_SETTINGS_FILE = "vessel_segmentation.properties";
  private static final String NUCLEAR_SETTINGS_FILE = "nuclear_segmentation.properties";
  private static final String CYTOPLASM_SETTINGS_FILE = "cytoplasm_segmentation.properties";
  private static final String MAIN_SETTINGS_FILE = "main_settings.properties";

  /**
   * Creates a new ConfigurationManager instance.
   * Initializes the configuration directory if it doesn't exist.
   */
  public ConfigurationManager() {
    LOGGER.debug("ConfigurationManager initialized");
    ensureConfigDirectoryExists();
  }

  /**
   * Ensure the configuration directory exists.
   */
  private void ensureConfigDirectoryExists() {
    try {
      Path configPath = Paths.get(CONFIG_DIR);
      if (!Files.exists(configPath)) {
        Files.createDirectories(configPath);
        LOGGER.info("Created configuration directory: {}", CONFIG_DIR);
      }
    } catch (IOException e) {
      LOGGER.error("Failed to create configuration directory: {}", CONFIG_DIR, e);
    }
  }

  /**
   * Load vessel segmentation settings from the properties file.
   *
   * @param settings The settings object to populate
   */
  public void loadVesselSegmentationSettings(VesselSegmentationSettings settings) {
    Path settingsFile = Paths.get(CONFIG_DIR, VESSEL_SETTINGS_FILE);

    if (!Files.exists(settingsFile)) {
      LOGGER.info("Vessel segmentation settings file not found, using defaults");
      return;
    }

    Properties properties = new Properties();
    try (InputStream input = Files.newInputStream(settingsFile)) {
      properties.load(input);

      // Load threshold
      String thresholdStr = properties.getProperty("threshold");
      if (thresholdStr != null) {
        try {
          int threshold = Integer.parseInt(thresholdStr);
          settings.setThreshold(threshold);
          LOGGER.debug("Loaded threshold: {}", threshold);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid threshold value in config: {}", thresholdStr);
        }
      }

      // Load min ROI size
      String minRoiSizeStr = properties.getProperty("minRoiSize");
      if (minRoiSizeStr != null) {
        try {
          double minRoiSize = Double.parseDouble(minRoiSizeStr);
          settings.setMinRoiSize(minRoiSize);
          LOGGER.debug("Loaded min ROI size: {}", minRoiSize);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid min ROI size value in config: {}", minRoiSizeStr);
        }
      }

      // Load max ROI size
      String maxRoiSizeStr = properties.getProperty("maxRoiSize");
      if (maxRoiSizeStr != null) {
        try {
          double maxRoiSize = Double.parseDouble(maxRoiSizeStr);
          settings.setMaxRoiSize(maxRoiSize);
          LOGGER.debug("Loaded max ROI size: {}", maxRoiSize);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid max ROI size value in config: {}", maxRoiSizeStr);
        }
      }

      // Load Gaussian blur sigma
      String gaussianBlurSigmaStr = properties.getProperty("gaussianBlurSigma");
      if (gaussianBlurSigmaStr != null) {
        try {
          double gaussianBlurSigma = Double.parseDouble(gaussianBlurSigmaStr);
          settings.setGaussianBlurSigma(gaussianBlurSigma);
          LOGGER.debug("Loaded Gaussian blur sigma: {}", gaussianBlurSigma);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid Gaussian blur sigma value in config: {}", gaussianBlurSigmaStr);
        }
      }

      // Load morphological closing setting
      String applyMorphologicalClosingStr = properties.getProperty("applyMorphologicalClosing");
      if (applyMorphologicalClosingStr != null) {
        boolean applyMorphologicalClosing = Boolean.parseBoolean(applyMorphologicalClosingStr);
        settings.setApplyMorphologicalClosing(applyMorphologicalClosing);
        LOGGER.debug("Loaded apply morphological closing: {}", applyMorphologicalClosing);
      }

      LOGGER.info("Successfully loaded vessel segmentation settings from: {}", settingsFile);

    } catch (IOException e) {
      LOGGER.error("Failed to load vessel segmentation settings from: {}", settingsFile, e);
    }
  }

  /**
   * Save vessel segmentation settings to the properties file.
   *
   * @param settings The settings object to save
   */
  public void saveVesselSegmentationSettings(VesselSegmentationSettings settings) {
    Path settingsFile = Paths.get(CONFIG_DIR, VESSEL_SETTINGS_FILE);

    Properties properties = new Properties();
    properties.setProperty("threshold", String.valueOf(settings.getThreshold()));
    properties.setProperty("minRoiSize", String.valueOf(settings.getMinRoiSize()));
    properties.setProperty("maxRoiSize", String.valueOf(settings.getMaxRoiSize()));
    properties.setProperty("gaussianBlurSigma", String.valueOf(settings.getGaussianBlurSigma()));
    properties.setProperty(
        "applyMorphologicalClosing", String.valueOf(settings.isApplyMorphologicalClosing()));

    try (OutputStream output = Files.newOutputStream(settingsFile)) {
      properties.store(output, "SciPathJ Vessel Segmentation Settings");
      LOGGER.info("Successfully saved vessel segmentation settings to: {}", settingsFile);
    } catch (IOException e) {
      LOGGER.error("Failed to save vessel segmentation settings to: {}", settingsFile, e);
    }
  }

  /**
   * Initialize vessel segmentation settings by loading from file or using defaults.
   *
   * @return Initialized VesselSegmentationSettings instance
   */
  public VesselSegmentationSettings initializeVesselSegmentationSettings() {
    VesselSegmentationSettings settings = VesselSegmentationSettings.getInstance();
    loadVesselSegmentationSettings(settings);
    return settings;
  }

  /**
   * Get the configuration directory path.
   *
   * @return The configuration directory path
   */
  public String getConfigDirectory() {
    return CONFIG_DIR;
  }

  /**
   * Load main settings from the properties file.
   *
   * @param settings The settings object to populate
   */
  public void loadMainSettings(MainSettings settings) {
    Path settingsFile = Paths.get(CONFIG_DIR, MAIN_SETTINGS_FILE);

    if (!Files.exists(settingsFile)) {
      LOGGER.info("Main settings file not found, using defaults");
      return;
    }

    Properties properties = new Properties();
    try (InputStream input = Files.newInputStream(settingsFile)) {
      properties.load(input);

      // Load scale settings
      String pixelsPerMicrometerStr = properties.getProperty("pixelsPerMicrometer");
      if (pixelsPerMicrometerStr != null) {
        try {
          double pixelsPerMicrometer = Double.parseDouble(pixelsPerMicrometerStr);
          settings.setPixelsPerMicrometer(pixelsPerMicrometer);
          LOGGER.debug("Loaded pixels per micrometer: {}", pixelsPerMicrometer);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid pixels per micrometer value in config: {}", pixelsPerMicrometerStr);
        }
      }

      String scaleUnit = properties.getProperty("scaleUnit");
      if (scaleUnit != null) {
        settings.setScaleUnit(scaleUnit);
        LOGGER.debug("Loaded scale unit: {}", scaleUnit);
      }

      // Load type-specific ROI appearance settings
      loadROICategorySettings(properties, "vessel", settings.getVesselSettings());
      loadROICategorySettings(properties, "nucleus", settings.getNucleusSettings());
      loadROICategorySettings(properties, "cytoplasm", settings.getCytoplasmSettings());
      loadROICategorySettings(properties, "cell", settings.getCellSettings());

      // For backward compatibility, also load old single ROI settings into vessel settings
      String roiBorderColorStr = properties.getProperty("roiBorderColor");
      if (roiBorderColorStr != null) {
        try {
          Color roiBorderColor = parseColor(roiBorderColorStr);
          settings.getVesselSettings().setBorderColor(roiBorderColor);
          LOGGER.debug("Loaded legacy ROI border color: {}", roiBorderColorStr);
        } catch (Exception e) {
          LOGGER.warn("Invalid ROI border color value in config: {}", roiBorderColorStr);
        }
      }

      String roiFillOpacityStr = properties.getProperty("roiFillOpacity");
      if (roiFillOpacityStr != null) {
        try {
          float roiFillOpacity = Float.parseFloat(roiFillOpacityStr);
          settings.getVesselSettings().setFillOpacity(roiFillOpacity);
          LOGGER.debug("Loaded legacy ROI fill opacity: {}", roiFillOpacity);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid ROI fill opacity value in config: {}", roiFillOpacityStr);
        }
      }

      String roiBorderWidthStr = properties.getProperty("roiBorderWidth");
      if (roiBorderWidthStr != null) {
        try {
          int roiBorderWidth = Integer.parseInt(roiBorderWidthStr);
          settings.getVesselSettings().setBorderWidth(roiBorderWidth);
          LOGGER.debug("Loaded legacy ROI border width: {}", roiBorderWidth);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid ROI border width value in config: {}", roiBorderWidthStr);
        }
      }

      LOGGER.info("Successfully loaded main settings from: {}", settingsFile);

    } catch (IOException e) {
      LOGGER.error("Failed to load main settings from: {}", settingsFile, e);
    }
  }

  /**
   * Save main settings to the properties file.
   *
   * @param settings The settings object to save
   */
  public void saveMainSettings(MainSettings settings) {
    Path settingsFile = Paths.get(CONFIG_DIR, MAIN_SETTINGS_FILE);

    Properties properties = new Properties();

    // Save scale settings
    properties.setProperty(
        "pixelsPerMicrometer", String.valueOf(settings.getPixelsPerMicrometer()));
    properties.setProperty("scaleUnit", settings.getScaleUnit());

    // Save type-specific ROI appearance settings
    saveROICategorySettings(properties, "vessel", settings.getVesselSettings());
    saveROICategorySettings(properties, "nucleus", settings.getNucleusSettings());
    saveROICategorySettings(properties, "cytoplasm", settings.getCytoplasmSettings());
    saveROICategorySettings(properties, "cell", settings.getCellSettings());

    // For backward compatibility, also save vessel ROI settings as legacy properties
    properties.setProperty(
        "roiBorderColor", colorToString(settings.getVesselSettings().getBorderColor()));
    properties.setProperty(
        "roiFillOpacity", String.valueOf(settings.getVesselSettings().getFillOpacity()));
    properties.setProperty(
        "roiBorderWidth", String.valueOf(settings.getVesselSettings().getBorderWidth()));

    try (OutputStream output = Files.newOutputStream(settingsFile)) {
      properties.store(output, "SciPathJ Main Settings");
      LOGGER.info("Successfully saved main settings to: {}", settingsFile);
    } catch (IOException e) {
      LOGGER.error("Failed to save main settings to: {}", settingsFile, e);
    }
  }

  /**
   * Load ROI category settings from properties.
   *
   * @param properties The properties object
   * @param categoryPrefix The category prefix (e.g., "vessel", "nucleus")
   * @param settings The ROI appearance settings to populate
   */
  private void loadROICategorySettings(
      Properties properties, String categoryPrefix, MainSettings.ROIAppearanceSettings settings) {
    // Load border color
    String borderColorStr = properties.getProperty(categoryPrefix + ".borderColor");
    if (borderColorStr != null) {
      try {
        Color borderColor = parseColor(borderColorStr);
        settings.setBorderColor(borderColor);
        LOGGER.debug("Loaded {} border color: {}", categoryPrefix, borderColorStr);
      } catch (Exception e) {
        LOGGER.warn("Invalid {} border color value in config: {}", categoryPrefix, borderColorStr);
      }
    }

    // Load fill opacity
    String fillOpacityStr = properties.getProperty(categoryPrefix + ".fillOpacity");
    if (fillOpacityStr != null) {
      try {
        float fillOpacity = Float.parseFloat(fillOpacityStr);
        settings.setFillOpacity(fillOpacity);
        LOGGER.debug("Loaded {} fill opacity: {}", categoryPrefix, fillOpacity);
      } catch (NumberFormatException e) {
        LOGGER.warn("Invalid {} fill opacity value in config: {}", categoryPrefix, fillOpacityStr);
      }
    }

    // Load border width
    String borderWidthStr = properties.getProperty(categoryPrefix + ".borderWidth");
    if (borderWidthStr != null) {
      try {
        int borderWidth = Integer.parseInt(borderWidthStr);
        settings.setBorderWidth(borderWidth);
        LOGGER.debug("Loaded {} border width: {}", categoryPrefix, borderWidth);
      } catch (NumberFormatException e) {
        LOGGER.warn("Invalid {} border width value in config: {}", categoryPrefix, borderWidthStr);
      }
    }
  }

  /**
   * Save ROI category settings to properties.
   *
   * @param properties The properties object
   * @param categoryPrefix The category prefix (e.g., "vessel", "nucleus")
   * @param settings The ROI appearance settings to save
   */
  private void saveROICategorySettings(
      Properties properties, String categoryPrefix, MainSettings.ROIAppearanceSettings settings) {
    properties.setProperty(
        categoryPrefix + ".borderColor", colorToString(settings.getBorderColor()));
    properties.setProperty(
        categoryPrefix + ".fillOpacity", String.valueOf(settings.getFillOpacity()));
    properties.setProperty(
        categoryPrefix + ".borderWidth", String.valueOf(settings.getBorderWidth()));
  }

  /**
   * Initialize main settings by loading from file or using defaults.
   *
   * @return Initialized MainSettings instance
   */
  public MainSettings initializeMainSettings() {
    MainSettings settings = MainSettings.getInstance();
    loadMainSettings(settings);
    return settings;
  }

  /**
   * Parse a color from string format "R,G,B" or "R,G,B,A".
   *
   * @param colorStr The color string
   * @return The parsed Color object
   */
  private Color parseColor(String colorStr) {
    String[] parts = colorStr.split(",");
    if (parts.length == 3) {
      int r = Integer.parseInt(parts[0].trim());
      int g = Integer.parseInt(parts[1].trim());
      int b = Integer.parseInt(parts[2].trim());
      return new Color(r, g, b);
    } else if (parts.length == 4) {
      int r = Integer.parseInt(parts[0].trim());
      int g = Integer.parseInt(parts[1].trim());
      int b = Integer.parseInt(parts[2].trim());
      int a = Integer.parseInt(parts[3].trim());
      return new Color(r, g, b, a);
    } else {
      throw new IllegalArgumentException("Invalid color format: " + colorStr);
    }
  }

  /**
   * Convert a color to string format "R,G,B".
   *
   * @param color The color to convert
   * @return The color string
   */
  private String colorToString(Color color) {
    return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
  }

  /**
   * Check if vessel segmentation settings file exists.
   *
   * @return true if the settings file exists
   */
  public boolean vesselSettingsFileExists() {
    Path settingsFile = Paths.get(CONFIG_DIR, VESSEL_SETTINGS_FILE);
    return Files.exists(settingsFile);
  }

  /**
   * Check if main settings file exists.
   *
   * @return true if the settings file exists
   */
  public boolean mainSettingsFileExists() {
    Path settingsFile = Paths.get(CONFIG_DIR, MAIN_SETTINGS_FILE);
    return Files.exists(settingsFile);
  }

  /**
   * Load nuclear segmentation settings from the properties file.
   *
   * @param settings The settings object to populate
   */
  public void loadNuclearSegmentationSettings(NuclearSegmentationSettings settings) {
    Path settingsFile = Paths.get(CONFIG_DIR, NUCLEAR_SETTINGS_FILE);

    if (!Files.exists(settingsFile)) {
      LOGGER.info("Nuclear segmentation settings file not found, using defaults");
      return;
    }

    Properties properties = new Properties();
    try (InputStream input = Files.newInputStream(settingsFile)) {
      properties.load(input);

      // Load StarDist model choice
      String modelChoice = properties.getProperty("modelChoice");
      if (modelChoice != null) {
        settings.setModelChoice(modelChoice);
        LOGGER.debug("Loaded model choice: {}", modelChoice);
      }

      // Load normalization setting
      String normalizeInputStr = properties.getProperty("normalizeInput");
      if (normalizeInputStr != null) {
        boolean normalizeInput = Boolean.parseBoolean(normalizeInputStr);
        settings.setNormalizeInput(normalizeInput);
        LOGGER.debug("Loaded normalize input: {}", normalizeInput);
      }

      // Load percentile settings
      String percentileBottomStr = properties.getProperty("percentileBottom");
      if (percentileBottomStr != null) {
        try {
          float percentileBottom = Float.parseFloat(percentileBottomStr);
          settings.setPercentileBottom(percentileBottom);
          LOGGER.debug("Loaded percentile bottom: {}", percentileBottom);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid percentile bottom value in config: {}", percentileBottomStr);
        }
      }

      String percentileTopStr = properties.getProperty("percentileTop");
      if (percentileTopStr != null) {
        try {
          float percentileTop = Float.parseFloat(percentileTopStr);
          settings.setPercentileTop(percentileTop);
          LOGGER.debug("Loaded percentile top: {}", percentileTop);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid percentile top value in config: {}", percentileTopStr);
        }
      }

      // Load probability threshold
      String probThreshStr = properties.getProperty("probThresh");
      if (probThreshStr != null) {
        try {
          float probThresh = Float.parseFloat(probThreshStr);
          settings.setProbThresh(probThresh);
          LOGGER.debug("Loaded probability threshold: {}", probThresh);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid probability threshold value in config: {}", probThreshStr);
        }
      }

      // Load NMS threshold
      String nmsThreshStr = properties.getProperty("nmsThresh");
      if (nmsThreshStr != null) {
        try {
          float nmsThresh = Float.parseFloat(nmsThreshStr);
          settings.setNmsThresh(nmsThresh);
          LOGGER.debug("Loaded NMS threshold: {}", nmsThresh);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid NMS threshold value in config: {}", nmsThreshStr);
        }
      }

      // Load number of tiles
      String nTilesStr = properties.getProperty("nTiles");
      if (nTilesStr != null) {
        try {
          int nTiles = Integer.parseInt(nTilesStr);
          settings.setNTiles(nTiles);
          LOGGER.debug("Loaded number of tiles: {}", nTiles);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid number of tiles value in config: {}", nTilesStr);
        }
      }

      // Load nucleus size constraints
      String minNucleusSizeStr = properties.getProperty("minNucleusSize");
      if (minNucleusSizeStr != null) {
        try {
          double minNucleusSize = Double.parseDouble(minNucleusSizeStr);
          settings.setMinNucleusSize(minNucleusSize);
          LOGGER.debug("Loaded min nucleus size: {}", minNucleusSize);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid min nucleus size value in config: {}", minNucleusSizeStr);
        }
      }

      String maxNucleusSizeStr = properties.getProperty("maxNucleusSize");
      if (maxNucleusSizeStr != null) {
        try {
          double maxNucleusSize = Double.parseDouble(maxNucleusSizeStr);
          settings.setMaxNucleusSize(maxNucleusSize);
          LOGGER.debug("Loaded max nucleus size: {}", maxNucleusSize);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid max nucleus size value in config: {}", maxNucleusSizeStr);
        }
      }

      LOGGER.info("Successfully loaded nuclear segmentation settings from: {}", settingsFile);

    } catch (IOException e) {
      LOGGER.error("Failed to load nuclear segmentation settings from: {}", settingsFile, e);
    }
  }

  /**
   * Save nuclear segmentation settings to the properties file.
   *
   * @param settings The settings object to save
   */
  public void saveNuclearSegmentationSettings(NuclearSegmentationSettings settings) {
    Path settingsFile = Paths.get(CONFIG_DIR, NUCLEAR_SETTINGS_FILE);

    Properties properties = new Properties();
    properties.setProperty("modelChoice", settings.getModelChoice());
    properties.setProperty("normalizeInput", String.valueOf(settings.isNormalizeInput()));
    properties.setProperty("percentileBottom", String.valueOf(settings.getPercentileBottom()));
    properties.setProperty("percentileTop", String.valueOf(settings.getPercentileTop()));
    properties.setProperty("probThresh", String.valueOf(settings.getProbThresh()));
    properties.setProperty("nmsThresh", String.valueOf(settings.getNmsThresh()));
    properties.setProperty("outputType", settings.getOutputType());
    properties.setProperty("nTiles", String.valueOf(settings.getNTiles()));
    properties.setProperty("excludeBoundary", String.valueOf(settings.getExcludeBoundary()));
    properties.setProperty("roiPosition", settings.getRoiPosition());
    properties.setProperty("verbose", String.valueOf(settings.isVerbose()));
    properties.setProperty("showCsbdeepProgress", String.valueOf(settings.isShowCsbdeepProgress()));
    properties.setProperty("showProbAndDist", String.valueOf(settings.isShowProbAndDist()));
    properties.setProperty("minNucleusSize", String.valueOf(settings.getMinNucleusSize()));
    properties.setProperty("maxNucleusSize", String.valueOf(settings.getMaxNucleusSize()));

    try (OutputStream output = Files.newOutputStream(settingsFile)) {
      properties.store(output, "SciPathJ Nuclear Segmentation Settings");
      LOGGER.info("Successfully saved nuclear segmentation settings to: {}", settingsFile);
    } catch (IOException e) {
      LOGGER.error("Failed to save nuclear segmentation settings to: {}", settingsFile, e);
    }
  }

  /**
   * Initialize nuclear segmentation settings by loading from file or using defaults.
   *
   * @return Initialized NuclearSegmentationSettings instance
   */
  public NuclearSegmentationSettings initializeNuclearSegmentationSettings() {
    NuclearSegmentationSettings settings = new NuclearSegmentationSettings();
    loadNuclearSegmentationSettings(settings);
    return settings;
  }

  /**
   * Check if nuclear segmentation settings file exists.
   *
   * @return true if the settings file exists
   */
  public boolean nuclearSettingsFileExists() {
    Path settingsFile = Paths.get(CONFIG_DIR, NUCLEAR_SETTINGS_FILE);
    return Files.exists(settingsFile);
  }

  /**
   * Load cytoplasm segmentation settings from the properties file.
   *
   * @param settings The settings object to populate
   */
  public void loadCytoplasmSegmentationSettings(CytoplasmSegmentationSettings settings) {
    Path settingsFile = Paths.get(CONFIG_DIR, CYTOPLASM_SETTINGS_FILE);

    if (!Files.exists(settingsFile)) {
      LOGGER.info("Cytoplasm segmentation settings file not found, using defaults");
      return;
    }

    Properties properties = new Properties();
    try (InputStream input = Files.newInputStream(settingsFile)) {
      properties.load(input);

      // Load exclude vessels setting
      String excludeVesselsStr = properties.getProperty("excludeVessels");
      if (excludeVesselsStr != null) {
        boolean excludeVessels = Boolean.parseBoolean(excludeVesselsStr);
        settings.setExcludeVessels(excludeVessels);
        LOGGER.debug("Loaded exclude vessels: {}", excludeVessels);
      }

      // Load minimum cell size
      String minCellSizeStr = properties.getProperty("minCellSize");
      if (minCellSizeStr != null) {
        try {
          double minCellSize = Double.parseDouble(minCellSizeStr);
          settings.setMinCellSize(minCellSize);
          LOGGER.debug("Loaded min cell size: {}", minCellSize);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid min cell size value in config: {}", minCellSizeStr);
        }
      }

      // Load maximum cell size
      String maxCellSizeStr = properties.getProperty("maxCellSize");
      if (maxCellSizeStr != null) {
        try {
          double maxCellSize = Double.parseDouble(maxCellSizeStr);
          settings.setMaxCellSize(maxCellSize);
          LOGGER.debug("Loaded max cell size: {}", maxCellSize);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid max cell size value in config: {}", maxCellSizeStr);
        }
      }

      // Load maximum aspect ratio
      String maxAspectRatioStr = properties.getProperty("maxAspectRatio");
      if (maxAspectRatioStr != null) {
        try {
          double maxAspectRatio = Double.parseDouble(maxAspectRatioStr);
          settings.setMaxAspectRatio(maxAspectRatio);
          LOGGER.debug("Loaded max aspect ratio: {}", maxAspectRatio);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid max aspect ratio value in config: {}", maxAspectRatioStr);
        }
      }

      // Load minimum cytoplasm area
      String minCytoplasmAreaStr = properties.getProperty("minCytoplasmArea");
      if (minCytoplasmAreaStr != null) {
        try {
          double minCytoplasmArea = Double.parseDouble(minCytoplasmAreaStr);
          settings.setMinCytoplasmArea(minCytoplasmArea);
          LOGGER.debug("Loaded min cytoplasm area: {}", minCytoplasmArea);
        } catch (NumberFormatException e) {
          LOGGER.warn("Invalid min cytoplasm area value in config: {}", minCytoplasmAreaStr);
        }
      }

      LOGGER.info("Successfully loaded cytoplasm segmentation settings from: {}", settingsFile);

    } catch (IOException e) {
      LOGGER.error("Failed to load cytoplasm segmentation settings from: {}", settingsFile, e);
    }
  }

  /**
   * Save cytoplasm segmentation settings to the properties file.
   *
   * @param settings The settings object to save
   */
  public void saveCytoplasmSegmentationSettings(CytoplasmSegmentationSettings settings) {
    Path settingsFile = Paths.get(CONFIG_DIR, CYTOPLASM_SETTINGS_FILE);

    Properties properties = new Properties();
    properties.setProperty("excludeVessels", String.valueOf(settings.isExcludeVessels()));
    properties.setProperty("minCellSize", String.valueOf(settings.getMinCellSize()));
    properties.setProperty("maxCellSize", String.valueOf(settings.getMaxCellSize()));
    properties.setProperty("maxAspectRatio", String.valueOf(settings.getMaxAspectRatio()));
    properties.setProperty("minCytoplasmArea", String.valueOf(settings.getMinCytoplasmArea()));

    try (OutputStream output = Files.newOutputStream(settingsFile)) {
      properties.store(output, "SciPathJ Cytoplasm Segmentation Settings");
      LOGGER.info("Successfully saved cytoplasm segmentation settings to: {}", settingsFile);
    } catch (IOException e) {
      LOGGER.error("Failed to save cytoplasm segmentation settings to: {}", settingsFile, e);
    }
  }

  /**
   * Initialize cytoplasm segmentation settings by loading from file or using defaults.
   *
   * @return Initialized CytoplasmSegmentationSettings instance
   */
  public CytoplasmSegmentationSettings initializeCytoplasmSegmentationSettings() {
    CytoplasmSegmentationSettings settings = CytoplasmSegmentationSettings.getInstance();
    loadCytoplasmSegmentationSettings(settings);
    return settings;
  }

  /**
   * Check if cytoplasm segmentation settings file exists.
   *
   * @return true if the settings file exists
   */
  public boolean cytoplasmSettingsFileExists() {
    Path settingsFile = Paths.get(CONFIG_DIR, CYTOPLASM_SETTINGS_FILE);
    return Files.exists(settingsFile);
  }
}
