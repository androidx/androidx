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

package android.arch.background.workmanager.constraints.controllers;

import android.arch.background.workmanager.WorkDatabase;
import android.arch.background.workmanager.constraints.NetworkState;
import android.arch.background.workmanager.constraints.listeners.NetworkStateListener;
import android.arch.background.workmanager.constraints.trackers.Trackers;
import android.arch.background.workmanager.model.Constraints;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * A {@link ConstraintController} for monitoring that the network connection is not roaming.
 */

public class NetworkStateNotRoamingController extends ConstraintController<NetworkStateListener> {
    private static final String TAG = "NetworkNotRoamingCtrlr";

    private boolean mIsConnected;
    private boolean mIsNotRoaming;
    private final NetworkStateListener mNetworkStateNotRoamingListener =
            new NetworkStateListener() {
                @Override
                public void setNetworkState(@NonNull NetworkState state) {
                    mIsConnected = state.isConnected();
                    mIsNotRoaming = state.isNotRoaming();
                    updateListener();
                }
            };

    public NetworkStateNotRoamingController(
            Context context,
            WorkDatabase workDatabase,
            LifecycleOwner lifecycleOwner,
            OnConstraintUpdatedCallback onConstraintUpdatedCallback,
            boolean allowPeriodic) {
        super(
                workDatabase.workSpecDao().getIdsForNetworkTypeController(
                        Constraints.NETWORK_TYPE_NOT_ROAMING,
                        allowPeriodic),
                lifecycleOwner,
                Trackers.getInstance(context).getNetworkStateTracker(),
                onConstraintUpdatedCallback
        );
    }

    @Override
    NetworkStateListener getListener() {
        return mNetworkStateNotRoamingListener;
    }

    /**
     * Check for not-roaming constraint on API 24+, when JobInfo#NETWORK_TYPE_NOT_ROAMING was added,
     * to be consistent with JobScheduler functionality.
     */
    @Override
    boolean isConstrained() {
        if (Build.VERSION.SDK_INT < 24) {
            Log.d(TAG, "Not-roaming network constraint is not supported before API 24, "
                    + "only checking for connected state.");
            return !mIsConnected;
        }
        return !mIsConnected || !mIsNotRoaming;
    }
}
