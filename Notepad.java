import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;
import javax.swing.undo.UndoManager;

public class Notepad extends JFrame {
    private final JTabbedPane tabbedPane;
    private final JFileChooser fileChooser;
    private boolean isDarkMode = false;
    private final int defaultFontSize = 16;
    private UndoManager undoManager;

    public Notepad() {
        setTitle("Notepad");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set custom application icon
        ImageIcon icon = new ImageIcon("./Notepad-icon.png"); // Adjust the path to your icon file
        setIconImage(icon.getImage());

        // Setup UI
        setLookAndFeel();

        tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT); // Allow tab scrolling if tabs overflow
        add(tabbedPane, BorderLayout.CENTER); // Add tabbedPane instead of JTextArea directly

        fileChooser = new JFileChooser();

        createMenuBar();
        autoSaveFeature();

        // Add initial tab
        createNewTab();

        // Add key bindings for shortcuts
        addKeyBindings();

        setVisible(true);
    }

    private void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("Failed to set look and feel: " + ex.getMessage());
        }
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setFont(new Font("Arial", Font.BOLD, 14)); // Increase font size for menu bar

        // File Menu
        JMenu fileMenu = createCenteredMenu("File");
        JMenuItem newFile = new JMenuItem("New");
        JMenuItem openFile = new JMenuItem("Open");
        JMenuItem saveFile = new JMenuItem("Save");
        JMenuItem saveAsFile = new JMenuItem("Save As");
        JMenuItem exitApp = new JMenuItem("Exit");

        newFile.addActionListener(e -> createNewTab());
        openFile.addActionListener(e -> openFile());
        saveFile.addActionListener(e -> saveFile());
        saveAsFile.addActionListener(e -> saveFileAs());
        exitApp.addActionListener(e -> System.exit(0));

        fileMenu.add(newFile);
        fileMenu.add(openFile);
        fileMenu.add(saveFile);
        fileMenu.add(saveAsFile);
        fileMenu.addSeparator();
        fileMenu.add(exitApp);

        // Edit Menu
        JMenu editMenu = createCenteredMenu("Edit");
        JMenuItem undo = new JMenuItem("Undo");
        JMenuItem redo = new JMenuItem("Redo");
        JMenuItem cut = new JMenuItem("Cut");
        JMenuItem copy = new JMenuItem("Copy");
        JMenuItem paste = new JMenuItem("Paste");
        JMenuItem find = new JMenuItem("Find");

        undo.addActionListener(e -> performUndo());
        redo.addActionListener(e -> performRedo());
        cut.addActionListener(e -> getCurrentTextArea().cut());
        copy.addActionListener(e -> getCurrentTextArea().copy());
        paste.addActionListener(e -> getCurrentTextArea().paste());
        find.addActionListener(e -> findText());

        editMenu.add(undo);
        editMenu.add(redo);
        editMenu.addSeparator();
        editMenu.add(cut);
        editMenu.add(copy);
        editMenu.add(paste);
        editMenu.addSeparator();
        editMenu.add(find);

        // View Menu
        JMenu viewMenu = createCenteredMenu("View");
        JMenuItem zoomIn = new JMenuItem("Zoom In");
        JMenuItem zoomOut = new JMenuItem("Zoom Out");
        JMenuItem resetZoom = new JMenuItem("Reset Zoom");
        JMenuItem toggleDarkMode = new JMenuItem("Switch to Dark Mode");
        JMenuItem customTheme = new JMenuItem("Custom Theme");

        zoomIn.addActionListener(e -> zoomText(2));
        zoomOut.addActionListener(e -> zoomText(-2));
        resetZoom.addActionListener(e -> resetZoom());
        toggleDarkMode.addActionListener(e -> toggleDarkMode());
        customTheme.addActionListener(e -> customizeTheme());

        viewMenu.add(zoomIn);
        viewMenu.add(zoomOut);
        viewMenu.add(resetZoom);
        viewMenu.addSeparator();
        viewMenu.add(toggleDarkMode);
        viewMenu.add(customTheme);

        // Tab Menu
        JMenu tabMenu = createCenteredMenu("Tabs");
        JMenuItem renameTab = new JMenuItem("Rename Tab");

        renameTab.addActionListener(e -> renameCurrentTab());

        tabMenu.add(renameTab);

        // Add menus to menu bar
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(tabMenu);

        setJMenuBar(menuBar);
    }

    private JMenu createCenteredMenu(String title) {
        JMenu menu = new JMenu(title);
        menu.setFont(new Font("Arial", Font.BOLD, 14)); // Larger font
        menu.getPopupMenu().setLayout(new GridLayout(0, 1)); // Center-align dropdown menu items
        return menu;
    }

    private JTextArea getCurrentTextArea() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex == -1) return null;
        JPanel tabPanel = (JPanel) tabbedPane.getComponentAt(selectedIndex);
        JScrollPane scrollPane = (JScrollPane) tabPanel.getComponent(0);
        return (JTextArea) scrollPane.getViewport().getView();
    }

    private void createNewTab() {
        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font("Arial", Font.PLAIN, defaultFontSize));
        undoManager = new UndoManager();
        textArea.getDocument().addUndoableEditListener(undoManager);

        JPanel tabPanel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(textArea);
        tabPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel tabStatusBar = new JPanel(new BorderLayout());
        JLabel tabWordCountLabel = new JLabel("Word Count: 0");
        JLabel lastEditedLabel = new JLabel("Last Edited: Never");
        tabStatusBar.add(tabWordCountLabel, BorderLayout.WEST);
        tabStatusBar.add(lastEditedLabel, BorderLayout.EAST);
        tabPanel.add(tabStatusBar, BorderLayout.SOUTH);

        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateWordCount(textArea, tabWordCountLabel);
                updateLastEdited(lastEditedLabel);
            }
        });

        tabbedPane.addTab("Untitled", tabPanel);
        tabbedPane.setSelectedComponent(tabPanel);
    }

    private void renameCurrentTab() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex != -1) {
            String newTitle = JOptionPane.showInputDialog(this, "Enter new tab name:");
            if (newTitle != null && !newTitle.trim().isEmpty()) {
                tabbedPane.setTitleAt(selectedIndex, newTitle);
            }
        }
    }

    private void zoomText(int increment) {
        JTextArea textArea = getCurrentTextArea();
        if (textArea != null) {
            Font currentFont = textArea.getFont();
            int newSize = currentFont.getSize() + increment;
            if (newSize >= 8 && newSize <= 72) { // Limit zoom range
                textArea.setFont(new Font(currentFont.getFontName(), currentFont.getStyle(), newSize));
            }
        }
    }

    private void resetZoom() {
        JTextArea textArea = getCurrentTextArea();
        if (textArea != null) {
            textArea.setFont(new Font("Arial", Font.PLAIN, defaultFontSize));
        }
    }

    private void performUndo() {
        if (undoManager.canUndo()) {
            undoManager.undo();
        }
    }

    private void performRedo() {
        if (undoManager.canRedo()) {
            undoManager.redo();
        }
    }

    private void openFile() {
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                JTextArea textArea = getCurrentTextArea();
                if (textArea != null) {
                    textArea.read(reader, null);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error opening file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFile() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex >= 0) {
            File file = new File("Untitled" + (selectedIndex + 1) + ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                JTextArea textArea = getCurrentTextArea();
                if (textArea != null) {
                    textArea.write(writer);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFileAs() {
        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                JTextArea textArea = getCurrentTextArea();
                if (textArea != null) {
                    textArea.write(writer);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void findText() {
        String searchText = JOptionPane.showInputDialog(this, "Enter text to find:");
        if (searchText != null) {
            JTextArea textArea = getCurrentTextArea();
            if (textArea != null) {
                String content = textArea.getText();
                int index = content.indexOf(searchText);
                if (index >= 0) {
                    textArea.select(index, index + searchText.length());
                } else {
                    JOptionPane.showMessageDialog(this, "Text not found!", "Find", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }

    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        JTextArea textArea = getCurrentTextArea();
        if (textArea != null) {
            if (isDarkMode) {
                textArea.setBackground(Color.DARK_GRAY);
                textArea.setForeground(Color.WHITE);
            } else {
                textArea.setBackground(Color.WHITE);
                textArea.setForeground(Color.BLACK);
            }
        }
    }

    private void customizeTheme() {
        JTextArea textArea = getCurrentTextArea();
        if (textArea != null) {
            Color bgColor = JColorChooser.showDialog(this, "Choose Background Color", textArea.getBackground());
            Color fgColor = JColorChooser.showDialog(this, "Choose Text Color", textArea.getForeground());

            if (bgColor != null) textArea.setBackground(bgColor);
            if (fgColor != null) textArea.setForeground(fgColor);
        }
    }

    private void updateWordCount(JTextArea textArea, JLabel wordCountLabel) {
        String text = textArea.getText().trim();
        int wordCount = text.isEmpty() ? 0 : text.split("\\s+").length;
        wordCountLabel.setText("Word Count: " + wordCount);
    }

    private void updateLastEdited(JLabel lastEditedLabel) {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        lastEditedLabel.setText("Last Edited: " + currentTime);
    }

    private void autoSaveFeature() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                saveFile();
            }
        }, 30000, 30000); // Auto-save every 30 seconds
    }

    private void addKeyBindings() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "newTab");
        actionMap.put("newTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createNewTab();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK), "closeTab");
        actionMap.put("closeTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = tabbedPane.getSelectedIndex();
                if (selectedIndex != -1 && tabbedPane.getTabCount() > 1) {
                    tabbedPane.removeTabAt(selectedIndex);
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performUndo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
        actionMap.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performRedo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK), "switchTab");
        actionMap.put("switchTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = tabbedPane.getSelectedIndex();
                int nextTabIndex = (selectedIndex + 1) % tabbedPane.getTabCount();
                tabbedPane.setSelectedIndex(nextTabIndex);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Notepad::new);
    }
}
