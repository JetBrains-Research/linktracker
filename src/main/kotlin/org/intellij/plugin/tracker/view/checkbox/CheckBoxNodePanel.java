package org.intellij.plugin.tracker.view.checkbox;

import java.awt.BorderLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Helper class defining Swing components for check box node renderer UI.
 */
public class CheckBoxNodePanel extends JPanel {

    public final JLabel label = new JLabel();
    public final JCheckBox check = new JCheckBox();

    public CheckBoxNodePanel() {
        this.check.setMargin(new Insets(0, 0, 0, 0));
        setLayout(new BorderLayout());
        add(check, BorderLayout.WEST);
        add(label, BorderLayout.CENTER);
    }

}
