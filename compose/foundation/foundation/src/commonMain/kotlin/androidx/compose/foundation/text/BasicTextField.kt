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

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ComposeFoundationFlags.isBasicTextFieldSizeOptimizationEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.contextmenu.modifier.ToolbarRequesterImpl
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
import androidx.compose.foundation.text.input.internal.collectIsDragAndDropHoveredAsState
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState.InputType
import androidx.compose.foundation.text.input.internal.selection.TextToolbarHandler
import androidx.compose.foundation.text.input.internal.selection.TextToolbarState
import androidx.compose.foundation.text.input.internal.selection.addBasicTextFieldTextContextMenuComponents
import androidx.compose.foundation.text.input.internal.selection.menuItem
import androidx.compose.foundation.text.selection.SelectedTextType
import androidx.compose.foundation.text.selection.SelectionHandle
import androidx.compose.foundation.text.selection.rememberPlatformSelectionBehaviors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

private object BasicTextFieldDefaults {
    val CursorBrush = SolidColor(Color.Black)
}

/**
 * Interactive text input field without decorations.
 *
 * Hoists editing state through [state].
 *
 * To add decorations (such as borders, placeholders, hints, prefixes, or suffixes) and increase the
 * hit target area, use [decorator].
 *
 * To filter or modify input (e.g., limit characters or restrict input patterns), use
 * [InputTransformation].
 *
 * To transform the visual output (e.g., apply password mask or format phone numbers), use
 * [OutputTransformation].
 *
 * To limit height, use [lineLimits].
 *
 * Hoists scroll state via [scrollState] to observe and manipulate scroll position, such as
 * scrolling a searched keyword into view without focusing.
 *
 * @param state holding the editing state
 * @param modifier for this layout
 * @param enabled controls enabled state. If false, field is not editable, focusable, or selectable
 * @param readOnly controls editable state. If true, field cannot be modified but can be focused and
 *   copied
 * @param inputTransformation to transform user changes. Only applies to user-initiated changes
 *   (e.g., keyboard input, paste, accessibility), not programmatic updates to [state]. Changing the
 *   transformation applies to the next user edit
 * @param textStyle configuration for text content
 * @param keyboardOptions software keyboard options
 * @param onKeyboardAction run when user triggers IME action
 * @param lineLimits limits for line count and scroll behavior. If set to [SingleLine], the text
 *   field scrolls horizontally and newlines ('\n') are replaced with spaces
 * @param onTextLayout callback run when a new text layout is calculated. The [TextLayoutResult]
 *   parameter contains paragraph information, size, baselines, and other details. Use this callback
 *   to add decoration or functionality, such as drawing selection
 * @param interactionSource to observe interactions
 * @param cursorBrush to paint the cursor
 * @param outputTransformation to transform output representation
 * @param decorator to add decorations (such as borders, placeholders, hints, or prefixes/suffixes)
 *   around the text field, and increase the hit target area. The decorator receives an
 *   `innerTextField` composable lambda representing the actual text input area, which must be
 *   called exactly once
 * @param scrollState to manage scroll. If [lineLimits] is [SingleLine], the text field scrolls
 *   horizontally. Otherwise, it scrolls vertically
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
    val singleLine = lineLimits == SingleLine
    // We're using this to communicate focus state to cursor for now.
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val orientation = if (singleLine) Orientation.Horizontal else Orientation.Vertical
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
                outputTransformation = outputTransformation,
            )
        }

    // Invalidate textLayoutState if TextFieldState itself has changed, since TextLayoutState
    // would be carrying an invalid TextFieldState in its nonMeasureInputs.
    val textLayoutState = remember(transformedState) { TextLayoutState() }

    // InputTransformation.keyboardOptions might be backed by Snapshot state.
    // Read in a restartable composable scope to make sure the resolved value is always up-to-date.
    val resolvedKeyboardOptions =
        keyboardOptions.fillUnspecifiedValuesWith(inputTransformation?.keyboardOptions)

    val coroutineScope = rememberCoroutineScope()
    @OptIn(ExperimentalFoundationApi::class)
    val platformSelectionBehaviors =
        if (ComposeFoundationFlags.isSmartSelectionEnabled) {
            val resolvedLocaleList = textStyle.localeList ?: LocaleList.current
            rememberPlatformSelectionBehaviors(SelectedTextType.EditableText, resolvedLocaleList)
        } else {
            null
        }
    val toolbarRequester = remember { ToolbarRequesterImpl() }
    val currentClipboard = LocalClipboard.current
    val textFieldSelectionState =
        remember(transformedState) {
            TextFieldSelectionState(
                textFieldState = transformedState,
                textLayoutState = textLayoutState,
                density = density,
                enabled = enabled,
                readOnly = readOnly,
                isPassword = isPassword,
                toolbarRequester = toolbarRequester,
                coroutineScope = coroutineScope,
                platformSelectionBehaviors = platformSelectionBehaviors,
                clipboard = currentClipboard,
            )
        }
    val currentHapticFeedback = LocalHapticFeedback.current
    val currentTextToolbar = LocalTextToolbar.current

    val textToolbarHandler =
        remember(coroutineScope, currentTextToolbar) {
            object : TextToolbarHandler {
                override suspend fun showTextToolbar(
                    selectionState: TextFieldSelectionState,
                    rect: Rect,
                ) =
                    with(selectionState) {
                        selectionState.updateClipboardEntry()
                        currentTextToolbar.showMenu(
                            rect = rect,
                            onCopyRequested =
                                menuItem(canShowCopyMenuItem(), TextToolbarState.None) {
                                    coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                        copy()
                                    }
                                },
                            onPasteRequested =
                                menuItem(canShowPasteMenuItem(), TextToolbarState.None) {
                                    coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                        paste()
                                    }
                                },
                            onCutRequested =
                                menuItem(canShowCutMenuItem(), TextToolbarState.None) {
                                    coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                        cut()
                                    }
                                },
                            onSelectAllRequested =
                                menuItem(canShowSelectAllMenuItem(), TextToolbarState.Selection) {
                                    selectAll()
                                },
                            onAutofillRequested =
                                menuItem(canShowAutofillMenuItem(), TextToolbarState.None) {
                                    autofill()
                                },
                        )
                    }

                override fun hideTextToolbar() {
                    if (currentTextToolbar.status == TextToolbarStatus.Shown) {
                        currentTextToolbar.hide()
                    }
                }
            }
        }

    SideEffect {
        // These properties are not backed by snapshot state, so they can't be updated directly in
        // composition.
        transformedState.update(inputTransformation)

        textFieldSelectionState.update(
            hapticFeedBack = currentHapticFeedback,
            clipboard = currentClipboard,
            density = density,
            enabled = enabled,
            readOnly = readOnly,
            isPassword = isPassword,
            showTextToolbar = textToolbarHandler,
        )
    }

    DisposableEffect(textFieldSelectionState) { onDispose { textFieldSelectionState.dispose() } }

    val overscrollEffect = rememberTextFieldOverscrollEffect()

    val handwritingEnabled =
        !isPassword &&
            keyboardOptions.keyboardType != KeyboardType.Password &&
            keyboardOptions.keyboardType != KeyboardType.NumberPassword
    val dragInputType = textFieldSelectionState.directDragGestureInitiator
    val decorationModifiers =
        modifier
            .stylusHandwriting(enabled, handwritingEnabled) {
                // If this is a password field, we can't trigger handwriting.
                // The expected behavior is 1) request focus 2) show software keyboard.
                // Note: TextField will show software keyboard automatically when it
                // gain focus. 3) show a toast message telling that handwriting is not
                // supported for password fields. TODO(b/335294152)
                if (handwritingEnabled) {
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
                    stylusHandwritingTrigger = stylusHandwritingTrigger,
                )
            )
            .scrollable(
                state = scrollState,
                orientation = orientation,
                // Disable scrolling when textField is disabled or another dragging gesture is
                // taking place
                enabled = enabled && dragInputType == InputType.None,
                reverseDirection =
                    ScrollableDefaults.reverseDirection(
                        layoutDirection = layoutDirection,
                        orientation = orientation,
                        reverseScrolling = false,
                    ),
                interactionSource = interactionSource,
                overscrollEffect = overscrollEffect,
            )
            .pointerHoverIcon(PointerIcon.Text)
            .addContextMenuComponents(textFieldSelectionState, coroutineScope)
            .textFieldOverlay(state, keyboardOptions, interactionSource)

    Box(decorationModifiers, propagateMinConstraints = true) {
        ContextMenuArea(textFieldSelectionState, enabled) {
            val nonNullDecorator = decorator ?: DefaultTextFieldDecorator
            nonNullDecorator.Decoration {
                val isFocused by interactionSource.collectIsFocusedAsState()
                val isDragHovered by interactionSource.collectIsDragAndDropHoveredAsState()
                val windowInfo = LocalWindowInfo.current
                val isWindowAndTextFieldFocused by
                    remember(interactionSource, windowInfo) {
                        // Using derived state here to avoid recomposing when window focus is
                        // obtained after the initial focus.
                        derivedStateOf { isFocused && windowInfo.isWindowFocused }
                    }

                rememberClipboardEventsHandler(
                    isEnabled = isFocused,
                    onPaste = { textFieldSelectionState.onPasteEvent(it) },
                    onCopy = { textFieldSelectionState.copyWithResult() },
                    onCut = { textFieldSelectionState.cutWithResult() },
                )

                val minLines: Int
                val maxLines: Int
                if (lineLimits is MultiLine) {
                    minLines = lineLimits.minHeightInLines
                    maxLines = lineLimits.maxHeightInLines
                } else {
                    minLines = 1
                    maxLines = 1
                }

                @OptIn(ExperimentalFoundationApi::class)
                val textFieldSize =
                    if (isBasicTextFieldSizeOptimizationEnabled) {
                        Modifier.textFieldSize(
                            textStyle = textStyle,
                            singleLineHeightProvider = textLayoutState,
                            minLines = minLines,
                            maxLines = maxLines,
                            singleLine = singleLine,
                        )
                    } else {
                        Modifier.heightForSingleLineField(textLayoutState)
                            .heightInLines(
                                textStyle = textStyle,
                                minLines = minLines,
                                maxLines = maxLines,
                                softWrap = !singleLine,
                            )
                            .textFieldMinSize(textStyle)
                    }
                Box(
                    propagateMinConstraints = true,
                    modifier =
                        textFieldSize
                            .clipToBounds()
                            .then(
                                TextFieldCoreModifier(
                                    isFocused = isWindowAndTextFieldFocused,
                                    isDragHovered = isDragHovered,
                                    isTouchDragInProgress = dragInputType == InputType.Touch,
                                    textLayoutState = textLayoutState,
                                    textFieldState = transformedState,
                                    textFieldSelectionState = textFieldSelectionState,
                                    cursorBrush = cursorBrush,
                                    writeable = enabled && !readOnly,
                                    scrollState = scrollState,
                                    orientation = orientation,
                                    toolbarRequester = toolbarRequester,
                                    platformSelectionBehaviors = platformSelectionBehaviors,
                                )
                            ),
                ) {
                    Box(
                        modifier =
                            TextFieldTextLayoutModifier(
                                textLayoutState = textLayoutState,
                                textFieldState = transformedState,
                                textStyle = textStyle,
                                singleLine = singleLine,
                                onTextLayout = onTextLayout,
                                keyboardOptions = resolvedKeyboardOptions,
                            )
                    )

                    if (
                        enabled &&
                            isWindowAndTextFieldFocused &&
                            textFieldSelectionState.isDirectTouchInteraction
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

/**
 * A modifier that can be used to determine the location and state of the text field. It is used on
 * multiplatform, where knowledge of the text field's state and location is required in order to
 * support platform-dependent features such as VoiceOver or Autofill (password autofill, one-time
 * codes, etc.).
 */
internal expect fun Modifier.textFieldOverlay(
    state: TextFieldState,
    keyboardOptions: KeyboardOptions,
    interactionSource: InteractionSource,
): Modifier

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.heightForSingleLineField(textLayoutState: TextLayoutState) =
    if (ComposeFoundationFlags.isBasicTextFieldMinSizeOptimizationEnabled) {
        layout { measurable, constraints ->
            val height = textLayoutState.heightForSingleLineField
            val heightPx = height.roundToPx()
            val wrappedConstraints =
                constraints.constrain(
                    Constraints(
                        minWidth = 0,
                        maxWidth = Constraints.Infinity,
                        minHeight = heightPx,
                        maxHeight = if (height == 0.dp) Constraints.Infinity else heightPx,
                    )
                )

            val placeable = measurable.measure(wrappedConstraints)
            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        }
    } else {
        val height = textLayoutState.heightForSingleLineField
        heightIn(min = height, max = if (height == 0.dp) Dp.Unspecified else height)
    }

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.addContextMenuComponents(
    textFieldSelectionState: TextFieldSelectionState,
    coroutineScope: CoroutineScope,
): Modifier =
    if (ComposeFoundationFlags.isNewContextMenuEnabled)
        addBasicTextFieldTextContextMenuComponents(textFieldSelectionState, coroutineScope)
    else this

@Composable
internal fun TextFieldCursorHandle(selectionState: TextFieldSelectionState) {
    // Does not recompose if only position of the handle changes.
    val cursorHandleVisible by
        remember(selectionState) {
            derivedStateOf { selectionState.getCursorHandleState(includePosition = false).visible }
        }
    if (cursorHandleVisible) {
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
    val startHandleState by
        remember(selectionState) {
            derivedStateOf {
                selectionState.getSelectionHandleState(
                    isStartHandle = true,
                    includePosition = false,
                )
            }
        }
    // Read once here to avoid repeating derived state reads
    val startHandle = startHandleState
    if (startHandle.visible) {
        SelectionHandle(
            offsetProvider = {
                selectionState
                    .getSelectionHandleState(isStartHandle = true, includePosition = true)
                    .position
            },
            isStartHandle = true,
            direction = startHandle.direction,
            handlesCrossed = startHandle.handlesCrossed,
            modifier =
                Modifier.pointerInput(selectionState) {
                    with(selectionState) { selectionHandleGestures(true) }
                },
            lineHeight = startHandle.lineHeight,
            minTouchTargetSize = MinTouchTargetSizeForHandles,
        )
    }

    // Does not recompose if only position of the handle changes.
    val endHandleState by
        remember(selectionState) {
            derivedStateOf {
                selectionState.getSelectionHandleState(
                    isStartHandle = false,
                    includePosition = false,
                )
            }
        }
    // Read once here to avoid repeating derived state reads
    val endHandle = endHandleState
    if (endHandle.visible) {
        SelectionHandle(
            offsetProvider = {
                selectionState
                    .getSelectionHandleState(isStartHandle = false, includePosition = true)
                    .position
            },
            isStartHandle = false,
            direction = endHandle.direction,
            handlesCrossed = endHandle.handlesCrossed,
            modifier =
                Modifier.pointerInput(selectionState) {
                    with(selectionState) { selectionHandleGestures(false) }
                },
            lineHeight = endHandle.lineHeight,
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
        @Composable { innerTextField -> innerTextField() },
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
        readOnly = readOnly,
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
        @Composable { innerTextField -> innerTextField() },
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
        readOnly = readOnly,
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
        @Composable { innerTextField -> innerTextField() },
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
        decorationBox = decorationBox,
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
        @Composable { innerTextField -> innerTextField() },
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
        decorationBox = decorationBox,
    )
}
