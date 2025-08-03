package com.scipath.scipathj.ui.components;

import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Status panel component for displaying application status and progress.
 *
 * <p>This component manages the status label, progress bar, and back button
 * that appear at the bottom of the main window.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class StatusPanel extends JPanel {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusPanel.class);
    
    private final JLabel statusLabel;
    private final JProgressBar progressBar;
    private final JButton backButton;
    
    /**
     * Creates a new StatusPanel instance.
     */
    public StatusPanel() {
        LOGGER.debug("Creating status panel");
        
        // Initialize components
        statusLabel = new JLabel("Select a pipeline to begin");
        progressBar = new JProgressBar(0, 100);
        backButton = UIUtils.createStandardButton("Back",
            FontIcon.of(FontAwesomeSolid.ARROW_LEFT, 16));
        
        setupComponents();
        setupLayout();
        
        LOGGER.debug("Status panel created successfully");
    }
    
    /**
     * Sets up the component properties.
     */
    private void setupComponents() {
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setVisible(false);
        backButton.setVisible(false);
    }
    
    /**
     * Sets up the panel layout.
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createPadding(UIConstants.SMALL_SPACING,
            UIConstants.MEDIUM_SPACING, UIConstants.SMALL_SPACING, UIConstants.MEDIUM_SPACING));
        
        add(statusLabel, BorderLayout.WEST);
        add(progressBar, BorderLayout.CENTER);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.add(backButton);
        add(rightPanel, BorderLayout.EAST);
    }
    
    /**
     * Sets the status text.
     *
     * @param status the status text to display
     */
    public void setStatus(String status) {
        statusLabel.setText(status);
        LOGGER.debug("Status updated: {}", status);
    }
    
    /**
     * Gets the current status text.
     *
     * @return the current status text
     */
    public String getStatus() {
        return statusLabel.getText();
    }
    
    /**
     * Shows the progress bar with the specified progress and message.
     *
     * @param progress the progress value (0-100)
     * @param message the progress message
     */
    public void showProgress(int progress, String message) {
        progressBar.setValue(progress);
        progressBar.setString(message);
        progressBar.setVisible(true);
        LOGGER.debug("Progress updated: {}% - {}", progress, message);
    }
    
    /**
     * Hides the progress bar.
     */
    public void hideProgress() {
        progressBar.setVisible(false);
        LOGGER.debug("Progress bar hidden");
    }
    
    /**
     * Sets the progress bar value.
     *
     * @param progress the progress value (0-100)
     */
    public void setProgress(int progress) {
        progressBar.setValue(progress);
    }
    
    /**
     * Sets the progress bar message.
     *
     * @param message the progress message
     */
    public void setProgressMessage(String message) {
        progressBar.setString(message);
    }
    
    /**
     * Shows the back button.
     */
    public void showBackButton() {
        backButton.setVisible(true);
        LOGGER.debug("Back button shown");
    }
    
    /**
     * Hides the back button.
     */
    public void hideBackButton() {
        backButton.setVisible(false);
        LOGGER.debug("Back button hidden");
    }
    
    /**
     * Sets the back button action listener.
     *
     * @param listener the action listener
     */
    public void setBackButtonListener(ActionListener listener) {
        // Remove existing listeners
        for (ActionListener al : backButton.getActionListeners()) {
            backButton.removeActionListener(al);
        }
        backButton.addActionListener(listener);
    }
    
    /**
     * Gets the back button component.
     *
     * @return the back button
     */
    public JButton getBackButton() {
        return backButton;
    }
    
    /**
     * Gets the progress bar component.
     *
     * @return the progress bar
     */
    public JProgressBar getProgressBar() {
        return progressBar;
    }
    
    /**
     * Gets the status label component.
     *
     * @return the status label
     */
    public JLabel getStatusLabel() {
        return statusLabel;
    }
}