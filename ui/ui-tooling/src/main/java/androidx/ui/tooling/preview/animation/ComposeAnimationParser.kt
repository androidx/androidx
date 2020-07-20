/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.tooling.preview.animation

import android.util.Log
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.TransitionAnimation
import androidx.compose.animation.core.getStates
import androidx.ui.animation.tooling.ComposeAnimation
import androidx.ui.animation.tooling.ComposeAnimationType

// TODO(b/160126628): support other animation types, e.g. AnimatedValue
/**
 * Parses this [TransitionAnimation.TransitionAnimationClockObserver] into a [ComposeAnimation]
 * of type [ComposeAnimationType.TRANSITION_ANIMATION].
 */
@OptIn(InternalAnimationApi::class)
internal fun TransitionAnimation<*>.TransitionAnimationClockObserver.parse(): ComposeAnimation {
    Log.d("ComposeAnimationParser", "TransitionAnimation subscribed")
    animation.monotonic = false
    return object : ComposeAnimation {
        override fun getType() = ComposeAnimationType.TRANSITION_ANIMATION

        override fun getAnimation() = animation

        override fun getStates() = animation.getStates().filterNotNull().toSet()
    }
}