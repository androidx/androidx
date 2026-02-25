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

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.ui.tooling.animation.ClockInfo
import androidx.compose.ui.tooling.animation.InfiniteTransitionComposeAnimation
import androidx.compose.ui.tooling.animation.InfiniteTransitionComposeAnimation.Companion.parse
import androidx.compose.ui.tooling.animation.ToolingOverride
import androidx.compose.ui.tooling.animation.clock.InfiniteTransitionClock

/**
 * [SearchInfo] for [androidx.compose.animation.core.rememberInfiniteTransition] animation.
 *
 * @param infiniteTransition used by [androidx.compose.animation.core.rememberInfiniteTransition]
 * @param toolingOverride allows to override behavior of the animation
 */
internal data class InfiniteTransitionSearchInfo(
    val infiniteTransition: InfiniteTransition,
    val toolingOverride: ToolingOverride<Long>,
) : SearchInfo<InfiniteTransitionComposeAnimation, InfiniteTransitionClock> {

    override val animationObject: Any = infiniteTransition

    override val label: String
        get() = infiniteTransition.label

    override val initialState: Any? = null

    override val targetState: Any? = null

    override fun setInitialStateToCurrentAnimationValue() {}

    override fun setTargetStateToCurrentAnimationValue() {}

    override fun createAnimation(): InfiniteTransitionComposeAnimation? {
        return this.parse()
    }

    override fun createClock(
        animation: InfiniteTransitionComposeAnimation,
        clockInfo: ClockInfo,
    ): InfiniteTransitionClock {
        return InfiniteTransitionClock(animation) { clockInfo.getMaxDurationPerIterationMillis() }
    }

    override fun attach() {
        toolingOverride.overrideState()
    }

    override fun detach() {
        toolingOverride.clearOverride()
    }
}
