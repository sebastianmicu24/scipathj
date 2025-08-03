package com.scipath.scipathj.data.model;

import ij.gui.Roi;

import java.util.Objects;

/**
 * Represents a single cell with its nucleus, cytoplasm, features, and classification.
 * 
 * <p>This is the fundamental data structure in SciPathJ, combining morphological
 * components (nucleus and cytoplasm) with extracted features and classification results.</p>
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class Cell {
    
    private final String id;
    private final NucleusROI nucleus;
    private final CytoplasmROI cytoplasm;
    private final FeatureVector features;
    private final ClassificationResult classification;
    private final SpatialMetrics spatialMetrics;
    
    /**
     * Creates a new Cell instance.
     * 
     * @param id unique cell identifier
     * @param nucleus nucleus ROI
     * @param cytoplasm cytoplasm ROI
     * @param features extracted features
     * @param classification classification result
     * @param spatialMetrics spatial analysis metrics
     */
    public Cell(String id, 
                NucleusROI nucleus, 
                CytoplasmROI cytoplasm,
                FeatureVector features,
                ClassificationResult classification,
                SpatialMetrics spatialMetrics) {
        this.id = Objects.requireNonNull(id, "Cell ID cannot be null");
        this.nucleus = Objects.requireNonNull(nucleus, "Nucleus cannot be null");
        this.cytoplasm = cytoplasm; // Can be null for nucleus-only analysis
        this.features = Objects.requireNonNull(features, "Features cannot be null");
        this.classification = classification; // Can be null if not classified yet
        this.spatialMetrics = spatialMetrics; // Can be null if spatial analysis not performed
    }
    
    /**
     * Gets the unique cell identifier.
     * 
     * @return cell ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the nucleus ROI.
     * 
     * @return nucleus ROI
     */
    public NucleusROI getNucleus() {
        return nucleus;
    }
    
    /**
     * Gets the cytoplasm ROI.
     * 
     * @return cytoplasm ROI, or null if not available
     */
    public CytoplasmROI getCytoplasm() {
        return cytoplasm;
    }
    
    /**
     * Gets the extracted features.
     * 
     * @return feature vector
     */
    public FeatureVector getFeatures() {
        return features;
    }
    
    /**
     * Gets the classification result.
     * 
     * @return classification result, or null if not classified
     */
    public ClassificationResult getClassification() {
        return classification;
    }
    
    /**
     * Gets the spatial analysis metrics.
     * 
     * @return spatial metrics, or null if not available
     */
    public SpatialMetrics getSpatialMetrics() {
        return spatialMetrics;
    }
    
    /**
     * Checks if this cell has cytoplasm information.
     * 
     * @return true if cytoplasm is available
     */
    public boolean hasCytoplasm() {
        return cytoplasm != null;
    }
    
    /**
     * Checks if this cell has been classified.
     * 
     * @return true if classification is available
     */
    public boolean isClassified() {
        return classification != null;
    }
    
    /**
     * Checks if this cell has spatial metrics.
     * 
     * @return true if spatial metrics are available
     */
    public boolean hasSpatialMetrics() {
        return spatialMetrics != null;
    }
    
    /**
     * Gets the total cell area (nucleus + cytoplasm).
     * 
     * @return total cell area in pixels
     */
    public double getTotalArea() {
        double area = nucleus.getArea();
        if (cytoplasm != null) {
            area += cytoplasm.getArea();
        }
        return area;
    }
    
    /**
     * Gets the nucleus-to-cytoplasm ratio.
     * 
     * @return N/C ratio, or Double.NaN if cytoplasm is not available
     */
    public double getNucleusCytoplasmRatio() {
        if (cytoplasm == null) {
            return Double.NaN;
        }
        
        double cytoArea = cytoplasm.getArea();
        if (cytoArea == 0) {
            return Double.POSITIVE_INFINITY;
        }
        
        return nucleus.getArea() / cytoArea;
    }
    
    /**
     * Gets the centroid coordinates of the cell (nucleus center).
     * 
     * @return centroid coordinates as [x, y]
     */
    public double[] getCentroid() {
        return new double[]{nucleus.getCentroidX(), nucleus.getCentroidY()};
    }
    
    /**
     * Gets a specific feature value by name.
     * 
     * @param featureName name of the feature
     * @return feature value, or Double.NaN if not found
     */
    public double getFeature(String featureName) {
        return features.getFeature(featureName);
    }
    
    /**
     * Gets the predicted cell type (if classified).
     * 
     * @return predicted cell type, or null if not classified
     */
    public String getPredictedType() {
        return classification != null ? classification.getPredictedClass() : null;
    }
    
    /**
     * Gets the classification confidence (if classified).
     * 
     * @return confidence score (0.0 to 1.0), or Double.NaN if not classified
     */
    public double getClassificationConfidence() {
        return classification != null ? classification.getConfidence() : Double.NaN;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Cell cell = (Cell) obj;
        return Objects.equals(id, cell.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Cell{id='%s', type='%s', area=%.1f, classified=%s}", 
                           id, getPredictedType(), getTotalArea(), isClassified());
    }
}