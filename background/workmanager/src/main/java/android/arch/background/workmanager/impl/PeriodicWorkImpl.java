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

package android.arch.background.workmanager.impl;

import android.arch.background.workmanager.Arguments;
import android.arch.background.workmanager.BaseWork;
import android.arch.background.workmanager.Constraints;
import android.arch.background.workmanager.PeriodicWork;
import android.arch.background.workmanager.Worker;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.support.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A concrete implementation of {@link PeriodicWork}.
 */

public class PeriodicWorkImpl extends PeriodicWork implements InternalWorkImpl {

    private WorkSpec mWorkSpec;
    private Set<String> mTags;

    PeriodicWorkImpl(Builder builder) {
        mWorkSpec = builder.mWorkSpec;
        mTags = builder.mTags;
    }

    @Override
    public String getId() {
        return mWorkSpec.getId();
    }

    @Override
    public WorkSpec getWorkSpec() {
        return mWorkSpec;
    }

    @Override
    public Set<String> getTags() {
        return mTags;
    }

    /**
     * Builder for {@link PeriodicWorkImpl} class.
     */
    public static class Builder extends PeriodicWork.Builder {

        private boolean mBackoffCriteriaSet = false;
        WorkSpec mWorkSpec = new WorkSpec(UUID.randomUUID().toString());
        Set<String> mTags = new HashSet<>();

        public Builder(Class<? extends Worker> workerClass, long intervalMillis) {
            super(workerClass, intervalMillis);
            mWorkSpec.setWorkerClassName(workerClass.getName());
            mWorkSpec.setPeriodic(intervalMillis);
        }

        public Builder(
                Class<? extends Worker> workerClass,
                long intervalMillis,
                long flexMillis) {
            super(workerClass, intervalMillis, flexMillis);
            mWorkSpec.setWorkerClassName(workerClass.getName());
            mWorkSpec.setPeriodic(intervalMillis, flexMillis);
        }

        @Override
        public PeriodicWork.Builder withInitialStatus(@BaseWork.WorkStatus int status) {
            mWorkSpec.setStatus(status);
            return this;
        }

        @Override
        public PeriodicWork.Builder withInitialRunAttemptCount(int runAttemptCount) {
            mWorkSpec.setRunAttemptCount(runAttemptCount);
            return this;
        }

        @Override
        public PeriodicWork.Builder withPeriodStartTime(long periodStartTime) {
            mWorkSpec.setPeriodStartTime(periodStartTime);
            return this;
        }

        @Override
        public PeriodicWork.Builder withBackoffCriteria(
                @BaseWork.BackoffPolicy int backoffPolicy,
                long backoffDelayMillis) {
            mBackoffCriteriaSet = true;
            mWorkSpec.setBackoffPolicy(backoffPolicy);
            mWorkSpec.setBackoffDelayDuration(backoffDelayMillis);
            return this;
        }

        @Override
        public PeriodicWork.Builder withConstraints(@NonNull Constraints constraints) {
            mWorkSpec.setConstraints(constraints);
            return this;
        }

        @Override
        public PeriodicWork.Builder withArguments(@NonNull Arguments arguments) {
            mWorkSpec.setArguments(arguments);
            return this;
        }

        @Override
        public PeriodicWork.Builder addTag(String tag) {
            mTags.add(tag);
            return this;
        }

        @Override
        public PeriodicWork build() {
            if (mBackoffCriteriaSet && mWorkSpec.getConstraints().requiresDeviceIdle()) {
                throw new IllegalArgumentException(
                        "Cannot set backoff criteria on an idle mode job");
            }
            return new PeriodicWorkImpl(this);
        }
    }
}
