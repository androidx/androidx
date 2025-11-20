/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit

/**
 * Animates label text for which the number of lines can vary, changing the size of the container
 * component.
 *
 * Displays the given string in a [Text], with an animation that fades text in line by line when new
 * lines of text are added or removed. This is intended to be be used for labels in a Button or
 * Card, where we want the container to expand to fit the contents when the lines of the text
 * change.
 *
 * @sample androidx.wear.compose.material3.samples.FadingExpandingLabelButtonSample
 * @param text Text string that will be shown.
 * @param modifier Modifier to be applied to the animated text.
 * @param color [Color] to apply to the text. If [Color.Unspecified], and [textStyle] has no color
 *   set,this will be [LocalContentColor].
 * @param fontSize The size of glyphs to use when painting the text. See [TextStyle.fontSize].
 * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic). See
 *   [TextStyle.fontStyle].
 * @param fontWeight The typeface thickness to use when painting the text (e.g., [FontWeight.Bold]).
 * @param fontFamily The font family to be used when rendering the text. See [TextStyle.fontFamily].
 * @param letterSpacing The amount of space to add between each letter. See
 *   [TextStyle.letterSpacing].
 * @param textDecoration The decorations to paint on the text (e.g., an underline). See
 *   [TextStyle.textDecoration].
 * @param textAlign The alignment of the text within the lines of the paragraph. See
 *   [TextStyle.textAlign].
 * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM. See
 *   [TextStyle.lineHeight].
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 *   text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 *   TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if necessary.
 *   If it is not null, then it must be greater than zero.
 * @param minLines The minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines].
 * @param textStyle [TextStyle] for the animated text.
 * @param animationSpec Animation spec for the text fade animation.
 */
@Composable
public fun FadingExpandingLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = LocalTextConfiguration.current.textAlign,
    lineHeight: TextUnit = TextUnit.Unspecified,
    softWrap: Boolean = true,
    maxLines: Int = LocalTextConfiguration.current.maxLines,
    minLines: Int = 1,
    textStyle: TextStyle = LocalTextStyle.current,
    animationSpec: FiniteAnimationSpec<Float> = FadingExpandingLabelDefaults.animationSpec,
) {
    val density = LocalDensity.current
    var currentText by remember { mutableStateOf(text) }
    var maxTextWidth by remember { mutableStateOf<Int?>(null) }
    var showAnimatedTextHeight by remember { mutableStateOf(false) }

    // Merge the optional parameters with the [TextStyle]
    val mergedTextStyle =
        textStyle.merge(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing,
        )

    val textMeasurer = rememberTextMeasurer()
    val textMeasureResult =
        remember(text, mergedTextStyle, maxTextWidth) {
            textMeasurer.measure(
                text = text,
                style = mergedTextStyle,
                softWrap = softWrap,
                maxLines = maxLines,
                // TODO(b/404793120): consider using BoxWithConstraints to get constraints once
                // b/404793120 is fixed.
                constraints = maxTextWidth?.let { Constraints(maxWidth = it) } ?: Constraints(),
            )
        }
    var currentTextMeasureResult by remember { mutableStateOf(textMeasureResult) }
    val animatedHeight = remember { Animatable(currentTextMeasureResult.size.height.toFloat()) }

    LaunchedEffect(textMeasureResult) {
        // Don't animate if text hasn't changed
        if (text == currentText && !showAnimatedTextHeight) {
            currentTextMeasureResult = textMeasureResult
            animatedHeight.snapTo(textMeasureResult.size.height.toFloat())
            return@LaunchedEffect
        }

        // If the text is expanding, update it before the fading lines animation, if it's
        // collapsing, update it after the animation. This is because we can only animate the
        // expanding fading effect on the larger text.
        val isLinesDecreasing = currentTextMeasureResult.lineCount > textMeasureResult.lineCount
        if (!isLinesDecreasing) {
            currentText = text
            currentTextMeasureResult = textMeasureResult
        }

        showAnimatedTextHeight = true
        // Animate to the new text height to reveal it with a fade-in animation
        animatedHeight.animateTo(textMeasureResult.size.height.toFloat(), animationSpec)

        if (isLinesDecreasing) {
            currentText = text
            currentTextMeasureResult = textMeasureResult
        }
    }

    Text(
        text = currentText,
        onTextLayout = { textLayoutResult ->
            // Set the max width that will be used to measure the text.
            maxTextWidth = textLayoutResult.layoutInput.constraints.maxWidth
        },
        modifier =
            if (showAnimatedTextHeight) {
                modifier
                    .height(with(density) { animatedHeight.value.toDp() })
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        for (i in 0 until currentTextMeasureResult.lineCount) {
                            val top = currentTextMeasureResult.getLineTop(i)
                            val bottom = currentTextMeasureResult.getLineBottom(i)

                            if (animatedHeight.value < bottom) {
                                val alpha =
                                    ((animatedHeight.value - top) / (bottom - top) - 0.5f) * 2
                                drawRect(
                                    Color(255, 255, 255, (alpha * 255).toInt().coerceIn(0, 255)),
                                    topLeft = Offset(0f, top),
                                    size = Size(size.width, bottom),
                                    blendMode = BlendMode.Modulate,
                                )
                            }
                        }
                    }
            } else {
                modifier
            },
        style = mergedTextStyle,
        maxLines = maxLines,
        overflow = TextOverflow.Visible,
        textAlign = textAlign,
    )
}

/** Contains default values for [FadingExpandingLabel]. */
public object FadingExpandingLabelDefaults {

    /** Default animation spec for [FadingExpandingLabel] */
    public val animationSpec: FiniteAnimationSpec<Float>
        @Composable get() = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
}
