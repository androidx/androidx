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

import androidx.room.compiler.processing.XNullability.NONNULL
import androidx.room.compiler.processing.XNullability.NULLABLE
import androidx.room.compiler.processing.asDeclaredType
import androidx.room.compiler.processing.isDeclared
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.className
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethod
import androidx.room.compiler.processing.util.kspResolver
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.typeName
import com.google.common.truth.Truth.assertThat
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import com.squareup.javapoet.WildcardTypeName
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
            package foo.bar;
            class Baz : AbstractClass(), MyInterface {
            }
            abstract class AbstractClass {}
            interface MyInterface {}
            """.trimIndent()
        )
        runKspTest(listOf(src)) {
            val subject = it.processingEnv.requireType("foo.bar.Baz")
            assertThat(subject.typeName).isEqualTo(
                ClassName.get("foo.bar", "Baz")
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
        runKspTest(
            listOf(src)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            subject.getField("errorType").type.asDeclaredType().let { type ->
                assertThat(type.isError()).isTrue()
                assertThat(type.typeArguments).isEmpty()
                assertThat(type.typeName).isEqualTo(ERROR_TYPE_NAME)
                assertThat(type.typeElement!!.className).isEqualTo(ERROR_TYPE_NAME)
            }

            subject.getField("listOfErrorType").type.asDeclaredType().let { type ->
                assertThat(type.isError()).isFalse()
                assertThat(type.typeArguments).hasSize(1)
                type.typeArguments.single().let { typeArg ->
                    assertThat(typeArg.isError()).isTrue()
                    assertThat(typeArg.typeName).isEqualTo(ERROR_TYPE_NAME)
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
        runKspTest(
            listOf(src)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            subject.getField("listOfNullableStrings").type.asDeclaredType().let { type ->
                assertThat(type.nullability).isEqualTo(NONNULL)
                assertThat(type.typeArguments).hasSize(1)
                assertThat(type.typeElement!!.className).isEqualTo(
                    List::class.typeName()
                )
                type.typeArguments.single().let { typeArg ->
                    assertThat(typeArg.nullability).isEqualTo(NULLABLE)
                    assertThat(
                        typeArg.isAssignableFrom(
                            invocation.processingEnv.requireType(String::class)
                        )
                    ).isTrue()
                }
            }

            subject.getField("listOfInts").type.asDeclaredType().let { type ->
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
                assertThat(type.typeElement!!.className).isEqualTo(
                    List::class.className()
                )
            }
        }
    }

    @Test
    fun equality() {
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
        runKspTest(
            listOf(src)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            val nullableStringList = subject.getField("listOfNullableStrings")
                .type.asDeclaredType()
            val nonNullStringList = subject.getField("listOfNonNullStrings")
                .type.asDeclaredType()
            assertThat(nullableStringList).isNotEqualTo(nonNullStringList)
            assertThat(nonNullStringList).isNotEqualTo(nullableStringList)

            val nullableStringList_2 = subject.getField("listOfNullableStrings_2")
                .type.asDeclaredType()
            val nonNullStringList_2 = subject.getField("listOfNonNullStrings_2")
                .type.asDeclaredType()
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
        runKspTest(
            listOf(src)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            subject.getField("simple").type.let {
                assertThat(it.rawType.typeName).isEqualTo(TypeName.INT)
            }
            subject.getField("list").type.asDeclaredType().let { list ->
                assertThat(list.rawType).isNotEqualTo(list)
                assertThat(list.typeArguments).isNotEmpty()
                assertThat(list.rawType.typeName)
                    .isEqualTo(ClassName.get("java.util", "List"))
            }
            subject.getField("map").type.asDeclaredType().let { map ->
                assertThat(map.rawType).isNotEqualTo(map)
                assertThat(map.typeArguments).hasSize(2)
                assertThat(map.rawType.typeName)
                    .isEqualTo(ClassName.get("java.util", "Map"))
            }
            subject.getField("listOfMaps").type.asDeclaredType().let { listOfMaps ->
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
        runKspTest(sources = listOf(src)) { invocation ->
            val resolver = (invocation.processingEnv as KspProcessingEnv).resolver
            val voidMethod = resolver.getClassDeclarationByName("foo.bar.Baz")!!
                .getDeclaredFunctions()
                .first {
                    it.simpleName.asString() == "voidMethod"
                }
            val returnType = voidMethod.returnType
            assertThat(
                returnType?.typeName(invocation.kspResolver)
            ).isEqualTo(
                ClassName.get("kotlin", "Unit")
            )
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
        runKspTest(
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
        runKspTest(
            listOf(src)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            fun getDefaultValue(name: String) = subject.getField(name).type.defaultValue()
            // javac types do not check nullability but checking it is more correct
            // since KSP is an opt-in by the developer, it is better for it to be more strict about
            // types.
            assertThat(getDefaultValue("intProp")).isEqualTo("0")
            assertThat(getDefaultValue("nullableIntProp")).isEqualTo("null")
            assertThat(getDefaultValue("longProp")).isEqualTo("0")
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
        runKspTest(
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
        runKspTest(
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
            // incompatible w/ java
            assertThat(check("longProp", "nullableLong")).isFalse()
            assertThat(check("listOfStrings1", "listOfStrings2")).isTrue()
            assertThat(check("listOfStrings1", "listOfNullableStrings")).isFalse()
            assertThat(check("listOfInts", "listOfStrings2")).isFalse()
        }
    }

    @Suppress("MapGetWithNotNullAssertionOperator")
    @Test
    fun extendsBounds() {
        val src = Source.kotlin(
            "foo.kt",
            """
            open class Foo;
            class Bar<T : Foo> {
            }
            class Bar_NullableFoo<T : Foo?>
            """.trimIndent()
        )
        runKspTest(
            listOf(src)
        ) { invocation ->
            val classNames = listOf("Bar", "Bar_NullableFoo")
            val typeArgs = classNames.associateWith { className ->
                invocation.processingEnv
                    .requireType(className)
                    .asDeclaredType()
                    .typeArguments
                    .single()
            }
            val typeName = typeArgs["Bar"]!!.typeName
            assertThat(typeName)
                .isEqualTo(
                    TypeVariableName.get("T", ClassName.get("", "Foo"))
                )
            assertThat(typeArgs["Bar"]!!.nullability).isEqualTo(NONNULL)
            assertThat(typeArgs["Bar_NullableFoo"]!!.typeName)
                .isEqualTo(
                    TypeVariableName.get("T", ClassName.get("", "Foo"))
                )
            assertThat(typeArgs["Bar_NullableFoo"]!!.nullability).isEqualTo(NULLABLE)
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
        runKspTest(
            listOf(src)
        ) { invocation ->

            val method = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
                .getMethod("wildcardMethod")
            val paramType = method.parameters.first().type
            check(paramType.isDeclared())
            val arg1 = paramType.typeArguments.single()
            assertThat(arg1.typeName)
                .isEqualTo(
                    WildcardTypeName.subtypeOf(
                        Number::class.java
                    )
                )
            assertThat(arg1.extendsBound()).isNull()
        }
    }
}