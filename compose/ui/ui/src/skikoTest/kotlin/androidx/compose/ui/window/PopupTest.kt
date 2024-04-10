/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.FillBox
import androidx.compose.ui.Modifier
import androidx.compose.ui.PopupState
import androidx.compose.ui.assertReceived
import androidx.compose.ui.assertReceivedLast
import androidx.compose.ui.assertReceivedNoEvents
import androidx.compose.ui.assertThat
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.isEqualTo
import androidx.compose.ui.platform.InsetsConfig
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformInsetsConfig
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.platform.ZeroInsetsConfig
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.touch
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.fail
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.jetbrains.skia.Surface

@OptIn(ExperimentalTestApi::class)
class PopupTest {

    private fun setPlatformInsets(insets: PlatformInsets) {
        PlatformInsetsConfig = object : InsetsConfig {
            override val safeInsets: PlatformInsets
                @Composable get() = insets

            override val ime: PlatformInsets
                @Composable get() = PlatformInsets.Zero

            @Composable
            override fun excludeInsets(
                safeInsets: Boolean,
                ime: Boolean,
                content: @Composable () -> Unit
            ) {
                content()
            }
        }
    }

    @BeforeTest
    fun before() {
        PlatformInsetsConfig = ZeroInsetsConfig
    }

    @AfterTest
    fun after() {
        PlatformInsetsConfig = ZeroInsetsConfig
    }

    @Test
    fun passCompositionLocalsToPopup() = runSkikoComposeUiTest {
        val compositionLocal = staticCompositionLocalOf<Int> {
            error("not set")
        }

        var actualLocalValue = 0

        setContent {
            CompositionLocalProvider(compositionLocal provides 3) {
                Popup {
                    actualLocalValue = compositionLocal.current
                }
            }
        }

        assertThat(actualLocalValue).isEqualTo(3)
    }

    // https://github.com/JetBrains/compose-multiplatform/issues/4558
    @Test
    fun changeInStaticCompositionLocalVisibleImmediatelyInPopup() = runComposeUiTest {
        // Test that when the provided value of a staticCompositionLocalOf changes, the change is
        // seen correctly in the content of a popup.
        // The `text` variable is needed in order to cause a recomposition of the popup content in
        // the same frame as CompositionLocalProvider is recomposed. Without this the bug doesn't
        // reproduce because first CompositionLocalProvider is recomposed and that triggers
        // recomposition of the popup content, which happens afterward.

        val compositionLocal = staticCompositionLocalOf<Int> {
            error("not set")
        }
        var providedValue by mutableStateOf(1)
        var text by mutableStateOf("1")
        var actualLocalValue = 0

        setContent {
            CompositionLocalProvider(compositionLocal provides providedValue) {
                Popup {
                    actualLocalValue = compositionLocal.current.also {
                        println(it)
                    }
                    Text(text)
                }
            }
        }

        assertThat(actualLocalValue).isEqualTo(providedValue)
        text = "2"
        providedValue = 2
        waitForIdle()
        assertThat(actualLocalValue).isEqualTo(providedValue)
    }

    // https://github.com/JetBrains/compose-multiplatform/issues/3142
    @Test
    fun passLayoutDirectionToPopup() = runSkikoComposeUiTest {
        lateinit var localLayoutDirection: LayoutDirection

        var layoutDirection by mutableStateOf(LayoutDirection.Rtl)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Popup {
                    localLayoutDirection = LocalLayoutDirection.current
                }
            }
        }

        assertThat(localLayoutDirection).isEqualTo(LayoutDirection.Rtl)

        // Test that changing the local propagates it into the popup
        layoutDirection = LayoutDirection.Ltr
        waitForIdle()
        assertThat(localLayoutDirection).isEqualTo(LayoutDirection.Ltr)
    }

    @Test
    fun onDisposeInsidePopup() = runSkikoComposeUiTest {
        var isPopupShowing by mutableStateOf(true)
        var isDisposed = false

        setContent {
            if (isPopupShowing) {
                Popup {
                    DisposableEffect(Unit) {
                        onDispose {
                            isDisposed = true
                        }
                    }
                }
            }
        }

        isPopupShowing = false
        waitForIdle()

        assertThat(isDisposed).isEqualTo(true)
    }

    @Test
    fun useDensityInsidePopup() = runSkikoComposeUiTest {
        var density by mutableStateOf(Density(2f, 1f))
        var densityInsidePopup = 0f

        setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                Popup {
                    densityInsidePopup = LocalDensity.current.density
                }
            }
        }

        assertThat(densityInsidePopup).isEqualTo(2f)

        density = Density(3f, 1f)
        waitForIdle()
        assertThat(densityInsidePopup).isEqualTo(3f)
    }

    @Test
    fun callDismissIfClickedOutsideOfFocusablePopup() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        var onDismissRequestCallCount = 0

        val background = FillBox()
        val popup = PopupState(
            IntRect(20, 20, 60, 60),
            focusable = true,
            onDismissRequest = { onDismissRequestCallCount++ }
        )

        setContent {
            background.Content()
            popup.Content()
        }

        assertThat(onDismissRequestCallCount).isEqualTo(0)
        scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f))
        background.events.assertReceivedNoEvents()
        assertThat(onDismissRequestCallCount).isEqualTo(1)
    }

    @Test
    fun callDismissIfClickedOutsideOfNonFocusablePopup() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        var onDismissRequestCallCount = 0

        val background = FillBox()
        val popup = PopupState(
            IntRect(20, 20, 60, 60),
            focusable = false,
            dismissOnClickOutside = true,
            onDismissRequest = { onDismissRequestCallCount++ }
        )

        setContent {
            background.Content()
            popup.Content()
        }

        assertThat(onDismissRequestCallCount).isEqualTo(0)
        scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f))
        background.events.assertReceivedLast(PointerEventType.Press, Offset(10f, 10f))
        assertThat(onDismissRequestCallCount).isEqualTo(1)
    }

    @Test
    fun callDismissIfClickedOutsideOfMultipleNonFocusablePopups() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        var onDismissRequestCallCount = 0

        val background = FillBox()
        val popup1 = PopupState(
            IntRect(20, 20, 60, 60),
            focusable = false,
            dismissOnClickOutside = true,
            onDismissRequest = { onDismissRequestCallCount++ }
        )
        val popup2 = PopupState(
            IntRect(30, 30, 70, 70),
            focusable = false,
            dismissOnClickOutside = true,
            onDismissRequest = { onDismissRequestCallCount++ }
        )

        setContent {
            background.Content()
            popup1.Content()
            popup2.Content()
        }

        assertThat(onDismissRequestCallCount).isEqualTo(0)
        scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f))
        background.events.assertReceivedLast(PointerEventType.Press, Offset(10f, 10f))
        assertThat(onDismissRequestCallCount).isEqualTo(2)
    }

    @Test
    fun callDismissForNonFocusablePopupsAbove() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        var onDismissRequestCallCount = 0

        val background = FillBox()
        val popup1 = PopupState(
            IntRect(10, 10, 50, 50),
            focusable = false,
            dismissOnClickOutside = true,
            onDismissRequest = { fail() }
        )
        val popup2 = PopupState(
            IntRect(20, 20, 60, 60),
            focusable = false,
            dismissOnClickOutside = true,
            onDismissRequest = { fail() }
        )
        val popup3 = PopupState(
            IntRect(30, 30, 70, 70),
            focusable = false,
            dismissOnClickOutside = true,
            onDismissRequest = { onDismissRequestCallCount++ }
        )
        val popup4 = PopupState(
            IntRect(40, 40, 80, 80),
            focusable = false,
            dismissOnClickOutside = true,
            onDismissRequest = { onDismissRequestCallCount++ }
        )

        setContent {
            background.Content()
            popup1.Content()
            popup2.Content()
            popup3.Content()
            popup4.Content()
        }

        assertThat(onDismissRequestCallCount).isEqualTo(0)
        scene.sendPointerEvent(PointerEventType.Press, Offset(55f, 25f))
        background.events.assertReceivedNoEvents()
        assertThat(onDismissRequestCallCount).isEqualTo(2)
    }

    @Test
    fun callDismissForAboveFocusablePopup() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        var onDismissRequestCallCount = 0

        val background = FillBox()
        val popup1 = PopupState(
            IntRect(10, 10, 50, 50),
            focusable = false,
            dismissOnClickOutside = true,
            onDismissRequest = { fail() }
        )
        val popup2 = PopupState(
            IntRect(20, 20, 60, 60),
            focusable = false,
            dismissOnClickOutside = true,
            onDismissRequest = { fail() }
        )
        val popup3 = PopupState(
            IntRect(30, 30, 70, 70),
            focusable = true,
            dismissOnClickOutside = true,
            onDismissRequest = { onDismissRequestCallCount++ }
        )
        val popup4 = PopupState(
            IntRect(40, 40, 80, 80),
            focusable = false,
            dismissOnClickOutside = true,
            onDismissRequest = { onDismissRequestCallCount++ }
        )

        setContent {
            background.Content()
            popup1.Content()
            popup2.Content()
            popup3.Content()
            popup4.Content()
        }

        assertThat(onDismissRequestCallCount).isEqualTo(0)
        scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f))
        background.events.assertReceivedNoEvents()
        assertThat(onDismissRequestCallCount).isEqualTo(2)
    }

    @Test
    fun passEventIfClickedOutsideOfNonFocusablePopup() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        var onDismissRequestCallCount = 0

        val background = FillBox()
        val popup = PopupState(
            IntRect(20, 20, 60, 60),
            focusable = false,
            onDismissRequest = { onDismissRequestCallCount++ }
        )

        setContent {
            background.Content()
            popup.Content()
        }

        assertThat(onDismissRequestCallCount).isEqualTo(0)
        scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f))
        background.events.assertReceivedLast(PointerEventType.Press, Offset(10f, 10f))
        assertThat(onDismissRequestCallCount).isEqualTo(0)
    }

    @Test
    fun doNotPassEventIfClickedOutsideOfFocusablePopup() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        val background = FillBox()
        val popup = PopupState(
            IntRect(20, 20, 60, 60),
            focusable = true
        )

        setContent {
            background.Content()
            popup.Content()
        }

        scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f))
        scene.sendPointerEvent(PointerEventType.Release, Offset(10f, 10f))
        background.events.assertReceivedNoEvents()
    }

    @Test
    fun canScrollOutsideOfNonFocusablePopup() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        val background = FillBox()
        val popup = PopupState(IntRect(20, 20, 60, 60), focusable = false)

        setContent {
            background.Content()
            popup.Content()
        }

        scene.sendPointerEvent(PointerEventType.Scroll, Offset(10f, 10f))
        background.events.assertReceivedLast(PointerEventType.Scroll, Offset(10f, 10f))
    }

    @Test
    fun cannotScrollOutsideOfFocusablePopup() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        val background = FillBox()
        val popup = PopupState(IntRect(20, 20, 60, 60), focusable = true)

        setContent {
            background.Content()
            popup.Content()
        }

        scene.sendPointerEvent(PointerEventType.Scroll, Offset(10f, 10f))
        background.events.assertReceivedNoEvents()
    }

    @Test
    fun openFocusablePopup() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        val openPopup = mutableStateOf(false)
        val background = FillBox {
            openPopup.value = true
        }
        val popup = PopupState(
            IntRect(20, 20, 60, 60),
            focusable = true,
            onDismissRequest = {
                openPopup.value = false
            }
        )

        setContent {
            background.Content()
            if (openPopup.value) {
                popup.Content()
            }
        }

        // Click (Press-Release cycle) opens popup and sends all events to "background"
        val buttons = PointerButtons(
            isPrimaryPressed = true
        )
        scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f), buttons = buttons, button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, Offset(10f, 10f), button = PointerButton.Primary)
        onNodeWithTag(popup.tag).assertIsDisplayed()

        background.events.assertReceived(PointerEventType.Press, Offset(10f, 10f))
        background.events.assertReceived(PointerEventType.Release, Offset(10f, 10f))
        background.events.assertReceived(PointerEventType.Enter, Offset(10f, 10f))
        background.events.assertReceivedLast(PointerEventType.Exit, Offset(10f, 10f))
    }

    @Test
    fun closeFocusablePopup() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        val openPopup = mutableStateOf(false)
        val background = FillBox()
        val popup = PopupState(
            IntRect(20, 20, 60, 60),
            focusable = true,
            onDismissRequest = {
                openPopup.value = false
            }
        )

        setContent {
            background.Content()
            if (openPopup.value) {
                popup.Content()
            }
        }

        // Moving without popup generates Enter because it's in bounds
        scene.sendPointerEvent(PointerEventType.Move, Offset(15f, 15f))
        background.events.assertReceivedLast(PointerEventType.Enter, Offset(15f, 15f))

        // Open popup
        openPopup.value = true
        onNodeWithTag(popup.tag).assertIsDisplayed()
        background.events.assertReceivedLast(PointerEventType.Exit, Offset(15f, 15f))

        // Click (Press-Move-Release cycle) outside closes popup and sends only Enter event to background
        val buttons = PointerButtons(
            isPrimaryPressed = true
        )
        scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f), buttons = buttons, button = PointerButton.Primary)
        onNodeWithTag(popup.tag).assertDoesNotExist() // Wait that it's really closed before next events

        scene.sendPointerEvent(PointerEventType.Move, Offset(11f, 11f), buttons = buttons)
        scene.sendPointerEvent(PointerEventType.Release, Offset(11f, 11f), button = PointerButton.Primary)
        background.events.assertReceivedLast(PointerEventType.Enter, Offset(11f, 11f))
    }

    @Test
    fun secondClickDoesNotDismissPopup() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        val background = FillBox()
        val popup = PopupState(
            IntRect(20, 20, 60, 60),
            dismissOnClickOutside = true,
            onDismissRequest = { fail() }
        )

        setContent {
            background.Content()
            popup.Content()
        }

        scene.sendPointerEvent(
            PointerEventType.Press,
            pointers = listOf(
                touch(50f, 50f, pressed = true, id = 1),
            )
        )
        scene.sendPointerEvent(
            PointerEventType.Press,
            pointers = listOf(
                touch(50f, 50f, pressed = true, id = 1),
                touch(10f, 10f, pressed = true, id = 2),
            )
        )
        scene.sendPointerEvent(
            PointerEventType.Release,
            pointers = listOf(
                touch(50f, 50f, pressed = false, id = 1),
                touch(10f, 10f, pressed = true, id = 2),
            )
        )
        scene.sendPointerEvent(
            PointerEventType.Release,
            pointers = listOf(
                touch(10f, 10f, pressed = false, id = 2),
            )
        )
    }

    @Test
    fun clippingEnabledPopup() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        setContent {
            Popup(
                offset = IntOffset(80, 80)
            ) {
                Box(Modifier.size(50.dp).testTag("box1"))
            }
            Popup(
                offset = IntOffset(-30, -30)
            ) {
                Box(Modifier.size(50.dp).testTag("box2"))
            }
        }
        onNodeWithTag("box1").assertPositionInRootIsEqualTo(50.dp, 50.dp)
        onNodeWithTag("box2").assertPositionInRootIsEqualTo(0.dp, 0.dp)
    }

    @Test
    fun clippingDisabledPopup() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        setContent {
            Popup(
                offset = IntOffset(80, 80),
                properties = PopupProperties(clippingEnabled = false)
            ) {
                Box(Modifier.size(50.dp).testTag("box1"))
            }
            Popup(
                offset = IntOffset(-30, -30),
                properties = PopupProperties(clippingEnabled = false)
            ) {
                Box(Modifier.size(50.dp).testTag("box2"))
            }
        }
        onNodeWithTag("box1").assertPositionInRootIsEqualTo(80.dp, 80.dp)
        onNodeWithTag("box2").assertPositionInRootIsEqualTo((-30).dp, (-30).dp)
    }

    // https://github.com/JetBrains/compose-multiplatform-core/pull/847
    @Test
    fun popupBoundsWithPlatformInsets() = runSkikoComposeUiTest(
        size = Size(200f, 200f)
    ) {
        setPlatformInsets(PlatformInsets(left = 5.dp, top = 50.dp, right = 5.dp, bottom = 10.dp))
        setContent {
            Popup(
                popupPositionProvider = object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset = IntOffset.Zero
                }
            ) {
                Box(Modifier.fillMaxSize().testTag("box1"))
            }
            Box(Modifier.offset(30.dp, 100.dp)) {
                Popup {
                    Box(Modifier.size(50.dp).testTag("box2"))
                }
            }
        }
        onNodeWithTag("box1")
            .assertPositionInRootIsEqualTo(5.dp, 50.dp)
            .assertWidthIsEqualTo(190.dp)
            .assertHeightIsEqualTo(140.dp)
        onNodeWithTag("box2")
            .assertPositionInRootIsEqualTo(30.dp, 100.dp) // Matches parent position (if inside bounds)
    }

    @Test
    fun doNotLoseHoverOutsideOfPopup() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        val openPopup = mutableStateOf(false)
        val background = FillBox()
        val popup = PopupState(
            IntRect(20, 20, 60, 60),
            focusable = false,
            onDismissRequest = {
                openPopup.value = false
            }
        )

        setContent {
            background.Content()
            if (openPopup.value) {
                popup.Content()
            }
        }

        // Moving without popup generates Enter because it's in bounds
        scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
        background.events.assertReceivedLast(PointerEventType.Enter, Offset(5f, 5f))

        // Open popup
        openPopup.value = true
        onNodeWithTag(popup.tag).assertIsDisplayed()

        // It should not generate extra Exit/Enter events
        background.events.assertReceivedNoEvents()
    }

    @Test
    fun popupContentLargerThanWindowInfoContainer() = runTest(StandardTestDispatcher()) {
        lateinit var scene: ComposeScene
        val size = IntSize(100, 100)
        val surface = Surface.makeRasterN32Premul(size.width, size.height)
        fun invalidate() {
            scene.render(surface.canvas.asComposeCanvas(), 1)
        }
        scene = MultiLayerComposeScene(
            composeSceneContext = object : ComposeSceneContext {
            }.also {
                val windowInfo = it.platformContext.windowInfo as WindowInfoImpl
                windowInfo.containerSize = IntSize(50, 50)
            },
            invalidate = ::invalidate
        )
        try {
            scene.size = size
            scene.setContent {
                Popup {
                    Box(Modifier.size(200.dp))
                }
            }
            invalidate()
        } finally {
            scene.close()
        }
    }
}
