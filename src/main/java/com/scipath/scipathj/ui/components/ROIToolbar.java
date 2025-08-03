package com.scipath.scipathj.ui.components;

import com.scipath.scipathj.data.model.UserROI;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Toolbar component for ROI management operations.
 * Provides buttons for creating, selecting, and managing ROIs.
 */
public class ROIToolbar extends JPanel {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ROIToolbar.class);
    
    // UI Components
    private JToggleButton squareROIButton;
    private JToggleButton rectangleROIButton;
    private JButton saveROIsButton;
    private JButton saveAllROIsButton;
    private JButton clearAllButton;
    private JLabel roiCountLabel;
    
    // Button group for ROI creation modes
    private ButtonGroup creationModeGroup;
    
    // Current state
    private String currentImageFileName;
    private boolean roiCreationEnabled = false;
    
    // Listeners
    private final List<ROIToolbarListener> listeners;
    public interface ROIToolbarListener {
        void onROICreationModeChanged(UserROI.ROIType type, boolean enabled);
        void onSaveROIs(String imageFileName, File outputFile);
        void onSaveAllROIs(File outputFile);
        void onClearAllROIs();
    }
    
    
    public ROIToolbar() {
        this.listeners = new ArrayList<>();
        initializeComponents();
        updateButtonStates();
    }
    
    private void initializeComponents() {
        setLayout(new FlowLayout(FlowLayout.LEFT, UIConstants.SMALL_SPACING, UIConstants.SMALL_SPACING));
        setBorder(UIUtils.createTitledBorder("ROI Tools"));
        setOpaque(false);
        
        // Create ROI creation buttons
        createROICreationButtons();
        
        // Add separator
        add(createSeparator());
        
        // Create ROI management buttons
        createROIManagementButtons();
        
        // Add separator
        add(createSeparator());
        
        // Create status label
        createStatusLabel();
        
        // Setup button group
        setupButtonGroup();
    }
    
    private void createROICreationButtons() {
        // Square ROI button
        squareROIButton = createToggleButton(
            FontAwesomeSolid.SQUARE,
            "Square ROI",
            "Create square regions of interest",
            e -> handleROICreationModeChange(UserROI.ROIType.SQUARE, squareROIButton.isSelected())
        );
        add(squareROIButton);
        
        // Rectangle ROI button
        rectangleROIButton = createToggleButton(
            FontAwesomeSolid.SQUARE_FULL,
            "Rectangle ROI",
            "Create rectangular regions of interest",
            e -> handleROICreationModeChange(UserROI.ROIType.RECTANGLE, rectangleROIButton.isSelected())
        );
        add(rectangleROIButton);
    }
    
    private void createROIManagementButtons() {
            // Save ROIs button (current image)
            saveROIsButton = createButton(
                FontAwesomeSolid.DOWNLOAD,
                "Save ROIs",
                "Save ROIs from current image to .roi/.zip file",
                e -> handleSaveROIs()
            );
            add(saveROIsButton);
            
            // Save All ROIs button (all images)
            saveAllROIsButton = createButton(
                FontAwesomeSolid.ARCHIVE,
                "Save All",
                "Save ROIs from all images to master ZIP file",
                e -> handleSaveAllROIs()
            );
            add(saveAllROIsButton);
            
            // Clear all ROIs button
            clearAllButton = createButton(
                FontAwesomeSolid.TRASH,
                "Clear All",
                "Clear all ROIs for current image",
                e -> handleClearAllROIs()
            );
            clearAllButton.setForeground(UIConstants.ERROR_COLOR);
            add(clearAllButton);
        }
    
    private void createStatusLabel() {
        roiCountLabel = UIUtils.createLabel("No ROIs", UIConstants.SMALL_FONT_SIZE, 
                                          UIManager.getColor("Label.disabledForeground"));
        roiCountLabel.setBorder(UIUtils.createPadding(0, UIConstants.MEDIUM_SPACING, 0, 0));
        add(roiCountLabel);
    }
    
    private void setupButtonGroup() {
        creationModeGroup = new ButtonGroup();
        creationModeGroup.add(squareROIButton);
        creationModeGroup.add(rectangleROIButton);
    }
    
    private JToggleButton createToggleButton(FontAwesomeSolid icon, String text, String tooltip, ActionListener action) {
        JToggleButton button = new JToggleButton();
        button.setIcon(UIUtils.createIcon(icon, UIConstants.ICON_SIZE_SMALL));
        button.setText(text);
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.addActionListener(action);
        button.setPreferredSize(new Dimension(120, 32));
        return button;
    }
    
    private JButton createButton(FontAwesomeSolid icon, String text, String tooltip, ActionListener action) {
        JButton button = UIUtils.createButton(text, UIConstants.SMALL_FONT_SIZE, action);
        button.setIcon(UIUtils.createIcon(icon, UIConstants.ICON_SIZE_SMALL));
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(120, 32));
        return button;
    }
    
    private Component createSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 25));
        return separator;
    }
    
    /**
     * Add a listener for toolbar events
     */
    public void addROIToolbarListener(ROIToolbarListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a listener for toolbar events
     */
    public void removeROIToolbarListener(ROIToolbarListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Set the current image filename for ROI operations
     */
    public void setCurrentImage(String imageFileName) {
        this.currentImageFileName = imageFileName;
        updateButtonStates();
        LOGGER.debug("Set current image for ROI toolbar: {}", imageFileName);
    }
    
    /**
     * Update the ROI count display
     */
    public void updateROICount(int totalCount) {
        if (totalCount == 0) {
            roiCountLabel.setText("No ROIs");
        } else {
            roiCountLabel.setText(String.format("%d ROI%s",
                                               totalCount, totalCount == 1 ? "" : "s"));
        }
        
        updateButtonStates();
    }
    
    /**
     * Enable or disable the toolbar
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        
        squareROIButton.setEnabled(enabled);
        rectangleROIButton.setEnabled(enabled);
        saveROIsButton.setEnabled(enabled);
        clearAllButton.setEnabled(enabled);
        
        if (!enabled) {
            // Clear selection when disabled
            creationModeGroup.clearSelection();
            roiCreationEnabled = false;
        }
        
        updateButtonStates();
    }
    
    /**
     * Clear ROI creation mode selection
     */
    public void clearCreationMode() {
        creationModeGroup.clearSelection();
        roiCreationEnabled = false;
        
        // Notify listeners
        listeners.forEach(listener -> {
            try {
                listener.onROICreationModeChanged(null, false);
            } catch (Exception e) {
                LOGGER.error("Error notifying ROI toolbar listener", e);
            }
        });
    }
    
        private void updateButtonStates() {
            boolean hasImage = currentImageFileName != null;
            boolean hasROIs = hasImage && ROIManager.getInstance().hasROIs(currentImageFileName);
            boolean hasAnyROIs = ROIManager.getInstance().getTotalROICount() > 0;
            
            // ROI creation buttons are enabled when we have an image
            squareROIButton.setEnabled(hasImage && isEnabled());
            rectangleROIButton.setEnabled(hasImage && isEnabled());
            
            // Current image management buttons depend on having ROIs for current image
            saveROIsButton.setEnabled(hasROIs && isEnabled());
            clearAllButton.setEnabled(hasROIs && isEnabled());
            
            // Save All button depends on having any ROIs across all images
            saveAllROIsButton.setEnabled(hasAnyROIs && isEnabled());
        }
    
    private void handleROICreationModeChange(UserROI.ROIType type, boolean selected) {
        roiCreationEnabled = selected;
        
        final UserROI.ROIType finalType = selected ? type : null;
        
        LOGGER.debug("ROI creation mode changed: {} (enabled: {})", finalType, selected);
        
        // Notify listeners
        listeners.forEach(listener -> {
            try {
                listener.onROICreationModeChanged(finalType, selected);
            } catch (Exception e) {
                LOGGER.error("Error notifying ROI toolbar listener", e);
            }
        });
    }
    
    
        private void handleSaveROIs() {
            if (currentImageFileName == null) {
                JOptionPane.showMessageDialog(this,
                                            "No image selected.",
                                            "Save ROIs",
                                            JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int roiCount = ROIManager.getInstance().getROICount(currentImageFileName);
            if (roiCount == 0) {
                JOptionPane.showMessageDialog(this,
                                            "No ROIs to save for current image.",
                                            "Save ROIs",
                                            JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            // Determine file type based on ROI count
            boolean useZipFormat = roiCount >= 2;
            String fileExtension = useZipFormat ? ".zip" : ".roi";
            String fileDescription = useZipFormat ? "ImageJ ROI Set files (*.zip)" : "ImageJ ROI files (*.roi)";
            String filterExtension = useZipFormat ? "zip" : "roi";
            
            // Show file chooser
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save ROIs");
            fileChooser.setFileFilter(new FileNameExtensionFilter(fileDescription, filterExtension));
            
            // Suggest filename based on image name and ROI count
            String baseName = currentImageFileName.replaceFirst("[.][^.]+$", ""); // Remove extension
            String suggestedFileName = baseName + "_rois" + fileExtension;
            fileChooser.setSelectedFile(new File(suggestedFileName));
            
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File outputFile = fileChooser.getSelectedFile();
                
                // Ensure correct extension based on ROI count
                String fileName = outputFile.getName().toLowerCase();
                if (useZipFormat && !fileName.endsWith(".zip")) {
                    outputFile = new File(outputFile.getAbsolutePath() + ".zip");
                } else if (!useZipFormat && !fileName.endsWith(".roi")) {
                    outputFile = new File(outputFile.getAbsolutePath() + ".roi");
                }
                
                final String finalImageFileName = currentImageFileName;
                final File finalOutputFile = outputFile;
                
                LOGGER.info("Saving {} ROIs for image '{}' to {} file '{}'",
                           roiCount, finalImageFileName, useZipFormat ? "ZIP" : "ROI", finalOutputFile.getAbsolutePath());
                
                // Notify listeners
                listeners.forEach(listener -> {
                    try {
                        listener.onSaveROIs(finalImageFileName, finalOutputFile);
                    } catch (Exception e) {
                        LOGGER.error("Error notifying ROI toolbar listener", e);
                    }
                });
            }
        }
private void handleSaveAllROIs() {
        int totalROICount = ROIManager.getInstance().getTotalROICount();
        if (totalROICount == 0) {
            JOptionPane.showMessageDialog(this, 
                                        "No ROIs to save from any image.", 
                                        "Save All ROIs", 
                                        JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Show file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save All ROIs");
        fileChooser.setFileFilter(new FileNameExtensionFilter("ZIP files (*.zip)", "zip"));
        
        // Suggest filename
        fileChooser.setSelectedFile(new File("all_rois.zip"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File outputFile = fileChooser.getSelectedFile();
            
            // Ensure .zip extension
            if (!outputFile.getName().toLowerCase().endsWith(".zip")) {
                outputFile = new File(outputFile.getAbsolutePath() + ".zip");
            }
            
            final File finalOutputFile = outputFile;
            
            LOGGER.info("Saving {} ROIs from all images to master ZIP file '{}'",
                       totalROICount, finalOutputFile.getAbsolutePath());
            
            // Notify listeners
            listeners.forEach(listener -> {
                try {
                    listener.onSaveAllROIs(finalOutputFile);
                } catch (Exception e) {
                    LOGGER.error("Error notifying ROI toolbar listener", e);
                }
            });
        }
    }
    
    private void handleClearAllROIs() {
        if (currentImageFileName == null) {
            return;
        }
        
        int roiCount = ROIManager.getInstance().getROICount(currentImageFileName);
        if (roiCount == 0) {
            JOptionPane.showMessageDialog(this, 
                                        "No ROIs to clear for current image.", 
                                        "Clear All ROIs", 
                                        JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this,
                                                 String.format("Clear all %d ROI%s for current image?", 
                                                              roiCount, roiCount == 1 ? "" : "s"),
                                                 "Clear All ROIs",
                                                 JOptionPane.YES_NO_OPTION,
                                                 JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            LOGGER.info("Clearing all {} ROIs for image '{}'", roiCount, currentImageFileName);
            
            // Notify listeners
            listeners.forEach(listener -> {
                try {
                    listener.onClearAllROIs();
                } catch (Exception e) {
                    LOGGER.error("Error notifying ROI toolbar listener", e);
                }
            });
        }
    }
}