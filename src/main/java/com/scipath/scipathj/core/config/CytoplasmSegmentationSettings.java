package com.scipath.scipathj.core.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration settings for cytoplasm segmentation using Voronoi tessellation.
 * This class manages all parameters related to cytoplasm detection and cell creation.
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class CytoplasmSegmentationSettings {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CytoplasmSegmentationSettings.class);
    
    // Singleton instance
    private static CytoplasmSegmentationSettings instance;
    
    // Default values based on SCHELI implementation
    public static final boolean DEFAULT_USE_VESSEL_EXCLUSION = true;
    public static final boolean DEFAULT_ADD_IMAGE_BORDER = true;
    public static final int DEFAULT_BORDER_WIDTH = 1;
    public static final boolean DEFAULT_APPLY_VORONOI = true;
    public static final double DEFAULT_MIN_CELL_SIZE = 100.0;
    public static final double DEFAULT_MAX_CELL_SIZE = 10000.0;
    public static final double DEFAULT_MIN_CYTOPLASM_SIZE = 50.0;
    public static final boolean DEFAULT_VALIDATE_CELL_SHAPE = true;
    public static final double DEFAULT_MAX_ASPECT_RATIO = 5.0;
    public static final boolean DEFAULT_LINK_NUCLEUS_TO_CYTOPLASM = true;
    public static final boolean DEFAULT_CREATE_CELL_ROIS = true;
    public static final boolean DEFAULT_EXCLUDE_BORDER_CELLS = false;
    
    @JsonProperty("useVesselExclusion")
    private boolean useVesselExclusion = DEFAULT_USE_VESSEL_EXCLUSION;
    
    @JsonProperty("addImageBorder")
    private boolean addImageBorder = DEFAULT_ADD_IMAGE_BORDER;
    
    @JsonProperty("borderWidth")
    private int borderWidth = DEFAULT_BORDER_WIDTH;
    
    @JsonProperty("applyVoronoi")
    private boolean applyVoronoi = DEFAULT_APPLY_VORONOI;
    
    @JsonProperty("minCellSize")
    private double minCellSize = DEFAULT_MIN_CELL_SIZE;
    
    @JsonProperty("maxCellSize")
    private double maxCellSize = DEFAULT_MAX_CELL_SIZE;
    
    @JsonProperty("minCytoplasmSize")
    private double minCytoplasmSize = DEFAULT_MIN_CYTOPLASM_SIZE;
    
    @JsonProperty("validateCellShape")
    private boolean validateCellShape = DEFAULT_VALIDATE_CELL_SHAPE;
    
    @JsonProperty("maxAspectRatio")
    private double maxAspectRatio = DEFAULT_MAX_ASPECT_RATIO;
    
    @JsonProperty("linkNucleusToCytoplasm")
    private boolean linkNucleusToCytoplasm = DEFAULT_LINK_NUCLEUS_TO_CYTOPLASM;
    
    @JsonProperty("createCellROIs")
    private boolean createCellROIs = DEFAULT_CREATE_CELL_ROIS;
    
    @JsonProperty("excludeBorderCells")
    private boolean excludeBorderCells = DEFAULT_EXCLUDE_BORDER_CELLS;
    
    /**
     * Private constructor for singleton pattern.
     */
    private CytoplasmSegmentationSettings() {
        LOGGER.debug("Created CytoplasmSegmentationSettings with default values");
    }
    
    /**
     * Gets the singleton instance of CytoplasmSegmentationSettings.
     *
     * @return the singleton instance
     */
    public static synchronized CytoplasmSegmentationSettings getInstance() {
        if (instance == null) {
            instance = new CytoplasmSegmentationSettings();
        }
        return instance;
    }
    
    /**
     * Gets whether vessel exclusion should be used.
     * 
     * @return true if vessels should be excluded from cytoplasm regions
     */
    public boolean isUseVesselExclusion() {
        return useVesselExclusion;
    }
    
    /**
     * Alias for isUseVesselExclusion() for compatibility.
     *
     * @return true if vessels should be excluded from cytoplasm regions
     */
    public boolean isExcludeVessels() {
        return useVesselExclusion;
    }
    
    /**
     * Sets whether vessel exclusion should be used.
     * 
     * @param useVesselExclusion true to exclude vessels from cytoplasm regions
     */
    public void setUseVesselExclusion(boolean useVesselExclusion) {
        this.useVesselExclusion = useVesselExclusion;
        LOGGER.debug("Set use vessel exclusion to: {}", useVesselExclusion);
    }
    
    /**
     * Alias for setUseVesselExclusion() for compatibility.
     *
     * @param excludeVessels true to exclude vessels from cytoplasm regions
     */
    public void setExcludeVessels(boolean excludeVessels) {
        setUseVesselExclusion(excludeVessels);
    }
    
    /**
     * Gets whether to add a border around the image.
     * 
     * @return true if image border should be added
     */
    public boolean isAddImageBorder() {
        return addImageBorder;
    }
    
    /**
     * Sets whether to add a border around the image.
     * 
     * @param addImageBorder true to add image border
     */
    public void setAddImageBorder(boolean addImageBorder) {
        this.addImageBorder = addImageBorder;
        LOGGER.debug("Set add image border to: {}", addImageBorder);
    }
    
    /**
     * Gets the width of the image border in pixels.
     * 
     * @return the border width
     */
    public int getBorderWidth() {
        return borderWidth;
    }
    
    /**
     * Sets the width of the image border in pixels.
     * 
     * @param borderWidth the border width (must be positive)
     */
    public void setBorderWidth(int borderWidth) {
        this.borderWidth = Math.max(1, borderWidth);
        LOGGER.debug("Set border width to: {}", this.borderWidth);
    }
    
    /**
     * Gets whether Voronoi tessellation should be applied.
     * 
     * @return true if Voronoi tessellation should be applied
     */
    public boolean isApplyVoronoi() {
        return applyVoronoi;
    }
    
    /**
     * Sets whether Voronoi tessellation should be applied.
     * 
     * @param applyVoronoi true to apply Voronoi tessellation
     */
    public void setApplyVoronoi(boolean applyVoronoi) {
        this.applyVoronoi = applyVoronoi;
        LOGGER.debug("Set apply Voronoi to: {}", applyVoronoi);
    }
    
    /**
     * Gets the minimum cell size in pixels.
     * 
     * @return the minimum cell size
     */
    public double getMinCellSize() {
        return minCellSize;
    }
    
    /**
     * Sets the minimum cell size in pixels.
     * 
     * @param minCellSize the minimum cell size (must be positive)
     */
    public void setMinCellSize(double minCellSize) {
        this.minCellSize = Math.max(0.0, minCellSize);
        LOGGER.debug("Set minimum cell size to: {}", this.minCellSize);
    }
    
    /**
     * Gets the maximum cell size in pixels.
     * 
     * @return the maximum cell size
     */
    public double getMaxCellSize() {
        return maxCellSize;
    }
    
    /**
     * Sets the maximum cell size in pixels.
     * 
     * @param maxCellSize the maximum cell size (must be positive)
     */
    public void setMaxCellSize(double maxCellSize) {
        this.maxCellSize = Math.max(0.0, maxCellSize);
        LOGGER.debug("Set maximum cell size to: {}", this.maxCellSize);
    }
    
    /**
     * Gets the minimum cytoplasm size in pixels.
     * 
     * @return the minimum cytoplasm size
     */
    public double getMinCytoplasmSize() {
        return minCytoplasmSize;
    }
    
    /**
     * Sets the minimum cytoplasm size in pixels.
     * 
     * @param minCytoplasmSize the minimum cytoplasm size (must be positive)
     */
    public void setMinCytoplasmSize(double minCytoplasmSize) {
        this.minCytoplasmSize = Math.max(0.0, minCytoplasmSize);
        LOGGER.debug("Set minimum cytoplasm size to: {}", this.minCytoplasmSize);
    }
    
    /**
     * Alias for getMinCytoplasmSize() for compatibility.
     *
     * @return the minimum cytoplasm area
     */
    public double getMinCytoplasmArea() {
        return minCytoplasmSize;
    }
    
    /**
     * Alias for setMinCytoplasmSize() for compatibility.
     *
     * @param minCytoplasmArea the minimum cytoplasm area (must be positive)
     */
    public void setMinCytoplasmArea(double minCytoplasmArea) {
        setMinCytoplasmSize(minCytoplasmArea);
    }
    
    /**
     * Gets whether cell shape validation should be performed.
     * 
     * @return true if cell shapes should be validated
     */
    public boolean isValidateCellShape() {
        return validateCellShape;
    }
    
    /**
     * Sets whether cell shape validation should be performed.
     * 
     * @param validateCellShape true to validate cell shapes
     */
    public void setValidateCellShape(boolean validateCellShape) {
        this.validateCellShape = validateCellShape;
        LOGGER.debug("Set validate cell shape to: {}", validateCellShape);
    }
    
    /**
     * Gets the maximum allowed aspect ratio for cells.
     * 
     * @return the maximum aspect ratio
     */
    public double getMaxAspectRatio() {
        return maxAspectRatio;
    }
    
    /**
     * Sets the maximum allowed aspect ratio for cells.
     * 
     * @param maxAspectRatio the maximum aspect ratio (must be >= 1.0)
     */
    public void setMaxAspectRatio(double maxAspectRatio) {
        this.maxAspectRatio = Math.max(1.0, maxAspectRatio);
        LOGGER.debug("Set maximum aspect ratio to: {}", this.maxAspectRatio);
    }
    
    /**
     * Gets whether nucleus-cytoplasm linking should be performed.
     * 
     * @return true if nucleus and cytoplasm should be linked
     */
    public boolean isLinkNucleusToCytoplasm() {
        return linkNucleusToCytoplasm;
    }
    
    /**
     * Sets whether nucleus-cytoplasm linking should be performed.
     * 
     * @param linkNucleusToCytoplasm true to link nucleus and cytoplasm
     */
    public void setLinkNucleusToCytoplasm(boolean linkNucleusToCytoplasm) {
        this.linkNucleusToCytoplasm = linkNucleusToCytoplasm;
        LOGGER.debug("Set link nucleus to cytoplasm to: {}", linkNucleusToCytoplasm);
    }
    
    /**
     * Gets whether cell ROIs should be created.
     * 
     * @return true if cell ROIs should be created
     */
    public boolean isCreateCellROIs() {
        return createCellROIs;
    }
    
    /**
     * Sets whether cell ROIs should be created.
     * 
     * @param createCellROIs true to create cell ROIs
     */
    public void setCreateCellROIs(boolean createCellROIs) {
        this.createCellROIs = createCellROIs;
        LOGGER.debug("Set create cell ROIs to: {}", createCellROIs);
    }
    
    /**
     * Gets whether border cells should be excluded.
     * 
     * @return true if border cells should be excluded
     */
    public boolean isExcludeBorderCells() {
        return excludeBorderCells;
    }
    
    /**
     * Sets whether border cells should be excluded.
     * 
     * @param excludeBorderCells true to exclude border cells
     */
    public void setExcludeBorderCells(boolean excludeBorderCells) {
        this.excludeBorderCells = excludeBorderCells;
        LOGGER.debug("Set exclude border cells to: {}", excludeBorderCells);
    }
    
    /**
     * Validates the current settings and returns whether they are valid.
     * 
     * @return true if settings are valid, false otherwise
     */
    public boolean isValid() {
        boolean valid = true;
        
        if (minCellSize >= maxCellSize) {
            LOGGER.warn("Invalid cell size range: min ({}) >= max ({})", minCellSize, maxCellSize);
            valid = false;
        }
        
        if (minCytoplasmSize < 0) {
            LOGGER.warn("Invalid minimum cytoplasm size: {} (must be >= 0)", minCytoplasmSize);
            valid = false;
        }
        
        if (maxAspectRatio < 1.0) {
            LOGGER.warn("Invalid maximum aspect ratio: {} (must be >= 1.0)", maxAspectRatio);
            valid = false;
        }
        
        if (borderWidth < 1) {
            LOGGER.warn("Invalid border width: {} (must be >= 1)", borderWidth);
            valid = false;
        }
        
        return valid;
    }
    
    /**
     * Resets all settings to their default values.
     */
    public void resetToDefaults() {
        this.useVesselExclusion = DEFAULT_USE_VESSEL_EXCLUSION;
        this.addImageBorder = DEFAULT_ADD_IMAGE_BORDER;
        this.borderWidth = DEFAULT_BORDER_WIDTH;
        this.applyVoronoi = DEFAULT_APPLY_VORONOI;
        this.minCellSize = DEFAULT_MIN_CELL_SIZE;
        this.maxCellSize = DEFAULT_MAX_CELL_SIZE;
        this.minCytoplasmSize = DEFAULT_MIN_CYTOPLASM_SIZE;
        this.validateCellShape = DEFAULT_VALIDATE_CELL_SHAPE;
        this.maxAspectRatio = DEFAULT_MAX_ASPECT_RATIO;
        this.linkNucleusToCytoplasm = DEFAULT_LINK_NUCLEUS_TO_CYTOPLASM;
        this.createCellROIs = DEFAULT_CREATE_CELL_ROIS;
        this.excludeBorderCells = DEFAULT_EXCLUDE_BORDER_CELLS;
        
        LOGGER.info("Reset cytoplasm segmentation settings to defaults");
    }
    
    @Override
    public String toString() {
        return String.format(
            "CytoplasmSegmentationSettings[vessels=%s, voronoi=%s, cellSize=%.1f-%.1f, " +
            "cytoplasmSize>=%.1f, aspectRatio<=%.1f, link=%s, createCells=%s]",
            useVesselExclusion, applyVoronoi, minCellSize, maxCellSize,
            minCytoplasmSize, maxAspectRatio, linkNucleusToCytoplasm, createCellROIs
        );
    }
}