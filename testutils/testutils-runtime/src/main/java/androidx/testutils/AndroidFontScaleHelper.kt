/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.testutils

import android.app.Activity
import android.os.Build
import android.provider.Settings
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assume.assumeTrue

/**
 * Helper utils to change the system font scale.
 */
object AndroidFontScaleHelper {
    /**
     * Returns the current global font scale for all apps via the system Settings
     */
    fun getSystemFontScale(): Float {
        return Settings.System.getFloat(
            InstrumentationRegistry.getInstrumentation().context.contentResolver,
            Settings.System.FONT_SCALE,
            /* Must default to 1 because when a test is first spun up it's not initialized. */
            /* def= */ 1f
        )
    }

    /**
     * Sets the global font scale for all apps via the system Settings.
     *
     * Note this only works on [Build.VERSION_CODES.Q]+. Earlier versions will force the test to be
     * skipped.
     *
     * @param fontScale the desired font scale
     * @param activityScenario scenario that provides an Activity under test. The activity will be
     *  continually polled until the font scale changes in that Activity's configuration. (Usually
     *  you will pass in <code>rule.activityRule.scenario</code>).
     */
    fun <A : Activity> setSystemFontScale(
        fontScale: Float,
        activityScenario: ActivityScenario<A>
    ) {
        if (Build.VERSION.SDK_INT >= 29) {
            Api29Impl.setSystemFontScale(fontScale, activityScenario)
        } else {
            assumeTrue("SDK must be >= Q to setSystemFontScale()", false)
        }
    }

    /**
     * Resets the global font scale for all apps to the default via the system Settings
     *
     * {@see #setSystemFontScale}
     */
    fun <A : Activity> resetSystemFontScale(
        activityScenario: ActivityScenario<A>
    ) {
        // TODO(b/279083734): would use Settings.System.resetToDefaults() if it existed
        setSystemFontScale(1f, activityScenario)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private object Api29Impl {
        @JvmStatic
        @DoNotInline
        fun <A : Activity> setSystemFontScale(
            fontScale: Float,
            activityScenario: ActivityScenario<A>
        ) {
            val instrumentationContext = InstrumentationRegistry.getInstrumentation().context

            invokeWithShellPermissions {
                Settings.System.putFloat(
                    instrumentationContext.contentResolver,
                    Settings.System.FONT_SCALE,
                    fontScale
                )
            }

            PollingCheck.waitFor(/* timeout= */ 5000) {
                val isActivityAtCorrectScale = AtomicBoolean(false)
                activityScenario.onActivity { activity ->
                    isActivityAtCorrectScale.set(
                        activity.resources.configuration.fontScale == fontScale
                    )
                }
                isActivityAtCorrectScale.get() &&
                    // For some reason we need both these checks otherwise other tests get messed up
                    // since the scale doesn't get reset in teardown.
                    getSystemFontScale() == fontScale
            }
        }

        /**
         * Runs the given function as root, with all shell permissions granted.
         */
        @JvmStatic
        @DoNotInline
        private fun invokeWithShellPermissions(runnable: () -> Unit) {
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
            try {
                uiAutomation.adoptShellPermissionIdentity()
                runnable()
            } finally {
                uiAutomation.dropShellPermissionIdentity()
            }
        }
    }
}
