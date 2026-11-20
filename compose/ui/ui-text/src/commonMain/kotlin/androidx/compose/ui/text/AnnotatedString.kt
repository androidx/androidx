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

import androidx.collection.mutableIntListOf
import androidx.collection.mutableLongListOf
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.text.AnnotatedString.Annotation
import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.internal.checkPrecondition
import androidx.compose.ui.text.internal.requirePrecondition
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType.Companion.Em
import androidx.compose.ui.unit.TextUnitType.Companion.Sp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFilteredMap
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

/**
 * Text with multiple styles.
 *
 * [SpanStyle] applies character-level styling (such as color, font, or decorations) to a range of
 * text. [ParagraphStyle] applies paragraph-level layout configuration (such as alignment, line
 * height, or indent) to the entire paragraph.
 *
 * Use [Builder] to construct.
 *
 * ### Precedence and Merging Rules
 * When multiple [SpanStyle]s are applied to overlapping ranges, they are merged character by
 * character:
 * - Styles appearing later in the [spanStyles] list take precedence and overwrite matching
 *   properties of earlier styles.
 * - Any unspecified properties (such as [androidx.compose.ui.graphics.Color.Unspecified] or
 *   [TextUnit.Unspecified]) do not overwrite prior styles, retaining the value from the previous
 *   style in the stack or the default text style.
 *
 * ### Paragraphs Arrangement
 *
 * [ParagraphStyle]s can be applied to parts of the text. However, paragraph ranges must not
 * partially overlap. They can only be nested or fully overlapping. If ranges are invalid, an
 * [IllegalArgumentException] is thrown.
 *
 * Valid arrangements (nested or non-overlapping):
 * - Non-overlapping: `\[abc\](def)` (two separate paragraphs)
 * - Nested: `\[abc(def)ghi\]` (inner paragraph `(def)` nested inside outer `\[abc...ghi\]`)
 * - Fully overlapping: `\[(abc)\]`
 *
 * Invalid arrangement (partial overlap):
 * - Overlapping: `\[abc(def\]ghi)` (inner starts inside outer, but ends outside - invalid!)
 *
 * Gaps between paragraph styles are filled with default styles. Nested paragraphs are split and
 * merged with parent styles.
 *
 * @property text text to display
 * @see SpanStyle
 * @see ParagraphStyle
 */
@Immutable
public class AnnotatedString
internal constructor(
    internal val annotations: List<Range<out Annotation>>?,
    public val text: String,
) : CharSequence {

    internal val spanStylesOrNull: List<Range<SpanStyle>>?
    /** All [SpanStyle] that have been applied to a range of this String */
    public val spanStyles: List<Range<SpanStyle>>
        get() = spanStylesOrNull ?: emptyList()

    internal val paragraphStylesOrNull: List<Range<ParagraphStyle>>?
    /** All [ParagraphStyle] that have been applied to a range of this String */
    public val paragraphStyles: List<Range<ParagraphStyle>>
        get() = paragraphStylesOrNull ?: emptyList()

    /**
     * True when [annotations] contains any [Annotation] other than [SpanStyle] or [ParagraphStyle]
     * (such as [StringAnnotation], [LinkAnnotation], [TtsAnnotation], [UrlAnnotation], [Bullet], or
     * [DrawAnnotation]), allowing query methods to skip scanning [annotations] when only styles are
     * present. Always `true` when [PackedTextStylesLatch.isEnabled] is `false`.
     */
    internal val hasOtherAnnotations: Boolean

    /**
     * Creates an [AnnotatedString] with pre-validated annotations.
     *
     * @param text text to display
     * @param annotations list of annotations to apply to text
     * @param trusted true if [annotations] is already safely sorted and immutable (e.g. built by
     *   [Builder] or [transform]) and does not require defensive copying.
     */
    internal constructor(
        text: String,
        annotations: List<Range<out Annotation>>?,
        trusted: Boolean,
    ) : this(
        annotations = annotations.nullIfEmpty()?.let { if (trusted) it else ArrayList(it) },
        text = text,
    )

    /**
     * Creates an [AnnotatedString] with styles.
     *
     * Use alternative constructor for other [Annotation] types.
     *
     * @param text text to display
     * @param spanStyles styles to apply to text. Overlapping [SpanStyle]s merge (see
     *   [AnnotatedString] merging rules).
     * @param paragraphStyles paragraph styles to apply to ranges of text. Ranges must follow the
     *   paragraph arrangement rules (see [AnnotatedString] class documentation).
     * @throws IllegalArgumentException if [paragraphStyles] contains invalid overlapping ranges
     * @sample androidx.compose.ui.text.samples.AnnotatedStringConstructorSample
     * @see SpanStyle
     * @see ParagraphStyle
     */
    public constructor(
        text: String,
        spanStyles: List<Range<SpanStyle>> = emptyList(),
        paragraphStyles: List<Range<ParagraphStyle>> = emptyList(),
    ) : this(
        text = text,
        annotations = constructAnnotationsFromSpansAndParagraphs(spanStyles, paragraphStyles),
        trusted = false,
    )

    /**
     * Creates an [AnnotatedString] with annotations.
     *
     * @param text text to display
     * @param annotations annotations to apply to text. Overlapping [SpanStyle]s merge (see
     *   [AnnotatedString] merging rules). [ParagraphStyle] ranges must follow paragraph arrangement
     *   rules (see [AnnotatedString] class documentation).
     * @throws IllegalArgumentException if [annotations] contains invalid overlapping paragraph
     *   ranges
     * @sample androidx.compose.ui.text.samples.AnnotatedStringMainConstructorSample
     * @see Annotation
     */
    public constructor(
        text: String,
        annotations: List<Range<out Annotation>> = emptyList(),
    ) : this(
        text = text,
        annotations = annotations,
        trusted = false,
    )

    init {
        if (!PackedTextStylesLatch.isEnabled) {
            val (spanStyles, paragraphStyles) = legacyInit(annotations)
            spanStylesOrNull = spanStyles
            paragraphStylesOrNull = paragraphStyles
            // In the legacy path, set hasOtherAnnotations to true so methods like
            // hasStringAnnotations, hasLinkAnnotations, and hasDrawAnnotations work
            // without needing an additional feature flag check.
            hasOtherAnnotations = true
        } else {
            val rawAnnotations = annotations
            var spanStyles: List<Range<SpanStyle>>? = null
            var paragraphStyles: List<Range<ParagraphStyle>>? = null
            var otherAnnotations = false
            if (rawAnnotations != null) {
                // Count spans and paragraphs in a first pass: profiling confirmed it is faster
                // to count first and allocate exact-capacity lists than dynamically resizing in one
                // pass.
                val size = rawAnnotations.size
                var spanCount = 0
                var paragraphCount = 0
                for (i in 0 until size) {
                    when (rawAnnotations[i].item) {
                        is SpanStyle -> spanCount++
                        is ParagraphStyle -> paragraphCount++
                        else -> otherAnnotations = true
                    }
                }
                if (spanCount == size) {
                    spanStyles =
                        @Suppress("UNCHECKED_CAST") (rawAnnotations as List<Range<SpanStyle>>)
                } else if (paragraphCount == size) {
                    paragraphStyles =
                        @Suppress("UNCHECKED_CAST") (rawAnnotations as List<Range<ParagraphStyle>>)
                } else {
                    val collectedSpans =
                        if (spanCount > 0) ArrayList<Range<SpanStyle>>(spanCount) else null
                    val collectedParagraphs =
                        if (paragraphCount > 0) {
                            ArrayList<Range<ParagraphStyle>>(paragraphCount)
                        } else {
                            null
                        }
                    for (i in 0 until size) {
                        val annotation = rawAnnotations[i]
                        when (annotation.item) {
                            is SpanStyle ->
                                collectedSpans?.add(
                                    @Suppress("UNCHECKED_CAST") (annotation as Range<SpanStyle>)
                                )
                            is ParagraphStyle ->
                                collectedParagraphs?.add(
                                    @Suppress("UNCHECKED_CAST")
                                    (annotation as Range<ParagraphStyle>)
                                )
                            else -> {}
                        }
                    }
                    spanStyles = collectedSpans
                    paragraphStyles = collectedParagraphs
                }
            }
            spanStylesOrNull = spanStyles
            paragraphStylesOrNull = paragraphStyles
            hasOtherAnnotations = otherAnnotations

            val sorted = sortParagraphStyles(paragraphStyles)
            if (sorted != null && sorted.size > 1) {
                var hasNestedOrOverlap = false
                for (i in 1 until sorted.size) {
                    if (sorted[i].start < sorted[i - 1].end) {
                        hasNestedOrOverlap = true
                        break
                    }
                }
                if (hasNestedOrOverlap) {
                    // ParagraphStyle should not overlap, Throws an exception if there is
                    // overlapping paragraphStyle.
                    // [*************************************].....
                    // ...[xxxxxx].................................
                    // ...............[xxxxxxxxxx].................
                    // ..............................[xxxxxxxxxxxx]
                    // ................... [**]...[**]...[**]..[**]
                    val previousEnds = mutableIntListOf(sorted[0].end)
                    for (i in 1 until sorted.size) {
                        val current = sorted[i]
                        while (previousEnds.isNotEmpty()) {
                            val previousEnd = previousEnds.last()
                            if (current.start >= previousEnd) {
                                previousEnds.removeAt(previousEnds.size - 1)
                            } else {
                                requirePrecondition(current.end <= previousEnd) {
                                    "Paragraph overlap not allowed, end ${current.end} should be less than or equal to $previousEnd"
                                }
                                break
                            }
                        }
                        previousEnds.add(current.end)
                    }
                }
            }
        }
    }

    private fun legacyInit(
        annotations: List<Range<out Annotation>>?
    ): Pair<List<Range<SpanStyle>>?, List<Range<ParagraphStyle>>?> {
        var spanStyles: MutableList<Range<SpanStyle>>? = null
        var paragraphStyles: MutableList<Range<ParagraphStyle>>? = null
        @Suppress("UNCHECKED_CAST")
        annotations?.fastForEach { annotation ->
            if (annotation.item is SpanStyle) {
                if (spanStyles == null) {
                    spanStyles = mutableListOf()
                }
                spanStyles.add(annotation as Range<SpanStyle>)
            } else if (annotation.item is ParagraphStyle) {
                if (paragraphStyles == null) {
                    paragraphStyles = mutableListOf()
                }
                paragraphStyles.add(annotation as Range<ParagraphStyle>)
            }
        }

        // ParagraphStyle should not overlap, Throws an exception if there is overlapping
        // paragraphStyle.
        // [*************************************].....
        // ...[xxxxxx].................................
        // ...............[xxxxxxxxxx].................
        // ..............................[xxxxxxxxxxxx]
        // ................... [**]...[**]...[**]..[**]
        @Suppress("ListIterator") val sorted = paragraphStyles?.sortedBy { it.start }
        if (!sorted.isNullOrEmpty()) {
            val previousEnds = mutableIntListOf(sorted.first().end)
            for (i in 1 until sorted.size) {
                val current = sorted[i]
                while (previousEnds.isNotEmpty()) {
                    val previousEnd = previousEnds.last()
                    if (current.start >= previousEnd) {
                        previousEnds.removeAt(previousEnds.size - 1)
                    } else {
                        requirePrecondition(current.end <= previousEnd) {
                            "Paragraph overlap not allowed, end ${current.end} should be less than or equal to $previousEnd"
                        }
                        break
                    }
                }
                previousEnds.add(current.end)
            }
        }
        return Pair(spanStyles, paragraphStyles)
    }

    public override val length: Int
        get() = text.length

    public override operator fun get(index: Int): Char = text[index]

    /**
     * Return a substring for the AnnotatedString and include the styles in the range of
     * [startIndex] (inclusive) and [endIndex] (exclusive).
     *
     * @param startIndex the inclusive start offset of the range
     * @param endIndex the exclusive end offset of the range
     */
    public override fun subSequence(startIndex: Int, endIndex: Int): AnnotatedString {
        requirePrecondition(startIndex <= endIndex) {
            "start ($startIndex) should be less or equal to end ($endIndex)"
        }
        if (startIndex == 0 && endIndex == text.length) return this
        val text = text.substring(startIndex, endIndex)
        return AnnotatedString(
            text = text,
            annotations = filterRanges(annotations, startIndex, endIndex),
            trusted = true,
        )
    }

    /**
     * Return a substring for the AnnotatedString and include the styles in the given [range].
     *
     * @param range the text range
     * @see subSequence(start: Int, end: Int)
     */
    public fun subSequence(range: TextRange): AnnotatedString {
        return subSequence(range.min, range.max)
    }

    @Stable
    public operator fun plus(other: AnnotatedString): AnnotatedString {
        if (!PackedTextStylesLatch.isEnabled) {
            return with(Builder(this)) {
                append(other)
                toAnnotatedString()
            }
        }
        val lhsAnnotations = this.annotations
        val rhsAnnotations = other.annotations
        if (other.text.isEmpty() && rhsAnnotations == null) return this
        if (this.text.isEmpty() && lhsAnnotations == null) return other
        val offset = this.text.length
        val combined =
            when {
                lhsAnnotations == null && rhsAnnotations == null -> null
                rhsAnnotations == null -> lhsAnnotations
                lhsAnnotations == null && offset == 0 -> rhsAnnotations
                lhsAnnotations == null ->
                    ArrayList<Range<out Annotation>>(rhsAnnotations.size).also { list ->
                        rhsAnnotations.fastForEach {
                            list.add(Range(it.item, offset + it.start, offset + it.end, it.tag))
                        }
                    }
                else ->
                    ArrayList<Range<out Annotation>>(lhsAnnotations.size + rhsAnnotations.size)
                        .also { list ->
                            lhsAnnotations.fastForEach { list.add(it) }
                            rhsAnnotations.fastForEach {
                                list.add(Range(it.item, offset + it.start, offset + it.end, it.tag))
                            }
                        }
            }
        return AnnotatedString(
            text = this.text + other.text,
            annotations = combined,
            trusted = true,
        )
    }

    /**
     * Shared helper for both [getStringAnnotations] calls:
     * - `getStringAnnotations(tag: String, start: Int, end: Int)` passes a non-null [tag] to match
     *   only [StringAnnotation] items where `range.tag == tag`.
     * - `getStringAnnotations(start: Int, end: Int)` passes `tag = null` to match all
     *   [StringAnnotation] items for any tag.
     *
     * The `item is StringAnnotation` check skips all other items ([SpanStyle], [ParagraphStyle],
     * [Bullet], [TtsAnnotation], [LinkAnnotation]) even though they use `""` for `Range.tag`. A
     * search for `tag = ""` only returns [StringAnnotation] items that were added with an empty
     * `""` tag via [Builder.addStringAnnotation] or [Builder.pushStringAnnotation].
     */
    private fun queryStringAnnotations(tag: String?, start: Int, end: Int): List<Range<String>> {
        if (!PackedTextStylesLatch.isEnabled) {
            return if (tag != null) {
                annotations?.fastFilteredMap({
                    it.item is StringAnnotation &&
                        tag == it.tag &&
                        intersect(start, end, it.start, it.end)
                }) {
                    Range((it.item as StringAnnotation).value, it.start, it.end, it.tag)
                } ?: listOf()
            } else {
                annotations?.fastFilteredMap({
                    it.item is StringAnnotation && intersect(start, end, it.start, it.end)
                }) {
                    Range((it.item as StringAnnotation).value, it.start, it.end, it.tag)
                } ?: listOf()
            }
        }
        if (!hasOtherAnnotations) return emptyList()
        val list = annotations ?: return emptyList()
        var result: ArrayList<Range<String>>? = null
        list.fastForEach { range ->
            val item = range.item
            if (
                item is StringAnnotation &&
                    (tag == null || tag == range.tag) &&
                    intersect(start, end, range.start, range.end)
            ) {
                val matching = result ?: ArrayList<Range<String>>().also { result = it }
                matching.add(Range(item.value, range.start, range.end, range.tag))
            }
        }
        return result ?: emptyList()
    }

    /**
     * Query the string annotations attached on this AnnotatedString. Annotations are metadata
     * attached on the AnnotatedString, for example, a URL is a string metadata attached on the a
     * certain range. Annotations are also store with [Range] like the styles.
     *
     * @param tag the tag of the annotations that is being queried. It's used to distinguish the
     *   annotations for different purposes.
     * @param start the start of the query range, inclusive.
     * @param end the end of the query range, exclusive.
     * @return a list of annotations stored in [Range]. Notice that All annotations that intersect
     *   with the range [start, end) will be returned. When [start] is bigger than [end], an empty
     *   list will be returned.
     */
    public fun getStringAnnotations(tag: String, start: Int, end: Int): List<Range<String>> =
        queryStringAnnotations(tag, start, end)

    /**
     * Returns true if [getStringAnnotations] with the same parameters would return a non-empty list
     */
    public fun hasStringAnnotations(tag: String, start: Int, end: Int): Boolean {
        if (!hasOtherAnnotations) return false
        return annotations?.fastAny {
            it.item is StringAnnotation && tag == it.tag && intersect(start, end, it.start, it.end)
        } ?: false
    }

    /**
     * Query all of the string annotations attached on this AnnotatedString.
     *
     * @param start the start of the query range, inclusive.
     * @param end the end of the query range, exclusive.
     * @return a list of annotations stored in [Range]. Notice that All annotations that intersect
     *   with the range [start, end) will be returned. When [start] is bigger than [end], an empty
     *   list will be returned.
     */
    public fun getStringAnnotations(start: Int, end: Int): List<Range<String>> =
        queryStringAnnotations(null, start, end)

    /**
     * Query all of the [TtsAnnotation]s attached on this [AnnotatedString].
     *
     * @param start the start of the query range, inclusive.
     * @param end the end of the query range, exclusive.
     * @return a list of annotations stored in [Range]. Notice that All annotations that intersect
     *   with the range [start, end) will be returned. When [start] is bigger than [end], an empty
     *   list will be returned.
     */
    @Suppress("UNCHECKED_CAST")
    public fun getTtsAnnotations(start: Int, end: Int): List<Range<TtsAnnotation>> {
        if (!PackedTextStylesLatch.isEnabled) {
            return ((annotations?.fastFilter {
                it.item is TtsAnnotation && intersect(start, end, it.start, it.end)
            } ?: listOf())
                as List<Range<TtsAnnotation>>)
        }
        if (!hasOtherAnnotations) return emptyList()
        val list = annotations ?: return emptyList()
        var result: ArrayList<Range<TtsAnnotation>>? = null
        list.fastForEach { range ->
            if (range.item is TtsAnnotation && intersect(start, end, range.start, range.end)) {
                val matching = result ?: ArrayList<Range<TtsAnnotation>>().also { result = it }
                matching.add(range as Range<TtsAnnotation>)
            }
        }
        return result ?: emptyList()
    }

    /**
     * Query all of the [UrlAnnotation]s attached on this [AnnotatedString].
     *
     * @param start the start of the query range, inclusive.
     * @param end the end of the query range, exclusive.
     * @return a list of annotations stored in [Range]. Notice that All annotations that intersect
     *   with the range [start, end) will be returned. When [start] is bigger than [end], an empty
     *   list will be returned.
     */
    @ExperimentalTextApi
    @Suppress("UNCHECKED_CAST", "Deprecation")
    @Deprecated("Use LinkAnnotation API instead", ReplaceWith("getLinkAnnotations(start, end)"))
    public fun getUrlAnnotations(start: Int, end: Int): List<Range<UrlAnnotation>> {
        if (!PackedTextStylesLatch.isEnabled) {
            return ((annotations?.fastFilter {
                it.item is UrlAnnotation && intersect(start, end, it.start, it.end)
            } ?: listOf())
                as List<Range<UrlAnnotation>>)
        }
        if (!hasOtherAnnotations) return emptyList()
        val list = annotations ?: return emptyList()
        var result: ArrayList<Range<UrlAnnotation>>? = null
        list.fastForEach { range ->
            if (range.item is UrlAnnotation && intersect(start, end, range.start, range.end)) {
                val matching = result ?: ArrayList<Range<UrlAnnotation>>().also { result = it }
                matching.add(range as Range<UrlAnnotation>)
            }
        }
        return result ?: emptyList()
    }

    /**
     * Query all of the [LinkAnnotation]s attached on this [AnnotatedString].
     *
     * @param start the start of the query range, inclusive.
     * @param end the end of the query range, exclusive.
     * @return a list of annotations stored in [Range]. Notice that All annotations that intersect
     *   with the range [start, end) will be returned. When [start] is bigger than [end], an empty
     *   list will be returned.
     */
    @Suppress("UNCHECKED_CAST")
    public fun getLinkAnnotations(start: Int, end: Int): List<Range<LinkAnnotation>> {
        if (!PackedTextStylesLatch.isEnabled) {
            return ((annotations?.fastFilter {
                it.item is LinkAnnotation && intersect(start, end, it.start, it.end)
            } ?: listOf())
                as List<Range<LinkAnnotation>>)
        }
        if (!hasOtherAnnotations) return emptyList()
        val list = annotations ?: return emptyList()
        var result: ArrayList<Range<LinkAnnotation>>? = null
        list.fastForEach { range ->
            if (range.item is LinkAnnotation && intersect(start, end, range.start, range.end)) {
                val matching = result ?: ArrayList<Range<LinkAnnotation>>().also { result = it }
                matching.add(range as Range<LinkAnnotation>)
            }
        }
        return result ?: emptyList()
    }

    /**
     * Returns true if [getLinkAnnotations] with the same parameters would return a non-empty list
     */
    public fun hasLinkAnnotations(start: Int, end: Int): Boolean {
        if (!hasOtherAnnotations) return false
        return annotations?.fastAny {
            it.item is LinkAnnotation && intersect(start, end, it.start, it.end)
        } ?: false
    }

    /**
     * Queries all [DrawAnnotation]s attached on this [AnnotatedString].
     *
     * @param start inclusive start of the query range
     * @param end exclusive end of the query range
     * @return a list of annotations stored in [Range]. Returns all annotations that intersect with
     *   `[start, end)`, or an empty list when [start] is greater than [end]
     */
    @Suppress("UNCHECKED_CAST")
    public fun getDrawAnnotations(start: Int, end: Int): List<Range<DrawAnnotation>> {
        if (!PackedTextStylesLatch.isEnabled) {
            return ((annotations?.fastFilter {
                it.item is DrawAnnotation && intersect(start, end, it.start, it.end)
            } ?: listOf())
                as List<Range<DrawAnnotation>>)
        }
        if (!hasOtherAnnotations) return emptyList()
        val list = annotations ?: return emptyList()
        var result: ArrayList<Range<DrawAnnotation>>? = null
        list.fastForEach { range ->
            if (range.item is DrawAnnotation && intersect(start, end, range.start, range.end)) {
                val matching = result ?: ArrayList<Range<DrawAnnotation>>().also { result = it }
                matching.add(range as Range<DrawAnnotation>)
            }
        }
        return result ?: emptyList()
    }

    /**
     * Returns true if [getDrawAnnotations] with the same parameters would return a non-empty list.
     *
     * @param start inclusive start of the query range
     * @param end exclusive end of the query range
     * @see DrawAnnotation
     */
    public fun hasDrawAnnotations(start: Int, end: Int): Boolean {
        if (!hasOtherAnnotations) return false
        return annotations?.fastAny {
            it.item is DrawAnnotation && intersect(start, end, it.start, it.end)
        } ?: false
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnnotatedString) return false
        if (text != other.text) return false
        if (annotations != other.annotations) return false
        return true
    }

    public override fun hashCode(): Int {
        return 31 * text.hashCode() + (annotations?.hashCode() ?: 0)
    }

    public override fun toString(): String {
        // AnnotatedString.toString has special value, it converts it into regular String
        // rather than debug string.
        return text
    }

    /**
     * Compare the annotations between this and another AnnotatedString.
     *
     * This may be used for fast partial equality checks.
     *
     * Note that this checks all annotations including [spanStyles] and [paragraphStyles], but
     * [equals] still may be false if [text] is different.
     *
     * @param other to compare annotations with
     * @return true if and only if this compares equal on annotations with other
     */
    public fun hasEqualAnnotations(other: AnnotatedString): Boolean =
        this.annotations == other.annotations

    /**
     * Returns true if layout-affecting annotations between this [AnnotatedString] and [other] are
     * the same.
     *
     * * Compares non-[DrawAnnotation] ranges—such as [SpanStyle], [ParagraphStyle], [Bullet],
     *   [LinkAnnotation], [TtsAnnotation], and [StringAnnotation]—in place without allocating
     *   intermediate lists.
     * * Ignores [DrawAnnotation] entries because they only run during the draw phase and do not
     *   change text measurement or layout.
     * * Does not compare [text], matching [hasEqualAnnotations].
     *
     * @param other [AnnotatedString] whose layout-affecting annotations are compared with this
     *   instance
     * @return true if all non-[DrawAnnotation] ranges match in order, bounds, tag, and value
     * @see hasSameDrawAffectingAnnotations
     * @see hasEqualAnnotations
     * @see TextStyle.hasSameLayoutAffectingAttributes
     */
    public fun hasSameLayoutAffectingAnnotations(other: AnnotatedString): Boolean {
        if (this === other || this.annotations === other.annotations) return true
        val a = this.annotations
        val b = other.annotations
        val aSize = a?.size ?: 0
        val bSize = b?.size ?: 0
        var i = 0
        var j = 0
        while (true) {
            while (i < aSize && a!![i].item is DrawAnnotation) i++
            while (j < bSize && b!![j].item is DrawAnnotation) j++
            if (i >= aSize || j >= bSize) {
                return i >= aSize && j >= bSize
            }
            if (a!![i] != b!![j]) return false
            i++
            j++
        }
    }

    /**
     * Returns true if draw-affecting [DrawAnnotation] ranges between this [AnnotatedString] and
     * [other] are the same.
     *
     * * Compares [DrawAnnotation] ranges in order (including `[start, end)` bounds, `tag`, and item
     *   equality) in place without allocating intermediate lists.
     * * Ignores non-[DrawAnnotation] annotations and [text], matching [hasEqualAnnotations].
     *
     * @param other [AnnotatedString] whose [DrawAnnotation] ranges are compared with this instance
     * @return true if all [DrawAnnotation] ranges match in order, bounds, tag, and value
     * @see hasSameLayoutAffectingAnnotations
     * @see hasEqualAnnotations
     * @see TextStyle.hasSameDrawAffectingAttributes
     */
    public fun hasSameDrawAffectingAnnotations(other: AnnotatedString): Boolean {
        if (this === other || this.annotations === other.annotations) return true
        val a = this.annotations
        val b = other.annotations
        val aSize = a?.size ?: 0
        val bSize = b?.size ?: 0
        var i = 0
        var j = 0
        while (true) {
            while (i < aSize && a!![i].item !is DrawAnnotation) i++
            while (j < bSize && b!![j].item !is DrawAnnotation) j++
            if (i >= aSize || j >= bSize) {
                return i >= aSize && j >= bSize
            }
            if (a!![i] != b!![j]) return false
            i++
            j++
        }
    }

    /**
     * Computes a hash code over all annotations except draw-only [DrawAnnotation] entries, matching
     * the equality contract of [hasSameLayoutAffectingAnnotations].
     *
     * Called by [CacheTextLayoutInput.hashCode] in `TextMeasurer.kt` so that layout cache lookups
     * hash and compare [AnnotatedString] keys without invalidating on [DrawAnnotation]-only
     * changes.
     */
    internal fun hashCodeLayoutAffectingAnnotations(): Int {
        val list = annotations ?: return 0
        var hash = 1
        var hasAny = false
        list.fastForEach { range ->
            if (range.item !is DrawAnnotation) {
                hash = 31 * hash + range.hashCode()
                hasAny = true
            }
        }
        return if (hasAny) hash else 0
    }

    /**
     * Returns a new [AnnotatedString] where a list of annotations contains the results of applying
     * the given [transform] function to each element in the original annotations list.
     *
     * @sample androidx.compose.ui.text.samples.AnnotatedStringMapAnnotationsSamples
     */
    public fun mapAnnotations(
        transform: (Range<out Annotation>) -> Range<out Annotation>
    ): AnnotatedString {
        if (!PackedTextStylesLatch.isEnabled) {
            val builder = Builder(this)
            builder.mapAnnotations(transform)
            return builder.toAnnotatedString()
        }
        val current = annotations ?: return this
        var mapped: ArrayList<Range<out Annotation>>? = null
        for (i in 0 until current.size) {
            val item = current[i]
            val next = transform(item)
            if (mapped != null) {
                mapped.add(next)
            } else if (next !== item) {
                mapped = ArrayList(current.size)
                for (j in 0 until i) {
                    mapped.add(current[j])
                }
                mapped.add(next)
            }
        }
        return if (mapped != null) {
            AnnotatedString(text = text, annotations = mapped, trusted = true)
        } else {
            this
        }
    }

    /**
     * Returns a new [AnnotatedString] where a list of annotations contains all elements yielded
     * from results [transform] function being invoked on each element of original annotations list.
     *
     * @see mapAnnotations
     */
    public fun flatMapAnnotations(
        transform: (Range<out Annotation>) -> List<Range<out Annotation>>
    ): AnnotatedString {
        if (!PackedTextStylesLatch.isEnabled) {
            val builder = Builder(this)
            builder.flatMapAnnotations(transform)
            return builder.toAnnotatedString()
        }
        val current = annotations ?: return this
        val mapped = ArrayList<Range<out Annotation>>(current.size)
        current.fastForEach { mapped.addAll(transform(it)) }
        return AnnotatedString(
            text = text,
            annotations = if (mapped.isEmpty()) null else mapped,
            trusted = true,
        )
    }

    /**
     * The information attached on the text such as a [SpanStyle].
     *
     * @param item The object attached to [AnnotatedString]s.
     * @param start The start of the range where [item] takes effect. It's inclusive
     * @param end The end of the range where [item] takes effect. It's exclusive
     * @param tag The tag used to distinguish the different ranges. It is useful to store custom
     *   data. And [Range]s with same tag can be queried with functions such as
     *   [getStringAnnotations].
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class Range<T>(
        public val item: T,
        public val start: Int,
        public val end: Int,
        public val tag: String,
    ) {
        public constructor(item: T, start: Int, end: Int) : this(item, start, end, "")

        init {
            requirePrecondition(start <= end) { "Reversed range is not supported" }
        }
    }

    /**
     * Builds an [AnnotatedString] incrementally.
     *
     * Implements [Appendable] for compatibility with standard text APIs.
     *
     * @param capacity initial capacity for the internal buffer
     * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderSample
     * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderAppendableSample
     */
    public class Builder public constructor(capacity: Int = 16) : Appendable {

        private data class MutableRange<T>(
            val item: T,
            val start: Int,
            var end: Int = Int.MIN_VALUE,
            val tag: String = "",
        ) {
            /**
             * Create an immutable [Range] object.
             *
             * @param defaultEnd if the end is not set yet, it will be set to this value.
             */
            fun toRange(defaultEnd: Int = Int.MIN_VALUE): Range<T> {
                val end = if (end == Int.MIN_VALUE) defaultEnd else end
                checkPrecondition(end != Int.MIN_VALUE) { "Item.end should be set first" }
                return Range(item = item, start = start, end = end, tag = tag)
            }

            companion object {
                fun <T> fromRange(range: Range<T>) =
                    MutableRange(range.item, range.start, range.end, range.tag)
            }
        }

        private val text: StringBuilder = StringBuilder(capacity)

        private val legacyStyleStack: MutableList<MutableRange<out Any>>? =
            if (PackedTextStylesLatch.isEnabled) null else mutableListOf()
        /**
         * Holds all objects of type [AnnotatedString.Annotation] including [SpanStyle]s and
         * [ParagraphStyle]s in order.
         */
        private val legacyAnnotations: MutableList<MutableRange<out Annotation>>? =
            if (PackedTextStylesLatch.isEnabled) null else mutableListOf()

        /**
         * Stack of slots in [rangeBounds] and [rangeItems] for open ranges from [pushStyle] or
         * [pushAnnotation] that [pop] has not closed yet.
         */
        private val styleStack = if (PackedTextStylesLatch.isEnabled) mutableIntListOf() else null

        /**
         * Flat `Long` list of packed `(start, end)` text offsets for each item in [rangeItems].
         * This skips making a `MutableRange` object for each style added or pushed:
         * ```
         *  +----------------------------------+----------------------------------+
         *  |            bits 63..32           |            bits 31..0            |
         *  |       start: Int (inclusive)     |        end: Int (exclusive)      |
         *  +----------------------------------+----------------------------------+
         * ```
         *
         * While a pushed range stays open on [styleStack], its low 32 bits hold [UNSET_RANGE_END]
         * (`Int.MIN_VALUE`). Calling [pop] sets the low 32 bits in place to the current [text]
         * length.
         */
        private val rangeBounds = if (PackedTextStylesLatch.isEnabled) mutableLongListOf() else null

        /**
         * Holds all objects of type [AnnotatedString.Annotation] including [SpanStyle]s and
         * [ParagraphStyle]s in order, matching [rangeBounds] 1-to-1.
         */
        private val rangeItems =
            if (PackedTextStylesLatch.isEnabled) ArrayList<Annotation>() else null

        /**
         * Lazy list of string tags that match [rangeBounds] 1-to-1.
         *
         * [SpanStyle], [ParagraphStyle], [Bullet], [TtsAnnotation], and [LinkAnnotation] ranges
         * always use an empty `""` tag. This list stays `null` until [recordRangeTag] sees a tag
         * that is not empty (such as from [addStringAnnotation]).
         */
        private var rangeTags: ArrayList<String>? = null

        private fun packRange(start: Int, end: Int): Long =
            (start.toLong() shl RANGE_START_SHIFT) or (end.toLong() and RANGE_END_MASK)

        private fun toRange(index: Int, defaultEnd: Int): Range<out Annotation> {
            val packed = rangeBounds!![index]
            val start = (packed shr RANGE_START_SHIFT).toInt()
            val rawEnd = packed.toInt()
            val end = if (rawEnd == UNSET_RANGE_END) defaultEnd else rawEnd
            checkPrecondition(end != UNSET_RANGE_END) { "Item.end should be set first" }
            return Range(
                item = rangeItems!![index],
                start = start,
                end = end,
                tag = rangeTags?.get(index) ?: "",
            )
        }

        /** Create an [Builder] instance using the given [String]. */
        public constructor(text: String) : this(text.length) {
            append(text)
        }

        /** Create an [Builder] instance using the given [AnnotatedString]. */
        public constructor(text: AnnotatedString) : this(text.text.length) {
            append(text)
        }

        /** Returns the length of the [String]. */
        public val length: Int
            get() = text.length

        /**
         * Appends the given [String] to this [Builder].
         *
         * @param text the text to append
         */
        public fun append(text: String) {
            this.text.append(text)
        }

        @Deprecated(
            message =
                "Replaced by the append(Char) method that returns an Appendable. " +
                    "This method must be kept around for binary compatibility.",
            level = DeprecationLevel.HIDDEN,
        )
        @Suppress("FunctionName", "unused")
        // Set the JvmName to preserve compatibility with bytecode that expects a void return type.
        @JvmName("append")
        public fun deprecated_append_returning_void(char: Char) {
            append(char)
        }

        /**
         * Appends [text] to this [Builder] if non-null, and returns this [Builder].
         *
         * If [text] is an [AnnotatedString], all spans and annotations will be copied over as well.
         * No other subtypes of [CharSequence] will be treated specially. For example, any
         * platform-specific types, such as `SpannedString` on Android, will only have their text
         * copied and any other information held in the sequence, such as Android `Span`s, will be
         * dropped.
         *
         * @param text the text to append
         */
        @Suppress("BuilderSetStyle", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        public override fun append(text: CharSequence?): Builder {
            if (text is AnnotatedString) {
                append(text)
            } else {
                this.text.append(text)
            }
            return this
        }

        /**
         * Appends the range of [text] between [start] (inclusive) and [end] (exclusive) to this
         * [Builder] if non-null, and returns this [Builder].
         *
         * If [text] is an [AnnotatedString], all spans and annotations from [text] between [start]
         * and [end] will be copied over as well. No other subtypes of [CharSequence] will be
         * treated specially. For example, any platform-specific types, such as `SpannedString` on
         * Android, will only have their text copied and any other information held in the sequence,
         * such as Android `Span`s, will be dropped.
         *
         * @param text the text to append
         * @param start The index of the first character in [text] to copy over (inclusive).
         * @param end The index after the last character in [text] to copy over (exclusive).
         */
        @Suppress("BuilderSetStyle", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        public override fun append(text: CharSequence?, start: Int, end: Int): Builder {
            if (text is AnnotatedString) {
                append(text, start, end)
            } else {
                this.text.append(text, start, end)
            }
            return this
        }

        // Kdoc comes from interface method.
        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        public override fun append(char: Char): Builder {
            this.text.append(char)
            return this
        }

        /**
         * Appends the given [AnnotatedString] to this [Builder].
         *
         * @param text the text to append
         */
        public fun append(text: AnnotatedString) {
            val start = this.text.length
            this.text.append(text.text)
            // offset every annotation with start and add to the builder
            text.annotations?.fastForEach {
                addAnnotation(it.item, start + it.start, start + it.end, it.tag)
            }
        }

        /**
         * Appends the range of [text] between [start] (inclusive) and [end] (exclusive) to this
         * [Builder]. All spans and annotations from [text] between [start] and [end] will be copied
         * over as well.
         *
         * @param text the text to append
         * @param start The index of the first character in [text] to copy over (inclusive).
         * @param end The index after the last character in [text] to copy over (exclusive).
         */
        @Suppress("BuilderSetStyle")
        public fun append(text: AnnotatedString, start: Int, end: Int) {
            val insertionStart = this.text.length
            this.text.append(text.text, start, end)
            // offset every annotation with insertionStart and add to the builder
            if (!PackedTextStylesLatch.isEnabled) {
                text.getLocalAnnotations(start, end)?.fastForEach {
                    legacyAnnotations!!.add(
                        MutableRange(
                            it.item,
                            insertionStart + it.start,
                            insertionStart + it.end,
                            it.tag,
                        )
                    )
                }
                return
            }
            if (start != end) {
                val fullRange = start == 0 && end >= text.text.length
                filterRanges(text.annotations, start, end, false, fullRange)?.fastForEach {
                    addAnnotation(
                        it.item,
                        insertionStart + it.start,
                        insertionStart + it.end,
                        it.tag,
                    )
                }
            }
        }

        /**
         * Saves [tag] for the item at [index]. On the first tag that is not empty, this makes
         * [rangeTags] and fills prior slots with `""`.
         */
        private fun recordRangeTag(index: Int, tag: String) {
            val tags = rangeTags
            if (tags != null) {
                tags.add(tag)
            } else if (tag.isNotEmpty()) {
                val newTags = ArrayList<String>(index * 2 + 4)
                for (i in 0 until index) newTags.add("")
                newTags.add(tag)
                rangeTags = newTags
            }
        }

        /**
         * Adds a closed range on `[start, end)` to [rangeBounds], [rangeItems], and [rangeTags].
         */
        private fun addAnnotation(item: Annotation, start: Int, end: Int, tag: String) {
            val legacy = legacyAnnotations
            if (legacy != null) {
                legacy.add(MutableRange(item = item, start = start, end = end, tag = tag))
                return
            }
            val items = rangeItems!!
            val idx = items.size
            rangeBounds!!.add(packRange(start, end))
            items.add(item)
            recordRangeTag(idx, tag)
        }

        /**
         * Adds an open range starting at the current [text] length with [UNSET_RANGE_END] as its
         * temp end. Pushes its index onto [styleStack] and returns the stack slot for [pop].
         */
        private fun pushAnnotation(item: Annotation, tag: String): Int {
            val legacy = legacyAnnotations
            val legacyStack = legacyStyleStack
            if (legacy != null && legacyStack != null) {
                MutableRange(item = item, start = text.length, tag = tag).also {
                    legacyStack.add(it)
                    legacy.add(it)
                }
                return legacyStack.size - 1
            }
            val stack = styleStack!!
            val idx = rangeItems!!.size
            addAnnotation(item, text.length, UNSET_RANGE_END, tag)
            stack.add(idx)
            return stack.size - 1
        }

        /**
         * Applies [style] to the given range.
         *
         * @param style [SpanStyle] to apply
         * @param start inclusive start offset
         * @param end exclusive end offset
         */
        public fun addStyle(style: SpanStyle, start: Int, end: Int) {
            addAnnotation(style, start, end, "")
        }

        /**
         * Applies [style] to the given range, creating a separate paragraph.
         *
         * Paragraph ranges must follow paragraph arrangement rules. See [AnnotatedString] class
         * documentation for details and examples.
         *
         * @param style [ParagraphStyle] to apply
         * @param start inclusive start offset
         * @param end exclusive end offset
         */
        public fun addStyle(style: ParagraphStyle, start: Int, end: Int) {
            addAnnotation(style, start, end, "")
        }

        /**
         * Associates a string annotation with a range.
         *
         * @param tag tag to identify the annotation
         * @param annotation string annotation value
         * @param start inclusive start offset
         * @param end exclusive end offset
         * @sample androidx.compose.ui.text.samples.AnnotatedStringAddStringAnnotationSample
         */
        public fun addStringAnnotation(tag: String, annotation: String, start: Int, end: Int) {
            addAnnotation(StringAnnotation(annotation), start, end, tag)
        }

        /**
         * Set a [TtsAnnotation] for the given range defined by [start] and [end].
         *
         * @param ttsAnnotation an object that stores text to speech metadata that intended for the
         *   TTS engine.
         * @param start the inclusive starting offset of the range
         * @param end the exclusive end offset of the range
         * @sample androidx.compose.ui.text.samples.AnnotatedStringAddStringAnnotationSample
         * @see getStringAnnotations
         */
        @Suppress("SetterReturnsThis")
        public fun addTtsAnnotation(ttsAnnotation: TtsAnnotation, start: Int, end: Int) {
            addAnnotation(ttsAnnotation, start, end, "")
        }

        /**
         * Set a [UrlAnnotation] for the given range defined by [start] and [end]. URLs may be
         * treated specially by screen readers, including being identified while reading text with
         * an audio icon or being summarized in a links menu.
         *
         * @param urlAnnotation A [UrlAnnotation] object that stores the URL being linked to.
         * @param start the inclusive starting offset of the range
         * @param end the exclusive end offset of the range
         * @sample androidx.compose.ui.text.samples.AnnotatedStringAddStringAnnotationSample
         * @see getStringAnnotations
         */
        @ExperimentalTextApi
        @Suppress("SetterReturnsThis", "Deprecation")
        @Deprecated(
            "Use LinkAnnotation API for links instead",
            ReplaceWith("addLink(, start, end)"),
        )
        public fun addUrlAnnotation(urlAnnotation: UrlAnnotation, start: Int, end: Int) {
            addAnnotation(urlAnnotation, start, end, "")
        }

        /**
         * Associates a URL link with a range.
         *
         * Clicking the text opens the URL using [androidx.compose.ui.platform.UriHandler].
         *
         * Screen readers present URLs in different ways, such as using a links menu or audio cues.
         *
         * @param url the target URL
         * @param start inclusive start offset
         * @param end exclusive end offset
         */
        @Suppress("SetterReturnsThis")
        public fun addLink(url: LinkAnnotation.Url, start: Int, end: Int) {
            addAnnotation(url, start, end, "")
        }

        /**
         * Associates a clickable link with a range.
         *
         * Clicking the text triggers a [LinkInteractionListener] with [clickable].
         *
         * Screen readers present clickables in different ways, such as using a links menu or audio
         * cues.
         *
         * @param clickable click metadata
         * @param start inclusive start offset
         * @param end exclusive end offset
         */
        @Suppress("SetterReturnsThis")
        public fun addLink(clickable: LinkAnnotation.Clickable, start: Int, end: Int) {
            addAnnotation(clickable, start, end, "")
        }

        /**
         * Adds an annotation to draw a bullet. Unlike another overload, this one doesn't add a
         * separate [ParagraphStyle]. As so for bullet to be rendered, make sure it starts on a
         * separate line by adding a newline before or wrapping with a [ParagraphStyle].
         *
         * For a convenient API to create a bullet list check [withBulletList].
         *
         * @param bullet a bullet to draw before the text
         * @param start the inclusive starting offset of the range
         * @param end the exclusive end offset of the range
         * @see withBulletList
         */
        public fun addBullet(bullet: Bullet, start: Int, end: Int) {
            addAnnotation(bullet, start, end, "")
        }

        /**
         * Adds an annotation to draw a [bullet] together with a paragraph that adds an
         * [indentation].
         *
         * @param bullet a bullet to draw before the text
         * @param indentation indentation that is added to the paragraph. Note that this indentation
         *   should be large enough to fit a bullet and a padding between the bullet and beginning
         *   of the paragraph
         * @param start the inclusive starting offset of the range
         * @param end the exclusive end offset of the range
         * @see withBulletList
         */
        public fun addBullet(bullet: Bullet, indentation: TextUnit, start: Int, end: Int) {
            addAnnotation(
                ParagraphStyle(textIndent = TextIndent(indentation, indentation)),
                start,
                end,
                "",
            )
            addAnnotation(bullet, start, end, "")
        }

        /**
         * Associates a [DrawAnnotation] with the character range from [start] to [end].
         *
         * @param drawAnnotation annotation to draw behind or over the text range
         * @param start inclusive starting offset of the range
         * @param end exclusive end offset of the range
         * @see withDrawAnnotation
         */
        @Suppress("SetterReturnsThis")
        public fun addDrawAnnotation(
            drawAnnotation: DrawAnnotation,
            start: Int,
            end: Int,
        ) {
            addAnnotation(drawAnnotation, start, end, "")
        }

        /**
         * Applies the given [SpanStyle] to any appended text until a corresponding [pop] is called.
         *
         * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderPushSample
         * @param style SpanStyle to be applied
         */
        public fun pushStyle(style: SpanStyle): Int = pushAnnotation(style, "")

        /**
         * Applies the given [ParagraphStyle] to any appended text until a corresponding [pop] is
         * called.
         *
         * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderPushParagraphStyleSample
         * @param style ParagraphStyle to be applied
         */
        public fun pushStyle(style: ParagraphStyle): Int = pushAnnotation(style, "")

        /**
         * Applies the given [bullet] annotation to any appended text until a corresponding [pop] is
         * called. For bullet to be rendered, make sure it starts on a separate line by either
         * adding a newline before or by wrapping with a [ParagraphStyle].
         *
         * For a convenient API to create a bullet list check [withBulletList].
         *
         * @see withBulletList
         */
        public fun pushBullet(bullet: Bullet): Int = pushAnnotation(bullet, "")

        /** Scope for a bullet list */
        public class BulletScope internal constructor(internal val builder: Builder) {
            internal val bullets = ArrayList<Bullet>()
            internal val paragraphStyles = ArrayList<ParagraphStyle>()
            internal val bulletListSettingStack = mutableListOf<Pair<TextUnit, Bullet>>()
        }

        private var _bulletScope: BulletScope? =
            if (PackedTextStylesLatch.isEnabled) null else BulletScope(this)
        private val bulletScope: BulletScope
            get() = _bulletScope ?: BulletScope(this).also { _bulletScope = it }

        /**
         * Creates a bullet list which allows to define a common [indentation] and a [bullet] for
         * evey bullet list item created inside the list.
         *
         * Note that when nesting the [withBulletList] calls, the indentation inside the nested list
         * will be a combination of all indentations in the nested chain. For example,
         * ```kotlin
         * withBulletList(10.sp) {
         *   withBulletList(15.sp) {
         *     // items indentation 25.sp
         *   }
         * }
         * ```
         */
        public fun <R : Any> withBulletList(
            indentation: TextUnit = Bullet.DefaultIndentation,
            bullet: Bullet = Bullet.Default,
            block: BulletScope.() -> R,
        ): R {
            val scope = bulletScope
            if (!PackedTextStylesLatch.isEnabled) {
                val adjustedIndentation =
                    scope.bulletListSettingStack.lastOrNull()?.first?.let {
                        checkPrecondition(it.type == indentation.type) {
                            "Indentation unit types of nested bullet lists must match. Current $it and previous is $indentation"
                        }
                        when (indentation.type) {
                            Sp -> (indentation.value + it.value).sp
                            Em -> (indentation.value + it.value).em
                            else -> indentation
                        }
                    } ?: indentation

                val parIndex =
                    pushStyle(
                        ParagraphStyle(
                            textIndent = TextIndent(adjustedIndentation, adjustedIndentation)
                        )
                    )
                scope.bulletListSettingStack.add(Pair(adjustedIndentation, bullet))
                return try {
                    block(scope)
                } finally {
                    if (scope.bulletListSettingStack.isNotEmpty()) {
                        scope.bulletListSettingStack.removeAt(
                            scope.bulletListSettingStack.lastIndex
                        )
                    }
                    pop(parIndex)
                }
            }
            val adjustedIndentation =
                if (scope.paragraphStyles.isEmpty()) {
                    indentation
                } else {
                    val prevIndent =
                        scope.paragraphStyles[scope.paragraphStyles.size - 1].textIndent
                    val previousIndentation =
                        if (prevIndent != null) prevIndent.firstLine else Bullet.DefaultIndentation
                    checkPrecondition(previousIndentation.type == indentation.type) {
                        "Indentation unit types of nested bullet lists must match. Current $previousIndentation and previous is $indentation"
                    }
                    when (indentation.type) {
                        Sp -> (indentation.value + previousIndentation.value).sp
                        Em -> (indentation.value + previousIndentation.value).em
                        else -> indentation
                    }
                }

            val parStyle =
                ParagraphStyle(textIndent = TextIndent(adjustedIndentation, adjustedIndentation))
            val parIndex = pushStyle(parStyle)
            scope.bullets.add(bullet)
            scope.paragraphStyles.add(parStyle)
            return try {
                block(scope)
            } finally {
                if (scope.paragraphStyles.isNotEmpty()) {
                    val lastIdx = scope.paragraphStyles.size - 1
                    scope.bullets.removeAt(lastIdx)
                    scope.paragraphStyles.removeAt(lastIdx)
                }
                pop(parIndex)
            }
        }

        /**
         * Creates a bullet list item around the content produced by the [block]. The list item
         * creates a separate paragraph with the indentation to the bullet defined by the preceding
         * [Builder.withBulletList] calls.
         *
         * @param bullet defines the bullet to be drawn
         * @param block function to be executed
         * @sample androidx.compose.ui.text.samples.AnnotatedStringWithBulletListSample
         */
        public fun <R : Any> BulletScope.withBulletListItem(
            bullet: Bullet? = null,
            block: Builder.() -> R,
        ): R {
            if (!PackedTextStylesLatch.isEnabled) {
                val lastItemInStack = bulletListSettingStack.lastOrNull()
                val itemIndentation = lastItemInStack?.first ?: Bullet.DefaultIndentation
                val itemBullet = bullet ?: (lastItemInStack?.second ?: Bullet.Default)
                val parIndex =
                    builder.pushStyle(
                        ParagraphStyle(textIndent = TextIndent(itemIndentation, itemIndentation))
                    )
                val bulletIndex = builder.pushBullet(itemBullet)
                return try {
                    block(builder)
                } finally {
                    builder.pop(bulletIndex)
                    builder.pop(parIndex)
                }
            }
            val itemBullet =
                bullet ?: (if (bullets.isEmpty()) Bullet.Default else bullets[bullets.size - 1])
            val parStyle =
                if (paragraphStyles.isNotEmpty()) {
                    paragraphStyles[paragraphStyles.size - 1]
                } else {
                    DefaultBulletParagraphStyleHolder.DefaultBulletParagraphStyle
                }
            val parIndex = builder.pushStyle(parStyle)
            val bulletIndex = builder.pushBullet(itemBullet)
            return try {
                block(builder)
            } finally {
                builder.pop(bulletIndex)
                builder.pop(parIndex)
            }
        }

        /**
         * Attach the given [annotation] to any appended text until a corresponding [pop] is called.
         *
         * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderPushStringAnnotationSample
         * @param tag the tag used to distinguish annotations
         * @param annotation the string annotation attached on this AnnotatedString
         * @see getStringAnnotations
         * @see Range
         */
        public fun pushStringAnnotation(tag: String, annotation: String): Int =
            pushAnnotation(StringAnnotation(annotation), tag)

        /**
         * Attach the given [ttsAnnotation] to any appended text until a corresponding [pop] is
         * called.
         *
         * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderPushStringAnnotationSample
         * @param ttsAnnotation an object that stores text to speech metadata that intended for the
         *   TTS engine.
         * @see getStringAnnotations
         * @see Range
         */
        public fun pushTtsAnnotation(ttsAnnotation: TtsAnnotation): Int =
            pushAnnotation(ttsAnnotation, "")

        /**
         * Attach the given [UrlAnnotation] to any appended text until a corresponding [pop] is
         * called.
         *
         * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderPushStringAnnotationSample
         * @param urlAnnotation A [UrlAnnotation] object that stores the URL being linked to.
         * @see getStringAnnotations
         * @see Range
         */
        @ExperimentalTextApi
        @Suppress("BuilderSetStyle", "Deprecation")
        @Deprecated(
            "Use LinkAnnotation API for links instead",
            ReplaceWith("pushLink(, start, end)"),
        )
        public fun pushUrlAnnotation(urlAnnotation: UrlAnnotation): Int =
            pushAnnotation(urlAnnotation, "")

        /**
         * Attach the given [LinkAnnotation] to any appended text until a corresponding [pop] is
         * called.
         *
         * @param link A [LinkAnnotation] object that stores the URL or clickable tag being linked
         *   to.
         * @see getStringAnnotations
         * @see Range
         */
        @Suppress("BuilderSetStyle")
        public fun pushLink(link: LinkAnnotation): Int = pushAnnotation(link, "")

        /**
         * Ends the style or annotation that was added via a push operation before.
         *
         * @see pushStyle
         * @see pushStringAnnotation
         */
        public fun pop() {
            val legacyStack = legacyStyleStack
            if (legacyStack != null) {
                checkPrecondition(legacyStack.isNotEmpty()) { "Nothing to pop." }
                // pop the last element
                val item = legacyStack.removeAt(legacyStack.size - 1)
                item.end = text.length
                return
            }
            val stack = styleStack!!
            checkPrecondition(stack.isNotEmpty()) { "Nothing to pop." }
            // pop the last element
            val idx = stack.removeAt(stack.size - 1)
            val bounds = rangeBounds!!
            val packed = bounds[idx]
            bounds[idx] = (packed and RANGE_START_MASK) or (text.length.toLong() and RANGE_END_MASK)
        }

        /**
         * Ends the styles or annotation up to and `including` the [pushStyle] or
         * [pushStringAnnotation] that returned the given index.
         *
         * @param index the result of the a previous [pushStyle] or [pushStringAnnotation] in order
         *   to pop to
         * @see pop
         * @see pushStyle
         * @see pushStringAnnotation
         */
        public fun pop(index: Int) {
            val legacyStack = legacyStyleStack
            if (legacyStack != null) {
                checkPrecondition(index < legacyStack.size) {
                    "$index should be less than ${legacyStack.size}"
                }
                while ((legacyStack.size - 1) >= index) {
                    pop()
                }
                return
            }
            val stack = styleStack!!
            checkPrecondition(index < stack.size) {
                "$index should be less than ${stack.size}"
            }
            while ((stack.size - 1) >= index) {
                pop()
            }
        }

        /** Constructs an [AnnotatedString] based on the configurations applied to the [Builder]. */
        public fun toAnnotatedString(): AnnotatedString {
            val legacy = legacyAnnotations
            if (legacy != null) {
                return AnnotatedString(
                    text = text.toString(),
                    annotations = legacy.fastMap { it.toRange(text.length) },
                    trusted = true,
                )
            }
            val len = text.length
            val items = rangeItems!!
            val count = items.size
            val list =
                if (count == 0) {
                    null
                } else {
                    ArrayList<Range<out Annotation>>(count).also { ranges ->
                        for (i in 0 until count) {
                            ranges.add(toRange(i, len))
                        }
                    }
                }
            return AnnotatedString(
                text = text.toString(),
                annotations = list,
                trusted = true,
            )
        }

        /** @see AnnotatedString.mapAnnotations */
        internal fun mapAnnotations(transform: (Range<out Annotation>) -> Range<out Annotation>) {
            val legacy = legacyAnnotations
            if (legacy != null) {
                for (i in legacy.indices) {
                    val newAnnotation = transform(legacy[i].toRange())
                    legacy[i] = MutableRange.fromRange(newAnnotation)
                }
                return
            }
            val items = rangeItems!!
            val bounds = rangeBounds!!
            for (i in 0 until items.size) {
                val r = transform(toRange(i, UNSET_RANGE_END))
                bounds[i] = packRange(r.start, r.end)
                items[i] = r.item
                val tags = rangeTags
                if (tags != null) {
                    tags[i] = r.tag
                } else if (r.tag.isNotEmpty()) {
                    val newTags = ArrayList<String>(items.size)
                    for (j in 0 until items.size) newTags.add(if (j == i) r.tag else "")
                    rangeTags = newTags
                }
            }
        }

        /** @see AnnotatedString.flatMapAnnotations */
        internal fun flatMapAnnotations(
            transform: (Range<out Annotation>) -> List<Range<out Annotation>>
        ) {
            val legacy = legacyAnnotations
            if (legacy != null) {
                val replacedAnnotations = legacy.fastFlatMap { annotation ->
                    transform(annotation.toRange()).fastMap { MutableRange.fromRange(it) }
                }
                legacy.clear()
                legacy.addAll(replacedAnnotations)
                return
            }
            val items = rangeItems!!
            val bounds = rangeBounds!!
            val count = items.size
            if (count == 0) return
            val newRanges = ArrayList<Range<out Annotation>>(count)
            for (i in 0 until count) {
                newRanges.addAll(transform(toRange(i, UNSET_RANGE_END)))
            }
            bounds.clear()
            items.clear()
            rangeTags = null
            newRanges.fastForEach { r ->
                addAnnotation(r.item, r.start, r.end, r.tag)
            }
        }
    }

    /**
     * Defines annotations that specify additional information to apply to ranges of text within the
     * given AnnotatedString.
     *
     * The AnnotatedString supports annotations that provide different kind of information, such as
     * * [SpanStyle] specifies character level styling such as color, font, letter spacing etc.
     * * [ParagraphStyle] for configuring styling on a paragraph level such as line heights, text
     *   aligning, text direction etc.
     * * [LinkAnnotation] to mark links in the text.
     * * [TtsAnnotation] provides information to assistive technologies such as screen readers.
     * * [DrawAnnotation] attaches custom draw-phase rendering callbacks to a range of text.
     * * Custom annotations using the [StringAnnotation].
     */
    public sealed interface Annotation {
        public companion object {
            /**
             * Saves and restores [AnnotatedString.Annotation] objects.
             *
             * Supports the following annotation types:
             * - [ParagraphStyle]
             * - [SpanStyle]
             * - [VerbatimTtsAnnotation]
             * - [UrlAnnotation]
             * - [LinkAnnotation.Url]
             * - [LinkAnnotation.Clickable]
             * - [StringAnnotation]
             * - [DrawAnnotation] subclasses that satisfy
             *   [androidx.compose.runtime.saveable.SaverScope.canBeSaved] (such as `Parcelable`
             *   `data class`es on Android)
             *
             * Note: Does not preserve [LinkInteractionListener] of [LinkAnnotation]s,
             * [BackgroundAnnotation]s, lambda-backed [DrawAnnotation] instances, or [Bullet]
             * annotations. Handle saving and restoring them manually if required.
             *
             * @sample androidx.compose.ui.text.samples.AnnotatedStringAnnotationSaverSample
             * @sample androidx.compose.ui.text.samples.LinkAnnotationSaverWithListenerSample
             */
            public val Saver: Saver<Annotation, Any>
                get() = AnnotationSaver
        }
    }

    // Unused private subclass of the marker interface to avoid exhaustive "when" statement
    @Suppress("unused") private class ExhaustiveAnnotation : Annotation

    public companion object {
        /**
         * The default [Saver] implementation for [AnnotatedString].
         *
         * Note this Saver doesn't preserve the [LinkInteractionListener] of the links,
         * [BackgroundAnnotation]s, lambda-backed [DrawAnnotation] instances, or [Bullet]
         * annotations. You should handle these cases manually if required (check
         * https://issuetracker.google.com/issues/332901550 for an example).
         */
        // Use a custom getter with no backing field so `AnnotatedString.<clinit>` does not load
        // `Savers.kt` early. This lets R8 tree-shake unused `Saver`s.
        public val Saver: Saver<AnnotatedString, *>
            get() = AnnotatedStringSaver
    }
}

/**
 * Packs `start` and `end` into one `Long` in [AnnotatedString.Builder.rangeBounds] to skip making a
 * `MutableRange` per style. Kept at file scope so `const val` inlines without adding a public
 * `Companion` on [AnnotatedString.Builder].
 *
 * [AnnotatedString.Builder.packRange] shifts `start` into the top 32 bits, and
 * [AnnotatedString.Builder.toRange] shifts it back out.
 */
private const val RANGE_START_SHIFT = 32

/**
 * Stops `end.toLong()` from filling the top 32 bits with sign bits and wiping out `start` when
 * `end` is [UNSET_RANGE_END].
 *
 * [AnnotatedString.Builder.packRange] uses it when adding ranges to
 * [AnnotatedString.Builder.rangeBounds], and [AnnotatedString.Builder.pop] uses it when setting
 * `end`.
 */
private const val RANGE_END_MASK = 0xFFFF_FFFFL

/**
 * Lets [AnnotatedString.Builder.pop] close an open range in place inside
 * [AnnotatedString.Builder.rangeBounds] without reading `start` back out. Written as
 * `-0x100000000L` because Kotlin `Long` literals are signed (`0xFFFF_FFFF_0000_0000` bits).
 *
 * [AnnotatedString.Builder.pop] uses it to keep the top 32 `start` bits while writing `text.length`
 * into `end`.
 */
private const val RANGE_START_MASK = -0x100000000L

/**
 * Marks a pushed range whose `end` offset is not known yet inside
 * [AnnotatedString.Builder.rangeBounds]. Text offsets are always `>= 0`, so this skips a second
 * tracking list.
 *
 * [AnnotatedString.Builder.pushAnnotation] writes it for [AnnotatedString.Builder.styleStack],
 * [AnnotatedString.Builder.pop] clears it, and [AnnotatedString.Builder.toRange] checks it in
 * [AnnotatedString.Builder.toAnnotatedString], [AnnotatedString.Builder.mapAnnotations], and
 * [AnnotatedString.Builder.flatMapAnnotations].
 */
private const val UNSET_RANGE_END = Int.MIN_VALUE

private fun List<Range<out Annotation>>?.nullIfEmpty(): List<Range<out Annotation>>? {
    if (this == null || isEmpty()) return null
    return this
}

private fun sortParagraphStyles(list: List<Range<ParagraphStyle>>?): List<Range<ParagraphStyle>>? {
    if (list == null || list.size <= 1) return list
    var isSorted = true
    for (i in 1 until list.size) {
        if (list[i - 1].start > list[i].start) {
            isSorted = false
            break
        }
    }
    if (isSorted) return list
    val sorted = ArrayList(list)
    sorted.sortBy { it.start }
    return sorted
}

/**
 * Combines [spanStyles] and [paragraphStyles] into a single list of annotations.
 *
 * When only one style list is provided, reuses that list directly to avoid allocating a new
 * [ArrayList]. When both lists are provided, merges them into a single [ArrayList].
 */
private fun constructAnnotationsFromSpansAndParagraphs(
    spanStyles: List<Range<SpanStyle>>,
    paragraphStyles: List<Range<ParagraphStyle>>,
): List<Range<out Annotation>>? {
    if (paragraphStyles.isEmpty()) {
        // Zero-allocation reuse: if paragraphStyles is empty, return spanStyles directly.
        return if (spanStyles.isEmpty()) null else spanStyles
    }
    if (spanStyles.isEmpty()) {
        // Zero-allocation reuse: if spanStyles is empty, return paragraphStyles directly.
        return paragraphStyles
    }
    // Both lists are non-empty; allocate and combine both into one list.
    val array = ArrayList<Range<out Annotation>>(spanStyles.size + paragraphStyles.size)
    spanStyles.fastForEach { array.add(it) }
    paragraphStyles.fastForEach { array.add(it) }
    return array
}

/**
 * A helper function used to determine the paragraph boundaries in [MultiParagraph].
 *
 * It reads paragraph information from [AnnotatedString.paragraphStyles] where only some parts of
 * text has [ParagraphStyle] specified, and unspecified parts(gaps between specified paragraphs) are
 * considered as default paragraph with default [ParagraphStyle]. For example, the following string
 * "(Hello World)Hi!" with a specified paragraph denoted by () will result in paragraphs "Hello
 * World" and "Hi!".
 *
 * **Algorithm implementation**
 * * Keep a stack of paragraphs that to be *fully* processed yet and a pointer to the end of last
 *   paragraph already added to the result.
 * * Iterate through each paragraph.
 * * Check if there's a gap between last added paragraph and start of current paragraph. If yes, we
 *   need to add text covered by it to the result, making sure to check the existing state of the
 *   stack to merge the styles correctly.
 * * Add a paragraph to the stack. Depending on its range, we might need to merge its style with the
 *   latest one in the stack.
 * * Along the way handle special cases like fully overlapped or zero-length paragraphs.
 * * After the last iteration, clear the stack by adding additional paragraphs to the result. Also
 *   move the pointer to the end of the text.
 *
 * @param defaultParagraphStyle The default [ParagraphStyle]. It's used for both unspecified default
 *   paragraphs and specified paragraph. When a specified paragraph's [ParagraphStyle] has a null
 *   attribute, the default one will be used instead.
 */
internal fun AnnotatedString.normalizedParagraphStyles(
    defaultParagraphStyle: ParagraphStyle
): List<Range<ParagraphStyle>> {
    if (PackedTextStylesLatch.isEnabled) {
        // 1. Fast path for zero paragraph styles: the whole string is a single default paragraph.
        val paragraphRanges = paragraphStylesOrNull
        if (paragraphRanges.isNullOrEmpty()) {
            return listOf(Range(defaultParagraphStyle, 0, text.length))
        }

        // 2. Fast path for a single non-empty paragraph style within `[0, text.length]`: no nesting
        // or overlap is possible, so emit up to 3 contiguous ranges (`[0, start)`, `[start, end)`,
        // and `[end, textLength)`) without allocating the `ArrayDeque` nesting stack.
        val sortedParagraphs = sortParagraphStyles(paragraphRanges) ?: emptyList()
        if (sortedParagraphs.size == 1) {
            val singleRange = sortedParagraphs[0]
            val start = singleRange.start
            val end = singleRange.end
            val textLength = text.length
            if (start < end && end <= textLength) {
                val mergedStyle = defaultParagraphStyle.merge(singleRange.item)
                if (start == 0 && end == textLength) {
                    return listOf(Range(mergedStyle, 0, textLength))
                }
                val result = ArrayList<Range<ParagraphStyle>>(3)
                if (start > 0) result.add(Range(defaultParagraphStyle, 0, start))
                result.add(Range(mergedStyle, start, end))
                if (end < textLength) result.add(Range(defaultParagraphStyle, end, textLength))
                return result
            }
        }
    }

    // 3. Sweep-line stack merge for multiple or nested paragraph styles:
    //    - `lastAdded` tracks the end offset of the last emitted paragraph range.
    //    - `stack` holds open enclosing paragraphs whose `end` has not yet been reached, with each
    //      nested entry pre-merged with its parent on the stack.
    val sortedParagraphs =
        if (PackedTextStylesLatch.isEnabled) {
            sortParagraphStyles(paragraphStylesOrNull) ?: emptyList()
        } else {
            @Suppress("ListIterator")
            paragraphStylesOrNull?.sortedBy { it.start } ?: emptyList()
        }
    val result = ArrayList<Range<ParagraphStyle>>()

    // a pointer to the last character added to the result list, takes values from 0 to text.length
    var lastAdded = 0
    val stack = ArrayDeque<Range<ParagraphStyle>>()

    sortedParagraphs.fastForEach {
        val currentItem = defaultParagraphStyle.merge(it.item)
        val currentStart = it.start
        val currentEnd = it.end
        // 3a. Advance `lastAdded` up to `currentStart` by emitting segments from enclosing
        //     paragraphs on `stack` that precede `currentStart`.
        while (lastAdded < currentStart && stack.isNotEmpty()) {
            val lastInStack = stack.last()
            // ..withStyle(A) { <-- last in stack....
            // ....append............................
            // ....withStyle(B) { <-- current........
            // ......append..........................
            // ....}.................................
            // ..}...................................
            if (currentStart < lastInStack.end) {
                result.add(Range(lastInStack.item, lastAdded, currentStart))
                lastAdded = currentStart
            } else {
                // ..withStyle(A) {......................
                // ....append............................
                // ....withStyle(B) { <-- last in stack..
                // ......append..........................
                // ....}.................................
                // ..}...................................
                // withStyle(C) <-- current
                result.add(Range(lastInStack.item, lastAdded, lastInStack.end))
                lastAdded = lastInStack.end
                // We now need to remove it from the stack but also make sure that we remove other
                // stack
                // entrances that have the same ends as the lastAdded
                while (stack.isNotEmpty() && lastAdded == stack.last().end) {
                    stack.removeLast()
                }
            }
        }

        // 3b. If `stack` was emptied before reaching `currentStart`, fill the unstyled gap
        //     `[lastAdded, currentStart)` with `defaultParagraphStyle`.
        if (lastAdded < currentStart) {
            result.add(Range(defaultParagraphStyle, lastAdded, currentStart))
            lastAdded = currentStart
        }

        // 3c. Push `current` onto `stack`, merging it with the enclosing paragraph on top of
        //     `stack` (or replacing the top entry if their bounds are identical or zero-length).
        val lastInStack = stack.lastOrNull()
        if (lastInStack != null) {
            if (lastInStack.start == currentStart && lastInStack.end == currentEnd) {
                // fully overlapped, we'll merge current with the previous one and remove the
                // previous one from the stack
                stack.removeLast()
                stack.add(Range(lastInStack.item.merge(currentItem), currentStart, currentEnd))
            } else if (lastInStack.start == lastInStack.end) {
                // this is a zero-length paragraph
                result.add(Range(lastInStack.item, lastInStack.start, lastInStack.end))
                stack.removeLast()
                stack.add(Range(currentItem, currentStart, currentEnd))
            } else if (lastInStack.end < currentEnd) {
                // This is already handled in the init require checks
                throw IllegalArgumentException()
            } else {
                stack.add(Range(lastInStack.item.merge(currentItem), currentStart, currentEnd))
            }
        } else {
            stack.add(Range(currentItem, currentStart, currentEnd))
        }
    }

    // The paragraph styles finished so we need to empty the stack to add the remaining to the
    // result.
    // 4. Drain remaining open enclosing paragraphs on `stack`, then cover any trailing unstyled
    //    text `[lastAdded, text.length)` with `defaultParagraphStyle`.
    while (lastAdded <= text.length && stack.isNotEmpty()) {
        // ..withStyle(A) {......................
        // ....append............................
        // ....withStyle(B) { <-- last in stack..
        // ......append..........................
        // ....}.................................
        // ..}...................................
        // ....End of AnnotatedString builder....
        val lastInStack = stack.last()
        result.add(Range(lastInStack.item, lastAdded, lastInStack.end))
        lastAdded = lastInStack.end
        // We now need to remove it from the stack but also make sure that we remove other stack
        // entrances that have the same ends as the lastAdded
        while (stack.isNotEmpty() && lastAdded == stack.last().end) {
            stack.removeLast()
        }
    }

    // There might be a text left at the end that isn't covered with a paragraph so using a default
    if (lastAdded < text.length) {
        result.add(Range(defaultParagraphStyle, lastAdded, text.length))
    }

    // This is a corner case where annotatedString is an empty string without any ParagraphStyle.
    // In this case, an empty ParagraphStyle is created.
    if (result.isEmpty()) {
        result.add(Range(defaultParagraphStyle, 0, 0))
    }
    return result
}

/**
 * Helper function used to return another AnnotatedString that is a substring from [start] to [end].
 * This will ignore the [ParagraphStyle]s and the resulting [AnnotatedString] will have no
 * [ParagraphStyle]s.
 *
 * @param start The start index of the paragraph range, inclusive
 * @param end The end index of the paragraph range, exclusive
 * @return The list of converted [SpanStyle]s in the given paragraph range
 */
internal fun AnnotatedString.substringWithoutParagraphStyles(
    start: Int,
    end: Int,
): AnnotatedString {
    if (!PackedTextStylesLatch.isEnabled) {
        return AnnotatedString(
            text = if (start != end) text.substring(start, end) else "",
            annotations = getLocalAnnotations(start, end) { it !is ParagraphStyle } ?: listOf(),
            trusted = true,
        )
    }
    // When the paragraph covers the whole text (`start == 0 && end >= text.length`):
    // - If there are no non-style annotations (`!hasOtherAnnotations`), `spanStylesOrNull` already
    //   contains every non-ParagraphStyle annotation for `[0, text.length]` and can be reused
    // as-is.
    // - Otherwise, passing `fullRange = true` to `filterRanges` strips `ParagraphStyle` entries
    //   without re-clamping or dropping trailing point annotations at `[text.length, text.length]`.
    val fullRange = start == 0 && end >= text.length
    if (fullRange && !hasOtherAnnotations) {
        return AnnotatedString(text = text, annotations = spanStylesOrNull, trusted = true)
    }
    val sourceRanges = if (hasOtherAnnotations) annotations else spanStylesOrNull
    return AnnotatedString(
        text = if (start != end) text.substring(start, end) else "",
        annotations =
            if (start == end) {
                null
            } else {
                filterRanges(sourceRanges, start, end, hasOtherAnnotations, fullRange)
            },
        trusted = true,
    )
}

/**
 * Helper function used to find the annotations in the text range. Also transforms the range of the
 * Found [AnnotatedString.Annotation]s in the range to a local range.
 *
 * @param start The start index of the text range, inclusive.
 * @param end The end index of the text range, exclusive.
 * @param predicate Filter function on the [AnnotatedString.Annotation]s. Notice that every
 *   annotation is accepted if the predicate isnull.
 * @return The list of converted [AnnotatedString.Annotation]s stored in [Range]. Notice that All
 *   annotations that intersect with the range [start, end) will be returned. When [start] is bigger
 *   than [end], or equal to [end], null will be returned.
 */
private fun AnnotatedString.getLocalAnnotations(
    start: Int,
    end: Int,
    predicate: ((Annotation) -> Boolean)? = null,
): List<Range<out Annotation>>? {
    if (start == end) {
        return null
    }
    val annotations = annotations ?: return null
    // If the given range covers the whole AnnotatedString, return it without conversion.
    if (start == 0 && end >= this.text.length) {
        return if (predicate == null) {
            annotations
        } else {
            annotations.fastFilter { predicate(it.item) }
        }
    }
    return annotations
        .fastFilter {
            (predicate?.invoke(it.item) ?: true) && intersect(start, end, it.start, it.end)
        }
        .fastMap {
            @Suppress("UNCHECKED_CAST")
            Range(
                it.item,
                it.start.fastCoerceIn(start, end) - start,
                it.end.fastCoerceIn(start, end) - start,
                it.tag,
            )
        }
}

internal inline fun <T> AnnotatedString.mapEachParagraphStyle(
    defaultParagraphStyle: ParagraphStyle,
    crossinline block:
        (annotatedString: AnnotatedString, paragraphStyle: Range<ParagraphStyle>) -> T,
): List<T> {
    return normalizedParagraphStyles(defaultParagraphStyle).fastMap { paragraphStyleRange ->
        val annotatedString =
            substringWithoutParagraphStyles(paragraphStyleRange.start, paragraphStyleRange.end)
        block(annotatedString, paragraphStyleRange)
    }
}

/**
 * Create upper case transformed [AnnotatedString]
 *
 * The uppercase sometimes maps different number of characters. This function adjusts the text style
 * and paragraph style ranges to transformed offset.
 *
 * Note, if the style's offset is middle of the uppercase mapping context, this function won't
 * transform the character, e.g. style starts from between base alphabet character and accent
 * character.
 *
 * @param localeList A locale list used for upper case mapping. Only the first locale is effective.
 *   If empty locale list is passed, use the current locale instead.
 * @return An uppercase transformed [AnnotatedString].
 */
@Deprecated(
    "This method allows passing an empty locale list, which will pull a locale in a way " +
        "that can't be backed by snapshot state. Call toUpperCase with an explicit locale " +
        "instead. If you have a non-empty locale list, the correct thing to do is use the first " +
        "locale in the list."
)
@Suppress("DEPRECATION")
public fun AnnotatedString.toUpperCase(
    localeList: LocaleList = LocaleList.current
): AnnotatedString {
    return transform { str, start, end -> str.substring(start, end).toUpperCase(localeList) }
}

/**
 * Create upper case transformed [AnnotatedString]
 *
 * The uppercase sometimes maps different number of characters. This function adjusts the text style
 * and paragraph style ranges to transformed offset.
 *
 * Note, if the style's offset is middle of the uppercase mapping context, this function won't
 * transform the character, e.g. style starts from between base alphabet character and accent
 * character.
 *
 * @param locale The locale used for upper case mapping.
 * @return An uppercase transformed [AnnotatedString].
 */
public fun AnnotatedString.toUpperCase(locale: Locale): AnnotatedString {
    return transform { str, start, end -> str.substring(start, end).toUpperCase(locale) }
}

/**
 * Create lower case transformed [AnnotatedString]
 *
 * The lowercase sometimes maps different number of characters. This function adjusts the text style
 * and paragraph style ranges to transformed offset.
 *
 * Note, if the style's offset is middle of the lowercase mapping context, this function won't
 * transform the character, e.g. style starts from between base alphabet character and accent
 * character.
 *
 * @param localeList A locale list used for lower case mapping. Only the first locale is effective.
 *   If empty locale list is passed, use the current locale instead.
 * @return A lowercase transformed string.
 */
@Deprecated(
    "This method allows passing an empty locale list, which will pull a locale in a way " +
        "that can't be backed by snapshot state. Call toLowerCase with an explicit locale " +
        "instead. If you have a non-empty locale list, the correct thing to do is use the first " +
        "locale in the list."
)
@Suppress("DEPRECATION")
public fun AnnotatedString.toLowerCase(
    localeList: LocaleList = LocaleList.current
): AnnotatedString {
    return transform { str, start, end -> str.substring(start, end).toLowerCase(localeList) }
}

/**
 * Create lower case transformed [AnnotatedString]
 *
 * The lowercase sometimes maps different number of characters. This function adjusts the text style
 * and paragraph style ranges to transformed offset.
 *
 * Note, if the style's offset is middle of the lowercase mapping context, this function won't
 * transform the character, e.g. style starts from between base alphabet character and accent
 * character.
 *
 * @param locale The locale used for lower case mapping.
 * @return A lowercase transformed [AnnotatedString].
 */
public fun AnnotatedString.toLowerCase(locale: Locale): AnnotatedString {
    return transform { str, start, end -> str.substring(start, end).toLowerCase(locale) }
}

/**
 * Create capitalized [AnnotatedString]
 *
 * The capitalization sometimes maps different number of characters. This function adjusts the text
 * style and paragraph style ranges to transformed offset.
 *
 * Note, if the style's offset is middle of the capitalization context, this function won't
 * transform the character, e.g. style starts from between base alphabet character and accent
 * character.
 *
 * @param localeList A locale list used for capitalize mapping. Only the first locale is effective.
 *   If empty locale list is passed, use the current locale instead. Note that, this locale is
 *   currently ignored since underlying Kotlin method is experimental.
 * @return A capitalized string.
 */
@Deprecated(
    "This method allows passing an empty locale list, which will pull a locale in a way " +
        "that can't be backed by snapshot state. Call capitalize with an explicit locale " +
        "instead. If you have a non-empty locale list, the correct thing to do is use the first " +
        "locale in the list."
)
@Suppress("DEPRECATION")
public fun AnnotatedString.capitalize(
    localeList: LocaleList = LocaleList.current
): AnnotatedString {
    return transform { str, start, end ->
        if (start == 0) {
            str.substring(start, end).capitalize(localeList)
        } else {
            str.substring(start, end)
        }
    }
}

/**
 * Create capitalized [AnnotatedString]
 *
 * The capitalization sometimes maps different number of characters. This function adjusts the text
 * style and paragraph style ranges to transformed offset.
 *
 * Note, if the style's offset is middle of the capitalization context, this function won't
 * transform the character, e.g. style starts from between base alphabet character and accent
 * character.
 *
 * @param locale The locale used for capitalize mapping.
 * @return A capitalized [AnnotatedString].
 */
public fun AnnotatedString.capitalize(locale: Locale): AnnotatedString {
    return transform { str, start, end ->
        if (start == 0) {
            str.substring(start, end).capitalize(locale)
        } else {
            str.substring(start, end)
        }
    }
}

/**
 * Create decapitalized [AnnotatedString]
 *
 * The decapitalization sometimes maps different number of characters. This function adjusts the
 * text style and paragraph style ranges to transformed offset.
 *
 * Note, if the style's offset is middle of the decapitalization context, this function won't
 * transform the character, e.g. style starts from between base alphabet character and accent
 * character.
 *
 * @param localeList A locale list used for decapitalize mapping. Only the first locale is
 *   effective. If empty locale list is passed, use the current locale instead. Note that, this
 *   locale is currently ignored since underlying Kotlin method is experimental.
 * @return A decapitalized [AnnotatedString].
 */
@Deprecated(
    "This method allows passing an empty locale list, which will pull a locale in a way " +
        "that can't be backed by snapshot state. Call decapitalize with an explicit locale " +
        "instead. If you have a non-empty locale list, the correct thing to do is use the first " +
        "locale in the list."
)
@Suppress("DEPRECATION")
public fun AnnotatedString.decapitalize(
    localeList: LocaleList = LocaleList.current
): AnnotatedString {
    return transform { str, start, end ->
        if (start == 0) {
            str.substring(start, end).decapitalize(localeList)
        } else {
            str.substring(start, end)
        }
    }
}

/**
 * Create decapitalized [AnnotatedString]
 *
 * The decapitalization sometimes maps different number of characters. This function adjusts the
 * text style and paragraph style ranges to transformed offset.
 *
 * Note, if the style's offset is middle of the decapitalization context, this function won't
 * transform the character, e.g. style starts from between base alphabet character and accent
 * character.
 *
 * @param locale The locale used for decapitalize mapping.
 * @return A decapitalized [AnnotatedString].
 */
public fun AnnotatedString.decapitalize(locale: Locale): AnnotatedString {
    return transform { str, start, end ->
        if (start == 0) {
            str.substring(start, end).decapitalize(locale)
        } else {
            str.substring(start, end)
        }
    }
}

/**
 * The core function of [AnnotatedString] transformation.
 *
 * @param transform the transformation method
 * @return newly allocated transformed AnnotatedString
 */
internal expect fun AnnotatedString.transform(
    transform: (String, Int, Int) -> String
): AnnotatedString

/**
 * Pushes [style] to the [AnnotatedString.Builder], executes [block] and then pops the [style].
 *
 * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderWithStyleSample
 * @param style [SpanStyle] to be applied
 * @param block function to be executed
 * @return result of the [block]
 * @see AnnotatedString.Builder.pushStyle
 * @see AnnotatedString.Builder.pop
 */
public inline fun <R : Any> Builder.withStyle(style: SpanStyle, block: Builder.() -> R): R {
    val index = pushStyle(style)
    return try {
        block(this)
    } finally {
        pop(index)
    }
}

/**
 * Pushes [style] to the [AnnotatedString.Builder], executes [block] and then pops the [style].
 *
 * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderWithStyleSample
 * @param style [SpanStyle] to be applied
 * @param block function to be executed
 * @return result of the [block]
 * @see AnnotatedString.Builder.pushStyle
 * @see AnnotatedString.Builder.pop
 */
public inline fun <R : Any> Builder.withStyle(
    style: ParagraphStyle,
    crossinline block: Builder.() -> R,
): R {
    val index = pushStyle(style)
    return try {
        block(this)
    } finally {
        pop(index)
    }
}

/**
 * Pushes an annotation to the [AnnotatedString.Builder], executes [block] and then pops the
 * annotation.
 *
 * @param tag the tag used to distinguish annotations
 * @param annotation the string annotation attached on this AnnotatedString
 * @param block function to be executed
 * @return result of the [block]
 * @see AnnotatedString.Builder.pushStringAnnotation
 * @see AnnotatedString.Builder.pop
 */
public inline fun <R : Any> Builder.withAnnotation(
    tag: String,
    annotation: String,
    crossinline block: Builder.() -> R,
): R {
    val index = pushStringAnnotation(tag, annotation)
    return try {
        block(this)
    } finally {
        pop(index)
    }
}

/**
 * Pushes an [TtsAnnotation] to the [AnnotatedString.Builder], executes [block] and then pops the
 * annotation.
 *
 * @param ttsAnnotation an object that stores text to speech metadata that intended for the TTS
 *   engine.
 * @param block function to be executed
 * @return result of the [block]
 * @see AnnotatedString.Builder.pushStringAnnotation
 * @see AnnotatedString.Builder.pop
 */
public inline fun <R : Any> Builder.withAnnotation(
    ttsAnnotation: TtsAnnotation,
    crossinline block: Builder.() -> R,
): R {
    val index = pushTtsAnnotation(ttsAnnotation)
    return try {
        block(this)
    } finally {
        pop(index)
    }
}

/**
 * Pushes an [UrlAnnotation] to the [AnnotatedString.Builder], executes [block] and then pops the
 * annotation.
 *
 * @param urlAnnotation A [UrlAnnotation] object that stores the URL being linked to.
 * @param block function to be executed
 * @return result of the [block]
 * @see AnnotatedString.Builder.pushStringAnnotation
 * @see AnnotatedString.Builder.pop
 */
@ExperimentalTextApi
@Deprecated("Use LinkAnnotation API for links instead", ReplaceWith("withLink(, block)"))
@Suppress("Deprecation")
public inline fun <R : Any> Builder.withAnnotation(
    urlAnnotation: UrlAnnotation,
    crossinline block: Builder.() -> R,
): R {
    val index = pushUrlAnnotation(urlAnnotation)
    return try {
        block(this)
    } finally {
        pop(index)
    }
}

/**
 * Pushes a [LinkAnnotation] to the [AnnotatedString.Builder], executes [block] and then pops the
 * annotation.
 *
 * @param link A [LinkAnnotation] object representing a clickable part of the text
 * @param block function to be executed
 * @return result of the [block]
 * @sample androidx.compose.ui.text.samples.AnnotatedStringWithLinkSample
 * @sample androidx.compose.ui.text.samples.AnnotatedStringWithHoveredLinkStylingSample
 * @sample androidx.compose.ui.text.samples.AnnotatedStringWithListenerSample
 */
public inline fun <R : Any> Builder.withLink(link: LinkAnnotation, block: Builder.() -> R): R {
    val index = pushLink(link)
    return try {
        block(this)
    } finally {
        pop(index)
    }
}

/**
 * Associates [drawAnnotation] with the text appended while running [block].
 *
 * @sample androidx.compose.ui.text.samples.DrawAnnotationSample
 * @param drawAnnotation annotation that draws behind or over the appended text range
 * @param block function to run while [drawAnnotation] is active
 * @return result of [block]
 * @see AnnotatedString.Builder.addDrawAnnotation
 */
public inline fun <R : Any> Builder.withDrawAnnotation(
    drawAnnotation: DrawAnnotation,
    block: Builder.() -> R,
): R {
    val start = length
    return try {
        block(this)
    } finally {
        addDrawAnnotation(drawAnnotation, start, length)
    }
}

/**
 * Helper function that checks if the range `[start, end)` intersects with each item in [ranges],
 * clips [ranges] to `[start, end)`, and shifts kept ranges by `-start` so their offsets match the
 * new substring. When [start] is bigger than [end], an empty list (or `null`) will be returned.
 *
 * @param ranges the input range list, or `null`
 * @param start the start index of the text slice
 * @param end the end index of the text slice
 * @param excludeParagraphStyles when `true`, drops any [Range] whose item is a [ParagraphStyle]
 *   (used by [substringWithoutParagraphStyles] after block bounds are split)
 * @param fullRange when `true`, `[start, end)` spans the whole text (`start == 0 && end >=
 *   text.length`). This does two things: (1) it skips clamping and making new [Range] objects (and
 *   returns [ranges] as-is when `!excludeParagraphStyles`), and (2) [intersect] treats `end` as an
 *   open bound and would drop a zero-length point range at `[text.length, text.length]`, while a
 *   full copy must keep all ranges on the string.
 */
private fun filterRanges(
    ranges: List<Range<out Annotation>>?,
    start: Int,
    end: Int,
    excludeParagraphStyles: Boolean = false,
    fullRange: Boolean = false,
): List<Range<out Annotation>>? {
    requirePrecondition(start <= end) {
        "start ($start) should be less than or equal to end ($end)"
    }
    val nonNullRanges = ranges ?: return null
    if (!PackedTextStylesLatch.isEnabled) {
        return nonNullRanges
            .fastFilter { intersect(start, end, it.start, it.end) }
            .fastMap {
                Range(
                    item = it.item,
                    start = it.start.fastCoerceIn(start, end) - start,
                    end = it.end.fastCoerceIn(start, end) - start,
                    tag = it.tag,
                )
            }
            .ifEmpty { null }
    }
    if (fullRange && !excludeParagraphStyles) return nonNullRanges
    val len = nonNullRanges.size
    var result: ArrayList<Range<out Annotation>>? = null
    for (i in 0 until len) {
        val range = nonNullRanges[i]
        if (
            (!excludeParagraphStyles || range.item !is ParagraphStyle) &&
                (fullRange || intersect(start, end, range.start, range.end))
        ) {
            val list = result ?: ArrayList<Range<out Annotation>>(len).also { result = it }
            if (fullRange) {
                list.add(range)
            } else {
                val clippedStart = range.start.fastCoerceIn(start, end) - start
                val clippedEnd = range.end.fastCoerceIn(start, end) - start
                list.add(
                    if (clippedStart == range.start && clippedEnd == range.end) {
                        range
                    } else {
                        Range(
                            item = range.item,
                            start = clippedStart,
                            end = clippedEnd,
                            tag = range.tag,
                        )
                    }
                )
            }
        }
    }
    return result
}

/**
 * Create an AnnotatedString with a [spanStyle] that will apply to the whole text.
 *
 * @param text the text to be styled
 * @param spanStyle [SpanStyle] to be applied to whole text
 * @param paragraphStyle [ParagraphStyle] to be applied to whole text
 */
public fun AnnotatedString(
    text: String,
    spanStyle: SpanStyle,
    paragraphStyle: ParagraphStyle? = null,
): AnnotatedString =
    AnnotatedString(
        text,
        listOf(Range(spanStyle, 0, text.length)),
        if (paragraphStyle == null) emptyList() else listOf(Range(paragraphStyle, 0, text.length)),
    )

/**
 * Create an AnnotatedString with a [paragraphStyle] that will apply to the whole text.
 *
 * @param text the text to be styled
 * @param paragraphStyle [ParagraphStyle] to be applied to whole text
 */
public fun AnnotatedString(text: String, paragraphStyle: ParagraphStyle): AnnotatedString =
    AnnotatedString(text, emptyList(), listOf(Range(paragraphStyle, 0, text.length)))

/**
 * Build a new AnnotatedString by populating newly created [AnnotatedString.Builder] provided by
 * [builder].
 *
 * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderLambdaSample
 * @param builder lambda to modify [AnnotatedString.Builder]
 */
public inline fun buildAnnotatedString(builder: (Builder).() -> Unit): AnnotatedString =
    Builder().apply(builder).toAnnotatedString()

/**
 * Helper function that checks if the range [baseStart, baseEnd) contains the range [targetStart,
 * targetEnd).
 *
 * @return true if
 *   [baseStart, baseEnd) contains [targetStart, targetEnd), vice versa. When [baseStart]==[baseEnd]
 *   it return true iff [targetStart]==[targetEnd]==[baseStart].
 */
internal fun contains(baseStart: Int, baseEnd: Int, targetStart: Int, targetEnd: Int) =
    (baseStart <= targetStart && targetEnd <= baseEnd) &&
        (baseEnd != targetEnd || (targetStart == targetEnd) == (baseStart == baseEnd))

/**
 * Helper function that checks if the range [lStart, lEnd) intersects with the range [rStart, rEnd).
 *
 * @return [lStart, lEnd) intersects with range [rStart, rEnd), vice versa.
 */
internal fun intersect(lStart: Int, lEnd: Int, rStart: Int, rEnd: Int): Boolean {
    // We can check if two ranges intersect just by performing the following operation:
    //
    //     lStart < rEnd && rStart < lEnd
    //
    // This operation handles all cases, including when one of the ranges is fully included in the
    // other ranges. This is however not enough in this particular case because our ranges are open
    // at the end, but closed at the start.
    //
    // This means the test above would fail cases like: [1, 4) intersect [1, 1)
    // To address this we check if either one of the ranges is a "point" (empty selection). If
    // that's the case and both ranges share the same start point, then they intersect.
    //
    // In addition, we use bitwise operators (or, and) instead of boolean operators (||, &&) to
    // generate branchless code.
    return ((lStart == lEnd) or (rStart == rEnd) and (lStart == rStart)) or
        ((lStart < rEnd) and (rStart < lEnd))
}

/**
 * Lazily holds [DefaultBulletParagraphStyle] on a `private object` with [@JvmField] so referencing
 * [AnnotatedString] or `AnnotatedStringKt` does not eagerly allocate a [ParagraphStyle] or trigger
 * [Bullet] class initialization during cold start, and R8 can tree-shake it when bullet lists are
 * unused.
 */
private object DefaultBulletParagraphStyleHolder {
    @JvmField
    val DefaultBulletParagraphStyle: ParagraphStyle =
        ParagraphStyle(
            textIndent = TextIndent(Bullet.DefaultIndentation, Bullet.DefaultIndentation)
        )
}

private val EmptyAnnotatedString: AnnotatedString = AnnotatedString("")

/** Returns an AnnotatedString with empty text and no annotations. */
internal fun emptyAnnotatedString() = EmptyAnnotatedString
