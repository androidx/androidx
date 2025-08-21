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

/**
 * Utility object providing helper functions for encoding and decoding instances of `T` using
 * [SavedState]. It supports serialization, parcelization (on Android), and deserialization.
 */
internal object SavedStateCodecTestUtils {

    /**
     * Test the following steps: 1. encode `T` to a `SavedState`, 2. parcelize it to a `Parcel`,
     * 3. marshall it to a byte array, 4. unmarshall it back to a parcel, 5. un-parcelize it back to
     *    a `SavedState`, and 6. decode it back to a `T`. Step 2 to 5 are only performed on Android.
     *
     * Here's the whole process:
     *
     * (A)Serializable -1-> (B)SavedState -2-> (C)Parcel -3-> (D)byte array -4-> (E)Parcel -5->
     * (F)SavedState -6-> (G)Serializable
     *
     * @param doMarshalling Used to enable/disable step 3 and 4.
     * @param checkEncoded Used to check the content of "B"
     * @param checkDecoded Used to compare the instances of "G" and "A".
     */
    inline fun <reified T : Any> T.encodeDecode(
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
