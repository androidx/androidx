/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.internal.requirePreconditionNotNull
import androidx.compose.foundation.text.input.internal.HeightForSingleLineFieldProvider
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn

/**
 * This is a modifier to calculate a text field's height in the number of lines and minimum width.
 * This modifier combines what previously was 3 modifiers: minHeightForSingleLineField +
 * heightInLines + textFieldMinSize. To establish the width, we use a width of the default string in
 * the provided text style. We also recalculate it after font is loaded. For height, we use the same
 * default string to calculate the minimum and maximum height in the number of requested minLines
 * and maxLines. In addition, for a single line text field we adjust the height based on the entered
 * text to avoid clipping of the tall glyphs (caused by the fact that such height is calculated for
 * a default "H" character). This is not an issue for multiline text field because the text can be
 * scrolled vertically.
 */
internal fun Modifier.textFieldSize(
    textStyle: TextStyle,
    singleLineHeightProvider: HeightForSingleLineFieldProvider,
    minLines: Int,
    maxLines: Int,
    softWrap: Boolean,
): Modifier {
    validateMinMaxLines(minLines, maxLines)
    return this then
        TextFieldSizeConstrainerElement(
            textStyle,
            minLines,
            maxLines,
            softWrap,
            singleLineHeightProvider,
        )
}

private class TextFieldSizeConstrainerElement(
    private val textStyle: TextStyle,
    private val minLines: Int,
    private val maxLines: Int,
    private val softWrap: Boolean,
    private val singleLineHeightProvider: HeightForSingleLineFieldProvider,
) : ModifierNodeElement<TextFieldSizeConstrainerNode>() {

    override fun create() =
        TextFieldSizeConstrainerNode(
            textStyle,
            minLines,
            maxLines,
            softWrap,
            singleLineHeightProvider,
        )

    override fun update(node: TextFieldSizeConstrainerNode) {
        node.update(textStyle, minLines, maxLines, softWrap, singleLineHeightProvider)
    }

    override fun hashCode(): Int {
        var result = textStyle.hashCode()
        result = 31 * result + minLines
        result = 31 * result + maxLines
        result = 31 * result + softWrap.hashCode()
        result = 31 * result + singleLineHeightProvider.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextFieldSizeConstrainerElement) return false
        if (textStyle != other.textStyle) return false
        if (minLines != other.minLines) return false
        if (maxLines != other.maxLines) return false
        if (softWrap != other.softWrap) return false
        if (singleLineHeightProvider != other.singleLineHeightProvider) return false
        return true
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "combinedTextFieldSize"
        properties["minLines"] = minLines
        properties["maxLines"] = maxLines
        properties["softWrap"] = softWrap
        properties["textStyle"] = textStyle
        properties["textLayoutState"] = singleLineHeightProvider
    }
}

private class TextFieldSizeConstrainerNode(
    private var textStyle: TextStyle,
    private var minLines: Int,
    private var maxLines: Int,
    private var softWrap: Boolean,
    private var singleLineHeightProvider: HeightForSingleLineFieldProvider,
) : Modifier.Node(), CompositionLocalConsumerModifierNode, LayoutModifierNode {

    private var dirty = false

    /**
     * We calculate a minimum width to ensure there's a space to place a cursor when the text field
     * is rendered with the empty text. This calculation is based on a default string.
     */
    private var precomputedMinWidth: Int = -1
    /**
     * The minimum height that the text field should occupy when developer sets a minLines. This is
     * calculated by measuring a default string.
     */
    private var precomputedMinLinesHeight: Int = -1
    /**
     * The maximum height that the text field should occupy when developer sets a maxLines. This is
     * calculated by measuring a default string.
     */
    private var precomputedMaxLinesHeight: Int = -1

    private var resolvedStyle: TextStyle? = null
    private var fontResolutionState: State<Any>? = null
    private var typeface: Any? = null

    private fun requireResolvedStyle() =
        requirePreconditionNotNull(resolvedStyle) { "Resolved style is not set." }

    private fun requireFontResolutionState() =
        requirePreconditionNotNull(fontResolutionState) { "Font resolution state is not set." }

    override val shouldAutoInvalidate = false

    override fun onAttach() {
        super.onAttach()
        // TODO: Remove when b/487589072 is fixed
        @Suppress("SuspiciousCompositionLocalModifierRead")
        val fontFamilyResolver = currentValueOf(LocalFontFamilyResolver)
        resolvedStyle = resolveDefaults(textStyle, requireLayoutDirection())
        // We trigger font resolution right away so we don't block the layout pass with it
        updateFontResolutionState(resolvedStyle!!, fontFamilyResolver)

        dirty = true
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        // Observe font resolution state so that we invalidate measure when it changes.
        // This calculates minimum width and height in minLines and maxLines
        computeDefaultSizeIfNeeded(requireFontResolutionState().value)

        val computedConstraints =
            if (!softWrap) { // single line
                // correction for tall glyph clipping in single line
                val height = singleLineHeightProvider.heightForSingleLineField
                val heightPx = height.roundToPx()
                Constraints(
                    minWidth = precomputedMinWidth,
                    maxWidth = Constraints.Infinity,
                    minHeight =
                        if (height == 0.dp) {
                            precomputedMinLinesHeight
                        } else {
                            heightPx
                        },
                    maxHeight = if (height == 0.dp) Constraints.Infinity else heightPx,
                )
            } else {
                Constraints(
                    minWidth = precomputedMinWidth,
                    minHeight =
                        precomputedMinLinesHeight.coerceIn(
                            constraints.minHeight,
                            constraints.maxHeight,
                        ),
                    maxHeight =
                        precomputedMaxLinesHeight.coerceIn(
                            constraints.minHeight,
                            constraints.maxHeight,
                        ),
                )
            }

        val measured = measurable.measure(constraints.constrain(computedConstraints))
        return layout(measured.width, measured.height) { measured.placeRelative(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int {
        computeDefaultSizeIfNeeded(requireFontResolutionState().value)
        return if (precomputedMinLinesHeight == precomputedMaxLinesHeight) {
            precomputedMinLinesHeight
        } else {
            measurable
                .minIntrinsicHeight(width)
                .fastCoerceIn(precomputedMinLinesHeight, precomputedMaxLinesHeight)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int {
        computeDefaultSizeIfNeeded(requireFontResolutionState().value)
        return if (precomputedMinLinesHeight == precomputedMaxLinesHeight) {
            precomputedMaxLinesHeight
        } else {
            measurable
                .maxIntrinsicHeight(width)
                .fastCoerceIn(precomputedMinLinesHeight, precomputedMaxLinesHeight)
        }
    }

    override fun onLayoutDirectionChange() {
        resolvedStyle = resolveDefaults(textStyle, requireLayoutDirection())
        dirty = true
        invalidateMeasurement()
    }

    override fun onDensityChange() {
        dirty = true
        invalidateMeasurement()
    }

    override fun onDetach() {
        resolvedStyle = null
        fontResolutionState = null
        typeface = null
        dirty = false
    }

    fun computeDefaultSizeIfNeeded(typeface: Any) {
        if (typeface != this.typeface) {
            this.typeface = typeface
            dirty = true
        }
        if (dirty) {
            val lines = if (minLines == 1 && (maxLines == 1 || maxLines == Int.MAX_VALUE)) 1 else 3
            /** Setting Ltr helps preserve the legacy behavior */
            val defaultParagraph =
                paragraphForDefaultText(
                    style = requireResolvedStyle().copy(textDirection = TextDirection.Ltr),
                    density = requireDensity(),
                    fontFamilyResolver = currentValueOf(LocalFontFamilyResolver),
                    lines = lines,
                )
            if (lines == 1) {
                precomputedMinLinesHeight = defaultParagraph.height.ceilToIntPx()
                precomputedMaxLinesHeight =
                    if (maxLines == 1) precomputedMinLinesHeight else Constraints.Infinity
            } else {
                val first = defaultParagraph.getLineHeight(0)
                val second = defaultParagraph.getLineHeight(1)
                val third = defaultParagraph.getLineHeight(2)

                precomputedMinLinesHeight =
                    computeHeightFromThreeLines(first, second, third, minLines)

                precomputedMaxLinesHeight =
                    computeHeightFromThreeLines(first, second, third, maxLines)
            }
            // min- and maxIntrinsicsWidth is the same for the default string. However, calculating
            // maxIntrinsicsWidth is ~40x quicker than the minIntrinsicsWidth. Moreover, for case
            // when lines == 1 above, the maxIntrinsicsWidth is already pre-calculated and cached
            // as part of the Boring metrics evaluation.
            precomputedMinWidth = defaultParagraph.maxIntrinsicWidth.ceilToIntPx()
            dirty = false
        }
    }

    fun update(
        textStyle: TextStyle,
        minLines: Int,
        maxLines: Int,
        softWrap: Boolean,
        singleLineHeightProvider: HeightForSingleLineFieldProvider,
    ) {
        if (this.textStyle != textStyle) {
            val resolvedStyle = resolveDefaults(textStyle, requireLayoutDirection())
            updateFontResolutionState(resolvedStyle, currentValueOf(LocalFontFamilyResolver))
            this.resolvedStyle = resolvedStyle
            this.textStyle = textStyle
            dirty = true
        }
        if (
            this.minLines != minLines ||
                this.maxLines != maxLines ||
                this.softWrap != softWrap ||
                this.singleLineHeightProvider.heightForSingleLineField !=
                    singleLineHeightProvider.heightForSingleLineField
        ) {
            this.minLines = minLines
            this.maxLines = maxLines
            this.softWrap = softWrap
            this.singleLineHeightProvider = singleLineHeightProvider
            dirty = true
        }
        if (dirty) {
            invalidateMeasurement()
        }
    }

    private fun updateFontResolutionState(
        resolvedStyle: TextStyle,
        fontFamilyResolver: FontFamily.Resolver,
    ) {
        fontResolutionState =
            fontFamilyResolver.resolve(
                resolvedStyle.fontFamily,
                resolvedStyle.fontWeight ?: FontWeight.Normal,
                resolvedStyle.fontStyle ?: FontStyle.Normal,
                resolvedStyle.fontSynthesis ?: FontSynthesis.All,
            )
    }
}

/**
 * A helper function that calculates the height for [linesLimit] based on height of top, bottom and
 * middle line height.
 */
private fun computeHeightFromThreeLines(
    topLine: Float,
    middleLine: Float,
    bottomLine: Float,
    linesLimit: Int,
): Int {
    return when (linesLimit) {
        1 -> {
            // for 1 line we want to include top and bottom padding. Not that this code is accessed
            // only when there's a soft wrap. In case of 1 line without a soft wrap ("single line")
            // case we have a separate calculation to ensure that tall scripts like Burmese are not
            // clipped.
            (topLine + bottomLine - middleLine).ceilToIntPx()
        }
        Int.MAX_VALUE -> {
            // this is to handle the default maxLines. Note that setting minLines to this value
            // would not pass verification earlier
            Constraints.Infinity
        }
        else -> {
            (topLine +
                    middleLine * (linesLimit - 2).fastCoerceAtLeast(0) +
                    bottomLine * (linesLimit - 1).fastCoerceAtMost(1))
                .ceilToIntPx()
        }
    }
}
