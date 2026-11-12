package com.shajikhan.winampskin;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Positions the three independent Winamp panels. A panel can be moved by
 * long-pressing anywhere on it. On release, it snaps to a neighbour only
 * when the result is aligned and leaves a real 5 mm gap.
 */
public class PanelWorkspace extends FrameLayout {
    private static final String PREFS_NAME = "xenamp_panel_positions";
    private static final float SNAP_MM = 5.0f;

    private final SharedPreferences preferences;
    private final List<PanelEntry> entries = new ArrayList<>();
    private int snapDistancePx;
    private int requestedPanelWidth;
    private int requestedPanelHeight;
    private View mainPanel;
    private View equalizerPanel;
    private View playlistPanel;

    public PanelWorkspace(Context context, AttributeSet attrs) {
        super(context, attrs);
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        snapDistancePx = Math.max(1, Math.round(
                SNAP_MM * getResources().getDisplayMetrics().densityDpi / 25.4f));
        setClipChildren(true);
    }

    public void arrangePanels(final View main, final View equalizer,
                              final View playlist, final int panelWidthPx,
                              final int panelHeightPx) {
        mainPanel = main;
        equalizerPanel = equalizer;
        playlistPanel = playlist;
        requestedPanelWidth = panelWidthPx;
        requestedPanelHeight = panelHeightPx;
        post(new Runnable() {
            @Override
            public void run() {
                applyLayout(panelWidthPx, panelHeightPx);
            }
        });
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (requestedPanelWidth > 0 && requestedPanelHeight > 0) {
            post(new Runnable() {
                @Override
                public void run() {
                    applyLayout(requestedPanelWidth, requestedPanelHeight);
                }
            });
        }
    }

    private void applyLayout(int requestedWidth, int requestedHeight) {
        int workspaceWidth = getWidth();
        int workspaceHeight = getHeight();
        if (workspaceWidth <= 0 || workspaceHeight <= 0
                || mainPanel == null || equalizerPanel == null || playlistPanel == null) {
            return;
        }

        int gap = snapDistancePx;
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        // Leave room for the playlist in portrait, while keeping the two
        // Winamp-sized panels at their natural aspect ratio.
        int minimumPlaylistHeight = landscape ? 0 : dp(90);
        int maxPanelHeight = Math.max(dp(64),
                (workspaceHeight - 2 * gap - minimumPlaylistHeight) / 2);
        int panelHeight = Math.min(requestedHeight, maxPanelHeight);
        int panelWidth = Math.min(Math.min(requestedWidth, workspaceWidth),
                Math.round(panelHeight * 275f / 116f));

        int remainingWidth = workspaceWidth - panelWidth - gap;
        boolean sideBySide = landscape && remainingWidth >= dp(180);
        String orientationKey = landscape ? "landscape_" : "portrait_";

        int mainDefaultX = sideBySide ? 0 : Math.max(0, (workspaceWidth - panelWidth) / 2);
        int mainDefaultY = 0;
        int equalizerDefaultX = mainDefaultX;
        int equalizerDefaultY = panelHeight + gap;
        int playlistDefaultX = sideBySide ? panelWidth + gap : 0;
        int playlistDefaultY = sideBySide ? 0 : 2 * (panelHeight + gap);
        int playlistWidth = sideBySide ? remainingWidth : workspaceWidth;
        int playlistHeight = sideBySide ? workspaceHeight
                : Math.max(0, workspaceHeight - playlistDefaultY);

        configurePanel(mainPanel, "main", orientationKey, panelWidth, panelHeight,
                mainDefaultX, mainDefaultY);
        configurePanel(equalizerPanel, "equalizer", orientationKey, panelWidth, panelHeight,
                equalizerDefaultX, equalizerDefaultY);
        configurePanel(playlistPanel, "playlist", orientationKey, playlistWidth, playlistHeight,
                playlistDefaultX, playlistDefaultY);

        // Old saved positions may have been created by the previous snapping
        // implementation, which allowed intersections. Discard only positions
        // that collide, restoring those panels to their safe default locations.
        restoreNonOverlappingDefaults();
    }

    private void configurePanel(View panel, String key, String orientationKey,
                                int width, int height, int defaultX, int defaultY) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                width, height, Gravity.TOP | Gravity.LEFT);
        panel.setLayoutParams(params);

        float density = getResources().getDisplayMetrics().density;
        String xKey = orientationKey + key + "_x";
        String yKey = orientationKey + key + "_y";
        float x = defaultX;
        float y = defaultY;
        if (preferences.contains(xKey) && preferences.contains(yKey)) {
            x = preferences.getFloat(xKey, defaultX / density) * density;
            y = preferences.getFloat(yKey, defaultY / density) * density;
        }

        panel.setX(clamp(x, 0, Math.max(0, getWidth() - width)));
        panel.setY(clamp(y, 0, Math.max(0, getHeight() - height)));

        PanelEntry entry = findEntry(panel);
        if (entry == null) {
            entry = new PanelEntry(key, panel, defaultX, defaultY);
            entries.add(entry);
        } else {
            entry.panel = panel;
            entry.defaultX = defaultX;
            entry.defaultY = defaultY;
        }
    }

    private PanelEntry findEntry(View panel) {
        for (PanelEntry entry : entries) {
            if (entry.panel == panel) return entry;
        }
        return null;
    }

    private void restoreNonOverlappingDefaults() {
        for (int i = 0; i < entries.size(); i++) {
            PanelEntry entry = entries.get(i);
            if (!hasOverlapWithEarlierEntries(entry, i)) continue;
            entry.panel.setX(clamp(entry.defaultX, 0,
                    Math.max(0, getWidth() - panelWidth(entry.panel))));
            entry.panel.setY(clamp(entry.defaultY, 0,
                    Math.max(0, getHeight() - panelHeight(entry.panel))));
        }
    }

    private boolean hasOverlapWithEarlierEntries(PanelEntry entry, int entryIndex) {
        for (int i = 0; i < entryIndex; i++) {
            if (intersects(entry.panel.getX(), entry.panel.getY(),
                    panelWidth(entry.panel), panelHeight(entry.panel),
                    entries.get(i).panel)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Called by DraggablePanel after a long-press drag ends. Candidate layouts
     * are considered as complete (x,y) pairs, never as independent axis snaps.
     */
    void onPanelDragFinished(DraggablePanel panel, float startX, float startY) {
        PanelEntry moving = findEntry(panel);
        if (moving == null) return;

        float currentX = clamp(panel.getX(), 0, Math.max(0, getWidth() - panelWidth(panel)));
        float currentY = clamp(panel.getY(), 0, Math.max(0, getHeight() - panelHeight(panel)));
        float[] best = new float[]{currentX, currentY, Float.MAX_VALUE};

        int movingWidth = panelWidth(panel);
        int movingHeight = panelHeight(panel);

        for (PanelEntry other : entries) {
            if (other == moving) continue;
            View neighbour = other.panel;
            int neighbourWidth = panelWidth(neighbour);
            int neighbourHeight = panelHeight(neighbour);
            float nx = neighbour.getX();
            float ny = neighbour.getY();

            // Put the moving panel to the left or right of the neighbour,
            // aligning top edges, bottom edges, or vertical centres.
            float[] alignedY = {
                    ny,
                    ny + neighbourHeight - movingHeight,
                    ny + (neighbourHeight - movingHeight) / 2f
            };
            float leftX = nx - movingWidth - snapDistancePx;
            float rightX = nx + neighbourWidth + snapDistancePx;
            for (float aligned : alignedY) {
                considerCandidate(panel, currentX, currentY, leftX, aligned, best);
                considerCandidate(panel, currentX, currentY, rightX, aligned, best);
            }

            // Put the moving panel above or below the neighbour,
            // aligning left edges, right edges, or horizontal centres.
            float[] alignedX = {
                    nx,
                    nx + neighbourWidth - movingWidth,
                    nx + (neighbourWidth - movingWidth) / 2f
            };
            float aboveY = ny - movingHeight - snapDistancePx;
            float belowY = ny + neighbourHeight + snapDistancePx;
            for (float aligned : alignedX) {
                considerCandidate(panel, currentX, currentY, aligned, aboveY, best);
                considerCandidate(panel, currentX, currentY, aligned, belowY, best);
            }
        }

        if (best[2] < Float.MAX_VALUE) {
            panel.setX(best[0]);
            panel.setY(best[1]);
        } else if (!isPositionFree(panel, currentX, currentY)) {
            // Never persist a layout in which two windows overlap.
            panel.setX(clamp(startX, 0, Math.max(0, getWidth() - movingWidth)));
            panel.setY(clamp(startY, 0, Math.max(0, getHeight() - movingHeight)));
        } else {
            panel.setX(currentX);
            panel.setY(currentY);
        }

        savePosition(moving);
        panel.bringToFront();
    }

    private void considerCandidate(DraggablePanel moving, float currentX, float currentY,
                                   float candidateX, float candidateY, float[] best) {
        float dx = Math.abs(currentX - candidateX);
        float dy = Math.abs(currentY - candidateY);
        if (dx > snapDistancePx || dy > snapDistancePx) return;

        int width = panelWidth(moving);
        int height = panelHeight(moving);
        if (candidateX < 0 || candidateY < 0
                || candidateX + width > getWidth()
                || candidateY + height > getHeight()) return;
        if (!isPositionFree(moving, candidateX, candidateY)) return;

        float distanceSquared = dx * dx + dy * dy;
        if (distanceSquared < best[2]) {
            best[0] = candidateX;
            best[1] = candidateY;
            best[2] = distanceSquared;
        }
    }

    private boolean isPositionFree(View moving, float x, float y) {
        int width = panelWidth(moving);
        int height = panelHeight(moving);
        for (PanelEntry entry : entries) {
            if (entry.panel == moving) continue;
            if (intersects(x, y, width, height, entry.panel)) return false;
        }
        return true;
    }

    private boolean intersects(float x, float y, int width, int height, View other) {
        float otherX = other.getX();
        float otherY = other.getY();
        int otherWidth = panelWidth(other);
        int otherHeight = panelHeight(other);
        return x < otherX + otherWidth && x + width > otherX
                && y < otherY + otherHeight && y + height > otherY;
    }

    private int panelWidth(View panel) {
        if (panel.getLayoutParams() != null && panel.getLayoutParams().width >= 0) {
            return panel.getLayoutParams().width;
        }
        return panel.getWidth();
    }

    private int panelHeight(View panel) {
        if (panel.getLayoutParams() != null && panel.getLayoutParams().height >= 0) {
            return panel.getLayoutParams().height;
        }
        return panel.getHeight();
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

    private static class PanelEntry {
        final String key;
        View panel;
        int defaultX;
        int defaultY;

        PanelEntry(String key, View panel, int defaultX, int defaultY) {
            this.key = key;
            this.panel = panel;
            this.defaultX = defaultX;
            this.defaultY = defaultY;
        }
    }
}
