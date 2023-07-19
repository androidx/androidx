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

import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.className
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethodByJvmName
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.WildcardTypeName
import org.junit.Test

class KSAsMemberOfTest {
    @Test
    fun asMemberOfInheritance() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            open class BaseClass<T, R>(val genericProp : T) {
                val normalInt:Int = 3
                val listOfGeneric : List<T> = TODO()
                val mapOfStringToGeneric2 : Map<String, R> = TODO()
                val pairOfGenerics : Pair<T, R> = TODO()
            }
            class SubClass(x : Int) : BaseClass<Int, List<String>>(x) {
                val subClassProp : String = "abc"
            }
            """.trimIndent()
        )

        runProcessorTest(sources = listOf(src)) { invocation ->
            val base = invocation.processingEnv.requireTypeElement("BaseClass")
            val sub = invocation.processingEnv.requireType("SubClass")
            base.getField("normalInt").let { prop ->
                assertThat(
                    prop.asMemberOf(sub).typeName
                ).isEqualTo(
                    TypeName.INT
                )
            }
            base.getField("genericProp").let { prop ->
                assertThat(
                    prop.asMemberOf(sub).typeName
                ).isEqualTo(
                    TypeName.INT.box()
                )
            }
            base.getField("listOfGeneric").let { prop ->
                assertThat(
                    prop.asMemberOf(sub).typeName
                ).isEqualTo(
                    ParameterizedTypeName.get(
                        List::class.className(),
                        TypeName.INT.box()
                    )
                )
            }

            val listOfStringsTypeName =
                ParameterizedTypeName.get(
                    List::class.className(),
                    WildcardTypeName.subtypeOf(String::class.className())
                )
            base.getField("mapOfStringToGeneric2").let { prop ->
                assertThat(
                    prop.asMemberOf(sub).typeName
                ).isEqualTo(
                    ParameterizedTypeName.get(
                        Map::class.className(),
                        String::class.className(),
                        listOfStringsTypeName
                    )
                )
            }

            base.getField("pairOfGenerics").let { prop ->
                assertThat(
                    prop.asMemberOf(sub).typeName
                ).isEqualTo(
                    ParameterizedTypeName.get(
                        Pair::class.className(),
                        TypeName.INT.box(), listOfStringsTypeName
                    )
                )
            }
        }
    }

    @Test
    fun asMemberOfNullabilityResolution() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            open class MyInterface<T> {
                val inheritedProp: T = TODO()
                var nullableProp: T? = TODO()
                val inheritedGenericProp: List<T> = TODO()
                val nullableGenericProp: List<T?> = TODO()
            }
            abstract class NonNullSubject : MyInterface<String>()
            abstract class NullableSubject: MyInterface<String?>()
            """.trimIndent()
        )
        runKspTest(sources = listOf(src)) { invocation ->
            val myInterface = invocation.processingEnv.requireTypeElement("MyInterface")
            val nonNullSubject = invocation.processingEnv.requireType("NonNullSubject")
            val nullableSubject = invocation.processingEnv.requireType("NullableSubject")
            val inheritedProp = myInterface.getField("inheritedProp")
            assertThat(
                inheritedProp.asMemberOf(nonNullSubject).nullability
            ).isEqualTo(XNullability.NONNULL)
            assertThat(
                inheritedProp.asMemberOf(nullableSubject).nullability
            ).isEqualTo(XNullability.NULLABLE)

            val nullableProp = myInterface.getField("nullableProp")
            assertThat(
                nullableProp.asMemberOf(nonNullSubject).nullability
            ).isEqualTo(XNullability.NULLABLE)
            assertThat(
                nullableProp.asMemberOf(nullableSubject).nullability
            ).isEqualTo(XNullability.NULLABLE)

            val inheritedGenericProp = myInterface.getField("inheritedGenericProp")
            inheritedGenericProp.asMemberOf(nonNullSubject).let {
                assertThat(it.nullability).isEqualTo(XNullability.NONNULL)
                assertThat(
                    it.typeArguments.first().nullability
                ).isEqualTo(XNullability.NONNULL)
            }
            inheritedGenericProp.asMemberOf(nullableSubject).let {
                assertThat(it.nullability).isEqualTo(XNullability.NONNULL)
                assertThat(
                    it.typeArguments.first().nullability
                ).isEqualTo(XNullability.NULLABLE)
            }

            val nullableGenericProp = myInterface.getField("nullableGenericProp")
            nullableGenericProp.asMemberOf(nonNullSubject).let {
                assertThat(it.nullability).isEqualTo(XNullability.NONNULL)
                assertThat(
                    it.typeArguments.first().nullability
                ).isEqualTo(XNullability.NULLABLE)
            }
            nullableGenericProp.asMemberOf(nullableSubject).let {
                assertThat(it.nullability).isEqualTo(XNullability.NONNULL)
                assertThat(
                    it.typeArguments.first().nullability
                ).isEqualTo(XNullability.NULLABLE)
            }
        }
    }

    @Test
    fun asMemberOfStatics() {
        val kotlinSrc = Source.kotlin(
            "KotlinClass.kt",
            """
            class KotlinClass {
                companion object {
                    @JvmStatic
                    var staticProp: String = ""
                    @JvmStatic
                    fun staticFun(x:Int) {}
                }
            }
            """.trimIndent()
        )
        val javaSrc = Source.java(
            "JavaClass",
            """
            class JavaClass {
                void staticFun(int x) {}
                static String staticProp;
            }
            """.trimIndent()
        )
        runKspTest(sources = listOf(kotlinSrc, javaSrc)) { invocation ->
            listOf("KotlinClass", "JavaClass").forEach {
                val typeElement = invocation.processingEnv.requireTypeElement(it)
                typeElement.getMethodByJvmName("staticFun").let { staticFun ->
                    val asMember = staticFun.asMemberOf(typeElement.type)
                    assertThat(asMember.returnType.typeName).isEqualTo(TypeName.VOID)
                    assertThat(
                        asMember.parameterTypes.single().typeName
                    ).isEqualTo(TypeName.INT)
                    // different codepath, execute it as well
                    assertThat(
                        staticFun.parameters.single().asMemberOf(typeElement.type).typeName
                    ).isEqualTo(TypeName.INT)
                }
                typeElement.getField("staticProp").let { staticProp ->
                    assertThat(
                        staticProp.asMemberOf(typeElement.type).typeName
                    ).isEqualTo(
                        ClassName.get(String::class.java)
                    )
                }
            }
        }
    }
}
