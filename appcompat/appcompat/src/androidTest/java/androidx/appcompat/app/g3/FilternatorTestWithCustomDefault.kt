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

package androidx.appcompat.app.g3

import androidx.appcompat.Orientation
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.withOrientation
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.testutils.LifecycleOwnerUtils.waitUntilState
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression test for b/236394773, adapted from GmsCore's own tests. The activity used for this
 * test has been modified to always call AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES).
 *
 * The primary purpose of this test is to ensure that the application configuration is not
 * accidentally modified when we modify the activity configuration.
 */
@Suppress("SameParameterValue")
@LargeTest
@SdkSuppress(minSdkVersion = 18) // UiDevice
@RunWith(AndroidJUnit4::class)
class FilternatorTestWithCustomDefault {
    @get:Rule
    val activityRule = ActivityScenarioRule(FilternatorActivityWithCustomDefault::class.java)

    private lateinit var uiDevice: UiDevice

    @Before
    fun setup() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    @After
    fun teardown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    @Test
    @Ignore("b/280630647")
    fun testConfigurationUpdatedOnLandscapeMode() {
        // Wait for the activity to fully start before rotating,
        // otherwise we won't receive onConfigurationChanged.
        val activity = activityRule.withActivity { this }
        waitUntilState(activity, Lifecycle.State.RESUMED)

        // Rotate and wait for the activity to check that
        // configuration has been properly updated.
        uiDevice.withOrientation(Orientation.LEFT) {
            FilternatorActivity.configurationLatch.await(5000, TimeUnit.MILLISECONDS)
            assertThat(FilternatorActivity.configurationException).isNull()
        }
    }
}
