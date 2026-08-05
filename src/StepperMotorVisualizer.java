import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * 8085 Stepper Motor Physical Motion Visualizer
 * Real-time I/O Port driven motor simulator tracking Coil A, B, C, D phase excitation,
 * rotor angle rotation, direction (CW/CCW), step count, and RPM calculation.
 */
public class StepperMotorVisualizer extends JFrame {

    private static StepperMotorVisualizer instance;
    private final Matrix matrix;
    private final Assembler assembler;

    private int motorPort = 0x00;
    private double currentAngle = 0.0; // Rotor angle in degrees
    private int stepCount = 0;
    private String direction = "STOPPED";
    private int lastPhaseMask = 0;

    // UI Components
    private MotorPanel motorPanel;
    private JComboBox<String> cbMotorPort;
    private JLabel lblStatus;
    private JLabel lblStepCount;
    private JLabel lblDirection;
    private JLabel[] coilLeds = new JLabel[4]; // Coil A, B, C, D

    private Timer refreshTimer;

    public StepperMotorVisualizer(Matrix matrix, Assembler assembler) {
        this.matrix = matrix;
        this.assembler = assembler;
        instance = this;

        setTitle(" 8085 Stepper Motor Motion Visualizer");
        setSize(780, 520);
        setLocationRelativeTo(assembler);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
        updateMotorState();

        // 30ms Live Refresh Timer for smooth rotor rotation animation
        refreshTimer = new Timer(30, e -> {
            if (isShowing()) {
                updateMotorState();
            }
        });
        refreshTimer.start();
    }

    public static StepperMotorVisualizer getInstance(Matrix matrix, Assembler assembler) {
        if (instance == null || !instance.isDisplayable()) {
            instance = new StepperMotorVisualizer(matrix, assembler);
        }
        return instance;
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        mainPanel.setBackground(new Color(0x0F, 0x17, 0x2A));

        // 1. TOP TITLE BANNER
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        JLabel titleLbl = new JLabel("8085 Stepper Motor Physical Motion Module", SwingConstants.LEFT);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLbl.setForeground(new Color(0x38, 0xBD, 0xF8));

        lblStatus = new JLabel("Port: 00H | Phase: 0x00 | Status: STOPPED", SwingConstants.RIGHT);
        lblStatus.setFont(new Font("Monospaced", Font.BOLD, 12));
        lblStatus.setForeground(new Color(0x34, 0xD3, 0x99));

        titlePanel.add(titleLbl, BorderLayout.WEST);
        titlePanel.add(lblStatus, BorderLayout.EAST);
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // 2. CENTER (Left: Motor Graphic, Right: Coil LEDs & Stats)
        motorPanel = new MotorPanel();

        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setPreferredSize(new Dimension(320, 360));
        rightPanel.setBackground(new Color(0x1E, 0x29, 0x3B));
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0x38, 0xBD, 0xF8)),
                "Motor Phase Coils & Telemetry",
                0, 0, new Font("Segoe UI", Font.BOLD, 12), new Color(0x38, 0xBD, 0xF8)
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Coil LEDs
        JPanel coilPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        coilPanel.setOpaque(false);
        coilPanel.setBorder(BorderFactory.createTitledBorder(null, "Phase Coils (Bits 0..3)", 0, 0, new Font("Segoe UI", Font.PLAIN, 11), Color.LIGHT_GRAY));

        String[] coilNames = {"Coil A (Bit 0)", "Coil B (Bit 1)", "Coil C (Bit 2)", "Coil D (Bit 3)"};
        for (int i = 0; i < 4; i++) {
            coilLeds[i] = new JLabel(coilNames[i], SwingConstants.CENTER);
            coilLeds[i].setFont(new Font("Consolas", Font.BOLD, 12));
            coilLeds[i].setOpaque(true);
            coilLeds[i].setBackground(new Color(0x0F, 0x17, 0x2A));
            coilLeds[i].setForeground(Color.GRAY);
            coilLeds[i].setBorder(BorderFactory.createLineBorder(new Color(0x47, 0x55, 0x69)));
            coilPanel.add(coilLeds[i]);
        }
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        rightPanel.add(coilPanel, gbc);

        // Direction & Step Stats
        lblDirection = createStatLabel("Direction: STOPPED", new Color(0xFB, 0xBF, 0x24));
        lblStepCount = createStatLabel("Total Steps: 0", new Color(0x38, 0xBD, 0xF8));

        gbc.gridy = 1; rightPanel.add(lblDirection, gbc);
        gbc.gridy = 2; rightPanel.add(lblStepCount, gbc);

        // Preset Script Loaders
        JPanel presetPanel = new JPanel(new GridLayout(2, 1, 6, 6));
        presetPanel.setOpaque(false);
        presetPanel.setBorder(BorderFactory.createTitledBorder(null, "Load Motor Control Scripts", 0, 0, new Font("Segoe UI", Font.BOLD, 11), Color.LIGHT_GRAY));

        JButton btnCw = new JButton("Clockwise Rotation (CW)");
        btnCw.addActionListener(e -> loadMotorScript("CW"));

        JButton btnCcw = new JButton("Counter-Clockwise (CCW)");
        btnCcw.addActionListener(e -> loadMotorScript("CCW"));

        presetPanel.add(btnCw);
        presetPanel.add(btnCcw);

        gbc.gridy = 3; rightPanel.add(presetPanel, gbc);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, motorPanel, rightPanel);
        splitPane.setDividerLocation(420);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // 3. BOTTOM CONTROL BAR
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        configPanel.setOpaque(false);

        configPanel.add(createLabel("Motor Control Port (OUT):"));
        cbMotorPort = createPortCombo(0x00);
        cbMotorPort.addActionListener(e -> {
            motorPort = cbMotorPort.getSelectedIndex();
            updateMotorState();
        });
        configPanel.add(cbMotorPort);

        mainPanel.add(configPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(0xE2, 0xE8, 0xF0));
        return l;
    }

    private JLabel createStatLabel(String text, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.BOLD, 13));
        l.setForeground(fg);
        return l;
    }

    private JComboBox<String> createPortCombo(int defaultPort) {
        String[] ports = new String[256];
        for (int i = 0; i < 256; i++) {
            ports[i] = String.format("%02XH (%d)", i, i);
        }
        JComboBox<String> cb = new JComboBox<>(ports);
        cb.setSelectedIndex(defaultPort);
        return cb;
    }

    public void updateMotorState() {
        Matrix m = (assembler != null && assembler.matrix != null) ? assembler.matrix : this.matrix;
        if (m == null || m.port == null) return;

        int phaseMask = motorPort < m.port.length ? (m.port[motorPort] & 0x0F) : 0;

        lblStatus.setText(String.format("Port [%02XH]: 0x%02X | Active Coils: %d", motorPort, phaseMask, Integer.bitCount(phaseMask)));

        // Update Coil LEDs
        Color activeGlow = new Color(0x00, 0xE6, 0x76);
        for (int b = 0; b < 4; b++) {
            boolean active = ((phaseMask >> b) & 1) != 0;
            if (active) {
                coilLeds[b].setBackground(activeGlow);
                coilLeds[b].setForeground(Color.BLACK);
            } else {
                coilLeds[b].setBackground(new Color(0x0F, 0x17, 0x2A));
                coilLeds[b].setForeground(Color.GRAY);
            }
        }

        // Detect Rotation Direction (CW / CCW)
        if (phaseMask != lastPhaseMask && phaseMask != 0) {
            if (isClockwiseStep(lastPhaseMask, phaseMask)) {
                direction = "CLOCKWISE";
                currentAngle = (currentAngle + 7.5) % 360.0;
                stepCount++;
            } else if (isCounterClockwiseStep(lastPhaseMask, phaseMask)) {
                direction = "COUNTER-CLOCKWISE";
                currentAngle = (currentAngle - 7.5 + 360.0) % 360.0;
                stepCount++;
            }
            lastPhaseMask = phaseMask;
        }

        lblDirection.setText("Direction: " + direction);
        lblStepCount.setText("Total Steps: " + stepCount);

        motorPanel.repaint();
    }

    private boolean isClockwiseStep(int prev, int curr) {
        // Wave drive: 1->2->4->8, Full step: 3->6->C->9
        return (prev == 1 && curr == 2) || (prev == 2 && curr == 4) || (prev == 4 && curr == 8) || (prev == 8 && curr == 1) ||
               (prev == 3 && curr == 6) || (prev == 6 && curr == 12) || (prev == 12 && curr == 9) || (prev == 9 && curr == 3);
    }

    private boolean isCounterClockwiseStep(int prev, int curr) {
        return (prev == 8 && curr == 4) || (prev == 4 && curr == 2) || (prev == 2 && curr == 1) || (prev == 1 && curr == 8) ||
               (prev == 9 && curr == 12) || (prev == 12 && curr == 6) || (prev == 6 && curr == 3) || (prev == 3 && curr == 9);
    }

    private void loadMotorScript(String type) {
        if (assembler == null) return;

        String code = "";
        if ("CW".equals(type)) {
            code = "; --- 8085 Stepper Motor Clockwise Rotation ---\n" +
                   "; Out Port: 00H | Wave Drive Sequence: 01H -> 02H -> 04H -> 08H\n\n" +
                   "START: MVI A,01H    ; Coil A\n" +
                   "       OUT 00H\n" +
                   "       CALL DELAY\n\n" +
                   "       MVI A,02H    ; Coil B\n" +
                   "       OUT 00H\n" +
                   "       CALL DELAY\n\n" +
                   "       MVI A,04H    ; Coil C\n" +
                   "       OUT 00H\n" +
                   "       CALL DELAY\n\n" +
                   "       MVI A,08H    ; Coil D\n" +
                   "       OUT 00H\n" +
                   "       CALL DELAY\n\n" +
                   "       JMP START     ; Repeat Rotation\n\n" +
                   "; --- Delay Subroutine ---\n" +
                   "DELAY: LXI B,0005H\n" +
                   "D_LOOP: DCX B\n" +
                   "       MOV A,B\n" +
                   "       ORA C\n" +
                   "       JNZ D_LOOP\n" +
                   "       RET\n";
        } else if ("CCW".equals(type)) {
            code = "; --- 8085 Stepper Motor Counter-Clockwise Rotation ---\n" +
                   "; Out Port: 00H | Wave Drive Sequence: 08H -> 04H -> 02H -> 01H\n\n" +
                   "START: MVI A,08H    ; Coil D\n" +
                   "       OUT 00H\n" +
                   "       CALL DELAY\n\n" +
                   "       MVI A,04H    ; Coil C\n" +
                   "       OUT 00H\n" +
                   "       CALL DELAY\n\n" +
                   "       MVI A,02H    ; Coil B\n" +
                   "       OUT 00H\n" +
                   "       CALL DELAY\n\n" +
                   "       MVI A,01H    ; Coil A\n" +
                   "       OUT 00H\n" +
                   "       CALL DELAY\n\n" +
                   "       JMP START     ; Repeat Rotation\n\n" +
                   "; --- Delay Subroutine ---\n" +
                   "DELAY: LXI B,0005H\n" +
                   "D_LOOP: DCX B\n" +
                   "       MOV A,B\n" +
                   "       ORA C\n" +
                   "       JNZ D_LOOP\n" +
                   "       RET\n";
        }

        assembler.jTextAreaAssemblyLanguageEditor.setText(code);
        JOptionPane.showMessageDialog(this,
            "Stepper Motor Assembly Script loaded into editor!\nClick 'Assemble' (F9) and 'Forward' (F8) to watch physical motor rotation.",
            "Preset Stepper Script Loaded", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Custom Graphic Panel rendering a rotating 2D Stepper Motor Rotor
     */
    private class MotorPanel extends JPanel {
        public MotorPanel() {
            setBackground(new Color(0x02, 0x06, 0x17));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int cx = w / 2;
            int cy = h / 2;
            int r = Math.min(w, h) / 2 - 35;

            // 1. Draw Stator Housing
            g2.setColor(new Color(0x1E, 0x29, 0x3B));
            g2.fillOval(cx - r - 15, cy - r - 15, (r + 15) * 2, (r + 15) * 2);
            g2.setColor(new Color(0x47, 0x55, 0x69));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawOval(cx - r - 15, cy - r - 15, (r + 15) * 2, (r + 15) * 2);

            // 2. Draw 4 Stator Pole Coils (Top=A, Right=B, Bottom=C, Left=D)
            int mask = lastPhaseMask;
            drawStatorCoil(g2, cx, cy - r - 5, "A", (mask & 1) != 0);
            drawStatorCoil(g2, cx + r + 5, cy, "B", (mask & 2) != 0);
            drawStatorCoil(g2, cx, cy + r + 5, "C", (mask & 4) != 0);
            drawStatorCoil(g2, cx - r - 5, cy, "D", (mask & 8) != 0);

            // 3. Draw Rotor Disk
            g2.setColor(new Color(0x33, 0x41, 0x55));
            g2.fillOval(cx - r + 10, cy - r + 10, (r - 10) * 2, (r - 10) * 2);

            // 4. Draw Rotating Pointer Shaft aligned to active coil magnetic vector
            double rad = Math.toRadians(currentAngle - 90);
            int vx = ((mask & 2) != 0 ? 1 : 0) - ((mask & 8) != 0 ? 1 : 0);
            int vy = ((mask & 4) != 0 ? 1 : 0) - ((mask & 1) != 0 ? 1 : 0);

            if (vx != 0 || vy != 0) {
                rad = Math.atan2(vy, vx);
            }

            int needleX = cx + (int) ((r - 20) * Math.cos(rad));
            int needleY = cy + (int) ((r - 20) * Math.sin(rad));

            g2.setColor(new Color(0x38, 0xBD, 0xF8));
            g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(cx, cy, needleX, needleY);

            // Rotor Center Knob
            g2.setColor(Color.WHITE);
            g2.fillOval(cx - 10, cy - 10, 20, 20);
            g2.setColor(new Color(0x02, 0x84, 0xC7));
            g2.fillOval(cx - 5, cy - 5, 10, 10);
        }

        private void drawStatorCoil(Graphics2D g2, int x, int y, String label, boolean active) {
            Color color = active ? new Color(0x00, 0xE6, 0x76) : new Color(0x33, 0x41, 0x55);
            g2.setColor(color);
            g2.fillOval(x - 12, y - 12, 24, 24);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(active ? Color.BLACK : Color.LIGHT_GRAY);
            g2.drawString(label, x - 4, y + 4);
        }
    }
}
