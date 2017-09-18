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

package android.arch.background.workmanager;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;

/**
 * The database entity that stores information about one {@link Worker}.
 */

@Entity
@TypeConverters(Arguments.class)
public class WorkItem {
    @Retention(SOURCE)
    @IntDef({STATUS_FAILED, STATUS_RUNNING, STATUS_SUCCEEDED, STATUS_ENQUEUED})
    @interface WorkStatus {
    }

    @Retention(SOURCE)
    @IntDef({BACKOFF_POLICY_EXPONENTIAL, BACKOFF_POLICY_LINEAR})
    @interface BackoffPolicy {
    }

    static final int STATUS_ENQUEUED = 0;
    static final int STATUS_RUNNING = 1;
    static final int STATUS_SUCCEEDED = 2;
    static final int STATUS_FAILED = 3;

    public static final int BACKOFF_POLICY_EXPONENTIAL = 0;
    public static final int BACKOFF_POLICY_LINEAR = 1;
    public static final long DEFAULT_BACKOFF_DELAY_DURATION = 30000;

    @ColumnInfo(name = "id")
    @PrimaryKey
    String mId;

    @ColumnInfo(name = "workspec_id")
    String mWorkSpecId;

    @ColumnInfo(name = "status")
    @WorkStatus
    int mStatus = STATUS_ENQUEUED;

    @ColumnInfo(name = "worker_class_name")
    String mWorkerClassName;

    @Embedded
    Constraints mConstraints = new Constraints.Builder().build();

    Arguments mArguments = new Arguments();

    // TODO(sumir): Should Backoff be disabled by default?
    @ColumnInfo(name = "backoff_policy")
    @BackoffPolicy
    int mBackoffPolicy = BACKOFF_POLICY_EXPONENTIAL;

    @ColumnInfo(name = "backoff_delay_duration")
    long mBackoffDelayDuration = DEFAULT_BACKOFF_DELAY_DURATION;

    WorkItem(String id, String workSpecId) {
        mId = id;
        mWorkSpecId = workSpecId;
    }
}
