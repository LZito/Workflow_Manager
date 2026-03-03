package at.lzito.workflowmanager.workflow.presentation;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

/**
 * Modal dialog that scans Windows Start Menu shortcuts via PowerShell and
 * presents a filterable list of installed applications. Returns the resolved
 * {@code .exe} path of the selected entry, or {@code null} on cancel.
 */
class InstalledAppPickerDialog extends JDialog {

    private String selectedPath = null;

    private final JTextField              filterField = new JTextField();
    private final DefaultListModel<AppItem> listModel = new DefaultListModel<>();
    private final JList<AppItem>           appList   = new JList<>(listModel);
    private final JLabel                  statusLabel = new JLabel("Scanning Start Menu shortcuts…");
    private final JButton                 okBtn       = new JButton("OK");

    /** Full unfiltered result set, populated on the background thread. */
    private final List<AppItem> allItems = new ArrayList<>();

    // ── Construction ──────────────────────────────────────────────────────────

    InstalledAppPickerDialog(Dialog parent) {
        super(parent, "Browse Installed Apps", true);
        buildUI();
        setSize(500, 440);
        setMinimumSize(new Dimension(400, 320));
        setLocationRelativeTo(parent);
        new Thread(this::scanStartMenu, "start-menu-scan").start();
    }

    /** @return the resolved {@code .exe} path chosen by the user, or {@code null}. */
    String getSelectedPath() {
        return selectedPath;
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private void buildUI() {
        setLayout(new BorderLayout(6, 6));

        // Filter row at top
        JPanel topPanel = new JPanel(new BorderLayout(4, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
        topPanel.add(new JLabel("Filter:"), BorderLayout.WEST);
        topPanel.add(filterField, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // App list
        appList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        appList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) onOK();
            }
        });
        appList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                okBtn.setEnabled(appList.getSelectedIndex() >= 0);
        });
        JScrollPane scroll = new JScrollPane(appList);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 6, 0, 6),
                scroll.getBorder()));
        add(scroll, BorderLayout.CENTER);

        // Status + buttons
        okBtn.setEnabled(false);
        okBtn.addActionListener(e -> onOK());
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnRow.add(okBtn);
        btnRow.add(cancelBtn);

        JPanel south = new JPanel(new BorderLayout(6, 0));
        south.setBorder(BorderFactory.createEmptyBorder(0, 6, 6, 6));
        south.add(statusLabel, BorderLayout.WEST);
        south.add(btnRow, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        // Live filter
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
    }

    // ── Background scan ───────────────────────────────────────────────────────

    private void scanStartMenu() {
        // Use -EncodedCommand (Base64-encoded UTF-16LE) to avoid all quoting issues
        // when passing a multi-statement script to powershell.exe.
        String script = String.join("\n",
            "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8",
            "$sh = New-Object -ComObject WScript.Shell",
            "$dirs = @(",
            "    [System.Environment]::GetFolderPath('CommonPrograms'),",
            "    [System.Environment]::GetFolderPath('Programs')",
            ")",
            "foreach ($dir in $dirs) {",
            "    if (-not (Test-Path $dir)) { continue }",
            "    Get-ChildItem -Path $dir -Recurse -Filter '*.lnk' -ErrorAction SilentlyContinue |",
            "    ForEach-Object {",
            "        try {",
            "            $lnk = $sh.CreateShortcut($_.FullName)",
            "            if ($lnk.TargetPath -like '*.exe') {",
            "                Write-Output ($_.BaseName + '|' + $lnk.TargetPath)",
            "            }",
            "        } catch {}",
            "    }",
            "}"
        );
        String encoded = Base64.getEncoder().encodeToString(
                script.getBytes(StandardCharsets.UTF_16LE));

        List<AppItem> found = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-NonInteractive",
                    "-EncodedCommand", encoded);
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int sep = line.indexOf('|');
                    if (sep > 0) {
                        String name = line.substring(0, sep).trim();
                        String path = line.substring(sep + 1).trim();
                        if (!name.isEmpty() && !path.isEmpty())
                            found.add(new AppItem(name, path));
                    }
                }
            }
            proc.waitFor();
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() ->
                    statusLabel.setText("Scan failed: " + ex.getMessage()));
            return;
        }

        found.sort(Comparator.comparing(a -> a.name.toLowerCase()));

        SwingUtilities.invokeLater(() -> {
            allItems.clear();
            allItems.addAll(found);
            applyFilter();
            statusLabel.setText(found.isEmpty()
                    ? "No apps found."
                    : found.size() + " apps found. Type to filter.");
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyFilter() {
        String text = filterField.getText().trim().toLowerCase();
        listModel.clear();
        for (AppItem item : allItems) {
            if (text.isEmpty() || item.name.toLowerCase().contains(text))
                listModel.addElement(item);
        }
    }

    private void onOK() {
        AppItem sel = appList.getSelectedValue();
        if (sel == null) return;
        selectedPath = sel.path;
        dispose();
    }

    // ── Inner type ────────────────────────────────────────────────────────────

    private static final class AppItem {
        final String name;
        final String path;

        AppItem(String name, String path) {
            this.name = name;
            this.path = path;
        }

        @Override public String toString() { return name; }
    }
}
