/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/**
 * MutableConfig is a {@link Config} that can be modified.
 *
 * <p>MutableConfig is the interface used to create immutable Config objects.
 */
public interface MutableConfig extends Config {

    /**
     * Inserts a Option/Value pair into the configuration.
     *
     * <p>If the option already exists in this configuration, it will be replaced.
     *
     * @param opt      The option to be added or modified
     * @param value    The value to insert for this option.
     * @param <ValueT> The type of the value being inserted.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    <ValueT> void insertOption(Option<ValueT> opt, ValueT value);

    /**
     * Removes an option from the configuration if it exists.
     *
     * @param opt      The option to remove from the configuration.
     * @param <ValueT> The type of the value being removed.
     * @return The value that previously existed for <code>opt</code>, or <code>null</code> if the
     * option did not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    <ValueT> ValueT removeOption(Option<ValueT> opt);
}
