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

package androidx.navigation.testing

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.CollectionNavType
import androidx.navigation.NavType
import androidx.navigation.toRoute
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.reflect.typeOf
import kotlinx.serialization.Serializable
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@SmallTest
@RunWith(RobolectricTestRunner::class)
class TestSavedStateHandleBuilder {

    @Test
    fun primitiveArgument() {
        @Serializable class TestClass(val arg: Int)

        val handle = SavedStateHandle(TestClass(12))
        assertThat(handle.contains("arg")).isTrue()
        val arg = handle.get<Int>("arg")
        assertThat(arg).isEqualTo(12)
    }

    @Test
    fun complexPathArgument() {
        @Serializable class TestClass(val arg: TestType)

        val typeMap = mapOf(typeOf<TestType>() to testNavType)
        val handle = SavedStateHandle(TestClass(TestType("test", 1)), typeMap)
        assertThat(handle.contains("arg")).isTrue()
        val arg = handle.get<String>("arg")
        assertThat(arg).isEqualTo("1.test")
    }

    @Test
    fun complexQueryArgument() {
        @Serializable class TestClass(val arg: List<TestType>)

        val arg = listOf(TestType("test", 1), TestType("test2", 2))
        val typeMap = mapOf(typeOf<List<TestType>>() to testCollectionNavType)
        val handle = SavedStateHandle(TestClass(arg), typeMap)
        assertThat(handle.contains("arg")).isTrue()
        val retrievedArg = handle.get<Array<String>>("arg")
        assertThat(retrievedArg.contentEquals(arrayOf("1.test", "2.test2"))).isTrue()
    }

    @Test
    fun multipleArgument() {
        @Serializable class TestClass(val arg: Boolean, val arg2: Float)

        val handle = SavedStateHandle(TestClass(true, 1.0F))
        assertThat(handle.contains("arg")).isTrue()
        assertThat(handle.contains("arg2")).isTrue()

        val arg = handle.get<Boolean>("arg")
        assertThat(arg).isEqualTo(true)

        val arg2 = handle.get<Float>("arg2")
        assertThat(arg2).isEqualTo(1.0F)
    }

    @Test
    fun nullArgument() {
        @Serializable class TestClass(val arg: String?)

        val handle = SavedStateHandle(TestClass(null))
        assertThat(handle.contains("arg")).isTrue()
        val arg = handle.get<TestType>("arg")
        assertThat(arg).isNull()
    }

    @Test
    fun nullLiteralArgument() {
        @Serializable class TestClass(val arg: String)

        val handle = SavedStateHandle(TestClass("null"))
        assertThat(handle.contains("arg")).isTrue()
        val arg = handle.get<String>("arg")
        assertThat(arg).isEqualTo("null")
    }

    @Test
    fun emptyStringArgument() {
        @Serializable class TestClass(val arg: String)

        val handle = SavedStateHandle(TestClass(""))
        assertThat(handle.contains("arg")).isTrue()
        val arg = handle.get<String>("arg")
        assertThat(arg).isEqualTo("")
    }

    @Test
    fun emptyStringListArgument() {
        @Serializable class TestClass(val arg: List<String>)

        val handle = SavedStateHandle(TestClass(emptyList()))
        assertThat(handle.contains("arg")).isTrue()
        val route = handle.toRoute<TestClass>()
        assertThat(route.arg).isEmpty()
    }

    @Test
    fun nullStringListArgumentEmptyList() {
        @Serializable class TestClass(val arg: List<String>?)

        val handle = SavedStateHandle(TestClass(null))
        // on null list, we default to an empty list if there is not defaultValue
        assertThat(handle.contains("arg")).isTrue()
        val route = handle.toRoute<TestClass>()
        assertThat(route.arg).isEmpty()
    }

    @Test
    fun nullStringListArgumentUseDefault() {
        @Serializable class TestClass(val arg: List<String>? = null)

        val handle = SavedStateHandle(TestClass())
        // on null list, we default to the default value since it is present, so the handle
        // here will not contain the arg. The arg will be auto-populated during decoding.
        assertThat(handle.contains("arg")).isFalse()
        val route = handle.toRoute<TestClass>()
        assertThat(route.arg).isNull()
    }

    @Test
    fun emptyStringListArgumentUseDefault() {
        @Serializable class TestClass(val arg: List<String> = listOf("one", "two"))

        val handle = SavedStateHandle(TestClass(emptyList()))
        // on empty list, we default to the default value since it is present, so the handle
        // here will not contain the arg. The arg will be auto-populated during decoding.
        assertThat(handle.contains("arg")).isFalse()
        val route = handle.toRoute<TestClass>()
        assertThat(route.arg).containsExactly("one", "two")
    }

    @Test
    fun emptyIntListArgument() {
        @Serializable class TestClass(val arg: List<Int>)

        val handle = SavedStateHandle(TestClass(emptyList()))
        assertThat(handle.contains("arg")).isTrue()
        val route = handle.toRoute<TestClass>()
        assertThat(route.arg).isEmpty()
    }

    @Test
    fun emptyIntListArgumentUseDefault() {
        @Serializable class TestClass(val arg: List<Int> = listOf(1, 2))

        val handle = SavedStateHandle(TestClass(emptyList()))
        // on empty list, we default to the default value since it is present, so the handle
        // here will not contain the arg. The arg will be auto-populated during decoding.
        assertThat(handle.contains("arg")).isFalse()
        val route = handle.toRoute<TestClass>()
        assertThat(route.arg).containsExactly(1, 2)
    }

    @Test
    fun nullIntListArgumentEmptyList() {
        @Serializable class TestClass(val arg: List<String>?)

        val handle = SavedStateHandle(TestClass(null))
        // on null list, we default to an empty list if there is not defaultValue
        assertThat(handle.contains("arg")).isTrue()
        val route = handle.toRoute<TestClass>()
        assertThat(route.arg).isEmpty()
    }

    @Test
    fun nullIntListArgumentUseDefault() {
        @Serializable class TestClass(val arg: List<String>? = null)

        val handle = SavedStateHandle(TestClass())
        // on null list, we default to the default value since it is present, so the handle
        // here will not contain the arg. The arg will be auto-populated during decoding.
        assertThat(handle.contains("arg")).isFalse()
        val route = handle.toRoute<TestClass>()
        assertThat(route.arg).isNull()
    }

    @Test
    fun defaultPrimitiveArgument() {
        @Serializable class TestClass(val arg: Int = 1)

        val handle = SavedStateHandle(TestClass())
        assertThat(handle.contains("arg")).isTrue()
        val arg = handle.get<Int>("arg")
        assertThat(arg).isEqualTo(1)
    }

    @Test
    fun defaultComplexArgument() {
        @Serializable class TestClass(val arg: TestType = TestType("test", 1))

        val typeMap = mapOf(typeOf<TestType>() to testNavType)
        val handle = SavedStateHandle(TestClass(), typeMap)
        assertThat(handle.contains("arg")).isTrue()
        val arg = handle.get<String>("arg")
        assertThat(arg).isEqualTo("1.test")
    }

    @Test
    fun handleToRoutePathArg() {
        @Serializable class TestClass(val arg: TestType)

        val typeMap = mapOf(typeOf<TestType>() to testNavType)
        val handle = SavedStateHandle(TestClass(TestType("test", 1)), typeMap)

        val route = handle.toRoute<TestClass>(typeMap)
        assertThat(route.arg.name).isEqualTo("test")
        assertThat(route.arg.id).isEqualTo(1)
    }

    @Test
    fun handleToRouteQueryArg() {
        @Serializable class TestClass(val arg: List<TestType>)

        val arg = listOf(TestType("test", 1), TestType("test2", 2))
        val typeMap = mapOf(typeOf<List<TestType>>() to testCollectionNavType)
        val handle = SavedStateHandle(TestClass(arg), typeMap)
        val route = handle.toRoute<TestClass>(typeMap)
        assertThat(route.arg).containsExactlyElementsIn(arg).inOrder()
    }
}

@Serializable private data class TestType(val name: String, val id: Int)

private val testNavType =
    object : NavType<TestType>(false) {
        override fun put(bundle: Bundle, key: String, value: TestType) {
            bundle.putString(key, serializeAsValue(value))
        }

        override fun get(bundle: Bundle, key: String): TestType =
            parseValue(bundle.getString(key) as String)

        override fun parseValue(value: String): TestType {
            val args = value.split(".")
            return TestType(id = args.first().toInt(), name = args.last())
        }

        override fun serializeAsValue(value: TestType) = "${value.id}.${value.name}"
    }

private val testCollectionNavType: NavType<List<TestType>> =
    object : CollectionNavType<List<TestType>>(false) {
        override fun serializeAsValues(value: List<TestType>): List<String> =
            value.map { "${it.id}.${it.name}" }

        override fun put(bundle: Bundle, key: String, value: List<TestType>) {
            bundle.putStringArray(key, serializeAsValues(value).toTypedArray())
        }

        override fun emptyCollection(): List<TestType> = emptyList()

        override fun get(bundle: Bundle, key: String): List<TestType> {
            return bundle.getStringArray(key)!!.map {
                val args = it.split(".")
                TestType(id = args.first().toInt(), name = args.last())
            }
        }

        override fun parseValue(value: String) = listOf(testNavType.parseValue(value))

        override fun parseValue(value: String, previousValue: List<TestType>): List<TestType> =
            previousValue.plus(testNavType.parseValue(value))
    }
