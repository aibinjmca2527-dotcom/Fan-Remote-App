package com.example.fanremote;

import android.util.Log;

import java.util.EnumMap;
import java.util.Map;

/**
 * SINGLE place where IR signals live. Nothing else in the app contains IR data.
 *
 * HOW TO ADD A SIGNAL (inside the static block below), either as Pronto hex:
 *
 *     pronto(RemoteButton.POWER, "0000 006D ....");
 *
 * or as raw data (carrier Hz, then on/off microsecond pattern):
 *
 *     raw(RemoteButton.SPEED_1, 38000, new int[]{9000, 4500, 560, 560, ...});
 *
 * (for signals that must be sent several times:
 *     raw(RemoteButton.POWER, 38000, new int[]{...}, 3, 40000); )
 *
 * Until a signal is registered, pressing that button shows a clear
 * "signal not added yet" message and transmits nothing.
 *
 * Optional extra signals for voice control (only register them if your remote really
 * has them): POWER_ON, POWER_OFF, SPEED_UP, SPEED_DOWN.
 */
public final class FanRemoteConfig {
    private static final String TAG = "FanRemoteConfig";
    private static final Map<RemoteButton, IrCommand> COMMANDS =
            new EnumMap<>(RemoteButton.class);

    static {
        // ---- Paste the real signals here ----
        // pronto(RemoteButton.POWER,   "<pronto hex>");
        // pronto(RemoteButton.SPEED_1, "<pronto hex>");
        // pronto(RemoteButton.SPEED_2, "<pronto hex>");
        // pronto(RemoteButton.SPEED_3, "<pronto hex>");
        // pronto(RemoteButton.SPEED_4, "<pronto hex>");
        // pronto(RemoteButton.SPEED_5, "<pronto hex>");
        // pronto(RemoteButton.BOOST,   "<pronto hex>");
        // pronto(RemoteButton.TIMER,   "<pronto hex>");
        // pronto(RemoteButton.LIGHT,   "<pronto hex>");
        // pronto(RemoteButton.SWING,   "<pronto hex>");
    }

    private FanRemoteConfig() {}

    private static void pronto(RemoteButton button, String hex) {
        try {
            register(IrCommand.pronto(button, hex));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Bad Pronto data for " + button, e);
        }
    }

    private static void raw(RemoteButton button, int hz, int[] patternUs) {
        register(IrCommand.raw(button, hz, patternUs));
    }

    private static void raw(RemoteButton button, int hz, int[] patternUs, int repeats, int gapUs) {
        register(IrCommand.raw(button, hz, patternUs, repeats, gapUs));
    }

    private static void register(IrCommand command) {
        COMMANDS.put(command.button, command);
    }

    /** The signal for a button, or null if none has been supplied. */
    public static IrCommand get(RemoteButton button) {
        return COMMANDS.get(button);
    }

    public static boolean has(RemoteButton button) {
        return COMMANDS.containsKey(button);
    }

    /** How many of the on-screen buttons currently have a signal. */
    public static int configuredUiButtons() {
        int n = 0;
        for (RemoteButton b : RemoteButton.UI_BUTTONS) {
            if (has(b)) n++;
        }
        return n;
    }
}
