package at.lzito.workflowmanager.workflow.presentation;

import at.lzito.workflowmanager.workflow.application.ActivateWorkflowUseCase;
import at.lzito.workflowmanager.workflow.application.ReloadWorkflowsUseCase;
import at.lzito.workflowmanager.workflow.application.SaveWorkflowsUseCase;
import at.lzito.workflowmanager.workflow.domain.Workflow;
import at.lzito.workflowmanager.workflow.domain.WorkflowRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * Main application window.
 *
 * <p>Layout:
 * <pre>
 *  ┌── header ─────────────────────────────────────┐
 *  │ Workflow Manager          [Edit Config][Reload] │
 *  ├── workflow area (CENTER, scrollable) ──────────┤
 *  │  [💻 Coding] [🎮 Gaming] [📧 Email]           │
 *  │  [🔧 Dev]                                      │
 *  ├── status bar ──────────────────────────────────┤
 *  │ ● Loaded 4 workflows                           │
 *  └────────────────────────────────────────────────┘
 * </pre>
 */
public class MainWindow extends JFrame {

    // ── Status colours ────────────────────────────────────────────────────────

    private static final Color COLOR_SUCCESS = new Color(0x4CAF50);
    private static final Color COLOR_ERROR   = new Color(0xEF5350);
    private static final Color COLOR_INFO    = new Color(0x9E9E9E);

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final ActivateWorkflowUseCase activateUseCase;
    private final ReloadWorkflowsUseCase  reloadUseCase;
    private final SaveWorkflowsUseCase    saveUseCase;
    private final WorkflowRepository      repository;
    private final boolean                 firstRun;

    // ── UI state ──────────────────────────────────────────────────────────────

    private final JPanel  workflowPanel = new JPanel(new WrapLayout());
    private final JLabel  statusLabel   = new JLabel(" ");
    private       TrayIcon trayIcon;

    // ─────────────────────────────────────────────────────────────────────────

    public MainWindow(ActivateWorkflowUseCase activateUseCase,
                      ReloadWorkflowsUseCase  reloadUseCase,
                      SaveWorkflowsUseCase    saveUseCase,
                      WorkflowRepository      repository,
                      boolean                 firstRun) {
        this.activateUseCase = activateUseCase;
        this.reloadUseCase   = reloadUseCase;
        this.saveUseCase     = saveUseCase;
        this.repository      = repository;
        this.firstRun        = firstRun;
        initialize();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Thread-safe. Colours the status bar red if the message looks like an error. */
    public void log(String message) {
        boolean isError = message.toLowerCase().contains("fail")
                       || message.toLowerCase().contains("error");
        setStatus(message, isError ? COLOR_ERROR : COLOR_INFO);
    }

    public void openConfigEditor() {
        ConfigEditorDialog dlg = new ConfigEditorDialog(this, saveUseCase, repository, this::reload);
        dlg.setVisible(true);
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    private void initialize() {
        setTitle("Workflow Manager");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                onCloseRequested();
            }
        });
        setSize(740, 340);
        setMinimumSize(new Dimension(500, 220));
        setLocationRelativeTo(null);

        buildLayout();
        setupTray();
        reload();
        setVisible(true);

        if (firstRun) openConfigEditor();
    }

    private void buildLayout() {
        setLayout(new BorderLayout());

        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, separatorColor()),
                new EmptyBorder(8, 14, 8, 10)));

        JLabel title = new JLabel("Workflow Manager");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        header.add(title, BorderLayout.WEST);

        JButton editBtn   = new JButton("Edit Config");
        JButton reloadBtn = new JButton("Reload");
        editBtn.addActionListener(e -> openConfigEditor());
        reloadBtn.addActionListener(e -> reload());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        buttons.add(editBtn);
        buttons.add(reloadBtn);
        header.add(buttons, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ── Workflow area ──────────────────────────────────────────────────────
        workflowPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane scroll = new JScrollPane(workflowPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        // ── Status bar ────────────────────────────────────────────────────────
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, separatorColor()),
                new EmptyBorder(5, 14, 5, 14)));
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));
        statusLabel.setForeground(COLOR_INFO);
        statusBar.add(statusLabel, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);
    }

    private void setupTray() {
        if (!SystemTray.isSupported()) return;

        var iconUrl = getClass().getResource("/icon.png");
        Image icon = iconUrl != null
                ? Toolkit.getDefaultToolkit().createImage(iconUrl)
                : new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        trayIcon = new TrayIcon(icon, "Workflow Manager");
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> SwingUtilities.invokeLater(() -> {
            setVisible(true);
            toFront();
        }));

        PopupMenu popup   = new PopupMenu();
        MenuItem showItem = new MenuItem("Show");
        showItem.addActionListener(e -> SwingUtilities.invokeLater(() -> {
            setVisible(true);
            toFront();
        }));
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

    private void onCloseRequested() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Keep Workflow Manager running in the background?",
                "Close",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            setVisible(false);
        } else if (choice == JOptionPane.NO_OPTION) {
            System.exit(0);
        }
    }

    void reload() {
        new Thread(() -> {
            try {
                List<Workflow> workflows = reloadUseCase.execute(this::onHotkeyActivated);
                SwingUtilities.invokeLater(() -> {
                    buildWorkflowButtons(workflows);
                    setStatus("Loaded " + workflows.size()
                            + (workflows.size() == 1 ? " workflow" : " workflows"),
                            COLOR_INFO);
                });
            } catch (IOException ex) {
                setStatus("Failed to load config: " + ex.getMessage(), COLOR_ERROR);
            }
        }, "reload-thread").start();
    }

    private void activate(Workflow workflow) {
        setStatus("Activating " + workflow.displayName() + "…", COLOR_INFO);
        new Thread(() -> {
            activateUseCase.execute(workflow);
            setStatus("Activated: " + workflow.displayName(), COLOR_SUCCESS);
        }, "activate-thread").start();
    }

    private void onHotkeyActivated(Workflow workflow) {
        new Thread(() -> {
            activateUseCase.execute(workflow);
            setStatus("Activated: " + workflow.displayName(), COLOR_SUCCESS);
            if (trayIcon != null) {
                trayIcon.displayMessage(
                        "Workflow activated",
                        workflow.displayName(),
                        TrayIcon.MessageType.INFO);
            }
        }, "hotkey-thread").start();
    }

    private void buildWorkflowButtons(List<Workflow> workflows) {
        workflowPanel.removeAll();
        if (workflows.isEmpty()) {
            JLabel hint = new JLabel("No workflows yet — click Edit Config to get started.");
            hint.setForeground(COLOR_INFO);
            hint.setBorder(new EmptyBorder(20, 4, 20, 4));
            workflowPanel.add(hint);
        } else {
            for (Workflow w : workflows) {
                workflowPanel.add(makeWorkflowButton(w));
            }
        }
        workflowPanel.revalidate();
        workflowPanel.repaint();
    }

    private JButton makeWorkflowButton(Workflow w) {
        JButton btn = new JButton(w.displayName());
        btn.setFont(btn.getFont().deriveFont(13f));
        btn.setPreferredSize(new Dimension(148, 52));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        if (w.hasHotkey()) btn.setToolTipText(w.getHotkey().getRaw());
        btn.addActionListener(e -> activate(w));
        return btn;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setStatus(String message, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
            statusLabel.setForeground(color);
        });
    }

    private static Color separatorColor() {
        Color c = UIManager.getColor("Separator.foreground");
        return c != null ? c : new Color(0x555555);
    }

    // ── WrapLayout — FlowLayout with correct preferred-height when wrapping ───

    private static final class WrapLayout extends FlowLayout {

        WrapLayout() { super(FlowLayout.LEFT, 8, 8); }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return compute(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension d = compute(target, false);
            d.width -= (getHgap() + 1);
            return d;
        }

        private Dimension compute(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                // Walk up to find a real width when not yet laid out
                int targetWidth = target.getSize().width;
                Container probe = target;
                while (targetWidth == 0 && probe.getParent() != null) {
                    probe = probe.getParent();
                    targetWidth = probe.getSize().width;
                }
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

                int hgap = getHgap(), vgap = getVgap();
                Insets ins = target.getInsets();
                int maxW = targetWidth - ins.left - ins.right - hgap * 2;

                Dimension dim = new Dimension(0, 0);
                int rowW = 0, rowH = 0;

                for (Component c : target.getComponents()) {
                    if (!c.isVisible()) continue;
                    Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                    if (rowW > 0 && rowW + hgap + d.width > maxW) {
                        commitRow(dim, rowW, rowH, vgap);
                        rowW = 0;
                        rowH = 0;
                    }
                    rowW += (rowW == 0 ? 0 : hgap) + d.width;
                    rowH = Math.max(rowH, d.height);
                }
                commitRow(dim, rowW, rowH, vgap);

                dim.width  += ins.left + ins.right  + hgap * 2;
                dim.height += ins.top  + ins.bottom + vgap * 2;
                return dim;
            }
        }

        private static void commitRow(Dimension dim, int w, int h, int vgap) {
            if (h == 0) return;
            dim.width = Math.max(dim.width, w);
            if (dim.height > 0) dim.height += vgap;
            dim.height += h;
        }
    }
}
