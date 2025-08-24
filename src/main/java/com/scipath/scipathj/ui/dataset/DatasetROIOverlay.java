package com.scipath.scipathj.ui.dataset;

import com.scipath.scipathj.roi.model.UserROI;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * ROI overlay component specifically for dataset creation.
 *
 * <p>This component displays ROIs on top of images and handles user interaction
 * for assigning ROIs to classes during dataset creation.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class DatasetROIOverlay extends JComponent {

  private List<UserROI> currentRois;
  private Consumer<UserROI> roiClickListener;
  private UserROI hoveredRoi;
  private Point mousePosition;

  public DatasetROIOverlay() {
    setOpaque(false);
    setupMouseInteraction();
  }

  /**
   * Sets the ROIs to display.
   */
  public void setRois(List<UserROI> rois) {
    this.currentRois = rois;
    repaint();
  }

  /**
   * Sets the ROI click listener.
   */
  public void setRoiClickListener(Consumer<UserROI> listener) {
    this.roiClickListener = listener;
  }

  /**
   * Sets up mouse interaction for ROI selection.
   */
  private void setupMouseInteraction() {
    MouseAdapter mouseAdapter = new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        mousePosition = e.getPoint();
        hoveredRoi = findRoiAtPosition(mousePosition);
        setCursor(hoveredRoi != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) :
                  Cursor.getDefaultCursor());
        repaint();
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        if (roiClickListener != null && hoveredRoi != null) {
          roiClickListener.accept(hoveredRoi);
        }
      }
    };

    addMouseListener(mouseAdapter);
    addMouseMotionListener(mouseAdapter);
  }

  /**
   * Finds the ROI at the given position.
   */
  private UserROI findRoiAtPosition(Point position) {
    if (currentRois == null) {
      return null;
    }

    // Check ROIs in reverse order (top to bottom)
    for (int i = currentRois.size() - 1; i >= 0; i--) {
      UserROI roi = currentRois.get(i);
      if (roi.contains(position.x, position.y)) {
        return roi;
      }
    }

    return null;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (currentRois == null || currentRois.isEmpty()) {
      return;
    }

    Graphics2D g2d = (Graphics2D) g.create();
    UIUtils.setupRenderingHints(g2d);

    // Draw all ROIs
    for (UserROI roi : currentRois) {
      boolean isHovered = roi.equals(hoveredRoi);
      drawRoi(g2d, roi, isHovered);
    }

    g2d.dispose();
  }

  /**
   * Draws a single ROI.
   */
  private void drawRoi(Graphics2D g2d, UserROI roi, boolean isHovered) {
    // Get ROI bounds
    Rectangle bounds = roi.getBounds();

    // Choose color based on ROI type and hover state
    Color roiColor = getRoiColor(roi, isHovered);

    // Draw ROI shape
    g2d.setColor(roiColor);
    g2d.setStroke(new BasicStroke(isHovered ? 3f : 2f));

    switch (roi.getType()) {
      case NUCLEUS:
      case CELL:
        // Draw as oval
        g2d.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
        break;
      case CYTOPLASM:
        // Draw as rectangle with rounded corners
        g2d.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height,
                         bounds.width / 4, bounds.height / 4);
        break;
      case VESSEL:
      case COMPLEX_SHAPE:
      default:
        // Draw as rectangle
        g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        break;
    }

    // Draw label if hovered
    if (isHovered) {
      drawRoiLabel(g2d, roi, bounds);
    }
  }

  /**
   * Gets the color for an ROI.
   */
  private Color getRoiColor(UserROI roi, boolean isHovered) {
    Color baseColor;
    switch (roi.getType()) {
      case NUCLEUS:
        baseColor = Color.BLUE;
        break;
      case CYTOPLASM:
        baseColor = Color.GREEN;
        break;
      case CELL:
        baseColor = Color.ORANGE;
        break;
      case VESSEL:
      case COMPLEX_SHAPE:
      default:
        baseColor = Color.RED;
        break;
    }

    // Brighten color if hovered
    if (isHovered) {
      return baseColor.brighter();
    }

    return baseColor;
  }

  /**
   * Draws a label for the ROI when hovered.
   */
  private void drawRoiLabel(Graphics2D g2d, UserROI roi, Rectangle bounds) {
    String label = roi.getName();
    if (label == null || label.isEmpty()) {
      label = roi.getType().toString();
    }

    // Set up font
    Font labelFont = getFont().deriveFont(Font.BOLD, 12f);
    g2d.setFont(labelFont);

    // Calculate label position
    FontMetrics fm = g2d.getFontMetrics();
    int labelWidth = fm.stringWidth(label);
    int labelHeight = fm.getHeight();

    int labelX = bounds.x + (bounds.width - labelWidth) / 2;
    int labelY = bounds.y - labelHeight - 5;

    // Ensure label stays within component bounds
    if (labelX < 5) labelX = 5;
    if (labelX + labelWidth > getWidth() - 5) labelX = getWidth() - labelWidth - 5;
    if (labelY < 5) labelY = bounds.y + bounds.height + labelHeight + 5;

    // Draw label background
    g2d.setColor(new Color(0, 0, 0, 180));
    g2d.fillRoundRect(labelX - 4, labelY - labelHeight + 2, labelWidth + 8, labelHeight, 4, 4);

    // Draw label text
    g2d.setColor(Color.WHITE);
    g2d.drawString(label, labelX, labelY);
  }

  /**
   * Updates the class assignment for an ROI.
   */
  public void updateRoiClass(UserROI roi, String className) {
    // TODO: Update ROI appearance based on class assignment
    // For now, this is a placeholder
    repaint();
  }
}