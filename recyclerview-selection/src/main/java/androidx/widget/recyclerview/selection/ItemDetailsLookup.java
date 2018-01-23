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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;

/**
 * Provides selection library and event handlers access to details about view items
 * presented by a {@link RecyclerView} instance. Implementations of this class provide
 * supplementary information about view holders used to make selection policy decisions.
 *
 * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
 */
public abstract class ItemDetailsLookup<K> {

    /**
     * @return true if there is an item at the event coordinates.
     */
    public boolean overItem(@NonNull MotionEvent e) {
        return getItemPosition(e) != RecyclerView.NO_POSITION;
    }

    /**
     * @return true if there is an item w/ a stable ID at the event coordinates.
     */
    public boolean overItemWithSelectionKey(@NonNull MotionEvent e) {
        return overItem(e) && hasSelectionKey(getItemDetails(e));
    }

    /**
     * @return true if the event coordinates are in an area of the item
     * that can result in dragging the item. List items frequently have a white
     * area that is not draggable allowing band selection to be initiated
     * in that area.
     */
    public boolean inItemDragRegion(@NonNull MotionEvent e) {
        return overItem(e) && getItemDetails(e).inDragRegion(e);
    }

    /**
     * @return true if the event coordinates are in a "selection hot spot"
     * region of an item. Contact in these regions result in immediate
     * selection, even when there is no existing selection.
     */
    public boolean inItemSelectRegion(@NonNull MotionEvent e) {
        return overItem(e) && getItemDetails(e).inSelectionHotspot(e);
    }

    /**
     * @return the adapter position of the item at the event coordinates.
     */
    public int getItemPosition(@NonNull MotionEvent e) {
        @Nullable ItemDetails<?> item = getItemDetails(e);
        return item != null
                ? item.getPosition()
                : RecyclerView.NO_POSITION;
    }

    private static boolean hasSelectionKey(@Nullable ItemDetails<?> item) {
        return item != null && item.getSelectionKey() != null;
    }

    private static boolean hasPosition(@Nullable ItemDetails<?> item) {
        return item != null && item.getPosition() != RecyclerView.NO_POSITION;
    }

    /**
     * @return the ItemDetails for the item under the event, or null.
     */
    public abstract @Nullable ItemDetails<K> getItemDetails(@NonNull MotionEvent e);

    /**
     * Class providing access to information about a RecyclerView item.
     * Information provided by this class is used by the selection library to
     * implement various aspects of selection policy.
     *
     * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
     */
    // TODO: Can this be merged with ViewHolder?
    public abstract static class ItemDetails<K> {

        /** @return the position of an item. */
        public abstract int getPosition();

        /** @return true if the item has a stable id. */
        public boolean hasSelectionKey() {
            return getSelectionKey() != null;
        }

        /** @return the stable id of an item. */
        public abstract @Nullable K getSelectionKey();

        /**
         * @return true if the event is in an area of the item that should be
         * directly interpreted as a user wishing to select the item. This
         * is useful for checkboxes and other UI affordances focused on enabling
         * selection.
         */
        public boolean inSelectionHotspot(@NonNull MotionEvent e) {
            return false;
        }

        /**
         * Events in the drag region will dealt with differently that events outside
         * of the drag region. This allows the client to implement custom handling
         * for events related to drag and drop.
         */
        public boolean inDragRegion(@NonNull MotionEvent e) {
            return false;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return (obj instanceof ItemDetails)
                    && isEqualTo((ItemDetails) obj);
        }

        private boolean isEqualTo(@NonNull ItemDetails other) {
            K key = getSelectionKey();
            boolean sameKeys = false;
            if (key == null) {
                sameKeys = other.getSelectionKey() == null;
            } else {
                sameKeys = key.equals(other.getSelectionKey());
            }
            return sameKeys && this.getPosition() == other.getPosition();
        }

        @Override
        public int hashCode() {
            return getPosition() >>> 8;
        }
    }
}
