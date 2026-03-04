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

package androidx.compose.animation.benchmark

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.graphics.painter.Painter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AnimatedImageVectorBenchmark {

    @get:Rule val rule = ComposeBenchmarkRule()

    /**
     * Measures the cost of creating a new
     * [androidx.compose.animation.graphics.vector.AnimatedImageVector] instance. Note that this
     * benchmark uses a very small and simple animated image vector to try to measure purely system
     * performance.
     */
    @Test
    fun createAnimatedImageVector() {
        rule.benchmarkFirstCompose(::AnimatedImageVectorInstantiationTestCase)
    }

    /**
     * Measures the recomposition cost of the Image composable reacting to a new
     * [androidx.compose.animation.graphics.vector.AnimatedImageVector] painter. Note that this
     * benchmark uses a very small and simple animated image vector to try to measure purely system
     * performance.
     */
    @Test
    fun recomposeAnimatedImageVector() {
        rule.toggleStateBenchmarkRecompose(
            ::AnimatedImageVectorPaintImageTestCase,
            assertOneRecomposition = false,
        )
    }

    /**
     * Measures the cost of drawing an
     * [androidx.compose.animation.graphics.vector.AnimatedImageVector]. Note that this benchmark
     * uses a very small and simple animated image vector to try to measure purely system
     * performance.
     */
    @Test
    fun drawAnimatedImageVector() {
        rule.toggleStateBenchmarkDraw(
            ::AnimatedImageVectorPaintImageTestCase,
            assertOneRecomposition = false,
        )
    }

    /**
     * Measures the recomposition cost of rememberAnimatedImageVector. Note that this benchmark uses
     * a very small and simple animated image vector to try to measure purely system performance.
     */
    @Test
    fun recomposeRememberAnimatedImageVector() {
        rule.toggleStateBenchmarkRecompose(
            ::AnimatedImageVectorRememberTestCase,
            assertOneRecomposition = false,
        )
    }
}

private class AnimatedImageVectorInstantiationTestCase : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        val image = AnimatedImageVector.animatedVectorResource(R.drawable.small_animated_vector)
    }
}

private class AnimatedImageVectorPaintImageTestCase : LayeredComposeTestCase(), ToggleableTestCase {
    private var state by mutableStateOf(false)

    private lateinit var painter: Painter

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        val image = AnimatedImageVector.animatedVectorResource(R.drawable.small_animated_vector)
        painter = rememberAnimatedVectorPainter(image, state)

        content()
    }

    @Composable
    override fun MeasuredContent() {
        Image(painter = painter, contentDescription = "Triangle")
    }

    override fun toggleState() {
        state = !state
    }
}

private class AnimatedImageVectorRememberTestCase : LayeredComposeTestCase(), ToggleableTestCase {
    private var state by mutableStateOf(false)

    private lateinit var image: AnimatedImageVector

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        image = AnimatedImageVector.animatedVectorResource(R.drawable.small_animated_vector)

        content()
    }

    @Composable
    override fun MeasuredContent() {
        val painter = rememberAnimatedVectorPainter(image, state)
        Image(painter = painter, contentDescription = "Triangle")
    }

    override fun toggleState() {
        state = !state
    }
}
