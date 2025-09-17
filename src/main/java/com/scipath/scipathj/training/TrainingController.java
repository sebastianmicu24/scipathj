package com.scipath.scipathj.training;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.*;
import java.io.File;

/**
 * Controller for XGBoost model training workflow in SciPathJ.
 * Manages the interaction between UI and training components.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class TrainingController {

    private static final Logger logger = LoggerFactory.getLogger(TrainingController.class);

    private final JFrame parentWindow;
    private TrainingSettings settings;
    private XGBoostTrainer trainer;
    private SwingWorker<Void, String> trainingWorker;

    /**
     * Constructs a new training controller.
     *
     * @param parentWindow the parent window for dialogs
     */
    public TrainingController(JFrame parentWindow) {
        this.parentWindow = parentWindow;
        this.settings = new TrainingSettings();
    }

    /**
     * Shows the training dialog.
     *
     * @param jsonFile optional JSON file to pre-select
     * @param outputDir optional output directory to pre-select
     */
    public void showTrainingDialog(File jsonFile, File outputDir) {
        TrainingDialog dialog = new TrainingDialog(parentWindow, settings, jsonFile, outputDir);
        dialog.setVisible(true);

        if (dialog.isApproved()) {
            TrainingSettings dialogSettings = dialog.getSettings();
            File selectedJsonFile = dialog.getJsonFile();
            File selectedOutputDir = dialog.getOutputDir();

            if (validateInputs(dialogSettings, selectedJsonFile, selectedOutputDir)) {
                this.settings = dialogSettings;
                startTraining(selectedJsonFile, selectedOutputDir);
            }
        }
    }

    /**
     * Starts training directly with provided settings (for integrated panels).
     *
     * @param settings training settings to use
     * @param jsonFile JSON training data file
     * @param outputDir output directory for results
     * @return true if training started successfully, false otherwise
     */
    public boolean startTrainingWithSettings(TrainingSettings settings, File jsonFile, File outputDir) {
        if (validateInputs(settings, jsonFile, outputDir)) {
            this.settings = settings;
            startTraining(jsonFile, outputDir);
            return true;
        }
        return false;
    }

    /**
     * Validates user inputs before training.
     *
     * @param settings training settings
     * @param jsonFile JSON data file
     * @param outputDir output directory
     * @return true if inputs are valid
     */
    private boolean validateInputs(TrainingSettings settings, File jsonFile, File outputDir) {
        // Validate settings
        java.util.List<String> validationErrors = settings.validate();
        if (!validationErrors.isEmpty()) {
            String errorMessage = "Invalid training settings:\n" +
                String.join("\n", validationErrors);
            JOptionPane.showMessageDialog(parentWindow, errorMessage,
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Validate JSON file
        if (jsonFile == null || !jsonFile.exists()) {
            JOptionPane.showMessageDialog(parentWindow,
                "Please select a valid JSON training data file.",
                "Invalid JSON File", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!jsonFile.getName().toLowerCase().endsWith(".json")) {
            JOptionPane.showMessageDialog(parentWindow,
                "Training data file must be a JSON file (.json extension).",
                "Invalid File Type", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Validate output directory
        if (outputDir == null) {
            JOptionPane.showMessageDialog(parentWindow,
                "Please select an output directory for model files.",
                "Invalid Output Directory", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Check if output directory exists or can be created
        if (!outputDir.exists()) {
            int result = JOptionPane.showConfirmDialog(parentWindow,
                "Output directory does not exist. Create it?\n" + outputDir.getAbsolutePath(),
                "Create Output Directory", JOptionPane.YES_NO_OPTION);

            if (result != JOptionPane.YES_OPTION) {
                return false;
            }
        }

        return true;
    }

    /**
     * Starts the training process in a background thread.
     *
     * @param jsonFile JSON training data file
     * @param outputDir output directory for results
     */
    private void startTraining(File jsonFile, File outputDir) {
        String outputDirPath = outputDir.getAbsolutePath();

        // Show progress dialog
        JDialog progressDialog = new JDialog(parentWindow, "Training XGBoost Model", true);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setString("Initializing training...");
        progressBar.setStringPainted(true);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            if (trainingWorker != null && !trainingWorker.isDone()) {
                int result = JOptionPane.showConfirmDialog(progressDialog,
                    "Are you sure you want to cancel the training?",
                    "Cancel Training", JOptionPane.YES_NO_OPTION);

                if (result == JOptionPane.YES_OPTION) {
                    trainingWorker.cancel(true);
                    progressDialog.dispose();
                }
            }
        });

        progressDialog.setLayout(new java.awt.BorderLayout());
        progressDialog.add(progressBar, java.awt.BorderLayout.CENTER);
        progressDialog.add(cancelButton, java.awt.BorderLayout.SOUTH);
        progressDialog.setSize(400, 120);
        progressDialog.setLocationRelativeTo(parentWindow);

        // Create and execute training worker
        trainingWorker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("Initializing XGBoost trainer...");

                try {
                    // Initialize trainer
                    trainer = new XGBoostTrainer(jsonFile, settings, outputDirPath);

                    publish("Starting model training...");
                    progressBar.setIndeterminate(false);
                    progressBar.setMaximum(100);
                    progressBar.setValue(0);

                    // Perform training
                    trainer.trainModel();

                    publish("Training completed successfully!");

                } catch (Exception ex) {
                    publish("Error: " + ex.getMessage());
                    logger.error("Training error", ex);
                    throw ex;
                }

                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    String latest = chunks.get(chunks.size() - 1);
                    progressBar.setString(latest);
                    logger.debug("Training progress: {}", latest);
                }
            }

            @Override
            protected void done() {
                progressDialog.dispose();

                try {
                    get(); // Check for exceptions
                    JOptionPane.showMessageDialog(parentWindow,
                        "XGBoost model training completed successfully!\n" +
                        "Model and results saved to:\n" + outputDirPath,
                        "Training Complete", JOptionPane.INFORMATION_MESSAGE);

                } catch (Exception ex) {
                    String errorMessage = "Training failed: " + ex.getMessage();
                    logger.error("Training failed", ex);
                    JOptionPane.showMessageDialog(parentWindow, errorMessage,
                        "Training Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        trainingWorker.execute();
        progressDialog.setVisible(true);
    }

    /**
     * Gets the current training settings.
     *
     * @return training settings
     */
    public TrainingSettings getSettings() {
        return settings;
    }

    /**
     * Sets the training settings.
     *
     * @param settings training settings
     */
    public void setSettings(TrainingSettings settings) {
        this.settings = settings;
    }

    /**
     * Gets the last trained model.
     *
     * @return XGBoost trainer instance (may be null if training not completed)
     */
    public XGBoostTrainer getTrainer() {
        return trainer;
    }
}