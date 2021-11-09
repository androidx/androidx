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

import androidx.hilt.GENERATED_ANNOTATION
import androidx.hilt.GENERATED_TYPE
import androidx.hilt.Sources
import androidx.hilt.compiler
import androidx.hilt.toJFO
import com.google.testing.compile.CompilationSubject.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WorkerGeneratorTest {

    @Test
    fun verifyAssistedFactory_mixedArgs() {
        val foo = """
        package androidx.hilt.work.test;

        public class Foo { }
        """.toJFO("androidx.hilt.work.test.Foo")

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
            MyWorker(@Assisted Context context, @Assisted WorkerParameters params, String s,
                    Foo f, long l) {
                super(context, params);
            }
        }
        """.toJFO("androidx.hilt.work.test.MyWorker")

        val expected = """
        package androidx.hilt.work.test;

        import androidx.hilt.work.WorkerAssistedFactory;
        import dagger.assisted.AssistedFactory;
        import $GENERATED_TYPE;

        $GENERATED_ANNOTATION
        @AssistedFactory
        public interface MyWorker_AssistedFactory extends WorkerAssistedFactory<MyWorker> {
        }
        """.toJFO("androidx.hilt.work.test.MyWorker_AssistedFactory")

        val compilation = compiler()
            .compile(
                foo, myWorker, Sources.LISTENABLE_WORKER, Sources.WORKER,
                Sources.WORKER_PARAMETERS
            )
        assertThat(compilation).apply {
            succeeded()
            generatedSourceFile("androidx.hilt.work.test.MyWorker_AssistedFactory")
                .hasSourceEquivalentTo(expected)
        }
    }

    @Test
    fun verifyMultibindModule() {
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
            MyWorker(@Assisted Context context, @Assisted WorkerParameters params) {
                super(context, params);
            }
        }
        """.toJFO("androidx.hilt.work.test.MyWorker")

        val expected = """
        package androidx.hilt.work.test;

        import androidx.hilt.work.WorkerAssistedFactory;
        import androidx.work.ListenableWorker;
        import dagger.Binds;
        import dagger.Module;
        import dagger.hilt.InstallIn;
        import dagger.hilt.codegen.OriginatingElement;
        import dagger.hilt.components.SingletonComponent;
        import dagger.multibindings.IntoMap;
        import dagger.multibindings.StringKey;
        import $GENERATED_TYPE;

        $GENERATED_ANNOTATION
        @Module
        @InstallIn(SingletonComponent.class)
        @OriginatingElement(topLevelClass = MyWorker.class)
        public interface MyWorker_HiltModule {
            @Binds
            @IntoMap
            @StringKey("androidx.hilt.work.test.MyWorker")
            WorkerAssistedFactory<? extends ListenableWorker> bind(MyWorker_AssistedFactory factory)
        }
        """.toJFO("androidx.hilt.work.test.MyWorker_HiltModule")

        val compilation = compiler()
            .compile(myWorker, Sources.LISTENABLE_WORKER, Sources.WORKER, Sources.WORKER_PARAMETERS)
        assertThat(compilation).apply {
            succeeded()
            generatedSourceFile("androidx.hilt.work.test.MyWorker_HiltModule")
                .hasSourceEquivalentTo(expected)
        }
    }
}
