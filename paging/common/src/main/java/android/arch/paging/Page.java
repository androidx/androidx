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

import java.util.List;

/**
 * Immutable class representing a page of data loaded from a DataSource.
 * <p>
 * Optionally stores before/after keys for cases where they cannot be computed, but the DataSource
 * can provide them as part of loading a page.
 * <p>
 * A page's list must never be modified.
 */
class Page<K, V> {
    @SuppressWarnings("WeakerAccess")
    @Nullable
    public final K beforeKey;
    @NonNull
    public final List<V> items;
    @SuppressWarnings("WeakerAccess")
    @Nullable
    public K afterKey;

    Page(@NonNull List<V> items) {
        this(null, items, null);
    }

    Page(@Nullable K beforeKey, @NonNull List<V> items, @Nullable K afterKey) {
        this.beforeKey = beforeKey;
        this.items = items;
        this.afterKey = afterKey;
    }
}
