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
import com.google.common.truth.Truth.assertAbout
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

internal fun assertThat(bitmap: Bitmap?): BitmapSubject {
    return assertAbout(BitmapSubject.SUBJECT_FACTORY).that(bitmap)!!
}

internal fun assertThat(typeface: Typeface?): TypefaceSubject {
    return assertAbout(TypefaceSubject.SUBJECT_FACTORY).that(typeface)!!
}

internal fun assertThat(charSequence: CharSequence?): CharSequenceSubject {
    return assertAbout(CharSequenceSubject.SUBJECT_FACTORY).that(charSequence)!!
}