/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import androidx.navigation.common.test.R
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.Serializable

@SmallTest
class NavTypeTest {

    private class Person : Serializable {
        var field: Int = 0
    }

    companion object {
        private const val i = 1
        private val ints = intArrayOf(0, 1)
        private const val l = 1L
        private val longs = longArrayOf(0L, 1L)
        private const val fl = 1.5f
        private val floats = floatArrayOf(1f, 2.5f)
        private const val b = true
        private val booleans = booleanArrayOf(b, false)
        private const val s = "a_string"
        private val strings = arrayOf("aa", "bb")
        private val reference = R.id.nav_id_reference
        private val referenceHex = "0x" + R.id.nav_id_reference.toString(16)
        private val parcelable = ActivityInfo()
        private val parcelables = arrayOf(parcelable)
        private val en = Bitmap.Config.ALPHA_8
        private val enString = "ALPHA_8"
        private val enStringCasing = "alpha_8"
        private val serializable = Person()
        private val serializables = arrayOf(Bitmap.Config.ALPHA_8)
        private val parcelableNavType = NavType.ParcelableType(ActivityInfo::class.java)
        private val parcelableArrayNavType =
            NavType.ParcelableArrayType(ActivityInfo::class.java)
        private val serializableNavType = NavType.SerializableType(Person::class.java)
        private val enumNavType = NavType.EnumType(Bitmap.Config::class.java)
        private val serializableArrayNavType =
            NavType.SerializableArrayType(Bitmap.Config::class.java)
    }

    @Test
    fun fromArgType() {
        assertThat(NavType.fromArgType("integer", null))
            .isEqualTo(NavType.IntType)
        assertThat(NavType.fromArgType("integer[]", null))
            .isEqualTo(NavType.IntArrayType)
        assertThat(NavType.fromArgType("long", null))
            .isEqualTo(NavType.LongType)
        assertThat(NavType.fromArgType("long[]", null))
            .isEqualTo(NavType.LongArrayType)
        assertThat(NavType.fromArgType("float", null))
            .isEqualTo(NavType.FloatType)
        assertThat(NavType.fromArgType("float[]", null))
            .isEqualTo(NavType.FloatArrayType)
        assertThat(NavType.fromArgType("boolean", null))
            .isEqualTo(NavType.BoolType)
        assertThat(NavType.fromArgType("boolean[]", null))
            .isEqualTo(NavType.BoolArrayType)
        assertThat(NavType.fromArgType("string", null))
            .isEqualTo(NavType.StringType)
        assertThat(NavType.fromArgType("string[]", null))
            .isEqualTo(NavType.StringArrayType)
        assertThat(NavType.fromArgType("reference", null))
            .isEqualTo(NavType.ReferenceType)
        assertThat(NavType.fromArgType("android.content.pm.ActivityInfo", null))
            .isEqualTo(parcelableNavType)
        assertThat(NavType.fromArgType("android.content.pm.ActivityInfo[]", null))
            .isEqualTo(parcelableArrayNavType)
        assertThat(NavType.fromArgType("androidx.navigation.NavTypeTest\$Person", null))
            .isEqualTo(serializableNavType)
        assertThat(NavType.fromArgType("android.graphics.Bitmap\$Config", null))
            .isEqualTo(enumNavType)
        assertThat(NavType.fromArgType("android.graphics.Bitmap\$Config[]", null))
            .isEqualTo(serializableArrayNavType)
        assertThat(NavType.fromArgType(null, null))
            .isEqualTo(NavType.StringType)
    }

    @Test
    fun inferFromValue() {
        assertThat(NavType.inferFromValue("stringvalue"))
            .isEqualTo(NavType.StringType)
        assertThat(NavType.inferFromValue("123"))
            .isEqualTo(NavType.IntType)
        assertThat(NavType.inferFromValue("0xFF"))
            .isEqualTo(NavType.IntType)
        assertThat(NavType.inferFromValue("123L"))
            .isEqualTo(NavType.LongType)
        assertThat(NavType.inferFromValue("1.5"))
            .isEqualTo(NavType.FloatType)
        assertThat(NavType.inferFromValue("true"))
            .isEqualTo(NavType.BoolType)
    }

    @Test
    fun inferFromValueType() {
        assertThat(NavType.inferFromValueType(i))
            .isEqualTo(NavType.IntType)
        assertThat(NavType.inferFromValueType(ints))
            .isEqualTo(NavType.IntArrayType)
        assertThat(NavType.inferFromValueType(l))
            .isEqualTo(NavType.LongType)
        assertThat(NavType.inferFromValueType(longs))
            .isEqualTo(NavType.LongArrayType)
        assertThat(NavType.inferFromValueType(fl))
            .isEqualTo(NavType.FloatType)
        assertThat(NavType.inferFromValueType(floats))
            .isEqualTo(NavType.FloatArrayType)
        assertThat(NavType.inferFromValueType(b))
            .isEqualTo(NavType.BoolType)
        assertThat(NavType.inferFromValueType(booleans))
            .isEqualTo(NavType.BoolArrayType)
        assertThat(NavType.inferFromValueType(s))
            .isEqualTo(NavType.StringType)
        assertThat(NavType.inferFromValueType(strings))
            .isEqualTo(NavType.StringArrayType)
        assertThat(NavType.inferFromValueType(parcelable))
            .isEqualTo(parcelableNavType)
        assertThat(NavType.inferFromValueType(parcelables))
            .isEqualTo(parcelableArrayNavType)
        assertThat(NavType.inferFromValueType(en))
            .isEqualTo(enumNavType)
        assertThat(NavType.inferFromValueType(serializable))
            .isEqualTo(serializableNavType)
        assertThat(NavType.inferFromValueType(serializables))
            .isEqualTo(serializableArrayNavType)
        assertThat(NavType.inferFromValueType(null))
            .isEqualTo(NavType.StringType)
    }

    @Test
    fun putAndGetFromBundle() {
        val key = "key"
        val bundle = Bundle()
        NavType.IntType.put(bundle, key, i)
        assertThat(NavType.IntType[bundle, key])
            .isEqualTo(i)
        bundle.clear()

        NavType.IntArrayType.put(bundle, key, ints)
        assertThat(NavType.IntArrayType[bundle, key])
            .isEqualTo(ints)
        bundle.clear()

        NavType.LongType.put(bundle, key, l)
        assertThat(NavType.LongType[bundle, key])
            .isEqualTo(l)
        bundle.clear()

        NavType.LongArrayType.put(bundle, key, longs)
        assertThat(NavType.LongArrayType[bundle, key])
            .isEqualTo(longs)
        bundle.clear()

        NavType.FloatType.put(bundle, key, fl)
        assertThat(NavType.FloatType[bundle, key])
            .isEqualTo(fl)
        bundle.clear()

        NavType.FloatArrayType.put(bundle, key, floats)
        assertThat(NavType.FloatArrayType[bundle, key])
            .isEqualTo(floats)
        bundle.clear()

        NavType.BoolType.put(bundle, key, b)
        assertThat(NavType.BoolType[bundle, key])
            .isEqualTo(b)
        bundle.clear()

        NavType.BoolArrayType.put(bundle, key, booleans)
        assertThat(NavType.BoolArrayType[bundle, key])
            .isEqualTo(booleans)
        bundle.clear()

        NavType.StringType.put(bundle, key, s)
        assertThat(NavType.StringType[bundle, key])
            .isEqualTo(s)
        bundle.clear()

        NavType.StringArrayType.put(bundle, key, strings)
        assertThat(NavType.StringArrayType[bundle, key])
            .isEqualTo(strings)
        bundle.clear()

        NavType.ReferenceType.put(bundle, key, reference)
        assertThat(NavType.ReferenceType[bundle, key])
            .isEqualTo(reference)
        bundle.clear()

        parcelableNavType.put(bundle, key, parcelable)
        assertThat(parcelableNavType[bundle, key])
            .isEqualTo(parcelable)
        bundle.clear()

        parcelableArrayNavType.put(bundle, key, parcelables)
        assertThat(parcelableArrayNavType[bundle, key])
            .isEqualTo(parcelables)
        bundle.clear()

        enumNavType.put(bundle, key, en)
        assertThat(enumNavType[bundle, key])
            .isEqualTo(en)
        bundle.clear()

        serializableNavType.put(bundle, key, serializable)
        assertThat(serializableNavType[bundle, key])
            .isEqualTo(serializable)
        bundle.clear()

        serializableArrayNavType.put(bundle, key, serializables)
        assertThat(serializableArrayNavType[bundle, key])
            .isEqualTo(serializables)
        bundle.clear()
    }

    @Test
    fun parseValueWithHex() {
        assertThat(NavType.IntType.parseValue(referenceHex))
            .isEqualTo(reference)

        assertThat(NavType.ReferenceType.parseValue(referenceHex))
            .isEqualTo(reference)
    }

    @Test
    fun parseEnumValue() {
        assertThat(enumNavType.parseValue(enString))
            .isEqualTo(en)

        assertThat(enumNavType.parseValue(enStringCasing))
            .isEqualTo(en)
    }
}
