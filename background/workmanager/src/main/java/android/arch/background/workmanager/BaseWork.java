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

import android.arch.background.workmanager.model.Arguments;
import android.arch.background.workmanager.model.Constraints;
import android.arch.background.workmanager.model.WorkSpec;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.lang.annotation.Retention;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The base class for defining repeated and one-shot work.
 */

public abstract class BaseWork {

    @Retention(SOURCE)
    @IntDef({STATUS_ENQUEUED, STATUS_RUNNING, STATUS_SUCCEEDED, STATUS_FAILED, STATUS_BLOCKED})
    public @interface WorkStatus {
    }

    @Retention(SOURCE)
    @IntDef({BACKOFF_POLICY_EXPONENTIAL, BACKOFF_POLICY_LINEAR})
    public @interface BackoffPolicy {
    }

    public static final int STATUS_ENQUEUED = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_SUCCEEDED = 2;
    public static final int STATUS_FAILED = 3;
    public static final int STATUS_BLOCKED = 4;

    public static final int BACKOFF_POLICY_EXPONENTIAL = 0;
    public static final int BACKOFF_POLICY_LINEAR = 1;
    public static final long DEFAULT_BACKOFF_DELAY_MILLIS = 30000L;

    /**
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/core/java/android/app/job/JobInfo.java#82}
     */
    public static final long MAX_BACKOFF_MILLIS = 5 * 60 * 60 * 1000; // 5 hours.

    /**
     * {@see https://android.googlesource.com/platform/frameworks/base/+/oreo-release/core/java/android/app/job/JobInfo.java#119}
     */
    public static final long MIN_BACKOFF_MILLIS = 10 * 1000; // 10 seconds.

    private static final String TAG = "BaseWork";

    private WorkSpec mWorkSpec;
    private Set<String> mTags;

    BaseWork(Builder builder) {
        mWorkSpec = builder.mWorkSpec;
        mTags = builder.mTags;
        if (builder.mBackoffCriteriaSet && mWorkSpec.getConstraints().requiresDeviceIdle()) {
            throw new IllegalArgumentException("Cannot set backoff criteria on an idle mode job");
        }
    }

    /**
     * @return The id for this set of work.
     */
    public String getId() {
        return mWorkSpec.getId();
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkSpec getWorkSpec() {
        return mWorkSpec;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Set<String> getTags() {
        return mTags;
    }

    /**
     * A builder for {@link BaseWork}.
     *
     * @param <W> The {@link BaseWork} class being created by this builder
     * @param <B> The concrete implementation of this builder
     */
    public abstract static class Builder<W extends BaseWork, B extends Builder<W, B>> {
        private boolean mBackoffCriteriaSet = false;
        WorkSpec mWorkSpec = new WorkSpec(UUID.randomUUID().toString());
        Set<String> mTags = new HashSet<>();

        protected Builder(Class<? extends Worker> workerClass) {
            mWorkSpec.setWorkerClassName(workerClass.getName());
        }

        @VisibleForTesting
        B withInitialStatus(@Work.WorkStatus int status) {
            mWorkSpec.setStatus(status);
            return getThis();
        }

        @VisibleForTesting
        B withInitialRunAttemptCount(int runAttemptCount) {
            mWorkSpec.setRunAttemptCount(runAttemptCount);
            return getThis();
        }

        /**
         * Change backoff policy and delay for the {@link BaseWork}.
         * Default is {@value Work#BACKOFF_POLICY_EXPONENTIAL} and 30 seconds.
         * Maximum backoff delay duration is {@value #MAX_BACKOFF_MILLIS}.
         *
         * @param backoffPolicy        Backoff Policy to use for {@link BaseWork}
         * @param backoffDelayMillis Time to wait before restarting {@link Worker}
         *                             (in milliseconds)
         * @return The current {@link Builder}.
         */
        public B withBackoffCriteria(@Work.BackoffPolicy int backoffPolicy,
                                                long backoffDelayMillis) {
            mBackoffCriteriaSet = true;
            if (backoffDelayMillis > MAX_BACKOFF_MILLIS) {
                Log.w(TAG, "Backoff delay duration exceeds maximum value");
                backoffDelayMillis = MAX_BACKOFF_MILLIS;
            }
            if (backoffDelayMillis < MIN_BACKOFF_MILLIS) {
                Log.w(TAG, "Backoff delay duration less than minimum value");
                backoffDelayMillis = MIN_BACKOFF_MILLIS;
            }
            mWorkSpec.setBackoffPolicy(backoffPolicy);
            mWorkSpec.setBackoffDelayDuration(backoffDelayMillis);
            return getThis();
        }

        /**
         * Add constraints to the {@link Work}.
         *
         * @param constraints The constraints for the {@link Work}
         * @return The current {@link Work.Builder}.
         */
        public B withConstraints(@NonNull Constraints constraints) {
            mWorkSpec.setConstraints(constraints);
            return getThis();
        }

        /**
         * Add arguments to the {@link BaseWork}.
         *
         * @param arguments key/value pairs that will be provided to the {@link Worker} class
         * @return The current {@link Builder}.
         */
        public B withArguments(Arguments arguments) {
            mWorkSpec.setArguments(arguments);
            return getThis();
        }

        /**
         * Add an optional tag to the {@link BaseWork}.  This is particularly useful for modules or
         * libraries who want to query for or cancel all of their own work.
         *
         * @param tag A tag for identifying the {@link BaseWork} in queries.
         * @return The current {@link Builder}.
         */
        public B addTag(String tag) {
            mTags.add(tag);
            return getThis();
        }

        abstract B getThis();

        /**
         * Builds this {@link BaseWork} object.
         *
         * @return The concrete implementation of {@link BaseWork} associated with this builder
         */
        public abstract W build();
    }
}
