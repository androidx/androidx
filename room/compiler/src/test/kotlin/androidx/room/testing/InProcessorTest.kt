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

import androidx.room.Query
import com.google.common.truth.Truth
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(JUnit4::class)
class InProcessorTest {
    @Test
    fun testInProcessorTestRuns() {
        val didRun = AtomicBoolean(false)
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource())
                .that(JavaFileObjects.forSourceString("foo.bar.MyClass",
                        """
                        package foo.bar;
                        abstract public class MyClass {
                        @androidx.room.Query("foo")
                        abstract public void setFoo(String foo);
                        }
                        """))
                .processedWith(TestProcessor.builder()
                        .nextRunHandler { invocation ->
                            didRun.set(true)
                            assertThat(invocation.annotations.size, `is`(1))
                            true
                        }
                        .forAnnotations(Query::class)
                        .build())
                .compilesWithoutError()
        assertThat(didRun.get(), `is`(true))
    }
}
