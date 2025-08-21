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

package androidx.compose.foundation.textfield

import androidx.compose.foundation.contextmenu.ContextMenuItemLabels
import androidx.compose.foundation.contextmenu.ContextMenuItemState
import androidx.compose.foundation.contextmenu.ProcessTextItemOverrideRule
import androidx.compose.foundation.contextmenu.assertContextMenuItem
import androidx.compose.foundation.contextmenu.assertContextMenuItems
import androidx.compose.foundation.contextmenu.clickOffPopup
import androidx.compose.foundation.contextmenu.contextMenuItemInteraction
import androidx.compose.foundation.internal.readText
import androidx.compose.foundation.internal.toClipEntry
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.PlatformSelectionBehaviorsRule
import androidx.compose.foundation.text.contextmenu.ProcessTextApi23Impl
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagFlipperRunner
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagSuppress
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.internal.selection.FakeClipboard
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.lerp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(ContextMenuFlagFlipperRunner::class)
class TextFieldContextMenuTest : FocusedWindowTest {
    @get:Rule val rule = createComposeRule()

    private val textFieldTag = "BTF"
    private val defaultFullWidthText = "M".repeat(20)

    @get:Rule
    val processTextRule =
        ProcessTextItemOverrideRule(
            ContextMenuItemLabels.PROCESS_TEXT_1,
            ContextMenuItemLabels.PROCESS_TEXT_2,
        )

    @get:Rule val platformSelectionBehaviorsRule = PlatformSelectionBehaviorsRule()

    // region BTF1 Context Menu Gesture Tests
    @Test
    fun btf1_contextMenu_rightClick_appears() {
        var value by mutableStateOf(TextFieldValue(defaultFullWidthText))
        rule.setTextFieldTestContent {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.testTag(textFieldTag),
            )
        }

        val contextMenuInteraction = rule.onNode(isPopup())
        contextMenuInteraction.assertDoesNotExist()
        rule.onNodeWithTag(textFieldTag).performMouseInput { rightClick(center) }
        contextMenuInteraction.assertExists()
    }

    @Test
    fun btf1_contextMenu_leftClick_doesNotAppear() {
        var value by mutableStateOf(TextFieldValue(defaultFullWidthText))
        rule.setTextFieldTestContent {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.testTag(textFieldTag),
            )
        }

        val contextMenuInteraction = rule.onNode(isPopup())
        contextMenuInteraction.assertDoesNotExist()
        rule.onNodeWithTag(textFieldTag).performMouseInput { click(center) }
        contextMenuInteraction.assertDoesNotExist()
    }

    @Test
    fun btf1_contextMenu_disabled_rightClick_doesNotAppear() {
        var value by mutableStateOf(TextFieldValue(defaultFullWidthText))
        rule.setTextFieldTestContent {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                enabled = false,
                modifier = Modifier.testTag(textFieldTag),
            )
        }

        val contextMenuInteraction = rule.onNode(isPopup())
        contextMenuInteraction.assertDoesNotExist()
        rule.onNodeWithTag(textFieldTag).performMouseInput { rightClick(center) }
        contextMenuInteraction.assertDoesNotExist()
    }

    @Test
    fun btf1_contextMenu_disappearsOnClickOffOfPopup() {
        var value by mutableStateOf(TextFieldValue(defaultFullWidthText))
        rule.setTextFieldTestContent {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.testTag(textFieldTag),
            )
        }

        val contextMenuInteraction = rule.onNode(isPopup())
        contextMenuInteraction.assertDoesNotExist()
        rule.onNodeWithTag(textFieldTag).performMouseInput { rightClick(center) }
        contextMenuInteraction.assertExists()
        rule.clickOffPopup { rootRect -> lerp(rootRect.topLeft, rootRect.center, 0.5f) }
        contextMenuInteraction.assertDoesNotExist()
    }

    // endregion BTF1 Context Menu Gesture Tests

    // region BTF1 Context Menu Item Click Tests
    @Test
    fun btf1_contextMenu_onClickCut() =
        runBtf1ClickContextMenuItemTest(
            labelToClick = ContextMenuItemLabels.CUT,
            expectedText = "Text  Text",
            expectedSelection = TextRange(5),
            expectedClipboardContent = "Text",
        )

    @Test
    fun btf1_contextMenu_onClickCopy() =
        runBtf1ClickContextMenuItemTest(
            labelToClick = ContextMenuItemLabels.COPY,
            expectedText = "Text Text Text",
            expectedSelection = TextRange(5, 9),
            expectedClipboardContent = "Text",
        )

    @Test
    fun btf1_contextMenu_onClickPaste() =
        runBtf1ClickContextMenuItemTest(
            labelToClick = ContextMenuItemLabels.PASTE,
            expectedText = "Text clip Text",
            expectedSelection = TextRange(9),
            expectedClipboardContent = "clip",
        )

    @Test
    fun btf1_contextMenu_onClickSelectAll() =
        runBtf1ClickContextMenuItemTest(
            labelToClick = ContextMenuItemLabels.SELECT_ALL,
            expectedText = "Text Text Text",
            expectedSelection = TextRange(0, 14),
            expectedClipboardContent = "clip",
        )

    @Test
    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    fun btf1_contextMenu_onClickProcessText() {
        var textToProcess: String? = null
        ProcessTextApi23Impl.onClickProcessTextItem = { _, _, editable, text, selection ->
            // editable is always false for SelectionContainer.
            assertThat(editable).isTrue()
            textToProcess = text.subSequence(selection.start, selection.end).toString()
        }

        runBtf1ClickContextMenuItemTest(
            labelToClick = ContextMenuItemLabels.PROCESS_TEXT_1,
            text = "abc def ghi",
            selection = TextRange(4, 7),
        ) { _, _ ->
            assertThat(textToProcess).isEqualTo("def")
        }
    }

    private fun runBtf1ClickContextMenuItemTest(
        labelToClick: String,
        expectedText: String,
        expectedSelection: TextRange,
        expectedClipboardContent: String,
    ) =
        runBtf1ClickContextMenuItemTest(labelToClick) { value, clipboard ->
            // Operation was applied
            assertThat(value.text).isEqualTo(expectedText)
            assertThat(value.selection).isEqualTo(expectedSelection)
            val clipboardContent = clipboard.getClipEntry()?.readText()
            assertThat(clipboardContent).isNotNull()
            assertThat(clipboardContent!!).isEqualTo(expectedClipboardContent)
        }

    private fun runBtf1ClickContextMenuItemTest(
        labelToClick: String,
        text: String = "Text Text Text",
        selection: TextRange = TextRange(5, 9),
        assertBlock: suspend (TextFieldValue, Clipboard) -> Unit,
    ) = runTest {
        val initialClipboardText = "clip"

        var value by mutableStateOf(TextFieldValue(text = text, selection = selection))

        val clipboard = FakeClipboard(initialClipboardText)

        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.testTag(textFieldTag),
                )
            }
        }

        rule.onNodeWithTag(textFieldTag).performMouseInput { rightClick(center) }
        val itemInteraction = rule.contextMenuItemInteraction(labelToClick)
        itemInteraction.assertHasClickAction()
        itemInteraction.assertIsEnabled()
        itemInteraction.performClick()
        rule.waitForIdle()

        // Context menu disappears
        rule.onNode(isPopup()).assertDoesNotExist()
        itemInteraction.assertDoesNotExist()

        assertBlock(value, clipboard)
    }

    // endregion BTF1 Context Menu Item Click Tests

    // region BTF1 Context Menu Correct Item Tests
    @Test
    @SdkSuppress(maxSdkVersion = 25)
    fun btf1_contextMenu_emptyClipboard_noSelection_itemsMatch_beforeApi26() = runTest {
        runBtf1CorrectItemsTest(isEmptyClipboard = true, selectionAmount = SelectionAmount.NONE) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun btf1_contextMenu_emptyClipboard_noSelection_itemsMatch_afterApi26() = runTest {
        runBtf1CorrectItemsTest(isEmptyClipboard = true, selectionAmount = SelectionAmount.NONE) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.ENABLED,
            )
        }
    }

    @Test
    fun btf1_contextMenu_emptyClipboard_partialSelection_itemsMatch() = runTest {
        runBtf1CorrectItemsTest(
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.PARTIAL,
        ) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.ENABLED,
                copyState = ContextMenuItemState.ENABLED,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }
    }

    @Test
    fun btf1_contextMenu_emptyClipboard_fullSelection_itemsMatch() = runTest {
        runBtf1CorrectItemsTest(isEmptyClipboard = true, selectionAmount = SelectionAmount.ALL) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.ENABLED,
                copyState = ContextMenuItemState.ENABLED,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.DOES_NOT_EXIST,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    fun btf1_contextMenu_nonEmptyClipboard_noSelection_itemsMatch_beforeApi26() = runTest {
        runBtf1CorrectItemsTest(isEmptyClipboard = false, selectionAmount = SelectionAmount.NONE) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.ENABLED,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun btf1_contextMenu_nonEmptyClipboard_noSelection_itemsMatch_afterApi26() = runTest {
        runBtf1CorrectItemsTest(isEmptyClipboard = false, selectionAmount = SelectionAmount.NONE) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.ENABLED,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.ENABLED,
            )
        }
    }

    @Test
    fun btf1_contextMenu_nonEmptyClipboard_partialSelection_itemsMatch() = runTest {
        runBtf1CorrectItemsTest(
            isPassword = false,
            isReadOnly = false,
            isEmptyClipboard = false,
            selectionAmount = SelectionAmount.PARTIAL,
        ) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.ENABLED,
                copyState = ContextMenuItemState.ENABLED,
                pasteState = ContextMenuItemState.ENABLED,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }
    }

    @Test
    fun btf1_contextMenu_nonEmptyClipboard_fullSelection_itemsMatch() = runTest {
        runBtf1CorrectItemsTest(isEmptyClipboard = false, selectionAmount = SelectionAmount.ALL) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.ENABLED,
                copyState = ContextMenuItemState.ENABLED,
                pasteState = ContextMenuItemState.ENABLED,
                selectAllState = ContextMenuItemState.DOES_NOT_EXIST,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    fun btf1_contextMenu_password_noSelection_itemsMatch_beforeApi26() = runTest {
        runBtf1CorrectItemsTest(isPassword = true, selectionAmount = SelectionAmount.NONE) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.ENABLED,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun btf1_contextMenu_password_noSelection_itemsMatch_afterApi26() = runTest {
        runBtf1CorrectItemsTest(isPassword = true, selectionAmount = SelectionAmount.NONE) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.ENABLED,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.ENABLED,
            )
        }
    }

    @Test
    fun btf1_contextMenu_password_partialSelection_itemsMatch() = runTest {
        runBtf1CorrectItemsTest(isPassword = true, selectionAmount = SelectionAmount.PARTIAL) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.ENABLED,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }
    }

    @Test
    fun btf1_contextMenu_password_fullSelection_itemsMatch() = runTest {
        runBtf1CorrectItemsTest(isPassword = true, selectionAmount = SelectionAmount.ALL) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.ENABLED,
                selectAllState = ContextMenuItemState.DOES_NOT_EXIST,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }
    }

    @Test
    fun btf1_contextMenu_readOnly_noSelection_itemsMatch() = runTest {
        runBtf1CorrectItemsTest(
            isReadOnly = true,
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.NONE,
        ) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }
    }

    @Test
    fun btf1_contextMenu_readOnly_partialSelection_itemsMatch() = runTest {
        runBtf1CorrectItemsTest(
            isReadOnly = true,
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.PARTIAL,
        ) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.ENABLED,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }
    }

    @Test
    fun btf1_contextMenu_readOnly_fullSelection_itemsMatch() = runTest {
        runBtf1CorrectItemsTest(
            isReadOnly = true,
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.ALL,
        ) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.ENABLED,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.DOES_NOT_EXIST,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }
    }

    @Test
    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    fun btf1_contextMenu_processText_itemsMatch() = runTest {
        runBtf1CorrectItemsTest(
            isReadOnly = true,
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.ALL,
        ) {
            rule.assertContextMenuItem(
                label = ContextMenuItemLabels.PROCESS_TEXT_1,
                state = ContextMenuItemState.ENABLED,
            )
            rule.assertContextMenuItem(
                label = ContextMenuItemLabels.PROCESS_TEXT_2,
                state = ContextMenuItemState.ENABLED,
            )
        }
    }

    private suspend fun runBtf1CorrectItemsTest(
        isPassword: Boolean = false,
        isReadOnly: Boolean = false,
        isEmptyClipboard: Boolean = false,
        selectionAmount: SelectionAmount = SelectionAmount.PARTIAL,
        assertBlock: () -> Unit,
    ) {
        val text = "Text Text Text"
        var value by
            mutableStateOf(
                TextFieldValue(
                    text = text,
                    selection =
                        when (selectionAmount) {
                            SelectionAmount.NONE -> TextRange.Zero
                            SelectionAmount.PARTIAL -> TextRange(5, 9)
                            SelectionAmount.ALL -> TextRange(0, 14)
                        },
                )
            )

        val visualTransformation =
            if (isPassword) PasswordVisualTransformation() else VisualTransformation.None

        val clipboard =
            FakeClipboard().apply {
                if (isEmptyClipboard) {
                    setClipEntry(null)
                } else {
                    setClipEntry(AnnotatedString("Clipboard Text").toClipEntry())
                }
            }

        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    visualTransformation = visualTransformation,
                    readOnly = isReadOnly,
                    modifier = Modifier.testTag(textFieldTag),
                )
            }
        }

        rule.onNodeWithTag(textFieldTag).performMouseInput { rightClick(center) }
        assertBlock()
    }

    // endregion BTF1 Context Menu Correct Item Tests

    // region BTF2 Context Menu Gesture Tests
    @Test
    fun btf2_contextMenu_rightClick_appears() {
        val state = TextFieldState(defaultFullWidthText)
        rule.setTextFieldTestContent {
            BasicTextField(state = state, modifier = Modifier.testTag(textFieldTag))
        }

        val contextMenuInteraction = rule.onNode(isPopup())
        contextMenuInteraction.assertDoesNotExist()
        rule.onNodeWithTag(textFieldTag).performMouseInput { rightClick(center) }
        contextMenuInteraction.assertExists()
    }

    @Test
    fun btf2_contextMenu_leftClick_doesNotAppear() {
        val state = TextFieldState(defaultFullWidthText)
        rule.setTextFieldTestContent {
            BasicTextField(state = state, modifier = Modifier.testTag(textFieldTag))
        }

        val contextMenuInteraction = rule.onNode(isPopup())
        contextMenuInteraction.assertDoesNotExist()
        rule.onNodeWithTag(textFieldTag).performMouseInput { click(center) }
        contextMenuInteraction.assertDoesNotExist()
    }

    @Test
    fun btf2_contextMenu_disabled_rightClick_doesNotAppear() {
        val state = TextFieldState(defaultFullWidthText)
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                enabled = false,
                modifier = Modifier.testTag(textFieldTag),
            )
        }

        val contextMenuInteraction = rule.onNode(isPopup())
        contextMenuInteraction.assertDoesNotExist()
        rule.onNodeWithTag(textFieldTag).performMouseInput { rightClick(center) }
        contextMenuInteraction.assertDoesNotExist()
    }

    @Test
    fun btf2_contextMenu_disappearsOnClickOffOfPopup() {
        val state = TextFieldState(defaultFullWidthText)
        rule.setTextFieldTestContent {
            BasicTextField(state = state, modifier = Modifier.testTag(textFieldTag))
        }

        val contextMenuInteraction = rule.onNode(isPopup())
        contextMenuInteraction.assertDoesNotExist()
        rule.onNodeWithTag(textFieldTag).performMouseInput { rightClick(center) }
        contextMenuInteraction.assertExists()
        rule.clickOffPopup { rootRect -> lerp(rootRect.topLeft, rootRect.center, 0.5f) }
        contextMenuInteraction.assertDoesNotExist()
    }

    // endregion BTF2 Context Menu Gesture Tests

    // region BTF2 Context Menu Item Click Tests
    @Test
    fun btf2_contextMenu_onClickCut() = runTest {
        runBtf2ClickContextMenuItemTest(
            labelToClick = ContextMenuItemLabels.CUT,
            expectedText = "Text  Text",
            expectedSelection = TextRange(5),
            expectedClipboardContent = "Text",
        )
    }

    @Test
    fun btf2_contextMenu_onClickCopy() = runTest {
        runBtf2ClickContextMenuItemTest(
            labelToClick = ContextMenuItemLabels.COPY,
            expectedText = "Text Text Text",
            expectedSelection = TextRange(5, 9),
            expectedClipboardContent = "Text",
        )
    }

    @Test
    fun btf2_contextMenu_onClickPaste() = runTest {
        runBtf2ClickContextMenuItemTest(
            labelToClick = ContextMenuItemLabels.PASTE,
            expectedText = "Text clip Text",
            expectedSelection = TextRange(9),
            expectedClipboardContent = "clip",
        )
    }

    @Test
    fun btf2_contextMenu_onClickSelectAll() = runTest {
        runBtf2ClickContextMenuItemTest(
            labelToClick = ContextMenuItemLabels.SELECT_ALL,
            expectedText = "Text Text Text",
            expectedSelection = TextRange(0, 14),
            expectedClipboardContent = "clip",
        )
    }

    @Test
    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    fun btf2_contextMenu_onClickProcessText() {
        var textToProcess: String? = null
        ProcessTextApi23Impl.onClickProcessTextItem = { _, _, editable, text, selection ->
            // editable is always false for SelectionContainer.
            assertThat(editable).isTrue()
            textToProcess = text.subSequence(selection.start, selection.end).toString()
        }

        runTest {
            runBtf2ClickContextMenuItemTest(
                labelToClick = ContextMenuItemLabels.PROCESS_TEXT_1,
                text = "abc def ghi",
                selection = TextRange(4, 7),
            ) { _, _ ->
                assertThat(textToProcess).isEqualTo("def")
            }
        }
    }

    private suspend fun runBtf2ClickContextMenuItemTest(
        labelToClick: String,
        expectedText: String,
        expectedSelection: TextRange,
        expectedClipboardContent: String,
    ) =
        runBtf2ClickContextMenuItemTest(labelToClick) { state, clipboard ->
            // Operation was applied
            assertThat(state.text).isEqualTo(expectedText)
            assertThat(state.selection).isEqualTo(expectedSelection)
            val clipboardContent = clipboard.getClipEntry()?.readText()
            assertThat(clipboardContent).isNotNull()
            assertThat(clipboardContent!!).isEqualTo(expectedClipboardContent)
        }

    private suspend fun runBtf2ClickContextMenuItemTest(
        labelToClick: String,
        text: String = "Text Text Text",
        selection: TextRange = TextRange(5, 9),
        assertBlock: suspend (TextFieldState, Clipboard) -> Unit,
    ) {
        val initialClipboardText = "clip"

        val state = TextFieldState(initialText = text, initialSelection = selection)

        val clipboard = FakeClipboard(initialClipboardText)

        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                BasicTextField(state = state, modifier = Modifier.testTag(textFieldTag))
            }
        }

        rule.onNodeWithTag(textFieldTag).performMouseInput { rightClick(center) }
        val itemInteraction = rule.contextMenuItemInteraction(labelToClick)
        itemInteraction.assertHasClickAction()
        itemInteraction.assertIsEnabled()
        itemInteraction.performClick()
        rule.waitForIdle()

        // Context menu disappears
        rule.onNode(isPopup()).assertDoesNotExist()
        itemInteraction.assertDoesNotExist()

        assertBlock(state, clipboard)
    }

    // endregion BTF2 Context Menu Item Click Tests

    // region BTF2 Context Menu Correct Item Tests
    @Test
    @SdkSuppress(maxSdkVersion = 25)
    fun btf2_contextMenu_emptyClipboard_noSelection_itemsMatch_beforeApi26() =
        runBtf2CorrectItemsTest(isEmptyClipboard = true, selectionAmount = SelectionAmount.NONE) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun btf2_contextMenu_emptyClipboard_noSelection_itemsMatch_afterApi26() =
        runBtf2CorrectItemsTest(isEmptyClipboard = true, selectionAmount = SelectionAmount.NONE) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.ENABLED,
            )
        }

    @Test
    fun btf2_contextMenu_emptyClipboard_partialSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.PARTIAL,
        ) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.ENABLED,
                copyState = ContextMenuItemState.ENABLED,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }

    @Test
    fun btf2_contextMenu_emptyClipboard_fullSelection_itemsMatch() =
        runBtf2CorrectItemsTest(isEmptyClipboard = true, selectionAmount = SelectionAmount.ALL) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.ENABLED,
                copyState = ContextMenuItemState.ENABLED,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.DOES_NOT_EXIST,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    fun btf2_contextMenu_nonEmptyClipboard_noSelection_itemsMatch_beforeApi26() =
        runBtf2CorrectItemsTest(isEmptyClipboard = false, selectionAmount = SelectionAmount.NONE) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.ENABLED,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun btf2_contextMenu_nonEmptyClipboard_noSelection_itemsMatch_afterApi26() =
        runBtf2CorrectItemsTest(isEmptyClipboard = false, selectionAmount = SelectionAmount.NONE) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.ENABLED,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.ENABLED,
            )
        }

    @Test
    fun btf2_contextMenu_nonEmptyClipboard_partialSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = false,
            selectionAmount = SelectionAmount.PARTIAL,
        ) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.ENABLED,
                copyState = ContextMenuItemState.ENABLED,
                pasteState = ContextMenuItemState.ENABLED,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }

    @Test
    fun btf2_contextMenu_nonEmptyClipboard_fullSelection_itemsMatch() =
        runBtf2CorrectItemsTest(isEmptyClipboard = false, selectionAmount = SelectionAmount.ALL) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.ENABLED,
                copyState = ContextMenuItemState.ENABLED,
                pasteState = ContextMenuItemState.ENABLED,
                selectAllState = ContextMenuItemState.DOES_NOT_EXIST,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    fun btf2_contextMenu_password_noSelection_itemsMatch_beforeApi26() =
        runBtf2CorrectItemsTest(isPassword = true, selectionAmount = SelectionAmount.NONE) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.ENABLED,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun btf2_contextMenu_password_noSelection_itemsMatch_afterApi26() =
        runBtf2CorrectItemsTest(isPassword = true, selectionAmount = SelectionAmount.NONE) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.ENABLED,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.ENABLED,
            )
        }

    @Test
    fun btf2_contextMenu_password_partialSelection_itemsMatch() =
        runBtf2CorrectItemsTest(isPassword = true, selectionAmount = SelectionAmount.PARTIAL) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.ENABLED,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }

    @Test
    fun btf2_contextMenu_password_fullSelection_itemsMatch() =
        runBtf2CorrectItemsTest(isPassword = true, selectionAmount = SelectionAmount.ALL) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.ENABLED,
                selectAllState = ContextMenuItemState.DOES_NOT_EXIST,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }

    @Test
    fun btf2_contextMenu_readOnly_noSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isReadOnly = true,
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.NONE,
        ) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }

    @Test
    fun btf2_contextMenu_readOnly_partialSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isReadOnly = true,
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.PARTIAL,
        ) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.ENABLED,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }

    @Test
    fun btf2_contextMenu_readOnly_fullSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isReadOnly = true,
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.ALL,
        ) {
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.ENABLED,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.DOES_NOT_EXIST,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }

    @Test
    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    fun btf2_contextMenu_processText_itemsMatch() = runBtf2CorrectItemsTest {
        rule.assertContextMenuItem(
            label = ContextMenuItemLabels.PROCESS_TEXT_1,
            state = ContextMenuItemState.ENABLED,
        )
        rule.assertContextMenuItem(
            label = ContextMenuItemLabels.PROCESS_TEXT_2,
            state = ContextMenuItemState.ENABLED,
        )
    }

    private enum class SelectionAmount {
        NONE,
        PARTIAL,
        ALL,
    }

    private fun runBtf2CorrectItemsTest(
        isPassword: Boolean = false, // todo use
        isReadOnly: Boolean = false,
        isEmptyClipboard: Boolean = false,
        selectionAmount: SelectionAmount = SelectionAmount.PARTIAL,
        assertBlock: () -> Unit,
    ) = runTest {
        check(!(isPassword && isReadOnly)) { "Can't be a password field and read-only." }

        val text = "Text Text Text"
        val state =
            TextFieldState(
                initialText = text,
                initialSelection =
                    when (selectionAmount) {
                        SelectionAmount.NONE -> TextRange.Zero
                        SelectionAmount.PARTIAL -> TextRange(5, 9)
                        SelectionAmount.ALL -> TextRange(0, 14)
                    },
            )

        val clipboard =
            FakeClipboard().apply {
                if (isEmptyClipboard) {
                    setClipEntry(null)
                } else {
                    setClipEntry(AnnotatedString("Clipboard Text").toClipEntry())
                }
            }

        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                if (isPassword) {
                    BasicSecureTextField(state = state, modifier = Modifier.testTag(textFieldTag))
                } else {
                    BasicTextField(
                        state = state,
                        readOnly = isReadOnly,
                        modifier = Modifier.testTag(textFieldTag),
                    )
                }
            }
        }

        rule.onNodeWithTag(textFieldTag).performMouseInput { rightClick(center) }
        assertBlock()
    }
    // endregion BTF2 Context Menu Correct Item Tests
}
