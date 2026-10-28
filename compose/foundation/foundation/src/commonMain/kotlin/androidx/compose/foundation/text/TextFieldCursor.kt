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

@file:Suppress("DEPRECATION") // b/552879150

package androidx.compose.foundation.text

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.internal.CursorAnimationState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalCursorBlinkEnabled
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import kotlin.math.floor
import kotlin.math.round
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.cursor(
    state: LegacyTextFieldState,
    value: TextFieldValue,
    offsetMapping: OffsetMapping,
    cursorBrush: Brush,
    enabled: Boolean,
): Modifier =
    if (enabled) {
        if (ComposeFoundationFlags.isNewTextFieldModifierImplementationEnabled) {
            this then CursorElement(state, value, offsetMapping, cursorBrush)
        } else {
            composed {
                val animateCursor = LocalCursorBlinkEnabled.current
                val cursorAnimation =
                    remember(animateCursor) { CursorAnimationState(animateCursor) }
                // Don't bother animating the cursor if it wouldn't draw any pixels.
                val isBrushSpecified =
                    !(cursorBrush is SolidColor && cursorBrush.value.isUnspecified)
                // Only animate the cursor when its window is actually focused. This also disables
                // the
                // cursor animation when the screen is off.
                // TODO confirm screen-off behavior.
                val isWindowFocused = LocalWindowInfo.current.isWindowFocused
                if (
                    isWindowFocused &&
                        state.hasFocus &&
                        value.selection.collapsed &&
                        isBrushSpecified
                ) {
                    LaunchedEffect(value.annotatedString, value.selection) {
                        cursorAnimation.snapToVisibleAndAnimate()
                    }
                    drawWithContent {
                        this.drawContent()
                        val cursorAlphaValue = cursorAnimation.cursorAlpha
                        if (cursorAlphaValue != 0f) {
                            val transformedOffset =
                                offsetMapping.originalToTransformed(value.selection.start)
                            val cursorRect =
                                state.layoutResult?.value?.getCursorRect(transformedOffset)
                                    ?: Rect(0f, 0f, 0f, 0f)
                            val cursorWidth = floor(DefaultCursorThickness.toPx()).coerceAtLeast(1f)
                            val cursorX =
                                (cursorRect.left + cursorWidth / 2)
                                    // Do not use coerceIn because it is not guaranteed that the
                                    // minimum
                                    // value is
                                    // smaller than the maximum value.
                                    .coerceAtMost(size.width - cursorWidth / 2)
                                    .coerceAtLeast(cursorWidth / 2)
                                    .let {
                                        // When cursor width is odd, draw it in the middle of a
                                        // pixel,
                                        // to avoid blurring due to antialiasing.
                                        if (cursorWidth.toInt() % 2 == 1) {
                                            floor(it) + 0.5f // round to nearest n+0.5
                                        } else round(it)
                                    }

                            drawLine(
                                brush = cursorBrush,
                                start = Offset(cursorX, cursorRect.top),
                                end = Offset(cursorX, cursorRect.bottom),
                                alpha = cursorAlphaValue,
                                strokeWidth = cursorWidth,
                            )
                        }
                    }
                } else {
                    Modifier
                }
            }
        }
    } else {
        this
    }

private class CursorElement(
    private val state: LegacyTextFieldState,
    private val value: TextFieldValue,
    private val offsetMapping: OffsetMapping,
    private val cursorBrush: Brush,
) : ModifierNodeElement<CursorNode>() {
    override fun create(): CursorNode = CursorNode(state, value, offsetMapping, cursorBrush)

    override fun update(node: CursorNode) {
        node.update(state, value, offsetMapping, cursorBrush)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "cursor"
        properties["state"] = state
        properties["value"] = value
        properties["offsetMapping"] = offsetMapping
        properties["cursorBrush"] = cursorBrush
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CursorElement) return false
        if (state != other.state) return false
        if (value != other.value) return false
        if (offsetMapping != other.offsetMapping) return false
        if (cursorBrush != other.cursorBrush) return false
        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + offsetMapping.hashCode()
        result = 31 * result + cursorBrush.hashCode()
        return result
    }
}

private class CursorNode(
    state: LegacyTextFieldState,
    value: TextFieldValue,
    private var offsetMapping: OffsetMapping,
    cursorBrush: Brush,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private var state by mutableStateOf(state)
    private var value by mutableStateOf(value)
    private var cursorBrush by mutableStateOf(cursorBrush)

    private var cursorAnimation: CursorAnimationState? = null
    private var animationJob: Job? = null

    override fun onAttach() {
        startCursorJob()
    }

    override fun onDetach() {
        animationJob?.cancel()
        animationJob = null
        cursorAnimation?.cancelAndHide()
        cursorAnimation = null
    }

    fun update(
        state: LegacyTextFieldState,
        value: TextFieldValue,
        offsetMapping: OffsetMapping,
        cursorBrush: Brush,
    ) {
        this.state = state
        this.value = value
        this.offsetMapping = offsetMapping
        this.cursorBrush = cursorBrush
    }

    private fun startCursorJob() {
        animationJob?.cancel()
        animationJob = coroutineScope.launch {
            snapshotFlow {
                if (!isAttached) return@snapshotFlow null
                val animateCursor = currentValueOf(LocalCursorBlinkEnabled)
                val isWindowFocused = currentValueOf(LocalWindowInfo).isWindowFocused
                val brush = cursorBrush
                val isBrushSpecified = !(brush is SolidColor && brush.value.isUnspecified)
                val currentValue = value
                val shouldAnimate =
                    isWindowFocused &&
                        state.hasFocus &&
                        currentValue.selection.collapsed &&
                        isBrushSpecified
                if (shouldAnimate) {
                    Triple(animateCursor, currentValue.annotatedString, currentValue.selection)
                } else {
                    null
                }
            }
                .collectLatest { params ->
                    if (params == null) {
                        cursorAnimation?.cancelAndHide()
                    } else {
                        val (animateCursor, _, _) = params
                        var anim = cursorAnimation
                        if (anim == null || anim.animate != animateCursor) {
                            anim?.cancelAndHide()
                            anim = CursorAnimationState(animateCursor)
                            cursorAnimation = anim
                            invalidateDraw()
                        }
                        anim.snapToVisibleAndAnimate()
                    }
                }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val cursorAlphaValue = cursorAnimation?.cursorAlpha ?: 0f
        if (cursorAlphaValue != 0f) {
            val currentValue = value
            val transformedOffset =
                offsetMapping.originalToTransformed(currentValue.selection.start)
            val cursorRect =
                state.layoutResult?.value?.getCursorRect(transformedOffset) ?: Rect(0f, 0f, 0f, 0f)
            val cursorWidth = floor(DefaultCursorThickness.toPx()).coerceAtLeast(1f)
            val cursorX =
                (cursorRect.left + cursorWidth / 2)
                    // Do not use coerceIn because it is not guaranteed that the minimum
                    // value is
                    // smaller than the maximum value.
                    .coerceAtMost(size.width - cursorWidth / 2)
                    .coerceAtLeast(cursorWidth / 2)
                    .let {
                        // When cursor width is odd, draw it in the middle of a pixel,
                        // to avoid blurring due to antialiasing.
                        if (cursorWidth.toInt() % 2 == 1) {
                            floor(it) + 0.5f // round to nearest n+0.5
                        } else round(it)
                    }

            drawLine(
                brush = cursorBrush,
                start = Offset(cursorX, cursorRect.top),
                end = Offset(cursorX, cursorRect.bottom),
                alpha = cursorAlphaValue,
                strokeWidth = cursorWidth,
            )
        }
    }
}

internal expect val DefaultCursorThickness: Dp
