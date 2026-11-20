package net.saga.console.chip8.core;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Decodes and formats CHIP-8 / SUPER-CHIP / XO-CHIP instructions using Java 27 pattern matching.
 */
public final class Disassembler {
    private static final HexFormat HEX = HexFormat.of().withUpperCase();

    private Disassembler() {}

    public record DisassembledLine(int address, int rawOpcode, int byteLength, String mnemonic, Instruction instruction) {
        public String formattedAddress() {
            return HEX.toHexDigits((short) address);
        }

        public String formattedOpcode() {
            return HEX.toHexDigits((short) rawOpcode);
        }
    }

    public static Instruction decode(int opcode, int nextWord) {
        int high = opcode & 0xF000;
        int x = (opcode & 0x0F00) >>> 8;
        int y = (opcode & 0x00F0) >>> 4;
        int n = opcode & 0x000F;
        int nn = opcode & 0x00FF;
        int nnn = opcode & 0x0FFF;

        return switch (high) {
            case 0x0000 -> switch (opcode) {
                case 0x0010 -> new Instruction.MegaOff(opcode);
                case 0x0011 -> new Instruction.MegaOn(opcode);
                case 0x00E0 -> new Instruction.ClearScreen(opcode);
                case 0x00EE -> new Instruction.ReturnSubroutine(opcode);
                case 0x00FB -> new Instruction.ScrollRight(opcode);
                case 0x00FC -> new Instruction.ScrollLeft(opcode);
                case 0x00FD -> new Instruction.ExitInterpreter(opcode);
                case 0x00FE -> new Instruction.DisableHiRes(opcode);
                case 0x00FF -> new Instruction.EnableHiRes(opcode);
                case 0x0700 -> new Instruction.StopDigiSound(opcode);
                default -> {
                    int topByte = opcode & 0x0F00;
                    int sub = opcode & 0x00F0;
                    if (topByte == 0x0100) {
                        yield new Instruction.LoadIMega(opcode, ((opcode & 0xFF) << 16) | (nextWord & 0xFFFF));
                    } else if (topByte == 0x0200) {
                        yield new Instruction.LoadPalette(opcode, nn);
                    } else if (topByte == 0x0300) {
                        yield new Instruction.SetSpriteWidth(opcode, nn == 0 ? 256 : nn);
                    } else if (topByte == 0x0400) {
                        yield new Instruction.SetSpriteHeight(opcode, nn == 0 ? 256 : nn);
                    } else if (topByte == 0x0500) {
                        yield new Instruction.SetScreenAlpha(opcode, nn);
                    } else if ((opcode & 0x0FF0) == 0x0600) {
                        yield new Instruction.PlayDigiSound(opcode, n);
                    } else if ((opcode & 0x0FF0) == 0x0800) {
                        yield new Instruction.SetBlendMode(opcode, n);
                    } else if (topByte == 0x0900) {
                        yield new Instruction.SetCollisionColor(opcode, nn);
                    } else if (sub == 0x00B0 && topByte == 0) {
                        yield new Instruction.ScrollUp(opcode, n);
                    } else if (sub == 0x00C0 && topByte == 0) {
                        yield new Instruction.ScrollDown(opcode, n);
                    } else if (sub == 0x00D0 && topByte == 0) {
                        yield new Instruction.ScrollUp(opcode, n);
                    } else {
                        yield new Instruction.Unknown(opcode);
                    }
                }
            };
            case 0x1000 -> new Instruction.Jump(opcode, nnn);
            case 0x2000 -> new Instruction.Call(opcode, nnn);
            case 0x3000 -> new Instruction.SkipEqualByte(opcode, x, nn);
            case 0x4000 -> new Instruction.SkipNotEqualByte(opcode, x, nn);
            case 0x5000 -> switch (n) {
                case 0x0 -> new Instruction.SkipEqualReg(opcode, x, y);
                case 0x2 -> new Instruction.SaveRegRange(opcode, x, y);
                case 0x3 -> new Instruction.LoadRegRange(opcode, x, y);
                default -> new Instruction.Unknown(opcode);
            };
            case 0x6000 -> new Instruction.LoadByte(opcode, x, nn);
            case 0x7000 -> new Instruction.AddByte(opcode, x, nn);
            case 0x8000 -> switch (n) {
                case 0x0 -> new Instruction.MoveReg(opcode, x, y);
                case 0x1 -> new Instruction.OrReg(opcode, x, y);
                case 0x2 -> new Instruction.AndReg(opcode, x, y);
                case 0x3 -> new Instruction.XorReg(opcode, x, y);
                case 0x4 -> new Instruction.AddReg(opcode, x, y);
                case 0x5 -> new Instruction.SubReg(opcode, x, y);
                case 0x6 -> new Instruction.ShiftRight(opcode, x, y);
                case 0x7 -> new Instruction.SubReverseReg(opcode, x, y);
                case 0xE -> new Instruction.ShiftLeft(opcode, x, y);
                default -> new Instruction.Unknown(opcode);
            };
            case 0x9000 -> n == 0 ? new Instruction.SkipNotEqualReg(opcode, x, y) : new Instruction.Unknown(opcode);
            case 0xA000 -> new Instruction.LoadI(opcode, nnn);
            case 0xB000 -> new Instruction.JumpOffset(opcode, nnn, x);
            case 0xC000 -> new Instruction.RandomMask(opcode, x, nn);
            case 0xD000 -> new Instruction.DrawSprite(opcode, x, y, n);
            case 0xE000 -> switch (nn) {
                case 0x9E -> new Instruction.SkipKeyPressed(opcode, x);
                case 0xA1 -> new Instruction.SkipKeyNotPressed(opcode, x);
                default -> new Instruction.Unknown(opcode);
            };
            case 0xF000 -> {
                if (opcode == 0xF000) {
                    yield new Instruction.LoadILong(opcode, nextWord & 0xFFFF);
                }
                if (opcode == 0xF002) {
                    yield new Instruction.LoadAudioPattern(opcode);
                }
                yield switch (nn) {
                    case 0x01 -> new Instruction.SelectPlanes(opcode, x);
                    case 0x07 -> new Instruction.LoadDelayTimer(opcode, x);
                    case 0x0A -> new Instruction.WaitKey(opcode, x);
                    case 0x15 -> new Instruction.SetDelayTimer(opcode, x);
                    case 0x18 -> new Instruction.SetSoundTimer(opcode, x);
                    case 0x1E -> new Instruction.AddToI(opcode, x);
                    case 0x29 -> new Instruction.LoadSmallFont(opcode, x);
                    case 0x30 -> new Instruction.LoadBigFont(opcode, x);
                    case 0x33 -> new Instruction.StoreBcd(opcode, x);
                    case 0x3A -> new Instruction.SetPitch(opcode, x);
                    case 0x55 -> new Instruction.StoreRegisters(opcode, x);
                    case 0x65 -> new Instruction.LoadRegisters(opcode, x);
                    case 0x75 -> new Instruction.SaveRplFlags(opcode, x);
                    case 0x85 -> new Instruction.LoadRplFlags(opcode, x);
                    default -> new Instruction.Unknown(opcode);
                };
            }
            default -> new Instruction.Unknown(opcode);
        };
    }

    public static String format(Instruction instruction) {
        return switch (instruction) {
            case Instruction.ClearScreen _ -> "CLS";
            case Instruction.ReturnSubroutine _ -> "RET";
            case Instruction.ScrollDown(_, int lines) -> "SCD " + lines;
            case Instruction.ScrollUp(_, int lines) -> "SCU " + lines;
            case Instruction.ScrollRight _ -> "SCR";
            case Instruction.ScrollLeft _ -> "SCL";
            case Instruction.ExitInterpreter _ -> "EXIT";
            case Instruction.DisableHiRes _ -> "LOW";
            case Instruction.EnableHiRes _ -> "HIGH";
            case Instruction.MegaOff _ -> "MEGAOFF";
            case Instruction.MegaOn _ -> "MEGAON";
            case Instruction.LoadIMega(_, int addr24) -> "LDHI I, 0x" + String.format("%06X", addr24 & 0xFFFFFF);
            case Instruction.LoadPalette(_, int count) -> "LDPAL " + count;
            case Instruction.SetSpriteWidth(_, int w) -> "SPRW " + w;
            case Instruction.SetSpriteHeight(_, int h) -> "SPRH " + h;
            case Instruction.SetScreenAlpha(_, int a) -> "ALPHA " + hex8(a);
            case Instruction.PlayDigiSound(_, int mode) -> "DIGISND " + mode;
            case Instruction.StopDigiSound _ -> "STOPSND";
            case Instruction.SetBlendMode(_, int m) -> "BMODE " + m;
            case Instruction.SetCollisionColor(_, int c) -> "CCOL " + hex8(c);
            case Instruction.Jump(_, int addr) -> "JP " + hex12(addr);
            case Instruction.Call(_, int addr) -> "CALL " + hex12(addr);
            case Instruction.SkipEqualByte(_, int vx, int val) -> "SE V" + hex4(vx) + ", " + hex8(val);
            case Instruction.SkipNotEqualByte(_, int vx, int val) -> "SNE V" + hex4(vx) + ", " + hex8(val);
            case Instruction.SkipEqualReg(_, int vx, int vy) -> "SE V" + hex4(vx) + ", V" + hex4(vy);
            case Instruction.SaveRegRange(_, int vx, int vy) -> "SAVE V" + hex4(vx) + "..V" + hex4(vy);
            case Instruction.LoadRegRange(_, int vx, int vy) -> "LOAD V" + hex4(vx) + "..V" + hex4(vy);
            case Instruction.LoadByte(_, int vx, int val) -> "LD V" + hex4(vx) + ", " + hex8(val);
            case Instruction.AddByte(_, int vx, int val) -> "ADD V" + hex4(vx) + ", " + hex8(val);
            case Instruction.MoveReg(_, int vx, int vy) -> "LD V" + hex4(vx) + ", V" + hex4(vy);
            case Instruction.OrReg(_, int vx, int vy) -> "OR V" + hex4(vx) + ", V" + hex4(vy);
            case Instruction.AndReg(_, int vx, int vy) -> "AND V" + hex4(vx) + ", V" + hex4(vy);
            case Instruction.XorReg(_, int vx, int vy) -> "XOR V" + hex4(vx) + ", V" + hex4(vy);
            case Instruction.AddReg(_, int vx, int vy) -> "ADD V" + hex4(vx) + ", V" + hex4(vy);
            case Instruction.SubReg(_, int vx, int vy) -> "SUB V" + hex4(vx) + ", V" + hex4(vy);
            case Instruction.ShiftRight(_, int vx, int vy) -> "SHR V" + hex4(vx) + ", V" + hex4(vy);
            case Instruction.SubReverseReg(_, int vx, int vy) -> "SUBN V" + hex4(vx) + ", V" + hex4(vy);
            case Instruction.ShiftLeft(_, int vx, int vy) -> "SHL V" + hex4(vx) + ", V" + hex4(vy);
            case Instruction.SkipNotEqualReg(_, int vx, int vy) -> "SNE V" + hex4(vx) + ", V" + hex4(vy);
            case Instruction.LoadI(_, int addr) -> "LD I, " + hex12(addr);
            case Instruction.LoadILong(_, int addr) -> "LD I, " + hex16(addr);
            case Instruction.JumpOffset(_, int addr, int vx) -> "JP V" + hex4(vx) + ", " + hex12(addr);
            case Instruction.RandomMask(_, int vx, int mask) -> "RND V" + hex4(vx) + ", " + hex8(mask);
            case Instruction.DrawSprite(_, int vx, int vy, int h) -> "DRW V" + hex4(vx) + ", V" + hex4(vy) + ", " + h;
            case Instruction.SkipKeyPressed(_, int vx) -> "SKP V" + hex4(vx);
            case Instruction.SkipKeyNotPressed(_, int vx) -> "SKNP V" + hex4(vx);
            case Instruction.SelectPlanes(_, int mask) -> "PLANE " + mask;
            case Instruction.LoadAudioPattern _ -> "AUDIO";
            case Instruction.SetPitch(_, int vx) -> "PITCH V" + hex4(vx);
            case Instruction.LoadDelayTimer(_, int vx) -> "LD V" + hex4(vx) + ", DT";
            case Instruction.WaitKey(_, int vx) -> "LD V" + hex4(vx) + ", K";
            case Instruction.SetDelayTimer(_, int vx) -> "LD DT, V" + hex4(vx);
            case Instruction.SetSoundTimer(_, int vx) -> "LD ST, V" + hex4(vx);
            case Instruction.AddToI(_, int vx) -> "ADD I, V" + hex4(vx);
            case Instruction.LoadSmallFont(_, int vx) -> "LD F, V" + hex4(vx);
            case Instruction.LoadBigFont(_, int vx) -> "LD HF, V" + hex4(vx);
            case Instruction.StoreBcd(_, int vx) -> "LD B, V" + hex4(vx);
            case Instruction.StoreRegisters(_, int max) -> "LD [I], V0..V" + hex4(max);
            case Instruction.LoadRegisters(_, int max) -> "LD V0..V" + hex4(max) + ", [I]";
            case Instruction.SaveRplFlags(_, int max) -> "LD R, V0..V" + hex4(max);
            case Instruction.LoadRplFlags(_, int max) -> "LD V0..V" + hex4(max) + ", R";
            case Instruction.Unknown(int raw) -> "DW " + hex16(raw);
        };
    }

    public static List<DisassembledLine> disassembleWindow(byte[] memory, int centerPc, int countBefore, int countAfter) {
        int start = Math.max(0x200, (centerPc - countBefore * 2) & ~1);
        int end = Math.min(memory.length - 2, centerPc + countAfter * 2);
        List<DisassembledLine> lines = new ArrayList<>();
        int addr = start;
        while (addr <= end) {
            int opcode = ((memory[addr] & 0xFF) << 8) | (memory[addr + 1] & 0xFF);
            int nextWord = (addr + 3 < memory.length)
                ? ((memory[addr + 2] & 0xFF) << 8) | (memory[addr + 3] & 0xFF)
                : 0;
            Instruction inst = decode(opcode, nextWord);
            lines.add(new DisassembledLine(addr, opcode, inst.byteLength(), format(inst), inst));
            addr += inst.byteLength();
        }
        return lines;
    }

    private static String hex4(int v) {
        return Integer.toHexString(v & 0xF).toUpperCase();
    }

    private static String hex8(int v) {
        return "0x" + HEX.toHexDigits((byte) (v & 0xFF));
    }

    private static String hex12(int v) {
        return "0x" + String.format("%03X", v & 0xFFF);
    }

    private static String hex16(int v) {
        return "0x" + HEX.toHexDigits((short) (v & 0xFFFF));
    }
}
