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

package androidx.room;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * A helper class that maintains {@link RoomTrackingLiveData} instances for an
 * {@link InvalidationTracker}.
 * <p>
 * We keep a strong reference to active LiveData instances to avoid garbage collection in case
 * developer does not hold onto the returned LiveData.
 */
class InvalidationLiveDataContainer {
    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    final Set<LiveData> mLiveDataSet = Collections.newSetFromMap(
            new IdentityHashMap<LiveData, Boolean>()
    );
    private final RoomDatabase mDatabase;

    InvalidationLiveDataContainer(RoomDatabase database) {
        mDatabase = database;
    }

    <T> LiveData<T> create(String[] tableNames, Callable<T> computeFunction) {
        return new RoomTrackingLiveData<>(mDatabase, this, computeFunction, tableNames);
    }

    void onActive(LiveData liveData) {
        mLiveDataSet.add(liveData);
    }

    void onInactive(LiveData liveData) {
        mLiveDataSet.remove(liveData);
    }
}
