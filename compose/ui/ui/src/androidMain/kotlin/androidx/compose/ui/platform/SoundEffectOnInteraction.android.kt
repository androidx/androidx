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

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * Configure whether sound effects are played for interactions (clicks) in the provided [content].
 * This acts as a configuration wrapper that intercepts the [LocalSoundEffect] and conditionally
 * delegates or drops invocations of [SoundEffect.playClickSound].
 *
 * @param enabled true if sound effects should be played on user interactions, false to silence
 *   them.
 * @param content The composable subtree to wrap.
 * @see LocalSoundEffect
 * @see SoundEffect
 */
@Composable
fun SoundEffectOnInteraction(enabled: Boolean, content: @Composable () -> Unit) {
    @OptIn(ExperimentalComposeUiApi::class)
    if (!AndroidComposeUiFlags.isInteractionSoundEffectsEnabled) {
        content()
        return
    }
    val current = LocalSoundEffect.current

    val wrapper =
        remember(current, enabled) {

            // Unwrap any existing delegating wrappers to avoid an arbitrarily deep call stack
            var delegate = current
            while (delegate is DelegatingSoundEffect) {
                delegate = delegate.delegate
            }
            DelegatingSoundEffect(delegate = delegate, isEnabled = enabled)
        }
    CompositionLocalProvider(LocalSoundEffect provides wrapper, content)
}

/**
 * An internal implementation of [SoundEffect] that acts as a passthrough to an underlying
 * [delegate] unless [isEnabled] is false, in which case it drops the calls.
 */
internal class DelegatingSoundEffect(val delegate: SoundEffect, private val isEnabled: Boolean) :
    SoundEffect {
    override fun playClickSound() {
        if (isEnabled) {
            delegate.playClickSound()
        }
    }
}
