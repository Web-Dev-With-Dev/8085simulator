import java.util.*;
import java.util.regex.*;

public class SyntaxLinter {

    public static class LintError {
        public final int lineNumber;
        public final String lineText;
        public final String message;

        public LintError(int lineNumber, String lineText, String message) {
            this.lineNumber = lineNumber;
            this.lineText = lineText;
            this.message = message;
        }

        @Override
        public String toString() {
            return "Line " + lineNumber + ": " + message + " -> \"" + lineText.trim() + "\"";
        }
    }

    private static final Set<String> VALID_REGISTERS = new HashSet<>(
            Arrays.asList("A", "B", "C", "D", "E", "H", "L", "M")
    );

    private static final Set<String> REGISTER_PAIRS_LXI = new HashSet<>(
            Arrays.asList("B", "D", "H", "SP")
    );

    private static final Set<String> REGISTER_PAIRS_PUSH_POP = new HashSet<>(
            Arrays.asList("B", "D", "H", "PSW")
    );

    private static final Set<String> REGISTER_PAIRS_STAX_LDAX = new HashSet<>(
            Arrays.asList("B", "D")
    );

    private static final Set<String> NO_OPERAND_MNEMONICS = new HashSet<>(Arrays.asList(
            "NOP", "RLC", "RRC", "RAL", "RAR", "RIM", "SIM", "DAA", "CMA", "STC", "CMC",
            "HLT", "RET", "RC", "RNC", "RZ", "RNZ", "RP", "RM", "RPE", "RPO",
            "XCHG", "XTHL", "SPHL", "PCHL", "EI", "DI"
    ));

    private static final Set<String> SINGLE_REG_MNEMONICS = new HashSet<>(Arrays.asList(
            "INR", "DCR", "ADD", "ADC", "SUB", "SBB", "ANA", "XRA", "ORA", "CMP"
    ));

    private static final Set<String> SINGLE_PAIR_MNEMONICS = new HashSet<>(Arrays.asList(
            "INX", "DCX", "DAD"
    ));

    private static final Set<String> JUMP_CALL_MNEMONICS = new HashSet<>(Arrays.asList(
            "JMP", "JC", "JNC", "JZ", "JNZ", "JP", "JM", "JPE", "JPO",
            "CALL", "CC", "CNC", "CZ", "CNZ", "CP", "CM", "CPE", "CPO"
    ));

    private static final Set<String> IMMEDIATE_BYTE_MNEMONICS = new HashSet<>(Arrays.asList(
            "ADI", "ACI", "SUI", "SBI", "ANI", "XRI", "ORI", "CPI", "IN", "OUT"
    ));

    private static final Set<String> IMMEDIATE_WORD_MNEMONICS = new HashSet<>(Arrays.asList(
            "STA", "LDA", "SHLD", "LHLD"
    ));

    public static List<LintError> lint(String sourceCode) {
        List<LintError> errors = new ArrayList<>();
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return errors;
        }

        String[] lines = sourceCode.split("\n", -1);
        Set<String> declaredLabels = new HashSet<>();

        // First pass: collect labels
        for (String line : lines) {
            String trimmed = line.trim();
            int colonIdx = trimmed.indexOf(':');
            if (colonIdx > 0 && !trimmed.startsWith(";") && !trimmed.startsWith("//") && !trimmed.startsWith("#")) {
                String lbl = trimmed.substring(0, colonIdx).trim().toUpperCase();
                if (!lbl.isEmpty() && Pattern.matches("[A-Z_][A-Z0-9_]*", lbl)) {
                    declaredLabels.add(lbl);
                }
            }
        }

        // Second pass: lint lines
        for (int i = 0; i < lines.length; i++) {
            String originalLine = lines[i];
            String trimmed = originalLine.trim();

            if (trimmed.isEmpty() || trimmed.startsWith(";") || trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith(".")) {
                continue;
            }

            // Strip comment
            int commentIdx = -1;
            for (int c = 0; c < trimmed.length(); c++) {
                char ch = trimmed.charAt(c);
                if (ch == ';' || ch == '#') { commentIdx = c; break; }
                if (ch == '/' && c + 1 < trimmed.length() && trimmed.charAt(c + 1) == '/') { commentIdx = c; break; }
            }
            if (commentIdx != -1) {
                trimmed = trimmed.substring(0, commentIdx).trim();
            }
            if (trimmed.isEmpty()) continue;

            // Strip label
            int colonIdx = trimmed.indexOf(':');
            if (colonIdx != -1) {
                String labelPart = trimmed.substring(0, colonIdx).trim();
                if (!Pattern.matches("[A-Za-z_][A-Za-z0-9_]*", labelPart)) {
                    errors.add(new LintError(i + 1, originalLine, "Invalid label name '" + labelPart + "'"));
                }
                trimmed = trimmed.substring(colonIdx + 1).trim();
            }
            if (trimmed.isEmpty()) continue;

            // Directives check
            String upper = trimmed.toUpperCase();
            if (upper.startsWith("DB ") || upper.startsWith("DW ") || upper.startsWith("EQU ") || upper.startsWith("ORG ") || upper.startsWith("#DEFINE")) {
                continue;
            }

            // Split mnemonic and operands
            String[] tokens = trimmed.split("\\s+", 2);
            String mnemonic = tokens[0].toUpperCase().trim();
            String operandStr = tokens.length > 1 ? tokens[1].trim() : "";

            // Validate Mnemonic
            if (NO_OPERAND_MNEMONICS.contains(mnemonic)) {
                if (!operandStr.isEmpty()) {
                    errors.add(new LintError(i + 1, originalLine, "Instruction '" + mnemonic + "' takes no operands"));
                }
            } else if (mnemonic.equals("MOV")) {
                String[] ops = operandStr.split("\\s*,\\s*");
                if (ops.length != 2) {
                    errors.add(new LintError(i + 1, originalLine, "MOV requires 2 operands (e.g. MOV A, B)"));
                } else {
                    String src = ops[0].toUpperCase();
                    String dst = ops[1].toUpperCase();
                    if (!VALID_REGISTERS.contains(src)) {
                        errors.add(new LintError(i + 1, originalLine, "Invalid register '" + ops[0] + "' in MOV"));
                    }
                    if (!VALID_REGISTERS.contains(dst)) {
                        errors.add(new LintError(i + 1, originalLine, "Invalid register '" + ops[1] + "' in MOV"));
                    }
                    if (src.equals("M") && dst.equals("M")) {
                        errors.add(new LintError(i + 1, originalLine, "MOV M, M is an invalid instruction"));
                    }
                }
            } else if (mnemonic.equals("MVI")) {
                String[] ops = operandStr.split("\\s*,\\s*");
                if (ops.length != 2) {
                    errors.add(new LintError(i + 1, originalLine, "MVI requires register and byte value (e.g. MVI A, 05H)"));
                } else {
                    if (!VALID_REGISTERS.contains(ops[0].toUpperCase())) {
                        errors.add(new LintError(i + 1, originalLine, "Invalid register '" + ops[0] + "' in MVI"));
                    }
                }
            } else if (mnemonic.equals("LXI")) {
                String[] ops = operandStr.split("\\s*,\\s*");
                if (ops.length != 2) {
                    errors.add(new LintError(i + 1, originalLine, "LXI requires register pair and 16-bit address/data (e.g. LXI H, 2000H)"));
                } else {
                    if (!REGISTER_PAIRS_LXI.contains(ops[0].toUpperCase())) {
                        errors.add(new LintError(i + 1, originalLine, "Invalid register pair '" + ops[0] + "' in LXI (must be B, D, H, or SP)"));
                    }
                }
            } else if (SINGLE_REG_MNEMONICS.contains(mnemonic)) {
                if (operandStr.isEmpty()) {
                    errors.add(new LintError(i + 1, originalLine, mnemonic + " requires a register operand"));
                } else if (!VALID_REGISTERS.contains(operandStr.toUpperCase())) {
                    errors.add(new LintError(i + 1, originalLine, "Invalid register '" + operandStr + "' for " + mnemonic));
                }
            } else if (SINGLE_PAIR_MNEMONICS.contains(mnemonic)) {
                if (operandStr.isEmpty()) {
                    errors.add(new LintError(i + 1, originalLine, mnemonic + " requires a register pair operand (B, D, H, SP)"));
                } else if (!REGISTER_PAIRS_LXI.contains(operandStr.toUpperCase())) {
                    errors.add(new LintError(i + 1, originalLine, "Invalid register pair '" + operandStr + "' for " + mnemonic));
                }
            } else if (mnemonic.equals("STAX") || mnemonic.equals("LDAX")) {
                if (operandStr.isEmpty()) {
                    errors.add(new LintError(i + 1, originalLine, mnemonic + " requires a register pair operand (B or D)"));
                } else if (!REGISTER_PAIRS_STAX_LDAX.contains(operandStr.toUpperCase())) {
                    errors.add(new LintError(i + 1, originalLine, "Invalid register pair '" + operandStr + "' for " + mnemonic + " (must be B or D)"));
                }
            } else if (mnemonic.equals("PUSH") || mnemonic.equals("POP")) {
                if (operandStr.isEmpty()) {
                    errors.add(new LintError(i + 1, originalLine, mnemonic + " requires a register pair operand (B, D, H, PSW)"));
                } else if (!REGISTER_PAIRS_PUSH_POP.contains(operandStr.toUpperCase())) {
                    errors.add(new LintError(i + 1, originalLine, "Invalid register pair '" + operandStr + "' for " + mnemonic + " (must be B, D, H, or PSW)"));
                }
            } else if (JUMP_CALL_MNEMONICS.contains(mnemonic)) {
                if (operandStr.isEmpty()) {
                    errors.add(new LintError(i + 1, originalLine, mnemonic + " requires a target label or address"));
                }
            } else if (IMMEDIATE_BYTE_MNEMONICS.contains(mnemonic) || IMMEDIATE_WORD_MNEMONICS.contains(mnemonic)) {
                if (operandStr.isEmpty()) {
                    errors.add(new LintError(i + 1, originalLine, mnemonic + " requires an operand"));
                }
            } else if (mnemonic.equals("RST")) {
                if (operandStr.isEmpty()) {
                    errors.add(new LintError(i + 1, originalLine, "RST requires vector number 0-7"));
                }
            } else {
                errors.add(new LintError(i + 1, originalLine, "Unknown 8085 mnemonic or directive '" + mnemonic + "'"));
            }
        }

        return errors;
    }
}
