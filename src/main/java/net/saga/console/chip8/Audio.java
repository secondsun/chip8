package net.saga.console.chip8;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

/**
 * This class is a helper class for emitting, playing, and viewing audio data.
 *
 * @author summers
 */
public final class Audio {
    
    private static final Synthesizer SYNTH;
    private static final MidiChannel[] CHANNELS;
    private static boolean PLAYING = false;
    static {
        try {
            SYNTH = MidiSystem.getSynthesizer();
            SYNTH.open();
            CHANNELS = SYNTH.getChannels();
        } catch (MidiUnavailableException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static void play() {
        if (!PLAYING) {
            CHANNELS[0].noteOn(60, 100);
            PLAYING = true;
        }
    }
    
    public static void stop() {
        if (PLAYING) {
            CHANNELS[0].noteOff(60);
            PLAYING = false;
        }
    }
    
    public static void mute() {
        CHANNELS[0].setMute(true);
    }
    
    public static void unmute() {
        CHANNELS[0].setMute(false);
    }
    
}
