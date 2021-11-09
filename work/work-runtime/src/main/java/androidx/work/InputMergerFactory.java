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

package androidx.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * A factory object that creates {@link InputMerger} instances. The factory is invoked every
 * time a work runs. You can override the default implementation of this factory by manually
 * initializing {@link WorkManager} (see {@link WorkManager#initialize(Context, Configuration)}
 * and specifying a new {@link InputMergerFactory} in
 * {@link Configuration.Builder#setInputMergerFactory(InputMergerFactory)}.
 */
public abstract class InputMergerFactory {
    /**
     * Override this method to create an instance of a {@link InputMerger} given its fully
     * qualified class name.
     * <p></p>
     * Throwing an {@link Exception} here will crash the application. If an
     * {@link InputMergerFactory} is unable to create an instance of a {@link InputMerger}, it
     * should return {@code null} so it can delegate to the default {@link InputMergerFactory}.
     *
     * @param className The fully qualified class name for the {@link InputMerger}
     * @return an instance of {@link InputMerger}
     */
    @Nullable
    public abstract InputMerger createInputMerger(@NonNull String className);

    /**
     * Creates an instance of a {@link InputMerger} given its fully
     * qualified class name with the correct fallback behavior.
     *
     * @param className The fully qualified class name for the {@link InputMerger}
     * @return an instance of {@link InputMerger}
     *
     * @hide
     */
    @Nullable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public final InputMerger createInputMergerWithDefaultFallback(@NonNull String className) {
        InputMerger inputMerger = createInputMerger(className);
        if (inputMerger == null) {
            inputMerger = InputMerger.fromClassName(className);
        }
        return inputMerger;
    }

    /**
     * @return A default {@link InputMergerFactory} with no custom behavior.
     *
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static InputMergerFactory getDefaultInputMergerFactory() {
        return new InputMergerFactory() {
            @Nullable
            @Override
            public InputMerger createInputMerger(@NonNull String className) {
                return null;
            }
        };
    }
}
