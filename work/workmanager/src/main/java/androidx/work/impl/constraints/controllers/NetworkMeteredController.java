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

package androidx.work.impl.constraints.controllers;

import static androidx.work.NetworkType.METERED;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.impl.constraints.NetworkState;
import androidx.work.impl.constraints.trackers.Trackers;
import androidx.work.impl.model.WorkSpec;

/**
 * A {@link ConstraintController} for monitoring that the network connection is metered.
 */

public class NetworkMeteredController extends ConstraintController<NetworkState> {
    private static final String TAG = "NetworkMeteredCtrlr";

    public NetworkMeteredController(Context context, OnConstraintUpdatedCallback callback) {
        super(Trackers.getInstance(context).getNetworkStateTracker(), callback);
    }

    @Override
    boolean hasConstraint(@NonNull WorkSpec workSpec) {
        return workSpec.constraints.getRequiredNetworkType() == METERED;
    }

    /**
     * Check for metered constraint on API 26+, when JobInfo#NETWORK_METERED was added, to
     * be consistent with JobScheduler functionality.
     */
    @Override
    boolean isConstrained(@NonNull NetworkState state) {
        if (Build.VERSION.SDK_INT < 26) {
            Log.d(TAG, "Metered network constraint is not supported before API 26, "
                    + "only checking for connected state.");
            return !state.isConnected();
        }
        return !state.isConnected() || !state.isMetered();
    }
}
