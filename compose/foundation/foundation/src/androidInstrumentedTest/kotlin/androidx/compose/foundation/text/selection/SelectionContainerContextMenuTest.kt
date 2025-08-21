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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.contextmenu.ContextMenuItemLabels
import androidx.compose.foundation.contextmenu.ContextMenuItemState
import androidx.compose.foundation.contextmenu.ProcessTextItemOverrideRule
import androidx.compose.foundation.contextmenu.assertContextMenuItem
import androidx.compose.foundation.contextmenu.assertContextMenuItems
import androidx.compose.foundation.contextmenu.clickOffPopup
import androidx.compose.foundation.contextmenu.contextMenuItemInteraction
import androidx.compose.foundation.internal.readText
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.PlatformSelectionBehaviorsRule
import androidx.compose.foundation.text.contextmenu.ProcessTextApi23Impl
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagFlipperRunner
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagSuppress
import androidx.compose.foundation.text.input.internal.selection.FakeClipboard
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.lerp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(ContextMenuFlagFlipperRunner::class)
open class SelectionContainerContextMenuTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule
    val processTextRule =
        ProcessTextItemOverrideRule(
            ContextMenuItemLabels.PROCESS_TEXT_1,
            ContextMenuItemLabels.PROCESS_TEXT_2,
        )

    @get:Rule val platformSelectionBehaviorsRule = PlatformSelectionBehaviorsRule()

    private val textTag = "text"
    private val defaultText = "Text Text Text"
    private val initialClipboardText = "clip"

    // region SelectionContainer Context Menu Gesture Tests
    @Test
    fun contextMenu_rightClick_appears() {
        rule.setContent {
            SelectionContainer { BasicText(defaultText, modifier = Modifier.testTag(textTag)) }
        }

        val contextMenuInteraction = rule.onNode(isPopup())
        contextMenuInteraction.assertDoesNotExist()
        rule.onNodeWithTag(textTag).performMouseInput { rightClick(center) }
        contextMenuInteraction.assertExists()
    }

    @Test
    fun contextMenu_leftClick_doesNotAppear() {
        rule.setContent {
            SelectionContainer { BasicText(defaultText, modifier = Modifier.testTag(textTag)) }
        }

        val contextMenuInteraction = rule.onNode(isPopup())
        contextMenuInteraction.assertDoesNotExist()
        rule.onNodeWithTag(textTag).performMouseInput { click(center) }
        contextMenuInteraction.assertDoesNotExist()
    }

    @Test
    fun contextMenu_disappearsOnClickOffOfPopup() {
        rule.setContent {
            SelectionContainer { BasicText(defaultText, modifier = Modifier.testTag(textTag)) }
        }

        val contextMenuInteraction = rule.onNode(isPopup())
        contextMenuInteraction.assertDoesNotExist()
        rule.onNodeWithTag(textTag).performMouseInput { rightClick(center) }
        contextMenuInteraction.assertExists()
        rule.clickOffPopup { rootRect -> lerp(rootRect.topLeft, rootRect.center, 0.5f) }
        contextMenuInteraction.assertDoesNotExist()
    }

    // endregion SelectionContainer Context Menu Gesture Tests

    // region Context Menu Item Click Tests
    @Test
    fun contextMenu_onClickCopy() = runTest {
        runClickContextMenuItemTest(
            labelToClick = ContextMenuItemLabels.COPY,
            expectedSelection = TextRange(5, 9),
            expectedClipboardContent = "Text",
        )
    }

    @Test
    fun contextMenu_onClickSelectAll() = runTest {
        runClickContextMenuItemTest(
            labelToClick = ContextMenuItemLabels.SELECT_ALL,
            expectedSelection = TextRange(0, 14),
        )
    }

    @Suppress("SameParameterValue")
    private suspend fun runClickContextMenuItemTest(
        labelToClick: String,
        expectedSelection: TextRange,
        expectedClipboardContent: String? = null,
    ) =
        runClickContextMenuItemTest(labelToClick) { selection, clipboard ->
            // Operation was applied
            assertThat(selection).isNotNull()
            assertThat(selection!!.toTextRange()).isEqualTo(expectedSelection)
            val clipboardContent = clipboard.getClipEntry()
            assertThat(clipboardContent).isNotNull()
            assertThat(clipboardContent!!.readText())
                .isEqualTo(expectedClipboardContent ?: initialClipboardText)
        }

    @Suppress("SameParameterValue")
    private suspend fun runClickContextMenuItemTest(
        labelToClick: String,
        text: String = defaultText,
        assertionBlock: suspend (Selection?, clipboard: Clipboard) -> Unit,
    ) {
        val clipboard = FakeClipboard(initialClipboardText)

        var selection by mutableStateOf<Selection?>(null)
        rule.setContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                SelectionContainer(selection = selection, onSelectionChange = { selection = it }) {
                    BasicText(text, modifier = Modifier.testTag(textTag))
                }
            }
        }

        // start selection of middle word
        rule.onNodeWithTag(textTag).performTouchInput { longClick(center) }

        // open context menu
        rule.onNodeWithTag(textTag).performMouseInput { rightClick(center) }

        val itemInteraction = rule.contextMenuItemInteraction(label = labelToClick)
        itemInteraction.assertHasClickAction()
        itemInteraction.assertIsEnabled()
        itemInteraction.performClick()
        rule.waitForIdle()

        // Context menu disappears
        rule.onNode(isPopup()).assertDoesNotExist()
        itemInteraction.assertDoesNotExist()

        // Assert
        assertionBlock(selection, clipboard)
    }

    // endregion Context Menu Item Click Tests

    // region Context Menu Correct Item Tests
    @Test
    fun contextMenu_noSelection_itemsMatch() =
        runCorrectItemsTest(selectionAmount = SelectionAmount.NONE) { selection ->
            assertThat(selection).isNull()
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }

    @Test
    fun contextMenu_partialSelection_itemsMatch() =
        runCorrectItemsTest(selectionAmount = SelectionAmount.PARTIAL) { selection ->
            assertThat(selection).isNotNull()
            assertThat(selection!!.toTextRange()).isEqualTo(TextRange(5, 9))
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.ENABLED,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.ENABLED,
                autofillState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }

    @Test
    fun contextMenu_fullSelection_itemsMatch() =
        runCorrectItemsTest(selectionAmount = SelectionAmount.ALL) { selection ->
            assertThat(selection).isNotNull()
            assertThat(selection!!.toTextRange()).isEqualTo(TextRange(0, 14))
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
    fun contextMenu_onClickProcessText() {
        val text = "abc def ghi"

        var textToProcess: String? = null
        ProcessTextApi23Impl.onClickProcessTextItem = { _, _, editable, text, selection ->
            // editable is always false for SelectionContainer.
            assertThat(editable).isFalse()
            textToProcess = text.subSequence(selection.start, selection.end).toString()
        }

        runTest {
            runClickContextMenuItemTest(
                labelToClick = ContextMenuItemLabels.PROCESS_TEXT_1,
                text = text,
            ) { _, _ ->
                assertThat(textToProcess).isEqualTo("def")
            }
        }
    }

    @Test
    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    fun contextMenu_processText_itemsMatch() = runCorrectItemsTest { selection ->
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

    private fun runCorrectItemsTest(
        selectionAmount: SelectionAmount = SelectionAmount.PARTIAL,
        assertBlock: (Selection?) -> Unit,
    ) {
        val text = "Text Text Text"

        val clipboard = FakeClipboard("Clipboard Text")

        var selection by mutableStateOf<Selection?>(null)

        rule.setContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                SelectionContainer(selection = selection, onSelectionChange = { selection = it }) {
                    BasicText(text, modifier = Modifier.testTag(textTag))
                }
            }
        }

        // set selection
        when (selectionAmount) {
            SelectionAmount.NONE -> {} // already no selection
            SelectionAmount.PARTIAL -> {
                // select middle word
                rule.onNodeWithTag(textTag).performTouchInput { longClick(center) }
                rule.waitForIdle()
            }
            SelectionAmount.ALL -> {
                // select everything
                rule.onNodeWithTag(textTag).performTouchInput {
                    val xShift = Offset(1f, 0f)
                    longPress(centerLeft + xShift)
                    moveTo(centerRight - xShift)
                    up()
                }
                rule.waitForIdle()
            }
        }

        // open context menu
        rule.onNodeWithTag(textTag).performMouseInput { rightClick(center) }

        assertBlock(selection)
    }
    // endregion Context Menu Correct Item Tests
}
