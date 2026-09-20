package com.example.fanremote;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps Google Assistant / shortcut requests onto FanCommandManager (the same engine the
 * UI buttons use). Nothing here talks to IR directly.
 *
 * Two inputs are understood:
 *  1. Extra "command" (set by res/xml/shortcuts.xml): POWER_ON, POWER_OFF, SPEED_UP,
 *     SPEED_DOWN, SPEED_1..SPEED_5, BOOST, TIMER, LIGHT, SWING  (exact mapping).
 *  2. Extra "feature" (free text from the Assistant OPEN_APP_FEATURE intent), e.g.
 *     "turn on fan", "increase speed", "speed 3".
 *
 * Phrase          ->  action
 *  "turn on"      ->  FanCommandManager.turnOn()     (POWER_ON, or POWER toggle if only that exists)
 *  "turn off"     ->  FanCommandManager.turnOff()
 *  "increase ..." ->  FanCommandManager.speedUp()    (SPEED_UP, or next SPEED_n)
 *  "decrease ..." ->  FanCommandManager.speedDown()
 *  "speed 3"      ->  SPEED_3 signal
 *  "boost/timer/light/swing" -> matching signal
 * A phrase whose signal has not been supplied returns a clear "not added" message;
 * nothing is invented.
 */
public final class AssistantCommandHandler {
    private static final String TAG = "AssistantHandler";
    public static final String EXTRA_COMMAND = "command";
    public static final String EXTRA_FEATURE = "feature";

    private static final Pattern DIGIT = Pattern.compile("\\b([1-5])\\b");

    private AssistantCommandHandler() {}

    public static void handle(Context context, Intent intent,
                              FanCommandManager.ResultCallback callback) {
        FanCommandManager manager = FanCommandManager.get(context);
        try {
            if (intent != null) {
                String command = intent.getStringExtra(EXTRA_COMMAND);
                if (command != null && runByName(manager,
                        command.trim().toUpperCase(Locale.ROOT), callback)) {
                    return;
                }
                String feature = intent.getStringExtra(EXTRA_FEATURE);
                if (feature != null && runByText(manager, feature, callback)) {
                    return;
                }
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Assistant command failed", e);
        }
        callback.onResult(new FanCommandManager.CommandResult(
                false, context.getString(R.string.err_assistant_unrecognised)));
    }

    private static boolean runByName(FanCommandManager m, String name,
                                     FanCommandManager.ResultCallback cb) {
        switch (name) {
            case "POWER_ON":   m.turnOn(cb);    return true;
            case "POWER_OFF":  m.turnOff(cb);   return true;
            case "SPEED_UP":   m.speedUp(cb);   return true;
            case "SPEED_DOWN": m.speedDown(cb); return true;
            default:
                try {
                    m.send(RemoteButton.valueOf(name), cb);
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
        }
    }

    private static boolean runByText(FanCommandManager m, String raw,
                                     FanCommandManager.ResultCallback cb) {
        String t = " " + raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim() + " ";
        t = t.replaceAll("\\bone\\b", "1").replaceAll("\\btwo\\b", "2")
                .replaceAll("\\bthree\\b", "3").replaceAll("\\bfour\\b", "4")
                .replaceAll("\\bfive\\b", "5");

        if (has(t, "boost|turbo")) { m.send(RemoteButton.BOOST, cb); return true; }
        if (has(t, "timer"))       { m.send(RemoteButton.TIMER, cb); return true; }
        if (has(t, "light|lamp"))  { m.send(RemoteButton.LIGHT, cb); return true; }
        if (has(t, "swing|oscillat\\w*|rotate")) { m.send(RemoteButton.SWING, cb); return true; }

        Matcher digit = DIGIT.matcher(t);
        if (digit.find()) {
            m.setSpeed(Integer.parseInt(digit.group(1)), cb);
            return true;
        }
        if (has(t, "increase|up|faster|higher|raise|more")) { m.speedUp(cb); return true; }
        if (has(t, "decrease|down|slower|lower|reduce|less")) { m.speedDown(cb); return true; }
        if (has(t, "off|stop"))    { m.turnOff(cb); return true; }
        if (has(t, "on|start"))    { m.turnOn(cb); return true; }
        return false;
    }

    private static boolean has(String text, String words) {
        return Pattern.compile("\\b(" + words + ")\\b").matcher(text).find();
    }
}
