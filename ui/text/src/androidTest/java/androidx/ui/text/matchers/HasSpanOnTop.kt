/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.text.matchers

import android.text.Spanned
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import kotlin.reflect.KClass

/**
 * Similar to [HasSpan], and it will also check that the span is not covered by other spans.
 * Notice that the matcher won't match if the given text is not an [Spanned] object.
 *
 * @param spanClazz the class of the expected span
 * @param start start position of the expected span
 * @param end end position of the expected span
 */
class HasSpanOnTop<T : Any>(
    private val spanClazz: KClass<out T>,
    private val start: Int,
    private val end: Int,
    private val predicate: ((T) -> Boolean)?
) : BaseMatcher<CharSequence>() {
    override fun matches(item: Any?): Boolean {
        if (item !is Spanned) return false
        // We assume that getSpans(..) sorts span according to when they are applied.
        val spans = item.getSpans(start, end, spanClazz.java)
        for (span in spans.reversed()) {
            val spanStart = item.getSpanStart(span)
            val spanEnd = item.getSpanEnd(span)
            if (spanStart == start || spanEnd == end) {
                // Find the target span
                return predicate?.invoke(span) ?: true
            } else if (start in spanStart until spanEnd || end in (spanStart + 1)..spanEnd) {
                // Find a span covers the given range.
                // Impossible to find the target span on top.
                return false
            }
        }
        return false
    }

    override fun describeTo(description: Description?) {
        description?.appendText("${spanClazz.java.name}[$start, $end] on top")
    }

    override fun describeMismatch(item: Any?, description: Description?) {
        val msg = if (item !is Spanned) {
            "is not a Spanned object"
        } else {
            val spans = item.getSpans(0, item.length, Any::class.java)
            if (spans.isEmpty()) {
                "has no span"
            } else {
                spans.joinToString(
                    separator = ", ",
                    transform = {
                        val start = item.getSpanStart(it)
                        val end = item.getSpanEnd(it)
                        "${it::class.java.name}[$start, $end]"
                    }
                )
            }
        }
        description?.appendText(msg)
    }
}