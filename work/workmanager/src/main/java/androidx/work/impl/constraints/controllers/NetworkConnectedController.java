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

import static androidx.work.NetworkType.CONNECTED;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import androidx.work.impl.constraints.NetworkState;
import androidx.work.impl.constraints.trackers.Trackers;
import androidx.work.impl.model.WorkSpec;

/**
 * A {@link ConstraintController} for monitoring that any usable network connection is available.
 * <p>
 * For API 26 and above, usable means that the {@link NetworkState} is validated, i.e.
 * it has a working internet connection.
 * <p>
 * For API 25 and below, usable simply means that {@link NetworkState} is connected.
 */

public class NetworkConnectedController extends ConstraintController<NetworkState> {
    public NetworkConnectedController(Context context, OnConstraintUpdatedCallback callback) {
        super(Trackers.getInstance(context).getNetworkStateTracker(), callback);
    }

    @Override
    boolean hasConstraint(@NonNull WorkSpec workSpec) {
        return workSpec.constraints.getRequiredNetworkType() == CONNECTED;
    }

    @Override
    boolean isConstrained(@NonNull NetworkState state) {
        if (Build.VERSION.SDK_INT >= 26) {
            return !state.isConnected() || !state.isValidated();
        } else {
            return !state.isConnected();
        }
    }
}
