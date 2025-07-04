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
import androidx.savedstate.IgnoreWebTarget
import androidx.savedstate.RobolectricTest
import androidx.savedstate.serialization.SavedStateCodecTestUtils.encodeDecode
import androidx.savedstate.serialization.serializers.MutableStateFlowSerializer
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

@IgnoreWebTarget
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

    @Test
    fun encodeDecode_formatSpecificRepresentationShouldWork() {
        val state = MutableStateFlow(listOf(1, 2, 3))
        val serializer = MutableStateFlowSerializer<List<Int>>()

        val encoded = encodeToSavedState(serializer, state)
        val decoded = decodeFromSavedState(serializer, encoded)

        assertThat(state.value).isEqualTo(decoded.value)
    }

    @Test
    fun serializerOnProperties() {
        @Serializable
        data class Foo(
            @Serializable(with = MutableStateFlowSerializer::class)
            val state: MutableStateFlow<User>
        )

        val original = Foo(MutableStateFlow(USER_JOHN_DOE))
        val encoded = encodeToSavedState(original)
        val decoded = decodeFromSavedState<Foo>(encoded)

        assertThat(decoded.state.value).isEqualTo(original.state.value)
    }

    @Test
    fun encodeDecode_primitivesShouldWork() {
        testEncodeDecode(MutableStateFlow(true))
        testEncodeDecode(MutableStateFlow(123.toShort()))
        testEncodeDecode(MutableStateFlow(123))
        testEncodeDecode(MutableStateFlow(123L))
        testEncodeDecode(MutableStateFlow(3.14F))
        testEncodeDecode(MutableStateFlow(3.14))
        testEncodeDecode(MutableStateFlow('c'))
        testEncodeDecode(MutableStateFlow("foo"))
    }

    @Test
    fun encodeDecode_enumsShouldWork() {
        testEncodeDecode(MutableStateFlow(MyEnum.B))
    }

    @Test
    fun encodeDecode_contextualsShouldWork() {
        testEncodeDecode(
            mutableState = MutableStateFlow(USER_JOHN_DOE),
            serializer = MutableStateFlowSerializer(USER_SERIALIZER),
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
        testEncodeDecode(MutableStateFlow(USER_JOHN_DOE))
    }

    @Test
    fun encodeDecode_objectsShouldWork() {
        testEncodeDecode(MutableStateFlow(MyObject))
    }

    @Test
    fun encodeDecode_listsShouldWork() {
        testEncodeDecode(MutableStateFlow(listOf(1, 3, 5)))
    }

    @Test
    fun encodeDecode_mapsShouldWork() {
        testEncodeDecode(MutableStateFlow(mapOf(3 to "foo", 4 to "bar")))
    }

    @Test
    fun fallbackShouldWork() {
        MutableStateFlow("foo")
            .encodeDecode(
                checkDecoded = { decoded, original ->
                    assertThat(decoded.value).isEqualTo(original.value)
                },
                checkEncoded = {
                    assertThat(size()).isEqualTo(1)
                    assertThat(getString("")).isEqualTo("foo")
                },
            )
    }

    @Test
    fun fallbackShouldWorkInPlugin() {
        @Serializable
        // The contextual serializer is still needed.
        // See https://github.com/Kotlin/kotlinx.serialization/issues/2969.
        data class Foo(@Contextual val bar: MutableStateFlow<String>)

        Foo(MutableStateFlow("foo"))
            .encodeDecode(
                checkDecoded = { decoded, original ->
                    assertThat(decoded.bar.value).isEqualTo(original.bar.value)
                },
                checkEncoded = {
                    assertThat(size()).isEqualTo(1)
                    assertThat(getString("bar")).isEqualTo("foo")
                },
            )
    }

    private inline fun <reified T : Any> testEncodeDecode(
        mutableState: MutableStateFlow<T>,
        serializer: KSerializer<MutableStateFlow<T>> = MutableStateFlowSerializer<T>(),
        configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
    ) {
        val encoded =
            encodeToSavedState(
                serializer = serializer,
                configuration = configuration,
                value = mutableState,
            )
        val decoded =
            decodeFromSavedState<MutableStateFlow<T>>(
                deserializer = serializer,
                configuration = configuration,
                savedState = encoded,
            )
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
