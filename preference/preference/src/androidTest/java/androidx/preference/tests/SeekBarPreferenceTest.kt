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

import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.widget.SeekBar
import androidx.preference.SeekBarPreference
import androidx.preference.test.R
import androidx.preference.tests.helpers.PreferenceTestHelperActivity
import androidx.test.annotation.UiThreadTest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import org.hamcrest.Description
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test for [androidx.preference.SeekBarPreference]. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SeekBarPreferenceTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule(PreferenceTestHelperActivity::class.java)

    private lateinit var seekBarPreference: SeekBarPreference

    @Before
    @UiThreadTest
    fun setUp() {
        val fragment = activityRule.activity.setupPreferenceHierarchy(R.xml.test_seekbar)
        seekBarPreference = fragment.preferenceScreen.getPreference(0) as SeekBarPreference
        seekBarPreference.min = 0
        seekBarPreference.max = 5
    }

    @Test
    fun testSetValue() {
        activityRule.runOnUiThread {
            // When a value of 3 is set
            seekBarPreference.value = 3
            // The internal value should be set to 3
            assertEquals(3, seekBarPreference.value)
        }
        // The seekbar's progress should be set to 3
        onView(withId(androidx.preference.R.id.seekbar)).check(matches(withSeekBarProgress(3)))
    }

    @Test
    fun testSetValue_belowMinValue() {
        activityRule.runOnUiThread {
            // Given a non-zero min value
            seekBarPreference.min = 3
            // When a value lower than the min value is set
            seekBarPreference.value = 0
            // The actual value set should be equal to the min value
            assertEquals(3, seekBarPreference.value)
        }
        // The seekbar's progress should be 0, as we are currently at the min value
        onView(withId(androidx.preference.R.id.seekbar)).check(matches(withSeekBarProgress(0)))
    }

    @Test
    fun testSetValue_aboveMaxValue() {
        activityRule.runOnUiThread {
            // Given a max value
            seekBarPreference.max = 7
            // When a value higher than the max value is set
            seekBarPreference.value = 10
            // The actual value set should be equal to the max value
            assertEquals(7, seekBarPreference.value)
        }
        // The seekbar's progress should be equal to the max value
        onView(withId(androidx.preference.R.id.seekbar)).check(matches(withSeekBarProgress(7)))
    }

    @Test
    fun testSeekBarLabel_updatesWhenValueIsChanged() {
        // By default only the seekbar, not the label should be visible
        onView(withId(androidx.preference.R.id.seekbar)).check(matches(isDisplayed()))
        onView(withId(androidx.preference.R.id.seekbar_value)).check(matches(not(isDisplayed())))

        // When we enable showing the value
        activityRule.runOnUiThread { seekBarPreference.showSeekBarValue = true }

        // Both the seekbar and the label should be visible
        onView(withId(androidx.preference.R.id.seekbar)).check(matches(isDisplayed()))
        onView(allOf(withId(androidx.preference.R.id.seekbar_value), withText("0")))
            .check(matches(isDisplayed()))

        // When we change the value
        activityRule.runOnUiThread { seekBarPreference.value = 5 }

        // The label should update to show the new value
        onView(allOf(withId(androidx.preference.R.id.seekbar_value), withText("5")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSeekBarLabel_updatesWhenSeekbarProgressChanges() {
        activityRule.runOnUiThread { seekBarPreference.showSeekBarValue = true }
        onView(allOf(withId(androidx.preference.R.id.seekbar_value), withText("0")))
            .check(matches(isDisplayed()))

        // When the seekbar's progress changes
        activityRule.activity.findViewById<SeekBar>(androidx.preference.R.id.seekbar).progress = 5

        // The label should update to show the new value
        onView(allOf(withId(androidx.preference.R.id.seekbar_value), withText("5")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSeekBarLabel_shouldCorrectlyHandlesMinValue() {
        activityRule.runOnUiThread {
            // Set the minimum and the saved value to 3
            seekBarPreference.min = 3
            seekBarPreference.value = 3
            seekBarPreference.showSeekBarValue = true
        }

        // The label should display the saved value
        onView(allOf(withId(androidx.preference.R.id.seekbar_value), withText("3")))
            .check(matches(isDisplayed()))

        // Since the value is also the minimum, the seekbar should have no progress
        onView(withId(androidx.preference.R.id.seekbar)).check(matches(withSeekBarProgress(0)))

        // Change the value to 5
        activityRule.runOnUiThread { seekBarPreference.value = 5 }

        // The label should display the saved value
        onView(allOf(withId(androidx.preference.R.id.seekbar_value), withText("5")))
            .check(matches(isDisplayed()))

        // However since the minimum is 3, the seekbar's progress should be offset by 3, and should
        // only have progressed to 2 (the current value - the minimum)
        onView(withId(androidx.preference.R.id.seekbar)).check(matches(withSeekBarProgress(2)))
    }

    @Test
    // Seems that these tests are flaky on certain devices with large screens due to the swipe not
    // fully dragging from one end to another. Should be safer to only run them on newer devices
    // where they are stable.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun testSeekBarPreferenceChangeListener() {
        // How many times the change listener has been called
        var updateCount = 0
        activityRule.runOnUiThread {
            seekBarPreference.value = 0
            seekBarPreference.showSeekBarValue = true
            seekBarPreference.updatesContinuously = false
            seekBarPreference.setOnPreferenceChangeListener { _, _ ->
                updateCount++
                true
            }
        }

        onView(allOf(withId(androidx.preference.R.id.seekbar_value), withText("0")))
            .check(matches(isDisplayed()))
        onView(withId(androidx.preference.R.id.seekbar)).check(matches(withSeekBarProgress(0)))

        // Fully drag the seekbar from left to right
        onView(withId(androidx.preference.R.id.seekbar)).perform(dragSeekBar())

        // The current value should be 5
        onView(allOf(withId(androidx.preference.R.id.seekbar_value), withText("5")))
            .check(matches(isDisplayed()))
        onView(withId(androidx.preference.R.id.seekbar)).check(matches(withSeekBarProgress(5)))

        activityRule.runOnUiThread {
            // SeekBarPreference should only attempt to update once, when the drag has stopped
            assertEquals(1, updateCount)
            assertEquals(5, seekBarPreference.value)
        }
    }

    @Test
    // Seems that these tests are flaky on certain devices with large screens due to the swipe not
    // fully dragging from one end to another. Should be safer to only run them on newer devices
    // where they are stable.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun testSeekBarPreferenceChangeListenerWithContinuousUpdates() {
        // How many times the change listener has been called
        var updateCount = 0
        activityRule.runOnUiThread {
            seekBarPreference.value = 0
            seekBarPreference.max = 5
            seekBarPreference.showSeekBarValue = true
            seekBarPreference.updatesContinuously = true
            seekBarPreference.setOnPreferenceChangeListener { _, _ ->
                updateCount++
                true
            }
        }

        onView(allOf(withId(androidx.preference.R.id.seekbar_value), withText("0")))
            .check(matches(isDisplayed()))
        onView(withId(androidx.preference.R.id.seekbar)).check(matches(withSeekBarProgress(0)))

        // Fully drag the seekbar from left to right
        onView(withId(androidx.preference.R.id.seekbar)).perform(dragSeekBar())

        // The current value should be 5
        onView(allOf(withId(androidx.preference.R.id.seekbar_value), withText("5")))
            .check(matches(isDisplayed()))
        onView(withId(androidx.preference.R.id.seekbar)).check(matches(withSeekBarProgress(5)))

        activityRule.runOnUiThread {
            // SeekBarPreference should attempt to update every time the seekbar's progress changed.
            // From 0-5, it should update 5 times for a smooth (and slow) swipe, but due to the
            // flakiness of injecting swipes sometimes the move event is fast enough that
            // intermediate updates are skipped in tests. Just assert that we had more than 1 update
            // so that we know that the SeekBarPreference is updating during the swipe.
            Truth.assertThat(updateCount).isGreaterThan(1)
            assertEquals(5, seekBarPreference.value)
        }
    }

    /** A [ViewAction] that drags a [SeekBar] from its left edge to the right edge of the screen */
    private fun dragSeekBar(): ViewAction {
        return GeneralSwipeAction(
            Swipe.FAST,
            CoordinatesProvider { view ->
                val location = IntArray(2)
                view.getLocationOnScreen(location)
                val posX = location[0]
                val posY = location[1]
                // Start at the beginning of the seekbar
                floatArrayOf(posX.toFloat(), posY.toFloat())
            },
            CoordinatesProvider { view ->
                val location = IntArray(2)
                view.getLocationOnScreen(location)
                // We want to swipe all the way to the right edge of the screen to avoid
                // flakiness due to sometimes not reaching the end of the seekbar
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION") /* defaultDisplay */
                activityRule.activity.windowManager.defaultDisplay.getMetrics(metrics)
                val posX = metrics.widthPixels
                val posY = location[1]
                floatArrayOf(posX.toFloat(), posY.toFloat())
            },
            Press.PINPOINT,
        )
    }

    /**
     * A matcher to assert the current progress of a [SeekBar]
     *
     * @param progress The expected progress of the [SeekBar]
     */
    private fun withSeekBarProgress(progress: Int): BoundedMatcher<View, SeekBar> {
        return object : BoundedMatcher<View, SeekBar>(SeekBar::class.java) {
            override fun matchesSafely(item: SeekBar?): Boolean {
                return item!!.progress == progress
            }

            override fun describeTo(description: Description?) {
                description?.appendText("with SeekBar progress:")?.appendValue(progress)
            }
        }
    }
}
