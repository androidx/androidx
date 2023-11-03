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

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.emoji2.emojipicker.R as EmojiPickerViewR
import androidx.emoji2.emojipicker.test.R
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers.hasWindowLayoutParams
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Description
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

class EmojiPickerViewTestActivity : Activity() {
    lateinit var emojiPickerView: EmojiPickerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.inflation_test)

        emojiPickerView = findViewById(R.id.emojiPickerTest)
    }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class EmojiPickerViewTest {
    private lateinit var context: Context

    @get:Rule
    val activityTestRule = ActivityScenarioRule(
        EmojiPickerViewTestActivity::class.java
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testCustomEmojiPickerView_rendered() {
        activityTestRule.scenario.onActivity {
            val mEmojiPickerView = it.findViewById<EmojiPickerView>(R.id.emojiPickerTest)
            assert(mEmojiPickerView.isVisible)
            assertEquals(mEmojiPickerView.emojiGridColumns, 10)
        }
    }

    @Test
    fun testCustomEmojiPickerView_noVariant() {
        activityTestRule.scenario.onActivity {
            val targetView = findViewByEmoji(
                it.findViewById(R.id.emojiPickerTest),
                GRINNING_FACE
            )!!
            // Not long-clickable
            assertEquals(targetView.isLongClickable, false)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testCustomEmojiPickerView_hasVariant() {
        lateinit var view: EmojiPickerView
        activityTestRule.scenario.onActivity {
            view = it.findViewById(R.id.emojiPickerTest)
        }
        findViewByEmoji(view, NOSE_EMOJI)
            ?: onView(withId(EmojiPickerViewR.id.emoji_picker_body))
                .perform(
                    RecyclerViewActions.scrollToHolder(createEmojiViewHolderMatcher(NOSE_EMOJI))
                )
        val targetView = findViewByEmoji(view, NOSE_EMOJI)!!
        // Long-clickable
        assertEquals(targetView.isLongClickable, true)
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    @Ignore("b/294556440")
    fun testStickyVariant_displayAndSaved() {
        lateinit var view: EmojiPickerView
        activityTestRule.scenario.onActivity {
            view = it.findViewById(R.id.emojiPickerTest)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        // Scroll to the nose emoji, long click then select nose in dark skin tone
        findViewByEmoji(view, NOSE_EMOJI)
            ?: onView(withId(EmojiPickerViewR.id.emoji_picker_body))
                .perform(
                    RecyclerViewActions.scrollToHolder(createEmojiViewHolderMatcher(NOSE_EMOJI))
                )
        onView(createEmojiViewMatcher(NOSE_EMOJI)).perform(longClick())
        onView(createEmojiViewMatcher(NOSE_EMOJI_DARK))
            .inRoot(hasWindowLayoutParams())
            .perform(click())
        assertNotNull(findViewByEmoji(view, NOSE_EMOJI_DARK))
        // Switch back to clear saved preference
        onView(createEmojiViewMatcher(NOSE_EMOJI_DARK)).perform(longClick())
        onView(createEmojiViewMatcher(NOSE_EMOJI))
            .inRoot(hasWindowLayoutParams())
            .perform(click())
        assertNotNull(findViewByEmoji(view, NOSE_EMOJI))
    }

    @Ignore // b/260915957
    @Test
    fun testHeader_highlightCurrentCategory() {
        disableRecent()
        assertSelectedHeaderIndex(0)
        scrollToEmoji(NOSE_EMOJI)
        assertSelectedHeaderIndex(1)
        scrollToEmoji(BAT)
        assertSelectedHeaderIndex(3)
        scrollToEmoji(KEY)
        assertSelectedHeaderIndex(7)
    }

    @Test
    fun testHeader_clickingIconWillScrollToCategory() {
        onView(createEmojiViewMatcher(STRAWBERRY)).check { view, _ ->
            assertNull(view)
        }

        onView(withId(EmojiPickerViewR.id.emoji_picker_header)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                4,
                click()
            )
        )

        onView(createEmojiViewMatcher(STRAWBERRY)).check { view, _ ->
            assertNotNull(view)
        }
        assertSelectedHeaderIndex(4)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testAddView_throwsException() {
        activityTestRule.scenario.onActivity {
            it.emojiPickerView.addView(View(context))
        }
    }

    private fun findViewByEmoji(root: View, emoji: String) =
        try {
            mutableListOf<EmojiView>().apply {
                findEmojiViews(root, this)
            }.first { it.emoji == emoji }
        } catch (e: NoSuchElementException) {
            null
        }

    private fun findEmojiViews(root: View, output: MutableList<EmojiView>) {
        if (root !is ViewGroup) {
            return
        }
        for (i in 0 until root.childCount) {
            root.getChildAt(i).apply {
                if (this is EmojiView) {
                    output.add(this)
                }
            }.also {
                findEmojiViews(it, output)
            }
        }
    }

    private fun createEmojiViewHolderMatcher(emoji: String) =
        object :
            BoundedMatcher<RecyclerView.ViewHolder, EmojiViewHolder>(EmojiViewHolder::class.java) {
            override fun describeTo(description: Description) {}
            override fun matchesSafely(item: EmojiViewHolder) =
                (item.itemView as EmojiView).emoji == emoji
        }

    private fun createEmojiViewMatcher(emoji: String) =
        object :
            BoundedMatcher<View, EmojiView>(EmojiView::class.java) {
            override fun describeTo(description: Description) {}
            override fun matchesSafely(item: EmojiView) = item.emoji == emoji
        }

    private fun assertSelectedHeaderIndex(expected: Int) = onView(
        withId(EmojiPickerViewR.id.emoji_picker_header)
    ).check { view, noViewFoundException ->
        view ?: throw noViewFoundException
        val selectedIndex = (view as RecyclerView)
            .children
            .withIndex()
            .single { (_, view) ->
                view.findViewById<ImageView>(EmojiPickerViewR.id.emoji_picker_header_icon)
                    .isSelected
            }.index
        assertEquals(expected, selectedIndex)
    }

    private fun scrollToEmoji(emoji: String) = onView(withId(EmojiPickerViewR.id.emoji_picker_body))
        .perform(RecyclerViewActions.scrollToHolder(createEmojiViewHolderMatcher(emoji)))

    private fun disableRecent() {
        activityTestRule.scenario.onActivity {
            it.emojiPickerView.setRecentEmojiProvider(object : RecentEmojiProvider {
                override fun recordSelection(emoji: String) {}

                override suspend fun getRecentEmojiList(): List<String> = listOf()
            })
        }
    }

    companion object {
        const val GRINNING_FACE = "\uD83D\uDE00"
        const val NOSE_EMOJI = "\uD83D\uDC43"
        const val NOSE_EMOJI_DARK = "\uD83D\uDC43\uD83C\uDFFF"
        const val BAT = "\uD83E\uDD87"
        const val KEY = "\uD83D\uDD11"
        const val STRAWBERRY = "\uD83C\uDF53"
    }
}
