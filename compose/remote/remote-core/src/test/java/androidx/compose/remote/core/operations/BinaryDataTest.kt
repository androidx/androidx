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

package androidx.compose.remote.core.operations

import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.testing.HeadlessRemoteContext
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BinaryDataTest {

    private fun roundTrip(id: Int, type: Int, flags: Int, data: ByteArray): BinaryData {
        val buffer = WireBuffer()
        BinaryData.apply(buffer, id, type, flags, data)
        buffer.setIndex(0)
        assertThat(buffer.readByte()).isEqualTo(Operations.BINARY_DATA)
        val ops = ArrayList<Operation>()
        BinaryData.read(buffer, ops)
        assertThat(ops).hasSize(1)
        assertThat(buffer.available()).isFalse()
        return ops[0] as BinaryData
    }

    @Test
    fun wireFormat_isIdTypeFlagsLengthData() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val buffer = WireBuffer()
        BinaryData.apply(buffer, 42, BinaryData.TYPE_RC_DOC, 7, data)
        buffer.setIndex(0)
        assertThat(buffer.readByte()).isEqualTo(Operations.BINARY_DATA)
        assertThat(buffer.readInt()).isEqualTo(42) // id
        assertThat(buffer.readInt()).isEqualTo(BinaryData.TYPE_RC_DOC) // type
        assertThat(buffer.readInt()).isEqualTo(7) // flags
        assertThat(buffer.readInt()).isEqualTo(5) // length
        for (b in data) {
            assertThat(buffer.readByte()).isEqualTo(b.toInt())
        }
        assertThat(buffer.available()).isFalse()
    }

    @Test
    fun readWrite_roundTripsAllFields() {
        val data = ByteArray(1000) { (it * 31).toByte() }
        val op = roundTrip(42, BinaryData.TYPE_RC_DOC, 3, data)
        assertThat(op.id).isEqualTo(42)
        assertThat(op.type).isEqualTo(BinaryData.TYPE_RC_DOC)
        assertThat(op.flags).isEqualTo(3)
        assertThat(op.data).isEqualTo(data)

        // write() of the read op reproduces the same bytes
        val a = WireBuffer()
        BinaryData.apply(a, 42, BinaryData.TYPE_RC_DOC, 3, data)
        val b = WireBuffer()
        op.write(b)
        assertThat(b.cloneBytes()).isEqualTo(a.cloneBytes())
    }

    @Test
    fun emptyBlob_roundTrips() {
        val op = roundTrip(5, BinaryData.TYPE_RAW, 0, ByteArray(0))
        assertThat(op.data).isEmpty()
    }

    @Test
    fun apply_registersBlobInContextUnderItsId() {
        val op = BinaryData(42, BinaryData.TYPE_RC_DOC, 0, byteArrayOf(9, 8, 7))
        val context = HeadlessRemoteContext()
        op.apply(context)
        assertThat(context.getObject(42)).isSameInstanceAs(op)
    }

    @Test
    fun write_rejectsBlobsOverTheLimit() {
        val buffer = WireBuffer()
        assertThrows(IllegalArgumentException::class.java) {
            BinaryData.apply(
                buffer,
                1,
                BinaryData.TYPE_RAW,
                0,
                ByteArray(Limits.MAX_BINARY_DATA + 1),
            )
        }
    }

    @Test
    fun read_rejectsLengthOverTheLimit() {
        val buffer = WireBuffer()
        buffer.start(Operations.BINARY_DATA)
        buffer.writeInt(1)
        buffer.writeInt(BinaryData.TYPE_RAW)
        buffer.writeInt(0)
        buffer.writeInt(Limits.MAX_BINARY_DATA + 1) // claims a huge length
        buffer.setIndex(1)
        assertThrows(RuntimeException::class.java) { BinaryData.read(buffer, ArrayList()) }
    }
}
