import java.io.*;
import java.util.*;

/**
 * AssemblyToCConverter translates 8085 Assembly routines directly into 
 * simple, clean, human-readable C programs with standard C variables.
 */
public class AssemblyToCConverter {

    public static String convertToC(String asmCode) {
        if (asmCode == null || asmCode.trim().isEmpty()) {
            return "// No assembly code provided\n";
        }

        StringBuilder cCode = new StringBuilder();

        cCode.append("/*\n");
        cCode.append(" * C Source Code translated from 8085 Assembly\n");
        cCode.append(" */\n\n");
        cCode.append("#include <stdio.h>\n");
        cCode.append("#include <stdint.h>\n\n");

        cCode.append("int main() {\n");
        cCode.append("    // Variables matching 8085 registers & memory\n");
        cCode.append("    int a = 0, b = 0, c = 0, d = 0, e = 0;\n");
        cCode.append("    int h = 0, l = 0, hl = 0;\n");
        cCode.append("    int memory[65536] = {0};\n\n");

        String[] lines = asmCode.split("\r?\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                cCode.append("\n");
                continue;
            }

            // Extract inline comments
            String comment = "";
            int commentIdx = line.indexOf(';');
            if (commentIdx == -1) commentIdx = line.indexOf("//");

            if (commentIdx != -1) {
                comment = " // " + line.substring(commentIdx + 1).replace(";", "").replace("//", "").trim();
                line = line.substring(0, commentIdx).trim();
            }

            if (line.isEmpty()) {
                if (!comment.isEmpty()) {
                    cCode.append("   ").append(comment).append("\n");
                }
                continue;
            }

            // Handle labels (e.g. START:)
            int colonIdx = line.indexOf(':');
            if (colonIdx != -1 && !line.startsWith(".")) {
                String labelStr = line.substring(0, colonIdx).trim();
                cCode.append("  ").append(labelStr).append(":\n");
                line = line.substring(colonIdx + 1).trim();
                if (line.isEmpty()) {
                    continue;
                }
            }

            // Translate instruction line into clean C statement
            String translatedC = translateToSimpleC(line);
            if (!translatedC.isEmpty()) {
                cCode.append("    ").append(translatedC).append(comment).append("\n");
            } else {
                cCode.append("    // ").append(line).append(comment).append("\n");
            }
        }

        cCode.append("\n    printf(\"Result (A) = %d\\n\", a);\n");
        cCode.append("    return 0;\n");
        cCode.append("}\n");

        return cCode.toString();
    }

    private static String translateToSimpleC(String line) {
        String u = line.trim();
        String upper = u.toUpperCase();

        String mnemonic = upper;
        String operandsStr = "";
        int spaceIdx = upper.indexOf(' ');
        if (spaceIdx != -1) {
            mnemonic = upper.substring(0, spaceIdx).trim();
            operandsStr = u.substring(spaceIdx + 1).trim();
        }

        String[] ops = operandsStr.split(",");
        for (int i = 0; i < ops.length; i++) {
            ops[i] = ops[i].trim();
        }

        // Directives
        if (mnemonic.equals("ORG") || mnemonic.equals("EQU")) {
            return "// " + line;
        }

        // 1. Data Movement
        if (mnemonic.equals("MOV")) {
            if (ops.length == 2) {
                String dest = formatVar(ops[0]);
                String src = formatVar(ops[1]);
                return dest + " = " + src + ";";
            }
        }
        if (mnemonic.equals("MVI")) {
            if (ops.length == 2) {
                String dest = formatVar(ops[0]);
                String val = formatVal(ops[1]);
                return dest + " = " + val + ";";
            }
        }
        if (mnemonic.equals("LXI")) {
            if (ops.length == 2) {
                String pair = ops[0].toUpperCase();
                String val = formatVal(ops[1]);
                if (pair.equals("H")) return "hl = " + val + ";";
                if (pair.equals("B")) return "b = (" + val + " >> 8); c = (" + val + " & 0xFF);";
                if (pair.equals("D")) return "d = (" + val + " >> 8); e = (" + val + " & 0xFF);";
                if (pair.equals("SP")) return "// sp = " + val + ";";
            }
        }
        if (mnemonic.equals("LDA")) {
            return "a = memory[" + formatVal(operandsStr) + "];";
        }
        if (mnemonic.equals("STA")) {
            return "memory[" + formatVal(operandsStr) + "] = a;";
        }
        if (mnemonic.equals("LHLD")) {
            String addr = formatVal(operandsStr);
            return "l = memory[" + addr + "]; h = memory[" + addr + " + 1]; hl = (h << 8) | l;";
        }
        if (mnemonic.equals("SHLD")) {
            String addr = formatVal(operandsStr);
            return "memory[" + addr + "] = l; memory[" + addr + " + 1] = h;";
        }
        if (mnemonic.equals("LDAX")) {
            String pair = operandsStr.toUpperCase();
            if (pair.equals("B")) return "a = memory[(b << 8) | c];";
            if (pair.equals("D")) return "a = memory[(d << 8) | e];";
        }
        if (mnemonic.equals("STAX")) {
            String pair = operandsStr.toUpperCase();
            if (pair.equals("B")) return "memory[(b << 8) | c] = a;";
            if (pair.equals("D")) return "memory[(d << 8) | e] = a;";
        }
        if (mnemonic.equals("XCHG")) {
            return "{ int th = h, tl = l; h = d; l = e; d = th; e = tl; hl = (h << 8) | l; }";
        }

        // 2. Arithmetic
        if (mnemonic.equals("ADD")) {
            String src = formatVar(operandsStr);
            return "a = a + " + src + ";";
        }
        if (mnemonic.equals("ADI")) {
            String val = formatVal(operandsStr);
            return "a = a + " + val + ";";
        }
        if (mnemonic.equals("ADC") || mnemonic.equals("ACI")) {
            String src = formatVar(operandsStr);
            return "a = a + " + src + "; // (with carry)";
        }
        if (mnemonic.equals("SUB")) {
            String src = formatVar(operandsStr);
            return "a = a - " + src + ";";
        }
        if (mnemonic.equals("SUI")) {
            String val = formatVal(operandsStr);
            return "a = a - " + val + ";";
        }
        if (mnemonic.equals("SBB") || mnemonic.equals("SBI")) {
            String src = formatVar(operandsStr);
            return "a = a - " + src + "; // (with borrow)";
        }
        if (mnemonic.equals("INR")) {
            String target = formatVar(operandsStr);
            return target + "++;";
        }
        if (mnemonic.equals("DCR")) {
            String target = formatVar(operandsStr);
            return target + "--;";
        }
        if (mnemonic.equals("INX")) {
            String pair = operandsStr.toUpperCase();
            if (pair.equals("H")) return "hl++; h = hl >> 8; l = hl & 0xFF;";
            if (pair.equals("B")) return "c++; if(c > 255){ c = 0; b++; }";
            if (pair.equals("D")) return "e++; if(e > 255){ e = 0; d++; }";
        }
        if (mnemonic.equals("DCX")) {
            String pair = operandsStr.toUpperCase();
            if (pair.equals("H")) return "hl--; h = hl >> 8; l = hl & 0xFF;";
            if (pair.equals("B")) return "c--; if(c < 0){ c = 255; b--; }";
            if (pair.equals("D")) return "e--; if(e < 0){ e = 255; d--; }";
        }
        if (mnemonic.equals("DAD")) {
            String pair = operandsStr.toUpperCase();
            if (pair.equals("B")) return "hl = hl + ((b << 8) | c);";
            if (pair.equals("D")) return "hl = hl + ((d << 8) | e);";
            if (pair.equals("H")) return "hl = hl + hl;";
        }

        // 3. Logic
        if (mnemonic.equals("ANA")) {
            return "a = a & " + formatVar(operandsStr) + ";";
        }
        if (mnemonic.equals("ANI")) {
            return "a = a & " + formatVal(operandsStr) + ";";
        }
        if (mnemonic.equals("ORA")) {
            return "a = a | " + formatVar(operandsStr) + ";";
        }
        if (mnemonic.equals("ORI")) {
            return "a = a | " + formatVal(operandsStr) + ";";
        }
        if (mnemonic.equals("XRA")) {
            String src = operandsStr.toUpperCase();
            if (src.equals("A")) return "a = 0;";
            return "a = a ^ " + formatVar(operandsStr) + ";";
        }
        if (mnemonic.equals("XRI")) {
            return "a = a ^ " + formatVal(operandsStr) + ";";
        }
        if (mnemonic.equals("CMP") || mnemonic.equals("CPI")) {
            return "// compare a with " + formatVal(operandsStr);
        }
        if (mnemonic.equals("RLC")) {
            return "a = (a << 1) | (a >> 7);";
        }
        if (mnemonic.equals("RRC")) {
            return "a = (a >> 1) | ((a & 1) << 7);";
        }
        if (mnemonic.equals("CMA")) {
            return "a = ~a;";
        }

        // 4. Jumps & Control Flow
        if (mnemonic.equals("JMP")) {
            return "goto " + operandsStr + ";";
        }
        if (mnemonic.equals("JZ")) {
            return "if (a == 0) goto " + operandsStr + ";";
        }
        if (mnemonic.equals("JNZ")) {
            return "if (a != 0) goto " + operandsStr + ";";
        }
        if (mnemonic.equals("JC")) {
            return "if (a > 255) goto " + operandsStr + ";";
        }
        if (mnemonic.equals("JNC")) {
            return "if (a <= 255) goto " + operandsStr + ";";
        }
        if (mnemonic.equals("CALL")) {
            return operandsStr + "();";
        }
        if (mnemonic.equals("RET")) {
            return "return;";
        }

        // 5. I/O & System
        if (mnemonic.equals("IN")) {
            return "a = read_port(" + formatVal(operandsStr) + ");";
        }
        if (mnemonic.equals("OUT")) {
            return "write_port(" + formatVal(operandsStr) + ", a);";
        }
        if (mnemonic.equals("NOP")) {
            return ";";
        }
        if (mnemonic.equals("HLT")) {
            return "break;";
        }

        return "// " + line;
    }

    private static String formatVar(String v) {
        v = v.trim().toUpperCase();
        if (v.equals("M")) return "memory[hl]";
        if (v.equals("A")) return "a";
        if (v.equals("B")) return "b";
        if (v.equals("C")) return "c";
        if (v.equals("D")) return "d";
        if (v.equals("E")) return "e";
        if (v.equals("H")) return "h";
        if (v.equals("L")) return "l";
        return formatVal(v);
    }

    private static String formatVal(String val) {
        val = val.trim();
        if (val.startsWith("0x") || val.startsWith("0X")) {
            return val;
        }
        if (val.toUpperCase().endsWith("H")) {
            String hex = val.substring(0, val.length() - 1);
            try {
                int dec = Integer.parseInt(hex, 16);
                if (dec < 10) return String.valueOf(dec);
                return "0x" + hex;
            } catch (Exception e) {
                return "0x" + hex;
            }
        }
        try {
            int dec = Integer.parseInt(val);
            return String.valueOf(dec);
        } catch (Exception e) {
            return val;
        }
    }
}
