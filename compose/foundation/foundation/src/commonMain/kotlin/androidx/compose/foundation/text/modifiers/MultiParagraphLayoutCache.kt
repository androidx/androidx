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

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.text.DefaultMinLines
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
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
    style: TextStyle,
    private var fontFamilyResolver: FontFamily.Resolver,
    private var overflow: TextOverflow = TextOverflow.Clip,
    private var softWrap: Boolean = true,
    private var maxLines: Int = Int.MAX_VALUE,
    private var minLines: Int = DefaultMinLines,
    private var placeholders: List<AnnotatedString.Range<Placeholder>>? = null,
    private var autoSize: TextAutoSize? = null,
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
                recordHistory(LayoutCacheOperation.MarkDirtyDensity)
                markDirty()
            }
        }

    /**
     * The style used for layout. Marks style-affected cache properties dirty if the new style's
     * layout-affecting attributes are different.
     */
    private var style: TextStyle = style
        set(value) {
            val newStyleHasSameLayoutAffectingAttrs = value.hasSameLayoutAffectingAttributes(field)
            field = value
            if (!newStyleHasSameLayoutAffectingAttrs) {
                markStyleAffectedDirty()
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

    /** Backing property for [fontSizeSearchScope] */
    private var _textAutoSizeLayoutScope: TextAutoSizeLayoutScopeImpl? = null

    /** Used to get the font size if AutoSize is enabled and perform layout with many font sizes */
    private val fontSizeSearchScope: TextAutoSizeLayoutScopeImpl
        get() {
            if (_textAutoSizeLayoutScope == null)
                _textAutoSizeLayoutScope = TextAutoSizeLayoutScopeImpl()
            return _textAutoSizeLayoutScope!!
        }

    /** The last computed TextLayoutResult, or throws if not initialized. */
    val textLayoutResult: TextLayoutResult
        get() =
            layoutCache
                ?: throw IllegalStateException(
                    "Internal Error: MultiParagraphLayoutCache could not provide TextLayoutResult during the draw phase. Please report this bug on the official Issue Tracker with the following diagnostic information: ${toString()}"
                )

    /** The last computed TextLayoutResult, or null if not initialized. */
    val layoutOrNull: TextLayoutResult?
        get() = layoutCache

    /**
     * A 64-bit flag that records the history of `markDirty`, `markStyleDirty`, and
     * `layoutWithConstraints` operations. Each 2-bit segment represents a distinct operation.
     * Consequently, this flag maintains a record of the last 32 operations performed on this cache.
     *
     * Bit representation:
     * ```
     *   | Operation                | Bits |
     *   | :----------------------- | :--- |
     *   | markStyleDirty           | 00   |
     *   | markDirtyDensity         | 01   |
     *   | markDirtyNodeUpdate      | 10   |
     *   | layoutWithConstraints    | 11   |
     * ```
     *
     * With the operations encoded in 2 bit segments and read from right to left. For example:
     * ```
     *   01111000 would represent that the last 4 operations performed were
     *   1. markStyleDirty (00)
     *   2. markDirtyNodeUpdate (10)
     *   3. layoutWithConstraints (11)
     *   4. markDirtyDensity (01)
     * ```
     *
     * This history can be used to debug or print as a log of what operations have been performed on
     * this [MultiParagraphLayoutCache].
     */
    @VisibleForTesting internal var historyFlag: Long = 0L

    private fun recordHistory(op: LayoutCacheOperation) {
        historyFlag = (historyFlag shl 2) or op.flag
    }

    /**
     * Update layout constraints for this text
     *
     * @return true if constraints caused a text layout invalidation
     */
    fun layoutWithConstraints(constraints: Constraints, layoutDirection: LayoutDirection): Boolean {
        recordHistory(LayoutCacheOperation.LayoutWithConstraints)
        val finalConstraints =
            if (minLines > 1) {
                useMinLinesConstrainer(constraints, layoutDirection)
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
                    multiParagraph = layoutCache!!.multiParagraph,
                )
            return true
        }
        if (autoSize != null) {
            intrinsicsLayoutDirection = layoutDirection
            val fontSizeBeforeLayout = style.fontSize
            // Here's where we perform auto size layout
            val optimalFontSize =
                with(autoSize!!) {
                    with(fontSizeSearchScope) {
                        var autoSizeFontSize = getFontSize(constraints, text)
                        if (autoSizeFontSize.isEm) {
                            autoSizeFontSize = fontSizeBeforeLayout * autoSizeFontSize
                        }
                        autoSizeFontSize
                    }
                }
            val autoSizeLayoutCache = fontSizeSearchScope.lastLayoutResult
            // After auto size layout, check if we have a cached result with the same font size.
            // If we do, we can populate the layoutCache and return early
            if (
                autoSizeLayoutCache != null &&
                    optimalFontSize == autoSizeLayoutCache.layoutInput.style.fontSize &&
                    autoSizeLayoutCache.layoutInput.overflow == overflow
            ) {
                layoutCache = autoSizeLayoutCache
                return true
            }
            // If our cache doesn't match the layout input, we need to update the style to mark the
            // relevant caches dirty and perform another layout pass
            style = style.copy(fontSize = optimalFontSize)
        }

        val multiParagraph = layoutText(finalConstraints, layoutDirection)

        layoutCache = textLayoutResult(layoutDirection, finalConstraints, multiParagraph)
        return true
    }

    private fun useMinLinesConstrainer(
        constraints: Constraints,
        layoutDirection: LayoutDirection,
    ): Constraints {
        val localMin =
            MinLinesConstrainer.from(
                    mMinLinesConstrainer,
                    layoutDirection,
                    style,
                    density!!,
                    fontFamilyResolver,
                )
                .also { mMinLinesConstrainer = it }
        return localMin.coerceMinLines(inConstraints = constraints, minLines = minLines)
    }

    private fun textLayoutResult(
        layoutDirection: LayoutDirection,
        finalConstraints: Constraints,
        multiParagraph: MultiParagraph,
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
                finalConstraints,
            ),
            multiParagraph,
            finalConstraints.constrain(
                IntSize(layoutWidth.ceilToIntPx(), multiParagraph.height.ceilToIntPx())
            ),
        )
    }

    /** The natural height of text at [width] in [layoutDirection] */
    fun intrinsicHeight(width: Int, layoutDirection: LayoutDirection): Int {
        val localWidth = cachedIntrinsicHeightInputWidth
        val localHeght = cachedIntrinsicHeight
        if (width == localWidth && localWidth != -1) return localHeght
        val constraints = Constraints(0, width, 0, Constraints.Infinity)
        val finalConstraints =
            if (minLines > 1) {
                useMinLinesConstrainer(constraints, layoutDirection)
            } else {
                constraints
            }
        val result =
            layoutText(finalConstraints, layoutDirection)
                .height
                .ceilToIntPx()
                .coerceAtLeast(finalConstraints.minHeight)

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
        placeholders: List<AnnotatedString.Range<Placeholder>>?,
        autoSize: TextAutoSize?,
    ) {
        this.text = text
        this.style = style
        this.fontFamilyResolver = fontFamilyResolver
        this.overflow = overflow
        this.softWrap = softWrap
        this.maxLines = maxLines
        this.minLines = minLines
        this.placeholders = placeholders
        this.autoSize = autoSize
        recordHistory(LayoutCacheOperation.MarkDirtyNode)
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
                    placeholders = placeholders.orEmpty(),
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
        layoutDirection: LayoutDirection,
    ): MultiParagraph {
        val localParagraphIntrinsics = setLayoutDirection(layoutDirection)

        return MultiParagraph(
            intrinsics = localParagraphIntrinsics,
            constraints =
                finalConstraints(
                    constraints,
                    softWrap,
                    overflow,
                    localParagraphIntrinsics.maxIntrinsicWidth,
                ),
            maxLines = finalMaxLines(softWrap, overflow, maxLines),
            overflow = overflow,
        )
    }

    /**
     * Attempt to compute if the new layout will be the same for the given constraints and
     * layoutDirection.
     */
    private fun TextLayoutResult?.newLayoutWillBeDifferent(
        constraints: Constraints,
        layoutDirection: LayoutDirection,
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
        if (constraints.minWidth != layoutInput.constraints.minWidth) return true

        // if we get here width won't change, height may be clipped
        if (constraints.maxHeight < multiParagraph.height || multiParagraph.didExceedMaxLines) {
            // vertical clip changes
            return true
        }

        // breaks can't change, height can't change
        return false
    }

    private fun markDirty() {
        paragraphIntrinsics = null
        layoutCache = null
        cachedIntrinsicHeight = -1
        cachedIntrinsicHeightInputWidth = -1
        _textAutoSizeLayoutScope = null
    }

    private fun markStyleAffectedDirty() {
        recordHistory(LayoutCacheOperation.MarkDirtyStyle)
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

    /** [MultiParagraph] specific implementation of [TextAutoSizeLayoutScope] */
    private inner class TextAutoSizeLayoutScopeImpl : TextAutoSizeLayoutScope {

        // override Density attributes
        override val density
            get() = this@MultiParagraphLayoutCache.density!!.density

        override val fontScale
            get() = this@MultiParagraphLayoutCache.density!!.fontScale

        /** The last [TextLayoutResult] from measuring the text */
        var lastLayoutResult: TextLayoutResult? = null
            private set

        override fun performLayout(
            constraints: Constraints,
            text: AnnotatedString,
            fontSize: TextUnit,
        ): TextLayoutResult {
            val styleBeforeLayout = style
            // If the cache is populated with a SP [TextUnit] and [performLayout]'s requested
            // fontSize is in EM, we want to scale the EM value to the SP value.
            val scaledFontSize =
                if (fontSize.isEm) {
                    style.fontSize * fontSize
                } else fontSize
            if (scaledFontSize != style.fontSize) {
                style = style.copy(fontSize = scaledFontSize)
            }

            val layoutConstraints =
                if (minLines > 1) useMinLinesConstrainer(constraints, intrinsicsLayoutDirection!!)
                else constraints

            val multiParagraph = layoutText(layoutConstraints, intrinsicsLayoutDirection!!)
            val layoutResult =
                textLayoutResult(intrinsicsLayoutDirection!!, layoutConstraints, multiParagraph)
            lastLayoutResult = layoutResult
            style = styleBeforeLayout
            return layoutResult
        }

        override fun TextUnit.toPx(): Float {
            if (isEm) {
                check(!style.fontSize.isEm) {
                    "InternalAutoSize -> toPx(): Cannot convert Em to Px when style.fontSize is Em\n" +
                        "Declare the composable's style.fontSize with Sp units instead."
                }
                check(style.fontSize != TextUnit.Unspecified) {
                    "InternalAutoSize -> toPx(): Cannot convert Em to Px when style.fontSize is " +
                        "not set. Please specify a font size."
                }
                return style.fontSize.toPx() * value
            }
            return toDp().toPx()
        }
    }

    override fun toString(): String =
        "MultiParagraphLayoutCache(textLayoutResult=${if (layoutCache != null) "<TextLayoutResult>" else "null"}, " +
            "lastDensity=$lastDensity, history=$historyFlag, constraints=${layoutCache?.layoutInput?.constraints ?: "null"})"
}

/**
 * Multiply [this] text unit by the [other] EM value.
 *
 * @return The multiplied text unit, or [DefaultFontSize] if [this] is [TextUnit.Unspecified]
 * @throws IllegalStateException If both [this] and the [other] are EM
 * @throws IllegalArgumentException If the [other] is not in EM
 */
private operator fun TextUnit.times(other: TextUnit): TextUnit {
    if (other.isEm) {
        if (this.isEm) {
            throw IllegalStateException(
                "Cannot convert Em to Px when style.fontSize is" +
                    " Em ($other). Please declare the style.fontSize" +
                    " with Sp units instead."
            )
        }
        if (this.isUnspecified) {
            // hardcoded here as DefaultFontSize is private in SpanStyle
            // TODO(b/364858402): Make DefaultFontSize public
            return DefaultFontSize * other.value
        }
        return this * other.value
    } else {
        throw IllegalArgumentException("The multiplier must be in em, but was $other.")
    }
}

private val DefaultFontSize = 14.sp
