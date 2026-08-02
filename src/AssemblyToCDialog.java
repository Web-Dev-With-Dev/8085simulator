import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

/**
 * AssemblyToCDialog provides a modern, interactive Swing dialog 
 * for viewing and exporting converted C code logic from 8085 assembly.
 */
public class AssemblyToCDialog extends JFrame {

    private JTextArea asmTextArea;
    private JTextArea cTextArea;
    private Assembler asm;

    public AssemblyToCDialog(Assembler asm) {
        this.asm = asm;
        setTitle("8085 Assembly to C / Microcontroller Code Converter");
        setSize(900, 650);
        setLocationRelativeTo(asm);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header Label
        JLabel headerLabel = new JLabel("<html><b style='font-size:14px; color:#0d6efd;'>Code to C / Microcontroller Converter</b><br>"
                + "<span style='font-size:11px; color:#6c757d;'>Converts 8085 assembly routines into equivalent C code for 8051, AVR, PIC, STM32 & GCC/Clang compilers.</span></html>");
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // Split Pane: Left = Assembly, Right = C Code
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.4);

        // Left Panel (Assembly Input)
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.add(new JLabel("8085 Assembly Code:"), BorderLayout.NORTH);
        asmTextArea = new JTextArea();
        asmTextArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        if (asm != null && asm.jTextAreaAssemblyLanguageEditor != null) {
            asmTextArea.setText(asm.jTextAreaAssemblyLanguageEditor.getText());
        }
        leftPanel.add(new JScrollPane(asmTextArea), BorderLayout.CENTER);

        // Right Panel (C Output)
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.add(new JLabel("Converted C Code Logic:"), BorderLayout.NORTH);
        cTextArea = new JTextArea();
        cTextArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        cTextArea.setEditable(true);
        cTextArea.setBackground(new Color(245, 247, 250));
        rightPanel.add(new JScrollPane(cTextArea), BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Bottom Action Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JButton convertButton = new JButton("Convert Code");
        convertButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        convertButton.addActionListener(e -> convertCode());

        JButton copyButton = new JButton("Copy C Code");
        copyButton.addActionListener(e -> copyToClipboard());

        JButton saveButton = new JButton("Save to .c File");
        saveButton.addActionListener(e -> saveCFile());

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(convertButton);
        buttonPanel.add(copyButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);

        // Perform initial conversion
        convertCode();
    }

    private void convertCode() {
        String inputAsm = asmTextArea.getText();
        String cCode = AssemblyToCConverter.convertToC(inputAsm);
        cTextArea.setText(cCode);
        cTextArea.setCaretPosition(0);
    }

    private void copyToClipboard() {
        String text = cTextArea.getText();
        if (text != null && !text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            JOptionPane.showMessageDialog(this, "Converted C code copied to clipboard!", "Copied", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void saveCFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Converted C File");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("C Source Code (*.c)", "c"));
        fc.setSelectedFile(new File("converted_routine.c"));
        int userSelection = fc.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fc.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".c")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".c");
            }
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileToSave)))) {
                out.print(cTextArea.getText());
                JOptionPane.showMessageDialog(this, "C code file saved successfully:\n" + fileToSave.getAbsolutePath(), "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to save C file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
