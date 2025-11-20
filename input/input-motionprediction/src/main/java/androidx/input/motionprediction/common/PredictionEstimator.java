/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.input.motionprediction.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 */
@SuppressWarnings("deprecation")
@RestrictTo(LIBRARY)
public class PredictionEstimator {
    private static final int MAX_PREDICTION_MS = 32;
    private static final int MS_IN_A_SECOND = 1000;

    private long mLastEventTime = -1;
    private final float mFrameTimeMs;
    private final int mOffsetMs;

    public PredictionEstimator(@NonNull Context context) {
        mFrameTimeMs = getFastestFrameTimeMs(context);
        mOffsetMs = Configuration.getInstance().predictionOffset();
    }

    /** Records the needed information from the event to calculate the prediction. */
    public void record(@NonNull MotionEvent event) {
        mLastEventTime = event.getEventTime();
    }

    /** Return the estimated amount of prediction needed. */
    public int estimate() {
        if (mLastEventTime <= 0) {
            return ((int) mFrameTimeMs) + mOffsetMs;
        }
        // The amount of prediction is the estimated amount of time it will take to land the
        // information on the screen from now, plus the time since the last recorded MotionEvent
        int estimatedMs = (int) (SystemClock.uptimeMillis() - mLastEventTime + mFrameTimeMs);
        return Math.min(MAX_PREDICTION_MS, estimatedMs + mOffsetMs);
    }

    private Display getDisplayForContext(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Api30Impl.getDisplayForContext(context);
        }
        return ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
    }

    private float getFastestFrameTimeMs(Context context) {
        Display defaultDisplay = getDisplayForContext(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Api23Impl.getFastestFrameTimeMs(defaultDisplay);
        } else {
            return Api21Impl.getFastestFrameTimeMs(defaultDisplay);
        }
    }

    @SuppressWarnings("deprecation")
    static class Api21Impl {
        private Api21Impl() {
            // Not instantiable
        }

        static float getFastestFrameTimeMs(Display display) {
            float[] refreshRates = display.getSupportedRefreshRates();
            float largestRefreshRate = refreshRates[0];

            for (int c = 1; c < refreshRates.length; c++) {
                if (refreshRates[c] > largestRefreshRate) {
                    largestRefreshRate = refreshRates[c];
                }
            }

            return MS_IN_A_SECOND / largestRefreshRate;
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    static class Api23Impl {
        private Api23Impl() {
            // Not instantiable
        }

        static float getFastestFrameTimeMs(Display display) {
            Display.Mode[] displayModes = display.getSupportedModes();
            float largestRefreshRate = displayModes[0].getRefreshRate();

            for (int c = 1; c < displayModes.length; c++) {
                float currentRefreshRate = displayModes[c].getRefreshRate();
                if (currentRefreshRate > largestRefreshRate) {
                    largestRefreshRate = currentRefreshRate;
                }
            }

            return MS_IN_A_SECOND / largestRefreshRate;
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    static class Api30Impl {
        private Api30Impl() {
            // Not instantiable
        }

        static Display getDisplayForContext(Context context) {
            return context.getDisplay();
        }
    }
}
