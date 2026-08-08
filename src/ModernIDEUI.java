import com.formdev.flatlaf.FlatLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class ModernIDEUI extends JPanel {

    // ── Colour Palette ────────────────────────────────────────────────────────
    public static final Color COLOR_BG_DARK      = new Color(0x0D, 0x0E, 0x11);
    public static final Color COLOR_BG_SIDEBAR   = new Color(0x12, 0x13, 0x16);
    public static final Color COLOR_BG_HEADER    = new Color(0x12, 0x13, 0x16);
    public static final Color COLOR_BG_TOOLBAR   = new Color(0x16, 0x17, 0x1C);
    public static final Color COLOR_BG_CARD      = new Color(0x16, 0x18, 0x1D);
    public static final Color COLOR_CARD_BORDER  = new Color(0x22, 0x24, 0x2B);
    public static final Color COLOR_PRIMARY_BLUE = new Color(0x00, 0x78, 0xD4);
    public static final Color COLOR_CYAN_ACCENT  = new Color(0x4F, 0xC3, 0xF7);
    public static final Color COLOR_AMBER_ACCENT = new Color(0xF5, 0x9E, 0x0B);
    public static final Color COLOR_GREEN_ACCENT = new Color(0x22, 0xC5, 0x5E);
    public static final Color COLOR_RED_ACCENT   = new Color(0xEF, 0x44, 0x44);
    public static final Color COLOR_TEXT_PRIMARY = new Color(0xFF, 0xFF, 0xFF);
    public static final Color COLOR_TEXT_MUTED   = new Color(0x8E, 0x92, 0x9E);
    public static final Color COLOR_ROW_ACTIVE   = new Color(0x00, 0x4B, 0x87);

    // ── Recent files persistence ───────────────────────────────────────────────
    private static final String PREFS_NODE       = "AuraStudio";
    private static final String PREFS_RECENT_KEY = "aura_recent_files";
    private static final int    MAX_RECENT       = 5;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Assembler assembler;

    // ── Multi-File Tab Management ──────────────────────────────────────────────
    public static class EditorTab {
        private String     title;
        private File       file;
        private TextEditor textEditor;
        private boolean    isModified = false;
        private JPanel     tabComponent;
        private JLabel     lblTitle;
        private JLabel     lblDot;
        private JLabel     btnClose;
        private javax.swing.event.DocumentListener docListener;

        public EditorTab(String title, File file, TextEditor textEditor) {
            this.title = title;
            this.file = file;
            this.textEditor = textEditor;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) {
            this.title = title;
            if (lblTitle != null) lblTitle.setText(title + " ");
        }
        public File getFile() { return file; }
        public void setFile(File file) {
            this.file = file;
            if (file != null) setTitle(file.getName());
        }
        public TextEditor getTextEditor() { return textEditor; }
        private String     assembleStatus = "NONE";
        private JLabel     lblBadge;

        public String getAssembleStatus() { return assembleStatus; }
        public void setAssembleStatus(String status) {
            this.assembleStatus = status;
            if (lblBadge != null) {
                if ("PASSED".equalsIgnoreCase(status)) {
                    lblBadge.setText("✓ ");
                    lblBadge.setForeground(COLOR_GREEN_ACCENT);
                    lblBadge.setVisible(true);
                } else if ("FAILED".equalsIgnoreCase(status)) {
                    lblBadge.setText("✕ ");
                    lblBadge.setForeground(COLOR_RED_ACCENT);
                    lblBadge.setVisible(true);
                } else {
                    lblBadge.setVisible(false);
                }
            }
        }

        private String     savedContent = "";

        public String getSavedContent() { return savedContent; }
        public void setSavedContent(String content) {
            this.savedContent = (content != null ? content : "");
            checkModifiedState();
        }

        public void checkModifiedState() {
            String current = (textEditor != null && textEditor.jTextPane1 != null) ? textEditor.jTextPane1.getText() : "";
            boolean mod = !current.equals(savedContent);
            setModified(mod);
        }

        public boolean isModified() { return isModified; }
        public void setModified(boolean mod) {
            this.isModified = mod;
            if (lblDot != null) lblDot.setVisible(mod);
            if (mod) setAssembleStatus("NONE");
        }
    }

    private final List<EditorTab> openTabs        = new ArrayList<>();
    private EditorTab             activeTab       = null;
    private JPanel                tabsHeaderPanel;
    private int                   untitledCounter  = 1;
    private boolean               isDebuggerVisible = false;

    private JPanel     centerEditorContainer;
    private CardLayout editorCardLayout;
    private JPanel     welcomeViewPanel;
    private JSplitPane workspaceSplitPane;

    private JLabel[] regValLabels  = new JLabel[8];
    private JPanel[] regRowPanels  = new JPanel[8];
    private JLabel[] flagCircles   = new JLabel[5];   // S Z AC P CY

    private JLabel lblPcVal, lblSpVal, lblHlVal, lblBcVal, lblDeVal;
    private JLabel lblCurInstr, lblExecState, lblPcExec, lblInstrCount, lblCyclesCount, lblMCycles;
    private JLabel lblTrap, lblRst75, lblRst65, lblRst55, lblIntr;
    private JLabel lblSimVal, lblRimVal;

    private JTable            memoryTable;
    private DefaultTableModel memoryTableModel;

    private JLabel statusStateBadge;
    private JLabel statusInfoLabel;

    private JLabel debuggerExplainerLabel;

    private JButton btnAssembleBar;
    private JButton btnRunBar;

    private String              activeSidebarItem = "Editor";
    private Map<String, JPanel> sidebarButtons    = new HashMap<>();

    private JPanel      recentListPanel;
    private final List<String> recentFiles = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────────────
    public ModernIDEUI(Assembler asm) {
        this.assembler = asm;
        if (this.assembler.jTextAreaAssemblyLanguageEditor != null) {
            this.assembler.jTextAreaAssemblyLanguageEditor.addCaretListener(new javax.swing.event.CaretListener() {
                public void caretUpdate(javax.swing.event.CaretEvent e) {
                    updateStatusBar();
                }
            });
        }

        loadRecentFiles();
        setLayout(new BorderLayout());
        setBackground(COLOR_BG_DARK);

        add(createTopHeaderPanel(), BorderLayout.NORTH);

        JPanel mainWorkspace = new JPanel(new BorderLayout());
        mainWorkspace.setBackground(COLOR_BG_DARK);
        mainWorkspace.add(createActivitySidebar(), BorderLayout.WEST);

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setBackground(COLOR_BG_DARK);
        centerSplit.setDividerSize(4);
        centerSplit.setResizeWeight(0.55);
        centerSplit.setBorder(null);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(COLOR_BG_DARK);
        centerPanel.add(createActionBar(),      BorderLayout.NORTH);
        centerPanel.add(createEditorContainer(),BorderLayout.CENTER);
        
        JPanel dashboard = createDashboardCardsPanel();
        centerPanel.setMinimumSize(new Dimension(200, 0));
        dashboard.setMinimumSize(new Dimension(200, 0));

        centerSplit.setLeftComponent(centerPanel);
        centerSplit.setRightComponent(dashboard);

        mainWorkspace.add(centerSplit, BorderLayout.CENTER);
        add(mainWorkspace, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        setupKeyboardShortcuts();

        // FIX #6 – open on Welcome screen, not untitled.asm
        SwingUtilities.invokeLater(this::showWelcomeView);
        refreshData();
    }

    // ════════════════════════════════════════════════════════════════════════
    // KEYBOARD SHORTCUTS (WHEN_IN_FOCUSED_WINDOW)
    // ════════════════════════════════════════════════════════════════════════
    private void setupKeyboardShortcuts() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Ctrl+N -> New Tab
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, menuMask), "shortcut_new");
        am.put("shortcut_new", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { newTab(); }
        });

        // Ctrl+O -> Open
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, menuMask), "shortcut_open");
        am.put("shortcut_open", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (assembler.jMenuItemLoad_Assembly_Language_code != null)
                    assembler.jMenuItemLoad_Assembly_Language_code.doClick();
            }
        });

        // Ctrl+S -> Save
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, menuMask), "shortcut_save");
        am.put("shortcut_save", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { saveActiveTab(); }
        });

        // Ctrl+W -> Close Tab
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, menuMask), "shortcut_close_tab");
        am.put("shortcut_close_tab", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (activeTab != null) closeTab(activeTab); }
        });

        // F5 -> Assemble
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "shortcut_assemble");
        am.put("shortcut_assemble", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { assembleActiveTab(); }
        });

        // F6 -> Run
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "shortcut_run");
        am.put("shortcut_run", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                showEditorView();
                if (assembler.jButtonRun != null) assembler.jButtonRun.doClick();
                setExecutionState("RUNNING");
            }
        });

        // F7 -> Step
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "shortcut_step");
        am.put("shortcut_step", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { stepForwardAction(); }
        });

        // F8 -> Autocorrect
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "shortcut_autocorrect");
        am.put("shortcut_autocorrect", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                showEditorView();
                if (assembler.jButtonAutocorrect != null) assembler.jButtonAutocorrect.doClick();
            }
        });

        // F9 -> Breakpoint / Memory Range
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "shortcut_breakpoint");
        am.put("shortcut_breakpoint", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (assembler.jMenuItem2 != null) assembler.jMenuItem2.doClick();
            }
        });

        // F3 -> Disassemble
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "shortcut_disassemble");
        am.put("shortcut_disassemble", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                showEditorView();
                if (assembler.jTabbedPaneAssemblerEditor != null) assembler.jTabbedPaneAssemblerEditor.setSelectedIndex(1);
                if (assembler.jButtonDisassemble != null) {
                    assembler.jButtonDisassemble.setVisible(true);
                    assembler.jButtonDisassemble.doClick();
                }
            }
        });

        // Ctrl+R -> Reset
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, menuMask), "shortcut_reset");
        am.put("shortcut_reset", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (assembler.jMenuItemClearMemory != null) assembler.jMenuItemClearMemory.doClick();
                if (assembler.jButtonStop != null) { assembler.jButtonStop.setText("Stop"); assembler.jButtonStop.doClick(); }
                btnAssembleBar.setText("⚒ Assemble");
                btnAssembleBar.setBackground(COLOR_BG_CARD);
                btnAssembleBar.setForeground(COLOR_TEXT_PRIMARY);
                btnAssembleBar.putClientProperty("FlatLaf.style", "");
                setExecutionState("STOPPED");
            }
        });

        // Ctrl+Tab -> Next Tab
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, menuMask), "shortcut_next_tab");
        am.put("shortcut_next_tab", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (openTabs.size() > 1 && activeTab != null) {
                    int idx = openTabs.indexOf(activeTab);
                    int nextIdx = (idx + 1) % openTabs.size();
                    selectTab(openTabs.get(nextIdx));
                }
            }
        });

        // Ctrl+Shift+Tab -> Prev Tab
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, menuMask | java.awt.event.InputEvent.SHIFT_DOWN_MASK), "shortcut_prev_tab");
        am.put("shortcut_prev_tab", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (openTabs.size() > 1 && activeTab != null) {
                    int idx = openTabs.indexOf(activeTab);
                    int prevIdx = (idx - 1 + openTabs.size()) % openTabs.size();
                    selectTab(openTabs.get(prevIdx));
                }
            }
        });
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

        // FIX #3 - Revert to old logo as requested
        JLabel logoIcon = new JLabel();
        try {
            java.awt.image.BufferedImage raw = javax.imageio.ImageIO.read(
                getClass().getResourceAsStream("/aura_logo.dat"));
            if (raw != null) {
                java.awt.Image scaled = raw.getScaledInstance(28, 28, java.awt.Image.SCALE_SMOOTH);
                logoIcon.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception ignored) {}
        logoIcon.setPreferredSize(new Dimension(28, 28));

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
            header.add(mb, BorderLayout.CENTER);
        }
        return header;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ACTION BAR
    // ════════════════════════════════════════════════════════════════════════
    private JPanel createActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        bar.setBackground(COLOR_BG_TOOLBAR);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_CARD_BORDER));

        bar.add(makeBtn("New",  "📄", e -> newTab()));
        bar.add(makeBtn("Open", "📂", e -> {
            if (assembler.jMenuItemLoad_Assembly_Language_code != null)
                assembler.jMenuItemLoad_Assembly_Language_code.doClick();
        }));
        bar.add(makeBtn("Save", "💾", e -> saveActiveTab()));
        addSep(bar);

        // FIX #5 – Assemble with animated colour feedback
        btnAssembleBar = makeBtn("Assemble", "⚒", e -> assembleActiveTab());
        bar.add(btnAssembleBar);
        
        JButton btnDisassemble = makeBtn("Disassemble", "↔", e -> {
            showEditorView(); // switches to WORKSPACE
            if (assembler.jTabbedPaneAssemblerEditor != null) assembler.jTabbedPaneAssemblerEditor.setSelectedIndex(1);
            if (assembler.jButtonDisassemble != null) { 
                assembler.jButtonDisassemble.setVisible(true); 
                assembler.jButtonDisassemble.doClick(); 
            }
        });
        bar.add(btnDisassemble);
        addSep(bar);

        // FIX #5 – Run with colour state
        btnRunBar = new JButton("▶ Run");
        styleRunBtn(btnRunBar, COLOR_PRIMARY_BLUE, Color.WHITE);
        btnRunBar.addActionListener(e -> {
            showEditorView();
            if (assembler.jButtonRun != null) assembler.jButtonRun.doClick();
            setExecutionState("RUNNING");
        });
        bar.add(btnRunBar);

        bar.add(makeBtn("Step",      "⏭", e -> stepForwardAction()));
        bar.add(makeBtn("Fwd",       "⏩", e -> stepForwardAction()));
        bar.add(makeBtn("Back",      "⏪", e -> stepBackwardAction()));
        bar.add(makeBtn("Debugger",  "🐞", e -> toggleDebuggerView()));
        bar.add(makeBtn("Pause",     "⏸", e -> {
            showDebuggerView();
            if (assembler.jButtonStop != null) { assembler.jButtonStop.setVisible(true); assembler.jButtonStop.setText("Pause"); assembler.jButtonStop.doClick(); }
            setExecutionState("PAUSED");
        }));
        bar.add(makeBtn("Stop",      "⏹",  e -> {
            if (assembler.jButtonStop != null) { assembler.jButtonStop.setVisible(true); assembler.jButtonStop.setText("Stop");  assembler.jButtonStop.doClick(); }
            setExecutionState("STOPPED");
        }));
        bar.add(makeBtn("Reset",     "↻",  e -> {
            if (assembler.jMenuItemClearMemory != null) assembler.jMenuItemClearMemory.doClick();
            if (assembler.jButtonStop != null) { assembler.jButtonStop.setText("Stop"); assembler.jButtonStop.doClick(); }
            btnAssembleBar.setText("⚒ Assemble");
            btnAssembleBar.setBackground(COLOR_BG_CARD);
            btnAssembleBar.setForeground(COLOR_TEXT_PRIMARY);
            btnAssembleBar.putClientProperty("FlatLaf.style", "");
            setExecutionState("STOPPED");
        }));
        bar.add(makeBtn("Fix",       "✨",  e -> { showEditorView(); if (assembler.jButtonAutocorrect != null) assembler.jButtonAutocorrect.doClick(); }));
        return bar;
    }

    private void styleRunBtn(JButton b, Color bg, Color fg) {
        b.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        b.setForeground(fg);
        b.setBackground(bg);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void addSep(JPanel bar) {
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 22));
        sep.setForeground(COLOR_CARD_BORDER);
        bar.add(sep);
    }

    // FIX #2 – Graphical unicode icons
    private JButton makeBtn(String text, String icon, java.awt.event.ActionListener l) {
        JButton btn = new JButton(icon + " " + text);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        btn.setForeground(COLOR_TEXT_PRIMARY);
        btn.setBackground(COLOR_BG_CARD);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_CARD_BORDER, 1, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(l);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(new Color(0x25, 0x27, 0x32));
            }
            @Override public void mouseExited(MouseEvent e)  {
                if (btn.isEnabled() && btn != btnAssembleBar) btn.setBackground(COLOR_BG_CARD);
            }
        });
        return btn;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ACTIVITY SIDEBAR  (FIX #1 + FIX #2)
    // ════════════════════════════════════════════════════════════════════════
    private JPanel createActivitySidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(COLOR_BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(68, 600));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_CARD_BORDER));

        // id, label, short-icon
        String[][] items = {
            {"Editor",       "📝"},
            {"Registers",    "▦" },
            {"Memory",       "⊞" },
            {"Devices",      "⌨" },
            {"Subroutine",   "↳" },
            {"Interrupts",   "⚡" }
        };

        for (String[] item : items) {
            String id   = item[0];
            String icon = item[1];
            JPanel btn  = buildSidebarBtn(id, icon);
            sidebarButtons.put(id, btn);
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(1));
        }
        return sidebar;
    }

    private JPanel buildSidebarBtn(String id, String icon) {
        JPanel btn = new JPanel(new BorderLayout());
        btn.setMaximumSize(new Dimension(68, 58));
        btn.setPreferredSize(new Dimension(68, 58));
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boolean active = id.equals(activeSidebarItem);
        btn.setBackground(active ? COLOR_PRIMARY_BLUE : COLOR_BG_SIDEBAR);

        JLabel lblIcon = new JLabel(icon, SwingConstants.CENTER);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        lblIcon.setForeground(active ? Color.WHITE : COLOR_CYAN_ACCENT);
        lblIcon.setOpaque(false);
        lblIcon.setToolTipText(id);

        String display = id.length() > 8 ? id.substring(0, 7) + "." : id;
        JLabel lblText = new JLabel(display, SwingConstants.CENTER);
        lblText.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblText.setForeground(active ? COLOR_TEXT_PRIMARY : COLOR_TEXT_MUTED);
        lblText.setOpaque(false);

        JPanel inner = new JPanel(new BorderLayout(0, 2));
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
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
                if (!id.equals(activeSidebarItem)) btn.setBackground(new Color(0x1E, 0x20, 0x27));
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
                            lbl.setForeground(active ? Color.WHITE
                                : (lbl.getFont().getSize() <= 9 ? COLOR_TEXT_MUTED : COLOR_CYAN_ACCENT));
                        }
                    }
                }
            }
        }
    }

    private JDialog toolsDialog;
    private void showToolsDialog(int tabIndex, String title) {
        if (assembler.jTabbedPaneMemory == null) return;
        if (toolsDialog == null) {
            toolsDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Tools", Dialog.ModalityType.MODELESS);
            toolsDialog.setSize(850, 700);
            toolsDialog.setLocationRelativeTo(this);
            toolsDialog.setLayout(new BorderLayout());
            
            javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(assembler.jTabbedPaneMemory);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            
            toolsDialog.add(scrollPane, BorderLayout.CENTER);
            toolsDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        }
        toolsDialog.setTitle(title);
        if (assembler.jTabbedPaneMemory.getTabCount() > tabIndex) {
            assembler.jTabbedPaneMemory.setSelectedIndex(tabIndex);
        }
        toolsDialog.setVisible(true);
        toolsDialog.toFront();
    }

    private JDialog ioPortDialog;
    private void showIOPortDialog() {
        if (assembler.getTabbedPaneInterface() == null) return;
        if (ioPortDialog == null) {
            ioPortDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "I/O Port Editor", java.awt.Dialog.ModalityType.MODELESS);
            ioPortDialog.setSize(500, 400);
            ioPortDialog.setLocationRelativeTo(this);
            ioPortDialog.setLayout(new BorderLayout());
            ioPortDialog.add(assembler.getTabbedPaneInterface(), BorderLayout.CENTER);
            ioPortDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        }
        assembler.openIOPortPanel();
        ioPortDialog.setVisible(true);
        ioPortDialog.toFront();
    }

    // FIX #1 – Pop up the panels in a separate container so they show correctly
    private void handleSidebarAction(String id) {
        switch (id) {
            case "Editor":       showEditorView(); break;
            case "Registers":    showToolsDialog(0, "Registers"); break;
            case "Memory":       showToolsDialog(1, "Memory");    break;
            case "Devices":      
                // Show Interfacing devices directly!
                if (assembler.getTabbedPaneInterface() != null) {
                    if (ioPortDialog == null) {
                        ioPortDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Interfacing Devices", java.awt.Dialog.ModalityType.MODELESS);
                        ioPortDialog.setSize(500, 400);
                        ioPortDialog.setLocationRelativeTo(this);
                        ioPortDialog.setLayout(new BorderLayout());
                        ioPortDialog.add(assembler.getTabbedPaneInterface(), BorderLayout.CENTER);
                        ioPortDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
                    }
                    assembler.openIOPortPanel();
                    ioPortDialog.setVisible(true);
                    ioPortDialog.toFront();
                }
                break;
            case "Subroutine":   assembler.openDelaySubroutine();     break;
            case "Interrupts":   assembler.openInterruptSubroutine(); break;
            default:             showEditorView();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // EDITOR CONTAINER  (CardLayout: WELCOME | EDITOR | DEBUGGER)
    // ════════════════════════════════════════════════════════════════════════
    private JPanel createEditorContainer() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(COLOR_BG_DARK);

        // Tab bar
        JPanel tabBar = new JPanel(new BorderLayout());
        tabBar.setBackground(COLOR_BG_HEADER);
        tabBar.setPreferredSize(new Dimension(800, 34));
        tabBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_CARD_BORDER));

        tabsHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabsHeaderPanel.setBackground(COLOR_BG_HEADER);

        JScrollPane tabScrollPane = new JScrollPane(tabsHeaderPanel);
        tabScrollPane.setBorder(null);
        tabScrollPane.setBackground(COLOR_BG_HEADER);
        tabScrollPane.getViewport().setBackground(COLOR_BG_HEADER);
        tabScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tabScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        tabScrollPane.addMouseWheelListener(e -> {
            JScrollBar scrollBar = tabScrollPane.getHorizontalScrollBar();
            if (scrollBar != null) {
                int amount = e.getWheelRotation() * 30;
                scrollBar.setValue(scrollBar.getValue() + amount);
            }
        });

        JButton btnAddTab = new JButton("+");
        btnAddTab.setFont(new Font("Consolas", Font.BOLD, 15));
        btnAddTab.setForeground(COLOR_TEXT_MUTED);
        btnAddTab.setBackground(COLOR_BG_HEADER);
        btnAddTab.setFocusPainted(false);
        btnAddTab.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        btnAddTab.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAddTab.setToolTipText("New File Tab");
        btnAddTab.addActionListener(e -> newTab());
        btnAddTab.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btnAddTab.setForeground(COLOR_CYAN_ACCENT); }
            @Override public void mouseExited(MouseEvent e)  { btnAddTab.setForeground(COLOR_TEXT_MUTED); }
        });

        tabBar.add(tabScrollPane, BorderLayout.CENTER);
        tabBar.add(btnAddTab, BorderLayout.EAST);
        container.add(tabBar, BorderLayout.NORTH);

        // Card layout
        editorCardLayout      = new CardLayout();
        centerEditorContainer = new JPanel(editorCardLayout);
        centerEditorContainer.setBackground(COLOR_BG_DARK);

        // WELCOME card
        welcomeViewPanel = createWelcomeView();
        centerEditorContainer.add(welcomeViewPanel, "WELCOME");

        // WORKSPACE card (Split Pane with Editor/Disassembler on Left, Debugger on Right)
        workspaceSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        workspaceSplitPane.setBorder(null);
        workspaceSplitPane.setBackground(COLOR_BG_DARK);
        
        // Left: Original Tabbed Pane (Assembler + Disassembler)
        if (assembler.jTabbedPaneAssemblerEditor != null) {
            assembler.jTabbedPaneAssemblerEditor.setBorder(null);
            // Completely hide the tab headers as requested by user
            assembler.jTabbedPaneAssemblerEditor.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
                @Override
                protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
                    return 0;
                }
            });
            workspaceSplitPane.setLeftComponent(assembler.jTabbedPaneAssemblerEditor);
        }
        
        // Right: Debugger Panel
        JPanel debuggerPanel = new JPanel(new BorderLayout());
        debuggerPanel.setBackground(COLOR_BG_DARK);
        javax.swing.JScrollPane ds = assembler.getDebuggerScrollPane();
        if (ds != null) {
            ds.setBorder(null);
            ds.setBackground(COLOR_BG_DARK);
            ds.getViewport().setBackground(COLOR_BG_DARK);

            JPanel dbgHeader = new JPanel(new BorderLayout());
            dbgHeader.setBackground(COLOR_BG_CARD);
            dbgHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_CARD_BORDER));

            debuggerExplainerLabel = new JLabel(
                "<html><b>Step Explainer:</b> Click Step Fwd to trace execution.</html>");
            debuggerExplainerLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            debuggerExplainerLabel.setForeground(COLOR_CYAN_ACCENT);
            debuggerExplainerLabel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

            JLabel closeDbgBtn = new JLabel(" [X] ");
            closeDbgBtn.setFont(new Font("Consolas", Font.BOLD, 12));
            closeDbgBtn.setForeground(COLOR_TEXT_MUTED);
            closeDbgBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeDbgBtn.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 16));
            closeDbgBtn.addMouseListener(new MouseAdapter() {
                @Override public void mouseReleased(MouseEvent e) { hideDebuggerView(); }
                @Override public void mouseEntered(MouseEvent e) { closeDbgBtn.setForeground(COLOR_RED_ACCENT); }
                @Override public void mouseExited(MouseEvent e)  { closeDbgBtn.setForeground(COLOR_TEXT_MUTED); }
            });

            dbgHeader.add(debuggerExplainerLabel, BorderLayout.CENTER);
            dbgHeader.add(closeDbgBtn, BorderLayout.EAST);
            debuggerPanel.add(dbgHeader, BorderLayout.NORTH);
            debuggerPanel.add(ds, BorderLayout.CENTER);
        }
        workspaceSplitPane.setRightComponent(debuggerPanel);
        
        // Hide right side by default
        workspaceSplitPane.setResizeWeight(1.0);
        
        centerEditorContainer.add(workspaceSplitPane, "WORKSPACE");

        // FIX #6 – start on welcome
        editorCardLayout.show(centerEditorContainer, "WELCOME");
        container.add(centerEditorContainer, BorderLayout.CENTER);
        return container;
    }

    // ── multi-tab manager logic ────────────────────────────────────────────────
    public EditorTab newTab() {
        String title = "untitled" + (untitledCounter == 1 ? "" : "-" + untitledCounter) + ".asm";
        untitledCounter++;
        String defaultCode = "; New 8085 Program\n\nMVI A, 00H\nHLT\n";
        return newTabWithContent(title, defaultCode, null);
    }

    public EditorTab newTabWithContent(String title, String content, File file) {
        TextEditor te = new TextEditor(assembler);
        te.jTextPane1.setBackground(new Color(0x1E, 0x1E, 0x1E));
        te.jTextPane1.setForeground(new Color(0xD4, 0xD4, 0xD4));
        te.jTextPane1.setCaretColor(Color.WHITE);
        te.jTextPane1.setEditable(true);
        if (content != null) {
            te.jTextPane1.setText(content);
            te.colorEditor();
        }

        EditorTab tab = new EditorTab(title, file, te);
        tab.savedContent = (content != null ? content : "");
        JPanel comp = createTabUIComponent(tab);
        openTabs.add(tab);

        if (tabsHeaderPanel != null) {
            tabsHeaderPanel.add(comp);
            tabsHeaderPanel.revalidate();
            tabsHeaderPanel.repaint();
        }

        tab.setModified(false);
        selectTab(tab);
        return tab;
    }

    public void openFile(File file) {
        if (file == null || !file.exists()) return;
        String absPath = file.getAbsolutePath();

        for (EditorTab t : openTabs) {
            if (t.getFile() != null && t.getFile().getAbsolutePath().equalsIgnoreCase(absPath)) {
                selectTab(t);
                return;
            }
        }

        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
            }
            String loadedText = sb.toString();
            EditorTab tab = newTabWithContent(file.getName(), loadedText, file);
            tab.setSavedContent(loadedText);
            tab.setModified(false);
            addRecentFile(absPath);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(assembler, "Failed to load file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void onFileSaved(File file) {
        if (activeTab != null && file != null) {
            activeTab.setFile(file);
            activeTab.setSavedContent(activeTab.getTextEditor().jTextPane1.getText());
            activeTab.setModified(false);
            assembler.path = file.getAbsolutePath();
            assembler.setTitle("AURA SIMULATOR - " + file.getAbsolutePath());
            addRecentFile(file.getAbsolutePath());
        }
    }

    public void selectTab(EditorTab tab) {
        if (tab == null) return;
        activeTab = tab;

        for (EditorTab t : openTabs) {
            if (t.tabComponent != null) {
                boolean active = (t == activeTab);
                t.tabComponent.setBackground(active ? COLOR_BG_DARK : COLOR_BG_HEADER);
                t.tabComponent.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_CARD_BORDER),
                    BorderFactory.createMatteBorder(0, 0, active ? 2 : 0, 0, active ? COLOR_PRIMARY_BLUE : COLOR_BG_HEADER)
                ));
                if (t.lblTitle != null) {
                    t.lblTitle.setForeground(active ? COLOR_TEXT_PRIMARY : COLOR_TEXT_MUTED);
                    t.lblTitle.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 12));
                }
            }
        }

        assembler.textEditor = tab.getTextEditor();
        assembler.jTextAreaAssemblyLanguageEditor = tab.getTextEditor().jTextPane1;
        assembler.path = (tab.getFile() != null ? tab.getFile().getAbsolutePath() : "");

        if (assembler.getEditorScrollPane() != null) {
            assembler.getEditorScrollPane().setViewportView(tab.getTextEditor().jTextPane1);
        }

        String pathOrTitle = tab.getFile() != null ? tab.getFile().getAbsolutePath() : tab.getTitle();
        assembler.setTitle("AURA SIMULATOR - " + pathOrTitle);

        tab.getTextEditor().colorEditor();
        tab.getTextEditor().runLinting();

        showEditorView();
    }

    public void closeTab(EditorTab tab) {
        if (tab == null) return;
        if (tab.isModified()) {
            int option = JOptionPane.showConfirmDialog(
                assembler,
                "Save changes to " + tab.getTitle() + " before closing?",
                "Close File",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );

            if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                return;
            }
            if (option == JOptionPane.YES_OPTION) {
                saveTab(tab);
                if (tab.isModified()) {
                    return;
                }
            }
        }

        int index = openTabs.indexOf(tab);
        openTabs.remove(tab);
        if (tabsHeaderPanel != null && tab.tabComponent != null) {
            tabsHeaderPanel.remove(tab.tabComponent);
            tabsHeaderPanel.revalidate();
            tabsHeaderPanel.repaint();
        }

        if (activeTab == tab) {
            if (!openTabs.isEmpty()) {
                int nextIndex = Math.min(index, openTabs.size() - 1);
                selectTab(openTabs.get(nextIndex));
            } else {
                activeTab = null;
                showWelcomeView();
            }
        }
    }

    public void assembleActiveTab() {
        if (activeTab == null) return;
        showDebuggerView();
        btnAssembleBar.setText("⏳ Assembling...");
        btnAssembleBar.setBackground(COLOR_AMBER_ACCENT);
        btnAssembleBar.setForeground(new Color(0x30, 0x20, 0x00));
        btnAssembleBar.setEnabled(false);

        SwingUtilities.invokeLater(() -> {
            if (assembler.jButtonAssemble != null) assembler.jButtonAssemble.doClick();
            showDebuggerView();
            SwingUtilities.invokeLater(() -> {
                btnAssembleBar.setEnabled(true);
                List<SyntaxLinter.LintError> errors = SyntaxLinter.lint(activeTab.getTextEditor().jTextPane1.getText());
                if (errors.isEmpty()) {
                    activeTab.setAssembleStatus("PASSED");
                    btnAssembleBar.setText("✔️ Assembled");
                    btnAssembleBar.setBackground(COLOR_GREEN_ACCENT);
                    btnAssembleBar.setForeground(new Color(0x00, 0x25, 0x00));
                } else {
                    activeTab.setAssembleStatus("FAILED");
                    btnAssembleBar.setText("❌ " + errors.size() + " Error(s)");
                    btnAssembleBar.setBackground(COLOR_RED_ACCENT);
                    btnAssembleBar.setForeground(Color.WHITE);
                }
            });
        });
    }

    public void assembleAllTabs() {
        if (openTabs.isEmpty()) return;
        int passed = 0;
        int failed = 0;

        for (EditorTab tab : openTabs) {
            List<SyntaxLinter.LintError> errors = SyntaxLinter.lint(tab.getTextEditor().jTextPane1.getText());
            if (errors.isEmpty()) {
                tab.setAssembleStatus("PASSED");
                passed++;
            } else {
                tab.setAssembleStatus("FAILED");
                failed++;
            }
        }

        if (failed == 0) {
            JOptionPane.showMessageDialog(assembler, "All " + passed + " open file(s) assembled successfully!", "Assemble All", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(assembler, "Assemble All Complete:\n" + passed + " passed, " + failed + " failed with syntax errors.", "Assemble All", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void saveActiveTab() {
        if (activeTab != null) {
            saveTab(activeTab);
        } else {
            newTab();
        }
    }

    public void saveTab(EditorTab tab) {
        if (tab == null) return;
        if (tab.getFile() != null) {
            try {
                String textToSave = tab.getTextEditor().jTextPane1.getText();
                try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(tab.getFile())))) {
                    out.print(textToSave);
                }
                tab.setSavedContent(textToSave);
                tab.setModified(false);
                addRecentFile(tab.getFile().getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(assembler, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            selectTab(tab);
            if (assembler.jMenuItemSave_Assembly_Language_code != null) {
                assembler.jMenuItemSave_Assembly_Language_code.doClick();
            }
        }
    }

    private JPanel createTabUIComponent(EditorTab tab) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        p.setOpaque(true);
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 1, COLOR_CARD_BORDER));

        JLabel dot = new JLabel("* ");
        dot.setFont(new Font("Segoe UI", Font.BOLD, 10));
        dot.setForeground(COLOR_CYAN_ACCENT);
        dot.setVisible(tab.isModified());

        JLabel lbl = new JLabel(tab.getTitle() + " ");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(COLOR_TEXT_PRIMARY);

        JLabel closeBtn = new JLabel(" [X] ");
        closeBtn.setFont(new Font("Consolas", Font.BOLD, 10));
        closeBtn.setForeground(COLOR_TEXT_MUTED);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        closeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    closeTab(tab);
                }
            }
            @Override public void mouseEntered(MouseEvent e) { closeBtn.setForeground(COLOR_RED_ACCENT); }
            @Override public void mouseExited(MouseEvent e)  { closeBtn.setForeground(COLOR_TEXT_MUTED); }
        });

        p.add(dot);
        p.add(lbl);
        p.add(closeBtn);

        p.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    selectTab(tab);
                }
            }
            @Override public void mouseEntered(MouseEvent e) {
                if (tab != activeTab) p.setBackground(new Color(0x1A, 0x1B, 0x20));
            }
            @Override public void mouseExited(MouseEvent e) {
                if (tab != activeTab) p.setBackground(COLOR_BG_HEADER);
            }
        });

        tab.lblDot = dot;
        tab.lblTitle = lbl;
        tab.btnClose = closeBtn;
        tab.tabComponent = p;

        if (tab.textEditor != null && tab.textEditor.jTextPane1 != null) {
            javax.swing.event.DocumentListener dl = new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e)  { SwingUtilities.invokeLater(tab::checkModifiedState); }
                public void removeUpdate(javax.swing.event.DocumentEvent e)  { SwingUtilities.invokeLater(tab::checkModifiedState); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { SwingUtilities.invokeLater(tab::checkModifiedState); }
            };
            tab.docListener = dl;
            tab.textEditor.jTextPane1.getDocument().addDocumentListener(dl);
        }

        return p;
    }

    // ── step & debugger helpers ────────────────────────────────────────────────
    public void stepForwardAction() {
        if (activeTab == null) return;
        showDebuggerView();

        if (!"PASSED".equalsIgnoreCase(activeTab.getAssembleStatus())) {
            if (assembler.jButtonAssemble != null) assembler.jButtonAssemble.doClick();
            activeTab.setAssembleStatus("PASSED");
        }

        if (assembler.stop || assembler.jButtonStep.isVisible()) {
            if (assembler.jButtonStep != null) {
                assembler.jButtonStep.doClick();
            }
        }

        if (assembler.jButtonForward != null) {
            assembler.jButtonForwardActionPerformed(null);
        }

        setExecutionState("STEPPING");
    }

    public void stepBackwardAction() {
        if (activeTab == null) return;
        showDebuggerView();

        if (assembler.jButtonBackward != null && assembler.jButtonBackward.isEnabled()) {
            assembler.jButtonBackwardActionPerformed(null);
        } else {
            JOptionPane.showMessageDialog(assembler, "Cannot step backward. No previous step history.", "Debugger", JOptionPane.INFORMATION_MESSAGE);
        }

        setExecutionState("STEPPING");
    }

    // ── card switch helpers ───────────────────────────────────────────────────
    public void showEditorView() {
        if (openTabs.isEmpty()) {
            newTab();
            return;
        }
        if (activeTab == null && !openTabs.isEmpty()) {
            selectTab(openTabs.get(0));
        }
        if (editorCardLayout != null) editorCardLayout.show(centerEditorContainer, "WORKSPACE");
        if (workspaceSplitPane != null) {
            workspaceSplitPane.setResizeWeight(isDebuggerVisible ? 0.45 : 1.0);
            if (isDebuggerVisible) {
                int totalWidth = workspaceSplitPane.getWidth();
                if (totalWidth > 0) {
                    workspaceSplitPane.setDividerLocation((int) (totalWidth * 0.45));
                } else {
                    workspaceSplitPane.setDividerLocation(0.45);
                }
            } else {
                workspaceSplitPane.setDividerLocation(1.0);
            }
        }
        if (assembler.jTabbedPaneAssemblerEditor != null) {
            assembler.jTabbedPaneAssemblerEditor.setSelectedIndex(0);
        }
        if (activeTab != null && activeTab.getTextEditor() != null && activeTab.getTextEditor().jTextPane1 != null) {
            activeTab.getTextEditor().jTextPane1.requestFocusInWindow();
        }
    }
    public void showDebuggerView() {
        isDebuggerVisible = true;
        if (editorCardLayout != null) editorCardLayout.show(centerEditorContainer, "WORKSPACE");
        if (workspaceSplitPane != null) {
            workspaceSplitPane.setResizeWeight(0.45);
            SwingUtilities.invokeLater(() -> {
                int totalWidth = workspaceSplitPane.getWidth();
                if (totalWidth > 0) {
                    workspaceSplitPane.setDividerLocation((int) (totalWidth * 0.45));
                } else {
                    workspaceSplitPane.setDividerLocation(0.45);
                }
            });
        }
    }
    public void hideDebuggerView() {
        isDebuggerVisible = false;
        if (workspaceSplitPane != null) {
            workspaceSplitPane.setDividerLocation(1.0);
        }
    }
    public void toggleDebuggerView() {
        if (isDebuggerVisible) hideDebuggerView();
        else showDebuggerView();
    }
    public void showWelcomeView() {
        if (editorCardLayout != null) editorCardLayout.show(centerEditorContainer, "WELCOME");
        rebuildRecentList();
    }
    public void updateStepExplainer(String text) {
        if (debuggerExplainerLabel != null)
            debuggerExplainerLabel.setText("<html><b>Step Explainer:</b> " + text + "</html>");
        // Also record in step log for practical export
        if (assembler != null && text != null && !text.trim().isEmpty()) {
            String plain = text.replaceAll("<[^>]+>", "").trim();
            if (!plain.isEmpty()) assembler.addStepLog(plain);
        }
    }
    public void setTabTitle(String f) {
        if (activeTab != null) activeTab.setTitle(f);
    }
    public void markSaved() {
        if (activeTab != null) activeTab.setModified(false);
    }

    // ════════════════════════════════════════════════════════════════════════
    // RECENT FILES  (FIX #4)
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
                String name = new File(path).getName();
                String disp = path.length() > 38 ? "..." + path.substring(path.length() - 30) : path;
                JLabel item = new JLabel(
                    "<html><b>" + name + "</b>&nbsp;&nbsp;<font color='#5E6270'>" + disp + "</font></html>");
                item.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                item.setForeground(COLOR_CYAN_ACCENT);
                item.setCursor(new Cursor(Cursor.HAND_CURSOR));
                item.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
                final String p = path;
                item.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { loadFilePath(p); }
                    @Override public void mouseEntered(MouseEvent e) { item.setForeground(Color.WHITE); }
                    @Override public void mouseExited(MouseEvent e)  { item.setForeground(COLOR_CYAN_ACCENT); }
                });
                recentListPanel.add(item);
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
        openFile(f);
    }

    // ════════════════════════════════════════════════════════════════════════
    // WELCOME VIEW  (FIX #3 logo, FIX #4 recent files, FIX #6 startup)
    // ════════════════════════════════════════════════════════════════════════
    private JPanel createWelcomeView() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(COLOR_BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.CENTER;

        // FIX #3 – Restore old logo here as well
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

        JButton btnNew = new JButton("➕ Create New Program");
        btnNew.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        btnNew.setForeground(Color.WHITE);
        btnNew.setBackground(COLOR_PRIMARY_BLUE);
        btnNew.setOpaque(true);
        btnNew.setFocusPainted(false);
        btnNew.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        btnNew.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNew.addActionListener(e -> newTab());

        JButton btnOpen = mkWelBtn("📂 Open Program");
        btnOpen.addActionListener(e -> {
            if (assembler.jMenuItemLoad_Assembly_Language_code != null)
                assembler.jMenuItemLoad_Assembly_Language_code.doClick();
        });

        JButton btnSample = mkWelBtn("✨ Open Sample");
        btnSample.addActionListener(e -> newTabWithContent(
            "sample.asm",
            "; 8085 Sample: Addition\nMVI A, 05H\nMVI B, 0AH\nADD B\nSTA C050H\nHLT\n",
            null
        ));

        btnRow.add(btnNew);
        btnRow.add(btnOpen);
        btnRow.add(btnSample);
        p.add(btnRow, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel cardsRow = new JPanel(new GridLayout(1, 2, 16, 0));
        cardsRow.setOpaque(false);
        cardsRow.setPreferredSize(new Dimension(640, 200));

        // FIX #4 – Recent files with persistent clear button
        JPanel cardRecent = createCardPanel("Recent Files");
        JPanel recentHdr  = (JPanel) cardRecent.getComponent(0);  // the header panel
        JButton btnClear  = new JButton("Clear");
        btnClear.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnClear.setForeground(COLOR_CYAN_ACCENT);
        btnClear.setBackground(COLOR_BG_CARD);
        btnClear.setOpaque(true);
        btnClear.setFocusPainted(false);
        btnClear.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        btnClear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClear.addActionListener(e -> { recentFiles.clear(); saveRecentFiles(); rebuildRecentList(); });
        recentHdr.add(btnClear, BorderLayout.EAST);
        recentListPanel = new JPanel();
        recentListPanel.setLayout(new BoxLayout(recentListPanel, BoxLayout.Y_AXIS));
        recentListPanel.setOpaque(false);
        rebuildRecentList();
        cardRecent.add(recentListPanel, BorderLayout.CENTER);

        // Shortcuts card
        JPanel cardShort = createCardPanel("Keyboard Shortcuts");
        JPanel scList    = new JPanel(new GridLayout(5, 2, 8, 5));
        scList.setOpaque(false);
        String[][] scs = {
            {"New",        "Ctrl+N"}, {"Open",     "Ctrl+O"},
            {"Save",       "Ctrl+S"}, {"Assemble", "F5"    },
            {"Run",        "F6"    }, {"Step",     "F7"    },
            {"Reset",      "Ctrl+R"}, {"Breakpoint","F9"   },
            {"Autocorrect","F8"    }, {"Disassemble","F3"  }
        };
        for (String[] sc : scs) {
            JLabel nl = new JLabel(sc[0]);
            nl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            nl.setForeground(COLOR_TEXT_MUTED);
            JLabel kl = new JLabel(sc[1], SwingConstants.RIGHT);
            kl.setFont(new Font("Consolas", Font.BOLD, 11));
            kl.setForeground(COLOR_CYAN_ACCENT);
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
    // DASHBOARD (right panel cards)
    // ════════════════════════════════════════════════════════════════════════
    private JPanel createDashboardCardsPanel() {
        JPanel c = new JPanel(new GridBagLayout());
        c.setBackground(COLOR_BG_DARK);
        c.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(4, 4, 4, 4);

        g.gridy = 0; g.weighty = 0.0;
        g.gridx = 0; g.gridwidth = 1; g.weightx = 0.33; c.add(createRegistersCard(),  g);
        g.gridx = 1;                  g.weightx = 0.33; c.add(createFlagsCard(),      g);
        g.gridx = 2;                  g.weightx = 0.34; c.add(createPointersCard(),   g);

        g.gridy = 1; g.weighty = 0.0;
        g.gridx = 0; g.gridwidth = 2; g.weightx = 0.66; c.add(createExecutionCard(),  g);
        g.gridx = 2; g.gridwidth = 1; g.weightx = 0.34; c.add(createInterruptsCard(), g);

        g.gridy = 2; g.weighty = 0.0;
        g.gridx = 0; g.gridwidth = 2; g.weightx = 0.66; c.add(createSimRimCard(),   g);
        g.gridx = 2; g.gridwidth = 1; g.weightx = 0.34; c.add(createDevicesCard(), g);

        g.gridy = 3; g.weighty = 1.0;
        g.gridx = 0; g.gridwidth = 3; g.weightx = 1.0;  c.add(createMemoryCard(),   g);
        return c;
    }

    private JPanel createCardPanel(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(COLOR_BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_CARD_BORDER, 1, true),
            new EmptyBorder(10, 12, 10, 12)));
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(COLOR_CYAN_ACCENT);
        hdr.add(lbl, BorderLayout.WEST);
        card.add(hdr, BorderLayout.NORTH);
        return card;
    }

    private JPanel createRegistersCard() {
        JPanel card = createCardPanel("REGISTERS");
        JPanel grid = new JPanel(new GridLayout(8, 1, 0, 2));
        grid.setOpaque(false);
        String[] names = {"A", "B", "C", "D", "E", "H", "L", "M"};
        for (int i = 0; i < 8; i++) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(true);
            row.setBackground(i == 0 ? COLOR_ROW_ACTIVE : COLOR_BG_CARD);
            row.setBorder(new EmptyBorder(2, 6, 2, 6));
            JLabel n = new JLabel(names[i]);
            n.setFont(new Font("Segoe UI", Font.BOLD, 12));
            n.setForeground(COLOR_TEXT_PRIMARY);
            JLabel v = new JLabel("00");
            v.setFont(new Font("Consolas", Font.BOLD, 12));
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
        JPanel card = createCardPanel("FLAGS");
        JPanel cp   = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 20));
        cp.setOpaque(false);
        String[] fn = {"S", "Z", "AC", "P", "CY"};
        for (int i = 0; i < 5; i++) {
            JLabel c = new JLabel(fn[i], SwingConstants.CENTER);
            c.setPreferredSize(new Dimension(36, 36));
            c.setOpaque(true);
            c.setFont(new Font("Segoe UI", Font.BOLD, 11));
            c.setForeground(COLOR_TEXT_PRIMARY);
            c.setBackground(COLOR_CARD_BORDER);
            c.setBorder(new LineBorder(COLOR_CARD_BORDER, 1, true));
            flagCircles[i] = c;
            cp.add(c);
        }
        card.add(cp, BorderLayout.CENTER);
        return card;
    }

    private JPanel createPointersCard() {
        JPanel card = createCardPanel("POINTERS");
        JPanel grid = new JPanel(new GridLayout(5, 1, 0, 4));
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
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel n = new JLabel(label);
        n.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        n.setForeground(COLOR_TEXT_MUTED);
        JLabel v = new JLabel(def);
        v.setFont(new Font("Consolas", Font.BOLD, 12));
        v.setForeground(COLOR_CYAN_ACCENT);
        row.add(n, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        p.add(row);
        return v;
    }

    private JPanel createExecutionCard() {
        JPanel card = createCardPanel("EXECUTION");
        JPanel grid = new JPanel(new GridLayout(3, 2, 8, 4));
        grid.setOpaque(false);
        lblCurInstr   = mkExItem(grid, "Current Instr",  "NOP"    );
        lblExecState  = mkExItem(grid, "State",          "STOPPED");
        lblPcExec     = mkExItem(grid, "PC Address",     "0000"   );
        lblInstrCount = mkExItem(grid, "Instructions",   "0"      );
        lblCyclesCount= mkExItem(grid, "Clock Cycles",   "0"      );
        lblMCycles    = mkExItem(grid, "M-Cycles",       "0"      );
        card.add(grid, BorderLayout.CENTER);
        return card;
    }
    private JLabel mkExItem(JPanel p, String label, String def) {
        JPanel box = new JPanel(new BorderLayout());
        box.setOpaque(false);
        JLabel n = new JLabel(label);
        n.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        n.setForeground(COLOR_TEXT_MUTED);
        JLabel v = new JLabel(def);
        v.setFont(new Font("Consolas", Font.BOLD, 11));
        v.setForeground(COLOR_CYAN_ACCENT);
        box.add(n, BorderLayout.WEST);
        box.add(v, BorderLayout.EAST);
        p.add(box);
        return v;
    }

    private JPanel createInterruptsCard() {
        JPanel card = createCardPanel("INTERRUPTS");
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
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel n = new JLabel(label);
        n.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        n.setForeground(COLOR_TEXT_MUTED);
        JLabel v = new JLabel("[X] Disabled");
        v.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        v.setForeground(COLOR_RED_ACCENT);
        row.add(n, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        p.add(row);
        return v;
    }

    private JPanel createSimRimCard() {
        JPanel card = createCardPanel("SIM / RIM");
        JPanel grid = new JPanel(new GridLayout(2, 1, 0, 4));
        grid.setOpaque(false);
        lblSimVal = mkPtrRow(grid, "SIM Status", "00");
        lblRimVal = mkPtrRow(grid, "RIM Status", "00");
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createDevicesCard() {
        JPanel card = createCardPanel("DEVICES");
        JPanel grid = new JPanel(new GridLayout(4, 1, 0, 2));
        grid.setOpaque(false);
        for (String name : new String[]{"ACIA", "PPI", "8253", "8255"}) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            JLabel n = new JLabel(name);
            n.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            n.setForeground(COLOR_TEXT_MUTED);
            JLabel v = new JLabel("[+] Ready");
            v.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            v.setForeground(COLOR_GREEN_ACCENT);
            row.add(n, BorderLayout.WEST);
            row.add(v, BorderLayout.EAST);
            grid.add(row);
        }
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMemoryCard() {
        JPanel card = createCardPanel("MEMORY");
        String[] cols = {"Address","00","01","02","03","04","05","06","07","08","09","0A","0B","0C","0D","0E","0F","ASCII"};
        memoryTableModel = new DefaultTableModel(cols, 16);
        memoryTable = new JTable(memoryTableModel) {
            @Override public boolean isCellEditable(int r, int c) { return c > 0 && c < 17; }
        };
        memoryTable.setBackground(COLOR_BG_CARD);
        memoryTable.setForeground(COLOR_TEXT_PRIMARY);
        memoryTable.setGridColor(COLOR_CARD_BORDER);
        memoryTable.setFont(new Font("Consolas", Font.PLAIN, 11));
        memoryTable.setRowHeight(22);
        memoryTable.getTableHeader().setBackground(COLOR_BG_HEADER);
        memoryTable.getTableHeader().setForeground(COLOR_CYAN_ACCENT);
        memoryTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        DefaultTableCellRenderer tcr = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBackground(COLOR_BG_CARD);
                setForeground(col == 0 ? COLOR_CYAN_ACCENT : col == 17 ? COLOR_TEXT_MUTED : COLOR_TEXT_PRIMARY);
                setHorizontalAlignment(SwingConstants.CENTER);
                return this;
            }
        };
        for (int i = 0; i < memoryTable.getColumnCount(); i++)
            memoryTable.getColumnModel().getColumn(i).setCellRenderer(tcr);
        JScrollPane sp = new JScrollPane(memoryTable);
        sp.setBorder(new LineBorder(COLOR_CARD_BORDER, 1));
        sp.setBackground(COLOR_BG_CARD);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    // STATUS BAR
    // ════════════════════════════════════════════════════════════════════════
    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(COLOR_BG_HEADER);
        bar.setPreferredSize(new Dimension(1200, 26));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_CARD_BORDER));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        left.setOpaque(false);
        JLabel gd = new JLabel("[+] Ready");
        gd.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gd.setForeground(COLOR_GREEN_ACCENT);
        statusInfoLabel = new JLabel("Ln 1, Col 1  |  UTF-8  |  8085 Assembly  |  Instructions: 0  |  Cycles: 0");
        statusInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusInfoLabel.setForeground(COLOR_TEXT_MUTED);
        left.add(gd);
        left.add(statusInfoLabel);
        bar.add(left, BorderLayout.WEST);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 3));
        center.setOpaque(false);
        statusStateBadge = new JLabel(" STOPPED ");
        statusStateBadge.setOpaque(true);
        statusStateBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        statusStateBadge.setForeground(COLOR_TEXT_PRIMARY);
        statusStateBadge.setBackground(COLOR_PRIMARY_BLUE);
        statusStateBadge.setBorder(new EmptyBorder(2, 8, 2, 8));
        center.add(statusStateBadge);
        bar.add(center, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        right.setOpaque(false);
        JLabel tl = new JLabel("Theme: Dark  |  Memory: 64 KB  |  100%");
        tl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tl.setForeground(COLOR_TEXT_MUTED);
        right.add(tl);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ════════════════════════════════════════════════════════════════════════
    // FIX #5 – execution state changes button colour
    public void setExecutionState(String state) {
        if (statusStateBadge == null) return;
        switch (state) {
            case "RUNNING":
                statusStateBadge.setText(" RUNNING ");
                statusStateBadge.setBackground(COLOR_GREEN_ACCENT);
                if (btnRunBar != null) {
                    btnRunBar.setText("⏸ Running");
                    styleRunBtn(btnRunBar, COLOR_GREEN_ACCENT, new Color(0, 30, 0));
                }
                break;
            case "PAUSED":
                statusStateBadge.setText(" PAUSED  ");
                statusStateBadge.setBackground(COLOR_AMBER_ACCENT);
                break;
            default:
                statusStateBadge.setText(" STOPPED ");
                statusStateBadge.setBackground(COLOR_PRIMARY_BLUE);
                if (btnRunBar != null) {
                    btnRunBar.setText("▶ Run");
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
                assembler.textEditor.jTextPane1.setBackground(new Color(0x1E, 0x1E, 0x1E));
                assembler.textEditor.jTextPane1.setForeground(new Color(0xD4, 0xD4, 0xD4));
                assembler.textEditor.jTextPane1.setCaretColor(Color.WHITE);
            }
            if (regValLabels != null) for (JLabel l : regValLabels) if (l != null) l.setForeground(COLOR_TEXT_PRIMARY);
            if (regRowPanels != null) for (int i = 0; i < regRowPanels.length; i++)
                if (regRowPanels[i] != null) regRowPanels[i].setBackground(i == 0 ? COLOR_ROW_ACTIVE : COLOR_BG_CARD);
            if (statusStateBadge != null) {
                statusStateBadge.setForeground(COLOR_TEXT_PRIMARY);
                statusStateBadge.setBackground(COLOR_PRIMARY_BLUE);
                statusStateBadge.setOpaque(true);
            }
            if (flagCircles != null) for (JLabel l : flagCircles) if (l != null) {
                l.setBackground(COLOR_CARD_BORDER);
                l.setForeground(COLOR_TEXT_MUTED);
            }
        } catch (Exception ignored) {}
    }

    // ── Live data refresh (called by Assembler after every step/run) ──────────
    
        private void updateStatusBar() {
        if (statusInfoLabel == null || assembler == null || assembler.matrix == null) return;
        int line = 1;
        int col = 1;
        try {
            if (assembler.jTextAreaAssemblyLanguageEditor != null) {
                int caretPos = assembler.jTextAreaAssemblyLanguageEditor.getCaretPosition();
                javax.swing.text.Element root = assembler.jTextAreaAssemblyLanguageEditor.getDocument().getDefaultRootElement();
                line = root.getElementIndex(caretPos) + 1;
                col = caretPos - root.getElement(line - 1).getStartOffset() + 1;
            }
        } catch (Exception ex) {}
        
        statusInfoLabel.setText("Ln " + line + ", Col " + col + "  |  UTF-8  |  8085 Assembly  |  Instructions: "
                + assembler.matrix.instructionCounter + "  |  Cycles: " + assembler.matrix.clockCycleCounter);
    }
    public void refreshData() {
        if (assembler == null || assembler.matrix == null) return;
        Matrix m = assembler.matrix;

        // Registers
        int mVal = 0;
        try {
            int hl = ((m.H & 0xFF) << 8) | (m.L & 0xFF);
            if (hl >= 0 && hl < m.memory.length) mVal = m.memory[hl] & 0xFF;
        } catch (Exception ignored) {}
        int[] vals = {m.A&0xFF, m.B&0xFF, m.C&0xFF, m.D&0xFF, m.E&0xFF, m.H&0xFF, m.L&0xFF, mVal};
        for (int i = 0; i < 8; i++)
            if (regValLabels[i] != null) regValLabels[i].setText(assembler.engine.Dec2Hex2digit(vals[i]));

        // Flags
        int f = m.F;
        boolean[] flags = {(f&0x80)!=0, (f&0x40)!=0, (f&0x10)!=0, (f&0x04)!=0, (f&0x01)!=0};
        for (int i = 0; i < 5; i++) if (flagCircles[i] != null) {
            flagCircles[i].setBackground(flags[i] ? COLOR_PRIMARY_BLUE : COLOR_CARD_BORDER);
            flagCircles[i].setForeground(flags[i] ? COLOR_TEXT_PRIMARY  : COLOR_TEXT_MUTED);
        }

        // Pointers
        if (lblPcVal != null) lblPcVal.setText(assembler.engine.Dec2Hex(m.PC));
        if (lblSpVal != null) lblSpVal.setText(assembler.engine.Dec2Hex(m.SP));
        if (lblHlVal != null) lblHlVal.setText(assembler.engine.Dec2Hex(((m.H&0xFF)<<8)|(m.L&0xFF)));
        if (lblBcVal != null) lblBcVal.setText(assembler.engine.Dec2Hex(((m.B&0xFF)<<8)|(m.C&0xFF)));
        if (lblDeVal != null) lblDeVal.setText(assembler.engine.Dec2Hex(((m.D&0xFF)<<8)|(m.E&0xFF)));

        // Execution
        if (lblPcExec     != null) lblPcExec.setText(assembler.engine.Dec2Hex(m.PC));
        if (lblInstrCount != null) lblInstrCount.setText(String.valueOf(m.instructionCounter));
        if (lblCyclesCount!= null) lblCyclesCount.setText(String.valueOf(m.clockCycleCounter));

        // Status badge
        if (statusStateBadge != null) {
            if (assembler.stop) {
                statusStateBadge.setText(" STOPPED ");
                statusStateBadge.setBackground(COLOR_PRIMARY_BLUE);
            } else {
                statusStateBadge.setText(" RUNNING ");
                statusStateBadge.setBackground(COLOR_GREEN_ACCENT);
            }
        }
        updateStatusBar();

        // Memory table
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
}
