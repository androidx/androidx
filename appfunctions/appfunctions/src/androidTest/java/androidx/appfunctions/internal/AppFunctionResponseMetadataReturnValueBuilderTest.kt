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

package androidx.appfunctions.internal

import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Build
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionUriGrant
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDoubleTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import kotlin.test.assertFailsWith
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
@RunWith(TestParameterInjector::class)
class AppFunctionResponseMetadataReturnValueBuilderTest {
    @Test
    fun testUnsafeBuildExecuteAppFunctionResponse_buildSingleResponse(
        @TestParameter isNullable: Boolean
    ) {
        val result = 100L
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType = AppFunctionLongTypeMetadata(isNullable = isNullable)
            )

        val returnValue =
            responseMetadata.unsafeBuildReturnValue(result, AppFunctionComponentsMetadata())

        assertThat(returnValue.getLong(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE))
            .isEqualTo(100L)
    }

    @Test
    fun testUnsafeBuildExecuteAppFunctionResponse_buildNonNullSingleResponse_wrongResultType() {
        val result: Double = 5.0
        val responseMetadata =
            AppFunctionResponseMetadata(valueType = AppFunctionLongTypeMetadata(isNullable = false))

        assertThrows(AppFunctionAppUnknownException::class.java) {
            responseMetadata.unsafeBuildReturnValue(result, AppFunctionComponentsMetadata())
        }
    }

    @Test
    fun testUnsafeBuildExecuteAppFunctionResponse_buildNonNullSingleResponse_resultNull() {
        val result = null
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType = AppFunctionDoubleTypeMetadata(isNullable = false)
            )

        assertThrows(AppFunctionAppUnknownException::class.java) {
            responseMetadata.unsafeBuildReturnValue(result, AppFunctionComponentsMetadata())
        }
    }

    @Test
    fun testUnsafeBuildExecuteAppFunctionResponse_buildNullableSingleResponse_resultNull() {
        val result = null
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType = AppFunctionStringTypeMetadata(isNullable = true)
            )

        val returnValue =
            responseMetadata.unsafeBuildReturnValue(result, AppFunctionComponentsMetadata())

        assertThat(returnValue.getString(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE))
            .isNull()
    }

    @Test
    fun testUnsafeBuildExecuteAppFunctionResponse_buildCollectionResponse(
        @TestParameter isNullable: Boolean
    ) {
        val result = doubleArrayOf(1.0, 2.0, 3.0)
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType =
                    AppFunctionArrayTypeMetadata(
                        itemType = AppFunctionDoubleTypeMetadata(isNullable = false),
                        isNullable = isNullable,
                    )
            )

        val returnValue =
            responseMetadata.unsafeBuildReturnValue(result, AppFunctionComponentsMetadata())

        assertThat(
                returnValue.getDoubleArray(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
            )
            .usingExactEquality()
            .containsExactly(1.0, 2.0, 3.0)
    }

    @Test
    fun testUnsafeBuildExecuteAppFunctionResponse_buildNonNullCollectionResponse_wrongResultType() {
        val result = booleanArrayOf(true, false, true)
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType =
                    AppFunctionArrayTypeMetadata(
                        itemType = AppFunctionStringTypeMetadata(isNullable = false),
                        isNullable = false,
                    )
            )

        assertThrows(AppFunctionAppUnknownException::class.java) {
            responseMetadata.unsafeBuildReturnValue(result, AppFunctionComponentsMetadata())
        }
    }

    @Test
    fun testUnsafeBuildExecuteAppFunctionResponse_buildNonNullCollectionResponse_resultNull() {
        val result = null
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType =
                    AppFunctionArrayTypeMetadata(
                        itemType = AppFunctionBooleanTypeMetadata(isNullable = false),
                        isNullable = false,
                    )
            )

        assertThrows(AppFunctionAppUnknownException::class.java) {
            responseMetadata.unsafeBuildReturnValue(result, AppFunctionComponentsMetadata())
        }
    }

    @Test
    fun testUnsafeBuildExecuteAppFunctionResponse_buildNullableCollectionResponse_resultNull() {
        val result = null
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType =
                    AppFunctionArrayTypeMetadata(
                        itemType = AppFunctionLongTypeMetadata(isNullable = false),
                        isNullable = true,
                    )
            )

        val returnValue =
            responseMetadata.unsafeBuildReturnValue(result, AppFunctionComponentsMetadata())

        assertThat(
                returnValue.getLongArray(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
            )
            .isNull()
    }

    @Test
    fun testUnsafeBuildExecuteAppFunctionResponse_failsForInvalidSpec() {
        val result = 10
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType =
                    AppFunctionIntTypeMetadata(isNullable = false, enumValues = setOf(1, 2, 3))
            )

        assertFailsWith<AppFunctionAppUnknownException> {
            responseMetadata.unsafeBuildReturnValue(result, AppFunctionComponentsMetadata())
        }
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildUnitResponse() {
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_UNIT,
                isNullable = false,
            )
        val returnValue = spec.unsafeBuildReturnValue(Unit)
        assertThat(returnValue).isEqualTo(AppFunctionData.EMPTY)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildSingleResponse(
        @TestParameter isNullable: Boolean
    ) {
        val result = 100L
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_LONG,
                isNullable = isNullable,
            )

        val returnValue = spec.unsafeBuildReturnValue(result)

        assertThat(returnValue.getLong(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE))
            .isEqualTo(100L)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildDoubleResponse() {
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_DOUBLE,
                isNullable = false,
            )
        val returnValue = spec.unsafeBuildReturnValue(5.0)
        assertThat(returnValue.getDouble(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE))
            .isEqualTo(5.0)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildIntResponse() {
        val spec =
            AppFunctionResponseSpec(type = AppFunctionDataTypeMetadata.TYPE_INT, isNullable = false)
        val returnValue = spec.unsafeBuildReturnValue(100)
        assertThat(returnValue.getInt(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE))
            .isEqualTo(100)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildFloatResponse() {
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_FLOAT,
                isNullable = false,
            )
        val returnValue = spec.unsafeBuildReturnValue(100.0f)
        assertThat(returnValue.getFloat(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE))
            .isEqualTo(100.0f)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildBooleanResponse() {
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_BOOLEAN,
                isNullable = false,
            )
        val returnValue = spec.unsafeBuildReturnValue(true)
        assertThat(returnValue.getBoolean(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE))
            .isEqualTo(true)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildStringResponse() {
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_STRING,
                isNullable = false,
            )
        val returnValue = spec.unsafeBuildReturnValue("test")
        assertThat(returnValue.getString(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE))
            .isEqualTo("test")
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildBytesResponse() {
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_BYTES,
                isNullable = false,
            )
        val returnValue = spec.unsafeBuildReturnValue(byteArrayOf(1, 2, 3))
        assertThat(
                returnValue
                    .getByteArray(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
                    ?.toList()
            )
            .containsExactly(1.toByte(), 2.toByte(), 3.toByte())
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildParcelableResponse() {
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_PARCELABLE,
                isNullable = false,
                objectQualifiedName = Intent::class.java.name,
            )
        val returnValue = spec.unsafeBuildReturnValue(Intent("test"))
        val parcelable =
            returnValue.getParcelable(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                Intent::class.java,
            )
        assertThat(parcelable?.action).isEqualTo("test")
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildObjectResponse() {
        val uri = Uri.parse("content://test")
        val grant = AppFunctionUriGrant(uri, FLAG_GRANT_READ_URI_PERMISSION)
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_OBJECT,
                isNullable = false,
                objectQualifiedName = AppFunctionUriGrant::class.java.name,
            )

        val returnValue = spec.unsafeBuildReturnValue(grant)

        val returnedData =
            returnValue.getAppFunctionData(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
        assertThat(returnedData).isNotNull()
        assertThat(
                returnedData?.deserialize<AppFunctionUriGrant>(AppFunctionUriGrant::class.java.name)
            )
            .isEqualTo(grant)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildReferenceResponse() {
        val uri = Uri.parse("content://test")
        val grant = AppFunctionUriGrant(uri, FLAG_GRANT_READ_URI_PERMISSION)
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_REFERENCE,
                isNullable = false,
                objectQualifiedName = AppFunctionUriGrant::class.java.name,
            )

        val returnValue = spec.unsafeBuildReturnValue(grant)

        val returnedData =
            returnValue.getAppFunctionData(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
        assertThat(returnedData).isNotNull()
        assertThat(
                returnedData?.deserialize<AppFunctionUriGrant>(AppFunctionUriGrant::class.java.name)
            )
            .isEqualTo(grant)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildNonNullSingleResponse_wrongResultType() {
        val result: Double = 5.0
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_LONG,
                isNullable = false,
            )

        assertThrows(AppFunctionAppUnknownException::class.java) {
            spec.unsafeBuildReturnValue(result)
        }
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildNonNullSingleResponse_resultNull() {
        val result = null
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_DOUBLE,
                isNullable = false,
            )

        assertThrows(AppFunctionAppUnknownException::class.java) {
            spec.unsafeBuildReturnValue(result)
        }
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildNullableSingleResponse_resultNull() {
        val result = null
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_STRING,
                isNullable = true,
            )

        val returnValue = spec.unsafeBuildReturnValue(result)

        assertThat(returnValue).isEqualTo(AppFunctionData.EMPTY)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildCollectionResponse(
        @TestParameter isNullable: Boolean
    ) {
        val result = doubleArrayOf(1.0, 2.0, 3.0)
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                isNullable = isNullable,
                itemType = AppFunctionDataTypeMetadata.TYPE_DOUBLE,
            )

        val returnValue = spec.unsafeBuildReturnValue(result)

        assertThat(
                returnValue.getDoubleArray(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
            )
            .usingExactEquality()
            .containsExactly(1.0, 2.0, 3.0)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildIntArrayResponse() {
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                isNullable = false,
                itemType = AppFunctionDataTypeMetadata.TYPE_INT,
            )
        val returnValue = spec.unsafeBuildReturnValue(intArrayOf(1, 2, 3))
        assertThat(
                returnValue
                    .getIntArray(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
                    ?.toList()
            )
            .containsExactly(1, 2, 3)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildFloatArrayResponse() {
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                isNullable = false,
                itemType = AppFunctionDataTypeMetadata.TYPE_FLOAT,
            )
        val returnValue = spec.unsafeBuildReturnValue(floatArrayOf(1.0f, 2.0f, 3.0f))
        assertThat(
                returnValue
                    .getFloatArray(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
                    ?.toList()
            )
            .containsExactly(1.0f, 2.0f, 3.0f)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildLongArrayResponse() {
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                isNullable = false,
                itemType = AppFunctionDataTypeMetadata.TYPE_LONG,
            )
        val returnValue = spec.unsafeBuildReturnValue(longArrayOf(1L, 2L, 3L))
        assertThat(
                returnValue
                    .getLongArray(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
                    ?.toList()
            )
            .containsExactly(1L, 2L, 3L)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildBooleanArrayResponse() {
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                isNullable = false,
                itemType = AppFunctionDataTypeMetadata.TYPE_BOOLEAN,
            )
        val returnValue = spec.unsafeBuildReturnValue(booleanArrayOf(true, false, true))
        assertThat(
                returnValue
                    .getBooleanArray(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
                    ?.toList()
            )
            .containsExactly(true, false, true)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildStringArrayResponse() {
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                isNullable = false,
                itemType = AppFunctionDataTypeMetadata.TYPE_STRING,
            )
        val returnValue = spec.unsafeBuildReturnValue(listOf("1", "2", "3"))
        assertThat(
                returnValue.getStringList(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
            )
            .containsExactly("1", "2", "3")
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildParcelableArrayResponse() {
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                isNullable = false,
                itemType = AppFunctionDataTypeMetadata.TYPE_PARCELABLE,
                itemQualifiedName = Intent::class.java.name,
            )
        val returnValue = spec.unsafeBuildReturnValue(listOf(Intent("test")))
        val list =
            returnValue.getParcelableList(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                Intent::class.java,
            )
        assertThat(list?.map { it.action }).containsExactly("test")
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildObjectArrayResponse() {
        val uri = Uri.parse("content://test")
        val grant = AppFunctionUriGrant(uri, FLAG_GRANT_READ_URI_PERMISSION)
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                isNullable = false,
                itemType = AppFunctionDataTypeMetadata.TYPE_OBJECT,
                itemQualifiedName = AppFunctionUriGrant::class.java.name,
            )

        val returnValue = spec.unsafeBuildReturnValue(listOf(grant))

        val returnedList =
            returnValue.getAppFunctionDataList(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
            )
        assertThat(returnedList).isNotNull()
        assertThat(
                returnedList?.map {
                    it.deserialize<AppFunctionUriGrant>(AppFunctionUriGrant::class.java.name)
                }
            )
            .containsExactly(grant)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildReferenceArrayResponse() {
        val uri = Uri.parse("content://test")
        val grant = AppFunctionUriGrant(uri, FLAG_GRANT_READ_URI_PERMISSION)
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                isNullable = false,
                itemType = AppFunctionDataTypeMetadata.TYPE_REFERENCE,
                itemQualifiedName = AppFunctionUriGrant::class.java.name,
            )

        val returnValue = spec.unsafeBuildReturnValue(listOf(grant))

        val returnedList =
            returnValue.getAppFunctionDataList(
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
            )
        assertThat(returnedList).isNotNull()
        assertThat(
                returnedList?.map {
                    it.deserialize<AppFunctionUriGrant>(AppFunctionUriGrant::class.java.name)
                }
            )
            .containsExactly(grant)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildNonNullCollectionResponse_wrongResultType() {
        val result = booleanArrayOf(true, false, true)
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                isNullable = false,
                itemType = AppFunctionDataTypeMetadata.TYPE_STRING,
            )

        assertThrows(AppFunctionAppUnknownException::class.java) {
            spec.unsafeBuildReturnValue(result)
        }
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildNonNullCollectionResponse_resultNull() {
        val result = null
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                isNullable = false,
                itemType = AppFunctionDataTypeMetadata.TYPE_BOOLEAN,
            )

        assertThrows(AppFunctionAppUnknownException::class.java) {
            spec.unsafeBuildReturnValue(result)
        }
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildNullableCollectionResponse_resultNull() {
        val result = null
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                isNullable = true,
                itemType = AppFunctionDataTypeMetadata.TYPE_LONG,
            )

        val returnValue = spec.unsafeBuildReturnValue(result)

        assertThat(returnValue).isEqualTo(AppFunctionData.EMPTY)
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_failsForInvalidSpec() {
        val result = 10
        val spec = AppFunctionResponseSpec(type = -1, isNullable = false)

        assertFailsWith<AppFunctionAppUnknownException> { spec.unsafeBuildReturnValue(result) }
    }

    @Test
    fun testUnsafeBuildExecuteResponseFromSpec_buildUnknownArrayItemType_throws() {
        val spec =
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                isNullable = false,
                itemType = -1,
            )
        assertThrows(AppFunctionAppUnknownException::class.java) {
            spec.unsafeBuildReturnValue(listOf("test"))
        }
    }

    @Test
    fun testAppFunctionResponseSpec_invalidObjectMissingQualifiedName_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_OBJECT,
                isNullable = false,
            )
        }
    }

    @Test
    fun testAppFunctionResponseSpec_invalidReferenceMissingQualifiedName_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_REFERENCE,
                isNullable = false,
            )
        }
    }

    @Test
    fun testAppFunctionResponseSpec_invalidArrayMissingItemType_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                isNullable = false,
            )
        }
    }

    @Test
    fun testAppFunctionResponseSpec_invalidObjectArrayMissingItemQualifiedName_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                isNullable = false,
                itemType = AppFunctionDataTypeMetadata.TYPE_OBJECT,
            )
        }
    }

    @Test
    fun testAppFunctionResponseSpec_invalidReferenceArrayMissingItemQualifiedName_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            AppFunctionResponseSpec(
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                isNullable = false,
                itemType = AppFunctionDataTypeMetadata.TYPE_REFERENCE,
            )
        }
    }
}
