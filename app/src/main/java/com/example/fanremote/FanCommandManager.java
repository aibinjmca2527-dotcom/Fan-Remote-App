package com.example.fanremote;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The ONE command engine. UI buttons and Google Assistant both call these methods,
 * and everything ends in IrTransmitter:
 *
 *   UI button ----\
 *                  >--> FanCommandManager --> IrTransmitter --> fan
 *   Assistant ----/
 *
 * Call all public methods from the main thread; callbacks arrive on the main thread.
 */
public final class FanCommandManager {

    public static final class CommandResult {
        public final boolean success;
        public final String message;

        public CommandResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public interface ResultCallback {
        void onResult(CommandResult result);
    }

    public interface StateListener {
        void onStateChanged(FanState state);
    }

    private static final String PREFS = "fan_state";
    private static final int MAX_SPEED = 5;
    private static FanCommandManager instance;

    public static synchronized FanCommandManager get(Context context) {
        if (instance == null) {
            instance = new FanCommandManager(context.getApplicationContext());
        }
        return instance;
    }

    private final Context appContext;
    private final IrTransmitter transmitter;
    private final SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ir-transmit");
        t.setDaemon(true);
        return t;
    });
    private final List<StateListener> listeners = new CopyOnWriteArrayList<>();

    private boolean powerOn;
    private int speed;

    private FanCommandManager(Context context) {
        appContext = context;
        transmitter = new IrTransmitter(context);
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        powerOn = prefs.getBoolean("power", false);
        speed = prefs.getInt("speed", 0);
    }

    // ---------------------------------------------------------------- state

    public boolean isIrAvailable() {
        return transmitter.isAvailable();
    }

    public FanState getState() {
        return new FanState(powerOn, speed);
    }

    public void addListener(StateListener l) {
        listeners.add(l);
    }

    public void removeListener(StateListener l) {
        listeners.remove(l);
    }

    /** Lets the user correct the assumed state (e.g. after using the physical remote). */
    public void resetAssumedState() {
        powerOn = false;
        speed = 0;
        persistAndNotify();
    }

    private void persistAndNotify() {
        prefs.edit().putBoolean("power", powerOn).putInt("speed", speed).apply();
        FanState s = getState();
        for (StateListener l : listeners) l.onStateChanged(s);
    }

    private void applyState(RemoteButton b) {
        switch (b) {
            case POWER:     powerOn = !powerOn; break;
            case POWER_ON:  powerOn = true; break;
            case POWER_OFF: powerOn = false; break;
            case SPEED_1: case SPEED_2: case SPEED_3: case SPEED_4: case SPEED_5:
                speed = b.speed;
                powerOn = true;
                break;
            case SPEED_UP:
                speed = Math.min(MAX_SPEED, Math.max(1, speed + 1));
                powerOn = true;
                break;
            case SPEED_DOWN:
                speed = Math.max(1, speed - 1);
                break;
            case BOOST:
                powerOn = true;
                break;
            default:
                break;
        }
        persistAndNotify();
    }

    // ------------------------------------------------------ low-level send

    /** Sends exactly one button's signal. */
    public void send(final RemoteButton button, final ResultCallback callback) {
        final IrCommand command = FanRemoteConfig.get(button);
        if (command == null) {
            deliver(callback, false, appContext.getString(R.string.err_no_signal, button.label));
            return;
        }
        if (!transmitter.isAvailable()) {
            deliver(callback, false, appContext.getString(R.string.err_no_ir));
            return;
        }
        io.execute(() -> {
            final IrTransmitter.Status status = transmitter.transmit(command);
            main.post(() -> {
                if (status == IrTransmitter.Status.OK) {
                    applyState(button);
                    deliver(callback, true, appContext.getString(R.string.sent_ok, button.label));
                } else {
                    deliver(callback, false, messageFor(status, button));
                }
            });
        });
    }

    private String messageFor(IrTransmitter.Status status, RemoteButton b) {
        switch (status) {
            case NO_EMITTER:            return appContext.getString(R.string.err_no_ir);
            case UNSUPPORTED_FREQUENCY: return appContext.getString(R.string.err_frequency);
            case INVALID_COMMAND:       return appContext.getString(R.string.err_invalid, b.label);
            default:                    return appContext.getString(R.string.err_failed);
        }
    }

    private void deliver(ResultCallback cb, boolean ok, String msg) {
        if (cb != null) cb.onResult(new CommandResult(ok, msg));
    }

    // ------------------------------------------- high-level (voice-friendly)

    public void turnOn(ResultCallback cb) {
        if (FanRemoteConfig.has(RemoteButton.POWER_ON)) {
            send(RemoteButton.POWER_ON, cb);
        } else if (FanRemoteConfig.has(RemoteButton.POWER)) {
            // Only a toggle exists: never send it blindly, or "turn on" could switch the fan off.
            if (powerOn) {
                deliver(cb, false, appContext.getString(R.string.info_already_on));
            } else {
                send(RemoteButton.POWER, cb);
            }
        } else {
            deliver(cb, false, appContext.getString(R.string.err_no_signal, RemoteButton.POWER.label));
        }
    }

    public void turnOff(ResultCallback cb) {
        if (FanRemoteConfig.has(RemoteButton.POWER_OFF)) {
            send(RemoteButton.POWER_OFF, cb);
        } else if (FanRemoteConfig.has(RemoteButton.POWER)) {
            if (!powerOn) {
                deliver(cb, false, appContext.getString(R.string.info_already_off));
            } else {
                send(RemoteButton.POWER, cb);
            }
        } else {
            deliver(cb, false, appContext.getString(R.string.err_no_signal, RemoteButton.POWER.label));
        }
    }

    public void setSpeed(int level, ResultCallback cb) {
        RemoteButton b = RemoteButton.forSpeed(level);
        if (b == null) {
            deliver(cb, false, appContext.getString(R.string.err_speed_range));
            return;
        }
        send(b, cb);
    }

    public void speedUp(ResultCallback cb) {
        if (FanRemoteConfig.has(RemoteButton.SPEED_UP)) {
            send(RemoteButton.SPEED_UP, cb);
            return;
        }
        if (speed >= MAX_SPEED) {
            deliver(cb, false, appContext.getString(R.string.info_max_speed));
            return;
        }
        setSpeed(Math.max(1, speed + 1), cb);
    }

    public void speedDown(ResultCallback cb) {
        if (FanRemoteConfig.has(RemoteButton.SPEED_DOWN)) {
            send(RemoteButton.SPEED_DOWN, cb);
            return;
        }
        if (speed <= 1) {
            deliver(cb, false, appContext.getString(R.string.info_min_speed));
            return;
        }
        setSpeed(speed - 1, cb);
    }
}
