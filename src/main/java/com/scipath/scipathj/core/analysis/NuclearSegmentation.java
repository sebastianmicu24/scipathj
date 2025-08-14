package com.scipath.scipathj.core.analysis;

import com.scipath.scipathj.core.config.ConfigurationManager;
import com.scipath.scipathj.core.config.NuclearSegmentationSettings;
import com.scipath.scipathj.data.model.NucleusROI;
import com.scipath.scipathj.ui.components.ROIManager;
import de.csbdresden.stardist.StarDist2D;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.scijava.Context;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Step 2 of the analysis pipeline: Nuclear Segmentation.
 *
 * H&E nuclear segmentation using StarDist's built-in Versatile H&E model.
 * This implementation uses StarDist's model choice mechanism and follows
 * the Single Responsibility Principle by focusing solely on nuclear segmentation.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class NuclearSegmentation implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(NuclearSegmentation.class);
  private static final int STARDIST_TIMEOUT_SECONDS = 60;

  private final ImagePlus originalImage;
  private final String imageFileName;
  private final ROIManager roiManager;
  private final NuclearSegmentationSettings settings;
  private Context context;
  private CommandService commandService;
  private DatasetService datasetService;

  /**
   * Constructor for NuclearSegmentation with default settings.
   * This constructor follows Dependency Injection principles.
   *
   * @param configurationManager The configuration manager instance
   * @param originalImage The original image to segment
   * @param imageFileName The filename of the image for ROI association
   * @param roiManager The ROI manager instance
   */
  public NuclearSegmentation(
      ConfigurationManager configurationManager,
      ImagePlus originalImage,
      String imageFileName,
      ROIManager roiManager) {
    this(
        configurationManager,
        originalImage,
        imageFileName,
        configurationManager.initializeNuclearSegmentationSettings(),
        roiManager);
  }

  /**
   * Constructor with custom settings following Dependency Injection principles.
   *
   * @param configurationManager The configuration manager instance
   * @param originalImage The original image to segment
   * @param imageFileName The filename of the image for ROI association
   * @param settings Custom nuclear segmentation settings
   * @param roiManager The ROI manager instance
   */
  public NuclearSegmentation(
      ConfigurationManager configurationManager,
      ImagePlus originalImage,
      String imageFileName,
      NuclearSegmentationSettings settings,
      ROIManager roiManager) {
    this.originalImage = originalImage;
    this.imageFileName = imageFileName;
    this.roiManager = roiManager;
    this.settings = settings;

    initializeContext();
  }

  /**
   * Initialize minimal SciJava context for StarDist.
   */
  private void initializeContext() {
    try {
      LOGGER.info("Initializing SciJava context for nuclear segmentation");

      // Create a minimal SciJava context with required services
      this.context =
          new Context(
              CommandService.class,
              DatasetService.class,
              org.scijava.app.StatusService.class,
              org.scijava.log.LogService.class,
              org.scijava.thread.ThreadService.class,
              org.scijava.plugin.PluginService.class,
              org.scijava.convert.ConvertService.class,
              org.scijava.module.ModuleService.class,
              net.imagej.tensorflow.TensorFlowService.class,
              org.scijava.ui.UIService.class,
              net.imagej.lut.LUTService.class,
              net.imagej.ops.OpService.class,
              org.scijava.prefs.PrefService.class,
              org.scijava.io.IOService.class,
              org.scijava.display.DisplayService.class,
              org.scijava.parse.ParseService.class,
              org.scijava.object.ObjectService.class,
              net.imagej.display.ImageDisplayService.class,
              net.imagej.types.DataTypeService.class,
              org.scijava.app.AppService.class,
              org.scijava.event.EventService.class);

      this.commandService = context.getService(CommandService.class);
      this.datasetService = context.getService(DatasetService.class);

      LOGGER.info(
          "SciJava context initialized successfully with {} services",
          context.getServiceIndex().size());

    } catch (Exception e) {
      LOGGER.error("Failed to initialize SciJava context", e);
      throw new NuclearSegmentationException("Failed to initialize context: " + e.getMessage(), e);
    }
  }

  /**
   * Segment nuclei using StarDist H&E model.
   *
   * @return List of NucleusROI objects representing detected nuclei
   * @throws NuclearSegmentationException if segmentation fails
   */
  public List<NucleusROI> segmentNuclei() throws NuclearSegmentationException {
    LOGGER.info("Starting nuclear segmentation for image '{}'", imageFileName);

    if (originalImage == null) {
      throw new NuclearSegmentationException("Original image is null for file: " + imageFileName);
    }

    try {
      // Check StarDist availability
      checkStarDistAvailability();

      // Convert ImagePlus to Dataset
      Dataset inputDataset = convertToDataset(originalImage);

      // Execute StarDist with H&E model
      List<NucleusROI> nucleiROIs = executeStarDistHE(inputDataset);

      // Add to ROI manager
      nucleiROIs.forEach(roiManager::addROI);

      LOGGER.info("Nuclear segmentation completed. Found {} nuclei", nucleiROIs.size());
      return nucleiROIs;

    } catch (Exception e) {
      LOGGER.error("StarDist nuclear segmentation failed for image: {}", imageFileName, e);
      throw new NuclearSegmentationException("Nuclear segmentation failed for " + imageFileName, e);
    }
  }

  /**
   * Check if StarDist and CSBDeep are available.
   */
  private void checkStarDistAvailability() throws NuclearSegmentationException {
    try {
      Class.forName("de.csbdresden.stardist.StarDist2D");
      Class.forName("de.csbdresden.csbdeep.commands.GenericNetwork");

      // Check TensorFlow availability
      Class<?> tfClass = Class.forName("org.tensorflow.TensorFlow");
      tfClass.getMethod("version").invoke(null);

    } catch (ClassNotFoundException e) {
      throw new NuclearSegmentationException(
          "StarDist or CSBDeep plugin not found: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new NuclearSegmentationException("TensorFlow validation failed: " + e.getMessage(), e);
    }
  }

  /**
   * Convert ImagePlus to Dataset.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private Dataset convertToDataset(ImagePlus imagePlus) throws NuclearSegmentationException {
    try {
      // Prepare image for StarDist (8-bit preferred)
      ImagePlus processedImage = prepareImageForStarDist(imagePlus);

      // Convert to ImgLib2
      Img img = ImageJFunctions.wrapReal(processedImage);

      // Create axes
      AxisType[] axes = {Axes.X, Axes.Y};
      if (processedImage.getNChannels() > 1) {
        axes = new AxisType[] {Axes.X, Axes.Y, Axes.CHANNEL};
      }

      ImgPlus imgPlus = new ImgPlus(img, processedImage.getTitle(), axes);
      Dataset dataset = datasetService.create(imgPlus);

      return dataset;

    } catch (Exception e) {
      throw new NuclearSegmentationException("Image conversion failed: " + e.getMessage(), e);
    }
  }

  /**
   * Prepare image for StarDist H&E model (ensure proper RGB format compatible with ImgLib2).
   */
  private ImagePlus prepareImageForStarDist(ImagePlus imagePlus) {
    // The H&E model needs RGB input, but ImgLib2 doesn't support 24-bit RGB directly
    // We need to convert RGB to separate 8-bit channels that ImgLib2 can handle

    if (imagePlus.getBitDepth() == 24 || imagePlus.getType() == ImagePlus.COLOR_RGB) {
      LOGGER.info("Converting 24-bit RGB image to 8-bit RGB stack for ImgLib2 compatibility");
      return convertRGBToChannelStack(imagePlus);
    }

    // For grayscale images, convert to 8-bit if needed
    if (imagePlus.getBitDepth() != 8) {
      LOGGER.info(
          "Converting {}-bit image to 8-bit for ImgLib2 compatibility", imagePlus.getBitDepth());
      ImagePlus converted = imagePlus.duplicate();
      converted.setTitle(imagePlus.getTitle() + "_8bit");
      new ij.process.ImageConverter(converted).convertToGray8();
      return converted;
    }

    // Already 8-bit, return as is
    return imagePlus;
  }

  /**
   * Convert RGB image to a 3-channel 8-bit stack that ImgLib2 can handle.
   */
  private ImagePlus convertRGBToChannelStack(ImagePlus rgbImage) {
    try {
      // Ensure we have an RGB image
      ImagePlus rgb = rgbImage.duplicate();
      if (!(rgb.getProcessor() instanceof ij.process.ColorProcessor)) {
        new ij.process.ImageConverter(rgb).convertToRGB();
      }

      // Split RGB channels
      ij.plugin.ChannelSplitter splitter = new ij.plugin.ChannelSplitter();
      ImagePlus[] channels = splitter.split(rgb);

      if (channels.length != 3) {
        LOGGER.warn("Expected 3 RGB channels, got {}", channels.length);
        // Fallback: convert to grayscale
        ImagePlus gray = rgb.duplicate();
        new ij.process.ImageConverter(gray).convertToGray8();
        return gray;
      }

      // Create a 3-channel stack
      ij.ImageStack stack = new ij.ImageStack(rgb.getWidth(), rgb.getHeight());
      stack.addSlice("Red", channels[0].getProcessor());
      stack.addSlice("Green", channels[1].getProcessor());
      stack.addSlice("Blue", channels[2].getProcessor());

      ImagePlus result = new ImagePlus(rgb.getTitle() + "_RGB_Stack", stack);
      result.setDimensions(3, 1, 1); // 3 channels, 1 slice, 1 frame

      return result;

    } catch (Exception e) {
      LOGGER.error("Failed to convert RGB to channel stack, falling back to grayscale", e);

      // Fallback: convert to 8-bit grayscale
      ImagePlus gray = rgbImage.duplicate();
      new ij.process.ImageConverter(gray).convertToGray8();
      return gray;
    }
  }

  /**
   * Execute StarDist with H&E model.
   */
  private List<NucleusROI> executeStarDistHE(Dataset inputDataset)
      throws NuclearSegmentationException {
    LOGGER.info("Executing StarDist2D with H&E model choice: {}", settings.getModelChoice());

    // Clear any existing ROI Manager
    RoiManager ijRoiManager = RoiManager.getInstance();
    if (ijRoiManager != null) {
      ijRoiManager.reset();
    }

    try {
      // Create StarDist parameters
      Map<String, Object> params = createHEParameters(inputDataset);

      // Execute StarDist2D command with timeout
      Future<CommandModule> future = commandService.run(StarDist2D.class, true, params);
      CommandModule result = future.get(STARDIST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

      if (result == null) {
        throw new NuclearSegmentationException("StarDist2D command returned null result");
      }

      // Extract ROIs from ImageJ ROI Manager
      ijRoiManager = RoiManager.getInstance();
      if (ijRoiManager == null) {
        throw new NuclearSegmentationException("No ROI Manager found after StarDist execution");
      }

      Roi[] detectedRois = ijRoiManager.getRoisAsArray();
      LOGGER.info("StarDist detected {} ROIs", detectedRois.length);

      // Convert to NucleusROI objects
      List<NucleusROI> nucleusROIs = convertToNucleusROIs(detectedRois);
      return nucleusROIs;

    } catch (java.util.concurrent.ExecutionException ee) {
      Throwable cause = ee.getCause();
      String message = cause != null ? cause.getMessage() : ee.getMessage();
      throw new NuclearSegmentationException("StarDist2D command execution failed: " + message, ee);

    } catch (java.util.concurrent.TimeoutException te) {
      throw new NuclearSegmentationException(
          "StarDist2D command timed out: " + te.getMessage(), te);

    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new NuclearSegmentationException(
          "StarDist2D command was interrupted: " + ie.getMessage(), ie);
    }
  }

  /**
   * Create parameters for StarDist H&E execution.
   */
  private Map<String, Object> createHEParameters(Dataset inputDataset) {
    Map<String, Object> params = new HashMap<>();

    // Input and model selection
    params.put("input", inputDataset);
    params.put("modelChoice", settings.getModelChoice());

    // Normalization settings from configuration
    params.put("normalizeInput", settings.isNormalizeInput());
    params.put("percentileBottom", (double) settings.getPercentileBottom());
    params.put("percentileTop", (double) settings.getPercentileTop());

    // Detection thresholds from configuration
    params.put("probThresh", (double) settings.getProbThresh());
    params.put("nmsThresh", (double) settings.getNmsThresh());

    // Output settings from configuration
    params.put("outputType", settings.getOutputType());
    params.put("excludeBoundary", settings.getExcludeBoundary());
    params.put("roiPosition", settings.getRoiPosition());

    // Performance settings from configuration
    params.put("nTiles", settings.getNTiles());
    params.put("verbose", settings.isVerbose());
    params.put("showCsbdeepProgress", settings.isShowCsbdeepProgress());
    params.put("showProbAndDist", settings.isShowProbAndDist());

    return params;
  }

  /**
   * Convert ImageJ ROIs to NucleusROI objects.
   */
  private List<NucleusROI> convertToNucleusROIs(Roi[] rois) {
    List<NucleusROI> nucleiROIs = new ArrayList<>();

    if (rois == null || rois.length == 0) {
      LOGGER.warn("No ROIs detected by StarDist");
      return nucleiROIs;
    }

    for (int i = 0; i < rois.length; i++) {
      Roi roi = rois[i];

      if (roi == null) {
        LOGGER.warn("Skipping null ROI at index {}", i);
        continue;
      }

      // Create nucleus name
      String nucleusName = "Nucleus_" + (i + 1);

      // Create NucleusROI
      NucleusROI nucleusROI = new NucleusROI(roi, imageFileName, nucleusName);
      nucleusROI.setSegmentationMethod("StarDist_HE");
      nucleusROI.setNotes(
          String.format(
              "Nucleus detected by StarDist %s model. "
                  + "Prob threshold: %.3f, NMS threshold: %.3f, Normalization: %s (%.1f-%.1f%%)",
              settings.getModelChoice(),
              settings.getProbThresh(),
              settings.getNmsThresh(),
              settings.isNormalizeInput() ? "enabled" : "disabled",
              settings.getPercentileBottom(),
              settings.getPercentileTop()));

      nucleiROIs.add(nucleusROI);
    }

    return nucleiROIs;
  }

  /**
   * Get statistics about the detected nuclei.
   */
  public String getStatistics(List<NucleusROI> nucleiROIs) {
    if (nucleiROIs.isEmpty()) {
      return "No nuclei detected";
    }

    double totalArea = nucleiROIs.stream().mapToDouble(NucleusROI::getNucleusArea).sum();
    double avgArea = totalArea / nucleiROIs.size();
    double minArea = nucleiROIs.stream().mapToDouble(NucleusROI::getNucleusArea).min().orElse(0.0);
    double maxArea = nucleiROIs.stream().mapToDouble(NucleusROI::getNucleusArea).max().orElse(0.0);

    return String.format(
        "H&E Nuclei: %d, Total area: %.1f px, Avg area: %.1f px (range: %.1f-%.1f)",
        nucleiROIs.size(), totalArea, avgArea, minArea, maxArea);
  }

  /**
   * Check if StarDist is available.
   */
  public boolean isAvailable() {
    try {
      checkStarDistAvailability();
      return true;
    } catch (Exception e) {
      LOGGER.error("StarDist is not available: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Close the context and release resources.
   */
  @Override
  public void close() {
    try {
      if (context != null) {
        context.dispose();
      }
    } catch (Exception e) {
      LOGGER.warn("Error disposing context", e);
    }
  }

  /**
   * Get the current nuclear segmentation settings.
   *
   * @return The settings instance
   */
  public NuclearSegmentationSettings getSettings() {
    return settings;
  }

  @Override
  public String toString() {
    return String.format(
        "NuclearSegmentation[image=%s, model=%s, probThresh=%.3f, available=%s]",
        imageFileName, settings.getModelChoice(), settings.getProbThresh(), isAvailable());
  }

  /**
   * Custom exception for nuclear segmentation errors.
   */
  public static class NuclearSegmentationException extends RuntimeException {
    public NuclearSegmentationException(String message) {
      super(message);
    }

    public NuclearSegmentationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
