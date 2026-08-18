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
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateReader
import androidx.savedstate.read
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * Utility object providing helper functions for encoding and decoding instances of `T` using
 * [SavedState]. It supports serialization, parcelization (on Android), and deserialization.
 */
internal object SavedStateCodecTestUtils {
    /**
     * Test the following steps:
     * 1. Encode `T` to a `SavedState`.
     * 2. Simulates platform-specific transmission (only performed on Android).
     * 3. Decode it back to a `T`.
     */
    inline fun <reified T> T.encodeDecode(
        configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
        doMarshalling: Boolean = true,
        noinline checkDecoded: (T, T) -> Unit = { decoded, original ->
            assertThat(decoded).isEqualTo(original)
        },
        noinline checkEncoded: SavedStateReader.() -> Unit = { assertThat(size()).isEqualTo(0) },
    ) {
        encodeDecode(
            serializer = configuration.serializersModule.serializer<T>(),
            configuration = configuration,
            doMarshalling = doMarshalling,
            checkDecoded = checkDecoded,
            checkEncoded = checkEncoded,
        )
    }

    /**
     * Test the following steps:
     * 1. Encode `T` to a `SavedState` using explicit [serializer].
     * 2. Simulates platform-specific transmission (only performed on Android).
     * 3. Decode it back to a `T`.
     */
    fun <T> T.encodeDecode(
        serializer: KSerializer<T>,
        configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
        doMarshalling: Boolean = true,
        checkDecoded: (T, T) -> Unit = { decoded, original ->
            assertThat(decoded).isEqualTo(original)
        },
        checkEncoded: SavedStateReader.() -> Unit = { assertThat(size()).isEqualTo(0) },
    ) {
        val encoded = encodeToSavedState(serializer, value = this, configuration)
        val restored = platformEncodeDecode(encoded, doMarshalling)
        val decoded = decodeFromSavedState(serializer, restored, configuration)
        encoded.read { checkEncoded() }
        checkDecoded(decoded, this)
    }
}

/**
 * Platform-specific function for encoding and decoding `SavedState` objects.
 *
 * This function ensures that the encoded state is processed through the platform's parcelization
 * and unparcelization logic (on Android) to simulate real-world behavior.
 *
 * @param savedState The `SavedState` to be encoded and then decoded.
 * @param doMarshalling A boolean flag indicating whether to perform bytes marshalling.
 * @return The resulting `SavedState` after going through the platform encoding-decoding process.
 */
expect fun platformEncodeDecode(savedState: SavedState, doMarshalling: Boolean = true): SavedState
