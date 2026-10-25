/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.text.vertical.testapp

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertValueEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the floating button and the saved state of [StyleColorsToggle]. */
@MediumTest
@RunWith(AndroidJUnit4::class)
class StyleColorsToggleTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun styleColorsToggle_startsOff_andClickTurnsItOn() {
        var styleColorsEnabled: Boolean? = null
        rule.setContent { StyleColorsToggle { styleColorsEnabled = it } }

        toggleButton().assertValueEquals("Off")
        rule.runOnIdle { assertThat(styleColorsEnabled).isFalse() }

        toggleButton().performClick()

        toggleButton().assertValueEquals("On")
        rule.runOnIdle { assertThat(styleColorsEnabled).isTrue() }
    }

    @Test
    fun styleColorsToggle_initiallyEnabled_startsOn_andClickTurnsItOff() {
        var styleColorsEnabled: Boolean? = null
        rule.setContent { StyleColorsToggle(initiallyEnabled = true) { styleColorsEnabled = it } }

        toggleButton().assertValueEquals("On")
        rule.runOnIdle { assertThat(styleColorsEnabled).isTrue() }

        toggleButton().performClick()

        toggleButton().assertValueEquals("Off")
        rule.runOnIdle { assertThat(styleColorsEnabled).isFalse() }
    }

    @Test
    fun styleColorsToggle_keepsStateAcrossRecreation() {
        val restorationTester = StateRestorationTester(rule)
        var styleColorsEnabled: Boolean? = null
        restorationTester.setContent { StyleColorsToggle { styleColorsEnabled = it } }
        toggleButton().performClick()

        restorationTester.emulateSavedInstanceStateRestore()

        toggleButton().assertValueEquals("On")
        rule.runOnIdle { assertThat(styleColorsEnabled).isTrue() }
    }

    @Test
    fun styleColorsDemo_startsWithStyleColorsOn() {
        rule.setContent { StyleColorsDemo() }

        toggleButton().assertValueEquals("On")
    }

    /** Returns the floating button of the toggle. */
    private fun toggleButton(): SemanticsNodeInteraction =
        rule.onNodeWithTag(STYLE_COLORS_TOGGLE_TAG)
}
