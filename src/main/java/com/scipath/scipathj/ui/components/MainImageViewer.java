package com.scipath.scipathj.ui.components;

import com.scipath.scipathj.data.model.UserROI;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component for displaying the main selected image in large format with ROI overlay support.
 */
public class MainImageViewer extends JPanel
    implements ROIOverlay.ROIOverlayListener, ROIManager.ROIChangeListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainImageViewer.class);
  private static final int MAX_DISPLAY_SIZE = 800;

  // Zoom functionality
  private static final double MIN_ZOOM = 0.1;
  private static final double MAX_ZOOM = 5.0;
  private static final double ZOOM_STEP = 0.1;
  private double currentZoom = 1.0;

  private JLabel imageLabel;
  private JLabel imageInfoLabel;
  private JScrollPane scrollPane;
  private ROIOverlay roiOverlay;
  private JLayeredPane layeredPane;
  private File currentImageFile;
  private ImagePlus currentImagePlus;
  private boolean isLoading = false;

  // Image display properties
  private Image originalImage;
  private int originalImageWidth;
  private int originalImageHeight;

  // ROI management
  private ROIManager roiManager;

  // Zoom controls
  private JPanel zoomControlPanel;
  private JButton zoomInButton;
  private JButton zoomOutButton;
  private JButton zoomFitButton;
  private JButton zoom100Button;
  private JLabel zoomLabel;

  public MainImageViewer() {
    this.roiManager = ROIManager.getInstance();
    initializeComponents();
    setupROISystem();
    showEmptyState();
  }

  private void initializeComponents() {
    setLayout(new BorderLayout());
    setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING));
    setOpaque(false);

    imageLabel = createImageLabel();
    roiOverlay = new ROIOverlay();
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
  }

  private void setupROISystem() {
    // Register as ROI overlay listener
    roiOverlay.addROIOverlayListener(this);

    // Register as ROI manager listener
    roiManager.addROIChangeListener(this);
  }

  private JLayeredPane createLayeredPane() {
    JLayeredPane pane = new JLayeredPane();
    pane.setPreferredSize(new Dimension(600, 400));

    // Add image label to bottom layer
    imageLabel.setBounds(0, 0, 600, 400);
    pane.add(imageLabel, JLayeredPane.DEFAULT_LAYER);

    // Add ROI overlay to top layer
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
    pane.setBorder(BorderFactory.createLoweredBevelBorder());

    // Add mouse wheel zoom functionality
    pane.addMouseWheelListener(
        new MouseWheelListener() {
          @Override
          public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.isControlDown() && currentImagePlus != null) {
              // Zoom with Ctrl + mouse wheel
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

    // Add scroll listeners to synchronize ROI overlay
    pane.getVerticalScrollBar()
        .addAdjustmentListener(
            new AdjustmentListener() {
              @Override
              public void adjustmentValueChanged(AdjustmentEvent e) {
                updateROIOverlayTransform();
              }
            });

    pane.getHorizontalScrollBar()
        .addAdjustmentListener(
            new AdjustmentListener() {
              @Override
              public void adjustmentValueChanged(AdjustmentEvent e) {
                updateROIOverlayTransform();
              }
            });

    return pane;
  }

  private JPanel createZoomControls() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
    panel.setOpaque(false);

    // Zoom out button
    zoomOutButton = UIUtils.createButton("", FontAwesomeSolid.SEARCH_MINUS, e -> zoomOut(null));
    zoomOutButton.setPreferredSize(new Dimension(30, 30));
    zoomOutButton.setToolTipText("Zoom Out");

    // Zoom label
    zoomLabel = new JLabel("100%");
    zoomLabel.setFont(zoomLabel.getFont().deriveFont(Font.PLAIN, UIConstants.SMALL_FONT_SIZE));
    zoomLabel.setPreferredSize(new Dimension(50, 30));
    zoomLabel.setHorizontalAlignment(SwingConstants.CENTER);

    // Zoom in button
    zoomInButton = UIUtils.createButton("", FontAwesomeSolid.SEARCH_PLUS, e -> zoomIn(null));
    zoomInButton.setPreferredSize(new Dimension(30, 30));
    zoomInButton.setToolTipText("Zoom In");

    // Fit to window button
    zoomFitButton = UIUtils.createButton("", FontAwesomeSolid.EXPAND_ARROWS_ALT, e -> zoomToFit());
    zoomFitButton.setPreferredSize(new Dimension(30, 30));
    zoomFitButton.setToolTipText("Fit to Window");

    // 100% zoom button
    zoom100Button = UIUtils.createButton("", FontAwesomeSolid.SEARCH, e -> zoomTo100());
    zoom100Button.setPreferredSize(new Dimension(30, 30));
    zoom100Button.setToolTipText("100%");

    panel.add(zoomOutButton);
    panel.add(zoomLabel);
    panel.add(zoomInButton);
    panel.add(new JSeparator(SwingConstants.VERTICAL));
    panel.add(zoomFitButton);
    panel.add(zoom100Button);

    // Initially disabled
    setZoomControlsEnabled(false);

    return panel;
  }

  private JLabel createInfoLabel() {
    JLabel label = UIUtils.createLabel("", UIConstants.TINY_FONT_SIZE, null);
    label.setHorizontalAlignment(SwingConstants.CENTER);
    label.setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING, 0, 0, 0));
    return label;
  }

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

  public void displayImage(File imageFile) {
    if (imageFile == null) {
      showEmptyState();
      return;
    }

    if (imageFile.equals(currentImageFile)) {
      LOGGER.debug("Image already displayed: {}", imageFile.getName());
      return;
    }

    this.currentImageFile = imageFile;
    LOGGER.info("Loading image for main viewer: {}", imageFile.getAbsolutePath());
    showLoadingState();

    CompletableFuture.supplyAsync(() -> loadImageSafely(imageFile))
        .thenAccept(this::handleImageLoaded)
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

  private void handleImageLoaded(ImagePlus imagePlus) {
    SwingUtilities.invokeLater(
        () -> {
          if (imagePlus != null) {
            displayLoadedImage(imagePlus);
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

  private void displayLoadedImage(ImagePlus imagePlus) {
    this.currentImagePlus = imagePlus;
    this.isLoading = false;

    try {
      Image image = imagePlus.getImage();
      if (image != null) {
        // Store original image properties
        this.originalImage = image;
        this.originalImageWidth = imagePlus.getWidth();
        this.originalImageHeight = imagePlus.getHeight();

        // Reset zoom to fit
        currentZoom = calculateFitZoom();

        // Create scaled image
        Image scaledImage = createScaledImage();
        ImageIcon imageIcon = new ImageIcon(scaledImage);
        imageLabel.setIcon(imageIcon);
        imageLabel.setText("");

        // Update layered pane layout
        updateLayeredPaneLayout(imageIcon);

        // Update ROI overlay with current image's ROIs
        updateROIOverlay();

        // Enable zoom controls
        setZoomControlsEnabled(true);
        updateZoomLabel();

        updateImageInfo();
        LOGGER.info("Successfully displayed image: {}", currentImageFile.getName());
      } else {
        showErrorState("Could not extract image data");
      }
    } catch (Exception e) {
      LOGGER.error("Error displaying image: {}", currentImageFile.getName(), e);
      showErrorState("Error processing image: " + e.getMessage());
    }
  }

  private void updateLayeredPaneLayout(ImageIcon imageIcon) {
    // Clear and rebuild layered pane
    layeredPane.removeAll();

    // Calculate image position and size
    int imageWidth = imageIcon.getIconWidth();
    int imageHeight = imageIcon.getIconHeight();

    // Update layered pane size to accommodate the full image
    // Ensure the layered pane is at least as large as the viewport
    Dimension viewportSize = scrollPane.getViewport().getSize();
    int layeredPaneWidth = Math.max(imageWidth, viewportSize.width);
    int layeredPaneHeight = Math.max(imageHeight, viewportSize.height);

    layeredPane.setPreferredSize(new Dimension(layeredPaneWidth, layeredPaneHeight));
    layeredPane.setSize(layeredPaneWidth, layeredPaneHeight);

    // Center the image within the layered pane
    int imageX = Math.max(0, (layeredPaneWidth - imageWidth) / 2);
    int imageY = Math.max(0, (layeredPaneHeight - imageHeight) / 2);

    // Set image label bounds
    imageLabel.setBounds(imageX, imageY, imageWidth, imageHeight);
    layeredPane.add(imageLabel, JLayeredPane.DEFAULT_LAYER);

    // Set ROI overlay bounds to cover the entire layered pane
    // This ensures ROIs are visible even when scrolling
    roiOverlay.setBounds(0, 0, layeredPaneWidth, layeredPaneHeight);
    layeredPane.add(roiOverlay, JLayeredPane.PALETTE_LAYER);

    // Update ROI overlay transform with proper offset
    updateROIOverlayTransform();

    // Force scroll pane to recognize the new size
    layeredPane.revalidate();
    layeredPane.repaint();
    scrollPane.revalidate();
    scrollPane.repaint();
  }

  private void updateROIOverlay() {
    if (currentImageFile != null) {
      String fileName = currentImageFile.getName();
      List<UserROI> rois = roiManager.getROIsForImage(fileName);
      roiOverlay.setDisplayedROIs(rois, fileName);
      LOGGER.debug("Updated ROI overlay with {} ROIs for image '{}'", rois.size(), fileName);
    }
  }

  private void updateImageInfo() {
    if (currentImageFile == null || currentImagePlus == null) {
      imageInfoLabel.setText("");
      return;
    }

    String fileName = currentImageFile.getName();
    String fileSize = ImageLoader.formatFileSize(currentImageFile.length());
    String dimensions = currentImagePlus.getWidth() + " × " + currentImagePlus.getHeight();
    String extension = ImageLoader.getFileExtension(fileName).toUpperCase();

    imageInfoLabel.setText(
        String.format("%s | %s | %s pixels | %s", fileName, fileSize, dimensions, extension));
  }

  public File getCurrentImageFile() {
    return currentImageFile;
  }

  public ImagePlus getCurrentImagePlus() {
    return currentImagePlus;
  }

  public boolean isLoading() {
    return isLoading;
  }

  public void clearImage() {
    roiOverlay.setDisplayedROIs(null, null);
    showEmptyState();
  }

  /**
   * Enable ROI creation mode
   */
  public void setROICreationMode(UserROI.ROIType type) {
    if (type != null) {
      roiOverlay.setROICreationMode(type);
      LOGGER.debug("Enabled ROI creation mode: {}", type);
    } else {
      roiOverlay.disableROICreationMode();
      LOGGER.debug("Disabled ROI creation mode");
    }
  }

  /**
   * Disable ROI creation mode
   */
  public void disableROICreationMode() {
    roiOverlay.disableROICreationMode();
  }

  /**
   * Get the current ROI overlay component
   */
  public ROIOverlay getROIOverlay() {
    return roiOverlay;
  }

  // ROIOverlay.ROIOverlayListener implementation
  @Override
  public void onROICreated(UserROI roi) {
    // Add the ROI to the manager
    roiManager.addROI(roi);
    LOGGER.info("ROI created and added to manager: {}", roi);
  }

  @Override
  public void onROISelected(UserROI roi) {
    LOGGER.debug("ROI selected: {}", roi.getName());
    // Future: Could trigger ROI properties panel update
  }

  @Override
  public void onROIDeselected() {
    LOGGER.debug("ROI selection cleared");
    // Future: Could trigger ROI properties panel clear
  }

  // ROIManager.ROIChangeListener implementation
  @Override
  public void onROIAdded(UserROI roi) {
    // Update overlay if this ROI belongs to current image
    if (currentImageFile != null && roi.getImageFileName().equals(currentImageFile.getName())) {
      updateROIOverlay();
    }
  }

  @Override
  public void onROIRemoved(UserROI roi) {
    // Update overlay if this ROI belonged to current image
    if (currentImageFile != null && roi.getImageFileName().equals(currentImageFile.getName())) {
      updateROIOverlay();
    }
  }

  @Override
  public void onROIUpdated(UserROI roi) {
    // Update overlay if this ROI belongs to current image
    if (currentImageFile != null && roi.getImageFileName().equals(currentImageFile.getName())) {
      updateROIOverlay();
    }
  }

  @Override
  public void onROIsCleared(String imageFileName) {
    // Update overlay if ROIs were cleared for current image
    if (currentImageFile != null && imageFileName.equals(currentImageFile.getName())) {
      updateROIOverlay();
    }
  }

  // ========== ZOOM FUNCTIONALITY ==========

  /**
   * Calculate the zoom level that fits the image to the viewport
   */
  private double calculateFitZoom() {
    if (originalImage == null || scrollPane == null) {
      return 1.0;
    }

    Dimension viewportSize = scrollPane.getViewport().getSize();
    double scaleX = (double) viewportSize.width / originalImageWidth;
    double scaleY = (double) viewportSize.height / originalImageHeight;

    return Math.min(Math.min(scaleX, scaleY), 1.0); // Don't zoom in beyond 100%
  }

  /**
   * Create a scaled version of the original image based on current zoom
   */
  private Image createScaledImage() {
    if (originalImage == null) {
      return null;
    }

    // Use the same rounding method as ROI overlay to ensure perfect alignment
    int scaledWidth = Math.max(1, (int) Math.round(originalImageWidth * currentZoom));
    int scaledHeight = Math.max(1, (int) Math.round(originalImageHeight * currentZoom));

    LOGGER.debug(
        "Creating scaled image: {}x{} -> {}x{} (zoom={})",
        originalImageWidth,
        originalImageHeight,
        scaledWidth,
        scaledHeight,
        currentZoom);

    return originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
  }

  /**
   * Update the zoom label to show current zoom percentage
   */
  private void updateZoomLabel() {
    if (zoomLabel != null) {
      int percentage = (int) Math.round(currentZoom * 100);
      zoomLabel.setText(percentage + "%");
    }
  }

  /**
   * Enable or disable zoom controls
   */
  private void setZoomControlsEnabled(boolean enabled) {
    if (zoomInButton != null) zoomInButton.setEnabled(enabled);
    if (zoomOutButton != null) zoomOutButton.setEnabled(enabled);
    if (zoomFitButton != null) zoomFitButton.setEnabled(enabled);
    if (zoom100Button != null) zoom100Button.setEnabled(enabled);
  }

  /**
   * Zoom in by one step
   */
  private void zoomIn(Point centerPoint) {
    if (currentImagePlus == null) return;

    double newZoom = Math.min(currentZoom + ZOOM_STEP, MAX_ZOOM);
    if (newZoom != currentZoom) {
      setZoom(newZoom, centerPoint);
    }
  }

  /**
   * Zoom out by one step
   */
  private void zoomOut(Point centerPoint) {
    if (currentImagePlus == null) return;

    double newZoom = Math.max(currentZoom - ZOOM_STEP, MIN_ZOOM);
    if (newZoom != currentZoom) {
      setZoom(newZoom, centerPoint);
    }
  }

  /**
   * Zoom to fit the image in the viewport
   */
  private void zoomToFit() {
    if (currentImagePlus == null) return;

    double fitZoom = calculateFitZoom();
    setZoom(fitZoom, null);
  }

  /**
   * Zoom to 100% (actual size)
   */
  private void zoomTo100() {
    if (currentImagePlus == null) return;

    setZoom(1.0, null);
  }

  /**
   * Set the zoom level and update the display
   */
  private void setZoom(double newZoom, Point centerPoint) {
    if (currentImagePlus == null || originalImage == null) return;

    // Store the current scroll position if we have a center point
    Point scrollPosition = null;
    if (centerPoint != null) {
      JViewport viewport = scrollPane.getViewport();
      Point viewPosition = viewport.getViewPosition();

      // Calculate the relative position in the image
      double relativeX = (centerPoint.x + viewPosition.x) / (originalImageWidth * currentZoom);
      double relativeY = (centerPoint.y + viewPosition.y) / (originalImageHeight * currentZoom);

      // Calculate new scroll position
      int newScrollX = (int) (relativeX * originalImageWidth * newZoom - centerPoint.x);
      int newScrollY = (int) (relativeY * originalImageHeight * newZoom - centerPoint.y);

      scrollPosition = new Point(newScrollX, newScrollY);
    }

    // Update zoom level
    currentZoom = newZoom;

    // Create new scaled image
    Image scaledImage = createScaledImage();
    ImageIcon imageIcon = new ImageIcon(scaledImage);
    imageLabel.setIcon(imageIcon);

    // Update layout
    updateLayeredPaneLayout(imageIcon);

    // Update zoom label
    updateZoomLabel();

    // Restore scroll position if specified
    if (scrollPosition != null) {
      final Point finalScrollPosition = scrollPosition;
      SwingUtilities.invokeLater(
          () -> {
            JViewport viewport = scrollPane.getViewport();
            int x =
                Math.max(
                    0,
                    Math.min(finalScrollPosition.x, layeredPane.getWidth() - viewport.getWidth()));
            int y =
                Math.max(
                    0,
                    Math.min(
                        finalScrollPosition.y, layeredPane.getHeight() - viewport.getHeight()));
            viewport.setViewPosition(new Point(x, y));
          });
    }

    LOGGER.debug("Zoom changed to {}%", Math.round(currentZoom * 100));
  }

  /**
   * Update ROI overlay transform to account for current zoom and scroll position
   */
  private void updateROIOverlayTransform() {
    if (roiOverlay == null || currentImagePlus == null || imageLabel.getIcon() == null) return;

    // Get current scroll position
    JViewport viewport = scrollPane.getViewport();
    Point viewPosition = viewport.getViewPosition();

    // Use the actual displayed image dimensions to ensure perfect alignment
    ImageIcon imageIcon = (ImageIcon) imageLabel.getIcon();
    double imageWidth = imageIcon.getIconWidth();
    double imageHeight = imageIcon.getIconHeight();

    // Calculate the actual scale based on displayed image size
    double actualScaleX = imageWidth / originalImageWidth;
    double actualScaleY = imageHeight / originalImageHeight;

    double layeredPaneWidth = Math.max(imageWidth, viewport.getWidth());
    double layeredPaneHeight = Math.max(imageHeight, viewport.getHeight());

    // Calculate image offset using actual displayed dimensions
    double imageX = Math.max(0, (layeredPaneWidth - imageWidth) / 2.0);
    double imageY = Math.max(0, (layeredPaneHeight - imageHeight) / 2.0);

    // Update ROI overlay transform with actual scale and scroll offset
    double offsetX = -viewPosition.x + imageX;
    double offsetY = -viewPosition.y + imageY;

    LOGGER.debug(
        "Updating ROI transform: actualScale=({:.6f},{:.6f}), viewPos=({},{}), imageSize=({}x{}),"
            + " imagePos=({:.3f},{:.3f}), finalOffset=({:.3f},{:.3f})",
        actualScaleX,
        actualScaleY,
        viewPosition.x,
        viewPosition.y,
        (int) imageWidth,
        (int) imageHeight,
        imageX,
        imageY,
        offsetX,
        offsetY);

    roiOverlay.setImageTransform(actualScaleX, actualScaleY, offsetX, offsetY);
  }

  /**
   * Get current zoom level
   */
  public double getCurrentZoom() {
    return currentZoom;
  }
}
