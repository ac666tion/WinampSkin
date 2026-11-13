package com.shajikhan.winampskin;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AbsSeekBar;
import android.widget.LinearLayout;

/**
 * A normal LinearLayout that can be moved by long-pressing anywhere on it.
 * Ordinary button taps and SeekBar movements are passed to the child views.
 * The panel takes over the gesture only after a stationary long press.
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
    private boolean resizingHandleTouch;
    private boolean interactiveChildTouch;

    private final Runnable armDrag = new Runnable() {
        @Override
        public void run() {
            longPressReady = true;
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
                resizingHandleTouch = isOverResizeHandle(event.getRawX(), event.getRawY());
                interactiveChildTouch = isInteractiveChildAt(event.getX(), event.getY());
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                startX = getX();
                startY = getY();
                longPressReady = false;
                dragging = false;
                moved = false;
                handler.removeCallbacks(armDrag);
                if (!interactiveChildTouch) {
                    handler.postDelayed(armDrag, LONG_PRESS_TIMEOUT_MS);
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                if (interactiveChildTouch || resizingHandleTouch) return false;
                if (dragging) return true;
                if (longPressReady) {
                    dragging = true;
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
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
                if (interactiveChildTouch) {
                    interactiveChildTouch = false;
                    handler.removeCallbacks(armDrag);
                    longPressReady = false;
                    return false;
                }
                if (resizingHandleTouch) {
                    resizingHandleTouch = false;
                    handler.removeCallbacks(armDrag);
                    longPressReady = false;
                    return false;
                }
                // A long press without a following movement is just a hold,
                // not a drag. Leave the child's normal click/tap behaviour intact.
                handler.removeCallbacks(armDrag);
                longPressReady = false;
                return false;

            default:
                return dragging;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    float dx = event.getRawX() - downRawX;
                    float dy = event.getRawY() - downRawY;
                    View parent = (View) getParent();
                    float maxX = Math.max(0, parent.getWidth() - getWidth());
                    float maxY = Math.max(0, parent.getHeight() - getHeight());
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
     * Do not let a child SeekBar permanently disable interception. Quick drags
     * cancel the pending long-press; a held press can still move its whole panel.
     */
    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // Intentionally ignored: long-press panel dragging remains available.
    }

    /**
     * Buttons, sliders and the playlist are always owned by their child views.
     * This prevents the panel's long-press drag gesture from stealing ordinary
     * taps, seek gestures, popup-menu buttons or playlist row selection.
     */
    private boolean isInteractiveChildAt(float x, float y) {
        return findInteractiveChild(this, x, y);
    }

    private boolean findInteractiveChild(ViewGroup parent, float x, float y) {
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            View child = parent.getChildAt(i);
            float left = child.getLeft() - parent.getScrollX();
            float top = child.getTop() - parent.getScrollY();
            float right = left + child.getWidth();
            float bottom = top + child.getHeight();
            if (x < left || x >= right || y < top || y >= bottom) continue;

            float childX = x - left;
            float childY = y - top;
            if (child instanceof ViewGroup
                    && findInteractiveChild((ViewGroup) child, childX, childY)) {
                return true;
            }
            if (child.isClickable() || child.isLongClickable()
                    || child instanceof AbsSeekBar || child instanceof AdapterView) {
                return true;
            }
        }
        return false;
    }

    private boolean isOverResizeHandle(float rawX, float rawY) {
        View handle = findViewById(R.id.playlist_resize_handle);
        if (handle == null || handle.getVisibility() != View.VISIBLE) return false;
        int[] location = new int[2];
        handle.getLocationOnScreen(location);
        return rawX >= location[0] && rawX <= location[0] + handle.getWidth()
                && rawY >= location[1] && rawY <= location[1] + handle.getHeight();
    }

    private void finishGesture() {
        handler.removeCallbacks(armDrag);
        longPressReady = false;
        dragging = false;
        moved = false;
        interactiveChildTouch = false;
        resizingHandleTouch = false;
        setAlpha(1.0f);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(armDrag);
        super.onDetachedFromWindow();
    }
}
