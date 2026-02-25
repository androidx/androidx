/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.tooling.animation.search

import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.ui.tooling.animation.ClockInfo
import androidx.compose.ui.tooling.animation.clock.ComposeAnimationClock

/**
 * Information found in slotTree required to create
 * [androidx.compose.animation.tooling.ComposeAnimation] and
 * [androidx.compose.ui.tooling.animation.clock.ComposeAnimationClock] for each animation. For
 * example [androidx.compose.animation.core.Transition],
 * [androidx.compose.ui.tooling.animation.ToolingState].
 *
 * @param Animation [ComposeAnimation] associated with this [SearchInfo].
 * @param Clock [ComposeAnimationClock] to be created using information from this [SearchInfo].
 */
internal interface SearchInfo<Animation : ComposeAnimation, Clock : ComposeAnimationClock<*, *>> {
    /** Animation object found in slotTree for which this [SearchInfo] is created. */
    val animationObject: Any

    /** Label of the animation. */
    val label: String

    /** Initial state of the animation. */
    val initialState: Any?

    /** Target state of the animation. */
    val targetState: Any?

    /**
     * Set the [initialState] of this [SearchInfo] to the current value of the [animationObject]
     * it's tracking.
     */
    fun setInitialStateToCurrentAnimationValue()

    /**
     * Set the [targetState] of this [SearchInfo] to the current value of the [animationObject] it's
     * tracking.
     */
    fun setTargetStateToCurrentAnimationValue()

    /**
     * Create [ComposeAnimation] for this [SearchInfo].
     *
     * @return created [ComposeAnimation]. Can return null if corresponding API is not available or
     *   animation could not be parsed or invalid.
     */
    fun createAnimation(): Animation?

    /**
     * Create [ComposeAnimationClock] for target [Animation].
     *
     * @param clockInfo extra information about
     *   [androidx.compose.ui.tooling.animation.PreviewAnimationClock] required to create a clock.
     */
    fun createClock(animation: Animation, clockInfo: ClockInfo): Clock

    /** Attach [SearchInfo]'s overrides to allow tooling control animation values. */
    fun attach() {}

    /**
     * Detach [SearchInfo]'s overrides previously attached in [attach] and let animation play
     * without intervention from tooling.
     */
    fun detach() {}
}
