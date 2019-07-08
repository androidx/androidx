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
 * Matcher to check if the given text contains a span matching the given class and position.
 * Notice that the matcher won't match if the given text is not an [Spanned] object.
 *
 * When [predicate] function is provided, for the span that is found, predicate will be called
 * to further validate the span object.
 *
 * @param spanClazz the class of the expected span
 * @param start start position of the expected span
 * @param end end position of the expected span
 * @param predicate function to further assert the span object
 */
class HasSpan<T : Any>(
    private val spanClazz: KClass<out T>,
    private val start: Int,
    private val end: Int,
    private val predicate: ((T) -> Boolean)? = null
) : BaseMatcher<CharSequence>() {
    override fun matches(item: Any?): Boolean {
        if (item !is Spanned) return false
        val spans = item.getSpans(start, end, spanClazz.java)
        for (span in spans) {
            if (item.getSpanStart(span) == start &&
                item.getSpanEnd(span) == end
            ) {
                return predicate?.invoke(span) ?: true
            }
        }
        return false
    }

    override fun describeTo(description: Description?) {
        description?.appendText("${spanClazz.java.name}[$start, $end]")
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