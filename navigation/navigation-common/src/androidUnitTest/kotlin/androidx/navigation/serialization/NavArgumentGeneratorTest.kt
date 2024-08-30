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
class NavArgumentGeneratorTest {
    @Test
    fun convertToInt() {
        @Serializable class TestClass(val arg: Int)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.IntType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToIntNullable() {
        @Serializable class TestClass(val arg: Int?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = InternalNavType.IntNullableType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToString() {
        @Serializable class TestClass(val arg: String)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.StringType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToStringNullable() {
        @Serializable class TestClass(val arg: String?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.StringType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToBoolean() {
        @Serializable class TestClass(val arg: Boolean)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.BoolType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToBooleanNullable() {
        @Serializable class TestClass(val arg: Boolean?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = InternalNavType.BoolNullableType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToFloat() {
        @Serializable class TestClass(val arg: Float)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.FloatType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToFloatNullable() {
        @Serializable class TestClass(val arg: Float?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = InternalNavType.FloatNullableType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToLong() {
        @Serializable class TestClass(val arg: Long)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.LongType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToLongNullable() {
        @Serializable class TestClass(val arg: Long?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = InternalNavType.LongNullableType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToIntArray() {
        @Serializable class TestClass(val arg: IntArray)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.IntArrayType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToIntArrayNullable() {
        @Serializable class TestClass(val arg: IntArray?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.IntArrayType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToIntList() {
        @Serializable class TestClass(val arg: List<Int>)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.IntListType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertArrayListToIntList() {
        @Serializable class TestClass(val arg: ArrayList<Int>)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.IntListType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToIntListNullable() {
        @Serializable class TestClass(val arg: List<Int>?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.IntListType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToLongArray() {
        @Serializable class TestClass(val arg: LongArray)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.LongArrayType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToLongArrayNullable() {
        @Serializable class TestClass(val arg: LongArray?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.LongArrayType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToLongList() {
        @Serializable class TestClass(val arg: List<Long>)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.LongListType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertArrayListToLongList() {
        @Serializable class TestClass(val arg: ArrayList<Long>)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.LongListType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToLongListNullable() {
        @Serializable class TestClass(val arg: List<Long>?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.LongListType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToFloatArray() {
        @Serializable class TestClass(val arg: FloatArray)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.FloatArrayType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToFloatArrayNullable() {
        @Serializable class TestClass(val arg: FloatArray?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.FloatArrayType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToFloatList() {
        @Serializable class TestClass(val arg: List<Float>)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.FloatListType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertArrayListToFloatList() {
        @Serializable class TestClass(val arg: ArrayList<Float>)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.FloatListType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToFloatListNullable() {
        @Serializable class TestClass(val arg: List<Float>?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.FloatListType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToBoolArray() {
        @Serializable class TestClass(val arg: BooleanArray)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.BoolArrayType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToBoolArrayNullable() {
        @Serializable class TestClass(val arg: BooleanArray?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.BoolArrayType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToBooleanList() {
        @Serializable class TestClass(val arg: List<Boolean>)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.BoolListType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertArrayListToBooleanList() {
        @Serializable class TestClass(val arg: ArrayList<Boolean>)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.BoolListType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToBooleanListNullable() {
        @Serializable class TestClass(val arg: List<Boolean>?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.BoolListType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToStringArray() {
        @Serializable class TestClass(val arg: Array<String>)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.StringArrayType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToStringArrayNullable() {
        @Serializable class TestClass(val arg: Array<String>?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.StringArrayType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToStringList() {
        @Serializable class TestClass(val arg: List<String>)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.StringListType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertArrayListToStringList() {
        @Serializable class TestClass(val arg: ArrayList<String>)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.StringListType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertToStringListNullable() {
        @Serializable class TestClass(val arg: List<String>?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.StringListType
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
                type = InternalNavType.EnumNullableType(TestEnum::class.java as Class<Enum<*>?>)
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
                    "serialName for this argument is the default fully qualified name"
            )
    }

    @Test
    fun convertToEnumArray() {
        @Serializable class TestClass(val arg: Array<TestEnum>)
        val navType =
            object : CollectionNavType<Array<TestEnum>>(false) {
                override fun put(bundle: Bundle, key: String, value: Array<TestEnum>) {}

                override fun serializeAsValues(value: Array<TestEnum>) = emptyList<String>()

                override fun emptyCollection(): Array<TestEnum> = emptyArray()

                override fun get(bundle: Bundle, key: String) = null

                override fun parseValue(value: String) = emptyArray<TestEnum>()
            }
        val converted =
            serializer<TestClass>()
                .generateNavArguments(mapOf(typeOf<Array<TestEnum>>() to navType))
        val expected =
            navArgument("arg") {
                type = navType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertWithDefaultValue() {
        @Serializable class TestClass(val arg: String = "test")
        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.StringType
                nullable = false
                unknownDefaultValuePresent = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isTrue()
    }

    @Test
    fun convertNullableWithDefaultValue() {
        @Serializable class TestClass(val arg: String? = "test")
        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.StringType
                nullable = true
                unknownDefaultValuePresent = true
                // since String? is nullable, we cannot know for sure the default value is not null
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isTrue()
    }

    @Test
    fun convertNullableWithNullDefaultValue() {
        @Serializable class TestClass(val arg: String? = null)
        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.StringType
                nullable = true
                unknownDefaultValuePresent = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isTrue()
    }

    @Test
    fun convertIllegalCustomType() {
        @Serializable class TestClass(val arg: Set<String>)

        val exception =
            assertFailsWith<IllegalArgumentException> {
                serializer<TestClass>().generateNavArguments()
            }

        assertThat(exception.message)
            .isEqualTo(
                "Cannot cast arg of type kotlin.collections.LinkedHashSet to a NavType. " +
                    "Make sure to provide custom NavType for this argument."
            )
    }

    @Test
    fun convertCustomType() {
        @Serializable class TestClass(val arg: ArrayList<String>)

        val CustomNavType =
            object : NavType<ArrayList<String>>(false) {
                override fun put(bundle: Bundle, key: String, value: ArrayList<String>) {}

                override fun get(bundle: Bundle, key: String): ArrayList<String> = arrayListOf()

                override fun parseValue(value: String): ArrayList<String> = arrayListOf()
            }

        val converted =
            serializer<TestClass>()
                .generateNavArguments(mapOf(typeOf<ArrayList<String>>() to CustomNavType))
        val expected =
            navArgument("arg") {
                type = CustomNavType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertCustomTypeNullable() {
        @Serializable class TestClass(val arg: ArrayList<String>?)

        val CustomNavType =
            object : NavType<ArrayList<String>?>(true) {
                override fun put(bundle: Bundle, key: String, value: ArrayList<String>?) {}

                override fun get(bundle: Bundle, key: String): ArrayList<String> = arrayListOf()

                override fun parseValue(value: String): ArrayList<String> = arrayListOf()
            }

        val converted =
            serializer<TestClass>()
                .generateNavArguments(mapOf(typeOf<ArrayList<String>?>() to CustomNavType))
        val expected =
            navArgument("arg") {
                type = CustomNavType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertCustomTypeNullableIllegal() {
        val CustomNavType =
            object : NavType<ArrayList<String>>(false) {
                override val name = "customNavType"

                override fun put(bundle: Bundle, key: String, value: ArrayList<String>) {}

                override fun get(bundle: Bundle, key: String): ArrayList<String> = arrayListOf()

                override fun parseValue(value: String): ArrayList<String> = arrayListOf()
            }

        // CustomNavType does not allow nullable but we declare the arg as nullable here
        @Serializable class TestClass(val arg: ArrayList<String>?)

        val exception =
            assertFailsWith<IllegalArgumentException> {
                serializer<TestClass>()
                    .generateNavArguments(mapOf(typeOf<ArrayList<String>?>() to CustomNavType))
            }
        assertThat(exception.message).isEqualTo("customNavType does not allow nullable values")
    }

    @Test
    fun convertMultiple() {
        @Serializable class TestClass(val arg: Int, val arg2: String?)

        val converted = serializer<TestClass>().generateNavArguments()
        val expectedInt =
            navArgument("arg") {
                type = NavType.IntType
                nullable = false
            }
        val expectedString =
            navArgument("arg2") {
                type = NavType.StringType
                nullable = true
            }
        assertThat(converted).containsExactlyInOrder(expectedInt, expectedString)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
        assertThat(converted[1].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertMultipleWithDefaultValues() {
        @Serializable class TestClass(val arg: Int = 0, val arg2: String? = "test")

        val converted = serializer<TestClass>().generateNavArguments()
        val expectedInt =
            navArgument("arg") {
                type = NavType.IntType
                nullable = false
                unknownDefaultValuePresent = true
            }
        val expectedString =
            navArgument("arg2") {
                type = NavType.StringType
                nullable = true
                unknownDefaultValuePresent = true
            }
        assertThat(converted).containsExactlyInOrder(expectedInt, expectedString)
        assertThat(converted[0].argument.isDefaultValueUnknown).isTrue()
        assertThat(converted[1].argument.isDefaultValueUnknown).isTrue()
    }

    @Test
    fun convertMultipleCustomTypes() {
        @Serializable class TestClass(val arg: ArrayList<String>?, val arg2: ArrayList<Int>)

        val CustomStringList =
            object : NavType<ArrayList<String>?>(true) {
                override fun put(bundle: Bundle, key: String, value: ArrayList<String>?) {}

                override fun get(bundle: Bundle, key: String): ArrayList<String> = arrayListOf()

                override fun parseValue(value: String): ArrayList<String> = arrayListOf()
            }

        val CustomIntList =
            object : NavType<ArrayList<Int>>(true) {
                override fun put(bundle: Bundle, key: String, value: ArrayList<Int>) {}

                override fun get(bundle: Bundle, key: String): ArrayList<Int> = arrayListOf()

                override fun parseValue(value: String): ArrayList<Int> = arrayListOf()
            }

        val converted =
            serializer<TestClass>()
                .generateNavArguments(
                    mapOf(
                        typeOf<ArrayList<String>?>() to CustomStringList,
                        typeOf<ArrayList<Int>>() to CustomIntList
                    )
                )
        val expectedStringList =
            navArgument("arg") {
                type = CustomStringList
                nullable = true
            }
        val expectedIntList =
            navArgument("arg2") {
                type = CustomIntList
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expectedStringList, expectedIntList)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
        assertThat(converted[1].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertMultipleCustomTypesWithDefaultValue() {
        @Serializable
        class TestClass(
            val arg: ArrayList<String>? = arrayListOf(),
            val arg2: ArrayList<Int> = arrayListOf()
        )

        val CustomStringList =
            object : NavType<ArrayList<String>?>(true) {
                override fun put(bundle: Bundle, key: String, value: ArrayList<String>?) {}

                override fun get(bundle: Bundle, key: String): ArrayList<String> = arrayListOf()

                override fun parseValue(value: String): ArrayList<String> = arrayListOf()
            }

        val CustomIntList =
            object : NavType<ArrayList<Int>>(true) {
                override fun put(bundle: Bundle, key: String, value: ArrayList<Int>) {}

                override fun get(bundle: Bundle, key: String): ArrayList<Int> = arrayListOf()

                override fun parseValue(value: String): ArrayList<Int> = arrayListOf()
            }

        val converted =
            serializer<TestClass>()
                .generateNavArguments(
                    mapOf(
                        typeOf<ArrayList<String>?>() to CustomStringList,
                        typeOf<ArrayList<Int>>() to CustomIntList
                    )
                )
        val expectedStringList =
            navArgument("arg") {
                type = CustomStringList
                nullable = true
                unknownDefaultValuePresent = true
            }
        val expectedIntList =
            navArgument("arg2") {
                type = CustomIntList
                nullable = false
                unknownDefaultValuePresent = true
            }
        assertThat(converted).containsExactlyInOrder(expectedStringList, expectedIntList)
        assertThat(converted[0].argument.isDefaultValueUnknown).isTrue()
        assertThat(converted[1].argument.isDefaultValueUnknown).isTrue()
    }

    @Test
    fun convertNestedCustomTypes() {
        @Serializable class TestClass(val arg: ArrayList<List<String>>)

        val CustomStringList =
            object : NavType<ArrayList<List<String>>>(false) {
                override fun put(bundle: Bundle, key: String, value: ArrayList<List<String>>) {}

                override fun get(bundle: Bundle, key: String): ArrayList<List<String>> =
                    arrayListOf()

                override fun parseValue(value: String): ArrayList<List<String>> = arrayListOf()
            }

        val converted =
            serializer<TestClass>()
                .generateNavArguments(mapOf(typeOf<ArrayList<List<String>>>() to CustomStringList))
        val expectedStringList =
            navArgument("arg") {
                type = CustomStringList
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expectedStringList)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertNativeAndCustomTypes() {
        @Serializable class TestClass(val arg: String, val arg2: ArrayList<Int>)

        val CustomIntList =
            object : NavType<ArrayList<Int>>(true) {
                override fun put(bundle: Bundle, key: String, value: ArrayList<Int>) {}

                override fun get(bundle: Bundle, key: String): ArrayList<Int> = arrayListOf()

                override fun parseValue(value: String): ArrayList<Int> = arrayListOf()
            }

        val converted =
            serializer<TestClass>()
                .generateNavArguments(mapOf(typeOf<ArrayList<Int>>() to CustomIntList))
        val expectedString =
            navArgument("arg") {
                type = NavType.StringType
                nullable = false
            }
        val expectedIntList =
            navArgument("arg2") {
                type = CustomIntList
                nullable = false
            }

        assertThat(converted).containsExactlyInOrder(expectedString, expectedIntList)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
        assertThat(converted[1].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertPrioritizesProvidedNavType() {
        val CustomIntNavType =
            object : NavType<Int>(true) {
                override fun put(bundle: Bundle, key: String, value: Int) {}

                override fun get(bundle: Bundle, key: String): Int = 0

                override fun parseValue(value: String): Int = 0
            }

        @Serializable class TestClass(val arg: Int)

        val converted =
            serializer<TestClass>().generateNavArguments(mapOf(typeOf<Int>() to CustomIntNavType))
        val expected =
            navArgument("arg") {
                type = CustomIntNavType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0]).isNotEqualTo(NavType.IntType)
        assertThat(converted[0].argument.isDefaultValueUnknown).isFalse()
    }

    @Test
    fun convertOnlyIfArgHasBackingField() {
        @Serializable
        class TestClass {
            val noBackingField: Int
                get() = 0
        }

        val converted = serializer<TestClass>().generateNavArguments()
        assertThat(converted).isEmpty()
    }

    @Test
    fun convertArgFromClassBody() {
        @Serializable
        class TestClass {
            val arg: Int = 0
        }

        val converted = serializer<TestClass>().generateNavArguments()
        val expected =
            navArgument("arg") {
                type = NavType.IntType
                nullable = false
                unknownDefaultValuePresent = true
            }
        assertThat(converted).containsExactlyInOrder(expected)
        assertThat(converted[0].argument.isDefaultValueUnknown).isTrue()
    }

    @Test
    fun nonSerializableClassInvalid() {
        @SerialName(PATH_SERIAL_NAME) class TestClass

        assertFailsWith<SerializationException> {
            // the class must be serializable
            serializer<TestClass>().generateNavArguments()
        }
    }

    @Test
    fun abstractClassInvalid() {
        @Serializable abstract class TestClass(val arg: Int)

        val serializer = serializer<TestClass>()
        val exception =
            assertFailsWith<IllegalArgumentException> { serializer.generateNavArguments() }
        assertThat(exception.message)
            .isEqualTo(
                "Cannot generate NavArguments for polymorphic serializer " +
                    "kotlinx.serialization.PolymorphicSerializer(baseClass: " +
                    "class androidx.navigation.serialization." +
                    "NavArgumentGeneratorTest\$abstractClassInvalid\$TestClass (Kotlin reflection " +
                    "is not available)). Arguments can only be generated from concrete classes " +
                    "or objects."
            )
    }

    @Test
    fun childClassOfAbstract_duplicateArgs() {
        @Serializable abstract class TestAbstractClass(val arg: Int)

        @Serializable class TestClass(val arg2: Int) : TestAbstractClass(0)

        val serializer = serializer<TestClass>()
        val converted = serializer.generateNavArguments()
        // args will be duplicated
        val expectedInt =
            navArgument("arg") {
                type = NavType.IntType
                nullable = false
            }
        val expectedInt2 =
            navArgument("arg2") {
                type = NavType.IntType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expectedInt, expectedInt2)
    }

    @Test
    fun childClassOfSealed_withArgs() {
        val serializer = serializer<SealedClass.TestClass>()
        val converted = serializer.generateNavArguments()
        // child class overrides parent variable so only child variables are generated as args
        val expected =
            navArgument("arg2") {
                type = NavType.IntType
                nullable = false
            }
        assertThat(converted).containsExactlyInOrder(expected)
    }

    // writing our own assert so we don't need to override NamedNavArgument's equals
    // and hashcode which will need to be public api.
    private fun assertThat(actual: List<NamedNavArgument>) = actual

    private fun List<NamedNavArgument>.containsExactlyInOrder(
        vararg expectedArgs: NamedNavArgument
    ) {
        if (expectedArgs.size != this.size) {
            fail("expected list has size ${expectedArgs.size} and actual list has size $size}")
        }
        for (i in indices) {
            val actual = this[i]
            val expected = expectedArgs[i]
            if (expected.name != actual.name) {
                fail("expected name ${expected.name}, was actually ${actual.name}")
            }

            if (!expected.argument.isEqual(actual.argument)) {
                fail(
                    """expected ${expected.name} to be:
                |   ${expected.argument}
                |   but was:
                |   ${actual.argument}
                """
                        .trimMargin()
                )
            }
        }
    }

    private fun NavArgument.isEqual(other: NavArgument): Boolean {
        if (this === other) return true
        if (javaClass != other.javaClass) return false
        if (isNullable != other.isNullable) return false
        if (isDefaultValuePresent != other.isDefaultValuePresent) return false
        if (isDefaultValueUnknown != other.isDefaultValueUnknown) return false
        if (type != other.type) return false
        // In context of serialization, we can only tell if defaultValue is present but don't know
        // actual value, so we cannot compare it to the generated defaultValue. But if
        // there is no defaultValue, we expect them both to be null.
        return if (!isDefaultValuePresent) {
            defaultValue == null && other.defaultValue == null
        } else true
    }

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
