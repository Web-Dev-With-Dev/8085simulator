import codecs

with codecs.open(r'D:\Aura Studio\8085simulator\src\ModernIDEUI.java', 'r', encoding='utf-8') as f:
    text = f.read()

# Add Editor button to Action Bar next to Disassemble
old_btn = '''        JButton btnDisassemble = makeBtn("Disassemble", "↔", e -> {
            showEditorView(); // switches to WORKSPACE
            if (assembler.jTabbedPaneAssemblerEditor != null) assembler.jTabbedPaneAssemblerEditor.setSelectedIndex(1);
            if (assembler.jButtonDisassemble != null) { 
                assembler.jButtonDisassemble.setVisible(true); 
                assembler.jButtonDisassemble.doClick(); 
            }
        });
        bar.add(btnDisassemble);
        addSep(bar);'''

new_btn = '''        JButton btnCode = makeBtn("Code", "📝", e -> {
            showEditorView(); // switches to WORKSPACE
            if (assembler.jTabbedPaneAssemblerEditor != null) assembler.jTabbedPaneAssemblerEditor.setSelectedIndex(0);
        });
        bar.add(btnCode);
        
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

text = text.replace(old_btn, new_btn)

with codecs.open(r'D:\Aura Studio\8085simulator\src\ModernIDEUI.java', 'w', encoding='utf-8') as f:
    f.write(text)

print('Added Code button')
