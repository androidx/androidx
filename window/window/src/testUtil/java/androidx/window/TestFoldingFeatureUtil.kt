/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.window

import android.graphics.Rect
import androidx.window.FoldingFeature.State.Companion.FLAT
import androidx.window.FoldingFeature.State.Companion.HALF_OPENED

/**
 * A class containing static methods for creating different window bound types. Test methods are
 * shared between the unit tests and the instrumentation tests.
 */
internal object TestFoldingFeatureUtil {
    /**
     * @param windowBounds the bounds of the window.
     * @return [Rect] that is a valid fold bound within the given window.
     */
    @JvmStatic
    fun validFoldBound(windowBounds: Rect): Rect {
        val verticalMid = windowBounds.height() / 2
        return Rect(0, verticalMid, windowBounds.width(), verticalMid)
    }

    /**
     * @return [Rect] containing the invalid zero bounds.
     */
    fun invalidZeroBound(): Rect {
        return Rect()
    }

    /**
     * @param windowBounds the bounds of the window.
     * @return [Rect] for bounds where the width is shorter than the window width.
     */
    fun invalidBoundShortWidth(windowBounds: Rect): Rect {
        return Rect(0, 0, windowBounds.width() / 2, 0)
    }

    /**
     * @param windowBounds the bounds of the window.
     * @return [Rect] for bounds where the height is shorter than the window height.
     */
    fun invalidBoundShortHeight(windowBounds: Rect): Rect {
        return Rect(0, 0, 0, windowBounds.height() / 2)
    }

    /**
     * @param windowBounds the bounds of the window.
     * @return a [List] of [Rect] of invalid bounds for folding features
     */
    @JvmStatic
    fun invalidFoldBounds(windowBounds: Rect): List<Rect> {
        return listOf(
            invalidZeroBound(),
            invalidBoundShortWidth(windowBounds),
            invalidBoundShortHeight(windowBounds)
        )
    }

    /**
     * @param bounds for the test [FoldingFeature]
     * @param type   of the [FoldingFeature]
     * @return [List] of [FoldingFeature] containing all the possible states for the
     * given type.
     */
    @JvmStatic
    fun allFoldStates(bounds: Bounds, type: FoldingFeature.Type): List<FoldingFeature> {
        return listOf(
            FoldingFeature(bounds, type, FLAT),
            FoldingFeature(bounds, type, HALF_OPENED)
        )
    }

    /**
     * @param bounds for the test [FoldingFeature]
     * @return [List] of [FoldingFeature] containing all the possible states and
     * types.
     */
    @JvmStatic
    fun allFoldingFeatureTypeAndStates(bounds: Bounds): List<FoldingFeature> {
        return allFoldStates(bounds, FoldingFeature.Type.HINGE) +
            allFoldStates(bounds, FoldingFeature.Type.FOLD)
    }
}
