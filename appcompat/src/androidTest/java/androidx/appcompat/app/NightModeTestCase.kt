/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.appcompat.app

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.testutils.NightModeUtils.assertConfigurationNightModeEquals
import androidx.appcompat.testutils.NightModeUtils.isSystemNightThemeEnabled
import androidx.appcompat.testutils.NightModeUtils.setNightModeAndWait
import androidx.appcompat.testutils.NightModeUtils.setNightModeAndWaitForDestroy
import androidx.appcompat.testutils.TestUtilsMatchers.isBackground
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.testutils.LifecycleOwnerUtils.waitForRecreation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull

import android.content.res.Configuration
import android.location.LocationManager
import android.webkit.WebView

import androidx.appcompat.test.R
import androidx.appcompat.testutils.NightModeUtils.NightSetMode
import androidx.core.content.ContextCompat
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.util.concurrent.CountDownLatch

@Suppress("DEPRECATION")
@LargeTest
@RunWith(Parameterized::class)
class NightModeTestCase(private val setMode: NightSetMode) {
    @get:Rule
    val rule = ActivityTestRule(NightModeActivity::class.java, false, false)

    @Before
    fun setup() {
        // By default we'll set the night mode to NO, which allows us to make better
        // assumptions in the test below
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
        // Now launch the test activity
        rule.launchActivity(null)
    }

    @Test
    fun testLocalDayNightModeRecreatesActivity() {
        if (setMode != NightSetMode.LOCAL) {
            // This test is only applicable when using setLocalNightMode
            return
        }

        // Verify first that we're in day mode
        onView(withId(R.id.text_night_mode)).check(matches(withText(STRING_DAY)))

        // Now force the local night mode to be yes (aka night mode)
        setNightModeAndWaitForDestroy(rule, MODE_NIGHT_YES, setMode)

        // Assert that the new local night mode is returned
        assertEquals(MODE_NIGHT_YES, rule.activity.delegate.localNightMode)

        // Now check the text has changed, signifying that night resources are being used
        onView(withId(R.id.text_night_mode)).check(matches(withText(STRING_NIGHT)))
    }

    @Test
    fun testSwitchingYesDoesNotAffectApplication() {
        // This test is only useful when the dark system theme is not enabled
        if (!isSystemNightThemeEnabled(rule.activity)) {
            assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_NO,
                rule.activity.applicationContext.resources.configuration)

            // Force the night mode to be yes (aka night mode)
            setNightModeAndWaitForDestroy(rule, MODE_NIGHT_YES, setMode)

            assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_NO,
                rule.activity.applicationContext.resources.configuration)
        }
    }

    @Test
    fun testSwitchingYesToFollowSystem() {
        // This test is only useful when the dark system theme is enabled
        if (!isSystemNightThemeEnabled(rule.activity)) {
            // Now force the night mode to be YES, so we're in dark theme
            setNightModeAndWaitForDestroy(rule, MODE_NIGHT_YES, setMode)
            onView(withId(R.id.text_night_mode)).check(matches(withText(STRING_NIGHT)))

            // Now force the local night mode to be FOLLOW_SYSTEM (which we know is dark)
            setNightModeAndWaitForDestroy(rule, MODE_NIGHT_FOLLOW_SYSTEM, setMode)
            onView(withId(R.id.text_night_mode)).check(matches(withText(STRING_DAY)))
        }
    }

    @Test
    fun testSwitchingNoToFollowSystem() {
        // This test is only useful when the dark system theme is enabled
        if (isSystemNightThemeEnabled(rule.activity)) {
            // Now force the night mode to be NO, so we're in light theme
            setNightModeAndWaitForDestroy(rule, MODE_NIGHT_NO, setMode)
            onView(withId(R.id.text_night_mode)).check(matches(withText(STRING_DAY)))

            // Now force the night mode to be FOLLOW_SYSTEM (which we know is dark)
            setNightModeAndWaitForDestroy(rule, MODE_NIGHT_FOLLOW_SYSTEM, setMode)
            onView(withId(R.id.text_night_mode)).check(matches(withText(STRING_NIGHT)))
        }
    }

    @Test
    fun testColorConvertedDrawableChangesWithNightMode() {
        val activity = rule.activity
        val dayColor = ContextCompat.getColor(activity, R.color.color_sky_day)
        val nightColor = ContextCompat.getColor(activity, R.color.color_sky_night)

        // Loop through and switching from day to night and vice-versa multiple times. It needs
        // to be looped since the issue is with drawable caching, therefore we need to prime the
        // cache for the issue to happen
        for (i in 0 until 5) {
            // First force it to be night mode and assert the color
            setNightModeAndWaitForDestroy(rule, MODE_NIGHT_YES, setMode)
            onView(withId(R.id.view_background)).check(matches(isBackground(nightColor)))

            // Now force the local night mode to be no (aka day mode) and assert the color
            setNightModeAndWaitForDestroy(rule, MODE_NIGHT_NO, setMode)
            onView(withId(R.id.view_background)).check(matches(isBackground(dayColor)))
        }
    }

    @Test
    fun testNightModeAutoTimeRecreatesOnTimeChange() {
        // Create a fake TwilightManager and set it as the app instance
        val twilightManager = FakeTwilightManager(rule.activity)
        TwilightManager.setInstance(twilightManager)

        // Verify that we're currently in day mode
        onView(withId(R.id.text_night_mode)).check(matches(withText(STRING_DAY)))

        // Set MODE_NIGHT_AUTO so that we will change to night mode automatically
        setNightModeAndWait(rule, AppCompatDelegate.MODE_NIGHT_AUTO_TIME, setMode)
        val newDelegate = rule.activity.delegate as AppCompatDelegateImpl

        // Update the fake twilight manager to be in night and trigger a fake 'time' change
        rule.runOnUiThread {
            twilightManager.isNightForTest = true
            newDelegate.autoTimeNightModeManager.onChange()
        }

        // Now wait until the Activity is destroyed (thus recreated)
        waitForRecreation(rule)

        // Check that the text has changed, signifying that night resources are being used
        onView(withId(R.id.text_night_mode)).check(matches(withText(STRING_NIGHT)))
    }

    @Test
    fun testNightModeAutoTimeRecreatesOnResume() {
        // Create a fake TwilightManager and set it as the app instance
        val twilightManager = FakeTwilightManager(rule.activity)
        TwilightManager.setInstance(twilightManager)

        // Set MODE_NIGHT_AUTO_TIME so that we will change to night mode automatically
        setNightModeAndWait(rule, AppCompatDelegate.MODE_NIGHT_AUTO_TIME, setMode)

        // Verify that we're currently in day mode
        onView(withId(R.id.text_night_mode)).check(matches(withText(STRING_DAY)))

        val resumeCompleteLatch = CountDownLatch(1)

        rule.runOnUiThread {
            val activity = rule.activity
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            // Now fool the Activity into thinking that it has gone into the background
            instrumentation.callActivityOnPause(activity)
            instrumentation.callActivityOnStop(activity)

            // Now update the twilight manager while the Activity is in the 'background'
            twilightManager.isNightForTest = true

            // Now tell the Activity that it has gone into the foreground again
            instrumentation.callActivityOnStart(activity)
            instrumentation.callActivityOnResume(activity)

            resumeCompleteLatch.countDown()
        }

        resumeCompleteLatch.await()
        // finally check that the text has changed, signifying that night resources are being used
        onView(withId(R.id.text_night_mode)).check(matches(withText(STRING_NIGHT)))
    }

    @Test
    fun testOnNightModeChangedCalled() {
        val activity = rule.activity
        // Set local night mode to YES
        setNightModeAndWait(rule, MODE_NIGHT_YES, setMode)
        // Assert that the Activity received a new value
        assertEquals(MODE_NIGHT_YES, activity.lastNightModeAndReset)
    }

    @Test
    fun testDialogDoesNotOverrideActivityConfiguration() {
        // Set Activity local night mode to YES
        setNightModeAndWaitForDestroy(rule, MODE_NIGHT_YES, setMode)

        // Assert that the uiMode is as expected
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_YES, rule.activity)

        // Now show a AppCompatDialog
        rule.runOnUiThread {
            AppCompatDialog(rule.activity).show()
        }

        // Assert that the uiMode is unchanged
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_YES, rule.activity)
    }

    @Test
    fun testLoadingWebViewMaintainsConfiguration() {
        // Set night mode and wait for the new Activity
        setNightModeAndWaitForDestroy(rule, MODE_NIGHT_YES, setMode)

        // Assert that the context still has a night themed configuration
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_YES, rule.activity)

        // Now load a WebView into the Activity
        rule.runOnUiThread { WebView(rule.activity) }

        // Now assert that the context still has a night themed configuration
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_YES, rule.activity)
    }

    @Test
    fun testDialogCleansUpAutoMode() {
        rule.runOnUiThread {
            val dialog = AppCompatDialog(rule.activity)
            val delegate = dialog.delegate as AppCompatDelegateImpl

            // Set the local night mode of the Dialog to be an AUTO mode
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_AUTO_TIME

            // Now show and dismiss the dialog
            dialog.show()
            dialog.dismiss()

            // Assert that the auto manager is destroyed (not listening)
            assertFalse(delegate.autoTimeNightModeManager.isListening)
        }
    }

    @Test
    fun testOnConfigurationChangeNotCalled() {
        var activity = rule.activity
        // Set local night mode to YES
        setNightModeAndWait(rule, MODE_NIGHT_YES, setMode)
        // Assert that onConfigurationChange was not called on the original activity
        assertNull(activity.lastConfigurationChangeAndClear)

        activity = rule.activity
        // Set local night mode back to NO
        setNightModeAndWait(rule, MODE_NIGHT_NO, setMode)
        // Assert that onConfigurationChange was not called
        assertNull(activity.lastConfigurationChangeAndClear)
    }

    @After
    fun cleanup() {
        rule.finishActivity()
        // Reset the default night mode
        setNightModeAndWait(rule, MODE_NIGHT_NO, NightSetMode.DEFAULT)
    }

    private class FakeTwilightManager(context: Context) : TwilightManager(
        context,
        ContextCompat.getSystemService(context, LocationManager::class.java)!!
    ) {
        var isNightForTest: Boolean = false

        override fun isNight(): Boolean {
            return isNightForTest
        }
    }

    companion object {
        private const val STRING_DAY = "DAY"
        private const val STRING_NIGHT = "NIGHT"

        @Parameterized.Parameters
        @JvmStatic
        fun data() = listOf(NightSetMode.DEFAULT, NightSetMode.LOCAL)
    }
}
