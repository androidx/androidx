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

package androidx.appcompat.testutils

import android.app.UiModeManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.LifecycleOwnerUtils
import androidx.testutils.PollingCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals

object NightModeUtils {
    private const val LOG_TAG = "NightModeUtils"

    enum class NightSetMode {
        /**
         * Set the night mode using [AppCompatDelegate.setDefaultNightMode]
         */
        DEFAULT,

        /**
         * Set the night mode using [AppCompatDelegate.setLocalNightMode]
         */
        LOCAL
    }

    fun assertConfigurationNightModeEquals(
        expectedNightMode: Int,
        context: Context
    ) {
        assertConfigurationNightModeEquals(
            null,
            expectedNightMode,
            context
        )
    }

    fun assertConfigurationNightModeEquals(
        message: String?,
        expectedNightMode: Int,
        context: Context
    ) {
        assertConfigurationNightModeEquals(
            message,
            expectedNightMode,
            context.resources.configuration
        )
    }

    fun assertConfigurationNightModeEquals(
        expectedNightMode: Int,
        configuration: Configuration
    ) {
        assertConfigurationNightModeEquals(
            null,
            expectedNightMode,
            configuration
        )
    }

    fun assertConfigurationNightModeEquals(
        message: String?,
        expectedNightMode: Int,
        configuration: Configuration
    ) {
        assertEquals(
            message,
            expectedNightMode.toLong(),
            (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK).toLong()
        )
    }

    fun <T : AppCompatActivity> setNightModeAndWait(
        @Suppress("DEPRECATION") activityRule: androidx.test.rule.ActivityTestRule<T>,
        @NightMode nightMode: Int,
        setMode: NightSetMode
    ) {
        setNightModeAndWait(activityRule.activity, activityRule, nightMode, setMode)
    }

    fun <T : AppCompatActivity> setNightModeAndWait(
        activity: AppCompatActivity?,
        @Suppress("DEPRECATION") activityRule: androidx.test.rule.ActivityTestRule<T>,
        @NightMode nightMode: Int,
        setMode: NightSetMode
    ) {
        Log.d(
            LOG_TAG,
            "setNightModeAndWait on Activity: " + activity +
                " to mode: " + nightMode +
                " using set mode: " + setMode
        )

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        activityRule.runOnUiThread { setNightMode(nightMode, activity, setMode) }
        instrumentation.waitForIdleSync()
    }

    fun <T : AppCompatActivity> setNightModeAndWaitForRecreate(
        @Suppress("DEPRECATION") activityRule: androidx.test.rule.ActivityTestRule<T>,
        @NightMode nightMode: Int,
        setMode: NightSetMode
    ): T = setNightModeAndWaitForRecreate(activityRule.activity, nightMode, setMode)

    fun <T : AppCompatActivity> setNightModeAndWaitForRecreate(
        activity: T,
        @NightMode nightMode: Int,
        setMode: NightSetMode
    ): T {
        Log.d(
            LOG_TAG,
            "setNightModeAndWaitForRecreate on Activity: " + activity +
                " to mode: " + nightMode +
                " using set mode: " + setMode
        )

        LifecycleOwnerUtils.waitUntilState(activity, Lifecycle.State.RESUMED)

        // Screen rotation kicks off a lot of background work, so we might need to wait a bit
        // between the activity reaching RESUMED state and it actually being shown on screen.
        PollingCheck.waitFor {
            activity.hasWindowFocus()
        }
        assertNotEquals(nightMode, getNightMode(activity, setMode))

        // Now perform night mode change wait for the Activity to be recreated
        return LifecycleOwnerUtils.waitForRecreation(activity) {
            setNightMode(nightMode, activity, setMode)
        }
    }

    fun <T : AppCompatActivity> rotateAndWaitForRecreate(activity: T): T {
        Log.e(LOG_TAG, "request rotate")
        LifecycleOwnerUtils.waitUntilState(activity, Lifecycle.State.RESUMED)

        // Now perform rotation and wait for the Activity to be recreated
        return LifecycleOwnerUtils.waitForRecreation(activity) {
            Log.e(LOG_TAG, "request rotate on ui thread")
            if (activity.resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE
            ) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            Log.e(LOG_TAG, "request rotate to " + activity.requestedOrientation)
        }
    }

    fun <T : AppCompatActivity> resetRotateAndWaitForRecreate(activity: T) {
        LifecycleOwnerUtils.waitUntilState(activity, Lifecycle.State.RESUMED)

        LifecycleOwnerUtils.waitForRecreation(activity) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    fun isSystemNightThemeEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return manager.nightMode == UiModeManager.MODE_NIGHT_YES
    }

    fun setNightMode(
        @NightMode nightMode: Int,
        activity: AppCompatActivity?,
        setMode: NightSetMode
    ) = when (setMode) {
        NightSetMode.DEFAULT -> AppCompatDelegate.setDefaultNightMode(nightMode)
        NightSetMode.LOCAL ->
            if (Build.VERSION.SDK_INT >= 17) {
                activity!!.delegate.localNightMode = nightMode
            } else {
                throw Exception("Local night mode is not supported on SDK_INT < 17")
            }
    }

    @NightMode
    fun getNightMode(
        activity: AppCompatActivity?,
        setMode: NightSetMode
    ): Int = when (setMode) {
        NightSetMode.DEFAULT -> AppCompatDelegate.getDefaultNightMode()
        NightSetMode.LOCAL -> activity!!.delegate.localNightMode
    }
}
