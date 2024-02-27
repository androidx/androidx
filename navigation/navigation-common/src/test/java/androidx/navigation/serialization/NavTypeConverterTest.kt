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
import androidx.navigation.NavType
import com.google.common.truth.Truth.assertThat
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertFails
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NavTypeConverterTest {

    @Test
    fun matchInt() {
        val descriptor = serializer<Int>().descriptor
        val kType = typeOf<Int>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchIntNullable() {
        val descriptor = serializer<Int?>().descriptor
        val kType = typeOf<Int?>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<Int>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchBoolean() {
        val descriptor = serializer<Boolean>().descriptor
        val kType = typeOf<Boolean>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchBooleanNullable() {
        val descriptor = serializer<Boolean?>().descriptor
        val kType = typeOf<Boolean?>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<Boolean>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchFloat() {
        val descriptor = serializer<Float>().descriptor
        val kType = typeOf<Float>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchFloatNullable() {
        val descriptor = serializer<Float?>().descriptor
        val kType = typeOf<Float?>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<Float>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchLong() {
        val descriptor = serializer<Long>().descriptor
        val kType = typeOf<Long>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchLongNullable() {
        val descriptor = serializer<Long?>().descriptor
        val kType = typeOf<Long?>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<Long>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchString() {
        val descriptor = serializer<String>().descriptor
        val kType = typeOf<String>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchStringNullable() {
        val descriptor = serializer<String?>().descriptor
        val kType = typeOf<String?>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<String>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchIntArray() {
        val descriptor = serializer<IntArray>().descriptor
        val kType = typeOf<IntArray>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchIntArrayNullable() {
        val descriptor = serializer<IntArray?>().descriptor
        val kType = typeOf<IntArray?>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<IntArray>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchArrayOfInt() {
        val descriptor = serializer<Array<Int>>().descriptor
        val kType = typeOf<Array<Int>>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val wrongTypeParameter = serializer<Array<Boolean>>().descriptor
        assertThat(wrongTypeParameter.matchKType(kType)).isFalse()
    }

    @Test
    fun matchBooleanArray() {
        val descriptor = serializer<BooleanArray>().descriptor
        val kType = typeOf<BooleanArray>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchBooleanArrayNullable() {
        val descriptor = serializer<BooleanArray?>().descriptor
        val kType = typeOf<BooleanArray?>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<BooleanArray>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchFloatArray() {
        val descriptor = serializer<FloatArray>().descriptor
        val kType = typeOf<FloatArray>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchFloatArrayNullable() {
        val descriptor = serializer<FloatArray?>().descriptor
        val kType = typeOf<FloatArray?>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<FloatArray>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchLongArray() {
        val descriptor = serializer<LongArray>().descriptor
        val kType = typeOf<LongArray>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchLongArrayNullable() {
        val descriptor = serializer<LongArray?>().descriptor
        val kType = typeOf<LongArray?>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<LongArray>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchStringArray() {
        val descriptor = serializer<Array<String>>().descriptor
        val kType = typeOf<Array<String>>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchStringArrayNullable() {
        val descriptor = serializer<Array<String>?>().descriptor
        val kType = typeOf<Array<String>?>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<Array<String>>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchArrayList() {
        val descriptor = serializer<ArrayList<Int>>().descriptor
        val kType = typeOf<ArrayList<Int>>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchArrayListNullable() {
        val descriptor = serializer<ArrayList<Int>?>().descriptor
        val kType = typeOf<ArrayList<Int>?>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<ArrayList<Int>>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchArrayListTypeParamNullable() {
        val descriptor = serializer<ArrayList<Int?>>().descriptor
        val kType = typeOf<ArrayList<Int?>>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<ArrayList<Int>>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchArrayListAllNullable() {
        val descriptor = serializer<ArrayList<Int?>?>().descriptor
        val kType = typeOf<ArrayList<Int?>?>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchSet() {
        val descriptor = serializer<Set<Int>>().descriptor
        val kType = typeOf<Set<Int>>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchSetNullable() {
        val descriptor = serializer<Set<Int>?>().descriptor
        val kType = typeOf<Set<Int>?>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<Set<Int>>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchSetTypeParamNullable() {
        val descriptor = serializer<Set<Int?>>().descriptor
        val kType = typeOf<Set<Int?>>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<Set<Int>>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchSetAllNullable() {
        val descriptor = serializer<Set<Int?>?>().descriptor
        val kType = typeOf<Set<Int?>?>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchMutableSet() {
        val descriptor = serializer<MutableSet<Int>>().descriptor
        val kType = typeOf<MutableSet<Int>>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchMutableSetAndSet() {
        val descriptor = serializer<MutableSet<Int>>().descriptor
        val kType = typeOf<Set<Int>>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchHashSet() {
        val descriptor = serializer<HashSet<Int>>().descriptor
        val kType = typeOf<HashSet<Int>>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchHashSetNullable() {
        val descriptor = serializer<HashSet<Int>?>().descriptor
        val kType = typeOf<HashSet<Int>?>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<Set<Int>>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchHashSetTypeParamNullable() {
        val descriptor = serializer<HashSet<Int?>>().descriptor
        val kType = typeOf<HashSet<Int?>>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<HashSet<Int>>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchHashSetAllNullable() {
        val descriptor = serializer<HashSet<Int?>?>().descriptor
        val kType = typeOf<HashSet<Int?>?>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchMap() {
        val descriptor = serializer<Map<Int, String>>().descriptor
        val kType = typeOf<Map<Int, String>>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchMapNullable() {
        val descriptor = serializer<Map<Int, String>?>().descriptor
        val kType = typeOf<Map<Int, String>?>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<Set<Int>>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchMapTypeParamNullable() {
        val descriptor = serializer<Map<Int?, String>>().descriptor
        val kType = typeOf<Map<Int?, String>>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val nonNullable = serializer<Map<Int, String>>().descriptor
        assertThat(nonNullable.matchKType(kType)).isFalse()
    }

    @Test
    fun matchMapAllNullable() {
        val descriptor = serializer<Map<Int?, String?>?>().descriptor
        val kType = typeOf<Map<Int?, String?>?>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchObject() {
        val descriptor = serializer<TestObject>().descriptor
        val kType = typeOf<TestObject>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchEnumClass() {
        val descriptor = serializer<TestEnum>().descriptor
        val kType = typeOf<TestEnum>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchWrongTypeParameter() {
        val descriptor = serializer<Set<Int>>().descriptor
        val kType = typeOf<Set<Boolean>>()
        assertThat(descriptor.matchKType(kType)).isFalse()
    }

    @Test
    fun matchWrongOrderTypeParameter() {
        val descriptor = serializer<Map<String, Int>>().descriptor
        val kType = typeOf<Map<Int, String>>()
        assertThat(descriptor.matchKType(kType)).isFalse()
    }

    @Test
    fun matchNestedTypeParameter() {
        val descriptor = serializer<List<List<Int>>>().descriptor
        val kType = typeOf<List<List<Int>>>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val notNested = serializer<List<Int>>().descriptor
        assertThat(notNested.matchKType(kType)).isFalse()
    }

    @Test
    fun matchMultiNestedTypeParameter() {
        val descriptor = serializer<Map<List<Int>, Set<Boolean>>>().descriptor
        val kType = typeOf<Map<List<Int>, Set<Boolean>>>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val wrongOrder = serializer<Map<Set<Boolean>, List<Int>>>().descriptor
        assertThat(wrongOrder.matchKType(kType)).isFalse()
    }

    @Test
    fun matchThriceNestedTypeParameter() {
        val descriptor = serializer<List<Set<List<Boolean>>>>().descriptor
        val kType = typeOf<List<Set<List<Boolean>>>>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchNativeClassWithCustomTypeParameter() {
        @Serializable
        class TestClass(val arg: Int, val arg2: String)

        val descriptor = serializer<List<TestClass>>().descriptor
        val kType = typeOf<List<TestClass>>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchNativeClassWithNestedCustomTypeParameter() {
        @Serializable
        open class Nested(val arg: Int)

        @Serializable
        class TestClass<T : Nested>(val arg: Nested)

        val descriptor = serializer<List<TestClass<Nested>>>().descriptor
        val kType = typeOf<List<TestClass<Nested>>>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

    @Test
    fun matchCustomClass() {
        @Serializable
        class TestClass(val arg: Int, val arg2: String)

        val descriptor = serializer<TestClass>().descriptor
        val kType = typeOf<TestClass>()
        assertThat(descriptor.matchKType(kType)).isTrue()
    }

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
    fun matchCustomClassTypeParameterWithSingleArg() {
        @Serializable
        class TestClass<T : Any>(val arg: T)

        val descriptor = serializer<TestClass<String>>().descriptor
        val kType = typeOf<TestClass<String>>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val descriptor2 = serializer<TestClass<Int>>().descriptor
        assertThat(descriptor2.matchKType(kType)).isFalse()
    }

    @Test
    fun matchCustomClassTypeParameterWithMultipleArg() {
        @Serializable
        class TestClass<T : Any, K : Any>(val arg: T, val arg2: K, val arg3: Int)

        val descriptor = serializer<TestClass<String, Int>>().descriptor
        val kType = typeOf<TestClass<String, Int>>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val descriptor2 = serializer<TestClass<Int, String>>().descriptor
        assertThat(descriptor2.matchKType(kType)).isFalse()
    }

    @Test
    fun matchCustomClassTypeParameterWithNoArgFails() {
        @Serializable
        class TestClass<T : Any>

        val descriptor = serializer<TestClass<Int>>().descriptor
        val kType = typeOf<TestClass<String>>()

        // type T is erased in serialization so we cannot differentiate between Int/String
        val isMatch = descriptor.matchKType(kType)
        val result = assertFails {
            assertThat(isMatch).isFalse()
        }
        assertThat(result.message).isEqualTo("expected to be false")
    }

    @Test
    fun matchCustomNestedType() {
        val descriptor = serializer<TestBaseClass.Nested>().descriptor
        val kType = typeOf<TestBaseClass.Nested>()
        assertThat(descriptor.matchKType(kType)).isTrue()

        val baseDescriptor = serializer<TestBaseClass>().descriptor
        assertThat(baseDescriptor.matchKType(kType)).isFalse()
    }

    @Test
    fun matchChildOfAbstract() {
        @Serializable
        abstract class Abstract

        @Serializable
        class FirstChild : Abstract()

        @Serializable
        class SecondChild : Abstract()

        val firstChildDescriptor = serializer<FirstChild>().descriptor
        val kType = typeOf<FirstChild>()
        assertThat(firstChildDescriptor.matchKType(kType)).isTrue()

        val secondChildDescriptor = serializer<SecondChild>().descriptor
        assertThat(secondChildDescriptor.matchKType(kType)).isFalse()
    }

    @Test
    fun getNavTypeNativePrimitive() {
        val intType = serializer<Int>().descriptor.getNavType()
        assertThat(intType).isEqualTo(NavType.IntType)

        val boolType = serializer<Boolean>().descriptor.getNavType()
        assertThat(boolType).isEqualTo(NavType.BoolType)

        val floatType = serializer<Float>().descriptor.getNavType()
        assertThat(floatType).isEqualTo(NavType.FloatType)

        val longType = serializer<Long>().descriptor.getNavType()
        assertThat(longType).isEqualTo(NavType.LongType)

        val stringType = serializer<String>().descriptor.getNavType()
        assertThat(stringType).isEqualTo(NavType.StringType)
    }

    @Test
    fun getNavTypeNativeArray() {
        val intType = serializer<IntArray>().descriptor.getNavType()
        assertThat(intType).isEqualTo(NavType.IntArrayType)

        val boolType = serializer<BooleanArray>().descriptor.getNavType()
        assertThat(boolType).isEqualTo(NavType.BoolArrayType)

        val floatType = serializer<FloatArray>().descriptor.getNavType()
        assertThat(floatType).isEqualTo(NavType.FloatArrayType)

        val longType = serializer<LongArray>().descriptor.getNavType()
        assertThat(longType).isEqualTo(NavType.LongArrayType)

        val stringType = serializer<Array<String>>().descriptor.getNavType()
        assertThat(stringType).isEqualTo(NavType.StringArrayType)
    }

    @Test
    fun getNavTypeParcelable() {
        val type = serializer<TestParcelable>().descriptor.getNavType()
        assertThat(type).isEqualTo(NavType.ParcelableType(TestParcelable::class.java))
    }

    @Test
    fun getNavTypeParcelableArray() {
        val type = serializer<Array<TestParcelable>>().descriptor.getNavType()
        assertThat(type).isEqualTo(
            NavType.ParcelableArrayType(TestParcelable::class.java)
        )
    }

    @Test
    fun getNavTypeSerializable() {
        val type = serializer<TestSerializable>().descriptor.getNavType()
        assertThat(type).isEqualTo(
            NavType.SerializableType(TestSerializable::class.java)
        )
    }

    @Test
    fun getNavTypeSerializableArray() {
        val type = serializer<Array<TestSerializable>>().descriptor.getNavType()
        assertThat(type).isEqualTo(
            NavType.SerializableArrayType(TestSerializable::class.java)
        )
    }

    @Test
    fun getNavTypeEnumSerializable() {
        val type = serializer<TestEnum>().descriptor.getNavType()
        assertThat(type).isEqualTo(
            NavType.EnumType(TestEnum::class.java)
        )
    }

    @Test
    fun getNavTypeEnumArraySerializable() {
        val type = serializer<Array<TestEnum>>().descriptor.getNavType()
        assertThat(type).isEqualTo(
            NavType.SerializableArrayType(TestEnum::class.java)
        )
    }

    @Test
    fun getNavTypeUnsupportedArray() {
        assertThat(serializer<Array<Double>>().descriptor.getNavType()).isEqualTo(UNKNOWN)

        @Serializable
        class TestClass
        assertThat(serializer<Array<TestClass>>().descriptor.getNavType()).isEqualTo(UNKNOWN)

        assertThat(serializer<Array<List<Double>>>().descriptor.getNavType()).isEqualTo(UNKNOWN)
    }

    @Serializable
    class TestBaseClass(val arg: Int) {
        @Serializable
        class Nested
    }

    @Serializable
    class TestObject {
        val arg: String = "test"
    }

    @Serializable
    enum class TestEnum {
        First,
        Second
    }

    @Serializable
    class TestParcelable(val arg: Int, val arg2: String) : Parcelable {
        override fun describeContents() = 0
        override fun writeToParcel(dest: Parcel, flags: Int) { }
    }

    @Serializable
    class TestSerializable(val arg: Int, val arg2: String) : java.io.Serializable
}
