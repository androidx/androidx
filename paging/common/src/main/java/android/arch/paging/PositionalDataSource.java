/*
 * Copyright 2017 The Android Open Source Project
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
import android.support.annotation.WorkerThread;

import java.util.Collections;

/**
 * Position-based data loader for a fixed-size, countable data set, supporting loads at arbitrary
 * positions.
 * <p>
 * Extend PositionalDataSource if you can support counting your data set, and loading based on
 * position information.
 * <p>
 * Note that unless {@link PagedList.Config#enablePlaceholders placeholders are disabled}
 * PositionalDataSource requires counting the size of the dataset. This allows pages to be tiled in
 * at arbitrary, non-contiguous locations based upon what the user observes in a {@link PagedList}.
 * <p>
 * Room can generate a Factory of PositionalDataSources for you:
 * <pre>
 * {@literal @}Dao
 * interface UserDao {
 *     {@literal @}Query("SELECT * FROM user ORDER BY mAge DESC")
 *     public abstract DataSource.Factory&lt;Integer, User> loadUsersByAgeDesc();
 * }</pre>
 *
 * @param <T> Type of items being loaded by the PositionalDataSource.
 */
public abstract class PositionalDataSource<T> extends DataSource<Integer, T> {

    /**
     * Load initial list data.
     * <p>
     * This method is called to load the initial page(s) from the DataSource.
     * <p>
     * Result list must be a multiple of pageSize to enable efficient tiling.
     *
     * @param requestedStartPosition Initial load position requested. Note that this may not be
     *                               within the bounds of your data set, it should be corrected
     *                               before you make your query.
     * @param requestedLoadSize Requested number of items to load. Note that this may be larger than
     *                          available data.
     * @param pageSize Defines page size acceptable for return values. List of items passed to the
     *                 callback must be an integer multiple of page size.
     * @param callback DataSource.InitialLoadCallback that receives initial load data, including
     *                 position and total data set size.
     */
    @WorkerThread
    public abstract void loadInitial(int requestedStartPosition, int requestedLoadSize,
            int pageSize, @NonNull InitialLoadCallback<T> callback);

    /**
     * Called to load a range of data from the DataSource.
     * <p>
     * This method is called to load additional pages from the DataSource after the
     * InitialLoadCallback passed to loadInitial has initialized a PagedList.
     * <p>
     * Unlike {@link #loadInitial(int, int, int, InitialLoadCallback)}, this method must return the
     * number of items requested, at the position requested.
     *
     * @param startPosition Initial load position.
     * @param count Number of items to load.
     * @param callback DataSource.LoadCallback that receives loaded data.
     */
    @WorkerThread
    public abstract void loadRange(int startPosition, int count,
            @NonNull LoadCallback<T> callback);

    @Override
    boolean isContiguous() {
        return false;
    }

    @NonNull
    ContiguousDataSource<Integer, T> wrapAsContiguousWithoutPlaceholders() {
        return new ContiguousWithoutPlaceholdersWrapper<>(this);
    }

    static int computeFirstLoadPosition(int position, int firstLoadSize, int pageSize, int size) {
        int roundedPageStart = Math.round(position / pageSize) * pageSize;

        // maximum start pos is that which will encompass end of list
        int maximumLoadPage = ((size - firstLoadSize + pageSize - 1) / pageSize) * pageSize;
        roundedPageStart = Math.min(maximumLoadPage, roundedPageStart);

        // minimum start position is 0
        roundedPageStart = Math.max(0, roundedPageStart);

        return roundedPageStart;
    }

    @SuppressWarnings("deprecation")
    static class ContiguousWithoutPlaceholdersWrapper<Value>
            extends ContiguousDataSource<Integer, Value> {

        @NonNull
        final PositionalDataSource<Value> mPositionalDataSource;

        ContiguousWithoutPlaceholdersWrapper(
                @NonNull PositionalDataSource<Value> positionalDataSource) {
            mPositionalDataSource = positionalDataSource;
        }

        @Override
        public void loadInitial(@Nullable Integer position, int initialLoadSize,
                boolean enablePlaceholders, @NonNull InitialLoadCallback<Value> callback) {
            final int convertPosition = position == null ? 0 : position;

            // Note enablePlaceholders will be false here, but we don't have a way to communicate
            // this to PositionalDataSource. This is fine, because only the list and its position
            // offset will be consumed by the InitialLoadCallback.
            mPositionalDataSource.loadInitial(
                    convertPosition, initialLoadSize, initialLoadSize, callback);
        }

        @Override
        void loadAfter(int currentEndIndex, @NonNull Value currentEndItem, int pageSize,
                @NonNull LoadCallback<Value> callback) {
            int startIndex = currentEndIndex + 1;
            mPositionalDataSource.loadRange(startIndex, pageSize, callback);
        }

        @Override
        void loadBefore(int currentBeginIndex, @NonNull Value currentBeginItem, int pageSize,
                @NonNull LoadCallback<Value> callback) {
            int startIndex = currentBeginIndex - 1;
            if (startIndex < 0) {
                callback.onResult(Collections.<Value>emptyList());
            } else {
                int loadSize = Math.min(pageSize, startIndex + 1);
                startIndex = startIndex - loadSize + 1;
                mPositionalDataSource.loadRange(startIndex, loadSize, callback);
            }
        }

        @Override
        Integer getKey(int position, Value item) {
            return position;
        }
    }
}
