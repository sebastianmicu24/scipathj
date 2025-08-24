package com.scipath.scipathj.ui.common;

import com.scipath.scipathj.core.config.MainSettings;
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
 * High-efficiency image viewer with optimized zoom and scroll handling.
 * Designed for maximum performance with large images and multiple ROIs.
 */
public class MainImageViewer extends JPanel
    implements ROIOverlay.ROIOverlayListener, ROIManager.ROIChangeListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainImageViewer.class);
  private static final int MAX_DISPLAY_SIZE = 800;

  // Zoom functionality
  private static final double MIN_ZOOM = 0.1; // 10%
  private static final double MAX_ZOOM = 4.0; // 400%
  private static final double ZOOM_STEP = 0.1;
  private double currentZoom = 1.0;

  // Core components
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
  private JSlider zoomSlider;

  // Performance optimization: batch update flags
  private boolean updatePending = false;
  private long lastUpdateTime = 0;
  private static final long UPDATE_THROTTLE_MS = 16; // ~60fps

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

    // Add component resize listener to handle any size changes
    addComponentListener(new java.awt.event.ComponentAdapter() {
      @Override
      public void componentResized(java.awt.event.ComponentEvent e) {
        LOGGER.debug("MainImageViewer resized: {}x{} - checking if layout update needed",
            getWidth(), getHeight());
        if (imageLabel.getIcon() != null) {
          // Force layout update when component is resized
          updateLayeredPaneLayout((ImageIcon) imageLabel.getIcon());
        }
      }
    });
  }

  private void setupROISystem() {
    roiOverlay.addROIOverlayListener(this);
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

    // Add mouse wheel zoom functionality
    pane.addMouseWheelListener(
        new MouseWheelListener() {
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
    pane.getVerticalScrollBar()
        .addAdjustmentListener(
            new AdjustmentListener() {
              @Override
              public void adjustmentValueChanged(AdjustmentEvent e) {
                LOGGER.debug("Vertical scroll event: value={}, type={}", e.getValue(), e.getAdjustmentType());
                throttledROIUpdate();
              }
            });

    pane.getHorizontalScrollBar()
        .addAdjustmentListener(
            new AdjustmentListener() {
              @Override
              public void adjustmentValueChanged(AdjustmentEvent e) {
                LOGGER.debug("Horizontal scroll event: value={}, type={}", e.getValue(), e.getAdjustmentType());
                throttledROIUpdate();
              }
            });

    // Add viewport resize listener to handle window resizing
    pane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
      @Override
      public void componentResized(java.awt.event.ComponentEvent e) {
        LOGGER.debug("Viewport resized: {}x{} - updating transforms",
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

    // Create wide zoom slider (10% to 400%)
    zoomSlider = new JSlider(10, 400, 100); // Values in percentage
    zoomSlider.setPreferredSize(new Dimension(400, 20)); // Much wider slider
    zoomSlider.setToolTipText("Zoom Level");
    zoomSlider.setMajorTickSpacing(50);
    zoomSlider.setMinorTickSpacing(10);
    zoomSlider.setPaintTicks(true);
    zoomSlider.setPaintLabels(true);
    zoomSlider.addChangeListener(e -> {
      double newZoom = zoomSlider.getValue() / 100.0;
      setZoom(newZoom, null);
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
    panel.add(new JSeparator(SwingConstants.VERTICAL));
    panel.add(rightGroup);

    setZoomControlsEnabled(false);
    return panel;
  }

  private JLabel createInfoLabel() {
    JLabel label = UIUtils.createLabel("", UIConstants.TINY_FONT_SIZE, null);
    label.setHorizontalAlignment(SwingConstants.CENTER);
    label.setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING, 0, 0, 0));
    return label;
  }

  // ===== PERFORMANCE OPTIMIZATION: THROTTLED UPDATES =====

  private void throttledROIUpdate() {
    if (!updatePending) {
      updatePending = true;
      long currentTime = System.currentTimeMillis();
      long timeSinceLastUpdate = currentTime - lastUpdateTime;

      LOGGER.debug("=== SCROLL EVENT TRIGGERED ===");
      LOGGER.debug("Update pending: {}, Time since last update: {}ms", updatePending, timeSinceLastUpdate);

      if (timeSinceLastUpdate >= UPDATE_THROTTLE_MS) {
        // Immediate update if enough time has passed
        LOGGER.debug("Performing immediate ROI update");
        performROIUpdate();
      } else {
        // Schedule throttled update
        long delay = UPDATE_THROTTLE_MS - timeSinceLastUpdate;
        LOGGER.debug("Scheduling throttled ROI update in {}ms", delay);
        Timer timer = new Timer((int) delay, e -> performROIUpdate());
        timer.setRepeats(false);
        timer.start();
      }
    } else {
      LOGGER.debug("Update already pending, skipping throttled update");
    }
  }

  private void performROIUpdate() {
    updatePending = false;
    lastUpdateTime = System.currentTimeMillis();

    LOGGER.debug("=== PERFORMING ROI UPDATE ===");
    LOGGER.debug("Current scroll position: ({}, {})",
        scrollPane.getViewport().getViewPosition().x,
        scrollPane.getViewport().getViewPosition().y);

    updateROIOverlayTransform();
    LOGGER.debug("=== ROI UPDATE COMPLETED ===");
  }

  // ===== IMAGE LOADING AND DISPLAY =====

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
        this.originalImage = image;
        this.originalImageWidth = imagePlus.getWidth();
        this.originalImageHeight = imagePlus.getHeight();

        // Start with 100% zoom (native image resolution)
        currentZoom = 1.0;

        // Create scaled image
        Image scaledImage = createScaledImage();
        ImageIcon imageIcon = new ImageIcon(scaledImage);
        imageLabel.setIcon(imageIcon);
        imageLabel.setText("");

        // Update layout
        updateLayeredPaneLayout(imageIcon);

        // Update ROI overlay
        updateROIOverlay();

        // Set original image dimensions for buffer allocation (working in native resolution)
        roiOverlay.setImageDimensions(originalImageWidth, originalImageHeight);

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

  // ===== LAYOUT AND TRANSFORM CALCULATIONS =====

  private void updateLayeredPaneLayout(ImageIcon imageIcon) {
    layeredPane.removeAll();

    int imageWidth = imageIcon.getIconWidth();
    int imageHeight = imageIcon.getIconHeight();

    // Calculate layered pane size - CRITICAL for synchronization
    // The layered pane must be large enough to contain the image AND provide
    // scrollable space when needed. The key is to use the maximum of:
    // 1. Image size (for when image is larger than viewport)
    // 2. Viewport size (for when viewport is larger than image)
    // 3. A minimum size to prevent edge cases
    Dimension viewportSize = scrollPane.getViewport().getSize();
    int minSize = 100; // Minimum size to prevent edge cases

    int layeredPaneWidth = Math.max(minSize, Math.max(imageWidth, viewportSize.width));
    int layeredPaneHeight = Math.max(minSize, Math.max(imageHeight, viewportSize.height));

    layeredPane.setPreferredSize(new Dimension(layeredPaneWidth, layeredPaneHeight));
    layeredPane.setSize(layeredPaneWidth, layeredPaneHeight);

    // Center the image within the layered pane
    // This calculation must be consistent with the transform calculation
    int imageX = Math.max(0, (layeredPaneWidth - imageWidth) / 2);
    int imageY = Math.max(0, (layeredPaneHeight - imageHeight) / 2);

    imageLabel.setBounds(imageX, imageY, imageWidth, imageHeight);
    layeredPane.add(imageLabel, JLayeredPane.DEFAULT_LAYER);

    // Set ROI overlay bounds to exactly match the layered pane
    roiOverlay.setBounds(0, 0, layeredPaneWidth, layeredPaneHeight);
    layeredPane.add(roiOverlay, JLayeredPane.PALETTE_LAYER);

    updateROIOverlayTransform();
    layeredPane.revalidate();
    layeredPane.repaint();

    LOGGER.debug("Layout update - Image: {}x{}, Viewport: {}x{}, LayeredPane: {}x{}, ImagePos: ({},{})",
        imageWidth, imageHeight, viewportSize.width, viewportSize.height,
        layeredPaneWidth, layeredPaneHeight, imageX, imageY);
  }

  public void updateROIOverlayTransform() {
    if (roiOverlay == null || currentImagePlus == null || imageLabel.getIcon() == null) return;

    // Get the exact same values used for image positioning
    ImageIcon imageIcon = (ImageIcon) imageLabel.getIcon();
    int imageWidth = imageIcon.getIconWidth();
    int imageHeight = imageIcon.getIconHeight();

    // Get current scroll position
    JViewport viewport = scrollPane.getViewport();
    Point viewPosition = viewport.getViewPosition();

    // Calculate scale factors (actual displayed size vs original size)
    double scaleX = (double) imageWidth / originalImageWidth;
    double scaleY = (double) imageHeight / originalImageHeight;

    // Calculate the image's actual position in the layered pane
    int layeredPaneWidth = layeredPane.getWidth();
    int layeredPaneHeight = layeredPane.getHeight();

    // Center the image in the layered pane (same as image positioning)
    int imageX = Math.max(0, (layeredPaneWidth - imageWidth) / 2);
    int imageY = Math.max(0, (layeredPaneHeight - imageHeight) / 2);

    // FIXED: Make overlay positioning match image positioning exactly
    // The image is positioned at (imageX, imageY) in the layered pane
    // The overlay should render its content at the same position to match

    // ENHANCED DEBUG LOGGING: Include scaling analysis
    LOGGER.debug("=== COORDINATE SYNC DEBUG ===");
    LOGGER.debug("Component dimensions: MainImageViewer={}x{}, Viewport={}x{}",
        getWidth(), getHeight(), viewport.getWidth(), viewport.getHeight());
    LOGGER.debug("Image: {}x{} -> {}x{} (scale: {}x{})",
        originalImageWidth, originalImageHeight, imageWidth, imageHeight, scaleX, scaleY);
    LOGGER.debug("LayeredPane: {}x{}, Image position: ({}, {})",
        layeredPaneWidth, layeredPaneHeight, imageX, imageY);
    LOGGER.debug("Scroll position: ({}, {})",
        viewPosition.x, viewPosition.y);
    LOGGER.debug("Scroll range: H[0-{}], V[0-{}]",
        layeredPaneWidth - viewport.getWidth(), layeredPaneHeight - viewport.getHeight());
    LOGGER.debug("=== END COORDINATE SYNC DEBUG ===");

    // Set the transform to match image positioning exactly
    // The overlay content should appear at the same position as the image
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

    // Update zoom label and slider
    updateZoomLabel();
    if (zoomSlider != null) {
      zoomSlider.setValue((int) Math.round(currentZoom * 100));
    }

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

  private void updateROIOverlay() {
    if (currentImageFile != null) {
      String fileName = currentImageFile.getName();
      List<UserROI> rois = roiManager.getROIsForImage(fileName);
      roiOverlay.setDisplayedROIs(rois, fileName);
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

  // ===== GETTERS AND SETTERS =====

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

  public void setROICreationMode(UserROI.ROIType type) {
    if (type != null) {
      roiOverlay.setROICreationMode(type);
      LOGGER.debug("Enabled ROI creation mode: {}", type);
    } else {
      roiOverlay.disableROICreationMode();
      LOGGER.debug("Disabled ROI creation mode");
    }
  }

  public void disableROICreationMode() {
    roiOverlay.disableROICreationMode();
  }

  public ROIOverlay getROIOverlay() {
    return roiOverlay;
  }

  public double getCurrentZoom() {
    return currentZoom;
  }

  // ===== ROI LISTENER IMPLEMENTATIONS =====

  @Override
  public void onROICreated(UserROI roi) {
    roiManager.addROI(roi);
    LOGGER.info("ROI created and added to manager: {}", roi);
  }

  @Override
  public void onROISelected(UserROI roi) {
    LOGGER.debug("ROI selected: {}", roi.getName());
  }

  @Override
  public void onROIDeselected() {
    LOGGER.debug("ROI selection cleared");
  }

  @Override
  public void onROIAdded(UserROI roi) {
    if (currentImageFile != null && roi.getImageFileName().equals(currentImageFile.getName())) {
      updateROIOverlay();
    }
  }

  @Override
  public void onROIRemoved(UserROI roi) {
    if (currentImageFile != null && roi.getImageFileName().equals(currentImageFile.getName())) {
      updateROIOverlay();
    }
  }

  @Override
  public void onROIUpdated(UserROI roi) {
    if (currentImageFile != null && roi.getImageFileName().equals(currentImageFile.getName())) {
      updateROIOverlay();
    }
  }

  @Override
  public void onROIsCleared(String imageFileName) {
    if (currentImageFile != null && imageFileName.equals(currentImageFile.getName())) {
      updateROIOverlay();
    }
  }

  // ===== SETTINGS INTEGRATION =====

  public void initializeWithSettings(MainSettings mainSettings) {
    if (roiOverlay != null && mainSettings != null) {
      roiOverlay.updateSettings(mainSettings);
      LOGGER.debug("Initialized ROI overlay with loaded settings");
    }
  }
}
