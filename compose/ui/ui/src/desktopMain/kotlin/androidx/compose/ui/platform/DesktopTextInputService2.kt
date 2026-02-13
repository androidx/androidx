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

package androidx.compose.ui.platform

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.awt.toAwtRectangleRounded
import androidx.compose.ui.scene.ComposeSceneMediator
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextEditorState
import androidx.compose.ui.text.substring
import java.awt.Rectangle
import java.awt.event.InputMethodEvent
import java.awt.event.KeyEvent
import java.awt.font.TextHitInfo
import java.awt.im.InputMethodRequests
import java.text.AttributedCharacterIterator
import java.text.AttributedString
import java.text.CharacterIterator
import javax.swing.SwingUtilities
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs

internal class DesktopTextInputService2(
    private val component: PlatformComponent,
) {

    private var inputMethodSession: InputMethodSession? = null

    private val extraCommitEventWorkaround = ExtraCommitEventWorkaround()

    suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
        val coroutineScope = CoroutineScope(currentCoroutineContext())
        suspendCancellableCoroutine<Nothing> { continuation ->
            coroutineScope.startInput(request)
            continuation.invokeOnCancellation {
                stopInput()
                coroutineScope.cancel()
            }
        }
    }

    private fun CoroutineScope.startInput(request: PlatformTextInputMethodRequest) {
        extraCommitEventWorkaround.onStartInput()

        component.enableInput(
            InputMethodSession(component, request).also {
                inputMethodSession = it
            }
        )

        // During composition, the selection should be collapsed and at the end of the composition.
        // If it changes unexpectedly, finish composing.
        // BTF1 commits the composition itself when the user clicks somewhere, but BTF2
        // does not. This commits the composition for BTF2
        launch {
            snapshotFlow { request.state.selection }.collect { selection ->
                val composition = request.state.composition ?: return@collect
                if (!selection.collapsed || (selection.end != composition.end)) {
                    request.editText {
                        finishComposingText()
                    }
                }
            }
        }

        // When something commits (or otherwise ends) the composition, end it for the platform too
        launch {
            snapshotFlow { request.state.composition }.collect { composition ->
                val composingText = inputMethodSession?.imeComposingText ?: return@collect
                if (composingText.isNotEmpty() && (composition == null)) {
                    SwingUtilities.invokeLater {
                        // The event sent due to ending the composition must be ignored because the
                        // composition has already been committed.
                        // Set this in `invokeLater` to make sure we ignore the event scheduled by
                        // `component.endComposition()`, and not some event that has already been
                        // scheduled beforehand.
                        inputMethodSession?.ignoreNextInputMethodEvent = true
                    }
                    component.endComposition()
                }
            }
        }
    }

    private fun stopInput() {
        component.disableInput()

        this.inputMethodSession = null
    }

    fun onKeyEvent(keyEvent: KeyEvent) {
        when (keyEvent.id) {
            KeyEvent.KEY_TYPED ->
                inputMethodSession?.charKeyPressed = !keyEvent.keyChar.isISOControl()
            KeyEvent.KEY_RELEASED ->
                inputMethodSession?.charKeyPressed = false
        }
    }

    fun inputMethodTextChanged(event: InputMethodEvent) {
        if (event.isConsumed) return
        val inputMethodSession = inputMethodSession ?: return

        if (extraCommitEventWorkaround.shouldIgnoreEvent(event)) return

        inputMethodSession.inputMethodTextChanged(event)
        event.consume()
    }
}

private class InputMethodSession(
    private val component: PlatformComponent,
    private val request: PlatformTextInputMethodRequest
) : InputMethodRequests {

    private val state: TextEditorState
        get() = request.state

    private val selection: TextRange
        get() = state.selection

    private val composition: TextRange?
        get() = state.composition

    // The composing text from the last InputMethodEvent
    var imeComposingText: String = ""
        private set

    var ignoreNextInputMethodEvent = false

    // This is required to support input of accented characters using press-and-hold method (http://support.apple.com/kb/PH11264).
    // JDK currently properly supports this functionality only for TextComponent/JTextComponent descendants.
    // For our editor component we need this workaround.
    // After https://bugs.openjdk.java.net/browse/JDK-8074882 is fixed, this workaround should be replaced with a proper solution.
    var charKeyPressed: Boolean = false
    private var needToDeletePreviousChar: Boolean = false

    override fun getLocationOffset(x: Int, y: Int): TextHitInfo? {
        if (composition != null) {
            // TODO: to properly implement this method we need to somehow have access to
            //  Paragraph at this point
            return TextHitInfo.leading(0)
        }
        return null
    }

    override fun cancelLatestCommittedText(
        attributes: Array<AttributedCharacterIterator.Attribute>?
    ): AttributedCharacterIterator? {
        return null
    }

    override fun getInsertPositionOffset(): Int {
        val composedStartIndex = composition?.start ?: 0
        val composedEndIndex = composition?.end ?: 0

        val caretIndex = selection.start
        if (caretIndex < composedStartIndex) {
            return caretIndex
        }
        if (caretIndex < composedEndIndex) {
            return composedStartIndex
        }
        return caretIndex - (composedEndIndex - composedStartIndex)
    }

    override fun getCommittedTextLength() =
        state.length - (composition?.length ?: 0)

    override fun getSelectedText(
        attributes: Array<AttributedCharacterIterator.Attribute>?
    ): AttributedCharacterIterator {
        if (charKeyPressed && (hostOs == OS.MacOS)) {
            needToDeletePreviousChar = true
        }
        val str = state.substring(selection)
        return AttributedString(str).iterator
    }

    override fun getTextLocation(offset: TextHitInfo?): Rectangle? {
        val awtRect = request.focusedRectInRoot()?.let {
            val centerX = it.topCenter.x
            it.copy(left = centerX, right = centerX).toAwtRectangleRounded(component.density)
        } ?: return null

        val locationOnScreen = component.locationOnScreen
        awtRect.translate(locationOnScreen.x, locationOnScreen.y)
        return awtRect
    }

    override fun getCommittedText(
        beginIndex: Int,
        endIndex: Int,
        attributes: Array<AttributedCharacterIterator.Attribute>?
    ): AttributedCharacterIterator {
        val comp = composition
        // When input is performed with Pinyin and backspace pressed,
        // comp is null and beginIndex > endIndex.
        // TODO Check is this an expected behavior?
        val range = TextRange(
            start = beginIndex.coerceAtMost(state.length),
            end = endIndex.coerceAtMost(state.length)
        )
        if (comp == null) {
            val res = state.substring(range)
            return AttributedString(res).iterator
        }
        val committed = state.substring(
            TextRange(
                min(range.min, comp.min).coerceAtMost(state.length),
                max(range.max, comp.max).coerceAtMost(state.length)
            )
        )
        return AttributedString(committed).iterator
    }

    fun inputMethodTextChanged(event: InputMethodEvent) {
        val committedText = event.committedText
        val composingText = event.composingText

        imeComposingText = composingText

        if (ignoreNextInputMethodEvent) {
            ignoreNextInputMethodEvent = false
            return
        }

        request.editText {
            if (needToDeletePreviousChar) {
                if ((selection.min > 0) && composingText.isEmpty() && (composition == null)) {
                    deleteSurroundingTextInCodePoints(1, 0)
                }
                needToDeletePreviousChar = false
            }
            commitText(committedText, 1)
            if (composingText.isNotEmpty()) {
                setComposingText(composingText, 1)
            }
        }
    }
}

/**
 * The committed text specified by the event, or an empty string if none.
 */
internal val InputMethodEvent.committedText: String
    get() = text.substringOrEmpty(0, committedCharacterCount)


/**
 * The composing text specified by the event, or an empty string if none.
 */
internal val InputMethodEvent.composingText: String
    get() = text.substringOrEmpty(committedCharacterCount, null)


/**
 * Returns the substring between [start] (inclusive) and [end] (exclusive, or the end of the
 * iterator, if `null`) of the given [AttributedCharacterIterator].
 */
private fun AttributedCharacterIterator?.substringOrEmpty(
    start: Int,
    end: Int? = null
) : String {
    if (this == null) return ""

    return buildString {
        index = start
        var c: Char = current()
        while ((c != CharacterIterator.DONE) && ((end == null) || (index < end))) {
            append(c)
            c = next()
        }
    }
}

/**
 * Implements a workaround for https://youtrack.jetbrains.com/issue/CMP-7976
 *
 * JBR sends an extra [InputMethodEvent] when focus moves away from a text field. This event
 * means to commit the current composition. Unfortunately, because we use a single actual Swing
 * component (see [ComposeSceneMediator]) as a source of [InputMethodEvent]s, this event gets
 * delivered to a new text session if the focus switches away to another text field.
 *
 * Regardless, Compose text fields commit their composition on focus loss (but not window focus
 * loss) themselves, so we don't need this event.
 */
private class ExtraCommitEventWorkaround {

    private var composingText: String = ""

    private var receivedInputMethodEventsSinceStartInput = false

    fun onStartInput() {
        receivedInputMethodEventsSinceStartInput = false
    }

    /**
     * Returns whether the given event should be ignored.
     */
    fun shouldIgnoreEvent(event: InputMethodEvent): Boolean {
        val isFirstEventAfterStartInput = !receivedInputMethodEventsSinceStartInput
        receivedInputMethodEventsSinceStartInput = true

        val currentComposingText = composingText
        composingText = event.composingText

        return isFirstEventAfterStartInput &&
            event.committedText == currentComposingText &&
            event.composingText.isEmpty()
    }
}