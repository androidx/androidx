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

package androidx.compose.foundation.anchoredDraggable

import androidx.compose.animation.core.FloatDecayAnimationSpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateToWithDecay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.testutils.createParameterizedComposeTestRule
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
class AnchoredDraggableDecayAnimationTest(testNewBehavior: Boolean) :
    AnchoredDraggableBackwardsCompatibleTest(testNewBehavior) {

    @get:Rule val parameterizedRule = createParameterizedComposeTestRule<Params>()

    private val AllAnchorConfigurations =
        listOf(
            // Smaller anchor -> bigger anchor
            Params(from = 200f, to = 300f),
            // Bigger anchor -> smaller anchor
            Params(from = 300f, to = 200f),
            // Smaller anchor -> bigger anchor with overshoot
            Params(from = 200f, to = 300f, overshoot = 3f),
            // Bigger anchor -> smaller anchor with overshoot
            Params(from = 300f, to = 200f, overshoot = 3f),
            // Negative -> positive
            Params(from = -200f, to = 300f),
            // Positive -> negative
            Params(from = 200f, to = -300f),
        )

    @Test
    fun anchoredDraggable_animateToWithDecay() {
        lateinit var scope: CoroutineScope
        parameterizedRule.setContent { scope = rememberCoroutineScope() }
        parameterizedRule.forEachParameter(AllAnchorConfigurations) { parameters ->
            val offsets = mutableListOf<Float>()
            val decaySpec =
                createFakeDecayAnimationSpec(
                    from = parameters.from,
                    to = parameters.to + parameters.directionalOvershoot
                )
            val generatedSpec = decaySpec.generateDecayAnimationSpec<Float>()
            val state by
                mutableStateOf(
                    createAnchoredDraggableState(
                        initialValue = parameters.anchors.closestAnchor(parameters.from)!!,
                        positionalThreshold = defaultPositionalThreshold,
                        velocityThreshold = defaultVelocityThreshold,
                        anchors = parameters.anchors,
                        decayAnimationSpec = generatedSpec,
                        snapAnimationSpec = defaultAnimationSpec
                    )
                )

            val offsetObservation =
                scope.launch {
                    snapshotFlow { state.offset }
                        .collect { latestOffset -> offsets.add(latestOffset) }
                }
            assertThat(state.offset).isEqualTo(parameters.from)

            scope.launch {
                state.animateToWithDecay(
                    targetValue = state.anchors.closestAnchor(parameters.to)!!,
                    velocity = defaultVelocityThreshold() * 10f * parameters.direction,
                    snapAnimationSpec = defaultAnimationSpec,
                    decayAnimationSpec = generatedSpec
                )
            }
            parameterizedRule.mainClock.advanceTimeUntil {
                offsets.size == decaySpec.values.toTypedArray().size
            }

            assertThat(offsets).containsExactlyElementsIn(decaySpec.values.toTypedArray())
            assertThat(state.offset).isEqualTo(parameters.to)

            offsets.clear()
            offsetObservation.cancel()
        }
    }

    private val defaultPositionalThreshold: (totalDistance: Float) -> Float = {
        with(rule.density) { 56.dp.toPx() }
    }
    private val defaultVelocityThreshold: () -> Float = {
        with(parameterizedRule.density) { 125.dp.toPx() }
    }
    private val defaultAnimationSpec = tween<Float>()

    /**
     * Params for this test case, as well as anchors and additional information derived.
     *
     * @param from The origin of the animation
     * @param to The target of the animation
     * @param overshoot How much the animation should overshoot, if at all. Should be a positive
     *   float value. Use [directionalOvershoot] to access the overshoot with directionality
     *   considered.
     */
    data class Params(val from: Float, val to: Float, val overshoot: Float = 0f) {
        val direction = sign(to - from)
        val directionalOvershoot = overshoot * direction
        val anchors = DraggableAnchors {
            ParameterizedTestAnchorValue.Start at from
            ParameterizedTestAnchorValue.End at to
        }
    }

    enum class ParameterizedTestAnchorValue {
        Start,
        End
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "testNewBehavior={0}")
        fun params() = listOf(false, true)
    }
}

private fun createFakeDecayAnimationSpec(
    from: Float,
    to: Float,
    stepSize: Int = 10
): FakeDecayAnimationSpec {
    val distance = abs(from - to).roundToInt()
    val direction = sign(to - from)
    val steps = distance / stepSize
    val values = FloatArray(steps + 1)
    val velocities = FloatArray(steps + 1)
    values[0] = from
    velocities[0] = from
    for (i in 1..steps) {
        values[i] = from + i * stepSize * direction
        velocities[i] = from + i * stepSize * direction
    }
    return FakeDecayAnimationSpec(values, velocities)
}

private class FakeDecayAnimationSpec(val values: FloatArray, val velocities: FloatArray) :
    FloatDecayAnimationSpec {

    init {
        assert(values.size == velocities.size) {
            "Expected values (size ${values.size}) and velocities (size ${velocities.size} to" +
                " be of same size."
        }
    }

    val frameLengthNanos = 16L * 1e+6

    private fun getFrameForPlayTime(playTimeNanos: Long) =
        (playTimeNanos / frameLengthNanos).roundToInt()

    override val absVelocityThreshold: Float = 0.1f

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        initialVelocity: Float
    ) = values[getFrameForPlayTime(playTimeNanos)]

    override fun getDurationNanos(initialValue: Float, initialVelocity: Float) =
        ((values.size - 1) * frameLengthNanos).toLong()

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        initialVelocity: Float
    ) = getFrameForPlayTime(playTimeNanos).toFloat()

    override fun getTargetValue(initialValue: Float, initialVelocity: Float) = values.last()
}
