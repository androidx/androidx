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

    // TODO(b/142729179): migrate this API to callbacks instead of static methods
    /**
     * Register entry hook for the {@code originMethod} in the {@code originClass}.
     * <p/>
     * This method performs bytecode transformation and injects a call to {@code hookMethod} of
     * {@code hookClass} at the start of {@code originMethod} of {@code originClass}.
     * <p/>
     * {@code originMethod} should be in the format:
     * "methodName(signature)", where signature is JAVA VM's format (the one that JNI uses). For
     * example, for method {@code Foo bla(Bar bla);} it should look like:
     * {@code bla(LpackageOfBar/Bar;)LpackageOfFoo/Foo;}
     * <p/>
     * {@code hookMethodName} is simply a name of the method <b>without signature</b>, this
     * method:
     * <ul>
     * <li> must be public static;</li>
     * <li> must have void return type;</li>
     * <li> must have {@link Object} as a first param, if the origin method isn't static. Reference
     * to {@code this} object will be passed as a first parameter;</li>
     * <li>must have all other parameters strictly matching parameters of {@code originMethod} </li>
     * </ul>
     *
     * @param originClass    class where {@code originMethod} is defined
     * @param originMethod   method which should be instrumented with entry hook
     * @param hookClass      class where {@code hookMethod} is defined
     * @param hookMethodName name of a method that should be called at the start of
     *                       {@code originMethod}
     */
    void registerEntryHook(@NonNull Class<?> originClass, @NonNull String originMethod,
            @NonNull Class<?> hookClass, @NonNull String hookMethodName);

    /**
     * Register exit hook for the {@code originMethod} in the {@code originClass}.
     * <p/>
     * This method performs bytecode transformation and injects a call to {@code hookMethod} of
     * {@code hookClass} at the end of {@code originMethod} of {@code originClass}.
     * <p/>
     * {@code originMethod} should be in the format:
     * "methodName(signature)", where signature is JAVA VM's format (the one that JNI uses). For
     * example, for method {@code Foo bla(Bar bla);} it should look like:
     * {@code bla(LpackageOfBar/Bar;)LpackageOfFoo/Foo;}
     * <p/>
     * {@code hookMethodName} is simply a name of the method <b>without signature</b>, this
     * method:
     * <ul>
     * <li> must be public static;</li>
     * <li> must have the same return type as {@code originMethod};</li>
     * <li> must have the only one parameter that matches return type of {@code originMethod};</li>
     * </ul>
     *
     * @param originClass  class where {@code originMethod} is defined
     * @param originMethod method which should be instrumented with entry hook
     */
    void registerExitHook(@NonNull Class<?> originClass, @NonNull String originMethod,
            @NonNull Class<?> hookClass, @NonNull String hookMethod);
}
