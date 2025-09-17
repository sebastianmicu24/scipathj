package com.scipath.scipathj.ui.training;

import com.scipath.scipathj.training.TrainingController;
import com.scipath.scipathj.training.TrainingSettings;
import com.scipath.scipathj.training.JSONDataReader;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Modern XGBoost training panel with comprehensive styling and advanced parameters.
 * Matches the program's design patterns and includes all SCHELI-style parameters.
 *
 * @author Sebastian Micu
 * @version 2.0.0
 */
public class XGBoostTrainingPanel extends JPanel {

    private final JFrame parentWindow;
    private final TrainingController trainingController;
    private TrainingSettings settings;

    // File selection components
    private JTextField jsonFileField;
    private JButton jsonBrowseButton;
    private JTextField outputDirField;
    private JButton outputBrowseButton;

    // Basic XGBoost parameters
    private JSpinner learningRateSpinner;
    private JSpinner maxDepthSpinner;
    private JSpinner numTreesSpinner;
    private JSpinner minChildWeightSpinner;
    private JSpinner subsampleSpinner;

    // Advanced SCHELI-style parameters
    private JSlider trainRatioSlider;
    private JCheckBox balanceClassesCheck;
    private JSpinner lambdaSpinner;
    private JSpinner alphaSpinner;
    private JSpinner gammaSpinner;
    private JSpinner colsampleBytreeSpinner;

    // Feature selection
    private JScrollPane featureScrollPane;
    private JPanel featurePanel;
    private java.util.Map<String, JCheckBox> featureCheckboxes = new java.util.HashMap<>();
    private JButton selectAllFeaturesButton;
    private JButton selectNoneFeaturesButton;
    
    // Feature status label for dynamic updates
    private JLabel featureStatusLabel;

    // Control buttons with proper styling
    private JButton startTrainingButton;
    private JButton resetButton;

    private File jsonFile;
    private File outputDir;

    // Theme-aware colors matching program style
    private static Color getPrimaryColor() {
        return new Color(0, 123, 255); // #007bff - blue works in both themes
    }

    private static Color getSuccessColor() {
        return new Color(40, 167, 69); // #28a745 - green works in both themes
    }

    private static Color getWarningColor() {
        return new Color(255, 193, 7); // #ffc107 - yellow/orange
    }

    private static Color getBackgroundColor() {
        return UIManager.getColor("Panel.background");
    }

    private static Color getCardColor() {
        return UIManager.getColor("Panel.background");
    }

    private static Color getTextSecondaryColor() {
        return UIManager.getColor("Label.disabledForeground");
    }

    private static Color getBorderColor() {
        Color fg = UIManager.getColor("Label.foreground");
        return new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 64);
    }

    public XGBoostTrainingPanel(JFrame parentWindow) {
        this.parentWindow = parentWindow;
        this.trainingController = new TrainingController(parentWindow);
        this.settings = new TrainingSettings();

        initializeComponents();
        loadCurrentSettings();
        setupEventHandlers();
    }

    /**
     * Create modern card panel with shadow effect.
     */
    private JPanel createModernCard() {
        JPanel card = new JPanel();
        card.setBackground(getCardColor());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            new EmptyBorder(20, 20, 20, 20)
        ));
        return card;
    }

    /**
     * Create modern file input panel.
     */
    private JPanel createModernFileInput(String labelText, String description,
                                        JTextField textField, JButton browseButton) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Label
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(UIManager.getColor("Label.foreground"));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);

        // Description
        if (description != null) {
            JLabel descLabel = new JLabel("<html><div style='width: 350px;'>" + description + "</div></html>");
            descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            descLabel.setForeground(getTextSecondaryColor());
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(descLabel);
        }

        panel.add(Box.createVerticalStrut(10));

        // Input panel with text field and button
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);
        inputPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Style text field
        textField.setEditable(false);
        textField.setPreferredSize(new Dimension(400, 35));
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            new EmptyBorder(6, 10, 6, 10)
        ));

        inputPanel.add(textField, BorderLayout.CENTER);
        inputPanel.add(browseButton, BorderLayout.EAST);

        panel.add(inputPanel);
        return panel;
    }

    /**
     * Create modern file input panel with enhanced styling and better width utilization.
     */
    private JPanel createModernFileInputNoBorder(String labelText, String description,
                                       JTextField textField, JButton browseButton) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(getCardColor());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            new EmptyBorder(20, 20, 20, 20)
        ));

        // Header panel with label and description
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        // Label with icon
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(UIManager.getColor("Label.foreground"));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(label);

        // Description
        if (description != null) {
            JLabel descLabel = new JLabel("<html><div style='width: 450px; color: #666;'>" + description + "</div></html>");
            descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            descLabel.setForeground(getTextSecondaryColor());
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            descLabel.setBorder(new EmptyBorder(5, 0, 15, 0));
            headerPanel.add(descLabel);
        }

        panel.add(headerPanel, BorderLayout.NORTH);

        // Input panel with full-width text field and button
        JPanel inputPanel = new JPanel(new BorderLayout(12, 0));
        inputPanel.setOpaque(false);

        // Style text field to use full available width
        textField.setEditable(false);
        textField.setPreferredSize(new Dimension(400, 40));
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        textField.setBackground(UIManager.getColor("TextField.background"));

        // Style browse button
        browseButton.setPreferredSize(new Dimension(140, 40));
        browseButton.setFont(new Font("Segoe UI", Font.BOLD, 12));

        inputPanel.add(textField, BorderLayout.CENTER);
        inputPanel.add(browseButton, BorderLayout.EAST);

        panel.add(inputPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Create a modern styled button.
     */
    private JButton createModernButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setPreferredSize(new Dimension(130, 32));
        button.setBorder(new EmptyBorder(6, 15, 6, 15));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    /**
     * Add parameter section header.
     */
    private void addParameterSection(JPanel panel, GridBagConstraints gbc, int row, String title) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        gbc.insets = new Insets(20, 10, 8, 10);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;

        JLabel sectionLabel = new JLabel(title);
        sectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        sectionLabel.setForeground(getPrimaryColor());

        JPanel sectionPanel = new JPanel(new BorderLayout());
        sectionPanel.setOpaque(false);
        sectionPanel.add(sectionLabel, BorderLayout.WEST);
        sectionPanel.add(new JSeparator(), BorderLayout.CENTER);
        sectionPanel.setBorder(new EmptyBorder(10, 0, 5, 0));

        panel.add(sectionPanel, gbc);
    }

    /**
     * Add spinner parameter row.
     */
    private void addSpinParameterRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent control) {
        // Label
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(8, 20, 8, 5);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(labelComponent, gbc);

        // Control
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(8, 5, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        panel.add(control, gbc);
    }


    /**
     * Initialize the panel with modern SciPathJ styling.
     */
    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBackground(getBackgroundColor());
        setBorder(new EmptyBorder(20, 30, 20, 30));

        // Modern header
        add(createModernHeader(), BorderLayout.NORTH);

        // Main content with card-based design
        add(createMainContent(), BorderLayout.CENTER);

        // Back button is handled by the main StatusPanel at the bottom of MainWindow
        // The StatusPanel back button will appear at the bottom of the application
    }

    /**
     * Create modern header with title and icon.
     */
    private JPanel createModernHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Icon section
        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        iconPanel.setOpaque(false);
        iconPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        FontIcon icon = FontIcon.of(FontAwesomeSolid.COG, 48, getPrimaryColor());
        iconPanel.add(new JLabel(icon));

        headerPanel.add(iconPanel, BorderLayout.CENTER);

        return headerPanel;
    }

    /**
     * Create main content with simple 2-column layout: parameters | file inputs & controls.
     */
    private JPanel createMainContent() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // Left column: Parameters with enhanced styling
        JPanel leftColumn = createEnhancedParametersPanel();

        // Right column: File inputs, feature selection, and buttons
        JPanel rightColumn = createRightColumnPanel();

        // Create horizontal split pane with better proportions
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(leftColumn);
        splitPane.setRightComponent(rightColumn);
        splitPane.setDividerSize(10);
        splitPane.setResizeWeight(0.4); // Give more space to the right column (60%)
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        splitPane.setDividerLocation(0.4);

        // Style the split pane divider
        splitPane.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(getBorderColor());
                        g.fillRect(0, 0, getSize().width, getSize().height);
                    }
                };
            }
        });

        contentPanel.add(splitPane, BorderLayout.CENTER);
        return contentPanel;
    }

    /**
     * Create enhanced parameters panel with better styling.
     */
    private JPanel createEnhancedParametersPanel() {
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.setOpaque(false);
        containerPanel.setBorder(new EmptyBorder(0, 0, 0, 10));

        JScrollPane paramScrollPane = new JScrollPane(createParameterGrid());
        paramScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        paramScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        paramScrollPane.setOpaque(false);
        paramScrollPane.getViewport().setOpaque(false);
        paramScrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        paramScrollPane.setBackground(getCardColor());
        paramScrollPane.getViewport().setBackground(getCardColor());

        containerPanel.add(paramScrollPane, BorderLayout.CENTER);
        return containerPanel;
    }

    /**
     * Create right column with file inputs, feature selection, and buttons.
     */
    private JPanel createRightColumnPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        rightPanel.setBorder(new EmptyBorder(0, 20, 0, 20)); // Better margins

        // Main content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // JSON file selection
        JPanel jsonPanel = createModernFileInputNoBorder(
            "Training Data (JSON File)",
            "Select the JSON file containing your training features and labels",
            jsonFileField = new JTextField(),
            jsonBrowseButton = createModernButton("Browse JSON Files", getSuccessColor())
        );
        contentPanel.add(jsonPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Output directory selection
        JPanel outputPanel = createModernFileInputNoBorder(
            "Output Directory",
            "Choose where to save the trained model and results",
            outputDirField = new JTextField(),
            outputBrowseButton = createModernButton("Browse Directory", getSuccessColor())
        );
        contentPanel.add(outputPanel);
        contentPanel.add(Box.createVerticalStrut(25));

        // Enhanced Feature selection section
        JPanel featureSection = createEnhancedFeatureSection();
        contentPanel.add(featureSection);

        rightPanel.add(contentPanel, BorderLayout.CENTER);

        // Actions panel at bottom with full-width button
        JPanel actionsPanel = createEnhancedActionsPanel();
        rightPanel.add(actionsPanel, BorderLayout.SOUTH);

        return rightPanel;
    }

    /**
     * Create enhanced feature selection section with better design.
     */
    private JPanel createEnhancedFeatureSection() {
        JPanel featureSection = new JPanel(new BorderLayout());
        featureSection.setOpaque(true);
        featureSection.setBackground(getCardColor());
        featureSection.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            new EmptyBorder(20, 20, 20, 20)
        ));

        // Header with title and description
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel featuresLabel = new JLabel("Feature Selection");
        featuresLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        featuresLabel.setForeground(UIManager.getColor("Label.foreground"));
        featuresLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(featuresLabel);

        JLabel descLabel = new JLabel("<html><div style='width: 450px; color: #666;'>Choose which features to include in the training process</div></html>");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(getTextSecondaryColor());
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descLabel.setBorder(new EmptyBorder(5, 0, 15, 0));
        headerPanel.add(descLabel);

        featureSection.add(headerPanel, BorderLayout.NORTH);

        // Controls panel with better layout
        JPanel controlsPanel = new JPanel(new BorderLayout());
        controlsPanel.setOpaque(false);
        controlsPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonsPanel.setOpaque(false);

        selectAllFeaturesButton = createModernButton("Select All", getPrimaryColor());
        selectNoneFeaturesButton = createModernButton("Select None", getWarningColor());
        selectAllFeaturesButton.setPreferredSize(new Dimension(110, 32));
        selectNoneFeaturesButton.setPreferredSize(new Dimension(110, 32));

        buttonsPanel.add(selectAllFeaturesButton);
        buttonsPanel.add(Box.createHorizontalStrut(10));
        buttonsPanel.add(selectNoneFeaturesButton);

        // Status info aligned to the right
        featureStatusLabel = new JLabel("Selected: 0 features");
        featureStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        featureStatusLabel.setForeground(getTextSecondaryColor());

        controlsPanel.add(buttonsPanel, BorderLayout.WEST);
        controlsPanel.add(featureStatusLabel, BorderLayout.EAST);

        featureSection.add(controlsPanel, BorderLayout.CENTER);

        // Feature list with better styling
        featurePanel = new JPanel();
        featurePanel.setLayout(new BoxLayout(featurePanel, BoxLayout.Y_AXIS));
        featurePanel.setOpaque(true);
        featurePanel.setBackground(UIManager.getColor("TextField.background"));
        featurePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane featureScrollPane = new JScrollPane(featurePanel);
        featureScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        featureScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        featureScrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        featureScrollPane.setPreferredSize(new Dimension(480, 200));
        featureScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // Add placeholder
        updateFeatureList();

        featureSection.add(featureScrollPane, BorderLayout.SOUTH);

        return featureSection;
    }

    /**
     * Create enhanced actions panel with full-width button.
     */
    private JPanel createEnhancedActionsPanel() {
        JPanel actionsPanel = new JPanel(new BorderLayout());
        actionsPanel.setOpaque(false);
        actionsPanel.setBorder(new EmptyBorder(25, 0, 0, 0));

        // Start Training button - full width with better styling
        startTrainingButton = new JButton("Start Training");
        startTrainingButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        startTrainingButton.setForeground(Color.WHITE);
        startTrainingButton.setBackground(getSuccessColor());
        startTrainingButton.setPreferredSize(new Dimension(0, 50)); // Full width, 50px height
        startTrainingButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getSuccessColor().darker(), 1),
            new EmptyBorder(12, 20, 12, 20)
        ));
        startTrainingButton.setFocusPainted(false);
        startTrainingButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Enhanced hover effect
        startTrainingButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                startTrainingButton.setBackground(getSuccessColor().darker());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                startTrainingButton.setBackground(getSuccessColor());
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                startTrainingButton.setBackground(getSuccessColor().darker().darker());
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                startTrainingButton.setBackground(startTrainingButton.contains(evt.getPoint()) ?
                    getSuccessColor().darker() : getSuccessColor());
            }
        });

        actionsPanel.add(startTrainingButton, BorderLayout.CENTER);

        return actionsPanel;
    }

    /**
     * Create parameter grid with all controls.
     */
    private JPanel createParameterGrid() {
        JPanel gridPanel = new JPanel(new GridBagLayout());
        gridPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Basic Parameters Section
        addParameterSection(gridPanel, gbc, row++, "Basic Parameters");

        // Learning Rate
        addSpinParameterRow(gridPanel, gbc, row++, "Learning Rate:",
            learningRateSpinner = new JSpinner(new SpinnerNumberModel(0.1, 0.01, 1.0, 0.01)));
        addSpinParameterRow(gridPanel, gbc, row++, "Max Depth:",
            maxDepthSpinner = new JSpinner(new SpinnerNumberModel(6, 1, 20, 1)));
        addSpinParameterRow(gridPanel, gbc, row++, "Number of Trees:",
            numTreesSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 2000, 50)));
        addSpinParameterRow(gridPanel, gbc, row++, "Min Child Weight:",
            minChildWeightSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1)));
        addSpinParameterRow(gridPanel, gbc, row++, "Subsample:",
            subsampleSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 1.0, 0.05)));

        // Advanced Parameters Section
        addParameterSection(gridPanel, gbc, row++, "Advanced Parameters");

        addSpinParameterRow(gridPanel, gbc, row++, "Lambda (L2 Reg):",
            lambdaSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0, 10.0, 0.1)));
        addSpinParameterRow(gridPanel, gbc, row++, "Alpha (L1 Reg):",
            alphaSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0, 10.0, 0.1)));
        addSpinParameterRow(gridPanel, gbc, row++, "Gamma (Loss Red):",
            gammaSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0, 10.0, 0.1)));
        addSpinParameterRow(gridPanel, gbc, row++, "Column Sample:",
            colsampleBytreeSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 1.0, 0.05)));

        // Training Configuration Section
        addParameterSection(gridPanel, gbc, row++, "Training Configuration");

        // Train/Test ratio slider
        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JPanel sliderPanel = new JPanel(new BorderLayout(10, 0));
        sliderPanel.setOpaque(false);

        sliderPanel.add(new JLabel("Train/Test Split Ratio:"), BorderLayout.WEST);

        trainRatioSlider = new JSlider(50, 95, 75);
        trainRatioSlider.setMajorTickSpacing(5);
        trainRatioSlider.setPaintTicks(true);
        trainRatioSlider.setPaintLabels(true);
        trainRatioSlider.setSnapToTicks(true);
        sliderPanel.add(trainRatioSlider, BorderLayout.CENTER);

        gridPanel.add(sliderPanel, gbc);

        // Balance classes checkbox
        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.insets = new Insets(15, 10, 8, 10);

        balanceClassesCheck = new JCheckBox("Balance Classes (Recommended for imbalanced data)");
        balanceClassesCheck.setSelected(true);
        balanceClassesCheck.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        balanceClassesCheck.setOpaque(false);
        gridPanel.add(balanceClassesCheck, gbc);

        return gridPanel;
    }

    /**
     * Load current settings into UI components.
     */
    private void loadCurrentSettings() {
        // Load parameter values from settings into all controls
        learningRateSpinner.setValue(settings.getLearningRate());
        maxDepthSpinner.setValue(settings.getMaxDepth());
        numTreesSpinner.setValue(settings.getNumTrees());
        minChildWeightSpinner.setValue(settings.getMinChildWeight());
        subsampleSpinner.setValue(settings.getSubsample());
        trainRatioSlider.setValue(Math.round(settings.getTrainRatio() * 100));
        balanceClassesCheck.setSelected(settings.isBalanceClasses());

        // Advanced parameters
        lambdaSpinner.setValue(settings.getLambda());
        alphaSpinner.setValue(settings.getAlpha());
        gammaSpinner.setValue(settings.getGamma());
        colsampleBytreeSpinner.setValue(settings.getColsampleBytree());

        // Load file paths
        if (jsonFile != null) {
            jsonFileField.setText(jsonFile.getAbsolutePath());
        }
        if (outputDir != null) {
            outputDirField.setText(outputDir.getAbsolutePath());
        }

        // Update feature list
        updateFeatureList();
    }

    /**
     * Set up event handlers for all components.
     */
    private void setupEventHandlers() {
        // Browse buttons
        jsonBrowseButton.addActionListener(e -> browseJsonFile());
        outputBrowseButton.addActionListener(e -> browseOutputDirectory());

        // Feature selection buttons
        if (selectAllFeaturesButton != null) {
            selectAllFeaturesButton.addActionListener(e -> selectAllFeatures());
        }
        if (selectNoneFeaturesButton != null) {
            selectNoneFeaturesButton.addActionListener(e -> selectNoneFeatures());
        }

        // Main action button
        startTrainingButton.addActionListener(e -> startTraining());
        
        // Back button is handled by the main StatusPanel - no action listener needed here
    }

    /**
     * Feature selection helper methods.
     */
    private void selectAllFeatures() {
        for (JCheckBox checkBox : featureCheckboxes.values()) {
            checkBox.setSelected(true);
        }
    }

    private void selectNoneFeatures() {
        for (JCheckBox checkBox : featureCheckboxes.values()) {
            checkBox.setSelected(false);
        }
    }

    /**
     * Update feature list with modern styling.
     */
    private void updateFeatureList() {
        featurePanel.removeAll();

        // If we have JSON file selected, try to preview features
        if (jsonFile != null && jsonFile.exists()) {
            try {
                // Create a temporary reader to detect features
                JSONDataReader tempReader = new JSONDataReader(jsonFile, null);
                java.util.List<String> features = tempReader.getFeatureNames();

                if (features.isEmpty()) {
                    featurePanel.add(new JLabel("No features detected or JSON file is empty."));
                } else {
                    featurePanel.add(new JLabel(String.format("Detected %d features:", features.size())));
                    featurePanel.add(Box.createVerticalStrut(5));

                    for (String feature : features) {
                        JCheckBox checkBox = new JCheckBox(feature);
                        checkBox.setSelected(settings.isFeatureEnabled(feature)); // Default to enabled
                        checkBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                        checkBox.setOpaque(false);
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

    /**
     * Browse for JSON file with modern interface.
     */
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

    /**
     * Browse for output directory with modern interface.
     */
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

    /**
     * Reset to defaults with modern styling.
     */
    private void resetToDefaults() {
        settings.resetToDefaults();
        loadCurrentSettings();
        updateFeatureList();
    }

    /**
     * Start training directly using panel settings (no dialog).
     */
    private void startTraining() {
        if (validateAndSaveSettings()) {
            // Start training directly with the panel settings
            trainingController.startTrainingWithSettings(settings, jsonFile, outputDir);
        }
    }


    /**
     * Validate and save settings.
     */
    private boolean validateAndSaveSettings() {
        // Update settings from UI - basic parameters
        settings.setLearningRate(((Number) learningRateSpinner.getValue()).floatValue());
        settings.setMaxDepth((Integer) maxDepthSpinner.getValue());
        settings.setNumTrees((Integer) numTreesSpinner.getValue());
        settings.setMinChildWeight((Integer) minChildWeightSpinner.getValue());
        settings.setSubsample(((Number) subsampleSpinner.getValue()).floatValue());
        settings.setTrainRatio((float) (trainRatioSlider.getValue() / 100.0));
        settings.setBalanceClasses(balanceClassesCheck.isSelected());

        // Update settings from UI - advanced SCHELI parameters
        settings.setLambda(((Number) lambdaSpinner.getValue()).floatValue());
        settings.setAlpha(((Number) alphaSpinner.getValue()).floatValue());
        settings.setGamma(((Number) gammaSpinner.getValue()).floatValue());
        settings.setColsampleBytree(((Number) colsampleBytreeSpinner.getValue()).floatValue());

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
        }

        return true;
    }
}