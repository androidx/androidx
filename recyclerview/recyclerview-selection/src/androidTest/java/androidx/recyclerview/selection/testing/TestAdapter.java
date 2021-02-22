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

import static org.junit.Assert.assertTrue;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestAdapter<K> extends Adapter<TestHolder> {

    private final List<K> mItems = new ArrayList<>();
    private final List<Integer> mNotifiedOfSelection = new ArrayList<>();
    private final AdapterDataObserver mAdapterObserver;

    public TestAdapter() {
        this(Collections.<K>emptyList());
    }

    public TestAdapter(List<K> items) {
        this(items, false);
    }

    public TestAdapter(List<K> items, boolean hasStableIds) {
        mItems.addAll(items);
        setHasStableIds(hasStableIds);
        mAdapterObserver = new RecyclerView.AdapterDataObserver() {

            @Override
            public void onChanged() {
            }

            @Override
            public void onItemRangeChanged(int startPosition, int itemCount, Object payload) {
                if (SelectionTracker.SELECTION_CHANGED_MARKER.equals(payload)) {
                    int last = startPosition + itemCount;
                    for (int i = startPosition; i < last; i++) {
                        mNotifiedOfSelection.add(i);
                    }
                }
            }

            @Override
            public void onItemRangeInserted(int startPosition, int itemCount) {
            }

            @Override
            public void onItemRangeRemoved(int startPosition, int itemCount) {
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                throw new UnsupportedOperationException();
            }
        };

        registerAdapterDataObserver(mAdapterObserver);
    }

    @Override
    public @NonNull TestHolder onCreateViewHolder(@NonNull ViewGroup view, int viewType) {
        return new TestHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TestHolder holder, int position) {
        // Ignore calls to this method which is called when bindViewHolder
        // is called. Some tests depend on bindViewHolder setting up position
        // and id information in ViewHolders.
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void updateTestModelIds(List<K> items) {
        mItems.clear();
        mItems.addAll(items);

        notifyDataSetChanged();
    }

    public int getPosition(K key) {
        return mItems.indexOf(key);
    }

    public K getSelectionKey(int position) {
        return mItems.get(position);
    }

    public boolean removeItem(K key) {
        int position = getPosition(key);
        if (position == RecyclerView.NO_POSITION) {
            return false;
        }

        @Nullable K removed = mItems.remove(position);
        notifyItemRemoved(position);
        return removed != null;
    }

    public void resetSelectionNotifications() {
        mNotifiedOfSelection.clear();
    }

    public void assertNotifiedOfSelectionChange(int position) {
        assertTrue(mNotifiedOfSelection.contains(position));
    }

    /*
     * Returns a reference to internal item list. Obvi for test only.
     */
    public List<K> getItems() {
        return mItems;
    }

    public static TestAdapter<String> createStringAdapter(int num) {
        List<String> items = new ArrayList<>(num);
        for (int i = 0; i < num; ++i) {
            items.add(Integer.toString(i));
        }
        return new TestAdapter<>(items);
    }
}
