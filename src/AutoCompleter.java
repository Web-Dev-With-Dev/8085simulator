import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.text.*;

public class AutoCompleter {

    private final JTextPane textPane;
    private final JPopupMenu popupMenu;
    private final JList<String> list;
    private final DefaultListModel<String> listModel;
    private final JScrollPane scrollPane;

    private static final String[] MNEMONICS = {
            "ADC", "ADD", "ADI", "ANA", "ANI", "CALL", "CC", "CM", "CMA", "CMC",
            "CMP", "CPI", "CPE", "CPO", "CNZ", "CZ", "DAA", "DAD", "DCR", "DCX",
            "DI", "EI", "HLT", "IN", "INR", "INX", "JC", "JM", "JMP", "JNC",
            "JNZ", "JPE", "JPO", "JZ", "LDA", "LDAX", "LHLD", "LXI", "MOV", "MVI",
            "NOP", "ORA", "ORI", "OUT", "POP", "PUSH", "RAL", "RAR", "RLC", "RRC",
            "RIM", "SIM", "RET", "RC", "RM", "RNC", "RNZ", "RPE", "RPO", "RZ",
            "RST", "SHLD", "SPHL", "STA", "STAX", "STC", "SUB", "SBB", "SUI",
            "XCHG", "XRA", "XRI", "XTHL", "DB", "DW", "EQU", "ORG", "#DEFINE"
    };

    private static final String[] REGISTERS = {
            "A", "B", "C", "D", "E", "H", "L", "M", "SP", "PSW"
    };

    public AutoCompleter(JTextPane pane) {
        this.textPane = pane;
        this.popupMenu = new JPopupMenu();
        this.popupMenu.setFocusable(false);

        this.listModel = new DefaultListModel<>();
        this.list = new JList<>(listModel);
        this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.list.setFocusable(false);
        this.list.setBackground(new Color(0x25, 0x25, 0x26));
        this.list.setForeground(new Color(0xE0, 0xE0, 0xE0));
        this.list.setSelectionBackground(new Color(0x00, 0x7A, 0xCC));
        this.list.setSelectionForeground(Color.WHITE);
        this.list.setFont(new Font("Monospaced", Font.BOLD, 12));
        this.list.setFixedCellHeight(22);

        this.scrollPane = new JScrollPane(list);
        this.scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0x00, 0x7A, 0xCC), 1));
        this.scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        this.popupMenu.setLayout(new BorderLayout());
        this.popupMenu.add(scrollPane, BorderLayout.CENTER);

        // Key Listeners
        this.textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (popupMenu.isVisible()) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        int index = list.getSelectedIndex() + 1;
                        if (index < listModel.getSize()) {
                            list.setSelectedIndex(index);
                            list.ensureIndexIsVisible(index);
                        }
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                        int index = list.getSelectedIndex() - 1;
                        if (index >= 0) {
                            list.setSelectedIndex(index);
                            list.ensureIndexIsVisible(index);
                        }
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB) {
                        insertSelectedCompletion();
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        popupMenu.setVisible(false);
                        e.consume();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_DOWN &&
                    e.getKeyCode() != KeyEvent.VK_UP &&
                    e.getKeyCode() != KeyEvent.VK_ENTER &&
                    e.getKeyCode() != KeyEvent.VK_TAB &&
                    e.getKeyCode() != KeyEvent.VK_ESCAPE) {
                    showCompletions();
                }
            }
        });

        this.list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 || e.getClickCount() == 2) {
                    insertSelectedCompletion();
                }
            }
        });
    }

    private void showCompletions() {
        try {
            int caretPos = textPane.getCaretPosition();
            String text = textPane.getText();
            if (caretPos <= 0 || text.isEmpty()) {
                popupMenu.setVisible(false);
                return;
            }

            // Find current word prefix safely
            int start = caretPos - 1;
            if (start >= text.length()) {
                start = text.length() - 1;
            }
            while (start >= 0) {
                char ch = text.charAt(start);
                if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '#') {
                    start--;
                } else {
                    break;
                }
            }
            start++;

            if (start > caretPos || start < 0 || caretPos > text.length()) {
                popupMenu.setVisible(false);
                return;
            }

            String prefix = text.substring(start, caretPos);
            if (prefix.trim().length() < 1) {
                popupMenu.setVisible(false);
                return;
            }

            String upperPrefix = prefix.toUpperCase();
            List<String> matches = new ArrayList<>();

            // Match Mnemonics
            for (String m : MNEMONICS) {
                if (m.startsWith(upperPrefix)) {
                    matches.add(m);
                }
            }

            // Match Registers
            for (String r : REGISTERS) {
                if (r.startsWith(upperPrefix) && !matches.contains(r)) {
                    matches.add(r);
                }
            }

            // Match User Defined Labels from document
            Pattern labelPattern = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*):");
            Matcher matcher = labelPattern.matcher(text);
            while (matcher.find()) {
                String label = matcher.group(1).toUpperCase();
                if (label.startsWith(upperPrefix) && !matches.contains(label)) {
                    matches.add(label);
                }
            }

            if (matches.isEmpty()) {
                popupMenu.setVisible(false);
                return;
            }

            listModel.clear();
            for (String m : matches) {
                listModel.addElement(m);
            }
            list.setSelectedIndex(0);

            int height = Math.min(matches.size() * 22 + 4, 150);
            scrollPane.setPreferredSize(new Dimension(180, height));
            popupMenu.pack();

            try {
                Rectangle r = textPane.modelToView(start);
                if (r != null) {
                    popupMenu.show(textPane, r.x, r.y + r.height + 2);
                    textPane.requestFocusInWindow();
                }
            } catch (Exception ex) {
                // Ignore positioning exception
            }
        } catch (Throwable t) {
            System.err.println("Autocompleter error: " + t);
        }
    }


    private void insertSelectedCompletion() {
        String selected = list.getSelectedValue();
        if (selected == null) {
            popupMenu.setVisible(false);
            return;
        }

        int caretPos = textPane.getCaretPosition();
        String text = textPane.getText();
        int start = caretPos - 1;
        while (start >= 0) {
            char ch = text.charAt(start);
            if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '#') {
                start--;
            } else {
                break;
            }
        }
        start++;

        try {
            Document doc = textPane.getDocument();
            int len = caretPos - start;
            doc.remove(start, len);
            doc.insertString(start, selected + " ", null);
            textPane.setCaretPosition(start + selected.length() + 1);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }

        popupMenu.setVisible(false);
    }
}
