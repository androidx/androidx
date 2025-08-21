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

import android.content.pm.ActivityInfo
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.Window
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.Text
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerCoords
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TestActivity2
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
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
    @get:Rule val rule = createAndroidComposeRule<TestActivity2>()

    lateinit var activity: ComponentActivity

    private val defaultText = "dialogText"
    private val testTag = "tag"
    private lateinit var dispatcher: OnBackPressedDispatcher

    @Test
    fun dialogTest_isShowingContent() {
        setupDialogTest(closeDialogOnDismiss = false)
        rule.onNodeWithTag(testTag).assertIsDisplayed()
    }

    @Test
    fun dialogTest_windowTitleCustom() {
        lateinit var window: Window
        rule.setContent {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(windowTitle = defaultText),
            ) {
                var parent = LocalView.current
                while (parent !is DialogWindowProvider) {
                    parent = parent.parent as View
                }
                window = (parent as DialogWindowProvider).window
                Box(Modifier.size(10.dp))
            }
        }

        rule.runOnIdle { assertThat(window.attributes.title).isEqualTo(defaultText) }
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun dialogTest_isNotDismissed_whenPressOutside_releaseInside() {
        setupDialogTest(dialogProperties = DialogProperties())
        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()

        clickDialog(pressOutside = true, releaseOutside = false)
        // The Dialog should still be visible
        textInteraction.assertIsDisplayed()
    }

    @Test
    fun dialogTest_isNotDismissed_whenPressInside_releaseOutside() {
        setupDialogTest(dialogProperties = DialogProperties())
        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()

        clickDialog(pressOutside = false, releaseOutside = true)
        // The Dialog should still be visible
        textInteraction.assertIsDisplayed()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun dialogTest_isNotDismissed_whenPressOutside_releaseInside_decorFitsFalse() {
        setupDialogTest(dialogProperties = DialogProperties(decorFitsSystemWindows = false))
        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()

        clickDialog(pressOutside = true, releaseOutside = false)
        // The Dialog should still be visible
        textInteraction.assertIsDisplayed()
    }

    @Test
    fun dialogTest_isNotDismissed_whenPressInside_releaseOutside_decorFitsFalse() {
        setupDialogTest(dialogProperties = DialogProperties(decorFitsSystemWindows = false))
        val textInteraction = rule.onNodeWithTag(testTag)
        textInteraction.assertIsDisplayed()

        clickDialog(pressOutside = false, releaseOutside = true)
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
    fun smallDialogHasSmallWindowDefaultWidthDecorFits() {
        lateinit var dialogView: View
        rule.setContent {
            Dialog(
                {},
                properties =
                    DialogProperties(usePlatformDefaultWidth = true, decorFitsSystemWindows = true),
            ) {
                dialogView = LocalView.current
                Box(Modifier.size(with(LocalDensity.current) { 100.toDp() }))
            }
        }
        rule.runOnIdle {
            var root = dialogView
            while (root.parent is View) {
                root = root.parent as View
            }
            assertThat(root.width).isEqualTo(100)
            assertThat(root.height).isEqualTo(100)
        }
    }

    @Test
    fun smallDialogHasSmallWindowNotDefaultWidthDecorFits() {
        lateinit var dialogView: View
        rule.setContent {
            Dialog(
                {},
                properties =
                    DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true),
            ) {
                dialogView = LocalView.current
                Box(Modifier.size(with(LocalDensity.current) { 100.toDp() }))
            }
        }
        rule.runOnIdle {
            var root = dialogView
            while (root.parent is View) {
                root = root.parent as View
            }
            assertThat(root.height).isEqualTo(100)
        }
    }

    @Test
    fun smallDialogHasSmallWindowDefaultWidthNoDecorFits() {
        lateinit var dialogView: View
        rule.setContent {
            Dialog(
                {},
                properties =
                    DialogProperties(usePlatformDefaultWidth = true, decorFitsSystemWindows = false),
            ) {
                dialogView = LocalView.current
                Box(Modifier.size(with(LocalDensity.current) { 100.toDp() }))
            }
        }
        rule.runOnIdle {
            val point = Point()
            @Suppress("DEPRECATION") dialogView.display.getRealSize(point)
            var root = dialogView
            while (root.parent is View) {
                root = root.parent as View
            }
            // For some reason, when decorFitsSystemWindows = false, the window doesn't
            // WRAP_CONTENT the dialog, but the width should be less than the full width of the
            // screen.
            assertThat(root.width).isLessThan(point.x)
            assertThat(root.height).isEqualTo(100)
        }
    }

    @Test
    fun smallDialogHasSmallWindowNotDefaultWidthNoDecorFits() {
        lateinit var dialogView: View
        rule.setContent {
            Dialog(
                {},
                properties =
                    DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false,
                    ),
            ) {
                dialogView = LocalView.current
                Box(Modifier.size(with(LocalDensity.current) { 100.toDp() }))
            }
        }
        rule.runOnIdle {
            var root = dialogView
            while (root.parent is View) {
                root = root.parent as View
            }
            assertThat(root.height).isEqualTo(100)
        }
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
                properties = DialogProperties(usePlatformDefaultWidth = false),
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
                properties = DialogProperties(usePlatformDefaultWidth = usePlatformDefaultWidth),
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
                    DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true),
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
            val x = 1f
            val y = 1f
            val down =
                MotionEvent(
                    eventTime = 0,
                    action = ACTION_DOWN,
                    numPointers = 1,
                    actionIndex = 0,
                    pointerProperties = arrayOf(PointerProperties(0)),
                    pointerCoords = arrayOf(PointerCoords(x, y)),
                    root,
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
                    root,
                )
            root.dispatchTouchEvent(up)
        }
        rule.waitForIdle()

        assertThat(dismissed).isTrue()
        assertThat(clicked).isFalse()
    }

    @Test
    fun dismissWhenClickingOutsideContentNoDecorFitsSystemWindows() {
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
                        decorFitsSystemWindows = false,
                    ),
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
            val x = 1f
            val y = 1f
            val down =
                MotionEvent(
                    eventTime = 0,
                    action = ACTION_DOWN,
                    numPointers = 1,
                    actionIndex = 0,
                    pointerProperties = arrayOf(PointerProperties(0)),
                    pointerCoords = arrayOf(PointerCoords(x, y)),
                    root,
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
                    root,
                )
            root.dispatchTouchEvent(up)
        }
        rule.waitForIdle()

        assertThat(dismissed).isTrue()
        assertThat(clicked).isFalse()
    }

    @Test
    fun dismissWhenClickingWithNaNEvent() {
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
                        decorFitsSystemWindows = false,
                    ),
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
            val x = Float.NaN
            val y = Float.NaN
            val down =
                MotionEvent(
                    eventTime = 0,
                    action = ACTION_DOWN,
                    numPointers = 1,
                    actionIndex = 0,
                    pointerProperties = arrayOf(PointerProperties(0)),
                    pointerCoords = arrayOf(PointerCoords(x, y)),
                    root,
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
                    root,
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
                val insets = WindowInsets.safeDrawing
                Box(
                    Modifier.fillMaxSize().onPlaced {
                        top = insets.getTop(density)
                        bottom = insets.getBottom(density)
                    }
                ) {
                    TextField(
                        "Hello World",
                        onValueChange = {},
                        Modifier.align(Alignment.BottomStart).focusRequester(focusRequester),
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(top).isEqualTo(0)
            assertThat(bottom).isEqualTo(0)
            focusRequester.requestFocus()
        }

        // On 35+, the IME WindowInsets are still passed, even though the content automatically
        // avoids the IME. b/430601578
        if (Build.VERSION.SDK_INT < 35) {
            rule.runOnIdle {
                assertThat(top).isEqualTo(0)
                assertThat(bottom).isEqualTo(0)
            }
        }
    }

    @Test
    fun dialogDefaultGravityIsCenter() {
        lateinit var view: View
        rule.setContent {
            Dialog(onDismissRequest = {}) {
                view = LocalView.current
                Box(Modifier.size(10.dp))
            }
        }
        rule.runOnIdle {
            var provider = view
            while (provider !is DialogWindowProvider) {
                provider = view.parent as View
            }
            val window = provider.window
            assertThat(window.attributes.gravity).isEqualTo(Gravity.CENTER)
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = 34)
    fun dialogThemeCanBeOverridden() {
        lateinit var window: Window
        rule.setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    ComposeView(
                            ContextThemeWrapper(
                                context,
                                androidx.compose.ui.tests.R.style.CustomDialogTheme,
                            )
                        )
                        .apply {
                            setContent {
                                Dialog(
                                    onDismissRequest = {},
                                    properties =
                                        DialogProperties(
                                            decorFitsSystemWindows = false,
                                            usePlatformDefaultWidth = false,
                                        ),
                                ) {
                                    var parent = LocalView.current
                                    while (parent !is DialogWindowProvider) {
                                        parent = parent.parent as View
                                    }
                                    window = (parent as DialogWindowProvider).window
                                    Box(Modifier.fillMaxSize().background(Color.Blue))
                                }
                            }
                        }
                },
            )
        }
        rule.runOnIdle {
            @Suppress("DEPRECATION") assertThat(window.statusBarColor).isEqualTo(Color.Red.toArgb())
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun fullScreenDialogDrawsBehindDisplayCutout() {
        lateinit var window: Window
        rule.setContent {
            Dialog(
                properties =
                    DialogProperties(
                        decorFitsSystemWindows = false,
                        usePlatformDefaultWidth = false,
                    ),
                onDismissRequest = {},
            ) {
                var parent = LocalView.current
                while (parent !is DialogWindowProvider) {
                    parent = parent.parent as View
                }
                window = (parent as DialogWindowProvider).window
                Box(Modifier.fillMaxSize())
            }
        }

        rule.runOnIdle {
            assertThat(window.attributes.layoutInDisplayCutoutMode)
                .isEqualTo(LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS)
        }
    }

    @Test
    fun fullScreenDialogPortraitNotDefaultWidthDecorFitsMatchesContainerSize() {
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        verifyFullScreenDialogMatchesContainer()
    }

    @Test
    fun fullScreenDialogLandscapeNotDefaultWidthDecorFitsMatchesContainerSize() {
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        verifyFullScreenDialogMatchesContainer()
    }

    private fun verifyFullScreenDialogMatchesContainer() {
        var mainContentWidth = 0
        var mainContentHeight = 0
        var dialogWidth = 0
        var dialogHeight = 0
        rule.activityRule.scenario.onActivity {
            WindowCompat.setDecorFitsSystemWindows(it.window, false)
        }
        rule.setContent {
            Box(
                modifier =
                    Modifier.safeDrawingPadding().fillMaxSize().onGloballyPositioned {
                        mainContentWidth = it.size.width
                        mainContentHeight = it.size.height
                    }
            ) {
                Dialog(
                    onDismissRequest = {},
                    properties =
                        DialogProperties(
                            usePlatformDefaultWidth = false,
                            decorFitsSystemWindows = true,
                        ),
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize().onGloballyPositioned {
                                dialogWidth = it.size.width
                                dialogHeight = it.size.height
                            }
                    ) {}
                }
            }
        }

        rule.runOnIdle {
            assertThat(mainContentWidth).isEqualTo(dialogWidth)
            assertThat(mainContentHeight).isEqualTo(dialogHeight)
        }
    }

    @Test
    fun fullScreenDialogWithImeNotDefaultWidthDecorFitsMatchesContainerSize() {
        var mainContentWidth = 0
        var mainContentHeight = 0
        var dialogWidth = 0
        var dialogHeight = 0
        rule.activityRule.scenario.onActivity {
            WindowCompat.setDecorFitsSystemWindows(it.window, false)
        }
        rule.setContent {
            val keyboardController = LocalSoftwareKeyboardController.current
            keyboardController?.show()
            Box(
                modifier =
                    Modifier.safeDrawingPadding().fillMaxSize().onGloballyPositioned {
                        mainContentWidth = it.size.width
                        mainContentHeight = it.size.height
                    }
            ) {
                Dialog(
                    onDismissRequest = {},
                    properties =
                        DialogProperties(
                            usePlatformDefaultWidth = false,
                            decorFitsSystemWindows = true,
                        ),
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize().onGloballyPositioned {
                                dialogWidth = it.size.width
                                dialogHeight = it.size.height
                            }
                    ) {
                        TextField(
                            state = rememberTextFieldState(initialText = "Hello"),
                            label = { Text("Label") },
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(mainContentWidth).isEqualTo(dialogWidth)
            assertThat(mainContentHeight).isEqualTo(dialogHeight)
        }
    }

    @Test
    fun fullScreenDialogNotDefaultWidthNoDecorFitsMatchesContainerSize() {
        var mainContentWidth = 0
        var mainContentHeight = 0
        var dialogWidth = 0
        var dialogHeight = 0
        rule.activityRule.scenario.onActivity {
            WindowCompat.setDecorFitsSystemWindows(it.window, false)
        }
        rule.setContent {
            Box(
                modifier =
                    Modifier.fillMaxSize().onGloballyPositioned {
                        mainContentWidth = it.size.width
                        mainContentHeight = it.size.height
                    }
            ) {
                Dialog(
                    onDismissRequest = {},
                    properties =
                        DialogProperties(
                            usePlatformDefaultWidth = false,
                            decorFitsSystemWindows = false,
                        ),
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize().onGloballyPositioned {
                                dialogWidth = it.size.width
                                dialogHeight = it.size.height
                            }
                    ) {}
                }
            }
        }

        rule.runOnIdle {
            assertThat(mainContentWidth).isEqualTo(dialogWidth)
            assertThat(mainContentHeight).isEqualTo(dialogHeight)
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
        clickDialog()
    }

    private fun clickDialog(pressOutside: Boolean = true, releaseOutside: Boolean = true) {
        val dialogBounds = rule.onNode(isRoot().and(hasAnyChild(isDialog()))).boundsOnScreen()
        val rootBounds = rule.onNode(isRoot().and(hasAnyChild(isDialog()).not())).boundsOnScreen()
        val outsidePosition = lerp(dialogBounds.topLeft, rootBounds.topLeft, 0.5f).round()
        val insidePosition = lerp(dialogBounds.topLeft, dialogBounds.bottomRight, 0.5f).round()
        val uiDevice = UiDevice.getInstance(getInstrumentation())
        if (pressOutside && releaseOutside) {
            uiDevice.click(outsidePosition.x, outsidePosition.y)
        } else if (!pressOutside && !releaseOutside) {
            uiDevice.click(insidePosition.x, insidePosition.y)
        } else if (pressOutside) {
            uiDevice.drag(
                outsidePosition.x,
                outsidePosition.y,
                insidePosition.x,
                insidePosition.y,
                10,
            )
        } else {
            uiDevice.drag(
                insidePosition.x,
                insidePosition.y,
                outsidePosition.x,
                outsidePosition.y,
                10,
            )
        }
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
                popupContentSize: IntSize,
            ): IntOffset = anchorBounds.topLeft + parentPositionInRoot.round()
        }

    Popup(popupPositionProvider = popupPositionOffset) { Box(Modifier.fillMaxSize()) }
}
