/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.expression.pipeline;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

/**
 * Controls evaluation triggering of time-related expressions.
 *
 * <p>Can be optionally provided to {@link DynamicTypeEvaluator} in order to have fine control of
 * how time-related expressions are triggered for re-evaluation.
 */
public interface TimeGateway {
    /** Callback for {@link TimeGateway} triggers. */
    interface TimeCallback {
        /**
         * Called just before an update happens. All onPreUpdate calls will be made before any
         * onUpdate calls fire.
         *
         * <p>Will be called on the same executor passed to {@link TimeGateway#registerForUpdates}.
         *
         * <p>It is up to the caller to ensure synchronization between {@link #onPreUpdate()} and
         * {@link #onData()} calls (both called on the {@link Executor} provided to {@link
         * #registerForUpdates}), e.g. by providing a single-threaded ordered {@link Executor}. If
         * not synchronized, it's possible that {@link #onData()} will be invoked before {@link
         * #onPreUpdate()} on the same time tick.
         */
        void onPreUpdate();

        /**
         * Notifies that the current time has changed.
         *
         * <p>Will be called on the same executor passed to {@link TimeGateway#registerForUpdates}.
         *
         * <p>It is up to the caller to ensure synchronization between {@link #onPreUpdate()} and
         * {@link #onData()} calls (both called on the {@link Executor} provided to {@link
         * #registerForUpdates}), e.g. by providing a single-threaded ordered {@link Executor}. If
         * not synchronized, it's possible that {@link #onData()} will be invoked before {@link
         * #onPreUpdate()} on the same time tick.
         */
        void onData();
    }

    /**
     * Register for time updates. All callbacks will be called on the provided executor.
     *
     * <p>It is up to the caller to ensure synchronization between {@link
     * TimeCallback#onPreUpdate()} and {@link TimeCallback#onData()} calls (both called on the
     * provided {@link Executor}), e.g. by providing a single-threaded ordered {@link Executor}. If
     * not synchronized, it's possible that {@link TimeCallback#onData()} will be invoked before
     * {@link TimeCallback#onPreUpdate()} on the same time tick.
     */
    void registerForUpdates(@NonNull Executor executor, @NonNull TimeCallback callback);

    /** Unregister for time updates. */
    void unregisterForUpdates(@NonNull TimeCallback callback);
}
