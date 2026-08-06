import io
import codecs
import re

with codecs.open(r'D:\Aura Studio\8085simulator\src\ModernIDEUI.java', 'r', encoding='utf-8') as f:
    text = f.read()

# Chunk 1: Update items array to remove Disassembler and Settings
text = text.replace(
'''        String[][] items = {
            {"Editor",       "📝"},
            {"Registers",    "▦" },
            {"Memory",       "⊞" },
            {"Devices",      "⌨" },
            {"Subroutine",   "↳" },
            {"Interrupts",   "⚡" },
            {"I/O Port",     "⇄" },
            {"Disassembler", "↔" },
            {"Settings",     "⚙" }
        };''',
'''        String[][] items = {
            {"Editor",       "📝"},
            {"Registers",    "▦" },
            {"Memory",       "⊞" },
            {"Devices",      "⌨" },
            {"Subroutine",   "↳" },
            {"Interrupts",   "⚡" },
            {"I/O Port",     "⇄" }
        };'''
)

# Chunk 2: Add Disassemble button to Action Bar next to Assemble
old_assemble = '''        // FIX #5 – Assemble with animated colour feedback
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
        addSep(bar);'''

new_assemble = '''        // FIX #5 – Assemble with animated colour feedback
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
        
        JButton btnDisassemble = makeBtn("Disassemble", "↔", e -> {
            showEditorView(); // switches to WORKSPACE
            if (assembler.jTabbedPaneAssemblerEditor != null) assembler.jTabbedPaneAssemblerEditor.setSelectedIndex(1);
            if (assembler.jButtonDisassemble != null) { 
                assembler.jButtonDisassemble.setVisible(true); 
                assembler.jButtonDisassemble.doClick(); 
            }
        });
        bar.add(btnDisassemble);
        addSep(bar);'''

text = text.replace(old_assemble, new_assemble)

# Chunk 3: handleSidebarAction for I/O Port and remove Disassembler/Settings
old_handle_action = '''    // FIX #1 – Pop up the panels in a separate container so they show correctly
    private void handleSidebarAction(String id) {
        switch (id) {
            case "Editor":       showEditorView(); break;
            case "Registers":    showToolsDialog(0, "Registers"); break;
            case "Memory":       showToolsDialog(1, "Memory");    break;
            case "Devices":      showToolsDialog(2, "Devices");   break;
            case "Subroutine":   assembler.openDelaySubroutine();     break;
            case "Interrupts":   assembler.openInterruptSubroutine(); break;
            case "I/O Port":     assembler.openIOPortPanel();         break;
            case "Disassembler": 
                showEditorView(); // switches to WORKSPACE
                if (assembler.jTabbedPaneAssemblerEditor != null) assembler.jTabbedPaneAssemblerEditor.setSelectedIndex(1);
                if (assembler.jButtonDisassemble != null) { assembler.jButtonDisassemble.setVisible(true); assembler.jButtonDisassemble.doClick(); }
                break;
            case "Settings":     assembler.openSettings();            break;
            default:             showEditorView();
        }
    }'''

new_handle_action = '''    private JDialog ioPortDialog;
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
        if (assembler.jCheckBoxMenuItemIOPort != null) {
            if (!assembler.jCheckBoxMenuItemIOPort.isSelected()) {
                assembler.jCheckBoxMenuItemIOPort.setSelected(true);
            }
            // Trigger logic if needed
            assembler.jCheckBoxMenuItemIOPort.doClick();
        }
        ioPortDialog.setVisible(true);
        ioPortDialog.toFront();
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
            case "I/O Port":     showIOPortDialog();                  break;
            default:             showEditorView();
        }
    }'''

text = text.replace(old_handle_action, new_handle_action)

with codecs.open(r'D:\Aura Studio\8085simulator\src\ModernIDEUI.java', 'w', encoding='utf-8') as f:
    f.write(text)

print('Rewrite of ModernIDEUI complete.')
