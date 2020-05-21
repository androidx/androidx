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
    fun verifyAssistedFactory_noArg() {
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

        import androidx.annotation.NonNull;
        import androidx.hilt.lifecycle.ViewModelAssistedFactory;
        import androidx.lifecycle.SavedStateHandle;
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
            public MyViewModel create(@NonNull SavedStateHandle arg0) {
                return new MyViewModel();
            }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel_AssistedFactory")

        val compilation = compiler()
            .compile(myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).apply {
            succeeded()
            generatedSourceFile("androidx.hilt.lifecycle.test.MyViewModel_AssistedFactory")
                .hasSourceEquivalentTo(expected)
        }
    }

    @Test
    fun verifyAssistedFactory_savedStateOnlyArg() {
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

        import androidx.annotation.NonNull;
        import androidx.hilt.lifecycle.ViewModelAssistedFactory;
        import androidx.lifecycle.SavedStateHandle;
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
            public MyViewModel create(@NonNull SavedStateHandle arg0) {
                return new MyViewModel(arg0);
            }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel_AssistedFactory")

        val compilation = compiler()
            .compile(myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).apply {
            succeeded()
            generatedSourceFile("androidx.hilt.lifecycle.test.MyViewModel_AssistedFactory")
                .hasSourceEquivalentTo(expected)
        }
    }

    @Test
    fun verifyAssistedFactory_mixedArgs() {
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

        import androidx.annotation.NonNull;
        import androidx.hilt.lifecycle.ViewModelAssistedFactory;
        import androidx.lifecycle.SavedStateHandle;
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
            public MyViewModel create(@NonNull SavedStateHandle arg0) {
                return new MyViewModel(s.get(), f.get(), arg0, l.get());
            }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel_AssistedFactory")

        val compilation = compiler()
            .compile(foo, myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).apply {
            succeeded()
            generatedSourceFile("androidx.hilt.lifecycle.test.MyViewModel_AssistedFactory")
                .hasSourceEquivalentTo(expected)
        }
    }

    @Test
    fun verifyAssistedFactory_mixedAndProviderArgs() {
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

        import androidx.annotation.NonNull;
        import androidx.hilt.lifecycle.ViewModelAssistedFactory;
        import androidx.lifecycle.SavedStateHandle;
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
            public MyViewModel create(@NonNull SavedStateHandle arg0) {
                return new MyViewModel(s.get(), f, arg0);
            }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel_AssistedFactory")

        val compilation = compiler()
            .compile(foo, myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).apply {
            succeeded()
            generatedSourceFile("androidx.hilt.lifecycle.test.MyViewModel_AssistedFactory")
                .hasSourceEquivalentTo(expected)
        }
    }

    @Test
    fun verifyAssistedFactory_qualifiedArgs() {
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

        import androidx.annotation.NonNull;
        import androidx.hilt.lifecycle.ViewModelAssistedFactory;
        import androidx.lifecycle.SavedStateHandle;
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
            public MyViewModel create(@NonNull SavedStateHandle arg0) {
                return new MyViewModel(s.get(), l, arg0);
            }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel_AssistedFactory")

        val compilation = compiler()
            .compile(myQualifier, myViewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).apply {
            succeeded()
            generatedSourceFile("androidx.hilt.lifecycle.test.MyViewModel_AssistedFactory")
                .hasSourceEquivalentTo(expected)
        }
    }

    @Test
    fun verifyMultibindModule() {
        val myViewModel = """
        package androidx.hilt.lifecycle.test;

        import androidx.lifecycle.ViewModel;
        import androidx.hilt.lifecycle.ViewModelInject;

        class MyViewModel extends ViewModel {
            @ViewModelInject
            MyViewModel() { }
        }
        """.toJFO("androidx.hilt.lifecycle.test.MyViewModel")

        val expected = """
        package androidx.hilt.lifecycle.test;

        import androidx.hilt.lifecycle.ViewModelAssistedFactory;
        import androidx.lifecycle.ViewModel;
        import dagger.Binds;
        import dagger.Module;
        import dagger.hilt.InstallIn;
        import dagger.hilt.android.components.ActivityRetainedComponent;
        import dagger.hilt.codegen.OriginatingElement;
        import dagger.multibindings.IntoMap;
        import dagger.multibindings.StringKey;
        import $GENERATED_TYPE;

        $GENERATED_ANNOTATION
        @Module
        @InstallIn(ActivityRetainedComponent.class)
        @OriginatingElement(topLevelClass = MyViewModel.class)
        public interface MyViewModel_HiltModule {
            @Binds
            @IntoMap
            @StringKey("androidx.hilt.lifecycle.test.MyViewModel")
            ViewModelAssistedFactory<? extends ViewModel> bind(MyViewModel_AssistedFactory factory)
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

        val expectedFactory = """
        package androidx.hilt.lifecycle.test;

        import androidx.annotation.NonNull;
        import androidx.hilt.lifecycle.ViewModelAssistedFactory;
        import androidx.lifecycle.SavedStateHandle;
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
            public Outer.InnerViewModel create(@NonNull SavedStateHandle arg0) {
                return new Outer.InnerViewModel();
            }
        }
        """.toJFO("androidx.hilt.lifecycle.test.Outer_InnerViewModel_AssistedFactory")

        val expectedModule = """
        package androidx.hilt.lifecycle.test;

        import androidx.hilt.lifecycle.ViewModelAssistedFactory;
        import androidx.lifecycle.ViewModel;
        import dagger.Binds;
        import dagger.Module;
        import dagger.hilt.InstallIn;
        import dagger.hilt.android.components.ActivityRetainedComponent;
        import dagger.hilt.codegen.OriginatingElement;
        import dagger.multibindings.IntoMap;
        import dagger.multibindings.StringKey;
        import $GENERATED_TYPE;

        $GENERATED_ANNOTATION
        @Module
        @InstallIn(ActivityRetainedComponent.class)
        @OriginatingElement(topLevelClass = Outer.class)
        public interface Outer_InnerViewModel_HiltModule {
            @Binds
            @IntoMap
            @StringKey("androidx.hilt.lifecycle.test.Outer.InnerViewModel")
            ViewModelAssistedFactory<? extends ViewModel> bind(
                    Outer_InnerViewModel_AssistedFactory factory)
        }
        """.toJFO("androidx.hilt.lifecycle.test.Outer_InnerViewModel_HiltModule")

        val compilation = compiler()
            .compile(viewModel, Sources.VIEW_MODEL, Sources.SAVED_STATE_HANDLE)
        assertThat(compilation).apply {
            succeeded()
            generatedSourceFile("androidx.hilt.lifecycle.test" +
                        ".Outer_InnerViewModel_AssistedFactory")
                .hasSourceEquivalentTo(expectedFactory)
            generatedSourceFile("androidx.hilt.lifecycle.test" +
                        ".Outer_InnerViewModel_HiltModule")
                .hasSourceEquivalentTo(expectedModule)
        }
    }
}