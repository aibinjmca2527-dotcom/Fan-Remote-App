package com.example.fanremote;

/**
 * What the app ASSUMES about the fan. IR is one-way, so this is only the result of the
 * commands this app sent; it can be wrong if the physical remote is also used.
 */
public final class FanState {
    public final boolean powerOn;
    public final int speed; // 0 = unknown, otherwise 1..5

    public FanState(boolean powerOn, int speed) {
        this.powerOn = powerOn;
        this.speed = speed;
    }
}
