/*
 * Copyright 2026 The Android Open Source Project
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

import android.app.appsearch.GenericDocument
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 33)
class AppFunctionDataBundleTest {

    @Test
    fun roundTrip_allPropertyTypes_afterParceling() {
        val nested =
            AppFunctionData.Builder("com.example.Nested")
                .setString("name", "inner")
                .setParcelable("uri", Uri.parse("content://example/nested"))
                .build()
        val data =
            AppFunctionData.Builder("com.example.Root")
                .setBoolean("bool", true)
                .setInt("int", 42)
                .setLong("long", 1234567890123L)
                .setFloat("float", 1.5f)
                .setDouble("double", 2.25)
                .setString("string", "hello")
                .setByteArray("bytes", byteArrayOf(1, 2, 3))
                .setBooleanArray("bools", booleanArrayOf(true, false))
                .setIntArray("ints", intArrayOf(1, 2))
                .setLongArray("longs", longArrayOf(10L, 20L))
                .setFloatArray("floats", floatArrayOf(0.5f, 1.5f))
                .setDoubleArray("doubles", doubleArrayOf(3.5, 4.5))
                .setStringList("strings", listOf("a", "b"))
                .setAppFunctionData("nested", nested)
                .setAppFunctionDataList("nestedList", listOf(nested))
                .setParcelable("uri", Uri.parse("content://example/root"))
                .build()

        val result = AppFunctionData.fromBundle(data.toBundle().parceled())

        assertThat(result.qualifiedName).isEqualTo("com.example.Root")
        assertThat(result.getBoolean("bool")).isTrue()
        assertThat(result.getInt("int")).isEqualTo(42)
        assertThat(result.getLong("long")).isEqualTo(1234567890123L)
        assertThat(result.getFloat("float")).isEqualTo(1.5f)
        assertThat(result.getDouble("double")).isEqualTo(2.25)
        assertThat(result.getString("string")).isEqualTo("hello")
        assertThat(result.getByteArray("bytes")).isEqualTo(byteArrayOf(1, 2, 3))
        assertThat(result.getBooleanArray("bools")).isEqualTo(booleanArrayOf(true, false))
        assertThat(result.getIntArray("ints")).isEqualTo(intArrayOf(1, 2))
        assertThat(result.getLongArray("longs")).isEqualTo(longArrayOf(10L, 20L))
        assertThat(result.getFloatArray("floats")).isEqualTo(floatArrayOf(0.5f, 1.5f))
        assertThat(result.getDoubleArray("doubles")).isEqualTo(doubleArrayOf(3.5, 4.5))
        assertThat(result.getStringList("strings")).containsExactly("a", "b").inOrder()
        assertThat(result.getParcelable<Uri>("uri")).isEqualTo(Uri.parse("content://example/root"))

        val resultNested = result.getAppFunctionData("nested")!!
        assertThat(resultNested.qualifiedName).isEqualTo("com.example.Nested")
        assertThat(resultNested.getString("name")).isEqualTo("inner")
        assertThat(resultNested.getParcelable<Uri>("uri"))
            .isEqualTo(Uri.parse("content://example/nested"))

        val resultNestedList = result.getAppFunctionDataList("nestedList")!!
        assertThat(resultNestedList).hasSize(1)
        assertThat(resultNestedList[0].getString("name")).isEqualTo("inner")
        assertThat(resultNestedList[0].getParcelable<Uri>("uri"))
            .isEqualTo(Uri.parse("content://example/nested"))
    }

    @Test
    fun roundTrip_emptyData() {
        val result = AppFunctionData.fromBundle(AppFunctionData.EMPTY.toBundle().parceled())

        assertThat(result.qualifiedName).isEqualTo(AppFunctionData.EMPTY.qualifiedName)
        assertThat(result.genericDocument.propertyNames).isEmpty()
    }

    @Test
    fun toBundle_nestedTooDeep_throws() {
        var doc = GenericDocument.Builder<GenericDocument.Builder<*>>("", "", "Leaf").build()
        repeat(70) {
            doc =
                GenericDocument.Builder<GenericDocument.Builder<*>>("", "", "Node")
                    .setPropertyDocument("child", doc)
                    .build()
        }
        val data = AppFunctionData(doc, Bundle())

        assertFailsWith<IllegalArgumentException> { data.toBundle() }
    }

    @Test
    fun fromBundle_nestedTooDeep_throws() {
        var docBundle =
            Bundle().apply {
                putString("namespace", "")
                putString("id", "")
                putString("schemaType", "Leaf")
            }
        repeat(70) {
            val childProp =
                Bundle().apply {
                    putInt("type", 6)
                    putParcelableArrayList("value", arrayListOf(docBundle))
                }
            val props = Bundle().apply { putBundle("child", childProp) }
            docBundle =
                Bundle().apply {
                    putString("namespace", "")
                    putString("id", "")
                    putString("schemaType", "Node")
                    putBundle("properties", props)
                }
        }
        val dataBundle = Bundle().apply { putBundle("document", docBundle) }

        assertFailsWith<IllegalArgumentException> { AppFunctionData.fromBundle(dataBundle) }
    }

    @Test
    fun fromBundle_missingDocument_throws() {
        assertFailsWith<IllegalArgumentException> { AppFunctionData.fromBundle(Bundle()) }
    }

    @Test
    fun fromBundle_unknownPropertyType_throws() {
        val prop = Bundle().apply { putInt("type", Int.MAX_VALUE) }
        val docBundle =
            Bundle().apply {
                putString("namespace", "")
                putString("id", "")
                putString("schemaType", "Test")
                putBundle("properties", Bundle().apply { putBundle("bad", prop) })
            }
        val dataBundle = Bundle().apply { putBundle("document", docBundle) }

        assertFailsWith<IllegalArgumentException> { AppFunctionData.fromBundle(dataBundle) }
    }

    @Test
    fun fromBundle_nullStringArrayElement_throws() {
        val prop =
            Bundle().apply {
                putInt("type", 1)
                putStringArray("value", arrayOf("ok", null))
            }
        val docBundle =
            Bundle().apply {
                putString("namespace", "")
                putString("id", "")
                putString("schemaType", "Test")
                putBundle("properties", Bundle().apply { putBundle("strings", prop) })
            }
        val dataBundle = Bundle().apply { putBundle("document", docBundle) }

        assertFailsWith<IllegalArgumentException> { AppFunctionData.fromBundle(dataBundle) }
    }

    @Test
    fun fromBundle_nullPropertyName_throws() {
        val prop =
            Bundle().apply {
                putInt("type", 1)
                putStringArray("value", arrayOf("ok"))
            }
        val docBundle =
            Bundle().apply {
                putString("namespace", "")
                putString("id", "")
                putString("schemaType", "Test")
                putBundle("properties", Bundle().apply { putBundle(null, prop) })
            }
        val dataBundle = Bundle().apply { putBundle("document", docBundle) }

        assertFailsWith<IllegalArgumentException> { AppFunctionData.fromBundle(dataBundle) }
    }

    private fun Bundle.parceled(): Bundle {
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(this)
            parcel.setDataPosition(0)
            return requireNotNull(parcel.readBundle(javaClass.classLoader))
        } finally {
            parcel.recycle()
        }
    }
}
