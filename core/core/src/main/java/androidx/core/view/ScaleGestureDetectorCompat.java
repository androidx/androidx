/*
 * Copyright 2018 The Android Open Source Project
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

import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;

/**
 * Helper for accessing features in {@link ScaleGestureDetector}.
 */
public final class ScaleGestureDetectorCompat {
    private ScaleGestureDetectorCompat() {}

    /**
     * Sets whether the associated {@link ScaleGestureDetector.OnScaleGestureListener} should
     * receive onScale callbacks when the user performs a doubleTap followed by a swipe. Note that
     * this is enabled by default if the app targets API 19 and newer.
     *
     * @param scaleGestureDetector detector for which to set the scaling mode.
     * @param enabled true to enable quick scaling, false to disable
     *
     * @deprecated Use {@link #setQuickScaleEnabled(ScaleGestureDetector, boolean)} that takes
     * {@link ScaleGestureDetector} instead of {@link Object}.
     */
    @Deprecated
    public static void setQuickScaleEnabled(Object scaleGestureDetector, boolean enabled) {
        ScaleGestureDetectorCompat.setQuickScaleEnabled(
                (ScaleGestureDetector) scaleGestureDetector, enabled);
    }

    /**
     * Sets whether the associated {@link ScaleGestureDetector.OnScaleGestureListener} should
     * receive onScale callbacks when the user performs a doubleTap followed by a swipe. Note that
     * this is enabled by default if the app targets API 19 and newer.
     *
     * @param scaleGestureDetector detector for which to set the scaling mode.
     * @param enabled true to enable quick scaling, false to disable
     * @deprecated Call {@link ScaleGestureDetector#setQuickScaleEnabled()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "scaleGestureDetector.setQuickScaleEnabled(enabled)")
    public static void setQuickScaleEnabled(
            @NonNull ScaleGestureDetector scaleGestureDetector, boolean enabled) {
        scaleGestureDetector.setQuickScaleEnabled(enabled);
    }

    /**
     * Returns whether the quick scale gesture, in which the user performs a double tap followed by
     * a swipe, should perform scaling. See
     * {@link #setQuickScaleEnabled(ScaleGestureDetector, boolean)}.
     *
     * @deprecated Use {@link #isQuickScaleEnabled(ScaleGestureDetector)} that takes
     * {@link ScaleGestureDetector} instead of {@link Object}.
     */
    @Deprecated
    public static boolean isQuickScaleEnabled(Object scaleGestureDetector) {
        return ScaleGestureDetectorCompat.isQuickScaleEnabled(
                (ScaleGestureDetector) scaleGestureDetector);
    }

    /**
     * Returns whether the quick scale gesture, in which the user performs a double tap followed by
     * a swipe, should perform scaling. See
     * {@link #setQuickScaleEnabled(ScaleGestureDetector, boolean)}.
     * @deprecated Call {@link ScaleGestureDetector#isQuickScaleEnabled()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "scaleGestureDetector.isQuickScaleEnabled()")
    public static boolean isQuickScaleEnabled(@NonNull ScaleGestureDetector scaleGestureDetector) {
        return scaleGestureDetector.isQuickScaleEnabled();
    }
}
