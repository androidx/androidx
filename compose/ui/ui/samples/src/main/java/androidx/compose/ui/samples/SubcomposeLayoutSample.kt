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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.composed
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.intermediateLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Sampled
@Composable
fun SubcomposeLayoutSample(
    mainContent: @Composable () -> Unit,
    dependentContent: @Composable (IntSize) -> Unit
) {
    // enum class SlotsEnum { Main, Dependent }
    SubcomposeLayout { constraints ->
        val mainPlaceables = subcompose(SlotsEnum.Main, mainContent).map {
            it.measure(constraints)
        }
        val maxSize = mainPlaceables.fold(IntSize.Zero) { currentMax, placeable ->
            IntSize(
                width = maxOf(currentMax.width, placeable.width),
                height = maxOf(currentMax.height, placeable.height)
            )
        }
        layout(maxSize.width, maxSize.height) {
            mainPlaceables.forEach { it.placeRelative(0, 0) }
            subcompose(SlotsEnum.Dependent) {
                dependentContent(maxSize)
            }.forEach {
                it.measure(constraints).placeRelative(0, 0)
            }
        }
    }
}

enum class SlotsEnum { Main, Dependent }

@OptIn(ExperimentalComposeUiApi::class)
@Sampled
fun SubcomposeLayoutWithIntermediateMeasurePolicySample() {
    // In this example, there is a custom modifier that animates the constraints and measures
    // child with the animated constraints, as defined below.
    // This modifier is built on top of `Modifier.intermediateLayout`, which
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

    // In the example below, the SubcomposeLayout has a parent layout that animates its width
    // between two fixed sizes using the `animateConstraints` modifier we created above.
    @Composable
    fun SubcomposeLayoutWithAnimatingParentLayout(
        isWide: Boolean,
        modifier: Modifier = Modifier,
        content: @Composable @UiComposable () -> Unit
    ) {
        // Create a MeasurePolicy to measure all children with incoming constraints and return the
        // largest width & height.
        val myMeasurePolicy = MeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val maxWidth = placeables.maxOf { it.width }
            val maxHeight = placeables.maxOf { it.height }
            layout(maxWidth, maxHeight) {
                placeables.forEach { it.place(0, 0) }
            }
        }
        Box(
            Modifier
                .requiredSize(if (isWide) 400.dp else 200.dp)
                .animateConstraints()
        ) {
            // SubcomposeLayout's measurePolicy will only be invoked with lookahead constraints.
            // The parent layout in this example is animating between two fixed widths. The
            // [measurePolicy] parameter will only be called with lookahead constraints
            // (i.e. constraints for 400.dp x 400.dp or 200.dp x 200.dp depending on the state.)
            // This may cause content lambda to jump to its final size. To create a smooth
            // experience, we need to remeasure the content with the intermediate
            // constraints created by the `animateConstraints` that we built above. Therefore, we
            // need to provide a [intermediateMeasurePolicy] to define how to measure the
            // content (using the measureables of the content that was composed in [measurePolicy])
            // with intermediate constraints.
            SubcomposeLayout(
                modifier,
                intermediateMeasurePolicy = { intermediateConstraints ->
                    // Retrieve the measureables for slotId = Unit, and measure them with
                    // intermediate constraints using the measurePolicy we created above.
                    with(myMeasurePolicy) {
                        measure(
                            measurablesForSlot(Unit),
                            intermediateConstraints
                        )
                    }
                },
                measurePolicy = { constraints ->
                    val measurables = subcompose(Unit) { content() }
                    with(myMeasurePolicy) { measure(measurables, constraints) }
                })
        }
    }
}