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

import androidx.compose.foundation.contextmenu.ProcessTextItemOverrideRule
import androidx.compose.foundation.internal.toClipEntry
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys.AutofillKey
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys.CopyKey
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys.CutKey
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys.PasteKey
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys.SelectAllKey
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagFlipperRunner
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagSuppress
import androidx.compose.foundation.text.contextmenu.test.FakeTextContextMenuProvider
import androidx.compose.foundation.text.contextmenu.test.assertItems
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.internal.selection.FakeClipboard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(ContextMenuFlagFlipperRunner::class)
@ContextMenuFlagSuppress(suppressedFlagValue = false)
class TextFieldTextContextMenuBuilderTest : FocusedWindowTest {
    @get:Rule val rule = createComposeRule()

    private val text = "Text Text Text"
    private val textFieldTag = "BTF"

    @get:Rule val processTextRule = ProcessTextItemOverrideRule()

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    fun btf1_contextMenu_emptyClipboard_noSelection_itemsMatch_beforeApi26() =
        runBtf1CorrectItemsTest(
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.NONE,
            expectedKeys = listOf(SelectAllKey),
        )

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun btf1_contextMenu_emptyClipboard_noSelection_itemsMatch_afterApi26() =
        runBtf1CorrectItemsTest(
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.NONE,
            expectedKeys = listOf(SelectAllKey, AutofillKey),
        )

    @Test
    fun btf1_contextMenu_emptyClipboard_partialSelection_itemsMatch() =
        runBtf1CorrectItemsTest(
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.PARTIAL,
            expectedKeys = listOf(CutKey, CopyKey, SelectAllKey),
        )

    @Test
    fun btf1_contextMenu_emptyClipboard_fullSelection_itemsMatch() =
        runBtf1CorrectItemsTest(
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.ALL,
            expectedKeys = listOf(CutKey, CopyKey),
        )

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    fun btf1_contextMenu_nonEmptyClipboard_noSelection_itemsMatch_beforeApi26() =
        runBtf1CorrectItemsTest(
            isEmptyClipboard = false,
            selectionAmount = SelectionAmount.NONE,
            expectedKeys = listOf(PasteKey, SelectAllKey),
        )

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun btf1_contextMenu_nonEmptyClipboard_noSelection_itemsMatch_afterApi26() =
        runBtf1CorrectItemsTest(
            isEmptyClipboard = false,
            selectionAmount = SelectionAmount.NONE,
            expectedKeys = listOf(PasteKey, SelectAllKey, AutofillKey),
        )

    @Test
    fun btf1_contextMenu_nonEmptyClipboard_partialSelection_itemsMatch() =
        runBtf1CorrectItemsTest(
            isPassword = false,
            isReadOnly = false,
            isEmptyClipboard = false,
            selectionAmount = SelectionAmount.PARTIAL,
            expectedKeys = listOf(CutKey, CopyKey, PasteKey, SelectAllKey),
        )

    @Test
    fun btf1_contextMenu_nonEmptyClipboard_fullSelection_itemsMatch() =
        runBtf1CorrectItemsTest(
            isEmptyClipboard = false,
            selectionAmount = SelectionAmount.ALL,
            expectedKeys = listOf(CutKey, CopyKey, PasteKey),
        )

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    fun btf1_contextMenu_password_noSelection_itemsMatch_beforeApi26() =
        runBtf1CorrectItemsTest(
            isPassword = true,
            selectionAmount = SelectionAmount.NONE,
            expectedKeys = listOf(PasteKey, SelectAllKey),
        )

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun btf1_contextMenu_password_noSelection_itemsMatch_afterApi26() =
        runBtf1CorrectItemsTest(
            isPassword = true,
            selectionAmount = SelectionAmount.NONE,
            expectedKeys = listOf(PasteKey, SelectAllKey, AutofillKey),
        )

    @Test
    fun btf1_contextMenu_password_partialSelection_itemsMatch() =
        runBtf1CorrectItemsTest(
            isPassword = true,
            selectionAmount = SelectionAmount.PARTIAL,
            expectedKeys = listOf(PasteKey, SelectAllKey),
        )

    @Test
    fun btf1_contextMenu_password_fullSelection_itemsMatch() =
        runBtf1CorrectItemsTest(
            isPassword = true,
            selectionAmount = SelectionAmount.ALL,
            expectedKeys = listOf(PasteKey),
        )

    @Test
    fun btf1_contextMenu_readOnly_noSelection_itemsMatch() =
        runBtf1CorrectItemsTest(
            isReadOnly = true,
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.NONE,
            expectedKeys = listOf(SelectAllKey),
        )

    @Test
    fun btf1_contextMenu_readOnly_partialSelection_itemsMatch() =
        runBtf1CorrectItemsTest(
            isReadOnly = true,
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.PARTIAL,
            expectedKeys = listOf(CopyKey, SelectAllKey),
        )

    @Test
    fun btf1_contextMenu_readOnly_fullSelection_itemsMatch() =
        runBtf1CorrectItemsTest(
            isReadOnly = true,
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.ALL,
            expectedKeys = listOf(CopyKey),
        )

    private fun runBtf1CorrectItemsTest(
        isPassword: Boolean = false,
        isReadOnly: Boolean = false,
        isEmptyClipboard: Boolean = false,
        selectionAmount: SelectionAmount = SelectionAmount.PARTIAL,
        expectedKeys: List<Any>,
    ) {
        check(!(isPassword && isReadOnly)) { "Can't be a password field and read-only." }

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

        runCorrectItemsTest(isEmptyClipboard, expectedKeys) {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                visualTransformation = visualTransformation,
                readOnly = isReadOnly,
                modifier = Modifier.testTag(textFieldTag),
            )
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    fun btf2_contextMenu_emptyClipboard_noSelection_itemsMatch_beforeApi26() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.NONE,
            expectedKeys = listOf(SelectAllKey),
        )

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun btf2_contextMenu_emptyClipboard_noSelection_itemsMatch_afterApi26() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.NONE,
            expectedKeys = listOf(SelectAllKey, AutofillKey),
        )

    @Test
    fun btf2_contextMenu_emptyClipboard_partialSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.PARTIAL,
            expectedKeys = listOf(CutKey, CopyKey, SelectAllKey),
        )

    @Test
    fun btf2_contextMenu_emptyClipboard_fullSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.ALL,
            expectedKeys = listOf(CutKey, CopyKey),
        )

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    fun btf2_contextMenu_nonEmptyClipboard_noSelection_itemsMatch_beforeApi26() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = false,
            selectionAmount = SelectionAmount.NONE,
            expectedKeys = listOf(PasteKey, SelectAllKey),
        )

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun btf2_contextMenu_nonEmptyClipboard_noSelection_itemsMatch_afterApi26() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = false,
            selectionAmount = SelectionAmount.NONE,
            expectedKeys = listOf(PasteKey, SelectAllKey, AutofillKey),
        )

    @Test
    fun btf2_contextMenu_nonEmptyClipboard_partialSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = false,
            selectionAmount = SelectionAmount.PARTIAL,
            expectedKeys = listOf(CutKey, CopyKey, PasteKey, SelectAllKey),
        )

    @Test
    fun btf2_contextMenu_nonEmptyClipboard_fullSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = false,
            selectionAmount = SelectionAmount.ALL,
            expectedKeys = listOf(CutKey, CopyKey, PasteKey),
        )

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    fun btf2_contextMenu_password_noSelection_itemsMatch_beforeApi26() =
        runBtf2CorrectItemsTest(
            isPassword = true,
            selectionAmount = SelectionAmount.NONE,
            expectedKeys = listOf(PasteKey, SelectAllKey),
        )

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun btf2_contextMenu_password_noSelection_itemsMatch_afterApi26() =
        runBtf2CorrectItemsTest(
            isPassword = true,
            selectionAmount = SelectionAmount.NONE,
            expectedKeys = listOf(PasteKey, SelectAllKey, AutofillKey),
        )

    @Test
    fun btf2_contextMenu_password_partialSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isPassword = true,
            selectionAmount = SelectionAmount.PARTIAL,
            expectedKeys = listOf(PasteKey, SelectAllKey),
        )

    @Test
    fun btf2_contextMenu_password_fullSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isPassword = true,
            selectionAmount = SelectionAmount.ALL,
            expectedKeys = listOf(PasteKey),
        )

    @Test
    fun btf2_contextMenu_readOnly_noSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isReadOnly = true,
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.NONE,
            expectedKeys = listOf(SelectAllKey),
        )

    @Test
    fun btf2_contextMenu_readOnly_partialSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isReadOnly = true,
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.PARTIAL,
            expectedKeys = listOf(CopyKey, SelectAllKey),
        )

    @Test
    fun btf2_contextMenu_readOnly_fullSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isReadOnly = true,
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.ALL,
            expectedKeys = listOf(CopyKey),
        )

    private enum class SelectionAmount {
        NONE,
        PARTIAL,
        ALL,
    }

    private fun runBtf2CorrectItemsTest(
        isPassword: Boolean = false,
        isReadOnly: Boolean = false,
        isEmptyClipboard: Boolean = false,
        selectionAmount: SelectionAmount = SelectionAmount.PARTIAL,
        expectedKeys: List<Any>,
    ) {
        check(!(isPassword && isReadOnly)) { "Can't be a password field and read-only." }
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

        runCorrectItemsTest(isEmptyClipboard, expectedKeys) {
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

    private fun runCorrectItemsTest(
        isEmptyClipboard: Boolean = false,
        expectedKeys: List<Any>,
        content: @Composable () -> Unit,
    ) {
        val clipboard = FakeClipboard()

        lateinit var data: TextContextMenuData
        val fakeProvider = FakeTextContextMenuProvider { data = it.data() }

        lateinit var coroutineScope: CoroutineScope
        rule.setTextFieldTestContent {
            coroutineScope = rememberCoroutineScope()
            CompositionLocalProvider(
                LocalClipboard provides clipboard,
                LocalTextContextMenuDropdownProvider provides fakeProvider,
                content = content,
            )
        }

        val clipEntry =
            AnnotatedString("Clipboard Text").takeUnless { isEmptyClipboard }?.toClipEntry()

        val clipEntryJob =
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                clipboard.setClipEntry(clipEntry)
            }

        rule.waitUntil("waiting for clip entry to be set") { clipEntryJob.isCompleted }

        rule.onNodeWithTag(textFieldTag).performMouseInput { rightClick(center) }

        data.assertItems(expectedKeys)
    }
}
