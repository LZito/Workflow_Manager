package at.lzito.workflowmanager.workflow.presentation;

import at.lzito.workflowmanager.workflow.domain.AppEntry;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Modal dialog for adding or editing a single {@link AppEntry}.
 * Returns {@code null} if the user cancels.
 */
class AppEntryEditorDialog extends JDialog {

    private AppEntry result = null;

    private final JTextField  nameField    = new JTextField(22);
    private final JRadioButton appRadio    = new JRadioButton("Application", true);
    private final JRadioButton urlRadio    = new JRadioButton("URL");
    private final JLabel      pathLabel    = new JLabel("Path:");
    private final JTextField  pathField    = new JTextField(22);
    private final JButton     browseBtn    = new JButton("Browse…");
    private final JButton     installedBtn = new JButton("Installed…");
    private       JPanel      pathBtns;          // holds browseBtn + installedBtn
    private final JLabel      urlLabel    = new JLabel("URL:");
    private final JTextField  urlField    = new JTextField(22);
    private final JTextField  argsField   = new JTextField(22);
    private final JSpinner    delaySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 60_000, 100));

    AppEntryEditorDialog(Dialog parent, AppEntry existing) {
        super(parent, existing == null ? "Add Entry" : "Edit Entry", true);
        buildUI();
        if (existing != null) populate(existing);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    AppEntry getResult() {
        return result;
    }

    // ── Build UI ──────────────────────────────────────────────────────────────

    private void buildUI() {
        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(appRadio);
        typeGroup.add(urlRadio);
        appRadio.addActionListener(e -> updateTypeVisibility());
        urlRadio.addActionListener(e -> updateTypeVisibility());

        browseBtn.addActionListener(e -> browse());
        installedBtn.addActionListener(e -> browseInstalled());
        pathBtns = new JPanel(new GridLayout(1, 2, 2, 0));
        pathBtns.add(browseBtn);
        pathBtns.add(installedBtn);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(4, 4, 4, 4);
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Label
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        form.add(new JLabel("Label:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        form.add(nameField, gbc);
        row++;

        // Type
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        form.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0;
        form.add(appRadio, gbc);
        gbc.gridx = 2;
        form.add(urlRadio, gbc);
        row++;

        // Path
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        form.add(pathLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(pathField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        form.add(pathBtns, gbc);
        row++;

        // URL
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        form.add(urlLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        form.add(urlField, gbc);
        gbc.gridwidth = 1;
        row++;

        // Args
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        form.add(new JLabel("Args:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        form.add(argsField, gbc);
        gbc.gridwidth = 1;
        row++;

        // Delay
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        form.add(new JLabel("Delay (ms):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0; gbc.gridwidth = 2;
        form.add(delaySpinner, gbc);
        gbc.gridwidth = 1;

        // Buttons
        JButton okBtn     = new JButton("OK");
        JButton cancelBtn = new JButton("Cancel");
        okBtn.addActionListener(e -> onOK());
        cancelBtn.addActionListener(e -> dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(okBtn);
        btnPanel.add(cancelBtn);

        setLayout(new BorderLayout(6, 6));
        add(form, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        updateTypeVisibility();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updateTypeVisibility() {
        boolean isApp = appRadio.isSelected();
        pathLabel.setVisible(isApp);
        pathField.setVisible(isApp);
        pathBtns.setVisible(isApp);
        urlLabel.setVisible(!isApp);
        urlField.setVisible(!isApp);
    }

    private void browseInstalled() {
        InstalledAppPickerDialog dlg = new InstalledAppPickerDialog(this);
        dlg.setVisible(true);
        String path = dlg.getSelectedPath();
        if (path != null) {
            pathField.setText(path);
            // Auto-fill label from the exe filename if the label field is still empty
            if (nameField.getText().trim().isEmpty()) {
                String fileName = path.contains("\\")
                        ? path.substring(path.lastIndexOf('\\') + 1)
                        : path.substring(path.lastIndexOf('/') + 1);
                if (fileName.toLowerCase().endsWith(".exe"))
                    fileName = fileName.substring(0, fileName.length() - 4);
                nameField.setText(fileName);
            }
        }
    }

    private void browse() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Executable");
        fc.setAcceptAllFileFilterUsed(true);
        FileNameExtensionFilter execFilter = new FileNameExtensionFilter(
                "Executables (*.exe, *.sh, *.bat, *.cmd)", "exe", "sh", "bat", "cmd");
        fc.addChoosableFileFilter(execFilter);
        fc.setFileFilter(execFilter);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void populate(AppEntry entry) {
        nameField.setText(entry.getName() != null ? entry.getName() : "");
        if (entry.isUrl()) {
            urlRadio.setSelected(true);
            urlField.setText(entry.getUrl() != null ? entry.getUrl() : "");
        } else {
            appRadio.setSelected(true);
            pathField.setText(entry.getPath() != null ? entry.getPath() : "");
        }
        argsField.setText(String.join(" ", entry.getArgs()));
        delaySpinner.setValue(entry.getDelayMs());
        updateTypeVisibility();
    }

    private void onOK() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Label must not be empty.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<String> args = argsField.getText().trim().isEmpty()
                ? List.of()
                : Arrays.asList(argsField.getText().trim().split("\\s+"));
        int delay = (int) delaySpinner.getValue();

        if (urlRadio.isSelected()) {
            result = new AppEntry(name, null, urlField.getText().trim(), args, delay);
        } else {
            result = new AppEntry(name, pathField.getText().trim(), null, args, delay);
        }
        dispose();
    }
}
