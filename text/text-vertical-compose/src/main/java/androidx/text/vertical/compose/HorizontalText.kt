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

import android.graphics.Typeface
import androidx.annotation.RestrictTo
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
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.text.vertical.HorizontalTextLayout
import kotlin.math.min

/**
 * Marks the bottom of the first visual line in [HorizontalText].
 *
 * Matches [HorizontalTextLayout.getLineBottom] for line `0`, which includes the full height of any
 * [androidx.text.vertical.RubySpan] or [androidx.text.vertical.EmphasisSpan] on the first line and
 * includes bottom font padding when the layout has a single line.
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
// TODO(b/573725571, b/571750156): Remove @RestrictTo in 1.1.0-alpha01
public val FirstLineBottom: HorizontalAlignmentLine = HorizontalAlignmentLine(::min)

/**
 * Displays horizontal text with Android spans such as [androidx.text.vertical.RubySpan] and
 * [androidx.text.vertical.EmphasisSpan].
 *
 * Wraps width to the widest line when [modifier] leaves a flexible width range. Provides
 * [FirstBaseline], [LastBaseline], and [FirstLineBottom] alignment lines from a single layout pass.
 *
 * Honors [TextStyle.color], [TextStyle.fontSize], [TextStyle.fontWeight], [TextStyle.fontStyle],
 * [TextStyle.fontSynthesis], [TextStyle.fontFamily], [TextStyle.fontFeatureSettings],
 * [TextStyle.background], [TextStyle.localeList], and [TextStyle.textAlign].
 * [androidx.compose.ui.text.style.TextAlign.Justify] falls back to start alignment. Ignores other
 * [TextStyle] members.
 *
 * @param text text to display, which may be a [android.text.Spanned] with Android spans
 * @param modifier [Modifier] to apply to this layout node
 * @param style [TextStyle] for text measurement, alignment, and rendering
 * @param overflow how to handle visual overflow when content exceeds bounds. Supports
 *   [TextOverflow.Clip] and [TextOverflow.Visible]; other values clip
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
// TODO(b/573725571, b/571750156): Remove @RestrictTo in 1.1.0-alpha01
@Composable
public fun HorizontalText(
    text: CharSequence,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val cache = remember { HorizontalTextLayoutCache() }
    var textLayout by remember { mutableStateOf<HorizontalTextLayout?>(null) }
    val clipModifier = if (overflow != TextOverflow.Visible) Modifier.clipToBounds() else Modifier
    val semanticsText = text as? AnnotatedString ?: AnnotatedString(text.toString())
    val semanticsModifier = Modifier.semantics { this.text = semanticsText }
    val drawModifier = Modifier.drawBehind {
        val currentLayout = textLayout ?: return@drawBehind
        drawIntoCanvas { canvas -> currentLayout.draw(canvas.nativeCanvas, 0f, 0f) }
    }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val resolver = LocalFontFamilyResolver.current
    val typeface by
        remember(style, resolver) {
            resolver.resolve(
                style.fontFamily,
                style.fontWeight ?: FontWeight.Normal,
                style.fontStyle ?: FontStyle.Normal,
                style.fontSynthesis ?: FontSynthesis.All,
            )
        }

    Layout(
        content = {},
        modifier = modifier.then(semanticsModifier).then(clipModifier).then(drawModifier),
    ) { _, constraints ->
        val htl =
            cache.getLayout(
                text = text,
                minWidth = constraints.minWidth,
                maxWidth = constraints.maxWidth,
                layoutDirection = layoutDirection,
                style = style,
                typeface = typeface as Typeface,
                density = density,
            )
        textLayout = htl
        val layoutWidth = htl.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeight = htl.height.coerceIn(constraints.minHeight, constraints.maxHeight)
        val lastLine = (htl.lineCount - 1).coerceAtLeast(0)

        layout(
            width = layoutWidth,
            height = layoutHeight,
            alignmentLines =
                mapOf(
                    FirstBaseline to htl.getLineBaseline(0),
                    LastBaseline to htl.getLineBaseline(lastLine),
                    FirstLineBottom to htl.getLineBottom(0),
                ),
        ) {}
    }
}
