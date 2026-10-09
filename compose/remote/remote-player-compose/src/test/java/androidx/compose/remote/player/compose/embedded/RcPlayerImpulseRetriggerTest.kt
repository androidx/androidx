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
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.SoundExpression
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ADD
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SUB
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * When the embedded player runs an impulse's initial pass (its one-shot setup, such as a sound,
 * haptic or particle reset): once per window, again when the start moves (a re-trigger, or a start
 * rescheduled ahead), and once per iteration of an enclosing canvas loop, but never once per frame.
 * Each initial pass plays a sound, so the number of sounds played counts the initial passes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalRemotePlayerApi::class)
class RcPlayerImpulseRetriggerTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun steadyWindow_runsInitialPassOnce() {
        val player = play { sound -> writer.impulse(2f, 1f) { writer.playSound(sound) } }

        player.advanceTo(1200)
        assertThat(player.played).isEqualTo(1)

        // 60 frames inside the window and then past its end don't repeat the setup.
        player.advanceTo(3600)
        assertThat(player.played).isEqualTo(1)
    }

    @Test
    fun retriggerInsideOpenWindow_rerunsInitialPass() {
        val player = play { sound ->
            val start = addNamedFloat("start", 1f)
            writer.impulse(2f, start) { writer.playSound(sound) }
        }

        player.advanceTo(1200)
        player.advanceTo(1500)
        assertThat(player.played).isEqualTo(1)

        // Re-trigger while the window is still open, as a tap setting start = now would.
        player.state.floatState("start").value = 1.45f
        player.advanceTo(1550)
        assertThat(player.played).isEqualTo(2)

        player.advanceTo(2500)
        assertThat(player.played).isEqualTo(2)
    }

    @Test
    fun startRescheduledAhead_runsInitialPassWhenTheNewWindowOpens() {
        val player = play { sound ->
            val start = addNamedFloat("start", 1f)
            writer.impulse(2f, start) { writer.playSound(sound) }
        }

        player.advanceTo(1500)
        assertThat(player.played).isEqualTo(1)

        player.state.floatState("start").value = 2.5f
        player.advanceTo(2400)
        assertThat(player.played).isEqualTo(1)

        player.advanceTo(2700)
        assertThat(player.played).isEqualTo(2)
        player.advanceTo(3500)
        assertThat(player.played).isEqualTo(2)
    }

    @Test
    fun impulseInCanvasLoop_runsInitialPassPerIteration() {
        val player = play { sound ->
            writer.startLoop(4f)
            writer.impulse(2f, 1f) { writer.playSound(sound) }
            writer.endLoop()
        }

        player.advanceTo(1200)
        assertThat(player.played).isEqualTo(4)

        player.advanceTo(2500)
        assertThat(player.played).isEqualTo(4)
    }

    @Test
    fun impulseInCanvasLoop_indexDependentStart_runsInitialPassOncePerIterationNotPerFrame() {
        val player = play { sound ->
            val index = writer.startLoop(3f)
            // Iteration i starts at 1 + 0.05 * i seconds.
            val start = writer.floatExpression(index, 0.05f, MUL, 1f, ADD)
            writer.impulse(2f, start) { writer.playSound(sound) }
            writer.endLoop()
        }

        player.advanceTo(900)
        assertThat(player.played).isEqualTo(0)

        player.advanceTo(1300)
        assertThat(player.played).isEqualTo(3)

        // Each iteration's start is stable, so ~40 more frames inside the windows add nothing.
        player.advanceTo(2900)
        assertThat(player.played).isEqualTo(3)
    }

    @Test
    fun startFollowingTheClock_runsInitialPassOnceThenProcess() {
        val player = play { sound ->
            // The window is always open, but its start moves every frame.
            val start = writer.floatExpression(RemoteContext.FLOAT_ANIMATION_TIME, 0.1f, SUB)
            writer.impulse(1f, start) {
                writer.playSound(sound)
                writer.impulseProcess {
                    painter.setColor(AndroidColor.RED).commit()
                    writer.drawRect(0f, 0f, 10f, 10f)
                }
            }
        }

        player.advanceTo(100)
        player.advanceTo(1000)
        assertThat(player.played).isEqualTo(1)
        assertRed(rule.captureRootBitmap(), 5, 5)
    }

    private class Player(val rule: RcPlayerTestRule, val state: RcPlayerState) {
        var played = 0
        private var elapsed = 0L

        /** Runs 16ms frames, drawing each one, until [millis] after the player started. */
        fun advanceTo(millis: Long) {
            while (elapsed < millis) {
                val step = minOf(16L, millis - elapsed)
                rule.mainClock.advanceTimeBy(step)
                elapsed += step
                rule.captureRootBitmap()
            }
        }
    }

    /**
     * Plays a raw draw-list document whose [content] gets the id of a sound to play in each initial
     * pass, and counts the sounds played.
     */
    private fun play(content: RemoteComposeContextAndroid.(sound: Int) -> Unit): Player {
        rule.mainClock.autoAdvance = false
        val document = buildDocument {
            val sound =
                writer.addSoundExpression(SoundExpression.TYPE_TONE, 440f, 0.1f, 0f, 1f, 1f, 1f)
            content(sound)
            writer.drawRect(0f, 0f, 1f, 1f)
        }
        val state = RcPlayerState(document)
        val player = Player(rule, state)
        val engine =
            object : CoreDocument.SoundEngine {
                override fun loadSound(soundId: Int, data: ByteArray) {}

                override fun playSound(soundId: Int) {
                    player.played++
                }
            }
        // The default SoundFeature plays through SoundSupport; count plays with a fake engine.
        val features =
            RcPlayerFeatures.forProfiles(document.profileMask) +
                RcPlayerFeatureSet(SoundFeature { RcSoundEngineHandle(engine) {} })
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(100.dp)) {
                    RcPlayer(state = state, features = features)
                }
            }
        }
        rule.captureRootBitmap()
        return player
    }
}
