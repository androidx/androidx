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

import android.os.IBinder
import android.os.Parcelable
import androidx.savedstate.serialization.serializers.CharSequenceArraySerializer
import androidx.savedstate.serialization.serializers.CharSequenceListSerializer
import androidx.savedstate.serialization.serializers.CharSequenceSerializer
import androidx.savedstate.serialization.serializers.DefaultJavaSerializableSerializer
import androidx.savedstate.serialization.serializers.DefaultParcelableSerializer
import androidx.savedstate.serialization.serializers.IBinderSerializer
import androidx.savedstate.serialization.serializers.ParcelableArraySerializer
import androidx.savedstate.serialization.serializers.ParcelableListSerializer
import androidx.savedstate.serialization.serializers.SparseParcelableArraySerializer
import java.io.Serializable as JavaSerializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy

@OptIn(ExperimentalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
internal actual fun <T> SavedStateEncoder.encodeFormatSpecificTypesOnPlatform(
    strategy: SerializationStrategy<T>,
    value: T,
): Boolean {
    val descriptor = strategy.descriptor
    // Check the serial name first.
    // This routes execution quickly. It prevents slow structural equality checks
    // for basic types. This improves save performance.
    when (descriptor.serialName) {
        ARRAY_LIST_NAME ->
            when (descriptor) {
                charSequenceListDescriptor,
                polymorphicCharSequenceListDescriptor,
                nullablePolymorphicCharSequenceListDescriptor -> {
                    CharSequenceListSerializer.serialize(this, value as List<CharSequence>)
                    return true
                }
                parcelableListDescriptor,
                polymorphicParcelableListDescriptor,
                nullablePolymorphicParcelableListDescriptor -> {
                    ParcelableListSerializer.serialize(this, value as List<Parcelable>)
                    return true
                }
            }

        ARRAY_NAME ->
            when (descriptor) {
                charSequenceArrayDescriptor,
                polymorphicCharSequenceArrayDescriptor,
                nullablePolymorphicCharSequenceArrayDescriptor -> {
                    CharSequenceArraySerializer.serialize(this, value as Array<CharSequence>)
                    return true
                }
                parcelableArrayDescriptor,
                polymorphicParcelableArrayDescriptor,
                nullablePolymorphicParcelableArrayDescriptor -> {
                    ParcelableArraySerializer.serialize(this, value as Array<Parcelable>)
                    return true
                }
            }

        SPARSE_ARRAY_NAME ->
            when (descriptor) {
                sparseParcelableArrayDescriptor,
                polymorphicSparseParcelableArrayDescriptor,
                nullablePolymorphicSparseParcelableArrayDescriptor -> {
                    SparseParcelableArraySerializer.serialize(
                        this,
                        value as android.util.SparseArray<Parcelable>,
                    )
                    return true
                }
            }

        POLYMORPHIC_CHAR_SEQUENCE_NAME -> {
            CharSequenceSerializer.serialize(this, value as CharSequence)
            return true
        }

        POLYMORPHIC_PARCELABLE_NAME -> {
            DefaultParcelableSerializer.serialize(this, value as Parcelable)
            return true
        }

        POLYMORPHIC_JAVA_SERIALIZABLE_NAME -> {
            DefaultJavaSerializableSerializer.serialize(this, value as JavaSerializable)
            return true
        }

        POLYMORPHIC_IBINDER_NAME -> {
            IBinderSerializer.serialize(this, value as IBinder)
            return true
        }
    }
    return false
}
