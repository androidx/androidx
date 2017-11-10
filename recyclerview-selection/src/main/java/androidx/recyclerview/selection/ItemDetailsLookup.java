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

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;

/**
 * Provides event handlers w/ access to details about documents details
 * view items Documents in the UI (RecyclerView).
 *
 * @param <K> Selection key type. Usually String or Long.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public abstract class ItemDetailsLookup<K> {

    /** @return true if there is an item under the finger/cursor. */
    public boolean overItem(MotionEvent e) {
        return getItemPosition(e) != RecyclerView.NO_POSITION;
    }

    /** @return true if there is an item w/ a stable ID under the finger/cursor. */
    public boolean overItemWithSelectionKey(MotionEvent e) {
        return overItem(e) && hasSelectionKey(getItemDetails(e));
    }

    /**
     * @return true if the event is over an area that can be dragged via touch
     * or via mouse. List items have a white area that is not draggable.
     */
    public boolean inItemDragRegion(MotionEvent e) {
        return overItem(e) && getItemDetails(e).inDragRegion(e);
    }

    /**
     * @return true if the event is in the "selection hot spot" region.
     * The hot spot region instantly selects in touch mode, vs launches.
     */
    public boolean inItemSelectRegion(MotionEvent e) {
        return overItem(e) && getItemDetails(e).inSelectionHotspot(e);
    }

    /**
     * @return the adapter position of the item under the finger/cursor.
     */
    public int getItemPosition(MotionEvent e) {
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
     * @return the DocumentDetails for the item under the event, or null.
     */
    public abstract @Nullable ItemDetails<K> getItemDetails(MotionEvent e);

    /**
     * Abstract class providing helper classes with access to information about
     * RecyclerView item associated with a MotionEvent.
     *
     * @param <K> Selection key type. Usually String or Long.
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
        public boolean inSelectionHotspot(MotionEvent e) {
            return false;
        }

        /**
         * Events in the drag region will dealt with differently that events outside
         * of the drag region. This allows the client to implement custom handling
         * for events related to drag and drop.
         */
        public boolean inDragRegion(MotionEvent e) {
            return false;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ItemDetails) {
                return isEqualTo((ItemDetails) obj);
            }
            return false;
        }

        private boolean isEqualTo(ItemDetails other) {
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
