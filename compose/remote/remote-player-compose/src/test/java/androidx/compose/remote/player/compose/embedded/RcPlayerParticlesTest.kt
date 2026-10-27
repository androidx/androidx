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
import androidx.compose.remote.core.operations.ParticlesCreate
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Behavioral tests for the particle system, which is bridged to the core (View player)
 * implementation — see RcPlayerParticles. Not a screenshot test: `captureToImage` is used only to
 * force draw passes (Robolectric doesn't draw otherwise); the assertions are on the particle
 * *state* the core simulation advances.
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerParticlesTest {

    @get:Rule val rule = RcPlayerTestRule()

    /**
     * Matches the View player, where `CoreDocument.paint` paints every op each frame: a
     * `ParticlesCreate` that is not wrapped in an impulse re-seeds on every paint, so the loop only
     * ever advances the particles one step from their seed.
     */
    @Test
    fun unwrappedCreateReseedsParticlesOnEveryPaint() {
        // 3 particles with dimensions (x, y) seeded at (10, 20), stepped by x += 1, y += 2.
        val vars = FloatArray(2)
        val document = buildDocument {
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

        // The frame loop stays alive for particle documents, so drive the clock manually.
        rule.mainClock.autoAdvance = false
        rule.setContent { Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) } }
        val create = requireNotNull(findParticlesCreate(document))

        repeat(3) {
            rule.mainClock.advanceTimeBy(32)
            rule.onRoot().captureToImage()
            for (particle in create.particles) {
                assertThat(particle[0]).isEqualTo(11f)
                assertThat(particle[1]).isEqualTo(22f)
            }
        }
    }

    /**
     * The documented idiom: `impulse { createParticles; impulseProcess { particlesLoop } }`. The
     * create runs once in the impulse's initial pass and the process steps the simulation on every
     * later frame, so the particles accumulate motion.
     */
    @Test
    fun impulseWrappedParticlesSeedOnceAndAccumulate() {
        val vars = FloatArray(2)
        val document = buildDocument {
            writer.impulse(10f, 0f) {
                val particles =
                    writer.createParticles(vars, arrayOf(floatArrayOf(10f), floatArrayOf(20f)), 3)
                writer.impulseProcess {
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
            }
        }

        rule.mainClock.autoAdvance = false
        rule.setContent { Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) } }
        val create = requireNotNull(findParticlesCreate(document))

        val xs = mutableListOf<Float>()
        repeat(4) {
            rule.mainClock.advanceTimeBy(32)
            rule.onRoot().captureToImage()
            for (particle in create.particles) {
                assertThat(particle[0]).isAtLeast(10f)
                // Seeded at (10, 20) and only ever stepped by (+1, +2).
                assertThat(particle[1] - 20f).isWithin(1e-3f).of(2f * (particle[0] - 10f))
            }
            xs += create.particles[0][0]
        }
        // Accumulates: strictly increasing after the initial pass, and well past a single step.
        for (i in 2 until xs.size) assertThat(xs[i]).isGreaterThan(xs[i - 1])
        assertThat(xs.last()).isGreaterThan(12f)
    }

    /**
     * Impulse gating: nothing runs before `startAt` or after `startAt + duration`, and inside the
     * window the process draws from the second frame on.
     */
    @Test
    fun impulseOnlyDrawsInsideItsTimeWindow() {
        val vars = FloatArray(2)
        val document = buildDocument {
            painter.setColor(AndroidColor.RED).commit()
            writer.impulse(1f, 0.5f) {
                val particles =
                    writer.createParticles(vars, arrayOf(floatArrayOf(50f), floatArrayOf(50f)), 1)
                writer.impulseProcess {
                    writer.particlesLoop(
                        particles,
                        null,
                        arrayOf(floatArrayOf(vars[0]), floatArrayOf(vars[1])),
                    ) {
                        writer.drawCircle(vars[0], vars[1], 8f)
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.setContent { Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) } }

        rule.mainClock.advanceTimeBy(200)
        assertNotRed(rule.onRoot().captureToImage().asAndroidBitmap(), 50, 50)

        rule.mainClock.advanceTimeBy(400) // ~0.6s: inside [0.5, 1.5]
        rule.onRoot().captureToImage() // initial pass seeds
        rule.mainClock.advanceTimeBy(32)
        assertRed(rule.onRoot().captureToImage().asAndroidBitmap(), 50, 50)

        rule.mainClock.advanceTimeBy(1200) // ~1.8s: past the window
        assertNotRed(rule.onRoot().captureToImage().asAndroidBitmap(), 50, 50)
    }

    /**
     * A variable `startAt` re-triggers the impulse: once the window has passed, moving it back
     * re-runs the initial pass, which re-seeds the particles.
     */
    @Test
    fun retriggeredImpulseReseedsParticles() {
        val vars = FloatArray(2)
        val document = buildDocument {
            val start = writer.addNamedFloat("start", 1000f)
            writer.impulse(10f, start) {
                val particles =
                    writer.createParticles(vars, arrayOf(floatArrayOf(10f), floatArrayOf(20f)), 1)
                writer.impulseProcess {
                    writer.particlesLoop(
                        particles,
                        null,
                        arrayOf(
                            floatArrayOf(vars[0], 1f, AnimatedFloatExpression.ADD),
                            floatArrayOf(vars[1]),
                        ),
                    ) {
                        writer.drawCircle(vars[0], vars[1], 2f)
                    }
                }
            }
        }
        val state = RcPlayerState(document, defaultPrefix = null)
        var start by state.floatState("start")

        rule.mainClock.autoAdvance = false
        rule.setContent { Box(modifier = Modifier.size(100.dp)) { RcPlayer(state = state) } }
        val create = requireNotNull(findParticlesCreate(document))
        fun frameX(): Float {
            rule.mainClock.advanceTimeBy(32)
            rule.onRoot().captureToImage()
            return create.particles[0][0]
        }

        // Before the window: never seeded.
        frameX()
        assertThat(frameX()).isEqualTo(0f)

        start = 0f
        assertThat(frameX()).isAtLeast(10f)
        frameX()
        val accumulated = frameX()
        assertThat(accumulated).isGreaterThan(11f)

        start = -100f // window over: re-arms the initial pass
        assertThat(frameX()).isEqualTo(accumulated)
        start = 0f
        val reseeded = frameX()
        assertThat(reseeded).isAtLeast(10f)
        assertThat(reseeded).isLessThan(accumulated)
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
        val document = buildDocument {
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
     * Motion is visible in the pixels: an impulse-wrapped particle stepping +8px/frame in x renders
     * at its current position on each frame, and its seed position clears.
     */
    @Test
    fun particleMotionRendersAtTheNewPositionEachFrame() {
        val vars = FloatArray(2)
        val document = buildDocument {
            painter.setColor(AndroidColor.RED).commit()
            writer.impulse(10f, 0f) {
                val particles =
                    writer.createParticles(vars, arrayOf(floatArrayOf(10f), floatArrayOf(50f)), 1)
                writer.impulseProcess {
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
            }
        }

        rule.mainClock.autoAdvance = false
        rule.setContent { Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) } }

        val create = requireNotNull(findParticlesCreate(document))
        rule.onRoot().captureToImage() // initial pass: seed
        rule.mainClock.advanceTimeBy(32)
        rule.onRoot().captureToImage() // process: first step
        val x1 = create.particles[0][0]
        rule.mainClock.advanceTimeBy(32)
        rule.onRoot().captureToImage()
        rule.mainClock.advanceTimeBy(32)
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val x2 = create.particles[0][0]
        assertThat(x2).isAtLeast(x1 + 16f)
        // The freshly drawn frame shows the particle at its *current* position, not a stale one.
        assertRed(bitmap, x2.toInt(), 50)
        // The seed position cleared back to the (non-red) background.
        assertNotRed(bitmap, 10, 50)
    }

    /**
     * ParticlesCompare (per-particle conditional pass, bridged to core): when the condition (x -
     * 50 > 0) fires, the update equations mutate the particle in place.
     */
    @Test
    fun particlesCompareMutatesMatchingParticlesThroughCoreBridge() {
        val vars = FloatArray(2)
        val document = buildDocument {
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

        rule.mainClock.autoAdvance = false
        rule.setContent { Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) } }
        rule.onRoot().captureToImage() // one frame: seed + loop + compare

        val create = requireNotNull(findParticlesCreate(document))
        assertThat(create.particles[0][0]).isEqualTo(100f)
        assertThat(create.particles[0][1]).isEqualTo(20f)
    }

    /**
     * The core bridge draws on the DrawScope's native canvas, so particles must inherit a transform
     * the Compose dispatcher applied earlier in the stream (here translate(40, 0)).
     */
    @Test
    fun particlesInheritEnclosingCanvasTransform() {
        val vars = FloatArray(2)
        rule.mainClock.autoAdvance = false
        val bitmap = rule.renderDocument {
            painter.setColor(AndroidColor.RED).commit()
            writer.save()
            writer.translate(40f, 0f)
            val particles =
                writer.createParticles(
                    vars,
                    arrayOf(floatArrayOf(10f), floatArrayOf(50f)),
                    1,
                )
            writer.particlesLoop(
                particles,
                null,
                arrayOf(floatArrayOf(vars[0]), floatArrayOf(vars[1])),
            ) {
                writer.drawCircle(vars[0], vars[1], 6f)
            }
            writer.restore()
        }

        assertRed(bitmap, 50, 50)
        assertNotRed(bitmap, 10, 50)
    }

    /**
     * Paint set inside the bridged particle subtree must not leak into Compose draws that follow: a
     * blue rect drawn after a red particle loop renders blue.
     */
    @Test
    fun drawsAfterParticlesUseTheirOwnPaint() {
        val vars = FloatArray(2)
        rule.mainClock.autoAdvance = false
        val bitmap = rule.renderDocument {
            val particles =
                writer.createParticles(
                    vars,
                    arrayOf(floatArrayOf(20f), floatArrayOf(20f)),
                    1,
                )
            writer.particlesLoop(
                particles,
                null,
                arrayOf(floatArrayOf(vars[0]), floatArrayOf(vars[1])),
            ) {
                painter.setColor(AndroidColor.RED).commit()
                writer.drawCircle(vars[0], vars[1], 6f)
            }
            painter.setColor(AndroidColor.BLUE).commit()
            writer.drawRect(70f, 70f, 90f, 90f)
        }

        assertRed(bitmap, 20, 20)
        val pixel = bitmap.getPixel(80, 80)
        assertThat(AndroidColor.blue(pixel)).isGreaterThan(200)
        assertThat(AndroidColor.red(pixel)).isLessThan(60)
    }

    /** Particles inside a layout document's canvas component (not a raw draw list) render. */
    @Test
    fun particlesRenderInsideLayoutCanvas() {
        val vars = FloatArray(2)
        rule.mainClock.autoAdvance = false
        val bitmap = rule.renderDocument {
            root {
                box(this.Modifier.fillMaxWidth().fillMaxHeight()) {
                    canvas(this.Modifier.fillMaxWidth().fillMaxHeight()) {
                        painter.setColor(AndroidColor.RED).commit()
                        val particles =
                            writer.createParticles(
                                vars,
                                arrayOf(floatArrayOf(30f), floatArrayOf(60f)),
                                1,
                            )
                        writer.particlesLoop(
                            particles,
                            null,
                            arrayOf(floatArrayOf(vars[0]), floatArrayOf(vars[1])),
                        ) {
                            writer.drawCircle(vars[0], vars[1], 6f)
                        }
                    }
                }
            }
        }

        assertRed(bitmap, 30, 60)
    }

    @Test
    fun explicitFeatureSetWithoutParticlesFeatureSkipsParticles() {
        val bitmap = rule.renderDocument(redParticleDocument(), RcPlayerFeatureSet.Empty)

        assertNotRed(bitmap, 30, 60)
    }

    @Test
    fun explicitFeatureSetWithParticlesFeatureRunsParticles() {
        val bitmap =
            rule.renderDocument(redParticleDocument(), RcPlayerFeatureSet(ParticlesFeature))

        assertRed(bitmap, 30, 60)
    }

    @Test
    fun defaultFeaturesIncludeParticlesForEveryProfile() {
        assertThat(RcPlayerFeatures.forProfiles(RcProfiles.PROFILE_BASELINE).features)
            .contains(ParticlesFeature)
        assertThat(RcPlayerFeatures.forProfiles(RcProfiles.PROFILE_ANDROIDX).features)
            .contains(ParticlesFeature)
    }

    @Test
    fun particleOpcodesAreUnsupportedWithoutParticlesFeature() {
        val unsupported = RcPlayerFeatureSet.Empty.unsupportedOpcodes(RcProfiles.PROFILE_ANDROIDX)

        ParticlesFeature.opcodes.forEach { assertThat(it in unsupported).isTrue() }
        assertThat(
                RcPlayerFeatureSet(ParticlesFeature)
                    .unsupportedOpcodes(RcProfiles.PROFILE_ANDROIDX)
                    .any { it in ParticlesFeature.opcodes }
            )
            .isFalse()
    }

    /** One red particle drawn at (30, 60). */
    private fun redParticleDocument(): CoreDocument {
        val vars = FloatArray(2)
        rule.mainClock.autoAdvance = false
        return buildDocument {
            painter.setColor(AndroidColor.RED).commit()
            val particles =
                writer.createParticles(vars, arrayOf(floatArrayOf(30f), floatArrayOf(60f)), 1)
            writer.particlesLoop(
                particles,
                null,
                arrayOf(floatArrayOf(vars[0]), floatArrayOf(vars[1])),
            ) {
                writer.drawCircle(vars[0], vars[1], 6f)
            }
        }
    }

    private fun findParticlesCreate(document: CoreDocument): ParticlesCreate? =
        document.allOperations().filterIsInstance<ParticlesCreate>().firstOrNull()
}
