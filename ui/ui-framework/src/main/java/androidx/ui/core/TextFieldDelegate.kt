/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import androidx.ui.geometry.Rect
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.input.EditOperation
import androidx.ui.input.EditProcessor
import androidx.ui.input.FinishComposingTextEditOp
import androidx.ui.input.INVALID_SESSION
import androidx.ui.input.ImeAction
import androidx.ui.input.InputSessionToken
import androidx.ui.input.InputState
import androidx.ui.input.KeyboardType
import androidx.ui.input.OffsetMap
import androidx.ui.input.SetSelectionEditOp
import androidx.ui.input.TextInputService
import androidx.ui.input.TransformedText
import androidx.ui.input.VisualTransformation
import androidx.ui.text.AnnotatedString
import androidx.ui.text.Paragraph
import androidx.ui.text.ParagraphConstraints
import androidx.ui.text.SpanStyle
import androidx.ui.text.TextDelegate
import androidx.ui.text.TextLayoutResult
import androidx.ui.text.TextPainter
import androidx.ui.text.TextRange
import androidx.ui.text.TextStyle
import androidx.ui.text.font.Font
import androidx.ui.text.style.TextDecoration
import androidx.ui.text.style.TextDirectionAlgorithm
import androidx.ui.unit.Density
import androidx.ui.unit.IntPx
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import androidx.ui.unit.isFinite
import androidx.ui.unit.px
import kotlin.math.ceil
import kotlin.math.roundToInt

// -5185306 = 0xFFB0E0E6 = A(0xFF), R(0xB0), G(0xE0), B(0xE6)
internal const val DEFAULT_COMPOSITION_COLOR: Int = -5185306

/**
 * Computed the line height for the empty TextField.
 *
 * The bounding box or x-advance of the empty text is empty, i.e. 0x0 box or 0px advance. However
 * this is not useful for TextField since text field want to reserve some amount of height for
 * accepting touch for starting text input. In Android, uses FontMetrics of the first font in the
 * fallback chain to compute this height, this is because custom font may have different
 * ascender/descender from the default font in Android.
 *
 * Until we have font metrics APIs, use the height of reference text as a workaround.
 */
private fun computeLineHeightForEmptyText(
    style: TextStyle,
    density: Density,
    resourceLoader: Font.ResourceLoader
): IntPx {
    return Paragraph(
        text = "H", // No meaning: just a reference character.
        style = TextStyle(
            textDirectionAlgorithm = TextDirectionAlgorithm.ContentOrLtr
        ).merge(style),
        spanStyles = listOf(),
        maxLines = 1,
        ellipsis = false,
        density = density,
        resourceLoader = resourceLoader,
        constraints = ParagraphConstraints(width = Float.POSITIVE_INFINITY)
    ).height.toIntPx()
}

private fun Float.toIntPx(): IntPx = ceil(this).roundToInt().ipx

internal class TextFieldDelegate {
    companion object {
        /**
         * Process text layout with given constraint.
         *
         * @param textDelegate The text painter
         * @param constraints The layout constraints
         * @return the bounding box size(width and height) of the layout result
         */
        @JvmStatic
        fun layout(
            textDelegate: TextDelegate,
            constraints: Constraints,
            prevResultText: TextLayoutResult? = null
        ): Triple<IntPx, IntPx, TextLayoutResult> {
            val layoutResult = if (constraints.maxWidth.isFinite()) {
                textDelegate.layout(
                    Constraints.fixedWidth(constraints.maxWidth),
                    prevResultText
                )
            } else {
                // TextField want to fill the required width but if infinite width is passed,
                // falling back to wrap-content behavior since it may be in the horizontal scroller.
                textDelegate.layoutIntrinsics()
                textDelegate.layout(
                    Constraints.fixedWidth(textDelegate.maxIntrinsicWidth),
                    prevResultText
                )
            }

            val isEmptyText = textDelegate.text.text.isEmpty()
            val height = if (isEmptyText) {
                computeLineHeightForEmptyText(
                    style = textDelegate.style,
                    density = textDelegate.density,
                    resourceLoader = textDelegate.resourceLoader
                )
            } else {
                layoutResult.size.height
            }
            val width = layoutResult.size.width
            return Triple(width, height, layoutResult)
        }

        /**
         * Draw the text content to the canvas
         *
         * @param canvas The target canvas.
         * @param value The editor state
         * @param offsetMap The offset map
         * @param hasFocus true if this composable is focused, otherwise false
         * @param selectionColor The selection color
         */
        @JvmStatic
        fun draw(
            canvas: Canvas,
            value: InputState,
            offsetMap: OffsetMap,
            textLayoutResult: TextLayoutResult,
            hasFocus: Boolean,
            selectionColor: Color
        ) {
            if (value.selection.collapsed) {
                if (hasFocus) {
                    val cursorRect = textLayoutResult.getCursorRect(
                        offsetMap.originalToTransformed(value.selection.min))
                    canvas.drawRect(cursorRect, Paint().apply { this.color = Color.Black })
                }
            } else {
                val start = offsetMap.originalToTransformed(value.selection.min)
                val end = offsetMap.originalToTransformed(value.selection.max)
                if (start != end) {
                    val selectionPath = textLayoutResult.getPathForRange(start, end)
                    canvas.drawPath(selectionPath, Paint().apply { this.color = selectionColor })
                }
            }
            TextPainter.paint(canvas, textLayoutResult)
        }

        /**
         * Notify system that focused input area.
         *
         * System is typically scrolled up not to be covered by keyboard.
         *
         * @param value The editor model
         * @param textDelegate The text delegate
         * @param layoutCoordinates The layout coordinates
         * @param textInputService The text input service
         * @param token The current input session token.
         * @param hasFocus True if focus is gained.
         * @param offsetMap The mapper from/to editing buffer to/from visible text.
         */
        @JvmStatic
        fun notifyFocusedRect(
            value: InputState,
            textDelegate: TextDelegate,
            textLayoutResult: TextLayoutResult,
            layoutCoordinates: LayoutCoordinates,
            textInputService: TextInputService,
            token: InputSessionToken,
            hasFocus: Boolean,
            offsetMap: OffsetMap
        ) {
            if (!hasFocus) {
                return
            }

            val bbox = if (value.selection.max < value.text.length) {
                textLayoutResult.getBoundingBox(
                    offsetMap.originalToTransformed(value.selection.max))
            } else if (value.selection.max != 0) {
                textLayoutResult.getBoundingBox(
                    offsetMap.originalToTransformed(value.selection.max) - 1)
            } else {
                val lineHeightForEmptyText = computeLineHeightForEmptyText(
                    textDelegate.style,
                    textDelegate.density,
                    textDelegate.resourceLoader
                )
                Rect(0f, 0f, 1.0f, lineHeightForEmptyText.value.toFloat())
            }
            val globalLT = layoutCoordinates.localToRoot(PxPosition(bbox.left.px, bbox.top.px))

            textInputService.notifyFocusedRect(
                token,
                Rect.fromLTWH(
                    globalLT.x.value,
                    globalLT.y.value,
                    bbox.width,
                    bbox.height
                )
            )
        }

        /**
         * Called when edit operations are passed from TextInputService
         *
         * @param ops A list of edit operations.
         * @param editProcessor The edit processor
         * @param onValueChange The callback called when the new editor state arrives.
         */
        @JvmStatic
        internal fun onEditCommand(
            ops: List<EditOperation>,
            editProcessor: EditProcessor,
            onValueChange: (InputState) -> Unit
        ) {
            onValueChange(editProcessor.onEditCommands(ops))
        }

        /**
         * Called when onRelease event is fired.
         *
         * @param position The event position in composable coordinate.
         * @param textLayoutResult The text layout result
         * @param editProcessor The edit processor
         * @param offsetMap The offset map
         * @param onValueChange The callback called when the new editor state arrives.
         * @param textInputService The text input service
         * @param token The current input session token.
         * @param hasFocus True if the composable has input focus, otherwise false.
         */
        @JvmStatic
        fun onRelease(
            position: PxPosition,
            textLayoutResult: TextLayoutResult,
            editProcessor: EditProcessor,
            offsetMap: OffsetMap,
            onValueChange: (InputState) -> Unit,
            textInputService: TextInputService?,
            token: InputSessionToken,
            hasFocus: Boolean
        ) {
            textInputService?.showSoftwareKeyboard(token)
            if (hasFocus) {
                val offset = offsetMap.transformedToOriginal(
                    textLayoutResult.getOffsetForPosition(position))
                onEditCommand(
                    listOf(SetSelectionEditOp(offset, offset)),
                    editProcessor,
                    onValueChange)
            }
        }

        /**
         * Called when the composable gained input focus
         *
         * @param textInputService The text input service
         * @param value The editor state
         * @param editProcessor The edit processor
         * @param keyboardType The keyboard type
         * @param onValueChange The callback called when the new editor state arrives.
         * @param onImeActionPerformed The callback called when the editor action arrives.
         */
        @JvmStatic
        fun onFocus(
            textInputService: TextInputService?,
            value: InputState,
            editProcessor: EditProcessor,
            keyboardType: KeyboardType,
            imeAction: ImeAction,
            onValueChange: (InputState) -> Unit,
            onImeActionPerformed: (ImeAction) -> Unit
        ): InputSessionToken {
            return textInputService?.startInput(
                initModel = InputState(value.text, value.selection, value.composition),
                keyboardType = keyboardType,
                imeAction = imeAction,
                onEditCommand = { onEditCommand(it, editProcessor, onValueChange) },
                onImeActionPerformed = onImeActionPerformed) ?: INVALID_SESSION
        }

        /**
         * Called when the composable loses input focus
         *
         * @param textInputService The text input service
         * @param token The current input session token.
         * @param editProcessor The edit processor
         * @param onValueChange The callback called when the new editor state arrives.
         */
        @JvmStatic
        fun onBlur(
            textInputService: TextInputService?,
            token: InputSessionToken,
            editProcessor: EditProcessor,
            onValueChange: (InputState) -> Unit
        ) {
            onEditCommand(listOf(FinishComposingTextEditOp()), editProcessor, onValueChange)
            textInputService?.stopInput(token)
        }

        /**
         * Helper function of applying visual transformation method to the EditorModel.
         *
         * @param value An editor state
         * @param visualTransformation A visual transformation
         */
        @JvmStatic
        fun applyVisualFilter(
            value: InputState,
            visualTransformation: VisualTransformation?
        ): TransformedText {
            val annotatedString = AnnotatedString(value.text)
            return visualTransformation?.filter(annotatedString)
                    ?: TransformedText(annotatedString, OffsetMap.identityOffsetMap)
        }

        /**
         *  Apply the composition text decoration (undeline) to the transformed text.
         *
         *  @param compositionRange An input state
         *  @param transformed A transformed text
         *  @return The transformed text with composition decoration.
         */
        fun applyCompositionDecoration(
            compositionRange: TextRange,
            transformed: TransformedText
        ): TransformedText =
            TransformedText(
                AnnotatedString.Builder(transformed.transformedText).apply {
                    addStyle(
                        SpanStyle(textDecoration = TextDecoration.Underline),
                        compositionRange.start,
                        compositionRange.end
                    )
                }.toAnnotatedString(),
                transformed.offsetMap
            )
    }
}
