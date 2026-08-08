import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SettingsDialog extends JDialog {

    private static final Color COLOR_BG_DARK       = new Color(0x0A, 0x0C, 0x10);
    private static final Color COLOR_BG_CARD       = new Color(0x14, 0x16, 0x1D);
    private static final Color COLOR_BG_HEADER     = new Color(0x11, 0x13, 0x18);
    private static final Color COLOR_CARD_BORDER   = new Color(0x23, 0x27, 0x33);
    private static final Color COLOR_PRIMARY_BLUE = new Color(0x0D, 0x6E, 0xFD);
    private static final Color COLOR_CYAN_ACCENT   = new Color(0x0D, 0xCA, 0xF0);
    private static final Color COLOR_TEXT_PRIMARY  = new Color(0xED, 0xF2, 0xF7);
    private static final Color COLOR_TEXT_MUTED    = new Color(0x8C, 0x98, 0xA8);

    private final Assembler assembler;
    private JTextField txtStopMnemonic;
    private JTextField txtStepDelay;
    private JTextField txtMemBegin;
    private JTextField txtMemStop;
    private JTextField txtBeginFrom;

    public SettingsDialog(Assembler asm) {
        super((Frame) null, "Aura Studio Settings", true);
        this.assembler = asm;

        setSize(480, 520);
        setLocationRelativeTo(asm);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(COLOR_BG_DARK);

        // Header
        mainPanel.add(createHeader(), BorderLayout.NORTH);

        // Content
        mainPanel.add(createFormContent(), BorderLayout.CENTER);

        // Footer Actions
        mainPanel.add(createFooter(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_BG_HEADER);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_CARD_BORDER),
            new EmptyBorder(14, 20, 14, 20)
        ));

        JLabel lblTitle = new JLabel("⚙  Simulator Settings");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(COLOR_TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Configure execution timing, memory boundaries & opcodes");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(COLOR_TEXT_MUTED);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        textPanel.setOpaque(false);
        textPanel.add(lblTitle);
        textPanel.add(lblSub);

        header.add(textPanel, BorderLayout.CENTER);
        return header;
    }

    private JPanel createFormContent() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(COLOR_BG_DARK);
        container.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Group 1: Execution & Timing
        container.add(createSectionTitle("EXECUTION & TIMING"));
        container.add(Box.createVerticalStrut(8));

        String currentMnemonic = "HLT";
        if (assembler != null && assembler.engine != null) {
            try {
                currentMnemonic = assembler.engine.S[assembler.stopAtIndex];
            } catch (Exception ignored) {}
        }
        txtStopMnemonic = createStyledTextField(currentMnemonic);
        txtStepDelay    = createStyledTextField(assembler != null ? String.valueOf(assembler.speed[0]) : "0.0");

        container.add(createFormField("Stop Mnemonic (HLT)", txtStopMnemonic, "Opcode used to halt execution (default: HLT)"));
        container.add(Box.createVerticalStrut(10));
        container.add(createFormField("Step Execution Delay (sec)", txtStepDelay, "Delay between step execution cycles in seconds"));

        container.add(Box.createVerticalStrut(16));

        // Group 2: Memory Range Settings
        container.add(createSectionTitle("MEMORY BOUNDARIES"));
        container.add(Box.createVerticalStrut(8));

        txtMemBegin  = createStyledTextField(assembler != null && assembler.jTextFieldMemBegin  != null ? assembler.jTextFieldMemBegin.getText()  : "C000");
        txtMemStop   = createStyledTextField(assembler != null && assembler.jTextFieldMemStop   != null ? assembler.jTextFieldMemStop.getText()   : "CFFF");
        txtBeginFrom = createStyledTextField(assembler != null && assembler.jTextFieldBeginFrom != null ? assembler.jTextFieldBeginFrom.getText() : "C000");

        container.add(createFormField("Memory Start Address (Hex)", txtMemBegin,  "Lowest valid memory address (e.g. C000)"));
        container.add(Box.createVerticalStrut(10));
        container.add(createFormField("Memory End Address (Hex)",   txtMemStop,   "Highest valid memory address (e.g. CFFF)"));
        container.add(Box.createVerticalStrut(10));
        container.add(createFormField("Program Counter (PC) Start",  txtBeginFrom, "Initial execution starting address (e.g. C000)"));

        return container;
    }

    private JLabel createSectionTitle(String title) {
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(COLOR_CYAN_ACCENT);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel createFormField(String labelText, JTextField field, String tip) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(COLOR_TEXT_PRIMARY);

        JLabel lblTip = new JLabel(tip);
        lblTip.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblTip.setForeground(COLOR_TEXT_MUTED);

        panel.add(lbl, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        panel.add(lblTip, BorderLayout.SOUTH);
        return panel;
    }

    private JTextField createStyledTextField(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(new Font("Consolas", Font.PLAIN, 12));
        tf.setForeground(COLOR_TEXT_PRIMARY);
        tf.setBackground(COLOR_BG_CARD);
        tf.setCaretColor(COLOR_CYAN_ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_CARD_BORDER, 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return tf;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setBackground(COLOR_BG_HEADER);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_CARD_BORDER));

        JButton btnReset = new JButton("Reset Defaults");
        styleBtn(btnReset, COLOR_BG_CARD, COLOR_TEXT_MUTED);
        btnReset.addActionListener(e -> {
            txtStopMnemonic.setText("HLT");
            txtStepDelay.setText("0.0");
            txtMemBegin.setText("C000");
            txtMemStop.setText("CFFF");
            txtBeginFrom.setText("C000");
        });

        JButton btnCancel = new JButton("Cancel");
        styleBtn(btnCancel, COLOR_BG_CARD, COLOR_TEXT_PRIMARY);
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = new JButton("Save & Apply");
        styleBtn(btnSave, COLOR_PRIMARY_BLUE, Color.WHITE);
        btnSave.addActionListener(e -> applyAndSave());

        footer.add(btnReset);
        footer.add(btnCancel);
        footer.add(btnSave);
        return footer;
    }

    private void styleBtn(JButton b, Color bg, Color fg) {
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(fg);
        b.setBackground(bg);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(COLOR_CARD_BORDER, 1, true),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (b.getBackground() != COLOR_PRIMARY_BLUE) {
                    b.setBackground(new Color(0x22, 0x26, 0x34));
                }
            }
            @Override public void mouseExited(MouseEvent e) {
                b.setBackground(bg);
            }
        });
    }

    private void applyAndSave() {
        if (assembler != null) {
            try {
                // 1. Update Stop Mnemonic
                String m = txtStopMnemonic.getText().trim();
                if (assembler.engine != null) {
                    int idx = assembler.engine.getIndexFromMnemonic(m);
                    if (idx >= 0) {
                        assembler.stopAtIndex = idx;
                    }
                }

                // 2. Update Step Delay
                try {
                    float delay = Float.parseFloat(txtStepDelay.getText().trim());
                    assembler.speed[0] = delay;
                } catch (Exception ignored) {}

                // 3. Update Memory Addresses
                if (assembler.jTextFieldMemBegin != null) {
                    assembler.jTextFieldMemBegin.setText(txtMemBegin.getText().trim());
                }
                if (assembler.jTextFieldMemStop != null) {
                    assembler.jTextFieldMemStop.setText(txtMemStop.getText().trim());
                }
                if (assembler.jTextFieldBeginFrom != null) {
                    assembler.jTextFieldBeginFrom.setText(txtBeginFrom.getText().trim());
                }

                assembler.saveSettings();
                assembler.loadSettings();

            } catch (Exception ex) {
                System.err.println("Error saving settings: " + ex);
            }
        }
        dispose();
    }
}
