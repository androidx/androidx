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

package androidx.work.impl;

import android.net.Network;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import androidx.work.Data;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Extra information to setup Workers.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Extras {

    private @NonNull Data mInputData;
    private @NonNull Set<String> mTags;
    private @NonNull RuntimeExtras mRuntimeExtras;
    private int mRunAttemptCount;
    private @NonNull Executor mBackgroundExecutor;

    public Extras(@NonNull Data inputData,
            @NonNull Collection<String> tags,
            @NonNull RuntimeExtras runtimeExtras,
            int runAttemptCount,
            @NonNull Executor backgroundExecutor) {
        mInputData = inputData;
        mTags = new HashSet<>(tags);
        mRuntimeExtras = runtimeExtras;
        mRunAttemptCount = runAttemptCount;
        mBackgroundExecutor = backgroundExecutor;
    }

    public @NonNull Data getInputData() {
        return mInputData;
    }

    public @NonNull Set<String> getTags() {
        return mTags;
    }

    public @NonNull RuntimeExtras getRuntimeExtras() {
        return mRuntimeExtras;
    }

    public int getRunAttemptCount() {
        return mRunAttemptCount;
    }

    public @NonNull Executor getBackgroundExecutor() {
        return mBackgroundExecutor;
    }

    /**
     * Extra runtime information for Workers.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class RuntimeExtras {
        public String[] triggeredContentAuthorities;
        public Uri[] triggeredContentUris;

        @RequiresApi(28)
        public Network network;
    }
}
