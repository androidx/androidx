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

import androidx.room.compiler.processing.util.CompilationTestCapabilities
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InProcessorTest {
    @Test
    fun testInProcessorTestRuns() {
        val source = Source.java(
            qName = "foo.bar.MyClass",
            code = """
                package foo.bar;
                abstract public class MyClass {
                @androidx.room.Query("foo")
                abstract public void setFoo(String foo);
                }
            """.trimIndent()
        )
        var runCount = 0
        runProcessorTest(sources = listOf(source)) {
            assertThat(
                it.processingEnv.findTypeElement("foo.bar.MyClass")
            ).isNotNull()
            runCount++
        }
        // run 3 times: javac, kapt, ksp (if enabled)
        assertThat(
            runCount
        ).isEqualTo(
            2 + if (CompilationTestCapabilities.canTestWithKsp) {
                1
            } else {
                0
            }
        )
    }
}
