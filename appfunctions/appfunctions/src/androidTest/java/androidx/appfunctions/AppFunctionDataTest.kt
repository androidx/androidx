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
import android.os.Build
import android.os.Bundle
import android.os.ext.SdkExtensions
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_BOOLEAN
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_BYTES
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_DOUBLE
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_FLOAT
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_INT
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_LONG
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_PENDING_INTENT
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_STRING
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class AppFunctionDataTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
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
                    mapOf(
                        "firstName" to
                            AppFunctionPrimitiveTypeMetadata(type = TYPE_STRING, isNullable = false)
                    ),
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
                    mapOf(
                        "innerDouble" to
                            AppFunctionPrimitiveTypeMetadata(type = TYPE_DOUBLE, isNullable = false)
                    ),
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
    fun testWrite_nestedAppFunctionData_notConformSpec() {
        val innerObjectType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "innerDouble" to
                            AppFunctionPrimitiveTypeMetadata(type = TYPE_DOUBLE, isNullable = false)
                    ),
                required = emptyList(),
                qualifiedName = "innerData",
                isNullable = false,
                description = "Inner data description",
            )
        val incorrectInnerObjectType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "innerDouble" to
                            AppFunctionPrimitiveTypeMetadata(type = TYPE_LONG, isNullable = false)
                    ),
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
    fun testWrite_nestedListAppFunctionData_notConformSpec() {
        val innerObjectType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "innerDouble" to
                            AppFunctionPrimitiveTypeMetadata(type = TYPE_DOUBLE, isNullable = false)
                    ),
                required = emptyList(),
                qualifiedName = "innerData",
                isNullable = false,
                description = "Inner data description",
            )
        val incorrectInnerObjectType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "innerDouble" to
                            AppFunctionPrimitiveTypeMetadata(type = TYPE_LONG, isNullable = false)
                    ),
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
            AppFunctionData.Builder("androidx.appfunctions.Note")
                .setString("title", "Test Title")
                .setAppFunctionData(
                    "attachment",
                    AppFunctionData.Builder("androidx.appfunctions.Attachment")
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
            AppFunctionData.Builder("androidx.appfunctions.Note")
                .setString("title", "Test Title")
                .setAppFunctionData(
                    "attachment",
                    AppFunctionData.Builder("androidx.appfunctions.Attachment")
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
            AppFunctionData.Builder("androidx.appfunctions-MissingFactoryClass")
                .setString("item", "test")
                .build()

        assertFailsWith(IllegalArgumentException::class) {
            data.deserialize(MissingFactoryClass::class.java)
        }
    }

    @Test
    fun testId_buildAsAppFunctionData_ReadAsGenericDocument() {
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 13)
        val data = AppFunctionData.Builder("").setString("id", "123456").build()
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
                valueType = AppFunctionPrimitiveTypeMetadata(TYPE_STRING, isNullable = true)
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
                valueType = AppFunctionPrimitiveTypeMetadata(TYPE_STRING, isNullable = true)
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
                properties =
                    mapOf(
                        "long" to AppFunctionPrimitiveTypeMetadata(TYPE_LONG, isNullable = false)
                    ),
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
                properties =
                    mapOf(
                        "long" to AppFunctionPrimitiveTypeMetadata(TYPE_LONG, isNullable = false)
                    ),
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

    companion object {
        val TEST_OBJECT_METADATA =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "int" to AppFunctionPrimitiveTypeMetadata(TYPE_INT, false),
                        "long" to AppFunctionPrimitiveTypeMetadata(TYPE_LONG, false),
                        "float" to AppFunctionPrimitiveTypeMetadata(TYPE_FLOAT, false),
                        "double" to AppFunctionPrimitiveTypeMetadata(TYPE_DOUBLE, false),
                        "boolean" to AppFunctionPrimitiveTypeMetadata(TYPE_BOOLEAN, false),
                        "string" to AppFunctionPrimitiveTypeMetadata(TYPE_STRING, false),
                        "pendingIntent" to
                            AppFunctionPrimitiveTypeMetadata(TYPE_PENDING_INTENT, false),
                        "intArray" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionPrimitiveTypeMetadata(TYPE_INT, false),
                                isNullable = false,
                            ),
                        "longArray" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionPrimitiveTypeMetadata(TYPE_LONG, false),
                                isNullable = false,
                            ),
                        "floatArray" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionPrimitiveTypeMetadata(TYPE_FLOAT, false),
                                isNullable = false,
                            ),
                        "doubleArray" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionPrimitiveTypeMetadata(TYPE_DOUBLE, false),
                                isNullable = false,
                            ),
                        "booleanArray" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionPrimitiveTypeMetadata(TYPE_BOOLEAN, false),
                                isNullable = false,
                            ),
                        "byteArray" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionPrimitiveTypeMetadata(TYPE_BYTES, false),
                                isNullable = false,
                            ),
                        "stringList" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionPrimitiveTypeMetadata(TYPE_STRING, false),
                                isNullable = false,
                            ),
                        "pendingIntentList" to
                            AppFunctionArrayTypeMetadata(
                                itemType =
                                    AppFunctionPrimitiveTypeMetadata(TYPE_PENDING_INTENT, false),
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
                    dataType = AppFunctionPrimitiveTypeMetadata(type = TYPE_INT, isNullable = false),
                ),
                AppFunctionParameterMetadata(
                    name = "long",
                    isRequired = true,
                    dataType =
                        AppFunctionPrimitiveTypeMetadata(type = TYPE_LONG, isNullable = false),
                ),
                AppFunctionParameterMetadata(
                    name = "float",
                    isRequired = true,
                    dataType =
                        AppFunctionPrimitiveTypeMetadata(type = TYPE_FLOAT, isNullable = false),
                ),
                AppFunctionParameterMetadata(
                    name = "double",
                    isRequired = true,
                    dataType =
                        AppFunctionPrimitiveTypeMetadata(type = TYPE_DOUBLE, isNullable = false),
                ),
                AppFunctionParameterMetadata(
                    name = "boolean",
                    isRequired = true,
                    dataType =
                        AppFunctionPrimitiveTypeMetadata(type = TYPE_BOOLEAN, isNullable = false),
                ),
                AppFunctionParameterMetadata(
                    name = "string",
                    isRequired = true,
                    dataType =
                        AppFunctionPrimitiveTypeMetadata(type = TYPE_STRING, isNullable = false),
                ),
                AppFunctionParameterMetadata(
                    name = "pendingIntent",
                    isRequired = true,
                    dataType =
                        AppFunctionPrimitiveTypeMetadata(
                            type = TYPE_PENDING_INTENT,
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "intArray",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = TYPE_INT,
                                    isNullable = false,
                                ),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "longArray",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = TYPE_LONG,
                                    isNullable = false,
                                ),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "floatArray",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = TYPE_FLOAT,
                                    isNullable = false,
                                ),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "doubleArray",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = TYPE_DOUBLE,
                                    isNullable = false,
                                ),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "booleanArray",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = TYPE_BOOLEAN,
                                    isNullable = false,
                                ),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "byteArray",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = TYPE_BYTES,
                                    isNullable = false,
                                ),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "stringList",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = TYPE_STRING,
                                    isNullable = false,
                                ),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "pendingIntentList",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = TYPE_PENDING_INTENT,
                                    isNullable = false,
                                ),
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
