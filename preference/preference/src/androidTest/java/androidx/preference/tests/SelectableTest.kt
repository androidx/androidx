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

package androidx.preference.tests

import android.content.Context
import android.graphics.drawable.StateListDrawable
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceViewHolder
import androidx.preference.test.R
import androidx.preference.tests.helpers.PreferenceTestHelperActivity
import androidx.test.annotation.UiThreadTest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test for selectable [Preference] logic. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SelectableTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule(PreferenceTestHelperActivity::class.java)

    private lateinit var fragment: PreferenceFragmentCompat

    private lateinit var preference: Preference
    private lateinit var category: PreferenceCategory

    @Before
    @UiThreadTest
    fun setUp() {
        fragment = activityRule.activity.setupPreferenceHierarchy(R.xml.test_selectable)
        preference = fragment.preferenceScreen.findPreference("preference")!!
        category = fragment.preferenceScreen.findPreference("category")!!
    }

    @Test
    fun testPreference_Selectable_clickPropagated() {
        // Should be selectable by default
        assertTrue(preference.isSelectable)
        var clicks = 0
        activityRule.runOnUiThread {
            preference.setOnPreferenceClickListener {
                clicks++
                true
            }
        }
        // We should receive one click
        onView(withText("Preference")).perform(click())
        activityRule.runOnUiThread { assertEquals(1, clicks) }
    }

    @Test
    fun testPreference_Unselectable_noClickPropagated() {
        var clicks = 0
        activityRule.runOnUiThread {
            preference.isSelectable = false
            preference.setOnPreferenceClickListener {
                clicks++
                true
            }
        }
        // No clicks should occur
        onView(withText("Preference")).perform(click())
        activityRule.runOnUiThread { assertEquals(0, clicks) }
    }

    @Test
    fun testPreferenceCategory_noClickPropagated() {
        // Should be unselectable by default
        assertFalse(category.isSelectable)
        var clicks = 0
        activityRule.runOnUiThread {
            category.setOnPreferenceClickListener {
                clicks++
                true
            }
        }
        // No clicks should occur
        onView(withText("Category")).perform(click())
        activityRule.runOnUiThread { assertEquals(0, clicks) }
    }

    @Test
    fun testPreference_Unselectable_noRipple() {
        activityRule.runOnUiThread {
            val testPreference = TestPreference(activityRule.activity)
            testPreference.title = "Test Preference"
            testPreference.isSelectable = false
            fragment.preferenceScreen.addPreference(testPreference)
        }
        // No click should propagate to the ripple
        onView(withText("Test Preference")).perform(click())
    }

    @Test
    fun testCopyablePreference_Unselectable_noRipple() {
        activityRule.runOnUiThread {
            val testPreference = TestPreference(activityRule.activity)
            testPreference.title = "Test Preference"
            testPreference.summary = "Copyable summary"
            testPreference.isSelectable = false
            testPreference.isCopyingEnabled = true
            fragment.preferenceScreen.removeAll()
            fragment.preferenceScreen.addPreference(testPreference)
        }
        // No click should propagate to the ripple
        onView(withText("Test Preference")).perform(click())
        // However long click should work fine
        onView(withText("Test Preference")).perform(longClick())
        // And the copy context menu should be displayed
        onView(withText("Copy")).check(matches(ViewMatchers.isDisplayed()))
    }

    /*
     * A Drawable that will fail the test when its state changes.
     */
    private class TestDrawable : StateListDrawable() {
        override fun onStateChange(stateSet: IntArray): Boolean {
            if (stateSet.contains(android.R.attr.state_pressed)) {
                fail("Ripple should not have been activated!")
            }
            return super.onStateChange(stateSet)
        }
    }

    /*
     * Preference that will fail the test when it receives a click event, and sets a TestDrawable
     * as its background.
     */
    private class TestPreference(context: Context) : Preference(context) {
        override fun onBindViewHolder(holder: PreferenceViewHolder) {
            super.onBindViewHolder(holder)
            // If there's no background, no ripple effect will play regardless.
            if (holder.itemView.background != null) {
                holder.itemView.setBackground(TestDrawable())
            }
        }
    }
}
