package com.shajikhan.winampskin;

import android.media.audiofx.Equalizer;
import android.util.Log;

/**
 * Android-legacy-compatible 10-slider adapter backed by the platform Equalizer.
 * Older Android devices commonly expose fewer than ten physical bands; each
 * Winamp slider is mapped to the nearest available hardware/effect band.
 */
public class WinampEqualizer {
    private static final String TAG = "WinampEqualizer";
    private static final int[] TARGET_FREQUENCIES = {
            60, 170, 310, 600, 1000, 3000, 6000, 12000, 14000, 16000
    };

    private final Equalizer equalizer;
    private final short[] sliderBands = new short[TARGET_FREQUENCIES.length];
    private final short[] levelRange;

    WinampEqualizer(int audioSessionId) {
        Equalizer candidate = null;
        short[] range = new short[]{-1500, 1500};
        try {
            candidate = new Equalizer(0, audioSessionId);
            range = candidate.getBandLevelRange();
            mapSliders(candidate);
            candidate.setEnabled(true);
        } catch (RuntimeException e) {
            Log.w(TAG, "System equalizer is unavailable; sliders will be inactive", e);
            if (candidate != null) {
                try { candidate.release(); } catch (RuntimeException ignored) { }
            }
            candidate = null;
        }
        equalizer = candidate;
        levelRange = range;
    }

    private void mapSliders(Equalizer eq) {
        short count = eq.getNumberOfBands();
        for (int i = 0; i < TARGET_FREQUENCIES.length; i++) {
            short best = 0;
            long bestDistance = Long.MAX_VALUE;
            for (short band = 0; band < count; band++) {
                long centerHz = eq.getCenterFreq(band) / 1000L;
                long distance = Math.abs(centerHz - TARGET_FREQUENCIES[i]);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = band;
                }
            }
            sliderBands[i] = best;
        }
    }

    public int getSliderProgress(int sliderIndex) {
        if (equalizer == null) return 50;
        try {
            short value = equalizer.getBandLevel(sliderBands[sliderIndex]);
            int min = levelRange[0];
            int max = levelRange[1];
            if (max <= min) return 50;
            return Math.max(0, Math.min(100, (value - min) * 100 / (max - min)));
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to read equalizer band", e);
            return 50;
        }
    }

    public void setSliderProgress(int sliderIndex, int progress) {
        if (equalizer == null) return;
        try {
            int bounded = Math.max(0, Math.min(100, progress));
            int min = levelRange[0];
            int max = levelRange[1];
            short value = (short) (min + ((max - min) * bounded / 100));
            equalizer.setBandLevel(sliderBands[sliderIndex], value);
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to change equalizer band", e);
        }
    }

    public void destroy() {
        if (equalizer != null) {
            try { equalizer.release(); } catch (RuntimeException ignored) { }
        }
    }
}
