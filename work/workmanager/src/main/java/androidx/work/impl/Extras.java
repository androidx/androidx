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
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import androidx.work.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extra information to setup Workers.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Extras {

    private @NonNull Data mInputData;
    private @NonNull Set<String> mTags;
    private @Nullable RuntimeExtras mRuntimeExtras;
    private int mRunAttemptCount;

    public Extras(@NonNull Data inputData,
            @NonNull List<String> tags,
            @Nullable RuntimeExtras runtimeExtras,
            int runAttemptCount) {
        mInputData = inputData;
        mTags = new HashSet<>(tags);
        mRuntimeExtras = runtimeExtras;
        mRunAttemptCount = runAttemptCount;
    }

    public @NonNull Data getInputData() {
        return mInputData;
    }

    public @NonNull Set<String> getTags() {
        return mTags;
    }

    public @Nullable RuntimeExtras getRuntimeExtras() {
        return mRuntimeExtras;
    }

    public int getRunAttemptCount() {
        return mRunAttemptCount;
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
