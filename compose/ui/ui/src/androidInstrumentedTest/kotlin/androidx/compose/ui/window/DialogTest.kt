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
package androidx.compose.ui.window

import android.content.res.Configuration
import android.view.KeyEvent
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DialogTest {
    @get:Rule val rule = createComposeRule()

    private val defaultText = "dialogText"
    private val testTag = "tag"
    private lateinit var dispatcher: OnBackPressedDispatcher

    @Test
    fun dialogTest_isShowingContent() {
        setupDialogTest(closeDialogOnDismiss = false)
        rule.onNodeWithTag(testTag).assertIsDisplayed()
    }

    @Test
    fun dialogTest_isNotDismissed_whenClicked() {
        var clickCount = 0
        setupDialogTest { DefaultDialogContent(Modifier.clickable { clickCount++ }) }

        assertThat(clickCount).isEqualTo(0)
        val interaction = rule.onNodeWithTag(testTag)
        interaction.assertIsDisplayed()

        // Click inside the dialog
        interaction.performClick()

        // Check that the Clickable was pressed and the Dialog is still visible.
        interaction.assertIsDisplayed()
        assertThat(clickCount).isEqualTo(1)
    }

    @Test
    fun dialogTest_isDismissed_whenSpecified() {
        setupDialogTest()
        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()

        clickOutsideDialog()
        textInteraction.assertDoesNotExist()
    }

    @Test
    fun dialogTest_isNotDismissed_whenNotSpecified() {
        setupDialogTest(closeDialogOnDismiss = false)
        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()

        clickOutsideDialog()
        // The Dialog should still be visible
        textInteraction.assertIsDisplayed()
    }

    @Test
    fun dialogTest_isNotDismissed_whenDismissOnClickOutsideIsFalse() {
        setupDialogTest(dialogProperties = DialogProperties(dismissOnClickOutside = false))
        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()

        clickOutsideDialog()
        // The Dialog should still be visible
        textInteraction.assertIsDisplayed()
    }

    @Test
    fun dialogTest_isDismissed_whenSpecified_backButtonPressed() {
        setupDialogTest()
        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()

        pressBackViaKey()
        textInteraction.assertDoesNotExist()
    }

    @Test
    fun dialogTest_isDismissed_whenSpecified_backDispatched() {
        setupDialogTest()
        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()

        dispatchBackButton()
        textInteraction.assertDoesNotExist()
    }

    @Test
    fun dialogTest_isNotDismissed_whenNotSpecified_backButtonPressed() {
        setupDialogTest(closeDialogOnDismiss = false)
        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()

        pressBackViaKey()
        // The Dialog should still be visible
        textInteraction.assertIsDisplayed()
    }

    @Test
    fun dialogTest_isNotDismissed_whenNotSpecified_backDispatched() {
        setupDialogTest(closeDialogOnDismiss = false)
        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()

        dispatchBackButton()
        // The Dialog should still be visible
        textInteraction.assertIsDisplayed()
    }

    @Test
    fun dialogTest_isNotDismissed_whenDismissOnBackPressIsFalse_backButtonPressed() {
        setupDialogTest(dialogProperties = DialogProperties(dismissOnBackPress = false))
        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()

        pressBackViaKey()
        // The Dialog should still be visible
        textInteraction.assertIsDisplayed()
    }

    @Test
    fun dialogTest_isNotDismissed_whenDismissOnBackPressIsFalse_backDispatched() {
        setupDialogTest(dialogProperties = DialogProperties(dismissOnBackPress = false))
        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()

        dispatchBackButton()
        // The Dialog should still be visible
        textInteraction.assertIsDisplayed()
    }

    @Test
    fun dialogTest_backHandler_isCalled_backButtonPressed() {
        var clickCount = 0
        setupDialogTest(closeDialogOnDismiss = false) {
            BackHandler { clickCount++ }
            DefaultDialogContent()
        }

        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()
        assertThat(clickCount).isEqualTo(0)

        pressBackViaKey()
        textInteraction.assertIsDisplayed()
        assertThat(clickCount).isEqualTo(1)
    }

    @Test
    fun dialogTest_backHandler_isCalled_backDispatched() {
        var clickCount = 0
        setupDialogTest(closeDialogOnDismiss = false) {
            BackHandler { clickCount++ }
            DefaultDialogContent()
        }

        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()
        assertThat(clickCount).isEqualTo(0)

        dispatchBackButton()
        textInteraction.assertIsDisplayed()
        assertThat(clickCount).isEqualTo(1)
    }

    @Test
    fun dialogTest_isDismissed_escapePressed() {
        setupDialogTest()

        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()

        pressEscape()
        textInteraction.assertDoesNotExist()
    }

    @Test
    fun dialogTest_isNotDismissed_whenNotSpecified_escapePressed() {
        setupDialogTest(closeDialogOnDismiss = false)

        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()

        pressEscape()
        textInteraction.assertIsDisplayed()
    }

    @Test
    fun dialog_preservesCompositionLocals() {
        val compositionLocal = compositionLocalOf<Float> { error("unset") }
        var value = 0f
        rule.setContent {
            CompositionLocalProvider(compositionLocal provides 1f) {
                Dialog(onDismissRequest = {}) { value = compositionLocal.current }
            }
        }
        rule.runOnIdle { assertEquals(1f, value) }
    }

    @Test
    fun canFillScreenWidth_dependingOnProperty() {
        var box1Width = 0
        var box2Width = 0
        lateinit var configuration: Configuration
        rule.setContent {
            configuration = LocalConfiguration.current
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(Modifier.fillMaxSize().onSizeChanged { box1Width = it.width })
            }
            Dialog(onDismissRequest = {}) {
                Box(Modifier.fillMaxSize().onSizeChanged { box2Width = it.width })
            }
        }
        val expectedWidth = with(rule.density) { configuration.screenWidthDp.dp.roundToPx() }
        assertThat(box1Width).isEqualTo(expectedWidth)
        assertThat(box2Width).isLessThan(box1Width)
    }

    @Test
    fun canChangeSize() {
        var width by mutableStateOf(10.dp)
        var usePlatformDefaultWidth by mutableStateOf(false)
        var actualWidth = 0

        rule.setContent {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(usePlatformDefaultWidth = usePlatformDefaultWidth)
            ) {
                Box(Modifier.size(width, 150.dp).onSizeChanged { actualWidth = it.width })
            }
        }

        rule.runOnIdle {
            with(rule.density) { assertThat(actualWidth).isEqualTo(10.dp.roundToPx()) }
        }

        width = 20.dp
        rule.runOnIdle {
            with(rule.density) { assertThat(actualWidth).isEqualTo(20.dp.roundToPx()) }
        }

        usePlatformDefaultWidth = true
        width = 30.dp
        rule.runOnIdle {
            with(rule.density) { assertThat(actualWidth).isEqualTo(30.dp.roundToPx()) }
        }

        width = 40.dp
        rule.runOnIdle {
            with(rule.density) { assertThat(actualWidth).isEqualTo(40.dp.roundToPx()) }
        }
    }

    private fun setupDialogTest(
        closeDialogOnDismiss: Boolean = true,
        dialogProperties: DialogProperties = DialogProperties(),
        dialogContent: @Composable () -> Unit = { DefaultDialogContent() },
    ) {
        rule.setContent {
            var showDialog by remember { mutableStateOf(true) }
            val onDismiss: () -> Unit =
                if (closeDialogOnDismiss) {
                    { showDialog = false }
                } else {
                    {}
                }
            if (showDialog) {
                Dialog(onDismiss, dialogProperties, dialogContent)
            }
        }
    }

    @Composable
    private fun DefaultDialogContent(modifier: Modifier = Modifier) {
        BasicText(defaultText, modifier = modifier.testTag(testTag))
        dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
    }

    /** Presses and releases the back button via a key press. */
    private fun pressBackViaKey() {
        UiDevice.getInstance(getInstrumentation()).pressBack()
    }

    /** Dispatches the back button directly, shortcutting any key presses. */
    private fun dispatchBackButton() {
        rule.runOnUiThread { dispatcher.onBackPressed() }
    }

    private fun pressEscape() {
        UiDevice.getInstance(getInstrumentation()).pressKeyCode(KeyEvent.KEYCODE_ESCAPE)
    }

    /** Try to dismiss the dialog by clicking between the topLefts of the dialog and the root. */
    private fun clickOutsideDialog() {
        val dialogBounds = rule.onNode(isRoot().and(hasAnyChild(isDialog()))).boundsOnScreen()
        val rootBounds = rule.onNode(isRoot().and(hasAnyChild(isDialog()).not())).boundsOnScreen()
        val clickPosition = lerp(dialogBounds.topLeft, rootBounds.topLeft, 0.5f).round()
        UiDevice.getInstance(getInstrumentation()).click(clickPosition.x, clickPosition.y)
    }

    private fun SemanticsNodeInteraction.boundsOnScreen(): Rect {
        val bounds = with(rule.density) { getUnclippedBoundsInRoot().toRect() }
        val positionOnScreen = fetchSemanticsNode().positionOnScreen
        return bounds.translate(positionOnScreen)
    }
}
