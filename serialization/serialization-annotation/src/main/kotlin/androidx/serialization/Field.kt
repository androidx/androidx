/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.serialization

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/**
 * Marks a property of a message class as a serializable field.
 *
 * Applying this annotation to the properties of a class marks it as a message class. A message
 * classes may be serialized to a proto-encoded byte array or a [android.os.Parcel] or used as
 * the request or response type of an [Action] on a service interface.
 *
 * ```kotlin
 * data class MyMessage(
 *     @Field(1) val textField: String,
 *     @Field(2, protoEncoding = ProtoEncoding.ZIG_ZAG_VARINT) negativeField: Int
 * )
 * ```
 *
 * ## Field Types
 *
 * Fields may be of a nullable or non-null scalar type including [Boolean], [Double], [Float],
 * [Int] or [UInt], [Long] or [ULong], [String], and bytes as [ByteArray], [UByteArray], or
 * [java.nio.ByteBuffer]. Complex types for fields include nullable or non-null enum classes with
 * [EnumValue] annotations and other message classes in the same compilation unit. Message
 * classes with only a parcel representation and no proto representation may also include
 * nullable service interfaces from the same package, nullable instances of active objects
 * including [android.os.IInterface], [android.os.IBinder], and [android.os.ParcelFileDescriptor].
 *
 * Fields can also be collections of supported non-null types either as arrays or collections.
 * Supported collection types include [Collection], [Iterable], [List], [Set], and
 * [java.util.SortedSet] as well as concrete implementations of [Collection] that have a default
 * constructor.
 *
 * Maps fields with non-null string or integral keys and non-null values of any supported type are
 * supported as well. Supported map types include [Map], [androidx.collection.SimpleArrayMap],
 * and the specialized `SparseArray` classes from [android.util] or [androidx.collection].
 * Concrete implementations of [Map] are also supported provided they have a default constructor.
 *
 * ## Default Values
 *
 * When deserializing an encoded message, fields missing from the encoded representation are set
 * to a default value. This property allows the serializer to reduce the size of encoded messages
 * by eliding scalar fields and nullable fields set to their default values entirely.
 *
 * For nullable fields, the default value is always `null`. The default for service fields and
 * active objects is always `null`, as these fields are required to be nullable.
 *
 * For non-null fields, the default varies based on the type of the field:
 *
 *   * Numeric fields default to zero
 *   * Boolean fields default to false
 *   * String and bytes fields default to an empty string, byte array or byte buffer
 *   * Enum fields default to the enum value marked with [EnumValue.DEFAULT]
 *   * Message fields default to an instance of the message class with all its fields set to
 *     their default values recursively
 *   * Array, collection, and map fields default to an empty container of the appropriate type
 *
 * If you need to know if a scalar type was present in an encoded message or simply set to the
 * default value, use the nullable version of the type.
 *
 * ## Message Classes
 *
 * Message classes be abstract and may extend other classes, including other message classes
 * within the same compilation unit. However messages themselves do not have a hierarchy, and
 * each concrete message class is flattened into one message in the resolved schema.
 *
 * Not every property of a message class needs to be serializable, but Serialization must be able
 * to instantiate a message class from the recognized fields parsed from an encoded message. This
 * means that a constructor of factory function must exist that only takes field parameters with
 * this annotation. Alternatively a builder class with this annotation on its setter methods may
 * be used for instantiation instead.
 *
 * This annotation can be used at multiple points for the same logical field, such as a parameter
 * to a factory function or a setter on a builder and an immutable property on the message class.
 * To avoid unexpected behavior, both copies of the annotation for a logical field must be
 * identical.
 *
 * @property id Integer ID of the field.
 *
 * Valid field IDs are positive integers between 1 and 268,435,456 (`0x0fff_ffff`) inclusive except
 * 19,000 through 19,999 inclusive, which are [reserved in proto][1]. Note that the upper limit is
 * one bit smaller than proto's upper limit to accommodate a 4 bits of field length in the parcel
 * encoding.
 *
 * Field IDs must be unique within a message, including any fields inherited from parent message
 * classes but may be reused between unrelated messages.
 *
 * To reserve field IDs for future use or to prevent unintentional reuse of removed field IDs,
 * apply the [Reserved] annotation to the message class.
 *
 * [1]: https://developers.google.com/protocol-buffers/docs/proto3#assigning-field-numbers
 *
 * @property protoEncoding The encoding for this field's proto representation.
 *
 * If this field is an array or supported collection type, this property sets the encoding of the
 * items in the collection. If the field is a supported map type, this property sets the encoding
 * of the values of the map.
 *
 * Only integral fields in the proto representation of a message have multiple encoding options.
 * Leave this set to [ProtoEncoding.DEFAULT] for non-integral fields.
 *
 * @property mapKeyProtoEncoding The proto encoding for this field's keys, if it's a map.
 *
 * This property is only applicable to fields of supported map types with integral keys. Leave
 * this set to [ProtoEncoding.DEFAULT] for non-map fields or map fields with string keys.
 */
@Retention(BINARY)
@Target(FIELD, FUNCTION, PROPERTY, VALUE_PARAMETER)
annotation class Field(
    @get:JvmName("value")
    val id: Int,
    val protoEncoding: ProtoEncoding = ProtoEncoding.DEFAULT,
    val mapKeyProtoEncoding: ProtoEncoding = ProtoEncoding.DEFAULT
)
