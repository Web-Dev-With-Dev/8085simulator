import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 8085 Call Stack & Subroutine Frame Visualizer
 * Provides visual stack inspection, subroutine call tracking, stack depth analysis,
 * and high-visibility stack memory tower graphics.
 */
public class CallStackVisualizer extends JFrame {

    private static CallStackVisualizer instance;
    private final Matrix matrix;
    private final Assembler assembler;

    private int initialSP = 0;
    private int maxSeenSP = 0;

    // UI Components
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JLabel lblSPVal;
    private JLabel lblDepthVal;
    private JLabel lblPCVal;
    private JLabel lblBaseSPVal;

    private JTable frameTable;
    private DefaultTableModel tableModel;
    private JTree callTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootTreeNode;
    private StackTowerPanel stackTowerPanel;

    private Timer refreshTimer;

    public static class StackFrameItem {
        public int address;     // Stack Address (SP + offset)
        public int val16;       // 16-bit combined value
        public int highByte;    // High byte (SP+1)
        public int lowByte;     // Low byte (SP+0)
        public String type;     // CALL Return PC, PUSH BC, PUSH DE, PUSH HL, PUSH PSW, Stack Word
        public String details;  // Detailed explanation

        public StackFrameItem(int address, int highByte, int lowByte, String type, String details) {
            this.address = address;
            this.highByte = highByte & 0xFF;
            this.lowByte = lowByte & 0xFF;
            this.val16 = ((highByte & 0xFF) << 8) | (lowByte & 0xFF);
            this.type = type;
            this.details = details;
        }
    }

    public CallStackVisualizer(Matrix matrix, Assembler assembler) {
        this.matrix = matrix;
        this.assembler = assembler;
        instance = this;

        Matrix m = (assembler != null && assembler.matrix != null) ? assembler.matrix : matrix;
        if (m != null && m.SP > 0) {
            this.initialSP = m.SP & 0xFFFF;
            this.maxSeenSP = this.initialSP;
        }

        setTitle(" 8085 Call Stack & Subroutine Frame Visualizer");
        setSize(980, 580);
        setLocationRelativeTo(assembler);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
        updateStackView();

        // Sync timer for live updating during simulation run or step (150ms interval)
        refreshTimer = new Timer(150, e -> {
            if (isShowing()) {
                updateStackView();
            }
        });
        refreshTimer.start();
    }

    public static CallStackVisualizer getInstance(Matrix matrix, Assembler assembler) {
        if (instance == null || !instance.isDisplayable()) {
            instance = new CallStackVisualizer(matrix, assembler);
        }
        return instance;
    }

    public static void refreshVisualizer() {
        if (instance != null && instance.isShowing()) {
            instance.updateStackView();
        }
    }

    private void initComponents() {
        mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. TOP HEADER SUMMARY PANEL
        headerPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(" Stack Pointer & CPU Execution Status "),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        lblSPVal = createBadgeLabel("SP: 0x0000", new Color(0x02, 0x84, 0xC7));
        lblDepthVal = createBadgeLabel("Depth: 0 Items", new Color(0x05, 0x96, 0x69));
        lblPCVal = createBadgeLabel("PC: 0x0000", new Color(0x7E, 0x22, 0xCE));
        lblBaseSPVal = createBadgeLabel("Base SP: N/A", new Color(0xD9, 0x77, 0x06));

        headerPanel.add(lblSPVal);
        headerPanel.add(lblDepthVal);
        headerPanel.add(lblPCVal);
        headerPanel.add(lblBaseSPVal);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // 2. CENTER SPLIT PANEL (Left: Tower / Call Tree, Right: Stack Table)
        JTabbedPane leftTabbedPane = new JTabbedPane();

        // Tab 1: Graphical Stack Tower
        stackTowerPanel = new StackTowerPanel();
        JScrollPane towerScrollPane = new JScrollPane(stackTowerPanel);
        towerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        leftTabbedPane.addTab("Stack Tower Graphic", towerScrollPane);

        // Tab 2: Subroutine Call Nesting Tree
        rootTreeNode = new DefaultMutableTreeNode("Main Program (0x0000)");
        treeModel = new DefaultTreeModel(rootTreeNode);
        callTree = new JTree(treeModel);
        callTree.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane treeScrollPane = new JScrollPane(callTree);
        leftTabbedPane.addTab("Subroutine Call Tree", treeScrollPane);

        leftTabbedPane.setPreferredSize(new Dimension(340, 400));

        // Right Panel: Table View
        String[] columnNames = {"Stack Addr", "Frame Type", "16-Bit Val", "High Byte (SP+1)", "Low Byte (SP+0)", "Frame Description"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        frameTable = new JTable(tableModel);
        frameTable.setRowHeight(26);
        frameTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        frameTable.setShowGrid(true);
        JScrollPane tableScrollPane = new JScrollPane(frameTable);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftTabbedPane, tableScrollPane);
        splitPane.setDividerLocation(340);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        // 3. BOTTOM CONTROL BAR
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JCheckBox chkLiveSync = new JCheckBox("Live Sync", true);
        chkLiveSync.addActionListener(e -> {
            if (chkLiveSync.isSelected()) refreshTimer.start();
            else refreshTimer.stop();
        });

        JButton btnResetBaseSP = new JButton("Reset Base SP");
        btnResetBaseSP.addActionListener(e -> {
            Matrix activeMatrix = (assembler != null && assembler.matrix != null) ? assembler.matrix : matrix;
            if (activeMatrix != null && activeMatrix.SP > 0) {
                initialSP = activeMatrix.SP & 0xFFFF;
                maxSeenSP = initialSP;
                updateStackView();
            }
        });

        JButton btnRefresh = new JButton("Refresh Now");
        btnRefresh.addActionListener(e -> updateStackView());

        bottomPanel.add(chkLiveSync);
        bottomPanel.add(btnResetBaseSP);
        bottomPanel.add(btnRefresh);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        applyTableStyling();
    }

    private JLabel createBadgeLabel(String text, Color accentColor) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Consolas", Font.BOLD, 13));
        label.setOpaque(true);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 1, true),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        return label;
    }

    public void updateStackView() {
        Matrix activeMatrix = (assembler != null && assembler.matrix != null) ? assembler.matrix : this.matrix;
        if (activeMatrix == null) return;

        boolean isDark = com.formdev.flatlaf.FlatLaf.isLafDark();
        int currentSP = activeMatrix.SP & 0xFFFF;
        int currentPC = activeMatrix.PC & 0xFFFF;

        // If SP is 0 or uninitialized, stack is empty
        if (currentSP == 0) {
            lblSPVal.setText("SP: 0x0000 (Uninitialized)");
            lblDepthVal.setText("Depth: 0 Items");
            lblPCVal.setText(String.format("PC: 0x%04X", currentPC));
            lblBaseSPVal.setText("Base SP: N/A");

            tableModel.setRowCount(0);
            rootTreeNode.removeAllChildren();
            treeModel.reload();
            stackTowerPanel.setStackItems(new ArrayList<>(), 0, 0);
            return;
        }

        // Dynamically adjust Base SP to highest SP observed
        if (currentSP > maxSeenSP || maxSeenSP == 0) {
            maxSeenSP = currentSP;
            initialSP = currentSP;
        }

        int baseSP = maxSeenSP;
        int bytesInStack = Math.max(0, baseSP - currentSP);
        int itemCount = bytesInStack / 2;

        lblSPVal.setText(String.format("SP: 0x%04X", currentSP));
        lblDepthVal.setText(String.format("Depth: %d Item(s)", itemCount));
        lblPCVal.setText(String.format("PC: 0x%04X", currentPC));
        lblBaseSPVal.setText(String.format("Base SP: 0x%04X", baseSP));

        // Color badge updates
        Color cyanC   = isDark ? new Color(0x38, 0xBD, 0xF8) : new Color(0x02, 0x84, 0xC7);
        Color emeraldC= isDark ? new Color(0x34, 0xD3, 0x99) : new Color(0x05, 0x96, 0x69);
        Color purpleC = isDark ? new Color(0xC0, 0x84, 0xFC) : new Color(0x7E, 0x22, 0xCE);
        Color amberC  = isDark ? new Color(0xFB, 0xBF, 0x24) : new Color(0xD9, 0x77, 0x06);

        lblSPVal.setForeground(cyanC);
        lblDepthVal.setForeground(emeraldC);
        lblPCVal.setForeground(purpleC);
        lblBaseSPVal.setForeground(amberC);

        List<StackFrameItem> items = new ArrayList<>();
        rootTreeNode.removeAllChildren();
        rootTreeNode.setUserObject(String.format("Main Program (PC: 0x%04X)", currentPC));
        DefaultMutableTreeNode currentTreeNode = rootTreeNode;

        // Traverse only the active stack region from currentSP up to baseSP
        int endAddr = Math.min(baseSP, currentSP + 128);

        for (int addr = currentSP; addr < endAddr && addr + 1 < activeMatrix.memory.length; addr += 2) {
            int lowByte = activeMatrix.memory[addr] & 0xFF;
            int highByte = activeMatrix.memory[addr + 1] & 0xFF;
            int val16 = (highByte << 8) | lowByte;

            String type = "Stack Word";
            String details = String.format("Word = 0x%04X (High: %02XH, Low: %02XH)", val16, highByte, lowByte);

            // 1. Check if val16 is a Subroutine CALL Return Address
            boolean isCallReturn = false;
            if (val16 >= 3 && val16 <= 0xFFFF) {
                int op3 = activeMatrix.memory[val16 - 3] & 0xFF;
                // CD = CALL, C4/CC/D4/DC/E4/EC/F4/FC = Conditional Calls
                if (op3 == 0xCD || op3 == 0xC4 || op3 == 0xCC || op3 == 0xD4 || op3 == 0xDC || op3 == 0xE4 || op3 == 0xEC || op3 == 0xF4 || op3 == 0xFC) {
                    isCallReturn = true;
                }
            }

            if (isCallReturn) {
                type = "CALL Return PC";
                details = String.format("Return PC -> 0x%04X (after CALL instruction)", val16);
            } else {
                // 2. Check if val16 matches active register pairs (PUSH BC, PUSH DE, PUSH HL, PUSH PSW)
                int regBC = ((activeMatrix.B & 0xFF) << 8) | (activeMatrix.C & 0xFF);
                int regDE = ((activeMatrix.D & 0xFF) << 8) | (activeMatrix.E & 0xFF);
                int regHL = ((activeMatrix.H & 0xFF) << 8) | (activeMatrix.L & 0xFF);
                int regPSW= ((activeMatrix.A & 0xFF) << 8) | (activeMatrix.F & 0xFF);

                if (highByte == (activeMatrix.D & 0xFF) && lowByte == (activeMatrix.E & 0xFF)) {
                    type = "PUSH DE Pair";
                    details = String.format("DE Pair = 0x%04X (D: %02XH, E: %02XH)", val16, highByte, lowByte);
                } else if (highByte == (activeMatrix.B & 0xFF) && lowByte == (activeMatrix.C & 0xFF)) {
                    type = "PUSH BC Pair";
                    details = String.format("BC Pair = 0x%04X (B: %02XH, C: %02XH)", val16, highByte, lowByte);
                } else if (highByte == (activeMatrix.H & 0xFF) && lowByte == (activeMatrix.L & 0xFF)) {
                    type = "PUSH HL Pair";
                    details = String.format("HL Pair = 0x%04X (H: %02XH, L: %02XH)", val16, highByte, lowByte);
                } else if (highByte == (activeMatrix.A & 0xFF) && lowByte == (activeMatrix.F & 0xFF)) {
                    type = "PUSH PSW (A-F)";
                    details = String.format("PSW Pair = 0x%04X (A: %02XH, Flags: %02XH)", val16, highByte, lowByte);
                }
            }

            StackFrameItem item = new StackFrameItem(addr, highByte, lowByte, type, details);
            items.add(item);

            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(
                String.format("[%s] Addr 0x%04X = 0x%04X", type, addr, val16)
            );
            currentTreeNode.add(childNode);
            currentTreeNode = childNode;
        }

        treeModel.reload();
        for (int i = 0; i < callTree.getRowCount(); i++) {
            callTree.expandRow(i);
        }

        // Populate Table Model
        tableModel.setRowCount(0);
        for (int i = 0; i < items.size(); i++) {
            StackFrameItem item = items.get(i);
            tableModel.addRow(new Object[]{
                String.format("0x%04X (SP+%d)", item.address, i * 2),
                item.type,
                String.format("0x%04X", item.val16),
                String.format("0x%02X (%d)", item.highByte, item.highByte),
                String.format("0x%02X (%d)", item.lowByte, item.lowByte),
                item.details
            });
        }

        // Update Graphic Tower
        stackTowerPanel.setStackItems(items, currentSP, baseSP);
        applyTableStyling();
    }

    private void applyTableStyling() {
        boolean isDark = com.formdev.flatlaf.FlatLaf.isLafDark();

        Color headerBg  = isDark ? new Color(0x0F, 0x17, 0x2A) : new Color(0xE0, 0xF2, 0xFE);
        Color headerFg  = isDark ? new Color(0x38, 0xBD, 0xF8) : new Color(0x02, 0x84, 0xC7);
        Color bgEven    = isDark ? new Color(0x0F, 0x17, 0x2A) : new Color(0xFF, 0xFF, 0xFF);
        Color bgOdd     = isDark ? new Color(0x1E, 0x29, 0x3B) : new Color(0xF0, 0xF9, 0xFF);
        Color gridColor = isDark ? new Color(0x33, 0x41, 0x55) : new Color(0xBA, 0xE6, 0xFD);
        Color selBg     = isDark ? new Color(0x03, 0x69, 0xA1) : new Color(0x02, 0x84, 0xC7);

        frameTable.setGridColor(gridColor);
        frameTable.setSelectionBackground(selBg);
        frameTable.setSelectionForeground(Color.WHITE);

        if (frameTable.getTableHeader() != null) {
            javax.swing.table.JTableHeader header = frameTable.getTableHeader();
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
                        BorderFactory.createEmptyBorder(2, 2, 2, 2)
                    ));
                    return l;
                }
            });
        }

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                String str = val != null ? val.toString() : "";
                JLabel comp = (JLabel) super.getTableCellRendererComponent(t, str, sel, focus, r, c);
                comp.setHorizontalAlignment(c == 0 || c == 5 ? SwingConstants.LEFT : SwingConstants.CENTER);
                comp.setFont(new Font(c == 2 || c == 3 || c == 4 ? "Consolas" : "Segoe UI", Font.BOLD, 12));
                comp.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));

                if (sel) {
                    comp.setBackground(selBg);
                    comp.setForeground(Color.WHITE);
                } else {
                    comp.setBackground(r % 2 == 0 ? bgEven : bgOdd);
                    if (c == 0) {
                        comp.setForeground(isDark ? new Color(0x38, 0xBD, 0xF8) : new Color(0x02, 0x84, 0xC7));
                    } else if (c == 1) {
                        if (str.startsWith("CALL")) comp.setForeground(isDark ? new Color(0xC0, 0x84, 0xFC) : new Color(0x7E, 0x22, 0xCE));
                        else if (str.startsWith("PUSH")) comp.setForeground(isDark ? new Color(0x38, 0xBD, 0xF8) : new Color(0x02, 0x84, 0xC7));
                        else comp.setForeground(isDark ? new Color(0xF1, 0xF5, 0xF9) : new Color(0x1E, 0x29, 0x3B));
                    } else if (c == 2) {
                        comp.setForeground(isDark ? new Color(0x34, 0xD3, 0x99) : new Color(0x05, 0x96, 0x69));
                    } else {
                        comp.setForeground(isDark ? new Color(0xE2, 0xE8, 0xF0) : new Color(0x1E, 0x29, 0x3B));
                    }
                }
                return comp;
            }
        };

        for (int i = 0; i < frameTable.getColumnCount(); i++) {
            frameTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        if (frameTable.getColumnCount() >= 6) {
            frameTable.getColumnModel().getColumn(0).setPreferredWidth(110);
            frameTable.getColumnModel().getColumn(1).setPreferredWidth(115);
            frameTable.getColumnModel().getColumn(2).setPreferredWidth(85);
            frameTable.getColumnModel().getColumn(3).setPreferredWidth(95);
            frameTable.getColumnModel().getColumn(4).setPreferredWidth(95);
            frameTable.getColumnModel().getColumn(5).setPreferredWidth(210);
        }
    }

    /**
     * Custom Graphic Panel rendering a 2D Stack Memory Tower
     */
    private static class StackTowerPanel extends JPanel {
        private List<StackFrameItem> items = new ArrayList<>();
        private int currentSP = 0;
        private int baseSP = 0;

        public StackTowerPanel() {
            setPreferredSize(new Dimension(320, 480));
        }

        public void setStackItems(List<StackFrameItem> items, int currentSP, int baseSP) {
            this.items = items;
            this.currentSP = currentSP;
            this.baseSP = baseSP;
            setPreferredSize(new Dimension(320, Math.max(480, (items.size() + 2) * 58)));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean isDark = com.formdev.flatlaf.FlatLaf.isLafDark();
            int width = getWidth();
            int y = 20;

            // Header Banner
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.setColor(isDark ? new Color(0x38, 0xBD, 0xF8) : new Color(0x02, 0x84, 0xC7));
            g2.drawString(String.format("SP: 0x%04X (Top of Stack)", currentSP), 15, y + 15);
            y += 35;

            if (items.isEmpty()) {
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
                g2.setColor(isDark ? Color.GRAY : Color.DARK_GRAY);
                g2.drawString(currentSP == 0 ? "Stack is uninitialized (SP = 0x0000)" : "Stack is empty (SP = Base SP)", 20, y + 30);
                return;
            }

            int boxW = width - 40;
            int boxH = 48;

            for (int i = 0; i < items.size(); i++) {
                StackFrameItem item = items.get(i);

                Color boxBg = isDark ? new Color(0x1E, 0x29, 0x3B) : new Color(0xF0, 0xF9, 0xFF);
                Color borderC = isDark ? new Color(0x38, 0xBD, 0xF8) : new Color(0x02, 0x84, 0xC7);

                if (item.type.startsWith("CALL")) {
                    boxBg = isDark ? new Color(0x3B, 0x07, 0x64) : new Color(0xF3, 0xE8, 0xFF);
                    borderC = isDark ? new Color(0xC0, 0x84, 0xFC) : new Color(0x7E, 0x22, 0xCE);
                } else if (item.type.startsWith("PUSH")) {
                    boxBg = isDark ? new Color(0x0F, 0x17, 0x2A) : new Color(0xE0, 0xF2, 0xFE);
                    borderC = isDark ? new Color(0x38, 0xBD, 0xF8) : new Color(0x02, 0x84, 0xC7);
                }

                if (i == 0) { // Top of Stack Highlight
                    boxBg = isDark ? new Color(0x06, 0x4E, 0x3B) : new Color(0xD1, 0xFA, 0xE5);
                    borderC = isDark ? new Color(0x34, 0xD3, 0x99) : new Color(0x05, 0x96, 0x69);
                }

                g2.setColor(boxBg);
                g2.fillRoundRect(20, y, boxW, boxH, 8, 8);

                g2.setColor(borderC);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect(20, y, boxW, boxH, 8, 8);

                // Slot Text
                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                g2.setColor(isDark ? Color.WHITE : Color.BLACK);
                g2.drawString(String.format("[0x%04X] 16-bit: 0x%04X", item.address, item.val16), 30, y + 20);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.setColor(isDark ? new Color(0x94, 0xA3, 0xB8) : new Color(0x47, 0x55, 0x69));
                g2.drawString(String.format("Type: %s (Hi: %02XH | Lo: %02XH)", item.type, item.highByte, item.lowByte), 30, y + 37);

                if (i == 0) {
                    g2.setColor(isDark ? new Color(0x34, 0xD3, 0x99) : new Color(0x05, 0x96, 0x69));
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    g2.drawString("SP", boxW - 25, y + 26);
                }

                y += 58;
            }
        }
    }
}
