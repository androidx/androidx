/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appsearch.localstorage;

import androidx.annotation.RestrictTo;
import androidx.appsearch.localstorage.stats.CallStats;
import androidx.appsearch.localstorage.stats.InitializeStats;
import androidx.appsearch.localstorage.visibilitystore.VisibilityChecker;

import com.google.android.icing.IcingSearchEngine;
import com.google.android.icing.IcingSearchEngineInterface;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A container for optional plugins and instrumentation builders used by {@link AppSearchImpl}.
 *
 * <p> All params in this class MUST be optional or has default value.
 *
 * <p>This class encapsulates optional dependencies and stateful builders (like stats collectors)
 * to keep the {@link AppSearchImpl#create} signature clean and extensible.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AppSearchUserPlugins {
    private final @Nullable VisibilityChecker mVisibilityChecker;
    private final @Nullable RevocableFileDescriptorStore mRevocableFileDescriptorStore;
    private final @Nullable IcingSearchEngineInterface mIcingSearchEngine;
    private final InitializeStats.@Nullable Builder mInitStatsBuilder;
    private final CallStats.@Nullable Builder mCallStatsBuilder;
    private final @NonNull LaunchVMFeatures mLaunchVMFeatures;

    /** An empty {@link AppSearchUserPlugins} instance with no plugins or stats builders. */
    public static final AppSearchUserPlugins EMPTY = new Builder().build();

    AppSearchUserPlugins(@NonNull Builder builder) {
        mVisibilityChecker = builder.mVisibilityChecker;
        mRevocableFileDescriptorStore = builder.mRevocableFileDescriptorStore;
        mIcingSearchEngine = builder.mIcingSearchEngine;
        mInitStatsBuilder = builder.mInitStatsBuilder;
        mCallStatsBuilder = builder.mCallStatsBuilder;
        mLaunchVMFeatures = builder.mLaunchVMFeatures;
    }

    /**
     * Returns the {@link VisibilityChecker} to verify access to schemas, or {@code null} if
     * not set.
     */
    @Nullable
    public VisibilityChecker getVisibilityChecker() {
        return mVisibilityChecker;
    }

    /**
     * Returns the {@link RevocableFileDescriptorStore} for managing file descriptors, or
     * {@code null} if not set.
     */
    @Nullable
    public RevocableFileDescriptorStore getRevocableFileDescriptorStore() {
        return mRevocableFileDescriptorStore;
    }

    /**
     * Returns the {@link IcingSearchEngineInterface} to use, or {@code null} if a default
     * {@link IcingSearchEngine} should be created.
     */
    @Nullable
    public IcingSearchEngineInterface getIcingSearchEngine() {
        return mIcingSearchEngine;
    }

    /** Returns the builder for collecting initialization stats, or {@code null} if not set. */

    public InitializeStats.@Nullable Builder getInitStatsBuilder() {
        return mInitStatsBuilder;
    }

    /** Returns the builder for collecting call stats, or {@code null} if not set. */
    public CallStats.@Nullable Builder getCallStatsBuilder() {
        return mCallStatsBuilder;
    }

    /** Returns whether the AI seal feature is enabled.  */
    @NonNull
    public LaunchVMFeatures getLaunchVMFeatures() {
        return mLaunchVMFeatures;
    }

    /** Builder for {@link AppSearchUserPlugins}. */
    public static final class Builder {
        private @Nullable VisibilityChecker mVisibilityChecker;
        private @Nullable RevocableFileDescriptorStore mRevocableFileDescriptorStore;
        private @Nullable IcingSearchEngineInterface mIcingSearchEngine;
        private InitializeStats.@Nullable Builder mInitStatsBuilder;
        private CallStats.@Nullable Builder mCallStatsBuilder;
        private @NonNull LaunchVMFeatures mLaunchVMFeatures = new LaunchVMFeatures();

        public Builder() {}

        /**
         * Sets the {@link VisibilityChecker} to check caller access to specific schemas.
         *
         * <p>If null, global queriers will only be able to access their own data.
         */
        @NonNull
        public Builder setVisibilityChecker(@Nullable VisibilityChecker visibilityChecker) {
            mVisibilityChecker = visibilityChecker;
            return this;
        }

        /** Sets the {@link RevocableFileDescriptorStore} for managing file descriptors. */
        @NonNull
        public Builder setRevocableFileDescriptorStore(
                @Nullable RevocableFileDescriptorStore revocableFileDescriptorStore) {
            mRevocableFileDescriptorStore = revocableFileDescriptorStore;
            return this;
        }

        /**
         * Sets the underlying {@link IcingSearchEngineInterface}.
         *
         * <p>If null, a new {@link IcingSearchEngine} instance will be created during init.
         */
        @NonNull
        public Builder setIcingSearchEngine(
                @Nullable IcingSearchEngineInterface icingSearchEngine) {
            mIcingSearchEngine = icingSearchEngine;
            return this;
        }

        /** Sets the {@link InitializeStats.Builder} for collecting initialization telemetry. */
        @NonNull
        public Builder setInitStatsBuilder(InitializeStats.@Nullable Builder initStatsBuilder) {
            mInitStatsBuilder = initStatsBuilder;
            return this;
        }

        /** Sets the {@link CallStats.Builder} for collecting general call telemetry. */
        @NonNull
        public Builder setCallStatsBuilder(CallStats.@Nullable Builder callStatsBuilder) {
            mCallStatsBuilder = callStatsBuilder;
            return this;
        }

        /** Sets whether the AI seal feature is enabled. */
        @NonNull
        public Builder setLaunchVMFeatures(@NonNull LaunchVMFeatures launchVMFeatures) {
            mLaunchVMFeatures = launchVMFeatures;
            return this;
        }

        /** Builds a new {@link AppSearchUserPlugins} instance. */
        @NonNull
        public AppSearchUserPlugins build() {
            return new AppSearchUserPlugins(this);
        }
    }
}
