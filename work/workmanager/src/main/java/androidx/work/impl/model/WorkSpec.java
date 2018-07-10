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

package androidx.work.impl.model;

import static androidx.work.PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS;
import static androidx.work.PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS;
import static androidx.work.State.ENQUEUED;
import static androidx.work.WorkRequest.MAX_BACKOFF_MILLIS;
import static androidx.work.WorkRequest.MIN_BACKOFF_MILLIS;

import android.arch.core.util.Function;
import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Relation;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.State;
import androidx.work.WorkRequest;
import androidx.work.WorkStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stores information about a logical unit of work.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Entity(
        indices = {@Index(value = {"schedule_requested_at"})}
)
public class WorkSpec {
    private static final String TAG = "WorkSpec";
    public static final long SCHEDULE_NOT_REQUESTED_YET = -1;

    @ColumnInfo(name = "id")
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "state")
    @NonNull
    public State state = ENQUEUED;

    @ColumnInfo(name = "worker_class_name")
    @NonNull
    public String workerClassName;

    @ColumnInfo(name = "input_merger_class_name")
    public String inputMergerClassName;

    @ColumnInfo(name = "input")
    @NonNull
    public Data input = Data.EMPTY;

    @ColumnInfo(name = "output")
    @NonNull
    public Data output = Data.EMPTY;

    @ColumnInfo(name = "initial_delay")
    public long initialDelay;

    @ColumnInfo(name = "interval_duration")
    public long intervalDuration;

    @ColumnInfo(name = "flex_duration")
    public long flexDuration;

    @Embedded
    @NonNull
    public Constraints constraints = Constraints.NONE;

    @ColumnInfo(name = "run_attempt_count")
    public int runAttemptCount;

    @ColumnInfo(name = "backoff_policy")
    @NonNull
    public BackoffPolicy backoffPolicy = BackoffPolicy.EXPONENTIAL;

    @ColumnInfo(name = "backoff_delay_duration")
    public long backoffDelayDuration = WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS;

    /**
     * For one-off work, this is the time that the work was unblocked by prerequisites.
     * For periodic work, this is the time that the period started.
     */
    @ColumnInfo(name = "period_start_time")
    public long periodStartTime;

    @ColumnInfo(name = "minimum_retention_duration")
    public long minimumRetentionDuration;

    /**
     * This field tells us if this {@link WorkSpec} instance, is actually currently scheduled and
     * being counted against the {@code SCHEDULER_LIMIT}. This bit is reset for PeriodicWorkRequests
     * in API < 23, because AlarmManager does not know of PeriodicWorkRequests. So for the next
     * request to be rescheduled this field has to be reset to {@code SCHEDULE_NOT_REQUESTED_AT}.
     * For the JobScheduler implementation, we don't reset this field because JobScheduler natively
     * supports PeriodicWorkRequests.
     */
    @ColumnInfo(name = "schedule_requested_at")
    public long scheduleRequestedAt = SCHEDULE_NOT_REQUESTED_YET;

    public WorkSpec(@NonNull String id, @NonNull String workerClassName) {
        this.id = id;
        this.workerClassName = workerClassName;
    }

    /**
     * @param backoffDelayDuration The backoff delay duration in milliseconds
     */
    public void setBackoffDelayDuration(long backoffDelayDuration) {
        if (backoffDelayDuration > MAX_BACKOFF_MILLIS) {
            Log.w(TAG, "Backoff delay duration exceeds maximum value");
            backoffDelayDuration = MAX_BACKOFF_MILLIS;
        }
        if (backoffDelayDuration < MIN_BACKOFF_MILLIS) {
            Log.w(TAG, "Backoff delay duration less than minimum value");
            backoffDelayDuration = MIN_BACKOFF_MILLIS;
        }
        this.backoffDelayDuration = backoffDelayDuration;
    }


    public boolean isPeriodic() {
        return intervalDuration != 0L;
    }

    public boolean isBackedOff() {
        return state == ENQUEUED && runAttemptCount > 0;
    }

    /**
     * Sets the periodic interval for this unit of work.
     *
     * @param intervalDuration The interval in milliseconds
     */
    public void setPeriodic(long intervalDuration) {
        if (intervalDuration < MIN_PERIODIC_INTERVAL_MILLIS) {
            Log.w(TAG, String.format(
                    "Interval duration lesser than minimum allowed value; Changed to %s",
                    MIN_PERIODIC_INTERVAL_MILLIS));
            intervalDuration = MIN_PERIODIC_INTERVAL_MILLIS;
        }
        setPeriodic(intervalDuration, intervalDuration);
    }

    /**
     * Sets the periodic interval for this unit of work.
     *
     * @param intervalDuration The interval in milliseconds
     * @param flexDuration The flex duration in milliseconds
     */
    public void setPeriodic(long intervalDuration, long flexDuration) {
        if (intervalDuration < MIN_PERIODIC_INTERVAL_MILLIS) {
            Log.w(TAG, String.format(
                    "Interval duration lesser than minimum allowed value; Changed to %s",
                    MIN_PERIODIC_INTERVAL_MILLIS));
            intervalDuration = MIN_PERIODIC_INTERVAL_MILLIS;
        }
        if (flexDuration < MIN_PERIODIC_FLEX_MILLIS) {
            Log.w(TAG,
                    String.format("Flex duration lesser than minimum allowed value; Changed to %s",
                            MIN_PERIODIC_FLEX_MILLIS));
            flexDuration = MIN_PERIODIC_FLEX_MILLIS;
        }
        if (flexDuration > intervalDuration) {
            Log.w(TAG, String.format("Flex duration greater than interval duration; Changed to %s",
                    intervalDuration));
            flexDuration = intervalDuration;
        }
        this.intervalDuration = intervalDuration;
        this.flexDuration = flexDuration;
    }

    /**
     * Calculates the UTC time at which this {@link WorkSpec} should be allowed to run.
     * This method accounts for work that is backed off or periodic.
     *
     * If Backoff Policy is set to {@link BackoffPolicy#EXPONENTIAL}, then delay
     * increases at an exponential rate with respect to the run attempt count and is capped at
     * {@link WorkRequest#MAX_BACKOFF_MILLIS}.
     *
     * If Backoff Policy is set to {@link BackoffPolicy#LINEAR}, then delay
     * increases at an linear rate with respect to the run attempt count and is capped at
     * {@link WorkRequest#MAX_BACKOFF_MILLIS}.
     *
     * Based on {@see https://android.googlesource.com/platform/frameworks/base/+/master/services/core/java/com/android/server/job/JobSchedulerService.java#1125}
     *
     * Note that this runtime is for WorkManager internal use and may not match what the OS
     * considers to be the next runtime.
     *
     * For jobs with constraints, this represents the earliest time at which constraints
     * should be monitored for this work.
     *
     * For jobs without constraints, this represents the earliest time at which this work is
     * allowed to run.
     *
     * @return UTC time at which this {@link WorkSpec} should be allowed to run.
     */
    public long calculateNextRunTime() {
        if (isBackedOff()) {
            boolean isLinearBackoff = (backoffPolicy == BackoffPolicy.LINEAR);
            long delay = isLinearBackoff ? (backoffDelayDuration * runAttemptCount)
                    : (long) Math.scalb(backoffDelayDuration, runAttemptCount - 1);
            return periodStartTime + Math.min(WorkRequest.MAX_BACKOFF_MILLIS, delay);
        } else if (isPeriodic()) {
            return periodStartTime + intervalDuration - flexDuration;
        } else {
            return periodStartTime + initialDelay;
        }
    }

    /**
     * @return <code>true</code> if the {@link WorkSpec} has constraints.
     */
    public boolean hasConstraints() {
        return !Constraints.NONE.equals(constraints);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkSpec workSpec = (WorkSpec) o;

        if (initialDelay != workSpec.initialDelay) return false;
        if (intervalDuration != workSpec.intervalDuration) return false;
        if (flexDuration != workSpec.flexDuration) return false;
        if (runAttemptCount != workSpec.runAttemptCount) return false;
        if (backoffDelayDuration != workSpec.backoffDelayDuration) return false;
        if (periodStartTime != workSpec.periodStartTime) return false;
        if (minimumRetentionDuration != workSpec.minimumRetentionDuration) return false;
        if (scheduleRequestedAt != workSpec.scheduleRequestedAt) return false;
        if (!id.equals(workSpec.id)) return false;
        if (state != workSpec.state) return false;
        if (!workerClassName.equals(workSpec.workerClassName)) return false;
        if (inputMergerClassName != null ? !inputMergerClassName.equals(
                workSpec.inputMergerClassName)
                : workSpec.inputMergerClassName != null) {
            return false;
        }
        if (!input.equals(workSpec.input)) return false;
        if (!output.equals(workSpec.output)) return false;
        if (!constraints.equals(workSpec.constraints)) return false;
        return backoffPolicy == workSpec.backoffPolicy;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + state.hashCode();
        result = 31 * result + workerClassName.hashCode();
        result = 31 * result + (inputMergerClassName != null ? inputMergerClassName.hashCode() : 0);
        result = 31 * result + input.hashCode();
        result = 31 * result + output.hashCode();
        result = 31 * result + (int) (initialDelay ^ (initialDelay >>> 32));
        result = 31 * result + (int) (intervalDuration ^ (intervalDuration >>> 32));
        result = 31 * result + (int) (flexDuration ^ (flexDuration >>> 32));
        result = 31 * result + constraints.hashCode();
        result = 31 * result + runAttemptCount;
        result = 31 * result + backoffPolicy.hashCode();
        result = 31 * result + (int) (backoffDelayDuration ^ (backoffDelayDuration >>> 32));
        result = 31 * result + (int) (periodStartTime ^ (periodStartTime >>> 32));
        result = 31 * result + (int) (minimumRetentionDuration ^ (minimumRetentionDuration >>> 32));
        result = 31 * result + (int) (scheduleRequestedAt ^ (scheduleRequestedAt >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "{WorkSpec: " + id + "}";
    }

    /**
     * A POJO containing the ID and state of a WorkSpec.
     */
    public static class IdAndState {

        @ColumnInfo(name = "id")
        public String id;

        @ColumnInfo(name = "state")
        public State state;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IdAndState that = (IdAndState) o;

            if (state != that.state) return false;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + state.hashCode();
            return result;
        }
    }

    /**
     * A POJO containing the ID, state, output, and tags of a WorkSpec.
     */
    public static class WorkStatusPojo {

        @ColumnInfo(name = "id")
        public String id;

        @ColumnInfo(name = "state")
        public State state;

        @ColumnInfo(name = "output")
        public Data output;

        @Relation(
                parentColumn = "id",
                entityColumn = "work_spec_id",
                entity = WorkTag.class,
                projection = {"tag"})
        public List<String> tags;

        /**
         * Converts this POJO to a {@link WorkStatus}.
         *
         * @return The {@link WorkStatus} represented by this POJO
         */
        public WorkStatus toWorkStatus() {
            return new WorkStatus(UUID.fromString(id), state, output, tags);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            WorkStatusPojo that = (WorkStatusPojo) o;

            if (id != null ? !id.equals(that.id) : that.id != null) return false;
            if (state != that.state) return false;
            if (output != null ? !output.equals(that.output) : that.output != null) return false;
            return tags != null ? tags.equals(that.tags) : that.tags == null;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (state != null ? state.hashCode() : 0);
            result = 31 * result + (output != null ? output.hashCode() : 0);
            result = 31 * result + (tags != null ? tags.hashCode() : 0);
            return result;
        }
    }

    public static final Function<List<WorkStatusPojo>, List<WorkStatus>> WORK_STATUS_MAPPER =
            new Function<List<WorkStatusPojo>, List<WorkStatus>>() {
                @Override
                public List<WorkStatus> apply(List<WorkStatusPojo> input) {
                    if (input == null) {
                        return null;
                    }
                    List<WorkStatus> output = new ArrayList<>(input.size());
                    for (WorkStatusPojo in : input) {
                        output.add(in.toWorkStatus());
                    }
                    return output;
                }
            };
}
