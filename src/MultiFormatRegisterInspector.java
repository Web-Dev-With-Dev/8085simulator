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

        setTitle("📊 8085 Multi-Format Register Inspector");
        setSize(850, 520);
        setLocationRelativeTo(assembler);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

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

    private void initComponents() {
        // --- TOP TOOLBAR ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        topPanel.setBackground(new Color(0x25, 0x25, 0x26));
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x3E, 0x3E, 0x42)));

        JLabel titleLabel = new JLabel("📊 Real-Time Multi-Format Register & Memory Inspector");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(new Color(0x00, 0xA8, 0xFF));
        topPanel.add(titleLabel);

        JButton btnRefresh = new JButton("🔄 Sync Now");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.addActionListener(e -> updateValues());
        topPanel.add(btnRefresh);

        add(topPanel, BorderLayout.NORTH);

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
        table.setBackground(new Color(0x21, 0x22, 0x24));
        table.setForeground(new Color(0xE0, 0xE0, 0xE0));
        table.setGridColor(new Color(0x3E, 0x40, 0x42));
        table.setSelectionBackground(new Color(0x00, 0x7A, 0xCC));
        table.setSelectionForeground(Color.WHITE);

        if (table.getTableHeader() != null) {
            table.getTableHeader().setBackground(new Color(0x2D, 0x2D, 0x2D));
            table.getTableHeader().setForeground(new Color(0xE0, 0xE0, 0xE0));
            table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
            table.getTableHeader().setPreferredSize(new Dimension(table.getTableHeader().getPreferredSize().width, 28));
        }

        // Custom Cell Renderers
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(SwingConstants.LEFT);

        DefaultTableCellRenderer centerMonoRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                setFont(new Font("Monospaced", Font.BOLD, 13));
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!sel) {
                    if (c == 1) setForeground(new Color(0x00, 0xE6, 0x76)); // Green for Hex
                    else if (c == 2 || c == 3) setForeground(new Color(0x29, 0xB6, 0xF6)); // Blue for Dec
                    else if (c == 4) setForeground(new Color(0xFF, 0xB7, 0x4D)); // Orange for Binary
                    else if (c == 5) setForeground(new Color(0xEA, 0x80, 0xFC)); // Pink/Purple for ASCII
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
        add(sp, BorderLayout.CENTER);

        // --- BOTTOM FLAG BREAKDOWN PANEL ---
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBackground(new Color(0x1E, 0x1E, 0x1E));
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x3E, 0x3E, 0x42)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        lblFlagsBreakdown = new JLabel("FLAGS: S=0 | Z=0 | AC=0 | P=0 | CY=0");
        lblFlagsBreakdown.setFont(new Font("Monospaced", Font.BOLD, 14));
        lblFlagsBreakdown.setForeground(new Color(0x00, 0xE6, 0x76));

        JLabel infoNote = new JLabel("💡 Binary is formatted in 4-bit groups. ASCII shows printable characters or '.' for non-printables.");
        infoNote.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        infoNote.setForeground(new Color(0xAA, 0xAA, 0xAA));

        bottomPanel.add(lblFlagsBreakdown, BorderLayout.CENTER);
        bottomPanel.add(infoNote, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void updateValues() {
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
                "🚩 8085 FLAGS:  [S: %d]  [Z: %d]  [AC: %d]  [P: %d]  [CY: %d]  (PSW Hex: 0x%02X)",
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
