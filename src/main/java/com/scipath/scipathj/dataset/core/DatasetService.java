package com.scipath.scipathj.dataset.core;

import com.scipath.scipathj.dataset.model.ClassificationClass;
import com.scipath.scipathj.dataset.model.TrainingDataset;
import com.scipath.scipathj.dataset.model.DatasetConfiguration;
import com.scipath.scipathj.dataset.repository.ROIRepository;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Core service for dataset creation operations.
 * Provides clean separation of concerns and proper resource management.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public class DatasetService implements AutoCloseable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetService.class);
    
    private final ROIRepository roiRepository;
    private final ClassificationManager classificationManager;
    private final FeatureExtractionService featureService;
    private final TrainingDataExporter exporter;
    
    /**
     * Creates a new dataset service with dependency injection.
     */
    public DatasetService(ROIRepository roiRepository, 
                         ClassificationManager classificationManager,
                         FeatureExtractionService featureService,
                         TrainingDataExporter exporter) {
        this.roiRepository = roiRepository;
        this.classificationManager = classificationManager;
        this.featureService = featureService;
        this.exporter = exporter;
        
        LOGGER.info("DatasetService initialized");
    }
    
    /**
     * Creates a training dataset from the current configuration.
     * 
     * @param config dataset configuration
     * @return complete training dataset
     * @throws DatasetException if creation fails
     */
    public TrainingDataset createTrainingDataset(DatasetConfiguration config) throws DatasetException {
        LOGGER.info("Creating training dataset from configuration");
        
        try {
            // Load ROIs from repository
            Map<String, UserROI> rois = roiRepository.loadROIs(config.roiZipFile(), config.imageFolder());
            
            // Filter for classified ROIs only
            Map<String, UserROI> classifiedROIs = filterClassifiedROIs(rois);
            
            if (classifiedROIs.isEmpty()) {
                throw new DatasetException("No classified ROIs found in dataset");
            }
            
            // Extract features for all classified ROIs
            Map<String, Map<String, Double>> features = featureService.extractFeatures(
                classifiedROIs, config.imageFolder());
            
            // Get class information
            List<ClassificationClass> classes = classificationManager.getAllClasses();
            
            // Build dataset
            TrainingDataset dataset = new TrainingDataset(
                classifiedROIs,
                features,
                classes,
                config
            );
            
            LOGGER.info("Training dataset created with {} ROIs and {} classes", 
                       classifiedROIs.size(), classes.size());
            
            return dataset;
            
        } catch (Exception e) {
            LOGGER.error("Failed to create training dataset", e);
            throw new DatasetException("Failed to create training dataset", e);
        }
    }
    
    /**
     * Exports training dataset to JSON format.
     * 
     * @param dataset training dataset to export
     * @param outputPath output file path
     * @throws DatasetException if export fails
     */
    public void exportTrainingData(TrainingDataset dataset, Path outputPath) throws DatasetException {
        LOGGER.info("Exporting training dataset to: {}", outputPath);
        
        try {
            exporter.exportToJSON(dataset, outputPath);
            LOGGER.info("Training dataset successfully exported");
            
        } catch (Exception e) {
            LOGGER.error("Failed to export training dataset", e);
            throw new DatasetException("Failed to export training dataset", e);
        }
    }
    
    /**
     * Filters ROIs to include only those with assigned classes.
     */
    private Map<String, UserROI> filterClassifiedROIs(Map<String, UserROI> allROIs) {
        return allROIs.entrySet().stream()
            .filter(entry -> entry.getValue().getAssignedClass() != null && 
                           !entry.getValue().getAssignedClass().equals("Unclassified"))
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
    }
    
    /**
     * Gets the classification manager.
     */
    public ClassificationManager getClassificationManager() {
        return classificationManager;
    }
    
    /**
     * Gets the feature extraction service.
     */
    public FeatureExtractionService getFeatureService() {
        return featureService;
    }
    
    @Override
    public void close() {
        LOGGER.debug("Closing DatasetService");
        
        // Close dependent services if they implement AutoCloseable
        try {
            if (featureService instanceof AutoCloseable) {
                ((AutoCloseable) featureService).close();
            }
        } catch (Exception e) {
            LOGGER.warn("Error closing feature service", e);
        }
        
        try {
            if (roiRepository instanceof AutoCloseable) {
                ((AutoCloseable) roiRepository).close();
            }
        } catch (Exception e) {
            LOGGER.warn("Error closing ROI repository", e);
        }
    }
}