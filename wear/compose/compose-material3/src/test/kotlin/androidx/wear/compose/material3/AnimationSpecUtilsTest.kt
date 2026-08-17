/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.MonotonicFrameClock
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnimationSpecUtilsTest {
    @Test
    fun testFaster() {
        createAnimationSpecs().forEach { spec ->
            val fasterSpec = spec.faster(100f)
            assertEquals(spec.getDuration() / 2f, fasterSpec.getDuration().toFloat(), 10f)
        }
    }

    @Test
    fun testSlower() {
        createAnimationSpecs().forEach { spec ->
            val slowerSpec = spec.slower(50f)
            assertEquals(spec.getDuration() * 2f, slowerSpec.getDuration().toFloat(), 10f)
        }
    }

    @Test
    fun testDelay() {
        createAnimationSpecs().forEach { spec ->
            val delayedSpec = spec.delayMillis(200)
            assertEquals(spec.getDuration() + 200f, delayedSpec.getDuration().toFloat(), 1f)
        }
    }

    @Test
    fun testDuration() {
        createAnimationSpecs().forEach { spec ->
            listOf(0.1f, 0.5f, 1f, 2f, 10f).forEach { speedupFactor ->
                val wrappedSpec = spec.speedFactor(speedupFactor)

                val expected = spec.getDuration() / speedupFactor
                val actual = wrappedSpec.getDuration().toFloat()

                // Tolerance is in nanos, so 10 is not that much
                assertEquals(expected, actual, 10f)
            }
        }
    }

    @Test
    fun testValue() {
        createAnimationSpecs().forEach { spec ->
            listOf(0.1f, 0.5f, 1f, 2f, 10f).forEach { speedupFactor ->
                val wrappedSpec = spec.speedFactor(speedupFactor)

                val duration = spec.getDuration()
                for (i in 0..100) {
                    val expected = spec.at(duration * i / 100)
                    val actual = wrappedSpec.at((duration / speedupFactor * i / 100).toLong())

                    assertEquals(expected, actual, 1e-6f)
                }
            }
        }
    }

    @Test
    fun testSpeed() {
        createAnimationSpecs().forEach { spec ->
            listOf(0.1f, 0.5f, 1f, 2f, 10f).forEach { speedupFactor ->
                val wrappedSpec = spec.speedFactor(speedupFactor)

                val duration = spec.getDuration()
                for (i in 0..100) {
                    val expected = spec.speedAt(duration * i / 100) * speedupFactor
                    val actual = wrappedSpec.speedAt((duration / speedupFactor * i / 100).toLong())

                    // The unit is pixels per second, so this is really small.
                    assertEquals(expected, actual, 0.006f)
                }
            }
        }
    }

    @Test
    fun testThen_stage1_animatesTowardsIntermediateTarget() {
        val initialValue = 0f
        val intermediateTarget = 50f
        val finalTargetValue = 100f

        testAnimationWithClock(
            initialValue = initialValue,
            targetValue = finalTargetValue,
            spec =
                tween<Float>(durationMillis = 100, easing = LinearEasing)
                    .then(
                        nextSpec = tween<Float>(durationMillis = 200, easing = LinearEasing),
                        delayMillis = 100,
                        intermediateTargetProvider = { _, _ -> intermediateTarget },
                    ),
        ) { animatable ->
            start()
            assertEquals(initialValue, animatable.value, 1e-6f)

            advanceTo(50)
            assertEquals(25f, animatable.value, 1e-6f)

            advanceTo(100)
            assertEquals(intermediateTarget, animatable.value, 1e-6f)
        }
    }

    @Test
    fun testThen_stage2_animatesFromCutoffTowardsFinalTarget() {
        val initialValue = 0f
        val intermediateTarget = 40f
        val finalTargetValue = 100f

        testAnimationWithClock(
            initialValue = initialValue,
            targetValue = finalTargetValue,
            spec =
                tween<Float>(durationMillis = 200, easing = LinearEasing)
                    .then(
                        nextSpec = tween<Float>(durationMillis = 300, easing = LinearEasing),
                        delayMillis = 100,
                        intermediateTargetProvider = { _, _ -> intermediateTarget },
                    ),
        ) { animatable ->
            start()
            advanceTo(250)
            assertEquals(60f, animatable.value, 1e-6f)

            advanceTo(400)
            assertEquals(finalTargetValue, animatable.value, 1e-6f)
        }
    }

    @Test
    fun testThen_velocityContinuityAtSwitchTime() {
        val initialValue = 0f
        val intermediateTarget = 50f
        val finalTargetValue = 100f

        val firstSpec = spring<Float>(dampingRatio = 0.5f, stiffness = 800f)
        val secondSpec = spring<Float>(dampingRatio = 0.8f, stiffness = 1500f)
        val chainedSpec =
            firstSpec.then(
                nextSpec = secondSpec,
                delayMillis = 100,
                intermediateTargetProvider = { _, _ -> intermediateTarget },
            )

        val stage1VelocityAtCutoff =
            firstSpec.speedAtMs(
                timeMillis = 100,
                initialValue = initialValue,
                targetValue = intermediateTarget,
            )
        val chainedVelocityAtCutoff =
            chainedSpec.speedAtMs(
                timeMillis = 100,
                initialValue = initialValue,
                targetValue = finalTargetValue,
            )

        assertEquals(stage1VelocityAtCutoff, chainedVelocityAtCutoff, 1e-6f)
    }

    @Test
    fun testThen_durationMatchesSwitchTimePlusStage2Duration() {
        val initialValue = 0f
        val intermediateTarget = 40f
        val finalTargetValue = 100f

        val firstSpec = spring<Float>(stiffness = 300f)
        val secondSpec = tween<Float>(durationMillis = 400, easing = FastOutSlowInEasing)
        val chainedSpec =
            firstSpec.then(
                nextSpec = secondSpec,
                delayMillis = 150,
                intermediateTargetProvider = { _, _ -> intermediateTarget },
            )

        // Total duration should equal switch delayMillis (150ms) + Stage 2 duration (400ms) = 550ms
        assertEquals(
            550f,
            chainedSpec
                .getDuration(initialValue = initialValue, targetValue = finalTargetValue)
                .toFloat(),
            1e-5f,
        )
    }

    @Test
    fun testThen_multiStageChaining() {
        val initialValue = 0f
        val intermediateTarget1 = 40f
        val intermediateTarget2 = 80f
        val finalTargetValue = 100f

        testAnimationWithClock(
            initialValue = initialValue,
            targetValue = finalTargetValue,
            spec =
                tween<Float>(durationMillis = 200, easing = LinearEasing)
                    .then(
                        nextSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
                        delayMillis = 100,
                        intermediateTargetProvider = { _, _ -> intermediateTarget1 },
                    )
                    .then(
                        nextSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                        delayMillis = 250,
                        intermediateTargetProvider = { _, _ -> intermediateTarget2 },
                    ),
        ) { animatable ->
            start()
            assertEquals(initialValue, animatable.value, 1e-6f)

            advanceTo(100)
            assertEquals(20f, animatable.value, 1e-6f)

            advanceTo(550)
            assertEquals(finalTargetValue, animatable.value, 1e-6f)
        }
    }

    @Test
    fun testThen_reusingSpecAcrossDifferentAnimations() {
        val startValue = 0f
        val endValue = 100f

        val firstSpec = tween<Float>(durationMillis = 200, easing = LinearEasing)
        val secondSpec = tween<Float>(durationMillis = 200, easing = LinearEasing)
        val chainedSpec =
            firstSpec.then(
                nextSpec = secondSpec,
                delayMillis = 100,
                intermediateTargetProvider = { initial, target -> (initial + target) / 2f },
            )

        testAnimationWithClock(
            initialValue = startValue,
            targetValue = endValue,
            spec = chainedSpec,
        ) { animatable ->
            start()
            assertEquals(startValue, animatable.value, 1e-6f)

            advanceTo(100) // Cutoff time
            assertEquals(25f, animatable.value, 1e-5f)

            advanceTo(300) // Full duration (100ms + 200ms)
            assertEquals(endValue, animatable.value, 1e-5f)
        }

        testAnimationWithClock(
            initialValue = endValue,
            targetValue = startValue,
            spec = chainedSpec,
        ) { animatable ->
            start()
            assertEquals(endValue, animatable.value, 1e-6f)

            advanceTo(100) // Cutoff time for second run
            assertEquals(75f, animatable.value, 1e-5f)

            advanceTo(300) // Full duration for second run
            assertEquals(startValue, animatable.value, 1e-5f)
        }
    }

    @Test
    fun testThen_matchesCoroutineSequentialAnimations() = runBlocking {
        val initialValue = 0f
        val intermediateTarget = 40f
        val finalTargetValue = 100f

        val firstSpec = spring<Float>(dampingRatio = 0.6f, stiffness = 800f)
        val secondSpec = tween<Float>(durationMillis = 350, easing = FastOutSlowInEasing)
        val chainedSpec =
            firstSpec.then(
                nextSpec = secondSpec,
                delayMillis = 100,
                intermediateTargetProvider = { _, _ -> intermediateTarget },
            )

        val clock = TestFrameClock()
        withContext(clock) {
            val manualAnimatable = Animatable(initialValue)
            val chainedAnimatable = Animatable(initialValue)

            // Launch single .animateTo call using chainedSpec
            val chainedJob = launch {
                chainedAnimatable.animateTo(
                    targetValue = finalTargetValue,
                    animationSpec = chainedSpec,
                )
            }

            // Launch manual sequential animation for Stage 1 towards intermediateTarget (40f)
            var manualJob = launch {
                manualAnimatable.animateTo(
                    targetValue = intermediateTarget,
                    animationSpec = firstSpec,
                )
            }

            // Advance frames from 0ms to 100ms (Stage 1 cutoff)
            for (ms in 0..100 step 1) {
                clock.frame(TimeUnit.MILLISECONDS.toNanos(ms.toLong()))
                assertEquals(
                    "Position mismatch at Stage 1 t=$ms ms",
                    manualAnimatable.value,
                    chainedAnimatable.value,
                    1e-3f,
                )
            }

            // At t = 100ms: Start Stage 2 animateTo towards final target (100f) using secondSpec
            manualJob.cancel()
            manualJob = launch {
                manualAnimatable.animateTo(
                    targetValue = finalTargetValue,
                    animationSpec = secondSpec,
                )
            }

            // Advance frames from 110ms to 500ms (Stage 2 playback)
            for (ms in 101..500 step 1) {
                clock.frame(TimeUnit.MILLISECONDS.toNanos(ms.toLong()))
                assertEquals(
                    "Position mismatch at Stage 2 t=$ms ms",
                    manualAnimatable.value,
                    chainedAnimatable.value,
                    1e-3f,
                )
            }

            manualJob.cancel()
            chainedJob.cancel()
        }
    }

    @Test
    fun testThen_vectorizedSpecDirectReuseWithDifferentTargets() {
        val firstSpec = tween<Float>(durationMillis = 200, easing = LinearEasing)
        val secondSpec = tween<Float>(durationMillis = 200, easing = LinearEasing)
        val chainedSpec =
            firstSpec.then(
                nextSpec = secondSpec,
                delayMillis = 100,
                intermediateTargetProvider = { initial, target -> (initial + target) / 2f },
            )
        val vectorized = chainedSpec.vectorize(Float.VectorConverter)

        // First call with target = 100f (intermediate is 50f; at 100ms cutoff value is 25f)
        // At t = 200ms (100ms into Stage 2 from 25f to 100f) -> 25 + 0.5 * 75 = 62.5f
        val v1 =
            Float.VectorConverter.convertFromVector(
                vectorized.getValueFromNanos(
                    TimeUnit.MILLISECONDS.toNanos(200),
                    0f.toVector(),
                    100f.toVector(),
                    0f.toVector(),
                )
            )
        assertEquals(62.5f, v1, 1e-5f)

        // Reusing the same vectorized instance with new target = 200f (intermediate is 100f; at
        // 100ms cutoff value is 50f)
        // At t = 200ms (100ms into Stage 2 from 50f to 200f) -> 50 + 0.5 * 150 = 125f
        val v2 =
            Float.VectorConverter.convertFromVector(
                vectorized.getValueFromNanos(
                    TimeUnit.MILLISECONDS.toNanos(200),
                    0f.toVector(),
                    200f.toVector(),
                    0f.toVector(),
                )
            )
        assertEquals(125f, v2, 1e-5f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testThen_negativeDelayThrows() {
        tween<Float>(durationMillis = 100)
            .then(
                nextSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                delayMillis = -1,
                intermediateTargetProvider = { _, _ -> 50f },
            )
    }

    private fun Float.toVector() = Float.VectorConverter.convertToVector(this)

    private fun AnimationVector1D.toFloat() = Float.VectorConverter.convertFromVector(this)

    private fun AnimationSpec<Float>.getDuration(
        initialValue: Float = 0f,
        targetValue: Float = 1f,
        initialVelocity: Float = 0f,
    ) =
        vectorize(Float.VectorConverter)
            .getDurationNanos(
                initialValue.toVector(),
                targetValue.toVector(),
                initialVelocity.toVector(),
            ) / 1_000_000

    private fun AnimationSpec<Float>.at(time: Long) =
        vectorize(Float.VectorConverter)
            .getValueFromNanos(time, 0f.toVector(), 1f.toVector(), 0f.toVector())
            .toFloat()

    private fun AnimationSpec<Float>.speedAt(time: Long) =
        vectorize(Float.VectorConverter)
            .getVelocityFromNanos(time, 0f.toVector(), 1f.toVector(), 0f.toVector())
            .toFloat()

    private fun AnimationSpec<Float>.speedAtMs(
        timeMillis: Long,
        initialValue: Float = 0f,
        targetValue: Float = 1f,
        initialVelocity: Float = 0f,
    ) =
        vectorize(Float.VectorConverter)
            .getVelocityFromNanos(
                TimeUnit.MILLISECONDS.toNanos(timeMillis),
                initialValue.toVector(),
                targetValue.toVector(),
                initialVelocity.toVector(),
            )
            .toFloat()

    private fun createAnimationSpecs() =
        buildList<FiniteAnimationSpec<Float>> {
            listOf(0.2f, 0.4f, 0.8f, 1f).forEach { damping ->
                listOf(50f, 200f, 400f, 1500f, 10_000f).forEach { stiffness ->
                    listOf(0.01f, 0.001f, 0.0001f).forEach { threshold ->
                        add(spring(damping, stiffness, threshold))
                    }
                }
            }
            listOf(0, 100, 300, 1000).forEach { duration ->
                listOf(0, 100, 500, 1500).forEach { delay ->
                    listOf(
                            FastOutSlowInEasing,
                            LinearOutSlowInEasing,
                            FastOutLinearInEasing,
                            LinearEasing,
                        )
                        .forEach { easing -> add(tween(duration, delay, easing)) }
                }
            }
        }

    private fun testAnimationWithClock(
        initialValue: Float = 0f,
        targetValue: Float,
        spec: AnimationSpec<Float>,
        block: suspend TestAnimationScope.(Animatable<Float, AnimationVector1D>) -> Unit,
    ) = runBlocking {
        val clock = TestFrameClock()
        withContext(clock) {
            val animatable = Animatable(initialValue)
            val job = launch { animatable.animateTo(targetValue, animationSpec = spec) }
            try {
                TestAnimationScope(clock).block(animatable)
            } finally {
                job.cancel()
            }
        }
    }

    private class TestAnimationScope(private val clock: TestFrameClock) {
        suspend fun start() {
            advanceTo(0)
        }

        suspend fun advanceTo(timeMillis: Long) {
            clock.frame(TimeUnit.MILLISECONDS.toNanos(timeMillis))
            yield()
        }
    }

    private class TestFrameClock : MonotonicFrameClock {
        private val frameCh = Channel<Long>(Channel.UNLIMITED)

        suspend fun frame(frameTimeNanos: Long) {
            frameCh.send(frameTimeNanos)
        }

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
            onFrame(frameCh.receive())
    }
}
