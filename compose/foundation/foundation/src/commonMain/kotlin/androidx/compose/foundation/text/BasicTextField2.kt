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
import androidx.compose.foundation.text.input.InputTransformation
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
import androidx.compose.foundation.text.input.internal.syncTextFieldState
import androidx.compose.foundation.text.selection.SelectionHandle
import androidx.compose.foundation.text.selection.SelectionHandleAnchor
import androidx.compose.foundation.text.selection.SelectionHandleInfo
import androidx.compose.foundation.text.selection.SelectionHandleInfoKey
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Basic text composable that provides an interactive box that accepts text input through software
 * or hardware keyboard, but provides no decorations like hint or placeholder.
 *
 * Whenever the user edits the text, [onValueChange] is called with the most up to date state
 * represented by [String] with which developer is expected to update their state.
 *
 * While focused and being edited, the caller temporarily loses _direct_ control of the contents of
 * the field through the [value] parameter. If an unexpected [value] is passed in during this time,
 * the contents of the field will _not_ be updated to reflect the value until editing is done. When
 * editing is done (i.e. focus is lost), the field will be updated to the last [value] received. Use
 * a [inputTransformation] to accept or reject changes during editing. For more direct control of
 * the field contents use the [BasicTextField2] overload that accepts a [TextFieldState].
 *
 * Unlike [TextFieldState] overload, this composable does not let the developer control selection,
 * cursor, and observe text composition information. Please check [TextFieldState] and corresponding
 * [BasicTextField2] overload for more information.
 *
 * If you want to add decorations to your text field, such as icon or similar, and increase the
 * hit target area, use the decorator:
 * @sample androidx.compose.foundation.samples.BasicTextField2DecoratorSample
 *
 * In order to filter (e.g. only allow digits, limit the number of characters), or change (e.g.
 * convert every character to uppercase) the input received from the user, use an
 * [InputTransformation].
 * @sample androidx.compose.foundation.samples.BasicTextField2CustomInputTransformationSample
 *
 * Limiting the height of the [BasicTextField2] in terms of line count and choosing a scroll
 * direction can be achieved by using [TextFieldLineLimits].
 *
 * Scroll state of the composable is also hoisted to enable observation and manipulation of the
 * scroll behavior by the developer, e.g. bringing a searched keyword into view by scrolling to its
 * position without focusing, or changing selection.
 *
 * @param value The input [String] text to be shown in the text field.
 * @param onValueChange The callback that is triggered when the user or the system updates the
 * text. The updated text is passed as a parameter of the callback. The value passed to the callback
 * will already have had the [inputTransformation] applied.
 * @param modifier optional [Modifier] for this text field.
 * @param enabled controls the enabled state of the [BasicTextField2]. When `false`, the text
 * field will be neither editable nor focusable, the input of the text field will not be selectable.
 * @param readOnly controls the editable state of the [BasicTextField2]. When `true`, the text
 * field can not be modified, however, a user can focus it and copy text from it. Read-only text
 * fields are usually used to display pre-filled forms that user can not edit.
 * @param inputTransformation Optional [InputTransformation] that will be used to transform changes
 * to the [TextFieldState] made by the user. The transformation will be applied to changes made by
 * hardware and software keyboard events, pasting or dropping text, accessibility services, and
 * tests. The transformation will _not_ be applied when a new [value] is passed in, or when the
 * transformation is changed. If the transformation is changed on an existing text field, it will be
 * applied to the next user edit, it will not immediately affect the current [value].
 * @param textStyle Typographic and graphic style configuration for text content that's displayed
 * in the editor.
 * @param keyboardOptions Software keyboard options that contain configurations such as
 * [KeyboardType] and [ImeAction].
 * @param keyboardActions When the input service emits an IME action, the corresponding callback
 * is called. Note that this IME action may be different from what you specified in
 * [KeyboardOptions.imeAction].
 * @param lineLimits Whether the text field should be [SingleLine], scroll horizontally, and
 * ignore newlines; or [MultiLine] and grow and scroll vertically. If [SingleLine] is passed, all
 * newline characters ('\n') within the text will be replaced with regular whitespace (' '),
 * ensuring that the contents of the text field are presented in a single line.
 * @param onTextLayout Callback that is executed when the text layout becomes queryable. The
 * callback receives a function that returns a [TextLayoutResult] if the layout can be calculated,
 * or null if it cannot. The function reads the layout result from a snapshot state object, and will
 * invalidate its caller when the layout result changes. A [TextLayoutResult] object contains
 * paragraph information, size of the text, baselines and other details. The callback can be used to
 * add additional decoration or functionality to the text. For example, to draw a cursor or
 * selection around the text. [Density] scope is the one that was used while creating the given text
 * layout.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this TextField. You can create and pass in your own remembered [MutableInteractionSource]
 * if you want to observe [Interaction]s and customize the appearance / behavior of this TextField
 * for different [Interaction]s.
 * @param cursorBrush [Brush] to paint cursor with. If [SolidColor] with [Color.Unspecified]
 * provided, then no cursor will be drawn.
 * @param outputTransformation An [OutputTransformation] that transforms how the contents of the
 * text field are presented.
 * @param decorator Allows to add decorations around text field, such as icon, placeholder, helper
 * messages or similar, and automatically increase the hit target area of the text field.
 * @param scrollState Scroll state that manages either horizontal or vertical scroll of TextField.
 * If [lineLimits] is [SingleLine], this text field is treated as single line with horizontal
 * scroll behavior. In other cases the text field becomes vertically scrollable.
 * @param outputTransformation An [OutputTransformation] that transforms how the contents of the
 * text field are presented.
 */
@ExperimentalFoundationApi
// This takes a composable lambda, but it is not primarily a container.
@Suppress("ComposableLambdaParameterPosition")
@Composable
fun BasicTextField2(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    inputTransformation: InputTransformation? = null,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    outputTransformation: OutputTransformation? = null,
    decorator: TextFieldDecorator? = null,
    scrollState: ScrollState = rememberScrollState(),
    // Last parameter must not be a function unless it's intended to be commonly used as a trailing
    // lambda.
) {
    val state = remember {
        TextFieldState(
            initialText = value,
            // Initialize the cursor to be at the end of the field.
            initialSelectionInChars = TextRange(value.length)
        )
    }

    // This is effectively a rememberUpdatedState, but it combines the updated state (text) with
    // some state that is preserved across updates (selection).
    var valueWithSelection by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        )
    }
    valueWithSelection = valueWithSelection.copy(text = value)

    BasicTextField2(
        state = state,
        modifier = modifier.syncTextFieldState(
            state = state,
            value = valueWithSelection,
            onValueChanged = {
                // Don't fire the callback if only the selection/cursor changed.
                if (it.text != valueWithSelection.text) {
                    onValueChange(it.text)
                }
                valueWithSelection = it
            },
            writeSelectionFromTextFieldValue = false
        ),
        enabled = enabled,
        readOnly = readOnly,
        inputTransformation = inputTransformation,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        lineLimits = lineLimits,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        scrollState = scrollState,
        outputTransformation = outputTransformation,
        decorator = decorator,
    )
}

/**
 * Basic text composable that provides an interactive box that accepts text input through software
 * or hardware keyboard, but provides no decorations like hint or placeholder.
 *
 * All the editing state of this composable is hoisted through [state]. Whenever the contents of
 * this composable change via user input or semantics, [TextFieldState.text] gets updated.
 * Similarly, all the programmatic updates made to [state] also reflect on this composable.
 *
 * If you want to add decorations to your text field, such as icon or similar, and increase the
 * hit target area, use the decorator:
 * @sample androidx.compose.foundation.samples.BasicTextField2DecoratorSample
 *
 * In order to filter (e.g. only allow digits, limit the number of characters), or change (e.g.
 * convert every character to uppercase) the input received from the user, use an
 * [InputTransformation].
 * @sample androidx.compose.foundation.samples.BasicTextField2CustomInputTransformationSample
 *
 * Limiting the height of the [BasicTextField2] in terms of line count and choosing a scroll
 * direction can be achieved by using [TextFieldLineLimits].
 *
 * Scroll state of the composable is also hoisted to enable observation and manipulation of the
 * scroll behavior by the developer, e.g. bringing a searched keyword into view by scrolling to its
 * position without focusing, or changing selection.
 *
 * @param state [TextFieldState] object that holds the internal editing state of [BasicTextField2].
 * @param modifier optional [Modifier] for this text field.
 * @param enabled controls the enabled state of the [BasicTextField2]. When `false`, the text
 * field will be neither editable nor focusable, the input of the text field will not be selectable.
 * @param readOnly controls the editable state of the [BasicTextField2]. When `true`, the text
 * field can not be modified, however, a user can focus it and copy text from it. Read-only text
 * fields are usually used to display pre-filled forms that user can not edit.
 * @param inputTransformation Optional [InputTransformation] that will be used to transform changes
 * to the [TextFieldState] made by the user. The transformation will be applied to changes made by
 * hardware and software keyboard events, pasting or dropping text, accessibility services, and
 * tests. The transformation will _not_ be applied when changing the [state] programmatically, or
 * when the transformation is changed. If the transformation is changed on an existing text field,
 * it will be applied to the next user edit. the transformation will not immediately affect the
 * current [state].
 * @param textStyle Typographic and graphic style configuration for text content that's displayed
 * in the editor.
 * @param keyboardOptions Software keyboard options that contain configurations such as
 * [KeyboardType] and [ImeAction].
 * @param keyboardActions When the input service emits an IME action, the corresponding callback
 * is called. Note that this IME action may be different from what you specified in
 * [KeyboardOptions.imeAction].
 * @param lineLimits Whether the text field should be [SingleLine], scroll horizontally, and
 * ignore newlines; or [MultiLine] and grow and scroll vertically. If [SingleLine] is passed, all
 * newline characters ('\n') within the text will be replaced with regular whitespace (' '),
 * ensuring that the contents of the text field are presented in a single line.
 * @param onTextLayout Callback that is executed when the text layout becomes queryable. The
 * callback receives a function that returns a [TextLayoutResult] if the layout can be calculated,
 * or null if it cannot. The function reads the layout result from a snapshot state object, and will
 * invalidate its caller when the layout result changes. A [TextLayoutResult] object contains
 * paragraph information, size of the text, baselines and other details. The callback can be used to
 * add additional decoration or functionality to the text. For example, to draw a cursor or
 * selection around the text. [Density] scope is the one that was used while creating the given text
 * layout.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this TextField. You can create and pass in your own remembered [MutableInteractionSource]
 * if you want to observe [Interaction]s and customize the appearance / behavior of this TextField
 * for different [Interaction]s.
 * @param cursorBrush [Brush] to paint cursor with. If [SolidColor] with [Color.Unspecified]
 * provided, then no cursor will be drawn.
 * @param outputTransformation An [OutputTransformation] that transforms how the contents of the
 * text field are presented.
 * @param decorator Allows to add decorations around text field, such as icon, placeholder, helper
 * messages or similar, and automatically increase the hit target area of the text field.
 * @param scrollState Scroll state that manages either horizontal or vertical scroll of TextField.
 * If [lineLimits] is [SingleLine], this text field is treated as single line with horizontal
 * scroll behavior. In other cases the text field becomes vertically scrollable.
 */
@ExperimentalFoundationApi
// This takes a composable lambda, but it is not primarily a container.
@Suppress("ComposableLambdaParameterPosition")
@Composable
fun BasicTextField2(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    inputTransformation: InputTransformation? = null,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    outputTransformation: OutputTransformation? = null,
    decorator: TextFieldDecorator? = null,
    scrollState: ScrollState = rememberScrollState(),
    // Last parameter must not be a function unless it's intended to be commonly used as a trailing
    // lambda.
) {
    BasicTextField2(
        state = state,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        inputTransformation = inputTransformation,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
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
 * codepoints.
 */
@OptIn(ExperimentalFoundationApi::class)
// This takes a composable lambda, but it is not primarily a container.
@Suppress("ComposableLambdaParameterPosition")
@Composable
internal fun BasicTextField2(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    inputTransformation: InputTransformation? = null,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    codepointTransformation: CodepointTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    decorator: TextFieldDecorator? = null,
    scrollState: ScrollState = rememberScrollState(),
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

    val transformedState = remember(
        state,
        inputTransformation,
        codepointTransformation,
        outputTransformation
    ) {
        // First prefer provided codepointTransformation if not null, e.g. BasicSecureTextField
        // would send PasswordTransformation. Second, apply a SingleLineCodepointTransformation if
        // text field is configured to be single line. Else, don't apply any visual transformation.
        val appliedCodepointTransformation = codepointTransformation
            ?: SingleLineCodepointTransformation.takeIf { singleLine }
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

    val textFieldSelectionState = remember(transformedState) {
        TextFieldSelectionState(
            textFieldState = transformedState,
            textLayoutState = textLayoutState,
            density = density,
            enabled = enabled,
            readOnly = readOnly,
            isFocused = isFocused && isWindowFocused
        )
    }
    val currentHapticFeedback = LocalHapticFeedback.current
    val currentClipboardManager = LocalClipboardManager.current
    val currentTextToolbar = LocalTextToolbar.current
    SideEffect {
        // These properties are not backed by snapshot state, so they can't be updated directly in
        // composition.
        textFieldSelectionState.update(
            hapticFeedBack = currentHapticFeedback,
            clipboardManager = currentClipboardManager,
            textToolbar = currentTextToolbar,
            density = density,
            enabled = enabled,
            readOnly = readOnly,
        )
    }

    DisposableEffect(textFieldSelectionState) {
        onDispose {
            textFieldSelectionState.dispose()
        }
    }

    val decorationModifiers = modifier
        .then(
            // semantics + some focus + input session + touch to focus
            TextFieldDecoratorModifier(
                textFieldState = transformedState,
                textLayoutState = textLayoutState,
                textFieldSelectionState = textFieldSelectionState,
                filter = inputTransformation,
                enabled = enabled,
                readOnly = readOnly,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                interactionSource = interactionSource
            )
        )
        .focusable(interactionSource = interactionSource, enabled = enabled)
        .scrollable(
            state = scrollState,
            orientation = orientation,
            // Disable scrolling when textField is disabled, there is no where to scroll, and
            // another dragging gesture is taking place
            enabled = enabled &&
                scrollState.maxValue > 0 &&
                textFieldSelectionState.draggingHandle == null,
            reverseDirection = ScrollableDefaults.reverseDirection(
                layoutDirection = layoutDirection,
                orientation = orientation,
                reverseScrolling = false
            ),
            interactionSource = interactionSource,
        )

    Box(decorationModifiers, propagateMinConstraints = true) {
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
                modifier = Modifier
                    .heightIn(min = textLayoutState.minHeightForSingleLineField)
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
                    modifier = Modifier
                        .bringIntoViewRequester(textLayoutState.bringIntoViewRequester)
                        .then(
                            TextFieldTextLayoutModifier(
                                textLayoutState = textLayoutState,
                                textFieldState = transformedState,
                                textStyle = textStyle,
                                singleLine = singleLine,
                                onTextLayout = onTextLayout
                            )
                        )
                )

                if (enabled && isFocused &&
                    isWindowFocused && textFieldSelectionState.isInTouchMode
                ) {
                    TextFieldSelectionHandles(
                        selectionState = textFieldSelectionState
                    )
                    if (!readOnly) {
                        TextFieldCursorHandle(
                            selectionState = textFieldSelectionState
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TextFieldCursorHandle(selectionState: TextFieldSelectionState) {
    val cursorHandleState = selectionState.cursorHandle
    if (cursorHandleState.visible) {
        CursorHandle(
            handlePosition = cursorHandleState.position,
            modifier = Modifier
                .semantics {
                    this[SelectionHandleInfoKey] = SelectionHandleInfo(
                        handle = Handle.Cursor,
                        position = cursorHandleState.position,
                        anchor = SelectionHandleAnchor.Middle,
                        visible = true,
                    )
                }
                .pointerInput(selectionState) {
                    with(selectionState) { cursorHandleGestures() }
                },
            minTouchTargetSize = MinTouchTargetSizeForHandles,
        )
    }
}

@Composable
internal fun TextFieldSelectionHandles(
    selectionState: TextFieldSelectionState
) {
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
            modifier = Modifier.pointerInput(selectionState) {
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
            modifier = Modifier.pointerInput(selectionState) {
                with(selectionState) { selectionHandleGestures(false) }
            },
            minTouchTargetSize = MinTouchTargetSizeForHandles,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
