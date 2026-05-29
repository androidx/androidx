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

package androidx.compose.ui.interaction

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.assertVisibleInContainer
import androidx.compose.ui.test.findNodeWithLabel
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.firstNodeOrNull
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.hold
import androidx.compose.ui.test.utils.up
import androidx.compose.ui.test.waitForContextMenu
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.ExperimentalForeignApi
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.UIKit.UIPasteboard

class TextFieldEditMenuTest {
    @Test
    fun testBasicTextFieldToolbar() = runContextMenuTest(false) {
        UIPasteboard.generalPasteboard().string = "Paste text"
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField("Hello-LongLongLongLongLongLong-text", {}, modifier = Modifier.testTag("TextField"))
            }
        }

        openToolbar(textFieldTag = "TextField")

        verifyFullToolbarPresent()
    }

    @Test
    fun testBasicTextField2Toolbar() = runContextMenuTest(false) {
        UIPasteboard.generalPasteboard().string = "Paste text"
        val textFieldState = TextFieldState("Hello-LongLongLongLongLongLong-text")
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(textFieldState, modifier = Modifier.testTag("TextField"))
            }
        }

        openToolbar(textFieldTag = "TextField")

        verifyFullToolbarPresent()
    }

    @Test
    fun testBasicTextFieldToolbarNewContextMenu() = runContextMenuTest(true) {
        UIPasteboard.generalPasteboard().string = "Paste text"
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                TextField("Hello-LongLongLongLongLong-text", {}, modifier = Modifier.testTag("TextField"))
            }
        }

        openToolbar(textFieldTag = "TextField")

        verifyFullToolbarPresent()
    }

    @Test
    fun testBasicTextField2ToolbarNewContextMenu() = runContextMenuTest(true) {
        UIPasteboard.generalPasteboard().string = "Paste text"
        val textFieldState = TextFieldState("Hello-LongLongLongLongLongLong-text")
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(textFieldState, modifier = Modifier.testTag("TextField"))
            }
        }

        openToolbar(textFieldTag = "TextField")

        verifyFullToolbarPresent()
    }

    @Test
    fun testBasicTextFieldToolbarInteraction() = runUIKitInstrumentedTest {
        val textFieldValue = mutableStateOf(TextFieldValue("Hello-LongLongLongLongLongLong-text"))
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(
                    value = textFieldValue.value,
                    onValueChange = { textFieldValue.value = it },
                    modifier = Modifier.testTag("TextField")
                )
            }
        }

        fun MutableState<TextFieldValue>.isFullySelected(): Boolean =
            value.selection.start == 0 && value.selection.end == value.text.length

        openToolbar(textFieldTag = "TextField")

        waitForContextMenu()
        assertFalse(textFieldValue.isFullySelected())

        tapContextMenuButton("Select All")

        waitUntil("Text field should be fully selected") {
            textFieldValue.isFullySelected()
        }
    }

    @Test
    fun testBasicTextField2ToolbarInteraction() = runUIKitInstrumentedTest {
        val textFieldState = TextFieldState("Hello-LongLongLongLongLongLong-text")
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(textFieldState, modifier = Modifier.testTag("TextField"))
            }
        }

        fun TextFieldState.isFullySelected(): Boolean =
            selection.start == 0 && selection.end == text.length

        openToolbar(textFieldTag = "TextField")

        waitForContextMenu()
        assertFalse(textFieldState.isFullySelected())

        tapContextMenuButton("Select All")

        waitUntil("Text field should be fully selected") {
            textFieldState.isFullySelected()
        }
    }

    @Test
    fun testTapsCountingWithMultiTouch() = runUIKitInstrumentedTest {
        var touchesDown = 0
        var touchesUp = 0

        setContent {
            Column {
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .testTag("Box 1")
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    event.changes.forEach { change ->
                                        if (change.changedToDown()) {
                                            touchesDown++
                                        } else if (change.changedToUp()) {
                                            touchesUp++
                                        }
                                    }
                                }
                            }
                        }
                )
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .testTag("Box 2")
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                while (true) {
                                    awaitPointerEvent(pass = PointerEventPass.Initial)
                                }
                            }
                        }
                )
            }
        }

        val tap1 = findNodeWithTag("Box 1").touchDown()
        val tap2 = findNodeWithTag("Box 2").touchDown()

        assertEquals(1, touchesDown)
        assertEquals(0, touchesUp)

        tap1.dragBy(dx = 20.dp, duration = 0.1.seconds)
        tap2.dragBy(dx = 20.dp, duration = 0.1.seconds)

        tap1.up()
        tap2.up()
        waitForIdle()

        assertEquals(1, touchesDown)
        assertEquals(1, touchesUp)
    }

    @Test
    fun testComposePanelClearFocusOnMouseDownEnabledFlag() = runUIKitInstrumentedTest {
        val focusRequester = FocusRequester()
        var textFieldIsFocused = false

        setContent(
            configure = {
                isClearFocusOnMouseDownEnabled = true
            }
        ) {
            Column {
                BasicTextField(
                    state = rememberTextFieldState(),
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            textFieldIsFocused = it.isFocused
                        }
                )
                Box(Modifier.size(100.dp).testTag("box"))
            }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        assertTrue(textFieldIsFocused)

        // No focus changes after tap
        findNodeWithTag("box").tap()
        waitForIdle()
        assertTrue(textFieldIsFocused)

        // Clear focus on a click
        findNodeWithTag("box").click()
        waitForIdle()
        assertFalse(textFieldIsFocused)
    }

    @Test
    fun testComposePanelClearFocusOnMouseDownDisabledFlag() = runUIKitInstrumentedTest {
        val focusRequester = FocusRequester()
        var textFieldIsFocused = false

        setContent(
            configure = {
                isClearFocusOnMouseDownEnabled = false
            }
        ) {
            Column {
                BasicTextField(
                    state = rememberTextFieldState(),
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            textFieldIsFocused = it.isFocused
                        }
                )
                Box(Modifier.size(100.dp).testTag("box"))
            }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        assertTrue(textFieldIsFocused)

        // No focus changes after tap
        findNodeWithTag("box").tap()
        waitForIdle()
        assertTrue(textFieldIsFocused)

        // No focus changes after click
        findNodeWithTag("box").click()
        waitForIdle()
        assertTrue(textFieldIsFocused)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun testBasicTextFieldCustomContextMenuItems() = runContextMenuTest(newContextMenuEnabled = true) {
        var customItemClicked = false
        val textFieldValue = mutableStateOf(TextFieldValue("Hello-LongLongLongLongLongLong-text"))
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(
                    value = textFieldValue.value,
                    onValueChange = { textFieldValue.value = it },
                    modifier = Modifier
                        .testTag("TextField")
                        .appendTextContextMenuComponents {
                            item(key = "CustomKey", label = "Custom Action") {
                                customItemClicked = true
                                close()
                            }
                        }
                        .filterTextContextMenuComponents { component ->
                            // Remove all other items to make sure the custom action won't be hidden
                            component.key == "CustomKey"
                        }
                )
            }
        }

        openToolbar(textFieldTag = "TextField")

        findNodeWithLabel("Custom Action").let {
            it.assertVisibleInContainer()
            assertTrue(it.isAccessibilityElement ?: false)
        }

        // Tap custom item and verify it was clicked
        tapContextMenuButton("Custom Action")
        waitUntil("Custom item should be clicked") {
            customItemClicked
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun testBasicTextField2CustomContextMenuItems() = runContextMenuTest(newContextMenuEnabled = true) {
        var customItemClicked = false
        val textFieldState = TextFieldState("Hello-LongLongLongLongLongLong-text")
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(
                    state = textFieldState,
                    modifier = Modifier
                        .testTag("TextField")
                        .appendTextContextMenuComponents {
                            item(key = "CustomKey", label = "Custom Action") {
                                customItemClicked = true
                                close()
                            }
                        }
                        .filterTextContextMenuComponents { component ->
                            // Remove all other items to make sure the custom action won't be hidden
                            component.key == "CustomKey"
                        }
                )
            }
        }

        openToolbar(textFieldTag = "TextField")

        findNodeWithLabel("Custom Action").let {
            it.assertVisibleInContainer()
            assertTrue(it.isAccessibilityElement ?: false)
        }

        // Tap custom item and verify it was clicked
        tapContextMenuButton("Custom Action")
        waitUntil("Custom item should be clicked") {
            customItemClicked
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun testSelectionContainerCustomContextMenuItems() = runContextMenuTest(newContextMenuEnabled = true) {
        var customItemClicked = false
        setContent {
            Column(modifier = Modifier.safeDrawingPadding()) {
                SelectionContainer(
                    modifier = Modifier
                        .testTag("SelectionContainer")
                        .appendTextContextMenuComponents {
                            item(key = "CustomKey", label = "Custom Action") {
                                customItemClicked = true
                                close()
                            }
                        }
                        .filterTextContextMenuComponents { component ->
                            // Remove all other items to make sure the custom action won't be hidden
                            component.key == "CustomKey"
                        }
                ) {
                    Text("Hello-LongLongLongLongLongLong-text")
                }
            }
        }

        openToolbar("SelectionContainer")

        findNodeWithLabel("Custom Action").let {
            it.assertVisibleInContainer()
            assertTrue(it.isAccessibilityElement ?: false)
        }

        // Tap custom item and verify it was clicked
        tapContextMenuButton("Custom Action")
        waitUntil("Custom item should be clicked") {
            customItemClicked
        }
    }

    private fun UIKitInstrumentedTest.openToolbar(textFieldTag: String) {
        findNodeWithTag(textFieldTag).tap()
        delay(500)
        findNodeWithTag(textFieldTag).doubleTap()
        waitForContextMenu()
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun runContextMenuTest(
        newContextMenuEnabled: Boolean,
        testBlock: UIKitInstrumentedTest.() -> Unit
    ) = runUIKitInstrumentedTest {
        val oldValue = ComposeFoundationFlags.isNewContextMenuEnabled
        ComposeFoundationFlags.isNewContextMenuEnabled = newContextMenuEnabled
        try {
            testBlock()
        } finally {
            ComposeFoundationFlags.isNewContextMenuEnabled = oldValue
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun UIKitInstrumentedTest.verifyFullToolbarPresent() {
        findNodeWithLabel("Cut").let {
            it.assertVisibleInContainer()
            assertTrue(it.isAccessibilityElement ?: false)
        }

        findNodeWithLabel("Copy").let {
            it.assertVisibleInContainer()
            assertTrue(it.isAccessibilityElement ?: false)
        }

        findNodeWithLabel("Paste").let {
            it.assertVisibleInContainer()
            assertTrue(it.isAccessibilityElement ?: false)
        }

        findNodeWithLabel("Select All").let {
            it.assertVisibleInContainer()
            assertTrue(it.isAccessibilityElement ?: false)
        }
    }

    private fun UIKitInstrumentedTest.tapContextMenuButton(label: String) {
        if (available(OS.Ios to OSVersion(16))) {
            findNodeWithLabel(label).tap()
        } else {
            // Because on iOS < 16 the context menu is shown in a separate window,
            // it's not fully interactive with the default Tap action.
            findNodeWithLabel(label)
                .touchDown(useNodeWindow = true)
                .hold()
                .also { delay(100) }
                .up()
        }
    }
}