package navigatorlh.View;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

public class AutoComplete extends JComboBox implements JComboBox.KeySelectionManager {
    private String searchFor;
    private long lap;

    public class CBDocument extends PlainDocument {
        @Override
        public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
            if (str == null)
                return;
            super.insertString(offset, str, a);
            if (!isPopupVisible() && str.length() != 0)
                fireActionEvent();
        }
    }

    public AutoComplete(Object[] items) {
        super(items);
        lap = new java.util.Date().getTime();
        setKeySelectionManager(this);
        JTextField tf;
        if (getEditor() != null) {
            tf = (JTextField) getEditor().getEditorComponent();
            if (tf != null) {
                tf.setDocument(new CBDocument());
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        JTextField tf = (JTextField) getEditor().getEditorComponent();
                        String text = tf.getText();
                        ComboBoxModel aModel = getModel();
                        String current;
                        for (int i = 0; i < aModel.getSize(); i++) {
                            current = aModel.getElementAt(i).toString();
                            if (current.toLowerCase().startsWith(text.toLowerCase())) {
                                tf.setText(current);
                                tf.setSelectionStart(text.length());
                                tf.setSelectionEnd(current.length());
                                break;
                            }
                        }
                    }
                });
            }
        }
    }

    public int selectionForKey(char aKey, ComboBoxModel aModel) {
        long now = new java.util.Date().getTime();
        if (searchFor != null && aKey == KeyEvent.VK_BACK_SPACE && searchFor.length() > 0) {
            searchFor = searchFor.substring(0, searchFor.length() - 1);
        } else {
            if (lap + 1000 < now)
                searchFor = "" + aKey;
            else
                searchFor = searchFor + aKey;
        }
        lap = now;
        String current;
        for (int i = 0; i < aModel.getSize(); i++) {
            current = aModel.getElementAt(i).toString().toLowerCase();
            if (current.toLowerCase().startsWith(searchFor.toLowerCase()))
                return i;
        }
        return -1;
    }

    @Override
    public void fireActionEvent() {
        super.fireActionEvent();
    }

    public static void main(String arg[]) {
        JFrame f = new JFrame("AutoCompletion");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(300, 100);
        Container cp = f.getContentPane();
        cp.setLayout(new GridLayout(2, 1));

        Locale[] locales = Locale.getAvailableLocales();
        // JComboBox 1 allowing any input
        JComboBox cBox = new AutoComplete(locales);

        java.util.List<String> items = new ArrayList<String>() {
        };
        for (int i = 0; i < locales.length; i++) {
            items.add(locales[i].toString());
        }

        // JComboBox 2 with strict input
        JComboBox cBox2 = new AutoComboBox(items);
        cBox.setBounds(50, 50, 100, 21);
        cBox.setEditable(true);
        cp.add(cBox);
        cp.add(cBox2);
        f.setVisible(true);
        f.setResizable(false);
        f.setLocationRelativeTo(null);
    }
}
