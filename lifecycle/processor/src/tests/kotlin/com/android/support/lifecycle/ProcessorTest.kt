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
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory.javaSource
import com.google.testing.compile.JavaSourcesSubjectFactory.javaSources
import java.io.File
import java.nio.charset.Charset

@RunWith(JUnit4::class)
class ProcessorTest {
    @Test
    fun testTest() {
        val code = File("src/tests/test-data/Bar.java").readText(Charset.defaultCharset())
        assertAbout(javaSource())
                .that(JavaFileObjects.forSourceString("foo.Bar", code))
                .processedWith(LifecycleProcessor())
            .compilesWithoutError()
    }
}