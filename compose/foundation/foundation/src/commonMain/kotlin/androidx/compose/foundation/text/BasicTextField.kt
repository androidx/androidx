/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.handwriting.stylusHandwriting
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.internal.CodepointTransformation
import androidx.compose.foundation.text.input.internal.SingleLineCodepointTransformation
import androidx.compose.foundation.text.input.internal.TextFieldCoreModifier
import androidx.compose.foundation.text.input.internal.TextFieldDecoratorModifier
import androidx.compose.foundation.text.input.internal.TextFieldTextLayoutModifier
import androidx.compose.foundation.text.input.internal.TextLayoutState
import androidx.compose.foundation.text.input.internal.TransformedTextFieldState
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState.InputType
import androidx.compose.foundation.text.selection.SelectionHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

private object BasicTextFieldDefaults {
    val CursorBrush = SolidColor(Color.Black)
}

/**
 * Basic text composable that provides an interactive box that accepts text input through software
 * or hardware keyboard, but provides no decorations like hint or placeholder.
 *
 * All the editing state of this composable is hoisted through [state]. Whenever the contents of
 * this composable change via user input or semantics, [TextFieldState.text] gets updated.
 * Similarly, all the programmatic updates made to [state] also reflect on this composable.
 *
 * If you want to add decorations to your text field, such as icon or similar, and increase the hit
 * target area, use the decorator.
 *
 * In order to filter (e.g. only allow digits, limit the number of characters), or change (e.g.
 * convert every character to uppercase) the input received from the user, use an
 * [InputTransformation].
 *
 * Limiting the height of the [BasicTextField] in terms of line count and choosing a scroll
 * direction can be achieved by using [TextFieldLineLimits].
 *
 * Scroll state of the composable is also hoisted to enable observation and manipulation of the
 * scroll behavior by the developer, e.g. bringing a searched keyword into view by scrolling to its
 * position without focusing, or changing selection.
 *
 * It's also possible to internally wrap around an existing TextFieldState and expose a more
 * lightweight state hoisting mechanism through a value that dictates the content of the TextField
 * and an onValueChange callback that communicates the changes to this value.
 *
 * @param state [TextFieldState] object that holds the internal editing state of [BasicTextField].
 * @param modifier optional [Modifier] for this text field.
 * @param enabled controls the enabled state of the [BasicTextField]. When `false`, the text field
 *   will be neither editable nor focusable, the input of the text field will not be selectable.
 * @param readOnly controls the editable state of the [BasicTextField]. When `true`, the text field
 *   can not be modified, however, a user can focus it and copy text from it. Read-only text fields
 *   are usually used to display pre-filled forms that user can not edit.
 * @param inputTransformation Optional [InputTransformation] that will be used to transform changes
 *   to the [TextFieldState] made by the user. The transformation will be applied to changes made by
 *   hardware and software keyboard events, pasting or dropping text, accessibility services, and
 *   tests. The transformation will _not_ be applied when changing the [state] programmatically, or
 *   when the transformation is changed. If the transformation is changed on an existing text field,
 *   it will be applied to the next user edit. the transformation will not immediately affect the
 *   current [state].
 * @param textStyle Typographic and graphic style configuration for text content that's displayed in
 *   the editor.
 * @param keyboardOptions Software keyboard options that contain configurations such as
 *   [KeyboardType] and [ImeAction].
 * @param onKeyboardAction Called when the user presses the action button in the input method editor
 *   (IME), or by pressing the enter key on a hardware keyboard. By default this parameter is null,
 *   and would execute the default behavior for a received IME Action e.g., [ImeAction.Done] would
 *   close the keyboard, [ImeAction.Next] would switch the focus to the next focusable item on the
 *   screen.
 * @param lineLimits Whether the text field should be [SingleLine], scroll horizontally, and ignore
 *   newlines; or [MultiLine] and grow and scroll vertically. If [SingleLine] is passed, all newline
 *   characters ('\n') within the text will be replaced with regular whitespace (' '), ensuring that
 *   the contents of the text field are presented in a single line.
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
 *   provided, then no cursor will be drawn.
 * @param outputTransformation An [OutputTransformation] that transforms how the contents of the
 *   text field are presented.
 * @param decorator Allows to add decorations around text field, such as icon, placeholder, helper
 *   messages or similar, and automatically increase the hit target area of the text field.
 * @param scrollState Scroll state that manages either horizontal or vertical scroll of TextField.
 *   If [lineLimits] is [SingleLine], this text field is treated as single line with horizontal
 *   scroll behavior. In other cases the text field becomes vertically scrollable.
 * @sample androidx.compose.foundation.samples.BasicTextFieldDecoratorSample
 * @sample androidx.compose.foundation.samples.BasicTextFieldCustomInputTransformationSample
 * @sample androidx.compose.foundation.samples.BasicTextFieldWithValueOnValueChangeSample
 */
// This takes a composable lambda, but it is not primarily a container.
@Suppress("ComposableLambdaParameterPosition")
@Composable
fun BasicTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    inputTransformation: InputTransformation? = null,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = BasicTextFieldDefaults.CursorBrush,
    outputTransformation: OutputTransformation? = null,
    decorator: TextFieldDecorator? = null,
    scrollState: ScrollState = rememberScrollState(),
    // Last parameter must not be a function unless it's intended to be commonly used as a trailing
    // lambda.
) {
    BasicTextField(
        state = state,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        inputTransformation = inputTransformation,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        lineLimits = lineLimits,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        codepointTransformation = null,
        outputTransformation = outputTransformation,
        decorator = decorator,
        scrollState = scrollState,
    )
}

/**
 * Internal core text field that accepts a [CodepointTransformation].
 *
 * @param codepointTransformation Visual transformation interface that provides a 1-to-1 mapping of
 *   codepoints.
 */
// This takes a composable lambda, but it is not primarily a container.
@Suppress("ComposableLambdaParameterPosition")
@Composable
internal fun BasicTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    inputTransformation: InputTransformation? = null,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = BasicTextFieldDefaults.CursorBrush,
    codepointTransformation: CodepointTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    decorator: TextFieldDecorator? = null,
    scrollState: ScrollState = rememberScrollState(),
    isPassword: Boolean = false,
    // Last parameter must not be a function unless it's intended to be commonly used as a trailing
    // lambda.
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val windowInfo = LocalWindowInfo.current
    val singleLine = lineLimits == SingleLine
    // We're using this to communicate focus state to cursor for now.
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val orientation = if (singleLine) Orientation.Horizontal else Orientation.Vertical
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val isDragHovered = interactionSource.collectIsHoveredAsState().value
    val isWindowFocused = windowInfo.isWindowFocused
    val stylusHandwritingTrigger = remember {
        MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_LATEST)
    }

    val transformedState =
        remember(state, codepointTransformation, outputTransformation) {
            // First prefer provided codepointTransformation if not null, e.g. BasicSecureTextField
            // would send PasswordTransformation. Second, apply a SingleLineCodepointTransformation
            // if
            // text field is configured to be single line. Else, don't apply any visual
            // transformation.
            val appliedCodepointTransformation =
                codepointTransformation ?: SingleLineCodepointTransformation.takeIf { singleLine }
            TransformedTextFieldState(
                textFieldState = state,
                inputTransformation = inputTransformation,
                codepointTransformation = appliedCodepointTransformation,
                outputTransformation = outputTransformation
            )
        }

    // Invalidate textLayoutState if TextFieldState itself has changed, since TextLayoutState
    // would be carrying an invalid TextFieldState in its nonMeasureInputs.
    val textLayoutState = remember(transformedState) { TextLayoutState() }

    // InputTransformation.keyboardOptions might be backed by Snapshot state.
    // Read in a restartable composable scope to make sure the resolved value is always up-to-date.
    val resolvedKeyboardOptions =
        keyboardOptions.fillUnspecifiedValuesWith(inputTransformation?.keyboardOptions)

    val textFieldSelectionState =
        remember(transformedState) {
            TextFieldSelectionState(
                textFieldState = transformedState,
                textLayoutState = textLayoutState,
                density = density,
                enabled = enabled,
                readOnly = readOnly,
                isFocused = isFocused && isWindowFocused,
                isPassword = isPassword,
            )
        }
    val currentHapticFeedback = LocalHapticFeedback.current
    val currentClipboardManager = LocalClipboardManager.current
    val currentTextToolbar = LocalTextToolbar.current
    SideEffect {
        // These properties are not backed by snapshot state, so they can't be updated directly in
        // composition.
        transformedState.update(inputTransformation)

        textFieldSelectionState.update(
            hapticFeedBack = currentHapticFeedback,
            clipboardManager = currentClipboardManager,
            textToolbar = currentTextToolbar,
            density = density,
            enabled = enabled,
            readOnly = readOnly,
            isPassword = isPassword,
        )
    }

    DisposableEffect(textFieldSelectionState) { onDispose { textFieldSelectionState.dispose() } }

    val decorationModifiers =
        modifier
            .then(
                // semantics + some focus + input session + touch to focus
                TextFieldDecoratorModifier(
                    textFieldState = transformedState,
                    textLayoutState = textLayoutState,
                    textFieldSelectionState = textFieldSelectionState,
                    filter = inputTransformation,
                    enabled = enabled,
                    readOnly = readOnly,
                    keyboardOptions = resolvedKeyboardOptions,
                    keyboardActionHandler = onKeyboardAction,
                    singleLine = singleLine,
                    interactionSource = interactionSource,
                    isPassword = isPassword,
                    stylusHandwritingTrigger = stylusHandwritingTrigger
                )
            )
            .stylusHandwriting(enabled) {
                // If this is a password field, we can't trigger handwriting.
                // The expected behavior is 1) request focus 2) show software keyboard.
                // Note: TextField will show software keyboard automatically when it
                // gain focus. 3) show a toast message telling that handwriting is not
                // supported for password fields. TODO(b/335294152)
                if (
                    !isPassword &&
                        keyboardOptions.keyboardType != KeyboardType.Password &&
                        keyboardOptions.keyboardType != KeyboardType.NumberPassword
                ) {
                    // Send the handwriting start signal to platform.
                    // The editor should send the signal when it is focused or is about
                    // to gain focus, Here are more details:
                    //   1) if the editor already has an active input session, the
                    //   platform handwriting service should already listen to this flow
                    //   and it'll start handwriting right away.
                    //
                    //   2) if the editor is not focused, but it'll be focused and
                    //   create a new input session, one handwriting signal will be
                    //   replayed when the platform collect this flow. And the platform
                    //   should trigger handwriting accordingly.
                    stylusHandwritingTrigger.tryEmit(Unit)
                }
            }
            .focusable(interactionSource = interactionSource, enabled = enabled)
            .scrollable(
                state = scrollState,
                orientation = orientation,
                // Disable scrolling when textField is disabled or another dragging gesture is
                // taking
                // place
                enabled =
                    enabled && textFieldSelectionState.directDragGestureInitiator == InputType.None,
                reverseDirection =
                    ScrollableDefaults.reverseDirection(
                        layoutDirection = layoutDirection,
                        orientation = orientation,
                        reverseScrolling = false
                    ),
                interactionSource = interactionSource,
            )
            .pointerHoverIcon(textPointerIcon)

    Box(decorationModifiers, propagateMinConstraints = true) {
        ContextMenuArea(textFieldSelectionState, enabled) {
            val nonNullDecorator = decorator ?: DefaultTextFieldDecorator
            nonNullDecorator.Decoration {
                val minLines: Int
                val maxLines: Int
                if (lineLimits is MultiLine) {
                    minLines = lineLimits.minHeightInLines
                    maxLines = lineLimits.maxHeightInLines
                } else {
                    minLines = 1
                    maxLines = 1
                }

                Box(
                    propagateMinConstraints = true,
                    modifier =
                        Modifier.heightIn(min = textLayoutState.minHeightForSingleLineField)
                            .heightInLines(
                                textStyle = textStyle,
                                minLines = minLines,
                                maxLines = maxLines
                            )
                            .textFieldMinSize(textStyle)
                            .clipToBounds()
                            .then(
                                TextFieldCoreModifier(
                                    isFocused = isFocused && isWindowFocused,
                                    isDragHovered = isDragHovered,
                                    textLayoutState = textLayoutState,
                                    textFieldState = transformedState,
                                    textFieldSelectionState = textFieldSelectionState,
                                    cursorBrush = cursorBrush,
                                    writeable = enabled && !readOnly,
                                    scrollState = scrollState,
                                    orientation = orientation
                                )
                            )
                ) {
                    Box(
                        modifier =
                            Modifier.bringIntoViewRequester(textLayoutState.bringIntoViewRequester)
                                .then(
                                    TextFieldTextLayoutModifier(
                                        textLayoutState = textLayoutState,
                                        textFieldState = transformedState,
                                        textStyle = textStyle,
                                        singleLine = singleLine,
                                        onTextLayout = onTextLayout,
                                        keyboardOptions = resolvedKeyboardOptions,
                                    )
                                )
                    )

                    if (
                        enabled &&
                            isFocused &&
                            isWindowFocused &&
                            textFieldSelectionState.isInTouchMode
                    ) {
                        TextFieldSelectionHandles(selectionState = textFieldSelectionState)
                        if (!readOnly) {
                            TextFieldCursorHandle(selectionState = textFieldSelectionState)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TextFieldCursorHandle(selectionState: TextFieldSelectionState) {
    // Does not recompose if only position of the handle changes.
    val cursorHandleState by
        remember(selectionState) {
            derivedStateOf { selectionState.getCursorHandleState(includePosition = false) }
        }
    if (cursorHandleState.visible) {
        CursorHandle(
            offsetProvider = {
                selectionState.getCursorHandleState(includePosition = true).position
            },
            modifier =
                Modifier.pointerInput(selectionState) {
                    with(selectionState) { cursorHandleGestures() }
                },
            minTouchTargetSize = MinTouchTargetSizeForHandles,
        )
    }
}

@Composable
internal fun TextFieldSelectionHandles(selectionState: TextFieldSelectionState) {
    // Does not recompose if only position of the handle changes.
    val startHandleState by remember {
        derivedStateOf {
            selectionState.getSelectionHandleState(isStartHandle = true, includePosition = false)
        }
    }
    if (startHandleState.visible) {
        SelectionHandle(
            offsetProvider = {
                selectionState
                    .getSelectionHandleState(isStartHandle = true, includePosition = true)
                    .position
            },
            isStartHandle = true,
            direction = startHandleState.direction,
            handlesCrossed = startHandleState.handlesCrossed,
            modifier =
                Modifier.pointerInput(selectionState) {
                    with(selectionState) { selectionHandleGestures(true) }
                },
            minTouchTargetSize = MinTouchTargetSizeForHandles,
        )
    }

    // Does not recompose if only position of the handle changes.
    val endHandleState by remember {
        derivedStateOf {
            selectionState.getSelectionHandleState(isStartHandle = false, includePosition = false)
        }
    }
    if (endHandleState.visible) {
        SelectionHandle(
            offsetProvider = {
                selectionState
                    .getSelectionHandleState(isStartHandle = false, includePosition = true)
                    .position
            },
            isStartHandle = false,
            direction = endHandleState.direction,
            handlesCrossed = endHandleState.handlesCrossed,
            modifier =
                Modifier.pointerInput(selectionState) {
                    with(selectionState) { selectionHandleGestures(false) }
                },
            minTouchTargetSize = MinTouchTargetSizeForHandles,
        )
    }
}

private val DefaultTextFieldDecorator = TextFieldDecorator { it() }

/**
 * Defines a minimum touch target area size for Selection and Cursor handles.
 *
 * Although BasicTextField is not part of Material spec, this accessibility feature is important
 * enough to be included at foundation layer, and also TextField cannot change selection handles
 * provided by BasicTextField to somehow achieve this accessibility requirement.
 *
 * This value is adopted from Android platform's TextView implementation.
 */
private val MinTouchTargetSizeForHandles = DpSize(40.dp, 40.dp)

/**
 * Basic composable that enables users to edit text via hardware or software keyboard, but provides
 * no decorations like hint or placeholder.
 *
 * Whenever the user edits the text, [onValueChange] is called with the most up to date state
 * represented by [String] with which developer is expected to update their state.
 *
 * Unlike [TextFieldValue] overload, this composable does not let the developer control selection,
 * cursor and text composition information. Please check [TextFieldValue] and corresponding
 * [BasicTextField] overload for more information.
 *
 * It is crucial that the value provided to the [onValueChange] is fed back into [BasicTextField] in
 * order to actually display and continue to edit that text in the field. The value you feed back
 * into the field may be different than the one provided to the [onValueChange] callback, however
 * the following caveats apply:
 * - The new value must be provided to [BasicTextField] immediately (i.e. by the next frame), or the
 *   text field may appear to glitch, e.g. the cursor may jump around. For more information about
 *   this requirement, see
 *   [this article](https://developer.android.com/jetpack/compose/text/user-input#state-practices).
 * - The value fed back into the field may be different from the one passed to [onValueChange],
 *   although this may result in the input connection being restarted, which can make the keyboard
 *   flicker for the user. This is acceptable when you're using the callback to, for example, filter
 *   out certain types of input, but should probably not be done on every update when entering
 *   freeform text.
 *
 * This composable provides basic text editing functionality, however does not include any
 * decorations such as borders, hints/placeholder. A design system based implementation such as
 * Material Design Filled text field is typically what is needed to cover most of the needs. This
 * composable is designed to be used when a custom implementation for different design system is
 * needed.
 *
 * Example usage:
 *
 * @sample androidx.compose.foundation.samples.BasicTextFieldWithStringSample
 *
 * For example, if you need to include a placeholder in your TextField, you can write a composable
 * using the decoration box like this:
 *
 * @sample androidx.compose.foundation.samples.PlaceholderBasicTextFieldSample
 *
 * If you want to add decorations to your text field, such as icon or similar, and increase the hit
 * target area, use the decoration box:
 *
 * @sample androidx.compose.foundation.samples.TextFieldWithIconSample
 *
 * In order to create formatted text field, for example for entering a phone number or a social
 * security number, use a [visualTransformation] parameter. Below is the example of the text field
 * for entering a credit card number:
 *
 * @sample androidx.compose.foundation.samples.CreditCardSample
 *
 * Note: This overload does not support [KeyboardOptions.showKeyboardOnFocus].
 *
 * @param value the input [String] text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 *   updated text comes as a parameter of the callback
 * @param modifier optional [Modifier] for this text field.
 * @param enabled controls the enabled state of the [BasicTextField]. When `false`, the text field
 *   will be neither editable nor focusable, the input of the text field will not be selectable
 * @param readOnly controls the editable state of the [BasicTextField]. When `true`, the text field
 *   can not be modified, however, a user can focus it and copy text from it. Read-only text fields
 *   are usually used to display pre-filled forms that user can not edit
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is
 *   called. Note that this IME action may be different from what you specified in
 *   [KeyboardOptions.imeAction].
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling text
 *   field instead of wrapping onto multiple lines. The keyboard will be informed to not show the
 *   return key as the [ImeAction]. [maxLines] and [minLines] are ignored as both are automatically
 *   set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param visualTransformation The visual transformation filter for changing the visual
 *   representation of the input. By default no visual transformation is applied.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 *   [TextLayoutResult] object that callback provides contains paragraph information, size of the
 *   text, baselines and other details. The callback can be used to add additional decoration or
 *   functionality to the text. For example, to draw a cursor or selection around the text.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param cursorBrush [Brush] to paint cursor with. If [SolidColor] with [Color.Unspecified]
 *   provided, there will be no cursor drawn
 * @param decorationBox Composable lambda that allows to add decorations around text field, such as
 *   icon, placeholder, helper messages or similar, and automatically increase the hit target area
 *   of the text field. To allow you to control the placement of the inner text field relative to
 *   your decorations, the text field implementation will pass in a framework-controlled composable
 *   parameter "innerTextField" to the decorationBox lambda you provide. You must call
 *   innerTextField exactly once.
 */
@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    // Holds the latest internal TextFieldValue state. We need to keep it to have the correct value
    // of the composition.
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }
    // Holds the latest TextFieldValue that BasicTextField was recomposed with. We couldn't simply
    // pass `TextFieldValue(text = value)` to the CoreTextField because we need to preserve the
    // composition.
    val textFieldValue = textFieldValueState.copy(text = value)

    SideEffect {
        if (
            textFieldValue.selection != textFieldValueState.selection ||
                textFieldValue.composition != textFieldValueState.composition
        ) {
            textFieldValueState = textFieldValue
        }
    }
    // Last String value that either text field was recomposed with or updated in the onValueChange
    // callback. We keep track of it to prevent calling onValueChange(String) for same String when
    // CoreTextField's onValueChange is called multiple times without recomposition in between.
    var lastTextValue by remember(value) { mutableStateOf(value) }

    CoreTextField(
        value = textFieldValue,
        onValueChange = { newTextFieldValueState ->
            textFieldValueState = newTextFieldValueState

            val stringChangedSinceLastInvocation = lastTextValue != newTextFieldValueState.text
            lastTextValue = newTextFieldValueState.text

            if (stringChangedSinceLastInvocation) {
                onValueChange(newTextFieldValueState.text)
            }
        },
        modifier = modifier,
        textStyle = textStyle,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        imeOptions = keyboardOptions.toImeOptions(singleLine = singleLine),
        keyboardActions = keyboardActions,
        softWrap = !singleLine,
        minLines = if (singleLine) 1 else minLines,
        maxLines = if (singleLine) 1 else maxLines,
        decorationBox = decorationBox,
        enabled = enabled,
        readOnly = readOnly
    )
}

/**
 * Basic composable that enables users to edit text via hardware or software keyboard, but provides
 * no decorations like hint or placeholder.
 *
 * Whenever the user edits the text, [onValueChange] is called with the most up to date state
 * represented by [TextFieldValue]. [TextFieldValue] contains the text entered by user, as well as
 * selection, cursor and text composition information. Please check [TextFieldValue] for the
 * description of its contents.
 *
 * It is crucial that the value provided to the [onValueChange] is fed back into [BasicTextField] in
 * order to actually display and continue to edit that text in the field. The value you feed back
 * into the field may be different than the one provided to the [onValueChange] callback, however
 * the following caveats apply:
 * - The new value must be provided to [BasicTextField] immediately (i.e. by the next frame), or the
 *   text field may appear to glitch, e.g. the cursor may jump around. For more information about
 *   this requirement, see
 *   [this article](https://developer.android.com/jetpack/compose/text/user-input#state-practices).
 * - The value fed back into the field may be different from the one passed to [onValueChange],
 *   although this may result in the input connection being restarted, which can make the keyboard
 *   flicker for the user. This is acceptable when you're using the callback to, for example, filter
 *   out certain types of input, but should probably not be done on every update when entering
 *   freeform text.
 *
 * This composable provides basic text editing functionality, however does not include any
 * decorations such as borders, hints/placeholder. A design system based implementation such as
 * Material Design Filled text field is typically what is needed to cover most of the needs. This
 * composable is designed to be used when a custom implementation for different design system is
 * needed.
 *
 * Example usage:
 *
 * @sample androidx.compose.foundation.samples.BasicTextFieldSample
 *
 * For example, if you need to include a placeholder in your TextField, you can write a composable
 * using the decoration box like this:
 *
 * @sample androidx.compose.foundation.samples.PlaceholderBasicTextFieldSample
 *
 * If you want to add decorations to your text field, such as icon or similar, and increase the hit
 * target area, use the decoration box:
 *
 * @sample androidx.compose.foundation.samples.TextFieldWithIconSample
 *
 * Note: This overload does not support [KeyboardOptions.showKeyboardOnFocus].
 *
 * @param value The [androidx.compose.ui.text.input.TextFieldValue] to be shown in the
 *   [BasicTextField].
 * @param onValueChange Called when the input service updates the values in [TextFieldValue].
 * @param modifier optional [Modifier] for this text field.
 * @param enabled controls the enabled state of the [BasicTextField]. When `false`, the text field
 *   will be neither editable nor focusable, the input of the text field will not be selectable
 * @param readOnly controls the editable state of the [BasicTextField]. When `true`, the text field
 *   can not be modified, however, a user can focus it and copy text from it. Read-only text fields
 *   are usually used to display pre-filled forms that user can not edit
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is
 *   called. Note that this IME action may be different from what you specified in
 *   [KeyboardOptions.imeAction].
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling text
 *   field instead of wrapping onto multiple lines. The keyboard will be informed to not show the
 *   return key as the [ImeAction]. [maxLines] and [minLines] are ignored as both are automatically
 *   set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param visualTransformation The visual transformation filter for changing the visual
 *   representation of the input. By default no visual transformation is applied.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 *   [TextLayoutResult] object that callback provides contains paragraph information, size of the
 *   text, baselines and other details. The callback can be used to add additional decoration or
 *   functionality to the text. For example, to draw a cursor or selection around the text.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param cursorBrush [Brush] to paint cursor with. If [SolidColor] with [Color.Unspecified]
 *   provided, there will be no cursor drawn
 * @param decorationBox Composable lambda that allows to add decorations around text field, such as
 *   icon, placeholder, helper messages or similar, and automatically increase the hit target area
 *   of the text field. To allow you to control the placement of the inner text field relative to
 *   your decorations, the text field implementation will pass in a framework-controlled composable
 *   parameter "innerTextField" to the decorationBox lambda you provide. You must call
 *   innerTextField exactly once.
 */
@Composable
fun BasicTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    CoreTextField(
        value = value,
        onValueChange = {
            if (value != it) {
                onValueChange(it)
            }
        },
        modifier = modifier,
        textStyle = textStyle,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        imeOptions = keyboardOptions.toImeOptions(singleLine = singleLine),
        keyboardActions = keyboardActions,
        softWrap = !singleLine,
        minLines = if (singleLine) 1 else minLines,
        maxLines = if (singleLine) 1 else maxLines,
        decorationBox = decorationBox,
        enabled = enabled,
        readOnly = readOnly
    )
}

@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        minLines = 1,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        decorationBox = decorationBox
    )
}

@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun BasicTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        minLines = 1,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        decorationBox = decorationBox
    )
}
