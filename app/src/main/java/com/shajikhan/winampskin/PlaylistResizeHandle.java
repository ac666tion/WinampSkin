package com.shajikhan.winampskin;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

/** Small bottom-right grip for resizing the playlist window. */
public class PlaylistResizeHandle extends TextView {
    private float downX;
    private float downY;
    private int startWidth;
    private int startHeight;
    private boolean resizing;

    public PlaylistResizeHandle(Context context, AttributeSet attrs) {
        super(context, attrs);
        setText("◢");
        setTextSize(18);
        setGravity(android.view.Gravity.CENTER);
        setTextColor(0xffffffff);
        setContentDescription("Resize playlist window");
        setClickable(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        View row = (View) getParent();
        View panel = (View) row.getParent();
        if (!(panel.getParent() instanceof PanelWorkspace)) return false;
        PanelWorkspace workspace = (PanelWorkspace) panel.getParent();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getRawX();
                downY = event.getRawY();
                startWidth = panel.getWidth();
                startHeight = panel.getHeight();
                resizing = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!resizing) return false;
                int width = startWidth + Math.round(event.getRawX() - downX);
                int height = startHeight + Math.round(event.getRawY() - downY);
                workspace.resizePlaylist(panel, width, height);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                resizing = false;
                performClick();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
