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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.util.Collections;
import java.util.List;

class PageResult<T> {
    @SuppressWarnings("unchecked")
    private static final PageResult INVALID_RESULT =
            new PageResult(Collections.EMPTY_LIST, 0);

    @SuppressWarnings("unchecked")
    static <T> PageResult<T> getInvalidResult() {
        return INVALID_RESULT;
    }


    @Retention(SOURCE)
    @IntDef({INIT, APPEND, PREPEND, TILE})
    @interface ResultType {}

    static final int INIT = 0;

    // contiguous results
    static final int APPEND = 1;
    static final int PREPEND = 2;

    // non-contiguous, tile result
    static final int TILE = 3;

    @NonNull
    public final List<T> page;
    @SuppressWarnings("WeakerAccess")
    public final int leadingNulls;
    @SuppressWarnings("WeakerAccess")
    public final int trailingNulls;
    @SuppressWarnings("WeakerAccess")
    public final int positionOffset;

    PageResult(@NonNull List<T> list, int leadingNulls, int trailingNulls, int positionOffset) {
        this.page = list;
        this.leadingNulls = leadingNulls;
        this.trailingNulls = trailingNulls;
        this.positionOffset = positionOffset;
    }

    PageResult(@NonNull List<T> list, int positionOffset) {
        this.page = list;
        this.leadingNulls = 0;
        this.trailingNulls = 0;
        this.positionOffset = positionOffset;
    }

    @Override
    public String toString() {
        return "Result " + leadingNulls
                + ", " + page
                + ", " + trailingNulls
                + ", offset " + positionOffset;
    }

    public boolean isInvalid() {
        return this == INVALID_RESULT;
    }

    abstract static class Receiver<T> {
        @MainThread
        public abstract void onPageResult(@ResultType int type, @NonNull PageResult<T> pageResult);
    }
}
