package com.scipath.scipathj.ui.training;

import com.scipath.scipathj.ui.training.wizard.*;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Main wizard panel for XGBoost training workflow.
 * Single-panel design with step-by-step navigation following the wizard pattern.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 */
public class XGBoostTrainingWizardPanel extends JPanel {

    // Wizard dimensions matching UI design
    public static final int WIZARD_WIDTH = 1200;
    public static final int WIZARD_HEIGHT = 900;
    public static final int CONTENT_MARGIN = 20;
    public static final int SECTION_SPACING = 15;

    private final JFrame parentWindow;
    private TrainingWizardState wizardState;
    private XGBoostTrainingWizardManager wizardManager;

    // Main layout components
    private CardLayout cardLayout;
    private JPanel cardContainer;
    private StepProgressIndicator progressIndicator;
    private NavigationControlPanel navigationPanel;

    // Step panels
    private DataLoadingStepPanel dataLoadingStep;
    private DataSplitStepPanel dataSplitStep;
    private ParameterSetupStepPanel parameterSetupStep;
    private TrainingProgressStepPanel trainingProgressStep;
    private EvaluationResultsStepPanel evaluationResultsStep;
    private FinalTestingStepPanel finalTestingStep;

    // Current wizard state
    private WizardStep currentStep = WizardStep.DATA_LOADING;

    /**
     * Wizard step enumeration with display names.
     */
    public enum WizardStep {
        DATA_LOADING("Load Data & Select Features", "📁"),
        SPLIT_CONFIG("Configure Data Split", "📊"),
        PARAMETER_SETUP("Setup Parameters", "⚙️"),
        TRAINING("Training in Progress", "🔄"),
        EVALUATION("Review Results", "📈"),
        FINAL_TESTING("Final Testing", "✅");

        private final String displayName;
        private final String icon;

        WizardStep(String displayName, String icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIcon() {
            return icon;
        }

        public int getStepNumber() {
            return ordinal() + 1;
        }

        public int getTotalSteps() {
            return values().length;
        }
    }

    /**
     * Creates the main wizard panel.
     *
     * @param parentWindow Parent window for dialogs and sizing
     */
    public XGBoostTrainingWizardPanel(JFrame parentWindow) {
        this.parentWindow = parentWindow;
        this.wizardState = new TrainingWizardState();
        this.wizardManager = new XGBoostTrainingWizardManager(this, wizardState);

        initializeComponents();
        setupLayout();
        setupEventHandlers();
        
        // Start with first step
        navigateToStep(WizardStep.DATA_LOADING);
    }

    /**
     * Initialize all wizard components.
     */
    private void initializeComponents() {
        // Set panel properties
        setPreferredSize(new Dimension(WIZARD_WIDTH, WIZARD_HEIGHT));
        setBackground(UIManager.getColor("Panel.background"));
        setBorder(new EmptyBorder(CONTENT_MARGIN, CONTENT_MARGIN, CONTENT_MARGIN, CONTENT_MARGIN));

        // Initialize card layout for step panels
        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setBackground(UIManager.getColor("Panel.background"));

        // Create progress indicator
        progressIndicator = new StepProgressIndicator();

        // Create navigation panel
        navigationPanel = new NavigationControlPanel();

        // Initialize all step panels
        initializeStepPanels();
    }

    /**
     * Initialize all step panels and add them to card container.
     */
    private void initializeStepPanels() {
        // Step 1: Data Loading & Feature Selection
        dataLoadingStep = new DataLoadingStepPanel(wizardState, wizardManager);
        cardContainer.add(dataLoadingStep, WizardStep.DATA_LOADING.name());

        // Step 2: Dataset Split Configuration
        dataSplitStep = new DataSplitStepPanel(wizardState, wizardManager);
        cardContainer.add(dataSplitStep, WizardStep.SPLIT_CONFIG.name());

        // Step 3: Hyperparameter Setup
        parameterSetupStep = new ParameterSetupStepPanel(wizardState, wizardManager);
        cardContainer.add(parameterSetupStep, WizardStep.PARAMETER_SETUP.name());

        // Step 4: Training Progress
        trainingProgressStep = new TrainingProgressStepPanel(wizardState, wizardManager);
        cardContainer.add(trainingProgressStep, WizardStep.TRAINING.name());

        // Step 5: Evaluation Results
        evaluationResultsStep = new EvaluationResultsStepPanel(wizardState, wizardManager);
        cardContainer.add(evaluationResultsStep, WizardStep.EVALUATION.name());

        // Step 6: Final Testing
        finalTestingStep = new FinalTestingStepPanel(wizardState, wizardManager);
        cardContainer.add(finalTestingStep, WizardStep.FINAL_TESTING.name());
    }

    /**
     * Setup the main layout structure.
     */
    private void setupLayout() {
        setLayout(new BorderLayout(0, SECTION_SPACING));

        // Top: Progress indicator
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(UIManager.getColor("Panel.background"));
        topPanel.setBorder(new EmptyBorder(0, 0, SECTION_SPACING, 0));
        topPanel.add(progressIndicator, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // Center: Step panels
        add(cardContainer, BorderLayout.CENTER);

        // Bottom: Navigation controls
        add(navigationPanel, BorderLayout.SOUTH);
    }

    /**
     * Setup event handlers for navigation.
     */
    private void setupEventHandlers() {
        navigationPanel.setPreviousButtonAction(e -> navigatePrevious());
        navigationPanel.setNextButtonAction(e -> navigateNext());
        navigationPanel.setCancelButtonAction(e -> cancelWizard());
    }

    /**
     * Navigate to a specific wizard step.
     *
     * @param step Target step
     */
    public void navigateToStep(WizardStep step) {
        // Save current step state before navigating
        if (currentStep != null) {
            wizardManager.saveCurrentStepState();
        }

        // Update current step
        WizardStep previousStep = currentStep;
        currentStep = step;

        // Show the target step panel
        cardLayout.show(cardContainer, step.name());

        // Update progress indicator
        progressIndicator.setCurrentStep(step);

        // Update navigation buttons
        updateNavigationButtons();

        // Restore step state
        wizardManager.restoreStepState(step);

        // Notify step change
        onStepChanged(previousStep, step);
    }

    /**
     * Navigate to previous step.
     */
    private void navigatePrevious() {
        if (wizardManager.canNavigatePrevious()) {
            WizardStep[] steps = WizardStep.values();
            int currentIndex = currentStep.ordinal();
            if (currentIndex > 0) {
                navigateToStep(steps[currentIndex - 1]);
            }
        }
    }

    /**
     * Navigate to next step.
     */
    private void navigateNext() {
        if (wizardManager.canNavigateNext()) {
            WizardStep[] steps = WizardStep.values();
            int currentIndex = currentStep.ordinal();
            if (currentIndex < steps.length - 1) {
                navigateToStep(steps[currentIndex + 1]);
            } else {
                // Last step - finish wizard
                finishWizard();
            }
        }
    }

    /**
     * Update navigation button states and labels.
     */
    private void updateNavigationButtons() {
        boolean canGoBack = wizardManager.canNavigatePrevious();
        boolean canGoNext = wizardManager.canNavigateNext();
        
        navigationPanel.setPreviousButtonEnabled(canGoBack);
        navigationPanel.setNextButtonEnabled(canGoNext);

        // Update button text based on current step
        String nextText = getNextButtonText();
        navigationPanel.setNextButtonText(nextText);

        // Update step indicator
        navigationPanel.setStepIndicatorText(String.format("Step %d of %d", 
            currentStep.getStepNumber(), currentStep.getTotalSteps()));
    }

    /**
     * Get appropriate next button text for current step.
     */
    private String getNextButtonText() {
        switch (currentStep) {
            case DATA_LOADING:
            case SPLIT_CONFIG:
                return "Next >";
            case PARAMETER_SETUP:
                return "Start Training";
            case TRAINING:
                // Check if training is completed
                if (wizardState.isTrainingCompleted()) {
                    return "Proceed to Results";
                } else {
                    return "Working...";
                }
            case EVALUATION:
                return "Proceed to Testing";
            case FINAL_TESTING:
                return "Finish";
            default:
                return "Next >";
        }
    }

    /**
     * Handle step change events.
     */
    private void onStepChanged(WizardStep previousStep, WizardStep newStep) {
        // Update window title if needed
        if (parentWindow != null) {
            String title = String.format("XGBoost Training - %s", newStep.getDisplayName());
            parentWindow.setTitle(title);
        }

        // Step-specific initialization
        switch (newStep) {
            case TRAINING:
                // Disable navigation during training
                navigationPanel.setNavigationEnabled(false);
                // Start the actual training process
                SwingUtilities.invokeLater(() -> {
                    trainingProgressStep.startTraining();
                });
                break;
            case EVALUATION:
                // Re-enable navigation after training
                navigationPanel.setNavigationEnabled(true);
                // Update evaluation results display
                SwingUtilities.invokeLater(() -> {
                    evaluationResultsStep.updateDisplay();
                });
                break;
        }
    }

    /**
     * Cancel the wizard and close.
     */
    private void cancelWizard() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to cancel the training wizard?\nAll progress will be lost.",
            "Cancel Training",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            // Clean up any resources
            wizardManager.cleanup();
            
            // Close parent window or hide panel
            if (parentWindow != null) {
                parentWindow.dispose();
            }
        }
    }

    /**
     * Finish the wizard successfully.
     */
    private void finishWizard() {
        // Show completion message
        JOptionPane.showMessageDialog(
            this,
            "XGBoost training completed successfully!\nModel has been saved and is ready for use.",
            "Training Complete",
            JOptionPane.INFORMATION_MESSAGE
        );

        // Optional: Keep window open to review results
        // or close automatically based on user preference
        
        // For now, just disable navigation to indicate completion
        navigationPanel.setNavigationEnabled(false);
    }

    // Public getters for wizard state access
    public WizardStep getCurrentStep() {
        return currentStep;
    }

    public TrainingWizardState getWizardState() {
        return wizardState;
    }
    
    public NavigationControlPanel getNavigationPanel() {
        return navigationPanel;
    }

    public XGBoostTrainingWizardManager getWizardManager() {
        return wizardManager;
    }

    public JFrame getParentWindow() {
        return parentWindow;
    }

    /**
     * Force navigation to a specific step (for testing/debugging).
     */
    public void forceNavigateToStep(WizardStep step) {
        navigateToStep(step);
    }

    /**
     * Get reference to a specific step panel.
     */
    public JPanel getStepPanel(WizardStep step) {
        switch (step) {
            case DATA_LOADING: return dataLoadingStep;
            case SPLIT_CONFIG: return dataSplitStep;
            case PARAMETER_SETUP: return parameterSetupStep;
            case TRAINING: return trainingProgressStep;
            case EVALUATION: return evaluationResultsStep;
            case FINAL_TESTING: return finalTestingStep;
            default: return null;
        }
    }
}