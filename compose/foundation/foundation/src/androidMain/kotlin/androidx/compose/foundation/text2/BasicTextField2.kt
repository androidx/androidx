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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.DefaultMinLines
import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.foundation.text.heightInLines
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SimpleLayout
import androidx.compose.foundation.text.textFieldMinSize
import androidx.compose.foundation.text2.service.AndroidTextInputPlugin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalPlatformTextInputPluginRegistry
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextPainter
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
 * @param state State object that holds the internal state of a [BasicTextField2]
 * @param modifier optional [Modifier] for this text field.
 * @param enabled controls the enabled state of the [BasicTextField2]. When `false`, the text
 * field will be neither editable nor focusable, the input of the text field will not be selectable.
 * @param readOnly controls the editable state of the [BasicTextField2]. When `true`, the text
 * field can not be modified, however, a user can focus it and copy text from it. Read-only text
 * fields are usually used to display pre-filled forms that user can not edit
 * @param textStyle Style configuration for text content that's displayed in the editor.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this TextField. You can create and pass in your own remembered [MutableInteractionSource]
 * if you want to observe [Interaction]s and customize the appearance / behavior of this TextField
 * for different [Interaction]s.
 * @param cursorBrush [Brush] to paint cursor with. If [SolidColor] with [Color.Unspecified]
 * provided, there will be no cursor drawn
 * @param minLines The minimum height in terms of minimum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines].
 * @param maxLines The maximum height in terms of maximum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines].
 * @param keyboardOptions software keyboard options that contains configuration such as
 * [KeyboardType] and [ImeAction].
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
@OptIn(InternalFoundationTextApi::class)
@Composable
fun BasicTextField2(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    minLines: Int = DefaultMinLines,
    maxLines: Int = Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onTextLayout: Density.(TextLayoutResult) -> Unit = {},
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    // only read from local and create an adapter if this text field is enabled and editable
    val textInputAdapter = LocalPlatformTextInputPluginRegistry.takeIf { enabled && !readOnly }
        ?.current?.rememberAdapter(AndroidTextInputPlugin)

    val fontFamilyResolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    val selectionBackgroundColor = LocalTextSelectionColors.current.backgroundColor
    val singleLine = minLines == 1 && maxLines == 1
    // We're using this to communicate focus state to cursor for now.
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    val textLayoutState = remember {
        TextLayoutState(
            TextDelegate(
                text = state.value.annotatedString,
                style = textStyle,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                softWrap = true,
                placeholders = emptyList()
            )
        )
    }

    val drawModifier = Modifier.drawBehind {
        textLayoutState.layoutResult?.let { layoutResult ->
            // draw selection
            val value = state.value
            if (!value.selection.collapsed) {
                val start = value.selection.min
                val end = value.selection.max
                if (start != end) {
                    val selectionPath = layoutResult.getPathForRange(start, end)
                    drawPath(selectionPath, color = selectionBackgroundColor)
                }
            }
            // draw text
            drawIntoCanvas { canvas ->
                TextPainter.paint(canvas, layoutResult)
            }
        }
    }

    val cursorModifier = Modifier.cursor(
        textLayoutState = textLayoutState,
        isFocused = interactionSource.collectIsFocusedAsState().value,
        state = state,
        cursorBrush = cursorBrush,
        enabled = enabled && !readOnly
    )

    val decorationModifiers = modifier
        .then(
            // semantics + some focus + input session + touch to focus
            TextFieldDecoratorModifierElement(
                textFieldState = state,
                textLayoutState = textLayoutState,
                textInputAdapter = textInputAdapter,
                enabled = enabled,
                readOnly = readOnly,
                keyboardOptions = keyboardOptions,
                singleLine = singleLine
            )
        )
        .focusable(interactionSource = interactionSource, enabled = enabled)

    Box(decorationModifiers) {
        decorationBox(innerTextField = {
            val coreModifiers = Modifier
                .heightInLines(
                    textStyle = textStyle,
                    minLines = minLines,
                    maxLines = maxLines
                )
                .then(drawModifier)
                .textFieldMinSize(textStyle)
                .then(cursorModifier)
                .onGloballyPositioned {
                    textLayoutState.proxy?.innerTextFieldCoordinates = it
                }

            // A custom layout that hosts TextLayout, Selection and Cursor Handles.
            SimpleLayout(coreModifiers) {
                // Text Layout
                Layout { _, constraints ->
                    val result = with(textLayoutState) {
                        layout(
                            text = state.value.annotatedString,
                            textStyle = textStyle,
                            softWrap = true,
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
            }
        })
    }
}
