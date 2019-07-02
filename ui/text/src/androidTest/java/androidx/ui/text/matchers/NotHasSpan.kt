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

import android.text.Spanned
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import kotlin.reflect.KClass

class NotHasSpan<T : Any>(
    private val spanClazz: KClass<out T>,
    private val start: Int,
    private val end: Int
) : BaseMatcher<CharSequence>() {
    override fun matches(item: Any?): Boolean {
        if (item !is Spanned) return true
        return item.nextSpanTransition(-1, end, spanClazz.java) >= end
    }

    override fun describeTo(description: Description?) {
        description?.appendText("no ${spanClazz.java.name} in range [$start, $end]")
    }

    override fun describeMismatch(item: Any?, description: Description?) {
        val msg = if (item !is Spanned) {
            "unexpected mismatch"
        } else {
            val spans = item.getSpans(start, end, spanClazz.java)
            spans.joinToString(
                prefix = "find spans ",
                separator = ", ",
                transform = {
                    val start = item.getSpanStart(it)
                    val end = item.getSpanEnd(it)
                    "${it::class.java.name}[$start, $end]"
                }
            )
        }
        description?.appendText(msg)
    }
}
