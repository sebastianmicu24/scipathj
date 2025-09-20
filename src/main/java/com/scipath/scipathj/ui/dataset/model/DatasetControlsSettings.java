package com.scipath.scipathj.ui.dataset.model;

/**
 * Settings container for DatasetControlsPanel configuration.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class DatasetControlsSettings {
    private final String defaultClassName;
    private final boolean showTrainingFeatures;
    private final boolean enableBulkOperations;
    private final int maxUndoSteps;

    /**
     * Creates new DatasetControlsSettings with default values.
     */
    public DatasetControlsSettings() {
        this.defaultClassName = "Unclassified";
        this.showTrainingFeatures = true;
        this.enableBulkOperations = true;
        this.maxUndoSteps = 10;
    }

    /**
     * Creates new DatasetControlsSettings with specified values.
     */
    public DatasetControlsSettings(String defaultClassName, boolean showTrainingFeatures,
                                   boolean enableBulkOperations, int maxUndoSteps) {
        this.defaultClassName = defaultClassName != null ? defaultClassName : "Unclassified";
        this.showTrainingFeatures = showTrainingFeatures;
        this.enableBulkOperations = enableBulkOperations;
        this.maxUndoSteps = Math.max(1, Math.min(100, maxUndoSteps));
    }

    public String getDefaultClassName() {
        return defaultClassName;
    }

    public boolean isShowTrainingFeatures() {
        return showTrainingFeatures;
    }

    public boolean isEnableBulkOperations() {
        return enableBulkOperations;
    }

    public int getMaxUndoSteps() {
        return maxUndoSteps;
    }

    /**
     * Creates a copy of this settings object.
     */
    public DatasetControlsSettings copy() {
        return new DatasetControlsSettings(defaultClassName, showTrainingFeatures,
                                         enableBulkOperations, maxUndoSteps);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DatasetControlsSettings other)) {
            return false;
        }
        return defaultClassName.equals(other.defaultClassName) &&
               showTrainingFeatures == other.showTrainingFeatures &&
               enableBulkOperations == other.enableBulkOperations &&
               maxUndoSteps == other.maxUndoSteps;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(defaultClassName, showTrainingFeatures,
                                    enableBulkOperations, maxUndoSteps);
    }

    @Override
    public String toString() {
        return String.format("DatasetControlsSettings{defaultClassName='%s', showTrainingFeatures=%s, " +
                           "enableBulkOperations=%s, maxUndoSteps=%d}",
                           defaultClassName, showTrainingFeatures, enableBulkOperations, maxUndoSteps);
    }
}