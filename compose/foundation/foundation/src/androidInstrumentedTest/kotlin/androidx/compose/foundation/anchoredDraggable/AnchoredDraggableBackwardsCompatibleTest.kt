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

package androidx.compose.foundation.anchoredDraggable

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableMinFlingVelocity
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.anchoredDraggableFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import org.junit.Rule

/**
 * Test helper that allows to test either old or new anchored draggable overloads before/after
 * aosp/3012013.
 */
abstract class AnchoredDraggableBackwardsCompatibleTest(private val testNewBehavior: Boolean) {

    @get:Rule val rule = createComposeRule()

    fun <T> createStateAndModifier(
        initialValue: T,
        orientation: Orientation,
        anchors: DraggableAnchors<T>? = null,
        confirmValueChange: (T) -> Boolean = { true },
        enabled: Boolean = true,
        reverseDirection: Boolean? = null,
        interactionSource: MutableInteractionSource? = null,
        overscrollEffect: OverscrollEffect? = null,
        startDragImmediately: Boolean? = null,
        positionalThreshold: (Float) -> Float = AnchoredDraggableDefaults.PositionalThreshold,
        velocityThreshold: () -> Float = {
            with(rule.density) { AnchoredDraggableMinFlingVelocity.toPx() }
        },
        snapAnimationSpec: AnimationSpec<Float> = AnchoredDraggableDefaults.SnapAnimationSpec,
        decayAnimationSpec: DecayAnimationSpec<Float> =
            AnchoredDraggableDefaults.DecayAnimationSpec,
        shouldCreateFling: Boolean = true
    ): Pair<AnchoredDraggableState<T>, Modifier> {
        val state =
            createAnchoredDraggableState(
                initialValue = initialValue,
                anchors = anchors,
                confirmValueChange = confirmValueChange,
                positionalThreshold = positionalThreshold,
                velocityThreshold = velocityThreshold,
                snapAnimationSpec = snapAnimationSpec,
                decayAnimationSpec = decayAnimationSpec
            )
        val modifier =
            if (testNewBehavior) {
                val flingBehavior =
                    if (shouldCreateFling) {
                        anchoredDraggableFlingBehavior(
                            state,
                            density = rule.density,
                            positionalThreshold = positionalThreshold,
                            snapAnimationSpec = snapAnimationSpec
                        )
                    } else {
                        null
                    }
                createAnchoredDraggableModifier(
                    state = state,
                    reverseDirection = reverseDirection,
                    orientation = orientation,
                    enabled = enabled,
                    interactionSource = interactionSource,
                    overscrollEffect = overscrollEffect,
                    startDragImmediately = startDragImmediately,
                    flingBehavior = flingBehavior
                )
            } else {
                createAnchoredDraggableModifier(
                    state = state,
                    reverseDirection = reverseDirection,
                    orientation = orientation,
                    enabled = enabled,
                    interactionSource = interactionSource,
                    overscrollEffect = overscrollEffect,
                    startDragImmediately = startDragImmediately,
                    flingBehavior = null
                )
            }
        return state to modifier
    }

    fun <T> createAnchoredDraggableState(
        initialValue: T,
        anchors: DraggableAnchors<T>? = null,
        confirmValueChange: (T) -> Boolean = { true },
        positionalThreshold: (Float) -> Float = AnchoredDraggableDefaults.PositionalThreshold,
        velocityThreshold: () -> Float = {
            with(rule.density) { AnchoredDraggableMinFlingVelocity.toPx() }
        },
        snapAnimationSpec: AnimationSpec<Float> = AnchoredDraggableDefaults.SnapAnimationSpec,
        decayAnimationSpec: DecayAnimationSpec<Float> =
            AnchoredDraggableDefaults.DecayAnimationSpec,
    ) =
        if (testNewBehavior) {
            val resolvedVelocityThreshold = velocityThreshold()
            check(
                resolvedVelocityThreshold ==
                    with(rule.density) { AnchoredDraggableMinFlingVelocity.toPx() }
            ) {
                "The velocity threshold resolved to $resolvedVelocityThreshold, but velocity " +
                    "thresholds are not configurable with testNewBehavior=true."
            }
            when (anchors) {
                null ->
                    AnchoredDraggableState(
                        initialValue,
                        confirmValueChange,
                    )
                else -> AnchoredDraggableState(initialValue, anchors, confirmValueChange)
            }
        } else {
            @Suppress("DEPRECATION")
            when (anchors) {
                null ->
                    AnchoredDraggableState(
                        initialValue = initialValue,
                        confirmValueChange = confirmValueChange,
                        positionalThreshold = positionalThreshold,
                        velocityThreshold = velocityThreshold,
                        snapAnimationSpec = snapAnimationSpec,
                        decayAnimationSpec = decayAnimationSpec
                    )
                else ->
                    AnchoredDraggableState(
                        initialValue = initialValue,
                        anchors = anchors,
                        confirmValueChange = confirmValueChange,
                        positionalThreshold = positionalThreshold,
                        velocityThreshold = velocityThreshold,
                        snapAnimationSpec = snapAnimationSpec,
                        decayAnimationSpec = decayAnimationSpec
                    )
            }
        }

    internal fun <T> createAnchoredDraggableModifier(
        state: AnchoredDraggableState<T>,
        orientation: Orientation,
        reverseDirection: Boolean? = null,
        enabled: Boolean = true,
        interactionSource: MutableInteractionSource? = null,
        overscrollEffect: OverscrollEffect? = null,
        startDragImmediately: Boolean? = null,
        flingBehavior: FlingBehavior? = null
    ): Modifier =
        when (reverseDirection) {
            null ->
                when (startDragImmediately) {
                    null ->
                        Modifier.anchoredDraggable(
                            state = state,
                            orientation = orientation,
                            enabled = enabled,
                            interactionSource = interactionSource,
                            overscrollEffect = overscrollEffect,
                            flingBehavior = flingBehavior
                        )
                    else ->
                        @Suppress("DEPRECATION")
                        Modifier.anchoredDraggable(
                            state = state,
                            orientation = orientation,
                            enabled = enabled,
                            interactionSource = interactionSource,
                            overscrollEffect = overscrollEffect,
                            startDragImmediately = startDragImmediately,
                            flingBehavior = flingBehavior
                        )
                }
            else ->
                when (startDragImmediately) {
                    null ->
                        Modifier.anchoredDraggable(
                            state = state,
                            reverseDirection = reverseDirection,
                            orientation = orientation,
                            enabled = enabled,
                            interactionSource = interactionSource,
                            overscrollEffect = overscrollEffect,
                            flingBehavior = flingBehavior
                        )
                    else ->
                        @Suppress("DEPRECATION")
                        Modifier.anchoredDraggable(
                            state = state,
                            reverseDirection = reverseDirection,
                            orientation = orientation,
                            enabled = enabled,
                            interactionSource = interactionSource,
                            overscrollEffect = overscrollEffect,
                            startDragImmediately = startDragImmediately,
                            flingBehavior = flingBehavior
                        )
                }
        }

    /**
     * Create a [FlingBehavior] with either the old or new behavior, depending on [testNewBehavior].
     *
     * @return A [anchoredDraggableFlingBehavior] instance or a [TargetedFlingBehavior] instance
     *   that calls the deprecated [AnchoredDraggableState.settle] overload.
     */
    internal fun <T> createAnchoredDraggableFlingBehavior(
        state: AnchoredDraggableState<T>,
        density: Density,
        positionalThreshold: (totalDistance: Float) -> Float =
            AnchoredDraggableDefaults.PositionalThreshold,
        snapAnimationSpec: AnimationSpec<Float> = AnchoredDraggableDefaults.SnapAnimationSpec
    ) =
        if (testNewBehavior) {
            anchoredDraggableFlingBehavior(
                state = state,
                density = density,
                positionalThreshold = positionalThreshold,
                snapAnimationSpec = snapAnimationSpec
            )
        } else {
            object : TargetedFlingBehavior {
                override suspend fun ScrollScope.performFling(
                    initialVelocity: Float,
                    onRemainingDistanceUpdated: (Float) -> Unit
                ): Float {
                    @Suppress("DEPRECATION") return state.settle(initialVelocity)
                }
            }
        }

    internal suspend inline fun performFling(
        flingBehavior: FlingBehavior,
        state: AnchoredDraggableState<*>,
        velocity: Float
    ) {
        if (testNewBehavior) {
            with(flingBehavior) {
                state.anchoredDrag { this.asScrollScope(state).performFling(velocity) }
            }
        } else {
            @Suppress("DEPRECATION") state.settle(velocity)
        }
    }

    internal val AnchoredDraggableMinFlingVelocityPx: Float
        get() = with(rule.density) { AnchoredDraggableMinFlingVelocity.toPx() }
}
