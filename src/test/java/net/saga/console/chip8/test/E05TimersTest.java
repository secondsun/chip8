package net.saga.console.chip8.test;

import net.saga.console.chip8.Chip8;
import net.saga.console.chip8.util.Chip8Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author summers
 */
public class E05TimersTest {

    private Chip8 chip8;

    @BeforeEach
    public void setUp() throws IOException {
        this.chip8 = Chip8Utils.createFromRom(getClass().getResource("/E05TimerLoop.ch8"));
        this.chip8.execute(0x6064);
        this.chip8.execute(0x6127);
        this.chip8.execute(0x6212);
        this.chip8.execute(0x63AE);
        this.chip8.execute(0x64FF);
        this.chip8.execute(0x65B4);
        this.chip8.execute(0x6642);
        this.chip8.execute(0x6F25);
    }

    /**
     * As documented in E03, we will modify the cycle method to count down the
     * delay timer.
     *
     * There are also timer instructions to set the timer to begin a countdown.
     *
     * FX15 Set the timer to VX FX07 Store the value of the timer to VX
     *
     * The timer works on a 60 hertz cycle. IE if you set the timer to 60 it
     * will countdown to 0 over the course of a second.
     *
     */
    @Test
    public void testDelayTimerOpcodes() {
        chip8.execute(0xF015); //Set timer to 0x64
        chip8.execute(0xF107); //Read timer into V1
        assertEquals(0x64, chip8.getV1());

    }

    @Test
    @Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
    public void testDelayTimerCountdown() {
        while (chip8.getV5() != 255) {
            chip8.cycle();
        }
    }

    /**
     * There is a second timer, the sound timer. The sound timer will emit a
     * tone until it reaches 0. It operates on the same cycle as the delay
     * timer.
     *
     * Opcodes : 0xFX18 Set the sound timer to the value in VX
     *
     */
    @Test
    public void testSoundTimer() {
        chip8.execute(0xF018); //Set timer to 0x64
        chip8.cycle();
    }

    /**
     * This test will test that sound is actually emitted.
     *
     * It is disabled by default because it could be annoying.
     *
     * @throws java.io.IOException loading the program may throw an ioexception.
     */
    @Disabled
    @Test
            @Timeout(value = 110000, unit = TimeUnit.MILLISECONDS)
    public void testEmitSoundTimer() throws IOException {

        Chip8 soundChip = Chip8Utils.createFromRom(getClass().getResource("/E05SoundLoop.ch8"));
        while (soundChip.getV5() != 255) {
            soundChip.cycle();
        }

    }

}
