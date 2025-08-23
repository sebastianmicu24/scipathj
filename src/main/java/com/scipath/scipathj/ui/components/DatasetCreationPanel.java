package com.scipath.scipathj.ui.components;

import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import javax.swing.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Panel for dataset creation functionality.
 *
 * <p>This panel allows users to select cells and create custom classification models.
 * Currently a placeholder implementation that will be expanded in future development.</p>
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class DatasetCreationPanel extends JPanel {

  /**
   * Creates a new dataset creation panel.
   */
  public DatasetCreationPanel() {
    initializeComponents();
  }

  /**
   * Initializes the panel components.
   */
  private void initializeComponents() {
    setLayout(new BorderLayout());
    setBorder(UIUtils.createPadding(UIConstants.EXTRA_LARGE_SPACING));

    // Title
    add(UIUtils.createTitleLabel("Dataset Creation"), BorderLayout.NORTH);

    // Main content
    add(createMainContent(), BorderLayout.CENTER);

    // Footer
    add(createFooter(), BorderLayout.SOUTH);
  }

  /**
   * Creates the main content area.
   */
  private JPanel createMainContent() {
    JPanel content = UIUtils.createVerticalPanel();

    // Coming soon message
    JPanel comingSoonPanel = new JPanel(new BorderLayout());
    comingSoonPanel.setBorder(UIUtils.createPadding(UIConstants.EXTRA_LARGE_SPACING));

    FontIcon clockIcon = FontIcon.of(FontAwesomeSolid.CLOCK, 48, UIManager.getColor("Label.disabledForeground"));
    comingSoonPanel.add(new JLabel(clockIcon), BorderLayout.NORTH);

    JLabel comingSoonLabel = new JLabel("<html><center><h2>Coming Soon</h2><br>Dataset creation functionality<br>is under development</center></html>");
    comingSoonLabel.setHorizontalAlignment(SwingConstants.CENTER);
    comingSoonLabel.setFont(comingSoonLabel.getFont().deriveFont(Font.ITALIC, UIConstants.NORMAL_FONT_SIZE));
    comingSoonLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    comingSoonPanel.add(comingSoonLabel, BorderLayout.CENTER);

    content.add(comingSoonPanel);

    return content;
  }

  /**
   * Creates the footer with additional information.
   */
  private JPanel createFooter() {
    JPanel footer = new JPanel(new BorderLayout());
    footer.setBorder(UIUtils.createPadding(UIConstants.LARGE_SPACING, 0, 0, 0));
    footer.setOpaque(false);

    String footerText = "This feature will allow you to create custom classification datasets";
    if (hasPrerequisites()) {
      footerText += " • Prerequisites: Completed analysis results";
    }

    JLabel footerLabel = new JLabel(footerText);
    footerLabel.setFont(footerLabel.getFont().deriveFont(Font.ITALIC, UIConstants.SMALL_FONT_SIZE));
    footerLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    footerLabel.setHorizontalAlignment(SwingConstants.CENTER);

    footer.add(footerLabel, BorderLayout.CENTER);

    return footer;
  }

  /**
   * Checks if prerequisites for dataset creation are met.
   */
  private boolean hasPrerequisites() {
    // TODO: Check if there are completed analysis results available
    // For now, always return false as this is a placeholder
    return false;
  }
}