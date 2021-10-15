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
import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkRequest.MAX_BACKOFF_MILLIS;
import static androidx.work.WorkRequest.MIN_BACKOFF_MILLIS;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.arch.core.util.Function;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Relation;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.Logger;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkInfo;
import androidx.work.WorkRequest;

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
        indices = {
                @Index(value = {"schedule_requested_at"}),
                @Index(value = {"period_start_time"})
        }
)
public final class WorkSpec {
    private static final String TAG = Logger.tagWithPrefix("WorkSpec");
    public static final long SCHEDULE_NOT_REQUESTED_YET = -1;

    @ColumnInfo(name = "id")
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "state")
    @NonNull
    public WorkInfo.State state = ENQUEUED;

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
    @IntRange(from = 0)
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

    /**
     * This is {@code true} when the WorkSpec needs to be hosted by a foreground service or a
     * high priority job.
     */
    @ColumnInfo(name = "run_in_foreground")
    public boolean expedited;

    /**
     * When set to <code>true</code> this {@link WorkSpec} falls back to a regular job when
     * an application runs out of expedited job quota.
     */
    @NonNull
    @ColumnInfo(name = "out_of_quota_policy")
    public OutOfQuotaPolicy outOfQuotaPolicy = OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST;

    public WorkSpec(@NonNull String id, @NonNull String workerClassName) {
        this.id = id;
        this.workerClassName = workerClassName;
    }

    public WorkSpec(@NonNull WorkSpec other) {
        id = other.id;
        workerClassName = other.workerClassName;
        state = other.state;
        inputMergerClassName = other.inputMergerClassName;
        input = new Data(other.input);
        output = new Data(other.output);
        initialDelay = other.initialDelay;
        intervalDuration = other.intervalDuration;
        flexDuration = other.flexDuration;
        constraints = new Constraints(other.constraints);
        runAttemptCount = other.runAttemptCount;
        backoffPolicy = other.backoffPolicy;
        backoffDelayDuration = other.backoffDelayDuration;
        periodStartTime = other.periodStartTime;
        minimumRetentionDuration = other.minimumRetentionDuration;
        scheduleRequestedAt = other.scheduleRequestedAt;
        expedited = other.expedited;
        outOfQuotaPolicy = other.outOfQuotaPolicy;
    }

    /**
     * @param backoffDelayDuration The backoff delay duration in milliseconds
     */
    public void setBackoffDelayDuration(long backoffDelayDuration) {
        if (backoffDelayDuration > MAX_BACKOFF_MILLIS) {
            Logger.get().warning(TAG, "Backoff delay duration exceeds maximum value");
            backoffDelayDuration = MAX_BACKOFF_MILLIS;
        }
        if (backoffDelayDuration < MIN_BACKOFF_MILLIS) {
            Logger.get().warning(TAG, "Backoff delay duration less than minimum value");
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
            Logger.get().warning(TAG, "Interval duration lesser than minimum allowed value; Changed to " + MIN_PERIODIC_INTERVAL_MILLIS);
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
            Logger.get().warning(TAG, "Interval duration lesser than minimum allowed value; Changed to " + MIN_PERIODIC_INTERVAL_MILLIS);
            intervalDuration = MIN_PERIODIC_INTERVAL_MILLIS;
        }
        if (flexDuration < MIN_PERIODIC_FLEX_MILLIS) {
            Logger.get().warning(TAG,
                    "Flex duration lesser than minimum allowed value; Changed to " + MIN_PERIODIC_FLEX_MILLIS);
            flexDuration = MIN_PERIODIC_FLEX_MILLIS;
        }
        if (flexDuration > intervalDuration) {
            Logger.get().warning(TAG,
                    "Flex duration greater than interval duration; Changed to " + intervalDuration);
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
            long now = System.currentTimeMillis();
            long start = periodStartTime == 0 ? (now + initialDelay) : periodStartTime;
            boolean isFlexApplicable = flexDuration != intervalDuration;
            if (isFlexApplicable) {
                // To correctly emulate flex, we need to set it
                // to now, so the PeriodicWorkRequest has an initial delay of
                // initialDelay + (interval - flex).

                // The subsequent runs will only add the interval duration and no flex.
                // This gives us the following behavior:
                // 1 => now + (interval - flex) + initialDelay = firstRunTime
                // 2 => firstRunTime + 2 * interval - flex
                // 3 => firstRunTime + 3 * interval - flex
                long offset = periodStartTime == 0 ? (-1 * flexDuration) : 0;
                return start + intervalDuration + offset;
            } else {
                // Don't use flexDuration for determining next run time for PeriodicWork
                // This is because intervalDuration could equal flexDuration.

                // The first run of a periodic work request is immediate in JobScheduler, and we
                // need to emulate this behavior.
                long offset = periodStartTime == 0 ? 0 : intervalDuration;
                return start + offset;
            }
        } else {
            // We are checking for (periodStartTime == 0) to support our testing use case.
            // For newly created WorkSpecs periodStartTime will always be 0.
            long start = (periodStartTime == 0) ? System.currentTimeMillis() : periodStartTime;
            return start + initialDelay;
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
        if (expedited != workSpec.expedited) return false;
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
        if (backoffPolicy != workSpec.backoffPolicy) return false;
        return outOfQuotaPolicy == workSpec.outOfQuotaPolicy;
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
        result = 31 * result + (expedited ? 1 : 0);
        result = 31 * result + outOfQuotaPolicy.hashCode();
        return result;
    }

    @NonNull
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
        public WorkInfo.State state;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IdAndState)) return false;

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
     * A POJO containing the ID, state, output, tags, and run attempt count of a WorkSpec.
     */
    public static class WorkInfoPojo {

        @ColumnInfo(name = "id")
        public String id;

        @ColumnInfo(name = "state")
        public WorkInfo.State state;

        @ColumnInfo(name = "output")
        public Data output;

        @ColumnInfo(name = "run_attempt_count")
        public int runAttemptCount;

        @Relation(
                parentColumn = "id",
                entityColumn = "work_spec_id",
                entity = WorkTag.class,
                projection = {"tag"})
        public List<String> tags;

        // This is actually a 1-1 relationship. However Room 2.1 models the type as a List.
        // This will change in Room 2.2
        @Relation(
                parentColumn = "id",
                entityColumn = "work_spec_id",
                entity = WorkProgress.class,
                projection = {"progress"})
        public List<Data> progress;

        /**
         * Converts this POJO to a {@link WorkInfo}.
         *
         * @return The {@link WorkInfo} represented by this POJO
         */
        @NonNull
        public WorkInfo toWorkInfo() {
            Data progress = this.progress != null && !this.progress.isEmpty()
                    ? this.progress.get(0)
                    : Data.EMPTY;

            return new WorkInfo(
                    UUID.fromString(id),
                    state,
                    output,
                    tags,
                    progress,
                    runAttemptCount);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WorkInfoPojo)) return false;

            WorkInfoPojo that = (WorkInfoPojo) o;

            if (runAttemptCount != that.runAttemptCount) return false;
            if (id != null ? !id.equals(that.id) : that.id != null) return false;
            if (state != that.state) return false;
            if (output != null ? !output.equals(that.output) : that.output != null) return false;
            if (tags != null ? !tags.equals(that.tags) : that.tags != null) return false;
            return progress != null ? progress.equals(that.progress) : that.progress == null;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (state != null ? state.hashCode() : 0);
            result = 31 * result + (output != null ? output.hashCode() : 0);
            result = 31 * result + runAttemptCount;
            result = 31 * result + (tags != null ? tags.hashCode() : 0);
            result = 31 * result + (progress != null ? progress.hashCode() : 0);
            return result;
        }
    }

    public static final Function<List<WorkInfoPojo>, List<WorkInfo>> WORK_INFO_MAPPER =
            new Function<List<WorkInfoPojo>, List<WorkInfo>>() {
                @Override
                public List<WorkInfo> apply(List<WorkInfoPojo> input) {
                    if (input == null) {
                        return null;
                    }
                    List<WorkInfo> output = new ArrayList<>(input.size());
                    for (WorkInfoPojo in : input) {
                        output.add(in.toWorkInfo());
                    }
                    return output;
                }
            };
}
