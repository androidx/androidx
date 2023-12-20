/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.core.util;

/**
 * Compat version of {@link java.util.function.Function}
 * @param <T> the type of the input to the operation
 * @param <R>: the type of the output of the function
 */
@FunctionalInterface
public interface Function<T, R> {
    /**
     * Applies the function to the argument parameter.
     *
     * @param t the argument for the function
     * @return the result after applying function
     */
    R apply(T t);
}
