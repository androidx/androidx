/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.ksp

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.asClassName
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XNullability.NONNULL
import androidx.room.compiler.processing.XNullability.NULLABLE
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.isByte
import androidx.room.compiler.processing.isInt
import androidx.room.compiler.processing.isLong
import androidx.room.compiler.processing.isVoid
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.getDeclaredField
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethodByJvmName
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.runProcessorTest
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.javapoet.JClassName
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.JTypeVariableName
import com.squareup.kotlinpoet.javapoet.JWildcardTypeName
import com.squareup.kotlinpoet.javapoet.KClassName
import com.squareup.kotlinpoet.javapoet.KTypeVariableName
import com.squareup.kotlinpoet.javapoet.KWildcardTypeName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KspTypeTest {
    @Test
    fun assignability() {
        val src = Source.kotlin(
            "foo.kt",
            """
            package foo.bar
            class Baz : AbstractClass(), MyInterface {
            }
            abstract class AbstractClass {}
            interface MyInterface {}
            """.trimIndent()
        )
        runProcessorTest(listOf(src)) {
            val subject = it.processingEnv.requireType("foo.bar.Baz")
            assertThat(subject.asTypeName()).isEqualTo(
                XClassName.get("foo.bar", "Baz")
            )
            // basic assertions for abstract class
            val abstractSubject = it.processingEnv.requireType("foo.bar.AbstractClass")
            // basic assertions for interface declaration
            val interfaceSubject = it.processingEnv.requireType("foo.bar.MyInterface")
            // check assignability
            assertThat(
                interfaceSubject.isAssignableFrom(
                    abstractSubject
                )
            ).isFalse()
            assertThat(
                interfaceSubject.isAssignableFrom(
                    subject
                )
            ).isTrue()
            assertThat(
                abstractSubject.isAssignableFrom(
                    subject
                )
            ).isTrue()
        }
    }

    @Test
    fun errorType() {
        val src = Source.kotlin(
            "foo.kt",
            """
            class Subject {
                val errorType : IDontExist = TODO()
                val listOfErrorType : List<IDontExist> = TODO()
            }
            """.trimIndent()
        )
        runProcessorTest(
            listOf(src)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            subject.getField("errorType").type.let { type ->
                assertThat(type.isError()).isTrue()
                assertThat(type.typeArguments).isEmpty()
                assertThat(type.asTypeName().java).isEqualTo(ERROR_JTYPE_NAME)
                assertThat(type.typeElement!!.asClassName().java).isEqualTo(ERROR_JTYPE_NAME)
                if (invocation.isKsp) {
                    assertThat(type.asTypeName().kotlin).isEqualTo(ERROR_KTYPE_NAME)
                    assertThat(type.typeElement!!.asClassName().kotlin).isEqualTo(ERROR_KTYPE_NAME)
                }
            }

            subject.getField("listOfErrorType").type.let { type ->
                assertThat(type.isError()).isFalse()
                assertThat(type.typeArguments).hasSize(1)
                type.typeArguments.single().let { typeArg ->
                    assertThat(typeArg.isError()).isTrue()
                    assertThat(typeArg.asTypeName().java).isEqualTo(ERROR_JTYPE_NAME)
                    if (invocation.isKsp) {
                        assertThat(typeArg.asTypeName().kotlin).isEqualTo(ERROR_KTYPE_NAME)
                    }
                }
            }
            invocation.assertCompilationResult {
                compilationDidFail()
            }
        }
    }

    @Test
    fun typeArguments() {
        val src = Source.kotlin(
            "foo.kt",
            """
            class Subject {
                val listOfNullableStrings : List<String?> = TODO()
                val listOfInts : List<Int> = TODO()
            }
            """.trimIndent()
        )
        runProcessorTest(
            listOf(src)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            subject.getField("listOfNullableStrings").type.let { type ->
                assertThat(type.nullability).isEqualTo(NONNULL)
                assertThat(type.typeArguments).hasSize(1)
                assertThat(type.typeElement!!.asClassName()).isEqualTo(
                    List::class.asClassName()
                )
                type.typeArguments.single().let { typeArg ->
                    assertThat(typeArg.nullability).isEqualTo(NULLABLE)
                    assertThat(
                        typeArg.isAssignableFrom(
                            invocation.processingEnv.requireType(String::class)
                        )
                    ).isTrue()
                    assertThat(typeArg.extendsBound()).isNull()
                }
            }

            subject.getField("listOfInts").type.let { type ->
                assertThat(type.nullability).isEqualTo(NONNULL)
                assertThat(type.typeArguments).hasSize(1)
                type.typeArguments.single().let { typeArg ->
                    assertThat(typeArg.nullability).isEqualTo(NONNULL)
                    assertThat(
                        typeArg.isAssignableFrom(
                            invocation.processingEnv.requireType(Int::class)
                        )
                    ).isTrue()
                }
                assertThat(type.typeElement!!.asClassName()).isEqualTo(
                    List::class.asClassName()
                )
            }
        }
    }

    @Test
    fun equality_ksp() {
        val src = Source.kotlin(
            "foo.kt",
            """
            class Subject {
                val listOfNullableStrings : List<String?> = TODO()
                val listOfNullableStrings_2 : List<String?> = TODO()
                val listOfNonNullStrings : List<String> = TODO()
                val listOfNonNullStrings_2 : List<String> = TODO()
                val nullableString : String? = TODO()
                val nonNullString : String = TODO()
            }
            """.trimIndent()
        )
        // run with javac when JavacType implements equality via isSameType instead of object
        // equality. Room uses isSameType when we need proper equality so it is not important but
        // good for consistency.
        runKspTest(
            listOf(src)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            val nullableStringList = subject.getField("listOfNullableStrings")
                .type
            val nonNullStringList = subject.getField("listOfNonNullStrings")
                .type
            assertThat(nullableStringList).isNotEqualTo(nonNullStringList)
            assertThat(nonNullStringList).isNotEqualTo(nullableStringList)

            val nullableStringList_2 = subject.getField("listOfNullableStrings_2")
                .type
            val nonNullStringList_2 = subject.getField("listOfNonNullStrings_2")
                .type
            assertThat(nullableStringList).isEqualTo(nullableStringList_2)
            assertThat(nonNullStringList).isEqualTo(nonNullStringList_2)

            val nullableString = subject.getField("nullableString").type
            val nonNullString = subject.getField("nonNullString").type
            assertThat(nullableString).isEqualTo(
                nullableStringList.typeArguments.single()
            )
            assertThat(nullableString).isNotEqualTo(
                nonNullStringList.typeArguments.single()
            )
            assertThat(nonNullString).isEqualTo(
                nonNullStringList.typeArguments.single()
            )
            assertThat(nonNullString).isNotEqualTo(
                nullableStringList.typeArguments.single()
            )
        }
    }

    @Test
    fun rawType() {
        val src = Source.kotlin(
            "foo.kt",
            """
            class Subject {
                val simple : Int = 0
                val list : List<String> = TODO()
                val map : Map<String, String> = TODO()
                val listOfMaps : List<Map<String, String>> = TODO()
            }
            """.trimIndent()
        )
        runProcessorTest(
            listOf(src)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            subject.getField("simple").type.let {
                assertThat(it.rawType.asTypeName()).isEqualTo(XTypeName.PRIMITIVE_INT)
            }
            subject.getField("list").type.let { list ->
                assertThat(list.rawType).isNotEqualTo(list)
                assertThat(list.typeArguments).isNotEmpty()
                assertThat(list.rawType.asTypeName()).isEqualTo(List::class.asClassName())
            }
            subject.getField("map").type.let { map ->
                assertThat(map.rawType).isNotEqualTo(map)
                assertThat(map.typeArguments).hasSize(2)
                assertThat(map.rawType.asTypeName())
                    .isEqualTo(Map::class.asClassName())
            }
            subject.getField("listOfMaps").type.let { listOfMaps ->
                assertThat(listOfMaps.rawType).isNotEqualTo(listOfMaps)
                assertThat(listOfMaps.typeArguments).hasSize(1)
            }
        }
    }

    @Test
    fun noneType() {
        val src = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            public class Baz {
                void voidMethod() {
                }
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val voidMethod = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
                .getMethodByJvmName("voidMethod")
            val returnType = voidMethod.returnType
            assertThat(returnType.asTypeName().java)
                .isEqualTo(JTypeName.VOID)
            if (invocation.isKsp) {
                assertThat(returnType.asTypeName().kotlin)
                    .isEqualTo(UNIT)
            }
            assertThat(returnType.isVoid()).isTrue()
        }
    }

    @Test
    fun isTypeChecks() {
        val src = Source.kotlin(
            "foo.kt",
            """
            class Subject {
                val intProp : Int = 0
                val nullableIntProp : Int? = null
                val longProp : Long = 0
                val nullableLongProp : Long? = null
                val byteProp : Byte = 0
                val nullableByteProp :Byte? = null
                val errorProp : IDontExist = TODO()
                val nullableErrorProp : IDontExist? = TODO()
            }
            """.trimIndent()
        )
        runProcessorTest(
            listOf(src)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            fun mapProp(name: String) = subject.getField(name).type.let {
                listOf(
                    "isInt" to it.isInt(),
                    "isLong" to it.isLong(),
                    "isByte" to it.isByte(),
                    "isError" to it.isError(),
                    "isNone" to it.isNone()
                ).filter {
                    it.second
                }.map {
                    it.first
                }
            }
            assertThat(mapProp("intProp")).containsExactly("isInt")
            assertThat(mapProp("nullableIntProp")).containsExactly("isInt")
            assertThat(mapProp("longProp")).containsExactly("isLong")
            assertThat(mapProp("nullableLongProp")).containsExactly("isLong")
            assertThat(mapProp("byteProp")).containsExactly("isByte")
            assertThat(mapProp("nullableByteProp")).containsExactly("isByte")
            assertThat(mapProp("errorProp")).containsExactly("isError")
            assertThat(mapProp("nullableErrorProp")).containsExactly("isError")
            invocation.assertCompilationResult {
                compilationDidFail()
            }
        }
    }

    @Test
    fun defaultValue() {
        val src = Source.kotlin(
            "foo.kt",
            """
            class Subject {
                val intProp : Int = 3 // kotlin default value is unrelated, will be ignored
                val nullableIntProp : Int? = null
                val longProp : Long = 3
                val nullableLongProp : Long? = null
                val floatProp = 3f
                val byteProp : Byte = 0
                val nullableByteProp :Byte? = null
                val errorProp : IDontExist = TODO()
                val nullableErrorProp : IDontExist? = TODO()
                val stringProp : String = "abc"
            }
            """.trimIndent()
        )
        runProcessorTest(
            listOf(src)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            fun getDefaultValue(name: String) = subject.getField(name).type.defaultValue()
            // javac types do not check nullability but checking it is more correct
            // since KSP is an opt-in by the developer, it is better for it to be more strict about
            // types.
            assertThat(getDefaultValue("intProp")).isEqualTo("0")
            assertThat(getDefaultValue("nullableIntProp")).isEqualTo("null")
            assertThat(getDefaultValue("longProp")).isEqualTo("0L")
            assertThat(getDefaultValue("nullableLongProp")).isEqualTo("null")
            assertThat(getDefaultValue("floatProp")).isEqualTo("0f")
            assertThat(getDefaultValue("byteProp")).isEqualTo("0")
            assertThat(getDefaultValue("nullableByteProp")).isEqualTo("null")
            assertThat(getDefaultValue("errorProp")).isEqualTo("null")
            assertThat(getDefaultValue("nullableErrorProp")).isEqualTo("null")
            assertThat(getDefaultValue("stringProp")).isEqualTo("null")
            invocation.assertCompilationResult {
                compilationDidFail()
            }
        }
    }

    @Test
    fun isTypeOf() {
        val src = Source.kotlin(
            "foo.kt",
            """
            class Subject {
                val intProp : Int = 3
                val longProp : Long = 3
                val stringProp : String = "abc"
                val listProp : List<String> = TODO()
            }
            """.trimIndent()
        )
        runProcessorTest(
            listOf(src)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            assertThat(
                subject.getField("stringProp").type.isTypeOf(
                    String::class
                )
            ).isTrue()
            assertThat(
                subject.getField("intProp").type.isTypeOf(
                    Int::class
                )
            ).isTrue()
            assertThat(
                subject.getField("longProp").type.isTypeOf(
                    Long::class
                )
            ).isTrue()
            assertThat(
                subject.getField("listProp").type.isTypeOf(
                    List::class
                )
            ).isTrue()
            assertThat(
                subject.getField("listProp").type.isTypeOf(
                    Set::class
                )
            ).isFalse()
            assertThat(
                subject.getField("listProp").type.isTypeOf(
                    Iterable::class
                )
            ).isFalse()
        }
    }

    @Test
    fun isSameType() {
        val src = Source.kotlin(
            "foo.kt",
            """
            class Subject {
                val intProp : Int = 3
                val intProp2 : Int = 4
                val longProp : Long = 0L
                val nullableLong : Long? = null
                val listOfStrings1 : List<String> = TODO()
                val listOfStrings2 : List<String> = TODO()
                val listOfInts : List<Int> = TODO()
                val listOfNullableStrings : List<String?> = TODO()
            }
            """.trimIndent()
        )
        runProcessorTest(
            listOf(src)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            fun check(prop1: String, prop2: String): Boolean {
                return subject.getField(prop1).type.isSameType(
                    subject.getField(prop2).type
                )
            }
            assertThat(check("intProp", "intProp2")).isTrue()
            assertThat(check("intProp2", "intProp")).isTrue()
            assertThat(check("intProp", "longProp")).isFalse()
            assertThat(check("longProp", "nullableLong")).isFalse()
            assertThat(check("listOfStrings1", "listOfStrings2")).isTrue()
            assertThat(
                check("listOfStrings1", "listOfNullableStrings")
            ).isEqualTo(!invocation.isKsp) // javac ignores nullability
            assertThat(check("listOfInts", "listOfStrings2")).isFalse()
        }
    }

    @Suppress("MapGetWithNotNullAssertionOperator")
    @Test
    fun upperBounds() {
        runProcessorTest(
            listOf(
                Source.kotlin(
                    "Foo.kt",
                    """
                    open class Foo
                    class Bar<T : Foo>
                    class Bar_NullableFoo<T : Foo?>
                    """.trimIndent()
                )
            )
        ) { invocation ->
            fun checkTypeElement(
                typeElement: XTypeElement,
                typeArgumentJClassName: JTypeVariableName,
                typeArgumentKClassName: TypeVariableName,
                nullability: XNullability,
            ) {
                val typeParameter = typeElement.typeParameters.single()
                assertThat(typeParameter.typeVariableName).isEqualTo(typeArgumentJClassName)

                val typeArgument = typeElement.type.typeArguments.single()
                assertThat(typeArgument.asTypeName().java).isEqualTo(typeArgumentJClassName)
                if (invocation.isKsp) {
                    assertThat(typeArgument.asTypeName().kotlin).isEqualTo(typeArgumentKClassName)
                }
                assertThat(typeArgument.nullability).isEqualTo(nullability)
            }
            checkTypeElement(
                typeElement = invocation.processingEnv.requireTypeElement("Bar"),
                typeArgumentJClassName = JTypeVariableName.get("T", JClassName.get("", "Foo")),
                typeArgumentKClassName = KTypeVariableName("T", KClassName("", "Foo")),
                nullability = NONNULL
            )
            checkTypeElement(
                typeElement = invocation.processingEnv.requireTypeElement("Bar_NullableFoo"),
                typeArgumentJClassName = JTypeVariableName.get("T", JClassName.get("", "Foo")),
                typeArgumentKClassName = KTypeVariableName(
                    "T", KClassName("", "Foo").copy(nullable = true)
                ),
                nullability = NULLABLE
            )
        }
    }

    @Suppress("MapGetWithNotNullAssertionOperator")
    @Test
    fun extendsBounds() {
        runProcessorTest(
            listOf(
                Source.kotlin(
                    "Foo.kt",
                    """
                    class Foo {
                        val barFoo: Bar<out Foo> = TODO()
                        val barNullableFoo: Bar<out Foo?> = TODO()
                    }
                    class Bar<T>
                    """.trimIndent()
                )
            )
        ) { invocation ->
            fun checkType(
                type: XType,
                typeArgumentJClassName: JWildcardTypeName,
                typeArgumentKClassName: KWildcardTypeName,
                nullability: XNullability,
            ) {
                val typeArgument = type.typeArguments.single()
                assertThat(typeArgument.asTypeName().java).isEqualTo(typeArgumentJClassName)
                if (invocation.isKsp) {
                    assertThat(typeArgument.asTypeName().kotlin).isEqualTo(typeArgumentKClassName)
                }
                assertThat(typeArgument.nullability).isEqualTo(nullability)
            }
            val foo = invocation.processingEnv.requireTypeElement("Foo")
            checkType(
                type = foo.getDeclaredField("barFoo").type,
                typeArgumentJClassName = JWildcardTypeName.subtypeOf(JClassName.get("", "Foo")),
                typeArgumentKClassName = KWildcardTypeName.producerOf(KClassName("", "Foo")),
                nullability = NONNULL
            )
            checkType(
                type = foo.getDeclaredField("barNullableFoo").type,
                typeArgumentJClassName = JWildcardTypeName.subtypeOf(JClassName.get("", "Foo")),
                typeArgumentKClassName = KWildcardTypeName.producerOf(
                    KClassName("", "Foo").copy(nullable = true)
                ),
                nullability = NULLABLE
            )
        }
    }

    @Test
    fun wildcardJava() {
        val src = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            import java.util.List;
            public class Baz {
                private void wildcardMethod(List<? extends Number> list) {
                }
            }
            """.trimIndent()
        )
        runProcessorTest(
            listOf(src)
        ) { invocation ->
            val typeElement = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
            val method = typeElement.getMethodByJvmName("wildcardMethod")
            val asMember = method.asMemberOf(typeElement.type)
            fun assertParamType(paramType: XType) {
                val arg1 = paramType.typeArguments.single()
                assertThat(arg1.asTypeName().java)
                    .isEqualTo(
                        JWildcardTypeName.subtypeOf(Number::class.java)
                    )
                assertThat(arg1.extendsBound()?.asTypeName()?.java).isEqualTo(
                    JClassName.get("java.lang", "Number")
                )
                if (invocation.isKsp) {
                    assertThat(arg1.asTypeName().kotlin)
                        .isEqualTo(
                            KWildcardTypeName.producerOf(Number::class)
                        )
                    assertThat(arg1.extendsBound()?.asTypeName()?.kotlin).isEqualTo(
                        KClassName("kotlin", "Number")
                    )
                }
                assertThat(
                    arg1.extendsBound()?.extendsBound()
                ).isNull()
            }
            assertParamType(method.parameters.first().type)
            assertParamType(asMember.parameterTypes.first())
        }
    }

    @Test
    fun oneSuperClass() {
        val src = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            class A {}
            interface B {}
            class Baz extends A implements B, C {}
            """.trimIndent()
        )
        runKspTest(
            listOf(src)
        ) { invocation ->
            val typeElement = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
            val exception = assertThrows(IllegalStateException::class) {
                typeElement.type.superTypes
            }
            exception.hasMessageThat().isEqualTo(
                "Class foo.bar.Baz should have only one super class." +
                    " Found 2 (foo.bar.A, error.NonExistentClass).")
            invocation.assertCompilationResult {
                compilationDidFail()
            }
        }
    }
}
