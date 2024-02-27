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
class RoutePatternTest {

    @Test
    fun basePath() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(PATH_SERIAL_NAME)
    }

    @Test
    fun pathArg() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String)

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME/{arg}"
        )
    }

    @Test
    fun multiplePathArg() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String, val arg2: Int)

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME/{arg}/{arg2}"
        )
    }

    @Test
    fun pathArgNullable() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String?)

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME/{arg}"
        )
    }

    @Test
    fun multiplePathArgNullable() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String?, val arg2: Int?)

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME/{arg}/{arg2}"
        )
    }

    @Test
    fun queryArg() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String = "test")

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME?arg={arg}"
        )
    }

    @Test
    fun multipleQueryArg() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String = "test", val arg2: Int = 0)

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME?arg={arg}&arg2={arg2}"
        )
    }

    @Test
    fun queryArgNullable() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String? = "test")

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME?arg={arg}"
        )
    }

    @Test
    fun queryArgWithNullDefaultValue() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: Int? = null)

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME?arg={arg}"
        )
    }

    @Test
    fun multipleQueryArgNullable() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String? = "test", val arg2: Int? = 0)

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME?arg={arg}&arg2={arg2}"
        )
    }

    @Test
    fun pathAndQueryArg() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val pathArg: String, val queryArg: Int = 0)

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME/{pathArg}?queryArg={queryArg}"
        )
    }

    @Test
    fun pathAndQueryArgInReverseOrder() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val queryArg: Int = 0, val pathArg: String)

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME/{pathArg}?queryArg={queryArg}"
        )
    }

    @Test
    fun pathAndQueryArgNullable() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val pathArg: String?, val queryArg: Int? = 0)

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME/{pathArg}?queryArg={queryArg}"
        )
    }

    @Test
    fun arrayType() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val array: IntArray)

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME?array={array}"
        )
    }

    @Test
    fun optionalArrayType() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val array: IntArray?)

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME?array={array}"
        )
    }

    @Test
    fun withSecondaryConstructor() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String) {
            constructor(arg2: Int) : this(arg2.toString())
        }

        // only class members would show up on routePattern
        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME/{arg}"
        )
    }

    @Test
    fun withCompanionObject() {
        assertThatRoutePatternFrom(serializer<ClassWithCompanionObject>()).isEqualTo(
            "$PATH_SERIAL_NAME/{arg}"
        )
    }

    @Test
    fun withCompanionParameter() {
        assertThatRoutePatternFrom(serializer<ClassWithCompanionParam>()).isEqualTo(
            "$PATH_SERIAL_NAME/{arg}"
        )
    }

    @Test
    fun withFunction() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: String) {
            fun testFun() { }
        }

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME/{arg}"
        )
    }

    @Test
    fun customParamType() {
        @Serializable
        class CustomType

        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val custom: CustomType)

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME?custom={custom}"
        )
    }

    @Test
    fun nestedCustomParamType() {
        @Serializable
        class NestedCustomType

        @Serializable
        class CustomType(val nested: NestedCustomType)

        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val custom: CustomType)

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME?custom={custom}"
        )
    }

    @Test
    fun customSerializerParamType() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(
            val arg: Int,
            @Serializable(with = CustomSerializer::class)
            val arg2: NonSerializedClass
        )

        // args will be duplicated
        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME/{arg}?arg2={arg2}"
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

        // only members with backing field should appear on routePattern
        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            PATH_SERIAL_NAME
        )
    }

    @Test
    fun queryArgFromClassBody() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass {
            val arg: Int = 0
        }

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME?arg={arg}"
        )
    }

    @Test
    fun pathArgFromClassBody() {
        @Serializable
        class CustomType
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass {
            lateinit var arg: CustomType
        }

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME?arg={arg}"
        )
    }

    @Test
    fun nonSerializableClassInvalid() {
        @SerialName(PATH_SERIAL_NAME)
        class TestClass

        assertFailsWith<SerializationException> {
            // the class must be serializable
            serializer<TestClass>().generateRoutePattern()
        }
    }

    @Test
    fun abstractClassInvalid() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        abstract class TestClass

        val patternException = assertFailsWith<IllegalArgumentException> {
            serializer<TestClass>().generateRoutePattern()
        }
        assertThat(patternException.message).isEqualTo(
            "Cannot generate route pattern from polymorphic class TestClass. Routes " +
                "can only be generated from concrete classes or objects."
        )
    }

    @Test
    fun childClassOfAbstract() {
        @Serializable
        abstract class TestAbstractClass

        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass : TestAbstractClass()

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            PATH_SERIAL_NAME
        )
    }

    @Test
    fun childClassOfAbstract_duplicateArgs() {
        @Serializable
        abstract class TestAbstractClass(val arg: Int)

        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg2: Int) : TestAbstractClass(0)

        // args will be duplicated
        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME/{arg}/{arg2}"
        )
    }

    @Test
    fun childClassOfSealed_withArgs() {
        // child class overrides parent variable so only child variable shows up in route pattern
        assertThatRoutePatternFrom(serializer<SealedClass.TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME/{arg2}"
        )
    }

    @Test
    fun childClassOfInterface() {
        @Serializable
        @SerialName(PATH_SERIAL_NAME)
        class TestClass(val arg: Int) : TestInterface

        assertThatRoutePatternFrom(serializer<TestClass>()).isEqualTo(
            "$PATH_SERIAL_NAME/{arg}"
        )
    }

    @Test
    fun routeFromPlainObject() {
        assertThatRoutePatternFrom(serializer<TestObject>()).isEqualTo(
            PATH_SERIAL_NAME
        )
    }

    @Test
    fun routeFromObject_argsNotSerialized() {
        // object variables are not serialized and does not show up on routePattern
        assertThatRoutePatternFrom(serializer<TestObjectWithArg>()).isEqualTo(
            PATH_SERIAL_NAME
        )
    }
}

private fun <T> assertThatRoutePatternFrom(serializer: KSerializer<T>) =
    serializer.generateRoutePattern()

private fun String.isEqualTo(other: String) {
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
internal sealed class SealedClass {
    abstract val arg: Int

    @Serializable
    @SerialName(PATH_SERIAL_NAME)
    // same value for arg and arg2
    class TestClass(val arg2: Int) : SealedClass() {
        override val arg: Int
            get() = arg2
    }
}

private class NonSerializedClass(val longArg: Long)

private class CustomSerializer : KSerializer<NonSerializedClass> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "Date", PrimitiveKind.LONG
    )
    override fun serialize(encoder: Encoder, value: NonSerializedClass) =
        encoder.encodeLong(value.longArg)
    override fun deserialize(decoder: Decoder): NonSerializedClass =
        NonSerializedClass(decoder.decodeLong())
}

private interface TestInterface
