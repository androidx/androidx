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

import androidx.savedstate.serialization.serializers.CharSequenceArraySerializer
import androidx.savedstate.serialization.serializers.CharSequenceListSerializer
import androidx.savedstate.serialization.serializers.CharSequenceSerializer
import androidx.savedstate.serialization.serializers.DefaultJavaSerializableSerializer
import androidx.savedstate.serialization.serializers.DefaultParcelableSerializer
import androidx.savedstate.serialization.serializers.IBinderSerializer
import androidx.savedstate.serialization.serializers.ParcelableArraySerializer
import androidx.savedstate.serialization.serializers.ParcelableListSerializer
import androidx.savedstate.serialization.serializers.SparseParcelableArraySerializer
import java.util.Arrays
import kotlin.reflect.KClass
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
@Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
internal actual fun <T> SavedStateDecoder.decodeFormatSpecificTypesOnPlatform(
    strategy: DeserializationStrategy<T>
): T? {
    val descriptor = strategy.descriptor
    // Check the serial name first. This routes execution quickly. It prevents slow structural
    // equality checks for basic types. This improves performance.
    return when (descriptor.serialName) {
        ARRAY_LIST_NAME ->
            when (descriptor) {
                charSequenceListDescriptor,
                polymorphicCharSequenceListDescriptor,
                nullablePolymorphicCharSequenceListDescriptor ->
                    CharSequenceListSerializer.deserialize(this)
                parcelableListDescriptor,
                polymorphicParcelableListDescriptor,
                nullablePolymorphicParcelableListDescriptor ->
                    ParcelableListSerializer.deserialize(this)
                else -> null
            }

        ARRAY_NAME ->
            when (descriptor) {
                charSequenceArrayDescriptor,
                polymorphicCharSequenceArrayDescriptor,
                nullablePolymorphicCharSequenceArrayDescriptor ->
                    CharSequenceArraySerializer.deserialize(this)
                parcelableArrayDescriptor -> {
                    val parcelableArr = ParcelableArraySerializer.deserialize(this)
                    val arrayKClass = getArrayKClass(strategy)
                    Arrays.copyOf(parcelableArr, parcelableArr.size, arrayKClass.java)
                }
                polymorphicParcelableArrayDescriptor,
                nullablePolymorphicParcelableArrayDescriptor ->
                    ParcelableArraySerializer.deserialize(this)
                else -> null
            }

        SPARSE_ARRAY_NAME ->
            when (descriptor) {
                sparseParcelableArrayDescriptor,
                polymorphicSparseParcelableArrayDescriptor,
                nullablePolymorphicSparseParcelableArrayDescriptor ->
                    SparseParcelableArraySerializer.deserialize(this)
                else -> null
            }

        POLYMORPHIC_CHAR_SEQUENCE_NAME -> CharSequenceSerializer.deserialize(this)
        POLYMORPHIC_PARCELABLE_NAME -> DefaultParcelableSerializer.deserialize(this)
        POLYMORPHIC_JAVA_SERIALIZABLE_NAME -> DefaultJavaSerializableSerializer.deserialize(this)
        POLYMORPHIC_IBINDER_NAME -> IBinderSerializer.deserialize(this)
        else -> null
    }
        as T?
}

// Get the array class with the element class captured by a
// `kotlinx.serialization.internal.ReferenceArraySerializer`, e.g. it returns
// `Array<MyParcelable>::class` if the captured class is `MyParcelable::class`.
private fun getArrayKClass(
    referenceArraySerializer: DeserializationStrategy<*>
): KClass<Array<out Any?>> {
    @Suppress("UNCHECKED_CAST")
    return referenceArraySerializer.deserialize(EmptyArrayDecoder)!!::class
        as KClass<Array<out Any?>>
}

// Used with `kotlinx.serialization.internal.ReferenceArraySerializer.deserialize()` to create an
// empty array.
@OptIn(ExperimentalSerializationApi::class)
private object EmptyArrayDecoder : AbstractDecoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return CompositeDecoder.DECODE_DONE
    }
}
