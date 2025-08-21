/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.frontend.state

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.player.view.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@SdkSuppress(minSdkVersion = 26)
@RunWith(RobolectricTestRunner::class)
class RemoteFloatTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    val creationState =
        RemoteComposeCreationState(AndroidxPlatformServices(), density = 1f, Size(1f, 1f))

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
    fun selectIfLT_less() {
        val result =
            selectIfLT(RemoteFloat(1f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(100f)
    }

    @Test
    fun selectIfLT_equal() {
        val result =
            selectIfLT(RemoteFloat(2f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(200f)
    }

    @Test
    fun selectIfLT_greater() {
        val result =
            selectIfLT(RemoteFloat(3f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfLE_less() {
        val result =
            selectIfLE(RemoteFloat(1f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfLE_equal() {
        val result =
            selectIfLE(RemoteFloat(2f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfLE_greater() {
        val result =
            selectIfLE(RemoteFloat(2f), RemoteFloat(1f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfGT_less() {
        val result =
            selectIfGT(RemoteFloat(1f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfGT_equal() {
        val result =
            selectIfGT(RemoteFloat(2f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfGT_greater() {
        val result =
            selectIfGT(RemoteFloat(3f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfGE_less() {
        val result =
            selectIfGE(RemoteFloat(1f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfGE_equal() {
        val result =
            selectIfGE(RemoteFloat(2f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfGE_greater() {
        val result =
            selectIfGE(RemoteFloat(2f), RemoteFloat(1f), RemoteFloat(100f), RemoteFloat(200f))
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
        val result = clamp(min, max, value)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(10.5f)
    }

    @Test
    fun clamp_mid() {
        val min = RemoteFloat(10.5f)
        val max = RemoteFloat(20.5f)
        val value = RemoteFloat(11.5f)
        val result = clamp(min, max, value)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(11.5f)
    }

    @Test
    fun clamp_high() {
        val min = RemoteFloat(10.5f)
        val max = RemoteFloat(20.5f)
        val value = RemoteFloat(21.5f)
        val result = clamp(min, max, value)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(20.5f)
    }

    @Test
    fun clamp_low_floatMinMax() {
        val value = RemoteFloat(1.5f)
        val result = clamp(10.5f, 20.5f, value)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(10.5f)
    }

    @Test
    fun clamp_mid_floatMinMax() {
        val value = RemoteFloat(11.5f)
        val result = clamp(10.5f, 20.5f, value)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(11.5f)
    }

    @Test
    fun clamp_high_floatMinMax() {
        val value = RemoteFloat(21.5f)
        val result = clamp(10.5f, 20.5f, value)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(20.5f)
    }

    @Test
    fun hasConstantValue_true() {
        assertThat(RemoteFloat(21.5f).hasConstantValue).isTrue()
        assertThat(RemoteFloat(21.5f).plus(RemoteFloat(21.5f)).hasConstantValue).isTrue()
        assertThat(RemoteFloat(21.5f).times(RemoteFloat(21.5f)).hasConstantValue).isTrue()
        assertThat(RemoteFloat(21.5f).minus(RemoteFloat(21.5f)).hasConstantValue).isTrue()
        assertThat(RemoteFloat(21.5f).div(RemoteFloat(21.5f)).hasConstantValue).isTrue()
        assertThat(clamp(10.5f, 20.5f, RemoteFloat(21.5f)).hasConstantValue).isTrue()
        assertThat(
                selectIfGT(RemoteFloat(3f), RemoteFloat(2f), RemoteFloat(100f), RemoteFloat(200f))
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

        makeAndUpdateCoreDocument { context.setNamedFloatOverride("testFloat", 20f) }

        assertThat(context.getFloat(resultId)).isEqualTo(200f)
    }

    @Test
    fun namedRemoteFloat_overriddenValue2() {
        val namedRemoteFloat = RemoteFloat.createNamedRemoteFloat("testFloat", 100f)
        val plusOne = namedRemoteFloat + RemoteFloat(1f)
        val result = plusOne * plusOne
        val resultId = result.getIdForCreationState(creationState)

        makeAndUpdateCoreDocument { context.setNamedFloatOverride("testFloat", 19f) }

        assertThat(context.getFloat(resultId)).isEqualTo(400f)
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }

    private fun makeAndUpdateCoreDocument(runAfterInit: () -> Unit) =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            initializeContext(context)

            runAfterInit()

            for (op in operations) {
                if (op is VariableSupport) {
                    op.updateVariables(context)
                }
                op.apply(context)
            }
        }
}
