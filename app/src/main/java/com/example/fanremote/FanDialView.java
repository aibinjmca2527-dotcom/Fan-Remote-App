package com.example.fanremote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * The round speed dial: five coloured wedges (3 top, then clockwise 4, 5, 1, 2)
 * around a BOOST centre button. Drawn with Canvas so it stays crisp and scales to any screen.
 */
public class FanDialView extends View {

    public interface Listener {
        void onSpeedPressed(int speed);
        void onBoostPressed();
    }

    private static final int HIT_NONE = -1;
    private static final int HIT_BOOST = -2;

    /** Speed shown in each wedge, starting at the top and going clockwise. */
    private static final int[] SPEEDS = {3, 4, 5, 1, 2};
    /** Centre angle of each wedge (Canvas degrees: 0 = east, clockwise). */
    private static final float[] CENTER_ANGLE = {270f, 342f, 54f, 126f, 198f};
    private static final int[][] COLORS = {
            {0xFF3AAAFF, 0xFF0A62E6}, // 3 blue
            {0xFF4FE8C8, 0xFF0E8C78}, // 4 teal
            {0xFFFF5AB4, 0xFF9A1AB6}, // 5 pink
            {0xFFFFBA2B, 0xFFFF7600}, // 1 orange
            {0xFF9C60FF, 0xFF4527D3}  // 2 purple
    };
    private static final int CYAN = 0xFF22E3FF;
    private static final float SWEEP = 72f;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path[] paths = new Path[5];
    private final Shader[] shaders = new Shader[5];
    private final RectF outerRect = new RectF();
    private final RectF innerRect = new RectF();
    private final Drawable boostIcon;
    private final float density;

    private float cx, cy, panelR, segOuter, segInner, ringR;
    private Shader panelShader, centerShader;
    private int selectedSpeed = 0;
    private int pressedHit = HIT_NONE;
    private Listener listener;

    public FanDialView(Context context) {
        this(context, null);
    }

    public FanDialView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        boostIcon = context.getDrawable(R.drawable.ic_fan_boost);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setColor(0xFFFFFFFF);
        setContentDescription(context.getString(R.string.cd_dial));
    }

    public void setListener(Listener l) {
        listener = l;
    }

    /** Highlights the wedge for the assumed current speed (0 = none). */
    public void setSelectedSpeed(int speed) {
        if (speed != selectedSpeed) {
            selectedSpeed = speed;
            invalidate();
        }
    }

    // ------------------------------------------------------------ geometry

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cx = w / 2f;
        cy = h / 2f;
        panelR = Math.min(w, h) / 2f - 6 * density;
        segOuter = panelR * 0.93f;
        segInner = panelR * 0.50f;
        ringR = panelR * 0.435f;
        outerRect.set(cx - segOuter, cy - segOuter, cx + segOuter, cy + segOuter);
        innerRect.set(cx - segInner, cy - segInner, cx + segInner, cy + segInner);

        float gapPx = 6 * density;
        float outerGap = (float) Math.toDegrees(gapPx / segOuter);
        float innerGap = (float) Math.toDegrees(gapPx / segInner);
        RectF b = new RectF();
        for (int i = 0; i < 5; i++) {
            float half = SWEEP / 2f;
            float oStart = CENTER_ANGLE[i] - half + outerGap / 2f;
            float oSweep = SWEEP - outerGap;
            float iStart = CENTER_ANGLE[i] + half - innerGap / 2f;
            float iSweep = -(SWEEP - innerGap);
            Path p = new Path();
            p.arcTo(outerRect, oStart, oSweep, true);
            p.arcTo(innerRect, iStart, iSweep, false);
            p.close();
            paths[i] = p;
            p.computeBounds(b, true);
            shaders[i] = new LinearGradient(b.centerX(), b.top, b.centerX(), b.bottom,
                    COLORS[i][0], COLORS[i][1], Shader.TileMode.CLAMP);
        }
        panelShader = new RadialGradient(cx, cy, panelR,
                new int[]{0xFF0C3A9A, 0xFF071F62, 0xFF061850}, new float[]{0f, 0.7f, 1f},
                Shader.TileMode.CLAMP);
        centerShader = new RadialGradient(cx, cy, ringR,
                new int[]{0xFF0B2B78, 0xFF041040}, null, Shader.TileMode.CLAMP);

        if (boostIcon != null) {
            float size = panelR * 0.42f;
            int left = Math.round(cx - size / 2f);
            int top = Math.round(cy - size * 0.78f);
            boostIcon.setBounds(left, top, Math.round(left + size), Math.round(top + size));
        }
    }

    // ------------------------------------------------------------- drawing

    @Override
    protected void onDraw(Canvas c) {
        if (paths[0] == null) return;

        // Outer dark panel + soft glow rim
        fill.setStyle(Paint.Style.FILL);
        fill.setShader(panelShader);
        c.drawCircle(cx, cy, panelR, fill);
        fill.setShader(null);
        stroke.setShader(null);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2 * density);
        stroke.setColor(0x552A7BFF);
        c.drawCircle(cx, cy, panelR, stroke);

        // Wedges
        CornerPathEffect round = new CornerPathEffect(10 * density);
        for (int i = 0; i < 5; i++) {
            fill.setPathEffect(round);
            fill.setStyle(Paint.Style.FILL);
            fill.setShader(shaders[i]);
            c.drawPath(paths[i], fill);
            fill.setShader(null);

            stroke.setPathEffect(round);
            stroke.setStrokeWidth(1.5f * density);
            stroke.setColor(0x66FFFFFF);
            c.drawPath(paths[i], stroke);

            if (pressedHit == i) {
                fill.setColor(0x55FFFFFF);
                c.drawPath(paths[i], fill);
            }
            if (SPEEDS[i] == selectedSpeed) {
                stroke.setStrokeWidth(9 * density);
                stroke.setColor(0x4422E3FF);
                c.drawPath(paths[i], stroke);
                stroke.setStrokeWidth(2.5f * density);
                stroke.setColor(CYAN);
                c.drawPath(paths[i], stroke);
            }
            fill.setPathEffect(null);
            stroke.setPathEffect(null);

            // Digit
            double a = Math.toRadians(CENTER_ANGLE[i]);
            float tr = panelR * 0.72f;
            text.setTextSize(panelR * 0.20f);
            text.setLetterSpacing(0f);
            text.setColor(0xFFFFFFFF);
            float ty = cy + (float) Math.sin(a) * tr
                    - (text.ascent() + text.descent()) / 2f;
            c.drawText(String.valueOf(SPEEDS[i]), cx + (float) Math.cos(a) * tr, ty, text);
        }

        // Centre BOOST button
        fill.setStyle(Paint.Style.FILL);
        fill.setShader(centerShader);
        c.drawCircle(cx, cy, ringR, fill);
        fill.setShader(null);
        if (pressedHit == HIT_BOOST) {
            fill.setColor(0x33FFFFFF);
            c.drawCircle(cx, cy, ringR, fill);
        }
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(8 * density);
        stroke.setColor(0x3322E3FF);
        c.drawCircle(cx, cy, ringR, stroke);
        stroke.setStrokeWidth(3 * density);
        stroke.setColor(CYAN);
        c.drawCircle(cx, cy, ringR, stroke);

        if (boostIcon != null) boostIcon.draw(c);
        text.setTextSize(panelR * 0.095f);
        text.setLetterSpacing(0.06f);
        c.drawText("BOOST", cx, cy + panelR * 0.29f, text);
    }

    // ------------------------------------------------------------ touch

    private int hitTest(float x, float y) {
        float dx = x - cx, dy = y - cy;
        double d = Math.hypot(dx, dy);
        if (d <= ringR) return HIT_BOOST;
        if (d > segOuter * 1.03f) return HIT_NONE;
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) angle += 360;
        double offset = (angle - (270 - SWEEP / 2) + 720) % 360;
        return Math.min(4, (int) (offset / SWEEP));
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressedHit = hitTest(e.getX(), e.getY());
                if (pressedHit == HIT_NONE) return false;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (pressedHit != HIT_NONE && hitTest(e.getX(), e.getY()) != pressedHit) {
                    pressedHit = HIT_NONE;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                int hit = pressedHit;
                boolean same = hit != HIT_NONE && hitTest(e.getX(), e.getY()) == hit;
                pressedHit = HIT_NONE;
                invalidate();
                if (same) {
                    performClick();
                    if (listener != null) {
                        if (hit == HIT_BOOST) listener.onBoostPressed();
                        else listener.onSpeedPressed(SPEEDS[hit]);
                    }
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressedHit = HIT_NONE;
                invalidate();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        return super.performClick();
    }
}
