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

import androidx.room.compiler.processing.util.CONTINUATION_CLASS_NAME
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.className
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethod
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.WildcardTypeName
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
            classpath = listOf(lib)
        ) { invocation ->
            listOf("lib", "app").forEach { pkg ->
                val elm = invocation.processingEnv.requireTypeElement("$pkg.Subject")
                elm.getField("prop").type.let {
                    assertThat(it.nullability).isEqualTo(XNullability.NONNULL)
                    assertThat(it.typeName).isEqualTo(TypeName.LONG)
                }
                elm.getField("nullable").type.let {
                    assertThat(it.nullability).isEqualTo(XNullability.NULLABLE)
                    assertThat(it.typeName).isEqualTo(TypeName.LONG.box())
                }
                elm.getField("inGeneric").type.let {
                    assertThat(it.nullability).isEqualTo(XNullability.NONNULL)
                    assertThat(it.typeName).isEqualTo(
                        ParameterizedTypeName.get(
                            List::class.className(),
                            TypeName.LONG.box()
                        )
                    )
                }
                elm.getMethod("suspendFun").parameters.last().type.let {
                    assertThat(it.typeName).isEqualTo(
                        ParameterizedTypeName.get(
                            CONTINUATION_CLASS_NAME,
                            WildcardTypeName.supertypeOf(TypeName.LONG.box())
                        )
                    )
                }
            }
        }
    }
}