/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.getDeclaredMethodByJvmName
import androidx.room.compiler.processing.util.runKaptTest
import androidx.room.compiler.processing.util.runKspTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class KSTypeVarianceResolverWithTypeParametersTest(
    private val t0: String,
    private val t1: String,
    private val t2: String,
    private val t3: String,
    private val t4: String,
) {
    @Test
    fun testResults() {
        // Note: The compilation results for all parameters of this parameterized test are
        // calculated at the same time (on the first call to compilationResults) because its much
        // faster to compile the sources all together rather than once per parameter. However,
        // there's still benefit to keeping this test parameterized since it makes it easier to
        // parse breakages due to a particular parameter.
        val key = key(t0, t1, t2, t3, t4)
        assertThat(compilationResults.kspSignaturesMap[key])
            .containsExactlyElementsIn(compilationResults.kaptSignaturesMap[key])
            .inOrder()
    }

    private companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-{1}-{2}-{3}-{4}")
        fun params(): List<Array<String>> {
            val genericTypes = listOf("Foo", "FooIn", "FooOut")
            val parameters: MutableList<Array<String>> = mutableListOf()
            for (t0 in genericTypes) {
                for (t1 in genericTypes) {
                    for (t2 in genericTypes) {
                        for (t3 in genericTypes) {
                            for (t4 in genericTypes) {
                                parameters.add(arrayOf(t0, t1, t2, t3, t4))
                            }
                        }
                    }
                }
            }
            return parameters
        }

        val compilationResults: CompilationResults by lazy {
            val sourcesMap = params().associate { (t0, t1, t2, t3, t4) ->
                val key = key(t0, t1, t2, t3, t4)
                key to listOf(
                    source("${key}0", "T", t = "$t0<$t1<$t2<$t3<$t4<Bar>>>>>"),
                    source("${key}1", "$t0<T>", t = "$t1<$t2<$t3<$t4<Bar>>>>"),
                    source("${key}2", "$t0<$t1<T>>", t = "$t2<$t3<$t4<Bar>>>"),
                    source("${key}3", "$t0<$t1<$t2<T>>>", t = "$t3<$t4<Bar>>"),
                    source("${key}4", "$t0<$t1<$t2<$t3<T>>>>", t = "$t4<Bar>"),
                    source("${key}5", "$t0<$t1<$t2<$t3<$t4<T>>>>>", t = "Bar"),
                )
            }
            val sources = sourcesMap.values.flatten() + Source.kotlin(
                "SharedInterfaces.kt",
                """
                interface Foo<T>
                interface FooIn<in T>
                interface FooOut<out T>
                interface Bar
                """.trimIndent()
            )
            val kaptSignaturesMap = buildMap(sourcesMap.size) {
                runKaptTest(sources) {
                    sourcesMap.keys.forEach { key ->
                        put(key, IntRange(0, 5).flatMap { i -> collectSignatures(it, key, i) })
                    }
                }
            }
            val kspSignaturesMap = buildMap(sourcesMap.size) {
                runKspTest(sources) {
                    sourcesMap.keys.forEach { key ->
                        put(key, IntRange(0, 5).flatMap { i -> collectSignatures(it, key, i) })
                    }
                }
            }
            CompilationResults(kaptSignaturesMap, kspSignaturesMap)
        }

        private fun source(suffix: String, type: String, t: String): Source {
            val sub = "Sub$suffix"
            val base = "Base$suffix"
            return Source.kotlin(
                "$sub.kt",
                """
                class $sub: $base<$t>() {
                    fun subMethod(param: $base<$t>): $base<$t> = TODO()
                }
                open class $base<T> {
                    fun baseMethod(param: $type): $type = TODO()
                }
                """.trimIndent()
            )
        }

        private fun collectSignatures(
            invocation: XTestInvocation,
            key: String,
            configuration: Int,
        ): List<String> {
            val subName = "Sub$key$configuration"
            val baseName = "Base$key$configuration"
            val sub = invocation.processingEnv.requireTypeElement(subName)
            val subMethod = sub.getDeclaredMethodByJvmName("subMethod")
            val subSuperclassType = sub.superClass!!
            val subMethodParamType = subMethod.parameters.single().type
            val subMethodReturnType = subMethod.returnType
            val base = invocation.processingEnv.requireTypeElement(baseName)
            // Note: For each method/field we test its signature when resolved asMemberOf from a
            // subtype, super class, param type, and return type, as we may get different signatures
            // depending on the scope of the type used with asMemberOf.
            return buildList {
                base.getDeclaredMethods().forEach { method ->
                    fun XMethodType.signature(): String {
                        val returnType = returnType.typeName
                        val parameters = parameterTypes.map { it.typeName }
                        return "${method.name} : $returnType : $parameters"
                    }
                    val fromSubType = method.asMemberOf(sub.type)
                    val fromSuperClassType = method.asMemberOf(subSuperclassType)
                    val fromParamType = method.asMemberOf(subMethodParamType)
                    val fromReturnType = method.asMemberOf(subMethodReturnType)
                    add("$configuration-fromSub-${fromSubType.signature()}")
                    add("$configuration-fromSuperClass-${fromSuperClassType.signature()}")
                    add("$configuration-fromParam-${fromParamType.signature()}")
                    add("$configuration-fromReturnType-${fromReturnType.signature()}")
                }
                base.getDeclaredFields().forEach { field ->
                    fun XType.signature() = "${field.name} : $typeName"
                    val fromSubType = field.asMemberOf(sub.type)
                    val fromSuperClassType = field.asMemberOf(subSuperclassType)
                    val fromParamType = field.asMemberOf(subMethodParamType)
                    val fromReturnType = field.asMemberOf(subMethodReturnType)
                    add("$configuration-fromSub-${fromSubType.signature()}")
                    add("$configuration-fromSuperClass-${fromSuperClassType.signature()}")
                    add("$configuration-fromParam-${fromParamType.signature()}")
                    add("$configuration-fromReturnType-${fromReturnType.signature()}")
                }
            }
        }

        fun key(t0: String, t1: String, t2: String, t3: String, t4: String) = "$t0$t1$t2$t3$t4"

        data class CompilationResults(
            val kaptSignaturesMap: Map<String, List<String>>,
            val kspSignaturesMap: Map<String, List<String>>,
        )
    }
}