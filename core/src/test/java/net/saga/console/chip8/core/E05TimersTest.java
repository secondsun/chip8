package net.saga.console.chip8.core;

import java.io.IOException;
import net.saga.console.chip8.core.audio.AudioConfig;
import net.saga.console.chip8.core.audio.AudioEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class E05TimersTest {
    @Test
    void testDelayTimerOpcodesAndFrameCountdown() throws IOException {
        Chip8Machine chip8 = new Chip8Machine(Chip8Config.legacyTutorial());
        try (var stream = getClass().getResourceAsStream("/E05TimerLoop.ch8")) {
            chip8.loadRom(stream.readAllBytes());
        }
        chip8.executeOpcode(0x6064);
        chip8.executeOpcode(0xF015);
        chip8.executeOpcode(0xF107);
        assertEquals(0x64, chip8.getV(1));

        // Reset and run the timer loop ROM deterministically (checking V5 after each instruction)
        chip8.reset();
        int steps = 0;
        while (chip8.getV(5) != 255 && steps < 5_000) {
            if (steps % 10 == 0) {
                chip8.tickTimers60Hz();
            }
            chip8.stepInstruction();
            steps++;
        }
        assertEquals(255, chip8.getV(5));
    }

    @Test
    void testSoundTimerAndAudioSynthesisMath() {
        Chip8Machine chip8 = new Chip8Machine(Chip8Config.legacyTutorial());
        chip8.executeOpcode(0x6005);
        chip8.executeOpcode(0xF018);
        assertEquals(5, chip8.getSoundTimer());

        chip8.tickTimers60Hz();
        assertEquals(4, chip8.getSoundTimer());

        for (AudioConfig.BuzzerWaveform wf : AudioConfig.BuzzerWaveform.values()) {
            double s = AudioEngine.sampleWaveform(wf, 0.25);
            assertTrue(s >= -1.0 && s <= 1.0, "Waveform sample out of bounds for " + wf);
        }
        assertEquals(4000.0, AudioEngine.xoChipPlaybackRate(64), 0.001);
        assertEquals(8000.0, AudioEngine.xoChipPlaybackRate(112), 0.001);

        // Pausing AudioEngine must silence active Mega-Chip PCM playback while preserving track
        try (AudioEngine engine = new AudioEngine()) {
            engine.playMegaSample(new byte[] { (byte) 128, (byte) 200, (byte) 60 }, 8000, true);
            assertTrue(engine.isSoundActive());
            engine.setPaused(true);
            assertEquals(false, engine.isSoundActive());
            engine.setPaused(false);
            assertTrue(engine.isSoundActive());
            engine.stopMegaSample();
            assertEquals(false, engine.isSoundActive());
        }
    }
}
