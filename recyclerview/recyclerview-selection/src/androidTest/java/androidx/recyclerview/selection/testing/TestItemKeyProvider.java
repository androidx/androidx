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

package androidx.recyclerview.selection.testing;

import static androidx.core.util.Preconditions.checkArgument;

import androidx.recyclerview.selection.ItemKeyProvider;

/**
 * Provides RecyclerView selection code access to stable ids backed
 * by TestAdapter.
 */
public final class TestItemKeyProvider<K> extends ItemKeyProvider<K> {

    private final TestAdapter<K> mAdapter;

    public TestItemKeyProvider(@Scope int scope, TestAdapter<K> adapter) {
        super(scope);
        checkArgument(adapter != null);
        mAdapter = adapter;
    }

    @Override
    public K getKey(int position) {
        return mAdapter.getSelectionKey(position);
    }

    @Override
    public int getPosition(K key) {
        return mAdapter.getPosition(key);
    }
}
