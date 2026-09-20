package com.example.fanremote;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;

/** Lightweight "physical button" feedback: a quick scale-down while pressed. */
final class PressEffect {
    private PressEffect() {}

    @SuppressLint("ClickableViewAccessibility")
    static void apply(View v) {
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                    break;
                default:
                    break;
            }
            return false; // let the normal click/ripple handling continue
        });
    }
}
