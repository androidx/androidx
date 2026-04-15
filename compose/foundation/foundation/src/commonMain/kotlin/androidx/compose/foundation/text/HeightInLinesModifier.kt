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

import androidx.compose.foundation.ComposeFoundationFlags.isBasicTextFieldHeightInLinesOptimizationEnabled
import androidx.compose.foundation.ComposeFoundationFlags.isBasicTextFieldMinSizeOptimizationEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.internal.requirePreconditionNotNull
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import kotlin.math.roundToInt

/**
 * The default minimum height in terms of minimum number of visible lines.
 *
 * Should not be used in public API and samples unless it's public, too.
 */
internal const val DefaultMinLines = 1

/**
 * Constraint the height of the TextField or BasicText so that it vertically occupies at least
 * [minLines] number of lines and at most [maxLines] number of lines. BasicText should not use this
 * function for calculating maxLines constraints since MultiParagraph computation already handles
 * that.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.heightInLines(
    textStyle: TextStyle,
    softWrap: Boolean,
    minLines: Int = DefaultMinLines,
    maxLines: Int = Int.MAX_VALUE,
): Modifier {
    validateMinMaxLines(minLines, maxLines)
    if (minLines == DefaultMinLines && maxLines == Int.MAX_VALUE) return this
    // This is a single line text field, it has a separate calculation for its minimum height
    if (!softWrap) return this

    return if (isBasicTextFieldMinSizeOptimizationEnabled) {
        this then HeightInLinesElement(textStyle, minLines, maxLines)
    } else {
        legacyHeightInLines(textStyle, minLines, maxLines)
    }
}

private class HeightInLinesElement(
    private val textStyle: TextStyle,
    private val minLines: Int,
    private val maxLines: Int,
) : ModifierNodeElement<HeightInLinesNode>() {

    override fun create() = HeightInLinesNode(textStyle, minLines, maxLines)

    override fun update(node: HeightInLinesNode) {
        node.update(textStyle, minLines, maxLines)
    }

    override fun hashCode(): Int {
        var result = textStyle.hashCode()
        result = 31 * result + minLines
        result = 31 * result + maxLines
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HeightInLinesElement) return false
        if (textStyle != other.textStyle) return false
        if (minLines != other.minLines) return false
        if (maxLines != other.maxLines) return false
        return true
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "heightInLines"
        properties["minLines"] = minLines
        properties["maxLines"] = maxLines
        properties["textStyle"] = textStyle
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class HeightInLinesNode(
    private var textStyle: TextStyle,
    private var minLines: Int,
    private var maxLines: Int,
) :
    Modifier.Node(),
    CompositionLocalConsumerModifierNode,
    LayoutModifierNode,
    ObserverModifierNode {

    private var dirty = false
    private var precomputedMinLinesHeight: Int = -1
    private var precomputedMaxLinesHeight: Int = -1

    private var resolvedStyle: TextStyle? = null
    private var fontResolutionState: State<Any>? = null

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
        fontResolutionState =
            fontFamilyResolver.resolve(
                requireResolvedStyle().fontFamily,
                requireResolvedStyle().fontWeight ?: FontWeight.Normal,
                requireResolvedStyle().fontStyle ?: FontStyle.Normal,
                requireResolvedStyle().fontSynthesis ?: FontSynthesis.All,
            )
        // Observe the value of the State returned by fontFamilyResolver.resolve so we can
        //  invalidate layout when its value changes/the font family is resolved
        observeReads { requireFontResolutionState().value }

        dirty = true
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        if (dirty) {
            computeHeights(
                density = this,
                resolvedStyle = requireResolvedStyle(),
                fontFamilyResolver = currentValueOf(LocalFontFamilyResolver),
            )
            dirty = false
        }

        val minHeight =
            if (precomputedMinLinesHeight != -1) {
                precomputedMinLinesHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
            } else {
                constraints.minHeight
            }

        val maxHeight =
            if (precomputedMaxLinesHeight != -1) {
                precomputedMaxLinesHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
            } else {
                constraints.maxHeight
            }

        val childConstraints = constraints.copy(minHeight = minHeight, maxHeight = maxHeight)

        val measured = measurable.measure(childConstraints)
        return layout(measured.width, measured.height) { measured.placeRelative(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int {
        getOrPrecomputeMinMaxHeight()
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
        getOrPrecomputeMinMaxHeight()
        return if (precomputedMinLinesHeight == precomputedMaxLinesHeight) {
            precomputedMaxLinesHeight
        } else {
            measurable
                .maxIntrinsicHeight(width)
                .fastCoerceIn(precomputedMinLinesHeight, precomputedMaxLinesHeight)
        }
    }

    private fun Density.getOrPrecomputeMinMaxHeight() {
        if (dirty) {
            computeHeights(
                density = this,
                resolvedStyle = requireResolvedStyle(),
                fontFamilyResolver = currentValueOf(LocalFontFamilyResolver),
            )
            dirty = false
        }
        precomputedMinLinesHeight = precomputedMinLinesHeight.fastCoerceAtLeast(0)
        precomputedMaxLinesHeight =
            if (precomputedMaxLinesHeight != -1) precomputedMaxLinesHeight else Constraints.Infinity
    }

    override fun onObservedReadsChanged() {
        onFontResolutionStateChanged()
    }

    private fun onFontResolutionStateChanged() {
        if (fontResolutionState != null) {
            observeReads { requireFontResolutionState().value }
        }
        dirty = true
        invalidateMeasurement()
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
        dirty = false
    }

    fun update(textStyle: TextStyle, minLines: Int, maxLines: Int) {
        if (this.textStyle != textStyle || this.minLines != minLines || this.maxLines != maxLines) {
            this.textStyle = textStyle
            this.minLines = minLines
            this.maxLines = maxLines
            resolvedStyle = resolveDefaults(textStyle, requireLayoutDirection())
            dirty = true
            invalidateMeasurement()
        }
    }

    private fun computeHeights(
        density: Density,
        resolvedStyle: TextStyle,
        fontFamilyResolver: FontFamily.Resolver,
    ) {
        if (isBasicTextFieldHeightInLinesOptimizationEnabled) {
            val threeLinesParagraph =
                paragraphForDefaultText(
                    style = resolvedStyle,
                    density = density,
                    fontFamilyResolver = fontFamilyResolver,
                    text =
                        EmptyTextReplacement +
                            "\n" +
                            EmptyTextReplacement +
                            "\n" +
                            EmptyTextReplacement,
                    maxLines = 3,
                )
            val first = threeLinesParagraph.getLineHeight(0)
            val second = threeLinesParagraph.getLineHeight(1)
            val third = threeLinesParagraph.getLineHeight(2)

            precomputedMinLinesHeight =
                computeHeightFromThreeLines(first, second, third, minLines, DefaultMinLines)
            precomputedMaxLinesHeight =
                computeHeightFromThreeLines(first, second, third, maxLines, Int.MAX_VALUE)
        } else {
            val firstLineHeight =
                computeSizeForDefaultText(
                        style = resolvedStyle,
                        density = density,
                        fontFamilyResolver = fontFamilyResolver,
                        text = EmptyTextReplacement,
                        maxLines = 1,
                    )
                    .height

            val firstTwoLinesHeight =
                computeSizeForDefaultText(
                        style = resolvedStyle,
                        density = density,
                        fontFamilyResolver = fontFamilyResolver,
                        text = EmptyTextReplacement + "\n" + EmptyTextReplacement,
                        maxLines = 2,
                    )
                    .height

            val lineHeight = firstTwoLinesHeight - firstLineHeight

            precomputedMinLinesHeight =
                if (minLines == DefaultMinLines) {
                    -1
                } else {
                    firstLineHeight + lineHeight * (minLines - 1)
                }

            precomputedMaxLinesHeight =
                if (maxLines == Int.MAX_VALUE) {
                    -1
                } else {
                    firstLineHeight + lineHeight * (maxLines - 1)
                }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.legacyHeightInLines(
    textStyle: TextStyle,
    minLines: Int = DefaultMinLines,
    maxLines: Int = Int.MAX_VALUE,
) =
    composed(
        inspectorInfo =
            debugInspectorInfo {
                name = "heightInLines"
                properties["minLines"] = minLines
                properties["maxLines"] = maxLines
                properties["textStyle"] = textStyle
            }
    ) {
        val density = LocalDensity.current
        val fontFamilyResolver = LocalFontFamilyResolver.current
        val layoutDirection = LocalLayoutDirection.current

        val resolvedStyle =
            remember(textStyle, layoutDirection) { resolveDefaults(textStyle, layoutDirection) }
        val typeface by
            remember(fontFamilyResolver, resolvedStyle) {
                fontFamilyResolver.resolve(
                    resolvedStyle.fontFamily,
                    resolvedStyle.fontWeight ?: FontWeight.Normal,
                    resolvedStyle.fontStyle ?: FontStyle.Normal,
                    resolvedStyle.fontSynthesis ?: FontSynthesis.All,
                )
            }

        var precomputedMinLinesHeight: Int? = null
        var precomputedMaxLinesHeight: Int? = null

        if (isBasicTextFieldHeightInLinesOptimizationEnabled) {
            val threeLinesParagraph =
                paragraphForDefaultText(
                    style = resolvedStyle,
                    density = density,
                    fontFamilyResolver = fontFamilyResolver,
                    text =
                        EmptyTextReplacement +
                            "\n" +
                            EmptyTextReplacement +
                            "\n" +
                            EmptyTextReplacement,
                    maxLines = 3,
                )
            val first = threeLinesParagraph.getLineHeight(0)
            val second = threeLinesParagraph.getLineHeight(1)
            val third = threeLinesParagraph.getLineHeight(2)

            precomputedMinLinesHeight =
                computeHeightFromThreeLines(first, second, third, minLines, DefaultMinLines)
            precomputedMaxLinesHeight =
                computeHeightFromThreeLines(first, second, third, maxLines, Int.MAX_VALUE)
        } else {
            // Legacy path
            val firstLineHeight =
                remember(density, fontFamilyResolver, textStyle, layoutDirection, typeface) {
                    computeSizeForDefaultText(
                            style = resolvedStyle,
                            density = density,
                            fontFamilyResolver = fontFamilyResolver,
                            text = EmptyTextReplacement,
                            maxLines = 1,
                        )
                        .height
                }

            val firstTwoLinesHeight =
                remember(density, fontFamilyResolver, textStyle, layoutDirection, typeface) {
                    val twoLines = EmptyTextReplacement + "\n" + EmptyTextReplacement
                    computeSizeForDefaultText(
                            style = resolvedStyle,
                            density = density,
                            fontFamilyResolver = fontFamilyResolver,
                            text = twoLines,
                            maxLines = 2,
                        )
                        .height
                }
            val lineHeight = firstTwoLinesHeight - firstLineHeight
            precomputedMinLinesHeight =
                if (minLines == DefaultMinLines) null
                else firstLineHeight + lineHeight * (minLines - 1)
            precomputedMaxLinesHeight =
                if (maxLines == Int.MAX_VALUE) null
                else firstLineHeight + lineHeight * (maxLines - 1)
        }

        with(density) {
            Modifier.heightIn(
                min = precomputedMinLinesHeight?.toDp() ?: Dp.Unspecified,
                max = precomputedMaxLinesHeight?.toDp() ?: Dp.Unspecified,
            )
        }
    }

internal fun validateMinMaxLines(minLines: Int, maxLines: Int) {
    requirePrecondition(minLines > 0 && maxLines > 0) {
        "both minLines $minLines and maxLines $maxLines must be greater than zero"
    }
    requirePrecondition(minLines <= maxLines) {
        "minLines $minLines must be less than or equal to maxLines $maxLines"
    }
}

/**
 * A helper function that calculates the height for [linesLimit] based on height of top, bottom and
 * middle line height. If the function is called with a *default* [linesLimit] value (which comes
 * from the BasicTextField, we won't do the calculations and simply return early. This is to ensure
 * that the text field doesn't perform unnecessary calculations if the user of the API doesn't
 * actually constrain the height with the minLines and/or maxLines.
 */
private fun computeHeightFromThreeLines(
    topLine: Float,
    middleLine: Float,
    bottomLine: Float,
    linesLimit: Int,
    defaultLinesLimit: Int,
): Int {
    return if (linesLimit == defaultLinesLimit) {
        -1
    } else {
        (topLine +
                middleLine * (linesLimit - 2).fastCoerceAtLeast(0) +
                bottomLine * (linesLimit - 1).fastCoerceAtMost(1))
            .roundToInt()
    }
}
