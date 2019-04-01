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

package androidx.camera.testing.fakes;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.Config;
import androidx.camera.core.MutableConfig;
import androidx.camera.core.MutableOptionsBundle;
import androidx.camera.core.OptionsBundle;

import java.util.Set;

/**
 * Wrapper for an empty Config
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class FakeConfig implements Config.Reader {

    private final Config mConfig;

    FakeConfig(Config config) {
        mConfig = config;
    }

    @Override
    public Config getConfig() {
        return mConfig;
    }

    /** Builder for an empty Config */
    public static final class Builder implements Config.Builder<FakeConfig, Builder> {

        private final MutableOptionsBundle mOptionsBundle;

        public Builder() {
            mOptionsBundle = MutableOptionsBundle.create();
        }

        @Override
        public MutableConfig getMutableConfig() {
            return mOptionsBundle;
        }

        @Override
        public Builder builder() {
            return this;
        }

        @Override
        public FakeConfig build() {
            return new FakeConfig(OptionsBundle.from(mOptionsBundle));
        }

        // Start of the default implementation of Config.Builder
        // *****************************************************************************************

        // Implementations of Config.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public <ValueT> Builder insertOption(Option<ValueT> opt, ValueT value) {
            getMutableConfig().insertOption(opt, value);
            return builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @Nullable
        public <ValueT> Builder removeOption(Option<ValueT> opt) {
            getMutableConfig().removeOption(opt);
            return builder();
        }

        // End of the default implementation of Config.Builder
        // *****************************************************************************************
    }

    // Start of the default implementation of Config
    // *********************************************************************************************

    // Implementations of Config.Reader default methods

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public boolean containsOption(Option<?> id) {
        return getConfig().containsOption(id);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(Option<ValueT> id) {
        return getConfig().retrieveOption(id);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(Option<ValueT> id, @Nullable ValueT valueIfMissing) {
        return getConfig().retrieveOption(id, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void findOptions(String idStem, OptionMatcher matcher) {
        getConfig().findOptions(idStem, matcher);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Set<Option<?>> listOptions() {
        return getConfig().listOptions();
    }

    // End of the default implementation of Config
    // *********************************************************************************************
}
