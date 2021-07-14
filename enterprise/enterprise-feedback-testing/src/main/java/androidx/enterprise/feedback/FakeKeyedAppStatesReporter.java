/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.enterprise.feedback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A fake {@link KeyedAppStatesReporter} for testing.
 *
 * <p>Example usage:
 * <pre>{@code
 *   FakeKeyedAppStatesReporter reporter = new FakeKeyedAppStatesReporter();
 *   // Inject the reporter to the part of your code it will be used.
 *   assertThat(reporter.getKeyedAppStatesByKey().get("myKey").getMessage()).isEqualTo("expected");
 * }</pre>
 */
public class FakeKeyedAppStatesReporter extends KeyedAppStatesReporter {

    // States are stored in both a List and a Map (rather than using Map#values) to ensure that
    // order and duplicates are preserved.
    private List<KeyedAppState> mKeyedAppStates =
            Collections.synchronizedList(new ArrayList<KeyedAppState>());
    private List<KeyedAppState> mOnDeviceKeyedAppStates =
            Collections.synchronizedList(new ArrayList<KeyedAppState>());
    private List<KeyedAppState> mUploadedKeyedAppStates =
            Collections.synchronizedList(new ArrayList<KeyedAppState>());
    private Map<String, KeyedAppState> mKeyedAppStatesByKey =
            Collections.synchronizedMap(new HashMap<String, KeyedAppState>());
    private Map<String, KeyedAppState> mOnDeviceKeyedAppStatesByKey =
            Collections.synchronizedMap(new HashMap<String, KeyedAppState>());
    private Map<String, KeyedAppState> mUploadedKeyedAppStatesByKey =
            Collections.synchronizedMap(new HashMap<String, KeyedAppState>());
    private AtomicInteger mNumberOfUploads = new AtomicInteger();

    /** @deprecated see {@link #setStates(Collection, KeyedAppStatesCallback)}. **/
    @Override
    @Deprecated
    public void setStates(@NonNull Collection<KeyedAppState> states) {
        setStates(states, /* callback= */ null);
    }

    /**
     * Record the states set.
     *
     * <p>Does not enforce any limit on total size of states collection.
     */
    @Override
    public void setStates(@NonNull Collection<KeyedAppState> states,
            @Nullable KeyedAppStatesCallback callback) {
        for (KeyedAppState state : states) {
            mOnDeviceKeyedAppStates.add(state);
            mOnDeviceKeyedAppStatesByKey.put(state.getKey(), state);
            mKeyedAppStates.add(state);
            mKeyedAppStatesByKey.put(state.getKey(), state);
        }
        if (callback != null) {
            callback.onResult(KeyedAppStatesCallback.STATUS_SUCCESS, /* throwable= */ null);
        }
    }

    /** @deprecated See {@link #setStatesImmediate(Collection, KeyedAppStatesCallback)}. **/
    @Override
    @Deprecated
    public void setStatesImmediate(@NonNull Collection<KeyedAppState> states) {
        setStatesImmediate(states, /* callback= */ null);
    }

    /**
     * Record the set states and immediately mark all states as having been uploaded.
     *
     * <p>Does not enforce any quota on uploading, or limit on total size of states collection.
     */
    @Override
    public void setStatesImmediate(@NonNull Collection<KeyedAppState> states,
            @Nullable KeyedAppStatesCallback callback) {
        setStates(states, callback);
        upload();
    }

    private void upload() {
        for (KeyedAppState state : mOnDeviceKeyedAppStates) {
            mUploadedKeyedAppStates.add(state);
            mUploadedKeyedAppStatesByKey.put(state.getKey(), state);
        }
        mOnDeviceKeyedAppStates.clear();
        mOnDeviceKeyedAppStatesByKey.clear();
        mNumberOfUploads.addAndGet(1);
    }

    /**
     * Get a list of all {@link KeyedAppState} instances that have been set.
     *
     * <p>This is in the order that they were set, and may contain multiple with the same key, if
     * that key has been set twice.
     */
    @NonNull
    public List<KeyedAppState> getKeyedAppStates() {
        return new ArrayList<>(mKeyedAppStates);
    }

    /**
     * Get a map of the latest {@link KeyedAppState} set for each key.
     */
    @NonNull
    public Map<String, KeyedAppState> getKeyedAppStatesByKey() {
        return new HashMap<>(mKeyedAppStatesByKey);
    }

    /**
     * Get a list of {@link KeyedAppState} instances that have been set but not yet uploaded.
     *
     * <p>This is in the order that they were set, and may contain multiple with the same key, if
     * that key has been set twice.
     *
     * <p>Once uploaded (using {@link #setStatesImmediate(Collection)}) instances will no longer be
     * returned by this method.
     */
    @NonNull
    public List<KeyedAppState> getOnDeviceKeyedAppStates() {
        return new ArrayList<>(mOnDeviceKeyedAppStates);
    }

    /**
     * Get a map of the latest {@link KeyedAppState} set for each key that has not yet uploaded.
     *
     * <p>Once uploaded (using {@link #setStatesImmediate(Collection)}) instances will no longer be
     * returned by this method.
     */
    @NonNull
    public Map<String, KeyedAppState> getOnDeviceKeyedAppStatesByKey() {
        return new HashMap<>(mOnDeviceKeyedAppStatesByKey);
    }

    /**
     * Get a list of {@link KeyedAppState} instances that have been set and uploaded.
     *
     * <p>This is in the order that they were set, and may contain multiple with the same key, if
     * that key has been set twice.
     *
     * <p>States will be returned by this method if they were set using
     * {@link #setStatesImmediate(Collection)} or if {@link #setStatesImmediate(Collection)} has
     * been called since they were set.
     */
    @NonNull
    public List<KeyedAppState> getUploadedKeyedAppStates() {
        return new ArrayList<>(mUploadedKeyedAppStates);
    }

    /**
     * Get a list of the latest {@link KeyedAppState} set for each key that has been uploaded.
     *
     * <p>This is in the order that they were set, and may contain multiple with the same key, if
     * that key has been set twice.
     *
     * <p>States will be returned by this method if they were set using
     * {@link #setStatesImmediate(Collection)} or if {@link #setStatesImmediate(Collection)} has
     * been called since they were set.
     */
    @NonNull
    public Map<String, KeyedAppState> getUploadedKeyedAppStatesByKey() {
        return new HashMap<>(mUploadedKeyedAppStatesByKey);
    }

    /**
     * Get the number of times {@link #setStatesImmediate(Collection)} has been called.
     */
    public int getNumberOfUploads() {
        return mNumberOfUploads.get();
    }
}
