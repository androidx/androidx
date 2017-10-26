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

package android.arch.background.workmanager.constraints;

import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.VisibleForTesting;

/**
 * Stores information about network state.
 */

public class NetworkState {

    private boolean mIsUsable;
    private boolean mIsMetered;
    private boolean mIsUnmetered;
    private boolean mIsNotRoaming;

    @VisibleForTesting
    public NetworkState(boolean isUsable, boolean isMetered, boolean isUnmetered,
                        boolean isNotRoaming) {
        mIsUsable = isUsable;
        mIsMetered = isMetered;
        mIsUnmetered = isUnmetered;
        mIsNotRoaming = isNotRoaming;
    }

    /**
     * Creates an instance of {@link NetworkState} based on network {@code info} and
     * {@code capabilities} based on the device's {@link Build.VERSION#SDK_INT}.
     *
     * @param info The {@link NetworkInfo} of the network to infer the state from.
     * @param capabilities The {@link NetworkCapabilities} of the network to infer the state from.
     * @return A {@link NetworkState} that represents the state of a network.
     */
    @RequiresApi(21)
    public static NetworkState create(NetworkInfo info, NetworkCapabilities capabilities) {
        if (Build.VERSION.SDK_INT >= 26) {
            return createApi26(info, capabilities);
        } else if (Build.VERSION.SDK_INT >= 24) {
            return createApi24(info, capabilities);
        }
        // TODO(janclarin): Support creating NetworkState for 21-23.
        return null;
    }

    /**
     * Determines if the network is usable. That is, the device should at least be connected to a
     * network. This should be used to determine if the {@link NetworkState} satisfies
     * {@link android.arch.background.workmanager.model.Constraints#NETWORK_TYPE_ANY}.
     *
     * @return {@code true} if the network is usable.
     */
    public boolean isUsable() {
        return mIsUsable;
    }

    /**
     * Determines if the network is metered.
     *
     * @return {@code true} if the network is metered.
     */
    public boolean isMetered() {
        return mIsMetered;
    }

    /**
     * Determines if the network is unmetered.
     *
     * @return {@code true} if the network is unmetered.
     */
    public boolean isUnmetered() {
        return mIsUnmetered;
    }

    /**
     * Determines if the network is not roaming.
     *
     * @return {@code true} if the network is not roaming.
     */
    public boolean isNotRoaming() {
        return mIsNotRoaming;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NetworkState other = (NetworkState) o;
        return this.mIsUsable == other.mIsUsable
                && this.mIsMetered == other.mIsMetered
                && this.mIsUnmetered == other.mIsUnmetered
                && this.mIsNotRoaming == other.mIsNotRoaming;
    }

    @Override
    public int hashCode() {
        int result = 0x0000;
        if (mIsUsable) result += 0x0001;
        if (mIsMetered) result += 0x0010;
        if (mIsUnmetered) result += 0x0100;
        if (mIsNotRoaming) result += 0x1000;
        return result;
    }

    /**
     * Based on the network constraint logic in API 25 {@link android.app.job.JobScheduler}'s
     * ConnectivityController#updateConstraintsSatisfied(JobStatus, NetworkCapabilities).
     * {@see https://android.googlesource.com/platform/frameworks/base/+/nougat-release/services/core/java/com/android/server/job/controllers/ConnectivityController.java#100}
     *
     * The only difference from the API 25 {@link android.app.job.JobScheduler} implementation is
     * that {@link NetworkState#mIsUsable} means that the network is simply connected, but may
     * <strong>not</strong> validated to have a working Internet connection. This is consistent with
     * API 24-25 behavior.
     * */
    @RequiresApi(24)
    private static NetworkState createApi24(NetworkInfo info, NetworkCapabilities capabilities) {
        boolean connected = info != null && info.isConnected();
        boolean metered = connected && capabilities != null && isMetered(capabilities);
        boolean unmetered = connected && capabilities != null && !isMetered(capabilities);
        boolean notRoaming = connected && !info.isRoaming();
        return new NetworkState(connected, metered, unmetered, notRoaming);
    }

    /**
     * Based on the network constraint logic in API 26 {@link android.app.job.JobScheduler}'s
     * ConnectivityController#updateConstraintsSatisfied(JobStatus, NetworkCapabilities).
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/services/core/java/com/android/server/job/controllers/ConnectivityController.java#102}
     *
     * Note: Metered/unmetered/notRoaming rely on {@code connected} instead of {@code usable} to
     * support private networks that don't verify internet connection.
     */
    @RequiresApi(26)
    private static NetworkState createApi26(NetworkInfo info, NetworkCapabilities capabilities) {
        boolean validated = capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        boolean connected = info != null && info.isConnected();
        boolean usable = connected && validated;
        boolean metered = connected && capabilities != null && isMetered(capabilities);
        boolean unmetered = connected && capabilities != null && !isMetered(capabilities);
        boolean notRoaming = connected && !info.isRoaming();
        return new NetworkState(usable, metered, unmetered, notRoaming);
    }

    @RequiresApi(21)
    private static boolean isMetered(NetworkCapabilities capabilities) {
        return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
    }
}
