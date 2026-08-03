/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 *
 * @author AURA SIMULATOR
 */
public class TextEditor {

    Hexcode h = new Hexcode();
    Preprocessor p;
    Assembler asm;
    AutoCompleter autoCompleter;
    String[] code;

    /** Creates new form text */
    public TextEditor(Assembler obj) {
        //initComponents();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        jScrollPane1.setViewportView(jTextPane1);
        jTextPane1.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 13));
        jTextPane1.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextPane1KeyReleased(evt);
            }
        });
        jTextPane1.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextPane1FocusLost(evt);
            }
        });
        //add(jScrollPane1);
        p = obj.preprocessor;
        asm = obj;
        h.initHexcode();
        code = h.S;
        jTextPane1.getDocument().putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
        autoCompleter = new AutoCompleter(jTextPane1);
    }

    public void setColor(Color c, String s) {
        try {
            StyledDocument doc = jTextPane1.getStyledDocument();
            javax.swing.text.Style style = jTextPane1.addStyle("", null);
            StyleConstants.setForeground(style, c);
            doc.insertString(doc.getLength(), s, style);
            jTextPane1.select(0, 10);
            jTextPane1.setSelectedTextColor(Color.YELLOW);
        } catch (BadLocationException ex) {
            System.out.println(ex);
        }
    }

    public void jTextPane1KeyReleased(java.awt.event.KeyEvent evt) {
        if (!doTextPaneEventHandle(evt)) {
            return;
        }
        colorEditor();
        runLinting();
    }

    public void runLinting() {
        if (asm != null) {
            java.util.List<SyntaxLinter.LintError> errors = SyntaxLinter.lint(jTextPane1.getText());
            asm.updateLintStatus(errors);
        }
    }

    public void jTextPane1FocusLost(java.awt.event.FocusEvent evt) {

        undo[++undoIndex] = jTextPane1.getText();
        try {
            String filepath = System.getProperty("user.dir") + asm.fileSeparator + "backup.dat";
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filepath)));
            out.println(asm.properShutdown);
            out.print(jTextPane1.getText());
            out.close();
        } catch (Exception e) {
        }
    }
    String[] undo = new String[0xFFF];
    int[] caretPos = new int[0xFFF];
    int undoIndex = -1;

    public void undo() {
        try{
            if (undoIndex > 0) {
            jTextPane1.setText(undo[--undoIndex]);
            jTextPane1.setCaretPosition(caretPos[undoIndex]);
        }
        }catch(Exception e){
            System.err.println(e);
        }
    }

    public void redo() {
        try{
            if (undoIndex < undo.length) {
            jTextPane1.setText(undo[++undoIndex]);
        }
        jTextPane1.setCaretPosition(caretPos[undoIndex]);
        }catch(Exception e){
            System.err.println(e);
        }
    }

    public boolean doTextPaneEventHandle(java.awt.event.KeyEvent evt) {
        boolean ok = false;
        if (evt.isControlDown() && evt.getKeyCode() == 90) //undo
        {
            undo();
            ok = true;
        } else if (evt.isControlDown() && evt.getKeyCode() == 89) // redo
        {
            redo();
            ok = true;
        } else if (evt.isControlDown() && evt.getKeyCode() == 65) // select all
        {
            ok = false;
        } else if (evt.getKeyCode() == 17) { //ctrl character
            ok = false;
        }
        else if (evt.isActionKey()) { //ctrl character
            ok = false;
        }
        else {
            undo[undoIndex = (++undoIndex) & undo.length] = jTextPane1.getText();
            caretPos[undoIndex] = jTextPane1.getCaretPosition();
            ok = true;
        }
        return ok;
    }

    public void colorEditor() {
        String s = jTextPane1.getText();
        int pt = jTextPane1.getCaretPosition();
        jTextPane1.setText("");

        boolean isDark = com.formdev.flatlaf.FlatLaf.isLafDark();
        Color defaultTextColor = isDark ? new Color(0xD4D4D4) : Color.black;
        Color c = defaultTextColor, tmpC = defaultTextColor;

        // label marker
        boolean label = false;
        for (int i = s.length() - 2; i >= 0; i--) {
            if (s.charAt(i) == ':') {
                label = true;
            }
            if (label && (s.charAt(i) == '#' || s.charAt(i) == '.' || s.charAt(i) == ';' || s.charAt(i) == '/' || s.charAt(i) == '\n')) {
                label = false;
                s = s.substring(0, i + 1) + (char) (0x207) + s.substring(i + 1);
            }
        }

        for (int i = 0; i < p.code.length; i++) {
            if (!p.code[i].equalsIgnoreCase("")) {
                if (s.contains(p.code[i])) {
                    s = s.replaceAll(p.code[i], (char) (0x200) + p.code[i] + (char) (0x201));
                }
                if (s.contains(p.code[i].toLowerCase())) {
                    s = s.replaceAll(p.code[i].toLowerCase(), (char) (0x200) + p.code[i].toLowerCase() + (char) (0x201));
                }
            }
        }

        for (int i = 0; i < code.length; i++) {
            if (!code[i].equalsIgnoreCase("")) {
                if (s.contains(code[i])) {
                    s = s.replaceAll(code[i], (char) (0x205) + code[i] + (char) (0x206));
                }
                if (s.contains(code[i].toLowerCase())) {
                    s = s.replaceAll(code[i].toLowerCase(), (char) (0x205) + code[i].toLowerCase() + (char) (0x206));
                }
            }
        }

        Color lastColor = defaultTextColor;
        for (int i = 0, mode = 0; i < s.length(); i++) {

            if (s.charAt(i) == '#') {
                c = isDark ? new Color(0xFF6B6B) : new Color(0xCC0000);
                mode = 3;
            } else if (s.charAt(i) == '.') {
                c = isDark ? new Color(0xFF6B6B) : new Color(0xCC0000);
                mode = 3;
            } else if (s.charAt(i) == ';') {
                c = isDark ? new Color(0x6A9955) : new Color(0x808080);
                mode = 2;
            } else if (s.charAt(i) == '/') {
                c = isDark ? new Color(0x6A9955) : new Color(0x808080);
                mode = 2;
            } else if (s.charAt(i) == 0x207) {
                c = isDark ? new Color(0x4EC9B0) : new Color(0x00CC66);
                mode = 1;
            } else if (s.charAt(i) == '\n') {
                c = defaultTextColor;
                mode = 0;
            }

            if (mode < 2) {
                if (s.charAt(i) == 0x205) {
                    c = isDark ? new Color(0x569CD6) : new Color(0x0066CC);
                } else if (s.charAt(i) == 0x206) {
                    c = defaultTextColor;
                }
            }

            if (mode == 3) {
                if (s.charAt(i) == 0x200) {
                    c = isDark ? new Color(0xDCDCAA) : new Color(0x660000);
                } else if (s.charAt(i) == 0x201) {
                    c = isDark ? new Color(0xFF6B6B) : new Color(0xCC0000);
                }
            }

            if (mode != 1) {
                lastColor = c;
            }

            if (s.charAt(i) != 0x205 && s.charAt(i) != 0x206 && s.charAt(i) != 0x207 && s.charAt(i) != 0x200 && s.charAt(i) != 0x201) {
                setColor(c, Character.toString(s.charAt(i)));
            }

            if (s.charAt(i) == ':') {
                c = lastColor;
            }
        }
        if (pt <= jTextPane1.getText().length()) {
            jTextPane1.setCaretPosition(pt);
        }
    }

    public void highligher(int row) {

        int[] line = new int[1000];
        line[0] = 0;
        for (int i = 0, lineNo = 0; i < jTextPane1.getText().length(); i++) {
            if (jTextPane1.getText().charAt(i) == '\n') {
                line[++lineNo] = i;
            }
        }
        try {
            jTextPane1.getHighlighter().removeAllHighlights();
            jTextPane1.setCaretPosition(line[row]);
            jTextPane1.getHighlighter().addHighlight(line[row], line[row + 1], new DefaultHighlighter.DefaultHighlightPainter(new Color(0xFF3333)));
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                //new TextEditor().setVisible(true);
            }
        });
    }
    public javax.swing.JScrollPane jScrollPane1;
    public javax.swing.JTextField jTextField1;
    public javax.swing.JTextPane jTextPane1;
}
