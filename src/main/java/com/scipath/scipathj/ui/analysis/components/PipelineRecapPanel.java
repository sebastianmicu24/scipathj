package com.scipath.scipathj.ui.analysis.components;

import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.ui.analysis.dialogs.settings.*;
import com.scipath.scipathj.ui.model.PipelineInfo;
import com.scipath.scipathj.ui.themes.ThemeManager;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Panel that displays a recap of the selected pipeline steps.
 */
public class PipelineRecapPanel extends JPanel {

  private final ConfigurationManager configurationManager;
  private PipelineInfo currentPipeline;
  private JLabel pipelineNameLabel;
  private JPanel stepsPanel;

  public PipelineRecapPanel(ConfigurationManager configurationManager) {
    this.configurationManager = configurationManager;
    initializeComponents();
  }

  private void initializeComponents() {
    setLayout(new BorderLayout());
    setBorder(UIUtils.createPadding(0, 0));
    setOpaque(false);

    pipelineNameLabel =
        UIUtils.createBoldLabel("No pipeline selected", UIConstants.SUBTITLE_FONT_SIZE);
    pipelineNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
    pipelineNameLabel.setBorder(UIUtils.createPadding(0, 0, UIConstants.EXTRA_LARGE_SPACING, 0));
    add(pipelineNameLabel, BorderLayout.NORTH);

    JPanel stepsContainer = UIUtils.createPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    stepsPanel = UIUtils.createHorizontalPanel();
    stepsContainer.add(stepsPanel);
    add(stepsContainer, BorderLayout.CENTER);

    setVisible(false);
  }

  public void setPipeline(PipelineInfo pipeline) {
    this.currentPipeline = pipeline;

    if (pipeline == null) {
      setVisible(false);
      return;
    }

    // Remove the pipeline name title to make more space
    pipelineNameLabel.setText("");
    // pipelineNameLabel.setText(pipeline.getDisplayName());
    updateStepsDisplay();
    setVisible(true);
    revalidate();
    repaint();
  }

  private void updateStepsDisplay() {
    stepsPanel.removeAll();

    if (currentPipeline == null) return;

    String[] steps = currentPipeline.getSteps();
    for (int i = 0; i < steps.length; i++) {
      stepsPanel.add(createStepBox(steps[i], i + 1));

      if (i < steps.length - 1) {
        JLabel arrow =
            UIUtils.createIconLabel(
                FontAwesomeSolid.ARROW_RIGHT, 18, UIManager.getColor("Label.disabledForeground"));
        arrow.setBorder(UIUtils.createPadding(0, UIConstants.MEDIUM_SPACING));
        stepsPanel.add(arrow);
      }
    }
  }

  private JPanel createStepBox(String stepName, int stepNumber) {
    return new StepBox(stepName, stepNumber);
  }

  private class StepBox extends JPanel {
    private final String stepName;
    private final int stepNumber;
    private boolean isHovered = false;

    public StepBox(String stepName, int stepNumber) {
      this.stepName = stepName;
      this.stepNumber = stepNumber;

      setLayout(new BorderLayout());
      setOpaque(false);
      setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING, UIConstants.LARGE_SPACING));
      setPreferredSize(new Dimension(140, 90));
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

      setupMouseHandlers();
      add(createContent(), BorderLayout.CENTER);
    }

    private void setupMouseHandlers() {
      MouseAdapter mouseHandler =
          new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              openSettingsDialog();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
              isHovered = true;
              repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
              isHovered = false;
              repaint();
            }
          };

      addMouseListener(mouseHandler);
    }

    private JPanel createContent() {
      JPanel content = UIUtils.createPanel(new BorderLayout());

      // Number badge container (always centered)
      JPanel numberContainer = UIUtils.createPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
      numberContainer.add(createNumberBadge());
      content.add(numberContainer, BorderLayout.NORTH);

      // Step name
      JLabel nameLabel =
          UIUtils.createLabel(
              "<html><center>" + stepName + "</center></html>", UIConstants.TINY_FONT_SIZE, null);
      nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
      nameLabel.setBorder(UIUtils.createPadding(UIConstants.SMALL_SPACING, 0, 0, 0));
      content.add(nameLabel, BorderLayout.CENTER);

      return content;
    }

    private void openSettingsDialog() {
      Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
      JDialog dialog = null;

      // Create appropriate settings dialog based on step number
      // Based on the actual pipeline: Vessel Segmentation, Nuclear Segmentation, Cell Creation,
      // Feature Extraction, Cell Classification, Final Analysis
      switch (stepNumber) {
        case 1: // Vessel Segmentation
          dialog = new VesselSegmentationSettingsDialog(parentFrame, configurationManager);
          break;
        case 2: // Nuclear Segmentation
          dialog = new NuclearSegmentationSettingsDialog(parentFrame, configurationManager);
          break;
        case 3: // Cell Creation (Cytoplasm Segmentation)
          dialog = new CytoplasmSegmentationSettingsDialog(parentFrame, configurationManager);
          break;
        case 4: // Feature Extraction
          dialog = new FeatureExtractionSettingsDialog(parentFrame);
          break;
        case 5: // Cell Classification
          dialog = new ClassificationSettingsDialog(parentFrame);
          break;
        case 6: // Final Analysis
          dialog = new FinalAnalysisSettingsDialog(parentFrame);
          break;
        default:
          // Fallback to vessel segmentation dialog
          dialog = new VesselSegmentationSettingsDialog(parentFrame, configurationManager);
          break;
      }

      if (dialog != null) {
        dialog.setVisible(true);
      }
    }

    private JPanel createNumberBadge() {
      JPanel badge =
          new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
              super.paintComponent(g);
              Graphics2D g2d = (Graphics2D) g.create();
              UIUtils.setupRenderingHints(g2d);
              g2d.setColor(getStepNumberColor(stepNumber));
              g2d.fillOval(0, 0, 28, 28);
              g2d.dispose();
            }
          };
      badge.setLayout(new BorderLayout());
      badge.setOpaque(false);
      badge.setPreferredSize(new Dimension(28, 28));

      JLabel numberLabel =
          UIUtils.createBoldLabel(String.valueOf(stepNumber), UIConstants.NORMAL_FONT_SIZE);
      numberLabel.setHorizontalAlignment(SwingConstants.CENTER);
      numberLabel.setForeground(Color.WHITE);
      badge.add(numberLabel, BorderLayout.CENTER);

      return badge;
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D) g.create();
      UIUtils.setupRenderingHints(g2d);

      // Draw background with hover effect
      Color bgColor = getStepColor(stepNumber);
      if (isHovered) {
        // Slightly brighter when hovered
        int alpha = ThemeManager.isDarkTheme() ? 40 : 25;
        bgColor = UIUtils.withAlpha(getBaseStepColor(stepNumber), alpha);
      }
      g2d.setColor(bgColor);
      g2d.fillRoundRect(
          0, 0, getWidth(), getHeight(), UIConstants.BORDER_RADIUS, UIConstants.BORDER_RADIUS);

      // Draw border
      g2d.setColor(UIManager.getColor("Component.borderColor"));
      g2d.drawRoundRect(
          0,
          0,
          getWidth() - 1,
          getHeight() - 1,
          UIConstants.BORDER_RADIUS,
          UIConstants.BORDER_RADIUS);

      // Draw gear icon in top-right corner (theme-aware)
      drawGearIcon(g2d);

      g2d.dispose();
    }

    private void drawGearIcon(Graphics2D g2d) {
      // Get theme-appropriate color for the gear icon
      Color gearColor =
          ThemeManager.isDarkTheme()
              ? new Color(200, 200, 200, 180)
              : // Light gray for dark theme
              new Color(100, 100, 100, 180); // Dark gray for light theme

      // Create gear icon
      FontIcon gearIcon = FontIcon.of(FontAwesomeSolid.COG, 12, gearColor);

      // Position in top-right corner with some padding
      int iconSize = 12;
      int padding = 6;
      int x = getWidth() - iconSize - padding;
      int y = padding;

      // Draw the icon
      gearIcon.paintIcon(this, g2d, x, y);
    }
  }

  private Color getStepColor(int stepNumber) {
    int alpha = ThemeManager.isDarkTheme() ? 25 : 15;
    return UIUtils.withAlpha(getBaseStepColor(stepNumber), alpha);
  }

  private Color getStepNumberColor(int stepNumber) {
    return getBaseStepColor(stepNumber);
  }

  private Color getBaseStepColor(int stepNumber) {
    switch (stepNumber % 6) {
      case 1:
        return new Color(70, 130, 180); // Steel blue
      case 2:
        return new Color(60, 150, 60); // Forest green
      case 3:
        return new Color(200, 100, 50); // Dark orange
      case 4:
        return new Color(180, 70, 130); // Dark pink
      case 5:
        return new Color(130, 70, 180); // Dark purple
      default:
        return new Color(180, 150, 50); // Dark yellow
    }
  }

  public PipelineInfo getCurrentPipeline() {
    return currentPipeline;
  }

  public void clearPipeline() {
    setPipeline(null);
  }
}
