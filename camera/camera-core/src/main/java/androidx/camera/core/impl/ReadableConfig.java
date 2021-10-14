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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Set;

/**
 * Interface that can be extended to create APIs for reading specific options.
 *
 * <p>ReadableConfig objects are also {@link Config} objects, so can be passed to any method that
 * expects a {@link Config}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ReadableConfig extends Config {

    /**
     * Returns the underlying immutable {@link Config} object.
     *
     * @return The underlying {@link Config} object.
     */
    @NonNull
    Config getConfig();

    @Override
    default boolean containsOption(@NonNull Option<?> id) {
        return getConfig().containsOption(id);
    }

    @Override
    @Nullable
    default <ValueT> ValueT retrieveOption(@NonNull Option<ValueT> id) {
        return getConfig().retrieveOption(id);
    }

    @Override
    @Nullable
    default <ValueT> ValueT retrieveOption(@NonNull Option<ValueT> id,
            @Nullable ValueT valueIfMissing) {
        return getConfig().retrieveOption(id, valueIfMissing);
    }

    @Override
    default void findOptions(@NonNull String idSearchString, @NonNull OptionMatcher matcher) {
        getConfig().findOptions(idSearchString, matcher);
    }

    @Override
    @NonNull
    default Set<Option<?>> listOptions() {
        return getConfig().listOptions();
    }

    @Override
    @Nullable
    default <ValueT> ValueT retrieveOptionWithPriority(@NonNull Option<ValueT> id,
            @NonNull OptionPriority priority) {
        return getConfig().retrieveOptionWithPriority(id, priority);
    }

    @Override
    @NonNull
    default OptionPriority getOptionPriority(@NonNull Option<?> opt) {
        return getConfig().getOptionPriority(opt);
    }

    @NonNull
    @Override
    default Set<OptionPriority> getPriorities(@NonNull Option<?> option) {
        return getConfig().getPriorities(option);
    }
}
