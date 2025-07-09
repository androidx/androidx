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
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_BOOLEAN
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_DOUBLE
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_INT
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_LONG
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_STRING
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
@RunWith(TestParameterInjector::class)
class AppFunctionDataParameterExtractorTest {

    private val testAppFunctionData =
        AppFunctionData.Builder("")
            .setLong("long", 1L)
            .setDouble("double", 2.0)
            .setBoolean("boolean", true)
            .setString("string", "testString")
            .setLongArray("longArray", longArrayOf(1L, 2L, 3L))
            .setDoubleArray("doubleArray", doubleArrayOf(1.0, 2.0, 3.0))
            .setBooleanArray("booleanArray", booleanArrayOf(false, true, false))
            .setStringList("stringList", listOf("testString1", "testString2", "testString3"))
            .build()

    @Test
    fun testAppFunctionData_extractRequiredSingleParameters_exist(
        @TestParameter isNullable: Boolean
    ) {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "long",
                isRequired = true,
                dataType =
                    AppFunctionPrimitiveTypeMetadata(type = TYPE_LONG, isNullable = isNullable),
            )

        val parameter = testAppFunctionData.unsafeGetParameterValue(parameterMetadata)

        assertThat(parameter).isEqualTo(1L)
    }

    @Test
    fun testAppFunctionData_extractRequiredSingleParameters_notExist(
        @TestParameter isNullable: Boolean
    ) {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "fakeDouble",
                isRequired = true,
                dataType =
                    AppFunctionPrimitiveTypeMetadata(type = TYPE_DOUBLE, isNullable = isNullable),
            )

        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            testAppFunctionData.unsafeGetParameterValue(parameterMetadata)
        }
    }

    @Test
    fun testAppFunctionData_extractNotRequiredSingleParameters_exist(
        @TestParameter isNullable: Boolean
    ) {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "boolean",
                isRequired = false,
                dataType =
                    AppFunctionPrimitiveTypeMetadata(type = TYPE_BOOLEAN, isNullable = isNullable),
            )

        val parameter = testAppFunctionData.unsafeGetParameterValue(parameterMetadata)

        assertThat(parameter).isEqualTo(true)
    }

    @Test
    fun testAppFunctionData_extractNotRequiredNullableSingleParameters_notExist() {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "fakeInt",
                isRequired = false,
                dataType = AppFunctionPrimitiveTypeMetadata(type = TYPE_INT, isNullable = true),
            )

        val parameter = testAppFunctionData.unsafeGetParameterValue(parameterMetadata)

        assertThat(parameter).isNull()
    }

    @Test
    fun testAppFunctionData_extractNotRequiredNonNullSingleParameters_notExist() {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "fakeInt",
                isRequired = false,
                dataType = AppFunctionPrimitiveTypeMetadata(type = TYPE_INT, isNullable = false),
            )

        val parameter = testAppFunctionData.unsafeGetParameterValue(parameterMetadata)

        assertThat(parameter).isEqualTo(0)
    }

    @Test
    fun testAppFunctionData_extractSingleParameters_wrongType(
        @TestParameter isRequired: Boolean,
        @TestParameter isNullable: Boolean,
    ) {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "boolean",
                isRequired = isRequired,
                dataType =
                    AppFunctionPrimitiveTypeMetadata(type = TYPE_STRING, isNullable = isNullable),
            )

        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            testAppFunctionData.unsafeGetParameterValue(parameterMetadata)
        }
    }

    @Test
    fun testAppFunctionData_extractRequiredCollectionParameters_exist(
        @TestParameter isNullable: Boolean
    ) {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "longArray",
                isRequired = true,
                dataType =
                    AppFunctionArrayTypeMetadata(
                        isNullable = isNullable,
                        itemType =
                            AppFunctionPrimitiveTypeMetadata(type = TYPE_LONG, isNullable = false),
                    ),
            )

        val parameter = testAppFunctionData.unsafeGetParameterValue(parameterMetadata)

        assertThat(parameter).isInstanceOf(LongArray::class.java)
        assertThat(parameter as LongArray).asList().containsExactly(1L, 2L, 3L)
    }

    @Test
    fun testAppFunctionData_extractRequiredCollectionParameters_notExist(
        @TestParameter isNullable: Boolean
    ) {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "fakeDoubleArray",
                isRequired = true,
                dataType =
                    AppFunctionArrayTypeMetadata(
                        isNullable = isNullable,
                        itemType =
                            AppFunctionPrimitiveTypeMetadata(type = TYPE_DOUBLE, isNullable = false),
                    ),
            )

        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            testAppFunctionData.unsafeGetParameterValue(parameterMetadata)
        }
    }

    @Test
    fun testAppFunctionData_extractNotRequiredCollectionParameters_exist(
        @TestParameter isNullable: Boolean
    ) {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "booleanArray",
                isRequired = false,
                dataType =
                    AppFunctionArrayTypeMetadata(
                        isNullable = isNullable,
                        itemType =
                            AppFunctionPrimitiveTypeMetadata(
                                type = TYPE_BOOLEAN,
                                isNullable = false,
                            ),
                    ),
            )

        val parameter = testAppFunctionData.unsafeGetParameterValue(parameterMetadata)

        assertThat(parameter).isInstanceOf(BooleanArray::class.java)
        assertThat(parameter as BooleanArray).asList().containsExactly(false, true, false)
    }

    @Test
    fun testAppFunctionData_extractNotRequiredNullableCollectionParameters_notExist() {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "fakeStringList",
                isRequired = false,
                dataType =
                    AppFunctionArrayTypeMetadata(
                        isNullable = true,
                        itemType =
                            AppFunctionPrimitiveTypeMetadata(type = TYPE_STRING, isNullable = true),
                    ),
            )

        val parameter = testAppFunctionData.unsafeGetParameterValue(parameterMetadata)

        assertThat(parameter).isNull()
    }

    @Test
    fun testAppFunctionData_extractNotRequiredNonNullCollectionParameters_notExist() {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "fakeStringList",
                isRequired = false,
                dataType =
                    AppFunctionArrayTypeMetadata(
                        isNullable = false,
                        itemType =
                            AppFunctionPrimitiveTypeMetadata(type = TYPE_STRING, isNullable = false),
                    ),
            )

        val parameter = testAppFunctionData.unsafeGetParameterValue(parameterMetadata)

        assertThat(parameter).isEqualTo(emptyList<String>())
    }

    @Test
    fun testAppFunctionData_extractCollectionParameters_wrongType(
        @TestParameter isRequired: Boolean,
        @TestParameter isNullable: Boolean,
    ) {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "booleanArray",
                isRequired = isRequired,
                dataType =
                    AppFunctionArrayTypeMetadata(
                        isNullable = isNullable,
                        itemType =
                            AppFunctionPrimitiveTypeMetadata(type = TYPE_STRING, isNullable = false),
                    ),
            )

        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            testAppFunctionData.unsafeGetParameterValue(parameterMetadata)
        }
    }
}
