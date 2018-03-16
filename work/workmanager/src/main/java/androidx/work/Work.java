/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import androidx.work.impl.model.WorkSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A class to execute a logical unit of non-repeating work.
 */

public class Work extends BaseWork {

    /**
     * Creates an array of {@link Work} with defaults from an array of {@link Worker} class names.
     *
     * @param workerClasses An array of {@link Worker} class names
     * @return A list of {@link Work} constructed by using defaults in the {@link Builder}
     */
    @SafeVarargs public static @NonNull List<Work> from(
            @NonNull Class<? extends Worker>... workerClasses) {
        return from(Arrays.asList(workerClasses));
    }

    /**
     * Creates a list of {@link Work} with defaults from an array of {@link Worker} class names.
     *
     * @param workerClasses A list of {@link Worker} class names
     * @return A list of {@link Work} constructed by using defaults in the {@link Builder}
     */
    public static @NonNull List<Work> from(
            @NonNull List<Class<? extends Worker>> workerClasses) {
        List<Work> workList = new ArrayList<>(workerClasses.size());
        for (Class<? extends Worker> workerClass : workerClasses) {
            workList.add(new Work.Builder(workerClass).build());
        }
        return workList;
    }


    Work(Builder builder) {
        super(builder.mWorkSpec, builder.mTags);
    }

    /**
     * Builder for {@link Work} class.
     */
    public static class Builder implements WorkBuilder<Work, Builder> {

        private boolean mBackoffCriteriaSet = false;
        WorkSpec mWorkSpec = new WorkSpec(UUID.randomUUID().toString());
        Set<String> mTags = new HashSet<>();

        public Builder(@NonNull Class<? extends Worker> workerClass) {
            mWorkSpec.setWorkerClassName(workerClass.getName());
            mWorkSpec.setInputMergerClassName(OverwritingInputMerger.class.getName());
        }

        @VisibleForTesting
        @Override
        public Builder withInitialState(@NonNull State state) {
            mWorkSpec.setState(state);
            return this;
        }

        @VisibleForTesting
        @Override
        public Builder withInitialRunAttemptCount(int runAttemptCount) {
            mWorkSpec.setRunAttemptCount(runAttemptCount);
            return this;
        }

        @VisibleForTesting
        @Override
        public Builder withPeriodStartTime(long periodStartTime, @NonNull TimeUnit timeUnit) {
            mWorkSpec.setPeriodStartTime(timeUnit.toMillis(periodStartTime));
            return this;
        }

        @Override
        public Builder withBackoffCriteria(
                @NonNull BackoffPolicy backoffPolicy,
                long backoffDelay,
                @NonNull TimeUnit timeUnit) {
            mBackoffCriteriaSet = true;
            mWorkSpec.setBackoffPolicy(backoffPolicy);
            mWorkSpec.setBackoffDelayDuration(timeUnit.toMillis(backoffDelay));
            return this;
        }

        @Override
        public Builder withConstraints(@NonNull Constraints constraints) {
            mWorkSpec.setConstraints(constraints);
            return this;
        }

        @Override
        public Builder withArguments(@NonNull Arguments arguments) {
            mWorkSpec.setArguments(arguments);
            return this;
        }

        @Override
        public Builder addTag(@NonNull String tag) {
            mTags.add(tag);
            return this;
        }

        @Override
        public Builder withInitialDelay(long duration, @NonNull TimeUnit timeUnit) {
            mWorkSpec.setInitialDelay(timeUnit.toMillis(duration));
            return this;
        }

        @Override
        public Builder withInputMerger(@NonNull Class<? extends InputMerger> inputMerger) {
            mWorkSpec.setInputMergerClassName(inputMerger.getName());
            return this;
        }

        @Override
        public Builder keepResultsForAtLeast(long duration, @NonNull TimeUnit timeUnit) {
            mWorkSpec.setMinimumRetentionDuration(timeUnit.toMillis(duration));
            return this;
        }

        @Override
        public Work build() {
            if (mBackoffCriteriaSet
                    && Build.VERSION.SDK_INT >= 23
                    && mWorkSpec.getConstraints().requiresDeviceIdle()) {
                throw new IllegalArgumentException(
                        "Cannot set backoff criteria on an idle mode job");
            }
            return new Work(this);
        }
    }
}
