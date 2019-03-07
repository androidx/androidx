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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Utility class that create LiveData instances which stay in memory as long as there is an
 * active observer.
 * see: b/74477406 for details.
 */
class WorkManagerLiveDataTracker {
    @VisibleForTesting
    final Set<LiveData> mLiveDataSet = Collections.newSetFromMap(
            new IdentityHashMap<LiveData, Boolean>());

    public <T> LiveData<T> track(LiveData<T> other) {
        return new TrackedLiveData<>(this, other);
    }

    void onActive(LiveData liveData) {
        mLiveDataSet.add(liveData);
    }

    void onInactive(LiveData liveData) {
        mLiveDataSet.remove(liveData);
    }

    static class TrackedLiveData<T> extends MediatorLiveData<T> {
        private final WorkManagerLiveDataTracker mContainer;
        TrackedLiveData(WorkManagerLiveDataTracker container, LiveData<T> wrapped) {
            mContainer = container;
            addSource(wrapped, new Observer<T>() {
                @Override
                public void onChanged(@Nullable T t) {
                    setValue(t);
                }
            });
        }

        @Override
        protected void onActive() {
            super.onActive();
            mContainer.onActive(this);
        }


        @Override
        protected void onInactive() {
            super.onInactive();
            mContainer.onInactive(this);
        }
    }
}
