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

package androidx.xr.compose.subspace.animation

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector3D
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.createChildTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastRoundToInt
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.animation.SpatialTransitionDefaults.DefaultAlphaAnimationSpec
import androidx.xr.compose.subspace.animation.SpatialTransitionDefaults.DefaultSlideAnimationSpec
import androidx.xr.compose.subspace.draw.alpha
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceMeasurable
import androidx.xr.compose.subspace.layout.SubspaceMeasurePolicy
import androidx.xr.compose.subspace.layout.SubspaceMeasureResult
import androidx.xr.compose.subspace.layout.SubspaceMeasureScope
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.layout
import androidx.xr.compose.unit.IntVolumeOffset
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import kotlin.math.max

@Composable
@SubspaceComposable
internal fun <T> AnimatedSpatialVisibilityImpl(
    transition: Transition<T>,
    visible: (T) -> Boolean,
    modifier: SubspaceModifier,
    enter: SpatialEnterTransition,
    exit: SpatialExitTransition,
    content: @Composable @SubspaceComposable AnimatedSpatialVisibilityScope.() -> Unit,
) {
    AnimatedSpatialEnterExitImpl(
        transition = transition,
        visible = visible,
        modifier =
            modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val size =
                    if (!visible(transition.targetState)) {
                        IntVolumeSize.Zero
                    } else {
                        IntVolumeSize(
                            placeable.measuredWidth,
                            placeable.measuredHeight,
                            placeable.measuredDepth,
                        )
                    }
                layout(size.width, size.height, size.depth) { placeable.place(Pose.Identity) }
            },
        enter = enter,
        exit = exit,
        shouldDisposeBlock = { current, target ->
            current == target && target == EnterExitState.PostExit
        },
        content = content,
    )
}

@OptIn(ExperimentalTransitionApi::class)
@Composable
@SubspaceComposable
private fun <T> AnimatedSpatialEnterExitImpl(
    transition: Transition<T>,
    visible: (T) -> Boolean,
    modifier: SubspaceModifier,
    enter: SpatialEnterTransition,
    exit: SpatialExitTransition,
    shouldDisposeBlock: (EnterExitState, EnterExitState) -> Boolean,
    content: @Composable @SubspaceComposable AnimatedSpatialVisibilityScope.() -> Unit,
) {
    // TODO(kmost): The original implementation of this in AnimatedEnterExitImpl also checks
    //  `transition.isSeeking` and `transition.hasInitialValueAnimations`, but these are internal
    //  APIs, so we can't access them.
    if (visible(transition.targetState) || visible(transition.currentState)) {
        val childTransition =
            transition.createChildTransition(label = "SpatialEnterExitTransition") {
                transition.targetEnterExit(visible, it)
            }

        val shouldDisposeBlockUpdated by rememberUpdatedState(shouldDisposeBlock)

        val shouldDisposeAfterExit by
            produceState(
                initialValue =
                    shouldDisposeBlock(childTransition.currentState, childTransition.targetState)
            ) {
                snapshotFlow { childTransition.exitFinished }
                    .collect {
                        value =
                            if (it) {
                                shouldDisposeBlockUpdated(
                                    childTransition.currentState,
                                    childTransition.targetState,
                                )
                            } else {
                                false
                            }
                    }
            }
        if (!childTransition.exitFinished || !shouldDisposeAfterExit) {
            val scope = remember(transition) { AnimatedSpatialVisibilityScope(childTransition) }
            SubspaceLayout(
                modifier = modifier.then(childTransition.createModifier(enter, exit)),
                content = { scope.content() },
                measurePolicy = remember { AnimatedSpatialEnterExitMeasurePolicy() },
            )
        }
    }
}

private class AnimatedSpatialEnterExitMeasurePolicy() : SubspaceMeasurePolicy {
    override fun SubspaceMeasureScope.measure(
        measurables: List<SubspaceMeasurable>,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        var maxWidth = 0
        var maxHeight = 0
        var maxDepth = 0
        val placeables =
            measurables.fastMap {
                it.measure(constraints).apply {
                    maxWidth = max(maxWidth, measuredWidth)
                    maxHeight = max(maxHeight, measuredHeight)
                    maxDepth = max(maxDepth, measuredDepth)
                }
            }
        return layout(maxWidth, maxHeight, maxDepth) {
            placeables.fastForEach { it.place(Pose.Identity) }
        }
    }
}

@Composable
internal fun Transition<EnterExitState>.createModifier(
    enter: SpatialEnterTransition,
    exit: SpatialExitTransition,
): SubspaceModifier {
    return fadeAnimationModifier(enter, exit).then(slideAnimationModifier(enter, exit))
}

@Composable
private fun Transition<EnterExitState>.fadeAnimationModifier(
    activeEnter: SpatialEnterTransition,
    activeExit: SpatialExitTransition,
): SubspaceModifier {
    // No fade animations
    if (activeEnter.data.fade == null && activeExit.data.fade == null) return SubspaceModifier

    val alphaSpec: @Composable Transition.Segment<EnterExitState>.() -> FiniteAnimationSpec<Float> =
        {
            when {
                EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible -> {
                    activeEnter.data.fade?.animationSpec ?: DefaultAlphaAnimationSpec
                }
                EnterExitState.Visible isTransitioningTo EnterExitState.PostExit -> {
                    activeExit.data.fade?.animationSpec ?: DefaultAlphaAnimationSpec
                }
                else -> DefaultAlphaAnimationSpec
            }
        }
    val animationLabel = remember { "$label alpha" }
    val alpha by
        animateFloat(alphaSpec, label = animationLabel) { targetState ->
            when (targetState) {
                EnterExitState.Visible -> 1f
                EnterExitState.PreEnter -> activeEnter.data.fade?.alpha ?: 1f
                EnterExitState.PostExit -> activeExit.data.fade?.alpha ?: 1f
            }
        }
    return SubspaceModifier.alpha(alpha)
}

@Composable
private fun Transition<EnterExitState>.slideAnimationModifier(
    activeEnter: SpatialEnterTransition,
    activeExit: SpatialExitTransition,
): SubspaceModifier {
    // No slide animations
    if (activeEnter.data.slide == null && activeExit.data.slide == null) return SubspaceModifier

    val offset = remember { Animatable(IntVolumeOffset.Zero, IntVolumeOffsetToVector) }

    val animationSpec =
        with(segment) {
            when {
                EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible ->
                    activeEnter.data.slide?.animationSpec ?: DefaultSlideAnimationSpec
                EnterExitState.Visible isTransitioningTo EnterExitState.PostExit ->
                    activeExit.data.slide?.animationSpec ?: DefaultSlideAnimationSpec
                else -> DefaultSlideAnimationSpec
            }
        }

    val density = LocalDensity.current
    fun EnterExitState.calculateOffset(fullSize: IntVolumeSize): IntVolumeOffset {
        return when (this) {
            EnterExitState.PreEnter ->
                activeEnter.data.slide?.slideOffset?.invoke(density, fullSize)
                    ?: IntVolumeOffset.Zero
            EnterExitState.Visible -> IntVolumeOffset.Zero
            EnterExitState.PostExit ->
                activeExit.data.slide?.slideOffset?.invoke(density, fullSize)
                    ?: IntVolumeOffset.Zero
        }
    }

    // This will be updated by the layout modifier below, and is necessary to compute offsets.
    var fullSize: IntVolumeSize by remember { mutableStateOf(IntVolumeSize.Zero) }
    var isInitialSnapToFinished by remember { mutableStateOf(false) }

    // When the size, target state, or animationSpec changes, animate towards the new target offset.
    LaunchedEffect(fullSize, targetState, animationSpec) {
        // Ideally, the offset would just be initialized via fullSize, but fullSize is not known
        // until layout time, so we need to defer this snapTo.
        // We also don't want to snapTo more than once, as it will result in the animation
        // restarting in odd ways if the developer changes the layout's size.
        if (!isInitialSnapToFinished) {
            isInitialSnapToFinished = true
            offset.snapTo(currentState.calculateOffset(fullSize))
        }
        offset.animateTo(targetState.calculateOffset(fullSize), animationSpec)
    }

    return SubspaceModifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        // Size can only be determined in a custom layout modifier
        fullSize =
            IntVolumeSize(
                placeable.measuredWidth,
                placeable.measuredHeight,
                placeable.measuredDepth,
            )
        layout(placeable.measuredWidth, placeable.measuredHeight, placeable.measuredDepth) {
            placeable.place(
                Pose(
                    // Place this layout at the offset calculated by our animation
                    Vector3(
                        offset.value.x.toFloat(),
                        offset.value.y.toFloat(),
                        offset.value.z.toFloat(),
                    )
                )
            )
        }
    }
}

// This converts Boolean visible to EnterExitState
@Composable
private fun <T> Transition<T>.targetEnterExit(
    visible: (T) -> Boolean,
    targetState: T,
): EnterExitState =
    key(this) {
        val hasBeenVisible = remember { mutableStateOf(false) }
        if (visible(currentState)) {
            hasBeenVisible.value = true
        }
        if (visible(targetState)) {
            EnterExitState.Visible
        } else {
            // If never been visible, visible = false means PreEnter, otherwise PostExit
            if (hasBeenVisible.value) {
                EnterExitState.PostExit
            } else {
                EnterExitState.PreEnter
            }
        }
    }

private val Transition<EnterExitState>.exitFinished
    get() = currentState == EnterExitState.PostExit && targetState == EnterExitState.PostExit

private val IntVolumeOffsetToVector: TwoWayConverter<IntVolumeOffset, AnimationVector3D> =
    TwoWayConverter(
        { AnimationVector3D(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) },
        { IntVolumeOffset(it.v1.fastRoundToInt(), it.v2.fastRoundToInt(), it.v3.fastRoundToInt()) },
    )
