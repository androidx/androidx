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
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.ParticlesCreate
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Behavioral tests for the particle system, which is bridged to the core (View player)
 * implementation — see RcPlayerParticles. Not a screenshot test: `captureToImage` is used only to
 * force draw passes (Robolectric doesn't draw otherwise); the assertions are on the particle
 * *state* the core simulation advances.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RcPlayerParticlesTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun particleSimulationSeedsAndAdvancesThroughCoreBridge() {
        // A raw draw-list document: 3 particles with dimensions (x, y) seeded at (10, 20), each
        // frame stepped by the RPN update equations x += 1, y += 2, drawing a circle per particle.
        val vars = FloatArray(2)
        val docContext =
            RemoteComposeContextAndroid(100, 100, "particles", AndroidxRcPlatformServices()) {
                val particles =
                    writer.createParticles(vars, arrayOf(floatArrayOf(10f), floatArrayOf(20f)), 3)
                writer.particlesLoop(
                    particles,
                    null,
                    arrayOf(
                        floatArrayOf(vars[0], 1f, AnimatedFloatExpression.ADD),
                        floatArrayOf(vars[1], 2f, AnimatedFloatExpression.ADD),
                    ),
                ) {
                    writer.drawCircle(vars[0], vars[1], 4f)
                }
            }

        val document =
            CoreDocument().apply {
                ByteArrayInputStream(docContext.writer.encodeToByteArray()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        // The frame loop stays alive for particle documents, so drive the clock manually.
        rule.mainClock.autoAdvance = false
        rule.setContent { Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) } }
        rule.mainClock.advanceTimeBy(32)
        rule.onRoot().captureToImage() // force a draw pass: seeds + first simulation step

        val create = requireNotNull(findParticlesCreate(document.getOperationsReflection()))
        val afterFirstDraw = create.particles.map { it.clone() }
        // Seeded from the initial equations (10, 20) and stepped at least once by +1/+2 per draw:
        // x > 10, y > 20 for every particle, and y - 20 == 2 * (x - 10).
        for (particle in afterFirstDraw) {
            assertThat(particle[0]).isGreaterThan(10f)
            assertThat(particle[1]).isGreaterThan(20f)
            assertThat(particle[1] - 20f).isWithin(1e-3f).of(2f * (particle[0] - 10f))
        }

        rule.mainClock.advanceTimeBy(32)
        rule.onRoot().captureToImage() // next frame: the core loop steps the simulation again

        val afterSecondDraw = create.particles
        for (i in afterSecondDraw.indices) {
            assertThat(afterSecondDraw[i][0]).isGreaterThan(afterFirstDraw[i][0])
            assertThat(afterSecondDraw[i][1]).isGreaterThan(afterFirstDraw[i][1])
        }
    }

    /**
     * Frame-0 pixel check: a static particle (identity update equations) must be *visible at its
     * seeded position on the first rendered frame* — the simulation must not need extra frames to
     * settle. Also exercises the paint bridge: the red PaintData is consumed by the Compose
     * dispatcher and replayed into the core paint context, so the particle circle renders red.
     */
    @Test
    fun particlesAreVisibleAtSeededPositionOnTheFirstFrame() {
        val vars = FloatArray(2)
        val docContext =
            RemoteComposeContextAndroid(100, 100, "particles", AndroidxRcPlatformServices()) {
                painter.setColor(AndroidColor.RED).commit()
                val particles =
                    writer.createParticles(vars, arrayOf(floatArrayOf(30f), floatArrayOf(40f)), 1)
                writer.particlesLoop(
                    particles,
                    null,
                    // Identity updates: the particle stays at its seeded position.
                    arrayOf(floatArrayOf(vars[0]), floatArrayOf(vars[1])),
                ) {
                    writer.drawCircle(vars[0], vars[1], 8f)
                }
            }

        val document =
            CoreDocument().apply {
                ByteArrayInputStream(docContext.writer.encodeToByteArray()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        rule.mainClock.autoAdvance = false
        rule.setContent { Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) } }

        // First rendered frame only.
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val onParticle = bitmap.getPixel(30, 40)
        val background = bitmap.getPixel(80, 80)
        assertThat(AndroidColor.red(onParticle)).isGreaterThan(200)
        assertThat(AndroidColor.green(onParticle)).isLessThan(60)
        // The background (test window) is not red — white or transparent, green stays high.
        assertThat(AndroidColor.green(background)).isGreaterThan(150)
    }

    /**
     * Motion is visible in the pixels: a particle stepping +8px/frame in x renders at a new
     * position on the next frame (old position clears, new position filled).
     */
    @Test
    fun particleMotionRendersAtTheNewPositionEachFrame() {
        val vars = FloatArray(2)
        val docContext =
            RemoteComposeContextAndroid(100, 100, "particles", AndroidxRcPlatformServices()) {
                painter.setColor(AndroidColor.RED).commit()
                val particles =
                    writer.createParticles(vars, arrayOf(floatArrayOf(10f), floatArrayOf(50f)), 1)
                writer.particlesLoop(
                    particles,
                    null,
                    arrayOf(
                        floatArrayOf(vars[0], 8f, AnimatedFloatExpression.ADD),
                        floatArrayOf(vars[1]),
                    ),
                ) {
                    writer.drawCircle(vars[0], vars[1], 6f)
                }
            }

        val document =
            CoreDocument().apply {
                ByteArrayInputStream(docContext.writer.encodeToByteArray()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        rule.mainClock.autoAdvance = false
        rule.setContent { Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) } }

        val create = requireNotNull(findParticlesCreate(document.getOperationsReflection()))
        rule.onRoot().captureToImage() // first frame: seed + first step
        val x1 = create.particles[0][0]
        rule.mainClock.advanceTimeBy(32)
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap() // next frame: stepped again
        val x2 = create.particles[0][0]
        assertThat(x2).isGreaterThan(x1)
        // The freshly drawn frame shows the particle at its *current* position, not a stale one.
        val onNew = bitmap.getPixel(x2.toInt(), 50)
        assertThat(AndroidColor.red(onNew)).isGreaterThan(200)
        val onOld = bitmap.getPixel((x1 - 8f).toInt().coerceAtLeast(0), 50)
        // The old position cleared back to the (non-red) background.
        assertThat(AndroidColor.green(onOld)).isGreaterThan(150)
    }

    /**
     * ParticlesCompare (per-particle conditional pass, bridged to core): when the condition (x -
     * 50 > 0) fires, the update equations mutate the particle in place.
     */
    @Test
    fun particlesCompareMutatesMatchingParticlesThroughCoreBridge() {
        val vars = FloatArray(2)
        val docContext =
            RemoteComposeContextAndroid(
                100,
                100,
                "particles",
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                AndroidxRcPlatformServices(),
            ) {
                val particles =
                    writer.createParticles(vars, arrayOf(floatArrayOf(60f), floatArrayOf(20f)), 1)
                // Seeding happens through the loop (identity updates keep x at 60).
                writer.particlesLoop(
                    particles,
                    null,
                    arrayOf(floatArrayOf(vars[0]), floatArrayOf(vars[1])),
                ) {
                    writer.drawCircle(vars[0], vars[1], 4f)
                }
                // Conditional pass: x > 50 (x - 50 > 0) -> set x = 100, keep y.
                writer.particlesComparison(
                    particles,
                    0.toShort(),
                    -1f,
                    -1f,
                    floatArrayOf(vars[0], 50f, AnimatedFloatExpression.SUB),
                    arrayOf(floatArrayOf(100f), floatArrayOf(vars[1])),
                    null,
                )
            }

        val document =
            CoreDocument().apply {
                ByteArrayInputStream(docContext.writer.encodeToByteArray()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        rule.mainClock.autoAdvance = false
        rule.setContent { Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) } }
        rule.onRoot().captureToImage() // one frame: seed + loop + compare

        val create = requireNotNull(findParticlesCreate(document.getOperationsReflection()))
        assertThat(create.particles[0][0]).isEqualTo(100f)
        assertThat(create.particles[0][1]).isEqualTo(20f)
    }

    private fun findParticlesCreate(operations: Collection<Operation>): ParticlesCreate? {
        for (op in operations) {
            if (op is ParticlesCreate) return op
            if (op is Container)
                findParticlesCreate(op.list)?.let {
                    return it
                }
        }
        return null
    }
}
