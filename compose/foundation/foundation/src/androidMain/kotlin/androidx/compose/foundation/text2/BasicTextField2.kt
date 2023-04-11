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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.foundation.text.heightInLines
import androidx.compose.foundation.text.textFieldMinSize
import androidx.compose.foundation.text2.input.CodepointTransformation
import androidx.compose.foundation.text2.input.TextEditFilter
import androidx.compose.foundation.text2.input.TextFieldLineLimits
import androidx.compose.foundation.text2.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text2.input.TextFieldLineLimits.SingleLine
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.internal.AndroidTextInputPlugin
import androidx.compose.foundation.text2.input.internal.TextFieldCoreModifier
import androidx.compose.foundation.text2.input.internal.TextFieldDecoratorModifier
import androidx.compose.foundation.text2.input.internal.TextLayoutState
import androidx.compose.foundation.text2.input.toVisualText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalPlatformTextInputPluginRegistry
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

/**
 * BasicTextField2 is a new text input Composable under heavy development. Please refrain from
 * using it in production since it has a very unstable API and implementation for the time being.
 * Many core features like selection, cursor, gestures, etc. may fail or simply not exist.
 *
 * @param state [TextFieldState] object that holds the internal state of a [BasicTextField2].
 * @param modifier optional [Modifier] for this text field.
 * @param enabled controls the enabled state of the [BasicTextField2]. When `false`, the text
 * field will be neither editable nor focusable, the input of the text field will not be selectable.
 * @param readOnly controls the editable state of the [BasicTextField2]. When `true`, the text
 * field can not be modified, however, a user can focus it and copy text from it. Read-only text
 * fields are usually used to display pre-filled forms that user can not edit.
 * @param filter Optional [TextEditFilter] that will be used to filter changes to the
 * [TextFieldState] made by the user. The filter will be applied to changes made by hardware and
 * software keyboard events, pasting or dropping text, accessibility services, and tests. The filter
 * will _not_ be applied when changing the [state] programmatically, or when the filter is changed.
 * If the filter is changed on an existing text field, it will be applied to the next user edit.
 * the filter will not immediately affect the current [state].
 * @param textStyle Style configuration for text content that's displayed in the editor.
 * @param keyboardOptions software keyboard options that contains configuration such as
 * [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback
 * is called. Note that this IME action may be different from what you specified in
 * [KeyboardOptions.imeAction].
 * @param lineLimits Whether the text field should be [SingleLine], scroll horizontally, and
 * ignore newlines; or [MultiLine] and grow and scroll vertically.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 * [TextLayoutResult] object that callback provides contains paragraph information, size of the
 * text, baselines and other details. The callback can be used to add additional decoration or
 * functionality to the text. For example, to draw a cursor or selection around the text. [Density]
 * scope is the one that was used while creating the given text layout.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this TextField. You can create and pass in your own remembered [MutableInteractionSource]
 * if you want to observe [Interaction]s and customize the appearance / behavior of this TextField
 * for different [Interaction]s.
 * @param cursorBrush [Brush] to paint cursor with. If [SolidColor] with [Color.Unspecified]
 * provided, there will be no cursor drawn
 * @param scrollState Scroll state that manages either horizontal or vertical scroll of TextField.
 * If [lineLimits] is [SingleLine], this text field is treated as single line with horizontal
 * scroll behavior. In other cases the text field becomes vertically scrollable.
 * @param codepointTransformation Visual transformation interface that provides a 1-to-1 mapping of
 * codepoints.
 * @param decorationBox Composable lambda that allows to add decorations around text field, such
 * as icon, placeholder, helper messages or similar, and automatically increase the hit target area
 * of the text field. To allow you to control the placement of the inner text field relative to your
 * decorations, the text field implementation will pass in a framework-controlled composable
 * parameter "innerTextField" to the decorationBox lambda you provide. You must call
 * innerTextField exactly once.
 */
@ExperimentalFoundationApi
@OptIn(InternalFoundationTextApi::class)
@Composable
fun BasicTextField2(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    filter: TextEditFilter? = null,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: Density.(TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    scrollState: ScrollState = rememberScrollState(),
    codepointTransformation: CodepointTransformation? = null,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    // only read from local and create an adapter if this text field is enabled and editable
    val textInputAdapter = LocalPlatformTextInputPluginRegistry.takeIf { enabled && !readOnly }
        ?.current?.rememberAdapter(AndroidTextInputPlugin)

    val fontFamilyResolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val singleLine = lineLimits == SingleLine
    // We're using this to communicate focus state to cursor for now.
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    val orientation = if (singleLine) Orientation.Horizontal else Orientation.Vertical

    val textLayoutState = remember {
        TextLayoutState(
            TextDelegate(
                text = AnnotatedString(state.text.toString()),
                style = textStyle,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                softWrap = true,
                placeholders = emptyList()
            )
        )
    }

    val decorationModifiers = modifier
        .then(
            // semantics + some focus + input session + touch to focus
            TextFieldDecoratorModifier(
                textFieldState = state,
                textLayoutState = textLayoutState,
                textInputAdapter = textInputAdapter,
                filter = filter,
                enabled = enabled,
                readOnly = readOnly,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
            )
        )
        .focusable(interactionSource = interactionSource, enabled = enabled)
        .scrollable(
            orientation = orientation,
            reverseDirection = ScrollableDefaults.reverseDirection(
                layoutDirection = layoutDirection,
                orientation = orientation,
                reverseScrolling = false
            ),
            state = scrollState,
            interactionSource = interactionSource,
            enabled = enabled && scrollState.maxValue > 0
        )

    Box(decorationModifiers) {
        decorationBox(innerTextField = {
            val minLines: Int
            val maxLines: Int
            if (lineLimits is MultiLine) {
                minLines = lineLimits.minHeightInLines
                maxLines = lineLimits.maxHeightInLines
            } else {
                minLines = 1
                maxLines = 1
            }

            val coreModifiers = Modifier
                .heightInLines(
                    textStyle = textStyle,
                    minLines = minLines,
                    maxLines = maxLines
                )
                .textFieldMinSize(textStyle)
                .clipToBounds()
                .then(
                    TextFieldCoreModifier(
                        isFocused = interactionSource.collectIsFocusedAsState().value,
                        textLayoutState = textLayoutState,
                        textFieldState = state,
                        cursorBrush = cursorBrush,
                        writeable = enabled && !readOnly,
                        scrollState = scrollState,
                        orientation = orientation
                    )
                )

            Layout(modifier = coreModifiers) { _, constraints ->
                val result = with(textLayoutState) {
                    val visualText = state.text.toVisualText(codepointTransformation)
                    layout(
                        text = AnnotatedString(visualText.toString()),
                        textStyle = textStyle,
                        softWrap = !singleLine,
                        density = density,
                        fontFamilyResolver = fontFamilyResolver,
                        constraints = constraints,
                        onTextLayout = onTextLayout
                    )
                }

                // TODO: min height

                layout(
                    width = result.size.width,
                    height = result.size.height,
                    alignmentLines = mapOf(
                        FirstBaseline to result.firstBaseline.roundToInt(),
                        LastBaseline to result.lastBaseline.roundToInt()
                    )
                ) {}
            }
        })
    }
}
