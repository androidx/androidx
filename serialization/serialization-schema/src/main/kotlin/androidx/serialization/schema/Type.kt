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

package androidx.serialization.schema

import androidx.serialization.ProtoEncoding
import androidx.serialization.ProtoEncoding.SIGNED_FIXED
import androidx.serialization.ProtoEncoding.SIGNED_VARINT
import androidx.serialization.ProtoEncoding.UNSIGNED_FIXED
import androidx.serialization.ProtoEncoding.UNSIGNED_VARINT
import androidx.serialization.ProtoEncoding.ZIG_ZAG_VARINT

/**
 * Root of the serialization type hierarchy.
 */
sealed class Type

// Declared Types ////////////////////////////////////////////////////////////////////////////////

/**
 * A user-declared message, enum, or service.
 *
 * @property name The qualified name of the type.
 * @property members Members of the type, such as message fields, enum values, or service actions.
 * @property reserved Reserved IDs and names within this type
 */
sealed class DeclaredType(
    val name: TypeName,
    val members: List<Member>,
    val reserved: Reserved
) : Type() {
    /**
     * A member of a user-declared type, such as a message field, enum value, or service action.
     */
    abstract class Member(
        val id: Int,
        val name: String
    )
}

/**
 * A user-declared message.
 *
 * Clients of this package may wish to subclass [Message] and [Field] to add additional context
 * or data such as source location.
 *
 * @property fields Fields of the message.
 *
 * Consider overriding this property in a subclass if you subclass [Field].
 */
open class Message(
    name: TypeName,
    open val fields: List<Field>,
    reserved: Reserved = Reserved.empty()
) : DeclaredType(name, fields, reserved) {
    /**
     * A field of a user-declared message.
     *
     * @property type The type of the field, may be any type starting from root.
     * @see androidx.serialization.Field
     */
    open class Field(
        id: Int,
        name: String,
        val type: Type
    ) : Member(id, name)
}

/**
 * A user-declared serializable enum.
 *
 * Clients of this package may wish to subclass [Enum] and [Value] to add additional context
 * or data such as source location.
 *
 * @property values Values of the enum.
 *
 * Consider overriding this property in a subclass if you subclass [Value].
 */
open class Enum(
    name: TypeName,
    open val values: List<Value>,
    reserved: Reserved = Reserved.empty()
) : DeclaredType(name, values, reserved) {
    /**
     * A value of a user-declared enum.
     *
     * @see androidx.serialization.EnumValue
     */
    open class Value(
        id: Int,
        name: String
    ) : Member(id, name)
}

/**
 * A user-declared service interface.
 *
 * Clients of this package may wish to subclass [Service] and [Action] to add additional context
 * or data such as source location.
 *
 * @property actions Actions of the service.
 *
 * Consider overriding this property in a subclass if you subclass [Action].
 */
open class Service(
    name: TypeName,
    open val actions: List<Action>,
    reserved: Reserved = Reserved.empty()
) : DeclaredType(name, actions, reserved) {
    /**
     * An action of a user-declared service interface.
     *
     * Consider overriding [request] and [response] if you subclass [Message].
     *
     * @property request The request message type, or null for a nullary action method.
     * @property response The response message type, or null for a void action method.
     * @property mode The synchronization mode of the action.
     * @see androidx.serialization.Action
     */
    open class Action(
        id: Int,
        name: String,
        open val request: Message? = null,
        open val response: Message? = null,
        val mode: Mode = Mode.BLOCKING
    ) : Member(id, name) {
        /**
         * The synchronization mode of an action.
         */
        enum class Mode {
            /**
             * Action method blocks until transaction completes on remote process.
             */
            BLOCKING,

            /**
             * Action method returns immediately and does not return a response.
             *
             * @see android.os.IBinder.FLAG_ONEWAY
             */
            ONE_WAY
        }
    }
}

// Collections ///////////////////////////////////////////////////////////////////////////////////

/**
 * A linear collection of values, such as a [Collection] or an [Array].
 */
open class CollectionType(val type: Type) : Type()

/**
 * An associative collection of keys and values, such as a [Map] or a [android.util.SparseArray].
 */
open class MapType(val keyType: Type, val valueType: Type) : Type()

// Scalars ///////////////////////////////////////////////////////////////////////////////////////

/**
 * A scalar value.
 *
 * Scalar fields have a default value of zero, empty, or false.
 *
 * @property name Scalar name, as used in proto.
 * @property protoEncoding Proto encoding of integral scalar values.
 */
sealed class Scalar(
    val name: String,
    val protoEncoding: ProtoEncoding = ProtoEncoding.DEFAULT
) : Type()

/**
 * Boolean scalar.
 */
object BoolScalar : Scalar("bool")

/**
 * Byte array scalar, such as [ByteArray] or [java.nio.ByteBuffer].
 */
object BytesScalar : Scalar("bytes")

/**
 * A 64-bit floating point scalar.
 */
object DoubleScalar : Scalar("double")

/**
 * A 32-bit floating point scalar.
 */
object FloatScalar : Scalar("float")

/**
 * String scalar.
 */
object StringScalar : Scalar("string")

/**
 * A 32-bit integer scalar type.
 */
sealed class IntScalar(name: String, protoEncoding: ProtoEncoding) : Scalar(name, protoEncoding)

/**
 * A 32-bit signed integer scalar using the [SIGNED_VARINT] encoding.
 */
object Int32Scalar : IntScalar("int32", SIGNED_VARINT)

/**
 * A 32-bit signed integer scalar using the [ZIG_ZAG_VARINT] encoding.
 */
object SInt32Scalar : IntScalar("sint32", ZIG_ZAG_VARINT)

/**
 * A 32-bit unsigned integer scalar using the [UNSIGNED_VARINT] encoding.
 */
object UInt32Scalar : IntScalar("uint32", UNSIGNED_VARINT)

/**
 * A 32-bit unsigned fixed width integer scalar.
 */
object Fixed32Scalar : IntScalar("fixed32", UNSIGNED_FIXED)

/**
 * A 32-bit signed fixed width integer scalar.
 */
object SFixed32Scalar : IntScalar("sfixed32", SIGNED_FIXED)

/**
 * A 64-bit integer scalar type.
 */
sealed class LongScalar(name: String, protoEncoding: ProtoEncoding) : Scalar(name, protoEncoding)

/**
 * A 64-bit signed integer scalar using the [SIGNED_VARINT] encoding.
 */
object Int64Scalar : LongScalar("int64", SIGNED_VARINT)

/**
 * A 64-bit signed integer scalar using the [ZIG_ZAG_VARINT] encoding.
 */
object SInt64Scalar : LongScalar("sint64", ZIG_ZAG_VARINT)

/**
 * A 64-bit unsigned integer scalar using the [UNSIGNED_VARINT] encoding.
 */
object UInt64Scalar : LongScalar("uint64", UNSIGNED_VARINT)

/**
 * A 64-bit unsigned fixed width integer scalar.
 */
object Fixed64Scalar : LongScalar("fixed64", UNSIGNED_FIXED)

/**
 * A 64-bit signed fixed width integer scalar.
 */
object SFixed64Scalar : LongScalar("sfixed64", SIGNED_FIXED)

// Wrapper Messages //////////////////////////////////////////////////////////////////////////////

/**
 * A message type that wraps a scalar value.
 *
 * This provides support for nullable field types. The message types are derived from proto's
 * [well known types][1].
 *
 * [1]: https://developers.google.com/protocol-buffers/docs/reference/google.protobuf
 *
 * @property scalar The wrapped scalar type.
 */
sealed class Wrapper(simpleName: String, val scalar: Scalar) : Type() {
    /**
     * The name of the type in the `google.protobuf` package.
     */
    val name = TypeName("google.protobuf", simpleName)
}

/**
 * Boolean wrapper message.
 */
object BoolWrapper : Wrapper("BoolWrapper", BoolScalar)

/**
 * Bytes wrapper message, such as a nullable [ByteArray] or [java.nio.ByteBuffer].
 */
object BytesWrapper : Wrapper("BytesValue", BytesScalar)

/**
 * A 64-bit floating point wrapper message.
 */
object DoubleWrapper : Wrapper("DoubleValue", DoubleScalar)

/**
 * A 32-bit floating point wrapper message.
 */
object FloatWrapper : Wrapper("FloatValue", FloatScalar)

/**
 * String wrapper message.
 */
object StringWrapper : Wrapper("StringValue", StringScalar)

/**
 * A 32-bit integer wrapper message type.
 */
sealed class IntWrapper(name: String, scalar: IntScalar) : Wrapper(name, scalar)

/**
 * A signed 32-bit integer wrapper message using the [SIGNED_VARINT] encoding.
 */
object Int32Wrapper : IntWrapper("Int32Value", Int32Scalar)

/**
 * An unsigned 32-bit integer wrapper message using the [UNSIGNED_VARINT] encoding.
 */
object UInt32Wrapper : IntWrapper("UInt32Value", UInt32Scalar)

/**
 * A 64-bit integer wrapper message type.
 */
sealed class LongWrapper(name: String, scalar: LongScalar) : Wrapper(name, scalar)

/**
 * A signed 64-bit integer wrapper message using the [SIGNED_VARINT] encoding.
 */
object Int64Wrapper : LongWrapper("Int64Value", Int64Scalar)

/**
 * An unsigned 64-bit integer wrapper message using the [UNSIGNED_VARINT] encoding.
 */
object UInt64Wrapper : LongWrapper("UInt64Value", UInt64Scalar)
