package com.example.fanremote;

/**
 * Every logical fan command. Buttons on the screen and Assistant voice commands both
 * refer to these values. A value only does something once its IR signal is registered
 * in {@link FanRemoteConfig}.
 */
public enum RemoteButton {
    POWER("Power", 0),          // physical power (toggle) button on the UI
    POWER_ON("Power on", 0),    // optional dedicated ON signal (used by voice if supplied)
    POWER_OFF("Power off", 0),  // optional dedicated OFF signal (used by voice if supplied)

    SPEED_1("Speed 1", 1),
    SPEED_2("Speed 2", 2),
    SPEED_3("Speed 3", 3),
    SPEED_4("Speed 4", 4),
    SPEED_5("Speed 5", 5),

    SPEED_UP("Speed up", 0),     // optional dedicated signal
    SPEED_DOWN("Speed down", 0), // optional dedicated signal

    BOOST("Boost", 0),
    TIMER("Timer", 0),
    LIGHT("Light", 0),
    SWING("Swing", 0);          // green circular-arrows button on the remote

    public final String label;
    public final int speed;

    RemoteButton(String label, int speed) {
        this.label = label;
        this.speed = speed;
    }

    /** Returns SPEED_1..SPEED_5 for 1..5, or null. */
    public static RemoteButton forSpeed(int speed) {
        switch (speed) {
            case 1: return SPEED_1;
            case 2: return SPEED_2;
            case 3: return SPEED_3;
            case 4: return SPEED_4;
            case 5: return SPEED_5;
            default: return null;
        }
    }

    /** The ten buttons that physically exist on the on-screen remote. */
    public static final RemoteButton[] UI_BUTTONS = {
            POWER, SPEED_1, SPEED_2, SPEED_3, SPEED_4, SPEED_5, BOOST, TIMER, LIGHT, SWING
    };
}
