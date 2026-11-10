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
import androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX
import androidx.compose.remote.core.operations.TextTransform
import androidx.compose.remote.creation.compose.capture.createTestCreationState
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt.Companion.createNamedRemoteInt
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import com.google.common.collect.ImmutableList
import com.google.common.truth.Truth.assertThat
import java.text.DecimalFormat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteStringTest {

    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    val creationState = createTestCreationState(profiles = PROFILE_ANDROIDX)

    @Test
    fun toRemoteStringWithPostfix() {
        val percentage = RemoteFloat(45.5f)
        val percentageString = percentage.toRemoteString(DecimalFormat("#0.0")) + RemoteString("%")
        val percentageStringId = percentageString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(percentageStringId)).isEqualTo("45.5%")
    }

    @Test
    fun floatIfLessThan_less() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfLt(
                v,
                RemoteFloat(10000f),
                v.toRemoteString(DecimalFormat("#0")),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfLessThan_equal() {
        val v = RemoteFloat(12345f)
        val conditionalString =
            selectIfLt(
                v,
                RemoteFloat(12345f),
                v.toRemoteString(DecimalFormat("###0")),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("12K")
    }

    @Test
    fun floatIfLessThan_greater() {
        val v = RemoteFloat(12345f)
        val conditionalString =
            selectIfLt(
                v,
                RemoteFloat(10000f),
                v.toRemoteString(DecimalFormat("###0")),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("12K")
    }

    @Test
    fun intIfLessThan_less() {
        val v = RemoteInt(1234)
        val conditionalString =
            selectIfLt(
                v,
                RemoteInt(10000),
                v.toRemoteString(DecimalFormat("####")),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun intIfLessThan_equal() {
        val v = RemoteInt(12345)
        val conditionalString =
            selectIfLt(
                v,
                RemoteInt(12345),
                v.toRemoteString(DecimalFormat("####")),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("12K")
    }

    @Test
    fun intIfLessThan_greater() {
        val v = RemoteInt(12345)
        val conditionalString =
            selectIfLt(
                v,
                RemoteInt(10000),
                v.toRemoteString(DecimalFormat("####")),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("12K")
    }

    @Test
    fun floatIfLessEqual_less() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfLe(
                v,
                RemoteFloat(10000f),
                v.toRemoteString(DecimalFormat("###0")),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfLessEqual_equal() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfLe(
                v,
                RemoteFloat(1234f),
                v.toRemoteString(DecimalFormat("###0")),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfLessEqual_greater() {
        val v = RemoteFloat(10000f)
        val conditionalString =
            selectIfLe(
                v,
                RemoteFloat(9999f),
                v.toRemoteString(DecimalFormat("###0")),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("10K")
    }

    @Test
    fun intIfLessEqual_less() {
        val v = RemoteInt(1234)
        val conditionalString =
            selectIfLe(
                v,
                RemoteInt(10000),
                v.toRemoteString(DecimalFormat("####")),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun intIfLessEqual_equal() {
        val v = RemoteInt(1234)
        val conditionalString =
            selectIfLe(
                v,
                RemoteInt(1234),
                v.toRemoteString(DecimalFormat("####")),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun intIfLessEqual_greater() {
        val v = RemoteInt(10000)
        val conditionalString =
            selectIfLe(
                v,
                RemoteInt(9999),
                v.toRemoteString(DecimalFormat("####")),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("10K")
    }

    @Test
    fun floatIfGreaterThan_less() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfGt(
                v,
                RemoteFloat(10000f),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("###0")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfGreaterThan_equal() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfGt(
                v,
                RemoteFloat(1234f),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("###0")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfGreaterThan_greater() {
        val v = RemoteFloat(12345f)
        val conditionalString =
            selectIfGt(
                v,
                RemoteFloat(10000f),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("###0")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("12K")
    }

    @Test
    fun intIfGreaterThan_less() {
        val v = RemoteInt(1234)
        val conditionalString =
            selectIfGt(
                v,
                RemoteInt(10000),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("####")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun intIfGreaterThan_equal() {
        val v = RemoteInt(1234)
        val conditionalString =
            selectIfGt(
                v,
                RemoteInt(1234),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("####")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun intIfGreaterThan_greater() {
        val v = RemoteInt(12345)
        val conditionalString =
            selectIfGt(
                v,
                RemoteInt(10000),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("####")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("12K")
    }

    @Test
    fun floatIfGreaterEqual_less() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfGe(
                v,
                RemoteFloat(10000f),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("###0")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfGreaterEqual_equal() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfGe(
                v,
                RemoteFloat(10000f),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("###0")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfGreaterEqual_greater() {
        val v = RemoteFloat(10000f)
        val conditionalString =
            selectIfGe(
                v,
                RemoteFloat(10000f),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("###0")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("10K")
    }

    @Test
    fun intIfGreaterEqual_less() {
        val v = RemoteInt(1234)
        val conditionalString =
            selectIfGe(
                v,
                RemoteInt(10000),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("####")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun intIfGreaterEqual_equal() {
        val v = RemoteInt(10000)
        val conditionalString =
            selectIfGe(
                v,
                RemoteInt(10000),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("####")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("10K")
    }

    @Test
    fun intIfGreaterEqual_greater() {
        val v = RemoteInt(10000)
        val conditionalString =
            selectIfGe(
                v,
                RemoteInt(10000),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("####")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("10K")
    }

    @Test
    fun hasConstantValue_decimalFormat() {
        assertThat(
                RemoteTimeVariables.continuousSeconds
                    .toRemoteString(DecimalFormat("#0.00"))
                    .hasConstantValue
            )
            .isFalse()
    }

    @Test
    fun uppercase_nop_decimalFormat_elidesTextTransform() {
        val percentage = RemoteFloat(45.5f)
        val source = percentage.toRemoteString(DecimalFormat("#0.0")) + RemoteString("%")
        val result = source.uppercase()
        val resultId = result.getIdForCreationState(creationState)

        val doc = makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("45.5%")
        assertThat(resultId).isEqualTo(source.getIdForCreationState(creationState))
        assertThat(doc.operations.filterIsInstance<TextTransform>()).isEmpty()
    }

    @Test
    fun lowercase_nop_decimalFormat_elidesTextTransform() {
        val percentage = RemoteFloat(45.5f)
        val source = percentage.toRemoteString(DecimalFormat("#0.0")) + RemoteString("%")
        val result = source.lowercase()
        val resultId = result.getIdForCreationState(creationState)

        val doc = makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("45.5%")
        assertThat(resultId).isEqualTo(source.getIdForCreationState(creationState))
        assertThat(doc.operations.filterIsInstance<TextTransform>()).isEmpty()
    }

    @Test
    fun remoteFloat_toRemoteString_caching() {
        val rf = createNamedRemoteFloat("testFloat", 1.0f)

        val rs1 = rf.toRemoteString(DecimalFormat("#0.##"))
        val rs2 = rf.toRemoteString(DecimalFormat("#0.##"))
        val rs3 = rf.toRemoteString(DecimalFormat("00.##")) // Different 'before'
        val rs4 = rf.toRemoteString(DecimalFormat("#0.00")) // Different 'after'
        val rs5 = rf.toRemoteString(DecimalFormat("#0.##;(#0.##)")) // Different 'flags'

        val id1 = rs1.getIdForCreationState(creationState)
        val id2 = rs2.getIdForCreationState(creationState)
        val id3 = rs3.getIdForCreationState(creationState)
        val id4 = rs4.getIdForCreationState(creationState)
        val id5 = rs5.getIdForCreationState(creationState)

        assertThat(id1).isEqualTo(id2)
        assertThat(id1).isNotEqualTo(id3)
        assertThat(id1).isNotEqualTo(id4)
        assertThat(id1).isNotEqualTo(id5)
        assertThat(ImmutableList.of(id1, id3, id4, id5)).containsNoDuplicates()
    }

    @Test
    fun remoteInt_toRemoteString_caching() {
        val ri = createNamedRemoteInt("testInt", 1)

        val rs1 = ri.toRemoteString(DecimalFormat("#"))
        val rs2 = ri.toRemoteString(DecimalFormat("#"))
        val rs3 = ri.toRemoteString(DecimalFormat("00")) // Different 'before'
        val rs4 = ri.toRemoteString(DecimalFormat("#;(#)")) // Different 'flags'

        val id1 = rs1.getIdForCreationState(creationState)
        val id2 = rs2.getIdForCreationState(creationState)
        val id3 = rs3.getIdForCreationState(creationState)
        val id4 = rs4.getIdForCreationState(creationState)

        assertThat(id1).isEqualTo(id2)
        assertThat(id1).isNotEqualTo(id3)
        assertThat(id1).isNotEqualTo(id4)
    }

    @Test
    fun differentRemoteFloats_differentIds() {
        val rf1 = createNamedRemoteFloat("testFloat1", 1.0f)
        val rf2 = createNamedRemoteFloat("testFloat2", 2.0f)

        val rs1 = rf1.toRemoteString(DecimalFormat("###0"))
        val rs2 = rf2.toRemoteString(DecimalFormat("###0"))

        val id1 = rs1.getIdForCreationState(creationState)
        val id2 = rs2.getIdForCreationState(creationState)

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun differentRemoteInts_differentIds() {
        val ri1 = createNamedRemoteInt("testInt1", 1)
        val ri2 = createNamedRemoteInt("testInt2", 2)

        val rs1 = ri1.toRemoteString(DecimalFormat("#"))
        val rs2 = ri2.toRemoteString(DecimalFormat("#"))

        val id1 = rs1.getIdForCreationState(creationState)
        val id2 = rs2.getIdForCreationState(creationState)

        assertThat(id1).isNotEqualTo(id2)
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}
