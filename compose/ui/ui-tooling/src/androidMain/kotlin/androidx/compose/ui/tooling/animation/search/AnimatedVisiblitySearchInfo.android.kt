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
import androidx.compose.ui.tooling.animation.AnimatedVisibilityComposeAnimation
import androidx.compose.ui.tooling.animation.clock.AnimatedVisibilityClock
import androidx.compose.ui.tooling.animation.parseAnimatedVisibility

/**
 * [SearchInfo] for [androidx.compose.animation.AnimatedVisibility] animation.
 *
 * @param transition used by [androidx.compose.animation.AnimatedVisibility]
 */
internal class AnimatedVisibilitySearchInfo(val transition: Transition<Boolean>) :
    SearchInfo<AnimatedVisibilityComposeAnimation, AnimatedVisibilityClock> {
    override fun createAnimation(): AnimatedVisibilityComposeAnimation {
        return transition.parseAnimatedVisibility()
    }

    override fun createClock(
        animation: AnimatedVisibilityComposeAnimation
    ): AnimatedVisibilityClock {
        return AnimatedVisibilityClock(animation)
    }
}
