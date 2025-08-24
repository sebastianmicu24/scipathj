package com.scipath.scipathj.ui.dataset;

import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.*;

/**
 * Main panel for dataset creation functionality.
 * Orchestrates the setup and classification phases using smaller, focused components.
 *
 * <p>This replaces the old monolithic DatasetCreationPanel (897 lines) with a clean,
 * modular architecture that follows single responsibility principle.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class DatasetMainPanel extends JPanel {

    private enum PanelState {
        SETUP, CLASSIFICATION
    }

    private PanelState currentState = PanelState.SETUP;
    private ActionListener setupCompleteListener;

    // Sub-components
    private DatasetSetupPanel setupPanel;
    private DatasetClassificationPanel classificationPanel;

    /**
     * Creates a new dataset main panel.
     */
    public DatasetMainPanel() {
        initializeComponents();
        showSetupPanel();
    }

    /**
     * Initializes the panel components.
     */
    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING));

        // Create sub-panels
        setupPanel = new DatasetSetupPanel();
        setupPanel.setSetupCompleteListener(e -> startClassification());

        classificationPanel = new DatasetClassificationPanel();
    }

    /**
     * Shows the setup panel.
     */
    private void showSetupPanel() {
        currentState = PanelState.SETUP;
        removeAll();

        add(setupPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    /**
     * Shows the classification panel.
     */
    private void showClassificationPanel() {
        currentState = PanelState.CLASSIFICATION;
        removeAll();

        add(classificationPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    /**
     * Starts the classification phase.
     */
    private void startClassification() {
        if (setupPanel.isSetupComplete()) {
            // Load images into classification panel
            File imageFolder = setupPanel.getSelectedImageFolder();
            if (imageFolder != null) {
                classificationPanel.loadImagesFromFolder(imageFolder);
            }

            // Switch to classification view
            showClassificationPanel();

            // Notify listener if set
            if (setupCompleteListener != null) {
                setupCompleteListener.actionPerformed(null);
            }
        }
    }

    /**
     * Sets the setup complete listener.
     */
    public void setSetupCompleteListener(ActionListener listener) {
        this.setupCompleteListener = listener;
    }

    /**
     * Gets the selected ROI ZIP file.
     */
    public File getSelectedRoiZip() {
        return setupPanel != null ? setupPanel.getSelectedRoiZip() : null;
    }

    /**
     * Gets the selected image folder.
     */
    public File getSelectedImageFolder() {
        return setupPanel != null ? setupPanel.getSelectedImageFolder() : null;
    }

    /**
     * Gets the current panel state.
     */
    public PanelState getCurrentState() {
        return currentState;
    }

    /**
     * Checks if setup is complete.
     */
    public boolean isSetupComplete() {
        return setupPanel != null && setupPanel.isSetupComplete();
    }

    /**
     * Gets the currently selected image file in classification mode.
     */
    public File getSelectedImageFile() {
        return currentState == PanelState.CLASSIFICATION && classificationPanel != null
            ? classificationPanel.getSelectedImageFile()
            : null;
    }

    /**
     * Gets the currently selected class name in classification mode.
     */
    public String getSelectedClassName() {
        return currentState == PanelState.CLASSIFICATION && classificationPanel != null
            ? classificationPanel.getSelectedClassName()
            : null;
    }

    /**
     * Adds a new class for classification.
     */
    public void addClass(String className) {
        if (classificationPanel != null) {
            classificationPanel.addClass(className);
        }
    }

    /**
     * Removes a class.
     */
    public void removeClass(String className) {
        if (classificationPanel != null) {
            classificationPanel.removeClass(className);
        }
    }

    /**
     * Gets all available class names.
     */
    public java.util.List<String> getClassNames() {
        return classificationPanel != null
            ? classificationPanel.getClassNames()
            : java.util.Collections.emptyList();
    }

    /**
     * Resets the panel to initial state.
     */
    public void reset() {
        currentState = PanelState.SETUP;
        if (setupPanel != null) {
            // Note: SetupPanel would need a reset method
        }
        showSetupPanel();
    }
}