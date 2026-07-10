/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.stack

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerEventType
import androidx.compose.ui.input.indirect.IndirectPointerInputModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Draws a gradient scrim at the top edge of the node that fades in on press and fades out on
 * release.
 */
internal fun Modifier.stackScrim() =
    graphicsLayer {
        // Offscreen composition strategy is used because the scrim below uses DstOut blend mode,
        // which effectively cuts out a portion of this layer's content to reveal what is behind it.
        compositingStrategy = CompositingStrategy.Offscreen
    } then StackScrimElement

private object StackScrimElement : ModifierNodeElement<StackScrimNode>() {

    override fun create() = StackScrimNode()

    override fun update(node: StackScrimNode) {}

    override fun equals(other: Any?) = other is StackScrimElement

    override fun hashCode() = javaClass.hashCode()

    override fun InspectorInfo.inspectableProperties() {
        name = "stackScrim"
    }
}

private class StackScrimNode :
    Modifier.Node(), PointerInputModifierNode, IndirectPointerInputModifierNode, DrawModifierNode {

    private var animatedAlpha: Animatable<Float, AnimationVector1D>? = null
    private var pointerCount = 0
    private var indirectPointerCount = 0
    private val isPressed: Boolean
        get() = pointerCount > 0 || indirectPointerCount > 0

    private var brush: Brush? = null
    private var lastDensity: Density? = null

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (pass == PointerEventPass.Initial) {
            when (pointerEvent.type) {
                PointerEventType.Press -> pointerCount++
                PointerEventType.Release -> pointerCount--
            }
            updateAnimationStateAndInvalidate()
        }
    }

    override fun onCancelPointerInput() {
        pointerCount = 0
        updateAnimationStateAndInvalidate()
    }

    override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
        if (pass == PointerEventPass.Initial) {
            when (event.type) {
                IndirectPointerEventType.Press -> indirectPointerCount++
                IndirectPointerEventType.Release -> indirectPointerCount--
            }
            updateAnimationStateAndInvalidate()
        }
    }

    override fun onCancelIndirectPointerInput() {
        indirectPointerCount = 0
        updateAnimationStateAndInvalidate()
    }

    override fun ContentDrawScope.draw() {
        // Draw the original content first.
        drawContent()

        // If the scrim is fully transparent, no need to draw it.
        if (animatedAlpha == null || animatedAlpha?.value == 0f) return

        val scrimHeightPx = ScrimHeight.toPx()

        if (brush == null || lastDensity != this@draw) {
            lastDensity = this@draw
            brush =
                Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = 0f,
                    endY = scrimHeightPx,
                )
        }

        brush?.let {
            drawRect(
                brush = it,
                blendMode = BlendMode.DstOut,
                alpha = animatedAlpha!!.value,
                size = size.copy(height = scrimHeightPx),
            )
        }
    }

    override fun onDetach() {
        animatedAlpha = null
        pointerCount = 0
        indirectPointerCount = 0
    }

    private fun updateAnimationStateAndInvalidate() {
        val targetAlpha = if (isPressed) 1f else 0f

        if (animatedAlpha == null) {
            animatedAlpha = Animatable(0f)
        }
        val alpha = animatedAlpha!!

        // Animate only if the target value is different from the current one.
        if (alpha.targetValue != targetAlpha) {
            coroutineScope.launch {
                val animationSpec =
                    if (targetAlpha == 1f) ScrimFadeInAnimationSpec else ScrimFadeOutAnimationSpec
                alpha.animateTo(targetAlpha, animationSpec)
            }
        }

        invalidateDraw()
    }
}

private val ScrimHeight = 48.dp
private val ScrimFadeInAnimationSpec: AnimationSpec<Float> = tween(200)
private val ScrimFadeOutAnimationSpec: AnimationSpec<Float> = tween(1500)
