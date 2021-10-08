/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.enterprise.feedback;

import androidx.annotation.Nullable;

/**
 * Merge multiple {@link KeyedAppStatesCallback} instances into a single one.
 *
 * <p>This will report success once {@code numReceivers} success results have been received.
 *
 * <p>It will report an error once a single non-success result is received.
 */
class KeyedAppStatesCallbackMerger implements KeyedAppStatesCallback {

    private boolean mHasReported = false;
    private int mSuccesses;
    private final int mNumReceivers;
    private final KeyedAppStatesCallback mOriginalCallback;

    KeyedAppStatesCallbackMerger(int numReceivers, KeyedAppStatesCallback originalCallback) {
        mNumReceivers = numReceivers;
        mOriginalCallback = originalCallback;

        if (mNumReceivers == 0) {
            mHasReported = true;
            mOriginalCallback.onResult(STATUS_SUCCESS, /* throwable= */ null);
        }
    }

    @Override
    public void onResult(int state, @Nullable Throwable throwable) {
        if (mHasReported) {
            // Only report once
            return;
        }

        if (state != STATUS_SUCCESS || ++mSuccesses >= mNumReceivers) {
            mHasReported = true;
            mOriginalCallback.onResult(state, throwable);
        }
    }
}
