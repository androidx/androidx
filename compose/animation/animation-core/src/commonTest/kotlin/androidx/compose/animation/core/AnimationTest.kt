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

package androidx.compose.animation.core

import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals

class AnimationTest {
    @Test
    fun testSnap() {
        val snap1D = VectorizedSnapSpec<AnimationVector1D>(200)
        val snap2D = VectorizedSnapSpec<AnimationVector2D>(2)
        val snap3D = VectorizedSnapSpec<AnimationVector3D>(0)
        val snap4D = VectorizedSnapSpec<AnimationVector4D>(5)

        val start1D = AnimationVector(0f)
        val end1D = AnimationVector(1f)
        assertEquals(start1D, snap1D.getValueFromMillis(0L, start1D, end1D, start1D))
        assertEquals(
            start1D,
            snap1D.getValueFromMillis(snap1D.delayMillis - 1L, start1D, end1D, start1D)
        )
        assertEquals(
            end1D,
            snap1D.getValueFromMillis(snap1D.delayMillis.toLong(), start1D, end1D, start1D)
        )
        assertEquals(
            end1D,
            snap1D.getValueFromMillis(snap1D.delayMillis + 100L, start1D, end1D, start1D)
        )

        val start2D = AnimationVector(0f, 50f)
        val end2D = AnimationVector(1f, -50f)
        assertEquals(start2D, snap2D.getValueFromMillis(0L, start2D, end2D, start2D))
        assertEquals(
            start2D,
            snap2D.getValueFromMillis(snap2D.delayMillis - 1L, start2D, end2D, start2D)
        )
        assertEquals(
            end2D,
            snap2D.getValueFromMillis(snap2D.delayMillis.toLong(), start2D, end2D, start2D)
        )
        assertEquals(
            end2D,
            snap2D.getValueFromMillis(snap2D.delayMillis + 100L, start2D, end2D, start2D)
        )

        val start3D = AnimationVector(0f, 20f, -100f)
        val end3D = AnimationVector(-40f, 0f, 200f)
        assertEquals(
            start3D,
            snap3D.getValueFromMillis(snap3D.delayMillis - 1L, start3D, end3D, start3D)
        )
        assertEquals(
            end3D,
            snap3D.getValueFromMillis(snap3D.delayMillis.toLong(), start3D, end3D, start3D)
        )
        assertEquals(
            end3D,
            snap3D.getValueFromMillis(snap3D.delayMillis + 100L, start3D, end3D, start3D)
        )

        val start4D = AnimationVector(48f, 26f, 88f, 177f)
        val end4D = AnimationVector(64f, 286f, -999f, 40f)
        assertEquals(start4D, snap4D.getValueFromMillis(0L, start4D, end4D, start4D))
        assertEquals(
            start4D,
            snap4D.getValueFromMillis(snap4D.delayMillis - 1L, start4D, end4D, start4D)
        )
        assertEquals(
            end4D,
            snap4D.getValueFromMillis(snap4D.delayMillis.toLong(), start4D, end4D, start4D)
        )
        assertEquals(
            end4D,
            snap4D.getValueFromMillis(snap4D.delayMillis + 100L, start4D, end4D, start4D)
        )
    }

    @Test
    fun testKeyframes() {
        val delay = 120
        val startValue = AnimationVector3D(100f, 200f, 300f)
        val endValue = AnimationVector3D(200f, 100f, 0f)
        val keyframes =
            VectorizedKeyframesSpec<AnimationVector3D>(
                mutableMapOf(
                    0 to (startValue to LinearEasing),
                    100 to (startValue to FastOutLinearInEasing)
                ),
                200,
                delay
            )

        assertEquals(startValue, keyframes.getValueFromMillis(0L, startValue, endValue, startValue))
        assertEquals(
            startValue,
            keyframes.getValueFromMillis(delay.toLong(), startValue, endValue, startValue)
        )
        for (i in 0..200 step 50) {
            val fraction: Float
            if (i <= 100) {
                fraction = 0f
            } else {
                fraction = FastOutLinearInEasing.transform((i - 100) / 100f)
            }
            val animValue =
                AnimationVector(
                    lerp(startValue.v1, endValue.v1, fraction),
                    lerp(startValue.v2, endValue.v2, fraction),
                    lerp(startValue.v3, endValue.v3, fraction)
                )
            assertEquals(
                animValue,
                keyframes.getValueFromMillis(delay + i.toLong(), startValue, endValue, startValue)
            )
        }

        // Test playtime > duration + delay
        assertEquals(endValue, keyframes.getValueFromMillis(500L, startValue, endValue, startValue))
    }

    @Test
    fun testTween() {
        val tween1D = VectorizedTweenSpec<AnimationVector1D>()
        val tween2D = VectorizedTweenSpec<AnimationVector2D>(200, easing = LinearEasing)
        val tween3D =
            VectorizedTweenSpec<AnimationVector3D>(delayMillis = 10, easing = FastOutLinearInEasing)
        val tween4D = VectorizedTweenSpec<AnimationVector4D>()

        // 1D vector
        val start1D = AnimationVector(0f)
        val end1D = AnimationVector(1f)
        assertEquals(start1D, tween1D.getValueFromMillis(0L, start1D, end1D, start1D))
        assertEquals(
            start1D,
            tween1D.getValueFromMillis(tween1D.delayMillis - 1L, start1D, end1D, start1D)
        )
        assertEquals(
            start1D,
            tween1D.getValueFromMillis(tween1D.delayMillis.toLong(), start1D, end1D, start1D)
        )
        val animValue1D =
            AnimationVector(
                lerp(
                    start1D.value,
                    end1D.value,
                    FastOutSlowInEasing.transform(100f / tween1D.durationMillis)
                )
            )
        assertEquals(
            animValue1D,
            tween1D.getValueFromMillis(tween1D.delayMillis + 100L, start1D, end1D, start1D)
        )

        // 2D vector
        val start2D = AnimationVector(0f, 50f)
        val end2D = AnimationVector(1f, -50f)
        assertEquals(start2D, tween2D.getValueFromMillis(0L, start2D, end2D, start2D))
        assertEquals(
            start2D,
            tween2D.getValueFromMillis(tween2D.delayMillis - 1L, start2D, end2D, start2D)
        )
        assertEquals(
            start2D,
            tween2D.getValueFromMillis(tween2D.delayMillis.toLong(), start2D, end2D, start2D)
        )
        val animValue2D =
            AnimationVector(
                lerp(start2D.v1, end2D.v1, 100f / tween2D.durationMillis),
                lerp(start2D.v2, end2D.v2, 100f / tween2D.durationMillis)
            )
        assertEquals(
            animValue2D,
            tween2D.getValueFromMillis(tween2D.delayMillis + 100L, start2D, end2D, start2D)
        )

        // 3D Vector
        val start3D = AnimationVector(0f, 20f, -100f)
        val end3D = AnimationVector(-40f, 0f, 200f)
        assertEquals(start3D, tween3D.getValueFromMillis(0L, start3D, end3D, start3D))
        assertEquals(
            start3D,
            tween3D.getValueFromMillis(tween3D.delayMillis - 1L, start3D, end3D, start3D)
        )
        assertEquals(
            start3D,
            tween3D.getValueFromMillis(tween3D.delayMillis.toLong(), start3D, end3D, start3D)
        )
        val animValue3D =
            AnimationVector(
                lerp(
                    start3D.v1,
                    end3D.v1,
                    FastOutLinearInEasing.transform(100f / tween3D.durationMillis)
                ),
                lerp(
                    start3D.v2,
                    end3D.v2,
                    FastOutLinearInEasing.transform(100f / tween3D.durationMillis)
                ),
                lerp(
                    start3D.v3,
                    end3D.v3,
                    FastOutLinearInEasing.transform(100f / tween3D.durationMillis)
                )
            )
        assertEquals(
            animValue3D,
            tween3D.getValueFromMillis(tween3D.delayMillis + 100L, start3D, end3D, start3D)
        )

        // 4D Vector
        val start4D = AnimationVector(48f, 26f, 88f, 177f)
        val end4D = AnimationVector(64f, 286f, -999f, 40f)
        assertEquals(start4D, tween4D.getValueFromMillis(0L, start4D, end4D, start4D))
        assertEquals(
            start4D,
            tween4D.getValueFromMillis(tween4D.delayMillis - 1L, start4D, end4D, start4D)
        )
        assertEquals(
            start4D,
            tween4D.getValueFromMillis(tween4D.delayMillis.toLong(), start4D, end4D, start4D)
        )
        val animValue4D =
            AnimationVector(
                lerp(
                    start4D.v1,
                    end4D.v1,
                    FastOutSlowInEasing.transform(100f / tween4D.durationMillis)
                ),
                lerp(
                    start4D.v2,
                    end4D.v2,
                    FastOutSlowInEasing.transform(100f / tween4D.durationMillis)
                ),
                lerp(
                    start4D.v3,
                    end4D.v3,
                    FastOutSlowInEasing.transform(100f / tween4D.durationMillis)
                ),
                lerp(
                    start4D.v4,
                    end4D.v4,
                    FastOutSlowInEasing.transform(100f / tween4D.durationMillis)
                )
            )
        assertEquals(
            animValue4D,
            tween4D.getValueFromMillis(tween4D.delayMillis + 100L, start4D, end4D, start4D)
        )
    }

    @Test
    fun testSpringAnimation() {
        val anim3D =
            VectorizedSpringSpec<AnimationVector3D>(
                Spring.DampingRatioHighBouncy,
                Spring.StiffnessMedium,
                null
            )
        val floatAnim = FloatSpringSpec(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium)

        val start = AnimationVector(75f, 1000f, -256f)
        val end = AnimationVector(1296f, -357f, 500f)
        val startVelocity = AnimationVector(0f, 75f, 5f)

        val duration =
            max(
                floatAnim.getDurationMillis(start.v1, end.v1, startVelocity.v1),
                max(
                    floatAnim.getDurationMillis(start.v2, end.v2, startVelocity.v2),
                    floatAnim.getDurationMillis(start.v3, end.v3, startVelocity.v3)
                )
            )
        assertEquals(duration, anim3D.getDurationMillis(start, end, startVelocity))

        for (i in 0..duration step 100) {
            assertEquals(
                AnimationVector(
                    floatAnim.getValueFromMillis(i, start.v1, end.v1, startVelocity.v1),
                    floatAnim.getValueFromMillis(i, start.v2, end.v2, startVelocity.v2),
                    floatAnim.getValueFromMillis(i, start.v3, end.v3, startVelocity.v3)
                ),
                anim3D.getValueFromMillis(i, start, end, startVelocity)
            )
        }
    }

    @Test
    fun testAnimation() {
        val start = AnimationVector(78f, -64f, 1020f, 0f)
        val end = AnimationVector(438f, 554f, -298f, 18f)
        val startVelocity = AnimationVector(0f, 74f, -20f, 0f)

        verifyAnimation(
            VectorizedSpringSpec(
                Spring.DampingRatioLowBouncy,
                Spring.StiffnessLow,
                AnimationVector(5f, 4f, 0.1f, 20f)
            ),
            start,
            end,
            startVelocity
        )

        verifyAnimation(
            VectorizedTweenSpec(1000, easing = FastOutLinearInEasing),
            start,
            end,
            startVelocity
        )

        verifyAnimation(VectorizedSnapSpec(200), start, end, startVelocity)

        verifyAnimation(
            VectorizedKeyframesSpec(mutableMapOf(200 to (start to LinearEasing)), 800),
            start,
            end,
            startVelocity
        )
    }

    @Test
    fun testVectorizedInfiniteRepeatableSpec_velocityOnRepetitions() {
        val repeatableSpec =
            VectorizedInfiniteRepeatableSpec(
                animation = VectorizedAverageVelocitySpec(durationMillis = 1000),
                repeatMode = RepeatMode.Restart,
            )
        val playTimeNanosA = 0L
        val playTimeNanosB = 1_000L * 1_000_000 - 1
        val playTimeNanosC = 1_000L * 1_000_000 + 1

        val vectorStart = AnimationVector(0f)
        val vectorEnd = AnimationVector(3f)
        val vectorV0 = AnimationVector(0f)

        val velocityAtA =
            repeatableSpec.getVelocityFromNanos(
                playTimeNanos = playTimeNanosA,
                initialValue = vectorStart,
                targetValue = vectorEnd,
                initialVelocity = vectorV0
            )

        val velocityAtB =
            repeatableSpec.getVelocityFromNanos(
                playTimeNanos = playTimeNanosB,
                initialValue = vectorStart,
                targetValue = vectorEnd,
                initialVelocity = vectorV0
            )

        val velocityAC =
            repeatableSpec.getVelocityFromNanos(
                playTimeNanos = playTimeNanosC,
                initialValue = vectorStart,
                targetValue = vectorEnd,
                initialVelocity = vectorV0
            )

        assertEquals(vectorV0, velocityAtA)

        // Final velocity will be the final velocity from the average of: [0, X] = 3 pixels/second
        // In other words: 6 pixels/second, or `vectorEnd[0] * 2f`
        // There will be a minor difference since we are measuring one nanosecond before the end
        assertEquals(vectorEnd[0] * 2f, velocityAtB[0], 0.01f)

        // Final velocity of "B" carries over to initial velocity of "C"
        // There will be a minor difference since we are measuring 2 nanoseconds between each other
        assertEquals(velocityAtB[0], velocityAC[0], 0.01f)
    }

    private fun verifyAnimation(
        anim: VectorizedAnimationSpec<AnimationVector4D>,
        start: AnimationVector4D,
        end: AnimationVector4D,
        startVelocity: AnimationVector4D
    ) {
        val fixedAnim =
            TargetBasedAnimation(anim, TwoWayConverter({ it }, { it }), start, end, startVelocity)
        for (playtime in 0..fixedAnim.durationMillis step 100) {
            assertEquals(
                anim.getValueFromMillis(playtime, start, end, startVelocity),
                fixedAnim.getValueFromMillis(playtime)
            )

            assertEquals(
                anim.getVelocityFromNanos(playtime * MillisToNanos, start, end, startVelocity),
                fixedAnim.getVelocityFromMillis(playtime)
            )
        }
        assertEquals(anim.getDurationMillis(start, end, startVelocity), fixedAnim.durationMillis)
    }

    /**
     * [VectorizedDurationBasedAnimationSpec] that promises to maintain the same average velocity
     * based on target/initial value and duration.
     *
     * This means that the instantaneous velocity will also depend on the initial velocity.
     */
    private class VectorizedAverageVelocitySpec<V : AnimationVector>(
        override val durationMillis: Int
    ) : VectorizedDurationBasedAnimationSpec<V> {
        private val durationSeconds = durationMillis.toFloat() / 1_000
        override val delayMillis: Int = 0

        override fun getValueFromNanos(
            playTimeNanos: Long,
            initialValue: V,
            targetValue: V,
            initialVelocity: V
        ): V {
            val playTimeSeconds = (playTimeNanos / 1_000_000).toFloat() / 1_000
            val velocity =
                getVelocityFromNanos(
                    playTimeNanos = playTimeNanos,
                    initialValue = initialValue,
                    targetValue = targetValue,
                    initialVelocity = initialVelocity
                )
            val valueVector = initialValue.newInstance()
            for (i in 0 until velocity.size) {
                valueVector[i] = velocity[i] * playTimeSeconds
            }
            return valueVector
        }

        override fun getVelocityFromNanos(
            playTimeNanos: Long,
            initialValue: V,
            targetValue: V,
            initialVelocity: V
        ): V {
            val playTimeSeconds = (playTimeNanos / 1_000_000).toFloat() / 1_000
            val averageVelocity = initialVelocity.newInstance()
            for (i in 0 until averageVelocity.size) {
                averageVelocity[i] = (targetValue[i] - initialValue[i]) / durationSeconds
            }
            val finalVelocity = initialVelocity.newInstance()
            for (i in 0 until averageVelocity.size) {
                finalVelocity[i] = averageVelocity[i] * 2 - initialVelocity[i]
            }
            val velocityVector = initialVelocity.newInstance()

            for (i in 0 until averageVelocity.size) {
                velocityVector[i] =
                    lerp(
                        start = initialVelocity[i],
                        stop = finalVelocity[i],
                        fraction = playTimeSeconds / durationSeconds
                    )
            }
            return velocityVector
        }
    }
}
