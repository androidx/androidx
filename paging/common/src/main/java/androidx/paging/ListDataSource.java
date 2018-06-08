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

package androidx.paging;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

class ListDataSource<T> extends PositionalDataSource<T> {
    private final List<T> mList;

    public ListDataSource(List<T> list) {
        mList = new ArrayList<>(list);
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams params,
            @NonNull LoadInitialCallback<T> callback) {
        final int totalCount = mList.size();

        final int position = computeInitialLoadPosition(params, totalCount);
        final int loadSize = computeInitialLoadSize(params, position, totalCount);

        // for simplicity, we could return everything immediately,
        // but we tile here since it's expected behavior
        List<T> sublist = mList.subList(position, position + loadSize);
        callback.onResult(sublist, position, totalCount);
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params,
            @NonNull LoadRangeCallback<T> callback) {
        callback.onResult(mList.subList(params.startPosition,
                params.startPosition + params.loadSize));
    }
}
