package net.imagej.tensorflow;

/**
 * Represents a cached TensorFlow model bundle with Java 21 compatibility.
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 */
public class CachedModelBundle {
    
    private final String modelName;
    private final String modelUrl;
    private final Object model;
    private final boolean cached;
    
    public CachedModelBundle(String modelName, String modelUrl, Object model, boolean cached) {
        this.modelName = modelName;
        this.modelUrl = modelUrl;
        this.model = model;
        this.cached = cached;
    }
    
    public CachedModelBundle(String modelName, String modelUrl) {
        this(modelName, modelUrl, null, false);
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public String getModelUrl() {
        return modelUrl;
    }
    
    public Object getModel() {
        return model;
    }
    
    public boolean isCached() {
        return cached;
    }
    /**
     * Get the underlying model object.
     * @return the model object
     */
    public Object model() {
        return model;
    }
    
    /**
     * Get the meta graph definition (compatibility method).
     * @return the MetaGraphDef bytes if available, null otherwise
     */
    public byte[] metaGraphDef() {
        if (model instanceof org.tensorflow.SavedModelBundle) {
            try {
                return ((org.tensorflow.SavedModelBundle) model).metaGraphDef();
            } catch (Exception e) {
                System.err.println("Error getting metaGraphDef: " + e.getMessage());
                return null;
            }
        }
        return null;
    }
    
    /**
     * Close the model and release resources.
     */
    public void close() {
        // For now, we don't have specific cleanup logic
        // This method exists to maintain compatibility with existing code
    }
    
    @Override
    public String toString() {
        return String.format("CachedModelBundle[name=%s, cached=%s]", modelName, cached);
    }
}