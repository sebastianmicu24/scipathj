package com.scipath.scipathj.ui.training.wizard;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Step 4: Training Progress Panel.
 * Shows real-time training progress with F1-score curves and live metrics.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 */
public class TrainingProgressStepPanel extends JPanel {

    private static final Color PRIMARY_COLOR = new Color(0, 123, 255);
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
    private static final Color DANGER_COLOR = new Color(220, 53, 69);
    private static final Color TRAIN_COLOR = new Color(54, 162, 235);
    private static final Color EVAL_COLOR = new Color(255, 99, 132);

    private final TrainingWizardState wizardState;
    private final XGBoostTrainingWizardManager wizardManager;

    // Chart components
    private ChartPanel chartPanel;
    private XYSeries trainingSeries;
    private XYSeries evaluationSeries;
    private XYSeriesCollection dataset;

    // Live metrics
    private JLabel currentEpochLabel;
    private JLabel totalEpochsLabel;
    private JLabel trainingF1Label;
    private JLabel evaluationF1Label;
    private JLabel bestEvalF1Label;
    private JLabel timeElapsedLabel;
    private JLabel etaLabel;

    // Progress components
    private JProgressBar epochProgressBar;
    private JLabel progressPercentLabel;

    // Training log
    private JTextArea trainingLogArea;
    private JScrollPane logScrollPane;

    // Control buttons
    private JButton cancelTrainingButton;
    private JLabel trainingStatusLabel;

    // Update timer
    private Timer updateTimer;
    private long trainingStartTime;

    /**
     * Creates the training progress step panel.
     *
     * @param wizardState The shared wizard state
     * @param wizardManager The wizard manager
     */
    public TrainingProgressStepPanel(TrainingWizardState wizardState, 
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
     /**
      * Initialize all components.
      */
     private void initializeComponents() {
         // Initialize chart data
         trainingSeries = new XYSeries("Training F1");
         evaluationSeries = new XYSeries("Evaluation F1");
         dataset = new XYSeriesCollection();
         dataset.addSeries(trainingSeries);
         dataset.addSeries(evaluationSeries);
 
         // Create chart
         JFreeChart chart = ChartFactory.createXYLineChart(
             "Real-time F1-Score Curves",
             "Epochs",
             "F1-Score",
             dataset,
             PlotOrientation.VERTICAL,
             true,
             true,
             false
         );
 
         // Customize chart appearance
         XYPlot plot = chart.getXYPlot();
         XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
         renderer.setSeriesPaint(0, TRAIN_COLOR);
         renderer.setSeriesPaint(1, EVAL_COLOR);
         renderer.setSeriesStroke(0, new BasicStroke(2.0f));
         renderer.setSeriesStroke(1, new BasicStroke(2.0f));
         plot.setRenderer(renderer);
         plot.setBackgroundPaint(UIManager.getColor("Panel.background"));
         plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
         plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
 
         chartPanel = new ChartPanel(chart);
         chartPanel.setPreferredSize(new Dimension(600, 300));
        currentEpochLabel = new JLabel("0");
        totalEpochsLabel = new JLabel("100");
        trainingF1Label = new JLabel("0.000");
        evaluationF1Label = new JLabel("0.000");
        bestEvalF1Label = new JLabel("0.000");
        timeElapsedLabel = new JLabel("00:00");
        etaLabel = new JLabel("--:--");

        // Style metric labels
        Font metricFont = new Font(Font.SANS_SERIF, Font.BOLD, 16);
        currentEpochLabel.setFont(metricFont);
        trainingF1Label.setFont(metricFont);
        trainingF1Label.setForeground(TRAIN_COLOR);
        evaluationF1Label.setFont(metricFont);
        evaluationF1Label.setForeground(EVAL_COLOR);
        bestEvalF1Label.setFont(metricFont);
        bestEvalF1Label.setForeground(SUCCESS_COLOR);

        // Progress bar
        epochProgressBar = new JProgressBar(0, 100);
        epochProgressBar.setStringPainted(true);
        epochProgressBar.setPreferredSize(new Dimension(400, 25));
        epochProgressBar.setForeground(PRIMARY_COLOR);

        progressPercentLabel = new JLabel("0%");
        progressPercentLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        // Training log
        trainingLogArea = new JTextArea(8, 50);
        trainingLogArea.setEditable(false);
        trainingLogArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        trainingLogArea.setBackground(UIManager.getColor("TextArea.background"));
        trainingLogArea.setForeground(UIManager.getColor("TextArea.foreground"));
        trainingLogArea.setText("Training not started...\n");

        logScrollPane = new JScrollPane(trainingLogArea);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Control components
        cancelTrainingButton = new JButton("Cancel Training");
        cancelTrainingButton.setIcon(FontIcon.of(FontAwesomeSolid.STOP, 16));
        styleDangerButton(cancelTrainingButton);
        cancelTrainingButton.setEnabled(false);

        trainingStatusLabel = new JLabel("Ready to start training");
        trainingStatusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        trainingStatusLabel.setForeground(PRIMARY_COLOR);

        // Update timer for live updates
        updateTimer = new Timer(1000, e -> updateLiveMetrics()); // Update every second
    }

    /**
     * Setup the layout structure.
     */
    private void setupLayout() {
        setLayout(new BorderLayout(0, 20));
        setOpaque(false);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setOpaque(false);

        // Chart section
        JPanel chartSection = createChartSection();
        
        // Metrics section
        JPanel metricsSection = createMetricsSection();
        
        // Progress section
        JPanel progressSection = createProgressSection();
        
        // Log section
        JPanel logSection = createLogSection();

        // Top panel with chart and metrics
        JPanel topPanel = new JPanel(new BorderLayout(20, 0));
        topPanel.setOpaque(false);
        topPanel.add(chartSection, BorderLayout.CENTER);
        topPanel.add(metricsSection, BorderLayout.EAST);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(progressSection, BorderLayout.CENTER);
        mainPanel.add(logSection, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        // Status panel at bottom
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        statusPanel.setOpaque(false);
        statusPanel.add(trainingStatusLabel);
        statusPanel.add(cancelTrainingButton);

        add(statusPanel, BorderLayout.SOUTH);
    }

    /**
     * Create the chart section.
     */
    private JPanel createChartSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);
        section.setBorder(createTitledBorder("Real-time F1-Score Curves"));

        section.add(chartPanel, BorderLayout.CENTER);
        return section;
    }

    /**
     * Create the live metrics section.
     */
    private JPanel createMetricsSection() {
        JPanel section = new JPanel(new GridLayout(6, 2, 10, 8));
        section.setOpaque(false);
        section.setBorder(createTitledBorder("Live Metrics"));
        section.setPreferredSize(new Dimension(250, 200));

        // Current epoch
        section.add(createMetricLabel("Current Epoch:"));
        section.add(currentEpochLabel);

        // Training F1
        section.add(createMetricLabel("Training F1:"));
        section.add(trainingF1Label);

        // Evaluation F1
        section.add(createMetricLabel("Eval F1:"));
        section.add(evaluationF1Label);

        // Best evaluation F1
        section.add(createMetricLabel("Best Eval:"));
        section.add(bestEvalF1Label);

        // Time elapsed
        section.add(createMetricLabel("Time Elapsed:"));
        section.add(timeElapsedLabel);

        // ETA
        section.add(createMetricLabel("ETA:"));
        section.add(etaLabel);

        return section;
    }

    /**
     * Create the progress section.
     */
    private JPanel createProgressSection() {
        JPanel section = new JPanel(new BorderLayout(10, 10));
        section.setOpaque(false);
        section.setBorder(createTitledBorder("Training Progress"));
        section.setPreferredSize(new Dimension(800, 80)); // Fixed height to prevent overlap

        // Progress bar with percentage
        JPanel progressPanel = new JPanel(new BorderLayout(10, 0));
        progressPanel.setOpaque(false);
        progressPanel.add(epochProgressBar, BorderLayout.CENTER);
        progressPanel.add(progressPercentLabel, BorderLayout.EAST);

        // Progress details
        JPanel detailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        detailsPanel.setOpaque(false);
        
        JLabel epochLabel = new JLabel("Epoch: ");
        epochLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        detailsPanel.add(epochLabel);
        detailsPanel.add(currentEpochLabel);
        detailsPanel.add(new JLabel(" / "));
        detailsPanel.add(totalEpochsLabel);

        section.add(progressPanel, BorderLayout.CENTER);
        section.add(detailsPanel, BorderLayout.SOUTH);

        return section;
    }

    /**
     * Create the training log section.
     */
    private JPanel createLogSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);
        section.setBorder(createTitledBorder("Training Log"));
        section.setPreferredSize(new Dimension(800, 160)); // Reduced height to prevent overlap

        section.add(logScrollPane, BorderLayout.CENTER);
        return section;
    }

    /**
     * Create a metric label with consistent styling.
     */
    private JLabel createMetricLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        label.setForeground(new Color(73, 80, 87));
        return label;
    }

    /**
     * Setup event handlers.
     */
    private void setupEventHandlers() {
        cancelTrainingButton.addActionListener(e -> cancelTraining());
    }

    /**
     * Cancel the training process.
     */
    private void cancelTraining() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to cancel the training?\nProgress will be lost.",
            "Cancel Training",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            stopTraining();
            appendToLog("Training cancelled by user.");
            trainingStatusLabel.setText("Training cancelled");
            trainingStatusLabel.setForeground(WARNING_COLOR);
        }
    }

    /**
     * Start monitoring training progress.
     */
    public void startTraining() {
        trainingStartTime = System.currentTimeMillis();
        wizardState.setTrainingStartTime(trainingStartTime);
        
        clearChart();
        clearLog();
        
        cancelTrainingButton.setEnabled(true);
        trainingStatusLabel.setText("Training in progress...");
        trainingStatusLabel.setForeground(SUCCESS_COLOR);
        
        appendToLog("Starting XGBoost training...");
        appendToLog("Configuration: " + wizardState.getSelectedFeatures().size() + " features, " +
                   wizardState.getTotalSamples() + " samples");
        
        // Explain training approach for different scenarios
        if (wizardState.getTotalSamples() <= 10) {
            appendToLog("Note: Small dataset detected. Training metrics may be simulated for demonstration.");
            appendToLog("In production, XGBoost requires larger datasets for meaningful results.");
        }
        
        int numClasses = wizardState.getClassDistribution().size();
        if (numClasses > 2) {
            appendToLog("Multi-class classification (" + numClasses + " classes) using multi:softprob objective.");
            appendToLog("Training will provide confidence scores for each class prediction.");
        } else if (numClasses == 2) {
            appendToLog("Binary classification using binary:logistic objective.");
        }
        
        updateTimer.start();
        
        // Start actual training through wizard manager
        wizardManager.startTraining();
    }

    /**
     * Stop training and clean up.
     */
    private void stopTraining() {
        updateTimer.stop();
        cancelTrainingButton.setEnabled(false);
        wizardState.setTrainingInProgress(false);
    }

    /**
     * Update live metrics display.
     */
    private void updateLiveMetrics() {
        if (!wizardState.isTrainingInProgress() && !wizardState.isTrainingCompleted()) {
            return;
        }

        // Update epoch information
        int currentEpoch = wizardState.getCurrentEpoch();
        int totalEpochs = wizardState.getTotalEpochs();
        
        // Show final epoch when training is completed
        if (wizardState.isTrainingCompleted() && currentEpoch == 0) {
            currentEpoch = totalEpochs;
        }
        
        currentEpochLabel.setText(String.valueOf(currentEpoch));
        totalEpochsLabel.setText(String.valueOf(totalEpochs));

        // Update progress bar
        int progress = totalEpochs > 0 ? (currentEpoch * 100) / totalEpochs : 0;
        epochProgressBar.setValue(progress);
        epochProgressBar.setString(currentEpoch + " / " + totalEpochs);
        progressPercentLabel.setText(progress + "%");

        // Update F1 scores
        List<Double> trainScores = wizardState.getTrainingF1Scores();
        List<Double> evalScores = wizardState.getEvaluationF1Scores();
        
        DecimalFormat df = new DecimalFormat("0.000");
        
        if (!trainScores.isEmpty()) {
            double latestTrainF1 = trainScores.get(trainScores.size() - 1);
            trainingF1Label.setText(df.format(latestTrainF1));
        }
        
        if (!evalScores.isEmpty()) {
            double latestEvalF1 = evalScores.get(evalScores.size() - 1);
            evaluationF1Label.setText(df.format(latestEvalF1));
        }
        
        bestEvalF1Label.setText(df.format(wizardState.getBestEvaluationF1()));

        // Update time information
        long elapsed = System.currentTimeMillis() - trainingStartTime;
        timeElapsedLabel.setText(formatTime(elapsed));
        
        if (currentEpoch > 0 && totalEpochs > currentEpoch) {
            long avgTimePerEpoch = elapsed / currentEpoch;
            long remainingEpochs = totalEpochs - currentEpoch;
            long eta = avgTimePerEpoch * remainingEpochs;
            etaLabel.setText(formatTime(eta));
        }

        // Update chart
        updateChart();
        
        // Add log entries for new epochs
        if (currentEpoch > 0 && currentEpoch <= trainScores.size()) {
            String logEntry = String.format("[%d] train-f1:%.3f  eval-f1:%.3f", 
                currentEpoch, 
                trainScores.get(currentEpoch - 1),
                evalScores.size() >= currentEpoch ? evalScores.get(currentEpoch - 1) : 0.0);
            appendToLog(logEntry);
        }

        // Check if training completed
        if (wizardState.isTrainingCompleted()) {
            completeTraining();
        }
    }

    /**
     * Update the chart with latest data.
     */
    private void updateChart() {
        trainingSeries.clear();
        evaluationSeries.clear();
        
        List<Double> trainScores = wizardState.getTrainingF1Scores();
        List<Double> evalScores = wizardState.getEvaluationF1Scores();
        
        for (int i = 0; i < trainScores.size(); i++) {
            trainingSeries.add(i + 1, trainScores.get(i));
        }
        
        for (int i = 0; i < evalScores.size(); i++) {
            evaluationSeries.add(i + 1, evalScores.get(i));
        }
        
        chartPanel.repaint();
    }

    /**
     * Complete training process.
     */
    private void completeTraining() {
        stopTraining();
        
        appendToLog("Training completed successfully!");
        appendToLog(String.format("Final training F1: %.3f", wizardState.getFinalTrainingF1()));
        appendToLog(String.format("Final evaluation F1: %.3f", wizardState.getFinalEvaluationF1()));
        appendToLog(String.format("Best evaluation F1: %.3f (epoch %d)",
            wizardState.getBestEvaluationF1(), wizardState.getBestEpoch()));
        
        trainingStatusLabel.setText("Training completed successfully!");
        trainingStatusLabel.setForeground(SUCCESS_COLOR);
        
        // Re-enable navigation now that training is complete
        wizardManager.enableNavigationAfterTraining();
        
        // Show completion notification
        JOptionPane.showMessageDialog(this,
            String.format("Training completed!\n\nFinal Results:\n" +
                         "Training F1: %.3f\n" +
                         "Evaluation F1: %.3f\n" +
                         "Best Evaluation F1: %.3f (epoch %d)",
                         wizardState.getFinalTrainingF1(),
                         wizardState.getFinalEvaluationF1(),
                         wizardState.getBestEvaluationF1(),
                         wizardState.getBestEpoch()),
            "Training Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Clear the chart data.
     */
    private void clearChart() {
        trainingSeries.clear();
        evaluationSeries.clear();
        chartPanel.repaint();
    }

    /**
     * Clear the training log.
     */
    private void clearLog() {
        trainingLogArea.setText("");
    }

    /**
     * Append text to training log.
     */
    private void appendToLog(String text) {
        SwingUtilities.invokeLater(() -> {
            trainingLogArea.append(text + "\n");
            trainingLogArea.setCaretPosition(trainingLogArea.getDocument().getLength());
        });
    }

    /**
     * Format time in milliseconds to MM:SS format.
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Update the entire display.
     */
    public void updateDisplay() {
        if (wizardState.isTrainingInProgress()) {
            if (!updateTimer.isRunning()) {
                updateTimer.start();
                cancelTrainingButton.setEnabled(true);
                trainingStatusLabel.setText("Training in progress...");
                trainingStatusLabel.setForeground(SUCCESS_COLOR);
            }
        } else if (wizardState.isTrainingCompleted()) {
            stopTraining();
            trainingStatusLabel.setText("Training completed");
            trainingStatusLabel.setForeground(SUCCESS_COLOR);
        }
        
        updateLiveMetrics();
    }

    // Utility methods for styling

    private void styleDangerButton(JButton button) {
        button.setBackground(DANGER_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(140, 35));
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