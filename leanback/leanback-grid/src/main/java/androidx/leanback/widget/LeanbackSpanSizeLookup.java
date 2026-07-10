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

import org.jspecify.annotations.NonNull;

/**
 * A helper class to provide the number of spans each item occupies. {@link BaseGridView} retrieves
 * the object from {@link androidx.recyclerview.widget.RecyclerView.Adapter} via
 * {@link FacetProvider} interface implemented by the adapter.
 * The class is similar to {@link androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup}.
 * It supports two special span size: {@link #FILL_ALL_SPANS_AND_PADDINGS} and
 * {@link #FILL_ALL_SPANS}.
 */
public abstract class LeanbackSpanSizeLookup {
    /**
     * Value returned by {@link #getSpanSize(int)}: fill all spans and padding area, it takes full
     * width of a vertical grid view or full height of a horizontal grid view.
     */
    public static final int FILL_ALL_SPANS_AND_PADDINGS = -1;

    /**
     * Value returned by {@link #getSpanSize(int)}:  fill all spans.  It's equivalent of returning
     * the spanCount.
     */
    public static final int FILL_ALL_SPANS = -2;

    private final SparseIntArray mSpanIndexCache = new SparseIntArray();
    private final SparseIntArray mSpanGroupIndexCache = new SparseIntArray();
    private boolean mCacheSpanIndices = false;
    private boolean mCacheSpanGroupIndices = false;

    /**
     * Returns the number of spans this item will take, or {@link #FILL_ALL_SPANS_AND_PADDINGS},
     * {@link #FILL_ALL_SPANS}.
     * @param position The adapter position of the item.
     * @return The number of spans this item will take, or {@link #FILL_ALL_SPANS_AND_PADDINGS},
     * {@link #FILL_ALL_SPANS}.
     */
    public abstract int getSpanSize(int position);

    /**
     * Clears the span index cache. GridLayoutManager automatically calls this method when
     * adapter changes occur.
     */
    public void invalidateSpanIndexCache() {
        mSpanIndexCache.clear();
    }

    /**
     * Clears the span group index cache. GridLayoutManager automatically calls this method
     * when adapter changes occur.
     */
    public void invalidateSpanGroupIndexCache() {
        mSpanGroupIndexCache.clear();
    }

    /**
     * Sets whether the results of {@link #getSpanIndex(int, int)} method should be cached or
     * not. By default these values are not cached. If you are not overriding
     * {@link #getSpanIndex(int, int)} with something highly performant, you should set this
     * to true for better performance.
     *
     * @param cacheSpanIndices Whether results of getSpanIndex should be cached or not.
     */
    public void setSpanIndexCacheEnabled(boolean cacheSpanIndices) {
        if (!cacheSpanIndices) {
            mSpanGroupIndexCache.clear();
        }
        mCacheSpanIndices = cacheSpanIndices;
    }

    /**
     * Returns whether results of {@link #getSpanIndex(int, int)} method are cached or not.
     *
     * @return True if results of {@link #getSpanIndex(int, int)} are cached.
     */
    public boolean isSpanIndexCacheEnabled() {
        return mCacheSpanIndices;
    }

    /**
     * Returns whether results of {@link #getSpanGroupIndex(int, int)} method are cached or not.
     *
     * @return True if results of {@link #getSpanGroupIndex(int, int)} are cached.
     */
    public boolean isSpanGroupIndexCacheEnabled() {
        return mCacheSpanGroupIndices;
    }

    /**
     * Sets whether the results of {@link #getSpanGroupIndex(int, int)} method should be cached
     * or not. By default these values are not cached. If you are not overriding
     * {@link #getSpanGroupIndex(int, int)} with something highly performant, and you are using
     * spans to calculate scrollbar offset and range, you should set this to true for better
     * performance.
     *
     * @param cacheSpanGroupIndices Whether results of getGroupSpanIndex should be cached or
     *                              not.
     */
    public void setSpanGroupIndexCacheEnabled(boolean cacheSpanGroupIndices)  {
        if (!cacheSpanGroupIndices) {
            mSpanGroupIndexCache.clear();
        }
        mCacheSpanGroupIndices = cacheSpanGroupIndices;
    }

    int getCachedSpanIndex(int position, int spanCount) {
        if (!mCacheSpanIndices) {
            // If app supplied SpanSizeLookup declares the cache is not enabled.  Then it is
            // the app's responsibility to make getSpanIndex() efficient.
            return getSpanIndex(position, spanCount);
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
        if (!mCacheSpanGroupIndices) {
            // If app supplied SpanSizeLookup declares the cache is not enabled.  Then it is
            // the app's responsibility to make getSpanGroupIndex() efficient.
            return getSpanGroupIndex(position, spanCount);
        }
        final int existing = mSpanGroupIndexCache.get(position, -1);
        if (existing != -1) {
            return existing;
        }
        final int value = getSpanGroupIndex(position, spanCount);
        mSpanGroupIndexCache.put(position, value);
        return value;
    }

    /**
     * Returns the number of spans this item will take, or {@link #FILL_ALL_SPANS_AND_PADDINGS}.
     * Default implementation calls {@link #getSpanSize(int)} without spanCount. Subclass may
     * override this method to return span size based on spanCount.
     * @param position The adapter position of the item.
     * @param spanCount The total available spans of the grid.
     * @return The number of spans this item will take, or {@link #FILL_ALL_SPANS_AND_PADDINGS}.
     */
    public int getSpanSize(int position, int spanCount) {
        int size = getSpanSize(position);
        if (size == FILL_ALL_SPANS) {
            size = spanCount;
        }
        return size;
    }

    private int getRealSpanSize(int position, int spanCount) {
        int size = getSpanSize(position);
        if (size == FILL_ALL_SPANS_AND_PADDINGS || size == FILL_ALL_SPANS) {
            return spanCount;
        }
        return size;
    }

    /**
     * Returns the final span index of the provided position.
     * <p>
     * If {@link VerticalGridView}, this is a column value.
     * If {@link HorizontalGridView}, this is a row value.
     * <p>
     * If you have a faster way to calculate span index for your items, you should override
     * this method. Otherwise, you should enable span index cache
     * ({@link #setSpanIndexCacheEnabled(boolean)}) for better performance. When caching is
     * disabled, default implementation traverses all items from 0 to
     * <code>position</code>. When caching is enabled, it calculates from the closest cached
     * value before the <code>position</code>.
     * <p>
     * If you override this method, you need to make sure it is consistent with
     * {@link #getSpanSize(int)}. GridLayoutManager does not call this method for
     * each item. It is called only for the reference item and rest of the items
     * are assigned to spans based on the reference item. For example, you cannot assign a
     * position to span 2 while span 1 is empty.
     * <p>
     * Note that span offsets always start with 0 and are not affected by RTL.
     *
     * @param position  The position of the item
     * @param spanCount The total number of spans in the grid
     * @return The final span position of the item. Should be between 0 (inclusive) and
     * <code>spanCount</code>(exclusive)
     */
    public int getSpanIndex(int position, int spanCount) {
        int positionSpanSize = getRealSpanSize(position, spanCount);
        if (positionSpanSize == spanCount) {
            return 0; // quick return for full-span items
        }
        int span = 0;
        int startPos = 0;
        // If caching is enabled, try to jump
        if (isSpanIndexCacheEnabled()) {
            int prevKey = findFirstKeyLessThan(mSpanIndexCache, position);
            if (prevKey >= 0) {
                span = mSpanIndexCache.get(prevKey) + getRealSpanSize(prevKey, spanCount);
                startPos = prevKey + 1;
            }
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

    /**
     * Returns the index of the group this position belongs.
     * <p>
     * If {@link VerticalGridView}, this is a row value.
     * If {@link HorizontalGridView}, this is a column value.
     * <p>
     * For example, if grid has 3 columns and each item occupies 1 span, span group index
     * for item 1 will be 0, item 5 will be 1.
     *
     * @param adapterPosition The position in adapter
     * @param spanCount The total number of spans in the grid
     * @return The index of the span group including the item at the given adapter position
     */
    public int getSpanGroupIndex(int adapterPosition, int spanCount) {
        int span = 0;
        int group = 0;
        int start = 0;
        if (isSpanGroupIndexCacheEnabled()) {
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

    /** Return the default LeanbackSpanSizeLookup that each item has a span size of 1. */
    @NonNull
    public static LeanbackSpanSizeLookup getDefault() {
        return DefaultSpanSizeLookup.INSTANCE;
    }

    /**
     * The default SpanSizeLookup that has one span.
     */
    static final class DefaultSpanSizeLookup extends LeanbackSpanSizeLookup {
        private DefaultSpanSizeLookup() {}

        @Override
        public int getSpanSize(int position) {
            return 1;
        }

        @NonNull
        static final DefaultSpanSizeLookup INSTANCE = new DefaultSpanSizeLookup();
    }
}
