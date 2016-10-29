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
 *
 */

package com.android.support.lifecycle

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import com.google.common.truth.Truth.assertAbout
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory.javaSource
import java.io.File
import java.nio.charset.Charset
import javax.tools.JavaFileObject

@RunWith(JUnit4::class)
class ProcessorTest {

    fun load(className: String, folder: String = ""): JavaFileObject {
        val folderPath = "src/tests/test-data/${if (folder.isEmpty()) "" else folder + "/" }"
        val code = File("$folderPath/$className.java").readText(Charset.defaultCharset())
        return JavaFileObjects.forSourceString("foo.$className", code);
    }

    fun processClass(className: String): CompileTester {
        val processedWith = assertAbout(javaSource())
                .that(load(className))
                .processedWith(LifecycleProcessor())
        return checkNotNull(processedWith)
    }

    @Test
    fun testTest() {
        processClass("Bar").compilesWithoutError()
    }

    @Test
    fun testTooManyArguments() {
        processClass("TooManyArgs").failsToCompile()?.withErrorContaining(
                LifecycleProcessor.TOO_MANY_ARGS_ERROR_MSG)
    }

    @Test
    fun testInvalidFirstArg() {
        processClass("InvalidFirstArg").failsToCompile()?.withErrorContaining(
                LifecycleProcessor.INVALID_FIRST_ARGUMENT)
    }

    @Test
    fun testInvalidSecondArg() {
        processClass("InvalidSecondArg").failsToCompile()?.withErrorContaining(
                LifecycleProcessor.INVALID_SECOND_ARGUMENT)
    }

    fun testInheritance() {
        processClass("InheritanceOk").compilesWithoutError()
    }

                @Test
    fun testInheritance2() {
        processClass("InheritanceOk2").compilesWithoutError().and().generatesSources(
                load("InheritanceOk2Base_LifecycleAdapter", "expected"),
                load("InheritanceOk2Derived_LifecycleAdapter", "expected")
        )
    }
}