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

package androidx.appcompat.app

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.testutils.NightModeUtils.NightSetMode
import androidx.appcompat.testutils.NightModeUtils.assertConfigurationNightModeEquals
import androidx.appcompat.testutils.NightModeUtils.setNightMode
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class NightModeUiModeConfigChangesTestCase(private val setMode: NightSetMode) {
    private lateinit var scenario: ActivityScenario<NightModeUiModeConfigChangesActivity>

    @Before
    fun setup() {
        // By default we'll set the night mode to NO, which allows us to make better
        // assumptions in the tests below
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
        // Launch the test activity
        scenario = ActivityScenario.launch(NightModeUiModeConfigChangesActivity::class.java)
    }

    @Test
    fun testOnConfigurationChangeCalledWhileStarted() {
        scenario.moveToState(Lifecycle.State.RESUMED)

        // Set local night mode to YES
        scenario.onActivity { setNightMode(MODE_NIGHT_YES, it, setMode) }
        // Assert that the onConfigurationChange was called with a new correct config
        scenario.onActivity {
            val lastConfig = it.lastConfigurationChangeAndClear
            assertNotNull(lastConfig)
            assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_YES, lastConfig)
        }

        // Set local night mode back to NO
        scenario.onActivity { setNightMode(MODE_NIGHT_NO, it, setMode) }
        // Assert that the onConfigurationChange was called with a new correct config
        scenario.onActivity {
            val lastConfig = it.lastConfigurationChangeAndClear
            assertNotNull(lastConfig)
            assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_NO, lastConfig)
        }
    }

    @Test
    fun testOnConfigurationChangeNotCalledWhenNotStarted() {
        scenario.moveToState(Lifecycle.State.CREATED)
        // And clear any previous config changes
        scenario.onActivity { it.lastConfigurationChangeAndClear }

        // Set local night mode to YES
        scenario.onActivity { setNightMode(MODE_NIGHT_YES, it, setMode) }
        // Assert that the onConfigurationChange was not called with a new correct config
        scenario.onActivity {
            assertNull(it.lastConfigurationChangeAndClear)
        }

        // Set local night mode back to NO
        scenario.onActivity { setNightMode(MODE_NIGHT_NO, it, setMode) }
        // Assert that the onConfigurationChange was not called with a new correct config
        scenario.onActivity {
            assertNull(it.lastConfigurationChangeAndClear)
        }
    }

    @Test
    fun testResourcesUpdated() {
        // Set local night mode to YES
        scenario.onActivity { setNightMode(MODE_NIGHT_YES, it, setMode) }

        // Assert that the Activity resources configuration was updated
        assertConfigurationNightModeEquals(
            Configuration.UI_MODE_NIGHT_YES,
            scenario.withActivity { this }
        )

        // Set local night mode back to NO
        scenario.onActivity { setNightMode(MODE_NIGHT_NO, it, setMode) }

        // Assert that the Activity resources configuration was updated
        assertConfigurationNightModeEquals(
            Configuration.UI_MODE_NIGHT_NO,
            scenario.withActivity { this }
        )
    }

    @Test
    fun testOnNightModeChangedCalled() {
        // Set local night mode to YES
        scenario.onActivity { setNightMode(MODE_NIGHT_YES, it, setMode) }
        // Assert that the Activity received a new value
        assertEquals(MODE_NIGHT_YES, scenario.withActivity { lastNightModeAndReset })

        // Set local night mode to NO
        scenario.onActivity { setNightMode(MODE_NIGHT_NO, it, setMode) }
        // Assert that the Activity received a new value
        assertEquals(MODE_NIGHT_NO, scenario.withActivity { lastNightModeAndReset })
    }

    @After
    fun cleanup() {
        // Reset the default night mode
        scenario.onActivity { setNightMode(MODE_NIGHT_NO, it, NightSetMode.DEFAULT) }
        scenario.close()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(NightSetMode.DEFAULT, NightSetMode.LOCAL)
    }
}
