package com.scipath.scipathj.ui.dataset.model;

/**
 * Holds the state of visual controls for ROI display.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class VisualControlsState {
    private float borderWidth;
    private float fillOpacity;
    private boolean showNuclei;
    private boolean showCells;

    /**
     * Creates a new VisualControlsState with default values.
     */
    public VisualControlsState() {
        this.borderWidth = 2.0f;
        this.fillOpacity = 0.2f;
        this.showNuclei = true;
        this.showCells = true;
    }

    /**
     * Creates a new VisualControlsState with specified values.
     */
    public VisualControlsState(float borderWidth, float fillOpacity, boolean showNuclei, boolean showCells) {
        this.borderWidth = borderWidth;
        this.fillOpacity = fillOpacity;
        this.showNuclei = showNuclei;
        this.showCells = showCells;
    }

    // Getters and setters
    public float getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(float borderWidth) {
        this.borderWidth = Math.max(0.0f, Math.min(10.0f, borderWidth)); // Clamp between 0-10
    }

    public float getFillOpacity() {
        return fillOpacity;
    }

    public void setFillOpacity(float fillOpacity) {
        this.fillOpacity = Math.max(0.0f, Math.min(1.0f, fillOpacity)); // Clamp between 0-1
    }

    public boolean isShowNuclei() {
        return showNuclei;
    }

    public void setShowNuclei(boolean showNuclei) {
        this.showNuclei = showNuclei;
    }

    public boolean isShowCells() {
        return showCells;
    }

    public void setShowCells(boolean showCells) {
        this.showCells = showCells;
    }

    /**
     * Creates a copy of this visual controls state.
     *
     * @return A new VisualControlsState with the same values
     */
    public VisualControlsState copy() {
        return new VisualControlsState(borderWidth, fillOpacity, showNuclei, showCells);
    }

    /**
     * Resets to default values.
     */
    public void resetToDefaults() {
        borderWidth = 2.0f;
        fillOpacity = 0.2f;
        showNuclei = true;
        showCells = true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VisualControlsState other)) {
            return false;
        }
        return Float.compare(borderWidth, other.borderWidth) == 0 &&
               Float.compare(fillOpacity, other.fillOpacity) == 0 &&
               showNuclei == other.showNuclei &&
               showCells == other.showCells;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(borderWidth, fillOpacity, showNuclei, showCells);
    }

    @Override
    public String toString() {
        return String.format("VisualControlsState{borderWidth=%.1f, fillOpacity=%.2f, showNuclei=%s, showCells=%s}",
                           borderWidth, fillOpacity, showNuclei, showCells);
    }
}