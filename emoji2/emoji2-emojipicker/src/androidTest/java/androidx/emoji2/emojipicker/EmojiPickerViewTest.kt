/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.emoji2.emojipicker

import androidx.emoji2.emojipicker.R as EmojiPickerViewR
import androidx.test.espresso.Espresso.onView
import org.hamcrest.Description
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.emoji2.emojipicker.test.R
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

class EmojiPickerViewTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.inflation_test)
    }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class EmojiPickerViewTest {
    private lateinit var mContext: Context

    @get:Rule
    val mActivityTestRule = ActivityScenarioRule(
        EmojiPickerViewTestActivity::class.java
    )

    @Before
    fun setUp() {
        mContext = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testCustomEmojiPickerView_rendered() {
        mActivityTestRule.scenario.onActivity {
            val mEmojiPickerView = it.findViewById<EmojiPickerView>(R.id.emojiPickerTest)
            assert(mEmojiPickerView.isVisible)
            assertEquals(mEmojiPickerView.emojiGridColumns, 10)
        }
    }

    @Test
    fun testCustomEmojiPickerView_noVariant() {
        mActivityTestRule.scenario.onActivity {
            val targetView = findViewByEmoji(
                it.findViewById(R.id.emojiPickerTest),
                "\uD83D\uDE00"
            )
            // No variant indicator
            assertEquals(
                (targetView.parent as FrameLayout).findViewById<ImageView>(
                    EmojiPickerViewR.id.variant_availability_indicator
                ).visibility,
                GONE
            )
            // Not long-clickable
            assertEquals(targetView.isLongClickable, false)
        }
    }

    @Test
    fun testCustomEmojiPickerView_hasVariant() {
        // 👃 has variants
        val noseEmoji = "\uD83D\uDC43"
        val noseEmojiViewHolderMatcher = object :
            BoundedMatcher<RecyclerView.ViewHolder, EmojiViewHolder>(EmojiViewHolder::class.java) {
            override fun describeTo(description: Description) {}

            override fun matchesSafely(item: EmojiViewHolder) =
                (item.itemView as FrameLayout)
                    .findViewById<EmojiView>(EmojiPickerViewR.id.emoji_view)
                    .emoji == noseEmoji
        }
        lateinit var bodyView: EmojiPickerBodyView
        mActivityTestRule.scenario.onActivity {
            bodyView = it.findViewById<EmojiPickerView>(R.id.emojiPickerTest)
                .findViewById(EmojiPickerViewR.id.emoji_picker_body)
        }
        onView(withId(EmojiPickerViewR.id.emoji_picker_body))
            .perform(RecyclerViewActions.scrollToHolder(noseEmojiViewHolderMatcher))
        val targetView = findViewByEmoji(bodyView, noseEmoji)
        // Variant indicator visible
        assertEquals(
            (targetView.parent as FrameLayout).findViewById<ImageView>(
                EmojiPickerViewR.id.variant_availability_indicator
            ).visibility, VISIBLE
        )
        // Long-clickable
        assertEquals(targetView.isLongClickable, true)
    }

    private fun findViewByEmoji(root: View, emoji: String) =
        mutableListOf<View>().apply {
            findViewsById(
                root,
                EmojiPickerViewR.id.emoji_view, this
            )
        }.first { (it as EmojiView).emoji == emoji }

    private fun findViewsById(root: View, id: Int, output: MutableList<View>) {
        if (root !is ViewGroup) {
            return
        }
        for (i in 0 until root.childCount) {
            root.getChildAt(i).apply {
                if (this.id == id) {
                    output.add(this)
                }
            }.also {
                findViewsById(it, id, output)
            }
        }
    }
}
