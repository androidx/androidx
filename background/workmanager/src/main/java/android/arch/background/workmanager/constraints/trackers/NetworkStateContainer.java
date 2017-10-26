/*
 * Copyright 2017 The Android Open Source Project
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

package android.arch.background.workmanager.constraints.trackers;

import android.arch.background.workmanager.constraints.NetworkState;
import android.arch.background.workmanager.constraints.listeners.NetworkStateListener;
import android.support.annotation.NonNull;

/**
 * Manages a {@link NetworkState} with helper functions for notifying {@link NetworkStateListener}s
 * if the {@link NetworkStateContainer#mState} is updated.
 */

class NetworkStateContainer {
    private NetworkState mState;

    NetworkStateContainer(@NonNull NetworkState networkState) {
        mState = networkState;
    }

    NetworkState getState() {
        return mState;
    }

    void setStateAndNotify(@NonNull NetworkState networkState,
                           @NonNull Iterable<NetworkStateListener> listeners) {
        if (mState == null || !mState.equals(networkState)) {
            mState = networkState;
            for (NetworkStateListener listener : listeners) {
                listener.setNetworkState(mState);
            }
        }
    }
}
