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


import static androidx.work.NetworkType.TEMPORARILY_UNMETERED;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.work.Clock;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.impl.model.WorkSpec;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.Task;


/**
 * Converts a {@link androidx.work.impl.model.WorkSpec} to a {@link Task}.
 */
public class GcmTaskConverter {
    /**
     * This is referring to the size of the execution window in seconds. {@link GcmNetworkManager}
     * requires that we specify a window of time relative to {@code now} where a {@link Task}
     * is eligible to run. This window is
     * [{@code windowStart}, {@code windowStart + EXECUTION_WINDOW_SIZE_IN_SECONDS}) .
     *
     * {@code windowStart} is based on {@code workSpec.calculateNextRunTime()} .
     */
    @VisibleForTesting
    public static final long EXECUTION_WINDOW_SIZE_IN_SECONDS = 5L;

    static final String EXTRA_WORK_GENERATION = "androidx.work.impl.background.gcm.GENERATION";
    private final Clock mClock;

    public GcmTaskConverter(@NonNull Clock clock) {
        mClock = clock;
    }

    OneoffTask convert(@NonNull WorkSpec workSpec) {
        Bundle extras = new Bundle();
        extras.putInt(EXTRA_WORK_GENERATION, workSpec.getGeneration());
        OneoffTask.Builder builder = new OneoffTask.Builder();
        builder.setService(WorkManagerGcmService.class)
                .setTag(workSpec.id)
                .setUpdateCurrent(true)
                .setExtras(extras)
                .setPersisted(false);

        // Next run time is in seconds.
        long now = SECONDS.convert(now(), MILLISECONDS);
        long nextRunTimeInSeconds = SECONDS.convert(workSpec.calculateNextRunTime(), MILLISECONDS);
        // GCMNetworkManager needs the execution window to be relative from the present.
        long offset = Math.max(nextRunTimeInSeconds - now, 0);
        builder.setExecutionWindow(offset, offset + EXECUTION_WINDOW_SIZE_IN_SECONDS);

        applyConstraints(builder, workSpec);
        return builder.build();
    }


    /**
     * Returns the current time in milliseconds.
     */
    @VisibleForTesting
    public long now() {
        return mClock.currentTimeMillis();
    }

    private static Task.Builder applyConstraints(
            @NonNull Task.Builder builder,
            @NonNull WorkSpec workSpec) {

        // Apply defaults
        builder.setRequiresCharging(false);
        builder.setRequiredNetwork(Task.NETWORK_STATE_ANY);

        if (workSpec.hasConstraints()) {
            Constraints constraints = workSpec.constraints;

            // Network Constraints
            NetworkType networkType = constraints.getRequiredNetworkType();
            switch (networkType) {
                case METERED:
                case NOT_ROAMING:
                case CONNECTED:
                    builder.setRequiredNetwork(Task.NETWORK_STATE_CONNECTED);
                    break;
                case UNMETERED:
                    builder.setRequiredNetwork(Task.NETWORK_STATE_UNMETERED);
                    break;
                case NOT_REQUIRED:
                    builder.setRequiredNetwork(Task.NETWORK_STATE_ANY);
                    break;
                default:
                    if (Build.VERSION.SDK_INT >= 30) {
                        if (networkType == TEMPORARILY_UNMETERED) {
                            builder.setRequiredNetwork(Task.NETWORK_STATE_ANY);
                        }
                    }
            }

            // Charging constraints
            if (constraints.requiresCharging()) {
                builder.setRequiresCharging(true);
            } else {
                builder.setRequiresCharging(false);
            }

            // No direct support for requires battery not low, and requires storage not low.
            // Using ConstraintTrackingWorker for such use cases.

        }

        return builder;
    }
}
