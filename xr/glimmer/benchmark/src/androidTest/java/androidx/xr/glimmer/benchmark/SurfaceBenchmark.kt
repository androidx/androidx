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

package androidx.xr.glimmer.benchmark

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.surface
import androidx.xr.glimmer.testutils.createGlimmerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SurfaceBenchmark {

    @get:Rule(0) val benchmarkRule = ComposeBenchmarkRule()

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    /**
     * Measures the time to do composition, measure, layout and draw to render the first frame
     * immediately after the `surface` modifier is applied.
     */
    @Test
    fun surface_firstFrame() {
        with(benchmarkRule) {
            runBenchmarkFor({ SurfaceTestCase(addSurfaceModifierEnabledByDefault = false) }) {
                measureRepeatedOnUiThread {
                    runWithMeasurementDisabled {
                        doFramesUntilNoChangesPending()
                        // Add surface as modifier
                        getTestCase().toggleState()
                    }

                    doFrame()

                    runWithMeasurementDisabled { disposeContent() }
                }
            }
        }
    }

    /** Measures the time to draw the first frame for `surface` that is added as modifier. */
    @Test
    fun surface_firstDraw() {
        with(benchmarkRule) {
            runBenchmarkFor({ SurfaceTestCase(addSurfaceModifierEnabledByDefault = false) }) {
                measureRepeatedOnUiThread {
                    runWithMeasurementDisabled {
                        doFramesUntilNoChangesPending()
                        // Add surface as modifier
                        getTestCase().toggleState()

                        recompose()
                        requestLayout()
                        measure()
                        layout()
                        drawPrepare()
                    }

                    draw()

                    runWithMeasurementDisabled {
                        drawFinish()
                        disposeContent()
                    }
                }
            }
        }
    }

    /**
     * Measures the time to do composition, measure, layout and draw to render the second frame
     * after focus animation started. This captures the frame cost of the `surface` focus animation
     * starting.
     */
    @Test
    fun surface_secondFrameFocusAnimation() {
        with(benchmarkRule) {
            runBenchmarkFor({ SurfaceTestCase(addSurfaceModifierEnabledByDefault = false) }) {
                measureRepeatedOnUiThread {
                    runWithMeasurementDisabled {
                        doFramesUntilNoChangesPending()

                        // Add surface as modifier
                        getTestCase().toggleState()

                        // Animation with progress == 0 (not expensive)
                        doFrame()
                    }

                    doFrame()

                    runWithMeasurementDisabled { disposeContent() }
                }
            }
        }
    }

    /**
     * Measures the time to draw the second frame after focus animation is started. This captures
     * the frame cost of the `surface` focus animation starting.
     */
    @Test
    fun surface_secondDrawFocusAnimation() {
        with(benchmarkRule) {
            runBenchmarkFor({ SurfaceTestCase(addSurfaceModifierEnabledByDefault = false) }) {
                measureRepeatedOnUiThread {
                    runWithMeasurementDisabled {
                        doFramesUntilNoChangesPending()

                        // Add surface as modifier
                        getTestCase().toggleState()

                        // Animation with progress == 0 (not expensive)
                        doFrame()

                        recompose()
                        requestLayout()
                        measure()
                        layout()
                        drawPrepare()
                    }

                    draw()

                    runWithMeasurementDisabled {
                        drawFinish()
                        disposeContent()
                    }
                }
            }
        }
    }

    /**
     * Measures the time to do composition, measure, layout and draw for the third frame after the
     * focus animation started. This captures the frame cost of the `surface` animation continuing.
     */
    @Test
    fun surface_thirdFrameFocusAnimation() {
        with(benchmarkRule) {
            runBenchmarkFor({ SurfaceTestCase(addSurfaceModifierEnabledByDefault = false) }) {
                measureRepeatedOnUiThread {
                    runWithMeasurementDisabled {
                        doFramesUntilNoChangesPending()

                        // Add surface as modifier
                        getTestCase().toggleState()

                        // Animation with progress == 0 (not expensive)
                        doFrame()

                        // First frame of animation.
                        doFrame()
                    }

                    doFrame()

                    runWithMeasurementDisabled { disposeContent() }
                }
            }
        }
    }

    /**
     * Measures the time to draw for third frame after the focus animation is started. This captures
     * the frame cost of the surface animation continuing.
     */
    @Test
    fun surface_thirdDrawFocusAnimation() {
        with(benchmarkRule) {
            runBenchmarkFor({ SurfaceTestCase(addSurfaceModifierEnabledByDefault = false) }) {
                measureRepeatedOnUiThread {
                    runWithMeasurementDisabled {
                        doFramesUntilNoChangesPending()

                        // Add surface as modifier
                        getTestCase().toggleState()

                        // Animation with progress == 0 (not expensive)
                        doFrame()

                        // First frame of animation.
                        doFrame()

                        recompose()
                        requestLayout()
                        measure()
                        layout()
                        drawPrepare()
                    }

                    draw()

                    runWithMeasurementDisabled {
                        drawFinish()
                        disposeContent()
                    }
                }
            }
        }
    }

    /**
     * Measures the time to process [PressInteraction.Press] and perform composition, measurement,
     * layout, and drawing to render the first frame. This is benchmarked on an already-focused
     * surface to isolate the press state change.
     */
    @Test
    fun surface_firstFramePressAnimation() {
        with(benchmarkRule) {
            runBenchmarkFor({ SurfaceTestCase(addSurfaceModifierEnabledByDefault = true) }) {
                runOnUiThread {
                    // Complete the focus animation frames. (288 frame ~= 2 second in 144fps)
                    doFramesUntilNoChangesPending(maxAmountOfFrames = 288)
                }

                val press = PressInteraction.Press(Offset.Zero)
                measureRepeatedOnUiThread {
                    runBlocking { getTestCase().emitInteraction(press) }

                    doFrame()

                    runWithMeasurementDisabled {
                        // Don't dispose the content to re-use for the next press interaction.
                        // Content will be disposed after `runBenchmarkFor` is completed.

                        runBlocking {
                            // We should reset the press state in surface for the next repeated
                            // measure.
                            getTestCase().emitInteraction(PressInteraction.Cancel(press))
                        }

                        // Run the release animation.
                        doFramesUntilNoChangesPending()
                    }
                }
            }
        }
    }

    /**
     * Measures the time to process [PressInteraction.Release] and perform composition, measurement,
     * layout, and draw to render the first frame. This is benchmarked on an already-focused and
     * pressed surface to isolate the press state change.
     */
    @Test
    fun surface_firstFrameReleaseAnimation() {
        with(benchmarkRule) {
            runBenchmarkFor({ SurfaceTestCase(addSurfaceModifierEnabledByDefault = true) }) {
                runOnUiThread {
                    // Complete the focus animation frames. (288 frame ~= 2 second in 144fps)
                    doFramesUntilNoChangesPending(maxAmountOfFrames = 288)
                }

                val press = PressInteraction.Press(Offset.Zero)
                measureRepeatedOnUiThread {
                    runWithMeasurementDisabled {
                        // Emit interaction to start press animation.
                        runBlocking { getTestCase().emitInteraction(press) }

                        doFramesUntilNoChangesPending()
                    }

                    // Emit interaction to trigger release animation
                    runBlocking { getTestCase().emitInteraction(PressInteraction.Release(press)) }

                    doFrame()

                    runWithMeasurementDisabled {
                        // Don't dispose the content to re-use for the next release interaction.
                        // Content will be disposed after `runBenchmarkFor` is completed.
                        doFramesUntilNoChangesPending()
                    }
                }
            }
        }
    }
}

private class SurfaceTestCase(addSurfaceModifierEnabledByDefault: Boolean) :
    LayeredComposeTestCase(), ToggleableTestCase {

    private val addSurfaceModifier = mutableStateOf(addSurfaceModifierEnabledByDefault)
    private val interactionSource = MutableInteractionSource()

    @Composable
    override fun MeasuredContent() {
        Box(
            modifier =
                Modifier.size(100.dp)
                    .then(
                        if (addSurfaceModifier.value) {
                            Modifier.surface(interactionSource = interactionSource)
                        } else Modifier
                    )
        )
    }

    override fun toggleState() {
        addSurfaceModifier.value = !addSurfaceModifier.value
    }

    suspend fun emitInteraction(interaction: Interaction) {
        interactionSource.emit(interaction)
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        GlimmerTheme { content() }
    }
}
