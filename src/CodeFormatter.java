import java.util.*;

public class CodeFormatter {

    public static String formatCode(String inputCode) {
        if (inputCode == null || inputCode.trim().isEmpty()) {
            return inputCode;
        }

        String[] lines = inputCode.split("\n", -1);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                sb.append("\n");
                continue;
            }

            // Preserve comment-only lines or directive lines starting with ';' '#' '/' '.'
            if (trimmed.startsWith(";") || trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith(".")) {
                sb.append(line).append("\n");
                continue;
            }

            String label = "";
            String mnemonic = "";
            String operands = "";
            String comment = "";

            String workLine = trimmed;

            // Extract comment
            int commentIdx = -1;
            for (int c = 0; c < workLine.length(); c++) {
                char ch = workLine.charAt(c);
                if (ch == ';' || ch == '#') {
                    commentIdx = c;
                    break;
                }
                if (ch == '/' && c + 1 < workLine.length() && workLine.charAt(c + 1) == '/') {
                    commentIdx = c;
                    break;
                }
            }

            if (commentIdx != -1) {
                comment = workLine.substring(commentIdx).trim();
                workLine = workLine.substring(0, commentIdx).trim();
            }

            if (workLine.isEmpty()) {
                sb.append(comment).append("\n");
                continue;
            }

            // Extract label (must end with colon ':')
            int colonIdx = workLine.indexOf(':');
            if (colonIdx != -1) {
                String lblRaw = workLine.substring(0, colonIdx).trim();
                label = lblRaw + ":"; // No space before colon!
                workLine = workLine.substring(colonIdx + 1).trim();
            }

            // Extract mnemonic and operands
            if (!workLine.isEmpty()) {
                String[] parts = workLine.split("\\s+", 2);
                mnemonic = parts[0].toUpperCase().trim();
                if (parts.length > 1) {
                    operands = parts[1].trim();
                    // Clean up spaces around commas: "A , 05H" -> "A, 05H"
                    operands = operands.replaceAll("\\s*,\\s*", ", ");
                }
            }

            // Build formatted line aligned into fixed columns (matching Image 1)
            StringBuilder lineSb = new StringBuilder();

            // Column 0-11: Label area (padded to 12 chars so mnemonic starts at col 12)
            if (!label.isEmpty()) {
                lineSb.append(label);
                while (lineSb.length() < 12) {
                    lineSb.append(" ");
                }
            } else {
                // 12 spaces if no label, aligning mnemonics vertically at col 12
                lineSb.append("            ");
            }

            // Column 12-19: Mnemonic area (padded to col 20 so operands start at col 20)
            if (!mnemonic.isEmpty()) {
                lineSb.append(mnemonic);
                if (!operands.isEmpty() || !comment.isEmpty()) {
                    while (lineSb.length() < 20) {
                        lineSb.append(" ");
                    }
                }
            }

            // Column 20-39: Operands area
            if (!operands.isEmpty()) {
                lineSb.append(operands);
            }

            // Column 40+: Comment area
            if (!comment.isEmpty()) {
                while (lineSb.length() < 40) {
                    lineSb.append(" ");
                }
                lineSb.append(comment);
            }

            sb.append(lineSb.toString().stripTrailing()).append("\n");
        }

        String result = sb.toString();
        if (!inputCode.endsWith("\n") && result.endsWith("\n")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }
}
