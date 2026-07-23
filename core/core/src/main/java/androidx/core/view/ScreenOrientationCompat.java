/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.core.view;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;
import androidx.annotation.UiContext;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.util.ObjectsCompat;

import org.jspecify.annotations.NonNull;

/**
 * Helper for resolving screen orientations.
 */
public final class ScreenOrientationCompat {

    private static volatile Boolean sCachedIsReverseDefaultRotation;

    private ScreenOrientationCompat() {}

    /**
     * Resolves a raw rotation angle into one of the four 4-way screen orientations relative to
     * the specified UI {@link Context}.
     * <p>
     * Raw rotation values (such as those from rotation sensors or {@link Display#getRotation()})
     * do not always map directly to standard screen orientations. For example, on
     * landscape-natural devices (such as many tablets and foldables), {@link Surface#ROTATION_0}
     * corresponds to landscape rather than portrait, and devices with reverse-default
     * rotation configurations invert 90-degree and 270-degree rotation directions.
     * <p>
     * <strong>Note:</strong> For general static layout decisions, applications should rely
     * directly on {@link Configuration#orientation} via
     * {@code context.getResources().getConfiguration().orientation}. Use this method when
     * converting rotation angles into {@link ActivityInfo} screen orientation constants.
     *
     * @param context        the visual UI {@link Context} (e.g., Activity or WindowContext)
     * @param targetRotation the rotation angle to evaluate, defined by {@link Surface} constants
     *                       (e.g., {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     *                       {@link Surface#ROTATION_180}, or {@link Surface#ROTATION_270})
     * @return one of the four screen orientation constants:
     *         {@link ActivityInfo#SCREEN_ORIENTATION_PORTRAIT},
     *         {@link ActivityInfo#SCREEN_ORIENTATION_LANDSCAPE},
     *         {@link ActivityInfo#SCREEN_ORIENTATION_REVERSE_PORTRAIT}, or
     *         {@link ActivityInfo#SCREEN_ORIENTATION_REVERSE_LANDSCAPE}
     */
    @SuppressWarnings("deprecation")
    public static int getScreenOrientationFromRotation(@NonNull @UiContext Context context,
            int targetRotation) {
        ObjectsCompat.requireNonNull(context, "context cannot be null");

        final Display display = ContextCompat.getDisplayOrDefault(context);
        final int currentRotation = display.getRotation();
        final Point size = new Point();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final Rect bounds = Api30Impl.getMaximumWindowMetricsBounds(context);
            size.set(bounds.width(), bounds.height());
        } else {
            display.getRealSize(size);
        }

        return resolveOrientation(size.x, size.y, currentRotation,
                isReverseDefaultRotation(context), targetRotation);
    }

    @VisibleForTesting
    static int resolveOrientation(int currentWidth, int currentHeight, int currentRotation,
            boolean isReverseDefault, int targetRotation) {
        // Determine unrotated baseline dimensions.
        final boolean isSideways = currentRotation == Surface.ROTATION_90
                || currentRotation == Surface.ROTATION_270;
        final int baseWidth = isSideways ? currentHeight : currentWidth;
        final int baseHeight = isSideways ? currentWidth : currentHeight;
        final int naturalOrientation = (baseWidth <= baseHeight)
                ? Configuration.ORIENTATION_PORTRAIT
                : Configuration.ORIENTATION_LANDSCAPE;

        if (naturalOrientation == Configuration.ORIENTATION_PORTRAIT) {
            if (!isReverseDefault) {
                switch (targetRotation) {
                    case Surface.ROTATION_90:
                        return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    case Surface.ROTATION_180:
                        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    case Surface.ROTATION_270:
                        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
            } else {
                switch (targetRotation) {
                    case Surface.ROTATION_90:
                        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    case Surface.ROTATION_180:
                        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    case Surface.ROTATION_270:
                        return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                }
            }
            return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        } else {
            if (!isReverseDefault) {
                switch (targetRotation) {
                    case Surface.ROTATION_90:
                        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    case Surface.ROTATION_180:
                        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    case Surface.ROTATION_270:
                        return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                }
            } else {
                switch (targetRotation) {
                    case Surface.ROTATION_90:
                        return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    case Surface.ROTATION_180:
                        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    case Surface.ROTATION_270:
                        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                }
            }
            return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
    }

    private static boolean isReverseDefaultRotation(@NonNull Context context) {
        if (sCachedIsReverseDefaultRotation != null) {
            return sCachedIsReverseDefaultRotation;
        }
        boolean isReverse = false;
        try {
            final int resId = context.getResources().getIdentifier(
                    "config_reverseDefaultRotation", "bool", "android");
            if (resId != 0) {
                isReverse = context.getResources().getBoolean(resId);
            }
        } catch (Exception e) {
            // Ignored
        }
        sCachedIsReverseDefaultRotation = isReverse;
        return isReverse;
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private static class Api30Impl {
        private Api30Impl() {}

        static Rect getMaximumWindowMetricsBounds(Context context) {
            return context.getSystemService(WindowManager.class).getMaximumWindowMetrics()
                    .getBounds();
        }
    }
}
