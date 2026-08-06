import codecs
import re

with codecs.open(r'D:\Aura Studio\8085simulator\src\ModernIDEUI.java', 'r', encoding='utf-8') as f:
    text = f.read()

# 1. Hide Tab Headers of jTabbedPaneAssemblerEditor
# I'll inject this inside createEditorContainer right after workspaceSplitPane.setLeftComponent(assembler.jTabbedPaneAssemblerEditor);
old_left_comp = '''        // Left: Original Tabbed Pane (Assembler + Disassembler)
        if (assembler.jTabbedPaneAssemblerEditor != null) {
            assembler.jTabbedPaneAssemblerEditor.setBorder(null);
            workspaceSplitPane.setLeftComponent(assembler.jTabbedPaneAssemblerEditor);'''

new_left_comp = '''        // Left: Original Tabbed Pane (Assembler + Disassembler)
        if (assembler.jTabbedPaneAssemblerEditor != null) {
            assembler.jTabbedPaneAssemblerEditor.setBorder(null);
            // Completely hide the tab headers as requested by user (the red circled buttons)
            assembler.jTabbedPaneAssemblerEditor.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
                @Override
                protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
                    return 0;
                }
            });
            workspaceSplitPane.setLeftComponent(assembler.jTabbedPaneAssemblerEditor);'''
text = text.replace(old_left_comp, new_left_comp)

# 2. Fix Assemble button hover
old_assemble_btn = '''        // FIX #5 – Assemble with animated colour feedback
        btnAssembleBar = makeBtn("Assemble", "⚒", e -> {
            showEditorView();
            btnAssembleBar.setText("⏳ Assembling...");
            btnAssembleBar.setBackground(COLOR_AMBER_ACCENT);
            btnAssembleBar.setForeground(new Color(0x30, 0x20, 0x00));
            btnAssembleBar.setEnabled(false);'''

new_assemble_btn = '''        // FIX #5 – Assemble with animated colour feedback
        btnAssembleBar = makeBtn("Assemble", "⚒", e -> {
            showEditorView();
            btnAssembleBar.setText("⏳ Assembling...");
            btnAssembleBar.setBackground(COLOR_AMBER_ACCENT);
            btnAssembleBar.setForeground(new Color(0x30, 0x20, 0x00));
            btnAssembleBar.setEnabled(false);
            btnAssembleBar.putClientProperty("FlatLaf.style", "hoverBackground: null; pressedBackground: null;");'''
text = text.replace(old_assemble_btn, new_assemble_btn)

# Make sure it resets hover property on reset
old_reset_btn = '''        bar.add(makeBtn("Reset",     "↻",  e -> {
            if (assembler.jMenuItemClearMemory != null) assembler.jMenuItemClearMemory.doClick();
            btnAssembleBar.setText("⚒ Assemble");
            btnAssembleBar.setBackground(COLOR_BG_CARD);
            btnAssembleBar.setForeground(COLOR_TEXT_PRIMARY);
            setExecutionState("STOPPED");
        }));'''
new_reset_btn = '''        bar.add(makeBtn("Reset",     "↻",  e -> {
            if (assembler.jMenuItemClearMemory != null) assembler.jMenuItemClearMemory.doClick();
            btnAssembleBar.setText("⚒ Assemble");
            btnAssembleBar.setBackground(COLOR_BG_CARD);
            btnAssembleBar.setForeground(COLOR_TEXT_PRIMARY);
            btnAssembleBar.putClientProperty("FlatLaf.style", "");
            setExecutionState("STOPPED");
        }));'''
text = text.replace(old_reset_btn, new_reset_btn)

# 3. Add Settings removal check (just in case it was missed)
old_items = '''        String[][] items = {
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
new_items = '''        String[][] items = {
            {"Editor",       "📝"},
            {"Registers",    "▦" },
            {"Memory",       "⊞" },
            {"Devices",      "⌨" },
            {"Subroutine",   "↳" },
            {"Interrupts",   "⚡" }
        };'''
# This handles both cases if it wasn't replaced properly last time
text = text.replace(old_items, new_items)

# The user explicitly asked: "device button not work in this we have show Interfacing device"
# Also: "i/o port not work" and "and setting button not still remove from left pannal"
# Let's completely remove I/O port from sidebar, and put ALL interface stuff inside the Devices popup!
new_items2 = '''        String[][] items = {
            {"Editor",       "📝"},
            {"Registers",    "▦" },
            {"Memory",       "⊞" },
            {"Devices",      "⌨" },
            {"Subroutine",   "↳" },
            {"Interrupts",   "⚡" },
            {"I/O Port",     "⇄" }
        };'''
text = text.replace(new_items2, new_items) # Remove I/O port from sidebar too

# Fix handleSidebarAction to match
old_handle_action = '''    // FIX #1 – Pop up the panels in a separate container so they show correctly
    private void handleSidebarAction(String id) {
        switch (id) {
            case "Editor":       showEditorView(); break;
            case "Registers":    showToolsDialog(0, "Registers"); break;
            case "Memory":       showToolsDialog(1, "Memory");    break;
            case "Devices":      showToolsDialog(2, "Devices");   break;
            case "Subroutine":   assembler.openDelaySubroutine();     break;
            case "Interrupts":   assembler.openInterruptSubroutine(); break;
            case "I/O Port":     showIOPortDialog();                  break;
            case "Disassembler": 
                showEditorView(); // switches to WORKSPACE
                if (assembler.jTabbedPaneAssemblerEditor != null) assembler.jTabbedPaneAssemblerEditor.setSelectedIndex(1);
                if (assembler.jButtonDisassemble != null) { assembler.jButtonDisassemble.setVisible(true); assembler.jButtonDisassemble.doClick(); }
                break;
            case "Settings":     assembler.openSettings();            break;
            default:             showEditorView();
        }
    }'''
new_handle_action = '''    // FIX #1 – Pop up the panels in a separate container so they show correctly
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
                    if (assembler.jCheckBoxMenuItemIOPort != null && !assembler.jCheckBoxMenuItemIOPort.isSelected()) assembler.jCheckBoxMenuItemIOPort.doClick();
                    ioPortDialog.setVisible(true);
                    ioPortDialog.toFront();
                }
                break;
            case "Subroutine":   assembler.openDelaySubroutine();     break;
            case "Interrupts":   assembler.openInterruptSubroutine(); break;
            default:             showEditorView();
        }
    }'''
text = text.replace(old_handle_action, new_handle_action)
# Also check if it's the already-replaced version:
old_handle_action2 = '''    // FIX #1 – Pop up the panels in a separate container so they show correctly
    private void handleSidebarAction(String id) {
        switch (id) {
            case "Editor":       showEditorView(); break;
            case "Registers":    showToolsDialog(0, "Registers"); break;
            case "Memory":       showToolsDialog(1, "Memory");    break;
            case "Devices":      showToolsDialog(2, "Devices");   break;
            case "Subroutine":   assembler.openDelaySubroutine();     break;
            case "Interrupts":   assembler.openInterruptSubroutine(); break;
            case "I/O Port":     showIOPortDialog();                  break;
            default:             showEditorView();
        }
    }'''
text = text.replace(old_handle_action2, new_handle_action)

with codecs.open(r'D:\Aura Studio\8085simulator\src\ModernIDEUI.java', 'w', encoding='utf-8') as f:
    f.write(text)

print('Rewrite 4 complete.')
