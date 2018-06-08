/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.paging;

import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

// NOTE: Room 1.0 depends on this class, so it should not be removed until
// we can require a version of Room that uses DataSource.Factory directly

/**
 * @hide
 * @deprecated Do not use this class.
 */
@Deprecated
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class LivePagedListProvider<Key, Value> extends DataSource.Factory<Key, Value> {

    @Override
    public DataSource<Key, Value> create() {
        return createDataSource();
    }

    @WorkerThread
    protected abstract DataSource<Key, Value> createDataSource();
}
