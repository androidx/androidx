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
import kotlinx.serialization.builtins.nullable

/**
 * Utility object providing helper functions for encoding and decoding instances of `T` using
 * [SavedState]. It supports serialization, parcelization (on Android), and deserialization.
 */
internal object SavedStateCodecTestUtils {
    /**
     * Test encoding the receiver and then decoding it back. It covers three cases:
     * 1. The receiver with the static type `T`.
     * 2. The receiver with the static type `T?`.
     * 3. `null` with the static type `T?`.
     */
    inline fun <reified T : Any> T.encodeDecode(
        serializer: KSerializer<T>? = null,
        configuration: SavedStateConfiguration? = null,
        doMarshalling: Boolean = true,
        // Only called for the first two cases.
        crossinline checkDecoded: (T, T) -> Unit = { decoded, original ->
            assertThat(decoded).isEqualTo(original)
        },
        // Only called for the first two cases.
        crossinline checkEncoded: SavedStateReader.() -> Unit = { assertThat(size()).isEqualTo(0) },
    ) {
        // Encode and decode `this` with the static type `T`.
        this.encodeDecodeImpl<T>(
            serializer,
            configuration,
            doMarshalling,
            checkDecoded,
            checkEncoded,
        )
        // Encode and decode `this` with the static type `T?`.
        this.encodeDecodeImpl<T?>(
            serializer?.nullable,
            configuration,
            doMarshalling,
            { decoded, original -> checkDecoded(decoded!!, original!!) },
            checkEncoded,
        )
        // Encode and decode `null` with the static type `T?`.
        null.encodeDecodeImpl<T?>(
            serializer?.nullable,
            configuration,
            doMarshalling,
            { decoded, original -> assertThat(decoded).isNull() },
            {
                assertThat(size()).isEqualTo(1)
                assertThat(isNull("")).isTrue()
            },
        )
    }

    /**
     * Test the following steps:
     * 1. Encode `T` to a `SavedState`.
     * 2. Parcelize it to a `Parcel`.
     * 3. Marshall it to a byte array.
     * 4. Unmarshall it back to a parcel.
     * 5. Un-parcelize it back to a `SavedState`.
     * 6. Decode it back to a `T`.
     *
     * (Step 2 to 5 are only performed on Android.)
     *
     * @param serializer The [KSerializer] to use.
     * @param configuration The [SavedStateConfiguration] to use.
     * @param doMarshalling Used to enable/disable step 3 and 4.
     * @param checkDecoded Used to compare receiver with the result from step 6.
     * @param checkEncoded Used to check the content of the result SavedState from step 1.
     */
    private inline fun <reified T> T.encodeDecodeImpl(
        serializer: KSerializer<T>? = null,
        configuration: SavedStateConfiguration? = null,
        doMarshalling: Boolean = true,
        checkDecoded: (T, T) -> Unit = { decoded, original ->
            assertThat(decoded).isEqualTo(original)
        },
        checkEncoded: SavedStateReader.() -> Unit = { assertThat(size()).isEqualTo(0) },
    ) {
        val encoded =
            if (serializer == null) {
                if (configuration == null) {
                    encodeToSavedState(this)
                } else {
                    encodeToSavedState(this, configuration)
                }
            } else {
                if (configuration == null) {
                    encodeToSavedState(serializer, this)
                } else {
                    encodeToSavedState(serializer, this, configuration)
                }
            }
        encoded.read { checkEncoded() }

        val restored = platformEncodeDecode(encoded, doMarshalling)

        val decoded =
            if (serializer == null) {
                if (configuration == null) {
                    decodeFromSavedState(restored)
                } else {
                    decodeFromSavedState(restored, configuration)
                }
            } else {
                if (configuration == null) {
                    decodeFromSavedState(serializer, restored)
                } else {
                    decodeFromSavedState(serializer, restored, configuration)
                }
            }

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
