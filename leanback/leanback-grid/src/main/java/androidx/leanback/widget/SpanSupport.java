/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.leanback.widget;

import android.util.SparseIntArray;

import androidx.recyclerview.widget.GridLayoutManager;

/**
 * Wrapper around GridLayoutManager.SpanSizeLookUp, duplicates the cache implementation since
 * the cache methods are package private.
 */
final class SpanSupport {
    static final int FILL_ALL_SPANS_AND_PADDING = -1;
    static final int FILL_ALL_SPANS = -2;

    private final GridLayoutManager.SpanSizeLookup mSpanSizeLookup;
    private final SparseIntArray mSpanIndexCache = new SparseIntArray();
    private final SparseIntArray mSpanGroupIndexCache = new SparseIntArray();

    SpanSupport(GridLayoutManager.SpanSizeLookup spanSizeLookup) {
        mSpanSizeLookup = spanSizeLookup;
    }

    void invalidateSpanIndexCache() {
        mSpanIndexCache.clear();
    }

    void invalidateSpanGroupIndexCache() {
        mSpanGroupIndexCache.clear();
    }

    int getCachedSpanIndex(int position, int spanCount) {
        if (!mSpanSizeLookup.isSpanIndexCacheEnabled()) {
            // If app supplied SpanSizeLookup declares the cache is not enabled.  Then it is
            // the app's responsibility to make getSpanIndex() efficient.
            return mSpanSizeLookup.getSpanIndex(position, spanCount);
        }
        final int existing = mSpanIndexCache.get(position, -1);
        if (existing != -1) {
            return existing;
        }
        final int value = getSpanIndex(position, spanCount);
        mSpanIndexCache.put(position, value);
        return value;
    }

    int getCachedSpanGroupIndex(int position, int spanCount) {
        if (!mSpanSizeLookup.isSpanGroupIndexCacheEnabled()) {
            // If app supplied SpanSizeLookup declares the cache is not enabled.  Then it is
            // the app's responsibility to make getSpanGroupIndex() efficient.
            return mSpanSizeLookup.getSpanGroupIndex(position, spanCount);
        }
        final int existing = mSpanGroupIndexCache.get(position, -1);
        if (existing != -1) {
            return existing;
        }
        final int value = getSpanGroupIndex(position, spanCount);
        mSpanGroupIndexCache.put(position, value);
        return value;
    }

    // Returns FILL_ALL_SPANS_AND_PADDING or real span count.
    // FILL_ALL_SPANS_AND_PADDING will have a special treatment in GridLayoutManager.
    public int getSpanSize(int position, int spanCount) {
        int size = mSpanSizeLookup.getSpanSize(position);
        if (size == FILL_ALL_SPANS) {
            size = spanCount;
        }
        return size;
    }

    private int getRealSpanSize(int position, int spanCount) {
        int size = mSpanSizeLookup.getSpanSize(position);
        if (size == FILL_ALL_SPANS_AND_PADDING || size == FILL_ALL_SPANS) {
            return spanCount;
        }
        return size;
    }

    // Implementation using built-in cache.
    private int getSpanIndex(int position, int spanCount) {
        int positionSpanSize = getRealSpanSize(position, spanCount);
        if (positionSpanSize == spanCount) {
            return 0; // quick return for full-span items
        }
        int span = 0;
        int startPos = 0;
        int prevKey = findFirstKeyLessThan(mSpanIndexCache, position);
        if (prevKey >= 0) {
            span = mSpanIndexCache.get(prevKey) + getRealSpanSize(prevKey, spanCount);
            startPos = prevKey + 1;
        }
        for (int i = startPos; i < position; i++) {
            int size = getRealSpanSize(i, spanCount);
            span += size;
            if (span == spanCount) {
                span = 0;
            } else if (span > spanCount) {
                // did not fit, moving to next row / column
                span = size;
            }
        }
        if (span + positionSpanSize <= spanCount) {
            return span;
        }
        return 0;
    }

    static int findFirstKeyLessThan(SparseIntArray cache, int position) {
        int lo = 0;
        int hi = cache.size() - 1;

        while (lo <= hi) {
            // Using unsigned shift here to divide by two because it is guaranteed to not
            // overflow.
            final int mid = (lo + hi) >>> 1;
            final int midVal = cache.keyAt(mid);
            if (midVal < position) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        int index = lo - 1;
        if (index >= 0 && index < cache.size()) {
            return cache.keyAt(index);
        }
        return -1;
    }

    // Implementation using built-in cache.
    private int getSpanGroupIndex(int adapterPosition, int spanCount) {
        int span = 0;
        int group = 0;
        int start = 0;
        // This finds the first non empty cached group cache key.
        int prevKey = findFirstKeyLessThan(mSpanGroupIndexCache, adapterPosition);
        if (prevKey != -1) {
            group = mSpanGroupIndexCache.get(prevKey);
            start = prevKey + 1;
            span = getCachedSpanIndex(prevKey, spanCount)
                    + getRealSpanSize(prevKey, spanCount);
            if (span == spanCount) {
                span = 0;
                group++;
            }
        }
        int positionSpanSize = getRealSpanSize(adapterPosition, spanCount);
        for (int i = start; i < adapterPosition; i++) {
            int size = getRealSpanSize(i, spanCount);
            span += size;
            if (span == spanCount) {
                span = 0;
                group++;
            } else if (span > spanCount) {
                // did not fit, moving to next row / column
                span = size;
                group++;
            }
        }
        if (span + positionSpanSize > spanCount) {
            group++;
        }
        return group;
    }
}
