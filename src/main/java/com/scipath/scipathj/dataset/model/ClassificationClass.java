package com.scipath.scipathj.dataset.model;

import java.time.LocalDateTime;

/**
 * Immutable record representing a classification class for dataset creation.
 * Uses modern Java record pattern for thread safety and immutability.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public record ClassificationClass(
    String name,
    int id,
    String color,
    LocalDateTime createdAt
) {
    
    /**
     * Creates a new classification class with current timestamp.
     */
    public static ClassificationClass create(String name, int id, String color) {
        return new ClassificationClass(name, id, color, LocalDateTime.now());
    }
    
    /**
     * Validates the class parameters.
     */
    public ClassificationClass {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Class name cannot be null or empty");
        }
        if (id < 0) {
            throw new IllegalArgumentException("Class ID must be non-negative");
        }
        if (color == null || !color.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("Color must be a valid hex color (e.g., #FF0000)");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created timestamp cannot be null");
        }
    }
    
    /**
     * Returns a display-friendly string representation.
     */
    @Override
    public String toString() {
        return String.format("ClassificationClass[name='%s', id=%d, color='%s']", 
                           name, id, color);
    }
}