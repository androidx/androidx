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

package androidx.compose.remote.creation.compose.text

import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.unit.TextUnitType

/** A converter for font scaling. Converts font sizes given in "sp" dimensions to a "dp". */
internal sealed interface RemoteFontScaleConverter {

    /** Converts a dimension in "sp" to "dp". */
    fun convertSpToDp(textUnit: RemoteTextUnit, fontScale: RemoteFloat): RemoteFloat

    companion object {
        val NonLinear: RemoteFontScaleConverter = NonLinearRemoteFontScaleConverter
        val Linear: RemoteFontScaleConverter = LinearRemoteFontScaleConverter
    }
}

/** Implementation of [RemoteFontScaleConverter] that simply scales font linearly. */
private object LinearRemoteFontScaleConverter : RemoteFontScaleConverter {
    override fun convertSpToDp(textUnit: RemoteTextUnit, fontScale: RemoteFloat): RemoteFloat {
        require(textUnit.type == TextUnitType.Sp) { "Only Sp is supported for conversion to dp" }
        return textUnit.value * fontScale
    }
}

/**
 * Implementation of [RemoteFontScaleConverter] that provides non-linear scaling similar to
 * [androidx.compose.ui.unit.fontscaling.FontScaleConverter].
 *
 * Standard linear scaling (Size * Scale) makes large text (e.g., headlines) grow too large for the
 * screen width, while small text remains readable. This converter applies a "dampening" effect:
 * small text scales nearly linearly, while large text scales significantly less. The mathematical
 * Model The conversion follows a two-phase growth model: DP(x, s) = x + A(x) * min(s-1, 0.5) +
 * B(x) * max(0, s-1.5) Phase 1 (Up to 1.5x):** Growth is controlled by multiplier [TABLE_A]. Phase
 * 2 (Above 1.5x):** Additional growth is controlled by multiplier [TABLE_B].
 *
 * @see androidx.compose.ui.unit.fontscaling.FontScaleConverter
 */
private object NonLinearRemoteFontScaleConverter : RemoteFontScaleConverter {

    /**
     * Anchor points for the Phase 1 growth multiplier (scales 1.0 to 1.5). Maps Font Size (sp) to
     * the rate of change.
     */
    private val TABLE_A =
        listOf(
            12f to { x: RemoteFloat -> x }, // Linear growth for small text
            14f to { x: RemoteFloat -> lerp(12f, 16f, 12f, 14f, x) },
            18f to { x: RemoteFloat -> lerp(16f, 12f, 14f, 18f, x) },
            20f to { _: RemoteFloat -> 12f.rf }, // Plateau
            24f to { x: RemoteFloat -> lerp(12f, 8f, 20f, 24f, x) },
            30f to { x: RemoteFloat -> lerp(8f, 0f, 24f, 30f, x) }, // Growth stops at 30sp
        )

    /**
     * Anchor points for the Phase 2 growth multiplier (scales 1.5 to 2.0). Maps Font Size (sp) to
     * the rate of change.
     */
    private val TABLE_B =
        listOf(
            12f to { x: RemoteFloat -> x },
            14f to { x: RemoteFloat -> lerp(12f, 8f, 12f, 14f, x) },
            18f to { x: RemoteFloat -> lerp(8f, 12f, 14f, 18f, x) },
            20f to { x: RemoteFloat -> lerp(12f, 16f, 18f, 20f, x) },
            30f to { _: RemoteFloat -> 16f.rf }, // Maximum growth rate
            100f to { x: RemoteFloat -> lerp(16f, 0f, 30f, 100f, x) },
        )

    /**
     * Converts [textUnit] from SP to DP using non-linear curves if the [fontScale] is 1.03 or
     * greater.
     * * @param textUnit The font size in SP.
     *
     * @param fontScale The system font scale factor (e.g., 1.0, 1.5, 2.0).
     * @return The scaled dimension in DP as a [RemoteFloat].
     */
    override fun convertSpToDp(textUnit: RemoteTextUnit, fontScale: RemoteFloat): RemoteFloat {
        val sp = textUnit.value
        // Android framework threshold for applying non-linear curves
        val isSmallScale = fontScale.lt(1.03f.rf)

        return isSmallScale.select(
            sp * fontScale, // Linear fallback
            calculateNonLinear(sp, fontScale),
        )
    }

    private fun calculateNonLinear(x: RemoteFloat, s: RemoteFloat): RemoteFloat {
        val aTerm = min(s - 1f, 0.5f)
        val bTerm = max(s - 1.5f, 0f)
        return x + (interpolate(x, TABLE_A) * aTerm) + (interpolate(x, TABLE_B) * bTerm)
    }

    /**
     * Performs piecewise linear interpolation across a table of anchor points. Uses [foldRight] to
     * construct a flattened conditional tree of [RemoteFloat.select] calls.
     *
     * @param x The current font size being evaluated.
     * @param table The list of threshold-to-calculation pairs.
     */
    private fun interpolate(
        x: RemoteFloat,
        table: List<Pair<Float, (RemoteFloat) -> RemoteFloat>>,
    ): RemoteFloat {
        return table.foldRight(0f.rf) { (threshold, calculation), acc ->
            x.lt(threshold.rf).select(calculation(x), acc)
        }
    }

    /** Standard linear interpolation helper for [RemoteFloat]. */
    private fun lerp(
        y1: Float,
        y2: Float,
        x1: Float,
        x2: Float,
        currentX: RemoteFloat,
    ): RemoteFloat {
        return lerp(from = y1.rf, to = y2.rf, tween = (currentX - x1) / (x2 - x1))
    }
}
