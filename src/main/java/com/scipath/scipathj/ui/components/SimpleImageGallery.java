package com.scipath.scipathj.ui.components;

import com.scipath.scipathj.ui.utils.ImageLoader;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simplified image gallery component for displaying thumbnails in a sidebar.
 *
 * <p>This component provides a vertical scrollable list of image thumbnails
 * with minimal information display. It's designed to work alongside a main
 * image viewer component.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class SimpleImageGallery extends JPanel {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleImageGallery.class);

  private JScrollPane scrollPane;
  private JPanel thumbnailContainer;
  private JLabel statusLabel;

  private File currentFolder;
  private List<SimpleImageThumbnail> thumbnails;
  private SimpleImageThumbnail selectedThumbnail;
  private ActionListener selectionChangeListener;

  private boolean isLoading = false;

  /**
   * Creates a new SimpleImageGallery.
   */
  public SimpleImageGallery() {
    this.thumbnails = new ArrayList<>();

    initializeComponents();
    setupLayout();
    showEmptyState();
  }

  /**
   * Initializes the UI components.
   */
  private void initializeComponents() {
    setPreferredSize(new Dimension(UIConstants.GALLERY_WIDTH, 400));
    setMinimumSize(new Dimension(UIConstants.GALLERY_WIDTH, 200));
    setOpaque(false);

    createThumbnailContainer();
    createScrollPane();
    createStatusLabel();
  }

  /**
   * Creates the thumbnail container with vertical layout.
   */
  private void createThumbnailContainer() {
    thumbnailContainer = UIUtils.createVerticalPanel();
    thumbnailContainer.setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING));
  }

  /**
   * Creates the scroll pane for the thumbnails.
   */
  private void createScrollPane() {
    scrollPane = new JScrollPane(thumbnailContainer);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setOpaque(false);
    scrollPane.getViewport().setOpaque(false);
  }

  /**
   * Creates the status label.
   */
  private void createStatusLabel() {
    statusLabel = new JLabel();
    statusLabel.setFont(statusLabel.getFont().deriveFont(UIConstants.TINY_FONT_SIZE));
    statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
    statusLabel.setBorder(
        UIUtils.createPadding(
            UIConstants.SMALL_SPACING,
            UIConstants.MEDIUM_SPACING,
            UIConstants.SMALL_SPACING,
            UIConstants.MEDIUM_SPACING));
  }

  /**
   * Sets up the layout of components.
   */
  private void setupLayout() {
    setLayout(new BorderLayout());

    // Main gallery area
    add(scrollPane, BorderLayout.CENTER);

    // Status at the bottom
    add(statusLabel, BorderLayout.SOUTH);
  }

  /**
   * Shows the empty state when no images are available.
   */
  private void showEmptyState() {
    thumbnailContainer.removeAll();
    thumbnailContainer.add(createStatePanel(FontAwesomeSolid.IMAGES, "No images", null));
    statusLabel.setText("");
    revalidate();
    repaint();
  }

  /**
   * Shows the loading state while images are being processed.
   */
  private void showLoadingState() {
    thumbnailContainer.removeAll();
    thumbnailContainer.add(createStatePanel(FontAwesomeSolid.SPINNER, "Loading...", null));
    statusLabel.setText("Loading images...");
    revalidate();
    repaint();
  }

  /**
   * Loads images from the specified folder.
   *
   * @param folder the folder containing images
   */
  public void loadImagesFromFolder(File folder) {
    loadImagesFromFolder(folder, null);
  }

  /**
   * Loads images from the specified folder, optionally highlighting a specific file.
   *
   * @param folder the folder containing images
   * @param highlightFile optional specific file to highlight (if null, loads all images)
   */
  public void loadImagesFromFolder(File folder, File highlightFile) {
    if (folder == null || !folder.exists() || !folder.isDirectory()) {
      LOGGER.warn("Invalid folder provided: {}", folder);
      showEmptyState();
      return;
    }

    if (folder.equals(currentFolder) && !thumbnails.isEmpty() && highlightFile == null) {
      LOGGER.debug("Folder already loaded: {}", folder.getAbsolutePath());
      return;
    }

    this.currentFolder = folder;
    this.isLoading = true;

    LOGGER.info("Loading images from folder: {}", folder.getAbsolutePath());

    // Clear previous thumbnails
    clearThumbnails();
    showLoadingState();

    // Load images asynchronously
    CompletableFuture.supplyAsync(
            () -> {
              File[] files = folder.listFiles();
              if (files == null) {
                return new File[0];
              }

              File[] imageFiles = ImageLoader.filterImageFiles(files);

              if (highlightFile != null) {
                // If a specific file is to be highlighted, put it first in the list
                File[] filteredImages = Arrays.stream(imageFiles)
                    .filter(file -> file.equals(highlightFile))
                    .toArray(File[]::new);
                return filteredImages.length > 0 ? filteredImages : imageFiles;
              } else {
                Arrays.sort(imageFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                return imageFiles;
              }
            })
        .thenAccept(
            imageFiles -> {
              SwingUtilities.invokeLater(
                  () -> {
                    if (imageFiles.length == 0) {
                      showNoImagesFound();
                    } else {
                      displayImages(imageFiles);
                    }
                  });
            })
        .exceptionally(
            throwable -> {
              LOGGER.error(
                  "Error loading images from folder: {}", folder.getAbsolutePath(), throwable);
              SwingUtilities.invokeLater(() -> showErrorState(throwable.getMessage()));
              return null;
            });
  }

  /**
   * Displays the provided image files as thumbnails.
   *
   * @param imageFiles array of image files to display
   */
  private void displayImages(File[] imageFiles) {
    // Setup thumbnail container
    thumbnailContainer.removeAll();

    // Create thumbnails
    for (File imageFile : imageFiles) {
      SimpleImageThumbnail thumbnail = new SimpleImageThumbnail(imageFile);

      // Add selection listener
      thumbnail.addPropertyChangeListener(
          "thumbnailSelected",
          evt -> {
            SimpleImageThumbnail clickedThumbnail = (SimpleImageThumbnail) evt.getNewValue();
            selectThumbnail(clickedThumbnail);
          });

      thumbnails.add(thumbnail);
      thumbnailContainer.add(thumbnail);

      // Add spacing between thumbnails
      if (thumbnails.size() < imageFiles.length) {
        thumbnailContainer.add(Box.createVerticalStrut(UIConstants.SMALL_SPACING));
      }
    }

    this.isLoading = false;
    updateStatusLabel();

    // Auto-select first image
    if (!thumbnails.isEmpty()) {
      selectThumbnail(thumbnails.get(0));
    }

    revalidate();
    repaint();

    LOGGER.info(
        "Displayed {} images from folder: {}", imageFiles.length, currentFolder.getAbsolutePath());
  }

  /**
   * Selects the specified thumbnail.
   *
   * @param thumbnail the thumbnail to select
   */
  private void selectThumbnail(SimpleImageThumbnail thumbnail) {
    // Deselect previous thumbnail
    if (selectedThumbnail != null) {
      selectedThumbnail.setSelected(false);
    }

    // Select new thumbnail
    selectedThumbnail = thumbnail;
    if (selectedThumbnail != null) {
      selectedThumbnail.setSelected(true);

      // Scroll to selected thumbnail
      scrollPane.getViewport().scrollRectToVisible(selectedThumbnail.getBounds());
    }

    updateStatusLabel();
    notifySelectionChange();
  }

  /**
   * Shows a message when no images are found in the folder.
   */
  private void showNoImagesFound() {
    thumbnailContainer.removeAll();
    thumbnailContainer.add(
        createStatePanel(
            FontAwesomeSolid.EXCLAMATION_TRIANGLE, "No images found", UIConstants.WARNING_COLOR));
    statusLabel.setText("No supported images");
    this.isLoading = false;
    revalidate();
    repaint();
  }

  /**
   * Shows an error state when loading fails.
   *
   * @param errorMessage the error message to display
   */
  private void showErrorState(String errorMessage) {
    thumbnailContainer.removeAll();
    thumbnailContainer.add(
        createStatePanel(
            FontAwesomeSolid.EXCLAMATION_CIRCLE, "Error loading", UIConstants.ERROR_COLOR));
    statusLabel.setText("Error: " + errorMessage);
    this.isLoading = false;
    revalidate();
    repaint();
  }

  /**
   * Creates a state panel with icon and message.
   */
  private JPanel createStatePanel(FontAwesomeSolid icon, String message, Color iconColor) {
    JPanel panel = UIUtils.createVerticalPanel();
    panel.setBorder(
        UIUtils.createPadding(50, UIConstants.LARGE_SPACING, 50, UIConstants.LARGE_SPACING));

    // Icon
    JLabel iconLabel = new JLabel(FontIcon.of(icon, icon == FontAwesomeSolid.IMAGES ? 32 : 24));
    iconLabel.setForeground(
        iconColor != null ? iconColor : UIManager.getColor("Label.disabledForeground"));
    iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    panel.add(iconLabel);

    panel.add(
        Box.createVerticalStrut(icon == FontAwesomeSolid.IMAGES ? 15 : UIConstants.MEDIUM_SPACING));

    // Message
    JLabel messageLabel = new JLabel(message);
    messageLabel.setFont(
        messageLabel
            .getFont()
            .deriveFont(
                icon == FontAwesomeSolid.IMAGES ? Font.BOLD : Font.PLAIN,
                icon == FontAwesomeSolid.IMAGES
                    ? UIConstants.NORMAL_FONT_SIZE
                    : UIConstants.SMALL_FONT_SIZE));
    messageLabel.setForeground(
        iconColor != null ? iconColor : UIManager.getColor("Label.disabledForeground"));
    messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    panel.add(messageLabel);

    return panel;
  }

  /**
   * Updates the status label with current information.
   */
  private void updateStatusLabel() {
    if (isLoading) {
      statusLabel.setText("Loading...");
    } else if (thumbnails.isEmpty()) {
      statusLabel.setText("No images");
    } else {
      statusLabel.setText(String.format("%d images", thumbnails.size()));
    }
  }

  /**
   * Clears all thumbnails and resets the state.
   */
  private void clearThumbnails() {
    thumbnails.clear();
    selectedThumbnail = null;
    thumbnailContainer.removeAll();
  }

  /**
   * Gets the currently selected image file.
   *
   * @return selected image file, or null if none selected
   */
  public File getSelectedImageFile() {
    return selectedThumbnail != null ? selectedThumbnail.getImageFile() : null;
  }

  /**
   * Gets all image files in the current folder.
   *
   * @return list of all image files
   */
  public List<File> getAllImageFiles() {
    return thumbnails.stream().map(SimpleImageThumbnail::getImageFile).toList();
  }

  /**
   * Gets the number of images currently displayed.
   *
   * @return number of images
   */
  public int getImageCount() {
    return thumbnails.size();
  }

  /**
   * Gets the current folder being displayed.
   *
   * @return current folder, or null if none
   */
  public File getCurrentFolder() {
    return currentFolder;
  }

  /**
   * Sets the selection change listener.
   *
   * @param listener the listener to notify when selection changes
   */
  public void setSelectionChangeListener(ActionListener listener) {
    this.selectionChangeListener = listener;
  }

  /**
   * Notifies the selection change listener.
   */
  private void notifySelectionChange() {
    if (selectionChangeListener != null) {
      selectionChangeListener.actionPerformed(null);
    }
  }

  /**
   * Selects an image by its file.
   *
   * @param imageFile the image file to select
   */
  public void selectImageByFile(File imageFile) {
    if (imageFile != null && thumbnails != null) {
      for (SimpleImageThumbnail thumbnail : thumbnails) {
        if (thumbnail.getImageFile().equals(imageFile)) {
          selectThumbnail(thumbnail);
          break;
        }
      }
    }
  }

  /**
   * Refreshes the gallery by reloading the current folder.
   */
  public void refresh() {
    if (currentFolder != null) {
      loadImagesFromFolder(currentFolder);
    }
  }
}
