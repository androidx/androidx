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

import java.util.Comparator;
import java.util.TreeMap;

/**
 * A MutableOptionsBundle is an {@link OptionsBundle} which allows for insertion/removal.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class MutableOptionsBundle extends OptionsBundle implements MutableConfig {

    private static final Comparator<Option<?>> ID_COMPARE =
            new Comparator<Option<?>>() {
                @Override
                public int compare(Option<?> o1, Option<?> o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            };

    private MutableOptionsBundle(TreeMap<Option<?>, Object> persistentOptions) {
        super(persistentOptions);
    }

    /**
     * Creates an empty MutableOptionsBundle.
     *
     * @return an empty MutableOptionsBundle containing no options.
     */
    public static MutableOptionsBundle create() {
        return new MutableOptionsBundle(new TreeMap<>(ID_COMPARE));
    }

    /**
     * Creates a MutableOptionsBundle from an existing immutable Config.
     *
     * @param otherConfig configuration options to insert.
     * @return a MutableOptionsBundle prepopulated with configuration options.
     */
    public static MutableOptionsBundle from(Config otherConfig) {
        TreeMap<Option<?>, Object> persistentOptions = new TreeMap<>(ID_COMPARE);
        for (Option<?> opt : otherConfig.listOptions()) {
            persistentOptions.put(opt, otherConfig.retrieveOption(opt));
        }

        return new MutableOptionsBundle(persistentOptions);
    }

    @Nullable
    @Override
    public <ValueT> ValueT removeOption(Option<ValueT> opt) {
        @SuppressWarnings("unchecked") // Options should have only been inserted via insertOption()
                ValueT value = (ValueT) mOptions.remove(opt);

        return value;
    }

    @Override
    public <ValueT> void insertOption(Option<ValueT> opt, ValueT value) {
        mOptions.put(opt, value);
    }
}
