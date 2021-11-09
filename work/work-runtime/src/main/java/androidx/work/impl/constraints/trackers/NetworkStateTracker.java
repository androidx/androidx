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
package androidx.work.impl.constraints.trackers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.net.ConnectivityManagerCompat;
import androidx.work.Logger;
import androidx.work.impl.constraints.NetworkState;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

/**
 * A {@link ConstraintTracker} for monitoring network state.
 * <p>
 * For API 24 and up: Network state is tracked using a registered {@link NetworkCallback} with
 * {@link ConnectivityManager#registerDefaultNetworkCallback(NetworkCallback)}, added in API 24.
 * <p>
 * For API 23 and below: Network state is tracked using a {@link android.content.BroadcastReceiver}.
 * Much less efficient than tracking with {@link NetworkCallback}s and {@link ConnectivityManager}.
 * <p>
 * Based on {@link android.app.job.JobScheduler}'s ConnectivityController on API 26.
 * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/services/core/java/com/android/server/job/controllers/ConnectivityController.java}
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class NetworkStateTracker extends ConstraintTracker<NetworkState> {

    // Synthetic Accessor
    static final String TAG = Logger.tagWithPrefix("NetworkStateTracker");

    private final ConnectivityManager mConnectivityManager;

    @RequiresApi(24)
    private NetworkStateCallback mNetworkCallback;
    private NetworkStateBroadcastReceiver mBroadcastReceiver;

    /**
     * Create an instance of {@link NetworkStateTracker}
     * @param context the application {@link Context}
     * @param taskExecutor The internal {@link TaskExecutor} being used by WorkManager.
     */
    public NetworkStateTracker(@NonNull Context context, @NonNull TaskExecutor taskExecutor) {
        super(context, taskExecutor);
        mConnectivityManager =
                (ConnectivityManager) mAppContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (isNetworkCallbackSupported()) {
            mNetworkCallback = new NetworkStateCallback();
        } else {
            mBroadcastReceiver = new NetworkStateBroadcastReceiver();
        }
    }

    @Override
    public NetworkState getInitialState() {
        return getActiveNetworkState();
    }

    @Override
    public void startTracking() {
        if (isNetworkCallbackSupported()) {
            try {
                Logger.get().debug(TAG, "Registering network callback");
                mConnectivityManager.registerDefaultNetworkCallback(mNetworkCallback);
            } catch (IllegalArgumentException | SecurityException e) {
                // Catching the exceptions since and moving on - this tracker is only used for
                // GreedyScheduler and there is nothing to be done about device-specific bugs.
                // IllegalStateException: Happening on NVIDIA Shield K1 Tablets.  See b/136569342.
                // SecurityException: Happening on Solone W1450.  See b/153246136.
                Logger.get().error(
                        TAG,
                        "Received exception while registering network callback",
                        e);
            }
        } else {
            Logger.get().debug(TAG, "Registering broadcast receiver");
            mAppContext.registerReceiver(mBroadcastReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    @Override
    public void stopTracking() {
        if (isNetworkCallbackSupported()) {
            try {
                Logger.get().debug(TAG, "Unregistering network callback");
                mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
            } catch (IllegalArgumentException | SecurityException e) {
                // Catching the exceptions since and moving on - this tracker is only used for
                // GreedyScheduler and there is nothing to be done about device-specific bugs.
                // IllegalStateException: Happening on NVIDIA Shield K1 Tablets.  See b/136569342.
                // SecurityException: Happening on Solone W1450.  See b/153246136.
                Logger.get().error(
                        TAG,
                        "Received exception while unregistering network callback",
                        e);
            }
        } else {
            Logger.get().debug(TAG, "Unregistering broadcast receiver");
            mAppContext.unregisterReceiver(mBroadcastReceiver);
        }
    }

    private static boolean isNetworkCallbackSupported() {
        // Based on requiring ConnectivityManager#registerDefaultNetworkCallback - added in API 24.
        return Build.VERSION.SDK_INT >= 24;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    NetworkState getActiveNetworkState() {
        // Use getActiveNetworkInfo() instead of getNetworkInfo(network) because it can detect VPNs.
        NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
        boolean isConnected = info != null && info.isConnected();
        boolean isValidated = isActiveNetworkValidated();
        boolean isMetered = ConnectivityManagerCompat.isActiveNetworkMetered(mConnectivityManager);
        boolean isNotRoaming = info != null && !info.isRoaming();
        return new NetworkState(isConnected, isValidated, isMetered, isNotRoaming);
    }

    @VisibleForTesting
    boolean isActiveNetworkValidated() {
        if (Build.VERSION.SDK_INT < 23) {
            return false; // NET_CAPABILITY_VALIDATED not available until API 23. Used on API 26+.
        }
        try {
            Network network = mConnectivityManager.getActiveNetwork();
            NetworkCapabilities capabilities = mConnectivityManager.getNetworkCapabilities(network);
            return capabilities != null
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (SecurityException exception) {
            // b/163342798
            Logger.get().error(TAG, "Unable to validate active network", exception);
            return false;
        }
    }

    @RequiresApi(24)
    private class NetworkStateCallback extends NetworkCallback {
        NetworkStateCallback() {
        }

        @Override
        public void onCapabilitiesChanged(
                @NonNull Network network, @NonNull NetworkCapabilities capabilities) {
            // The Network parameter is unreliable when a VPN app is running - use active network.
            Logger.get().debug(
                    TAG,
                    "Network capabilities changed: " + capabilities);
            setState(getActiveNetworkState());
        }

        @Override
        public void onLost(@NonNull Network network) {
            Logger.get().debug(TAG, "Network connection lost");
            setState(getActiveNetworkState());
        }
    }

    private class NetworkStateBroadcastReceiver extends BroadcastReceiver {
        NetworkStateBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                Logger.get().debug(TAG, "Network broadcast received");
                setState(getActiveNetworkState());
            }
        }
    }
}
