/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalRetryPolicy;
import androidx.camera.core.RetryPolicy;
import androidx.core.util.Preconditions;

/**
 * Automatically halts retries if execution time surpasses a specified timeout.
 *
 * <p>This retry policy monitors the total execution time of a task. If the time surpasses the
 * configured timeout threshold, it immediately stops any further retries by returning
 * {@link RetryConfig#NOT_RETRY}.
 *
 * <p>If the task total execution within the timeout, this policy delegates the retry decision to
 * the underlying {@link RetryPolicy}, allowing for normal retry behavior based on other factors.
 *
 * <p>A timeout of 0 means the execution will never be halted.
 */
@ExperimentalRetryPolicy
public final class TimeoutRetryPolicy implements RetryPolicy {

    private final long mTimeoutInMillis;
    private final RetryPolicy mDelegatePolicy;

    /**
     * Constructor.
     *
     * @param timeoutInMillis  The maximum allowed execution time in milliseconds. Must be
     *                         0 or greater. 0 means the execution will never be halted.
     * @param delegatePolicy   The RetryPolicy used to decide retries within the timeout.
     */
    public TimeoutRetryPolicy(long timeoutInMillis, @NonNull RetryPolicy delegatePolicy) {
        Preconditions.checkArgument(timeoutInMillis >= 0, "Timeout must be non-negative.");
        mTimeoutInMillis = timeoutInMillis;
        mDelegatePolicy = delegatePolicy;
    }

    @NonNull
    @Override
    public RetryConfig onRetryDecisionRequested(@NonNull ExecutionState executionState) {
        RetryConfig retryConfig = mDelegatePolicy.onRetryDecisionRequested(executionState);
        return getTimeoutInMillis() > 0 && executionState.getExecutedTimeInMillis()
                >= getTimeoutInMillis() - retryConfig.getRetryDelayInMillis()
                ? RetryConfig.NOT_RETRY : retryConfig;
    }

    @Override
    public long getTimeoutInMillis() {
        return mTimeoutInMillis;
    }
}
