/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.inspection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * This interface exposes special tooling capabilities provided by JVMTI.
 */
public interface InspectorEnvironment {
    /**
     * Returns a list of all present instances of the given class in heap.
     *
     * @param clazz class whose instances should be looked up
     * @return a list of instances of {@code clazz}
     */
    @NonNull
    <T> List<T> findInstances(@NonNull Class<T> clazz);

    /**
     * A callback invoked at the entry to an instrumented method.
     *
     * {@link InspectorEnvironment#registerEntryHook(Class, String, EntryHook)}
     */
    interface EntryHook {
        /**
         * Called inline at the entry of an instrumented method.
         *
         * @param thisObject "this" object of origin method or {@code null} if origin method is
         *                   static.
         * @param args arguments passed into the origin method
         */
        void onEntry(@Nullable Object thisObject, @NonNull List<Object> args);
    }

    /**
     * Register entry hook for the {@code originMethod} in the {@code originClass}.
     * <p/>
     * This method performs bytecode transformation and injects a call to {@code entryHook}
     * at the start of {@code originMethod} of {@code originClass}.
     * <p/>
     * {@code originMethod} should be in the format:
     * "methodName(signature)", where signature is JAVA VM's format (the one that JNI uses). For
     * example, for method {@code Foo bla(Bar bla);} it should look like:
     * {@code bla(LpackageOfBar/Bar;)LpackageOfFoo/Foo;}
     *
     * @param originClass  class where {@code originMethod} is defined
     * @param originMethod method which should be instrumented with entry hook
     * @param entryHook    a hook to be called at the entry of {@code origin method}
     */
    void registerEntryHook(@NonNull Class<?> originClass, @NonNull String originMethod,
            @NonNull EntryHook entryHook);

    /**
     * A callback invoked at the exit to an instrumented method.
     *
     * @param <T> The type of data returned by an instrumented method.
     */
    interface ExitHook<T> {
        /**
         * Called inline at the exit of an instrumented method and allows to intercept
         * a returned value of an origin method.
         *
         * @param result an object that was meant to be returned by origin method
         * @return an object that should be returned instead by origin method.
         */
        T onExit(T result);
    }

    /**
     * Register exit hook for the {@code originMethod} in the {@code originClass}.
     * <p/>
     * This method performs bytecode transformation and injects a call to {@code exitHook}
     * at the end of {@code originMethod} of {@code originClass}.
     * <p/>
     * {@code originMethod} should be in the format:
     * "methodName(signature)", where signature is JAVA VM's format (the one that JNI uses). For
     * example, for method {@code Foo bla(Bar bla);} it should look like:
     * {@code bla(LpackageOfBar/Bar;)LpackageOfFoo/Foo;}
     *
     * @param originClass  class where {@code originMethod} is defined
     * @param originMethod method which should be instrumented with entry hook
     * @param exitHook    a hook to be called at the exit of {@code origin method}
     */
    <T> void registerExitHook(@NonNull Class<?> originClass, @NonNull String originMethod,
            @NonNull ExitHook<T> exitHook);
}
