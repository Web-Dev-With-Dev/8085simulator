import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * 8085 4-Way Traffic Light Controller Visualizer
 * Simulates a 4-way intersection (North, South, East, West) with glowing Red, Yellow,
 * and Green LEDs controlled directly by I/O Port bit patterns (OUT 00H / OUT 01H).
 */
public class TrafficLightVisualizer extends JFrame {

    private static TrafficLightVisualizer instance;
    private final Matrix matrix;
    private final Assembler assembler;

    // Default Configuration
    private int nsPort = 0x00; // North & South Port (or 8-bit Combined Port)
    private int ewPort = 0x01; // East & West Port
    private boolean dualPortMode = true; // true = 2 Ports (00H & 01H), false = 1 Combined Port (00H)

    // Traffic Light States: 0 = RED, 1 = YELLOW, 2 = GREEN
    private int northState = 0;
    private int southState = 0;
    private int eastState = 0;
    private int westState = 0;

    // UI Components
    private JunctionPanel junctionPanel;
    private JComboBox<String> cbNsPort;
    private JComboBox<String> cbEwPort;
    private JRadioButton rbDualPort;
    private JRadioButton rbSinglePort;
    private JLabel lblStateSummary;

    private Timer refreshTimer;

    public TrafficLightVisualizer(Matrix matrix, Assembler assembler) {
        this.matrix = matrix;
        this.assembler = assembler;
        instance = this;

        setTitle(" 8085 4-Way Traffic Light Controller Simulator");
        setSize(940, 600);
        setLocationRelativeTo(assembler);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
        updateTrafficStates();

        // 50ms Live Refresh Timer for real-time I/O Port polling
        refreshTimer = new Timer(50, e -> {
            if (isShowing()) {
                updateTrafficStates();
            }
        });
        refreshTimer.start();
    }

    public static TrafficLightVisualizer getInstance(Matrix matrix, Assembler assembler) {
        if (instance == null || !instance.isDisplayable()) {
            instance = new TrafficLightVisualizer(matrix, assembler);
        }
        return instance;
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(0x0F, 0x17, 0x2A));

        // 1. TOP TITLE BANNER
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        JLabel titleLbl = new JLabel("8085 4-Way Traffic Light Junction Controller", SwingConstants.LEFT);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLbl.setForeground(new Color(0x38, 0xBD, 0xF8));

        lblStateSummary = new JLabel("North: RED | South: RED | East: RED | West: RED", SwingConstants.RIGHT);
        lblStateSummary.setFont(new Font("Monospaced", Font.BOLD, 12));
        lblStateSummary.setForeground(new Color(0x34, 0xD3, 0x99));

        titlePanel.add(titleLbl, BorderLayout.WEST);
        titlePanel.add(lblStateSummary, BorderLayout.EAST);
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // 2. CENTER JUNCTION SIMULATOR PANEL
        junctionPanel = new JunctionPanel();
        mainPanel.add(junctionPanel, BorderLayout.CENTER);

        // 3. BOTTOM CONTROL TOOLBAR & PRESETS
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);

        // Configuration Settings
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        configPanel.setOpaque(false);
        configPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0x33, 0x41, 0x55)),
            "I/O Port & Decoding Mode",
            0, 0, new Font("Segoe UI", Font.BOLD, 12), new Color(0x94, 0xA3, 0xB8)
        ));

        configPanel.add(createLabel("N/S Port:"));
        cbNsPort = createPortCombo(0x00);
        cbNsPort.addActionListener(e -> {
            nsPort = cbNsPort.getSelectedIndex();
            updateTrafficStates();
        });
        configPanel.add(cbNsPort);

        configPanel.add(createLabel("E/W Port:"));
        cbEwPort = createPortCombo(0x01);
        cbEwPort.addActionListener(e -> {
            ewPort = cbEwPort.getSelectedIndex();
            updateTrafficStates();
        });
        configPanel.add(cbEwPort);

        rbDualPort = new JRadioButton("Dual Port (00H & 01H)", true);
        rbSinglePort = new JRadioButton("Single Port (00H Bitmask)", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbDualPort); bg.add(rbSinglePort);
        rbDualPort.setForeground(Color.WHITE); rbSinglePort.setForeground(Color.WHITE);
        rbDualPort.setOpaque(false); rbSinglePort.setOpaque(false);

        ActionListener modeAL = e -> {
            dualPortMode = rbDualPort.isSelected();
            cbEwPort.setEnabled(dualPortMode);
            updateTrafficStates();
        };
        rbDualPort.addActionListener(modeAL);
        rbSinglePort.addActionListener(modeAL);

        configPanel.add(rbDualPort);
        configPanel.add(rbSinglePort);

        bottomPanel.add(configPanel, BorderLayout.CENTER);

        // Preset Script Loaders
        JPanel presetsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        presetsPanel.setOpaque(false);
        presetsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0x33, 0x41, 0x55)),
            "Preset Traffic Sequences",
            0, 0, new Font("Segoe UI", Font.BOLD, 12), new Color(0x94, 0xA3, 0xB8)
        ));

        JButton btnSeq = new JButton("Full Traffic Loop");
        btnSeq.addActionListener(e -> loadTrafficScript("FULL"));

        JButton btnNight = new JButton("Flashing Amber");
        btnNight.addActionListener(e -> loadTrafficScript("AMBER"));

        JButton btnStop = new JButton("Emergency Stop");
        btnStop.addActionListener(e -> loadTrafficScript("STOP"));

        presetsPanel.add(btnSeq);
        presetsPanel.add(btnNight);
        presetsPanel.add(btnStop);

        bottomPanel.add(presetsPanel, BorderLayout.EAST);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(0xE2, 0xE8, 0xF0));
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

    public void updateTrafficStates() {
        Matrix m = (assembler != null && assembler.matrix != null) ? assembler.matrix : this.matrix;
        if (m == null || m.port == null) return;

        int nsVal = nsPort < m.port.length ? (m.port[nsPort] & 0xFF) : 0;
        int ewVal = ewPort < m.port.length ? (m.port[ewPort] & 0xFF) : 0;

        if (dualPortMode) {
            // Dual Port Mode:
            // Port 00H (N/S): Bit 0 = N-RED, Bit 1 = N-YELLOW, Bit 2 = N-GREEN
            //                 Bit 3 = S-RED, Bit 4 = S-YELLOW, Bit 5 = S-GREEN
            // Port 01H (E/W): Bit 0 = E-RED, Bit 1 = E-YELLOW, Bit 2 = E-GREEN
            //                 Bit 3 = W-RED, Bit 4 = W-YELLOW, Bit 5 = W-GREEN
            northState = decodeBitsToState(nsVal & 0x07);
            southState = decodeBitsToState((nsVal >> 3) & 0x07);

            eastState = decodeBitsToState(ewVal & 0x07);
            westState = decodeBitsToState((ewVal >> 3) & 0x07);
        } else {
            // Single Port Mode:
            // Port 00H: Bits 0..1 = North, Bits 2..3 = South, Bits 4..5 = East, Bits 6..7 = West
            northState = nsVal & 0x03;
            southState = (nsVal >> 2) & 0x03;
            eastState = (nsVal >> 4) & 0x03;
            westState = (nsVal >> 6) & 0x03;
        }

        lblStateSummary.setText(String.format("North: %s | South: %s | East: %s | West: %s",
            getStateName(northState), getStateName(southState), getStateName(eastState), getStateName(westState)));

        junctionPanel.repaint();
    }

    private int decodeBitsToState(int bits) {
        // bit 2 = GREEN (2), bit 1 = YELLOW (1), bit 0 = RED (0)
        if ((bits & 4) != 0) return 2; // GREEN
        if ((bits & 2) != 0) return 1; // YELLOW
        return 0;                      // RED
    }

    private String getStateName(int state) {
        switch (state) {
            case 1: return "AMBER";
            case 2: return "GREEN";
            default: return "RED";
        }
    }

    private void loadTrafficScript(String type) {
        if (assembler == null) return;

        String code = "";
        if ("FULL".equals(type)) {
            code = "; --- 8085 4-Way Traffic Light Loop ---\n" +
                   "; Port 00H: North & South | Port 01H: East & West\n" +
                   "; Bit Patterns: 01H = RED, 02H = AMBER, 04H = GREEN\n\n" +
                   "START: MVI A,24H    ; N-GREEN (04H) + S-GREEN (20H) = 24H\n" +
                   "       OUT 00H\n" +
                   "       MVI A,09H    ; E-RED (01H) + W-RED (08H) = 09H\n" +
                   "       OUT 01H\n" +
                   "       CALL DELAY\n\n" +
                   "       MVI A,12H    ; N-AMBER (02H) + S-AMBER (10H) = 12H\n" +
                   "       OUT 00H\n" +
                   "       CALL DELAY\n\n" +
                   "       MVI A,09H    ; N-RED (01H) + S-RED (08H) = 09H\n" +
                   "       OUT 00H\n" +
                   "       MVI A,24H    ; E-GREEN (04H) + W-GREEN (20H) = 24H\n" +
                   "       OUT 01H\n" +
                   "       CALL DELAY\n\n" +
                   "       MVI A,12H    ; E-AMBER (02H) + W-AMBER (10H) = 12H\n" +
                   "       OUT 01H\n" +
                   "       CALL DELAY\n\n" +
                   "       JMP START     ; Repeat Traffic Cycle\n\n" +
                   "; --- Delay Subroutine ---\n" +
                   "DELAY: LXI B,0005H\n" +
                   "D_LOOP: DCX B\n" +
                   "       MOV A,B\n" +
                   "       ORA C\n" +
                   "       JNZ D_LOOP\n" +
                   "       RET\n";
        } else if ("AMBER".equals(type)) {
            code = "; --- Night Flashing Amber Mode ---\n" +
                   "START: MVI A,12H    ; N & S Amber\n" +
                   "       OUT 00H\n" +
                   "       OUT 01H       ; E & W Amber\n" +
                   "       CALL DELAY\n" +
                   "       MVI A,00H    ; Lights OFF\n" +
                   "       OUT 00H\n" +
                   "       OUT 01H\n" +
                   "       CALL DELAY\n" +
                   "       JMP START\n\n" +
                   "DELAY: LXI B,0005H\n" +
                   "D_LOOP: DCX B\n" +
                   "       MOV A,B\n" +
                   "       ORA C\n" +
                   "       JNZ D_LOOP\n" +
                   "       RET\n";
        } else if ("STOP".equals(type)) {
            code = "; --- Emergency All-Red Stop Mode ---\n" +
                   "MVI A,09H    ; N-RED (01H) + S-RED (08H) = 09H\n" +
                   "OUT 00H       ; Set North & South to RED\n" +
                   "OUT 01H       ; Set East & West to RED\n" +
                   "HLT\n";
        }

        assembler.jTextAreaAssemblyLanguageEditor.setText(code);
        JOptionPane.showMessageDialog(this,
            "Traffic Sequence Script loaded into editor!\nClick 'Assemble' (F9) and 'Forward' (F8) to watch the live junction simulation.",
            "Preset Traffic Script Loaded", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Custom Graphic Panel rendering a 4-Way Traffic Junction with signal heads
     */
    private class JunctionPanel extends JPanel {
        public JunctionPanel() {
            setBackground(new Color(0x06, 0x4E, 0x3B)); // Grass surrounding
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            int roadWidth = 140;
            int cx = w / 2;
            int cy = h / 2;

            // 1. Draw Asphalt Roads
            g2.setColor(new Color(0x1E, 0x29, 0x3B));
            g2.fillRect(cx - roadWidth / 2, 0, roadWidth, h); // Vertical Road (North-South)
            g2.fillRect(0, cy - roadWidth / 2, w, roadWidth); // Horizontal Road (East-West)

            // 2. Draw Center Intersection Box
            g2.setColor(new Color(0x33, 0x41, 0x55));
            g2.fillRect(cx - roadWidth / 2, cy - roadWidth / 2, roadWidth, roadWidth);

            // 3. Draw Road Markings (Yellow Dashed Lines & White Stop Lines)
            g2.setColor(new Color(0xFB, 0xBF, 0x24)); // Yellow
            Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{12, 12}, 0);
            g2.setStroke(dashed);

            // North/South Centerlines
            g2.drawLine(cx, 0, cx, cy - roadWidth / 2 - 10);
            g2.drawLine(cx, cy + roadWidth / 2 + 10, cx, h);

            // East/West Centerlines
            g2.drawLine(0, cy, cx - roadWidth / 2 - 10, cy);
            g2.drawLine(cx + roadWidth / 2 + 10, cy, w, cy);

            // White Stop Lines
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(4.0f));
            g2.drawLine(cx - roadWidth / 2, cy - roadWidth / 2 - 8, cx, cy - roadWidth / 2 - 8); // North Stop Line
            g2.drawLine(cx, cy + roadWidth / 2 + 8, cx + roadWidth / 2, cy + roadWidth / 2 + 8); // South Stop Line
            g2.drawLine(cx - roadWidth / 2 - 8, cy, cx - roadWidth / 2 - 8, cy + roadWidth / 2); // West Stop Line
            g2.drawLine(cx + roadWidth / 2 + 8, cy - roadWidth / 2, cx + roadWidth / 2 + 8, cy); // East Stop Line

            // 4. Draw Traffic Signal Heads
            drawSignalHead(g2, cx - roadWidth / 2 - 45, cy - roadWidth / 2 - 80, "NORTH", northState);
            drawSignalHead(g2, cx + roadWidth / 2 + 10, cy + roadWidth / 2 + 10, "SOUTH", southState);
            drawSignalHead(g2, cx + roadWidth / 2 + 10, cy - roadWidth / 2 - 80, "EAST", eastState);
            drawSignalHead(g2, cx - roadWidth / 2 - 45, cy + roadWidth / 2 + 10, "WEST", westState);
        }

        private void drawSignalHead(Graphics2D g2, int x, int y, String label, int state) {
            int w = 38, h = 85;

            // Signal Box Housing
            g2.setColor(new Color(0x0F, 0x17, 0x2A));
            g2.fillRoundRect(x, y, w, h, 8, 8);
            g2.setColor(new Color(0x47, 0x55, 0x69));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x, y, w, h, 8, 8);

            // Label
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString(label, x + 3, y - 4);

            int lampR = 20;
            int cx = x + w / 2 - lampR / 2;

            // RED Lamp
            boolean redOn = (state == 0);
            Color redC = redOn ? new Color(0xFF, 0x17, 0x44) : new Color(0x45, 0x0A, 0x0A);
            g2.setColor(redC);
            g2.fillOval(cx, y + 6, lampR, lampR);
            if (redOn) drawGlow(g2, cx, y + 6, lampR, new Color(0xFF, 0x17, 0x44, 120));

            // AMBER / YELLOW Lamp
            boolean amberOn = (state == 1);
            Color amberC = amberOn ? new Color(0xFF, 0xD6, 0x00) : new Color(0x45, 0x30, 0x00);
            g2.setColor(amberC);
            g2.fillOval(cx, y + 31, lampR, lampR);
            if (amberOn) drawGlow(g2, cx, y + 31, lampR, new Color(0xFF, 0xD6, 0x00, 120));

            // GREEN Lamp
            boolean greenOn = (state == 2);
            Color greenC = greenOn ? new Color(0x00, 0xE6, 0x76) : new Color(0x02, 0x33, 0x19);
            g2.setColor(greenC);
            g2.fillOval(cx, y + 56, lampR, lampR);
            if (greenOn) drawGlow(g2, cx, y + 56, lampR, new Color(0x00, 0xE6, 0x76, 120));
        }

        private void drawGlow(Graphics2D g2, int x, int y, int r, Color color) {
            g2.setColor(color);
            g2.fillOval(x - 3, y - 3, r + 6, r + 6);
        }
    }
}
