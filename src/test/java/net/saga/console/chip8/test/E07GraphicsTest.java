package net.saga.console.chip8.test;

import java.io.IOException;
import net.saga.console.chip8.Chip8;
import net.saga.console.chip8.util.Chip8Utils;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 * This last test class covers graphics. There are three main parts of graphics
 * : The I Register, Fonts, and Sprites.
 */
public class E07GraphicsTest {

    private Chip8 chip8;

    @Before
    public void setUp() throws IOException {
        //Graphics ROM has the sprite FF3C stored starting at 202
        // This is the following two row sprite
        //
        //  ********
        //    ****  
        //
        //Executed the program just infinitely loops
        this.chip8 = Chip8Utils.createFromRom(getClass().getResource("/E07GraphicsRom.ch8"));
        this.chip8.execute(0x6064);
        this.chip8.execute(0x6127);
        this.chip8.execute(0x6212);
        this.chip8.execute(0x63AE);
        this.chip8.execute(0x64FF);
        this.chip8.execute(0x65B4);
        this.chip8.execute(0x6642);
        this.chip8.execute(0x673F);
        this.chip8.execute(0x681F);
        this.chip8.execute(0x6F25);
    }

    /**
     * The I Register holds memory addresses. A program can not read the value
     * currently set in the I register but may only set it.
     *
     * There are two opcodes for the IRegister : ANNN : Stores in the I register
     * the address 0xNNN FX1E : Adds to the value in the I-Register the value of
     * VX.
     */
    @Test
    public void testIRegister() {
        this.chip8.execute(0xA123);
        assertEquals(0x123, chip8.getiRegister());

        this.chip8.execute(0xF11E);
        assertEquals(0x123 + 0x27, chip8.getiRegister());

    }

    /**
     *
     * Sprints in chip8 are represented in memory as columns of 8 bits with a
     * variable number of rows. The 8 bits each correspond to a pixel with 1
     * being toggled and 0 being transparent. The sprites are accessed starting
     * at the memory address pointed to by the I register.
     *
     * Sprites are XOR drawn. This means that a 1 will flip that particular
     * pixel. If a pixel is unset by this operation then the register VF is set
     * to 1. Otherwise it is 0.
     *
     * DXYN : Draw a Sprite of N rows at position VX,VY with the data pointed to
     * by the I register.
     *
     *
     */
    @Test
    public void drawSprite() {
        chip8.execute(0xA202);
        chip8.execute(0xD122); // Draw Sprite at 39, 18
        assertEquals(0, chip8.getVF());

        byte[] video = chip8.getScreen();
        assertEquals(1, video[39 + 64 * 18]);
        assertEquals(1, video[40 + 64 * 18]);
        assertEquals(1, video[41 + 64 * 18]);
        assertEquals(1, video[42 + 64 * 18]);
        assertEquals(1, video[43 + 64 * 18]);
        assertEquals(1, video[44 + 64 * 18]);
        assertEquals(1, video[45 + 64 * 18]);
        assertEquals(1, video[46 + 64 * 18]);

        assertEquals(0, video[39 + 64 * 19]);
        assertEquals(0, video[40 + 64 * 19]);
        assertEquals(1, video[41 + 64 * 19]);
        assertEquals(1, video[42 + 64 * 19]);
        assertEquals(1, video[43 + 64 * 19]);
        assertEquals(1, video[44 + 64 * 19]);
        assertEquals(0, video[45 + 64 * 19]);
        assertEquals(0, video[46 + 64 * 19]);

        chip8.execute(0xD122); // Draw Sprite at 39,18
        assertEquals(1, chip8.getVF());

        video = chip8.getScreen();
        assertEquals(0, video[39 + 64 * 18]);
        assertEquals(0, video[40 + 64 * 18]);
        assertEquals(0, video[41 + 64 * 18]);
        assertEquals(0, video[42 + 64 * 18]);
        assertEquals(0, video[43 + 64 * 18]);
        assertEquals(0, video[44 + 64 * 18]);
        assertEquals(0, video[45 + 64 * 18]);
        assertEquals(0, video[46 + 64 * 18]);

        assertEquals(0, video[39 + 64 * 19]);
        assertEquals(0, video[40 + 64 * 19]);
        assertEquals(0, video[41 + 64 * 19]);
        assertEquals(0, video[42 + 64 * 19]);
        assertEquals(0, video[43 + 64 * 19]);
        assertEquals(0, video[44 + 64 * 19]);
        assertEquals(0, video[45 + 64 * 19]);
        assertEquals(0, video[46 + 64 * 19]);

    }

    /**
     * Sprites wrap around on their axis. IE If you draw to X 65 it will wrap to
     * position 1.
     */
    @Test
    public void drawSpriteWrap() {
        chip8.execute(0xA202);
        chip8.execute(0xD212); // Draw Sprite at 18, 7 (39 wraps to 7)
        assertEquals(0, chip8.getVF());

        byte[] video = chip8.getScreen();
        assertEquals(1, video[18 + 64 * 7]);
        assertEquals(1, video[19 + 64 * 7]);
        assertEquals(1, video[20 + 64 * 7]);
        assertEquals(1, video[21 + 64 * 7]);
        assertEquals(1, video[22 + 64 * 7]);
        assertEquals(1, video[23 + 64 * 7]);
        assertEquals(1, video[24 + 64 * 7]);
        assertEquals(1, video[25 + 64 * 7]);

        assertEquals(0, video[18 + 64 * 8]);
        assertEquals(0, video[19 + 64 * 8]);
        assertEquals(1, video[20 + 64 * 8]);
        assertEquals(1, video[21 + 64 * 8]);
        assertEquals(1, video[22 + 64 * 8]);
        assertEquals(1, video[23 + 64 * 8]);
        assertEquals(0, video[24 + 64 * 8]);
        assertEquals(0, video[25 + 64 * 8]);

    }

    /**
     * Sprites wrap around on their axis. IE If you draw to X 65 it will wrap to
     * position 1.
     */
    @Test
    public void drawSpriteBottomRightEdgeTests() {
        chip8.execute(0xA202);
        chip8.execute(0xD781); 
        chip8.execute(0xD871); 
        chip8.execute(0xD781); 
        chip8.execute(0xD871); 
    }
    
    /**
     * The opcode 00E0 clears the screen.
     *
     */
    @Test
    public void testClearVideo() {
        chip8.execute(0xA202);
        chip8.execute(0xD212); // Draw Sprite at 18, 7 (39 wraps to 7)
        chip8.execute(0x00E0);

        byte[] video = chip8.getScreen();

        assertEquals(0, video[18 + 64 * 7]);
        assertEquals(0, video[19 + 64 * 7]);
        assertEquals(0, video[20 + 64 * 7]);
        assertEquals(0, video[21 + 64 * 7]);
        assertEquals(0, video[22 + 64 * 7]);
        assertEquals(0, video[23 + 64 * 7]);
        assertEquals(0, video[24 + 64 * 7]);
        assertEquals(0, video[25 + 64 * 7]);

    }

    /**
     * Chip 8 has build in hexedecimal fonts. These fonts are stored in the 200
     * bytes of reserved data and accessed using a special opcode.
     *
     * FX29 : Set the I register to the address of the sprite corresponding to
     * the hex digit stored in VX.
     */
    @Test
    public void testHexFonts() {

        chip8.execute(0x6100); //Store 00 in V1
        chip8.execute(0x6200); //Store 00 in V2

        for (int i = 0; i <= 0xf; i++) {
            chip8.execute(0x6000 + i); //Store 00 in V0
            chip8.execute(0xF029); // Set I to the memory address of the font sprite in V0
            chip8.execute(0xD125); // Draw all 5 lines of the sprite at 0, 0.

            checkGraphics(i);//Test for being drawn.
            chip8.execute(0x00E0); // Clear Screen

        }

    }

    private void checkGraphics(int i) {
        byte[] video = chip8.getScreen();
        switch (i) {
            case 0:
                assertEquals((byte)0xF0, (byte)Chip8Utils.getSpriteRow(0, 0, video));
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 1, video));
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 2, video));
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 3, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 4, video));
                break;
            case 1:
                assertEquals((byte)0x20, Chip8Utils.getSpriteRow(0, 0, video));
                assertEquals((byte)0x60, Chip8Utils.getSpriteRow(0, 1, video));
                assertEquals((byte)0x20, Chip8Utils.getSpriteRow(0, 2, video));
                assertEquals((byte)0x20, Chip8Utils.getSpriteRow(0, 3, video));
                assertEquals((byte)0x70, Chip8Utils.getSpriteRow(0, 4, video));
                break;
            case 2:
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 0, video));
                assertEquals((byte)0x10, Chip8Utils.getSpriteRow(0, 1, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 2, video));
                assertEquals((byte)0x80, Chip8Utils.getSpriteRow(0, 3, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 4, video));
                break;
            case 3:
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 0, video));
                assertEquals((byte)0x10, Chip8Utils.getSpriteRow(0, 1, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 2, video));
                assertEquals((byte)0x10, Chip8Utils.getSpriteRow(0, 3, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 4, video));
                break;
            case 4:
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 0, video));
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 1, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 2, video));
                assertEquals((byte)0x10, Chip8Utils.getSpriteRow(0, 3, video));
                assertEquals((byte)0x10, Chip8Utils.getSpriteRow(0, 4, video));
                break;
            case 5:
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 0, video));
                assertEquals((byte)0x80, Chip8Utils.getSpriteRow(0, 1, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 2, video));
                assertEquals((byte)0x10, Chip8Utils.getSpriteRow(0, 3, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 4, video));
                break;
            case 6:
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 0, video));
                assertEquals((byte)0x80, Chip8Utils.getSpriteRow(0, 1, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 2, video));
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 3, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 4, video));
                break;
            case 7:
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 0, video));
                assertEquals((byte)0x10, Chip8Utils.getSpriteRow(0, 1, video));
                assertEquals((byte)0x20, Chip8Utils.getSpriteRow(0, 2, video));
                assertEquals((byte)0x40, Chip8Utils.getSpriteRow(0, 3, video));
                assertEquals((byte)0x40, Chip8Utils.getSpriteRow(0, 4, video));
                break;
            case 8:
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 0, video));
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 1, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 2, video));
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 3, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 4, video));
                break;
            case 9:
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 0, video));
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 1, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 2, video));
                assertEquals((byte)0x10, Chip8Utils.getSpriteRow(0, 3, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 4, video));
                break;
            case 0xA:
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 0, video));
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 1, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 2, video));
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 3, video));
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 4, video));
                break;
            case 0xB:
                assertEquals((byte)0xE0, Chip8Utils.getSpriteRow(0, 0, video));
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 1, video));
                assertEquals((byte)0xE0, Chip8Utils.getSpriteRow(0, 2, video));
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 3, video));
                assertEquals((byte)0xE0, Chip8Utils.getSpriteRow(0, 4, video));
                break;
            case 0xC:
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 0, video));
                assertEquals((byte)0x80, Chip8Utils.getSpriteRow(0, 1, video));
                assertEquals((byte)0x80, Chip8Utils.getSpriteRow(0, 2, video));
                assertEquals((byte)0x80, Chip8Utils.getSpriteRow(0, 3, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 4, video));
                break;
            case 0xD:
                assertEquals((byte)0xE0, Chip8Utils.getSpriteRow(0, 0, video));
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 1, video));
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 2, video));
                assertEquals((byte)0x90, Chip8Utils.getSpriteRow(0, 3, video));
                assertEquals((byte)0xE0, Chip8Utils.getSpriteRow(0, 4, video));
                break;
            case 0xE:
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 0, video));
                assertEquals((byte)0x80, Chip8Utils.getSpriteRow(0, 1, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 2, video));
                assertEquals((byte)0x80, Chip8Utils.getSpriteRow(0, 3, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 4, video));
                break;
            case 0xF:
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 0, video));
                assertEquals((byte)0x80, Chip8Utils.getSpriteRow(0, 1, video));
                assertEquals((byte)0xF0, Chip8Utils.getSpriteRow(0, 2, video));
                assertEquals((byte)0x80, Chip8Utils.getSpriteRow(0, 3, video));
                assertEquals((byte)0x80, Chip8Utils.getSpriteRow(0, 4, video));

                break;
        }
    }

}
