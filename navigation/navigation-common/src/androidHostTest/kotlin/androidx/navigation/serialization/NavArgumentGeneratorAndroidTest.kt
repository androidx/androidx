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

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.navigation.CollectionNavType
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.common.truth.Truth.assertThat
import kotlin.reflect.typeOf
import kotlin.test.assertFailsWith
import kotlin.test.fail
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NavArgumentGeneratorAndroidTest {
    
    @Test
    fun convertToEnumList() {
        @Serializable class TestClass(val arg: List<TestEnum>)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = InternalAndroidNavType.EnumListType(TestEnum::class.java)
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToEnumListNullable() {
        @Serializable class TestClass(val arg: List<TestEnum>?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = InternalAndroidNavType.EnumListType(TestEnum::class.java)
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToParcelable() {
        @Serializable
        class TestParcelable : Parcelable {
            override fun describeContents() = 0

            override fun writeToParcel(dest: Parcel, flags: Int) {}
        }

        @Serializable class TestClass(val arg: TestParcelable)

        val navType =
            object : NavType<TestParcelable>(false) {
                override fun put(bundle: Bundle, key: String, value: TestParcelable) {}

                override fun get(bundle: Bundle, key: String) = null

                override fun parseValue(value: String) = TestParcelable()
            }

        val converted =
            serializer<TestClass>().generateNavArguments(mapOf(typeOf<TestParcelable>() to navType))
        val expected =
            navArgument("arg") {
                type = navType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToParcelableNullable() {
        @Serializable
        class TestParcelable : Parcelable {
            override fun describeContents() = 0

            override fun writeToParcel(dest: Parcel, flags: Int) {}
        }

        @Serializable class TestClass(val arg: TestParcelable?)

        val navType =
            object : NavType<TestParcelable?>(true) {
                override fun put(bundle: Bundle, key: String, value: TestParcelable?) {}

                override fun get(bundle: Bundle, key: String) = null

                override fun parseValue(value: String) = TestParcelable()
            }

        val converted =
            serializer<TestClass>()
                .generateNavArguments(mapOf(typeOf<TestParcelable?>() to navType))
        val expected =
            navArgument("arg") {
                type = navType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToParcelableArray() {
        @Serializable
        class TestParcelable : Parcelable {
            override fun describeContents() = 0

            override fun writeToParcel(dest: Parcel, flags: Int) {}
        }

        @Serializable class TestClass(val arg: Array<TestParcelable>)

        val navType =
            object : NavType<Array<TestParcelable>>(false) {
                override fun put(bundle: Bundle, key: String, value: Array<TestParcelable>) {}

                override fun get(bundle: Bundle, key: String) = null

                override fun parseValue(value: String) = emptyArray<TestParcelable>()
            }
        val converted =
            serializer<TestClass>()
                .generateNavArguments(mapOf(typeOf<Array<TestParcelable>>() to navType))
        val expected =
            navArgument("arg") {
                type = navType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToParcelableArrayNullable() {
        @Serializable
        class TestParcelable : Parcelable {
            override fun describeContents() = 0

            override fun writeToParcel(dest: Parcel, flags: Int) {}
        }

        @Serializable class TestClass(val arg: Array<TestParcelable>?)

        val navType =
            object : NavType<Array<TestParcelable>>(true) {
                override fun put(bundle: Bundle, key: String, value: Array<TestParcelable>) {}

                override fun get(bundle: Bundle, key: String) = null

                override fun parseValue(value: String) = emptyArray<TestParcelable>()
            }
        val converted =
            serializer<TestClass>()
                .generateNavArguments(mapOf(typeOf<Array<TestParcelable>?>() to navType))
        val expected =
            navArgument("arg") {
                type = navType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToSerializable() {
        @Serializable class TestSerializable : java.io.Serializable

        @Serializable class TestClass(val arg: TestSerializable)

        val navType =
            object : NavType<TestSerializable>(false) {
                override fun put(bundle: Bundle, key: String, value: TestSerializable) {}

                override fun get(bundle: Bundle, key: String) = null

                override fun parseValue(value: String) = TestSerializable()
            }
        val converted =
            serializer<TestClass>()
                .generateNavArguments(mapOf(typeOf<TestSerializable>() to navType))
        val expected =
            navArgument("arg") {
                type = navType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToSerializableNullable() {
        @Serializable class TestSerializable : java.io.Serializable

        @Serializable class TestClass(val arg: TestSerializable?)

        val navType =
            object : NavType<TestSerializable>(true) {
                override fun put(bundle: Bundle, key: String, value: TestSerializable) {}

                override fun get(bundle: Bundle, key: String) = null

                override fun parseValue(value: String) = TestSerializable()
            }
        val converted =
            serializer<TestClass>()
                .generateNavArguments(mapOf(typeOf<TestSerializable?>() to navType))
        val expected =
            navArgument("arg") {
                type = navType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToSerializableArray() {
        @Serializable class TestSerializable : java.io.Serializable

        @Serializable class TestClass(val arg: Array<TestSerializable>)

        val navType =
            object : NavType<Array<TestSerializable>>(false) {
                override fun put(bundle: Bundle, key: String, value: Array<TestSerializable>) {}

                override fun get(bundle: Bundle, key: String) = null

                override fun parseValue(value: String) = emptyArray<TestSerializable>()
            }
        val converted =
            serializer<TestClass>()
                .generateNavArguments(mapOf(typeOf<Array<TestSerializable>>() to navType))
        val expected =
            navArgument("arg") {
                type = navType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToSerializableArrayNullable() {
        @Serializable class TestSerializable : java.io.Serializable

        @Serializable class TestClass(val arg: Array<TestSerializable>?)

        val navType =
            object : NavType<Array<TestSerializable>>(true) {
                override fun put(bundle: Bundle, key: String, value: Array<TestSerializable>) {}

                override fun get(bundle: Bundle, key: String) = null

                override fun parseValue(value: String) = emptyArray<TestSerializable>()
            }
        val converted =
            serializer<TestClass>()
                .generateNavArguments(mapOf(typeOf<Array<TestSerializable>?>() to navType))
        val expected =
            navArgument("arg") {
                type = navType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToEnum() {
        @Serializable class TestClass(val arg: TestEnum)

        val expected =
            navArgument("arg") {
                type = NavType.EnumType(TestEnum::class.java)
                nullable = false
            }
        val converted = serializer<TestClass>().generateNavArguments()
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToTopLevelEnum() {
        @Serializable class TestClass(val arg: TestTopLevelEnum)

        val expected =
            navArgument("arg") {
                type = NavType.EnumType(TestTopLevelEnum::class.java)
                nullable = false
            }
        val converted = serializer<TestClass>().generateNavArguments()
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToEnumNullable() {
        @Serializable class TestClass(val arg: TestEnum?)

        @Suppress("UNCHECKED_CAST")
        val expected =
            navArgument("arg") {
                type =
                    InternalAndroidNavType.EnumNullableType(TestEnum::class.java as Class<Enum<*>?>)
                nullable = true
            }
        val converted = serializer<TestClass>().generateNavArguments()
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToNestedEnum() {
        @Serializable class TestClass(val arg: EnumWrapper.NestedEnum)

        val expected =
            navArgument("arg") {
                type = NavType.EnumType(EnumWrapper.NestedEnum::class.java)
                nullable = false
            }
        val converted = serializer<TestClass>().generateNavArguments()
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToEnumOverriddenSerialNameIllegal() {
        @Serializable class TestClass(val arg: TestEnumCustomSerialName)

        val exception =
            assertFailsWith<IllegalArgumentException> {
                serializer<TestClass>().generateNavArguments()
            }
        assertThat(exception.message)
            .isEqualTo(
                "Cannot find class with name \"MyCustomSerialName\". Ensure that the " +
                    "serialName for this argument is the default fully qualified name." +
                    "\nIf the build is minified, try annotating the Enum class " +
                    "with \"androidx.annotation.Keep\" to ensure the Enum is not removed."
            )
    }

    // writing our own assert so we don't need to override NamedNavArgument's equals
    // and hashcode which will need to be public api.
    private fun assertThat(actual: List<NamedNavArgument>) = actual

    enum class TestEnum {
        TEST
    }

    @SerialName("MyCustomSerialName")
    enum class TestEnumCustomSerialName {
        TEST
    }

    @Serializable
    private class EnumWrapper {
        enum class NestedEnum {
            ONE,
            TWO
        }
    }
}

enum class TestTopLevelEnum {
    TEST
}
