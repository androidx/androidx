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

package androidx.compose.ui.tooling.animation.clock

import androidx.compose.animation.tooling.ComposeAnimatedProperty
import androidx.compose.animation.tooling.TransitionInfo
import androidx.compose.ui.tooling.animation.UnsupportedComposeAnimation
import androidx.compose.ui.tooling.animation.states.ComposeAnimationState
import androidx.compose.ui.tooling.animation.states.NoopState

/**
 * [ComposeAnimationClock] for unsupported [androidx.compose.animation.tooling.ComposeAnimation].
 */
internal class NoopClock(override val animation: UnsupportedComposeAnimation) :
    ComposeAnimationClock<UnsupportedComposeAnimation, ComposeAnimationState> {

    override var state = NoopState

    override fun getMaxDuration(): Long = 0

    override fun getMaxDurationPerIteration(): Long = 0

    override fun getAnimatedProperties(): List<ComposeAnimatedProperty> = emptyList()

    override fun getTransitions(stepMillis: Long): List<TransitionInfo> = emptyList()

    override fun setClockTime(animationTimeNanos: Long) {}

    override fun setStateParameters(par1: Any, par2: Any?) {}
}
