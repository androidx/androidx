/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.savedstate.compose.serialization.serializers

import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.kruth.assertThat
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.Test
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
internal class SnapshotStateSetSerializerTest {

    @Test
    fun encodeDecode_serializable_withElementSerializer() {
        doTest(mutableStateSetOf(Data(1), Data(2)), SnapshotStateSetSerializer(serializer()))
    }

    @Test
    fun encodeDecode_serializable() {
        doTest(mutableStateSetOf(Data(1), Data(2)))
    }

    @Test
    fun encodeDecode_boolean() {
        doTest(mutableStateSetOf(true, false))
    }

    @Test
    fun encodeDecode_short() {
        doTest(mutableStateSetOf(123.toShort(), 456.toShort()))
    }

    @Test
    fun encodeDecode_int() {
        doTest(mutableStateSetOf(123, 456))
    }

    @Test
    fun encodeDecode_long() {
        doTest(mutableStateSetOf(123L, 456L))
    }

    @Test
    fun encodeDecode_float() {
        doTest(mutableStateSetOf(3.14F, 2.71F))
    }

    @Test
    fun encodeDecode_double() {
        doTest(mutableStateSetOf(3.14, 2.71))
    }

    @Test
    fun encodeDecode_char() {
        doTest(mutableStateSetOf('c', 'd'))
    }

    @Test
    fun encodeDecode_strings() {
        doTest(mutableStateSetOf("foo", "bar"))
    }

    private inline fun <reified T : Any> doTest(
        original: SnapshotStateSet<T>,
        serializer: SnapshotStateSetSerializer<T> = SnapshotStateSetSerializer(),
    ) {
        val serialized = encodeToSavedState(serializer, original)
        val deserialized = decodeFromSavedState(serializer, serialized)
        assertThat(deserialized).isEqualTo(original)
    }

    @Serializable private data class Data(val value: Int)
}
