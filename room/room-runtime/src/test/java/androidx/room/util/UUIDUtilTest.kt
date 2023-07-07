/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.util

import androidx.kruth.assertThat
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.random.Random
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UUIDUtilTest {

    @Test
    fun convertToByte() {
        val uuid = UUID.randomUUID()

        val expected = ByteBuffer.wrap(ByteArray(16)).apply {
            putLong(uuid.mostSignificantBits)
            putLong(uuid.leastSignificantBits)
        }.array()

        val result = convertUUIDToByte(uuid)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun convertToUUID() {
        val byteArray = Random.Default.nextBytes(ByteArray(16))

        val buffer = ByteBuffer.wrap(byteArray)
        val expected = UUID(buffer.long, buffer.long)

        val result = convertByteToUUID(byteArray)

        assertThat(result).isEqualTo(expected)
    }
}
