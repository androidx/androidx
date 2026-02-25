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

package androidx.appfunctions.integration.testapp

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appfunction.integration.test.sharedschema.AppFunctionNote
import androidx.appfunction.integration.test.sharedschema.ClassWithOptionalValues
import androidx.appfunction.integration.test.sharedschema.CreateNoteAppFunction
import androidx.appfunction.integration.test.sharedschema.CreateNoteParams
import androidx.appfunction.integration.test.sharedschema.DateTime
import androidx.appfunction.integration.test.sharedschema.FilesData
import androidx.appfunction.integration.test.sharedschema.IntEnumSerializable
import androidx.appfunction.integration.test.sharedschema.Note
import androidx.appfunction.integration.test.sharedschema.OneOfSealedInterface
import androidx.appfunction.integration.test.sharedschema.OneOfSealedNestedSerializable
import androidx.appfunction.integration.test.sharedschema.OpenableNote
import androidx.appfunction.integration.test.sharedschema.Owner
import androidx.appfunction.integration.test.sharedschema.ResourceFunctionResponse
import androidx.appfunction.integration.test.sharedschema.UpdateNoteParams
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionIntValueConstraint
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionStringValueConstraint
import androidx.appfunctions.AppFunctionTextResource
import androidx.appfunctions.AppFunctionUriGrant
import androidx.appfunctions.service.AppFunction
import java.time.LocalDateTime
import kotlinx.coroutines.delay

@Suppress("UNUSED_PARAMETER")
class TestFunctions {
    /**
     * Returns the sum of the given two numbers.
     *
     * @param num1 The first number.
     * @param num2 The second number.
     * @return The sum of the two numbers.
     */
    @AppFunction(isDescribedByKDoc = true)
    fun add(appFunctionContext: AppFunctionContext, num1: Long, num2: Long) = num1 + num2

    @AppFunction
    fun logLocalDateTime(appFunctionContext: AppFunctionContext, dateTime: DateTime) {
        Log.d("TestFunctions", "LocalDateTime: ${dateTime.localDateTime}")
    }

    @AppFunction
    fun getLocalDate(appFunctionContext: AppFunctionContext): DateTime {
        return DateTime(localDateTime = LocalDateTime.now())
    }

    @AppFunction
    fun doThrow(appFunctionContext: AppFunctionContext) {
        throw AppFunctionInvalidArgumentException("invalid")
    }

    @AppFunction fun voidFunction(appFunctionContext: AppFunctionContext) {}

    @AppFunction
    fun createNote(
        appFunctionContext: AppFunctionContext,
        createNoteParams: CreateNoteParams,
    ): Note {
        return Note(
            title = createNoteParams.title,
            content = createNoteParams.content,
            owner = createNoteParams.owner,
            attachments = createNoteParams.attachments,
        )
    }

    @AppFunction
    fun updateNote(
        appFunctionContext: AppFunctionContext,
        updateNoteParams: UpdateNoteParams,
    ): Note {
        return Note(
            title =
                (updateNoteParams.title?.value ?: "DefaultTitle") +
                    "_" +
                    (updateNoteParams.nullableTitle?.value ?: "DefaultTitle"),
            content =
                (updateNoteParams.content?.value ?: listOf("DefaultContent")) +
                    (updateNoteParams.nullableContent?.value ?: listOf("DefaultContent")),
            owner = Owner("test"),
            attachments = updateNoteParams.attachments?.value ?: emptyList(),
            modifiedTime = updateNoteParams.modifiedTime?.value,
        )
    }

    @AppFunction
    fun getOpenableNote(
        appFunctionContext: AppFunctionContext,
        createNoteParams: CreateNoteParams,
    ): OpenableNote {
        return OpenableNote(
            title = createNoteParams.title,
            content = createNoteParams.content,
            owner = createNoteParams.owner,
            attachments = createNoteParams.attachments,
            intentToOpen =
                PendingIntent.getActivity(
                    appFunctionContext.context,
                    0,
                    Intent(),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
        )
    }

    @AppFunction
    fun echoClassWithOptionalValues(
        appFunctionContext: AppFunctionContext,
        classWithOptionalValues: ClassWithOptionalValues,
    ): ClassWithOptionalValues {
        return classWithOptionalValues
    }

    @AppFunction
    fun enumValueFunction(
        appFunctionContext: AppFunctionContext,
        @AppFunctionIntValueConstraint(enumValues = [0, 1]) intEnum: Int,
        @AppFunctionStringValueConstraint(enumValues = ["A", "B"]) stringEnum: String,
        intEnumSerializable: IntEnumSerializable? = null,
    ) {
        throw UnsupportedOperationException("Not implemented")
    }

    @AppFunction
    fun echoFunctionWithOptionalParameters(
        appFunctionContext: AppFunctionContext,
        // Int
        optionalNonNullInt: Int = 1,
        optionalNullableInt: Int? = 1,
        // Long
        optionalNonNullLong: Long = 2L,
        optionalNullableLong: Long? = 2L,
        // Boolean
        optionalNonNullBoolean: Boolean = false,
        optionalNullableBoolean: Boolean? = false,
        // Float
        optionalNonNullFloat: Float = 3.0f,
        optionalNullableFloat: Float? = 3.0f,
        // Double
        optionalNonNullDouble: Double = 4.0,
        optionalNullableDouble: Double? = 4.0,
        // String
        optionalNullableString: String? = "Default String",
        // Serializable
        optionalNullableSerializable: Owner? = Owner("Default Owner"),
        // Proxy (using LocalDateTime as an example)
        optionalNullableProxySerializable: LocalDateTime? = LocalDateTime.now(),
        // IntArray
        optionalNonNullIntArray: IntArray = intArrayOf(1),
        optionalNullableIntArray: IntArray? = intArrayOf(1),
        // LongArray
        optionalNonNullLongArray: LongArray = longArrayOf(2L),
        optionalNullableLongArray: LongArray? = longArrayOf(2L),
        // BooleanArray
        optionalNonNullBooleanArray: BooleanArray = booleanArrayOf(false),
        optionalNullableBooleanArray: BooleanArray? = booleanArrayOf(false),
        // FloatArray
        optionalNonNullFloatArray: FloatArray = floatArrayOf(3.0f),
        optionalNullableFloatArray: FloatArray? = floatArrayOf(3.0f),
        // DoubleArray
        optionalNonNullDoubleArray: DoubleArray = doubleArrayOf(4.0),
        optionalNullableDoubleArray: DoubleArray? = doubleArrayOf(4.0),
        // ByteArray
        optionalNonNullByteArray: ByteArray = byteArrayOf(5),
        optionalNullableByteArray: ByteArray? = byteArrayOf(5),
        // List<String>
        optionalNonNullListString: List<String> = listOf("Default String"),
        optionalNullableListString: List<String>? = listOf("Default String"),
        // List of Serializable
        optionalNonNullSerializableList: List<Owner> = listOf(Owner("Default Owner")),
        optionalNullableSerializableList: List<Owner>? = listOf(Owner("Default Owner")),
        // List of Proxy
        optionalNonNullProxySerializableList: List<LocalDateTime> = listOf(LocalDateTime.now()),
        optionalNullableProxySerializableList: List<LocalDateTime>? = listOf(LocalDateTime.now()),
    ): ClassWithOptionalValues {
        return ClassWithOptionalValues(
            optionalNonNullInt = optionalNonNullInt,
            optionalNullableInt = optionalNullableInt,
            optionalNonNullLong = optionalNonNullLong,
            optionalNullableLong = optionalNullableLong,
            optionalNonNullBoolean = optionalNonNullBoolean,
            optionalNullableBoolean = optionalNullableBoolean,
            optionalNonNullFloat = optionalNonNullFloat,
            optionalNullableFloat = optionalNullableFloat,
            optionalNonNullDouble = optionalNonNullDouble,
            optionalNullableDouble = optionalNullableDouble,
            optionalNullableString = optionalNullableString,
            optionalNullableSerializable = optionalNullableSerializable,
            optionalNullableProxySerializable = optionalNullableProxySerializable,
            optionalNonNullIntArray = optionalNonNullIntArray,
            optionalNullableIntArray = optionalNullableIntArray,
            optionalNonNullLongArray = optionalNonNullLongArray,
            optionalNullableLongArray = optionalNullableLongArray,
            optionalNonNullBooleanArray = optionalNonNullBooleanArray,
            optionalNullableBooleanArray = optionalNullableBooleanArray,
            optionalNonNullFloatArray = optionalNonNullFloatArray,
            optionalNullableFloatArray = optionalNullableFloatArray,
            optionalNonNullDoubleArray = optionalNonNullDoubleArray,
            optionalNullableDoubleArray = optionalNullableDoubleArray,
            optionalNonNullByteArray = optionalNonNullByteArray,
            optionalNullableByteArray = optionalNullableByteArray,
            optionalNonNullListString = optionalNonNullListString,
            optionalNullableListString = optionalNullableListString,
            optionalNonNullSerializableList = optionalNonNullSerializableList,
            optionalNullableSerializableList = optionalNullableSerializableList,
            optionalNonNullProxySerializableList = optionalNonNullProxySerializableList,
            optionalNullableProxySerializableList = optionalNullableProxySerializableList,
        )
    }

    @AppFunction
    fun getFilesData(appFunctionContext: AppFunctionContext): FilesData {
        return FilesData(
            readOnlyUri =
                AppFunctionUriGrant(
                    uri =
                        Uri.parse(
                            "content://androidx.appfunctions.integration.testapp.provider/read_only_test_file.txt"
                        ),
                    modeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION,
                ),
            writeOnlyUri =
                AppFunctionUriGrant(
                    uri =
                        Uri.parse(
                            "content://androidx.appfunctions.integration.testapp.provider/write_only_test_file.txt"
                        ),
                    modeFlags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                ),
            readWriteUri =
                AppFunctionUriGrant(
                    uri =
                        Uri.parse(
                            "content://androidx.appfunctions.integration.testapp.provider/read_write_test_file.txt"
                        ),
                    modeFlags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                ),
            persistReadWriteUri =
                AppFunctionUriGrant(
                    uri =
                        Uri.parse(
                            "content://androidx.appfunctions.integration.testapp.provider/persist_read_write_test_file.txt"
                        ),
                    modeFlags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                ),
        )
    }

    @AppFunction
    suspend fun longRunningFunction(appFunctionContext: AppFunctionContext): String {
        delay(500)
        return "Completed"
    }

    @AppFunction
    @Deprecated("deprecatedFunction is deprecated")
    fun deprecatedFunction(appFunctionContext: AppFunctionContext) {}
}

@Suppress("UNUSED_PARAMETER")
class TestFactory {
    private val createdByFactory: Boolean

    constructor() : this(false)

    constructor(createdByFactory: Boolean) {
        this.createdByFactory = createdByFactory
    }

    @AppFunction
    fun isCreatedByFactory(appFunctionContext: AppFunctionContext): Boolean = createdByFactory
}

class NotesFunctions : CreateNoteAppFunction {

    /**
     * Create a note.
     *
     * @param parameters The parameters.
     * @param tag The optional tag.
     * @return [androidx.appfunction.integration.test.sharedschema.CreateNoteAppFunction.Response]
     *   as response.
     */
    @AppFunction(isDescribedByKDoc = true)
    override suspend fun createNote(
        appFunctionContext: AppFunctionContext,
        parameters: CreateNoteAppFunction.Parameters,
        tag: String?,
    ): CreateNoteAppFunction.Response {
        return CreateNoteAppFunction.Response(
            AppFunctionNote(id = "testId", title = parameters.title),
            tag = tag,
        )
    }
}

class OneOfFunctions {

    @AppFunction
    fun oneOfFunction(
        appFunctionContext: AppFunctionContext,
        oneOfList: List<OneOfSealedInterface>,
    ) = oneOfList.map { OneOfSealedNestedSerializable(sealedInterface = it) }
}

class ResourceFunctions {
    @AppFunction
    fun textResourceFunction(
        appFunctionContext: AppFunctionContext,
        text: String,
    ): ResourceFunctionResponse =
        ResourceFunctionResponse(
            stringValue = text,
            resources = listOf(AppFunctionTextResource(mimeType = "text/plain", content = text)),
        )
}
