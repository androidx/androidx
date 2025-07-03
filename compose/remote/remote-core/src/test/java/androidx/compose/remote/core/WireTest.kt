/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.core

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Test

/** Test of WireBuffer class verifying internal consistency */
class WireTest {
    @Test
    fun testWireBuffer01() {
        val initSize = 23
        val w = WireBuffer(initSize)

        assertEquals(false, w.available()) // nothing to read at this point
        assertEquals(0, w.size())
        assertEquals(initSize, w.max_size)

        val a = 122
        val b = 2234
        val c = 3123456
        val d = 1234567886541234L
        val e = "hello"
        val f = true
        val g = 1.2345678901234
        val h = 1.2345678f

        w.writeByte(a)
        w.writeShort(b)
        w.writeInt(c)
        w.writeLong(d)
        w.writeUTF8(e)
        w.writeBoolean(f)
        w.writeDouble(g)
        w.writeFloat(h)
        val buf = ByteArray(12)

        val random = Random()
        random.nextBytes(buf)
        w.writeBuffer(buf)
        w.writeFloat(h)

        w.index = 0
        assertEquals(a, w.readByte())
        assertEquals(b, w.readShort())
        assertEquals(c, w.readInt())
        assertEquals(d, w.readLong())
        assertEquals(e, w.readUTF8())
        assertEquals(f, w.readBoolean())
        assertEquals(g, w.readDouble(), 0.0)
        assertEquals(h, w.readFloat())

        val out = w.readBuffer()
        assertEquals(buf.contentToString(), out.contentToString())
        assertEquals(h, w.readFloat())
    }

    @Test
    fun testWireBuffer02() {
        val initSize = 32
        val w = WireBuffer(initSize)
        val buf = ByteArray(3200)

        val random = Random()
        random.nextBytes(buf)

        for (byte in buf) {
            w.writeByte(byte.toInt())
            w.writeInt(byte.toInt())
        }

        assertEquals(16384, w.max_size)

        w.index = 0

        for (byte in buf) {
            assertEquals(byte, w.readByte().toByte())
            assertEquals(byte.toInt(), w.readInt())
        }
    }
}
