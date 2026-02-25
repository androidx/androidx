/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.brush

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo

/**
 * Utilities to help with texture animation progress calculations.
 *
 * TODO: b/398881704 - Each coat and each paint preference within a coat can have its own animation
 *   duration. The animation progress needs to be calculated by the renderer for a particular coat
 *   and the particular paint that is being used for rendering.
 *
 * Currently, all texture layers of a coat are required to have the same animation parameters.
 *
 * TODO: b/267164444 - Support texture layers within the same coat having different animation specs.
 */
@ExperimentalInkCustomBrushApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
public object TextureAnimationProgressHelper {

    /**
     * Returns the progress value in [0, 1) for the animation specified by [brushFamily] at the
     * timestamp [systemElapsedTimeMillis], which is in the [android.os.SystemClock.uptimeMillis]
     * time base, and typically comes from [android.view.Choreographer.FrameCallback.doFrame]. If
     * [brushFamily] does not support animation, then a progress value of 0 will be returned.
     */
    @FloatRange(from = 0.0, to = 1.0, toInclusive = false)
    public fun calculateAnimationProgress(
        systemElapsedTimeMillis: Long,
        brushFamily: BrushFamily,
    ): Float =
        calculateAnimationProgress(systemElapsedTimeMillis, getAnimationDurationMillis(brushFamily))

    /**
     * Returns the progress value in [0, 1) for an animation with duration [animationDurationMillis]
     * at the timestamp [systemElapsedTimeMillis], which is in the
     * [android.os.SystemClock.uptimeMillis] time base, and typically comes from
     * [android.view.Choreographer.FrameCallback.doFrame]. If [animationDurationMillis] is 0, then a
     * progress value of 0 will be returned.
     */
    @FloatRange(from = 0.0, to = 1.0, toInclusive = false)
    public fun calculateAnimationProgress(
        systemElapsedTimeMillis: Long,
        animationDurationMillis: Long,
    ): Float {
        if (animationDurationMillis == 0L) {
            return 0F
        }
        // This is mostly equivalent to:
        // (shape.lastUpdateSystemElapsedTimeMillis / textureAnimationDurationMillis.toFloat()) % 1f
        // But doing the modulo operation first, before any conversion to float, avoids significant
        // loss
        // of precision, since the system uptime in practice can be much larger than the animation
        // duration.
        return (systemElapsedTimeMillis % animationDurationMillis) /
            animationDurationMillis.toFloat()
    }

    /**
     * Extract the first non-zero animation duration from a [BrushFamily]. If it does not support
     * animation, then a duration of 0 will be returned.
     */
    @IntRange(from = 0)
    public fun getAnimationDurationMillis(brushFamily: BrushFamily): Long {
        for (coat in brushFamily.coats) {
            for (paintPreference in coat.paintPreferences) {
                for (textureLayer in paintPreference.textureLayers) {
                    if (textureLayer.animationFrames > 1) {
                        return textureLayer.animationDurationMillis
                    }
                }
            }
        }
        return 0
    }
}
