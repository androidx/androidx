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

import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.app.NightModeCustomConfigurationActivity.CUSTOM_FONT_SCALE
import androidx.appcompat.testutils.NightModeUtils
import androidx.appcompat.testutils.NightModeUtils.NightSetMode
import androidx.appcompat.testutils.NightModeUtils.setNightModeAndWaitForDestroy
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class NightModeCustomFontScaleTestCase(private val setMode: NightSetMode) {
    @get:Rule
    val rule = ActivityTestRule(NightModeCustomConfigurationActivity::class.java, false, false)

    @Before
    fun setup() {
        // By default we'll set the night mode to NO, which allows us to make better
        // assumptions in the tests below
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
        // Launch the test activity
        rule.launchActivity(null)
    }

    @Test
    fun testFontScaleIsMaintained() {
        // Check that the custom configuration properties are maintained
        val config = rule.activity.resources.configuration
        assertEquals(CUSTOM_FONT_SCALE, config.fontScale)
    }

    @Test
    fun testFontScaleIsMaintainedInDarkTheme() {
        // Set local night mode to YES
        setNightModeAndWaitForDestroy(rule, MODE_NIGHT_YES, setMode)

        // Check that the custom configuration properties are maintained
        val config = rule.activity.resources.configuration
        assertEquals(CUSTOM_FONT_SCALE, config.fontScale)
    }

    @After
    fun cleanup() {
        rule.finishActivity()
        // Reset the default night mode
        NightModeUtils.setNightModeAndWait(rule, MODE_NIGHT_NO, NightSetMode.DEFAULT)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(NightSetMode.DEFAULT, NightSetMode.LOCAL)
    }
}
