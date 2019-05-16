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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Information about a particular {@link WorkRequest} containing the id of the WorkRequest, its
 * current {@link State}, output, tags, and run attempt count.  Note that output is only available
 * for the terminal states ({@link State#SUCCEEDED} and {@link State#FAILED}).
 */

public final class WorkInfo {

    private @NonNull UUID mId;
    private @NonNull State mState;
    private @NonNull Data mOutputData;
    private @NonNull Set<String> mTags;
    private int mRunAttemptCount;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkInfo(
            @NonNull UUID id,
            @NonNull State state,
            @NonNull Data outputData,
            @NonNull List<String> tags,
            int runAttemptCount) {
        mId = id;
        mState = state;
        mOutputData = outputData;
        mTags = new HashSet<>(tags);
        mRunAttemptCount = runAttemptCount;
    }

    /**
     * Gets the identifier of the {@link WorkRequest}.
     *
     * @return The identifier of a {@link WorkRequest}
     */
    public @NonNull UUID getId() {
        return mId;
    }

    /**
     * Gets the current {@link State} of the {@link WorkRequest}.
     *
     * @return The current {@link State} of the {@link WorkRequest}
     */
    public @NonNull State getState() {
        return mState;
    }

    /**
     * Gets the output {@link Data} for the {@link WorkRequest}.  If the WorkRequest is unfinished,
     * this is always {@link Data#EMPTY}.
     *
     * @return The output {@link Data} of the {@link WorkRequest}
     */
    public @NonNull Data getOutputData() {
        return mOutputData;
    }

    /**
     * Gets the {@link Set} of tags associated with the {@link WorkRequest}.
     *
     * @return The {@link Set} of tags associated with the {@link WorkRequest}
     */
    public @NonNull Set<String> getTags() {
        return mTags;
    }

    /**
     * Gets the run attempt count of the {@link WorkRequest}.  Note that for
     * {@link PeriodicWorkRequest}s, the run attempt count gets reset between successful runs.
     *
     * @return The run attempt count of the {@link WorkRequest}.
     */
    @IntRange(from = 0)
    public int getRunAttemptCount() {
        return mRunAttemptCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkInfo workInfo = (WorkInfo) o;

        if (mRunAttemptCount != workInfo.mRunAttemptCount) return false;
        if (!mId.equals(workInfo.mId)) return false;
        if (mState != workInfo.mState) return false;
        if (!mOutputData.equals(workInfo.mOutputData)) return false;
        return mTags.equals(workInfo.mTags);
    }

    @Override
    public int hashCode() {
        int result = mId.hashCode();
        result = 31 * result + mState.hashCode();
        result = 31 * result + mOutputData.hashCode();
        result = 31 * result + mTags.hashCode();
        result = 31 * result + mRunAttemptCount;
        return result;
    }

    @Override
    public String toString() {
        return "WorkInfo{"
                +   "mId='" + mId + '\''
                +   ", mState=" + mState
                +   ", mOutputData=" + mOutputData
                +   ", mTags=" + mTags
                + '}';
    }

    /**
     * The current lifecycle state of a {@link WorkRequest}.
     */
    public enum State {

        /**
         * Used to indicate that the {@link WorkRequest} is enqueued and eligible to run when its
         * {@link Constraints} are met and resources are available.
         */
        ENQUEUED,

        /**
         * Used to indicate that the {@link WorkRequest} is currently being executed.
         */
        RUNNING,

        /**
         * Used to indicate that the {@link WorkRequest} has completed in a successful state.  Note
         * that {@link PeriodicWorkRequest}s will never enter this state (they will simply go back
         * to {@link #ENQUEUED} and be eligible to run again).
         */
        SUCCEEDED,

        /**
         * Used to indicate that the {@link WorkRequest} has completed in a failure state.  All
         * dependent work will also be marked as {@code #FAILED} and will never run.
         */
        FAILED,

        /**
         * Used to indicate that the {@link WorkRequest} is currently blocked because its
         * prerequisites haven't finished successfully.
         */
        BLOCKED,

        /**
         * Used to indicate that the {@link WorkRequest} has been cancelled and will not execute.
         * All dependent work will also be marked as {@code #CANCELLED} and will not run.
         */
        CANCELLED;

        /**
         * Returns {@code true} if this State is considered finished.
         *
         * @return {@code true} for {@link #SUCCEEDED}, {@link #FAILED}, and * {@link #CANCELLED}
         *         states
         */
        public boolean isFinished() {
            return (this == SUCCEEDED || this == FAILED || this == CANCELLED);
        }
    }
}
