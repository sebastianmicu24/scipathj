package com.scipath.scipathj.ui.training.wizard;

import com.scipath.scipathj.ui.utils.UIUtils;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Step 1: Data Loading & Feature Selection Panel.
 * Handles JSON file selection and comprehensive feature selection interface.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 */
public class DataLoadingStepPanel extends JPanel {

    private static final Color PRIMARY_COLOR = new Color(0, 123, 255);
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
    private static final Color DANGER_COLOR = new Color(220, 53, 69);

    private final TrainingWizardState wizardState;
    private final XGBoostTrainingWizardManager wizardManager;

    // File selection components
    private JTextField jsonFileField;
    private JButton browseButton;
    private JLabel fileStatusLabel;

    // Feature selection components
    private JPanel featureCategoriesPanel;
    private Map<String, JPanel> categoryPanels = new HashMap<>();
    private Map<String, JCheckBox> featureCheckboxes = new HashMap<>();
    private JLabel selectedFeaturesLabel;
    private JButton selectAllButton;
    private JButton selectNoneButton;
    private JButton smartRecommendationsButton;

    // Data validation components
    private JPanel validationPanel;
    private JLabel samplesLabel;
    private JLabel classesLabel;
    private JLabel validationStatusLabel;

    /**
     * Creates the data loading step panel.
     *
     * @param wizardState The shared wizard state
     * @param wizardManager The wizard manager
     */
    public DataLoadingStepPanel(TrainingWizardState wizardState, 
                               XGBoostTrainingWizardManager wizardManager) {
        this.wizardState = wizardState;
        this.wizardManager = wizardManager;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        updateDisplay();
    }

    /**
     * Initialize all components.
     */
    private void initializeComponents() {
        // File selection components
        jsonFileField = new JTextField();
        jsonFileField.setEditable(false);
        jsonFileField.setPreferredSize(new Dimension(400, 35));
        jsonFileField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        
        browseButton = new JButton("Browse...");
        browseButton.setIcon(FontIcon.of(FontAwesomeSolid.FOLDER_OPEN, 16));
        browseButton.setPreferredSize(new Dimension(120, 35));
        styleSecondaryButton(browseButton);
        
        fileStatusLabel = new JLabel("No file selected");
        fileStatusLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        fileStatusLabel.setForeground(Color.GRAY);

        // Feature selection components
        featureCategoriesPanel = new JPanel();
        featureCategoriesPanel.setLayout(new GridLayout(1, 3, 15, 0)); // 1 row, 3 columns, 15px horizontal gap
        featureCategoriesPanel.setOpaque(false);

        selectedFeaturesLabel = new JLabel("Selected: 0 features");
        selectedFeaturesLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        selectedFeaturesLabel.setForeground(PRIMARY_COLOR);

        selectAllButton = new JButton("Select All");
        selectAllButton.setIcon(FontIcon.of(FontAwesomeSolid.CHECK_SQUARE, 14));
        styleSecondaryButton(selectAllButton);

        selectNoneButton = new JButton("Select None");
        selectNoneButton.setIcon(FontIcon.of(FontAwesomeSolid.SQUARE, 14));
        styleSecondaryButton(selectNoneButton);

        smartRecommendationsButton = new JButton("Smart Recommendations");
        smartRecommendationsButton.setIcon(FontIcon.of(FontAwesomeSolid.MAGIC, 14));
        stylePrimaryButton(smartRecommendationsButton);

        // Data validation components
        samplesLabel = new JLabel("Samples: Not loaded");
        classesLabel = new JLabel("Classes: Not loaded");
        validationStatusLabel = new JLabel("⚠️ Please select a training data file");
        validationStatusLabel.setForeground(WARNING_COLOR);

        validationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        validationPanel.setOpaque(false);
        validationPanel.add(samplesLabel);
        validationPanel.add(classesLabel);
    }

    /**
     * Setup the layout structure.
     */
    private void setupLayout() {
        setLayout(new BorderLayout(0, 20));
        setOpaque(false);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout(0, 25));
        mainPanel.setOpaque(false);

        // File selection section
        JPanel fileSection = createFileSelectionSection();
        
        // Feature selection section
        JPanel featureSection = createFeatureSelectionSection();
        
        // Validation section
        JPanel validationSection = createValidationSection();

        mainPanel.add(fileSection, BorderLayout.NORTH);
        mainPanel.add(featureSection, BorderLayout.CENTER);
        mainPanel.add(validationSection, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Create the file selection section.
     */
    private JPanel createFileSelectionSection() {
        JPanel section = new JPanel(new BorderLayout(10, 10));
        section.setOpaque(false);
        // Modern section title (no border)
        JLabel sectionTitle = new JLabel("Training Data File");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        sectionTitle.setForeground(UIManager.getColor("Label.foreground"));
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(sectionTitle);
        section.add(Box.createVerticalStrut(15));

        // File input row
        JPanel fileInputPanel = new JPanel(new BorderLayout(10, 0));
        fileInputPanel.setOpaque(false);
        fileInputPanel.add(jsonFileField, BorderLayout.CENTER);
        fileInputPanel.add(browseButton, BorderLayout.EAST);

        // Status row
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        statusPanel.setOpaque(false);
        statusPanel.add(fileStatusLabel);

        section.add(fileInputPanel, BorderLayout.CENTER);
        section.add(statusPanel, BorderLayout.SOUTH);

        return section;
    }

    /**
     * Create the feature selection section.
     */
    private JPanel createFeatureSelectionSection() {
        JPanel section = new JPanel(new BorderLayout(0, 15));
        section.setOpaque(false);
        // Modern section title (no border)
        JLabel sectionTitle = new JLabel("Feature Selection");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        sectionTitle.setForeground(UIManager.getColor("Label.foreground"));
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(sectionTitle);
        section.add(Box.createVerticalStrut(15));

        // Feature categories grid
        JScrollPane categoriesScroll = new JScrollPane(featureCategoriesPanel);
        categoriesScroll.setPreferredSize(new Dimension(800, 400)); // Increased height for better visibility
        categoriesScroll.setBorder(BorderFactory.createEmptyBorder());
        categoriesScroll.setOpaque(false);
        categoriesScroll.getViewport().setOpaque(false);

        // Control buttons panel
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controlsPanel.setOpaque(false);
        controlsPanel.add(selectAllButton);
        controlsPanel.add(selectNoneButton);
        controlsPanel.add(smartRecommendationsButton);
        controlsPanel.add(Box.createHorizontalStrut(20));
        controlsPanel.add(selectedFeaturesLabel);

        section.add(categoriesScroll, BorderLayout.CENTER);
        section.add(controlsPanel, BorderLayout.SOUTH);

        return section;
    }

    /**
     * Create the validation section.
     */
    private JPanel createValidationSection() {
        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setOpaque(false);
        // Modern section title (no border)
        JLabel sectionTitle = new JLabel("Data Validation");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        sectionTitle.setForeground(UIManager.getColor("Label.foreground"));
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(sectionTitle);
        section.add(Box.createVerticalStrut(15));

        section.add(validationPanel, BorderLayout.CENTER);
        section.add(validationStatusLabel, BorderLayout.SOUTH);

        return section;
    }

    /**
     * Setup event handlers.
     */
    private void setupEventHandlers() {
        browseButton.addActionListener(e -> browseForFile());
        selectAllButton.addActionListener(e -> selectAllFeatures());
        selectNoneButton.addActionListener(e -> selectNoFeatures());
        smartRecommendationsButton.addActionListener(e -> applySmartRecommendations());
    }

    /**
     * Browse for JSON training data file.
     */
    private void browseForFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadTrainingDataFile(selectedFile);
        }
    }

    /**
     * Load and analyze the selected training data file.
     */
    private void loadTrainingDataFile(File file) {
        jsonFileField.setText(file.getAbsolutePath());
        fileStatusLabel.setText("Loading...");
        fileStatusLabel.setForeground(PRIMARY_COLOR);

        // Use wizard manager to load data
        SwingUtilities.invokeLater(() -> {
            boolean success = wizardManager.loadTrainingData(file);
            
            if (success) {
                fileStatusLabel.setText("✅ " + wizardState.getTotalSamples() + " samples detected, " 
                    + wizardState.getAvailableFeatures().size() + " features available");
                fileStatusLabel.setForeground(SUCCESS_COLOR);
                
                updateFeatureCategories();
                updateValidationDisplay();
                
            } else {
                fileStatusLabel.setText("❌ Failed to load file");
                fileStatusLabel.setForeground(DANGER_COLOR);
                
                clearFeatureSelection();
                updateValidationDisplay();
            }
        });
    }

    /**
     * Update feature categories display.
     */
    private void updateFeatureCategories() {
        featureCategoriesPanel.removeAll();
        categoryPanels.clear();
        featureCheckboxes.clear();

        Map<String, List<String>> featuresByCategory = wizardState.getFeaturesByCategory();
        
        for (Map.Entry<String, List<String>> entry : featuresByCategory.entrySet()) {
            String category = entry.getKey();
            List<String> features = entry.getValue();
            
            JPanel categoryPanel = createFeatureCategoryPanel(category, features);
            categoryPanels.put(category, categoryPanel);
            featureCategoriesPanel.add(categoryPanel);
        }
        
        revalidate();
        repaint();
        updateSelectedFeaturesCount();
    }

    /**
     * Create a panel for a feature category.
     */
    private JPanel createFeatureCategoryPanel(String category, List<String> features) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        // Modern category title (no border)
        JLabel categoryTitle = new JLabel(category + " (" + features.size() + ")");
        categoryTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        categoryTitle.setForeground(UIManager.getColor("Label.foreground"));
        categoryTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(categoryTitle);
        panel.add(Box.createVerticalStrut(10));

        JPanel featuresPanel = new JPanel(new GridLayout(0, 1, 2, 2));
        featuresPanel.setOpaque(false);

        for (String feature : features) {
            JCheckBox checkbox = new JCheckBox(feature);
            checkbox.setOpaque(false);
            checkbox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            
            // Check if feature is selected
            boolean selected = wizardState.getSelectedFeatures().contains(feature);
            checkbox.setSelected(selected);
            
            checkbox.addActionListener(e -> {
                if (checkbox.isSelected()) {
                    wizardState.getSelectedFeatures().add(feature);
                } else {
                    wizardState.getSelectedFeatures().remove(feature);
                }
                updateSelectedFeaturesCount();
            });
            
            featureCheckboxes.put(feature, checkbox);
            featuresPanel.add(checkbox);
        }

        JScrollPane scrollPane = new JScrollPane(featuresPanel);
        scrollPane.setPreferredSize(new Dimension(250, 150));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Select all available features.
     */
    private void selectAllFeatures() {
        wizardState.getSelectedFeatures().clear();
        wizardState.getSelectedFeatures().addAll(wizardState.getAvailableFeatures());
        
        for (JCheckBox checkbox : featureCheckboxes.values()) {
            checkbox.setSelected(true);
        }
        
        updateSelectedFeaturesCount();
    }

    /**
     * Deselect all features.
     */
    private void selectNoFeatures() {
        wizardState.getSelectedFeatures().clear();
        
        for (JCheckBox checkbox : featureCheckboxes.values()) {
            checkbox.setSelected(false);
        }
        
        updateSelectedFeaturesCount();
    }

    /**
     * Apply smart feature recommendations.
     */
    private void applySmartRecommendations() {
        wizardManager.applySmartFeatureRecommendations();
        
        // Update checkboxes to reflect selections
        for (Map.Entry<String, JCheckBox> entry : featureCheckboxes.entrySet()) {
            String feature = entry.getKey();
            JCheckBox checkbox = entry.getValue();
            
            boolean selected = wizardState.getSelectedFeatures().contains(feature);
            checkbox.setSelected(selected);
        }
        
        updateSelectedFeaturesCount();
        
        // Show info message
        JOptionPane.showMessageDialog(this, 
            "Applied smart recommendations based on histological analysis best practices.\n" +
            wizardState.getSelectedFeatures().size() + " features selected.",
            "Smart Recommendations", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Update selected features count display.
     */
    private void updateSelectedFeaturesCount() {
        int count = wizardState.getSelectedFeatures().size();
        selectedFeaturesLabel.setText("Selected: " + count + " features");
        
        // Debug logging
        System.out.println("DEBUG - DataLoadingStepPanel.updateSelectedFeaturesCount():");
        System.out.println("  Selected features count: " + count);
        if (count > 0 && count <= 5) {
            System.out.println("  Selected features: " + wizardState.getSelectedFeatures());
        } else if (count > 5) {
            System.out.println("  First 5 selected features: " +
                wizardState.getSelectedFeatures().stream().limit(5).toList());
        }
        
        if (count == 0) {
            selectedFeaturesLabel.setForeground(DANGER_COLOR);
        } else if (count < 10) {
            selectedFeaturesLabel.setForeground(WARNING_COLOR);
        } else {
            selectedFeaturesLabel.setForeground(SUCCESS_COLOR);
        }
        
        // Update validation status when feature selection changes
        updateValidationDisplay();
        
        // Notify wizard manager to refresh navigation button states
        wizardManager.refreshNavigationButtons();
    }

    /**
     * Clear feature selection when no file is loaded.
     */
    private void clearFeatureSelection() {
        featureCategoriesPanel.removeAll();
        categoryPanels.clear();
        featureCheckboxes.clear();
        wizardState.getSelectedFeatures().clear();
        updateSelectedFeaturesCount();
        revalidate();
        repaint();
    }

    /**
     * Update validation display.
     */
    private void updateValidationDisplay() {
        if (wizardState.getJsonFile() != null) {
            samplesLabel.setText("Samples: " + wizardState.getTotalSamples());
            int numClasses = wizardState.getClassDistribution().size();
            classesLabel.setText("Classes: " + numClasses);
            
            // Check for single-class dataset - methodological error
            if (numClasses < 2) {
                validationStatusLabel.setText("❌ Dataset has only " + numClasses + " class. XGBoost requires ≥2 classes for classification");
                validationStatusLabel.setForeground(DANGER_COLOR);
                
                // Show warning dialog
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                        "⚠️ METHODOLOGICAL WARNING ⚠️\n\n" +
                        "This dataset contains only " + numClasses + " class(es).\n" +
                        "XGBoost classification requires at least 2 classes to:\n" +
                        "• Calculate meaningful precision/recall metrics\n" +
                        "• Train a discriminative model\n" +
                        "• Generate confidence scores\n\n" +
                        "Please use a dataset with multiple classes or consider:\n" +
                        "• Anomaly detection techniques\n" +
                        "• Unsupervised clustering methods",
                        "Invalid Dataset for Classification",
                        JOptionPane.WARNING_MESSAGE);
                });
            } else if (wizardState.getSelectedFeatures().isEmpty()) {
                validationStatusLabel.setText("⚠️ Please select at least one feature");
                validationStatusLabel.setForeground(WARNING_COLOR);
            } else {
                validationStatusLabel.setText("✅ Ready to proceed (" + numClasses + " classes detected)");
                validationStatusLabel.setForeground(SUCCESS_COLOR);
            }
        } else {
            samplesLabel.setText("Samples: Not loaded");
            classesLabel.setText("Classes: Not loaded");
            validationStatusLabel.setText("⚠️ Please select a training data file");
            validationStatusLabel.setForeground(WARNING_COLOR);
        }
    }

    /**
     * Update the entire display.
     */
    public void updateDisplay() {
        // Update file field
        if (wizardState.getJsonFile() != null) {
            jsonFileField.setText(wizardState.getJsonFile().getAbsolutePath());
            updateFeatureCategories();
        }
        
        updateValidationDisplay();
    }

    // Utility methods for styling

    private void stylePrimaryButton(JButton button) {
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void styleSecondaryButton(JButton button) {
        button.setBackground(UIManager.getColor("Button.background"));
        button.setForeground(new Color(108, 117, 125));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(108, 117, 125), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private TitledBorder createTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(223, 225, 229), 1),
            title
        );
        border.setTitleFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        border.setTitleColor(new Color(73, 80, 87));
        return border;
    }
}