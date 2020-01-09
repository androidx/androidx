/*
 * Copyright 2020 The Android Open Source Project
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

/** A complex type from the `google.protobuf` package. */
sealed class WellKnownType : ComplexType {
    /** The path to import this type in a proto file. */
    abstract val importPath: String

    override val typeKind: Type.Kind
        get() = Type.Kind.WELL_KNOWN

    override val reserved: Reserved
        get() = Reserved.empty()
}

/**
 * A message that wraps a single scalar, used for nullable fields.
 *
 * @property scalar The scalar type this message wraps.
 */
sealed class Wrapper(val scalar: Scalar, simpleName: String) : WellKnownType(), Message {
    override val name = TypeName("google.protobuf", simpleName)
    override val importPath = "google/protobuf/wrappers.proto"

    override val fields = listOf<Message.Field>(
        object : Message.Field {
            override val id = 1
            override val name = "value"
            override val type = scalar
        }
    )
}

/** A boolean wrapper message. */
object BoolValue : Wrapper(Scalar.BOOL, "BoolValue")

/** A byte array wrapper message. */
object BytesValue : Wrapper(Scalar.BYTES, "BytesValue")

/** A 32-bit floating point wrapper message. */
object FloatValue : Wrapper(Scalar.FLOAT, "FloatValue")

/** A 64-bit floating point wrapper message. */
object DoubleValue : Wrapper(Scalar.DOUBLE, "DoubleValue")

/** A 32-bit signed integer wrapper message. */
object Int32Value : Wrapper(Scalar.INT32, "Int32Value")

/** A 32-bit unsigned integer wrapper message. */
object UInt32Value : Wrapper(Scalar.UINT32, "UInt32Value")

/** A 64-bit signed integer wrapper message. */
object Int64Value : Wrapper(Scalar.INT64, "Int64Value")

/** A 64-bit unsigned integer wrapper message. */
object UInt64Value : Wrapper(Scalar.UINT64, "UInt64Value")

/** A Unicode string wrapper message. */
object StringValue : Wrapper(Scalar.STRING, "StringValue")

/** A duration of time, usually represented by [java.time.Duration]. */
object Duration : WellKnownType(), Message {
    override val name = TypeName("google.protobuf", "Duration")
    override val importPath = "google/protobuf/duration.proto"

    override val fields = listOf(
        object : Message.Field {
            override val type = Scalar.INT64
            override val id = 1
            override val name = "seconds"
        },
        object : Message.Field {
            override val type = Scalar.INT32
            override val id = 2
            override val name = "nanos"
        }
    )
}

/** A moment in time, usually represented as an [java.time.Instant]. */
object Timestamp : WellKnownType(), Message {
    override val name = TypeName("google.protobuf", "Timestamp")
    override val importPath = "google/protobuf/timestamp.proto"

    override val fields = listOf(
        object : Message.Field {
            override val type = Scalar.INT64
            override val id = 1
            override val name = "seconds"
        },
        object : Message.Field {
            override val type = Scalar.INT32
            override val id = 2
            override val name = "nanos"
        }
    )
}
