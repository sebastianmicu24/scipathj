package com.scipath.scipathj.ui.dataset;

import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Panel for managing classification classes in dataset creation.
 * Handles adding, removing, and selecting classes for ROI assignment.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class DatasetClassManager extends JPanel {

    private List<String> classNames;
    private String selectedClass;
    private JComboBox<String> classSelector;
    private JButton addClassButton;
    private JButton removeClassButton;
    private JTextField newClassField;

    /**
     * Creates a new dataset class manager.
     */
    public DatasetClassManager() {
        this.classNames = new ArrayList<>();
        initializeComponents();

        // Add default classes
        addDefaultClasses();
    }

    /**
     * Initializes the panel components.
     */
    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING));
        setPreferredSize(new Dimension(-1, 80));

        // Title
        add(UIUtils.createBoldLabel("Class Management", UIConstants.NORMAL_FONT_SIZE), BorderLayout.NORTH);

        // Main content
        add(createMainContent(), BorderLayout.CENTER);
    }

    /**
     * Creates the main content with class controls.
     */
    private JPanel createMainContent() {
        JPanel content = new JPanel(new BorderLayout());

        // Left side - Class controls
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.add(UIUtils.createLabel("Classes:", UIConstants.NORMAL_FONT_SIZE, null));

        addClassButton = UIUtils.createButton("Add Class", FontAwesomeSolid.PLUS, e -> showAddClassDialog());
        leftPanel.add(addClassButton);

        removeClassButton = UIUtils.createButton("Remove Class", FontAwesomeSolid.MINUS, e -> removeSelectedClass());
        leftPanel.add(removeClassButton);

        content.add(leftPanel, BorderLayout.WEST);

        // Center - Class selector
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        centerPanel.add(UIUtils.createLabel("Selected Class:", UIConstants.SMALL_FONT_SIZE,
                                          UIManager.getColor("Label.foreground")));

        classSelector = new JComboBox<>();
        classSelector.addActionListener(e -> selectedClass = (String) classSelector.getSelectedItem());
        centerPanel.add(classSelector);

        content.add(centerPanel, BorderLayout.CENTER);

        // Right side - Instructions
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.add(UIUtils.createLabel("Select a class to assign ROIs", UIConstants.SMALL_FONT_SIZE,
                                         UIManager.getColor("Label.disabledForeground")));
        content.add(rightPanel, BorderLayout.EAST);

        return content;
    }

    /**
     * Adds the default classes.
     */
    private void addDefaultClasses() {
        addClass("Class 1");
        addClass("Class 2");
        if (!classNames.isEmpty()) {
            selectedClass = classNames.get(0);
            updateClassSelector();
        }
    }

    /**
     * Shows dialog for adding a new class.
     */
    private void showAddClassDialog() {
        String className = JOptionPane.showInputDialog(this,
            "Enter class name:",
            "Add New Class",
            JOptionPane.QUESTION_MESSAGE);

        if (className != null && !className.trim().isEmpty()) {
            addClass(className.trim());
            JOptionPane.showMessageDialog(this,
                "Class '" + className + "' added successfully!",
                "Class Added",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Removes the currently selected class.
     */
    private void removeSelectedClass() {
        if (classNames.size() <= 1) {
            JOptionPane.showMessageDialog(this,
                "Cannot remove the last class!",
                "Cannot Remove",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String classToRemove = (String) JOptionPane.showInputDialog(
            this,
            "Select class to remove:",
            "Remove Class",
            JOptionPane.QUESTION_MESSAGE,
            null,
            classNames.toArray(),
            selectedClass
        );

        if (classToRemove != null) {
            removeClass(classToRemove);
            JOptionPane.showMessageDialog(this,
                "Class '" + classToRemove + "' removed successfully!",
                "Class Removed",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Adds a new class.
     */
    public void addClass(String className) {
        if (className != null && !className.trim().isEmpty() && !classNames.contains(className)) {
            classNames.add(className.trim());
            selectedClass = className;
            updateClassSelector();
        }
    }

    /**
     * Removes a class.
     */
    public void removeClass(String className) {
        if (classNames.size() > 1 && classNames.contains(className)) {
            classNames.remove(className);

            // Update selected class if it was removed
            if (selectedClass.equals(className)) {
                selectedClass = classNames.isEmpty() ? null : classNames.get(0);
            }

            updateClassSelector();
        }
    }

    /**
     * Updates the class selector combo box.
     */
    private void updateClassSelector() {
        if (classSelector != null) {
            classSelector.removeAllItems();
            for (String className : classNames) {
                classSelector.addItem(className);
            }
            if (selectedClass != null) {
                classSelector.setSelectedItem(selectedClass);
            }
        }
    }

    /**
     * Gets the currently selected class name.
     */
    public String getSelectedClassName() {
        return selectedClass;
    }

    /**
     * Sets the selected class.
     */
    public void setSelectedClass(String className) {
        if (classNames.contains(className)) {
            selectedClass = className;
            if (classSelector != null) {
                classSelector.setSelectedItem(className);
            }
        }
    }

    /**
     * Gets all available class names.
     */
    public List<String> getClassNames() {
        return new ArrayList<>(classNames);
    }

    /**
     * Gets the number of classes.
     */
    public int getClassCount() {
        return classNames.size();
    }

    /**
     * Checks if a class exists.
     */
    public boolean hasClass(String className) {
        return classNames.contains(className);
    }

    /**
     * Gets the color for a class (for visual distinction).
     */
    public Color getClassColor(String className) {
        int classIndex = classNames.indexOf(className);
        switch (classIndex % 6) {
            case 0: return Color.RED;
            case 1: return Color.BLUE;
            case 2: return Color.GREEN;
            case 3: return Color.ORANGE;
            case 4: return Color.MAGENTA;
            case 5: return Color.CYAN;
            default: return Color.GRAY;
        }
    }

    /**
     * Clears all classes and resets to defaults.
     */
    public void resetToDefaults() {
        classNames.clear();
        addDefaultClasses();
    }
}