/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.support.annotation.RequiresApi;
import android.util.Log;

/**
 * A {@link ConstraintTracker} for monitoring network state with {@link ConnectivityManager}. This
 * is heavily based on how {@link android.app.job.JobScheduler}'s ConnectivityController monitors
 * network connectivity in API 26.
 * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/services/core/java/com/android/server/job/controllers/ConnectivityController.java}
 *
 * <p>
 * This approach works on API 24+ because
 * {@link ConnectivityManager#registerDefaultNetworkCallback(NetworkCallback)} was added in API 24.
 * This allows us to receive network state via {@link NetworkCallback} for the system default
 * network.
 * </p>
 */
@RequiresApi(24)
public class ConnectivityManagerNetworkStateTracker
        extends ConstraintTracker<NetworkStateListener> {
    private static final String TAG = "ConnManagerNetwrkTrcker";

    private final ConnectivityManager mConnectivityManager;
    private final NetworkCallback mNetworkCallback = new NetworkCallback() {
        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
            Log.d(TAG, "Network connection capability changed to: " + capabilities);
            // ConnectivityManager.getActiveNetworkInfo() is used instead of getNetworkInfo(network)
            // because the network parameter may not be usable when a VPN app is running.
            NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
            NetworkState updatedState = NetworkState.create(info, capabilities);
            mNetworkStateContainer.setStateAndNotify(updatedState, mListeners);
        }

        @Override
        public void onLost(Network network) {
            Log.d(TAG, "Network connection lost.");
            mNetworkStateContainer.setStateAndNotify(getActiveNetworkState(), mListeners);
        }
    };

    private NetworkStateContainer mNetworkStateContainer;

    ConnectivityManagerNetworkStateTracker(Context context) {
        super(context);
        mConnectivityManager = mAppContext.getSystemService(ConnectivityManager.class);
    }

    @Override
    public void setUpInitialState(NetworkStateListener listener) {
        if (mNetworkStateContainer == null) {
            mNetworkStateContainer = new NetworkStateContainer(getActiveNetworkState());
        }
        NetworkState currentState = mNetworkStateContainer.getState();
        if (currentState != null) {
            listener.setNetworkState(currentState);
        }
    }

    @Override
    public void startTracking() {
        mConnectivityManager.registerDefaultNetworkCallback(mNetworkCallback);
    }

    @Override
    public void stopTracking() {
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
    }

    private NetworkState getActiveNetworkState() {
        NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
        NetworkCapabilities capabilities = mConnectivityManager
                .getNetworkCapabilities(mConnectivityManager.getActiveNetwork());
        return NetworkState.create(info, capabilities);
    }
}
