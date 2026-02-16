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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.ui.tooling.animation.AnimateXAsStateComposeAnimation
import androidx.compose.ui.tooling.animation.AnimateXAsStateComposeAnimation.Companion.parse
import androidx.compose.ui.tooling.animation.ToolingOverride
import androidx.compose.ui.tooling.animation.clock.AnimateXAsStateClock

/**
 * [SearchInfo] for [androidx.compose.animation.core.animateValueAsState] animation.
 *
 * @param animatable used by [androidx.compose.animation.core.animateValueAsState]
 * @param animationSpec used by [androidx.compose.animation.core.animateValueAsState]
 * @param toolingOverride allows to override behavior of the animation
 */
internal data class AnimateXAsStateSearchInfo<T, V : AnimationVector>(
    val animatable: Animatable<T, V>,
    val animationSpec: AnimationSpec<T>,
    val toolingOverride: ToolingOverride<T>,
) : SearchInfo<AnimateXAsStateComposeAnimation<*, *>, AnimateXAsStateClock<*, *>> {
    override fun createAnimation(): AnimateXAsStateComposeAnimation<*, *>? {
        return this.parse()
    }

    override fun createClock(
        animation: AnimateXAsStateComposeAnimation<*, *>
    ): AnimateXAsStateClock<*, *> {
        return AnimateXAsStateClock(animation)
    }

    override fun attach() {
        toolingOverride.overrideState()
    }

    override fun detach() {
        toolingOverride.clearOverride()
    }
}
