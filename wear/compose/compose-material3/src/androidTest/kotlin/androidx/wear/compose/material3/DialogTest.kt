/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class DialogTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            Dialog(visible = true, modifier = Modifier.testTag(TEST_TAG), onDismissRequest = {}) {}
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun dialogContent_composedOnce() {
        var recomposeCounter = 0
        rule.setContentWithTheme {
            var visible by remember { mutableStateOf(false) }
            Button(modifier = Modifier.testTag(SHOW_BUTTON_TAG), onClick = { visible = true }) {}

            Dialog(
                visible = visible,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
            ) {
                recomposeCounter++
            }
        }
        rule.onNodeWithTag(SHOW_BUTTON_TAG).performClick()
        rule.waitForIdle()
        Assert.assertEquals(1, recomposeCounter)
    }

    @Test
    fun displays_content() {
        rule.setContentWithTheme {
            Dialog(visible = true, onDismissRequest = {}) {
                Text("Text", modifier = Modifier.testTag(TEST_TAG))
            }
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun supports_swipeToDismiss() {
        var dismissCounter = 0
        rule.setContentWithTheme {
            var visible by remember { mutableStateOf(true) }
            Dialog(
                visible = visible,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {
                    visible = false
                    dismissCounter++
                },
            ) {}
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun onDismissRequest_not_called_when_hidden() {
        val visible = mutableStateOf(true)
        var dismissCounter = 0
        rule.setContentWithTheme {
            Dialog(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = { dismissCounter++ },
                visible = visible.value,
            ) {}
        }
        rule.waitForIdle()
        visible.value = false
        rule.waitUntilDoesNotExist(hasTestTag(TEST_TAG))
        Assert.assertEquals(0, dismissCounter)
    }

    @Test
    fun hides_dialog_when_visible_false() {
        rule.setContentWithTheme {
            Dialog(modifier = Modifier.testTag(TEST_TAG), onDismissRequest = {}, visible = false) {}
        }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun shrink_background_when_dialog_is_shown() {
        val scaffoldState = ScaffoldState()
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalScaffoldState provides scaffoldState) {
                var visible by remember { mutableStateOf(false) }
                Button(
                    modifier = Modifier.testTag(SHOW_BUTTON_TAG),
                    onClick = { visible = true },
                ) {}

                Dialog(
                    visible = visible,
                    modifier = Modifier.testTag(TEST_TAG),
                    onDismissRequest = {},
                ) {}
            }
        }
        rule.onNodeWithTag(SHOW_BUTTON_TAG).performClick()
        rule.waitForIdle()
        assert(scaffoldState.parentScale.floatValue < 1f)
    }

    @Test
    fun expand_background_when_dialog_is_hidden() {
        val scaffoldState = ScaffoldState()
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalScaffoldState provides scaffoldState) {
                var visible by remember { mutableStateOf(true) }
                Button(
                    modifier = Modifier.testTag(SHOW_BUTTON_TAG),
                    onClick = { visible = false },
                ) {}

                Dialog(
                    visible = visible,
                    modifier = Modifier.testTag(TEST_TAG),
                    onDismissRequest = {},
                ) {}
            }
        }
        rule.onNodeWithTag(SHOW_BUTTON_TAG).performClick()
        rule.waitForIdle()
        Assert.assertEquals(scaffoldState.parentScale.floatValue, 1f, 0.01f)
    }

    @Test
    fun expand_background_when_dialog_is_removed() {
        val scaffoldState = ScaffoldState()
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalScaffoldState provides scaffoldState) {
                var visible by remember { mutableStateOf(true) }
                Button(
                    modifier = Modifier.testTag(SHOW_BUTTON_TAG),
                    onClick = { visible = false },
                ) {}

                if (visible) {
                    Dialog(
                        visible = visible,
                        modifier = Modifier.testTag(TEST_TAG),
                        onDismissRequest = {},
                    ) {}
                }
            }
        }
        rule.onNodeWithTag(SHOW_BUTTON_TAG).performClick()
        rule.waitForIdle()
        Assert.assertEquals(scaffoldState.parentScale.floatValue, 1f, 0.01f)
    }

    @Test
    fun dialogProperties_whenStatusBarEnabled_forcesEdgeToEdgeAndPreservesProperties() {
        var window: Window? = null
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                Dialog(
                    visible = true,
                    onDismissRequest = {},
                    properties =
                        DialogProperties(
                            windowTitle = "CustomTitle",
                            decorFitsSystemWindows = true,
                            usePlatformDefaultWidth = true,
                        ),
                ) {
                    var parent = LocalView.current.parent
                    while (parent != null && parent !is DialogWindowProvider) {
                        parent = parent.parent
                    }
                    window = (parent as? DialogWindowProvider)?.window
                }
            }
        }
        rule.waitForIdle()
        Assert.assertNotNull(window)
        Assert.assertEquals("CustomTitle", window!!.attributes.title)
        val flags = window!!.attributes.flags
        Assert.assertTrue(
            "Expected FLAG_LAYOUT_IN_SCREEN to be set when decorFitsSystemWindows is forced to false",
            (flags and WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN) != 0,
        )
    }

    @Test
    fun dialogProperties_whenStatusBarDisabled_preservesPassedProperties() {
        var window: Window? = null
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides false) {
                Dialog(
                    visible = true,
                    onDismissRequest = {},
                    properties =
                        DialogProperties(
                            windowTitle = "CustomTitle",
                            decorFitsSystemWindows = true,
                            usePlatformDefaultWidth = true,
                        ),
                ) {
                    var parent = LocalView.current.parent
                    while (parent != null && parent !is DialogWindowProvider) {
                        parent = parent.parent
                    }
                    window = (parent as? DialogWindowProvider)?.window
                }
            }
        }
        rule.waitForIdle()
        Assert.assertNotNull(window)
        Assert.assertEquals("CustomTitle", window!!.attributes.title)
        val flags = window!!.attributes.flags
        Assert.assertTrue(
            "Expected FLAG_LAYOUT_IN_SCREEN not to be set when decorFitsSystemWindows is true",
            (flags and WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN) == 0,
        )
    }
}

private const val SHOW_BUTTON_TAG = "show-button"

@Suppress("UNCHECKED_CAST")
private val LocalStatusBarEnabledForTest: ProvidableCompositionLocal<Boolean>
    get() = LocalStatusBarEnabled as ProvidableCompositionLocal<Boolean>
