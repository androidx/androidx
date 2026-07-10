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

import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import kotlin.jvm.JvmStatic

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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@ExperimentalInkAnimationApi
public object TextureAnimationProgressHelper {

    /**
     * Extract the first non-zero animation duration from a [BrushPaint]. If it does not support
     * animation, then a duration of 0 will be returned.
     */
    @JvmStatic
    @IntRange(from = 0, to = 1 shl 24)
    public fun getAnimationDurationMillis(brushPaint: BrushPaint): Long {
        for (textureLayer in brushPaint.textureLayers) {
            when (textureLayer) {
                is BrushPaint.StampingTexture -> {
                    if (textureLayer.animationFrames > 1) {
                        return textureLayer.animationDurationMillis
                    }
                }
            }
        }
        return 0L
    }
}
