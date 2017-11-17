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
 * A {@link ConstraintController} for monitoring that the network connection is metered.
 */

public class NetworkMeteredController extends ConstraintController<NetworkStateListener> {
    private static final String TAG = "NetworkMeteredCtrlr";

    private boolean mIsConnected;
    private boolean mIsMetered;
    private final NetworkStateListener mNetworkStateMeteredListener = new NetworkStateListener() {
        @Override
        public void setNetworkState(@NonNull NetworkState state) {
            mIsConnected = state.isConnected();
            mIsMetered = state.isMetered();
            updateListener();
        }
    };

    public NetworkMeteredController(
            Context context,
            WorkDatabase workDatabase,
            LifecycleOwner lifecycleOwner,
            OnConstraintUpdatedCallback onConstraintUpdatedCallback,
            boolean allowPeriodic) {
        super(
                workDatabase.workSpecDao().getIdsForNetworkTypeController(
                        Constraints.NETWORK_METERED,
                        allowPeriodic),
                lifecycleOwner,
                Trackers.getInstance(context).getNetworkStateTracker(),
                onConstraintUpdatedCallback
        );
    }

    @Override
    NetworkStateListener getListener() {
        return mNetworkStateMeteredListener;
    }

    /**
     * Check for metered constraint on API 26+, when JobInfo#NETWORK_METERED was added, to
     * be consistent with JobScheduler functionality.
     */
    @Override
    boolean isConstrained() {
        if (Build.VERSION.SDK_INT < 26) {
            Log.d(TAG, "Metered network constraint is not supported before API 26, "
                    + "only checking for connected state.");
            return !mIsConnected;
        }
        return !mIsConnected || !mIsMetered;
    }
}
