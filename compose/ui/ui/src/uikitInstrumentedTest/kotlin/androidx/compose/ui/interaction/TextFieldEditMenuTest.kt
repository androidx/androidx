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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.test.findNodeWithLabelOrNull
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.findFirstDescendant
import androidx.compose.ui.test.utils.hold
import androidx.compose.ui.test.utils.isLoupeView
import androidx.compose.ui.test.utils.up
import androidx.compose.ui.test.waitForContextMenu
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
        val textValue = mutableStateOf(TextFieldValue("Hello-LongLongLongLongLongLong-text"))
        setContent {
            val focusRequester = remember { FocusRequester() }
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(
                    textValue.value,
                    { textValue.value = it },
                    modifier = textFieldModifier(focusRequester)
                )
            }
            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
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
            val focusRequester = remember { FocusRequester() }
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(
                    textFieldState,
                    modifier = textFieldModifier(focusRequester)
                )
            }
            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
            }
        }

        openToolbar(textFieldTag = "TextField")

        verifyFullToolbarPresent()
    }

    @Test
    fun testBasicTextFieldToolbarNewContextMenu() = runContextMenuTest(true) {
        UIPasteboard.generalPasteboard().string = "Paste text"
        setContent {
            val focusRequester = remember { FocusRequester() }
            Column(modifier = Modifier.safeDrawingPadding()) {
                TextField("Hello-LongLongLongLongLong-text", {}, modifier = textFieldModifier(focusRequester))
            }
            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
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
            val focusRequester = remember { FocusRequester() }
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(textFieldState, modifier = textFieldModifier(focusRequester))
            }
            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
            }
        }

        openToolbar(textFieldTag = "TextField")

        verifyFullToolbarPresent()
    }

    @Test
    fun testBasicTextFieldToolbarInteraction() = runUIKitInstrumentedTest {
        val textFieldValue = mutableStateOf(TextFieldValue("Hello-LongLongLongLongLongLong-text"))
        setContent {
            val focusRequester = remember { FocusRequester() }
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(
                    value = textFieldValue.value,
                    onValueChange = { textFieldValue.value = it },
                    modifier = textFieldModifier(focusRequester)
                )
            }
            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
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
            val focusRequester = remember { FocusRequester() }
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(textFieldState, modifier = textFieldModifier(focusRequester))
            }
            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
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
    fun testBasicTextFieldLongPressShowsContextMenu() = runUIKitInstrumentedTest {
        UIPasteboard.generalPasteboard().string = "Paste text"
        val textFieldValue = mutableStateOf(TextFieldValue("Text", TextRange(4,4)))
        setContent {
            val focusRequester = remember { FocusRequester() }
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(
                    value = textFieldValue.value,
                    onValueChange = { textFieldValue.value = it },
                    modifier = textFieldModifier(focusRequester)
                )
            }
            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
            }
        }

        // A long press positions the cursor and, on release, reveals the context menu.
        longPressAndAwaitContextMenu("TextField")

        waitForContextMenu()
        findNodeWithLabel("Paste").assertVisibleInContainer()

        // A short tap elsewhere dismisses the context menu.
        findNodeWithTag("TextField").tap()
        waitUntil("Context menu should be hidden") {
            findNodeWithLabelOrNull("Paste") == null
        }

        // A tap again brings the context menu back.
        longPressAndAwaitContextMenu("TextField")
        findNodeWithLabel("Paste").assertVisibleInContainer()
    }

    @Test
    fun testBasicTextField2LongPressShowsContextMenu() = runUIKitInstrumentedTest {
        UIPasteboard.generalPasteboard().string = "Paste text"
        val textFieldState = TextFieldState("Text", TextRange(4,4))
        setContent {
            val focusRequester = remember { FocusRequester() }
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(
                    state = textFieldState,
                    modifier = textFieldModifier(focusRequester)
                )
            }
            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
            }
        }

        // A long press positions the cursor and, on release, reveals the context menu.
        longPressAndAwaitContextMenu("TextField")
        findNodeWithLabel("Paste").assertVisibleInContainer()

        // A short tap elsewhere dismisses the context menu.
        findNodeWithTag("TextField").tap()
        waitUntil("Context menu should be hidden") {
            findNodeWithLabelOrNull("Paste") == null
        }

        // A long press again brings the context menu back.
        longPressAndAwaitContextMenu("TextField")
        findNodeWithLabel("Paste").assertVisibleInContainer()
    }

    @Test
    fun testEditableCollapsedClipboardText() =
        runComplexTextFieldTest { textFieldKind, newContextMenu ->
            UIPasteboard.generalPasteboard().string = "Paste text"
            setTextFieldContent(
                textFieldKind = textFieldKind,
                initialValue = TextFieldValue("Text", TextRange(4, 4)),
                readOnly = false
            )

            longPressAndAwaitContextMenu("TextField")
            verifyContextMenuItemsVisible(
                labels = if (newContextMenu) {
                    listOf("Paste", "Select All")
                } else {
                    listOf("Paste", "Select", "Select All")
                }
            )

            verifyContextMenuItemsHidden(
                labels = if (newContextMenu) {
                    listOf("Cut", "Copy", "Select")
                } else {
                    listOf("Cut", "Copy")
                }
            )
        }

    private fun runComplexTextFieldTest(test: UIKitInstrumentedTest.(EditableTextFieldKind, newContextMenuEnabled: Boolean) -> Unit) {
        for (newContextMenuEnabled in arrayOf(false, true)) {
            for (textFieldKind in EditableTextFieldKind.entries) {
                runContextMenuTest(newContextMenuEnabled) {
                    test(textFieldKind, newContextMenuEnabled)
                }
            }
        }
    }

    @Test
    fun testEditableCollapsedClipboardEmpty() =
        runComplexTextFieldTest { textFieldKind, newContextMenu ->
            UIPasteboard.generalPasteboard().string = null
            setTextFieldContent(
                textFieldKind = textFieldKind,
                initialValue = TextFieldValue("Text", TextRange(4, 4)),
                readOnly = false
            )

            longPressAndAwaitContextMenu("TextField")
            verifyContextMenuItemsVisible(
                labels = if (newContextMenu) {
                    listOf("Select All")
                } else {
                    listOf("Select", "Select All")
                }
            )

            verifyContextMenuItemsHidden(
                labels = if (newContextMenu) {
                    listOf("Cut", "Copy", "Paste", "Select")
                } else {
                    listOf("Cut", "Copy", "Paste")
                }
            )
        }

    @Test
    fun testEditablePartialSelectionClipboardText() =
        runComplexTextFieldTest { textFieldKind, _ ->
            UIPasteboard.generalPasteboard().string = "Paste text"
            setTextFieldContent(
                textFieldKind = textFieldKind,
                initialValue = TextFieldValue(PARTIAL_SELECTION_TEXT),
                readOnly = false
            )

            openToolbar("TextField")
            verifyContextMenuItemsVisible(labels = listOf("Cut", "Copy", "Paste", "Select All"))
            verifyContextMenuItemsHidden(labels = listOf("Select"))
        }

    @Test
    fun testEditableFullSelectionClipboardTextBasicTextField() {
        for (newContextMenuEnabled in arrayOf(false, true)) {
            runEditableFullSelectionClipboardTextTest(newContextMenuEnabled) {
                val textFieldValue = mutableStateOf(TextFieldValue("Text", TextRange(4, 4)))
                setContent {
                    val focusRequester = remember { FocusRequester() }
                    Column(modifier = Modifier.safeDrawingPadding()) {
                        BasicTextField(
                            value = textFieldValue.value,
                            onValueChange = { textFieldValue.value = it },
                            modifier = textFieldModifier(focusRequester)
                        )
                    }
                    LaunchedEffect(focusRequester) {
                        focusRequester.requestFocus()
                    }
                }

                val isFullySelected = {
                    val selection = textFieldValue.value.selection
                    selection.start == 0 && selection.end == textFieldValue.value.text.length
                }
                isFullySelected
            }
        }
    }

    @Test
    fun testEditableFullSelectionClipboardTextBasicTextField2OldContextMenu() =
        runEditableFullSelectionClipboardTextTest(newContextMenuEnabled = false) {
            runEditableFullSelectionClipboardTextBasicTextField2()
        }

    @Test
    @Ignore // CMP-10301: Menu is not shown after tap on Select All
    fun testEditableFullSelectionClipboardTextBasicTextField2NewContextMenu() =
        runEditableFullSelectionClipboardTextTest(newContextMenuEnabled = true) {
            runEditableFullSelectionClipboardTextBasicTextField2()
        }

    private fun UIKitInstrumentedTest.runEditableFullSelectionClipboardTextBasicTextField2(): () -> Boolean {
        val textFieldState = TextFieldState("Text", TextRange(4, 4))
        setContent {
            val focusRequester = remember { FocusRequester() }
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(
                    state = textFieldState,
                    modifier = textFieldModifier(focusRequester)
                )
            }
            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
            }
        }

        return {
            val selection = textFieldState.selection
            selection.start == 0 && selection.end == textFieldState.text.length
        }
    }

    private fun runEditableFullSelectionClipboardTextTest(
        newContextMenuEnabled: Boolean,
        setContentAndGetIsFullySelected: UIKitInstrumentedTest.() -> () -> Boolean
    ) =
        runContextMenuTest(newContextMenuEnabled) {
            UIPasteboard.generalPasteboard().string = "Paste text"
            val isFullySelected = setContentAndGetIsFullySelected()

            longPressAndAwaitContextMenu("TextField")
            tapContextMenuButton("Select All")
            waitUntil("Text field should be fully selected") {
                isFullySelected()
            }

            val visible = listOf("Cut", "Copy", "Paste")
            val hidden = listOf("Select", "Select All")

            waitUntil("Context menu should update for full selection") {
                visible.all { findNodeWithLabelOrNull(it) != null } &&
                    hidden.all { findNodeWithLabelOrNull(it) == null }
            }

            verifyContextMenuItemsVisible(labels = visible)
            verifyContextMenuItemsHidden(labels = hidden)
        }

    @Test
    fun testReadOnlyCollapsedClipboardText() =
        runComplexTextFieldTest { textFieldKind, _ ->
            UIPasteboard.generalPasteboard().string = "Paste text"
            setTextFieldContent(
                textFieldKind = textFieldKind,
                initialValue = TextFieldValue("Text", TextRange(4, 4)),
                readOnly = true
            )

            longPressAndAwaitContextMenu("TextField")
            verifyContextMenuItemsVisible(labels = listOf("Select All"))
            verifyContextMenuItemsHidden(labels = listOf("Cut", "Copy", "Paste", "Select"))
        }

    @Test
    fun testReadOnlyPartialSelectionClipboardText() =
        runComplexTextFieldTest { textFieldKind, _ ->
            UIPasteboard.generalPasteboard().string = "Paste text"
            setTextFieldContent(
                textFieldKind = textFieldKind,
                initialValue = TextFieldValue(PARTIAL_SELECTION_TEXT),
                readOnly = true
            )

            openToolbar("TextField")
            verifyContextMenuItemsVisible(labels = listOf("Copy", "Select All"))
            verifyContextMenuItemsHidden(labels = listOf("Cut", "Paste", "Select"))
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
            val focusRequester = remember { FocusRequester() }
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(
                    value = textFieldValue.value,
                    onValueChange = { textFieldValue.value = it },
                    modifier = textFieldModifier(focusRequester)
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
            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
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
            val focusRequester = remember { FocusRequester() }
            Column(modifier = Modifier.safeDrawingPadding()) {
                BasicTextField(
                    state = textFieldState,
                    modifier = textFieldModifier(focusRequester)
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
            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
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

    private fun textFieldModifier(focusRequester: FocusRequester): Modifier =
        Modifier
            .testTag("TextField")
            .focusRequester(focusRequester)

    private fun UIKitInstrumentedTest.longPressAndAwaitContextMenu(textFieldTag: String) {
        val touch = findNodeWithTag(textFieldTag).touchDown()
        waitUntil {
            findFirstDescendant { it.isLoupeView } != null
        }
        touch.up()
        waitForContextMenu()
    }

    private fun UIKitInstrumentedTest.setTextFieldContent(
        textFieldKind: EditableTextFieldKind,
        initialValue: TextFieldValue,
        readOnly: Boolean,
    ) {
        setContent {
            val focusRequester = remember { FocusRequester() }
            Column(modifier = Modifier.safeDrawingPadding()) {
                when (textFieldKind) {
                    EditableTextFieldKind.BasicTextField -> {
                        val textFieldValue = remember {
                            mutableStateOf(initialValue)
                        }
                        BasicTextField(
                            value = textFieldValue.value,
                            onValueChange = { textFieldValue.value = it },
                            modifier = textFieldModifier(focusRequester),
                            readOnly = readOnly
                        )
                    }
                    EditableTextFieldKind.BasicTextField2 -> {
                        val textFieldState = remember {
                            TextFieldState(initialValue.text, initialValue.selection)
                        }
                        BasicTextField(
                            state = textFieldState,
                            modifier = textFieldModifier(focusRequester),
                            readOnly = readOnly
                        )
                    }
                }
            }
            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
            }
        }
    }

    private enum class EditableTextFieldKind {
        BasicTextField,
        BasicTextField2
    }

    private companion object {
        private const val PARTIAL_SELECTION_TEXT = "accomplishment extraordinary magnificent establishment"
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
    private fun UIKitInstrumentedTest.verifyContextMenuItemsVisible(labels: List<String>) {
        labels.forEach { label ->
            findNodeWithLabel(label).let {
                it.assertVisibleInContainer()
                assertTrue(it.isAccessibilityElement ?: false)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class) private fun UIKitInstrumentedTest.verifyContextMenuItemsHidden(labels: List<String>) {
        labels.forEach { label ->
            assertNull(
                findNodeWithLabelOrNull(label),
                "Context menu item \"$label\" should be hidden"
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun UIKitInstrumentedTest.verifyFullToolbarPresent() {
        verifyContextMenuItemsVisible(listOf("Cut", "Copy", "Paste", "Select All"))
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
