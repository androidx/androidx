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

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.className
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.runProcessorTestIncludingKsp
import androidx.room.compiler.processing.util.typeName
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import org.junit.Test

class KspFieldElementTest {
    @Test
    fun simple() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            class Foo {
                val intField: Int = 0
                @JvmField
                val jvmField: Int = 0
                protected val protectedField: Int = 0
                @JvmField
                protected val protectedJvmField: Int = 0
            }
            """.trimIndent()
        )
        runKspTest(sources = listOf(src), succeed = true) { invocation ->
            val fooElement = invocation.processingEnv.requireTypeElement("Foo")
            fooElement.getField("intField").let { field ->
                assertThat(field.name).isEqualTo("intField")
                assertThat(field.kindName()).isEqualTo("property")
                assertThat(field.enclosingTypeElement).isEqualTo(fooElement)
                assertThat(field.isPublic()).isFalse()
                assertThat(field.isProtected()).isFalse()
                // from java, it is private
                assertThat(field.isPrivate()).isTrue()
            }
            fooElement.getField("jvmField").let { field ->
                assertThat(field.name).isEqualTo("jvmField")
                assertThat(field.kindName()).isEqualTo("property")
                assertThat(field.enclosingTypeElement).isEqualTo(fooElement)
                assertThat(field.isPublic()).isTrue()
                assertThat(field.isProtected()).isFalse()
                assertThat(field.isPrivate()).isFalse()
            }
            fooElement.getField("protectedField").let { field ->
                assertThat(field.name).isEqualTo("protectedField")
                assertThat(field.kindName()).isEqualTo("property")
                assertThat(field.enclosingTypeElement).isEqualTo(fooElement)
                assertThat(field.isPublic()).isFalse()
                assertThat(field.isProtected()).isFalse()
                // from java, it is private
                assertThat(field.isPrivate()).isTrue()
            }
            fooElement.getField("protectedJvmField").let { field ->
                assertThat(field.name).isEqualTo("protectedJvmField")
                assertThat(field.kindName()).isEqualTo("property")
                assertThat(field.enclosingTypeElement).isEqualTo(fooElement)
                assertThat(field.isPublic()).isFalse()
                assertThat(field.isProtected()).isTrue()
                assertThat(field.isPrivate()).isFalse()
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
        runProcessorTestIncludingKsp(sources = listOf(src)) { invocation ->
            val sub = invocation.processingEnv.requireTypeElement("Sub1")
            val base = invocation.processingEnv.requireTypeElement("Base")
            val t = base.getField("t")
            val listOfR = base.getField("listOfR")
            if (invocation.isKsp) {
                // KSP replaces unspecified type parameters with Any? while javac keeps them as is.
                // This might be an issue when detecting errors but besides that it should be OK.
                // It other words, it shouldn't be an issue in proper code. It will only have an
                // impact when Room wants to report `unbound generics` errors as we won't
                // recognize them and instead assume they were set as Object.
                // see: UNBOUND_GENERICS errors in ProcessorErrors)
                assertThat(t.type.typeName).isEqualTo(TypeName.OBJECT)
            } else {
                assertThat(t.type.typeName).isEqualTo(TypeVariableName.get("T"))
            }
            val typeVariableName = if (invocation.isKsp) {
                "E" // ksp reads from class declaration
            } else {
                "R" // javac reads from variable declaration
            }
            assertThat(listOfR.type.typeName)
                .isEqualTo(
                    ParameterizedTypeName.get(
                        List::class.className(),
                        TypeVariableName.get(typeVariableName)
                    )
                )

            assertThat(t.enclosingTypeElement).isEqualTo(base)
            assertThat(listOfR.enclosingTypeElement).isEqualTo(base)
            assertThat(t.asMemberOf(sub.type).typeName).isEqualTo(TypeName.INT.box())
            assertThat(listOfR.asMemberOf(sub.type).typeName)
                .isEqualTo(
                    ParameterizedTypeName.get(
                        List::class.className(),
                        String::class.typeName()
                    )
                )
        }
    }
}
