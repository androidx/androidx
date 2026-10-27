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
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.emoji2.emojipicker.R as EmojiPickerViewR
import androidx.emoji2.emojipicker.test.R
import androidx.emoji2.text.EmojiCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers.hasWindowLayoutParams
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.util.HumanReadables
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
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

    /**
     * Clears the recent emojis and sticky variants saved by earlier tests, and waits for
     * [EmojiCompat] to finish loading. Runs before the activity is launched, because the picker
     * reads these preferences when it's created, and otherwise [EmojiCompat] finishes loading
     * ~500ms after the first activity resumes and calls `notifyDataSetChanged()` mid-test (which
     * inserts compat-rendered emojis on older API levels and shifts adapter positions during
     * scrolling).
     */
    @get:Rule(order = 0)
    val resetPickerStateRule =
        object : ExternalResource() {
            override fun before() {
                ApplicationProvider.getApplicationContext<Context>()
                    .getSharedPreferences(
                        StickyVariantProvider.PREFERENCES_FILE_NAME,
                        Context.MODE_PRIVATE,
                    )
                    .edit()
                    .clear()
                    .commit()
                if (EmojiCompat.isConfigured()) {
                    val emojiCompat = EmojiCompat.get()
                    val loaded = CountDownLatch(1)
                    emojiCompat.registerInitCallback(
                        object : EmojiCompat.InitCallback() {
                            override fun onInitialized() {
                                loaded.countDown()
                            }

                            override fun onFailed(throwable: Throwable?) {
                                loaded.countDown()
                            }
                        }
                    )
                    if (emojiCompat.loadState == EmojiCompat.LOAD_STATE_DEFAULT) {
                        emojiCompat.load()
                    }
                    assertTrue(loaded.await(10, TimeUnit.SECONDS))
                }
            }
        }

    @get:Rule(order = 1)
    val activityTestRule = ActivityScenarioRule(EmojiPickerViewTestActivity::class.java)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // EmojiPickerView loads the bundled emoji list on a background coroutine before inflating
        // its header and body RecyclerViews on the main thread. Wait until inflation completes.
        val inflated = CountDownLatch(1)
        activityTestRule.scenario.onActivity {
            if (it.emojiPickerView.childCount > 0) {
                inflated.countDown()
            } else {
                it.emojiPickerView.setOnHierarchyChangeListener(
                    object : ViewGroup.OnHierarchyChangeListener {
                        override fun onChildViewAdded(parent: View?, child: View?) {
                            inflated.countDown()
                        }

                        override fun onChildViewRemoved(parent: View?, child: View?) {}
                    }
                )
            }
        }
        assertTrue(inflated.await(10, TimeUnit.SECONDS))
    }

    @Test
    fun testCustomEmojiPickerView_rendered() {
        activityTestRule.scenario.onActivity {
            val mEmojiPickerView = it.findViewById<EmojiPickerView>(R.id.emojiPickerTest)
            assertTrue(mEmojiPickerView.isVisible)
            assertEquals(10, mEmojiPickerView.emojiGridColumns)
        }
    }

    @Test
    fun testCustomEmojiPickerView_noVariant() {
        onView(createEmojiViewMatcher(GRINNING_FACE)).check { targetView, noViewFoundException ->
            targetView ?: throw noViewFoundException
            assertFalse(targetView.isLongClickable)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testCustomEmojiPickerView_hasVariant() {
        scrollToEmoji(NOSE_EMOJI)
        // Look the view up through Espresso, which reads the hierarchy on the main thread once the
        // scroll's layout pass is done. Walking the hierarchy from the test thread right after the
        // scroll can miss the newly laid out view.
        onView(createEmojiViewMatcher(NOSE_EMOJI)).check { targetView, noViewFoundException ->
            targetView ?: throw noViewFoundException
            assertTrue(targetView.isLongClickable)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testStickyVariant_displayAndSaved() {
        // Scroll to the nose emoji, long click then select nose in dark skin tone
        scrollToEmoji(NOSE_EMOJI)
        onView(createEmojiViewMatcher(NOSE_EMOJI)).perform(longClick())
        onView(createEmojiViewMatcher(NOSE_EMOJI_DARK))
            .inRoot(hasWindowLayoutParams())
            .perform(click())
        onView(createEmojiViewMatcher(NOSE_EMOJI_DARK)).check(matches(isDisplayed()))
        // Switch back to clear saved preference
        onView(createEmojiViewMatcher(NOSE_EMOJI_DARK)).perform(longClick())
        onView(createEmojiViewMatcher(NOSE_EMOJI)).inRoot(hasWindowLayoutParams()).perform(click())
        onView(createEmojiViewMatcher(NOSE_EMOJI)).check(matches(isDisplayed()))
    }

    @Test
    fun testHeader_highlightCurrentCategory() {
        disableRecent()
        assertSelectedHeaderIndex(0)
        scrollEmojiRowToTop(NOSE_EMOJI)
        assertSelectedHeaderIndex(1)
        scrollEmojiRowToTop(BAT)
        assertSelectedHeaderIndex(3)
        scrollEmojiRowToTop(KEY)
        assertSelectedHeaderIndex(7)
    }

    @SdkSuppress(maxSdkVersion = 34) // b/427259390
    @Test
    fun testHeader_clickingIconWillScrollToCategory() {
        onView(createEmojiViewMatcher(STRAWBERRY)).check { view, _ -> assertNull(view) }

        onView(withId(EmojiPickerViewR.id.emoji_picker_header))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(4, click())
            )

        onView(createEmojiViewMatcher(STRAWBERRY)).check { view, _ -> assertNotNull(view) }
        assertSelectedHeaderIndex(4)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testAddView_throwsException() {
        activityTestRule.scenario.onActivity { it.emojiPickerView.addView(View(context)) }
    }

    private fun createEmojiViewHolderMatcher(emoji: String) =
        object :
            BoundedMatcher<RecyclerView.ViewHolder, EmojiViewHolder>(EmojiViewHolder::class.java) {
            override fun describeTo(description: Description) {}

            override fun matchesSafely(item: EmojiViewHolder) =
                (item.itemView as EmojiView).emoji == emoji
        }

    private fun createEmojiViewMatcher(emoji: String) =
        object : BoundedMatcher<View, EmojiView>(EmojiView::class.java) {
            override fun describeTo(description: Description) {}

            override fun matchesSafely(item: EmojiView) = item.emoji == emoji
        }

    private fun assertSelectedHeaderIndex(expected: Int) {
        onView(withId(EmojiPickerViewR.id.emoji_picker_header)).check { view, noViewFoundException
            ->
            view ?: throw noViewFoundException
            val rv = view as RecyclerView
            // Query via findViewHolderForAdapterPosition rather than rv.children so we read the
            // current ViewHolder for each tab and ignore any pre-change ViewHolder that
            // DefaultItemAnimator is still cross-fading out.
            val selectedIndex =
                (0 until rv.adapter!!.itemCount).single { index ->
                    rv.findViewHolderForAdapterPosition(index)!!
                        .itemView
                        .findViewById<ImageView>(EmojiPickerViewR.id.emoji_picker_header_icon)
                        .isSelected
                }
            assertEquals(expected, selectedIndex)
        }
    }

    private fun scrollToEmoji(emoji: String) =
        onView(withId(EmojiPickerViewR.id.emoji_picker_body))
            .perform(RecyclerViewActions.scrollToHolder(createEmojiViewHolderMatcher(emoji)))

    /**
     * Scrolls the body so the row containing [emoji] is as close to the top as the list allows (the
     * first row, unless the list end is reached). The header highlights the category of the first
     * completely visible item, and [scrollToEmoji] alone leaves the emoji at the bottom of the
     * screen, below the end of the previous category.
     */
    private fun scrollEmojiRowToTop(emoji: String) =
        onView(withId(EmojiPickerViewR.id.emoji_picker_body))
            .perform(scrollEmojiRowToTopAction(emoji))

    /**
     * A [ViewAction] that resolves the adapter position of [emoji] by binding view holders, reusing
     * one holder per view type the same way [RecyclerViewActions.scrollToHolder] does, rather than
     * by reading the currently attached children. It then jumps there with a zero offset in a
     * single layout pass.
     */
    private fun scrollEmojiRowToTopAction(emoji: String) =
        object : ViewAction {
            override fun getConstraints() =
                allOf(isAssignableFrom(RecyclerView::class.java), isDisplayed())

            override fun getDescription() = "scroll row containing $emoji to top"

            override fun perform(uiController: UiController, view: View) {
                val recyclerView = view as RecyclerView
                val adapter = recyclerView.adapter!!
                val holdersByViewType = SparseArray<RecyclerView.ViewHolder>()
                val position =
                    (0 until adapter.itemCount).firstOrNull { pos ->
                        val viewType = adapter.getItemViewType(pos)
                        val holder =
                            holdersByViewType[viewType]
                                ?: adapter.createViewHolder(recyclerView, viewType).also {
                                    holdersByViewType.put(viewType, it)
                                }
                        adapter.bindViewHolder(holder, pos)
                        (holder.itemView as? EmojiView)?.emoji == emoji
                    }
                        ?: throw PerformException.Builder()
                            .withActionDescription(description)
                            .withViewDescription(HumanReadables.describe(view))
                            .withCause(
                                IllegalStateException(
                                    "No item with emoji $emoji among ${adapter.itemCount} items"
                                )
                            )
                            .build()
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    position,
                    0,
                )
                uiController.loopMainThreadUntilIdle()
            }
        }

    private fun disableRecent() {
        activityTestRule.scenario.onActivity {
            it.emojiPickerView.setRecentEmojiProvider(
                object : RecentEmojiProvider {
                    override fun recordSelection(emoji: String) {}

                    override suspend fun getRecentEmojiList(): List<String> = listOf()
                }
            )
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
