package com.scipath.scipathj.ui.dataset;

import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Panel for the initial setup phase of dataset creation.
 * Handles file selection (ROI ZIP and image folder).
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class DatasetSetupPanel extends JPanel {

    private ActionListener setupCompleteListener;
    private File selectedRoiZip;
    private File selectedImageFolder;

    // Setup components
    private JTextField roiZipPathField;
    private JTextField imageFolderPathField;
    private JButton browseRoiZipButton;
    private JButton browseImageFolderButton;
    private JButton startClassificationButton;

    /**
     * Creates a new dataset setup panel.
     */
    public DatasetSetupPanel() {
        initializeComponents();
    }

    /**
     * Initializes the panel components.
     */
    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING));

        // Title
        add(UIUtils.createTitleLabel("Dataset Creation - Setup"), BorderLayout.NORTH);

        // Main content
        add(createSetupContent(), BorderLayout.CENTER);

        // Footer
        add(createFooter(), BorderLayout.SOUTH);
    }

    /**
     * Creates the main setup content.
     */
    private JPanel createSetupContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(UIUtils.createPadding(UIConstants.EXTRA_LARGE_SPACING));

        // Icon
        FontIcon icon = FontIcon.of(FontAwesomeSolid.PLUS_CIRCLE, 64, new Color(34, 139, 34));
        content.add(new JLabel(icon), BorderLayout.NORTH);

        // Instructions and file selection
        JPanel centerPanel = UIUtils.createVerticalPanel();
        centerPanel.add(Box.createVerticalStrut(UIConstants.LARGE_SPACING));

        // ROI ZIP selection
        JPanel roiZipPanel = createFileSelectionPanel(
            "Select ROI ZIP File:",
            "ZIP file containing ROIs (each image is a nested ZIP with .roi files)",
            roiZipPathField = new JTextField(30),
            browseRoiZipButton = UIUtils.createButton("Browse ZIP", FontAwesomeSolid.SEARCH, e -> browseRoiZip())
        );
        centerPanel.add(roiZipPanel);
        centerPanel.add(Box.createVerticalStrut(UIConstants.MEDIUM_SPACING));

        // Image folder selection
        JPanel imageFolderPanel = createFileSelectionPanel(
            "Select Images Folder:",
            "Folder containing the images corresponding to the ROIs",
            imageFolderPathField = new JTextField(30),
            browseImageFolderButton = UIUtils.createButton("Browse Folder", FontAwesomeSolid.SEARCH, e -> browseImageFolder())
        );
        centerPanel.add(imageFolderPanel);
        centerPanel.add(Box.createVerticalStrut(UIConstants.EXTRA_LARGE_SPACING));

        // Start button
        startClassificationButton = UIUtils.createButton(
            "Start Classification",
            FontAwesomeSolid.ARROW_RIGHT,
            e -> startClassification()
        );
        startClassificationButton.setPreferredSize(new Dimension(180, 45));
        startClassificationButton.setEnabled(false);
        centerPanel.add(startClassificationButton);

        content.add(centerPanel, BorderLayout.CENTER);
        return content;
    }

    /**
     * Creates a file selection panel with label, text field, and browse button.
     */
    private JPanel createFileSelectionPanel(String labelText, String description,
                                         JTextField textField, JButton browseButton) {
        JPanel panel = new JPanel(new BorderLayout(UIConstants.MEDIUM_SPACING, UIConstants.SMALL_SPACING));

        // Label and description
        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.add(UIUtils.createBoldLabel(labelText, UIConstants.NORMAL_FONT_SIZE), BorderLayout.NORTH);

        if (description != null) {
            JLabel descLabel = UIUtils.createLabel(description, UIConstants.SMALL_FONT_SIZE,
                                                UIManager.getColor("Label.disabledForeground"));
            labelPanel.add(descLabel, BorderLayout.SOUTH);
        }

        panel.add(labelPanel, BorderLayout.NORTH);

        // Text field and button
        JPanel inputPanel = new JPanel(new BorderLayout(UIConstants.SMALL_SPACING, 0));
        textField.setEditable(false);
        inputPanel.add(textField, BorderLayout.CENTER);
        inputPanel.add(browseButton, BorderLayout.EAST);

        panel.add(inputPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates the footer with additional information.
     */
    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING, 0, 0, 0));
        footer.setOpaque(false);

        String footerText = "Set up your dataset by selecting ROI files and corresponding images";
        JLabel footerLabel = new JLabel(footerText);
        footerLabel.setFont(footerLabel.getFont().deriveFont(Font.ITALIC, UIConstants.SMALL_FONT_SIZE));
        footerLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        footerLabel.setHorizontalAlignment(SwingConstants.CENTER);

        footer.add(footerLabel, BorderLayout.CENTER);

        return footer;
    }

    /**
     * Browses for ROI ZIP file.
     */
    private void browseRoiZip() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Select ROI ZIP File");
        fileChooser.setCurrentDirectory(FileSystemView.getFileSystemView().getDefaultDirectory());

        FileNameExtensionFilter filter = new FileNameExtensionFilter("ZIP Files", "zip");
        fileChooser.setFileFilter(filter);

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedRoiZip = fileChooser.getSelectedFile();
            roiZipPathField.setText(selectedRoiZip.getAbsolutePath());
            updateStartButtonState();
        }
    }

    /**
     * Browses for image folder.
     */
    private void browseImageFolder() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Images Folder");
        fileChooser.setCurrentDirectory(FileSystemView.getFileSystemView().getDefaultDirectory());

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedImageFolder = fileChooser.getSelectedFile();
            imageFolderPathField.setText(selectedImageFolder.getAbsolutePath());
            updateStartButtonState();
        }
    }

    /**
     * Updates the start button state based on selections.
     */
    private void updateStartButtonState() {
        boolean canStart = selectedRoiZip != null && selectedImageFolder != null;
        startClassificationButton.setEnabled(canStart);
    }

    /**
     * Starts the classification interface.
     */
    private void startClassification() {
        if (selectedRoiZip != null && selectedImageFolder != null) {
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
        return selectedRoiZip;
    }

    /**
     * Gets the selected image folder.
     */
    public File getSelectedImageFolder() {
        return selectedImageFolder;
    }

    /**
     * Checks if setup is complete.
     */
    public boolean isSetupComplete() {
        return selectedRoiZip != null && selectedImageFolder != null;
    }
}