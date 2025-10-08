package com.scipath.scipathj.ui.dataset.view;

import com.scipath.scipathj.ui.dataset.model.ClassItem;
import com.scipath.scipathj.ui.dataset.model.VisualControlsState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * View component for DatasetControls UI.
 * Responsible for all UI layout and component creation, separated from business logic.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class DatasetControlsView extends JPanel {
    // File operations components
    private JButton loadROIsButton;
    private JButton clearROIsButton;
    private JButton downloadTrainingDataButton;
    private JButton trainXGBoostButton;

    // Visual controls components
    private JSlider borderWidthSlider;
    private JSlider fillOpacitySlider;
    private JCheckBox showNucleiCheckBox;
    private JCheckBox showCellsCheckBox;

    // Class management components
    private JComboBox<ClassItem> classComboBox;
    private JButton addClassButton;
    private JTextField classNameField;
    private JButton colorPickerButton;
    private JPanel colorPreview;
    private JPanel classCountersPanel;

    // Status label
    private JLabel statusLabel;

    /**
     * Creates a new DatasetControlsView.
     */
    public DatasetControlsView() {
        initializeComponents();
        setupModernLayout();
    }

    /**
     * Initialize all UI components.
     */
    private void initializeComponents() {
        // File operations
        loadROIsButton = createModernButton("Load ROIs from ZIP", new Color(40, 167, 69));
        clearROIsButton = createModernButton("Clear ROIs", new Color(220, 53, 69));
        downloadTrainingDataButton = createModernButton("Download Training Data", new Color(102, 102, 255));
        trainXGBoostButton = createModernButton("Train XGBoost Model", new Color(128, 0, 128));
        trainXGBoostButton.setVisible(false);

        // Visual controls
        borderWidthSlider = createModernSlider(1, 5, 2, 1);
        fillOpacitySlider = createModernSlider(0, 100, 20, 25);

        showNucleiCheckBox = createStyledCheckBox("Show Nuclei", true);
        showCellsCheckBox = createStyledCheckBox("Show Cells", true);

        // Class management with colored renderer
        classComboBox = new JComboBox<>();
        classComboBox.setRenderer(new RobustClassItemRenderer());
        classComboBox.setPreferredSize(new Dimension(180, 28));
        classComboBox.setMaximumSize(new Dimension(180, 28));

        addClassButton = createModernButton("Add Class", new Color(0, 123, 255));
        classNameField = createStyledTextField(15);

        // Setup placeholder behavior for class name field
        setupPlaceholderBehavior(classNameField, "Enter class name");

        // Color picker
        colorPickerButton = createColorPickerButton();
        colorPreview = createColorPreview();

        // Class counters panel
        classCountersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        classCountersPanel.setOpaque(false);


        // Status label
        statusLabel = new JLabel("Ready");
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
    }

    /**
     * Setup the modern layout using sections.
     */
    private void setupModernLayout() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Main panel with modern card-based sections
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);

        // File operations section
        mainPanel.add(createModernSection("File Operations", createFileOperationsPanel()));
        mainPanel.add(Box.createVerticalStrut(15));

        // Visual controls section
        mainPanel.add(createModernSection("Display Settings", createVisualControlsPanel()));
        mainPanel.add(Box.createVerticalStrut(15));

        // Class management section
        mainPanel.add(createModernSection("Class Assignment", createClassManagementPanel()));
        mainPanel.add(Box.createVerticalStrut(15));

        // Class counters section
        mainPanel.add(createModernSection("Class Counts", classCountersPanel));

        // Add main panel
        add(mainPanel, BorderLayout.CENTER);

        // Add status bar
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    // === FACTORY METHODS ===

    private JButton createModernButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    private JSlider createModernSlider(int min, int max, int value, int majorTick) {
        JSlider slider = new JSlider(min, max, value);
        slider.setMajorTickSpacing(majorTick);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setOpaque(false);
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);
        return slider;
    }

    private JCheckBox createStyledCheckBox(String text, boolean selected) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setOpaque(false);
        checkBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        checkBox.setForeground(UIManager.getColor("CheckBox.foreground"));
        checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        return checkBox;
    }

    private JTextField createStyledTextField(int columns) {
        JTextField textField = new JTextField(columns);
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(6, 8, 6, 8)
        ));
        return textField;
    }

    private JButton createColorPickerButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(40, 30));
        button.setMinimumSize(new Dimension(40, 30));
        button.setMaximumSize(new Dimension(40, 30));
        button.setSize(new Dimension(40, 30));
        button.setBackground(new Color(255, 87, 34)); // Material Orange
        button.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setToolTipText("Pick a color for the new class");
        return button;
    }

    public JPanel createColorPreview() {
        JPanel preview = new JPanel();
        preview.setPreferredSize(new Dimension(20, 20));
        preview.setBackground(new Color(255, 87, 34)); // Material Orange
        preview.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        return preview;
    }

    /**
     * Setup placeholder behavior for a text field.
     * When focused, clear placeholder text if present.
     * When unfocused and empty, restore placeholder text.
     */
    private void setupPlaceholderBehavior(JTextField textField, String placeholderText) {
        // Store original foreground color
        Color originalColor = textField.getForeground();

        // Initially show placeholder
        textField.setText(placeholderText);
        textField.setForeground(new Color(128, 128, 128)); // Gray placeholder text

        textField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (placeholderText.equals(textField.getText())) {
                    textField.setText("");
                    textField.setForeground(originalColor);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (textField.getText().trim().isEmpty()) {
                    textField.setText(placeholderText);
                    textField.setForeground(new Color(128, 128, 128));
                }
            }
        });
    }

    // === PANEL CREATION METHODS ===

    private JPanel createFileOperationsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonsPanel.setOpaque(false);

        buttonsPanel.add(loadROIsButton);
        buttonsPanel.add(clearROIsButton);

        panel.add(buttonsPanel);
        panel.add(Box.createVerticalStrut(10));

        // Training panel
        JPanel trainingPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        trainingPanel.setOpaque(false);

        trainingPanel.add(downloadTrainingDataButton);
        trainingPanel.add(trainXGBoostButton);

        panel.add(trainingPanel);

        return panel;
    }

    private JPanel createVisualControlsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Border width
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Border Width:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(borderWidthSlider, gbc);

        // Fill opacity
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Fill Opacity:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(fillOpacitySlider, gbc);

        // Checkboxes
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(showNucleiCheckBox, gbc);
        gbc.gridx = 1;
        panel.add(showCellsCheckBox, gbc);

        return panel;
    }

    private JPanel createClassManagementPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Current class selection
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Selected Class:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(classComboBox, gbc);

        // Add new class row
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(colorPickerButton, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(classNameField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(addClassButton, gbc);

        return panel;
    }


    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(5, 0, 0, 0));
        statusBar.add(statusLabel, BorderLayout.WEST);
        return statusBar;
    }

    private JPanel createModernSection(String title, JPanel content) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(getBackground());
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));

        // Title label
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(0, 123, 255)); // Primary blue
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));

        section.add(titleLabel, BorderLayout.NORTH);
        section.add(content, BorderLayout.CENTER);

        return section;
    }

    private Color getBorderColor() {
        Color fg = UIManager.getColor("Label.foreground");
        return new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 64); // Semi-transparent border
    }

    // === GETTERS FOR CONTROLLER ACCESS ===

    public JButton getLoadROIsButton() { return loadROIsButton; }
    public JButton getClearROIsButton() { return clearROIsButton; }
    public JButton getDownloadTrainingDataButton() { return downloadTrainingDataButton; }
    public JButton getTrainXGBoostButton() { return trainXGBoostButton; }

    public JSlider getBorderWidthSlider() { return borderWidthSlider; }
    public JSlider getFillOpacitySlider() { return fillOpacitySlider; }
    public JCheckBox getShowNucleiCheckBox() { return showNucleiCheckBox; }
    public JCheckBox getShowCellsCheckBox() { return showCellsCheckBox; }

    public JComboBox<ClassItem> getClassComboBox() { return classComboBox; }
    public JButton getAddClassButton() { return addClassButton; }
    public JTextField getClassNameField() { return classNameField; }
    public JButton getColorPickerButton() { return colorPickerButton; }
    public JPanel getColorPreview() { return colorPreview; }
    public JPanel getClassCountersPanel() { return classCountersPanel; }

    public JLabel getStatusLabel() { return statusLabel; }

    // === UTILITY METHODS ===

    /**
     * Set status message.
     */
    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    /**
     * Update visual controls state.
     */
    public void updateVisualControls(VisualControlsState state) {
        if (state != null) {
            borderWidthSlider.setValue((int) state.getBorderWidth());
            fillOpacitySlider.setValue((int) state.getFillOpacity());
            showNucleiCheckBox.setSelected(state.isShowNuclei());
            showCellsCheckBox.setSelected(state.isShowCells());
        }
    }

    /**
     * Set the action listener for the color picker button.
     * This allows the controller to handle color selection.
     */
    public void setColorPickerAction(ActionListener listener) {
        colorPickerButton.addActionListener(listener);
    }

    /**
     * Update class counters display.
     */
    public void updateClassCounters(java.util.Map<String, Integer> counts, java.util.Map<String, Color> colors) {
        classCountersPanel.removeAll();

        if (counts != null && !counts.isEmpty()) {
            for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
                String className = entry.getKey();
                int count = entry.getValue();
                Color color = colors != null ? colors.getOrDefault(className, Color.YELLOW) : Color.YELLOW;

                if (!"Unclassified".equals(className) && count > 0) {
                    JLabel counter = new JLabel(className + ": " + count);
                    counter.setOpaque(true);
                    counter.setBackground(color);
                    counter.setForeground(getContrastColor(color));
                    counter.setBorder(new EmptyBorder(5, 10, 5, 10));
                    counter.setFont(new Font("Segoe UI", Font.BOLD, 11));

                    classCountersPanel.add(counter);
                }
            }
        }

        classCountersPanel.revalidate();
        classCountersPanel.repaint();
    }

    private Color getContrastColor(Color color) {
        // Calculate luminance for contrast
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255.0;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Robust renderer for ClassItem that displays color squares.
     * Fixed to avoid circular references and properly handle color squares.
     */
    private class RobustClassItemRenderer extends JLabel implements ListCellRenderer<ClassItem> {
        private static final int PREFERRED_SQUARE_SIZE = 16;
        private static final int MIN_SQUARE_SIZE = 8;
        private final int BORDER_MARGIN = 2;

        private ClassItem currentItem;
        private int availableWidth = 0;

        public RobustClassItemRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ClassItem> list, ClassItem value,
                                                    int index, boolean isSelected, boolean cellHasFocus) {
            this.currentItem = value;

            // Set text and basic properties
            setText(value != null ? value.getName() : "");
            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            setFont(new Font("Segoe UI", Font.PLAIN, 12));

            // Add left padding to prevent text from overlapping the color square
            setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 0));

            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (currentItem != null) {
                // Calculate dynamic square size and position based on available space
                Dimension size = getSize();
                int availableWidth = size.width;
                int availableHeight = size.height;

                // Calculate square size - scale down if space is limited
                int squareSize = calculateDynamicSquareSize(availableWidth, availableHeight);
                int squareX = calculateDynamicSquareX(availableWidth, squareSize);
                int y = Math.max(1, (availableHeight - squareSize) / 2);

                // Ensure square stays within bounds
                if (squareX + squareSize > availableWidth) {
                    squareSize = Math.max(MIN_SQUARE_SIZE, availableWidth - squareX - BORDER_MARGIN);
                }
                if (squareSize < MIN_SQUARE_SIZE) {
                    squareSize = MIN_SQUARE_SIZE;
                    squareX = Math.max(0, availableWidth - squareSize - BORDER_MARGIN);
                }

                // Fill square
                g.setColor(currentItem.getColor());
                g.fillRect(squareX, y, squareSize, squareSize);

                // Draw border
                g.setColor(Color.DARK_GRAY);
                g.drawRect(squareX, y, squareSize, squareSize);
            }
        }

        private int calculateDynamicSquareSize(int availableWidth, int availableHeight) {
            // Try preferred size first
            if (availableWidth >= PREFERRED_SQUARE_SIZE + BORDER_MARGIN * 2 + 40) { // Space for square + text
                return PREFERRED_SQUARE_SIZE;
            }

            // Scale down proportionally if space is limited
            int maxSquareSize = Math.min(availableWidth - BORDER_MARGIN * 2 - 20, availableHeight - 4);
            return Math.max(MIN_SQUARE_SIZE, Math.min(PREFERRED_SQUARE_SIZE, maxSquareSize));
        }

        private int calculateDynamicSquareX(int availableWidth, int squareSize) {
            // Position at the left, with small margin
            return BORDER_MARGIN;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension textSize = super.getPreferredSize();
            // Ensure enough space for preferred square + text
            int preferredWidth = PREFERRED_SQUARE_SIZE + BORDER_MARGIN * 2 + textSize.width + 10;
            return new Dimension(preferredWidth, Math.max(24, textSize.height));
        }

        @Override
        public Dimension getMinimumSize() {
            // Absolute minimum - smaller than before to allow more flexibility
            return new Dimension(MIN_SQUARE_SIZE + BORDER_MARGIN * 2 + 15, 18);
        }

        public int getAppropriateSquareSize(int availableWidth) {
            return calculateDynamicSquareSize(availableWidth, PREFERRED_SQUARE_SIZE);
        }
    }
}