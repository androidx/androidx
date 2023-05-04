/*
 * Copyright 2022 The Android Open Source Project
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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.intermediateLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import kotlinx.coroutines.launch

@Sampled
@Composable
fun IntermediateLayoutSample() {
    // Creates a custom modifier that animates the constraints and measures child with the
    // animated constraints. This modifier is built on top of `Modifier.intermediateLayout`, which
    // allows access to the lookahead size of the layout. A resize animation will be kicked off
    // whenever the lookahead size changes, to animate children from current size to lookahead size.
    // Fixed constraints created based on the animation value will be used to measure
    // child, so the child layout gradually changes its size and potentially its child's placement
    // to fit within the animated constraints.
    fun Modifier.animateConstraints() = composed {
        // Creates a size animation
        var sizeAnimation: Animatable<IntSize, AnimationVector2D>? by remember {
            mutableStateOf(null)
        }

        this.intermediateLayout { measurable, _ ->
            // When layout changes, the lookahead pass will calculate a new final size for the
            // child layout. This lookahead size can be used to animate the size
            // change, such that the animation starts from the current size and gradually
            // change towards `lookaheadSize`.
            if (lookaheadSize != sizeAnimation?.targetValue) {
                sizeAnimation?.run {
                    launch { animateTo(lookaheadSize) }
                } ?: Animatable(lookaheadSize, IntSize.VectorConverter).let {
                    sizeAnimation = it
                }
            }
            val (width, height) = sizeAnimation!!.value
            // Creates a fixed set of constraints using the animated size
            val animatedConstraints = Constraints.fixed(width, height)
            // Measure child with animated constraints.
            val placeable = measurable.measure(animatedConstraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }

    var fullWidth by remember { mutableStateOf(false) }
    Row(
        (if (fullWidth) Modifier.fillMaxWidth() else Modifier.width(100.dp))
            .height(200.dp)
            // Use the custom modifier created above to animate the constraints passed
            // to the child, and therefore resize children in an animation.
            .animateConstraints()
            .clickable { fullWidth = !fullWidth }) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.Red)
        )
        Box(
            Modifier
                .weight(2f)
                .fillMaxHeight()
                .background(Color.Yellow)
        )
    }
}

@Sampled
@Composable
fun LookaheadLayoutCoordinatesSample() {
    // Creates a custom modifier to animate the local position of the layout within the
    // given LookaheadScope, whenever the relative position changes.
    fun Modifier.animatePlacementInScope(lookaheadScope: LookaheadScope) = composed {
        // Creates an offset animation
        var offsetAnimation: Animatable<IntOffset, AnimationVector2D>? by mutableStateOf(
            null
        )
        var targetOffset: IntOffset? by mutableStateOf(null)

        this.intermediateLayout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                // Converts coordinates of the current layout to LookaheadCoordinates
                val coordinates = coordinates
                if (coordinates != null) {
                    // Calculates the target offset within the lookaheadScope
                    val target = with(lookaheadScope) {
                        lookaheadScopeCoordinates
                            .localLookaheadPositionOf(coordinates)
                            .round().also { targetOffset = it }
                    }

                    // Uses the target offset to start an offset animation
                    if (target != offsetAnimation?.targetValue) {
                        offsetAnimation?.run {
                            launch { animateTo(target) }
                        } ?: Animatable(target, IntOffset.VectorConverter).let {
                            offsetAnimation = it
                        }
                    }
                    // Calculates the *current* offset within the given LookaheadScope
                    val placementOffset =
                        lookaheadScopeCoordinates.localPositionOf(
                            coordinates,
                            Offset.Zero
                        ).round()
                    // Calculates the delta between animated position in scope and current
                    // position in scope, and places the child at the delta offset. This puts
                    // the child layout at the animated position.
                    val (x, y) = requireNotNull(offsetAnimation).run { value - placementOffset }
                    placeable.place(x, y)
                } else {
                    placeable.place(0, 0)
                }
            }
        }
    }

    val colors = listOf(
        Color(0xffff6f69), Color(0xffffcc5c), Color(0xff264653), Color(0xff2a9d84)
    )

    var isInColumn by remember { mutableStateOf(true) }
    LookaheadScope {
        // Creates movable content containing 4 boxes. They will be put either in a [Row] or in a
        // [Column] depending on the state
        val items = remember {
            movableContentOf {
                colors.forEach { color ->
                    Box(
                        Modifier
                            .padding(15.dp)
                            .size(100.dp, 80.dp)
                            .animatePlacementInScope(this)
                            .background(color, RoundedCornerShape(20))
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { isInColumn = !isInColumn }
        ) {
            // As the items get moved between Column and Row, their positions in LookaheadLayout
            // will change. The `animatePlacementInScope` modifier created above will
            // observe that final position change via `localLookaheadPositionOf`, and create
            // a position animation.
            if (isInColumn) {
                Column(Modifier.fillMaxSize()) {
                    items()
                }
            } else {
                Row { items() }
            }
        }
    }
}

@Sampled
@Composable
fun animateContentSizeAfterLookaheadPass() {
    var sizeAnim by remember {
        mutableStateOf<Animatable<IntSize, AnimationVector2D>?>(null)
    }
    var lookaheadSize by remember {
        mutableStateOf<IntSize?>(null)
    }
    val coroutineScope = rememberCoroutineScope()
    LookaheadScope {
        // The Box is in a LookaheadScope. This means there will be a lookahead measure pass
        // before the main measure pass.
        // Here we are creating something similar to the `animateContentSize` modifier.
        Box(
            Modifier
                .clipToBounds()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)

                    val measuredSize = IntSize(placeable.width, placeable.height)
                    val (width, height) = if (isLookingAhead) {
                        // Record lookahead size if we are in lookahead pass. This lookahead size
                        // will be used for size animation, such that the main measure pass will
                        // gradually change size until it reaches the lookahead size.
                        lookaheadSize = measuredSize
                        measuredSize
                    } else {
                        // Since we are in an explicit lookaheadScope, we know the lookahead pass
                        // is guaranteed to happen, therefore the lookahead size that we recorded is
                        // not null.
                        val target = requireNotNull(lookaheadSize)
                        val anim = sizeAnim?.also {
                            coroutineScope.launch { it.animateTo(target) }
                        } ?: Animatable(target, IntSize.VectorConverter)
                        sizeAnim = anim
                        // By returning the animated size only during main pass, we are allowing
                        // lookahead pass to see the future layout past the animation.
                        anim.value
                    }

                    layout(width, height) {
                        placeable.place(0, 0)
                    }
                }) {
            // Some content that changes size
        }
    }
}
