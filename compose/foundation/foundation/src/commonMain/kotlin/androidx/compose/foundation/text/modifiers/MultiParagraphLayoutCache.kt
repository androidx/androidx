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

package androidx.compose.foundation.text.modifiers

import androidx.compose.foundation.text.DefaultMinLines
import androidx.compose.foundation.text.ceilToIntPx
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.MultiParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrain
import kotlin.math.min

/**
 * Performs text layout using [MultiParagraph].
 *
 * Results are cached whenever possible, for example when only constraints change in a way that
 * cannot reflow text.
 *
 * All measurements are cached.
 */
internal class MultiParagraphLayoutCache(
    private var text: AnnotatedString,
    private var style: TextStyle,
    private var fontFamilyResolver: FontFamily.Resolver,
    private var overflow: TextOverflow = TextOverflow.Clip,
    private var softWrap: Boolean = true,
    private var maxLines: Int = Int.MAX_VALUE,
    private var minLines: Int = DefaultMinLines,
    private var placeholders: List<AnnotatedString.Range<Placeholder>>? = null,
) {
    /** Convert min max lines into actual constraints */
    private var mMinLinesConstrainer: MinLinesConstrainer? = null

    /**
     * Density is an interface which makes it behave like a provider, rather than a final class.
     * Whenever Density changes, the object itself may remain the same, making the below density
     * variable mutate internally. This value holds the last seen density whenever Compose sends us
     * a Density may have changed notification via layout or draw phase.
     */
    private var lastDensity: InlineDensity = InlineDensity.Unspecified

    /** Density that text layout is performed in */
    internal var density: Density? = null
        set(value) {
            val localField = field
            val newDensity = value?.let { InlineDensity(it) } ?: InlineDensity.Unspecified
            if (localField == null) {
                field = value
                lastDensity = newDensity
                return
            }

            if (value == null || lastDensity != newDensity) {
                field = value
                lastDensity = newDensity
                markDirty()
            }
        }

    /** [MultiParagraphIntrinsics] will be initialized lazily */
    private var paragraphIntrinsics: MultiParagraphIntrinsics? = null

    /** [LayoutDirection] used to compute [MultiParagraphIntrinsics] */
    private var intrinsicsLayoutDirection: LayoutDirection? = null

    /** Cached value of final [TextLayoutResult] */
    private var layoutCache: TextLayoutResult? = null

    /** Input width for the last call to [intrinsicHeight] */
    private var cachedIntrinsicHeightInputWidth: Int = -1

    /** Output height for last call to [intrinsicHeight] at [cachedIntrinsicHeightInputWidth] */
    private var cachedIntrinsicHeight: Int = -1

    /** The last computed TextLayoutResult, or throws if not initialized. */
    val textLayoutResult: TextLayoutResult
        get() =
            layoutCache ?: throw IllegalStateException("You must call layoutWithConstraints first")

    /** The last computed TextLayoutResult, or null if not initialized. */
    val layoutOrNull: TextLayoutResult?
        get() = layoutCache

    /**
     * Update layout constraints for this text
     *
     * @return true if constraints caused a text layout invalidation
     */
    fun layoutWithConstraints(constraints: Constraints, layoutDirection: LayoutDirection): Boolean {
        val finalConstraints =
            if (minLines > 1) {
                val localMin =
                    MinLinesConstrainer.from(
                            mMinLinesConstrainer,
                            layoutDirection,
                            style,
                            density!!,
                            fontFamilyResolver
                        )
                        .also { mMinLinesConstrainer = it }
                localMin.coerceMinLines(inConstraints = constraints, minLines = minLines)
            } else {
                constraints
            }
        if (!layoutCache.newLayoutWillBeDifferent(finalConstraints, layoutDirection)) {
            if (finalConstraints == layoutCache!!.layoutInput.constraints) return false
            // we need to regen the input, constraints aren't the same
            layoutCache =
                textLayoutResult(
                    layoutDirection = layoutDirection,
                    finalConstraints = finalConstraints,
                    multiParagraph = layoutCache!!.multiParagraph
                )
            return true
        }
        val multiParagraph = layoutText(finalConstraints, layoutDirection)

        layoutCache = textLayoutResult(layoutDirection, finalConstraints, multiParagraph)
        return true
    }

    private fun textLayoutResult(
        layoutDirection: LayoutDirection,
        finalConstraints: Constraints,
        multiParagraph: MultiParagraph
    ): TextLayoutResult {
        val layoutWidth = min(multiParagraph.intrinsics.maxIntrinsicWidth, multiParagraph.width)
        return TextLayoutResult(
            TextLayoutInput(
                text,
                style,
                placeholders.orEmpty(),
                maxLines,
                softWrap,
                overflow,
                density!!,
                layoutDirection,
                fontFamilyResolver,
                finalConstraints
            ),
            multiParagraph,
            finalConstraints.constrain(
                IntSize(layoutWidth.ceilToIntPx(), multiParagraph.height.ceilToIntPx())
            )
        )
    }

    /** The natural height of text at [width] in [layoutDirection] */
    fun intrinsicHeight(width: Int, layoutDirection: LayoutDirection): Int {
        val localWidth = cachedIntrinsicHeightInputWidth
        val localHeght = cachedIntrinsicHeight
        if (width == localWidth && localWidth != -1) return localHeght
        val result =
            layoutText(Constraints(0, width, 0, Constraints.Infinity), layoutDirection)
                .height
                .ceilToIntPx()

        cachedIntrinsicHeightInputWidth = width
        cachedIntrinsicHeight = result
        return result
    }

    /** Call when any parameters change, invalidation is a result of calling this method. */
    fun update(
        text: AnnotatedString,
        style: TextStyle,
        fontFamilyResolver: FontFamily.Resolver,
        overflow: TextOverflow,
        softWrap: Boolean,
        maxLines: Int,
        minLines: Int,
        placeholders: List<AnnotatedString.Range<Placeholder>>?
    ) {
        this.text = text
        this.style = style
        this.fontFamilyResolver = fontFamilyResolver
        this.overflow = overflow
        this.softWrap = softWrap
        this.maxLines = maxLines
        this.minLines = minLines
        this.placeholders = placeholders
        markDirty()
    }

    /**
     * Minimum information required to compute [MultiParagraphIntrinsics].
     *
     * After calling paragraphIntrinsics is cached.
     */
    private fun setLayoutDirection(layoutDirection: LayoutDirection): MultiParagraphIntrinsics {
        val localIntrinsics = paragraphIntrinsics
        val intrinsics =
            if (
                localIntrinsics == null ||
                    layoutDirection != intrinsicsLayoutDirection ||
                    localIntrinsics.hasStaleResolvedFonts
            ) {
                intrinsicsLayoutDirection = layoutDirection
                MultiParagraphIntrinsics(
                    annotatedString = text,
                    style = resolveDefaults(style, layoutDirection),
                    density = density!!,
                    fontFamilyResolver = fontFamilyResolver,
                    placeholders = placeholders.orEmpty()
                )
            } else {
                localIntrinsics
            }

        paragraphIntrinsics = intrinsics
        return intrinsics
    }

    /**
     * Computes the visual position of the glyphs for painting the text.
     *
     * The text will layout with a width that's as close to its max intrinsic width as possible
     * while still being greater than or equal to `minWidth` and less than or equal to `maxWidth`.
     */
    private fun layoutText(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MultiParagraph {
        val localParagraphIntrinsics = setLayoutDirection(layoutDirection)

        return MultiParagraph(
            intrinsics = localParagraphIntrinsics,
            constraints =
                finalConstraints(
                    constraints,
                    softWrap,
                    overflow,
                    localParagraphIntrinsics.maxIntrinsicWidth
                ),
            // This is a fallback behavior for ellipsis. Native
            maxLines = finalMaxLines(softWrap, overflow, maxLines),
            overflow = overflow
        )
    }

    /**
     * Attempt to compute if the new layout will be the same for the given constraints and
     * layoutDirection.
     */
    private fun TextLayoutResult?.newLayoutWillBeDifferent(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): Boolean {
        // no layout yet
        if (this == null) return true

        // async typeface changes
        if (this.multiParagraph.intrinsics.hasStaleResolvedFonts) return true

        // layout direction changed
        if (layoutDirection != layoutInput.layoutDirection) return true

        // if we were passed identical constraints just skip more work
        if (constraints == layoutInput.constraints) return false

        if (constraints.maxWidth != layoutInput.constraints.maxWidth) return true

        // if we get here width won't change, height may be clipped
        if (constraints.maxHeight < multiParagraph.height || multiParagraph.didExceedMaxLines) {
            // vertical clip changes
            return true
        }

        // breaks can't change, height can't change
        return false
    }

    /**
     * Compute the maxWidth for text layout from [Constraints]
     *
     * Falls back to [paragraphIntrinsics.maxIntrinsicWidth] when not exact constraints.
     */
    private fun maxWidth(constraints: Constraints): Int =
        finalMaxWidth(constraints, softWrap, overflow, paragraphIntrinsics!!.maxIntrinsicWidth)

    private fun markDirty() {
        paragraphIntrinsics = null
        layoutCache = null
        cachedIntrinsicHeight = -1
        cachedIntrinsicHeightInputWidth = -1
    }

    /** The width at which increasing the width of the text no longer decreases the height. */
    fun maxIntrinsicWidth(layoutDirection: LayoutDirection): Int {
        return setLayoutDirection(layoutDirection).maxIntrinsicWidth.ceilToIntPx()
    }

    /** The width for text if all soft wrap opportunities were taken. */
    fun minIntrinsicWidth(layoutDirection: LayoutDirection): Int {
        return setLayoutDirection(layoutDirection).minIntrinsicWidth.ceilToIntPx()
    }
}
