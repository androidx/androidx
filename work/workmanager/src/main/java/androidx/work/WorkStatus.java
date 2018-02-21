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

/**
 * A simple class with the id of a {@link BaseWork}, its current {@link State}, and its output.
 * Note that output is only available for terminal states ({@link State#SUCCEEDED} and
 * {@link State#FAILED}).
 */

public class WorkStatus {

    private String mId;
    private State mState;
    private Arguments mOutput;

    public WorkStatus(@NonNull String id, @NonNull State state, @NonNull Arguments output) {
        mId = id;
        mState = state;
        mOutput = output;
    }

    public @NonNull String getId() {
        return mId;
    }

    public @NonNull State getState() {
        return mState;
    }

    public @NonNull Arguments getOutputArguments() {
        return mOutput;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkStatus that = (WorkStatus) o;

        if (!mId.equals(that.mId)) return false;
        if (mState != that.mState) return false;
        return mOutput.equals(that.mOutput);
    }

    @Override
    public int hashCode() {
        int result = mId.hashCode();
        result = 31 * result + mState.hashCode();
        result = 31 * result + mOutput.hashCode();
        return result;
    }
}
