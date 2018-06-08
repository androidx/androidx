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

package androidx.work.impl.background.systemjob;

import static android.support.annotation.VisibleForTesting.PACKAGE_PRIVATE;

import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ContentUriTriggers;
import androidx.work.NetworkType;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;

/**
 * Converts a {@link WorkSpec} into a JobInfo.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(api = WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL)
class SystemJobInfoConverter {
    private static final String TAG = "SystemJobInfoConverter";

    static final String EXTRA_WORK_SPEC_ID = "EXTRA_WORK_SPEC_ID";
    static final String EXTRA_IS_PERIODIC = "EXTRA_IS_PERIODIC";

    private final ComponentName mWorkServiceComponent;

    @VisibleForTesting(otherwise = PACKAGE_PRIVATE)
    SystemJobInfoConverter(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        mWorkServiceComponent = new ComponentName(appContext, SystemJobService.class);
    }

    /**
     * Converts a {@link WorkSpec} into a {@link JobInfo}.
     *
     * Note: All {@link JobInfo} are set to persist on reboot.
     *
     * @param workSpec The {@link WorkSpec} to convert
     * @param jobId The {@code jobId} to use. This is useful when de-duping jobs on reschedule.
     * @return The {@link JobInfo} representing the same information as the {@link WorkSpec}
     */
    JobInfo convert(WorkSpec workSpec, int jobId) {
        Constraints constraints = workSpec.constraints;
        // TODO(janclarin): Support newer required network types if unsupported by API version.
        int jobInfoNetworkType = convertNetworkType(constraints.getRequiredNetworkType());
        PersistableBundle extras = new PersistableBundle();
        extras.putString(EXTRA_WORK_SPEC_ID, workSpec.id);
        extras.putBoolean(EXTRA_IS_PERIODIC, workSpec.isPeriodic());
        JobInfo.Builder builder = new JobInfo.Builder(jobId, mWorkServiceComponent)
                .setRequiredNetworkType(jobInfoNetworkType)
                .setRequiresCharging(constraints.requiresCharging())
                .setRequiresDeviceIdle(constraints.requiresDeviceIdle())
                .setExtras(extras);

        if (!constraints.requiresDeviceIdle()) {
            // Device Idle and Backoff Criteria cannot be set together
            int backoffPolicy = workSpec.backoffPolicy == BackoffPolicy.LINEAR
                    ? JobInfo.BACKOFF_POLICY_LINEAR : JobInfo.BACKOFF_POLICY_EXPONENTIAL;
            builder.setBackoffCriteria(workSpec.backoffDelayDuration, backoffPolicy);
        }

        if (workSpec.isPeriodic()) {
            if (Build.VERSION.SDK_INT >= 24) {
                builder.setPeriodic(workSpec.intervalDuration, workSpec.flexDuration);
            } else {
                Log.d(TAG,
                        "Flex duration is currently not supported before API 24. Ignoring.");
                builder.setPeriodic(workSpec.intervalDuration);
            }
        } else {
            // Even if a WorkRequest has no constraints, setMinimumLatency(0) still needs to be
            // called due to an issue in JobInfo.Builder#build and JobInfo with no constraints. See
            // b/67716867.
            builder.setMinimumLatency(workSpec.initialDelay);
        }

        if (Build.VERSION.SDK_INT >= 24 && constraints.hasContentUriTriggers()) {
            for (ContentUriTriggers.Trigger trigger : constraints.getContentUriTriggers()) {
                builder.addTriggerContentUri(convertContentUriTrigger(trigger));
            }
        }

        // We don't want to persist these jobs because we reschedule these jobs on BOOT_COMPLETED.
        // That way ForceStopRunnable correctly reschedules Jobs when necessary.
        builder.setPersisted(false);
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
     * Converts {@link NetworkType} into {@link JobInfo}'s network values.
     *
     * @param networkType The {@link NetworkType} network type
     * @return The {@link JobInfo} network type
     */
    static int convertNetworkType(NetworkType networkType) {
        switch(networkType) {
            case NOT_REQUIRED:
                return JobInfo.NETWORK_TYPE_NONE;
            case CONNECTED:
                return JobInfo.NETWORK_TYPE_ANY;
            case UNMETERED:
                return JobInfo.NETWORK_TYPE_UNMETERED;
            case NOT_ROAMING:
                if (Build.VERSION.SDK_INT >= 24) {
                    return JobInfo.NETWORK_TYPE_NOT_ROAMING;
                }
                break;
            case METERED:
                if (Build.VERSION.SDK_INT >= 26) {
                    return JobInfo.NETWORK_TYPE_METERED;
                }
                break;
        }
        Log.d(TAG, String.format(
                "API version too low. Cannot convert network type value %s", networkType));
        return JobInfo.NETWORK_TYPE_ANY;
    }
}
