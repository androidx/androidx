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

import androidx.kruth.assertThat
import androidx.room.compiler.processing.testcode.KotlinTestClass
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.asJTypeName
import androidx.room.compiler.processing.util.getMethodByJvmName
import androidx.room.compiler.processing.util.getParameter
import androidx.room.compiler.processing.util.runKaptTest
import androidx.room.compiler.processing.util.runProcessorTest
import org.junit.Ignore
import org.junit.Test

class KotlinMetadataTest {
    @Test
    fun readWithMetadata() {
        val source =
            Source.kotlin(
                "Dummy.kt",
                """
            class Dummy
            """
                    .trimIndent()
            )
        runProcessorTest(sources = listOf(source)) {
            val element = it.processingEnv.requireTypeElement(KotlinTestClass::class)
            element.getMethodByJvmName("mySuspendMethod").apply {
                assertThat(parameters).hasSize(2)
                assertThat(getParameter("param1").type.asTypeName().java)
                    .isEqualTo(String::class.asJTypeName())
                assertThat(isSuspendFunction()).isTrue()
            }
        }
    }

    @Ignore("b/360398921")
    @Test
    fun inlineReifiedFunctionAndKAPT4() {
        val source =
            Source.kotlin(
                "Foo.kt",
                """
                class Foo {
                    val f: String = "hi"
                    inline fun <reified T> inlineReifiedFun(t: T) {}
                }
                """
                    .trimIndent()
            )
        runKaptTest(sources = listOf(source), kotlincArguments = listOf("-Xuse-kapt4")) { invocation
            ->
            invocation.processingEnv.requireTypeElement("Foo").let { element ->
                val f = element.getDeclaredFields().single()
                // This shouldn't throw NullPointerException when inline reified functions are
                // present.
                f.getAllAnnotations().let {
                    if (invocation.isKsp) {
                        assertThat(it).isEmpty()
                    } else {
                        assertThat(it.count()).isEqualTo(1)
                        assertThat(it.single().name).isEqualTo("NotNull")
                    }
                }
            }
        }
    }
}
