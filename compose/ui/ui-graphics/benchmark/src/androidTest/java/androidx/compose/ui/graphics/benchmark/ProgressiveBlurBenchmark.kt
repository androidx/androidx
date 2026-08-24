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

package androidx.compose.ui.graphics.benchmark

import android.os.Build
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.blur.BlurStop
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmarks [BlurRadiusSpec.createRenderEffect] and platform effect creation.
 *
 * Simulates the per-draw work behind `Modifier.blur { }` when the blur configuration changes. The
 * blur node builds a fresh effect on each layer block execution. When the value differs from the
 * previous frame, the layer materializes it via `asAndroidRenderEffect()`. Each iteration
 * constructs and materializes a new instance.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class ProgressiveBlurBenchmark {
    @get:Rule val benchmarkRule = BenchmarkRule()

    private val density = Density(2f)
    private val size = Size(300f, 300f)

    private fun stops(count: Int): List<BlurStop> =
        List(count) { i -> BlurStop(fraction = i / (count - 1f), radius = (i * 4).dp) }

    /** Baseline: the uniform fast path (a plain platform blur effect). */
    @Test
    fun resolveUniform() {
        val radius = BlurRadiusSpec.uniform(8.dp)
        benchmarkRule.measureRepeated {
            radius.createRenderEffect(size, density, TileMode.Clamp).asAndroidRenderEffect()
        }
    }

    /** Two-stop vertical gradient (the axis-aligned fast path, no mask). */
    @Test
    fun resolveVerticalGradient() {
        val radius = BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.dp)
        benchmarkRule.measureRepeated {
            radius.createRenderEffect(size, density, TileMode.Clamp).asAndroidRenderEffect()
        }
    }

    /** Multi-stop (mask path) with 3 stops; allocation should match [resolveMultiStop5]. */
    @Test
    fun resolveMultiStop3() {
        val radius = BlurRadiusSpec.verticalGradient(stops(3))
        benchmarkRule.measureRepeated {
            radius.createRenderEffect(size, density, TileMode.Clamp).asAndroidRenderEffect()
        }
    }

    /** Multi-stop (mask path) with 5 stops; allocation should match [resolveMultiStop3]. */
    @Test
    fun resolveMultiStop5() {
        val radius = BlurRadiusSpec.verticalGradient(stops(5))
        benchmarkRule.measureRepeated {
            radius.createRenderEffect(size, density, TileMode.Clamp).asAndroidRenderEffect()
        }
    }

    /** Two-stop radial gradient (mask path). */
    @Test
    fun resolveRadial() {
        val radius = BlurRadiusSpec.radialGradient(startRadius = 0.dp, endRadius = 20.dp)
        benchmarkRule.measureRepeated {
            radius.createRenderEffect(size, density, TileMode.Clamp).asAndroidRenderEffect()
        }
    }

    /**
     * Updates radius uniforms without recompiling the shader.
     *
     * Simulates an animation where the radius changes every frame but the gradient shape remains
     * constant. Reuses the shared program and only updates uniforms.
     */
    @Test
    fun animateRadiusCacheHit() {
        val frames =
            Array(8) {
                BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = (it * 4).dp)
            }
        var i = 0
        benchmarkRule.measureRepeated {
            frames[i++ % frames.size]
                .createRenderEffect(size, density, TileMode.Clamp)
                .asAndroidRenderEffect()
        }
    }

    /**
     * Updates uniform arrays for changing stop counts without recompiling the shader.
     *
     * Simulates changing the stop count every frame. Because the mask program is
     * stop-count-independent, this cost should match [animateRadiusCacheHit].
     */
    @Test
    fun alternateStopCountUniformUpdate() {
        val three = BlurRadiusSpec.verticalGradient(stops(3))
        val five = BlurRadiusSpec.verticalGradient(stops(5))
        var toggle = false
        benchmarkRule.measureRepeated {
            (if (toggle) three else five)
                .createRenderEffect(size, density, TileMode.Clamp)
                .asAndroidRenderEffect()
            toggle = !toggle
        }
    }
}
