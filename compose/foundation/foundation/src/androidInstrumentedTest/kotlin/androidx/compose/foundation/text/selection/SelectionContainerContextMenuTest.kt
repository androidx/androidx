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
import androidx.compose.foundation.contextmenu.assertContextMenuItems
import androidx.compose.foundation.contextmenu.clickOffPopup
import androidx.compose.foundation.contextmenu.contextMenuItemInteraction
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.internal.selection.FakeClipboardManager
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalClipboardManager
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SelectionContainerContextMenuTest {
    @get:Rule val rule = createComposeRule()

    private val textTag = "text"
    private val defaultText = "Text Text Text"

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
    fun contextMenu_onClickCopy() =
        runClickContextMenuItemTest(
            labelToClick = ContextMenuItemLabels.COPY,
            expectedSelection = TextRange(5, 9),
            expectedClipboardContent = "Text",
        )

    @Test
    fun contextMenu_onClickSelectAll() =
        runClickContextMenuItemTest(
            labelToClick = ContextMenuItemLabels.SELECT_ALL,
            expectedSelection = TextRange(0, 14),
        )

    @Suppress("SameParameterValue")
    private fun runClickContextMenuItemTest(
        labelToClick: String,
        expectedSelection: TextRange,
        expectedClipboardContent: String? = null,
    ) {
        val initialClipboardText = "clip"

        val clipboardManager =
            FakeClipboardManager(
                initialText = initialClipboardText,
                supportsClipEntry = true,
            )

        var selection by mutableStateOf<Selection?>(null)
        rule.setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                SelectionContainer(
                    selection = selection,
                    onSelectionChange = { selection = it },
                ) {
                    BasicText(defaultText, modifier = Modifier.testTag(textTag))
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

        // Operation was applied
        assertThat(selection).isNotNull()
        assertThat(selection!!.toTextRange()).isEqualTo(expectedSelection)
        val clipboardContent = clipboardManager.getText()
        assertThat(clipboardContent).isNotNull()
        assertThat(clipboardContent!!.text)
            .isEqualTo(expectedClipboardContent ?: initialClipboardText)
    }

    // endregion Context Menu Item Click Tests

    // region Context Menu Correct Item Tests
    @Test
    fun contextMenu_noSelection_itemsMatch() =
        runCorrectItemsTest(
            selectionAmount = SelectionAmount.NONE,
        ) { selection ->
            assertThat(selection).isNull()
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.DOES_NOT_EXIST,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.ENABLED,
            )
        }

    @Test
    fun contextMenu_partialSelection_itemsMatch() =
        runCorrectItemsTest(
            selectionAmount = SelectionAmount.PARTIAL,
        ) { selection ->
            assertThat(selection).isNotNull()
            assertThat(selection!!.toTextRange()).isEqualTo(TextRange(5, 9))
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.ENABLED,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.ENABLED,
            )
        }

    @Test
    fun contextMenu_fullSelection_itemsMatch() =
        runCorrectItemsTest(
            selectionAmount = SelectionAmount.ALL,
        ) { selection ->
            assertThat(selection).isNotNull()
            assertThat(selection!!.toTextRange()).isEqualTo(TextRange(0, 14))
            rule.assertContextMenuItems(
                cutState = ContextMenuItemState.DOES_NOT_EXIST,
                copyState = ContextMenuItemState.ENABLED,
                pasteState = ContextMenuItemState.DOES_NOT_EXIST,
                selectAllState = ContextMenuItemState.DOES_NOT_EXIST,
            )
        }

    private enum class SelectionAmount {
        NONE,
        PARTIAL,
        ALL
    }

    private fun runCorrectItemsTest(
        selectionAmount: SelectionAmount = SelectionAmount.PARTIAL,
        assertBlock: (Selection?) -> Unit,
    ) {
        val text = "Text Text Text"

        val clipboardManager =
            FakeClipboardManager(
                initialText = "Clipboard Text",
                supportsClipEntry = true,
            )

        var selection by mutableStateOf<Selection?>(null)

        rule.setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                SelectionContainer(
                    selection = selection,
                    onSelectionChange = { selection = it },
                ) {
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
