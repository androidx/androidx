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

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
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
internal class SnapshotStateListSerializerTest {

    @Test
    fun encodeDecode_serializable_withElementSerializer() {
        doTest(mutableStateListOf(Data(1), Data(2)), SnapshotStateListSerializer(serializer()))
    }

    @Test
    fun encodeDecode_serializable() {
        doTest(mutableStateListOf(Data(1), Data(2)))
    }

    @Test
    fun encodeDecode_boolean() {
        doTest(mutableStateListOf(true, false))
    }

    @Test
    fun encodeDecode_short() {
        doTest(mutableStateListOf(123.toShort(), 456.toShort()))
    }

    @Test
    fun encodeDecode_int() {
        doTest(mutableStateListOf(123, 456))
    }

    @Test
    fun encodeDecode_long() {
        doTest(mutableStateListOf(123L, 456L))
    }

    @Test
    fun encodeDecode_float() {
        doTest(mutableStateListOf(3.14F, 2.71F))
    }

    @Test
    fun encodeDecode_double() {
        doTest(mutableStateListOf(3.14, 2.71))
    }

    @Test
    fun encodeDecode_char() {
        doTest(mutableStateListOf('c', 'd'))
    }

    @Test
    fun encodeDecode_strings() {
        doTest(mutableStateListOf("foo", "bar"))
    }

    private inline fun <reified T : Any> doTest(
        original: SnapshotStateList<T>,
        serializer: SnapshotStateListSerializer<T> = SnapshotStateListSerializer(),
    ) {
        val serialized = encodeToSavedState(serializer, original)
        val deserialized = decodeFromSavedState(serializer, serialized)
        assertThat(original.toList()).isEqualTo(deserialized.toList())
    }

    @Serializable private data class Data(val value: Int)
}
