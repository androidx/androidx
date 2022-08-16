/*
 * Copyright 2021 The Android Open Source Project
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

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.LifecycleOwnerUtils
import androidx.testutils.PollingCheck
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals

object LocalesUtils {
    private const val LOG_TAG = "LocalesUtils"

    /**
     * A test {@link LocaleListCompat} containing locales [CANADA_FRENCH, CHINESE].
     */
    var CUSTOM_LOCALE_LIST: LocaleListCompat = LocaleListCompat.getEmptyLocaleList()

    fun initCustomLocaleList() {
        if (Build.VERSION.SDK_INT >= 24) {
            CUSTOM_LOCALE_LIST = LocaleListCompat.forLanguageTags(
                Locale.CANADA_FRENCH.toLanguageTag() + "," +
                    Locale.CHINESE.toLanguageTag()
            )
        } else {
            CUSTOM_LOCALE_LIST = LocaleListCompat.create(Locale.CHINESE)
        }
    }

    fun assertConfigurationLocalesEquals(
        expectedLocales: LocaleListCompat,
        context: Context
    ) {
        assertConfigurationLocalesEquals(
            null,
            expectedLocales,
            context
        )
    }

    fun assertConfigurationLocalesEquals(
        message: String?,
        expectedLocales: LocaleListCompat,
        context: Context
    ) {
        assertConfigurationLocalesEquals(
            message,
            expectedLocales,
            context.resources.configuration
        )
    }

    fun assertConfigurationLocalesEquals(
        expectedLocales: LocaleListCompat,
        configuration: Configuration
    ) {
        assertConfigurationLocalesEquals(
            null,
            expectedLocales,
            configuration
        )
    }

    fun assertConfigurationLocalesEquals(
        message: String?,
        expectedLocales: LocaleListCompat,
        configuration: Configuration
    ) {
        if (Build.VERSION.SDK_INT >= 24) {
            assertEquals(
                message,
                expectedLocales.toLanguageTags(),
                configuration.locales.toLanguageTags()
            )
        } else {
            assertEquals(
                message,
                expectedLocales.get(0),
                @Suppress("DEPRECATION") configuration.locale
            )
        }
    }

    fun <T : AppCompatActivity> setLocalesAndWait(
        @Suppress("DEPRECATION") activityRule: androidx.test.rule.ActivityTestRule<T>,
        locales: LocaleListCompat
    ) {
        setLocalesAndWait(activityRule.activity, activityRule, locales)
    }

    fun <T : AppCompatActivity> setLocalesAndWait(
        activity: AppCompatActivity?,
        @Suppress("DEPRECATION") activityRule: androidx.test.rule.ActivityTestRule<T>,
        locales: LocaleListCompat
    ) {
        Log.d(
            LOG_TAG,
            "setLocalesAndWait on Activity: " + activity +
                " to locales: " + locales
        )

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        activityRule.runOnUiThread { setLocales(locales) }
        instrumentation.waitForIdleSync()
    }

    fun <T : AppCompatActivity> setLocalesAndWaitForRecreate(
        @Suppress("DEPRECATION") activityRule: androidx.test.rule.ActivityTestRule<T>,
        locales: LocaleListCompat
    ): T = setLocalesAndWaitForRecreate(activityRule.activity, locales)

    fun <T : AppCompatActivity> setLocalesAndWaitForRecreate(
        activity: T,
        locales: LocaleListCompat
    ): T {
        Log.d(
            LOG_TAG,
            "setLocalesAndWaitForRecreate on Activity: " + activity +
                " to mode: " + locales
        )

        LifecycleOwnerUtils.waitUntilState(activity, Lifecycle.State.RESUMED)

        // Screen rotation kicks off a lot of background work, so we might need to wait a bit
        // between the activity reaching RESUMED state and it actually being shown on screen.
        PollingCheck.waitFor {
            activity.hasWindowFocus()
        }
        assertNotEquals(locales, getLocales())

        // Now perform locales change and wait for the Activity to be recreated.
        return LifecycleOwnerUtils.waitForRecreation(activity) {
            setLocales(locales)
        }
    }

    fun setLocales(
        locales: LocaleListCompat
    ) = AppCompatDelegate.setApplicationLocales(locales)

    private fun getLocales(): LocaleListCompat = AppCompatDelegate.getApplicationLocales()
}