package at.lzito.workflowmanager.ui;

import at.lzito.workflowmanager.application.ActivateWorkflowUseCase;
import at.lzito.workflowmanager.application.ReloadWorkflowsUseCase;
import at.lzito.workflowmanager.domain.Workflow;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * Main application window.
 *
 * <p>Depends only on application-layer use cases — zero direct infrastructure imports.
 * All workflow activation runs on a background thread so the EDT is never blocked.
 */
public class MainWindow extends JFrame {

    private final ActivateWorkflowUseCase activateUseCase;
    private final ReloadWorkflowsUseCase  reloadUseCase;

    private final JPanel   workflowPanel = new JPanel();
    private final JTextArea logArea      = new JTextArea();

    public MainWindow(ActivateWorkflowUseCase activateUseCase, ReloadWorkflowsUseCase reloadUseCase) {
        this.activateUseCase = activateUseCase;
        this.reloadUseCase   = reloadUseCase;
        initialize();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Thread-safe: appends a line to the log area. */
    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void initialize() {
        setTitle("Workflow Manager");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(640, 420);
        setLocationRelativeTo(null);

        buildLayout();
        setupTray();
        reload();
        setVisible(true);
    }

    private void buildLayout() {
        setLayout(new BorderLayout(8, 8));

        // Workflow buttons (top)
        workflowPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        add(new JScrollPane(workflowPanel), BorderLayout.NORTH);

        // Log area (centre)
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Toolbar (bottom)
        JButton reloadBtn = new JButton("Reload Config");
        reloadBtn.addActionListener(e -> reload());
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toolbar.add(reloadBtn);
        add(toolbar, BorderLayout.SOUTH);
    }

    private void setupTray() {
        if (!SystemTray.isSupported()) return;

        var iconUrl = getClass().getResource("/icon.png");
        Image icon  = iconUrl != null
                ? Toolkit.getDefaultToolkit().createImage(iconUrl)
                : new BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);

        TrayIcon trayIcon = new TrayIcon(icon, "Workflow Manager");
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> setVisible(true));

        PopupMenu popup   = new PopupMenu();
        MenuItem showItem = new MenuItem("Show");
        showItem.addActionListener(e -> setVisible(true));
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        popup.add(showItem);
        popup.addSeparator();
        popup.add(exitItem);
        trayIcon.setPopupMenu(popup);

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException ignored) {}
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void reload() {
        new Thread(() -> {
            try {
                List<Workflow> workflows = reloadUseCase.execute(this::onHotkeyActivated);
                SwingUtilities.invokeLater(() -> buildWorkflowButtons(workflows));
            } catch (IOException ex) {
                log("Failed to load config: " + ex.getMessage());
            }
        }, "reload-thread").start();
    }

    private void activate(Workflow workflow) {
        new Thread(() -> activateUseCase.execute(workflow), "activate-thread").start();
    }

    private void onHotkeyActivated(Workflow workflow) {
        new Thread(() -> activateUseCase.execute(workflow), "hotkey-thread").start();
    }

    private void buildWorkflowButtons(List<Workflow> workflows) {
        workflowPanel.removeAll();
        for (Workflow w : workflows) {
            JButton btn = new JButton(w.displayName());
            btn.setToolTipText(w.hasHotkey() ? w.getHotkey().getRaw() : null);
            btn.addActionListener(e -> activate(w));
            workflowPanel.add(btn);
        }
        workflowPanel.revalidate();
        workflowPanel.repaint();
    }
}
