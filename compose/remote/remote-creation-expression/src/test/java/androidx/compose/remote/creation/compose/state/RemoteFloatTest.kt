/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.remote.creation.compose.state

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.RemoteContext.ID_CONTINUOUS_SEC
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.Utils.asNan
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteFloatTest {
    val context = TestRemoteContext()
    val time = createNamedRemoteFloat("time", 100f).createReference()
    val JUN_06_2025_UTC =
        RemoteLong(
            LocalDateTime.parse("2025-06-06T01:02:03")
                .atZone(ZoneOffset.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
    lateinit var creationState: RemoteComposeCreationState

    @Before
    fun setUp() {
        creationState = testCreationState()
    }

    @Test
    fun addition() {
        val sum = RemoteFloat(100f) + RemoteFloat(20f) + RemoteFloat(3f)
        val sumId = sum.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(sumId)).isEqualTo(123f)
    }

    @Test
    fun addition_remoteFloatAndFloat() {
        val sum = RemoteFloat(100f) + 20f
        val sumId = sum.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(sumId)).isEqualTo(120f)
    }

    @Test
    fun expressionsWithSameLoweredArray_areWrittenOnce() {
        val x = createNamedRemoteFloat("x", 1f)
        val y = createNamedRemoteFloat("y", 1f)
        fun expression(operand: RemoteFloat, operator: Float) =
            RemoteFloat(null, RemoteStateInstanceKey()) { creationState ->
                floatArrayOf(operand.getFloatIdForCreationState(creationState), 2f, operator)
            }

        val xPlus2 = expression(x, AnimatedFloatExpression.ADD).getIdForCreationState(creationState)
        val xPlus2Dup =
            expression(x, AnimatedFloatExpression.ADD).getIdForCreationState(creationState)
        val xTimes2 =
            expression(x, AnimatedFloatExpression.MUL).getIdForCreationState(creationState)
        val yPlus2 = expression(y, AnimatedFloatExpression.ADD).getIdForCreationState(creationState)

        assertThat(xPlus2Dup).isEqualTo(xPlus2)
        // Operators and variable ids are both NaN-encoded, so these must not be deduplicated.
        assertThat(xTimes2).isNotEqualTo(xPlus2)
        assertThat(yPlus2).isNotEqualTo(xPlus2)
    }

    @Test
    fun animatedExpressionsWithSameLoweredArray_areWrittenOnce() {
        val x = createNamedRemoteFloat("x", 1f)
        val anim = floatArrayOf(1f, CUBIC_STANDARD.toFloat())

        val first = animatedRemoteFloat(x, anim.copyOf()).getIdForCreationState(creationState)
        val second = animatedRemoteFloat(x, anim.copyOf()).getIdForCreationState(creationState)
        val other =
            animatedRemoteFloat(x, floatArrayOf(2f, CUBIC_STANDARD.toFloat()))
                .getIdForCreationState(creationState)

        assertThat(second).isEqualTo(first)
        assertThat(other).isNotEqualTo(first)
    }

    @Test
    fun toRemoteInt() {
        val sum = (RemoteFloat(100f) + RemoteFloat(20f) + RemoteFloat(3f)).toRemoteInt()
        val sumId = sum.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(sumId)).isEqualTo(123)
    }

    @Test
    fun selectIfLt_less() {
        val result =
            selectIfLt(RemoteFloat(1f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(100f)
    }

    @Test
    fun selectIfLt_equal() {
        val result =
            selectIfLt(RemoteFloat(2f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(200f)
    }

    @Test
    fun selectIfLt_greater() {
        val result =
            selectIfLt(RemoteFloat(3f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfLe_less() {
        val result =
            selectIfLe(RemoteFloat(1f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfLe_equal() {
        val result =
            selectIfLe(RemoteFloat(2f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfLe_greater() {
        val result =
            selectIfLe(RemoteFloat(2f), RemoteFloat(1f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfGt_less() {
        val result =
            selectIfGt(RemoteFloat(1f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfGt_equal() {
        val result =
            selectIfGt(RemoteFloat(2f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfGt_greater() {
        val result =
            selectIfGt(RemoteFloat(3f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfGe_less() {
        val result =
            selectIfGe(RemoteFloat(1f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfGe_equal() {
        val result =
            selectIfGe(RemoteFloat(2f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfGe_greater() {
        val result =
            selectIfGe(RemoteFloat(2f), RemoteFloat(1f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(100)
    }

    @Test
    fun unaryMinus() {
        val result = -RemoteFloat(2.5f)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(-2.5f)
    }

    @Test
    fun unaryMinus_complexExpression() {
        // Create a complex float expression
        var complexFloat = RemoteFloat(0f)
        for (i in 1..50) {
            complexFloat += RemoteFloat(i.toFloat())
        }

        val result = -complexFloat

        // Assertions
        val finalArray = result.arrayForCreationState(creationState)
        assertThat(finalArray.size < 20).isTrue()

        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()
        val expected = -((50 * 51) / 2f)
        assertThat(context.getFloat(resultId)).isEqualTo(expected)
    }

    @Test
    fun ceil() {
        val a = RemoteFloat(10.4f)
        val a2 = ceil(a)
        val a2id = a2.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(a2id)).isEqualTo(11.0f)
    }

    @Test
    fun floor() {
        val a = RemoteFloat(10.4f)
        val a2 = floor(a)
        val a2id = a2.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(a2id)).isEqualTo(10.0f)
    }

    @Test
    fun round() {
        val a = RemoteFloat(10.4f)
        val b = RemoteFloat(10.6f)
        val a2 = round(a)
        val b2 = round(b)
        val a2id = a2.getIdForCreationState(creationState)
        val b2id = b2.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(a2id)).isEqualTo(10.0f)
        assertThat(context.getFloat(b2id)).isEqualTo(11.0f)
    }

    @Test
    fun clamp_low() {
        val min = RemoteFloat(10.5f)
        val max = RemoteFloat(20.5f)
        val value = RemoteFloat(1.5f)
        val result = clamp(value = value, min = min, max = max)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(10.5f)
    }

    @Test
    fun clamp_mid() {
        val min = RemoteFloat(10.5f)
        val max = RemoteFloat(20.5f)
        val value = RemoteFloat(11.5f)
        val result = clamp(value = value, min = min, max = max)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(11.5f)
    }

    @Test
    fun clamp_high() {
        val min = RemoteFloat(10.5f)
        val max = RemoteFloat(20.5f)
        val value = RemoteFloat(21.5f)
        val result = clamp(value = value, min = min, max = max)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(20.5f)
    }

    @Test
    fun clamp_low_floatMinMax() {
        val value = RemoteFloat(1.5f)
        val result = clamp(value = value, min = 10.5f, max = 20.5f)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(10.5f)
    }

    @Test
    fun clamp_mid_floatMinMax() {
        val value = RemoteFloat(11.5f)
        val result = clamp(value = value, min = 10.5f, max = 20.5f)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(11.5f)
    }

    @Test
    fun clamp_high_floatMinMax() {
        val value = RemoteFloat(21.5f)
        val result = clamp(value = value, min = 10.5f, max = 20.5f)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(20.5f)
    }

    @Test
    fun constantValue_constant() {
        assertThat(((RemoteFloat(10f) - RemoteFloat(5f)) * RemoteFloat(3f)).constantValue)
            .isEqualTo(15f)
    }

    @Test
    fun constantValue_notConstant() {
        assertThat((RemoteFloat(10f) - RemoteTimeVariables.continuousSeconds).constantValueOrNull)
            .isNull()
    }

    @Test
    fun one_divided_by() {
        val result = RemoteFloat(1f) / RemoteFloat(2f)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(0.5f)
    }

    @Test
    fun toRemoteIntRoundTripRounding_static() {
        val result = RemoteFloat(-3.9f).toRemoteInt().toRemoteFloat()
        assertThat(result.constantValue).isEqualTo(-3.0f)
    }

    @Test
    fun toRemoteIntRoundTripRounding_dynamic() {
        val result = createNamedRemoteFloat("testValue", -3.9f).toRemoteInt().toRemoteFloat()
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(-3.0f)
    }

    @Test
    fun hasConstantValue_true() {
        assertThat(RemoteFloat(21.5f).hasConstantValue).isTrue()
        assertThat(RemoteFloat(21.5f).plus(RemoteFloat(21.5f)).hasConstantValue).isTrue()
        assertThat(RemoteFloat(21.5f).times(RemoteFloat(21.5f)).hasConstantValue).isTrue()
        assertThat(RemoteFloat(21.5f).minus(RemoteFloat(21.5f)).hasConstantValue).isTrue()
        assertThat(RemoteFloat(21.5f).div(RemoteFloat(21.5f)).hasConstantValue).isTrue()
        assertThat(clamp(value = RemoteFloat(21.5f), min = 10.5f, max = 20.5f).hasConstantValue)
            .isTrue()
        assertThat(
                selectIfGt(RemoteFloat(3f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
                    .hasConstantValue
            )
            .isTrue()
        assertThat(RemoteFloat(21.5f).plus(RemoteInt(10).toRemoteFloat()).hasConstantValue).isTrue()
        assertThat(RemoteInt(10).toRemoteFloat().hasConstantValue).isTrue()
    }

    @Test
    fun hasConstantValue_false() {
        assertThat(RemoteTimeVariables.continuousSeconds.hasConstantValue).isFalse()
        assertThat(RemoteFloat(21.5f).plus(RemoteTimeVariables.continuousSeconds).hasConstantValue)
            .isFalse()
        assertThat(createNamedRemoteFloat("value", 1f).hasConstantValue).isFalse()
    }

    @Test
    fun lerp() {
        val from = RemoteFloat(100f)
        val to = RemoteFloat(200f)
        val result0 = lerp(from, to, RemoteFloat(0f))
        val result0_25 = lerp(from, to, RemoteFloat(0.25f))
        val result0_5 = lerp(from, to, RemoteFloat(0.5f))
        val result0_75 = lerp(from, to, RemoteFloat(0.75f))
        val result1 = lerp(from, to, RemoteFloat(1f))
        val result0Id = result0.getIdForCreationState(creationState)
        val result0_25Id = result0_25.getIdForCreationState(creationState)
        val result0_5Id = result0_5.getIdForCreationState(creationState)
        val result0_75Id = result0_75.getIdForCreationState(creationState)
        val result1Id = result1.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(result0Id)).isEqualTo(100f)
        assertThat(context.getFloat(result0_25Id)).isEqualTo(125f)
        assertThat(context.getFloat(result0_5Id)).isEqualTo(150f)
        assertThat(context.getFloat(result0_75Id)).isEqualTo(175f)
        assertThat(context.getFloat(result1Id)).isEqualTo(200f)
    }

    @Test
    fun cubicEasing_constant() {
        // Linear: 1, 1, 0, 0
        val x1 = RemoteFloat(1f)
        val y1 = RemoteFloat(1f)
        val x2 = RemoteFloat(0f)
        val y2 = RemoteFloat(0f)
        val progress = RemoteFloat(0.5f)
        val result = cubicEasing(x1, y1, x2, y2, progress)

        assertThat(result.constantValue).isWithin(0.01f).of(0.5f)
    }

    @Test
    fun cubicEasing_expression() {
        val x1 = RemoteFloat(1f)
        val y1 = RemoteFloat(1f)
        val x2 = RemoteFloat(0f)
        val y2 = RemoteFloat(0f)
        val progress = RemoteTimeVariables.continuousSeconds // non-constant
        val result = cubicEasing(x1, y1, x2, y2, progress)
        val resultId = result.getIdForCreationState(creationState)

        makeAndUpdateCoreDocument() { context.loadFloat(RemoteContext.ID_CONTINUOUS_SEC, 0.5f) }

        assertThat(context.getFloat(resultId)).isWithin(0.01f).of(0.5f)
    }

    @Test
    fun evalSpline_constant() {
        val points = RemoteFloatArray(listOf(0f.rf, 0f.rf, 1f.rf, 1f.rf))
        val result = evalSpline(points, loop = false, progress = 0.5f.rf)

        makeAndPaintCoreDocument()

        assertThat(result.constantValue).isWithin(0.01f).of(0.5f)
    }

    @Test
    fun evalSpline_expression() {
        val points = RemoteFloatArray(listOf(0f.rf, 0f.rf, 1f.rf, 1f.rf))
        val progress = RemoteTimeVariables.continuousSeconds // non-constant
        val result = evalSpline(points, loop = false, progress)
        val resultId = result.getIdForCreationState(creationState)

        makeAndUpdateCoreDocument() { context.loadFloat(RemoteContext.ID_CONTINUOUS_SEC, 0.5f) }

        assertThat(context.getFloat(resultId)).isWithin(0.01f).of(0.5f)
    }

    @Test
    fun evalSpline_loop_constant() {
        val points = RemoteFloatArray(listOf(0f.rf, 1f.rf, 0f.rf))
        // 1.5 should be equivalent to 0.5 because of loop
        val result = evalSpline(points, loop = true, progress = 1.5f.rf)

        assertThat(result.constantValue).isWithin(0.01f).of(1.0f)
    }

    @Test
    fun evalSpline_loop_expression() {
        val points = RemoteFloatArray(listOf(0f.rf, 1f.rf, 0f.rf))
        val progress = RemoteTimeVariables.continuousSeconds // non-constant
        val result = evalSpline(points, loop = true, progress)
        val resultId = result.getIdForCreationState(creationState)

        makeAndUpdateCoreDocument() {
            // 1.5 should be equivalent to 0.5 because of loop
            context.loadFloat(RemoteContext.ID_CONTINUOUS_SEC, 1.5f)
        }

        // For (0, 1, 0) at 0.5, it should be 1.0
        assertThat(context.getFloat(resultId)).isWithin(0.01f).of(1.0f)
    }

    @Test
    fun toDeg() {
        val rad = RemoteFloat(Math.PI.toFloat())
        val deg = toDeg(rad)
        val degId = deg.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(degId)).isEqualTo(180f)
    }

    @Test
    fun toRad() {
        val deg = RemoteFloat(180f)
        val rad = toRad(deg)
        val radId = rad.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(radId)).isEqualTo(Math.PI.toFloat())
    }

    @Test
    fun interpolateRemoteFloat_linear() {
        val result =
            interpolateRemoteFloat(
                progress = RemoteFloat(0.5f),
                initialValue = RemoteFloat(100f),
                targetValue = RemoteFloat(200f),
                CUBIC_LINEAR,
            )

        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isWithin(0.01f).of(150f)
    }

    @Test
    fun interpolateRemoteFloat_standard() {
        val result =
            interpolateRemoteFloat(
                progress = RemoteFloat(0.5f),
                initialValue = RemoteFloat(100f),
                targetValue = RemoteFloat(200f),
                CUBIC_STANDARD,
            )

        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isFinite()
    }

    @Test
    fun interpolateRemoteFloat_wrap() {
        // Shortest path between 350 and 10 is 20 degrees, so at 0.5 it should be 360 (or 0)
        val result =
            interpolateRemoteFloat(
                progress = RemoteFloat(0.5f),
                initialValue = RemoteFloat(350f),
                targetValue = RemoteFloat(10f),
                CUBIC_LINEAR,
                wrap = RemoteFloat(360f),
            )

        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        val value = context.getFloat(resultId)
        assertThat(value).isWithin(0.01f).of(360f)
    }

    @Test
    fun interpolateRemoteFloat_bounce() {
        val result =
            interpolateRemoteFloat(
                progress = RemoteFloat(0.5f),
                initialValue = RemoteFloat(0f),
                targetValue = RemoteFloat(100f),
                EASE_OUT_BOUNCE,
            )

        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        val value = context.getFloat(resultId)
        assertThat(value).isWithin(0.01f).of(76.5625f)
    }

    @Test
    fun interpolateRemoteFloat_elastic() {
        val result =
            interpolateRemoteFloat(
                progress = RemoteFloat(0.5f),
                initialValue = RemoteFloat(0f),
                targetValue = RemoteFloat(100f),
                EASE_OUT_ELASTIC,
            )

        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        val value = context.getFloat(resultId)
        assertThat(value).isWithin(0.01f).of(101.56f)
    }

    @Test
    fun interpolateRemoteFloat_custom() {
        // Custom linear: 1, 1, 0, 0
        val spec = RemoteFloatArray(listOf(1f.rf, 1f.rf, 0f.rf, 0f.rf))
        val result =
            interpolateRemoteFloat(
                progress = RemoteFloat(0.5f),
                initialValue = RemoteFloat(0f),
                targetValue = RemoteFloat(100f),
                CUBIC_CUSTOM,
                spec = spec,
            )

        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isWithin(0.01f).of(50f)
    }

    @Test
    fun interpolateRemoteFloat_spline() {
        // Simple linear spline: 0, 1
        val result =
            interpolateRemoteFloat(
                progress = RemoteFloat(0.5f),
                initialValue = RemoteFloat(0f),
                targetValue = RemoteFloat(100f),
                SPLINE_CUSTOM,
                spec = RemoteFloatArray(listOf(0f.rf, 1f.rf)),
            )

        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isWithin(0.1f).of(50f)
    }

    @Test
    fun timeOfReferenceInSeconds() {
        val result = timeOfReferenceInSeconds(JUN_06_2025_UTC)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(3f)
    }

    @Test
    fun timeOfReferenceInMinutes() {
        val result = timeOfReferenceInMinutes(JUN_06_2025_UTC)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(2f)
    }

    @Test
    fun timeOfReferenceInHours() {
        val result = timeOfReferenceInHours(JUN_06_2025_UTC)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(1f)
    }

    @Test
    fun dayOfMonthForReference() {
        val result = dayOfMonthForReference(JUN_06_2025_UTC)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(6f)
    }

    @Test
    fun monthOfYearForReference() {
        val result = monthOfYearForReference(JUN_06_2025_UTC)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(5f)
    }

    @Test
    fun dayOfWeekForReference() {
        val result = dayOfWeekForReference(JUN_06_2025_UTC)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(4f)
    }

    @Test
    fun yearForReference() {
        val result = yearForReference(JUN_06_2025_UTC)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(2025f)
    }

    @Test
    fun namedRemoteFloat_initialValue() {
        val namedRemoteFloat = createNamedRemoteFloat("testFloat", 100.0f)
        val result = namedRemoteFloat * RemoteFloat(10f)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(1000f)
    }

    @Test
    fun namedRemoteFloat_overriddenValue() {
        val namedRemoteFloat = createNamedRemoteFloat("testFloat", 100.0f)
        val result = namedRemoteFloat * RemoteFloat(10f)
        val resultId = result.getIdForCreationState(creationState)

        makeAndUpdateCoreDocument { context.setNamedFloatOverride("USER:testFloat", 20f) }

        assertThat(context.getFloat(resultId)).isEqualTo(200f)
    }

    @Test
    fun namedRemoteFloat_overriddenValue2() {
        val namedRemoteFloat = createNamedRemoteFloat("testFloat", 100f)
        val plusOne = namedRemoteFloat + RemoteFloat(1f)
        val result = plusOne * plusOne
        val resultId = result.getIdForCreationState(creationState)

        makeAndUpdateCoreDocument { context.setNamedFloatOverride("USER:testFloat", 19f) }

        assertThat(context.getFloat(resultId)).isEqualTo(400f)
    }

    @Test
    fun namedRemoteFloatExpression_smokeTest() {
        val namedExpr = createNamedRemoteFloat("testExpr", RemoteFloat(10f) + RemoteFloat(5f))
        val result = namedExpr * RemoteFloat(2f)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(30f)
    }

    @Test
    fun asRemoteDp_createsCorrectly() {
        val floatValue = 10.5f
        val remoteFloat = RemoteFloat(floatValue)
        val remoteFloatDp = remoteFloat.asRemoteDp()

        assertThat(remoteFloatDp.value).isEqualTo(remoteFloat)
    }

    @Test
    fun asRemoteDp_hasSameFloatValueAsOriginalRemoteFloat() {
        val floatValue = 10.5f
        val remoteFloat = RemoteFloat(floatValue)
        val remoteFloatDp = remoteFloat.asRemoteDp()
        val resultId = remoteFloat.getIdForCreationState(creationState)
        val resultDpId = remoteFloatDp.value.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(floatValue)
        assertThat(context.getFloat(resultDpId)).isEqualTo(floatValue)
    }

    @Test
    fun asRemoteDp_hasSameIdFromOriginalRemoteFloat() {
        val floatValue = 10.5f
        val remoteFloat = RemoteFloat(floatValue)
        val remoteFloatDp = remoteFloat.asRemoteDp()

        val resultId = remoteFloat.getIdForCreationState(creationState)
        val resultDpId = remoteFloatDp.value.getIdForCreationState(creationState)

        assertThat(resultId).isEqualTo(resultDpId)
    }

    @Test
    fun longExpression_usesReferences() {
        var longExpression = createNamedRemoteFloat("test", 1f)
        for (i in 0..50) {
            longExpression += RemoteFloat(i.toFloat())
        }

        val finalArray = longExpression.arrayForCreationState(creationState)
        assertThat(finalArray.size < 20).isTrue()

        val expected = 1f + (50 * 51) / 2f
        val longExpressionId = longExpression.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()
        assertThat(context.getFloat(longExpressionId)).isEqualTo(expected)
    }

    @Test
    fun addAndAddPeepholeOptimization() {
        val expr = RemoteTimeVariables.continuousSeconds + 100f + 50f
        val array = expr.arrayForCreationState(creationState)
        assertThat(AnimatedFloatExpression.toString(array, null)).isEqualTo("[1] 150.0 + ")
    }

    @Test
    fun addAndSubtractPeepholeOptimization() {
        val expr = RemoteTimeVariables.continuousSeconds + 100f - 50f
        val array = expr.arrayForCreationState(creationState)
        assertThat(AnimatedFloatExpression.toString(array, null)).isEqualTo("[1] 50.0 + ")
    }

    @Test
    fun subtractAndSubtractPeepholeOptimization() {
        val expr = RemoteTimeVariables.continuousSeconds - 100f - 50f
        val array = expr.arrayForCreationState(creationState)
        assertThat(AnimatedFloatExpression.toString(array, null)).isEqualTo("[1] 150.0 - ")
    }

    @Test
    fun subtractAndAddPeepholeOptimization() {
        val expr = RemoteTimeVariables.continuousSeconds - 100f + 50f
        val array = expr.arrayForCreationState(creationState)
        assertThat(AnimatedFloatExpression.toString(array, null)).isEqualTo("[1] 50.0 - ")
    }

    @Test
    fun multiplyAndMultiplyPeepholeOptimization() {
        val expr = RemoteTimeVariables.continuousSeconds * 4f * 2f
        val array = expr.arrayForCreationState(creationState)
        assertThat(AnimatedFloatExpression.toString(array, null)).isEqualTo("[1] 8.0 * ")
    }

    @Test
    fun multiplyAndDividePeepholeOptimization() {
        val expr = RemoteTimeVariables.continuousSeconds * 4f / 2f
        val array = expr.arrayForCreationState(creationState)
        assertThat(AnimatedFloatExpression.toString(array, null)).isEqualTo("[1] 2.0 * ")
    }

    @Test
    fun divideAndDividePeepholeOptimization() {
        val expr = RemoteTimeVariables.continuousSeconds / 4f / 2f
        val array = expr.arrayForCreationState(creationState)
        assertThat(AnimatedFloatExpression.toString(array, null)).isEqualTo("[1] 8.0 / ")
    }

    @Test
    fun divideAndMultiplyPeepholeOptimization() {
        val expr = RemoteTimeVariables.continuousSeconds / 4f * 2f
        val array = expr.arrayForCreationState(creationState)
        assertThat(AnimatedFloatExpression.toString(array, null)).isEqualTo("[1] 2.0 / ")
    }

    @Test
    fun createReference_resolvesToSameValue() {
        val rf = RemoteFloat(10f)
        val ref = rf.createReference()

        assertThat(rf.constantValue).isEqualTo(10f)
        assertThat(ref.constantValue).isEqualTo(10f)

        val refId = ref.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(refId)).isEqualTo(10f)
    }

    @Test
    fun createReference_forcedResolvesToSameValue() {
        val rf = RemoteFloat(10f)
        val ref = rf.createReference(forceRemote = true)

        assertThat(rf.constantValue).isEqualTo(10f)
        assertThat(ref.constantValueOrNull).isNull()

        val refId = ref.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(refId)).isEqualTo(10f)
    }

    @Test
    fun animateRemoteFloat_smokeTest() {
        val rf1 = RemoteFloat(10f).createReference(forceRemote = true)
        val rf2 = RemoteFloat(5f).createReference(forceRemote = true)
        val animated = animateRemoteFloat(rf1 / rf2, duration = 2f, type = CUBIC_DECELERATE)

        assertThat(animated).isInstanceOf(RemoteFloat::class.java)

        val animatedId = animated.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        val floatId = animated.getFloatIdForCreationState(creationState)
        assertThat(floatId).isNaN()

        assertThat(context.getFloat(animatedId)).isEqualTo(2f)
    }

    @Test
    fun animateRemoteFloat_defaultParameters_generatesAnimationInDocument() {
        val rf = RemoteFloat(10f).createReference(forceRemote = true)
        val animated = animateRemoteFloat(rf, duration = 1f, type = CUBIC_STANDARD)

        assertThat(animated).isInstanceOf(RemoteFloat::class.java)
        val animatedId = animated.getIdForCreationState(creationState)

        val coreDoc =
            CoreDocument().apply {
                val buffer = creationState.document.buffer
                buffer.buffer.index = 0
                initFromBuffer(buffer)
            }

        val floatExpr =
            coreDoc.operations.filterIsInstance<FloatExpression>().first { it.mId == animatedId }
        assertThat(floatExpr.mSrcAnimation).isNotNull()
        assertThat(floatExpr.mSrcAnimation!!.isNotEmpty()).isTrue()
        assertThat(floatExpr.mFloatAnimation).isNotNull()
        assertThat(floatExpr.mFloatAnimation!!.duration).isEqualTo(1.0f)
        assertThat(floatExpr.mFloatAnimation!!.type).isEqualTo(CUBIC_STANDARD)
    }

    @Test
    fun animateRemoteFloat_onPreAllocatedRemoteFloatExpression_preservesAnimation() {
        val seconds = RemoteTimeVariables.continuousSeconds
        val source = floor(seconds % 4f.rf).toRemoteInt()
        val targetAngle = (source.toRemoteFloat()) * 90f.rf
        val angle = animateRemoteFloat(targetAngle, duration = 1.0f, type = CUBIC_STANDARD)

        val angleId = angle.getIdForCreationState(creationState)

        val coreDoc =
            CoreDocument().apply {
                val buffer = creationState.document.buffer
                buffer.buffer.index = 0
                initFromBuffer(buffer)
            }

        val floatExpr =
            coreDoc.operations.filterIsInstance<FloatExpression>().first { it.mId == angleId }
        assertThat(floatExpr.mSrcAnimation).isNotNull()
        assertThat(floatExpr.mSrcAnimation!!.isNotEmpty()).isTrue()
        assertThat(floatExpr.mFloatAnimation).isNotNull()
        assertThat(floatExpr.mFloatAnimation!!.duration).isEqualTo(1.0f)
        assertThat(floatExpr.mFloatAnimation!!.type).isEqualTo(CUBIC_STANDARD)
    }

    @Test
    fun animateRemoteFloat_onAlreadyWrittenRemoteFloatExpression_preservesAnimation() {
        val seconds = RemoteTimeVariables.continuousSeconds
        val source = floor(seconds % 4f.rf).toRemoteInt()
        val targetAngle = (source.toRemoteFloat()) * 90f.rf

        // Write targetAngle to document first as an un-animated expression
        val targetAngleId = targetAngle.getIdForCreationState(creationState)

        // Now animate targetAngle
        val angle = animateRemoteFloat(targetAngle, duration = 1.0f, type = CUBIC_STANDARD)
        val angleId = angle.getIdForCreationState(creationState)

        val coreDoc =
            CoreDocument().apply {
                val buffer = creationState.document.buffer
                buffer.buffer.index = 0
                initFromBuffer(buffer)
            }

        val targetExpr =
            coreDoc.operations.filterIsInstance<FloatExpression>().first { it.mId == targetAngleId }
        assertThat(targetExpr.mFloatAnimation).isNull()

        val angleExpr =
            coreDoc.operations.filterIsInstance<FloatExpression>().first { it.mId == angleId }
        assertThat(angleExpr.mSrcAnimation).isNotNull()
        assertThat(angleExpr.mFloatAnimation).isNotNull()
        assertThat(angleExpr.mFloatAnimation!!.duration).isEqualTo(1.0f)
        assertThat(angleExpr.mFloatAnimation!!.type).isEqualTo(CUBIC_STANDARD)
    }

    @Test
    fun animateRemoteFloat_deduplication_sameAnimationAndExpression() {
        val rf = RemoteFloat(10f).createReference(forceRemote = true)
        val anim1 = animateRemoteFloat(rf, duration = 1.0f, type = CUBIC_STANDARD)
        val anim2 = animateRemoteFloat(rf, duration = 1.0f, type = CUBIC_STANDARD)

        val id1 = anim1.getIdForCreationState(creationState)
        val id2 = anim2.getIdForCreationState(creationState)

        assertThat(id1).isEqualTo(id2)
    }

    @Test
    fun animateRemoteFloat_doesNotDeduplicateWithUnanimatedExpression() {
        val expr = RemoteTimeVariables.continuousSeconds * 2f
        val animated = animateRemoteFloat(expr, duration = 1.0f, type = CUBIC_STANDARD)

        val exprId = expr.getIdForCreationState(creationState)
        val animatedId = animated.getIdForCreationState(creationState)

        assertThat(animatedId).isNotEqualTo(exprId)

        val coreDoc =
            CoreDocument().apply {
                val buffer = creationState.document.buffer
                buffer.buffer.index = 0
                initFromBuffer(buffer)
            }

        val exprOp =
            coreDoc.operations.filterIsInstance<FloatExpression>().first { it.mId == exprId }
        val animOp =
            coreDoc.operations.filterIsInstance<FloatExpression>().first { it.mId == animatedId }

        assertThat(exprOp.mFloatAnimation).isNull()
        assertThat(animOp.mFloatAnimation).isNotNull()
    }

    @Test
    fun animateRemoteFloat_onMutableRemoteFloat_preservesAnimation() {
        val mutableFloat = MutableRemoteFloat(5f)
        val animated = animateRemoteFloat(mutableFloat, duration = 1.0f, type = CUBIC_STANDARD)
        val mutableId = mutableFloat.getIdForCreationState(creationState)
        val animatedId = animated.getIdForCreationState(creationState)

        assertThat(animatedId).isNotEqualTo(mutableId)

        val coreDoc =
            CoreDocument().apply {
                val buffer = creationState.document.buffer
                buffer.buffer.index = 0
                initFromBuffer(buffer)
            }

        val animOp =
            coreDoc.operations.filterIsInstance<FloatExpression>().first { it.mId == animatedId }
        assertThat(animOp.mFloatAnimation).isNotNull()
        assertThat(animOp.mFloatAnimation!!.duration).isEqualTo(1.0f)
    }

    @Test
    fun animateRemoteFloatAsState_withTween() {
        val rf = RemoteFloat(10f).createReference(forceRemote = true)
        val animated =
            animateRemoteFloatAsState(
                targetValue = rf,
                animationSpec = remoteTween(durationMillis = 500, easing = RemoteEasing.Decelerate),
            )

        assertThat(animated).isInstanceOf(RemoteFloat::class.java)
        val animatedId = animated.getIdForCreationState(creationState)

        val coreDoc =
            CoreDocument().apply {
                val buffer = creationState.document.buffer
                buffer.buffer.index = 0
                initFromBuffer(buffer)
            }

        val floatExpr =
            coreDoc.operations.filterIsInstance<FloatExpression>().first { it.mId == animatedId }
        assertThat(floatExpr.mFloatAnimation).isNotNull()
        assertThat(floatExpr.mFloatAnimation!!.duration).isEqualTo(0.5f)
        assertThat(floatExpr.mFloatAnimation!!.type).isEqualTo(CUBIC_DECELERATE)
    }

    @Test
    fun animateRemoteFloatAsState_withSpring() {
        val rf = RemoteFloat(10f).createReference(forceRemote = true)
        val animated =
            animateRemoteFloatAsState(
                targetValue = rf,
                animationSpec = remoteSpring(stiffness = 100f, dampingRatio = 0.8f),
            )

        assertThat(animated).isInstanceOf(RemoteFloat::class.java)
        val animatedId = animated.getIdForCreationState(creationState)

        val coreDoc =
            CoreDocument().apply {
                val buffer = creationState.document.buffer
                buffer.buffer.index = 0
                initFromBuffer(buffer)
            }

        val floatExpr =
            coreDoc.operations.filterIsInstance<FloatExpression>().first { it.mId == animatedId }
        assertThat(floatExpr.mSrcAnimation).isNotNull()
        assertThat(floatExpr.mSrcAnimation!![0]).isEqualTo(0f)
        assertThat(floatExpr.mSrcAnimation!![1]).isEqualTo(100f)
        assertThat(floatExpr.mSrcAnimation!![2]).isEqualTo(16f)
        assertThat(floatExpr.mSrcAnimation!![3]).isEqualTo(0.001f)
        assertThat(floatExpr.mSrcAnimation!![4]).isEqualTo(Float.fromBits(0))
    }

    @Test
    fun remoteEasing_spline_defensiveCopy() {
        val points = floatArrayOf(0f, 0f, 1f, 1f)
        val easing = RemoteEasing.spline(points)
        points[0] = 99f
        assertThat(easing.spec!![0]).isEqualTo(0f)
    }

    @Test
    fun remoteEasing_equalsAndHashCode() {
        val easing1 = RemoteEasing.cubic(0.2f, 0.4f, 0.6f, 0.8f)
        val easing2 = RemoteEasing.cubic(0.2f, 0.4f, 0.6f, 0.8f)
        val easing3 = RemoteEasing.cubic(0.1f, 0.4f, 0.6f, 0.8f)

        assertThat(easing1).isEqualTo(easing2)
        assertThat(easing1.hashCode()).isEqualTo(easing2.hashCode())
        assertThat(easing1).isNotEqualTo(easing3)
    }

    @Test
    fun remoteTweenSpec_equalsAndHashCode() {
        val spec1 = remoteTween(durationMillis = 400, easing = RemoteEasing.Linear)
        val spec2 = remoteTween(durationMillis = 400, easing = RemoteEasing.Linear)
        val spec3 = remoteTween(durationMillis = 500, easing = RemoteEasing.Linear)

        assertThat(spec1).isEqualTo(spec2)
        assertThat(spec1.hashCode()).isEqualTo(spec2.hashCode())
        assertThat(spec1).isNotEqualTo(spec3)
    }

    @Test
    fun remoteSpringSpec_equalsAndHashCode() {
        val spec1 =
            remoteSpring(
                stiffness = 100f,
                dampingRatio = 0.5f,
                stopThreshold = 0.01f,
                boundaryMode = 1,
            )
        val spec2 =
            remoteSpring(
                stiffness = 100f,
                dampingRatio = 0.5f,
                stopThreshold = 0.01f,
                boundaryMode = 1,
            )
        val spec3 =
            remoteSpring(
                stiffness = 200f,
                dampingRatio = 0.5f,
                stopThreshold = 0.01f,
                boundaryMode = 1,
            )

        assertThat(spec1).isEqualTo(spec2)
        assertThat(spec1.hashCode()).isEqualTo(spec2.hashCode())
        assertThat(spec1).isNotEqualTo(spec3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun remoteTweenSpec_negativeDuration_throws() {
        remoteTween(durationMillis = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun remoteSpringSpec_nonPositiveStiffness_throws() {
        remoteSpring(stiffness = 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun remoteSpringSpec_negativeDamping_throws() {
        remoteSpring(dampingRatio = -0.1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun remoteSpringSpec_nonPositiveStopThreshold_throws() {
        remoteSpring(stopThreshold = 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun remoteSpringSpec_withInitialValue_throws() {
        val rf = RemoteFloat(10f)
        remoteSpring().animate(rf, initialValue = 5f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun remoteSpringSpec_withWrap_throws() {
        val rf = RemoteFloat(10f)
        remoteSpring().animate(rf, wrap = 360f)
    }

    @Test
    fun cacheKeys() {
        val constant = RemoteFloat(10f)
        assertThat(constant.cacheKey).isEqualTo(RemoteConstantCacheKey(10f))

        val named = createNamedRemoteFloat("test", 1f)
        assertThat(named.cacheKey).isEqualTo(RemoteNamedCacheKey(RemoteState.Domain.User, "test"))

        val op = constant + named
        // flipped because peephole
        val key = op.cacheKey as RemoteOperationCacheKey
        assertThat(key.op.name).isEqualTo("Plus")
        assertThat(key.args).isEqualTo(listOf(named.cacheKey, constant.cacheKey))
    }

    @Test
    fun peepholeOptimization_plus() {
        val expr = (time + 10f) + 1f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:time\" type=1",
                "FloatConstant[43] = 100.0",
                "FloatExpression[44] = ([43] 11.0 + )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_minus() {
        val expr = (time - 10f) - 1f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:time\" type=1",
                "FloatConstant[43] = 100.0",
                "FloatExpression[44] = ([43] 11.0 - )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_minus2() {
        val expr = (time + 10f) - 1f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:time\" type=1",
                "FloatConstant[43] = 100.0",
                "FloatExpression[44] = ([43] 9.0 + )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_times() {
        val expr = (time * 10f) * 2f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:time\" type=1",
                "FloatConstant[43] = 100.0",
                "FloatExpression[44] = ([43] 20.0 * )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_div() {
        val expr = (time / 10f) / 2f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:time\" type=1",
                "FloatConstant[43] = 100.0",
                "FloatExpression[44] = ([43] 20.0 / )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_div2() {
        val expr = (time * 10f) / 2f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:time\" type=1",
                "FloatConstant[43] = 100.0",
                "FloatExpression[44] = ([43] 5.0 * )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_complex() {
        val expr = (time + 10f) - 5f + 2f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:time\" type=1",
                "FloatConstant[43] = 100.0",
                "FloatExpression[44] = ([43] 7.0 + )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_notPossible() {
        val expr = (time * 10f) + 2f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:time\" type=1",
                "FloatConstant[43] = 100.0",
                "FloatExpression[44] = ([43] 10.0 * 2.0 + )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_select_div() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val expr = myBool.select(10f.rf, 20f.rf) / 2f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "FloatExpression[44] = (10.0 5.0 [43] ifElse )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_select_rem() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val expr = myBool.select(10f.rf, 20f.rf) % 3f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "FloatExpression[44] = (2.0 1.0 [43] ifElse )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_select_remByZero_doesNotCrashDuringPeepHole() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val expr = myBool.select(10f.rf, 20f.rf) % 0f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "FloatExpression[44] = (20.0 10.0 [43] ifElse 0.0 % )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_select_times() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val expr = myBool.select(10f.rf, 20f.rf) * 3f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "FloatExpression[44] = (60.0 30.0 [43] ifElse )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_select_plus() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val expr = myBool.select(10f.rf, 20f.rf) + 5f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "FloatExpression[44] = (25.0 15.0 [43] ifElse )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_select_minus() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val expr = myBool.select(10f.rf, 20f.rf) - 5f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "FloatExpression[44] = (15.0 5.0 [43] ifElse )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_select_unaryMinus() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val expr = -myBool.select(10f.rf, 20f.rf)

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "FloatExpression[44] = (-20.0 -10.0 [43] ifElse )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_select_chained() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val expr = (myBool.select(10f.rf, 20f.rf) + 10f) * 2f / 4f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "FloatExpression[44] = (15.0 10.0 [43] ifElse )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_selectIfLt_div() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 5f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 10f)
        val expr = selectIfLt(x, y, 100f.rf, 200f.rf) / 10f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:y\" type=1",
                "FloatConstant[43] = 10.0",
                "VariableName[44] = \"USER:x\" type=1",
                "FloatConstant[44] = 5.0",
                "FloatExpression[45] = (20.0 10.0 [43] [44] - ifElse )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_selectIfGt_times() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 5f)
        val expr = selectIfGt(x, 10f.rf, 10f.rf, 20f.rf) * 2f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:x\" type=1",
                "FloatConstant[43] = 5.0",
                "FloatExpression[44] = (40.0 20.0 [43] 10.0 - ifElse )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_selectIfLe_plus() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 5f)
        val expr = selectIfLe(x, 10f.rf, 10f.rf, 20f.rf) + 5f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:x\" type=1",
                "FloatConstant[43] = 5.0",
                "FloatExpression[44] = (15.0 25.0 [43] 10.0 - ifElse )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_selectIfGe_minus() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 5f)
        val expr = selectIfGe(x, 10f.rf, 10f.rf, 20f.rf) - 5f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:x\" type=1",
                "FloatConstant[43] = 5.0",
                "FloatExpression[44] = (5.0 15.0 10.0 [43] - ifElse )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_booleanComparisonSelect_times() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 5f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 10f)
        val expr = (x.isLessThan(y)).select(10f.rf, 20f.rf) * 3f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:y\" type=1",
                "FloatConstant[43] = 10.0",
                "VariableName[44] = \"USER:x\" type=1",
                "FloatConstant[44] = 5.0",
                "FloatExpression[45] = (60.0 30.0 [43] [44] - ifElse )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_selectWithConsumingCondition_doesNotFoldBranches() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 5f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 10f)

        val rawSelect =
            RemoteFloat(
                constantValueOrNull = null,
                cacheKey = RemoteStateInstanceKey(),
            ) { creationState ->
                floatArrayOf(
                    10f,
                    20f,
                    x.getFloatIdForCreationState(creationState),
                    AnimatedFloatExpression.SUB,
                    y.getFloatIdForCreationState(creationState),
                    AnimatedFloatExpression.IFELSE,
                )
            }

        val expr = rawSelect * 2f
        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:x\" type=1",
                "FloatConstant[43] = 5.0",
                "VariableName[44] = \"USER:y\" type=1",
                "FloatConstant[44] = 10.0",
                "FloatExpression[45] = (10.0 20.0 [43] - [44] ifElse 2.0 * )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_foldTrailingConstant_nanResult_doesNotCorruptArray() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 5f)
        val inf = Float.POSITIVE_INFINITY
        val expr = (x + inf) - inf

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:x\" type=1",
                "FloatConstant[43] = 5.0",
                "FloatExpression[44] = ([43] Infinity + Infinity - )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_foldSelectBranches_nanResult_doesNotCorruptArray() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val inf = Float.POSITIVE_INFINITY
        val expr = myBool.select(inf.rf, 10f.rf) - inf

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "FloatExpression[44] = (10.0 Infinity [43] ifElse Infinity - )",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_zeroDiv() {
        val expr = RemoteFloat(0f) / time

        val ops = getOperationsStrings(expr)
        assertThat(ops).containsExactly("FloatExpression[43] = (0.0 )").inOrder()
    }

    @Test
    fun peepholeOptimization_trimToIdentity_plusMinus() {
        val expr = (time + 10f) - 10f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly("VariableName[43] = \"USER:time\" type=1", "FloatConstant[43] = 100.0")
            .inOrder()
        assertThat(expr.getIdForCreationState(creationState))
            .isEqualTo(time.getIdForCreationState(creationState))
    }

    @Test
    fun peepholeOptimization_trimToIdentity_minusPlus() {
        val expr = (time - 10f) + 10f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly("VariableName[43] = \"USER:time\" type=1", "FloatConstant[43] = 100.0")
            .inOrder()
        assertThat(expr.getIdForCreationState(creationState))
            .isEqualTo(time.getIdForCreationState(creationState))
    }

    @Test
    fun peepholeOptimization_trimToIdentity_timesDiv() {
        val expr = (time * 2f) / 2f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly("VariableName[43] = \"USER:time\" type=1", "FloatConstant[43] = 100.0")
            .inOrder()
        assertThat(expr.getIdForCreationState(creationState))
            .isEqualTo(time.getIdForCreationState(creationState))
    }

    @Test
    fun peepholeOptimization_trimToIdentity_divTimes() {
        val expr = (time / 2f) * 2f

        val ops = getOperationsStrings(expr)
        assertThat(ops)
            .containsExactly("VariableName[43] = \"USER:time\" type=1", "FloatConstant[43] = 100.0")
            .inOrder()
        assertThat(expr.getIdForCreationState(creationState))
            .isEqualTo(time.getIdForCreationState(creationState))
    }

    private fun getOperationsStrings(expr: RemoteFloat): List<String> =
        CoreDocument().run {
            expr.getIdForCreationState(creationState)

            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            getOperations()
                .map { it.toString() }
                .filter {
                    !it.contains("HEADER") &&
                        !it.contains("TextData") &&
                        !it.contains("RootContentDescription")
                }
        }

    @Test
    fun RemoteFloatConstructorFromId() {
        val floatFromId = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC)

        assertThat(floatFromId.hasConstantValue).isFalse()
        assertThat(floatFromId.cacheKey).isEqualTo(RemoteStateIdKey(ID_CONTINUOUS_SEC))
    }

    @Test
    fun RemoteFloatConstructorFromConstant() {
        val floatFromId = RemoteFloat(42f)

        assertThat(floatFromId.hasConstantValue).isTrue()
        assertThat(floatFromId.cacheKey).isEqualTo(RemoteConstantCacheKey(42f))
    }

    @Test
    fun sharedExpressionReferenced() {
        val a = RemoteTimeVariables.continuousSeconds + 1f
        val b = a * 2f

        a.getIdForCreationState(creationState)
        assertThat(a.hasBeenWrittenToDoc(creationState)).isTrue()

        val bArray = b.arrayForCreationState(creationState)

        assertThat(bArray.size).isEqualTo(3)
        assertThat(bArray[0]).isEqualTo(a.getFloatIdForCreationState(creationState))
    }

    @Test
    fun unsharedExpressionInlined() {
        val a = RemoteTimeVariables.continuousSeconds + 1f
        val b = a * 2f

        assertThat(a.hasBeenWrittenToDoc(creationState)).isFalse()

        val bArray = b.arrayForCreationState(creationState)

        assertThat(bArray.size).isEqualTo(5)
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }

    private fun makeAndUpdateCoreDocument(
        buffer: RemoteComposeBuffer? = null,
        runAfterInit: (CoreDocument) -> Unit = {},
    ) =
        CoreDocument().apply {
            val buffer = buffer ?: creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            initializeContext(context)

            runAfterInit(this)

            for (op in operations) {
                if (op is VariableSupport) {
                    op.updateVariables(context)
                }
                op.apply(context)
            }
        }

    @Test
    fun toDebugString_constant() {
        val c = RemoteFloat(45.5f)
        assertThat(c.toDebugString()).isEqualTo("45.5")
    }

    @Test
    fun toDebugString_variables() {
        val x = createNamedRemoteFloat("x", 10f)
        assertThat(x.toDebugString()).isEqualTo("user:x")

        assertThat(RemoteTimeVariables.continuousSeconds.toDebugString())
            .isEqualTo("context:continuous_sec")
        assertThat(RemoteConfiguration.density.toDebugString()).isEqualTo("context:density")
        assertThat(RemoteTimeVariables.animationTime.toDebugString())
            .isEqualTo("context:animation_time")
        assertThat(RemoteTimeVariables.animationDeltaTime.toDebugString())
            .isEqualTo("context:animation_delta_time")
        assertThat(RemoteConfiguration.windowWidthPx.toDebugString())
            .isEqualTo("context:window_width")
        assertThat(RemoteConfiguration.windowHeightPx.toDebugString())
            .isEqualTo("context:window_height")
        assertThat(RemoteConfiguration.fontSizePx.toDebugString()).isEqualTo("context:font_size")
        assertThat(RemoteFloat(asNan(999)).toDebugString()).isEqualTo("context:#999")
    }

    @Test
    fun companionContextVariables() {
        creationState = testCreationState(width = 40, height = 50)
        val animTimeId =
            (RemoteTimeVariables.animationTime + 1f).getIdForCreationState(creationState)
        val animDeltaId =
            (RemoteTimeVariables.animationDeltaTime + 1f).getIdForCreationState(creationState)
        val contSecId =
            (RemoteTimeVariables.continuousSeconds + 1f).getIdForCreationState(creationState)
        val winWidthId =
            (RemoteConfiguration.windowWidthPx + 1f).getIdForCreationState(creationState)
        val winHeightId =
            (RemoteConfiguration.windowHeightPx + 1f).getIdForCreationState(creationState)
        val densityId = (RemoteConfiguration.density + 1f).getIdForCreationState(creationState)
        val fontSizeId = (RemoteConfiguration.fontSizePx + 1f).getIdForCreationState(creationState)

        makeAndUpdateCoreDocument {
            context.loadFloat(RemoteContext.ID_ANIMATION_TIME, 10f)
            context.loadFloat(RemoteContext.ID_ANIMATION_DELTA_TIME, 20f)
            context.loadFloat(RemoteContext.ID_CONTINUOUS_SEC, 30f)
            context.loadFloat(RemoteContext.ID_WINDOW_WIDTH, 40f)
            context.loadFloat(RemoteContext.ID_WINDOW_HEIGHT, 50f)
            context.loadFloat(RemoteContext.ID_DENSITY, 80f)
            context.loadFloat(RemoteContext.ID_FONT_SIZE, 90f)
        }

        assertThat(context.getFloat(animTimeId)).isEqualTo(11f)
        assertThat(context.getFloat(animDeltaId)).isEqualTo(21f)
        assertThat(context.getFloat(contSecId)).isEqualTo(31f)
        assertThat(context.getFloat(winWidthId)).isEqualTo(41f)
        assertThat(context.getFloat(winHeightId)).isEqualTo(51f)
        assertThat(context.getFloat(densityId)).isEqualTo(81f)
        assertThat(context.getFloat(fontSizeId)).isEqualTo(91f)
    }

    @Test
    fun toDebugString_function_abs() {
        val x = createNamedRemoteFloat("x", 10f)
        assertThat(abs(-x).toDebugString()).isEqualTo("abs(-user:x)")
    }

    @Test
    fun toDebugString_arithmetic_compound() {
        val x = createNamedRemoteFloat("x", 10f)
        val c = RemoteFloat(45.5f)
        val expr = (x + c) * abs(-x)
        assertThat(expr.toDebugString()).isEqualTo("(user:x + 45.5) * abs(-user:x)")
    }

    @Test
    fun toDebugString_arithmetic_associativityWrapping() {
        val x = createNamedRemoteFloat("x", 10f)
        val c = RemoteFloat(45.5f)
        val assocExpr = x - (c + x)
        assertThat(assocExpr.toDebugString()).isEqualTo("user:x - (user:x + 45.5)")
    }

    @Test
    fun toDebugString_function_timeOfReferenceInSeconds() {
        val timeRef = timeOfReferenceInSeconds(RemoteLong(1000L))
        assertThat(timeRef.toDebugString()).isEqualTo("timeOfReferenceInSeconds(1000)")
    }

    @Test
    fun toDebugString_typeConversions() {
        val x = createNamedRemoteFloat("x", 10f)
        assertThat(x.toRemoteString().toDebugString()).isEqualTo("user:x.toRemoteString()")
        assertThat(x.toRemoteInt().toDebugString()).isEqualTo("user:x.toRemoteInt()")
    }

    @Test
    fun toDebugString_customFormatting_options() {
        val x = createNamedRemoteFloat("x", 10f)
        val customFormatted = x.toRemoteStringOptions(before = 10, after = 2, flags = 5)
        assertThat(customFormatted.toDebugString()).isEqualTo("user:x.toRemoteString(10, 2, 5)")
    }

    @Test
    fun toDebugString_customFormatting_singleArgFallback() {
        val x = createNamedRemoteFloat("x", 10f)
        val singleArgKey = x.toRemoteString().cacheKey
        assertThat(singleArgKey.toDebugString()).isEqualTo("user:x.toRemoteString()")
    }
}
