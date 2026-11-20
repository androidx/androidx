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

package androidx.compose.ui.text

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.internal.requirePrecondition
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilteredMap

/**
 * Calculates and provides the intrinsic width of text that contains [ParagraphStyle].
 *
 * @see MultiParagraph
 * @see Placeholder
 */
public class MultiParagraphIntrinsics
@VisibleForTesting
internal constructor(
    /** The text to be laid out. */
    public val annotatedString: AnnotatedString,
    /**
     * A list of [Placeholder]s that specify ranges of text which will be skipped during layout and
     * replaced with [Placeholder].
     */
    public val placeholders: List<AnnotatedString.Range<Placeholder>>,
    internal val infoList: List<ParagraphIntrinsicInfo>,
) : ParagraphIntrinsics {

    /**
     * Calculates and provides the intrinsic width and height of text that contains
     * [ParagraphStyle].
     *
     * @param annotatedString the text to be laid out
     * @param style the [TextStyle] to be applied to the whole text
     * @param placeholders a list of [Placeholder]s that specify ranges of text which will be
     *   skipped during layout and replaced with [Placeholder]. It's required that the range of each
     *   [Placeholder] doesn't cross paragraph boundary, otherwise [IllegalArgumentException] is
     *   thrown.
     * @param density density of the device
     * @param fontFamilyResolver [FontFamily.Resolver] to be used to load the font given in
     *   [SpanStyle]s
     * @param softWrap Whether the text should break at soft line breaks. When the intention is to
     *   lay out text as a single line, setting [softWrap] to false enables optimizations that avoid
     *   certain expensive calculations
     * @param defaultLocaleList the default locale list to use for formatting.
     * @throws IllegalArgumentException if any of the [placeholders] crosses a paragraph boundary.
     */
    public constructor(
        annotatedString: AnnotatedString,
        style: TextStyle,
        placeholders: List<AnnotatedString.Range<Placeholder>>,
        density: Density,
        fontFamilyResolver: FontFamily.Resolver,
        softWrap: Boolean,
        defaultLocaleList: LocaleList,
    ) : this(
        annotatedString = annotatedString,
        placeholders = placeholders,
        infoList =
            createParagraphIntrinsicInfoList(
                annotatedString = annotatedString,
                style = style,
                placeholders = placeholders,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                softWrap = softWrap,
                defaultLocaleList = defaultLocaleList,
            ),
    )

    @Suppress("DEPRECATION")
    @Deprecated(
        "Use an overload with `defaultLocaleList` instead",
        ReplaceWith(
            "MultiParagraphIntrinsics(annotatedString, style, placeholders, density, fontFamilyResolver, softWrap, LocaleList.current)"
        ),
    )
    public constructor(
        annotatedString: AnnotatedString,
        style: TextStyle,
        placeholders: List<AnnotatedString.Range<Placeholder>>,
        density: Density,
        fontFamilyResolver: FontFamily.Resolver,
        softWrap: Boolean,
    ) : this(
        annotatedString,
        style,
        placeholders,
        density,
        fontFamilyResolver,
        softWrap,
        LocaleList.current,
    )

    @Suppress("DEPRECATION")
    @Deprecated(
        "Use an overload with `softWrap` instead",
        ReplaceWith(
            "MultiParagraphIntrinsics(annotatedString, style, placeholders, density, fontFamilyResolver, true)"
        ),
    )
    public constructor(
        annotatedString: AnnotatedString,
        style: TextStyle,
        placeholders: List<AnnotatedString.Range<Placeholder>>,
        density: Density,
        fontFamilyResolver: FontFamily.Resolver,
    ) : this(annotatedString, style, placeholders, density, fontFamilyResolver, true)

    @Suppress("DEPRECATION")
    @Deprecated(
        "Font.ResourceLoader is deprecated, call with fontFamilyResolver",
        replaceWith =
            ReplaceWith(
                "MultiParagraphIntrinsics(annotatedString, style, " +
                    "placeholders, density, createFontFamilyResolver(resourceLoader), true)",
                "androidx.compose.ui.text.font.createFontFamilyResolver",
            ),
    )
    public constructor(
        annotatedString: AnnotatedString,
        style: TextStyle,
        placeholders: List<AnnotatedString.Range<Placeholder>>,
        density: Density,
        resourceLoader: Font.ResourceLoader,
    ) : this(
        annotatedString,
        style,
        placeholders,
        density,
        createFontFamilyResolver(resourceLoader),
    )

    private var _minIntrinsicWidth: Float = Float.NaN
    public override val minIntrinsicWidth: Float
        get() {
            var v = _minIntrinsicWidth
            if (v.isNaN()) {
                // Direct indexed loop evaluates each paragraph's width once and avoids redundant
                // getter calls.
                var max = 0f
                val list = infoList
                for (i in list.indices) {
                    val w = list[i].intrinsics.minIntrinsicWidth
                    if (w > max) max = w
                }
                v = max
                _minIntrinsicWidth = v
            }
            return v
        }

    private var _maxIntrinsicWidth: Float = Float.NaN
    public override val maxIntrinsicWidth: Float
        get() {
            var v = _maxIntrinsicWidth
            if (v.isNaN()) {
                // Direct indexed loop evaluates each paragraph's width once and avoids redundant
                // getter calls.
                var max = 0f
                val list = infoList
                for (i in list.indices) {
                    val w = list[i].intrinsics.maxIntrinsicWidth
                    if (w > max) max = w
                }
                v = max
                _maxIntrinsicWidth = v
            }
            return v
        }

    public override val hasStaleResolvedFonts: Boolean
        get() = infoList.fastAny { it.intrinsics.hasStaleResolvedFonts }
}

/** Creates a list of [ParagraphIntrinsicInfo] for each paragraph in [annotatedString]. */
private fun createParagraphIntrinsicInfoList(
    annotatedString: AnnotatedString,
    style: TextStyle,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    softWrap: Boolean,
    defaultLocaleList: LocaleList,
): List<ParagraphIntrinsicInfo> =
    if (!PackedTextStylesLatch.isEnabled) {
        val paragraphStyle = style.toParagraphStyle()
        annotatedString.mapEachParagraphStyle(paragraphStyle) { annotatedString, paragraphStyleItem
            ->
            val currentParagraphStyle =
                resolveTextDirection(paragraphStyleItem.item, paragraphStyle)

            ParagraphIntrinsicInfo(
                intrinsics =
                    ParagraphIntrinsics(
                        text = annotatedString.text,
                        style = style.merge(currentParagraphStyle),
                        annotations = annotatedString.annotations ?: emptyList(),
                        placeholders =
                            placeholders.getLocalPlaceholders(
                                paragraphStyleItem.start,
                                paragraphStyleItem.end,
                            ),
                        density = density,
                        fontFamilyResolver = fontFamilyResolver,
                        softWrap = softWrap,
                        defaultLocaleList = defaultLocaleList,
                    ),
                startIndex = paragraphStyleItem.start,
                endIndex = paragraphStyleItem.end,
            )
        }
    } else if (annotatedString.paragraphStylesOrNull.isNullOrEmpty()) {
        listOf(
            paragraphInfo(
                annotatedString,
                style,
                placeholders.getLocalPlaceholders(0, annotatedString.text.length),
                0,
                annotatedString.text.length,
                density,
                fontFamilyResolver,
                softWrap,
                defaultLocaleList,
            )
        )
    } else {
        val paragraphStyle = style.paragraphStyle
        annotatedString.mapEachParagraphStyle(paragraphStyle) { annotatedString, paragraphStyleItem
            ->
            paragraphInfo(
                annotatedString,
                style.merge(resolveTextDirection(paragraphStyleItem.item, paragraphStyle)),
                placeholders.getLocalPlaceholders(
                    paragraphStyleItem.start,
                    paragraphStyleItem.end,
                ),
                paragraphStyleItem.start,
                paragraphStyleItem.end,
                density,
                fontFamilyResolver,
                softWrap,
                defaultLocaleList,
            )
        }
    }

/**
 * if the [style] does `not` have [TextDirection] set, it will return a new [ParagraphStyle] where
 * [TextDirection] is set using the [defaultStyle]. Otherwise returns the same [style] object.
 *
 * @param style ParagraphStyle to be checked for [TextDirection]
 * @param defaultStyle [ParagraphStyle] passed to [MultiParagraphIntrinsics] as the main style
 */
@VisibleForTesting
internal fun resolveTextDirection(
    style: ParagraphStyle,
    defaultStyle: ParagraphStyle,
): ParagraphStyle {
    return if (style.textDirection != TextDirection.Unspecified) style
    else style.copy(textDirection = defaultStyle.textDirection)
}

@VisibleForTesting
internal fun List<AnnotatedString.Range<Placeholder>>.getLocalPlaceholders(
    start: Int,
    end: Int,
): List<AnnotatedString.Range<Placeholder>> {
    if (!PackedTextStylesLatch.isEnabled) {
        return fastFilteredMap({ intersect(start, end, it.start, it.end) }) {
            requirePrecondition(start <= it.start && it.end <= end) {
                "placeholder can not overlap with paragraph."
            }
            AnnotatedString.Range(it.item, it.start - start, it.end - start)
        }
    }
    if (isEmpty()) return emptyList()
    var result: ArrayList<AnnotatedString.Range<Placeholder>>? = null
    for (i in indices) {
        val placeholderRange = this[i]
        if (intersect(start, end, placeholderRange.start, placeholderRange.end)) {
            requirePrecondition(start <= placeholderRange.start && placeholderRange.end <= end) {
                "placeholder can not overlap with paragraph."
            }
            val list =
                result ?: ArrayList<AnnotatedString.Range<Placeholder>>(size).also { result = it }
            list.add(
                if (start == 0) {
                    placeholderRange
                } else {
                    AnnotatedString.Range(
                        placeholderRange.item,
                        placeholderRange.start - start,
                        placeholderRange.end - start,
                    )
                }
            )
        }
    }
    return result ?: emptyList()
}

internal data class ParagraphIntrinsicInfo(
    val intrinsics: ParagraphIntrinsics,
    val startIndex: Int,
    val endIndex: Int,
)

private fun paragraphInfo(
    text: AnnotatedString,
    style: TextStyle,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    start: Int,
    end: Int,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    softWrap: Boolean,
    defaultLocaleList: LocaleList,
) =
    ParagraphIntrinsicInfo(
        intrinsics =
            ParagraphIntrinsics(
                text = text.text,
                style = style,
                annotations = if (start == end) emptyList() else (text.annotations ?: emptyList()),
                placeholders = placeholders,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                softWrap = softWrap,
                defaultLocaleList = defaultLocaleList,
            ),
        startIndex = start,
        endIndex = end,
    )
