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

package androidx.compose.foundation.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.internal.CodepointTransformation
import androidx.compose.foundation.text.input.then
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow

/**
 * BasicSecureTextField is specifically designed for password entry fields and is a preconfigured
 * alternative to [BasicTextField]. It only supports a single line of content and comes with default
 * settings for [KeyboardOptions], [InputTransformation], and [CodepointTransformation] that are
 * appropriate for entering secure content. Additionally, some context menu actions like cut, copy,
 * and drag are disabled for added security.
 *
 * @param state [TextFieldState] object that holds the internal state of a [BasicSecureTextField].
 * @param modifier optional [Modifier] for this text field.
 * @param enabled controls the enabled state of the [BasicSecureTextField]. When `false`, the text
 *   field will be neither editable nor focusable, the input of the text field will not be
 *   selectable.
 * @param readOnly controls the editable state of the [BasicSecureTextField]. When `true`, the text
 *   field can not be modified, however, a user can focus on it. Read-only text fields are usually
 *   used to display pre-filled forms that a user can not edit.
 * @param inputTransformation Optional [InputTransformation] that will be used to transform changes
 *   to the [TextFieldState] made by the user. The transformation will be applied to changes made by
 *   hardware and software keyboard events, pasting or dropping text, accessibility services, and
 *   tests. The transformation will _not_ be applied when changing the [state] programmatically, or
 *   when the transformation is changed. If the transformation is changed on an existing text field,
 *   it will be applied to the next user edit. The transformation will not immediately affect the
 *   current [state].
 * @param textStyle Style configuration for text content that's displayed in the editor.
 * @param keyboardOptions Software keyboard options that contain configurations such as
 *   [KeyboardType] and [ImeAction]. This composable by default configures [KeyboardOptions] for a
 *   secure text field by disabling auto correct and setting [KeyboardType] to
 *   [KeyboardType.Password].
 * @param onKeyboardAction Called when the user presses the action button in the input method editor
 *   (IME), or by pressing the enter key on a hardware keyboard. By default this parameter is null,
 *   and would execute the default behavior for a received IME Action e.g., [ImeAction.Done] would
 *   close the keyboard, [ImeAction.Next] would switch the focus to the next focusable item on the
 *   screen.
 * @param onTextLayout Callback that is executed when the text layout becomes queryable. The
 *   callback receives a function that returns a [TextLayoutResult] if the layout can be calculated,
 *   or null if it cannot. The function reads the layout result from a snapshot state object, and
 *   will invalidate its caller when the layout result changes. A [TextLayoutResult] object contains
 *   paragraph information, size of the text, baselines and other details. The callback can be used
 *   to add additional decoration or functionality to the text. For example, to draw a cursor or
 *   selection around the text. [Density] scope is the one that was used while creating the given
 *   text layout.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for this TextField. You can create and pass in your own remembered [MutableInteractionSource]
 *   if you want to observe [Interaction]s and customize the appearance / behavior of this TextField
 *   for different [Interaction]s.
 * @param cursorBrush [Brush] to paint cursor with. If [SolidColor] with [Color.Unspecified]
 *   provided, there will be no cursor drawn.
 * @param decorator Allows to add decorations around text field, such as icon, placeholder, helper
 *   messages or similar, and automatically increase the hit target area of the text field.
 * @param textObfuscationMode Determines the method used to obscure the input text.
 * @param textObfuscationCharacter Which character to use while obfuscating the text. It doesn't
 *   have an effect when [textObfuscationMode] is set to [TextObfuscationMode.Visible].
 */
// This takes a composable lambda, but it is not primarily a container.
@Suppress("ComposableLambdaParameterPosition")
@Composable
fun BasicSecureTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    inputTransformation: InputTransformation? = null,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.SecureTextField,
    onKeyboardAction: KeyboardActionHandler? = null,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorator: TextFieldDecorator? = null,
    // Last parameter must not be a function unless it's intended to be commonly used as a trailing
    // lambda.
    textObfuscationMode: TextObfuscationMode = TextObfuscationMode.RevealLastTyped,
    textObfuscationCharacter: Char = DefaultObfuscationCharacter,
) {
    val obfuscationMaskState = rememberUpdatedState(textObfuscationCharacter)
    val secureTextFieldController = remember { SecureTextFieldController(obfuscationMaskState) }
    LaunchedEffect(secureTextFieldController) {
        // start a coroutine that listens for scheduled hide events.
        secureTextFieldController.observeHideEvents()
    }

    // revealing last typed character depends on two conditions;
    // 1 - Requested Obfuscation method
    // 2 - if the system allows it
    val revealLastTypedEnabled = textObfuscationMode == TextObfuscationMode.RevealLastTyped

    // while toggling between obfuscation methods if the revealing gets disabled, reset the reveal.
    LaunchedEffect(revealLastTypedEnabled) {
        if (!revealLastTypedEnabled) {
            secureTextFieldController.passwordInputTransformation.hide()
        }
    }

    val codepointTransformation =
        remember(textObfuscationMode) {
            when (textObfuscationMode) {
                TextObfuscationMode.RevealLastTyped -> {
                    secureTextFieldController.codepointTransformation
                }
                TextObfuscationMode.Hidden -> {
                    CodepointTransformation { _, _ -> obfuscationMaskState.value.code }
                }
                else -> null
            }
        }

    val secureTextFieldModifier =
        modifier.then(
            if (revealLastTypedEnabled) {
                secureTextFieldController.focusChangeModifier
            } else {
                Modifier
            }
        )

    DisableCutCopy {
        BasicTextField(
            state = state,
            modifier = secureTextFieldModifier,
            enabled = enabled,
            readOnly = readOnly,
            inputTransformation =
                if (revealLastTypedEnabled) {
                    inputTransformation.then(secureTextFieldController.passwordInputTransformation)
                } else inputTransformation,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            lineLimits = TextFieldLineLimits.SingleLine,
            onTextLayout = onTextLayout,
            interactionSource = interactionSource,
            cursorBrush = cursorBrush,
            codepointTransformation = codepointTransformation,
            decorator = decorator,
            isPassword = true,
        )
    }
}

/** Enables chaining nullable transformations with the regular chaining order. */
private fun InputTransformation?.then(next: InputTransformation?): InputTransformation? {
    return when {
        this == null -> next
        next == null -> this
        else -> this.then(next)
    }
}

internal class SecureTextFieldController(private val obfuscationMaskState: State<Char>) {
    /**
     * A special [InputTransformation] that tracks changes to the content to identify the last typed
     * character to reveal. `scheduleHide` lambda is delegated to a member function to be able to
     * use [passwordInputTransformation] instance.
     */
    val passwordInputTransformation = PasswordInputTransformation(::scheduleHide)

    /** Pass to [BasicTextField] for obscuring text input. */
    val codepointTransformation = CodepointTransformation { codepointIndex, codepoint ->
        if (codepointIndex == passwordInputTransformation.revealCodepointIndex) {
            // reveal the last typed character by not obscuring it
            codepoint
        } else {
            obfuscationMaskState.value.code
        }
    }

    val focusChangeModifier =
        Modifier.onFocusChanged { if (!it.isFocused) passwordInputTransformation.hide() }

    private val resetTimerSignal = Channel<Unit>(Channel.UNLIMITED)

    suspend fun observeHideEvents() {
        resetTimerSignal.consumeAsFlow().collectLatest {
            delay(LAST_TYPED_CHARACTER_REVEAL_DURATION_MILLIS)
            passwordInputTransformation.hide()
        }
    }

    private fun scheduleHide() {
        // signal the listener that a new hide call is scheduled.
        val result = resetTimerSignal.trySend(Unit)
        if (result.isFailure) {
            passwordInputTransformation.hide()
        }
    }
}

/**
 * Special filter that tracks the changes in a TextField to identify the last typed character and
 * mark it for reveal in password fields.
 *
 * @param scheduleHide A lambda that schedules a [hide] call into future after a new character is
 *   typed.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class PasswordInputTransformation(val scheduleHide: () -> Unit) : InputTransformation {
    // TODO: Consider setting this as a tracking annotation in AnnotatedString.
    internal var revealCodepointIndex by mutableIntStateOf(-1)
        private set

    override fun TextFieldBuffer.transformInput() {
        // We only care about a single character insertion changes
        val singleCharacterInsertion =
            changes.changeCount == 1 &&
                changes.getRange(0).length == 1 &&
                changes.getOriginalRange(0).length == 0

        // if there is an expanded selection, don't reveal anything
        if (!singleCharacterInsertion || hasSelection) {
            revealCodepointIndex = -1
            return
        }

        val insertionPoint = changes.getRange(0).min
        if (revealCodepointIndex != insertionPoint) {
            // start the timer for auto hide
            scheduleHide()
            revealCodepointIndex = insertionPoint
        }
    }

    /** Removes any revealed character index. Everything goes back into hiding. */
    fun hide() {
        revealCodepointIndex = -1
    }
}

// adopted from PasswordTransformationMethod from Android platform.
private const val LAST_TYPED_CHARACTER_REVEAL_DURATION_MILLIS = 1500L

private const val DefaultObfuscationCharacter: Char = '\u2022'

/**
 * Overrides the TextToolbar and keyboard shortcuts to never allow copy or cut options by the
 * composables inside [content].
 */
@Composable
private fun DisableCutCopy(content: @Composable () -> Unit) {
    val currentToolbar = LocalTextToolbar.current
    val copyDisabledToolbar =
        remember(currentToolbar) {
            object : TextToolbar by currentToolbar {
                override fun showMenu(
                    rect: Rect,
                    onCopyRequested: (() -> Unit)?,
                    onPasteRequested: (() -> Unit)?,
                    onCutRequested: (() -> Unit)?,
                    onSelectAllRequested: (() -> Unit)?
                ) {
                    currentToolbar.showMenu(
                        rect = rect,
                        onPasteRequested = onPasteRequested,
                        onSelectAllRequested = onSelectAllRequested,
                        onCopyRequested = null,
                        onCutRequested = null
                    )
                }
            }
        }
    CompositionLocalProvider(LocalTextToolbar provides copyDisabledToolbar) {
        Box(
            modifier =
                Modifier.onPreviewKeyEvent { keyEvent ->
                    // BasicTextField uses this static mapping
                    val command = platformDefaultKeyMapping.map(keyEvent)
                    // do not propagate copy and cut operations
                    command == KeyCommand.COPY || command == KeyCommand.CUT
                }
        ) {
            content()
        }
    }
}

@Deprecated(
    message = "Please use the overload that takes in readOnly parameter.",
    level = DeprecationLevel.HIDDEN
)
@Suppress("ComposableLambdaParameterPosition")
@Composable
fun BasicSecureTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    inputTransformation: InputTransformation? = null,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.SecureTextField,
    onKeyboardAction: KeyboardActionHandler? = null,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorator: TextFieldDecorator? = null,
    // Last parameter must not be a function unless it's intended to be commonly used as a trailing
    // lambda.
    textObfuscationMode: TextObfuscationMode = TextObfuscationMode.RevealLastTyped,
    textObfuscationCharacter: Char = DefaultObfuscationCharacter,
) {
    BasicSecureTextField(
        state = state,
        modifier = modifier,
        enabled = enabled,
        readOnly = false,
        inputTransformation = inputTransformation,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        decorator = decorator,
        textObfuscationMode = textObfuscationMode,
        textObfuscationCharacter = textObfuscationCharacter
    )
}
