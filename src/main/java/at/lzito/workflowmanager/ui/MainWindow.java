package at.lzito.workflowmanager.ui;

import at.lzito.workflowmanager.config.ConfigLoader;
import at.lzito.workflowmanager.engine.WorkflowEngine;
import at.lzito.workflowmanager.model.Workflow;
import at.lzito.workflowmanager.model.WorkflowConfig;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class MainWindow extends JFrame {

    private final ConfigLoader configLoader = new ConfigLoader();
    private final JTextArea logArea = new JTextArea();
    private final JPanel workflowPanel = new JPanel();
    private WorkflowConfig config;
    private WorkflowEngine engine;

    public MainWindow() {
        setTitle("Workflow Manager");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        engine = new WorkflowEngine(this::log);
        initComponents();
        setupTray();
        loadConfig();
        setVisible(true);
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));

        // Workflow buttons (top)
        workflowPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        add(new JScrollPane(workflowPanel), BorderLayout.NORTH);

        // Log area (center)
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Toolbar (bottom)
        JButton reloadBtn = new JButton("Reload Config");
        reloadBtn.addActionListener(e -> loadConfig());
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toolbar.add(reloadBtn);
        add(toolbar, BorderLayout.SOUTH);
    }

    private void loadConfig() {
        try {
            config = configLoader.load();
            buildWorkflowButtons(config.getWorkflows());
            log("Config loaded: " + configLoader.getConfigFile());
        } catch (IOException e) {
            log("Failed to load config: " + e.getMessage());
        }
    }

    private void buildWorkflowButtons(List<Workflow> workflows) {
        workflowPanel.removeAll();
        for (Workflow w : workflows) {
            String label = (w.getIcon() != null ? w.getIcon() + " " : "") + w.getName();
            JButton btn = new JButton(label);
            btn.setToolTipText(w.getHotkey());
            btn.addActionListener(e -> engine.activate(w, workflows));
            workflowPanel.add(btn);
        }
        workflowPanel.revalidate();
        workflowPanel.repaint();
    }

    private void setupTray() {
        if (!SystemTray.isSupported()) return;
        SystemTray tray = SystemTray.getSystemTray();
        Image icon = Toolkit.getDefaultToolkit().createImage(
                getClass().getResource("/icon.png") != null
                        ? getClass().getResource("/icon.png").getPath()
                        : "");
        TrayIcon trayIcon = new TrayIcon(icon, "Workflow Manager");
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> setVisible(true));

        PopupMenu popup = new PopupMenu();
        MenuItem showItem = new MenuItem("Show");
        showItem.addActionListener(e -> setVisible(true));
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        popup.add(showItem);
        popup.addSeparator();
        popup.add(exitItem);
        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
        } catch (AWTException ignored) {}
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
