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

import static androidx.work.NetworkType.TEMPORARILY_UNMETERED;
import static androidx.work.NetworkType.UNMETERED;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.impl.constraints.NetworkState;
import androidx.work.impl.constraints.trackers.Trackers;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

/**
 * A {@link ConstraintController} for monitoring that the network connection is unmetered.
 */

public class NetworkUnmeteredController extends ConstraintController<NetworkState> {
    public NetworkUnmeteredController(
            @NonNull Context context,
            @NonNull TaskExecutor taskExecutor) {
        super(Trackers.getInstance(context, taskExecutor).getNetworkStateTracker());
    }

    @Override
    boolean hasConstraint(@NonNull WorkSpec workSpec) {
        return workSpec.constraints.getRequiredNetworkType() == UNMETERED
                || (Build.VERSION.SDK_INT >= 30
                && workSpec.constraints.getRequiredNetworkType() == TEMPORARILY_UNMETERED);
    }

    @Override
    boolean isConstrained(@NonNull NetworkState state) {
        return !state.isConnected() || state.isMetered();
    }
}
