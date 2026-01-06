/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.compose.animation.scene.mechanics

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import com.android.compose.animation.scene.ContentKey
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.ElementStateScope
import com.android.compose.animation.scene.content.state.TransitionState
import com.android.compose.animation.scene.transformation.CustomPropertyTransformation
import com.android.compose.animation.scene.transformation.PropertyTransformationScope
import com.android.mechanics.MotionValue
import com.android.mechanics.ProvidedGestureContext
import com.android.mechanics.spec.InputDirection
import com.android.mechanics.spec.MotionSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Callback to create a [MotionSpec] on the first call to [CustomPropertyTransformation.transform]
 */
typealias SpecFactory =
    PropertyTransformationScope.(content: ContentKey, element: ElementKey) -> MotionSpec

/** Callback to compute the [MotionValue] per frame */
typealias MotionValueInput =
    PropertyTransformationScope.(progress: Float, content: ContentKey, element: ElementKey) -> Float

/**
 * Adapter to create a [MotionValue] and `keepRunning()` it temporarily while a
 * [CustomPropertyTransformation] is in progress and until the animation settles.
 *
 * The [MotionValue]'s input is by default the transition progress.
 */
internal class TransitionScopedMechanicsAdapter(
    private val computeInput: MotionValueInput = { progress, _, _ -> progress },
    private val stableThreshold: Float = MotionValue.StableThresholdEffect,
    private val label: String? = null,
    private val getSpec: SpecFactory,
) {

    private val input = mutableFloatStateOf(0f)
    private var motionValue: MotionValue? = null
    private var transformationContent: ContentKey? = null
    private var transformationElement: ElementKey? = null

    fun PropertyTransformationScope.update(
        content: ContentKey,
        element: ElementKey,
        transition: TransitionState.Transition,
        transitionScope: CoroutineScope,
    ): Float {
        val progress = transition.progressTo(content)
        input.floatValue = computeInput(progress, content, element)
        var motionValue = motionValue

        if (motionValue == null) {
            transformationContent = content
            transformationElement = element
            motionValue =
                MotionValue(
                    input = { input.floatValue },
                    gestureContext =
                        transition.gestureContext
                            ?: ProvidedGestureContext(
                                0f,
                                appearDirection(content, element, transition),
                            ),
                    spec = derivedStateOf { getSpec(content, element) }::value,
                    label = label,
                    stableThreshold = stableThreshold,
                )

            this@TransitionScopedMechanicsAdapter.motionValue = motionValue

            transitionScope.launch {
                motionValue.keepRunningWhile { !transition.isProgressStable || !isStable }
            }
        } else {
            check(content == transformationContent && element == transformationElement) {
                "update received ($content, $element), " +
                    "instead of ($transformationContent, $transformationElement)"
            }
        }

        return motionValue.output
    }

    companion object {
        /**
         * Computes the InputDirection for a triggered transition of an element appearing /
         * disappearing.
         *
         * Since [CustomPropertyTransformation] are only supported for non-shared elements, the
         * [TransitionScopedMechanicsAdapter] is only used in the context of an element appearing /
         * disappearing. This helper computes the direction to result in [InputDirection.Max] for an
         * appear transition, and [InputDirection.Min] for a disappear transition.
         */
        @VisibleForTesting
        internal fun ElementStateScope.appearDirection(
            content: ContentKey,
            element: ElementKey,
            transition: TransitionState.Transition,
        ): InputDirection {
            check(!transition.isInitiatedByUserInput)

            val inMaxDirection =
                when (transition) {
                    is TransitionState.Transition.ChangeScene -> {
                        val transitionTowardsContent = content == transition.toContent
                        val elementInContent = element.targetSize(content) != null
                        val isReversed = transition.currentScene != transition.toScene
                        (transitionTowardsContent xor elementInContent) xor !isReversed
                    }

                    is TransitionState.Transition.ShowOrHideOverlay -> {
                        val transitioningTowardsOverlay = transition.overlay == transition.toContent
                        val isReversed =
                            transitioningTowardsOverlay xor transition.isEffectivelyShown
                        transitioningTowardsOverlay xor isReversed
                    }

                    is TransitionState.Transition.ReplaceOverlay -> {
                        transition.effectivelyShownOverlay == content
                    }
                }

            return if (inMaxDirection) InputDirection.Max else InputDirection.Min
        }
    }
}
