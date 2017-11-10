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

package androidx.recyclerview.selection;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static android.support.v4.util.Preconditions.checkArgument;

import static androidx.recyclerview.selection.Shared.VERBOSE;

import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

/**
 * Provides the necessary glue to notify RecyclerView when selection data changes,
 * and to notify SelectionHelper when the underlying RecyclerView.Adapter data changes.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@VisibleForTesting
public class EventBridge {

    private static final String TAG = "EventsRelays";

    /**
     * Installs the event bridge for on the supplied adapter/helper.
     *
     * @param adapter
     * @param selectionHelper
     * @param keyProvider
     * @param <K>
     */
    @VisibleForTesting
    public static <K> void install(
            RecyclerView.Adapter<?> adapter,
            SelectionHelper<K> selectionHelper,
            ItemKeyProvider<K> keyProvider) {
        new AdapterToSelectionHelper(adapter, selectionHelper);
        new SelectionHelperToAdapter<>(selectionHelper, keyProvider, adapter);
    }

    private static final class AdapterToSelectionHelper extends RecyclerView.AdapterDataObserver {

        private final SelectionHelper<?> mSelectionHelper;

        AdapterToSelectionHelper(
                RecyclerView.Adapter<?> adapter,
                SelectionHelper<?> selectionHelper) {
            adapter.registerAdapterDataObserver(this);

            checkArgument(selectionHelper != null);
            mSelectionHelper = selectionHelper;
        }

        @Override
        public void onChanged() {
            mSelectionHelper.onDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int startPosition, int itemCount, Object payload) {
            // No change in position. Ignore, since we assume
            // selection is a user driven activity. So changes
            // in properties of items shouldn't result in a
            // change of selection.
            // TODO: It is possible properties of items chould change to make them unselectable.
        }

        @Override
        public void onItemRangeInserted(int startPosition, int itemCount) {
            // Uninteresting to us since selection is stable ID based.
        }

        @Override
        public void onItemRangeRemoved(int startPosition, int itemCount) {
            // Uninteresting to us since selection is stable ID based.
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            // Uninteresting to us since selection is stable ID based.
        }
    }

    private static final class SelectionHelperToAdapter<K>
            extends SelectionHelper.SelectionObserver<K> {

        private final ItemKeyProvider<K> mKeyProvider;
        private final RecyclerView.Adapter<?> mAdapter;

        SelectionHelperToAdapter(
                SelectionHelper<K> selectionHelper,
                ItemKeyProvider<K> keyProvider,
                RecyclerView.Adapter<?> adapter) {

            selectionHelper.addObserver(this);

            checkArgument(keyProvider != null);
            checkArgument(adapter != null);

            mKeyProvider = keyProvider;
            mAdapter = adapter;
        }

        /**
         * Called when state of an item has been changed.
         */
        @Override
        public void onItemStateChanged(K key, boolean selected) {
            int position = mKeyProvider.getPosition(key);
            if (VERBOSE) Log.v(TAG, "ITEM " + key + " CHANGED at pos: " + position);

            if (position < 0) {
                Log.w(TAG, "Item change notification received for unknown item: " + key);
                return;
            }

            mAdapter.notifyItemChanged(position, SelectionHelper.SELECTION_CHANGED_MARKER);
        }
    }
}
