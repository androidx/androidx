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
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.common.test.R
import androidx.navigation.navArgument
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

enum class ArgumentSource {
    BUNDLE,
    SAVED_STATE_HANDLE
}

@RunWith(JUnit4::class)
class RouteDecoderSavedStateTest : RouteDecoderTest(ArgumentSource.SAVED_STATE_HANDLE)

@RunWith(JUnit4::class)
class RouteDecoderBundleTest : RouteDecoderTest(ArgumentSource.BUNDLE)

abstract class RouteDecoderTest(val source: ArgumentSource) {

    @Test
    fun decodeString() {
        @Serializable
        data class TestClass(val arg: String)

        val bundle = bundleOf("arg" to "theArg")
        val result = decode<TestClass>(bundle, listOf(stringArgument("arg")))
        assertThat(result.arg).isEqualTo("theArg")
    }

    @Test
    fun decodeInt() {
        @Serializable
        class TestClass(val arg: Int)

        val bundle = bundleOf("arg" to 15)
        val result = decode<TestClass>(bundle, listOf(intArgument("arg")))
        assertThat(result.arg).isEqualTo(15)
    }

    @Test
    fun decodeBoolean() {
        @Serializable
        class TestClass(val arg: Boolean)

        val bundle = bundleOf("arg" to false)
        val result = decode<TestClass>(bundle, listOf(
            navArgument("arg") {
                type = NavType.BoolType
            }
        ))
        assertThat(result.arg).isEqualTo(false)
    }

    @Test
    fun decodeLong() {
        @Serializable
        class TestClass(val arg: Long)

        val bundle = bundleOf("arg" to 1L)
        val result = decode<TestClass>(bundle, listOf(
            navArgument("arg") {
                type = NavType.LongType
            }
        ))
        assertThat(result.arg).isEqualTo(1L)
    }

    @Test
    fun decodeFloat() {
        @Serializable
        class TestClass(val arg: Float)

        val bundle = bundleOf("arg" to 1.0F)
        val result = decode<TestClass>(bundle, listOf(
            navArgument("arg") {
                type = NavType.FloatType
            }
        ))
        assertThat(result.arg).isEqualTo(1.0F)
    }

    @Test
    fun decodeReference() {
        @Serializable
        class TestClass(val arg: Int)
        val bundle = bundleOf("arg" to R.id.nav_id_reference)
        val result = decode<TestClass>(bundle, listOf(
            navArgument("arg") {
                type = NavType.ReferenceType
            }
        ))
        assertThat(result.arg).isEqualTo(R.id.nav_id_reference)
    }

    @Test
    fun decodeStringArray() {
        @Serializable
        data class TestClass(val arg: Array<String>)

        val expected = arrayOf("arg1", "arg")
        val bundle = bundleOf("arg" to expected)
        val result = decode<TestClass>(bundle, listOf(
            navArgument("arg") {
                type = NavType.StringArrayType
            }
        ))
        assertThat(result.arg).isEqualTo(expected)
    }

    @Test
    fun decodeIntArray() {
        @Serializable
        class TestClass(val arg: IntArray)

        val bundle = bundleOf("arg" to intArrayOf(0, 1, 2, 3))
        val result = decode<TestClass>(bundle, listOf(
            navArgument("arg") {
                type = NavType.IntArrayType
            }
        ))
        assertThat(result.arg).isEqualTo(intArrayOf(0, 1, 2, 3))
    }

    @Test
    fun decodeBooleanArray() {
        @Serializable
        class TestClass(val arg: BooleanArray)

        val bundle = bundleOf("arg" to booleanArrayOf(false, true))
        val result = decode<TestClass>(bundle, listOf(
            navArgument("arg") {
                type = NavType.BoolArrayType
            }
        ))
        assertThat(result.arg).isEqualTo(booleanArrayOf(false, true))
    }

    @Test
    fun decodeLongArray() {
        @Serializable
        class TestClass(val arg: LongArray)

        val bundle = bundleOf("arg" to longArrayOf(1L, 2L))
        val result = decode<TestClass>(bundle, listOf(
            navArgument("arg") {
                type = NavType.LongArrayType
            }
        ))
        assertThat(result.arg).isEqualTo(longArrayOf(1L, 2L))
    }

    @Test
    fun decodeFloatArray() {
        @Serializable
        class TestClass(val arg: FloatArray)

        val bundle = bundleOf("arg" to floatArrayOf(1.0F, 1.5F))
        val result = decode<TestClass>(bundle, listOf(
            navArgument("arg") {
                type = NavType.FloatArrayType
            }
        ))
        assertThat(result.arg).isEqualTo(floatArrayOf(1.0F, 1.5F))
    }

    @Test
    fun decodeIntString() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: Int, val arg2: String)

        val bundle = bundleOf("arg" to 15, "arg2" to "theArg")
        val result = decode<TestClass>(
            bundle,
            listOf(stringArgument("arg2"), intArgument("arg"))
        )
        assertThat(result.arg).isEqualTo(15)
        assertThat(result.arg2).isEqualTo("theArg")
    }

    @Test
    fun decodeCustomType() {
        @Serializable
        class CustomType(val nestedArg: Int) : Parcelable {
            override fun describeContents() = 0
            override fun writeToParcel(dest: Parcel, flags: Int) {}
        }

        @Serializable
        class TestClass(val custom: CustomType)

        @Suppress("DEPRECATION")
        val customArg = navArgument("custom") {
            type = object : NavType<CustomType>(false) {
                override fun put(bundle: Bundle, key: String, value: CustomType) {
                    bundle.putString(key, value.nestedArg.toString())
                }
                override fun get(bundle: Bundle, key: String): CustomType =
                    CustomType(nestedArg = bundle.getString(key)!!.toInt())
                override fun parseValue(value: String): CustomType = CustomType(15)
                override fun serializeAsValue(value: CustomType) = ""
            }
        }
        val bundle = Bundle().apply {
            customArg.argument.type.put(this, "custom", CustomType(1))
        }
        val result = decode<TestClass>(
            bundle,
            listOf(customArg)
        )
        assertThat(result.custom.nestedArg).isEqualTo(1)
    }

    @Test
    fun decodeNullLiteral() {
        @Serializable
        data class TestClass(val arg: String)

        val bundle = bundleOf("arg" to "null")
        val result = decode<TestClass>(bundle, listOf(stringArgument("arg")))
        assertThat(result.arg).isEqualTo("null")
    }

    @Test
    fun decodeDefaultValue() {
        @Serializable
        data class TestClass(val arg: String = "defaultValue")

        val bundle = bundleOf()
        val result = decode<TestClass>(
            bundle,
            listOf(stringArgument("arg", true))
        )
        assertThat(result.arg).isEqualTo("defaultValue")
    }

    @Test
    fun decodeMultipleDefaultValue() {
        @Serializable
        data class TestClass(val arg: String = "defaultValue", val arg2: Int = 0)

        val bundle = bundleOf()
        val result = decode<TestClass>(
            bundle,
            listOf(
                stringArgument("arg", true),
                intArgument("arg2", true)
            )
        )
        assertThat(result.arg).isEqualTo("defaultValue")
        assertThat(result.arg2).isEqualTo(0)
    }

    @Test
    fun decodeDefaultValueOverridden() {
        @Serializable
        data class TestClass(val arg: String = "defaultValue")

        val bundle = bundleOf("arg" to "newValue")
        val result = decode<TestClass>(
            bundle,
            listOf(stringArgument("arg", true))
        )
        assertThat(result.arg).isEqualTo("newValue")
    }

    @Test
    fun decodeMultipleDefaultValueOverridden() {
        @Serializable
        data class TestClass(val arg: String = "defaultValue", val arg2: Int = 0)

        val bundle = bundleOf("arg" to "newValue", "arg2" to 1)
        val result = decode<TestClass>(
            bundle,
            listOf(
                stringArgument("arg", true),
                intArgument("arg2", hasDefaultValue = true)
            )
        )
        assertThat(result.arg).isEqualTo("newValue")
        assertThat(result.arg2).isEqualTo(1)
    }

    @Test
    fun decodePartialDefaultValue() {
        @Serializable
        data class TestClass(val arg: String = "defaultValue", val arg2: Int)

        val bundle = bundleOf("arg2" to 1)
        val result = decode<TestClass>(
            bundle,
            listOf(
                stringArgument("arg", true),
                intArgument("arg2", false)
            )
        )
        assertThat(result.arg).isEqualTo("defaultValue")
        assertThat(result.arg2).isEqualTo(1)
    }

    @Test
    fun decodeNullPrimitive() {
        @Serializable
        data class TestClass(val arg: String?)

        val bundle = bundleOf("arg" to null)
        val result = decode<TestClass>(bundle, listOf(nullableStringArgument("arg")))
        assertThat(result.arg).isEqualTo(null)
    }

    @Test
    fun decodeNullCustom() {
        @Serializable
        class CustomType : Parcelable {
            override fun describeContents() = 0
            override fun writeToParcel(dest: Parcel, flags: Int) {}
        }

        @Serializable
        class TestClass(val custom: CustomType?)

        @Suppress("DEPRECATION")
        val customArg = navArgument("custom") {
            type = object : NavType<CustomType>(true) {
                override fun put(bundle: Bundle, key: String, value: CustomType) { }
                override fun get(bundle: Bundle, key: String): CustomType? =
                    bundle[key] as? CustomType
                override fun parseValue(value: String): CustomType = CustomType()
                override fun serializeAsValue(value: CustomType) = ""
            }
        }
        val bundle = bundleOf("custom" to null)
        val result = decode<TestClass>(
            bundle,
            listOf(customArg)
        )
        assertThat(result.custom).isNull()
    }

    @Test
    fun decodeNestedNull() {
        @Serializable
        class CustomType(val arg: Int?) : Parcelable {
            override fun describeContents() = 0
            override fun writeToParcel(dest: Parcel, flags: Int) {}
        }

        @Serializable
        class TestClass(val custom: CustomType)

        @Suppress("DEPRECATION")
        val customArg = navArgument("custom") {
            type = object : NavType<CustomType>(false) {
                override fun put(bundle: Bundle, key: String, value: CustomType) { }
                override fun get(bundle: Bundle, key: String): CustomType? =
                    bundle[key] as? CustomType
                override fun parseValue(value: String): CustomType = CustomType(0)
                override fun serializeAsValue(value: CustomType) = ""
            }
        }
        val bundle = bundleOf("custom" to CustomType(null))
        val result = decode<TestClass>(
            bundle,
            listOf(customArg)
        )
        assertThat(result.custom.arg).isNull()
    }

    @Test
    fun decodeCollectionNavType() {
        val arg = listOf(CustomType(1), CustomType(3), CustomType(5))
        val bundle = bundleOf("list" to arg)
        val result = decode<TestClassCollectionArg>(
            bundle,
            listOf(navArgument("list") { type = collectionNavType })
        )
        assertThat(result.list).containsExactlyElementsIn(arg).inOrder()
    }

    @Suppress("DEPRECATION")
    private inline fun <reified T : Any> decode(
        bundle: Bundle,
        args: List<NamedNavArgument> = emptyList()
    ): T {
        val typeMap = mutableMapOf<String, NavType<Any?>>()
        args.forEach { typeMap[it.name] = it.argument.type }
        return if (source == ArgumentSource.BUNDLE) {
            serializer<T>().decodeArguments(bundle, typeMap)
        } else {
            val handle = SavedStateHandle()
            bundle.keySet().forEach {
                handle[it] = bundle[it]
            }
            serializer<T>().decodeArguments(handle, typeMap)
        }
    }
}
