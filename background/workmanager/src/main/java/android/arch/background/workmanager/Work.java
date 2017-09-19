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

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.util.UUID;

/**
 * A class to create a logical unit of work.
 */

public class Work {

    @Retention(SOURCE)
    @IntDef({STATUS_FAILED, STATUS_RUNNING, STATUS_SUCCEEDED, STATUS_ENQUEUED})
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

    public static final int BACKOFF_POLICY_EXPONENTIAL = 0;
    public static final int BACKOFF_POLICY_LINEAR = 1;
    public static final long DEFAULT_BACKOFF_DELAY_DURATION = 30000L;

    private WorkSpec mWorkSpec;

    private Work(WorkSpec workSpec) {
        mWorkSpec = workSpec;
    }

    /**
     * @return The id for this set of work.
     */
    public String getId() {
        return mWorkSpec.mId;
    }


    WorkSpec getWorkSpec() {
        return mWorkSpec;
    }

    /**
     * Builder for {@link Work} class.
     */
    public static class Builder {
        private WorkSpec mWorkSpec = new WorkSpec(UUID.randomUUID().toString());

        public Builder(Class<? extends Worker> workerClass) {
            mWorkSpec.mWorkerClassName = workerClass.getName();
        }

        /**
         * Add constraints to the current {@link WorkSpec}.
         *
         * @param constraints the constraints to attach to the work item
         * @return current builder
         */
        public Builder withConstraints(@NonNull Constraints constraints) {
            mWorkSpec.mConstraints = constraints;
            return this;
        }

        /**
         * Change backoff policy and delay for the current {@link WorkSpec}.
         * Default is {@value Work#BACKOFF_POLICY_EXPONENTIAL} and 30 seconds.
         *
         * @param backoffPolicy Backoff Policy to use for current {@link WorkSpec}
         * @param backoffDelayDuration Time to wait before restarting {@link Worker}
         *                             (in milliseconds)
         * @return current builder
         */
        public Builder withBackoffCriteria(@BackoffPolicy int backoffPolicy,
                                           long backoffDelayDuration) {
            // TODO(xbhatnag): Enforce restrictions on backoff delay. 30 seconds?
            mWorkSpec.mBackoffPolicy = backoffPolicy;
            mWorkSpec.mBackoffDelayDuration = backoffDelayDuration;
            return this;
        }

        /**
         * Add arguments to the current {@link WorkSpec}.
         * @param arguments key/value pairs that will be provided to the {@link Worker} class
         * @return current builder
         */
        public Builder withArguments(Arguments arguments) {
            mWorkSpec.mArguments = arguments;
            return this;
        }

        /**
         * Generates the {@link Work} from this builder
         *
         * @return new {@link Work}
         */
        public Work build() {
            return new Work(mWorkSpec);
        }
    }
}
