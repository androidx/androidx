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

package androidx.compose.foundation.text2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text2.input.CodepointTransformation
import androidx.compose.foundation.text2.input.TextEditFilter
import androidx.compose.foundation.text2.input.TextFieldBufferWithSelection
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.TextFieldLineLimits
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.TextObfuscationMode
import androidx.compose.foundation.text2.input.mask
import androidx.compose.foundation.text2.input.then
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

/**
 * BasicSecureTextField is a new text input component that is still in heavy development.
 * We strongly advise against using it in production as its API and implementation are currently
 * unstable. Many essential features such as selection, cursor, gestures, etc. may not work
 * correctly or may not even exist yet.
 *
 * BasicSecureTextField is specifically designed for password entry fields and is a preconfigured
 * alternative to BasicTextField2. It only supports a single line of content and comes with default
 * settings for KeyboardOptions, filter, and codepointTransformation that are appropriate for
 * entering secure content. Additionally, some context menu actions like cut, copy, and drag are
 * disabled for added security.
 *
 * @param state [TextFieldState] object that holds the internal state of a [BasicTextField2].
 * @param modifier optional [Modifier] for this text field.
 * @param enabled controls the enabled state of the [BasicTextField2]. When `false`, the text
 * field will be neither editable nor focusable, the input of the text field will not be selectable.
 * @param onSubmit Called when the user submits a form either by pressing the action button in the
 * input method editor (IME), or by pressing the enter key on a hardware keyboard. If the user
 * submits the form by pressing the action button in the IME, the provided IME action is passed to
 * the function. If the user submits the form by pressing the enter key on a hardware keyboard,
 * the defined [imeAction] parameter is passed to the function. Return true to indicate that the
 * action has been handled completely, which will skip the default behavior, such as hiding the
 * keyboard for the [ImeAction.Done] action.
 * @param imeAction The IME action. This IME action is honored by keyboard and may show specific
 * icons on the keyboard.
 * @param textObfuscationMode Determines the method used to obscure the input text.
 * @param keyboardType The keyboard type to be used in this text field. It is set to
 * [KeyboardType.Password] by default. Use [KeyboardType.NumberPassword] for numerical password
 * fields.
 * @param filter Optional [TextEditFilter] that will be used to filter changes to the
 * [TextFieldState] made by the user. The filter will be applied to changes made by hardware and
 * software keyboard events, pasting or dropping text, accessibility services, and tests. The filter
 * will _not_ be applied when changing the [state] programmatically, or when the filter is changed.
 * If the filter is changed on an existing text field, it will be applied to the next user edit.
 * the filter will not immediately affect the current [state].
 * @param textStyle Style configuration for text content that's displayed in the editor.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this TextField. You can create and pass in your own remembered [MutableInteractionSource]
 * if you want to observe [Interaction]s and customize the appearance / behavior of this TextField
 * for different [Interaction]s.
 * @param cursorBrush [Brush] to paint cursor with. If [SolidColor] with [Color.Unspecified]
 * provided, there will be no cursor drawn
 * @param scrollState Used to manage the horizontal scroll when the input content exceeds the
 * bounds of the text field. It controls the state of the scroll for the text field.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 * [TextLayoutResult] object that callback provides contains paragraph information, size of the
 * text, baselines and other details. The callback can be used to add additional decoration or
 * functionality to the text. For example, to draw a cursor or selection around the text. [Density]
 * scope is the one that was used while creating the given text layout.
 * @param decorationBox Composable lambda that allows to add decorations around text field, such
 * as icon, placeholder, helper messages or similar, and automatically increase the hit target area
 * of the text field. To allow you to control the placement of the inner text field relative to your
 * decorations, the text field implementation will pass in a framework-controlled composable
 * parameter "innerTextField" to the decorationBox lambda you provide. You must call
 * innerTextField exactly once.
 */
@ExperimentalFoundationApi
@Composable
fun BasicSecureTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    onSubmit: ((ImeAction) -> Boolean)? = null,
    imeAction: ImeAction = ImeAction.Default,
    textObfuscationMode: TextObfuscationMode = TextObfuscationMode.RevealLastTyped,
    keyboardType: KeyboardType = KeyboardType.Password,
    enabled: Boolean = true,
    filter: TextEditFilter? = null,
    textStyle: TextStyle = TextStyle.Default,
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    scrollState: ScrollState = rememberScrollState(),
    onTextLayout: Density.(TextLayoutResult) -> Unit = {},
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    val coroutineScope = rememberCoroutineScope()
    val secureTextFieldController = remember(coroutineScope) {
        SecureTextFieldController(coroutineScope)
    }

    // revealing last typed character depends on two conditions;
    // 1 - Requested Obfuscation method
    // 2 - if the system allows it
    val revealLastTypedEnabled = textObfuscationMode == TextObfuscationMode.RevealLastTyped

    // while toggling between obfuscation methods if the revealing gets disabled, reset the reveal.
    if (!revealLastTypedEnabled) {
        secureTextFieldController.passwordRevealFilter.hide()
    }

    val codepointTransformation = when {
        revealLastTypedEnabled -> {
            secureTextFieldController.codepointTransformation
        }

        textObfuscationMode == TextObfuscationMode.Hidden -> {
            CodepointTransformation.mask('\u2022')
        }

        else -> {
            CodepointTransformation.None
        }
    }

    val secureTextFieldModifier = modifier
        .semantics(mergeDescendants = true) { password() }
        .then(
            if (revealLastTypedEnabled) {
                secureTextFieldController.focusChangeModifier
            } else {
                Modifier
            }
        )

    BasicTextField2(
        state = state,
        modifier = secureTextFieldModifier,
        enabled = enabled,
        readOnly = false,
        filter = if (revealLastTypedEnabled) {
            filter?.then(secureTextFieldController.passwordRevealFilter)
                ?: secureTextFieldController.passwordRevealFilter
        } else filter,
        textStyle = textStyle,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        lineLimits = TextFieldLineLimits.SingleLine,
        scrollState = scrollState,
        keyboardOptions = KeyboardOptions(
            autoCorrect = false,
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = onSubmit?.let { KeyboardActions(onSubmit = it) }
            ?: KeyboardActions.Default,
        onTextLayout = onTextLayout,
        codepointTransformation = codepointTransformation,
        decorationBox = decorationBox,
    )
}

@OptIn(ExperimentalFoundationApi::class)
internal class SecureTextFieldController(
    coroutineScope: CoroutineScope
) {
    /**
     * A special [TextEditFilter] that tracks changes to the content to identify the last typed
     * character to reveal. `scheduleHide` lambda is delegated to a member function to be able to
     * use [passwordRevealFilter] instance.
     */
    val passwordRevealFilter = PasswordRevealFilter(::scheduleHide)

    /**
     * Pass to [BasicTextField2] for obscuring text input.
     */
    val codepointTransformation = CodepointTransformation { codepointIndex, codepoint ->
        if (codepointIndex == passwordRevealFilter.revealCodepointIndex) {
            // reveal the last typed character by not obscuring it
            codepoint
        } else {
            0x2022
        }
    }

    val focusChangeModifier = Modifier.onFocusChanged {
        if (!it.isFocused) passwordRevealFilter.hide()
    }

    private val resetTimerSignal = Channel<Unit>(Channel.UNLIMITED)

    init {
        // start a coroutine that listens for scheduled hide events.
        coroutineScope.launch {
            resetTimerSignal.consumeAsFlow()
                .collectLatest {
                    delay(LAST_TYPED_CHARACTER_REVEAL_DURATION_MILLIS)
                    passwordRevealFilter.hide()
                }
        }
    }

    private fun scheduleHide() {
        // signal the listener that a new hide call is scheduled.
        val result = resetTimerSignal.trySend(Unit)
        if (!result.isSuccess) {
            passwordRevealFilter.hide()
        }
    }
}

/**
 * Special filter that tracks the changes in a TextField to identify the last typed character and
 * mark it for reveal in password fields.
 *
 * @param scheduleHide A lambda that schedules a [hide] call into future after a new character is
 * typed.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class PasswordRevealFilter(
    val scheduleHide: () -> Unit
) : TextEditFilter {
    // TODO: Consider setting this as a tracking annotation in AnnotatedString.
    internal var revealCodepointIndex by mutableStateOf(-1)
        private set

    override fun filter(
        originalValue: TextFieldCharSequence,
        valueWithChanges: TextFieldBufferWithSelection
    ) {
        // We only care about a single character insertion changes
        val singleCharacterInsertion = valueWithChanges.changes.changeCount == 1 &&
            valueWithChanges.changes.getRange(0).length == 1 &&
            valueWithChanges.changes.getOriginalRange(0).length == 0

        // if there is an expanded selection, don't reveal anything
        if (!singleCharacterInsertion || valueWithChanges.hasSelection) {
            revealCodepointIndex = -1
            return
        }

        val insertionPoint = valueWithChanges.changes.getRange(0).min
        if (revealCodepointIndex != insertionPoint) {
            // start the timer for auto hide
            scheduleHide()
            revealCodepointIndex = insertionPoint
        }
    }

    /**
     * Removes any revealed character index. Everything goes back into hiding.
     */
    fun hide() {
        revealCodepointIndex = -1
    }
}

// adopted from PasswordTransformationMethod from Android platform.
private const val LAST_TYPED_CHARACTER_REVEAL_DURATION_MILLIS = 1500L

private fun KeyboardActions(onSubmit: (ImeAction) -> Boolean) = KeyboardActions(
    onDone = { if (!onSubmit(ImeAction.Done)) defaultKeyboardAction(ImeAction.Done) },
    onGo = { if (!onSubmit(ImeAction.Go)) defaultKeyboardAction(ImeAction.Go) },
    onNext = { if (!onSubmit(ImeAction.Next)) defaultKeyboardAction(ImeAction.Next) },
    onPrevious = { if (!onSubmit(ImeAction.Previous)) defaultKeyboardAction(ImeAction.Previous) },
    onSearch = { if (!onSubmit(ImeAction.Search)) defaultKeyboardAction(ImeAction.Search) },
    onSend = { if (!onSubmit(ImeAction.Send)) defaultKeyboardAction(ImeAction.Send) },
)