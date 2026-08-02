
import java.io.*;

public class Matrix {
    int beginAddress=0,stopAddress=65535;
    int A,B,C,D,E,F,H,L,SP,PC=0;
    int D1;
    int SOD,SDE,SID,R75,R65,R55,MSE,RR75,M75,M65,M55,IE,INTR,_INTA,TRAP,HOLD,_HOLDA,_RESETIN=1,RESETOUT,IO_M,_RD,_WR;
    long  clockCycleCounter,instructionCounter;
    int temp=0;
    int memory[];
    int port[];
    int select=0;
    String[] label;
    String[][] preLabel;
    AssemblerEngine engine;
    Assembler o;
    public Matrix(Assembler o) {
        this.o=o;
        memory=new int[65536];
        port=new int[256];
        label=new String[65536];
        preLabel=new String[500][2];
        engine=new AssemblerEngine(this);
        for(int i=0;i<65536;i++){label[i]="";}
    }

     public String comment(int i)
    {
        switch(i)
        {
            case 0:  return "[NOP]: No operation performed. PC advances.";
            case 1:  return "[LXI B]: Loaded 16-bit value (" + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H) into BC pair. B=" + engine.Dec2Hex2digit(memory[(PC + 2) & 0xFFFF]) + "H, C=" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H.";
            case 2:  return "[STAX B]: Copied Accumulator A (" + engine.Dec2Hex2digit(A) + "H) to memory address " + engine.Dec2Hex((B << 8) | C) + "H (pointed by BC).";
            case 3:  return "[INX B]: Incremented BC register pair by 1. New BC = " + engine.Dec2Hex((B << 8) | C) + "H.";
            case 4:  return "[INR B]: Incremented Register B by 1. New B = " + engine.Dec2Hex2digit(B) + "H.";
            case 5:  return "[DCR B]: Decremented Register B by 1. New B = " + engine.Dec2Hex2digit(B) + "H.";
            case 6:  return "[MVI B]: Loaded immediate byte (" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H) into Register B.";
            case 7:  return "[RLC]: Rotated Accumulator A left by 1 bit. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 9:  return "[DAD B]: Added BC pair to HL pair. New HL = " + engine.Dec2Hex((H << 8) | L) + "H.";
            case 10: return "[LDAX B]: Loaded Accumulator A from memory address " + engine.Dec2Hex((B << 8) | C) + "H (pointed by BC). New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 11: return "[DCX B]: Decremented BC register pair by 1. New BC = " + engine.Dec2Hex((B << 8) | C) + "H.";
            case 12: return "[INR C]: Incremented Register C by 1. New C = " + engine.Dec2Hex2digit(C) + "H.";
            case 13: return "[DCR C]: Decremented Register C by 1. New C = " + engine.Dec2Hex2digit(C) + "H.";
            case 14: return "[MVI C]: Loaded immediate byte (" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H) into Register C.";
            case 15: return "[RRC]: Rotated Accumulator A right by 1 bit. New A = " + engine.Dec2Hex2digit(A) + "H.";

            case 17: return "[LXI D]: Loaded 16-bit value (" + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H) into DE pair. D=" + engine.Dec2Hex2digit(memory[(PC + 2) & 0xFFFF]) + "H, E=" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H.";
            case 18: return "[STAX D]: Copied Accumulator A (" + engine.Dec2Hex2digit(A) + "H) to memory address " + engine.Dec2Hex((D << 8) | E) + "H (pointed by DE).";
            case 19: return "[INX D]: Incremented DE register pair by 1. New DE = " + engine.Dec2Hex((D << 8) | E) + "H.";
            case 20: return "[INR D]: Incremented Register D by 1. New D = " + engine.Dec2Hex2digit(D) + "H.";
            case 21: return "[DCR D]: Decremented Register D by 1. New D = " + engine.Dec2Hex2digit(D) + "H.";
            case 22: return "[MVI D]: Loaded immediate byte (" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H) into Register D.";
            case 23: return "[RAL]: Rotated Accumulator A left through Carry. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 25: return "[DAD D]: Added DE pair to HL pair. New HL = " + engine.Dec2Hex((H << 8) | L) + "H.";
            case 26: return "[LDAX D]: Loaded Accumulator A from memory address " + engine.Dec2Hex((D << 8) | E) + "H (pointed by DE). New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 27: return "[DCX D]: Decremented DE register pair by 1. New DE = " + engine.Dec2Hex((D << 8) | E) + "H.";
            case 28: return "[INR E]: Incremented Register E by 1. New E = " + engine.Dec2Hex2digit(E) + "H.";
            case 29: return "[DCR E]: Decremented Register E by 1. New E = " + engine.Dec2Hex2digit(E) + "H.";
            case 30: return "[MVI E]: Loaded immediate byte (" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H) into Register E.";
            case 31: return "[RAR]: Rotated Accumulator A right through Carry. New A = " + engine.Dec2Hex2digit(A) + "H.";

            case 33: return "[LXI H]: Loaded 16-bit address (" + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H) into HL pair. H=" + engine.Dec2Hex2digit(memory[(PC + 2) & 0xFFFF]) + "H, L=" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H. Pointer HL = " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H.";
            case 34: return "[SHLD]: Stored L and H into memory locations " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H and " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF] + 1) + "H.";
            case 35: return "[INX H]: Incremented HL register pair by 1. New HL = " + engine.Dec2Hex((H << 8) | L) + "H.";
            case 36: return "[INR H]: Incremented Register H by 1. New H = " + engine.Dec2Hex2digit(H) + "H.";
            case 37: return "[DCR H]: Decremented Register H by 1. New H = " + engine.Dec2Hex2digit(H) + "H.";
            case 38: return "[MVI H]: Loaded immediate byte (" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H) into Register H.";
            case 39: return "[DAA]: Decimal Adjust Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 41: return "[DAD H]: Added HL pair to HL pair (Doubled HL). New HL = " + engine.Dec2Hex((H << 8) | L) + "H.";
            case 42: return "[LHLD]: Loaded L and H from memory locations " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H and " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF] + 1) + "H. New HL = " + engine.Dec2Hex((H << 8) | L) + "H.";
            case 43: return "[DCX H]: Decremented HL register pair by 1. New HL = " + engine.Dec2Hex((H << 8) | L) + "H.";
            case 44: return "[INR L]: Incremented Register L by 1. New L = " + engine.Dec2Hex2digit(L) + "H.";
            case 45: return "[DCR L]: Decremented Register L by 1. New L = " + engine.Dec2Hex2digit(L) + "H.";
            case 46: return "[MVI L]: Loaded immediate byte (" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H) into Register L.";
            case 47: return "[CMA]: Complemented Accumulator A (Bitwise NOT). New A = " + engine.Dec2Hex2digit(A) + "H.";

            case 49: return "[LXI SP]: Loaded Stack Pointer SP with 16-bit address " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H.";
            case 50: return "[STA]: Stored Accumulator A (" + engine.Dec2Hex2digit(A) + "H) into memory location " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H.";
            case 52: return "[INR M]: Incremented memory byte at location [" + engine.Dec2Hex((H << 8) | L) + "H] by 1. New value = " + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H.";
            case 53: return "[DCR M]: Decremented memory byte at location [" + engine.Dec2Hex((H << 8) | L) + "H] by 1. New value = " + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H.";
            case 54: return "[MVI M]: Loaded immediate byte (" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H) into memory location [" + engine.Dec2Hex((H << 8) | L) + "H].";
            case 55: return "[STC]: Set Carry Flag CY = 1.";
            case 57: return "[DAD SP]: Added Stack Pointer SP to HL pair. New HL = " + engine.Dec2Hex((H << 8) | L) + "H.";
            case 58: return "[LDA]: Loaded byte from memory location " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H into Accumulator A (" + engine.Dec2Hex2digit(A) + "H).";
            case 60: return "[INR A]: Incremented Accumulator A by 1. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 61: return "[DCR A]: Decremented Accumulator A by 1. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 62: return "[MVI A]: Loaded immediate byte (" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H) into Accumulator A.";
            case 63: return "[CMC]: Complemented Carry Flag CY.";

            case 70: return "[MOV B, M]: Copied data byte (" + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H) from memory location [" + engine.Dec2Hex((H << 8) | L) + "H] into Register B.";
            case 78: return "[MOV C, M]: Copied data byte (" + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H) from memory location [" + engine.Dec2Hex((H << 8) | L) + "H] into Register C.";
            case 86: return "[MOV D, M]: Copied data byte (" + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H) from memory location [" + engine.Dec2Hex((H << 8) | L) + "H] into Register D.";
            case 94: return "[MOV E, M]: Copied data byte (" + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H) from memory location [" + engine.Dec2Hex((H << 8) | L) + "H] into Register E.";
            case 102: return "[MOV H, M]: Copied data byte (" + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H) from memory location [" + engine.Dec2Hex((H << 8) | L) + "H] into Register H.";
            case 110: return "[MOV L, M]: Copied data byte (" + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H) from memory location [" + engine.Dec2Hex((H << 8) | L) + "H] into Register L.";
            case 118: return "[HLT]: Program execution halted.";
            case 126: return "[MOV A, M]: Copied data byte (" + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H) from memory location [" + engine.Dec2Hex((H << 8) | L) + "H] into Accumulator A.";

            case 112: return "[MOV M, B]: Copied Register B (" + engine.Dec2Hex2digit(B) + "H) into memory location [" + engine.Dec2Hex((H << 8) | L) + "H].";
            case 113: return "[MOV M, C]: Copied Register C (" + engine.Dec2Hex2digit(C) + "H) into memory location [" + engine.Dec2Hex((H << 8) | L) + "H].";
            case 114: return "[MOV M, D]: Copied Register D (" + engine.Dec2Hex2digit(D) + "H) into memory location [" + engine.Dec2Hex((H << 8) | L) + "H].";
            case 115: return "[MOV M, E]: Copied Register E (" + engine.Dec2Hex2digit(E) + "H) into memory location [" + engine.Dec2Hex((H << 8) | L) + "H].";
            case 116: return "[MOV M, H]: Copied Register H (" + engine.Dec2Hex2digit(H) + "H) into memory location [" + engine.Dec2Hex((H << 8) | L) + "H].";
            case 117: return "[MOV M, L]: Copied Register L (" + engine.Dec2Hex2digit(L) + "H) into memory location [" + engine.Dec2Hex((H << 8) | L) + "H].";
            case 119: return "[MOV M, A]: Copied Accumulator A (" + engine.Dec2Hex2digit(A) + "H) into memory location [" + engine.Dec2Hex((H << 8) | L) + "H].";

            case 134: return "[ADD M]: Added memory byte (" + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H) from [" + engine.Dec2Hex((H << 8) | L) + "H] to Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 142: return "[ADC M]: Added memory byte (" + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H) and Carry to Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 150: return "[SUB M]: Subtracted memory byte (" + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H) from Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 158: return "[SBB M]: Subtracted memory byte (" + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H) and Borrow from Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 166: return "[ANA M]: Bitwise AND memory byte (" + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H) with Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 174: return "[XRA M]: Bitwise XOR memory byte (" + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H) with Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 182: return "[ORA M]: Bitwise OR memory byte (" + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H) with Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 190: return "[CMP M]: Compared memory byte (" + engine.Dec2Hex2digit(memory[(H << 8) | L]) + "H) with Accumulator A (" + engine.Dec2Hex2digit(A) + "H).";

            case 194: return "[JNZ]: Jump to address " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H because Zero flag Z = 0.";
            case 195: return "[JMP]: Jumped unconditionally to address " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H.";
            case 198: return "[ADI]: Added immediate byte (" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H) to Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 202: return "[JZ]: Jump to address " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H because Zero flag Z = 1.";
            case 205: return "[CALL]: Called subroutine at address " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H.";
            case 206: return "[ACI]: Added immediate byte (" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H) and Carry to Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 210: return "[JNC]: Jump to address " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H because Carry flag CY = 0.";
            case 218: return "[JC]: Jump to address " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H because Carry flag CY = 1.";
            case 222: return "[SUI]: Subtracted immediate byte (" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H) from Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 226: return "[JPO]: Jump to address " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H because Parity is Odd.";
            case 230: return "[ANI]: Bitwise AND immediate byte (" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H) with Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 234: return "[JPE]: Jump to address " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H because Parity is Even.";
            case 235: return "[XCHG]: Swapped HL and DE register pairs. New HL = " + engine.Dec2Hex((H << 8) | L) + "H, DE = " + engine.Dec2Hex((D << 8) | E) + "H.";
            case 238: return "[XRI]: Bitwise XOR immediate byte (" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H) with Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 242: return "[JP]: Jump to address " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H because Sign flag S = 0 (Positive).";
            case 246: return "[ORI]: Bitwise OR immediate byte (" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H) with Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
            case 249: return "[SPHL]: Set Stack Pointer SP = HL (" + engine.Dec2Hex((H << 8) | L) + "H).";
            case 250: return "[JM]: Jump to address " + engine.Dec2Hex(256 * memory[(PC + 2) & 0xFFFF] + memory[(PC + 1) & 0xFFFF]) + "H because Sign flag S = 1 (Negative).";
            case 254: return "[CPI]: Compared Accumulator A (" + engine.Dec2Hex2digit(A) + "H) with immediate byte (" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H).";

            case 219: return "[IN]: Read input byte (" + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H) from Port " + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H into Accumulator A.";
            case 211: return "[OUT]: Output Accumulator A (" + engine.Dec2Hex2digit(A) + "H) to Port " + engine.Dec2Hex2digit(memory[(PC + 1) & 0xFFFF]) + "H.";
            case 201: return "[RET]: Returned from subroutine.";
        }

        // Fallback generic explanation for register-register MOV, ADD, SUB, etc.
        if (i >= 64 && i <= 127) {
            String[] regs = {"B", "C", "D", "E", "H", "L", "M", "A"};
            String dst = regs[(i - 64) / 8];
            String src = regs[(i - 64) % 8];
            return "[MOV " + dst + ", " + src + "]: Copied content from " + src + " to " + dst + ".";
        }
        if (i >= 128 && i <= 135) {
            String[] regs = {"B", "C", "D", "E", "H", "L", "M", "A"};
            return "[ADD " + regs[i - 128] + "]: Added Register " + regs[i - 128] + " to Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
        }
        if (i >= 144 && i <= 151) {
            String[] regs = {"B", "C", "D", "E", "H", "L", "M", "A"};
            return "[SUB " + regs[i - 144] + "]: Subtracted Register " + regs[i - 144] + " from Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
        }
        if (i >= 160 && i <= 167) {
            String[] regs = {"B", "C", "D", "E", "H", "L", "M", "A"};
            return "[ANA " + regs[i - 160] + "]: Bitwise AND Register " + regs[i - 160] + " with Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
        }
        if (i >= 168 && i <= 175) {
            String[] regs = {"B", "C", "D", "E", "H", "L", "M", "A"};
            return "[XRA " + regs[i - 168] + "]: Bitwise XOR Register " + regs[i - 168] + " with Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
        }
        if (i >= 176 && i <= 183) {
            String[] regs = {"B", "C", "D", "E", "H", "L", "M", "A"};
            return "[ORA " + regs[i - 176] + "]: Bitwise OR Register " + regs[i - 176] + " with Accumulator A. New A = " + engine.Dec2Hex2digit(A) + "H.";
        }
        if (i >= 184 && i <= 191) {
            String[] regs = {"B", "C", "D", "E", "H", "L", "M", "A"};
            return "[CMP " + regs[i - 184] + "]: Compared Register " + regs[i - 184] + " with Accumulator A (" + engine.Dec2Hex2digit(A) + "H).";
        }

        return "Executed opcode " + engine.Dec2Hex2digit(i) + "H at PC = " + engine.Dec2Hex(PC) + "H.";
    }

    public String tstates(int n) {
        switch (n) {
            case 0:
                return "F";
            case 1:
                return "FRR";
            case 2:
                return "FW";
            case 3:
                return "S";
            case 4:
                return "F";
            case 5:
                return "F";
            case 6:
                return "FR";
            case 7:
                return "F";
            case 8:
                return "F";
            case 9:
                return "FBB";
            case 10:
                return "FR";
            case 11:
                return "S";
            case 12:
                return "F";
            case 13:
                return "F";
            case 14:
                return "FR";
            case 15:
                return "F";
            case 16:
                return "F";
            case 17:
                return "FRR";
            case 18:
                return "FW";
            case 19:
                return "S";
            case 20:
                return "F";
            case 21:
                return "F";
            case 22:
                return "FR";
            case 23:
                return "F";
            case 24:
                return "F";
            case 25:
                return "FBB";
            case 26:
                return "FR";
            case 27:
                return "S";
            case 28:
                return "F";
            case 29:
                return "F";
            case 30:
                return "FR";
            case 31:
                return "F";
            case 32:
                return "F";
            case 33:
                return "FRR";
            case 34:
                return "FRRWW";
            case 35:
                return "S";
            case 36:
                return "F";
            case 37:
                return "F";
            case 38:
                return "FR";
            case 39:
                return "F";
            case 40:
                return "F";
            case 41:
                return "FBB";
            case 42:
                return "FRRRR";
            case 43:
                return "S";
            case 44:
                return "F";
            case 45:
                return "F";
            case 46:
                return "FR";
            case 47:
                return "F";
            case 48:
                return "F";
            case 49:
                return "FRR";
            case 50:
                return "FRRW";
            case 51:
                return "S";
            case 52:
                return "FRW";
            case 53:
                return "FRW";
            case 54:
                return "FRW";
            case 55:
                return "F";
            case 56:
                return "F";
            case 57:
                return "FBB";
            case 58:
                return "FRRR";
            case 59:
                return "S";
            case 60:
                return "F";
            case 61:
                return "F";
            case 62:
                return "FR";
            case 63:
                return "F";
            case 64:
                return "F";
            case 65:
                return "F";
            case 66:
                return "F";
            case 67:
                return "F";
            case 68:
                return "F";
            case 69:
                return "F";
            case 70:
                return "FR";
            case 71:
                return "F";
            case 72:
                return "F";
            case 73:
                return "F";
            case 74:
                return "F";
            case 75:
                return "F";
            case 76:
                return "F";
            case 77:
                return "F";
            case 78:
                return "FR";
            case 79:
                return "F";
            case 80:
                return "F";
            case 81:
                return "F";
            case 82:
                return "F";
            case 83:
                return "F";
            case 84:
                return "F";
            case 85:
                return "F";
            case 86:
                return "FR";
            case 87:
                return "F";
            case 88:
                return "F";
            case 89:
                return "F";
            case 90:
                return "F";
            case 91:
                return "F";
            case 92:
                return "F";
            case 93:
                return "F";
            case 94:
                return "FR";
            case 95:
                return "F";
            case 96:
                return "F";
            case 97:
                return "F";
            case 98:
                return "F";
            case 99:
                return "F";
            case 100:
                return "F";
            case 101:
                return "F";
            case 102:
                return "FR";
            case 103:
                return "F";
            case 104:
                return "F";
            case 105:
                return "F";
            case 106:
                return "F";
            case 107:
                return "F";
            case 108:
                return "F";
            case 109:
                return "F";
            case 110:
                return "FR";
            case 111:
                return "F";
            case 112:
                return "FW";
            case 113:
                return "FW";
            case 114:
                return "FW";
            case 115:
                return "FW";
            case 116:
                return "FW";
            case 117:
                return "FW";
            case 118:
                return "FB";
            case 119:
                return "FW";
            case 120:
                return "F";
            case 121:
                return "F";
            case 122:
                return "F";
            case 123:
                return "F";
            case 124:
                return "F";
            case 125:
                return "F";
            case 126:
                return "FR";
            case 127:
                return "F";
            case 128:
                return "F";
            case 129:
                return "F";
            case 130:
                return "F";
            case 131:
                return "F";
            case 132:
                return "F";
            case 133:
                return "F";
            case 134:
                return "FR";
            case 135:
                return "F";
            case 136:
                return "F";
            case 137:
                return "F";
            case 138:
                return "F";
            case 139:
                return "F";
            case 140:
                return "F";
            case 141:
                return "F";
            case 142:
                return "FR";
            case 143:
                return "F";
            case 144:
                return "F";
            case 145:
                return "F";
            case 146:
                return "F";
            case 147:
                return "F";
            case 148:
                return "F";
            case 149:
                return "F";
            case 150:
                return "FR";
            case 151:
                return "F";
            case 152:
                return "F";
            case 153:
                return "F";
            case 154:
                return "F";
            case 155:
                return "F";
            case 156:
                return "F";
            case 157:
                return "F";
            case 158:
                return "FR";
            case 159:
                return "F";
            case 160:
                return "F";
            case 161:
                return "F";
            case 162:
                return "F";
            case 163:
                return "F";
            case 164:
                return "F";
            case 165:
                return "F";
            case 166:
                return "FR";
            case 167:
                return "F";
            case 168:
                return "F";
            case 169:
                return "F";
            case 170:
                return "F";
            case 171:
                return "F";
            case 172:
                return "F";
            case 173:
                return "F";
            case 174:
                return "FR";
            case 175:
                return "F";
            case 176:
                return "F";
            case 177:
                return "F";
            case 178:
                return "F";
            case 179:
                return "F";
            case 180:
                return "F";
            case 181:
                return "F";
            case 182:
                return "FR";
            case 183:
                return "F";
            case 184:
                return "F";
            case 185:
                return "F";
            case 186:
                return "F";
            case 187:
                return "F";
            case 188:
                return "F";
            case 189:
                return "F";
            case 190:
                return "FR";
            case 191:
                return "F";
            case 192:
                return "SRR";
            case 193:
                return "FRR";
            case 194:
                return "FRR";
            case 195:
                return "FRR";
            case 196:
                return "SRRWW";
            case 197:
                return "SWW";
            case 198:
                return "FR";
            case 199:
                return "SWW";
            case 200:
                return "SRR";
            case 201:
                return "SRR";
            case 202:
                return "FRR";
            case 203:
                return "F";
            case 204:
                return "SRRWW";
            case 205:
                return "SRRWW";
            case 206:
                return "FR";
            case 207:
                return "SWW";
            case 208:
                return "SRR";
            case 209:
                return "FRR";
            case 210:
                return "FRR";
            case 211:
                return "FRO";
            case 212:
                return "SRRWW";
            case 213:
                return "SWW";
            case 214:
                return "FR";
            case 215:
                return "SWW";
            case 216:
                return "SRR";
            case 217:
                return "F";
            case 218:
                return "FRR";
            case 219:
                return "FRI";
            case 220:
                return "SRRWW";
            case 221:
                return "F";
            case 222:
                return "FR";
            case 223:
                return "SWW";
            case 224:
                return "SRR";
            case 225:
                return "FRR";
            case 226:
                return "FRR";
            case 227:
                return "FRRWW";
            case 228:
                return "SRRWW";
            case 229:
                return "SWW";
            case 230:
                return "FR";
            case 231:
                return "SWW";
            case 232:
                return "SRR";
            case 233:
                return "S";
            case 234:
                return "FRR";
            case 235:
                return "F";
            case 236:
                return "SRRWW";
            case 237:
                return "F";
            case 238:
                return "FR";
            case 239:
                return "SWW";
            case 240:
                return "SRR";
            case 241:
                return "FRR";
            case 242:
                return "FRR";
            case 243:
                return "F";
            case 244:
                return "SRRWW";
            case 245:
                return "SWW";
            case 246:
                return "FR";
            case 247:
                return "SWW";
            case 248:
                return "SRR";
            case 249:
                return "S";
            case 250:
                return "FRR";
            case 251:
                return "F";
            case 252:
                return "SRRWW";
            case 253:
                return "F";
            case 254:
                return "FR";
            case 255:
                return "SWW";
            default: return "0";
        }
    }


    public static class ExecutionState {
        int A, B, C, D, E, F, H, L, SP, PC, D1;
        int SOD, SDE, SID, R75, R65, R55, MSE, RR75, M75, M65, M55, IE, INTR, _INTA, TRAP, HOLD, _HOLDA, _RESETIN, RESETOUT, IO_M, _RD, _WR;
        long clockCycleCounter, instructionCounter;
        int select;
        int beginAddress, stopAddress;
        int[] memory;
        int[] port;
        String[] label;

        public ExecutionState(Matrix x) {
            this.A = x.A; this.F = x.F;
            this.B = x.B; this.C = x.C;
            this.D = x.D; this.E = x.E;
            this.H = x.H; this.L = x.L;
            this.SP = x.SP; this.PC = x.PC;
            this.D1 = x.D1;
            this.clockCycleCounter = x.clockCycleCounter;
            this.instructionCounter = x.instructionCounter;
            this.SOD = x.SOD; this.SDE = x.SDE; this.SID = x.SID;
            this.R75 = x.R75; this.R65 = x.R65; this.R55 = x.R55;
            this.MSE = x.MSE; this.RR75 = x.RR75; this.M75 = x.M75;
            this.M65 = x.M65; this.M55 = x.M55; this.IE = x.IE;
            this.INTR = x.INTR; this._INTA = x._INTA; this.TRAP = x.TRAP;
            this.HOLD = x.HOLD; this._HOLDA = x._HOLDA; this._RESETIN = x._RESETIN;
            this.RESETOUT = x.RESETOUT; this.IO_M = x.IO_M; this._RD = x._RD; this._WR = x._WR;
            this.beginAddress = x.beginAddress;
            this.stopAddress = x.stopAddress;
            this.select = x.select;

            this.memory = new int[65536];
            System.arraycopy(x.memory, 0, this.memory, 0, 65536);
            this.port = new int[256];
            System.arraycopy(x.port, 0, this.port, 0, 256);
            this.label = new String[65536];
            System.arraycopy(x.label, 0, this.label, 0, 65536);
        }

        public void restoreTo(Matrix x) {
            x.A = this.A; x.F = this.F;
            x.B = this.B; x.C = this.C;
            x.D = this.D; x.E = this.E;
            x.H = this.H; x.L = this.L;
            x.SP = this.SP; x.PC = this.PC;
            x.D1 = this.D1;
            x.clockCycleCounter = this.clockCycleCounter;
            x.instructionCounter = this.instructionCounter;
            x.SOD = this.SOD; x.SDE = this.SDE; x.SID = this.SID;
            x.R75 = this.R75; x.R65 = this.R65; x.R55 = this.R55;
            x.MSE = this.MSE; x.RR75 = this.RR75; x.M75 = this.M75;
            x.M65 = this.M65; x.M55 = this.M55; x.IE = this.IE;
            x.INTR = this.INTR; x._INTA = this._INTA; x.TRAP = this.TRAP;
            x.HOLD = this.HOLD; x._HOLDA = this._HOLDA; x._RESETIN = this._RESETIN;
            x.RESETOUT = this.RESETOUT; x.IO_M = this.IO_M; x._RD = this._RD; x._WR = this._WR;
            x.beginAddress = this.beginAddress;
            x.stopAddress = this.stopAddress;
            x.select = this.select;

            System.arraycopy(this.memory, 0, x.memory, 0, 65536);
            System.arraycopy(this.port, 0, x.port, 0, 256);
            System.arraycopy(this.label, 0, x.label, 0, 65536);
        }
    }

    private final java.util.Stack<ExecutionState> historyStack = new java.util.Stack<>();
    public static final int MAX_HISTORY_DEPTH = 5000;

    public void createCopy(Matrix x) {
        if (historyStack.size() >= MAX_HISTORY_DEPTH) {
            historyStack.remove(0);
        }
        historyStack.push(new ExecutionState(x));
    }

    public void readCopy(Matrix x) {
        if (!historyStack.isEmpty()) {
            ExecutionState state = historyStack.pop();
            state.restoreTo(x);
        } else {
            if (o != null && o.jButtonBackward != null) {
                o.jButtonBackward.setEnabled(false);
            }
        }
    }

    public void clearHistory() {
        historyStack.clear();
    }

    public int getHistorySize() {
        return historyStack.size();
    }
    public void functionRun(int index)
    {
        int no[];
        PC++;
        MemoryHeatmapVisualizer.recordExec(PC - 1, 1);
        if(o.stopAtIndex==index)o.stop=true;
        switch(index)
        {
            case 0://NOP
                    break;

            case 1://LXI B
                   C=memory[PC++];
                   B=memory[PC++];
                   MemoryHeatmapVisualizer.recordRead(PC - 2);
                   MemoryHeatmapVisualizer.recordRead(PC - 1);
                   break;
            case 2://STAX B
                    int targetAddr2 = (B << 8) + C;
                    memory[targetAddr2] = A;
                    MemoryHeatmapVisualizer.recordWrite(targetAddr2);
                    break;
            case 3://INX B
                   temp=B<<8 | C;
                   temp++;
                   B=(temp>>8)&0xff; C=temp&0x00ff;
                    break;
            case 4://INR B
                   F=getFlagADD(B,0x01)&0xFE | F&0x01;
                   B=(B+1)&0xff;
                    break;
            case 5:F=getFlagADD(B,0xFF)&0xFE | F&0x01; //2's complement and carry falg not affected
                    B=(B-1)&0xff;                                      
                    break;
            case 6:B=memory[PC++];
                    break;
            case 7:temp=A/128;A=(A%128)*2+temp;
                   F=(F&254)^temp;
                    break;
            case 8:
                    break;
            case 9:H=H+B;L=L+C;
                   if(L>255){H=H+L/256;L=L%256;}
                   if(H>255){F=(F&254)^1;H=H%256;}
                    break;
            case 10:int addrLdaxB = 256 * B + C;
                    A = memory[addrLdaxB];
                    MemoryHeatmapVisualizer.recordRead(addrLdaxB);
                    break;
            case 11:C--;
                    if(((B*256)+C)<0){B=255;C=255;}
                    if(C<0){C=255;B--;}
                   break;
            case 12:
                   F=getFlagADD(C,0x01)&0xFE | F&0x01;
                    C=(C+1)&0xff;
                    break;
            case 13:F=getFlagADD(C,0xFF)&0xFE | F&0x01; 
                    C=(C-1)&0xff;                                      
                    break;
            case 14:
                    C=memory[PC++];
                    break;
            case 15:temp=A%2;A=A/2+temp*128;
                    F=(F&254)^temp;
                    break;
            case 16:
                    break;
            case 17:
                    E=memory[PC++];
                    D=memory[PC++];
                    break;
            case 18:int addrStaxD = 256 * D + E;
                    memory[addrStaxD] = A;
                    MemoryHeatmapVisualizer.recordWrite(addrStaxD);
                    break;
            case 19:temp=D<<8 | E;
                   temp++;
                   D=(temp>>8)&0xff; E=temp&0x00ff;
                    break;
            case 20:F=getFlagADD(D,0x01)&0xFE | F&0x01;
                    D=(D+1)&0xff;
                    break;

            case 21:F=getFlagADD(D,0xFF)&0xFE | F&0x01; 
                    D=(D-1)&0xff;
                    break;
            case 22:
                    D=memory[PC++];
                    break;
            case 23:temp=A/128;A=(A%128)*2+(F&1);
                    F=(F&254)^temp;
                    break;
            case 24:
                    break;
            case 25:H=H+D;L=L+E;
                   if(L>255){H=H+L/256;L=L%256;}
                   if(H>255){F=(F&254)^1;H=H%256;}
                    break;
            case 26:int addrLdaxD = 256 * D + E;
                    A = memory[addrLdaxD];
                    MemoryHeatmapVisualizer.recordRead(addrLdaxD);
                    break;
            case 27:E--;
                    if(((D*256)+E)<0){D=255;E=255;}
                    if(E<0){E=255;D--;}
                    break;
            case 28:F=getFlagADD(E,0x01)&0xFE | F&0x01;
                    E=(E+1)&0xff;
                    break;
            case 29:F=getFlagADD(E,0xFF)&0xFE | F&0x01; 
                    E=(E-1)&0xff;
                    break;
            case 30:E=memory[PC++];
                    break;

            case 31:temp=A%2;A=A/2+(F&1)*128;
                    F=(F&254)^temp;
                   break;
            case 32:A=SID*128+R75*64+R65*32+R55*16+IE*8+M75*4+M65*2+M55;
                    break;
            case 33:
                    L=memory[PC++];
                    H=memory[PC++];
                    break;
            case 34:temp=(memory[PC++]+memory[PC++]*256);
                    memory[temp]=L;
                    memory[temp+1]=H;
                    MemoryHeatmapVisualizer.recordWrite(temp);
                    MemoryHeatmapVisualizer.recordWrite(temp + 1);
                    break;
            case 35:temp=H<<8 | L;
                   temp++;
                   H=(temp>>8)&0xff; L=temp&0x00ff;
                    break;
            case 36:F=getFlagADD(H,0x01)&0xFE | F&0x01;
                    H=(H+1)&0xff;
                    break;
            case 37:F=getFlagADD(H,0xFF)&0xFE | F&0x01; 
                    H=(H-1)&0xff;
                    break;
            case 38:H=memory[PC++];
                    break;
            case 39:temp=0;
                    if((A&0x0f)>0x09 || (F&0x10)==0x10) {
                        temp=0x06;
                        }
                    if((A&0xf0)>0x90 || (F&0x1)==1){
                        temp=temp+0x60;
                        }
                    F=(getFlagADD(A,temp)&0xFF)^(F&0x01);
                    A=A+temp;
                    A=A&0xff;
                    break;
            case 40:
                    break;

            case 41:H=H+H;L=L+L;
                   if(L>255){H=H+L/256;L=L%256;}
                   if(H>255){F=(F&254)^1;H=H%256;}
                    break;
            case 42:temp=(memory[PC++]+memory[PC++]*256);
                    L=memory[temp];
                    H=memory[temp+1];
                    MemoryHeatmapVisualizer.recordRead(temp);
                    MemoryHeatmapVisualizer.recordRead(temp + 1);
                    break;
            case 43:L--;
                    if(((H*256)+L)<0){H=255;L=255;}
                    if(L<0){L=255;H--;}
                    break;
            case 44:F=getFlagADD(L,0x01)&0xFE | F&0x01;                   
                    L=(L+1)&0xff;
                    break;
            case 45:F=getFlagADD(L,0xFF)&0xFE | F&0x01; 
                    L=(L-1)&0xff;
                    break;
            case 46:L=memory[PC++];
                    break;
            case 47:A=255-A;
                    break;
            case 48:SOD=A/128;
                    SDE=(A%128)/64;
                    D1=(A%64)/32;
                    RR75=(A%32)/16;
                    MSE=(A%16)/8;
                    M75=(A%8)/4;
                    M65=(A%4)/2;
                    M55=(A%2);
                    break;
            case 49:SP=memory[PC++]+memory[PC++]*256;
                    break;
            case 50:int staAddr = memory[PC++] + memory[PC++] * 256;
                    memory[staAddr] = A;
                    MemoryHeatmapVisualizer.recordWrite(staAddr);
                    break;
            case 51:SP=(SP+1)&0xFFFF;
                   break;
            case 52:F=getFlagADD(memory[256*H+L],0x01)&0xFE | F&0x01;                    
                    memory[256*H+L]=(memory[256*H+L]+1)&0xff;
                    MemoryHeatmapVisualizer.recordWrite(256*H+L);
                    break;
            case 53:F=getFlagADD(memory[256*H+L],0xFF)&0xFE | F&0x01; 
                    memory[256*H+L]=(memory[256*H+L]-1)&0xff;                    
                    MemoryHeatmapVisualizer.recordWrite(256*H+L);
                    break;
            case 54:int mviMAddr = 256 * H + L;
                    memory[mviMAddr] = memory[PC++];
                    MemoryHeatmapVisualizer.recordWrite(mviMAddr);
                    break;
            case 55:F=(F|1);
                    break;
            case 56:
                    break;
            case 57:H=H+(SP&240);L=L+(SP&15);
                   if(L>255){H=H+L/256;L=L%256;}
                   if(H>255){F=(F&254)^1;H=H%256;}
                   break;
            case 58:int ldaAddr = memory[PC++] + memory[PC++] * 256;
                    A = memory[ldaAddr];
                    MemoryHeatmapVisualizer.recordRead(ldaAddr);
                    break;
            case 59:SP--;
                    if(SP<0)SP=stopAddress;
                    break;
            case 60:F=getFlagADD(A,0x01)&0xFE | F&0x01;
                    A=(A+1)&0xff;
                    break;
            case 61:F=getFlagADD(A,0xFF)&0xFE | F&0x01; 
                    A=(A-1)&0xff;
                   break;
            case 62:A=memory[PC++];
                    break;
            case 63:F=F^1;
                    break;
            case 64:B=B;
                    break;
            case 65:B=C;
                    break;
            case 66:B=D;
                    break;
            case 67:B=E;
                    break;
            case 68:B=H;
                    break;
            case 69:B=L;
                    break;
            case 70:B=memory[256*H+L];
                    break;
            case 71:B=A;
                   break;
            case 72:C=B;
                    break;
            case 73:C=C;
                    break;
            case 74:C=D;
                    break;
            case 75:C=E;
                    break;
            case 76:C=H;
                    break;
            case 77:C=L;
                    break;
            case 78:C=memory[256*H+L];
                    break;
            case 79:C=A;
                    break;
            case 80:D=B;
                    break;
            case 81:D=C;
                   break;
            case 82:D=D;
                    break;
            case 83:D=E;
                    break;
            case 84:D=H;
                    break;
            case 85:D=L;
                    break;
            case 86:D=memory[256*H+L];
                    break;
            case 87:D=A;
                    break;
            case 88:E=B;
                    break;
            case 89:E=C;
                    break;
            case 90:E=D;
                    break;
            case 91:E=E;
                   break;
            case 92:E=H;
                    break;
            case 93:E=L;
                    break;
            case 94:E=memory[256*H+L];
                    break;
            case 95:E=A;
                    break;
            case 96:H=B;
                    break;
            case 97:H=C;
                    break;
            case 98:H=D;
                    break;
            case 99:H=E;
                    break;
            case 100:H=H;
                    break;
            case 101:H=L;
                   break;
            case 102:H=memory[256*H+L];
                    break;
            case 103:H=A;
                    break;
            case 104:L=B;
                    break;
            case 105:L=C;
                    break;
            case 106:L=D;
                    break;
            case 107:L=E;
                    break;
            case 108:L=H;
                    break;
            case 109:L=L;
                    break;
            case 110:L=memory[256*H+L];
                    break;
            case 111:L=A;
                   break;
            case 112:memory[256*H+L]=B;
                    break;
            case 113:memory[256*H+L]=C;
                    break;
            case 114:memory[256*H+L]=D;
                    break;
            case 115:memory[256*H+L]=E;
                    break;
            case 116:memory[256*H+L]=H;
                    break;
            case 117:memory[256*H+L]=L;
                    break;
            case 118:o.stop=true;PC--;
                    break;
            case 119:memory[256*H+L]=A;
                    break;
            case 120:A=B;
                    break;
            case 121:A=C;
                   break;
            case 122:A=D;
                    break;
            case 123:A=E;
                    break;
            case 124:A=H;
                    break;
            case 125:A=L;
                    break;
            case 126:A=memory[256*H+L];
                    break;
            case 127:A=A;
                    break;
            case 128:F=getFlagADD(A, B);
                     A=A+B;
                     if(A/256>=1)A=A%256;
                    break;
            case 129:F=getFlagADD(A, C);
                     A=A+C;
                     if(A/256>=1)A=A%256;
                    break;
            case 130:F=getFlagADD(A, D);
                     A=A+D;
                     if(A/256>=1)A=A%256;
                    break;
            case 131:F=getFlagADD(A, E);
                     A=A+E;
                     if(A/256>=1)A=A%256;
                   break;
            case 132:F=getFlagADD(A, H);
                     A=A+H;
                     if(A/256>=1)A=A%256;
                    break;
            case 133:F=getFlagADD(A, L);
                     A=A+L;
                     if(A/256>=1)A=A%256;
                    break;
            case 134:F=getFlagADD(A, memory[H*256+L]);
                     A=A+memory[H*256+L];
                     if(A/256>=1)A=A%256;
                    break;
            case 135:F=getFlagADD(A, A);
                     A=A+A;
                     if(A/256>=1)A=A%256;
                    break;
            case 136:temp=F&1;
                     F=getFlagADD(A, B+temp);
                     A=A+B+temp;
                     if(A/256>=1)A=A%256;
                    break;
            case 137:temp=F&1;
                     F=getFlagADD(A, C+temp);
                     A=A+C+temp;
                     if(A/256>=1)A=A%256;
                    break;
            case 138:temp=F&1;
                     F=getFlagADD(A, D+temp);
                     A=A+D+temp;
                     if(A/256>=1)A=A%256;
                    break;
            case 139:temp=F&1;
                     F=getFlagADD(A, E+temp);
                     A=A+E+temp;
                     if(A/256>=1)A=A%256;
                    break;
            case 140:temp=F&1;
                     F=getFlagADD(A, H+temp);
                     A=A+H+temp;
                     if(A/256>=1)A=A%256;
                    break;

            case 141:temp=F&1;
                     F=getFlagADD(A, L+temp);
                     A=A+L+temp;
                     if(A/256>=1)A=A%256;
                    break;
            case 142:temp=F&1;
                     F=getFlagADD(A, memory[H*256+L]+temp);
                     A=A+memory[H*256+L]+temp;
                     if(A/256>=1)A=A%256;
                    break;
            case 143:temp=F&1;
                     F=getFlagADD(A, A+temp);
                     A=A+A+temp;
                     if(A/256>=1)A=A%256;
                    break;
            case 144:temp=_2sCompliment(B);
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 145:temp=_2sCompliment(C); 
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 146:temp=_2sCompliment(D);  
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 147:temp=_2sCompliment(E);  
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 148:temp=_2sCompliment(H);  
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 149:temp=_2sCompliment(L); 
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 150:temp=_2sCompliment(memory[H*256+L]);  
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 151:temp=_2sCompliment(A); 
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                   break;
            case 152:temp = B+(F&0x01);
                     temp=_2sCompliment(temp);
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 153:temp = C+(F&0x01);
                     temp=_2sCompliment(temp); 
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 154:temp = D+(F&0x01);
                     temp=_2sCompliment(temp); 
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 155:temp = E+(F&0x01);
                     temp=_2sCompliment(temp); 
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 156:temp = H+(F&0x01);
                     temp=_2sCompliment(temp); 
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 157:temp = L+(F&0x01);
                     temp=_2sCompliment(temp); 
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 158:temp = memory[H*256+L]+(F&0x01);
                     temp=_2sCompliment(temp); 
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 159:temp = A+(F&0x01);
                     temp=_2sCompliment(temp); 
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 160:A=(A&B);
                    F=getFlagForLogic(A, 1, 0);
                    break;
            case 161:A=(A&C);
                    F=getFlagForLogic(A, 1, 0);
                   break;
            case 162:A=(A&D);
                    F=getFlagForLogic(A, 1, 0);
                    break;
            case 163:A=(A&E);
                    F=getFlagForLogic(A, 1, 0);
                    break;
            case 164:A=(A&H);
                    F=getFlagForLogic(A, 1, 0);
                    break;
            case 165:A=(A&L);
                    F=getFlagForLogic(A, 1, 0);
                    break;
            case 166:A=(A&memory[H* 256+L]);
                    F=getFlagForLogic(A, 1, 0);
                    break;
            case 167:A=(A&A);
                    F=getFlagForLogic(A, 1, 0);
                    break;
            case 168:A=(A^B);
                    F=getFlagForLogic(A, 0, 0);
                    break;
            case 169:A=(A^C);
                    F=getFlagForLogic(A, 0, 0);
                    break;
            case 170:A=(A^D);
                    F=getFlagForLogic(A, 0, 0);
                    break;
            case 171:A=(A^E);
                    F=getFlagForLogic(A, 0, 0);
                   break;
            case 172:A=(A^H);
                    F=getFlagForLogic(A, 0, 0);
                    break;
            case 173:A=(A^L);
                    F=getFlagForLogic(A, 0, 0);
                    break;
            case 174:A=(A^memory[H*256+L]);
                    F=getFlagForLogic(A, 0, 0);
                    break;
            case 175:A=(A^A);
                    F=getFlagForLogic(A, 0, 0);
                    break;
            case 176:A=(A|B);
                    F=getFlagForLogic(A, 0, 0);
                    break;
            case 177:A=(A|C);
                    F=getFlagForLogic(A, 0, 0);
                    break;
            case 178:A=(A|D);
                    F=getFlagForLogic(A, 0, 0);
                    break;
            case 179:A=(A|E);
                    F=getFlagForLogic(A, 0, 0);
                    break;
            case 180:A=(A|H);
                    F=getFlagForLogic(A, 0, 0);
                    break;

            case 181:A=(A|L);
                    F=getFlagForLogic(A, 0, 0);
                   break;
            case 182:A=(A|memory[H*256+L]);
                    F=getFlagForLogic(A, 0, 0);
                    break;
            case 183:A=(A|A);
                    F=getFlagForLogic(A, 0, 0);
                    break;
            case 184:temp=_2sCompliment(B);
                     F=getFlagADD(A,temp);F=F^0x01;
                    break;
            case 185:temp=_2sCompliment(C);
                     F=getFlagADD(A,temp);F=F^0x01;
                    break;
            case 186:temp=_2sCompliment(D);
                     F=getFlagADD(A,temp);F=F^0x01;
                    break;
            case 187:temp=_2sCompliment(E);
                     F=getFlagADD(A,temp);F=F^0x01;
                    break;
            case 188:temp=_2sCompliment(H);
                     F=getFlagADD(A,temp);F=F^0x01;
                    break;
            case 189:temp=_2sCompliment(L);
                     F=getFlagADD(A,temp);F=F^0x01;
                    break;
            case 190:temp=_2sCompliment(memory[H*256+L]);
                     F=getFlagADD(A,temp);F=F^0x01;
                    break;
            case 191:temp=_2sCompliment(A);
                     F=getFlagADD(A,temp);F=F^0x01;
                   break;
            case 192:if((F&64)==0)
                     {int n[]=popInStack();
                        PC=n[0]*256+n[1];
                      }
                    break;
            case 193:no=popInStack();
                     B=no[0];C=no[1];
                    break;
            case 194: if ((F&64)== 0)PC = memory[PC++] + memory[PC++] * 256;
                      else {PC += 2;clockCycleCounter-=3;}
                    break;
            case 195:PC=memory[PC++]+memory[PC++]*256;
                    break;
            case 196:if((F&64)==0){
                    temp=PC+2;
                    pushInStack(temp/256, temp%256);
                    PC = memory[PC++] + memory[PC++] * 256;
                     }
                    else PC+=2;
                    break;
            case 197:pushInStack(B, C);
                    break;
            case 198:F=getFlagADD(A, memory[PC]);
                     A=A+memory[PC++];
                     if(A/256>=1)A=A%256;
                    break;
            case 199:pushInStack(PC/256, PC%256);
                    PC=beginAddress;
                    break;
            case 200:if((F&64)==64)
                     {int n[]=popInStack();
                        PC=n[0]*256+n[1];
                      }
                    break;

            case 201:{int n[]=popInStack();
                        PC=n[0]*256+n[1];   
                        }
                   break;
            case 202: if ((F&64)== 64)PC = memory[PC++] + memory[PC++] * 256;
                      else {PC += 2;clockCycleCounter-=3;}
                    break;
            case 203:
                    break;
            case 204:if((F&64)==64){
                    temp=PC+2;
                    pushInStack(temp/256, temp%256);
                    PC = memory[PC++] + memory[PC++] * 256;
                     }
                    else PC+=2;
                    break;
            case 205:temp=PC+2;
                    pushInStack(temp/256, temp%256);
                    PC = memory[PC++] + memory[PC++] * 256;
                    break;
            case 206:temp=F&1;
                     F=getFlagADD(A, memory[PC]+temp);
                     A=A+memory[PC++]+temp;
                     if(A/256>=1)A=A%256;
                    break;
            case 207:pushInStack(PC/256, PC%256);
                    PC=beginAddress+8;
                    break;
            case 208:if((F&1)==0)
                     {int n[]=popInStack();
                        PC=n[0]*256+n[1];
                      }
                    break;
            case 209:no=popInStack();
                     D=no[0];E=no[1];
                    break;
            case 210:
                    if ((F&1)== 0)PC = memory[PC++] + memory[PC++] * 256;
                    else {PC += 2;clockCycleCounter-=3;}
                    break;
            case 211:port[memory[PC++]]=A;
                   break;
            case 212:if((F&1)==0){
                    temp=PC+2;
                    pushInStack(temp/256, temp%256);
                    PC = memory[PC++] + memory[PC++] * 256;
                     }
                    else PC+=2;
                    break;
            case 213:pushInStack(D, E);
                    break;
            case 214:temp = memory[PC++];
                     temp=_2sCompliment(temp); 
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 215:pushInStack(PC/256, PC%256);
                    PC=beginAddress+16;
                    break;
            case 216:if((F&1)==1)
                     {int n[]=popInStack();
                        PC=n[0]*256+n[1];
                      }
                    break;
            case 217:
                    break;
            case 218:
                    if ((F&1)== 1)PC = memory[PC++] + memory[PC++] * 256;
                    else {PC += 2;clockCycleCounter-=3;}
                    break;
            case 219:A=port[memory[PC++]];
                    break;
            case 220:if((F&1)==1){
                    temp=PC+2;
                    pushInStack(temp/256, temp%256);
                    PC = memory[PC++] + memory[PC++] * 256;
                     }
                    else PC+=2;
                    break;
            case 221:
                   break;
            case 222:temp = memory[PC++]+(F&0x01);
                     temp=_2sCompliment(temp); 
                     F=getFlagADD(A,temp);F=F^0x01;
                     A=(A+temp)&0xff;
                    break;
            case 223:pushInStack(PC/256, PC%256);
                    PC=beginAddress+24;
                    break;
            case 224:if((F&4)==0)
                     {int n[]=popInStack();
                        PC=n[0]*256+n[1];
                      }
                    break;
            case 225:no=popInStack();
                     H=no[0];L=no[1];
                    break;
            case 226:
                    if ((F&4)== 0)PC = memory[PC++] + memory[PC++] * 256;
                    else {PC += 2;clockCycleCounter-=3;}
                    break;
            case 227:{
                      int temp1=H,temp2=L;
                      H=(memory[SP+1]&0xff);L=(memory[SP]&0x00ff);
                      memory[SP+1]=temp1; memory[SP]=temp2;
                     }
                    break;
            case 228:if((F&4)==0){
                    temp=PC+2;
                    pushInStack(temp/256, temp%256);
                    PC = memory[PC++] + memory[PC++] * 256;
                     }
                    else PC+=2;
                    break;
            case 229:pushInStack(H, L);
                    break;
            case 230:A=(A&memory[PC++]);
                    F=getFlagForLogic(A, 1, 0);
                    break;
            case 231:pushInStack(PC/256, PC%256);
                   PC=beginAddress+32;
                   break;
            case 232:if((F&4)==4)
                     {int n[]=popInStack();
                        PC=n[0]*256+n[1];
                      }
                    break;
            case 233:PC=H*256+L;
                    break;
            case 234:
                    if ((F&4)== 4)PC = memory[PC++] + memory[PC++] * 256;
                    else {PC += 2;clockCycleCounter-=3;}
                    break;
            case 235:temp=D;D=H;H=temp;
                     temp=E;E=L;L=temp;
                    break;
            case 236:if((F&4)==4){
                    temp=PC+2;
                    pushInStack(temp/256, temp%256);
                    PC = memory[PC++] + memory[PC++] * 256;
                     }
                    else PC+=2;
                    break;
            case 237:
                    break;
            case 238:A=(A^memory[PC++]);
                    F=getFlagForLogic(A, 0, 0);
                    break;
            case 239:pushInStack(PC/256, PC%256);
                    PC=beginAddress+40;
                    break;
            case 240:if((F&128)==0)
                     {int n[]=popInStack();
                        PC=n[0]*256+n[1];
                      }
                    break;
            case 241:no=popInStack();
                     A=no[0];F=no[1];
                    break;
            case 242:
                    if ((F&128)== 0)PC = memory[PC++] + memory[PC++] * 256;
                    else {PC += 2;clockCycleCounter-=3;}
                    break;
            case 243:IE=0;
                    break;
            case 244:if((F&128)==0){
                    temp=PC+2;
                    pushInStack(temp/256, temp%256);
                    PC = memory[PC++] + memory[PC++] * 256;
                     }
                    else PC+=2;
                    break;
            case 245:pushInStack(A, F);
                    break;
            case 246:A=(A|memory[PC++]);
                    F=getFlagForLogic(A, 0, 0);
                    break;
            case 247:pushInStack(PC/256, PC%256);
                    PC=beginAddress+48;
                    break;
            case 248:if((F&128)==128)
                     {int n[]=popInStack();
                        PC=n[0]*256+n[1];
                      }
                    break;
            case 249:SP=H*256+L;
                    break;
            case 250:
                    if ((F&128)== 128)PC = memory[PC++] + memory[PC++] * 256;
                    else {PC += 2;clockCycleCounter-=3;}
                    break;
            case 251:IE=1;
                   break;
            case 252:if((F&128)==128){
                    temp=PC+2;
                    pushInStack(temp/256, temp%256);
                    PC = memory[PC++] + memory[PC++] * 256;
                     }
                    else PC+=2;
                    break;
            case 253:
                    break;
            case 254:temp=_2sCompliment(memory[PC++]);
                     F=getFlagADD(A,temp);F=F^0x01;
                    break;
            case 255:pushInStack(PC/256, PC%256);
                    PC=beginAddress+56;
                    break;

        }
        
        clockCycleCounter=clockCycleCounter+engine.I[index][2];
        instructionCounter++;

        if(stopAddress<PC||beginAddress>PC){
                        o.jButtonStop.doClick();
                        o.jLabelErrorHang.setText("You have exceeded the memory range");
                        o.jLabelErrorHang.setVisible(true);
                        PC=beginAddress;
       }
                interruptProcess();

    }

    public void pushInStack(int b,int c)
    {
        if(SP==beginAddress||SP==0)SP=stopAddress+1;
        memory[--SP]=b;
        memory[--SP]=c;
    }

    public int[] popInStack()
    {
        int[] no=new int[2];
        if(SP==stopAddress)SP=0;
        no[1]=memory[SP++];
        no[0]=memory[SP++];
        if(SP-1==stopAddress)SP=beginAddress;
        return no;

    }

    public void interruptProcess(){
        if(_RESETIN==0)IE=0;
        if(IE==1&&TRAP==1){IE=0;_INTA=0;PC=0x0024;}
        if(IE==1&&R75==1&&(RR75==0||_RESETIN==0)&&!(M75==1&&MSE==1)){IE=0;_INTA=0;PC=0x003C;}
        if(IE==1&&R65==1&&(!(M65==1&&MSE==1))){IE=0;_INTA=0;PC=0x0034;}
        if(IE==1&&R55==1&&!(M55==1&&MSE==1)){IE=0;_INTA=0;PC=0x002C;}        
        if(IE==1&&INTR==1){IE=0;_INTA=0;PC=0x0008;}
    }
    
   public int getFlagADD(int b,int c)
    {
        int S=0,Z=0,AC=0,P=0,CY=0,t=0;
        t = b + c;
        AC=((b&0xf)+(c&0xf))>0x0f?1:0;
        Z = (t & 0xff) == 0x00 ? 1 : 0;
        S = (t & 0x80) == 0x80 ? 1 : 0;
        CY= (t & 0x100)== 0x100 ? 1 : 0;
        P=getParity(t&0xff);
        return S<<7 | Z<<6 | AC<<4 | P<<2 | CY;
    }
   
      public int getFlagINR(int b,int CY)
    {
        int S=0,Z=0,AC=0,P=0,t=0;
        t = b + 1;
        AC=((b&0xf)+1)>0x0f?1:0;
        Z = (t & 0xff) == 0x00 ? 1 : 0;
        S = (t & 0x80) == 0x80 ? 1 : 0;
        P=getParity(t&0xff);
        return S<<7 | Z<<6 | AC<<4 | P<<2 | CY;

    }
   

    public int getFlagForLogic(int b,int ac,int c)
    {
        int S=0,Z=0,d2=0,AC=0,d1=0,P=0,d0=0,CY=0;
        if(b>127)S=1;
        if(b==c)Z=1;
        AC=ac;CY=c;
        P=(b/128)^(b/64)%2^(b/32)%2^(b/16)%2^(b/8)%2^(b/4)%2^(b/2)%2^(b%2)^1;
        return S*128+Z*64+d2*32+AC*16+d1*8+P*4+d0*2+CY;

    }
    
    int getParity(int num){
        int parity = num & 0xff;
        parity ^= ( parity >> 4 );
        parity ^= parity >> 2;
        parity ^= parity >> 1;
        return ( parity & 1 ) == 0?1:0;
    }
    
    int _2sCompliment(int num){
        return (((num&0xFF)^0xFF)+1)&0xFF;
    }
    
    public static void main(String[] args) {
        Assembler a=new Assembler();
        Matrix matrix = new Matrix(a);
        matrix.PC=90;
        matrix.functionRun(195);
        System.out.println(matrix.engine.Dec2Bin(240));
        System.out.println(Integer.toBinaryString(matrix.getFlagADD(18, 18)));
        System.out.println((36 & 0x80) != 0x80 ? 1 : 0);
        System.out.println(matrix.getParity(0xff));
        System.out.println("0x"+Integer.toHexString(0xff<<4));
        System.out.println("0x"+Integer.toHexString(0xff>>4));
        System.out.println("0x"+Integer.toHexString(0xff>>>4));
        System.out.println("0x"+Integer.toHexString(1<<7|1<<2));
        System.out.println("0x"+Integer.toHexString(0x12<<8|0x34));
        a.dispose();
        
}

}
