package com.shajikhan.winampskin;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.TextView;

import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Hosts the three Winamp panels and lets the user rearrange them by
 * long-pressing the small grip in each panel's title strip.
 *
 * Positions are stored independently for portrait and landscape and are
 * clamped to the visible workspace. Panels magnetically align to one another
 * or sit 5 mm apart when their edges approach within 5 mm.
 */
public class PanelWorkspace extends FrameLayout {
    private static final String PREFS_NAME = "xenamp_panel_positions";
    private static final long LONG_PRESS_MS = 500L;
    private static final float SNAP_MM = 5.0f;

    private final Handler handler = new Handler();
    private final SharedPreferences preferences;
    private final List<PanelEntry> entries = new ArrayList<>();
    private final int touchSlop;
    private int snapDistancePx;
    private boolean configured;
    private View mainPanel;
    private View equalizerPanel;
    private View playlistPanel;

    public PanelWorkspace(Context context, AttributeSet attrs) {
        super(context, attrs);
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        snapDistancePx = Math.round(SNAP_MM * getResources().getDisplayMetrics().densityDpi / 25.4f);
        setClipChildren(true);
    }

    public void arrangePanels(final View main, final View equalizer,
                              final View playlist, final int panelWidthPx,
                              final int panelHeightPx) {
        mainPanel = main;
        equalizerPanel = equalizer;
        playlistPanel = playlist;
        post(new Runnable() {
            @Override
            public void run() {
                applyLayout(panelWidthPx, panelHeightPx);
            }
        });
    }

    private void applyLayout(int requestedPanelWidth, int requestedPanelHeight) {
        final int workspaceWidth = getWidth();
        final int workspaceHeight = getHeight();
        if (workspaceWidth <= 0 || workspaceHeight <= 0
                || mainPanel == null || equalizerPanel == null || playlistPanel == null) {
            return;
        }

        final float density = getResources().getDisplayMetrics().density;
        final int gap = Math.max(1, snapDistancePx);
        final int panelWidth = Math.min(workspaceWidth, requestedPanelWidth);
        final int panelHeight = Math.min(workspaceHeight, requestedPanelHeight);
        final boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        final int remainingWidth = workspaceWidth - panelWidth - gap;
        final boolean sideBySide = landscape && remainingWidth >= dp(180);

        final String orientationKey = landscape ? "landscape_" : "portrait_";
        final int mainDefaultX = sideBySide ? 0 : Math.max(0, (workspaceWidth - panelWidth) / 2);
        final int mainDefaultY = 0;
        final int equalizerDefaultX = mainDefaultX;
        final int equalizerDefaultY = Math.min(workspaceHeight, panelHeight + gap);
        final int playlistDefaultX = sideBySide ? panelWidth + gap : 0;
        final int playlistDefaultY = sideBySide ? 0
                : Math.min(workspaceHeight, panelHeight * 2 + gap * 2);
        final int playlistWidth = sideBySide ? remainingWidth : workspaceWidth;
        final int playlistHeight = sideBySide ? workspaceHeight
                : Math.max(0, workspaceHeight - playlistDefaultY);

        configurePanel(mainPanel, "main", orientationKey,
                panelWidth, panelHeight, mainDefaultX, mainDefaultY);
        configurePanel(equalizerPanel, "equalizer", orientationKey,
                panelWidth, panelHeight, equalizerDefaultX, equalizerDefaultY);
        configurePanel(playlistPanel, "playlist", orientationKey,
                playlistWidth, playlistHeight, playlistDefaultX, playlistDefaultY);

        configured = true;
        for (PanelEntry entry : entries) {
            positionHandle(entry);
        }
    }

    private void configurePanel(View panel, String key, String orientationKey,
                                int width, int height, int defaultX, int defaultY) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                width, height, Gravity.TOP | Gravity.LEFT);
        panel.setLayoutParams(params);

        float density = getResources().getDisplayMetrics().density;
        float x = defaultX;
        float y = defaultY;
        String xKey = orientationKey + key + "_x";
        String yKey = orientationKey + key + "_y";
        if (preferences.contains(xKey) && preferences.contains(yKey)) {
            x = preferences.getFloat(xKey, defaultX / density) * density;
            y = preferences.getFloat(yKey, defaultY / density) * density;
        }
        panel.setX(clamp(x, 0, Math.max(0, getWidth() - width)));
        panel.setY(clamp(y, 0, Math.max(0, getHeight() - height)));

        PanelEntry entry = findEntry(key);
        if (entry == null) {
            entry = new PanelEntry(key, panel);
            entries.add(entry);
            addView(entry.handle, new FrameLayout.LayoutParams(
                    dp(46), dp(18), Gravity.TOP | Gravity.LEFT));
        } else {
            entry.panel = panel;
        }
        positionHandle(entry);
    }

    private PanelEntry findEntry(String key) {
        for (PanelEntry entry : entries) {
            if (entry.key.equals(key)) return entry;
        }
        return null;
    }

    private void positionHandle(PanelEntry entry) {
        if (entry.handle.getParent() != this || entry.panel.getWidth() <= 0) return;
        entry.handle.setX(entry.panel.getX()
                + Math.max(0, (entry.panel.getWidth() - entry.handle.getWidth()) / 2f));
        entry.handle.setY(entry.panel.getY() + dp(1));
        entry.handle.bringToFront();
    }

    private class PanelEntry {
        final String key;
        View panel;
        final TextView handle;
        float downRawX;
        float downRawY;
        float panelStartX;
        float panelStartY;
        boolean dragging;
        boolean movedBeforeLongPress;
        final Runnable beginDrag;

        PanelEntry(String key, View panel) {
            this.key = key;
            this.panel = panel;
            handle = new TextView(getContext());
            handle.setText("•••");
            handle.setTextSize(10);
            handle.setTextColor(Color.WHITE);
            handle.setGravity(Gravity.CENTER);
            handle.setIncludeFontPadding(false);
            GradientDrawable background = new GradientDrawable();
            background.setColor(0xAA303030);
            background.setCornerRadius(dp(6));
            background.setStroke(Math.max(1, dp(1)), 0x88FFFFFF);
            handle.setBackgroundDrawable(background);
            handle.setAlpha(0.72f);

            beginDrag = new Runnable() {
                @Override
                public void run() {
                    if (!movedBeforeLongPress) {
                        dragging = true;
                        panel.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        panel.bringToFront();
                        handle.bringToFront();
                        handle.setAlpha(1.0f);
                    }
                }
            };

            handle.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            downRawX = event.getRawX();
                            downRawY = event.getRawY();
                            panelStartX = panel.getX();
                            panelStartY = panel.getY();
                            dragging = false;
                            movedBeforeLongPress = false;
                            handler.removeCallbacks(beginDrag);
                            handler.postDelayed(beginDrag, LONG_PRESS_MS);
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getRawX() - downRawX;
                            float dy = event.getRawY() - downRawY;
                            if (!dragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                                movedBeforeLongPress = true;
                                handler.removeCallbacks(beginDrag);
                            }
                            if (dragging) {
                                panel.setX(clamp(panelStartX + dx, 0,
                                        Math.max(0, getWidth() - panel.getWidth())));
                                panel.setY(clamp(panelStartY + dy, 0,
                                        Math.max(0, getHeight() - panel.getHeight())));
                                positionHandle(PanelEntry.this);
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            handler.removeCallbacks(beginDrag);
                            if (dragging) {
                                snapToNeighbours(PanelEntry.this);
                                savePosition(PanelEntry.this);
                            }
                            dragging = false;
                            handle.setAlpha(0.72f);
                            positionHandle(PanelEntry.this);
                            return true;
                    }
                    return true;
                }
            });
        }
    }

    private void snapToNeighbours(PanelEntry moving) {
        final int gap = snapDistancePx;
        float x = moving.panel.getX();
        float y = moving.panel.getY();
        float bestX = x;
        float bestY = y;
        float bestDx = snapDistancePx + 1;
        float bestDy = snapDistancePx + 1;
        int movingWidth = moving.panel.getWidth();
        int movingHeight = moving.panel.getHeight();

        for (PanelEntry other : entries) {
            if (other == moving || other.panel.getWidth() <= 0) continue;
            float ox = other.panel.getX();
            float oy = other.panel.getY();
            int ow = other.panel.getWidth();
            int oh = other.panel.getHeight();

            float[] xCandidates = {
                    ox,
                    ox + ow - movingWidth,
                    ox + (ow - movingWidth) / 2f,
                    ox + ow + gap,
                    ox - gap - movingWidth
            };
            float[] yCandidates = {
                    oy,
                    oy + oh - movingHeight,
                    oy + (oh - movingHeight) / 2f,
                    oy + oh + gap,
                    oy - gap - movingHeight
            };

            for (float candidate : xCandidates) {
                float distance = Math.abs(x - candidate);
                if (distance <= snapDistancePx && distance < bestDx) {
                    bestDx = distance;
                    bestX = candidate;
                }
            }
            for (float candidate : yCandidates) {
                float distance = Math.abs(y - candidate);
                if (distance <= snapDistancePx && distance < bestDy) {
                    bestDy = distance;
                    bestY = candidate;
                }
            }
        }

        moving.panel.setX(clamp(bestX, 0, Math.max(0, getWidth() - movingWidth)));
        moving.panel.setY(clamp(bestY, 0, Math.max(0, getHeight() - movingHeight)));
        positionHandle(moving);
    }

    private void savePosition(PanelEntry entry) {
        float density = getResources().getDisplayMetrics().density;
        String orientationKey = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE ? "landscape_" : "portrait_";
        preferences.edit()
                .putFloat(orientationKey + entry.key + "_x", entry.panel.getX() / density)
                .putFloat(orientationKey + entry.key + "_y", entry.panel.getY() / density)
                .apply();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
