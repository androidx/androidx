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

package androidx.ui.text.matchers

import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontWeight
import org.hamcrest.Matcher
import kotlin.reflect.KClass

/**
 * Returns a matcher to check the equality of two [Bitmap]s.
 *
 * @param operand the [Bitmap] to be matched.
 * @see IsEqualBitmap
 */
fun equalToBitmap(operand: Bitmap): Matcher<Bitmap> {
    return IsEqualBitmap(operand)
}

/**
 * Returns a matcher to check if the given text contains a span matching the given class and
 * position. Notice that the created matcher won't match if the given text is not an
 * [android.text.Spanned] object.
 *
 * When [predicate] function is provided, for the span that is found, predicate will be called
 * to further validate the span object.
 *
 * @param spanClazz the class of the expected span
 * @param start start position of the expected span
 * @param end end position of the expected span
 * @param predicate function to further assert the span object
 * @see HasSpan
 */
fun <T : Any> hasSpan(
    spanClazz: KClass<out T>,
    start: Int,
    end: Int,
    predicate: ((T) -> Boolean)? = null
): Matcher<CharSequence> {
    return HasSpan(spanClazz, start, end, predicate)
}

/**
 * Returns a matcher to check if the given text doesn't contain any span matching the given class
 * in the given range.
 *
 * @param spanClazz the class of the checked span
 * @param start the start range checked range
 * @param end the end of the checked range
 * @see HasSpan
 */
fun <T : Any> notHasSpan(
    spanClazz: KClass<out T>,
    start: Int,
    end: Int
): Matcher<CharSequence> {
    return NotHasSpan(spanClazz, start, end)
}

/**
 * Similar to [hasSpan], and the returned matcher will also check that the span is not covered by
 * other spans.
 *
 * @param spanClazz the class of the expected span
 * @param start start position of the expected span
 * @param end end position of the expected span
 * @see HasSpanOnTop
 */
fun <T : Any> hasSpanOnTop(
    spanClazz: KClass<out T>,
    start: Int,
    end: Int,
    predicate: ((T) -> Boolean)? = null
): Matcher<CharSequence> {
    return HasSpanOnTop(spanClazz, start, end, predicate)
}

/**
 * Verifies that [Typeface] object has the given [FontWeight] and [FontStyle].
 *
 * @param [fontWeight] expected [FontWeight]
 * @param [fontStyle] expected [FontStyle]
 */
fun isTypefaceOf(fontWeight: FontWeight, fontStyle: FontStyle): Matcher<Typeface> {
    return IsTypefaceOf(fontWeight, fontStyle)
}