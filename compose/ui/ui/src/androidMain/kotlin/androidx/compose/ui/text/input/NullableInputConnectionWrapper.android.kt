/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.text.input

import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.view.KeyEvent
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.HandwritingGesture
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.view.inputmethod.PreviewableHandwritingGesture
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor
import java.util.function.IntConsumer

/**
 * Creates a [NullableInputConnectionWrapper] – see the kdoc on that interface for more info.
 *
 * @param delegate The [InputConnection] that will receive all calls on this object until
 *   `disposeDelegate` or `closeConnection` are called.
 * @param onConnectionClosed A callback that will be invoked the first time `closeConnection` is
 *   called. Will not be invoked by `disposeDelegate`, and will not be invoked if `disposeDelegate`
 *   is called before `closeConnection`. Passed in [NullableInputConnectionWrapper] is the same
 *   instance that is created here.
 */
internal fun NullableInputConnectionWrapper(
    delegate: InputConnection,
    onConnectionClosed: (NullableInputConnectionWrapper) -> Unit
): NullableInputConnectionWrapper =
    when {
        Build.VERSION.SDK_INT >= 34 ->
            NullableInputConnectionWrapperApi34(delegate, onConnectionClosed)
        Build.VERSION.SDK_INT >= 25 ->
            NullableInputConnectionWrapperApi25(delegate, onConnectionClosed)
        Build.VERSION.SDK_INT >= 24 ->
            NullableInputConnectionWrapperApi24(delegate, onConnectionClosed)
        else -> NullableInputConnectionWrapperApi21(delegate, onConnectionClosed)
    }

/**
 * An [InputConnection] that will delegate all calls to a delegate [InputConnection]. This is
 * similar to the platform `InputConnectionWrapper` class, but no-ops when the delegate is null
 * instead of throwing.
 *
 * This class allows the PlatformTextInput system to make stronger guarantees about the lifetime of
 * [InputConnection]s – see [PlatformTextInputMethodRequest.createInputConnection] for documentation
 * about what these guarantees are.
 *
 * This class has two responsibilities besides basic delegation:
 * - Clear its reference to its delegate as eagerly as possible, so even if this instance is leaked
 *   by the system the underlying [InputConnection] is not.
 * - Ensure that [InputConnection.closeConnection] is invoked on the delegate on every API level
 *   where it's available (24+), as soon as possible.
 */
internal sealed interface NullableInputConnectionWrapper : InputConnection {
    /**
     * Call [closeConnection] on the delegate without invoking `onConnectionClosed`, then null out
     * the delegate. Calling this multiple times has no effect. Also cancels the associated
     * `delegateScope`.
     */
    fun disposeDelegate()
}

private open class NullableInputConnectionWrapperApi21(
    delegate: InputConnection,
    private val onConnectionClosed: (NullableInputConnectionWrapper) -> Unit
) : NullableInputConnectionWrapper {

    protected var delegate: InputConnection? = delegate
        private set

    final override fun disposeDelegate() {
        delegate?.let {
            closeDelegate(it)
            delegate = null
        }
    }

    final override fun closeConnection() {
        delegate?.let {
            disposeDelegate()
            onConnectionClosed(this)
        }
    }

    override fun getTextBeforeCursor(p0: Int, p1: Int): CharSequence? =
        delegate?.getTextBeforeCursor(p0, p1)

    override fun getTextAfterCursor(p0: Int, p1: Int): CharSequence? =
        delegate?.getTextAfterCursor(p0, p1)

    override fun getSelectedText(p0: Int): CharSequence? = delegate?.getSelectedText(p0)

    override fun getCursorCapsMode(p0: Int): Int = delegate?.getCursorCapsMode(p0) ?: 0

    override fun getExtractedText(p0: ExtractedTextRequest?, p1: Int): ExtractedText? =
        delegate?.getExtractedText(p0, p1)

    override fun deleteSurroundingText(p0: Int, p1: Int): Boolean =
        delegate?.deleteSurroundingText(p0, p1) ?: false

    override fun deleteSurroundingTextInCodePoints(p0: Int, p1: Int): Boolean = false

    override fun setComposingText(p0: CharSequence?, p1: Int): Boolean =
        delegate?.setComposingText(p0, p1) ?: false

    override fun setComposingRegion(p0: Int, p1: Int): Boolean =
        delegate?.setComposingRegion(p0, p1) ?: false

    override fun finishComposingText(): Boolean = delegate?.finishComposingText() ?: false

    override fun commitText(p0: CharSequence?, p1: Int): Boolean =
        delegate?.commitText(p0, p1) ?: false

    override fun commitCompletion(p0: CompletionInfo?): Boolean =
        delegate?.commitCompletion(p0) ?: false

    override fun commitCorrection(p0: CorrectionInfo?): Boolean =
        delegate?.commitCorrection(p0) ?: false

    override fun setSelection(p0: Int, p1: Int): Boolean = delegate?.setSelection(p0, p1) ?: false

    override fun performEditorAction(p0: Int): Boolean = delegate?.performEditorAction(p0) ?: false

    override fun performContextMenuAction(p0: Int): Boolean =
        delegate?.performContextMenuAction(p0) ?: false

    override fun beginBatchEdit(): Boolean = delegate?.beginBatchEdit() ?: false

    override fun endBatchEdit(): Boolean = delegate?.endBatchEdit() ?: false

    override fun sendKeyEvent(p0: KeyEvent?): Boolean = delegate?.sendKeyEvent(p0) ?: false

    override fun clearMetaKeyStates(p0: Int): Boolean = delegate?.clearMetaKeyStates(p0) ?: false

    override fun reportFullscreenMode(p0: Boolean): Boolean =
        delegate?.reportFullscreenMode(p0) ?: false

    override fun performPrivateCommand(p0: String?, p1: Bundle?): Boolean =
        delegate?.performPrivateCommand(p0, p1) ?: false

    override fun requestCursorUpdates(p0: Int): Boolean =
        delegate?.requestCursorUpdates(p0) ?: false

    // Only supported on higher API level.
    override fun getHandler(): Handler? = null

    // Only supported on higher API level.
    override fun commitContent(p0: InputContentInfo, p1: Int, p2: Bundle?): Boolean = false

    /** Support method to invoke [InputConnection.closeConnection] on supported API levels. */
    protected open fun closeDelegate(delegate: InputConnection) {}
}

@RequiresApi(24)
private open class NullableInputConnectionWrapperApi24(
    delegate: InputConnection,
    onConnectionClosed: (NullableInputConnectionWrapper) -> Unit
) : NullableInputConnectionWrapperApi21(delegate, onConnectionClosed) {

    final override fun deleteSurroundingTextInCodePoints(p0: Int, p1: Int): Boolean =
        delegate?.deleteSurroundingTextInCodePoints(p0, p1) ?: false

    final override fun getHandler(): Handler? = delegate?.handler

    final override fun closeDelegate(delegate: InputConnection) {
        delegate.closeConnection()
    }
}

@RequiresApi(25)
private open class NullableInputConnectionWrapperApi25(
    delegate: InputConnection,
    onConnectionClosed: (NullableInputConnectionWrapper) -> Unit
) : NullableInputConnectionWrapperApi24(delegate, onConnectionClosed) {

    final override fun commitContent(p0: InputContentInfo, p1: Int, p2: Bundle?): Boolean =
        delegate?.commitContent(p0, p1, p2) ?: false
}

@RequiresApi(34)
private open class NullableInputConnectionWrapperApi34(
    delegate: InputConnection,
    onConnectionClosed: (NullableInputConnectionWrapper) -> Unit
) : NullableInputConnectionWrapperApi25(delegate, onConnectionClosed) {
    final override fun performHandwritingGesture(
        gesture: HandwritingGesture,
        executor: Executor?,
        consumer: IntConsumer?
    ) {
        delegate?.performHandwritingGesture(gesture, executor, consumer)
    }

    final override fun previewHandwritingGesture(
        gesture: PreviewableHandwritingGesture,
        cancellationSignal: CancellationSignal?
    ): Boolean = delegate?.previewHandwritingGesture(gesture, cancellationSignal) ?: false
}
