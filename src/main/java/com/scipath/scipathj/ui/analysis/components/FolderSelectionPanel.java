package com.scipath.scipathj.ui.analysis.components;

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
 * Panel for selecting input containing images to analyze.
 * Supports both folder and single file selection with drag-and-drop functionality.
 */
public class FolderSelectionPanel extends JPanel {

  private File selectedFolder;
  private File selectedFile;
  private boolean isFolderMode = true; // true for folder, false for single file
  private JLabel pathLabel;
  private JButton clearButton;
  private JPanel folderDropArea;
  private JPanel fileDropArea;
  private ActionListener folderChangeListener;
  private boolean folderDragOver = false;
  private boolean fileDragOver = false;

  public FolderSelectionPanel() {
    initializeComponents();
    setupDragAndDrop();
  }

  private void initializeComponents() {
    setLayout(new BorderLayout(0, UIConstants.LARGE_SPACING));
    setBorder(UIUtils.createPadding(UIConstants.MEDIUM_SPACING, UIConstants.LARGE_SPACING));
    setOpaque(false);

    add(UIUtils.createTitleLabel("Select Input"), BorderLayout.NORTH);
    add(createDualDropArea(), BorderLayout.CENTER);
    add(createPathInfoPanel(), BorderLayout.SOUTH);
  }

  private JPanel createDualDropArea() {
    JPanel container = UIUtils.createPanel(new GridLayout(1, 2, UIConstants.LARGE_SPACING, 0));

    // Folder drop area
    folderDropArea = createDropArea(true);
    container.add(folderDropArea);

    // File drop area
    fileDropArea = createDropArea(false);
    container.add(fileDropArea);

    return container;
  }

  private JPanel createDropArea(boolean isFolder) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setPreferredSize(new Dimension(280, 200));
    panel.setBorder(BorderFactory.createDashedBorder(UIConstants.BORDER_COLOR, 2, 5, 3, true));

    // Content panel
    JPanel contentPanel = new JPanel(new GridBagLayout());
    contentPanel.setOpaque(false);
    panel.add(contentPanel, BorderLayout.CENTER);

    addDropAreaContent(contentPanel, isFolder);
    return panel;
  }

  private void addDropAreaContent(JPanel panel, boolean isFolder) {
    GridBagConstraints gbc = new GridBagConstraints();

    // Icon
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(0, 0, UIConstants.LARGE_SPACING, 0);
    panel.add(
        UIUtils.createIconLabel(
            isFolder ? FontAwesomeSolid.FOLDER_OPEN : FontAwesomeSolid.FILE_IMAGE,
            48, UIManager.getColor("Label.disabledForeground")),
        gbc);

    // Instructions
    gbc.gridy++;
    gbc.insets = new Insets(0, 0, UIConstants.TINY_SPACING, 0);
    panel.add(
        UIUtils.createLabel(
            isFolder ? "Drag and drop a folder here" : "Drag and drop a file here",
            UIConstants.SMALL_FONT_SIZE,
            UIManager.getColor("Label.disabledForeground")),
        gbc);

    gbc.gridy++;
    gbc.insets = new Insets(0, 0, UIConstants.EXTRA_LARGE_SPACING, 0);
    panel.add(
        UIUtils.createLabel(
            "or click the button below to browse",
            UIConstants.SMALL_FONT_SIZE,
            UIManager.getColor("Label.disabledForeground")),
        gbc);

    // Browse button
    gbc.gridy++;
    gbc.insets = new Insets(0, 0, 0, 0);
    JButton browseButton =
        UIUtils.createButton(
            isFolder ? "Browse for Folder" : "Browse for File",
            FontAwesomeSolid.SEARCH,
            isFolder ? e -> browseForFolder() : e -> browseForFile());
    browseButton.setPreferredSize(new Dimension(160, 40));
    panel.add(browseButton, gbc);
  }

  private JPanel createPathInfoPanel() {
    JPanel panel = UIUtils.createPanel(new BorderLayout(UIConstants.MEDIUM_SPACING, 0));

    panel.add(
        UIUtils.createBoldLabel("Selected Path:", UIConstants.SMALL_FONT_SIZE),
        BorderLayout.WEST);

    pathLabel =
        UIUtils.createLabel(
            "No path selected",
            UIConstants.SMALL_FONT_SIZE,
            UIManager.getColor("Label.disabledForeground"));
    panel.add(pathLabel, BorderLayout.CENTER);

    clearButton = UIUtils.createButton("Clear", FontAwesomeSolid.TIMES, e -> clearSelection());
    clearButton.setPreferredSize(new Dimension(90, 30));
    clearButton.setEnabled(false);
    panel.add(clearButton, BorderLayout.EAST);

    return panel;
  }

  private void setupDragAndDrop() {
    // Folder drop area
    new DropTarget(
        folderDropArea,
        new DropTargetListener() {
          @Override
          public void dragEnter(DropTargetDragEvent dtde) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
              setDragOver(true, true);
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
            setDragOver(false, true);
          }

          @Override
          public void drop(DropTargetDropEvent dtde) {
            setDragOver(false, true);

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

    // File drop area
    new DropTarget(
        fileDropArea,
        new DropTargetListener() {
          @Override
          public void dragEnter(DropTargetDragEvent dtde) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
              setDragOver(true, false);
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
            setDragOver(false, false);
          }

          @Override
          public void drop(DropTargetDropEvent dtde) {
            setDragOver(false, false);

            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
              dtde.acceptDrop(DnDConstants.ACTION_COPY);

              try {
                @SuppressWarnings("unchecked")
                List<File> files =
                    (List<File>)
                        dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                if (!files.isEmpty() && files.get(0).isFile()) {
                  setSelectedFile(files.get(0));
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

  private void setDragOver(boolean dragOver, boolean isFolder) {
    if (isFolder) {
      if (this.folderDragOver != dragOver) {
        this.folderDragOver = dragOver;
        updateDropAreaBorder(true);
      }
    } else {
      if (this.fileDragOver != dragOver) {
        this.fileDragOver = dragOver;
        updateDropAreaBorder(false);
      }
    }
  }

  private void updateDropAreaBorder(boolean isFolder) {
    JPanel targetArea = isFolder ? folderDropArea : fileDropArea;
    boolean dragOver = isFolder ? folderDragOver : fileDragOver;

    Color borderColor = dragOver ? UIConstants.SELECTION_COLOR : UIConstants.BORDER_COLOR;
    targetArea.setBorder(BorderFactory.createDashedBorder(borderColor, 2, 5, 3, true));
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

  private void browseForFile() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setDialogTitle("Select Image File");
    fileChooser.setCurrentDirectory(FileSystemView.getFileSystemView().getDefaultDirectory());

    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      setSelectedFile(fileChooser.getSelectedFile());
    }
  }

  public void setSelectedFolder(File folder) {
    this.selectedFolder = folder;
    this.selectedFile = null;
    this.isFolderMode = true;

    if (folder != null && folder.isDirectory()) {
      pathLabel.setText(folder.getAbsolutePath());
      pathLabel.setForeground(UIManager.getColor("Label.foreground"));
      clearButton.setEnabled(true);
      notifyFolderChange();
    } else {
      clearSelection();
    }
  }

  public void setSelectedFile(File file) {
    this.selectedFile = file;
    this.selectedFolder = file != null ? file.getParentFile() : null;
    this.isFolderMode = false;

    if (file != null && file.isFile()) {
      pathLabel.setText(file.getAbsolutePath());
      pathLabel.setForeground(UIManager.getColor("Label.foreground"));
      clearButton.setEnabled(true);
      notifyFolderChange();
    } else {
      clearSelection();
    }
  }

  public void clearSelection() {
    this.selectedFolder = null;
    this.selectedFile = null;
    pathLabel.setText("No path selected");
    pathLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
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

  public File getSelectedFile() {
    return selectedFile;
  }

  public boolean hasSelection() {
    return (selectedFolder != null && selectedFolder.isDirectory()) ||
           (selectedFile != null && selectedFile.isFile());
  }

  public boolean isFolderMode() {
    return isFolderMode;
  }

  public void setFolderChangeListener(ActionListener listener) {
    this.folderChangeListener = listener;
  }
}
