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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Logger;
import androidx.core.util.Consumer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * Singleton holder for managing global {@link QuirkSettings} configuration.
 *
 * <p>This class provides thread-safe access to a single, shared instance of `QuirkSettings`,
 * ensuring consistency across the application. It notify registered listeners when the settings
 * change.
 */
public final class QuirkSettingsHolder {

    /**
     * The default {@link QuirkSettings} configuration.
     *
     * <p>This default configuration enables all quirks when the device natively exhibits them.
     */
    public static final QuirkSettings DEFAULT = QuirkSettings.withDefaultBehavior();
    private static final QuirkSettingsHolder sInstance = new QuirkSettingsHolder();

    private final MutableStateObservable<QuirkSettings> mObservable =
            MutableStateObservable.withInitialState(DEFAULT);

    /**
     * Returns the singleton instance of {@link QuirkSettingsHolder}.
     */
    @NonNull
    public static QuirkSettingsHolder instance() {
        return sInstance;
    }

    /**
     * Retrieves the current global {@link QuirkSettings} instance.
     *
     * <p>This method is thread-safe and returns a snapshot of the current settings.
     *
     * @return The current global QuirkSettings instance.
     */
    @NonNull
    public QuirkSettings get() {
        try {
            return mObservable.fetchData().get();
        } catch (ExecutionException | InterruptedException e) {
            throw new AssertionError("Unexpected error in QuirkSettings StateObservable", e);
        }
    }

    /**
     * Sets the global {@link QuirkSettings} instance, triggering notifications to listeners.
     *
     * @param settings The new QuirkSettings instance to be set globally.
     */
    public void set(@NonNull QuirkSettings settings) {
        mObservable.setState(settings);
    }

    /**
     * Adds a listener to be notified when the global {@link QuirkSettings} change.
     *
     * <p>The listener will be invoked on the specified executor whenever the settings are updated.
     *
     * @param executor The executor on which the listener should be called.
     * @param listener The listener to be notified of changes.
     */
    public void observe(@NonNull Executor executor, @NonNull Consumer<QuirkSettings> listener) {
        mObservable.addObserver(executor, new ObserverToConsumerAdapter<>(listener));
    }

    /**
     * Resets the internal state of the {@link QuirkSettingsHolder}.
     *
     * <p>Clears observers and restores to {@link #DEFAULT}.
     *
     * <p>This method is intended for testing purposes and should not be used in production code.
     */
    @VisibleForTesting
    public void reset() {
        mObservable.removeObservers();
        mObservable.setState(DEFAULT);
    }

    /**
     * Adapts an {@link Observable.Observer} to work with a {@link Consumer}.
     */
    private static class ObserverToConsumerAdapter<T> implements Observable.Observer<T> {
        private static final String TAG = "ObserverToConsumerAdapter";
        private final Consumer<T> mDelegate;

        /**
         * Creates a new ObserverToConsumerAdapter.
         *
         * @param delegate     The underlying consumer to receive filtered updates.
         */
        ObserverToConsumerAdapter(@NonNull Consumer<T> delegate) {
            mDelegate = delegate;
        }

        @Override
        public void onNewData(@Nullable T newValue) {
            mDelegate.accept(newValue);
        }

        @Override
        public void onError(@NonNull Throwable t) {
            Logger.e(TAG, "Unexpected error in Observable", t);
        }
    }
}
