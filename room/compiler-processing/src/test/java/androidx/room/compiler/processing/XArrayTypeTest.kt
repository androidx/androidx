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

package androidx.room.compiler.processing

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.TypeName
import org.junit.Test

class XArrayTypeTest {
    @Test
    fun xArrayType() {
        val source = Source.java(
            "foo.bar.Baz", """
            package foo.bar;
            class Baz {
                String[] param;
            }
        """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(source)
        ) {
            val type = it.processingEnv
                .requireTypeElement("foo.bar.Baz")
                .getField("param")
                .type
            assertThat(type.isArray()).isTrue()
            assertThat(type.typeName).isEqualTo(
                ArrayTypeName.of(TypeName.get(String::class.java))
            )
            assertThat(type.asArray().componentType.typeName).isEqualTo(
                TypeName.get(String::class.java)
            )

            val objArray = it.processingEnv.getArrayType(
                TypeName.OBJECT
            )
            assertThat(objArray.isArray()).isTrue()
            assertThat(objArray.asArray().componentType.typeName).isEqualTo(
                TypeName.OBJECT
            )
            assertThat(objArray.typeName).isEqualTo(
                ArrayTypeName.of(TypeName.OBJECT)
            )
        }
    }

    @Test
    fun notAnArray() {
        runProcessorTest {
            val list = it.processingEnv.requireType("java.util.List")
            assertThat(list.isArray()).isFalse()
        }
    }
}
