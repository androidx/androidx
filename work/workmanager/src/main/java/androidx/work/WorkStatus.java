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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A simple class with the id of a {@link WorkRequest}, its current {@link State}, output, and
 * tags.  Note that output is only available for terminal states ({@link State#SUCCEEDED} and
 * {@link State#FAILED}).
 */

public final class WorkStatus {

    private @NonNull UUID mId;
    private @NonNull State mState;
    private @NonNull Data mOutputData;
    private @NonNull Set<String> mTags;

    public WorkStatus(
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

        WorkStatus that = (WorkStatus) o;

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
        return "WorkStatus{"
                +   "mId='" + mId + '\''
                +   ", mState=" + mState
                +   ", mOutputData=" + mOutputData
                +   ", mTags=" + mTags
                + '}';
    }
}
