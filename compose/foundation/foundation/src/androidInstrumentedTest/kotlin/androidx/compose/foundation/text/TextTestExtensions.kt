/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.compose.foundation.text

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.toFontFamily
import androidx.testutils.fonts.R
import kotlin.math.ceil
import kotlin.math.roundToInt

fun Float.toIntPx(): Int = ceil(this).roundToInt()

val TEST_FONT =
    Font(resId = R.font.sample_font, weight = FontWeight.Normal, style = FontStyle.Normal)

val TEST_FONT_FAMILY = TEST_FONT.toFontFamily()

/** Insert the given [string] at the given [index]. */
internal fun String.insert(index: Int, string: String): String {
    return substring(0, index) + string + substring(index)
}

/** Helper function that returns the minimal [Rect] which contains the given [substring]. */
internal fun TextLayoutResult.boundingBoxOf(substring: String): Rect {
    val index = layoutInput.text.indexOf(substring)

    return boundingBoxOf(*Array(substring.length) { getBoundingBox(index + it) })
}

/** Convert an Offset to android PointF. */
internal fun Offset.toPointF(): PointF = PointF(x, y)

/** Helper function that returns the [TextRange] of the given string. */
internal fun CharSequence.rangeOf(string: String): TextRange {
    val index = indexOf(string)
    return TextRange(index, index + string.length)
}

/** Helper function that returns the minimal [Rect] which contains all the given [rects]. */
private fun boundingBoxOf(vararg rects: Rect): Rect {
    return Rect(
        left = rects.minOf { it.left },
        top = rects.minOf { it.top },
        right = rects.maxOf { it.right },
        bottom = rects.maxOf { it.bottom }
    )
}
