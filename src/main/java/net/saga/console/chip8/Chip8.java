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
        loadFont(memory);
    }

    public Chip8(byte[] memory) {
        if (memory.length > 4096) {
            throw new IllegalArgumentException("Memory may not be greater than 4096 bytes");
        }
        this.memory = memory;
        loadFont(memory);
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
        final int high = (instruction & 0xF000);

        switch (high) {
            case 0x1000: {//1NNN Jump to NNN
                int low = 0x0FFF & instruction;
                pc = low;

            }
            break;
            case 0x2000: {//2NNN start subroutine at NNN
                stack[sp] = pc;
                sp++;
                int low = 0x0FFF & instruction;
                pc = low;

            }
            break;
            case 0x3000: {//3XNN Skip if Vx = NN
                int low = 0x0FF & instruction;
                int register = (instruction & 0x0f00) >> 8;
                if ((getVX(register)) == low) {
                    pc += 0x2;
                }
            }
            break;
            case 0x5000: {//5XY0 Skip if Vx = Vy
                int registery = (instruction & 0x00f0) >> 4;
                int registerx = (instruction & 0x0f00) >> 8;
                if (getVX(registerx) == getVX(registery)) {
                    pc += 0x2;
                }
            }
            break;
            case 0x4000: {//$XNN Skip if Vx != NN
                int low = 0x0FF & instruction;
                int register = (instruction & 0x0f00) >> 8;
                if (getVX(register) != low) {
                    pc += 0x2;
                }
            }
            break;
            case 0x9000: {//9XY0 Skip if Vx != Vy
                int registery = (instruction & 0x00f0) >> 4;
                int registerx = (instruction & 0x0f00) >> 8;
                if (getVX(registerx) != getVX(registery)) {
                    pc += 0x2;
                }
            }
            break;
            case 0x0000: {
                switch (instruction) {
                    case 0x00EE:
                        sp--;
                        pc = stack[sp];
                        break;
                    case 0x00E0:
                        video = new byte[video.length];
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported opcode:" + Integer.toHexString(instruction));
                }
            }
            break;
            case 0xF000: {
                int low = instruction & 0xFF;
                int register = (instruction & 0x0F00) >> 8;

                switch (low) {
                    case 0x15:
                        delayTimer = getVX(register);
                        break;
                    case 0x65: {
                        byte maxRegister = (byte) register;
                        for (int i = 0; i <= maxRegister; i++) {
                            setVX(memory[iRegister], i);
                            iRegister++;
                        }
                        }break;
                    case 0x55:
                        byte maxRegister = (byte) register;
                        for (int i = 0; i <= maxRegister; i++) {
                            memory[iRegister] = (byte) getVX(i);
                            iRegister++;
                        }
                        break;
                    case 0x18:
                        soundTimer = getVX(register);
                        break;
                    case 0x0A:
                        if (Input.read() == -1) {
                            pc -= 0x2;
                        } else {
                            setVX(Input.read(), register);
                        }
                        break;
                    case 0x07:
                        setVX(delayTimer, register);
                        break;
                    case 0x29:
                        iRegister = getCharacterAddress(getVX(register));
                        break;
                    case 0x33:
                        int value = getVX(register);
                        memory[iRegister] = (byte) (value / 100);
                        memory[iRegister + 1] = (byte) (((value) % 100) / 10);
                        memory[iRegister + 2] = (byte) (((value) % 100) % 10);
                        break;
                    case 0x1E:
                        iRegister += getVX(register);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported opcode:" + Integer.toHexString(instruction));
                }
            }
            break;

            case 0xB000: {//BNNN Jump to NNN + V0
                int low = 0x0FFF & instruction;
                pc = low + getVX(0);

            }
            break;
            case 0x6000: {//6XNN	Store number NN in register VX
                int low = 0x0FF & instruction;
                int register = (instruction & 0x0f00) >> 8;
                setVX(low, register);
            }
            break;
            case 0x7000: { //7XNN	Adds number NN to register VX
                int low = 0x0FF & instruction;
                int register = (instruction & 0x0f00) >> 8;
                setVX(getVX(register) + low, register);
            }
            break;
            case 0xC000: { //7XNN	Mask a random and  number NN to register VX
                int low = 0x0FF & instruction;
                int register = (instruction & 0x0f00) >> 8;
                setVX(random.nextInt(0xFF) & low, register);
            }
            break;
            case 0xE000: { //EXop Skips based on keyboard input
                int low = 0x0FF & instruction;
                int register = (instruction & 0x0f00) >> 8;

                switch (low) {
                    case 0x9E:
                        //skip if register == input
                        if (getVX(register) == Input.read()) {
                            pc += 0x2;
                        }
                        break;
                    case 0xA1:
                        //skip if register != input
                        if (getVX(register) != Input.read()) {
                            pc += 0x2;
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported opcode:" + Integer.toHexString(instruction));
                }

            }
            break;
            case 0x8000: {
                int low = 0x00F & instruction;
                int registerX = (instruction & 0x0F00) >> 8;
                int registerY = (instruction & 0x00F0) >> 4;

                switch (low) {
                    case 0: {
                        setVX(getVX(registerY), registerX);
                    }
                    break;
                    case 1: {
                        setVX(getVX(registerY) | getVX(registerX), registerX);
                    }
                    break;

                    case 2: {
                        setVX(getVX(registerY) & getVX(registerX), registerX);
                    }
                    break;
                    case 3: {
                        setVX(getVX(registerY) ^ getVX(registerX), registerX);
                    }
                    break;
                    case 4: {
                        int sum = getVX(registerX) + getVX(registerY);
                        registers[0xf] = sum > 0xFF?1:0;
                        setVX(sum, registerX);                        
                    }
                    break;
                    case 5: {
                        int difference = getVX(registerX) - getVX(registerY);
                        registers[0xf] = difference > 0 ? 1 : 0;
                        setVX(difference, registerX);                        
                        
                    }
                    break;
                    case 6: {
                        registers[0xf] = getVX(registerX) & 0x01;
                        setVX(getVX(registerX) >> 1, registerX);
                    }
                    break;
                    case 0xE: {
                        registers[0xf] = (getVX(registerX) >> 7) & 0x01;
                        setVX(getVX(registerX) << 1, registerX);
                    }
                    break;
                    case 7: {
                        int difference = getVX(registerY) - getVX(registerX);
                        registers[0xf] = difference > 0 ? 1 : 0;
                        setVX(difference, registerX);           
                    }
                    break;
                    default:
                        throw new UnsupportedOperationException("Unsupported opcode:" + Integer.toHexString(instruction));
                }

            }
            break;
            case 0xA000: {
                int low = 0x0FFF & instruction;
                iRegister = low;
            }
            break;
            case 0xD000: {
                int lines = 0x00F & instruction;
                int registerX = (instruction & 0x0F00) >> 8;
                int registerY = (instruction & 0x00F0) >> 4;

                int x = getVX(registerX);
                int y = getVX(registerY);
                registers[0xF] = 0;
                for (int count = 0; (count < lines) ; count++) {
                    byte oldVideo = getSpriteRow(x, y + count);
                    writeVideo(x, y + count, memory[iRegister + count]);
                    registers[0xF] = (((byte)memory[iRegister + count] & oldVideo)) == 0 ? ((byte)registers[0xF]) : 1;
                }

            }
            break;
            default:
                throw new UnsupportedOperationException("Unsupported opcode:" + Integer.toHexString(instruction));

        }

    }

    public void cycle() {
        int instruction = ((memory[pc++] << 8) & 0xFF00) | (memory[pc++] & 0xFF);
        long time = System.currentTimeMillis();
        if (time > nextTimer) {
            countDownTimers();
            nextTimer = time + (1000 / 60);
        }
        execute(instruction);
    }

    private void countDownTimers() {
        if (delayTimer > 0) {
            delayTimer--;
        }
        if (soundTimer > 0) {
            soundTimer--;
            Audio.play();
        } else {
            Audio.stop();
        }
    }

    public int getiRegister() {
        return iRegister;
    }

    public byte[] getScreen() {
        return video;
    }

    /**
     *
     * Packs a graphics row (8 pixels of the sprite) into a btye
     *
     * @param x
     * @param y
     * @return
     */
    private byte getSpriteRow(int x, int y) {
        x = x % 64;
        y = y % 32;
        byte byte1 = video[x + y * 64];
        byte byte2 = video[(x + 1) % 64 + y * 64];
        byte byte3 = video[(x + 2) % 64 + y * 64];
        byte byte4 = video[(x + 3) % 64 + y * 64];
        byte byte5 = video[(x + 4) % 64 + y * 64];
        byte byte6 = video[(x + 5) % 64 + y * 64];
        byte byte7 = video[(x + 6) % 64 + y * 64];
        byte byte8 = video[(x + 7) % 64 + y * 64];

        return (byte) ((byte1 << 7)
                | (byte2 << 6)
                | (byte3 << 5)
                | (byte4 << 4)
                | (byte5 << 3)
                | (byte6 << 2)
                | (byte7 << 1)
                | (byte8));

    }

    private void writeVideo(int x, int y, byte b) {
        x = x % 64;
        y = y % 32;
        video[(x) + (y) * 64] ^= (byte) ((b & 0b10000000) >> 7);
        video[(1 + (x)) % 64 + (y) * 64] ^= (byte) ((b & 0b01000000) >> 6);
        video[(2 + (x)) % 64 + (y) * 64] ^= (byte) ((b & 0b00100000) >> 5);
        video[(3 + (x)) % 64  + (y) * 64] ^= (byte) ((b & 0b00010000) >> 4);
        video[(4 + (x)) % 64  + (y) * 64] ^= (byte) ((b & 0b00001000) >> 3);
        video[(5 + (x)) % 64  + (y) * 64] ^= (byte) ((b & 0b00000100) >> 2);
        video[(6 + (x)) % 64  + (y) * 64] ^= (byte) ((b & 0b00000010) >> 1);
        video[(7 + (x)) % 64  + (y) * 64] ^= (byte) ((b & 0b00000001));
    }

    private int getCharacterAddress(int digit) {
        if (((0xff)&digit) > 0xf) {
            throw new IllegalArgumentException(digit + " is not a vlaid character");
        }
        return 5 * digit;
    }

    private void loadFont(byte[] memory) {
        int i = 0;

        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x90;
        memory[i++] = (byte) 0x90;
        memory[i++] = (byte) 0x90;
        memory[i++] = (byte) 0xF0;

        memory[i++] = (byte) 0x20;
        memory[i++] = (byte) 0x60;
        memory[i++] = (byte) 0x20;
        memory[i++] = (byte) 0x20;
        memory[i++] = (byte) 0x70;

        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x10;
        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x80;
        memory[i++] = (byte) 0xF0;

        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x10;
        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x10;
        memory[i++] = (byte) 0xF0;

        memory[i++] = (byte) 0x90;
        memory[i++] = (byte) 0x90;
        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x10;
        memory[i++] = (byte) 0x10;

        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x80;
        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x10;
        memory[i++] = (byte) 0xF0;

        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x80;
        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x90;
        memory[i++] = (byte) 0xF0;

        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x10;
        memory[i++] = (byte) 0x20;
        memory[i++] = (byte) 0x40;
        memory[i++] = (byte) 0x40;

        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x90;
        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x90;
        memory[i++] = (byte) 0xF0;

        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x90;
        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x10;
        memory[i++] = (byte) 0xF0;

        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x90;
        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x90;
        memory[i++] = (byte) 0x90;

        memory[i++] = (byte) 0xE0;
        memory[i++] = (byte) 0x90;
        memory[i++] = (byte) 0xE0;
        memory[i++] = (byte) 0x90;
        memory[i++] = (byte) 0xE0;

        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x80;
        memory[i++] = (byte) 0x80;
        memory[i++] = (byte) 0x80;
        memory[i++] = (byte) 0xF0;

        memory[i++] = (byte) 0xE0;
        memory[i++] = (byte) 0x90;
        memory[i++] = (byte) 0x90;
        memory[i++] = (byte) 0x90;
        memory[i++] = (byte) 0xE0;

        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x80;
        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x80;
        memory[i++] = (byte) 0xF0;

        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x80;
        memory[i++] = (byte) 0xF0;
        memory[i++] = (byte) 0x80;
        memory[i++] = (byte) 0x80;

    }

    public byte[] getMemory() {
        return Arrays.copyOf(memory, memory.length);
    }

    public int getSP() {
        return sp;
    }

}
