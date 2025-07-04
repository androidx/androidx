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

package androidx.appfunctions.service.internal

import android.os.Build
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_BOOLEAN
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_DOUBLE
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_LONG
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_STRING
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
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
                valueType =
                    AppFunctionPrimitiveTypeMetadata(type = TYPE_LONG, isNullable = isNullable)
            )

        val returnValue = responseMetadata.unsafeBuildReturnValue(result)

        assertThat(returnValue.getLong(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE))
            .isEqualTo(100L)
    }

    @Test
    fun testUnsafeBuildExecuteAppFunctionResponse_buildNonNullSingleResponse_wrongResultType() {
        val result: Double = 5.0
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType = AppFunctionPrimitiveTypeMetadata(type = TYPE_LONG, isNullable = false)
            )

        assertThrows(AppFunctionAppUnknownException::class.java) {
            responseMetadata.unsafeBuildReturnValue(result)
        }
    }

    @Test
    fun testUnsafeBuildExecuteAppFunctionResponse_buildNonNullSingleResponse_resultNull() {
        val result = null
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType = AppFunctionPrimitiveTypeMetadata(type = TYPE_DOUBLE, isNullable = false)
            )

        assertThrows(AppFunctionAppUnknownException::class.java) {
            responseMetadata.unsafeBuildReturnValue(result)
        }
    }

    @Test
    fun testUnsafeBuildExecuteAppFunctionResponse_buildNullableSingleResponse_resultNull() {
        val result = null
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType = AppFunctionPrimitiveTypeMetadata(type = TYPE_STRING, isNullable = true)
            )

        val returnValue = responseMetadata.unsafeBuildReturnValue(result)

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
                        itemType =
                            AppFunctionPrimitiveTypeMetadata(
                                type = TYPE_DOUBLE,
                                isNullable = false,
                            ),
                        isNullable = isNullable,
                    )
            )

        val returnValue = responseMetadata.unsafeBuildReturnValue(result)

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
                        itemType =
                            AppFunctionPrimitiveTypeMetadata(
                                type = TYPE_STRING,
                                isNullable = false,
                            ),
                        isNullable = false,
                    )
            )

        assertThrows(AppFunctionAppUnknownException::class.java) {
            responseMetadata.unsafeBuildReturnValue(result)
        }
    }

    @Test
    fun testUnsafeBuildExecuteAppFunctionResponse_buildNonNullCollectionResponse_resultNull() {
        val result = null
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType =
                    AppFunctionArrayTypeMetadata(
                        itemType =
                            AppFunctionPrimitiveTypeMetadata(
                                type = TYPE_BOOLEAN,
                                isNullable = false,
                            ),
                        isNullable = false,
                    )
            )

        assertThrows(AppFunctionAppUnknownException::class.java) {
            responseMetadata.unsafeBuildReturnValue(result)
        }
    }

    @Test
    fun testUnsafeBuildExecuteAppFunctionResponse_buildNullableCollectionResponse_resultNull() {
        val result = null
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType =
                    AppFunctionArrayTypeMetadata(
                        itemType =
                            AppFunctionPrimitiveTypeMetadata(type = TYPE_LONG, isNullable = false),
                        isNullable = true,
                    )
            )

        val returnValue = responseMetadata.unsafeBuildReturnValue(result)

        assertThat(
                returnValue.getLongArray(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
            )
            .isNull()
    }
}
