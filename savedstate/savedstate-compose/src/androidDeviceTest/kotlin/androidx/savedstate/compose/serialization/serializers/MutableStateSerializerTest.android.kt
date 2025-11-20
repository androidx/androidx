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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.kruth.assertThat
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.Test
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
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

    @Test
    fun encodeDecode_formatSpecificRepresentationShouldWork() {
        val state = mutableStateOf(listOf(1, 2, 3))
        val serializer = MutableStateSerializer<List<Int>>()

        val encoded = encodeToSavedState(serializer, state)
        val decoded = decodeFromSavedState(serializer, encoded)

        assertThat(state.value).isEqualTo(decoded.value)
    }

    @Test
    fun serializerOnProperties() {
        @Serializable
        data class Foo(
            @Serializable(with = MutableStateSerializer::class) val state: MutableState<User>
        )

        val original = Foo(mutableStateOf(USER_JOHN_DOE))
        val encoded = encodeToSavedState(original)
        val decoded = decodeFromSavedState<Foo>(encoded)

        assertThat(decoded.state.value).isEqualTo(original.state.value)
    }

    @Test
    fun encodeDecode_primitivesShouldWork() {
        testEncodeDecode(mutableStateOf(true))
        testEncodeDecode(mutableStateOf(123.toShort()))
        testEncodeDecode(mutableStateOf(123))
        testEncodeDecode(mutableStateOf(123L))
        testEncodeDecode(mutableStateOf(3.14F))
        testEncodeDecode(mutableStateOf(3.14))
        testEncodeDecode(mutableStateOf('c'))
        testEncodeDecode(mutableStateOf("foo"))
    }

    @Test
    fun encodeDecode_enumsShouldWork() {
        testEncodeDecode(mutableStateOf(MyEnum.B))
    }

    @Test
    fun encodeDecode_contextualsShouldWork() {
        testEncodeDecode(
            mutableStateOf(USER_JOHN_DOE),
            serializer = MutableStateSerializer(USER_SERIALIZER),
            configuration =
                SavedStateConfiguration {
                    serializersModule = SerializersModule {
                        contextual(User::class, serializer<User>())
                    }
                },
        )
    }

    @Test
    fun encodeDecode_classesShouldWork() {
        testEncodeDecode(mutableStateOf(USER_JOHN_DOE))
    }

    @Test
    fun encodeDecode_objectsShouldWork() {
        testEncodeDecode(mutableStateOf(MyObject))
    }

    @Test
    fun encodeDecode_listsShouldWork() {
        testEncodeDecode(mutableStateOf(listOf(1, 3, 5)))
    }

    @Test
    fun encodeDecode_mapsShouldWork() {
        testEncodeDecode(mutableStateOf(mapOf(3 to "foo", 4 to "bar")))
    }

    private inline fun <reified T : Any> testEncodeDecode(
        mutableState: MutableState<T>,
        serializer: KSerializer<MutableState<T>> = MutableStateSerializer<T>(),
        configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
    ) {
        val encoded =
            encodeToSavedState(
                serializer = serializer,
                configuration = configuration,
                value = mutableState,
            )
        val decoded =
            decodeFromSavedState<MutableState<T>>(deserializer = serializer, savedState = encoded)

        assertThat(decoded.value).isEqualTo(mutableState.value)
    }

    companion object {
        val USER_JOHN_DOE = User(name = "John", surname = "Doe")
        val USER_SERIALIZER = serializer<User>()
    }

    @Serializable data class User(val name: String, val surname: String)

    // `@Serializable` is needed for using the enum as root in native and js.
    @Serializable
    private enum class MyEnum {
        A,
        B,
    }

    @Serializable
    private data object MyObject {
        val user = USER_JOHN_DOE
    }
}
