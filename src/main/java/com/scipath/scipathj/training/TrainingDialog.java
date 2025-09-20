package com.scipath.scipathj.training;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Configuration dialog for XGBoost training in SciPathJ.
 * Provides settings for model parameters, file paths, and feature selection.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class TrainingDialog extends JDialog {

    private static final int WINDOW_WIDTH = 700;
    private static final int WINDOW_HEIGHT = 600;

    private TrainingSettings settings;
    private File jsonFile;
    private File outputDir;
    private boolean approved = false;

    // UI Components
    private JTextField jsonFileField;
    private JButton jsonBrowseButton;
    private JTextField outputDirField;
    private JButton outputBrowseButton;

    // Parameter sliders
    private JSpinner learningRateSpinner;
    private JSpinner maxDepthSpinner;
    private JSpinner numTreesSpinner;
    private JSpinner minChildWeightSpinner;
    private JSpinner subsampleSpinner;
    private JSlider trainRatioSlider;
    private JCheckBox balanceClassesCheck;

    // Feature list (simplified)
    private JScrollPane featureScrollPane;
    private JPanel featurePanel;
    private java.util.Map<String, JCheckBox> featureCheckboxes = new java.util.HashMap<>();

    /**
     * Constructor.
     *
     * @param parent parent window
     * @param initialSettings initial training settings
     * @param initialJsonFile initial JSON file (can be null)
     * @param initialOutputDir initial output directory (can be null)
     */
    public TrainingDialog(Frame parent, TrainingSettings initialSettings,
                         File initialJsonFile, File initialOutputDir) {
        super(parent, "XGBoost Model Training Configuration", true);

        this.settings = initialSettings != null
            ? new TrainingSettings().copyFrom(initialSettings)
            : new TrainingSettings();
        this.jsonFile = initialJsonFile;
        this.outputDir = initialOutputDir;

        initializeDialog();
        createComponents();
        loadCurrentSettings();
        setupEventHandlers();

        pack();
        setLocationRelativeTo(parent);
    }

    private void initializeDialog() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/icon.png"));
            setIconImage(icon.getImage());
        } catch (Exception e) {
            // Icon loading failed, continue without icon
        }
    }

    private void createComponents() {
        setLayout(new BorderLayout());

        // Main content
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // File paths section
        createFilePathsSection(mainPanel, gbc);

        // Parameters section
        createParametersSection(mainPanel, gbc);

        // Features section
        createFeaturesSection(mainPanel, gbc);

        // Add main panel to dialog with padding
        JPanel paddedPanel = new JPanel(new BorderLayout());
        paddedPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        paddedPanel.add(mainPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(paddedPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private void createFilePathsSection(JPanel panel, GridBagConstraints gbc) {
        TitledBorder titledBorder = BorderFactory.createTitledBorder("File Paths");
        titledBorder.setTitleFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        JPanel filePanel = new JPanel(new GridBagLayout());
        filePanel.setBorder(titledBorder);

        GridBagConstraints fileGbc = new GridBagConstraints();
        fileGbc.insets = new Insets(2, 5, 2, 5);
        fileGbc.fill = GridBagConstraints.HORIZONTAL;
        fileGbc.weightx = 1.0;

        // JSON file row
        fileGbc.gridy = 0;
        fileGbc.gridx = 0;
        filePanel.add(new JLabel("Training Data (JSON):"), fileGbc);

        fileGbc.gridx = 1;
        jsonFileField = new JTextField(30);
        jsonFileField.setEditable(false);
        jsonFileField.setText(jsonFile != null ? jsonFile.getAbsolutePath() : "");
        filePanel.add(jsonFileField, fileGbc);

        fileGbc.gridx = 2;
        jsonBrowseButton = new JButton("Browse...");
        jsonBrowseButton.setIcon(FontIcon.of(FontAwesomeSolid.FILE, 14));
        filePanel.add(jsonBrowseButton, fileGbc);

        // Output directory row
        fileGbc.gridy = 1;
        fileGbc.gridx = 0;
        filePanel.add(new JLabel("Output Directory:"), fileGbc);

        fileGbc.gridx = 1;
        outputDirField = new JTextField(30);
        outputDirField.setEditable(false);
        outputDirField.setText(outputDir != null ? outputDir.getAbsolutePath() : "");
        filePanel.add(outputDirField, fileGbc);

        fileGbc.gridx = 2;
        outputBrowseButton = new JButton("Browse...");
        outputBrowseButton.setIcon(FontIcon.of(FontAwesomeSolid.FOLDER, 14));
        filePanel.add(outputBrowseButton, fileGbc);

        gbc.gridy = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(filePanel, gbc);
    }

    private void createParametersSection(JPanel panel, GridBagConstraints gbc) {
        TitledBorder titledBorder = BorderFactory.createTitledBorder("XGBoost Parameters");
        titledBorder.setTitleFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        JPanel paramPanel = new JPanel(new GridBagLayout());
        paramPanel.setBorder(titledBorder);

        GridBagConstraints paramGbc = new GridBagConstraints();
        paramGbc.insets = new Insets(2, 5, 2, 5);
        paramGbc.fill = GridBagConstraints.HORIZONTAL;

        // Learning rate
        paramGbc.gridy = 0;
        paramGbc.gridx = 0;
        paramPanel.add(new JLabel("Learning Rate:"), paramGbc);
        paramGbc.gridx = 1;
        learningRateSpinner = new JSpinner(new SpinnerNumberModel(0.1, 0.01, 1.0, 0.01));
        paramPanel.add(learningRateSpinner, paramGbc);

        paramGbc.gridy = 1;
        paramGbc.gridx = 0;
        paramPanel.add(new JLabel("Max Depth:"), paramGbc);
        paramGbc.gridx = 1;
        maxDepthSpinner = new JSpinner(new SpinnerNumberModel(6, 1, 20, 1));
        paramPanel.add(maxDepthSpinner, paramGbc);

        paramGbc.gridy = 2;
        paramGbc.gridx = 0;
        paramPanel.add(new JLabel("Number of Trees:"), paramGbc);
        paramGbc.gridx = 1;
        numTreesSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 2000, 50));
        paramPanel.add(numTreesSpinner, paramGbc);

        paramGbc.gridy = 3;
        paramGbc.gridx = 0;
        paramPanel.add(new JLabel("Min Child Weight:"), paramGbc);
        paramGbc.gridx = 1;
        minChildWeightSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
        paramPanel.add(minChildWeightSpinner, paramGbc);

        paramGbc.gridy = 4;
        paramGbc.gridx = 0;
        paramPanel.add(new JLabel("Subsample:"), paramGbc);
        paramGbc.gridx = 1;
        subsampleSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 1.0, 0.05));
        paramPanel.add(subsampleSpinner, paramGbc);

        // Train ratio
        paramGbc.gridy = 5;
        paramGbc.gridx = 0;
        paramPanel.add(new JLabel("Train Ratio:"), paramGbc);
        paramGbc.gridx = 1;
        trainRatioSlider = new JSlider(50, 90, 70);
        trainRatioSlider.setMajorTickSpacing(10);
        trainRatioSlider.setPaintTicks(true);
        trainRatioSlider.setPaintLabels(true);
        trainRatioSlider.setSnapToTicks(true);
        paramPanel.add(trainRatioSlider, paramGbc);

        // Balance classes
        paramGbc.gridy = 6;
        paramGbc.gridx = 0;
        paramGbc.gridwidth = 2;
        balanceClassesCheck = new JCheckBox("Balance Classes (Recommended for imbalanced data)");
        balanceClassesCheck.setSelected(true);
        paramPanel.add(balanceClassesCheck, paramGbc);

        gbc.gridy = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(paramPanel, gbc);
    }

    private void createFeaturesSection(JPanel panel, GridBagConstraints gbc) {
        TitledBorder titledBorder = BorderFactory.createTitledBorder("Feature Selection");
        titledBorder.setTitleFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        featurePanel = new JPanel();
        featurePanel.setLayout(new BoxLayout(featurePanel, BoxLayout.Y_AXIS));
        featurePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Add descriptive label
        JLabel descriptionLabel = new JLabel("Features will be automatically detected from the JSON file.");
        descriptionLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        featurePanel.add(descriptionLabel);

        featureScrollPane = new JScrollPane(featurePanel);
        featureScrollPane.setPreferredSize(new Dimension(-1, 150));
        featureScrollPane.setBorder(titledBorder);

        gbc.gridy = 2;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        panel.add(featureScrollPane, gbc);
    }

    private void loadCurrentSettings() {
        // Load parameter values from settings
        learningRateSpinner.setValue(settings.getLearningRate());
        maxDepthSpinner.setValue(settings.getMaxDepth());
        numTreesSpinner.setValue(settings.getNumTrees());
        minChildWeightSpinner.setValue(settings.getMinChildWeight());
        subsampleSpinner.setValue(settings.getSubsample());
        trainRatioSlider.setValue(Math.round(settings.getTrainRatio() * 100));
        balanceClassesCheck.setSelected(settings.isBalanceClasses());

        // Load file paths
        if (jsonFile != null) {
            jsonFileField.setText(jsonFile.getAbsolutePath());
        }
        if (outputDir != null) {
            outputDirField.setText(outputDir.getAbsolutePath());
        }

        // This would be updated when JSON file is selected to show available features
        updateFeatureList();
    }

    private void updateFeatureList() {
        featurePanel.removeAll();

        // If we have JSON file selected, try to preview features
        if (jsonFile != null && jsonFile.exists()) {
            try {
                // Create a temporary reader to detect features
                com.scipath.scipathj.training.JSONDataReader tempReader =
                    new com.scipath.scipathj.training.JSONDataReader(jsonFile, null);
                java.util.List<String> features = tempReader.getFeatureNames();

                if (features.isEmpty()) {
                    featurePanel.add(new JLabel("No features detected or JSON file is empty."));
                } else {
                    featurePanel.add(new JLabel(String.format("Detected %d features:", features.size())));
                    featurePanel.add(Box.createVerticalStrut(5));

                    for (String feature : features) {
                        JCheckBox checkBox = new JCheckBox(feature);
                        checkBox.setSelected(settings.isFeatureEnabled(feature)); // Default to enabled
                        featureCheckboxes.put(feature, checkBox);
                        featurePanel.add(checkBox);
                    }
                }

            } catch (Exception e) {
                featurePanel.add(new JLabel("Error reading JSON file: " + e.getMessage()));
            }
        } else {
            featurePanel.add(new JLabel("Select a JSON file to see available features."));
        }

        featurePanel.revalidate();
        featurePanel.repaint();
    }

    private void setupEventHandlers() {
        // Browse buttons
        jsonBrowseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseJsonFile();
            }
        });

        outputBrowseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseOutputDirectory();
            }
        });
    }

    private void browseJsonFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Training Data JSON File");
        fileChooser.setAcceptAllFileFilterUsed(true);

        if (jsonFile != null) {
            fileChooser.setSelectedFile(jsonFile);
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            jsonFile = fileChooser.getSelectedFile();
            jsonFileField.setText(jsonFile.getAbsolutePath());
            updateFeatureList();
        }
    }

    private void browseOutputDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Output Directory for Model");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (outputDir != null) {
            fileChooser.setSelectedFile(outputDir);
        }

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDir = fileChooser.getSelectedFile();
            outputDirField.setText(outputDir.getAbsolutePath());
        }
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = new JButton("Start Training");
        okButton.setPreferredSize(new Dimension(120, 30));
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validateAndSaveSettings()) {
                    approved = true;
                    dispose();
                }
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(80, 30));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                approved = false;
                dispose();
            }
        });

        JButton resetButton = new JButton("Reset");
        resetButton.setPreferredSize(new Dimension(80, 30));
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetToDefaults();
            }
        });

        buttonPanel.add(resetButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);

        // Set OK button as default
        getRootPane().setDefaultButton(okButton);

        return buttonPanel;
    }

    private boolean validateAndSaveSettings() {
        // Update settings from UI
        settings.setLearningRate(((Number) learningRateSpinner.getValue()).floatValue());
        settings.setMaxDepth((Integer) maxDepthSpinner.getValue());
        settings.setNumTrees((Integer) numTreesSpinner.getValue());
        settings.setMinChildWeight((Integer) minChildWeightSpinner.getValue());
        settings.setSubsample(((Number) subsampleSpinner.getValue()).floatValue());
        settings.setTrainRatio((float) (trainRatioSlider.getValue() / 100.0));
        settings.setBalanceClasses(balanceClassesCheck.isSelected());

        // Validate settings
        java.util.List<String> errors = settings.validate();
        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Invalid settings:\n" + String.join("\n", errors),
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Validate files
        if (jsonFile == null || !jsonFile.exists()) {
            JOptionPane.showMessageDialog(this,
                "Please select a valid JSON training data file.",
                "Missing File", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (outputDir == null) {
            JOptionPane.showMessageDialog(this,
                "Please select an output directory for the model.",
                "Missing Directory", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Update feature selections
        if (!featureCheckboxes.isEmpty()) {
            java.util.List<String> selectedFeatures = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, JCheckBox> entry : featureCheckboxes.entrySet()) {
                if (entry.getValue().isSelected()) {
                    selectedFeatures.add(entry.getKey());
                }
            }
            settings.setSelectedFeatures(selectedFeatures);
        } else {
            // If no checkboxes, select all features
            // This will be handled in the JSONDataReader
        }

        return true;
    }

    private void resetToDefaults() {
        settings.resetToDefaults();
        loadCurrentSettings();
        updateFeatureList();
    }

    // Getters for result
    public TrainingSettings getSettings() {
        return settings;
    }

    public File getJsonFile() {
        return jsonFile;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public boolean isApproved() {
        return approved;
    }
}