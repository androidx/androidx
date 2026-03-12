/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation.serialization

import android.os.Parcel
import android.os.Parcelable
import androidx.kruth.assertThat
import androidx.navigation.NavType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

class NavTypeConverterAndroidTest {

    @Test
    fun matchCustomParcelable() {
        val descriptor = serializer<TestParcelable>().descriptor
        val kType = typeOf<TestParcelable>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchCustomSerializable() {
        val descriptor = serializer<TestSerializable>().descriptor
        val kType = typeOf<TestSerializable>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun getNavTypeParcelable() {
        val type = serializer<TestParcelable>().descriptor.getNavType()
        assertThat(type).isEqualTo(UNKNOWN)
    }

    @Test
    fun getNavTypeParcelableArray() {
        val type = serializer<Array<TestParcelable>>().descriptor.getNavType()
        assertThat(type).isEqualTo(UNKNOWN)
    }

    @Test
    fun getNavTypeSerializable() {
        val type = serializer<TestSerializable>().descriptor.getNavType()
        assertThat(type).isEqualTo(UNKNOWN)
    }

    @Test
    fun getNavTypeSerializableArray() {
        val type = serializer<Array<TestSerializable>>().descriptor.getNavType()
        assertThat(type).isEqualTo(UNKNOWN)
    }

    @Test
    fun getNavTypeEnumSerializable() {
        val type = serializer<TestEnum>().descriptor.getNavType()
        assertThat(type).isEqualTo(NavType.EnumType(TestEnum::class.java))
    }

    @Serializable
    class TestParcelable(val arg: Int, val arg2: String) : Parcelable {
        override fun describeContents() = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {}
    }

    @Serializable class TestSerializable(val arg: Int, val arg2: String) : java.io.Serializable

    @Serializable
    enum class TestEnum {
        First,
        Second
    }
}
