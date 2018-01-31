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

package androidx.widget.recyclerview.selection;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static android.support.v4.util.Preconditions.checkArgument;

import static androidx.widget.recyclerview.selection.Shared.VERBOSE;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

/**
 * Provides the necessary glue to notify RecyclerView when selection data changes,
 * and to notify SelectionTracker when the underlying RecyclerView.Adapter data changes.
 *
 * This strict decoupling is necessary to permit a single SelectionTracker to work
 * with multiple RecyclerView instances. This may be necessary when multiple
 * different views of data are presented to the user.
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
     * @param selectionTracker
     * @param keyProvider
     *
     * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
     */
    public static <K> void install(
            @NonNull RecyclerView.Adapter<?> adapter,
            @NonNull SelectionTracker<K> selectionTracker,
            @NonNull ItemKeyProvider<K> keyProvider) {

        // setup bridges to relay selection events.
        new AdapterToTrackerBridge(adapter, selectionTracker);
        new TrackerToAdapterBridge<>(selectionTracker, keyProvider, adapter);
    }

    private static final class AdapterToTrackerBridge extends RecyclerView.AdapterDataObserver {

        private final SelectionTracker<?> mSelectionTracker;

        AdapterToTrackerBridge(
                @NonNull RecyclerView.Adapter<?> adapter,
                @NonNull SelectionTracker<?> selectionTracker) {
            adapter.registerAdapterDataObserver(this);

            checkArgument(selectionTracker != null);
            mSelectionTracker = selectionTracker;
        }

        @Override
        public void onChanged() {
            mSelectionTracker.onDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int startPosition, int itemCount, @Nullable Object payload) {
            // No change in position. Ignore.
            // TODO(b/72393576): Properties of items could change. Should reevaluate selected status
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

    private static final class TrackerToAdapterBridge<K>
            extends SelectionTracker.SelectionObserver<K> {

        private final ItemKeyProvider<K> mKeyProvider;
        private final RecyclerView.Adapter<?> mAdapter;

        TrackerToAdapterBridge(
                @NonNull SelectionTracker<K> selectionTracker,
                @NonNull ItemKeyProvider<K> keyProvider,
                @NonNull RecyclerView.Adapter<?> adapter) {

            selectionTracker.addObserver(this);

            checkArgument(keyProvider != null);
            checkArgument(adapter != null);

            mKeyProvider = keyProvider;
            mAdapter = adapter;
        }

        /**
         * Called when state of an item has been changed.
         */
        @Override
        public void onItemStateChanged(@NonNull K key, boolean selected) {
            int position = mKeyProvider.getPosition(key);
            if (VERBOSE) Log.v(TAG, "ITEM " + key + " CHANGED at pos: " + position);

            if (position < 0) {
                Log.w(TAG, "Item change notification received for unknown item: " + key);
                return;
            }

            mAdapter.notifyItemChanged(position, SelectionTracker.SELECTION_CHANGED_MARKER);
        }
    }
}
