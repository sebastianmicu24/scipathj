package com.scipath.scipathj.ui.training.wizard;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.text.DecimalFormat;
import java.util.Map;

/**
 * Step 2: Data Split Configuration Panel.
 * Handles train/evaluation/test split configuration with interactive sliders and live preview.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 */
public class DataSplitStepPanel extends JPanel {

    private static final Color PRIMARY_COLOR = new Color(0, 123, 255);
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
    private static final Color DANGER_COLOR = new Color(220, 53, 69);
    private static final Color TRAIN_COLOR = new Color(54, 162, 235);
    private static final Color EVAL_COLOR = new Color(255, 159, 64);
    private static final Color TEST_COLOR = new Color(75, 192, 192);

    private final TrainingWizardState wizardState;
    private final XGBoostTrainingWizardManager wizardManager;

    // Split ratio controls
    private JSlider trainRatioSlider;
    private JSlider evalRatioSlider;
    private JSlider testRatioSlider;
    private JLabel trainPercentLabel;
    private JLabel evalPercentLabel;
    private JLabel testPercentLabel;
    private JLabel trainSamplesLabel;
    private JLabel evalSamplesLabel;
    private JLabel testSamplesLabel;

    // Visual preview
    private SplitVisualizationPanel visualPreview;
    
    // Class distribution table
    private JTable classDistributionTable;
    private JScrollPane classTableScrollPane;
    
    // Validation status
    private JLabel validationStatusLabel;
    private JPanel constraintsPanel;

    private boolean updating = false; // Prevent recursive updates

    /**
     * Creates the data split configuration step panel.
     *
     * @param wizardState The shared wizard state
     * @param wizardManager The wizard manager
     */
    public DataSplitStepPanel(TrainingWizardState wizardState, 
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
        // Split ratio sliders
        trainRatioSlider = new JSlider(10, 80, 70);
        trainRatioSlider.setMajorTickSpacing(10);
        trainRatioSlider.setMinorTickSpacing(5);
        trainRatioSlider.setPaintTicks(true);
        trainRatioSlider.setPaintLabels(true);
        
        evalRatioSlider = new JSlider(5, 40, 20);
        evalRatioSlider.setMajorTickSpacing(5);
        evalRatioSlider.setMinorTickSpacing(5);
        evalRatioSlider.setPaintTicks(true);
        evalRatioSlider.setPaintLabels(true);
        
        testRatioSlider = new JSlider(5, 30, 10);
        testRatioSlider.setMajorTickSpacing(5);
        testRatioSlider.setMinorTickSpacing(5);
        testRatioSlider.setPaintTicks(true);
        testRatioSlider.setPaintLabels(true);

        // Percentage labels
        trainPercentLabel = new JLabel("70%");
        trainPercentLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        trainPercentLabel.setForeground(TRAIN_COLOR);
        
        evalPercentLabel = new JLabel("20%");
        evalPercentLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        evalPercentLabel.setForeground(EVAL_COLOR);
        
        testPercentLabel = new JLabel("10%");
        testPercentLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        testPercentLabel.setForeground(TEST_COLOR);

        // Sample count labels
        trainSamplesLabel = new JLabel("(0 samples)");
        evalSamplesLabel = new JLabel("(0 samples)");
        testSamplesLabel = new JLabel("(0 samples)");

        // Visual preview
        visualPreview = new SplitVisualizationPanel();
        
        // Class distribution table
        createClassDistributionTable();
        
        // Validation status
        validationStatusLabel = new JLabel("✅ Balanced distribution across all splits");
        validationStatusLabel.setForeground(SUCCESS_COLOR);
        validationStatusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        
        // Constraints panel
        constraintsPanel = createConstraintsPanel();
    }

    /**
     * Create the class distribution table.
     */
    private void createClassDistributionTable() {
        String[] columns = {"Class", "Train", "Eval", "Test", "Total"};
        String[][] data = {{"No data", "0", "0", "0", "0"}};
        
        classDistributionTable = new JTable(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        classDistributionTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        classDistributionTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        classDistributionTable.setRowHeight(25);
        classDistributionTable.setShowGrid(true);
        classDistributionTable.setGridColor(new Color(223, 225, 229));
        
        classTableScrollPane = new JScrollPane(classDistributionTable);
        classTableScrollPane.setPreferredSize(new Dimension(400, 120));
        classTableScrollPane.setBorder(BorderFactory.createLineBorder(new Color(223, 225, 229)));
    }

    /**
     * Create constraints information panel.
     */
    private JPanel createConstraintsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        panel.setOpaque(false);
        
        JLabel constraintLabel = new JLabel("⚠️ Constraint: Each split must be at least 5% (minimum for statistical validity)");
        constraintLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        constraintLabel.setForeground(WARNING_COLOR);
        constraintLabel.setIcon(FontIcon.of(FontAwesomeSolid.INFO_CIRCLE, 14, WARNING_COLOR));
        
        panel.add(constraintLabel);
        return panel;
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

        // Split configuration section
        JPanel splitConfigSection = createSplitConfigurationSection();
        
        // Preview section
        JPanel previewSection = createPreviewSection();
        
        // Validation section
        JPanel validationSection = createValidationSection();

        mainPanel.add(splitConfigSection, BorderLayout.NORTH);
        mainPanel.add(previewSection, BorderLayout.CENTER);
        mainPanel.add(validationSection, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Create the split configuration section.
     */
    private JPanel createSplitConfigurationSection() {
        JPanel section = new JPanel(new GridLayout(3, 1, 10, 15));
        section.setOpaque(false);
        section.setBorder(createTitledBorder("Split Configuration"));

        // Training set row
        JPanel trainRow = createSplitRow("Training Set:", trainRatioSlider, trainPercentLabel, trainSamplesLabel, TRAIN_COLOR);
        
        // Evaluation set row
        JPanel evalRow = createSplitRow("Evaluation Set:", evalRatioSlider, evalPercentLabel, evalSamplesLabel, EVAL_COLOR);
        
        // Test set row
        JPanel testRow = createSplitRow("Test Set:", testRatioSlider, testPercentLabel, testSamplesLabel, TEST_COLOR);

        section.add(trainRow);
        section.add(evalRow);
        section.add(testRow);

        return section;
    }

    /**
     * Create a split configuration row.
     */
    private JPanel createSplitRow(String label, JSlider slider, JLabel percentLabel, JLabel samplesLabel, Color color) {
        JPanel row = new JPanel(new BorderLayout(15, 0));
        row.setOpaque(false);

        // Label
        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        nameLabel.setPreferredSize(new Dimension(120, 25));
        nameLabel.setForeground(color);

        // Slider
        slider.setOpaque(false);
        slider.setPreferredSize(new Dimension(300, 50));

        // Percentage and samples info
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        infoPanel.setOpaque(false);
        infoPanel.add(percentLabel);
        infoPanel.add(samplesLabel);

        row.add(nameLabel, BorderLayout.WEST);
        row.add(slider, BorderLayout.CENTER);
        row.add(infoPanel, BorderLayout.EAST);

        return row;
    }

    /**
     * Create the preview section.
     */
    private JPanel createPreviewSection() {
        JPanel section = new JPanel(new BorderLayout(20, 15));
        section.setOpaque(false);
        section.setBorder(createTitledBorder("Preview"));

        // Left: Visual pie chart
        JPanel visualPanel = new JPanel(new BorderLayout());
        visualPanel.setOpaque(false);
        visualPanel.setBorder(BorderFactory.createTitledBorder("Visual Split Preview"));
        visualPanel.add(visualPreview, BorderLayout.CENTER);
        visualPanel.setPreferredSize(new Dimension(300, 200));

        // Right: Class distribution table
        JPanel distributionPanel = new JPanel(new BorderLayout());
        distributionPanel.setOpaque(false);
        distributionPanel.setBorder(BorderFactory.createTitledBorder("Class Distribution Preview"));
        distributionPanel.add(classTableScrollPane, BorderLayout.CENTER);

        section.add(visualPanel, BorderLayout.WEST);
        section.add(distributionPanel, BorderLayout.CENTER);

        return section;
    }

    /**
     * Create the validation section.
     */
    private JPanel createValidationSection() {
        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setOpaque(false);
        section.setBorder(createTitledBorder("Split Validation"));

        section.add(validationStatusLabel, BorderLayout.CENTER);
        section.add(constraintsPanel, BorderLayout.SOUTH);

        return section;
    }

    /**
     * Setup event handlers.
     */
    private void setupEventHandlers() {
        ChangeListener sliderListener = e -> {
            if (!updating) {
                adjustSplitRatios();
                updateDisplay();
            }
        };

        trainRatioSlider.addChangeListener(sliderListener);
        evalRatioSlider.addChangeListener(sliderListener);
        testRatioSlider.addChangeListener(sliderListener);
    }

    /**
     * Adjust split ratios to ensure they sum to 100%.
     */
    private void adjustSplitRatios() {
        updating = true;
        
        int trainPercent = trainRatioSlider.getValue();
        int evalPercent = evalRatioSlider.getValue();
        int testPercent = testRatioSlider.getValue();
        
        // Ensure ratios sum to 100%
        int total = trainPercent + evalPercent + testPercent;
        if (total != 100) {
            // Adjust the test ratio to make total 100%
            testPercent = 100 - trainPercent - evalPercent;
            testPercent = Math.max(5, Math.min(30, testPercent)); // Keep within bounds
            testRatioSlider.setValue(testPercent);
        }
        
        // Update wizard state
        wizardState.setTrainRatio(trainPercent / 100.0);
        wizardState.setEvalRatio(evalPercent / 100.0);
        wizardState.setTestRatio(testPercent / 100.0);
        
        updating = false;
    }

    /**
     * Update all display components.
     */
    public void updateDisplay() {
        if (updating) return;
        
        updating = true;
        
        // Update sliders from wizard state
        trainRatioSlider.setValue((int) Math.round(wizardState.getTrainRatio() * 100));
        evalRatioSlider.setValue((int) Math.round(wizardState.getEvalRatio() * 100));
        testRatioSlider.setValue((int) Math.round(wizardState.getTestRatio() * 100));
        
        // Update percentage labels
        DecimalFormat df = new DecimalFormat("#");
        trainPercentLabel.setText(df.format(wizardState.getTrainRatio() * 100) + "%");
        evalPercentLabel.setText(df.format(wizardState.getEvalRatio() * 100) + "%");
        testPercentLabel.setText(df.format(wizardState.getTestRatio() * 100) + "%");
        
        // Update sample counts
        int totalSamples = wizardState.getTotalSamples();
        int trainSamples = (int) Math.round(totalSamples * wizardState.getTrainRatio());
        int evalSamples = (int) Math.round(totalSamples * wizardState.getEvalRatio());
        int testSamples = totalSamples - trainSamples - evalSamples;
        
        trainSamplesLabel.setText("(" + trainSamples + " samples)");
        evalSamplesLabel.setText("(" + evalSamples + " samples)");
        testSamplesLabel.setText("(" + testSamples + " samples)");
        
        // Update visual preview
        visualPreview.updateRatios(wizardState.getTrainRatio(), wizardState.getEvalRatio(), wizardState.getTestRatio());
        
        // Update class distribution table
        updateClassDistributionTable();
        
        // Update validation status
        updateValidationStatus();
        
        updating = false;
    }

    /**
     * Update the class distribution table.
     */
    private void updateClassDistributionTable() {
        Map<String, Integer> classDistribution = wizardState.getClassDistribution();
        Map<String, Integer> trainCounts = wizardState.getTrainClassCounts();
        Map<String, Integer> evalCounts = wizardState.getEvalClassCounts();
        Map<String, Integer> testCounts = wizardState.getTestClassCounts();
        
        if (classDistribution.isEmpty()) {
            String[][] data = {{"No data loaded", "0", "0", "0", "0"}};
            String[] columns = {"Class", "Train", "Eval", "Test", "Total"};
            classDistributionTable.setModel(new javax.swing.table.DefaultTableModel(data, columns) {
                @Override
                public boolean isCellEditable(int row, int column) { return false; }
            });
            return;
        }
        
        String[][] data = new String[classDistribution.size()][5];
        String[] columns = {"Class", "Train", "Eval", "Test", "Total"};
        
        int row = 0;
        for (Map.Entry<String, Integer> entry : classDistribution.entrySet()) {
            String className = entry.getKey();
            int total = entry.getValue();
            int train = trainCounts.getOrDefault(className, 0);
            int eval = evalCounts.getOrDefault(className, 0);
            int test = testCounts.getOrDefault(className, 0);
            
            data[row][0] = className;
            data[row][1] = String.valueOf(train);
            data[row][2] = String.valueOf(eval);
            data[row][3] = String.valueOf(test);
            data[row][4] = String.valueOf(total);
            row++;
        }
        
        classDistributionTable.setModel(new javax.swing.table.DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        });
    }

    /**
     * Update validation status.
     */
    private void updateValidationStatus() {
        double total = wizardState.getTrainRatio() + wizardState.getEvalRatio() + wizardState.getTestRatio();
        boolean ratiosValid = Math.abs(total - 1.0) < 0.001;
        
        int totalSamples = wizardState.getTotalSamples();
        int minSamples = Math.max(1, totalSamples / 20); // At least 5% or 1 sample
        
        boolean trainValid = wizardState.getTrainRatio() * totalSamples >= minSamples;
        boolean evalValid = wizardState.getEvalRatio() * totalSamples >= minSamples;
        boolean testValid = wizardState.getTestRatio() * totalSamples >= minSamples;
        
        if (!ratiosValid) {
            validationStatusLabel.setText("❌ Split ratios must sum to 100%");
            validationStatusLabel.setForeground(DANGER_COLOR);
        } else if (!trainValid || !evalValid || !testValid) {
            validationStatusLabel.setText("⚠️ One or more splits are too small (minimum 5%)");
            validationStatusLabel.setForeground(WARNING_COLOR);
        } else {
            validationStatusLabel.setText("✅ Balanced distribution across all splits");
            validationStatusLabel.setForeground(SUCCESS_COLOR);
        }
    }

    // Utility methods

    private TitledBorder createTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(223, 225, 229), 1),
            title
        );
        border.setTitleFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        border.setTitleColor(new Color(73, 80, 87));
        return border;
    }

    /**
     * Custom panel for visualizing split ratios as a pie chart.
     */
    private class SplitVisualizationPanel extends JPanel {
        private double trainRatio = 0.7;
        private double evalRatio = 0.2;
        private double testRatio = 0.1;

        public SplitVisualizationPanel() {
            setPreferredSize(new Dimension(200, 200));
            setOpaque(false);
        }

        public void updateRatios(double train, double eval, double test) {
            this.trainRatio = train;
            this.evalRatio = eval;
            this.testRatio = test;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int size = Math.min(getWidth(), getHeight()) - 20;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            
            // Draw pie slices
            int startAngle = 0;
            
            // Training slice
            int trainAngle = (int) Math.round(trainRatio * 360);
            g2d.setColor(TRAIN_COLOR);
            g2d.fillArc(x, y, size, size, startAngle, trainAngle);
            startAngle += trainAngle;
            
            // Evaluation slice
            int evalAngle = (int) Math.round(evalRatio * 360);
            g2d.setColor(EVAL_COLOR);
            g2d.fillArc(x, y, size, size, startAngle, evalAngle);
            startAngle += evalAngle;
            
            // Test slice
            int testAngle = 360 - startAngle;
            g2d.setColor(TEST_COLOR);
            g2d.fillArc(x, y, size, size, startAngle, testAngle);
            
            // Draw border
            g2d.setColor(UIManager.getColor("Panel.foreground"));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(x, y, size, size);
            
            g2d.dispose();
        }
    }
}