package at.lzito.workflowmanager.workflow.presentation;

import at.lzito.workflowmanager.workflow.application.WorkflowAppService;
import at.lzito.workflowmanager.workflow.domain.AppEntry;
import at.lzito.workflowmanager.workflow.domain.Hotkey;
import at.lzito.workflowmanager.workflow.domain.Workflow;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal dialog that lets the user view and edit the full workflow configuration.
 *
 * <p>Layout:
 * <pre>
 *  ┌─WEST(180)─┬──────────CENTER──────────────┐
 *  │ [list]    │ ┌─Workflow─────────────────┐  │
 *  │           │ │ Name / Icon / Hotkey     │  │
 *  │           │ └──────────────────────────┘  │
 *  │           │ ┌─Apps to Open─────────────┐  │
 *  │           │ │ table + Add/Edit/Remove   │  │
 *  │           │ └──────────────────────────┘  │
 *  │ [+] [−]   │ ┌─Processes to Close───────┐  │
 *  │           │ │ list + field + Add/Remove │  │
 *  └───────────┴─┴──────────────────────────┴──┘
 *  [Clear Config]              [Save & Apply] [Cancel]
 * </pre>
 */
public class ConfigEditorDialog extends JDialog {

    private final WorkflowAppService appService;
    private final Runnable           onSaved;

    // ── Internal mutable model ────────────────────────────────────────────────

    private final List<WfEntry> model = new ArrayList<>();
    private int selectedIdx = -1;

    // ── Left panel ────────────────────────────────────────────────────────────

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String>            wfList    = new JList<>(listModel);

    // ── Workflow form ─────────────────────────────────────────────────────────

    private final JTextField nameField        = new JTextField(18);
    private final JTextField iconField        = new JTextField(4);
    private final JTextField hotkeyField      = new JTextField(14);
    private final JCheckBox  closeOthersCheck = new JCheckBox("Close other apps when activated");

    // ── Apps table ────────────────────────────────────────────────────────────

    private final DefaultTableModel appsTableModel = new DefaultTableModel(
            new String[]{"Label", "Type", "Path / URL", "Delay (ms)"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable appsTable = new JTable(appsTableModel);

    // ── Processes list ────────────────────────────────────────────────────────

    private final DefaultListModel<String> processListModel = new DefaultListModel<>();
    private final JList<String>            processesList    = new JList<>(processListModel);
    private final JTextField               newProcessField  = new JTextField(18);

    // ─────────────────────────────────────────────────────────────────────────

    public ConfigEditorDialog(JFrame parent, WorkflowAppService appService, Runnable onSaved) {
        super(parent, "Edit Config", true);
        this.appService = appService;
        this.onSaved    = onSaved;

        loadModel();
        buildUI();
        setSize(720, 530);
        setMinimumSize(new Dimension(600, 420));
        setLocationRelativeTo(parent);
    }

    // ── Model initialisation ──────────────────────────────────────────────────

    private void loadModel() {
        model.clear();
        for (Workflow w : appService.getAll()) {
            model.add(WfEntry.from(w));
        }
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        setLayout(new BorderLayout(6, 6));

        JPanel westPanel = buildWestPanel();
        westPanel.setPreferredSize(new Dimension(180, 0));

        JPanel centerPanel = buildCenterPanel();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, westPanel, centerPanel);
        split.setDividerLocation(180);
        split.setResizeWeight(0.0);
        add(split, BorderLayout.CENTER);
        add(buildSouthPanel(), BorderLayout.SOUTH);

        // Listener must be attached before setSelectedIndex so loadEntry fires
        wfList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int newIdx = wfList.getSelectedIndex();
            commitCurrentEntry();
            selectedIdx = newIdx;
            if (newIdx >= 0) loadEntry(newIdx);
            else             clearForm();
        });

        refreshListModel();
        if (!model.isEmpty()) {
            wfList.setSelectedIndex(0);
        }
    }

    private JPanel buildWestPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 0));

        wfList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(wfList), BorderLayout.CENTER);

        JButton addBtn    = new JButton("+");
        JButton removeBtn = new JButton("−");
        addBtn.setToolTipText("Add workflow");
        removeBtn.setToolTipText("Remove selected workflow");
        addBtn.addActionListener(e -> addWorkflow());
        removeBtn.addActionListener(e -> removeWorkflow());

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 4, 0));
        btnRow.add(addBtn);
        btnRow.add(removeBtn);
        panel.add(btnRow, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(buildFormPanel(),      BorderLayout.NORTH);
        panel.add(buildAppsPanel(),      BorderLayout.CENTER);
        panel.add(buildProcessesPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Workflow"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: Name  |  Icon
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(nameField, gbc);
        gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Icon:"), gbc);
        gbc.gridx = 3;
        panel.add(iconField, gbc);

        // Row 1: Hotkey  |  Close Others checkbox
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Hotkey:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(hotkeyField, gbc);
        gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.gridwidth = 2;
        panel.add(closeOthersCheck, gbc);
        gbc.gridwidth = 1;

        return panel;
    }

    private JPanel buildAppsPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Apps to Open"));

        appsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        appsTable.setRowHeight(22);
        appsTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        appsTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        appsTable.getColumnModel().getColumn(2).setPreferredWidth(250);
        appsTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        panel.add(new JScrollPane(appsTable), BorderLayout.CENTER);

        JButton addBtn    = new JButton("Add");
        JButton editBtn   = new JButton("Edit");
        JButton removeBtn = new JButton("Remove");
        addBtn.addActionListener(e -> addApp());
        editBtn.addActionListener(e -> editApp());
        removeBtn.addActionListener(e -> removeApp());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btns.add(addBtn);
        btns.add(editBtn);
        btns.add(removeBtn);
        panel.add(btns, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildProcessesPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Processes to Close"));
        panel.setPreferredSize(new Dimension(0, 140));

        processesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(processesList), BorderLayout.CENTER);

        JButton addBtn    = new JButton("Add");
        JButton removeBtn = new JButton("Remove");
        addBtn.addActionListener(e -> addProcess());
        removeBtn.addActionListener(e -> removeProcess());

        JPanel inputRow = new JPanel(new BorderLayout(4, 0));
        inputRow.add(newProcessField, BorderLayout.CENTER);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btns.add(addBtn);
        btns.add(removeBtn);
        inputRow.add(btns, BorderLayout.EAST);
        panel.add(inputRow, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildSouthPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 6, 6, 6));

        JButton saveBtn   = new JButton("Save & Apply");
        JButton cancelBtn = new JButton("Cancel");
        JButton clearBtn  = new JButton("Clear Config");

        saveBtn.addActionListener(e -> onSave());
        cancelBtn.addActionListener(e -> dispose());
        clearBtn.addActionListener(e -> onClear());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.add(saveBtn);
        right.add(cancelBtn);

        panel.add(clearBtn, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    // ── Workflow list actions ─────────────────────────────────────────────────

    private void addWorkflow() {
        WfEntry e = new WfEntry("New Workflow", "", "", false,
                new ArrayList<>(), new ArrayList<>());
        model.add(e);
        listModel.addElement(buildListLabel(e));
        wfList.setSelectedIndex(model.size() - 1);   // triggers listener → commit + load
    }

    private void removeWorkflow() {
        if (selectedIdx < 0) return;
        String name = model.get(selectedIdx).name;
        int confirm = JOptionPane.showConfirmDialog(this,
                "Remove workflow \"" + name + "\"?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        int idxToRemove = selectedIdx;
        selectedIdx = -1;               // prevent stale commits during list events
        model.remove(idxToRemove);
        listModel.remove(idxToRemove);

        int newIdx = Math.min(idxToRemove, model.size() - 1);
        if (newIdx >= 0) {
            wfList.setSelectedIndex(newIdx);
        } else {
            clearForm();
        }
    }

    // ── App entry actions ─────────────────────────────────────────────────────

    private void addApp() {
        if (selectedIdx < 0) return;
        AppEntryEditorDialog dlg = new AppEntryEditorDialog(this, null);
        dlg.setVisible(true);
        AppEntry result = dlg.getResult();
        if (result != null) {
            model.get(selectedIdx).apps.add(result);
            refreshAppsTable();
        }
    }

    private void editApp() {
        if (selectedIdx < 0) return;
        int row = appsTable.getSelectedRow();
        if (row < 0) return;
        AppEntry existing = model.get(selectedIdx).apps.get(row);
        AppEntryEditorDialog dlg = new AppEntryEditorDialog(this, existing);
        dlg.setVisible(true);
        AppEntry result = dlg.getResult();
        if (result != null) {
            model.get(selectedIdx).apps.set(row, result);
            refreshAppsTable();
        }
    }

    private void removeApp() {
        if (selectedIdx < 0) return;
        int row = appsTable.getSelectedRow();
        if (row < 0) return;
        model.get(selectedIdx).apps.remove(row);
        refreshAppsTable();
    }

    // ── Process list actions ──────────────────────────────────────────────────

    private void addProcess() {
        if (selectedIdx < 0) return;
        String proc = newProcessField.getText().trim();
        if (proc.isEmpty()) return;
        model.get(selectedIdx).processes.add(proc);
        processListModel.addElement(proc);
        newProcessField.setText("");
    }

    private void removeProcess() {
        if (selectedIdx < 0) return;
        int row = processesList.getSelectedIndex();
        if (row < 0) return;
        model.get(selectedIdx).processes.remove(row);
        processListModel.remove(row);
    }

    // ── Bottom button actions ─────────────────────────────────────────────────

    private void onSave() {
        commitCurrentEntry();
        List<Workflow> workflows = model.stream()
                .filter(e -> !e.name.isBlank())
                .map(WfEntry::toWorkflow)
                .toList();
        try {
            appService.save(workflows);
            dispose();
            onSaved.run();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onClear() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete all workflows and reset config?\nThis cannot be undone.",
                "Clear Config", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            appService.resetConfig();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Reset failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        selectedIdx = -1;
        model.clear();
        refreshListModel();
        clearForm();
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void commitCurrentEntry() {
        if (selectedIdx < 0 || selectedIdx >= model.size()) return;
        WfEntry e   = model.get(selectedIdx);
        e.name        = nameField.getText().trim();
        e.icon        = iconField.getText().trim();
        e.hotkey      = hotkeyField.getText().trim();
        e.closeOthers = closeOthersCheck.isSelected();
        listModel.set(selectedIdx, buildListLabel(e));
    }

    private void loadEntry(int idx) {
        WfEntry e = model.get(idx);
        nameField.setText(e.name   != null ? e.name   : "");
        iconField.setText(e.icon   != null ? e.icon   : "");
        hotkeyField.setText(e.hotkey != null ? e.hotkey : "");
        closeOthersCheck.setSelected(e.closeOthers);
        refreshAppsTable();
        refreshProcessesList();
    }

    private void clearForm() {
        nameField.setText("");
        iconField.setText("");
        hotkeyField.setText("");
        closeOthersCheck.setSelected(false);
        appsTableModel.setRowCount(0);
        processListModel.clear();
    }

    private void refreshListModel() {
        listModel.clear();
        for (WfEntry e : model) listModel.addElement(buildListLabel(e));
    }

    private void refreshAppsTable() {
        appsTableModel.setRowCount(0);
        if (selectedIdx < 0 || selectedIdx >= model.size()) return;
        for (AppEntry e : model.get(selectedIdx).apps) {
            appsTableModel.addRow(new Object[]{
                    e.getName(),
                    e.isUrl() ? "URL" : "Application",
                    e.isUrl() ? e.getUrl() : e.getPath(),
                    e.getDelayMs()
            });
        }
    }

    private void refreshProcessesList() {
        processListModel.clear();
        if (selectedIdx < 0 || selectedIdx >= model.size()) return;
        for (String proc : model.get(selectedIdx).processes) {
            processListModel.addElement(proc);
        }
    }

    private String buildListLabel(WfEntry e) {
        return (e.icon != null && !e.icon.isBlank()) ? e.icon + " " + e.name : e.name;
    }

    // ── Mutable workflow holder (editor-internal only) ────────────────────────

    private static class WfEntry {
        String        name, icon, hotkey;
        boolean       closeOthers;
        List<AppEntry> apps;
        List<String>   processes;

        WfEntry(String name, String icon, String hotkey, boolean closeOthers,
                List<AppEntry> apps, List<String> processes) {
            this.name         = name;
            this.icon         = icon;
            this.hotkey       = hotkey;
            this.closeOthers  = closeOthers;
            this.apps         = apps      != null ? new ArrayList<>(apps)      : new ArrayList<>();
            this.processes    = processes != null ? new ArrayList<>(processes)  : new ArrayList<>();
        }

        static WfEntry from(Workflow w) {
            return new WfEntry(
                    w.getName(),
                    w.getIcon(),
                    w.hasHotkey() ? w.getHotkey().getRaw() : "",
                    w.isCloseOthers(),
                    w.getAppsToOpen(),
                    w.getProcessesToClose());
        }

        Workflow toWorkflow() {
            String resolvedName = (name == null || name.isBlank()) ? "Unnamed" : name;
            Hotkey resolvedHotkey = (hotkey == null || hotkey.isBlank()) ? null : Hotkey.of(hotkey);
            return new Workflow(resolvedName, icon, resolvedHotkey,
                    new ArrayList<>(apps), new ArrayList<>(processes), closeOthers);
        }
    }
}
