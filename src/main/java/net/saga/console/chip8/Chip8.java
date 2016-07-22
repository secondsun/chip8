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

import java.util.Random;

/**
 * This class is the main chip8 system
 */
public class Chip8 {

    private int pc = 0x200;
    private int i = 0;
    private int[] registers = new int[0x10];
    private final Random random = new Random();
    
    public int getPC() {
        return pc;
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

    public void execute(int i) {
        final int high = (i & 0xF000) >> 12;

        switch (high) {
            case 6: {//6XNN	Store number NN in register VX
                int low = 0x0FF & i;
                int register = (i & 0x0f00) >> 8;
                registers[register] = low;
            }
            break;
            case 7: { //7XNN	Adds number NN to register VX
                int low = 0x0FF & i;
                int register = (i & 0x0f00) >> 8;
                registers[register] += low;
            }
            break;
            case 0xC: { //7XNN	Mask a random and  number NN to register VX
                int low = 0x0FF & i;
                int register = (i & 0x0f00) >> 8;
                registers[register] = random.nextInt(0xFF) & low;
            }
            break;
            case 8: {
                int low = 0x00F & i;
                int registerX = (i & 0x0F00) >> 8;
                int registerY = (i & 0x00F0) >> 4;

                switch (low) {
                    case 0: {
                        registers[registerX] = registers[registerY];
                    }
                    break;
                    case 1: {
                        registers[registerX] |= registers[registerY];
                    }
                    break;
                    
                    case 2: {
                        registers[registerX] &= registers[registerY];
                    }
                    break;
                    case 3: {
                        registers[registerX] ^= registers[registerY];
                    }
                    break;
                    case 4: {
                        registers[registerX] += registers[registerY];
                        registers[0xf] = (registers[registerX] ) >> 8 != 0?1:0;
                    } break;
                    case 5: {
                        registers[registerX] = registers[registerX] - registers[registerY];
                        registers[0xf] = (registers[registerX] ) >> 8 != 0?1:0;
                    } break;
                    case 6: {
                        registers[0xf] = registers[registerY] & 0x1;
                        registers[registerX] = registers[registerY] >> 1;
                    } break;
                    case 0xE: {
                        registers[0xf] = (registers[registerY] ) >> 7;
                        registers[registerX] = registers[registerY] << 1;
                    } break;
                    case 7: {
                        registers[registerX] = registers[registerY] - registers[registerX];
                        registers[0xf] = (registers[registerX] ) >> 8 != 0?1:0;
                    } break;
                    default:
                        throw new UnsupportedOperationException("Unsupported opcode.");
                }

            }
            break;
            default:
                throw new UnsupportedOperationException("Unsupported opcode."); //To change body of generated methods, choose Tools | Templates.

        }

    }

}
