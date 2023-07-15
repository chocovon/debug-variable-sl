import common.Settings;
import ui.dialog.RightPanel;

import javax.swing.*;

public class RightPanelTest {

    public static void main(String[] args) {
        // create a frame to hold the components
        JFrame frame = new JFrame("RightPanelTest");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(new RightPanel().getRightPanel(new Settings(), settings1 -> {
            System.out.println("Settings was changed.");
        }));

        // pack the frame and make it visible
        frame.pack();
        frame.setVisible(true);
    }
}