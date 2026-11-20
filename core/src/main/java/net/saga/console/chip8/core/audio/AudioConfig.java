package net.saga.console.chip8.core.audio;

/**
 * Audio synthesis configuration for CHIP-8 buzzer and XO-CHIP pattern audio.
 */
public record AudioConfig(
    boolean muted,
    float volume,
    BuzzerWaveform waveform,
    int frequencyHz
) {
    public enum BuzzerWaveform {
        SINE("Soft Sine", "Smooth pure sine tone"),
        SQUARE("Classic Square", "Authentic crisp 50% duty-cycle buzzer"),
        TRIANGLE("Warm Triangle", "Mellow NES-triangle-like tone"),
        SAWTOOTH("Retro Sawtooth", "Buzzy vintage synth wave"),
        RETRO_PULSE_25("25% Chiptune Pulse", "Classic 8-bit console pulse wave"),
        SOFT_CHIPTUNE("Filtered Chiptune", "Harmonic soft-clipped retro tone");

        private final String displayName;
        private final String description;

        BuzzerWaveform(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return description;
        }
    }

    public AudioConfig {
        volume = Math.clamp(volume, 0.0f, 1.0f);
        frequencyHz = Math.clamp(frequencyHz, 110, 2000);
        if (waveform == null) {
            waveform = BuzzerWaveform.RETRO_PULSE_25;
        }
    }

    public static AudioConfig defaults() {
        return new AudioConfig(false, 0.45f, BuzzerWaveform.RETRO_PULSE_25, 440);
    }
}
