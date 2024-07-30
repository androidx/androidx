/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.testing

import androidx.kruth.assertThat
import androidx.room.compiler.processing.util.CompilationTestCapabilities
import androidx.room.compiler.processing.util.Source
import androidx.room.runProcessorTestWithK1
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class InProcessorTest(private val kotlinCode: Boolean) {
    @Test
    fun testInProcessorTestRuns() {
        val source =
            if (kotlinCode) {
                Source.kotlin(
                    filePath = "MyClass.kt",
                    code =
                        """
                package foo.bar
                abstract class MyClass {
                @androidx.room.Query("foo")
                abstract fun setFoo(foo: String):Unit
                }
                """
                            .trimIndent()
                )
            } else {
                Source.java(
                    qName = "foo.bar.MyClass",
                    code =
                        """
                package foo.bar;
                abstract public class MyClass {
                @androidx.room.Query("foo")
                abstract public void setFoo(String foo);
                }
                """
                            .trimIndent()
                )
            }

        var runCount = 0
        runProcessorTestWithK1(sources = listOf(source)) {
            assertThat(it.processingEnv.findTypeElement("foo.bar.MyClass")).isNotNull()
            runCount++
        }
        // run 1 or 2 times
        // +1 if KSP is enabled
        // 1 for javac or kapt depending on whether source is in kotlin or java
        assertThat(runCount)
            .isEqualTo(
                1 +
                    if (CompilationTestCapabilities.canTestWithKsp) {
                        1
                    } else {
                        0
                    }
            )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "kotlinCode_{0}")
        fun params() = arrayOf(true, false)
    }
}
