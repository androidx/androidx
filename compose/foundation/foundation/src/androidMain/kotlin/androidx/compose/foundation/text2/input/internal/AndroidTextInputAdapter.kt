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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.text2.input.internal

import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.Choreographer
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextEditFilter
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformTextInput
import androidx.compose.ui.text.input.PlatformTextInputAdapter
import androidx.core.view.inputmethod.EditorInfoCompat
import java.util.concurrent.Executor
import org.jetbrains.annotations.TestOnly

/**
 * Enable to print logs during debugging, see [logDebug].
 */
@VisibleForTesting
internal const val TIA_DEBUG = false
private const val TAG = "AndroidTextInputAdapter"

internal class AndroidTextInputAdapter constructor(
    view: View,
    private val platformTextInput: PlatformTextInput
) : PlatformTextInputAdapter {

    private var currentTextInputSession: EditableTextInputSession? = null

    private val inputMethodManager = ComposeInputMethodManager(view)

    private val textInputCommandExecutor = TextInputCommandExecutor(view, inputMethodManager)

    private val resetListener = EditProcessor.ResetListener { old, new ->
        val needUpdateSelection = (old.selectionInChars != new.selectionInChars) ||
            old.compositionInChars != new.compositionInChars
        if (needUpdateSelection) {
            inputMethodManager.updateSelection(
                selectionStart = new.selectionInChars.min,
                selectionEnd = new.selectionInChars.max,
                compositionStart = new.compositionInChars?.min ?: -1,
                compositionEnd = new.compositionInChars?.max ?: -1
            )
        }

        if (!old.contentEquals(new)) {
            inputMethodManager.restartInput()
        }
    }

    override fun createInputConnection(outAttrs: EditorInfo): InputConnection {
        logDebug { "createInputConnection" }
        val value = currentTextInputSession?.value ?: TextFieldCharSequence()
        val imeOptions = currentTextInputSession?.imeOptions ?: ImeOptions.Default

        logDebug { "createInputConnection.value = $value" }

        outAttrs.update(value, imeOptions)

        val inputConnection = StatelessInputConnection(
            activeSessionProvider = { currentTextInputSession }
        )
        testInputConnectionCreatedListener?.invoke(outAttrs, inputConnection)
        return inputConnection
    }

    /**
     * Clear the resources before being disposed. Any active session should be stopped from
     * receiving any more input.
     */
    override fun onDisposed() {
        currentTextInputSession?.dispose()
    }

    /**
     * Start a new input session and close the active session if there is one. This session will
     * be responsible for maintaining an agreement between text editor and lower level platform
     * APIs to agree on where to direct incoming requests. If there are multiple text editors on a
     * screen, only the active(focused) one should receive inputs from the platform input
     * connection.
     *
     * Each session is tied with a [TextFieldState] from the start. The current editing state of
     * the text editor is read and written via platform calls as long as the session is active.
     * [ImeOptions] are used to initialize an [InputConnection] when [createInputConnection] is
     * called. Any change in [ImeOptions] should start a new session. Regularly starting a new input
     * session also instructs the platform to request a new [InputConnection] but that's not
     * guaranteed. [AndroidTextInputAdapter] behaves as a bridge to establish a stable communication
     * channel between active [TextInputSession] in this class and the [InputConnection] used by
     * the platform.
     *
     * @param state Text editing state
     * @param imeOptions How to configure the IME when creating new [InputConnection]s
     * @param initialFilter The initial [TextEditFilter]. The filter can be changed after the
     * session is started by calling [TextInputSession.setFilter].
     * @param onImeActionPerformed A callback to pass received editor action from IME.
     * @return A handle to manage active session between Adapter and platform APIs.
     */
    fun startInputSession(
        state: TextFieldState,
        imeOptions: ImeOptions,
        initialFilter: TextEditFilter?,
        onImeActionPerformed: (ImeAction) -> Unit
    ): TextInputSession {
        if (!isMainThread()) {
            throw IllegalStateException("Input sessions can only be started from the main thread.")
        }
        logDebug { "startInputSession.state = $state" }
        platformTextInput.requestInputFocus()
        textInputCommandExecutor.send(TextInputCommand.StartInput)

        val nextSession = createEditableTextInputSession(
            state = state,
            imeOptions = imeOptions,
            initialFilter = initialFilter,
            onImeActionPerformed = onImeActionPerformed
        )
        currentTextInputSession = nextSession
        return nextSession
    }

    private fun createEditableTextInputSession(
        state: TextFieldState,
        imeOptions: ImeOptions,
        initialFilter: TextEditFilter?,
        onImeActionPerformed: (ImeAction) -> Unit
    ) = object : EditableTextInputSession {

        // Immediately start listening to reset events.
        init {
            state.editProcessor.addResetListener(resetListener)
        }

        // region TextInputSession
        override val isOpen: Boolean
            get() = currentTextInputSession == this

        override fun showSoftwareKeyboard() {
            if (isOpen) {
                textInputCommandExecutor.send(TextInputCommand.ShowKeyboard)
            }
        }

        override fun hideSoftwareKeyboard() {
            if (isOpen) {
                textInputCommandExecutor.send(TextInputCommand.HideKeyboard)
            }
        }

        override fun dispose() {
            state.editProcessor.removeResetListener(resetListener)
            stopInputSession(this)
        }
        // endregion

        // region EditableTextInputSession
        override val value: TextFieldCharSequence
            get() = state.text

        private var filter: TextEditFilter? = initialFilter

        override fun setFilter(filter: TextEditFilter?) {
            this.filter = filter
        }

        override fun requestEdits(editCommands: List<EditCommand>) {
            state.editProcessor.update(editCommands, filter)
        }

        override fun sendKeyEvent(keyEvent: KeyEvent) {
            inputMethodManager.sendKeyEvent(keyEvent)
        }

        override val imeOptions: ImeOptions = imeOptions

        override fun onImeAction(imeAction: ImeAction) = onImeActionPerformed(imeAction)
        // endregion
    }

    /**
     * Stop the given [session] if it's active and clear resources.
     */
    private fun stopInputSession(session: TextInputSession) {
        if (!isMainThread()) {
            throw IllegalStateException("Input sessions can only be stopped from the main thread.")
        }
        if (currentTextInputSession == session) {
            currentTextInputSession = null
            platformTextInput.releaseInputFocus()
            textInputCommandExecutor.send(TextInputCommand.StopInput)
        }
    }

    companion object {
        private var testInputConnectionCreatedListener: ((EditorInfo, InputConnection) -> Unit)? =
            null

        /**
         * Set a function to be called when an [AndroidTextInputAdapter] returns from
         * [createInputConnection]. This method should only be used to assert on the [EditorInfo]
         * and grab the [InputConnection] to inject commands.
         */
        @TestOnly
        @VisibleForTesting
        fun setInputConnectionCreatedListenerForTests(
            listener: ((EditorInfo, InputConnection) -> Unit)?
        ) {
            testInputConnectionCreatedListener = listener
        }
    }
}

/**
 * Commands that can be sent into [TextInputCommandExecutor].
 */
internal enum class TextInputCommand {
    StartInput,
    StopInput,
    ShowKeyboard,
    HideKeyboard;
}

/**
 * TODO: kdoc
 */
internal class TextInputCommandExecutor(
    private val view: View,
    private val inputMethodManager: ComposeInputMethodManager,
    private val inputCommandProcessorExecutor: Executor = Choreographer.getInstance().asExecutor(),
) {
    /**
     * A channel that is used to debounce rapid operations such as showing/hiding the keyboard and
     * starting/stopping input, so we can make the minimal number of calls on the
     * [inputMethodManager]. The [TextInputCommand]s sent to this channel are processed by
     * [processQueue].
     */
    private val textInputCommandQueue = mutableVectorOf<TextInputCommand>()
    private var frameCallback: Runnable? = null

    fun send(textInputCommand: TextInputCommand) {
        textInputCommandQueue += textInputCommand
        if (frameCallback == null) {
            frameCallback = Runnable {
                frameCallback = null
                processQueue()
            }.also(inputCommandProcessorExecutor::execute)
        }
    }

    private fun processQueue() {
        logDebug { "processQueue has started" }
        // When focus changes to a non-Compose view, the system will take care of managing the
        // keyboard (via ImeFocusController) so we don't need to do anything. This can happen
        // when a Compose text field is focused, then the user taps on an EditText view.
        // And any commands that come in while we're not focused should also just be ignored,
        // since no unfocused view should be messing with the keyboard.
        // TODO(b/215761849) When focus moves to a different ComposeView than this one, this
        //  logic doesn't work and the keyboard is not hidden.
        if (!view.isFocused) {
            logDebug { "processing queue returning early because the view is not focused" }
            // All queued commands should be ignored.
            textInputCommandQueue.clear()
            return
        }

        // Multiple commands may have been queued up in the channel while this function was
        // waiting to be resumed. We don't execute the commands as they come in because making a
        // bunch of calls to change the actual IME quickly can result in flickers. Instead, we
        // manually coalesce the commands to figure out the minimum number of IME operations we
        // need to get to the desired final state.
        // The queued commands effectively operate on a simple state machine consisting of two
        // flags:
        //   1. Whether to start a new input connection (true), tear down the input connection
        //      (false), or leave the current connection as-is (null).
        var startInput: Boolean? = null
        //   2. Whether to show the keyboard (true), hide the keyboard (false), or leave the
        //      keyboard visibility as-is (null).
        var showKeyboard: Boolean? = null

        // And a function that performs the appropriate state transition given a command.
        fun TextInputCommand.applyToState() {
            when (this) {
                TextInputCommand.StartInput -> {
                    // Any commands before restarting the input are meaningless since they would
                    // apply to the connection we're going to tear down and recreate.
                    // Starting a new connection implicitly stops the previous connection.
                    startInput = true
                    // It doesn't make sense to start a new connection without the keyboard
                    // showing.
                    showKeyboard = true
                }

                TextInputCommand.StopInput -> {
                    startInput = false
                    // It also doesn't make sense to keep the keyboard visible if it's not
                    // connected to anything. Note that this is different than the Android
                    // default behavior for Views, which is to keep the keyboard showing even
                    // after the view that the IME was shown for loses focus.
                    // See this doc for some notes and discussion on whether we should auto-hide
                    // or match Android:
                    // https://docs.google.com/document/d/1o-y3NkfFPCBhfDekdVEEl41tqtjjqs8jOss6txNgqaw/edit?resourcekey=0-o728aLn51uXXnA4Pkpe88Q#heading=h.ieacosb5rizm
                    showKeyboard = false
                }

                TextInputCommand.ShowKeyboard,
                TextInputCommand.HideKeyboard -> {
                    // Any keyboard visibility commands sent after input is stopped but before
                    // input is started should be ignored.
                    // Otherwise, the last visibility command sent either before the last stop
                    // command, or after the last start command, is the one that should take
                    // effect.
                    if (startInput != false) {
                        showKeyboard = this == TextInputCommand.ShowKeyboard
                    }
                }
            }
        }

        // Feed all the queued commands into the state machine.
        textInputCommandQueue.forEach { command ->
            command.applyToState()
            logDebug { "command: $command applied to state" }
        }

        logDebug { "commands are applied. startInput = $startInput, showKeyboard = $showKeyboard" }

        // Now that we've calculated what operations we need to perform on the actual input
        // manager, perform them.
        // If the keyboard visibility was changed after starting a new connection, we need to
        // perform that operation change after starting it.
        // If the keyboard visibility was changed before closing the connection, we need to
        // perform that operation before closing the connection so it doesn't no-op.
        if (startInput == true) {
            restartInputImmediately()
        }
        showKeyboard?.also(::setKeyboardVisibleImmediately)
        if (startInput == false) {
            restartInputImmediately()
        }
    }

    /** Immediately restart the IME connection, bypassing the [textInputCommandQueue]. */
    private fun restartInputImmediately() {
        logDebug { "restartInputImmediately" }
        inputMethodManager.restartInput()
    }

    /** Immediately show or hide the keyboard, bypassing the [textInputCommandQueue]. */
    private fun setKeyboardVisibleImmediately(visible: Boolean) {
        logDebug { "setKeyboardVisibleImmediately(visible: $visible)" }
        if (visible) {
            inputMethodManager.showSoftInput()
        } else {
            inputMethodManager.hideSoftInput()
        }
    }
}

private fun Choreographer.asExecutor(): Executor = Executor { runnable ->
    postFrameCallback { runnable.run() }
}

/**
 * Fills necessary info of EditorInfo.
 */
internal fun EditorInfo.update(textFieldValue: TextFieldCharSequence, imeOptions: ImeOptions) {
    this.imeOptions = when (imeOptions.imeAction) {
        ImeAction.Default -> {
            if (imeOptions.singleLine) {
                // this is the last resort to enable single line
                // Android IME still shows return key even if multi line is not send
                // TextView.java#onCreateInputConnection
                EditorInfo.IME_ACTION_DONE
            } else {
                EditorInfo.IME_ACTION_UNSPECIFIED
            }
        }
        ImeAction.None -> EditorInfo.IME_ACTION_NONE
        ImeAction.Go -> EditorInfo.IME_ACTION_GO
        ImeAction.Next -> EditorInfo.IME_ACTION_NEXT
        ImeAction.Previous -> EditorInfo.IME_ACTION_PREVIOUS
        ImeAction.Search -> EditorInfo.IME_ACTION_SEARCH
        ImeAction.Send -> EditorInfo.IME_ACTION_SEND
        ImeAction.Done -> EditorInfo.IME_ACTION_DONE
        else -> error("invalid ImeAction")
    }

    this.inputType = when (imeOptions.keyboardType) {
        KeyboardType.Text -> InputType.TYPE_CLASS_TEXT
        KeyboardType.Ascii -> {
            this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_FORCE_ASCII
            InputType.TYPE_CLASS_TEXT
        }
        KeyboardType.Number ->
            InputType.TYPE_CLASS_NUMBER
        KeyboardType.Phone ->
            InputType.TYPE_CLASS_PHONE
        KeyboardType.Uri ->
            InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI
        KeyboardType.Email ->
            InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        KeyboardType.Password ->
            InputType.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
        KeyboardType.NumberPassword ->
            InputType.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD
        KeyboardType.Decimal ->
            InputType.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_FLAG_DECIMAL
        else -> error("Invalid Keyboard Type")
    }

    if (!imeOptions.singleLine) {
        if (hasFlag(this.inputType, InputType.TYPE_CLASS_TEXT)) {
            // TextView.java#setInputTypeSingleLine
            this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE

            if (imeOptions.imeAction == ImeAction.Default) {
                this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_NO_ENTER_ACTION
            }
        }
    }

    if (hasFlag(this.inputType, InputType.TYPE_CLASS_TEXT)) {
        when (imeOptions.capitalization) {
            KeyboardCapitalization.Characters -> {
                this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            }

            KeyboardCapitalization.Words -> {
                this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            }

            KeyboardCapitalization.Sentences -> {
                this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }

            else -> {
                /* do nothing */
            }
        }

        if (imeOptions.autoCorrect) {
            this.inputType = this.inputType or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
        }
    }

    this.initialSelStart = textFieldValue.selectionInChars.start
    this.initialSelEnd = textFieldValue.selectionInChars.end

    EditorInfoCompat.setInitialSurroundingText(this, textFieldValue)

    this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN
}

private fun hasFlag(bits: Int, flag: Int): Boolean = (bits and flag) == flag

private fun logDebug(tag: String = TAG, content: () -> String) {
    if (TIA_DEBUG) {
        Log.d(tag, content())
    }
}

private fun isMainThread() = Looper.myLooper() === Looper.getMainLooper()