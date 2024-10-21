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

package androidx.compose.foundation.text.input.internal

import android.content.ClipDescription
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.HandwritingGesture
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.view.inputmethod.PreviewableHandwritingGesture
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.firstUriOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class StatelessInputConnectionTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var ic: StatelessInputConnection
    private val activeSession: TextInputSession =
        object : TextInputSession {
            override val text: TextFieldCharSequence
                get() = this@StatelessInputConnectionTest.value

            override fun onImeAction(imeAction: ImeAction) {
                this@StatelessInputConnectionTest.onImeAction?.invoke(imeAction)
            }

            override fun mapFromTransformed(range: TextRange): TextRange {
                mapFromTransformedCalled = range
                return state.mapFromTransformed(range)
            }

            override fun mapToTransformed(range: TextRange): TextRange {
                mapToTransformedCalled = range
                return state.mapToTransformed(range)
            }

            override fun beginBatchEdit(): Boolean {
                beginBatchEditCalls++
                batchDepth++
                return true
            }

            override fun edit(block: TextFieldBuffer.() -> Unit) {
                beginBatchEdit()
                edits.add(block)
                endBatchEdit()
            }

            override fun endBatchEdit(): Boolean {
                endBatchEditCalls++
                batchDepth--
                if (batchDepth == 0 && edits.isNotEmpty()) {
                    onRequestEdit?.invoke { edits.forEach { it.invoke(this) } }
                    edits.clear()
                }
                return batchDepth > 0
            }

            override fun sendKeyEvent(keyEvent: KeyEvent) {
                onSendKeyEvent?.invoke(keyEvent)
            }

            override fun requestCursorUpdates(cursorUpdateMode: Int) {}

            override fun onCommitContent(transferableContent: TransferableContent): Boolean {
                return this@StatelessInputConnectionTest.onCommitContent?.invoke(
                    transferableContent
                ) ?: false
            }

            override fun performHandwritingGesture(gesture: HandwritingGesture): Int {
                return InputConnection.HANDWRITING_GESTURE_RESULT_UNSUPPORTED
            }

            override fun previewHandwritingGesture(
                gesture: PreviewableHandwritingGesture,
                cancellationSignal: CancellationSignal?
            ): Boolean {
                return false
            }
        }

    private var edits = mutableVectorOf<TextFieldBuffer.() -> Unit>()
    private var state: TransformedTextFieldState = TransformedTextFieldState(TextFieldState())
    private var value: TextFieldCharSequence = TextFieldCharSequence()
        set(value) {
            field = value
            state = TransformedTextFieldState(TextFieldState(value.toString(), value.selection))
        }

    private var onRequestEdit: ((TextFieldBuffer.() -> Unit) -> Unit)? = null
    private var onSendKeyEvent: ((KeyEvent) -> Unit)? = null
    private var onImeAction: ((ImeAction) -> Unit)? = null
    private var onCommitContent: ((TransferableContent) -> Boolean)? = null

    private var beginBatchEditCalls = 0
    private var endBatchEditCalls = 0
    private var mapFromTransformedCalled: TextRange? = null
    private var mapToTransformedCalled: TextRange? = null

    private var batchDepth = 0

    @Before
    fun setup() {
        ic = StatelessInputConnection(activeSession, EditorInfo())
    }

    @Test
    fun getTextBeforeAndAfterCursorTest() {
        assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("")
        assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("")

        // Set "Hello, World", and place the cursor at the beginning of the text.
        value = TextFieldCharSequence(text = "Hello, World", selection = TextRange.Zero)

        assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("")
        assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("Hello, World")

        // Set "Hello, World", and place the cursor between "H" and "e".
        value = TextFieldCharSequence(text = "Hello, World", selection = TextRange(1))

        assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("H")
        assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("ello, World")

        // Set "Hello, World", and place the cursor at the end of the text.
        value = TextFieldCharSequence(text = "Hello, World", selection = TextRange(12))

        assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("Hello, World")
        assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("")
    }

    @Test
    fun getTextBeforeAndAfterCursorTest_maxCharTest() {
        // Set "Hello, World", and place the cursor at the beginning of the text.
        value = TextFieldCharSequence(text = "Hello, World", selection = TextRange.Zero)

        assertThat(ic.getTextBeforeCursor(5, 0)).isEqualTo("")
        assertThat(ic.getTextAfterCursor(5, 0)).isEqualTo("Hello")

        // Set "Hello, World", and place the cursor between "H" and "e".
        value = TextFieldCharSequence(text = "Hello, World", selection = TextRange(1))

        assertThat(ic.getTextBeforeCursor(5, 0)).isEqualTo("H")
        assertThat(ic.getTextAfterCursor(5, 0)).isEqualTo("ello,")

        // Set "Hello, World", and place the cursor at the end of the text.
        value = TextFieldCharSequence(text = "Hello, World", selection = TextRange(12))

        assertThat(ic.getTextBeforeCursor(5, 0)).isEqualTo("World")
        assertThat(ic.getTextAfterCursor(5, 0)).isEqualTo("")
    }

    @Test
    fun getSelectedTextTest() {
        // Set "Hello, World", and place the cursor at the beginning of the text.
        value = TextFieldCharSequence(text = "Hello, World", selection = TextRange.Zero)

        assertThat(ic.getSelectedText(0)).isNull()

        // Set "Hello, World", and place the cursor between "H" and "e".
        value = TextFieldCharSequence(text = "Hello, World", selection = TextRange(0, 1))

        assertThat(ic.getSelectedText(0)).isEqualTo("H")

        // Set "Hello, World", and place the cursor at the end of the text.
        value = TextFieldCharSequence(text = "Hello, World", selection = TextRange(0, 12))

        assertThat(ic.getSelectedText(0)).isEqualTo("Hello, World")
    }

    @Test
    fun commitTextTest_batchSession() {
        var requestEditsCalled = 0
        onRequestEdit = { block ->
            requestEditsCalled++
            state.editUntransformedTextAsUser { block() }
        }
        value = TextFieldCharSequence(text = "", selection = TextRange.Zero)

        // IME set text "Hello, World." with two commitText API within the single batch session.
        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.commitText("Hello, ", 1)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.commitText("World.", 1)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        ic.endBatchEdit()

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(state.untransformedText.toString()).isEqualTo("Hello, World.")
        assertThat(state.untransformedText.selection).isEqualTo(TextRange(13))
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @SdkSuppress(minSdkVersion = 25)
    @Test
    fun commitContent_parsesToTransferableContent() {
        var transferableContent: TransferableContent? = null
        onCommitContent = {
            transferableContent = it
            true
        }
        val contentUri = Uri.parse("content://com.example/content")
        val linkUri = Uri.parse("https://example.com")
        val description = ClipDescription("label", arrayOf("text/plain"))
        val extras = Bundle().apply { putString("key", "value") }
        val result =
            ic.commitContent(
                InputContentInfo(contentUri, description, linkUri),
                InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                extras
            )

        assertThat(transferableContent).isNotNull()
        assertThat(transferableContent?.clipEntry).isNotNull()
        assertThat(transferableContent?.clipEntry?.firstUriOrNull()).isEqualTo(contentUri)
        assertThat(transferableContent?.clipEntry?.clipData?.itemCount).isEqualTo(1)
        assertThat(transferableContent?.clipMetadata?.clipDescription).isSameInstanceAs(description)

        assertThat(transferableContent?.source).isEqualTo(TransferableContent.Source.Keyboard)
        assertThat(transferableContent?.platformTransferableContent?.linkUri).isEqualTo(linkUri)
        assertThat(transferableContent?.platformTransferableContent?.extras?.keySet())
            .contains("key")
        assertThat(transferableContent?.platformTransferableContent?.extras?.keySet())
            .contains("EXTRA_INPUT_CONTENT_INFO")

        assertTrue(result)
    }

    @SdkSuppress(minSdkVersion = 25)
    @Test
    fun commitContent_returnsResultIfFalse() {
        onCommitContent = { false }
        val contentUri = Uri.parse("content://com.example/content")
        val description = ClipDescription("label", arrayOf("text/plain"))
        val result = ic.commitContent(InputContentInfo(contentUri, description), 0, null)

        assertFalse(result)
    }

    @SdkSuppress(minSdkVersion = 25)
    @Test
    fun commitContent_returnsFalseWhenNotDefined() {
        onCommitContent = null
        val contentUri = Uri.parse("content://com.example/content")
        val description = ClipDescription("label", arrayOf("text/plain"))
        val result = ic.commitContent(InputContentInfo(contentUri, description), 0, null)

        assertFalse(result)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @SdkSuppress(maxSdkVersion = 24)
    @Test
    fun performPrivateCommand_parsesToTransferableContent() {
        var transferableContent: TransferableContent? = null
        onCommitContent = {
            transferableContent = it
            true
        }

        val editorInfo = EditorInfo()
        EditorInfoCompat.setContentMimeTypes(editorInfo, arrayOf("text/plain"))

        ic = StatelessInputConnection(activeSession, editorInfo)

        val contentUri = Uri.parse("content://com.example/content")
        val linkUri = Uri.parse("https://example.com")
        val description = ClipDescription("label", arrayOf("text/plain"))
        val extras = Bundle().apply { putString("key", "value") }
        // this will internally call performPrivateCommand when SDK <= 24
        val result =
            InputConnectionCompat.commitContent(
                ic,
                editorInfo,
                InputContentInfoCompat(contentUri, description, linkUri),
                InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                extras
            )

        assertThat(transferableContent).isNotNull()
        assertThat(transferableContent?.clipEntry).isNotNull()
        assertThat(transferableContent?.clipEntry?.firstUriOrNull()).isEqualTo(contentUri)
        assertThat(transferableContent?.clipEntry?.clipData?.itemCount).isEqualTo(1)
        assertThat(transferableContent?.clipMetadata?.clipDescription).isSameInstanceAs(description)

        assertThat(transferableContent?.source).isEqualTo(TransferableContent.Source.Keyboard)
        assertThat(transferableContent?.platformTransferableContent?.linkUri).isEqualTo(linkUri)
        assertThat(transferableContent?.platformTransferableContent?.extras?.keySet())
            .contains("key")
        // Permissions do not exist below SDK 25
        assertThat(transferableContent?.platformTransferableContent?.extras?.keySet())
            .doesNotContain("EXTRA_INPUT_CONTENT_INFO")

        assertTrue(result)
    }

    @Test
    fun setComposingText_appliesComposingSpans() {
        var requestEditsCalled = 0
        state = TransformedTextFieldState(TextFieldState("hello "))
        onRequestEdit = { block ->
            requestEditsCalled++
            state.editUntransformedTextAsUser { block() }
        }

        ic.setComposingText(
            SpannableStringBuilder().append("world").apply {
                setSpan(BackgroundColorSpan(Color.RED), 0, 3, 0)
                setSpan(BackgroundColorSpan(Color.BLUE), 3, 5, 0)
                setSpan(UnderlineSpan(), 0, 5, 0)
            },
            1
        )

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(state.untransformedText.composition).isEqualTo(TextRange(6, 11))
        assertThat(state.untransformedText.composingAnnotations).isNotNull()
        assertThat(state.untransformedText.composingAnnotations)
            .containsExactlyElementsIn(
                listOf(
                    AnnotatedString.Range(
                        SpanStyle(background = androidx.compose.ui.graphics.Color.Red),
                        6,
                        9
                    ),
                    AnnotatedString.Range(
                        SpanStyle(background = androidx.compose.ui.graphics.Color.Blue),
                        9,
                        11
                    ),
                    AnnotatedString.Range(
                        SpanStyle(textDecoration = TextDecoration.Underline),
                        6,
                        11
                    )
                )
            )
    }

    @Test
    fun verify_backgroundColorSpan() {
        val expected =
            listOf(
                AnnotatedString.Range(
                    SpanStyle(background = androidx.compose.ui.graphics.Color.Red),
                    0,
                    1
                )
            )
        val actual =
            buildSpannableString(
                    BackgroundColorSpan(androidx.compose.ui.graphics.Color.Red.toArgb())
                )
                .toAnnotationList()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun verify_foregroundColorSpan() {
        val expected =
            listOf(
                AnnotatedString.Range(
                    SpanStyle(color = androidx.compose.ui.graphics.Color.Red),
                    0,
                    1
                )
            )
        val actual =
            buildSpannableString(
                    ForegroundColorSpan(androidx.compose.ui.graphics.Color.Red.toArgb())
                )
                .toAnnotationList()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun verify_strikeThroughSpan() {
        val expected =
            listOf(
                AnnotatedString.Range(SpanStyle(textDecoration = TextDecoration.LineThrough), 0, 1)
            )
        val actual = buildSpannableString(StrikethroughSpan()).toAnnotationList()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun verify_styleSpan() {
        val expected =
            listOf(
                AnnotatedString.Range(
                    SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                    0,
                    1
                )
            )
        val actual = buildSpannableString(StyleSpan(Typeface.BOLD_ITALIC)).toAnnotationList()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun verify_typefaceSpan() {
        val expected =
            listOf(AnnotatedString.Range(SpanStyle(fontFamily = FontFamily.Monospace), 0, 1))
        val actual = buildSpannableString(TypefaceSpan("monospace")).toAnnotationList()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun verify_underlineSpan() {
        val expected =
            listOf(
                AnnotatedString.Range(SpanStyle(textDecoration = TextDecoration.Underline), 0, 1)
            )
        val actual = buildSpannableString(UnderlineSpan()).toAnnotationList()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun mixedAPICalls_batchSession() {
        var requestEditsCalled = 0
        onRequestEdit = { block ->
            requestEditsCalled++
            state.editUntransformedTextAsUser { block() }
        }
        value = TextFieldCharSequence(text = "", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.setComposingText("Hello, ", 1)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.finishComposingText()).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.commitText("World.", 1)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.setSelection(0, 12)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        assertThat(ic.commitText("", 1)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        ic.endBatchEdit()

        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(state.untransformedText.toString()).isEqualTo(".")
        assertThat(state.untransformedText.selection).isEqualTo(TextRange(0))
    }

    @Test
    fun setSelection_respectsOutputTransformation() {
        state =
            TransformedTextFieldState(
                textFieldState = TextFieldState("abc def"),
                outputTransformation = { insert(4, "ghi ") }
            )
        var requestEditsCalled = 0
        onRequestEdit = { block ->
            requestEditsCalled++
            state.editUntransformedTextAsUser { block() }
        }

        ic.beginBatchEdit()

        assertThat(ic.setSelection(5, 5)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        ic.endBatchEdit()
        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(mapFromTransformedCalled).isEqualTo(TextRange(5))
        assertThat(state.untransformedText.selection).isEqualTo(TextRange(4))
        assertThat(state.visualText.selection).isEqualTo(TextRange(4))
        assertThat(state.selectionWedgeAffinity)
            .isEqualTo(SelectionWedgeAffinity(WedgeAffinity.Start))
    }

    @Test
    fun setSelection_coercesRange() {
        state = TransformedTextFieldState(textFieldState = TextFieldState("abc def"))
        var requestEditsCalled = 0
        onRequestEdit = { block ->
            requestEditsCalled++
            state.editUntransformedTextAsUser { block() }
        }

        ic.beginBatchEdit()

        assertThat(ic.setSelection(Int.MIN_VALUE, Int.MAX_VALUE)).isTrue()
        assertThat(requestEditsCalled).isEqualTo(0)

        ic.endBatchEdit()
        assertThat(requestEditsCalled).isEqualTo(1)
        assertThat(state.untransformedText.selection).isEqualTo(TextRange(0, 7))
    }

    @Test
    fun closeConnection() {
        // Everything is internal and there is nothing to expect.
        // Just make sure it is not crashed by calling method.
        ic.closeConnection()
    }

    @Test
    fun do_not_callback_if_only_readonly_ops() {
        var requestEditsCalled = 0
        onRequestEdit = { _ -> requestEditsCalled++ }
        ic.beginBatchEdit()
        ic.getSelectedText(1)
        ic.endBatchEdit()
        assertThat(requestEditsCalled).isEqualTo(0)
    }

    @Test
    fun sendKeyEvent_whenIMERequests() {
        val keyEvents = mutableListOf<KeyEvent>()
        onSendKeyEvent = { keyEvents += it }
        val keyEvent1 = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0)
        val keyEvent2 = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0)
        ic.sendKeyEvent(keyEvent1)
        ic.sendKeyEvent(keyEvent2)

        assertThat(keyEvents.size).isEqualTo(2)
        assertThat(keyEvents.first()).isEqualTo(keyEvent1)
        assertThat(keyEvents.last()).isEqualTo(keyEvent2)
    }

    @Test
    fun performImeAction_whenIMERequests() {
        val receivedImeActions = mutableListOf<ImeAction>()
        onImeAction = { receivedImeActions += it }
        ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
        ic.performEditorAction(EditorInfo.IME_ACTION_GO)
        ic.performEditorAction(EditorInfo.IME_ACTION_NEXT)
        ic.performEditorAction(EditorInfo.IME_ACTION_NONE)
        ic.performEditorAction(EditorInfo.IME_ACTION_PREVIOUS)
        ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
        ic.performEditorAction(EditorInfo.IME_ACTION_SEND)
        ic.performEditorAction(EditorInfo.IME_ACTION_UNSPECIFIED)
        ic.performEditorAction(-1)

        assertThat(receivedImeActions)
            .isEqualTo(
                listOf(
                    ImeAction.Done,
                    ImeAction.Go,
                    ImeAction.Next,
                    ImeAction.Default, // None is evaluated back to Default.
                    ImeAction.Previous,
                    ImeAction.Search,
                    ImeAction.Send,
                    ImeAction.Default, // Unspecified is evaluated back to Default.
                    ImeAction.Default // Unrecognized is evaluated back to Default.
                )
            )
    }

    @Test
    fun selectAll_contextMenuAction_triggersSelection() {
        value = TextFieldCharSequence("Hello")
        var callCount = 0
        onRequestEdit = { block ->
            callCount++
            state.editUntransformedTextAsUser { block() }
        }

        ic.performContextMenuAction(android.R.id.selectAll)

        assertThat(callCount).isEqualTo(1)
        assertThat(state.untransformedText.selection).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun cut_contextMenuAction_triggersSyntheticKeyEvents() {
        val keyEvents = mutableListOf<KeyEvent>()
        onSendKeyEvent = { keyEvents += it }

        ic.performContextMenuAction(android.R.id.cut)

        assertThat(keyEvents.size).isEqualTo(2)
        assertThat(keyEvents[0].action).isEqualTo(KeyEvent.ACTION_DOWN)
        assertThat(keyEvents[0].keyCode).isEqualTo(KeyEvent.KEYCODE_CUT)
        assertThat(keyEvents[1].action).isEqualTo(KeyEvent.ACTION_UP)
        assertThat(keyEvents[1].keyCode).isEqualTo(KeyEvent.KEYCODE_CUT)
    }

    @Test
    fun copy_contextMenuAction_triggersSyntheticKeyEvents() {
        val keyEvents = mutableListOf<KeyEvent>()
        onSendKeyEvent = { keyEvents += it }

        ic.performContextMenuAction(android.R.id.copy)

        assertThat(keyEvents.size).isEqualTo(2)
        assertThat(keyEvents[0].action).isEqualTo(KeyEvent.ACTION_DOWN)
        assertThat(keyEvents[0].keyCode).isEqualTo(KeyEvent.KEYCODE_COPY)
        assertThat(keyEvents[1].action).isEqualTo(KeyEvent.ACTION_UP)
        assertThat(keyEvents[1].keyCode).isEqualTo(KeyEvent.KEYCODE_COPY)
    }

    @Test
    fun paste_contextMenuAction_triggersSyntheticKeyEvents() {
        val keyEvents = mutableListOf<KeyEvent>()
        onSendKeyEvent = { keyEvents += it }

        ic.performContextMenuAction(android.R.id.paste)

        assertThat(keyEvents.size).isEqualTo(2)
        assertThat(keyEvents[0].action).isEqualTo(KeyEvent.ACTION_DOWN)
        assertThat(keyEvents[0].keyCode).isEqualTo(KeyEvent.KEYCODE_PASTE)
        assertThat(keyEvents[1].action).isEqualTo(KeyEvent.ACTION_UP)
        assertThat(keyEvents[1].keyCode).isEqualTo(KeyEvent.KEYCODE_PASTE)
    }

    @Test
    fun debugMode_isDisabled() {
        // run this in presubmit to check that we are not accidentally enabling logs on prod
        assertFalse(
            SIC_DEBUG,
            "Oops, looks like you accidentally enabled logging. Don't worry, we've all " +
                "been there. Just remember to turn it off before you deploy your code."
        )
    }

    private fun buildSpannableString(span: Any) =
        SpannableStringBuilder().also { it.append("a", span, Spanned.SPAN_INCLUSIVE_INCLUSIVE) }
}
