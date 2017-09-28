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

package android.arch.background.workmanager.systemjob;

import static android.support.annotation.VisibleForTesting.PACKAGE_PRIVATE;

import android.app.job.JobInfo;
import android.arch.background.workmanager.WorkSpecConverter;
import android.arch.background.workmanager.model.Constraints;
import android.arch.background.workmanager.model.WorkSpec;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

/**
 * Converts a {@link WorkSpec} into a JobInfo.
 */
@RequiresApi(api = 21)
public class SystemJobConverter implements WorkSpecConverter<JobInfo> {
    private static final String TAG = "SystemJobConverter";
    @VisibleForTesting(otherwise = PACKAGE_PRIVATE)
    public static final String EXTRAS_WORK_SPEC_ID = "WORK_SPEC_ID";

    private final ComponentName mWorkServiceComponent;
    private final SystemJobIdGenerator mJobIdGenerator;

    /**
     * Constructs a {@link SystemJobIdGenerator}.
     *
     * @param context A non-null {@link Context}.
     */
    public SystemJobConverter(@NonNull Context context) {
        this(context, new SystemJobIdGenerator(context));
    }

    @VisibleForTesting(otherwise = PACKAGE_PRIVATE)
    public SystemJobConverter(@NonNull Context context, SystemJobIdGenerator jobIdGenerator) {
        Context appContext = context.getApplicationContext();
        mWorkServiceComponent = new ComponentName(appContext, SystemJobService.class);
        mJobIdGenerator = jobIdGenerator;
    }

    @Override
    public JobInfo convert(WorkSpec workSpec) {
        Constraints constraints = workSpec.getConstraints();
        int jobId = mJobIdGenerator.nextId();
        int jobNetworkType = convertNetworkType(constraints.getRequiredNetworkType());
        PersistableBundle extras = new PersistableBundle();
        extras.putString(EXTRAS_WORK_SPEC_ID, workSpec.getId());

        JobInfo.Builder builder =
                new JobInfo.Builder(jobId, mWorkServiceComponent)
                        .setMinimumLatency(constraints.getInitialDelay())
                        .setExtras(extras)
                        .setRequiredNetworkType(jobNetworkType)
                        .setRequiresCharging(constraints.requiresCharging())
                        .setRequiresDeviceIdle(constraints.requiresDeviceIdle());

        if (Build.VERSION.SDK_INT >= 26) {
            builder.setRequiresBatteryNotLow(constraints.requiresBatteryNotLow());
            builder.setRequiresStorageNotLow(constraints.requiresStorageNotLow());
        } else {
            // TODO(janclarin): Create compat version of batteryNotLow/storageNotLow constraints.
            Log.w(TAG, "Could not set requiresBatteryNowLow or requiresStorageNotLow constraints.");
        }
        return builder.build();
    }

    @Override
    public int convertNetworkType(@Constraints.NetworkType int networkType)
            throws IllegalArgumentException {
        switch(networkType) {
            case Constraints.NETWORK_TYPE_NONE:
                return JobInfo.NETWORK_TYPE_NONE;
            case Constraints.NETWORK_TYPE_CONNECTED:
                return JobInfo.NETWORK_TYPE_ANY;
            case Constraints.NETWORK_TYPE_UNMETERED:
                return JobInfo.NETWORK_TYPE_UNMETERED;
            case Constraints.NETWORK_TYPE_NOT_ROAMING:
                if (Build.VERSION.SDK_INT >= 24) {
                    return JobInfo.NETWORK_TYPE_NOT_ROAMING;
                }
                break;
            case Constraints.NETWORK_TYPE_METERED:
                if (Build.VERSION.SDK_INT >= 26) {
                    return JobInfo.NETWORK_TYPE_METERED;
                }
                break;
        }
        throw new IllegalArgumentException("NetworkType of " + networkType + " is not supported.");
    }
}
