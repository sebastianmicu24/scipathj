package com.scipath.scipathj.ui.training.wizard;

import com.scipath.scipathj.ui.utils.UIUtils;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Navigation control panel for wizard steps.
 * Provides Previous/Next/Cancel buttons and step indicator.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 */
public class NavigationControlPanel extends JPanel {

    private static final Color PRIMARY_COLOR = new Color(0, 123, 255);
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color SECONDARY_COLOR = new Color(108, 117, 125);

    // Navigation buttons
    private JButton previousButton;
    private JButton nextButton;
    private JButton cancelButton;
    
    // Step indicator
    private JLabel stepIndicatorLabel;
    
    // Control flags
    private boolean navigationEnabled = true;

    /**
     * Creates a new navigation control panel.
     */
    public NavigationControlPanel() {
        initializeComponents();
        setupLayout();
        setupStyling();
    }

    /**
     * Initialize all components.
     */
    private void initializeComponents() {
        // Previous button
        previousButton = new JButton("< Previous");
        previousButton.setIcon(FontIcon.of(FontAwesomeSolid.ARROW_LEFT, 14));
        previousButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        previousButton.setIconTextGap(8);
        
        // Next button
        nextButton = new JButton("Next >");
        nextButton.setIcon(FontIcon.of(FontAwesomeSolid.ARROW_RIGHT, 14));
        nextButton.setHorizontalTextPosition(SwingConstants.LEFT);
        nextButton.setIconTextGap(8);
        
        // Cancel button
        cancelButton = new JButton("Cancel");
        cancelButton.setIcon(FontIcon.of(FontAwesomeSolid.TIMES, 14));
        cancelButton.setIconTextGap(8);
        
        // Step indicator
        stepIndicatorLabel = new JLabel("Step 1 of 6", SwingConstants.CENTER);
    }

    /**
     * Setup the layout structure.
     */
    private void setupLayout() {
        setLayout(new BorderLayout(15, 0));
        setBorder(new EmptyBorder(15, 20, 15, 20));
        setOpaque(true);
        setBackground(UIManager.getColor("Panel.background"));
        
        // Left side: Cancel button
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(cancelButton);
        
        // Center: Step indicator
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerPanel.setOpaque(false);
        centerPanel.add(stepIndicatorLabel);
        
        // Right side: Previous and Next buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(previousButton);
        rightPanel.add(nextButton);
        
        add(leftPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    /**
     * Setup styling for all components.
     */
    private void setupStyling() {
        // Button dimensions
        Dimension buttonSize = new Dimension(120, 35);
        
        // Previous button styling
        previousButton.setPreferredSize(buttonSize);
        styleSecondaryButton(previousButton);
        
        // Next button styling (primary)
        nextButton.setPreferredSize(buttonSize);
        stylePrimaryButton(nextButton);
        
        // Cancel button styling
        cancelButton.setPreferredSize(new Dimension(100, 35));
        styleSecondaryButton(cancelButton);
        
        // Step indicator styling
        stepIndicatorLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        stepIndicatorLabel.setForeground(SECONDARY_COLOR);
        
        // Initial state
        previousButton.setEnabled(false); // Disabled on first step
    }

    /**
     * Apply primary button styling (for Next/Finish actions).
     */
    private void stylePrimaryButton(JButton button) {
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(PRIMARY_COLOR.darker());
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(PRIMARY_COLOR);
                }
            }
        });
    }

    /**
     * Apply secondary button styling (for Previous/Cancel actions).
     */
    private void styleSecondaryButton(JButton button) {
        button.setBackground(UIManager.getColor("Button.background"));
        button.setForeground(SECONDARY_COLOR);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(248, 249, 250));
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(UIManager.getColor("Button.background"));
                }
            }
        });
    }

    /**
     * Apply success button styling (for completion actions).
     */
    private void styleSuccessButton(JButton button) {
        button.setBackground(SUCCESS_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(SUCCESS_COLOR.darker());
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(SUCCESS_COLOR);
                }
            }
        });
    }

    // Public API methods for wizard panel

    /**
     * Set the action for the previous button.
     */
    public void setPreviousButtonAction(ActionListener action) {
        // Remove existing listeners
        for (ActionListener listener : previousButton.getActionListeners()) {
            previousButton.removeActionListener(listener);
        }
        previousButton.addActionListener(action);
    }

    /**
     * Set the action for the next button.
     */
    public void setNextButtonAction(ActionListener action) {
        // Remove existing listeners
        for (ActionListener listener : nextButton.getActionListeners()) {
            nextButton.removeActionListener(listener);
        }
        nextButton.addActionListener(action);
    }

    /**
     * Set the action for the cancel button.
     */
    public void setCancelButtonAction(ActionListener action) {
        // Remove existing listeners
        for (ActionListener listener : cancelButton.getActionListeners()) {
            cancelButton.removeActionListener(listener);
        }
        cancelButton.addActionListener(action);
    }

    /**
     * Enable or disable the previous button.
     */
    public void setPreviousButtonEnabled(boolean enabled) {
        previousButton.setEnabled(enabled && navigationEnabled);
    }

    /**
     * Enable or disable the next button.
     */
    public void setNextButtonEnabled(boolean enabled) {
        nextButton.setEnabled(enabled && navigationEnabled);
    }

    /**
     * Set the text for the next button.
     */
    public void setNextButtonText(String text) {
        nextButton.setText(text);
        
        // Update icon based on text
        if (text.contains("Finish") || text.contains("Complete")) {
            nextButton.setIcon(FontIcon.of(FontAwesomeSolid.CHECK, 14));
            styleSuccessButton(nextButton);
        } else if (text.contains("Training") || text.contains("Start")) {
            nextButton.setIcon(FontIcon.of(FontAwesomeSolid.PLAY, 14));
            stylePrimaryButton(nextButton);
        } else if (text.contains("Working") || text.contains("Progress")) {
            nextButton.setIcon(FontIcon.of(FontAwesomeSolid.SPINNER, 14));
            nextButton.setEnabled(false);
        } else {
            nextButton.setIcon(FontIcon.of(FontAwesomeSolid.ARROW_RIGHT, 14));
            stylePrimaryButton(nextButton);
        }
        
        nextButton.setHorizontalTextPosition(SwingConstants.LEFT);
        nextButton.setIconTextGap(8);
    }

    /**
     * Set the step indicator text.
     */
    public void setStepIndicatorText(String text) {
        stepIndicatorLabel.setText(text);
    }

    /**
     * Enable or disable all navigation.
     */
    public void setNavigationEnabled(boolean enabled) {
        this.navigationEnabled = enabled;
        
        previousButton.setEnabled(enabled && previousButton.isEnabled());
        nextButton.setEnabled(enabled && nextButton.isEnabled());
        cancelButton.setEnabled(enabled);
        
        // Visual feedback for disabled state
        if (!enabled) {
            stepIndicatorLabel.setForeground(SECONDARY_COLOR.brighter());
        } else {
            stepIndicatorLabel.setForeground(SECONDARY_COLOR);
        }
    }

    /**
     * Get current navigation enabled state.
     */
    public boolean isNavigationEnabled() {
        return navigationEnabled;
    }

    /**
     * Get reference to buttons for advanced customization.
     */
    public JButton getPreviousButton() {
        return previousButton;
    }

    public JButton getNextButton() {
        return nextButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JLabel getStepIndicatorLabel() {
        return stepIndicatorLabel;
    }

    /**
     * Reset to initial state.
     */
    public void reset() {
        setPreviousButtonEnabled(false);
        setNextButtonEnabled(true);
        setNextButtonText("Next >");
        setStepIndicatorText("Step 1 of 6");
        setNavigationEnabled(true);
    }

    /**
     * Set working state (during training).
     */
    public void setWorkingState(boolean working) {
        if (working) {
            setNextButtonText("Working...");
            setNavigationEnabled(false);
        } else {
            setNavigationEnabled(true);
        }
    }
}