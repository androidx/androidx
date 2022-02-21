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

@file:Suppress("deprecation")

package androidx.appcompat.app

import android.content.Intent
import androidx.appcompat.testutils.LocalesActivityTestRule
import androidx.appcompat.testutils.LocalesUtils.CUSTOM_LOCALE_LIST
import androidx.appcompat.testutils.LocalesUtils.assertConfigurationLocalesEquals
import androidx.appcompat.testutils.LocalesUtils.setLocalesAndWaitForRecreate
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.Assert.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
// TODO(b/218430372): Modify SdkSuppress annotation in tests for backward compatibility of
// setApplicationLocales
@SdkSuppress(maxSdkVersion = 31)
class LocalesPersistTestCase() {
    @get:Rule
    val rule = LocalesActivityTestRule(LocalesUpdateActivity::class.java)
    private var systemLocales = LocaleListCompat.getEmptyLocaleList()
    private var expectedLocales = LocaleListCompat.getEmptyLocaleList()

    @Before
    fun setUp() {
        // Since no locales are applied as of now, current configuration will have system
        // locales.
        systemLocales = LocalesUpdateActivity.getConfigLocales(
            rule.activity.resources.configuration
        )
        expectedLocales = LocalesUpdateActivity.overlayCustomAndSystemLocales(
            CUSTOM_LOCALE_LIST, systemLocales
        )
    }

    /**
     * This test verifies that the locales persist in storage when a metadata entry for
     * "autoStoreLocale" is provided as an opt-in.
     * To replicate the scenario of app-startup a method resetStaticRequestedAndStoredLocales()
     * is called to clear out the static storage of locales. The flow of the test is:
     * setApplicationLocales is called on the firstActivity and it is recreated as recreatedFirst.
     * Now the locales must have been stored and the static storage would also hold these. Then
     * we clear out the static storage and create a new activity secondActivity by using an intent
     * and because the static storage was already clear the activity should sync locales from
     * storage and start up in the app-specific locales.
     */
    @Test
    fun testLocalesAppliedInNewActivityAfterStaticStorageCleared() {
        // mimics opting in to "autoStoreLocales"
        AppCompatDelegate.setIsAutoStoreLocalesOptedIn(true)

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val firstActivity = rule.activity
        assertConfigurationLocalesEquals(systemLocales, firstActivity)

        // Now change the locales for the activity
        val recreatedFirst = setLocalesAndWaitForRecreate(
            firstActivity,
            CUSTOM_LOCALE_LIST
        )

        assertConfigurationLocalesEquals(expectedLocales, recreatedFirst)

        // clear out static storage so that, when a new activity starts it syncs locales
        // from storage.
        AppCompatDelegate.resetStaticRequestedAndStoredLocales()
        // verify that the static locales were cleared out.
        assertNull(AppCompatDelegate.getRequestedAppLocales())

        // Start a new Activity, so that the original Activity goes into the background
        val intent = Intent(recreatedFirst, AppCompatActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val secondActivity = instrumentation.startActivitySync(intent) as AppCompatActivity

        // assert that the new activity started with the app-specific locales after reading them
        // from storage.
        assertConfigurationLocalesEquals(expectedLocales, secondActivity)
    }

    @Test
    fun testNewActivityCreatedWhenNoAppLocalesExist() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val firstActivity = rule.activity
        assertConfigurationLocalesEquals(systemLocales, firstActivity)

        // Start a new Activity, so that the original Activity goes into the background
        val intent = Intent(firstActivity, AppCompatActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val secondActivity = instrumentation.startActivitySync(intent) as AppCompatActivity

        // assert that the new activity started with the systemLocales.
        assertConfigurationLocalesEquals(systemLocales, secondActivity)
    }

    @After
    fun teardown() {
        //
        AppCompatDelegate.setIsAutoStoreLocalesOptedIn(false)
        rule.runOnUiThread {
            // clean-up
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
    }
}
