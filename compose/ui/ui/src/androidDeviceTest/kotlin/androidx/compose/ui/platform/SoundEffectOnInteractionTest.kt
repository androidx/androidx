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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SoundEffectOnInteractionTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun soundEffects_default_isNotNull() {
        var soundEffect: SoundEffect? = null
        rule.setContent { soundEffect = LocalSoundEffect.current }
        assertThat(soundEffect).isNotNull()
    }

    @Test
    fun soundEffects_disabled_dropsCall() {
        val fakeSoundEffects = FakeSoundEffect()
        rule.setContent {
            CompositionLocalProvider(LocalSoundEffect provides fakeSoundEffects) {
                SoundEffectOnInteraction(enabled = false) {
                    val soundEffects = LocalSoundEffect.current
                    soundEffects?.playClickSound()
                }
            }
        }
        assertThat(fakeSoundEffects.playClickSoundCount).isEqualTo(0)
    }

    @Test
    fun soundEffects_nestedEnableDisable() {
        val fakeSoundEffects = FakeSoundEffect()

        rule.setContent {
            CompositionLocalProvider(LocalSoundEffect provides fakeSoundEffects) {
                SoundEffectOnInteraction(enabled = false) {
                    val wrapper1 = LocalSoundEffect.current
                    wrapper1?.playClickSound() // Should be dropped

                    SoundEffectOnInteraction(enabled = true) {
                        val wrapper2 = LocalSoundEffect.current
                        wrapper2?.playClickSound() // Should NOT be dropped
                    }
                }
            }
        }

        // Ensure only the inner wrapper played the sound
        assertThat(fakeSoundEffects.playClickSoundCount).isEqualTo(1)
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class)
    fun soundEffects_disabled_byFlag_dropsCall() {
        var soundEffect: SoundEffect? = null
        val originalFlag = AndroidComposeUiFlags.isInteractionSoundEffectsEnabled
        AndroidComposeUiFlags.isInteractionSoundEffectsEnabled = false
        try {
            rule.setContent { soundEffect = LocalSoundEffect.current }
            assertThat(soundEffect).isNotInstanceOf(AndroidSoundEffect::class.java)
        } finally {
            AndroidComposeUiFlags.isInteractionSoundEffectsEnabled = originalFlag
        }
    }

    private class FakeSoundEffect : SoundEffect {
        var playClickSoundCount = 0

        override fun playClickSound() {
            playClickSoundCount++
        }
    }
}
