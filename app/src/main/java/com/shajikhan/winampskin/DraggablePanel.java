package com.shajikhan.winampskin;

import android.content.Context;
import android.os.Handler;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import android.util.AttributeSet;

/**
 * A normal LinearLayout which becomes movable after a long press anywhere
 * inside its bounds. Child buttons and sliders continue to receive ordinary
 * taps and drags; the parent takes over only after the long-press timeout.
 */
public class DraggablePanel extends LinearLayout {
    private static final long LONG_PRESS_TIMEOUT_MS = 500L;

    private final Handler handler = new Handler();
    private final int touchSlop;
    private float downRawX;
    private float downRawY;
    private float startX;
    private float startY;
    private boolean longPressReady;
    private boolean dragging;
    private boolean moved;

    private final Runnable armDrag = new Runnable() {
        @Override
        public void run() {
            longPressReady = true;
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
    };

    public DraggablePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setOrientation(LinearLayout.VERTICAL);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                startX = getX();
                startY = getY();
                longPressReady = false;
                dragging = false;
                moved = false;
                handler.removeCallbacks(armDrag);
                handler.postDelayed(armDrag, LONG_PRESS_TIMEOUT_MS);
                return false;

            case MotionEvent.ACTION_MOVE:
                if (dragging) return true;
                if (longPressReady) {
                    dragging = true;
                    moved = true;
                    setAlpha(0.92f);
                    bringToFront();
                    return true;
                }
                if (Math.abs(event.getRawX() - downRawX) > touchSlop
                        || Math.abs(event.getRawY() - downRawY) > touchSlop) {
                    handler.removeCallbacks(armDrag);
                    longPressReady = false;
                }
                return false;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(armDrag);
                if (longPressReady) {
                    dragging = true;
                    return true;
                }
                longPressReady = false;
                return false;

            default:
                return dragging;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Used when the user presses an empty part of the panel.
                return true;

            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    float dx = event.getRawX() - downRawX;
                    float dy = event.getRawY() - downRawY;
                    float maxX = Math.max(0, getParent() == null
                            ? getX() : ((android.view.View) getParent()).getWidth() - getWidth());
                    float maxY = Math.max(0, getParent() == null
                            ? getY() : ((android.view.View) getParent()).getHeight() - getHeight());
                    setX(Math.max(0, Math.min(maxX, startX + dx)));
                    setY(Math.max(0, Math.min(maxY, startY + dy)));
                    moved = true;
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(armDrag);
                if (dragging && moved && getParent() instanceof PanelWorkspace) {
                    ((PanelWorkspace) getParent()).onPanelDragFinished(this, startX, startY);
                }
                finishGesture();
                return true;

            default:
                return true;
        }
    }

    /**
     * A SeekBar may ask its parent not to intercept a drag. We intentionally
     * ignore that request: quick movements still cancel the long press, while
     * a held press on any part of a panel can always enter panel-move mode.
     */
    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // Keep the delayed long-press gesture available over child controls.
    }

    private void finishGesture() {
        handler.removeCallbacks(armDrag);
        longPressReady = false;
        dragging = false;
        moved = false;
        setAlpha(1.0f);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(armDrag);
        super.onDetachedFromWindow();
    }
}
