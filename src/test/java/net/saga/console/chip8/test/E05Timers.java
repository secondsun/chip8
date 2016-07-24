
package net.saga.console.chip8.test;

import net.saga.console.chip8.Chip8;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author summers
 */
public class E05Timers {

    private Chip8 chip8;

    @Before
    public void setUp() {
        this.chip8 = new Chip8();
        this.chip8.execute(0x6064);
        this.chip8.execute(0x6127);
        this.chip8.execute(0x6212);
        this.chip8.execute(0x63AE);
        this.chip8.execute(0x64FF);
        this.chip8.execute(0x65B4);
        this.chip8.execute(0x6642);
        this.chip8.execute(0x6F25);
    }

    @Test
    public void testDelayTimer() {
        throw new IllegalStateException("Not implemented");
    }
    
    @Test
    public void testSoundTimer() {
        throw new IllegalStateException("Not implemented");
    }
    
}
