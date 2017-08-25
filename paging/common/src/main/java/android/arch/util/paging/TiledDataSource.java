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

import java.util.List;

/**
 * Position-based data loader for fixed size, arbitrary-page access load.
 *
 * @param <Type> Type of items being loaded by the TiledDataSource.
 */
public abstract class TiledDataSource<Type> extends DataSource<Integer, Type> {

    /**
     * Number of items that this DataSource can provide in total.
     *
     * @return Number of items this DataSource can provide. Must be <code>0</code> or greater.
     */
    @Override
    public abstract int loadCount();

    @Override
    boolean isContiguous() {
        return false;
    }

    /**
     * Called to load items at from the specified position range.
     *
     * @param startPosition Index of first item to load.
     * @param count         Exact number of items to load. Returning a different number will cause
     *                      an exception to be thrown.
     * @return List of loaded items. Null if the DataSource is no longer valid, and should
     *         not be queried again.
     */
    @SuppressWarnings("WeakerAccess")
    public abstract List<Type> loadRange(int startPosition, int count);
}
