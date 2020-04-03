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

package androidx.lifecycle.hilt

import com.google.testing.compile.CompilationSubject.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class HiltViewModelGeneratorTest {

    private val GENERATED_TYPE = try {
        Class.forName("javax.annotation.processing.Generated")
        "javax.annotation.processing.Generated"
    } catch (_: ClassNotFoundException) {
        "javax.annotation.Generated"
    }

    private val GENERATED_ANNOTATION =
        "@Generated(\"androidx.lifecycle.hilt.HiltViewModelProcessor\")"

    @Test
    fun verifyAssistedFactory_noArg() {
        val myViewModel = """
        package androidx.lifecycle.hilt.test;

        import androidx.lifecycle.ViewModel;
        import androidx.lifecycle.hilt.ViewModelInject;

        class MyViewModel extends ViewModel {
            @ViewModelInject
            MyViewModel() { }
        }
        """.toJFO("androidx.lifecycle.hilt.test.MyViewModel")

        val expected = """
        package androidx.lifecycle.hilt.test;

        import androidx.annotation.NonNull;
        import androidx.lifecycle.SavedStateHandle;
        import androidx.lifecycle.hilt.ViewModelAssistedFactory;
        import java.lang.Override;
        import $GENERATED_TYPE;
        import javax.inject.Inject;

        $GENERATED_ANNOTATION
        public final class MyViewModel_AssistedFactory implements
                ViewModelAssistedFactory<MyViewModel> {

            @Inject
            MyViewModel_AssistedFactory() { }

            @Override
            @NonNull
            public MyViewModel create(@NonNull SavedStateHandle handle) {
                return new MyViewModel();
            }
        }
        """.toJFO("androidx.lifecycle.hilt.test.MyViewModel_AssistedFactory")

        val compilation = compiler()
            .compile(myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).succeeded()
        assertThat(compilation)
            .generatedSourceFile("androidx.lifecycle.hilt.test.MyViewModel_AssistedFactory")
            .hasSourceEquivalentTo(expected)
    }

    @Test
    fun verifyAssistedFactory_savedStateOnlyArg() {
        val myViewModel = """
        package androidx.lifecycle.hilt.test;

        import androidx.lifecycle.ViewModel;
        import androidx.lifecycle.SavedStateHandle;
        import androidx.lifecycle.hilt.ViewModelInject;

        class MyViewModel extends ViewModel {
            @ViewModelInject
            MyViewModel(SavedStateHandle savedState) { }
        }
        """.toJFO("androidx.lifecycle.hilt.test.MyViewModel")

        val expected = """
        package androidx.lifecycle.hilt.test;

        import androidx.annotation.NonNull;
        import androidx.lifecycle.SavedStateHandle;
        import androidx.lifecycle.hilt.ViewModelAssistedFactory;
        import java.lang.Override;
        import $GENERATED_TYPE;
        import javax.inject.Inject;

        $GENERATED_ANNOTATION
        public final class MyViewModel_AssistedFactory implements
                ViewModelAssistedFactory<MyViewModel> {

            @Inject
            MyViewModel_AssistedFactory() { }

            @Override
            @NonNull
            public MyViewModel create(@NonNull SavedStateHandle handle) {
                return new MyViewModel(handle);
            }
        }
        """.toJFO("androidx.lifecycle.hilt.test.MyViewModel_AssistedFactory")

        val compilation = compiler()
            .compile(myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).succeeded()
        assertThat(compilation)
            .generatedSourceFile("androidx.lifecycle.hilt.test.MyViewModel_AssistedFactory")
            .hasSourceEquivalentTo(expected)
    }

    @Test
    fun verifyAssistedFactory_mixedArgs() {
        val foo = """
        package androidx.lifecycle.hilt.test;

        public class Foo { }
        """.toJFO("androidx.lifecycle.hilt.test.Foo")

        val myViewModel = """
        package androidx.lifecycle.hilt.test;

        import androidx.lifecycle.ViewModel;
        import androidx.lifecycle.SavedStateHandle;
        import androidx.lifecycle.hilt.ViewModelInject;
        import java.lang.String;

        class MyViewModel extends ViewModel {
            @ViewModelInject
            MyViewModel(String s, Foo f, SavedStateHandle savedState, long l) { }
        }
        """.toJFO("androidx.lifecycle.hilt.test.MyViewModel")

        val expected = """
        package androidx.lifecycle.hilt.test;

        import androidx.annotation.NonNull;
        import androidx.lifecycle.SavedStateHandle;
        import androidx.lifecycle.hilt.ViewModelAssistedFactory;
        import java.lang.Long;
        import java.lang.Override;
        import java.lang.String;
        import $GENERATED_TYPE;
        import javax.inject.Inject;
        import javax.inject.Provider;

        $GENERATED_ANNOTATION
        public final class MyViewModel_AssistedFactory implements
                ViewModelAssistedFactory<MyViewModel> {

            private final Provider<String> s;
            private final Provider<Foo> f;
            private final Provider<Long> l;

            @Inject
            MyViewModel_AssistedFactory(Provider<String> s, Provider<Foo> f, Provider<Long> l) {
                this.s = s;
                this.f = f;
                this.l = l;
            }

            @Override
            @NonNull
            public MyViewModel create(@NonNull SavedStateHandle handle) {
                return new MyViewModel(s.get(), f.get(), handle, l.get());
            }
        }
        """.toJFO("androidx.lifecycle.hilt.test.MyViewModel_AssistedFactory")

        val compilation = compiler()
            .compile(foo, myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).succeeded()
        assertThat(compilation)
            .generatedSourceFile("androidx.lifecycle.hilt.test.MyViewModel_AssistedFactory")
            .hasSourceEquivalentTo(expected)
    }

    @Test
    fun verifyAssistedFactory_mixedAndProviderArgs() {
        val foo = """
        package androidx.lifecycle.hilt.test;

        public class Foo { }
        """.toJFO("androidx.lifecycle.hilt.test.Foo")

        val myViewModel = """
        package androidx.lifecycle.hilt.test;

        import androidx.lifecycle.ViewModel;
        import androidx.lifecycle.SavedStateHandle;
        import androidx.lifecycle.hilt.ViewModelInject;
        import java.lang.String;
        import javax.inject.Provider;

        class MyViewModel extends ViewModel {
            @ViewModelInject
            MyViewModel(String s, Provider<Foo> f, SavedStateHandle savedState) { }
        }
        """.toJFO("androidx.lifecycle.hilt.test.MyViewModel")

        val expected = """
        package androidx.lifecycle.hilt.test;

        import androidx.annotation.NonNull;
        import androidx.lifecycle.SavedStateHandle;
        import androidx.lifecycle.hilt.ViewModelAssistedFactory;
        import java.lang.Override;
        import java.lang.String;
        import $GENERATED_TYPE;
        import javax.inject.Inject;
        import javax.inject.Provider;

        $GENERATED_ANNOTATION
        public final class MyViewModel_AssistedFactory implements
                ViewModelAssistedFactory<MyViewModel> {

            private final Provider<String> s;
            private final Provider<Foo> f;

            @Inject
            MyViewModel_AssistedFactory(Provider<String> s, Provider<Foo> f) {
                this.s = s;
                this.f = f;
            }

            @Override
            @NonNull
            public MyViewModel create(@NonNull SavedStateHandle handle) {
                return new MyViewModel(s.get(), f, handle);
            }
        }
        """.toJFO("androidx.lifecycle.hilt.test.MyViewModel_AssistedFactory")

        val compilation = compiler()
            .compile(foo, myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).succeeded()
        assertThat(compilation)
            .generatedSourceFile("androidx.lifecycle.hilt.test.MyViewModel_AssistedFactory")
            .hasSourceEquivalentTo(expected)
    }

    @Test
    fun verifyAssistedFactory_qualifiedArgs() {
        val myQualifier = """
        package androidx.lifecycle.hilt.test;

        import javax.inject.Qualifier;

        @Qualifier
        public @interface MyQualifier { }
        """.toJFO("androidx.lifecycle.hilt.test.MyQualifier")

        val myViewModel = """
        package androidx.lifecycle.hilt.test;

        import androidx.lifecycle.ViewModel;
        import androidx.lifecycle.SavedStateHandle;
        import androidx.lifecycle.hilt.ViewModelInject;
        import java.lang.Long;
        import java.lang.String;
        import javax.inject.Named;
        import javax.inject.Provider;

        class MyViewModel extends ViewModel {
            @ViewModelInject
            MyViewModel(@Named("TheString") String s, @MyQualifier Provider<Long> l,
                    SavedStateHandle savedState) {
            }
        }
        """.toJFO("androidx.lifecycle.hilt.test.MyViewModel")

        val expected = """
        package androidx.lifecycle.hilt.test;

        import androidx.annotation.NonNull;
        import androidx.lifecycle.SavedStateHandle;
        import androidx.lifecycle.hilt.ViewModelAssistedFactory;
        import java.lang.Long;
        import java.lang.Override;
        import java.lang.String;
        import $GENERATED_TYPE;
        import javax.inject.Inject;
        import javax.inject.Named;
        import javax.inject.Provider;

        $GENERATED_ANNOTATION
        public final class MyViewModel_AssistedFactory implements
                ViewModelAssistedFactory<MyViewModel> {

            private final Provider<String> s;
            private final Provider<Long> l;

            @Inject
            MyViewModel_AssistedFactory(@Named("TheString") Provider<String> s,
                    @MyQualifier Provider<Long> l) {
                this.s = s;
                this.l = l;
            }

            @Override
            @NonNull
            public MyViewModel create(@NonNull SavedStateHandle handle) {
                return new MyViewModel(s.get(), l, handle);
            }
        }
        """.toJFO("androidx.lifecycle.hilt.test.MyViewModel_AssistedFactory")

        val compilation = compiler()
            .compile(myQualifier, myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).succeeded()
        assertThat(compilation)
            .generatedSourceFile("androidx.lifecycle.hilt.test.MyViewModel_AssistedFactory")
            .hasSourceEquivalentTo(expected)
    }

    @Test
    fun verifyAssistedFactory_multipleSavedStateArg() {
        val myViewModel = """
        package androidx.lifecycle.hilt.test;

        import androidx.lifecycle.ViewModel;
        import androidx.lifecycle.SavedStateHandle;
        import androidx.lifecycle.hilt.ViewModelInject;
        import java.lang.String;

        class MyViewModel extends ViewModel {
            @ViewModelInject
            MyViewModel(SavedStateHandle savedState, String s, SavedStateHandle savedState2) { }
        }
        """.toJFO("androidx.lifecycle.hilt.test.MyViewModel")

        val expected = """
        package androidx.lifecycle.hilt.test;

        import androidx.annotation.NonNull;
        import androidx.lifecycle.SavedStateHandle;
        import androidx.lifecycle.hilt.ViewModelAssistedFactory;
        import java.lang.Override;
        import java.lang.String;
        import $GENERATED_TYPE;
        import javax.inject.Inject;
        import javax.inject.Provider;

        $GENERATED_ANNOTATION
        public final class MyViewModel_AssistedFactory implements
                ViewModelAssistedFactory<MyViewModel> {

            private final Provider<String> s;

            @Inject
            MyViewModel_AssistedFactory(Provider<String> s) {
                this.s = s;
            }

            @Override
            @NonNull
            public MyViewModel create(@NonNull SavedStateHandle handle) {
                return new MyViewModel(handle, s.get(), handle);
            }
        }
        """.toJFO("androidx.lifecycle.hilt.test.MyViewModel_AssistedFactory")

        val compilation = compiler()
            .compile(myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).succeeded()
        assertThat(compilation)
            .generatedSourceFile("androidx.lifecycle.hilt.test.MyViewModel_AssistedFactory")
            .hasSourceEquivalentTo(expected)
    }

    @Test
    fun verifyMultibindModule() {
        val myViewModel = """
        package androidx.lifecycle.hilt.test;

        import androidx.lifecycle.ViewModel;
        import androidx.lifecycle.hilt.ViewModelInject;

        class MyViewModel extends ViewModel {
            @ViewModelInject
            MyViewModel() { }
        }
        """.toJFO("androidx.lifecycle.hilt.test.MyViewModel")

        val expected = """
        package androidx.lifecycle.hilt.test;

        import androidx.lifecycle.ViewModel;
        import androidx.lifecycle.hilt.ViewModelAssistedFactory;
        import androidx.lifecycle.hilt.ViewModelKey;
        import dagger.Binds;
        import dagger.Module;
        import dagger.hilt.InstallIn;
        import dagger.hilt.android.components.ActivityRetainedComponent;
        import dagger.multibindings.IntoMap;
        import $GENERATED_TYPE;

        $GENERATED_ANNOTATION
        @Module
        @InstallIn(ActivityRetainedComponent.class)
        public interface MyViewModel_HiltModule {
            @Binds
            @IntoMap
            @ViewModelKey(MyViewModel.class)
            ViewModelAssistedFactory<? extends ViewModel> bind(MyViewModel_AssistedFactory factory)
        }
        """.toJFO("androidx.lifecycle.hilt.test.MyViewModel_HiltModule")

        val compilation = compiler()
            .compile(myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).succeeded()
        assertThat(compilation)
            .generatedSourceFile("androidx.lifecycle.hilt.test.MyViewModel_HiltModule")
            .hasSourceEquivalentTo(expected)
    }

    @Test
    fun verifyInnerClass() {
        val viewModel = """
        package androidx.lifecycle.hilt.test;

        import androidx.lifecycle.ViewModel;
        import androidx.lifecycle.hilt.ViewModelInject;

        class Outer {
            static class InnerViewModel extends ViewModel {
                @ViewModelInject
                InnerViewModel() { }
            }
        }
        """.toJFO("androidx.lifecycle.hilt.test.Outer")

        val expectedFactory = """
        package androidx.lifecycle.hilt.test;

        import androidx.annotation.NonNull;
        import androidx.lifecycle.SavedStateHandle;
        import androidx.lifecycle.hilt.ViewModelAssistedFactory;
        import java.lang.Override;
        import $GENERATED_TYPE;
        import javax.inject.Inject;

        $GENERATED_ANNOTATION
        public final class Outer_InnerViewModel_AssistedFactory implements
                ViewModelAssistedFactory<Outer.InnerViewModel> {

            @Inject
            Outer_InnerViewModel_AssistedFactory() { }

            @Override
            @NonNull
            public Outer.InnerViewModel create(@NonNull SavedStateHandle handle) {
                return new Outer.InnerViewModel();
            }
        }
        """.toJFO("androidx.lifecycle.hilt.test.Outer_InnerViewModel_AssistedFactory")

        val expectedModule = """
        package androidx.lifecycle.hilt.test;

        import androidx.lifecycle.ViewModel;
        import androidx.lifecycle.hilt.ViewModelAssistedFactory;
        import androidx.lifecycle.hilt.ViewModelKey;
        import dagger.Binds;
        import dagger.Module;
        import dagger.hilt.InstallIn;
        import dagger.hilt.android.components.ActivityRetainedComponent;
        import dagger.multibindings.IntoMap;
        import $GENERATED_TYPE;

        $GENERATED_ANNOTATION
        @Module
        @InstallIn(ActivityRetainedComponent.class)
        public interface Outer_InnerViewModel_HiltModule {
            @Binds
            @IntoMap
            @ViewModelKey(Outer.InnerViewModel.class)
            ViewModelAssistedFactory<? extends ViewModel> bind(
                    Outer_InnerViewModel_AssistedFactory factory)
        }
        """.toJFO("androidx.lifecycle.hilt.test.Outer_InnerViewModel_HiltModule")

        val compilation = compiler()
            .compile(viewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).succeeded()
        assertThat(compilation)
            .generatedSourceFile("androidx.lifecycle.hilt.test" +
                    ".Outer_InnerViewModel_AssistedFactory")
            .hasSourceEquivalentTo(expectedFactory)
        assertThat(compilation)
            .generatedSourceFile("androidx.lifecycle.hilt.test" +
                    ".Outer_InnerViewModel_HiltModule")
            .hasSourceEquivalentTo(expectedModule)
    }
}