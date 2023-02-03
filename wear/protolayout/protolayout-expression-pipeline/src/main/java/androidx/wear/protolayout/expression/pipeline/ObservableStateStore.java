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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.wear.protolayout.expression.StateEntryBuilders;
import androidx.wear.protolayout.expression.proto.StateEntryProto.StateEntryValue;

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
public class ObservableStateStore {
    @NonNull
    private final Map<String, StateEntryValue> mCurrentState = new ArrayMap<>();

    @NonNull
    private final Map<String, Set<DynamicTypeValueReceiver<StateEntryValue>>> mRegisteredCallbacks =
            new ArrayMap<>();

    /** Creates a {@link ObservableStateStore}. */
    @NonNull
    public static ObservableStateStore create(
            @NonNull Map<String, StateEntryBuilders.StateEntryValue> initialState) {
        return new ObservableStateStore(toProto(initialState));
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public ObservableStateStore(@NonNull Map<String, StateEntryValue> initialState) {
        mCurrentState.putAll(initialState);
    }

    /**
     * Sets the given state, replacing the current state.
     *
     * <p>Informs registered listeners of changed values, invalidates removed values.
     */
    @UiThread
    public void setStateEntryValues(
            @NonNull Map<String, StateEntryBuilders.StateEntryValue> newState) {
        setStateEntryValuesProto(toProto(newState));
    }

    /**
     * Sets the given state, replacing the current state.
     *
     * <p>Informs registered listeners of changed values, invalidates removed values.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @UiThread
    public void setStateEntryValuesProto(@NonNull Map<String, StateEntryValue> newState) {
        // Figure out which nodes have actually changed.
        Set<String> removedKeys = getRemovedKeys(newState);
        Map<String, StateEntryValue> changedEntries = getChangedEntries(newState);

        Stream.concat(removedKeys.stream(), changedEntries.keySet().stream())
                .forEach(key -> {
                    for (DynamicTypeValueReceiver<StateEntryValue> callback :
                            mRegisteredCallbacks.getOrDefault(key, Collections.emptySet())) {
                        callback.onPreUpdate();
                    }
                });

        mCurrentState.clear();
        mCurrentState.putAll(newState);

        for (String key : removedKeys) {
            for (DynamicTypeValueReceiver<StateEntryValue> callback :
                    mRegisteredCallbacks.getOrDefault(key, Collections.emptySet())) {
                callback.onInvalidated();
            }
        }
        for (Entry<String, StateEntryValue> entry : changedEntries.entrySet()) {
            for (DynamicTypeValueReceiver<StateEntryValue> callback :
                    mRegisteredCallbacks.getOrDefault(entry.getKey(), Collections.emptySet())) {
                callback.onData(entry.getValue());
            }
        }
    }

    /**
     * Gets state with the given key.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @UiThread
    @Nullable
    public StateEntryValue getStateEntryValuesProto(@NonNull String key) {
        return mCurrentState.get(key);
    }

    /**
     * Registers the given callback for updates to the state for the given key.
     *
     * <p>Note that the callback will be executed on the UI thread.
     */
    @UiThread
    void registerCallback(
            @NonNull String key, @NonNull DynamicTypeValueReceiver<StateEntryValue> callback) {
        mRegisteredCallbacks.computeIfAbsent(key, k -> new ArraySet<>()).add(callback);
    }

    /** Unregisters from receiving the updates. */
    @UiThread
    void unregisterCallback(
            @NonNull String key, @NonNull DynamicTypeValueReceiver<StateEntryValue> callback) {
        Set<DynamicTypeValueReceiver<StateEntryValue>> callbackSet = mRegisteredCallbacks.get(key);
        if (callbackSet != null) {
            callbackSet.remove(callback);
        }
    }

    @NonNull
    private static Map<String, StateEntryValue> toProto(
            @NonNull Map<String, StateEntryBuilders.StateEntryValue> value) {
        return value.entrySet().stream()
                .collect(toMap(Entry::getKey, entry -> entry.getValue().toStateEntryValueProto()));
    }

    @NonNull
    private Set<String> getRemovedKeys(@NonNull Map<String, StateEntryValue> newState) {
        Set<String> result = new ArraySet<>(mCurrentState.keySet());
        result.removeAll(newState.keySet());
        return result;
    }

    @NonNull
    private Map<String, StateEntryValue> getChangedEntries(
            @NonNull Map<String, StateEntryValue> newState) {
        Map<String, StateEntryValue> result = new ArrayMap<>();
        for (Entry<String, StateEntryValue> newEntry : newState.entrySet()) {
            StateEntryValue currentEntry = mCurrentState.get(newEntry.getKey());
            if (currentEntry == null || !currentEntry.equals(newEntry.getValue())) {
                result.put(newEntry.getKey(), newEntry.getValue());
            }
        }
        return result;
    }
}
