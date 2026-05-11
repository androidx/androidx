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

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class RemoteLongTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    val creationState = RemoteComposeCreationState(AndroidxRcPlatformServices(), Size(1f, 1f))

    @Test
    fun namedRemoteLong_initialValue() {
        val namedRemoteLong = RemoteLong.createNamedRemoteLong("testLong", 100L)
        val longId = namedRemoteLong.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getLong(longId)).isEqualTo(100L)
    }

    @Test
    fun namedRemoteLong_setValue() {
        val namedRemoteLong = RemoteLong.createNamedRemoteLong("testLong", 100L)
        val longId = namedRemoteLong.getIdForCreationState(creationState)

        makeAndUpdateCoreDocument { context.setNamedLong("USER:testLong", 20) }

        assertThat(context.getLong(longId)).isEqualTo(20L)
    }

    @Test
    fun namedRemoteLong_arithmetic() {
        val l1Low = RemoteInt.createNamedRemoteInt("long1.low", 1000)
        val l1High = RemoteInt.createNamedRemoteInt("long1.high", 0)
        val long1 = RemoteLong.fromLowHigh(l1Low, l1High)

        val l2Low = RemoteInt.createNamedRemoteInt("long2.low", 2000)
        val l2High = RemoteInt.createNamedRemoteInt("long2.high", 0)
        val long2 = RemoteLong.fromLowHigh(l2Low, l2High)

        val sum = long1 + long2
        val diff = long1 - long2
        val prod = long1 * long2

        val sumLowId = sum.low.getIdForCreationState(creationState)
        val sumHighId = sum.high.getIdForCreationState(creationState)
        val diffLowId = diff.low.getIdForCreationState(creationState)
        val diffHighId = diff.high.getIdForCreationState(creationState)
        val prodLowId = prod.low.getIdForCreationState(creationState)
        val prodHighId = prod.high.getIdForCreationState(creationState)

        val val1 = 0x123456789ABCDEFL
        val val2 = 0x11111111111111L

        makeAndUpdateCoreDocument {
            context.setNamedIntegerOverride("USER:long1.low", val1.toInt())
            context.setNamedIntegerOverride("USER:long1.high", (val1 shr 32).toInt())

            context.setNamedIntegerOverride("USER:long2.low", val2.toInt())
            context.setNamedIntegerOverride("USER:long2.high", (val2 shr 32).toInt())
        }

        val expectedSum = val1 + val2
        assertThat(context.getInteger(sumLowId)).isEqualTo(expectedSum.toInt())
        assertThat(context.getInteger(sumHighId)).isEqualTo((expectedSum shr 32).toInt())

        val expectedDiff = val1 - val2
        assertThat(context.getInteger(diffLowId)).isEqualTo(expectedDiff.toInt())
        assertThat(context.getInteger(diffHighId)).isEqualTo((expectedDiff shr 32).toInt())

        val expectedProd = val1 * val2
        assertThat(context.getInteger(prodLowId)).isEqualTo(expectedProd.toInt())
        assertThat(context.getInteger(prodHighId)).isEqualTo((expectedProd shr 32).toInt())
    }

    @Test
    fun remoteInt_toRemoteLong() {
        val ri = 123.ri
        val rl = ri.toRemoteLong()
        assertThat(rl.constantValueOrNull).isEqualTo(123L)

        val negativeRi = (-123).ri
        val negativeRl = negativeRi.toRemoteLong()
        assertThat(negativeRl.constantValueOrNull).isEqualTo(-123L)
    }

    @Test
    fun remoteLong_cacheKey() {
        val long1 = RemoteLong(10L)
        val long2 = RemoteLong(10L)
        assertThat(long1.cacheKey).isNotNull()
        assertThat(long1.cacheKey).isEqualTo(long2.cacheKey)
    }

    @Test
    fun remoteLong_cacheKey_plusMinus() {
        val long1 = RemoteLong(10L)
        val long2 = RemoteLong(20L)
        val sum = long1 + long2
        val diff = long1 - long2
        val prod = long1 * long2

        assertThat(sum.cacheKey).isNotEqualTo(diff.cacheKey)
        assertThat(sum.cacheKey).isNotEqualTo(long1.cacheKey)
        assertThat(prod.cacheKey).isNotEqualTo(sum.cacheKey)

        val sum2 = long1 + long2
        assertThat(sum.cacheKey).isEqualTo(sum2.cacheKey)

        val diff2 = long1 - long2
        assertThat(diff.cacheKey).isEqualTo(diff2.cacheKey)

        val prod2 = long1 * long2
        assertThat(prod.cacheKey).isEqualTo(prod2.cacheKey)
    }

    @Test
    fun remoteLong_times() {
        val long1 = RemoteLong(0x000000123456789AL)
        val long2 = RemoteLong(0x0000000087654321L)
        val prod = long1 * long2

        val expected = 0x000000123456789AL * 0x0000000087654321L
        assertThat(prod.constantValueOrNull).isEqualTo(expected)

        val long3 = RemoteLong(-100L)
        val long4 = RemoteLong(50L)
        val prodNegative = long3 * long4
        assertThat(prodNegative.constantValueOrNull).isEqualTo(-5000L)
    }

    @Test
    fun remoteLong_multiply_math() {
        val a = 0x12345678
        val b = 0x87654321.toInt()

        val a0 = a and 0xFFFF
        val a1 = (a shr 16) and 0xFFFF
        val b0 = b and 0xFFFF
        val b1 = (b shr 16) and 0xFFFF

        val m00 = a0 * b0
        val m00_carry = (m00 shr 16) and 0xFFFF

        val mid1 = a1 * b0 + m00_carry
        val mid1_lo = mid1 and 0xFFFF
        val mid1_hi = (mid1 shr 16) and 0xFFFF

        val mid2 = a0 * b1
        val mid2_lo = mid2 and 0xFFFF
        val mid2_hi = (mid2 shr 16) and 0xFFFF

        val mid_lo_sum = mid1_lo + mid2_lo
        val mid_lo_sum_carry = (mid_lo_sum shr 16) and 0xFFFF

        val high_cross = mid1_hi + mid2_hi + mid_lo_sum_carry

        val m11 = a1 * b1
        val upper32 = m11 + high_cross

        val final_low = a * b
        val final_high = upper32

        val A = a.toLong() and 0xFFFFFFFFL
        val B = b.toLong() and 0xFFFFFFFFL
        val prod = A * B
        val expected_low = prod.toInt()
        val expected_high = (prod ushr 32).toInt()

        assertThat(final_low).isEqualTo(expected_low)
        assertThat(final_high).isEqualTo(expected_high)
    }

    @Test
    fun remoteLong_lowHigh_constant() {
        val long1 = RemoteLong(0x123456789ABCDEFL)
        assertThat(long1.low.constantValueOrNull).isEqualTo(0x89ABCDEF.toInt())
        assertThat(long1.high.constantValueOrNull).isEqualTo(0x01234567)
    }

    @Test
    fun remoteLong_toRemoteInt() {
        val long1 = RemoteLong(0x123456789ABCDEFL)
        assertThat(long1.toRemoteInt().constantValueOrNull).isEqualTo(0x89ABCDEF.toInt())
    }

    @Test
    fun remoteLong_plus() {
        val long1 = RemoteLong(0x123456789ABCDEFL)
        val long2 = RemoteLong(0x11111111111111L)
        val sum = long1 + long2
        assertThat(sum.constantValueOrNull).isEqualTo(0x123456789ABCDEFL + 0x11111111111111L)

        val sumDynamic = RemoteLong(long1.low, long1.high) + RemoteLong(long2.low, long2.high)
        assertThat(sumDynamic.constantValueOrNull).isEqualTo(0x123456789ABCDEFL + 0x11111111111111L)
    }

    @Test
    fun remoteLong_plus_carry() {
        val long1 = RemoteLong(0x10000000FFFFFFFFL)
        val long2 = RemoteLong(0x0000000000000001L)
        val sumDynamic = RemoteLong(long1.low, long1.high) + RemoteLong(long2.low, long2.high)
        assertThat(sumDynamic.constantValueOrNull)
            .isEqualTo(0x10000000FFFFFFFFL + 0x0000000000000001L)
    }

    @Test
    fun remoteLong_minus() {
        val long1 = RemoteLong(0x123456789ABCDEFL)
        val long2 = RemoteLong(0x11111111111111L)
        val diff = long1 - long2
        assertThat(diff.constantValueOrNull).isEqualTo(0x123456789ABCDEFL - 0x11111111111111L)

        val diffDynamic = RemoteLong(long1.low, long1.high) - RemoteLong(long2.low, long2.high)
        assertThat(diffDynamic.constantValueOrNull)
            .isEqualTo(0x123456789ABCDEFL - 0x11111111111111L)
    }

    @Test
    fun remoteLong_minus_borrow() {
        val long1 = RemoteLong(0x1000000100000000L)
        val long2 = RemoteLong(0x0000000000000001L)
        val diffDynamic = RemoteLong(long1.low, long1.high) - RemoteLong(long2.low, long2.high)
        assertThat(diffDynamic.constantValueOrNull)
            .isEqualTo(0x1000000100000000L - 0x0000000000000001L)
    }

    @Test
    fun mutableRemoteLong_smokeTest() {
        val mutableLong = MutableRemoteLong(100L)
        val resultId = mutableLong.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getLong(resultId)).isEqualTo(100L)
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
