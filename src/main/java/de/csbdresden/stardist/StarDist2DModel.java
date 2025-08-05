package de.csbdresden.stardist;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

import org.scijava.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StarDist2DModel {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StarDist2DModel.class);
    
    static final String MODEL_DSB2018_HEAVY_AUGMENTATION = "Versatile (fluorescent nuclei)";
    static final String MODEL_DSB2018_PAPER = "DSB 2018 (from StarDist 2D paper)";
    static final String MODEL_HE_HEAVY_AUGMENTATION = "Versatile (H&E nuclei)";
    static final String MODEL_DEFAULT = MODEL_DSB2018_HEAVY_AUGMENTATION;
    
    static final Map<String, StarDist2DModel> MODELS = new LinkedHashMap<String, StarDist2DModel>();
    static {
        MODELS.put(MODEL_DSB2018_PAPER, new StarDist2DModel(StarDist2DModel.class.getClassLoader().getResource("models/2D/dsb2018_paper.zip"), 0.417819, 0.5, 8, 48));
        MODELS.put(MODEL_DSB2018_HEAVY_AUGMENTATION, new StarDist2DModel(StarDist2DModel.class.getClassLoader().getResource("models/2D/dsb2018_heavy_augment.zip"), 0.479071, 0.3, 16, 96));
        MODELS.put(MODEL_HE_HEAVY_AUGMENTATION, new StarDist2DModel(StarDist2DModel.class.getClassLoader().getResource("models/2D/he_heavy_augment.zip"), 0.692478, 0.3, 16, 96));
    }
    
    // -----------
    
    public final URL url;
    public final double probThresh;
    public final double nmsThresh;
    public final int sizeDivBy;
    public final int tileOverlap;
    private final String protocol;
    private File localModelDirectory;
    
    public StarDist2DModel(URL url, double probThresh, double nmsThresh, int sizeDivBy, int tileOverlap) {
        this.url = url;
        this.protocol = url.getProtocol().toLowerCase();
        this.probThresh = probThresh;
        this.nmsThresh = nmsThresh;
        this.sizeDivBy = sizeDivBy;
        this.tileOverlap = tileOverlap;
    }
    
    public boolean isTempFile() {
        // If we found a local model directory, it's not a temp file
        if (localModelDirectory != null) {
            return false;
        }
        return protocol.equals("jar");
    }
    
    /**
     * Override canGetFile to return true for local models too.
     */
    public boolean canGetFile() {
        // Check if we have a local model directory
        try {
            if (findLocalModelFile() != null) {
                return true;
            }
        } catch (IOException e) {
            // Ignore exception and fall back to original behavior
        }
        return protocol.equals("file") || protocol.equals("jar");
    }
    
    public File getFile() throws IOException {
        // First, try to find the model in the local models directory
        File localModelFile = findLocalModelFile();
        if (localModelFile != null && localModelFile.exists()) {
            this.localModelDirectory = localModelFile;
            return localModelFile;
        }
        
        // If not found locally, use the original approach
        switch (protocol) {
        case "file":
            return FileUtils.urlToFile(url);
        case "jar":
            final File tmpModelFile = File.createTempFile("stardist_model_", ".zip");
            Files.copy(url.openStream(), tmpModelFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tmpModelFile;
        default:
            return null;
        }
    }
    
    /**
     * Try to find the model in the local models directory.
     * This looks for already extracted models to avoid extraction issues.
     */
    private File findLocalModelFile() throws IOException {
        // Determine which model this is based on the URL
        String modelName = getModelNameFromUrl();
        if (modelName == null) {
            return null;
        }
        
        // Look for the model in the resources directory first
        File resourceModelDir = new File("src/main/resources/models/2D/" + modelName);
        if (resourceModelDir.exists() && resourceModelDir.isDirectory()) {
            // Check if this directory contains a SavedModel structure
            File savedModelPb = new File(resourceModelDir, "saved_model.pb");
            File variablesDir = new File(resourceModelDir, "variables");
            
            if (savedModelPb.exists() && variablesDir.exists()) {
                LOGGER.debug("Found model in resources directory: {}", resourceModelDir.getAbsolutePath());
                return resourceModelDir;
            }
        }
        
        // If not found in resources, try other possible locations
        String[] possiblePaths = {
            "models",                           // When running from scipathj/ directory
            "scipathj/models",                  // When running from parent directory
            "./models",                         // Explicit current directory
            "./scipathj/models",               // Explicit parent to scipathj
            System.getProperty("user.dir") + "/models",           // Absolute current dir
            System.getProperty("user.dir") + "/scipathj/models"   // Absolute scipathj dir
        };
        
        for (String path : possiblePaths) {
            File modelsDir = new File(path);
            if (modelsDir.exists() && modelsDir.isDirectory()) {
                // Look for the specific model directory
                File[] modelDirs = modelsDir.listFiles(File::isDirectory);
                if (modelDirs != null) {
                    for (File modelDir : modelDirs) {
                        // Check if this directory contains a SavedModel structure
                        File savedModelPb = new File(modelDir, "saved_model.pb");
                        File variablesDir = new File(modelDir, "variables");
                        
                        if (savedModelPb.exists() && variablesDir.exists()) {
                            // Check if this matches our expected model
                            if (isMatchingModel(modelDir, modelName)) {
                                LOGGER.debug("Found model in directory: {}", modelDir.getAbsolutePath());
                                return modelDir;
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Determine the model name based on the URL.
     */
    private String getModelNameFromUrl() {
        String urlStr = url.toString();
        if (urlStr.contains("he_heavy_augment")) {
            return "he_heavy_augment";
        } else if (urlStr.contains("dsb2018_heavy_augment")) {
            return "dsb2018_heavy_augment";
        } else if (urlStr.contains("dsb2018_paper")) {
            return "dsb2018_paper";
        }
        return null;
    }
    
    /**
     * Check if this model directory matches the expected model.
     * For now, we'll accept any valid SavedModel structure since we only have one model.
     */
    private boolean isMatchingModel(File modelDir, String expectedModelName) {
        // Check if the directory name matches the expected model name
        if (modelDir.getName().equals(expectedModelName)) {
            return true;
        }
        
        // For backward compatibility, also accept any valid SavedModel structure
        // This handles the case where the model directory has a generated name
        File savedModelPb = new File(modelDir, "saved_model.pb");
        File variablesDir = new File(modelDir, "variables");
        
        return savedModelPb.exists() && variablesDir.exists();
    }
        
}
