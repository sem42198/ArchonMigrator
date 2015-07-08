/*
 * Created by JFormDesigner on Thu Jan 03 10:26:44 EST 2013
 */

package org.nyu.edu.dlts;

import java.awt.event.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.nyu.edu.dlts.utils.ASpaceClient;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Simple dialog for viewing or editing source code with syntax highlighting
 *
 * @author Nathan Stevens
 */
public class CodeViewerDialog extends JDialog {
    private RSyntaxTextArea textArea;
    private boolean editable = false;

    /**
     * Constructor which code is past in
     *
     * @param owner
     * @param code
     * @param syntaxStyle
     */
    public CodeViewerDialog(Frame owner, String syntaxStyle,  String code, boolean editable) {
        super(owner);
        initComponents();

        this.editable = editable;

        // add the syntax area now
        textArea = new RSyntaxTextArea(30, 100);
        textArea.setSyntaxEditingStyle(syntaxStyle);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setEditable(editable);
        textArea.setText(code);

        RTextScrollPane sp = new RTextScrollPane(textArea);
        sp.setFoldIndicatorEnabled(true);

        contentPanel.add(sp, BorderLayout.CENTER);

        // make sure we open this window somewhere that make sense
        setLocation(owner.getLocationOnScreen());
    }



    /**
     * Method to set the script that is displayed
     *
     * @param script
     */
    public void setCurrentScript(String script) {
        textArea.setText(script);
    }

    /**
     * Method to return the current script, for example after it been edited
     *
     * @return The script
     */
    public String getCurrentScript() {
        return textArea.getText();
    }

    /**
     * Close the dialog when the window is closed
     */
    private void okButtonActionPerformed() {
        setVisible(false);

        if(!editable) {
            dispose();
        }
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        scrollPane1 = new JScrollPane();
        messageTextArea = new JTextArea();
        buttonBar = new JPanel();
        okButton = new JButton();

        //======== this ========
        setTitle("Code Viewer");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BorderLayout());

                //======== scrollPane1 ========
                {

                    //---- messageTextArea ----
                    messageTextArea.setRows(4);
                    messageTextArea.setEditable(false);
                    scrollPane1.setViewportView(messageTextArea);
                }
                contentPanel.add(scrollPane1, BorderLayout.SOUTH);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0};

                //---- okButton ----
                okButton.setText("OK");
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        okButtonActionPerformed();
                    }
                });
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JScrollPane scrollPane1;
    private JTextArea messageTextArea;
    private JPanel buttonBar;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
