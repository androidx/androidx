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

import android.hardware.camera2.params.LensShadingMap as Camera2LensShadingMap
import android.hardware.camera2.params.RggbChannelVector
import java.lang.Class

/**
 * An implementation of [LensShadingMapWrapper] that wraps a native [Camera2LensShadingMap] object.
 */
internal class AndroidLensShadingMap(private val lensShadingMap: Camera2LensShadingMap) :
    LensShadingMapWrapper {

    override val rowCount: Int
        get() = lensShadingMap.rowCount

    override val columnCount: Int
        get() = lensShadingMap.columnCount

    override val gainFactorCount: Int
        get() = lensShadingMap.gainFactorCount

    override fun getGainFactor(colorChannel: Int, column: Int, row: Int): Float {
        return lensShadingMap.getGainFactor(colorChannel, column, row)
    }

    override fun getGainFactorVector(column: Int, row: Int): RggbChannelVector {
        return lensShadingMap.getGainFactorVector(column, row)
    }

    override fun copyGainFactors(destination: FloatArray, offset: Int) {
        lensShadingMap.copyGainFactors(destination, offset)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            type.isInstance(lensShadingMap) -> lensShadingMap as T
            else -> null
        }
}
