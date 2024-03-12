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
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

const val PATH_SERIAL_NAME = "www.test.com"

@RunWith(JUnit4::class)
class RouteFilledTest {

    @Test
    fun basePath() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass

        val serializer = serializer<TestClass>()

        val clazz = TestClass()
        assertThatRouteFilledFrom(clazz, serializer).isEqualTo(PATH_SERIAL_NAME)
    }

    @Test
    fun pathArg() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String)

        val serializer = serializer<TestClass>()

        val clazz = TestClass("test")
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(stringArgument("arg"))
        ).isEqualTo("$PATH_SERIAL_NAME/test")
    }

    @Test
    fun multiplePathArg() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String, val arg2: Int)

        val serializer = serializer<TestClass>()
        val clazz = TestClass("test", 0)

        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(stringArgument("arg"), intArgument("arg2"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME/test/0"
        )
    }

    @Test
    fun pathArgNullable() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String?)

        val serializer = serializer<TestClass>()
        val clazz = TestClass("test")
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(nullableStringArgument("arg"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME/test"
        )
    }

    @Test
    fun pathArgNull() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String?)

        val serializer = serializer<TestClass>()
        val clazz = TestClass(null)
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(nullableStringArgument("arg"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME/null"
        )
    }

    @Test
    fun pathArgNullLiteral() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String?)

        val clazz = TestClass("null")
        assertThatRouteFilledFrom(
            clazz,
            serializer<TestClass>(),
            listOf(nullableStringArgument("arg"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME/null"
        )
    }

    @Test
    fun multiplePathArgNullable() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String?, val arg2: Int?)

        val serializer = serializer<TestClass>()
        val clazz = TestClass("test", 0)
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(nullableStringArgument("arg"), nullableIntArgument("arg2"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME/test/0"
        )
    }

    @Test
    fun multiplePathArgNull() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String?, val arg2: Int?)

        val serializer = serializer<TestClass>()
        val clazz = TestClass(null, null)
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(nullableStringArgument("arg"), nullableIntArgument("arg2"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME/null/null"
        )
    }

    @Test
    fun queryArg() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String = "test")

        val serializer = serializer<TestClass>()
        val clazz = TestClass()
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(stringArgument("arg", true))
        ).isEqualTo(
            "$PATH_SERIAL_NAME?arg=test"
        )
    }

    @Test
    fun queryArgOverrideDefault() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String = "test")

        val serializer = serializer<TestClass>()
        val clazz = TestClass("newTest")
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(stringArgument("arg", true))
        ).isEqualTo(
            "$PATH_SERIAL_NAME?arg=newTest"
        )
    }

    @Test
    fun queryArgNullable() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String? = "test")

        val serializer = serializer<TestClass>()
        val clazz = TestClass()
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(nullableStringArgument("arg", true))
        ).isEqualTo(
            "$PATH_SERIAL_NAME?arg=test"
        )
    }

    @Test
    fun queryArgNull() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String? = null)

        val serializer = serializer<TestClass>()
        val clazz = TestClass()
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(nullableStringArgument("arg", true))
        ).isEqualTo(
            "$PATH_SERIAL_NAME?arg=null"
        )
    }

    @Test
    fun queryArgNullLiteral() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String? = null)

        val clazz = TestClass("null")
        assertThatRouteFilledFrom(
            clazz,
            serializer<TestClass>(),
            listOf(nullableStringArgument("arg", true))
        ).isEqualTo(
            "$PATH_SERIAL_NAME?arg=null"
        )
    }

    @Test
    fun multipleQueryArgNullable() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String? = "test", val arg2: Int? = 0)

        val serializer = serializer<TestClass>()
        val clazz = TestClass()
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(
                nullableStringArgument("arg", true),
                nullableIntArgument("arg2", true)
            )
        ).isEqualTo(
            "$PATH_SERIAL_NAME?arg=test&arg2=0"
        )
    }

    @Test
    fun multipleQueryArgNull() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String? = null, val arg2: Int? = null)

        val serializer = serializer<TestClass>()
        val clazz = TestClass()
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(
                nullableStringArgument("arg", true),
                nullableIntArgument("arg2", true)
            )
        ).isEqualTo(
            "$PATH_SERIAL_NAME?arg=null&arg2=null"
        )
    }

    @Test
    fun pathAndQueryArg() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val pathArg: String, val queryArg: Int = 0)

        val serializer = serializer<TestClass>()
        val clazz = TestClass("test")
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(
                stringArgument("pathArg"),
                intArgument("queryArg", true)
            )
        ).isEqualTo(
            "$PATH_SERIAL_NAME/test?queryArg=0"
        )
    }

    @Test
    fun pathAndQueryArgInReverseOrder() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val queryArg: Int = 0, val pathArg: String)

        val serializer = serializer<TestClass>()
        val clazz = TestClass(1, "test")
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(
                intArgument("queryArg", true),
                stringArgument("pathArg")
            )
        ).isEqualTo(
            "$PATH_SERIAL_NAME/test?queryArg=1"
        )
    }

    @Test
    fun pathAndQueryArgNullable() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val pathArg: String?, val queryArg: Int? = 0)

        val serializer = serializer<TestClass>()
        val clazz = TestClass("test", 1)
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(
                nullableStringArgument("pathArg"),
                nullableIntArgument("queryArg", true)
            )
        ).isEqualTo(
            "$PATH_SERIAL_NAME/test?queryArg=1"
        )
    }

    @Test
    fun queryArrayArg() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val array: IntArray)

        val serializer = serializer<TestClass>()
        val clazz = TestClass(intArrayOf(0, 1, 2))
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(intArrayArgument("array"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME?array=0&array=1&array=2"
        )
    }

    @Test
    fun queryNullableArrayArg() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val array: IntArray?)

        val serializer = serializer<TestClass>()
        val clazz = TestClass(intArrayOf(0, 1, 2))
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(intArrayArgument("array"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME?array=0&array=1&array=2"
        )
    }

    @Test
    fun queryNullArrayArg() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val array: IntArray? = null)

        val serializer = serializer<TestClass>()
        val clazz = TestClass()
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(intArrayArgument("array"),)
        ).isEqualTo(
            "$PATH_SERIAL_NAME?array=null"
        )
    }

    @Test
    fun pathAndQueryArray() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val string: String, val array: IntArray)

        val serializer = serializer<TestClass>()
        val clazz = TestClass("test", intArrayOf(0, 1, 2))
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(
                stringArgument("string"),
                intArrayArgument("array")
            )
        ).isEqualTo(
            "$PATH_SERIAL_NAME/test?array=0&array=1&array=2"
        )
    }

    @Test
    fun queryPrimitiveAndArray() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val array: IntArray, val arg: Int = 0)

        val serializer = serializer<TestClass>()
        val clazz = TestClass(intArrayOf(0, 1, 2), 15)
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(
                intArrayArgument("array"),
                intArgument("arg")
            )
        ).isEqualTo(
            "$PATH_SERIAL_NAME?array=0&array=1&array=2&arg=15"
        )
    }

    @Test
    fun withSecondaryConstructor() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String) {
            constructor(arg2: Int) : this(arg2.toString())
        }

        val serializer = serializer<TestClass>()
        val clazz = TestClass(0)
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(stringArgument("arg"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME/0"
        )
    }

    @Test
    fun withCompanionObject() {
        val serializer = serializer<ClassWithCompanionObject>()
        val clazz = ClassWithCompanionObject(0)
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(intArgument("arg"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME/0"
        )
    }

    @Test
    fun withCompanionParameter() {
        val serializer = serializer<ClassWithCompanionParam>()
        val clazz = ClassWithCompanionParam(0)
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(intArgument("arg"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME/0"
        )
    }

    @Test
    fun withFunction() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String) {
            fun testFun() { }
        }

        val serializer = serializer<TestClass>()
        val clazz = TestClass("test")
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(stringArgument("arg"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME/test"
        )
    }

    @Test
    fun customParamType() {
        @Serializable
        class CustomType

        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val custom: CustomType)

        val customArg = navArgument("custom") {
            type = object : NavType<CustomType>(false) {
                override fun put(bundle: Bundle, key: String, value: CustomType) { }
                override fun get(bundle: Bundle, key: String): CustomType? = null
                override fun parseValue(value: String): CustomType = CustomType()
                override fun serializeAsValue(value: CustomType) = "customValue"
            }
            nullable = false
            unknownDefaultValuePresent = false
        }

        val serializer = serializer<TestClass>()
        val clazz = TestClass(CustomType())
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(customArg)
        ).isEqualTo(
            "$PATH_SERIAL_NAME/customValue"
        )
    }

    @Test
    fun nestedCustomParamType() {
        @Serializable
        class NestedCustomType { override fun toString() = "nestedCustomValue" }

        @Serializable
        class CustomType(val nested: NestedCustomType)

        val customArg = navArgument("custom") {
            type = object : NavType<CustomType>(false) {
                override fun put(bundle: Bundle, key: String, value: CustomType) { }
                override fun get(bundle: Bundle, key: String) = null
                override fun parseValue(value: String): CustomType = CustomType(NestedCustomType())
                override fun serializeAsValue(value: CustomType) = "customValue[${value.nested}]"
            }
            nullable = false
            unknownDefaultValuePresent = false
        }

        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val custom: CustomType)

        val serializer = serializer<TestClass>()
        val clazz = TestClass(CustomType(NestedCustomType()))
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(customArg)
        ).isEqualTo(
            "$PATH_SERIAL_NAME/customValue[nestedCustomValue]"
        )
    }

    @Test
    fun customSerializerParamType() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(
            val arg: Int,
            @Serializable(with = CustomSerializer::class)
            val arg2: CustomSerializerClass
        )

        val customArg = navArgument("arg2") {
            type = object : NavType<CustomSerializerClass>(false) {
                override fun put(bundle: Bundle, key: String, value: CustomSerializerClass) { }
                override fun get(bundle: Bundle, key: String) = null
                override fun parseValue(value: String) = CustomSerializerClass(1L)
                override fun serializeAsValue(value: CustomSerializerClass) =
                    "customSerializerClass[${value.longArg}]"
            }
            nullable = false
            unknownDefaultValuePresent = false
        }
        val serializer = serializer<TestClass>()
        val clazz = TestClass(0, CustomSerializerClass(1L))
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(intArgument("arg"), customArg)
        ).isEqualTo(
            "$PATH_SERIAL_NAME/0/customSerializerClass[1]"
        )
    }

    @Test
    fun paramWithNoBackingField() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass {
            val noBackingField: Int
                get() = 0
        }
        val serializer = serializer<TestClass>()
        // only members with backing field should appear on route
        val clazz = TestClass()
        assertThatRouteFilledFrom(
            clazz,
            serializer
        ).isEqualTo(
            "$PATH_SERIAL_NAME"
        )
    }

    @Test
    fun queryArgFromClassBody() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass {
            val arg: Int = 0
        }
        val serializer = serializer<TestClass>()
        val clazz = TestClass()
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(intArgument("arg"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME?arg=0"
        )
    }

    @Test
    fun pathArgFromClassBody() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass {
            lateinit var arg: IntArray
        }
        val serializer = serializer<TestClass>()
        val clazz = TestClass().also { it.arg = intArrayOf(0) }
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(intArrayArgument("arg"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME?arg=0"
        )
    }

    @Test
    fun nonSerializableClassInvalid() {
        @SerialName(PATH_SERIAL_NAME)
        class TestClass

        assertFailsWith<SerializationException> {
            // the class must be serializable
            serializer<TestClass>().generateRouteWithArgs(TestClass(), emptyMap())
        }
    }

    @Test
    fun childClassOfAbstract() {
        @Serializable
        abstract class TestAbstractClass

        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass : TestAbstractClass()

        val serializer = serializer<TestClass>()
        val clazz = TestClass()
        assertThatRouteFilledFrom(clazz, serializer,
        ).isEqualTo(
            "$PATH_SERIAL_NAME"
        )
    }

    @Test
    fun childClassOfAbstract_duplicateArgs() {
        @Serializable
        abstract class TestAbstractClass(val arg: Int)

        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg2: Int) : TestAbstractClass(arg2)

        val serializer = serializer<TestClass>()
        // args will be duplicated
        val clazz = TestClass(0)
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(intArgument("arg"), intArgument("arg2"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME/0/0"
        )
    }

    @Test
    fun childClassOfSealed_withArgs() {
        val serializer = serializer<SealedClass.TestClass>()
        // child class overrides parent variable so only child variable shows up in route pattern
        val clazz = SealedClass.TestClass(0)
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(intArgument("arg2"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME/0"
        )
    }

    @Test
    fun childClassOfInterface() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: Int) : TestInterface

        val serializer = serializer<TestClass>()
        val clazz = TestClass(0)
        assertThatRouteFilledFrom(
            clazz,
            serializer,
            listOf(intArgument("arg"))
        ).isEqualTo(
            "$PATH_SERIAL_NAME/0"
        )
    }

    @Test
    fun routeFromObject() {
        val serializer = serializer<TestObject>()
        assertThatRouteFilledFrom(TestObject, serializer).isEqualTo(
            "$PATH_SERIAL_NAME"
        )
    }

    @Test
    fun routeFromObject_argsNotSerialized() {
        val serializer = serializer<TestObjectWithArg>()
        // object variables are not serialized and does not show up on route
        assertThatRouteFilledFrom(TestObjectWithArg, serializer).isEqualTo(
            "$PATH_SERIAL_NAME"
        )
    }
}

private fun <T : Any> assertThatRouteFilledFrom(
    obj: T,
    serializer: KSerializer<T>,
    customArgs: List<NamedNavArgument>? = null
): String {
    val typeMap = mutableMapOf<String, NavType<Any?>>()
    customArgs?.forEach { typeMap[it.name] = it.argument.type }
    return serializer.generateRouteWithArgs(obj, typeMap)
}

internal fun String.isEqualTo(other: String) {
    assertThat(this).isEqualTo(other)
}

@Serializable
@SerialName(PATH_SERIAL_NAME)
private class ClassWithCompanionObject(val arg: Int) {
    companion object TestObject
}

@Serializable
@SerialName(PATH_SERIAL_NAME)
private class ClassWithCompanionParam(val arg: Int) {
    companion object {
        val companionVal: String = "hello"
    }
}

@Serializable
@SerialName(PATH_SERIAL_NAME)
internal object TestObject

@Serializable
@SerialName(PATH_SERIAL_NAME)
internal object TestObjectWithArg {
    val arg: Int = 0
}

@Serializable
private sealed class SealedClass {
    abstract val arg: Int

    @Serializable
    @SerialName(PATH_SERIAL_NAME)
    // same value for arg and arg2
    class TestClass(val arg2: Int) : SealedClass() {
        override val arg: Int
            get() = arg2
    }
}

private class CustomSerializerClass(val longArg: Long)

private class CustomSerializer : KSerializer<CustomSerializerClass> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "Date", PrimitiveKind.LONG
    )
    override fun serialize(encoder: Encoder, value: CustomSerializerClass) =
        encoder.encodeLong(value.longArg)
    override fun deserialize(decoder: Decoder): CustomSerializerClass =
        CustomSerializerClass(decoder.decodeLong())
}

private interface TestInterface

private fun stringArgument(
    name: String,
    hasDefaultValue: Boolean = false
) = navArgument(name) {
    type = NavType.StringType
    nullable = false
    unknownDefaultValuePresent = hasDefaultValue
}

private fun nullableStringArgument(
    name: String,
    hasDefaultValue: Boolean = false
) = navArgument(name) {
    type = NavType.StringType
    nullable = true
    unknownDefaultValuePresent = hasDefaultValue
}

private fun intArgument(
    name: String,
    hasDefaultValue: Boolean = false
) = navArgument(name) {
    type = NavType.IntType
    nullable = false
    unknownDefaultValuePresent = hasDefaultValue
}

private fun nullableIntArgument(
    name: String,
    hasDefaultValue: Boolean = false
) = navArgument(name) {
    type = NullableIntType
    nullable = true
    unknownDefaultValuePresent = hasDefaultValue
}

private fun intArrayArgument(
    name: String,
    hasDefaultValue: Boolean = false
) = navArgument(name) {
    type = NavType.IntArrayType
    nullable = true
    unknownDefaultValuePresent = hasDefaultValue
}

private val NullableIntType: NavType<Int?> = object : NavType<Int?>(true) {
    override val name: String
        get() = "nullable_integer"

    override fun put(bundle: Bundle, key: String, value: Int?) {
        value?.let { bundle.putInt(key, value) }
    }

    @Suppress("DEPRECATION")
    override fun get(bundle: Bundle, key: String): Int? {
        val value = bundle[key]
        return value?.let { it as Int }
    }

    override fun parseValue(value: String): Int? {
        return if (value == "null") {
            null
        } else if (value.startsWith("0x")) {
            value.substring(2).toInt(16)
        } else {
            value.toInt()
        }
    }

    override fun serializeAsValue(value: Int?): String = value?.toString() ?: "null"
}
