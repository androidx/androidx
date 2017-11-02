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

package android.arch.background.workmanager.firebase;

import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.model.Constraints;
import android.arch.background.workmanager.model.ContentUriTriggers;
import android.arch.background.workmanager.model.WorkSpec;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobTrigger;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.ObservedUri;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Converts a {@link WorkSpec} into a {@link Job}.
 */

class FirebaseJobConverter {
    private static final String TAG = "FirebaseJobConverter";
    private FirebaseJobDispatcher mDispatcher;

    FirebaseJobConverter(FirebaseJobDispatcher dispatcher) {
        mDispatcher = dispatcher;
    }

    // TODO(xbhatnag): Add initial delay and periodic
    Job convert(WorkSpec workSpec) {
        return mDispatcher.newJobBuilder()
                .setService(FirebaseJobService.class)
                .setTag(workSpec.getId())
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setRetryStrategy(createRetryStrategy(workSpec))
                .setConstraints(getConstraints(workSpec))
                .setTrigger(createTrigger(workSpec))
                .build();
    }

    private JobTrigger createTrigger(WorkSpec workSpec) {
        if (workSpec.getConstraints().hasContentUriTriggers()) {
            List<ObservedUri> observedUris = new ArrayList<>();
            ContentUriTriggers triggers = workSpec.getConstraints().getContentUriTriggers();
            for (ContentUriTriggers.Trigger trigger : triggers) {
                observedUris.add(convertContentUriTrigger(trigger));
            }
            return Trigger.contentUriTrigger(observedUris);
        } else {
            return Trigger.NOW;
        }
    }

    private RetryStrategy createRetryStrategy(WorkSpec workSpec) {
        int policy = workSpec.getBackoffPolicy() == Work.BACKOFF_POLICY_LINEAR
                ? RetryStrategy.RETRY_POLICY_LINEAR : RetryStrategy.RETRY_POLICY_EXPONENTIAL;
        int initialBackoff = (int) TimeUnit.SECONDS
                .convert(workSpec.getBackoffDelayDuration(), TimeUnit.MILLISECONDS);
        int maxBackoff = (int) TimeUnit.SECONDS
                .convert(Work.MAX_BACKOFF_DURATION, TimeUnit.MILLISECONDS);
        return mDispatcher.newRetryStrategy(policy, initialBackoff, maxBackoff);
    }

    private static ObservedUri convertContentUriTrigger(
            ContentUriTriggers.Trigger trigger) {
        int flag = trigger.shouldTriggerForDescendants()
                ? ObservedUri.Flags.FLAG_NOTIFY_FOR_DESCENDANTS : 0;
        return new ObservedUri(trigger.getUri(), flag);
    }

    private int[] getConstraints(WorkSpec workSpec) {
        // TODO(xbhatnag): Do not ignore constraints. Consider fallback to AlarmManager?
        Constraints constraints = workSpec.getConstraints();
        List<Integer> mConstraints = new ArrayList<>();

        if (constraints.requiresDeviceIdle()) {
            mConstraints.add(Constraint.DEVICE_IDLE);
        }

        if (constraints.requiresCharging()) {
            mConstraints.add(Constraint.DEVICE_CHARGING);
        }

        if (constraints.requiresBatteryNotLow()) {
            Log.w(TAG, "Battery Not Low is not a supported constraint "
                    + "with FirebaseJobDisaptcher");
        }

        if (constraints.requiresStorageNotLow()) {
            Log.w(TAG, "Storage Not Low is not a supported constraint "
                    + "with FirebaseJobDisaptcher");
        }

        switch (constraints.getRequiredNetworkType()) {
            case Constraints.NETWORK_TYPE_METERED:
                Log.w(TAG, "Metered Network is not a supported constraint with "
                        + "FirebaseJobDispatcher. Falling back to Any Network constraint.");
                mConstraints.add(Constraint.ON_ANY_NETWORK);
                break;
            case Constraints.NETWORK_TYPE_NOT_ROAMING:
                Log.w(TAG, "Not Roaming Network is not a supported constraint with "
                        + "FirebaseJobDispatcher. Falling back to Any Network constraint.");
                mConstraints.add(Constraint.ON_ANY_NETWORK);
                break;
            case Constraints.NETWORK_TYPE_ANY:
                mConstraints.add(Constraint.ON_ANY_NETWORK);
                break;
            case Constraints.NETWORK_TYPE_UNMETERED:
                mConstraints.add(Constraint.ON_UNMETERED_NETWORK);
                break;
        }

        return toIntArray(mConstraints);
    }

    private int[] toIntArray(List<Integer> integers) {
        int size = integers.size();
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = integers.get(i);
        }
        return array;
    }
}
