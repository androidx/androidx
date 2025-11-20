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
import kotlinx.serialization.Serializable
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
public object SizeSerializer : KSerializer<Size> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("android.util.Size")

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
public object SizeFSerializer : KSerializer<SizeF> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("android.util.SizeF")

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
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
internal object CharSequenceSerializer : KSerializer<CharSequence> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("kotlin.CharSequence")

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

internal object DefaultJavaSerializableSerializer : JavaSerializableSerializer<JavaSerializable>()

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
    final override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("java.io.Serializable")

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

internal object DefaultParcelableSerializer : ParcelableSerializer<Parcelable>()

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
    final override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("android.os.Parcelable")

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
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
internal object IBinderSerializer : KSerializer<IBinder> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("android.os.IBinder")

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
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
internal object CharSequenceArraySerializer : KSerializer<Array<CharSequence>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("kotlin.Array<kotlin.CharSequence>")

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
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
internal object ParcelableArraySerializer : KSerializer<Array<Parcelable>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("kotlin.Array<android.os.Parcelable>")

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
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
internal object CharSequenceListSerializer : KSerializer<List<CharSequence>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("kotlin.collections.List<kotlin.CharSequence>")

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
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
internal object ParcelableListSerializer : KSerializer<List<Parcelable>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("kotlin.collections.List<android.os.Parcelable>")

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
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
internal object SparseParcelableArraySerializer : KSerializer<SparseArray<Parcelable>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("android.util.SparseArray<android.os.Parcelable>")

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

/**
 * A serializer for [SparseArray].
 *
 * @sample androidx.savedstate.sparseArraySerializer
 * @see androidx.savedstate.serialization.encodeToSavedState
 * @see androidx.savedstate.serialization.decodeFromSavedState
 */
@OptIn(ExperimentalSerializationApi::class)
public class SparseArraySerializer<T>(elementSerializer: KSerializer<T>) :
    KSerializer<SparseArray<T>> {

    private val surrogateSerializer = SparseArraySurrogate.serializer(elementSerializer)

    // We can't use `SerialDescriptor("android.util.SparseArray", surrogateSerializer.descriptor)
    // as the `WrappedSerialDescriptor` returned doesn't have a proper `equals()` to trigger our
    // format-specific serialization:
    // https://github.com/Kotlin/kotlinx.serialization/issues/2941
    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: SparseArray<T>) {
        val surrogate =
            SparseArraySurrogate(
                keys = List(value.size()) { index -> value.keyAt(index) },
                values = List(value.size()) { index -> value.valueAt(index) },
            )
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): SparseArray<T> {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        require(surrogate.keys.size == surrogate.values.size)
        return SparseArray<T>(surrogate.keys.size).apply {
            for (index in surrogate.keys.indices) {
                append(surrogate.keys[index], surrogate.values[index])
            }
        }
    }

    @Serializable private class SparseArraySurrogate<T>(val keys: List<Int>, val values: List<T>)
}
