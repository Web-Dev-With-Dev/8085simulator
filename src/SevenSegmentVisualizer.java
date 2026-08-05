import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;

/**
 * 8085 Interactive 4/8-Digit 7-Segment LED Display Unit
 * Supports Raw Bitmask, BCD Decoding, Multiplexed I/O Ports (00H/01H),
 * Common Cathode / Common Anode polarity, and customizable LED colors.
 */
public class SevenSegmentVisualizer extends JFrame {

    private static SevenSegmentVisualizer instance;
    private final Matrix matrix;
    private final Assembler assembler;

    // Default configuration
    private int dataPort = 0x00;
    private int selectPort = 0x01;
    private int numDigits = 4;
    private boolean commonCathode = true; // true = Active HIGH, false = Active LOW
    private String decodeMode = "BCD"; // Default to Direct BCD / Hex Decoder

    private void initDecodeModeCombo() {
        cbDecodeMode = new JComboBox<>(new String[]{
            "Raw 7-Segment Bitmask",
            "Direct BCD / Hex Decoder",
            "Multiplexed Dual-Port"
        });
        cbDecodeMode.setSelectedIndex(1);
        cbDecodeMode.addActionListener(e -> {
            int idx = cbDecodeMode.getSelectedIndex();
            decodeMode = idx == 0 ? "BITMASK" : idx == 1 ? "BCD" : "MULTIPLEXED";
            updateDisplay();
        });
    }
    private Color ledColor = new Color(0xFF, 0x17, 0x44); // Neon Red

    // Digit segment states: bits 0..6 = a..g, bit 7 = dp
    private final int[] digitSegments = new int[8];

    // UI Panels
    private JPanel displayContainer;
    private SevenSegmentDigitPanel[] digitPanels;
    private JComboBox<String> cbDataPort;
    private JComboBox<String> cbSelectPort;
    private JComboBox<String> cbDecodeMode;
    private JComboBox<String> cbLedColor;
    private JComboBox<String> cbDigits;
    private JRadioButton rbCathode;
    private JRadioButton rbAnode;
    private JLabel lblPortStatus;

    private Timer refreshTimer;

    // Standard 7-Segment Hex BCD Lookup Table (0-9, A-F)
    private static final int[] BCD_LUT = {
        0x3F, // 0: a,b,c,d,e,f
        0x06, // 1: b,c
        0x5B, // 2: a,b,d,e,g
        0x4F, // 3: a,b,c,d,g
        0x66, // 4: b,c,f,g
        0x6D, // 5: a,c,d,f,g
        0x7D, // 6: a,c,d,e,f,g
        0x07, // 7: a,b,c
        0x7F, // 8: a,b,c,d,e,f,g
        0x6F, // 9: a,b,c,d,f,g
        0x77, // A: a,b,c,e,f,g
        0x7C, // b: c,d,e,f,g
        0x39, // C: a,d,e,f
        0x5E, // d: b,c,d,e,g
        0x79, // E: a,d,e,f,g
        0x71  // F: a,e,f,g
    };

    public SevenSegmentVisualizer(Matrix matrix, Assembler assembler) {
        this.matrix = matrix;
        this.assembler = assembler;
        instance = this;

        setTitle(" 8085 7-Segment LED Display Unit");
        setSize(920, 460);
        setLocationRelativeTo(assembler);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
        updateDisplay();

        // 50ms Live Refresh Timer for real-time I/O Port monitoring
        refreshTimer = new Timer(50, e -> {
            if (isShowing()) {
                updateDisplay();
            }
        });
        refreshTimer.start();
    }

    public static SevenSegmentVisualizer getInstance(Matrix matrix, Assembler assembler) {
        if (instance == null || !instance.isDisplayable()) {
            instance = new SevenSegmentVisualizer(matrix, assembler);
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
        JLabel titleLbl = new JLabel("8085 7-Segment LED Display Module", SwingConstants.LEFT);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLbl.setForeground(new Color(0x38, 0xBD, 0xF8));

        lblPortStatus = new JLabel("Data Port: 00H [0x00] | Select Port: 01H [0x00]", SwingConstants.RIGHT);
        lblPortStatus.setFont(new Font("Monospaced", Font.BOLD, 12));
        lblPortStatus.setForeground(new Color(0x34, 0xD3, 0x99));

        titlePanel.add(titleLbl, BorderLayout.WEST);
        titlePanel.add(lblPortStatus, BorderLayout.EAST);
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // 2. CENTER LED DISPLAY BEZEL
        displayContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 20));
        displayContainer.setBackground(new Color(0x02, 0x06, 0x17));
        displayContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x33, 0x41, 0x55), 2, true),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        rebuildDigitPanels();
        mainPanel.add(displayContainer, BorderLayout.CENTER);

        // 3. BOTTOM CONTROL & CONFIGURATION TOOLBAR
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setOpaque(false);
        configPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0x33, 0x41, 0x55)),
                "Display Configuration & Port Settings",
                0, 0, new Font("Segoe UI", Font.BOLD, 12), new Color(0x94, 0xA3, 0xB8)
            ),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Data Port Selector
        gbc.gridx = 0; gbc.gridy = 0;
        configPanel.add(createLabel("Data Port:"), gbc);
        gbc.gridx = 1;
        cbDataPort = createPortCombo(0x00);
        cbDataPort.addActionListener(e -> {
            dataPort = cbDataPort.getSelectedIndex();
            updateDisplay();
        });
        configPanel.add(cbDataPort, gbc);

        // Select Port Selector (Multiplexing)
        gbc.gridx = 2; gbc.gridy = 0;
        configPanel.add(createLabel("Select Port:"), gbc);
        gbc.gridx = 3;
        cbSelectPort = createPortCombo(0x01);
        cbSelectPort.addActionListener(e -> {
            selectPort = cbSelectPort.getSelectedIndex();
            updateDisplay();
        });
        configPanel.add(cbSelectPort, gbc);

        // Decode Mode Selector
        gbc.gridx = 4; gbc.gridy = 0;
        configPanel.add(createLabel("Decode Mode:"), gbc);
        gbc.gridx = 5;
        initDecodeModeCombo();
        configPanel.add(cbDecodeMode, gbc);

        // Digit Count Selector
        gbc.gridx = 0; gbc.gridy = 1;
        configPanel.add(createLabel("Digit Count:"), gbc);
        gbc.gridx = 1;
        cbDigits = new JComboBox<>(new String[]{"4 Digits", "8 Digits"});
        cbDigits.addActionListener(e -> {
            numDigits = cbDigits.getSelectedIndex() == 0 ? 4 : 8;
            rebuildDigitPanels();
            updateDisplay();
        });
        configPanel.add(cbDigits, gbc);

        // LED Color Theme
        gbc.gridx = 2; gbc.gridy = 1;
        configPanel.add(createLabel("LED Color:"), gbc);
        gbc.gridx = 3;
        cbLedColor = new JComboBox<>(new String[]{"Neon Red", "Cyber Green", "Ice Cyan", "Amber Orange"});
        cbLedColor.addActionListener(e -> {
            switch (cbLedColor.getSelectedIndex()) {
                case 0: ledColor = new Color(0xFF, 0x17, 0x44); break;
                case 1: ledColor = new Color(0x00, 0xE6, 0x76); break;
                case 2: ledColor = new Color(0x00, 0xE5, 0xFF); break;
                case 3: ledColor = new Color(0xFF, 0x91, 0x00); break;
            }
            displayContainer.repaint();
        });
        configPanel.add(cbLedColor, gbc);

        // Polarity (Common Cathode / Anode)
        gbc.gridx = 4; gbc.gridy = 1;
        configPanel.add(createLabel("Polarity:"), gbc);
        gbc.gridx = 5;
        JPanel polPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        polPanel.setOpaque(false);
        rbCathode = new JRadioButton("Com. Cathode (+)", true);
        rbAnode = new JRadioButton("Com. Anode (-)", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbCathode); bg.add(rbAnode);
        rbCathode.setForeground(Color.WHITE); rbAnode.setForeground(Color.WHITE);
        rbCathode.setOpaque(false); rbAnode.setOpaque(false);

        ActionListener polAL = e -> {
            commonCathode = rbCathode.isSelected();
            updateDisplay();
        };
        rbCathode.addActionListener(polAL);
        rbAnode.addActionListener(polAL);

        polPanel.add(rbCathode);
        polPanel.add(rbAnode);
        configPanel.add(polPanel, gbc);

        mainPanel.add(configPanel, BorderLayout.SOUTH);
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

    private void rebuildDigitPanels() {
        displayContainer.removeAll();
        digitPanels = new SevenSegmentDigitPanel[numDigits];
        for (int i = 0; i < numDigits; i++) {
            final int digitIdx = i;
            digitPanels[i] = new SevenSegmentDigitPanel(digitIdx);
            displayContainer.add(digitPanels[i]);
        }
        displayContainer.revalidate();
        displayContainer.repaint();
    }

    public void updateDisplay() {
        Matrix m = (assembler != null && assembler.matrix != null) ? assembler.matrix : this.matrix;
        if (m == null || m.port == null) return;

        int rawDataVal = m.port[dataPort] & 0xFF;
        int rawSelectVal = m.port[selectPort] & 0xFF;

        lblPortStatus.setText(String.format("Data Port [%02XH]: 0x%02X (%d) | Select Port [%02XH]: 0x%02X",
            dataPort, rawDataVal, rawDataVal, selectPort, rawSelectVal));

        // Adjust for Common Anode polarity inversion
        int effectiveData = commonCathode ? rawDataVal : (~rawDataVal & 0xFF);
        int effectiveSelect = commonCathode ? rawSelectVal : (~rawSelectVal & 0xFF);

        if ("BITMASK".equals(decodeMode)) {
            // Apply raw 7-segment bitmask to all digits, or nibble multiplex
            for (int i = 0; i < numDigits; i++) {
                digitSegments[i] = effectiveData;
            }
        } else if ("BCD".equals(decodeMode)) {
            // Decode rawDataVal as 2-digit BCD/Hex value
            int highNibble = (effectiveData >> 4) & 0x0F;
            int lowNibble = effectiveData & 0x0F;

            int segHigh = BCD_LUT[highNibble];
            int segLow = BCD_LUT[lowNibble];

            for (int i = 0; i < numDigits; i++) {
                if (i == numDigits - 2) digitSegments[i] = segHigh;
                else if (i == numDigits - 1) digitSegments[i] = segLow;
                else digitSegments[i] = 0;
            }
        } else if ("MULTIPLEXED".equals(decodeMode)) {
            // Multiplexed Dual-Port: effectiveSelect selects digit bitmask (01H -> Dig 0, 02H -> Dig 1, etc.)
            for (int i = 0; i < numDigits; i++) {
                boolean isSelected = (effectiveSelect & (1 << i)) != 0 || (effectiveSelect == 0 && i == 0);
                if (isSelected) {
                    digitSegments[i] = BCD_LUT[effectiveData & 0x0F];
                }
            }
        }

        displayContainer.repaint();
    }

    /**
     * Custom Graphic Panel rendering a single 7-Segment LED Digit
     */
    private class SevenSegmentDigitPanel extends JPanel {
        private final int digitIndex;

        public SevenSegmentDigitPanel(int digitIndex) {
            this.digitIndex = digitIndex;
            setPreferredSize(new Dimension(85, 140));
            setOpaque(false);
            setToolTipText(String.format("Digit %d (Click to test segments)", digitIndex));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Manual test toggle
                    digitSegments[digitIndex] = (digitSegments[digitIndex] + 1) & 0xFF;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int mask = digitIndex < digitSegments.length ? digitSegments[digitIndex] : 0;
            Color offColor = new Color(0x1E, 0x29, 0x3B);
            Color onColor = ledColor;

            // Draw Digit Background Box
            g2.setColor(new Color(0x0F, 0x17, 0x2A));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.setColor(new Color(0x33, 0x41, 0x55));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

            // Digit Index Label at top
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.setColor(new Color(0x64, 0x74, 0x8B));
            g2.drawString("DIG " + digitIndex, 28, 14);

            int x0 = 18, y0 = 24;
            int w = 45, h = 95;
            int t = 7; // segment thickness

            // Segment Path definitions
            // a: Top horizontal
            drawHorizontalSegment(g2, x0 + t, y0, w - 2 * t, t, (mask & 1) != 0 ? onColor : offColor);
            // b: Top-Right vertical
            drawVerticalSegment(g2, x0 + w - t, y0 + t, t, h / 2 - t, (mask & 2) != 0 ? onColor : offColor);
            // c: Bottom-Right vertical
            drawVerticalSegment(g2, x0 + w - t, y0 + h / 2, t, h / 2 - t, (mask & 4) != 0 ? onColor : offColor);
            // d: Bottom horizontal
            drawHorizontalSegment(g2, x0 + t, y0 + h - t, w - 2 * t, t, (mask & 8) != 0 ? onColor : offColor);
            // e: Bottom-Left vertical
            drawVerticalSegment(g2, x0, y0 + h / 2, t, h / 2 - t, (mask & 16) != 0 ? onColor : offColor);
            // f: Top-Left vertical
            drawVerticalSegment(g2, x0, y0 + t, t, h / 2 - t, (mask & 32) != 0 ? onColor : offColor);
            // g: Middle horizontal
            drawHorizontalSegment(g2, x0 + t, y0 + h / 2 - t / 2, w - 2 * t, t, (mask & 64) != 0 ? onColor : offColor);

            // dp: Decimal Point LED
            boolean dpOn = (mask & 128) != 0;
            g2.setColor(dpOn ? onColor : offColor);
            g2.fillOval(x0 + w + 4, y0 + h - t - 2, 7, 7);
            if (dpOn) {
                g2.setColor(new Color(onColor.getRed(), onColor.getGreen(), onColor.getBlue(), 100));
                g2.fillOval(x0 + w + 2, y0 + h - t - 4, 11, 11);
            }
        }

        private void drawHorizontalSegment(Graphics2D g2, int x, int y, int w, int h, Color color) {
            Path2D p = new Path2D.Float();
            p.moveTo(x, y + h / 2.0f);
            p.lineTo(x + h / 2.0f, y);
            p.lineTo(x + w - h / 2.0f, y);
            p.lineTo(x + w, y + h / 2.0f);
            p.lineTo(x + w - h / 2.0f, y + h);
            p.lineTo(x + h / 2.0f, y + h);
            p.closePath();

            g2.setColor(color);
            g2.fill(p);
            if (color != new Color(0x1E, 0x29, 0x3B)) { // Glow effect for ON state
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
                g2.setStroke(new BasicStroke(3.0f));
                g2.draw(p);
            }
        }

        private void drawVerticalSegment(Graphics2D g2, int x, int y, int w, int h, Color color) {
            Path2D p = new Path2D.Float();
            p.moveTo(x + w / 2.0f, y);
            p.lineTo(x + w, y + w / 2.0f);
            p.lineTo(x + w, y + h - w / 2.0f);
            p.lineTo(x + w / 2.0f, y + h);
            p.lineTo(x, y + h - w / 2.0f);
            p.lineTo(x, y + w / 2.0f);
            p.closePath();

            g2.setColor(color);
            g2.fill(p);
            if (color != new Color(0x1E, 0x29, 0x3B)) { // Glow effect for ON state
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
                g2.setStroke(new BasicStroke(3.0f));
                g2.draw(p);
            }
        }
    }
}
