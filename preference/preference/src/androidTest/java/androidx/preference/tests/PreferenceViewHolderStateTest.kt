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

package androidx.preference.tests

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceViewHolder
import androidx.preference.test.R
import androidx.preference.tests.helpers.PreferenceTestHelperActivity
import androidx.test.annotation.UiThreadTest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.repeatedlyUntil
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test for resetting [PreferenceViewHolder] state. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PreferenceViewHolderStateTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule(PreferenceTestHelperActivity::class.java)

    private lateinit var fragment: PreferenceFragmentCompat

    @Before
    @UiThreadTest
    fun setUp() {
        fragment = activityRule.activity.setupPreferenceHierarchy()
    }

    @Test
    fun testReusingViewHolder_unselectablePreference_stateReset() {
        val preferenceScreen = fragment.preferenceScreen

        val copyableTitle = "Copyable"

        // Add 20 unselectable + copyable Preferences. This is so that when we add a new item, it
        // will be offscreen, and when scrolling to it RecyclerView will attempt to reuse an
        // existing cached ViewHolder.
        val preferences =
            (1..20).map { index ->
                TestPreference(fragment.context!!).apply {
                    title = copyableTitle + index
                    summary = "Summary to copy"
                    isCopyingEnabled = true
                    isSelectable = false
                }
            }

        activityRule.runOnUiThread { preferences.forEach { preferenceScreen.addPreference(it) } }

        // Wait for a Preference to be visible
        onView(withText("${copyableTitle}1")).check(matches(isDisplayed()))

        // The title color should be the same as the summary color for unselectable Preferences
        val unselectableTitleColor = preferences[0].titleColor
        val unselectableSummaryColor = preferences[0].summaryColor

        assertEquals(unselectableTitleColor!!, unselectableSummaryColor!!)

        val normalTitle = "Normal"

        // Add a normal, selectable Preference to the end. This will currently be displayed off
        // screen, and not yet bound.
        val normalPreference =
            TestPreference(fragment.context!!).apply {
                title = normalTitle
                summary = "Normal summary"
            }

        // Assert that we haven't bound this Preference yet
        assertNull(normalPreference.background)
        assertNull(normalPreference.titleColor)
        assertNull(normalPreference.summaryColor)

        activityRule.runOnUiThread { preferenceScreen.addPreference(normalPreference) }

        val maxAttempts = 10

        // Scroll until we find the new Preference, which will trigger RecyclerView to rebind an
        // existing cached ViewHolder, so we can ensure that the state is reset. We use swipeUp
        // here instead of scrolling directly to the item, as we need to allow time for older
        // views to be cached first, instead of instantly snapping to the item.
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(repeatedlyUntil(swipeUp(), hasDescendant(withText(normalTitle)), maxAttempts))

        // We should have a ripple / state list drawable as the background
        assertNotNull(normalPreference.background)
        // The title color should be different from the title of the unselected Preference
        assertNotEquals(unselectableTitleColor, normalPreference.titleColor)
        // The summary color should be the same as the unselected Preference
        assertEquals(unselectableSummaryColor, normalPreference.summaryColor)
    }

    @Test
    fun testReusingViewHolder_disabledPreference_stateReset() {
        val preferenceScreen = fragment.preferenceScreen

        val disabledTitle = "Disabled"

        // Add 40 disabled Preferences. The ones at the end haven't been bound yet, and we want
        // to ensure that when they are bound, reusing a ViewHolder, they should have the correct
        // disabled state.
        val preferences =
            (1..40).map { index ->
                TestPreference(fragment.context!!).apply {
                    title = disabledTitle + index
                    isEnabled = false
                }
            }

        activityRule.runOnUiThread { preferences.forEach { preferenceScreen.addPreference(it) } }

        // Wait for a Preference to be visible
        onView(withText("${disabledTitle}1")).check(matches(isDisplayed()))

        val expectedTitleColor = preferences[0].titleColor

        val maxAttempts = 10

        // Scroll until the end, ensuring all Preferences have been bound.
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                repeatedlyUntil(
                    swipeUp(),
                    hasDescendant(withText("${disabledTitle}40")),
                    maxAttempts,
                )
            )

        // All preferences should have the correct title color
        preferences.forEach { preference ->
            assertEquals(expectedTitleColor, preference.titleColor)
        }
    }
}

/**
 * Testing [Preference] class that records the background [Drawable] and the color of its title and
 * summary once bound.
 */
private class TestPreference(context: Context) : Preference(context) {
    var background: Drawable? = null
    var titleColor: Int? = null
    var summaryColor: Int? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        background = holder.itemView.background
        titleColor = (holder.findViewById(android.R.id.title) as TextView).currentTextColor
        summaryColor = (holder.findViewById(android.R.id.summary) as TextView).currentTextColor
    }
}
