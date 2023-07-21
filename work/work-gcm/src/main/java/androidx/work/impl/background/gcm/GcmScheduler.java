/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.impl.background.gcm;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Clock;
import androidx.work.Logger;
import androidx.work.impl.Scheduler;
import androidx.work.impl.model.WorkSpec;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.Task;

/**
 * The {@link androidx.work.WorkManager} scheduler which uses
 * {@link com.google.android.gms.gcm.GcmNetworkManager}.
 */
public class GcmScheduler implements Scheduler {
    private static final String TAG = Logger.tagWithPrefix("GcmScheduler");

    private final GcmNetworkManager mNetworkManager;
    private final GcmTaskConverter mTaskConverter;

    public GcmScheduler(@NonNull Context context, @NonNull Clock clock) {
        boolean isPlayServicesAvailable = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
        if (!isPlayServicesAvailable) {
            throw new IllegalStateException("Google Play Services not available");
        }
        mNetworkManager = GcmNetworkManager.getInstance(context);
        mTaskConverter = new GcmTaskConverter(clock);
    }

    @Override
    public void schedule(@NonNull WorkSpec... workSpecs) {
        for (WorkSpec workSpec : workSpecs) {
            Task task = mTaskConverter.convert(workSpec);
            Logger.get().debug(TAG, "Scheduling " + workSpec + "with " + task);
            mNetworkManager.schedule(task);
        }
    }

    @Override
    public void cancel(@NonNull String workSpecId) {
        Logger.get().debug(TAG, "Cancelling " + workSpecId);
        mNetworkManager.cancelTask(workSpecId, WorkManagerGcmService.class);
    }

    @Override
    public boolean hasLimitedSchedulingSlots() {
        return true;
    }
}
