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
import androidx.room.compiler.processing.util.KotlinTypeNames.STRING_CLASS_NAME
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.runKspTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import org.junit.Test

class KspFieldElementTest {
    @Test
    fun simple() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            class Foo {
                val intField: Int = 0
            }
            """.trimIndent()
        )
        runKspTest(sources = listOf(src), succeed = true) { invocation ->
            val fooElement = invocation.processingEnv.requireTypeElement("Foo")
            fooElement
                .getField("intField").let { field ->
                    assertThat(field.name).isEqualTo("intField")
                    assertThat(field.kindName()).isEqualTo("property")
                    assertThat(field.enclosingTypeElement).isEqualTo(fooElement)
                }
        }
    }

    @Test
    fun asMemberOf() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            open class Base<T, R> {
                val t : T = TODO()
                val listOfR : List<R> = TODO()
            }
            class Sub1 : Base<Int, String>()
            """.trimIndent()
        )
        runKspTest(sources = listOf(src), succeed = true) { invocation ->
            val sub = invocation.processingEnv.requireTypeElement("Sub1")
            val base = invocation.processingEnv.requireTypeElement("Base")
            val t = base.getField("t")
            val listOfR = base.getField("listOfR")
            assertThat(t.type.typeName).isEqualTo(ClassName.get("Base", "T"))
            assertThat(listOfR.type.typeName)
                .isEqualTo(
                    ParameterizedTypeName.get(LIST_CLASS_NAME, ClassName.get("Base", "R"))
                )
            assertThat(t.enclosingTypeElement).isEqualTo(base)
            assertThat(listOfR.enclosingTypeElement).isEqualTo(base)
            assertThat(t.asMemberOf(sub.type).typeName).isEqualTo(INT_CLASS_NAME)
            assertThat(listOfR.asMemberOf(sub.type).typeName)
                .isEqualTo(ParameterizedTypeName.get(LIST_CLASS_NAME, STRING_CLASS_NAME))
        }
    }
}
