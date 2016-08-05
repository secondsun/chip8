/*
 * The MIT License
 *
 * Copyright 2016 summers.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.saga.console.chip8;

import net.saga.console.chip8.util.Input;
import net.saga.console.chip8.util.Audio;
import java.util.Arrays;
import java.util.Random;

/**
 * This class is the main chip8 system
 */
public class Chip8 {

    private int pc = 0x200;
    private int iRegister = 0;
    private final int[] registers = new int[0x10];
    private final Random random = new Random();
    private int sp = 0;
    private int delayTimer = 0;
    private int soundTimer = 0;
    private final int[] stack = new int[16];
    private final byte[] memory;
    private byte[] video = new byte[64 * 32];
    private long nextTimer = 0;

    public Chip8() {
        this.memory = new byte[4096];
    }

    public Chip8(byte[] memory) {
        if (memory.length > 4096) {
            throw new IllegalArgumentException("Memory may not be greater than 4096 bytes");
        }
        this.memory = memory;
    }

    public int getPC() {
        return pc;
    }

    private void setVX(int value, int register) {
         registers[register] = (0x000000FF & value);
    }

    private int getVX(int x) {
         return 0x000000FF & registers[x];
    }
    
    public int getV0() {
        return registers[0] & 0xFF;
    }

    public int getV1() {
        return registers[1] & 0xFF;
    }

    public int getV2() {
        return registers[0x2] & 0xFF;
    }

    public int getV3() {
        return registers[0x3] & 0xFF;
    }

    public int getV4() {
        return registers[0x4] & 0xFF;
    }

    public int getV5() {
        return registers[0x5] & 0xFF;
    }

    public int getV6() {
        return registers[0x6] & 0xFF;
    }

    public int getV7() {
        return registers[0x7] & 0xFF;
    }

    public int getV8() {
        return registers[0x8] & 0xFF;
    }

    public int getV9() {
        return registers[0x9] & 0xFF;
    }

    public int getVA() {
        return registers[0xa] & 0xFF;
    }

    public int getVB() {
        return registers[0xb] & 0xFF;
    }

    public int getVC() {
        return registers[0xc] & 0xFF;
    }

    public int getVD() {
        return registers[0xd] & 0xFF;
    }

    public int getVE() {
        return registers[0xe] & 0xFF;
    }

    public int getVF() {
        return registers[0xf] & 0xFF;
    }

    public void execute(int instruction) {
        throw new UnsupportedOperationException("Unsupported opcode:" + Integer.toHexString(instruction));
    }

    public void cycle() {
        
    }

    public int getiRegister() {
        return iRegister;
    }

    public byte[] getScreen() {
        return Arrays.copyOf(video, video.length);
    }

    public byte[] getMemory() {
        return Arrays.copyOf(memory, memory.length);
    }

    public int getSP() {
        return sp;
    }

}
