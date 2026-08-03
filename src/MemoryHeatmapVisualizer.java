import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Interactive Memory Heatmap & Visualizer for 8085 Microprocessor Simulator
 * 64 KB Memory Map (0x0000 - 0xFFFF) showing real-time memory activity:
 *  Green: Program Code Execution Bytes
 *  Red: Memory Writes
 *  Blue: Memory Reads
 */
public class MemoryHeatmapVisualizer extends JFrame {

    public static final int MEMORY_SIZE = 65536; // 64 KB
    public static final int ACCESS_NONE = 0;
    public static final int ACCESS_EXEC = 1; //  Green
    public static final int ACCESS_READ = 2; //  Blue
    public static final int ACCESS_WRITE = 3; //  Red

    // Access Data Arrays
    private static final byte[] accessType = new byte[MEMORY_SIZE];
    private static final long[] lastAccessTime = new long[MEMORY_SIZE];
    private static final int[] execCounts = new int[MEMORY_SIZE];
    private static final int[] readCounts = new int[MEMORY_SIZE];
    private static final int[] writeCounts = new int[MEMORY_SIZE];

    private static MemoryHeatmapVisualizer instance;
    private final Matrix matrix;
    private final Assembler assembler;

    // View Settings
    private int selectedPage = 0; // 0..255 (Each page is 256 bytes)
    private boolean fullMapMode = true; // true = 64KB overview grid, false = 256-byte page detail
    private boolean showExec = true;
    private boolean showRead = true;
    private boolean showWrite = true;
    private int hoveredAddress = -1;
    private int selectedAddress = 0;

    // GUI Components
    private HeatmapCanvas canvas;
    private JComboBox<String> pageCombo;
    private JCheckBox chkExec, chkRead, chkWrite;
    private JLabel lblAddr, lblValHex, lblValDec, lblValBin, lblType, lblStats;
    private Timer repaintTimer;

    private JPanel topPanel;
    private JPanel bottomPanel;
    private JPanel infoGrid;
    private JLabel lblLegendExec, lblLegendRead, lblLegendWrite, lblLegendUnaccessed;

    public MemoryHeatmapVisualizer(Matrix matrix, Assembler assembler) {
        this.matrix = matrix;
        this.assembler = assembler;
        instance = this;

        setTitle(" 8085 Interactive Memory Heatmap & Visualizer");
        setSize(950, 700);
        setLocationRelativeTo(assembler);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();

        // Animation timer for live glow decay
        repaintTimer = new Timer(50, e -> {
            if (canvas != null && isShowing()) {
                canvas.repaint();
            }
        });
        repaintTimer.start();
    }

    public static MemoryHeatmapVisualizer getInstance(Matrix matrix, Assembler assembler) {
        if (instance == null || !instance.isDisplayable()) {
            instance = new MemoryHeatmapVisualizer(matrix, assembler);
        }
        return instance;
    }

    public static void recordExec(int address, int length) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < length; i++) {
            int addr = (address + i) & 0xFFFF;
            accessType[addr] = ACCESS_EXEC;
            lastAccessTime[addr] = now;
            execCounts[addr]++;
        }
        if (instance != null && instance.canvas != null) {
            instance.canvas.repaint();
        }
    }

    public static void recordRead(int address) {
        int addr = address & 0xFFFF;
        accessType[addr] = ACCESS_READ;
        lastAccessTime[addr] = System.currentTimeMillis();
        readCounts[addr]++;
        if (instance != null) {
            if (!instance.fullMapMode) {
                int targetPage = addr / 256;
                if (instance.selectedPage != targetPage && instance.pageCombo != null) {
                    instance.selectedPage = targetPage;
                    instance.pageCombo.setSelectedIndex(targetPage);
                }
            }
            if (instance.canvas != null) {
                instance.canvas.repaint();
            }
        }
    }

    public static void recordWrite(int address) {
        int addr = address & 0xFFFF;
        accessType[addr] = ACCESS_WRITE;
        lastAccessTime[addr] = System.currentTimeMillis();
        writeCounts[addr]++;
        if (instance != null) {
            if (!instance.fullMapMode) {
                int targetPage = addr / 256;
                if (instance.selectedPage != targetPage && instance.pageCombo != null) {
                    instance.selectedPage = targetPage;
                    instance.pageCombo.setSelectedIndex(targetPage);
                }
            }
            if (instance.canvas != null) {
                instance.canvas.repaint();
            }
        }
    }

    public static void resetHeatmap() {
        java.util.Arrays.fill(accessType, (byte) 0);
        java.util.Arrays.fill(lastAccessTime, 0L);
        java.util.Arrays.fill(execCounts, 0);
        java.util.Arrays.fill(readCounts, 0);
        java.util.Arrays.fill(writeCounts, 0);
        if (instance != null && instance.canvas != null) {
            instance.canvas.repaint();
        }
    }

    public void updateThemeColors() {
        if (topPanel == null || bottomPanel == null || infoGrid == null) return;
        boolean isDark = com.formdev.flatlaf.FlatLaf.isLafDark();
        if (isDark) {
            topPanel.setBackground(new Color(0x25, 0x25, 0x26));
            topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x3E, 0x3E, 0x42)));

            chkExec.setForeground(new Color(0x2E, 0xCC, 0x71));
            chkRead.setForeground(new Color(0x34, 0x98, 0xDB));
            chkWrite.setForeground(new Color(0xE7, 0x4C, 0x3C));

            bottomPanel.setBackground(new Color(0x1E, 0x1E, 0x1E));
            bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x3E, 0x3E, 0x42)),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));

            Color valFg = new Color(0xE0, 0xE0, 0xE0);
            lblAddr.setForeground(valFg);
            lblValHex.setForeground(valFg);
            lblValDec.setForeground(valFg);
            lblValBin.setForeground(valFg);
            lblStats.setForeground(valFg);

            for (Component comp : infoGrid.getComponents()) {
                if (comp instanceof JPanel) {
                    JPanel p = (JPanel) comp;
                    p.setBackground(new Color(0x2A, 0x2A, 0x2D));
                    if (p.getComponentCount() > 0 && p.getComponent(0) instanceof JLabel) {
                        p.getComponent(0).setForeground(new Color(0xAA, 0xAA, 0xAA));
                    }
                }
            }

            lblLegendExec.setForeground(new Color(0x2E, 0xCC, 0x71));
            lblLegendRead.setForeground(new Color(0x34, 0x98, 0xDB));
            lblLegendWrite.setForeground(new Color(0xE7, 0x4C, 0x3C));
            lblLegendUnaccessed.setForeground(new Color(0x88, 0x88, 0x88));
        } else {
            topPanel.setBackground(new Color(0xF1, 0xF5, 0xF9));
            topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xCB, 0xD5, 0xE1)));

            chkExec.setForeground(new Color(0x15, 0x80, 0x3D));
            chkRead.setForeground(new Color(0x02, 0x84, 0xC7));
            chkWrite.setForeground(new Color(0xDC, 0x26, 0x26));

            bottomPanel.setBackground(new Color(0xF1, 0xF5, 0xF9));
            bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xCB, 0xD5, 0xE1)),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));

            Color valFg = new Color(0x0F, 0x17, 0x2A);
            lblAddr.setForeground(valFg);
            lblValHex.setForeground(valFg);
            lblValDec.setForeground(valFg);
            lblValBin.setForeground(valFg);
            lblStats.setForeground(valFg);

            for (Component comp : infoGrid.getComponents()) {
                if (comp instanceof JPanel) {
                    JPanel p = (JPanel) comp;
                    p.setBackground(new Color(0xE2, 0xE8, 0xF0));
                    if (p.getComponentCount() > 0 && p.getComponent(0) instanceof JLabel) {
                        p.getComponent(0).setForeground(new Color(0x47, 0x55, 0x69));
                    }
                }
            }

            lblLegendExec.setForeground(new Color(0x15, 0x80, 0x3D));
            lblLegendRead.setForeground(new Color(0x02, 0x84, 0xC7));
            lblLegendWrite.setForeground(new Color(0xDC, 0x26, 0x26));
            lblLegendUnaccessed.setForeground(new Color(0x64, 0x74, 0x8B));
        }

        if (canvas != null) {
            canvas.updateUI();
            canvas.repaint();
        }
        updateInspector(selectedAddress);
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

        // --- TOP CONTROL BAR ---
        topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));

        JButton btnToggleMode = new JButton(" Toggle 64KB / Page View");
        btnToggleMode.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnToggleMode.addActionListener(e -> {
            fullMapMode = !fullMapMode;
            btnToggleMode.setText(fullMapMode ? " Switch to Page Detail View" : " Switch to 64KB Full Map");
            pageCombo.setEnabled(!fullMapMode);
            canvas.repaint();
        });
        topPanel.add(btnToggleMode);

        topPanel.add(new JLabel("  Page (256B):"));
        String[] pageItems = new String[256];
        for (int i = 0; i < 256; i++) {
            int start = i * 256;
            int end = start + 255;
            pageItems[i] = String.format("Page %02X (%04XH - %04XH)", i, start, end);
        }
        pageCombo = new JComboBox<>(pageItems);
        pageCombo.setFont(new Font("Monospaced", Font.BOLD, 12));
        pageCombo.setEnabled(false);
        pageCombo.addActionListener(e -> {
            selectedPage = pageCombo.getSelectedIndex();
            canvas.repaint();
        });
        topPanel.add(pageCombo);

        chkExec = new JCheckBox(" Code Exec", true);
        chkRead = new JCheckBox(" Memory Read", true);
        chkWrite = new JCheckBox(" Memory Write", true);
        styleCheckBox(chkExec, new Color(0x2E, 0xCC, 0x71));
        styleCheckBox(chkRead, new Color(0x34, 0x98, 0xDB));
        styleCheckBox(chkWrite, new Color(0xE7, 0x4C, 0x3C));

        chkExec.addActionListener(e -> { showExec = chkExec.isSelected(); canvas.repaint(); });
        chkRead.addActionListener(e -> { showRead = chkRead.isSelected(); canvas.repaint(); });
        chkWrite.addActionListener(e -> { showWrite = chkWrite.isSelected(); canvas.repaint(); });

        topPanel.add(chkExec);
        topPanel.add(chkRead);
        topPanel.add(chkWrite);

        JButton btnReset = new JButton(" Clear Activity");
        btnReset.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnReset.addActionListener(e -> resetHeatmap());
        topPanel.add(btnReset);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // --- MAIN CANVAS AREA ---
        canvas = new HeatmapCanvas();
        mainPanel.add(canvas, BorderLayout.CENTER);

        // --- BOTTOM DETAILS PANEL ---
        bottomPanel = new JPanel(new BorderLayout(10, 5));

        infoGrid = new JPanel(new GridLayout(1, 6, 15, 5));
        infoGrid.setOpaque(false);

        lblAddr = createValueLabel("0x0000 (0)");
        lblValHex = createValueLabel("0x00");
        lblValDec = createValueLabel("0");
        lblValBin = createValueLabel("00000000");
        lblType = createValueLabel("NONE");
        lblStats = createValueLabel("Exec: 0 | R: 0 | W: 0");

        infoGrid.add(createFieldBox("Address:", lblAddr));
        infoGrid.add(createFieldBox("Hex Value:", lblValHex));
        infoGrid.add(createFieldBox("Dec Value:", lblValDec));
        infoGrid.add(createFieldBox("Binary:", lblValBin));
        infoGrid.add(createFieldBox("Last Access:", lblType));
        infoGrid.add(createFieldBox("Counts:", lblStats));

        bottomPanel.add(infoGrid, BorderLayout.CENTER);

        // Legend bar
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 2));
        legendPanel.setOpaque(false);
        lblLegendExec = createLegendBadge(" Execution Byte", new Color(0x2E, 0xCC, 0x71));
        lblLegendRead = createLegendBadge(" Read Access", new Color(0x34, 0x98, 0xDB));
        lblLegendWrite = createLegendBadge(" Write Access", new Color(0xE7, 0x4C, 0x3C));
        lblLegendUnaccessed = createLegendBadge(" Unaccessed", new Color(0x2A, 0x2A, 0x2C));
        legendPanel.add(lblLegendExec);
        legendPanel.add(lblLegendRead);
        legendPanel.add(lblLegendWrite);
        legendPanel.add(lblLegendUnaccessed);
        bottomPanel.add(legendPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        updateInspector(0);
    }

    private void styleCheckBox(JCheckBox cb, Color fg) {
        cb.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cb.setForeground(fg);
        cb.setOpaque(false);
    }

    private JPanel createFieldBox(String title, JLabel valLabel) {
        JPanel p = new JPanel(new BorderLayout(2, 2));
        p.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        titleLabel.setForeground(new Color(0xAA, 0xAA, 0xAA));
        p.add(titleLabel, BorderLayout.NORTH);
        p.add(valLabel, BorderLayout.CENTER);
        return p;
    }

    private JLabel createValueLabel(String initialText) {
        JLabel lbl = new JLabel(initialText);
        lbl.setFont(new Font("Monospaced", Font.BOLD, 13));
        lbl.setForeground(new Color(0xE0, 0xE0, 0xE0));
        return lbl;
    }

    private JLabel createLegendBadge(String text, Color c) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(c);
        return lbl;
    }

    private void updateInspector(int addr) {
        if (addr < 0 || addr >= MEMORY_SIZE) return;
        selectedAddress = addr;
        int val = (assembler != null && assembler.matrix != null && assembler.matrix.memory != null)
                ? (assembler.matrix.memory[addr] & 0xFF)
                : ((matrix != null && matrix.memory != null) ? (matrix.memory[addr] & 0xFF) : 0);

        lblAddr.setText(String.format("0x%04X (%d)", addr, addr));
        lblValHex.setText(String.format("0x%02X", val));
        lblValDec.setText(String.valueOf(val));
        
        String bin = Integer.toBinaryString(val);
        while (bin.length() < 8) bin = "0" + bin;
        lblValBin.setText(bin);

        int type = accessType[addr];
        boolean isDark = com.formdev.flatlaf.FlatLaf.isLafDark();
        if (writeCounts[addr] > 0 && readCounts[addr] > 0) {
            lblType.setText(" WRITE &  READ");
            lblType.setForeground(isDark ? new Color(0xE0, 0x40, 0xFB) : new Color(0x8E, 0x44, 0xAD));
        } else if (type == ACCESS_EXEC) {
            lblType.setText(" EXECUTION");
            lblType.setForeground(isDark ? new Color(0x2E, 0xCC, 0x71) : new Color(0x27, 0xAE, 0x60));
        } else if (type == ACCESS_READ) {
            lblType.setText(" READ");
            lblType.setForeground(isDark ? new Color(0x34, 0x98, 0xDB) : new Color(0x29, 0x80, 0xB9));
        } else if (type == ACCESS_WRITE) {
            lblType.setText(" WRITE");
            lblType.setForeground(isDark ? new Color(0xE7, 0x4C, 0x3C) : new Color(0xC0, 0x39, 0x2B));
        } else {
            lblType.setText("NONE");
            lblType.setForeground(isDark ? new Color(0xAA, 0xAA, 0xAA) : new Color(0x66, 0x66, 0x66));
        }

        lblStats.setText(String.format("E:%d | R:%d | W:%d", execCounts[addr], readCounts[addr], writeCounts[addr]));
    }

    // --- CUSTOM CANVAS RENDERING CLASS ---
    private class HeatmapCanvas extends JPanel {

        public HeatmapCanvas() {
            setToolTipText("");

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int addr = getAddressFromPoint(e.getPoint());
                    if (addr != hoveredAddress) {
                        hoveredAddress = addr;
                        if (hoveredAddress != -1) {
                            updateInspector(hoveredAddress);
                        }
                        repaint();
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    int addr = getAddressFromPoint(e.getPoint());
                    if (addr != -1) {
                        updateInspector(addr);
                        if (e.getClickCount() == 2 && fullMapMode) {
                            // Double click to zoom into page
                            selectedPage = addr / 256;
                            pageCombo.setSelectedIndex(selectedPage);
                            fullMapMode = false;
                            pageCombo.setEnabled(true);
                            repaint();
                        }
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoveredAddress = -1;
                    repaint();
                }
            };

            addMouseMotionListener(adapter);
            addMouseListener(adapter);
        }

        @Override
        public void updateUI() {
            super.updateUI();
            boolean isDark = com.formdev.flatlaf.FlatLaf.isLafDark();
            setBackground(isDark ? new Color(0x18, 0x18, 0x18) : new Color(0xFA, 0xFA, 0xFA));
        }

        private int getAddressFromPoint(Point p) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return -1;

            if (fullMapMode) {
                // 256 x 256 grid
                double cellW = (double) w / 256.0;
                double cellH = (double) h / 256.0;
                int col = (int) (p.x / cellW);
                int row = (int) (p.y / cellH);
                col = Math.min(255, Math.max(0, col));
                row = Math.min(255, Math.max(0, row));
                return row * 256 + col;
            } else {
                // 16 x 16 grid for current 256-byte page
                double cellW = (double) w / 16.0;
                double cellH = (double) h / 16.0;
                int col = (int) (p.x / cellW);
                int row = (int) (p.y / cellH);
                col = Math.min(15, Math.max(0, col));
                row = Math.min(15, Math.max(0, row));
                return selectedPage * 256 + row * 16 + col;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            long now = System.currentTimeMillis();
            boolean isDark = com.formdev.flatlaf.FlatLaf.isLafDark();

            if (fullMapMode) {
                // 64 KB FULL MAP (256 cols x 256 rows)
                double cellW = (double) w / 256.0;
                double cellH = (double) h / 256.0;

                for (int row = 0; row < 256; row++) {
                    for (int col = 0; col < 256; col++) {
                        int addr = row * 256 + col;
                        Color c = getCellColor(addr, now);

                        int x = (int) (col * cellW);
                        int y = (int) (row * cellH);
                        int cw = (int) Math.ceil(cellW);
                        int ch = (int) Math.ceil(cellH);

                        g2.setColor(c);
                        g2.fillRect(x, y, cw, ch);

                        if (addr == hoveredAddress || addr == selectedAddress) {
                            g2.setColor(isDark ? Color.WHITE : Color.BLACK);
                            g2.drawRect(x, y, Math.max(1, cw - 1), Math.max(1, ch - 1));
                        }
                    }
                }
            } else {
                // PAGE DETAIL VIEW (16 cols x 16 rows = 256 bytes)
                double cellW = (double) w / 16.0;
                double cellH = (double) h / 16.0;
                int baseAddr = selectedPage * 256;

                for (int row = 0; row < 16; row++) {
                    for (int col = 0; col < 16; col++) {
                        int offset = row * 16 + col;
                        int addr = baseAddr + offset;
                        Color c = getCellColor(addr, now);

                        int x = (int) (col * cellW);
                        int y = (int) (row * cellH);
                        int cw = (int) cellW;
                        int ch = (int) cellH;

                        // Cell fill
                        g2.setColor(c);
                        g2.fillRect(x + 2, y + 2, cw - 4, ch - 4);

                        // Cell border
                        g2.setColor(isDark ? new Color(0x3A, 0x3A, 0x3C) : new Color(0xDA, 0xDA, 0xDA));
                        g2.drawRect(x + 2, y + 2, cw - 4, ch - 4);

                        if (addr == hoveredAddress || addr == selectedAddress) {
                            g2.setColor(isDark ? Color.YELLOW : new Color(0x00, 0x7A, 0xCC));
                            g2.setStroke(new BasicStroke(2));
                            g2.drawRect(x + 1, y + 1, cw - 2, ch - 2);
                            g2.setStroke(new BasicStroke(1));
                        }

                        // Text inside cell
                        int val = (assembler != null && assembler.matrix != null && assembler.matrix.memory != null)
                                ? (assembler.matrix.memory[addr] & 0xFF)
                                : ((matrix != null && matrix.memory != null) ? (matrix.memory[addr] & 0xFF) : 0);
                        g2.setFont(new Font("Monospaced", Font.BOLD, Math.min(13, (int) (cellH * 0.28))));
                        g2.setColor(isDark ? new Color(0xAA, 0xAA, 0xAA) : new Color(0x55, 0x55, 0x55));
                        g2.drawString(String.format("%04X", addr), x + 6, y + (int) (cellH * 0.35));

                        g2.setFont(new Font("Monospaced", Font.BOLD, Math.min(16, (int) (cellH * 0.38))));
                        g2.setColor(isDark ? Color.WHITE : new Color(0x11, 0x11, 0x11));
                        g2.drawString(String.format("%02X", val), x + 6, y + (int) (cellH * 0.75));
                    }
                }
            }

            g2.dispose();
        }

        private Color getCellColor(int addr, long now) {
            boolean isDark = com.formdev.flatlaf.FlatLaf.isLafDark();
            Color unaccessedColor = isDark ? new Color(0x22, 0x22, 0x24) : new Color(0xEE, 0xEE, 0xF0);

            int type = accessType[addr];
            if ((type == ACCESS_EXEC && !showExec) ||
                (type == ACCESS_READ && !showRead) ||
                (type == ACCESS_WRITE && !showWrite)) {
                return unaccessedColor;
            }

            if (type == ACCESS_NONE) {
                return unaccessedColor;
            }

            // Glow decay based on last access timestamp (up to 3 seconds fade)
            long elapsed = now - lastAccessTime[addr];
            float decay = 1.0f - Math.min(1.0f, elapsed / 3000.0f); // 1.0 = fresh glow, 0.0 = settled color

            float factor = 0.30f + 0.70f * decay;

            Color baseColor;
            if (writeCounts[addr] > 0 && readCounts[addr] > 0 && showWrite && showRead) {
                baseColor = isDark ? new Color(0xE0, 0x40, 0xFB) : new Color(0x8E, 0x44, 0xAD);
            } else {
                switch (type) {
                    case ACCESS_EXEC: //  Green
                        baseColor = isDark ? new Color(46, 204, 113) : new Color(39, 174, 96);
                        break;
                    case ACCESS_READ: //  Blue
                        baseColor = isDark ? new Color(52, 152, 219) : new Color(41, 128, 185);
                        break;
                    case ACCESS_WRITE: //  Red
                        baseColor = isDark ? new Color(231, 76, 60) : new Color(192, 57, 43);
                        break;
                    default:
                        return unaccessedColor;
                }
            }

            int r = (int) (baseColor.getRed() * factor + unaccessedColor.getRed() * (1 - factor));
            int g = (int) (baseColor.getGreen() * factor + unaccessedColor.getGreen() * (1 - factor));
            int b = (int) (baseColor.getBlue() * factor + unaccessedColor.getBlue() * (1 - factor));
            return new Color(
                Math.min(255, Math.max(0, r)),
                Math.min(255, Math.max(0, g)),
                Math.min(255, Math.max(0, b))
            );
        }
    }
}
