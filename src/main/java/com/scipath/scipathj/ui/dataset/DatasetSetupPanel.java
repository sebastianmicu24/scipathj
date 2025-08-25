package com.scipath.scipathj.ui.dataset;

import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Modern, professional setup panel for dataset creation with enhanced styling.
 * Inspired by modern web design principles with clean layout and professional appearance.
 *
 * @author Sebastian Micu
 * @version 2.0.0
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

    // Theme-aware color scheme that adapts to light/dark themes
    private static Color getPrimaryColor() {
        return new Color(0, 123, 255); // #007bff - blue works in both themes
    }
    
    private static Color getSuccessColor() {
        return new Color(40, 167, 69); // #28a745 - green works in both themes
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
        return new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 64); // Semi-transparent border
    }

    /**
     * Creates a new modern dataset setup panel.
     */
    public DatasetSetupPanel() {
        initializeComponents();
        applyModernStyling();
    }

    /**
     * Initializes the panel components with modern design.
     */
    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBackground(getBackgroundColor());
        setBorder(new EmptyBorder(20, 30, 20, 30));

        // Title with modern styling
        add(createModernTitle(), BorderLayout.NORTH);

        // Main content with card-based design
        add(createModernSetupContent(), BorderLayout.CENTER);

        // Footer with subtle styling
        add(createModernFooter(), BorderLayout.SOUTH);
    }

    /**
     * Creates modern title section.
     */
    private JPanel createModernTitle() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Cell Selection Tool", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(getPrimaryColor());

        titlePanel.add(titleLabel, BorderLayout.CENTER);
        return titlePanel;
    }

    /**
     * Creates the main setup content with modern card-based layout.
     */
    private JPanel createModernSetupContent() {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        // Create main card
        JPanel card = createModernCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Add icon section
        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        iconPanel.setOpaque(false);
        iconPanel.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        FontIcon icon = FontIcon.of(FontAwesomeSolid.PLUS_CIRCLE, 48, getSuccessColor());
        iconPanel.add(new JLabel(icon));
        card.add(iconPanel);

        // Input Files section
        card.add(createInputFilesSection());
        card.add(Box.createVerticalStrut(25));

        // Start button section
        card.add(createStartButtonSection());

        container.add(card, BorderLayout.CENTER);
        return container;
    }

    /**
     * Creates a modern card panel with shadow effect.
     */
    private JPanel createModernCard() {
        JPanel card = new JPanel();
        card.setBackground(getCardColor());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            new EmptyBorder(30, 30, 30, 30)
        ));
        return card;
    }

    /**
     * Creates the input files section.
     */
    private JPanel createInputFilesSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);

        // Section title
        JLabel sectionTitle = new JLabel("Input Files");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        sectionTitle.setForeground(getPrimaryColor());
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(sectionTitle);
        section.add(Box.createVerticalStrut(20));

        // ROI ZIP file selection
        JPanel roiZipPanel = createModernFileInput(
            "Choose Cell Data Folder",
            "ZIP file containing ROIs (each image is a nested ZIP with .roi files)",
            roiZipPathField = new JTextField(),
            browseRoiZipButton = createModernButton("Browse ZIP", getSuccessColor(), e -> browseRoiZip())
        );
        section.add(roiZipPanel);
        section.add(Box.createVerticalStrut(25));

        // Image folder selection
        JPanel imageFolderPanel = createModernFileInput(
            "Choose Image Folder",
            "Folder containing the images corresponding to the ROIs",
            imageFolderPathField = new JTextField(),
            browseImageFolderButton = createModernButton("Browse Folder", getSuccessColor(), e -> browseImageFolder())
        );
        section.add(imageFolderPanel);

        return section;
    }

    /**
     * Creates the start button section.
     */
    private JPanel createStartButtonSection() {
        JPanel section = new JPanel(new FlowLayout(FlowLayout.CENTER));
        section.setOpaque(false);
        section.setBorder(new EmptyBorder(10, 0, 0, 0));

        startClassificationButton = createProminentButton("Start Classification", e -> startClassification());
        startClassificationButton.setEnabled(false);

        section.add(startClassificationButton);
        return section;
    }
    
    /**
     * Creates a prominent, eye-catching button for the main action.
     */
    private JButton createProminentButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        
        // Use a vibrant gradient-like color
        Color primaryColor = new Color(0, 123, 255);
        Color hoverColor = new Color(0, 86, 179);
        
        button.setBackground(primaryColor);
        button.setPreferredSize(new Dimension(250, 50));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(primaryColor.darker(), 1),
            new EmptyBorder(12, 24, 12, 24)
        ));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Enhanced hover effect with scaling
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor);
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(primaryColor);
            }
            
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor.darker());
            }
            
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                button.setBackground(button.contains(evt.getPoint()) ? hoverColor : primaryColor);
            }
        });
        
        if (action != null) {
            button.addActionListener(action);
        }
        
        return button;
    }

    /**
     * Creates a modern file input panel.
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
            JLabel descLabel = new JLabel(description);
            descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            descLabel.setForeground(getTextSecondaryColor());
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(descLabel);
        }

        panel.add(Box.createVerticalStrut(8));

        // Input panel with text field and button
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);
        inputPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Style text field
        textField.setEditable(false);
        textField.setPreferredSize(new Dimension(350, 40));
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));

        inputPanel.add(textField, BorderLayout.CENTER);
        inputPanel.add(browseButton, BorderLayout.EAST);

        panel.add(inputPanel);
        return panel;
    }

    /**
     * Creates a modern styled button.
     */
    private JButton createModernButton(String text, Color bgColor, ActionListener action) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setBorder(new EmptyBorder(10, 20, 10, 20));
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
        
        if (action != null) {
            button.addActionListener(action);
        }
        
        return button;
    }

    /**
     * Creates the modern footer.
     */
    private JPanel createModernFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(30, 0, 0, 0));

        String footerText = "Set up your dataset by selecting ROI files and corresponding images";
        JLabel footerLabel = new JLabel(footerText, SwingConstants.CENTER);
        footerLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        footerLabel.setForeground(getTextSecondaryColor());

        footer.add(footerLabel, BorderLayout.CENTER);
        return footer;
    }

    /**
     * Applies modern styling to components.
     */
    private void applyModernStyling() {
        // Apply modern look and feel enhancements
        setBackground(getBackgroundColor());
        
        // Set preferred size for better layout
        setPreferredSize(new Dimension(900, 650));
        setMinimumSize(new Dimension(700, 500));
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