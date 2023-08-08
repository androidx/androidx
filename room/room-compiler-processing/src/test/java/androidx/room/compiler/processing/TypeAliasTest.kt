/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing

import androidx.kruth.assertThat
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.asClassName
import androidx.room.compiler.processing.util.CONTINUATION_JCLASS_NAME
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.asJClassName
import androidx.room.compiler.processing.util.asKClassName
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethodByJvmName
import androidx.room.compiler.processing.util.runProcessorTest
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.javapoet.JParameterizedTypeName
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.JWildcardTypeName
import kotlin.coroutines.Continuation
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TypeAliasTest {
    @Test
    fun kotlinTypeAlias() {
        fun produceSource(pkg: String) = Source.kotlin(
            "$pkg/Foo.kt",
            """
            package $pkg
            typealias MyLong = Long
            class Subject {
                var prop: MyLong = 1L
                val nullable: MyLong? = null
                val inGeneric : List<MyLong> = TODO()
                suspend fun suspendFun() : MyLong = TODO()
            }
            """.trimIndent()
        )
        val lib = compileFiles(listOf(produceSource("lib")))
        runProcessorTest(
            sources = listOf(produceSource("app")),
            classpath = lib
        ) { invocation ->
            listOf("lib", "app").forEach { pkg ->
                val elm = invocation.processingEnv.requireTypeElement("$pkg.Subject")
                elm.getField("prop").type.let {
                    assertThat(it.nullability).isEqualTo(XNullability.NONNULL)
                    assertThat(it.asTypeName()).isEqualTo(XTypeName.PRIMITIVE_LONG)
                }
                elm.getField("nullable").type.let {
                    assertThat(it.nullability).isEqualTo(XNullability.NULLABLE)
                    assertThat(it.asTypeName())
                        .isEqualTo(Long::class.asClassName().copy(nullable = true))
                }
                elm.getField("inGeneric").type.let {
                    assertThat(it.nullability).isEqualTo(XNullability.NONNULL)
                    assertThat(it.asTypeName().java).isEqualTo(
                        JParameterizedTypeName.get(
                            List::class.asJClassName(),
                            JTypeName.LONG.box()
                        )
                    )
                    if (invocation.isKsp) {
                        assertThat(it.asTypeName().kotlin).isEqualTo(
                            List::class.asKClassName().parameterizedBy(LONG)
                        )
                    }
                }
                elm.getMethodByJvmName("suspendFun").parameters.last().type.let {
                    assertThat(it.asTypeName().java).isEqualTo(
                        JParameterizedTypeName.get(
                            CONTINUATION_JCLASS_NAME,
                            JWildcardTypeName.supertypeOf(JTypeName.LONG.box())
                        )
                    )
                    if (invocation.isKsp) {
                        assertThat(it.asTypeName().kotlin).isEqualTo(
                            Continuation::class.asKClassName().parameterizedBy(LONG)
                        )
                    }
                }
            }
        }
    }
}
