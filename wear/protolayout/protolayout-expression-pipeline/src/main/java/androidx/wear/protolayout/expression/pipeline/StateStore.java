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

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

/**
 * State storage for ProtoLayout, which also supports sending callback when data items change.
 *
 * <p>Note that this class is **not** thread-safe. Since ProtoLayout inflation currently happens on
 * the main thread, and because updates will eventually affect the main thread, this whole class
 * must only be used from the UI thread.
 */
public final class StateStore extends DataStore {
    @SuppressLint("MinMaxConstant")
    private static final int MAX_STATE_ENTRY_COUNT = 30;

    private static final String TAG = "ProtoLayoutStateStore";

    @NonNull private final Map<AppDataKey<?>, DynamicDataValue> mCurrentAppState
            = new ArrayMap<>();

    @NonNull
    private final
    Map<DynamicDataKey<?>,
            Set<DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue>>>
            mRegisteredCallbacks = new ArrayMap<>();

    /**
     * Creates a {@link StateStore}.
     *
     * @throws IllegalStateException if number of initialState entries is greater than
     * {@link StateStore#getMaxStateEntryCount()}.
     */
    @NonNull
    public static StateStore create(
            @NonNull Map<AppDataKey<?>, DynamicDataBuilders.DynamicDataValue<?>>
                    initialState) {
        return new StateStore(toProto(initialState));
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public StateStore(
            @NonNull Map<AppDataKey<?>, DynamicDataValue> initialState) {
        if (initialState.size() > getMaxStateEntryCount()) {
            throw stateTooLargeException(initialState.size());
        }
        mCurrentAppState.putAll(initialState);
    }

    /**
     * Sets the given app state, replacing the current app state.
     *
     * <p>Informs registered listeners of changed values, invalidates removed values.
     *
     * @throws IllegalStateException if number of state entries is greater than
     * {@link StateStore#getMaxStateEntryCount()}. The state will not update and old state entries
     * will stay in place.
     */
    @UiThread
    public void setAppStateEntryValues(
            @NonNull Map<AppDataKey<?>, DynamicDataBuilders.DynamicDataValue<?>> newState) {
        setAppStateEntryValuesProto(toProto(newState));
    }

    /**
     * Sets the given app state, replacing the current app state.
     *
     * <p>Informs registered listeners of changed values, invalidates removed values.
     *
     * @throws IllegalStateException if number of state entries is larger than
     * {@link StateStore#getMaxStateEntryCount()}. The state will not update and old state entries
     * will stay in place.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @UiThread
    public void setAppStateEntryValuesProto(
            @NonNull Map<AppDataKey<?>, DynamicDataValue> newState) {
        if (newState.size() > getMaxStateEntryCount()) {
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

    /** Gets dynamic value with the given {@code key}. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @UiThread
    @Nullable
    @Override
    public DynamicDataValue getDynamicDataValuesProto(@NonNull DynamicDataKey<?> key) {
        return mCurrentAppState.get(key);
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
        mRegisteredCallbacks.computeIfAbsent(key, k -> new ArraySet<>()).add(callback);
    }

    /** Unregisters the callback for the given {@code key} from receiving the updates. */
    @UiThread
    @Override
    void unregisterCallback(
            @NonNull DynamicDataKey<?> key,
            @NonNull DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue> callback) {

        Set<DynamicTypeValueReceiverWithPreUpdate<DynamicDataValue>> callbackSet =
                mRegisteredCallbacks.get(key);
        if (callbackSet != null) {
            callbackSet.remove(callback);
        }
    }

    /**
     * Returns the maximum number for state entries allowed for this {@link StateStore}.
     *
     * <p>The ProtoLayout state model is not designed to handle large volumes of layout provided
     * state. So we limit the number of state entries to keep the on-the-wire size and state
     * store update times manageable.
     */
    public static int getMaxStateEntryCount(){
        return MAX_STATE_ENTRY_COUNT;
    }

    @NonNull
    private static Map<AppDataKey<?>, DynamicDataValue> toProto(
            @NonNull Map<AppDataKey<?>, DynamicDataBuilders.DynamicDataValue<?>> value) {
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
                        stateSize, getMaxStateEntryCount()));
    }
}
