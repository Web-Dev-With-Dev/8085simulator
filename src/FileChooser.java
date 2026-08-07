import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileChooser extends javax.swing.JFrame {

    public static int objectNo = 0;
    public String path = "";
    Assembler o;
    FileFilter fileFilter;

    public FileChooser() {
        System.out.println(objectNo);
        objectNo++;
        if (objectNo == 1)
            initComponents();
        jFileChooser1.setApproveButtonText("Open");

    }

    public FileChooser(String s, Assembler o) {
        objectNo++;
        o.setEnabled(false);
        initComponents();
        File file = new File(o.path);
        jFileChooser1.setSelectedFile(file);
        jFileChooser1.setApproveButtonText(s);
        setTitle(s);
        jFileChooser1.setApproveButtonMnemonic(s.length() > 0 ? s.charAt(0) : 'O');

        String lowerS = s.toLowerCase();
        if (lowerS.contains("binary") || lowerS.contains("bin")) {
            fileFilter = new FileNameExtensionFilter("Raw Binary File (.bin)", "bin");
        } else if (lowerS.contains("hex")) {
            fileFilter = new FileNameExtensionFilter("Intel HEX File (.hex)", "hex");
        } else if (lowerS.contains("doc")) {
            fileFilter = new FileNameExtensionFilter("Word Document / Code Text (.docx, .doc, .txt)", "docx", "doc",
                    "txt");
        } else if (lowerS.contains("c code") || lowerS.contains("c file") || lowerS.endsWith(".c")) {
            fileFilter = new FileNameExtensionFilter("C Source Code (.c)", "c");
        } else {
            fileFilter = new FileNameExtensionFilter("8085 Assembler Language (.asm)", "asm");
        }

        jFileChooser1.setFileFilter(fileFilter);
        jFileChooser1.setFileHidingEnabled(false);
        this.o = o;
        objectNo = 0;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFileChooser1 = new javax.swing.JFileChooser();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        setBounds(new java.awt.Rectangle(300, 200, 0, 0));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jFileChooser1.setName("jFileChooser1"); // NOI18N
        jFileChooser1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFileChooser1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jFileChooser1, javax.swing.GroupLayout.DEFAULT_SIZE, 572, Short.MAX_VALUE)
                                .addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(jFileChooser1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap()));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jFileChooser1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jFileChooser1ActionPerformed
        o.setEnabled(true);
        if (evt.getActionCommand().equalsIgnoreCase("CancelSelection")) {
            dispose();
            if (o.closeStateCall)
                (new Popup(o)).terminate("Are you sure you want to exit without saving changes ? ");
        }
        if (evt.getActionCommand().equalsIgnoreCase("ApproveSelection")) {
            path = jFileChooser1.getSelectedFile().toString();
            o.path = path;
            o.setTitle("AURA SIMULATOR - " + o.path);
            File selectedFile = jFileChooser1.getSelectedFile();
            String btnText = jFileChooser1.getApproveButtonText();

            if (btnText.equalsIgnoreCase("Load Mnemonics")) {
                if (o.modernIDEUI != null) {
                    o.modernIDEUI.openFile(selectedFile);
                } else {
                    String s = "", line;
                    try {
                        BufferedReader in = new BufferedReader(new FileReader(path));
                        while ((line = in.readLine()) != null) {
                            s = s + line + "\n";
                        }
                        o.jTextAreaAssemblyLanguageEditor.setText(s);
                        o.textEditor.colorEditor();
                        in.close();
                    } catch (Exception e) {
                        Popup.show("Failed to load the file.");
                    }
                }
            } else if (btnText.equalsIgnoreCase("Save Mnemonics")) {
                try {
                    path = path.replace(".asm", "");
                } catch (Exception e) {
                }
                try {
                    File saveFile = new File(path + ".asm");
                    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(saveFile)));
                    out.print(o.jTextAreaAssemblyLanguageEditor.getText());
                    out.close();
                    if (o.modernIDEUI != null) {
                        o.modernIDEUI.onFileSaved(saveFile);
                    }
                } catch (Exception e) {
                    Popup.show("Unable to save the file.");
                }
            } else if (btnText.contains("Intel HEX") || btnText.equalsIgnoreCase("Load Hexcode")) {
                if (!path.contains("."))
                    path = path + ".hex";
                File f = new File(path);
                boolean success = BinaryHexManager.importIntelHex(o, f);
                // Also show in disassembler text view
                try (BufferedReader in = new BufferedReader(new FileReader(f))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    o.jTextAreaDisassembler.setText(sb.toString());
                } catch (Exception e) {
                }
                if (!success) {
                    Popup.show("Failed to load Intel HEX file.");
                }
            } else if (btnText.contains("Save Intel HEX") || btnText.equalsIgnoreCase("Save Hexcode")) {
                if (!path.contains("."))
                    path = path + ".hex";
                File f = new File(path);
                boolean success = BinaryHexManager.exportIntelHex(o, f, 0, -1);
                if (!success) {
                    Popup.show("Unable to save Intel HEX file.");
                }
            } else if (btnText.contains("Load Raw Binary") || btnText.contains("Load Binary")) {
                if (!path.contains("."))
                    path = path + ".bin";
                File f = new File(path);
                int startAddr = 0;
                String addrInput = javax.swing.JOptionPane.showInputDialog(this,
                        "Enter Start Memory Address (Hex or Dec):", "0000");
                if (addrInput != null && !addrInput.trim().isEmpty()) {
                    try {
                        String clean = addrInput.trim();
                        if (clean.toLowerCase().startsWith("0x")) {
                            startAddr = Integer.parseInt(clean.substring(2), 16);
                        } else if (clean.toLowerCase().endsWith("h")) {
                            startAddr = Integer.parseInt(clean.substring(0, clean.length() - 1), 16);
                        } else {
                            startAddr = Integer.parseInt(clean, 16);
                        }
                    } catch (Exception ex) {
                        startAddr = 0;
                    }
                }
                boolean success = BinaryHexManager.importRawBinary(o, f, startAddr);
                if (!success) {
                    Popup.show("Failed to load raw binary file.");
                }
            } else if (btnText.contains("Save Raw Binary") || btnText.contains("Save Binary")) {
                if (!path.contains("."))
                    path = path + ".bin";
                File f = new File(path);
                boolean success = BinaryHexManager.exportRawBinary(o, f, 0, -1);
                if (!success) {
                    Popup.show("Unable to save raw binary file.");
                }
            } else if (btnText.contains("Import Documentation") || btnText.contains("Import Doc")) {
                File f = new File(path);
                boolean success = BinaryHexManager.importDocumentation(o, f);
                if (!success) {
                    Popup.show("Failed to import documentation file.");
                }
            } else if (btnText.contains("Export Documentation") || btnText.contains("Export Doc")) {
                if (!path.contains("."))
                    path = path + ".docx";
                File f = new File(path);
                boolean success = BinaryHexManager.exportDocumentationDoc(o, f);
                if (!success) {
                    Popup.show("Unable to export documentation.");
                }
            } else if (btnText.contains("Export to C Code") || btnText.contains("Save C File")) {
                if (!path.contains("."))
                    path = path + ".c";
                File f = new File(path);
                String cCode = AssemblyToCConverter.convertToC(o.jTextAreaAssemblyLanguageEditor.getText());
                try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)))) {
                    out.print(cCode);
                } catch (Exception ex) {
                    Popup.show("Unable to export C code file.");
                }
            }
            o.jTextAreaAssemblyLanguageEditor.select(0, 0);

            dispose();
            if (o.closeStateCall)
                System.exit(0);
        }
    }// GEN-LAST:event_jFileChooser1ActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_formWindowClosing
        o.setEnabled(true);
    }// GEN-LAST:event_formWindowClosing

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                FileChooser f = new FileChooser();
                f.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JFileChooser jFileChooser1;
    // End of variables declaration//GEN-END:variables

}
