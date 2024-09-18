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

import android.util.DisplayMetrics
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.gesture.MotionEvent
import androidx.compose.ui.gesture.PointerProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerCoords
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
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
    fun dialogTest_isNotDismissed_whenClicked_noClickableContent() {
        setupDialogTest { DefaultDialogContent() }

        val interaction = rule.onNodeWithTag(testTag)
        interaction.assertIsDisplayed()

        // Click inside the dialog
        interaction.performClick()

        // Check that the Clickable was pressed and the Dialog is still visible.
        interaction.assertIsDisplayed()
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
    fun dialogTest_isDismissed_whenSpecified_decorFitsFalse() {
        setupDialogTest(dialogProperties = DialogProperties(decorFitsSystemWindows = false))
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
    fun dialogTest_isNotDismissed_whenDismissOnClickOutsideIsFalse_decorFitsFalse() {
        setupDialogTest(
            dialogProperties =
                DialogProperties(dismissOnClickOutside = false, decorFitsSystemWindows = false)
        )
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
        lateinit var displayMetrics: DisplayMetrics
        rule.setContent {
            displayMetrics = LocalView.current.context.resources.displayMetrics
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
        val expectedWidth = with(rule.density) { displayMetrics.widthPixels }
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

    @Test
    fun ensurePositionIsCorrect() {
        var positionInRoot by mutableStateOf(Offset.Zero)
        lateinit var view: View

        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                Dialog(onDismissRequest = {}) {
                    view = LocalView.current
                    // I know this is weird, but this is how to reproduce the bug:
                    val dialogWindowProvider = view.parent as DialogWindowProvider
                    dialogWindowProvider.window.setGravity(Gravity.FILL)

                    var boxSize by remember { mutableStateOf(IntSize.Zero) }

                    Box(
                        modifier =
                            Modifier.fillMaxSize().onGloballyPositioned {
                                boxSize = it.size
                                positionInRoot = it.positionOnScreen()
                            },
                        contentAlignment = Alignment.TopStart,
                    ) {
                        PopupUsingPosition(positionInRoot)
                    }
                }
            }
        }

        val realPosition = intArrayOf(0, 0)
        rule.runOnIdle { view.getLocationOnScreen(realPosition) }

        rule.runOnIdle {
            assertThat(positionInRoot.x.roundToInt()).isEqualTo(realPosition[0])
            assertThat(positionInRoot.y.roundToInt()).isEqualTo(realPosition[1])
        }
    }

    @Test
    fun dismissWhenClickingOutsideContent() {
        var dismissed = false
        var clicked = false
        lateinit var composeView: View
        val clickBoxTag = "clickBox"
        rule.setContent {
            Dialog(
                onDismissRequest = { dismissed = true },
                properties =
                    DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false
                    )
            ) {
                composeView = LocalView.current
                Box(Modifier.size(10.dp).testTag(clickBoxTag).clickable { clicked = true })
            }
        }

        // click inside the compose view
        rule.onNodeWithTag(clickBoxTag).performClick()

        rule.waitForIdle()

        assertThat(dismissed).isFalse()
        assertThat(clicked).isTrue()

        clicked = false

        // click outside the compose view
        rule.waitForIdle()
        var root = composeView
        while (root.parent is View) {
            root = root.parent as View
        }

        rule.runOnIdle {
            val x = root.width / 4f
            val y = root.height / 4f
            val down =
                MotionEvent(
                    eventTime = 0,
                    action = ACTION_DOWN,
                    numPointers = 1,
                    actionIndex = 0,
                    pointerProperties = arrayOf(PointerProperties(0)),
                    pointerCoords = arrayOf(PointerCoords(x, y)),
                    root
                )
            root.dispatchTouchEvent(down)
            val up =
                MotionEvent(
                    eventTime = 10,
                    action = ACTION_UP,
                    numPointers = 1,
                    actionIndex = 0,
                    pointerProperties = arrayOf(PointerProperties(0)),
                    pointerCoords = arrayOf(PointerCoords(x, y)),
                    root
                )
            root.dispatchTouchEvent(up)
        }
        rule.waitForIdle()

        assertThat(dismissed).isTrue()
        assertThat(clicked).isFalse()
    }

    @Test
    fun dialogInsetsWhenDecorFitsSystemWindows() {
        var top = -1
        var bottom = -1
        val focusRequester = FocusRequester()
        rule.setContent {
            Dialog(onDismissRequest = {}) {
                val density = LocalDensity.current
                val insets = WindowInsets.safeContent
                Box(
                    Modifier.fillMaxSize().onPlaced {
                        top = insets.getTop(density)
                        bottom = insets.getBottom(density)
                    }
                ) {
                    TextField(
                        "Hello World",
                        onValueChange = {},
                        Modifier.align(Alignment.BottomStart).focusRequester(focusRequester)
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(top).isEqualTo(0)
            assertThat(bottom).isEqualTo(0)
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(top).isEqualTo(0)
            assertThat(bottom).isEqualTo(0)
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

@Composable
private fun PopupUsingPosition(parentPositionInRoot: Offset) {
    // In split screen mode, the parents can have a y offset in vertical mode and a x offset in
    // vertical mode, which needs to be accounted for when calculating gravity and offset
    val popupPositionOffset =
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = anchorBounds.topLeft + parentPositionInRoot.round()
        }

    Popup(popupPositionProvider = popupPositionOffset) { Box(Modifier.fillMaxSize()) }
}
