package net.saga.console.chip8.test;


import net.saga.console.chip8.Chip8;
import net.saga.console.chip8.util.Chip8Utils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

public class E09JITTest {

    private Chip8 chip8;

    /**
     * This test takes our example program from E07GraphicsTest and jits it. This program is an infinite loop.
     * @throws IOException
     */
    @Test
    @DisplayName("JIT an infinite loop")
    public void jitProgram() throws IOException {
        this.chip8 = Chip8Utils.createFromRom(getClass().getResource("/E07GraphicsRom.ch8"));
        fail("not implemented");
    }

}
