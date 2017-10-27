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

import android.support.annotation.AnyThread;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;

class PageResult<K, V> {
    static final int INIT = 0;

    // contiguous results
    static final int APPEND = 1;
    static final int PREPEND = 2;

    // non-contiguous, tile result
    static final int TILE = 3;

    public final int type;
    public final Page<K, V> page;
    @SuppressWarnings("WeakerAccess")
    public final int leadingNulls;
    @SuppressWarnings("WeakerAccess")
    public final int trailingNulls;
    @SuppressWarnings("WeakerAccess")
    public final int positionOffset;

    PageResult(int type, Page<K, V> page, int leadingNulls, int trailingNulls, int positionOffset) {
        this.type = type;
        this.page = page;
        this.leadingNulls = leadingNulls;
        this.trailingNulls = trailingNulls;
        this.positionOffset = positionOffset;
    }

    PageResult(int type) {
        this.type = type;
        this.page = null;
        this.leadingNulls = 0;
        this.trailingNulls = 0;
        this.positionOffset = 0;
    }

    interface Receiver<K, V> {
        @AnyThread
        void postOnPageResult(@NonNull PageResult<K, V> pageResult);
        @MainThread
        void onPageResult(@NonNull PageResult<K, V> pageResult);
    }
}
