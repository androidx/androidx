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

import androidx.room.compiler.processing.util.KotlinTypeNames.INT_CLASS_NAME
import androidx.room.compiler.processing.util.KotlinTypeNames.LIST_CLASS_NAME
import androidx.room.compiler.processing.util.KotlinTypeNames.MAP_CLASS_NAME
import androidx.room.compiler.processing.util.KotlinTypeNames.PAIR_CLASS_NAME
import androidx.room.compiler.processing.util.KotlinTypeNames.STRING_CLASS_NAME
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.TestInvocation
import androidx.room.compiler.processing.util.runKspTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ParameterizedTypeName
import org.jetbrains.kotlin.ksp.getDeclaredProperties
import org.jetbrains.kotlin.ksp.symbol.KSClassDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSPropertyDeclaration
import org.jetbrains.kotlin.ksp.symbol.Nullability
import org.junit.Test

class KSAsMemberOfTest {
    @Test
    fun asMemberOfInheritance() {
        val src = Source.kotlin(
            "Foo.kt", """
            open class BaseClass<T, R>(val genericProp : T) {
                val listOfGeneric : List<T> = TODO()
                val mapOfStringToGeneric2 : Map<String, R> = TODO()
                val pairOfGenerics : Pair<T, R> = TODO()
            }
            class SubClass(x : Int) : BaseClass<Int, List<String>>(x) {
                val subClassProp : String = "abc"
            }
        """.trimIndent()
        )

        runKspTest(sources = listOf(src), succeed = true) { invocation ->
            val base = invocation.requireClass("BaseClass")
            val sub = invocation.requireClass("SubClass").asStarProjectedType()
            base.requireProperty("genericProp").let { prop ->
                assertThat(prop.typeAsMemberOf(invocation.kspResolver, sub).typeName())
                    .isEqualTo(INT_CLASS_NAME)
            }
            base.requireProperty("listOfGeneric").let { prop ->
                assertThat(prop.typeAsMemberOf(invocation.kspResolver, sub).typeName())
                    .isEqualTo(ParameterizedTypeName.get(LIST_CLASS_NAME, INT_CLASS_NAME))
            }

            val listOfStringsTypeName =
                ParameterizedTypeName.get(LIST_CLASS_NAME, STRING_CLASS_NAME)
            base.requireProperty("mapOfStringToGeneric2").let { prop ->
                assertThat(prop.typeAsMemberOf(invocation.kspResolver, sub).typeName())
                    .isEqualTo(
                        ParameterizedTypeName.get(
                            MAP_CLASS_NAME, STRING_CLASS_NAME, listOfStringsTypeName
                        )
                    )
            }

            base.requireProperty("pairOfGenerics").let { prop ->
                assertThat(prop.typeAsMemberOf(invocation.kspResolver, sub).typeName())
                    .isEqualTo(
                        ParameterizedTypeName.get(
                            PAIR_CLASS_NAME, INT_CLASS_NAME, listOfStringsTypeName
                        )
                    )
            }
        }
    }

    @Test
    fun asMemberOfNullabilityResolution() {
        val src = Source.kotlin(
            "Foo.kt", """
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
        runKspTest(sources = listOf(src), succeed = true) { invocation ->
            val myInterface = invocation.requireClass("MyInterface")
            val nonNullSubject = invocation.requireClass("NonNullSubject").asStarProjectedType()
            val nullableSubject = invocation.requireClass("NullableSubject").asStarProjectedType()
            val inheritedProp = myInterface.requireProperty("inheritedProp")
            assertThat(
                inheritedProp.typeAsMemberOf(invocation.kspResolver, nonNullSubject).nullability
            ).isEqualTo(Nullability.NOT_NULL)
            assertThat(
                inheritedProp.typeAsMemberOf(invocation.kspResolver, nullableSubject).nullability
            ).isEqualTo(Nullability.NULLABLE)

            val nullableProp = myInterface.requireProperty("nullableProp")
            assertThat(
                nullableProp.typeAsMemberOf(invocation.kspResolver, nonNullSubject).nullability
            ).isEqualTo(Nullability.NULLABLE)
            assertThat(
                nullableProp.typeAsMemberOf(invocation.kspResolver, nullableSubject).nullability
            ).isEqualTo(Nullability.NULLABLE)

            val inheritedGenericProp = myInterface.requireProperty("inheritedGenericProp")
            inheritedGenericProp.typeAsMemberOf(invocation.kspResolver, nonNullSubject).let {
                assertThat(it.nullability).isEqualTo(Nullability.NOT_NULL)
                assertThat(
                    it.arguments.first().type?.resolve()?.nullability
                ).isEqualTo(Nullability.NOT_NULL)
            }
            inheritedGenericProp.typeAsMemberOf(invocation.kspResolver, nullableSubject).let {
                assertThat(it.nullability).isEqualTo(Nullability.NOT_NULL)
                assertThat(
                    it.arguments.first().type?.resolve()?.nullability
                ).isEqualTo(Nullability.NULLABLE)
            }

            val nullableGenericProp = myInterface.requireProperty("nullableGenericProp")
            nullableGenericProp.typeAsMemberOf(invocation.kspResolver, nonNullSubject).let {
                assertThat(it.nullability).isEqualTo(Nullability.NOT_NULL)
                assertThat(
                    it.arguments.first().type?.resolve()?.nullability
                ).isEqualTo(Nullability.NULLABLE)
            }
            nullableGenericProp.typeAsMemberOf(invocation.kspResolver, nullableSubject).let {
                assertThat(it.nullability).isEqualTo(Nullability.NOT_NULL)
                assertThat(
                    it.arguments.first().type?.resolve()?.nullability
                ).isEqualTo(Nullability.NULLABLE)
            }
        }
    }

    private fun TestInvocation.requireClass(name: String): KSClassDeclaration {
        val resolver = (processingEnv as KspProcessingEnv).resolver
        return resolver.requireClass(name)
    }

    private fun KSClassDeclaration.requireProperty(name: String): KSPropertyDeclaration {
        return this.getDeclaredProperties().first {
            it.simpleName.asString() == name
        }
    }
}
