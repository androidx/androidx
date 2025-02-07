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

package androidx.savedstate.serialization

import androidx.kruth.assertThat
import androidx.savedstate.RobolectricTest
import androidx.savedstate.serialization.serializers.MutableStateFlowSerializer
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

internal class MutableStateFlowSerializerTest : RobolectricTest() {

    @Test
    fun encodeDecode_withImplicitSerializer() {
        val state = MutableStateFlow(USER_JOHN_DOE)
        val serializer = MutableStateFlowSerializer<User>()

        val encoded = encodeToSavedState(serializer, state)
        val decoded = decodeFromSavedState(serializer, encoded)

        assertThat(state.value).isEqualTo(decoded.value)
    }

    @Test
    fun encodeDecode_withExplicitSerializer() {
        val state = MutableStateFlow(USER_JOHN_DOE)
        val serializer = MutableStateFlowSerializer(USER_SERIALIZER)

        val encoded = encodeToSavedState(serializer, state)
        val decoded = decodeFromSavedState(serializer, encoded)

        assertThat(state.value).isEqualTo(decoded.value)
    }

    companion object {
        val USER_JOHN_DOE = User(name = "John", surname = "Doe")
        @OptIn(InternalSerializationApi::class) val USER_SERIALIZER = User::class.serializer()
    }

    @Serializable data class User(val name: String = "John", val surname: String = "Doe")
}
