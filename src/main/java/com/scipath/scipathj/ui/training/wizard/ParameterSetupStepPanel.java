package com.scipath.scipathj.ui.training.wizard;

import com.scipath.scipathj.training.TrainingSettings;
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

/**
 * Step 3: Parameter Setup Panel.
 * Handles XGBoost hyperparameter configuration with preset options and manual tuning.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 */
public class ParameterSetupStepPanel extends JPanel {

    private static final Color PRIMARY_COLOR = new Color(0, 123, 255);
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
    private static final Color DANGER_COLOR = new Color(220, 53, 69);

    private final TrainingWizardState wizardState;
    private final XGBoostTrainingWizardManager wizardManager;

    // Preset configuration
    private JComboBox<String> presetComboBox;
    private JButton applyPresetButton;
    private JLabel presetDescriptionLabel;

    // Core XGBoost parameters
    private JSpinner learningRateSpinner;
    private JSpinner maxDepthSpinner;
    private JSpinner numTreesSpinner;
    private JSpinner minChildWeightSpinner;

    // Advanced parameters
    private JSpinner subsampleSpinner;
    private JSpinner colsampleBytreeSpinner;
    private JSpinner lambdaSpinner;
    private JSpinner alphaSpinner;
    private JSpinner gammaSpinner;

    // Output configuration
    private JTextField outputDirField;
    private JButton outputBrowseButton;
    private JLabel outputStatusLabel;

    // Validation and summary
    private JTextArea configSummaryArea;
    private JLabel validationStatusLabel;

    /**
     * Creates the parameter setup step panel.
     *
     * @param wizardState The shared wizard state
     * @param wizardManager The wizard manager
     */
    public ParameterSetupStepPanel(TrainingWizardState wizardState, 
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
        // Preset configuration
        presetComboBox = new JComboBox<>(new String[]{"Conservative", "Balanced", "Aggressive"});
        presetComboBox.setSelectedItem("Balanced");
        presetComboBox.setPreferredSize(new Dimension(150, 35));
        
        applyPresetButton = new JButton("Apply Preset");
        applyPresetButton.setIcon(FontIcon.of(FontAwesomeSolid.MAGIC, 14));
        stylePrimaryButton(applyPresetButton);
        
        presetDescriptionLabel = new JLabel(getPresetDescription("Balanced"));
        presetDescriptionLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        presetDescriptionLabel.setForeground(Color.GRAY);

        // Core parameters with tooltips
        learningRateSpinner = createParameterSpinner(0.01, 0.5, 0.1, 0.01, 
            "Controls the step size at each boosting step. Lower values are safer but slower.");
        maxDepthSpinner = createParameterSpinner(1, 15, 6, 1, 
            "Maximum depth of a tree. Higher values may lead to overfitting.");
        numTreesSpinner = createParameterSpinner(10, 1000, 100, 10, 
            "Number of boosting rounds. More trees can improve performance but may overfit.");
        minChildWeightSpinner = createParameterSpinner(1, 10, 1, 1, 
            "Minimum sum of instance weight needed in a child. Higher values prevent overfitting.");

        // Advanced parameters
        subsampleSpinner = createParameterSpinner(0.1, 1.0, 1.0, 0.1, 
            "Subsample ratio of the training instances. Lower values prevent overfitting.");
        colsampleBytreeSpinner = createParameterSpinner(0.1, 1.0, 1.0, 0.1, 
            "Subsample ratio of columns when constructing each tree.");
        lambdaSpinner = createParameterSpinner(0.0, 10.0, 1.0, 0.1, 
            "L2 regularization term on weights. Higher values reduce overfitting.");
        alphaSpinner = createParameterSpinner(0.0, 10.0, 0.0, 0.1, 
            "L1 regularization term on weights. Higher values reduce overfitting.");
        gammaSpinner = createParameterSpinner(0.0, 10.0, 0.0, 0.1, 
            "Minimum loss reduction required to make a further partition.");

        // Output configuration
        outputDirField = new JTextField();
        outputDirField.setEditable(false);
        outputDirField.setPreferredSize(new Dimension(400, 35));
        
        // Set default output directory in both UI and wizard state
        File defaultOutputDir = new File(System.getProperty("user.home") + File.separator + "XGBoost_Models");
        outputDirField.setText(defaultOutputDir.getAbsolutePath());
        wizardState.setOutputDirectory(defaultOutputDir);
        
        outputBrowseButton = new JButton("Browse...");
        outputBrowseButton.setIcon(FontIcon.of(FontAwesomeSolid.FOLDER_OPEN, 16));
        styleSecondaryButton(outputBrowseButton);
        
        outputStatusLabel = new JLabel("✅ Valid output directory");
        outputStatusLabel.setForeground(SUCCESS_COLOR);

        // Configuration summary
        configSummaryArea = new JTextArea(6, 40);
        configSummaryArea.setEditable(false);
        configSummaryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        configSummaryArea.setBackground(new Color(248, 249, 250));
        configSummaryArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        validationStatusLabel = new JLabel("✅ Configuration is ready for training");
        validationStatusLabel.setForeground(SUCCESS_COLOR);
        validationStatusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
    }

    /**
     * Create a parameter spinner with tooltip.
     */
    private JSpinner createParameterSpinner(double min, double max, double initial, double step, String tooltip) {
        SpinnerNumberModel model = new SpinnerNumberModel(initial, min, max, step);
        JSpinner spinner = new JSpinner(model);
        spinner.setPreferredSize(new Dimension(100, 30));
        spinner.setToolTipText(tooltip);
        
        // Add change listener to update summary
        spinner.addChangeListener(e -> updateConfigurationSummary());
        
        return spinner;
    }

    /**
     * Setup the layout structure.
     */
    private void setupLayout() {
        setLayout(new BorderLayout(0, 20));
        setOpaque(false);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        // Create tabbed pane for organized parameter sections
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        // Quick Setup tab
        JPanel quickSetupTab = createQuickSetupTab();
        tabbedPane.addTab("Quick Setup", FontIcon.of(FontAwesomeSolid.BOLT, 16), quickSetupTab, "Preset configurations for quick start");

        // Advanced Parameters tab
        JPanel advancedTab = createAdvancedParametersTab();
        tabbedPane.addTab("Advanced", FontIcon.of(FontAwesomeSolid.COG, 16), advancedTab, "Fine-tune individual parameters");

        // Output & Summary tab
        JPanel outputTab = createOutputSummaryTab();
        tabbedPane.addTab("Output & Summary", FontIcon.of(FontAwesomeSolid.SAVE, 16), outputTab, "Configure output and review settings");

        add(tabbedPane, BorderLayout.CENTER);

        // Validation status at bottom
        JPanel validationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        validationPanel.setOpaque(false);
        validationPanel.add(validationStatusLabel);
        add(validationPanel, BorderLayout.SOUTH);
    }

    /**
     * Create the quick setup tab.
     */
    private JPanel createQuickSetupTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 20));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Preset selection section
        JPanel presetSection = new JPanel(new BorderLayout(0, 15));
        presetSection.setOpaque(false);
        presetSection.setBorder(createTitledBorder("Configuration Presets"));

        JPanel presetControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        presetControls.setOpaque(false);
        presetControls.add(new JLabel("Preset:"));
        presetControls.add(presetComboBox);
        presetControls.add(applyPresetButton);

        presetSection.add(presetControls, BorderLayout.NORTH);
        presetSection.add(presetDescriptionLabel, BorderLayout.CENTER);

        // Core parameters section
        JPanel coreSection = createCoreParametersSection();

        tab.add(presetSection, BorderLayout.NORTH);
        tab.add(coreSection, BorderLayout.CENTER);

        return tab;
    }

    /**
     * Create core parameters section.
     */
    private JPanel createCoreParametersSection() {
        JPanel section = new JPanel(new GridLayout(2, 2, 20, 15));
        section.setOpaque(false);
        section.setBorder(createTitledBorder("Core Parameters"));

        // Learning rate
        section.add(createParameterPanel("Learning Rate:", learningRateSpinner));
        
        // Max depth
        section.add(createParameterPanel("Max Depth:", maxDepthSpinner));
        
        // Number of trees
        section.add(createParameterPanel("Number of Trees:", numTreesSpinner));
        
        // Min child weight
        section.add(createParameterPanel("Min Child Weight:", minChildWeightSpinner));

        return section;
    }

    /**
     * Create the advanced parameters tab.
     */
    private JPanel createAdvancedParametersTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 20));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Sampling parameters
        JPanel samplingSection = new JPanel(new GridLayout(1, 2, 20, 15));
        samplingSection.setOpaque(false);
        samplingSection.setBorder(createTitledBorder("Sampling Parameters"));
        samplingSection.add(createParameterPanel("Subsample:", subsampleSpinner));
        samplingSection.add(createParameterPanel("Column Sample by Tree:", colsampleBytreeSpinner));

        // Regularization parameters
        JPanel regularizationSection = new JPanel(new GridLayout(1, 3, 20, 15));
        regularizationSection.setOpaque(false);
        regularizationSection.setBorder(createTitledBorder("Regularization Parameters"));
        regularizationSection.add(createParameterPanel("Lambda (L2):", lambdaSpinner));
        regularizationSection.add(createParameterPanel("Alpha (L1):", alphaSpinner));
        regularizationSection.add(createParameterPanel("Gamma:", gammaSpinner));

        // Tips section
        JPanel tipsSection = createTipsSection();

        tab.add(samplingSection, BorderLayout.NORTH);
        tab.add(regularizationSection, BorderLayout.CENTER);
        tab.add(tipsSection, BorderLayout.SOUTH);

        return tab;
    }

    /**
     * Create tips section for advanced parameters.
     */
    private JPanel createTipsSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);
        section.setBorder(createTitledBorder("Parameter Tuning Tips"));

        String tips = "• Increase lambda/alpha to reduce overfitting\n" +
                     "• Lower subsample to improve generalization\n" +
                     "• Higher gamma makes the algorithm more conservative\n" +
                     "• Start with conservative settings and gradually adjust\n" +
                     "• Monitor training vs validation performance";

        JTextArea tipsArea = new JTextArea(tips);
        tipsArea.setEditable(false);
        tipsArea.setOpaque(false);
        tipsArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        tipsArea.setForeground(new Color(73, 80, 87));

        section.add(tipsArea, BorderLayout.CENTER);
        return section;
    }

    /**
     * Create the output and summary tab.
     */
    private JPanel createOutputSummaryTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 20));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Output directory section
        JPanel outputSection = new JPanel(new BorderLayout(10, 10));
        outputSection.setOpaque(false);
        outputSection.setBorder(createTitledBorder("Output Directory"));

        JPanel outputControls = new JPanel(new BorderLayout(10, 0));
        outputControls.setOpaque(false);
        outputControls.add(outputDirField, BorderLayout.CENTER);
        outputControls.add(outputBrowseButton, BorderLayout.EAST);

        outputSection.add(outputControls, BorderLayout.CENTER);
        outputSection.add(outputStatusLabel, BorderLayout.SOUTH);

        // Configuration summary section
        JPanel summarySection = new JPanel(new BorderLayout());
        summarySection.setOpaque(false);
        summarySection.setBorder(createTitledBorder("Configuration Summary"));

        JScrollPane summaryScroll = new JScrollPane(configSummaryArea);
        summaryScroll.setPreferredSize(new Dimension(500, 150));

        summarySection.add(summaryScroll, BorderLayout.CENTER);

        tab.add(outputSection, BorderLayout.NORTH);
        tab.add(summarySection, BorderLayout.CENTER);

        return tab;
    }

    /**
     * Create a parameter panel with label and spinner.
     */
    private JPanel createParameterPanel(String labelText, JSpinner spinner) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        panel.add(label, BorderLayout.NORTH);
        panel.add(spinner, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Setup event handlers.
     */
    private void setupEventHandlers() {
        // Preset selection
        presetComboBox.addActionListener(e -> updatePresetDescription());
        applyPresetButton.addActionListener(e -> applySelectedPreset());

        // Output directory
        outputBrowseButton.addActionListener(e -> browseForOutputDirectory());

        // Update summary when any parameter changes
        updateConfigurationSummary();
    }

    /**
     * Update preset description based on selection.
     */
    private void updatePresetDescription() {
        String selected = (String) presetComboBox.getSelectedItem();
        presetDescriptionLabel.setText(getPresetDescription(selected));
    }

    /**
     * Get description for a preset configuration.
     */
    private String getPresetDescription(String preset) {
        switch (preset.toLowerCase()) {
            case "conservative":
                return "Slower learning, deeper trees, strong regularization. Safest for avoiding overfitting.";
            case "balanced":
                return "Balanced approach with moderate learning rate and regularization. Good starting point.";
            case "aggressive":
                return "Faster learning, more trees, lighter regularization. Higher performance but risk of overfitting.";
            default:
                return "Select a preset configuration to see description.";
        }
    }

    /**
     * Apply the selected preset configuration.
     */
    private void applySelectedPreset() {
        String selected = (String) presetComboBox.getSelectedItem();
        wizardManager.applyParameterPreset(selected);
        
        // Update spinners to reflect new values
        updateSpinnersFromSettings();
        updateConfigurationSummary();
        
        JOptionPane.showMessageDialog(this, 
            "Applied " + selected + " preset configuration.\nYou can further customize individual parameters if needed.",
            "Preset Applied", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Update spinners from current training settings.
     */
    private void updateSpinnersFromSettings() {
        TrainingSettings settings = wizardState.getTrainingSettings();
        
        learningRateSpinner.setValue((double) settings.getLearningRate());
        maxDepthSpinner.setValue(settings.getMaxDepth());
        numTreesSpinner.setValue(settings.getNumTrees());
        minChildWeightSpinner.setValue(settings.getMinChildWeight());
        subsampleSpinner.setValue((double) settings.getSubsample());
        colsampleBytreeSpinner.setValue((double) settings.getColsampleBytree());
        lambdaSpinner.setValue((double) settings.getLambda());
        alphaSpinner.setValue((double) settings.getAlpha());
        gammaSpinner.setValue((double) settings.getGamma());
    }

    /**
     * Browse for output directory.
     */
    private void browseForOutputDirectory() {
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirChooser.setCurrentDirectory(new File(outputDirField.getText()));
        
        int result = dirChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = dirChooser.getSelectedFile();
            outputDirField.setText(selectedDir.getAbsolutePath());
            wizardState.setOutputDirectory(selectedDir);
            updateOutputStatus();
        }
    }

    /**
     * Update output directory status.
     */
    private void updateOutputStatus() {
        File outputDir = new File(outputDirField.getText());
        
        if (!outputDir.exists()) {
            outputStatusLabel.setText("⚠️ Directory will be created");
            outputStatusLabel.setForeground(WARNING_COLOR);
        } else if (!outputDir.canWrite()) {
            outputStatusLabel.setText("❌ Directory is not writable");
            outputStatusLabel.setForeground(DANGER_COLOR);
        } else {
            outputStatusLabel.setText("✅ Valid output directory");
            outputStatusLabel.setForeground(SUCCESS_COLOR);
        }
    }

    /**
     * Update configuration summary.
     */
    private void updateConfigurationSummary() {
        // Update training settings from spinners
        TrainingSettings settings = wizardState.getTrainingSettings();
        settings.setLearningRate(((Number) learningRateSpinner.getValue()).floatValue());
        settings.setMaxDepth(((Number) maxDepthSpinner.getValue()).intValue());
        settings.setNumTrees(((Number) numTreesSpinner.getValue()).intValue());
        settings.setMinChildWeight(((Number) minChildWeightSpinner.getValue()).intValue());
        settings.setSubsample(((Number) subsampleSpinner.getValue()).floatValue());
        settings.setColsampleBytree(((Number) colsampleBytreeSpinner.getValue()).floatValue());
        settings.setLambda(((Number) lambdaSpinner.getValue()).floatValue());
        settings.setAlpha(((Number) alphaSpinner.getValue()).floatValue());
        settings.setGamma(((Number) gammaSpinner.getValue()).floatValue());
        
        // Auto-detect and configure multi-class settings
        int numClasses = wizardState.getClassDistribution().size();
        settings.setNumClasses(numClasses);  // This will auto-set objective and evalMetric

        // Generate summary text
        StringBuilder summary = new StringBuilder();
        summary.append("XGBoost Training Configuration:\n");
        summary.append("=====================================\n\n");
        
        // Multi-class configuration (most important for methodological correctness)
        summary.append("Multi-Class Configuration:\n");
        summary.append(String.format("  Number of Classes: %d\n", numClasses));
        summary.append(String.format("  Objective: %s\n", settings.getObjective()));
        summary.append(String.format("  Evaluation Metric: %s\n", settings.getEvalMetric()));
        if (numClasses > 2) {
            summary.append("  → Confidence scores for each class\n");
            summary.append("  → Probability-based predictions\n");
        }
        
        summary.append("\nCore Parameters:\n");
        summary.append(String.format("  Learning Rate: %.3f\n", settings.getLearningRate()));
        summary.append(String.format("  Max Depth: %d\n", settings.getMaxDepth()));
        summary.append(String.format("  Number of Trees: %d\n", settings.getNumTrees()));
        summary.append(String.format("  Min Child Weight: %d\n", settings.getMinChildWeight()));
        summary.append("\nAdvanced Parameters:\n");
        summary.append(String.format("  Subsample: %.3f\n", settings.getSubsample()));
        summary.append(String.format("  Column Sample by Tree: %.3f\n", settings.getColsampleBytree()));
        summary.append(String.format("  Lambda (L2): %.3f\n", settings.getLambda()));
        summary.append(String.format("  Alpha (L1): %.3f\n", settings.getAlpha()));
        summary.append(String.format("  Gamma: %.3f\n", settings.getGamma()));
        summary.append("\nData Configuration:\n");
        summary.append(String.format("  Selected Features: %d\n", wizardState.getSelectedFeatures().size()));
        summary.append(String.format("  Total Samples: %d\n", wizardState.getTotalSamples()));
        summary.append(String.format("  Train/Eval/Test Split: %.0f%%/%.0f%%/%.0f%%\n",
            wizardState.getTrainRatio() * 100,
            wizardState.getEvalRatio() * 100,
            wizardState.getTestRatio() * 100));

        configSummaryArea.setText(summary.toString());
        updateValidationStatus();
    }

    /**
     * Update validation status.
     */
    private void updateValidationStatus() {
        File outputDir = new File(outputDirField.getText());
        
        // Debug logging
        System.out.println("DEBUG - ParameterSetupStepPanel.updateValidationStatus():");
        System.out.println("  Selected features count: " + wizardState.getSelectedFeatures().size());
        System.out.println("  Available features count: " + wizardState.getAvailableFeatures().size());
        if (!wizardState.getSelectedFeatures().isEmpty()) {
            System.out.println("  First few selected features: " +
                wizardState.getSelectedFeatures().stream().limit(5).toList());
        }
        
        if (!outputDir.exists() && !outputDir.getParentFile().canWrite()) {
            validationStatusLabel.setText("❌ Cannot create output directory");
            validationStatusLabel.setForeground(DANGER_COLOR);
        } else if (wizardState.getSelectedFeatures().isEmpty()) {
            validationStatusLabel.setText("❌ No features selected");
            validationStatusLabel.setForeground(DANGER_COLOR);
        } else {
            validationStatusLabel.setText("✅ Configuration is ready for training");
            validationStatusLabel.setForeground(SUCCESS_COLOR);
        }
    }

    /**
     * Update the entire display.
     */
    public void updateDisplay() {
        // Update output directory
        if (wizardState.getOutputDirectory() != null) {
            outputDirField.setText(wizardState.getOutputDirectory().getAbsolutePath());
        }
        
        // Update spinners
        updateSpinnersFromSettings();
        
        // Update summary and validation
        updateConfigurationSummary();
        updateOutputStatus();
    }

    // Utility methods for styling

    private void stylePrimaryButton(JButton button) {
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 35));
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
        button.setPreferredSize(new Dimension(100, 35));
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