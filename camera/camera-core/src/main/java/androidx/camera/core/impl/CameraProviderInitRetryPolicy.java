/*
 * Copyright 2023 The Android Open Source Project
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

import static androidx.camera.core.impl.CameraValidator.CameraIdListIncorrectException;

import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalRetryPolicy;
import androidx.camera.core.Logger;
import androidx.camera.core.RetryPolicy;

/**
 * Basic retry policy that automatically retries most failures with a standard delay.
 *
 * <p>This policy will initiate a retry with the
 * {@link RetryConfig#DEFAULT_DELAY_RETRY} delay for any failure status except
 * {@link ExecutionState#STATUS_CONFIGURATION_FAIL}.
 */
@ExperimentalRetryPolicy
public final class CameraProviderInitRetryPolicy implements RetryPolicyInternal {

    private final RetryPolicy mDelegatePolicy;

    public CameraProviderInitRetryPolicy(long timeoutInMillis) {
        mDelegatePolicy = new TimeoutRetryPolicy(timeoutInMillis, new RetryPolicy() {
            @NonNull
            @Override
            public RetryConfig onRetryDecisionRequested(@NonNull ExecutionState executionState) {
                if (executionState.getStatus() == ExecutionState.STATUS_CONFIGURATION_FAIL) {
                    return RetryConfig.NOT_RETRY;
                }

                return RetryConfig.DEFAULT_DELAY_RETRY;
            }

            @Override
            public long getTimeoutInMillis() {
                return timeoutInMillis;
            }
        });
    }

    @NonNull
    @Override
    public RetryConfig onRetryDecisionRequested(@NonNull ExecutionState executionState) {
        return mDelegatePolicy.onRetryDecisionRequested(executionState);
    }

    @Override
    public long getTimeoutInMillis() {
        return mDelegatePolicy.getTimeoutInMillis();
    }

    @NonNull
    @Override
    public RetryPolicy copy(long timeoutInMillis) {
        return new CameraProviderInitRetryPolicy(timeoutInMillis);
    }

    /**
     * A legacy implementation of {@link CameraProviderInitRetryPolicy} that treats
     * {@link CameraIdListIncorrectException} as a special case.
     *
     * <p>In older versions of the CameraProviderInitRetryPolicy, there's a special rule for
     * handling CameraValidator.CameraIdListIncorrectException errors if:
     * <ul>
     *   <li>The camera initialization task takes longer than the allowed timeout.
     *   <li>The error is CameraValidator.CameraIdListIncorrectException.
     *   <li>There's more than one camera available.
     * </ul>
     * Then:
     * <ul>
     *   <li>The task is considered complete and won't be retried.
     * </ul>
     */
    public static final class Legacy implements RetryPolicyInternal {

        private final RetryPolicy mBasePolicy;

        public Legacy(long timeoutInMillis) {
            mBasePolicy = new CameraProviderInitRetryPolicy(timeoutInMillis);
        }

        @NonNull
        @Override
        public RetryConfig onRetryDecisionRequested(@NonNull ExecutionState executionState) {
            if (!mBasePolicy.onRetryDecisionRequested(executionState).shouldRetry()) {
                Throwable cause = executionState.getCause();
                if (cause instanceof CameraIdListIncorrectException) {
                    Logger.e("CameraX", "The device might underreport the amount of the "
                            + "cameras. Finish the initialize task since we are already "
                            + "reaching the maximum number of retries.");
                    if (((CameraIdListIncorrectException) cause).getAvailableCameraCount() > 0) {
                        // If the initialization task execution time exceeds the timeout
                        // threshold and the error type is CameraIdListIncorrectException,
                        // consider the initialization complete without retrying.
                        return RetryConfig.COMPLETE_WITHOUT_FAILURE;
                    }
                }
                return RetryConfig.NOT_RETRY;
            }
            return RetryConfig.DEFAULT_DELAY_RETRY;
        }

        @Override
        public long getTimeoutInMillis() {
            return mBasePolicy.getTimeoutInMillis();
        }

        @NonNull
        @Override
        public RetryPolicy copy(long timeoutInMillis) {
            return new Legacy(timeoutInMillis);
        }
    }
}
