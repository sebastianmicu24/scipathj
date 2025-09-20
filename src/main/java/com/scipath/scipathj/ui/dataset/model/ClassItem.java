package com.scipath.scipathj.ui.dataset.model;

import java.awt.Color;
import java.util.Objects;

/**
 * Represents a classification class with name and color.
 * Used in combo boxes and class management panels.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class ClassItem {
    private final String name;
    private final Color color;

    /**
     * Creates a new ClassItem with name and color.
     *
     * @param name  The class name (cannot be null)
     * @param color The class color (cannot be null)
     * @throws IllegalArgumentException if name or color is null
     */
    public ClassItem(String name, Color color) {
        if (name == null) {
            throw new IllegalArgumentException("Class name cannot be null");
        }
        if (color == null) {
            throw new IllegalArgumentException("Class color cannot be null");
        }

        this.name = name;
        this.color = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()); // defensive copy
    }

    /**
     * Gets the class name.
     *
     * @return The class name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the class color.
     *
     * @return The class color
     */
    public Color getColor() {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()); // defensive copy
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ClassItem other)) {
            return false;
        }
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}