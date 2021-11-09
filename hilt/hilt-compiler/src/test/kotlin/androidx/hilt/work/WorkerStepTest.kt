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

package androidx.hilt.work

import androidx.hilt.Sources
import androidx.hilt.compiler
import androidx.hilt.toJFO
import com.google.testing.compile.CompilationSubject.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WorkerStepTest {

    @Test
    fun verifyEnclosingElementExtendsWorker() {
        val myWorker = """
        package androidx.hilt.work.test;

        import android.content.Context;
        import androidx.hilt.work.HiltWorker;
        import androidx.work.WorkerParameters;
        import dagger.assisted.Assisted;
        import dagger.assisted.AssistedInject;

        @HiltWorker
        class MyWorker {
            @AssistedInject
            MyWorker(@Assisted Context context, @Assisted WorkerParameters params) { }
        }
        """.toJFO("androidx.hilt.work.work.MyWorker")

        val compilation = compiler()
            .compile(myWorker, Sources.LISTENABLE_WORKER, Sources.WORKER, Sources.WORKER_PARAMETERS)
        assertThat(compilation).apply {
            failed()
            hadErrorCount(1)
            hadErrorContainingMatch(
                "@HiltWorker is only supported on types that subclass " +
                    "androidx.work.ListenableWorker."
            )
        }
    }

    @Test
    fun verifySingleAnnotatedConstructor() {
        val myWorker = """
        package androidx.hilt.work.test;

        import android.content.Context;
        import androidx.hilt.work.HiltWorker;
        import androidx.work.Worker;
        import androidx.work.WorkerParameters;
        import dagger.assisted.Assisted;
        import dagger.assisted.AssistedInject;
        import java.lang.String;

        @HiltWorker
        class MyWorker extends Worker {
            @AssistedInject
            MyWorker(@Assisted Context context, @Assisted WorkerParameters params) {
                super(context, params);
            }

            @AssistedInject
            MyWorker(Context context, WorkerParameters params, String s) {
                super(context, params);
            }
        }
        """.toJFO("androidx.hilt.work.test.MyWorker")

        val compilation = compiler()
            .compile(myWorker, Sources.LISTENABLE_WORKER, Sources.WORKER, Sources.WORKER_PARAMETERS)
        assertThat(compilation).apply {
            failed()
            hadErrorCount(1)
            hadErrorContainingMatch(
                "@HiltWorker annotated class should contain exactly one @AssistedInject " +
                    "annotated constructor."
            )
        }
    }

    @Test
    fun verifyNonPrivateConstructor() {
        val myWorker = """
        package androidx.hilt.work.test;

        import android.content.Context;
        import androidx.hilt.work.HiltWorker;
        import androidx.work.Worker;
        import androidx.work.WorkerParameters;
        import dagger.assisted.Assisted;
        import dagger.assisted.AssistedInject;

        @HiltWorker
        class MyWorker extends Worker {
            @AssistedInject
            private MyWorker(@Assisted Context context, @Assisted WorkerParameters params) {
                super(context, params);
            }
        }
        """.toJFO("androidx.hilt.work.test.MyWorker")

        val compilation = compiler()
            .compile(myWorker, Sources.LISTENABLE_WORKER, Sources.WORKER, Sources.WORKER_PARAMETERS)
        assertThat(compilation).apply {
            failed()
            hadErrorCount(1)
            hadErrorContainingMatch(
                "@AssistedInject annotated constructors must not be private."
            )
        }
    }

    @Test
    fun verifyInnerClassIsStatic() {
        val myWorker = """
        package androidx.hilt.work.test;

        import android.content.Context;
        import androidx.hilt.work.HiltWorker;
        import androidx.work.Worker;
        import androidx.work.WorkerParameters;
        import dagger.assisted.Assisted;
        import dagger.assisted.AssistedInject;

        class Outer {
            @HiltWorker
            class MyWorker extends Worker {
                @AssistedInject
                MyWorker(@Assisted Context context, @Assisted WorkerParameters params) {
                    super(context, params);
                }
            }
        }
        """.toJFO("androidx.hilt.work.test.Outer")

        val compilation = compiler()
            .compile(myWorker, Sources.LISTENABLE_WORKER, Sources.WORKER, Sources.WORKER_PARAMETERS)
        assertThat(compilation).apply {
            failed()
            hadErrorCount(1)
            hadErrorContainingMatch(
                "@HiltWorker may only be used on inner classes " +
                    "if they are static."
            )
        }
    }

    @Test
    fun verifyConstructorAnnotation() {
        val myWorker = """
        package androidx.hilt.work.test;

        import android.content.Context;
        import androidx.hilt.work.HiltWorker;
        import androidx.work.Worker;
        import androidx.work.WorkerParameters;
        import dagger.assisted.Assisted;
        import dagger.assisted.AssistedInject;
        import java.lang.String;
        import javax.inject.Inject;

        @HiltWorker
        class MyWorker extends Worker {
            @Inject
            MyWorker(@Assisted Context context, @Assisted WorkerParameters params) {
                super(context, params);
            }
        }
        """.toJFO("androidx.hilt.work.test.MyWorker")

        val compilation = compiler()
            .compile(myWorker, Sources.LISTENABLE_WORKER, Sources.WORKER, Sources.WORKER_PARAMETERS)
        assertThat(compilation).apply {
            failed()
            hadErrorContainingMatch(
                "Worker constructor should be annotated with @AssistedInject instead of @Inject."
            )
        }
    }

    @Test
    fun verifyAssistedParamOrder() {
        val myWorker = """
        package androidx.hilt.work.test;

        import android.content.Context;
        import androidx.hilt.work.HiltWorker;
        import androidx.work.Worker;
        import androidx.work.WorkerParameters;
        import dagger.assisted.Assisted;
        import dagger.assisted.AssistedInject;
        import java.lang.String;

        @HiltWorker
        class MyWorker extends Worker {
            @AssistedInject
            MyWorker(@Assisted WorkerParameters params, @Assisted Context context) {
                super(context, params);
            }
        }
        """.toJFO("androidx.hilt.work.test.MyWorker")

        val compilation = compiler()
            .compile(myWorker, Sources.LISTENABLE_WORKER, Sources.WORKER, Sources.WORKER_PARAMETERS)
        assertThat(compilation).apply {
            failed()
            hadErrorContainingMatch(
                "The 'Context' parameter must be declared before the 'WorkerParameters' in the " +
                    "@AssistedInject constructor of a @HiltWorker annotated class.",
            )
        }
    }
}