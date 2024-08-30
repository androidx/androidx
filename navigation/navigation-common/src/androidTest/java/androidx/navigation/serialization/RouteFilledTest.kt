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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

const val PATH_SERIAL_NAME = "www.test.com"

@RunWith(JUnit4::class)
class RouteFilledTest {

    @Test
    fun basePath() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass

        val clazz = TestClass()
        assertThatRouteFilledFrom(clazz).isEqualTo(PATH_SERIAL_NAME)
    }

    @Test
    fun pathArg() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val arg: String)

        val clazz = TestClass("test")
        assertThatRouteFilledFrom(clazz, listOf(stringArgument("arg")))
            .isEqualTo("$PATH_SERIAL_NAME/test")
    }

    @Test
    fun multiplePathArg() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val arg: String, val arg2: Int)

        val clazz = TestClass("test", 0)

        assertThatRouteFilledFrom(clazz, listOf(stringArgument("arg"), intArgument("arg2")))
            .isEqualTo("$PATH_SERIAL_NAME/test/0")
    }

    @Test
    fun pathArgNullable() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val arg: String?)

        val clazz = TestClass("test")
        assertThatRouteFilledFrom(clazz, listOf(nullableStringArgument("arg")))
            .isEqualTo("$PATH_SERIAL_NAME/test")
    }

    @Test
    fun pathArgNull() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val arg: String?)

        val clazz = TestClass(null)
        assertThatRouteFilledFrom(clazz, listOf(nullableStringArgument("arg")))
            .isEqualTo("$PATH_SERIAL_NAME/null")
    }

    @Test
    fun pathArgNullLiteral() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val arg: String?)

        val clazz = TestClass("null")
        assertThatRouteFilledFrom(clazz, listOf(nullableStringArgument("arg")))
            .isEqualTo("$PATH_SERIAL_NAME/null")
    }

    @Test
    fun multiplePathArgNullable() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String?, val arg2: Int?)

        val clazz = TestClass("test", 0)
        assertThatRouteFilledFrom(
                clazz,
                listOf(nullableStringArgument("arg"), nullableIntArgument("arg2"))
            )
            .isEqualTo("$PATH_SERIAL_NAME/test/0")
    }

    @Test
    fun multiplePathArgNull() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String?, val arg2: Int?)

        val clazz = TestClass(null, null)
        assertThatRouteFilledFrom(
                clazz,
                listOf(nullableStringArgument("arg"), nullableIntArgument("arg2"))
            )
            .isEqualTo("$PATH_SERIAL_NAME/null/null")
    }

    @Test
    fun queryArg() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val arg: String = "test")

        val clazz = TestClass()
        assertThatRouteFilledFrom(clazz, listOf(stringArgument("arg", true)))
            .isEqualTo("$PATH_SERIAL_NAME?arg=test")
    }

    @Test
    fun queryArgOverrideDefault() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val arg: String = "test")

        val clazz = TestClass("newTest")
        assertThatRouteFilledFrom(clazz, listOf(stringArgument("arg", true)))
            .isEqualTo("$PATH_SERIAL_NAME?arg=newTest")
    }

    @Test
    fun queryArgNullable() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val arg: String? = "test")

        val clazz = TestClass()
        assertThatRouteFilledFrom(clazz, listOf(nullableStringArgument("arg", true)))
            .isEqualTo("$PATH_SERIAL_NAME?arg=test")
    }

    @Test
    fun queryArgNull() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val arg: String? = null)

        val clazz = TestClass()
        assertThatRouteFilledFrom(clazz, listOf(nullableStringArgument("arg", true)))
            .isEqualTo("$PATH_SERIAL_NAME?arg=null")
    }

    @Test
    fun queryArgNullLiteral() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val arg: String? = null)

        val clazz = TestClass("null")
        assertThatRouteFilledFrom(clazz, listOf(nullableStringArgument("arg", true)))
            .isEqualTo("$PATH_SERIAL_NAME?arg=null")
    }

    @Test
    fun multipleQueryArgNullable() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String? = "test", val arg2: Int? = 0)

        val clazz = TestClass()
        assertThatRouteFilledFrom(
                clazz,
                listOf(nullableStringArgument("arg", true), nullableIntArgument("arg2", true))
            )
            .isEqualTo("$PATH_SERIAL_NAME?arg=test&arg2=0")
    }

    @Test
    fun multipleQueryArgNull() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String? = null, val arg2: Int? = null)

        val clazz = TestClass()
        assertThatRouteFilledFrom(
                clazz,
                listOf(nullableStringArgument("arg", true), nullableIntArgument("arg2", true))
            )
            .isEqualTo("$PATH_SERIAL_NAME?arg=null&arg2=null")
    }

    @Test
    fun pathAndQueryArg() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val pathArg: String, val queryArg: Int = 0)

        val clazz = TestClass("test")
        assertThatRouteFilledFrom(
                clazz,
                listOf(stringArgument("pathArg"), intArgument("queryArg", true))
            )
            .isEqualTo("$PATH_SERIAL_NAME/test?queryArg=0")
    }

    @Test
    fun pathAndQueryArgInReverseOrder() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val queryArg: Int = 0, val pathArg: String)

        val clazz = TestClass(1, "test")
        assertThatRouteFilledFrom(
                clazz,
                listOf(intArgument("queryArg", true), stringArgument("pathArg"))
            )
            .isEqualTo("$PATH_SERIAL_NAME/test?queryArg=1")
    }

    @Test
    fun pathAndQueryArgNullable() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val pathArg: String?, val queryArg: Int? = 0)

        val clazz = TestClass("test", 1)
        assertThatRouteFilledFrom(
                clazz,
                listOf(nullableStringArgument("pathArg"), nullableIntArgument("queryArg", true))
            )
            .isEqualTo("$PATH_SERIAL_NAME/test?queryArg=1")
    }

    @Test
    fun queryArrayArg() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val array: IntArray)

        val clazz = TestClass(intArrayOf(0, 1, 2))
        assertThatRouteFilledFrom(clazz, listOf(intArrayArgument("array")))
            .isEqualTo("$PATH_SERIAL_NAME?array=0&array=1&array=2")
    }

    @Test
    fun queryNullableArrayArg() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val array: IntArray?)

        val clazz = TestClass(intArrayOf(0, 1, 2))
        assertThatRouteFilledFrom(clazz, listOf(intArrayArgument("array")))
            .isEqualTo("$PATH_SERIAL_NAME?array=0&array=1&array=2")
    }

    @Test
    fun queryNullArrayArg() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val array: IntArray? = null)

        val clazz = TestClass()
        assertThatRouteFilledFrom(clazz, listOf(intArrayArgument("array")))
            .isEqualTo("$PATH_SERIAL_NAME")
    }

    @Test
    fun pathAndQueryArray() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val string: String, val array: IntArray)

        val clazz = TestClass("test", intArrayOf(0, 1, 2))
        assertThatRouteFilledFrom(
                clazz,
                listOf(stringArgument("string"), intArrayArgument("array"))
            )
            .isEqualTo("$PATH_SERIAL_NAME/test?array=0&array=1&array=2")
    }

    @Test
    fun queryPrimitiveAndArray() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val array: IntArray, val arg: Int = 0)

        val clazz = TestClass(intArrayOf(0, 1, 2), 15)
        assertThatRouteFilledFrom(clazz, listOf(intArrayArgument("array"), intArgument("arg")))
            .isEqualTo("$PATH_SERIAL_NAME?array=0&array=1&array=2&arg=15")
    }

    @Test
    fun routeListArgs() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class IntList(val list: List<Int>)
        assertThatRouteFilledFrom(
                IntList(listOf(1, 2)),
                listOf(
                    navArgument("list") {
                        type = NavType.IntListType
                        nullable = false
                        unknownDefaultValuePresent = false
                    }
                )
            )
            .isEqualTo("$PATH_SERIAL_NAME?list=1&list=2")
    }

    @Test
    fun withSecondaryConstructor() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String) {
            constructor(arg2: Int) : this(arg2.toString())
        }

        val clazz = TestClass(0)
        assertThatRouteFilledFrom(clazz, listOf(stringArgument("arg")))
            .isEqualTo("$PATH_SERIAL_NAME/0")
    }

    @Test
    fun withCompanionObject() {
        val clazz = ClassWithCompanionObject(0)
        assertThatRouteFilledFrom(clazz, listOf(intArgument("arg")))
            .isEqualTo("$PATH_SERIAL_NAME/0")
    }

    @Test
    fun withCompanionParameter() {
        val clazz = ClassWithCompanionParam(0)
        assertThatRouteFilledFrom(clazz, listOf(intArgument("arg")))
            .isEqualTo("$PATH_SERIAL_NAME/0")
    }

    @Test
    fun withFunction() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String) {
            fun testFun() {}
        }

        val clazz = TestClass("test")
        assertThatRouteFilledFrom(clazz, listOf(stringArgument("arg")))
            .isEqualTo("$PATH_SERIAL_NAME/test")
    }

    @Test
    fun enumType() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: TestEnum, val arg2: TestEnum)

        val clazz = TestClass(TestEnum.ONE, TestEnum.TWO)
        assertThatRouteFilledFrom(
                clazz,
                listOf(
                    enumArgument("arg", TestEnum::class.java),
                    enumArgument("arg2", TestEnum::class.java)
                )
            )
            .isEqualTo("$PATH_SERIAL_NAME/ONE/TWO")
    }

    @Test
    fun enumNullableType() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: TestEnum?, val arg2: TestEnum?)

        val clazz = TestClass(TestEnum.ONE, null)
        assertThatRouteFilledFrom(
                clazz,
                listOf(
                    enumArgument("arg", TestEnum::class.java),
                    enumArgument("arg2", TestEnum::class.java)
                )
            )
            .isEqualTo("$PATH_SERIAL_NAME/ONE/null")
    }

    @Test
    fun customParamType() {
        @Serializable class CustomType

        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val custom: CustomType)

        val customArg =
            navArgument("custom") {
                type =
                    object : NavType<CustomType>(false) {
                        override fun put(bundle: Bundle, key: String, value: CustomType) {}

                        override fun get(bundle: Bundle, key: String): CustomType? = null

                        override fun parseValue(value: String): CustomType = CustomType()

                        override fun serializeAsValue(value: CustomType) = "customValue"
                    }
                nullable = false
                unknownDefaultValuePresent = false
            }

        val clazz = TestClass(CustomType())
        assertThatRouteFilledFrom(clazz, listOf(customArg))
            .isEqualTo("$PATH_SERIAL_NAME/customValue")
    }

    @Test
    fun nestedCustomParamType() {
        @Serializable
        class NestedCustomType {
            override fun toString() = "nestedCustomValue"
        }

        @Serializable class CustomType(val nested: NestedCustomType)

        val customArg =
            navArgument("custom") {
                type =
                    object : NavType<CustomType>(false) {
                        override fun put(bundle: Bundle, key: String, value: CustomType) {}

                        override fun get(bundle: Bundle, key: String) = null

                        override fun parseValue(value: String): CustomType =
                            CustomType(NestedCustomType())

                        override fun serializeAsValue(value: CustomType) =
                            "customValue[${value.nested}]"
                    }
                nullable = false
                unknownDefaultValuePresent = false
            }

        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val custom: CustomType)

        val clazz = TestClass(CustomType(NestedCustomType()))
        assertThatRouteFilledFrom(clazz, listOf(customArg))
            .isEqualTo("$PATH_SERIAL_NAME/customValue[nestedCustomValue]")
    }

    @Test
    fun customSerializerParamType() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(
            val arg: Int,
            @Serializable(with = CustomSerializer::class) val arg2: CustomSerializerClass
        )

        val customArg =
            navArgument("arg2") {
                type =
                    object : NavType<CustomSerializerClass>(false) {
                        override fun put(
                            bundle: Bundle,
                            key: String,
                            value: CustomSerializerClass
                        ) {}

                        override fun get(bundle: Bundle, key: String) = null

                        override fun parseValue(value: String) = CustomSerializerClass(1L)

                        override fun serializeAsValue(value: CustomSerializerClass) =
                            "customSerializerClass[${value.longArg}]"
                    }
                nullable = false
                unknownDefaultValuePresent = false
            }
        val clazz = TestClass(0, CustomSerializerClass(1L))
        assertThatRouteFilledFrom(clazz, listOf(intArgument("arg"), customArg))
            .isEqualTo("$PATH_SERIAL_NAME/0/customSerializerClass[1]")
    }

    @Test
    fun customTypeParam() {
        @Serializable open class TypeParam
        @Serializable class CustomType<T : TypeParam>
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val custom: CustomType<TypeParam>)

        val navType =
            object : NavType<CustomType<TypeParam>>(false) {
                override val name: String
                    get() = "CustomType"

                override fun put(bundle: Bundle, key: String, value: CustomType<TypeParam>) {}

                override fun get(bundle: Bundle, key: String): CustomType<TypeParam>? = null

                override fun parseValue(value: String): CustomType<TypeParam> = CustomType()

                override fun serializeAsValue(value: CustomType<TypeParam>) = "customValue"
            }
        assertThatRouteFilledFrom(
                TestClass(CustomType()),
                listOf(navArgument("custom") { type = navType })
            )
            .isEqualTo("$PATH_SERIAL_NAME/customValue")
    }

    @Test
    fun customTypeParamNested() {
        @Serializable open class TypeParamNested
        @Serializable open class TypeParam<K : TypeParamNested>
        @Serializable class CustomType<T : TypeParam<TypeParamNested>>
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val custom: CustomType<TypeParam<TypeParamNested>>)

        val navType =
            object : NavType<CustomType<TypeParam<TypeParamNested>>>(false) {
                override val name: String
                    get() = "CustomType"

                override fun put(
                    bundle: Bundle,
                    key: String,
                    value: CustomType<TypeParam<TypeParamNested>>
                ) {}

                override fun get(
                    bundle: Bundle,
                    key: String
                ): CustomType<TypeParam<TypeParamNested>>? = null

                override fun parseValue(value: String): CustomType<TypeParam<TypeParamNested>> =
                    CustomType()

                override fun serializeAsValue(value: CustomType<TypeParam<TypeParamNested>>) =
                    "customValue"
            }
        assertThatRouteFilledFrom(
                TestClass(CustomType()),
                listOf(navArgument("custom") { type = navType })
            )
            .isEqualTo("$PATH_SERIAL_NAME/customValue")
    }

    @Test
    fun paramWithNoBackingField() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass {
            val noBackingField: Int
                get() = 0
        }
        // only members with backing field should appear on route
        val clazz = TestClass()
        assertThatRouteFilledFrom(clazz).isEqualTo(PATH_SERIAL_NAME)
    }

    @Test
    fun queryArgFromClassBody() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass {
            val arg: Int = 0
        }
        val clazz = TestClass()
        assertThatRouteFilledFrom(clazz, listOf(intArgument("arg")))
            .isEqualTo("$PATH_SERIAL_NAME?arg=0")
    }

    @Test
    fun pathArgFromClassBody() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass {
            lateinit var arg: IntArray
        }
        val clazz = TestClass().also { it.arg = intArrayOf(0) }
        assertThatRouteFilledFrom(clazz, listOf(intArrayArgument("arg")))
            .isEqualTo("$PATH_SERIAL_NAME?arg=0")
    }

    @Test
    fun nonSerializableClassInvalid() {
        @SerialName(PATH_SERIAL_NAME) class TestClass

        assertFailsWith<SerializationException> {
            // the class must be serializable
            generateRouteWithArgs(TestClass(), emptyMap())
        }
    }

    @Test
    fun childClassOfAbstract() {
        @Serializable abstract class TestAbstractClass

        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass : TestAbstractClass()

        val clazz = TestClass()
        assertThatRouteFilledFrom(
                clazz,
            )
            .isEqualTo(PATH_SERIAL_NAME)
    }

    @Test
    fun childClassOfAbstract_duplicateArgs() {
        @Serializable abstract class TestAbstractClass(val arg: Int)

        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg2: Int) : TestAbstractClass(arg2)

        // args will be duplicated
        val clazz = TestClass(0)
        assertThatRouteFilledFrom(clazz, listOf(intArgument("arg"), intArgument("arg2")))
            .isEqualTo("$PATH_SERIAL_NAME/0/0")
    }

    @Test
    fun childClassOfSealed_withArgs() {
        // child class overrides parent variable so only child variable shows up in route pattern
        val clazz = SealedClass.TestClass(0)
        assertThatRouteFilledFrom(clazz, listOf(intArgument("arg2")))
            .isEqualTo("$PATH_SERIAL_NAME/0")
    }

    @Test
    fun childClassOfInterface() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val arg: Int) : TestInterface

        val clazz = TestClass(0)
        assertThatRouteFilledFrom(clazz, listOf(intArgument("arg")))
            .isEqualTo("$PATH_SERIAL_NAME/0")
    }

    @Test
    fun routeFromObject() {
        assertThatRouteFilledFrom(TestObject).isEqualTo(PATH_SERIAL_NAME)
    }

    @Test
    fun routeFromObject_argsNotSerialized() {
        // object variables are not serialized and does not show up on route
        assertThatRouteFilledFrom(TestObjectWithArg).isEqualTo(PATH_SERIAL_NAME)
    }

    @Test
    fun collectionNavType() {
        assertThatRouteFilledFrom(
                TestClassCollectionArg(
                    listOf(CustomTypeWithArg(1), CustomTypeWithArg(3), CustomTypeWithArg(5))
                ),
                listOf(navArgument("list") { type = collectionNavType })
            )
            .isEqualTo("$PATH_SERIAL_NAME?list=1&list=3&list=5")
    }

    @Test
    fun nullStringList() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val list: List<String>?)

        val clazz = TestClass(null)

        val listArg =
            navArgument("list") {
                type = NavType.StringListType
                nullable = true
            }

        assertThatRouteFilledFrom(clazz, listOf(listArg)).isEqualTo("$PATH_SERIAL_NAME")
    }

    @Test
    fun nullIntList() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val list: List<Int>?)

        val clazz = TestClass(null)

        val listArg =
            navArgument("list") {
                type = NavType.IntListType
                nullable = true
            }

        assertThatRouteFilledFrom(clazz, listOf(listArg)).isEqualTo(PATH_SERIAL_NAME)
    }

    @Test
    fun emptyStringList() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val list: List<String>)

        val clazz = TestClass(emptyList())

        val listArg =
            navArgument("list") {
                type = NavType.StringListType
                nullable = false
            }

        assertThatRouteFilledFrom(clazz, listOf(listArg)).isEqualTo("$PATH_SERIAL_NAME")
    }

    @Test
    fun emptyIntList() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val list: List<Int>)

        val clazz = TestClass(emptyList())

        val listArg =
            navArgument("list") {
                type = NavType.IntListType
                nullable = false
            }

        assertThatRouteFilledFrom(clazz, listOf(listArg)).isEqualTo("$PATH_SERIAL_NAME")
    }

    @Test
    fun defaultEmptyStringList() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val list: List<String> = emptyList())

        val clazz = TestClass()

        val listArg =
            navArgument("list") {
                type = NavType.StringListType
                nullable = false
            }

        assertThatRouteFilledFrom(clazz, listOf(listArg)).isEqualTo("$PATH_SERIAL_NAME")
    }

    @Test
    fun defaultEmptyIntList() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val list: List<Int> = emptyList())

        val clazz = TestClass()

        val listArg =
            navArgument("list") {
                type = NavType.IntListType
                nullable = false
            }

        assertThatRouteFilledFrom(clazz, listOf(listArg)).isEqualTo("$PATH_SERIAL_NAME")
    }

    @Test
    fun encodeDouble() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val arg: Double)

        val clazz = TestClass(11E123)
        val arg =
            navArgument("arg") {
                type = InternalNavType.DoubleType
                nullable = false
            }
        assertThatRouteFilledFrom(clazz, listOf(arg)).isEqualTo("$PATH_SERIAL_NAME/1.1E124")
    }

    @Test
    fun encodeDoubleNullable() {
        @Serializable @SerialName(PATH_SERIAL_NAME) class TestClass(val arg: Double?)

        val clazz = TestClass(11E123)
        val arg =
            navArgument("arg") {
                type = InternalNavType.DoubleNullableType
                nullable = false
            }
        assertThatRouteFilledFrom(clazz, listOf(arg)).isEqualTo("$PATH_SERIAL_NAME/1.1E124")

        val clazz2 = TestClass(null)
        val arg2 =
            navArgument("arg") {
                type = InternalNavType.DoubleNullableType
                nullable = false
            }
        assertThatRouteFilledFrom(clazz2, listOf(arg2)).isEqualTo("$PATH_SERIAL_NAME/null")
    }
}

private fun <T : Any> assertThatRouteFilledFrom(
    obj: T,
    customArgs: List<NamedNavArgument>? = null
): String {
    val typeMap = mutableMapOf<String, NavType<Any?>>()
    customArgs?.forEach { typeMap[it.name] = it.argument.type }
    return generateRouteWithArgs(obj, typeMap)
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

@Serializable @SerialName(PATH_SERIAL_NAME) internal object TestObject

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
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: CustomSerializerClass) =
        encoder.encodeLong(value.longArg)

    override fun deserialize(decoder: Decoder): CustomSerializerClass =
        CustomSerializerClass(decoder.decodeLong())
}

private interface TestInterface

private fun nullableIntArgument(name: String, hasDefaultValue: Boolean = false) =
    navArgument(name) {
        type = InternalNavType.IntNullableType
        nullable = true
        unknownDefaultValuePresent = hasDefaultValue
    }

private fun intArrayArgument(name: String, hasDefaultValue: Boolean = false) =
    navArgument(name) {
        type = NavType.IntArrayType
        nullable = true
        unknownDefaultValuePresent = hasDefaultValue
    }

@Serializable
private enum class TestEnum {
    ONE,
    TWO
}
