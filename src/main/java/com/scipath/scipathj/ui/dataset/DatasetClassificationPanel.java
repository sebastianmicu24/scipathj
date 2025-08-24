package com.scipath.scipathj.ui.dataset;

import com.scipath.scipathj.ui.common.SimpleImageGallery;
import com.scipath.scipathj.ui.common.MainImageViewer;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.io.File;
import javax.swing.*;

/**
 * Panel for the classification phase of dataset creation.
 * Handles image display, ROI overlay, and class assignment.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class DatasetClassificationPanel extends JPanel {

    private SimpleImageGallery imageGallery;
    private MainImageViewer mainImageViewer;
    private DatasetClassManager classManager;

    /**
     * Creates a new dataset classification panel.
     */
    public DatasetClassificationPanel() {
        initializeComponents();
    }

    /**
     * Initializes the panel components.
     */
    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING));

        // Title
        add(UIUtils.createTitleLabel("Dataset Creation - Classification"), BorderLayout.NORTH);

        // Class management panel at top
        classManager = new DatasetClassManager();
        add(classManager, BorderLayout.NORTH);

        // Main content area
        add(createMainContent(), BorderLayout.CENTER);

        // Footer
        add(createFooter(), BorderLayout.SOUTH);
    }

    /**
     * Creates the main content area with image gallery and viewer.
     */
    private JPanel createMainContent() {
        JPanel mainContent = new JPanel(new BorderLayout());

        // Image gallery on the left
        imageGallery = new SimpleImageGallery();
        mainContent.add(imageGallery, BorderLayout.WEST);

        // Main image viewer on the right
        mainImageViewer = new MainImageViewer();
        mainContent.add(mainImageViewer, BorderLayout.CENTER);

        return mainContent;
    }

    /**
     * Creates the footer with instructions.
     */
    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING, 0, 0, 0));
        footer.setOpaque(false);

        String footerText = "Click on ROIs to assign them to the selected class • Use class manager to add/remove classes";
        if (imageGallery.getSelectedImageFile() != null) {
            footerText += " • Current image: " + imageGallery.getSelectedImageFile().getName();
        }

        JLabel footerLabel = new JLabel(footerText);
        footerLabel.setFont(footerLabel.getFont().deriveFont(Font.ITALIC, UIConstants.SMALL_FONT_SIZE));
        footerLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        footerLabel.setHorizontalAlignment(SwingConstants.CENTER);

        footer.add(footerLabel, BorderLayout.CENTER);

        return footer;
    }

    /**
     * Loads images from the specified folder.
     */
    public void loadImagesFromFolder(File imageFolder) {
        if (imageGallery != null) {
            imageGallery.loadImagesFromFolder(imageFolder);

            // Set up image selection listener
            imageGallery.setSelectionChangeListener(e -> {
                File selectedImage = imageGallery.getSelectedImageFile();
                if (selectedImage != null && mainImageViewer != null) {
                    mainImageViewer.displayImage(selectedImage);
                    // TODO: Load ROIs for this image
                }
            });
        }
    }

    /**
     * Gets the image gallery component.
     */
    public SimpleImageGallery getImageGallery() {
        return imageGallery;
    }

    /**
     * Gets the main image viewer component.
     */
    public MainImageViewer getMainImageViewer() {
        return mainImageViewer;
    }

    /**
     * Gets the class manager component.
     */
    public DatasetClassManager getClassManager() {
        return classManager;
    }

    /**
     * Gets the currently selected image file.
     */
    public File getSelectedImageFile() {
        return imageGallery != null ? imageGallery.getSelectedImageFile() : null;
    }

    /**
     * Gets the currently selected class name.
     */
    public String getSelectedClassName() {
        return classManager != null ? classManager.getSelectedClassName() : null;
    }

    /**
     * Adds a new class for classification.
     */
    public void addClass(String className) {
        if (classManager != null) {
            classManager.addClass(className);
        }
    }

    /**
     * Removes a class.
     */
    public void removeClass(String className) {
        if (classManager != null) {
            classManager.removeClass(className);
        }
    }

    /**
     * Gets all available class names.
     */
    public java.util.List<String> getClassNames() {
        return classManager != null ? classManager.getClassNames() : java.util.Collections.emptyList();
    }
}