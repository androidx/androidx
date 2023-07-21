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

import static java.util.stream.Collectors.toMap;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicDataBuilders;
import androidx.wear.protolayout.expression.DynamicDataKey;
import androidx.wear.protolayout.expression.proto.DynamicDataProto.DynamicDataValue;
import androidx.wear.protolayout.expression.PlatformDataKey;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/**
 * State storage for ProtoLayout, which also supports sending callback when data items change.
 *
 * <p>Note that this class is **not** thread-safe. Since ProtoLayout inflation currently happens on
 * the main thread, and because updates will eventually affect the main thread, this whole class
 * must only be used from the UI thread.
 */
public class StateStore {
    /**
     * Maximum number for state entries allowed for this {@link StateStore}.
     *
     * <p>The ProtoLayout state model is not designed to handle large volumes of layout provided
     * state. So we limit the number of state entries to keep the on-the-wire size and state
     * store update times manageable.
     */
    @SuppressLint("MinMaxConstant")
    public static final int MAX_STATE_ENTRY_COUNT = 100;

    private final Executor mUiExecutor;
    @NonNull private final Map<AppDataKey<?>, DynamicDataValue> mCurrentAppState
            = new ArrayMap<>();

    @NonNull
    private final Map<PlatformDataKey<?>, DynamicDataValue> mCurrentPlatformData
            = new ArrayMap<>();

    @NonNull
    private final
    Map<DynamicDataKey<?>,
            Set<DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue>>>
            mRegisteredCallbacks = new ArrayMap<>();

    @NonNull
    private final Map<PlatformDataKey<?>, PlatformDataProvider>
            mSourceKeyToDataProviders = new ArrayMap<>();

    @NonNull
    private final Map<PlatformDataProvider, Integer> mProviderToRegisteredKeyCount
            = new ArrayMap<>();

    /**
     * Creates a {@link StateStore}.
     *
     * @throws IllegalStateException if number of initialState entries is greater than
     * {@link StateStore#MAX_STATE_ENTRY_COUNT}.
     */
    @NonNull
    public static StateStore create(
            @NonNull Map<AppDataKey<?>, DynamicDataBuilders.DynamicDataValue>
                    initialState) {
        return new StateStore(toProto(initialState));
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public StateStore(
            @NonNull Map<AppDataKey<?>, DynamicDataValue> initialState) {
        if (initialState.size() > MAX_STATE_ENTRY_COUNT) {
            throw stateTooLargeException(initialState.size());
        }
        mCurrentAppState.putAll(initialState);
        mUiExecutor = new MainThreadExecutor(new Handler(Looper.getMainLooper()));
    }

    void putAllPlatformProviders(
            @NonNull Map<PlatformDataKey<?>, PlatformDataProvider> sourceKeyToDataProviders) {
        mSourceKeyToDataProviders.putAll(sourceKeyToDataProviders);
    }

    /**
     * Sets the given app state, replacing the current app state.
     *
     * <p>Informs registered listeners of changed values, invalidates removed values.
     *
     * @throws IllegalStateException if number of state entries is greater than
     * {@link StateStore#MAX_STATE_ENTRY_COUNT}. The state will not update and old state entries
     * will stay in place.
     */
    @UiThread
    public void setAppStateEntryValues(
            @NonNull Map<AppDataKey<?>, DynamicDataBuilders.DynamicDataValue> newState) {
        setAppStateEntryValuesProto(toProto(newState));
    }

    /**
     * Sets the given app state, replacing the current app state.
     *
     * <p>Informs registered listeners of changed values, invalidates removed values.
     *
     * @throws IllegalStateException if number of state entries is larger than
     * {@link StateStore#MAX_STATE_ENTRY_COUNT}. The state will not update and old state entries
     * will stay in place.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @UiThread
    public void setAppStateEntryValuesProto(
            @NonNull Map<AppDataKey<?>, DynamicDataValue> newState) {
        if (newState.size() > MAX_STATE_ENTRY_COUNT) {
            throw stateTooLargeException(newState.size());
        }

        // Figure out which nodes have actually changed.
        Set<AppDataKey<?>> removedKeys = getRemovedAppKeys(newState);
        Map<AppDataKey<?>, DynamicDataValue> changedEntries = getChangedAppEntries(newState);

        Stream.concat(removedKeys.stream(), changedEntries.keySet().stream())
                .forEach(
                        key -> {
                            for (DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> callback :
                                    mRegisteredCallbacks.getOrDefault(
                                            key, Collections.emptySet())) {
                                callback.onPreUpdate();
                            }
                        });

        mCurrentAppState.clear();
        mCurrentAppState.putAll(newState);

        for (AppDataKey<?> key : removedKeys) {
            for (DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> callback :
                    mRegisteredCallbacks.getOrDefault(key, Collections.emptySet())) {
                callback.onInvalidated();
            }
        }
        for (Entry<AppDataKey<?>, DynamicDataValue> entry
                : changedEntries.entrySet()) {
            for (DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> callback :
                    mRegisteredCallbacks.getOrDefault(entry.getKey(), Collections.emptySet())) {
                callback.onData(entry.getValue());
            }
        }
    }

    /**
     * Update the given platform data item.
     *
     * <p>Informs registered listeners of changed values.
     */
    void updatePlatformDataEntries(
            @NonNull Map<PlatformDataKey<?>, DynamicDataBuilders.DynamicDataValue> newData) {
        updatePlatformDataEntryProto(
                newData.entrySet().stream().collect(
                        toMap(Entry::getKey, entry -> entry.getValue().toDynamicDataValueProto()))
        );
    }

    /**
     * Update the given platform data item.
     *
     * <p>Informs registered listeners of changed values.
     */
    void updatePlatformDataEntryProto(
            @NonNull Map<PlatformDataKey<?>, DynamicDataValue> newData) {
        Map<PlatformDataKey<?>, DynamicDataValue> changedEntries = new ArrayMap<>();
        for (Entry<PlatformDataKey<?>, DynamicDataValue> newEntry : newData.entrySet()) {
            DynamicDataValue currentEntry = mCurrentPlatformData.get(newEntry.getKey());
            if (currentEntry == null || !currentEntry.equals(newEntry.getValue())) {
                changedEntries.put(newEntry.getKey(), newEntry.getValue());
            }
        }

        for (Entry<PlatformDataKey<?>, DynamicDataValue> entry : changedEntries.entrySet()) {
            for (DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> callback :
                    mRegisteredCallbacks.getOrDefault(entry.getKey(), Collections.emptySet())) {
                callback.onPreUpdate();
            }
        }

        for (Entry<PlatformDataKey<?>, DynamicDataValue> entry : changedEntries.entrySet()) {
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
    void removePlatformDataEntry(@NonNull Set<PlatformDataKey<?>> keys) {
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
    public DynamicDataValue getDynamicDataValuesProto(@NonNull DynamicDataKey<?> key) {
        if (key instanceof AppDataKey) {
            return mCurrentAppState.get(key);
        }

        if (key instanceof PlatformDataKey) {
            return mCurrentPlatformData.get(key);
        }

        return null;
    }

    /**
     * Registers the given callback for updates to the data item for the given {@code key}.
     *
     * <p>Note that the callback will be executed on the UI thread.
     */
    @UiThread
    void registerCallback(
            @NonNull DynamicDataKey<?> key,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> callback) {
        mRegisteredCallbacks.computeIfAbsent(key, k -> new ArraySet<>()).add(callback);

        if (!(key instanceof PlatformDataKey) ||
                (mRegisteredCallbacks.containsKey(key) && mRegisteredCallbacks.get(key).size() > 1)
        ) {
            return;
        }

        PlatformDataProvider platformDataProvider = mSourceKeyToDataProviders.get(key);
        if (platformDataProvider != null) {
            int registeredKeyCount =
                    mProviderToRegisteredKeyCount.getOrDefault(platformDataProvider, 0);

            if (registeredKeyCount == 0) {
                platformDataProvider.registerForData(
                        mUiExecutor,
                        new PlatformDataReceiver() {
                            @Override
                            public void onData(
                                    @NonNull
                                    Map<PlatformDataKey<?>, DynamicDataBuilders.DynamicDataValue>
                                            newData) {
                                updatePlatformDataEntries(newData);
                            }

                            @Override
                            public void onInvalidated(@NonNull Set<PlatformDataKey<?>> keys) {
                                removePlatformDataEntry(keys);
                            }
                        });
            }

            mProviderToRegisteredKeyCount.put(platformDataProvider, registeredKeyCount + 1);
        } else {
            throw new IllegalArgumentException(
                    String.format("No platform data provider for %s", key));
        }
    }

    /** Unregisters the callback for the given {@code key} from receiving the updates. */
    @UiThread
    void unregisterCallback(
            @NonNull DynamicDataKey<?> key,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> callback) {

        Set<DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue>> callbackSet =
                mRegisteredCallbacks.get(key);
        if (callbackSet != null) {
            callbackSet.remove(callback);

            if (!(key instanceof PlatformDataKey) || !callbackSet.isEmpty()) {
                return;
            }

            PlatformDataProvider platformDataProvider = mSourceKeyToDataProviders.get(key);
            if (platformDataProvider != null) {
                int registeredKeyCount =
                        mProviderToRegisteredKeyCount.getOrDefault(platformDataProvider, 0);
                if (registeredKeyCount == 1) {
                    platformDataProvider.unregisterForData();
                }
                mProviderToRegisteredKeyCount.put(platformDataProvider, registeredKeyCount - 1);
            } else {
                throw new IllegalArgumentException(
                        String.format("No platform data provider for %s", key));
            }
        }
    }

    @NonNull
    private static Map<AppDataKey<?>, DynamicDataValue> toProto(
            @NonNull Map<AppDataKey<?>, DynamicDataBuilders.DynamicDataValue> value) {
        return value.entrySet().stream()
                .collect(toMap(Entry::getKey, entry -> entry.getValue().toDynamicDataValueProto()));
    }

    @NonNull
    private Set<AppDataKey<?>> getRemovedAppKeys(
            @NonNull Map<AppDataKey<?>, DynamicDataValue> newState) {
        Set<AppDataKey<?>> result = new ArraySet<>(mCurrentAppState.keySet());
        result.removeAll(newState.keySet());
        return result;
    }

    @NonNull
    private Map<AppDataKey<?>, DynamicDataValue> getChangedAppEntries(
            @NonNull Map<AppDataKey<?>, DynamicDataValue> newState) {
        Map<AppDataKey<?>, DynamicDataValue> result = new ArrayMap<>();
        for (Entry<AppDataKey<?>, DynamicDataValue> newEntry
                : newState.entrySet()) {
            DynamicDataValue currentEntry = mCurrentAppState.get(newEntry.getKey());
            if (currentEntry == null || !currentEntry.equals(newEntry.getValue())) {
                result.put(newEntry.getKey(), newEntry.getValue());
            }
        }
        return result;
    }

    static IllegalStateException stateTooLargeException(int stateSize) {
        return new IllegalStateException(
                String.format(
                        "Too many state entries: %d. The maximum number of allowed state entries "
                                + "is %d.",
                        stateSize, MAX_STATE_ENTRY_COUNT));
    }
}
