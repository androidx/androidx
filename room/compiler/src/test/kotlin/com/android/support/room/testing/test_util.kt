import com.android.support.room.Query
import com.android.support.room.testing.TestInvocation
import com.android.support.room.testing.TestProcessor
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert

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
fun simpleRun(f: (TestInvocation) -> Unit): CompileTester {
    return Truth.assertAbout(JavaSourceSubjectFactory.javaSource())
            .that(JavaFileObjects.forSourceString("foo.bar.MyClass",
                    """
                    package foo.bar;
                    abstract public class MyClass {
                    @com.android.support.room.Query("foo")
                    abstract public void setFoo(String foo);
                    }
                    """))
            .processedWith(TestProcessor.builder()
                    .nextRunHandler {
                        f(it)
                        true
                    }
                    .forAnnotations(Query::class)
                    .build())
}
