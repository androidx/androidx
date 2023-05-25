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

import static java.util.stream.Collectors.toMap;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.wear.protolayout.expression.DynamicDataKey;
import androidx.wear.protolayout.expression.PlatformDataKey;
import androidx.wear.protolayout.expression.PlatformDataValues;
import androidx.wear.protolayout.expression.proto.DynamicDataProto.DynamicDataValue;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Platform data storage for ProtoLayout, which also supports sending callback when data items
 * change.
 *
 * <p>Note that this class is **not** thread-safe. Since ProtoLayout inflation currently happens on
 * the main thread, and because updates will eventually affect the main thread, this whole class
 * must only be used from the UI thread.
 */
final class PlatformDataStore extends DataStore {
    private static final String TAG = "ProtoLayoutPlatformDataStore";

    private final Executor mUiExecutor;

    @NonNull
    private final Map<PlatformDataKey<?>, DynamicDataValue> mCurrentPlatformData =
            new ArrayMap<>();

    @NonNull
    private final Map<DynamicDataKey<?>,
            Set<DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue>>>
            mRegisteredCallbacks = new ArrayMap<>();

    @NonNull
    private final Map<PlatformDataKey<?>, PlatformDataProvider>
            mSourceKeyToDataProviders = new ArrayMap<>();

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    PlatformDataStore(
            @NonNull Map<PlatformDataKey<?>, PlatformDataProvider> sourceKeyToDataProviders) {
        mSourceKeyToDataProviders.putAll(sourceKeyToDataProviders);
        mUiExecutor = new MainThreadExecutor();
    }

    /**
     * Update the given platform data item.
     *
     * <p>Informs registered listeners of changed values.
     */
    void updatePlatformDataEntries(@NonNull PlatformDataValues newData) {
        updatePlatformDataEntriesProto(
                newData.getAll().entrySet().stream().collect(toMap(
                        entry -> (PlatformDataKey<?>)entry.getKey(),
                        entry -> entry.getValue().toDynamicDataValueProto()))
        );
    }

    /**
     * Update the given platform data item.
     *
     * <p>Informs registered listeners of changed values.
     */
    void updatePlatformDataEntriesProto(
            @NonNull Map<PlatformDataKey<?>, DynamicDataValue> newData) {
        Map<PlatformDataKey<?>, DynamicDataValue> changedEntries = new ArrayMap<>();
        for (Map.Entry<PlatformDataKey<?>, DynamicDataValue> newEntry : newData.entrySet()) {
            DynamicDataValue currentEntry = mCurrentPlatformData.get(newEntry.getKey());
            if (currentEntry == null || !currentEntry.equals(newEntry.getValue())) {
                changedEntries.put(newEntry.getKey(), newEntry.getValue());
            }
        }

        for (Map.Entry<PlatformDataKey<?>, DynamicDataValue> entry : changedEntries.entrySet()) {
            for (DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> callback :
                    mRegisteredCallbacks.getOrDefault(entry.getKey(), Collections.emptySet())) {
                callback.onPreUpdate();
            }
        }

        for (Map.Entry<PlatformDataKey<?>, DynamicDataValue> entry : changedEntries.entrySet()) {
            for (DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> callback :
                    mRegisteredCallbacks.getOrDefault(entry.getKey(), Collections.emptySet())) {
                callback.onData(entry.getValue());
            }
            mCurrentPlatformData.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Remove the platform data item with the given key.
     *
     * <p>Informs registered listeners by invalidating removed values.
     */
    void removePlatformDataEntries(@NonNull Set<PlatformDataKey<?>> keys) {
        for (PlatformDataKey<?> key : keys) {
            if (mCurrentPlatformData.get(key) != null) {
                for (DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> callback :
                        mRegisteredCallbacks.getOrDefault(key, Collections.emptySet())) {
                    callback.onPreUpdate();
                }
            }
        }

        for (PlatformDataKey<?> key : keys) {
            if (mCurrentPlatformData.get(key) != null) {
                for (DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> callback :
                        mRegisteredCallbacks.getOrDefault(key, Collections.emptySet())) {
                    callback.onInvalidated();
                }
                mCurrentPlatformData.remove(key);
            }
        }
    }

    /** Gets dynamic value with the given {@code key}. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @UiThread
    @Nullable
    @Override
    public DynamicDataValue getDynamicDataValuesProto(@NonNull DynamicDataKey<?> key) {
        return mCurrentPlatformData.get(key);
    }

    /**
     * Registers the given callback for updates to the data item for the given {@code key}.
     *
     * <p>Note that the callback will be executed on the UI thread.
     */
    @UiThread
    @Override
    void registerCallback(
            @NonNull DynamicDataKey<?> key,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> callback) {
        if (!(key instanceof PlatformDataKey)) {
            return;
        }

        // The key has callback previously, then the provider receiver already set.
        if (mRegisteredCallbacks.containsKey(key) && !mRegisteredCallbacks.get(key).isEmpty()) {
            mRegisteredCallbacks.get(key).add(callback);
            return;
        }

        PlatformDataProvider platformDataProvider = mSourceKeyToDataProviders.get(key);
        if (platformDataProvider == null) {
            Log.w(TAG, String.format("No platform data provider for %s.", key));
            return;
        }

        // There is other key from the provider has callback registered, so the provider receiver
        // already set
        if (hasRegisteredCallback(platformDataProvider)) {
            mRegisteredCallbacks.computeIfAbsent(key, k -> new ArraySet<>()).add(callback);
            return;
        }

        mRegisteredCallbacks.computeIfAbsent(key, k -> new ArraySet<>()).add(callback);
        // Set receiver to the provider
        platformDataProvider.setReceiver(
                mUiExecutor,
                new PlatformDataReceiver() {
                    @Override
                    public void onData(@NonNull PlatformDataValues newData) {
                        updatePlatformDataEntries(newData);
                    }

                    @Override
                    public void onInvalidated(@NonNull Set<PlatformDataKey<?>> keys) {
                        removePlatformDataEntries(keys);
                    }
                });
    }

    /** Unregisters the callback for the given {@code key} from receiving the updates. */
    @UiThread
    @Override
    void unregisterCallback(
            @NonNull DynamicDataKey<?> key,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> callback) {
        Set<DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue>> callbackSet =
                mRegisteredCallbacks.get(key);

        if (callbackSet == null) {
            return;
        }

        callbackSet.remove(callback);

        if (!(key instanceof PlatformDataKey) || !callbackSet.isEmpty()) {
            return;
        }

        PlatformDataProvider platformDataProvider = mSourceKeyToDataProviders.get(key);
        if (platformDataProvider == null) {
            Log.w(TAG, String.format("No platform data provider for %s", key));
            return;
        }

        if (!hasRegisteredCallback(platformDataProvider)) {
            platformDataProvider.clearReceiver();
        }
    }

    // check whether any key from the provider has registered callback
    private boolean hasRegisteredCallback(PlatformDataProvider provider) {
        return mSourceKeyToDataProviders.entrySet().stream().anyMatch(
                entry -> Objects.equals(entry.getValue(), provider)
                        && !mRegisteredCallbacks.getOrDefault(
                        entry.getKey(), Collections.emptySet()).isEmpty());
    }
}
