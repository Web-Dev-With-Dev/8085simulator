import com.formdev.flatlaf.FlatLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * ModernIDEUI - Modern Professional Dark-themed IDE Layout for Aura Studio 8085 Simulator.
 * Strictly matches the user's reference design with cards, sidebar, top bar, editor welcome view,
 * register/flag/pointer inspector cards, memory table, and status bar.
 */
public class ModernIDEUI extends JPanel {

    // Color Palette Tokens (Matching Reference Image)
    public static final Color COLOR_BG_DARK     = new Color(0x0D, 0x0E, 0x11);
    public static final Color COLOR_BG_SIDEBAR  = new Color(0x12, 0x13, 0x16);
    public static final Color COLOR_BG_HEADER   = new Color(0x12, 0x13, 0x16);
    public static final Color COLOR_BG_TOOLBAR  = new Color(0x16, 0x17, 0x1C);
    public static final Color COLOR_BG_CARD     = new Color(0x16, 0x18, 0x1D);
    public static final Color COLOR_CARD_BORDER = new Color(0x22, 0x24, 0x2B);
    public static final Color COLOR_PRIMARY_BLUE = new Color(0x00, 0x78, 0xD4);
    public static final Color COLOR_CYAN_ACCENT  = new Color(0x4F, 0xC3, 0xF7);
    public static final Color COLOR_AMBER_ACCENT = new Color(0xF5, 0x9E, 0x0B);
    public static final Color COLOR_GREEN_ACCENT = new Color(0x22, 0xC5, 0x5E);
    public static final Color COLOR_RED_ACCENT   = new Color(0xEF, 0x44, 0x44);
    public static final Color COLOR_TEXT_PRIMARY = new Color(0xFF, 0xFF, 0xFF);
    public static final Color COLOR_TEXT_MUTED   = new Color(0x8E, 0x92, 0x9E);
    public static final Color COLOR_ROW_ACTIVE   = new Color(0x00, 0x4B, 0x87);

    private final Assembler assembler;
    
    // Core Panel References
    private JPanel centerEditorContainer;
    private CardLayout editorCardLayout;
    private JPanel welcomeViewPanel;
    private JPanel codeEditorPanel;

    // Card Components
    private JLabel[] regValLabels = new JLabel[8];
    private JPanel[] regRowPanels = new JPanel[8];
    private JLabel[] flagCircles = new JLabel[5]; // S, Z, AC, P, CY
    
    private JLabel lblPcVal, lblSpVal, lblHlVal, lblBcVal, lblDeVal;
    private JLabel lblCurInstr, lblExecState, lblPcExec, lblInstrCount, lblCyclesCount, lblMCycles;
    private JLabel lblTrap, lblRst75, lblRst65, lblRst55, lblIntr;
    private JLabel lblSimVal, lblRimVal;
    
    private JTable memoryTable;
    private DefaultTableModel memoryTableModel;
    
    private JLabel statusStateBadge;
    private JLabel statusInfoLabel;

    // Tab bar state (updated by document listener and file load)
    private JPanel activeTab;
    private JLabel tabTitle;
    private JLabel unsavedDot;

    private JLabel debuggerExplainerLabel;
    private String activeSidebarItem = "Editor";
    private Map<String, JPanel> sidebarButtons = new HashMap<>();

    public ModernIDEUI(Assembler asm) {
        this.assembler = asm;
        setLayout(new BorderLayout());
        setBackground(COLOR_BG_DARK);
        
        // 1. Build Top Header & Menu Bar
        add(createTopHeaderPanel(), BorderLayout.NORTH);
        
        // 2. Main Central Split / Workspace
        JPanel mainWorkspace = new JPanel(new BorderLayout());
        mainWorkspace.setBackground(COLOR_BG_DARK);
        
        // Left Activity Sidebar Rail
        mainWorkspace.add(createActivitySidebar(), BorderLayout.WEST);
        
        // Split Pane: Center (Editor) + Right (Dashboard Cards)
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setBackground(COLOR_BG_DARK);
        centerSplit.setDividerSize(4);
        centerSplit.setResizeWeight(0.55);
        centerSplit.setBorder(null);

        // Center Area: Top Action Bar + Code Editor / Welcome View
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(COLOR_BG_DARK);
        centerPanel.add(createActionBar(), BorderLayout.NORTH);
        centerPanel.add(createEditorContainer(), BorderLayout.CENTER);
        
        centerSplit.setLeftComponent(centerPanel);
        centerSplit.setRightComponent(createDashboardCardsPanel());
        
        mainWorkspace.add(centerSplit, BorderLayout.CENTER);
        add(mainWorkspace, BorderLayout.CENTER);
        
        // 3. Bottom Status Bar
        add(createStatusBar(), BorderLayout.SOUTH);
        
        // Initial Refresh
        refreshData();
    }

    /**
     * Top Header Panel with Aura Studio branding and menu bar
     */
    private JPanel createTopHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_BG_HEADER);
        header.setPreferredSize(new Dimension(1200, 42));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_CARD_BORDER));

        // Left Branding
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        brandPanel.setOpaque(false);
        
        JLabel logoIcon = new JLabel("⚡");
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.BOLD, 18));
        logoIcon.setForeground(COLOR_CYAN_ACCENT);
        
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

        // Menu Bar embedded cleanly
        JMenuBar mb = assembler.getJMenuBar();
        if (mb != null) {
            mb.setOpaque(false);
            mb.setBackground(COLOR_BG_HEADER);
            mb.setBorder(null);
            header.add(mb, BorderLayout.CENTER);
        }

        return header;
    }

    /**
     * Action Bar / Tool Bar (New, Open, Save, Assemble, Run, Step, Stop, etc.)
     */
    private JPanel createActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setBackground(COLOR_BG_TOOLBAR);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_CARD_BORDER));

        bar.add(createToolButton("New", "\uD83D\uDCC4", e -> {
            assembler.jTextAreaAssemblyLanguageEditor.setText("; New 8085 Program\n\nMVI A, 00H\nHLT\n");
            assembler.textEditor.colorEditor();
            showEditorView();
        }));
        bar.add(createToolButton("Open", "\uD83D\uDCC1", e -> {
            if (assembler.jMenuItemLoad_Assembly_Language_code != null) {
                assembler.jMenuItemLoad_Assembly_Language_code.doClick();
            }
            // Show editor after a brief delay to let file load dialog finish
            javax.swing.Timer t = new javax.swing.Timer(500, ev -> showEditorView());
            t.setRepeats(false);
            t.start();
        }));
        bar.add(createToolButton("Save", "\uD83D\uDCBE", e -> {
            if (assembler.jMenuItemSave_Assembly_Language_code != null) {
                assembler.jMenuItemSave_Assembly_Language_code.doClick();
            }
        }));

        // Separator
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 22));
        sep.setForeground(COLOR_CARD_BORDER);
        bar.add(sep);

        bar.add(createToolButton("Assemble", "\u2699", e -> {
            showEditorView();
            // Use invokeLater so editor is visible before assembling
            javax.swing.SwingUtilities.invokeLater(() -> {
                if (assembler.jButtonAssemble != null) {
                    assembler.jButtonAssemble.doClick();
                }
            });
        }));

        // Run Button (Primary Blue Accent Button)
        JButton btnRun = new JButton("\u25B6 Run");
        btnRun.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRun.setForeground(COLOR_TEXT_PRIMARY);
        btnRun.setBackground(COLOR_PRIMARY_BLUE);
        btnRun.setFocusPainted(false);
        btnRun.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btnRun.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRun.setOpaque(true);
        btnRun.addActionListener(e -> {
            showEditorView();
            // jButtonRun may be hidden but still actionable via doClick
            if (assembler.jButtonRun != null) {
                assembler.jButtonRun.doClick();
            }
            if (assembler.modernIDEUI != null) {
                assembler.modernIDEUI.setExecutionState("RUNNING");
            }
        });
        bar.add(btnRun);

        bar.add(createToolButton("Step", "\u23EF", e -> {
            showDebuggerView();
            if (assembler.jButtonStep != null) {
                assembler.jButtonStep.doClick();
            }
        }));

        bar.add(createToolButton("Step Fwd", "\u23ED", e -> {
            showDebuggerView();
            if (assembler.jButtonForward != null) {
                assembler.jButtonForward.doClick();
            }
        }));

        bar.add(createToolButton("Step Back", "\u23EE", e -> {
            showDebuggerView();
            if (assembler.jButtonBackward != null) {
                assembler.jButtonBackward.doClick();
            }
        }));
        bar.add(createToolButton("Pause", "\u23F8", e -> {
            // Set text to Pause BEFORE doClick so the action handler reads it correctly
            if (assembler.jButtonStop != null) {
                assembler.jButtonStop.setVisible(true);
                assembler.jButtonStop.setText("Pause");
                assembler.jButtonStop.doClick();
            }
            setExecutionState("PAUSED");
        }));
        bar.add(createToolButton("Stop", "\u23F9", e -> {
            if (assembler.jButtonStop != null) {
                assembler.jButtonStop.setVisible(true);
                assembler.jButtonStop.setText("Stop");
                assembler.jButtonStop.doClick();
            }
            setExecutionState("STOPPED");
        }));
        bar.add(createToolButton("Reset", "\u21BA", e -> {
            if (assembler.jMenuItemClearMemory != null) {
                assembler.jMenuItemClearMemory.doClick();
            }
            setExecutionState("STOPPED");
        }));
        bar.add(createToolButton("Autocorrect", "\u2728", e -> {
            showEditorView();
            if (assembler.jButtonAutocorrect != null) {
                assembler.jButtonAutocorrect.doClick();
            }
        }));

        return bar;
    }

    /** Update the status badge color/text from outside (called by Assembler on state changes) */
    public void setExecutionState(String state) {
        if (statusStateBadge == null) return;
        switch (state) {
            case "RUNNING":
                statusStateBadge.setText(" RUNNING ");
                statusStateBadge.setBackground(COLOR_GREEN_ACCENT);
                break;
            case "PAUSED":
                statusStateBadge.setText(" PAUSED  ");
                statusStateBadge.setBackground(COLOR_AMBER_ACCENT);
                break;
            default: // STOPPED
                statusStateBadge.setText(" STOPPED ");
                statusStateBadge.setBackground(COLOR_PRIMARY_BLUE);
                break;
        }
        statusStateBadge.repaint();
    }

    /**
     * Creates a toolbar button with a plain Unicode/text icon and label.
     * Uses a two-component approach: icon label (Segoe UI Emoji) + text label, so
     * icons always render correctly regardless of LAF theme changes.
     */
    private JButton createToolButton(String text, String icon, java.awt.event.ActionListener listener) {
        // Use HTML so Swing can render the icon in emoji-capable font and text in regular font
        String htmlLabel = "<html><span style='font-family:Segoe UI Emoji;font-size:11pt;'>" + icon +
                           "</span>&nbsp;" + text + "</html>";
        JButton btn = new JButton(htmlLabel);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setForeground(COLOR_TEXT_PRIMARY);
        btn.setBackground(COLOR_BG_CARD);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(listener);
        return btn;
    }

    /**
     * Activity Sidebar Rail (Left vertical navigation bar)
     */
    private JPanel createActivitySidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(COLOR_BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(68, 600));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_CARD_BORDER));

        // Each item: { id, unicode icon char }
        String[][] items = {
                {"Editor",       "\u270F"},  // pencil
                {"Registers",    "\uD83D\uDDD3"},  // table
                {"Memory",       "\uD83D\uDCBE"},  // disk
                {"Devices",      "\uD83D\uDD0C"},  // plug
                {"Subroutine",   "\uD83D\uDD00"},  // arrows
                {"Interrupts",   "\uD83D\uDD14"},  // bell
                {"I/O Port",     "\u26A1"},  // bolt
                {"Disassembler", "\u007B\u007D"}, // {}
                {"Settings",     "\u2699"}   // gear
        };

        for (String[] item : items) {
            String id = item[0];
            String icon = item[1];

            JPanel btn = new JPanel(new BorderLayout());
            btn.setMaximumSize(new Dimension(68, 58));
            btn.setPreferredSize(new Dimension(68, 58));
            btn.setOpaque(true);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            boolean isActive = id.equals(activeSidebarItem);
            btn.setBackground(isActive ? COLOR_PRIMARY_BLUE : COLOR_BG_SIDEBAR);

            // Icon in emoji font so it renders properly
            JLabel lblIcon = new JLabel(icon, SwingConstants.CENTER);
            lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            lblIcon.setForeground(COLOR_TEXT_PRIMARY);
            lblIcon.setOpaque(false);

            JLabel lblText = new JLabel(id, SwingConstants.CENTER);
            lblText.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            lblText.setForeground(isActive ? COLOR_TEXT_PRIMARY : COLOR_TEXT_MUTED);
            lblText.setOpaque(false);

            JPanel innerPanel = new JPanel(new BorderLayout(0, 2));
            innerPanel.setOpaque(false);
            innerPanel.add(lblIcon, BorderLayout.CENTER);
            innerPanel.add(lblText, BorderLayout.SOUTH);
            innerPanel.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));

            btn.add(innerPanel, BorderLayout.CENTER);

            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    activeSidebarItem = id;
                    updateSidebarSelection();
                    handleSidebarAction(id);
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!id.equals(activeSidebarItem)) {
                        btn.setBackground(new Color(0x1E, 0x20, 0x27));
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (!id.equals(activeSidebarItem)) {
                        btn.setBackground(COLOR_BG_SIDEBAR);
                    }
                }
            });

            sidebarButtons.put(id, btn);
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(2));
        }

        return sidebar;
    }

    private void updateSidebarSelection() {
        for (Map.Entry<String, JPanel> entry : sidebarButtons.entrySet()) {
            boolean active = entry.getKey().equals(activeSidebarItem);
            JPanel sideBtn = entry.getValue();
            sideBtn.setBackground(active ? COLOR_PRIMARY_BLUE : COLOR_BG_SIDEBAR);
            // Update the inner panel and its children labels
            for (Component c : sideBtn.getComponents()) {
                if (c instanceof JPanel) { // inner panel
                    for (Component inner : ((JPanel)c).getComponents()) {
                        if (inner instanceof JLabel) {
                            JLabel lbl = (JLabel)inner;
                            if (lbl.getFont().getSize() == 9) {
                                lbl.setForeground(active ? COLOR_TEXT_PRIMARY : COLOR_TEXT_MUTED);
                            } else {
                                lbl.setForeground(COLOR_TEXT_PRIMARY);
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleSidebarAction(String id) {
        switch (id) {
            case "Editor":
                showEditorView();
                break;
            case "Registers":
                showEditorView();
                assembler.showRegistersPanel();
                break;
            case "Memory":
                showEditorView();
                assembler.showMemoryPanel();
                break;
            case "Devices":
                showEditorView();
                assembler.showDevicesPanel();
                break;
            case "Subroutine":
                showEditorView();
                assembler.openDelaySubroutine();
                break;
            case "Interrupts":
                showEditorView();
                assembler.openInterruptSubroutine();
                break;
            case "I/O Port":
                showEditorView();
                assembler.openIOPortPanel();
                break;
            case "Disassembler":
                showEditorView();
                assembler.openDisassemblerTab();
                break;
            case "Settings":
                assembler.openSettings();
                break;
            default:
                showEditorView();
                break;
        }
    }

    /**
     * Editor Container holding Tab bar + Switchable Welcome View and Code Editor View
     */
    private JPanel createEditorContainer() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(COLOR_BG_DARK);

        // Sub Tab Bar (untitled.asm +)
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabBar.setBackground(COLOR_BG_HEADER);
        tabBar.setPreferredSize(new Dimension(800, 32));
        tabBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_CARD_BORDER));

        JPanel activeTab = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        activeTab.setBackground(COLOR_BG_DARK);
        activeTab.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, COLOR_PRIMARY_BLUE));

        // Unsaved dot — shown when editor has unsaved content
        unsavedDot = new JLabel("● ");
        unsavedDot.setFont(new Font("Segoe UI", Font.BOLD, 10));
        unsavedDot.setForeground(COLOR_CYAN_ACCENT);
        unsavedDot.setVisible(false); // hidden until something is typed

        tabTitle = new JLabel("untitled.asm ");
        tabTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabTitle.setForeground(COLOR_TEXT_PRIMARY);

        JLabel closeBtn = new JLabel("✕");
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        closeBtn.setForeground(COLOR_TEXT_MUTED);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Close = clear editor, reset to untitled, hide unsaved dot, show welcome card
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                if (javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                    int confirm = javax.swing.JOptionPane.showConfirmDialog(
                        assembler,
                        "Close this file? Unsaved changes will be lost.",
                        "Close File", javax.swing.JOptionPane.YES_NO_OPTION);
                    if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                        assembler.jTextAreaAssemblyLanguageEditor.setText("");
                        tabTitle.setText("untitled.asm ");
                        if (unsavedDot != null) unsavedDot.setVisible(false);
                        showWelcomeView();
                    }
                }
            }
            @Override public void mouseEntered(MouseEvent e) { closeBtn.setForeground(COLOR_TEXT_PRIMARY); }
            @Override public void mouseExited(MouseEvent e)  { closeBtn.setForeground(COLOR_TEXT_MUTED);   }
        });

        activeTab.add(unsavedDot);
        activeTab.add(tabTitle);
        activeTab.add(closeBtn);
        tabBar.add(activeTab);

        container.add(tabBar, BorderLayout.NORTH);

        // Center Card Layout (Welcome vs Code Editor)
        editorCardLayout = new CardLayout();
        centerEditorContainer = new JPanel(editorCardLayout);
        centerEditorContainer.setBackground(COLOR_BG_DARK);

        // 1. Welcome View
        welcomeViewPanel = createWelcomeView();
        centerEditorContainer.add(welcomeViewPanel, "WELCOME");

        // 2. Code Editor View
        // Re-use the NetBeans-configured scroll pane (jScrollPane9) directly.
        // This is much safer as it has all the original layout bounds, borders,
        // and lines rendering logic set up correctly.
        codeEditorPanel = new JPanel(new BorderLayout());
        codeEditorPanel.setBackground(new Color(0x1E, 0x1E, 0x1E));

        if (assembler.getEditorScrollPane() != null) {
            javax.swing.JScrollPane editorScroll = assembler.getEditorScrollPane();
            editorScroll.setBorder(null);
            editorScroll.setBackground(new Color(0x1E, 0x1E, 0x1E));
            editorScroll.getViewport().setBackground(new Color(0x1E, 0x1E, 0x1E));

            if (assembler.textEditor != null && assembler.textEditor.jTextPane1 != null) {
                // Ensure text pane is editable and styled correctly
                assembler.textEditor.jTextPane1.setBackground(new Color(0x1E, 0x1E, 0x1E));
                assembler.textEditor.jTextPane1.setForeground(new Color(0xD4, 0xD4, 0xD4));
                assembler.textEditor.jTextPane1.setCaretColor(Color.WHITE);
                assembler.textEditor.jTextPane1.setEditable(true);

                // Track document changes to show/hide the unsaved dot
                assembler.textEditor.jTextPane1.getDocument().addDocumentListener(
                    new javax.swing.event.DocumentListener() {
                        public void insertUpdate(javax.swing.event.DocumentEvent e)  { markUnsaved(); }
                        public void removeUpdate(javax.swing.event.DocumentEvent e)  { markUnsaved(); }
                        public void changedUpdate(javax.swing.event.DocumentEvent e) {}
                        private void markUnsaved() {
                            if (unsavedDot != null) unsavedDot.setVisible(true);
                        }
                    });
            }

            codeEditorPanel.add(editorScroll, BorderLayout.CENTER);
        }
        centerEditorContainer.add(codeEditorPanel, "EDITOR");

        // 3. Step Debugger View
        JPanel debuggerPanel = new JPanel(new BorderLayout());
        debuggerPanel.setBackground(COLOR_BG_DARK);

        if (assembler.getDebuggerScrollPane() != null) {
            javax.swing.JScrollPane debugScroll = assembler.getDebuggerScrollPane();
            debugScroll.setBorder(null);
            debugScroll.setBackground(COLOR_BG_DARK);
            debugScroll.getViewport().setBackground(COLOR_BG_DARK);
            
            // Add a large label at the top for the Step Explainer
            debuggerExplainerLabel = new JLabel("<html><b>Step Explainer:</b> Click 'Step Fwd' to begin execution trace.</html>");
            debuggerExplainerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            debuggerExplainerLabel.setForeground(COLOR_CYAN_ACCENT);
            debuggerExplainerLabel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
            debuggerExplainerLabel.setBackground(COLOR_BG_CARD);
            debuggerExplainerLabel.setOpaque(true);
            
            debuggerPanel.add(debuggerExplainerLabel, BorderLayout.NORTH);
            debuggerPanel.add(debugScroll, BorderLayout.CENTER);
        }
        centerEditorContainer.add(debuggerPanel, "DEBUGGER");

        // Always start on Editor view
        editorCardLayout.show(centerEditorContainer, "EDITOR");

        container.add(centerEditorContainer, BorderLayout.CENTER);
        return container;
    }

    public void showEditorView() {
        if (editorCardLayout != null && centerEditorContainer != null) {
            editorCardLayout.show(centerEditorContainer, "EDITOR");
        }
        // Re-focus the text pane so user can type immediately
        if (assembler.textEditor != null && assembler.textEditor.jTextPane1 != null) {
            assembler.textEditor.jTextPane1.requestFocusInWindow();
        }
    }

    public void showDebuggerView() {
        if (editorCardLayout != null && centerEditorContainer != null) {
            editorCardLayout.show(centerEditorContainer, "DEBUGGER");
        }
    }

    public void updateStepExplainer(String text) {
        if (debuggerExplainerLabel != null) {
            debuggerExplainerLabel.setText("<html><b>Step Explainer:</b> " + text + "</html>");
        }
    }

    /** Update the tab title (e.g. when a file is opened or saved) */
    public void setTabTitle(String filename) {
        if (tabTitle != null) {
            tabTitle.setText(filename + " ");
        }
    }

    /** Mark the current file as saved (hide unsaved dot) */
    public void markSaved() {
        if (unsavedDot != null) unsavedDot.setVisible(false);
    }

    public void showWelcomeView() {
        if (editorCardLayout != null && centerEditorContainer != null) {
            editorCardLayout.show(centerEditorContainer, "WELCOME");
        }
    }

    /**
     * Welcome Screen (Matches exact layout from reference image)
     */
    private JPanel createWelcomeView() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(COLOR_BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;

        // Big Blue Stylized Emblem Icon
        JLabel emblem = new JLabel("A");
        emblem.setFont(new Font("Segoe UI Black", Font.BOLD, 48));
        emblem.setForeground(COLOR_CYAN_ACCENT);
        p.add(emblem, gbc);

        // Aura Studio Title
        gbc.gridy++;
        JLabel lblTitle = new JLabel("Aura Studio");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(COLOR_TEXT_PRIMARY);
        p.add(lblTitle, gbc);

        // Subtitle
        gbc.gridy++;
        JLabel lblSub = new JLabel("8085 Microprocessor Simulator");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSub.setForeground(COLOR_TEXT_MUTED);
        p.add(lblSub, gbc);

        // Action Buttons Row
        gbc.gridy++;
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        btnRow.setOpaque(false);

        JButton btnNew = new JButton("+ Create New Program");
        btnNew.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnNew.setForeground(COLOR_TEXT_PRIMARY);
        btnNew.setBackground(COLOR_PRIMARY_BLUE);
        btnNew.setFocusPainted(false);
        btnNew.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btnNew.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNew.addActionListener(e -> {
            assembler.jTextAreaAssemblyLanguageEditor.setText("; Aura Studio 8085 Program\n\nMVI A, 05H\nMVI B, 03H\nADD B\nHLT\n");
            assembler.textEditor.colorEditor();
            showEditorView();
        });

        JButton btnOpen = new JButton("📁 Open Program");
        btnOpen.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnOpen.setForeground(COLOR_TEXT_PRIMARY);
        btnOpen.setBackground(COLOR_BG_CARD);
        btnOpen.setFocusPainted(false);
        btnOpen.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        btnOpen.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOpen.addActionListener(e -> {
            if (assembler.jMenuItemLoad_Assembly_Language_code != null) {
                assembler.jMenuItemLoad_Assembly_Language_code.doClick();
            }
            showEditorView();
        });

        JButton btnSample = new JButton("📦 Open Sample");
        btnSample.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnSample.setForeground(COLOR_TEXT_PRIMARY);
        btnSample.setBackground(COLOR_BG_CARD);
        btnSample.setFocusPainted(false);
        btnSample.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        btnSample.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSample.addActionListener(e -> {
            assembler.jTextAreaAssemblyLanguageEditor.setText("; 8085 Sample Program: Addition of two 8-bit numbers\nMVI A, 05H\nMVI B, 0A4H\nADD B\nSTA C050H\nHLT\n");
            assembler.textEditor.colorEditor();
            showEditorView();
        });

        btnRow.add(btnNew);
        btnRow.add(btnOpen);
        btnRow.add(btnSample);
        p.add(btnRow, gbc);

        // Side-by-Side Cards (Recent Files & Helpful Shortcuts)
        gbc.gridy++;
        JPanel cardsRow = new JPanel(new GridLayout(1, 2, 16, 0));
        cardsRow.setOpaque(false);
        cardsRow.setPreferredSize(new Dimension(560, 200));

        // Recent Files Card
        JPanel cardRecent = createCardPanel("Recent Files");
        JPanel recentHeader = (JPanel) cardRecent.getComponent(0);
        JLabel lblClear = new JLabel("Clear");
        lblClear.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblClear.setForeground(COLOR_CYAN_ACCENT);
        lblClear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        recentHeader.add(lblClear, BorderLayout.EAST);

        JPanel recentList = new JPanel();
        recentList.setLayout(new BoxLayout(recentList, BoxLayout.Y_AXIS));
        recentList.setOpaque(false);

        String[] samples = {"factorial.asm", "count.asm", "addition.asm", "bubble_sort.asm", "array_sum.asm"};
        for (String sample : samples) {
            JLabel item = new JLabel(sample + "    >");
            item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            item.setForeground(COLOR_TEXT_MUTED);
            item.setCursor(new Cursor(Cursor.HAND_CURSOR));
            item.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            item.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    loadSampleCode(sample);
                    showEditorView();
                }
            });
            recentList.add(item);
        }
        cardRecent.add(recentList, BorderLayout.CENTER);

        // Shortcuts Card
        JPanel cardShortcuts = createCardPanel("Helpful Shortcuts");
        JPanel shortcutsList = new JPanel(new GridLayout(4, 2, 8, 6));
        shortcutsList.setOpaque(false);

        String[][] scs = {
                {"New Program", "Ctrl + N"},
                {"Open Program", "Ctrl + O"},
                {"Save Program", "Ctrl + S"},
                {"Assemble", "F5"},
                {"Run", "F6"},
                {"Step", "F7"},
                {"Reset", "Ctrl + R"},
                {"Toggle Breakpoint", "F9"}
        };

        for (String[] sc : scs) {
            JLabel name = new JLabel(sc[0]);
            name.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            name.setForeground(COLOR_TEXT_MUTED);

            JLabel key = new JLabel(sc[1], SwingConstants.RIGHT);
            key.setFont(new Font("Consolas", Font.BOLD, 11));
            key.setForeground(COLOR_CYAN_ACCENT);

            shortcutsList.add(name);
            shortcutsList.add(key);
        }
        cardShortcuts.add(shortcutsList, BorderLayout.CENTER);

        cardsRow.add(cardRecent);
        cardsRow.add(cardShortcuts);
        p.add(cardsRow, gbc);

        return p;
    }

    private void loadSampleCode(String name) {
        if ("factorial.asm".equalsIgnoreCase(name)) {
            assembler.jTextAreaAssemblyLanguageEditor.setText("; Factorial of a Number\nMVI C, 05H\nMVI A, 01H\nFACT: DCR C\nJZ END\nMOV B, A\nMVI D, 00H\nMULT: ADD B\nDCR C\nJNZ MULT\nEND: HLT\n");
        } else if ("addition.asm".equalsIgnoreCase(name)) {
            assembler.jTextAreaAssemblyLanguageEditor.setText("; 8-bit Addition\nMVI A, 20H\nMVI B, 30H\nADD B\nSTA C000H\nHLT\n");
        } else if ("count.asm".equalsIgnoreCase(name)) {
            assembler.jTextAreaAssemblyLanguageEditor.setText("; Count from 1 to 10\nMVI B, 0AH\nMVI A, 00H\nLOOP: INR A\nDCR B\nJNZ LOOP\nSTA C050H\nHLT\n");
        } else if ("bubble_sort.asm".equalsIgnoreCase(name)) {
            assembler.jTextAreaAssemblyLanguageEditor.setText("; Bubble Sort\nLXI H, C050H\nMVI B, 05H\nOUTER: MOV C, B\nDCR C\nLXI H, C050H\nINNER: MOV A, M\nINX H\nCMP M\nJC SKIP\nMOV D, M\nMOV M, A\nDCX H\nMOV M, D\nINX H\nSKIP: DCR C\nJNZ INNER\nDCR B\nJNZ OUTER\nHLT\n");
        } else if ("array_sum.asm".equalsIgnoreCase(name)) {
            assembler.jTextAreaAssemblyLanguageEditor.setText("; Sum of Array Elements\nLXI H, C050H\nMVI B, 05H\nMVI A, 00H\nLOOP: ADD M\nINX H\nDCR B\nJNZ LOOP\nSTA C100H\nHLT\n");
        } else {
            assembler.jTextAreaAssemblyLanguageEditor.setText("; 8085 Program: " + name + "\nMVI A, 10H\nMVI B, 05H\nADD B\nHLT\n");
        }
        assembler.textEditor.colorEditor();
    }

    /**
     * Dashboard Cards Panel (Right Side - Static, No Scrolling)
     */
    private JPanel createDashboardCardsPanel() {
        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(COLOR_BG_DARK);
        container.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(4, 4, 4, 4);

        // Row 0: REGISTERS | FLAGS | POINTERS
        gbc.gridy = 0; gbc.weighty = 0.30;
        gbc.gridx = 0; gbc.weightx = 0.33;
        container.add(createRegistersCard(), gbc);
        gbc.gridx = 1; gbc.weightx = 0.33;
        container.add(createFlagsCard(), gbc);
        gbc.gridx = 2; gbc.weightx = 0.34;
        container.add(createPointersCard(), gbc);

        // Row 1: EXECUTION | INTERRUPTS
        gbc.gridy = 1; gbc.weighty = 0.20;
        gbc.gridx = 0; gbc.gridwidth = 2; gbc.weightx = 0.66;
        container.add(createExecutionCard(), gbc);
        gbc.gridx = 2; gbc.gridwidth = 1; gbc.weightx = 0.34;
        container.add(createInterruptsCard(), gbc);

        // Row 2: SIM/RIM | DEVICES
        gbc.gridy = 2; gbc.weighty = 0.15;
        gbc.gridx = 0; gbc.gridwidth = 2; gbc.weightx = 0.66;
        container.add(createSimRimCard(), gbc);
        gbc.gridx = 2; gbc.gridwidth = 1; gbc.weightx = 0.34;
        container.add(createDevicesCard(), gbc);

        // Row 3: MEMORY (Full Width)
        gbc.gridy = 3; gbc.weighty = 0.35;
        gbc.gridx = 0; gbc.gridwidth = 3; gbc.weightx = 1.0;
        container.add(createMemoryCard(), gbc);

        return container;
    }

    private JPanel createCardPanel(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(COLOR_BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_CARD_BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(COLOR_CYAN_ACCENT);

        header.add(lblTitle, BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);
        return card;
    }

    /**
     * REGISTERS Card (A, B, C, D, E, H, L, M)
     */
    private JPanel createRegistersCard() {
        JPanel card = createCardPanel("REGISTERS");
        JPanel grid = new JPanel(new GridLayout(8, 1, 0, 2));
        grid.setOpaque(false);

        String[] regNames = {"A", "B", "C", "D", "E", "H", "L", "M"};
        for (int i = 0; i < 8; i++) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(true);
            row.setBackground(i == 0 ? COLOR_ROW_ACTIVE : COLOR_BG_CARD);
            row.setBorder(new EmptyBorder(2, 6, 2, 6));

            JLabel lblName = new JLabel(regNames[i]);
            lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblName.setForeground(COLOR_TEXT_PRIMARY);

            JLabel lblVal = new JLabel("00");
            lblVal.setFont(new Font("Consolas", Font.BOLD, 12));
            lblVal.setForeground(COLOR_TEXT_PRIMARY);

            row.add(lblName, BorderLayout.WEST);
            row.add(lblVal, BorderLayout.EAST);

            regRowPanels[i] = row;
            regValLabels[i] = lblVal;
            grid.add(row);
        }

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    /**
     * FLAGS Card (S, Z, AC, P, CY)
     */
    private JPanel createFlagsCard() {
        JPanel card = createCardPanel("FLAGS");
        JPanel circlesPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 20));
        circlesPanel.setOpaque(false);

        String[] names = {"S", "Z", "AC", "P", "CY"};
        for (int i = 0; i < 5; i++) {
            JLabel circle = new JLabel(names[i], SwingConstants.CENTER);
            circle.setPreferredSize(new Dimension(36, 36));
            circle.setOpaque(true);
            circle.setFont(new Font("Segoe UI", Font.BOLD, 11));
            circle.setForeground(COLOR_TEXT_PRIMARY);
            circle.setBackground(COLOR_CARD_BORDER);
            circle.setBorder(new LineBorder(COLOR_CARD_BORDER, 1, true));

            flagCircles[i] = circle;
            circlesPanel.add(circle);
        }

        card.add(circlesPanel, BorderLayout.CENTER);
        return card;
    }

    /**
     * POINTERS Card (PC, SP, HL, BC, DE)
     */
    private JPanel createPointersCard() {
        JPanel card = createCardPanel("POINTERS");
        JPanel grid = new JPanel(new GridLayout(5, 1, 0, 4));
        grid.setOpaque(false);

        lblPcVal = createPointerRow(grid, "PC", "0000");
        lblSpVal = createPointerRow(grid, "SP", "FFFF");
        lblHlVal = createPointerRow(grid, "HL", "0000");
        lblBcVal = createPointerRow(grid, "BC", "0000");
        lblDeVal = createPointerRow(grid, "DE", "0000");

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JLabel createPointerRow(JPanel parent, String label, String defaultVal) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel lblName = new JLabel(label);
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblName.setForeground(COLOR_TEXT_MUTED);

        JLabel lblVal = new JLabel(defaultVal);
        lblVal.setFont(new Font("Consolas", Font.BOLD, 12));
        lblVal.setForeground(COLOR_CYAN_ACCENT);

        row.add(lblName, BorderLayout.WEST);
        row.add(lblVal, BorderLayout.EAST);
        parent.add(row);
        return lblVal;
    }

    /**
     * EXECUTION Card
     */
    private JPanel createExecutionCard() {
        JPanel card = createCardPanel("EXECUTION");
        JPanel grid = new JPanel(new GridLayout(3, 2, 8, 4));
        grid.setOpaque(false);

        lblCurInstr = createExecutionItem(grid, "Current Instr", "NOP");
        lblExecState = createExecutionItem(grid, "State", "STOPPED");
        lblPcExec = createExecutionItem(grid, "PC Address", "0000");
        lblInstrCount = createExecutionItem(grid, "Instructions", "0");
        lblCyclesCount = createExecutionItem(grid, "Clock Cycles", "0");
        lblMCycles = createExecutionItem(grid, "M-Cycles", "0");

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JLabel createExecutionItem(JPanel parent, String label, String defaultVal) {
        JPanel box = new JPanel(new BorderLayout());
        box.setOpaque(false);

        JLabel lblName = new JLabel(label);
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblName.setForeground(COLOR_TEXT_MUTED);

        JLabel lblVal = new JLabel(defaultVal);
        lblVal.setFont(new Font("Consolas", Font.BOLD, 11));
        lblVal.setForeground(COLOR_CYAN_ACCENT);

        box.add(lblName, BorderLayout.WEST);
        box.add(lblVal, BorderLayout.EAST);
        parent.add(box);
        return lblVal;
    }

    /**
     * INTERRUPTS Card
     */
    private JPanel createInterruptsCard() {
        JPanel card = createCardPanel("INTERRUPTS");
        JPanel grid = new JPanel(new GridLayout(5, 1, 0, 2));
        grid.setOpaque(false);

        lblTrap = createInterruptRow(grid, "TRAP");
        lblRst75 = createInterruptRow(grid, "RST 7.5");
        lblRst65 = createInterruptRow(grid, "RST 6.5");
        lblRst55 = createInterruptRow(grid, "RST 5.5");
        lblIntr = createInterruptRow(grid, "INTR");

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JLabel createInterruptRow(JPanel parent, String label) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel lblName = new JLabel(label);
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblName.setForeground(COLOR_TEXT_MUTED);

        JLabel lblVal = new JLabel("✕ Disabled");
        lblVal.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblVal.setForeground(COLOR_RED_ACCENT);

        row.add(lblName, BorderLayout.WEST);
        row.add(lblVal, BorderLayout.EAST);
        parent.add(row);
        return lblVal;
    }

    /**
     * SIM / RIM Card
     */
    private JPanel createSimRimCard() {
        JPanel card = createCardPanel("SIM / RIM");
        JPanel grid = new JPanel(new GridLayout(2, 1, 0, 4));
        grid.setOpaque(false);

        lblSimVal = createPointerRow(grid, "SIM Status", "00");
        lblRimVal = createPointerRow(grid, "RIM Status", "00");

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    /**
     * DEVICES Card
     */
    private JPanel createDevicesCard() {
        JPanel card = createCardPanel("DEVICES");
        JPanel grid = new JPanel(new GridLayout(4, 1, 0, 2));
        grid.setOpaque(false);

        createDeviceRow(grid, "ACIA");
        createDeviceRow(grid, "PPI");
        createDeviceRow(grid, "8253");
        createDeviceRow(grid, "8255");

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private void createDeviceRow(JPanel parent, String name) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblName.setForeground(COLOR_TEXT_MUTED);

        JLabel lblVal = new JLabel("● Ready");
        lblVal.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblVal.setForeground(COLOR_GREEN_ACCENT);

        row.add(lblName, BorderLayout.WEST);
        row.add(lblVal, BorderLayout.EAST);
        parent.add(row);
    }

    /**
     * MEMORY Card (Bottom full width card with address grid & ASCII)
     */
    private JPanel createMemoryCard() {
        JPanel card = createCardPanel("MEMORY");

        String[] cols = {"Address", "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "0A", "0B", "0C", "0D", "0E", "0F", "ASCII"};
        memoryTableModel = new DefaultTableModel(cols, 16);
        memoryTable = new JTable(memoryTableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column > 0 && column < 17;
            }
        };

        memoryTable.setBackground(COLOR_BG_CARD);
        memoryTable.setForeground(COLOR_TEXT_PRIMARY);
        memoryTable.setGridColor(COLOR_CARD_BORDER);
        memoryTable.setFont(new Font("Consolas", Font.PLAIN, 11));
        memoryTable.setRowHeight(22);
        memoryTable.getTableHeader().setBackground(COLOR_BG_HEADER);
        memoryTable.getTableHeader().setForeground(COLOR_CYAN_ACCENT);
        memoryTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));

        // Custom Cell Renderer for Memory Table Highlights
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(COLOR_BG_CARD);
                c.setForeground(COLOR_TEXT_PRIMARY);
                setHorizontalAlignment(SwingConstants.CENTER);

                if (column == 0) {
                    c.setForeground(COLOR_CYAN_ACCENT);
                    setFont(new Font("Consolas", Font.BOLD, 11));
                } else if (column == 17) {
                    c.setForeground(COLOR_TEXT_MUTED);
                    setFont(new Font("Consolas", Font.PLAIN, 11));
                }
                return c;
            }
        };

        for (int i = 0; i < memoryTable.getColumnCount(); i++) {
            memoryTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        JScrollPane sp = new JScrollPane(memoryTable);
        sp.setPreferredSize(new Dimension(800, 200));
        sp.setBorder(new LineBorder(COLOR_CARD_BORDER, 1));
        sp.setBackground(COLOR_BG_CARD);

        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    /**
     * Bottom Status Bar
     */
    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(COLOR_BG_HEADER);
        bar.setPreferredSize(new Dimension(1200, 26));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_CARD_BORDER));

        // Left Status
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        left.setOpaque(false);

        JLabel greenDot = new JLabel("● Ready");
        greenDot.setFont(new Font("Segoe UI", Font.BOLD, 11));
        greenDot.setForeground(COLOR_GREEN_ACCENT);

        statusInfoLabel = new JLabel("Ln 1, Col 1  |  UTF-8  |  8085 Assembly  |  Instructions: 0  |  Cycles: 0");
        statusInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusInfoLabel.setForeground(COLOR_TEXT_MUTED);

        left.add(greenDot);
        left.add(statusInfoLabel);
        bar.add(left, BorderLayout.WEST);

        // Center State Badge
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

        // Right Info
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        right.setOpaque(false);

        JLabel themeLbl = new JLabel("Theme: Black  |  Memory: 64 KB  |  📶 100%");
        themeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        themeLbl.setForeground(COLOR_TEXT_MUTED);

        right.add(themeLbl);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    /**
     * Synchronize and Refresh UI components with underlying simulator state (Matrix / Assembler)
     */
    /**
     * Override updateUI so that FlatLaf theme changes do NOT reset our
     * hardcoded dark color scheme on cards, labels, and panels.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        // After theme update, re-assert our dark background colors
        if (COLOR_BG_DARK != null) {
            setBackground(COLOR_BG_DARK);
        }
        // Re-apply dark background to all known child panels / labels
        reapplyColorScheme();
    }

    /**
     * Re-applies our hardcoded color scheme to all components
     * after an LAF update (e.g. after a theme switch).
     */
    public void reapplyColorScheme() {
        try {
            setBackground(COLOR_BG_DARK);
            // Re-force the text editor background
            if (assembler != null && assembler.textEditor != null && assembler.textEditor.jTextPane1 != null) {
                assembler.textEditor.jTextPane1.setBackground(new Color(0x1E, 0x1E, 0x1E));
                assembler.textEditor.jTextPane1.setForeground(new Color(0xD4, 0xD4, 0xD4));
                assembler.textEditor.jTextPane1.setCaretColor(Color.WHITE);
                if (assembler.textEditor.jScrollPane1 != null) {
                    assembler.textEditor.jScrollPane1.setBackground(new Color(0x1E, 0x1E, 0x1E));
                    assembler.textEditor.jScrollPane1.getViewport().setBackground(new Color(0x1E, 0x1E, 0x1E));
                }
            }
            // Re-style register value labels
            if (regValLabels != null) {
                for (JLabel lbl : regValLabels) {
                    if (lbl != null) {
                        lbl.setForeground(COLOR_TEXT_PRIMARY);
                        lbl.setBackground(COLOR_BG_CARD);
                    }
                }
            }
            if (regRowPanels != null) {
                for (int i = 0; i < regRowPanels.length; i++) {
                    if (regRowPanels[i] != null) {
                        regRowPanels[i].setBackground(i == 0 ? COLOR_ROW_ACTIVE : COLOR_BG_CARD);
                    }
                }
            }
            // Re-apply status badge colors
            if (statusStateBadge != null) {
                statusStateBadge.setForeground(COLOR_TEXT_PRIMARY);
                statusStateBadge.setBackground(COLOR_PRIMARY_BLUE);
                statusStateBadge.setOpaque(true);
            }
            // Re-apply flag circles
            if (flagCircles != null) {
                for (JLabel lbl : flagCircles) {
                    if (lbl != null) {
                        lbl.setBackground(COLOR_CARD_BORDER);
                        lbl.setForeground(COLOR_TEXT_MUTED);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public void refreshData() {
        if (assembler == null || assembler.matrix == null) return;
        Matrix m = assembler.matrix;

        // 1. Registers
        int mVal = 0;
        try {
            int hlAddr = ((m.H & 0xFF) << 8) | (m.L & 0xFF);
            if (hlAddr >= 0 && hlAddr < m.memory.length) {
                mVal = m.memory[hlAddr] & 0xFF;
            }
        } catch (Exception ignored) {}

        int[] vals = {m.A & 0xFF, m.B & 0xFF, m.C & 0xFF, m.D & 0xFF, m.E & 0xFF, m.H & 0xFF, m.L & 0xFF, mVal};
        for (int i = 0; i < 8; i++) {
            if (regValLabels[i] != null) {
                regValLabels[i].setText(assembler.engine.Dec2Hex2digit(vals[i]));
            }
        }

        // 2. Flags (S, Z, AC, P, CY)
        int f = m.F;
        boolean s = (f & 0x80) != 0;
        boolean z = (f & 0x40) != 0;
        boolean ac = (f & 0x10) != 0;
        boolean p = (f & 0x04) != 0;
        boolean cy = (f & 0x01) != 0;

        boolean[] flags = {s, z, ac, p, cy};
        for (int i = 0; i < 5; i++) {
            if (flagCircles[i] != null) {
                flagCircles[i].setBackground(flags[i] ? COLOR_PRIMARY_BLUE : COLOR_CARD_BORDER);
                flagCircles[i].setForeground(flags[i] ? COLOR_TEXT_PRIMARY : COLOR_TEXT_MUTED);
            }
        }

        // 3. Pointers
        if (lblPcVal != null) lblPcVal.setText(assembler.engine.Dec2Hex(m.PC));
        if (lblSpVal != null) lblSpVal.setText(assembler.engine.Dec2Hex(m.SP));
        if (lblHlVal != null) lblHlVal.setText(assembler.engine.Dec2Hex(((m.H & 0xFF) << 8) | (m.L & 0xFF)));
        if (lblBcVal != null) lblBcVal.setText(assembler.engine.Dec2Hex(((m.B & 0xFF) << 8) | (m.C & 0xFF)));
        if (lblDeVal != null) lblDeVal.setText(assembler.engine.Dec2Hex(((m.D & 0xFF) << 8) | (m.E & 0xFF)));

        // 4. Execution Details
        if (lblPcExec != null) lblPcExec.setText(assembler.engine.Dec2Hex(m.PC));
        if (lblInstrCount != null) lblInstrCount.setText(String.valueOf(m.instructionCounter));
        if (lblCyclesCount != null) lblCyclesCount.setText(String.valueOf(m.clockCycleCounter));

        // State Badge
        if (statusStateBadge != null) {
            if (assembler.stop) {
                statusStateBadge.setText(" STOPPED ");
                statusStateBadge.setBackground(COLOR_PRIMARY_BLUE);
            } else {
                statusStateBadge.setText(" RUNNING ");
                statusStateBadge.setBackground(COLOR_GREEN_ACCENT);
            }
        }

        // Status Bar Info
        if (statusInfoLabel != null) {
            statusInfoLabel.setText("Ln 1, Col 1  |  UTF-8  |  8085 Assembly  |  Instructions: " + m.instructionCounter + "  |  Cycles: " + m.clockCycleCounter);
        }

        // 5. Memory Table Refresh (First 256 bytes starting at beginAddress)
        if (memoryTableModel != null) {
            int base = m.beginAddress & 0xFFF0;
            for (int r = 0; r < 16; r++) {
                int rowAddr = base + r * 16;
                memoryTableModel.setValueAt(assembler.engine.Dec2Hex(rowAddr), r, 0);

                StringBuilder asciiStr = new StringBuilder();
                for (int c = 0; c < 16; c++) {
                    int addr = rowAddr + c;
                    int val = m.memory[addr & 0xFFFF] & 0xFF;
                    memoryTableModel.setValueAt(assembler.engine.Dec2Hex2digit(val), r, c + 1);

                    char asciiChar = (val >= 32 && val <= 126) ? (char) val : '.';
                    asciiStr.append(asciiChar);
                }
                memoryTableModel.setValueAt(asciiStr.toString(), r, 17);
            }
        }
    }
}
