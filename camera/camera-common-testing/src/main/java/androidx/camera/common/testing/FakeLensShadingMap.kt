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

package androidx.camera.common.testing

import android.hardware.camera2.params.RggbChannelVector
import androidx.annotation.RestrictTo
import androidx.camera.common.LensShadingMapWrapper
import java.lang.Class

/**
 * A fake implementation of [LensShadingMapWrapper] for testing.
 *
 * This class allows setting mock values for row count, column count, and gain factors.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeLensShadingMap
internal constructor(
    override val rowCount: Int,
    override val columnCount: Int,
    private val gainFactors: FloatArray,
) : LensShadingMapWrapper {

    init {
        require(gainFactors.size == rowCount * columnCount * 4) {
            "gainFactors.size (${gainFactors.size}) must be equal to ${rowCount * columnCount * 4}"
        }
    }

    override val gainFactorCount: Int
        get() = gainFactors.size

    override fun getGainFactor(colorChannel: Int, column: Int, row: Int): Float {
        require(colorChannel in 0..3) { "colorChannel ($colorChannel) must be between 0 and 3" }
        require(column in 0 until columnCount) {
            "column ($column) must be between 0 and $columnCount"
        }
        require(row in 0 until rowCount) { "row ($row) must be between 0 and $rowCount" }
        val index = (row * columnCount + column) * 4 + colorChannel
        return gainFactors[index]
    }

    override fun getGainFactorVector(column: Int, row: Int): RggbChannelVector {
        require(column in 0 until columnCount) {
            "column ($column) must be between 0 and $columnCount"
        }
        require(row in 0 until rowCount) { "row ($row) must be between 0 and $rowCount" }
        val index = (row * columnCount + column) * 4
        return RggbChannelVector(
            gainFactors[index], // R
            gainFactors[index + 1], // Geven
            gainFactors[index + 2], // Godd
            gainFactors[index + 3], // B
        )
    }

    override fun copyGainFactors(destination: FloatArray, offset: Int) {
        require(destination.size - offset >= gainFactorCount) {
            "destination.size - offset (${destination.size - offset}) must be at least $gainFactorCount"
        }
        gainFactors.copyInto(destination, destinationOffset = offset)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            else -> null
        }

    public companion object {
        /** Creates a [FakeLensShadingMap] instance for Kotlin clients. */
        @JvmSynthetic
        @Suppress("MissingJvmstatic")
        public operator fun invoke(
            rowCount: Int,
            columnCount: Int,
            gainFactors: FloatArray = FloatArray(rowCount * columnCount * 4) { 1.0f },
        ): FakeLensShadingMap {
            return FakeLensShadingMap(rowCount, columnCount, gainFactors)
        }

        /** Creates a [FakeLensShadingMap] instance for Java compatibility. */
        @JvmStatic
        @JvmOverloads
        public fun create(
            rowCount: Int,
            columnCount: Int,
            gainFactors: FloatArray = FloatArray(rowCount * columnCount * 4) { 1.0f },
        ): FakeLensShadingMap {
            return FakeLensShadingMap(rowCount, columnCount, gainFactors)
        }
    }
}
