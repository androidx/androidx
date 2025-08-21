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
import kotlinx.serialization.SerializationStrategy

@Suppress("UNCHECKED_CAST")
internal actual fun <T> SavedStateEncoder.encodeFormatSpecificTypesOnPlatform(
    strategy: SerializationStrategy<T>,
    value: T,
): Boolean {
    when (strategy.descriptor) {
        polymorphicCharSequenceDescriptor ->
            CharSequenceSerializer.serialize(this, value as CharSequence)
        polymorphicParcelableDescriptor ->
            DefaultParcelableSerializer.serialize(this, value as Parcelable)
        polymorphicJavaSerializableDescriptor ->
            DefaultJavaSerializableSerializer.serialize(this, value as JavaSerializable)
        polymorphicIBinderDescriptor -> IBinderSerializer.serialize(this, value as IBinder)
        charSequenceArrayDescriptor,
        polymorphicCharSequenceArrayDescriptor ->
            CharSequenceArraySerializer.serialize(this, value as Array<CharSequence>)
        charSequenceListDescriptor,
        polymorphicCharSequenceListDescriptor ->
            CharSequenceListSerializer.serialize(this, value as List<CharSequence>)
        parcelableArrayDescriptor,
        polymorphicParcelableArrayDescriptor ->
            ParcelableArraySerializer.serialize(this, value as Array<Parcelable>)
        parcelableListDescriptor,
        polymorphicParcelableListDescriptor ->
            ParcelableListSerializer.serialize(this, value as List<Parcelable>)
        sparseParcelableArrayDescriptor,
        polymorphicSparseParcelableArrayDescriptor,
        nullablePolymorphicSparseParcelableArrayDescriptor ->
            SparseParcelableArraySerializer.serialize(
                this,
                value as android.util.SparseArray<Parcelable>,
            )
        else -> return false
    }
    return true
}
