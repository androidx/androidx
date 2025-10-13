/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.input.internal.selection

import androidx.compose.foundation.MagnifierNode
import androidx.compose.foundation.isPlatformMagnifierSupported
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.input.internal.TextLayoutState
import androidx.compose.foundation.text.input.internal.TransformedTextFieldState
import androidx.compose.foundation.text.input.internal.fromTextLayoutToCore
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.MagnifierPostTravel
import androidx.compose.foundation.text.selection.visibleBounds
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Initializes either an actual TextFieldMagnifierNode implementation or No-op node according to
 * whether magnifier is supported.
 */
internal actual fun textFieldMagnifierNode(
    textFieldState: TransformedTextFieldState,
    textFieldSelectionState: TextFieldSelectionState,
    textLayoutState: TextLayoutState,
    visible: Boolean
): TextFieldMagnifierNode {
    return if (isPlatformMagnifierSupported()) {
        TextFieldMagnifierNodeImpl(
            textFieldState = textFieldState,
            textFieldSelectionState = textFieldSelectionState,
            textLayoutState = textLayoutState,
            visible = visible
        )
    } else {
        object : TextFieldMagnifierNode() {
            override fun update(
                textFieldState: TransformedTextFieldState,
                textFieldSelectionState: TextFieldSelectionState,
                textLayoutState: TextLayoutState,
                visible: Boolean
            ) {
            }
        }
    }
}

internal class TextFieldMagnifierNodeImpl(
    private var textFieldState: TransformedTextFieldState,
    private var textFieldSelectionState: TextFieldSelectionState,
    private var textLayoutState: TextLayoutState,
    private var visible: Boolean
) : TextFieldMagnifierNode(),
    ObserverModifierNode,
    CompositionLocalConsumerModifierNode {

    private var sourceCenter by mutableStateOf(Offset.Unspecified)
    private var color by mutableStateOf(Color.Unspecified)
    private var density by mutableStateOf(Density(1f,1f))
    private var magnifierSize by mutableStateOf(DpSize.Zero)

    private val magnifierNode = delegate(
        MagnifierNode(
            sourceCenter = { sourceCenter },
            onSizeChanged = { magnifierSize = it },
        )
    )

    private var positioningJob : Job? = null

    override fun onAttach() {
        super.onAttach()
        onObservedReadsChanged()
        restartPositionJob()
    }

    override fun onObservedReadsChanged() {
        observeReads {
            color = currentValueOf(LocalTextSelectionColors).handleColor
            density = currentValueOf(LocalDensity)

            magnifierNode.update(color = color)
        }
    }

    override fun update(
        textFieldState: TransformedTextFieldState,
        textFieldSelectionState: TextFieldSelectionState,
        textLayoutState: TextLayoutState,
        visible: Boolean
    ) {
        val previousTextFieldState = this.textFieldState
        val previousSelectionState = this.textFieldSelectionState
        val previousLayoutState = this.textLayoutState
        val wasVisible = this.visible

        this.textFieldState = textFieldState
        this.textFieldSelectionState = textFieldSelectionState
        this.textLayoutState = textLayoutState
        this.visible = visible

        if (textFieldState != previousTextFieldState ||
            textFieldSelectionState != previousSelectionState ||
            textLayoutState != previousLayoutState ||
            visible != wasVisible
        ) {
            restartPositionJob()
        }
    }

    private fun restartPositionJob() {
        positioningJob?.cancel()
        if (visible) {
            positioningJob = coroutineScope.launch {
                snapshotFlow {
                    calculateSelectionMagnifierCenterIOS(
                        textFieldState = textFieldState,
                        selectionState = textFieldSelectionState,
                        textLayoutState = textLayoutState,
                        magnifierSize = with(density) {
                            IntSize(
                                magnifierSize.width.roundToPx(),
                                magnifierSize.height.roundToPx(),
                            )
                        },
                        density = density.density
                    )
                }.collect {
                    sourceCenter = it
                }
            }
        } else {
            sourceCenter = Offset.Unspecified
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        with(magnifierNode) { draw() }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        magnifierNode.onGloballyPositioned(coordinates)
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        with(magnifierNode) { applySemantics() }
    }
}

private fun calculateSelectionMagnifierCenterIOS(
    textFieldState: TransformedTextFieldState,
    selectionState: TextFieldSelectionState,
    textLayoutState: TextLayoutState,
    magnifierSize: IntSize,
    density: Float
): Offset {

    val dragPosition =
        selectionState.handleDragPosition.takeIf { it.isSpecified } ?: return Offset.Unspecified

    val selection = textFieldState.visualText.selection

    val textOffset = when (selectionState.draggingHandle) {
        null -> return Offset.Unspecified
        Handle.Cursor,
        Handle.SelectionStart -> selection.start

        Handle.SelectionEnd -> selection.end
    }

    val layoutResult = textLayoutState.layoutResult ?: return Offset.Unspecified

    // hide magnifier when selection goes below the text field
    if (dragPosition.y > layoutResult.lastBaseline + MagnifierPostTravel.value * density) {
        return Offset.Unspecified
    }

    val coreNodeBounds = textLayoutState.coreNodeCoordinates
        ?.takeIf { it.isAttached }
        ?.visibleBounds()
        ?.takeIf { !it.isEmpty }
        ?: return Offset.Unspecified

    // Center vertically on the current line.
    val centerY = if (textFieldState.visualText.text.isNotEmpty()) {
        val line = layoutResult.getLineForOffset(textOffset)
        val top = layoutResult.getLineTop(line)
        val bottom = layoutResult.getLineBottom(line)
        ((bottom - top) / 2) + top
    } else {
        coreNodeBounds.center.y
    }

    val offset = textLayoutState.fromTextLayoutToCore(Offset(dragPosition.x, centerY))

    return Offset(
        x = offset.x.coerceIn(
            -magnifierSize.width / 4f,
            max(0f, coreNodeBounds.right) + magnifierSize.width / 4
        ),
        y = offset.y.coerceIn(
            coreNodeBounds.top,
            coreNodeBounds.bottom
        )
    )
}
