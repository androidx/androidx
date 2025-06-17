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

package androidx.compose.ui.keyboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.TextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.dpRectInWindow
import androidx.compose.ui.test.utils.forEachWithPrevious
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.asDpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.window.KeyboardVisibilityListener
import androidx.compose.ui.window.KeyboardVisibilityObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRect
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationOptions

internal class KeyboardInsetsTest {
    @Test
    fun testImeInsetsAnimationFrames_FocusAboveKeyboard() = runUIKitInstrumentedTest {
        val contentFrames = mutableListOf<DpRect>()
        var lastContentFrame = DpRect(DpOffset.Unspecified, DpSize.Unspecified)
        var focusManager: FocusManager? = null
        val focusRequester = FocusRequester()

        setContent({
            onFocusBehavior = OnFocusBehavior.FocusableAboveKeyboard
        }) {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .onGloballyPositioned { coordinates ->
                        // Since you can have multiple layouts per render cycle, remember the last
                        // one and add it for further analysis during the render phase.
                        lastContentFrame = coordinates.boundsInWindow().toDpRect(density)
                    }
                    .drawWithContent {
                        contentFrames.add(lastContentFrame)
                    }
            ) {
                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .align(Alignment.BottomCenter)
                )
            }
        }

        val screenRect = DpRect(origin = DpOffset.Zero, size = screenSize)

        assertEquals(screenRect, contentFrames.last())
        assertTrue(contentFrames.all { it == screenRect })

        // Show keyboard with animation
        focusRequester.requestFocus()
        contentFrames.clear()
        waitForIdle()

        val visibleRect = DpRect(
            left = 0.dp,
            top = 0.dp,
            right = screenSize.width,
            bottom = screenSize.height - keyboardHeight
        )

        assertTrue(contentFrames.count() > 5, "Animation should produce large number of frames")
        assertEquals(visibleRect, contentFrames.last(), "")
        contentFrames.forEach {
            assertEquals(0.dp, it.top, "Content must be top-aligned")
        }
        contentFrames.forEachWithPrevious { previousFrame, nextFrame ->
            assertTrue(
                nextFrame.bottom <= previousFrame.bottom,
                "Content must shrink up on every frame"
            )
            assertTrue(
                nextFrame.height <= previousFrame.height,
                "Content size must decrease on every frame"
            )
        }

        // Hide keyboard with animation
        focusManager?.clearFocus()
        contentFrames.clear()
        waitForIdle()

        assertTrue(contentFrames.count() > 5, "Animation should produce large number of frames")
        assertEquals(screenRect, contentFrames.last())
        contentFrames.forEach {
            assertEquals(0.dp, it.top, "Content must be top-aligned")
        }
        contentFrames.forEachWithPrevious { previousFrame, nextFrame ->
            assertTrue(
                actual = nextFrame.bottom >= previousFrame.bottom,
                "Content must expand down on every frame"
            )
            assertTrue(
                actual = nextFrame.height >= previousFrame.height,
                "Content size must increase on every frame"
            )
        }
    }

    @Test
    fun testImeInsetsAnimationFrames_DoNothing() = runUIKitInstrumentedTest {
        val contentFrames = mutableListOf<DpRect>()
        var lastContentFrame = DpRect(DpOffset.Unspecified, DpSize.Unspecified)
        var focusManager: FocusManager? = null
        val focusRequester = FocusRequester()

        setContent({
            onFocusBehavior = OnFocusBehavior.DoNothing
        }) {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .onGloballyPositioned { coordinates ->
                        // Since you can have multiple layouts per render cycle, remember the last
                        // one and add it for further analysis during the render phase.
                        lastContentFrame = coordinates.boundsInWindow().toDpRect(density)
                    }
                    .drawWithContent {
                        contentFrames.add(lastContentFrame)
                    }
            ) {
                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .align(Alignment.BottomCenter)
                )
            }
        }

        val screenRect = DpRect(origin = DpOffset.Zero, size = screenSize)

        assertEquals(screenRect, contentFrames.last())
        assertTrue(contentFrames.all { it == screenRect })

        // Show keyboard with animation
        focusRequester.requestFocus()
        contentFrames.clear()
        waitForIdle()

        val visibleRect = DpRect(
            left = 0.dp,
            top = 0.dp,
            right = screenSize.width,
            bottom = screenSize.height - keyboardHeight
        )

        assertTrue(contentFrames.count() > 5, "Animation should produce large number of frames")
        assertEquals(visibleRect, contentFrames.last(), "")
        contentFrames.forEach {
            assertEquals(0.dp, it.top, "Content must be top-aligned")
        }
        contentFrames.forEachWithPrevious { previousFrame, nextFrame ->
            assertTrue(
                nextFrame.bottom <= previousFrame.bottom,
                "Content must shrink up on every frame"
            )
            assertTrue(
                nextFrame.height <= previousFrame.height,
                "Content size must decrease on every frame"
            )
        }

        // Hide keyboard with animation
        focusManager?.clearFocus()
        contentFrames.clear()
        waitForIdle()

        assertTrue(contentFrames.count() > 5, "Animation should produce large number of frames")
        assertEquals(screenRect, contentFrames.last())
        contentFrames.forEach {
            assertEquals(0.dp, it.top, "Content must be top-aligned")
        }
        contentFrames.forEachWithPrevious { previousFrame, nextFrame ->
            assertTrue(
                actual = nextFrame.bottom >= previousFrame.bottom,
                "Content must expand down on every frame"
            )
            assertTrue(
                actual = nextFrame.height >= previousFrame.height,
                "Content size must increase on every frame"
            )
        }
    }

    @Test
    fun testFocusableAboveKeyboardOffsetBehavior() = runUIKitInstrumentedTest {
        var textRectInWindow: DpRect? = null
        var textRectInRoot: DpRect? = null
        val bottomPadding = 100.dp
        val interopView = UIView()
        var focusManager: FocusManager? = null

        setContent({
            onFocusBehavior = OnFocusBehavior.FocusableAboveKeyboard
        }) {
            focusManager = LocalFocusManager.current
            val focusRequester = remember { FocusRequester() }
            Box(Modifier.padding(bottom = bottomPadding)) {
                UIKitView(
                    factory = { interopView },
                    modifier = Modifier.fillMaxSize()
                )
                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .align(Alignment.BottomCenter)
                        .onGloballyPositioned { coordinates ->
                            textRectInWindow = coordinates.boundsInWindow().toDpRect(density)
                            textRectInRoot = coordinates.boundsInRoot().toDpRect(density)
                        }
                )
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        assertEquals(screenSize.height - keyboardHeight, textRectInWindow?.bottom)
        assertEquals(screenSize.height - keyboardHeight, textRectInRoot?.bottom)
        assertEquals(screenSize.height - keyboardHeight, interopView.dpRectInWindow().bottom)

        focusManager?.clearFocus()
        waitForIdle()

        assertEquals(screenSize.height - bottomPadding, textRectInWindow?.bottom)
        assertEquals(screenSize.height - bottomPadding, textRectInRoot?.bottom)
        assertEquals(screenSize.height - bottomPadding, interopView.dpRectInWindow().bottom)
    }

    @Test
    fun testFocusableAboveKeyboardRefocusBehavior() = runUIKitInstrumentedTest {
        var text1RectInWindow: DpRect? = null
        var text2RectInWindow: DpRect? = null
        var text3RectInWindow: DpRect? = null

        val focusRequester1 = FocusRequester()
        val focusRequester2 = FocusRequester()
        val focusRequester3 = FocusRequester()
        setContent({
            onFocusBehavior = OnFocusBehavior.FocusableAboveKeyboard
        }) {
            @Composable
            fun TestTextField(
                requester: FocusRequester,
                onPositionInWindowChanged: (DpRect) -> Unit
            ) = TextField(
                value = "",
                onValueChange = {},
                modifier = Modifier
                    .focusRequester(requester)
                    .onGloballyPositioned {
                        onPositionInWindowChanged(it.boundsInWindow().toDpRect(density))
                    }
            )

            Column(Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.weight(1f))
                TestTextField(focusRequester1) { text1RectInWindow = it }
                TestTextField(focusRequester2) { text2RectInWindow = it }
                TestTextField(focusRequester3) { text3RectInWindow = it }
            }

            LaunchedEffect(Unit) {
                focusRequester1.requestFocus()
            }
        }

        assertEquals(screenSize.height - keyboardHeight, text1RectInWindow?.bottom)
        assertNotEquals(screenSize.height - keyboardHeight, text2RectInWindow?.bottom)
        assertNotEquals(screenSize.height - keyboardHeight, text3RectInWindow?.bottom)

        focusRequester2.requestFocus()
        waitForIdle()

        assertNotEquals(screenSize.height - keyboardHeight, text1RectInWindow?.bottom)
        assertEquals(screenSize.height - keyboardHeight, text2RectInWindow?.bottom)
        assertNotEquals(screenSize.height - keyboardHeight, text3RectInWindow?.bottom)

        focusRequester3.requestFocus()
        waitForIdle()

        assertNotEquals(screenSize.height - keyboardHeight, text1RectInWindow?.bottom)
        assertNotEquals(screenSize.height - keyboardHeight, text2RectInWindow?.bottom)
        assertEquals(screenSize.height - keyboardHeight, text3RectInWindow?.bottom)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testRefocusByTapKeyboardSizeNotChanges() = runUIKitInstrumentedTest {
        val keyboardFrames = mutableListOf<DpRect>()
        val contentFrames = mutableListOf<DpRect>()
        val observer = object : KeyboardVisibilityObserver {
            override fun keyboardWillShow(
                targetFrame: CValue<CGRect>,
                duration: Double,
                animationOptions: UIViewAnimationOptions
            ) {
            }

            override fun keyboardWillHide(
                targetFrame: CValue<CGRect>,
                duration: Double,
                animationOptions: UIViewAnimationOptions
            ) {
            }

            override fun keyboardWillChangeFrame(
                targetFrame: CValue<CGRect>,
                duration: Double,
                animationOptions: UIViewAnimationOptions
            ) {
                keyboardFrames.add(targetFrame.asDpRect())
            }
        }
        KeyboardVisibilityListener.addObserver(observer)

        setContent {
            Column(modifier = Modifier.fillMaxSize().imePadding().onGloballyPositioned {
                contentFrames.add(it.boundsInRoot().toDpRect(density))
            }) {
                Spacer(Modifier.weight(100f))
                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.testTag("TF1")
                )
                TextField(
                    state = rememberTextFieldState(),
                    modifier = Modifier.testTag("TF2")
                )
            }
        }

        findNodeWithTag("TF1").tap()
        waitForIdle()

        assertFalse(keyboardFrames.isEmpty(), "No keyboard frame changes detected")
        assertFalse(contentFrames.emptyOrAllEqual(), "No content size changes due to ime height changes detected")
        // Remove frame changes during the keyboard appearance
        keyboardFrames.clear()
        contentFrames.clear()

        // Switch between text fields
        findNodeWithTag("TF2").tap()
        waitForIdle()
        findNodeWithTag("TF1").tap()
        waitForIdle()
        KeyboardVisibilityListener.removeObserver(observer)

        // Verify that nor keyboard or content size changed and keyboard presents on the screen.
        assertTrue(keyboardFrames.emptyOrAllEqual())
        assertTrue(contentFrames.emptyOrAllEqual())
        assertNotEquals(0.dp, keyboardHeight)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testRefocusProgrammaticallyKeyboardSizeNotChanges() = runUIKitInstrumentedTest {
        val focusRequester1 = FocusRequester()
        val focusRequester2 = FocusRequester()
        val keyboardFrames = mutableListOf<DpRect>()
        val contentFrames = mutableListOf<DpRect>()
        val observer = object : KeyboardVisibilityObserver {
            override fun keyboardWillShow(
                targetFrame: CValue<CGRect>,
                duration: Double,
                animationOptions: UIViewAnimationOptions
            ) {
            }

            override fun keyboardWillHide(
                targetFrame: CValue<CGRect>,
                duration: Double,
                animationOptions: UIViewAnimationOptions
            ) {
            }

            override fun keyboardWillChangeFrame(
                targetFrame: CValue<CGRect>,
                duration: Double,
                animationOptions: UIViewAnimationOptions
            ) {
                keyboardFrames.add(targetFrame.asDpRect())
            }
        }
        KeyboardVisibilityListener.addObserver(observer)

        setContent {
            Column(modifier = Modifier.fillMaxSize().imePadding().onGloballyPositioned {
                contentFrames.add(it.boundsInRoot().toDpRect(density))
            }) {
                Spacer(Modifier.weight(100f))
                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester1)
                )
                TextField(
                    state = rememberTextFieldState(),
                    modifier = Modifier.focusRequester(focusRequester2)
                )
            }
        }

        focusRequester1.requestFocus()
        waitForIdle()

        assertFalse(keyboardFrames.isEmpty(), "No keyboard frame changes detected")
        assertFalse(contentFrames.emptyOrAllEqual(), "No content size changes due to ime height changes detected")
        // Remove frame changes during the keyboard appearance
        keyboardFrames.clear()
        contentFrames.clear()

        // Switch between text fields
        focusRequester2.requestFocus()
        waitForIdle()
        focusRequester1.requestFocus()
        waitForIdle()
        KeyboardVisibilityListener.removeObserver(observer)

        // Verify that nor keyboard or content size changed and keyboard presents on the screen.
        assertTrue(keyboardFrames.emptyOrAllEqual())
        assertTrue(contentFrames.emptyOrAllEqual())
        assertNotEquals(0.dp, keyboardHeight)
    }

    @Test
    fun testFocusableAboveKeyboardLargeTextField() = runUIKitInstrumentedTest {
        var lastTextFieldFrame = DpRect(DpOffset.Unspecified, DpSize.Unspecified)
        val topOffset = 100.dp

        setContent({
            onFocusBehavior = OnFocusBehavior.FocusableAboveKeyboard
        }) {
            val focusRequester = remember { FocusRequester() }
            BasicTextField(
                value = "test Focusable AboveKeyboard Large Text Field".repeat(200),
                onValueChange = {},
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .padding(top = topOffset)
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        lastTextFieldFrame = coordinates.boundsInWindow().toDpRect(density)
                    }
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        val expectedTextField = DpRect(
            left = 0.dp,
            top = 0.dp,
            right = screenSize.width,
            bottom = screenSize.height - min(topOffset, keyboardHeight)
        )
        assertEquals(expectedTextField, lastTextFieldFrame)
    }

    @Test
    fun testFocusableAboveKeyboardWithIMEInsetsLargeTextField() = runUIKitInstrumentedTest {
        var lastTextFieldFrame = DpRect(DpOffset.Unspecified, DpSize.Unspecified)
        val topOffset = 100.dp

        setContent({
            onFocusBehavior = OnFocusBehavior.FocusableAboveKeyboard
        }) {
            val focusRequester = remember { FocusRequester() }
            BasicTextField(
                value = "test Focusable AboveKeyboard Large Text Field".repeat(200),
                onValueChange = {},
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxSize()
                    .padding(top = topOffset)
                    .imePadding()
                    .onGloballyPositioned { coordinates ->
                        lastTextFieldFrame = coordinates.boundsInWindow().toDpRect(density)
                    }
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        val expectedTextField = DpRect(
            left = 0.dp,
            top = topOffset,
            right = screenSize.width,
            bottom = screenSize.height - keyboardHeight
        )
        assertEquals(expectedTextField, lastTextFieldFrame)
    }

    @Test
    fun testFocusBehaviorDoNothingLargeTextField() = runUIKitInstrumentedTest {
        var lastTextFieldFrame = DpRect(DpOffset.Unspecified, DpSize.Unspecified)

        setContent({
            onFocusBehavior = OnFocusBehavior.DoNothing
        }) {
            val focusRequester = remember { FocusRequester() }
            BasicTextField(
                value = "test Focusable AboveKeyboard Large Text Field".repeat(200),
                onValueChange = {},
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        lastTextFieldFrame = coordinates.boundsInWindow().toDpRect(density)
                    }
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        val expectedTextField = DpRect(
            left = 0.dp,
            top = 0.dp,
            right = screenSize.width,
            bottom = screenSize.height
        )
        assertEquals(expectedTextField, lastTextFieldFrame)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun testFocusableAboveKeyboardInModalBottomSheet() = runUIKitInstrumentedTest {
        var lastTextFieldFrame = DpRect(DpOffset.Unspecified, DpSize.Unspecified)

        setContent({
            onFocusBehavior = OnFocusBehavior.FocusableAboveKeyboard
        }) {
            val focusRequester = remember { FocusRequester() }
            ModalBottomSheet(
                onDismissRequest = {},
                contentWindowInsets = { WindowInsets.ime }
            ) {
                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onGloballyPositioned { coordinates ->
                            lastTextFieldFrame = coordinates.boundsInWindow().toDpRect(density)
                        }
                )
            }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        assertEquals(screenSize.height - keyboardHeight, lastTextFieldFrame.bottom)
    }
}

private fun List<*>.emptyOrAllEqual(): Boolean {
    var allItemsEqual = true
    forEachWithPrevious { prev, next ->
        if (prev != next) {
            allItemsEqual = false
        }
    }
    return allItemsEqual
}
