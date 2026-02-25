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

package androidx.appfunction.integration.test.sharedschema

import android.app.PendingIntent
import androidx.appfunctions.AppFunctionIntValueConstraint
import androidx.appfunctions.AppFunctionResourceContainer
import androidx.appfunctions.AppFunctionSchemaCapability
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionTextResource
import androidx.appfunctions.AppFunctionUriGrant
import java.time.LocalDateTime

// TODO: b/463375632 - We should write tests in Agent app to not use these since they're not part of
// schema.

/** Example parameterized AppFunctionSerializable. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SetField<T>(
    /** Value property of SetField. */
    val value: T
)

@AppFunctionSerializable
data class UpdateNoteParams(
    val title: SetField<String>? = null,
    val nullableTitle: SetField<String?>? = null,
    val content: SetField<List<String>>? = null,
    val nullableContent: SetField<List<String>?>? = null,
    val attachments: SetField<List<Attachment>>? = null,
    val modifiedTime: SetField<LocalDateTime>? = null,
)

@AppFunctionSerializable data class Attachment(val uri: String, val nested: Attachment? = null)

@AppFunctionSerializable data class Owner(val name: String)

@AppFunctionSerializable
data class CreateNoteParams(
    val title: String,
    val content: List<String>,
    val owner: Owner,
    val attachments: List<Attachment>,
    val folderId: String?,
)

/** Represents a note in the notes app. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class Note(
    /** The note's title. */
    val title: String,
    /** The note's content. */
    val content: List<String>,
    /** The note's [Owner]. */
    val owner: Owner,
    /** The note's attachments. */
    val attachments: List<Attachment>,
    /** The note's last modified time. */
    val modifiedTime: LocalDateTime? = null,
)

@AppFunctionSerializable
data class OpenableNote(
    val title: String,
    val content: List<String>,
    val owner: Owner,
    val attachments: List<Attachment>,
    val modifiedTime: LocalDateTime? = null,
    override val intentToOpen: PendingIntent,
) : AppFunctionOpenable

@AppFunctionSchemaCapability
interface AppFunctionOpenable {
    val intentToOpen: PendingIntent
}

@AppFunctionSerializable data class DateTime(val localDateTime: LocalDateTime)

@AppFunctionSerializable
data class ClassWithOptionalValues(
    // Int
    val optionalNonNullInt: Int = 1,
    val optionalNullableInt: Int? = 1,

    // Long
    val optionalNonNullLong: Long = 2L,
    val optionalNullableLong: Long? = 2L,

    // Boolean
    val optionalNonNullBoolean: Boolean = false,
    val optionalNullableBoolean: Boolean? = false,

    // Float
    val optionalNonNullFloat: Float = 3.0f,
    val optionalNullableFloat: Float? = 3.0f,

    // Double
    val optionalNonNullDouble: Double = 4.0,
    val optionalNullableDouble: Double? = 4.0,

    // String
    val optionalNullableString: String? = "Default String",

    // Serializable
    val optionalNullableSerializable: Owner? = Owner("Default Owner"),

    // Proxy
    val optionalNullableProxySerializable: LocalDateTime? = LocalDateTime.now(),

    // IntArray
    val optionalNonNullIntArray: IntArray = intArrayOf(1),
    val optionalNullableIntArray: IntArray? = intArrayOf(1),

    // LongArray
    val optionalNonNullLongArray: LongArray = longArrayOf(2L),
    val optionalNullableLongArray: LongArray? = longArrayOf(2L),

    // BooleanArray
    val optionalNonNullBooleanArray: BooleanArray = booleanArrayOf(false),
    val optionalNullableBooleanArray: BooleanArray? = booleanArrayOf(false),

    // FloatArray
    val optionalNonNullFloatArray: FloatArray = floatArrayOf(3.0f),
    val optionalNullableFloatArray: FloatArray? = floatArrayOf(3.0f),

    // DoubleArray
    val optionalNonNullDoubleArray: DoubleArray = doubleArrayOf(4.0),
    val optionalNullableDoubleArray: DoubleArray? = doubleArrayOf(4.0),

    // ByteArray
    val optionalNonNullByteArray: ByteArray = byteArrayOf(5),
    val optionalNullableByteArray: ByteArray? = byteArrayOf(5),

    // List<String>
    val optionalNonNullListString: List<String> = listOf("Default String"),
    val optionalNullableListString: List<String>? = listOf("Default String"),

    // List of Serializable
    val optionalNonNullSerializableList: List<Owner> = listOf(Owner("Default Owner")),
    val optionalNullableSerializableList: List<Owner>? = listOf(Owner("Default Owner")),

    // List of Proxy
    val optionalNonNullProxySerializableList: List<LocalDateTime> = listOf(LocalDateTime.now()),
    val optionalNullableProxySerializableList: List<LocalDateTime>? = listOf(LocalDateTime.now()),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClassWithOptionalValues

        if (optionalNonNullInt != other.optionalNonNullInt) return false
        if (optionalNullableInt != other.optionalNullableInt) return false
        if (optionalNonNullLong != other.optionalNonNullLong) return false
        if (optionalNullableLong != other.optionalNullableLong) return false
        if (optionalNonNullBoolean != other.optionalNonNullBoolean) return false
        if (optionalNullableBoolean != other.optionalNullableBoolean) return false
        if (optionalNonNullFloat != other.optionalNonNullFloat) return false
        if (optionalNullableFloat != other.optionalNullableFloat) return false
        if (optionalNonNullDouble != other.optionalNonNullDouble) return false
        if (optionalNullableDouble != other.optionalNullableDouble) return false
        if (optionalNullableString != other.optionalNullableString) return false
        if (optionalNullableSerializable != other.optionalNullableSerializable) return false
        if (optionalNullableProxySerializable != other.optionalNullableProxySerializable)
            return false
        if (!optionalNonNullIntArray.contentEquals(other.optionalNonNullIntArray)) return false
        if (!optionalNullableIntArray.contentEquals(other.optionalNullableIntArray)) return false
        if (!optionalNonNullLongArray.contentEquals(other.optionalNonNullLongArray)) return false
        if (!optionalNullableLongArray.contentEquals(other.optionalNullableLongArray)) return false
        if (!optionalNonNullBooleanArray.contentEquals(other.optionalNonNullBooleanArray))
            return false
        if (!optionalNullableBooleanArray.contentEquals(other.optionalNullableBooleanArray))
            return false
        if (!optionalNonNullFloatArray.contentEquals(other.optionalNonNullFloatArray)) return false
        if (!optionalNullableFloatArray.contentEquals(other.optionalNullableFloatArray))
            return false
        if (!optionalNonNullDoubleArray.contentEquals(other.optionalNonNullDoubleArray))
            return false
        if (!optionalNullableDoubleArray.contentEquals(other.optionalNullableDoubleArray))
            return false
        if (!optionalNonNullByteArray.contentEquals(other.optionalNonNullByteArray)) return false
        if (!optionalNullableByteArray.contentEquals(other.optionalNullableByteArray)) return false
        if (optionalNonNullListString != other.optionalNonNullListString) return false
        if (optionalNullableListString != other.optionalNullableListString) return false
        if (optionalNonNullSerializableList != other.optionalNonNullSerializableList) return false
        if (optionalNullableSerializableList != other.optionalNullableSerializableList) return false
        if (optionalNonNullProxySerializableList != other.optionalNonNullProxySerializableList)
            return false
        if (optionalNullableProxySerializableList != other.optionalNullableProxySerializableList)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = optionalNonNullInt
        result = 31 * result + (optionalNullableInt ?: 0)
        result = 31 * result + optionalNonNullLong.hashCode()
        result = 31 * result + (optionalNullableLong?.hashCode() ?: 0)
        result = 31 * result + optionalNonNullBoolean.hashCode()
        result = 31 * result + (optionalNullableBoolean?.hashCode() ?: 0)
        result = 31 * result + optionalNonNullFloat.hashCode()
        result = 31 * result + (optionalNullableFloat?.hashCode() ?: 0)
        result = 31 * result + optionalNonNullDouble.hashCode()
        result = 31 * result + (optionalNullableDouble?.hashCode() ?: 0)
        result = 31 * result + (optionalNullableString?.hashCode() ?: 0)
        result = 31 * result + (optionalNullableSerializable?.hashCode() ?: 0)
        result = 31 * result + (optionalNullableProxySerializable?.hashCode() ?: 0)
        result = 31 * result + optionalNonNullIntArray.contentHashCode()
        result = 31 * result + (optionalNullableIntArray?.contentHashCode() ?: 0)
        result = 31 * result + optionalNonNullLongArray.contentHashCode()
        result = 31 * result + (optionalNullableLongArray?.contentHashCode() ?: 0)
        result = 31 * result + optionalNonNullBooleanArray.contentHashCode()
        result = 31 * result + (optionalNullableBooleanArray?.contentHashCode() ?: 0)
        result = 31 * result + optionalNonNullFloatArray.contentHashCode()
        result = 31 * result + (optionalNullableFloatArray?.contentHashCode() ?: 0)
        result = 31 * result + optionalNonNullDoubleArray.contentHashCode()
        result = 31 * result + (optionalNullableDoubleArray?.contentHashCode() ?: 0)
        result = 31 * result + optionalNonNullByteArray.contentHashCode()
        result = 31 * result + (optionalNullableByteArray?.contentHashCode() ?: 0)
        result = 31 * result + optionalNonNullListString.hashCode()
        result = 31 * result + (optionalNullableListString?.hashCode() ?: 0)
        result = 31 * result + optionalNonNullSerializableList.hashCode()
        result = 31 * result + (optionalNullableSerializableList?.hashCode() ?: 0)
        result = 31 * result + optionalNonNullProxySerializableList.hashCode()
        result = 31 * result + (optionalNullableProxySerializableList?.hashCode() ?: 0)
        return result
    }
}

@AppFunctionSerializable
data class FilesData(
    val readOnlyUri: AppFunctionUriGrant,
    val writeOnlyUri: AppFunctionUriGrant,
    val readWriteUri: AppFunctionUriGrant,
    val persistReadWriteUri: AppFunctionUriGrant,
)

@AppFunctionSerializable
sealed interface OneOfSealedInterface {
    val interfaceProperty: String
}

@AppFunctionSerializable
data class ASubclass(override val interfaceProperty: String, val str: String) : OneOfSealedInterface

@AppFunctionSerializable
data class BSubclass(
    override val interfaceProperty: String,
    val integer: Int,
    override val resources: List<AppFunctionTextResource>,
) : OneOfSealedInterface, AppFunctionResourceContainer

@AppFunctionSerializable
data class OneOfSealedNestedSerializable(val sealedInterface: OneOfSealedInterface)

@AppFunctionSerializable
data class IntEnumSerializable(
    @property:AppFunctionIntValueConstraint(enumValues = [10, 20]) val value: Int
)

@AppFunctionSerializable
data class ResourceFunctionResponse(
    val stringValue: String,
    override val resources: List<AppFunctionTextResource>,
) : AppFunctionResourceContainer
