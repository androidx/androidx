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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.annotation.VisibleForTesting.PACKAGE_PRIVATE;
import static androidx.core.util.Preconditions.checkArgument;
import static androidx.recyclerview.selection.Shared.VERBOSE;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.RecyclerView;

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
@VisibleForTesting(otherwise = PACKAGE_PRIVATE)
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

        // setup bridges to relay selection and adapter events
        new TrackerToAdapterBridge<>(selectionTracker, keyProvider, adapter);
        adapter.registerAdapterDataObserver(selectionTracker.getAdapterDataObserver());
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

    private EventBridge() {
    }
}
