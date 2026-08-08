import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.util.Units;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

/**
 * CreatePracticalDialog – generates a formatted .docx Word document
 * containing: header (enrollment), info block, aim, theory (based on
 * instructions used), code, registers/flags table, step-by-step trace,
 * and conclusion.
 */
public class CreatePracticalDialog extends JDialog {

    // ── Colors matching Aura Studio dark theme ──────────────────────────────
    private static final Color BG_DARK   = new Color(0x1A, 0x1A, 0x2E);
    private static final Color BG_CARD   = new Color(0x16, 0x16, 0x2E);
    private static final Color BG_FIELD  = new Color(0x0F, 0x0F, 0x1A);
    private static final Color ACCENT    = new Color(0x00, 0xB4, 0xFF);
    private static final Color TEXT_PRI  = new Color(0xE0, 0xE0, 0xFF);
    private static final Color TEXT_MUT  = new Color(0x70, 0x70, 0x99);
    private static final Color GREEN     = new Color(0x00, 0xCC, 0x66);
    private static final Color BORDER_C  = new Color(0x30, 0x30, 0x55);

    // ── Input fields ─────────────────────────────────────────────────────────
    private JTextField  tfPracticalNumber;
    private JTextField  tfPracticalName;
    private JTextArea   taAim;
    private JTextField  tfDate;
    private JTextField  tfEnrollment;
    private JTextField  tfSubject;

    private final Assembler assembler;
    private final java.util.List<String> stepLog;

    // ─────────────────────────────────────────────────────────────────────────
    public CreatePracticalDialog(Assembler asm, java.util.List<String> stepLog) {
        super(asm, "Create Practical File", true);
        this.assembler = asm;
        this.stepLog   = stepLog != null ? stepLog : new ArrayList<>();
        setSize(660, 580);
        setLocationRelativeTo(asm);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x0D, 0x0D, 0x25));
        header.setBorder(new EmptyBorder(16, 20, 16, 20));
        JLabel title = new JLabel("📄  Create Practical File (.docx)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(ACCENT);
        JLabel sub = new JLabel("Fill in the details — a formatted Word document will be generated.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(TEXT_MUT);
        JPanel hBox = new JPanel(); hBox.setOpaque(false);
        hBox.setLayout(new BoxLayout(hBox, BoxLayout.Y_AXIS));
        hBox.add(title); hBox.add(Box.createVerticalStrut(4)); hBox.add(sub);
        header.add(hBox, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_DARK);
        form.setBorder(new EmptyBorder(20, 24, 8, 24));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill    = GridBagConstraints.HORIZONTAL;
        gc.insets  = new Insets(7, 0, 7, 0);
        gc.weightx = 1.0;
        String today = new SimpleDateFormat("dd/MM/yyyy").format(new Date());

        // Row 0 – Practical No + Name
        gc.gridy = 0; gc.gridx = 0; gc.weightx = 0.20; gc.gridwidth = 1;
        form.add(label("Practical No."), gc);
        gc.gridx = 1; gc.weightx = 0.30;
        tfPracticalNumber = field("e.g. 1"); form.add(tfPracticalNumber, gc);
        gc.gridx = 2; gc.weightx = 0.20; gc.insets = new Insets(7, 14, 7, 0);
        form.add(label("Practical Name"), gc);
        gc.gridx = 3; gc.weightx = 0.30; gc.insets = new Insets(7, 0, 7, 0);
        tfPracticalName = field("e.g. 8-bit Addition"); form.add(tfPracticalName, gc);

        // Row 1 – Aim (multi-line)
        gc.gridy = 1; gc.gridx = 0; gc.weightx = 0.20; gc.gridwidth = 1;
        gc.insets = new Insets(7, 0, 2, 0);
        form.add(label("Practical Aim"), gc);
        gc.gridx = 1; gc.weightx = 0.80; gc.gridwidth = 3;
        taAim = new JTextArea(3, 30);
        taAim.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        taAim.setBackground(BG_FIELD); taAim.setForeground(TEXT_PRI);
        taAim.setCaretColor(ACCENT); taAim.setLineWrap(true); taAim.setWrapStyleWord(true);
        taAim.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_C, 1, true), new EmptyBorder(6, 8, 6, 8)));
        JScrollPane aimScroll = new JScrollPane(taAim); aimScroll.setBorder(null);
        form.add(aimScroll, gc);

        // Row 2 – Date + Enrollment
        gc.gridy = 2; gc.gridx = 0; gc.weightx = 0.20; gc.gridwidth = 1;
        gc.insets = new Insets(7, 0, 7, 0);
        form.add(label("Date"), gc);
        gc.gridx = 1; gc.weightx = 0.30;
        tfDate = field(today); tfDate.setText(today); form.add(tfDate, gc);
        gc.gridx = 2; gc.weightx = 0.20; gc.insets = new Insets(7, 14, 7, 0);
        form.add(label("Enrollment No."), gc);
        gc.gridx = 3; gc.weightx = 0.30; gc.insets = new Insets(7, 0, 7, 0);
        tfEnrollment = field("e.g. 230000000000"); form.add(tfEnrollment, gc);

        // Row 3 – Subject
        gc.gridy = 3; gc.gridx = 0; gc.weightx = 0.20; gc.gridwidth = 1;
        form.add(label("Subject Name"), gc);
        gc.gridx = 1; gc.weightx = 0.80; gc.gridwidth = 3;
        tfSubject = field("e.g. Microprocessor & Interfacing"); form.add(tfSubject, gc);

        root.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        btnRow.setBackground(new Color(0x0D, 0x0D, 0x25));
        btnRow.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_C));

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setForeground(TEXT_MUT); btnCancel.setBackground(BG_CARD);
        btnCancel.setFocusPainted(false); btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancel.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER_C, 1, true), new EmptyBorder(8, 18, 8, 18)));
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> dispose());

        JButton btnGen = new JButton("Generate .docx Practical");
        btnGen.setForeground(new Color(0x00, 0x15, 0x00)); btnGen.setBackground(GREEN);
        btnGen.setOpaque(true); btnGen.setFocusPainted(false);
        btnGen.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnGen.setBorder(BorderFactory.createCompoundBorder(new LineBorder(GREEN.darker(), 1, true), new EmptyBorder(8, 20, 8, 20)));
        btnGen.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGen.addActionListener(e -> generateDocx());

        btnRow.add(btnCancel); btnRow.add(btnGen);
        root.add(btnRow, BorderLayout.SOUTH);
        setContentPane(root);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12)); l.setForeground(ACCENT); return l;
    }
    private JTextField field(String ph) {
        JTextField f = new JTextField(20) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setFont(new Font("Segoe UI", Font.ITALIC, 12)); g2.setColor(TEXT_MUT);
                    g2.drawString(ph, getInsets().left + 4, getHeight() / 2 + 5);
                }
            }
        };
        f.setFont(new Font("Segoe UI", Font.PLAIN, 12)); f.setBackground(BG_FIELD); f.setForeground(TEXT_PRI);
        f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER_C, 1, true), new EmptyBorder(6, 8, 6, 8)));
        return f;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main – generate the .docx
    // ─────────────────────────────────────────────────────────────────────────
    private void generateDocx() {
        String practNo    = tfPracticalNumber.getText().trim();
        String practName  = tfPracticalName.getText().trim();
        String aim        = taAim.getText().trim();
        String date       = tfDate.getText().trim();
        String enrollment = tfEnrollment.getText().trim();
        String subject    = tfSubject.getText().trim();

        if (practNo.isEmpty() || practName.isEmpty() || enrollment.isEmpty() || subject.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please fill in Practical Number, Name, Enrollment Number, and Subject Name.",
                "Missing Info", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Practical Document");
        String suggested = "Practical_" + practNo + "_" + practName.replaceAll("[^a-zA-Z0-9]", "_") + ".docx";
        fc.setSelectedFile(new File(suggested));
        fc.setFileFilter(new FileNameExtensionFilter("Word Document (*.docx)", "docx"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File outFile = fc.getSelectedFile();
        if (!outFile.getName().toLowerCase().endsWith(".docx"))
            outFile = new File(outFile.getPath() + ".docx");

        try {
            XWPFDocument doc = buildDocument(practNo, practName, aim, date, enrollment, subject);
            try (FileOutputStream fos = new FileOutputStream(outFile)) { doc.write(fos); }
            doc.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to create .docx:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            return;
        }

        dispose();
        JOptionPane.showMessageDialog(assembler,
            "Practical document saved!\n" + outFile.getAbsolutePath(),
            "Success", JOptionPane.INFORMATION_MESSAGE);
        try { Desktop.getDesktop().open(outFile); } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Build the XWPFDocument
    // ─────────────────────────────────────────────────────────────────────────
    private XWPFDocument buildDocument(String practNo, String practName,
            String aim, String date, String enrollment, String subject) throws Exception {

        XWPFDocument doc = new XWPFDocument();

        // ── Page margins (narrow) ─────────────────────────────────────────────
        CTSectPr sectPr = doc.getDocument().getBody().addNewSectPr();
        CTPageMar mar = sectPr.addNewPgMar();
        mar.setTop(java.math.BigInteger.valueOf(720));
        mar.setBottom(java.math.BigInteger.valueOf(720));
        mar.setLeft(java.math.BigInteger.valueOf(1080));
        mar.setRight(java.math.BigInteger.valueOf(1080));

        // ── Title paragraph acts as page header ──────────────────────────────
        XWPFParagraph hdrPara = doc.createParagraph();
        hdrPara.setAlignment(ParagraphAlignment.CENTER);
        hdrPara.setBorderBottom(Borders.DOUBLE);
        hdrPara.setSpacingAfter(160);
        XWPFRun hdrRun = hdrPara.createRun();
        hdrRun.setText("ENROLLMENT NO.: " + enrollment);
        hdrRun.setBold(true);
        hdrRun.setFontSize(14);
        hdrRun.setFontFamily("Calibri");
        hdrRun.setColor("1F4E79");
        hdrRun.addCarriageReturn();
        hdrRun.setText("AURA STUDIO — 8085 MICROPROCESSOR SIMULATOR");
        hdrRun.setFontSize(11);
        hdrRun.setColor("2E86AB");

        // ════════════════════════════════════════════════════════════════════
        // TITLE BLOCK
        // ════════════════════════════════════════════════════════════════════
        addTitleBlock(doc, practNo, practName, date, subject);

        // ════════════════════════════════════════════════════════════════════
        // AIM
        // ════════════════════════════════════════════════════════════════════
        addSectionHeading(doc, "AIM");
        String aimText = aim.isEmpty()
            ? "To study and implement the given 8085 assembly language program using Aura Studio simulator."
            : aim;
        addBodyPara(doc, aimText);

        // ════════════════════════════════════════════════════════════════════
        // THEORY
        // ════════════════════════════════════════════════════════════════════
        addSectionHeading(doc, "THEORY");
        addTheory(doc);

        // ════════════════════════════════════════════════════════════════════
        // PROGRAM / CODE
        // ════════════════════════════════════════════════════════════════════
        addSectionHeading(doc, "PROGRAM / CODE");
        addCodeBlock(doc);

        // ════════════════════════════════════════════════════════════════════
        // REGISTERS AND FLAGS
        // ════════════════════════════════════════════════════════════════════
        addSectionHeading(doc, "REGISTERS AND FLAGS (AFTER EXECUTION)");
        addRegistersTable(doc);

        // ════════════════════════════════════════════════════════════════════
        // STEP-BY-STEP TRACE
        // ════════════════════════════════════════════════════════════════════
        if (!stepLog.isEmpty()) {
            addSectionHeading(doc, "STEP-BY-STEP EXECUTION TRACE");
            int step = 1;
            for (String s : stepLog) {
                XWPFParagraph p = doc.createParagraph();
                p.setStyle("ListParagraph");
                p.setNumID(getListNumId(doc));
                XWPFRun r = p.createRun();
                r.setText(s);
                r.setFontFamily("Consolas");
                r.setFontSize(10);
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // CONCLUSION
        // ════════════════════════════════════════════════════════════════════
        addSectionHeading(doc, "CONCLUSION");
        addConclusion(doc, practName, aim);

        return doc;
    }

    // ── Title block: info table ───────────────────────────────────────────────
    private void addTitleBlock(XWPFDocument doc, String practNo, String practName,
                               String date, String subject) {
        // Main Title
        XWPFParagraph title = doc.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        title.setSpacingAfter(80);
        XWPFRun tRun = title.createRun();
        tRun.setText("AURA STUDIO — 8085 MICROPROCESSOR SIMULATOR");
        tRun.setBold(true); tRun.setFontSize(16); tRun.setFontFamily("Calibri");
        tRun.setColor("1F4E79");

        // Info table
        XWPFTable tbl = doc.createTable(4, 2);
        tbl.setWidth("100%");
        CTTblPr tblPr = tbl.getCTTbl().getTblPr();
        if (tblPr == null) tblPr = tbl.getCTTbl().addNewTblPr();

        String[][] rows = {
            {"Subject",           subject},
            {"Practical No.",     practNo},
            {"Practical Name",    practName},
            {"Date",              date}
        };
        for (int i = 0; i < rows.length; i++) {
            XWPFTableRow row = tbl.getRow(i);
            // Label cell
            XWPFTableCell labelCell = row.getCell(0);
            labelCell.setWidth("30%");
            styleCell(labelCell, rows[i][0], true, "1F4E79", "D6E4F0");
            // Value cell
            XWPFTableCell valCell = row.getCell(1);
            valCell.setWidth("70%");
            styleCell(valCell, rows[i][1], false, "000000", "FFFFFF");
        }
        doc.createParagraph(); // spacer
    }

    private void styleCell(XWPFTableCell cell, String text, boolean bold, String color, String bgColor) {
        cell.setColor(bgColor);
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setSpacingBefore(60); p.setSpacingAfter(60);
        XWPFRun r = p.createRun();
        r.setText(text); r.setBold(bold); r.setFontFamily("Calibri"); r.setFontSize(11); r.setColor(color);
    }

    // ── Section heading ───────────────────────────────────────────────────────
    private void addSectionHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.LEFT);
        p.setBorderBottom(Borders.SINGLE);
        p.setSpacingBefore(200); p.setSpacingAfter(100);
        XWPFRun r = p.createRun();
        r.setText(text); r.setBold(true); r.setFontSize(13); r.setFontFamily("Calibri"); r.setColor("1F4E79");
    }

    // ── Plain body paragraph ──────────────────────────────────────────────────
    private void addBodyPara(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.BOTH);
        p.setSpacingAfter(100);
        XWPFRun r = p.createRun();
        r.setText(text); r.setFontFamily("Calibri"); r.setFontSize(11);
    }

    // ── Code block (monospaced, shaded) ──────────────────────────────────────
    private void addCodeBlock(XWPFDocument doc) {
        String code = getRawCode();
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(0);
        CTShd shd = p.getCTP().addNewPPr().addNewShd();
        shd.setFill("F0F0F0"); shd.setColor("auto");
        for (String line : code.split("\n")) {
            XWPFParagraph lp = doc.createParagraph();
            CTShd ls = lp.getCTP().addNewPPr().addNewShd();
            ls.setFill("F3F4F6"); ls.setColor("auto");
            lp.setSpacingAfter(0); lp.setSpacingBefore(0);
            XWPFRun r = lp.createRun();
            r.setText(line.isEmpty() ? " " : line);
            r.setFontFamily("Consolas"); r.setFontSize(10);
        }
        doc.createParagraph(); // spacer
    }

    // ── Registers and flags table ─────────────────────────────────────────────
    private void addRegistersTable(XWPFDocument doc) {
        if (assembler == null || assembler.matrix == null) {
            addBodyPara(doc, "[Run the program first to populate register values]");
            return;
        }
        Matrix m = assembler.matrix;
        int mVal = 0;
        try {
            int hl = ((m.H & 0xFF) << 8) | (m.L & 0xFF);
            if (hl >= 0 && hl < m.memory.length) mVal = m.memory[hl] & 0xFF;
        } catch (Exception ignored) {}

        // Registers table
        XWPFTable regTbl = doc.createTable(9, 4);
        regTbl.setWidth("100%");
        // Header row
        XWPFTableRow hdr = regTbl.getRow(0);
        for (int c = 0; c < 4; c++) {
            String[] hdrs = {"Register", "Value (Hex)", "Register / Pointer", "Value (Hex)"};
            styleCell(hdr.getCell(c), hdrs[c], true, "FFFFFF", "1F4E79");
        }
        // Data rows
        String[][] regData = {
            {"A (Accumulator)", assembler.engine.Dec2Hex2digit(m.A & 0xFF), "H", assembler.engine.Dec2Hex2digit(m.H & 0xFF)},
            {"B", assembler.engine.Dec2Hex2digit(m.B & 0xFF), "L", assembler.engine.Dec2Hex2digit(m.L & 0xFF)},
            {"C", assembler.engine.Dec2Hex2digit(m.C & 0xFF), "M [HL]", assembler.engine.Dec2Hex2digit(mVal)},
            {"D", assembler.engine.Dec2Hex2digit(m.D & 0xFF), "PC", assembler.engine.Dec2Hex(m.PC)},
            {"E", assembler.engine.Dec2Hex2digit(m.E & 0xFF), "SP", assembler.engine.Dec2Hex(m.SP)},
            {"HL (pair)", assembler.engine.Dec2Hex(((m.H & 0xFF) << 8) | (m.L & 0xFF)), "BC (pair)", assembler.engine.Dec2Hex(((m.B & 0xFF) << 8) | (m.C & 0xFF))},
            {"DE (pair)", assembler.engine.Dec2Hex(((m.D & 0xFF) << 8) | (m.E & 0xFF)), "PSW (F)", assembler.engine.Dec2Hex2digit(m.F & 0xFF)},
            {"Instructions", String.valueOf(m.instructionCounter), "Clock Cycles", String.valueOf(m.clockCycleCounter)}
        };
        for (int i = 0; i < regData.length; i++) {
            XWPFTableRow row = regTbl.getRow(i + 1);
            String bg = (i % 2 == 0) ? "EBF5FB" : "FFFFFF";
            for (int c = 0; c < 4; c++) {
                String val = c < regData[i].length ? regData[i][c] : "";
                styleCell(row.getCell(c), val, false, "000000", bg);
            }
        }

        doc.createParagraph(); // spacer

        // Flags table
        addBodyPara(doc, "FLAGS:");
        int f = m.F;
        boolean[] flags = {(f & 0x80) != 0, (f & 0x40) != 0, (f & 0x10) != 0, (f & 0x04) != 0, (f & 0x01) != 0};
        String[] flagNames = {"Sign (S)", "Zero (Z)", "Auxiliary Carry (AC)", "Parity (P)", "Carry (CY)"};
        XWPFTable flagTbl = doc.createTable(2, 5);
        flagTbl.setWidth("100%");
        XWPFTableRow fnRow = flagTbl.getRow(0);
        XWPFTableRow fvRow = flagTbl.getRow(1);
        for (int i = 0; i < 5; i++) {
            styleCell(fnRow.getCell(i), flagNames[i], true, "FFFFFF", "1F4E79");
            styleCell(fvRow.getCell(i), flags[i] ? "SET (1)" : "RESET (0)", false,
                      flags[i] ? "006600" : "CC0000", flags[i] ? "E8F8E8" : "FFE8E8");
        }
        doc.createParagraph(); // spacer
    }

    // ── Theory section ────────────────────────────────────────────────────────
    private void addTheory(XWPFDocument doc) {
        String code = getRawCode().toUpperCase();

        addBodyPara(doc,
            "The 8085 is an 8-bit general-purpose microprocessor by Intel. It has a 16-bit address " +
            "bus, an 8-bit data bus, and supports a comprehensive instruction set. The following " +
            "describes the instructions relevant to this practical:");

        if (hasAny(code, "MOV", "MVI", "LDA", "STA", "LHLD", "SHLD", "LXI", "LDAX", "STAX", "XCHG", "PUSH", "POP")) {
            addSubHeading(doc, "Data Transfer Instructions");
            String[][] dt = {
                {"MOV  Rd, Rs", "Copies contents of source register into the destination register."},
                {"MVI  Rd, d8", "Moves an 8-bit immediate data value into the specified register or memory."},
                {"LDA  addr",   "Loads the accumulator with the byte stored at the given memory address."},
                {"STA  addr",   "Stores the accumulator content at the specified memory address."},
                {"LXI  Rp,d16", "Loads a 16-bit immediate value directly into a register pair."},
                {"LHLD addr",   "Loads HL register pair directly from memory."},
                {"SHLD addr",   "Stores HL register pair directly to memory."},
                {"PUSH Rp",     "Pushes the register pair content onto the stack (SP decremented by 2)."},
                {"POP  Rp",     "Pops two bytes from the stack into the register pair (SP incremented by 2)."},
                {"XCHG",        "Exchanges the contents of the DE pair with the HL pair."}
            };
            addInstructionTable(doc, dt);
        }
        if (hasAny(code, "ADD", "ADC", "ADI", "ACI", "SUB", "SBB", "SUI", "INR", "DCR", "INX", "DCX", "DAD", "DAA")) {
            addSubHeading(doc, "Arithmetic Instructions");
            String[][] ar = {
                {"ADD  R/M",   "Adds the content of a register/memory to the accumulator."},
                {"ADC  R/M",   "Adds register/memory content plus the carry flag to the accumulator."},
                {"ADI  d8",    "Adds an 8-bit immediate value to the accumulator."},
                {"SUB  R/M",   "Subtracts the register/memory content from the accumulator."},
                {"SBB  R/M",   "Subtracts register/memory content and borrow from the accumulator."},
                {"INR  R/M",   "Increments the specified register or memory by 1."},
                {"DCR  R/M",   "Decrements the specified register or memory by 1."},
                {"INX  Rp",    "Increments the 16-bit value in the register pair by 1."},
                {"DCX  Rp",    "Decrements the 16-bit value in the register pair by 1."},
                {"DAD  Rp",    "Adds the register pair to HL (16-bit addition); result in HL."},
                {"DAA",        "Decimal Adjust Accumulator: adjusts result after BCD arithmetic."}
            };
            addInstructionTable(doc, ar);
        }
        if (hasAny(code, "ANA", "ORA", "XRA", "CMP", "ANI", "ORI", "XRI", "CPI", "RLC", "RRC", "RAL", "RAR", "CMA", "CMC", "STC")) {
            addSubHeading(doc, "Logical / Rotate Instructions");
            String[][] lg = {
                {"ANA  R/M",  "Logical AND of accumulator with register/memory. Clears CY, sets AC."},
                {"ORA  R/M",  "Logical OR of accumulator with register/memory."},
                {"XRA  R/M",  "Logical XOR of accumulator with register/memory."},
                {"CMP  R/M",  "Compares register/memory with accumulator by subtraction (no store)."},
                {"ANI  d8",   "Logical AND of accumulator with 8-bit immediate data."},
                {"ORI  d8",   "Logical OR of accumulator with 8-bit immediate data."},
                {"XRI  d8",   "Logical XOR of accumulator with 8-bit immediate data."},
                {"CPI  d8",   "Compares accumulator with 8-bit immediate value; sets flags."},
                {"RLC",       "Rotates accumulator left; bit 7 goes to bit 0 and CY flag."},
                {"RRC",       "Rotates accumulator right; bit 0 goes to bit 7 and CY flag."},
                {"CMA",       "Complements all bits of the accumulator (1's complement)."},
                {"STC",       "Sets the carry flag to 1."}
            };
            addInstructionTable(doc, lg);
        }
        if (hasAny(code, "JMP", "JNZ", "JZ", "JNC", "JC", "JM", "JP", "CALL", "RET", "PCHL")) {
            addSubHeading(doc, "Branching / Control Transfer Instructions");
            String[][] br = {
                {"JMP  addr", "Unconditional jump: transfers control to the specified address."},
                {"JNZ  addr", "Jump if Zero flag is NOT set."},
                {"JZ   addr", "Jump if Zero flag IS set."},
                {"JNC  addr", "Jump if Carry flag is NOT set."},
                {"JC   addr", "Jump if Carry flag IS set."},
                {"JM   addr", "Jump if Sign flag is set (result was negative)."},
                {"JP   addr", "Jump if Sign flag is NOT set (result was positive)."},
                {"CALL addr", "Calls subroutine: saves return address on stack, jumps to addr."},
                {"RET",       "Returns from subroutine: restores PC from stack."}
            };
            addInstructionTable(doc, br);
        }
        if (hasAny(code, "NOP", "HLT", "EI", "DI", "RIM", "SIM", "RST")) {
            addSubHeading(doc, "Machine Control / Miscellaneous Instructions");
            String[][] mc = {
                {"NOP",  "No Operation. Instruction fetched and decoded but nothing executed."},
                {"HLT",  "Halt: stops execution. An interrupt or RESET is needed to resume."},
                {"EI",   "Enable Interrupts: allows the processor to accept maskable interrupts."},
                {"DI",   "Disable Interrupts: processor ignores maskable interrupts."},
                {"RIM",  "Read Interrupt Mask: reads SID bit and interrupt enable status."},
                {"SIM",  "Set Interrupt Mask: sets interrupt masks and SOD output bit."}
            };
            addInstructionTable(doc, mc);
        }
    }

    private boolean hasAny(String code, String... mnemonics) {
        for (String m : mnemonics) if (code.contains(m)) return true;
        return false;
    }

    private void addSubHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(120); p.setSpacingAfter(60);
        XWPFRun r = p.createRun();
        r.setText(text); r.setBold(true); r.setFontFamily("Calibri"); r.setFontSize(11); r.setColor("2E86AB");
    }

    private void addInstructionTable(XWPFDocument doc, String[][] rows) {
        XWPFTable tbl = doc.createTable(rows.length + 1, 2);
        tbl.setWidth("100%");
        // Header
        styleCell(tbl.getRow(0).getCell(0), "Instruction / Opcode", true, "FFFFFF", "2E86AB");
        styleCell(tbl.getRow(0).getCell(1), "Description", true, "FFFFFF", "2E86AB");
        for (int i = 0; i < rows.length; i++) {
            String bg = (i % 2 == 0) ? "EAF4FC" : "FFFFFF";
            styleCell(tbl.getRow(i + 1).getCell(0), rows[i][0], true,  "1A1A2E", bg);
            styleCell(tbl.getRow(i + 1).getCell(1), rows[i][1], false, "1A1A2E", bg);
            // monospace for instruction column
            XWPFRun instrRun = tbl.getRow(i + 1).getCell(0).getParagraphs().get(0).getRuns().get(0);
            instrRun.setFontFamily("Consolas");
            instrRun.setFontSize(10);
        }
        doc.createParagraph();
    }

    // ── Conclusion ────────────────────────────────────────────────────────────
    private void addConclusion(XWPFDocument doc, String practName, String aim) {
        String code = getRawCode().toUpperCase();
        StringBuilder sb = new StringBuilder();
        sb.append("Thus, the practical \"").append(practName)
          .append("\" has been successfully implemented using the 8085 assembly language ");
        sb.append("in Aura Studio Microprocessor Simulator.\n\n");
        if (!aim.isEmpty()) sb.append("The aim was: ").append(aim).append("\n\n");
        sb.append("Through this practical, we observed the following:\n");
        if (hasAny(code, "ADD", "ADI", "ADC")) sb.append("• The ADD/ADI/ADC instructions performed arithmetic addition and stored the result in the Accumulator.\n");
        if (hasAny(code, "SUB", "SUI", "SBB")) sb.append("• Subtraction was performed using SUB/SUI/SBB instructions.\n");
        if (hasAny(code, "MOV", "MVI")) sb.append("• Data was efficiently transferred between registers using MOV and MVI instructions.\n");
        if (hasAny(code, "LDA", "STA")) sb.append("• LDA and STA instructions were used to read from and write to memory locations.\n");
        if (hasAny(code, "JMP", "JNZ", "JZ", "JC", "JNC")) sb.append("• Branching/looping was achieved using conditional and unconditional jump instructions.\n");
        if (hasAny(code, "CALL", "RET")) sb.append("• Subroutine calls were made using CALL and RET, demonstrating stack usage.\n");
        if (hasAny(code, "CMP", "CPI")) sb.append("• The CMP/CPI instructions compared values by setting appropriate flags.\n");
        if (hasAny(code, "HLT")) sb.append("• The HLT instruction was used to gracefully terminate program execution.\n");
        sb.append("\nThe program executed correctly and the register/flag values confirm the expected output was achieved.");
        addBodyPara(doc, sb.toString());
    }

    // ── Get raw assembly code ─────────────────────────────────────────────────
    private String getRawCode() {
        try {
            if (assembler != null && assembler.jTextAreaAssemblyLanguageEditor != null) {
                String c = assembler.jTextAreaAssemblyLanguageEditor.getText();
                if (c != null && !c.trim().isEmpty()) return c;
            }
        } catch (Exception ignored) {}
        return "; No assembly code loaded.";
    }

    // ── Numbering helper (for step list) ─────────────────────────────────────
    private java.math.BigInteger getListNumId(XWPFDocument doc) {
        try {
            XWPFNumbering num = doc.createNumbering();
            CTAbstractNum abstractNum = CTAbstractNum.Factory.newInstance();
            abstractNum.setAbstractNumId(java.math.BigInteger.valueOf(0));
            CTLvl lvl = abstractNum.addNewLvl();
            lvl.setIlvl(java.math.BigInteger.valueOf(0));
            lvl.addNewNumFmt().setVal(STNumberFormat.DECIMAL);
            lvl.addNewLvlText().setVal("%1.");
            lvl.addNewStart().setVal(java.math.BigInteger.valueOf(1));
            XWPFAbstractNum xAbstractNum = new XWPFAbstractNum(abstractNum);
            java.math.BigInteger abstractNumID = num.addAbstractNum(xAbstractNum);
            return num.addNum(abstractNumID);
        } catch (Exception e) {
            return java.math.BigInteger.valueOf(1);
        }
    }
}
