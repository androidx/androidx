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

import android.content.ClipData
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Parcelable
import android.text.Spanned
import android.text.TextUtils
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.HandwritingGesture
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.inputmethod.InputContentInfo
import android.view.inputmethod.PreviewableHandwritingGesture
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.PlatformTransferableContent
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.text.input.PlacedAnnotation
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.getSelectedText
import androidx.compose.foundation.text.input.getTextAfterSelection
import androidx.compose.foundation.text.input.getTextBeforeSelection
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.platform.toClipMetadata
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
import androidx.core.view.inputmethod.InputConnectionCompat.OnCommitContentListener
import androidx.core.view.inputmethod.InputContentInfoCompat
import java.util.concurrent.Executor
import java.util.function.IntConsumer

@VisibleForTesting internal const val SIC_DEBUG = false
private const val STATELESS_TAG = "StatelessIC"
private const val DEBUG_CLASS = "StatelessInputConnection"

private const val EXTRA_INPUT_CONTENT_INFO = "EXTRA_INPUT_CONTENT_INFO"

/**
 * An input connection that delegates its reads and writes to the active text input session.
 * InputConnections are requested and used by framework to create bridge from IME to an active
 * editor.
 *
 * @param editorInfo Required to create an InputConnection wrapper to support [commitContent] on all
 *   API levels.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class StatelessInputConnection(
    private val session: TextInputSession,
    editorInfo: EditorInfo
) : InputConnection {
    /**
     * The depth of the batch session. 0 means no session.
     *
     * Sometimes InputConnection does not call begin/endBatchEdit functions before calling other
     * edit functions like commitText or setComposingText. StatelessInputConnection starts and
     * finishes a new artificial batch for every EditCommand to make sure that there is always an
     * ongoing batch. EditCommands are only applied when batchDepth reaches 0.
     */
    private var batchDepth: Int = 0

    /**
     * The input state from the currently active [TextInputSession]. Returns empty TextFieldValue if
     * there is no active session.
     */
    private val text: TextFieldCharSequence
        get() = session.text

    /** Recording of editing operations for batch editing */
    private val editCommands = mutableVectorOf<ImeEditCommandScope.() -> Unit>()

    /**
     * Wraps this StatelessInputConnection to halt a possible infinite loop in [commitContent]
     * chain.
     *
     * if [StatelessInputConnection] is wrapped via [InputConnectionCompat] without intervention,
     * [commitContent] and [performPrivateCommand] delegates back to their super, which would be
     * this [StatelessInputConnection]. Then, those functions defined here would call the wrapped
     * helper again, causing an infinite loop. Instead this terminal is introduced as a final
     * receiver of [commitContent] and [performPrivateCommand] calls to end the chain when there's
     * no configuration to handle the request.
     *
     * Note; Rather than creating an InputConnection with loads of empty or throwing defaults, we
     * choose to wrap this [StatelessInputConnection] one more time to create this terminal.
     * [terminalInputConnection] should never receive any call other than [commitContent] or
     * [performPrivateCommand].
     *
     * Pseudo inverted stack trace after IME calls [InputConnection.commitContent].
     * 1. StatelessInputConnection#commitContent ->
     * 2. commitContentDelegateInputConnection#commitContent ->
     * 3. terminalInputConnection#commitContent # ends here.
     */
    private val terminalInputConnection =
        object : InputConnectionWrapper(this, false) {
            override fun commitContent(
                inputContentInfo: InputContentInfo,
                flags: Int,
                opts: Bundle?
            ): Boolean {
                return false
            }

            override fun performPrivateCommand(action: String?, data: Bundle?): Boolean {
                // according to docs, return true even if we don't understand the command
                return true
            }
        }

    /**
     * Compose supports below API 25 where [commitContent] is not defined. Support libraries add
     * this functionality for IMEs and Editors via [InputConnectionCompat] and [EditorInfoCompat].
     * To create an InputConnection that supports [commitContent] on all API levels, we need to wrap
     * [StatelessInputConnection] using [InputConnectionCompat.createWrapper].
     *
     * We would like to send [commitContent] calls to the current listener
     * [TextInputSession.onCommitContent] we have in active input session. It is not possible to
     * create a wrapper via [InputConnectionCompat] and then update its listener. Therefore, we
     * cannot simply wrap [StatelessInputConnection] from outside and pass it to the system.
     * Instead, we create this internal wrapper that helps us delegate the [commitContent] calls to
     * the active listener in [session].
     *
     * @see performPrivateCommand
     * @see commitContent
     */
    @Suppress("DEPRECATION")
    private val commitContentDelegateInputConnection =
        InputConnectionCompat.createWrapper(
            terminalInputConnection,
            editorInfo,
            object : OnCommitContentListener {
                override fun onCommitContent(
                    inputContentInfo: InputContentInfoCompat,
                    flags: Int,
                    opts: Bundle?
                ): Boolean {
                    // The below code is mostly copied from `InputConnectionCompat.java`
                    var extras: Bundle? = opts
                    if (
                        Build.VERSION.SDK_INT >= 25 &&
                            (flags and INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0
                    ) {
                        try {
                            inputContentInfo.requestPermission()
                        } catch (e: Exception) {
                            logDebug(
                                "Can't insert content from IME; requestPermission() failed, $e"
                            )
                            return false
                        }
                        // Permissions granted above are revoked automatically by the platform when
                        // the
                        // corresponding InputContentInfo object is garbage collected. To prevent
                        // this from happening prematurely (before the receiving app has had a
                        // chance
                        // to process the content), we set the InputContentInfo object into the
                        // extras of the payload passed to onReceiveContent.
                        val inputContentInfoFmk = inputContentInfo.unwrap() as Parcelable
                        extras = if (opts == null) Bundle() else Bundle(opts)
                        extras.putParcelable(EXTRA_INPUT_CONTENT_INFO, inputContentInfoFmk)
                    }
                    return session.onCommitContent(inputContentInfo.toTransferableContent(extras))
                }
            }
        )

    // region Methods for batch editing and session control
    override fun beginBatchEdit(): Boolean {
        logDebug("beginBatchEdit()")
        return beginBatchEditInternal()
    }

    private fun beginBatchEditInternal() = session.beginBatchEdit()

    override fun endBatchEdit(): Boolean {
        logDebug("endBatchEdit()")
        return endBatchEditInternal()
    }

    private fun endBatchEditInternal() = session.endBatchEdit()

    override fun closeConnection() {
        logDebug("closeConnection()")
        editCommands.clear()
        batchDepth = 0
    }

    // endregion

    // region Callbacks for text editing

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        logDebug("commitText(\"$text\", $newCursorPosition)")
        if (text == null) return true
        session.commitText(text.toString(), newCursorPosition)
        return true
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean {
        logDebug("setComposingRegion($start, $end)")
        session.setComposingRegion(start, end)
        return true
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        logDebug("setComposingText(\"$text\", $newCursorPosition)")
        if (text == null) return true
        session.setComposingText(
            text = text.toString(),
            newCursorPosition = newCursorPosition,
            annotations = (text as? Spanned)?.toAnnotationList()
        )
        return true
    }

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
        logDebug("deleteSurroundingTextInCodePoints($beforeLength, $afterLength)")
        session.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
        return true
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        logDebug("deleteSurroundingText($beforeLength, $afterLength)")
        session.deleteSurroundingText(beforeLength, afterLength)
        return true
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        logDebug("setSelection($start, $end)")
        session.setSelection(start, end)
        return true
    }

    override fun finishComposingText(): Boolean {
        logDebug("finishComposingText()")
        session.finishComposingText()
        return true
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean {
        logDebug("sendKeyEvent($event)")
        session.sendKeyEvent(event)
        return true
    }

    // endregion

    // region Callbacks for retrieving editing buffer info by IME

    override fun getTextBeforeCursor(maxChars: Int, flags: Int): CharSequence {
        // TODO(b/135556699) should return styled text
        val result = text.getTextBeforeSelection(maxChars).toString()
        logDebug("getTextBeforeCursor($maxChars, $flags): $result")
        return result
    }

    override fun getTextAfterCursor(maxChars: Int, flags: Int): CharSequence {
        // TODO(b/135556699) should return styled text
        val result = text.getTextAfterSelection(maxChars).toString()
        logDebug("getTextAfterCursor($maxChars, $flags): $result")
        return result
    }

    override fun getSelectedText(flags: Int): CharSequence? {
        // https://source.chromium.org/chromium/chromium/src/+/master:content/public/android/java/src/org/chromium/content/browser/input/TextInputState.java;l=56;drc=0e20d1eb38227949805a4c0e9d5cdeddc8d23637
        val result: CharSequence? =
            if (text.selection.collapsed) {
                null
            } else {
                // TODO(b/135556699) should return styled text
                text.getSelectedText().toString()
            }
        logDebug("getSelectedText($flags): $result")
        return result
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
        logDebug("requestCursorUpdates($cursorUpdateMode)")
        session.requestCursorUpdates(cursorUpdateMode)
        return true
    }

    override fun performHandwritingGesture(
        gesture: HandwritingGesture,
        executor: Executor?,
        consumer: IntConsumer?
    ) {
        logDebug("performHandwritingGesture($gesture, $executor, $consumer)")
        // This InputConnection#performHandwritingGesture is added on Api 34. No need to support
        // it on earlier versions. Also, IntConsumer.accept() needs Api 24. And we have to create
        // an object class for it.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return

        Api34PerformHandwritingGestureImpl.performHandwritingGesture(
            session,
            gesture,
            executor,
            consumer
        )
    }

    override fun previewHandwritingGesture(
        gesture: PreviewableHandwritingGesture,
        cancellationSignal: CancellationSignal?
    ): Boolean {
        logDebug("previewHandwritingGesture($gesture, $cancellationSignal)")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false

        return Api34PerformHandwritingGestureImpl.previewHandwritingGesture(
            session,
            gesture,
            cancellationSignal
        )
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText {
        logDebug("getExtractedText($request, $flags)")
        //        extractedTextMonitorMode = (flags and InputConnection.GET_EXTRACTED_TEXT_MONITOR)
        // != 0
        //        if (extractedTextMonitorMode) {
        //            currentExtractedTextRequestToken = request?.token ?: 0
        //        }
        // TODO(halilibo): Implement extracted text monitor
        // TODO(b/135556699) should return styled text
        return text.toExtractedText()
    }

    override fun getCursorCapsMode(reqModes: Int): Int {
        logDebug("getCursorCapsMode($reqModes)")
        return TextUtils.getCapsMode(text, text.selection.min, reqModes)
    }

    // endregion

    // region Editor action and Key events.

    override fun performContextMenuAction(id: Int): Boolean {
        logDebug("performContextMenuAction($id)")
        when (id) {
            android.R.id.selectAll -> session.setSelection(0, text.length)
            // TODO(siyamed): Need proper connection to cut/copy/paste
            android.R.id.cut -> sendSynthesizedKeyEvent(KeyEvent.KEYCODE_CUT)
            android.R.id.copy -> sendSynthesizedKeyEvent(KeyEvent.KEYCODE_COPY)
            android.R.id.paste -> sendSynthesizedKeyEvent(KeyEvent.KEYCODE_PASTE)
            android.R.id.startSelectingText -> {} // not supported
            android.R.id.stopSelectingText -> {} // not supported
            android.R.id.copyUrl -> {} // not supported
            android.R.id.switchInputMethod -> {} // not supported
            else -> {
                // not supported
            }
        }
        return false
    }

    private fun sendSynthesizedKeyEvent(code: Int) {
        sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    override fun performEditorAction(editorAction: Int): Boolean {
        logDebug("performEditorAction($editorAction)")

        val imeAction =
            when (editorAction) {
                EditorInfo.IME_ACTION_UNSPECIFIED -> ImeAction.Default
                EditorInfo.IME_ACTION_DONE -> ImeAction.Done
                EditorInfo.IME_ACTION_SEND -> ImeAction.Send
                EditorInfo.IME_ACTION_SEARCH -> ImeAction.Search
                EditorInfo.IME_ACTION_PREVIOUS -> ImeAction.Previous
                EditorInfo.IME_ACTION_NEXT -> ImeAction.Next
                EditorInfo.IME_ACTION_GO -> ImeAction.Go
                else -> {
                    logDebug("IME sent an unrecognized editor action: $editorAction")
                    ImeAction.Default
                }
            }

        session.onImeAction(imeAction)
        return true
    }

    // endregion

    // region Unsupported callbacks

    override fun commitCompletion(text: CompletionInfo?): Boolean {
        logDebug("commitCompletion(${text?.text})")
        // We don't support this callback.
        // The API documents says this should return if the input connection is no longer valid, but
        // The Chromium implementation already returning false, so assuming it is safe to return
        // false if not supported.
        // see
        // https://cs.chromium.org/chromium/src/content/public/android/java/src/org/chromium/content/browser/input/ThreadedInputConnection.java
        return false
    }

    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean {
        // logDebug("commitCorrection($correctionInfo),autoCorrect:$autoCorrect")
        // Should add an event here so that we can implement the autocorrect highlight
        // Bug: 170647219
        // TODO(halilibo): Implement autoCorrect from ImeOptions
        return true
    }

    override fun getHandler(): Handler? {
        logDebug("getHandler()")
        return null // Returns null means using default Handler
    }

    override fun clearMetaKeyStates(states: Int): Boolean {
        logDebug("clearMetaKeyStates($states)")
        // We don't support this callback.
        // The API documents says this should return if the input connection is no longer valid, but
        // The Chromium implementation already returning false, so assuming it is safe to return
        // false if not supported.
        // see
        // https://cs.chromium.org/chromium/src/content/public/android/java/src/org/chromium/content/browser/input/ThreadedInputConnection.java
        return false
    }

    override fun reportFullscreenMode(enabled: Boolean): Boolean {
        logDebug("reportFullscreenMode($enabled)")
        return false // This value is ignored according to the API docs.
    }

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean {
        logDebug("performPrivateCommand($action, $data)")
        return commitContentDelegateInputConnection.performPrivateCommand(action, data)
    }

    override fun commitContent(
        inputContentInfo: InputContentInfo,
        flags: Int,
        opts: Bundle?
    ): Boolean {
        logDebug("commitContent($inputContentInfo, $flags, $opts)")
        return if (Build.VERSION.SDK_INT >= 25) {
            Api25CommitContentImpl.commitContent(
                inputConnection = commitContentDelegateInputConnection,
                inputContentInfo = inputContentInfo,
                flags = flags,
                opts = opts
            )
        } else {
            // This should never happen. Platform does not know about `commitContent` below API 25
            // so it cannot be called.
            false
        }
    }

    // endregion

    private fun logDebug(message: String) {
        if (SIC_DEBUG) {
            Log.d(STATELESS_TAG, "$DEBUG_CLASS.$message")
        }
    }
}

@RequiresApi(25)
private object Api25CommitContentImpl {

    fun commitContent(
        inputConnection: InputConnection,
        inputContentInfo: InputContentInfo,
        flags: Int,
        opts: Bundle?
    ): Boolean {
        return inputConnection.commitContent(inputContentInfo, flags, opts)
    }
}

@RequiresApi(34)
private object Api34PerformHandwritingGestureImpl {
    fun performHandwritingGesture(
        session: TextInputSession,
        gesture: HandwritingGesture,
        executor: Executor?,
        intConsumer: IntConsumer?
    ) {
        val result = session.performHandwritingGesture(gesture)
        if (intConsumer == null) return

        if (executor != null) {
            executor.execute { intConsumer.accept(result) }
        } else {
            intConsumer.accept(result)
        }
    }

    fun previewHandwritingGesture(
        session: TextInputSession,
        gesture: PreviewableHandwritingGesture,
        cancellationSignal: CancellationSignal?
    ): Boolean {
        return session.previewHandwritingGesture(gesture, cancellationSignal)
    }
}

private fun TextFieldCharSequence.toExtractedText(): ExtractedText {
    val res = ExtractedText()
    res.text = this
    res.startOffset = 0
    res.partialEndOffset = length
    res.partialStartOffset = -1 // -1 means full text
    res.selectionStart = selection.min
    res.selectionEnd = selection.max
    res.flags = if ('\n' in this) 0 else ExtractedText.FLAG_SINGLE_LINE
    return res
}

@OptIn(ExperimentalFoundationApi::class)
internal fun InputContentInfoCompat.toTransferableContent(extras: Bundle?): TransferableContent {
    val clipData = ClipData(description, ClipData.Item(contentUri))
    return TransferableContent(
        clipEntry = clipData.toClipEntry(),
        source = TransferableContent.Source.Keyboard,
        clipMetadata = description.toClipMetadata(),
        platformTransferableContent =
            PlatformTransferableContent(linkUri = linkUri, extras = extras ?: Bundle.EMPTY)
    )
}

@VisibleForTesting
internal fun Spanned.toAnnotationList(): List<PlacedAnnotation>? {
    var mutableAnnotationList: MutableList<PlacedAnnotation>? = null
    val spans = getSpans(0, length, Any::class.java)
    spans.forEach { span ->
        span.toAnnotation()?.let { annotation ->
            if (mutableAnnotationList == null) {
                mutableAnnotationList = mutableListOf()
            }
            mutableAnnotationList?.add(
                AnnotatedString.Range(
                    item = annotation,
                    start = getSpanStart(span),
                    end = getSpanEnd(span)
                )
            )
        }
    }
    return mutableAnnotationList
}

// The following functions were borrowed from ui-text module where they are currently private.
private fun Any.toAnnotation(): AnnotatedString.Annotation? {
    return when (this) {
        is BackgroundColorSpan -> {
            SpanStyle(background = Color(this.backgroundColor))
        }
        is ForegroundColorSpan -> {
            SpanStyle(color = Color(this.foregroundColor))
        }
        is StrikethroughSpan -> {
            SpanStyle(textDecoration = TextDecoration.LineThrough)
        }
        is StyleSpan -> {
            this.toSpanStyle()
        }
        is TypefaceSpan -> {
            this.toSpanStyle()
        }
        is UnderlineSpan -> {
            SpanStyle(textDecoration = TextDecoration.Underline)
        }
        else -> null
    }
}

private fun StyleSpan.toSpanStyle(): SpanStyle? {
    /**
     * StyleSpan doc: styles are cumulative -- if both bold and italic are set in separate spans, or
     * if the base style is bold and a span calls for italic, you get bold italic. You can't turn
     * off a style from the base style.
     */
    return when (style) {
        Typeface.BOLD -> {
            SpanStyle(fontWeight = FontWeight.Bold)
        }
        Typeface.ITALIC -> {
            SpanStyle(fontStyle = FontStyle.Italic)
        }
        Typeface.BOLD_ITALIC -> {
            SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
        }
        else -> null
    }
}

private fun TypefaceSpan.toSpanStyle(): SpanStyle {
    val fontFamily =
        when (family) {
            FontFamily.Cursive.name -> FontFamily.Cursive
            FontFamily.Monospace.name -> FontFamily.Monospace
            FontFamily.SansSerif.name -> FontFamily.SansSerif
            FontFamily.Serif.name -> FontFamily.Serif
            else -> {
                optionalFontFamilyFromName(family)
            }
        }
    return SpanStyle(fontFamily = fontFamily)
}

/**
 * Mirrors [androidx.compose.ui.text.font.PlatformTypefaces.optionalOnDeviceFontFamilyByName]
 * behavior with both font weight and font style being Normal in this case
 */
private fun optionalFontFamilyFromName(familyName: String?): FontFamily? {
    if (familyName.isNullOrEmpty()) return null
    val typeface = Typeface.create(familyName, Typeface.NORMAL)
    return typeface
        .takeIf {
            typeface != Typeface.DEFAULT &&
                typeface != Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        ?.let { FontFamily(it) }
}
