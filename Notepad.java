import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

public class Notepad extends JFrame {
    private JTabbedPane tabbedPane;
    private JFileChooser fileChooser;
    private boolean isDarkMode = false;
    private JMenuItem toggleDarkMode;

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
        JMenuItem cut = new JMenuItem("Cut");
        JMenuItem copy = new JMenuItem("Copy");
        JMenuItem paste = new JMenuItem("Paste");
        JMenuItem find = new JMenuItem("Find");

        cut.addActionListener(e -> getCurrentTextArea().cut());
        copy.addActionListener(e -> getCurrentTextArea().copy());
        paste.addActionListener(e -> getCurrentTextArea().paste());
        find.addActionListener(e -> findText());

        editMenu.add(cut);
        editMenu.add(copy);
        editMenu.add(paste);
        editMenu.addSeparator();
        editMenu.add(find);

        // View Menu
        JMenu viewMenu = createCenteredMenu("View");
        toggleDarkMode = new JMenuItem("Switch to Dark Mode");
        toggleDarkMode.addActionListener(e -> toggleDarkMode());
        JMenuItem customTheme = new JMenuItem("Custom Theme");
        customTheme.addActionListener(e -> customizeTheme());

        viewMenu.add(toggleDarkMode);
        viewMenu.add(customTheme);

        // Tools Menu
        JMenu toolsMenu = createCenteredMenu("Tools");
        JMenuItem boldText = new JMenuItem("Bold Text");
        boldText.addActionListener(e -> getCurrentTextArea().setFont(getCurrentTextArea().getFont().deriveFont(Font.BOLD)));
        JMenuItem italicText = new JMenuItem("Italic Text");
        italicText.addActionListener(e -> getCurrentTextArea().setFont(getCurrentTextArea().getFont().deriveFont(Font.ITALIC)));

        toolsMenu.add(boldText);
        toolsMenu.add(italicText);

        // Add menus to menu bar
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);

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
        textArea.setFont(new Font("Davish - Sans Serif", Font.PLAIN, 16));

        JPanel tabPanel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(textArea);
        tabPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel tabStatusBar = new JPanel(new BorderLayout());
        JLabel tabWordCountLabel = new JLabel("Word Count: 0");
        tabWordCountLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JLabel lastEditedLabel = new JLabel("Last Edited: Never");
        lastEditedLabel.setFont(new Font("Arial", Font.PLAIN, 14));
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

        // Adding tab with close functionality
        JPanel tabHeader = new JPanel(new BorderLayout());
        JLabel tabLabel = new JLabel("Untitled");
        JButton closeButton = new JButton("X");
        closeButton.addActionListener(e -> closeTab(tabPanel));

        tabHeader.add(tabLabel, BorderLayout.WEST);
        tabHeader.add(closeButton, BorderLayout.EAST);

        tabbedPane.addTab(null, tabPanel);
        tabbedPane.setTabComponentAt(tabbedPane.indexOfComponent(tabPanel), tabHeader);
        tabbedPane.setSelectedComponent(tabPanel);
    }

    private void closeTab(JPanel tabPanel) {
        if (tabbedPane.getTabCount() > 1) {
            tabbedPane.remove(tabPanel);
        }
    }

    private void openFile() {
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                JTextArea textArea = new JTextArea();
                textArea.read(reader, null);
                textArea.setFont(new Font("Davish - Sans Serif", Font.PLAIN, 16));
                createNewTab();
                JPanel tabPanel = (JPanel) tabbedPane.getSelectedComponent();
                JScrollPane scrollPane = (JScrollPane) tabPanel.getComponent(0);
                JTextArea currentTextArea = (JTextArea) scrollPane.getViewport().getView();
                currentTextArea.setText(textArea.getText());
                tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), file.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error opening file!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFile() {
        File file = new File(tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()));
        if (file == null) {
            saveFileAs();
        } else {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                getCurrentTextArea().write(writer);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving file!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFileAs() {
        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                getCurrentTextArea().write(writer);
                tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), file.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving file!", "Error", JOptionPane.ERROR_MESSAGE);
            }
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

    private void findText() {
        String searchText = JOptionPane.showInputDialog(this, "Enter text to find:");
        if (searchText != null) {
            String content = getCurrentTextArea().getText();
            int index = content.indexOf(searchText);
            if (index >= 0) {
                getCurrentTextArea().select(index, index + searchText.length());
            } else {
                JOptionPane.showMessageDialog(this, "Text not found!", "Find", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            getCurrentTextArea().setBackground(Color.DARK_GRAY);
            getCurrentTextArea().setForeground(Color.WHITE);
            toggleDarkMode.setText("Switch to Light Mode");
        } else {
            getCurrentTextArea().setBackground(Color.WHITE);
            getCurrentTextArea().setForeground(Color.BLACK);
            toggleDarkMode.setText("Switch to Dark Mode");
        }
    }

    private void customizeTheme() {
        Color bgColor = JColorChooser.showDialog(this, "Choose Background Color", getCurrentTextArea().getBackground());
        Color fgColor = JColorChooser.showDialog(this, "Choose Text Color", getCurrentTextArea().getForeground());

        if (bgColor != null) getCurrentTextArea().setBackground(bgColor);
        if (fgColor != null) getCurrentTextArea().setForeground(fgColor);
    }

    private void autoSaveFeature() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                File currentFile = new File(tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()));
                if (currentFile.exists()) {
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) {
                        getCurrentTextArea().write(writer);
                    } catch (IOException ex) {
                        System.err.println("Auto-save failed.");
                    }
                }
            }
        }, 30000, 30000); // Auto-save every 30 seconds
    }

    private void addKeyBindings() {
        // Ctrl + N to open a new tab
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "newTab");
        actionMap.put("newTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createNewTab();
            }
        });

        // Ctrl + Z to close the current tab
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "closeTab");
        actionMap.put("closeTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (tabbedPane.getTabCount() > 1) {
                    int selectedTabIndex = tabbedPane.getSelectedIndex();
                    tabbedPane.removeTabAt(selectedTabIndex);
                }
            }
        });

        // Ctrl + Tab to switch between tabs
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK), "switchTab");
        actionMap.put("switchTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedTabIndex = tabbedPane.getSelectedIndex();
                int nextTabIndex = (selectedTabIndex + 1) % tabbedPane.getTabCount();
                tabbedPane.setSelectedIndex(nextTabIndex);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Notepad::new);
    }
}
