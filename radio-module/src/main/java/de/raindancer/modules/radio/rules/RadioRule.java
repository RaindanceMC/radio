package de.raindancer.modules.radio.rules;

import java.util.OptionalInt;

/** The volume's bounds and steps, and how a length reads. */
public final class RadioRule {

    /** Quieter than this is the same as off, which has its own button. */
    public static final int QUIETEST = 5;
    public static final int LOUDEST = 100;
    public static final int DEFAULT_VOLUME = 60;
    private static final int STEP = 10;

    private RadioRule() {
    }

    public static int volume(int percent) {
        return Math.max(QUIETEST, Math.min(LOUDEST, percent));
    }

    public static int louder(int percent) {
        return volume((percent / STEP + 1) * STEP);
    }

    public static int quieter(int percent) {
        return volume(((percent + STEP - 1) / STEP - 1) * STEP);
    }

    public static OptionalInt typedVolume(String typed) {
        try {
            return OptionalInt.of(volume(Integer.parseInt(typed.strip().replaceFirst("%$", ""))));
        } catch (NumberFormatException | NullPointerException notANumber) {
            return OptionalInt.empty();
        }
    }

    public static String clock(double seconds) {
        long whole = Math.round(seconds);
        return whole / 60 + ":" + String.format("%02d", whole % 60);
    }
}
