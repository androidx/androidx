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

package android.arch.paging;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;

public class StringPagedList extends PagedList<String> implements PagedStorage.Callback {
    StringPagedList(int leadingNulls, int trailingNulls, String... items) {
        super(new PagedStorage<Integer, String>(),
                null, null, null);
        PagedStorage<Integer, String> keyedStorage = (PagedStorage<Integer, String>) mStorage;
        keyedStorage.init(leadingNulls,
                new Page<Integer, String>(null, Arrays.asList(items), null),
                trailingNulls,
                0,
                this);
    }

    @Override
    boolean isContiguous() {
        return true;
    }

    @Nullable
    @Override
    public Object getLastKey() {
        return null;
    }

    @Override
    protected void dispatchUpdatesSinceSnapshot(@NonNull PagedList<String> storageSnapshot,
            @NonNull Callback callback) {
    }

    @Override
    protected void loadAroundInternal(int index) {
    }

    @Override
    public void onInitialized(int count) {
    }

    @Override
    public void onPagePrepended(int leadingNulls, int changed, int added) {
    }

    @Override
    public void onPageAppended(int endPosition, int changed, int added) {
    }

    @Override
    public void onPagePlaceholderInserted(int pageIndex) {
    }

    @Override
    public void onPageInserted(int start, int count) {
    }
}
