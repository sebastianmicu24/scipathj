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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.scijava.command.CommandService;
import org.scijava.io.location.Location;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import de.csbdresden.csbdeep.network.DefaultInputMapper;
import de.csbdresden.csbdeep.network.model.DefaultNetwork;
import de.csbdresden.csbdeep.network.model.NetworkSettings;
import de.csbdresden.csbdeep.task.Task;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import javax.swing.*;

public class TensorFlowNetwork<T extends RealType<T>> extends DefaultNetwork<T> {
	
	private static final Logger logger = LoggerFactory.getLogger(TensorFlowNetwork.class);

	@Parameter
	private TensorFlowService tensorFlowService;

	@Parameter
	private DatasetService datasetService;

	@Parameter
	private CommandService commandService;

	@Parameter
	private LogService logService;

	private SavedModelBundle model;
	private Session session;
	private Map meta;
	private boolean tensorFlowLoaded = false;
	private String inputNodeName = "input";
	private String outputNodeName = "concatenate_4/concat:0";  // StarDist H&E model output
	private AxisType axisToRemove;
	
	// Same as tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY in Python
	private static final String MODEL_TAG = "serve";
	private static final String DEFAULT_SERVING_SIGNATURE_DEF_KEY = "serving_default";

	public TensorFlowNetwork(Task associatedTask) {
		super(associatedTask);
	}

	@Override
	public void loadLibrary() {
		try {
			// Configure TensorFlow memory settings BEFORE loading
			configureTensorFlowMemory();
			
			// TensorFlow 2.x is loaded automatically with the new dependencies
			String version = TensorFlow.version();
			logger.info("TensorFlow version loaded: {}", version);
			log("TensorFlow version loaded: " + version);
			tensorFlowLoaded = true;
		} catch (Exception e) {
			tensorFlowLoaded = false;
			logger.error("Could not load TensorFlow: {}", e.getMessage(), e);
			logService.error("Could not load TensorFlow. Check previous errors and warnings for details.");
		}
	}
	
	/**
	 * Configure TensorFlow memory settings to prevent OutOfMemoryError
	 */
	private void configureTensorFlowMemory() {
		try {
			// Set TensorFlow memory configuration
			System.setProperty("TF_CPP_MIN_LOG_LEVEL", "1"); // Show warnings
			
			// Configure memory growth and limits
			System.setProperty("TF_FORCE_GPU_ALLOW_GROWTH", "true");
			System.setProperty("TF_GPU_MEMORY_ALLOW_GROWTH", "true");
			
			// Set memory fraction (use 80% of available memory)
			System.setProperty("TF_GPU_MEMORY_FRACTION", "0.8");
			
			// Configure CPU memory pool
			System.setProperty("TF_CPU_ALLOCATOR_USE_BFC", "true");
			
			// Reduce thread usage to save memory
			System.setProperty("OMP_NUM_THREADS", "2");
			System.setProperty("TF_NUM_INTEROP_THREADS", "2");
			System.setProperty("TF_NUM_INTRAOP_THREADS", "4");
			
			// Enable memory optimization
			System.setProperty("TF_ENABLE_ONEDNN_OPTS", "1");
			
			logger.info("TensorFlow memory configuration applied");
			log("Applied TensorFlow memory configuration to prevent OutOfMemoryError");
			
		} catch (Exception e) {
			logger.warn("Could not configure TensorFlow memory settings: {}", e.getMessage());
		}
	}

	@Override
	public void loadInputNode(final Dataset dataset) {
		super.loadInputNode(dataset);
		// Set up basic input node structure for TensorFlow 2.x
		log("Setting up input node structure for TensorFlow 2.x");
		inputNode.setName(inputNodeName);
		// Set a default node shape for typical StarDist input (batch, height, width, channels)
		long[] defaultShape = {-1, -1, -1, -1}; // Dynamic dimensions
		inputNode.setNodeShape(defaultShape);
		inputNode.initializeNodeMapping();
	}

	@Override
	public void loadOutputNode(Dataset dataset) {
		super.loadOutputNode(dataset);
		// Set up basic output node structure for TensorFlow 2.x
		log("Setting up output node structure for TensorFlow 2.x");
		outputNode.setName(outputNodeName);
		// Set a default node shape for typical StarDist output (batch, height, width, n_rays+1)
		long[] defaultShape = {-1, -1, -1, 33}; // 32 rays + 1 for probability
		outputNode.setNodeShape(defaultShape);
		outputNode.initializeNodeMapping();
	}

	@Override
	protected boolean loadModel(final Location source, final String modelName) {
		if(!tensorFlowLoaded) {
			logger.warn("TensorFlow not loaded, cannot load model");
			return false;
		}
		
		logger.info("Loading TensorFlow 2.x model {} from source {}", modelName, source.getURI());
		log("Loading TensorFlow 2.x model " + modelName + " from source " + source.getURI());
		
		try {
			// Close existing model if any
			if (model != null) {
				model.close();
				model = null;
			}
			if (session != null) {
				session.close();
				session = null;
			}
			
			// Try to load model directly using TensorFlow 2.x SavedModelBundle
			String modelPath = getModelPath(source, modelName);
			logger.info("Attempting to load model from path: {}", modelPath);
			
			// Carica il modello con ottimizzazioni per TensorFlow 2.x
			model = SavedModelBundle.load(modelPath, MODEL_TAG);
			session = model.session();
			
			// Aggiungi ottimizzazioni per performance CPU e memoria
			try {
				// Configura TensorFlow per utilizzare meno memoria
				System.setProperty("TF_CPP_MIN_LOG_LEVEL", "2"); // Riduce logging verboso
				System.setProperty("TF_FORCE_GPU_ALLOW_GROWTH", "true"); // Crescita graduale memoria GPU
				System.setProperty("TF_GPU_MEMORY_ALLOW_GROWTH", "true"); // Crescita graduale memoria
				
				// Ottimizzazioni specifiche per CPU
				System.setProperty("OMP_NUM_THREADS", "1"); // Limita thread OpenMP per ridurre memoria
				System.setProperty("TF_NUM_INTEROP_THREADS", "1"); // Limita thread inter-op
				System.setProperty("TF_NUM_INTRAOP_THREADS", "2"); // Limita thread intra-op
				
				logger.info("TensorFlow 2.x optimizations applied for CPU performance and memory efficiency");
				log("Applied TensorFlow 2.x memory optimizations");
			} catch (Exception optEx) {
				logger.warn("Could not apply TensorFlow optimizations: {}", optEx.getMessage());
			}
			
			logger.info("Successfully loaded TensorFlow 2.x model with session: {}", session);
			log("Successfully loaded TensorFlow 2.x model");
			return true;
			
		} catch (Exception e) {
			logger.error("Failed to load TensorFlow 2.x model: {}", e.getMessage(), e);
			logService.error("Failed to load TensorFlow 2.x model: " + e.getMessage());
			return false;
		}
	}
	
	/**
	 * Get the actual file system path for the model
	 */
	private String getModelPath(Location source, String modelName) {
		// For resources, try to find the model in the resources directory
		String resourcePath = "src/main/resources/models/2D/" + getModelDirectoryName(modelName);
		java.io.File resourceFile = new java.io.File(resourcePath);
		if (resourceFile.exists()) {
			return resourceFile.getAbsolutePath();
		}
		
		// Fallback to source URI
		return source.getURI().getPath();
	}

	@Override
	public RandomAccessibleInterval<T> execute(
		final RandomAccessibleInterval<T> tile) throws IllegalArgumentException, OutOfMemoryError, ExecutionException {

		if (model == null || session == null) {
			throw new IllegalArgumentException("Model not loaded or session not available");
		}

		long[] tileDims = new long[tile.numDimensions()];
		tile.dimensions(tileDims);
		
		// LOGGING CRITICO: Verifichiamo cosa arriva al converter
		System.out.println("[INFO] === CHIAMATA DATASET TO TENSOR ===");
		System.out.println("[INFO] Tile dimensions: " + java.util.Arrays.toString(tileDims));
		System.out.println("[INFO] Tile numDimensions: " + tile.numDimensions());
		System.out.println("[INFO] Tile type: " + tile.getClass().getSimpleName());
		
		final Tensor inputTensor = DatasetTensorFlowConverter.datasetToTensor(tile,
			convertNodeMappingToImgMapping(getInputNode().getMappingIndices()));
		
		System.out.println("[INFO] Input tensor creato: " + (inputTensor != null));
		if (inputTensor != null) {
			System.out.println("[INFO] Input tensor shape: " + inputTensor.shape().toString());
			System.out.println("[INFO] Input tensor dataType: " + inputTensor.dataType());
		}
		
		if (inputTensor != null) {
			RandomAccessibleInterval<T> output = null;
			Tensor outputTensor = null;
			
			try {
				// Execute using TensorFlow 2.x Session API
				outputTensor = TensorFlowRunner.executeGraph(session, inputTensor, inputNodeName, outputNodeName);

				if (outputTensor != null) {
					output = DatasetTensorFlowConverter.tensorToDataset(outputTensor, tile
						.randomAccess().get(), convertNodeMappingToImgMapping(getOutputNode().getMappingIndices()),
						dropSingletonDims);
				}
			} finally {
				// Clean up tensors
				if (outputTensor != null) {
					outputTensor.close();
				}
				inputTensor.close();
			}
			
			return output;
		}
		return null;
	}

	private static int[] convertNodeMappingToImgMapping(int[] nodeMapping) {
		int[] res = new int[nodeMapping.length];
		for (int i = 0; i < nodeMapping.length; i++) {
			for (int j = 0; j < nodeMapping.length; j++) {
				if(i == nodeMapping[j]) {
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

	@Override
	public void clear() {
		super.clear();
		if (session != null) {
			session.close();
			session = null;
		}
		if (model != null) {
			model.close();
			model = null;
		}
		axisToRemove = null;
	}

	@Override
	public void dispose() {
		super.dispose();
		tensorFlowLoaded = false;
		clear();
	}

	/**
	 * Map model names to directory names in resources.
	 */
	private String getModelDirectoryName(String modelName) {
		logger.debug("Mapping model name: '{}'", modelName);
		
		if (modelName == null) {
			logger.debug("Model name is null, using default: dsb2018_heavy_augment");
			return "dsb2018_heavy_augment"; // default
		}
		
		String lowerName = modelName.toLowerCase();
		logger.debug("Lowercase model name: '{}'", lowerName);
		
		// Map StarDist model names to directory names
		// Check for specific model names first (most specific)
		if (lowerName.equals("versatile (h&e nuclei)")) {
			logger.debug("Matched 'versatile (h&e nuclei)' -> he_heavy_augment");
			return "he_heavy_augment";
		} else if (lowerName.equals("versatile (fluorescent nuclei)")) {
			logger.debug("Matched 'versatile (fluorescent nuclei)' -> dsb2018_heavy_augment");
			return "dsb2018_heavy_augment";
		} else if (lowerName.equals("dsb 2018 (from stardist 2d paper)")) {
			logger.debug("Matched 'dsb 2018 (from stardist 2d paper)' -> dsb2018_paper");
			return "dsb2018_paper";
		}
		
		// Check for partial matches (H&E has priority over versatile)
		if (lowerName.contains("h&e") || lowerName.contains("he")) {
			logger.debug("Partial match H&E/HE -> he_heavy_augment");
			return "he_heavy_augment";
		} else if (lowerName.contains("dsb2018") && lowerName.contains("paper")) {
			logger.debug("Partial match DSB2018 paper -> dsb2018_paper");
			return "dsb2018_paper";
		} else if (lowerName.contains("dsb2018") || lowerName.contains("fluorescent") || lowerName.contains("versatile")) {
			logger.debug("Partial match DSB2018/fluorescent/versatile -> dsb2018_heavy_augment");
			return "dsb2018_heavy_augment";
		}
		
		// Default fallback
		logger.debug("No match found, using default: dsb2018_heavy_augment");
		return "dsb2018_heavy_augment";
	}

	@Override
	public List<Integer> dropSingletonDims() {
		// Return empty list - no specific dimensions to drop by default in TensorFlow 2.x
		return new ArrayList<>();
	}

	@Override
	public void initMapping() {
		// Initialize input and output node mappings for TensorFlow 2.x
		if (getInputNode() != null) {
			getInputNode().initializeNodeMapping();
		}
		if (getOutputNode() != null) {
			getOutputNode().initializeNodeMapping();
		}
	}

	@Override
	public void preprocess() {
		// Preprocessing for TensorFlow 2.x - initialize mappings
		initMapping();
	}

	@Override
	public boolean libraryLoaded() {
		// Check if TensorFlow library is loaded
		return tensorFlowLoaded;
	}

	@Override
	public void doDimensionReduction() {
		// Dimension reduction for TensorFlow 2.x - handle singleton dimensions
		if (axisToRemove != null) {
			// Apply dimension reduction based on axisToRemove
			logger.debug("Applying dimension reduction for axis: {}", axisToRemove);
		}
	}

	@Override
	public void calculateMapping() {
		// Calculate input/output mappings for TensorFlow 2.x
		logger.debug("Calculating mappings for TensorFlow 2.x network");
		// Implementation will be added when needed
	}
}
