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
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionUriGrant
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDoubleTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
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
        AppFunctionData.Builder(
                listOf(
                    AppFunctionParameterMetadata(
                        "long",
                        true,
                        AppFunctionLongTypeMetadata(isNullable = false),
                    ),
                    AppFunctionParameterMetadata(
                        "double",
                        true,
                        AppFunctionDoubleTypeMetadata(isNullable = false),
                    ),
                    AppFunctionParameterMetadata(
                        "boolean",
                        true,
                        AppFunctionBooleanTypeMetadata(isNullable = false),
                    ),
                    AppFunctionParameterMetadata(
                        "string",
                        true,
                        AppFunctionStringTypeMetadata(isNullable = false),
                    ),
                    AppFunctionParameterMetadata(
                        "longArray",
                        true,
                        AppFunctionArrayTypeMetadata(
                            isNullable = false,
                            itemType = AppFunctionLongTypeMetadata(isNullable = false),
                        ),
                    ),
                    AppFunctionParameterMetadata(
                        "doubleArray",
                        true,
                        AppFunctionArrayTypeMetadata(
                            isNullable = false,
                            itemType = AppFunctionDoubleTypeMetadata(isNullable = false),
                        ),
                    ),
                    AppFunctionParameterMetadata(
                        "booleanArray",
                        true,
                        AppFunctionArrayTypeMetadata(
                            isNullable = false,
                            itemType = AppFunctionBooleanTypeMetadata(isNullable = false),
                        ),
                    ),
                    AppFunctionParameterMetadata(
                        "stringList",
                        true,
                        AppFunctionArrayTypeMetadata(
                            isNullable = false,
                            itemType = AppFunctionStringTypeMetadata(isNullable = false),
                        ),
                    ),
                ),
                AppFunctionComponentsMetadata(),
            )
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
                dataType = AppFunctionLongTypeMetadata(isNullable = isNullable),
            )

        val parameter = testAppFunctionData.unsafeGetParameterValue(parameterMetadata)

        assertThat(parameter).isEqualTo(1L)
    }

    @Test
    fun testAppFunctionData_extractRequiredNonNullSingleParameters_notExist() {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "fakeDouble",
                isRequired = true,
                dataType = AppFunctionDoubleTypeMetadata(isNullable = false),
            )

        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            testAppFunctionData.unsafeGetParameterValue(parameterMetadata)
        }
    }

    @Test
    fun testAppFunctionData_extractRequiredNullableSingleParameters_notExist() {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "fakeDouble",
                isRequired = true,
                dataType = AppFunctionDoubleTypeMetadata(isNullable = true),
            )
        val testData =
            AppFunctionData.Builder(listOf(parameterMetadata), AppFunctionComponentsMetadata())
                .build()

        val value = testData.unsafeGetParameterValue(parameterMetadata)

        assertThat(value).isNull()
    }

    @Test
    fun testAppFunctionData_extractNotRequiredSingleParameters_exist(
        @TestParameter isNullable: Boolean
    ) {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "boolean",
                isRequired = false,
                dataType = AppFunctionBooleanTypeMetadata(isNullable = isNullable),
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
                dataType = AppFunctionIntTypeMetadata(isNullable = true),
            )
        val testData =
            AppFunctionData.Builder(listOf(parameterMetadata), AppFunctionComponentsMetadata())
                .build()

        val parameter = testData.unsafeGetParameterValue(parameterMetadata)

        assertThat(parameter).isNull()
    }

    @Test
    fun testAppFunctionData_extractNotRequiredNonNullSingleParameters_notExist() {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "fakeInt",
                isRequired = false,
                dataType = AppFunctionIntTypeMetadata(isNullable = false),
            )
        val testData =
            AppFunctionData.Builder(listOf(parameterMetadata), AppFunctionComponentsMetadata())
                .build()

        val parameter = testData.unsafeGetParameterValue(parameterMetadata)

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
                dataType = AppFunctionStringTypeMetadata(isNullable = isNullable),
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
                        itemType = AppFunctionLongTypeMetadata(isNullable = false),
                    ),
            )

        val parameter = testAppFunctionData.unsafeGetParameterValue(parameterMetadata)

        assertThat(parameter).isInstanceOf(LongArray::class.java)
        assertThat(parameter as LongArray).asList().containsExactly(1L, 2L, 3L)
    }

    @Test
    fun testAppFunctionData_extractRequiredNonNullCollectionParameters_notExist() {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "fakeDoubleArray",
                isRequired = true,
                dataType =
                    AppFunctionArrayTypeMetadata(
                        isNullable = false,
                        itemType = AppFunctionDoubleTypeMetadata(isNullable = false),
                    ),
            )

        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            testAppFunctionData.unsafeGetParameterValue(parameterMetadata)
        }
    }

    @Test
    fun testAppFunctionData_extractRequiredNullableCollectionParameters_notExist() {
        val parameterMetadata =
            AppFunctionParameterMetadata(
                name = "fakeDoubleArray",
                isRequired = true,
                dataType =
                    AppFunctionArrayTypeMetadata(
                        isNullable = true,
                        itemType = AppFunctionDoubleTypeMetadata(isNullable = false),
                    ),
            )
        val testData =
            AppFunctionData.Builder(listOf(parameterMetadata), AppFunctionComponentsMetadata())
                .build()

        val value = testData.unsafeGetParameterValue(parameterMetadata)

        assertThat(value).isNull()
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
                        itemType = AppFunctionBooleanTypeMetadata(isNullable = false),
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
                        itemType = AppFunctionStringTypeMetadata(isNullable = true),
                    ),
            )
        val testData =
            AppFunctionData.Builder(listOf(parameterMetadata), AppFunctionComponentsMetadata())
                .build()

        val parameter = testData.unsafeGetParameterValue(parameterMetadata)

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
                        itemType = AppFunctionStringTypeMetadata(isNullable = false),
                    ),
            )
        val testData =
            AppFunctionData.Builder(listOf(parameterMetadata), AppFunctionComponentsMetadata())
                .build()

        val parameter = testData.unsafeGetParameterValue(parameterMetadata)

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
                        itemType = AppFunctionStringTypeMetadata(isNullable = false),
                    ),
            )

        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            testAppFunctionData.unsafeGetParameterValue(parameterMetadata)
        }
    }

    private val testAppFunctionDataFromSpec =
        AppFunctionData.Builder("")
            .setLong("long", 1L)
            .setDouble("double", 2.0)
            .setBoolean("boolean", true)
            .setString("string", "testString")
            .setInt("int", 1)
            .setFloat("float", 1.0f)
            .setByteArray("bytes", byteArrayOf(1, 2, 3))
            .setParcelable("parcelable", Intent("test"))
            .setLongArray("longArray", longArrayOf(1L, 2L, 3L))
            .setDoubleArray("doubleArray", doubleArrayOf(1.0, 2.0, 3.0))
            .setBooleanArray("booleanArray", booleanArrayOf(false, true, false))
            .setStringList("stringList", listOf("testString1", "testString2", "testString3"))
            .setIntArray("intArray", intArrayOf(1, 2, 3))
            .setFloatArray("floatArray", floatArrayOf(1.0f, 2.0f, 3.0f))
            .setParcelableList("parcelableList", listOf(Intent("test")))
            .build()

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredSingleParameters_exist(
        @TestParameter isNullable: Boolean
    ) {
        val spec =
            AppFunctionParameterSpec(
                name = "long",
                isRequired = true,
                isNullable = isNullable,
                type = AppFunctionDataTypeMetadata.TYPE_LONG,
            )

        val parameter = testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)

        assertThat(parameter).isEqualTo(1L)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredDouble_exists() {
        val spec =
            AppFunctionParameterSpec(
                name = "double",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_DOUBLE,
            )

        val parameter = testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)

        assertThat(parameter).isEqualTo(2.0)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredBoolean_exists() {
        val spec =
            AppFunctionParameterSpec(
                name = "boolean",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_BOOLEAN,
            )

        val parameter = testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)

        assertThat(parameter).isEqualTo(true)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredString_exists() {
        val spec =
            AppFunctionParameterSpec(
                name = "string",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_STRING,
            )

        val parameter = testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)

        assertThat(parameter).isEqualTo("testString")
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredDoubleArray_exists() {
        val spec =
            AppFunctionParameterSpec(
                name = "doubleArray",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_DOUBLE,
            )

        val parameter = testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)

        assertThat(parameter).isInstanceOf(DoubleArray::class.java)
        assertThat((parameter as DoubleArray).toList()).containsExactly(1.0, 2.0, 3.0)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredBooleanArray_exists() {
        val spec =
            AppFunctionParameterSpec(
                name = "booleanArray",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_BOOLEAN,
            )

        val parameter = testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)

        assertThat(parameter).isInstanceOf(BooleanArray::class.java)
        assertThat((parameter as BooleanArray).toList()).containsExactly(false, true, false)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredStringArray_exists() {
        val spec =
            AppFunctionParameterSpec(
                name = "stringList",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_STRING,
            )

        val parameter = testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)

        @Suppress("UNCHECKED_CAST")
        assertThat(parameter as List<String>)
            .containsExactly("testString1", "testString2", "testString3")
            .inOrder()
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredInt_exists() {
        val spec =
            AppFunctionParameterSpec(
                name = "int",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_INT,
            )
        assertThat(testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)).isEqualTo(1)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredFloat_exists() {
        val spec =
            AppFunctionParameterSpec(
                name = "float",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_FLOAT,
            )
        assertThat(testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)).isEqualTo(1.0f)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredBytes_exists() {
        val spec =
            AppFunctionParameterSpec(
                name = "bytes",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_BYTES,
            )
        val extracted = testAppFunctionDataFromSpec.unsafeGetParameterValue(spec) as ByteArray
        assertThat(extracted.toList()).containsExactly(1.toByte(), 2.toByte(), 3.toByte())
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredParcelable_exists() {
        val spec =
            AppFunctionParameterSpec(
                name = "parcelable",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_PARCELABLE,
                objectQualifiedName = Intent::class.java.name,
            )
        val extracted = testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)
        assertThat(extracted).isInstanceOf(Intent::class.java)
        assertThat((extracted as Intent).action).isEqualTo("test")
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredObjectParameters_exist() {
        val uri = Uri.parse("content://test")
        val grant = AppFunctionUriGrant(uri, FLAG_GRANT_READ_URI_PERMISSION)
        val testData =
            AppFunctionData.Builder("")
                .setAppFunctionData(
                    "object",
                    AppFunctionData.serialize(grant, AppFunctionUriGrant::class.java.name),
                )
                .build()
        val spec =
            AppFunctionParameterSpec(
                name = "object",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_OBJECT,
                objectQualifiedName = AppFunctionUriGrant::class.java.name,
            )

        val parameter = testData.unsafeGetParameterValue(spec)

        assertThat(parameter).isEqualTo(grant)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredReferenceParameters_exist() {
        val uri = Uri.parse("content://test")
        val grant = AppFunctionUriGrant(uri, FLAG_GRANT_READ_URI_PERMISSION)
        val testData =
            AppFunctionData.Builder("")
                .setAppFunctionData(
                    "reference",
                    AppFunctionData.serialize(grant, AppFunctionUriGrant::class.java.name),
                )
                .build()
        val spec =
            AppFunctionParameterSpec(
                name = "reference",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_REFERENCE,
                objectQualifiedName = AppFunctionUriGrant::class.java.name,
            )

        val parameter = testData.unsafeGetParameterValue(spec)

        assertThat(parameter).isEqualTo(grant)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_notRequiredSingleParameters_exist(
        @TestParameter isNullable: Boolean
    ) {
        val spec =
            AppFunctionParameterSpec(
                name = "boolean",
                isRequired = false,
                isNullable = isNullable,
                type = AppFunctionDataTypeMetadata.TYPE_BOOLEAN,
            )

        val parameter = testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)

        assertThat(parameter).isEqualTo(true)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredNonNullSingleParameters_notExist() {
        val spec =
            AppFunctionParameterSpec(
                name = "fakeDouble",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_DOUBLE,
            )

        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)
        }
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredNullableSingleParameters_notExist() {
        val spec =
            AppFunctionParameterSpec(
                name = "fakeDouble",
                isRequired = true,
                isNullable = true,
                type = AppFunctionDataTypeMetadata.TYPE_DOUBLE,
            )
        val testData = AppFunctionData.Builder("").build()

        val value = testData.unsafeGetParameterValue(spec)

        assertThat(value).isNull()
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_notRequiredNonNullSingleParameters_notExist() {
        val spec =
            AppFunctionParameterSpec(
                name = "fakeInt",
                isRequired = false,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_INT,
            )
        val testData = AppFunctionData.Builder("").build()

        val parameter = testData.unsafeGetParameterValue(spec)

        assertThat(parameter).isEqualTo(0)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_notRequiredNullableSingleParameters_notExist() {
        val spec =
            AppFunctionParameterSpec(
                name = "fakeInt",
                isRequired = false,
                isNullable = true,
                type = AppFunctionDataTypeMetadata.TYPE_INT,
            )
        val testData = AppFunctionData.Builder("").build()

        val parameter = testData.unsafeGetParameterValue(spec)

        assertThat(parameter).isNull()
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_singleParameters_wrongType(
        @TestParameter isRequired: Boolean,
        @TestParameter isNullable: Boolean,
    ) {
        val spec =
            AppFunctionParameterSpec(
                name = "boolean",
                isRequired = isRequired,
                isNullable = isNullable,
                type = AppFunctionDataTypeMetadata.TYPE_STRING,
            )

        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)
        }
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_unknownType_throws() {
        val spec =
            AppFunctionParameterSpec(
                name = "unknown",
                isRequired = true,
                isNullable = false,
                type = -1,
            )
        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)
        }
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredCollectionParameters_exist(
        @TestParameter isNullable: Boolean
    ) {
        val spec =
            AppFunctionParameterSpec(
                name = "longArray",
                isRequired = true,
                isNullable = isNullable,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_LONG,
            )

        val parameter = testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)

        assertThat(parameter).isInstanceOf(LongArray::class.java)
        assertThat(parameter as LongArray).asList().containsExactly(1L, 2L, 3L)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredIntArray_exists() {
        val spec =
            AppFunctionParameterSpec(
                name = "intArray",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_INT,
            )
        val extracted = testAppFunctionDataFromSpec.unsafeGetParameterValue(spec) as IntArray
        assertThat(extracted.toList()).containsExactly(1, 2, 3)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredFloatArray_exists() {
        val spec =
            AppFunctionParameterSpec(
                name = "floatArray",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_FLOAT,
            )
        val extracted = testAppFunctionDataFromSpec.unsafeGetParameterValue(spec) as FloatArray
        assertThat(extracted.toList()).containsExactly(1.0f, 2.0f, 3.0f)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredParcelableArray_exists() {
        val spec =
            AppFunctionParameterSpec(
                name = "parcelableList",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_PARCELABLE,
                itemQualifiedName = Intent::class.java.name,
            )
        val extracted = testAppFunctionDataFromSpec.unsafeGetParameterValue(spec) as List<*>
        assertThat(extracted).hasSize(1)
        assertThat((extracted[0] as Intent).action).isEqualTo("test")
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredObjectArrayParameters_exist() {
        val uri = Uri.parse("content://test")
        val grant = AppFunctionUriGrant(uri, FLAG_GRANT_READ_URI_PERMISSION)
        val testData =
            AppFunctionData.Builder("")
                .setAppFunctionDataList(
                    "objectList",
                    listOf(AppFunctionData.serialize(grant, AppFunctionUriGrant::class.java.name)),
                )
                .build()
        val spec =
            AppFunctionParameterSpec(
                name = "objectList",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_OBJECT,
                itemQualifiedName = AppFunctionUriGrant::class.java.name,
            )

        val parameter = testData.unsafeGetParameterValue(spec)

        assertThat(parameter).isInstanceOf(List::class.java)
        assertThat(parameter as List<*>).containsExactly(grant)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredReferenceArrayParameters_exist() {
        val uri = Uri.parse("content://test")
        val grant = AppFunctionUriGrant(uri, FLAG_GRANT_READ_URI_PERMISSION)
        val testData =
            AppFunctionData.Builder("")
                .setAppFunctionDataList(
                    "referenceList",
                    listOf(AppFunctionData.serialize(grant, AppFunctionUriGrant::class.java.name)),
                )
                .build()
        val spec =
            AppFunctionParameterSpec(
                name = "referenceList",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_REFERENCE,
                itemQualifiedName = AppFunctionUriGrant::class.java.name,
            )

        val parameter = testData.unsafeGetParameterValue(spec)

        assertThat(parameter).isInstanceOf(List::class.java)
        assertThat(parameter as List<*>).containsExactly(grant)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_notRequiredCollectionParameters_exist(
        @TestParameter isNullable: Boolean
    ) {
        val spec =
            AppFunctionParameterSpec(
                name = "booleanArray",
                isRequired = false,
                isNullable = isNullable,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_BOOLEAN,
            )

        val parameter = testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)

        assertThat(parameter).isInstanceOf(BooleanArray::class.java)
        assertThat(parameter as BooleanArray).asList().containsExactly(false, true, false)
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredNonNullCollectionParameters_notExist() {
        val spec =
            AppFunctionParameterSpec(
                name = "fakeDoubleArray",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_DOUBLE,
            )

        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)
        }
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_requiredNullableCollectionParameters_notExist() {
        val spec =
            AppFunctionParameterSpec(
                name = "fakeDoubleArray",
                isRequired = true,
                isNullable = true,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_DOUBLE,
            )
        val testData = AppFunctionData.Builder("").build()

        val value = testData.unsafeGetParameterValue(spec)

        assertThat(value).isNull()
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_notRequiredNonNullCollectionParameters_notExist() {
        val spec =
            AppFunctionParameterSpec(
                name = "fakeStringList",
                isRequired = false,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_STRING,
            )
        val testData = AppFunctionData.Builder("").build()

        val parameter = testData.unsafeGetParameterValue(spec)

        assertThat(parameter).isEqualTo(emptyList<String>())
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_notRequiredNullableCollectionParameters_notExist() {
        val spec =
            AppFunctionParameterSpec(
                name = "fakeStringList",
                isRequired = false,
                isNullable = true,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_STRING,
            )
        val testData = AppFunctionData.Builder("").build()

        val parameter = testData.unsafeGetParameterValue(spec)

        assertThat(parameter).isNull()
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_collectionParameters_wrongType(
        @TestParameter isRequired: Boolean,
        @TestParameter isNullable: Boolean,
    ) {
        val spec =
            AppFunctionParameterSpec(
                name = "booleanArray",
                isRequired = isRequired,
                isNullable = isNullable,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_STRING,
            )

        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)
        }
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_bytesArray_throws() {
        val spec =
            AppFunctionParameterSpec(
                name = "bytesArray",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_BYTES,
            )
        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)
        }
    }

    @Test
    fun testUnsafeGetParameterValueFromSpec_unknownArrayItemType_throws() {
        val spec =
            AppFunctionParameterSpec(
                name = "unknownArray",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = -1,
            )
        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            testAppFunctionDataFromSpec.unsafeGetParameterValue(spec)
        }
    }

    @Test
    fun testAppFunctionParameterSpec_invalidObjectMissingQualifiedName_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            AppFunctionParameterSpec(
                name = "test",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_OBJECT,
            )
        }
    }

    @Test
    fun testAppFunctionParameterSpec_invalidReferenceMissingQualifiedName_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            AppFunctionParameterSpec(
                name = "test",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_REFERENCE,
            )
        }
    }

    @Test
    fun testAppFunctionParameterSpec_invalidArrayMissingItemType_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            AppFunctionParameterSpec(
                name = "test",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
            )
        }
    }

    @Test
    fun testAppFunctionParameterSpec_invalidObjectArrayMissingItemQualifiedName_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            AppFunctionParameterSpec(
                name = "test",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_OBJECT,
            )
        }
    }

    @Test
    fun testAppFunctionParameterSpec_invalidReferenceArrayMissingItemQualifiedName_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            AppFunctionParameterSpec(
                name = "test",
                isRequired = true,
                isNullable = false,
                type = AppFunctionDataTypeMetadata.TYPE_ARRAY,
                itemType = AppFunctionDataTypeMetadata.TYPE_REFERENCE,
            )
        }
    }
}
