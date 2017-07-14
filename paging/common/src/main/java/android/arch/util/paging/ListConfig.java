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

package android.arch.util.paging;

/**
 * Configuration object for list paging. Used to inform how and when a {@link LazyList} loads
 * content from a {@link CountedDataSource}.
 */
public class ListConfig {
    final int mPageSize;
    final int mPrefetchDistance;

    ListConfig(int pageSize, int prefetchDistance) {
        mPageSize = pageSize;
        mPrefetchDistance = prefetchDistance;
    }

    /**
     * Creates a {@link Builder} that can be used to construct a {@link ListConfig}.
     *
     * @return a new {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for {@link ListConfig}.
     * <p>
     * You must at minimum specify a {@link Builder#pageSize(int)}.
     */
    public static class Builder {
        private int mPageSize = -1;
        private int mPrefetchDistance = -1;

        /**
         * Defines the number of items loaded at once from the DataSource.
         * <p>
         * Should be several times the number of visible items onscreen.
         */
        public Builder pageSize(int pageSize) {
            this.mPageSize = pageSize;
            return this;
        }

        /**
         * Defines how far from the edge of loaded content an access must be to trigger further
         * loading. Defaults to page size.
         * <p>
         * A value of 0 indicates that no list items will be loaded before they are first requested.
         * <p>
         * Should be several times the number of visible items onscreen.
         */
        public Builder prefetchDistance(int prefetchDistance) {
            this.mPrefetchDistance = prefetchDistance;
            return this;
        }

        /**
         * Creates a {@link ListConfig} with the given parameters.
         *
         * @return A new ListConfig.
         */
        public ListConfig create() {
            if (mPageSize < 1) {
                throw new IllegalArgumentException("Page size must be a positive number");
            }
            if (mPrefetchDistance < 0) {
                mPrefetchDistance = mPageSize;
            }
            return new ListConfig(mPageSize, mPrefetchDistance);
        }
    }
}
