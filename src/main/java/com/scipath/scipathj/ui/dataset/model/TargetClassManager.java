package com.scipath.scipathj.ui.dataset.model;

import com.scipath.scipathj.ui.dataset.model.ClassItem;
import com.scipath.scipathj.infrastructure.roi.UserROI;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages ROI classification classes with colors and counters.
 * Handles class creation, modification, and ROI class assignment.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class TargetClassManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TargetClassManager.class);

    // Default/unclassified color
    private static final Color UNCLASSIFIED_COLOR = new Color(255, 255, 0);  // Yellow

    // Class storage
    private final Map<String, Color> classColors = new HashMap<>();
    private final Map<String, Integer> classCounts = new HashMap<>();
    private final Set<String> classNames = new HashSet<>();

    // Model for combo box
    private final DefaultComboBoxModel<ClassItem> classModel;
    private JComboBox<ClassItem> classComboBox;

    // Selected class tracking
    private String selectedClass = "Unclassified";

    /**
     * Creates a new TargetClassManager with default classes.
     */
    public TargetClassManager() {
        this.classModel = new DefaultComboBoxModel<>();
        initializeDefaultClasses();
    }

    /**
     * Initialize default classes.
     */
    private void initializeDefaultClasses() {
        addClass("Unclassified", UNCLASSIFIED_COLOR);
        classCounts.put("Unclassified", 0);
    }

    /**
     * Add a new class with specified color.
     *
     * @param className The class name
     * @param color The class color
     * @return true if class was added, false if it already exists
     */
    public boolean addClass(String className, Color color) {
        if (className == null || className.trim().isEmpty()) {
            LOGGER.warn("Cannot add class with empty name");
            return false;
        }

        if (classColors.containsKey(className)) {
            LOGGER.debug("Class '{}' already exists", className);
            return false;
        }

        classNames.add(className);
        classColors.put(className, color);
        classCounts.put(className, 0);

        ClassItem item = new ClassItem(className, color);
        classModel.addElement(item);

        // Auto-select if first class added
        if (classModel.getSize() == 1) {
            setSelectedClass(className);
        }

        LOGGER.info("Added class '{}' with color {}", className, color);
        return true;
    }

    /**
     * Remove a class.
     *
     * @param className The class name to remove
     * @return true if class was removed
     */
    public boolean removeClass(String className) {
        if ("Unclassified".equals(className)) {
            LOGGER.warn("Cannot remove 'Unclassified' class");
            return false;
        }

        if (!classColors.containsKey(className)) {
            LOGGER.warn("Class '{}' does not exist", className);
            return false;
        }

        // Remove from all data structures
        classNames.remove(className);
        classColors.remove(className);
        classCounts.remove(className);

        // Remove from model
        for (int i = classModel.getSize() - 1; i >= 0; i--) {
            ClassItem item = classModel.getElementAt(i);
            if (className.equals(item.getName())) {
                classModel.removeElement(item);
                break;
            }
        }

        // Update selection if necessary
        if (className.equals(selectedClass)) {
            if (classModel.getSize() > 0) {
                ClassItem item = classModel.getElementAt(0);
                setSelectedClass(item.getName());
            } else {
                selectedClass = null;
            }
        }

        LOGGER.info("Removed class '{}'", className);
        return true;
    }

    /**
     * Update the color of an existing class.
     *
     * @param className The class name
     * @param newColor The new color
     * @return true if color was updated
     */
    public boolean updateClassColor(String className, Color newColor) {
        if (!classColors.containsKey(className)) {
            LOGGER.warn("Class '{}' does not exist", className);
            return false;
        }

        classColors.put(className, newColor);

        // Update in model
        for (int i = 0; i < classModel.getSize(); i++) {
            ClassItem item = classModel.getElementAt(i);
            if (className.equals(item.getName())) {
                classModel.removeElementAt(i);
                classModel.insertElementAt(new ClassItem(className, newColor), i);
                break;
            }
        }

        LOGGER.debug("Updated color for class '{}' to {}", className, newColor);
        return true;
    }

    /**
     * Get color for a class.
     *
     * @param className The class name
     * @return The class color, or UNCLASSIFIED_COLOR if not found
     */
    public Color getClassColor(String className) {
        return classColors.getOrDefault(className, UNCLASSIFIED_COLOR);
    }

    /**
     * Get count for a class.
     *
     * @param className The class name
     * @return The class count, or 0 if not found
     */
    public int getClassCount(String className) {
        return classCounts.getOrDefault(className, 0);
    }

    /**
     * Set count for a class.
     *
     * @param className The class name
     * @param count The new count
     */
    public void setClassCount(String className, int count) {
        if (count < 0) {
            count = 0;
        }
        classCounts.put(className, count);
        LOGGER.debug("Set count for class '{}' to {}", className, count);
    }

    /**
     * Increment count for a class.
     *
     * @param className The class name
     * @return The new count
     */
    public int incrementClassCount(String className) {
        int currentCount = getClassCount(className);
        setClassCount(className, currentCount + 1);
        return currentCount + 1;
    }

    /**
     * Decrement count for a class.
     *
     * @param className The class name
     * @return The new count
     */
    public int decrementClassCount(String className) {
        int currentCount = getClassCount(className);
        setClassCount(className, Math.max(0, currentCount - 1));
        return Math.max(0, currentCount - 1);
    }

    /**
     * Check if a class exists.
     *
     * @param className The class name
     * @return true if class exists
     */
    public boolean containsClass(String className) {
        return classColors.containsKey(className);
    }

    /**
     * Get all class names.
     *
     * @return Set of class names
     */
    public Set<String> getClassNames() {
        return new HashSet<>(classNames);
    }

    /**
     * Get all class colors.
     *
     * @return Map of class names to colors
     */
    public Map<String, Color> getClassColors() {
        return new HashMap<>(classColors);
    }

    /**
     * Get all class counts.
     *
     * @return Map of class names to counts
     */
    public Map<String, Integer> getClassCounts() {
        return new HashMap<>(classCounts);
    }

    /**
     * Set the selected class.
     *
     * @param className The class name to select
     * @return true if class was selected
     */
    public boolean setSelectedClass(String className) {
        if (!classColors.containsKey(className)) {
            LOGGER.warn("Cannot select non-existent class '{}'", className);
            return false;
        }

        selectedClass = className;

        // Update combo box if available
        if (classComboBox != null) {
            for (int i = 0; i < classModel.getSize(); i++) {
                ClassItem item = classModel.getElementAt(i);
                if (className.equals(item.getName())) {
                    classComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        LOGGER.debug("Selected class '{}'", className);
        return true;
    }

    /**
     * Get the currently selected class.
     *
     * @return The selected class name
     */
    public String getSelectedClass() {
        return selectedClass;
    }

    /**
     * Get the ClassItem for the selected class.
     *
     * @return The selected ClassItem
     */
    public ClassItem getSelectedClassItem() {
        for (int i = 0; i < classModel.getSize(); i++) {
            ClassItem item = classModel.getElementAt(i);
            if (selectedClass != null && selectedClass.equals(item.getName())) {
                return item;
            }
        }
        return null;
    }

    /**
     * Set the JComboBox reference for synchronization.
     *
     * @param comboBox The combo box
     */
    public void setComboBox(JComboBox<ClassItem> comboBox) {
        this.classComboBox = comboBox;
        this.classComboBox.setModel(classModel);
    }

    /**
     * Get the combo box model.
     *
     * @return The DefaultComboBoxModel
     */
    public DefaultComboBoxModel<ClassItem> getComboBoxModel() {
        return classModel;
    }

    /**
     * Reset class counts for all classes.
     */
    public void resetCounts() {
        for (String className : classNames) {
            classCounts.put(className, 0);
        }
        classCounts.put("Unclassified", 0);
        LOGGER.debug("Reset counts for all classes");
    }

    /**
     * Extract cell ID from ROI name (utility method).
     *
     * @param roiName The ROI name
     * @return The extracted cell ID
     */
    public static String extractCellIdFromROIName(String roiName) {
        if (roiName == null || roiName.isEmpty()) {
            return "unknown";
        }

        int lastUnderscoreIndex = roiName.lastIndexOf('_');
        if (lastUnderscoreIndex > 0 && lastUnderscoreIndex < roiName.length() - 1) {
            return roiName.substring(lastUnderscoreIndex + 1);
        }

        return roiName;
    }

    /**
     * Calculate contrast color for text display on colored background.
     */
    public static Color getContrastColor(Color color) {
        // Calculate luminance
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255.0;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    @Override
    public String toString() {
        return String.format("TargetClassManager{classes=%d, selected='%s', counts=%s}",
                           classNames.size(), selectedClass, classCounts);
    }
}