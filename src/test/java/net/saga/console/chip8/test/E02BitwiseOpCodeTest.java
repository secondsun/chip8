package net.saga.console.chip8.test;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.saga.console.chip8.Chip8;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertEquals;

/**
 *
 * In E01 We set up the Chip-8 processor and implemented the basic arithmetic
 * opcodes. In this section we will implement bitwise opcodes.
 *
 * These include AND, OR, XOR, and shifts.
 *
 * In this tests we will initialize the registers to the following values : V0 =
 * 0x64 V1 = 0x27 V2 = 0x12 V3 = 0xAE V4 = 0xFF V5 = 0xB4 V6 = 0x42 VF = 0x25
 *
 * All other registers will be 0.
 *
 * Additionally we will be using reflection to control the random values
 * generated by java.
 *
 */
public class E02BitwiseOpCodeTest {

    private Chip8 chip8;

    public E02BitwiseOpCodeTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        this.chip8 = new Chip8();
        controlRandom();
        this.chip8.execute(0x6064);
        this.chip8.execute(0x6127);
        this.chip8.execute(0x6212);
        this.chip8.execute(0x63AE);
        this.chip8.execute(0x64FF);
        this.chip8.execute(0x65B4);
        this.chip8.execute(0x6642);
        this.chip8.execute(0x6F25);
    }

    @After
    public void tearDown() {
    }

    /**
     * 8XY2	Set VX to VX & VY
     */
    @Test
    public void testAndOpCodes() {
        chip8.execute(0x8012); // v0 = 0x64 & 0x27
        assertEquals(36, chip8.getV0());
        assertEquals(0x27, chip8.getV1());

        chip8.execute(0x8232); // v2 = 0x12 & 0xAE
        assertEquals(2, chip8.getV2());
        assertEquals(0xAE, chip8.getV3());

        chip8.execute(0x8FE2); // 0x25 & 0x0
        assertEquals(0, chip8.getVF());

    }

    /**
     * 8XY1	Set VX to VX | VY
     */
    @Test
    public void testOROpCodes() {
        chip8.execute(0x8011); // v0 = 0x64 | 0x27
        assertEquals(103, chip8.getV0());
        assertEquals(0x27, chip8.getV1());

        chip8.execute(0x8231); // v2 = 0x12 | 0xAE
        assertEquals(190, chip8.getV2());
        assertEquals(0xAE, chip8.getV3());

        chip8.execute(0x8FE1); // 0x25 | 0x0
        assertEquals(0x25, chip8.getVF());

    }

    /**
     * 8XY3	Set VX to VX ^ VY
     */
    @Test
    public void testXOROpCodes() {
        chip8.execute(0x8013); // v0 = 0x64 ^ 0x27
        assertEquals(67, chip8.getV0());
        assertEquals(0x27, chip8.getV1());

        chip8.execute(0x8233); // v2 = 0x12 ^ 0xAE
        assertEquals(188, chip8.getV2());
        assertEquals(0xAE, chip8.getV3());

        chip8.execute(0x8FE3); // 0x25 ^ 0x0
        assertEquals(0x25, chip8.getVF());

    }

    /**
     * 8XY6	Shift VX Right one place and store the result in VX. Also store the
     * lest significant bit in VF
     */
    @Test
    public void testShiftRight() {
        chip8.execute(0x8016); // v0 = 0x27 >> 1; xF = 0x1
        assertEquals(0x32, chip8.getV0());
        assertEquals(0x0, chip8.getVF());

        chip8.execute(0x8236); // v2 = 0xAE >> 1; VF = 0x0
        assertEquals(0x09, chip8.getV2());
        assertEquals(0x0, chip8.getVF());

        chip8.execute(0x8446); // V4 = 0xFF >> 1; VF = 0x1;
        assertEquals(127, chip8.getV4());
        assertEquals(0x1, chip8.getVF());

    }

    /**
     * 8XYE	Shift VX left one place and store the result in VX. Also store the
     * most significant bit in VF
     */
    @Test
    public void testShiftLeft() {
        chip8.execute(0x801E); // v0 = 0x27 << 1; xF = 0x1
        assertEquals(200, chip8.getV0());
        assertEquals(0x0, chip8.getVF());

        chip8.execute(0x823E); // v2 = 0xAE << 1; VF = 0x0
        assertEquals(36, chip8.getV2());
        assertEquals(0x0, chip8.getVF());

        chip8.execute(0x844E); // V4 = 0xFF << 1; VF = 0x1;
        assertEquals(254, chip8.getV4());
        assertEquals(0x1, chip8.getVF());

    }

    /**
     * CXNN Generate a Random number, mask it with NN and store the results in
     * Vx.
     */
    @Test
    public void testRandom() {
        //230, 198,153, 29
        chip8.execute(0xC1FF); // V1 = 230 & 0xFF
        assertEquals(230, chip8.getV1());
        
        chip8.execute(0xC23E); // v2 = 198 & 0x3E
        assertEquals(6, chip8.getV2());
        
        chip8.execute(0xC44E); // V4 = 153 & 0x4E
        assertEquals(8, chip8.getV4());
        

    }

    private void controlRandom() {
        try {
            Field field = chip8.getClass().getDeclaredField("random");
            field.setAccessible(true);
            field.set(chip8, new Random(42));
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

}
