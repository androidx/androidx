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

import androidx.compose.ui.text.LinkAnnotation

/**
 * A class containing text with link annotations.
 *
 * Follows the API of [androidx.compose.ui.text.AnnotatedString], but restricted to the link
 * feature.
 */
public class RemoteAnnotatedString(
    public val text: String,
    public val linkAnnotations: List<Range<LinkAnnotation>> = emptyList(),
) {
    public data class Range<T>(val item: T, val start: Int, val end: Int)

    public class Builder(capacity: Int = 16) {
        private val text = StringBuilder(capacity)
        private val linkAnnotations = mutableListOf<Range<LinkAnnotation>>()
        private val styleStack = mutableListOf<MutableRange<LinkAnnotation>>()

        public val length: Int
            get() = text.length

        public fun append(text: String): Builder {
            this.text.append(text)
            return this
        }

        public fun append(char: Char): Builder {
            this.text.append(char)
            return this
        }

        public fun append(text: RemoteAnnotatedString): Builder {
            val start = this.text.length
            this.text.append(text.text)
            text.linkAnnotations.forEach {
                linkAnnotations.add(Range(it.item, it.start + start, it.end + start))
            }
            return this
        }

        public fun pushLink(link: LinkAnnotation): Int {
            val range = MutableRange(item = link, start = text.length)
            styleStack.add(range)
            return styleStack.size - 1
        }

        public fun pop() {
            check(styleStack.isNotEmpty()) { "Nothing to pop." }
            val item = styleStack.removeAt(styleStack.size - 1)
            item.end = text.length
            linkAnnotations.add(item.toRange())
        }

        public fun pop(index: Int) {
            check(index < styleStack.size) { "$index should be less than ${styleStack.size}" }
            while (styleStack.size - 1 >= index) {
                pop()
            }
        }

        public fun toRemoteAnnotatedString(): RemoteAnnotatedString {
            return RemoteAnnotatedString(
                text = text.toString(),
                linkAnnotations = linkAnnotations.toList(),
            )
        }
    }

    private data class MutableRange<T>(val item: T, val start: Int, var end: Int = -1) {
        fun toRange(): Range<T> = Range(item, start, end)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteAnnotatedString) return false
        if (text != other.text) return false
        if (linkAnnotations != other.linkAnnotations) return false
        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + linkAnnotations.hashCode()
        return result
    }

    override fun toString(): String = text
}

/** Builds a [RemoteAnnotatedString] using the [RemoteAnnotatedString.Builder] DSL. */
public inline fun buildRemoteAnnotatedString(
    builder: RemoteAnnotatedString.Builder.() -> Unit
): RemoteAnnotatedString {
    val b = RemoteAnnotatedString.Builder()
    b.builder()
    return b.toRemoteAnnotatedString()
}

/**
 * Pushes a [LinkAnnotation] to the [RemoteAnnotatedString.Builder], executes [block] and then pops
 * the annotation.
 */
public inline fun <R : Any?> RemoteAnnotatedString.Builder.withLink(
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
