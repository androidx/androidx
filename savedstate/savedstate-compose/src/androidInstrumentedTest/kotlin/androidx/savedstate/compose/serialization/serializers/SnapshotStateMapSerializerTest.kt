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

package androidx.savedstate.compose.serialization.serializers

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
internal class SnapshotStateMapSerializerTest {

    @Test
    fun encodeDecode_serializable_withElementSerializer() {
        val original = mutableStateMapOf(Data(11) to Data(21), Data(12) to Data(22))
        val serializer = SnapshotStateMapSerializer<Data, Data>(serializer(), serializer())
        doTest(original, serializer)
    }

    @Test
    fun encodeDecode_serializable() {
        val original = mutableStateMapOf(Data(11) to Data(21), Data(12) to Data(22))
        val serializer = SnapshotStateMapSerializer<Data, Data>()
        doTest(original, serializer)
    }

    @Test
    fun encodeDecode_boolean() {
        doTest(mutableStateMapOf(true to false, false to false))
    }

    @Test
    fun encodeDecode_short() {
        doTest(mutableStateMapOf(123.toShort() to 456.toShort(), 789.toShort() to 1011.toShort()))
    }

    @Test
    fun encodeDecode_int() {
        doTest(mutableStateMapOf(123 to 456, 789 to 1011))
    }

    @Test
    fun encodeDecode_long() {
        doTest(mutableStateMapOf(123L to 456L, 789L to 1011L))
    }

    @Test
    fun encodeDecode_float() {
        doTest(mutableStateMapOf(3.14F to 2.71F, 1.0F to 2.0F))
    }

    @Test
    fun encodeDecode_double() {
        doTest(mutableStateMapOf(3.14 to 2.71, 1.0 to 2.0))
    }

    @Test
    fun encodeDecode_char() {
        doTest(mutableStateMapOf('c' to 'd', 'e' to 'f'))
    }

    @Test
    fun encodeDecode_strings() {
        doTest(mutableStateMapOf("foo" to "bar", "baz" to "qux"))
    }

    private inline fun <reified K, reified V> doTest(
        original: SnapshotStateMap<K, V>,
        serializer: SnapshotStateMapSerializer<K, V> = SnapshotStateMapSerializer<K, V>(),
    ) {
        val serialized = encodeToSavedState(serializer, original)
        val deserialized = decodeFromSavedState(serializer, serialized)
        assertThat(original.toMap()).isEqualTo(deserialized.toMap())
    }

    @Serializable private data class Data(val value: Int)
}
