import com.formdev.flatlaf.FlatLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class ModernIDEUI extends JPanel {

    // ── Theme Color System (Supports Dark & Light Modes) ─────────────────────
    public static boolean isDarkMode = true;

    public static Color COLOR_BG_DARK      = new Color(0x05, 0x05, 0x07);
    public static Color COLOR_BG_SIDEBAR   = new Color(0x0A, 0x0B, 0x0E);
    public static Color COLOR_BG_HEADER    = new Color(0x0A, 0x0B, 0x0E);
    public static Color COLOR_BG_TOOLBAR   = new Color(0x12, 0x13, 0x18);
    public static Color COLOR_BG_CARD      = new Color(0x14, 0x16, 0x1D);
    public static Color COLOR_CARD_BORDER  = new Color(0x2A, 0x2E, 0x3D);
    public static Color COLOR_PRIMARY_BLUE = new Color(0x0D, 0x6E, 0xFD);
    public static Color COLOR_CYAN_ACCENT  = new Color(0x38, 0xBD, 0xF8);
    public static Color COLOR_AMBER_ACCENT = new Color(0xF5, 0x9E, 0x0B);
    public static Color COLOR_GREEN_ACCENT = new Color(0x22, 0xC5, 0x5E);
    public static Color COLOR_RED_ACCENT   = new Color(0xEF, 0x44, 0x44);
    public static Color COLOR_TEXT_PRIMARY = new Color(0xFF, 0xFF, 0xFF);
    public static Color COLOR_TEXT_MUTED   = new Color(0x9E, 0xA3, 0xB2);
    public static Color COLOR_ROW_ACTIVE   = new Color(0x1E, 0x28, 0x3D);

    private JLabel btnThemeHeader;
    private JLabel statusThemeMetaLabel;

    public static void updateThemeColors() {
        if (isDarkMode) {
            COLOR_BG_DARK      = new Color(0x05, 0x05, 0x07);
            COLOR_BG_SIDEBAR   = new Color(0x0A, 0x0B, 0x0E);
            COLOR_BG_HEADER    = new Color(0x0A, 0x0B, 0x0E);
            COLOR_BG_TOOLBAR   = new Color(0x12, 0x13, 0x18);
            COLOR_BG_CARD      = new Color(0x14, 0x16, 0x1D);
            COLOR_CARD_BORDER  = new Color(0x2A, 0x2E, 0x3D);
            COLOR_PRIMARY_BLUE = new Color(0x0D, 0x6E, 0xFD);
            COLOR_CYAN_ACCENT  = new Color(0x38, 0xBD, 0xF8);
            COLOR_AMBER_ACCENT = new Color(0xF5, 0x9E, 0x0B);
            COLOR_GREEN_ACCENT = new Color(0x22, 0xC5, 0x5E);
            COLOR_RED_ACCENT   = new Color(0xEF, 0x44, 0x44);
            COLOR_TEXT_PRIMARY = new Color(0xFF, 0xFF, 0xFF);
            COLOR_TEXT_MUTED   = new Color(0x9E, 0xA3, 0xB2);
            COLOR_ROW_ACTIVE   = new Color(0x1E, 0x28, 0x3D);
        } else {
            COLOR_BG_DARK      = new Color(0xF8, 0xFA, 0xFC); // Canvas background
            COLOR_BG_SIDEBAR   = new Color(0xFF, 0xFF, 0xFF); // Pure white sidebar
            COLOR_BG_HEADER    = new Color(0xFF, 0xFF, 0xFF); // Pure white header
            COLOR_BG_TOOLBAR   = new Color(0xFF, 0xFF, 0xFF); // Pure white action bar
            COLOR_BG_CARD      = new Color(0xFF, 0xFF, 0xFF); // Pure white cards
            COLOR_CARD_BORDER  = new Color(0xE2, 0xE8, 0xF0); // Light gray borders
            COLOR_PRIMARY_BLUE = new Color(0x00, 0x78, 0xD4); // Electric primary blue
            COLOR_CYAN_ACCENT  = new Color(0x02, 0x84, 0xC7); // Sky blue accent
            COLOR_AMBER_ACCENT = new Color(0xD9, 0x77, 0x06); // Warm amber accent
            COLOR_GREEN_ACCENT = new Color(0x16, 0xA3, 0x4A); // Forest green accent
            COLOR_RED_ACCENT   = new Color(0xDC, 0x26, 0x26); // Crimson red accent
            COLOR_TEXT_PRIMARY = new Color(0x0F, 0x17, 0x2A); // Dark slate text
            COLOR_TEXT_MUTED   = new Color(0x64, 0x74, 0x8B); // Slate gray muted text
            COLOR_ROW_ACTIVE   = new Color(0xDB, 0xEA, 0xFE); // Soft light blue row
        }
    }

    // ── Recent files persistence ───────────────────────────────────────────────
    private static final String PREFS_NODE       = "AuraStudio";
    private static final String PREFS_RECENT_KEY = "aura_recent_files";
    private static final int    MAX_RECENT       = 5;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Assembler assembler;

    private JPanel     centerEditorContainer;
    private CardLayout editorCardLayout;
    private JPanel     welcomeViewPanel;
    private JSplitPane workspaceSplitPane;

    private JLabel[] regValLabels  = new JLabel[8];
    private JPanel[] regRowPanels  = new JPanel[8];
    private CircleFlagBadge[] flagCircles = new CircleFlagBadge[5];   // S Z AC P CY

    private JLabel lblPcVal, lblSpVal, lblHlVal, lblBcVal, lblDeVal;
    private JLabel lblCurInstr, lblPcExec, lblInstrCount, lblCyclesCount, lblMCycles;
    private PillBadgeLabel lblExecStateBadge;
    private JLabel lblTrap, lblRst75, lblRst65, lblRst55, lblIntr;
    private JLabel lblSimVal, lblRimVal;

    private JTable            memoryTable;
    private DefaultTableModel memoryTableModel;

    private JLabel statusStateBadge;
    private JLabel statusInfoLabel;

    private JLabel tabTitle;
    private JLabel unsavedDot;
    private JLabel debuggerExplainerLabel;

    private JButton btnAssembleBar;
    private JButton btnRunBar;

    public enum AssembleStatus {
        INITIAL,
        SUCCESS,
        ERROR
    }

    private AssembleStatus currentAssembleStatus = AssembleStatus.INITIAL;

    public void setAssembleStatus(boolean success) {
        currentAssembleStatus = success ? AssembleStatus.SUCCESS : AssembleStatus.ERROR;
        updateAssembleButtonStyle();
    }

    public void resetAssembleStatus() {
        if (currentAssembleStatus != AssembleStatus.INITIAL) {
            currentAssembleStatus = AssembleStatus.INITIAL;
            updateAssembleButtonStyle();
        }
    }

    public void updateAssembleButtonStyle() {
        if (btnAssembleBar == null) return;
        btnAssembleBar.setEnabled(true);
        switch (currentAssembleStatus) {
            case SUCCESS:
                btnAssembleBar.setText("✓ Assembled");
                btnAssembleBar.setBackground(COLOR_GREEN_ACCENT);
                btnAssembleBar.setForeground(Color.WHITE);
                break;
            case ERROR:
                btnAssembleBar.setText("✖ Error");
                btnAssembleBar.setBackground(COLOR_RED_ACCENT);
                btnAssembleBar.setForeground(Color.WHITE);
                break;
            case INITIAL:
            default:
                btnAssembleBar.setText("⚙ Assemble");
                btnAssembleBar.setBackground(COLOR_BG_CARD);
                btnAssembleBar.setForeground(COLOR_TEXT_PRIMARY);
                break;
        }
    }

    private String              activeSidebarItem = "Editor";
    private Map<String, JPanel> sidebarButtons    = new HashMap<>();

    private JPanel      recentListPanel;
    private final List<String> recentFiles = new ArrayList<>();

    // ── Custom Vector Icon Renderer for Card Headers ──────────────────────────
    static class CardHeaderIcon implements Icon {
        private final String type;
        private final Color color;

        public CardHeaderIcon(String type, Color color) {
            this.type = type;
            this.color = color;
        }

        @Override public int getIconWidth()  { return 12; }
        @Override public int getIconHeight() { return 12; }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            switch (type) {
                case "REGISTERS":
                    g2.drawRoundRect(x, y, 11, 11, 2, 2);
                    g2.fillRect(x + 2, y + 2, 3, 3);
                    g2.fillRect(x + 6, y + 2, 3, 3);
                    g2.fillRect(x + 2, y + 6, 3, 3);
                    g2.fillRect(x + 6, y + 6, 3, 3);
                    break;
                case "FLAGS":
                    g2.fillRect(x + 1, y, 2, 11);
                    int[] px = {x + 3, x + 10, x + 3};
                    int[] py = {y + 1, y + 3,  y + 7};
                    g2.fillPolygon(px, py, 3);
                    break;
                case "POINTERS":
                    g2.fillOval(x + 2, y + 1, 7, 7);
                    g2.drawLine(x + 5, y + 8, x + 5, y + 11);
                    break;
                case "EXECUTION":
                case "INTERRUPTS":
                    int[] lx = {x + 6, x + 2, x + 5, x + 3, x + 9, x + 6};
                    int[] ly = {y,     y + 5, y + 5, y + 11,y + 4,  y + 4};
                    g2.fillPolygon(lx, ly, 6);
                    break;
                case "SIM / RIM":
                    g2.fillRect(x + 1, y + 7, 2, 4);
                    g2.fillRect(x + 4, y + 4, 2, 7);
                    g2.fillRect(x + 7, y + 2, 2, 9);
                    g2.fillRect(x + 10,y,     2, 11);
                    break;
                case "DEVICES":
                    g2.drawRoundRect(x, y, 10, 10, 2, 2);
                    g2.fillRect(x + 2, y + 2, 6, 6);
                    break;
                case "MEMORY":
                    g2.drawRoundRect(x, y, 10, 10, 2, 2);
                    g2.drawLine(x, y + 4, x + 10, y + 4);
                    g2.drawLine(x, y + 7, x + 10, y + 7);
                    g2.drawLine(x + 4, y, x + 4,  y + 10);
                    g2.drawLine(x + 7, y, x + 7,  y + 10);
                    break;
                default:
                    g2.fillRect(x, y, 10, 10);
            }
            g2.dispose();
        }
    }

    // ── Smooth Circular Flag Badge ───────────────────────────────────────────
    static class CircleFlagBadge extends JComponent {
        private final String label;
        private boolean active;

        public CircleFlagBadge(String label, boolean active) {
            this.label = label;
            this.active = active;
            setPreferredSize(new Dimension(32, 32));
            setMinimumSize(new Dimension(32, 32));
            setMaximumSize(new Dimension(32, 32));
        }

        public void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int size = Math.min(w, h) - 2;
            int x = (w - size) / 2;
            int y = (h - size) / 2;

            if (active) {
                g2.setColor(COLOR_PRIMARY_BLUE);
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(isDarkMode ? new Color(0x22, 0x24, 0x2B) : new Color(0xF1, 0xF5, 0xF9));
                g2.fillOval(x, y, size, size);
                g2.setColor(COLOR_CARD_BORDER);
                g2.drawOval(x, y, size, size);
                g2.setColor(COLOR_TEXT_MUTED);
            }

            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(label)) / 2;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(label, tx, ty);
            g2.dispose();
        }
    }

    // ── Rounded Pill Badge Label ──────────────────────────────────────────────
    static class PillBadgeLabel extends JLabel {
        private Color bgColor;
        private Color borderColor;

        public PillBadgeLabel(String text, Color fg, Color bg, Color border) {
            super(text, SwingConstants.CENTER);
            this.bgColor = bg;
            this.borderColor = border;
            setFont(new Font("Segoe UI", Font.BOLD, 9));
            setForeground(fg);
            setOpaque(false);
            setBorder(new EmptyBorder(2, 6, 2, 6));
        }

        public void setColors(Color fg, Color bg, Color border) {
            setForeground(fg);
            this.bgColor = bg;
            this.borderColor = border;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── Green Dot Icon ────────────────────────────────────────────────────────
    static class GreenDotIcon implements Icon {
        @Override public int getIconWidth()  { return 6; }
        @Override public int getIconHeight() { return 6; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(COLOR_GREEN_ACCENT);
            g2.fillOval(x, y + 1, 5, 5);
            g2.dispose();
        }
    }

    // ── Constructor ───────────────────────────────────────────────────────────
    public ModernIDEUI(Assembler asm) {
        this.assembler = asm;
        loadRecentFiles();
        if (recentFiles.isEmpty()) {
            recentFiles.add("factorial.asm");
            recentFiles.add("count.asm");
            recentFiles.add("addition.asm");
            recentFiles.add("bubble_sort.asm");
            recentFiles.add("array_sum.asm");
        }
        setLayout(new BorderLayout());
        setBackground(COLOR_BG_DARK);

        add(createTopHeaderPanel(), BorderLayout.NORTH);

        JPanel mainWorkspace = new JPanel(new BorderLayout());
        mainWorkspace.setBackground(COLOR_BG_DARK);
        mainWorkspace.add(createActivitySidebar(), BorderLayout.WEST);

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setBackground(COLOR_BG_DARK);
        centerSplit.setDividerSize(4);
        centerSplit.setResizeWeight(0.58);
        centerSplit.setBorder(null);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(COLOR_BG_DARK);
        centerPanel.add(createActionBar(),      BorderLayout.NORTH);
        centerPanel.add(createEditorContainer(),BorderLayout.CENTER);
        
        JPanel dashboardCardsPanel = createDashboardCardsPanel();
        centerPanel.setMinimumSize(new Dimension(200, 0));
        dashboardCardsPanel.setMinimumSize(new Dimension(360, 0));

        centerSplit.setLeftComponent(centerPanel);
        centerSplit.setRightComponent(dashboardCardsPanel);

        mainWorkspace.add(centerSplit, BorderLayout.CENTER);
        add(mainWorkspace, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        SwingUtilities.invokeLater(this::showWelcomeView);
        refreshData();
    }

    // ════════════════════════════════════════════════════════════════════════
    // TOP HEADER
    // ════════════════════════════════════════════════════════════════════════
    private JPanel createTopHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_BG_HEADER);
        header.setPreferredSize(new Dimension(1200, 42));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_CARD_BORDER));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        brandPanel.setOpaque(false);

        JLabel logoIcon = new JLabel();
        try {
            java.awt.image.BufferedImage raw = javax.imageio.ImageIO.read(
                getClass().getResourceAsStream("/aura_logo.dat"));
            if (raw != null) {
                java.awt.Image scaled = raw.getScaledInstance(26, 26, java.awt.Image.SCALE_SMOOTH);
                logoIcon.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception ignored) {}
        logoIcon.setPreferredSize(new Dimension(26, 26));

        JLabel titleLabel = new JLabel("Aura Studio");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setForeground(COLOR_TEXT_PRIMARY);

        JLabel subLabel = new JLabel("8085 Microprocessor Simulator");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(COLOR_TEXT_MUTED);

        brandPanel.add(logoIcon);
        brandPanel.add(titleLabel);
        brandPanel.add(subLabel);
        header.add(brandPanel, BorderLayout.WEST);

        JMenuBar mb = assembler.getJMenuBar();
        if (mb != null) {
            mb.setOpaque(false);
            mb.setBackground(COLOR_BG_HEADER);
            mb.setBorder(null);
            
            boolean hasThemeMenu = false;
            for (int i = 0; i < mb.getMenuCount(); i++) {
                if ("Theme".equalsIgnoreCase(mb.getMenu(i).getText())) {
                    hasThemeMenu = true;
                    break;
                }
            }
            if (!hasThemeMenu) {
                JMenu themeMenu = new JMenu("Theme");
                themeMenu.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                JMenuItem toggleItem = new JMenuItem("Toggle Light / Dark Theme");
                toggleItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                toggleItem.addActionListener(e -> toggleTheme());
                themeMenu.add(toggleItem);
                mb.add(themeMenu);
            }
            header.add(mb, BorderLayout.CENTER);
        }

        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 8));
        rightTools.setOpaque(false);

        btnThemeHeader = new JLabel(isDarkMode ? "☀️" : "🌙");
        btnThemeHeader.setFont(getIconFont(14, Font.PLAIN));
        btnThemeHeader.setForeground(COLOR_TEXT_MUTED);
        btnThemeHeader.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnThemeHeader.setToolTipText(isDarkMode ? "Switch to Light Theme" : "Switch to Dark Theme");
        btnThemeHeader.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                toggleTheme();
            }
        });

        JLabel btnSpeed = new JLabel("⏱");
        btnSpeed.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btnSpeed.setForeground(COLOR_TEXT_MUTED);
        btnSpeed.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSpeed.setToolTipText("Execution Speed / Settings");
        btnSpeed.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                assembler.openSettings();
            }
        });

        JLabel btnDots = new JLabel("⋮");
        btnDots.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnDots.setForeground(COLOR_TEXT_MUTED);
        btnDots.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDots.setToolTipText("More Options / Settings");
        btnDots.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                assembler.openSettings();
            }
        });

        rightTools.add(btnThemeHeader);
        rightTools.add(btnSpeed);
        rightTools.add(btnDots);
        header.add(rightTools, BorderLayout.EAST);

        return header;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ACTION BAR
    // ════════════════════════════════════════════════════════════════════════
    private JPanel createActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        bar.setBackground(COLOR_BG_TOOLBAR);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_CARD_BORDER));

        bar.add(makeBtn("New",  "📄", e -> {
            assembler.jTextAreaAssemblyLanguageEditor.setText("; New 8085 Program\n\nMVI A, 00H\nHLT\n");
            assembler.textEditor.colorEditor();
            setTabTitle("untitled.asm");
            showEditorView();
        }));
        bar.add(makeBtn("Open", "📂", e -> {
            if (assembler.jMenuItemLoad_Assembly_Language_code != null)
                assembler.jMenuItemLoad_Assembly_Language_code.doClick();
        }));
        bar.add(makeBtn("Save", "💾", e -> {
            if (assembler.jMenuItemSave_Assembly_Language_code != null)
                assembler.jMenuItemSave_Assembly_Language_code.doClick();
            markSaved();
        }));
        addSep(bar);

        btnAssembleBar = makeBtn("Assemble", "⚙", e -> {
            showEditorView();
            btnAssembleBar.setText("⏳ Assembling...");
            btnAssembleBar.setBackground(COLOR_AMBER_ACCENT);
            btnAssembleBar.setForeground(new Color(0x30, 0x20, 0x00));
            btnAssembleBar.setEnabled(false);
            SwingUtilities.invokeLater(() -> {
                if (assembler.jButtonAssemble != null) assembler.jButtonAssemble.doClick();
            });
        });
        bar.add(btnAssembleBar);

        btnRunBar = new JButton("▶ Run   ▼");
        styleRunBtn(btnRunBar, COLOR_PRIMARY_BLUE, Color.WHITE);
        btnRunBar.addActionListener(e -> {
            showEditorView();
            if (assembler.jButtonRun != null) assembler.jButtonRun.doClick();
            setExecutionState("RUNNING");
        });
        bar.add(btnRunBar);
        addSep(bar);

        bar.add(makeBtn("Step Back", "⏮", e -> {
            showDebuggerView();
            if (assembler.jButtonBackward != null) assembler.jButtonBackward.doClick();
        }));
        bar.add(makeBtn("Step Fwd",  "⏭", e -> {
            showDebuggerView();
            if (assembler.jButtonForward != null && assembler.jButtonForward.isVisible()) {
                assembler.jButtonForward.doClick();
            } else if (assembler.jButtonStep != null) {
                assembler.jButtonStep.doClick();
            }
        }));
        bar.add(makeBtn("Step",      "⏯", e -> {
            showDebuggerView();
            if (assembler.jButtonStep != null) assembler.jButtonStep.doClick();
        }));
        bar.add(makeBtn("Pause",     "⏸", e -> {
            if (assembler.jButtonStop != null) { assembler.jButtonStop.setVisible(true); assembler.jButtonStop.setText("Pause"); assembler.jButtonStop.doClick(); }
            setExecutionState("PAUSED");
        }));
        bar.add(makeBtn("Stop",      "⏹",  e -> {
            if (assembler.jButtonStop != null) { assembler.jButtonStop.setVisible(true); assembler.jButtonStop.setText("Stop");  assembler.jButtonStop.doClick(); }
            setExecutionState("STOPPED");
        }));
        bar.add(makeBtn("Reset",     "↺",  e -> {
            if (assembler.jMenuItemClearMemory != null) assembler.jMenuItemClearMemory.doClick();
            resetAssembleStatus();
            setExecutionState("STOPPED");
        }));
        bar.add(makeBtn("Autocorrect","✨", e -> { showEditorView(); if (assembler.jButtonAutocorrect != null) assembler.jButtonAutocorrect.doClick(); }));
        return bar;
    }

    private void styleRunBtn(JButton b, Color bg, Color fg) {
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(fg);
        b.setBackground(bg);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_PRIMARY_BLUE, 1, true),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void addSep(JPanel bar) {
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 22));
        sep.setForeground(COLOR_CARD_BORDER);
        bar.add(sep);
    }

    private JButton makeBtn(String text, String icon, java.awt.event.ActionListener l) {
        JButton btn = new JButton(icon + "  " + text);
        btn.setFont(getIconFont(12, Font.PLAIN));
        btn.setForeground(COLOR_TEXT_PRIMARY);
        btn.setBackground(COLOR_BG_CARD);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_CARD_BORDER, 1, true),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(l);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    if (btn == btnAssembleBar) {
                        Color hoverBg = isDarkMode ? new Color(0x1F, 0x22, 0x2C) : new Color(0xF1, 0xF5, 0xF9);
                        if (currentAssembleStatus == AssembleStatus.SUCCESS) {
                            btn.setBackground(new Color(0x16, 0xA3, 0x4A));
                        } else if (currentAssembleStatus == AssembleStatus.ERROR) {
                            btn.setBackground(new Color(0xDC, 0x26, 0x26));
                        } else {
                            btn.setBackground(hoverBg);
                        }
                    } else {
                        btn.setBackground(isDarkMode ? new Color(0x1F, 0x22, 0x2C) : new Color(0xF1, 0xF5, 0xF9));
                    }
                }
            }
            @Override public void mouseExited(MouseEvent e)  {
                if (btn.isEnabled()) {
                    if (btn == btnAssembleBar) {
                        updateAssembleButtonStyle();
                    } else {
                        btn.setBackground(COLOR_BG_CARD);
                    }
                }
            }
        });
        return btn;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ACTIVITY SIDEBAR
    // ════════════════════════════════════════════════════════════════════════
    private JPanel createActivitySidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(COLOR_BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(72, 600));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_CARD_BORDER));

        String[][] items = {
            {"Editor",       "📄"},
            {"Registers",    "💻"},
            {"Memory",       "▦" },
            {"Devices",      "🩺"},
            {"Subroutine",   "🔀"},
            {"Interrupts",   "🔔"},
            {"Disassembler", "{}"},
            {"Settings",     "⚙"}
        };

        for (String[] item : items) {
            String id   = item[0];
            String icon = item[1];
            JPanel btn  = buildSidebarBtn(id, icon);
            sidebarButtons.put(id, btn);
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(2));
        }
        return sidebar;
    }

    private JPanel buildSidebarBtn(String id, String icon) {
        JPanel btn = new JPanel(new BorderLayout());
        btn.setMaximumSize(new Dimension(72, 56));
        btn.setPreferredSize(new Dimension(72, 56));
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boolean active = id.equals(activeSidebarItem);
        btn.setBackground(active ? COLOR_PRIMARY_BLUE : COLOR_BG_SIDEBAR);

        JLabel lblIcon = new JLabel(icon, SwingConstants.CENTER);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        lblIcon.setForeground(active ? Color.WHITE : COLOR_TEXT_MUTED);
        lblIcon.setOpaque(false);
        lblIcon.setToolTipText(id);

        String display = id.length() > 9 ? id.substring(0, 8) + "." : id;
        JLabel lblText = new JLabel(display, SwingConstants.CENTER);
        lblText.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblText.setForeground(active ? Color.WHITE : COLOR_TEXT_MUTED);
        lblText.setOpaque(false);

        JPanel inner = new JPanel(new BorderLayout(0, 2));
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createEmptyBorder(6, 2, 4, 2));
        inner.add(lblIcon, BorderLayout.CENTER);
        inner.add(lblText, BorderLayout.SOUTH);
        btn.add(inner, BorderLayout.CENTER);

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                activeSidebarItem = id;
                updateSidebarSelection();
                handleSidebarAction(id);
            }
            @Override public void mouseEntered(MouseEvent e) {
                if (!id.equals(activeSidebarItem)) btn.setBackground(isDarkMode ? new Color(0x19, 0x1C, 0x25) : new Color(0xF1, 0xF5, 0xF9));
            }
            @Override public void mouseExited(MouseEvent e)  {
                if (!id.equals(activeSidebarItem)) btn.setBackground(COLOR_BG_SIDEBAR);
            }
        });
        return btn;
    }

    private void updateSidebarSelection() {
        for (Map.Entry<String, JPanel> entry : sidebarButtons.entrySet()) {
            boolean active = entry.getKey().equals(activeSidebarItem);
            JPanel  sBtn   = entry.getValue();
            sBtn.setBackground(active ? COLOR_PRIMARY_BLUE : COLOR_BG_SIDEBAR);
            for (Component c : sBtn.getComponents()) {
                if (c instanceof JPanel) {
                    for (Component inner : ((JPanel) c).getComponents()) {
                        if (inner instanceof JLabel) {
                            JLabel lbl = (JLabel) inner;
                            lbl.setForeground(active ? Color.WHITE : COLOR_TEXT_MUTED);
                        }
                    }
                }
            }
        }
    }

    public static Font getIconFont(int size, int style) {
        String[] families = {"Segoe UI Emoji", "Segoe UI Symbol", "Segoe UI", "Dialog"};
        for (String f : families) {
            Font font = new Font(f, style, size);
            if (font.canDisplay('⚙')) {
                return font;
            }
        }
        return new Font("Segoe UI", style, size);
    }

    private static boolean hasIconPrefix(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        char first = text.trim().charAt(0);
        return !(Character.isLetterOrDigit(first) || first == '(' || first == '[' || first == '{');
    }

    private static String getIconForButton(String text) {
        if (text == null) return "";
        String t = text.toLowerCase();
        if (t.contains("assemble")) return "⚙";
        if (t.contains("disassemble")) return "🔍";
        if (t.contains("autocorrect")) return "✨";
        if (t.contains("anal")) return "📊";
        if (t.contains("run")) return "▶";
        if (t.contains("back")) return "⏮";
        if (t.contains("fwd") || t.contains("forward")) return "⏭";
        if (t.contains("step")) return "⏯";
        if (t.contains("pause")) return "⏸";
        if (t.contains("stop")) return "⏹";
        if (t.contains("reset") || t.contains("clear")) return "↺";
        if (t.contains("insert") || t.contains("add") || t.contains("create")) return "➕";
        if (t.contains("cancel") || t.contains("close")) return "✖";
        if (t.contains("help")) return "❓";
        if (t.contains("set") || t.contains("sec") || t.contains("speed")) return "⏱";
        if (t.contains("open") || t.contains("load")) return "📂";
        if (t.contains("save")) return "💾";
        if (t.contains("new")) return "📄";
        return "⚡";
    }

    public static void applyObsidianTheme(Component comp) {
        if (comp == null) return;

        Color bgDark       = COLOR_BG_DARK;
        Color bgCard       = COLOR_BG_CARD;
        Color border       = COLOR_CARD_BORDER;
        Color primaryBlue  = COLOR_PRIMARY_BLUE;
        Color cyanAccent   = COLOR_CYAN_ACCENT;
        Color textPrimary  = COLOR_TEXT_PRIMARY;
        Color textMuted    = COLOR_TEXT_MUTED;

        if (comp instanceof RootPaneContainer) {
            RootPaneContainer r = (RootPaneContainer) comp;
            r.getContentPane().setBackground(bgDark);
            applyObsidianTheme(r.getContentPane());
            return;
        }

        if (comp instanceof JPanel) {
            comp.setBackground(bgDark);
        } else if (comp instanceof JRadioButton || comp instanceof JCheckBox) {
            comp.setBackground(bgDark);
            comp.setForeground(textPrimary);
            comp.setFont(getIconFont(12, Font.PLAIN));
            if (comp instanceof JRadioButton) ((JRadioButton) comp).setOpaque(false);
            if (comp instanceof JCheckBox) ((JCheckBox) comp).setOpaque(false);
        } else if (comp instanceof JTable) {
            JTable table = (JTable) comp;
            table.setBackground(bgCard);
            table.setForeground(textPrimary);
            table.setGridColor(border);
            table.setSelectionBackground(primaryBlue);
            table.setSelectionForeground(Color.WHITE);
            table.setFont(new Font("Consolas", Font.PLAIN, 12));
            table.setRowHeight(24);
            if (table.getTableHeader() != null) {
                table.getTableHeader().setBackground(isDarkMode ? new Color(0x11, 0x13, 0x18) : new Color(0xF1, 0xF5, 0xF9));
                table.getTableHeader().setForeground(cyanAccent);
                table.getTableHeader().setFont(getIconFont(12, Font.BOLD));
            }
            javax.swing.table.DefaultTableCellRenderer darkRenderer = new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (isSelected) {
                        c.setBackground(primaryBlue);
                        c.setForeground(Color.WHITE);
                    } else {
                        c.setBackground(row % 2 == 0 ? bgCard : (isDarkMode ? new Color(0x19, 0x1C, 0x26) : new Color(0xF8, 0xFA, 0xFC)));
                        c.setForeground(textPrimary);
                    }
                    if (c instanceof JLabel) {
                        ((JLabel) c).setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                    }
                    return c;
                }
            };
            for (int i = 0; i < table.getColumnCount(); i++) {
                table.getColumnModel().getColumn(i).setCellRenderer(darkRenderer);
            }
        } else if (comp instanceof JTabbedPane) {
            JTabbedPane tab = (JTabbedPane) comp;
            tab.setBackground(bgDark);
            tab.setForeground(textPrimary);
            tab.setFont(getIconFont(12, Font.BOLD));
        } else if (comp instanceof JTextArea) {
            JTextArea ta = (JTextArea) comp;
            ta.setBackground(bgCard);
            ta.setForeground(textPrimary);
            ta.setCaretColor(cyanAccent);
            ta.setFont(getIconFont(12, Font.PLAIN));
            ta.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(border, 1, true),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
        } else if (comp instanceof JTextField) {
            JTextField tf = (JTextField) comp;
            tf.setBackground(bgCard);
            tf.setForeground(textPrimary);
            tf.setCaretColor(cyanAccent);
            tf.setFont(getIconFont(12, Font.PLAIN));
            tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(border, 1, true),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
        } else if (comp instanceof JComboBox) {
            JComboBox<?> cb = (JComboBox<?>) comp;
            cb.setBackground(bgCard);
            cb.setForeground(textPrimary);
            cb.setFont(getIconFont(12, Font.PLAIN));
        } else if (comp instanceof JButton) {
            JButton btn = (JButton) comp;
            btn.setBackground(primaryBlue);
            btn.setForeground(Color.WHITE);
            btn.setFont(getIconFont(12, Font.BOLD));
            btn.setFocusPainted(false);
            btn.setOpaque(true);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(border, 1, true),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
            ));

            String txt = btn.getText();
            if (txt != null && !txt.trim().isEmpty()) {
                if (!hasIconPrefix(txt)) {
                    String icon = getIconForButton(txt);
                    if (icon != null && !icon.isEmpty()) {
                        btn.setText(icon + "  " + txt);
                    }
                }
            }
        } else if (comp instanceof JLabel) {
            JLabel lbl = (JLabel) comp;
            lbl.setForeground(textPrimary);
            lbl.setFont(getIconFont(lbl.getFont().getSize(), lbl.getFont().getStyle()));
        } else if (comp instanceof JSeparator) {
            comp.setForeground(border);
            comp.setBackground(border);
        } else if (comp instanceof JScrollPane) {
            JScrollPane sp = (JScrollPane) comp;
            sp.setBackground(bgDark);
            sp.getViewport().setBackground(bgDark);
            sp.setBorder(new LineBorder(border, 1));
        }

        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                applyObsidianTheme(child);
            }
        }
    }

    private JDialog registersDialog;
    private JDialog memoryDialog;
    private JDialog devicesDialog;

    private JDialog createStyledToolsWindow(String titleText, Component viewContent, int w, int h) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), titleText, Dialog.ModalityType.MODELESS);
        dialog.setSize(w, h);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_BG_HEADER);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_CARD_BORDER),
            new EmptyBorder(10, 16, 10, 16)
        ));

        JLabel titleLbl = new JLabel("⚙  " + titleText);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(COLOR_TEXT_PRIMARY);

        header.add(titleLbl, BorderLayout.WEST);
        dialog.add(header, BorderLayout.NORTH);

        JPanel contentContainer = new JPanel(new BorderLayout());
        contentContainer.setBackground(COLOR_BG_DARK);
        contentContainer.setBorder(new EmptyBorder(10, 10, 10, 10));

        if (viewContent != null) {
            contentContainer.add(viewContent, BorderLayout.CENTER);
        }
        dialog.add(contentContainer, BorderLayout.CENTER);
        applyObsidianTheme(dialog);

        return dialog;
    }

    private void showRegistersWindow() {
        if (registersDialog == null) {
            Component content = assembler.getRegistersInternalFrame() != null ?
                assembler.getRegistersInternalFrame().getContentPane() : new JPanel();
            JScrollPane scrollPane = new JScrollPane(content);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
            scrollPane.setBorder(null);
            registersDialog = createStyledToolsWindow("Registers Inspector", scrollPane, 750, 580);
        }
        applyObsidianTheme(registersDialog);
        registersDialog.setVisible(true);
        registersDialog.toFront();
    }

    private void showMemoryWindow() {
        if (memoryDialog == null) {
            Component content = assembler.getMemoryInternalFrame() != null ?
                assembler.getMemoryInternalFrame().getContentPane() : new JPanel();
            memoryDialog = createStyledToolsWindow("Memory Inspector", content, 750, 560);
        }
        applyObsidianTheme(memoryDialog);
        memoryDialog.setVisible(true);
        memoryDialog.toFront();
    }

    private void showDevicesWindow() {
        if (devicesDialog == null) {
            Component content = assembler.getTabbedPaneInterface() != null ?
                assembler.getTabbedPaneInterface() :
                (assembler.getDevicesInternalFrame() != null ? assembler.getDevicesInternalFrame().getContentPane() : new JPanel());
            devicesDialog = createStyledToolsWindow("I/O Inspector & Devices", content, 750, 540);
        }
        applyObsidianTheme(devicesDialog);
        devicesDialog.setVisible(true);
        devicesDialog.toFront();
    }

    private void handleSidebarAction(String id) {
        switch (id) {
            case "Editor":       showEditorView(); break;
            case "Registers":    showRegistersWindow(); break;
            case "Memory":       showMemoryWindow();    break;
            case "Devices":      showDevicesWindow();   break;
            case "Subroutine":   assembler.openDelaySubroutine();     break;
            case "Interrupts":   assembler.openInterruptSubroutine(); break;
            case "Disassembler":
                showEditorView();
                if (assembler.jTabbedPaneAssemblerEditor != null) assembler.jTabbedPaneAssemblerEditor.setSelectedIndex(1);
                break;
            case "Settings":     assembler.openSettings();            break;
            default:             showEditorView();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // EDITOR CONTAINER
    // ════════════════════════════════════════════════════════════════════════
    private JPanel createEditorContainer() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(COLOR_BG_DARK);

        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabBar.setBackground(COLOR_BG_HEADER);
        tabBar.setPreferredSize(new Dimension(800, 34));
        tabBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_CARD_BORDER));

        JPanel activeTabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 7));
        activeTabPanel.setBackground(COLOR_BG_DARK);
        activeTabPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, COLOR_PRIMARY_BLUE));

        JLabel tabFileIcon = new JLabel("📄 ");
        tabFileIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));

        unsavedDot = new JLabel("* ");
        unsavedDot.setFont(new Font("Segoe UI", Font.BOLD, 10));
        unsavedDot.setForeground(COLOR_CYAN_ACCENT);
        unsavedDot.setVisible(false);

        tabTitle = new JLabel("untitled.asm ");
        tabTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabTitle.setForeground(COLOR_TEXT_PRIMARY);

        JLabel closeBtn = new JLabel("✕");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        closeBtn.setForeground(COLOR_TEXT_MUTED);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    int c = JOptionPane.showConfirmDialog(assembler,
                        "Close this file? Unsaved changes will be lost.", "Close File",
                        JOptionPane.YES_NO_OPTION);
                    if (c == JOptionPane.YES_OPTION) {
                        assembler.jTextAreaAssemblyLanguageEditor.setText("");
                        tabTitle.setText("untitled.asm ");
                        unsavedDot.setVisible(false);
                        showWelcomeView();
                    }
                }
            }
            @Override public void mouseEntered(MouseEvent e) { closeBtn.setForeground(COLOR_RED_ACCENT); }
            @Override public void mouseExited(MouseEvent e)  { closeBtn.setForeground(COLOR_TEXT_MUTED);  }
        });

        activeTabPanel.add(tabFileIcon);
        activeTabPanel.add(unsavedDot);
        activeTabPanel.add(tabTitle);
        activeTabPanel.add(closeBtn);
        tabBar.add(activeTabPanel);

        JLabel addTabBtn = new JLabel("  ➕  ");
        addTabBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        addTabBtn.setForeground(COLOR_TEXT_MUTED);
        addTabBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addTabBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                assembler.jTextAreaAssemblyLanguageEditor.setText("; New Program\n\nMVI A, 00H\nHLT\n");
                assembler.textEditor.colorEditor();
                setTabTitle("untitled.asm");
                showEditorView();
            }
            @Override public void mouseEntered(MouseEvent e) { addTabBtn.setForeground(COLOR_TEXT_PRIMARY); }
            @Override public void mouseExited(MouseEvent e)  { addTabBtn.setForeground(COLOR_TEXT_MUTED); }
        });
        tabBar.add(addTabBtn);

        container.add(tabBar, BorderLayout.NORTH);

        editorCardLayout      = new CardLayout();
        centerEditorContainer = new JPanel(editorCardLayout);
        centerEditorContainer.setBackground(COLOR_BG_DARK);

        welcomeViewPanel = createWelcomeView();
        centerEditorContainer.add(welcomeViewPanel, "WELCOME");

        workspaceSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        workspaceSplitPane.setBorder(null);
        workspaceSplitPane.setBackground(COLOR_BG_DARK);
        
        if (assembler.jTabbedPaneAssemblerEditor != null) {
            assembler.jTabbedPaneAssemblerEditor.setBorder(null);
            assembler.jTabbedPaneAssemblerEditor.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
                @Override
                protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
                    return 0;
                }
            });
            workspaceSplitPane.setLeftComponent(assembler.jTabbedPaneAssemblerEditor);
            if (assembler.textEditor != null && assembler.textEditor.jTextPane1 != null) {
                assembler.textEditor.jTextPane1.setBackground(isDarkMode ? new Color(0x0E, 0x0F, 0x14) : Color.WHITE);
                assembler.textEditor.jTextPane1.setForeground(isDarkMode ? new Color(0xD4, 0xD4, 0xD4) : new Color(0x0F, 0x17, 0x2A));
                assembler.textEditor.jTextPane1.setCaretColor(isDarkMode ? Color.WHITE : new Color(0x0F, 0x17, 0x2A));
                assembler.textEditor.jTextPane1.setEditable(true);
                assembler.textEditor.jTextPane1.getDocument().addDocumentListener(
                    new javax.swing.event.DocumentListener() {
                        public void insertUpdate(javax.swing.event.DocumentEvent e)  { unsavedDot.setVisible(true); }
                        public void removeUpdate(javax.swing.event.DocumentEvent e)  { unsavedDot.setVisible(true); }
                        public void changedUpdate(javax.swing.event.DocumentEvent e) {}
                    });
            }
        }
        
        JPanel debuggerPanel = new JPanel(new BorderLayout());
        debuggerPanel.setBackground(COLOR_BG_DARK);
        JScrollPane ds = assembler.getDebuggerScrollPane();
        if (ds != null) {
            ds.setBorder(null);
            ds.setBackground(COLOR_BG_DARK);
            ds.getViewport().setBackground(COLOR_BG_DARK);
            debuggerExplainerLabel = new JLabel(
                "<html><b>Step Explainer:</b> Click Step Fwd to trace execution.</html>");
            debuggerExplainerLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
            debuggerExplainerLabel.setForeground(COLOR_CYAN_ACCENT);
            debuggerExplainerLabel.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
            debuggerExplainerLabel.setBackground(COLOR_BG_CARD);
            debuggerExplainerLabel.setOpaque(true);
            debuggerPanel.add(debuggerExplainerLabel, BorderLayout.NORTH);
            debuggerPanel.add(ds, BorderLayout.CENTER);
        }
        workspaceSplitPane.setRightComponent(debuggerPanel);
        workspaceSplitPane.setResizeWeight(1.0);
        
        centerEditorContainer.add(workspaceSplitPane, "WORKSPACE");
        editorCardLayout.show(centerEditorContainer, "WELCOME");
        container.add(centerEditorContainer, BorderLayout.CENTER);
        return container;
    }

    public void showEditorView() {
        if (editorCardLayout != null) editorCardLayout.show(centerEditorContainer, "WORKSPACE");
        if (workspaceSplitPane != null) {
            workspaceSplitPane.setDividerLocation(1.0);
        }
        if (assembler.jTabbedPaneAssemblerEditor != null) {
            assembler.jTabbedPaneAssemblerEditor.setSelectedIndex(0);
        }
        if (assembler.textEditor != null && assembler.textEditor.jTextPane1 != null)
            assembler.textEditor.jTextPane1.requestFocusInWindow();
    }
    public void showDebuggerView() {
        if (editorCardLayout != null) editorCardLayout.show(centerEditorContainer, "WORKSPACE");
        if (workspaceSplitPane != null) {
            workspaceSplitPane.setDividerLocation(0.65);
        }
    }
    public void showWelcomeView() {
        if (editorCardLayout != null) editorCardLayout.show(centerEditorContainer, "WELCOME");
        rebuildRecentList();
    }
    public void updateStepExplainer(String text) {
        if (debuggerExplainerLabel != null)
            debuggerExplainerLabel.setText("<html><b>Step Explainer:</b> " + text + "</html>");
    }
    public void setTabTitle(String f) { if (tabTitle != null) tabTitle.setText(f + " "); }
    public void markSaved()           { if (unsavedDot != null) unsavedDot.setVisible(false); }

    // ════════════════════════════════════════════════════════════════════════
    // RECENT FILES
    // ════════════════════════════════════════════════════════════════════════
    private void loadRecentFiles() {
        recentFiles.clear();
        try {
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
            String raw = prefs.get(PREFS_RECENT_KEY, "");
            if (!raw.isEmpty())
                for (String f : raw.split("\\|"))
                    if (!f.trim().isEmpty()) recentFiles.add(f.trim());
        } catch (Exception ignored) {}
    }
    private void saveRecentFiles() {
        try {
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
            prefs.put(PREFS_RECENT_KEY, String.join("|", recentFiles));
            prefs.flush();
        } catch (Exception ignored) {}
    }
    public void addRecentFile(String path) {
        recentFiles.remove(path);
        recentFiles.add(0, path);
        while (recentFiles.size() > MAX_RECENT) recentFiles.remove(recentFiles.size() - 1);
        saveRecentFiles();
        rebuildRecentList();
    }
    private void rebuildRecentList() {
        if (recentListPanel == null) return;
        recentListPanel.removeAll();
        if (recentFiles.isEmpty()) {
            JLabel empty = new JLabel("No recent files.");
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            empty.setForeground(COLOR_TEXT_MUTED);
            recentListPanel.add(empty);
        } else {
            for (String path : recentFiles) {
                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(false);
                row.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

                String name = new File(path).getName();
                JLabel item = new JLabel("📄  " + name);
                item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                item.setForeground(COLOR_TEXT_PRIMARY);
                item.setCursor(new Cursor(Cursor.HAND_CURSOR));

                JLabel chevron = new JLabel("❯");
                chevron.setFont(new Font("Segoe UI", Font.BOLD, 10));
                chevron.setForeground(COLOR_TEXT_MUTED);

                row.add(item, BorderLayout.WEST);
                row.add(chevron, BorderLayout.EAST);

                final String p = path;
                row.setCursor(new Cursor(Cursor.HAND_CURSOR));
                row.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { loadFilePath(p); }
                    @Override public void mouseEntered(MouseEvent e) {
                        item.setForeground(COLOR_CYAN_ACCENT);
                        chevron.setForeground(COLOR_CYAN_ACCENT);
                    }
                    @Override public void mouseExited(MouseEvent e)  {
                        item.setForeground(COLOR_TEXT_PRIMARY);
                        chevron.setForeground(COLOR_TEXT_MUTED);
                    }
                });
                recentListPanel.add(row);
            }
        }
        recentListPanel.revalidate();
        recentListPanel.repaint();
    }
    private void loadFilePath(String path) {
        File f = new File(path);
        if (!f.exists()) {
            JOptionPane.showMessageDialog(assembler, "File not found:\n" + path, "Error", JOptionPane.ERROR_MESSAGE);
            recentFiles.remove(path); saveRecentFiles(); rebuildRecentList(); return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
            }
            assembler.jTextAreaAssemblyLanguageEditor.setText(sb.toString());
            assembler.textEditor.colorEditor();
            setTabTitle(f.getName());
            markSaved();
            addRecentFile(path);
            showEditorView();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(assembler, "Failed to load: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // WELCOME VIEW
    // ════════════════════════════════════════════════════════════════════════
    private JPanel createWelcomeView() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(COLOR_BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 8, 10);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel logoPanel = new JLabel();
        try {
            java.awt.image.BufferedImage raw = javax.imageio.ImageIO.read(
                getClass().getResourceAsStream("/aura_logo.dat"));
            if (raw != null) {
                java.awt.Image scaled = raw.getScaledInstance(112, 112, java.awt.Image.SCALE_SMOOTH);
                logoPanel.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception ignored) {}
        logoPanel.setPreferredSize(new Dimension(112, 112));
        p.add(logoPanel, gbc);

        gbc.gridy++;
        JLabel t = new JLabel("Aura Studio");
        t.setFont(new Font("Segoe UI", Font.BOLD, 28));
        t.setForeground(COLOR_TEXT_PRIMARY);
        p.add(t, gbc);

        gbc.gridy++;
        JLabel s = new JLabel("8085 Microprocessor Simulator");
        s.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        s.setForeground(COLOR_TEXT_MUTED);
        p.add(s, gbc);

        gbc.gridy++;
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        btnRow.setOpaque(false);

        JButton btnNew = new JButton("➕  Create New Program");
        btnNew.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnNew.setForeground(Color.WHITE);
        btnNew.setBackground(COLOR_PRIMARY_BLUE);
        btnNew.setOpaque(true);
        btnNew.setFocusPainted(false);
        btnNew.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_PRIMARY_BLUE, 1, true),
            BorderFactory.createEmptyBorder(9, 20, 9, 20)));
        btnNew.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNew.addActionListener(e -> {
            assembler.jTextAreaAssemblyLanguageEditor.setText(
                "; Aura Studio 8085 Program\n\nMVI A, 05H\nMVI B, 03H\nADD B\nHLT\n");
            assembler.textEditor.colorEditor();
            setTabTitle("untitled.asm");
            showEditorView();
        });

        JButton btnOpen = mkWelBtn("📂  Open Program");
        btnOpen.addActionListener(e -> {
            if (assembler.jMenuItemLoad_Assembly_Language_code != null)
                assembler.jMenuItemLoad_Assembly_Language_code.doClick();
        });

        JButton btnSample = mkWelBtn("🧊  Open Sample");
        btnSample.addActionListener(e -> {
            assembler.jTextAreaAssemblyLanguageEditor.setText(
                "; 8085 Sample: Addition\nMVI A, 05H\nMVI B, 0AH\nADD B\nSTA C050H\nHLT\n");
            assembler.textEditor.colorEditor();
            setTabTitle("sample.asm");
            showEditorView();
        });

        btnRow.add(btnNew);
        btnRow.add(btnOpen);
        btnRow.add(btnSample);
        p.add(btnRow, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel cardsRow = new JPanel(new GridLayout(1, 2, 16, 0));
        cardsRow.setOpaque(false);
        cardsRow.setPreferredSize(new Dimension(640, 220));

        JPanel cardRecent = createCardPanel("Recent Files");
        JPanel recentHdr  = (JPanel) cardRecent.getComponent(0);
        JLabel btnClear   = new JLabel("Clear");
        btnClear.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClear.setForeground(COLOR_CYAN_ACCENT);
        btnClear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClear.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                recentFiles.clear(); saveRecentFiles(); rebuildRecentList();
            }
        });
        recentHdr.add(btnClear, BorderLayout.EAST);
        recentListPanel = new JPanel();
        recentListPanel.setLayout(new BoxLayout(recentListPanel, BoxLayout.Y_AXIS));
        recentListPanel.setOpaque(false);
        rebuildRecentList();
        cardRecent.add(recentListPanel, BorderLayout.CENTER);

        JPanel cardShort = createCardPanel("Helpful Shortcuts");
        JPanel scList    = new JPanel(new GridLayout(4, 2, 12, 6));
        scList.setOpaque(false);
        String[][] scs = {
            {"New Program",     "Ctrl + N"},
            {"Open Program",    "Ctrl + O"},
            {"Save Program",    "Ctrl + S"},
            {"Assemble",        "F5"},
            {"Run",             "F6"},
            {"Step",            "F7"},
            {"Reset",           "Ctrl + R"},
            {"Toggle Breakpoint","F9"}
        };
        for (String[] sc : scs) {
            JLabel nl = new JLabel(sc[0]);
            nl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            nl.setForeground(COLOR_TEXT_MUTED);

            JLabel kl = new JLabel(sc[1], SwingConstants.RIGHT);
            kl.setFont(new Font("Consolas", Font.BOLD, 11));
            kl.setForeground(COLOR_TEXT_PRIMARY);
            
            scList.add(nl);
            scList.add(kl);
        }
        cardShort.add(scList, BorderLayout.CENTER);

        cardsRow.add(cardRecent);
        cardsRow.add(cardShort);
        p.add(cardsRow, gbc);
        return p;
    }

    private JButton mkWelBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        b.setForeground(COLOR_TEXT_PRIMARY);
        b.setBackground(COLOR_BG_CARD);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_CARD_BORDER, 1, true),
            BorderFactory.createEmptyBorder(9, 18, 9, 18)));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ════════════════════════════════════════════════════════════════════════
    // DASHBOARD CARDS (Upper cards expand, Memory compact ~5-7 lines)
    // ════════════════════════════════════════════════════════════════════════
    private JPanel createDashboardCardsPanel() {
        JPanel c = new JPanel(new GridBagLayout());
        c.setBackground(COLOR_BG_DARK);
        c.setBorder(new EmptyBorder(6, 6, 6, 6));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(3, 3, 3, 3);

        // Balanced weights for dashboard cards (Memory showing 8-10 lines)
        g.gridy = 0; g.weighty = 0.26;
        g.gridx = 0; g.gridwidth = 1; g.weightx = 0.28; c.add(createRegistersCard(),  g);
        g.gridx = 1;                  g.weightx = 0.38; c.add(createFlagsCard(),      g);
        g.gridx = 2;                  g.weightx = 0.34; c.add(createPointersCard(),   g);

        g.gridy = 1; g.weighty = 0.26;
        g.gridx = 0; g.gridwidth = 2; g.weightx = 0.66; c.add(createExecutionCard(),  g);
        g.gridx = 2; g.gridwidth = 1; g.weightx = 0.34; c.add(createInterruptsCard(), g);

        g.gridy = 2; g.weighty = 0.22;
        g.gridx = 0; g.gridwidth = 2; g.weightx = 0.66; c.add(createSimRimCard(),   g);
        g.gridx = 2; g.gridwidth = 1; g.weightx = 0.34; c.add(createDevicesCard(), g);

        // Memory card: showing ~8-10 lines with scrollbar
        g.gridy = 3; g.weighty = 0.26;
        g.gridx = 0; g.gridwidth = 3; g.weightx = 1.0;  c.add(createMemoryCard(),   g);

        return c;
    }

    private JPanel createCardPanel(String title) {
        return createCardPanel(title, "REGISTERS", COLOR_CYAN_ACCENT);
    }

    private JPanel createCardPanel(String title, String iconType, Color iconColor) {
        JPanel card = new JPanel(new BorderLayout(0, 5));
        card.setBackground(COLOR_BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_CARD_BORDER, 1, true),
            new EmptyBorder(6, 8, 6, 8)));
        
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, isDarkMode ? new Color(0x22, 0x26, 0x33) : new Color(0xF1, 0xF5, 0xF9)));

        JLabel lbl = new JLabel("  " + title);
        lbl.setIcon(new CardHeaderIcon(iconType, iconColor));
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(COLOR_TEXT_PRIMARY);
        hdr.add(lbl, BorderLayout.WEST);
        
        card.add(hdr, BorderLayout.NORTH);
        return card;
    }

    private JPanel createRegistersCard() {
        JPanel card = createCardPanel("REGISTERS", "REGISTERS", COLOR_PRIMARY_BLUE);
        JPanel grid = new JPanel(new GridLayout(8, 1, 0, 2));
        grid.setOpaque(false);
        String[] names = {"A", "B", "C", "D", "E", "H", "L", "M"};
        for (int i = 0; i < 8; i++) {
            JPanel row = new JPanel(new BorderLayout(4, 0));
            row.setOpaque(true);
            row.setBackground(i == 0 ? COLOR_ROW_ACTIVE : COLOR_BG_CARD);
            row.setBorder(new EmptyBorder(2, 5, 2, 5));
            
            JLabel n = new JLabel(names[i]);
            n.setFont(new Font("Segoe UI", Font.BOLD, 11));
            n.setForeground(COLOR_TEXT_PRIMARY);
            
            JLabel v = new JLabel("00", SwingConstants.RIGHT);
            v.setFont(new Font("Consolas", Font.BOLD, 11));
            v.setForeground(COLOR_TEXT_PRIMARY);
            
            row.add(n, BorderLayout.WEST);
            row.add(v, BorderLayout.EAST);
            regRowPanels[i] = row;
            regValLabels[i] = v;
            grid.add(row);
        }
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createFlagsCard() {
        JPanel card = createCardPanel("FLAGS", "FLAGS", COLOR_GREEN_ACCENT);
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
        topRow.setOpaque(false);
        JPanel botRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        botRow.setOpaque(false);

        String[] fn = {"S", "Z", "AC", "P", "CY"};
        for (int i = 0; i < 5; i++) {
            boolean initialActive = (i == 1 || i == 4);
            CircleFlagBadge c = new CircleFlagBadge(fn[i], initialActive);
            flagCircles[i] = c;
            
            if (i < 3) topRow.add(c);
            else botRow.add(c);
        }
        container.add(Box.createVerticalStrut(4));
        container.add(topRow);
        container.add(Box.createVerticalStrut(4));
        container.add(botRow);
        card.add(container, BorderLayout.CENTER);
        return card;
    }

    private JPanel createPointersCard() {
        JPanel card = createCardPanel("POINTERS", "POINTERS", COLOR_AMBER_ACCENT);
        JPanel grid = new JPanel(new GridLayout(5, 1, 0, 3));
        grid.setOpaque(false);
        lblPcVal = mkPtrRow(grid, "PC", "0000");
        lblSpVal = mkPtrRow(grid, "SP", "FFFF");
        lblHlVal = mkPtrRow(grid, "HL", "0000");
        lblBcVal = mkPtrRow(grid, "BC", "0000");
        lblDeVal = mkPtrRow(grid, "DE", "0000");
        card.add(grid, BorderLayout.CENTER);
        return card;
    }
    private JLabel mkPtrRow(JPanel p, String label, String def) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setOpaque(false);
        JLabel n = new JLabel(label);
        n.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        n.setForeground(COLOR_TEXT_PRIMARY);
        JLabel v = new JLabel(def, SwingConstants.RIGHT);
        v.setFont(new Font("Consolas", Font.BOLD, 11));
        v.setForeground(COLOR_TEXT_PRIMARY);
        row.add(n, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        p.add(row);
        return v;
    }

    private JPanel createExecutionCard() {
        JPanel card = createCardPanel("EXECUTION", "EXECUTION", COLOR_CYAN_ACCENT);
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 3, 3, 3);

        // Row 0: Current Instruction & Execution State
        gbc.gridy = 0;
        gbc.gridx = 0; gbc.weightx = 0.35;
        JLabel n1 = new JLabel("Cur Instr.");
        n1.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        n1.setForeground(COLOR_TEXT_MUTED);
        grid.add(n1, gbc);

        gbc.gridx = 1; gbc.weightx = 0.15;
        lblCurInstr = new JLabel("NOP", SwingConstants.RIGHT);
        lblCurInstr.setFont(new Font("Consolas", Font.BOLD, 10));
        lblCurInstr.setForeground(COLOR_CYAN_ACCENT);
        grid.add(lblCurInstr, gbc);

        gbc.gridx = 2; gbc.weightx = 0.35;
        JLabel n2 = new JLabel("Exec State");
        n2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        n2.setForeground(COLOR_TEXT_MUTED);
        grid.add(n2, gbc);

        gbc.gridx = 3; gbc.weightx = 0.15;
        lblExecStateBadge = new PillBadgeLabel("STOPPED", COLOR_GREEN_ACCENT, new Color(0x10, 0x35, 0x1D), COLOR_GREEN_ACCENT);
        grid.add(lblExecStateBadge, gbc);

        // Row 1: Program Counter & Instructions
        gbc.gridy = 1;
        gbc.gridx = 0; gbc.weightx = 0.35;
        JLabel n3 = new JLabel("Prog. Counter");
        n3.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        n3.setForeground(COLOR_TEXT_MUTED);
        grid.add(n3, gbc);

        gbc.gridx = 1; gbc.weightx = 0.15;
        lblPcExec = new JLabel("0000", SwingConstants.RIGHT);
        lblPcExec.setFont(new Font("Consolas", Font.BOLD, 10));
        lblPcExec.setForeground(COLOR_TEXT_PRIMARY);
        grid.add(lblPcExec, gbc);

        gbc.gridx = 2; gbc.weightx = 0.35;
        JLabel n4 = new JLabel("Instructions");
        n4.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        n4.setForeground(COLOR_TEXT_MUTED);
        grid.add(n4, gbc);

        gbc.gridx = 3; gbc.weightx = 0.15;
        lblInstrCount = new JLabel("0", SwingConstants.RIGHT);
        lblInstrCount.setFont(new Font("Consolas", Font.BOLD, 10));
        lblInstrCount.setForeground(COLOR_TEXT_PRIMARY);
        grid.add(lblInstrCount, gbc);

        // Row 2: Clock Cycles & Machine Cycles
        gbc.gridy = 2;
        gbc.gridx = 0; gbc.weightx = 0.35;
        JLabel n5 = new JLabel("Clock Cycles");
        n5.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        n5.setForeground(COLOR_TEXT_MUTED);
        grid.add(n5, gbc);

        gbc.gridx = 1; gbc.weightx = 0.15;
        lblCyclesCount = new JLabel("0", SwingConstants.RIGHT);
        lblCyclesCount.setFont(new Font("Consolas", Font.BOLD, 10));
        lblCyclesCount.setForeground(COLOR_TEXT_PRIMARY);
        grid.add(lblCyclesCount, gbc);

        gbc.gridx = 2; gbc.weightx = 0.35;
        JLabel n6 = new JLabel("Mach. Cycles");
        n6.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        n6.setForeground(COLOR_TEXT_MUTED);
        grid.add(n6, gbc);

        gbc.gridx = 3; gbc.weightx = 0.15;
        lblMCycles = new JLabel("0", SwingConstants.RIGHT);
        lblMCycles.setFont(new Font("Consolas", Font.BOLD, 10));
        lblMCycles.setForeground(COLOR_TEXT_PRIMARY);
        grid.add(lblMCycles, gbc);

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createInterruptsCard() {
        JPanel card = createCardPanel("INTERRUPTS", "INTERRUPTS", COLOR_RED_ACCENT);
        JPanel grid = new JPanel(new GridLayout(5, 1, 0, 2));
        grid.setOpaque(false);
        lblTrap  = mkIrqRow(grid, "TRAP"   );
        lblRst75 = mkIrqRow(grid, "RST 7.5");
        lblRst65 = mkIrqRow(grid, "RST 6.5");
        lblRst55 = mkIrqRow(grid, "RST 5.5");
        lblIntr  = mkIrqRow(grid, "INTR"   );
        card.add(grid, BorderLayout.CENTER);
        return card;
    }
    private JLabel mkIrqRow(JPanel p, String label) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setOpaque(false);
        JLabel n = new JLabel(label);
        n.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        n.setForeground(COLOR_TEXT_MUTED);
        JLabel v = new JLabel("✕ Disabled", SwingConstants.RIGHT);
        v.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        v.setForeground(COLOR_RED_ACCENT);
        row.add(n, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        p.add(row);
        return v;
    }

    private JPanel createSimRimCard() {
        JPanel card = createCardPanel("SIM / RIM", "SIM / RIM", COLOR_PRIMARY_BLUE);
        JPanel grid = new JPanel(new GridLayout(2, 1, 0, 3));
        grid.setOpaque(false);
        lblSimVal = mkPtrRow(grid, "SIM", "00");
        lblRimVal = mkPtrRow(grid, "RIM", "00");
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createDevicesCard() {
        JPanel card = createCardPanel("DEVICES", "DEVICES", COLOR_GREEN_ACCENT);
        JPanel grid = new JPanel(new GridLayout(1, 4, 2, 0));
        grid.setOpaque(false);
        String[] devNames = {"ACIA", "PPI", "8253", "8255"};
        for (int i = 0; i < 4; i++) {
            JPanel inner = new JPanel();
            inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
            inner.setOpaque(false);

            JLabel nameLbl = new JLabel(devNames[i], SwingConstants.CENTER);
            nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
            nameLbl.setForeground(COLOR_TEXT_PRIMARY);
            nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel statusLbl = new JLabel("Ready", SwingConstants.CENTER);
            statusLbl.setIcon(new GreenDotIcon());
            statusLbl.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            statusLbl.setForeground(COLOR_GREEN_ACCENT);
            statusLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            inner.add(nameLbl);
            inner.add(Box.createVerticalStrut(2));
            inner.add(statusLbl);
            grid.add(inner);
        }
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    // MEMORY Card: Showing ~5 to 7 lines with vertical scrollbar
    private JPanel createMemoryCard() {
        JPanel card = createCardPanel("MEMORY", "MEMORY", COLOR_CYAN_ACCENT);
        
        JPanel hdr = (JPanel) card.getComponent(0);
        JPanel rightIcons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightIcons.setOpaque(false);
        JLabel editIcon = new JLabel("✎");
        editIcon.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        editIcon.setForeground(COLOR_TEXT_MUTED);
        editIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JLabel refreshIcon = new JLabel("↻");
        refreshIcon.setFont(new Font("Segoe UI", Font.BOLD, 11));
        refreshIcon.setForeground(COLOR_TEXT_MUTED);
        refreshIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JLabel dotsIcon = new JLabel("⋮");
        dotsIcon.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dotsIcon.setForeground(COLOR_TEXT_MUTED);
        dotsIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));

        rightIcons.add(editIcon);
        rightIcons.add(refreshIcon);
        rightIcons.add(dotsIcon);
        hdr.add(rightIcons, BorderLayout.EAST);

        String[] cols = {"Address","00","01","02","03","04","05","06","07","08","09","0A","0B","0C","0D","0E","0F","ASCII"};
        memoryTableModel = new DefaultTableModel(cols, 16);
        memoryTable = new JTable(memoryTableModel) {
            @Override public boolean isCellEditable(int r, int c) { return c > 0 && c < 17; }
        };
        memoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        memoryTable.setBackground(COLOR_BG_CARD);
        memoryTable.setForeground(COLOR_TEXT_PRIMARY);
        memoryTable.setGridColor(COLOR_CARD_BORDER);
        memoryTable.setFont(new Font("Consolas", Font.PLAIN, 10));
        memoryTable.setRowHeight(19);

        JTableHeader th = memoryTable.getTableHeader();
        th.setBackground(COLOR_BG_CARD);
        th.setForeground(COLOR_TEXT_PRIMARY);
        th.setFont(new Font("Segoe UI", Font.BOLD, 10));
        th.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(COLOR_BG_CARD);
                setForeground(column == 0 ? COLOR_CYAN_ACCENT : COLOR_TEXT_PRIMARY);
                setFont(new Font("Segoe UI", Font.BOLD, 10));
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, COLOR_CARD_BORDER));
                return this;
            }
        });

        DefaultTableCellRenderer tcr = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                
                if (col == 0) {
                    setBackground(COLOR_BG_CARD);
                    setForeground(COLOR_CYAN_ACCENT);
                } 
                else if (col == 17) {
                    setBackground(COLOR_BG_CARD);
                    setForeground(COLOR_TEXT_MUTED);
                } 
                else if (row == 0 && col == 1) {
                    setBackground(COLOR_PRIMARY_BLUE);
                    setForeground(Color.WHITE);
                } else if (row == 1 && col == 6) {
                    setBackground(COLOR_AMBER_ACCENT);
                    setForeground(Color.BLACK);
                } else if (row == 3 && col == 11) {
                    setBackground(COLOR_AMBER_ACCENT);
                    setForeground(Color.BLACK);
                } else {
                    setBackground(COLOR_BG_CARD);
                    setForeground(COLOR_TEXT_PRIMARY);
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return this;
            }
        };
        
        for (int i = 0; i < memoryTable.getColumnCount(); i++)
            memoryTable.getColumnModel().getColumn(i).setCellRenderer(tcr);

        memoryTable.getColumnModel().getColumn(0).setPreferredWidth(48);
        for (int i = 1; i <= 16; i++) {
            memoryTable.getColumnModel().getColumn(i).setPreferredWidth(19);
        }
        memoryTable.getColumnModel().getColumn(17).setPreferredWidth(45);
        
        JScrollPane sp = new JScrollPane(memoryTable);
        sp.setBorder(new LineBorder(COLOR_CARD_BORDER, 1));
        sp.setBackground(COLOR_BG_CARD);
        sp.getViewport().setBackground(COLOR_BG_CARD);
        sp.setPreferredSize(new Dimension(380, 200)); // Displays ~8 to 10 lines visible + scrollbar
        sp.getHorizontalScrollBar().setUnitIncrement(12);
        sp.getVerticalScrollBar().setUnitIncrement(12);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    // STATUS BAR
    // ════════════════════════════════════════════════════════════════════════
    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(COLOR_BG_HEADER);
        bar.setPreferredSize(new Dimension(1200, 28));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_CARD_BORDER));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        left.setOpaque(false);
        JLabel gd = new JLabel(" Ready");
        gd.setIcon(new GreenDotIcon());
        gd.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gd.setForeground(COLOR_TEXT_PRIMARY);
        left.add(gd);
        bar.add(left, BorderLayout.WEST);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 4));
        center.setOpaque(false);
        
        statusInfoLabel = new JLabel("Ln 1, Col 1  |  UTF-8  |  8085 Assembly  |  Instructions: 0  |  Cycles: 0");
        statusInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusInfoLabel.setForeground(COLOR_TEXT_MUTED);

        statusStateBadge = new JLabel(" STOPPED ");
        statusStateBadge.setOpaque(true);
        statusStateBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        statusStateBadge.setForeground(COLOR_TEXT_PRIMARY);
        statusStateBadge.setBackground(COLOR_PRIMARY_BLUE);
        statusStateBadge.setBorder(new EmptyBorder(2, 8, 2, 8));

        statusThemeMetaLabel = new JLabel("Theme: " + (isDarkMode ? "Dark" : "Light") + "  |  Memory: 0 KB");
        JLabel rightMeta = statusThemeMetaLabel;
        rightMeta.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rightMeta.setForeground(COLOR_TEXT_MUTED);

        center.add(statusInfoLabel);
        center.add(statusStateBadge);
        center.add(rightMeta);
        bar.add(center, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        right.setOpaque(false);
        JLabel wifiIcon = new JLabel("📶  100%");
        wifiIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 11));
        wifiIcon.setForeground(COLOR_CYAN_ACCENT);
        right.add(wifiIcon);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    // ════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ════════════════════════════════════════════════════════════════════════
    public void setExecutionState(String state) {
        if (statusStateBadge == null) return;
        switch (state) {
            case "RUNNING":
                statusStateBadge.setText(" RUNNING ");
                statusStateBadge.setBackground(COLOR_GREEN_ACCENT);
                if (lblExecStateBadge != null) {
                    lblExecStateBadge.setText("RUNNING");
                    lblExecStateBadge.setColors(COLOR_GREEN_ACCENT, new Color(0x10, 0x35, 0x1D), COLOR_GREEN_ACCENT);
                }
                if (btnRunBar != null) {
                    btnRunBar.setText("⏸ Running");
                    styleRunBtn(btnRunBar, COLOR_GREEN_ACCENT, new Color(0, 30, 0));
                }
                break;
            case "PAUSED":
                statusStateBadge.setText(" PAUSED  ");
                statusStateBadge.setBackground(COLOR_AMBER_ACCENT);
                if (lblExecStateBadge != null) {
                    lblExecStateBadge.setText("PAUSED");
                    lblExecStateBadge.setColors(COLOR_AMBER_ACCENT, new Color(0x35, 0x2A, 0x10), COLOR_AMBER_ACCENT);
                }
                break;
            default:
                statusStateBadge.setText(" STOPPED ");
                statusStateBadge.setBackground(COLOR_PRIMARY_BLUE);
                if (lblExecStateBadge != null) {
                    lblExecStateBadge.setText("STOPPED");
                    lblExecStateBadge.setColors(COLOR_GREEN_ACCENT, new Color(0x10, 0x35, 0x1D), COLOR_GREEN_ACCENT);
                }
                if (btnRunBar != null) {
                    btnRunBar.setText("▶ Run   ▼");
                    styleRunBtn(btnRunBar, COLOR_PRIMARY_BLUE, Color.WHITE);
                }
        }
        statusStateBadge.repaint();
    }

    @Override public void updateUI() {
        super.updateUI();
        if (COLOR_BG_DARK != null) setBackground(COLOR_BG_DARK);
        reapplyColorScheme();
    }

    public void reapplyColorScheme() {
        try {
            setBackground(COLOR_BG_DARK);
            if (assembler != null && assembler.textEditor != null && assembler.textEditor.jTextPane1 != null) {
                assembler.textEditor.jTextPane1.setBackground(isDarkMode ? new Color(0x0E, 0x0F, 0x14) : Color.WHITE);
                assembler.textEditor.jTextPane1.setForeground(isDarkMode ? new Color(0xD4, 0xD4, 0xD4) : new Color(0x0F, 0x17, 0x2A));
                assembler.textEditor.jTextPane1.setCaretColor(isDarkMode ? Color.WHITE : new Color(0x0F, 0x17, 0x2A));
                assembler.textEditor.jTextPane1.setSelectionColor(isDarkMode ? COLOR_ROW_ACTIVE : new Color(0xDB, 0xEA, 0xFE));
                assembler.textEditor.jTextPane1.setSelectedTextColor(isDarkMode ? Color.WHITE : new Color(0x0F, 0x17, 0x2A));
                assembler.textEditor.colorEditor();
            }
            if (regValLabels != null) for (JLabel l : regValLabels) if (l != null) l.setForeground(COLOR_TEXT_PRIMARY);
            if (regRowPanels != null) for (int i = 0; i < regRowPanels.length; i++)
                if (regRowPanels[i] != null) regRowPanels[i].setBackground(i == 0 ? COLOR_ROW_ACTIVE : COLOR_BG_CARD);
            if (statusStateBadge != null) {
                statusStateBadge.setForeground(Color.WHITE);
                statusStateBadge.setBackground(COLOR_PRIMARY_BLUE);
                statusStateBadge.setOpaque(true);
            }
        } catch (Exception ignored) {}
    }

    public void refreshData() {
        if (assembler == null || assembler.matrix == null) return;
        Matrix m = assembler.matrix;

        int mVal = 0;
        try {
            int hl = ((m.H & 0xFF) << 8) | (m.L & 0xFF);
            if (hl >= 0 && hl < m.memory.length) mVal = m.memory[hl] & 0xFF;
        } catch (Exception ignored) {}
        int[] vals = {m.A&0xFF, m.B&0xFF, m.C&0xFF, m.D&0xFF, m.E&0xFF, m.H&0xFF, m.L&0xFF, mVal};
        for (int i = 0; i < 8; i++)
            if (regValLabels[i] != null) regValLabels[i].setText(assembler.engine.Dec2Hex2digit(vals[i]));

        int f = m.F;
        boolean[] flags = {(f&0x80)!=0, (f&0x40)!=0, (f&0x10)!=0, (f&0x04)!=0, (f&0x01)!=0};
        for (int i = 0; i < 5; i++) if (flagCircles[i] != null) {
            flagCircles[i].setActive(flags[i]);
        }

        if (lblPcVal != null) lblPcVal.setText(assembler.engine.Dec2Hex(m.PC));
        if (lblSpVal != null) lblSpVal.setText(assembler.engine.Dec2Hex(m.SP));
        if (lblHlVal != null) lblHlVal.setText(assembler.engine.Dec2Hex(((m.H&0xFF)<<8)|(m.L&0xFF)));
        if (lblBcVal != null) lblBcVal.setText(assembler.engine.Dec2Hex(((m.B&0xFF)<<8)|(m.C&0xFF)));
        if (lblDeVal != null) lblDeVal.setText(assembler.engine.Dec2Hex(((m.D&0xFF)<<8)|(m.E&0xFF)));

        if (lblPcExec     != null) lblPcExec.setText(assembler.engine.Dec2Hex(m.PC));
        if (lblInstrCount != null) lblInstrCount.setText(String.valueOf(m.instructionCounter));
        if (lblCyclesCount!= null) lblCyclesCount.setText(String.valueOf(m.clockCycleCounter));

        if (statusStateBadge != null) {
            if (assembler.stop) {
                statusStateBadge.setText(" STOPPED ");
                statusStateBadge.setBackground(COLOR_PRIMARY_BLUE);
            } else {
                statusStateBadge.setText(" RUNNING ");
                statusStateBadge.setBackground(COLOR_GREEN_ACCENT);
            }
        }
        if (statusInfoLabel != null)
            statusInfoLabel.setText("Ln 1, Col 1  |  UTF-8  |  8085 Assembly  |  Instructions: "
                + m.instructionCounter + "  |  Cycles: " + m.clockCycleCounter);

        if (memoryTableModel != null) {
            int base = m.beginAddress & 0xFFF0;
            for (int row = 0; row < 16; row++) {
                int rowAddr = base + row * 16;
                memoryTableModel.setValueAt(assembler.engine.Dec2Hex(rowAddr), row, 0);
                StringBuilder ascii = new StringBuilder();
                for (int col = 0; col < 16; col++) {
                    int addr = rowAddr + col;
                    int v    = m.memory[addr & 0xFFFF] & 0xFF;
                    memoryTableModel.setValueAt(assembler.engine.Dec2Hex2digit(v), row, col + 1);
                    ascii.append((v >= 32 && v <= 126) ? (char) v : '.');
                }
                memoryTableModel.setValueAt(ascii.toString(), row, 17);
            }
        }
    }
    public void toggleTheme() {
        isDarkMode = !isDarkMode;
        updateThemeColors();
        try {
            if (isDarkMode) {
                com.formdev.flatlaf.FlatDarkLaf.setup();
            } else {
                com.formdev.flatlaf.FlatLightLaf.setup();
            }
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w != null) {
                SwingUtilities.updateComponentTreeUI(w);
            } else {
                SwingUtilities.updateComponentTreeUI(this);
            }
        } catch (Exception ignored) {}

        if (statusThemeMetaLabel != null) {
            statusThemeMetaLabel.setText("Theme: " + (isDarkMode ? "Dark" : "Light") + "  |  Memory: 0 KB");
        }
        if (btnThemeHeader != null) {
            btnThemeHeader.setText(isDarkMode ? "☀️" : "🌙");
            btnThemeHeader.setToolTipText(isDarkMode ? "Switch to Light Theme" : "Switch to Dark Theme");
        }

        reapplyColorScheme();
        updateSidebarSelection();
        applyObsidianTheme(this);

        if (registersDialog != null) applyObsidianTheme(registersDialog);
        if (memoryDialog != null) applyObsidianTheme(memoryDialog);
        if (devicesDialog != null) applyObsidianTheme(devicesDialog);

        Window topWin = SwingUtilities.getWindowAncestor(this);
        if (topWin != null) {
            topWin.revalidate();
            topWin.repaint();
        }
        revalidate();
        repaint();
    }
}
