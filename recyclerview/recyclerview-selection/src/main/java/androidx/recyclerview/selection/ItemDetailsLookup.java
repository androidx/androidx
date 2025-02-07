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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.view.MotionEvent;

import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.RecyclerView;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The Selection library calls {@link #getItemDetails(MotionEvent)} when it needs
 * access to information about the area and/or {@link ItemDetails} under a {@link MotionEvent}.
 * Your implementation must negotiate
 * {@link RecyclerView.ViewHolder ViewHolder} lookup with the
 * corresponding RecyclerView instance, and the subsequent conversion of the ViewHolder
 * instance to an {@link ItemDetails} instance.
 *
 * <p>
 * <b>Example</b>
 * <pre>
 * final class MyDetailsLookup extends ItemDetailsLookup<Uri> {
 *
 *   private final RecyclerView mRecyclerView;
 *
 *   MyDetailsLookup(RecyclerView recyclerView) {
 *       mRecyclerView = recyclerView;
 *   }
 *
 *   public @Nullable ItemDetails<Uri> getItemDetails(@NonNull MotionEvent e) {
 *       View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
 *       if (view != null) {
 *           ViewHolder holder = mRecyclerView.getChildViewHolder(view);
 *           if (holder instanceof MyHolder) {
 *               return ((MyHolder) holder).getItemDetails();
 *           }
 *       }
 *       return null;
 *   }
 *}
 * </pre>
 *
 * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
 */
public abstract class ItemDetailsLookup<K> {

    /**
     * @return true if there is an item at the event coordinates.
     */
    final boolean overItem(@NonNull MotionEvent e) {
        return getItemPosition(e) != RecyclerView.NO_POSITION;
    }

    /**
     * @return true if there is an item w/ a stable ID at the event coordinates.
     */
    @RestrictTo(LIBRARY)
    protected boolean overItemWithSelectionKey(@NonNull MotionEvent e) {
        return overItem(e) && hasSelectionKey(getItemDetails(e));
    }

    /**
     * @return true if the event coordinates are in an area of the item
     * that can result in dragging the item. List items frequently have a white
     * area that is not draggable allowing band selection to be initiated
     * in that area.
     */
    final boolean inItemDragRegion(@NonNull MotionEvent e) {
        return overItem(e) && getItemDetails(e).inDragRegion(e);
    }

    /**
     * @return true if the event coordinates are in a "selection hot spot"
     * region of an item. Contact in these regions result in immediate
     * selection, even when there is no existing selection.
     */
    final boolean inItemSelectRegion(@NonNull MotionEvent e) {
        return overItem(e) && getItemDetails(e).inSelectionHotspot(e);
    }

    /**
     * @return the adapter position of the item at the event coordinates.
     */
    final int getItemPosition(@NonNull MotionEvent e) {
        ItemDetails<?> item = getItemDetails(e);
        return item != null
                ? item.getPosition()
                : RecyclerView.NO_POSITION;
    }

    private static boolean hasSelectionKey(@Nullable ItemDetails<?> item) {
        return item != null && item.getSelectionKey() != null;
    }

    /**
     * @return the ItemDetails for the item under the event, or null.
     */
    public abstract @Nullable ItemDetails<K> getItemDetails(@NonNull MotionEvent e);

    /**
     * An ItemDetails implementation provides the selection library with access to information
     * about a specific RecyclerView item. This class is a key component in controling
     * the behaviors of the selection library in the context of a specific activity.
     *
     * <p>
     * <b>Selection Hotspot</b>
     *
     * <p>
     * This is an optional feature identifying an area within a view that
     * is single-tap to select. Ordinarily a single tap on an item when there is no
     * existing selection will result in that item being activated. If the tap
     * occurs within the "selection hotspot" the item will instead be selected.
     *
     * <p>
     * See {@link OnItemActivatedListener} for details on handling item activation.
     *
     * <p>
     * <b>Drag Region</b>
     *
     * <p>
     * The selection library provides support for mouse driven band selection. The "lasso"
     * typically associated with mouse selection can be started only in an empty
     * area of the RecyclerView (an area where the item position == RecyclerView#NO_POSITION,
     * or where RecyclerView#findChildViewUnder returns null). But in many instances
     * the item views presented by RecyclerView will contain areas that may be perceived
     * by the user as being empty. The user may expect to be able to initiate band
     * selection in these empty areas.
     *
     * <p>
     * The "drag region" concept exists in large part to accommodate this user expectation.
     * Drag region is the content in an item view that the user doesn't otherwise
     * perceive to be empty or part of the background of recycler view.
     *
     * Take for example a traditional single column layout where
     * the view layout width is "match_parent":
     * <pre>
     * -------------------------------------------------------
     * | [icon]  A string label.   ...empty space...         |
     * -------------------------------------------------------
     *   < ---  drag region  --> < --treated as background-->
     *</pre>
     *
     * <p>
     * Further more, within a drag region, a mouse click and drag will immediately
     * initiate drag and drop (if supported by your configuration).
     *
     * <p>
     * As user expectations around touch and mouse input differ substantially,
     * "drag region" has no effect on handling of touch input.
     *
     * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
     */
    public abstract static class ItemDetails<K> {

        /**
         * Returns the adapter position of the item. See
         * {@link RecyclerView.ViewHolder#getAbsoluteAdapterPosition() ViewHolder
         * .getAbsoluteAdapterPosition}
         *
         * @return the position of an item.
         */
        public abstract int getPosition();

        /**
         * @return true if the item has a selection key.
         */
        public boolean hasSelectionKey() {
            return getSelectionKey() != null;
        }

        /**
         * @return the selection key of an item.
         */
        public abstract @Nullable K getSelectionKey();

        /**
         * Areas are often included in a view that behave similar to checkboxes, such
         * as the icon to the left of an email message. "selection
         * hotspot" provides a mechanism to identify such regions, and for the
         * library to directly translate taps in these regions into a change
         * in selection state.
         *
         * @return true if the event is in an area of the item that should be
         * directly interpreted as a user wishing to select the item. This
         * is useful for checkboxes and other UI affordances focused on enabling
         * selection.
         */
        public boolean inSelectionHotspot(@NonNull MotionEvent e) {
            return false;
        }

        /**
         * "Item Drag Region" identifies areas of an item that are not considered when the library
         * evaluates whether or not to initiate band-selection for mouse input. The drag region
         * will usually correspond to an area of an item that represents user visible content.
         * Mouse driven band selection operations are only ever initiated in non-drag-regions.
         * This is a consideration as many layouts may not include empty space between
         * RecyclerView items where band selection can be initiated.
         *
         * <p>
         * For example. You may present a single column list of contact names in a
         * RecyclerView instance in which the individual view items expand to fill all
         * available space.
         * But within the expanded view item after the contact name there may be empty space that a
         * user would reasonably expect to initiate band selection. When a MotionEvent occurs
         * in such an area, you should return identify this as NOT in a drag region.
         *
         * <p>
         * Further more, within a drag region, a mouse click and drag will immediately
         * initiate drag and drop (if supported by your configuration).
         *
         * @return true if the item is in an area of the item that can result in dragging
         * the item. List items frequently have a white area that is not draggable allowing
         * mouse driven band selection to be initiated in that area.
         */
        public boolean inDragRegion(@NonNull MotionEvent e) {
            return false;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return (obj instanceof ItemDetails)
                    && isEqualTo((ItemDetails<?>) obj);
        }

        private boolean isEqualTo(@NonNull ItemDetails<?> other) {
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
