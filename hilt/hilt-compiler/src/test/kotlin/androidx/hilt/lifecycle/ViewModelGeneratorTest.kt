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

package androidx.hilt.lifecycle

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
class ViewModelGeneratorTest {

    @Test
    fun verifyModule_noArg() {
        val myViewModel = """
        package androidx.hilt.lifecycle.test;

        import androidx.hilt.lifecycle.ViewModelInject;
        import androidx.lifecycle.ViewModel;

        class MyViewModel extends ViewModel {
            @ViewModelInject
            MyViewModel() { }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel")

        val expected = """
        package androidx.hilt.lifecycle.test;

        import androidx.hilt.lifecycle.InternalViewModelInjectMap;
        import androidx.hilt.lifecycle.ViewModelComponent;
        import androidx.lifecycle.ViewModel;
        import dagger.Module;
        import dagger.Provides;
        import dagger.hilt.InstallIn;
        import dagger.hilt.codegen.OriginatingElement;
        import dagger.multibindings.IntoMap;
        import dagger.multibindings.StringKey;
        import $GENERATED_TYPE;

        $GENERATED_ANNOTATION
        @Module
        @InstallIn(ViewModelComponent.class)
        @OriginatingElement(
            topLevelClass = MyViewModel.class
        )
        public final class MyViewModel_HiltModule {
            private MyViewModel_HiltModule() {
            }

            @Provides
            @IntoMap
            @StringKey("androidx.hilt.lifecycle.test.MyViewModel")
            @InternalViewModelInjectMap
            public static ViewModel provide() {
              return new MyViewModel();
            }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel_HiltModule")

        val compilation = compiler()
            .compile(myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).apply {
            succeeded()
            generatedSourceFile("androidx.hilt.lifecycle.test.MyViewModel_HiltModule")
                .hasSourceEquivalentTo(expected)
        }
    }

    @Test
    fun verifyModule_savedStateOnlyArg() {
        val myViewModel = """
        package androidx.hilt.lifecycle.test;

        import androidx.hilt.Assisted;
        import androidx.hilt.lifecycle.ViewModelInject;
        import androidx.lifecycle.ViewModel;
        import androidx.lifecycle.SavedStateHandle;

        class MyViewModel extends ViewModel {
            @ViewModelInject
            MyViewModel(@Assisted SavedStateHandle savedState) { }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel")

        val expected = """
        package androidx.hilt.lifecycle.test;

        import androidx.hilt.lifecycle.InternalViewModelInjectMap;
        import androidx.hilt.lifecycle.ViewModelComponent;
        import androidx.lifecycle.SavedStateHandle;
        import androidx.lifecycle.ViewModel;
        import dagger.Module;
        import dagger.Provides;
        import dagger.hilt.InstallIn;
        import dagger.hilt.codegen.OriginatingElement;
        import dagger.multibindings.IntoMap;
        import dagger.multibindings.StringKey;
        import $GENERATED_TYPE;

        $GENERATED_ANNOTATION
        @Module
        @InstallIn(ViewModelComponent.class)
        @OriginatingElement(
            topLevelClass = MyViewModel.class
        )
        public final class MyViewModel_HiltModule {
            private MyViewModel_HiltModule() {
            }

            @Provides
            @IntoMap
            @StringKey("androidx.hilt.lifecycle.test.MyViewModel")
            @InternalViewModelInjectMap
            public static ViewModel provide(SavedStateHandle savedState) {
              return new MyViewModel(savedState);
            }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel_HiltModule")

        val compilation = compiler()
            .compile(myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).apply {
            succeeded()
            generatedSourceFile("androidx.hilt.lifecycle.test.MyViewModel_HiltModule")
                .hasSourceEquivalentTo(expected)
        }
    }

    @Test
    fun verifyModule_mixedArgs() {
        val foo = """
        package androidx.hilt.lifecycle.test;

        public class Foo { }
        """.toJFO("androidx.hilt.lifecycle.test.Foo")

        val myViewModel = """
        package androidx.hilt.lifecycle.test;

        import androidx.hilt.Assisted;
        import androidx.hilt.lifecycle.ViewModelInject;
        import androidx.lifecycle.ViewModel;
        import androidx.lifecycle.SavedStateHandle;
        import java.lang.String;

        class MyViewModel extends ViewModel {
            @ViewModelInject
            MyViewModel(String s, Foo f, @Assisted SavedStateHandle savedState, long l) { }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel")

        val expected = """
        package androidx.hilt.lifecycle.test;

        import androidx.hilt.lifecycle.InternalViewModelInjectMap;
        import androidx.hilt.lifecycle.ViewModelComponent;
        import androidx.lifecycle.SavedStateHandle;
        import androidx.lifecycle.ViewModel;
        import dagger.Module;
        import dagger.Provides;
        import dagger.hilt.InstallIn;
        import dagger.hilt.codegen.OriginatingElement;
        import dagger.multibindings.IntoMap;
        import dagger.multibindings.StringKey;
        import java.lang.String;
        import $GENERATED_TYPE;

        $GENERATED_ANNOTATION
        @Module
        @InstallIn(ViewModelComponent.class)
        @OriginatingElement(
            topLevelClass = MyViewModel.class
        )
        public final class MyViewModel_HiltModule {
            private MyViewModel_HiltModule() {
            }

            @Provides
            @IntoMap
            @StringKey("androidx.hilt.lifecycle.test.MyViewModel")
            @InternalViewModelInjectMap
            public static ViewModel provide(String s, Foo f, SavedStateHandle savedState, long l) {
              return new MyViewModel(s, f, savedState, l);
            }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel_HiltModule")

        val compilation = compiler()
            .compile(foo, myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).apply {
            succeeded()
            generatedSourceFile("androidx.hilt.lifecycle.test.MyViewModel_HiltModule")
                .hasSourceEquivalentTo(expected)
        }
    }

    @Test
    fun verifyModule_mixedAndProviderArgs() {
        val foo = """
        package androidx.hilt.lifecycle.test;

        public class Foo { }
        """.toJFO("androidx.hilt.lifecycle.test.Foo")

        val myViewModel = """
        package androidx.hilt.lifecycle.test;

        import androidx.hilt.Assisted;
        import androidx.hilt.lifecycle.ViewModelInject;
        import androidx.lifecycle.ViewModel;
        import androidx.lifecycle.SavedStateHandle;
        import java.lang.String;
        import javax.inject.Provider;

        class MyViewModel extends ViewModel {
            @ViewModelInject
            MyViewModel(String s, Provider<Foo> f, @Assisted SavedStateHandle savedState) { }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel")

        val expected = """
        package androidx.hilt.lifecycle.test;

        import androidx.hilt.lifecycle.InternalViewModelInjectMap;
        import androidx.hilt.lifecycle.ViewModelComponent;
        import androidx.lifecycle.SavedStateHandle;
        import androidx.lifecycle.ViewModel;
        import dagger.Module;
        import dagger.Provides;
        import dagger.hilt.InstallIn;
        import dagger.hilt.codegen.OriginatingElement;
        import dagger.multibindings.IntoMap;
        import dagger.multibindings.StringKey;
        import java.lang.String;
        import $GENERATED_TYPE;
        import javax.inject.Provider;

        $GENERATED_ANNOTATION
        @Module
        @InstallIn(ViewModelComponent.class)
        @OriginatingElement(
            topLevelClass = MyViewModel.class
        )
        public final class MyViewModel_HiltModule {
            private MyViewModel_HiltModule() {
            }

            @Provides
            @IntoMap
            @StringKey("androidx.hilt.lifecycle.test.MyViewModel")
            @InternalViewModelInjectMap
            public static ViewModel provide(String s, Provider<Foo> f, SavedStateHandle savedState) {
              return new MyViewModel(s, f, savedState);
            }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel_HiltModule")

        val compilation = compiler()
            .compile(foo, myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).apply {
            succeeded()
            generatedSourceFile("androidx.hilt.lifecycle.test.MyViewModel_HiltModule")
                .hasSourceEquivalentTo(expected)
        }
    }

    @Test
    fun verifyModule_qualifiedArgs() {
        val myQualifier = """
        package androidx.hilt.lifecycle.test;

        import javax.inject.Qualifier;

        @Qualifier
        public @interface MyQualifier { }
        """.toJFO("androidx.hilt.lifecycle.test.MyQualifier")

        val myViewModel = """
        package androidx.hilt.lifecycle.test;

        import androidx.hilt.Assisted;
        import androidx.hilt.lifecycle.ViewModelInject;
        import androidx.lifecycle.ViewModel;
        import androidx.lifecycle.SavedStateHandle;
        import java.lang.Long;
        import java.lang.String;
        import javax.inject.Named;
        import javax.inject.Provider;

        class MyViewModel extends ViewModel {
            @ViewModelInject
            MyViewModel(@Named("TheString") String s, @MyQualifier Provider<Long> l,
                    @Assisted SavedStateHandle savedState) {
            }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel")

        val expected = """
        package androidx.hilt.lifecycle.test;

        import androidx.hilt.lifecycle.InternalViewModelInjectMap;
        import androidx.hilt.lifecycle.ViewModelComponent;
        import androidx.lifecycle.SavedStateHandle;
        import androidx.lifecycle.ViewModel;
        import dagger.Module;
        import dagger.Provides;
        import dagger.hilt.InstallIn;
        import dagger.hilt.codegen.OriginatingElement;
        import dagger.multibindings.IntoMap;
        import dagger.multibindings.StringKey;
import java.lang.Long;
        import java.lang.String;
        import $GENERATED_TYPE;
        import javax.inject.Named;
        import javax.inject.Provider;

        $GENERATED_ANNOTATION
        @Module
        @InstallIn(ViewModelComponent.class)
        @OriginatingElement(
            topLevelClass = MyViewModel.class
        )
        public final class MyViewModel_HiltModule {
            private MyViewModel_HiltModule() {
            }

            @Provides
            @IntoMap
            @StringKey("androidx.hilt.lifecycle.test.MyViewModel")
            @InternalViewModelInjectMap
            public static ViewModel provide(@Named("TheString") String s,
                    @MyQualifier Provider<Long> l, SavedStateHandle savedState) {
              return new MyViewModel(s, l, savedState);
            }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel_HiltModule")

        val compilation = compiler()
            .compile(myQualifier, myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).apply {
            succeeded()
            generatedSourceFile("androidx.hilt.lifecycle.test.MyViewModel_HiltModule")
                .hasSourceEquivalentTo(expected)
        }
    }

    @Test
    fun verifyInnerClass() {
        val viewModel = """
        package androidx.hilt.lifecycle.test;

        import androidx.hilt.lifecycle.ViewModelInject;
        import androidx.lifecycle.ViewModel;

        class Outer {
            static class InnerViewModel extends ViewModel {
                @ViewModelInject
                InnerViewModel() { }
            }
        }
        """.toJFO("androidx.hilt.lifecycle.test.Outer")

        val expectedModule = """
        package androidx.hilt.lifecycle.test;

        import androidx.hilt.lifecycle.InternalViewModelInjectMap;
        import androidx.hilt.lifecycle.ViewModelComponent;
        import androidx.lifecycle.ViewModel;
        import dagger.Module;
        import dagger.Provides;
        import dagger.hilt.InstallIn;
        import dagger.hilt.codegen.OriginatingElement;
        import dagger.multibindings.IntoMap;
        import dagger.multibindings.StringKey;
        import $GENERATED_TYPE;

        $GENERATED_ANNOTATION
        @Module
        @InstallIn(ViewModelComponent.class)
        @OriginatingElement(topLevelClass = Outer.class)
        public final class Outer_InnerViewModel_HiltModule {
            private Outer_InnerViewModel_HiltModule() {
            }

            @Provides
            @IntoMap
            @StringKey("androidx.hilt.lifecycle.test.Outer${'$'}InnerViewModel")
            @InternalViewModelInjectMap
            public static ViewModel provide() {
              return new Outer.InnerViewModel();
            }
        }
        """.toJFO("androidx.hilt.lifecycle.test.Outer_InnerViewModel_HiltModule")

        val compilation = compiler()
            .compile(viewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).apply {
            succeeded()
            generatedSourceFile(
                "androidx.hilt.lifecycle.test" +
                    ".Outer_InnerViewModel_HiltModule"
            )
                .hasSourceEquivalentTo(expectedModule)
        }
    }
}