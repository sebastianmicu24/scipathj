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
 * Main menu panel for SciPathJ application.
 *
 * <p>This component displays the three main application functions:
 * 1. Perform Analysis - Run segmentation and classification on images
 * 2. Create Dataset - Select cells and create custom classification models
 * 3. Visualize Results - View and analyze previously processed data</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class MainMenuPanel extends JPanel {

  private ActionListener optionSelectedListener;
  private MainMenuOption selectedOption;
  private final List<MainMenuOptionButton> optionButtons;

  /**
   * Main menu option enumeration.
   */
  public enum MainMenuOption {
    ANALYSIS("full_he", "Perform Analysis", "Run segmentation and classification on tissue images",
             FontAwesomeSolid.MICROSCOPE, UIConstants.SELECTION_COLOR),
    DATASET_CREATION("dataset_creator", "Create Dataset", "Select cells and create custom classification models",
                    FontAwesomeSolid.PLUS_CIRCLE, new Color(34, 139, 34)),
    VISUALIZATION("View_Results", "Visualize Results", "View and analyze previously processed data",
                 FontAwesomeSolid.CHART_BAR, new Color(255, 140, 0));

    private final String pipelineId;
    private final String displayName;
    private final String description;
    private final FontAwesomeSolid icon;
    private final Color color;

    MainMenuOption(String pipelineId, String displayName, String description,
                  FontAwesomeSolid icon, Color color) {
      this.pipelineId = pipelineId;
      this.displayName = displayName;
      this.description = description;
      this.icon = icon;
      this.color = color;
    }

    public String getPipelineId() { return pipelineId; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public FontAwesomeSolid getIcon() { return icon; }
    public Color getColor() { return color; }

    public PipelineInfo getPipelineInfo() {
      return PipelineRegistry.getPipelineById(pipelineId);
    }
  }

  /**
   * Creates a new main menu panel.
   */
  public MainMenuPanel() {
    this.optionButtons = new ArrayList<>();
    initializeComponents();
  }

  /**
   * Initializes the panel components.
   */
  private void initializeComponents() {
    setLayout(new BorderLayout());
    setBorder(UIUtils.createPadding(UIConstants.EXTRA_LARGE_SPACING));

    // Title
    add(UIUtils.createTitleLabel("SciPathJ - Main Menu"), BorderLayout.NORTH);

    // Main options
    add(createMainOptionsPanel(), BorderLayout.CENTER);

    // Footer with app info
    add(createFooterPanel(), BorderLayout.SOUTH);
  }

  /**
   * Creates the panel containing the three main options.
   */
  private JPanel createMainOptionsPanel() {
    JPanel panel = UIUtils.createVerticalPanel();

    for (MainMenuOption option : MainMenuOption.values()) {
      MainMenuOptionButton optionButton = new MainMenuOptionButton(option);
      optionButtons.add(optionButton);
      panel.add(optionButton);

      if (option != MainMenuOption.values()[MainMenuOption.values().length - 1]) {
        panel.add(Box.createVerticalStrut(UIConstants.LARGE_SPACING));
      }
    }

    return panel;
  }

  /**
   * Creates the footer panel with application information.
   */
  private JPanel createFooterPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING, 0, 0, 0));
    panel.setOpaque(false);

    // Version and status info
    String footerText = "Ready to process histopathological images";
    if (hasDisabledOptions()) {
      footerText += " • Some features are under development";
    }

    JLabel footerLabel = new JLabel(footerText);
    footerLabel.setFont(footerLabel.getFont().deriveFont(Font.ITALIC, UIConstants.SMALL_FONT_SIZE));
    footerLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    footerLabel.setHorizontalAlignment(SwingConstants.CENTER);

    panel.add(footerLabel, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Checks if any main menu options are disabled.
   */
  private boolean hasDisabledOptions() {
    return MainMenuOption.values().length != PipelineRegistry.getEnabledPipelines().size();
  }

  /**
   * Custom panel representing a selectable main menu option.
   */
  private class MainMenuOptionButton extends JPanel {
    private final MainMenuOption option;
    private boolean selected = false;
    private boolean hovered = false;

    public MainMenuOptionButton(MainMenuOption option) {
      this.option = option;
      initializeButton();
    }

    private void initializeButton() {
      setLayout(new BorderLayout());
      setPreferredSize(new Dimension(700, 100));
      setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
      setCursor(option.getPipelineInfo().isEnabled()
          ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
          : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

      // Content panel
      JPanel contentPanel = new JPanel(new BorderLayout());
      contentPanel.setOpaque(false);
      contentPanel.setBorder(
          UIUtils.createPadding(
              UIConstants.LARGE_SPACING,
              UIConstants.EXTRA_LARGE_SPACING,
              UIConstants.LARGE_SPACING,
              UIConstants.EXTRA_LARGE_SPACING));

      // Left side - Icon and title
      JPanel leftPanel = new JPanel(new BorderLayout());
      leftPanel.setOpaque(false);

      // Icon with padding
      FontIcon icon = FontIcon.of(option.getIcon(), 32, option.getColor());
      JLabel iconLabel = new JLabel(icon);
      iconLabel.setBorder(UIUtils.createPadding(0, 0, 0, UIConstants.MEDIUM_SPACING));
      leftPanel.add(iconLabel, BorderLayout.WEST);

      // Title and description
      JPanel textPanel = new JPanel(new BorderLayout());
      textPanel.setOpaque(false);
      textPanel.setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING, 0, 0, 0));

      JLabel titleLabel = new JLabel(option.getDisplayName());
      titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, UIConstants.LARGE_FONT_SIZE + 2));

      JLabel descLabel = new JLabel("<html><div style='margin-top: 4px; width: 400px;'>" +
          option.getDescription() + "</div></html>");
      descLabel.setFont(descLabel.getFont().deriveFont(UIConstants.SMALL_FONT_SIZE + 1));
      descLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

      textPanel.add(titleLabel, BorderLayout.NORTH);
      textPanel.add(descLabel, BorderLayout.CENTER);

      leftPanel.add(textPanel, BorderLayout.CENTER);

      contentPanel.add(leftPanel, BorderLayout.WEST);

      // Right side - Status and arrow
      JPanel rightPanel = new JPanel(new BorderLayout());
      rightPanel.setOpaque(false);

      if (!option.getPipelineInfo().isEnabled()) {
        // Coming soon label
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.setOpaque(false);

        FontIcon clockIcon = FontIcon.of(FontAwesomeSolid.CLOCK, 14, UIManager.getColor("Label.disabledForeground"));
        JLabel comingSoonLabel = new JLabel("Coming Soon");
        comingSoonLabel.setFont(comingSoonLabel.getFont().deriveFont(Font.ITALIC, UIConstants.SMALL_FONT_SIZE));
        comingSoonLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        statusPanel.add(new JLabel(clockIcon));
        statusPanel.add(comingSoonLabel);

        rightPanel.add(statusPanel, BorderLayout.CENTER);
      } else {
        // Arrow icon for enabled options
        FontIcon arrowIcon = FontIcon.of(FontAwesomeSolid.CHEVRON_RIGHT, 20, UIManager.getColor("Label.disabledForeground"));
        rightPanel.add(new JLabel(arrowIcon), BorderLayout.EAST);
      }

      contentPanel.add(rightPanel, BorderLayout.EAST);

      add(contentPanel, BorderLayout.CENTER);

      // Add mouse listeners for interaction
      addMouseListener(
          new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              if (option.getPipelineInfo().isEnabled()) {
                selectOption();
              }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
              if (option.getPipelineInfo().isEnabled()) {
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

    private void selectOption() {
      // Deselect all other options
      for (MainMenuOptionButton button : optionButtons) {
        button.setSelected(false);
      }

      // Select this option
      setSelected(true);
      selectedOption = option;

      // Notify listener
      if (optionSelectedListener != null) {
        optionSelectedListener.actionPerformed(
            new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, option.getPipelineId()));
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
      int arc = 15;

      // Determine colors based on theme and state
      Color backgroundColor;
      Color borderColor;

      if (!option.getPipelineInfo().isEnabled()) {
        backgroundColor = UIManager.getColor("Panel.background");
        borderColor = UIManager.getColor("Component.borderColor");
      } else if (selected) {
        backgroundColor = UIUtils.getBackgroundColor(true);
        borderColor = option.getColor();
      } else if (hovered) {
        backgroundColor = UIUtils.getBackgroundColor(false);
        borderColor = option.getColor().brighter();
      } else {
        backgroundColor = UIManager.getColor("Panel.background");
        borderColor = UIUtils.getBorderColor(false);
      }

      // Draw background
      g2d.setColor(backgroundColor);
      g2d.fillRoundRect(0, 0, width, height, arc, arc);

      // Draw border
      g2d.setColor(borderColor);
      g2d.setStroke(new BasicStroke(selected ? 3f : 1f));
      g2d.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);

      g2d.dispose();
    }
  }

  /**
   * Sets the listener for option selection events.
   *
   * @param listener the action listener
   */
  public void setOptionSelectedListener(ActionListener listener) {
    this.optionSelectedListener = listener;
  }

  /**
   * Gets the currently selected main menu option.
   *
   * @return selected option, or null if none selected
   */
  public MainMenuOption getSelectedOption() {
    return selectedOption;
  }

  /**
   * Gets the pipeline info for the currently selected option.
   *
   * @return selected pipeline info, or null if none selected
   */
  public PipelineInfo getSelectedPipeline() {
    return selectedOption != null ? selectedOption.getPipelineInfo() : null;
  }

  /**
   * Selects an option by pipeline ID.
   *
   * @param pipelineId the pipeline ID to select
   */
  public void selectOption(String pipelineId) {
    for (MainMenuOption option : MainMenuOption.values()) {
      if (option.getPipelineId().equals(pipelineId) && option.getPipelineInfo().isEnabled()) {
        // Find and select the corresponding option button
        for (MainMenuOptionButton button : optionButtons) {
          if (button.option == option) {
            button.selectOption();
            break;
          }
        }
        break;
      }
    }
  }
}