package com.scipath.scipathj.ui.components;

import com.scipath.scipathj.ui.model.PipelineInfo;
import com.scipath.scipathj.ui.model.PipelineRegistry;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Panel for selecting analysis pipelines.
 *
 * <p>This component displays available pipelines in a user-friendly format,
 * allowing users to select which analysis pipeline to use.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class PipelineSelectionPanel extends JPanel {

  private ActionListener continueListener;
  private PipelineInfo selectedPipeline;
  private final List<PipelineBox> pipelineBoxes;
  private JButton continueButton;

  /**
   * Creates a new pipeline selection panel.
   */
  public PipelineSelectionPanel() {
    this.pipelineBoxes = new ArrayList<>();
    initializeComponents();
  }

  /**
   * Initializes the panel components.
   */
  private void initializeComponents() {
    setLayout(new BorderLayout());
    setBorder(UIUtils.createPadding(UIConstants.EXTRA_LARGE_SPACING));

    add(UIUtils.createTitleLabel("Select Analysis Pipeline"), BorderLayout.NORTH);
    add(createPipelinesPanel(), BorderLayout.CENTER);
    add(createButtonPanel(), BorderLayout.SOUTH);
  }

  /**
   * Creates the panel containing pipeline options.
   *
   * @return panel with pipeline boxes
   */
  private JPanel createPipelinesPanel() {
    JPanel panel = UIUtils.createVerticalPanel();

    List<PipelineInfo> pipelines = PipelineRegistry.getAllPipelines();

    for (int i = 0; i < pipelines.size(); i++) {
      PipelineInfo pipeline = pipelines.get(i);
      PipelineBox pipelineBox = new PipelineBox(pipeline);
      pipelineBoxes.add(pipelineBox);
      panel.add(pipelineBox);

      if (i < pipelines.size() - 1) {
        panel.add(Box.createVerticalStrut(UIConstants.MEDIUM_SPACING + UIConstants.SMALL_SPACING));
      }
    }

    return panel;
  }

  /**
   * Custom panel representing a selectable pipeline box.
   */
  private class PipelineBox extends JPanel {
    private final PipelineInfo pipeline;
    private boolean selected = false;
    private boolean hovered = false;

    public PipelineBox(PipelineInfo pipeline) {
      this.pipeline = pipeline;
      initializeBox();
    }

    private void initializeBox() {
      setLayout(new BorderLayout());
      setPreferredSize(new Dimension(600, 80));
      setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
      setCursor(
          pipeline.isEnabled()
              ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
              : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

      // Content panel
      JPanel contentPanel = new JPanel(new BorderLayout());
      contentPanel.setOpaque(false);
      contentPanel.setBorder(
          UIUtils.createPadding(
              UIConstants.MEDIUM_SPACING + UIConstants.SMALL_SPACING,
              UIConstants.LARGE_SPACING,
              UIConstants.MEDIUM_SPACING + UIConstants.SMALL_SPACING,
              UIConstants.LARGE_SPACING));

      // Header with name and status
      JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      headerPanel.setOpaque(false);

      JLabel nameLabel = new JLabel(pipeline.getDisplayName());
      nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, UIConstants.LARGE_FONT_SIZE));
      headerPanel.add(nameLabel);

      if (!pipeline.isEnabled()) {
        JLabel statusLabel = new JLabel(" (Coming Soon)");
        statusLabel.setFont(
            statusLabel.getFont().deriveFont(Font.ITALIC, UIConstants.SMALL_FONT_SIZE + 1f));
        statusLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        headerPanel.add(statusLabel);
      }

      contentPanel.add(headerPanel, BorderLayout.NORTH);

      // Description
      JLabel descLabel =
          new JLabel(
              "<html><div style='margin-top: 5px;'>" + pipeline.getDescription() + "</div></html>");
      descLabel.setFont(descLabel.getFont().deriveFont(UIConstants.SMALL_FONT_SIZE + 1f));
      descLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
      contentPanel.add(descLabel, BorderLayout.CENTER);

      add(contentPanel, BorderLayout.CENTER);

      // Add mouse listeners for interaction
      addMouseListener(
          new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              if (pipeline.isEnabled()) {
                selectBox();
              }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
              if (pipeline.isEnabled()) {
                hovered = true;
                repaint();
              }
            }

            @Override
            public void mouseExited(MouseEvent e) {
              hovered = false;
              repaint();
            }
          });
    }

    private void selectBox() {
      // Deselect all other boxes
      for (PipelineBox box : pipelineBoxes) {
        box.setSelected(false);
      }

      // Select this box
      setSelected(true);
      selectedPipeline = pipeline;

      // Update continue button state
      if (continueButton != null) {
        continueButton.setEnabled(true);
      }
    }

    public void setSelected(boolean selected) {
      this.selected = selected;
      repaint();
    }

    public boolean isSelected() {
      return selected;
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);

      Graphics2D g2d = (Graphics2D) g.create();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      int width = getWidth();
      int height = getHeight();
      int arc = 12;

      // Determine colors based on theme and state
      Color backgroundColor;
      Color borderColor;

      if (!pipeline.isEnabled()) {
        backgroundColor = UIManager.getColor("Panel.background");
        borderColor = UIManager.getColor("Component.borderColor");
      } else if (selected) {
        backgroundColor = UIUtils.getBackgroundColor(true);
        borderColor = UIConstants.SELECTION_COLOR;
      } else if (hovered) {
        backgroundColor = UIUtils.getBackgroundColor(false);
        borderColor = UIManager.getColor("Component.focusColor");
      } else {
        backgroundColor = UIManager.getColor("Panel.background");
        borderColor = UIUtils.getBorderColor(false);
      }

      // Draw background
      g2d.setColor(backgroundColor);
      g2d.fillRoundRect(0, 0, width, height, arc, arc);

      // Draw border
      g2d.setColor(borderColor);
      g2d.setStroke(new BasicStroke(selected ? 2f : 1f));
      g2d.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);

      g2d.dispose();
    }
  }

  /**
   * Creates the button panel with continue button.
   *
   * @return button panel
   */
  private JPanel createButtonPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    panel.setBorder(UIUtils.createPadding(UIConstants.EXTRA_LARGE_SPACING, 0, 0, 0));
    panel.setOpaque(false);

    continueButton =
        UIUtils.createStandardButton("Continue", FontIcon.of(FontAwesomeSolid.ARROW_RIGHT, 16));
    continueButton.setPreferredSize(new Dimension(130, 40));
    continueButton.setEnabled(false);

    continueButton.addActionListener(
        e -> {
          if (selectedPipeline != null && continueListener != null) {
            continueListener.actionPerformed(e);
          }
        });

    panel.add(continueButton);
    return panel;
  }

  /**
   * Sets the listener for continue button events.
   *
   * @param listener the action listener
   */
  public void setContinueListener(ActionListener listener) {
    this.continueListener = listener;
  }

  /**
   * Sets the listener for pipeline selection events.
   *
   * @param listener the action listener
   */
  public void setSelectionListener(ActionListener listener) {
    this.continueListener = listener;
  }

  /**
   * Gets the currently selected pipeline.
   *
   * @return selected pipeline, or null if none selected
   */
  public PipelineInfo getSelectedPipeline() {
    return selectedPipeline;
  }

  /**
   * Selects a pipeline by ID.
   *
   * @param pipelineId the pipeline ID to select
   */
  public void selectPipeline(String pipelineId) {
    PipelineInfo pipeline = PipelineRegistry.getPipelineById(pipelineId);
    if (pipeline != null && pipeline.isEnabled()) {
      // Find and select the corresponding pipeline box
      for (PipelineBox box : pipelineBoxes) {
        if (box.pipeline.getId().equals(pipelineId)) {
          box.selectBox();
          break;
        }
      }
    }
  }
}
