/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.text.platform

import android.text.SpannableString
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class AndroidParagraphHelperTest {
    @Test
    fun setSpanWithPriority_withSamePriority_returnSameSetOrder() {
        val spanned = SpannableString("Hello World")
        val span0 = Any()
        val span1 = Any()
        val span2 = Any()
        val priority = 0
        spanned.setSpanWithPriority(span0, 0, 1, priority)
        spanned.setSpanWithPriority(span1, 0, 1, priority)
        spanned.setSpanWithPriority(span2, 0, 1, priority)

        val spans = spanned.getSpans(0, 1, Any::class.java)
        assertThat(spans[0]).isSameInstanceAs(span0)
        assertThat(spans[1]).isSameInstanceAs(span1)
        assertThat(spans[2]).isSameInstanceAs(span2)
    }

    @Test
    fun setSpanWithPriority_withIncreasingPriority_returnPriorityOrder() {
        val spanned = SpannableString("Hello World")
        val span0 = Any()
        val span1 = Any()
        val span2 = Any()
        spanned.setSpanWithPriority(span0, 0, 1, 0)
        spanned.setSpanWithPriority(span1, 0, 1, 1)
        spanned.setSpanWithPriority(span2, 0, 1, 2)

        val spans = spanned.getSpans(0, 1, Any::class.java)
        assertThat(spans[0]).isSameInstanceAs(span2)
        assertThat(spans[1]).isSameInstanceAs(span1)
        assertThat(spans[2]).isSameInstanceAs(span0)
    }

    @Test
    fun setSpanWithPriority_withDescendingPriority_returnPriorityOrder() {
        val spanned = SpannableString("Hello World")
        val span0 = Any()
        val span1 = Any()
        val span2 = Any()
        spanned.setSpanWithPriority(span0, 0, 1, 2)
        spanned.setSpanWithPriority(span1, 0, 1, 1)
        spanned.setSpanWithPriority(span2, 0, 1, 0)

        val spans = spanned.getSpans(0, 1, Any::class.java)
        assertThat(spans[0]).isSameInstanceAs(span0)
        assertThat(spans[1]).isSameInstanceAs(span1)
        assertThat(spans[2]).isSameInstanceAs(span2)
    }

    @Test
    fun setSpanWithPriority_withWigglingPriority_returnPriorityOrder() {
        val spanned = SpannableString("Hello World")
        val span0 = Any()
        val span1 = Any()
        val span2 = Any()
        val span3 = Any()
        spanned.setSpanWithPriority(span0, 0, 1, 0)
        spanned.setSpanWithPriority(span1, 0, 1, 1)
        spanned.setSpanWithPriority(span2, 0, 1, 0)
        spanned.setSpanWithPriority(span3, 0, 1, 1)

        val spans = spanned.getSpans(0, 1, Any::class.java)
        assertThat(spans[0]).isSameInstanceAs(span1)
        assertThat(spans[1]).isSameInstanceAs(span3)
        assertThat(spans[2]).isSameInstanceAs(span0)
        assertThat(spans[3]).isSameInstanceAs(span2)
    }
}