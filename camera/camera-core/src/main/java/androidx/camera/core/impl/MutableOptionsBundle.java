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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A MutableOptionsBundle is an {@link OptionsBundle} which allows for insertion/removal.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class MutableOptionsBundle extends OptionsBundle implements MutableConfig {
    @NonNull
    private static final OptionPriority DEFAULT_PRIORITY = OptionPriority.OPTIONAL;

    private MutableOptionsBundle(
            TreeMap<Option<?>, Map<OptionPriority, Object>> persistentOptions) {
        super(persistentOptions);
    }

    /**
     * Creates an empty MutableOptionsBundle.
     *
     * @return an empty MutableOptionsBundle containing no options.
     */
    @NonNull
    public static MutableOptionsBundle create() {
        return new MutableOptionsBundle(new TreeMap<>(ID_COMPARE));
    }

    /**
     * Creates a MutableOptionsBundle from an existing immutable Config.
     *
     * @param otherConfig configuration options to insert.
     * @return a MutableOptionsBundle prepopulated with configuration options.
     */
    @NonNull
    public static MutableOptionsBundle from(@NonNull Config otherConfig) {
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

        return new MutableOptionsBundle(persistentOptions);
    }

    @Nullable
    @Override
    public <ValueT> ValueT removeOption(@NonNull Option<ValueT> opt) {
        @SuppressWarnings("unchecked") // Options should have only been inserted via insertOption()
                ValueT value = (ValueT) mOptions.remove(opt);

        return value;
    }

    @Override
    public <ValueT> void insertOption(@NonNull Option<ValueT> opt, @Nullable ValueT value) {
        insertOption(opt, DEFAULT_PRIORITY, value);
    }

    @Override
    public <ValueT> void insertOption(@NonNull Option<ValueT> opt,
            @NonNull OptionPriority priority, @Nullable ValueT value) {
        Map<OptionPriority, Object> values = mOptions.get(opt);

        if (values == null) {
            // the option is first added
            values = new ArrayMap<>();
            mOptions.put(opt, values);
            values.put(priority, value);
            return;
        }

        // get the highest priority.
        OptionPriority priority1 = Collections.min(values.keySet());
        OptionPriority priority2 = priority;
        Object value1 = values.get(priority1);
        ValueT value2 = value;
        if (!value1.equals(value2) && Config.hasConflict(priority1, priority2)) {
            throw new IllegalArgumentException("Option values conflicts: " + opt.getId()
                    + ", existing value (" + priority1 + ")=" + values.get(priority1)
                    + ", conflicting (" + priority2 + ")=" + value);
        }

        values.put(priority, value);
    }
}
