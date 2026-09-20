package com.example.fanremote;

/**
 * One IR signal: carrier frequency + on/off pattern in microseconds
 * (mark, space, mark, space, ...), which is exactly what ConsumerIrManager expects.
 * Instances are immutable.
 */
public final class IrCommand {
    public final RemoteButton button;
    public final int frequencyHz;
    private final int[] pattern;
    public final int repeatCount;
    public final int repeatGapUs;

    private IrCommand(RemoteButton button, int frequencyHz, int[] pattern,
                      int repeatCount, int repeatGapUs) {
        this.button = button;
        this.frequencyHz = frequencyHz;
        this.pattern = pattern;
        this.repeatCount = Math.max(1, repeatCount);
        this.repeatGapUs = repeatGapUs;
    }

    /** Raw signal: carrier in Hz, pattern in microseconds. Sent once. */
    public static IrCommand raw(RemoteButton button, int frequencyHz, int[] patternUs) {
        return new IrCommand(button, frequencyHz, patternUs.clone(), 1, 0);
    }

    /** Raw signal that must be sent several times back-to-back with a gap between frames. */
    public static IrCommand raw(RemoteButton button, int frequencyHz, int[] patternUs,
                                int repeatCount, int repeatGapUs) {
        return new IrCommand(button, frequencyHz, patternUs.clone(), repeatCount, repeatGapUs);
    }

    /**
     * Pronto hex (e.g. "0000 006D 0022 0000 ..."). This is the only conversion applied:
     * Pronto stores timings in carrier cycles, ConsumerIrManager wants microseconds.
     * The once-sequence is followed by the repeat-sequence, sent one time.
     */
    public static IrCommand pronto(RemoteButton button, String hex) {
        String[] words = hex.trim().split("\\s+");
        int[] v = new int[words.length];
        for (int i = 0; i < words.length; i++) {
            v[i] = Integer.parseInt(words[i], 16);
        }
        if (v.length < 6 || v[0] != 0) {
            throw new IllegalArgumentException("Unsupported Pronto format (must start with 0000)");
        }
        double periodUs = v[1] * 0.241246;
        if (periodUs <= 0) throw new IllegalArgumentException("Bad Pronto frequency code");
        int frequency = (int) Math.round(1000000.0 / periodUs);
        int pairs = v[2] + v[3];
        if (pairs <= 0 || v.length < 4 + pairs * 2) {
            throw new IllegalArgumentException("Pronto data length mismatch");
        }
        int[] p = new int[pairs * 2];
        for (int i = 0; i < p.length; i++) {
            p[i] = (int) Math.round(v[4 + i] * periodUs);
        }
        return new IrCommand(button, frequency, p, 1, 0);
    }

    public int[] getPattern() {
        return pattern.clone();
    }

    /** Returns null if the command is usable, otherwise a short problem description. */
    public String validate() {
        if (frequencyHz < 10000 || frequencyHz > 100000) return "carrier frequency out of range";
        if (pattern.length == 0) return "empty pattern";
        for (int d : pattern) {
            if (d <= 0) return "pattern contains a non-positive duration";
        }
        if (repeatCount > 1 && repeatGapUs <= 0) return "repeat gap missing";
        return null;
    }

    /** Final pattern handed to ConsumerIrManager (repeats expanded if needed). */
    public int[] buildTransmitPattern() {
        if (repeatCount <= 1) return pattern.clone();
        boolean odd = (pattern.length % 2) != 0;
        int[] out = new int[(pattern.length + (odd ? 1 : 0)) * repeatCount - (odd ? 1 : 0)];
        int pos = 0;
        for (int r = 0; r < repeatCount; r++) {
            System.arraycopy(pattern, 0, out, pos, pattern.length);
            pos += pattern.length;
            if (r < repeatCount - 1) {
                if (odd) {
                    out[pos++] = repeatGapUs;
                } else {
                    out[pos - 1] += repeatGapUs;
                }
            }
        }
        return out;
    }
}
