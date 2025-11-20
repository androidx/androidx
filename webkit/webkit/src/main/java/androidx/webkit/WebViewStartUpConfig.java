/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.webkit;

import android.content.Context;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Configuration object for
 * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}.
 * <p>
 * This is different from {@link ProcessGlobalConfig}. This object defines the configuration for
 * a particular call to
 * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}.
 */
@WebViewCompat.ExperimentalAsyncStartUp
public final class WebViewStartUpConfig {
    private final Executor mExecutor;
    private final boolean mShouldRunUiThreadStartUpTasks;
    private final @Nullable Set<String> mProfilesToLoadDuringStartup;

    private WebViewStartUpConfig(
            @NonNull Executor executor, boolean shouldRunUiThreadStartUpTasks,
            @Nullable Set<String> profilesToLoadDuringStartup) {
        mExecutor = executor;
        mShouldRunUiThreadStartUpTasks = shouldRunUiThreadStartUpTasks;
        mProfilesToLoadDuringStartup = profilesToLoadDuringStartup;
    }

    public @NonNull Executor getBackgroundExecutor() {
        return mExecutor;
    }

    /**
     * Whether to run only parts of startup that doesn't block the UI thread.
     * <p>
     * WebView startup tasks that are required to run on the UI thread are not attempted when
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * is called if set to {@code false}.
     * <p>
     * Defaults to `true`. If not set to `false`, UI thread startup tasks will be
     * run.
     */
    public boolean shouldRunUiThreadStartUpTasks() {
        return mShouldRunUiThreadStartUpTasks;
    }

    /**
     * Returns the {@link Set} of profiles to be loaded during the UI thread blocking
     * parts of WebView startup.
     * <p>
     * <p>For more details on the behavior of this setting, see the documentation for
     * {@link WebViewStartUpConfig.Builder#setProfilesToLoadDuringStartup(Set)}.
     * <p>
     *
     * @return A {@link Set} of profiles to be loaded, or {@code null} if
     * this configuration setting is not active.
     * @see WebViewStartUpConfig.Builder#setProfilesToLoadDuringStartup(Set)
     */
    // The lint warning is suppressed here as the null represents a state, please see the javadoc
    // above.
    @SuppressWarnings("NullableCollection")
    public @Nullable Set<String> getProfilesToLoadDuringStartup() {
        return mProfilesToLoadDuringStartup;
    }

    @WebViewCompat.ExperimentalAsyncStartUp
    public static final class Builder {
        private final Executor mExecutor;
        private boolean mShouldRunUiThreadStartUpTasks = true;
        private @Nullable Set<String> mProfilesToLoadDuringStartup = null;

        /**
         * Builder for {@link WebViewStartUpConfig}.
         *
         * @param executor The portions of WebView startup that can run on a background
         *                 thread are scheduled on this executor. Blocking tasks will be run on
         *                 the executor.
         */
        public Builder(@NonNull Executor executor) {
            mExecutor = executor;
        }

        /**
         * Setter to run only parts of startup that doesn't block the UI thread.
         * <p>
         * WebView startup tasks that are required to run on the UI thread are not attempted when
         * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
         * is called if set to {@code false}.
         * <p>
         * Defaults to `true`. If not set to `false`, UI thread startup tasks will be
         * run.
         *
         * @throws IllegalArgumentException if this is set to {@code false} after a set of
         *                                  profiles to load has already been specified via
         *                                  {@link #setProfilesToLoadDuringStartup(Set)}.
         */
        public @NonNull Builder setShouldRunUiThreadStartUpTasks(
                boolean shouldRunUiThreadStartUpTasks) {
            if (mProfilesToLoadDuringStartup != null && !shouldRunUiThreadStartUpTasks) {
                throw new IllegalArgumentException(
                        "Can't specify profiles to load without running UI thread startup tasks");
            }
            mShouldRunUiThreadStartUpTasks = shouldRunUiThreadStartUpTasks;
            return this;
        }

        /**
         * Specifies a set of profiles to load before the startup callback is invoked.
         * <p>
         * This method allows you to specify a set of profiles that are guaranteed to have been
         * loaded before the {@link WebViewCompat.WebViewStartUpCallback} is invoked. This can
         * improve the performance of subsequent operations on those profiles at the cost of
         * increasing the initial startup time.
         *
         * <p>Any profiles not specified in the set will not have resources allocated for them until
         * they are used for the first time.
         *
         * <p>The behavior depends on the value provided:
         * <ul>
         * <li><b>Empty Set:</b> No profiles are loaded, not even the default profile.</li>
         * <li><b>Non-empty Set:</b> All profiles named in the set are loaded. These profiles
         * will be created if they do not already exist.</li>
         * </ul>
         *
         * If this method isn't called, the default profile will be loaded during startup.
         *
         * <p><b>Note:</b> This method cannot be used if
         * {@link #setShouldRunUiThreadStartUpTasks(boolean)} is set to {@code false}.
         *
         * <p>This method will be no-op if
         * {@link WebViewFeature#isStartupFeatureSupported(Context, String)}
         * returns false for {@link WebViewFeature#STARTUP_FEATURE_SET_PROFILES_TO_LOAD}.
         *
         * @param profiles A {@link Set} of profile names to pre-load or an empty Set to load none.
         * @return The {@link Builder} instance for method chaining.
         * @throws IllegalArgumentException if this method is called when
         *                                  {@link #setShouldRunUiThreadStartUpTasks(boolean)}
         *                                  has been set to {@code false}.
         */
        public @NonNull Builder setProfilesToLoadDuringStartup(@NonNull Set<String> profiles) {
            if (!mShouldRunUiThreadStartUpTasks) {
                throw new IllegalArgumentException(
                        "Can't specify profiles to load without running UI thread startup "
                                + "tasks");
            }
            this.mProfilesToLoadDuringStartup = new HashSet<>(profiles);
            return this;
        }

        /**
         * Build and return a {@link WebViewStartUpConfig} object.
         *
         * @return immutable {@link WebViewStartUpConfig} object.
         */
        public @NonNull WebViewStartUpConfig build() {
            return new WebViewStartUpConfig(mExecutor, mShouldRunUiThreadStartUpTasks,
                    mProfilesToLoadDuringStartup);
        }
    }
}
