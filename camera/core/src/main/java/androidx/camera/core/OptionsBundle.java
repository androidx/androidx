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

import java.util.Collections;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * An immutable implementation of {@link Config}.
 *
 * <p>OptionsBundle is a collection of {@link Config.Option}s and their values which can be
 * queried based on exact {@link Config.Option} objects or based on Option ids.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public class OptionsBundle implements Config {

    private static final OptionsBundle EMPTY_BUNDLE =
            new OptionsBundle(new TreeMap<>(new Comparator<Option<?>>() {
                @Override
                public int compare(Option<?> o1, Option<?> o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            }));
    // TODO: Make these options parcelable
    protected final TreeMap<Option<?>, Object> mOptions;

    OptionsBundle(TreeMap<Option<?>, Object> options) {
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
    public static OptionsBundle from(Config otherConfig) {
        // No need to create another instance since OptionsBundle is immutable
        if (OptionsBundle.class.equals(otherConfig.getClass())) {
            return (OptionsBundle) otherConfig;
        }

        TreeMap<Option<?>, Object> persistentOptions =
                new TreeMap<>(new Comparator<Option<?>>() {
                    @Override
                    public int compare(Option<?> o1, Option<?> o2) {
                        return o1.getId().compareTo(o2.getId());
                    }
                });
        for (Option<?> opt : otherConfig.listOptions()) {
            persistentOptions.put(opt, otherConfig.retrieveOption(opt));
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
    public static OptionsBundle emptyBundle() {
        return EMPTY_BUNDLE;
    }

    @Override
    public Set<Option<?>> listOptions() {
        return Collections.unmodifiableSet(mOptions.keySet());
    }

    @Override
    public boolean containsOption(Option<?> id) {
        return mOptions.containsKey(id);
    }

    @Override
    public <ValueT> ValueT retrieveOption(Option<ValueT> id) {
        ValueT value = retrieveOption(id, /*valueIfMissing=*/ null);
        if (value == null) {
            throw new IllegalArgumentException("Option does not exist: " + id);
        }

        return value;
    }

    @Nullable
    @Override
    public <ValueT> ValueT retrieveOption(Option<ValueT> id, @Nullable ValueT valueIfMissing) {
        @SuppressWarnings("unchecked") // Options should have only been inserted via insertOption()
                ValueT value = (ValueT) mOptions.get(id);
        if (value == null) {
            value = valueIfMissing;
        }

        return value;
    }

    @Override
    public void findOptions(String idStem, OptionMatcher matcher) {
        Option<Void> query = Option.create(idStem, Void.class);
        for (Entry<Option<?>, Object> entry : mOptions.tailMap(query).entrySet()) {
            if (!entry.getKey().getId().startsWith(idStem)) {
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
}
