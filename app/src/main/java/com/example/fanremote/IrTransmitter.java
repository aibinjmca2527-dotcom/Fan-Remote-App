package com.example.fanremote;

import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.util.Log;

/** Thin wrapper around ConsumerIrManager (the phone's built-in IR blaster). */
public final class IrTransmitter {
    private static final String TAG = "IrTransmitter";

    public enum Status { OK, NO_EMITTER, INVALID_COMMAND, UNSUPPORTED_FREQUENCY, FAILED }

    private final ConsumerIrManager manager;

    public IrTransmitter(Context context) {
        ConsumerIrManager m = null;
        try {
            m = (ConsumerIrManager) context.getApplicationContext()
                    .getSystemService(Context.CONSUMER_IR_SERVICE);
        } catch (RuntimeException e) {
            Log.w(TAG, "IR service unavailable", e);
        }
        manager = m;
    }

    public boolean isAvailable() {
        try {
            return manager != null && manager.hasIrEmitter();
        } catch (RuntimeException e) {
            Log.w(TAG, "hasIrEmitter failed", e);
            return false;
        }
    }

    private boolean isFrequencySupported(int hz) {
        try {
            ConsumerIrManager.CarrierFrequencyRange[] ranges = manager.getCarrierFrequencies();
            if (ranges == null || ranges.length == 0) return true; // let transmit() decide
            for (ConsumerIrManager.CarrierFrequencyRange r : ranges) {
                if (hz >= r.getMinFrequency() && hz <= r.getMaxFrequency()) return true;
            }
            return false;
        } catch (RuntimeException e) {
            return true;
        }
    }

    /** Blocking call (a signal lasts ~100 ms); FanCommandManager runs it off the UI thread. */
    public Status transmit(IrCommand command) {
        if (!isAvailable()) return Status.NO_EMITTER;
        String problem = command.validate();
        if (problem != null) {
            Log.w(TAG, "Invalid command " + command.button + ": " + problem);
            return Status.INVALID_COMMAND;
        }
        if (!isFrequencySupported(command.frequencyHz)) return Status.UNSUPPORTED_FREQUENCY;
        try {
            manager.transmit(command.frequencyHz, command.buildTransmitPattern());
            return Status.OK;
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Rejected pattern", e);
            return Status.INVALID_COMMAND;
        } catch (RuntimeException e) { // includes SecurityException
            Log.w(TAG, "Transmit failed", e);
            return Status.FAILED;
        }
    }
}
