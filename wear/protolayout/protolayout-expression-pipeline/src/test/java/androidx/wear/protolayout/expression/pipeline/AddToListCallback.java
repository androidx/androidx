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

package androidx.wear.protolayout.expression.pipeline;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class AddToListCallback<T> implements DynamicTypeValueReceiverWithPreUpdate<T> {
    private final List<T> mListToUpdate;
    @Nullable private final List<Boolean> mInvalidListToUpdate;

    private int mPreUpdateCallCounts = 0;
    private int mUpdateCallCount = 0;

    public AddToListCallback(List<T> list) {
        this.mListToUpdate = list;
        this.mInvalidListToUpdate = null;
    }

    public AddToListCallback(List<T> list, @Nullable List<Boolean> invalidList) {
        this.mListToUpdate = list;
        this.mInvalidListToUpdate = invalidList;
    }

    @Override
    public void onPreUpdate() { mPreUpdateCallCounts++; }

    @Override
    public void onData(@NonNull T newData) {
        mUpdateCallCount++;
        mListToUpdate.add(newData);
    }

    @Override
    public void onInvalidated() {
        mUpdateCallCount++;
        if (mInvalidListToUpdate != null) {
            mInvalidListToUpdate.add(true);
        }
    }

    public boolean isPreUpdateAndUpdateInSync() {
        return mPreUpdateCallCounts == mUpdateCallCount;
    }
}
