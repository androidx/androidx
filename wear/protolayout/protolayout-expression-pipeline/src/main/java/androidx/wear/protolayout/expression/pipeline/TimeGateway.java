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
 * A gateway to Time data sources. This should call any provided callbacks every second.
 *
 * <p>Implementations of this class should track a few things:
 *
 * <ul>
 *   <li>Surface lifecycle. Implementations should keep track of registered consumers, and
 *       deregister them all when the surface is no longer available.
 *   <li>Device state. Implementations should react to device state (i.e. ambient mode), and
 *       activity state (i.e. is the surface in the foreground), and enable/disable updates
 *       accordingly.
 * </ul>
 */
interface TimeGateway {
    /** Callback for time notifications. */
    interface TimeCallback {
        /**
         * Called just before an update happens. All onPreUpdate calls will be made before any
         * onUpdate calls fire.
         *
         * <p>Will be called on the same executor passed to {@link TimeGateway#registerForUpdates}.
         */
        void onPreUpdate();

        /**
         * Notifies that the current time has changed.
         *
         * <p>Will be called on the same executor passed to {@link TimeGateway#registerForUpdates}.
         */
        void onData();
    }

    /** Register for time updates. All callbacks will be called on the provided executor. */
    void registerForUpdates(@NonNull Executor executor, @NonNull TimeCallback callback);

    /** Unregister for time updates. */
    void unregisterForUpdates(@NonNull TimeCallback callback);
}
