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

import androidx.hilt.ext.GENERATED_ANNOTATION
import androidx.hilt.ext.GENERATED_TYPE
import androidx.hilt.ext.Sources
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runProcessorTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WorkerGeneratorTest {

    @Test
    fun verifyAssistedFactory_mixedArgs() {
        val foo = Source.java(
            "androidx.hilt.work.test.Foo",
            """
            package androidx.hilt.work.test;

            public class Foo { }
            """
        )

        val myWorker = Source.java(
            "androidx.hilt.work.test.MyWorker",
            """
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
            """
        )

        val expected = Source.java(
            "androidx.hilt.work.test.MyWorker_AssistedFactory",
            """
            package androidx.hilt.work.test;

            import androidx.hilt.work.WorkerAssistedFactory;
            import dagger.assisted.AssistedFactory;
            import $GENERATED_TYPE;

            $GENERATED_ANNOTATION
            @AssistedFactory
            public interface MyWorker_AssistedFactory extends WorkerAssistedFactory<MyWorker> {
            }
            """
        )

        runProcessorTest(
            sources = listOf(
                foo, myWorker, Sources.LISTENABLE_WORKER, Sources.WORKER,
                Sources.WORKER_PARAMETERS
            ),
            createProcessingSteps = { listOf(WorkerStep()) }
        ) {
            it.generatedSource(expected)
        }
    }

    @Test
    fun verifyMultibindModule() {
        val myWorker = Source.java(
            "androidx.hilt.work.test.MyWorker",
            """
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
            """
        )

        val expected = Source.java(
            "androidx.hilt.work.test.MyWorker_HiltModule",
            """
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
            @OriginatingElement(
                topLevelClass = MyWorker.class
            )
            public interface MyWorker_HiltModule {
                @Binds
                @IntoMap
                @StringKey("androidx.hilt.work.test.MyWorker")
                WorkerAssistedFactory<? extends ListenableWorker> bind(MyWorker_AssistedFactory factory);
            }
            """
        )

        runProcessorTest(
            sources = listOf(
                myWorker, Sources.LISTENABLE_WORKER, Sources.WORKER, Sources.WORKER_PARAMETERS
            ),
            createProcessingSteps = { listOf(WorkerStep()) }
        ) {
            it.generatedSource(expected)
        }
    }
}
