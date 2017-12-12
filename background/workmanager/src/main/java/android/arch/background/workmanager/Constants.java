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
package android.arch.background.workmanager;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;

/**
 * Constants for {@link WorkManager} object construction.
 */

public interface Constants {

    @Retention(SOURCE)
    @IntDef({STATUS_ENQUEUED, STATUS_RUNNING, STATUS_SUCCEEDED, STATUS_FAILED, STATUS_BLOCKED,
            STATUS_CANCELLED})
    @interface WorkStatus {
    }

    @Retention(SOURCE)
    @IntDef({BACKOFF_POLICY_EXPONENTIAL, BACKOFF_POLICY_LINEAR})
    @interface BackoffPolicy {
    }

    int STATUS_ENQUEUED = 0;
    int STATUS_RUNNING = 1;
    int STATUS_SUCCEEDED = 2;
    int STATUS_FAILED = 3;
    int STATUS_BLOCKED = 4;
    int STATUS_CANCELLED = 5;

    int BACKOFF_POLICY_EXPONENTIAL = 0;
    int BACKOFF_POLICY_LINEAR = 1;
    long DEFAULT_BACKOFF_DELAY_MILLIS = 30000L;

    /**
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/core/java/android/app/job/JobInfo.java#82}
     */
    long MAX_BACKOFF_MILLIS = 5 * 60 * 60 * 1000; // 5 hours.

    /**
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/core/java/android/app/job/JobInfo.java#119}
     */
    long MIN_BACKOFF_MILLIS = 10 * 1000; // 10 seconds.

    /**
     * The minimum interval duration for {@link PeriodicWork}, in milliseconds.
     * Based on {@see https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/job/JobInfo.java#110}.
     */
    long MIN_PERIODIC_INTERVAL_MILLIS = 15 * 60 * 1000L; // 15 minutes.

    /**
     * The minimum flex duration for {@link PeriodicWork}, in milliseconds.
     * Based on {@see https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/job/JobInfo.java#113}.
     */
    long MIN_PERIODIC_FLEX_MILLIS = 5 * 60 * 1000L; // 5 minutes.
}
