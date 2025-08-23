package com.scipath.scipathj.ui.components;

import com.scipath.scipathj.core.config.MainSettings;
import com.scipath.scipathj.data.model.UserROI;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Toolbar component for ROI management operations.
 * Provides buttons for creating, selecting, and managing ROIs.
 */
public class ROIToolbar extends JPanel {

  private static final Logger LOGGER = LoggerFactory.getLogger(ROIToolbar.class);

  // UI Components
  private JButton saveROIsButton;
  private JButton saveAllROIsButton;
  private JButton clearAllButton;
  private JButton featuresButton;
  private JLabel roiCountLabel;

  // ROI type filter buttons with checkboxes
  private JCheckBox vesselButton;
  private JCheckBox nucleusButton;
  private JCheckBox cytoplasmButton;
  private JCheckBox cellButton;
  private JCheckBox ignoreButton;


  // Current state
  private String currentImageFileName;
  private boolean roiCreationEnabled = false;
  private MainSettings mainSettings;

  // ROI filter state
  private boolean vesselFilterEnabled = true;
  private boolean nucleusFilterEnabled = true;
  private boolean cytoplasmFilterEnabled = true;
  private boolean cellFilterEnabled = true;
  private boolean ignoreFilterEnabled = true;

  // Listeners
  private final List<ROIToolbarListener> listeners;

  public interface ROIToolbarListener {
     void onROICreationModeChanged(UserROI.ROIType type, boolean enabled);

     void onSaveROIs(String imageFileName, File outputFile);

     void onSaveAllROIs(File outputFile);

     void onClearAllROIs();

     void onROIFilterChanged(MainSettings.ROICategory category, boolean enabled);

     void onShowROIStatistics();

     void onChangeROIType(String imageFileName, UserROI.ROIType newType);
 
     void onShowFeatures();
   }

  public ROIToolbar() {
    this.listeners = new ArrayList<>();
    initializeComponents();
    updateButtonStates();
  }

  private void initializeComponents() {
    setLayout(
        new FlowLayout(FlowLayout.LEFT, UIConstants.SMALL_SPACING, UIConstants.SMALL_SPACING));
    setBorder(UIUtils.createPadding(UIConstants.SMALL_SPACING));
    setOpaque(false);

    // Make the toolbar taller to accommodate longer text
    setPreferredSize(new Dimension(getPreferredSize().width, 80));

    // Create ROI management buttons
    createROIManagementButtons();

    // Add separator
    add(createSeparator());

    // Create status label
    createStatusLabel();

    // Add separator
    add(createSeparator());

    // Create ROI type filter buttons
    createROITypeFilterButtons();
  }


  private void createROIManagementButtons() {
    // Save ROIs button (current image) - with multiline text
    saveROIsButton = new JButton();
    saveROIsButton.setIcon(UIUtils.createIcon(FontAwesomeSolid.DOWNLOAD, UIConstants.ICON_SIZE_SMALL));
    saveROIsButton.setText("<html>Save Img ROIs<html>");
    saveROIsButton.setToolTipText("Save ROIs from current image to .roi/.zip file");
    saveROIsButton.setFocusPainted(false);
    saveROIsButton.addActionListener(e -> handleSaveROIs());
    saveROIsButton.setPreferredSize(new Dimension(140, 35)); // Slightly less tall
    add(saveROIsButton);

    // Save All ROIs button (all images)
    saveAllROIsButton = new JButton();
    saveAllROIsButton.setIcon(UIUtils.createIcon(FontAwesomeSolid.ARCHIVE, UIConstants.ICON_SIZE_SMALL));
    saveAllROIsButton.setText("Save All ROIs");
    saveAllROIsButton.setToolTipText("Save ROIs from all images to master ZIP file");
    saveAllROIsButton.setFocusPainted(false);
    saveAllROIsButton.addActionListener(e -> handleSaveAllROIs());
    saveAllROIsButton.setPreferredSize(new Dimension(140, 35)); // Slightly less tall
    add(saveAllROIsButton);

    // Clear all ROIs button
    clearAllButton = new JButton();
    clearAllButton.setIcon(UIUtils.createIcon(FontAwesomeSolid.TRASH, UIConstants.ICON_SIZE_SMALL));
    clearAllButton.setText("Clear All");
    clearAllButton.setToolTipText("Clear all ROIs for current image");
    clearAllButton.setFocusPainted(false);
    clearAllButton.addActionListener(e -> handleClearAllROIs());
    clearAllButton.setForeground(UIConstants.ERROR_COLOR);
    clearAllButton.setPreferredSize(new Dimension(140, 30)); // Slightly less tall
    add(clearAllButton);

    // Features button
    featuresButton = UIUtils.createButton("Features", UIConstants.SMALL_FONT_SIZE, e -> handleShowFeatures());
    featuresButton.setIcon(UIUtils.createIcon(FontAwesomeSolid.TABLE, UIConstants.ICON_SIZE_SMALL));
    featuresButton.setToolTipText("View extracted features");
    featuresButton.setPreferredSize(new Dimension(140, 30)); // Slightly less tall
    add(featuresButton);
  }

  private void createStatusLabel() {
    roiCountLabel =
        UIUtils.createLabel(
            "No ROIs", UIConstants.SMALL_FONT_SIZE, UIManager.getColor("Label.disabledForeground"));
    roiCountLabel.setBorder(UIUtils.createPadding(0, UIConstants.MEDIUM_SPACING, 0, 0));
    add(roiCountLabel);
  }


  private JToggleButton createToggleButton(
      FontAwesomeSolid icon, String text, String tooltip, ActionListener action) {
    JToggleButton button = new JToggleButton();
    button.setIcon(UIUtils.createIcon(icon, UIConstants.ICON_SIZE_SMALL));
    button.setText(text);
    button.setToolTipText(tooltip);
    button.setFocusPainted(false);
    button.addActionListener(action);
    button.setPreferredSize(new Dimension(120, 32));
    return button;
  }

  private JButton createButton(
      FontAwesomeSolid icon, String text, String tooltip, ActionListener action) {
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

  private void createROITypeFilterButtons() {
    // Create a panel for the filter buttons to control their layout
    JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.SMALL_SPACING, 0));
    filterPanel.setOpaque(false);

    // Fixed width to accommodate text + 5 numbers (e.g., "Cytoplasms (99999)")
    int buttonWidth = 130;
    int buttonHeight = 32;

    // Vessel filter button with checkbox
    vesselButton = createStyledFilterButton("Vessels", vesselFilterEnabled,
        e -> handleFilterChanged(MainSettings.ROICategory.VESSEL, vesselButton.isSelected()));
    vesselButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
    filterPanel.add(vesselButton);

    // Nucleus filter button with checkbox
    nucleusButton = createStyledFilterButton("Nuclei", nucleusFilterEnabled,
        e -> handleFilterChanged(MainSettings.ROICategory.NUCLEUS, nucleusButton.isSelected()));
    nucleusButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
    filterPanel.add(nucleusButton);

    // Cytoplasm filter button with checkbox
    cytoplasmButton = createStyledFilterButton("Cytoplasms", cytoplasmFilterEnabled,
        e -> handleFilterChanged(MainSettings.ROICategory.CYTOPLASM, cytoplasmButton.isSelected()));
    cytoplasmButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
    filterPanel.add(cytoplasmButton);

    // Cell filter button with checkbox
    cellButton = createStyledFilterButton("Cells", cellFilterEnabled,
        e -> handleFilterChanged(MainSettings.ROICategory.CELL, cellButton.isSelected()));
    cellButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
    filterPanel.add(cellButton);

    // Ignore filter button with checkbox
    ignoreButton = createStyledIgnoreButton("Ignore", ignoreFilterEnabled,
        e -> handleFilterChanged(null, ignoreButton.isSelected()));
    ignoreButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
    filterPanel.add(ignoreButton);

    // Statistics button
    JButton statsButton = createStyledStatsButton();
    statsButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
    filterPanel.add(statsButton);

    add(filterPanel);
  }


  private JCheckBox createStyledFilterButton(String text, boolean selected, ActionListener listener) {
    JCheckBox button = new JCheckBox(text);
    button.setSelected(selected);
    button.setFocusPainted(false);
    button.addActionListener(listener);

    // Styling for semi-transparent appearance with solid border
    button.setOpaque(false); // Must be false for transparency to work
    button.setBorderPainted(true);
    button.setBorder(new RoundedBorder(Color.BLACK, 1, 8));

    // Set background with 0.15 opacity (more transparent)
    Color originalBg = button.getBackground();
    Color semiTransparentBg = new Color(originalBg.getRed(), originalBg.getGreen(), originalBg.getBlue(), 38); // 0.15 * 255
    button.setBackground(semiTransparentBg);

    return button;
  }

  private JCheckBox createStyledIgnoreButton(String text, boolean selected, ActionListener listener) {
    return createStyledFilterButton(text, selected, listener);
  }

  private JButton createStyledStatsButton() {
    JButton button = new JButton("Stats");
    button.setFocusPainted(false);
    button.addActionListener(e -> handleShowStatistics());

    // Same styling as filter buttons
    button.setOpaque(true);
    button.setBorderPainted(true);
    button.setBorder(new RoundedBorder(Color.BLACK, 1, 8));

    // Set background with 0.15 opacity (more transparent)
    Color originalBg = button.getBackground();
    Color semiTransparentBg = new Color(originalBg.getRed(), originalBg.getGreen(), originalBg.getBlue(), 38); // 0.15 * 255
    button.setBackground(semiTransparentBg);

    // Ensure button is non-opaque so the alpha channel is respected
    button.setOpaque(false);

    // Ensure button is non-opaque so the alpha channel is respected
    button.setOpaque(false);

    return button;
  }

  /**
   * Custom border with rounded corners
   */
  private static class RoundedBorder extends AbstractBorder {
    private final Color color;
    private final int thickness;
    private final int radius;

    public RoundedBorder(Color color, int thickness, int radius) {
      this.color = color;
      this.thickness = thickness;
      this.radius = radius;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
      Graphics2D g2d = (Graphics2D) g.create();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setColor(color);
      g2d.setStroke(new BasicStroke(thickness));
      g2d.draw(new RoundRectangle2D.Float(x + thickness/2.0f, y + thickness/2.0f,
          width - thickness, height - thickness, radius, radius));
      g2d.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
      return new Insets(thickness, thickness, thickness, thickness);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
      insets.set(thickness, thickness, thickness, thickness);
      return insets;
    }
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
      roiCountLabel.setText(String.format("%d ROI%s", totalCount, totalCount == 1 ? "" : "s"));
    }

    updateButtonStates();
  }

  /**
   * Enable or disable the toolbar
   */
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);

    saveROIsButton.setEnabled(enabled);
    clearAllButton.setEnabled(enabled);

    if (!enabled) {
      // Clear selection when disabled
      roiCreationEnabled = false;
    }

    updateButtonStates();
  }

  /**
   * Clear ROI creation mode selection
   */
  public void clearCreationMode() {
    roiCreationEnabled = false;

    // Notify listeners
    listeners.forEach(
        listener -> {
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

    // Current image management buttons depend on having ROIs for current image
    saveROIsButton.setEnabled(hasROIs && isEnabled());
    clearAllButton.setEnabled(hasROIs && isEnabled());

    // Save All button depends on having any ROIs across all images
    saveAllROIsButton.setEnabled(hasAnyROIs && isEnabled());
  }


  private void handleSaveROIs() {
    if (currentImageFileName == null) {
      JOptionPane.showMessageDialog(
          this, "No image selected.", "Save ROIs", JOptionPane.WARNING_MESSAGE);
      return;
    }

    int roiCount = ROIManager.getInstance().getROICount(currentImageFileName);
    if (roiCount == 0) {
      JOptionPane.showMessageDialog(
          this, "No ROIs to save for current image.", "Save ROIs", JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    // Determine file type based on ROI count
    boolean useZipFormat = roiCount >= 2;
    String fileExtension = useZipFormat ? ".zip" : ".roi";
    String fileDescription =
        useZipFormat ? "ImageJ ROI Set files (*.zip)" : "ImageJ ROI files (*.roi)";
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

      LOGGER.info(
          "Saving {} ROIs for image '{}' to {} file '{}'",
          roiCount,
          finalImageFileName,
          useZipFormat ? "ZIP" : "ROI",
          finalOutputFile.getAbsolutePath());

      // Notify listeners
      listeners.forEach(
          listener -> {
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
      JOptionPane.showMessageDialog(
          this,
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

      LOGGER.info(
          "Saving {} ROIs from all images to master ZIP file '{}'",
          totalROICount,
          finalOutputFile.getAbsolutePath());

      // Notify listeners
      listeners.forEach(
          listener -> {
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
      JOptionPane.showMessageDialog(
          this,
          "No ROIs to clear for current image.",
          "Clear All ROIs",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    int result =
        JOptionPane.showConfirmDialog(
            this,
            String.format(
                "Clear all %d ROI%s for current image?", roiCount, roiCount == 1 ? "" : "s"),
            "Clear All ROIs",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

    if (result == JOptionPane.YES_OPTION) {
      LOGGER.info("Clearing all {} ROIs for image '{}'", roiCount, currentImageFileName);

      // Notify listeners
      listeners.forEach(
          listener -> {
            try {
              listener.onClearAllROIs();
            } catch (Exception e) {
              LOGGER.error("Error notifying ROI toolbar listener", e);
            }
          });
    }
  }

  private void handleFilterChanged(MainSettings.ROICategory category, boolean enabled) {
    // Update internal state
    if (category == MainSettings.ROICategory.VESSEL) {
      vesselFilterEnabled = enabled;
    } else if (category == MainSettings.ROICategory.NUCLEUS) {
      nucleusFilterEnabled = enabled;
    } else if (category == MainSettings.ROICategory.CYTOPLASM) {
      cytoplasmFilterEnabled = enabled;
    } else if (category == MainSettings.ROICategory.CELL) {
      cellFilterEnabled = enabled;
    } else {
      // Ignore filter
      ignoreFilterEnabled = enabled;
    }

    // Notify listeners
    listeners.forEach(listener -> {
      try {
        listener.onROIFilterChanged(category, enabled);
      } catch (Exception e) {
        LOGGER.error("Error notifying ROI filter change", e);
      }
    });

    LOGGER.debug("ROI filter changed: {} -> {}", category, enabled);
  }

  private void handleShowStatistics() {
    // Notify listeners to show statistics dialog
    listeners.forEach(listener -> {
      try {
        listener.onShowROIStatistics();
      } catch (Exception e) {
        LOGGER.error("Error notifying ROI statistics request", e);
      }
    });

    LOGGER.debug("Requested ROI statistics display");
  }

  private void handleShowFeatures() {
    // Notify listeners to show features dialog
    listeners.forEach(listener -> {
      try {
        listener.onShowFeatures();
      } catch (Exception e) {
        LOGGER.error("Error notifying ROI features request", e);
      }
    });

    LOGGER.debug("Requested features display");
  }


  /**
   * Set the main settings for color and appearance configuration
   */
  public void setMainSettings(MainSettings settings) {
    this.mainSettings = settings;
    updateButtonColors();
    LOGGER.debug("Updated main settings in ROI toolbar");
  }

  /**
   * Update button colors based on main settings
   */
  private void updateButtonColors() {
    if (mainSettings == null) return;

    // Update button colors and text colors for adaptive visibility
    updateButtonColor(vesselButton, MainSettings.ROICategory.VESSEL);
    updateButtonColor(nucleusButton, MainSettings.ROICategory.NUCLEUS);
    updateButtonColor(cytoplasmButton, MainSettings.ROICategory.CYTOPLASM);
    updateButtonColor(cellButton, MainSettings.ROICategory.CELL);
  }

  /**
   * Update a single button's color and text color for adaptive visibility
   */
  private void updateButtonColor(JCheckBox button, MainSettings.ROICategory category) {
    if (button == null || mainSettings == null) return;

    MainSettings.ROIAppearanceSettings settings = mainSettings.getSettingsForCategory(category);
    Color bgColor = settings.borderColor();

    // Set button background with transparency
    Color semiTransparentBg = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 38); // 0.15 * 255
    button.setBackground(semiTransparentBg);
    button.setOpaque(false); // Must be false for transparency

    // Calculate adaptive text color (black or white) for contrast
    Color textColor = getAdaptiveTextColor(bgColor);
    button.setForeground(textColor);
  }

  /**
   * Calculate adaptive text color (black or white) based on background color brightness
   */
  private Color getAdaptiveTextColor(Color bgColor) {
    // Calculate luminance using the standard formula
    double luminance = 0.299 * bgColor.getRed() + 0.587 * bgColor.getGreen() + 0.114 * bgColor.getBlue();
    // Use white text on dark backgrounds, black text on light backgrounds
    return luminance < 128 ? Color.WHITE : Color.BLACK;
  }

  /**
   * Update ROI counts on buttons
   */
  public void updateROITypeCounts(java.util.Map<MainSettings.ROICategory, Integer> counts) {
    if (counts == null) return;

    // Debug logging to understand the counts
    // LOGGER.debug("Updating ROI counts: Vessels={}, Nuclei={}, Cytoplasms={}, Cells={}, Ignored={}",
        // counts.getOrDefault(MainSettings.ROICategory.VESSEL, 0),
        // counts.getOrDefault(MainSettings.ROICategory.NUCLEUS, 0),
        // counts.getOrDefault(MainSettings.ROICategory.CYTOPLASM, 0),
        // counts.getOrDefault(MainSettings.ROICategory.CELL, 0),
        // counts.getOrDefault(null, 0));

    // Update button text with counts
    updateButtonText(vesselButton, "Vessels", counts.getOrDefault(MainSettings.ROICategory.VESSEL, 0));
    updateButtonText(nucleusButton, "Nuclei", counts.getOrDefault(MainSettings.ROICategory.NUCLEUS, 0));
    updateButtonText(cytoplasmButton, "Cytoplasms", counts.getOrDefault(MainSettings.ROICategory.CYTOPLASM, 0));
    updateButtonText(cellButton, "Cells", counts.getOrDefault(MainSettings.ROICategory.CELL, 0));

    // For ignore button, use the actual ignored ROI count (stored with null key)
    int ignoredROIs = counts.getOrDefault(null, 0);
    updateButtonText(ignoreButton, "Ignore", ignoredROIs);
  }

  /**
   * Update button text to include count
   */
  private void updateButtonText(JToggleButton button, String baseText, int count) {
    if (button == null) return;
    button.setText(String.format("%s (%d)", baseText, count));
  }

  private void updateButtonText(JCheckBox button, String baseText, int count) {
    if (button == null) return;
    button.setText(String.format("%s (%d)", baseText, count));
  }
}
