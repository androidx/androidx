/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.savedstate.serialization.serializers

import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.serialization.SavedStateDecoder
import androidx.savedstate.serialization.SavedStateEncoder
import androidx.savedstate.write
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A serializer for [SavedState]. This serializer uses [SavedState]'s API to save/load a
 * [SavedState].
 *
 * Note that this serializer should be used with
 * [androidx.savedstate.serialization.SavedStateEncoder] or
 * [androidx.savedstate.serialization.SavedStateDecoder] only. Using it with other Encoders/Decoders
 * may throw [IllegalArgumentException].
 *
 * @sample androidx.savedstate.savedStateSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
public object SavedStateSerializer : KSerializer<SavedState> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("androidx.savedstate.SavedState")

    override fun serialize(encoder: Encoder, value: SavedState) {
        require(encoder is SavedStateEncoder) {
            encoderErrorMessage(descriptor.serialName, encoder)
        }
        encoder.run {
            if (key == "") { // root
                savedState.write { putAll(value) }
            } else {
                savedState.write { putSavedState(key, value) }
            }
        }
    }

    override fun deserialize(decoder: Decoder): SavedState {
        require(decoder is SavedStateDecoder) {
            decoderErrorMessage(descriptor.serialName, decoder)
        }
        return decoder.run {
            if (key == "") { // root
                savedState
            } else {
                savedState.read { getSavedState(key) }
            }
        }
    }
}

internal fun encoderErrorMessage(serialName: String, encoder: Encoder): String {
    return "Cannot serialize $serialName with '${encoder::class.simpleName}'." +
        " This serializer can only be used with SavedStateEncoder." +
        " Use 'encodeToSavedState' instead."
}

internal fun decoderErrorMessage(serialName: String, decoder: Decoder): String {
    return "Cannot deserialize $serialName with '${decoder::class.simpleName}'." +
        " This serializer can only be used with SavedStateDecoder." +
        " Use 'decodeFromSavedState' instead."
}
