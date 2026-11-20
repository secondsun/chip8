package net.saga.console.chip8.core;

/**
 * Sealed algebraic representation of decoded CHIP-8 / SUPER-CHIP / XO-CHIP instructions.
 */
public sealed interface Instruction {
    int rawOpcode();
    default int byteLength() {
        return 2;
    }

    record ClearScreen(int rawOpcode) implements Instruction {}
    record ReturnSubroutine(int rawOpcode) implements Instruction {}
    record ScrollDown(int rawOpcode, int lines) implements Instruction {}
    record ScrollUp(int rawOpcode, int lines) implements Instruction {}
    record ScrollRight(int rawOpcode) implements Instruction {}
    record ScrollLeft(int rawOpcode) implements Instruction {}
    record ExitInterpreter(int rawOpcode) implements Instruction {}
    record DisableHiRes(int rawOpcode) implements Instruction {}
    record EnableHiRes(int rawOpcode) implements Instruction {}
    record MegaOff(int rawOpcode) implements Instruction {}
    record MegaOn(int rawOpcode) implements Instruction {}
    record LoadIMega(int rawOpcode, int address24) implements Instruction {
        @Override
        public int byteLength() {
            return 4;
        }
    }
    record LoadPalette(int rawOpcode, int count) implements Instruction {}
    record SetSpriteWidth(int rawOpcode, int width) implements Instruction {}
    record SetSpriteHeight(int rawOpcode, int height) implements Instruction {}
    record SetScreenAlpha(int rawOpcode, int alpha) implements Instruction {}
    record PlayDigiSound(int rawOpcode, int loopMode) implements Instruction {}
    record StopDigiSound(int rawOpcode) implements Instruction {}
    record SetBlendMode(int rawOpcode, int mode) implements Instruction {}
    record SetCollisionColor(int rawOpcode, int colorIndex) implements Instruction {}
    record Jump(int rawOpcode, int address) implements Instruction {}
    record Call(int rawOpcode, int address) implements Instruction {}
    record SkipEqualByte(int rawOpcode, int vx, int value) implements Instruction {}
    record SkipNotEqualByte(int rawOpcode, int vx, int value) implements Instruction {}
    record SkipEqualReg(int rawOpcode, int vx, int vy) implements Instruction {}
    record SaveRegRange(int rawOpcode, int vx, int vy) implements Instruction {}
    record LoadRegRange(int rawOpcode, int vx, int vy) implements Instruction {}
    record LoadByte(int rawOpcode, int vx, int value) implements Instruction {}
    record AddByte(int rawOpcode, int vx, int value) implements Instruction {}
    record MoveReg(int rawOpcode, int vx, int vy) implements Instruction {}
    record OrReg(int rawOpcode, int vx, int vy) implements Instruction {}
    record AndReg(int rawOpcode, int vx, int vy) implements Instruction {}
    record XorReg(int rawOpcode, int vx, int vy) implements Instruction {}
    record AddReg(int rawOpcode, int vx, int vy) implements Instruction {}
    record SubReg(int rawOpcode, int vx, int vy) implements Instruction {}
    record ShiftRight(int rawOpcode, int vx, int vy) implements Instruction {}
    record SubReverseReg(int rawOpcode, int vx, int vy) implements Instruction {}
    record ShiftLeft(int rawOpcode, int vx, int vy) implements Instruction {}
    record SkipNotEqualReg(int rawOpcode, int vx, int vy) implements Instruction {}
    record LoadI(int rawOpcode, int address) implements Instruction {}
    record LoadILong(int rawOpcode, int address) implements Instruction {
        @Override
        public int byteLength() {
            return 4;
        }
    }
    record JumpOffset(int rawOpcode, int address, int vx) implements Instruction {}
    record RandomMask(int rawOpcode, int vx, int mask) implements Instruction {}
    record DrawSprite(int rawOpcode, int vx, int vy, int height) implements Instruction {}
    record SkipKeyPressed(int rawOpcode, int vx) implements Instruction {}
    record SkipKeyNotPressed(int rawOpcode, int vx) implements Instruction {}
    record SelectPlanes(int rawOpcode, int planeMask) implements Instruction {}
    record LoadAudioPattern(int rawOpcode) implements Instruction {}
    record SetPitch(int rawOpcode, int vx) implements Instruction {}
    record LoadDelayTimer(int rawOpcode, int vx) implements Instruction {}
    record WaitKey(int rawOpcode, int vx) implements Instruction {}
    record SetDelayTimer(int rawOpcode, int vx) implements Instruction {}
    record SetSoundTimer(int rawOpcode, int vx) implements Instruction {}
    record AddToI(int rawOpcode, int vx) implements Instruction {}
    record LoadSmallFont(int rawOpcode, int vx) implements Instruction {}
    record LoadBigFont(int rawOpcode, int vx) implements Instruction {}
    record StoreBcd(int rawOpcode, int vx) implements Instruction {}
    record StoreRegisters(int rawOpcode, int maxRegister) implements Instruction {}
    record LoadRegisters(int rawOpcode, int maxRegister) implements Instruction {}
    record SaveRplFlags(int rawOpcode, int maxRegister) implements Instruction {}
    record LoadRplFlags(int rawOpcode, int maxRegister) implements Instruction {}
    record Unknown(int rawOpcode) implements Instruction {}
}
