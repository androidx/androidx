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

import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.ExperimentalRetryPolicy;
import androidx.camera.core.InitializationException;
import androidx.camera.core.RetryPolicy;

/**
 * This class acts as a container for information about the execution state of the camera
 * initialization process.
 */
@ExperimentalRetryPolicy
public final class CameraProviderExecutionState implements RetryPolicy.ExecutionState {

    private final int mStatus;
    private final int mAttemptCount;
    private final long mTaskExecutedTimeInMillis;
    @Nullable
    private final Throwable mCause;

    /**
     * Constructs a {@link CameraProviderExecutionState} object.
     *
     * @param taskStartTimeInMillis The start time of the task in milliseconds.
     * @param attemptCount          The number of times the task has been attempted.
     * @param throwable             The error that occurred during the task execution, or null if
     *                              there was no error.
     */
    public CameraProviderExecutionState(
            long taskStartTimeInMillis,
            int attemptCount,
            @Nullable Throwable throwable) {
        mTaskExecutedTimeInMillis = SystemClock.elapsedRealtime() - taskStartTimeInMillis;
        mAttemptCount = attemptCount;
        if (throwable instanceof CameraValidator.CameraIdListIncorrectException) {
            mStatus = STATUS_CAMERA_UNAVAILABLE;
            mCause = throwable;
        } else if (throwable instanceof InitializationException) {
            Throwable cause = throwable.getCause();
            mCause = cause != null ? cause : throwable;
            if (mCause instanceof CameraUnavailableException) {
                mStatus = STATUS_CAMERA_UNAVAILABLE;
            } else if (mCause instanceof IllegalArgumentException) {
                mStatus = STATUS_CONFIGURATION_FAIL;
            } else {
                mStatus = STATUS_UNKNOWN_ERROR;
            }
        } else {
            mStatus = STATUS_UNKNOWN_ERROR;
            mCause = throwable;
        }
    }

    /**
     * @return The status of the ExecutionState.
     */
    @Status
    @Override
    public int getStatus() {
        return mStatus;
    }

    /**
     * @return The cause that occurred during the task execution, or null if there was no error.
     */
    @Nullable
    @Override
    public Throwable getCause() {
        return mCause;
    }

    /**
     * @return The time in milliseconds that the task has been executing.
     */
    @Override
    public long getExecutedTimeInMillis() {
        return mTaskExecutedTimeInMillis;
    }

    /**
     * @return The number of times the task has been attempted.
     */
    @Override
    public int getNumOfAttempts() {
        return mAttemptCount;
    }
}
