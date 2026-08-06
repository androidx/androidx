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

package androidx.camera.common

import android.hardware.camera2.params.RggbChannelVector
import androidx.annotation.RestrictTo

/**
 * Provides compatibility-focused access to [android.hardware.camera2.params.LensShadingMap].
 *
 * @see android.hardware.camera2.params.LensShadingMap
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface LensShadingMapWrapper : UnsafeWrapper {

    /** Get the number of rows in this map. */
    public val rowCount: Int

    /** Get the number of columns in this map. */
    public val columnCount: Int

    /** Get the total number of gain factors in this map. */
    public val gainFactorCount: Int

    /** Get a single gain factor from this lens shading map by its row and column. */
    public fun getGainFactor(colorChannel: Int, column: Int, row: Int): Float

    /** Get a gain factor vector from this lens shading map by its row and column. */
    public fun getGainFactorVector(column: Int, row: Int): RggbChannelVector

    /** Copy all gain factors in row-major order into the destination array. */
    public fun copyGainFactors(destination: FloatArray, offset: Int)

    public companion object {
        /** The red color channel. */
        public const val COLOR_CHANNEL_RED: Int = 0

        /** The green-red color channel. */
        public const val COLOR_CHANNEL_GREEN_RED: Int = 1

        /** The green-blue color channel. */
        public const val COLOR_CHANNEL_GREEN_BLUE: Int = 2

        /** The blue color channel. */
        public const val COLOR_CHANNEL_BLUE: Int = 3
    }
}
