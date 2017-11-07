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
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.model.Constraints;
import android.arch.background.workmanager.model.ContentUriTriggers;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.utils.IdGenerator;
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
@RequiresApi(api = 23)
class SystemJobInfoConverter {
    private static final String TAG = "SystemJobInfoConverter";

    static final String EXTRA_WORK_SPEC_ID = "EXTRA_WORK_SPEC_ID";
    static final String EXTRA_IS_PERIODIC = "EXTRA_IS_PERIODIC";

    private final ComponentName mWorkServiceComponent;
    private final IdGenerator mIdGenerator;

    /**
     * Constructs a {@link IdGenerator}.
     *
     * @param context A non-null {@link Context}.
     */
    SystemJobInfoConverter(@NonNull Context context) {
        this(context, new IdGenerator(context));
    }

    @VisibleForTesting(otherwise = PACKAGE_PRIVATE)
    SystemJobInfoConverter(@NonNull Context context, IdGenerator idGenerator) {
        Context appContext = context.getApplicationContext();
        mWorkServiceComponent = new ComponentName(appContext, SystemJobService.class);
        mIdGenerator = idGenerator;
    }

    /**
     * Converts a {@link WorkSpec} into a {@link JobInfo}.
     *
     * Note: All {@link JobInfo} are set to persist on reboot.
     *
     * @param workSpec The {@link WorkSpec} to convert
     * @return The {@link JobInfo} representing the same information as the {@link WorkSpec}
     */
    JobInfo convert(WorkSpec workSpec) {
        Constraints constraints = workSpec.getConstraints();
        int jobId = mIdGenerator.nextJobSchedulerId();
        // TODO(janclarin): Support newer required network types if unsupported by API version.
        int jobInfoNetworkType = convertNetworkType(constraints.getRequiredNetworkType());
        PersistableBundle extras = new PersistableBundle();
        extras.putString(EXTRA_WORK_SPEC_ID, workSpec.getId());
        extras.putBoolean(EXTRA_IS_PERIODIC, workSpec.isPeriodic());
        JobInfo.Builder builder = new JobInfo.Builder(jobId, mWorkServiceComponent)
                .setRequiredNetworkType(jobInfoNetworkType)
                .setRequiresCharging(constraints.requiresCharging())
                .setRequiresDeviceIdle(constraints.requiresDeviceIdle())
                .setExtras(extras);

        if (!constraints.requiresDeviceIdle()) {
            // Device Idle and Backoff Criteria cannot be set together
            int backoffPolicy = workSpec.getBackoffPolicy() == Work.BACKOFF_POLICY_LINEAR
                    ? JobInfo.BACKOFF_POLICY_LINEAR : JobInfo.BACKOFF_POLICY_EXPONENTIAL;
            builder.setBackoffCriteria(workSpec.getBackoffDelayDuration(), backoffPolicy);
        }

        if (workSpec.isPeriodic()) {
            builder = setBuilderPeriodic(builder, workSpec);
        } else {
            // Even if a Work has no constraints, setMinimumLatency(0) still needs to be called due
            // to an issue in JobInfo.Builder#build and JobInfo with no constraints. See b/67716867.
            builder.setMinimumLatency(workSpec.getInitialDelay());
        }

        if (Build.VERSION.SDK_INT >= 24 && constraints.hasContentUriTriggers()) {
            for (ContentUriTriggers.Trigger trigger : constraints.getContentUriTriggers()) {
                builder.addTriggerContentUri(convertContentUriTrigger(trigger));
            }
        } else {
            // Jobs with Content Uri Triggers cannot be persisted
            builder.setPersisted(true);
        }

        // TODO(janclarin): Support requires[Battery|Storage]NotLow for versions older than 26.
        if (Build.VERSION.SDK_INT >= 26) {
            builder.setRequiresBatteryNotLow(constraints.requiresBatteryNotLow());
            builder.setRequiresStorageNotLow(constraints.requiresStorageNotLow());
        }
        return builder.build();
    }

    @RequiresApi(24)
    private static JobInfo.TriggerContentUri convertContentUriTrigger(
            ContentUriTriggers.Trigger trigger) {
        int flag = trigger.shouldTriggerForDescendants()
                ? JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS : 0;
        return new JobInfo.TriggerContentUri(trigger.getUri(), flag);
    }

    /**
     * Converts {@link Constraints.NetworkType} into {@link JobInfo}'s network values.
     *
     * @param networkType The {@link Constraints.NetworkType} network type
     * @return The {@link JobInfo} network type
     */
    static int convertNetworkType(@Constraints.NetworkType int networkType) {
        switch(networkType) {
            case Constraints.NETWORK_TYPE_NONE:
                return JobInfo.NETWORK_TYPE_NONE;
            case Constraints.NETWORK_TYPE_ANY:
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
        Log.d(TAG, "API version too low. Cannot convert network type value " + networkType);
        return JobInfo.NETWORK_TYPE_ANY;
    }

    private static JobInfo.Builder setBuilderPeriodic(JobInfo.Builder builder, WorkSpec workSpec) {
        long intervalDuration = workSpec.getIntervalDuration();
        long flexDuration = workSpec.getFlexDuration();

        if (Build.VERSION.SDK_INT >= 24) {
            builder.setPeriodic(intervalDuration, flexDuration);
        } else {
            // TODO(janclarin): Support flex for JobScheduler before API 24.
            Log.d(TAG, "Flex duration is currently not supported before API 24. Ignoring.");
            builder.setPeriodic(intervalDuration);
        }
        return builder;
    }
}
