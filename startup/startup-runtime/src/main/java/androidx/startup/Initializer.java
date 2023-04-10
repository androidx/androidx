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

package androidx.startup;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Initializes library components during app startup.
 *
 * Discovered and initialized by {@link InitializationProvider}.
 *
 * @param <T> The type of the component being initialized.
 */
public interface Initializer<T> {

    /**
     * Initializes a library component within the application {@link Context}.
     *
     * @param context The application context.
     */
    @NonNull
    T create(@NonNull Context context);

    /**
     * Gets a list of this initializer's dependencies.
     *
     * Dependencies are initialized before the dependent initializer. For
     * example, if initializer A defines initializer B as a dependency, B is
     * initialized before A.
     *
     * @return A list of initializer dependencies.
     */
    @NonNull
    List<Class<? extends Initializer<?>>> dependencies();
}
