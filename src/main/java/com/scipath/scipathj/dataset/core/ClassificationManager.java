package com.scipath.scipathj.dataset.core;

import com.scipath.scipathj.dataset.model.ClassificationClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe manager for classification classes in dataset creation.
 * Handles creation, management, and persistence of classification classes.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public class ClassificationManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassificationManager.class);
    
    private final Map<String, ClassificationClass> classes = new ConcurrentHashMap<>();
    private final AtomicInteger classIdGenerator = new AtomicInteger(0);
    
    // Default colors for classes
    private static final String[] DEFAULT_COLORS = {
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FECA57",
        "#FF9FF3", "#54A0FF", "#5F27CD", "#00D2D3", "#FF9F43",
        "#10AC84", "#EE5A24", "#0ABDE3", "#E17055", "#A29BFE"
    };
    
    /**
     * Creates a new classification class with auto-generated ID and default color.
     * 
     * @param name class name
     * @return created classification class
     * @throws IllegalArgumentException if name is invalid or already exists
     */
    public ClassificationClass createClass(String name) {
        return createClass(name, getNextDefaultColor());
    }
    
    /**
     * Creates a new classification class with custom color.
     * 
     * @param name class name
     * @param color hex color string (e.g., "#FF0000")
     * @return created classification class
     * @throws IllegalArgumentException if parameters are invalid or name already exists
     */
    public ClassificationClass createClass(String name, String color) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Class name cannot be null or empty");
        }
        
        String trimmedName = name.trim();
        if (classes.containsKey(trimmedName)) {
            throw new IllegalArgumentException("Class already exists: " + trimmedName);
        }
        
        int id = classIdGenerator.getAndIncrement();
        ClassificationClass clazz = ClassificationClass.create(trimmedName, id, color);
        classes.put(trimmedName, clazz);
        
        LOGGER.info("Created classification class: {} (ID: {}, Color: {})", trimmedName, id, color);
        return clazz;
    }
    
    /**
     * Gets a classification class by name.
     * 
     * @param name class name
     * @return classification class or null if not found
     */
    public ClassificationClass getClass(String name) {
        return classes.get(name);
    }
    
    /**
     * Gets all classification classes.
     * 
     * @return immutable list of all classes
     */
    public List<ClassificationClass> getAllClasses() {
        return new CopyOnWriteArrayList<>(classes.values());
    }
    
    /**
     * Gets all class names.
     * 
     * @return immutable list of class names
     */
    public List<String> getClassNames() {
        return List.copyOf(classes.keySet());
    }
    
    /**
     * Checks if a class exists.
     * 
     * @param name class name
     * @return true if class exists
     */
    public boolean hasClass(String name) {
        return classes.containsKey(name);
    }
    
    /**
     * Removes a classification class.
     * 
     * @param name class name to remove
     * @return true if class was removed, false if not found
     */
    public boolean removeClass(String name) {
        ClassificationClass removed = classes.remove(name);
        if (removed != null) {
            LOGGER.info("Removed classification class: {}", name);
            return true;
        }
        return false;
    }
    
    /**
     * Gets the number of classes.
     * 
     * @return number of classes
     */
    public int getClassCount() {
        return classes.size();
    }
    
    /**
     * Clears all classes.
     */
    public void clearAll() {
        int count = classes.size();
        classes.clear();
        classIdGenerator.set(0);
        LOGGER.info("Cleared all classification classes ({})", count);
    }
    
    /**
     * Gets the next default color for a new class.
     */
    private String getNextDefaultColor() {
        int index = classes.size() % DEFAULT_COLORS.length;
        return DEFAULT_COLORS[index];
    }
    
    /**
     * Creates a mapping from class names to IDs.
     * 
     * @return map of class name to ID
     */
    public Map<String, Integer> getClassNameToIdMap() {
        return classes.values().stream()
            .collect(java.util.stream.Collectors.toMap(
                ClassificationClass::name,
                ClassificationClass::id
            ));
    }
    
    /**
     * Creates a mapping from IDs to class names.
     * 
     * @return map of ID to class name
     */
    public Map<Integer, String> getIdToClassNameMap() {
        return classes.values().stream()
            .collect(java.util.stream.Collectors.toMap(
                ClassificationClass::id,
                ClassificationClass::name
            ));
    }
    
    /**
     * Creates a mapping from class names to colors.
     * 
     * @return map of class name to color
     */
    public Map<String, String> getClassColorMap() {
        return classes.values().stream()
            .collect(java.util.stream.Collectors.toMap(
                ClassificationClass::name,
                ClassificationClass::color
            ));
    }
    
    @Override
    public String toString() {
        return String.format("ClassificationManager[classes=%d]", classes.size());
    }
}