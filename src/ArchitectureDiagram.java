import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.geom.*;

public class ArchitectureDiagram extends JFrame {

    // Modern Colors
    private static final Color COLOR_BG_DARK = new Color(0x0A, 0x0C, 0x10);
    private static final Color COLOR_CPU_BG = new Color(0x1F, 0x22, 0x2B);
    private static final Color COLOR_CPU_PIN = new Color(0xC0, 0xC0, 0xC0);
    private static final Color COLOR_MEM_BG = new Color(0x1A, 0x4D, 0x2E); // PCB Green
    private static final Color COLOR_BLOCK = new Color(0x2B, 0x30, 0x3D);
    private static final Color COLOR_BLOCK_BORDER = new Color(0x3B, 0x40, 0x50);
    private static final Color COLOR_HIGHLIGHT = new Color(0x3B, 0x82, 0xF6); // Blue highlight
    private static final Color COLOR_TEXT = new Color(0xF3, 0xF4, 0xF6);
    private static final Color COLOR_MUTED = new Color(0x9C, 0xA3, 0xAF);
    private static final Color COLOR_BUS = new Color(0x4B, 0x55, 0x63);

    private DiagramPanel panel;
    private Assembler assembler;

    public ArchitectureDiagram(Assembler assembler) {
        this.assembler = assembler;
        setTitle("AURA Studio - Animated CPU Architecture");
        setSize(1280, 800);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLocationRelativeTo(null); 
        
        try {
            java.awt.image.BufferedImage raw = javax.imageio.ImageIO.read(getClass().getResourceAsStream("/aura_logo.dat"));
            if (raw != null) setIconImage(raw);
        } catch (Exception ignored) {}

        setLayout(new BorderLayout());
        
        panel = new DiagramPanel();
        add(panel, BorderLayout.CENTER);

        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 15));
        controlPanel.setBackground(new Color(0x11, 0x18, 0x27));
        controlPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x1F, 0x29, 0x37)));

        JButton btnBack = createStyledButton("<< Step Backward", new Color(0x4B, 0x55, 0x63));
        btnBack.addActionListener(e -> {
            assembler.jButtonBackwardActionPerformed(null);
        });

        JButton btnFwd = createStyledButton("Step Forward >>", COLOR_HIGHLIGHT);
        btnFwd.addActionListener(e -> {
            assembler.jButtonForwardActionPerformed(null);
        });

        controlPanel.add(btnBack);
        controlPanel.add(btnFwd);
        add(controlPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(200, 45));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    public void updateState(String instruction, String explanation, Map<String, String> registers, Map<String, Boolean> flags) {
        panel.updateState(instruction, explanation, registers, flags);
    }

    public void reset() {
        panel.reset();
    }

    private class DiagramPanel extends JPanel {
        private String currentInstruction = "";
        private String currentExplanation = "";
        private Map<String, String> registers = new HashMap<>();
        private Map<String, Boolean> flags = new HashMap<>();
        
        private Set<String> activeBlocks = new HashSet<>();
        private final Map<String, Rectangle> blocks = new HashMap<>();

        // Animation state
        private javax.swing.Timer animTimer;
        private double animProgress = 0.0;
        private int animDirection = 1;
        private boolean isAnimating = false;

        public DiagramPanel() {
            setBackground(COLOR_BG_DARK);
            initBlocks();
            
            animTimer = new javax.swing.Timer(16, e -> {
                if (isAnimating) {
                    animProgress += 0.05 * animDirection;
                    if (animProgress >= 1.0) {
                        animProgress = 1.0;
                        animDirection = -1;
                    } else if (animProgress <= 0.0) {
                        animProgress = 0.0;
                        animDirection = 1;
                    }
                    repaint();
                }
            });
        }

        private void initBlocks() {
            // CPU Bounds: x=80, y=60, w=600, h=520
            int cpuX = 80, cpuY = 60;
            int rw = 80, rh = 50;
            
            blocks.put("ALU", new Rectangle(cpuX + 40, cpuY + 40, rw * 2 + 20, rh * 2));
            blocks.put("Accumulator", new Rectangle(cpuX + 40, cpuY + 160, rw * 2 + 20, rh));
            blocks.put("Flags", new Rectangle(cpuX + 40, cpuY + 230, rw * 2 + 20, rh));
            blocks.put("Instr Reg", new Rectangle(cpuX + 40, cpuY + 320, rw * 2 + 20, rh));
            blocks.put("Instr Dec", new Rectangle(cpuX + 40, cpuY + 390, rw * 2 + 20, rh));

            int rx = cpuX + 380;
            blocks.put("B", new Rectangle(rx, cpuY + 40, rw, rh));
            blocks.put("C", new Rectangle(rx + rw + 10, cpuY + 40, rw, rh));
            blocks.put("D", new Rectangle(rx, cpuY + 110, rw, rh));
            blocks.put("E", new Rectangle(rx + rw + 10, cpuY + 110, rw, rh));
            blocks.put("H", new Rectangle(rx, cpuY + 180, rw, rh));
            blocks.put("L", new Rectangle(rx + rw + 10, cpuY + 180, rw, rh));
            blocks.put("Stack Ptr", new Rectangle(rx, cpuY + 260, rw * 2 + 10, rh));
            blocks.put("Prog Ctr", new Rectangle(rx, cpuY + 330, rw * 2 + 10, rh));

            // Memory block: x=880, y=100, w=320, h=400
            blocks.put("Memory (RAM)", new Rectangle(880, 100, 320, 400));
        }

        public void reset() {
            currentInstruction = "";
            currentExplanation = "";
            activeBlocks.clear();
            registers.clear();
            flags.clear();
            isAnimating = false;
            if (animTimer != null) animTimer.stop();
            repaint();
        }

        public void updateState(String inst, String expl, Map<String, String> regs, Map<String, Boolean> flgs) {
            this.currentInstruction = inst != null ? inst.trim().toUpperCase() : "";
            this.currentExplanation = expl != null ? expl.replaceAll("<[^>]+>", "").trim() : "";
            if (regs != null) this.registers = new HashMap<>(regs);
            if (flgs != null) this.flags = new HashMap<>(flgs);
            
            determineActiveBlocks();
            
            animProgress = 0.0;
            animDirection = 1;
            isAnimating = activeBlocks.size() > 0;
            if (isAnimating) animTimer.start();
            else repaint();
        }

        private void determineActiveBlocks() {
            activeBlocks.clear();
            if (currentInstruction.isEmpty()) return;

            String[] parts = currentInstruction.split("[\\s,]+");
            if (parts.length == 0) return;
            
            String op = parts[0];
            String source = null;
            String dest = null;

            if (op.equals("ADD") || op.equals("SUB") || op.equals("ANA") || op.equals("XRA") || op.equals("ORA") || op.equals("CMP")) {
                activeBlocks.add("ALU");
                activeBlocks.add("Accumulator");
                activeBlocks.add("Flags");
                if (parts.length > 1) {
                    source = mapReg(parts[1]);
                    if (source.equals("Memory (RAM)")) { activeBlocks.add("H"); activeBlocks.add("L"); }
                }
                dest = "Accumulator";
            } else if (op.equals("MOV")) {
                if (parts.length >= 3) {
                    dest = mapReg(parts[1]);
                    source = mapReg(parts[2]);
                    if (dest.equals("Memory (RAM)") || source.equals("Memory (RAM)")) {
                        activeBlocks.add("H"); activeBlocks.add("L");
                    }
                }
            } else if (op.equals("MVI")) {
                if (parts.length > 1) dest = mapReg(parts[1]);
            } else if (op.equals("LXI")) {
                if (parts.length > 1) {
                    String pair = parts[1];
                    if (pair.equals("B")) { activeBlocks.add("B"); activeBlocks.add("C"); dest = "B"; }
                    if (pair.equals("D")) { activeBlocks.add("D"); activeBlocks.add("E"); dest = "D"; }
                    if (pair.equals("H")) { activeBlocks.add("H"); activeBlocks.add("L"); dest = "H"; }
                    if (pair.equals("SP")) { activeBlocks.add("Stack Ptr"); dest = "Stack Ptr"; }
                }
            } else if (op.equals("LDA")) {
                dest = "Accumulator";
                source = "Memory (RAM)";
            } else if (op.equals("STA")) {
                source = "Accumulator";
                dest = "Memory (RAM)";
            } else if (op.equals("PUSH")) {
                source = mapReg(parts.length > 1 ? parts[1] : "");
                dest = "Memory (RAM)";
                activeBlocks.add("Stack Ptr");
            } else if (op.equals("POP")) {
                dest = mapReg(parts.length > 1 ? parts[1] : "");
                source = "Memory (RAM)";
                activeBlocks.add("Stack Ptr");
            } else if (op.startsWith("J") || op.startsWith("C") || op.startsWith("R")) {
                dest = "Prog Ctr";
            } else if (op.equals("INR") || op.equals("DCR") || op.equals("INX") || op.equals("DCX")) {
                if (parts.length > 1) dest = mapReg(parts[1]);
                if ("Memory (RAM)".equals(dest)) { activeBlocks.add("H"); activeBlocks.add("L"); }
                activeBlocks.add("ALU");
                activeBlocks.add("Flags");
                if (op.equals("INX") || op.equals("DCX")) {
                    if ("B".equals(dest)) { activeBlocks.add("C"); }
                    if ("D".equals(dest)) { activeBlocks.add("E"); }
                    if ("H".equals(dest)) { activeBlocks.add("L"); }
                }
            } else if (op.equals("XCHG")) {
                activeBlocks.add("H"); activeBlocks.add("L");
                activeBlocks.add("D"); activeBlocks.add("E");
            }

            if (source != null) activeBlocks.add(source);
            if (dest != null) activeBlocks.add(dest);
        }

        private String mapReg(String r) {
            if (r.equals("A")) return "Accumulator";
            if (r.equals("M")) return "Memory (RAM)";
            if (r.equals("SP")) return "Stack Ptr";
            if (r.equals("PSW")) return "Accumulator"; 
            if (Arrays.asList("B","C","D","E","H","L").contains(r)) return r;
            return r;
        }

        private Color pulseColor(Color baseColor, Color highlightColor, double progress) {
            int r1 = baseColor.getRed();
            int g1 = baseColor.getGreen();
            int b1 = baseColor.getBlue();
            int r2 = highlightColor.getRed();
            int g2 = highlightColor.getGreen();
            int b2 = highlightColor.getBlue();
            
            int r = (int)(r1 + (r2 - r1) * progress);
            int g = (int)(g1 + (g2 - g1) * progress);
            int b = (int)(b1 + (b2 - b1) * progress);
            return new Color(r, g, b);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            drawTitle(g2);
            drawExternalBuses(g2);
            drawCPU(g2);
            drawMemory(g2);
            drawBlocks(g2);
            drawExplanation(g2);
        }

        private void drawTitle(Graphics2D g2) {
            g2.setColor(COLOR_TEXT);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
            g2.drawString("", 40, 45);
        }

        private void drawCPU(Graphics2D g2) {
            int cpuX = 80, cpuY = 60, cpuW = 600, cpuH = 520;
            
            // Draw Pins
            g2.setColor(COLOR_CPU_PIN);
            for (int i = 0; i < 20; i++) {
                g2.fillRect(cpuX - 15, cpuY + 20 + i*25, 20, 10);
                g2.fillRect(cpuX + cpuW - 5, cpuY + 20 + i*25, 20, 10);
                if(i < 15) {
                    g2.fillRect(cpuX + 40 + i*35, cpuY - 15, 10, 20);
                    g2.fillRect(cpuX + 40 + i*35, cpuY + cpuH - 5, 10, 20);
                }
            }
            
            // CPU Body
            g2.setColor(COLOR_CPU_BG);
            g2.fillRoundRect(cpuX, cpuY, cpuW, cpuH, 20, 20);
            g2.setColor(COLOR_CPU_BG.brighter());
            g2.setStroke(new BasicStroke(3));
            g2.drawRoundRect(cpuX, cpuY, cpuW, cpuH, 20, 20);
            
            // CPU Label
            g2.setColor(new Color(255, 255, 255, 60));
            g2.setFont(new Font("Arial", Font.BOLD, 48));
            g2.drawString("INTEL 8085", cpuX + 30, cpuY + 500);

            // Internal Bus Line
            g2.setColor(COLOR_BUS);
            g2.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(350, cpuY + 40, 350, cpuY + 450);
        }

        private void drawMemory(Graphics2D g2) {
            Rectangle m = blocks.get("Memory (RAM)");
            
            boolean active = activeBlocks.contains("Memory (RAM)");
            Color memBg = COLOR_MEM_BG;
            if (active) {
                memBg = pulseColor(COLOR_MEM_BG, COLOR_HIGHLIGHT, animProgress);
            }
            
            g2.setColor(memBg);
            g2.fillRoundRect(m.x, m.y, m.width, m.height, 15, 15);
            g2.setColor(active ? Color.WHITE : memBg.brighter());
            g2.setStroke(new BasicStroke(active ? 4 : 3));
            g2.drawRoundRect(m.x, m.y, m.width, m.height, 15, 15);
            
            g2.setColor(Color.BLACK);
            for(int i=0; i<4; i++) {
                g2.fillRoundRect(m.x + 30, m.y + 40 + i*80, m.width - 60, 60, 8, 8);
                g2.setColor(Color.DARK_GRAY);
                g2.drawRoundRect(m.x + 30, m.y + 40 + i*80, m.width - 60, 60, 8, 8);
                g2.setColor(Color.BLACK);
            }
            
            g2.setColor(new Color(0xFF, 0xD7, 0x00));
            for(int i=0; i<20; i++) {
                g2.fillRect(m.x - 10, m.y + 40 + i*18, 10, 10);
            }
        }

        private void drawExternalBuses(Graphics2D g2) {
            g2.setStroke(new BasicStroke(10));
            
            boolean dataActive = activeBlocks.contains("Memory (RAM)") || (currentInstruction.startsWith("MVI") || currentInstruction.startsWith("LXI") || currentInstruction.startsWith("J") || currentInstruction.startsWith("C") || currentInstruction.startsWith("R"));
            if (dataActive) {
                g2.setColor(pulseColor(COLOR_BUS, COLOR_HIGHLIGHT, animProgress));
            } else g2.setColor(COLOR_BUS);
            g2.drawLine(680, 250, 880, 250);
            
            if (dataActive) {
                g2.setColor(pulseColor(COLOR_BUS, COLOR_HIGHLIGHT, animProgress));
            } else g2.setColor(COLOR_BUS);
            g2.drawLine(680, 310, 880, 310);
            
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setColor(COLOR_MUTED);
            g2.drawString("Data Bus (8-bit)", 720, 240);
            g2.drawString("Address Bus (16-bit)", 710, 300);
        }

        private void drawBlocks(Graphics2D g2) {
            for (Map.Entry<String, Rectangle> entry : blocks.entrySet()) {
                String name = entry.getKey();
                Rectangle rect = entry.getValue();
                
                if (name.equals("Memory (RAM)")) {
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                    g2.drawString(name, rect.x + 80, rect.y + 30);
                    continue; 
                }
                
                boolean active = activeBlocks.contains(name);
                
                if (active) {
                    g2.setColor(pulseColor(COLOR_BLOCK, COLOR_HIGHLIGHT, animProgress));
                } else {
                    g2.setColor(COLOR_BLOCK);
                }
                
                g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
                
                g2.setColor(active ? Color.WHITE : COLOR_BLOCK_BORDER);
                g2.setStroke(new BasicStroke(active ? 3 : 2));
                g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
                
                g2.setColor(active ? Color.WHITE : COLOR_TEXT);
                g2.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 15));
                FontMetrics fm = g2.getFontMetrics();
                
                String val = getRegValue(name);
                int ty = rect.y + ((rect.height - fm.getHeight()) / 2) + fm.getAscent();
                if (val != null) ty -= 10;
                
                int tx = rect.x + (rect.width - fm.stringWidth(name)) / 2;
                g2.drawString(name, tx, ty);
                
                if (val != null) {
                    g2.setColor(active ? Color.WHITE : new Color(0x34, 0xD3, 0x99)); 
                    g2.setFont(new Font("Consolas", Font.BOLD, 18));
                    fm = g2.getFontMetrics();
                    int vx = rect.x + (rect.width - fm.stringWidth(val)) / 2;
                    int vy = rect.y + rect.height - 10;
                    g2.drawString(val, vx, vy);
                }
                
                if (rect.x > 200 && rect.x < 500) {
                    if (active) {
                        g2.setColor(pulseColor(COLOR_BUS, COLOR_HIGHLIGHT, animProgress));
                    } else {
                        g2.setColor(COLOR_BUS);
                    }
                    g2.setStroke(new BasicStroke(4));
                    if (rect.x > 350) g2.drawLine(350, rect.y + rect.height/2, rect.x, rect.y + rect.height/2);
                    else g2.drawLine(rect.x + rect.width, rect.y + rect.height/2, 350, rect.y + rect.height/2);
                }
            }
        }

        private String getRegValue(String name) {
            if (registers.isEmpty()) return null;
            if (name.equals("Accumulator")) return registers.get("A");
            if (Arrays.asList("B","C","D","E","H","L").contains(name)) return registers.get(name);
            if (name.equals("Stack Ptr")) return registers.get("SP");
            if (name.equals("Prog Ctr")) return registers.get("PC");
            if (name.equals("Flags")) {
                StringBuilder sb = new StringBuilder();
                if(flags.getOrDefault("S", false)) sb.append("S ");
                if(flags.getOrDefault("Z", false)) sb.append("Z ");
                if(flags.getOrDefault("AC", false)) sb.append("AC ");
                if(flags.getOrDefault("P", false)) sb.append("P ");
                if(flags.getOrDefault("CY", false)) sb.append("CY ");
                String v = sb.toString().trim();
                return v.isEmpty() ? "--" : v;
            }
            return null;
        }

        private void drawExplanation(Graphics2D g2) {
            g2.setColor(COLOR_CPU_BG);
            g2.fillRoundRect(80, 620, 220, 70, 15, 15);
            g2.setColor(COLOR_BLOCK_BORDER);
            g2.drawRoundRect(80, 620, 220, 70, 15, 15);
            
            g2.setFont(new Font("Consolas", Font.BOLD, 26));
            g2.setColor(COLOR_HIGHLIGHT);
            g2.drawString(currentInstruction.isEmpty() ? "HALT" : currentInstruction, 100, 665);
            
            g2.setColor(COLOR_CPU_BG);
            g2.fillRoundRect(320, 620, 880, 70, 15, 15);
            g2.setColor(COLOR_BLOCK_BORDER);
            g2.drawRoundRect(320, 620, 880, 70, 15, 15);
            
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            g2.setColor(COLOR_TEXT);
            String text = currentExplanation.isEmpty() ? "Waiting for execution to begin..." : currentExplanation;
            if (text.length() > 90) text = text.substring(0, 87) + "...";
            g2.drawString(text, 340, 660);
        }
    }
}
