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

package androidx.compose.foundation.text2.input.internal.selection

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MagnifierNode
import androidx.compose.foundation.isPlatformMagnifierSupported
import androidx.compose.foundation.text.selection.MagnifierSpringSpec
import androidx.compose.foundation.text.selection.OffsetDisplacementThreshold
import androidx.compose.foundation.text.selection.UnspecifiedSafeOffsetVectorConverter
import androidx.compose.foundation.text2.input.internal.TextLayoutState
import androidx.compose.foundation.text2.input.internal.TransformedTextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
internal class TextFieldMagnifierNodeImpl28(
    private var textFieldState: TransformedTextFieldState,
    private var textFieldSelectionState: TextFieldSelectionState,
    private var textLayoutState: TextLayoutState,
    private var isFocused: Boolean
) : TextFieldMagnifierNode(), CompositionLocalConsumerModifierNode {

    private var magnifierSize: IntSize by mutableStateOf(IntSize.Zero)

    // Can't use Offset.VectorConverter because we need to handle Unspecified specially.
    private val animatable =
        Animatable(
            initialValue = calculateSelectionMagnifierCenterAndroid(
                textFieldState = textFieldState,
                selectionState = textFieldSelectionState,
                textLayoutState = textLayoutState,
                magnifierSize = magnifierSize
            ),
            typeConverter = UnspecifiedSafeOffsetVectorConverter,
            visibilityThreshold = OffsetDisplacementThreshold
        )

    private val magnifierNode = delegate(
        MagnifierNode(
            sourceCenter = { animatable.value },
            onSizeChanged = { size ->
                magnifierSize = with(currentValueOf(LocalDensity)) {
                    IntSize(size.width.roundToPx(), size.height.roundToPx())
                }
            },
            useTextDefault = true
        )
    )

    private var animationJob: Job? = null

    override fun onAttach() {
        restartAnimationJob()
    }

    override fun update(
        textFieldState: TransformedTextFieldState,
        textFieldSelectionState: TextFieldSelectionState,
        textLayoutState: TextLayoutState,
        isFocused: Boolean
    ) {
        val previousTextFieldState = this.textFieldState
        val previousSelectionState = this.textFieldSelectionState
        val previousLayoutState = this.textLayoutState
        val wasFocused = this.isFocused

        this.textFieldState = textFieldState
        this.textFieldSelectionState = textFieldSelectionState
        this.textLayoutState = textLayoutState
        this.isFocused = isFocused

        if (textFieldState != previousTextFieldState ||
            textFieldSelectionState != previousSelectionState ||
            textLayoutState != previousLayoutState ||
            isFocused != wasFocused
        ) {
            restartAnimationJob()
        }
    }

    private fun restartAnimationJob() {
        animationJob?.cancel()
        animationJob = null
        // never start an expensive animation job if we do not have focus or
        // magnifier is not supported.
        if (!isFocused || !isPlatformMagnifierSupported()) return
        animationJob = coroutineScope.launch {
            val animationScope = this
            snapshotFlow {
                calculateSelectionMagnifierCenterAndroid(
                    textFieldState,
                    textFieldSelectionState,
                    textLayoutState,
                    magnifierSize
                )
            }
                .collect { targetValue ->
                    // Only animate the position when moving vertically (i.e. jumping between
                    // lines), since horizontal movement in a single line should stay as close to
                    // the gesture as possible and animation would only add unnecessary lag.
                    if (
                        animatable.value.isSpecified &&
                        targetValue.isSpecified &&
                        animatable.value.y != targetValue.y
                    ) {
                        // Launch the animation, instead of cancelling and re-starting manually via
                        // collectLatest, so if another animation is started before this one
                        // finishes, the new one will use the correct velocity, e.g. in order to
                        // propagate spring inertia.
                        animationScope.launch {
                            animatable.animateTo(targetValue, MagnifierSpringSpec)
                        }
                    } else {
                        animatable.snapTo(targetValue)
                    }
                }
        }
    }

    // TODO(halilibo) Remove this once delegation can propagate this events on its own
    override fun ContentDrawScope.draw() {
        drawContent()
        with(magnifierNode) { draw() }
    }

    // TODO(halilibo) Remove this once delegation can propagate this events on its own
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        magnifierNode.onGloballyPositioned(coordinates)
    }

    // TODO(halilibo) Remove this once delegation can propagate this events on its own
    override fun SemanticsPropertyReceiver.applySemantics() {
        with(magnifierNode) { applySemantics() }
    }
}

/**
 * Initializes either an actual TextFieldMagnifierNode implementation or No-op node according to
 * whether magnifier is supported.
 */
@SuppressLint("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
internal actual fun textFieldMagnifierNode(
    textFieldState: TransformedTextFieldState,
    textFieldSelectionState: TextFieldSelectionState,
    textLayoutState: TextLayoutState,
    isFocused: Boolean
): TextFieldMagnifierNode {
    return if (isPlatformMagnifierSupported()) {
        TextFieldMagnifierNodeImpl28(
            textFieldState = textFieldState,
            textFieldSelectionState = textFieldSelectionState,
            textLayoutState = textLayoutState,
            isFocused = isFocused
        )
    } else {
        object : TextFieldMagnifierNode() {
            override fun update(
                textFieldState: TransformedTextFieldState,
                textFieldSelectionState: TextFieldSelectionState,
                textLayoutState: TextLayoutState,
                isFocused: Boolean
            ) {}
        }
    }
}
