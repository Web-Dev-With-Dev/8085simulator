import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * BinaryHexManager handles Intel HEX (.hex) and Raw Binary (.bin)
 * Import/Export as well as Word Document (.docx, .doc) & Text Output / Input.
 */
public class BinaryHexManager {

    /**
     * Export memory as standard Intel HEX format (.hex)
     * Format: :LLAAAATTDD...CC
     */
    public static boolean exportIntelHex(Assembler asm, File file, int startAddr, int endAddr) {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            if (startAddr < 0) startAddr = 0;
            if (endAddr > 65535 || endAddr < startAddr) {
                endAddr = findMaxUsedAddress(asm);
            }
            if (endAddr < startAddr) endAddr = startAddr;

            int currentAddr = startAddr;
            while (currentAddr <= endAddr) {
                // Determine block size (up to 16 bytes per line)
                int bytesInLine = Math.min(16, endAddr - currentAddr + 1);
                
                // Check if all bytes in this 16-byte block are zero; skip large empty blocks if desired,
                // but for contiguous hex files we export active ranges.
                StringBuilder lineBuilder = new StringBuilder();
                lineBuilder.append(String.format("%02X", bytesInLine)); // LL
                lineBuilder.append(String.format("%04X", currentAddr)); // AAAA
                lineBuilder.append("00"); // TT = 00 Data record

                int sum = bytesInLine + (currentAddr >> 8) + (currentAddr & 0xFF) + 0;

                for (int i = 0; i < bytesInLine; i++) {
                    int val = asm.matrix.memory[currentAddr + i] & 0xFF;
                    lineBuilder.append(String.format("%02X", val));
                    sum += val;
                }

                int checksum = (-sum) & 0xFF;
                lineBuilder.append(String.format("%02X", checksum));

                writer.println(":" + lineBuilder.toString());
                currentAddr += bytesInLine;
            }

            // Write EOF Record
            writer.println(":00000001FF");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Import Intel HEX (.hex) file into simulator memory
     */
    public static boolean importIntelHex(Assembler asm, File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int loadedBytes = 0;
            int baseAddress = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith(":") || line.length() < 11) {
                    continue; // Skip non-hex lines or comments
                }

                int recordLen = Integer.parseInt(line.substring(1, 3), 16);
                int address = Integer.parseInt(line.substring(3, 7), 16) + baseAddress;
                int recordType = Integer.parseInt(line.substring(7, 9), 16);

                // Verify Checksum
                int sum = 0;
                for (int i = 1; i < line.length() - 2; i += 2) {
                    sum += Integer.parseInt(line.substring(i, i + 2), 16);
                }
                int expectedChecksum = Integer.parseInt(line.substring(line.length() - 2), 16);
                int calculatedChecksum = (-sum) & 0xFF;

                if (calculatedChecksum != expectedChecksum) {
                    System.err.println("Intel HEX Checksum Mismatch at line: " + line);
                    // We continue or warn, but process valid records
                }

                if (recordType == 0) { // Data Record
                    for (int i = 0; i < recordLen; i++) {
                        int byteVal = Integer.parseInt(line.substring(9 + (i * 2), 11 + (i * 2)), 16);
                        if (address + i < 65536) {
                            asm.matrix.memory[address + i] = byteVal;
                            loadedBytes++;
                        }
                    }
                } else if (recordType == 1) { // End of File
                    break;
                } else if (recordType == 2) { // Extended Segment Address Record
                    baseAddress = Integer.parseInt(line.substring(9, 13), 16) << 4;
                } else if (recordType == 4) { // Extended Linear Address Record
                    baseAddress = Integer.parseInt(line.substring(9, 13), 16) << 16;
                }
            }

            // Trigger disassembler to update assembly editor view
            if (asm.disassembler != null) {
                asm.disassembler.disasemble();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Export raw binary (.bin) file from simulator memory
     */
    public static boolean exportRawBinary(Assembler asm, File file, int startAddr, int endAddr) {
        if (startAddr < 0) startAddr = 0;
        if (endAddr > 65535 || endAddr < startAddr) {
            endAddr = findMaxUsedAddress(asm);
        }
        if (endAddr < startAddr) endAddr = startAddr;

        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (int i = startAddr; i <= endAddr; i++) {
                fos.write(asm.matrix.memory[i] & 0xFF);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Import raw binary (.bin) file into simulator memory starting at startAddr
     */
    public static boolean importRawBinary(Assembler asm, File file, int startAddr) {
        if (startAddr < 0) startAddr = 0;
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            int currentAddr = startAddr;

            while ((bytesRead = fis.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    if (currentAddr < 65536) {
                        asm.matrix.memory[currentAddr++] = buffer[i] & 0xFF;
                    }
                }
            }

            if (asm.disassembler != null) {
                asm.disassembler.disasemble();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Export Documentation as genuine Word Document (.docx, .doc) or Text (.txt)
     * Contains ONLY the pure assembly code text (no HTML, no headers).
     */
    public static boolean exportDocumentationDoc(Assembler asm, File file) {
        String fileName = file.getName().toLowerCase();
        String codeText = asm.jTextAreaAssemblyLanguageEditor.getText();

        if (fileName.endsWith(".docx")) {
            return writeDocxZip(file, codeText);
        } else if (fileName.endsWith(".doc") || fileName.endsWith(".rtf")) {
            return writeRtfDoc(file, codeText);
        } else {
            // Default plain text file (.txt)
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8)))) {
                writer.print(codeText);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    private static boolean writeDocxZip(File file, String codeText) {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
            // 1. Add [Content_Types].xml
            zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
            String contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
                "  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
                "  <Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>\n" +
                "</Types>";
            zos.write(contentTypes.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 2. Add _rels/.rels
            zos.putNextEntry(new ZipEntry("_rels/.rels"));
            String rels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>\n" +
                "</Relationships>";
            zos.write(rels.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 3. Add word/document.xml
            zos.putNextEntry(new ZipEntry("word/document.xml"));
            StringBuilder docXml = new StringBuilder();
            docXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            docXml.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n");
            docXml.append("  <w:body>\n");

            String[] lines = codeText.split("\r?\n");
            for (String line : lines) {
                String escapedLine = escapeXml(line);
                docXml.append("    <w:p>\n");
                docXml.append("      <w:r>\n");
                docXml.append("        <w:rPr><w:rFonts w:ascii=\"Consolas\" w:hAnsi=\"Consolas\"/><w:sz w:val=\"22\"/></w:rPr>\n");
                docXml.append("        <w:t xml:space=\"preserve\">").append(escapedLine).append("</w:t>\n");
                docXml.append("      </w:r>\n");
                docXml.append("    </w:p>\n");
            }

            docXml.append("  </w:body>\n");
            docXml.append("</w:document>");
            zos.write(docXml.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean writeRtfDoc(File file, String codeText) {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8)))) {
            writer.println("{\\rtf1\\ansi\\deff0{\\fonttbl{\\f0\\fnil\\fcharset0 Consolas;}}");
            writer.println("\\f0\\fs22 ");
            String[] lines = codeText.split("\r?\n");
            for (String line : lines) {
                String escaped = line.replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}");
                writer.print(escaped + "\\par\n");
            }
            writer.println("}");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    /**
     * Import Documentation (.docx, .doc, .txt) and load assembly code
     */
    public static boolean importDocumentation(Assembler asm, File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".docx")) {
            return readDocxZip(asm, file);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("{\\rtf") || line.trim().startsWith("\\f0") || line.trim().startsWith("}")) {
                    continue;
                }
                sb.append(line).append("\n");
            }
            asm.jTextAreaAssemblyLanguageEditor.setText(sb.toString());
            if (asm.textEditor != null) {
                asm.textEditor.colorEditor();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean readDocxZip(Assembler asm, File file) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry;
            StringBuilder code = new StringBuilder();
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("word/document.xml")) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Matcher m = Pattern.compile("<w:t[^>]*>(.*?)</w:t>").matcher(line);
                        while (m.find()) {
                            code.append(unescapeXml(m.group(1)));
                        }
                        if (line.contains("</w:p>")) {
                            code.append("\n");
                        }
                    }
                }
            }
            asm.jTextAreaAssemblyLanguageEditor.setText(code.toString());
            if (asm.textEditor != null) {
                asm.textEditor.colorEditor();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String unescapeXml(String text) {
        if (text == null) return "";
        return text.replace("&amp;", "&")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&quot;", "\"")
                   .replace("&apos;", "'");
    }

    // Helper functions
    private static int findMinUsedAddress(Assembler asm) {
        for (int i = 0; i < 65536; i++) {
            if (asm.matrix.memory[i] != 0 || (asm.matrix.label[i] != null && !asm.matrix.label[i].isEmpty())) {
                return i;
            }
        }
        return 0;
    }

    private static int findMaxUsedAddress(Assembler asm) {
        int max = 0;
        for (int i = 65535; i >= 0; i--) {
            if (asm.matrix.memory[i] != 0 || (asm.matrix.label[i] != null && !asm.matrix.label[i].isEmpty())) {
                return i;
            }
        }
        return max;
    }

    private static boolean isMemoryEmptyFrom(Assembler asm, int from, int to) {
        for (int i = from; i <= to; i++) {
            if (asm.matrix.memory[i] != 0) return false;
        }
        return true;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private static boolean isProbable8085Assembly(String line) {
        String u = line.toUpperCase();
        return u.contains("MVI ") || u.contains("LXI ") || u.contains("MOV ") || u.contains("ADD ") ||
               u.contains("SUB ") || u.contains("INR ") || u.contains("DCR ") || u.contains("STA ") ||
               u.contains("LDA ") || u.contains("JMP ") || u.contains("CALL ") || u.contains("RET ") ||
               u.contains("HLT") || u.contains("NOP") || u.contains("IN ") || u.contains("OUT ") ||
               u.contains("ORG ") || u.contains("CPI ");
    }

    private static String clean8085Line(String line) {
        // Strip address prefixes like "0x2000" or line numbers if present
        line = line.replaceAll("(?i)^(0x[0-9a-f]{4}|[0-9a-f]{4}h?|[0-9]+[:\\s]+)", "").trim();
        return line;
    }
}
