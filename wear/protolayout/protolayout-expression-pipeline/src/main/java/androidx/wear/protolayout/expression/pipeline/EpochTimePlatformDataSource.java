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
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Utility for time data source. */
class EpochTimePlatformDataSource {
    @NonNull private final MainThreadExecutor mExecutor = new MainThreadExecutor();

    @NonNull final List<DynamicTypeValueReceiverWithPreUpdate<Instant>> mConsumerToTimeCallback =
            new ArrayList<>();
    @NonNull private final Supplier<Instant> mClock;
    @Nullable private final PlatformTimeUpdateNotifier mUpdateNotifier;

    EpochTimePlatformDataSource(
            @NonNull Supplier<Instant> clock,
            @Nullable PlatformTimeUpdateNotifier platformTimeUpdateNotifier) {
        this.mClock = clock;
        this.mUpdateNotifier = platformTimeUpdateNotifier;
    }

    @UiThread
    void registerForData(DynamicTypeValueReceiverWithPreUpdate<Instant> consumer) {
        if (mConsumerToTimeCallback.isEmpty() && mUpdateNotifier != null) {
            mUpdateNotifier.setReceiver(mExecutor, this::tick);
        }
        mConsumerToTimeCallback.add(consumer);
    }

    @UiThread
    void unregisterForData(DynamicTypeValueReceiverWithPreUpdate<Instant> consumer) {
        mConsumerToTimeCallback.remove(consumer);
        if (mConsumerToTimeCallback.isEmpty() && mUpdateNotifier != null) {
            mUpdateNotifier.clearReceiver();
        }
    }

    /**
     * Updates all registered consumers with the new time.
     */
    @SuppressWarnings("NullAway")
    private void tick() {
        mConsumerToTimeCallback.forEach(
                DynamicTypeValueReceiverWithPreUpdate::onPreUpdate);
        Instant currentTime = mClock.get();
        mConsumerToTimeCallback.forEach(c -> c.onData(currentTime));
    }

    @VisibleForTesting
    int getRegisterConsumersCount() {
        return mConsumerToTimeCallback.size();
    }
}
