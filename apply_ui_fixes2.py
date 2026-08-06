import io
import codecs
import re

with codecs.open(r'D:\Aura Studio\8085simulator\src\ModernIDEUI.java', 'r', encoding='utf-8') as f:
    text = f.read()

# Chunk 1: Sidebar buttons to have both icon and label, as requested
text = text.replace(
'''        JLabel lblIcon = new JLabel(icon, SwingConstants.CENTER);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        lblIcon.setForeground(active ? Color.WHITE : COLOR_CYAN_ACCENT);
        lblIcon.setOpaque(false);
        lblIcon.setToolTipText(id);

        JPanel inner = new JPanel(new BorderLayout(0, 2));
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        inner.add(lblIcon, BorderLayout.CENTER);
        btn.add(inner, BorderLayout.CENTER);''',
'''        JLabel lblIcon = new JLabel(icon, SwingConstants.CENTER);
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
        btn.add(inner, BorderLayout.CENTER);'''
)

# Chunk 2: Action Bar -> Disassembler needs to show right split if we want it?
# Actually, let's redefine the Editor Container.
# First, let's find createEditorContainer. We'll replace the part that creates codeEditorPanel and debuggerPanel.

editor_container_old = '''        // EDITOR card
        codeEditorPanel = new JPanel(new BorderLayout());
        codeEditorPanel.setBackground(new Color(0x1E, 0x1E, 0x1E));
        javax.swing.JScrollPane es = assembler.getEditorScrollPane();
        if (es != null) {
            es.setBorder(null);
            es.setBackground(new Color(0x1E, 0x1E, 0x1E));
            es.getViewport().setBackground(new Color(0x1E, 0x1E, 0x1E));
            if (assembler.textEditor != null && assembler.textEditor.jTextPane1 != null) {
                assembler.textEditor.jTextPane1.setBackground(new Color(0x1E, 0x1E, 0x1E));
                assembler.textEditor.jTextPane1.setForeground(new Color(0xD4, 0xD4, 0xD4));
                assembler.textEditor.jTextPane1.setCaretColor(Color.WHITE);
                assembler.textEditor.jTextPane1.setEditable(true);
                assembler.textEditor.jTextPane1.getDocument().addDocumentListener(
                    new javax.swing.event.DocumentListener() {
                        public void insertUpdate(javax.swing.event.DocumentEvent e)  { unsavedDot.setVisible(true); }
                        public void removeUpdate(javax.swing.event.DocumentEvent e)  { unsavedDot.setVisible(true); }
                        public void changedUpdate(javax.swing.event.DocumentEvent e) {}
                    });
            }
            codeEditorPanel.add(es, BorderLayout.CENTER);
        }
        centerEditorContainer.add(codeEditorPanel, "EDITOR");

        // DEBUGGER card
        JPanel debuggerPanel = new JPanel(new BorderLayout());
        debuggerPanel.setBackground(COLOR_BG_DARK);
        javax.swing.JScrollPane ds = assembler.getDebuggerScrollPane();
        if (ds != null) {
            ds.setBorder(null);
            ds.setBackground(COLOR_BG_DARK);
            ds.getViewport().setBackground(COLOR_BG_DARK);
            debuggerExplainerLabel = new JLabel(
                "<html><b>Step Explainer:</b> Click Step Fwd to trace execution.</html>");
            debuggerExplainerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            debuggerExplainerLabel.setForeground(COLOR_CYAN_ACCENT);
            debuggerExplainerLabel.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
            debuggerExplainerLabel.setBackground(COLOR_BG_CARD);
            debuggerExplainerLabel.setOpaque(true);
            debuggerPanel.add(debuggerExplainerLabel, BorderLayout.NORTH);
            debuggerPanel.add(ds, BorderLayout.CENTER);
        }
        centerEditorContainer.add(debuggerPanel, "DEBUGGER");'''

editor_container_new = '''        // WORKSPACE card (Split Pane with Editor/Disassembler on Left, Debugger on Right)
        workspaceSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        workspaceSplitPane.setBorder(null);
        workspaceSplitPane.setBackground(COLOR_BG_DARK);
        
        // Left: Original Tabbed Pane (Assembler + Disassembler)
        if (assembler.jTabbedPaneAssemblerEditor != null) {
            assembler.jTabbedPaneAssemblerEditor.setBorder(null);
            workspaceSplitPane.setLeftComponent(assembler.jTabbedPaneAssemblerEditor);
            if (assembler.textEditor != null && assembler.textEditor.jTextPane1 != null) {
                assembler.textEditor.jTextPane1.setBackground(new Color(0x1E, 0x1E, 0x1E));
                assembler.textEditor.jTextPane1.setForeground(new Color(0xD4, 0xD4, 0xD4));
                assembler.textEditor.jTextPane1.setCaretColor(Color.WHITE);
                assembler.textEditor.jTextPane1.setEditable(true);
                assembler.textEditor.jTextPane1.getDocument().addDocumentListener(
                    new javax.swing.event.DocumentListener() {
                        public void insertUpdate(javax.swing.event.DocumentEvent e)  { unsavedDot.setVisible(true); }
                        public void removeUpdate(javax.swing.event.DocumentEvent e)  { unsavedDot.setVisible(true); }
                        public void changedUpdate(javax.swing.event.DocumentEvent e) {}
                    });
            }
        }
        
        // Right: Debugger Panel
        JPanel debuggerPanel = new JPanel(new BorderLayout());
        debuggerPanel.setBackground(COLOR_BG_DARK);
        javax.swing.JScrollPane ds = assembler.getDebuggerScrollPane();
        if (ds != null) {
            ds.setBorder(null);
            ds.setBackground(COLOR_BG_DARK);
            ds.getViewport().setBackground(COLOR_BG_DARK);
            debuggerExplainerLabel = new JLabel(
                "<html><b>Step Explainer:</b> Click Step Fwd to trace execution.</html>");
            debuggerExplainerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            debuggerExplainerLabel.setForeground(COLOR_CYAN_ACCENT);
            debuggerExplainerLabel.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
            debuggerExplainerLabel.setBackground(COLOR_BG_CARD);
            debuggerExplainerLabel.setOpaque(true);
            debuggerPanel.add(debuggerExplainerLabel, BorderLayout.NORTH);
            debuggerPanel.add(ds, BorderLayout.CENTER);
        }
        workspaceSplitPane.setRightComponent(debuggerPanel);
        
        // Hide right side by default
        workspaceSplitPane.setResizeWeight(1.0);
        
        centerEditorContainer.add(workspaceSplitPane, "WORKSPACE");'''

text = text.replace(editor_container_old, editor_container_new)

# Add JSplitPane variable declaration at the top of ModernIDEUI
text = text.replace('private JPanel     codeEditorPanel;', 'private JSplitPane workspaceSplitPane;')

# Update card switch helpers
text = text.replace(
'''    public void showEditorView() {
        if (editorCardLayout != null) editorCardLayout.show(centerEditorContainer, "EDITOR");
        if (assembler.textEditor != null && assembler.textEditor.jTextPane1 != null)
            assembler.textEditor.jTextPane1.requestFocusInWindow();
    }
    public void showDebuggerView() {
        if (editorCardLayout != null) editorCardLayout.show(centerEditorContainer, "DEBUGGER");
    }''',
'''    public void showEditorView() {
        if (editorCardLayout != null) editorCardLayout.show(centerEditorContainer, "WORKSPACE");
        if (workspaceSplitPane != null) {
            workspaceSplitPane.setDividerLocation(1.0); // Hide right panel
        }
        if (assembler.jTabbedPaneAssemblerEditor != null) {
            assembler.jTabbedPaneAssemblerEditor.setSelectedIndex(0); // Show Assembler
        }
        if (assembler.textEditor != null && assembler.textEditor.jTextPane1 != null)
            assembler.textEditor.jTextPane1.requestFocusInWindow();
    }
    public void showDebuggerView() {
        if (editorCardLayout != null) editorCardLayout.show(centerEditorContainer, "WORKSPACE");
        if (workspaceSplitPane != null) {
            workspaceSplitPane.setDividerLocation(0.65); // Show debugger beside editor
        }
    }'''
)

# Update sidebar action for Disassembler
text = text.replace(
'''            case "Disassembler": assembler.openDisassemblerTab();     break;''',
'''            case "Disassembler": 
                showEditorView(); // switches to WORKSPACE
                if (assembler.jTabbedPaneAssemblerEditor != null) assembler.jTabbedPaneAssemblerEditor.setSelectedIndex(1);
                if (assembler.jButtonDisassemble != null) { assembler.jButtonDisassemble.setVisible(true); assembler.jButtonDisassemble.doClick(); }
                break;'''
)

with codecs.open(r'D:\Aura Studio\8085simulator\src\ModernIDEUI.java', 'w', encoding='utf-8') as f:
    f.write(text)

print('Rewrite of ModernIDEUI complete.')
