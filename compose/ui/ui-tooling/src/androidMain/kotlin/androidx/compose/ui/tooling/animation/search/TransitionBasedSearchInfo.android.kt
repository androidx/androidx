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

import androidx.compose.animation.core.Transition
import androidx.compose.ui.tooling.animation.TransitionBasedAnimation
import androidx.compose.ui.tooling.animation.clock.TransitionClock

/** [SearchInfo] for animations based on [Transition]. */
internal abstract class TransitionBasedSearchInfo<AnimationType : TransitionBasedAnimation<*>>(
    val transition: Transition<*>
) : SearchInfo<AnimationType, TransitionClock<*>> {

    override val animationObject: Any = transition

    final override var initialState: Any? = null
        private set

    final override var targetState: Any? = null
        private set

    override fun setInitialStateToCurrentAnimationValue() {
        initialState = transition.targetState
    }

    override fun setTargetStateToCurrentAnimationValue() {
        targetState = transition.targetState
    }
}
