package dialog;

import common.Settings;
import ui.dialog.RightPanel;

import javax.swing.*;

// unable to test this, some mysterious error happens
public class RightPanelTest {

    public static void main(String[] args) {
        // create a frame to hold the components
        JFrame frame = new JFrame("RightPanelTest");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(new RightPanel().createRightPanel(new Settings(),
                settings -> System.out.println("Settings was changed.")));

        // pack the frame and make it visible
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.requestFocus();
    }
}