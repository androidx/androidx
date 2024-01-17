/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.wear.protolayout.expression.PlatformDataKey;

import java.util.Set;
import java.util.concurrent.Executor;

/**
 * This interface is used by platform data providers to yield dynamic data for their supported data
 * keys.
 *
 * <p>It's up to the implementations to check if the expression provider has the required permission
 * before sending data with {@link PlatformDataReceiver#onData}. If a required permission is not
 * granted or is revoked they should stop sending more data and call {@link
 * PlatformDataReceiver#onInvalidated(Set)} instead.
 */
public interface PlatformDataProvider {
    /**
     * Sets the receiver for receiving the platform data from this provider.
     *
     * <p>Calling this method while there is already a receiver set, should replace the previous
     * receiver.
     *
     * <p>The implementation should periodically send the dynamic data values for the set of {@link
     * PlatformDataKey}s specified when registering this {@link PlatformDataProvider} in {@link
     * DynamicTypeEvaluator.Config.Builder#addPlatformDataProvider}
     *
     * @param executor The executor to run the {@link PlatformDataReceiver#onData} or {@link
     *     PlatformDataReceiver#onInvalidated(Set)} on.
     * @param receiver The {@link PlatformDataReceiver} to receive the new dynamic values or to be
     *     notified that the current data has been invalidated for the registered keys.
     */
    void setReceiver(@NonNull Executor executor, @NonNull PlatformDataReceiver receiver);

    /** Clears the receiver from the provider. */
    void clearReceiver();
}
