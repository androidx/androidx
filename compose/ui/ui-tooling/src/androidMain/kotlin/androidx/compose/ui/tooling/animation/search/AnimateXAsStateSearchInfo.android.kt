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

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.tooling.AnimateValueAsStateToolingHandle
import androidx.compose.ui.tooling.animation.AnimateXAsStateComposeAnimation
import androidx.compose.ui.tooling.animation.AnimateXAsStateComposeAnimation.Companion.parse
import androidx.compose.ui.tooling.animation.ClockInfo
import androidx.compose.ui.tooling.animation.ToolingState
import androidx.compose.ui.tooling.animation.clock.AnimateXAsStateClock

/** [SearchInfo] for [androidx.compose.animation.core.animateValueAsState] animation. */
internal data class AnimateXAsStateSearchInfo<T, V : AnimationVector>(
    val toolingHandle: AnimateValueAsStateToolingHandle<T, V>
) : SearchInfo<AnimateXAsStateComposeAnimation<*, *>, AnimateXAsStateClock<*, *>> {
    val toolingOverride = ToolingState(toolingHandle.animatable.value)
    val animatable
        get() = toolingHandle.animatable

    val animationSpec
        get() = toolingHandle.animationSpec

    override val animationObject: Any = toolingHandle.animatable

    override val label: String
        get() = toolingHandle.animatable.label

    override var initialState: Any? = null
        private set

    override var targetState: Any? = null
        private set

    override fun setInitialStateToCurrentAnimationValue() {
        initialState = toolingHandle.animatable.targetValue
    }

    override fun setTargetStateToCurrentAnimationValue() {
        targetState = toolingHandle.animatable.targetValue
    }

    override fun createAnimation(): AnimateXAsStateComposeAnimation<*, *>? {
        return this.parse()
    }

    override fun createClock(
        animation: AnimateXAsStateComposeAnimation<*, *>,
        clockInfo: ClockInfo,
    ): AnimateXAsStateClock<*, *> {
        return AnimateXAsStateClock(animation)
    }

    override fun attach() {
        toolingHandle.setToolingOverrideState(toolingOverride)
    }

    override fun detach() {
        toolingHandle.setToolingOverrideState(null)
    }
}
