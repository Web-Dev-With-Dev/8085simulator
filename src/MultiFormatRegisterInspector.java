import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;

/**
 * Multi-Format Register Inspector for 8085 Microprocessor Simulator
 * Displays all 8085 registers simultaneously in:
 * 1. Hexadecimal (0x3A)
 * 2. Decimal (58 / Signed)
 * 3. Binary (0011 1010)
 * 4. ASCII Character ('A', ':', etc.)
 */
public class MultiFormatRegisterInspector extends JFrame {

    private static MultiFormatRegisterInspector instance;
    private final Matrix matrix;
    private final Assembler assembler;

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblFlagsBreakdown;
    private Timer refreshTimer;

    private JPanel topPanel;
    private JPanel bottomPanel;
    private JLabel titleLabel;
    private JLabel infoNote;

    // Register Names
    private static final String[] REG_NAMES = {
        "Accumulator (A)",
        "Register B",
        "Register C",
        "Register D",
        "Register E",
        "Register H",
        "Register L",
        "Memory (M)",
        "BC Pair (16-bit)",
        "DE Pair (16-bit)",
        "HL Pair (16-bit)",
        "Stack Pointer (SP)",
        "Program Counter (PC)",
        "Flag Register (PSW)"
    };

    public MultiFormatRegisterInspector(Matrix matrix, Assembler assembler) {
        this.matrix = matrix;
        this.assembler = assembler;
        instance = this;

        setTitle(" 8085 Multi-Format Register Inspector");
        setSize(850, 520);
        setLocationRelativeTo(assembler);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
        updateValues();

        // Refresh timer for live sync with simulator
        refreshTimer = new Timer(100, e -> {
            if (isShowing()) {
                updateValues();
            }
        });
        refreshTimer.start();
    }

    public static MultiFormatRegisterInspector getInstance(Matrix matrix, Assembler assembler) {
        if (instance == null || !instance.isDisplayable()) {
            instance = new MultiFormatRegisterInspector(matrix, assembler);
        }
        return instance;
    }

    public static void refreshInspector() {
        if (instance != null && instance.isShowing()) {
            instance.updateValues();
        }
    }

    public void updateThemeColors() {
        if (topPanel == null || table == null || bottomPanel == null) return;
        boolean isDark = com.formdev.flatlaf.FlatLaf.isLafDark();

        Color bgTop    = isDark ? new Color(0x25, 0x25, 0x28) : new Color(0xF1, 0xF5, 0xF9);
        Color borderC  = isDark ? new Color(0x3E, 0x40, 0x45) : new Color(0xCB, 0xD5, 0xE1);
        Color titleFg  = isDark ? new Color(0x00, 0xA8, 0xFF) : new Color(0x02, 0x84, 0xC7);

        Color tableBg  = isDark ? new Color(0x1D, 0x1E, 0x23) : new Color(0xFF, 0xFF, 0xFF);
        Color tableFg  = isDark ? new Color(0xF0, 0xF0, 0xF0) : new Color(0x0F, 0x17, 0x2A);
        Color gridColor= isDark ? new Color(0x36, 0x38, 0x42) : new Color(0xCB, 0xD5, 0xE1);
        Color selBg    = isDark ? new Color(0x3E, 0x44, 0x52) : new Color(0x25, 0x63, 0xEB);

        Color headerBg = isDark ? new Color(0x28, 0x2A, 0x36) : new Color(0xE2, 0xE8, 0xF0);
        Color headerFg = isDark ? new Color(0xFF, 0xB7, 0x4D) : new Color(0xC2, 0x41, 0x0C);

        topPanel.setBackground(bgTop);
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderC));
        titleLabel.setForeground(titleFg);

        table.setBackground(tableBg);
        table.setForeground(tableFg);
        table.setGridColor(gridColor);
        table.setSelectionBackground(selBg);
        table.setSelectionForeground(Color.WHITE);

        if (table.getTableHeader() != null) {
            javax.swing.table.JTableHeader header = table.getTableHeader();
            header.setFont(new Font("Segoe UI", Font.BOLD, 12));
            header.setBackground(headerBg);
            header.setForeground(headerFg);
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, 28));

            header.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                    JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                    l.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    l.setBackground(headerBg);
                    l.setForeground(headerFg);
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                    l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, gridColor),
                        BorderFactory.createEmptyBorder(3, 4, 3, 4)
                    ));
                    return l;
                }
            });
        }

        bottomPanel.setBackground(bgTop);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, borderC),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        lblFlagsBreakdown.setForeground(isDark ? new Color(0x00, 0xE6, 0x76) : new Color(0x15, 0x80, 0x3D));
        if (infoNote != null) {
            infoNote.setForeground(isDark ? new Color(0xAA, 0xAA, 0xAA) : new Color(0x47, 0x55, 0x69));
        }
        table.repaint();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            public void updateUI() {
                super.updateUI();
                updateThemeColors();
            }
        };
        setContentPane(mainPanel);

        // --- TOP TOOLBAR ---
        topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        titleLabel = new JLabel(" Real-Time Multi-Format Register & Memory Inspector");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        topPanel.add(titleLabel);

        JButton btnRefresh = new JButton(" Sync Now");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.addActionListener(e -> updateValues());
        topPanel.add(btnRefresh);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // --- TABLE ---
        String[] colNames = {"Register", "Hexadecimal", "Decimal (Unsigned)", "Decimal (Signed)", "Binary (4-Bit Groups)", "ASCII Char"};
        tableModel = new DefaultTableModel(colNames, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false; // View only in inspector table
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setShowGrid(true);

        // Custom Cell Renderers
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel comp = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                comp.setHorizontalAlignment(SwingConstants.LEFT);
                comp.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
                boolean isDark = com.formdev.flatlaf.FlatLaf.isLafDark();
                Color bgEven = isDark ? new Color(0x1D, 0x1E, 0x23) : new Color(0xFF, 0xFF, 0xFF);
                Color bgOdd  = isDark ? new Color(0x24, 0x26, 0x2E) : new Color(0xF8, 0xFA, 0xFC);
                Color textFg = isDark ? new Color(0xF0, 0xF0, 0xF0) : new Color(0x0F, 0x17, 0x2A);
                Color selBg  = isDark ? new Color(0x3E, 0x44, 0x52) : new Color(0x25, 0x63, 0xEB);

                if (sel) {
                    comp.setBackground(selBg);
                    comp.setForeground(Color.WHITE);
                } else {
                    comp.setBackground(r % 2 == 0 ? bgEven : bgOdd);
                    comp.setForeground(textFg);
                }
                return comp;
            }
        };

        DefaultTableCellRenderer centerMonoRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel comp = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                setFont(new Font("Monospaced", Font.BOLD, 13));
                setHorizontalAlignment(SwingConstants.CENTER);
                boolean isDark = com.formdev.flatlaf.FlatLaf.isLafDark();
                Color bgEven = isDark ? new Color(0x1D, 0x1E, 0x23) : new Color(0xFF, 0xFF, 0xFF);
                Color bgOdd  = isDark ? new Color(0x24, 0x26, 0x2E) : new Color(0xF8, 0xFA, 0xFC);
                Color selBg  = isDark ? new Color(0x3E, 0x44, 0x52) : new Color(0x25, 0x63, 0xEB);

                if (sel) {
                    comp.setBackground(selBg);
                    comp.setForeground(Color.WHITE);
                } else {
                    comp.setBackground(r % 2 == 0 ? bgEven : bgOdd);
                    if (c == 1) setForeground(isDark ? new Color(0x00, 0xE6, 0x76) : new Color(0x15, 0x80, 0x3D)); // Green for Hex
                    else if (c == 2 || c == 3) setForeground(isDark ? new Color(0x29, 0xB6, 0xF6) : new Color(0x02, 0x84, 0xC7)); // Blue for Dec
                    else if (c == 4) setForeground(isDark ? new Color(0xFF, 0xB7, 0x4D) : new Color(0xC2, 0x41, 0x0C)); // Amber/Orange for Binary
                    else if (c == 5) setForeground(isDark ? new Color(0xEA, 0x80, 0xFC) : new Color(0x7E, 0x22, 0xCE)); // Purple for ASCII
                }
                return comp;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            if (i == 0) {
                table.getColumnModel().getColumn(i).setCellRenderer(leftRenderer);
                table.getColumnModel().getColumn(i).setPreferredWidth(170);
            } else {
                table.getColumnModel().getColumn(i).setCellRenderer(centerMonoRenderer);
                if (i == 4) table.getColumnModel().getColumn(i).setPreferredWidth(180);
                else table.getColumnModel().getColumn(i).setPreferredWidth(120);
            }
        }

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        mainPanel.add(sp, BorderLayout.CENTER);

        // --- BOTTOM FLAG BREAKDOWN PANEL ---
        bottomPanel = new JPanel(new BorderLayout(5, 5));

        lblFlagsBreakdown = new JLabel("FLAGS: S=0 | Z=0 | AC=0 | P=0 | CY=0");
        lblFlagsBreakdown.setFont(new Font("Monospaced", Font.BOLD, 14));

        infoNote = new JLabel(" Binary is formatted in 4-bit groups. ASCII shows printable characters or '.' for non-printables.");
        infoNote.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        bottomPanel.add(lblFlagsBreakdown, BorderLayout.CENTER);
        bottomPanel.add(infoNote, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        updateThemeColors();
    }

    public void updateValues() {
        Matrix matrix = (assembler != null && assembler.matrix != null) ? assembler.matrix : this.matrix;
        if (matrix == null) return;

        int a = matrix.A & 0xFF;
        int b = matrix.B & 0xFF;
        int c = matrix.C & 0xFF;
        int d = matrix.D & 0xFF;
        int e = matrix.E & 0xFF;
        int h = matrix.H & 0xFF;
        int l = matrix.L & 0xFF;

        int memAddr = ((h << 8) | l) & 0xFFFF;
        int m = (matrix.memory != null && memAddr < matrix.memory.length) ? (matrix.memory[memAddr] & 0xFF) : 0;

        int bc = ((b << 8) | c) & 0xFFFF;
        int de = ((d << 8) | e) & 0xFFFF;
        int hl = ((h << 8) | l) & 0xFFFF;

        int sp = matrix.SP & 0xFFFF;
        int pc = matrix.PC & 0xFFFF;
        int flags = matrix.F & 0xFF;

        tableModel.setRowCount(0);

        // 8-bit Registers
        addRow8Bit("Accumulator (A)", a);
        addRow8Bit("Register B", b);
        addRow8Bit("Register C", c);
        addRow8Bit("Register D", d);
        addRow8Bit("Register E", e);
        addRow8Bit("Register H", h);
        addRow8Bit("Register L", l);
        addRow8Bit(String.format("Memory M [%04XH]", memAddr), m);

        // 16-bit Registers
        addRow16Bit("BC Pair", bc);
        addRow16Bit("DE Pair", de);
        addRow16Bit("HL Pair", hl);
        addRow16Bit("Stack Pointer (SP)", sp);
        addRow16Bit("Program Counter (PC)", pc);

        // Flag Register
        addRow8Bit("Flag Register (PSW)", flags);

        // Flag breakdown label
        int sign = (flags & 128) != 0 ? 1 : 0;
        int zero = (flags & 64) != 0 ? 1 : 0;
        int auxCarry = (flags & 16) != 0 ? 1 : 0;
        int parity = (flags & 4) != 0 ? 1 : 0;
        int carry = (flags & 1) != 0 ? 1 : 0;

        lblFlagsBreakdown.setText(String.format(
                " 8085 FLAGS:  [S: %d]  [Z: %d]  [AC: %d]  [P: %d]  [CY: %d]  (PSW Hex: 0x%02X)",
                sign, zero, auxCarry, parity, carry, flags
        ));
    }

    private void addRow8Bit(String name, int val) {
        String hex = String.format("0x%02X", val);
        String decUnsigned = String.valueOf(val);
        
        byte signedVal = (byte) val;
        String decSigned = String.format("%+d", signedVal);

        String binRaw = String.format("%8s", Integer.toBinaryString(val)).replace(' ', '0');
        String binFormatted = binRaw.substring(0, 4) + " " + binRaw.substring(4);

        String asciiChar;
        if (val >= 32 && val <= 126) {
            asciiChar = "'" + (char) val + "'";
        } else {
            asciiChar = "'.' (0x" + String.format("%02X", val) + ")";
        }

        tableModel.addRow(new Object[]{name, hex, decUnsigned, decSigned, binFormatted, asciiChar});
    }

    private void addRow16Bit(String name, int val) {
        String hex = String.format("0x%04X", val);
        String decUnsigned = String.valueOf(val);
        
        short signedVal = (short) val;
        String decSigned = String.format("%+d", signedVal);

        String binRaw = String.format("%16s", Integer.toBinaryString(val)).replace(' ', '0');
        String binFormatted = binRaw.substring(0, 4) + " " + binRaw.substring(4, 8) + " " +
                               binRaw.substring(8, 12) + " " + binRaw.substring(12);

        String asciiChar = "N/A (16-Bit)";

        tableModel.addRow(new Object[]{name, hex, decUnsigned, decSigned, binFormatted, asciiChar});
    }
}
