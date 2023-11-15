/*
 * Copyright 2020 The Android Open Source Project
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

import android.content.Context
import androidx.compose.ui.unit.fontscaling.FontScaleConverter
import androidx.compose.ui.unit.fontscaling.FontScaleConverterFactory

/**
 * Creates a [Density] for this [Context].
 *
 * @param context density values will be extracted from this [Context]
 */
fun Density(context: Context): Density {
    val fontScale = context.resources.configuration.fontScale
    return DensityWithConverter(
        context.resources.displayMetrics.density,
        fontScale,
        FontScaleConverterFactory.forScale(fontScale) ?: LinearFontScaleConverter(fontScale)
    )
}

private data class DensityWithConverter(
    override val density: Float,
    override val fontScale: Float,
    private val converter: FontScaleConverter
) : Density {

    override fun Dp.toSp(): TextUnit {
        return converter.convertDpToSp(value).sp
    }

    override fun TextUnit.toDp(): Dp {
        check(type == TextUnitType.Sp) { "Only Sp can convert to Px" }
        return Dp(converter.convertSpToDp(value))
    }
}

private data class LinearFontScaleConverter(private val fontScale: Float) : FontScaleConverter {
    override fun convertSpToDp(sp: Float) = sp * fontScale
    override fun convertDpToSp(dp: Float) = dp / fontScale
}
