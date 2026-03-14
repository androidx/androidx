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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.RemoteContext.ID_CONTINUOUS_SEC
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.v2.captureSingleRemoteDocumentV2
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteFloatTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    val applicationContext = ApplicationProvider.getApplicationContext<Context>()
    val creationState = RemoteComposeCreationState(AndroidxRcPlatformServices(), Size(1f, 1f))
    val time = RemoteFloat.createNamedRemoteFloat("time", 100f).createReference()

    val JUN_06_2025_UTC =
        RemoteLong(
            LocalDateTime.parse("2025-06-06T01:02:03")
                .atZone(ZoneOffset.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

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
        assertThat(
                (RemoteFloat(10f) - RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC))
                    .constantValueOrNull
            )
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
        assertThat(RemoteFloat(21.5f).toRemoteString(2).hasConstantValue).isTrue()
        assertThat(RemoteInt(10).toRemoteFloat().hasConstantValue).isTrue()
    }

    @Test
    fun hasConstantValue_false() {
        assertThat(RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC).hasConstantValue).isFalse()
        assertThat(
                RemoteFloat(21.5f)
                    .plus(RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC))
                    .hasConstantValue
            )
            .isFalse()
        assertThat(RemoteFloat.createNamedRemoteFloat("value", 1f).hasConstantValue).isFalse()
        assertThat(
                RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC).toRemoteString(2).hasConstantValue
            )
            .isFalse()
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
        val namedRemoteFloat = RemoteFloat.createNamedRemoteFloat("testFloat", 100.0f)
        val result = namedRemoteFloat * RemoteFloat(10f)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(1000f)
    }

    @Test
    fun namedRemoteFloat_overriddenValue() {
        val namedRemoteFloat = RemoteFloat.createNamedRemoteFloat("testFloat", 100.0f)
        val result = namedRemoteFloat * RemoteFloat(10f)
        val resultId = result.getIdForCreationState(creationState)

        makeAndUpdateCoreDocument { context.setNamedFloatOverride("USER:testFloat", 20f) }

        assertThat(context.getFloat(resultId)).isEqualTo(200f)
    }

    @Test
    fun namedRemoteFloat_overriddenValue2() {
        val namedRemoteFloat = RemoteFloat.createNamedRemoteFloat("testFloat", 100f)
        val plusOne = namedRemoteFloat + RemoteFloat(1f)
        val result = plusOne * plusOne
        val resultId = result.getIdForCreationState(creationState)

        makeAndUpdateCoreDocument { context.setNamedFloatOverride("USER:testFloat", 19f) }

        assertThat(context.getFloat(resultId)).isEqualTo(400f)
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
        // This test checks that when we create a very long expression, we don't just
        // inline everything. The MAX_SAFE_FLOAT_ARRAY is 30, so we create an expression
        // that would be much larger than that if inlined.
        var longExpression = RemoteFloat.createNamedRemoteFloat("test", 1f)
        for (i in 0..50) {
            longExpression += RemoteFloat(i.toFloat())
        }

        // The array should be relatively small, having been split up.
        val finalArray = longExpression.arrayForCreationState(creationState)
        assertThat(finalArray.size < 20).isTrue()

        // The initial value is 1, and we add the sum of 0..50.
        val expected = 1f + (50 * 51) / 2f
        val longExpressionId = longExpression.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()
        assertThat(context.getFloat(longExpressionId)).isEqualTo(expected)
    }

    fun testTextFromFloat(
        expected: String,
        value: RemoteFloat,
        before: Int,
        after: Int = 2,
        flags: Int = TextFromFloat.PAD_AFTER_ZERO,
    ) {
        val constantFloatString = value.toRemoteString(before, after, flags)

        val constantStringId = constantFloatString.getIdForCreationState(creationState)

        // ensure we have an id to look up
        val variableFloat = value.createReference()
        val variableFloatId = variableFloat.getIdForCreationState(creationState)
        val variableFloatString = variableFloat.toRemoteString(before, after, flags)
        val variableStringId = variableFloatString.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(variableFloatId)).isEqualTo(value.constantValue)
        assertThat(context.getText(constantStringId)).isEqualTo(expected)
        assertThat(context.getText(variableStringId)).isEqualTo(expected)
    }

    @Test
    fun textFromFloat() {
        testTextFromFloat(".50", 0.5f.rf, 0)
        testTextFromFloat("-.50", (-0.5f).rf, 0)
        testTextFromFloat(
            "00.5000",
            0.5f.rf,
            2,
            4,
            TextFromFloat.PAD_AFTER_ZERO or TextFromFloat.PAD_PRE_ZERO,
        )
        testTextFromFloat(
            "5000000",
            5000000.rf,
            10,
            0,
            TextFromFloat.PAD_PRE_NONE or TextFromFloat.PAD_AFTER_NONE,
        )
    }

    fun testTextFromFloat(expected: String, value: RemoteFloat, formatter: DecimalFormat) {
        val constantFloatString = value.toRemoteString(formatter)

        val constantStringId = constantFloatString.getIdForCreationState(creationState)

        // ensure we have an id to look up
        val variableFloat = value.createReference()
        val variableFloatId = variableFloat.getIdForCreationState(creationState)
        val variableFloatString = variableFloat.toRemoteString(formatter)
        val variableStringId = variableFloatString.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(variableFloatId)).isEqualTo(value.constantValue)
        assertThat(context.getText(constantStringId)).isEqualTo(expected)
        assertThat(context.getText(variableStringId)).isEqualTo(expected)
    }

    @Test
    fun textFromFloatFormatting() {
        testTextFromFloat("0.500", 0.5f.rf, DecimalFormat("#0.000"))
        testTextFromFloat("-0.500", (-0.5f).rf, DecimalFormat("#0.000"))
        testTextFromFloat("0.50", 0.5f.rf, DecimalFormat("#,##0.00;(#,##0.00)"))
        testTextFromFloat("(0.50)", (-0.5f).rf, DecimalFormat("#,##0.00;(#,##0.00)"))
        testTextFromFloat("(50,000.50)", (-50000.50001f).rf, DecimalFormat("#,##0.00;(#,##0.00)"))
        testTextFromFloat("5000000.0", 5000000.rf, DecimalFormat("#0.##"))

        //        val indianFormatter = DecimalFormat.getNumberInstance(Locale("hi", "IN")) as
        // DecimalFormat
        //        testTextFromFloat("50,00,000.0", 5000000.rf, indianFormatter)
    }

    @Test
    fun addAndAddPeepholeOptimization() {
        val expr = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) + 100f + 50f
        val array = expr.arrayForCreationState(creationState)
        assertThat(AnimatedFloatExpression.toString(array, null)).isEqualTo("[1] 150.0 + ")
    }

    @Test
    fun addAndSubtractPeepholeOptimization() {
        val expr = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) + 100f - 50f
        val array = expr.arrayForCreationState(creationState)
        assertThat(AnimatedFloatExpression.toString(array, null)).isEqualTo("[1] 50.0 + ")
    }

    @Test
    fun subtractAndSubtractPeepholeOptimization() {
        val expr = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) - 100f - 50f
        val array = expr.arrayForCreationState(creationState)
        assertThat(AnimatedFloatExpression.toString(array, null)).isEqualTo("[1] 150.0 - ")
    }

    @Test
    fun subtractAndAddPeepholeOptimization() {
        val expr = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) - 100f + 50f
        val array = expr.arrayForCreationState(creationState)
        assertThat(AnimatedFloatExpression.toString(array, null)).isEqualTo("[1] 50.0 - ")
    }

    @Test
    fun multiplyAndMultiplyPeepholeOptimization() {
        val expr = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) * 4f * 2f
        val array = expr.arrayForCreationState(creationState)
        assertThat(AnimatedFloatExpression.toString(array, null)).isEqualTo("[1] 8.0 * ")
    }

    @Test
    fun multiplyAndDividePeepholeOptimization() {
        val expr = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) * 4f / 2f
        val array = expr.arrayForCreationState(creationState)
        assertThat(AnimatedFloatExpression.toString(array, null)).isEqualTo("[1] 2.0 * ")
    }

    @Test
    fun divideAndDividePeepholeOptimization() {
        val expr = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) / 4f / 2f
        val array = expr.arrayForCreationState(creationState)
        assertThat(AnimatedFloatExpression.toString(array, null)).isEqualTo("[1] 8.0 / ")
    }

    @Test
    fun divideAndMultiplyPeepholeOptimization() {
        val expr = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) / 4f * 2f
        val array = expr.arrayForCreationState(creationState)
        assertThat(AnimatedFloatExpression.toString(array, null)).isEqualTo("[1] 2.0 / ")
    }

    @Test
    fun createReference_resolvesToSameValue() {
        val rf = RemoteFloat(10f)
        val ref = rf.createReference()

        assertThat(rf.constantValue).isEqualTo(10f)
        // createReference preserves constantValue for optimization
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
        val animated = animateRemoteFloat(duration = 2f, type = CUBIC_DECELERATE) { rf1 / rf2 }

        assertThat(animated).isInstanceOf(AnimatedRemoteFloat::class.java)

        val animatedId = animated.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        val floatId = animated.getFloatIdForCreationState(creationState)
        assertThat(floatId).isNaN()

        assertThat(context.getFloat(animatedId)).isEqualTo(2f)
    }

    @Test
    fun cacheKeys() {
        val constant = RemoteFloat(10f)
        assertThat(constant.cacheKey).isEqualTo(RemoteConstantCacheKey(10f))

        val named = RemoteFloat.createNamedRemoteFloat("test", 1f)
        assertThat(named.cacheKey).isEqualTo(RemoteNamedCacheKey(RemoteState.Domain.User, "test"))

        val op = constant + named
        // flipped because peephole
        assertThat(op.cacheKey)
            .isEqualTo(
                RemoteOperationCacheKey.create(RemoteFloat.OperationKey.Plus, named, constant)
            )
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
        // expr.getIdForCreationState should return the same ID as time
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

    @Test
    fun rememberNamedRemoteFloatConstant() = runTest {
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocumentV2(
                creationDisplayInfo = displayInfo,
                context = applicationContext,
            ) {
                val myFloatFromConstant = rememberNamedRemoteFloat("C") { 5.rf }
                RemoteBox(modifier = RemoteModifier.size(myFloatFromConstant.asRemoteDp()))
            }

        makeAndUpdateCoreDocument(
            RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document.bytes))
        )

        val floatId = context.getVariableId("USER:C")
        assertThat(context.getFloat(floatId)).isEqualTo(5f)
        context.setNamedFloatOverride("USER:C", 20f)
        assertThat(context.getFloat(floatId)).isEqualTo(20f)
    }

    @Test
    fun rememberNamedRemoteFloatExpression() = runTest {
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocumentV2(
                creationDisplayInfo = displayInfo,
                context = applicationContext,
            ) {
                val myFloatFromConstant =
                    rememberNamedRemoteFloat("E") {
                        RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC)
                    }
                RemoteBox(modifier = RemoteModifier.size(myFloatFromConstant.asRemoteDp()))
            }

        makeAndUpdateCoreDocument(
            RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document.bytes))
        )

        val floatId = context.getVariableId("USER:E")
        assertThat(context.getFloat(floatId)).isEqualTo(context.getFloat(ID_CONTINUOUS_SEC))

        context.setNamedFloatOverride("USER:E", 20f)
        assertThat(context.getFloat(floatId)).isEqualTo(20f)
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

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }

    private fun makeAndPaintCoreDocument(document: CoreDocument) =
        CoreDocument().apply {
            val buffer = document.buffer
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
}
