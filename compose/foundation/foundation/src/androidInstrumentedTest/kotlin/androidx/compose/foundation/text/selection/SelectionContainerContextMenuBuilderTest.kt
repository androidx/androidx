/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.foundation.contextmenu.ProcessTextItemOverrideRule
import androidx.compose.foundation.internal.readText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextContextMenuItems
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys.CopyKey
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys.SelectAllKey
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagFlipperRunner
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagSuppress
import androidx.compose.foundation.text.contextmenu.test.TestTextContextMenuDataInvoker
import androidx.compose.foundation.text.contextmenu.test.assertItems
import androidx.compose.foundation.text.contextmenu.test.testTextContextMenuDataReader
import androidx.compose.foundation.text.input.internal.selection.FakeClipboard
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(ContextMenuFlagFlipperRunner::class)
@ContextMenuFlagSuppress(suppressedFlagValue = false)
class SelectionContainerContextMenuBuilderTest {
    @get:Rule val rule = createComposeRule()

    private val textTag = "text"
    private val defaultText = "Text Text Text"
    private val initialClipboardText = "initialClipboardText"

    @get:Rule val processTextRule = ProcessTextItemOverrideRule()

    @Test
    fun whenTouch_onClick_copy() = runTest {
        runClickContextMenuItemTest(
            isMouse = false,
            expectedSessionClosed = true,
            itemToInvoke = TextContextMenuItems.Copy,
            expectedSelection = TextRange(5, 9),
            expectedClipboardContent = "Text",
        )
    }

    @Test
    fun whenTouch_onClick_selectAll() = runTest {
        runClickContextMenuItemTest(
            isMouse = false,
            expectedSessionClosed = false,
            itemToInvoke = TextContextMenuItems.SelectAll,
            expectedSelection = TextRange(0, 14),
            expectedClipboardContent = initialClipboardText,
        )
    }

    @Test
    fun whenMouse_onClick_copy() = runTest {
        runClickContextMenuItemTest(
            isMouse = true,
            expectedSessionClosed = true,
            itemToInvoke = TextContextMenuItems.Copy,
            expectedSelection = TextRange(5, 9),
            expectedClipboardContent = "Text",
        )
    }

    @Test
    fun whenMouse_onClick_selectAll() = runTest {
        runClickContextMenuItemTest(
            isMouse = true,
            expectedSessionClosed = true,
            itemToInvoke = TextContextMenuItems.SelectAll,
            expectedSelection = TextRange(0, 14),
            expectedClipboardContent = initialClipboardText,
        )
    }

    private suspend fun runClickContextMenuItemTest(
        isMouse: Boolean,
        expectedSessionClosed: Boolean,
        itemToInvoke: TextContextMenuItems,
        expectedSelection: TextRange,
        expectedClipboardContent: String,
    ) {
        var sessionClosed = false
        val fakeSession =
            object : TextContextMenuSession {
                override fun close() {
                    sessionClosed = true
                }
            }

        val fakeClipboard = FakeClipboard(initialClipboardText)

        val reader = TestTextContextMenuDataInvoker()
        var selection by mutableStateOf<Selection?>(null)
        rule.setContent {
            CompositionLocalProvider(LocalClipboard provides fakeClipboard) {
                SelectionContainer(selection = selection, onSelectionChange = { selection = it }) {
                    BasicText(
                        defaultText,
                        modifier = Modifier.testTag(textTag).testTextContextMenuDataReader(reader),
                    )
                }
            }
        }

        // start selection of middle word
        if (isMouse) {
            rule.onNodeWithTag(textTag).performMouseInput { repeat(2) { click() } }
        } else {
            rule.onNodeWithTag(textTag).performTouchInput { longClick() }
        }

        // collect our context menu items.
        val data = reader.invokeTraversal()

        // get and verify component
        val component = data.components.singleOrNull { it.key == itemToInvoke.key }
        assertWithMessage("Component with key %s not found", itemToInvoke.key)
            .that(component)
            .isNotNull()
        assertThat(component).isInstanceOf(TextContextMenuItem::class.java)
        val item = component as TextContextMenuItem

        // simulate clicking the item.
        item.onClick(fakeSession)

        // verify whether close called
        if (expectedSessionClosed) {
            assertThat(sessionClosed).isTrue()
        } else {
            assertThat(sessionClosed).isFalse()
        }

        // verify selection updated
        assertThat(selection).isNotNull()
        assertThat(selection!!.toTextRange()).isEqualTo(expectedSelection)

        // verify clipboard contents
        val clipboardContent = fakeClipboard.getClipEntry()
        assertThat(clipboardContent).isNotNull()
        assertThat(clipboardContent!!.readText()).isEqualTo(expectedClipboardContent)
    }

    @Test fun whenNoSelection_itemsMatch() = runItemMatchTest(expectedItems = listOf(SelectAllKey))

    @Test
    fun whenPartialSelection_itemsMatch() =
        runItemMatchTest(
            actions = { rule.onNodeWithTag(textTag).performTouchInput { longClick(center) } },
            expectedItems = listOf(CopyKey, SelectAllKey),
        )

    @Test
    fun whenFullSelection_itemsMatch() =
        runItemMatchTest(
            actions = {
                rule.onNodeWithTag(textTag).performTouchInput {
                    val xShift = Offset(1f, 0f)
                    longPress(centerLeft + xShift)
                    moveTo(centerRight - xShift)
                    up()
                }
            },
            expectedItems = listOf(CopyKey),
        )

    private fun runItemMatchTest(actions: (() -> Unit)? = null, expectedItems: List<Any>) {
        val reader = TestTextContextMenuDataInvoker()
        rule.setContent {
            SelectionContainer {
                Box(Modifier.testTextContextMenuDataReader(reader)) {
                    BasicText(text = defaultText, modifier = Modifier.testTag(textTag))
                }
            }
        }

        if (actions != null) {
            actions()
            rule.waitForIdle()
        }

        val actualData = reader.invokeTraversal()
        actualData.assertItems(expectedItems)
    }
}
