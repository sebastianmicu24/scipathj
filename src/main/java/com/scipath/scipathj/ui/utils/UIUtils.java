package com.scipath.scipathj.ui.utils;

import com.scipath.scipathj.ui.themes.ThemeManager;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Utility methods for common UI operations and styling.
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public final class UIUtils {
    
    // Prevent instantiation
    private UIUtils() {}
    
    /**
     * Creates a standard empty border with the specified padding.
     */
    public static Border createPadding(int padding) {
        return new EmptyBorder(padding, padding, padding, padding);
    }
    
    /**
     * Creates an empty border with different padding for each side.
     */
    public static Border createPadding(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }
    
    /**
     * Creates an empty border with vertical and horizontal padding.
     */
    public static Border createPadding(int vertical, int horizontal) {
        return new EmptyBorder(vertical, horizontal, vertical, horizontal);
    }
    
    /**
     * Creates a standard title label with consistent styling.
     */
    public static JLabel createTitleLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, UIConstants.TITLE_FONT_SIZE));
        label.setBorder(createPadding(0, 0, UIConstants.EXTRA_LARGE_SPACING, 0));
        return label;
    }
    /**
     * Creates a standard button with consistent styling.
     */
    public static JButton createStandardButton(String text, Icon icon) {
        JButton button = new JButton(text);
        if (icon != null) {
            button.setIcon(icon);
        }
        button.setPreferredSize(UIConstants.BUTTON_SIZE);
        button.setFont(button.getFont().deriveFont(Font.BOLD, UIConstants.NORMAL_FONT_SIZE));
        return button;
    }
    
    /**
     * Creates a small button with consistent styling.
     */
    public static JButton createSmallButton(String text, Icon icon) {
        JButton button = new JButton(text);
        if (icon != null) {
            button.setIcon(icon);
        }
        button.setPreferredSize(UIConstants.SMALL_BUTTON_SIZE);
        button.setFont(button.getFont().deriveFont(Font.BOLD, UIConstants.SMALL_FONT_SIZE));
        return button;
    }
    
    
    /**
     * Gets theme-aware hover color.
     */
    public static Color getHoverColor() {
        return ThemeManager.isDarkTheme() ? 
            new Color(255, 255, 255, 100) : 
            new Color(0, 0, 0, 100);
    }
    
    /**
     * Gets theme-aware background color with transparency.
     */
    public static Color getBackgroundColor(boolean highlighted) {
        if (highlighted) {
            return ThemeManager.isDarkTheme() ?
                new Color(70, 130, 180, 40) : new Color(70, 130, 180, 30);
        } else {
            return ThemeManager.isDarkTheme() ?
                new Color(255, 255, 255, 10) : new Color(0, 0, 0, 5);
        }
    }
    
    /**
     * Gets theme-aware border color.
     */
    public static Color getBorderColor(boolean focused) {
        if (focused) {
            return UIConstants.SELECTION_COLOR;
        } else {
            return ThemeManager.isDarkTheme() ? 
                new Color(80, 80, 80) : new Color(200, 200, 200);
        }
    }
    
    /**
     * Creates a rounded rectangle shape.
     */
    public static Shape createRoundedRectangle(int x, int y, int width, int height, int radius) {
        return new java.awt.geom.RoundRectangle2D.Float(x, y, width, height, radius, radius);
    }
    
    /**
     * Sets up standard rendering hints for graphics.
     */
    public static void setupRenderingHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }
    
    /**
     * Creates a panel with vertical box layout.
     */
    public static JPanel createVerticalPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        return panel;
    }
    
    /**
     * Creates a panel with horizontal box layout.
     */
    public static JPanel createHorizontalPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false);
        return panel;
    }
    
    /**
     * Creates a centered panel with the specified component.
     */
    public static JPanel createCenteredPanel(Component component) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.add(component);
        return panel;
    }
    
    /**
     * Creates a panel with the specified layout manager.
     */
    public static JPanel createPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }
    
    /**
     * Creates a label with specified text, font size, and color.
     */
    public static JLabel createLabel(String text, float fontSize, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(fontSize));
        if (color != null) {
            label.setForeground(color);
        }
        return label;
    }
    
    /**
     * Creates a bold label with specified text and font size.
     */
    public static JLabel createBoldLabel(String text, float fontSize) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, fontSize));
        return label;
    }
    
    /**
     * Creates an icon label with specified icon, size, and color.
     */
    public static JLabel createIconLabel(Ikon icon, int size, Color color) {
        JLabel label = new JLabel(FontIcon.of(icon, size));
        if (color != null) {
            label.setForeground(color);
        }
        return label;
    }
    
    /**
     * Creates a button with text, icon, and action listener.
     */
    public static JButton createButton(String text, Ikon icon, ActionListener listener) {
        JButton button = new JButton(text);
        if (icon != null) {
            button.setIcon(FontIcon.of(icon, 16));
        }
        if (listener != null) {
            button.addActionListener(listener);
        }
        button.setFont(button.getFont().deriveFont(Font.BOLD, UIConstants.NORMAL_FONT_SIZE));
        return button;
    }
    
    /**
     * Creates a button with text, font size, and action listener.
     */
    public static JButton createButton(String text, float fontSize, ActionListener listener) {
        JButton button = new JButton(text);
        if (listener != null) {
            button.addActionListener(listener);
        }
        button.setFont(button.getFont().deriveFont(Font.BOLD, fontSize));
        return button;
    }
    
    /**
     * Creates an icon from the specified Ikon with given size.
     */
    public static Icon createIcon(Ikon icon, int size) {
        return FontIcon.of(icon, size);
    }
    
    /**
     * Creates a titled border with the specified title.
     */
    public static Border createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(title);
    }
    
    /**
     * Creates a color with alpha transparency.
     */
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}