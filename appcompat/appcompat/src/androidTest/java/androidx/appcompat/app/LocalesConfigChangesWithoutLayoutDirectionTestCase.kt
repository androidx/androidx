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

import androidx.appcompat.testutils.LocalesUtils
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import junit.framework.Assert.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test

@SdkSuppress(maxSdkVersion = 32)
class LocalesConfigChangesWithoutLayoutDirectionTestCase {
    private lateinit var scenario:
        ActivityScenario<LocalesConfigChangesActivityWithoutLayoutDirection>
    private var systemLocales = LocaleListCompat.getEmptyLocaleList()
    private var expectedLocales = LocaleListCompat.getEmptyLocaleList()

    @Before
    fun setup() {
        LocalesUtils.initCustomLocaleList()
        // By default we'll set the apps to use system locales, which allows us to make better
        // assumptions in the tests below.
        // Launch the test activity.
        scenario =
            ActivityScenario.launch(LocalesConfigChangesActivityWithoutLayoutDirection::class.java)
        scenario.onActivity {
            // Since no locales are applied as of now, current configuration will have system
            // locales.
            systemLocales = LocalesUpdateActivity.getConfigLocales(it.resources.configuration)
            // expected locales is an overlay of custom and system locales.
            expectedLocales =
                LocalesUpdateActivity.overlayCustomAndSystemLocales(
                    LocalesUtils.CUSTOM_LOCALE_LIST,
                    systemLocales,
                )
        }
    }

    @Test
    fun testOnConfigurationChangeNotCalledWhileStarted() {
        scenario.moveToState(Lifecycle.State.RESUMED)

        // Set locales to CUSTOM_LOCALE_LIST.
        scenario.onActivity { LocalesUtils.setLocales(LocalesUtils.CUSTOM_LOCALE_LIST) }
        // Assert that the onConfigurationChange was called with a new correct config.
        scenario.onActivity {
            // the call should not have reached the LocalesUpdateActivity.onConfigurationChange()
            // because the manifest entry for LocalesConfigChangesActivityWithoutLayoutDirection
            // only handles locale and not layoutDir.
            assertNull(it.lastConfigurationChangeAndClear)
            LocalesUtils.assertConfigurationLocalesEquals(
                expectedLocales,
                it.resources.configuration!!,
            )
        }
    }

    @Test
    fun testViewOnConfigurationChangeNotCalledWhileStarted() {
        scenario.moveToState(Lifecycle.State.RESUMED)

        // Set locales to CUSTOM_LOCALE_LIST.
        scenario.onActivity { LocalesUtils.setLocales(LocalesUtils.CUSTOM_LOCALE_LIST) }
        // Assert that the onConfigurationChange was called with a new correct config.
        scenario.onActivity {
            // the call should not have reached the LocalesUpdateActivity.onConfigurationChange()
            // because the manifest entry for LocalesConfigChangesActivityWithoutLayoutDirection
            // only handles locale and not layoutDir.
            assertNull(it.lastViewConfigurationChangeAndClear)
            LocalesUtils.assertConfigurationLocalesEquals(
                expectedLocales,
                it.resources.configuration!!,
            )
        }
    }

    @After
    fun teardown() {
        LocalesUpdateActivity.teardown()
    }
}
