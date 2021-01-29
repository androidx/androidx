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

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.getAllFieldNames
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * see: https://github.com/google/ksp/issues/250
 */
class OrderOfFieldsTest {
    @Test
    fun outOfOrderKotlin() {
        val libSource = Source.kotlin(
            "lib.kt",
            """
            class KotlinClass {
                val b: String = TODO()
                val a: String = TODO()
                val c: String = TODO()
                val isB:String = TODO()
                val isA:String = TODO()
                val isC:String = TODO()
            }
            """.trimIndent()
        )
        val classpath = compileFiles(listOf(libSource))
        runProcessorTest(
            sources = emptyList(),
            classpath = listOf(classpath)
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("KotlinClass")
            assertThat(element.getAllFieldNames())
                .containsExactly("b", "a", "c", "isB", "isA", "isC")
                .inOrder()
        }
    }
}