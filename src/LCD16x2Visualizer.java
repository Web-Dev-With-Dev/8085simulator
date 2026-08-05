import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

/**
 * 8085 16x2 Character LCD Display Visualizer (HD44780 Controller)
 * Real-time I/O Port driven simulator supporting HD44780 command sets
 * (Clear 01H, Home 02H, Line 1 80H, Line 2 C0H, Shift 18H/1CH) and ASCII Data Port writes.
 */
public class LCD16x2Visualizer extends JFrame {

    private static LCD16x2Visualizer instance;
    private final Matrix matrix;
    private final Assembler assembler;

    // Ports
    private int commandPort = 0x00;
    private int dataPort = 0x01;

    // HD44780 LCD State
    private final char[][] ddram = new char[2][16]; // 2 lines x 16 chars
    private int cursorLine = 0; // 0 or 1
    private int cursorCol = 0;  // 0 .. 15
    private boolean displayOn = true;
    private boolean cursorOn = true;
    private boolean blinkOn = false;
    private int displayShift = 0; // Shift offset for scrolling text

    private int lastCmdVal = -1;
    private int lastDataVal = -1;
    private int lastCmdWriteCount = -1;
    private int lastDataWriteCount = -1;

    // UI Components
    private LCDCanvasPanel lcdCanvasPanel;
    private JComboBox<String> cbCmdPort;
    private JComboBox<String> cbDataPort;
    private JComboBox<String> cbTheme;
    private JLabel lblStatus;
    private JLabel lblCursorPos;

    private Color bgBacklight = new Color(0x00, 0xE6, 0x76); // Retro Emerald Green
    private Color charPixelColor = new Color(0x02, 0x2C, 0x12); // Dark LCD Pixels

    private Timer refreshTimer;
    private Timer blinkTimer;
    private boolean blinkState = true;

    public LCD16x2Visualizer(Matrix matrix, Assembler assembler) {
        this.matrix = matrix;
        this.assembler = assembler;
        instance = this;

        setTitle(" 8085 16x2 Character LCD Display Module (HD44780)");
        setSize(960, 520);
        setLocationRelativeTo(assembler);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        clearLcdState();
        initComponents();
        updateLcdState();

        // 30ms Live Refresh Timer for real-time I/O Port polling
        refreshTimer = new Timer(30, e -> {
            if (isShowing()) {
                updateLcdState();
            }
        });
        refreshTimer.start();

        // 500ms Blinking Cursor Timer
        blinkTimer = new Timer(500, e -> {
            blinkState = !blinkState;
            if (isShowing() && (cursorOn || blinkOn)) {
                lcdCanvasPanel.repaint();
            }
        });
        blinkTimer.start();
    }

    public static LCD16x2Visualizer getInstance(Matrix matrix, Assembler assembler) {
        if (instance == null || !instance.isDisplayable()) {
            instance = new LCD16x2Visualizer(matrix, assembler);
        }
        return instance;
    }

    private void clearLcdState() {
        for (int r = 0; r < 2; r++) {
            Arrays.fill(ddram[r], ' ');
        }
        cursorLine = 0;
        cursorCol = 0;
        displayShift = 0;
        // NOTE: Do NOT clear portQueues here — future queued characters must still be processed
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        mainPanel.setBackground(new Color(0x0F, 0x17, 0x2A));

        // 1. TOP TITLE BANNER
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        JLabel titleLbl = new JLabel("8085 16x2 Character LCD Display (HD44780)", SwingConstants.LEFT);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLbl.setForeground(new Color(0x38, 0xBD, 0xF8));

        lblStatus = new JLabel("Cmd Port [00H]: 0x00 | Data Port [01H]: 0x00", SwingConstants.RIGHT);
        lblStatus.setFont(new Font("Monospaced", Font.BOLD, 12));
        lblStatus.setForeground(new Color(0x34, 0xD3, 0x99));

        titlePanel.add(titleLbl, BorderLayout.WEST);
        titlePanel.add(lblStatus, BorderLayout.EAST);
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // 2. CENTER LCD BEZEL & CANVAS
        lcdCanvasPanel = new LCDCanvasPanel();
        mainPanel.add(lcdCanvasPanel, BorderLayout.CENTER);

        // 3. BOTTOM CONTROL BAR & PRESET SCRIPT LOADERS
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);

        // Configuration
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        configPanel.setOpaque(false);
        configPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0x33, 0x41, 0x55)),
            "HD44780 Port & Theme Controls",
            0, 0, new Font("Segoe UI", Font.BOLD, 12), new Color(0x94, 0xA3, 0xB8)
        ));

        configPanel.add(createLabel("Cmd Port:"));
        cbCmdPort = createPortCombo(0x00);
        cbCmdPort.addActionListener(e -> {
            commandPort = cbCmdPort.getSelectedIndex();
            updateLcdState();
        });
        configPanel.add(cbCmdPort);

        configPanel.add(createLabel("Data Port:"));
        cbDataPort = createPortCombo(0x01);
        cbDataPort.addActionListener(e -> {
            dataPort = cbDataPort.getSelectedIndex();
            updateLcdState();
        });
        configPanel.add(cbDataPort);

        configPanel.add(createLabel("Backlight:"));
        cbTheme = new JComboBox<>(new String[]{"Retro Emerald", "Cyan Ice", "Amber Gold", "Monochrome"});
        cbTheme.addActionListener(e -> {
            switch (cbTheme.getSelectedIndex()) {
                case 0:
                    bgBacklight = new Color(0x00, 0xE6, 0x76);
                    charPixelColor = new Color(0x02, 0x2C, 0x12);
                    break;
                case 1:
                    bgBacklight = new Color(0x00, 0xE5, 0xFF);
                    charPixelColor = new Color(0x00, 0x2B, 0x4D);
                    break;
                case 2:
                    bgBacklight = new Color(0xFF, 0xD6, 0x00);
                    charPixelColor = new Color(0x3B, 0x2A, 0x00);
                    break;
                case 3:
                    bgBacklight = new Color(0xE2, 0xE8, 0xF0);
                    charPixelColor = new Color(0x0F, 0x17, 0x2A);
                    break;
            }
            lcdCanvasPanel.repaint();
        });
        configPanel.add(cbTheme);

        lblCursorPos = new JLabel("Cursor: L1 P0", SwingConstants.CENTER);
        lblCursorPos.setFont(new Font("Monospaced", Font.BOLD, 12));
        lblCursorPos.setForeground(new Color(0xFB, 0xBF, 0x24));
        configPanel.add(lblCursorPos);

        bottomPanel.add(configPanel, BorderLayout.CENTER);

        // Presets Panel
        JPanel presetsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        presetsPanel.setOpaque(false);
        presetsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0x33, 0x41, 0x55)),
            "Preset LCD Scripts",
            0, 0, new Font("Segoe UI", Font.BOLD, 12), new Color(0x94, 0xA3, 0xB8)
        ));

        JButton btnHello = new JButton("Hello World");
        btnHello.addActionListener(e -> loadLcdScript("HELLO"));

        JButton btnScroll = new JButton("Scrolling Text");
        btnScroll.addActionListener(e -> loadLcdScript("SCROLL"));

        JButton btn2Line = new JButton("2-Line Status");
        btn2Line.addActionListener(e -> loadLcdScript("2LINE"));

        JButton btnClear = new JButton("Clear Screen");
        btnClear.addActionListener(e -> {
            clearLcdState();
            lcdCanvasPanel.repaint();
        });

        presetsPanel.add(btnHello);
        presetsPanel.add(btnScroll);
        presetsPanel.add(btn2Line);
        presetsPanel.add(btnClear);

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

    public void updateLcdState() {
        Matrix m = (assembler != null && assembler.matrix != null) ? assembler.matrix : this.matrix;
        if (m == null || m.port == null) return;

        int cmdVal = commandPort < m.port.length ? (m.port[commandPort] & 0xFF) : 0;
        int dataVal = dataPort < m.port.length ? (m.port[dataPort] & 0xFF) : 0;

        int cmdWriteCount = (m.portWriteCount != null && commandPort < m.portWriteCount.length) ? m.portWriteCount[commandPort] : 0;
        int dataWriteCount = (m.portWriteCount != null && dataPort < m.portWriteCount.length) ? m.portWriteCount[dataPort] : 0;

        lblStatus.setText(String.format("Cmd Port [%02XH]: 0x%02X | Data Port [%02XH]: 0x%02X ('%c')",
            commandPort, cmdVal, dataPort, dataVal, (dataVal >= 32 && dataVal <= 126) ? (char) dataVal : '.'));

        boolean processedData = false;
        boolean processedCmd = false;

        // Process Command Port FIFO Queue
        if (m.portQueue != null && commandPort < m.portQueue.length && m.portQueue[commandPort] != null) {
            while (!m.portQueue[commandPort].isEmpty()) {
                Integer cmd = m.portQueue[commandPort].poll();
                if (cmd != null) {
                    executeLcdCommand(cmd & 0xFF);
                    processedCmd = true;
                }
            }
        }

        // Process Data Port FIFO Queue
        if (m.portQueue != null && dataPort < m.portQueue.length && m.portQueue[dataPort] != null) {
            while (!m.portQueue[dataPort].isEmpty()) {
                Integer d = m.portQueue[dataPort].poll();
                if (d != null) {
                    writeLcdData((char) (d & 0xFF));
                    processedData = true;
                }
            }
        }

        // Fallback for portWriteCount increment if queue was bypassed
        if (!processedCmd && cmdWriteCount != lastCmdWriteCount && cmdVal != 0) {
            executeLcdCommand(cmdVal);
        }
        if (!processedData && dataWriteCount != lastDataWriteCount && dataVal != 0) {
            writeLcdData((char) dataVal);
        }

        lastCmdWriteCount = cmdWriteCount;
        lastDataWriteCount = dataWriteCount;

        lblCursorPos.setText(String.format("Cursor: L%d P%d", cursorLine + 1, cursorCol));
        lcdCanvasPanel.repaint();
    }

    private void executeLcdCommand(int cmd) {
        if (cmd == 0x01) {
            // Clear Display
            clearLcdState();
        } else if (cmd == 0x02) {
            // Return Home
            cursorLine = 0;
            cursorCol = 0;
            displayShift = 0;
        } else if ((cmd & 0xF8) == 0x08) {
            // Display Control (08H + D + C + B)
            displayOn = (cmd & 0x04) != 0;
            cursorOn = (cmd & 0x02) != 0;
            blinkOn = (cmd & 0x01) != 0;
        } else if (cmd == 0x18) {
            // Shift Display Left
            displayShift = (displayShift + 1) % 16;
        } else if (cmd == 0x1C) {
            // Shift Display Right
            displayShift = (displayShift - 1 + 16) % 16;
        } else if ((cmd & 0x80) != 0) {
            // Set DDRAM Address
            if ((cmd & 0x40) != 0) {
                // Line 2: C0H .. CFH
                cursorLine = 1;
                cursorCol = Math.min(15, cmd & 0x0F);
            } else {
                // Line 1: 80H .. 8FH
                cursorLine = 0;
                cursorCol = Math.min(15, cmd & 0x0F);
            }
        }
    }

    private void writeLcdData(char ch) {
        if (ch < 32 || ch > 126) return; // Only accept valid printable ASCII characters (0x20..0x7E)

        if (cursorLine >= 0 && cursorLine < 2 && cursorCol >= 0 && cursorCol < 16) {
            ddram[cursorLine][cursorCol] = ch;
            cursorCol++;
            if (cursorCol >= 16) {
                cursorCol = 0;
                cursorLine = (cursorLine + 1) % 2;
            }
        }
    }

    private void loadLcdScript(String type) {
        if (assembler == null) return;

        String code = "";
        if ("HELLO".equals(type)) {
            code = "; --- 8085 16x2 LCD Hello World ---\n" +
                   "; Cmd Port: 00H | Data Port: 01H\n\n" +
                   "START: MVI A,01H    ; Clear Display\n" +
                   "       OUT 00H\n" +
                   "       MVI A,80H    ; Line 1, Pos 0\n" +
                   "       OUT 00H\n\n" +
                   "       ; Print 'H' 'E' 'L' 'L' 'O'\n" +
                   "       MVI A,48H    ; 'H'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,45H    ; 'E'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,4CH    ; 'L'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,4CH    ; 'L'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,4FH    ; 'O'\n" +
                   "       OUT 01H\n\n" +
                   "       ; Print ' ' 'W' 'O' 'R' 'L' 'D'\n" +
                   "       MVI A,20H    ; ' '\n" +
                   "       OUT 01H\n" +
                   "       MVI A,57H    ; 'W'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,4FH    ; 'O'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,52H    ; 'R'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,4CH    ; 'L'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,44H    ; 'D'\n" +
                   "       OUT 01H\n" +
                   "       HLT\n";
        } else if ("SCROLL".equals(type)) {
            code = "; --- 8085 16x2 LCD Text Scroller ---\n" +
                   "START: MVI A,01H    ; Clear Display\n" +
                   "       OUT 00H\n" +
                   "       MVI A,80H    ; Line 1\n" +
                   "       OUT 00H\n" +
                   "       MVI A,41H    ; 'A'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,55H    ; 'U'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,52H    ; 'R'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,41H    ; 'A'\n" +
                   "       OUT 01H\n\n" +
                   "LOOP:  MVI A,18H    ; Shift Left Command (18H)\n" +
                   "       OUT 00H\n" +
                   "       JMP LOOP\n";
        } else if ("2LINE".equals(type)) {
            code = "; --- 8085 16x2 LCD 2-Line Status ---\n" +
                   "START: MVI A,01H    ; Clear Display\n" +
                   "       OUT 00H\n" +
                   "       MVI A,80H    ; Line 1 Pos 0\n" +
                   "       OUT 00H\n" +
                   "       MVI A,38H    ; '8'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,30H    ; '0'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,38H    ; '8'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,35H    ; '5'\n" +
                   "       OUT 01H\n\n" +
                   "       MVI A,C0H    ; Line 2 Pos 0\n" +
                   "       OUT 00H\n" +
                   "       MVI A,52H    ; 'R'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,45H    ; 'E'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,41H    ; 'A'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,44H    ; 'D'\n" +
                   "       OUT 01H\n" +
                   "       MVI A,59H    ; 'Y'\n" +
                   "       OUT 01H\n" +
                   "       HLT\n";
        }

        assembler.jTextAreaAssemblyLanguageEditor.setText(code);
        JOptionPane.showMessageDialog(this,
            "16x2 LCD Assembly Script loaded into editor!\nClick 'Assemble' (F9) and 'Forward' (F8) to watch text render on the LCD screen.",
            "Preset LCD Script Loaded", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Custom Graphic Panel rendering the 16x2 Character LCD Glass Screen
     */
    private class LCDCanvasPanel extends JPanel {
        public LCDCanvasPanel() {
            setPreferredSize(new Dimension(840, 240));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Draw Outer Bezel
            g2.setColor(new Color(0x02, 0x06, 0x17));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(0x33, 0x41, 0x55));
            g2.setStroke(new BasicStroke(3.0f));
            g2.drawRoundRect(8, 8, w - 16, h - 16, 12, 12);

            // 2. Draw Backlit Glass Screen
            int marginX = 25, marginY = 25;
            int screenW = w - 2 * marginX;
            int screenH = h - 2 * marginY;

            g2.setColor(bgBacklight);
            g2.fillRoundRect(marginX, marginY, screenW, screenH, 8, 8);

            // 3. Render 16x2 Character Cells
            int charW = (screenW - 20) / 16;
            int charH = (screenH - 20) / 2;

            g2.setFont(new Font("Consolas", Font.BOLD, 22));

            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 16; c++) {
                    int colIdx = (c + displayShift) % 16;
                    char ch = ddram[r][colIdx];

                    int cellX = marginX + 10 + c * charW;
                    int cellY = marginY + 10 + r * charH;

                    // Faint 5x8 Grid Background Box for each character cell
                    g2.setColor(new Color(charPixelColor.getRed(), charPixelColor.getGreen(), charPixelColor.getBlue(), 25));
                    g2.fillRect(cellX + 2, cellY + 2, charW - 4, charH - 4);

                    // Draw Character
                    if (ch != ' ') {
                        g2.setColor(charPixelColor);
                        g2.drawString(String.valueOf(ch), cellX + 8, cellY + charH - 12);
                    }

                    // Draw Cursor (if active on this cell)
                    if (r == cursorLine && c == cursorCol && (cursorOn || blinkOn)) {
                        if (blinkState) {
                            g2.setColor(charPixelColor);
                            if (blinkOn) {
                                // Blinking Block Cursor
                                g2.fillRect(cellX + 2, cellY + 2, charW - 4, charH - 4);
                            } else {
                                // Underline Cursor
                                g2.fillRect(cellX + 4, cellY + charH - 6, charW - 8, 3);
                            }
                        }
                    }
                }
            }
        }
    }
}
