package com.example.fanremote;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Invisible activity launched by Google Assistant App Actions / static shortcuts.
 * It forwards the request to AssistantCommandHandler (-> FanCommandManager -> IrTransmitter)
 * and closes immediately. It does not touch the keyguard: if Android/Assistant requires the
 * phone to be unlocked before opening an app, that requirement is respected.
 */
public class AssistantActivity extends Activity {
    private static final String TAG = "AssistantActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            AssistantCommandHandler.handle(this, getIntent(), result -> {
                Toast.makeText(getApplicationContext(), result.message, Toast.LENGTH_SHORT).show();
                finishQuietly();
            });
        } catch (RuntimeException e) {
            Log.w(TAG, "Unexpected error", e);
            Toast.makeText(getApplicationContext(), R.string.err_failed, Toast.LENGTH_SHORT).show();
            finishQuietly();
        }
    }

    private void finishQuietly() {
        if (!isFinishing()) {
            finish();
            overridePendingTransition(0, 0);
        }
    }
}
