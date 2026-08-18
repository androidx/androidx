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

package androidx.compose.remote.integration.demos.player

import androidx.collection.mutableIntIntMapOf
import androidx.collection.mutableIntListOf
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString.Annotation
import androidx.compose.ui.text.Bullet
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.decapitalize
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType.Companion.Em
import androidx.compose.ui.unit.TextUnitType.Companion.Sp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilteredMap
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

/**
 * An immutable text sequence with styling information and annotations.
 *
 * Remote counterpart of [androidx.compose.ui.text.AnnotatedString], supporting character-level
 * [SpanStyle]s, paragraph-level [ParagraphStyle]s, [LinkAnnotation]s, and [Bullet]s.
 */
@Immutable
public class RemoteAnnotatedString
internal constructor(internal val annotations: List<Range<Annotation>>?, public val text: String) :
    CharSequence {

    internal val spanStylesOrNull: List<Range<SpanStyle>>?
    /** All [SpanStyle] that have been applied to a range of this String */
    public val spanStyles: List<Range<SpanStyle>>
        get() = spanStylesOrNull ?: listOf()

    internal val paragraphStylesOrNull: List<Range<ParagraphStyle>>?
    /** All [ParagraphStyle] that have been applied to a range of this String */
    public val paragraphStyles: List<Range<ParagraphStyle>>
        get() = paragraphStylesOrNull ?: listOf()

    /** All [LinkAnnotation] that have been applied to a range of this String */
    public val linkAnnotations: List<Range<LinkAnnotation>>
        get() =
            annotations?.fastFilteredMap({ it.item is LinkAnnotation }) {
                @Suppress("UNCHECKED_CAST")
                it as Range<LinkAnnotation>
            } ?: listOf()

    /**
     * Creates a [RemoteAnnotatedString] with styles.
     *
     * @param text text to display
     * @param spanStyles styles to apply to text. Overlapping [SpanStyle]s merge.
     * @param paragraphStyles paragraph styles to apply to ranges of text.
     */
    public constructor(
        text: String,
        spanStyles: List<Range<SpanStyle>> = listOf(),
        paragraphStyles: List<Range<ParagraphStyle>> = listOf(),
    ) : this(constructAnnotationsFromSpansAndParagraphs(spanStyles, paragraphStyles), text)

    /**
     * Creates a [RemoteAnnotatedString] with annotations.
     *
     * @param text text to display
     * @param annotations annotations to apply to text.
     */
    public constructor(
        text: String,
        annotations: List<Range<Annotation>> = listOf(),
    ) : this(annotations.ifEmpty { null }, text)

    init {
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
        spanStylesOrNull = spanStyles
        paragraphStylesOrNull = paragraphStyles

        @Suppress("ListIterator") val sorted = paragraphStylesOrNull?.sortedBy { it.start }
        if (!sorted.isNullOrEmpty()) {
            val previousEnds = mutableIntListOf(sorted.first().end)
            for (i in 1 until sorted.size) {
                val current = sorted[i]
                while (previousEnds.isNotEmpty()) {
                    val previousEnd = previousEnds.last()
                    if (current.start >= previousEnd) {
                        previousEnds.removeAt(previousEnds.size - 1)
                    } else {
                        require(current.end <= previousEnd) {
                            "Paragraph overlap not allowed, end ${current.end} should be <= $previousEnd"
                        }
                        break
                    }
                }
                previousEnds.add(current.end)
            }
        }
    }

    public override val length: Int
        get() = text.length

    public override operator fun get(index: Int): Char = text[index]

    /**
     * Return a substring for the [RemoteAnnotatedString] and include the styles in the range of
     * [startIndex] (inclusive) and [endIndex] (exclusive).
     */
    public override fun subSequence(startIndex: Int, endIndex: Int): RemoteAnnotatedString {
        require(startIndex <= endIndex) {
            "start ($startIndex) should be less than or equal to end ($endIndex)"
        }
        if (startIndex == 0 && endIndex == text.length) return this
        val subText = text.substring(startIndex, endIndex)
        return RemoteAnnotatedString(
            text = subText,
            annotations = filterRanges(annotations, startIndex, endIndex) ?: listOf(),
        )
    }

    /** Return a substring for the [RemoteAnnotatedString] in the given [range]. */
    public fun subSequence(range: TextRange): RemoteAnnotatedString {
        return subSequence(range.min, range.max)
    }

    @Stable
    public operator fun plus(other: RemoteAnnotatedString): RemoteAnnotatedString {
        return with(Builder(this)) {
            append(other)
            toRemoteAnnotatedString()
        }
    }

    /** Query all [LinkAnnotation]s attached to this [RemoteAnnotatedString]. */
    public fun getLinkAnnotations(start: Int, end: Int): List<Range<LinkAnnotation>> =
        annotations?.fastFilteredMap({
            it.item is LinkAnnotation && intersect(start, end, it.start, it.end)
        }) {
            @Suppress("UNCHECKED_CAST")
            it as Range<LinkAnnotation>
        } ?: listOf()

    /** Returns true if [getLinkAnnotations] would return a non-empty list. */
    public fun hasLinkAnnotations(start: Int, end: Int): Boolean =
        annotations?.fastAny {
            it.item is LinkAnnotation && intersect(start, end, it.start, it.end)
        } ?: false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteAnnotatedString) return false
        if (text != other.text) return false
        if (annotations != other.annotations) return false
        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (annotations?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = text

    /** Compare the annotations between this and another [RemoteAnnotatedString]. */
    public fun hasEqualAnnotations(other: RemoteAnnotatedString): Boolean =
        this.annotations == other.annotations

    /** Transforms annotations with the given [transform] lambda. */
    public fun mapAnnotations(
        transform: (Range<Annotation>) -> Range<Annotation>
    ): RemoteAnnotatedString {
        val builder = Builder(this)
        builder.mapAnnotations(transform)
        return builder.toRemoteAnnotatedString()
    }

    /** Transforms annotations by flat mapping each annotation with [transform]. */
    public fun flatMapAnnotations(
        transform: (Range<Annotation>) -> List<Range<Annotation>>
    ): RemoteAnnotatedString {
        val builder = Builder(this)
        builder.flatMapAnnotations(transform)
        return builder.toRemoteAnnotatedString()
    }

    /** Information attached to the text range. */
    @Immutable
    public data class Range<out T>(
        public val item: T,
        public val start: Int,
        public val end: Int,
        public val tag: String = "",
    ) {
        init {
            require(start <= end) { "Reversed range is not supported" }
        }
    }

    /** Builds a [RemoteAnnotatedString] incrementally. */
    public class Builder(capacity: Int = 16) : Appendable {

        private data class MutableRange<T>(
            val item: T,
            val start: Int,
            var end: Int = Int.MIN_VALUE,
            val tag: String = "",
        ) {
            fun toRange(defaultEnd: Int = Int.MIN_VALUE): Range<T> {
                val resolvedEnd = if (end == Int.MIN_VALUE) defaultEnd else end
                check(resolvedEnd != Int.MIN_VALUE) { "Item.end should be set first" }
                return Range(item = item, start = start, end = resolvedEnd, tag = tag)
            }

            companion object {
                fun <T> fromRange(range: Range<T>) =
                    MutableRange(range.item, range.start, range.end, range.tag)
            }
        }

        private val text: StringBuilder = StringBuilder(capacity)
        private val styleStack: MutableList<MutableRange<out Any>> = mutableListOf()
        private val annotations = mutableListOf<MutableRange<out Annotation>>()

        /** Create a [Builder] instance using the given [String]. */
        public constructor(text: String) : this() {
            append(text)
        }

        /** Create a [Builder] instance using the given [RemoteAnnotatedString]. */
        public constructor(text: RemoteAnnotatedString) : this() {
            append(text)
        }

        /** Returns the length of the string under construction. */
        public val length: Int
            get() = text.length

        /** Appends the given [String] to this builder. */
        public fun append(text: String) {
            this.text.append(text)
        }

        public override fun append(text: CharSequence?): Builder {
            if (text is RemoteAnnotatedString) {
                append(text)
            } else {
                this.text.append(text)
            }
            return this
        }

        public override fun append(text: CharSequence?, start: Int, end: Int): Builder {
            if (text is RemoteAnnotatedString) {
                append(text, start, end)
            } else {
                this.text.append(text, start, end)
            }
            return this
        }

        public override fun append(char: Char): Builder {
            this.text.append(char)
            return this
        }

        /** Appends the given [RemoteAnnotatedString] to this builder. */
        public fun append(text: RemoteAnnotatedString) {
            val start = this.text.length
            this.text.append(text.text)
            text.annotations?.fastForEach {
                annotations.add(MutableRange(it.item, start + it.start, start + it.end, it.tag))
            }
        }

        /** Appends a slice of the given [RemoteAnnotatedString] to this builder. */
        public fun append(text: RemoteAnnotatedString, start: Int, end: Int) {
            val insertionStart = this.text.length
            this.text.append(text.text, start, end)
            text.getLocalAnnotations(start, end)?.fastForEach {
                annotations.add(
                    MutableRange(
                        it.item,
                        insertionStart + it.start,
                        insertionStart + it.end,
                        it.tag,
                    )
                )
            }
        }

        /** Applies [style] to the given range. */
        public fun addStyle(style: SpanStyle, start: Int, end: Int) {
            annotations.add(MutableRange(item = style, start = start, end = end))
        }

        /** Applies [style] to the given range. */
        public fun addStyle(style: ParagraphStyle, start: Int, end: Int) {
            annotations.add(MutableRange(item = style, start = start, end = end))
        }

        /** Associates a URL link with a range. */
        public fun addLink(url: LinkAnnotation.Url, start: Int, end: Int) {
            annotations.add(MutableRange(url, start, end))
        }

        /** Associates a clickable link with a range. */
        public fun addLink(clickable: LinkAnnotation.Clickable, start: Int, end: Int) {
            annotations.add(MutableRange(clickable, start, end))
        }

        /** Adds a [bullet] to draw before the text. */
        public fun addBullet(bullet: Bullet, start: Int, end: Int) {
            annotations.add(MutableRange(item = bullet, start = start, end = end))
        }

        /** Adds a [bullet] with an [indentation]. */
        public fun addBullet(bullet: Bullet, indentation: TextUnit, start: Int, end: Int) {
            val bulletParStyle = ParagraphStyle(textIndent = TextIndent(indentation, indentation))
            annotations.add(MutableRange(item = bulletParStyle, start = start, end = end))
            annotations.add(MutableRange(item = bullet, start = start, end = end))
        }

        /** Applies the given [SpanStyle] until popped. */
        public fun pushStyle(style: SpanStyle): Int {
            MutableRange(item = style, start = text.length).also {
                styleStack.add(it)
                annotations.add(it)
            }
            return styleStack.size - 1
        }

        /** Applies the given [ParagraphStyle] until popped. */
        public fun pushStyle(style: ParagraphStyle): Int {
            MutableRange(item = style, start = text.length).also {
                styleStack.add(it)
                annotations.add(it)
            }
            return styleStack.size - 1
        }

        /** Applies the given [bullet] until popped. */
        public fun pushBullet(bullet: Bullet): Int {
            MutableRange(item = bullet, start = text.length).also {
                styleStack.add(it)
                annotations.add(it)
            }
            return styleStack.size - 1
        }

        /** Scope for a bullet list. */
        public class BulletScope internal constructor(internal val builder: Builder) {
            internal val bulletListSettingStack = mutableListOf<Pair<TextUnit, Bullet>>()
        }

        private val bulletScope = BulletScope(this)

        /** Creates a bullet list context with common [indentation] and [bullet]. */
        public fun <R : Any> withBulletList(
            indentation: TextUnit = Bullet.DefaultIndentation,
            bullet: Bullet = Bullet.Default,
            block: BulletScope.() -> R,
        ): R {
            val adjustedIndentation =
                bulletScope.bulletListSettingStack.lastOrNull()?.first?.let {
                    check(it.type == indentation.type) {
                        "Indentation unit types of nested bullet lists must match."
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
            bulletScope.bulletListSettingStack.add(Pair(adjustedIndentation, bullet))
            return try {
                block(bulletScope)
            } finally {
                if (bulletScope.bulletListSettingStack.isNotEmpty()) {
                    bulletScope.bulletListSettingStack.removeAt(
                        bulletScope.bulletListSettingStack.lastIndex
                    )
                }
                pop(parIndex)
            }
        }

        /** Creates a bullet list item inside a [BulletScope]. */
        public fun <R : Any> BulletScope.withBulletListItem(
            bullet: Bullet? = null,
            block: Builder.() -> R,
        ): R {
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

        /** Attaches the given [LinkAnnotation] until popped. */
        public fun pushLink(link: LinkAnnotation): Int {
            MutableRange(item = link, start = text.length).also {
                styleStack.add(it)
                annotations.add(it)
            }
            return styleStack.size - 1
        }

        /** Ends the style or annotation added via the most recent push operation. */
        public fun pop() {
            check(styleStack.isNotEmpty()) { "Nothing to pop." }
            val item = styleStack.removeAt(styleStack.size - 1)
            item.end = text.length
        }

        /** Ends styles or annotations up to and including the push that returned [index]. */
        public fun pop(index: Int) {
            check(index < styleStack.size) { "$index should be less than ${styleStack.size}" }
            while ((styleStack.size - 1) >= index) {
                pop()
            }
        }

        /** Constructs the [RemoteAnnotatedString]. */
        public fun toRemoteAnnotatedString(): RemoteAnnotatedString {
            return RemoteAnnotatedString(
                text = text.toString(),
                annotations = annotations.fastMap { it.toRange(text.length) },
            )
        }

        internal fun mapAnnotations(transform: (Range<Annotation>) -> Range<Annotation>) {
            for (i in annotations.indices) {
                val newAnnotation = transform(annotations[i].toRange())
                annotations[i] = MutableRange.fromRange(newAnnotation)
            }
        }

        internal fun flatMapAnnotations(transform: (Range<Annotation>) -> List<Range<Annotation>>) {
            val replacedAnnotations =
                annotations.fastFlatMap { annotation ->
                    transform(annotation.toRange()).fastMap { MutableRange.fromRange(it) }
                }
            annotations.clear()
            annotations.addAll(replacedAnnotations)
        }
    }
}

private fun constructAnnotationsFromSpansAndParagraphs(
    spanStyles: List<RemoteAnnotatedString.Range<SpanStyle>>,
    paragraphStyles: List<RemoteAnnotatedString.Range<ParagraphStyle>>,
): List<RemoteAnnotatedString.Range<Annotation>>? {
    return if (spanStyles.isEmpty() && paragraphStyles.isEmpty()) {
        null
    } else if (paragraphStyles.isEmpty()) {
        spanStyles
    } else if (spanStyles.isEmpty()) {
        paragraphStyles
    } else {
        ArrayList<RemoteAnnotatedString.Range<Annotation>>(spanStyles.size + paragraphStyles.size)
            .also { array ->
                spanStyles.fastForEach { array.add(it) }
                paragraphStyles.fastForEach { array.add(it) }
            }
    }
}

private fun <T> filterRanges(
    ranges: List<RemoteAnnotatedString.Range<T>>?,
    start: Int,
    end: Int,
): List<RemoteAnnotatedString.Range<T>>? {
    require(start <= end) { "start ($start) should be <= end ($end)" }
    val nonNullRange = ranges ?: return null

    return nonNullRange
        .fastFilteredMap({ intersect(start, end, it.start, it.end) }) {
            RemoteAnnotatedString.Range(
                item = it.item,
                start = maxOf(start, it.start) - start,
                end = minOf(end, it.end) - start,
                tag = it.tag,
            )
        }
        .ifEmpty { null }
}

private fun RemoteAnnotatedString.getLocalAnnotations(
    start: Int,
    end: Int,
): List<RemoteAnnotatedString.Range<Annotation>>? {
    if (start == end) return null
    val annotations = annotations ?: return null
    if (start == 0 && end >= this.text.length) {
        return annotations
    }
    return annotations.fastFilteredMap({ intersect(start, end, it.start, it.end) }) {
        RemoteAnnotatedString.Range(
            tag = it.tag,
            item = it.item,
            start = it.start.coerceIn(start, end) - start,
            end = it.end.coerceIn(start, end) - start,
        )
    }
}

internal fun intersect(lStart: Int, lEnd: Int, rStart: Int, rEnd: Int): Boolean {
    return (lStart < rEnd && rStart < lEnd) ||
        (lStart == lEnd && lStart >= rStart && lStart <= rEnd) ||
        (rStart == rEnd && rStart >= lStart && rStart <= lEnd)
}

/** Builds a [RemoteAnnotatedString] using the [RemoteAnnotatedString.Builder] DSL. */
public inline fun buildRemoteAnnotatedString(
    builder: RemoteAnnotatedString.Builder.() -> Unit
): RemoteAnnotatedString = RemoteAnnotatedString.Builder().apply(builder).toRemoteAnnotatedString()

/** Pushes [style], executes [block] and then pops the [style]. */
public inline fun <R : Any> RemoteAnnotatedString.Builder.withStyle(
    style: SpanStyle,
    block: RemoteAnnotatedString.Builder.() -> R,
): R {
    val index = pushStyle(style)
    return try {
        block(this)
    } finally {
        pop(index)
    }
}

/** Pushes [style], executes [block] and then pops the [style]. */
public inline fun <R : Any> RemoteAnnotatedString.Builder.withStyle(
    style: ParagraphStyle,
    crossinline block: RemoteAnnotatedString.Builder.() -> R,
): R {
    val index = pushStyle(style)
    return try {
        block(this)
    } finally {
        pop(index)
    }
}

/** Pushes a [LinkAnnotation], executes [block] and then pops the annotation. */
public inline fun <R : Any> RemoteAnnotatedString.Builder.withLink(
    link: LinkAnnotation,
    block: RemoteAnnotatedString.Builder.() -> R,
): R {
    val index = pushLink(link)
    return try {
        block(this)
    } finally {
        pop(index)
    }
}

/** Creates a [RemoteAnnotatedString] with a [spanStyle] and optional [paragraphStyle]. */
public fun RemoteAnnotatedString(
    text: String,
    spanStyle: SpanStyle,
    paragraphStyle: ParagraphStyle? = null,
): RemoteAnnotatedString =
    RemoteAnnotatedString(
        text,
        listOf(RemoteAnnotatedString.Range(spanStyle, 0, text.length)),
        if (paragraphStyle == null) listOf()
        else listOf(RemoteAnnotatedString.Range(paragraphStyle, 0, text.length)),
    )

/** Creates a [RemoteAnnotatedString] with a [paragraphStyle]. */
public fun RemoteAnnotatedString(
    text: String,
    paragraphStyle: ParagraphStyle,
): RemoteAnnotatedString =
    RemoteAnnotatedString(
        text,
        listOf(),
        listOf(RemoteAnnotatedString.Range(paragraphStyle, 0, text.length)),
    )

/** Creates an uppercase transformed [RemoteAnnotatedString]. */
public fun RemoteAnnotatedString.toUpperCase(
    localeList: LocaleList = LocaleList.current
): RemoteAnnotatedString {
    return transform { str, start, end -> str.substring(start, end).toUpperCase(localeList) }
}

/** Creates a lowercase transformed [RemoteAnnotatedString]. */
public fun RemoteAnnotatedString.toLowerCase(
    localeList: LocaleList = LocaleList.current
): RemoteAnnotatedString {
    return transform { str, start, end -> str.substring(start, end).toLowerCase(localeList) }
}

/** Creates a capitalized [RemoteAnnotatedString]. */
public fun RemoteAnnotatedString.capitalize(
    localeList: LocaleList = LocaleList.current
): RemoteAnnotatedString {
    return transform { str, start, end ->
        if (start == 0) {
            str.substring(start, end).capitalize(localeList)
        } else {
            str.substring(start, end)
        }
    }
}

/** Creates a decapitalized [RemoteAnnotatedString]. */
public fun RemoteAnnotatedString.decapitalize(
    localeList: LocaleList = LocaleList.current
): RemoteAnnotatedString {
    return transform { str, start, end ->
        if (start == 0) {
            str.substring(start, end).decapitalize(localeList)
        } else {
            str.substring(start, end)
        }
    }
}

/** Core transformation method for [RemoteAnnotatedString]. */
internal fun RemoteAnnotatedString.transform(
    transform: (String, Int, Int) -> String
): RemoteAnnotatedString {
    val transitions = mutableIntListOf(0, text.length)
    annotations?.fastForEach {
        transitions.add(it.start)
        transitions.add(it.end)
    }
    transitions.sort()

    val uniqueTransitions = mutableIntListOf()
    for (i in transitions.indices) {
        val value = transitions[i]
        if (uniqueTransitions.isEmpty() || uniqueTransitions.last() != value) {
            uniqueTransitions.add(value)
        }
    }

    val resultStr = StringBuilder()
    val offsetMap = mutableIntIntMapOf()
    offsetMap[0] = 0
    for (i in 0 until uniqueTransitions.size - 1) {
        val start = uniqueTransitions[i]
        val end = uniqueTransitions[i + 1]
        resultStr.append(transform(text, start, end))
        offsetMap.put(end, resultStr.length)
    }
    val newAnnotations =
        annotations?.fastMap {
            RemoteAnnotatedString.Range(
                item = it.item,
                start = offsetMap[it.start],
                end = offsetMap[it.end],
                tag = it.tag,
            )
        }

    return RemoteAnnotatedString(
        text = resultStr.toString(),
        annotations = newAnnotations ?: listOf(),
    )
}
