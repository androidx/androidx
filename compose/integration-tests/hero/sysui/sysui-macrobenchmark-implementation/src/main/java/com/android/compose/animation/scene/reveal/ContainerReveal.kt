/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.compose.animation.scene.reveal

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.unit.IntSize
import com.android.compose.animation.scene.ContentKey
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.TransitionBuilder
import com.android.compose.animation.scene.UserActionDistance
import com.android.compose.animation.scene.content.state.TransitionState
import com.android.compose.animation.scene.mechanics.MotionValueInput
import com.android.compose.animation.scene.mechanics.TransitionScopedMechanicsAdapter
import com.android.compose.animation.scene.mechanics.VerticalContainerRevealFlag
import com.android.compose.animation.scene.transformation.CustomPropertyTransformation
import com.android.compose.animation.scene.transformation.PropertyTransformation
import com.android.compose.animation.scene.transformation.PropertyTransformationScope
import com.android.mechanics.MotionValue
import com.android.mechanics.behavior.VerticalExpandContainerSpec
import kotlinx.coroutines.CoroutineScope

interface ContainerRevealHaptics {
    /**
     * Called when the reveal threshold is crossed while the user was dragging on screen.
     *
     * Important: This callback is called during layout and its implementation should therefore be
     * very fast or posted to a different thread.
     *
     * @param revealed whether we go from hidden to revealed, i.e. whether the container size is
     *   going to jump from a smaller size to a bigger size.
     */
    fun onRevealThresholdCrossed(revealed: Boolean)
}

/**
 * Animate the reveal of [container] by animating its size.
 *
 * This implicitly sets the [distance] of the transition to the target size of [container]
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun TransitionBuilder.verticalContainerReveal(
    container: ElementKey,
    motionSpec: VerticalExpandContainerSpec,
    haptics: ContainerRevealHaptics,
    useMechanics: Boolean = VerticalContainerRevealFlag.isEnabled,
) {
    // Make the swipe distance be exactly the target height of the container.
    // TODO(b/376438969): Make sure that this works correctly when the target size of the element
    // is changing during the transition (e.g. a notification was added). At the moment, the user
    // action distance is only called until it returns a value > 0f, which is then cached.
    distance = UserActionDistance { fromContent, toContent, _ ->
        val targetSizeInFromContent = container.targetSize(fromContent)
        val targetSizeInToContent = container.targetSize(toContent)
        if (targetSizeInFromContent != null && targetSizeInToContent != null) {
            error(
                "verticalContainerReveal should not be used with shared elements, but " +
                    "${container.debugName} is in both ${fromContent.debugName} and " +
                    toContent.debugName
            )
        }

        (targetSizeInToContent?.height ?: targetSizeInFromContent?.height)?.toFloat() ?: 0f
    }

    if (!useMechanics) {
        scaleSize(container, height = 0f)
        return
    }

    // TODO(b/392534646) Add haptics back
    val heightInput: MotionValueInput = { progress, content, element ->
        val idleSize = checkNotNull(element.targetSize(content))
        val targetHeight = idleSize.height.toFloat()
        targetHeight * progress
    }

    transformation(container) {
        object : CustomPropertyTransformation<IntSize> {
            override val property = PropertyTransformation.Property.Size

            val heightValue =
                TransitionScopedMechanicsAdapter(
                    computeInput = heightInput,
                    stableThreshold = MotionValue.StableThresholdSpatial,
                    label = "verticalContainerReveal::height",
                ) { _, _ ->
                    motionSpec.createHeightSpec(motionScheme, density = this)
                }
            val widthValue =
                TransitionScopedMechanicsAdapter(
                    computeInput = heightInput,
                    stableThreshold = MotionValue.StableThresholdSpatial,
                    label = "verticalContainerReveal::width",
                ) { content, element ->
                    val idleSize = checkNotNull(element.targetSize(content))
                    val intrinsicWidth = idleSize.width.toFloat()
                    motionSpec.createWidthSpec(intrinsicWidth, motionScheme, density = this)
                }

            override fun PropertyTransformationScope.transform(
                content: ContentKey,
                element: ElementKey,
                transition: TransitionState.Transition,
                transitionScope: CoroutineScope,
            ): IntSize {

                val height =
                    with(heightValue) { update(content, element, transition, transitionScope) }
                val width =
                    with(widthValue) { update(content, element, transition, transitionScope) }

                return IntSize(width.toInt(), height.toInt())
            }
        }
    }

    transformation(container) {
        object : CustomPropertyTransformation<Float> {

            override val property = PropertyTransformation.Property.Alpha
            val alphaValue =
                TransitionScopedMechanicsAdapter(
                    computeInput = heightInput,
                    label = "verticalContainerReveal::alpha",
                ) { _, _ ->
                    motionSpec.createAlphaSpec(motionScheme, density = this)
                }

            override fun PropertyTransformationScope.transform(
                content: ContentKey,
                element: ElementKey,
                transition: TransitionState.Transition,
                transitionScope: CoroutineScope,
            ): Float {
                return with(alphaValue) { update(content, element, transition, transitionScope) }
            }
        }
    }
}
