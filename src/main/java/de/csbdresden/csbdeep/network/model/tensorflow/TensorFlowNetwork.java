/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2020 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package de.csbdresden.csbdeep.network.model.tensorflow;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.protobuf.InvalidProtocolBufferException;
import de.csbdresden.csbdeep.network.DefaultInputMapper;
import de.csbdresden.csbdeep.network.model.DefaultNetwork;
import de.csbdresden.csbdeep.network.model.NetworkSettings;
import de.csbdresden.csbdeep.task.Task;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.tensorflow.CachedModelBundle;
import net.imagej.tensorflow.TensorFlowService;
import net.imagej.tensorflow.ui.TensorFlowLibraryManagementCommand;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.command.CommandService;
import org.scijava.io.location.Location;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;
import org.tensorflow.framework.TensorInfo;
import org.tensorflow.framework.TensorShapeProto;

public class TensorFlowNetwork<T extends RealType<T>> extends DefaultNetwork<T> {
  @Parameter private TensorFlowService tensorFlowService;

  @Parameter private DatasetService datasetService;

  @Parameter private CommandService commandService;

  @Parameter private LogService logService;

  private CachedModelBundle model;
  private SignatureDef sig;
  private Map meta;
  private boolean tensorFlowLoaded = false;
  private TensorInfo inputTensorInfo, outputTensorInfo;
  private AxisType axisToRemove;
  // Same as
  // tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY
  // in Python. Perhaps this should be an exported constant in TensorFlow's Java
  // API.
  private static final String MODEL_TAG = "serve";
  private static final String DEFAULT_SERVING_SIGNATURE_DEF_KEY = "serving_default";

  public TensorFlowNetwork(Task associatedTask) {
    super(associatedTask);
  }

  @Override
  public void loadLibrary() {
    tensorFlowService.loadLibrary();
    if (tensorFlowService.getStatus().isLoaded()) {
      log(tensorFlowService.getStatus().getInfo());
      tensorFlowLoaded = true;
    } else {
      tensorFlowLoaded = false;
      logService.error(
          "Could not load TensorFlow. Check previous errors and warnings for details.");
      JOptionPane.showMessageDialog(
          null,
          "<html>Could not load TensorFlow.<br/>Opening the TensorFlow Library Management"
              + " tool.</html>",
          "Loading TensorFlow failed",
          JOptionPane.ERROR_MESSAGE);
      commandService.run(TensorFlowLibraryManagementCommand.class, true);
    }
  }

  @Override
  public void loadInputNode(final Dataset dataset) {
    super.loadInputNode(dataset);
    if (sig != null && sig.getInputsCount() > 0) {
      inputNode.setName(sig.getInputsMap().keySet().iterator().next());
      setInputTensor(sig.getInputsOrThrow(inputNode.getName()));
      inputNode.setNodeShape(getShape(getInputTensorInfo().getTensorShape()));
      inputNode.initializeNodeMapping();
    } else {
      // If no signature is available, set up basic node structure
      log("No signature available, setting up basic input node structure");
      inputNode.setName("input");
      // Set a default node shape for typical StarDist input (batch, height, width, channels)
      long[] defaultShape = {-1, -1, -1, -1}; // Dynamic dimensions
      inputNode.setNodeShape(defaultShape);
      inputNode.initializeNodeMapping();
    }
  }

  @Override
  public void loadOutputNode(Dataset dataset) {
    super.loadOutputNode(dataset);
    if (sig != null && sig.getOutputsCount() > 0) {
      outputNode.setName(sig.getOutputsMap().keySet().iterator().next());
      setOutputTensor(sig.getOutputsOrThrow(outputNode.getName()));
      outputNode.setNodeShape(getShape(getOutputTensorInfo().getTensorShape()));
      outputNode.initializeNodeMapping();
    } else {
      // If no signature is available, set up basic node structure
      log("No signature available, setting up basic output node structure");
      outputNode.setName("output");
      // Set a default node shape for typical StarDist output (batch, height, width, n_rays+1)
      long[] defaultShape = {-1, -1, -1, 33}; // 32 rays + 1 for probability
      outputNode.setNodeShape(defaultShape);
      outputNode.initializeNodeMapping();
    }
  }

  private long[] getShape(final TensorShapeProto tensorShape) {
    final long[] shape = new long[tensorShape.getDimCount()];
    for (int i = 0; i < shape.length; i++) {
      shape[i] = tensorShape.getDim(i).getSize();
    }
    return shape;
  }

  @Override
  protected boolean loadModel(final Location source, final String modelName) {
    if (!tensorFlowLoaded) {
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "TensorFlow not loaded, cannot load model");
      return false;
    }
    log("Loading TensorFlow model " + modelName + " from source file " + source.getURI());
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "=== STARTING MODEL LOADING ===");
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow("Model name: " + modelName);
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "Source URI: " + source.getURI());
    try {
      if (model != null) {
        model.close();
      }
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Attempting to load cached model using TensorFlowService");
      model = tensorFlowService.loadCachedModel(source, modelName, MODEL_TAG);
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "TensorFlowService.loadCachedModel() completed");
      //			loadNetworkSettingsFromJson(tensorFlowService.loadFile(source, modelName, "meta.json"));
    } catch (TensorFlowException e) {
      com.scipath.scipathj.core.utils.DirectFileLogger.logException(
          "TENSORFLOW", "TensorFlowException during loadCachedModel", e);
      return false;
    }
    // Extract names from the model signature.
    // The strings "input", "probabilities" and "patches" are meant to be
    // in sync with the model exporter (export_saved_model()) in Python.
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "Checking model state: model=" + (model != null ? "not null" : "null"));
    if (model != null) {
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Model.model() state: " + (model.model() != null ? "not null" : "null"));
    }
    if (model != null && model.model() != null) {
      try {
        // Try to extract signature definition from the model
        if (model.model() instanceof org.tensorflow.SavedModelBundle) {
          org.tensorflow.SavedModelBundle savedModel =
              (org.tensorflow.SavedModelBundle) model.model();
          byte[] metaGraphDef = savedModel.metaGraphDef();
          if (metaGraphDef != null && metaGraphDef.length > 0) {
            MetaGraphDef mgd = MetaGraphDef.parseFrom(metaGraphDef);
            sig = mgd.getSignatureDefOrThrow(DEFAULT_SERVING_SIGNATURE_DEF_KEY);
            log(
                "Successfully loaded model signature with "
                    + sig.getInputsCount()
                    + " inputs and "
                    + sig.getOutputsCount()
                    + " outputs");
            com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
                "Successfully loaded model signature with "
                    + sig.getInputsCount()
                    + " inputs and "
                    + sig.getOutputsCount()
                    + " outputs");
          } else {
            log("No metaGraphDef found in SavedModelBundle");
            com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
                "No metaGraphDef found in SavedModelBundle");
            sig = null;
          }
        } else {
          log("Model is not a SavedModelBundle, signature will be null");
          com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
              "Model is not a SavedModelBundle, signature will be null");
          sig = null;
        }
      } catch (InvalidProtocolBufferException e) {
        log("Failed to parse model signature: " + e.getMessage());
        com.scipath.scipathj.core.utils.DirectFileLogger.logException(
            "TENSORFLOW", "Failed to parse model signature", e);
        sig = null;
      } catch (Exception e) {
        log("Error loading model signature: " + e.getMessage());
        com.scipath.scipathj.core.utils.DirectFileLogger.logException(
            "TENSORFLOW", "Error loading model signature", e);
        sig = null;
      }
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Model loaded successfully via TensorFlowService");
      return true;
    } else {
      logService.error(
          "TensorFlowService.loadCachedModel() returned null model.model() - trying direct"
              + " loading");
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "TensorFlowService.loadCachedModel() returned null model.model() - trying direct"
              + " loading");
      // Try direct loading as fallback for JPackage environment
      try {
        com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
            "Attempting direct model loading fallback");
        model = loadModelDirectly(source, modelName);
        if (model != null && model.model() != null) {
          log("Successfully loaded model using direct loading fallback");
          com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
              "Successfully loaded model using direct loading fallback");
          // Try to extract signature definition from the directly loaded model
          try {
            if (model.model() instanceof org.tensorflow.SavedModelBundle) {
              org.tensorflow.SavedModelBundle savedModel =
                  (org.tensorflow.SavedModelBundle) model.model();
              byte[] metaGraphDef = savedModel.metaGraphDef();
              if (metaGraphDef != null && metaGraphDef.length > 0) {
                MetaGraphDef mgd = MetaGraphDef.parseFrom(metaGraphDef);
                sig = mgd.getSignatureDefOrThrow(DEFAULT_SERVING_SIGNATURE_DEF_KEY);
                log(
                    "Successfully loaded model signature with "
                        + sig.getInputsCount()
                        + " inputs and "
                        + sig.getOutputsCount()
                        + " outputs");
              } else {
                log("No metaGraphDef found in directly loaded SavedModelBundle");
                sig = null;
              }
            } else {
              log("Directly loaded model is not a SavedModelBundle, signature will be null");
              sig = null;
            }
          } catch (InvalidProtocolBufferException e) {
            log("Failed to parse directly loaded model signature: " + e.getMessage());
            sig = null;
          } catch (Exception e) {
            log("Error loading directly loaded model signature: " + e.getMessage());
            sig = null;
          }
          return true;
        } else {
          logService.error("Direct model loading also failed");
          com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
              "Direct model loading also failed - model or model.model() is null");
          return false;
        }
      } catch (Exception e) {
        logService.error("Exception during direct model loading: " + e.getMessage());
        com.scipath.scipathj.core.utils.DirectFileLogger.logException(
            "TENSORFLOW", "Exception during direct model loading", e);
        return false;
      }
    }
  }

  private void loadNetworkSettingsFromJson(File jsonFile) {
    networkSettings = new NetworkSettings();
    try {
      JsonReader reader = new JsonReader(new FileReader(jsonFile));
      try {
        readNetworkSettingsArray(reader);
      } finally {
        reader.close();
      }
    } catch (IOException e) {
      log("No meta.json file found for network.");
    }
  }

  private void readNetworkSettingsArray(JsonReader reader) throws IOException {
    reader.beginObject();
    while (reader.hasNext()) {
      String name = reader.nextName();
      if (name.equals("axes_div_by") && reader.peek() != JsonToken.NULL) {
        networkSettings.axesDivBy = readIntArray(reader);
      } else if (name.equals("tile_overlap") && reader.peek() != JsonToken.NULL) {
        networkSettings.tileOverlap = readIntArray(reader);
      } else if (name.equals("axes") && reader.peek() != JsonToken.NULL) {
        networkSettings.axesIn = readAxesString(reader);
      } else if (name.equals("axes_out") && reader.peek() != JsonToken.NULL) {
        networkSettings.axesOut = readAxesString(reader);
      } else if (name.equals("tiling") && reader.peek() != JsonToken.NULL) {
        networkSettings.tilingAllowed = readBooleanArray(reader);
      } else {
        reader.skipValue();
      }
    }
    reader.endObject();
  }

  private List readAxesString(JsonReader reader) throws IOException {
    String singleEntry = reader.nextString();
    return toAxesList(singleEntry);
  }

  private List<AxisType> toAxesList(String axesStr) {
    return DefaultInputMapper.parseMappingStr(axesStr);
  }

  private List<Integer> readIntArray(JsonReader reader) throws IOException {
    List<Integer> res = new ArrayList<>();
    try {
      Integer singleEntry = reader.nextInt();
      res.add(singleEntry);
    } catch (IllegalStateException | NumberFormatException e) {
      reader.beginArray();
      while (reader.hasNext()) {
        res.add(reader.nextInt());
      }
      reader.endArray();
    }
    return res;
  }

  private List<Boolean> readBooleanArray(JsonReader reader) throws IOException {
    List<Boolean> res = new ArrayList<>();
    try {
      Boolean singleEntry = reader.nextBoolean();
      res.add(singleEntry);
    } catch (IllegalStateException | NumberFormatException e) {
      reader.beginArray();
      while (reader.hasNext()) {
        res.add(reader.nextBoolean());
      }
      reader.endArray();
    }
    return res;
  }

  @Override
  public void preprocess() {
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow("=== INIZIO PREPROCESSING ===");
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "InputNode presente: " + (inputNode != null));
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "OutputNode presente: " + (outputNode != null));
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "Model presente: " + (model != null));
    if (model != null) {
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Model.model() presente: " + (model.model() != null));
    }

    initMapping();
    calculateMapping();

    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow("=== FINE PREPROCESSING ===");
  }

  @Override
  public void initMapping() {
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow("--- Inizio initMapping ---");
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "InputNode stato: " + (inputNode != null ? "presente" : "NULL"));
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "OutputNode stato: " + (outputNode != null ? "presente" : "NULL"));

    if (inputNode != null) {
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Chiamata inputNode.setMappingDefaults()");
      inputNode.setMappingDefaults();
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "inputNode.setMappingDefaults() completata");
    } else {
      log("Warning: inputNode is null in initMapping(), skipping setMappingDefaults()");
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "ERRORE CRITICO: inputNode è null in initMapping()!");
    }
    if (outputNode != null) {
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Chiamata outputNode.setMappingDefaults()");
      outputNode.setMappingDefaults();
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "outputNode.setMappingDefaults() completata");
    } else {
      log("Warning: outputNode is null in initMapping(), skipping setMappingDefaults()");
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "ERRORE CRITICO: outputNode è null in initMapping()!");
    }

    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow("--- Fine initMapping ---");
  }

  @Override
  public void calculateMapping() {
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "--- Inizio calculateMapping ---");
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "InputNode presente prima doDimensionReduction: " + (inputNode != null));
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "OutputNode presente prima doDimensionReduction: " + (outputNode != null));

    doDimensionReduction();

    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "InputNode presente prima generateMapping: " + (inputNode != null));
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "OutputNode presente prima generateMapping: " + (outputNode != null));

    generateMapping();

    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow("--- Fine calculateMapping ---");
  }

  @Override
  public List<Integer> dropSingletonDims() {
    outputNode.dropSingletonDims();
    return inputNode.dropSingletonDims();
  }

  @Override
  public void doDimensionReduction() {
    int diff = getOutputNode().getNodeShape().length - getInputNode().getNodeShape().length;
    if (diff == 0) return;
    if (diff > 0) status.logError("Cannot handle case INPUT TENSOR SIZE < OUTPUT TENSOR SIZE");
    if (diff == -1) {
      doSingleDimensionReduction();
    } else {
      status.logWarning(
          "Cannot apply axes from input tensor to output tensor because more than one dimension got"
              + " reduced.");
      getInputNode().setTilingAllowed(false);
      getOutputNode().setTilingAllowed(false);
    }
  }

  private void doSingleDimensionReduction() {
    if (getInputNode().getImageAxes().contains(Axes.TIME)) {
      axisToRemove = Axes.TIME;
    } else {
      axisToRemove = Axes.Z;
    }
    final Dataset outputDummy =
        createEmptyDuplicateWithoutAxis(
            inputNode.getImageAxes(), inputNode.getImageDimensions(), axisToRemove);
    getOutputNode().initialize(outputDummy);
    List<AxisType> mapping = new ArrayList<>();
    mapping.addAll(getInputNode().getNodeAxes());
    mapping.remove(axisToRemove);
    getOutputNode().setMapping(mapping.toArray(new AxisType[0]));
  }

  @Override
  public boolean libraryLoaded() {
    return tensorFlowLoaded;
  }

  private void generateMapping() {
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "--- Inizio generateMapping ---");

    if (inputNode != null) {
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Chiamata inputNode.generateMapping()");
      inputNode.generateMapping();
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "inputNode.generateMapping() completata");
    } else {
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "ERRORE CRITICO: inputNode è null in generateMapping()!");
    }

    if (outputNode != null) {
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Chiamata outputNode.generateMapping()");
      outputNode.generateMapping();
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "outputNode.generateMapping() completata");
    } else {
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "ERRORE CRITICO: outputNode è null in generateMapping()!");
    }

    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow("--- Fine generateMapping ---");
  }

  private Dataset createEmptyDuplicateWithoutAxis(
      List<AxisType> imageAxes, List<Long> imageDimensions, AxisType axisToRemove) {
    int numDims = imageAxes.size();
    if (imageAxes.contains(axisToRemove)) {
      numDims--;
    }
    final long[] dims = new long[numDims];
    final AxisType[] axes = new AxisType[numDims];
    int j = 0;
    for (int i = 0; i < numDims; i++) {
      final AxisType axisType = imageAxes.get(i);
      if (axisType != axisToRemove) {
        axes[j] = axisType;
        dims[j] = imageDimensions.get(i);
        j++;
      }
    }
    final Dataset result = datasetService.create(new FloatType(), dims, "", axes);
    return result;
  }

  // TODO this is the tensorflow runner
  @Override
  public RandomAccessibleInterval<T> execute(final RandomAccessibleInterval<T> tile)
      throws IllegalArgumentException, OutOfMemoryError, ExecutionException {

    long[] tileDims = new long[tile.numDimensions()];
    tile.dimensions(tileDims);
    final Tensor inputTensor =
        DatasetTensorFlowConverter.datasetToTensor(
            tile, convertNodeMappingToImgMapping(getInputNode().getMappingIndices()));
    if (inputTensor != null) {
      RandomAccessibleInterval<T> output = null;
      Tensor outputTensor = null;
      if (model.model() instanceof org.tensorflow.SavedModelBundle) {
        outputTensor =
            TensorFlowRunner.executeGraph(
                (org.tensorflow.SavedModelBundle) model.model(),
                inputTensor,
                getInputTensorInfo(),
                getOutputTensorInfo());
      }

      if (outputTensor != null) {
        output =
            DatasetTensorFlowConverter.tensorToDataset(
                outputTensor,
                tile.randomAccess().get(),
                convertNodeMappingToImgMapping(getOutputNode().getMappingIndices()),
                dropSingletonDims);
        outputTensor.close();
      }
      inputTensor.close();
      return output;
    }
    return null;
  }

  private static int[] convertNodeMappingToImgMapping(int[] nodeMapping) {
    int[] res = new int[nodeMapping.length];
    for (int i = 0; i < nodeMapping.length; i++) {
      for (int j = 0; j < nodeMapping.length; j++) {
        if (i == nodeMapping[j]) {
          res[i] = j;
          break;
        }
      }
    }
    return res;
  }

  @Override
  public boolean isInitialized() {
    return model != null;
  }

  public void setInputTensor(final TensorInfo tensorInfo) {
    inputTensorInfo = tensorInfo;
    logTensorShape("Shape of input tensor", tensorInfo);
  }

  protected void logTensorShape(String title, final TensorInfo tensorInfo) {
    long[] dims = new long[tensorInfo.getTensorShape().getDimCount()];
    for (int i = 0; i < dims.length; i++) {
      dims[i] = tensorInfo.getTensorShape().getDimList().get(i).getSize();
    }
    log(title + ": " + Arrays.toString(dims));
  }

  public void setOutputTensor(final TensorInfo tensorInfo) {
    outputTensorInfo = tensorInfo;
    logTensorShape("Shape of output tensor", tensorInfo);
  }

  public TensorInfo getInputTensorInfo() {
    return inputTensorInfo;
  }

  public TensorInfo getOutputTensorInfo() {
    return outputTensorInfo;
  }

  @Override
  public void clear() {
    super.clear();
    sig = null;
    model = null;
    inputTensorInfo = null;
    outputTensorInfo = null;
    axisToRemove = null;
  }

  @Override
  public void dispose() {
    super.dispose();
    tensorFlowLoaded = false;
    clear();
  }

  /**
   * Direct model loading method for JPackage environment.
   * First tries to load from JAR resources, then falls back to ZIP extraction.
   */
  private CachedModelBundle loadModelDirectly(final Location source, final String modelName)
      throws Exception {
    log("=== STARTING DIRECT MODEL LOADING ===");
    log("Source: " + source.getURI());
    log("Model name: " + modelName);
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "=== STARTING DIRECT MODEL LOADING ===");
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow("Source: " + source.getURI());
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow("Model name: " + modelName);

    // STRATEGY 1: Try to load from JAR resources first (much safer for JPackage)
    try {
      log("Attempting to load model from JAR resources...");
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Attempting to load model from JAR resources...");
      String resourcePath = "/models/2D/" + getModelDirectoryName(modelName) + "/";
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Resource path: " + resourcePath);
      java.net.URL resourceUrl = getClass().getResource(resourcePath);

      if (resourceUrl != null) {
        log("Found model in JAR resources: " + resourceUrl);
        com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
            "Found model in JAR resources: " + resourceUrl);

        // Extract model from JAR to temporary directory
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("scipathj_model_");
        log("Created temp directory: " + tempDir.toString());

        // Extract the model directory from JAR resources
        extractModelFromResources(resourcePath, tempDir.toFile());

        // Load model directly using TensorFlow API
        String modelDirName = getModelDirectoryName(modelName);
        java.io.File modelDir = new java.io.File(tempDir.toFile(), modelDirName);
        if (!modelDir.exists()) {
          // Try to find the model directory in the extracted files
          modelDir = findModelDirectory(tempDir.toFile());
        }

        if (modelDir != null) {
          org.tensorflow.SavedModelBundle savedModel =
              org.tensorflow.SavedModelBundle.load(modelDir.getAbsolutePath(), "serve");
          log("SavedModelBundle loaded successfully from JAR resources");

          // Create a wrapper that implements CachedModelBundle interface
          CachedModelBundle directModel =
              new DirectTensorFlowModel(modelName, source.getURI().toString(), savedModel, tempDir);
          log("Direct model wrapper created from JAR resources");

          return directModel;
        } else {
          log("Could not find model directory in extracted JAR resources");
          com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
              "Could not find model directory in extracted JAR resources");
        }
      } else {
        log("Model not found in JAR resources, falling back to ZIP extraction");
        com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
            "Model not found in JAR resources, falling back to ZIP extraction");
      }
    } catch (Exception e) {
      log("JAR resource loading failed: " + e.getMessage() + ", falling back to ZIP extraction");
      com.scipath.scipathj.core.utils.DirectFileLogger.logException(
          "TENSORFLOW", "JAR resource loading failed, falling back to ZIP extraction", e);
    }

    // STRATEGY 2: Fallback to ZIP extraction (original method)
    try {
      log("Attempting ZIP extraction fallback...");

      // Create a temporary directory for model extraction
      java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("scipathj_model_");
      log("Created temp directory: " + tempDir.toString());

      // Extract ZIP to temporary directory
      java.io.File sourceFile = new java.io.File(source.getURI());
      if (!sourceFile.exists()) {
        log("Source file does not exist: " + sourceFile.getAbsolutePath());
        throw new java.io.FileNotFoundException(
            "Model file not found: " + sourceFile.getAbsolutePath());
      }

      log("Extracting ZIP file...");
      extractZipFile(sourceFile, tempDir.toFile());

      // Look for saved_model.pb in the extracted directory
      java.io.File modelDir = findModelDirectory(tempDir.toFile());
      if (modelDir == null) {
        log("Could not find saved_model.pb in extracted files");
        throw new Exception("saved_model.pb not found in model archive");
      }

      log("Found model directory: " + modelDir.getAbsolutePath());

      // Load model directly using TensorFlow API
      org.tensorflow.SavedModelBundle savedModel =
          org.tensorflow.SavedModelBundle.load(modelDir.getAbsolutePath(), "serve");
      log("SavedModelBundle loaded successfully from ZIP");

      // Create a wrapper that implements CachedModelBundle interface
      CachedModelBundle directModel =
          new DirectTensorFlowModel(modelName, source.getURI().toString(), savedModel, tempDir);
      log("Direct model wrapper created from ZIP");

      return directModel;

    } catch (Exception e) {
      log("All direct model loading strategies failed: " + e.getMessage());
      throw e;
    }
  }

  /**
   * Extract model from JAR resources to temporary directory.
   */
  private void extractModelFromResources(String resourcePath, java.io.File targetDir)
      throws Exception {
    log("Extracting model from JAR resources: " + resourcePath);

    try {
      // Create target directory
      targetDir.mkdirs();

      // Extract the model directory name from the resource path
      String modelDirName =
          resourcePath.substring(resourcePath.lastIndexOf("/", resourcePath.length() - 2) + 1);
      if (modelDirName.endsWith("/")) {
        modelDirName = modelDirName.substring(0, modelDirName.length() - 1);
      }

      // Create the model subdirectory
      java.io.File modelSubDir = new java.io.File(targetDir, modelDirName);
      modelSubDir.mkdirs();

      // Extract specific model files from JAR resources
      // We need to extract: saved_model.pb, variables/variables.data-00000-of-00001,
      // variables/variables.index

      // Extract saved_model.pb
      String savedModelPath = resourcePath + "saved_model.pb";
      java.io.InputStream savedModelIS = getClass().getResourceAsStream(savedModelPath);
      if (savedModelIS != null) {
        java.io.File savedModelFile = new java.io.File(modelSubDir, "saved_model.pb");
        java.nio.file.Files.copy(
            savedModelIS,
            savedModelFile.toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        savedModelIS.close();
        log("Extracted saved_model.pb to " + savedModelFile.getAbsolutePath());
      } else {
        throw new Exception("saved_model.pb not found in resources: " + savedModelPath);
      }

      // Create variables directory
      java.io.File variablesDir = new java.io.File(modelSubDir, "variables");
      variablesDir.mkdirs();

      // Extract variables.data file
      String variablesDataPath = resourcePath + "variables/variables.data-00000-of-00001";
      java.io.InputStream variablesDataIS = getClass().getResourceAsStream(variablesDataPath);
      if (variablesDataIS != null) {
        java.io.File variablesDataFile =
            new java.io.File(variablesDir, "variables.data-00000-of-00001");
        java.nio.file.Files.copy(
            variablesDataIS,
            variablesDataFile.toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        variablesDataIS.close();
        log("Extracted variables.data-00000-of-00001 to " + variablesDataFile.getAbsolutePath());
      } else {
        log("Warning: variables.data-00000-of-00001 not found in resources: " + variablesDataPath);
      }

      // Extract variables.index file
      String variablesIndexPath = resourcePath + "variables/variables.index";
      java.io.InputStream variablesIndexIS = getClass().getResourceAsStream(variablesIndexPath);
      if (variablesIndexIS != null) {
        java.io.File variablesIndexFile = new java.io.File(variablesDir, "variables.index");
        java.nio.file.Files.copy(
            variablesIndexIS,
            variablesIndexFile.toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        variablesIndexIS.close();
        log("Extracted variables.index to " + variablesIndexFile.getAbsolutePath());
      } else {
        log("Warning: variables.index not found in resources: " + variablesIndexPath);
      }

      log(
          "Model extraction from JAR resources completed successfully to "
              + modelSubDir.getAbsolutePath());

    } catch (Exception e) {
      log("Failed to extract model from resources: " + e.getMessage());
      throw e;
    }
  }

  /**
   * Extract ZIP file to target directory.
   */
  private void extractZipFile(java.io.File zipFile, java.io.File targetDir) throws Exception {
    try {
      java.util.zip.ZipInputStream zipIS =
          new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile));
      java.util.zip.ZipEntry entry;
      while ((entry = zipIS.getNextEntry()) != null) {
        java.io.File file = new java.io.File(targetDir, entry.getName());
        if (entry.isDirectory()) {
          file.mkdirs();
        } else {
          file.getParentFile().mkdirs();
          java.nio.file.Files.copy(
              zipIS, file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
      }
      zipIS.close();
    } catch (Exception e) {
      log("Failed to extract ZIP file: " + e.getMessage());
      throw e;
    }
  }

  /**
   * Find model directory containing saved_model.pb.
   */
  private java.io.File findModelDirectory(java.io.File rootDir) {
    java.io.File[] files = rootDir.listFiles();
    if (files != null) {
      for (java.io.File file : files) {
        if (file.isDirectory()) {
          java.io.File savedModelFile = new java.io.File(file, "saved_model.pb");
          if (savedModelFile.exists()) {
            return file;
          }
          // Recursively search subdirectories
          java.io.File found = findModelDirectory(file);
          if (found != null) {
            return found;
          }
        }
      }
    }
    return null;
  }

  /**
   * Map model names to directory names in resources.
   */
  private String getModelDirectoryName(String modelName) {
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow("=== MODEL NAME MAPPING ===");
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "Input model name: '" + modelName + "'");

    if (modelName == null) {
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Model name is null, using default: dsb2018_heavy_augment");
      return "dsb2018_heavy_augment"; // default
    }

    String lowerName = modelName.toLowerCase();
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "Lowercase model name: '" + lowerName + "'");

    // Map StarDist model names to directory names
    // Check for specific model names first (most specific)
    if (lowerName.equals("versatile (h&e nuclei)")) {
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Matched 'versatile (h&e nuclei)' -> he_heavy_augment");
      return "he_heavy_augment";
    } else if (lowerName.equals("versatile (fluorescent nuclei)")) {
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Matched 'versatile (fluorescent nuclei)' -> dsb2018_heavy_augment");
      return "dsb2018_heavy_augment";
    } else if (lowerName.equals("dsb 2018 (from stardist 2d paper)")) {
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Matched 'dsb 2018 (from stardist 2d paper)' -> dsb2018_paper");
      return "dsb2018_paper";
    }

    // Check for partial matches (H&E has priority over versatile)
    if (lowerName.contains("h&e") || lowerName.contains("he")) {
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Partial match H&E/HE -> he_heavy_augment");
      return "he_heavy_augment";
    } else if (lowerName.contains("dsb2018") && lowerName.contains("paper")) {
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Partial match DSB2018 paper -> dsb2018_paper");
      return "dsb2018_paper";
    } else if (lowerName.contains("dsb2018")
        || lowerName.contains("fluorescent")
        || lowerName.contains("versatile")) {
      com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
          "Partial match DSB2018/fluorescent/versatile -> dsb2018_heavy_augment");
      return "dsb2018_heavy_augment";
    }

    // Default fallback
    com.scipath.scipathj.core.utils.DirectFileLogger.logTensorFlow(
        "No match found, using default: dsb2018_heavy_augment");
    return "dsb2018_heavy_augment";
  }

  /**
   * Wrapper class for direct TensorFlow model loading.
   */
  private static class DirectTensorFlowModel extends CachedModelBundle {
    private final org.tensorflow.SavedModelBundle savedModel;
    private final java.nio.file.Path tempDir;

    public DirectTensorFlowModel(
        String modelName,
        String modelUrl,
        org.tensorflow.SavedModelBundle savedModel,
        java.nio.file.Path tempDir) {
      super(modelName, modelUrl, savedModel, true);
      this.savedModel = savedModel;
      this.tempDir = tempDir;
    }

    @Override
    public Object model() {
      return savedModel;
    }

    @Override
    public byte[] metaGraphDef() {
      if (savedModel != null) {
        try {
          return savedModel.metaGraphDef();
        } catch (Exception e) {
          System.err.println("Error getting metaGraphDef: " + e.getMessage());
          return null;
        }
      }
      return null;
    }

    @Override
    public void close() {
      if (savedModel != null) {
        savedModel.close();
      }
      // Clean up temporary directory
      try {
        deleteDirectory(tempDir.toFile());
      } catch (Exception e) {
        // Ignore cleanup errors
      }
    }

    private void deleteDirectory(java.io.File directory) {
      java.io.File[] files = directory.listFiles();
      if (files != null) {
        for (java.io.File file : files) {
          if (file.isDirectory()) {
            deleteDirectory(file);
          } else {
            file.delete();
          }
        }
      }
      directory.delete();
    }
  }
}
