/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.codegen

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room.compiler.processing.KnownTypeNames
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethodByJvmName
import androidx.room.compiler.processing.util.runProcessorTest
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.javapoet.JClassName
import org.junit.Test

class XTypeNameTest {

    @Test
    fun equality() {
        assertThat(XTypeName(java = JClassName.INT.box(), kotlin = INT))
            .isEqualTo(XTypeName(java = JClassName.INT.box(), kotlin = INT))

        assertThat(XTypeName(java = JClassName.INT.box(), kotlin = INT))
            .isNotEqualTo(XTypeName(java = JClassName.INT.box(), kotlin = SHORT))

        assertThat(XTypeName(java = JClassName.INT.box(), kotlin = SHORT))
            .isNotEqualTo(XTypeName(java = JClassName.INT.box(), kotlin = INT))
    }

    @Test
    fun equality_kotlinUnavailable() {
        assertThat(XTypeName(java = JClassName.INT.box(), kotlin = INT))
            .isEqualTo(
                XTypeName(java = JClassName.INT.box(), kotlin = XTypeName.UNAVAILABLE_KTYPE_NAME)
            )

        assertThat(
                XTypeName(java = JClassName.INT.box(), kotlin = XTypeName.UNAVAILABLE_KTYPE_NAME)
            )
            .isEqualTo(XTypeName(java = JClassName.INT.box(), kotlin = INT))

        assertThat(XTypeName(java = JClassName.SHORT.box(), kotlin = SHORT))
            .isNotEqualTo(
                XTypeName(java = JClassName.INT.box(), kotlin = XTypeName.UNAVAILABLE_KTYPE_NAME)
            )

        assertThat(
                XTypeName(java = JClassName.INT.box(), kotlin = XTypeName.UNAVAILABLE_KTYPE_NAME)
            )
            .isNotEqualTo(XTypeName(java = JClassName.SHORT.box(), kotlin = SHORT))
    }

    @Test
    fun hashCode_kotlinUnavailable() {
        val expectedClass = XClassName.get("foo", "Bar")
        assertThat(
                XTypeName(
                        java = JClassName.get("foo", "Bar"),
                        kotlin = XTypeName.UNAVAILABLE_KTYPE_NAME
                    )
                    .hashCode()
            )
            .isEqualTo(expectedClass.hashCode())
    }

    @Test
    fun rawType() {
        val expectedRawClass = XClassName.get("foo", "Bar")
        assertThat(expectedRawClass.parametrizedBy(String::class.asClassName()).rawTypeName)
            .isEqualTo(expectedRawClass)
    }

    @Test
    fun equalsIgnoreNullability() {
        assertThat(
                XTypeName.BOXED_INT.copy(nullable = false)
                    .equalsIgnoreNullability(XTypeName.BOXED_INT.copy(nullable = true))
            )
            .isTrue()

        assertThat(
                XTypeName.BOXED_INT.copy(nullable = false)
                    .equalsIgnoreNullability(XTypeName.BOXED_LONG.copy(nullable = true))
            )
            .isFalse()
    }

    @Test
    fun toString_codeLanguage() {
        assertThat(XTypeName.ANY_OBJECT.toString(CodeLanguage.JAVA)).isEqualTo("java.lang.Object")
        assertThat(XTypeName.ANY_OBJECT.toString(CodeLanguage.KOTLIN)).isEqualTo("kotlin.Any")
    }

    @Test
    fun mutations_kotlinUnavailable() {
        val typeName =
            XClassName(
                java = JClassName.get("test", "Foo"),
                kotlin = XTypeName.UNAVAILABLE_KTYPE_NAME,
                nullability = XNullability.UNKNOWN
            )
        assertThat(typeName.copy(nullable = true).kotlin)
            .isEqualTo(XTypeName.UNAVAILABLE_KTYPE_NAME)
        assertThat(typeName.copy(nullable = false).kotlin)
            .isEqualTo(XTypeName.UNAVAILABLE_KTYPE_NAME)
        assertThat(typeName.parametrizedBy(XTypeName.BOXED_LONG).kotlin)
            .isEqualTo(XTypeName.UNAVAILABLE_KTYPE_NAME)
        assertThat(XTypeName.getArrayName(typeName).kotlin)
            .isEqualTo(XTypeName.UNAVAILABLE_KTYPE_NAME)
        assertThat(XTypeName.getConsumerSuperName(typeName).kotlin)
            .isEqualTo(XTypeName.UNAVAILABLE_KTYPE_NAME)
        assertThat(XTypeName.getProducerExtendsName(typeName).kotlin)
            .isEqualTo(XTypeName.UNAVAILABLE_KTYPE_NAME)
    }

    @Test
    fun arrays() {
        XTypeName.getArrayName(Number::class.asClassName()).let {
            assertThat(it.toString(CodeLanguage.JAVA)).isEqualTo("java.lang.Number[]")
            assertThat(it.toString(CodeLanguage.KOTLIN)).isEqualTo("kotlin.Array<kotlin.Number>")
        }
        XTypeName.getArrayName(XTypeName.getProducerExtendsName(Number::class.asClassName())).let {
            assertThat(it.toString(CodeLanguage.JAVA)).isEqualTo("java.lang.Number[]")
            assertThat(it.toString(CodeLanguage.KOTLIN))
                .isEqualTo("kotlin.Array<out kotlin.Number>")
        }
        assertThrows<IllegalArgumentException> {
                XTypeName.getArrayName(XTypeName.getConsumerSuperName(Number::class.asClassName()))
            }
            .hasMessageThat()
            .isEqualTo(
                "Can't have contra-variant component types in Java " +
                    "arrays. Found '? super java.lang.Number'."
            )
    }

    @Test
    fun testKotlinUnit() {
        val voidUnit = XTypeName.UNIT_VOID
        val unit = KnownTypeNames.KOTLIN_UNIT
        val inUnit = XTypeName.getConsumerSuperName(unit)
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            class Foo {
                var accessorField: Unit = Unit
                    get() = Unit
                    set(value) {
                        field = value
                    }
                fun f1(): Unit = TODO()
                fun f2(): (Unit) -> Unit = TODO()
                fun f3(): List<Unit> = TODO()
                fun f4(u: Unit) {}
                fun f5(l: (Unit) -> Unit) {}
                fun f6(l: List<Unit>) {}
                fun f7(): Unit? = TODO()
            }
            open class Parent<T> {
                val field: T = TODO()
                val listField: List<T> = TODO()
                val lambdaField: (T) -> T = TODO()
                var accessorField: T = TODO()
                    get() = TODO()
                    set(value) {
                        field = value
                    }
                fun f(t: T): T = t
                fun <T> fWithTypeVar(): T = TODO()
            }
            class Child: Parent<Unit>()
            """
                    .trimIndent()
            )
        runProcessorTest(sources = listOf(src)) { invocation ->
            invocation.processingEnv.requireTypeElement("Foo").let { cls ->
                cls.getField("accessorField").getter!!.let { getter ->
                    assertThat(getter.returnType.asTypeName()).isEqualTo(unit)
                    assertThat(getter.asMemberOf(cls.type).returnType.asTypeName()).isEqualTo(unit)
                }
                cls.getField("accessorField").setter!!.let { setter ->
                    assertThat(setter.returnType.asTypeName()).isEqualTo(voidUnit)
                    assertThat(setter.parameters.single().type.asTypeName()).isEqualTo(unit)
                    setter.asMemberOf(cls.type).let { setterType ->
                        assertThat(setterType.returnType.asTypeName()).isEqualTo(voidUnit)
                        assertThat(setterType.parameterTypes.single().asTypeName()).isEqualTo(unit)
                    }
                }
                // When used directly in return types it should be `void/kotlinUnit`.
                cls.getMethodByJvmName("f1").let { method ->
                    assertThat(method.returnType.asTypeName()).isEqualTo(voidUnit)
                    assertThat(method.asMemberOf(cls.type).returnType.asTypeName())
                        .isEqualTo(voidUnit)
                }
                cls.getMethodByJvmName("f2").returnType.let { lambdaType ->
                    assertThat(lambdaType.typeArguments[0].asTypeName()).isEqualTo(unit)
                    assertThat(lambdaType.typeArguments[1].asTypeName()).isEqualTo(unit)
                }
                assertThat(
                        cls.getMethodByJvmName("f3").returnType.typeArguments.single().asTypeName()
                    )
                    .isEqualTo(unit)
                assertThat(cls.getMethodByJvmName("f4").parameters.single().type.asTypeName())
                    .isEqualTo(unit)
                cls.getMethodByJvmName("f5").parameters.single().let { funParam ->
                    funParam.type.typeArguments[0].asTypeName().let { paramTypeName ->
                        if (invocation.isKsp) {
                            assertThat(paramTypeName).isEqualTo(unit)
                        } else {
                            // TODO: Somehow KAPT keeps the variance for param type.
                            assertThat(paramTypeName.java).isEqualTo(inUnit.java)
                        }
                    }
                    assertThat(funParam.type.typeArguments[1].asTypeName()).isEqualTo(unit)
                }
                assertThat(
                        cls.getMethodByJvmName("f6")
                            .parameters
                            .single()
                            .type
                            .typeArguments
                            .single()
                            .asTypeName()
                    )
                    .isEqualTo(unit)
                assertThat(cls.getMethodByJvmName("f7").returnType.asTypeName())
                    .isEqualTo(unit.copy(nullable = true))
            }
            invocation.processingEnv.requireTypeElement("Child").let { cls ->
                assertThat(cls.getField("field").asMemberOf(cls.type).asTypeName()).isEqualTo(unit)
                cls.getField("listField").asMemberOf(cls.type).asTypeName().let { fieldType ->
                    assertThat(fieldType).isEqualTo(List::class.asClassName().parametrizedBy(unit))
                }
                assertThat(
                        cls.getField("accessorField")
                            .getter!!
                            .asMemberOf(cls.type)
                            .returnType
                            .asTypeName()
                    )
                    .isEqualTo(unit)
                cls.getField("accessorField").setter!!.asMemberOf(cls.type).let { setterType ->
                    assertThat(setterType.returnType.asTypeName()).isEqualTo(voidUnit)
                    assertThat(setterType.parameterTypes.single().asTypeName()).isEqualTo(unit)
                }
                cls.superClass!!.asTypeName().let { superType ->
                    assertThat(superType)
                        .isEqualTo(XClassName.get("", "Parent").parametrizedBy(unit))
                }
                cls.getMethodByJvmName("f").asMemberOf(cls.type).let { funType ->
                    assertThat(funType.parameterTypes.single().asTypeName()).isEqualTo(unit)
                    // When `kotlin.Unit` is used in a type argument + return type it's not
                    // void to Java anymore.
                    assertThat(funType.returnType.asTypeName()).isEqualTo(unit)
                }
            }
        }
        val javaSrc =
            Source.java(
                "Foo",
                """
            import kotlin.Unit;
            class Foo {
                Unit f() {
                    return Unit.INSTANCE;
                }
                void g() {}
            }
        """
                    .trimIndent()
            )
        runProcessorTest(listOf(javaSrc)) { invocation ->
            invocation.processingEnv.requireTypeElement("Foo").let { cls ->
                assertThat(cls.getMethodByJvmName("f").returnType.asTypeName())
                    .isEqualTo(unit.copy(nullable = true))
                assertThat(cls.getMethodByJvmName("g").returnType.asTypeName()).isEqualTo(voidUnit)
            }
        }
    }
}
