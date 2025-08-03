package net.imagej.tensorflow;

import org.scijava.download.DiskLocationCache;
import org.scijava.io.location.Location;
import org.scijava.service.Service;

import java.io.File;

/**
 * Service interface for TensorFlow operations with Java 21 compatibility.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 */
public interface TensorFlowService extends Service {
    
    /**
     * Load the TensorFlow native library.
     *
     * @throws RuntimeException if library loading fails
     */
    void loadLibrary();
    
    /**
     * Get the TensorFlow JAR version.
     *
     * @return the version string
     */
    String getJarVersion();
    
    /**
     * Get the TensorFlow version.
     *
     * @return the TensorFlow version object
     */
    default TensorFlowVersion getTensorFlowVersion() {
        return new TensorFlowVersion(getJarVersion());
    }
    
    /**
     * Get the model cache for storing downloaded models.
     *
     * @return the disk location cache
     */
    DiskLocationCache modelCache();
    
    /**
     * Get the directory for storing a specific model.
     *
     * @param modelName the name of the model
     * @return the model directory
     */
    File modelDir(String modelName);
    
    /**
     * Load a cached model (simple version).
     *
     * @param modelName the name of the model
     * @param modelUrl the URL of the model
     * @return the cached model bundle, or null if not cached
     */
    CachedModelBundle loadCachedModel(String modelName, String modelUrl);
    
    /**
     * Load a cached model (full version with location).
     *
     * @param location the model location
     * @param modelName the name of the model
     * @param modelUrl the URL of the model
     * @return the cached model bundle, or null if not cached
     */
    default CachedModelBundle loadCachedModel(Location location, String modelName, String modelUrl) {
        return loadCachedModel(modelName, modelUrl);
    }
    
    /**
     * Check if the TensorFlow library is loaded.
     *
     * @return true if loaded, false otherwise
     */
    boolean isLibraryLoaded();
    
    /**
     * Get the status of the TensorFlow service.
     *
     * @return TensorFlow version with status
     */
    default TensorFlowVersion getStatus() {
        String version = getJarVersion();
        boolean loaded = isLibraryLoaded();
        String info = loaded ? "TensorFlow loaded successfully" : "TensorFlow not loaded";
        return new TensorFlowVersion(version, loaded, info);
    }
}