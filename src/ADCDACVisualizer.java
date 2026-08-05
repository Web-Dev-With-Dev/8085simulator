import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 8-Bit ADC & DAC Waveform Generator Visualizer for 8085 Microprocessor
 * Features:
 * 1. Live Oscilloscope Canvas rendering DAC waveform output (OUT instruction).
 * 2. Interactive Analog Voltage Slider converting 0.0V-5.0V to 8-bit digital value for ADC (IN instruction).
 * 3. Signal statistics, Timebase/Voltage scaling, and preset assembly waveform loaders.
 */
public class ADCDACVisualizer extends JFrame {

    private static ADCDACVisualizer instance;
    private final Matrix matrix;
    private final Assembler assembler;

    // Default Ports
    private int dacPort = 0x00;
    private int adcPort = 0x01;

    // ADC State
    private double adcAnalogVoltage = 2.50; // 0.00V to 5.00V
    private int adcDigitalValue = 127;     // 0 to 255

    // DAC State & Oscilloscope History
    private final List<Double> dacVoltageHistory = new ArrayList<>();
    private final int MAX_SAMPLES = 400;
    private double currentDacVoltage = 0.0;

    // UI Components
    private OscilloscopePanel oscilloscopePanel;
    private JSlider sldAnalogIn;
    private JLabel lblAnalogVal;
    private JLabel lblDigitalVal;
    private JLabel[] bitLeds = new JLabel[8];

    private JComboBox<String> cbDacPort;
    private JComboBox<String> cbAdcPort;
    private JComboBox<String> cbTraceColor;
    private JSlider sldTimebase;

    private JLabel lblVmin, lblVmax, lblVpp, lblVavg;

    private Timer refreshTimer;
    private Color traceColor = new Color(0x00, 0xE6, 0x76); // Cyber Emerald Green

    public ADCDACVisualizer(Matrix matrix, Assembler assembler) {
        this.matrix = matrix;
        this.assembler = assembler;
        instance = this;

        setTitle(" 8-Bit ADC & DAC Waveform Generator Visualizer");
        setSize(980, 620);
        setLocationRelativeTo(assembler);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
        updateAdcFromSlider();
        updateVisualizer();

        // 30ms Live Sync Timer for smooth oscilloscope plotting
        refreshTimer = new Timer(30, e -> {
            if (isShowing()) {
                updateVisualizer();
            }
        });
        refreshTimer.start();
    }

    public static ADCDACVisualizer getInstance(Matrix matrix, Assembler assembler) {
        if (instance == null || !instance.isDisplayable()) {
            instance = new ADCDACVisualizer(matrix, assembler);
        }
        return instance;
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(0x0F, 0x17, 0x2A));

        // 1. TOP HEADER BANNER
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel titleLbl = new JLabel("8-Bit ADC & DAC Waveform Oscilloscope", SwingConstants.LEFT);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLbl.setForeground(new Color(0x38, 0xBD, 0xF8));

        JLabel infoLbl = new JLabel("DAC Output: OUT Port 00H | ADC Input: IN Port 01H", SwingConstants.RIGHT);
        infoLbl.setFont(new Font("Monospaced", Font.BOLD, 12));
        infoLbl.setForeground(new Color(0x34, 0xD3, 0x99));

        headerPanel.add(titleLbl, BorderLayout.WEST);
        headerPanel.add(infoLbl, BorderLayout.EAST);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // 2. CENTER SPLIT (Left: Oscilloscope Canvas, Right: ADC Input Panel)
        oscilloscopePanel = new OscilloscopePanel();
        
        // Right Panel: ADC & Stats Panel
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setPreferredSize(new Dimension(340, 400));
        rightPanel.setOpaque(false);

        // ADC Panel
        JPanel adcPanel = new JPanel(new GridBagLayout());
        adcPanel.setBackground(new Color(0x1E, 0x29, 0x3B));
        adcPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0x38, 0xBD, 0xF8)),
                "8-Bit ADC Input Control (V_in -> IN Port)",
                0, 0, new Font("Segoe UI", Font.BOLD, 12), new Color(0x38, 0xBD, 0xF8)
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        lblAnalogVal = new JLabel("Analog Input (V_in): 2.50 V", SwingConstants.CENTER);
        lblAnalogVal.setFont(new Font("Consolas", Font.BOLD, 14));
        lblAnalogVal.setForeground(new Color(0x38, 0xBD, 0xF8));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        adcPanel.add(lblAnalogVal, gbc);

        sldAnalogIn = new JSlider(0, 500, 250); // 0.00V to 5.00V (step 0.01V)
        sldAnalogIn.setOpaque(false);
        sldAnalogIn.addChangeListener(e -> updateAdcFromSlider());
        gbc.gridy = 1;
        adcPanel.add(sldAnalogIn, gbc);

        lblDigitalVal = new JLabel("Digital Byte: 0x7F (127 Decimal)", SwingConstants.CENTER);
        lblDigitalVal.setFont(new Font("Consolas", Font.BOLD, 13));
        lblDigitalVal.setForeground(new Color(0x34, 0xD3, 0x99));
        gbc.gridy = 2;
        adcPanel.add(lblDigitalVal, gbc);

        // 8-Bit Digital LEDs Display
        JPanel ledsPanel = new JPanel(new GridLayout(1, 8, 4, 0));
        ledsPanel.setOpaque(false);
        ledsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(null, "Bit Pattern (D7..D0)", 0, 0, new Font("Segoe UI", Font.PLAIN, 11), Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));

        for (int b = 7; b >= 0; b--) {
            bitLeds[b] = new JLabel(String.valueOf(b), SwingConstants.CENTER);
            bitLeds[b].setFont(new Font("Consolas", Font.BOLD, 11));
            bitLeds[b].setOpaque(true);
            bitLeds[b].setBackground(new Color(0x0F, 0x17, 0x2A));
            bitLeds[b].setForeground(Color.GRAY);
            bitLeds[b].setBorder(BorderFactory.createLineBorder(new Color(0x47, 0x55, 0x69)));
            ledsPanel.add(bitLeds[b]);
        }
        gbc.gridy = 3;
        adcPanel.add(ledsPanel, gbc);

        rightPanel.add(adcPanel, BorderLayout.NORTH);

        // Signal Stats Panel
        JPanel statsPanel = new JPanel(new GridLayout(4, 1, 4, 4));
        statsPanel.setBackground(new Color(0x1E, 0x29, 0x3B));
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0x34, 0xD3, 0x99)),
                "DAC Output Oscilloscope Stats",
                0, 0, new Font("Segoe UI", Font.BOLD, 12), new Color(0x34, 0xD3, 0x99)
            ),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        lblVmin = createStatLabel("V_min: 0.00 V", new Color(0x94, 0xA3, 0xB8));
        lblVmax = createStatLabel("V_max: 0.00 V", new Color(0x94, 0xA3, 0xB8));
        lblVpp  = createStatLabel("Peak-to-Peak (V_pp): 0.00 V", new Color(0xFB, 0xBF, 0x24));
        lblVavg = createStatLabel("Current DAC Output: 0.00 V (0x00)", new Color(0x38, 0xBD, 0xF8));

        statsPanel.add(lblVavg);
        statsPanel.add(lblVmin);
        statsPanel.add(lblVmax);
        statsPanel.add(lblVpp);

        rightPanel.add(statsPanel, BorderLayout.CENTER);

        // Preset Waveform Loaders
        JPanel presetsPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        presetsPanel.setOpaque(false);
        presetsPanel.setBorder(BorderFactory.createTitledBorder(null, "Load Waveform Assembly Scripts", 0, 0, new Font("Segoe UI", Font.BOLD, 11), Color.LIGHT_GRAY));

        JButton btnRamp = new JButton("Sawtooth Wave");
        btnRamp.addActionListener(e -> loadWaveScript("RAMP"));

        JButton btnSquare = new JButton("Square Wave");
        btnSquare.addActionListener(e -> loadWaveScript("SQUARE"));

        JButton btnTri = new JButton("Triangular Wave");
        btnTri.addActionListener(e -> loadWaveScript("TRIANGLE"));

        JButton btnClearTrace = new JButton("Clear Trace");
        btnClearTrace.addActionListener(e -> {
            dacVoltageHistory.clear();
            oscilloscopePanel.repaint();
        });

        presetsPanel.add(btnRamp);
        presetsPanel.add(btnSquare);
        presetsPanel.add(btnTri);
        presetsPanel.add(btnClearTrace);

        rightPanel.add(presetsPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, oscilloscopePanel, rightPanel);
        splitPane.setDividerLocation(600);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // 3. BOTTOM CONFIGURATION BAR
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        configPanel.setOpaque(false);

        configPanel.add(createLabel("DAC Port (OUT):"));
        cbDacPort = createPortCombo(0x00);
        cbDacPort.addActionListener(e -> {
            dacPort = cbDacPort.getSelectedIndex();
            updateVisualizer();
        });
        configPanel.add(cbDacPort);

        configPanel.add(createLabel("ADC Port (IN):"));
        cbAdcPort = createPortCombo(0x01);
        cbAdcPort.addActionListener(e -> {
            adcPort = cbAdcPort.getSelectedIndex();
            updateAdcFromSlider();
        });
        configPanel.add(cbAdcPort);

        configPanel.add(createLabel("Trace Color:"));
        cbTraceColor = new JComboBox<>(new String[]{"Emerald Green", "Ice Cyan", "Amber Yellow", "Neon Pink"});
        cbTraceColor.addActionListener(e -> {
            switch (cbTraceColor.getSelectedIndex()) {
                case 0: traceColor = new Color(0x00, 0xE6, 0x76); break;
                case 1: traceColor = new Color(0x00, 0xE5, 0xFF); break;
                case 2: traceColor = new Color(0xFF, 0xD6, 0x00); break;
                case 3: traceColor = new Color(0xFF, 0x17, 0x44); break;
            }
            oscilloscopePanel.repaint();
        });
        configPanel.add(cbTraceColor);

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
        l.setFont(new Font("Monospaced", Font.BOLD, 12));
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

    private void updateAdcFromSlider() {
        int sliderVal = sldAnalogIn.getValue(); // 0..500
        adcAnalogVoltage = sliderVal / 100.0;    // 0.00V .. 5.00V
        adcDigitalValue = (int) Math.round((adcAnalogVoltage / 5.0) * 255.0);
        adcDigitalValue = Math.min(255, Math.max(0, adcDigitalValue));

        lblAnalogVal.setText(String.format("Analog Input (V_in): %.2f V", adcAnalogVoltage));
        lblDigitalVal.setText(String.format("Digital Byte: 0x%02X (%d Dec)", adcDigitalValue, adcDigitalValue));

        // Update 8-bit LED indicators
        for (int b = 0; b < 8; b++) {
            boolean bitOn = ((adcDigitalValue >> b) & 1) != 0;
            if (bitOn) {
                bitLeds[b].setBackground(new Color(0x00, 0xE6, 0x76));
                bitLeds[b].setForeground(Color.BLACK);
            } else {
                bitLeds[b].setBackground(new Color(0x0F, 0x17, 0x2A));
                bitLeds[b].setForeground(Color.GRAY);
            }
        }

        // Write ADC digital value directly into matrix.port[adcPort]
        Matrix m = (assembler != null && assembler.matrix != null) ? assembler.matrix : this.matrix;
        if (m != null && m.port != null && adcPort < m.port.length) {
            m.port[adcPort] = adcDigitalValue;
        }
    }

    public void updateVisualizer() {
        Matrix m = (assembler != null && assembler.matrix != null) ? assembler.matrix : this.matrix;
        if (m == null || m.port == null) return;

        // Keep ADC port in sync with slider
        if (adcPort < m.port.length) {
            m.port[adcPort] = adcDigitalValue;
        }

        // Read DAC port output value (OUT dacPort)
        int rawDacVal = dacPort < m.port.length ? (m.port[dacPort] & 0xFF) : 0;
        currentDacVoltage = (rawDacVal / 255.0) * 5.0;

        // Append to history for continuous scrolling waveform
        dacVoltageHistory.add(currentDacVoltage);
        if (dacVoltageHistory.size() > MAX_SAMPLES) {
            dacVoltageHistory.remove(0);
        }

        // Calculate Stats
        double vMin = 5.0, vMax = 0.0;
        for (double v : dacVoltageHistory) {
            if (v < vMin) vMin = v;
            if (v > vMax) vMax = v;
        }
        if (dacVoltageHistory.isEmpty()) {
            vMin = 0.0; vMax = 0.0;
        }
        double vPp = Math.max(0.0, vMax - vMin);

        lblVavg.setText(String.format("Current DAC Output: %.2f V (0x%02X)", currentDacVoltage, rawDacVal));
        lblVmin.setText(String.format("V_min: %.2f V", vMin));
        lblVmax.setText(String.format("V_max: %.2f V", vMax));
        lblVpp.setText(String.format("Peak-to-Peak (V_pp): %.2f V", vPp));

        oscilloscopePanel.repaint();
    }

    private void loadWaveScript(String type) {
        if (assembler == null) return;

        String code = "";
        if ("RAMP".equals(type)) {
            code = "; --- 8085 Sawtooth / Ramp Waveform Generator ---\n" +
                   "; Outputs increasing ramp to DAC Port 00H\n\n" +
                   "START:\n" +
                   "    MVI A, 00H    ; Start at 0.0V\n" +
                   "RAMP_LOOP:\n" +
                   "    OUT 00H       ; Output voltage step to DAC\n" +
                   "    INR A         ; Increment voltage (0 -> 255)\n" +
                   "    JNZ RAMP_LOOP ; Continue ramp\n" +
                   "    JMP START     ; Repeat waveform\n";
        } else if ("SQUARE".equals(type)) {
            code = "; --- 8085 Square Waveform Generator ---\n" +
                   "; Toggles DAC Port 00H between 0.0V and 5.0V\n\n" +
                   "START:\n" +
                   "    MVI A, FFH    ; 5.0V High Level\n" +
                   "    OUT 00H\n" +
                   "    MVI A, 00H    ; 0.0V Low Level\n" +
                   "    OUT 00H\n" +
                   "    JMP START     ; Repeat square wave\n";
        } else if ("TRIANGLE".equals(type)) {
            code = "; --- 8085 Triangular Waveform Generator ---\n" +
                   "; Ramps UP to 5V, then ramps DOWN to 0V\n\n" +
                   "START:\n" +
                   "    MVI A, 00H\n" +
                   "UP_LOOP:\n" +
                   "    OUT 00H\n" +
                   "    INR A\n" +
                   "    CPI FFH\n" +
                   "    JNZ UP_LOOP\n" +
                   "DOWN_LOOP:\n" +
                   "    OUT 00H\n" +
                   "    DCR A\n" +
                   "    JNZ DOWN_LOOP\n" +
                   "    JMP START\n";
        }

        assembler.jTextAreaAssemblyLanguageEditor.setText(code);
        JOptionPane.showMessageDialog(this,
            "Waveform Assembly Script loaded into editor!\nClick 'Assemble' (F9) and 'Forward' (F8) to see live waveform plot on Oscilloscope.",
            "Preset Waveform Loaded", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Custom Graphic Panel rendering the Oscilloscope Screen with Grid & Glowing Trace
     */
    private class OscilloscopePanel extends JPanel {
        public OscilloscopePanel() {
            setBackground(new Color(0x02, 0x06, 0x17)); // Dark CRT Screen
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Draw Grid Lines & Subdivisions
            g2.setColor(new Color(0x1E, 0x29, 0x3B));
            g2.setStroke(new BasicStroke(1.0f));

            int gridCols = 10;
            int gridRows = 8;
            for (int c = 1; c < gridCols; c++) {
                int x = c * w / gridCols;
                g2.drawLine(x, 0, x, h);
            }
            for (int r = 1; r < gridRows; r++) {
                int y = r * h / gridRows;
                g2.drawLine(0, y, w, y);
            }

            // Center Axis Lines
            g2.setColor(new Color(0x33, 0x41, 0x55));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(w / 2, 0, w / 2, h);
            g2.drawLine(0, h / 2, w, h / 2);

            // Voltage Scale Labels (0V to 5V)
            g2.setFont(new Font("Monospaced", Font.BOLD, 11));
            g2.setColor(new Color(0x64, 0x74, 0x8B));
            g2.drawString("5.0V", 10, 20);
            g2.drawString("2.5V", 10, h / 2 - 4);
            g2.drawString("0.0V", 10, h - 10);
            g2.drawString("TIME ->", w - 60, h - 10);

            // 2. Plot DAC Voltage Trace Line
            if (dacVoltageHistory.size() < 2) return;

            g2.setColor(traceColor);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int points = dacVoltageHistory.size();
            int xStep = Math.max(1, w / MAX_SAMPLES);

            int prevX = 0;
            int prevY = h - (int) ((dacVoltageHistory.get(0) / 5.0) * (h - 40) + 20);

            for (int i = 1; i < points; i++) {
                int currX = i * xStep;
                double v = dacVoltageHistory.get(i);
                int currY = h - (int) ((v / 5.0) * (h - 40) + 20);

                g2.drawLine(prevX, prevY, currX, currY);

                // Glow Overlay effect
                g2.setColor(new Color(traceColor.getRed(), traceColor.getGreen(), traceColor.getBlue(), 60));
                g2.setStroke(new BasicStroke(5.0f));
                g2.drawLine(prevX, prevY, currX, currY);

                g2.setColor(traceColor);
                g2.setStroke(new BasicStroke(2.2f));

                prevX = currX;
                prevY = currY;
            }

            // Draw Beam Dot at head
            g2.setColor(Color.WHITE);
            g2.fillOval(prevX - 4, prevY - 4, 8, 8);
        }
    }
}
