/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

/**
 * The {@link Identifier} class allows anything to be wrapped as an {@link Identifier} instance.
 * The caller should make the input value object unique when calling the {@link #create(Object)}
 * function. So that the {@link Identifier} can be recognized and used for specific purposes.
 */
@AutoValue
public abstract class Identifier {
    /**
     * Creates an {@link Identifier} for the specified input value.
     */
    @NonNull
    public static Identifier create(@NonNull Object value) {
        return new AutoValue_Identifier(value);
    }

    /**
     * Retrieves the value of this {@link Identifier}.
     */
    @NonNull
    public abstract Object getValue();
}
