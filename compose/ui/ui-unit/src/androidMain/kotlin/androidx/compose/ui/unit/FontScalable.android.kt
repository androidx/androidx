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

package androidx.compose.ui.unit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.fontscaling.FontScaleConverterFactory
import androidx.compose.ui.unit.internal.JvmDefaultWithCompatibility

/**
 * Converts [TextUnit] to [Dp] and vice-versa.
 *
 * Note that the converter can't be cached in the interface itself. FontScaleConverterFactory
 * already caches the tables, but it still does a a map lookup for each conversion. If you are
 * implementing this interface, you should cache your own converter for additional speed.
 */
@Immutable
@JvmDefaultWithCompatibility
actual interface FontScalable {
    /**
     * Current user preference for the scaling factor for fonts.
     */
    @Stable
    actual val fontScale: Float

    /**
     * Convert [Dp] to Sp. Sp is used for font size, etc.
     */
    @Stable
    actual fun Dp.toSp(): TextUnit {
        val converter = FontScaleConverterFactory.forScale(fontScale)
        return (converter?.convertDpToSp(value) ?: (value / fontScale)).sp
    }

    /**
     * Convert Sp to [Dp].
     * @throws IllegalStateException if TextUnit other than SP unit is specified.
     */
    @Stable
    actual fun TextUnit.toDp(): Dp {
        check(type == TextUnitType.Sp) { "Only Sp can convert to Px" }
        val converter = FontScaleConverterFactory.forScale(fontScale)
        return if (converter == null) Dp(value * fontScale) else Dp(converter.convertSpToDp(value))
    }
}
