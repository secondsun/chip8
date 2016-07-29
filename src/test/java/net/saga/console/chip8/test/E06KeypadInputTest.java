package net.saga.console.chip8.test;

import java.io.IOException;
import net.saga.console.chip8.Chip8;
import net.saga.console.chip8.Chip8Utils;
import net.saga.console.chip8.Input;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * Chip-8 has support for a 16 button keypad input. The keys are assigned values
 * from 0x0 - 0xF. The mapping from your keyboard/input device to chip8
 * shouldn't matter for these tests.
 */
public class E06KeypadInputTest {

    private Chip8 chip8;

    @Before
    public void setUp() throws IOException {
        this.chip8 = Chip8Utils.createFromRom(getClass().getResource("/E06KeypadLoop.ch8"));
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
    public void unpress() {
        Input.unpress();
    }

    /**
     * The opcode FX0A will wait for a key press and store its value into
     * register VX.
     * 
     * This test will execute the opcode and then call cycle several times.  As 
     * long as the program counter does not increment we will assume that chip8
     * is not executing.
     * 
     */
    @Test
    public void testChip8WaitsForKeyboardInput() {
        int pc = chip8.getPC();
        chip8.cycle();
        chip8.cycle();
        chip8.cycle();
        chip8.cycle();
        assertEquals(pc, chip8.getPC());
    }

    @Test
    public void testChip8ContinuesAfterKeyboardInput() {
        int pc = chip8.getPC();
        chip8.cycle();
        chip8.cycle();
        assertEquals(pc, chip8.getPC());
        
        Input.press(0xA);
        chip8.cycle();
        chip8.cycle();
        assertEquals(0xA, chip8.getV6());
        
    }
    
    /**
     * The next opcode will skip the next instruction until the keypress matches 
     * a value in a certain register.
     * 
     * EX9E : Skip the next instruction of the key corresponding to the value in 
     * VX is pressed.
     * 
     */
    @Test
    public void skipIfPressed() {
        Input.press(0x1);
        chip8.execute(0x6002);//Store 0x02 into V0
        chip8.execute(0xE09E);//Skip if 0x02 is pressed (it isn't)
        assertEquals(0x200, chip8.getPC());
        
        Input.press(0x2);
        chip8.execute(0x6002);//Store 0x02 into V0
        chip8.execute(0xE09E);//Skip if 0x02 is pressed (it is)
        assertEquals(0x202, chip8.getPC());
        
    }
    
    /**
     * The next opcode will not skip the next instruction if the keypress 
     * matches a value in a certain register.
     * 
     * EXA1 : Skip the next instruction of the key corresponding to the value in 
     * VX is not pressed.
     * 
     */
    @Test
    public void skipIfNotPressed() {
        Input.press(0x1);
        chip8.execute(0x6002);//Store 0x02 into V0
        chip8.execute(0xE0A1);//Skip if 0x02 is not pressed (it isn't)
        assertEquals(0x202, chip8.getPC());
        
        Input.press(0x2);
        chip8.execute(0x6002);//Store 0x02 into V0
        chip8.execute(0xE0A1);//Skip if 0x02 is pressed (it is)
        assertEquals(0x202, chip8.getPC());
    }
    
    
}
