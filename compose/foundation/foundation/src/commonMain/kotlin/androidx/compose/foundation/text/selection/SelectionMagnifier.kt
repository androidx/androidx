/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val UnspecifiedAnimationVector2D = AnimationVector2D(Float.NaN, Float.NaN)

/** Like `Offset.VectorConverter` but propagates [Offset.Unspecified] values. */
internal val UnspecifiedSafeOffsetVectorConverter =
    TwoWayConverter<Offset, AnimationVector2D>(
        convertToVector = {
            if (it.isSpecified) {
                AnimationVector2D(it.x, it.y)
            } else {
                UnspecifiedAnimationVector2D
            }
        },
        convertFromVector = { Offset(it.v1, it.v2) },
    )

internal val OffsetDisplacementThreshold =
    Offset(Spring.DefaultDisplacementThreshold, Spring.DefaultDisplacementThreshold)

internal val MagnifierSpringSpec = SpringSpec(visibilityThreshold = OffsetDisplacementThreshold)

/**
 * The text magnifier follows horizontal dragging exactly, but is vertically clamped to the current
 * line, so when it changes lines we animate it.
 */
internal fun Modifier.animatedSelectionMagnifier(
    magnifierCenter: (magnifierSize: IntSize) -> Offset,
    platformMagnifier:
        DelegatableNode.(
            animatedCenter: () -> Offset,
            onSizeChanged: (IntSize) -> Unit,
        ) -> DelegatableNode,
): Modifier =
    this then
        AnimatedSelectionMagnifierElement(
            magnifierCenter = magnifierCenter,
            platformMagnifier = platformMagnifier,
        )

internal fun Modifier.animatedSelectionMagnifierLegacy(
    magnifierCenter: () -> Offset,
    platformMagnifier: (animatedCenter: () -> Offset) -> Modifier,
): Modifier = composed {
    val animatedCenter by rememberAnimatedMagnifierPosition(targetCalculation = magnifierCenter)
    return@composed platformMagnifier { animatedCenter }
}

@Composable
private fun rememberAnimatedMagnifierPosition(targetCalculation: () -> Offset): State<Offset> {
    val targetValue by remember { derivedStateOf(targetCalculation) }
    val animatable = remember {
        // Can't use Offset.VectorConverter because we need to handle Unspecified specially.
        Animatable(targetValue, UnspecifiedSafeOffsetVectorConverter, OffsetDisplacementThreshold)
    }
    LaunchedEffect(Unit) {
        val animationScope = this
        snapshotFlow { targetValue }
            .collect { targetValue ->
                // Only animate the position when moving vertically (i.e. jumping between lines),
                // since horizontal movement in a single line should stay as close to the gesture as
                // possible and animation would only add unnecessary lag.
                if (
                    animatable.value.isSpecified &&
                        targetValue.isSpecified &&
                        animatable.value.y != targetValue.y
                ) {
                    // Launch the animation, instead of cancelling and re-starting manually via
                    // collectLatest, so if another animation is started before this one finishes,
                    // the new one will use the correct velocity, e.g. in order to propagate spring
                    // inertia.
                    animationScope.launch { animatable.animateTo(targetValue, MagnifierSpringSpec) }
                } else {
                    animatable.snapTo(targetValue)
                }
            }
    }
    return animatable.asState()
}

private class AnimatedSelectionMagnifierElement(
    private val magnifierCenter: (magnifierSize: IntSize) -> Offset,
    private val platformMagnifier:
        DelegatableNode.(
            animatedCenter: () -> Offset,
            onSizeChanged: (IntSize) -> Unit,
        ) -> DelegatableNode,
) : ModifierNodeElement<AnimatedSelectionMagnifierNode>() {
    override fun create(): AnimatedSelectionMagnifierNode =
        AnimatedSelectionMagnifierNode(
            magnifierCenter = magnifierCenter,
            platformMagnifier = platformMagnifier,
        )

    override fun update(node: AnimatedSelectionMagnifierNode) {
        node.update(magnifierCenter = magnifierCenter, platformMagnifier = platformMagnifier)
    }

    override fun InspectorInfo.inspectableProperties() {
        // The underlying magnifier modifier provides more meaningful inspector info.
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimatedSelectionMagnifierElement) return false
        if (magnifierCenter !== other.magnifierCenter) return false
        if (platformMagnifier !== other.platformMagnifier) return false
        return true
    }

    override fun hashCode(): Int {
        var result = magnifierCenter.hashCode()
        result = 31 * result + platformMagnifier.hashCode()
        return result
    }
}

private class AnimatedSelectionMagnifierNode(
    magnifierCenter: (magnifierSize: IntSize) -> Offset,
    private var platformMagnifier:
        DelegatableNode.(
            animatedCenter: () -> Offset,
            onSizeChanged: (IntSize) -> Unit,
        ) -> DelegatableNode,
) : DelegatingNode() {
    private var magnifierSize: IntSize by mutableStateOf(IntSize.Zero)
    private var magnifierCenter by mutableStateOf(magnifierCenter)
    private val targetValue by derivedStateOf { this.magnifierCenter(magnifierSize) }

    // Can't use Offset.VectorConverter because we need to handle Unspecified specially.
    private val animatable =
        Animatable(targetValue, UnspecifiedSafeOffsetVectorConverter, OffsetDisplacementThreshold)

    private var magnifierNode: DelegatableNode =
        delegate(platformMagnifier({ animatable.value }, { size -> magnifierSize = size }))

    private var animationJob: Job? = null

    override fun onAttach() {
        startAnimationJob()
    }

    fun update(
        magnifierCenter: (magnifierSize: IntSize) -> Offset,
        platformMagnifier:
            DelegatableNode.(
                animatedCenter: () -> Offset,
                onSizeChanged: (IntSize) -> Unit,
            ) -> DelegatableNode,
    ) {
        this.magnifierCenter = magnifierCenter
        if (this.platformMagnifier !== platformMagnifier) {
            this.platformMagnifier = platformMagnifier
            undelegate(magnifierNode)
            magnifierNode =
                delegate(platformMagnifier({ animatable.value }, { size -> magnifierSize = size }))
        }
    }

    private fun startAnimationJob() {
        animationJob?.cancel()
        animationJob = coroutineScope.launch {
            val animationScope = this
            snapshotFlow { targetValue }
                .collect { targetValue ->
                    // Only animate the position when moving vertically (i.e. jumping between
                    // lines),
                    // since horizontal movement in a single line should stay as close to the
                    // gesture as
                    // possible and animation would only add unnecessary lag.
                    if (
                        animatable.value.isSpecified &&
                            targetValue.isSpecified &&
                            animatable.value.y != targetValue.y
                    ) {
                        // Launch the animation, instead of cancelling and re-starting manually via
                        // collectLatest, so if another animation is started before this one
                        // finishes,
                        // the new one will use the correct velocity, e.g. in order to propagate
                        // spring
                        // inertia.
                        animationScope.launch {
                            animatable.animateTo(targetValue, MagnifierSpringSpec)
                        }
                    } else {
                        animatable.snapTo(targetValue)
                    }
                }
        }
    }
}
