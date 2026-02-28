package at.lzito.workflowmanager;

import com.formdev.flatlaf.FlatDarkLaf;
import at.lzito.workflowmanager.ui.MainWindow;

import javax.swing.*;

public class App {

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(MainWindow::new);
    }
}
