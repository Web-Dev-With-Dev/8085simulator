import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class setStopMnemonic extends javax.swing.JFrame {

    Assembler o;
    int n;
    String s="oh";
    AssemblerEngine engine;

    public setStopMnemonic() {
        initComponents();
        applyDarkTheme();
    }

    public setStopMnemonic(Assembler o) {
        o.setEnabled(false);
        this.o=o;
        initComponents();
        engine=o.engine;
        jTextField1.setText(engine.S[o.stopAtIndex]);
        applyDarkTheme();
    }

    private void applyDarkTheme() {
        Color bg = new Color(0x0A, 0x0C, 0x10);
        Color cardBg = new Color(0x14, 0x16, 0x1D);
        Color border = new Color(0x23, 0x27, 0x33);
        Color text = new Color(0xED, 0xF2, 0xF7);
        Color accent = new Color(0x0D, 0x6E, 0xFD);

        getContentPane().setBackground(bg);
        if (jTextField1 != null) {
            jTextField1.setBackground(cardBg);
            jTextField1.setForeground(text);
            jTextField1.setCaretColor(new Color(0x0D, 0xCA, 0xF0));
            jTextField1.setFont(new Font("Consolas", Font.BOLD, 13));
            jTextField1.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(border, 1, true),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
        }
        if (jButton1 != null) {
            jButton1.setBackground(accent);
            jButton1.setForeground(Color.WHITE);
            jButton1.setFont(new Font("Segoe UI", Font.BOLD, 12));
            jButton1.setFocusPainted(false);
            jButton1.setOpaque(true);
            jButton1.setCursor(new Cursor(Cursor.HAND_CURSOR));
            jButton1.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Mnemonic");
        setBounds(new java.awt.Rectangle(500, 300, 0, 0));
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jTextField1.setName("jTextField1");
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });
        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField1KeyPressed(evt);
            }
        });

        jButton1.setText("oh");
        jButton1.setName("jButton1");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jButton1)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {
        jButton1.setText(s);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        if(jButton1.getText().equalsIgnoreCase("Set")){
            n=engine.getIndexFromMnemonic(jTextField1.getText());
            jTextField1.setText(engine.S[n]);
            jButton1.setText("OK");
            o.stopAtIndex=n;
        }
        else if(jButton1.getText().equalsIgnoreCase("Seconds")){
            try {
                o.speed[0]=Float.parseFloat(jTextField1.getText().toString().trim());
                jButton1.setText("OK");
            } catch (Exception e) {
                jTextField1.setText("0");
                jButton1.setText("Seconds");
            }
        }
        else if(jButton1.getText().equalsIgnoreCase("OK")){
            o.setEnabled(true);
            dispose();
        }
    }

    private void jTextField1KeyPressed(java.awt.event.KeyEvent evt) {
        jButton1.setText(s);
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        if (o != null) o.setEnabled(true);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new setStopMnemonic().setVisible(true);
            }
        });
    }

    public javax.swing.JButton jButton1;
    public javax.swing.JTextField jTextField1;
}
