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

@file:Suppress("UNUSED_VARIABLE")

package androidx.savedstate

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Sampled
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import androidx.savedstate.serialization.serializers.JavaSerializableSerializer
import androidx.savedstate.serialization.serializers.ParcelableSerializer
import androidx.savedstate.serialization.serializers.SavedStateSerializer
import androidx.savedstate.serialization.serializers.SizeFSerializer
import androidx.savedstate.serialization.serializers.SizeSerializer
import androidx.savedstate.serialization.serializers.SparseArraySerializer
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Sampled
fun encode() {
    @Serializable data class User(val id: Int, val name: String)
    val user = User(123, "foo")
    val savedState = encodeToSavedState(user)
}

@Sampled
fun encodeWithExplicitSerializer() {
    class UUIDSerializer : KSerializer<UUID> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("UUIDSerializer", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): UUID {
            return UUID.fromString(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, value: UUID) {
            encoder.encodeString(value.toString())
        }
    }
    encodeToSavedState(UUIDSerializer(), UUID.randomUUID())
}

@Sampled
fun encodeWithExplicitSerializerAndConfig() {
    val config = SavedStateConfiguration {
        serializersModule = SerializersModule {
            polymorphic(Any::class) { subclass(String::class) }
        }
    }
    val value = "foo"
    val encoded =
        encodeToSavedState(
            serializer = PolymorphicSerializer(Any::class),
            value = value,
            configuration = config,
        )
}

val userSavedState = savedState {
    putInt("id", 123)
    putString("name", "foo")
}

val uuidSavedState = savedState { putString("", UUID.randomUUID().toString()) }

@Sampled
fun decode() {
    @Serializable data class User(val id: Int, val name: String)
    val user = decodeFromSavedState<User>(userSavedState)
}

@Sampled
fun decodeWithExplicitSerializer() {
    class UUIDSerializer : KSerializer<UUID> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("UUIDSerializer", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): UUID {
            return UUID.fromString(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, value: UUID) {
            encoder.encodeString(value.toString())
        }
    }
    val uuid = decodeFromSavedState(UUIDSerializer(), uuidSavedState)
}

@Sampled
fun decodeWithExplicitSerializerAndConfig() {
    val config = SavedStateConfiguration {
        serializersModule = SerializersModule {
            polymorphic(Any::class) { subclass(String::class) }
        }
    }
    val value = "foo"
    val encoded =
        encodeToSavedState(
            serializer = PolymorphicSerializer(Any::class),
            value = value,
            configuration = config,
        )
    val decoded =
        decodeFromSavedState(
            deserializer = PolymorphicSerializer(Any::class),
            savedState = encoded,
            configuration = config,
        )
}

@Sampled
fun savedStateSerializer() {
    @Serializable
    data class MyModel(
        @Serializable(with = SavedStateSerializer::class) val savedState: SavedState
    )
}

@Sampled
fun sizeSerializer() {
    @Serializable
    data class MyModel(@Serializable(with = SizeSerializer::class) val size: android.util.Size)
}

@Sampled
fun sizeFSerializer() {
    @Serializable
    data class MyModel(@Serializable(with = SizeFSerializer::class) val sizeF: android.util.SizeF)
}

private class MyJavaSerializable : java.io.Serializable

private class MyJavaSerializableSerializer : JavaSerializableSerializer<MyJavaSerializable>()

@Sampled
fun serializableSerializer() {
    @Serializable
    data class MyModel(
        @Serializable(with = MyJavaSerializableSerializer::class)
        val serializable: MyJavaSerializable
    )
}

// The class is only used to demonstrate that Parcelables can be used with SavedState Serialization.
@Suppress("BanParcelableUsage", "ParcelCreator")
private class MyParcelable : Parcelable {
    override fun describeContents(): Int {
        error("Should not be called")
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        error("Should not be called")
    }
}

private class MyParcelableSerializer : ParcelableSerializer<MyParcelable>()

@Sampled
fun parcelableSerializer() {
    @Serializable
    data class MyModel(
        @Serializable(with = MyParcelableSerializer::class) val parcelable: MyParcelable
    )
}

@Sampled
fun sparseArraySerializer() {
    @Serializable
    class MyModel(
        @Serializable(with = SparseArraySerializer::class)
        val sparseParcelableArray: android.util.SparseArray<android.os.Parcelable>
    )
}

@Sampled
fun config() {
    val config = SavedStateConfiguration {
        serializersModule = SerializersModule {
            polymorphic(Any::class) { subclass(String::class) }
        }
    }
    val value = "foo"
    val encoded =
        encodeToSavedState(
            serializer = PolymorphicSerializer(Any::class),
            value = value,
            configuration = config,
        )
    val decoded =
        decodeFromSavedState(
            deserializer = PolymorphicSerializer(Any::class),
            savedState = encoded,
            configuration = config,
        )
}
