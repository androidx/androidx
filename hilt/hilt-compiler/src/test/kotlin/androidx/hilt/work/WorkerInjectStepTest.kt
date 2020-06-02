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
class WorkerInjectStepTest {

    @Test
    fun verifyEnclosingElementExtendsWorker() {
        val myWorker = """
        package androidx.hilt.work.test;

        import android.content.Context;
        import androidx.hilt.Assisted;
        import androidx.hilt.work.WorkerInject;
        import androidx.work.WorkerParameters;

        class MyWorker {
            @WorkerInject
            MyWorker(@Assisted Context context, @Assisted WorkerParameters params) { }
        }
        """.toJFO("androidx.hilt.work.work.MyWorker")

        val compilation = compiler()
            .compile(myWorker, Sources.LISTENABLE_WORKER, Sources.WORKER, Sources.WORKER_PARAMETERS)
        assertThat(compilation).apply {
            failed()
            hadErrorCount(1)
            hadErrorContainingMatch("@WorkerInject is only supported on types that subclass " +
                        "androidx.work.ListenableWorker.")
        }
    }

    @Test
    fun verifySingleAnnotatedConstructor() {
        val myWorker = """
        package androidx.hilt.work.test;

        import android.content.Context;
        import androidx.hilt.Assisted;
        import androidx.hilt.work.WorkerInject;
        import androidx.work.Worker;
        import androidx.work.WorkerParameters;
        import java.lang.String;

        class MyWorker extends Worker {
            @WorkerInject
            MyWorker(@Assisted Context context, @Assisted WorkerParameters params) {
                super(context, params);
            }

            @WorkerInject
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
            hadErrorContainingMatch("Multiple @WorkerInject annotated constructors found.")
        }
    }

    @Test
    fun verifyNonPrivateConstructor() {
        val myWorker = """
        package androidx.hilt.work.test;

        import android.content.Context;
        import androidx.hilt.Assisted;
        import androidx.hilt.work.WorkerInject;
        import androidx.work.Worker;
        import androidx.work.WorkerParameters;

        class MyWorker extends Worker {
            @WorkerInject
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
            hadErrorContainingMatch("@WorkerInject annotated constructors must not be " +
                        "private.")
        }
    }

    @Test
    fun verifyInnerClassIsStatic() {
        val myWorker = """
        package androidx.hilt.work.test;

        import android.content.Context;
        import androidx.hilt.Assisted;
        import androidx.hilt.work.WorkerInject;
        import androidx.work.Worker;
        import androidx.work.WorkerParameters;

        class Outer {
            class MyWorker extends Worker {
                @WorkerInject
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
            hadErrorContainingMatch("@WorkerInject may only be used on inner classes " +
                        "if they are static.")
        }
    }
}