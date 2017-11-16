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

package android.arch.background.workmanager.model;

import android.arch.background.workmanager.BaseWork;
import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

/**
 * Stores information about a logical unit of work.
 */
@Entity
@TypeConverters(value = {Arguments.class, ContentUriTriggers.class})
public class WorkSpec {
    private static final String TAG = "WorkSpec";

    @ColumnInfo(name = "id")
    @PrimaryKey
    @NonNull
    String mId;

    @ColumnInfo(name = "status")
    @BaseWork.WorkStatus
    int mStatus = BaseWork.STATUS_ENQUEUED;

    @ColumnInfo(name = "worker_class_name")
    String mWorkerClassName;

    @ColumnInfo(name = "initial_delay")
    long mInitialDelay;

    @ColumnInfo(name = "interval_duration")
    long mIntervalDuration;

    @ColumnInfo(name = "flex_duration")
    long mFlexDuration;

    @Embedded
    Constraints mConstraints = new Constraints.Builder().build();

    Arguments mArguments = new Arguments();

    @ColumnInfo(name = "tag")
    String mTag;

    @ColumnInfo(name = "run_attempt_count")
    int mRunAttemptCount;

    // TODO(sumir): Should Backoff be disabled by default?
    @ColumnInfo(name = "backoff_policy")
    @BaseWork.BackoffPolicy
    int mBackoffPolicy = BaseWork.BACKOFF_POLICY_EXPONENTIAL;

    @ColumnInfo(name = "backoff_delay_duration")
    long mBackoffDelayDuration = BaseWork.DEFAULT_BACKOFF_DELAY_DURATION;

    public WorkSpec(@NonNull String id) {
        mId = id;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    public void setId(@NonNull String id) {
        mId = id;
    }

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(int status) {
        mStatus = status;
    }

    public String getWorkerClassName() {
        return mWorkerClassName;
    }

    public void setWorkerClassName(String workerClassName) {
        mWorkerClassName = workerClassName;
    }

    public Constraints getConstraints() {
        return mConstraints;
    }

    public void setConstraints(Constraints constraints) {
        mConstraints = constraints;
    }

    public Arguments getArguments() {
        return mArguments;
    }

    public void setArguments(Arguments arguments) {
        mArguments = arguments;
    }

    public String getTag() {
        return mTag;
    }

    public void setTag(String tag) {
        mTag = tag;
    }

    public int getBackoffPolicy() {
        return mBackoffPolicy;
    }

    public void setBackoffPolicy(int backoffPolicy) {
        mBackoffPolicy = backoffPolicy;
    }

    public long getBackoffDelayDuration() {
        return mBackoffDelayDuration;
    }

    public void setBackoffDelayDuration(long backoffDelayDuration) {
        mBackoffDelayDuration = backoffDelayDuration;
    }

    public long getInitialDelay() {
        return mInitialDelay;
    }

    public void setInitialDelay(long initialDelay) {
        mInitialDelay = initialDelay;
    }

    public boolean isPeriodic() {
        return mIntervalDuration != 0L;
    }

    public void setPeriodic(long intervalDuration) {
        setPeriodic(intervalDuration, intervalDuration);
    }

    public void setPeriodic(long intervalDuration, long flexDuration) {
        mIntervalDuration = intervalDuration;
        mFlexDuration = flexDuration;
    }

    public long getIntervalDuration() {
        return mIntervalDuration;
    }

    public void setIntervalDuration(long intervalDuration) {
        mIntervalDuration = intervalDuration;
    }

    public long getFlexDuration() {
        return mFlexDuration;
    }

    public void setFlexDuration(long flexDuration) {
        mFlexDuration = flexDuration;
    }

    public void setRunAttemptCount(int runAttemptCount) {
        this.mRunAttemptCount = runAttemptCount;
    }

    public int getRunAttemptCount() {
        return mRunAttemptCount;
    }

    /**
     * Calculates delay with which this Work item should be executed.
     *
     * If the run attempt count is 0, the initial delay is returned.
     *
     * If Backoff Policy is set to {@link BaseWork#BACKOFF_POLICY_EXPONENTIAL}, then delay
     * increases at an exponential rate with respect to the run attempt count and is capped at
     * {@link BaseWork#MAX_BACKOFF_DURATION}.
     *
     * if Backoff Policy is set to {@link BaseWork#BACKOFF_POLICY_LINEAR}, then delay
     * increases at an linear rate with respect to the run attempt count and is capped at
     * {@link BaseWork#MAX_BACKOFF_DURATION}.
     *
     * Based on {@see https://android.googlesource.com/platform/frameworks/base/+/master/services/core/java/com/android/server/job/JobSchedulerService.java#1125}
     *
     * Note that this delay is for WorkManager internal use and may not match what the OS considers
     * to be the current delay.
     *
     * @return non-negative delay to execute this item with (in milliseconds)
     */
    public long calculateDelay() {
        if (mRunAttemptCount <= 0) {
            return mInitialDelay;
        }
        long delay;
        if (mBackoffPolicy == BaseWork.BACKOFF_POLICY_LINEAR) {
            delay = mBackoffDelayDuration * mRunAttemptCount;
        } else {
            // default to exponential backoff policy
            delay = (long) Math.scalb(mBackoffDelayDuration, mRunAttemptCount - 1);
        }
        return Math.min(BaseWork.MAX_BACKOFF_DURATION, delay);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkSpec other = (WorkSpec) o;
        return mId.equals(other.mId)
                && mStatus == other.mStatus
                && mInitialDelay == other.mInitialDelay
                && mIntervalDuration == other.mIntervalDuration
                && mFlexDuration == other.mFlexDuration
                && mRunAttemptCount == other.mRunAttemptCount
                && mBackoffPolicy == other.mBackoffPolicy
                && mBackoffDelayDuration == other.mBackoffDelayDuration
                && (mWorkerClassName != null
                        ? mWorkerClassName.equals(other.mWorkerClassName)
                        : other.mWorkerClassName == null)
                && (mConstraints != null
                        ? mConstraints.equals(other.mConstraints)
                        : other.mConstraints == null)
                && (mArguments != null
                        ? mArguments.equals(other.mArguments)
                        : other.mArguments == null)
                && (mTag != null ? mTag.equals(other.mTag) : other.mTag == null);
    }

    @Override
    public int hashCode() {
        int result = mId.hashCode();
        result = 31 * result + mStatus;
        result = 31 * result + (mWorkerClassName != null ? mWorkerClassName.hashCode() : 0);
        result = 31 * result + (int) (mInitialDelay ^ (mInitialDelay >>> 32));
        result = 31 * result + (int) (mIntervalDuration ^ (mIntervalDuration >>> 32));
        result = 31 * result + (int) (mFlexDuration ^ (mFlexDuration >>> 32));
        result = 31 * result + (mConstraints != null ? mConstraints.hashCode() : 0);
        result = 31 * result + (mArguments != null ? mArguments.hashCode() : 0);
        result = 31 * result + (mTag != null ? mTag.hashCode() : 0);
        result = 31 * result + mRunAttemptCount;
        result = 31 * result + mBackoffPolicy;
        result = 31 * result + (int) (mBackoffDelayDuration ^ (mBackoffDelayDuration >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "{WorkSpec: " + mId + "}";
    }
}
