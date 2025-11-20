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

import androidx.appcompat.testutils.LocalesUtils
import androidx.appcompat.testutils.LocalesUtils.CUSTOM_LOCALE_LIST
import androidx.appcompat.testutils.LocalesUtils.assertConfigurationLocalesEquals
import androidx.appcompat.testutils.LocalesUtils.setLocales
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@LargeTest
@SdkSuppress(maxSdkVersion = 32)
class LocalesConfigChangesTestCase() {
    private lateinit var scenario: ActivityScenario<LocalesConfigChangesActivity>
    private var systemLocales = LocaleListCompat.getEmptyLocaleList()
    private var expectedLocales = LocaleListCompat.getEmptyLocaleList()

    @Before
    fun setup() {
        LocalesUtils.initCustomLocaleList()
        // By default we'll set the apps to use system locales, which allows us to make better
        // assumptions in the tests below.
        // Launch the test activity.
        scenario = ActivityScenario.launch(LocalesConfigChangesActivity::class.java)
        scenario.onActivity {
            // Since no locales are applied as of now, current configuration will have system
            // locales.
            systemLocales = LocalesUpdateActivity.getConfigLocales(it.resources.configuration)
            // expected locales is an overlay of custom and system locales.
            expectedLocales =
                LocalesUpdateActivity.overlayCustomAndSystemLocales(
                    CUSTOM_LOCALE_LIST,
                    systemLocales,
                )
        }
    }

    @Test
    fun testOnConfigurationChangeCalledWhileStarted() {
        scenario.moveToState(Lifecycle.State.RESUMED)

        // Set locales to CUSTOM_LOCALE_LIST.
        scenario.onActivity { setLocales(CUSTOM_LOCALE_LIST) }
        // Assert that the onConfigurationChange was called with a new correct config.
        scenario.onActivity {
            val lastConfig = it.lastConfigurationChangeAndClear
            assertConfigurationLocalesEquals(expectedLocales, lastConfig!!)
        }

        // Set locales back to system locales.
        scenario.onActivity { setLocales(LocaleListCompat.getEmptyLocaleList()) }
        // Assert that the onConfigurationChange was called with a new correct config.
        scenario.onActivity {
            val lastConfig = it.lastConfigurationChangeAndClear
            assertConfigurationLocalesEquals(systemLocales, lastConfig!!)
        }
    }

    @Test
    fun testOnConfigurationChangeCalledWhileStopped() {
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.moveToState(Lifecycle.State.CREATED)

        // Set locales to CUSTOM_LOCALE_LIST.
        scenario.onActivity { setLocales(CUSTOM_LOCALE_LIST) }
        // Assert that the onConfigurationChange was called with a new correct config.
        scenario.onActivity {
            val lastConfig = it.lastConfigurationChangeAndClear
            assertConfigurationLocalesEquals(expectedLocales, lastConfig!!)
        }

        // Set locales back to system locales.
        scenario.onActivity { setLocales(LocaleListCompat.getEmptyLocaleList()) }
        // Assert that the onConfigurationChange was called with a new correct config.
        scenario.onActivity {
            val lastConfig = it.lastConfigurationChangeAndClear
            assertConfigurationLocalesEquals(systemLocales, lastConfig!!)
        }
    }

    @Test
    fun testViewOnConfigurationChangeCalledWhileStarted() {
        scenario.moveToState(Lifecycle.State.RESUMED)

        // Set locales to CUSTOM_LOCALE_LIST.
        scenario.onActivity { setLocales(CUSTOM_LOCALE_LIST) }
        // Assert that the onConfigurationChange was called with a new correct config.
        scenario.onActivity {
            val lastConfig = it.lastViewConfigurationChangeAndClear
            assertConfigurationLocalesEquals(expectedLocales, lastConfig!!)
        }

        // Set locales back to system locales.
        scenario.onActivity { setLocales(LocaleListCompat.getEmptyLocaleList()) }
        // Assert that the onConfigurationChange was called with a new correct config.
        scenario.onActivity {
            val lastConfig = it.lastViewConfigurationChangeAndClear
            assertConfigurationLocalesEquals(systemLocales, lastConfig!!)
        }
    }

    @Test
    fun testViewOnConfigurationChangeCalledWhileStopped() {
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.moveToState(Lifecycle.State.CREATED)

        // Set locales to CUSTOM_LOCALE_LIST.
        scenario.onActivity { setLocales(CUSTOM_LOCALE_LIST) }
        // Assert that the onConfigurationChange was called with a new correct config.
        scenario.onActivity {
            val lastConfig = it.lastViewConfigurationChangeAndClear
            assertConfigurationLocalesEquals(expectedLocales, lastConfig!!)
        }

        // Set locales back to system locales.
        scenario.onActivity { setLocales(LocaleListCompat.getEmptyLocaleList()) }
        // Assert that the onConfigurationChange was called with a new correct config.
        scenario.onActivity {
            val lastConfig = it.lastViewConfigurationChangeAndClear
            assertConfigurationLocalesEquals(systemLocales, lastConfig!!)
        }
    }

    @Test
    fun testOnConfigurationChangeNotCalledWhileDestroyed() {
        scenario.moveToState(Lifecycle.State.RESUMED)

        lateinit var activity: LocalesConfigChangesActivity
        scenario.onActivity { activity = it }

        scenario.moveToState(Lifecycle.State.DESTROYED)

        // Clear any previous config changes.
        activity.lastConfigurationChangeAndClear

        // Set locales to CUSTOM_LOCALE_LIST.
        setLocales(CUSTOM_LOCALE_LIST)
        // Assert that the onConfigurationChange was not called with a new correct config.
        assertNull(activity.lastConfigurationChangeAndClear)

        // Set locales back to system locales.
        setLocales(LocaleListCompat.getEmptyLocaleList())
        // Assert that the onConfigurationChange was not called with a new correct config.
        assertNull(activity.lastConfigurationChangeAndClear)
    }

    @Test
    fun testResourcesUpdated() {
        // Set locales to CUSTOM_LOCALE_LIST.
        scenario.onActivity { setLocales(CUSTOM_LOCALE_LIST) }

        // Assert that the Activity resources configuration was updated.
        assertConfigurationLocalesEquals(expectedLocales, scenario.withActivity { this })

        // Set locales back to system locales.
        scenario.onActivity { setLocales(LocaleListCompat.getEmptyLocaleList()) }

        // Assert that the Activity resources configuration was updated.
        assertConfigurationLocalesEquals(systemLocales, scenario.withActivity { this })
    }

    @Test
    fun testOnLocalesChangedCalled() {
        // Set locales to CUSTOM_LOCALE_LIST.
        scenario.onActivity { setLocales(CUSTOM_LOCALE_LIST) }
        // Assert that the Activity received a new value.
        assertEquals(expectedLocales, scenario.withActivity { lastLocalesAndReset })

        // Set locales back to system locales.
        scenario.onActivity { setLocales(LocaleListCompat.getEmptyLocaleList()) }
        // Assert that the Activity received a new value.
        assertEquals(systemLocales, scenario.withActivity { lastLocalesAndReset })
    }

    @After
    fun cleanup() {
        // Reset the system locales.
        if (scenario.state != Lifecycle.State.DESTROYED) {
            scenario.onActivity { setLocales(LocaleListCompat.getEmptyLocaleList()) }
        }
        LocalesUpdateActivity.teardown()
        scenario.close()
    }
}
