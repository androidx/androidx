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

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A simple class with the id of a {@link WorkRequest}, its current {@link State}, output, and
 * tags.  Note that output is only available for terminal states ({@link State#SUCCEEDED} and
 * {@link State#FAILED}).
 */

public final class WorkInfo {

    private @NonNull UUID mId;
    private @NonNull State mState;
    private @NonNull Data mOutputData;
    private @NonNull Set<String> mTags;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkInfo(
            @NonNull UUID id,
            @NonNull State state,
            @NonNull Data outputData,
            @NonNull List<String> tags) {
        mId = id;
        mState = state;
        mOutputData = outputData;
        mTags = new HashSet<>(tags);
    }

    public @NonNull UUID getId() {
        return mId;
    }

    public @NonNull State getState() {
        return mState;
    }

    public @NonNull Data getOutputData() {
        return mOutputData;
    }

    public @NonNull Set<String> getTags() {
        return mTags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkInfo that = (WorkInfo) o;

        if (mId != null ? !mId.equals(that.mId) : that.mId != null) return false;
        if (mState != that.mState) return false;
        if (mOutputData != null ? !mOutputData.equals(that.mOutputData)
                : that.mOutputData != null) {
            return false;
        }
        return mTags != null ? mTags.equals(that.mTags) : that.mTags == null;
    }

    @Override
    public int hashCode() {
        int result = mId != null ? mId.hashCode() : 0;
        result = 31 * result + (mState != null ? mState.hashCode() : 0);
        result = 31 * result + (mOutputData != null ? mOutputData.hashCode() : 0);
        result = 31 * result + (mTags != null ? mTags.hashCode() : 0);
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
     * The current state of a unit of work.
     */
    public enum State {

        /**
         * The state for work that is enqueued (hasn't completed and isn't running)
         */
        ENQUEUED,

        /**
         * The state for work that is currently being executed
         */
        RUNNING,

        /**
         * The state for work that has completed successfully
         */
        SUCCEEDED,

        /**
         * The state for work that has completed in a failure state
         */
        FAILED,

        /**
         * The state for work that is currently blocked because its prerequisites haven't finished
         * successfully
         */
        BLOCKED,

        /**
         * The state for work that has been cancelled and will not execute
         */
        CANCELLED;

        /**
         * Returns {@code true} if this State is considered finished.
         *
         * @return {@code true} for {@link #SUCCEEDED}, {@link #FAILED}, and
         * {@link #CANCELLED} states
         */
        public boolean isFinished() {
            return (this == SUCCEEDED || this == FAILED || this == CANCELLED);
        }
    }
}
