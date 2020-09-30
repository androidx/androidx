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
import org.junit.Test

class KSProertyDeclarationExtTest {
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
                assertThat(prop.typeAsMemberOf(sub).typeName()).isEqualTo(INT_CLASS_NAME)
            }
            base.requireProperty("listOfGeneric").let { prop ->
                assertThat(prop.typeAsMemberOf(sub).typeName())
                    .isEqualTo(ParameterizedTypeName.get(LIST_CLASS_NAME, INT_CLASS_NAME))
            }

            val listOfStringsTypeName =
                ParameterizedTypeName.get(LIST_CLASS_NAME, STRING_CLASS_NAME)
            base.requireProperty("mapOfStringToGeneric2").let { prop ->
                assertThat(prop.typeAsMemberOf(sub).typeName())
                    .isEqualTo(
                        ParameterizedTypeName.get(
                            MAP_CLASS_NAME, STRING_CLASS_NAME, listOfStringsTypeName
                        )
                    )
            }

            base.requireProperty("pairOfGenerics").let { prop ->
                assertThat(prop.typeAsMemberOf(sub).typeName())
                    .isEqualTo(
                        ParameterizedTypeName.get(
                            PAIR_CLASS_NAME, INT_CLASS_NAME, listOfStringsTypeName
                        )
                    )
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
