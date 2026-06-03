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

/**
 * Interface representing the capability to play sound effects on user interaction.
 *
 * This is used primarily to play interaction sound effects for click gestures.
 *
 * @sample androidx.compose.ui.samples.InteractionSoundSamples
 * @see LocalSoundEffect
 */
interface SoundEffect {

    /**
     * Plays a click sound effect.
     *
     * This method triggers the standard click sound effect, provided it is supported by the
     * platform, enabled by the user's system, and has not been silenced or customized via
     * `SoundEffectOnInteraction`.
     */
    fun playClickSound()
}
