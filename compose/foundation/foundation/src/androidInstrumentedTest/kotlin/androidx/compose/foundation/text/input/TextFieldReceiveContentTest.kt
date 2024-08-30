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

package androidx.compose.foundation.text.input

import android.content.ClipDescription
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.assertClipData
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.createClipData
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.internal.selection.FakeClipboardManager
import androidx.compose.foundation.text.selection.FakeTextToolbar
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.requestFocus
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests InputConnection#commitContent calls from BasicTextField to receiveContent modifier.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class TextFieldReceiveContentTest {

    @get:Rule
    val rule = createComposeRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val tag = "BasicTextField"

    @SdkSuppress(minSdkVersion = 25)
    @Test
    fun commitContentReturnsFalse_whenNoReceiveContentConfigured() {
        inputMethodInterceptor.setContent {
            BasicTextField(state = rememberTextFieldState(), modifier = Modifier.testTag(tag))
        }
        rule.onNodeWithTag(tag).requestFocus()
        inputMethodInterceptor.withInputConnection {
            assertFalse(
                commitContent(
                    createInputContentInfo().unwrap() as InputContentInfo,
                    0,
                    null
                )
            )
        }
    }

    @SdkSuppress(maxSdkVersion = 24)
    @Test
    fun preformPrivateCommandReturnsFalse_whenNoReceiveContentConfigured() {
        inputMethodInterceptor.setContent {
            BasicTextField(state = rememberTextFieldState(), modifier = Modifier.testTag(tag))
        }
        rule.onNodeWithTag(tag).requestFocus()
        inputMethodInterceptor.onIdle { editorInfo, inputConnection ->
            // Although we are testing `performPrivateCommand` that should return true by default
            // in the existence of no configuration, semantically the caller is still calling
            // commitContent which should return false by default.
            assertFalse(
                InputConnectionCompat.commitContent(
                    inputConnection,
                    editorInfo,
                    InputContentInfoCompat(DEFAULT_CONTENT_URI, DEFAULT_CLIP_DESCRIPTION, null),
                    0,
                    null
                )
            )
        }
    }

    @Test
    fun singleReceiveContent_configuresEditorInfo() {
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = rememberTextFieldState(),
                modifier = Modifier
                    .testTag(tag)
                    .contentReceiver { null }
            )
        }
        rule.onNodeWithTag(tag).requestFocus()
        inputMethodInterceptor.withEditorInfo {
            val contentMimeTypes = EditorInfoCompat.getContentMimeTypes(this)
            assertThat(contentMimeTypes).asList().containsAtLeastElementsIn(arrayOf("*/*"))
        }
    }

    @Test
    fun singleReceiveContent_isCalledAfterCommitContent() {
        var transferableContent: TransferableContent? = null
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = rememberTextFieldState(),
                modifier = Modifier
                    .testTag(tag)
                    .contentReceiver {
                        transferableContent = it
                        null
                    }
            )
        }
        rule.onNodeWithTag(tag).requestFocus()

        val linkUri = Uri.parse("https://example.com")
        val bundle = Bundle().apply { putString("key", "value") }
        inputMethodInterceptor.onIdle { editorInfo, inputConnection ->
            InputConnectionCompat.commitContent(
                inputConnection,
                editorInfo,
                createInputContentInfo(linkUri = linkUri),
                0,
                bundle
            )
        }

        rule.runOnIdle {
            assertThat(transferableContent).isNotNull()
            assertThat(transferableContent?.source).isEqualTo(TransferableContent.Source.Keyboard)
            assertThat(transferableContent?.clipMetadata?.clipDescription)
                .isEqualTo(DEFAULT_CLIP_DESCRIPTION)

            assertThat(transferableContent?.clipEntry?.clipData?.itemCount).isEqualTo(1)
            assertThat(transferableContent?.clipEntry?.clipData?.getItemAt(0)?.uri)
                .isEqualTo(DEFAULT_CONTENT_URI)

            assertThat(transferableContent?.platformTransferableContent?.linkUri)
                .isEqualTo(linkUri)
            assertThat(transferableContent?.platformTransferableContent?.extras)
                .isEqualTo(bundle)
        }
    }

    @SdkSuppress(minSdkVersion = 25) // Permissions are acquired only on SDK levels 25 or higher.
    @Test
    fun singleReceiveContent_permissionIsRequested() {
        var transferableContent: TransferableContent? = null
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = rememberTextFieldState(),
                modifier = Modifier
                    .testTag(tag)
                    .contentReceiver {
                        transferableContent = it
                        null
                    }
            )
        }
        rule.onNodeWithTag(tag).requestFocus()

        val inputContentInfo: InputContentInfoCompat = createInputContentInfo()

        inputMethodInterceptor.onIdle { editorInfo, inputConnection ->
            InputConnectionCompat.commitContent(
                inputConnection,
                editorInfo,
                inputContentInfo,
                InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                null
            )
        }

        rule.runOnIdle {
            assertThat(transferableContent).isNotNull()
            assertTrue(
                transferableContent?.platformTransferableContent
                    ?.extras
                    ?.containsKey("EXTRA_INPUT_CONTENT_INFO") ?: false
            )
        }
    }

    @Test
    fun multiReceiveContent_delegatesRemainingItems_toParent() {
        var childTransferableContent: TransferableContent? = null
        var parentTransferableContent: TransferableContent? = null
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = rememberTextFieldState(),
                modifier = Modifier
                    .testTag(tag)
                    .contentReceiver {
                        parentTransferableContent = it
                        null
                    }
                    .contentReceiver {
                        childTransferableContent = it
                        it
                    }
            )
        }
        rule.onNodeWithTag(tag).requestFocus()
        inputMethodInterceptor.onIdle { editorInfo, inputConnection ->
            InputConnectionCompat.commitContent(
                inputConnection,
                editorInfo,
                createInputContentInfo(),
                0,
                null
            )
        }

        rule.runOnIdle {
            assertThat(childTransferableContent).isNotNull()
            assertThat(childTransferableContent).isSameInstanceAs(parentTransferableContent)

            assertThat(parentTransferableContent?.source)
                .isEqualTo(TransferableContent.Source.Keyboard)
            assertThat(parentTransferableContent?.clipMetadata?.clipDescription)
                .isEqualTo(DEFAULT_CLIP_DESCRIPTION)

            assertThat(parentTransferableContent?.clipEntry?.clipData?.itemCount).isEqualTo(1)
            assertThat(parentTransferableContent?.clipEntry?.clipData?.getItemAt(0)?.uri)
                .isEqualTo(DEFAULT_CONTENT_URI)
        }
    }

    @Test
    fun multiReceiveContent_doesNotCallParent_ifAllItemsAreProcessed() {
        var childTransferableContent: TransferableContent? = null
        var parentTransferableContent: TransferableContent? = null
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = rememberTextFieldState(),
                modifier = Modifier
                    .testTag(tag)
                    .contentReceiver {
                        parentTransferableContent = it
                        null
                    }
                    .contentReceiver {
                        childTransferableContent = it
                        null
                    }
            )
        }
        rule.onNodeWithTag(tag).requestFocus()
        inputMethodInterceptor.onIdle { editorInfo, inputConnection ->
            InputConnectionCompat.commitContent(
                inputConnection,
                editorInfo,
                createInputContentInfo(),
                0,
                null
            )
        }

        rule.runOnIdle {
            assertThat(childTransferableContent).isNotNull()
            assertThat(parentTransferableContent).isNull()
        }
    }

    @Test
    fun semanticsPasteContent_delegatesToReceiveContent() {
        val clipboardManager = FakeClipboardManager(supportsClipEntry = true)
        val clipEntry = createClipData().toClipEntry()
        clipboardManager.setClip(clipEntry)
        lateinit var transferableContent: TransferableContent
        rule.setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                BasicTextField(
                    state = rememberTextFieldState(),
                    modifier = Modifier
                        .testTag(tag)
                        .contentReceiver {
                            transferableContent = it
                            null
                        }
                )
            }
        }

        rule.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.PasteText)

        rule.runOnIdle {
            assertClipData(transferableContent.clipEntry.clipData)
                .isEqualToClipData(clipEntry.clipData)
        }
    }

    @Test
    fun semanticsPasteContent_pastesLeftOverText() {
        val clipboardManager = FakeClipboardManager(supportsClipEntry = true)
        val clipEntry = createClipData {
            addText("some text")
            addUri()
            addIntent()
            addText("more text")
        }.toClipEntry()
        clipboardManager.setClip(clipEntry)
        val state = TextFieldState()
        rule.setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                BasicTextField(
                    state = state,
                    modifier = Modifier
                        .testTag(tag)
                        .contentReceiver {
                            it.consume { item ->
                                // only consume if there's no text
                                item.text == null
                            }
                        }
                )
            }
        }

        rule.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.PasteText)

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("some text\nmore text")
        }
    }

    @Test
    fun semanticsPasteContent_goesFromChildToParent() {
        val clipboardManager = FakeClipboardManager(supportsClipEntry = true)
        val clipEntry = createClipData {
            addText("a")
            addText("b")
            addText("c")
            addText("d")
        }.toClipEntry()
        clipboardManager.setClip(clipEntry)

        lateinit var transferableContent1: TransferableContent
        lateinit var transferableContent2: TransferableContent
        lateinit var transferableContent3: TransferableContent
        val state = TextFieldState()

        rule.setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                BasicTextField(
                    state = state,
                    modifier = Modifier
                        .testTag(tag)
                        .contentReceiver { content ->
                            transferableContent1 = content
                            content.consume {
                                it.text.contains("a")
                            }
                        }
                        .contentReceiver { content ->
                            transferableContent2 = content
                            content.consume {
                                it.text.contains("b")
                            }
                        }
                        .contentReceiver { content ->
                            transferableContent3 = content
                            content.consume {
                                it.text.contains("c")
                            }
                        }
                )
            }
        }

        rule.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.PasteText)

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("d")
            assertThat(transferableContent3.clipEntry.clipData.itemCount).isEqualTo(4)
            assertThat(transferableContent2.clipEntry.clipData.itemCount).isEqualTo(3)
            assertThat(transferableContent1.clipEntry.clipData.itemCount).isEqualTo(2)
        }
    }

    @Test
    fun toolbarPasteContent_delegatesToReceiveContent() {
        val clipboardManager = FakeClipboardManager(supportsClipEntry = true)
        val clipEntry = createClipData().toClipEntry()
        clipboardManager.setClip(clipEntry)
        var pasteOption: (() -> Unit)? = null
        val textToolbar = FakeTextToolbar(
            onShowMenu = { _, _, onPasteRequested, _, _ ->
                pasteOption = onPasteRequested
            },
            onHideMenu = {}
        )
        lateinit var transferableContent: TransferableContent
        rule.setContent {
            CompositionLocalProvider(
                LocalClipboardManager provides clipboardManager,
                LocalTextToolbar provides textToolbar
            ) {
                BasicTextField(
                    state = rememberTextFieldState(),
                    modifier = Modifier
                        .testTag(tag)
                        .contentReceiver {
                            transferableContent = it
                            null
                        }
                )
            }
        }

        rule.runOnIdle {
            pasteOption?.invoke()
        }

        rule.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.PasteText)

        rule.runOnIdle {
            assertClipData(transferableContent.clipEntry.clipData)
                .isEqualToClipData(clipEntry.clipData)
        }
    }

    companion object {
        private val DEFAULT_CONTENT_URI = Uri.parse("content://com.example.app/content")
        private val DEFAULT_CLIP_DESCRIPTION = ClipDescription("image", arrayOf("image/jpeg"))

        private fun createInputContentInfo(
            contentUri: Uri = DEFAULT_CONTENT_URI,
            clipDescription: ClipDescription = DEFAULT_CLIP_DESCRIPTION,
            linkUri: Uri? = null
        ) = InputContentInfoCompat(contentUri, clipDescription, linkUri)

        private fun InputMethodInterceptor.onIdle(block: (EditorInfo, InputConnection) -> Unit) {
            withInputConnection {
                withEditorInfo {
                    block(this@withEditorInfo, this@withInputConnection)
                }
            }
        }
    }
}
