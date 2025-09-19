package com.scipath.scipathj.training.model;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Immutable configuration record for XGBoost model training.
 * Contains all parameters needed to train a model.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public record TrainingConfiguration(
    File dataFile,
    Path outputDir,
    List<String> selectedFeatures,
    XGBoostParameters parameters,
    DataSplitStrategy splitStrategy
) {
    
    /**
     * XGBoost hyperparameters.
     */
    public record XGBoostParameters(
        float learningRate,
        int maxDepth,
        int numTrees,
        float minChildWeight,
        float subsample,
        float colsampleBytree,
        float lambda,
        float alpha,
        float gamma,
        boolean balanceClasses
    ) {
        
        /**
         * Creates default XGBoost parameters.
         */
        public static XGBoostParameters createDefault() {
            return new XGBoostParameters(
                0.1f,     // learningRate
                6,        // maxDepth
                100,      // numTrees
                1.0f,     // minChildWeight
                1.0f,     // subsample
                1.0f,     // colsampleBytree
                1.0f,     // lambda
                0.0f,     // alpha
                0.0f,     // gamma
                false     // balanceClasses
            );
        }
    }
    
    /**
     * Data splitting strategy.
     */
    public record DataSplitStrategy(
        float trainRatio,
        boolean stratified,
        long randomSeed
    ) {
        
        /**
         * Creates default split strategy.
         */
        public static DataSplitStrategy createDefault() {
            return new DataSplitStrategy(0.7f, true, 42L);
        }
    }
    
    /**
     * Creates a default configuration.
     */
    public static TrainingConfiguration createDefault(File dataFile, Path outputDir) {
        return new TrainingConfiguration(
            dataFile,
            outputDir,
            List.of(), // Empty list means all features
            XGBoostParameters.createDefault(),
            DataSplitStrategy.createDefault()
        );
    }
    
    /**
     * Validates the configuration parameters.
     */
    public TrainingConfiguration {
        if (dataFile == null || !dataFile.exists()) {
            throw new IllegalArgumentException("Data file must exist");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("Output directory cannot be null");
        }
        if (selectedFeatures == null) {
            throw new IllegalArgumentException("Selected features list cannot be null");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("XGBoost parameters cannot be null");
        }
        if (splitStrategy == null) {
            throw new IllegalArgumentException("Split strategy cannot be null");
        }
    }
    
    /**
     * Returns a display name for this configuration.
     */
    public String getDisplayName() {
        return String.format("Training[%s, %d trees, lr=%.3f]", 
                           dataFile.getName(), 
                           parameters.numTrees(), 
                           parameters.learningRate());
    }
    
    /**
     * Returns the data file path as string.
     */
    public String getDataFilePath() {
        return dataFile.getAbsolutePath();
    }
    
    /**
     * Returns the output directory path as string.
     */
    public String getOutputDirPath() {
        return outputDir.toString();
    }
}