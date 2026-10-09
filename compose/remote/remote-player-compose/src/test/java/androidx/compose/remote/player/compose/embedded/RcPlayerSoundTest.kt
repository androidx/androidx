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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Sound support in the embedded player, using a fake engine in place of `SoundSupport`. */
@OptIn(ExperimentalRemotePlayerApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerSoundTest {

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    private val engine = FakeSoundEngine()
    private var created = 0
    private var released = 0
    private val factory = RcSoundEngineFactory {
        created++
        RcSoundEngineHandle(engine) { released++ }
    }

    @Test
    fun soundDataLoadedAtSetupReachesTheEngine() {
        val data = byteArrayOf(1, 2, 3, 4)
        var soundId = 0
        val document = buildDocument { soundId = writer.addSound(data) }

        play(document)

        assertThat(created).isEqualTo(1)
        assertThat(engine.loaded[soundId]).isEqualTo(data)
    }

    @Test
    fun playSoundInAnImpulseFiresOnce() {
        var soundId = 0
        val document = buildDocument {
            soundId = writer.addSound(byteArrayOf(1, 2, 3, 4))
            writer.impulse(10f, 0f) { writer.playSound(soundId) }
            painter.setColor(AndroidColor.RED).commit()
            writer.drawRect(0f, 0f, 10f, 10f)
        }

        rule.mainClock.autoAdvance = false
        play(document)
        repeat(3) {
            rule.mainClock.advanceTimeBy(32)
            rule.onRoot().captureToImage()
        }

        assertThat(engine.played).containsExactly(soundId)
    }

    @Test
    fun documentsWithoutSoundDoNotCreateAnEngine() {
        val document = buildDocument {
            painter.setColor(AndroidColor.RED).commit()
            writer.drawRect(0f, 0f, 10f, 10f)
        }

        play(document)

        assertThat(created).isEqualTo(0)
    }

    @Test
    fun engineIsReleasedWhenThePlayerLeavesComposition() {
        val document = buildDocument { writer.addSound(byteArrayOf(1, 2, 3, 4)) }
        var show by mutableStateOf(true)
        rule.setContent {
            if (show) {
                Box(modifier = Modifier.size(100.dp)) {
                    RcPlayer(document = document, features = features(document))
                }
            }
        }
        rule.waitForIdle()
        assertThat(released).isEqualTo(0)

        show = false
        rule.waitForIdle()

        assertThat(released).isEqualTo(1)
    }

    @Test
    fun soundsAreLoadedOnceIntoEachEngineWhenThePlayerReentersComposition() {
        val data = byteArrayOf(1, 2, 3, 4)
        var soundId = 0
        val document = buildDocument { soundId = writer.addSound(data) }
        val engines = mutableListOf<FakeSoundEngine>()
        val features =
            RcPlayerFeatures.forProfiles(document.profileMask) +
                RcPlayerFeatureSet(
                    SoundFeature {
                        val engine = FakeSoundEngine().also { engines += it }
                        RcSoundEngineHandle(engine) {}
                    }
                )
        var show by mutableStateOf(true)
        rule.setContent {
            if (show) {
                Box(modifier = Modifier.size(100.dp)) {
                    RcPlayer(document = document, features = features)
                }
            }
        }
        rule.waitForIdle()

        show = false
        rule.waitForIdle()
        show = true
        rule.waitForIdle()

        assertThat(engines).hasSize(2)
        engines.forEach { engine ->
            assertThat(engine.loaded[soundId]).isEqualTo(data)
            assertThat(engine.loadCount).isEqualTo(1)
        }
    }

    @Test
    fun defaultFeaturesIncludeSoundForTheExperimentalProfile() {
        val experimental = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL

        assertThat(RcPlayerFeatures.forProfiles(experimental).features)
            .contains(SoundFeature.Default)
        assertThat(RcPlayerFeatures.forProfiles(RcProfiles.PROFILE_ANDROIDX).features)
            .doesNotContain(SoundFeature.Default)
    }

    @Test
    fun explicitFeatureSetWithoutSoundFeatureDoesNotCreateAnEngine() {
        val document = buildDocument { writer.addSound(byteArrayOf(1, 2, 3, 4)) }

        rule.renderDocument(document, RcPlayerFeatureSet.Empty)

        assertThat(created).isEqualTo(0)
    }

    private fun play(document: CoreDocument) {
        rule.renderDocument(document, features(document))
    }

    /**
     * The default features for [document], with the fake engine's feature replacing the default.
     */
    private fun features(document: CoreDocument): RcPlayerFeatureSet =
        RcPlayerFeatures.forProfiles(document.profileMask) +
            RcPlayerFeatureSet(SoundFeature(factory))

    private class FakeSoundEngine : CoreDocument.SoundEngine {
        val loaded = mutableMapOf<Int, ByteArray>()
        val played = mutableListOf<Int>()
        var loadCount = 0

        override fun loadSound(soundId: Int, data: ByteArray) {
            loadCount++
            loaded[soundId] = data
        }

        override fun playSound(soundId: Int) {
            played += soundId
        }
    }
}
