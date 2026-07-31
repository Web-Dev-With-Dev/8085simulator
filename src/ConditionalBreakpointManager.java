import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Conditional Breakpoint Manager for 8085 Microprocessor Simulator
 *
 * Edge-triggered breakpoint engine:
 *   - Pauses execution ONCE when a condition transitions to true.
 *   - Each rule carries a "Post-Hit Action":
 *       PAUSE          → stop, show Continue/Step panel (default)
 *       RUN_AFTER      → immediately resume "Run All at Once" from the breakpoint PC
 *       RUN_AND_NOTIFY → resume running AND flash a non-blocking toast notification
 */
public class ConditionalBreakpointManager extends JFrame {

    // ─────────────────────────────────────────────────────────────
    //  Post-hit actions
    // ─────────────────────────────────────────────────────────────
    public enum PostHitAction {
        PAUSE("⏸ Pause Execution"),
        RUN_AFTER("▶ Run All After Breakpoint"),
        RUN_AND_NOTIFY("▶ Run + Toast Notify");

        public final String label;
        PostHitAction(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    // ─────────────────────────────────────────────────────────────
    //  Singleton / shared state
    // ─────────────────────────────────────────────────────────────
    private static ConditionalBreakpointManager instance;
    private static final List<BreakpointRule> rules = new ArrayList<>();

    private final Matrix   matrix;
    private final Assembler assembler;

    // ─────────────────────────────────────────────────────────────
    //  UI components
    // ─────────────────────────────────────────────────────────────
    private JTable          table;
    private DefaultTableModel tableModel;
    private JLabel          statusBadge;

    // ═════════════════════════════════════════════════════════════
    //  BreakpointRule data class
    // ═════════════════════════════════════════════════════════════
    public static class BreakpointRule {
        public boolean       enabled        = true;
        public String        category;       // "Register" | "Memory Value" | "Memory Modified" | "Flag"
        public String        target;         // "A", "B", "2050H", "Z Flag", …
        public String        operator;       // "==", "!=", ">", "<", ">=", "<=", "MODIFIED"
        public int           compareValue;
        public String        hexValString;
        public PostHitAction postHitAction   = PostHitAction.PAUSE;

        // edge-trigger internal state
        public int     lastMemVal       = -1;
        public boolean wasTrueLastStep  = false;
        public int     hitCount         = 0;   // how many times this rule has fired

        public BreakpointRule(String category, String target, String operator,
                              int compareValue, String hexValString) {
            this.category     = category;
            this.target       = target;
            this.operator     = operator;
            this.compareValue = compareValue;
            this.hexValString = hexValString;
        }

        public String getSummary() {
            if ("Memory Modified".equalsIgnoreCase(category))
                return "Memory " + target + " MODIFIED";
            return target + " " + operator + " " + hexValString;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Constructor / singleton
    // ─────────────────────────────────────────────────────────────
    public ConditionalBreakpointManager(Matrix matrix, Assembler assembler) {
        this.matrix   = matrix;
        this.assembler = assembler;
        instance = this;

        setTitle("⏸️  8085 Conditional Breakpoints");
        setSize(860, 500);
        setLocationRelativeTo(assembler);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));

        initComponents();

        if (rules.isEmpty()) {
            rules.add(new BreakpointRule("Register",       "A",     "==",       255, "0xFF"));
            rules.add(new BreakpointRule("Memory Modified","2050H", "MODIFIED",   0, "N/A"));
        }
        refreshTable();
        applyDarkTheme();
    }

    public static ConditionalBreakpointManager getInstance(Matrix matrix, Assembler assembler) {
        if (instance == null || !instance.isDisplayable())
            instance = new ConditionalBreakpointManager(matrix, assembler);
        return instance;
    }

    // ─────────────────────────────────────────────────────────────
    //  State management (called by Assembler before fresh runs)
    // ─────────────────────────────────────────────────────────────

    /**
     * Resets edge-trigger state (preserves hit counters).
     * Pre-seeds wasTrueLastStep by evaluating each rule against the
     * CURRENT matrix state — so a condition that is ALREADY true right
     * now (e.g. A == 0xFF from a previous run) does NOT fire immediately
     * on the next instruction.  Memory-Modified rules always start fresh.
     */
    public static void resetState(Matrix matrix) {
        for (BreakpointRule r : rules) {
            r.lastMemVal = -1;
            if (matrix != null && !"Memory Modified".equalsIgnoreCase(r.category)) {
                r.wasTrueLastStep = evaluateRule(r, matrix);   // pre-arm
            } else {
                r.wasTrueLastStep = false;
            }
        }
    }

    /** Overload for callers that don't have a Matrix reference */
    public static void resetState() { resetState(null); }

    /**
     * Full reset including hit counters — call on complete Stop or brand-new run.
     * Also pre-seeds wasTrueLastStep so pre-existing conditions don't fire.
     */
    public static void fullReset(Matrix matrix) {
        for (BreakpointRule r : rules) {
            r.lastMemVal = -1;
            r.hitCount   = 0;
            if (matrix != null && !"Memory Modified".equalsIgnoreCase(r.category)) {
                r.wasTrueLastStep = evaluateRule(r, matrix);
            } else {
                r.wasTrueLastStep = false;
            }
        }
        if (instance != null) instance.refreshTable();
    }

    /** Overload for callers that don't have a Matrix reference */
    public static void fullReset() { fullReset(null); }

    // ─────────────────────────────────────────────────────────────
    //  Core check — called after every instruction by Assembler.run()
    //
    //  Returns a TriggeredResult describing which rule fired and
    //  what post-hit action should be taken.  Returns null if no
    //  breakpoint triggered.
    // ─────────────────────────────────────────────────────────────
    public static TriggeredResult checkBreakpoints(Matrix matrix) {
        if (matrix == null || rules.isEmpty()) return null;

        for (BreakpointRule rule : rules) {
            if (!rule.enabled) continue;

            boolean isNowTrue = evaluateRule(rule, matrix);

            // Edge trigger: fire only on FALSE → TRUE transition
            if (isNowTrue && !rule.wasTrueLastStep) {
                rule.wasTrueLastStep = true;
                rule.hitCount++;
                if (instance != null)
                    SwingUtilities.invokeLater(() -> instance.refreshTable());
                return new TriggeredResult(rule.getSummary(), rule.postHitAction, rule.hitCount);
            } else {
                rule.wasTrueLastStep = isNowTrue;
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    //  Result record returned by checkBreakpoints()
    // ─────────────────────────────────────────────────────────────
    public static class TriggeredResult {
        public final String        summary;
        public final PostHitAction postHitAction;
        public final int           hitCount;

        TriggeredResult(String summary, PostHitAction action, int hitCount) {
            this.summary       = summary;
            this.postHitAction = action;
            this.hitCount      = hitCount;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Rule evaluation
    // ─────────────────────────────────────────────────────────────
    private static boolean evaluateRule(BreakpointRule r, Matrix matrix) {
        if ("Register".equalsIgnoreCase(r.category)) {
            return compare(getRegisterValue(r.target, matrix), r.operator, r.compareValue);
        } else if ("Memory Value".equalsIgnoreCase(r.category)) {
            int addr   = parseHexOrDec(r.target);
            int memVal = (matrix.memory != null && addr < matrix.memory.length)
                         ? (matrix.memory[addr] & 0xFF) : 0;
            return compare(memVal, r.operator, r.compareValue);
        } else if ("Memory Modified".equalsIgnoreCase(r.category)) {
            int addr       = parseHexOrDec(r.target);
            int currentVal = (matrix.memory != null && addr < matrix.memory.length)
                             ? (matrix.memory[addr] & 0xFF) : 0;
            if (r.lastMemVal == -1) { r.lastMemVal = currentVal; return false; }
            if (currentVal != r.lastMemVal) { r.lastMemVal = currentVal; return true; }
            return false;
        } else if ("Flag".equalsIgnoreCase(r.category)) {
            int flags  = matrix.F & 0xFF;
            int bitVal = 0;
            if      (r.target.contains("S")  || r.target.contains("Sign"))    bitVal = (flags & 128) != 0 ? 1 : 0;
            else if (r.target.contains("Z")  || r.target.contains("Zero"))    bitVal = (flags &  64) != 0 ? 1 : 0;
            else if (r.target.contains("AC"))                                  bitVal = (flags &  16) != 0 ? 1 : 0;
            else if (r.target.contains("P")  || r.target.contains("Parity"))  bitVal = (flags &   4) != 0 ? 1 : 0;
            else if (r.target.contains("CY") || r.target.contains("Carry"))   bitVal = (flags &   1) != 0 ? 1 : 0;
            return compare(bitVal, r.operator, r.compareValue);
        }
        return false;
    }

    private static int getRegisterValue(String name, Matrix matrix) {
        String c = name.trim().toUpperCase();
        if (c.equals("A") || c.contains("ACC")) return matrix.A & 0xFF;
        if (c.equals("B"))  return matrix.B  & 0xFF;
        if (c.equals("C"))  return matrix.C  & 0xFF;
        if (c.equals("D"))  return matrix.D  & 0xFF;
        if (c.equals("E"))  return matrix.E  & 0xFF;
        if (c.equals("H"))  return matrix.H  & 0xFF;
        if (c.equals("L"))  return matrix.L  & 0xFF;
        if (c.equals("M"))  return (matrix.memory != null)
                                   ? (matrix.memory[((matrix.H & 0xFF) << 8) | (matrix.L & 0xFF)] & 0xFF) : 0;
        if (c.equals("BC")) return ((matrix.B & 0xFF) << 8) | (matrix.C & 0xFF);
        if (c.equals("DE")) return ((matrix.D & 0xFF) << 8) | (matrix.E & 0xFF);
        if (c.equals("HL")) return ((matrix.H & 0xFF) << 8) | (matrix.L & 0xFF);
        if (c.equals("SP")) return matrix.SP & 0xFFFF;
        if (c.equals("PC")) return matrix.PC & 0xFFFF;
        return 0;
    }

    private static boolean compare(int a, String op, int b) {
        switch (op) {
            case "==": return a == b;
            case "!=": return a != b;
            case ">":  return a >  b;
            case "<":  return a <  b;
            case ">=": return a >= b;
            case "<=": return a <= b;
            default:   return false;
        }
    }

    public static int parseHexOrDec(String s) {
        if (s == null) return 0;
        String str = s.trim().toUpperCase();
        if (str.isEmpty() || str.equals("N/A")) return 0;
        try {
            if (str.endsWith("H"))   return Integer.parseInt(str.substring(0, str.length() - 1), 16);
            if (str.startsWith("0X")) return Integer.parseInt(str.substring(2), 16);
            if (str.matches(".*[A-F].*")) return Integer.parseInt(str, 16);
            try { return Integer.parseInt(str); }
            catch (NumberFormatException e) { return Integer.parseInt(str, 16); }
        } catch (Exception e) { return 0; }
    }

    // ─────────────────────────────────────────────────────────────
    //  UI construction
    // ─────────────────────────────────────────────────────────────
    private void initComponents() {

        // ── Top toolbar ───────────────────────────────────────────
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        topBar.setBackground(new Color(0x1C, 0x1C, 0x2A));
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x3A, 0x3A, 0x5A)));

        JButton btnAdd    = toolbarButton("➕ Add Rule",      new Color(0x2E, 0x7D, 0x32));
        JButton btnRemove = toolbarButton("❌ Remove",        new Color(0x7B, 0x1F, 0x1F));
        JButton btnClear  = toolbarButton("🧹 Clear All",    new Color(0x4A, 0x4A, 0x6A));
        JButton btnRunAll = toolbarButton("▶ Run All After", new Color(0x0D, 0x5C, 0x87));

        btnAdd.addActionListener(e -> showAddRuleDialog());
        btnRemove.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel >= 0 && sel < rules.size()) { rules.remove(sel); refreshTable(); }
        });
        btnClear.addActionListener(e -> { rules.clear(); refreshTable(); });
        btnRunAll.addActionListener(e -> setAllRulesRunAfter());

        topBar.add(btnAdd);
        topBar.add(btnRemove);
        topBar.add(btnClear);
        topBar.add(Box.createHorizontalStrut(16));
        topBar.add(btnRunAll);

        add(topBar, BorderLayout.NORTH);

        // ── Table ─────────────────────────────────────────────────
        String[] cols = {"✓", "Category", "Target", "Op", "Value", "Post-Hit Action", "Hits", "Rule Summary"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public Class<?> getColumnClass(int col) {
                return col == 0 ? Boolean.class : String.class;
            }
            @Override public boolean isCellEditable(int row, int col) {
                return col == 0; // only checkbox editable directly
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setBackground(new Color(0x1A, 0x1A, 0x28));
        table.setForeground(new Color(0xE0, 0xE0, 0xF0));
        table.setGridColor(new Color(0x2E, 0x2E, 0x4E));
        table.setSelectionBackground(new Color(0x2A, 0x5C, 0x8A));
        table.setSelectionForeground(Color.WHITE);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);

        // Colour-code "Post-Hit Action" column
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                setHorizontalAlignment(CENTER);
                String v = val == null ? "" : val.toString();
                if (v.contains("Pause"))          setForeground(new Color(0xFF, 0xC1, 0x07));
                else if (v.contains("Toast"))     setForeground(new Color(0x4C, 0xC9, 0xF0));
                else                               setForeground(new Color(0x5C, 0xE6, 0x5C));
                if (!sel) setBackground(new Color(0x1A, 0x1A, 0x28));
                return this;
            }
        });

        // Hits column centered
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            { setHorizontalAlignment(CENTER); }
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                int hits = 0;
                try { hits = Integer.parseInt(val == null ? "0" : val.toString()); } catch (Exception ignored) {}
                setForeground(hits > 0 ? new Color(0xFF, 0x70, 0x70) : new Color(0x88, 0x88, 0xAA));
                if (!sel) setBackground(new Color(0x1A, 0x1A, 0x28));
                return this;
            }
        });

        if (table.getTableHeader() != null) {
            table.getTableHeader().setBackground(new Color(0x14, 0x14, 0x22));
            table.getTableHeader().setForeground(new Color(0xCC, 0xCC, 0xFF));
            table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
            table.getTableHeader().setReorderingAllowed(false);
        }

        // Checkbox toggle updates enabled flag
        tableModel.addTableModelListener(e -> {
            int row = e.getFirstRow(), col = e.getColumn();
            if (col == 0 && row >= 0 && row < rules.size())
                rules.get(row).enabled = (Boolean) tableModel.getValueAt(row, 0);
        });

        // Double-click to cycle Post-Hit Action
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    if (row >= 0 && row < rules.size() && col == 5) {
                        BreakpointRule r = rules.get(row);
                        PostHitAction[] vals = PostHitAction.values();
                        r.postHitAction = vals[(r.postHitAction.ordinal() + 1) % vals.length];
                        refreshTable();
                    }
                }
            }
        });

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(55);
        table.getColumnModel().getColumn(4).setPreferredWidth(65);
        table.getColumnModel().getColumn(5).setPreferredWidth(180);
        table.getColumnModel().getColumn(6).setPreferredWidth(40);
        table.getColumnModel().getColumn(7).setPreferredWidth(210);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(new Color(0x1A, 0x1A, 0x28));
        add(sp, BorderLayout.CENTER);

        // ── Status bar ────────────────────────────────────────────
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(0x12, 0x12, 0x1E));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x3A, 0x3A, 0x5A)));

        JLabel hint = new JLabel("  💡 Double-click Post-Hit Action cell to cycle: Pause → Run After → Run+Notify.  |  ▶ Run All After sets ALL rules to 'Run After'.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(new Color(0x88, 0x88, 0xAA));
        hint.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        statusBadge = new JLabel("● Idle");
        statusBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusBadge.setForeground(new Color(0x5C, 0xE6, 0x5C));
        statusBadge.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

        statusBar.add(hint,        BorderLayout.CENTER);
        statusBar.add(statusBadge, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);
    }

    /** Set ALL rules to RUN_AFTER so execution never stops at breakpoints */
    private void setAllRulesRunAfter() {
        for (BreakpointRule r : rules) r.postHitAction = PostHitAction.RUN_AFTER;
        refreshTable();
        setStatus("▶ All rules set to 'Run All After Breakpoint'", new Color(0x5C, 0xE6, 0x5C));
    }

    public void setStatus(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            if (statusBadge != null) {
                statusBadge.setText("● " + msg);
                statusBadge.setForeground(color);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  Table refresh
    // ─────────────────────────────────────────────────────────────
    public void refreshTable() {
        tableModel.setRowCount(0);
        for (BreakpointRule r : rules) {
            tableModel.addRow(new Object[]{
                r.enabled,
                r.category,
                r.target,
                r.operator,
                r.hexValString,
                r.postHitAction.label,
                String.valueOf(r.hitCount),
                r.getSummary()
            });
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Add-rule dialog
    // ─────────────────────────────────────────────────────────────
    private void showAddRuleDialog() {
        JDialog dlg = new JDialog(this, "Add Conditional Breakpoint Rule", true);
        dlg.setSize(450, 380);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(new Color(0x1C, 0x1C, 0x2A));
        dlg.setLayout(new GridBagLayout());

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7, 10, 7, 10);
        g.fill   = GridBagConstraints.HORIZONTAL;

        JComboBox<String> comboCat    = darkCombo(new String[]{"Register", "Memory Modified", "Memory Value", "Flag"});
        JComboBox<String> comboTarget = darkCombo(new String[]{"A", "B", "C", "D", "E", "H", "L", "M", "BC", "DE", "HL", "SP", "PC"});
        JComboBox<String> comboOp     = darkCombo(new String[]{"==", "!=", ">", "<", ">=", "<="});
        JTextField        txtVal      = darkTextField("0xFF");
        JComboBox<PostHitAction> comboAction = new JComboBox<>(PostHitAction.values());
        styleCombo(comboAction);

        comboCat.addActionListener(e -> {
            String cat = (String) comboCat.getSelectedItem();
            comboTarget.removeAllItems();
            if ("Register".equals(cat)) {
                for (String s : new String[]{"A","B","C","D","E","H","L","M","BC","DE","HL","SP","PC"}) comboTarget.addItem(s);
                comboOp.setEnabled(true); txtVal.setEnabled(true);
            } else if ("Memory Modified".equals(cat)) {
                comboTarget.addItem("2050H"); comboTarget.setEditable(true);
                comboOp.setEnabled(false);   txtVal.setEnabled(false);
            } else if ("Memory Value".equals(cat)) {
                comboTarget.addItem("2050H"); comboTarget.setEditable(true);
                comboOp.setEnabled(true);    txtVal.setEnabled(true);
            } else if ("Flag".equals(cat)) {
                for (String s : new String[]{"Z (Zero)","CY (Carry)","S (Sign)","P (Parity)","AC (Aux Carry)"}) comboTarget.addItem(s);
                comboOp.setEnabled(true); txtVal.setText("1"); txtVal.setEnabled(true);
            }
        });

        addRow(dlg, g, 0, "Category:",              comboCat);
        addRow(dlg, g, 1, "Target / Address:",      comboTarget);
        addRow(dlg, g, 2, "Operator:",              comboOp);
        addRow(dlg, g, 3, "Compare Value (Hex/Dec):", txtVal);
        addRow(dlg, g, 4, "Post-Hit Action:",       comboAction);

        JButton btnSave = toolbarButton("💾 Save Breakpoint Rule", new Color(0x2E, 0x7D, 0x32));
        btnSave.addActionListener(e -> {
            String cat     = (String) comboCat.getSelectedItem();
            String target  = comboTarget.getSelectedItem() != null ? comboTarget.getSelectedItem().toString() : "A";
            String op      = "Memory Modified".equals(cat) ? "MODIFIED" : (String) comboOp.getSelectedItem();
            String valStr  = txtVal.getText().trim();
            int    parsed  = parseHexOrDec(valStr);

            BreakpointRule newRule = new BreakpointRule(cat, target, op, parsed, valStr);
            newRule.postHitAction  = (PostHitAction) comboAction.getSelectedItem();
            rules.add(newRule);
            refreshTable();
            dlg.dispose();
        });

        g.gridx = 0; g.gridy = 5; g.gridwidth = 2; g.insets = new Insets(16, 10, 10, 10);
        dlg.add(btnSave, g);

        applyDarkDialog(dlg);
        dlg.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────
    //  Dialog helpers
    // ─────────────────────────────────────────────────────────────
    private void addRow(JDialog dlg, GridBagConstraints g, int row, String label, JComponent comp) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 1;
        JLabel lbl = new JLabel(label);
        lbl.setForeground(new Color(0xCC, 0xCC, 0xFF));
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dlg.add(lbl, g);
        g.gridx = 1; dlg.add(comp, g);
    }

    private void applyDarkDialog(JDialog dlg) {
        for (Component c : dlg.getContentPane().getComponents()) {
            if (c instanceof JComboBox || c instanceof JTextField || c instanceof JButton) {
                c.setBackground(new Color(0x25, 0x25, 0x40));
                c.setForeground(new Color(0xE0, 0xE0, 0xFF));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Widget factories
    // ─────────────────────────────────────────────────────────────
    private JButton toolbarButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.darker(), 1, true),
            BorderFactory.createEmptyBorder(5, 14, 5, 14)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }

    private JComboBox<String> darkCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        styleCombo(cb);
        return cb;
    }

    private void styleCombo(JComboBox<?> cb) {
        cb.setBackground(new Color(0x25, 0x25, 0x40));
        cb.setForeground(new Color(0xE0, 0xE0, 0xFF));
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    }

    private JTextField darkTextField(String text) {
        JTextField tf = new JTextField(text);
        tf.setBackground(new Color(0x25, 0x25, 0x40));
        tf.setForeground(new Color(0xE0, 0xE0, 0xFF));
        tf.setCaretColor(Color.WHITE);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x4A, 0x4A, 0x70)),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        return tf;
    }

    private void applyDarkTheme() {
        getContentPane().setBackground(new Color(0x12, 0x12, 0x1E));
    }
}
