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

package androidx.appfunctions

import android.app.PendingIntent
import android.app.appsearch.GenericDocument
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.os.ext.SdkExtensions
import androidx.appfunctions.Attachment.Companion.ATTACHMENT_OBJECT_TYPE_METADATA
import androidx.appfunctions.Note.Companion.NOTE_OBJECT_TYPE_METADATA
import androidx.appfunctions.internal.AppFunctionUriGrantTestInventory.Companion.TEST_APP_FUNCTION_URI_GRANT_HOLDER_OBJECT_METADATA
import androidx.appfunctions.internal.AppFunctionUriGrantTestInventory.Companion.TEST_COMPONENT_METADATA
import androidx.appfunctions.internal.AppFunctionUriGrantTestInventory.Companion.TEST_NESTED_APP_FUNCTION_URI_GRANT_OBJECT_METADATA
import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBytesTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDoubleTypeMetadata
import androidx.appfunctions.metadata.AppFunctionFloatTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPendingIntentTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class AppFunctionDataTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testBuild_missingRequiredNullableParameter_succeed() {
        val parameterMetadata =
            listOf(
                AppFunctionParameterMetadata(
                    name = "requiredNullableString",
                    isRequired = true,
                    dataType = AppFunctionStringTypeMetadata(isNullable = true),
                )
            )

        AppFunctionData.Builder(parameterMetadata, AppFunctionComponentsMetadata()).build()
    }

    @Test
    fun testBuild_missingRequiredNullableObjectFields_succeed() {
        val objectMetadata =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "requiredNullableDouble" to AppFunctionDoubleTypeMetadata(isNullable = true)
                    ),
                required = listOf("requiredNullableDouble"),
                qualifiedName = "testObject",
                isNullable = false,
            )

        AppFunctionData.Builder(objectMetadata, AppFunctionComponentsMetadata()).build()
    }

    @Test
    fun testBuild_missingRequiredNullableAllOfObjectFields_succeed() {
        val allOfTypeMetadata =
            AppFunctionAllOfTypeMetadata(
                matchAll =
                    listOf(
                        AppFunctionObjectTypeMetadata(
                            properties =
                                mapOf(
                                    "requiredNullableStringList" to
                                        AppFunctionArrayTypeMetadata(
                                            itemType =
                                                AppFunctionStringTypeMetadata(isNullable = false),
                                            isNullable = true,
                                        )
                                ),
                            required = listOf("requiredNullableStringList"),
                            qualifiedName = "testObject",
                            isNullable = false,
                        )
                    ),
                qualifiedName = "testAllOf",
                isNullable = false,
            )

        AppFunctionData.Builder(allOfTypeMetadata, AppFunctionComponentsMetadata()).build()
    }

    @Test
    fun testBuild_missingRequiredNullableResponseFields_succeed() {
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType =
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf(
                                "unimportantField" to
                                    AppFunctionStringTypeMetadata(isNullable = false)
                            ),
                        required = listOf("unimportantField"),
                        isNullable = true,
                        qualifiedName = "testObject",
                    )
            )

        AppFunctionData.Builder(responseMetadata, AppFunctionComponentsMetadata()).build()
    }

    @Test
    fun testBuild_missingRequiredFields_throwsException() {
        val parameterMetadata =
            listOf(
                AppFunctionParameterMetadata(
                    name = "requiredInt",
                    isRequired = true,
                    dataType = AppFunctionIntTypeMetadata(isNullable = false),
                ),
                AppFunctionParameterMetadata(
                    name = "optionalString",
                    isRequired = false,
                    dataType = AppFunctionStringTypeMetadata(isNullable = true),
                ),
            )
        val builder = AppFunctionData.Builder(parameterMetadata, AppFunctionComponentsMetadata())

        // Attempt to build without setting the required "requiredInt" field
        assertFailsWith<IllegalArgumentException> { builder.build() }

        // Set the optional field but still miss the required one
        builder.setString("optionalString", "test")
        assertFailsWith<IllegalArgumentException> { builder.build() }

        // Set the required field, now build should succeed
        builder.setInt("requiredInt", 123)
        // No exception should be thrown here
        builder.build()
    }

    @Test
    fun testBuild_missingRequiredPendingIntent_throwsException() {
        val parameterMetadata =
            listOf(
                AppFunctionParameterMetadata(
                    name = "requiredPi",
                    isRequired = true,
                    dataType = AppFunctionPendingIntentTypeMetadata(isNullable = false),
                ),
                AppFunctionParameterMetadata(
                    name = "optionalString",
                    isRequired = false,
                    dataType = AppFunctionStringTypeMetadata(isNullable = true),
                ),
            )
        val builder = AppFunctionData.Builder(parameterMetadata, AppFunctionComponentsMetadata())

        // Attempt to build without setting the required "requiredPi" field
        assertFailsWith<IllegalArgumentException> { builder.build() }

        // Set the optional field but still miss the required PendingIntent
        builder.setString("optionalString", "test")
        assertFailsWith<IllegalArgumentException> { builder.build() }

        // Set the required PendingIntent field, now build should succeed
        val dummyPendingIntent =
            PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        builder.setPendingIntent("requiredPi", dummyPendingIntent)
        // No exception should be thrown here
        builder.build()
    }

    @Test
    fun testBuild_missingRequiredPendingIntentList_throwsException() {
        val parameterMetadata =
            listOf(
                AppFunctionParameterMetadata(
                    name = "requiredPiList",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType = AppFunctionPendingIntentTypeMetadata(isNullable = false),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "optionalInt",
                    isRequired = false,
                    dataType = AppFunctionIntTypeMetadata(isNullable = true),
                ),
            )
        val builder = AppFunctionData.Builder(parameterMetadata, AppFunctionComponentsMetadata())

        // Attempt to build without setting the required "requiredPiList" field
        assertFailsWith<IllegalArgumentException> { builder.build() }

        // Set the optional field but still miss the required PendingIntent list
        builder.setInt("optionalInt", 42)
        assertFailsWith<IllegalArgumentException> { builder.build() }

        // Set the required PendingIntent list field, now build should succeed
        val dummyPendingIntent =
            PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        builder.setPendingIntentList("requiredPiList", listOf(dummyPendingIntent))
        // No exception should be thrown here
        builder.build()
    }

    @Test
    fun testReadWrite_asParameters_conformSpec() {
        val builder =
            AppFunctionData.Builder(TEST_PARAMETER_METADATA, AppFunctionComponentsMetadata())

        builder.setInt("int", 234)
        builder.setLong("long", 123L)
        builder.setFloat("float", 100.0f)
        builder.setDouble("double", 50.0)
        builder.setBoolean("boolean", true)
        builder.setString("string", "testString")
        builder.setPendingIntent(
            "pendingIntent",
            PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
        )
        builder.setIntArray("intArray", intArrayOf(4, 5, 6))
        builder.setLongArray("longArray", longArrayOf(1L, 2L, 3L))
        builder.setFloatArray("floatArray", floatArrayOf(10.0f, 20.0f, 30.0f))
        builder.setDoubleArray("doubleArray", doubleArrayOf(4.0, 5.0, 6.0))
        builder.setBooleanArray("booleanArray", booleanArrayOf(false, true, false))
        builder.setByteArray("byteArray", byteArrayOf(10.toByte(), 20.toByte()))
        builder.setStringList("stringList", listOf("1", "2", "3"))
        builder.setPendingIntentList(
            "pendingIntentList",
            listOf(
                PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
                PendingIntent.getService(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
            ),
        )
        val data = builder.build()

        assertThat(data.getInt("int")).isEqualTo(234)
        assertThat(data.getLong("long")).isEqualTo(123L)
        assertThat(data.getFloat("float")).isEqualTo(100.0f)
        assertThat(data.getDouble("double")).isEqualTo(50.0)
        assertThat(data.getBoolean("boolean")).isTrue()
        assertThat(data.getString("string")).isEqualTo("testString")
        assertThat(data.getPendingIntent("pendingIntent"))
            .isEqualTo(
                PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
            )
        assertThat(data.getIntArray("intArray")).asList().containsExactly(4, 5, 6)
        assertThat(data.getLongArray("longArray")).asList().containsExactly(1L, 2L, 3L)
        assertThat(data.getFloatArray("floatArray"))
            .usingExactEquality()
            .containsExactly(10.0f, 20.0f, 30.0f)
        assertThat(data.getDoubleArray("doubleArray"))
            .usingExactEquality()
            .containsExactly(4.0, 5.0, 6.0)
        assertThat(data.getBooleanArray("booleanArray"))
            .asList()
            .containsExactly(false, true, false)
        assertThat(data.getByteArray("byteArray"))
            .asList()
            .containsExactly(10.toByte(), 20.toByte())
        assertThat(data.getStringList("stringList")).containsExactly("1", "2", "3")
        assertThat(data.getPendingIntentList("pendingIntentList"))
            .containsExactly(
                PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
                PendingIntent.getService(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
            )
    }

    @Test
    fun testWrite_asParameters_notConformSpec() {
        val builder =
            AppFunctionData.Builder(TEST_PARAMETER_METADATA, AppFunctionComponentsMetadata())

        assertFailsWith(IllegalArgumentException::class) {
            builder.setIntArray("int", intArrayOf(100, 200))
        }
        assertFailsWith(IllegalArgumentException::class) { builder.setLong("int", 50) }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setLongArray("long", longArrayOf(100, 200))
        }
        assertFailsWith(IllegalArgumentException::class) { builder.setDouble("long", 50.0) }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setFloatArray("float", floatArrayOf(50.0f, 100.0f))
        }
        assertFailsWith(IllegalArgumentException::class) { builder.setDouble("float", 20.0) }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setDoubleArray("double", doubleArrayOf(50.0, 100.0))
        }
        assertFailsWith(IllegalArgumentException::class) { builder.setBoolean("double", true) }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setBooleanArray("boolean", booleanArrayOf(false, true))
        }
        assertFailsWith(IllegalArgumentException::class) { builder.setLong("boolean", 100) }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setStringList("string", listOf("test"))
        }
        assertFailsWith(IllegalArgumentException::class) { builder.setDouble("string", 100.0) }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setPendingIntentList(
                "pendingIntent",
                listOf(
                    PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
                ),
            )
        }
        assertFailsWith(IllegalArgumentException::class) {
            builder.setString("pendingIntent", "test")
        }

        assertFailsWith(IllegalArgumentException::class) { builder.setInt("intArray", 100) }
        assertFailsWith(IllegalArgumentException::class) {
            builder.setLongArray("intArray", longArrayOf(2, 3))
        }

        assertFailsWith(IllegalArgumentException::class) { builder.setLong("longArray", 100L) }
        assertFailsWith(IllegalArgumentException::class) {
            builder.setDoubleArray("longArray", doubleArrayOf(2.0))
        }

        assertFailsWith(IllegalArgumentException::class) { builder.setDouble("floatArray", 1.0) }
        assertFailsWith(IllegalArgumentException::class) {
            builder.setDoubleArray("floatArray", doubleArrayOf(1.0))
        }

        assertFailsWith(IllegalArgumentException::class) { builder.setDouble("doubleArray", 1.0) }
        assertFailsWith(IllegalArgumentException::class) {
            builder.setBooleanArray("doubleArray", booleanArrayOf(false))
        }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setBoolean("booleanArray", false)
        }
        assertFailsWith(IllegalArgumentException::class) {
            builder.setStringList("booleanArray", listOf("test1"))
        }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setBooleanArray("byteArray", booleanArrayOf(false, true))
        }
        assertFailsWith(IllegalArgumentException::class) { builder.setInt("byteArray", 1) }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setString("stringList", "test1")
        }
        assertFailsWith(IllegalArgumentException::class) {
            builder.setLongArray("stringList", longArrayOf(1))
        }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setPendingIntent(
                "pendingIntentList",
                PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
            )
        }
        assertFailsWith(IllegalArgumentException::class) {
            builder.setStringList("pendingIntentList", listOf("string"))
        }
    }

    @Test
    fun testRead_asParameters_notConformSpec() {
        val builder =
            AppFunctionData.Builder(TEST_PARAMETER_METADATA, AppFunctionComponentsMetadata())
        builder.setInt("int", 234)
        builder.setLong("long", 123L)
        builder.setFloat("float", 100.0f)
        builder.setDouble("double", 50.0)
        builder.setBoolean("boolean", true)
        builder.setString("string", "testString")
        builder.setPendingIntent(
            "pendingIntent",
            PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
        )
        builder.setIntArray("intArray", intArrayOf(4, 5, 6))
        builder.setLongArray("longArray", longArrayOf(1L, 2L, 3L))
        builder.setFloatArray("floatArray", floatArrayOf(10.0f, 20.0f, 30.0f))
        builder.setDoubleArray("doubleArray", doubleArrayOf(4.0, 5.0, 6.0))
        builder.setBooleanArray("booleanArray", booleanArrayOf(false, true, false))
        builder.setByteArray("byteArray", byteArrayOf(10.toByte(), 20.toByte()))
        builder.setStringList("stringList", listOf("1", "2", "3"))
        builder.setPendingIntentList(
            "pendingIntentList",
            listOf(PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)),
        )
        val data = builder.build()

        assertFailsWith(IllegalArgumentException::class) { data.getLong("int") }
        assertFailsWith(IllegalArgumentException::class) { data.getIntArray("int") }

        assertFailsWith(IllegalArgumentException::class) { data.getDouble("long") }
        assertFailsWith(IllegalArgumentException::class) { data.getLongArray("long") }

        assertFailsWith(IllegalArgumentException::class) { data.getDouble("float") }
        assertFailsWith(IllegalArgumentException::class) { data.getFloatArray("float") }

        assertFailsWith(IllegalArgumentException::class) { data.getBoolean("double") }
        assertFailsWith(IllegalArgumentException::class) { data.getDoubleArray("double") }

        assertFailsWith(IllegalArgumentException::class) { data.getString("boolean") }
        assertFailsWith(IllegalArgumentException::class) { data.getBooleanArray("boolean") }

        assertFailsWith(IllegalArgumentException::class) { data.getLong("string") }
        assertFailsWith(IllegalArgumentException::class) { data.getStringList("string") }

        assertFailsWith(IllegalArgumentException::class) { data.getString("pendingIntent") }
        assertFailsWith(IllegalArgumentException::class) {
            data.getPendingIntentList("pendingIntent")
        }

        assertFailsWith(IllegalArgumentException::class) { data.getLongArray("intArray") }
        assertFailsWith(IllegalArgumentException::class) { data.getInt("intArray") }

        assertFailsWith(IllegalArgumentException::class) { data.getDoubleArray("longArray") }
        assertFailsWith(IllegalArgumentException::class) { data.getLong("longArray") }

        assertFailsWith(IllegalArgumentException::class) { data.getDoubleArray("floatArray") }
        assertFailsWith(IllegalArgumentException::class) { data.getFloat("floatArray") }

        assertFailsWith(IllegalArgumentException::class) { data.getBooleanArray("doubleArray") }
        assertFailsWith(IllegalArgumentException::class) { data.getDouble("doubleArray") }

        assertFailsWith(IllegalArgumentException::class) { data.getStringList("booleanArray") }
        assertFailsWith(IllegalArgumentException::class) { data.getBoolean("booleanArray") }

        assertFailsWith(IllegalArgumentException::class) { data.getLongArray("byteArray") }
        assertFailsWith(IllegalArgumentException::class) { data.getBoolean("byteArray") }

        assertFailsWith(IllegalArgumentException::class) { data.getLongArray("stringList") }
        assertFailsWith(IllegalArgumentException::class) { data.getString("stringList") }

        assertFailsWith(IllegalArgumentException::class) { data.getStringList("pendingIntentList") }
        assertFailsWith(IllegalArgumentException::class) {
            data.getPendingIntent("pendingIntentList")
        }
    }

    @Test
    fun testReadWrite_asObject_conformSpec() {
        val builder = AppFunctionData.Builder(TEST_OBJECT_METADATA, AppFunctionComponentsMetadata())
        val testBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val testGameCharacter = GameCharacter(name = "Test", level = 10, characterClass = "test")

        builder.setInt("int", 234)
        builder.setLong("long", 123L)
        builder.setFloat("float", 100.0f)
        builder.setDouble("double", 50.0)
        builder.setBoolean("boolean", true)
        builder.setString("string", "testString")
        builder.setPendingIntent(
            "pendingIntent",
            PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
        )
        builder.setParcelable("bitmap", testBitmap)
        builder.setParcelable("customParcelable", testGameCharacter)
        builder.setIntArray("intArray", intArrayOf(4, 5, 6))
        builder.setLongArray("longArray", longArrayOf(1L, 2L, 3L))
        builder.setFloatArray("floatArray", floatArrayOf(10.0f, 20.0f, 30.0f))
        builder.setDoubleArray("doubleArray", doubleArrayOf(4.0, 5.0, 6.0))
        builder.setBooleanArray("booleanArray", booleanArrayOf(false, true, false))
        builder.setByteArray("byteArray", byteArrayOf(10.toByte(), 20.toByte()))
        builder.setStringList("stringList", listOf("1", "2", "3"))
        builder.setPendingIntentList(
            "pendingIntentList",
            listOf(
                PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
                PendingIntent.getService(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
            ),
        )
        builder.setParcelableList("bitmapList", listOf(testBitmap))
        builder.setParcelableList("customParcelableList", listOf(testGameCharacter))
        val data = builder.build()

        assertThat(data.getInt("int")).isEqualTo(234)
        assertThat(data.getLong("long")).isEqualTo(123L)
        assertThat(data.getFloat("float")).isEqualTo(100.0f)
        assertThat(data.getDouble("double")).isEqualTo(50.0)
        assertThat(data.getBoolean("boolean")).isTrue()
        assertThat(data.getString("string")).isEqualTo("testString")
        assertThat(data.getPendingIntent("pendingIntent"))
            .isEqualTo(
                PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
            )
        assertThat(data.getParcelable("bitmap", Bitmap::class.java)).isEqualTo(testBitmap)
        assertThat(data.getParcelable("customParcelable", GameCharacter::class.java))
            .isEqualTo(testGameCharacter)
        assertThat(data.getIntArray("intArray")).asList().containsExactly(4, 5, 6)
        assertThat(data.getLongArray("longArray")).asList().containsExactly(1L, 2L, 3L)
        assertThat(data.getFloatArray("floatArray"))
            .usingExactEquality()
            .containsExactly(10.0f, 20.0f, 30.0f)
        assertThat(data.getDoubleArray("doubleArray"))
            .usingExactEquality()
            .containsExactly(4.0, 5.0, 6.0)
        assertThat(data.getBooleanArray("booleanArray"))
            .asList()
            .containsExactly(false, true, false)
        assertThat(data.getByteArray("byteArray"))
            .asList()
            .containsExactly(10.toByte(), 20.toByte())
        assertThat(data.getStringList("stringList")).containsExactly("1", "2", "3")
        assertThat(data.getPendingIntentList("pendingIntentList"))
            .containsExactly(
                PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
                PendingIntent.getService(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
            )
        assertThat(data.getParcelableList("bitmapList", Bitmap::class.java))
            .containsExactly(testBitmap)
        assertThat(data.getParcelableList("customParcelableList", GameCharacter::class.java))
            .containsExactly(testGameCharacter)
    }

    @Test
    fun testWrite_intEnumValue_conformanceFailsForInvalidValues() {
        val afdBuilder =
            AppFunctionData.Builder(
                parameterMetadataList =
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "intEnum",
                            isRequired = false,
                            dataType =
                                AppFunctionIntTypeMetadata(
                                    isNullable = false,
                                    enumValues = setOf(1, 2),
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "intEnumArray",
                            isRequired = false,
                            dataType =
                                AppFunctionArrayTypeMetadata(
                                    isNullable = false,
                                    itemType =
                                        AppFunctionIntTypeMetadata(
                                            isNullable = false,
                                            enumValues = setOf(1, 2),
                                        ),
                                ),
                        ),
                    ),
                componentMetadata = AppFunctionComponentsMetadata(),
            )

        assertFailsWith<IllegalArgumentException> { afdBuilder.setInt("intEnum", 190) }
        assertFailsWith<IllegalArgumentException> {
            afdBuilder.setIntArray("intEnumArray", intArrayOf(1, 2, 190))
        }
    }

    @Test
    fun testReadWrite_intEnumValues_conformanceSuccess() {
        val afdBuilder =
            AppFunctionData.Builder(
                parameterMetadataList =
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "intEnum",
                            isRequired = false,
                            dataType =
                                AppFunctionIntTypeMetadata(
                                    isNullable = false,
                                    enumValues = setOf(1, 2),
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "intEnumArray",
                            isRequired = false,
                            dataType =
                                AppFunctionArrayTypeMetadata(
                                    isNullable = false,
                                    itemType =
                                        AppFunctionIntTypeMetadata(
                                            isNullable = false,
                                            enumValues = setOf(1, 2),
                                        ),
                                ),
                        ),
                    ),
                componentMetadata = AppFunctionComponentsMetadata(),
            )

        afdBuilder.setInt("intEnum", 2)
        afdBuilder.setIntArray("intEnumArray", intArrayOf(1, 2))
        val afd = afdBuilder.build()

        assertThat(afd.getInt("intEnum")).isEqualTo(2)
        assertThat(afd.getIntArray("intEnumArray")?.asList()).containsExactly(1, 2)
    }

    @Test
    fun testWrite_stringEnumValue_conformanceFailsForInvalidValues() {
        val afdBuilder =
            AppFunctionData.Builder(
                parameterMetadataList =
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "stringEnum",
                            isRequired = false,
                            dataType =
                                AppFunctionStringTypeMetadata(
                                    isNullable = false,
                                    enumValues = setOf("A", "B"),
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "stringEnumArray",
                            isRequired = false,
                            dataType =
                                AppFunctionArrayTypeMetadata(
                                    isNullable = false,
                                    itemType =
                                        AppFunctionStringTypeMetadata(
                                            isNullable = false,
                                            enumValues = setOf("A", "B"),
                                        ),
                                ),
                        ),
                    ),
                componentMetadata = AppFunctionComponentsMetadata(),
            )

        assertFailsWith<IllegalArgumentException> { afdBuilder.setString("stringEnum", "C") }
        assertFailsWith<IllegalArgumentException> {
            afdBuilder.setStringList("stringEnumArray", listOf("A", "B", "C"))
        }
    }

    @Test
    fun testReadWrite_stringEnumValues_conformanceSuccess() {
        val afdBuilder =
            AppFunctionData.Builder(
                parameterMetadataList =
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "stringEnum",
                            isRequired = false,
                            dataType =
                                AppFunctionStringTypeMetadata(
                                    isNullable = false,
                                    enumValues = setOf("A", "B"),
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "stringEnumList",
                            isRequired = false,
                            dataType =
                                AppFunctionArrayTypeMetadata(
                                    isNullable = false,
                                    itemType =
                                        AppFunctionStringTypeMetadata(
                                            isNullable = false,
                                            enumValues = setOf("A", "B"),
                                        ),
                                ),
                        ),
                    ),
                componentMetadata = AppFunctionComponentsMetadata(),
            )

        afdBuilder.setString("stringEnum", "A")
        afdBuilder.setStringList("stringEnumList", listOf("A", "B"))
        val afd = afdBuilder.build()

        assertThat(afd.getString("stringEnum")).isEqualTo("A")
        assertThat(afd.getStringList("stringEnumList")).containsExactly("A", "B")
    }

    @Test
    fun testWrite_asObject_notConformSpec() {
        val builder = AppFunctionData.Builder(TEST_OBJECT_METADATA, AppFunctionComponentsMetadata())

        assertFailsWith(IllegalArgumentException::class) {
            builder.setIntArray("int", intArrayOf(100, 200))
        }
        assertFailsWith(IllegalArgumentException::class) { builder.setLong("int", 50) }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setLongArray("long", longArrayOf(100, 200))
        }
        assertFailsWith(IllegalArgumentException::class) { builder.setDouble("long", 50.0) }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setFloatArray("float", floatArrayOf(50.0f, 100.0f))
        }
        assertFailsWith(IllegalArgumentException::class) { builder.setDouble("float", 20.0) }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setDoubleArray("double", doubleArrayOf(50.0, 100.0))
        }
        assertFailsWith(IllegalArgumentException::class) { builder.setBoolean("double", true) }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setBooleanArray("boolean", booleanArrayOf(false, true))
        }
        assertFailsWith(IllegalArgumentException::class) { builder.setLong("boolean", 100) }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setStringList("string", listOf("test"))
        }
        assertFailsWith(IllegalArgumentException::class) { builder.setDouble("string", 100.0) }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setPendingIntentList(
                "pendingIntent",
                listOf(
                    PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
                ),
            )
        }
        assertFailsWith(IllegalArgumentException::class) {
            builder.setString("pendingIntent", "test")
        }

        assertFailsWith(IllegalArgumentException::class) { builder.setInt("intArray", 100) }
        assertFailsWith(IllegalArgumentException::class) {
            builder.setLongArray("intArray", longArrayOf(2, 3))
        }

        assertFailsWith(IllegalArgumentException::class) { builder.setLong("longArray", 100L) }
        assertFailsWith(IllegalArgumentException::class) {
            builder.setDoubleArray("longArray", doubleArrayOf(2.0))
        }

        assertFailsWith(IllegalArgumentException::class) { builder.setDouble("floatArray", 1.0) }
        assertFailsWith(IllegalArgumentException::class) {
            builder.setDoubleArray("floatArray", doubleArrayOf(1.0))
        }

        assertFailsWith(IllegalArgumentException::class) { builder.setDouble("doubleArray", 1.0) }
        assertFailsWith(IllegalArgumentException::class) {
            builder.setBooleanArray("doubleArray", booleanArrayOf(false))
        }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setBoolean("booleanArray", false)
        }
        assertFailsWith(IllegalArgumentException::class) {
            builder.setStringList("booleanArray", listOf("test1"))
        }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setBooleanArray("byteArray", booleanArrayOf(false, true))
        }
        assertFailsWith(IllegalArgumentException::class) { builder.setInt("byteArray", 1) }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setString("stringList", "test1")
        }
        assertFailsWith(IllegalArgumentException::class) {
            builder.setLongArray("stringList", longArrayOf(1))
        }

        assertFailsWith(IllegalArgumentException::class) {
            builder.setPendingIntent(
                "pendingIntentList",
                PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
            )
        }
        assertFailsWith(IllegalArgumentException::class) {
            builder.setStringList("pendingIntentList", listOf("string"))
        }
    }

    @Test
    fun testRead_asObject_notConformSpec() {
        val builder = AppFunctionData.Builder(TEST_OBJECT_METADATA, AppFunctionComponentsMetadata())
        builder.setInt("int", 234)
        builder.setLong("long", 123L)
        builder.setFloat("float", 100.0f)
        builder.setDouble("double", 50.0)
        builder.setBoolean("boolean", true)
        builder.setString("string", "testString")
        builder.setPendingIntent(
            "pendingIntent",
            PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
        )
        builder.setIntArray("intArray", intArrayOf(4, 5, 6))
        builder.setLongArray("longArray", longArrayOf(1L, 2L, 3L))
        builder.setFloatArray("floatArray", floatArrayOf(10.0f, 20.0f, 30.0f))
        builder.setDoubleArray("doubleArray", doubleArrayOf(4.0, 5.0, 6.0))
        builder.setBooleanArray("booleanArray", booleanArrayOf(false, true, false))
        builder.setByteArray("byteArray", byteArrayOf(10.toByte(), 20.toByte()))
        builder.setStringList("stringList", listOf("1", "2", "3"))
        builder.setPendingIntentList(
            "pendingIntentList",
            listOf(PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)),
        )
        val data = builder.build()

        assertFailsWith(IllegalArgumentException::class) { data.getLong("int") }
        assertFailsWith(IllegalArgumentException::class) { data.getIntArray("int") }

        assertFailsWith(IllegalArgumentException::class) { data.getDouble("long") }
        assertFailsWith(IllegalArgumentException::class) { data.getLongArray("long") }

        assertFailsWith(IllegalArgumentException::class) { data.getDouble("float") }
        assertFailsWith(IllegalArgumentException::class) { data.getFloatArray("float") }

        assertFailsWith(IllegalArgumentException::class) { data.getBoolean("double") }
        assertFailsWith(IllegalArgumentException::class) { data.getDoubleArray("double") }

        assertFailsWith(IllegalArgumentException::class) { data.getString("boolean") }
        assertFailsWith(IllegalArgumentException::class) { data.getBooleanArray("boolean") }

        assertFailsWith(IllegalArgumentException::class) { data.getLong("string") }
        assertFailsWith(IllegalArgumentException::class) { data.getStringList("string") }

        assertFailsWith(IllegalArgumentException::class) { data.getString("pendingIntent") }
        assertFailsWith(IllegalArgumentException::class) {
            data.getPendingIntentList("pendingIntent")
        }

        assertFailsWith(IllegalArgumentException::class) { data.getLongArray("intArray") }
        assertFailsWith(IllegalArgumentException::class) { data.getInt("intArray") }

        assertFailsWith(IllegalArgumentException::class) { data.getDoubleArray("longArray") }
        assertFailsWith(IllegalArgumentException::class) { data.getLong("longArray") }

        assertFailsWith(IllegalArgumentException::class) { data.getDoubleArray("floatArray") }
        assertFailsWith(IllegalArgumentException::class) { data.getFloat("floatArray") }

        assertFailsWith(IllegalArgumentException::class) { data.getBooleanArray("doubleArray") }
        assertFailsWith(IllegalArgumentException::class) { data.getDouble("doubleArray") }

        assertFailsWith(IllegalArgumentException::class) { data.getStringList("booleanArray") }
        assertFailsWith(IllegalArgumentException::class) { data.getBoolean("booleanArray") }

        assertFailsWith(IllegalArgumentException::class) { data.getLongArray("byteArray") }
        assertFailsWith(IllegalArgumentException::class) { data.getBoolean("byteArray") }

        assertFailsWith(IllegalArgumentException::class) { data.getLongArray("stringList") }
        assertFailsWith(IllegalArgumentException::class) { data.getString("stringList") }

        assertFailsWith(IllegalArgumentException::class) { data.getStringList("pendingIntentList") }
        assertFailsWith(IllegalArgumentException::class) {
            data.getPendingIntent("pendingIntentList")
        }
    }

    @Test
    fun testReadWrite_nestedObjectParameter() {
        val data =
            AppFunctionData.Builder(TEST_NESTED_PARAMETER_METADATA, AppFunctionComponentsMetadata())
                .setAppFunctionData(
                    "data",
                    AppFunctionData.Builder(TEST_OBJECT_METADATA, AppFunctionComponentsMetadata())
                        .setLong("long", 100)
                        .build(),
                )
                .setAppFunctionDataList(
                    "dataList",
                    listOf(
                        AppFunctionData.Builder(
                                TEST_OBJECT_METADATA,
                                AppFunctionComponentsMetadata(),
                            )
                            .setDouble("double", 20.0)
                            .build(),
                        AppFunctionData.Builder(
                                TEST_OBJECT_METADATA,
                                AppFunctionComponentsMetadata(),
                            )
                            .setString("string", "testString")
                            .build(),
                    ),
                )
                .build()

        assertThat(data.getAppFunctionData("data")?.getLong("long")).isEqualTo(100)
        assertThat(data.getAppFunctionDataList("dataList")).hasSize(2)
        assertThat(data.getAppFunctionDataList("dataList")?.get(0)?.getDouble("double"))
            .isEqualTo(20.0)
        assertThat(data.getAppFunctionDataList("dataList")?.get(1)?.getString("string"))
            .isEqualTo("testString")
    }

    @Test
    fun getAppFunctionData_arrayWithReferenceType() {
        val personMetadata =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf("firstName" to AppFunctionStringTypeMetadata(isNullable = false)),
                required = listOf(),
                qualifiedName =
                    "com.testdata.anotherDifferentPackage.AnotherDiffPackageSerializable",
                isNullable = true,
                description = "Description for AnotherDiffPackageSerializable",
            )
        val componentMetadata =
            AppFunctionComponentsMetadata(dataTypes = mapOf("Person" to personMetadata))
        val personsMetadata =
            AppFunctionArrayTypeMetadata(
                AppFunctionReferenceTypeMetadata("Person", isNullable = false),
                isNullable = false,
            )

        val data =
            AppFunctionData.Builder(
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "persons",
                            dataType = personsMetadata,
                            isRequired = true,
                        )
                    ),
                    componentMetadata,
                )
                .setAppFunctionDataList(
                    "persons",
                    listOf(
                        AppFunctionData.Builder(
                                objectTypeMetadata = personMetadata,
                                componentMetadata = componentMetadata,
                            )
                            .setString("firstName", "John")
                            .build(),
                        AppFunctionData.Builder(
                                objectTypeMetadata = personMetadata,
                                componentMetadata = componentMetadata,
                            )
                            .setString("firstName", "Mary")
                            .build(),
                    ),
                )
                .build()

        assertThat(data.getAppFunctionDataList("persons")).hasSize(2)
        val persons = data.getAppFunctionDataList("persons")
        assertThat(persons?.get(0)?.getString("firstName")).isEqualTo("John")
        assertThat(persons?.get(1)?.getString("firstName")).isEqualTo("Mary")
    }

    @Test
    fun testReadWrite_nestedAppFunctionData_conformSpec() {
        val innerObjectType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf("innerDouble" to AppFunctionDoubleTypeMetadata(isNullable = false)),
                required = emptyList(),
                qualifiedName = "innerData",
                isNullable = false,
                description = "Inner data description",
            )
        val outerObjectType =
            AppFunctionObjectTypeMetadata(
                properties = mapOf("nestedData" to innerObjectType),
                required = emptyList(),
                qualifiedName = "outerData",
                isNullable = false,
                description = "Outer data description",
            )
        val innerDataBuilder =
            AppFunctionData.Builder(innerObjectType, AppFunctionComponentsMetadata())
        val outerDataBuilder =
            AppFunctionData.Builder(outerObjectType, AppFunctionComponentsMetadata())

        innerDataBuilder.setDouble("innerDouble", 500.0)
        outerDataBuilder.setAppFunctionData("nestedData", innerDataBuilder.build())
        val outerData = outerDataBuilder.build()

        assertThat(outerData.getAppFunctionData("nestedData")?.getDouble("innerDouble"))
            .isEqualTo(500.0)
    }

    @Test
    fun testReadWrite_nestedAllOfType_conformSpec() {
        val innerObjectType =
            AppFunctionAllOfTypeMetadata(
                matchAll =
                    listOf(
                        AppFunctionObjectTypeMetadata(
                            properties =
                                mapOf(
                                    "innerLong" to AppFunctionLongTypeMetadata(isNullable = false)
                                ),
                            required = emptyList(),
                            qualifiedName = "innerLongData",
                            isNullable = false,
                            description = "Inner data description",
                        ),
                        AppFunctionObjectTypeMetadata(
                            properties =
                                mapOf(
                                    "innerDouble" to
                                        AppFunctionDoubleTypeMetadata(isNullable = false)
                                ),
                            required = emptyList(),
                            qualifiedName = "innerDoubleData",
                            isNullable = false,
                            description = "Inner data description",
                        ),
                    ),
                qualifiedName = null,
                isNullable = false,
            )
        val outerObjectType =
            AppFunctionObjectTypeMetadata(
                properties = mapOf("nestedData" to innerObjectType),
                required = emptyList(),
                qualifiedName = "outerData",
                isNullable = false,
                description = "Outer data description",
            )

        val innerDataBuilder =
            AppFunctionData.Builder(innerObjectType, AppFunctionComponentsMetadata())
        val outerDataBuilder =
            AppFunctionData.Builder(outerObjectType, AppFunctionComponentsMetadata())

        innerDataBuilder.setDouble("innerDouble", 500.0)
        innerDataBuilder.setLong("innerLong", 100)
        outerDataBuilder.setAppFunctionData("nestedData", innerDataBuilder.build())
        val outerData = outerDataBuilder.build()

        assertThat(outerData.getAppFunctionData("nestedData")?.getDouble("innerDouble"))
            .isEqualTo(500.0)
        assertThat(outerData.getAppFunctionData("nestedData")?.getLong("innerLong")).isEqualTo(100)
    }

    @Test
    @Ignore("b/447064745: Re-enable when the child object validation is fixed")
    fun testWrite_nestedAppFunctionData_notConformSpec() {
        val innerObjectType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf("innerDouble" to AppFunctionDoubleTypeMetadata(isNullable = false)),
                required = emptyList(),
                qualifiedName = "innerData",
                isNullable = false,
                description = "Inner data description",
            )
        val incorrectInnerObjectType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf("innerDouble" to AppFunctionLongTypeMetadata(isNullable = false)),
                required = emptyList(),
                qualifiedName = "innerData",
                isNullable = false,
                description = "Incorrect inner data description",
            )
        val outerObjectType =
            AppFunctionObjectTypeMetadata(
                properties = mapOf("nestedData" to innerObjectType),
                required = emptyList(),
                qualifiedName = "outerData",
                isNullable = false,
                description = "Outer data description",
            )
        val incorrectInnerDataBuilder =
            AppFunctionData.Builder(incorrectInnerObjectType, AppFunctionComponentsMetadata())
        val outerDataBuilder =
            AppFunctionData.Builder(outerObjectType, AppFunctionComponentsMetadata())

        incorrectInnerDataBuilder.setLong("innerDouble", 500)
        assertFailsWith(IllegalArgumentException::class) {
            outerDataBuilder.setAppFunctionData("nestedData", incorrectInnerDataBuilder.build())
        }
    }

    @Test
    @Ignore("b/447064745: Re-enable when the child object validation is fixed")
    fun testWrite_nestedListAppFunctionData_notConformSpec() {
        val innerObjectType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf("innerDouble" to AppFunctionDoubleTypeMetadata(isNullable = false)),
                required = emptyList(),
                qualifiedName = "innerData",
                isNullable = false,
                description = "Inner data description",
            )
        val incorrectInnerObjectType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf("innerDouble" to AppFunctionLongTypeMetadata(isNullable = false)),
                required = emptyList(),
                qualifiedName = "innerData",
                isNullable = false,
                description = "Incorrect inner data description",
            )
        val outerObjectType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf("nestedDataList" to AppFunctionArrayTypeMetadata(innerObjectType, false)),
                required = emptyList(),
                qualifiedName = "outerData",
                isNullable = false,
                description = "Outer data description",
            )
        val correctInnerDataBuilder =
            AppFunctionData.Builder(innerObjectType, AppFunctionComponentsMetadata())
        val incorrectInnerDataBuilder =
            AppFunctionData.Builder(incorrectInnerObjectType, AppFunctionComponentsMetadata())
        val outerDataBuilder =
            AppFunctionData.Builder(outerObjectType, AppFunctionComponentsMetadata())

        correctInnerDataBuilder.setDouble("innerDouble", 500.0)
        incorrectInnerDataBuilder.setLong("innerDouble", 500)

        assertFailsWith(IllegalArgumentException::class) {
            outerDataBuilder.setAppFunctionDataList(
                "nestedDataList",
                listOf(correctInnerDataBuilder.build(), incorrectInnerDataBuilder.build()),
            )
        }
    }

    @Test
    fun testSerialize() {
        val note = Note(title = "Test Title", attachment = Attachment(uri = "Test Uri"))

        val data = AppFunctionData.serialize(note, Note::class.java)

        assertThat(data.getString("title")).isEqualTo("Test Title")
        assertThat(data.getAppFunctionData("attachment")?.getString("uri")).isEqualTo("Test Uri")
    }

    @Test
    fun testSerialize_withQualifiedName() {
        val note = Note(title = "Test Title", attachment = Attachment(uri = "Test Uri"))

        val data = AppFunctionData.serialize(note, "androidx.appfunctions.Note")

        assertThat(data.getString("title")).isEqualTo("Test Title")
        assertThat(data.getAppFunctionData("attachment")?.getString("uri")).isEqualTo("Test Uri")
    }

    @Test
    fun testDeserialize() {
        val data =
            AppFunctionData.Builder(
                    NOTE_OBJECT_TYPE_METADATA,
                    AppFunctionComponentsMetadata(
                        mapOf(
                            "androidx.appfunctions.Attachment" to ATTACHMENT_OBJECT_TYPE_METADATA,
                            "androidx.appfunctions.Note" to NOTE_OBJECT_TYPE_METADATA,
                        )
                    ),
                )
                .setString("title", "Test Title")
                .setAppFunctionData(
                    "attachment",
                    AppFunctionData.Builder(
                            ATTACHMENT_OBJECT_TYPE_METADATA,
                            AppFunctionComponentsMetadata(
                                mapOf(
                                    "androidx.appfunctions.Attachment" to
                                        ATTACHMENT_OBJECT_TYPE_METADATA
                                )
                            ),
                        )
                        .setString("uri", "Test Uri")
                        .build(),
                )
                .build()

        val note = data.deserialize(Note::class.java)

        assertThat(note.title).isEqualTo("Test Title")
        assertThat(note.attachment.uri).isEqualTo("Test Uri")
    }

    @Test
    fun testDeserialize_withQualifiedName() {
        val data =
            AppFunctionData.Builder(
                    NOTE_OBJECT_TYPE_METADATA,
                    AppFunctionComponentsMetadata(
                        mapOf(
                            "androidx.appfunctions.Attachment" to ATTACHMENT_OBJECT_TYPE_METADATA,
                            "androidx.appfunctions.Note" to NOTE_OBJECT_TYPE_METADATA,
                        )
                    ),
                )
                .setString("title", "Test Title")
                .setAppFunctionData(
                    "attachment",
                    AppFunctionData.Builder(
                            ATTACHMENT_OBJECT_TYPE_METADATA,
                            AppFunctionComponentsMetadata(
                                mapOf(
                                    "androidx.appfunctions.Attachment" to
                                        ATTACHMENT_OBJECT_TYPE_METADATA
                                )
                            ),
                        )
                        .setString("uri", "Test Uri")
                        .build(),
                )
                .build()

        val note = data.deserialize<Note>("androidx.appfunctions.Note")

        assertThat(note.title).isEqualTo("Test Title")
        assertThat(note.attachment.uri).isEqualTo("Test Uri")
    }

    @Test
    fun testSerialize_missingFactory() {
        val missingFactoryClass = MissingFactoryClass("test")

        assertFailsWith(IllegalArgumentException::class) {
            AppFunctionData.serialize(missingFactoryClass, MissingFactoryClass::class.java)
        }
    }

    @Test
    fun testDeserialize_missingFactory() {
        val data =
            AppFunctionData.Builder(
                    AppFunctionObjectTypeMetadata(
                        properties =
                            mapOf("item" to AppFunctionStringTypeMetadata(isNullable = false)),
                        required = listOf("item"),
                        qualifiedName = "androidx.appfunctions-MissingFactoryClass",
                        isNullable = false,
                    ),
                    AppFunctionComponentsMetadata(),
                )
                .setString("item", "test")
                .build()

        assertFailsWith(IllegalArgumentException::class) {
            data.deserialize(MissingFactoryClass::class.java)
        }
    }

    @Test
    fun testId_buildAsAppFunctionData_ReadAsGenericDocument() {
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 13)
        val data =
            AppFunctionData.Builder(
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "id",
                            isRequired = true,
                            dataType = AppFunctionStringTypeMetadata(isNullable = false),
                        )
                    ),
                    AppFunctionComponentsMetadata(),
                )
                .setString("id", "123456")
                .build()
        val gd = data.genericDocument

        assertThat(gd.id).isEqualTo("123456")
    }

    @Test
    fun testId_buildAsGenericDocument_ReadAsAppFunctionData() {
        val extras = Bundle.EMPTY
        val gd = GenericDocument.Builder<GenericDocument.Builder<*>>("", "123456", "").build()

        val data = AppFunctionData(gd, extras)

        assertThat(data.containsKey("id")).isTrue()
        assertThat(data.getString("id")).isEqualTo("123456")
    }

    @Test
    fun buildAppFunctionData_withPrimitiveResponse_incorrectType() {
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType = AppFunctionStringTypeMetadata(isNullable = true)
            )

        assertFailsWith(IllegalArgumentException::class) {
            AppFunctionData.Builder(responseMetadata, AppFunctionComponentsMetadata())
                .setInt(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE, 10)
                .build()
        }
    }

    @Test
    fun buildAppFunctionData_withPrimitiveResponse_correctType() {
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType = AppFunctionStringTypeMetadata(isNullable = true)
            )

        val data =
            AppFunctionData.Builder(responseMetadata, AppFunctionComponentsMetadata())
                .setString(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE, "test")
                .build()

        assertThat(data.getString(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE))
            .isEqualTo("test")
    }

    @Test
    fun buildAppFunctionData_withObjectResponse() {
        val objectMetadata =
            AppFunctionObjectTypeMetadata(
                properties = mapOf("long" to AppFunctionLongTypeMetadata(isNullable = false)),
                required = listOf("long"),
                isNullable = false,
                qualifiedName = "testObject",
                description = "Test object description",
            )
        val responseMetadata = AppFunctionResponseMetadata(valueType = objectMetadata)

        val data =
            AppFunctionData.Builder(responseMetadata, AppFunctionComponentsMetadata())
                .setAppFunctionData(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    AppFunctionData.Builder(objectMetadata, AppFunctionComponentsMetadata())
                        .setLong("long", 100L)
                        .build(),
                )
                .build()

        assertThat(
                data
                    .getAppFunctionData(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)!!
                    .getLong("long")
            )
            .isEqualTo(100L)
    }

    @Test
    fun buildAppFunctionData_withReferenceResponse() {
        val objectMetadata =
            AppFunctionObjectTypeMetadata(
                properties = mapOf("long" to AppFunctionLongTypeMetadata(isNullable = false)),
                required = listOf("long"),
                isNullable = false,
                qualifiedName = "testObject",
                description = "Test object description",
            )
        val componentMetadata =
            AppFunctionComponentsMetadata(dataTypes = mapOf("testObject" to objectMetadata))
        val responseMetadata =
            AppFunctionResponseMetadata(
                valueType =
                    AppFunctionReferenceTypeMetadata(
                        referenceDataType = "testObject",
                        isNullable = true,
                    )
            )

        val data =
            AppFunctionData.Builder(responseMetadata, componentMetadata)
                .setAppFunctionData(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                    AppFunctionData.Builder(objectMetadata, componentMetadata)
                        .setLong("long", 100L)
                        .build(),
                )
                .build()

        assertThat(
                data
                    .getAppFunctionData(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)!!
                    .getLong("long")
            )
            .isEqualTo(100L)
    }

    @Test
    fun visitAppFunctionUriGrant_visitAllGrants() {
        val data =
            AppFunctionData.Builder(
                    TEST_NESTED_APP_FUNCTION_URI_GRANT_OBJECT_METADATA,
                    TEST_COMPONENT_METADATA,
                )
                .setAppFunctionData(
                    "firstGrant",
                    AppFunctionData.serialize(
                        AppFunctionUriGrant(
                            uri = Uri.parse("content://com.example/1"),
                            modeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        ),
                        AppFunctionUriGrant::class.java,
                    ),
                )
                .setAppFunctionData(
                    "nest",
                    AppFunctionData.Builder(
                            TEST_APP_FUNCTION_URI_GRANT_HOLDER_OBJECT_METADATA,
                            TEST_COMPONENT_METADATA,
                        )
                        .setAppFunctionData(
                            "secondGrant",
                            AppFunctionData.serialize(
                                AppFunctionUriGrant(
                                    uri = Uri.parse("content://com.example/2"),
                                    modeFlags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                                ),
                                AppFunctionUriGrant::class.java,
                            ),
                        )
                        .build(),
                )
                .setAppFunctionDataList(
                    "thirdGrants",
                    listOf(
                        AppFunctionData.serialize(
                            AppFunctionUriGrant(
                                uri = Uri.parse("content://com.example/3-1"),
                                modeFlags =
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                            ),
                            AppFunctionUriGrant::class.java,
                        ),
                        AppFunctionData.serialize(
                            AppFunctionUriGrant(
                                uri = Uri.parse("content://com.example/3-2"),
                                modeFlags =
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
                            ),
                            AppFunctionUriGrant::class.java,
                        ),
                    ),
                )
                .build()

        val visited = buildList { data.visitAppFunctionUriGrants { uriGrant -> add(uriGrant) } }

        assertThat(visited)
            .containsExactly(
                AppFunctionUriGrant(
                    uri = Uri.parse("content://com.example/1"),
                    modeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION,
                ),
                AppFunctionUriGrant(
                    uri = Uri.parse("content://com.example/2"),
                    modeFlags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                ),
                AppFunctionUriGrant(
                    uri = Uri.parse("content://com.example/3-1"),
                    modeFlags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                ),
                AppFunctionUriGrant(
                    uri = Uri.parse("content://com.example/3-2"),
                    modeFlags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
                ),
            )
    }

    @Test
    fun getParcelable_withWrongParcelableName_returnsNull() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val gameCharacter = GameCharacter(name = "Test", level = 10, characterClass = "Warrior")
        val data =
            AppFunctionData.Builder(TEST_OBJECT_METADATA, TEST_COMPONENT_METADATA)
                .setParcelable(key = "bitmap", bitmap)
                .setParcelableList(key = "customParcelableList", listOf(gameCharacter))
                .build()

        assertThat(data.getParcelable("bitmap", GameCharacter::class.java)).isNull()
        assertThat(data.getParcelableList("customParcelableList", Bitmap::class.java)).isNull()
    }

    /** A custom Parcelable class representing a game character, implemented manually. */
    private data class GameCharacter(val name: String, val level: Int, val characterClass: String) :
        Parcelable {

        constructor(
            parcel: Parcel
        ) : this(
            name = parcel.readString() ?: "",
            level = parcel.readInt(),
            characterClass = parcel.readString() ?: "",
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
            parcel.writeInt(level)
            parcel.writeString(characterClass)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<GameCharacter> {
            override fun createFromParcel(parcel: Parcel): GameCharacter {
                return GameCharacter(parcel)
            }

            override fun newArray(size: Int): Array<GameCharacter?> {
                return arrayOfNulls(size)
            }
        }
    }

    @Test
    fun buildAllOfTypeObject_allRequiredField_success() {
        val data =
            AppFunctionData.Builder(
                    OpenableNote.OPENABLE_NOTE_ALL_OF_TYPE_METADATA,
                    OpenableNote.COMPONENT_METADATA,
                )
                .setString("title", "test")
                .setAppFunctionData(
                    "attachment",
                    AppFunctionData.Builder(
                            ATTACHMENT_OBJECT_TYPE_METADATA,
                            AppFunctionComponentsMetadata(),
                        )
                        .setString("uri", "test")
                        .build(),
                )
                .setPendingIntent(
                    "intentToOpen",
                    PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
                )
                .build()

        assertThat(data.getString("title")).isEqualTo("test")
        assertThat(data.getAppFunctionData("attachment")?.getString("uri")).isEqualTo("test")
        assertThat(data.getPendingIntent("intentToOpen")).isNotNull()
    }

    @Test
    fun serializeAllOfTypeObject_allRequiredField_success() {
        val data =
            AppFunctionData.serialize(
                OpenableNote(
                    title = "test",
                    attachment = Attachment(uri = "test"),
                    intentToOpen =
                        PendingIntent.getActivity(
                            context,
                            0,
                            Intent(),
                            PendingIntent.FLAG_IMMUTABLE,
                        ),
                ),
                OpenableNote::class.java,
            )

        assertThat(data.getString("title")).isEqualTo("test")
        assertThat(data.getAppFunctionData("attachment")?.getString("uri")).isEqualTo("test")
        assertThat(data.getPendingIntent("intentToOpen")).isNotNull()
        // Also ensure that read validation is applied
        assertFailsWith<IllegalArgumentException> { data.getInt("intentToOpen") }
    }

    @Test
    fun buildAllOfTypeObject_missOriginalClassField_fail() {
        assertFailsWith<IllegalArgumentException> {
            AppFunctionData.Builder(
                    OpenableNote.OPENABLE_NOTE_ALL_OF_TYPE_METADATA,
                    OpenableNote.COMPONENT_METADATA,
                )
                .setPendingIntent(
                    "intentToOpen",
                    PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
                )
                .build()
        }
    }

    @Test
    fun buildAllOfTypeObject_missCapabilityField_fail() {
        assertFailsWith<IllegalArgumentException> {
            AppFunctionData.Builder(
                    OpenableNote.OPENABLE_NOTE_ALL_OF_TYPE_METADATA,
                    OpenableNote.COMPONENT_METADATA,
                )
                .setString("title", "test")
                .setAppFunctionData(
                    "attachment",
                    AppFunctionData.Builder(
                            ATTACHMENT_OBJECT_TYPE_METADATA,
                            AppFunctionComponentsMetadata(),
                        )
                        .setString("uri", "test")
                        .build(),
                )
                .build()
        }
    }

    companion object {
        val TEST_OBJECT_METADATA =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "int" to AppFunctionIntTypeMetadata(false),
                        "long" to AppFunctionLongTypeMetadata(false),
                        "float" to AppFunctionFloatTypeMetadata(false),
                        "double" to AppFunctionDoubleTypeMetadata(false),
                        "boolean" to AppFunctionBooleanTypeMetadata(false),
                        "string" to AppFunctionStringTypeMetadata(false),
                        "pendingIntent" to AppFunctionPendingIntentTypeMetadata(false),
                        "intArray" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionIntTypeMetadata(false),
                                isNullable = false,
                            ),
                        "longArray" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionLongTypeMetadata(false),
                                isNullable = false,
                            ),
                        "floatArray" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionFloatTypeMetadata(false),
                                isNullable = false,
                            ),
                        "doubleArray" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionDoubleTypeMetadata(false),
                                isNullable = false,
                            ),
                        "booleanArray" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionBooleanTypeMetadata(false),
                                isNullable = false,
                            ),
                        "byteArray" to AppFunctionBytesTypeMetadata(false),
                        "stringList" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionStringTypeMetadata(false),
                                isNullable = false,
                            ),
                        "pendingIntentList" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionPendingIntentTypeMetadata(false),
                                isNullable = false,
                            ),
                    ),
                required = emptyList(),
                qualifiedName = "test",
                isNullable = false,
                description = "Test object description",
            )

        val TEST_PARAMETER_METADATA =
            listOf(
                AppFunctionParameterMetadata(
                    name = "int",
                    isRequired = true,
                    dataType = AppFunctionIntTypeMetadata(isNullable = false),
                ),
                AppFunctionParameterMetadata(
                    name = "long",
                    isRequired = true,
                    dataType = AppFunctionLongTypeMetadata(isNullable = false),
                ),
                AppFunctionParameterMetadata(
                    name = "float",
                    isRequired = true,
                    dataType = AppFunctionFloatTypeMetadata(isNullable = false),
                ),
                AppFunctionParameterMetadata(
                    name = "double",
                    isRequired = true,
                    dataType = AppFunctionDoubleTypeMetadata(isNullable = false),
                ),
                AppFunctionParameterMetadata(
                    name = "boolean",
                    isRequired = true,
                    dataType = AppFunctionBooleanTypeMetadata(isNullable = false),
                ),
                AppFunctionParameterMetadata(
                    name = "string",
                    isRequired = true,
                    dataType = AppFunctionStringTypeMetadata(isNullable = false),
                ),
                AppFunctionParameterMetadata(
                    name = "pendingIntent",
                    isRequired = true,
                    dataType = AppFunctionPendingIntentTypeMetadata(isNullable = false),
                ),
                AppFunctionParameterMetadata(
                    name = "intArray",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType = AppFunctionIntTypeMetadata(isNullable = false),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "longArray",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType = AppFunctionLongTypeMetadata(isNullable = false),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "floatArray",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType = AppFunctionFloatTypeMetadata(isNullable = false),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "doubleArray",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType = AppFunctionDoubleTypeMetadata(isNullable = false),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "booleanArray",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType = AppFunctionBooleanTypeMetadata(isNullable = false),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "byteArray",
                    isRequired = true,
                    dataType = AppFunctionBytesTypeMetadata(isNullable = false),
                ),
                AppFunctionParameterMetadata(
                    name = "stringList",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType = AppFunctionStringTypeMetadata(isNullable = false),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "pendingIntentList",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType = AppFunctionPendingIntentTypeMetadata(isNullable = false),
                            isNullable = false,
                        ),
                ),
            )
        val TEST_NESTED_PARAMETER_METADATA =
            listOf(
                AppFunctionParameterMetadata(
                    name = "data",
                    isRequired = true,
                    dataType = TEST_OBJECT_METADATA,
                ),
                AppFunctionParameterMetadata(
                    name = "dataList",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType = TEST_OBJECT_METADATA,
                            isNullable = false,
                        ),
                ),
            )
    }
}
