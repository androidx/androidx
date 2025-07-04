/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appsearch.stats;

import android.annotation.SuppressLint;

import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

/**
 * Encapsulates base statistics information for AppSearch results.
 *
 * This class provides a convenient way to store and retrieve key statistics,
 * such as the result code and a bitmask of enabled features.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BaseStats {
    private static final int LAUNCH_VM = 0;

    private final long mEnabledFeatures;

    protected BaseStats(@NonNull Builder<?> builder) {
        Preconditions.checkNotNull(builder);
        mEnabledFeatures = builder.mEnabledFeatures;
    }

    /** Returns the bitmask representing the enabled features. */
    public long getEnabledFeatures() {
        return mEnabledFeatures;
    }

    /**
     * Builder for {@link BaseStats}.
     *
     * @param <BuilderType> Type of subclass who extends this.
     */
    // This builder is specifically designed to be extended by stats classes.
    @SuppressLint("StaticFinalBuilder")
    @SuppressWarnings("rawtypes")
    public static class Builder<BuilderType extends Builder> {
        private final BuilderType mBuilderTypeInstance;
        long mEnabledFeatures;

        /** Creates a new {@link BaseStats.Builder}. */
        @SuppressWarnings("unchecked")
        public Builder() {
            mBuilderTypeInstance = (BuilderType) this;
        }

        /** Sets bitmask for all enabled features . */
        @CanIgnoreReturnValue
        public @NonNull BuilderType setLaunchVMEnabled(boolean enabled) {
            modifyEnabledFeature(LAUNCH_VM, enabled);
            return mBuilderTypeInstance;
        }

        /** Builds the {@link BaseStats} instance. */
        public @NonNull BaseStats build() {
            return new BaseStats(this);
        }

        private void modifyEnabledFeature(int feature, boolean enabled) {
            if (enabled) {
                mEnabledFeatures |= (1L << feature);
            } else {
                mEnabledFeatures &= ~(1L << feature);
            }
        }
    }
}
