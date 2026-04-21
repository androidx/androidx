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

package androidx.text.vertical.compose

import android.text.TextPaint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.text.vertical.TextOrientation
import androidx.text.vertical.VerticalTextLayout
import kotlin.math.ceil
import kotlin.math.min

/**
 * Displays text laid out vertically using [VerticalTextLayout].
 *
 * This overload accepts an Android [CharSequence] and [TextPaint] directly, allowing use of
 * vertical-specific spans such as [androidx.text.vertical.RubySpan],
 * [androidx.text.vertical.EmphasisSpan], [androidx.text.vertical.FontShearSpan], and
 * [androidx.text.vertical.TextOrientationSpan].
 *
 * NOTE: Vertical text layout requires API 36 (Android 16). On older API levels, nothing is
 * rendered.
 *
 * @param text The [CharSequence] text to display.
 * @param paint The [TextPaint] to use for text measurement and rendering.
 * @param modifier The [Modifier] to apply to the layout.
 * @param overflow How to handle text that overflows the available space.
 * @param maxColumns The maximum number of columns. Defaults to unlimited.
 * @param minColumns The minimum number of columns for layout sizing.
 * @param orientation The text orientation mode for vertical layout.
 */
@Composable
public fun VerticalText(
    text: CharSequence,
    paint: TextPaint,
    modifier: Modifier = Modifier,
    overflow: TextOverflow = TextOverflow.Clip,
    maxColumns: Int = Int.MAX_VALUE,
    minColumns: Int = 1,
    orientation: TextOrientation = TextOrientation.Mixed,
) {
    VerticalTextImpl(
        text = text,
        paint = paint,
        modifier = modifier,
        overflow = overflow,
        maxColumns = maxColumns,
        minColumns = minColumns,
        orientation = orientation,
        semanticsText = text.toString(),
    )
}

private class VerticalTextLayoutCache {
    private var layout: VerticalTextLayout? = null
    private var lastText: CharSequence? = null
    private var lastConstraintsHeight: Int = -1
    private var lastOrientation: TextOrientation? = null

    /** Returns the cached layout if the inputs are unchanged, otherwise creates a new one. */
    fun getLayout(
        text: CharSequence,
        height: Int,
        orientation: TextOrientation,
        paint: TextPaint,
    ): VerticalTextLayout {
        val cached = layout
        if (
            cached != null &&
                lastText == text &&
                lastConstraintsHeight == height &&
                lastOrientation == orientation
        ) {
            return cached
        }
        val newLayout =
            VerticalTextLayout(
                text = text,
                start = 0,
                end = text.length,
                paint = paint,
                height = height.toFloat(),
                orientation = orientation,
            )
        layout = newLayout
        lastText = text
        lastConstraintsHeight = height
        lastOrientation = orientation
        return newLayout
    }
}

@Composable
private fun VerticalTextImpl(
    text: CharSequence,
    paint: TextPaint,
    modifier: Modifier,
    overflow: TextOverflow,
    maxColumns: Int,
    minColumns: Int,
    orientation: TextOrientation,
    semanticsText: String,
) {
    check(minColumns <= maxColumns) { "maxColumn must be bigger than minColumns!" }

    val cache = remember { VerticalTextLayoutCache() }
    var textLayout by remember { mutableStateOf<VerticalTextLayout?>(null) }
    val clipModifier = if (overflow == TextOverflow.Clip) Modifier.clipToBounds() else Modifier
    val semanticsModifier = Modifier.semantics { this.text = AnnotatedString(semanticsText) }
    val drawModifier =
        Modifier.drawBehind {
            drawIntoCanvas { canvas -> textLayout?.draw(canvas.nativeCanvas, size.width, 0f) }
        }

    Layout(
        content = {},
        modifier = modifier.then(semanticsModifier).then(clipModifier).then(drawModifier),
    ) { _, constraints ->
        val vtl = cache.getLayout(text, constraints.maxHeight, orientation, paint)
        textLayout = vtl

        val columnWidth = if (vtl.lineCount > 0) vtl.width / vtl.lineCount else 0f
        val effectiveColumns = min(vtl.lineCount, maxColumns)
        val desiredWidth = if (effectiveColumns > 0) columnWidth * effectiveColumns else 0f
        val minWidth = columnWidth * minColumns

        val layoutWidth =
            ceil(maxOf(desiredWidth, minWidth))
                .toInt()
                .coerceIn(constraints.minWidth, constraints.maxWidth)

        layout(layoutWidth, constraints.maxHeight) {}
    }
}
