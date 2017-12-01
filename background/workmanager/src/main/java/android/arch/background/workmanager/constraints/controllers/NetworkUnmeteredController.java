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
import android.arch.background.workmanager.constraints.trackers.Trackers;
import android.arch.background.workmanager.model.Constraints;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.support.annotation.NonNull;

/**
 * A {@link ConstraintController} for monitoring that the network connection is unmetered.
 */

public class NetworkUnmeteredController extends ConstraintController<NetworkState> {

    private boolean mIsConnectedAndUnmetered;

    public NetworkUnmeteredController(
            Context context,
            WorkDatabase workDatabase,
            LifecycleOwner lifecycleOwner,
            OnConstraintUpdatedCallback onConstraintUpdatedCallback,
            boolean allowPeriodic) {
        super(
                workDatabase.workSpecDao().getIdsForNetworkTypeController(
                        Constraints.NETWORK_UNMETERED,
                        allowPeriodic),
                lifecycleOwner,
                Trackers.getInstance(context).getNetworkStateTracker(),
                onConstraintUpdatedCallback
        );
    }

    @Override
    boolean isConstrained() {
        return !mIsConnectedAndUnmetered;
    }

    @Override
    public void onConstraintChanged(@NonNull NetworkState state) {
        mIsConnectedAndUnmetered = state.isConnected() && !state.isMetered();
        updateListener();
    }
}
