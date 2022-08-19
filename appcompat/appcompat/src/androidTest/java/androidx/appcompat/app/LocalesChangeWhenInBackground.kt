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

package androidx.appcompat.app

import android.content.Intent
import androidx.appcompat.testutils.LocalesActivityTestRule
import androidx.appcompat.testutils.LocalesUtils.CUSTOM_LOCALE_LIST
import androidx.appcompat.testutils.LocalesUtils.assertConfigurationLocalesEquals
import androidx.appcompat.testutils.LocalesUtils.setLocalesAndWaitForRecreate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.LifecycleOwnerUtils.waitUntilState
import junit.framework.TestCase.assertNotSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
// TODO(b/218430372): Modify SdkSuppress annotation in tests for backward compatibility of
//  setApplicationLocales
@SdkSuppress(maxSdkVersion = 31)
class LocalesChangeWhenInBackground {
    @get:Rule
    val rule = LocalesActivityTestRule(LocalesUpdateActivity::class.java)
    private var systemLocales = LocaleListCompat.getEmptyLocaleList()

    @Before
    fun setUp() {
        // Since no locales are applied as of now, current configuration will have system
        // locales.
        systemLocales = LocalesUpdateActivity.getConfigLocales(
            rule.activity.resources.configuration)
    }

    @Test
    fun testLocalesChangeWhenInBackground() {

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val firstActivity = rule.activity
        assertConfigurationLocalesEquals(systemLocales, firstActivity)

        // Start a new Activity, so that the original Activity goes into the background
        val intent = Intent(firstActivity, AppCompatActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val secondActivity = instrumentation.startActivitySync(intent) as AppCompatActivity
        assertConfigurationLocalesEquals(systemLocales, secondActivity)

        // Now change the locales for the foreground activity
        val recreatedSecond = setLocalesAndWaitForRecreate(
            secondActivity,
            CUSTOM_LOCALE_LIST
        )

        // Now finish the foreground activity and wait until it is destroyed,
        // allowing the recreated activity to come to the foreground
        instrumentation.runOnMainSync { recreatedSecond.finish() }
        waitUntilState(recreatedSecond, Lifecycle.State.DESTROYED)

        // Assert that the recreated Activity becomes resumed
        instrumentation.waitForIdleSync()
        assertNotSame(rule.activity, firstActivity)
        waitUntilState(rule.activity, Lifecycle.State.RESUMED)
    }
}
