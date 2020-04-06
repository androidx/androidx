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

package androidx.hilt

import com.google.testing.compile.CompilationSubject.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class HiltViewModelProcessorTest {

    @Test
    fun validViewModel() {
        val myViewModel = """
        package androidx.hilt.lifecycle.test;

        import androidx.lifecycle.ViewModel;
        import androidx.hilt.lifecycle.ViewModelInject;

        class MyViewModel extends ViewModel {
            @ViewModelInject
            MyViewModel() { }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel")

        val compilation = compiler()
            .compile(myViewModel,
                Sources.VIEW_MODEL,
                Sources.SAVED_STATE_HANDLE
            )
        assertThat(compilation).succeeded()
    }

    @Test
    fun verifyEnclosingElementExtendsViewModel() {
        val myViewModel = """
        package androidx.hilt.lifecycle.test;

        import androidx.hilt.lifecycle.ViewModelInject;

        class MyViewModel {
            @ViewModelInject
            MyViewModel() { }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel")

        val compilation = compiler()
            .compile(myViewModel, Sources.VIEW_MODEL)
        assertThat(compilation).failed()
        assertThat(compilation).hadErrorCount(1)
        assertThat(compilation)
            .hadErrorContainingMatch("@ViewModelInject is only supported on types that subclass " +
                    "androidx.lifecycle.ViewModel.")
    }

    @Test
    fun verifySingleAnnotatedConstructor() {
        val myViewModel = """
        package androidx.hilt.lifecycle.test;

        import androidx.lifecycle.ViewModel;
        import androidx.hilt.lifecycle.ViewModelInject;

        class MyViewModel extends ViewModel {
            @ViewModelInject
            MyViewModel() { }

            @ViewModelInject
            MyViewModel(String s) { }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel")

        val compilation = compiler()
            .compile(myViewModel, Sources.VIEW_MODEL)
        assertThat(compilation).failed()
        assertThat(compilation).hadErrorCount(1)
        assertThat(compilation)
            .hadErrorContainingMatch("Multiple @ViewModelInject annotated constructors found.")
    }

    @Test
    fun verifyNonPrivateConstructor() {
        val myViewModel = """
        package androidx.hilt.lifecycle.test;

        import androidx.lifecycle.ViewModel;
        import androidx.hilt.lifecycle.ViewModelInject;

        class MyViewModel extends ViewModel {
            @ViewModelInject
            private MyViewModel() { }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel")

        val compilation = compiler()
            .compile(myViewModel, Sources.VIEW_MODEL)
        assertThat(compilation).failed()
        assertThat(compilation).hadErrorCount(1)
        assertThat(compilation)
            .hadErrorContainingMatch("@ViewModelInject annotated constructors must not be " +
                    "private.")
    }

    @Test
    fun verifyInnerClassIsStatic() {
        val myViewModel = """
        package androidx.hilt.lifecycle.test;

        import androidx.lifecycle.ViewModel;
        import androidx.hilt.lifecycle.ViewModelInject;

        class Outer {
            class MyViewModel extends ViewModel {
                @ViewModelInject
                MyViewModel() { }
            }
        }
        """.toJFO("androidx.hilt.lifecycle.test.Outer")

        val compilation = compiler()
            .compile(myViewModel, Sources.VIEW_MODEL)
        assertThat(compilation).failed()
        assertThat(compilation).hadErrorCount(1)
        assertThat(compilation)
            .hadErrorContainingMatch("@ViewModelInject may only be used on inner classes " +
                    "if they are static.")
    }
}