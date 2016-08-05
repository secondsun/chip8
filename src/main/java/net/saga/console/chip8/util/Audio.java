package net.saga.console.chip8.util;

import java.util.function.IntSupplier;
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

    private static final AudioFormat AF = new AudioFormat((float) 44100, 8, 1, true, false);
    private static SourceDataLine SDL;
    private static byte[] BUF = new byte[1];

    static {
        try {
            SDL = AudioSystem.getSourceDataLine(AF);
            SDL.open();

        } catch (LineUnavailableException ex) {
            SDL = null;
        }
    }

    private static final IntSupplier STREAM = (new IntSupplier() {

        int i = 0;

        @Override
        public int getAsInt() {
            double angle = i / ((float) 44100 / 880) * 2.0 * Math.PI;
            i++;
            return (byte) (Math.sin(angle) * 20);

        }
    });

    private static boolean PLAYING = false;

    public static void play() {
        if (!PLAYING) {
            SDL.start();
        }

        for (int i = 0; i < (44100 / 60); i++) {
            int number = STREAM.getAsInt();
            BUF[0] = (byte) number;
            SDL.write(BUF, 0, 1);
        }

    }

    public static void stop() {
        if (PLAYING) {
            SDL.stop();
            PLAYING = false;
        }
    }

}
