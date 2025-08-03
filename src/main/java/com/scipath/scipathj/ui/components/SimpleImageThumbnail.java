package com.scipath.scipathj.ui.components;

import com.scipath.scipathj.ui.utils.ImageLoader;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Simplified image thumbnail component for the gallery view.
 * 
 * <p>This component displays a rounded thumbnail of an image file with just
 * the filename below it. It's designed for a clean, minimal gallery interface.</p>
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class SimpleImageThumbnail extends JPanel {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleImageThumbnail.class);
    
    private final File imageFile;
    private JLabel imageLabel;
    private JLabel fileNameLabel;
    private BufferedImage thumbnailImage;
    private boolean isSelected = false;
    private boolean isLoading = false;
    
    /**
     * Creates a new SimpleImageThumbnail for the specified image file.
     * 
     * @param imageFile the image file to display
     */
    public SimpleImageThumbnail(File imageFile) {
        this.imageFile = imageFile;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        loadThumbnailAsync();
    }
    
    /**
     * Initializes the UI components.
     */
    private void initializeComponents() {
        setPreferredSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.PANEL_HEIGHT));
        setMinimumSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.PANEL_HEIGHT));
        setMaximumSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.PANEL_HEIGHT));
        setBorder(UIUtils.createPadding(8));
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        createImageLabel();
        createFileNameLabel();
        showLoadingIndicator();
    }
    
    /**
     * Creates the image display label with rounded corners.
     */
    private void createImageLabel() {
        imageLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                if (getIcon() != null) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    UIUtils.setupRenderingHints(g2d);
                    
                    // Create rounded clipping area
                    g2d.setClip(UIUtils.createRoundedRectangle(0, 0, getWidth(), getHeight(), UIConstants.BORDER_RADIUS));
                    
                    super.paintComponent(g2d);
                    g2d.dispose();
                } else {
                    super.paintComponent(g);
                }
            }
        };
        
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(UIConstants.THUMBNAIL_SIZE, UIConstants.THUMBNAIL_SIZE));
        imageLabel.setOpaque(true);
        imageLabel.setBackground(Color.LIGHT_GRAY);
    }
    
    /**
     * Creates the file name label with truncated text.
     */
    private void createFileNameLabel() {
        String fileName = truncateFileName(imageFile.getName());
        
        fileNameLabel = new JLabel(fileName);
        fileNameLabel.setFont(fileNameLabel.getFont().deriveFont(UIConstants.TINY_FONT_SIZE));
        fileNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        fileNameLabel.setForeground(UIManager.getColor("Label.foreground"));
    }
    
    /**
     * Truncates the file name for display.
     */
    private String truncateFileName(String fileName) {
        // Remove extension for cleaner display
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            fileName = fileName.substring(0, lastDot);
        }
        // Truncate if too long
        if (fileName.length() > 15) {
            fileName = fileName.substring(0, 12) + "...";
        }
        return fileName;
    }
    
    /**
     * Sets up the layout of components.
     */
    private void setupLayout() {
        setLayout(new BorderLayout(0, UIConstants.SMALL_SPACING + 1));
        add(imageLabel, BorderLayout.CENTER);
        add(fileNameLabel, BorderLayout.SOUTH);
    }
    
    /**
     * Sets up event handlers.
     */
    private void setupEventHandlers() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setSelected(!isSelected);
                // Fire property change for selection
                firePropertyChange("thumbnailSelected", null, SimpleImageThumbnail.this);
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected) {
                    imageLabel.setBorder(BorderFactory.createLineBorder(UIUtils.getHoverColor(), 2));
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (!isSelected) {
                    imageLabel.setBorder(null);
                }
            }
        };
        
        addMouseListener(mouseHandler);
        imageLabel.addMouseListener(mouseHandler);
    }
    
    /**
     * Shows a loading indicator while the thumbnail is being generated.
     */
    private void showLoadingIndicator() {
        isLoading = true;
        imageLabel.setText("Loading...");
        imageLabel.setIcon(null);
        imageLabel.setBackground(Color.LIGHT_GRAY);
    }
    
    /**
     * Shows an error indicator when thumbnail loading fails.
     */
    private void showErrorIndicator() {
        isLoading = false;
        imageLabel.setText("Error");
        imageLabel.setIcon(null);
        imageLabel.setBackground(Color.PINK);
    }
    
    /**
     * Loads the thumbnail image asynchronously.
     */
    private void loadThumbnailAsync() {
        CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.debug("Loading thumbnail for: {}", imageFile.getName());
                return ImageLoader.createThumbnail(imageFile.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.warn("Failed to create thumbnail for: {}", imageFile.getName(), e);
                return null;
            }
        }).thenAccept(thumbnail -> {
            SwingUtilities.invokeLater(() -> {
                if (thumbnail != null) {
                    setThumbnailImage(thumbnail);
                } else {
                    showErrorIndicator();
                }
            });
        }).exceptionally(throwable -> {
            LOGGER.error("Error in thumbnail loading task for: {}", imageFile.getName(), throwable);
            SwingUtilities.invokeLater(this::showErrorIndicator);
            return null;
        });
    }
    
    /**
     * Sets the thumbnail image and updates the display.
     * 
     * @param thumbnail the thumbnail image
     */
    private void setThumbnailImage(BufferedImage thumbnail) {
        this.thumbnailImage = thumbnail;
        this.isLoading = false;
        
        // Scale the image to fit the thumbnail size while maintaining aspect ratio
        Image scaledImage = thumbnail.getScaledInstance(UIConstants.THUMBNAIL_SIZE, UIConstants.THUMBNAIL_SIZE, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.setText("");
        imageLabel.setBackground(null);
        
        LOGGER.debug("Thumbnail loaded successfully for: {}", imageFile.getName());
    }
    
    /**
     * Sets the selection state of this thumbnail.
     * 
     * @param selected true to select, false to deselect
     */
    public void setSelected(boolean selected) {
        if (this.isSelected != selected) {
            this.isSelected = selected;
            updateAppearance();
        }
    }
    
    /**
     * Gets the selection state of this thumbnail.
     * 
     * @return true if selected
     */
    public boolean isSelected() {
        return isSelected;
    }
    
    /**
     * Gets the image file associated with this thumbnail.
     * 
     * @return the image file
     */
    public File getImageFile() {
        return imageFile;
    }
    
    /**
     * Gets the thumbnail image.
     * 
     * @return the thumbnail image, or null if not loaded
     */
    public BufferedImage getThumbnailImage() {
        return thumbnailImage;
    }
    
    /**
     * Updates the visual appearance based on selection state.
     */
    private void updateAppearance() {
        if (isSelected) {
            imageLabel.setBorder(BorderFactory.createLineBorder(UIConstants.SELECTION_COLOR, 3));
            fileNameLabel.setForeground(UIConstants.SELECTION_COLOR);
        } else {
            imageLabel.setBorder(null);
            fileNameLabel.setForeground(UIManager.getColor("Label.foreground"));
        }
        repaint();
    }
}