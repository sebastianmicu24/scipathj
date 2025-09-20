package com.scipath.scipathj.ui.training.wizard;

import com.scipath.scipathj.ui.training.XGBoostTrainingWizardPanel;
import com.scipath.scipathj.ui.utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Visual progress indicator for wizard steps.
 * Shows current step and completion status in a professional manner.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 */
public class StepProgressIndicator extends JPanel {

    private static final int CIRCLE_DIAMETER = 30;
    private static final int LINE_WIDTH = 40;
    private static final Color COMPLETED_COLOR = new Color(40, 167, 69); // Green
    private static final Color CURRENT_COLOR = new Color(0, 123, 255); // Blue
    private static final Color PENDING_COLOR = new Color(108, 117, 125); // Gray
    private static final Color LINE_COLOR = new Color(223, 225, 229); // Light gray

    private XGBoostTrainingWizardPanel.WizardStep currentStep;
    private final XGBoostTrainingWizardPanel.WizardStep[] allSteps;

    /**
     * Creates a new step progress indicator.
     */
    public StepProgressIndicator() {
        this.allSteps = XGBoostTrainingWizardPanel.WizardStep.values();
        this.currentStep = allSteps[0]; // Start with first step
        
        setPreferredSize(new Dimension(800, 80));
        setMinimumSize(new Dimension(800, 80));
        setOpaque(false);
        setBorder(new EmptyBorder(15, 20, 15, 20));
    }

    /**
     * Updates the current step and repaints the indicator.
     *
     * @param step The current step
     */
    public void setCurrentStep(XGBoostTrainingWizardPanel.WizardStep step) {
        this.currentStep = step;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int totalWidth = getWidth() - 40; // Account for borders
        int stepWidth = totalWidth / allSteps.length;
        int yCenter = getHeight() / 2;
        
        // Draw connecting lines first
        drawConnectingLines(g2d, stepWidth, yCenter);
        
        // Draw step circles and labels
        for (int i = 0; i < allSteps.length; i++) {
            XGBoostTrainingWizardPanel.WizardStep step = allSteps[i];
            int x = 20 + i * stepWidth + stepWidth / 2;
            
            drawStepCircle(g2d, step, x, yCenter, i);
            drawStepLabel(g2d, step, x, yCenter);
        }
        
        g2d.dispose();
    }

    /**
     * Draw connecting lines between steps.
     */
    private void drawConnectingLines(Graphics2D g2d, int stepWidth, int yCenter) {
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        for (int i = 0; i < allSteps.length - 1; i++) {
            int x1 = 20 + i * stepWidth + stepWidth / 2 + CIRCLE_DIAMETER / 2;
            int x2 = 20 + (i + 1) * stepWidth + stepWidth / 2 - CIRCLE_DIAMETER / 2;
            
            // Determine line color based on completion status
            Color lineColor = getLineColor(i);
            g2d.setColor(lineColor);
            g2d.drawLine(x1, yCenter, x2, yCenter);
        }
    }

    /**
     * Get the color for connecting line based on step completion.
     */
    private Color getLineColor(int lineIndex) {
        XGBoostTrainingWizardPanel.WizardStep leftStep = allSteps[lineIndex];
        return isStepCompleted(leftStep) ? COMPLETED_COLOR : LINE_COLOR;
    }

    /**
     * Draw a step circle with appropriate styling.
     */
    private void drawStepCircle(Graphics2D g2d, XGBoostTrainingWizardPanel.WizardStep step, 
                               int x, int y, int stepIndex) {
        int circleX = x - CIRCLE_DIAMETER / 2;
        int circleY = y - CIRCLE_DIAMETER / 2;
        
        // Determine circle appearance
        Color fillColor;
        Color borderColor;
        String text;
        
        if (isStepCompleted(step)) {
            fillColor = COMPLETED_COLOR;
            borderColor = COMPLETED_COLOR;
            text = "✓";
        } else if (step == currentStep) {
            fillColor = CURRENT_COLOR;
            borderColor = CURRENT_COLOR;
            text = String.valueOf(stepIndex + 1);
        } else {
            fillColor = getBackground() != null ? getBackground() : UIManager.getColor("Panel.background");
            borderColor = PENDING_COLOR;
            text = String.valueOf(stepIndex + 1);
        }
        
        // Draw circle
        g2d.setColor(fillColor);
        g2d.fillOval(circleX, circleY, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
        
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(borderColor);
        g2d.drawOval(circleX, circleY, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
        
        // Draw text/icon
        g2d.setColor(isStepCompleted(step) || step == currentStep ? UIManager.getColor("Panel.background") : PENDING_COLOR);
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 14);
        g2d.setFont(font);
        
        FontMetrics fm = g2d.getFontMetrics();
        int textX = x - fm.stringWidth(text) / 2;
        int textY = y + fm.getAscent() / 2 - 2;
        
        g2d.drawString(text, textX, textY);
    }

    /**
     * Draw step label below the circle.
     */
    private void drawStepLabel(Graphics2D g2d, XGBoostTrainingWizardPanel.WizardStep step, 
                              int x, int y) {
        String label = step.getDisplayName();
        
        // Use smaller font for labels
        Font labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
        g2d.setFont(labelFont);
        
        // Color based on step status
        Color textColor;
        if (step == currentStep) {
            textColor = CURRENT_COLOR;
        } else if (isStepCompleted(step)) {
            textColor = COMPLETED_COLOR;
        } else {
            textColor = PENDING_COLOR;
        }
        
        g2d.setColor(textColor);
        
        FontMetrics fm = g2d.getFontMetrics();
        int textX = x - fm.stringWidth(label) / 2;
        int textY = y + CIRCLE_DIAMETER / 2 + 20;
        
        g2d.drawString(label, textX, textY);
    }

    /**
     * Check if a step is completed.
     */
    private boolean isStepCompleted(XGBoostTrainingWizardPanel.WizardStep step) {
        if (currentStep == null) return false;
        return step.ordinal() < currentStep.ordinal();
    }

    /**
     * Get the current step for external access.
     */
    public XGBoostTrainingWizardPanel.WizardStep getCurrentStep() {
        return currentStep;
    }

    /**
     * Get progress percentage (0-100).
     */
    public int getProgressPercentage() {
        if (currentStep == null) return 0;
        return (int) Math.round((currentStep.ordinal() + 1) * 100.0 / allSteps.length);
    }

    /**
     * Get completion status text.
     */
    public String getProgressText() {
        if (currentStep == null) return "Starting...";
        
        int current = currentStep.ordinal() + 1;
        int total = allSteps.length;
        
        return String.format("Step %d of %d: %s", current, total, currentStep.getDisplayName());
    }

    /**
     * Force a specific step to appear completed (for testing).
     */
    public void markStepCompleted(XGBoostTrainingWizardPanel.WizardStep step, boolean completed) {
        // This could be implemented with a Set<WizardStep> to track manually completed steps
        // For now, completion is based on current step position
        repaint();
    }
}