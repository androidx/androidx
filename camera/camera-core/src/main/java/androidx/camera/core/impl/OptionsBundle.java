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

import android.util.ArrayMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * An immutable implementation of {@link Config}.
 *
 * <p>OptionsBundle is a collection of {@link Config.Option}s and their values which can be
 * queried based on exact {@link Config.Option} objects or based on Option ids.
 */
public class OptionsBundle implements Config {
    protected static final Comparator<Option<?>> ID_COMPARE =
            (o1, o2) -> {
                return o1.getId().compareTo(o2.getId());
            };
    private static final OptionsBundle EMPTY_BUNDLE =
            new OptionsBundle(new TreeMap<>(ID_COMPARE));

    // TODO: Make these options parcelable
    protected final TreeMap<Option<?>, Map<OptionPriority, Object>> mOptions;

    OptionsBundle(TreeMap<Option<?>, Map<OptionPriority, Object>> options) {
        mOptions = options;
    }

    /**
     * Create an OptionsBundle from another configuration.
     *
     * <p>This will copy the options/values from the provided configuration.
     *
     * @param otherConfig Configuration containing options/values to be copied.
     * @return A new OptionsBundle pre-populated with options/values.
     */
    public static @NonNull OptionsBundle from(@NonNull Config otherConfig) {
        // No need to create another instance since OptionsBundle is immutable
        if (OptionsBundle.class.equals(otherConfig.getClass())) {
            return (OptionsBundle) otherConfig;
        }

        TreeMap<Option<?>, Map<OptionPriority, Object>> persistentOptions =
                new TreeMap<>(ID_COMPARE);
        for (Option<?> opt : otherConfig.listOptions()) {
            Set<OptionPriority> priorities = otherConfig.getPriorities(opt);
            Map<OptionPriority, Object> valuesMap = new ArrayMap<>();
            for (OptionPriority priority : priorities) {
                valuesMap.put(priority, otherConfig.retrieveOptionWithPriority(opt, priority));
            }
            persistentOptions.put(opt, valuesMap);
        }

        return new OptionsBundle(persistentOptions);
    }

    /**
     * Create an empty OptionsBundle.
     *
     * <p>This options bundle will have no option/value pairs.
     *
     * @return An OptionsBundle pre-populated with no options/values.
     */
    public static @NonNull OptionsBundle emptyBundle() {
        return EMPTY_BUNDLE;
    }

    @Override
    public @NonNull Set<Option<?>> listOptions() {
        return Collections.unmodifiableSet(mOptions.keySet());
    }

    @Override
    public boolean containsOption(@NonNull Option<?> id) {
        return mOptions.containsKey(id);
    }

    @Override
    public <ValueT> @Nullable ValueT retrieveOption(@NonNull Option<ValueT> id) {
        Map<OptionPriority, Object> values = mOptions.get(id);
        if (values == null) {
            throw new IllegalArgumentException("Option does not exist: " + id);
        }
        OptionPriority highestPrirotiy = Collections.min(values.keySet());

        @SuppressWarnings("unchecked")
        ValueT value = (ValueT) values.get(highestPrirotiy);
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ValueT> @Nullable ValueT retrieveOption(@NonNull Option<ValueT> id,
            @Nullable ValueT valueIfMissing) {
        // For performance reason, do not catch IllegalArgumentException from retrieveOption(id)
        // to return valueIfMissing.
        Map<OptionPriority, Object> values = mOptions.get(id);
        if (values == null) {
            return valueIfMissing;
        }
        OptionPriority highestPrirotiy = Collections.min(values.keySet());

        @SuppressWarnings("unchecked")
        ValueT value = (ValueT) values.get(highestPrirotiy);
        return value;
    }

    @Override
    public <ValueT> @Nullable ValueT retrieveOptionWithPriority(@NonNull Option<ValueT> id,
            @NonNull OptionPriority priority) {
        Map<OptionPriority, Object> values = mOptions.get(id);
        if (values == null) {
            throw new IllegalArgumentException("Option does not exist: " + id);
        }
        if (!values.containsKey(priority)) {
            throw new IllegalArgumentException("Option does not exist: " + id + " with priority="
                    + priority);
        }
        @SuppressWarnings("unchecked")
        ValueT value = (ValueT) values.get(priority);
        return value;
    }

    @Override
    public @NonNull OptionPriority getOptionPriority(@NonNull Option<?> opt) {
        Map<OptionPriority, Object> values = mOptions.get(opt);
        if (values == null) {
            throw new IllegalArgumentException("Option does not exist: " + opt);
        }
        OptionPriority highestPrirotiy = Collections.min(values.keySet());
        return highestPrirotiy;
    }

    @Override
    public void findOptions(@NonNull String idSearchString, @NonNull OptionMatcher matcher) {
        Option<Void> query = Option.create(idSearchString, Void.class);
        for (Map.Entry<Option<?>, Map<OptionPriority, Object>> entry :
                mOptions.tailMap(query).entrySet()) {
            if (!entry.getKey().getId().startsWith(idSearchString)) {
                // We've reached the end of the range that contains our search stem.
                break;
            }

            Option<?> option = entry.getKey();
            if (!matcher.onOptionMatched(option)) {
                // Caller does not need further results
                break;
            }
        }
    }

    @Override
    public @NonNull Set<OptionPriority> getPriorities(@NonNull Option<?> opt) {
        Map<OptionPriority, Object> values = mOptions.get(opt);
        if (values == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(values.keySet());
    }
}
