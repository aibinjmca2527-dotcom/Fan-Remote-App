package com.example.fanremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/** The remote screen. Contains no IR logic: every press goes to FanCommandManager. */
public class MainActivity extends Activity implements FanCommandManager.StateListener {
    private static final String PREFS = "ui_prefs";
    private static final String KEY_HAPTIC = "haptic";

    private FanCommandManager manager;
    private SharedPreferences prefs;
    private FanDialView dial;
    private TextView statusText;
    private TextView speedText;
    private TextView hintText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        manager = FanCommandManager.get(this);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        dial = findViewById(R.id.dial);
        statusText = findViewById(R.id.statusText);
        speedText = findViewById(R.id.speedText);
        hintText = findViewById(R.id.hintText);

        bind(R.id.btnPower, RemoteButton.POWER);

        bind(R.id.btnLight, RemoteButton.LIGHT);


        dial.setListener(new FanDialView.Listener() {
            @Override
            public void onSpeedPressed(int speed) {
                RemoteButton b = RemoteButton.forSpeed(speed);
                if (b != null) manager.send(b, MainActivity.this::showResult);
            }

            @Override
            public void onBoostPressed() {
                manager.send(RemoteButton.BOOST, MainActivity.this::showResult);
            }
        });

        View menu = findViewById(R.id.btnMenu);
        View settings = findViewById(R.id.btnSettings);
        PressEffect.apply(menu);
        PressEffect.apply(settings);
        menu.setOnClickListener(v -> showHelp());
        settings.setOnClickListener(v -> showSettings());

        if (savedInstanceState == null && !manager.isIrAvailable()) {
            new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    .setTitle(R.string.no_ir_title)
                    .setMessage(R.string.no_ir_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private void bind(int viewId, final RemoteButton button) {
        View v = findViewById(viewId);
        PressEffect.apply(v);
        v.setOnClickListener(view -> {
            if (prefs.getBoolean(KEY_HAPTIC, true)) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
            manager.send(button, this::showResult);
        });
    }

    private void showResult(FanCommandManager.CommandResult r) {
        if (isFinishing() || isDestroyed()) return;
        hintText.setText(r.message);
        if (!r.success) Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        manager.addListener(this);
        onStateChanged(manager.getState());
    }

    @Override
    protected void onStop() {
        manager.removeListener(this);
        super.onStop();
    }

    @Override
    public void onStateChanged(FanState state) {
        statusText.setText(state.powerOn ? R.string.on : R.string.off);
        speedText.setText(state.boost && state.powerOn ? "B" : (state.speed > 0 ? String.valueOf(state.speed) : "\u2013"));
        dial.setSelectedSpeed(state.powerOn && !state.boost ? state.speed : 0);
    }

    private void showHelp() {
        String ir = getString(manager.isIrAvailable() ? R.string.ir_ready : R.string.ir_missing);
        String body = getString(R.string.help_body, ir,
                FanRemoteConfig.configuredUiButtons(), RemoteButton.UI_BUTTONS.length);
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(R.string.help_title)
                .setMessage(body)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showSettings() {
        final boolean[] checked = {prefs.getBoolean(KEY_HAPTIC, true)};
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(R.string.settings_title)
                .setMultiChoiceItems(new CharSequence[]{getString(R.string.setting_haptic)},
                        checked, (d, which, isChecked) ->
                                prefs.edit().putBoolean(KEY_HAPTIC, isChecked).apply())
                .setNeutralButton(R.string.reset_state, (d, w) -> {
                    manager.resetAssumedState();
                    hintText.setText(R.string.state_reset);
                })
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
