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

import androidx.compose.runtime.mutableStateOf
import androidx.kruth.assertThat
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class MutableStateSerializerTest {

    @Test
    fun encodeDecode_withImplicitSerializer() {
        val state = mutableStateOf(USER_JOHN_DOE)
        val serializer = MutableStateSerializer<User>()

        val encoded = encodeToSavedState(serializer, state)
        val decoded = decodeFromSavedState(serializer, encoded)

        assertThat(state.value).isEqualTo(decoded.value)
    }

    @Test
    fun encodeDecode_withExplicitSerializer() {
        val state = mutableStateOf(USER_JOHN_DOE)
        val serializer = MutableStateSerializer(USER_SERIALIZER)

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
