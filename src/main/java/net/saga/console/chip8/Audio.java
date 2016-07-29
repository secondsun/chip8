package net.saga.console.chip8;

import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * This class is a helper class for emitting, playing, and viewing audio data.
 *
 * @author summers
 */
public final class Audio {

    private static byte[] BUF = new byte[1];
    private static boolean MUTE = false;
    private static final AudioFormat AF = new AudioFormat((float) 44100, 8, 1, true, false);
    private static SourceDataLine SDL;

    static {
        try {
            SDL = AudioSystem.getSourceDataLine(AF);
            SDL.open();
            SDL.start();
        } catch (LineUnavailableException ex) {
            MUTE = true;
            SDL = null;
        }
    }

    public static void reset() {
        if (SDL != null) {
            if (SDL.isOpen()) {
                SDL.stop();
                SDL.close();
            }
        }
    
        BUF = new byte[1];
        MUTE = false;
        try {
            SDL = AudioSystem.getSourceDataLine(AF);
            SDL.open();
            SDL.start();
        } catch (LineUnavailableException ex) {
            MUTE = true;
            SDL = null;
        }
    }

    private Audio() {
    }

    //Adds 1/60 second of a tone to the audio buffer
    public static void appendTone() {
        if (BUF.length == 1) {
            BUF = new byte[44100/60];
            for (int i = 0; i < (1000/60) * (44100/1000); i++) {
                double angle = i / ((float) 44100 / 880) * 2.0 * Math.PI;
                BUF[i] = (byte) (Math.sin(angle) * 20);
            }
        }
    }

    public static void playTone() {
        if (SDL != null && !MUTE) {
            System.out.println(BUF[0] - BUF[BUF.length - 1]);
                SDL.write(BUF, 0, BUF.length);
        }
    }
    

    public static void mute() {
        MUTE = true;
    }

    public static void unmute() {
        MUTE = false;
    }

    public static byte[] getBuffer() {
        return Arrays.copyOf(BUF, BUF.length);
    }

}
