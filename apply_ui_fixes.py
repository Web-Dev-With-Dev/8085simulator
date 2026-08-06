import io
import codecs

with codecs.open(r'D:\Aura Studio\8085simulator\src\ModernIDEUI.java', 'r', encoding='utf-8') as f:
    text = f.read()

# Chunk 1: Header logo
text = text.replace(
'''        // FIX #3 – Java2D logo, no emoji, no line through the A
        JLabel logoIcon = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // glow ring
                g2.setColor(new Color(0x00, 0x78, 0xD4, 55));
                g2.fillOval(0, 0, 28, 28);
                // gradient fill
                g2.setPaint(new GradientPaint(0, 0, new Color(0x00,0x9A,0xFF), 28, 28, new Color(0x00,0x50,0xAA)));
                g2.fillOval(3, 3, 22, 22);
                // letter A (no crossbar line hack - just clean bold A)
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                String s = "A";
                g2.drawString(s, (28 - fm.stringWidth(s)) / 2, (28 + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        logoIcon.setPreferredSize(new Dimension(28, 28));''',
'''        // FIX #3 - Revert to old logo as requested
        JLabel logoIcon = new JLabel();
        try {
            java.awt.image.BufferedImage raw = javax.imageio.ImageIO.read(
                getClass().getResourceAsStream("/aura_logo.dat"));
            if (raw != null) {
                java.awt.Image scaled = raw.getScaledInstance(28, 28, java.awt.Image.SCALE_SMOOTH);
                logoIcon.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception ignored) {}
        logoIcon.setPreferredSize(new Dimension(28, 28));'''
)

# Chunk 2: Action Bar Buttons
text = text.replace(
'''        bar.add(makeBtn("New",  "[+]", e -> {
            assembler.jTextAreaAssemblyLanguageEditor.setText("; New 8085 Program\\n\\nMVI A, 00H\\nHLT\\n");
            assembler.textEditor.colorEditor();
            setTabTitle("untitled.asm");
            showEditorView();
        }));
        bar.add(makeBtn("Open", "[O]", e -> {
            if (assembler.jMenuItemLoad_Assembly_Language_code != null)
                assembler.jMenuItemLoad_Assembly_Language_code.doClick();
        }));
        bar.add(makeBtn("Save", "[S]", e -> {
            if (assembler.jMenuItemSave_Assembly_Language_code != null)
                assembler.jMenuItemSave_Assembly_Language_code.doClick();
            markSaved();
        }));
        addSep(bar);

        // FIX #5 – Assemble with animated colour feedback
        btnAssembleBar = makeBtn("Assemble", "[=]", e -> {
            showEditorView();
            btnAssembleBar.setText("[..] Assembling...");
            btnAssembleBar.setBackground(COLOR_AMBER_ACCENT);
            btnAssembleBar.setForeground(new Color(0x30, 0x20, 0x00));
            btnAssembleBar.setEnabled(false);
            SwingUtilities.invokeLater(() -> {
                if (assembler.jButtonAssemble != null) assembler.jButtonAssemble.doClick();
                SwingUtilities.invokeLater(() -> {
                    btnAssembleBar.setEnabled(true);
                    btnAssembleBar.setText("[OK] Assembled");
                    btnAssembleBar.setBackground(COLOR_GREEN_ACCENT);
                    btnAssembleBar.setForeground(new Color(0x00, 0x25, 0x00));
                });
            });
        });
        bar.add(btnAssembleBar);
        addSep(bar);

        // FIX #5 – Run with colour state
        btnRunBar = new JButton("[>] Run");
        styleRunBtn(btnRunBar, COLOR_PRIMARY_BLUE, Color.WHITE);
        btnRunBar.addActionListener(e -> {
            showEditorView();
            if (assembler.jButtonRun != null) assembler.jButtonRun.doClick();
            setExecutionState("RUNNING");
        });
        bar.add(btnRunBar);

        bar.add(makeBtn("Step",      "[>>]", e -> { showDebuggerView(); if (assembler.jButtonStep     != null) assembler.jButtonStep.doClick(); }));
        bar.add(makeBtn("Fwd",       "[>|]", e -> { showDebuggerView(); if (assembler.jButtonForward  != null) assembler.jButtonForward.doClick(); }));
        bar.add(makeBtn("Back",      "[|<]", e -> { showDebuggerView(); if (assembler.jButtonBackward != null) assembler.jButtonBackward.doClick(); }));
        bar.add(makeBtn("Pause",     "[||]", e -> {
            if (assembler.jButtonStop != null) { assembler.jButtonStop.setVisible(true); assembler.jButtonStop.setText("Pause"); assembler.jButtonStop.doClick(); }
            setExecutionState("PAUSED");
        }));
        bar.add(makeBtn("Stop",      "[X]",  e -> {
            if (assembler.jButtonStop != null) { assembler.jButtonStop.setVisible(true); assembler.jButtonStop.setText("Stop");  assembler.jButtonStop.doClick(); }
            setExecutionState("STOPPED");
        }));
        bar.add(makeBtn("Reset",     "[R]",  e -> {
            if (assembler.jMenuItemClearMemory != null) assembler.jMenuItemClearMemory.doClick();
            btnAssembleBar.setText("[=] Assemble");
            btnAssembleBar.setBackground(COLOR_BG_CARD);
            btnAssembleBar.setForeground(COLOR_TEXT_PRIMARY);
            setExecutionState("STOPPED");
        }));
        bar.add(makeBtn("Fix",       "[*]",  e -> { showEditorView(); if (assembler.jButtonAutocorrect != null) assembler.jButtonAutocorrect.doClick(); }));''',
'''        bar.add(makeBtn("New",  "📄", e -> {
            assembler.jTextAreaAssemblyLanguageEditor.setText("; New 8085 Program\\n\\nMVI A, 00H\\nHLT\\n");
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

        // FIX #5 – Assemble with animated colour feedback
        btnAssembleBar = makeBtn("Assemble", "⚒", e -> {
            showEditorView();
            btnAssembleBar.setText("⏳ Assembling...");
            btnAssembleBar.setBackground(COLOR_AMBER_ACCENT);
            btnAssembleBar.setForeground(new Color(0x30, 0x20, 0x00));
            btnAssembleBar.setEnabled(false);
            SwingUtilities.invokeLater(() -> {
                if (assembler.jButtonAssemble != null) assembler.jButtonAssemble.doClick();
                SwingUtilities.invokeLater(() -> {
                    btnAssembleBar.setEnabled(true);
                    btnAssembleBar.setText("✔️ Assembled");
                    btnAssembleBar.setBackground(COLOR_GREEN_ACCENT);
                    btnAssembleBar.setForeground(new Color(0x00, 0x25, 0x00));
                });
            });
        });
        bar.add(btnAssembleBar);
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

        bar.add(makeBtn("Step",      "⏭", e -> { showDebuggerView(); if (assembler.jButtonStep     != null) assembler.jButtonStep.doClick(); }));
        bar.add(makeBtn("Fwd",       "⏩", e -> { showDebuggerView(); if (assembler.jButtonForward  != null) assembler.jButtonForward.doClick(); }));
        bar.add(makeBtn("Back",      "⏪", e -> { showDebuggerView(); if (assembler.jButtonBackward != null) assembler.jButtonBackward.doClick(); }));
        bar.add(makeBtn("Pause",     "⏸", e -> {
            if (assembler.jButtonStop != null) { assembler.jButtonStop.setVisible(true); assembler.jButtonStop.setText("Pause"); assembler.jButtonStop.doClick(); }
            setExecutionState("PAUSED");
        }));
        bar.add(makeBtn("Stop",      "⏹",  e -> {
            if (assembler.jButtonStop != null) { assembler.jButtonStop.setVisible(true); assembler.jButtonStop.setText("Stop");  assembler.jButtonStop.doClick(); }
            setExecutionState("STOPPED");
        }));
        bar.add(makeBtn("Reset",     "↻",  e -> {
            if (assembler.jMenuItemClearMemory != null) assembler.jMenuItemClearMemory.doClick();
            btnAssembleBar.setText("⚒ Assemble");
            btnAssembleBar.setBackground(COLOR_BG_CARD);
            btnAssembleBar.setForeground(COLOR_TEXT_PRIMARY);
            setExecutionState("STOPPED");
        }));
        bar.add(makeBtn("Fix",       "✨",  e -> { showEditorView(); if (assembler.jButtonAutocorrect != null) assembler.jButtonAutocorrect.doClick(); }));'''
)

# Chunk 3: makeBtn Font
text = text.replace(
'''    // FIX #2 – Bracket-text icons (Consolas renders these reliably everywhere)
    private JButton makeBtn(String text, String icon, java.awt.event.ActionListener l) {
        JButton btn = new JButton(icon + " " + text);
        btn.setFont(new Font("Consolas", Font.PLAIN, 12));''',
'''    // FIX #2 – Graphical unicode icons
    private JButton makeBtn(String text, String icon, java.awt.event.ActionListener l) {
        JButton btn = new JButton(icon + " " + text);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));'''
)

# Chunk 4: styleRunBtn Font
text = text.replace(
'''    private void styleRunBtn(JButton b, Color bg, Color fg) {
        b.setFont(new Font("Consolas", Font.BOLD, 12));''',
'''    private void styleRunBtn(JButton b, Color bg, Color fg) {
        b.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));'''
)

# Chunk 5: Sidebar array
text = text.replace(
'''        // id, label, short-icon
        String[][] items = {
            {"Editor",       "Ed" },
            {"Registers",    "Reg"},
            {"Memory",       "Mem"},
            {"Devices",      "Dev"},
            {"Subroutine",   "Sub"},
            {"Interrupts",   "IRQ"},
            {"I/O Port",     "I/O"},
            {"Disassembler", "Dis"},
            {"Settings",     "Cfg"}
        };''',
'''        // id, label, short-icon
        String[][] items = {
            {"Editor",       "📝"},
            {"Registers",    "▦" },
            {"Memory",       "⊞" },
            {"Devices",      "⌨" },
            {"Subroutine",   "↳" },
            {"Interrupts",   "⚡" },
            {"I/O Port",     "⇄" },
            {"Disassembler", "↔" },
            {"Settings",     "⚙" }
        };'''
)

# Chunk 6: Sidebar layout
text = text.replace(
'''        JLabel lblIcon = new JLabel(icon, SwingConstants.CENTER);
        lblIcon.setFont(new Font("Consolas", Font.BOLD, 13));
        lblIcon.setForeground(active ? Color.WHITE : COLOR_CYAN_ACCENT);
        lblIcon.setOpaque(false);

        String display = id.length() > 8 ? id.substring(0, 7) + "." : id;
        JLabel lblText = new JLabel(display, SwingConstants.CENTER);
        lblText.setFont(new Font("Segoe UI", Font.PLAIN, 8));
        lblText.setForeground(active ? COLOR_TEXT_PRIMARY : COLOR_TEXT_MUTED);
        lblText.setOpaque(false);

        JPanel inner = new JPanel(new BorderLayout(0, 2));
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        inner.add(lblIcon, BorderLayout.CENTER);
        inner.add(lblText, BorderLayout.SOUTH);
        btn.add(inner, BorderLayout.CENTER);''',
'''        JLabel lblIcon = new JLabel(icon, SwingConstants.CENTER);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        lblIcon.setForeground(active ? Color.WHITE : COLOR_CYAN_ACCENT);
        lblIcon.setOpaque(false);
        lblIcon.setToolTipText(id);

        JPanel inner = new JPanel(new BorderLayout(0, 2));
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        inner.add(lblIcon, BorderLayout.CENTER);
        btn.add(inner, BorderLayout.CENTER);'''
)

# Chunk 7: Dialog for tools
text = text.replace(
'''    // FIX #1 – Every sidebar button calls the correct Assembler method
    private void handleSidebarAction(String id) {
        switch (id) {
            case "Editor":       showEditorView(); break;
            case "Registers":    assembler.showRegistersPanel(); break;
            case "Memory":       assembler.showMemoryPanel();    break;
            case "Devices":      assembler.showDevicesPanel();   break;
            case "Subroutine":   assembler.openDelaySubroutine();     break;
            case "Interrupts":   assembler.openInterruptSubroutine(); break;
            case "I/O Port":     assembler.openIOPortPanel();         break;
            case "Disassembler": assembler.openDisassemblerTab();     break;
            case "Settings":     assembler.openSettings();            break;
            default:             showEditorView();
        }
    }''',
'''    private JDialog toolsDialog;
    private void showToolsDialog(int tabIndex, String title) {
        if (assembler.jTabbedPaneMemory == null) return;
        if (toolsDialog == null) {
            toolsDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Tools", Dialog.ModalityType.MODELESS);
            toolsDialog.setSize(700, 500);
            toolsDialog.setLocationRelativeTo(this);
            toolsDialog.setLayout(new BorderLayout());
            toolsDialog.add(assembler.jTabbedPaneMemory, BorderLayout.CENTER);
            toolsDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        }
        toolsDialog.setTitle(title);
        if (assembler.jTabbedPaneMemory.getTabCount() > tabIndex) {
            assembler.jTabbedPaneMemory.setSelectedIndex(tabIndex);
        }
        toolsDialog.setVisible(true);
        toolsDialog.toFront();
    }

    // FIX #1 – Pop up the panels in a separate container so they show correctly
    private void handleSidebarAction(String id) {
        switch (id) {
            case "Editor":       showEditorView(); break;
            case "Registers":    showToolsDialog(0, "Registers"); break;
            case "Memory":       showToolsDialog(1, "Memory");    break;
            case "Devices":      showToolsDialog(2, "Devices");   break;
            case "Subroutine":   assembler.openDelaySubroutine();     break;
            case "Interrupts":   assembler.openInterruptSubroutine(); break;
            case "I/O Port":     assembler.openIOPortPanel();         break;
            case "Disassembler": assembler.openDisassemblerTab();     break;
            case "Settings":     assembler.openSettings();            break;
            default:             showEditorView();
        }
    }'''
)

# Chunk 8: Welcome view logo
text = text.replace(
'''        // FIX #3 – Professional Java2D logo (no line through A, no emoji)
        JPanel logoPanel = new JPanel(null) {
            @Override public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2, cy = getHeight() / 2, r = 46;
                // outer glow
                g2.setColor(new Color(0x00, 0x78, 0xD4, 40));
                g2.fillOval(cx - r - 10, cy - r - 10, (r + 10) * 2, (r + 10) * 2);
                // ring
                g2.setColor(new Color(0x00, 0x78, 0xD4, 120));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(cx - r - 2, cy - r - 2, (r + 2) * 2, (r + 2) * 2);
                // gradient body
                g2.setPaint(new GradientPaint(cx - r, cy - r, new Color(0x00, 0x9A, 0xFF),
                                              cx + r, cy + r, new Color(0x00, 0x4A, 0xA0)));
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                // grid lines (subtle)
                g2.setColor(new Color(0xFF, 0xFF, 0xFF, 22));
                g2.setStroke(new BasicStroke(1f));
                for (int i = -r; i < r; i += 14) g2.drawLine(cx + i, cy - r, cx + i, cy + r);
                // Bold "A" — no crossbar trick, just a clean font "A"
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Black", Font.BOLD, 52));
                FontMetrics fm = g2.getFontMetrics();
                String sym = "A";
                g2.drawString(sym, cx - fm.stringWidth(sym) / 2, cy + fm.getAscent() / 2 - 4);
                // accent dots
                g2.setColor(COLOR_CYAN_ACCENT);
                int[][] dots = {{cx + r + 6, cy - 14}, {cx - r - 6, cy + 14}, {cx + 14, cy - r - 6}};
                for (int[] d : dots) g2.fillOval(d[0] - 3, d[1] - 3, 6, 6);
                g2.dispose();
            }
        };
        logoPanel.setOpaque(false);
        logoPanel.setPreferredSize(new Dimension(112, 112));
        p.add(logoPanel, gbc);''',
'''        // FIX #3 – Restore old logo here as well
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
        p.add(logoPanel, gbc);'''
)

# Chunk 9: setExecutionState
text = text.replace(
'''            case "RUNNING":
                statusStateBadge.setText(" RUNNING ");
                statusStateBadge.setBackground(COLOR_GREEN_ACCENT);
                if (btnRunBar != null) {
                    btnRunBar.setText("[||] Running");
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
                    btnRunBar.setText("[>] Run");
                    styleRunBtn(btnRunBar, COLOR_PRIMARY_BLUE, Color.WHITE);
                }''',
'''            case "RUNNING":
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
                }'''
)

# Chunk 10: welcome view buttons
text = text.replace(
'''        JButton btnNew = new JButton("+ Create New Program");
        btnNew.setFont(new Font("Segoe UI", Font.BOLD, 13));''',
'''        JButton btnNew = new JButton("➕ Create New Program");
        btnNew.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));'''
)
text = text.replace(
'''        JButton btnOpen = mkWelBtn("[O] Open Program");''',
'''        JButton btnOpen = mkWelBtn("📂 Open Program");'''
)
text = text.replace(
'''        JButton btnSample = mkWelBtn("[*] Open Sample");''',
'''        JButton btnSample = mkWelBtn("✨ Open Sample");'''
)
text = text.replace(
'''    private JButton mkWelBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));''',
'''    private JButton mkWelBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));'''
)

with codecs.open(r'D:\Aura Studio\8085simulator\src\ModernIDEUI.java', 'w', encoding='utf-8') as f:
    f.write(text)

print('Rewrite complete.')
