package net.saga.console.chip8.core.audio;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Click-free, real-time PCM synthesizer supporting:
 * - Standard CHIP-8 configurable buzzer presets (6 waveforms + pitch)
 * - XO-CHIP 16-byte programmable pattern audio (F002 / FX3A)
 * - Mega-Chip 8-bit digitized PCM streaming (060N / 0700)
 */
public final class AudioEngine implements AutoCloseable {
    public static final int SAMPLE_RATE = 44_100;
    private static final int CHUNK_SAMPLES = 512;
    private static final int SCOPE_SIZE = 64;

    private record MegaSampleTrack(byte[] samples, int sampleRateHz, boolean loop) {}

    private final AtomicReference<AudioConfig> config = new AtomicReference<>(AudioConfig.defaults());
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean paused = false;
    private volatile boolean active = false;
    private volatile boolean testToneActive = false;
    private volatile boolean xoChipPatternMode = false;
    private volatile int xoPitch = 64;
    private final byte[] xoPattern = new byte[16];

    private final AtomicReference<MegaSampleTrack> megaTrack = new AtomicReference<>();
    private final AtomicBoolean megaTrackReset = new AtomicBoolean(false);

    private final float[] scopeBuffer = new float[SCOPE_SIZE];
    private Thread synthThread;

    public AudioEngine() {
        Arrays.fill(xoPattern, 0, 8, (byte) 0xFF);
        Arrays.fill(xoPattern, 8, 16, (byte) 0x00);
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        synthThread = Thread.ofVirtual().name("chip8-audio-synth").start(this::synthLoop);
    }

    public void setConfig(AudioConfig newConfig) {
        if (newConfig != null) {
            config.set(newConfig);
        }
    }

    public AudioConfig getConfig() {
        return config.get();
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setSoundActive(boolean soundActive) {
        this.active = soundActive;
    }

    public void setTestToneActive(boolean active) {
        this.testToneActive = active;
    }

    public boolean isSoundActive() {
        AudioConfig cfg = config.get();
        if (cfg.muted() || cfg.volume() <= 0.001f) {
            return false;
        }
        if (testToneActive) {
            return true;
        }
        return !paused && (active || megaTrack.get() != null);
    }

    public void setXoChipPatternMode(boolean enabled) {
        this.xoChipPatternMode = enabled;
    }

    public void loadXoPattern(byte[] pattern16) {
        synchronized (xoPattern) {
            System.arraycopy(pattern16, 0, xoPattern, 0, Math.min(16, pattern16.length));
        }
    }

    public void setXoPitch(int pitchRegister) {
        this.xoPitch = pitchRegister & 0xFF;
    }

    /**
     * Starts Mega-Chip 8-bit unsigned mono PCM audio playback (060N).
     */
    public void playMegaSample(byte[] unsigned8BitPcm, int sampleRateHz, boolean loop) {
        if (unsigned8BitPcm == null || unsigned8BitPcm.length == 0 || sampleRateHz <= 0) {
            stopMegaSample();
            return;
        }
        megaTrack.set(new MegaSampleTrack(Arrays.copyOf(unsigned8BitPcm, unsigned8BitPcm.length), sampleRateHz, loop));
        megaTrackReset.set(true);
    }

    /**
     * Stops Mega-Chip digitized PCM playback (0700).
     */
    public void stopMegaSample() {
        megaTrack.set(null);
    }

    public float[] snapshotWaveform() {
        synchronized (scopeBuffer) {
            return Arrays.copyOf(scopeBuffer, scopeBuffer.length);
        }
    }

    public static double sampleWaveform(AudioConfig.BuzzerWaveform waveform, double phase) {
        double p = phase - Math.floor(phase);
        return switch (waveform) {
            case SINE -> Math.sin(2.0 * Math.PI * p);
            case SQUARE -> p < 0.5 ? 0.85 : -0.85;
            case TRIANGLE -> 4.0 * Math.abs(p - 0.5) - 1.0;
            case SAWTOOTH -> (2.0 * p - 1.0) * 0.8;
            case RETRO_PULSE_25 -> p < 0.25 ? 0.85 : -0.85;
            case SOFT_CHIPTUNE -> {
                double fundamental = Math.sin(2.0 * Math.PI * p);
                double third = Math.sin(6.0 * Math.PI * p) / 3.0;
                double fifth = Math.sin(10.0 * Math.PI * p) / 5.0;
                yield Math.tanh(1.4 * (fundamental + third + fifth));
            }
        };
    }

    public static double xoChipPlaybackRate(int pitchRegister) {
        return 4000.0 * Math.pow(2.0, ((pitchRegister & 0xFF) - 64.0) / 48.0);
    }

    private void synthLoop() {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        SourceDataLine line = null;
        try {
            line = AudioSystem.getSourceDataLine(format);
            line.open(format, CHUNK_SAMPLES * 4);
            line.start();
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException ignored) {
            line = null;
        }

        byte[] pcm = new byte[CHUNK_SAMPLES * 2];
        double phase = 0.0;
        double xoBitPosition = 0.0;
        double megaPos = 0.0;
        double envelope = 0.0;
        double megaEnvelope = 0.0;
        double lowPassState = 0.0;
        byte[] localPattern = new byte[16];

        try {
            while (running.get()) {
                AudioConfig cfg = config.get();
                boolean isPausedNow = paused;
                boolean muted = cfg.muted() || cfg.volume() <= 0.001f;
                boolean isTestTone = testToneActive;
                boolean shouldBuzz = !muted && (isTestTone || (active && !isPausedNow));
                double targetEnv = shouldBuzz ? 1.0 : 0.0;
                double envStep = 1.0 / (SAMPLE_RATE * 0.006);

                if (megaTrackReset.getAndSet(false)) {
                    megaPos = 0.0;
                }
                MegaSampleTrack currentMega = megaTrack.get();
                boolean shouldPlayMega = currentMega != null && !muted && !isPausedNow;
                double targetMegaEnv = shouldPlayMega ? 1.0 : 0.0;
                double megaStep = currentMega != null ? (double) currentMega.sampleRateHz() / SAMPLE_RATE : 0.0;

                boolean patternMode = xoChipPatternMode && !isTestTone;
                if (patternMode) {
                    synchronized (xoPattern) {
                        System.arraycopy(xoPattern, 0, localPattern, 0, 16);
                    }
                }
                double xoStepPerSample = xoChipPlaybackRate(xoPitch) / SAMPLE_RATE;
                double phaseStep = (double) cfg.frequencyHz() / SAMPLE_RATE;

                for (int i = 0; i < CHUNK_SAMPLES; i++) {
                    if (envelope < targetEnv) {
                        envelope = Math.min(targetEnv, envelope + envStep);
                    } else if (envelope > targetEnv) {
                        envelope = Math.max(targetEnv, envelope - envStep);
                    }
                    if (megaEnvelope < targetMegaEnv) {
                        megaEnvelope = Math.min(targetMegaEnv, megaEnvelope + envStep);
                    } else if (megaEnvelope > targetMegaEnv) {
                        megaEnvelope = Math.max(targetMegaEnv, megaEnvelope - envStep);
                    }

                    double buzzSample;
                    if (envelope <= 0.0001) {
                        buzzSample = 0.0;
                    } else if (patternMode) {
                        int bitIdx = ((int) xoBitPosition) & 127;
                        int byteIdx = bitIdx >>> 3;
                        int bitInByte = 7 - (bitIdx & 7);
                        int bit = (localPattern[byteIdx] >>> bitInByte) & 1;
                        double square = bit == 1 ? 0.85 : -0.85;
                        lowPassState += 0.35 * (square - lowPassState);
                        buzzSample = lowPassState * envelope;
                        xoBitPosition = (xoBitPosition + xoStepPerSample) % 128.0;
                    } else {
                        buzzSample = sampleWaveform(cfg.waveform(), phase) * envelope;
                        phase = (phase + phaseStep) % 1.0;
                    }

                    double megaSample = 0.0;
                    if (currentMega != null && megaEnvelope > 0.0001) {
                        byte[] samples = currentMega.samples();
                        int idx = (int) megaPos;
                        if (idx >= samples.length) {
                            if (currentMega.loop()) {
                                megaPos %= samples.length;
                                idx = (int) megaPos;
                            } else {
                                megaTrack.compareAndSet(currentMega, null);
                                currentMega = null;
                            }
                        }
                        if (currentMega != null) {
                            int nextIdx = (idx + 1 < samples.length) ? idx + 1 : (currentMega.loop() ? 0 : idx);
                            double frac = megaPos - idx;
                            double s0 = ((samples[idx] & 0xFF) - 128.0) / 128.0;
                            double s1 = ((samples[nextIdx] & 0xFF) - 128.0) / 128.0;
                            megaSample = (s0 + (s1 - s0) * frac) * megaEnvelope;
                            if (shouldPlayMega) {
                                megaPos += megaStep;
                            }
                        }
                    }

                    double mixed = Math.clamp(buzzSample + megaSample, -1.0, 1.0) * cfg.volume();
                    int sample16 = (int) Math.clamp(Math.round(mixed * 28_000.0), Short.MIN_VALUE, Short.MAX_VALUE);
                    pcm[i * 2] = (byte) (sample16 & 0xFF);
                    pcm[i * 2 + 1] = (byte) ((sample16 >>> 8) & 0xFF);

                    if ((i & 7) == 0) {
                        int scopeIdx = i >>> 3;
                        if (scopeIdx < SCOPE_SIZE) {
                            synchronized (scopeBuffer) {
                                scopeBuffer[scopeIdx] = (float) mixed;
                            }
                        }
                    }
                }

                if (line != null) {
                    line.write(pcm, 0, pcm.length);
                } else {
                    try {
                        Thread.sleep(11);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } finally {
            if (line != null) {
                line.stop();
                line.close();
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        if (synthThread != null) {
            synthThread.interrupt();
        }
    }
}
