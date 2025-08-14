package com.scipath.scipathj.ui.components;

import com.scipath.scipathj.ui.themes.ThemeManager;
import com.scipath.scipathj.ui.utils.UIConstants;
import com.scipath.scipathj.ui.utils.UIUtils;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * Panel for selecting input folder containing images to analyze.
 * Supports both button-based selection and drag-and-drop functionality.
 */
public class FolderSelectionPanel extends JPanel {

  private File selectedFolder;
  private JLabel folderPathLabel;
  private JButton clearButton;
  private JPanel dropArea;
  private ActionListener folderChangeListener;
  private boolean dragOver = false;

  public FolderSelectionPanel() {
    initializeComponents();
    setupDragAndDrop();
  }

  private void initializeComponents() {
    setLayout(new BorderLayout(0, UIConstants.LARGE_SPACING));
    setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING, UIConstants.LARGE_SPACING));
    setOpaque(false);

    add(UIUtils.createTitleLabel("Select Input Folder"), BorderLayout.NORTH);
    add(dropArea = createDropArea(), BorderLayout.CENTER);
    add(createFolderInfoPanel(), BorderLayout.SOUTH);
  }

  private JPanel createDropArea() {
    JPanel panel =
        new JPanel() {
          @Override
          protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintDropAreaBackground(g);
          }
        };

    panel.setLayout(new GridBagLayout());
    panel.setPreferredSize(new Dimension(600, 280));
    panel.setOpaque(false);

    addDropAreaContent(panel);
    return panel;
  }

  private void paintDropAreaBackground(Graphics g) {
    Graphics2D g2d = (Graphics2D) g.create();
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int width = getWidth();
    int height = getHeight();

    Color backgroundColor =
        dragOver
            ? UIUtils.withAlpha(UIConstants.ACCENT_COLOR, ThemeManager.isDarkTheme() ? 30 : 20)
            : (ThemeManager.isDarkTheme() ? new Color(255, 255, 255, 5) : new Color(0, 0, 0, 3));

    Color borderColor = dragOver ? UIConstants.ACCENT_COLOR : UIConstants.BORDER_COLOR;

    g2d.setColor(backgroundColor);
    g2d.fillRoundRect(0, 0, width, height, UIConstants.BORDER_RADIUS, UIConstants.BORDER_RADIUS);

    g2d.setColor(borderColor);
    g2d.setStroke(
        new BasicStroke(
            2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {10f, 10f}, 0f));
    g2d.drawRoundRect(
        1, 1, width - 2, height - 2, UIConstants.BORDER_RADIUS, UIConstants.BORDER_RADIUS);

    g2d.dispose();
  }

  private void addDropAreaContent(JPanel panel) {
    GridBagConstraints gbc = new GridBagConstraints();

    // Folder icon
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(0, 0, UIConstants.LARGE_SPACING, 0);
    panel.add(
        UIUtils.createIconLabel(
            FontAwesomeSolid.FOLDER_OPEN, 64, UIManager.getColor("Label.disabledForeground")),
        gbc);

    // Instructions
    gbc.gridy++;
    gbc.insets = new Insets(0, 0, UIConstants.TINY_SPACING, 0);
    panel.add(
        UIUtils.createLabel(
            "Drag and drop a folder here",
            UIConstants.MEDIUM_FONT_SIZE,
            UIManager.getColor("Label.disabledForeground")),
        gbc);

    gbc.gridy++;
    gbc.insets = new Insets(0, 0, UIConstants.EXTRA_LARGE_SPACING, 0);
    panel.add(
        UIUtils.createLabel(
            "or click the button below to browse",
            UIConstants.MEDIUM_FONT_SIZE,
            UIManager.getColor("Label.disabledForeground")),
        gbc);

    // Browse button
    gbc.gridy++;
    gbc.insets = new Insets(0, 0, 0, 0);
    JButton browseButton =
        UIUtils.createButton("Browse for Folder", FontAwesomeSolid.SEARCH, e -> browseForFolder());
    browseButton.setPreferredSize(new Dimension(190, 45));
    panel.add(browseButton, gbc);
  }

  private JPanel createFolderInfoPanel() {
    JPanel panel = UIUtils.createPanel(new BorderLayout(UIConstants.MEDIUM_SPACING, 0));

    panel.add(
        UIUtils.createBoldLabel("Selected Folder:", UIConstants.SMALL_FONT_SIZE),
        BorderLayout.WEST);

    folderPathLabel =
        UIUtils.createLabel(
            "No folder selected",
            UIConstants.SMALL_FONT_SIZE,
            UIManager.getColor("Label.disabledForeground"));
    panel.add(folderPathLabel, BorderLayout.CENTER);

    clearButton = UIUtils.createButton("Clear", FontAwesomeSolid.TIMES, e -> clearSelection());
    clearButton.setPreferredSize(new Dimension(90, 30));
    clearButton.setEnabled(false);
    panel.add(clearButton, BorderLayout.EAST);

    return panel;
  }

  private void setupDragAndDrop() {
    new DropTarget(
        dropArea,
        new DropTargetListener() {
          @Override
          public void dragEnter(DropTargetDragEvent dtde) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
              setDragOver(true);
              dtde.acceptDrag(DnDConstants.ACTION_COPY);
            } else {
              dtde.rejectDrag();
            }
          }

          @Override
          public void dragOver(DropTargetDragEvent dtde) {}

          @Override
          public void dropActionChanged(DropTargetDragEvent dtde) {}

          @Override
          public void dragExit(DropTargetEvent dte) {
            setDragOver(false);
          }

          @Override
          public void drop(DropTargetDropEvent dtde) {
            setDragOver(false);

            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
              dtde.acceptDrop(DnDConstants.ACTION_COPY);

              try {
                @SuppressWarnings("unchecked")
                List<File> files =
                    (List<File>)
                        dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                if (!files.isEmpty() && files.get(0).isDirectory()) {
                  setSelectedFolder(files.get(0));
                  dtde.dropComplete(true);
                  return;
                }
              } catch (Exception ignored) {
              }
            }

            dtde.dropComplete(false);
          }
        });
  }

  private void setDragOver(boolean dragOver) {
    if (this.dragOver != dragOver) {
      this.dragOver = dragOver;
      dropArea.repaint();
    }
  }

  private void browseForFolder() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fileChooser.setDialogTitle("Select Folder Containing Images");
    fileChooser.setCurrentDirectory(FileSystemView.getFileSystemView().getDefaultDirectory());

    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      setSelectedFolder(fileChooser.getSelectedFile());
    }
  }

  public void setSelectedFolder(File folder) {
    this.selectedFolder = folder;

    if (folder != null && folder.isDirectory()) {
      folderPathLabel.setText(folder.getAbsolutePath());
      folderPathLabel.setForeground(UIManager.getColor("Label.foreground"));
      clearButton.setEnabled(true);
      notifyFolderChange();
    } else {
      clearSelection();
    }
  }

  public void clearSelection() {
    this.selectedFolder = null;
    folderPathLabel.setText("No folder selected");
    folderPathLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
    clearButton.setEnabled(false);
    notifyFolderChange();
  }

  private void notifyFolderChange() {
    if (folderChangeListener != null) {
      folderChangeListener.actionPerformed(null);
    }
  }

  public File getSelectedFolder() {
    return selectedFolder;
  }

  public boolean hasSelection() {
    return selectedFolder != null && selectedFolder.isDirectory();
  }

  public void setFolderChangeListener(ActionListener listener) {
    this.folderChangeListener = listener;
  }
}
