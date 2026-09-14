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

package androidx.xr.glimmer

import android.os.Build
import android.view.KeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.glimmer.testutils.createGlimmerRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class AlertDialogTest {

    @get:Rule(0)
    val rule = createComposeRule(config = ComposeUiTestConfig(inputMode = InputMode.Keyboard))

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @Test
    fun semantics() {
        rule.setGlimmerThemeContent {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = { Button(onClick = {}) { Text("OK") } },
                text = { Text("Dialog body text") },
            )
        }

        rule.onNode(isDialog()).assertExists()
    }

    @Test
    fun elementsDisplayed_withAllSlots() {
        rule.setGlimmerThemeContent {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = { Button(onClick = {}) { Text("Confirm") } },
                dismissButton = { Button(onClick = {}) { Text("Dismiss") } },
                icon = { Icon(FavoriteIcon, contentDescription = "Favorite") },
                title = { Text("Alert Title") },
                text = { Text("Alert message body") },
            )
        }

        rule.onNodeWithText("Alert Title").assertIsDisplayed()
        rule.onNodeWithText("Alert message body").assertIsDisplayed()
        rule.onNodeWithText("Confirm").assertIsDisplayed()
        rule.onNodeWithText("Dismiss").assertIsDisplayed()
        rule.onNodeWithContentDescription("Favorite").assertIsDisplayed()
    }

    @Test
    fun elementsDisplayed_minimalSlots() {
        rule.setGlimmerThemeContent {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = { Button(onClick = {}) { Text("OK") } },
                text = { Text("Minimal message body") },
            )
        }

        rule.onNodeWithText("Minimal message body").assertIsDisplayed()
        rule.onNodeWithText("OK").assertIsDisplayed()
    }

    @Test
    fun initialFocus_focusesConfirmButton() {
        rule.setGlimmerThemeContent {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {
                    Button(modifier = Modifier.testTag("confirm"), onClick = {}) { Text("Confirm") }
                },
                dismissButton = {
                    Button(modifier = Modifier.testTag("dismiss"), onClick = {}) { Text("Dismiss") }
                },
                text = { Text("Dialog body text") },
            )
        }

        rule.onNodeWithTag("confirm").assertIsFocused()
    }

    @Test
    fun initialFocus_singleButton_focusesConfirmButton() {
        rule.setGlimmerThemeContent {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {
                    Button(modifier = Modifier.testTag("confirm"), onClick = {}) { Text("OK") }
                },
                text = { Text("Dialog body text") },
            )
        }

        rule.onNodeWithTag("confirm").assertIsFocused()
    }

    @Test
    fun onDismissRequest_calledOnBackPress() {
        var dismissed = false
        rule.setGlimmerThemeContent {
            AlertDialog(
                onDismissRequest = {
                    assertThat(dismissed).isFalse()
                    dismissed = true
                },
                confirmButton = { Button(onClick = {}) { Text("OK") } },
                text = { Text("Dialog body text") },
            )
        }

        rule.waitForIdle()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)

        rule.runOnIdle { assertThat(dismissed).isTrue() }
    }
}
