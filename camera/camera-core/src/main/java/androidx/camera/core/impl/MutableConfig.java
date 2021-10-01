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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * MutableConfig is a {@link Config} that can be modified.
 *
 * <p>MutableConfig is the interface used to create immutable Config objects.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface MutableConfig extends Config {

    /**
     * Inserts a Option/Value pair into the configuration
     *
     * @param opt      The option to be added or modified
     * @param value    The value to insert for this option.
     * @param <ValueT> The type of the value being inserted.
     */
    <ValueT> void insertOption(@NonNull Option<ValueT> opt, @Nullable ValueT value);

    /**
     * Inserts a Option/Value pair into the configuration by the rules of {@link OptionPriority}.
     *
     * @param opt      The option to be added or modified
     * @param value    The value to insert for this option.
     * @param <ValueT> The type of the value being inserted.
     * @throws {@link IllegalArgumentException} if there is a conflict that cannot be resolved.
     */
    <ValueT> void insertOption(@NonNull Option<ValueT> opt, @NonNull OptionPriority priority,
            @Nullable ValueT value);

    /**
     * Removes an option from the configuration if it exists.
     *
     * @param opt      The option to remove from the configuration.
     * @param <ValueT> The type of the value being removed.
     * @return The value that previously existed for <code>opt</code>, or <code>null</code> if the
     * option did not exist in this configuration.
     */
    @Nullable
    <ValueT> ValueT removeOption(@NonNull Option<ValueT> opt);
}
