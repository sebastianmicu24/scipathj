package com.scipath.scipathj.ui.dataset;

import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.ui.themes.ThemeManager;
import com.scipath.scipathj.ui.utils.ImageLoader;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import ij.ImagePlus;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Streamlined dataset image viewer with zoom/pan and integrated NewDatasetROIOverlay.
 * Designed specifically for dataset creation workflow with high-performance ROI display.
 * 
 * @author Sebastian Micu
 * @version 4.0.0
 */
public class DatasetImageViewer extends JPanel {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetImageViewer.class);

    // Zoom functionality
    private static final double MIN_ZOOM = 0.1; // 10%
    private static final double MAX_ZOOM = 4.0; // 400%
    private static final double ZOOM_STEP = 0.1;
    private double currentZoom = 1.0;

    // Core components
    private JLabel imageLabel;
    private JLabel imageInfoLabel;
    private JScrollPane scrollPane;
    private NewDatasetROIOverlay roiOverlay;
    private JLayeredPane layeredPane;
    private File currentImageFile;
    private ImagePlus currentImagePlus;
    private boolean isLoading = false;

    // Image display properties
    private Image originalImage;
    private int originalImageWidth;
    private int originalImageHeight;

    // Zoom controls
    private JPanel zoomControlPanel;
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JButton zoomFitButton;
    private JButton zoom100Button;
    private JLabel zoomLabel;
    private JSlider zoomSlider;

    // Performance optimization
    private boolean updatePending = false;
    private long lastUpdateTime = 0;
    private static final long UPDATE_THROTTLE_MS = 16; // ~60fps

    public DatasetImageViewer() {
        initializeComponents();
        setupROISystem();
        showEmptyState();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING));
        setOpaque(false);

        imageLabel = createImageLabel();
        roiOverlay = new NewDatasetROIOverlay();
        layeredPane = createLayeredPane();
        scrollPane = createScrollPane();
        imageInfoLabel = createInfoLabel();
        zoomControlPanel = createZoomControls();

        // Create top panel with zoom controls
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(zoomControlPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(imageInfoLabel, BorderLayout.SOUTH);

        // Add component resize listener
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                LOGGER.debug("DatasetImageViewer resized: {}x{}", getWidth(), getHeight());
                if (imageLabel.getIcon() != null) {
                    updateLayeredPaneLayout((ImageIcon) imageLabel.getIcon());
                }
            }
        });
    }

    private void setupROISystem() {
        LOGGER.debug("Setup streamlined ROI system complete");
    }

    private JLayeredPane createLayeredPane() {
        JLayeredPane pane = new JLayeredPane();
        pane.setPreferredSize(new Dimension(600, 400));

        // Add image label to bottom layer
        imageLabel.setBounds(0, 0, 600, 400);
        pane.add(imageLabel, JLayeredPane.DEFAULT_LAYER);

        // Add streamlined ROI overlay to top layer
        roiOverlay.setBounds(0, 0, 600, 400);
        pane.add(roiOverlay, JLayeredPane.PALETTE_LAYER);

        return pane;
    }

    private JLabel createImageLabel() {
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(
            ThemeManager.isDarkTheme() ? new Color(40, 40, 40) : new Color(250, 250, 250));
        return label;
    }

    private JScrollPane createScrollPane() {
        JScrollPane pane = new JScrollPane(layeredPane);
        pane.setPreferredSize(new Dimension(600, 400));
        pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.getVerticalScrollBar().setUnitIncrement(16);
        pane.getHorizontalScrollBar().setUnitIncrement(16);

        // Add mouse wheel zoom functionality
        pane.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown() && currentImagePlus != null) {
                    Point mousePoint = e.getPoint();
                    if (e.getWheelRotation() < 0) {
                        zoomIn(mousePoint);
                    } else {
                        zoomOut(mousePoint);
                    }
                    e.consume();
                }
            }
        });

        // Add scroll listeners with throttling
        pane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                LOGGER.trace("Vertical scroll: {}", e.getValue());
                throttledROIUpdate();
            }
        });

        pane.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                LOGGER.trace("Horizontal scroll: {}", e.getValue());
                throttledROIUpdate();
            }
        });

        // Add viewport resize listener
        pane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                LOGGER.debug("Viewport resized: {}x{}", 
                    pane.getViewport().getWidth(), pane.getViewport().getHeight());
                if (imageLabel.getIcon() != null) {
                    updateLayeredPaneLayout((ImageIcon) imageLabel.getIcon());
                }
            }
        });

        return pane;
    }

    private JPanel createZoomControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        panel.setOpaque(false);

        // Left group: percentage label and zoom controls
        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        leftGroup.setOpaque(false);

        zoomLabel = new JLabel("100%");
        zoomLabel.setFont(zoomLabel.getFont().deriveFont(Font.PLAIN, UIConstants.SMALL_FONT_SIZE));
        zoomLabel.setPreferredSize(new Dimension(45, 30));
        zoomLabel.setHorizontalAlignment(SwingConstants.CENTER);
        leftGroup.add(zoomLabel);

        zoomOutButton = UIUtils.createButton("", FontAwesomeSolid.SEARCH_MINUS, e -> zoomOut(null));
        zoomOutButton.setPreferredSize(new Dimension(28, 28));
        zoomOutButton.setToolTipText("Zoom Out");
        leftGroup.add(zoomOutButton);

        // Create zoom slider
        zoomSlider = new JSlider(10, 400, 100); // Values in percentage
        zoomSlider.setPreferredSize(new Dimension(300, 20));
        zoomSlider.setToolTipText("Zoom Level");
        zoomSlider.setMajorTickSpacing(50);
        zoomSlider.setMinorTickSpacing(10);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setPaintLabels(true);
        zoomSlider.addChangeListener(e -> {
            double newZoom = zoomSlider.getValue() / 100.0;
            if (Math.abs(newZoom - currentZoom) > 0.001) {
                setZoom(newZoom, null);
            }
        });
        leftGroup.add(zoomSlider);

        zoomInButton = UIUtils.createButton("", FontAwesomeSolid.SEARCH_PLUS, e -> zoomIn(null));
        zoomInButton.setPreferredSize(new Dimension(28, 28));
        zoomInButton.setToolTipText("Zoom In");
        leftGroup.add(zoomInButton);

        // Right group: fit and 100% buttons
        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        rightGroup.setOpaque(false);

        zoomFitButton = UIUtils.createButton("", FontAwesomeSolid.EXPAND_ARROWS_ALT, e -> zoomToFit());
        zoomFitButton.setPreferredSize(new Dimension(28, 28));
        zoomFitButton.setToolTipText("Fit to Window");
        rightGroup.add(zoomFitButton);

        zoom100Button = UIUtils.createButton("", FontAwesomeSolid.SEARCH, e -> zoomTo100());
        zoom100Button.setPreferredSize(new Dimension(28, 28));
        zoom100Button.setToolTipText("100%");
        rightGroup.add(zoom100Button);

        panel.add(leftGroup);
        panel.add(rightGroup);

        return panel;
    }

    private JLabel createInfoLabel() {
        JLabel label = UIUtils.createLabel("", UIConstants.TINY_FONT_SIZE, null);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING, 0, 0, 0));
        return label;
    }

    // ===== IMAGE LOADING =====

    public void loadImageWithROIs(File imageFile, File roiZipFile) {
        if (imageFile == null || !imageFile.exists()) {
            showErrorState("Image file not found");
            return;
        }

        if (isLoading) {
            LOGGER.debug("Already loading image, ignoring request");
            return;
        }

        currentImageFile = imageFile;
        LOGGER.info("Loading image and ROIs: {}", imageFile.getName());
        showLoadingState();

        CompletableFuture.supplyAsync(() -> loadImageSafely(imageFile))
            .thenAccept(imagePlus -> handleImageLoaded(imagePlus, roiZipFile))
            .exceptionally(this::handleImageError);
    }

    private ImagePlus loadImageSafely(File imageFile) {
        try {
            return ImageLoader.loadImage(imageFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Error loading image: {}", imageFile.getAbsolutePath(), e);
            return null;
        }
    }

    private void handleImageLoaded(ImagePlus imagePlus, File roiZipFile) {
        SwingUtilities.invokeLater(() -> {
            if (imagePlus != null) {
                displayImageWithROIs(imagePlus, roiZipFile);
            } else {
                showErrorState("Unsupported format or corrupted file");
            }
        });
    }

    private Void handleImageError(Throwable throwable) {
        LOGGER.error("Error in image loading task: {}", currentImageFile.getAbsolutePath(), throwable);
        SwingUtilities.invokeLater(() -> showErrorState(throwable.getMessage()));
        return null;
    }

    private void displayImageWithROIs(ImagePlus imagePlus, File roiZipFile) {
        try {
            currentImagePlus = imagePlus;
            isLoading = false;
            originalImage = imagePlus.getImage();
            originalImageWidth = imagePlus.getWidth();
            originalImageHeight = imagePlus.getHeight();

            // Start with fit zoom for better initial view
            currentZoom = calculateFitZoom();

            // Create and display scaled image
            Image scaledImage = createScaledImage();
            if (scaledImage != null) {
                ImageIcon imageIcon = new ImageIcon(scaledImage);
                imageLabel.setIcon(imageIcon);
                imageLabel.setText("");

                // Update layout
                updateLayeredPaneLayout(imageIcon);

                // Set original image dimensions for ROI overlay
                roiOverlay.setImageDimensions(originalImageWidth, originalImageHeight);

                // Load ROIs from ZIP file if provided
                if (roiZipFile != null && roiZipFile.exists()) {
                    roiOverlay.loadROIsFromZip(roiZipFile, currentImageFile.getName());
                }

                // Enable zoom controls
                setZoomControlsEnabled(true);
                updateZoomLabel();

                updateImageInfo();
                LOGGER.info("Successfully displayed image: {}", currentImageFile.getName());
            } else {
                showErrorState("Could not scale image");
            }
        } catch (Exception e) {
            LOGGER.error("Error displaying image: {}", currentImageFile.getName(), e);
            showErrorState("Error processing image: " + e.getMessage());
        }
    }

    // ===== LAYOUT AND TRANSFORM CALCULATIONS =====

    private void updateLayeredPaneLayout(ImageIcon imageIcon) {
        layeredPane.removeAll();

        int imageWidth = imageIcon.getIconWidth();
        int imageHeight = imageIcon.getIconHeight();

        // Calculate layered pane size
        Dimension viewportSize = scrollPane.getViewport().getSize();
        int minSize = 100;

        int layeredPaneWidth = Math.max(minSize, Math.max(imageWidth, viewportSize.width));
        int layeredPaneHeight = Math.max(minSize, Math.max(imageHeight, viewportSize.height));

        layeredPane.setPreferredSize(new Dimension(layeredPaneWidth, layeredPaneHeight));
        layeredPane.setSize(layeredPaneWidth, layeredPaneHeight);

        // Center the image within the layered pane
        int imageX = Math.max(0, (layeredPaneWidth - imageWidth) / 2);
        int imageY = Math.max(0, (layeredPaneHeight - imageHeight) / 2);

        imageLabel.setBounds(imageX, imageY, imageWidth, imageHeight);
        layeredPane.add(imageLabel, JLayeredPane.DEFAULT_LAYER);

        // Set ROI overlay bounds to match layered pane
        roiOverlay.setBounds(0, 0, layeredPaneWidth, layeredPaneHeight);
        layeredPane.add(roiOverlay, JLayeredPane.PALETTE_LAYER);

        updateROIOverlayTransform();
        layeredPane.revalidate();
        layeredPane.repaint();

        LOGGER.debug("Layout update - Image: {}x{}, Viewport: {}x{}, LayeredPane: {}x{}, ImagePos: ({},{})",
            imageWidth, imageHeight, viewportSize.width, viewportSize.height,
            layeredPaneWidth, layeredPaneHeight, imageX, imageY);
    }

    private void updateROIOverlayTransform() {
        if (roiOverlay == null || currentImagePlus == null || imageLabel.getIcon() == null) {
            return;
        }

        // Get image positioning values
        ImageIcon imageIcon = (ImageIcon) imageLabel.getIcon();
        int displayedImageWidth = imageIcon.getIconWidth();
        int displayedImageHeight = imageIcon.getIconHeight();

        // Calculate scale factors (same for both X and Y to maintain aspect ratio)
        double scaleX = (double) displayedImageWidth / originalImageWidth;
        double scaleY = (double) displayedImageHeight / originalImageHeight;

        // Calculate image position in layered pane (centered)
        int layeredPaneWidth = layeredPane.getWidth();
        int layeredPaneHeight = layeredPane.getHeight();
        int imageX = Math.max(0, (layeredPaneWidth - displayedImageWidth) / 2);
        int imageY = Math.max(0, (layeredPaneHeight - displayedImageHeight) / 2);

        LOGGER.trace("ROI Transform - Scale: {}x{}, Position: ({},{}), Image: {}x{}, Original: {}x{}",
            scaleX, scaleY, imageX, imageY, displayedImageWidth, displayedImageHeight, 
            originalImageWidth, originalImageHeight);

        // Set transform for accurate overlay alignment
        roiOverlay.setImageTransform(scaleX, scaleY, imageX, imageY);
    }

    // ===== ZOOM FUNCTIONALITY =====

    private double calculateFitZoom() {
        if (originalImage == null || scrollPane == null) {
            return 1.0;
        }

        Dimension viewportSize = scrollPane.getViewport().getSize();
        double scaleX = (double) viewportSize.width / originalImageWidth;
        double scaleY = (double) viewportSize.height / originalImageHeight;

        return Math.min(Math.min(scaleX, scaleY), 1.0);
    }

    private Image createScaledImage() {
        if (originalImage == null) return null;

        int scaledWidth = Math.max(1, (int) Math.round(originalImageWidth * currentZoom));
        int scaledHeight = Math.max(1, (int) Math.round(originalImageHeight * currentZoom));

        LOGGER.debug("Creating scaled image: {}x{} -> {}x{} (zoom={})",
            originalImageWidth, originalImageHeight, scaledWidth, scaledHeight, currentZoom);

        return originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
    }

    private void updateZoomLabel() {
        if (zoomLabel != null) {
            int percentage = (int) Math.round(currentZoom * 100);
            zoomLabel.setText(percentage + "%");
        }
    }

    private void setZoomControlsEnabled(boolean enabled) {
        if (zoomInButton != null) zoomInButton.setEnabled(enabled);
        if (zoomOutButton != null) zoomOutButton.setEnabled(enabled);
        if (zoomFitButton != null) zoomFitButton.setEnabled(enabled);
        if (zoom100Button != null) zoom100Button.setEnabled(enabled);
        if (zoomSlider != null) zoomSlider.setEnabled(enabled);
    }

    private void zoomIn(Point centerPoint) {
        if (currentImagePlus == null) return;

        double newZoom = Math.min(currentZoom + ZOOM_STEP, MAX_ZOOM);
        if (newZoom != currentZoom) {
            setZoom(newZoom, centerPoint);
        }
    }

    private void zoomOut(Point centerPoint) {
        if (currentImagePlus == null) return;

        double newZoom = Math.max(currentZoom - ZOOM_STEP, MIN_ZOOM);
        if (newZoom != currentZoom) {
            setZoom(newZoom, centerPoint);
        }
    }

    private void zoomToFit() {
        if (currentImagePlus == null) return;

        double fitZoom = calculateFitZoom();
        setZoom(fitZoom, null);
    }

    private void zoomTo100() {
        if (currentImagePlus == null) return;

        setZoom(1.0, null);
    }

    private void setZoom(double newZoom, Point centerPoint) {
        if (currentImagePlus == null || originalImage == null) return;

        // Store scroll position if we have a center point
        final Point scrollPosition;
        if (centerPoint != null) {
            JViewport viewport = scrollPane.getViewport();
            Point viewPosition = viewport.getViewPosition();

            double relativeX = (centerPoint.x + viewPosition.x) / (originalImageWidth * currentZoom);
            double relativeY = (centerPoint.y + viewPosition.y) / (originalImageHeight * currentZoom);

            int newScrollX = (int) (relativeX * originalImageWidth * newZoom - centerPoint.x);
            int newScrollY = (int) (relativeY * originalImageHeight * newZoom - centerPoint.y);

            scrollPosition = new Point(newScrollX, newScrollY);
        } else {
            scrollPosition = null;
        }

        currentZoom = newZoom;

        // Update image
        Image scaledImage = createScaledImage();
        if (scaledImage != null) {
            ImageIcon newImageIcon = new ImageIcon(scaledImage);
            imageLabel.setIcon(newImageIcon);

            updateLayeredPaneLayout(newImageIcon);

            if (scrollPosition != null) {
                SwingUtilities.invokeLater(() -> {
                    JViewport viewport = scrollPane.getViewport();
                    int x = Math.max(0, Math.min(scrollPosition.x, layeredPane.getWidth() - viewport.getWidth()));
                    int y = Math.max(0, Math.min(scrollPosition.y, layeredPane.getHeight() - viewport.getHeight()));
                    viewport.setViewPosition(new Point(x, y));
                });
            }

            updateZoomLabel();
            updateImageInfo();

            // Update slider without triggering listener
            if (zoomSlider != null && !zoomSlider.getValueIsAdjusting()) {
                int sliderValue = (int) Math.round(currentZoom * 100);
                zoomSlider.setValue(sliderValue);
            }
        }
    }

    // ===== THROTTLED UPDATES =====

    private void throttledROIUpdate() {
        long now = System.currentTimeMillis();
        if (!updatePending && (now - lastUpdateTime) >= UPDATE_THROTTLE_MS) {
            updateROIOverlayTransform();
            lastUpdateTime = now;
        } else if (!updatePending) {
            updatePending = true;
            SwingUtilities.invokeLater(() -> {
                updateROIOverlayTransform();
                updatePending = false;
                lastUpdateTime = System.currentTimeMillis();
            });
        }
    }

    // ===== STATE MANAGEMENT =====

    private void showEmptyState() {
        showStatePanel(
            FontAwesomeSolid.IMAGE,
            "No image selected",
            "Select an image from the gallery to view it here",
            UIManager.getColor("Label.disabledForeground"));
        imageInfoLabel.setText("");
        currentImageFile = null;
        currentImagePlus = null;
        originalImage = null;
        currentZoom = 1.0;
        setZoomControlsEnabled(false);
        updateZoomLabel();
    }

    private void showLoadingState() {
        isLoading = true;
        showStatePanel(
            FontAwesomeSolid.SPINNER,
            "Loading image...",
            null,
            UIManager.getColor("Label.disabledForeground"));
        imageInfoLabel.setText(
            "Loading " + (currentImageFile != null ? currentImageFile.getName() : "image") + "...");
    }

    private void showErrorState(String errorMessage) {
        isLoading = false;
        showStatePanel(
            FontAwesomeSolid.EXCLAMATION_TRIANGLE,
            "Failed to load image",
            errorMessage,
            UIConstants.ERROR_COLOR);
        imageInfoLabel.setText(
            "Error loading " + (currentImageFile != null ? currentImageFile.getName() : "image"));
    }

    private void showStatePanel(FontAwesomeSolid icon, String message, String detail, Color color) {
        JPanel panel = UIUtils.createPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Icon
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, UIConstants.LARGE_SPACING, 0);
        panel.add(UIUtils.createIconLabel(icon, icon == FontAwesomeSolid.IMAGE ? 64 : 48, color), gbc);

        // Main message
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, detail != null ? UIConstants.MEDIUM_SPACING : 0, 0);
        panel.add(
            UIUtils.createBoldLabel(
                message,
                icon == FontAwesomeSolid.IMAGE
                    ? UIConstants.SUBTITLE_FONT_SIZE
                    : UIConstants.LARGE_FONT_SIZE),
            gbc);

        // Detail message
        if (detail != null) {
            gbc.gridy++;
            gbc.insets = new Insets(0, 0, 0, 0);
            JLabel detailLabel =
                UIUtils.createLabel(
                    "<html><center>" + detail + "</center></html>",
                    UIConstants.NORMAL_FONT_SIZE,
                    UIManager.getColor("Label.disabledForeground"));
            panel.add(detailLabel, gbc);
        }

        // Clear the layered pane and show state panel
        layeredPane.removeAll();
        layeredPane.add(panel, JLayeredPane.DEFAULT_LAYER);
        panel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
        layeredPane.revalidate();
        layeredPane.repaint();
    }

    private void updateImageInfo() {
        if (currentImageFile != null && currentImagePlus != null) {
            String info = String.format("%s | %d × %d pixels | %s | %.0f%%",
                currentImageFile.getName(),
                originalImageWidth, originalImageHeight,
                ImageLoader.getFileExtension(currentImageFile.getName()).toUpperCase(),
                currentZoom * 100);
            imageInfoLabel.setText(info);
        } else {
            imageInfoLabel.setText("");
        }
    }

    // ===== PUBLIC API =====

    public File getCurrentImageFile() {
        return currentImageFile;
    }

    public ImagePlus getCurrentImagePlus() {
        return currentImagePlus;
    }

    public NewDatasetROIOverlay getROIOverlay() {
        return roiOverlay;
    }

    public double getCurrentZoom() {
        return currentZoom;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void clearImage() {
        roiOverlay.clear();
        showEmptyState();
    }
}