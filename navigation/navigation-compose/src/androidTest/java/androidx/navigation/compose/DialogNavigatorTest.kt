/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.navigation.compose

import androidx.compose.material.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.testing.TestNavigatorState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DialogNavigatorTest {
    @get:Rule
    val rule = createComposeRule()

    private val defaultText = "dialogText"

    @Test
    fun testDialogs() {
        val navigator = DialogNavigator()
        val navigatorState = TestNavigatorState()
        navigator.onAttach(navigatorState)

        rule.setContent {
            navigator.Dialogs()
        }

        rule.onNodeWithText(defaultText).assertDoesNotExist()

        val dialog = DialogNavigator.Destination(navigator) {
            Text(defaultText)
        }
        val entry = navigatorState.createBackStackEntry(dialog, null)
        navigator.navigate(listOf(entry), null, null)

        rule.onNodeWithText(defaultText).assertIsDisplayed()
    }

    @Test
    fun testPopDismissesDialog() {
        val navigator = DialogNavigator()
        val navigatorState = TestNavigatorState()
        navigator.onAttach(navigatorState)
        val dialog = DialogNavigator.Destination(navigator) {
            Text(defaultText)
        }
        val entry = navigatorState.createBackStackEntry(dialog, null)
        navigator.navigate(listOf(entry), null, null)

        rule.setContent {
            navigator.Dialogs()
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        navigator.popBackStack(entry, false)

        rule.onNodeWithText(defaultText).assertDoesNotExist()
    }
}
