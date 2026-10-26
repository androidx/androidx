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

package androidx.compose.material3

import androidx.compose.material3.samples.GroupedMenuSample
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies that the menu samples shown in the catalog save their state across recreation. */
@MediumTest
@RunWith(AndroidJUnit4::class)
class MenuSamplesTest {
    @get:Rule val rule = createComposeRule()

    private val restorationTester by lazy { StateRestorationTester(rule) }

    @Test
    fun groupedMenuSample_checkedState_isRestored() {
        restorationTester.setContent { GroupedMenuSample() }
        openMenu()
        rule.onNodeWithText("Edit").performClick()
        rule.onNodeWithText("Edit").assertIsSelected()
        rule.onNodeWithText("Settings").assertIsNotSelected()

        restorationTester.emulateSavedInstanceStateRestore()

        rule.onNodeWithText("Edit").assertIsDisplayed().assertIsSelected()
        rule.onNodeWithText("Settings").assertIsNotSelected()
    }

    private fun openMenu() {
        rule.onNodeWithContentDescription("Localized description").performClick()
        rule.waitForIdle()
    }
}
