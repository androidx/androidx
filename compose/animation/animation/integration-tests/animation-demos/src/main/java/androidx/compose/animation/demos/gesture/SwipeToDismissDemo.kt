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

package androidx.compose.animation.demos.gesture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Preview
@Composable
fun SwipeToDismissDemo() {
    Column {
        var index by remember { mutableIntStateOf(0) }
        Box(Modifier.requiredHeight(300.dp).fillMaxWidth()) {
            Box(
                Modifier.swipeToDismiss(index)
                    .align(Alignment.BottomCenter)
                    .requiredSize(150.dp)
                    .background(pastelColors[index])
            )
        }
        Text(
            "Swipe up to dismiss",
            fontSize = 30.sp,
            modifier = Modifier.padding(40.dp).align(Alignment.CenterHorizontally),
        )
        Button(
            onClick = { index = (index + 1) % pastelColors.size },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("New Card")
        }
    }
}

private fun Modifier.swipeToDismiss(index: Int): Modifier = this then SwipeToDismissElement(index)

private data class SwipeToDismissElement(val index: Int) :
    ModifierNodeElement<SwipeToDismissNode>() {
    override fun InspectorInfo.inspectableProperties() {
        name = "swipeToDismiss"
    }

    override fun create() = SwipeToDismissNode(index)

    override fun update(node: SwipeToDismissNode) {
        node.update(index)
    }
}

private class SwipeToDismissNode(private var index: Int) : DelegatingNode(), LayoutModifierNode {
    private lateinit var animatedOffset: Animatable<Float, AnimationVector1D>

    init {
        delegate(SuspendingPointerInputModifierNode { detectSwipeToDismiss(animatedOffset) })
    }

    override fun onAttach() {
        animatedOffset = Animatable(0f)
    }

    fun update(index: Int) {
        if (this.index != index) {
            this.index = index
            if (isAttached) {
                coroutineScope.launch { animatedOffset.snapTo(0f) }
            }
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val offset = animatedOffset.value
            placeable.placeWithLayer(0, offset.roundToInt()) {
                alpha = calculateAlpha(offset, placeable.height)
            }
        }
    }
}

private suspend fun PointerInputScope.detectSwipeToDismiss(
    animatedOffset: Animatable<Float, AnimationVector1D>
) {
    val density: Density = this
    coroutineScope {
        while (true) {
            val velocityTracker = VelocityTracker()
            awaitPointerEventScope {
                val pointerId = awaitFirstDown().id
                verticalDrag(pointerId) {
                    launch { animatedOffset.snapTo(animatedOffset.value + it.positionChange().y) }
                    velocityTracker.addPosition(it.uptimeMillis, it.position)
                }
            }
            val velocity = velocityTracker.calculateVelocity().y
            launch {
                // Either fling out of the sight, or snap back
                val decay = splineBasedDecay<Float>(density)
                if (decay.calculateTargetValue(animatedOffset.value, velocity) >= -size.height) {
                    // Not enough velocity to be dismissed
                    animatedOffset.animateTo(0f, initialVelocity = velocity)
                } else {
                    animatedOffset.updateBounds(lowerBound = -size.height.toFloat())
                    animatedOffset.animateDecay(velocity, decay)
                }
            }
        }
    }
}

private fun calculateAlpha(offset: Float, size: Int): Float {
    if (size <= 0) return 1f
    val alpha = (offset + size) / size
    return alpha.coerceIn(0f, 1f)
}

internal val pastelColors =
    listOf(
        Color(0xFFffd7d7),
        Color(0xFFffe9d6),
        Color(0xFFfffbd0),
        Color(0xFFe3ffd9),
        Color(0xFFd0fff8),
    )
