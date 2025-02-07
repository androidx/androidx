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

@file:JvmName("BuiltInSerializerKt")

package androidx.savedstate.serialization.serializers

import android.os.IBinder
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.serialization.SavedStateDecoder
import androidx.savedstate.serialization.SavedStateEncoder
import androidx.savedstate.write
import java.io.Serializable as JavaSerializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A serializer for [Size]. This serializer uses [SavedState]'s API directly to save/load a [Size].
 *
 * Note that this serializer should be used with [SavedStateEncoder] or [SavedStateDecoder] only.
 * Using it with other Encoders/Decoders may throw [IllegalArgumentException].
 *
 * @sample androidx.savedstate.sizeSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
public class SizeSerializer : KSerializer<Size> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Size")

    override fun serialize(encoder: Encoder, value: Size) {
        require(encoder is SavedStateEncoder) {
            encoderErrorMessage(descriptor.serialName, encoder)
        }
        encoder.run { savedState.write { putSize(key, value) } }
    }

    override fun deserialize(decoder: Decoder): Size {
        require(decoder is SavedStateDecoder) {
            decoderErrorMessage(descriptor.serialName, decoder)
        }
        return decoder.run { savedState.read { getSize(key) } }
    }
}

/**
 * A serializer for [SizeF]. This serializer uses [SavedState]'s API directly to save/load a
 * [SizeF].
 *
 * Note that this serializer should be used with [SavedStateEncoder] or [SavedStateDecoder] only.
 * Using it with other Encoders/Decoders may throw [IllegalArgumentException].
 *
 * @sample androidx.savedstate.sizeFSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
public class SizeFSerializer : KSerializer<SizeF> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SizeF")

    override fun serialize(encoder: Encoder, value: SizeF) {
        require(encoder is SavedStateEncoder) {
            encoderErrorMessage(descriptor.serialName, encoder)
        }
        encoder.run { savedState.write { putSizeF(key, value) } }
    }

    override fun deserialize(decoder: Decoder): SizeF {
        require(decoder is SavedStateDecoder) {
            decoderErrorMessage(descriptor.serialName, decoder)
        }
        return decoder.run { savedState.read { getSizeF(key) } }
    }
}

/**
 * A serializer for [CharSequence]. This serializer uses [SavedState]'s API directly to save/load a
 * [CharSequence].
 *
 * Note that this serializer should be used with [SavedStateEncoder] or [SavedStateDecoder] only.
 * Using it with other Encoders/Decoders may throw [IllegalArgumentException].
 *
 * @sample androidx.savedstate.charSequenceSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
public class CharSequenceSerializer : KSerializer<CharSequence> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CharSequence")

    override fun serialize(encoder: Encoder, value: CharSequence) {
        require(encoder is SavedStateEncoder) {
            encoderErrorMessage(descriptor.serialName, encoder)
        }
        encoder.run { savedState.write { putCharSequence(key, value) } }
    }

    override fun deserialize(decoder: Decoder): CharSequence {
        require(decoder is SavedStateDecoder) {
            decoderErrorMessage(descriptor.serialName, decoder)
        }
        return decoder.run { savedState.read { getCharSequence(key) } }
    }
}

/**
 * A serializer for [java.io.Serializable]. This serializer uses [SavedState]'s API directly to
 * save/load a [java.io.Serializable]. You must extend this serializer for each of your
 * [java.io.Serializable] subclasses.
 *
 * Note that this serializer should be used with [SavedStateEncoder] or [SavedStateDecoder] only.
 * Using it with other Encoders/Decoders may throw [IllegalArgumentException].
 *
 * @sample androidx.savedstate.serializableSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
public abstract class JavaSerializableSerializer<T : JavaSerializable> : KSerializer<T> {
    final override val descriptor: SerialDescriptor = buildClassSerialDescriptor("JavaSerializable")

    final override fun serialize(encoder: Encoder, value: T) {
        require(encoder is SavedStateEncoder) {
            encoderErrorMessage(descriptor.serialName, encoder)
        }
        encoder.run { savedState.write { putJavaSerializable(key, value as JavaSerializable) } }
    }

    @Suppress("UNCHECKED_CAST")
    final override fun deserialize(decoder: Decoder): T {
        require(decoder is SavedStateDecoder) {
            decoderErrorMessage(descriptor.serialName, decoder)
        }
        return decoder.run { savedState.read { getJavaSerializable<JavaSerializable>(key) as T } }
    }
}

/**
 * A serializer for [Parcelable]. This serializer uses [SavedState]'s API directly to save/load a
 * [Parcelable]. You must extend this serializer for each of your [Parcelable] subclasses.
 *
 * Note that this serializer should be used with [SavedStateEncoder] or [SavedStateDecoder] only.
 * Using it with other Encoders/Decoders may throw [IllegalArgumentException].
 *
 * @sample androidx.savedstate.parcelableSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
public abstract class ParcelableSerializer<T : Parcelable> : KSerializer<T> {
    final override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Parcelable")

    final override fun serialize(encoder: Encoder, value: T) {
        require(encoder is SavedStateEncoder) {
            encoderErrorMessage(descriptor.serialName, encoder)
        }
        encoder.run { savedState.write { putParcelable(key, value as Parcelable) } }
    }

    @Suppress("UNCHECKED_CAST")
    final override fun deserialize(decoder: Decoder): T {
        require(decoder is SavedStateDecoder) {
            decoderErrorMessage(descriptor.serialName, decoder)
        }
        return decoder.run { savedState.read { getParcelable<Parcelable>(key) as T } }
    }
}

/**
 * A serializer for [IBinder]. This serializer uses [SavedState]'s API directly to save/load a
 * [IBinder].
 *
 * Note that this serializer should be used with [SavedStateEncoder] or [SavedStateDecoder] only.
 * Using it with other Encoders/Decoders may throw [IllegalArgumentException].
 *
 * @sample androidx.savedstate.iBinderSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
public class IBinderSerializer : KSerializer<IBinder> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("IBinder")

    override fun serialize(encoder: Encoder, value: IBinder) {
        require(encoder is SavedStateEncoder) {
            encoderErrorMessage(descriptor.serialName, encoder)
        }
        encoder.run { savedState.write { putBinder(key, value) } }
    }

    override fun deserialize(decoder: Decoder): IBinder {
        require(decoder is SavedStateDecoder) {
            decoderErrorMessage(descriptor.serialName, decoder)
        }
        return decoder.run { savedState.read { getBinder(key) } }
    }
}

/**
 * A serializer for [Array<CharSequence>]. This serializer uses [SavedState]'s API directly to
 * save/load a [Array<CharSequence>].
 *
 * Note that this serializer should be used with [SavedStateEncoder] or [SavedStateDecoder] only.
 * Using it with other Encoders/Decoders may throw [IllegalArgumentException].
 *
 * @sample androidx.savedstate.charSequenceArraySerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
public class CharSequenceArraySerializer : KSerializer<Array<CharSequence>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CharSequenceArray")

    override fun serialize(encoder: Encoder, @Suppress("ArrayReturn") value: Array<CharSequence>) {
        require(encoder is SavedStateEncoder) {
            encoderErrorMessage(descriptor.serialName, encoder)
        }
        encoder.run { savedState.write { putCharSequenceArray(key, value) } }
    }

    @Suppress("ArrayReturn")
    override fun deserialize(decoder: Decoder): Array<CharSequence> {
        require(decoder is SavedStateDecoder) {
            decoderErrorMessage(descriptor.serialName, decoder)
        }
        return decoder.run { savedState.read { getCharSequenceArray(key) } }
    }
}

/**
 * A serializer for [Array<Parcelable>]. This serializer uses [SavedState]'s API directly to
 * save/load a [Array<Parcelable>].
 *
 * Note that this serializer should be used with [SavedStateEncoder] or [SavedStateDecoder] only.
 * Using it with other Encoders/Decoders may throw [IllegalArgumentException].
 *
 * @sample androidx.savedstate.parcelableArraySerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
public class ParcelableArraySerializer : KSerializer<Array<Parcelable>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ParcelableArray")

    override fun serialize(encoder: Encoder, @Suppress("ArrayReturn") value: Array<Parcelable>) {
        require(encoder is SavedStateEncoder) {
            encoderErrorMessage(descriptor.serialName, encoder)
        }
        encoder.run { savedState.write { putParcelableArray(key, value) } }
    }

    @Suppress("ArrayReturn")
    override fun deserialize(decoder: Decoder): Array<Parcelable> {
        require(decoder is SavedStateDecoder) {
            decoderErrorMessage(descriptor.serialName, decoder)
        }
        return decoder.run { savedState.read { getParcelableArray(key) } }
    }
}

/**
 * A serializer for [Array<CharSequence>]. This serializer uses [SavedState]'s API directly to
 * save/load a [Array<CharSequence>].
 *
 * Note that this serializer should be used with [SavedStateEncoder] or [SavedStateDecoder] only.
 * Using it with other Encoders/Decoders may throw [IllegalArgumentException].
 *
 * @sample androidx.savedstate.charSequenceListSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
public class CharSequenceListSerializer : KSerializer<List<CharSequence>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CharSequenceList")

    override fun serialize(encoder: Encoder, value: List<CharSequence>) {
        require(encoder is SavedStateEncoder) {
            encoderErrorMessage(descriptor.serialName, encoder)
        }
        encoder.run { savedState.write { putCharSequenceList(key, value) } }
    }

    override fun deserialize(decoder: Decoder): List<CharSequence> {
        require(decoder is SavedStateDecoder) {
            decoderErrorMessage(descriptor.serialName, decoder)
        }
        return decoder.run { savedState.read { getCharSequenceList(key) } }
    }
}

/**
 * A serializer for [List<Parcelable>]. This serializer uses [SavedState]'s API directly to
 * save/load a [List<Parcelable>].
 *
 * Note that this serializer should be used with [SavedStateEncoder] or [SavedStateDecoder] only.
 * Using it with other Encoders/Decoders may throw [IllegalArgumentException].
 *
 * @sample androidx.savedstate.parcelableListSerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
public class ParcelableListSerializer : KSerializer<List<Parcelable>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ParcelableList")

    override fun serialize(encoder: Encoder, value: List<Parcelable>) {
        require(encoder is SavedStateEncoder) {
            encoderErrorMessage(descriptor.serialName, encoder)
        }
        encoder.run { savedState.write { putParcelableList(key, value) } }
    }

    override fun deserialize(decoder: Decoder): List<Parcelable> {
        require(decoder is SavedStateDecoder) {
            decoderErrorMessage(descriptor.serialName, decoder)
        }
        return decoder.run { savedState.read { getParcelableList(key) } }
    }
}

/**
 * A serializer for [SparseArray<Parcelable>]. This serializer uses [SavedState]'s API directly to
 * save/load a [SparseArray<Parcelable>].
 *
 * Note that this serializer should be used with [SavedStateEncoder] or [SavedStateDecoder] only.
 * Using it with other Encoders/Decoders may throw [IllegalArgumentException].
 *
 * @sample androidx.savedstate.sparseParcelableArraySerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
public class SparseParcelableArraySerializer : KSerializer<SparseArray<Parcelable>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SparseParcelableArray")

    override fun serialize(encoder: Encoder, value: SparseArray<Parcelable>) {
        require(encoder is SavedStateEncoder) {
            encoderErrorMessage(descriptor.serialName, encoder)
        }
        encoder.run { savedState.write { putSparseParcelableArray(key, value) } }
    }

    override fun deserialize(decoder: Decoder): SparseArray<Parcelable> {
        require(decoder is SavedStateDecoder) {
            decoderErrorMessage(descriptor.serialName, decoder)
        }
        return decoder.run { savedState.read { getSparseParcelableArray(key) } }
    }
}
